package com.gamelutagpt;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * Lightweight software 3D fighter renderer.
 *
 * The combat simulation remains 2D. This renderer builds a low-poly humanoid
 * in local X/Y/Z space, projects its depth to the Canvas and shades the visible
 * faces in discrete bands to create a cel-shaded/toon look.
 */
final class CelShadedFighter3D {
    static final int GUARD_NONE = 0;
    static final int GUARD_HIGH = 1;
    static final int GUARD_LOW = 2;

    private static final float DEPTH_X = 0.42f;
    private static final float DEPTH_Y = 0.16f;

    private final Path frontPath = new Path();
    private final Path sidePath = new Path();

    void draw(
        Canvas c,
        Paint paint,
        float x,
        float baseY,
        int color,
        boolean crouching,
        boolean airborne,
        float walkTime,
        String attackType,
        float attackPhase,
        int guardPose,
        boolean superPose,
        boolean hitFlash
    ) {
        float crouch = crouching ? 1f : 0f;
        float hipY = 50f - 18f * crouch;
        float shoulderY = 112f - 32f * crouch;
        float headY = 146f - 42f * crouch;

        float walk = (!crouching && !airborne)
            ? (float)Math.sin(walkTime)
            : 0f;

        float rearArmX = -27f - 10f * walk;
        float rearArmY = shoulderY - 34f + 5f * walk;
        float frontArmX = 31f + 10f * walk;
        float frontArmY = shoulderY - 34f - 5f * walk;

        float rearFootX = -25f + 17f * walk;
        float rearFootY = 1f;
        float frontFootX = 27f - 17f * walk;
        float frontFootY = 1f;

        if (airborne) {
            rearFootX = -31f;
            rearFootY = 24f;
            frontFootX = 35f;
            frontFootY = 18f;
        }

        if (guardPose == GUARD_HIGH) {
            rearArmX = -24f;
            rearArmY = headY - 2f;
            frontArmX = 31f;
            frontArmY = headY + 4f;
        } else if (guardPose == GUARD_LOW) {
            rearArmX = -35f;
            rearArmY = hipY + 24f;
            frontArmX = 43f;
            frontArmY = hipY + 18f;
            rearFootX = -39f;
            frontFootX = 41f;
        } else if (superPose) {
            float pulse = 1f + 0.08f * (float)Math.sin(walkTime * 2f);
            rearArmX = -72f * pulse;
            rearArmY = shoulderY + 10f;
            frontArmX = 82f * pulse;
            frontArmY = shoulderY + 13f;
            rearFootX = -39f;
            frontFootX = 43f;
        } else if (attackType != null && !attackType.isEmpty()) {
            float p = Math.max(0f, Math.min(1f, attackPhase));
            if ("L".equals(attackType)) {
                frontArmX = 35f + 86f * p;
                frontArmY = shoulderY - 8f;
            } else if ("M".equals(attackType)) {
                frontFootX = 30f + 100f * p;
                frontFootY = 14f + 42f * p;
                rearArmX -= 12f * p;
            } else if ("H".equals(attackType)) {
                frontArmX = 34f + 72f * p;
                frontArmY = shoulderY + 52f * p;
                rearArmX = -42f;
                rearArmY = shoulderY + 4f;
            } else if ("2L".equals(attackType)) {
                frontArmX = 34f + 68f * p;
                frontArmY = hipY + 30f;
            } else if ("2M".equals(attackType)) {
                frontFootX = 41f + 112f * p;
                frontFootY = 7f;
            } else if ("2H".equals(attackType)) {
                frontArmX = 38f + 88f * p;
                frontArmY = shoulderY + 44f * p;
                frontFootX = 42f;
            } else if ("S".equals(attackType) || "SUPER".equals(attackType)) {
                frontArmX = 75f + 42f * p;
                frontArmY = shoulderY + 3f;
                rearArmX = 38f + 28f * p;
                rearArmY = shoulderY - 5f;
            }
        }

        int baseColor = hitFlash ? Color.WHITE : color;
        int darkColor = shade(baseColor, 0.48f);
        int midColor = shade(baseColor, 0.76f);
        int lightColor = shade(baseColor, 1.16f);
        int outline = Color.rgb(24, 25, 31);

        // Back limbs first.
        drawLimb(c, paint, x, baseY, -9f, hipY + 3f, -11f, rearFootX, rearFootY, -13f, 17f, darkColor, midColor, outline);
        drawLimb(c, paint, x, baseY, -20f, shoulderY - 7f, -15f, rearArmX, rearArmY, -18f, 14f, darkColor, midColor, outline);

        // Torso: tapered low-poly prism.
        drawTorso(c, paint, x, baseY, hipY, shoulderY, 27f, 35f, 28f, baseColor, darkColor, lightColor, outline);

        // Front limbs.
        drawLimb(c, paint, x, baseY, 10f, hipY + 3f, 11f, frontFootX, frontFootY, 14f, 18f, midColor, baseColor, outline);
        drawLimb(c, paint, x, baseY, 20f, shoulderY - 7f, 16f, frontArmX, frontArmY, 20f, 15f, midColor, lightColor, outline);

        // Hands / shoes add extra volume.
        drawJoint(c, paint, x, baseY, rearArmX, rearArmY, -18f, 11f, darkColor, outline);
        drawJoint(c, paint, x, baseY, frontArmX, frontArmY, 20f, 12f, lightColor, outline);
        drawJoint(c, paint, x, baseY, rearFootX, rearFootY + 3f, -13f, 13f, darkColor, outline);
        drawJoint(c, paint, x, baseY, frontFootX, frontFootY + 3f, 14f, 14f, midColor, outline);

        // Neck + head.
        drawLimb(c, paint, x, baseY, 0f, shoulderY + 4f, 1f, 0f, headY - 18f, 2f, 13f, midColor, baseColor, outline);
        drawHead(c, paint, x, baseY, 1f, headY, 28f, baseColor, darkColor, lightColor, outline);

        // Small hard-edged chest highlight: gives the model a toon-lit plane.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(lightColor);
        frontPath.reset();
        point(frontPath, x, baseY, 7f, shoulderY - 6f, 29f, true);
        point(frontPath, x, baseY, 25f, shoulderY - 20f, 24f, false);
        point(frontPath, x, baseY, 18f, hipY + 24f, 23f, false);
        point(frontPath, x, baseY, 5f, hipY + 31f, 25f, false);
        frontPath.close();
        c.drawPath(frontPath, paint);
    }

