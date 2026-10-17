package android.graphics;

import android.content.res.Resources;
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
        return reduzir(image, w, h);
    }

    /**
     * Reduz por média de área (cada pixel novo é a média ponderada dos pixels que ele cobre),
     * em ARGB pré-multiplicado: o mesmo tipo de filtro do libwebp ao decodificar em escala.
     */
    static BufferedImage reduzir(BufferedImage image, int w, int h) {
        BufferedImage src = Bitmap.toPremultiplied(image);
        int sw = src.getWidth(), sh = src.getHeight();
        int[] in = ((java.awt.image.DataBufferInt) src.getRaster().getDataBuffer()).getData();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        int[] dst = ((java.awt.image.DataBufferInt) out.getRaster().getDataBuffer()).getData();
        // Pesos horizontais: para cada coluna nova, o intervalo [x0, x1) da origem que ela cobre.
        double fx = (double) sw / w, fy = (double) sh / h;
        int[] x0 = new int[w], x1 = new int[w];
        double[][] wx = new double[w][];
        for (int x = 0; x < w; x++) {
            double a = x * fx, b = Math.min(sw, (x + 1) * fx);
            x0[x] = (int) Math.floor(a);
            x1[x] = Math.min(sw, (int) Math.ceil(b));
            wx[x] = new double[x1[x] - x0[x]];
            for (int i = x0[x]; i < x1[x]; i++) wx[x][i - x0[x]] = Math.min(b, i + 1) - Math.max(a, i);
        }
        java.util.stream.IntStream.range(0, h).parallel().forEach(y -> {
            double a = y * fy, b = Math.min(sh, (y + 1) * fy);
            int y0 = (int) Math.floor(a), y1 = Math.min(sh, (int) Math.ceil(b));
            double[] acc = new double[4 * w];
            for (int sy = y0; sy < y1; sy++) {
                double wy = Math.min(b, sy + 1) - Math.max(a, sy);
                int row = sy * sw;
                for (int x = 0; x < w; x++) {
                    double ca = 0, cr = 0, cg = 0, cb = 0;
                    double[] ws = wx[x];
                    for (int i = 0; i < ws.length; i++) {
                        int p = in[row + x0[x] + i];
                        double k = ws[i];
                        ca += (p >>> 24) * k; cr += ((p >> 16) & 255) * k; cg += ((p >> 8) & 255) * k; cb += (p & 255) * k;
                    }
                    acc[4 * x] += ca * wy; acc[4 * x + 1] += cr * wy; acc[4 * x + 2] += cg * wy; acc[4 * x + 3] += cb * wy;
                }
            }
            double area = fx * fy;
            for (int x = 0; x < w; x++) {
                int pa = clamp(acc[4 * x] / area), pr = clamp(acc[4 * x + 1] / area);
                int pg = clamp(acc[4 * x + 2] / area), pb = clamp(acc[4 * x + 3] / area);
                dst[y * w + x] = pa << 24 | Math.min(pa, pr) << 16 | Math.min(pa, pg) << 8 | Math.min(pa, pb);
            }
        });
        return out;
    }

    private static int clamp(double v) { return (int) Math.max(0, Math.min(255, Math.round(v))); }

    private static byte[] readAll(InputStream is) throws IOException {
        return is.readAllBytes();
    }
}
