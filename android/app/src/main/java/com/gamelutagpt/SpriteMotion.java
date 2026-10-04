package com.gamelutagpt;

/** Simulation-owned animation clock. Drawing never advances an animation. */
final class SpriteMotion {
    enum Clip { IDLE, WALK_FORWARD, WALK_BACK, DASH, BACKDASH, CROUCH, RISE, JUMP, FALL, LAND, LIGHT_JAB, COMBAT }
    Clip clip = Clip.IDLE;
    float time;
    float distance;
    private boolean wasGrounded = true;

    void update(float dt, boolean grounded, boolean crouching, float velocityY,
                float travel, boolean forward, boolean dash, boolean backdash,
                boolean lightJab, boolean combat, boolean locked) {
        dt = Math.max(0, Math.min(.1f, dt));
        Clip next;
        if (!grounded) next = velocityY < -35 ? Clip.JUMP : Clip.FALL;
        else if (crouching) next = Clip.CROUCH;
        else if (lightJab) next = Clip.LIGHT_JAB;
        else if (combat || locked) next = Clip.COMBAT;
        else if (!wasGrounded || (clip == Clip.LAND && time < .10f)) next = Clip.LAND;
        else if (backdash && Math.abs(travel) > .001f) next = Clip.BACKDASH;
        else if (Math.abs(travel) > .001f) next = dash && forward ? Clip.DASH : forward ? Clip.WALK_FORWARD : Clip.WALK_BACK;
        else if (clip == Clip.CROUCH || (clip == Clip.RISE && time < .075f)) next = Clip.RISE;
        else next = Clip.IDLE;
        wasGrounded = grounded;
        if (next != clip) { clip = next; time = 0; distance = 0; }
        time += dt;
        distance += Math.abs(travel);
    }

    boolean usesIdleSheet() { return clip == Clip.IDLE || clip == Clip.COMBAT; }
    int frame() {
        switch (clip) {
            case WALK_FORWARD: return ((int)(distance / 36f)) % 4;
            case WALK_BACK: return 4 + ((int)(distance / 32f)) % 4;
            case CROUCH: return time < .075f ? 8 : 9;
            case RISE: return 8;
            case JUMP: return 10;
            case FALL: return 11;
            case DASH: return 12 + ((int)(time / .10f) % 2);
            case BACKDASH: return 14;
            case LAND: return 15;
            case LIGHT_JAB:
                if (time < .040f) return 0;
                if (time < .100f) return 1;
                return 2;
            case IDLE: return ((int)(time / .12f)) % 8;
            default: return 0;
        }
    }
}