    private void drawTorso(
        Canvas c,
        Paint paint,
        float worldX,
        float baseY,
        float hipY,
        float shoulderY,
        float hipHalf,
        float shoulderHalf,
        float depth,
        int front,
        int side,
        int highlight,
        int outline
    ) {
        float zFront = depth * 0.50f;
        float zBack = -depth * 0.50f;

        // Side plane.
        sidePath.reset();
        point(sidePath, worldX, baseY, shoulderHalf, shoulderY, zFront, true);
        point(sidePath, worldX, baseY, shoulderHalf, shoulderY, zBack, false);
        point(sidePath, worldX, baseY, hipHalf, hipY, zBack, false);
        point(sidePath, worldX, baseY, hipHalf, hipY, zFront, false);
        sidePath.close();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(side);
        c.drawPath(sidePath, paint);

        // Front plane.
        frontPath.reset();
        point(frontPath, worldX, baseY, -shoulderHalf, shoulderY, zFront, true);
        point(frontPath, worldX, baseY, shoulderHalf, shoulderY, zFront, false);
        point(frontPath, worldX, baseY, hipHalf, hipY, zFront, false);
        point(frontPath, worldX, baseY, -hipHalf, hipY, zFront, false);
        frontPath.close();

        paint.setColor(front);
        c.drawPath(frontPath, paint);

        // Bright upper plane.
        Path p = sidePath;
        p.reset();
        point(p, worldX, baseY, -shoulderHalf, shoulderY, zBack, true);
        point(p, worldX, baseY, shoulderHalf, shoulderY, zBack, false);
        point(p, worldX, baseY, shoulderHalf, shoulderY, zFront, false);
        point(p, worldX, baseY, -shoulderHalf, shoulderY, zFront, false);
        p.close();
        paint.setColor(highlight);
        c.drawPath(p, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.2f);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(outline);
        c.drawPath(frontPath, paint);
        c.drawPath(sidePath, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawLimb(
        Canvas c,
        Paint paint,
        float worldX,
        float baseY,
        float ax,
        float ay,
        float az,
        float bx,
        float by,
        float bz,
        float width,
        int sideColor,
        int frontColor,
        int outline
    ) {
        float dx = bx - ax;
        float dy = by - ay;
        float len = (float)Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) len = 1f;

        float px = -dy / len * width * 0.5f;
        float py = dx / len * width * 0.5f;
        float depth = width * 0.72f;

        float aFrontZ = az + depth * 0.5f;
        float bFrontZ = bz + depth * 0.5f;
        float aBackZ = az - depth * 0.5f;
        float bBackZ = bz - depth * 0.5f;

        sidePath.reset();
        point(sidePath, worldX, baseY, ax + px, ay + py, aFrontZ, true);
        point(sidePath, worldX, baseY, ax + px, ay + py, aBackZ, false);
        point(sidePath, worldX, baseY, bx + px, by + py, bBackZ, false);
        point(sidePath, worldX, baseY, bx + px, by + py, bFrontZ, false);
        sidePath.close();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(sideColor);
        c.drawPath(sidePath, paint);

        frontPath.reset();
        point(frontPath, worldX, baseY, ax + px, ay + py, aFrontZ, true);
        point(frontPath, worldX, baseY, ax - px, ay - py, aFrontZ, false);
        point(frontPath, worldX, baseY, bx - px, by - py, bFrontZ, false);
        point(frontPath, worldX, baseY, bx + px, by + py, bFrontZ, false);
        frontPath.close();

        paint.setColor(frontColor);
        c.drawPath(frontPath, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.1f);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(outline);
        c.drawPath(frontPath, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawJoint(
        Canvas c,
        Paint paint,
        float worldX,
        float baseY,
        float x,
        float y,
        float z,
        float radius,
        int color,
        int outline
    ) {
        float sx = projectX(worldX, x, z);
        float sy = projectY(baseY, y, z);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        c.drawCircle(sx, sy, radius, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(outline);
        c.drawCircle(sx, sy, radius, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawHead(
        Canvas c,
        Paint paint,
        float worldX,
        float baseY,
        float x,
        float y,
        float z,
        int front,
        int side,
        int highlight,
        int outline
    ) {
        float sx = projectX(worldX, x, z);
        float sy = projectY(baseY, y, z);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(side);
        c.drawCircle(sx - 6f, sy + 4f, z + 2f, paint);

        paint.setColor(front);
        c.drawCircle(sx, sy, z, paint);

        paint.setColor(highlight);
        c.drawOval(
            sx - z * 0.48f,
            sy - z * 0.57f,
            sx + z * 0.05f,
            sy + z * 0.03f,
            paint
        );

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.4f);
        paint.setColor(outline);
        c.drawCircle(sx, sy, z, paint);

        // Anime-like brow/eye mark, still very simple at prototype stage.
        paint.setStrokeWidth(3f);
        c.drawLine(sx + 5f, sy - 2f, sx + 16f, sy - 4f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void point(
        Path path,
        float worldX,
        float baseY,
        float x,
        float y,
        float z,
        boolean move
    ) {
        float sx = projectX(worldX, x, z);
        float sy = projectY(baseY, y, z);
        if (move) path.moveTo(sx, sy);
        else path.lineTo(sx, sy);
    }

    private float projectX(float worldX, float x, float z) {
        return worldX + x + z * DEPTH_X;
    }

    private float projectY(float baseY, float y, float z) {
        return baseY - y - z * DEPTH_Y;
    }

    private int shade(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.min(255, Math.max(0, Math.round(Color.red(color) * factor)));
        int g = Math.min(255, Math.max(0, Math.round(Color.green(color) * factor)));
        int b = Math.min(255, Math.max(0, Math.round(Color.blue(color) * factor)));
        return Color.argb(a, r, g, b);
    }
}
