package cirilo.atc.service;

import cirilo.atc.model.Balloon;
import cirilo.atc.util.ImageUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImageProcessingService {

    public List<Balloon> processGeminiResponse(byte[] imageBytes, String geminiResponse) throws IOException {
        JSONArray jsonArray = new JSONArray(geminiResponse);
        List<Balloon> balloons = new ArrayList<>();
        int[] imageDimensions = ImageUtils.getImageDimensions(imageBytes);

        for (int i = 0; i < jsonArray.length(); ++i){
            JSONObject obj = jsonArray.getJSONObject(i);
            Balloon balloon = new Balloon();

            balloon.setTranslatedText(obj.getString("translatedText"));
            balloon.setX(obj.getInt("x"));
            balloon.setX(obj.getInt("y"));
            balloon.setWidth(obj.getInt("width"));
            balloon.setHeight(obj.getInt("height"));

            int fontSize = calculateOptimalFontSize(balloon, imageDimensions[0]);
            balloon.setFontSize(fontSize);

            balloons.add(balloon);
        }
        return balloons;
    }

    private int calculateOptimalFontSize(Balloon balloon, int imageWidth){
        int baseSize = (int) (imageWidth * 0.03);
        String text = balloon.getTranslatedText();
        int maxWidth = balloon.getWidth() - 20;
        int maxHeight = balloon.getHeight() - 10;

        // reduzir ate cabe no balao
        while (baseSize > 8) {
            int textWidth = estimateTextWidth(text, baseSize);
            int textHeight = baseSize; // Altura aproximada

            if (textWidth <= maxWidth && textHeight <= maxHeight) break;
            baseSize--;
        }

        return Math.max(baseSize, 8); // Tamanho mínimo de fonte
    }

    private int estimateTextWidth(String text, int fontSize){
        //estimativa de 0.6 fontSize * numero de caracteres
        return (int) (0.6 * fontSize * text.length());
    }
}
