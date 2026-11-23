package pelletpanic;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** File-backed profiles. No database, JDBC driver, or network connection. */
final class LocalGameStore implements AutoCloseable {
    private record SkinDefinition(String name, String prefix, int price) {
        Skin skin(boolean purchased) {
            return new Skin(name, price, purchased,
                    image("up1"), image("down1"), image("left1"), image("right1"),
                    image("up2"), image("down2"), image("left2"), image("right2"));
        }

        private String image(String frame) {
            return "/Images/" + prefix + frame + ".png";
        }
    }

    private static final List<SkinDefinition> CATALOGUE = List.of(
            new SkinDefinition("RedPlanet", "red", 0),
            new SkinDefinition("BluePlanet", "blueplanet", 25),
            new SkinDefinition("Earth", "earth", 50),
            new SkinDefinition("PurplePlanet", "purpleplanet", 75),
            new SkinDefinition("Sun", "sun", 100));

    static Skin starterSkin() {
        return CATALOGUE.get(0).skin(true);
    }

    private record Profile(int coins, int score, int highScore, Set<String> purchased, String equipped) {
        Profile {
            purchased = Set.copyOf(purchased);
        }
    }

    private final Path directory;
    private final Map<Integer, Profile> profiles = new HashMap<>();
    private final FileChannel lockChannel;
    private final FileLock lock;

    LocalGameStore(Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
        FileChannel channel = null;
        try {
            Files.createDirectories(this.directory);
            channel = FileChannel.open(this.directory.resolve(".lock"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            FileLock acquired = channel.tryLock();
            if (acquired == null) {
                throw new IOException("Another game is already using this save folder. Close it first.");
            }
            lockChannel = channel;
            lock = acquired;
        } catch (IOException | OverlappingFileLockException exception) {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException closeFailure) {
                    exception.addSuppressed(closeFailure);
                }
            }
            throw failure("Cannot open local saves; close other game instances and check folder permissions", exception);
        }
    }

    synchronized int getCoins(int playerId) {
        return profile(playerId).coins();
    }

    synchronized int getHighScore(int playerId) {
        return profile(playerId).highScore();
    }

    synchronized void saveGameData(int coins, int score, int highScore, int playerId) {
        requireNonNegative(coins, "coins");
        requireNonNegative(score, "score");
        requireNonNegative(highScore, "high score");
        Profile current = profile(playerId);
        commit(playerId, new Profile(Math.addExact(current.coins(), coins), score,
                Math.max(current.highScore(), Math.max(score, highScore)), current.purchased(), current.equipped()));
    }

    synchronized List<Skin> getSkins(int playerId, boolean purchased) {
        Profile current = profile(playerId);
        List<Skin> result = new ArrayList<>();
        for (SkinDefinition definition : CATALOGUE) {
            if (current.purchased().contains(definition.name()) == purchased) {
                result.add(definition.skin(purchased));
            }
        }
        return result;
    }

    synchronized Skin getEquippedSkin(int playerId) {
        return definition(profile(playerId).equipped()).skin(true);
    }

    synchronized boolean purchaseSkin(int playerId, String name, int quotedPrice) {
        SkinDefinition skin = definition(name);
        Profile current = profile(playerId);
        if (quotedPrice != skin.price() || current.purchased().contains(name) || current.coins() < skin.price()) {
            return false;
        }
        Set<String> purchased = new HashSet<>(current.purchased());
        purchased.add(name);
        // Debit and ownership are committed together in the same file replacement.
        commit(playerId, new Profile(current.coins() - skin.price(), current.score(), current.highScore(),
                purchased, current.equipped()));
        return true;
    }

    synchronized void equipSkin(int playerId, String name) {
        definition(name);
        Profile current = profile(playerId);
        if (!current.purchased().contains(name)) {
            throw new IllegalArgumentException("Skin has not been purchased: " + name);
        }
        commit(playerId, new Profile(current.coins(), current.score(), current.highScore(), current.purchased(), name));
    }

