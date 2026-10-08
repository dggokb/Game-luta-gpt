package android.content.res;

public final class AssetFileDescriptor implements AutoCloseable {
    private final String path;
    AssetFileDescriptor(String path){ this.path=path; }
    public String path(){ return path; }
    @Override public void close(){}
}
