package android.graphics;

import android.content.res.Resources;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** PNG e JPG pelo ImageIO; WEBP pelo plugin TwelveMonkeys (dependência do módulo :pc). */
public class BitmapFactory {
    public static class Options {
        public boolean inJustDecodeBounds;
        public int inSampleSize = 1;
        public boolean inScaled = true;
        public Bitmap.Config inPreferredConfig = Bitmap.Config.ARGB_8888;
        public boolean inMutable;
        public int outWidth = -1;
        public int outHeight = -1;
        public String outMimeType;
    }

    public static Bitmap decodeStream(InputStream is) { return decodeStream(is, null, null); }

    public static Bitmap decodeStream(InputStream is, Rect outPadding, Options opts) {
        if (is == null) return null;
        try {
            byte[] data = readAll(is);
            if (opts != null && opts.inJustDecodeBounds) {
                readBounds(data, opts);
                return null;
            }
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) return null;
            int sample = opts == null ? 1 : Math.max(1, opts.inSampleSize);
            if (sample > 1) image = downsample(image, sample);
            if (opts != null) {
                opts.outWidth = image.getWidth();
                opts.outHeight = image.getHeight();
            }
            return new Bitmap(image);
        } catch (IOException e) {
            return null;
        }
    }

    public static Bitmap decodeResource(Resources res, int id, Options opts) {
        if (opts == null || (!opts.inJustDecodeBounds && opts.inSampleSize <= 1)) {
            Bitmap b = res.decodeDrawable(id);
            if (opts != null && b != null) {
                opts.outWidth = b.getWidth();
                opts.outHeight = b.getHeight();
            }
            return b;
        }
        try (InputStream in = new BufferedInputStream(res.openRawResource(id))) {
            return decodeStream(in, null, opts);
        } catch (IOException e) {
            return null;
        }
    }

    public static Bitmap decodeResource(Resources res, int id) { return decodeResource(res, id, null); }

    public static Bitmap decodeFile(String pathName) {
        try (InputStream in = new BufferedInputStream(java.nio.file.Files.newInputStream(java.nio.file.Paths.get(pathName)))) {
            return decodeStream(in);
        } catch (IOException e) {
            return null;
        }
    }

    public static Bitmap decodeByteArray(byte[] data, int offset, int length) {
        return decodeStream(new ByteArrayInputStream(data, offset, length));
    }

    private static void readBounds(byte[] data, Options opts) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) return;
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                opts.outWidth = reader.getWidth(0);
                opts.outHeight = reader.getHeight(0);
                opts.outMimeType = "image/" + reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
            } finally {
                reader.dispose();
            }
        }
    }

    private static BufferedImage downsample(BufferedImage image, int sample) {
        int w = Math.max(1, image.getWidth() / sample), h = Math.max(1, image.getHeight() / sample);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(image, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private static byte[] readAll(InputStream is) throws IOException {
        return is.readAllBytes();
    }
}
