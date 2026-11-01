package com.gamelutagpt;

import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.SurfaceView;
import java.awt.image.BufferedImage;
import java.io.File;
import org.junit.Assume;
import org.junit.Test;

/**
 * Cor alternativa (o mesmo personagem dos dois lados): pelo OpenGL de verdade, o idle de vários
 * lutadores com as cores normais (em cima) e com {@link Filtro#corAlternativa} (embaixo). A cor
 * tem de mudar e o desenho continuar no mesmo lugar (só a cor muda, nada de animação nova).
 * Salva a comparação em {@code build/cor-alternativa.png}. Precisa de OpenGL 3.3; sem ele é pulado:
 * {@code XDG_RUNTIME_DIR=/tmp/xdg LIBGL_ALWAYS_SOFTWARE=1 xvfb-run -a gradle :pc:test --tests '*CorAlternativa*'}.
 */
public class CorAlternativaGlPcTest {
    private static final String[] IDS = {"player_base", "p02", "p03", "p05", "p09", "p13", "p14", "p15"};
    private static final int CEL = 220, W = CEL * IDS.length, H = CEL * 2;

    @Test
    public void aCorMudaEODesenhoFicaNoLugar() throws Exception {
        Assume.assumeTrue(DesenhoGl.permitido());
        Bitmap[] folhas = new Bitmap[IDS.length];
        for (int i = 0; i < IDS.length; i++) {
            File f = new File("../app/src/main/res/drawable-nodpi/" + IDS[i] + "_idle.webp");
            if (!f.exists()) f = new File("../app/src/main/res/drawable-nodpi/" + IDS[i] + "_idle.png");
            folhas[i] = BitmapFactory.decodeFile(f.getPath());
        }
        BufferedImage[] ultimo = new BufferedImage[1];
        SurfaceView sv = new SurfaceView(null);
        sv.attach(new SurfaceView.Target() {
            @Override public Canvas beginFrame() { return null; }
            @Override public void endFrame(Canvas canvas) {}
            @Override public int larguraGl() { return W; }
            @Override public int alturaGl() { return H; }
            @Override public void mostrarPronto(BufferedImage imagem) {
                BufferedImage copia = new BufferedImage(imagem.getWidth(), imagem.getHeight(), BufferedImage.TYPE_INT_RGB);
                copia.getGraphics().drawImage(imagem, 0, 0, null);
                ultimo[0] = copia;
            }
        }, W, H);
        DesenhoGl gl = new DesenhoGl(sv, sv, pronta -> {});
        Paint normal = new Paint(Paint.FILTER_BITMAP_FLAG), alt = new Paint(Paint.FILTER_BITMAP_FLAG);
        alt.setColorFilter(Filtro.corAlternativa(null, GameView.COR_ALT_GIRO, GameView.COR_ALT_ESCUROS,
            GameView.COR_ALT_MATIZ_ESCUROS).canvas);
        try {
            for (int quadro = 0; quadro < 60; quadro++) {
                Canvas c = gl.comecarQuadro();
                Assume.assumeTrue("sem OpenGL: " + gl.motivoFalha(), c != null);
                c.drawColor(0xFF808890);
                for (int i = 0; i < IDS.length; i++) {
                    Bitmap b = folhas[i];
                    if (b == null) continue;
                    // O primeiro pedaço da folha (um ou dois quadros do idle), encaixado na célula.
                    int lado = Math.min(b.getWidth(), b.getHeight());
                    Rect src = new Rect(0, 0, Math.min(b.getWidth(), lado), lado);
                    c.drawBitmap(b, src, new Rect(i * CEL, 0, (i + 1) * CEL, CEL), normal);
                    c.drawBitmap(b, src, new Rect(i * CEL, CEL, (i + 1) * CEL, 2 * CEL), alt);
                }
                gl.terminarQuadro();
                Thread.sleep(2);
            }
        } finally {
            gl.soltarThread();
            gl.liberar();
        }
        BufferedImage img = ultimo[0];
        assertTrue("nenhum quadro do OpenGL", img != null);
        javax.imageio.ImageIO.write(img, "png", new File("build/cor-alternativa.png"));
        int mudou = 0, desenho = 0, forma = 0;
        for (int y = 0; y < CEL; y++) {
            for (int x = 0; x < W; x++) {
                int a = img.getRGB(x, y) & 0xFFFFFF, b = img.getRGB(x, y + CEL) & 0xFFFFFF;
                boolean fundoA = a == 0x808890, fundoB = b == 0x808890;
                if (!fundoA) desenho++;
                if (fundoA == fundoB) forma++;
                if (!fundoA && Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF)) + Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF)) +
                    Math.abs((a & 0xFF) - (b & 0xFF)) > 40) mudou++;
            }
        }
        assertTrue("sem desenho: " + desenho, desenho > 5000);
        assertTrue("a forma mudou: " + forma + " de " + (W * CEL), forma > W * CEL * 0.98);
        assertTrue("a cor quase não mudou: " + mudou + " de " + desenho, mudou > desenho * 0.15);
    }
}
