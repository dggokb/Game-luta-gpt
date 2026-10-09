package android.content.res;
import java.io.*;
public final class AssetFileDescriptor implements AutoCloseable {
  private final InputStream stream;
  public AssetFileDescriptor(InputStream stream){this.stream=stream;}
  public InputStream createInputStream(){return stream;}
  public void close() throws IOException {stream.close();}
}