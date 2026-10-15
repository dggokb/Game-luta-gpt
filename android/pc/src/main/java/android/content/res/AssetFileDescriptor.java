package android.content.res;

import java.io.Closeable;
import java.io.File;

/** PC: aponta para o arquivo do asset no disco (o SoundPool lê dali). */
public class AssetFileDescriptor implements Closeable {
    private final File file;

    public AssetFileDescriptor(File file) { this.file = file; }

    /** Extensão do PC: o arquivo real. */
    public File file() { return file; }

    public long getLength() { return file.length(); }

    public long getStartOffset() { return 0L; }

    @Override public void close() {}
}
