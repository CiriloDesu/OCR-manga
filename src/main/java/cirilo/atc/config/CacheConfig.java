package cirilo.atc.config;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@Configuration
public class CacheConfig {

    @Bean("translationKeyGenerator")
    public KeyGenerator translationKeyGenerator() {
        return (target, method, params) -> {
            // params are: byte[] imageBytes, String targetLang, int processedWidth, int processedHeight
            if (params.length < 4) {
                return "defaultKey"; // Or throw error
            }
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update((byte[]) params[0]); // imageBytes
                md.update(((String) params[1]).getBytes()); // targetLang
                md.update(String.valueOf((int) params[2]).getBytes()); // processedWidth
                md.update(String.valueOf((int) params[3]).getBytes()); // processedHeight
                return bytesToHex(md.digest());
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("MD5 not available for cache key", e);
            }
        };
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}