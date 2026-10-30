package com.gamelutagpt;

/**
 * Everything one fighter asks for on one simulation frame. A human pad fills the held
 * direction and the buttons pressed since the previous frame; the CPU also uses the
 * explicit dash/jump/special requests. The engine never reads touch events or clocks.
 */
final class FighterInput {
    /** Screen direction 0..8: 0 neutral, then → ↘ ↓ ↙ ← ↖ ↑ ↗ (see ControlsLayout). */
    int direction;
    static final int PASSOU_MAX = 16;
    /**
     * Direções por onde o direcional passou desde o quadro anterior, antes de {@link #direction}
     * (o toque é mais rápido que os quadros). O motor põe todas no histórico dos comandos.
     */
    final int[] passou = new int[PASSOU_MAX];
    int passouCount;
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
    /** M + H (or the spot between them): pushblock while blocking, otherwise a heavy. */
    boolean pushblock;
    /** TAG: calls the partner as an assist, or turns the assist on screen into a tag. */
    boolean assist;
    /** ↓ + TAG: raw tag (the partner takes the point). */
    boolean tag;
    /** Botão SHINKAI (ou SUPER + TAG no mesmo quadro): ativa o SHINKAI com a barra cheia. */
    boolean shinkai;
    /** CPU only: an already-resolved special of strength "L", "M" or "H". */
    String special;
    /** CPU only: requests that a human expresses through the direction history. */
    boolean dash, backdash, jump, superJump;
    /** An attack button (L, M or H) is still held down (charges a chargeable special). */
    boolean holding;

    void clear() {
        direction = 0;
        passouCount = 0;
        holding = false;
        clearPresses();
    }

    void clearPresses() {
        light = medium = heavy = auto = superAttack = ultra = assist = tag = grab = pushblock = shinkai = false;
        special = null;
        dash = backdash = jump = superJump = false;
    }

    void copyFrom(FighterInput other) {
        direction = other.direction;
        passouCount = other.passouCount;
        System.arraycopy(other.passou, 0, passou, 0, other.passouCount);
        light = other.light;
        medium = other.medium;
        heavy = other.heavy;
        auto = other.auto;
        superAttack = other.superAttack;
        ultra = other.ultra;
        assist = other.assist;
        grab = other.grab;
        pushblock = other.pushblock;
        tag = other.tag;
        shinkai = other.shinkai;
        special = other.special;
        dash = other.dash;
        backdash = other.backdash;
        jump = other.jump;
        superJump = other.superJump;
        holding = other.holding;
    }
}
