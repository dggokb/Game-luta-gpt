package android.media;
import android.content.res.AssetFileDescriptor;
public final class SoundPool {
  public static final class Builder {
    public Builder setMaxStreams(int value){return this;}
    public Builder setAudioAttributes(AudioAttributes value){return this;}
    public SoundPool build(){return new SoundPool();}
  }
  public int load(AssetFileDescriptor value,int priority){return 0;}
  public int play(int id,float left,float right,int priority,int loop,float rate){return 0;}
  public void release(){}
}