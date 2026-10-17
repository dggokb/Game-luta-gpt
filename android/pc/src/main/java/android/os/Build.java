package android.os;

/** PC: comporta-se como um Android 9 (o GameView pede o Canvas "pela GPU", que aqui é o Java2D). */
public final class Build {
    public static final String MODEL = "PC";
    public static final String MANUFACTURER = System.getProperty("os.name", "PC");

    private Build() {}

    public static final class VERSION {
        public static final int SDK_INT = 28;
        public static final String RELEASE = "9";
        private VERSION() {}
    }

    public static final class VERSION_CODES {
        public static final int N = 24, N_MR1 = 25, O = 26, O_MR1 = 27, P = 28, Q = 29;
        private VERSION_CODES() {}
    }
}
