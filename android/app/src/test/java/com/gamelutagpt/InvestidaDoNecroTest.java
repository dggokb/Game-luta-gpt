package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * A Investida (S1) do p12 bate pelo bote desenhado (etapa H8b): o desenho salta até ~290 px à
 * frente da raiz enquanto o corpo avança, e a caixa do golpe (120 px) ficava para trás. A 320 px o
 * Necro atravessava o rival no desenho sem acertar (T25, achado 4).
 */
public class InvestidaDoNecroTest {
    private static final CharacterDefinition NECRO = GeneratedCharacters.get("p12");
    private static final CharacterDefinition RYUGA = GeneratedCharacters.get("player_base");

    /** O Necro solta a Investida a {@code distancia} px do Ryūga parado: o quadro do acerto, ou -1. */
    private static int hitFrame(float distancia) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(NECRO, RYUGA, 600f, 600f + distancia);
        s.steps(2);
        s.in[0].special = "M";
        for (int i = 0; i < 60; i++) {
            s.step();
            for (CombatEngine.HitEvent h : s.engine.events()) if (h.attacker == 0) return s.f(0).attackFrame;
        }
        return -1;
    }

    @Test public void theDrawnLungeHitsWhereItPassesThroughTheRival() {
        assertTrue("a 320 px o bote acerta", hitFrame(320f) >= 0);
        assertTrue("a 200 px também", hitFrame(200f) >= 0);
    }

    @Test public void itStillMissesBeyondTheDrawnLunge() {
        assertEquals("a 520 px o desenho não chega", -1, hitFrame(520f));
    }

    @Test public void theDamageIsUnchanged() {
        assertEquals(940, NECRO.fighter.energy.damage);
        assertTrue(NECRO.fighter.energy.attack.mechanics.drawnStrike);
    }
}
