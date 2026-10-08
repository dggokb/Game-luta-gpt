package android.media;

public final class AudioAttributes {
    public static final int USAGE_GAME=14;
    public static final int CONTENT_TYPE_SONIFICATION=4;
    public static final class Builder {
        public Builder setUsage(int usage){ return this; }
        public Builder setContentType(int type){ return this; }
        public AudioAttributes build(){ return new AudioAttributes(); }
    }
}
