package android.graphics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.Test;

/** O Canvas do PC desenha como o do Android (ângulos, recortes, camadas, filtros, perspectiva). */
public class CanvasPcTest {
    private BufferedImage img;
    private Canvas canvas;

    private Canvas novo(int w, int h) {
        img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        canvas = new Canvas();
        canvas.begin(g, img, w, h);
        return canvas;
    }

    private int px(int x, int y) { return img.getRGB(x, y) & 0xFFFFFF; }

    @Test
    public void arcsTurnClockwiseLikeAndroid() {
        Canvas c = novo(100, 100);
        Paint p = new Paint();
        p.setColor(Color.RED);
        // De 12 horas (-90°) até 3 horas: o quadrante de cima à direita.
        c.drawArc(0, 0, 100, 100, -90, 90, true, p);
        assertEquals(0xFF0000, px(75, 25));
        assertEquals(0, px(25, 25));
        assertEquals(0, px(75, 75));
    }

    @Test
    public void saveClipRestore() {
        Canvas c = novo(100, 100);
        Paint p = new Paint();
        p.setColor(Color.GREEN);
        int count = c.save();
        c.clipRect(0, 0, 50, 50);
        c.drawRect(0, 0, 100, 100, p);
        c.restoreToCount(count);
        assertEquals(0x00FF00, px(10, 10));
        assertEquals(0, px(70, 70));
        p.setColor(Color.BLUE);
        c.drawRect(0, 0, 100, 100, p);
        assertEquals(0x0000FF, px(70, 70));
    }

    @Test
    public void scaleAroundPivotMirrors() {
        Canvas c = novo(100, 10);
        Paint p = new Paint();
        p.setColor(Color.WHITE);
        c.save();
        c.scale(-1f, 1f, 50f, 0f);
        c.drawRect(0, 0, 10, 10, p);
        c.restore();
        assertEquals(0xFFFFFF, px(95, 5));
        assertEquals(0, px(5, 5));
    }

    @Test
    public void layerAlphaFadesEverythingInside() {
        Canvas c = novo(20, 20);
        Paint p = new Paint();
        p.setColor(Color.WHITE);
        int layer = c.saveLayerAlpha(0, 0, 20, 20, 128);
        c.drawRect(0, 0, 20, 20, p);
        c.drawRect(0, 0, 20, 20, p);
        c.restoreToCount(layer);
        int v = px(10, 10) & 0xFF;
        assertTrue("meio branco: " + v, Math.abs(v - 128) <= 2);
    }

    @Test
    public void hitFlashKeepsTransparentPixelsTransparent() {
        Bitmap b = Bitmap.createBitmap(2, 1, Bitmap.Config.ARGB_8888);
        b.setPixel(0, 0, 0xFF0000FF);
        b.setPixel(1, 0, 0x00000000);
        Canvas c = novo(2, 1);
        Paint p = new Paint();
        p.setColorFilter(new PorterDuffColorFilter(Color.argb(150, 255, 255, 255), PorterDuff.Mode.SRC_ATOP));
        c.drawBitmap(b, null, new RectF(0, 0, 2, 1), p);
        int flashed = px(0, 0);
        assertTrue("azul clareado", (flashed & 0xFF) == 0xFF && ((flashed >> 16) & 0xFF) > 100);
        assertEquals(0, px(1, 0));
    }

    @Test
    public void washedOutMatrixMatchesAndroidFormula() {
        ColorMatrix m = new ColorMatrix();
        m.setSaturation(0f);
        int gray = new ColorMatrixColorFilter(m).filter(Color.rgb(255, 0, 0));
        assertEquals(Color.red(gray), Color.green(gray));
        assertEquals(Math.round(0.213f * 255), Color.red(gray));
    }

    @Test
    public void polyToPolyMapsTheFourCorners() {
        Matrix m = new Matrix();
        float[] src = {0, 0, 10, 0, 10, 10, 0, 10};
        float[] dst = {5, 5, 50, 10, 45, 60, 0, 40};
        assertTrue(m.setPolyToPoly(src, 0, dst, 0, 4));
        float[] out = new float[8];
        m.mapPoints(out, src);
        for (int i = 0; i < 8; i++) assertEquals(dst[i], out[i], 1e-3f);
        assertTrue(!m.isAffine());
    }

    @Test
    public void fastScaledBitmapMatchesJava2d() {
        BufferedImage src = new BufferedImage(64, 48, BufferedImage.TYPE_INT_ARGB_PRE);
        for (int y = 0; y < 48; y++)
            for (int x = 0; x < 64; x++) src.setRGB(x, y, 0xFF000000 | (x * 4 << 16) | (y * 5 << 8) | ((x + y) * 2));
        Bitmap b = Bitmap.wrap(src);
        RectF dst = new RectF(3.2f, 4.7f, 3.2f + 101f, 4.7f + 77f);

        Canvas rapido = novo(120, 90);
        rapido.drawBitmap(b, null, dst, null);
        BufferedImage a = img;
        Canvas lento = novo(120, 90);
        lento.save();
        lento.rotate(1e-4f);  // com rotação o Canvas usa o caminho do próprio Java2D
        lento.drawBitmap(b, null, dst, null);
        lento.restore();
        BufferedImage bb = img;
        long diff = 0;
        int n = 0;
        for (int y = 10; y < 75; y++)
            for (int x = 10; x < 100; x++) {
                int p = a.getRGB(x, y), q = bb.getRGB(x, y);
                for (int s = 0; s < 24; s += 8) diff += Math.abs(((p >> s) & 0xFF) - ((q >> s) & 0xFF));
                n += 3;
            }
        assertTrue("diferença média " + (double) diff / n, (double) diff / n < 6.0);
    }

    @Test
    public void cachedTextLooksLikeOutlinedText() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(30);
        p.setColor(Color.WHITE);
        p.setTextAlign(Paint.Align.CENTER);
        p.setFakeBoldText(true);
        Canvas c = novo(200, 50);
        c.drawText("LUTA", 100, 38, p);
        BufferedImage a = img;
        long brancos = 0;
        for (int y = 0; y < 50; y++) for (int x = 0; x < 200; x++) if ((a.getRGB(x, y) & 0xFF) > 128) brancos++;
        assertTrue("texto desenhado: " + brancos, brancos > 300);
        // Centralizado: a tinta fica dos dois lados de x = 100.
        boolean esquerda = false, direita = false;
        for (int y = 0; y < 50; y++) {
            esquerda |= (a.getRGB(70, y) & 0xFF) > 128;
            direita |= (a.getRGB(130, y) & 0xFF) > 128;
        }
        assertTrue(esquerda && direita);
    }
}
