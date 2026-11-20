package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Do pulo e do super pulo para a frente, perto ou por cima do rival, o corpo pousa parado no
 * motor (20/11, o dono: "caindo do super pulo ainda desliza", no Kōga e na Yin-lu): o deslize que
 * se vê não é do corpo, é do desenho; isto garante que o motor não passe a escorregar.
 */
public class PousoSemDeslizeTest {
    private static void pousoParado(String id, boolean superPulo, float gap) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get(id), GeneratedCharacters.get("player_base"), 800f, 800f + gap);
        boolean noAr = false;
        int pousou = -1;
        float x = 0f;
        for (int t = 0; t < 200; t++) {
            if (t == 2) {
                if (superPulo) s.in[0].superJump = true; else s.in[0].jump = true;
                s.in[0].direction = 8;
            }
            s.step();
            s.in[0].jump = s.in[0].superJump = false;
            s.in[0].direction = 0;
            CombatFighter f = s.f(0);
            noAr |= !f.grounded;
            if (noAr && f.grounded && pousou < 0) { pousou = t; x = f.x; }
            if (pousou >= 0 && t - pousou <= 20) {
                assertEquals(id + (superPulo ? " super pulo" : " pulo") + " gap " + gap + ": parado no pouso (+" + (t - pousou) + ")",
                    x, f.x, 1f);
            }
        }
        assertTrue(id + ": pousou", pousou >= 0);
    }

    @Test public void oKogaPousaParado() {
        for (float gap : new float[] {120f, 220f, 320f}) {
            pousoParado("p03", true, gap);
            pousoParado("p03", false, gap);
        }
    }

    @Test public void aYinLuPousaParada() {
        for (float gap : new float[] {120f, 220f, 320f}) {
            pousoParado("p02", true, gap);
            pousoParado("p02", false, gap);
        }
    }
}
