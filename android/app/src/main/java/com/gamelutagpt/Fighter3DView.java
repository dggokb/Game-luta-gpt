package com.gamelutagpt;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;

/**
 * Transparent OpenGL surface used only for true 3D fighters.
 */
final class Fighter3DView extends GLSurfaceView {
    Fighter3DView(Context context, Fighter3DState state) {
        super(context);

        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setZOrderMediaOverlay(true);

        setPreserveEGLContextOnPause(true);
        setRenderer(new Fighter3DRenderer(state));
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        setFocusable(false);
        setClickable(true);
    }
}
