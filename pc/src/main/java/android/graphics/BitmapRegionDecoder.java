package android.graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageReadParam;
import javax.imageio.stream.ImageInputStream;

/**
 * Decode only the portrait's atlas cell, not the entire multi-frame texture.
 * Uses ImageIO source-region decoding for the Android BitmapRegionDecoder API.
 */
public final class BitmapRegionDecoder {
    private final ImageReader reader;
    private final ImageInputStream imageInput;
    private BitmapRegionDecoder(ImageReader reader,ImageInputStream in){
        this.reader=reader;this.imageInput=in;
    }
    public static BitmapRegionDecoder newInstance(InputStream input,boolean shared) throws IOException {
        ImageInputStream in=ImageIO.createImageInputStream(input);
        if(in==null)throw new IOException("No ImageIO input");
        Iterator<ImageReader> readers=ImageIO.getImageReaders(in);
        if(!readers.hasNext()){in.close();throw new IOException("No ImageIO codec for atlas");}
        ImageReader reader=readers.next();
        reader.setInput(in,true,true);
        return new BitmapRegionDecoder(reader,in);
    }
    public Bitmap decodeRegion(Rect rect,BitmapFactory.Options options) throws IOException {
        if(rect.width()<1||rect.height()<1)throw new IllegalArgumentException("Invalid atlas region");
        ImageReadParam params=reader.getDefaultReadParam();
        params.setSourceRegion(new Rectangle(rect.left,rect.top,rect.width(),rect.height()));
        BufferedImage out=reader.read(0,params);
        if(out==null)throw new IOException("Failed decoding portrait region");
        return new Bitmap(out);
    }
    public void recycle(){
        reader.dispose();
        try{imageInput.close();}catch(IOException ignore){}
    }
}
