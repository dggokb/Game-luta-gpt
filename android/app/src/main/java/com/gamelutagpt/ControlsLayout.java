package com.gamelutagpt;

/**
 * Geometry of the on-screen controls, in virtual-screen units. Input hit-testing and HUD
 * drawing read the same values, so a button is always pressed where it is drawn.
 */
final class ControlsLayout {
    private ControlsLayout() {}

    enum Control { AI_TOGGLE, DEBUG_TOGGLE, HEAL_PLAYER, HEAL_OPPONENT, DPAD, SUPER, LIGHT, MEDIUM, HEAVY, COMBO, TAG, THROW, PUSHBLOCK, NONE }

    static final float DPAD_X = 175f;
    static final float DPAD_Y = 555f;
    static final float DPAD_RADIUS = 122f;
    static final float DPAD_DEADZONE = 28f;
    private static final float DPAD_DIAGONAL = 0.70710677f;
    // Directions 1..8 clockwise from right: → ↘ ↓ ↙ ← ↖ ↑ ↗ (0 = neutral).
    static final String[] DPAD_LABELS = {
        "→", "↘", "↓", "↙", "←", "↖", "↑", "↗"
    };
    static final float[] DPAD_UNIT_X = {
        1f, DPAD_DIAGONAL, 0f, -DPAD_DIAGONAL,
        -1f, -DPAD_DIAGONAL, 0f, DPAD_DIAGONAL
    };
    static final float[] DPAD_UNIT_Y = {
        0f, DPAD_DIAGONAL, 1f, DPAD_DIAGONAL,
        0f, -DPAD_DIAGONAL, -1f, -DPAD_DIAGONAL
    };

    static final float ATTACK_RADIUS = 54f;
    static final float LIGHT_X = 1005f;
    static final float LIGHT_Y = 598f;
    static final float MEDIUM_X = 1100f;
    static final float MEDIUM_Y = 515f;
    static final float HEAVY_X = 1195f;
    static final float HEAVY_Y = 598f;

    // Throw: the spot between L and M counts as both (one thumb cannot press two buttons).
    static final float THROW_X = (LIGHT_X + MEDIUM_X) * 0.5f;
    static final float THROW_Y = (LIGHT_Y + MEDIUM_Y) * 0.5f;
    static final float THROW_RADIUS = 22f;
    // Pushblock: the spot between M and H (M + H).
    static final float PUSHBLOCK_X = (MEDIUM_X + HEAVY_X) * 0.5f;
    static final float PUSHBLOCK_Y = (MEDIUM_Y + HEAVY_Y) * 0.5f;

    static final float COMBO_X = 1100f;
    static final float COMBO_Y = 650f;
    static final float COMBO_RADIUS = 43f;

    static final float TAG_X = 930f;
    static final float TAG_Y = 505f;
    static final float TAG_RADIUS = 43f;

    static final float SUPER_X = 905f;
    static final float SUPER_Y = 620f;
    static final float SUPER_RADIUS = 46f;

    static final float AI_BUTTON_LEFT = 1082f;
    static final float AI_BUTTON_TOP = 124f;
    static final float AI_BUTTON_RIGHT = 1248f;
    static final float AI_BUTTON_BOTTOM = 172f;

    static final float DEBUG_BUTTON_LEFT = 1082f;
    static final float DEBUG_BUTTON_TOP = 182f;
    static final float DEBUG_BUTTON_RIGHT = 1248f;
    static final float DEBUG_BUTTON_BOTTOM = 222f;

    // Test helpers: refill the player's team and the CPU without restarting the app.
    static final float HEAL_BUTTON_TOP = 232f;
    static final float HEAL_BUTTON_BOTTOM = 272f;
    static final float HEAL_PLAYER_LEFT = 1082f;
    static final float HEAL_PLAYER_RIGHT = 1162f;
    static final float HEAL_OPPONENT_LEFT = 1168f;
    static final float HEAL_OPPONENT_RIGHT = 1248f;

    /** First control under the point, in the priority order the input handler uses. */
    static Control controlAt(float x, float y) {
        if (insideRect(x, y, AI_BUTTON_LEFT, AI_BUTTON_TOP, AI_BUTTON_RIGHT, AI_BUTTON_BOTTOM)) {
            return Control.AI_TOGGLE;
        }
        if (insideRect(x, y, DEBUG_BUTTON_LEFT, DEBUG_BUTTON_TOP, DEBUG_BUTTON_RIGHT, DEBUG_BUTTON_BOTTOM)) {
            return Control.DEBUG_TOGGLE;
        }
        if (insideRect(x, y, HEAL_PLAYER_LEFT, HEAL_BUTTON_TOP, HEAL_PLAYER_RIGHT, HEAL_BUTTON_BOTTOM)) {
            return Control.HEAL_PLAYER;
        }
        if (insideRect(x, y, HEAL_OPPONENT_LEFT, HEAL_BUTTON_TOP, HEAL_OPPONENT_RIGHT, HEAL_BUTTON_BOTTOM)) {
            return Control.HEAL_OPPONENT;
        }
        if (insideCircle(x, y, DPAD_X, DPAD_Y, DPAD_RADIUS)) return Control.DPAD;
        if (insideCircle(x, y, SUPER_X, SUPER_Y, SUPER_RADIUS)) return Control.SUPER;
        if (insideCircle(x, y, THROW_X, THROW_Y, THROW_RADIUS)) return Control.THROW;
        if (insideCircle(x, y, PUSHBLOCK_X, PUSHBLOCK_Y, THROW_RADIUS)) return Control.PUSHBLOCK;
        if (insideCircle(x, y, LIGHT_X, LIGHT_Y, ATTACK_RADIUS)) return Control.LIGHT;
        if (insideCircle(x, y, MEDIUM_X, MEDIUM_Y, ATTACK_RADIUS)) return Control.MEDIUM;
        if (insideCircle(x, y, HEAVY_X, HEAVY_Y, ATTACK_RADIUS)) return Control.HEAVY;
        if (insideCircle(x, y, COMBO_X, COMBO_Y, COMBO_RADIUS)) return Control.COMBO;
        if (insideCircle(x, y, TAG_X, TAG_Y, TAG_RADIUS)) return Control.TAG;
        return Control.NONE;
    }

    /** D-pad direction 1..8 under the point, or 0 inside the dead zone. */
    static int dpadDirectionAt(float x, float y) {
        float dx = x - DPAD_X;
        float dy = y - DPAD_Y;
        if (dx * dx + dy * dy < DPAD_DEADZONE * DPAD_DEADZONE) return 0;
        double degrees = Math.toDegrees(Math.atan2(dy, dx));
        if (degrees < 0) degrees += 360.0;
        int sector = ((int)Math.floor((degrees + 22.5) / 45.0)) % 8;
        return sector + 1;
    }

    static boolean isDownDirection(int direction) {
        return direction == 2 || direction == 3 || direction == 4;
    }

    static boolean isUpDirection(int direction) {
        return direction == 6 || direction == 7 || direction == 8;
    }

    static boolean insideRect(float x, float y, float left, float top, float right, float bottom) {
        return x >= left && x <= right && y >= top && y <= bottom;
    }

    static boolean insideCircle(float x, float y, float cx, float cy, float radius) {
        float dx = x - cx;
        float dy = y - cy;
        return dx * dx + dy * dy <= radius * radius;
    }
}
