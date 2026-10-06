package com.gamelutagpt;

/**
 * Touch controls collapsed into per-frame engine input: the held D-pad direction plus the
 * buttons pressed since the previous simulation frame. A press is never lost between
 * frames and is delivered exactly once.
 */
final class PadInput {
    enum Button { LIGHT, MEDIUM, HEAVY, AUTO, SUPER }

    private int direction;
    private boolean light, medium, heavy, auto, superAttack;

    int direction() {
        return direction;
    }

    void setDirection(int screenDirection) {
        direction = screenDirection;
    }

    void press(Button button) {
        switch (button) {
            case LIGHT: light = true; break;
            case MEDIUM: medium = true; break;
            case HEAVY: heavy = true; break;
            case AUTO: auto = true; break;
            case SUPER: superAttack = true; break;
            default: break;
        }
    }

    /** Writes this frame's input and forgets the delivered presses. */
    void drainInto(FighterInput out) {
        out.clear();
        out.direction = direction;
        out.light = light;
        out.medium = medium;
        out.heavy = heavy;
        out.auto = auto;
        out.superAttack = superAttack;
        // ↓ + SUPER asks for the ultra; the engine falls back to SUPER without three bars.
        out.ultra = superAttack && ControlsLayout.isDownDirection(direction);
        light = medium = heavy = auto = superAttack = false;
    }

    void reset() {
        direction = 0;
        light = medium = heavy = auto = superAttack = false;
    }
}
