package cirilo.atc.controller;

import cirilo.atc.model.*;
import cirilo.atc.service.GeminiService;
import cirilo.atc.service.ImageProcessingService;
import cirilo.atc.util.ImageUtils; // Assuming ImageUtils.getImageDimensions and resizeImage exist
import org.imgscalr.Scalr; // For resizing
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Objects;

@RestController
@RequestMapping("/api")
public class TranslationController {

    private final GeminiService geminiService;
    private final ImageProcessingService processingService;
    private static final int MAX_DIMENSION = 2000; // Max dimension for Gemini processing

    @Autowired
    public TranslationController(GeminiService geminiService, ImageProcessingService processingService) {
        this.geminiService = geminiService;
        this.processingService = processingService;
    }

    @PostMapping(value = "/process-manga-page", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiSuccessResponse> translateMangaPage(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam("targetLanguage") String targetLanguage,
            @RequestParam(value = "displayWidth", required = false) Integer displayWidthParam,
            @RequestParam(value = "displayHeight", required = false) Integer displayHeightParam) {

        long startTime = System.currentTimeMillis();

        if (imageFile == null || imageFile.isEmpty()) {
            // This should ideally be caught by @NotNull on a DTO if using @Valid
            // For @RequestParam, Spring handles missing required files with an error.
            // If made optional, this check is good.
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
                System.out.println("Redimensionando imagem de " + originalWidth + "x" + originalHeight + " para caber em " + MAX_DIMENSION + "x" + MAX_DIMENSION);

                BufferedImage resizedImage = Scalr.resize(originalBufferedImage, Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC,
                        (originalWidth > originalHeight ? MAX_DIMENSION : (int) (MAX_DIMENSION * ((double) originalWidth / originalHeight))),
                        (originalHeight > originalWidth ? MAX_DIMENSION : (int) (MAX_DIMENSION * ((double) originalHeight / originalWidth))),
                        Scalr.OP_ANTIALIAS);

                // Convert BufferedImage back to byte array (e.g., PNG)
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                ImageIO.write(resizedImage, "png", baos);
                processedImageBytes = baos.toByteArray();

                processedWidth = resizedImage.getWidth();
                processedHeight = resizedImage.getHeight();

                scaleFactorX = (double) originalWidth / processedWidth;
                scaleFactorY = (double) originalHeight / processedHeight;
                System.out.println("Imagem redimensionada para " + processedWidth + "x" + processedHeight + ", fatores de escala: " + scaleFactorX + "x" + scaleFactorY);
            }

            // Use display dimensions if provided, otherwise original dimensions
            int displayWidth = (displayWidthParam != null && displayWidthParam > 0) ? displayWidthParam : originalWidth;
            int displayHeight = (displayHeightParam != null && displayHeightParam > 0) ? displayHeightParam : originalHeight;

            String geminiResponseString = geminiService.processImage(
                    processedImageBytes,
                    targetLanguage,
                    processedWidth, // Pass dimensions of the image sent to Gemini
                    processedHeight
            );

            List<ApiBalloon> unscaledBalloons = processingService.processGeminiResponse(geminiResponseString);
            List<ApiBalloon> scaledBalloons = new ArrayList<>();

            for (ApiBalloon unscaled : unscaledBalloons) {
                ApiBoundingBox unscaledBox = unscaled.getBounding_box();
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

            ApiProcessedPageData pageData = new ApiProcessedPageData(scaledBalloons);
            ApiDebugInfo debugInfo = new ApiDebugInfo(
                    new ApiDimensions(originalWidth, originalHeight),
                    new ApiDimensions(processedWidth, processedHeight),
                    new ApiScaleFactors(scaleFactorX, scaleFactorY),
                    new ApiDimensions(displayWidth, displayHeight) // displaySize from Node.js
            );
            long processingTime = System.currentTimeMillis() - startTime;

            ApiSuccessResponse successResponse = new ApiSuccessResponse(true, pageData, debugInfo, processingTime);
            return ResponseEntity.ok(successResponse);

        } catch (IOException e) {
            System.err.println("Erro de IO no controller: " + e.getMessage());
            // Consider how to map this to the Node.js fallback structure if strict adherence is needed.
            // For now, GlobalExceptionHandler will handle it.
            throw new RuntimeException("Erro ao processar a imagem: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Erro geral no controller: " + e.getMessage());
            e.printStackTrace(); // Log stack trace for debugging
            throw new RuntimeException("Erro inesperado: " + e.getMessage(), e);
        }
    }
}