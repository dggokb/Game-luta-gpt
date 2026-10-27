package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/** Desgaste da luta: os estágios pela vida e a respiração ofegante só no fim. */
public class DesgasteDaLutaTest {
    private static FighterState lutador(float vida) {
        FighterState s = new FighterState(GeneratedCharacters.get("p15"), "P1");
        s.life = Math.round(s.profile.maxLife * vida);
        return s;
    }

    @Test
    public void stagesFollowTheLifeThresholds() {
        assertEquals(0, DesgasteDaLuta.estagio(lutador(1f)));
        assertEquals(0, DesgasteDaLuta.estagio(lutador(0.70f)));
        assertEquals(1, DesgasteDaLuta.estagio(lutador(0.60f)));
        assertEquals(2, DesgasteDaLuta.estagio(lutador(0.30f)));
        assertEquals(3, DesgasteDaLuta.estagio(lutador(0.08f)));
        assertEquals(3, DesgasteDaLuta.estagio(lutador(0f)));
    }

    @Test
    public void onlyTheLastStageBreathesHard() {
        for (int quadro = 0; quadro < 60; quadro += 7) {
            assertEquals(1f, DesgasteDaLuta.folego(lutador(0.30f), quadro), 0f);
        }
        boolean mexeu = false;
        for (int quadro = 0; quadro < 60; quadro++) {
            float f = DesgasteDaLuta.folego(lutador(0.05f), quadro);
            assertTrue(Math.abs(f - 1f) <= 0.02f);
            mexeu |= f != 1f;
        }
        assertTrue(mexeu);
    }

    @Test
    public void aCleanFighterHasNoFilter() {
        DesgasteDaLuta d = new DesgasteDaLuta();
        assertNull(d.tinta(lutador(1f)));
        assertNull(d.sujeira(lutador(1f)));
        assertNotNull(d.tinta(lutador(0.2f)));
        assertNotNull(d.sujeira(lutador(0.2f)));
    }
}
