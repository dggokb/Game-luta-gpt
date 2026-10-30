package android.graphics;

import java.awt.LinearGradientPaint;
import java.awt.geom.Point2D;

public class LinearGradient extends Shader {
    private final java.awt.Paint paint;
    /** Vertical de duas cores (a neblina, a água): o retângulo é pintado linha a linha no Canvas. */
    final boolean vertical2;
    final float gy0, gy1;
    final int cor0, cor1;

    public LinearGradient(float x0, float y0, float x1, float y1, int[] colors, float[] positions, TileMode tile) {
        paint = build(x0, y0, x1, y1, colors, positions, tile);
        vertical2 = x0 == x1 && y0 != y1 && colors.length == 2 && tile == TileMode.CLAMP
            && (positions == null || (positions.length == 2 && positions[0] == 0f && positions[1] == 1f));
        gy0 = y0;
        gy1 = y1;
        cor0 = colors[0];
        cor1 = colors[colors.length - 1];
    }

    public LinearGradient(float x0, float y0, float x1, float y1, int color0, int color1, TileMode tile) {
        this(x0, y0, x1, y1, new int[] {color0, color1}, null, tile);
    }

    private static java.awt.Paint build(float x0, float y0, float x1, float y1, int[] colors, float[] positions, TileMode tile) {
        if (colors.length == 1 || (x0 == x1 && y0 == y1)) return new java.awt.Color(colors[colors.length - 1], true);
        int[] c = colors;
        float[] p = positions;
        if (c.length == 2 && p == null) p = new float[] {0f, 1f};
        return new LinearGradientPaint(new Point2D.Float(x0, y0), new Point2D.Float(x1, y1),
            fractions(c.length, p), colors(c), cycle(tile));
    }

    @Override java.awt.Paint toAwt() { return paint; }
}
