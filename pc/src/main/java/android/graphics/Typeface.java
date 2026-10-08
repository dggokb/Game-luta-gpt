package android.graphics;

public final class Typeface {
    public static final Typeface DEFAULT_BOLD=new Typeface(true);
    final boolean bold;
    private Typeface(boolean bold){ this.bold=bold; }
}
