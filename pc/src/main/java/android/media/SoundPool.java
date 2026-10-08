package android.media;

import android.content.res.AssetFileDescriptor;
import java.util.concurrent.atomic.AtomicInteger;

public final class SoundPool {
    private final AtomicInteger ids=new AtomicInteger(1);
    public static final class Builder {
        public Builder setMaxStreams(int streams){ return this; }
        public Builder setAudioAttributes(AudioAttributes attributes){ return this; }
        public SoundPool build(){ return new SoundPool(); }
    }
    public int load(AssetFileDescriptor descriptor,int priority){ return ids.getAndIncrement(); }
    public int play(int soundId,float left,float right,int priority,int loop,float rate){ return soundId; }
    public void release(){}
}
