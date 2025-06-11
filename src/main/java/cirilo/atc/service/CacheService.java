package cirilo.atc.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile; // Keep if other methods use it
import java.io.IOException; // For getBytes
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;


@Service
public class CacheService {

    // Key now depends on image bytes and target language.
    // Processed width/height might also be relevant if the Gemini call depends on them for caching.
    // For simplicity, let's assume the core is image + targetLang.
    @Cacheable(value = "translations", keyGenerator = "translationKeyGenerator")
    public String getCachedTranslation(byte[] imageBytes, String targetLang, int processedWidth, int processedHeight) {
        // This method is a cache placeholder. The actual call to GeminiService
        // happens in the controller, and this method would be called *before* that
        // if a cache hit is expected.
        // If using @Cacheable on GeminiService.processImage directly, this method might not be needed.
        // For now, let's assume it's a placeholder.
        return null;
    }

    // If you want to cache the result of GeminiService.processImage,
    // you could annotate that method directly or use a custom key generator.
    // Example key generation (can be defined as a bean for "translationKeyGenerator"):
    /*
    public String generateKey(byte[] imageBytes, String targetLang, int processedWidth, int processedHeight) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(imageBytes);
            md.update(targetLang.getBytes());
            md.update(String.valueOf(processedWidth).getBytes());
            md.update(String.valueOf(processedHeight).getBytes());
            return bytesToHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available for cache key", e);
        }
    }
    */


    // generateImageHash and bytesToHex can remain as utility if needed elsewhere
    public String generateImageHash(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(bytes);
        return bytesToHex(digest);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @CacheEvict(value = "translations", allEntries = true)
    public void evictAllTranslationsCache() {
        System.out.println("Cache de traduções invalidado.");
    }
}