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

    @Test public void everyFighterReachesTheLauncherAndJumpCancelsInTheAir() {
        for (String id : FIGHTERS) {
            for (String from : new String[] {"L", "2L", "M"}) assertTrue(id + " " + from + " > 2H", route(id, from, "2H"));
            assertTrue(id + " L > H", route(id, "L", "H"));
            assertTrue(id + " 2H > pulo", route(id, "2H", AttackDefinition.JUMP));
            assertTrue(id + " jM > pulo no ar", route(id, "jM", AttackDefinition.JUMP));
            assertEquals(id + " o lançador uma vez por corrente", 1, GeneratedCharacters.get(id).attack("2H").maxUsesPerChain);
        }
    }

    @Test public void theHeavyLeavesEveryoneFreeToFollowUp() {
        // O H do P01 tinha 37 quadros de recuperação (-23 no acerto): o lutador ficava parado no
        // fim do golpe sem conseguir combar nem dar dash. Todo H acerta com vantagem.
        for (String id : FIGHTERS) {
            AttackDefinition h = GeneratedCharacters.get(id).attack("H");
            int advantage = h.hitstunFrames - (h.activeFrames - 1 + h.recoveryFrames);
            assertTrue(id + " H no acerto: " + advantage, advantage >= 0);
        }
    }

    @Test public void eachStyleAddsItsOwnRoutes() {
        assertTrue("pressão: M > M", route("p04", "M", "M"));
        assertTrue("pressão: M cancela em pulo", route("p03", "M", AttackDefinition.JUMP));
        assertTrue("chutes: M > M", route("p02", "M", "M"));
        assertTrue("chutes: jM > jM", route("p02", "jM", "jM"));
        assertTrue("aéreo: jL > jL", route("p05", "jL", "jL"));
        assertTrue("aéreo: M sobe para o ar", route("p05", "M", AttackDefinition.JUMP));
        assertTrue("alcance: L > S1", route("p08", "L", "S"));
        assertTrue("alcance: 2M > S1", route("p11", "2M", "S"));
        assertTrue("tanque: H > H", route("p09", "H", "H"));
        assertTrue("imprevisível: H > L", route("p12", "H", "L"));
        assertTrue("imprevisível: M > L", route("p12", "M", "L"));
        // O equilibrado é só a base: nada repete nem volta.
        assertFalse(route("player_base", "M", "M"));
        assertFalse(route("player_base", "H", "H"));
        assertFalse(route("player_base", "H", "L"));
        assertFalse("só o imprevisível volta", route("p04", "H", "L"));
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

    @Test public void lightMediumLauncherInOneChain() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(BASE, NPC, 500f, 600f);
        s.in[0].light = true;
        s.step();
        s.until(() -> s.f(0).outcome == CombatFighter.Outcome.HIT, 30);
        s.in[0].medium = true;
        s.until(() -> s.attackId(0).equals("M") && s.f(0).outcome == CombatFighter.Outcome.HIT, 40);
        s.in[0].direction = 3;
        s.in[0].heavy = true;
        s.until(() -> s.attackId(0).equals("2H"), 40);
        s.until(() -> s.f(1).launched, 40);
        assertEquals("L > M > 2H é um combo só", 3, s.engine.session(1).hitCount);
    }

    @Test public void reverseBeatStringOnBlockAlwaysEnds() {
        // O p12 aperta L, M, H sem parar contra quem defende: sem o limite da corrente, H > L
        // faria uma pressão infinita (na defesa não há combo para contar). O empurrão da defesa
        // já separa os dois no jogo; aqui a distância fica fixa para testar só o limite.
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get("p12"), BASE, 500f, 610f);
        s.in[1].direction = 1;   // de costas para o p12: defende
        String[] cycle = {"L", "M", "H"};
        int longest = 0;
        for (int i = 0; i < 400; i++) {
            String b = cycle[i % 3];
            s.in[0].light = b.equals("L");
            s.in[0].medium = b.equals("M");
            s.in[0].heavy = b.equals("H");
            s.f(0).x = s.f(1).x - 105f;
            s.step();
            assertFalse("nunca acerta quem defende", s.engine.session(1) != null);
            longest = Math.max(longest, s.f(0).chain.size());
        }
        assertTrue("a corrente voltou para trás pelo menos uma vez: " + longest, longest > 3);
        assertTrue("e acabou no limite (L, M e H 2 vezes cada): " + longest, longest <= 6);
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
