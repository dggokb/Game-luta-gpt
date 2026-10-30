package android.opengl;

import android.graphics.Bitmap;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.SinglePixelPackedSampleModel;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * PC: sobe um {@link Bitmap} para a textura ligada. O bitmap do PC já é ARGB com o alfa
 * multiplicado (o mesmo que o GLUtils do Android sobe): os inteiros vão direto, como BGRA.
 */
public final class GLUtils {
    private GLUtils() {}

    public static void texImage2D(int target, int level, Bitmap bitmap, int border) {
        BufferedImage img = bitmap.image();
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
        GL11.glTexImage2D(target, level, GL11.GL_RGBA8, img.getWidth(), img.getHeight(), border,
            GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixels(img));
    }

    public static void texSubImage2D(int target, int level, int x, int y, Bitmap bitmap) {
        BufferedImage img = bitmap.image();
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
        GL11.glTexSubImage2D(target, level, x, y, img.getWidth(), img.getHeight(),
            GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixels(img));
    }

    /** Os pixels ARGB pré-multiplicados, linha a linha, sem sobra (cópia só quando precisa). */
    private static int[] pixels(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        if (img.getType() == BufferedImage.TYPE_INT_ARGB_PRE
            && img.getRaster().getDataBuffer() instanceof DataBufferInt
            && img.getSampleModel() instanceof SinglePixelPackedSampleModel
            && ((SinglePixelPackedSampleModel) img.getSampleModel()).getScanlineStride() == w
            && img.getRaster().getSampleModelTranslateX() == 0 && img.getRaster().getSampleModelTranslateY() == 0) {
            int[] dados = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
            if (dados.length == w * h) return dados;
        }
        int[] argb = img.getRGB(0, 0, w, h, null, 0, w);
        for (int i = 0; i < argb.length; i++) {
            int p = argb[i], a = p >>> 24;
            if (a == 255) continue;
            if (a == 0) {
                argb[i] = 0;
                continue;
            }
            int r = ((p >> 16) & 0xFF) * a / 255, g = ((p >> 8) & 0xFF) * a / 255, b = (p & 0xFF) * a / 255;
            argb[i] = a << 24 | r << 16 | g << 8 | b;
        }
        return argb;
    }
}