    synchronized void resetPurchases(int playerId) {
        Profile current = profile(playerId);
        commit(playerId, new Profile(current.coins(), current.score(), current.highScore(), Set.of("RedPlanet"), "RedPlanet"));
    }

    private Profile profile(int playerId) {
        if (!lock.isValid()) {
            throw new IllegalStateException("Local save store is closed.");
        }
        if (playerId < 1) {
            throw new IllegalArgumentException("Player ID must be positive.");
        }
        Profile cached = profiles.get(playerId);
        if (cached != null) {
            return cached;
        }
        Path file = file(playerId);
        try {
            // Unreadable or malformed progress must not silently become an empty game.
            if (Files.notExists(file)) {
                Profile initial = new Profile(0, 0, 0, Set.of("RedPlanet"), "RedPlanet");
                commit(playerId, initial);
                return initial;
            }
            Properties values = new Properties();
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                values.load(reader);
            }
            if (!"1".equals(values.getProperty("version"))) {
                throw new IllegalArgumentException("Unsupported or missing save version.");
            }
            int coins = number(values, "coins");
            int score = number(values, "score");
            int highScore = number(values, "highScore");
            if (highScore < score) {
                throw new IllegalArgumentException("High score is lower than saved score.");
            }
            Set<String> purchased = new HashSet<>();
            for (String name : values.getProperty("purchased", "").split(",")) {
                definition(name);
                purchased.add(name);
            }
            String equipped = values.getProperty("equipped", "");
            definition(equipped);
            if (!purchased.contains("RedPlanet") || !purchased.contains(equipped)) {
                throw new IllegalArgumentException("Missing starter or equipped skin ownership.");
            }
            Profile loaded = new Profile(coins, score, highScore, purchased, equipped);
            profiles.put(playerId, loaded);
            return loaded;
        } catch (IOException | IllegalArgumentException exception) {
            throw failure("Cannot read " + file.getFileName() + "; the file was preserved. Restore its .bak if needed", exception);
        }
    }

    private void commit(int playerId, Profile profile) {
        Properties values = new Properties();
        values.setProperty("version", "1");
        values.setProperty("coins", Integer.toString(profile.coins()));
        values.setProperty("score", Integer.toString(profile.score()));
        values.setProperty("highScore", Integer.toString(profile.highScore()));
        values.setProperty("purchased", String.join(",", CATALOGUE.stream().map(SkinDefinition::name)
                .filter(profile.purchased()::contains).toList()));
        values.setProperty("equipped", profile.equipped());
        Path temporary = null;
        try {
            temporary = Files.createTempFile(directory, "player-" + playerId + "-", ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                values.store(writer, "Pellet Panic local save");
            }
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            Path destination = file(playerId);
            if (Files.exists(destination)) {
                Files.copy(destination, directory.resolve(destination.getFileName() + ".bak"),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            profiles.put(playerId, profile);
        } catch (IOException exception) {
            throw failure("Cannot save progress; the in-memory balance was not changed", exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException exception) {
                    System.err.println("Could not remove temporary save: " + temporary);
                }
            }
        }
    }

    private Path file(int playerId) {
        return directory.resolve("player-" + playerId + ".properties");
    }

    private static int number(Properties values, String name) {
        int value = Integer.parseInt(values.getProperty(name, ""));
        requireNonNegative(value, name);
        return value;
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative.");
        }
    }

    private static SkinDefinition definition(String name) {
        return CATALOGUE.stream().filter(skin -> skin.name().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown skin: " + name));
    }

    private IllegalStateException failure(String message, Exception cause) {
        return new IllegalStateException(message + " (" + directory + "): " + cause.getMessage(), cause);
    }

    @Override
    public synchronized void close() throws IOException {
        try {
            lock.release();
        } finally {
            lockChannel.close();
        }
    }
}
