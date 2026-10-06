package com.gamelutagpt;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

/** Static stage: sky, sun, clouds, mountains, ground and arena bounds. */
final class StageRenderer {
    private final LinearGradient skyGradient = new LinearGradient(
        0f,
        Arena.WORLD_TOP,
        0f,
        Arena.VH,
        Color.rgb(21, 55, 103),
        Color.rgb(240, 171, 99),
        Shader.TileMode.CLAMP
    );
    private final Path[] mountainPaths = new Path[15];

    StageRenderer() {
        for (int i = 0; i < mountainPaths.length; i++) {
            float x = -100f + i * 190f;
            float h = 105f + (i % 5) * 24f;
            Path path = new Path();
            path.moveTo(x, Arena.GROUND_Y);
            path.lineTo(x + 115f, Arena.GROUND_Y - h);
            path.lineTo(x + 245f, Arena.GROUND_Y);
            path.close();
            mountainPaths[i] = path;
        }
    }

    void draw(Canvas c, Paint paint) {
        paint.setShader(skyGradient);
        c.drawRect(0, Arena.WORLD_TOP, Arena.WORLD_WIDTH, Arena.VH, paint);
        paint.setShader(null);

        paint.setColor(Color.argb(130, 255, 244, 201));
        c.drawCircle(2050, -40, 58, paint);

        paint.setColor(Color.argb(75, 255, 255, 255));
        for (int i = 0; i < 12; i++) {
            float cloudX = 110f + i * 215f;
            float cloudY = -365f + (i % 4) * 92f;
            c.drawOval(cloudX, cloudY, cloudX + 120f, cloudY + 38f, paint);
        }

        paint.setColor(Color.rgb(53, 73, 88));
        for (Path mountainPath : mountainPaths) {
            c.drawPath(mountainPath, paint);
        }

        paint.setColor(Color.rgb(116, 81, 50));
        c.drawRect(0, Arena.GROUND_Y, Arena.WORLD_WIDTH, Arena.VH, paint);
        paint.setColor(Color.rgb(148, 108, 67));
        for (int i = 0; i < 38; i++) {
            float x = (i * 83f) % Arena.WORLD_WIDTH;
            c.drawRoundRect(
                x,
                Arena.GROUND_Y + 35 + (i % 3) * 38,
                x + 55,
                Arena.GROUND_Y + 40 + (i % 3) * 38,
                3,
                3,
                paint
            );
        }

        paint.setColor(Color.argb(90, 255, 255, 255));
        c.drawRect(Arena.LEFT_BOUND, 190, Arena.LEFT_BOUND + 3, Arena.GROUND_Y, paint);
        c.drawRect(Arena.RIGHT_BOUND - 3, 190, Arena.RIGHT_BOUND, Arena.GROUND_Y, paint);
    }
}
