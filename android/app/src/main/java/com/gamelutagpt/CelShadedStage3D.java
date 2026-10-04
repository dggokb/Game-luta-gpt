package com.gamelutagpt;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * Lightweight pseudo-3D cel-shaded stage renderer.
 *
 * Combat/world simulation remains unchanged. The stage is drawn in world
 * coordinates with multiple parallax layers, low-poly volumes and a perspective
 * floor converging toward the fight-camera center.
 */
final class CelShadedStage3D {
    private final Path path = new Path();
    private final Path path2 = new Path();

    void draw(
        Canvas c,
        Paint paint,
        float worldWidth,
        float worldTop,
        float groundY,
        float cameraX,
        float leftBound,
        float rightBound
    ) {
        drawSky(c, paint, worldWidth, worldTop, groundY);
        drawSun(c, paint, cameraX);
        drawFarMountains(c, paint, worldWidth, groundY, cameraX);
        drawMidMountains(c, paint, worldWidth, groundY, cameraX);
        drawRuins(c, paint, worldWidth, groundY, cameraX);
        drawPerspectiveGround(c, paint, worldWidth, groundY, cameraX);
        drawForegroundRocks(c, paint, worldWidth, groundY, cameraX);
        drawFightBounds(c, paint, groundY, leftBound, rightBound);
    }

    private void drawSky(
        Canvas c,
        Paint paint,
        float worldWidth,
        float worldTop,
        float groundY
    ) {
        paint.setStyle(Paint.Style.FILL);

        // Hard color bands instead of a gradient: deliberate anime/cel look.
        paint.setColor(Color.rgb(28, 58, 103));
        c.drawRect(0f, worldTop, worldWidth, -150f, paint);

        paint.setColor(Color.rgb(57, 91, 132));
        c.drawRect(0f, -150f, worldWidth, 145f, paint);

        paint.setColor(Color.rgb(172, 115, 88));
        c.drawRect(0f, 145f, worldWidth, 365f, paint);

        paint.setColor(Color.rgb(221, 151, 96));
        c.drawRect(0f, 365f, worldWidth, groundY, paint);

        // Thin horizon glow.
        paint.setColor(Color.rgb(245, 186, 116));
        c.drawRect(0f, 340f, worldWidth, 372f, paint);
    }

    private void drawSun(Canvas c, Paint paint, float cameraX) {
        float x = cameraX + 360f;
        float y = -28f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(255, 210, 126));
        c.drawCircle(x, y, 67f, paint);

