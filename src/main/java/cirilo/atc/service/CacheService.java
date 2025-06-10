package cirilo.atc.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.security.MessageDigest;

@Service
public class CacheService {

    @Cacheable(value = "translations", key = "T(java.util.Objects).hash(#image.getBytes(), #sourceLang, #targetLang)")
    public String getCachedTranslation(MultipartFile image, String sourceLang, String targetLang) {
        return null; // Força chamada ao serviço quando não estiver em cache
    }

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
}