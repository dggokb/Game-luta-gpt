package android.media;

/** PC: guardado só por compatibilidade; o som sai pelo mixer padrão. */
public final class AudioAttributes {
    public static final int USAGE_GAME = 14;
    public static final int USAGE_MEDIA = 1;
    public static final int CONTENT_TYPE_SONIFICATION = 4;
    public static final int CONTENT_TYPE_MUSIC = 2;

    private AudioAttributes() {}

    public static final class Builder {
        public Builder setUsage(int usage) { return this; }
        public Builder setContentType(int contentType) { return this; }
        public AudioAttributes build() { return new AudioAttributes(); }
    }
}
