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
    /**
     * SUPER pressed while holding down. The engine starts an ultra when there are three
     * bars; otherwise the press counts as a plain SUPER.
     */
    boolean ultra;
    /** L + M (or the spot between them): throw, or tech the opponent's throw. */
    boolean grab;
    /** TAG: calls the partner as an assist, or turns the assist on screen into a tag. */
    boolean assist;
    /** ↓ + TAG: raw tag (the partner takes the point). */
    boolean tag;
    /** CPU only: an already-resolved special of strength "L", "M" or "H". */
    String special;
    /** CPU only: requests that a human expresses through the direction history. */
    boolean dash, backdash, jump, superJump;

    void clear() {
        direction = 0;
        clearPresses();
    }

    void clearPresses() {
        light = medium = heavy = auto = superAttack = ultra = assist = tag = grab = false;
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
        ultra = other.ultra;
        assist = other.assist;
        grab = other.grab;
        tag = other.tag;
        special = other.special;
        dash = other.dash;
        backdash = other.backdash;
        jump = other.jump;
        superJump = other.superJump;
    }
}