        paint.setColor(Color.rgb(255, 236, 176));
        c.drawCircle(x - 12f, y - 10f, 47f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setColor(Color.rgb(95, 62, 64));
        c.drawCircle(x, y, 67f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawFarMountains(
        Canvas c,
        Paint paint,
        float worldWidth,
        float groundY,
        float cameraX
    ) {
        final float baseY = 394f;
        final float parallax = 0.78f;
        float shift = cameraX * parallax;

        for (int i = -2; i < 16; i++) {
            float center = i * 245f + shift - 500f;
            float height = 125f + (i & 3) * 27f;
            float half = 170f;

            path.reset();
            path.moveTo(center - half, baseY);
            path.lineTo(center, baseY - height);
            path.lineTo(center + half, baseY);
            path.close();

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(58, 68, 83));
            c.drawPath(path, paint);

            path2.reset();
            path2.moveTo(center, baseY - height);
            path2.lineTo(center + half, baseY);
            path2.lineTo(center + 24f, baseY);
            path2.lineTo(center - 12f, baseY - height + 32f);
            path2.close();

            paint.setColor(Color.rgb(37, 45, 59));
            c.drawPath(path2, paint);
        }
    }

    private void drawMidMountains(
        Canvas c,
        Paint paint,
        float worldWidth,
        float groundY,
        float cameraX
    ) {
        final float baseY = 440f;
        final float parallax = 0.90f;
        float shift = cameraX * parallax;

        for (int i = -1; i < 13; i++) {
            float center = i * 310f + shift - 430f;
            float height = 180f + (i % 4) * 24f;
            float half = 215f;

            // Front face.
            path.reset();
            path.moveTo(center - half, baseY);
            path.lineTo(center - 18f, baseY - height);
            path.lineTo(center + 42f, baseY - height + 34f);
            path.lineTo(center + half, baseY);
            path.close();

            paint.setColor(Color.rgb(78, 71, 72));
            c.drawPath(path, paint);

            // Lit face.
            path2.reset();
            path2.moveTo(center - half, baseY);
            path2.lineTo(center - 18f, baseY - height);
            path2.lineTo(center + 8f, baseY - height + 72f);
            path2.lineTo(center - 70f, baseY);
            path2.close();

            paint.setColor(Color.rgb(111, 88, 79));
            c.drawPath(path2, paint);

            // Hard outline.
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3f);
            paint.setColor(Color.rgb(30, 31, 38));
            c.drawPath(path, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void drawRuins(
        Canvas c,
        Paint paint,
        float worldWidth,
        float groundY,
        float cameraX
    ) {
        // Sparse low-poly pillars to make the stage read as a 3D arena.
        for (int i = 0; i < 7; i++) {
            float x = 185f + i * 395f;
            float h = 105f + (i % 3) * 34f;
            float w = 34f + (i % 2) * 8f;
            float depth = 18f;
            drawPrism(
                c,
                paint,
                x,
                438f - h,
                w,
                h,
                depth,
                Color.rgb(118, 91, 72),
                Color.rgb(77, 63, 61),
                Color.rgb(155, 119, 84)
            );

            // Cap block.
            drawPrism(
                c,
                paint,
                x - 8f,
                426f - h,
                w + 16f,
                16f,
                depth + 5f,
                Color.rgb(137, 104, 77),
                Color.rgb(84, 68, 63),
                Color.rgb(171, 133, 92)
            );
        }
    }

    private void drawPerspectiveGround(
        Canvas c,
        Paint paint,
        float worldWidth,
        float groundY,
        float cameraX
    ) {
        final float horizonY = 420f;
        final float bottomY = 760f;

        // Ground base.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(104, 73, 52));
        c.drawRect(0f, horizonY, worldWidth, bottomY, paint);

        // Broad cel-shaded depth bands.
        paint.setColor(Color.rgb(126, 88, 58));
        c.drawRect(0f, 480f, worldWidth, 555f, paint);

        paint.setColor(Color.rgb(91, 63, 48));
        c.drawRect(0f, 640f, worldWidth, bottomY, paint);

        // Perspective lane lines converge at the camera/fight center.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(Color.argb(120, 236, 197, 139));

        float vanishX = cameraX;
        for (int i = -8; i <= 8; i++) {
            float bottomX = cameraX + i * 245f;
            c.drawLine(vanishX, horizonY, bottomX, bottomY, paint);
        }

        // Horizontal perspective slices get farther apart toward the viewer.
        for (int i = 1; i <= 8; i++) {
            float t = i / 8f;
            float curved = t * t;
            float y = horizonY + (bottomY - horizonY) * curved;
            c.drawLine(
                cameraX - 1350f,
                y,
                cameraX + 1350f,
                y,
                paint
            );
        }

        // Main fight strip: darker plane under the fighters.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(68, 18, 18, 22));
        path.reset();
        path.moveTo(cameraX - 670f, groundY - 13f);
        path.lineTo(cameraX + 670f, groundY - 13f);
        path.lineTo(cameraX + 930f, bottomY);
        path.lineTo(cameraX - 930f, bottomY);
        path.close();
        c.drawPath(path, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(Color.argb(130, 34, 28, 28));
        c.drawLine(0f, groundY, worldWidth, groundY, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawForegroundRocks(
        Canvas c,
        Paint paint,
        float worldWidth,
        float groundY,
        float cameraX
    ) {
        // Low-poly foreground chunks. Kept mostly outside the combat lane.
        for (int i = 0; i < 12; i++) {
            float x = 45f + i * 230f;
            float y = groundY + 78f + (i % 3) * 35f;
            float size = 26f + (i % 4) * 8f;

            path.reset();
            path.moveTo(x - size, y + size * 0.4f);
            path.lineTo(x - size * 0.38f, y - size * 0.72f);
            path.lineTo(x + size * 0.55f, y - size * 0.42f);
            path.lineTo(x + size, y + size * 0.35f);
            path.lineTo(x + size * 0.15f, y + size * 0.72f);
            path.close();

            paint.setColor(Color.rgb(72, 55, 48));
            c.drawPath(path, paint);

            path2.reset();
            path2.moveTo(x - size * 0.38f, y - size * 0.72f);
            path2.lineTo(x + size * 0.55f, y - size * 0.42f);
            path2.lineTo(x + size * 0.10f, y + size * 0.03f);
            path2.lineTo(x - size * 0.58f, y - size * 0.04f);
            path2.close();

            paint.setColor(Color.rgb(128, 91, 62));
            c.drawPath(path2, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.5f);
            paint.setColor(Color.rgb(30, 28, 31));
            c.drawPath(path, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void drawFightBounds(
        Canvas c,
        Paint paint,
        float groundY,
        float leftBound,
        float rightBound
    ) {
        // Keep the old mechanical limits readable, but make them feel like
        // stylized arena pylons rather than debug lines.
        drawPrism(
            c,
            paint,
            leftBound - 7f,
            260f,
            14f,
            groundY - 260f,
            12f,
            Color.argb(150, 157, 181, 196),
            Color.argb(150, 86, 104, 121),
            Color.argb(170, 214, 229, 235)
        );
        drawPrism(
            c,
            paint,
            rightBound - 7f,
            260f,
            14f,
            groundY - 260f,
            12f,
            Color.argb(150, 157, 181, 196),
            Color.argb(150, 86, 104, 121),
            Color.argb(170, 214, 229, 235)
        );
    }

    private void drawPrism(
        Canvas c,
        Paint paint,
        float x,
        float y,
        float width,
        float height,
        float depth,
        int frontColor,
        int sideColor,
        int topColor
    ) {
        // Front.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(frontColor);
        c.drawRect(x, y, x + width, y + height, paint);

        // Right/depth face.
        path.reset();
        path.moveTo(x + width, y);
        path.lineTo(x + width + depth, y - depth * 0.42f);
        path.lineTo(x + width + depth, y + height - depth * 0.42f);
        path.lineTo(x + width, y + height);
        path.close();
        paint.setColor(sideColor);
        c.drawPath(path, paint);

        // Top face.
        path2.reset();
        path2.moveTo(x, y);
        path2.lineTo(x + depth, y - depth * 0.42f);
        path2.lineTo(x + width + depth, y - depth * 0.42f);
        path2.lineTo(x + width, y);
        path2.close();
        paint.setColor(topColor);
        c.drawPath(path2, paint);

        // Toon outline.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f);
        paint.setColor(Color.rgb(31, 30, 34));
        c.drawRect(x, y, x + width, y + height, paint);
        c.drawPath(path, paint);
        c.drawPath(path2, paint);
        paint.setStyle(Paint.Style.FILL);
    }
}
