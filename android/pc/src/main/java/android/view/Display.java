package android.view;

/** PC: uma tela de 60 Hz. */
public class Display {
    static final Display PADRAO = new Display();

    public float getRefreshRate() { return 60f; }
}
