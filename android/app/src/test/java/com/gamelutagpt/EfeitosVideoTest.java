package com.gamelutagpt;

import static org.junit.Assert.*;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Efeitos de vídeo: o que não existe não toca (o clássico entra) e o que toca some no fim. */
@RunWith(RobolectricTestRunner.class)
public class EfeitosVideoTest {
    private static EfeitosVideo.Folha folha(String nome, int quadros, float ms) {
        int[][] q = new int[quadros][];
        float[][] d = new float[quadros][];
        float[] fim = new float[quadros];
        for (int i = 0; i < quadros; i++) {
            q[i] = new int[] {i * 10, 0, 10, 10};
            d[i] = new float[] {-5f, -10f};
            fim[i] = ms * (i + 1);
        }
        return new EfeitosVideo.Folha(nome, Bitmap.createBitmap(10 * quadros, 10, Bitmap.Config.ARGB_8888), 1.5f, q, d, fim);
    }

    @Test public void efeitoQueNaoExisteNaoToca() {
        EfeitosVideo e = new EfeitosVideo(null);
        e.carregar();
        assertFalse(e.tocar("impacto_forte", 0f, 0f, 1f, false));
        assertEquals(0, e.tocando());
    }

    @Test public void tocaPassaPelosQuadrosESome() {
        EfeitosVideo e = new EfeitosVideo(null);
        e.adicionar(folha("fumaca_pouso", 4, 50f));
        assertTrue(e.tocar("fumaca_pouso", 100f, 200f, 1f, true));
        assertEquals(1, e.tocando());
        Canvas c = new Canvas(Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888));
        // 4 quadros de 50 ms = 200 ms = 12 passos de 60 Hz.
        for (int i = 0; i < 11; i++) {
            e.desenhar(c);
            e.passo();
        }
        assertEquals("ainda tocando no último quadro", 1, e.tocando());
        e.passo();
        assertEquals(0, e.tocando());
    }

    @Test public void quadroPeloTempo() {
        EfeitosVideo.Folha f = folha("x", 3, 40f);
        assertEquals(0, f.quadroEm(0f));
        assertEquals(1, f.quadroEm(45f));
        assertEquals(2, f.quadroEm(119f));
        assertEquals(-1, f.quadroEm(120f));
    }
}
