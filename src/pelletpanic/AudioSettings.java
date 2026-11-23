package pelletpanic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

record AudioSettings(boolean muted, float music, float effects) {
    static final AudioSettings DEFAULT = new AudioSettings(false, .18f, .55f);

    AudioSettings {
        if (!Float.isFinite(music) || !Float.isFinite(effects) || music < 0 || music > 1 || effects < 0 || effects > 1) {
            throw new IllegalArgumentException("Audio volumes must be between 0 and 1.");
        }
    }

    static AudioSettings load(Path path) {
        if (Files.notExists(path)) {
            return DEFAULT;
        }
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Properties values = new Properties();
            values.load(reader);
            String muted = values.getProperty("muted");
            if (!"1".equals(values.getProperty("version")) || !("true".equals(muted) || "false".equals(muted))) {
                throw new IllegalArgumentException("Unsupported audio settings.");
            }
            return new AudioSettings(Boolean.parseBoolean(values.getProperty("muted")),
                    Float.parseFloat(values.getProperty("music", "")), Float.parseFloat(values.getProperty("effects", "")));
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("Audio settings could not be read; using defaults. Original file preserved: " + path);
            return DEFAULT;
        }
    }

    void save(Path path) throws IOException {
        path = path.toAbsolutePath();
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), "audio-", ".tmp");
        try {
            Properties values = new Properties();
            values.setProperty("version", "1");
            values.setProperty("muted", Boolean.toString(muted));
            values.setProperty("music", Float.toString(music));
            values.setProperty("effects", Float.toString(effects));
            try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                values.store(writer, "Pellet Panic audio preferences");
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
