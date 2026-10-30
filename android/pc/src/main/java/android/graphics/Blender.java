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

    /**
     * O recorte {@code [sx0,sx1) x [sy0,sy1)} de {@code src} ampliado/reduzido (bilinear) para
     * {@code dw}x{@code dh} em ({@code dx0}, {@code dy0}) e misturado no destino, numa passada só e
     * pelos núcleos todos. Para a imagem cujo tamanho muda a cada quadro (o zoom da câmera): o
     * Java2D escalava por um caminho de uma thread só (~7 ms por camada de tela cheia a 720p).
     */
    static void blendScaled(BufferedImage src, int sx0, int sy0, int sx1, int sy1, BufferedImage dst,
                            int dx0, int dy0, int dw, int dh, boolean flipX, boolean flipY,
                            int cx0, int cy0, int cx1, int cy1, int extraAlpha) {
        int x0 = Math.max(Math.max(dx0, cx0), 0), y0 = Math.max(Math.max(dy0, cy0), 0);
        int x1 = Math.min(Math.min(dx0 + dw, cx1), dst.getWidth()), y1 = Math.min(Math.min(dy0 + dh, cy1), dst.getHeight());
        if (x1 <= x0 || y1 <= y0 || extraAlpha <= 0) return;
        final int[] s = ((DataBufferInt) src.getRaster().getDataBuffer()).getData();
        final int[] d = ((DataBufferInt) dst.getRaster().getDataBuffer()).getData();
        final int stride = src.getWidth();
        final boolean srcRgb = src.getType() == BufferedImage.TYPE_INT_RGB;
        final boolean dstRgb = dst.getType() == BufferedImage.TYPE_INT_RGB;
        final int sw = sx1 - sx0, sh = sy1 - sy0, w = x1 - x0, ddw = dst.getWidth();
        // Colunas: os dois vizinhos na origem e o peso (0..256) do segundo.
        final int[] xa = new int[w], xb = new int[w], xf = new int[w];
        for (int i = 0; i < w; i++) {
            int lx = x0 + i - dx0;
            if (flipX) lx = dw - 1 - lx;
            double u = (lx + 0.5) * sw / dw - 0.5;
            int u0 = (int) Math.floor(u);
            xf[i] = (int) Math.round((u - u0) * 256);
            xa[i] = sx0 + Math.max(0, Math.min(sw - 1, u0));
            xb[i] = sx0 + Math.max(0, Math.min(sw - 1, u0 + 1));
        }
        final int fx0 = x0, fdy0 = dy0, ea = extraAlpha;
        IntStream rows = IntStream.range(y0, y1);
        if ((long) w * (y1 - y0) >= PARALLEL_PIXELS) rows = rows.parallel();
        rows.forEach(y -> {
            int ly = y - fdy0;
            if (flipY) ly = dh - 1 - ly;
            double v = (ly + 0.5) * sh / dh - 0.5;
            int v0 = (int) Math.floor(v);
            int fy = (int) Math.round((v - v0) * 256), iy = 256 - fy;
            int ra = (sy0 + Math.max(0, Math.min(sh - 1, v0))) * stride;
            int rb = (sy0 + Math.max(0, Math.min(sh - 1, v0 + 1))) * stride;
            int di = y * ddw + fx0;
            for (int i = 0; i < w; i++) {
                int p00 = s[ra + xa[i]], p10 = s[ra + xb[i]], p01 = s[rb + xa[i]], p11 = s[rb + xb[i]];
                if (srcRgb) {
                    p00 |= 0xFF000000; p10 |= 0xFF000000; p01 |= 0xFF000000; p11 |= 0xFF000000;
                } else if ((p00 | p10 | p01 | p11) == 0) {
                    continue;
                }
                int fx = xf[i], ix = 256 - fx;
                // Dois canais por vez (00RR00BB e 00AA00GG), pesos de 8 bits.
                int top02 = (((p00 & 0xFF00FF) * ix + (p10 & 0xFF00FF) * fx) >>> 8) & 0xFF00FF;
                int top13 = ((((p00 >>> 8) & 0xFF00FF) * ix + ((p10 >>> 8) & 0xFF00FF) * fx) >>> 8) & 0xFF00FF;
                int bot02 = (((p01 & 0xFF00FF) * ix + (p11 & 0xFF00FF) * fx) >>> 8) & 0xFF00FF;
                int bot13 = ((((p01 >>> 8) & 0xFF00FF) * ix + ((p11 >>> 8) & 0xFF00FF) * fx) >>> 8) & 0xFF00FF;
                int p = (((top02 * iy + bot02 * fy) >>> 8) & 0xFF00FF) | ((((top13 * iy + bot13 * fy) >>> 8) & 0xFF00FF) << 8);
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
                int qrb = (q & 0xFF00FF) * ia + 0x800080;
                int qag = ((q >>> 8) & 0xFF00FF) * ia + 0x800080;
                int orb = (p & 0xFF00FF) + (((qrb + ((qrb >>> 8) & 0xFF00FF)) >>> 8) & 0xFF00FF);
                int oag = ((p >>> 8) & 0xFF00FF) + (((qag + ((qag >>> 8) & 0xFF00FF)) >>> 8) & 0xFF00FF);
                d[di + i] = (orb & 0xFF00FF) | ((oag & 0xFF00FF) << 8);
            }
        });
    }

    /**
     * Pinta {@code [x0,x1) x [y0,y1)} do destino com uma cor pré-multiplicada por linha
     * ({@code cores[y - y0]}), "fonte sobre destino", pelos núcleos todos: os retângulos
     * translúcidos de tela cheia (escurecer, clarão) e os degradês verticais (neblina, água).
     */
    static void fillRows(BufferedImage dst, int x0, int y0, int x1, int y1, int[] cores) {
        final int[] d = ((DataBufferInt) dst.getRaster().getDataBuffer()).getData();
        final boolean dstRgb = dst.getType() == BufferedImage.TYPE_INT_RGB;
        final int dw = dst.getWidth(), fx0 = x0, fx1 = x1, fy0 = y0;
        IntStream rows = IntStream.range(y0, y1);
        if ((long) (x1 - x0) * (y1 - y0) >= PARALLEL_PIXELS) rows = rows.parallel();
        rows.forEach(y -> {
            int p = cores[y - fy0];
            int a = p >>> 24;
            if (a == 0) return;
            int base = y * dw;
            if (a == 255) {
                java.util.Arrays.fill(d, base + fx0, base + fx1, p);
                return;
            }
            int ia = 255 - a, prb = p & 0xFF00FF, pag = (p >>> 8) & 0xFF00FF;
            for (int i = base + fx0; i < base + fx1; i++) {
                int q = d[i];
                if (dstRgb) q |= 0xFF000000;
                int qrb = (q & 0xFF00FF) * ia + 0x800080;
                int qag = ((q >>> 8) & 0xFF00FF) * ia + 0x800080;
                int orb = prb + (((qrb + ((qrb >>> 8) & 0xFF00FF)) >>> 8) & 0xFF00FF);
                int oag = pag + (((qag + ((qag >>> 8) & 0xFF00FF)) >>> 8) & 0xFF00FF);
                d[i] = (orb & 0xFF00FF) | ((oag & 0xFF00FF) << 8);
            }
        });
    }

    /** {@code argb} (não pré-multiplicado) vezes {@code extra}/255, pré-multiplicado. */
    static int premultiplicar(int argb, int extra) {
        int a = ((argb >>> 24) * extra + 127) / 255;
        int r = (((argb >> 16) & 0xFF) * a + 127) / 255, g = (((argb >> 8) & 0xFF) * a + 127) / 255, b = ((argb & 0xFF) * a + 127) / 255;
        return (a << 24) | (r << 16) | (g << 8) | b;
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
