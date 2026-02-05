package cirilo.atc.service;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.LoadLibs; // Para ajudar a carregar libs nativas
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Rectangle; // Para bounding boxes
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class LocalOcrService {

    private static final Logger logger = LoggerFactory.getLogger(LocalOcrService.class);
    private final ITesseract tesseractInstance;

    public LocalOcrService(@Value("${tessdata.path:./tessdata}") String tessDataPathConfig) {
        tesseractInstance = new Tesseract();
        try {
            // Tenta carregar as bibliotecas nativas do Tesseract que vêm com Tess4J
            File tempFolder = LoadLibs.extractTessResources("tessdata");
            tesseractInstance.setDatapath(tempFolder.getAbsolutePath());
            logger.info("Tesseract data path (via LoadLibs) set to: {}", tempFolder.getAbsolutePath());
        } catch (Exception e) {
            logger.warn("Falha ao extrair tessdata com LoadLibs, tentando caminho configurado: {}. Erro: {}", tessDataPathConfig, e.getMessage());
            // Se LoadLibs falhar ou se você preferir um caminho explícito:
            tesseractInstance.setDatapath(tessDataPathConfig);
            logger.info("Tesseract data path (via config) set to: {}", tessDataPathConfig);
        }
    }

    /**
     * Realiza OCR em uma imagem e tenta extrair blocos de texto com coordenadas.
     * NOTA: A segmentação precisa de "balões" é um desafio complexo.
     * Esta implementação usa getSegmentedRegions no nível de linha de texto,
     * o que pode ser uma aproximação.
     *
     * @param imageBytes Bytes da imagem a ser processada.
     * @param sourceLanguageTess Código do idioma para o Tesseract (ex: "jpn", "eng").
     * @return Uma lista de OcrTextRegion.
     * @throws IOException Se houver erro ao ler a imagem.
     * @throws TesseractException Se o OCR falhar.
     */
    public List<OcrTextRegion> performOcr(byte[] imageBytes, String sourceLanguageTess) throws IOException, TesseractException {
        logger.info("Realizando OCR local para o idioma: {}", sourceLanguageTess);
        List<OcrTextRegion> textRegions = new ArrayList<>();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) {
            throw new IOException("Não foi possível ler os bytes da imagem para OCR.");
        }

        tesseractInstance.setLanguage(sourceLanguageTess);
        tesseractInstance.setPageSegMode(ITessAPI.TessPageSegMode.PSM_AUTO_OSD); // Tenta detectar orientação e script

        try {
            // Obter regiões segmentadas (nível de linha de texto pode ser uma boa aproximação para balões)
            // ITessAPI.TessPageIteratorLevel.RIL_TEXTLINE para linhas de texto
            // ITessAPI.TessPageIteratorLevel.RIL_BLOCK para blocos de texto
            List<Rectangle> regions = tesseractInstance.getSegmentedRegions(image, ITessAPI.TessPageIteratorLevel.RIL_TEXTLINE);
            logger.debug("Tesseract encontrou {} regiões segmentadas (linhas de texto).", regions.size());

            for (Rectangle rect : regions) {
                // Realiza OCR dentro de cada região detectada
                String regionText = tesseractInstance.doOCR(image, rect);
                if (regionText != null && !regionText.trim().isEmpty()) {
                    textRegions.add(new OcrTextRegion(
                            regionText.trim(),
                            rect.x,
                            rect.y,
                            rect.width,
                            rect.height
                    ));
                    logger.debug("OCR da região [x:{}, y:{}, w:{}, h:{}]: {}", rect.x, rect.y, rect.width, rect.height, regionText.trim());
                }
            }
        } catch (TesseractException e) {
            logger.error("Falha no OCR com Tesseract", e);
            throw e;
        }

        if (textRegions.isEmpty()) {
            logger.warn("Nenhum texto detectado pelo OCR local. Tentando OCR na imagem inteira como fallback.");
            try {
                String fullImageText = tesseractInstance.doOCR(image);
                if (fullImageText != null && !fullImageText.trim().isEmpty()) {
                    textRegions.add(new OcrTextRegion(
                            fullImageText.trim(),
                            0, 0, // Coordenadas da imagem inteira
                            image.getWidth(), image.getHeight()
                    ));
                    logger.info("OCR da imagem inteira (fallback): {}", fullImageText.trim());
                }
            } catch (TesseractException e) {
                logger.error("Falha no OCR da imagem inteira (fallback)", e);
                // Não relança a exceção aqui para permitir que o fluxo continue se o OCR parcial falhou
            }
        }


        logger.info("OCR local encontrou {} regiões de texto.", textRegions.size());
        return textRegions;
    }
}