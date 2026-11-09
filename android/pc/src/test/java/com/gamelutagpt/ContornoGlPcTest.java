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
 * O contorno do corpo ({@link Filtro#comContorno}) pelo OpenGL: o idle duas vezes no fundo claro,
 * à esquerda sem filtro e à direita com um contorno vermelho, por fora do corpo. Salva em {@code build/contorno-gl.png}. Precisa de
 * OpenGL 3.3; sem ele é pulado.
 */
public class ContornoGlPcTest {
    private static final int W = 700, H = 520, FUNDO = 0xFFD8D8D8;

    @Test
    public void fioPorForaDoCorpo() throws Exception {
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
        Paint sem = new Paint(Paint.FILTER_BITMAP_FLAG), com = new Paint(Paint.FILTER_BITMAP_FLAG);
        com.setColorFilter(Filtro.comContorno(null, 0xFF0000, 4f / H).canvas);
        // O primeiro quadro da folha do idle (328 x 470).
        Rect src = new Rect(0, 0, 328, 470);
        try {
            for (int quadro = 0; quadro < 40; quadro++) {
                Canvas c = gl.comecarQuadro();
                Assume.assumeTrue("sem OpenGL: " + gl.motivoFalha(), c != null);
                c.drawColor(FUNDO);
                c.drawBitmap(b, src, new Rect(10, 20, 338, 490), sem);
                c.drawBitmap(b, src, new Rect(360, 20, 688, 490), com);
                gl.terminarQuadro();
                Thread.sleep(2);
            }
        } finally {
            gl.soltarThread();
            gl.liberar();
        }
        BufferedImage img = ultimo[0];
        assertTrue("nenhum quadro do OpenGL", img != null);
        javax.imageio.ImageIO.write(img, "png", new File("build/contorno-gl.png"));
        int esquerda = vermelhos(img, 0, W / 2), direita = vermelhos(img, W / 2, W);
        System.out.printf("contorno: vermelhos sem %d, com %d%n", esquerda, direita);
        // O gi tem vermelho dos dois lados; o fio soma bem mais à direita.
        assertTrue("o contorno não apareceu: sem " + esquerda + ", com " + direita, direita > esquerda * 3);
    }

    private static int vermelhos(BufferedImage img, int x0, int x1) {
        int n = 0;
        for (int y = 0; y < H; y++) {
            for (int x = x0; x < x1; x++) {
                int p = img.getRGB(x, y);
                if ((p >> 16 & 0xFF) > 170 && (p >> 8 & 0xFF) < 90 && (p & 0xFF) < 90) n++;
            }
        }
        return n;
    }
}
