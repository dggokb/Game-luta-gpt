package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Segurança e uso dos assists aprovados na lista G (p13 com H, p15 com 2H, p07 e os de corpo a corpo):
 * defendidos, deixam o rival preso depois do último contato (o titular está livre o tempo todo e o
 * assist não tem corpo, então não há o que punir); no canto, o golpe sai dentro da arena e pega.
 */
public class AssistSegurancaTest {
    private static final String[] MELEE = {"p07", "p12", "p13", "p15"};

    private static CombatEngine engine(String partner, float x0, float x1) {
        FighterState a = new FighterState(GeneratedCharacters.get("p02"), "P1");
        FighterState d = new FighterState(GeneratedCharacters.get("player_base"), "P2");
        CombatEngine e = new CombatEngine(a, d, x0, x1, new CombatConfig());
        e.setTeam(0, a, new FighterState(GeneratedCharacters.get(partner), "P1"));
        return e;
    }

    /** Quadros que o rival, defendendo agachado, fica preso depois do último contato do assist (-1 se não tocou). */
    private static int blockedAdvantage(String partner, float gap) {
        CombatEngine e = engine(partner, 900f, 900f + gap);
        FighterInput[] in = {new FighterInput(), new FighterInput()};
        in[0].assist = true;
        int lastContact = -1, free = -1;
        boolean hit = false;
        for (int t = 0; t < 200; t++) {
            in[1].direction = e.fighter(0).x >= e.fighter(1).x ? 4 : 2;
            e.step(in[0], in[1]);
            in[0].clearPresses();
            for (CombatEngine.HitEvent h : e.events()) {
                if (h.attacker != 0) continue;
                lastContact = t;
                hit |= !h.blocked;
                free = -1;
            }
            if (lastContact >= 0 && free < 0 && e.fighter(1).canAct()) free = t;
        }
        assertFalse(partner + ": o rival agachado defende o assist", hit);
        return lastContact < 0 ? -1 : free - lastContact;
    }

    @Test public void aBlockedMeleeAssistLeavesTheRivalStuck() {
        for (String p : MELEE) {
            int adv = blockedAdvantage(p, 250f);
            assertTrue(p + ": o assist toca a 250 px", adv >= 0);
            assertTrue(p + ": rival preso +" + adv + " q depois do último contato", adv >= 20);
        }
    }

    @Test public void inTheCornerTheAssistStrikesInsideTheArena() {
        for (String p : MELEE) {
            for (boolean pointInCorner : new boolean[] {true, false}) {
                float x0 = pointInCorner ? Arena.LEFT_BOUND + 20f : Arena.RIGHT_BOUND - 270f;
                float x1 = pointInCorner ? x0 + 250f : Arena.RIGHT_BOUND - 20f;
                CombatEngine e = engine(p, x0, x1);
                FighterInput[] in = {new FighterInput(), new FighterInput()};
                in[0].assist = true;
                boolean hit = false;
                for (int t = 0; t < 200; t++) {
                    e.step(in[0], in[1]);
                    in[0].clearPresses();
                    TeamSystem.Side s = e.team(0);
                    if (s.assist != null && s.assistPhase == TeamSystem.ASSIST_ATTACK) {
                        assertTrue(p + ": dentro da arena", s.assist.x >= Arena.LEFT_BOUND - 1f && s.assist.x <= Arena.RIGHT_BOUND + 1f);
                    }
                    for (CombatEngine.HitEvent h : e.events()) hit |= h.attacker == 0;
                }
                assertTrue(p + (pointInCorner ? ", titular no canto" : ", rival no canto") + ": acerta", hit);
            }
        }
    }
}
