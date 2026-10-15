package android.graphics;

/** Matriz de cor 4x5 (linhas R, G, B, A; colunas R, G, B, A, deslocamento 0..255). */
public class ColorMatrix {
    private final float[] array = new float[20];

    public ColorMatrix() { reset(); }

    public ColorMatrix(float[] src) { set(src); }

    public ColorMatrix(ColorMatrix src) { set(src.array); }

    public final float[] getArray() { return array; }

    public void reset() {
        java.util.Arrays.fill(array, 0f);
        array[0] = array[6] = array[12] = array[18] = 1f;
    }

    public void set(ColorMatrix src) { set(src.array); }

    public void set(float[] src) { System.arraycopy(src, 0, array, 0, 20); }

    public void setScale(float rScale, float gScale, float bScale, float aScale) {
        java.util.Arrays.fill(array, 0f);
        array[0] = rScale; array[6] = gScale; array[12] = bScale; array[18] = aScale;
    }

    public void setSaturation(float sat) {
        reset();
        float invSat = 1 - sat;
        float r = 0.213f * invSat, g = 0.715f * invSat, b = 0.072f * invSat;
        array[0] = r + sat; array[1] = g; array[2] = b;
        array[5] = r; array[6] = g + sat; array[7] = b;
        array[10] = r; array[11] = g; array[12] = b + sat;
    }

    public void setConcat(ColorMatrix matA, ColorMatrix matB) {
        float[] a = matA.array.clone(), b = matB.array.clone(), r = new float[20];
        int index = 0;
        for (int j = 0; j < 20; j += 5) {
            for (int i = 0; i < 4; i++) {
                r[index++] = a[j] * b[i] + a[j + 1] * b[i + 5] + a[j + 2] * b[i + 10] + a[j + 3] * b[i + 15];
            }
            r[index++] = a[j] * b[4] + a[j + 1] * b[9] + a[j + 2] * b[14] + a[j + 3] * b[19] + a[j + 4];
        }
        System.arraycopy(r, 0, array, 0, 20);
    }

    public void preConcat(ColorMatrix prematrix) { setConcat(this, prematrix); }

    public void postConcat(ColorMatrix postmatrix) { setConcat(postmatrix, this); }
}
