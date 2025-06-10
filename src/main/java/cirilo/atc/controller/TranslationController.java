package cirilo.atc.controller;

import cirilo.atc.model.Balloon;
import cirilo.atc.model.TranslationRequest;
import cirilo.atc.model.TranslationResponse;
import cirilo.atc.service.GeminiService;
import cirilo.atc.service.ImageProcessingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("api/translate")
@Validated
public class TranslationController {

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("ja", "en", "pt");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private ImageProcessingService processingService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<TranslationResponse> translateManga(
            @Valid TranslationRequest request) throws IOException {

        validateRequest(request);

        // Obter bytes da imagem
        byte[] imageBytes = request.getImage().getBytes();

        String geminiResponse = geminiService.processImage(
                imageBytes, // Passando bytes em vez de MultipartFile
                request.getSourceLanguage(),
                request.getTargetLanguage()
        );

        List<Balloon> balloons = processingService.processGeminiResponse(
                imageBytes, // Passando bytes para processamento
                geminiResponse
        );

        return ResponseEntity.ok(new TranslationResponse(balloons));
    }

    private void validateRequest(TranslationRequest request) {
        if (!SUPPORTED_LANGUAGES.contains(request.getSourceLanguage())) {
            throw new IllegalArgumentException("Idioma de origem não suportado");
        }

        if (!SUPPORTED_LANGUAGES.contains(request.getTargetLanguage())) {
            throw new IllegalArgumentException("Idioma de destino não suportado");
        }

        if (request.getImage().getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Tamanho da imagem excede 10MB");
        }

        if (!request.getImage().getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("Tipo de arquivo não suportado");
        }
    }
}