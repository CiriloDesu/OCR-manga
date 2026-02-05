package cirilo.atc.controller;

import cirilo.atc.model.*;
import cirilo.atc.service.GeminiService;
import cirilo.atc.service.ImageProcessingService;
import cirilo.atc.service.LocalOcrService; // Importar o novo serviço
import cirilo.atc.service.OcrTextRegion;
import net.sourceforge.tess4j.TesseractException; // Importar exceção do Tesseract
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TranslationController {

    private static final Logger logger = LoggerFactory.getLogger(TranslationController.class);

    private final GeminiService geminiService;
    private final ImageProcessingService processingService;
    private final LocalOcrService localOcrService; // Adicionar o novo serviço
    private static final int MAX_DIMENSION = 2000; // Para o endpoint original

    @Value("${app.debug.enabled:false}")
    private boolean debugEnabled;

    @Value("${app.debug.output-path:debug_output.png}")
    private String debugOutputPath;

    @Autowired
    public TranslationController(GeminiService geminiService,
                                 ImageProcessingService processingService,
                                 LocalOcrService localOcrService) { // Injetar o novo serviço
        this.geminiService = geminiService;
        this.processingService = processingService;
        this.localOcrService = localOcrService;
    }

    // Endpoint original que usa Gemini para tudo
    @PostMapping(value = "/process-manga-page", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiSuccessResponse> translateMangaPage(
            // ... (código existente deste método) ...
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam(value = "sourceLanguage", defaultValue = "ja") String sourceLanguage,
            @RequestParam("targetLanguage") String targetLanguage,
            @RequestParam(value = "displayWidth", required = false) Integer displayWidthParam,
            @RequestParam(value = "displayHeight", required = false) Integer displayHeightParam) {

        long startTime = System.currentTimeMillis();

        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma imagem enviada");
        }

        try {
            byte[] originalImageBytes = imageFile.getBytes();
            BufferedImage originalBufferedImage = ImageIO.read(new ByteArrayInputStream(originalImageBytes));
            if (originalBufferedImage == null) {
                throw new IOException("Não foi possível ler a imagem. Formato pode não ser suportado.");
            }

            int originalWidth = originalBufferedImage.getWidth();
            int originalHeight = originalBufferedImage.getHeight();

            byte[] processedImageBytes = originalImageBytes;
            int processedWidth = originalWidth;
            int processedHeight = originalHeight;
            double scaleFactorX = 1.0;
            double scaleFactorY = 1.0;

            if (originalWidth > MAX_DIMENSION || originalHeight > MAX_DIMENSION) {
                logger.info("Redimensionando imagem de {}x{} para caber em {}x{}", originalWidth, originalHeight, MAX_DIMENSION, MAX_DIMENSION);

                BufferedImage resizedImage = Scalr.resize(originalBufferedImage, Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC,
                        (originalWidth > originalHeight ? MAX_DIMENSION : (int) (MAX_DIMENSION * ((double) originalWidth / originalHeight))),
                        (originalHeight > originalWidth ? MAX_DIMENSION : (int) (MAX_DIMENSION * ((double) originalHeight / originalWidth))),
                        Scalr.OP_ANTIALIAS);

                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                ImageIO.write(resizedImage, "png", baos); // Ensure PNG for consistency with payload
                processedImageBytes = baos.toByteArray();

                processedWidth = resizedImage.getWidth();
                processedHeight = resizedImage.getHeight();

                scaleFactorX = (double) originalWidth / processedWidth;
                scaleFactorY = (double) originalHeight / processedHeight;
                logger.info("Imagem redimensionada para {}x{}, fatores de escala: X={}, Y={}", processedWidth, processedHeight, scaleFactorX, scaleFactorY);
            }

            int displayWidth = (displayWidthParam != null && displayWidthParam > 0) ? displayWidthParam : originalWidth;
            int displayHeight = (displayHeightParam != null && displayHeightParam > 0) ? displayHeightParam : originalHeight;

            String geminiResponseString = geminiService.processImage(
                    processedImageBytes,
                    sourceLanguage,
                    targetLanguage,
                    processedWidth,
                    processedHeight
            );

            List<ApiBalloon> unscaledBalloons = processingService.processGeminiResponse(geminiResponseString);
            List<ApiBalloon> scaledBalloons = new ArrayList<>();

            for (ApiBalloon unscaled : unscaledBalloons) {
                ApiBoundingBox unscaledBox = unscaled.getBounding_box();
                if (unscaledBox == null) {
                    logger.warn("Balão não escalado sem bounding_box, pulando: {}", unscaled.getOriginal_text());
                    continue;
                }
                int scaledX = (int) Math.round(unscaledBox.getX() * scaleFactorX);
                int scaledY = (int) Math.round(unscaledBox.getY() * scaleFactorY);
                int scaledWidth = (int) Math.round(unscaledBox.getWidth() * scaleFactorX);
                int scaledHeight = (int) Math.round(unscaledBox.getHeight() * scaleFactorY);

                ApiBoundingBox scaledBox = new ApiBoundingBox(scaledX, scaledY, scaledWidth, scaledHeight);
                scaledBalloons.add(new ApiBalloon(
                        unscaled.getOriginal_text(),
                        unscaled.getTranslated_text(),
                        scaledBox,
                        unscaled.getBalloon_type()
                ));
            }

            if (debugEnabled) {
                try {
                    processingService.debugOutput(originalImageBytes, scaledBalloons, debugOutputPath);
                } catch (IOException e) {
                    logger.error("Erro ao gerar imagem de debug: {}", e.getMessage(), e);
                }
            }

            ApiProcessedPageData pageData = new ApiProcessedPageData(scaledBalloons);
            ApiDebugInfo debugInfo = new ApiDebugInfo(
                    new ApiDimensions(originalWidth, originalHeight),
                    new ApiDimensions(processedWidth, processedHeight),
                    new ApiScaleFactors(scaleFactorX, scaleFactorY),
                    new ApiDimensions(displayWidth, displayHeight)
            );
            long processingTime = System.currentTimeMillis() - startTime;
            logger.info("Tempo total de processamento da requisição (Gemini Full): {} ms", processingTime);

            ApiSuccessResponse successResponse = new ApiSuccessResponse(true, pageData, debugInfo, processingTime);
            return ResponseEntity.ok(successResponse);

        } catch (IOException e) {
            logger.error("Erro de IO no controller (Gemini Full): {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao processar a imagem: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Erro geral no controller (Gemini Full): {}", e.getMessage(), e);
            throw new RuntimeException("Erro inesperado: " + e.getMessage(), e);
        }
    }


    // Novo endpoint para OCR Local + Tradução Gemini
    @PostMapping(value = "/process-manga-page-local-ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiSuccessResponse> translateMangaPageLocalOcr(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam(value = "sourceLanguage", defaultValue = "jpn") String sourceLanguageTess, // Para Tesseract (ex: jpn, eng)
            @RequestParam(value = "sourceLanguageGemini", defaultValue = "ja") String sourceLanguageGemini, // Para Gemini (ex: ja, en)
            @RequestParam("targetLanguage") String targetLanguage,
            @RequestParam(value = "displayWidth", required = false) Integer displayWidthParam,
            @RequestParam(value = "displayHeight", required = false) Integer displayHeightParam) {

        long startTime = System.currentTimeMillis();

        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma imagem enviada");
        }

        try {
            byte[] originalImageBytes = imageFile.getBytes();
            BufferedImage originalBufferedImage = ImageIO.read(new ByteArrayInputStream(originalImageBytes));
            if (originalBufferedImage == null) {
                throw new IOException("Não foi possível ler a imagem. Formato pode não ser suportado.");
            }

            int originalWidth = originalBufferedImage.getWidth();
            int originalHeight = originalBufferedImage.getHeight();

            // Realiza OCR local
            // Não há redimensionamento aqui, Tesseract processará a imagem original.
            // Se o desempenho for um problema, ou a qualidade do OCR, pré-processamento (incluindo redimensionamento)
            // pode ser necessário antes de chamar localOcrService.performOcr.
            List<OcrTextRegion> ocrRegions = localOcrService.performOcr(originalImageBytes, sourceLanguageTess);
            List<ApiBalloon> finalBalloons = new ArrayList<>();

            for (OcrTextRegion region : ocrRegions) {
                if (region.getText() == null || region.getText().trim().isEmpty()) {
                    logger.debug("Região OCR vazia pulada.");
                    continue;
                }
                // Traduz cada texto detectado pelo OCR local
                String translatedText = geminiService.translateText(region.getText(), sourceLanguageGemini, targetLanguage);

                // As coordenadas de 'region' são absolutas para a imagem original
                ApiBoundingBox bbox = new ApiBoundingBox(region.getX(), region.getY(), region.getWidth(), region.getHeight());
                finalBalloons.add(new ApiBalloon(region.getText(), translatedText, bbox, "ocr_local")); // Tipo de balão
            }

            if (debugEnabled) {
                try {
                    // As coordenadas em finalBalloons já são absolutas para a imagem original
                    processingService.debugOutput(originalImageBytes, finalBalloons, debugOutputPath.replace(".png", "_local_ocr.png"));
                } catch (IOException e) {
                    logger.error("Erro ao gerar imagem de debug para OCR local: {}", e.getMessage(), e);
                }
            }

            ApiProcessedPageData pageData = new ApiProcessedPageData(finalBalloons);
            // Para este fluxo, não há "imagem processada" separada (a menos que você adicione pré-processamento para OCR)
            // e não há fatores de escala aplicados após o OCR.
            ApiDebugInfo debugInfo = new ApiDebugInfo(
                    new ApiDimensions(originalWidth, originalHeight),
                    new ApiDimensions(originalWidth, originalHeight), // "Processada" é a mesma que original neste fluxo
                    new ApiScaleFactors(1.0, 1.0), // Sem escalonamento pós-OCR
                    new ApiDimensions(
                            (displayWidthParam != null && displayWidthParam > 0) ? displayWidthParam : originalWidth,
                            (displayHeightParam != null && displayHeightParam > 0) ? displayHeightParam : originalHeight
                    )
            );
            long processingTime = System.currentTimeMillis() - startTime;
            logger.info("Tempo total de processamento da requisição (OCR Local): {} ms", processingTime);

            ApiSuccessResponse successResponse = new ApiSuccessResponse(true, pageData, debugInfo, processingTime);
            return ResponseEntity.ok(successResponse);

        } catch (TesseractException e) {
            logger.error("Erro durante OCR local com Tesseract: {}", e.getMessage(), e);
            // Você pode querer um status HTTP diferente para erros de OCR, ex: 500 ou 503
            throw new RuntimeException("Erro no OCR local: " + e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Erro de IO no controller (OCR Local): {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao processar a imagem (OCR Local): " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Erro geral no controller (OCR Local): {}", e.getMessage(), e);
            throw new RuntimeException("Erro inesperado (OCR Local): " + e.getMessage(), e);
        }
    }
}