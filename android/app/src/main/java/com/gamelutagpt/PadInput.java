package com.gamelutagpt;

/**
 * Touch controls collapsed into per-frame engine input: the held D-pad direction plus the
 * buttons pressed since the previous simulation frame. A press is never lost between
 * frames and is delivered exactly once.
 */
final class PadInput {
    enum Button { LIGHT, MEDIUM, HEAVY, AUTO, SUPER, TAG, THROW, PUSHBLOCK, OVERDRIVE }

    private int direction;
    private boolean light, medium, heavy, auto, superAttack, tag, grab, pushblock, overdrive;
    /** Frames since L or M was delivered alone, or -1: two fingers rarely land on one frame. */
    private int lightAge = -1, mediumAge = -1;
    /** L and M pressed this many frames apart still make a throw. */
    static final int THROW_LENIENCY_FRAMES = 2;

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
            case TAG: tag = true; break;
            case THROW: grab = true; break;
            case PUSHBLOCK: pushblock = true; break;
            case OVERDRIVE: overdrive = true; break;
            default: break;
        }
    }

    /** Writes this frame's input and forgets the delivered presses. */
    void drainInto(FighterInput out) {
        out.clear();
        out.direction = direction;
        // L + M (two fingers, up to THROW_LENIENCY_FRAMES apart) or the spot between them:
        // throw. When the first button already started a jab, the engine turns it into the throw.
        boolean late = (light && mediumAge >= 0) || (medium && lightAge >= 0);
        out.grab = grab || (light && medium) || late;
        if (out.grab) {
            light = medium = false;
            lightAge = mediumAge = -1;
        } else {
            lightAge = light ? 0 : lightAge >= 0 && lightAge < THROW_LENIENCY_FRAMES ? lightAge + 1 : -1;
            mediumAge = medium ? 0 : mediumAge >= 0 && mediumAge < THROW_LENIENCY_FRAMES ? mediumAge + 1 : -1;
        }
        // M + H on one frame or the spot between them: pushblock (a heavy outside blockstun).
        out.pushblock = pushblock || (medium && heavy);
        if (out.pushblock) medium = heavy = false;
        // OD button, or SUPER + TAG on one frame: Overdrive (neither the Super nor the assist).
        out.overdrive = overdrive || (superAttack && tag);
        if (out.overdrive) superAttack = tag = false;
        out.light = light;
        out.medium = medium;
        out.heavy = heavy;
        out.auto = auto;
        out.superAttack = superAttack;
        // ↓ + SUPER asks for the ultra; the engine falls back to SUPER without three bars.
        out.ultra = superAttack && ControlsLayout.isDownDirection(direction);
        // TAG calls the assist; ↓ + TAG is the raw tag (no hold to tell apart, so no delay).
        out.tag = tag && ControlsLayout.isDownDirection(direction);
        out.assist = tag && !out.tag;
        light = medium = heavy = auto = superAttack = tag = grab = pushblock = overdrive = false;
    }

    void reset() {
        direction = 0;
        lightAge = mediumAge = -1;
        light = medium = heavy = auto = superAttack = tag = grab = pushblock = overdrive = false;
    }
}
