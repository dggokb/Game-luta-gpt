package android.media;

import android.content.res.AssetFileDescriptor;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

/**
 * PC: efeitos curtos com o javax.sound. O Java toca WAV (PCM) sem bibliotecas; OGG e MP3
 * são avisados uma vez e ignorados, como um som que não existe.
 */
public class SoundPool {
    private static final String TAG = "SoundPool";

    private static final class Sound {
        final AudioFormat format;
        final byte[] data;

        Sound(AudioFormat format, byte[] data) {
            this.format = format;
            this.data = data;
        }
    }

    private final int maxStreams;
    private final List<Sound> sounds = new ArrayList<>();
    private final List<Clip> playing = new ArrayList<>();
    private boolean warnedFormat;
    private final boolean enabled = !Boolean.getBoolean("gameluta.semSom");

    SoundPool(int maxStreams) {
        this.maxStreams = Math.max(1, maxStreams);
    }

    public static class Builder {
        private int maxStreams = 1;

        public Builder setMaxStreams(int maxStreams) { this.maxStreams = maxStreams; return this; }
        public Builder setAudioAttributes(AudioAttributes attributes) { return this; }
        public SoundPool build() { return new SoundPool(maxStreams); }
    }

    /** Devolve o id do som (a partir de 1), ou 0 quando o formato não toca no PC. */
    public synchronized int load(AssetFileDescriptor descriptor, int priority) {
        File file = descriptor.file();
        String name = file.getName().toLowerCase(java.util.Locale.ROOT);
        if (!name.endsWith(".wav")) {
            if (!warnedFormat) {
                warnedFormat = true;
                Log.w(TAG, "no PC só WAV toca; ignorando " + file.getName() + " (e outros OGG/MP3)");
            }
            return 0;
        }
        try (InputStream raw = new BufferedInputStream(Files.newInputStream(file.toPath()));
             AudioInputStream in = AudioSystem.getAudioInputStream(raw)) {
            byte[] data = in.readAllBytes();
            sounds.add(new Sound(in.getFormat(), data));
            return sounds.size();
        } catch (Exception e) {
            Log.w(TAG, "não carregou " + file, e);
            return 0;
        }
    }

    public synchronized int play(int soundId, float leftVolume, float rightVolume, int priority, int loop, float rate) {
        if (!enabled || soundId <= 0 || soundId > sounds.size()) return 0;
        playing.removeIf(c -> {
            if (c.isActive()) return false;
            c.close();
            return true;
        });
        if (playing.size() >= maxStreams) {
            Clip oldest = playing.remove(0);
            oldest.stop();
            oldest.close();
        }
        Sound sound = sounds.get(soundId - 1);
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(sound.format, sound.data, 0, sound.data.length);
            float volume = Math.max(leftVolume, rightVolume);
            if (volume < 1f && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gain.setValue(Math.max(gain.getMinimum(), 20f * (float) Math.log10(Math.max(volume, 0.0001f))));
            }
            if (loop != 0) clip.loop(loop < 0 ? Clip.LOOP_CONTINUOUSLY : loop);
            else clip.start();
            playing.add(clip);
            return playing.size();
        } catch (Exception e) {
            // Sem placa de som (servidor, CI): segue em silêncio.
            Log.d(TAG, "sem saída de áudio: " + e);
            return 0;
        }
    }

    public synchronized void release() {
        for (Clip c : playing) c.close();
        playing.clear();
        sounds.clear();
    }
}
