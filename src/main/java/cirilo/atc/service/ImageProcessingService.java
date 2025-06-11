package cirilo.atc.service;

import cirilo.atc.model.ApiBalloon;
import cirilo.atc.model.ApiBoundingBox;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ImageProcessingService {

    public List<ApiBalloon> processGeminiResponse(String geminiRawResponse) {
        List<ApiBalloon> apiBalloons = new ArrayList<>();
        try {
            // Gemini might wrap the JSON in a larger structure.
            // Example: response.data.candidates[0].content.parts[0].text
            // This parsing assumes geminiRawResponse is the string containing the {"balloons": [...]} structure.
            JSONObject rootResponse = new JSONObject(geminiRawResponse);

            // Safely extract the JSON text part if the response is structured like the Node.js example
            String jsonText = "";
            if (rootResponse.has("candidates")) {
                JSONArray candidates = rootResponse.getJSONArray("candidates");
                if (candidates.length() > 0) {
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    if (firstCandidate.has("content")) {
                        JSONObject content = firstCandidate.getJSONObject("content");
                        if (content.has("parts")) {
                            JSONArray parts = content.getJSONArray("parts");
                            if (parts.length() > 0) {
                                jsonText = parts.getJSONObject(0).getString("text");
                            }
                        }
                    }
                }
            } else if (rootResponse.has("balloons")) {
                // If the raw response is already the direct JSON we need
                jsonText = geminiRawResponse;
            }


            if (jsonText.isEmpty()) {
                System.err.println("Não foi possível extrair o texto JSON da resposta do Gemini.");
                return apiBalloons; // Return empty list
            }

            // Now parse the actual JSON content
            JSONObject parsedResult = new JSONObject(jsonText);

            if (parsedResult.has("balloons")) {
                JSONArray balloonsArray = parsedResult.getJSONArray("balloons");
                for (int i = 0; i < balloonsArray.length(); i++) {
                    JSONObject balloonObj = balloonsArray.getJSONObject(i);

                    String originalText = balloonObj.optString("original_text", "");
                    String translatedText = balloonObj.optString("translated_text", "");
                    String balloonType = balloonObj.optString("balloon_type", "normal");

                    JSONObject bboxObj = balloonObj.optJSONObject("bounding_box");
                    if (bboxObj == null) {
                        System.err.println("Balão sem bounding_box, pulando: " + balloonObj.toString());
                        continue;
                    }

                    int x = bboxObj.optInt("x", 0);
                    int y = bboxObj.optInt("y", 0);
                    int width = bboxObj.optInt("width", 0);
                    int height = bboxObj.optInt("height", 0);

                    if (width <= 0 || height <= 0) {
                        System.err.println("Balão com dimensões inválidas na bounding_box, pulando: " + balloonObj.toString());
                        continue;
                    }

                    ApiBoundingBox apiBoundingBox = new ApiBoundingBox(x, y, width, height);
                    ApiBalloon apiBalloon = new ApiBalloon(originalText, translatedText, apiBoundingBox, balloonType);
                    apiBalloons.add(apiBalloon);
                }
            } else {
                System.err.println("Resposta do Gemini não contém a chave 'balloons'. Resposta: " + jsonText);
            }

        } catch (JSONException e) {
            System.err.println("Erro ao processar JSON da resposta do Gemini: " + e.getMessage());
            // Consider logging the raw response for debugging
            System.err.println("Resposta bruta do Gemini que causou o erro: " + geminiRawResponse);
        }
        return apiBalloons;
    }
}