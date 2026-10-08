package android.graphics;

public final class ColorMatrix {
    float[] values;
    public ColorMatrix(){ this.values=identity(); }
    public ColorMatrix(float[] values){ this.values=values==null?identity():values.clone(); }
    public void setSaturation(float saturation){
        // Color filters are intentionally lightweight in the desktop adapter.
        // Keep the API contract so gameplay/render code stays shared.
    }
    public void postConcat(ColorMatrix other){}
    private static float[] identity(){
        return new float[]{
            1,0,0,0,0,
            0,1,0,0,0,
            0,0,1,0,0,
            0,0,0,1,0
        };
    }
}
