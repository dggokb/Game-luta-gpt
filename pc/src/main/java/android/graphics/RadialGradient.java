package android.graphics;

public final class RadialGradient extends Shader {
    final float cx,cy,radius;
    final int color0,color1;
    public RadialGradient(float cx,float cy,float radius,int color0,int color1,TileMode tileMode){
        this.cx=cx;this.cy=cy;this.radius=radius;this.color0=color0;this.color1=color1;
    }
}
