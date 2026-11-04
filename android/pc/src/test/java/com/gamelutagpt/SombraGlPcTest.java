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
 * A sombra de verdade ({@link Filtro#sombra}) pelo OpenGL: o idle deitado no chão claro, como o
 * {@code GameView} desenha a sombra comprida. Em cima a silhueta lisa (a de antes), embaixo a
 * nova: tem de escurecer o chão tanto quanto perto dos pés e ir clareando até a ponta (a cabeça).
 * Salva em {@code build/sombra-gl.png}. Precisa de OpenGL 3.3; sem ele é pulado.
 */
public class SombraGlPcTest {
    private static final int W = 900, H = 520, CHAO = 0xFFD8B890, SOLA = 120, ALTURA = 300;
    private static final float FRENTE = 0.35f;

    @Test
    public void duraNosPesEClareandoNaPonta() throws Exception {
        Assume.assumeTrue(DesenhoGl.permitido());
        File f = new File("../app/src/main/res/drawable-nodpi/player_base_idle.webp");
        if (!f.exists()) f = new File("../app/src/main/res/drawable-nodpi/player_base_idle.png");
        Bitmap b = BitmapFactory.decodeFile(f.getPath());
        Assume.assumeTrue("sem a folha do idle", b != null);
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
        Paint lisa = new Paint(Paint.FILTER_BITMAP_FLAG), nova = new Paint(Paint.FILTER_BITMAP_FLAG);
        lisa.setColorFilter(Filtro.silhueta(0xFF000000).canvas);
        nova.setColorFilter(Filtro.sombra(0x1C0A14, 0.55f).canvas);
        lisa.setAlpha(150);
        nova.setAlpha(150);
        // O primeiro quadro da folha do idle (328 x 470).
        Rect src = new Rect(0, 0, 328, 470);
        try {
            for (int quadro = 0; quadro < 40; quadro++) {
                Canvas c = gl.comecarQuadro();
                Assume.assumeTrue("sem OpenGL: " + gl.motivoFalha(), c != null);
                c.drawColor(CHAO);
                for (int linha = 0; linha < 2; linha++) {
                    // A sombra deitada: pés na linha "sola", cabeça indo para a direita e para baixo.
                    float sola = SOLA + linha * 260f;
                    float lado2 = 0.9f, frente = FRENTE;
                    android.graphics.Matrix m = new android.graphics.Matrix();
                    m.setValues(new float[] {1f, -lado2, lado2 * sola + 60f, 0f, -frente, sola * (1f + frente), 0f, 0f, 1f});
                    c.save();
                    c.concat(m);
                    c.drawBitmap(b, src, new Rect(0, (int)sola - ALTURA, 210, (int)sola), linha == 0 ? lisa : nova);
                    c.restore();
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
        javax.imageio.ImageIO.write(img, "png", new File("build/sombra-gl.png"));
        // Escuro perto dos pés (o primeiro terço da sombra deitada) e na ponta (o último), na
        // silhueta lisa e na nova: a nova segura o escuro nos pés e perde na ponta.
        int comprimento = Math.round(ALTURA * FRENTE);
        double lisaPes = escuro(img, SOLA, SOLA + comprimento / 3), lisaPonta = escuro(img, SOLA + 2 * comprimento / 3, SOLA + comprimento);
        double novaPes = escuro(img, SOLA + 260, SOLA + 260 + comprimento / 3);
        double novaPonta = escuro(img, SOLA + 260 + 2 * comprimento / 3, SOLA + 260 + comprimento);
        double pes = novaPes / lisaPes, ponta = novaPonta / lisaPonta;
        System.out.printf("sombra nova / lisa: pés %.2f, ponta %.2f%n", pes, ponta);
        assertTrue("a nova quase não escurece perto dos pés: " + pes, pes > 0.75);
        assertTrue("a nova não clareia na ponta: " + ponta + " (pés " + pes + ")", ponta < pes * 0.85);
    }

    /** Quanto o chão escureceu (soma da queda do brilho) entre as linhas y0 e y1. */
    private static double escuro(BufferedImage img, int y0, int y1) {
        int chao = (CHAO >> 16 & 0xFF) + (CHAO >> 8 & 0xFF) + (CHAO & 0xFF);
        double soma = 0;
        for (int y = y0; y < y1; y++) {
            for (int x = 0; x < W; x++) {
                int p = img.getRGB(x, y);
                soma += Math.max(0, chao - ((p >> 16 & 0xFF) + (p >> 8 & 0xFF) + (p & 0xFF)));
            }
        }
        return soma;
    }
}
