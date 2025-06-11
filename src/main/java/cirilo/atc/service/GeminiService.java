package cirilo.atc.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Service
public class GeminiService {

    @Value("${GEMINI_API_KEY}")
    private String apiKey;

    // OLD DEPRECATED MODEL
    // private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1alpha/models/gemini-pro-vision:generateContent";

    // SUGGESTED NEWER MODEL (as per Google's error message and your previous Node.js example)
    // Note: The Node.js example used "gemini-1.5-flash-preview-0514", the error suggests "gemini-1.5-flash".
    // Let's use the more general "gemini-1.5-flash-latest" or a specific preview if you know it works best.
    // Or directly what Google suggested:
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-001:generateContent";
    // Alternatively, if you want to stick to the alpha/preview from your Node.js example (if it's still valid and works):
    // private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1alpha/models/gemini-1.5-flash-preview-0514:generateContent";
    // Or the direct suggestion from the error:
    // private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"; // Note: often models have -latest or a preview tag.

    private final RestTemplate restTemplate;

    public GeminiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String processImage(byte[] imageBytes, String targetLanguage, int processedWidth, int processedHeight) {
        String payload = buildGeminiPayload(imageBytes, targetLanguage, processedWidth, processedHeight);
        System.out.println("Enviando requisição para Gemini com o modelo: " + GEMINI_URL); // Log the model being used
        long startTime = System.currentTimeMillis();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(payload, headers); // Renamed for clarity
        String url = GEMINI_URL + "?key=" + apiKey;

        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            System.err.println("Erro na API Gemini: " + response.getBody());
            // It's good that HttpClientErrorException is thrown here, which your GlobalExceptionHandler can catch.
            // The exception already contains the status code and body.
            throw new RuntimeException("Erro na API Gemini: " + response.getStatusCode() + " - " + response.getBody());
        }

        long duration = System.currentTimeMillis() - startTime;
        System.out.printf("Resposta recebida em %d ms%n", duration);

        return response.getBody();
    }

    private String buildGeminiPayload(byte[] imageBytes, String targetLanguage, int processedWidth, int processedHeight) {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String targetLanguageName = getLanguageName(targetLanguage);

        String prompt = String.format("""
        Você é um especialista em análise de imagem de mangás. Sua tarefa é localizar com precisão todos os balões de fala e texto em uma página de mangá.

        INFORMAÇÕES IMPORTANTES:
        - A imagem que você está analisando tem %d x %d pixels.
        - TODAS as coordenadas que você fornecer DEVEM ser baseadas nestas dimensões exatas.
        - Seja extremamente preciso nas coordenadas para garantir perfeito alinhamento. A qualidade do resultado final depende da sua precisão. Verifique seu trabalho com cuidado.
        - Antes de enviar a respostar verificar se bounding box está deslocado do centro do balão!
                import cv2
                
                def refine_bounding_box(image, initial_box, padding=20):
                    x, y, w, h = initial_box["x"], initial_box["y"], initial_box["width"], initial_box["height"]
                    roi = image[y:y+h, x:x+w]
                
                    # Aumenta um pouco o ROI para capturar bordas do balão
                    roi = cv2.copyMakeBorder(roi, padding, padding, padding, padding, cv2.BORDER_CONSTANT, value=[255,255,255])
                
                    # Converte para escala de cinza e aplica limiar
                    gray = cv2.cvtColor(roi, cv2.COLOR_BGR2GRAY)
                    _, thresh = cv2.threshold(gray, 200, 255, cv2.THRESH_BINARY_INV)
                
                    # Detecta contornos
                    contours, _ = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
                    if not contours:
                        return initial_box
                
                    # Encontra o maior contorno (provavelmente o balão)
                    largest = max(contours, key=cv2.contourArea)
                    x2, y2, w2, h2 = cv2.boundingRect(largest)
                
                    # Corrige coordenadas em relação à imagem original
                    return {
                        "x": x + x2 - padding,
                        "y": y + y2 - padding,
                        "width": w2,
                        "height": h2
                    }
                
        INSTRUÇÕES:
        1. Detecte TODOS os balões de diálogo na imagem
            2. Execute OCR do texto dentro de cada balão em '%s'
            3. Traduza o texto para '%s'
            4. Retorne APENAS um JSON array com objetos contendo:
        ESTRUTURA DE SAÍDA JSON OBRIGATÓRIA:
        {
          "balloons": [
            {
              "original_text": "texto japonês original",
              "translated_text": "texto traduzido",
              "balloon_type": "tipo do balão ('normal', 'thought', 'shout', 'text_only')",
              "bounding_box": {
                "x": número (coordenada X do canto superior esquerdo em pixels),
                "y": número (coordenada Y do canto superior esquerdo em pixels),
                "width": número (LARGURA do balão em pixels),
                "height": número (ALTURA do balão em pixels)
              }
            }
          ]
        }
                5. Coordenadas devem ser em pixels absolutos
                            6. Mantenha a ordem original dos balões
                            7. Formato de resposta EXCLUSIVAMENTE JSON, sem comentários
        REGRAS CRÍTICAS:
        - Precisão é tudo. As coordenadas (x, y, width, height) devem delinear perfeitamente o balão/texto original.
        - As coordenadas devem ser em PIXELS e relativas às dimensões %d x %d.
        - Se nenhum texto for encontrado, retorne: {"balloons": []}.
        """, processedWidth, processedHeight, targetLanguageName, processedWidth, processedHeight, processedWidth, processedHeight);

        // Ensure the generation_config is compatible with the new model.
        // "gemini-1.5-flash" models typically use "application/json" for response_mime_type.
        return String.format("""
        {
          "contents": [
            {
              "parts": [
                { "text": "%s" },
                {
                  "inline_data": {
                    "mime_type": "image/png",
                    "data": "%s"
                  }
                }
              ]
            }
          ],
          "generation_config": {
            "response_mime_type": "application/json",
            "temperature": 0.1,
            "maxOutputTokens": 8192
          }
        }
        """, prompt.replace("\"", "\\\"").replace("\n", "\\n"), base64Image);
    }

    private String getLanguageName(String langCode) {
        return switch (langCode.toLowerCase()) {
            case "pt", "pt-br" -> "português brasileiro";
            case "en", "en-us" -> "inglês americano";
            case "es" -> "espanhol";
            case "fr" -> "francês";
            default -> langCode;
        };
    }
}