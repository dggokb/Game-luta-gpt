package com.gamelutagpt;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import java.util.List;

/** Projectiles and Super cinematic overlays, drawn in world space. */
final class EffectsRenderer {
    void drawEnergyProjectiles(Canvas c, Paint paint, List<Projectile> projectiles) {
        for (Projectile projectile : projectiles) {
            c.save();
            if (projectile.direction < 0) {
                c.scale(-1f, 1f, projectile.x, projectile.y);
            }

            paint.setColor(Color.argb(75, 255, 255, 255));
            c.drawCircle(projectile.x, projectile.y, 29f, paint);

            paint.setColor(projectile.color);
            c.drawCircle(projectile.x, projectile.y, 20f, paint);

            paint.setColor(Color.WHITE);
            c.drawCircle(projectile.x + 5f, projectile.y - 5f, 8f, paint);
            c.restore();
        }
    }

    void drawSuperProjectiles(Canvas c, Paint paint, List<Projectile> projectiles) {
        for (Projectile projectile : projectiles) {
            c.save();
            if (projectile.direction < 0) {
                c.scale(-1f, 1f, projectile.x, projectile.y);
            }

            paint.setColor(Color.argb(70, 255, 255, 255));
            c.drawCircle(projectile.x, projectile.y, 62f, paint);

            paint.setColor(Color.argb(120, Color.red(projectile.color), Color.green(projectile.color), Color.blue(projectile.color)));
            c.drawOval(
                projectile.x - 72f,
                projectile.y - 34f,
                projectile.x + 34f,
                projectile.y + 34f,
                paint
            );

            paint.setColor(projectile.color);
            c.drawCircle(projectile.x, projectile.y, 40f, paint);

            paint.setColor(Color.WHITE);
            c.drawCircle(projectile.x + 12f, projectile.y - 10f, 17f, paint);

            paint.setColor(Color.argb(100, 255, 255, 255));
            c.drawRect(
                projectile.x - 135f,
                projectile.y - 9f,
                projectile.x - 38f,
                projectile.y + 9f,
                paint
            );
            c.restore();
        }
    }

    /** Full-world tint; used for the Super darkening and the release flash. */
    void drawWorldOverlay(Canvas c, Paint paint, int alpha, int red, int green, int blue) {
        if (alpha <= 0) return;
        paint.setColor(Color.argb(alpha, red, green, blue));
        c.drawRect(0f, Arena.WORLD_TOP, Arena.WORLD_WIDTH, Arena.VH + 120f, paint);
    }

    /** Ultra rush trail behind the fighter; canvas is already mirrored to its facing. */
    void drawUltraRush(Canvas c, Paint paint, float x, float baseY, int color) {
        float centerY = baseY - 78f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 7; i++) {
            float y = centerY - 72f + i * 24f;
            float length = 130f + (i % 3) * 70f;
            paint.setStrokeWidth(i % 2 == 0 ? 6f : 3f);
            paint.setColor(i % 2 == 0 ? color : Color.argb(200, 255, 255, 255));
            c.drawLine(x - 40f - length, y, x - 40f, y, paint);
        }
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStyle(Paint.Style.FILL);
    }

    /** Air dash streaks behind the body; canvas is mirrored so the motion goes right. */
    void drawAirDash(Canvas c, Paint paint, float x, float baseY, int color) {
        float centerY = baseY - 90f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 4; i++) {
            float y = centerY - 48f + i * 32f;
            float length = 70f + (i % 2) * 45f;
            paint.setStrokeWidth(i % 2 == 0 ? 4f : 2.5f);
            paint.setColor(i % 2 == 0 ? Color.argb(170, 255, 255, 255) : (color & 0x00FFFFFF) | 0xB4000000);
            c.drawLine(x - 30f - length, y, x - 30f, y, paint);
        }
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStyle(Paint.Style.FILL);
    }

    /** Charge rings and speed lines around the fighter; canvas is already mirrored to its facing. */
    void drawSuperCharge(Canvas c, Paint paint, float x, float baseY, float phaseTimer, int color) {
        float centerY = baseY - 78f;
        float pulse = 1f + 0.16f * (float)Math.sin(phaseTimer * 28f);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);
        paint.setColor(Color.argb(185, 255, 255, 255));
        c.drawCircle(x, centerY, 58f * pulse, paint);

        paint.setStrokeWidth(3f);
        paint.setColor(color);
        c.drawCircle(x, centerY, 82f * pulse, paint);

        for (int i = 0; i < 7; i++) {
            float y = centerY - 105f + i * 34f;
            float length = 90f + (i % 3) * 42f;
            paint.setStrokeWidth(4f);
            paint.setColor(Color.argb(145, 255, 255, 255));
            c.drawLine(x - 150f - length, y + 24f, x - 78f, y, paint);
        }

        paint.setStyle(Paint.Style.FILL);
    }
}
