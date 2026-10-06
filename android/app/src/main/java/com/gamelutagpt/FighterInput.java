package com.gamelutagpt;

/**
 * Everything one fighter asks for on one simulation frame. A human pad fills the held
 * direction and the buttons pressed since the previous frame; the CPU also uses the
 * explicit dash/jump/special requests. The engine never reads touch events or clocks.
 */
final class FighterInput {
    /** Screen direction 0..8: 0 neutral, then → ↘ ↓ ↙ ← ↖ ↑ ↗ (see ControlsLayout). */
    int direction;
    boolean light, medium, heavy;
    /** Auto-combo button: the next step of the character's declared autoCombo route. */
    boolean auto;
    boolean superAttack;
    /** CPU only: an already-resolved special of strength "L", "M" or "H". */
    String special;
    /** CPU only: requests that a human expresses through the direction history. */
    boolean dash, backdash, jump, superJump;

    void clear() {
        direction = 0;
        clearPresses();
    }

    void clearPresses() {
        light = medium = heavy = auto = superAttack = false;
        special = null;
        dash = backdash = jump = superJump = false;
    }

    void copyFrom(FighterInput other) {
        direction = other.direction;
        light = other.light;
        medium = other.medium;
        heavy = other.heavy;
        auto = other.auto;
        superAttack = other.superAttack;
        special = other.special;
        dash = other.dash;
        backdash = other.backdash;
        jump = other.jump;
        superJump = other.superJump;
    }
}
