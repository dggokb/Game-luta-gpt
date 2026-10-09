package android.graphics;
public final class ColorMatrixColorFilter extends ColorFilter {
  private final ColorMatrix matrix;
  public ColorMatrixColorFilter(ColorMatrix m){this.matrix=m;}
  public ColorMatrix getMatrix(){return matrix;}
}