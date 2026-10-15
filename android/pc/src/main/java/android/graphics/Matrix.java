package android.graphics;

import java.awt.geom.AffineTransform;

/**
 * Matriz 3x3 com perspectiva, como a do Android (o seletor usa {@link #setPolyToPoly} para
 * entortar os retratos nos cards).
 */
public class Matrix {
    public static final int MSCALE_X = 0, MSKEW_X = 1, MTRANS_X = 2;
    public static final int MSKEW_Y = 3, MSCALE_Y = 4, MTRANS_Y = 5;
    public static final int MPERSP_0 = 6, MPERSP_1 = 7, MPERSP_2 = 8;

    final double[] m = new double[9];

    public Matrix() { reset(); }

    public Matrix(Matrix src) { set(src); }

    public void reset() {
        java.util.Arrays.fill(m, 0d);
        m[0] = m[4] = m[8] = 1d;
    }

    public void set(Matrix src) {
        if (src == null) reset();
        else System.arraycopy(src.m, 0, m, 0, 9);
    }

    public boolean isIdentity() {
        return m[0] == 1 && m[1] == 0 && m[2] == 0 && m[3] == 0 && m[4] == 1 && m[5] == 0 && isAffine();
    }

    public boolean isAffine() { return m[6] == 0 && m[7] == 0 && m[8] == 1; }

    public void getValues(float[] values) { for (int i = 0; i < 9; i++) values[i] = (float) m[i]; }

    public void setValues(float[] values) { for (int i = 0; i < 9; i++) m[i] = values[i]; }

    public void setTranslate(float dx, float dy) { reset(); m[2] = dx; m[5] = dy; }

    public void setScale(float sx, float sy) { reset(); m[0] = sx; m[4] = sy; }

    public void setScale(float sx, float sy, float px, float py) {
        setScale(sx, sy);
        m[2] = px - sx * px;
        m[5] = py - sy * py;
    }

    public void setRotate(float degrees) { setRotate(degrees, 0f, 0f); }

    public void setRotate(float degrees, float px, float py) {
        double r = Math.toRadians(degrees), c = Math.cos(r), s = Math.sin(r);
        reset();
        m[0] = c; m[1] = -s; m[3] = s; m[4] = c;
        m[2] = px - c * px + s * py;
        m[5] = py - s * px - c * py;
    }

    public boolean preConcat(Matrix other) { multiply(m, other.m); return true; }

    public boolean postConcat(Matrix other) { double[] r = other.m.clone(); multiply(r, m); System.arraycopy(r, 0, m, 0, 9); return true; }

    public boolean setConcat(Matrix a, Matrix b) { double[] r = a.m.clone(); multiply(r, b.m); System.arraycopy(r, 0, m, 0, 9); return true; }

    public boolean preTranslate(float dx, float dy) { Matrix t = new Matrix(); t.setTranslate(dx, dy); return preConcat(t); }

    public boolean postTranslate(float dx, float dy) { Matrix t = new Matrix(); t.setTranslate(dx, dy); return postConcat(t); }

    public boolean preScale(float sx, float sy) { Matrix t = new Matrix(); t.setScale(sx, sy); return preConcat(t); }

    public boolean postScale(float sx, float sy) { Matrix t = new Matrix(); t.setScale(sx, sy); return postConcat(t); }

    public boolean postScale(float sx, float sy, float px, float py) { Matrix t = new Matrix(); t.setScale(sx, sy, px, py); return postConcat(t); }

    public boolean preRotate(float degrees) { Matrix t = new Matrix(); t.setRotate(degrees); return preConcat(t); }

    public boolean postRotate(float degrees) { Matrix t = new Matrix(); t.setRotate(degrees); return postConcat(t); }

    public boolean postRotate(float degrees, float px, float py) { Matrix t = new Matrix(); t.setRotate(degrees, px, py); return postConcat(t); }

    public boolean invert(Matrix inverse) {
        double[] r = invert3(m);
        if (r == null) return false;
        System.arraycopy(r, 0, inverse.m, 0, 9);
        return true;
    }

    public void mapPoints(float[] pts) { mapPoints(pts, 0, pts, 0, pts.length / 2); }

    public void mapPoints(float[] dst, float[] src) { mapPoints(dst, 0, src, 0, src.length / 2); }

    public void mapPoints(float[] dst, int dstIndex, float[] src, int srcIndex, int pointCount) {
        for (int i = 0; i < pointCount; i++) {
            double x = src[srcIndex + 2 * i], y = src[srcIndex + 2 * i + 1];
            double w = m[6] * x + m[7] * y + m[8];
            dst[dstIndex + 2 * i] = (float) ((m[0] * x + m[1] * y + m[2]) / w);
            dst[dstIndex + 2 * i + 1] = (float) ((m[3] * x + m[4] * y + m[5]) / w);
        }
    }

    public boolean mapRect(RectF rect) {
        float[] p = {rect.left, rect.top, rect.right, rect.top, rect.right, rect.bottom, rect.left, rect.bottom};
        mapPoints(p);
        rect.set(Math.min(Math.min(p[0], p[2]), Math.min(p[4], p[6])), Math.min(Math.min(p[1], p[3]), Math.min(p[5], p[7])),
            Math.max(Math.max(p[0], p[2]), Math.max(p[4], p[6])), Math.max(Math.max(p[1], p[3]), Math.max(p[5], p[7])));
        return isAffine() && m[1] == 0 && m[3] == 0;
    }

