//package cirilo.atc.config;
//
//import com.google.auth.oauth2.GoogleCredentials;
//import com.google.cloud.vertexai.VertexAI;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.web.client.RestTemplate;
//
//import java.io.IOException;
//
//@Configuration
//public class GeminiConfig {
//
//    @Value("${GEMINI_PROJECT_ID:}")
//    private String projectId;
//
//    @Value("${GEMINI_LOCATION:us-central1}")
//    private String location;
//
//    @Bean
//    public VertexAI vertexAI() throws IOException {
//        GoogleCredentials credentials = GoogleCredentials.fromStream(
//                new ClassPathResource("credentials.json").getInputStream()
//        );
//
//        return new VertexAI.Builder()
//                .setProjectId(projectId)
//                .setLocation(location)
//                .setCredentials(credentials)
//                .build();
//    }
//
//
//    @Bean
//    public RestTemplate restTemplate() {
//        return new RestTemplate();
//    }
//}