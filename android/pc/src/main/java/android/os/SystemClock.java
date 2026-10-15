package android.os;

/** PC: relógio monotônico, como o do Android. */
public final class SystemClock {
    private static final long START = System.nanoTime();

    private SystemClock() {}

    public static long uptimeMillis() { return (System.nanoTime() - START) / 1_000_000L; }

    public static long elapsedRealtime() { return uptimeMillis(); }

    public static long elapsedRealtimeNanos() { return System.nanoTime() - START; }
}
