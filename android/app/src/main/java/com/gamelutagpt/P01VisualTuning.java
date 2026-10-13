package com.gamelutagpt;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Live on-device visual-size adjustments in DEBUG. Rendering only: no combat
 * physics, frame timing, sprite assets or hitboxes are modified.
 *
 * Every control is a multiplier on top of the per-frame idle calibration.
 * Store in SharedPreferences so APK updates retain the selected proportions.
 */
final class P01VisualTuning {
    static final String[] NAMES = {"DASH", "2H", "VITORIA"};
    private static final String[] ATLASES = {
        "player_base_dash", "player_base_crouch_heavy", "player_base_victory"
    };
    private static final float MIN = .80f, MAX = 1.25f, STEP = .05f;
    private static final String PREF = "p01_visual_scale_v1";
    private static final float[] MULTIPLIERS = {1f, 1f, 1f};

    static float scale(String atlas) {
        for (int i = 0; i < ATLASES.length; i++)
            if (ATLASES[i].equals(atlas)) return MULTIPLIERS[i];
        return 1f;
    }

    static int percent(int row) {
        return Math.round(MULTIPLIERS[row] * 100f);
    }

    static void load(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        for (int i = 0; i < MULTIPLIERS.length; i++)
            MULTIPLIERS[i] = bounded(p.getFloat(ATLASES[i], 1f));
    }

    static void adjust(Context context, int row, int delta) {
        if (row < 0 || row >= MULTIPLIERS.length || delta == 0) return;
        MULTIPLIERS[row] = bounded(MULTIPLIERS[row] + (delta > 0 ? STEP : -STEP));
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putFloat(ATLASES[row], MULTIPLIERS[row]).apply();
    }

    static void reset(Context context) {
        for (int i = 0; i < MULTIPLIERS.length; i++) MULTIPLIERS[i] = 1f;
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply();
    }

    private static float bounded(float requested) {
        return Math.round(Math.max(MIN, Math.min(MAX, requested)) / STEP) * STEP;
    }
    private P01VisualTuning() {}
}
