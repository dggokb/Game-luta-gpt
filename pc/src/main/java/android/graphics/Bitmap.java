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
  public void getPixels(int[] colors,int offset,int stride,int x,int y,int width,int height) {
    if(colors==null)throw new IllegalArgumentException("colors cannot be null");
    image.getRGB(x,y,width,height,colors,offset,stride);
  }
  public static Bitmap createBitmap(Bitmap source,int x,int y,int width,int height) {
    if(source==null||width<1||height<1)throw new IllegalArgumentException("Invalid bitmap crop");
    java.awt.image.BufferedImage sub=source.image.getSubimage(x,y,width,height);
    java.awt.image.BufferedImage copy=new java.awt.image.BufferedImage(width,height,java.awt.image.BufferedImage.TYPE_INT_ARGB);
    java.awt.Graphics2D g=copy.createGraphics();g.drawImage(sub,0,0,null);g.dispose();
    return new Bitmap(copy);
  }
  public void setDensity(int density){}
}