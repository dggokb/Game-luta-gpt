package android.util;

/** PC: o log do Android vai para o terminal (stderr). */
public final class Log {
    public static final int VERBOSE = 2, DEBUG = 3, INFO = 4, WARN = 5, ERROR = 6;
    /** Mínimo impresso; -Dgameluta.log=debug mostra tudo. */
    private static final int MIN = "debug".equals(System.getProperty("gameluta.log")) ? VERBOSE : INFO;

    private Log() {}

    public static int v(String tag, String msg) { return print(VERBOSE, tag, msg, null); }
    public static int d(String tag, String msg) { return print(DEBUG, tag, msg, null); }
    public static int i(String tag, String msg) { return print(INFO, tag, msg, null); }
    public static int w(String tag, String msg) { return print(WARN, tag, msg, null); }
    public static int e(String tag, String msg) { return print(ERROR, tag, msg, null); }
    public static int d(String tag, String msg, Throwable tr) { return print(DEBUG, tag, msg, tr); }
    public static int i(String tag, String msg, Throwable tr) { return print(INFO, tag, msg, tr); }
    public static int w(String tag, String msg, Throwable tr) { return print(WARN, tag, msg, tr); }
    public static int w(String tag, Throwable tr) { return print(WARN, tag, "", tr); }
    public static int e(String tag, String msg, Throwable tr) { return print(ERROR, tag, msg, tr); }

    private static int print(int level, String tag, String msg, Throwable tr) {
        if (level < MIN) return 0;
        String name = "VDIWE".substring(level - 2, level - 1);
        StringBuilder line = new StringBuilder().append(name).append('/').append(tag).append(": ").append(msg);
        if (tr != null) line.append(" (").append(tr).append(')');
        System.err.println(line);
        return line.length();
    }
}
