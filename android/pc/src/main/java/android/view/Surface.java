package android.view;

public class Surface {
    private volatile boolean valid;

    public static final int FRAME_RATE_COMPATIBILITY_DEFAULT = 0, FRAME_RATE_COMPATIBILITY_FIXED_SOURCE = 1;

    public boolean isValid() { return valid; }

    public void setFrameRate(float frameRate, int compatibility) {}

    /** Extensão do PC. */
    public void setValid(boolean valid) { this.valid = valid; }
}
