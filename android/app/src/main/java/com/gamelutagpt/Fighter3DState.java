package com.gamelutagpt;

/**
 * Coherent render snapshot shared between the combat SurfaceView and the
 * OpenGL fighter layer. Publication and copy hold this object monitor; GPU work uses its private copy.
 */
final class Fighter3DState {
    static final int GUARD_NONE = 0;
    static final int GUARD_HIGH = 1;
    static final int GUARD_LOW = 2;

    volatile float playerX = 420f;
    volatile float playerY = 565f;
    volatile float visualOffsetX = 0f;

    volatile float cameraX = 700f;
    volatile float cameraTop = 72f;
    volatile float renderZoom = 1.12f;

    float animationTime;
    float verticalSpeed;
    boolean dashing;
    volatile float walkTime = 0f;
    volatile float attackPhase = 0f;
    volatile float knockdownAngle = 0f;

    volatile int facingDirection = 1;
    volatile int guardPose = 0;

    volatile boolean crouching = false;
    volatile boolean airborne = false;
    volatile boolean superJumping = false;
    volatile boolean superPose = false;
    volatile boolean hitFlash = false;
    volatile boolean visible = true;

    volatile String attackType = "";
    synchronized void copyTo(Fighter3DState target) {
        target.playerX = playerX;
        target.playerY = playerY;
        target.visualOffsetX = visualOffsetX;
        target.cameraX = cameraX;
        target.cameraTop = cameraTop;
        target.renderZoom = renderZoom;
        target.animationTime = animationTime;
        target.verticalSpeed = verticalSpeed;
        target.dashing = dashing;
        target.walkTime = walkTime;
        target.attackPhase = attackPhase;
        target.knockdownAngle = knockdownAngle;
        target.facingDirection = facingDirection;
        target.guardPose = guardPose;
        target.crouching = crouching;
        target.airborne = airborne;
        target.superJumping = superJumping;
        target.superPose = superPose;
        target.hitFlash = hitFlash;
        target.visible = visible;
        target.attackType = attackType;
    }
}
