package android.graphics;

public final class ColorMatrix {
    final float[] values;
    public ColorMatrix(float[] values){ this.values=values==null?new float[0]:values.clone(); }
}
