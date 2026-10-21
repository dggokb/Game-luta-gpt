package com.gamelutagpt;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Correntes de combo (tools/sprites/mecanicas.json, bloco "combos"): a base igual para todos
 * (a corrente chega no lançador, cancelamento de pulo no ar) e o estilo de cada lutador.
 */
public class CorrentesComboTest {
    private static final String[] FIGHTERS =
        {"player_base", "p02", "p03", "p04", "p05", "p06", "p07", "p08", "p09", "p10", "p11", "p12"};
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();

    private static boolean route(String fighter, String from, String to) {
        return GeneratedCharacters.get(fighter).attack(from).cancelsInto(to);
    }

    @Test public void theChainCountsUsesAndALinkStartsAnotherOne() {
        CombatFighter f = new CombatFighter(0, new FighterState(BASE, "P1"), 500f, 1);
        AttackDefinition m = BASE.attack("M");
        assertTrue(CancelSystem.chainAllows(f, m));
        f.chain.add("M");
        assertFalse("M uma vez por corrente", CancelSystem.chainAllows(f, m));
        f.chain.clear();
        assertTrue("outra corrente, depois de uma ligação", CancelSystem.chainAllows(f, m));
    }

    @Test public void airJumpCancelRestartsTheAirChainOncePerFlight() {
        // Rota de verdade: 2H lança, super pulo, jM acerta, ↑ no ar, jM acerta de novo.
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(BASE, NPC, 500f, 600f);
        s.in[0].direction = 3;
        s.in[0].heavy = true;
        s.step();
        s.until(() -> s.f(0).outcome == CombatFighter.Outcome.HIT, 40);
        s.in[0].direction = 7;
        s.until(() -> !s.f(0).grounded, 30);
        s.in[0].direction = 0;
        int airHits = 0, airJumps = 0;
        for (int i = 0; i < 140 && !s.f(0).grounded; i++) {
            boolean confirmed = s.f(0).attacking() && s.f(0).outcome == CombatFighter.Outcome.HIT && s.f(0).hitstop == 0;
            s.in[0].medium = !s.f(0).attacking();
            // Aperta ↑ a cada jM que acerta: só o primeiro vira pulo no ar.
            s.in[0].direction = confirmed ? 7 : 0;
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) if (e.attacker == 0 && !e.blocked) airHits++;
            if (s.engine.cues().contains("AIR_JUMP")) {
                airJumps++;
                assertTrue("subindo de novo", s.f(0).vy < 0f);
            }
        }
        assertEquals("um pulo no ar por voo", 1, airJumps);
        assertTrue("o jM acertou antes e depois do pulo no ar: " + airHits, airHits >= 2);
    }
}
