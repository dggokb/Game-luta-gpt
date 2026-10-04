package com.gamelutagpt;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Base64;

/**
 * Bitmap-sprite renderer for Fighter Prototype 01.
 *
 * Gameplay never depends on frame artwork: it supplies state, this class picks
 * the atlas frame. Replacing the atlas later does not touch physics/hitboxes.
 */
final class SpriteFighterRenderer {
    static final int FRAME_W = 192;
    static final int FRAME_H = 224;
    static final int COLS = 4;

    private static final int IDLE_1 = 0;
    private static final int IDLE_2 = 1;
    private static final int WALK_1 = 2;
    private static final int WALK_2 = 3;
    private static final int CROUCH = 4;
    private static final int JUMP = 5;
    private static final int GUARD_HIGH = 6;
    private static final int GUARD_LOW = 7;
    private static final int LIGHT = 8;
    private static final int MEDIUM = 9;
    private static final int HEAVY = 10;
    private static final int CROUCH_LIGHT = 11;
    private static final int CROUCH_MEDIUM = 12;
    private static final int CROUCH_HEAVY = 13;
    private static final int SPECIAL = 14;
    private static final int SUPER = 15;

    private static final String ATLAS_B64 = "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";

    private final Bitmap atlas;
    private final Rect source = new Rect();
    private final RectF destination = new RectF();
    private final ColorFilter hitFlash =
        new PorterDuffColorFilter(
            Color.argb(150, 255, 255, 255),
            PorterDuff.Mode.SRC_ATOP
        );
    private final ColorFilter blockFlash =
        new PorterDuffColorFilter(
            Color.rgb(205, 240, 255),
            PorterDuff.Mode.MULTIPLY
        );

    SpriteFighterRenderer() {
        byte[] data = Base64.decode(ATLAS_B64, Base64.DEFAULT);
        atlas = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (atlas == null) {
            throw new IllegalStateException(
                "Could not decode Prototype 01 sprite atlas"
            );
        }
        atlas.setDensity(Bitmap.DENSITY_NONE);
    }

    void draw(
        Canvas canvas,
        Paint paint,
        float x,
        float baseY,
        boolean grounded,
        boolean crouching,
        boolean moving,
        float walkTime,
        String attackType,
        float attackTimer,
        int guardPose,
        boolean superPose,
        boolean damageFlash,
        boolean guardFlash
    ) {
        int frame = chooseFrame(
            grounded,
            crouching,
            moving,
            walkTime,
            attackType,
            attackTimer,
            guardPose,
            superPose
        );

        int col = frame % COLS;
        int row = frame / COLS;
        source.set(
            col * FRAME_W,
            row * FRAME_H,
            col * FRAME_W + FRAME_W,
            row * FRAME_H + FRAME_H
        );

        // Bottom-aligned to the exact gameplay foot position.
        float drawH = 205f;
        float drawW = drawH * FRAME_W / FRAME_H;
        destination.set(
            x - drawW * 0.5f,
            baseY - drawH,
            x + drawW * 0.5f,
            baseY
        );

        paint.setFilterBitmap(true);
        if (damageFlash) {
            paint.setColorFilter(hitFlash);
        } else if (guardFlash) {
            paint.setColorFilter(blockFlash);
        } else {
            paint.setColorFilter(null);
        }

        canvas.drawBitmap(atlas, source, destination, paint);

        paint.setColorFilter(null);
        paint.setFilterBitmap(false);
    }

    private int chooseFrame(
        boolean grounded,
        boolean crouching,
        boolean moving,
        float walkTime,
        String attackType,
        float attackTimer,
        int guardPose,
        boolean superPose
    ) {
        if (superPose) return SUPER;

        if (attackTimer > 0f && attackType != null) {
            if ("L".equals(attackType)) return LIGHT;
            if ("M".equals(attackType)) return MEDIUM;
            if ("H".equals(attackType)) return HEAVY;
            if ("2L".equals(attackType)) return CROUCH_LIGHT;
            if ("2M".equals(attackType)) return CROUCH_MEDIUM;
            if ("2H".equals(attackType)) return CROUCH_HEAVY;
            if ("S".equals(attackType)) return SPECIAL;
            if ("SUPER".equals(attackType)) return SUPER;
        }

        if (guardPose == 1) return GUARD_HIGH;
        if (guardPose == 2) return GUARD_LOW;

        if (!grounded) return JUMP;
        if (crouching) return CROUCH;

        if (moving) {
            return Math.sin(walkTime) >= 0f
                ? WALK_1
                : WALK_2;
        }

        long idleTick = System.nanoTime() / 350_000_000L;
        return (idleTick & 1L) == 0L
            ? IDLE_1
            : IDLE_2;
    }
}
