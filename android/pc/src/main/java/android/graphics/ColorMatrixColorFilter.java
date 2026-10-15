package android.graphics;

public class ColorMatrixColorFilter extends ColorFilter {
    private final float[] m;

    public ColorMatrixColorFilter(ColorMatrix matrix) { m = matrix.getArray().clone(); }

    public ColorMatrixColorFilter(float[] array) { m = array.clone(); }

    @Override int filter(int c) {
        float r = Color.red(c), g = Color.green(c), b = Color.blue(c), a = Color.alpha(c);
        return Color.argb(
            clamp(m[15] * r + m[16] * g + m[17] * b + m[18] * a + m[19]),
            clamp(m[0] * r + m[1] * g + m[2] * b + m[3] * a + m[4]),
            clamp(m[5] * r + m[6] * g + m[7] * b + m[8] * a + m[9]),
            clamp(m[10] * r + m[11] * g + m[12] * b + m[13] * a + m[14]));
    }

    private static int clamp(float v) { return v <= 0f ? 0 : v >= 255f ? 255 : Math.round(v); }

    @Override public boolean equals(Object o) { return o instanceof ColorMatrixColorFilter && java.util.Arrays.equals(m, ((ColorMatrixColorFilter) o).m); }

    @Override public int hashCode() { return java.util.Arrays.hashCode(m); }
}
