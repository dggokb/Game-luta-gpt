package android.app;

/** PC: a "memória do aparelho" é o heap da JVM (onde os bitmaps ficam no PC). */
public class ActivityManager {
    public static class MemoryInfo {
        public long availMem;
        public long totalMem;
        public long threshold;
        public boolean lowMemory;
    }

    public void getMemoryInfo(MemoryInfo out) {
        Runtime r = Runtime.getRuntime();
        out.totalMem = r.maxMemory();
        out.availMem = r.maxMemory() - (r.totalMemory() - r.freeMemory());
        out.threshold = out.totalMem / 10;
        out.lowMemory = out.availMem < out.threshold;
    }

    public boolean isLowRamDevice() { return false; }
}
