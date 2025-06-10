package cirilo.atc.config;

// IMPORT CORRETO: Este @Value é do Spring, para injetar propriedades do application.properties
import org.springframework.beans.factory.annotation.Value;
import com.google.cloud.vertexai.VertexAI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration; // IMPORT ESSENCIAL

import java.io.IOException;

@Configuration
public class GeminiConfig {

    @Value("${GEMINI_API_KEY}")
    private String apiKey;

    @Value("${GEMINI_PROJECT_ID}")
    private String projectId;

    @Value("${GEMINI_LOCATION}")
    private String location;

    @Bean
    public VertexAI vertexAI() throws IOException {
        return new VertexAI(projectId, location);
    }
}