package pelletpanic;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/** Shared original artwork, loaded once from the packaged classpath. */
final class GameAssets {
    private static final Map<String, BufferedImage> IMAGES = new ConcurrentHashMap<>();

    private GameAssets() {
    }

    static BufferedImage image(String path) {
        return IMAGES.computeIfAbsent(path, key -> {
            try (var stream = GameAssets.class.getResourceAsStream(key)) {
                if (stream == null) {
                    throw new IllegalStateException("Missing image: " + key);
                }
                BufferedImage image = ImageIO.read(stream);
                if (image == null) {
                    throw new IllegalStateException("Invalid image: " + key);
                }
                return image;
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot load image: " + key, exception);
            }
        });
    }
}
