package com.gamelutagpt;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Transparent real-3D layer aligned to the existing fight camera.
 */
final class Fighter3DRenderer implements GLSurfaceView.Renderer {
    private static final float VW = 1280f;
    private static final float VH = 720f;
    private static final float WORLD_WIDTH = 2600f;

    private final Fighter3DState shared;
    private final Fighter3DState state = new Fighter3DState();
    private final FighterPrototype01Rig rig =
        new FighterPrototype01Rig();
    private final FighterPrototype01Animator animator =
        new FighterPrototype01Animator();
    private final FighterPrototype01Model model =
        new FighterPrototype01Model();

    private Toon3DProgram program;

    private final float[] projection = new float[16];
    private final float[] root = new float[16];

    Fighter3DRenderer(Fighter3DState state) {
        this.shared = state;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0f, 0f, 0f, 0f);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(
            GLES20.GL_SRC_ALPHA,
            GLES20.GL_ONE_MINUS_SRC_ALPHA
        );

        program = new Toon3DProgram();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(
            GLES20.GL_COLOR_BUFFER_BIT |
            GLES20.GL_DEPTH_BUFFER_BIT
        );

        shared.copyTo(state);
        if (!state.visible || program == null) return;

        float zoom = Math.max(0.01f, state.renderZoom);
        float visibleWorldWidth = VW / zoom;
        float halfVisible = visibleWorldWidth * 0.5f;

        float cameraLeft = clamp(
            state.cameraX - halfVisible,
            0f,
            WORLD_WIDTH - visibleWorldWidth
        );

        float viewHeight = VH / zoom;

        // bottom > top intentionally matches Android Canvas' downward Y axis.
        Matrix.orthoM(
            projection,
            0,
            cameraLeft,
            cameraLeft + visibleWorldWidth,
            state.cameraTop + viewHeight,
            state.cameraTop,
            -1000f,
            1000f
        );

        animator.apply(state, rig);

        Matrix.setIdentityM(root, 0);
        Matrix.translateM(
            root,
            0,
            state.playerX + state.visualOffsetX,
            state.playerY,
            0f
        );

        // The concept is shown at a subtle 3/4 angle so the real mesh volume
        // remains readable while gameplay stays on a 2D fighting plane.
        float yaw = 25f;
        // Mirror the fighting stance without showing the back of the face.
        Matrix.scaleM(root, 0, state.facingDirection >= 0 ? 1f : -1f, 1f, 1f);
        GLES20.glFrontFace(state.facingDirection >= 0 ? GLES20.GL_CW : GLES20.GL_CCW);
        Matrix.rotateM(root, 0, yaw, 0f, 1f, 0f);

        if (state.knockdownAngle != 0f) {
            Matrix.rotateM(
                root,
                0,
                state.knockdownAngle,
                0f,
                0f,
                1f
            );
        }

        model.draw(
            program,
            rig,
            root,
            projection,
            state.hitFlash
        );
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
