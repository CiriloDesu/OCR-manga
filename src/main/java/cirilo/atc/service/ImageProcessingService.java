package cirilo.atc.service;

import cirilo.atc.model.ApiBalloon;
import cirilo.atc.model.ApiBoundingBox;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImageProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(ImageProcessingService.class);

    // Adapted processGeminiResponse method
    public List<ApiBalloon> processGeminiResponse(String geminiResponse) {
        List<ApiBalloon> apiBalloons = new ArrayList<>();
        if (geminiResponse == null || geminiResponse.trim().isEmpty()) {
            logger.warn("Resposta do Gemini vazia ou nula.");
            return apiBalloons;
        }

        try {
            String jsonToParse = geminiResponse;
            try {
                JSONObject rootResponse = new JSONObject(geminiResponse);
                if (rootResponse.has("candidates")) {
                    JSONArray candidates = rootResponse.getJSONArray("candidates");
                    if (!candidates.isEmpty()) {
                        JSONObject firstCandidate = candidates.getJSONObject(0);
                        if (firstCandidate.has("content") && firstCandidate.getJSONObject("content").has("parts")) {
                            JSONArray parts = firstCandidate.getJSONObject("content").getJSONArray("parts");
                            if (!parts.isEmpty() && parts.getJSONObject(0).has("text")) {
                                jsonToParse = parts.getJSONObject(0).getString("text");
                                logger.info("Extraído JSON do wrapper padrão do Gemini.");
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                logger.debug("Resposta do Gemini não parece ser o wrapper padrão, tentando parse direto: {}", e.getMessage());
            }

            JSONArray jsonArray = new JSONArray(jsonToParse.trim());

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String originalText = obj.optString("text", "");
                String translatedText = obj.optString("translated", "");
                int x = obj.optInt("x");
                int y = obj.optInt("y");
                int width = obj.optInt("width");
                int height = obj.optInt("height");

                if (width <= 0 || height <= 0) {
                    logger.warn("Balão com dimensões inválidas (width/height <= 0), pulando: {}", obj.toString());
                    continue;
                }

                ApiBoundingBox apiBoundingBox = new ApiBoundingBox(x, y, width, height);
                ApiBalloon apiBalloon = new ApiBalloon(originalText, translatedText, apiBoundingBox, "normal");
                apiBalloons.add(apiBalloon);
            }
        } catch (JSONException e) {
            logger.error("Erro ao processar JSON da resposta do Gemini: {}", e.getMessage(), e);
            logger.error("Resposta bruta do Gemini que causou o erro: {}", geminiResponse);
        }
        return apiBalloons;
    }

    public void debugOutput(byte[] imageBytes, List<ApiBalloon> balloons, String outputPath) throws IOException {
        if (balloons == null || balloons.isEmpty()) {
            logger.info("Nenhum balão para desenhar na imagem de debug.");
            return;
        }
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        Graphics2D g = image.createGraphics();
        g.setColor(Color.RED);

        for (ApiBalloon balloon : balloons) {
            ApiBoundingBox bbox = balloon.getBounding_box();
            if (bbox != null) {
                g.drawRect(bbox.getX(), bbox.getY(), bbox.getWidth(), bbox.getHeight());
            }
        }

        File outputFile = new File(outputPath);
        // Create parent directories if they don't exist
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (parentDir.mkdirs()) {
                logger.info("Diretório de debug criado: {}", parentDir.getAbsolutePath());
            } else {
                logger.error("Falha ao criar diretório de debug: {}", parentDir.getAbsolutePath());
                // Optionally, you could throw an IOException here or decide not to write the file
                // For now, it will proceed and likely fail at ImageIO.write if mkdirs failed
            }
        }

        ImageIO.write(image, "png", outputFile);
        logger.info("Imagem de debug salva em: {}", outputFile.getAbsolutePath());
        g.dispose();
    }
}