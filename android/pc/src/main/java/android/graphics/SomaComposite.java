package android.graphics;

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * Mistura somada no desenho do PC (o PorterDuff.Mode.ADD do Android, que o Java2D não tem): a cor
 * da imagem, já multiplicada pelo alfa dela e pelo do Paint, soma com o que está embaixo, e o alfa
 * de baixo fica. É o mesmo resultado do OpenGL do jogo (com.gamelutagpt.Mistura).
 */
final class SomaComposite implements Composite {
    private final float alfa;

    SomaComposite(float alfa) {
        this.alfa = Math.max(0f, Math.min(1f, alfa));
    }

    @Override
    public CompositeContext createContext(ColorModel origem, ColorModel destino, RenderingHints dicas) {
        final boolean origemPre = origem.isAlphaPremultiplied();
        return new CompositeContext() {
            @Override public void dispose() {}

            @Override
            public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
                int w = Math.min(src.getWidth(), dstIn.getWidth()), h = Math.min(src.getHeight(), dstIn.getHeight());
                int[] s = new int[w], d = new int[w];
                int k = Math.round(alfa * 256f);
                for (int y = 0; y < h; y++) {
                    src.getDataElements(0, y, w, 1, s);
                    dstIn.getDataElements(0, y, w, 1, d);
                    for (int x = 0; x < w; x++) {
                        int p = s[x], a = p >>> 24;
                        if (a == 0) continue;
                        int r = (p >> 16) & 0xFF, g = (p >> 8) & 0xFF, b = p & 0xFF;
                        if (!origemPre) { r = r * a / 255; g = g * a / 255; b = b * a / 255; }
                        r = r * k >> 8; g = g * k >> 8; b = b * k >> 8;
                        int q = d[x];
                        int dr = Math.min(255, ((q >> 16) & 0xFF) + r);
                        int dg = Math.min(255, ((q >> 8) & 0xFF) + g);
                        int db = Math.min(255, (q & 0xFF) + b);
                        int da = Math.max(q >>> 24, Math.max(dr, Math.max(dg, db)));
                        d[x] = (da << 24) | (dr << 16) | (dg << 8) | db;
                    }
                    dstOut.setDataElements(0, y, w, 1, d);
                }
            }
        };
    }
}
