package com.gamelutagpt;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.util.Base64;

/**
 * Idle-only sprite test for Fighter Prototype 01.
 * Combat remains fully independent from artwork.
 */
final class SpriteFighterRenderer {
    private static final long IDLE_FRAME_NS = 120_000_000L;

    private static final int IDLE_COLUMNS = 4;
    private static final int IDLE_ROWS = 2;
    private static final int IDLE_FRAME_COUNT = IDLE_COLUMNS * IDLE_ROWS;

    private final Bitmap[] idleFrames = new Bitmap[IDLE_FRAME_COUNT];
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
        Bitmap idleSheet = decodeSafe(IdleSpriteData.encoded());
        buildIdleFrames(idleSheet);
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
        Bitmap frame = chooseFrame(
            grounded,
            crouching,
            moving,
            attackType,
            attackTimer,
            guardPose,
            superPose
        );

        float drawH = 205f;
        float drawW =
            drawH * frame.getWidth() / frame.getHeight();

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

        canvas.drawBitmap(
            frame,
            null,
            destination,
            paint
        );

        paint.setColorFilter(null);
        paint.setFilterBitmap(false);
    }

    private void buildIdleFrames(Bitmap sheet) {
        if (sheet.getWidth() < IDLE_COLUMNS || sheet.getHeight() < IDLE_ROWS) {
            for (int i = 0; i < idleFrames.length; i++) {
                idleFrames[i] = sheet;
            }
            return;
        }

        int frameWidth = sheet.getWidth() / IDLE_COLUMNS;
        int frameHeight = sheet.getHeight() / IDLE_ROWS;

        for (int i = 0; i < IDLE_FRAME_COUNT; i++) {
            int column = i % IDLE_COLUMNS;
            int row = i / IDLE_COLUMNS;
            Bitmap frame = Bitmap.createBitmap(
                sheet,
                column * frameWidth,
                row * frameHeight,
                frameWidth,
                frameHeight
            );
            frame.setDensity(Bitmap.DENSITY_NONE);
            idleFrames[i] = frame;
        }
    }

    private Bitmap chooseFrame(
        boolean grounded,
        boolean crouching,
        boolean moving,
        String attackType,
        float attackTimer,
        int guardPose,
        boolean superPose
    ) {
        boolean trulyIdle =
            grounded &&
            !crouching &&
            !moving &&
            attackTimer <= 0f &&
            (attackType == null || attackType.isEmpty()) &&
            guardPose == 0 &&
            !superPose;

        if (!trulyIdle) {
            // This build validates only the idle animation.
            // Other sprite states will be authored next.
            return idleFrames[0];
        }

        long step =
            (System.nanoTime() / IDLE_FRAME_NS) % IDLE_FRAME_COUNT;

        // Full 8-frame loop from the base fighter idle sprite sheet.
        return idleFrames[(int) step];
    }

    private Bitmap decodeSafe(String encoded) {
        try {
            byte[] data =
                Base64.decode(encoded, Base64.DEFAULT);

            Bitmap decoded =
                BitmapFactory.decodeByteArray(
                    data,
                    0,
                    data.length
                );

            if (decoded != null) {
                decoded.setDensity(Bitmap.DENSITY_NONE);
                return decoded;
            }
        } catch (Throwable ignored) {
            // Never crash the game because one sprite failed to decode.
        }

        Bitmap fallback =
            Bitmap.createBitmap(
                1,
                1,
                Bitmap.Config.ARGB_8888
            );
        fallback.eraseColor(Color.TRANSPARENT);
        return fallback;
    }
}
