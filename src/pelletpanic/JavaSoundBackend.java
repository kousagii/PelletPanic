package pelletpanic;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

/** Standard Java Sound only; shipped PCM WAV files need no codec or native dependency. */
final class JavaSoundBackend implements AudioEngine.Backend {
    static final String MUSIC = "/Audio/outer-space.wav";
    private final Map<Sound, Voice> effects = new EnumMap<>(Sound.class);
    private Voice music;

    @Override
    public void play(Sound sound, float gain) throws Exception {
        Voice voice = effects.get(sound);
        if (voice == null) {
            voice = new Voice("/Audio/" + sound.file, gain, false);
            effects.put(sound, voice);
        }
        voice.volume(gain);
        voice.clip.stop();
        voice.clip.setFramePosition(0);
        voice.clip.start();
    }

    @Override
    public void music(boolean playing, float gain) throws Exception {
        if (music == null && !playing) {
            return;
        }
        if (music == null) {
            music = new Voice(MUSIC, gain, true);
        }
        music.volume(gain);
        if (playing && !music.clip.isRunning()) {
            if (music.clip.getFramePosition() >= music.clip.getFrameLength()) {
                music.clip.setFramePosition(0);
            }
            music.clip.loop(Clip.LOOP_CONTINUOUSLY);
        } else if (!playing) {
            music.clip.stop(); // Keep the frame position for pause/resume and mute.
        }
    }

    @Override
    public void stopEffects() {
        effects.values().forEach(voice -> voice.clip.stop());
    }

    @Override
    public void close() {
        effects.values().forEach(Voice::close);
        effects.clear();
        if (music != null) {
            music.close();
            music = null;
        }
    }

    private static final class Voice {
        private final AudioFormat format;
        private final byte[] samples;
        private Clip clip;
        private float gain;
        private final boolean looping;

        Voice(String path, float gain, boolean looping) throws Exception {
            this.looping = looping;
            var resource = JavaSoundBackend.class.getResourceAsStream(path);
            if (resource == null) {
                throw new IOException("Missing sound: " + path);
            }
            try (var buffered = new BufferedInputStream(resource);
                    var input = AudioSystem.getAudioInputStream(buffered)) {
                format = input.getFormat();
                if (format.getSampleSizeInBits() != 16 || format.isBigEndian()
                        || !AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding())) {
                    throw new IOException("Expected signed 16-bit little-endian PCM: " + path);
                }
                samples = input.readAllBytes();
            }
            this.gain = gain;
            open(gain);
        }

        private void open(float volume) throws Exception {
            Clip candidate = AudioSystem.getClip();
            try {
                // Some mixers expose controls only after opening. Open once, then
                // choose hardware gain or a software-scaled PCM fallback.
                candidate.open(format, samples, 0, samples.length);
                boolean hardwareGain = candidate.isControlSupported(FloatControl.Type.MASTER_GAIN);
                if (hardwareGain) {
                    clip = candidate;
                    applyHardwareGain(volume);
                } else {
                    candidate.close();
                    byte[] scaled = scaledSamples(samples, volume);
                    candidate = AudioSystem.getClip();
                    candidate.open(format, scaled, 0, scaled.length);
                    clip = candidate;
                }
            } catch (Exception exception) {
                candidate.close();
                throw exception;
            }
        }

        void volume(float next) throws Exception {
            if (Float.compare(next, gain) == 0) {
                return;
            }
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                applyHardwareGain(next);
            } else {
                int position = clip.getFramePosition();
                boolean playing = clip.isRunning();
                clip.close();
                open(next);
                clip.setFramePosition(Math.min(position, clip.getFrameLength()));
                if (playing) {
                    if (looping) {
                        clip.loop(Clip.LOOP_CONTINUOUSLY);
                    } else {
                        clip.start();
                    }
                }
            }
            gain = next;
        }

        private void applyHardwareGain(float volume) {
            FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float decibels = volume <= 0 ? control.getMinimum() : (float) (20 * Math.log10(volume));
            control.setValue(Math.max(control.getMinimum(), Math.min(control.getMaximum(), decibels)));
        }

        void close() {
            if (clip != null) {
                clip.close();
            }
        }
    }

    static byte[] scaledSamples(byte[] source, float volume) {
        byte[] result = source.clone();
        for (int index = 0; index + 1 < result.length; index += 2) {
            short sample = (short) ((source[index] & 0xFF) | (source[index + 1] << 8));
            int scaled = Math.round(sample * volume);
            result[index] = (byte) scaled;
            result[index + 1] = (byte) (scaled >> 8);
        }
        return result;
    }
}
