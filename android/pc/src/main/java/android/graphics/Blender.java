package android.graphics;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.stream.IntStream;

/**
 * Mistura "fonte sobre destino" (SrcOver) de uma imagem ARGB pré-multiplicada em outra,
 * dividida por linhas entre os núcleos do processador. O cenário cobre a tela várias vezes
 * por quadro, e o laço do Java2D usa um núcleo só.
 */
final class Blender {
    /** -Dgameluta.misturaJava2d=true volta ao laço do Java2D. */
    static final boolean ENABLED = !Boolean.getBoolean("gameluta.misturaJava2d");
    private static final int PARALLEL_PIXELS = 40_000;

    private Blender() {}

    /** Os pixels da imagem podem ser lidos/escritos direto? */
    static boolean supports(BufferedImage img) {
        int t = img.getType();
        return t == BufferedImage.TYPE_INT_ARGB_PRE || t == BufferedImage.TYPE_INT_RGB;
    }

    /**
     * Mistura {@code src} em {@code dst} com o canto em ({@code dx}, {@code dy}), só dentro do
     * retângulo de recorte {@code [cx0,cx1) x [cy0,cy1)} do destino. {@code extraAlpha} 0..255.
     */
    static void blend(BufferedImage src, BufferedImage dst, int dx, int dy,
                      int cx0, int cy0, int cx1, int cy1, int extraAlpha) {
        int sw = src.getWidth(), sh = src.getHeight();
        int x0 = Math.max(Math.max(dx, cx0), 0), y0 = Math.max(Math.max(dy, cy0), 0);
        int x1 = Math.min(Math.min(dx + sw, cx1), dst.getWidth()), y1 = Math.min(Math.min(dy + sh, cy1), dst.getHeight());
        if (x1 <= x0 || y1 <= y0 || extraAlpha <= 0) return;
        final int[] s = ((DataBufferInt) src.getRaster().getDataBuffer()).getData();
        final int[] d = ((DataBufferInt) dst.getRaster().getDataBuffer()).getData();
        final boolean srcOpaque = src.getType() == BufferedImage.TYPE_INT_RGB;
        final boolean dstRgb = dst.getType() == BufferedImage.TYPE_INT_RGB;
        final int dw = dst.getWidth();
        final int w = x1 - x0;
        final int fx0 = x0, fdx = dx, fdy = dy;
        final int ea = extraAlpha;
        IntStream rows = IntStream.range(y0, y1);
        if ((long) w * (y1 - y0) >= PARALLEL_PIXELS) rows = rows.parallel();
        rows.forEach(y -> {
            int si = (y - fdy) * sw + (fx0 - fdx);
            int di = y * dw + fx0;
            if (srcOpaque && ea == 255) {
                if (dstRgb) {
                    System.arraycopy(s, si, d, di, w);
                } else {
                    for (int i = 0; i < w; i++) d[di + i] = s[si + i] | 0xFF000000;
                }
                return;
            }
            for (int i = 0; i < w; i++) {
                int p = s[si + i];
                if (srcOpaque) p |= 0xFF000000;
                if (ea != 255) p = scale(p, ea);
                int a = p >>> 24;
                if (a == 0) continue;
                if (a == 255) {
                    d[di + i] = p;
                    continue;
                }
                int q = d[di + i];
                if (dstRgb) q |= 0xFF000000;
                int ia = 255 - a;
                int rb = (p & 0xFF00FF) + ((((q & 0xFF00FF) * ia + 0x800080) + (((q & 0xFF00FF) * ia + 0x800080) >>> 8 & 0xFF00FF)) >>> 8 & 0xFF00FF);
                int ag = ((p >>> 8) & 0xFF00FF) + (((((q >>> 8) & 0xFF00FF) * ia + 0x800080) + ((((q >>> 8) & 0xFF00FF) * ia + 0x800080) >>> 8 & 0xFF00FF)) >>> 8 & 0xFF00FF);
                d[di + i] = (rb & 0xFF00FF) | ((ag & 0xFF00FF) << 8);
            }
        });
    }

    /** Multiplica os quatro canais pré-multiplicados por {@code ea}/255. */
    private static int scale(int p, int ea) {
        int rb = ((p & 0xFF00FF) * ea + 0x800080);
        rb = ((rb + ((rb >>> 8) & 0xFF00FF)) >>> 8) & 0xFF00FF;
        int ag = (((p >>> 8) & 0xFF00FF) * ea + 0x800080);
        ag = ((ag + ((ag >>> 8) & 0xFF00FF)) >>> 8) & 0xFF00FF;
        return rb | (ag << 8);
    }
}