    /** Como no Android: leva {@code pointCount} (0..4) pontos de src para dst. */
    public boolean setPolyToPoly(float[] src, int srcIndex, float[] dst, int dstIndex, int pointCount) {
        if (pointCount < 0 || pointCount > 4) throw new IllegalArgumentException("pointCount");
        if (pointCount == 0) { reset(); return true; }
        if (pointCount == 1) { setTranslate(dst[dstIndex] - src[srcIndex], dst[dstIndex + 1] - src[srcIndex + 1]); return true; }
        if (pointCount < 4) {
            // Similaridade (2 pontos) ou afim (3 pontos), completando o triângulo.
            float[] s = triangle(src, srcIndex, pointCount), d = triangle(dst, dstIndex, pointCount);
            double[] a = affineFromTriangle(s), b = affineFromTriangle(d);
            double[] ai = invert3(a);
            if (ai == null) return false;
            multiply(b, ai);
            System.arraycopy(b, 0, m, 0, 9);
            return true;
        }
        double[] a = squareToQuad(src, srcIndex), b = squareToQuad(dst, dstIndex);
        if (a == null || b == null) return false;
        double[] ai = invert3(a);
        if (ai == null) return false;
        multiply(b, ai);
        if (Math.abs(b[8]) > 1e-12) for (int i = 0; i < 9; i++) b[i] /= b[8];
        System.arraycopy(b, 0, m, 0, 9);
        return true;
    }

    /** A parte afim (ignora perspectiva). */
    AffineTransform toAffine() {
        return new AffineTransform(m[0], m[3], m[1], m[4], m[2], m[5]);
    }

    @Override public boolean equals(Object o) { return o instanceof Matrix && java.util.Arrays.equals(m, ((Matrix) o).m); }

    @Override public int hashCode() { return java.util.Arrays.hashCode(m); }

    private static float[] triangle(float[] p, int i, int count) {
        float x0 = p[i], y0 = p[i + 1], x1 = p[i + 2], y1 = p[i + 3];
        if (count == 3) return new float[] {x0, y0, x1, y1, p[i + 4], p[i + 5]};
        // Terceiro ponto: o segundo girado 90° em volta do primeiro (mantém a similaridade).
        return new float[] {x0, y0, x1, y1, x0 - (y1 - y0), y0 + (x1 - x0)};
    }

    private static double[] affineFromTriangle(float[] t) {
        return new double[] {t[2] - t[0], t[4] - t[0], t[0], t[3] - t[1], t[5] - t[1], t[1], 0, 0, 1};
    }

    /** Leva o quadrado unitário (0,0)(1,0)(1,1)(0,1) para o quadrilátero p. */
    private static double[] squareToQuad(float[] p, int i) {
        double x0 = p[i], y0 = p[i + 1], x1 = p[i + 2], y1 = p[i + 3];
        double x2 = p[i + 4], y2 = p[i + 5], x3 = p[i + 6], y3 = p[i + 7];
        double sx = x0 - x1 + x2 - x3, sy = y0 - y1 + y2 - y3;
        if (Math.abs(sx) < 1e-9 && Math.abs(sy) < 1e-9) {
            return new double[] {x1 - x0, x3 - x0, x0, y1 - y0, y3 - y0, y0, 0, 0, 1};
        }
        double dx1 = x1 - x2, dx2 = x3 - x2, dy1 = y1 - y2, dy2 = y3 - y2;
        double den = dx1 * dy2 - dx2 * dy1;
        if (Math.abs(den) < 1e-12) return null;
        double g = (sx * dy2 - dx2 * sy) / den;
        double h = (dx1 * sy - sx * dy1) / den;
        return new double[] {
            x1 - x0 + g * x1, x3 - x0 + h * x3, x0,
            y1 - y0 + g * y1, y3 - y0 + h * y3, y0,
            g, h, 1};
    }

    /** a = a × b (linha por coluna). */
    static void multiply(double[] a, double[] b) {
        double[] r = new double[9];
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++)
                r[row * 3 + col] = a[row * 3] * b[col] + a[row * 3 + 1] * b[3 + col] + a[row * 3 + 2] * b[6 + col];
        System.arraycopy(r, 0, a, 0, 9);
    }

    static double[] invert3(double[] a) {
        double det = a[0] * (a[4] * a[8] - a[5] * a[7]) - a[1] * (a[3] * a[8] - a[5] * a[6]) + a[2] * (a[3] * a[7] - a[4] * a[6]);
        if (Math.abs(det) < 1e-15) return null;
        double inv = 1d / det;
        return new double[] {
            (a[4] * a[8] - a[5] * a[7]) * inv, (a[2] * a[7] - a[1] * a[8]) * inv, (a[1] * a[5] - a[2] * a[4]) * inv,
            (a[5] * a[6] - a[3] * a[8]) * inv, (a[0] * a[8] - a[2] * a[6]) * inv, (a[2] * a[3] - a[0] * a[5]) * inv,
            (a[3] * a[7] - a[4] * a[6]) * inv, (a[1] * a[6] - a[0] * a[7]) * inv, (a[0] * a[4] - a[1] * a[3]) * inv};
    }
}
