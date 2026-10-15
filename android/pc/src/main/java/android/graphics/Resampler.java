package android.graphics;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Redimensionamento bilinear rápido (inteiro, em paralelo) com cache pelo tamanho final em
 * pixels da tela.
 *
 * <p>O Java2D desenha imagem escalada com filtro por um caminho genérico lento; o cenário
 * cobre a tela várias vezes por quadro (camadas e reflexos). Aqui cada imagem é escalada
 * uma vez para o tamanho exato em que aparece, e o quadro só copia pixels. Enquanto a
 * câmera não muda o zoom, nada é recalculado.
 */
final class Resampler {
    /** -Dgameluta.escalaJava2d=true volta ao desenho escalado do próprio Java2D. */
    static final boolean ENABLED = !Boolean.getBoolean("gameluta.escalaJava2d");

    /** Pixels guardados no cache (~4 bytes cada). */
    private static final long BUDGET = Long.getLong("gameluta.cacheEscala", 24_000_000L);

    private static final class Key {
        final BufferedImage image;
        final int sx0, sy0, sx1, sy1, dw, dh;
        final boolean flipX, flipY;

        Key(BufferedImage image, int sx0, int sy0, int sx1, int sy1, int dw, int dh, boolean flipX, boolean flipY) {
            this.image = image;
            this.sx0 = sx0;
            this.sy0 = sy0;
            this.sx1 = sx1;
            this.sy1 = sy1;
            this.dw = dw;
            this.dh = dh;
            this.flipX = flipX;
            this.flipY = flipY;
        }

        @Override public boolean equals(Object o) {
            if (!(o instanceof Key)) return false;
            Key k = (Key) o;
            return k.image == image && k.sx0 == sx0 && k.sy0 == sy0 && k.sx1 == sx1 && k.sy1 == sy1
                && k.dw == dw && k.dh == dh && k.flipX == flipX && k.flipY == flipY;
        }

        @Override public int hashCode() {
            int h = System.identityHashCode(image);
            h = h * 31 + sx0; h = h * 31 + sy0; h = h * 31 + sx1; h = h * 31 + sy1;
            h = h * 31 + dw; h = h * 31 + dh;
            return h * 4 + (flipX ? 2 : 0) + (flipY ? 1 : 0);
        }
    }

    /** Imagem escalada, já sem as margens transparentes: desenhar em (x + dx, y + dy). */
    static final class Scaled {
        final BufferedImage image;
        final int dx, dy;

        Scaled(BufferedImage image, int dx, int dy) {
            this.image = image;
            this.dx = dx;
            this.dy = dy;
        }
    }

