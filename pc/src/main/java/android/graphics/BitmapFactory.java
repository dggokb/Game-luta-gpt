package android.graphics;

import android.content.res.Resources;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

public final class BitmapFactory {
    private BitmapFactory(){}

    public static final class Options {
        public boolean inScaled=true;
        public boolean inJustDecodeBounds=false;
        public int inSampleSize=1;
        public int outWidth=0;
        public int outHeight=0;
    }

    public static Bitmap decodeResource(Resources resources,int id,Options options){
        try(InputStream input=resources.openDrawable(id)){
            if(input==null)return null;
            return decodeStream(input,null,options);
        }catch(IOException exception){
            return null;
        }
    }

    public static Bitmap decodeStream(InputStream input,Object padding,Options options){
        if(input==null)return null;
        try{
            BufferedImage image=ImageIO.read(input);
            if(image==null)return null;
            if(options!=null){
                options.outWidth=image.getWidth();
                options.outHeight=image.getHeight();
                if(options.inJustDecodeBounds)return null;
                int sample=Math.max(1,options.inSampleSize);
                if(sample>1){
                    int w=Math.max(1,image.getWidth()/sample);
                    int h=Math.max(1,image.getHeight()/sample);
                    BufferedImage scaled=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g=scaled.createGraphics();
                    try{ g.drawImage(image,0,0,w,h,null); } finally { g.dispose(); }
                    image=scaled;
                }
            }
            return new Bitmap(image);
        }catch(IOException exception){
            return null;
        }
    }
}
