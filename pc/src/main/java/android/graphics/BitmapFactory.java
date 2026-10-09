package android.graphics;

import android.content.res.Resources;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

public final class BitmapFactory {
    private BitmapFactory(){}

    public static final class Options { public boolean inScaled=true; }

    public static Bitmap decodeResource(Resources resources,int id,Options options){
        try(InputStream input=resources.openDrawable(id)){
            if(input==null)return null;
            java.awt.image.BufferedImage image=ImageIO.read(input);
            return image==null?null:new Bitmap(image);
        }catch(IOException exception){
            return null;
        }
    }
}
