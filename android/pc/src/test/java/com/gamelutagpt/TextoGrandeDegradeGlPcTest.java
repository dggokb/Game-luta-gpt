package com.gamelutagpt;

import static org.junit.Assert.assertTrue;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.SurfaceView;
import java.awt.image.BufferedImage;
import org.junit.Assume;
import org.junit.Test;

/**
 * O "VITÓRIA" da tela de vitória (texto grande, numa textura própria) pintado pelo degradê
 * linear: a forma tem de vir da textura dele, não da textura compartilhada dos textos. Antes o
 * retângulo do título saía cheio de pedaços miúdos dos outros textos (o "Para jogar", números...).
 * Precisa de OpenGL 3.3; sem ele o teste é pulado. No Linux sem tela:
 * {@code XDG_RUNTIME_DIR=/tmp/xdg LIBGL_ALWAYS_SOFTWARE=1 xvfb-run -a gradle :pc:test --tests '*TextoGrandeDegrade*'}.
 */
public class TextoGrandeDegradeGlPcTest {
    private static final int W = 900, H = 560;
    private static final float CIMA = 190f, BAIXO = 450f;

    @Test
    public void textoProprioComDegradeUsaAFormaDele() throws Exception {
        Assume.assumeTrue(DesenhoGl.permitido());
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
        Shader brilho = new Degrade.Linear(0f, BAIXO - 132f * 0.75f, 0f, BAIXO,
            new int[] {0xFFFFFFFF, 0xFFFFF1B8, 0xFFFFC23A, 0xFFF0781E}, new float[] {0f, .4f, .75f, 1f},
            Shader.TileMode.CLAMP);
        Paint tinta = new Paint(Paint.ANTI_ALIAS_FLAG);
        tinta.setTextAlign(Paint.Align.CENTER);
        String[] outros = {"Para jogar", "Vamos treinar", "Kōga", "MENU", "PLAYER 1", "BATALHA", "2 2 5 5 5 7 6"};
        try {
            for (int quadro = 0; quadro < 160; quadro++) {
                Canvas c = gl.comecarQuadro();
                Assume.assumeTrue("sem OpenGL: " + gl.motivoFalha(), c != null);
                c.drawColor(0xFF000000);
                if (quadro < 80) {
                    // Outros textos na textura compartilhada (como os do menu e do HUD).
                    tinta.setShader(null);
                    tinta.setTextSize(22f);
                    tinta.setColor(0xFFFFFFFF);
                    for (int i = 0; i < outros.length; i++) c.drawText(outros[i], 120f, 40f + i * 24f, tinta);
                    // Para o quadro final só ter os dois títulos.
                    c.drawColor(0xFF000000);
                }
                tinta.setTextSize(132f);
                // Em cima: o mesmo texto liso (a referência da forma).
                tinta.setShader(null);
                tinta.setColor(0xFFFFFFFF);
                c.drawText("VITÓRIA", W / 2f, CIMA, tinta);
                // Embaixo: com o degradê (o miolo dourado do título).
                tinta.setShader(brilho);
                tinta.setAlpha(255);
                c.drawText("VITÓRIA", W / 2f, BAIXO, tinta);
                gl.terminarQuadro();
                Thread.sleep(quadro < 120 ? 4 : 0);
            }
        } finally {
            gl.soltarThread();
            gl.liberar();
        }
        BufferedImage img = ultimo[0];
        assertTrue("nenhum quadro do OpenGL", img != null);
        // A forma (pixel aceso) de cada título, com o de baixo deslocado para cima.
        int dy = Math.round(BAIXO - CIMA);
        int liso = 0, degrade = 0, ambos = 0;
        for (int y = 0; y < Math.round((CIMA + BAIXO) / 2f); y++) {
            for (int x = 0; x < W; x++) {
                boolean a = aceso(img.getRGB(x, y));
                boolean b = y + dy < H && aceso(img.getRGB(x, y + dy));
                if (a) liso++;
                if (b) degrade++;
                if (a && b) ambos++;
            }
        }
        assertTrue("o título liso não apareceu: " + liso, liso > 2000);
        float iou = ambos / (float)Math.max(1, liso + degrade - ambos);
        assertTrue("a forma do título com degradê não é a do texto (liso " + liso + ", degradê " + degrade
            + ", em comum " + ambos + ", IoU " + iou + ")", iou > 0.85f);
    }

    private static boolean aceso(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        return Math.max(r, Math.max(g, b)) > 96;
    }
}