    private static long cachedPixels;
    private static final Map<Key, Scaled> CACHE = new LinkedHashMap<Key, Scaled>(256, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Key, Scaled> eldest) {
            if (cachedPixels <= BUDGET) return false;
            BufferedImage img = eldest.getValue().image;
            cachedPixels -= (long) img.getWidth() * img.getHeight();
            return true;
        }
    };

    private Resampler() {}

    /** O recorte {@code [sx0,sx1) x [sy0,sy1)} de {@code image} com {@code dw}x{@code dh} pixels. */
    static synchronized Scaled get(BufferedImage image, int sx0, int sy0, int sx1, int sy1,
                                   int dw, int dh, boolean flipX, boolean flipY) {
        Key key = new Key(image, sx0, sy0, sx1, sy1, dw, dh, flipX, flipY);
        Scaled out = CACHE.get(key);
        if (out != null) return out;
        out = compact(scale(image, sx0, sy0, sx1, sy1, dw, dh, flipX, flipY));
        cachedPixels += (long) out.image.getWidth() * out.image.getHeight();
        CACHE.put(key, out);
        return out;
    }

    /**
     * Toda opaca: vira TYPE_INT_RGB (o Java2D copia sem misturar). Com bordas transparentes
     * largas: recorta, para não misturar pixels invisíveis todo quadro.
     */
    static Scaled compact(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[] px = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        int minX = w, minY = h, maxX = -1, maxY = -1;
        boolean opaque = true;
        for (int y = 0; y < h; y++) {
            int row = y * w;
            for (int x = 0; x < w; x++) {
                int a = px[row + x] >>> 24;
                if (a != 0xFF) opaque = false;
                if (a != 0) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    maxY = y;
                }
            }
        }
        if (opaque) {
            BufferedImage rgb = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            System.arraycopy(px, 0, ((DataBufferInt) rgb.getRaster().getDataBuffer()).getData(), 0, px.length);
            return new Scaled(rgb, 0, 0);
        }
        if (maxX < 0) return new Scaled(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB_PRE), 0, 0);
        int cw = maxX - minX + 1, ch = maxY - minY + 1;
        if ((long) cw * ch > (long) w * h * 85 / 100) return new Scaled(img, 0, 0);
        BufferedImage cut = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB_PRE);
        int[] out = ((DataBufferInt) cut.getRaster().getDataBuffer()).getData();
        for (int y = 0; y < ch; y++) System.arraycopy(px, (minY + y) * w + minX, out, y * cw, cw);
        return new Scaled(cut, minX, minY);
    }

    static BufferedImage scale(BufferedImage image, int sx0, int sy0, int sx1, int sy1,
                               int dw, int dh, boolean flipX, boolean flipY) {
        final int[] src = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        final int stride = image.getWidth();
        final int sw = sx1 - sx0, sh = sy1 - sy0;
        BufferedImage result = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_ARGB_PRE);
        final int[] dst = ((DataBufferInt) result.getRaster().getDataBuffer()).getData();
        // Colunas: índice dos dois vizinhos e peso (0..256) do segundo.
        final int[] xa = new int[dw], xb = new int[dw], xf = new int[dw];
        for (int x = 0; x < dw; x++) {
            int dx = flipX ? dw - 1 - x : x;
            double u = (dx + 0.5) * sw / dw - 0.5;
            int u0 = (int) Math.floor(u);
            int f = (int) Math.round((u - u0) * 256);
            int a = Math.max(0, Math.min(sw - 1, u0)), b = Math.max(0, Math.min(sw - 1, u0 + 1));
            xa[x] = sx0 + a;
            xb[x] = sx0 + b;
            xf[x] = f;
        }
        IntStream rows = IntStream.range(0, dh);
        if ((long) dw * dh > 60_000) rows = rows.parallel();
        rows.forEach(y -> {
            int dy = flipY ? dh - 1 - y : y;
            double v = (dy + 0.5) * sh / dh - 0.5;
            int v0 = (int) Math.floor(v);
            int fy = (int) Math.round((v - v0) * 256);
            int ra = (sy0 + Math.max(0, Math.min(sh - 1, v0))) * stride;
            int rb = (sy0 + Math.max(0, Math.min(sh - 1, v0 + 1))) * stride;
            int iy = 256 - fy;
            int o = y * dw;
            for (int x = 0; x < dw; x++) {
                int fx = xf[x], ix = 256 - fx;
                int p00 = src[ra + xa[x]], p10 = src[ra + xb[x]];
                int p01 = src[rb + xa[x]], p11 = src[rb + xb[x]];
                if ((p00 | p10 | p01 | p11) == 0) { dst[o + x] = 0; continue; }
                // Dois canais por vez (AARRGGBB → 00RR00BB e 00AA00GG), pesos de 8 bits.
                int top02 = (((p00 & 0xFF00FF) * ix + (p10 & 0xFF00FF) * fx) >>> 8) & 0xFF00FF;
                int top13 = ((((p00 >>> 8) & 0xFF00FF) * ix + ((p10 >>> 8) & 0xFF00FF) * fx) >>> 8) & 0xFF00FF;
                int bot02 = (((p01 & 0xFF00FF) * ix + (p11 & 0xFF00FF) * fx) >>> 8) & 0xFF00FF;
                int bot13 = ((((p01 >>> 8) & 0xFF00FF) * ix + ((p11 >>> 8) & 0xFF00FF) * fx) >>> 8) & 0xFF00FF;
                int c02 = ((top02 * iy + bot02 * fy) >>> 8) & 0xFF00FF;
                int c13 = ((top13 * iy + bot13 * fy) >>> 8) & 0xFF00FF;
                dst[o + x] = c02 | (c13 << 8);
            }
        });
        return result;
    }
}
