package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Combo breaker: apanhando num combo que já tem 2 acertos, L, M ou H quebra na hora, em qualquer
 * golpe — por 1 barra. O "!" fica sobre quem apanha enquanto dá. Quem batia é jogado para longe,
 * fica atordoado um instante e quem quebrou corre sozinho até ele (o dash automático); o
 * atordoamento curto não deixa troco garantido (docs/balanco/ajustes.md).
 */
public class ComboBreakerTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();

    private static final class Match {
        final FighterState first = new FighterState(BASE, "P1");
        final FighterState cpu = new FighterState(NPC, "CPU");
        final CombatEngine engine;
        final FighterInput[] in = {new FighterInput(), new FighterInput()};

        Match() { this(new CombatConfig()); }

        Match(CombatConfig config) { engine = new CombatEngine(first, cpu, 500f, 600f, config); }

        CombatFighter f(int i) { return engine.fighter(i); }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
        }

        int back(int i) { return MotionParser.relative(5, f(i).facing); }

        /** O aperto do CPU: L, M, H, só trás, ou SUPER (que não quebra). */
        static final int L = 0, BACK = 1, SUPER = 2, M = 3, H = 4;

        void combo(String target, int lead) { combo(target, lead, L); }

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
                    if (how == L) in[1].light = true;
                    else if (how == M) in[1].medium = true;
                    else if (how == H) in[1].heavy = true;
                    else if (how == BACK) in[1].direction = back(1);
                    else in[1].superAttack = true;
                }
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

    @Test public void aButtonRightBeforeTheHeavyEnderBreaksTheCombo() {
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

    /**
     * O estalo joga quem batia para longe (arrastando o pé) e quem quebrou corre sozinho até ele
     * (sem → →), parando ao alcance do L. Quem batia se recompõe antes de a corrida chegar.
     */
    @Test public void theBreakerThrowsTheAttackerAwayAndTheBreakerDashesAfterHim() {
        Match m = new Match();
        m.cpu.superMeter = CombatConfig.METER_PER_BAR;
        m.combo("H", 3);
        assertTrue(m.engine.cues().contains("COMBO_BREAKER"));
        CombatConfig config = m.engine.config;
        float attackerStart = m.f(0).x, breakerStart = m.f(1).x;
        boolean ran = false;
        int recompos = -1;
        for (int i = 0; i < config.comboBreakerHitstopFrames + config.comboBreakerPushFrames; i++) {
            m.step();
            ran |= m.f(1).forwardDashing;
            if (recompos < 0 && m.f(0).canAct()) recompos = i;
        }
        assertTrue("jogado para trás: " + Math.abs(m.f(0).x - attackerStart),
            Math.abs(m.f(0).x - attackerStart) > 0.6f * config.comboBreakerPushGap);
        assertTrue("corre sozinho (dash automático)", ran);
        for (int i = 0; i < config.comboBreakerChaseFrames && m.f(1).breakerChase > 0; i++) {
            m.step();
            if (recompos < 0 && m.f(0).canAct()) recompos = i;
        }
        assertTrue("correu um bom pedaço: " + Math.abs(m.f(1).x - breakerStart),
            Math.abs(m.f(1).x - breakerStart) > 0.6f * config.comboBreakerPushGap);
        assertTrue("chegou ao alcance do L: " + m.gap(), m.gap() <= m.touching() + config.comboBreakerCloseGap + 12f);
        assertEquals("parou de correr", 0, m.f(1).breakerChase);
        assertTrue("quem batia já se recompôs", m.f(0).canAct());
    }

    /**
     * H1 (atordoamento 4): depois do estalo quem quebrou age {@code comboBreakerStunFrames + 1}
     * quadros antes de quem batia (+5), com ele à folga do empurrão. Medido com os dois parados.
     */
    @Test public void afterTheBreakerTheBreakerActsStunPlusOneFramesFirst() {
        Match m = new Match();
        m.cpu.superMeter = CombatConfig.METER_PER_BAR;
        m.combo("H", 3);
        assertTrue(m.engine.cues().contains("COMBO_BREAKER"));
        int breakerFree = -1, attackerFree = -1;
        for (int i = 1; i <= 120 && attackerFree < 0; i++) {
            m.step();
            if (breakerFree < 0 && m.f(1).canAct()) breakerFree = i;
            if (attackerFree < 0 && m.f(0).canAct()) attackerFree = i;
        }
        assertEquals(m.engine.config.comboBreakerStunFrames + 1, attackerFree - breakerFree);
    }

    /**
     * Roteiro padrão (H1): cada um dos 16 quebra o L > M > H do Ryūga e aperta cada normal de chão
     * assim que alcança (correndo até lá); quem batia defende assim que pode. Normal que demora mais
     * que a vantagem de quem quebrou para sair nunca entra limpo; os rápidos (L, 2L e alguns M) ainda
     * entram na ponta do alcance (docs/balanco/ajustes.md, H1; com 20 quadros entrava quase tudo). O
     * golpe que quem batia deixou no buffer para fechar o combo não sai sozinho depois do estalo.
     */
    @Test public void aNormalSlowerThanTheBreakerAdvantageNeverLandsGuaranteed() {
        String[] roster = {"player_base", "p02", "p03", "p04", "p05", "p06", "p07", "p08", "p09", "p10", "p11", "p12", "p13", "p14", "p15", "p16"};
        String[] normals = {"L", "2L", "M", "2M", "H", "2H"};
        CombatConfig config = new CombatConfig();
        int advantage = config.comboBreakerStunFrames + 1;
        int checked = 0;
        for (String id : roster) {
            CharacterDefinition ch = GeneratedCharacters.get(id);
            for (String g : normals) {
                if (ch.attack(g) == null || ch.attack(g).startupFrames <= advantage) continue;
                checked++;
                FighterState breaker = new FighterState(ch, "CPU");
                breaker.superMeter = CombatConfig.METER_PER_BAR;
                CombatEngine e = new CombatEngine(new FighterState(BASE, "P1"), breaker, 500f, 600f, config);
                FighterInput[] in = {new FighterInput(), new FighterInput()};
                String[] seq = {"L", "M", "H"};
                int k = 0;
                boolean broke = false, answered = false;
                in[0].light = true;
                for (int i = 0; i < 240 && !answered; i++) {
                    CombatFighter a = e.fighter(0), d = e.fighter(1);
                    if (!broke) {
                        if (k == 2 && e.canBreakCombo(d)) in[1].light = true;
                    } else {
                        // Quem batia defende (agachado, menos contra golpe alto ou no ar) assim que pode.
                        boolean right = d.x >= a.x;
                        boolean high = !d.grounded || d.attacking() && d.attack != null && d.attack.overhead;
                        in[0].direction = high ? (right ? 5 : 1) : (right ? 4 : 2);
                        if (d.canAct()) {
                            float gap = Math.abs(a.x - d.x);
                            if (CpuReach.reaches(ch, g, false, gap, a.body(), a.crouchingBody())) {
                                if (g.startsWith("2")) in[1].direction = d.facing > 0 ? 2 : 4;
                                if (g.endsWith("L")) in[1].light = true; else if (g.endsWith("M")) in[1].medium = true; else in[1].heavy = true;
                            } else {
                                in[1].direction = a.x > d.x ? 1 : 5;
                                in[1].dash = true;
                            }
                        }
                    }
                    e.step(in[0], in[1]);
                    in[0].clearPresses();
                    in[1].clearPresses();
                    for (CombatEngine.HitEvent h : e.events()) {
                        if (!broke && h.attacker == 0 && !h.blocked && k < 2) {
                            k++;
                            if ("M".equals(seq[k])) in[0].medium = true; else in[0].heavy = true;
                        }
                        if (broke && h.attacker == 1) {
                            assertTrue(id + " " + g + " (sai em " + ch.attack(g).startupFrames + " q): o troco não entra garantido", h.blocked);
                            answered = true;
                        }
                    }
                    if (!broke && e.cues().contains("COMBO_BREAKER")) broke = true;
                }
                assertTrue(id + " " + g + ": quebrou", broke);
            }
        }
        assertTrue("normais conferidos: " + checked, checked > 40);
    }

    /** Com a corrida ligada (a opção antiga), quem quebrou corre sozinho até ele. */
    @Test public void withTheChaseOptionTheDefenderRunsAlone() {
        CombatConfig config = new CombatConfig();
        config.comboBreakerChaseFrames = 40;
        config.comboBreakerStunFrames = 44;
        Match m = new Match(config);
        m.cpu.superMeter = CombatConfig.METER_PER_BAR;
        m.combo("H", 3);
        assertTrue(m.engine.cues().contains("COMBO_BREAKER"));
        float breakerStart = m.f(1).x;
        boolean ran = false;
        for (int i = 0; i < config.comboBreakerHitstopFrames + config.comboBreakerPushFrames; i++) {
            m.step();
            ran |= m.f(1).forwardDashing;
        }
        assertTrue("correu sem → →", ran);
        for (int i = 0; i < 30; i++) m.step();
        assertTrue("correu um bom pedaço: " + Math.abs(m.f(1).x - breakerStart),
            Math.abs(m.f(1).x - breakerStart) > 0.6f * config.comboBreakerPushGap);
        assertTrue("chegou perto: " + m.gap(), m.gap() <= m.touching() + config.comboBreakerCloseGap + 12f);
        assertEquals("parou de correr", 0, m.f(1).breakerChase);
    }

    @Test public void withTheChaseOptionHoldingBackGivesUpTheRun() {
        CombatConfig config = new CombatConfig();
        config.comboBreakerChaseFrames = 40;
        Match m = new Match(config);
        m.cpu.superMeter = CombatConfig.METER_PER_BAR;
        m.combo("H", 3);
        assertTrue(m.engine.cues().contains("COMBO_BREAKER"));
        for (int i = 0; i < 60 && !m.f(1).forwardDashing; i++) m.step();
        assertTrue(m.f(1).forwardDashing);
        m.in[1].direction = m.back(1);
        m.step();
        assertEquals(0, m.f(1).breakerChase);
        m.in[1].direction = 0;
        float before = m.gap();
        for (int i = 0; i < 10; i++) m.step();
        assertTrue("não corre mais", m.gap() >= before - 1f);
    }

    /** Não precisa de tempo certo: logo depois do 2º acerto (bem antes do H) também quebra. */
    @Test public void superBreaksAnywhereInTheCombo() {
        int contactM = Match.contactOf("M"), contactH = Match.contactOf("H");
        assertTrue(contactH - contactM > 6);
        Match m = new Match();
        m.cpu.superMeter = CombatConfig.METER_PER_BAR;
        m.combo("H", contactH - contactM - 2);
        assertTrue(m.engine.cues().contains("COMBO_BREAKER"));
        assertEquals("livre na hora", CombatFighter.Status.NEUTRAL, m.f(1).status);
        assertEquals(0, m.cpu.superMeter);
    }

    /** No ar, com o atordoamento acabado, o botão é o tech (cai de pé) e a barra fica. */
    @Test public void afterTheAirHitstunTheButtonTechsInsteadOfBreaking() {
        Match m = new Match();
        m.cpu.superMeter = CombatConfig.METER_PER_BAR;
        String[] seq = {"L", "M", "H"};
        int k = 0;
        m.in[0].light = true;
        boolean ready = false;
        for (int i = 0; i < 200 && !ready; i++) {
            m.step();
            for (CombatEngine.HitEvent h : m.engine.events()) {
                if (h.attacker == 0 && !h.blocked && k < 2) {
                    k++;
                    if ("M".equals(seq[k])) m.in[0].medium = true; else m.in[0].heavy = true;
                }
            }
            ready = m.f(1).status == CombatFighter.Status.AIR_HITSTUN && m.f(1).stunLeft == 0 && !m.f(1).frozen();
        }
        assertTrue("o H lançou e o atordoamento no ar acabou", ready);
        assertEquals(-1, m.engine.breakerCue(m.f(1)));
        boolean hardFall = m.f(1).hardFall;
        m.in[1].light = true;
        boolean tech = false;
        for (int i = 0; i < 4; i++) {
            m.step();
            tech |= m.engine.cues().contains("AIR_TECH");
            assertFalse(m.engine.cues().contains("COMBO_BREAKER"));
        }
        // A queda forte (o H derruba) não deixa cair de pé; a barra fica de qualquer jeito.
        assertTrue("caiu de pé (ou a queda é forte)", tech || hardFall);
        assertEquals("a barra fica", CombatConfig.METER_PER_BAR, m.cpu.superMeter);
    }

    /** Qualquer botão de golpe quebra: M e H também. */
    @Test public void mediumAndHeavyBreakToo() {
        for (int how : new int[] {Match.M, Match.H}) {
            Match m = new Match();
            m.cpu.superMeter = CombatConfig.METER_PER_BAR;
            m.combo("H", 3, how);
            assertTrue(m.engine.cues().contains("COMBO_BREAKER"));
        }
    }

    /** O SUPER não quebra (vai para o buffer, como sempre). */
    /** Sendo arremessado (agarrado ou no agarrão de um especial) não se quebra o combo. */
    @Test public void beingThrownCannotBreak() {
        Match m = new Match();
        m.cpu.superMeter = CombatConfig.METER_PER_BAR;
        // L > M entram: o combo já pode ser quebrado.
        int contact = Match.contactOf("H");
        m.in[0].light = true;
        String[] seq = {"L", "M", "H"};
        int k = 0;
        for (int i = 0; i < contact - 2; i++) {
            m.step();
            for (CombatEngine.HitEvent h : m.engine.events()) {
                if (h.attacker == 0 && !h.blocked && k < 2) {
                    k++;
                    if ("M".equals(seq[k])) m.in[0].medium = true; else m.in[0].heavy = true;
                }
            }
        }
        assertTrue("apanhando no combo, quebra", m.engine.canBreakCombo(m.f(1)));
        CombatFighter.Status antes = m.f(1).status;
        m.f(1).status = CombatFighter.Status.THROWN;
        assertFalse("sendo arremessado, não quebra", m.engine.canBreakCombo(m.f(1)));
        m.f(1).status = antes;
    }

    @Test public void superDoesNotBreak() {
        Match m = new Match();
        m.cpu.superMeter = CombatConfig.METER_PER_BAR;
        m.combo("H", 3, Match.SUPER);
        assertFalse(m.engine.cues().contains("COMBO_BREAKER"));
    }

    @Test public void backAloneDoesNotBreak() {
        Match m = new Match();
        m.cpu.superMeter = CombatConfig.METER_PER_BAR;
        m.combo("H", 3, Match.BACK);
        assertFalse(m.engine.cues().contains("COMBO_BREAKER"));
        assertEquals(3, m.engine.session(1).hitCount);
    }

    @Test public void withoutMeterTheEnderLands() {
        Match m = new Match();
        m.cpu.superMeter = CombatConfig.METER_PER_BAR - 1;
        m.combo("H", 3);
        assertFalse(m.engine.cues().contains("COMBO_BREAKER"));
        assertEquals(3, m.engine.session(1).hitCount);
    }

    /** No primeiro golpe do combo ainda não dá: o L antes do M entrar não quebra. */
    @Test public void theFirstHitCannotBeBroken() {
        Match m = new Match();
        m.cpu.superMeter = CombatConfig.METER_PER_BAR;
        m.combo("M", 3);
        assertFalse(m.engine.cues().contains("COMBO_BREAKER"));
        assertEquals(CombatConfig.METER_PER_BAR, m.cpu.superMeter);
    }

    /** Super não se quebra: apanhando dele (vários acertos), os botões não fazem nada e não tem "!". */
    @Test public void aSuperCannotBeBroken() {
        Match m = new Match();
        m.cpu.superMeter = CombatConfig.METER_PER_BAR;
        m.first.superMeter = CombatConfig.METER_PER_BAR * 3;
        m.in[0].superAttack = true;
        int superHits = 0;
        boolean broke = false;
        for (int i = 0; i < 200 && !broke; i++) {
            if (m.f(1).inHitstun()) m.in[1].light = true;
            m.step();
            for (CombatEngine.HitEvent h : m.engine.events()) if (h.attacker == 0 && !h.blocked) superHits++;
            if (superHits > 0) assertEquals("sem \"!\" no Super", -1, m.engine.breakerCue(m.f(1)));
            broke = m.engine.cues().contains("COMBO_BREAKER");
        }
        assertTrue("o Super entrou com vários acertos: " + superHits, superHits >= 2);
        assertFalse(broke);
    }

    /** A CPU forte que quebra o combo vai atrás do troco: quem não defende leva. */
    @Test public void theCpuGoesForTheCounterAfterItsBreaker() {
        for (int level = 6; level <= 8; level++) {
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
                m.in[1].light = i == contact - 3;
                m.in[1].medium = m.in[1].heavy = m.in[1].pushblock = m.in[1].superAttack = false;
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
            // Com 4 quadros de atordoamento o troco não é garantido: a CPU vai atrás de quem não defende.
            boolean counter = false;
            for (int i = 0; i < 120 && !counter; i++) {
                ai.fill(m.engine, 1, m.in[1]);
                m.step();
                for (CombatEngine.HitEvent h : m.engine.events()) if (h.attacker == 1 && !h.blocked) counter = true;
            }
            assertTrue("nível " + level + ": depois do breaker a CPU acerta quem não defende", counter);
        }
    }

    /** O "!" de cada quadro do L > M > H sem aperto (quadros que faltam até a batida, ou -1). */
    private static java.util.List<Integer> cuesOfTheCombo(int meter) {
        Match m = new Match();
        m.cpu.superMeter = meter;
        java.util.List<Integer> cues = new java.util.ArrayList<>();
        String[] seq = {"L", "M", "H"};
        int k = 0;
        m.in[0].light = true;
        for (int i = 0; i < 200; i++) {
            m.step();
            cues.add(m.engine.breakerCue(m.f(1)));
            for (CombatEngine.HitEvent h : m.engine.events()) {
                if (h.attacker == 0 && !h.blocked && k < 2) {
                    k++;
                    if ("M".equals(seq[k])) m.in[0].medium = true; else m.in[0].heavy = true;
                }
            }
        }
        return cues;
    }

    /** O "!" aparece no 2º acerto e fica até o combo acabar (antes do 2º acerto não tem). */
    @Test public void theCueStaysForTheWholeBreakableCombo() {
        int contactM = Match.contactOf("M"), contactH = Match.contactOf("H");
        java.util.List<Integer> cues = cuesOfTheCombo(CombatConfig.METER_PER_BAR);
        for (int i = 0; i < contactM; i++) assertEquals("só o L entrou: sem aviso", -1, (int)cues.get(i));
        for (int i = contactM; i < contactH; i++) assertEquals("do M até o H: " + cues, 0, (int)cues.get(i));
        assertEquals("some quando o combo acaba", -1, (int)cues.get(cues.size() - 1));
    }

    /**
     * A CPU forte quebra os combos que decide quebrar (45% no nível 8): o aperto cabe no
     * atordoamento, não chega tarde.
     */
    @Test public void aStrongCpuBreaksSomeCombos() {
        int broken = 0;
        for (int seed = 0; seed < 30; seed++) {
            Match m = new Match();
            m.cpu.superMeter = CombatConfig.METER_PER_BAR;
            AiController ai = new AiController(new OpponentAi(new java.util.Random(seed)), new java.util.Random(seed * 13L));
            ai.setEnabled(true);
            ai.setLevel(8);
            String[] seq = {"L", "M", "H"};
            int k = 0;
            m.in[0].light = true;
            for (int i = 0; i < 90; i++) {
                ai.fill(m.engine, 1, m.in[1]);
                m.step();
                for (CombatEngine.HitEvent h : m.engine.events()) {
                    if (h.attacker == 0 && !h.blocked && k < 2) {
                        k++;
                        if ("M".equals(seq[k])) m.in[0].medium = true; else m.in[0].heavy = true;
                    }
                }
                if (m.engine.cues().contains("COMBO_BREAKER")) { broken++; break; }
            }
        }
        assertTrue("quebrou " + broken + " de 30", broken >= 4 && broken <= 26);
    }

    @Test public void noCueWithoutMeter() {
        for (int cue : cuesOfTheCombo(CombatConfig.METER_PER_BAR - 1)) assertEquals(-1, cue);
    }
}
