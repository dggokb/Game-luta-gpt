package android.view;

public class Surface {
    private volatile boolean valid;

    public boolean isValid() { return valid; }

    /** Extensão do PC. */
    public void setValid(boolean valid) { this.valid = valid; }
}
