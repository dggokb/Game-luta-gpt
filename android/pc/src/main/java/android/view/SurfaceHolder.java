package android.view;

import android.graphics.Canvas;

public interface SurfaceHolder {
    interface Callback {
        void surfaceCreated(SurfaceHolder holder);
        void surfaceChanged(SurfaceHolder holder, int format, int width, int height);
        void surfaceDestroyed(SurfaceHolder holder);
    }

    void addCallback(Callback callback);
    void removeCallback(Callback callback);
    Surface getSurface();
    Canvas lockCanvas();
    Canvas lockHardwareCanvas();
    void unlockCanvasAndPost(Canvas canvas);
}
