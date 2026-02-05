package cirilo.atc.service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    @Value("${GEMINI_API_KEY}")
    private String apiKey;

    // Choose your desired Gemini model URL here
    // Example using a common flash model, adjust if needed:
    //private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent";
    // private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-001:generateContent"; // As per your previous file
     private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1alpha/models/gemini-2.5-flash-preview-05-20:generateContent";


    private final RestTemplate restTemplate;

    public GeminiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Modified to include sourceLanguage
    public String processImage(byte[] imageBytes, String sourceLanguage, String targetLanguage, int processedWidth, int processedHeight) {
        String payload = buildGeminiPayload(imageBytes, sourceLanguage, targetLanguage, processedWidth, processedHeight);
        logger.info("Enviando requisição para Gemini com o modelo: {}", GEMINI_URL);
        logger.debug("Payload para Gemini: {}", payload); // Log payload for debugging if needed (can be large)
        long startTime = System.currentTimeMillis();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(payload, headers);
        String url = GEMINI_URL + "?key=" + apiKey;

        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            logger.error("Erro na API Gemini: {} - {}", response.getStatusCode(), response.getBody());
            throw new RuntimeException("Erro na API Gemini: " + response.getStatusCode() + " - " + response.getBody());
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Resposta recebida da Gemini em {} ms", duration);

        // Console output for Gemini's raw response
        String geminiRawResponse = response.getBody();
        System.out.println("=============== RAW GEMINI RESPONSE START ===============");
        System.out.println(geminiRawResponse);
        System.out.println("================ RAW GEMINI RESPONSE END ================");
        logger.debug("Raw Gemini Response: {}", geminiRawResponse);


        return geminiRawResponse;
    }

    // Método existente processImage(...) permanece igual

    /**
     * Traduz um bloco de texto usando a API Gemini.
     * @param textToTranslate O texto a ser traduzido.
     * @param sourceLanguage O código do idioma de origem (ex: "ja", "en").
     * @param targetLanguage O código do idioma de destino (ex: "pt-br", "en").
     * @return O texto traduzido, ou uma mensagem de erro/fallback.
     */
    public String translateText(String textToTranslate, String sourceLanguage, String targetLanguage) {
        logger.info("Requisitando tradução do Gemini para o texto. Origem: {}, Destino: {}", sourceLanguage, targetLanguage);
        String payload = buildTextTranslationPayload(textToTranslate, sourceLanguage, targetLanguage);
        // logger.debug("Payload de tradução para Gemini: {}", payload); // Pode ser verboso
        long startTime = System.currentTimeMillis();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(payload, headers);
        // Usando a mesma GEMINI_URL
        // Se um endpoint de modelo apenas de texto for preferido, defina outra constante.
        String url = GEMINI_URL + "?key=" + apiKey;

        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(url, requestEntity, String.class);
        } catch (Exception e) {
            logger.error("Erro ao chamar a API Gemini para tradução de texto: {}", e.getMessage(), e);
            return "Erro na comunicação com o serviço de tradução"; // Fallback
        }


        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            logger.error("Erro na API Gemini (translateText): {} - {}", response.getStatusCode(), response.getBody());
            return "Falha ao traduzir texto"; // Fallback
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Tradução recebida do Gemini em {} ms", duration);

        String geminiRawResponse = response.getBody();
        // System.out.println("=============== RAW GEMINI TRANSLATION RESPONSE START ===============");
        // System.out.println(geminiRawResponse);
        // System.out.println("================ RAW GEMINI TRANSLATION RESPONSE END ================");
        logger.debug("Raw Gemini Translation Response: {}", geminiRawResponse);

        // Extrai o texto traduzido da resposta do Gemini
        try {
            JSONObject rootResponse = new JSONObject(geminiRawResponse);
            if (rootResponse.has("candidates")) {
                JSONArray candidates = rootResponse.getJSONArray("candidates");
                if (!candidates.isEmpty()) {
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    if (firstCandidate.has("content") && firstCandidate.getJSONObject("content").has("parts")) {
                        JSONArray parts = firstCandidate.getJSONObject("content").getJSONArray("parts");
                        if (!parts.isEmpty() && parts.getJSONObject(0).has("text")) {
                            return parts.getJSONObject(0).getString("text").trim();
                        }
                    }
                }
            }
            logger.warn("Não foi possível extrair texto traduzido da resposta do Gemini (translateText): {}", geminiRawResponse);
            return textToTranslate; // Retorna o original como fallback se a extração falhar
        } catch (JSONException e) {
            logger.error("Erro ao parsear JSON da tradução do Gemini (translateText): {}", e.getMessage(), e);
            return textToTranslate; // Retorna o original como fallback
        }
    }

    private String buildTextTranslationPayload(String textToTranslate, String sourceLang, String targetLang) {
        String sourceLanguageName = getLanguageName(sourceLang); // Reutiliza o helper
        String targetLanguageName = getLanguageName(targetLang); // Reutiliza o helper

        // Prompt simples para tradução
        String prompt = String.format("Traduza o seguinte texto de '%s' para '%s'. Retorne APENAS o texto traduzido, sem nenhuma formatação adicional ou frases explicativas.\n\nTexto original:\n\"%s\"",
                sourceLanguageName, targetLanguageName, textToTranslate.replace("\"", "\\\"")); // Escapa aspas no texto

        return String.format("""
        {
          "contents": [
            {
              "parts": [
                { "text": "%s" }
              ]
            }
          ],
          "generation_config": {
            "response_mime_type": "application/json",
            "temperature": 0.2, 
            "maxOutputTokens": 2048 
          }
        }
        """, prompt.replace("\n", "\\n")); // Escapa novas linhas no prompt JSON
    }

    // Adapted buildGeminiPayload method
    private String buildGeminiPayload(byte[] imageBytes, String sourceLang, String targetLang, int processedWidth, int processedHeight) {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String sourceLanguageName = getLanguageName(sourceLang);
        String targetLanguageName = getLanguageName(targetLang);

        // Using the prompt structure from your input, adapted slightly for clarity and consistency
        // The prompt now asks for coordinates relative to the image it's analyzing (the processed one)
        String prompt = String.format("""
        ANÁLISE DE MANGÁ - Siga EXATAMENTE:
        A imagem que você está analisando tem %d x %d pixels. TODAS as coordenadas devem ser baseadas nestas dimensões.

        1. Detecte TODOS os balões de diálogo na imagem.
        2. Para CADA balão:
           - Coordenadas ABSOLUTAS (x,y) do CANTO SUPERIOR ESQUERDO do balão.
           - Largura (width) e altura (height) PRECISAS do balão.
           - Texto ORIGINAL extraído do balão em %s.
           - Tradução do texto original para %s.
        3. Formato de SAÍDA OBRIGATÓRIO (JSON Array):
           [{"x":number,"y":number,"width":number,"height":number,"text":"texto original","translated":"texto traduzido"}]
        4. As coordenadas (x, y, width, height) devem ser em PIXELS e relativas às dimensões da imagem que você está analisando (%d x %d pixels).
        5. Inclua APENAS o JSON array na resposta, SEM markdown, comentários ou qualquer outro texto fora do JSON.
        6. Se nenhum texto/balão for encontrado, retorne um JSON array vazio: [].
        """, processedWidth, processedHeight, sourceLanguageName, targetLanguageName, processedWidth, processedHeight);

        return String.format("""
        {
          "contents": [
            {
              "parts": [
                {
                  "text": "%s"
                },
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
        if (langCode == null || langCode.trim().isEmpty()) {
            return "o idioma original"; // Default if langCode is not specific
        }
        return switch (langCode.toLowerCase()) {
            case "pt", "pt-br" -> "português brasileiro";
            case "en", "en-us" -> "inglês americano";
            case "es" -> "espanhol";
            case "fr" -> "francês";
            case "ja", "jp" -> "japonês";
            // Add more as needed
            default -> langCode;
        };
    }
}