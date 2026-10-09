package android.content.res;
import java.io.*;
public final class AssetManager {
  public InputStream open(String path) throws IOException {
    InputStream in=AssetManager.class.getClassLoader().getResourceAsStream(path);
    if(in==null) throw new FileNotFoundException("Game asset not present: "+path);
    return in;
  }
  public AssetFileDescriptor openFd(String path) throws IOException {
    return new AssetFileDescriptor(open(path));
  }
  public String[] list(String path) throws IOException {
    // Audio asset indexing is optional for this PC prototype; art JSON/PNG is
    // loaded explicitly via open() and is fully functional.
    return new String[0];
  }
}