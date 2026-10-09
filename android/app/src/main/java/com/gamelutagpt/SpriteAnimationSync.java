package com.gamelutagpt;

/** Deterministic, physics-authoritative sprite pacing. Never alters game collisions. */
final class SpriteAnimationSync {
    private SpriteAnimationSync() {}
    static float travelDistance(float previous,float actualDelta) {
        if (!Float.isFinite(previous) || !Float.isFinite(actualDelta)) return 0f;
        return Math.max(0f,previous)+Math.abs(actualDelta);
    }
    /** Ensure animation reaches its ascent pose when physics approaches the apex. */
    static float ascentClock(float clock,float vy,float takeoffSpeed,float duration) {
        if (takeoffSpeed<=35f || duration<=0f || !Float.isFinite(vy)) return clock;
        float phase=1f-Math.max(0f,Math.min(1f,-vy/takeoffSpeed));
        return Math.max(clock,Math.max(0f,phase*duration-.00001f));
    }
}
