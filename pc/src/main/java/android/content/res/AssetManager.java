package android.content.res;

import java.io.IOException;
import java.io.InputStream;

public final class AssetManager {
    public InputStream open(String path) throws IOException {
        String normalized = path == null ? "" : path.replace('\\','/');
        InputStream in = AssetManager.class.getClassLoader().getResourceAsStream("assets/" + normalized);
        if (in == null) throw new IOException("Asset not found: " + normalized);
        return in;
    }

    public String[] list(String folder) throws IOException {
        // Audio is optional on the desktop compatibility layer. Exact assets are
        // still available through open(); directory enumeration can be empty.
        return new String[0];
    }

    public AssetFileDescriptor openFd(String path) throws IOException {
        return new AssetFileDescriptor(path);
    }
}
