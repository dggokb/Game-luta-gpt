package android.graphics;
import java.awt.image.BufferedImage;
public final class Bitmap {
  public static final int DENSITY_NONE=0;
  final BufferedImage image;
  Bitmap(BufferedImage image){this.image=image;}
  public int getWidth(){return image.getWidth();}
  public int getHeight(){return image.getHeight();}
  public int getAllocationByteCount(){return image.getWidth()*image.getHeight()*4;}
  public void recycle(){image.flush();}
  public void setDensity(int density){}
}