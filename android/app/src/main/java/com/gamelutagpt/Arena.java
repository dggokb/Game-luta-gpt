package com.gamelutagpt;

/** World and virtual-screen dimensions shared by simulation and rendering. */
final class Arena {
    private Arena() {}

    /** Virtual screen; the canvas is scaled from this to the real surface. */
    static final float VW = 1280f;
    static final float VH = 720f;
    static final float GROUND_Y = 565f;
    static final float WORLD_WIDTH = 2600f;
    static final float WORLD_TOP = -520f;
    static final float LEFT_BOUND = 90f;
    static final float RIGHT_BOUND = WORLD_WIDTH - 90f;

    static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
