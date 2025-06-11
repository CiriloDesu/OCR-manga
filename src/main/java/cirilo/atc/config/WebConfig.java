package cirilo.atc.config; // Use o seu pacote de configuração

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${allowed.origin}")
    private String allowedOrigin;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Esta configuração se aplica a todos os controllers da sua aplicação
        registry.addMapping("/api/**") // Aplica o CORS a todos os caminhos que começam com /api/
                .allowedOrigins(allowedOrigin) // Permite a origem da sua extensão
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Permite os métodos necessários
                .allowedHeaders("*"); // Permite todos os cabeçalhos
    }
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}