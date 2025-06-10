package cirilo.atc.util;

import org.imgscalr.Scalr;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ImageUtils {

    public static int[] getImageDimensions(byte[] imageData) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageData));
        return new int[]{img.getWidth(), img.getHeight()};
    }

    public static BufferedImage resizeImage(byte[] imageData, int targetWidth) throws IOException {
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
        return Scalr.resize(originalImage, targetWidth);
    }
}
