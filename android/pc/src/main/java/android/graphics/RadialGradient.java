package android.graphics;

import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;

public class RadialGradient extends Shader {
    private final java.awt.Paint paint;

    public RadialGradient(float centerX, float centerY, float radius, int[] colors, float[] stops, TileMode tileMode) {
        if (radius <= 0f) throw new IllegalArgumentException("radius must be > 0");
        paint = colors.length == 1 ? new java.awt.Color(colors[0], true)
            : new RadialGradientPaint(new Point2D.Float(centerX, centerY), radius,
                fractions(colors.length, stops), colors(colors), cycle(tileMode));
    }

    public RadialGradient(float centerX, float centerY, float radius, int centerColor, int edgeColor, TileMode tileMode) {
        this(centerX, centerY, radius, new int[] {centerColor, edgeColor}, null, tileMode);
    }

    @Override java.awt.Paint toAwt() { return paint; }
}
