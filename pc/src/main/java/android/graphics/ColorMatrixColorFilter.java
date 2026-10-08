package android.graphics;

public final class ColorMatrixColorFilter extends ColorFilter {
    final ColorMatrix matrix;
    public ColorMatrixColorFilter(ColorMatrix matrix){ this.matrix=matrix; }
}
