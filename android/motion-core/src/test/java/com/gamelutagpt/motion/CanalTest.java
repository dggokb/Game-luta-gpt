package com.gamelutagpt.motion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CanalTest {
    @Test
    public void mudarOAlvoNaoDaSaltoEChegaNoAlvoNovo() {
        Canal c = new Canal(Mola.PADRAO);
        c.alvo(0, 5f);
        assertEquals("no quadro da mudança o valor ainda é o de antes", 0f, c.valor(0), 1e-5f);
        // Muda de novo no meio do caminho: continua de onde estava, sem reiniciar.
        float antes = c.valor(10);
        c.alvo(10, -3f);
        assertEquals(antes, c.valor(10), 1e-5f);
        assertEquals(-3f, c.valor(400), 1e-3f);
    }

    @Test
    public void trancosSomamESomem() {
        Canal c = new Canal(Mola.PADRAO);
        c.degrau(0, 0.1f, Mola.ELASTICA);
        c.impulso(0, 4f, Mola.RAPIDA);
        assertEquals(0.1f, c.valor(0), 1e-6f);
        assertEquals(0f, c.valor(600), 1e-6f);
        c.limpar(600);
        assertEquals("parados saem", 0, c.componentes());
    }

    @Test
    public void limparNaoMudaOValor() {
        Canal c = new Canal(Mola.PADRAO);
        for (int q = 0; q < 40; q += 4) c.impulso(q, 1f, Mola.ELASTICA);
        for (int q = 40; q < 200; q++) {
            float v = c.valor(q);
            c.limpar(q);
            assertEquals(v, c.valor(q), 1e-4f);
        }
    }

    @Test
    public void cheioOMaisVelhoSai() {
        Canal c = new Canal(Mola.PADRAO);
        for (int i = 0; i < Canal.MAXIMO + 5; i++) c.impulso(i, 1f, Mola.PESADA);
        assertEquals(Canal.MAXIMO, c.componentes());
        assertTrue(Float.isFinite(c.valor(30)));
    }
}
