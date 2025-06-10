package cirilo.atc;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MangaTranslatorApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().load();

        System.out.println("=== ENV VARS ===");
        System.out.println("GEMINI_API_KEY: " + (dotenv.get("GEMINI_API_KEY") != null ? "***" : "null"));
        System.out.println("GEMINI_PROJECT_ID: " + dotenv.get("GEMINI_PROJECT_ID"));
        System.out.println("=================");

        dotenv.entries().forEach(e ->
                System.setProperty(e.getKey(), e.getValue())
        );

        SpringApplication.run(MangaTranslatorApplication.class, args);
    }
}