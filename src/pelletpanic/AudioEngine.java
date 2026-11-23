package pelletpanic;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/** Ordered audio commands and settings I/O, always outside the Swing event thread. */
final class AudioEngine implements AutoCloseable {
    interface Backend {
        void play(Sound sound, float gain) throws Exception;
        void music(boolean playing, float gain) throws Exception;
        void stopEffects();
        void close();
    }

    private final Backend backend;
    private final Path settingsFile;
    private final LongSupplier clock;
    private final Map<Sound, Long> lastPlayed = new EnumMap<>(Sound.class);
    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(action -> {
        Thread thread = new Thread(action, "pellet-panic-audio");
        thread.setDaemon(true);
        return thread;
    });
    private volatile AudioSettings settings;
    private volatile boolean closed;
    private boolean available = true;
    private boolean musicWanted;

    AudioEngine(Backend backend, Path settingsFile, LongSupplier clock) {
        this.backend = backend;
        this.settingsFile = settingsFile;
        this.clock = clock;
        settings = AudioSettings.load(settingsFile);
    }

    AudioSettings settings() {
        return settings;
    }

    void addListener(Runnable listener) {
        listeners.add(listener);
    }

    void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    void play(Sound sound) {
        enqueue(() -> {
            if (!available || settings.muted() || settings.effects() == 0) {
                return;
            }
            long now = clock.getAsLong();
            Long previous = lastPlayed.get(sound);
            if (previous != null && now - previous < sound.cooldownNanos) {
                return;
            }
            lastPlayed.put(sound, now);
            attempt(() -> backend.play(sound, settings.effects() * sound.gain));
        });
    }

    void setMusicPlaying(boolean playing) {
        enqueue(() -> {
            if (musicWanted != playing) {
                musicWanted = playing;
                applyMusic();
            }
        });
    }

    void stopEffects() {
        enqueue(backend::stopEffects);
    }

    void toggleMuted() {
        enqueue(() -> update(new AudioSettings(!settings.muted(), settings.music(), settings.effects())));
    }

    void setVolumes(float music, float effects) {
        AudioSettings validated = new AudioSettings(false, music, effects);
        enqueue(() -> update(new AudioSettings(settings.muted(), validated.music(), validated.effects())));
    }

    private void update(AudioSettings next) {
        if (settings.equals(next)) {
            return;
        }
        settings = next;
        if (settings.muted() || settings.effects() == 0) {
            backend.stopEffects();
        }
        applyMusic();
        try {
            settings.save(settingsFile);
        } catch (IOException exception) {
            System.err.println("Audio preferences could not be saved: " + exception.getMessage());
        }
        listeners.forEach(Runnable::run);
    }

    private void applyMusic() {
        if (available) {
            attempt(() -> backend.music(musicWanted && !settings.muted() && settings.music() > 0, settings.music()));
        }
    }

    @FunctionalInterface
    private interface AudioOperation {
        void run() throws Exception;
    }

    private void attempt(AudioOperation operation) {
        try {
            operation.run();
        } catch (Exception exception) {
            available = false;
            backend.close();
            System.err.println("Audio is unavailable; the game will continue silently. " + exception.getMessage());
        }
    }

    private synchronized void enqueue(Runnable operation) {
        if (!closed) {
            worker.execute(operation);
        }
    }

    synchronized CompletableFuture<Void> idle() {
        return closed ? CompletableFuture.completedFuture(null) : CompletableFuture.runAsync(() -> { }, worker);
    }

    @Override
    public void close() {
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            worker.execute(backend::close);
            worker.shutdown();
        }
        try {
            if (!worker.awaitTermination(3, TimeUnit.SECONDS)) {
                worker.shutdownNow();
            }
        } catch (InterruptedException exception) {
            worker.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
