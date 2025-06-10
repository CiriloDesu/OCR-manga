package cirilo.atc.service;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content; // <-- IMPORT ESSENCIAL
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.PartMaker;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    private final GenerativeModel model;

    @Autowired
    public GeminiService(VertexAI vertexAI) {
        this.model = new GenerativeModel("gemini-2.5-flash-preview-05-20", vertexAI);
    }

    public String processImage(byte[] imageBytes, String sourceLang, String targetLang) {
        try {
            String prompt = String.format("""
               Tarefa: Tradução de mangá
               Requisitos:
               1. Detecte TODOS os balões de diálogo na imagem
               2. Execute OCR do texto dentro de cada balão em '%s'
               3. Traduza o texto para '%s'
               4. Retorne APENAS um JSON array com objetos contendo:
                  {
                    "translatedText": "texto traduzido",
                    "x": x,         // coordenada X do canto superior esquerdo
                    "y": y,         // coordenada Y do canto superior esquerdo
                    "width": width,  // largura do balão
                    "height": height // altura do balão
                  }
               5. Coordenadas devem ser em pixels absolutos
               6. Mantenha a ordem original dos balões
               7. Formato de resposta EXCLUSIVAMENTE JSON, sem comentários"""
                    , sourceLang, targetLang);


            Part imagePart = PartMaker.fromMimeTypeAndData("image/png", imageBytes);
            Part textPart = Part.newBuilder().setText(prompt).build();

            Content contentRequest = Content.newBuilder()
                    .addParts(imagePart)
                    .addParts(textPart)
                    .build();

            GenerateContentResponse response = model.generateContent(contentRequest);

            return ResponseHandler.getText(response);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar imagem com Gemini: " + e.getMessage(), e);
        }
    }
}