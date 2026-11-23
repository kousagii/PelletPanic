package pelletpanic;

import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import java.util.Set;

/** Application audio facade. Headless tests never open devices or real preferences. */
final class GameAudio {
    private static AudioEngine engine;
    private static final AudioSettings SILENT = new AudioSettings(true, 0, 0);

    private GameAudio() {
    }

    private static boolean disabled() {
        return Boolean.getBoolean("pelletpanic.audio.disabled") || GraphicsEnvironment.isHeadless();
    }

    private static synchronized AudioEngine engine() {
        if (engine == null) {
            Path settings = Path.of(System.getProperty("pelletpanic.saveDir", "data"), "audio.properties");
            engine = new AudioEngine(new JavaSoundBackend(), settings, System::nanoTime);
            Runtime.getRuntime().addShutdownHook(new Thread(engine::close, "pellet-panic-audio-close"));
        }
        return engine;
    }

    static void initialize() {
        if (!disabled()) {
            engine();
        }
    }

    static void play(Sound sound) {
        if (!disabled()) {
            engine().play(sound);
        }
    }

    static void events(Set<GameEvent> events) {
        if (events.contains(GameEvent.GAME_OVER)) {
            setMusicPlaying(false);
        }
        for (GameEvent event : events) {
            if (event == GameEvent.HIT && events.contains(GameEvent.GAME_OVER)) {
                continue;
            }
            play(Sound.valueOf(event.name()));
        }
    }

    static void setMusicPlaying(boolean playing) {
        if (!disabled()) {
            engine().setMusicPlaying(playing);
        }
    }

    static void pauseGameplay() {
        if (!disabled()) {
            engine().setMusicPlaying(false);
            engine().stopEffects();
        }
    }

    static AudioSettings settings() {
        return disabled() ? SILENT : engine().settings();
    }

    static void toggleMuted() {
        if (!disabled()) {
            engine().toggleMuted();
        }
    }

    static void setVolumes(float music, float effects) {
        if (!disabled()) {
            engine().setVolumes(music, effects);
        }
    }

    static void addListener(Runnable listener) {
        if (!disabled()) {
            engine().addListener(listener);
        }
    }

    static void removeListener(Runnable listener) {
        if (!disabled()) {
            engine().removeListener(listener);
        }
    }
}
