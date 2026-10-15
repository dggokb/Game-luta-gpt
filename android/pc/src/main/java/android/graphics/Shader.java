package android.graphics;

/** Base dos degradês; cada um vira um {@link java.awt.Paint} do Java2D. */
public class Shader {
    public enum TileMode { CLAMP, REPEAT, MIRROR, DECAL }

    private Matrix localMatrix;

    public void setLocalMatrix(Matrix matrix) { localMatrix = matrix == null || matrix.isIdentity() ? null : new Matrix(matrix); }

    public boolean getLocalMatrix(Matrix out) {
        if (localMatrix == null) { out.reset(); return false; }
        out.set(localMatrix);
        return true;
    }

    /** O Paint do Java2D, em coordenadas locais; null = sem shader. */
    java.awt.Paint toAwt() { return null; }

    java.awt.geom.AffineTransform localTransform() { return localMatrix == null ? null : localMatrix.toAffine(); }

    static java.awt.MultipleGradientPaint.CycleMethod cycle(TileMode mode) {
        switch (mode) {
            case REPEAT: return java.awt.MultipleGradientPaint.CycleMethod.REPEAT;
            case MIRROR: return java.awt.MultipleGradientPaint.CycleMethod.REFLECT;
            default: return java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE;
        }
    }

    /**
     * Paradas do degradê no formato do Java2D: posições estritamente crescentes entre 0 e 1
     * (o Android aceita repetidas e null = espaçadas por igual).
     */
    static float[] fractions(int count, float[] positions) {
        float[] f = new float[count];
        for (int i = 0; i < count; i++) {
            float p = positions == null ? (count == 1 ? 0f : i / (float) (count - 1)) : positions[i];
            p = Math.max(0f, Math.min(1f, p));
            if (i > 0 && p <= f[i - 1]) p = Math.min(1f, f[i - 1] + 1e-5f);
            f[i] = p;
        }
        // O empurrão pode ter colado o fim em 1: desfaz de trás para frente.
        for (int i = count - 2; i >= 0; i--) if (f[i] >= f[i + 1]) f[i] = f[i + 1] - 1e-5f;
        return f;
    }

    static java.awt.Color[] colors(int[] argb) {
        java.awt.Color[] c = new java.awt.Color[argb.length];
        for (int i = 0; i < argb.length; i++) c[i] = new java.awt.Color(argb[i], true);
        return c;
    }
}
