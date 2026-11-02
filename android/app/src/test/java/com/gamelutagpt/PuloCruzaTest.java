package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * O pulo para frente de perto passa por cima do rival parado (o corpo no ar encolhe as pernas e
 * fica mais estreito, como no mercado); de longe ele cai na frente. Antes o Ryūga não passava nem
 * do Kōga (pedido do jogador, 02/11).
 */
public class PuloCruzaTest {
    /** Pula para frente com o rival a {@code gap} px; devolve se caiu do outro lado. */
    private static boolean passa(String quem, String rival, float gap) {
        FighterState a = new FighterState(GeneratedCharacters.get(quem), "P1");
        FighterState b = new FighterState(GeneratedCharacters.get(rival), "P2");
        CombatEngine e = new CombatEngine(a, b, 600f, 600f + gap, new CombatConfig());
        FighterInput i0 = new FighterInput(), i1 = new FighterInput();
        for (int k = 0; k < 10; k++) { e.step(i0, i1); i0.clearPresses(); i1.clearPresses(); }
        boolean saiu = false;
        for (int k = 0; k < 120; k++) {
            i0.direction = k < 3 ? 8 : 0;   // ↗
            if (k == 0) i0.jump = true;
            e.step(i0, i1);
            i0.clearPresses();
            i1.clearPresses();
            if (!e.fighter(0).grounded) saiu = true;
            if (saiu && e.fighter(0).grounded) return e.fighter(0).x > e.fighter(1).x;
        }
        fail("não aterrissou");
        return false;
    }

    @Test public void oRyugaPassaPorCimaDoKogaEDosMaisAltos() {
        for (String rival : new String[] {"p03", "p09", "p13", "player_base"}) {
            assertTrue("Ryūga sobre " + rival + " de 150 px", passa("player_base", rival, 150f));
            assertTrue("Ryūga sobre " + rival + " de 200 px", passa("player_base", rival, 200f));
        }
    }

    @Test public void deLongeOPuloCaiNaFrente() {
        assertFalse(passa("player_base", "p03", 300f));
        assertFalse(passa("p13", "player_base", 300f));
    }
}
