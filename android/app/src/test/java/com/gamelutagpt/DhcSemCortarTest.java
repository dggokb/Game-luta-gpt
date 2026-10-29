package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * DHC que não corta o Super (auditoria T20, etapa G2b): o TAG no Super já solto fica pedido e o
 * parceiro entra quando as pancadas do Super acabam (no Super de capangas do p07, quando eles
 * batem); o parceiro de Super corpo a corpo entra até onde o Super dele alcança o rival.
 */
public class DhcSemCortarTest {

    /** {@code point} + {@code partner} contra o boneco (vida sem fim), 3 barras, M H Super. */
    private static final class Match {
        final FighterState first, second;
        final CombatEngine engine;
        final FighterInput[] in = {new FighterInput(), new FighterInput()};
        int damage, partnerHits, pointHits;
        boolean dhcSeen;

        Match(String point, String partner) { this(point, partner, new CombatConfig()); }

        Match(String point, String partner, CombatConfig config) {
            first = new FighterState(GeneratedCharacters.get(point), "P1");
            second = new FighterState(GeneratedCharacters.get(partner), "P1");
            FighterState dummy = new FighterState(GeneratedCharacters.get("player_base"), "P2");
            engine = new CombatEngine(first, dummy, 900f, 1040f, config);
            engine.setTeam(0, first, second);
            dummy.life = 10_000_000;
            first.superMeter = 3 * CombatConfig.METER_PER_BAR;
        }

        CombatFighter f(int i) { return engine.fighter(i); }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
            for (CombatEngine.HitEvent h : engine.events()) {
                if (h.attacker != 0) continue;
                damage += h.damage;
                if (f(0).state == second) partnerHits++; else pointHits++;
            }
            if (engine.cues().contains("DHC")) dhcSeen = true;
        }

        /** M, H e o Super; com {@code dhc}, segura TAG desde o Super solto. */
        void run(boolean dhc) {
            String[] route = {"M", "H", "SUPER"};
            int k = 0;
            for (int t = 0; t < 420; t++) {
                if (k < route.length) {
                    if (route[k].equals("M")) in[0].medium = true;
                    else if (route[k].equals("H")) in[0].heavy = true;
                    else in[0].superAttack = true;
                } else if (dhc && f(0).state == first) {
                    in[0].assist = true;
                }
                step();
                CombatFighter me = f(0);
                if (k < route.length && me.attacking() &&
                    (route[k].equals("SUPER") ? me.attack.kind == AttackDefinition.Kind.SUPER : me.attack.id.equals(route[k]))) k++;
            }
        }
    }

    @Test public void theDhcWaitsForTheHenchmenOfP07() {
        Match alone = new Match("p07", "p08");
        alone.run(false);
        Match dhc = new Match("p07", "p08");
        dhc.run(true);
        assertTrue("o DHC saiu", dhc.dhcSeen);
        assertTrue("os capangas bateram antes da troca: " + dhc.pointHits + " de " + alone.pointHits,
            dhc.pointHits >= alone.pointHits);
        assertTrue("o DHC soma ao Super em vez de cortá-lo: " + dhc.damage + " contra " + alone.damage,
            dhc.damage > alone.damage);
    }

    @Test public void theDhcDoesNotCutAMultiHitSuper() {
        Match alone = new Match("p03", "p04");
        alone.run(false);
        Match dhc = new Match("p03", "p04");
        dhc.run(true);
        assertTrue(dhc.dhcSeen);
        assertEquals("todas as pancadas do Super do Kōga", alone.pointHits, dhc.pointHits);
        assertTrue("e o Super do parceiro ainda pega", dhc.partnerHits > 0);
    }

    @Test public void anotherTagWhileTheDhcIsPendingCallsNoAssist() {
        Match m = new Match("p03", "p04");
        String[] route = {"M", "H", "SUPER"};
        int k = 0;
        for (int t = 0; t < 300 && !m.dhcSeen; t++) {
            if (k < route.length) {
                if (route[k].equals("M")) m.in[0].medium = true;
                else if (route[k].equals("H")) m.in[0].heavy = true;
                else m.in[0].superAttack = true;
            } else {
                m.in[0].assist = true;
            }
            m.step();
            CombatFighter me = m.f(0);
            if (k < route.length && me.attacking() &&
                (route[k].equals("SUPER") ? me.attack.kind == AttackDefinition.Kind.SUPER : me.attack.id.equals(route[k]))) k++;
            assertNull("nenhum assist por cima do DHC pedido", m.engine.team(0).assist);
        }
        assertTrue(m.dhcSeen);
    }

    @Test public void aMeleeDhcPartnerEntersWithinReachOfTheRival() {
        Match m = new Match("p07", "p08");
        m.run(true);
        assertTrue(m.dhcSeen);
        assertTrue("o Super do p08 pega o rival que os capangas jogaram longe", m.partnerHits > 0);
        CombatConfig parado = new CombatConfig();
        parado.dhcMaxAdvance = 0f;
        Match where = new Match("p07", "p08", parado);
        where.run(true);
        assertTrue(where.dhcSeen);
        assertEquals("entrando onde o titular estava, o Super do p08 erra", 0, where.partnerHits);
    }
}
