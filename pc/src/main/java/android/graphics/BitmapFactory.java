package android.graphics;
import android.content.res.Resources;
import java.io.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
public final class BitmapFactory {
  private BitmapFactory(){}
  public static final class Options {
    public boolean inScaled=true;
    public boolean inJustDecodeBounds=false;
    public int inSampleSize=1;
    public int outWidth=0,outHeight=0;
  }
  public static Bitmap decodeResource(Resources resources,int id,Options options){
    try(InputStream in=resources.openDrawable(id)){
      return in==null?null:decodeStream(in,null,options);
    }catch(IOException ex){return null;}
  }
  public static Bitmap decodeStream(InputStream in,Rect unused,Options options) {
    try {
      BufferedImage img=ImageIO.read(in);
      if(img==null)return null;
      if(options!=null){
        options.outWidth=img.getWidth();options.outHeight=img.getHeight();
        if(options.inJustDecodeBounds)return null;
        int sample=Math.max(1,options.inSampleSize);
        if(sample>1){
          int w=Math.max(1,img.getWidth()/sample),h=Math.max(1,img.getHeight()/sample);
          BufferedImage scaled=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
          Graphics2D g=scaled.createGraphics();
          g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
          g.drawImage(img,0,0,w,h,null);g.dispose();
          img.flush();img=scaled;
        }
      }
      return new Bitmap(img);
    }catch(IOException e){return null;}
  }
}