package android.os;

/** PC: as prioridades de thread do Android não fazem nada aqui. */
public final class Process {
    public static final int THREAD_PRIORITY_DEFAULT = 0;
    public static final int THREAD_PRIORITY_BACKGROUND = 10;
    public static final int THREAD_PRIORITY_DISPLAY = -4;

    private Process() {}

    public static void setThreadPriority(int priority) {}
}
