package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Combo breaker: no golpe que fecha a sequência (forte ou especial) de um combo que já está
 * entrando, M + H em cima da hora quebra o combo — por 1 barra, uma vez por combo. Quem batia é
 * jogado para longe e quem quebrou corre sozinho até ele.
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

        /** O aperto do CPU: M + H, só trás, ou o M e o H em dois dedos, um quadro de diferença. */
        static final int MH = 0, BACK = 1, TWO_FINGERS = 2;

        void combo(String target, int lead) { combo(target, lead, MH); }

        /**
         * P1 faz L > M > H; o CPU aperta {@code lead} quadros antes do contato do golpe
         * {@code target} (o quadro vem de uma luta igual sem o aperto).
         */
        void combo(String target, int lead, int how) {
            int contact = target == null ? -1 : contactOf(target);
            String[] seq = {"L", "M", "H"};
            int k = 0;
            in[0].light = true;
            for (int i = 0; i < 90; i++) {
                if (contact >= 0 && i == contact - lead) {
                    if (how == MH) in[1].pushblock = true;
                    else if (how == BACK) in[1].direction = back(1);
                    else in[1].medium = true;
                }
                if (how == TWO_FINGERS && contact >= 0 && i == contact - lead + 1) in[1].heavy = true;
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

        float gap() { return Math.abs(f(0).x - f(1).x); }

        float touching() { return f(0).body().halfWidth + f(1).body().halfWidth; }
    }

    @Test public void mhRightBeforeTheHeavyEnderBreaksTheCombo() {
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

    /** O estalo joga quem batia para longe (arrastando o pé) e quem quebrou corre sozinho até ele. */
    @Test public void theBreakerThrowsTheAttackerAwayAndTheDefenderRunsAlone() {
        Match m = new Match();
        m.cpu.superMeter = CombatConfig.METER_PER_BAR;
        m.combo("H", 3);
        assertTrue(m.engine.cues().contains("COMBO_BREAKER"));
        CombatConfig config = m.engine.config;
        float attackerStart = m.f(0).x, breakerStart = m.f(1).x;
        // Congelamento do estalo + o deslize.
        boolean ran = false;
        for (int i = 0; i < config.comboBreakerHitstopFrames + config.comboBreakerPushFrames; i++) {
            m.step();
            ran |= m.f(1).forwardDashing;
        }
        assertTrue("jogado para trás: " + Math.abs(m.f(0).x - attackerStart),
            Math.abs(m.f(0).x - attackerStart) > 0.6f * config.comboBreakerPushGap);
        assertTrue("correu sem → →", ran);
        for (int i = 0; i < 30; i++) m.step();
        assertTrue("correu um bom pedaço: " + Math.abs(m.f(1).x - breakerStart),
            Math.abs(m.f(1).x - breakerStart) > 0.6f * config.comboBreakerPushGap);
        assertTrue("chegou perto: " + m.gap(), m.gap() <= m.touching() + config.comboBreakerCloseGap + 12f);
        assertEquals("parou de correr", 0, m.f(1).breakerChase);
        assertTrue("o outro ainda atordoado", m.f(0).inHitstun());
    }

    @Test public void theBreakerLetsTheDefenderComboBack() {
        Match m = new Match();
        m.cpu.superMeter = CombatConfig.METER_PER_BAR;
        m.combo("H", 3);
        assertTrue(m.engine.cues().contains("COMBO_BREAKER"));
        // Espera a corrida terminar e só então aperta: ainda dá tempo.
        while (m.f(1).breakerChase > 0) m.step();
        int lifeBefore = m.first.life;
        m.in[1].light = true;
        boolean hit = false;
        for (int i = 0; i < 40 && !hit; i++) {
            m.step();
            for (CombatEngine.HitEvent h : m.engine.events()) if (h.attacker == 1 && !h.blocked) hit = true;
        }
        assertTrue("o troco entra", hit);
        assertTrue(m.first.life < lifeBefore);
    }

    /** O golpe apertado no meio da corrida espera chegar perto: não sai de longe no vazio. */
    @Test public void anAttackPressedDuringTheRunWaitsForTheArrival() {
        Match m = new Match();
        m.cpu.superMeter = CombatConfig.METER_PER_BAR;
        m.combo("H", 3);
        assertTrue(m.engine.cues().contains("COMBO_BREAKER"));
        // Apertado ainda no congelamento do estalo, antes até de começar a correr.
        m.step();
        m.in[1].light = true;
        float far = m.touching() + m.engine.config.comboBreakerCloseGap + 15f;
        boolean hit = false, whiffed = false;
        for (int i = 0; i < 60 && !hit; i++) {
            m.step();
            if (m.f(1).attacking() && m.gap() > far) whiffed = true;
            for (CombatEngine.HitEvent h : m.engine.events()) if (h.attacker == 1 && !h.blocked) hit = true;
        }
        assertFalse("não saiu de longe", whiffed);
        assertTrue("entrou ao chegar", hit);
    }

    @Test public void holdingBackGivesUpTheRun() {
        Match m = new Match();
        m.cpu.superMeter = CombatConfig.METER_PER_BAR;
        m.combo("H", 3);
        assertTrue(m.engine.cues().contains("COMBO_BREAKER"));
        while (!m.f(1).forwardDashing) m.step();
        m.in[1].direction = m.back(1);
        m.step();
        assertEquals(0, m.f(1).breakerChase);
        m.in[1].direction = 0;
        float before = m.gap();
        for (int i = 0; i < 10; i++) m.step();
        assertTrue("não corre mais", m.gap() >= before - 1f);
    }

    @Test public void twoFingersAFrameApartAlsoBreak() {
        Match m = new Match();
        m.cpu.superMeter = CombatConfig.METER_PER_BAR;
        m.combo("H", 4, Match.TWO_FINGERS);
        assertTrue(m.engine.cues().contains("COMBO_BREAKER"));
    }

    /** O gesto antigo (trás em cima da hora) agora é só a defesa: não quebra. */
    @Test public void backAloneNoLongerBreaks() {
        Match m = new Match();
        m.cpu.superMeter = CombatConfig.METER_PER_BAR;
        m.combo("H", 3, Match.BACK);
        assertFalse(m.engine.cues().contains("COMBO_BREAKER"));
        assertEquals(3, m.engine.session(1).hitCount);
    }

    /** Quem fica batendo M + H não ganha a janela. */
    @Test public void mashingMhDoesNotBreak() {
        int contact = Match.contactOf("H");
        Match m = new Match();
        m.cpu.superMeter = CombatConfig.METER_PER_BAR;
        String[] seq = {"L", "M", "H"};
        int k = 0;
        m.in[0].light = true;
        boolean broke = false;
        for (int i = 0; i < 90 && !broke; i++) {
            if (i >= 2 && i <= contact && (contact - i) % 6 == 3) m.in[1].pushblock = true;
            m.step();
            for (CombatEngine.HitEvent h : m.engine.events()) {
                if (h.attacker == 0 && !h.blocked && k < 2) {
                    k++;
                    if ("M".equals(seq[k])) m.in[0].medium = true; else m.in[0].heavy = true;
                }
            }
            broke = m.engine.cues().contains("COMBO_BREAKER");
        }
        assertFalse(broke);
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
        // M + H em cima do M (o 2º golpe, não fecha a sequência): nada.
        m.combo("M", 3);
        assertFalse(m.engine.cues().contains("COMBO_BREAKER"));
        assertEquals(CombatConfig.METER_PER_BAR, m.cpu.superMeter);
    }

    @Test public void pressingTooEarlyDoesNotBreak() {
        Match m = new Match();
        m.cpu.superMeter = CombatConfig.METER_PER_BAR;
        m.combo("H", 25);
        assertFalse(m.engine.cues().contains("COMBO_BREAKER"));
        assertEquals(3, m.engine.session(1).hitCount);
    }

    /** A CPU que quebra o combo sempre emenda o dela no atordoado. */
    @Test public void theCpuAlwaysCombosAfterItsBreaker() {
        for (int level = 1; level <= 8; level++) {
            int contact = Match.contactOf("H");
            Match m = new Match();
            m.cpu.superMeter = CombatConfig.METER_PER_BAR;
            AiController ai = new AiController(new OpponentAi(new java.util.Random(level)), new java.util.Random(level * 7L));
            ai.setEnabled(true);
            ai.setLevel(level);
            String[] seq = {"L", "M", "H"};
            int k = 0;
            m.in[0].light = true;
            boolean broke = false;
            for (int i = 0; i < 90 && !broke; i++) {
                ai.fill(m.engine, 1, m.in[1]);
                // O aperto em cima da hora (o que a CPU às vezes acerta no palpite), garantido aqui.
                m.in[1].pushblock = i == contact - 3;
                m.in[1].light = m.in[1].medium = m.in[1].heavy = false;
                m.step();
                for (CombatEngine.HitEvent h : m.engine.events()) {
                    if (h.attacker == 0 && !h.blocked && k < 2) {
                        k++;
                        if ("M".equals(seq[k])) m.in[0].medium = true; else m.in[0].heavy = true;
                    }
                }
                broke = m.engine.cues().contains("COMBO_BREAKER");
            }
            assertTrue("nível " + level + ": quebrou", broke);
            boolean counter = false;
            for (int i = 0; i < 60 && !counter; i++) {
                ai.fill(m.engine, 1, m.in[1]);
                m.step();
                for (CombatEngine.HitEvent h : m.engine.events()) if (h.attacker == 1 && !h.blocked) counter = true;
            }
            assertTrue("nível " + level + ": depois do breaker a CPU emenda o combo", counter);
        }
    }
}
