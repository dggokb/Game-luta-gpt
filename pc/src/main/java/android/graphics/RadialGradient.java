package android.graphics;
public final class RadialGradient extends Shader {
  final float cx,cy,radius;
  final int centerColor,edgeColor;
  public RadialGradient(float cx,float cy,float radius,int centerColor,int edgeColor,TileMode tile){
    this.cx=cx;this.cy=cy;this.radius=radius;this.centerColor=centerColor;this.edgeColor=edgeColor;
  }
}