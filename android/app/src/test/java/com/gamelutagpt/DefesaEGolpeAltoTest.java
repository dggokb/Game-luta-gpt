package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * As três peças da defesa contra a defesa: o golpe alto (→ + M, pega o agachado), o chip
 * (especial e Super defendidos arranham, em vida vermelha, sem matar) e a defesa perfeita
 * (trás em cima da hora: sem gastar guarda, um pouco de vida, blockstun curto).
 */
public class DefesaEGolpeAltoTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();

    private static final class Match {
        final FighterState first = new FighterState(BASE, "P1");
        final FighterState cpu = new FighterState(NPC, "CPU");
        final CombatEngine engine;
        final FighterInput[] in = {new FighterInput(), new FighterInput()};

        Match(float x0, float x1) {
            engine = new CombatEngine(first, cpu, x0, x1, new CombatConfig());
        }

        CombatFighter f(int i) { return engine.fighter(i); }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
        }

        void steps(int n) { for (int i = 0; i < n; i++) step(); }

        /** Direção de tela de {@code relative} (1 frente, 5 trás, 4 baixo-trás) para o lutador i. */
        int screen(int i, int relative) { return MotionParser.relative(relative, f(i).facing); }

        /** Anda até o primeiro contato em {@code defender} (ou null). */
        CombatEngine.HitEvent untilContact(int defender, int frames) {
            for (int i = 0; i < frames; i++) {
                step();
                for (CombatEngine.HitEvent e : engine.events()) if (e.defender == defender) return e;
            }
            return null;
        }

        /**
         * O CPU aperta M; P1 fica parado e aperta trás {@code lead} quadros antes de o golpe
         * ficar ativo (0 = nunca). Devolve o contato.
         */
        CombatEngine.HitEvent cpuMediumWithBackAt(int lead) {
            in[1].medium = true;
            step();
            AttackDefinition attack = f(1).attack;
            for (int i = 0; i < 60; i++) {
                if (lead > 0 && attack.startupFrames - f(1).attackFrame <= lead) in[0].direction = screen(0, 5);
                step();
                for (CombatEngine.HitEvent e : engine.events()) if (e.defender == 0) return e;
            }
            return null;
        }
    }

    // ------------------------------------------------------------ golpe alto

    @Test public void everyFighterGetsTheOverheadFromItsMedium() {
        for (String id : new String[] {"player_base", "monster_npc", "p02", "p06", "p10", "p12"}) {
            CharacterDefinition c = GeneratedCharacters.get(id);
            AttackDefinition overhead = c.attack(GolpeAlto.BINDING);
            assertNotNull(id + " tem o 6M", overhead);
            assertTrue(overhead.overhead);
            assertFalse(overhead.low);
            assertEquals(GolpeAlto.STARTUP, overhead.startupFrames);
            assertEquals("só uma vez por combo", 1, overhead.maxUsesPerCombo);
            for (String t : overhead.cancelInto()) {
                assertTrue(id + ": o 6M só cancela em especial/Super, não em " + t,
                    t.equals("SUPER") || t.startsWith("S"));
            }
        }
    }

    @Test public void aPackOverheadWithItsOwnAnimationKeepsTheSharedTiming() {
        // p15: o 6M do pacote, com o vídeo próprio (M_BAIXO), no lugar do montado do M.
        CharacterDefinition c = GeneratedCharacters.get("p15");
        CharacterDefinition.Move oh = c.moves.get(GolpeAlto.BINDING), m = c.moves.get("M");
        assertFalse("não é o 6M montado do M", GolpeAlto.derived(c, GolpeAlto.BINDING));
        assertNotSame(m.animation, oh.animation);
        assertTrue(oh.attack.overhead);
        assertFalse(oh.attack.low);
        assertEquals(GolpeAlto.STARTUP, oh.attack.startupFrames);
        assertEquals(GolpeAlto.ACTIVE, oh.attack.activeFrames);
        assertEquals(GolpeAlto.RECOVERY, oh.attack.recoveryFrames);
        assertEquals(1, oh.attack.maxUsesPerCombo);
        assertEquals(m.attack.damage * GolpeAlto.DAMAGE_PERMILLE / 1000, oh.attack.damage);
    }

    @Test public void theOverheadHoldsTheWindUpInsteadOfSlowingTheWholeClip() {
        for (String id : new String[] {"player_base", "p02", "p06", "p10"}) {
            CharacterDefinition c = GeneratedCharacters.get(id);
            CharacterDefinition.Move m = c.moves.get("M"), oh = c.moves.get(GolpeAlto.BINDING);
            if (m.animation == null) continue;
            float fps = CombatConfig.FPS;
            int windUp = Math.max(0, m.attack.startupFrames - 2);
            // A preparação roda no ritmo do M (sem câmera lenta)...
            assertEquals(id + ": preparação no ritmo do M", m.animationTime(windUp / fps), oh.animationTime(windUp / fps), 1e-4f);
            // ...a pose segura até o impacto do 6M, que cai no quadro do impacto do M...
            assertEquals(id + ": impacto alinhado", m.animationTime(m.attack.startupFrames / fps),
                oh.animationTime(GolpeAlto.STARTUP / fps), 1e-4f);
            float meio = oh.animationTime((windUp + GolpeAlto.STARTUP) / 2f / fps);
            assertTrue(id + ": pose segurada", meio >= m.animationTime(windUp / fps) - 1e-4f &&
                meio <= m.animationTime(m.attack.startupFrames / fps) + 1e-4f);
            // ...e termina no fim do clipe.
            assertEquals(m.animation.duration, oh.animationTime(oh.attack.totalFrames / fps), 1e-3f);
        }
    }

    @Test public void forwardMediumFromNeutralIsTheOverheadButNotInsideACombo() {
        Match m = new Match(500f, 600f);
        m.in[0].direction = m.screen(0, 1);
        m.in[0].medium = true;
        m.step();
        assertEquals(GolpeAlto.BINDING, m.f(0).attack.id);

        AttackDefinition light = BASE.attack("L");
        if (!light.cancelsInto("M")) return;
        Match c = new Match(500f, 600f);
        c.engine.config.justDefenseFrames = -1;
        c.in[0].light = true;
        c.step();
        assertNotNull(c.untilContact(1, 30));
        c.in[0].direction = c.screen(0, 1);
        c.in[0].medium = true;
        for (int i = 0; i < 30 && c.f(0).attacking() && "L".equals(c.f(0).attack.id); i++) c.step();
        assertEquals("dentro do combo o → + M continua sendo o M", "M", c.f(0).attack.id);
    }

    @Test public void theOverheadHitsTheCrouchingGuardAndIsBlockedStanding() {
        Match low = new Match(500f, 600f);
        low.engine.config.justDefenseFrames = -1;
        low.in[1].direction = low.screen(1, 4);
        low.steps(10);
        assertEquals(CombatFighter.GUARD_LOW, low.engine.guardFromInput(low.f(1)));
        low.in[0].direction = low.screen(0, 1);
        low.in[0].medium = true;
        CombatEngine.HitEvent e = low.untilContact(1, 40);
        assertNotNull(e);
        assertFalse("agachado não segura o golpe alto", e.blocked);

        Match high = new Match(500f, 600f);
        high.engine.config.justDefenseFrames = -1;
        high.in[1].direction = high.screen(1, 5);
        high.steps(10);
        high.in[0].direction = high.screen(0, 1);
        high.in[0].medium = true;
        e = high.untilContact(1, 40);
        assertNotNull(e);
        assertTrue("em pé defende", e.blocked);
    }

    @Test public void theOverheadWarnsWhenItStarts() {
        Match m = new Match(500f, 600f);
        m.in[0].direction = m.screen(0, 1);
        m.in[0].medium = true;
        m.step();
        assertTrue(m.engine.cues().contains("OVERHEAD"));
    }

    // ------------------------------------------------------------ chip

    @Test public void chipTurnsIntoRedLifeAndNeverKnocksOut() {
        FighterState s = new FighterState(BASE, "P1");
        s.chip(30);
        assertEquals(s.profile.maxLife - 30, s.life);
        assertEquals("todo o arranhão é vida vermelha", 30, s.recoverableLife);
        s.life = 5;
        s.recoverableLife = 0;
        s.chip(40);
        assertEquals("nunca o último ponto", 1, s.life);
        s.chip(40);
        assertEquals(1, s.life);
    }

    @Test public void aBlockedSpecialChipsAndABlockedNormalDoesNot() {
        Match normal = new Match(500f, 600f);
        normal.engine.config.justDefenseFrames = -1;
        normal.in[1].direction = normal.screen(1, 5);
        normal.in[0].heavy = true;
        CombatEngine.HitEvent e = normal.untilContact(1, 40);
        assertNotNull(e);
        assertTrue(e.blocked);
        assertEquals("normal defendido não arranha", normal.cpu.profile.maxLife, normal.cpu.life);

        Match special = new Match(500f, 1100f);
        special.engine.config.justDefenseFrames = -1;
        special.in[1].direction = special.screen(1, 5);
        special.in[0].special = "M";
        e = special.untilContact(1, 200);
        assertNotNull(e);
        assertTrue(e.blocked);
        int lost = special.cpu.profile.maxLife - special.cpu.life;
        assertTrue("especial defendido arranha", lost > 0);
        assertEquals("o arranhão volta (vida vermelha)", lost, special.cpu.recoverableLife);
    }

    @Test public void chipNeverFinishesTheFight() {
        Match m = new Match(500f, 1100f);
        m.engine.config.justDefenseFrames = -1;
        m.cpu.life = 1;
        m.in[1].direction = m.screen(1, 5);
        m.in[0].special = "M";
        CombatEngine.HitEvent e = m.untilContact(1, 200);
        assertNotNull(e);
        assertTrue(e.blocked);
        assertEquals(1, m.cpu.life);
        assertFalse(m.f(1).ko());
    }

    // ------------------------------------------------------------ defesa perfeita

    @Test public void backRightBeforeContactIsAPerfectDefense() {
        Match m = new Match(600f, 720f);
        m.first.life = m.first.profile.maxLife / 2;
        m.first.recoverableLife = m.first.profile.maxLife / 4;
        CombatEngine.HitEvent e = m.cpuMediumWithBackAt(3);
        assertNotNull(e);
        assertTrue(e.blocked);
        assertTrue(m.engine.cues().contains("JUST_DEFENSE"));
        assertTrue(m.f(0).justDefended);
        assertEquals("não gasta guarda", CombatConfig.GUARD_MAX, m.first.guard);
        AttackDefinition attack = NPC.attack("M");
        assertEquals("blockstun curto",
            Math.max(1, attack.blockstunFrames * m.engine.config.justDefenseBlockstunPermille / 1000), m.f(0).stunTotal);
        int heal = m.first.profile.maxLife * m.engine.config.justDefenseHealPermille / 1000;
        assertEquals("a vida volta", m.first.profile.maxLife / 2 + heal, m.first.life);
        assertEquals("primeiro a vermelha", m.first.profile.maxLife / 4 - heal, m.first.recoverableLife);
    }

    @Test public void holdingBackFromBeforeIsAPlainBlock() {
        Match m = new Match(600f, 720f);
        m.in[0].direction = m.screen(0, 5);
        m.steps(2);
        m.f(0).x = 600f;   // andou para trás: volta ao alcance
        CombatEngine.HitEvent e = m.cpuMediumWithBackAt(0);
        assertNotNull(e);
        assertTrue(e.blocked);
        assertFalse(m.engine.cues().contains("JUST_DEFENSE"));
        assertTrue("a defesa comum gasta a guarda", m.first.guard < CombatConfig.GUARD_MAX);
    }

    @Test public void mashingBackDoesNotOpenTheWindow() {
        Match m = new Match(600f, 720f);
        // Apertou a defesa (baixo-trás, para não virar backdash) e soltou agora há pouco: o
        // aperto em cima da hora não vale.
        m.in[0].direction = m.screen(0, 4);
        m.step();
        m.in[0].direction = 0;
        CombatEngine.HitEvent e = m.cpuMediumWithBackAt(3);
        assertNotNull(e);
        assertTrue(e.blocked);
        int startup = NPC.attack("M").startupFrames;
        if (startup < m.engine.config.justDefenseLockoutFrames) {
            assertFalse("bater trás sem parar não vale", m.engine.cues().contains("JUST_DEFENSE"));
            assertTrue(m.first.guard < CombatConfig.GUARD_MAX);
        }
    }

    @Test public void perfectDefenseTakesNoChip() {
        // Primeiro sem defender: em que quadro a energia chega.
        Match probe = new Match(500f, 1100f);
        probe.in[0].special = "M";
        int contact = -1;
        for (int i = 0; i < 200 && contact < 0; i++) {
            probe.step();
            for (CombatEngine.HitEvent e : probe.engine.events()) if (e.defender == 1) contact = i;
        }
        assertTrue(contact > 10);
        // De novo, apertando trás 3 quadros antes de ela chegar.
        Match m = new Match(500f, 1100f);
        m.in[0].special = "M";
        for (int i = 0; i <= contact; i++) {
            if (i >= contact - 3) m.in[1].direction = m.screen(1, 5);
            m.step();
        }
        assertTrue(m.engine.cues().contains("JUST_DEFENSE"));
        assertEquals("sem chip na defesa perfeita", m.cpu.profile.maxLife, m.cpu.life);
    }
}
