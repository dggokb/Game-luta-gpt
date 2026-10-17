package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Combo breaker: no golpe que fecha a sequência (forte ou especial) de um combo que já está
 * entrando, trás em cima da hora quebra o combo — por 1 barra, uma vez por combo.
 */
public class ComboBreakerTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();

    private static final class Match {
        final FighterState first = new FighterState(BASE, "P1");
        final FighterState cpu = new FighterState(NPC, "CPU");
        final CombatEngine engine = new CombatEngine(first, cpu, 500f, 600f, new CombatConfig());
        final FighterInput[] in = {new FighterInput(), new FighterInput()};

        CombatFighter f(int i) { return engine.fighter(i); }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
        }

        int back(int i) { return MotionParser.relative(5, f(i).facing); }

        /**
         * P1 faz L > M > H; o CPU aperta trás {@code lead} quadros antes do contato do golpe
         * {@code target} (o quadro vem de uma luta igual sem o aperto). Devolve o último quadro.
         */
        void combo(String target, int lead) {
            int contact = target == null ? -1 : contactOf(target);
            String[] seq = {"L", "M", "H"};
            int k = 0;
            in[0].light = true;
            for (int i = 0; i < 90; i++) {
                if (contact >= 0 && i == contact - lead) in[1].direction = back(1);
                step();
                for (CombatEngine.HitEvent h : engine.events()) {
                    if (h.attacker == 0 && !h.blocked && k < 2) {
                        k++;
                        if ("M".equals(seq[k])) in[0].medium = true; else in[0].heavy = true;
                    }
                }
                if (engine.cues().contains("COMBO_BREAKER")) return;
            }
        }

        static int contactOf(String move) {
            Match probe = new Match();
            String[] seq = {"L", "M", "H"};
            int k = 0;
            probe.in[0].light = true;
            for (int i = 0; i < 90; i++) {
                probe.step();
                for (CombatEngine.HitEvent h : probe.engine.events()) {
                    if (h.attacker != 0) continue;
                    if (move.equals(h.moveId)) return i;
                    if (k < 2) {
                        k++;
                        if ("M".equals(seq[k])) probe.in[0].medium = true; else probe.in[0].heavy = true;
                    }
                }
            }
            return -1;
        }
    }

    @Test public void backRightBeforeTheHeavyEnderBreaksTheCombo() {
        Match m = new Match();
        m.cpu.superMeter = CombatConfig.METER_PER_BAR;
        m.combo("H", 3);
        assertTrue(m.engine.cues().contains("COMBO_BREAKER"));
        assertEquals("custa 1 barra", 0, m.cpu.superMeter);
        assertEquals("o H não entra", CombatFighter.Status.NEUTRAL, m.f(1).status);
        assertTrue(m.f(1).comboBroke);
        assertEquals("quem batia fica atordoado", CombatFighter.Status.HITSTUN, m.f(0).status);
        assertEquals(m.engine.config.comboBreakerStunFrames, m.f(0).stunLeft);
        assertFalse(m.f(0).attacking());
        assertNull("o combo acabou", m.engine.session(1) != null && m.engine.session(1).active ? m.engine.session(1) : null);
    }

    @Test public void theBreakerLetsTheDefenderComboBack() {
        Match m = new Match();
        m.cpu.superMeter = CombatConfig.METER_PER_BAR;
        m.combo("H", 3);
        assertTrue(m.engine.cues().contains("COMBO_BREAKER"));
        int lifeBefore = m.first.life;
        m.in[1].direction = 0;
        m.in[1].light = true;
        boolean hit = false;
        for (int i = 0; i < 40 && !hit; i++) {
            m.step();
            for (CombatEngine.HitEvent h : m.engine.events()) if (h.attacker == 1 && !h.blocked) hit = true;
        }
        assertTrue("o troco entra", hit);
        assertTrue(m.first.life < lifeBefore);
    }

    @Test public void withoutMeterTheEnderLands() {
        Match m = new Match();
        m.cpu.superMeter = CombatConfig.METER_PER_BAR - 1;
        m.combo("H", 3);
        assertFalse(m.engine.cues().contains("COMBO_BREAKER"));
        assertEquals(3, m.engine.session(1).hitCount);
    }

    @Test public void lightAndMediumHitsCannotBeBroken() {
        Match m = new Match();
        m.cpu.superMeter = CombatConfig.METER_PER_BAR;
        // Trás em cima do M (o 2º golpe, não fecha a sequência): nada.
        m.combo("M", 3);
        assertFalse(m.engine.cues().contains("COMBO_BREAKER"));
        assertEquals(CombatConfig.METER_PER_BAR, m.cpu.superMeter);
    }

    @Test public void holdingBackFromBeforeDoesNotBreak() {
        Match m = new Match();
        m.cpu.superMeter = CombatConfig.METER_PER_BAR;
        m.combo("H", 25);
        assertFalse(m.engine.cues().contains("COMBO_BREAKER"));
        assertEquals(3, m.engine.session(1).hitCount);
    }
}
