package android.view;

import android.content.Context;

/** O mínimo de View que o jogo usa; o tamanho vem da janela do PC. */
public class View {
    private final Context context;
    private volatile int width, height;
    private boolean focusable, keepScreenOn;

    public View(Context context) { this.context = context; }

    public final Context getContext() { return context; }

    public final int getWidth() { return width; }

    public final int getHeight() { return height; }

    /** Extensão do PC: a janela mudou de tamanho. */
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setFocusable(boolean focusable) { this.focusable = focusable; }

    public boolean isFocusable() { return focusable; }

    public void setKeepScreenOn(boolean keepScreenOn) { this.keepScreenOn = keepScreenOn; }

    public boolean getKeepScreenOn() { return keepScreenOn; }

    public boolean onTouchEvent(MotionEvent event) { return false; }

    public boolean onKeyDown(int keyCode, KeyEvent event) { return false; }

    public boolean onKeyUp(int keyCode, KeyEvent event) { return false; }

    public void invalidate() {}

    /** PC: a tela de 60 Hz. */
    public Display getDisplay() { return Display.PADRAO; }

    /** No PC a janela avisa por setSize; fica para quem sobrescreve (o celular usa). */
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {}

    /** No PC não há gesto de sistema na borda da tela. */
    public void setSystemGestureExclusionRects(java.util.List<android.graphics.Rect> rects) {}

    public boolean post(Runnable action) {
        java.awt.EventQueue.invokeLater(action);
        return true;
    }
}
