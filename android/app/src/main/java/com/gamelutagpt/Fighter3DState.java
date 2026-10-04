package com.gamelutagpt;

/**
 * Lock-free render snapshot shared between the combat SurfaceView and the
 * OpenGL fighter layer. All values are written by GameView and read by the GL
 * thread.
 */
final class Fighter3DState {
    volatile float playerX = 420f;
    volatile float playerY = 565f;
    volatile float visualOffsetX = 0f;

    volatile float cameraX = 700f;
    volatile float cameraTop = 72f;
    volatile float renderZoom = 1.12f;

    volatile float walkTime = 0f;
    volatile float attackPhase = 0f;
    volatile float knockdownAngle = 0f;

    volatile int facingDirection = 1;
    volatile int guardPose = 0;

    volatile boolean crouching = false;
    volatile boolean airborne = false;
    volatile boolean superPose = false;
    volatile boolean hitFlash = false;
    volatile boolean visible = true;

    volatile String attackType = "";
}
