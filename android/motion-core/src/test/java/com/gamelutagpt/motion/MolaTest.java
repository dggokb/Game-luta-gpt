package com.gamelutagpt.motion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MolaTest {
    private static final Mola[] TODAS = {Mola.RAPIDA, Mola.PADRAO, Mola.PESADA, Mola.ELASTICA,
        new Mola(2.5f, 1.8f)};

    @Test
    public void oDegrauComecaEmUmEAcabaEmZero() {
        for (Mola m : TODAS) {
            assertEquals(1f, m.degrau(0f), 1e-6f);
            assertEquals(0f, m.degrau((float)m.fim + 0.01f), 0f);
            assertTrue("chega perto de zero antes do fim", Math.abs(m.degrau((float)m.fim * 0.99f)) < 0.01f);
        }
    }

    @Test
    public void oImpulsoComecaEmZeroEChegaAUmNoPico() {
        for (Mola m : TODAS) {
            assertEquals(0f, m.impulso(0f), 0f);
            float maior = 0f;
            for (int q = 1; q < 240; q++) maior = Math.max(maior, m.impulso(q / 600f));
            assertEquals("pico normalizado", 1f, maior, 0.01f);
            assertEquals(0f, m.impulso((float)m.fim + 0.01f), 0f);
        }
    }

    @Test
    public void soAMolaPoucoAmortecidaPassaDoPonto() {
        assertTrue("a elástica balança", minimo(Mola.ELASTICA) < -0.1f);
        assertTrue("a pesada não passa do ponto", minimo(Mola.PESADA) > -1e-4f);
        assertTrue("a superamortecida também não", minimo(new Mola(2.5f, 1.8f)) > -1e-4f);
    }

    @Test
    public void oValorEContinuoQuadroAQuadro() {
        // Sem saltos entre quadros vizinhos: a forma fechada não depende de integrar.
        for (Mola m : TODAS) {
            for (int q = 1; q < 120; q++) {
                assertTrue(Math.abs(m.degrau(q / 60f) - m.degrau((q - 1) / 60f)) < 0.5f);
            }
        }
    }

    private static float minimo(Mola m) {
        float menor = 1f;
        for (int q = 0; q < 300; q++) menor = Math.min(menor, m.degrau(q / 120f));
        return menor;
    }
}
