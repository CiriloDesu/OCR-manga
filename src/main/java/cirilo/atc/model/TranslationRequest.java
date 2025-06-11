package cirilo.atc.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public class TranslationRequest {

    @NotNull(message = "A imagem não pode ser nula")
    // Você pode criar anotações customizadas para validações mais complexas
    // @IsImage(message = "O arquivo deve ser uma imagem")
    // @MaxFileSize(size = 10 * 1024 * 1024, message = "O arquivo excede 10MB")
    private MultipartFile image;

    @NotBlank(message = "O idioma de origem é obrigatório")
    // @SupportedLanguage(message = "Idioma de origem não suportado")
    private String sourceLanguage;

    @NotBlank(message = "O idioma de destino é obrigatório")
    // @SupportedLanguage(message = "Idioma de destino não suportado")
    private String targetLanguage;

    public MultipartFile getImage() {
        return image;
    }

    public void setImage(MultipartFile image) {
        this.image = image;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public void setSourceLanguage(String sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage;
    }
}
