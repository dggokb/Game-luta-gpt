package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/** Guard break: cada defesa gasta a guarda; zerou, a defesa quebra e ele fica aberto. */
public class GuardBreakTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();

    private static final class Match {
        final FighterState first = new FighterState(BASE, "P1");
        final FighterState cpu = new FighterState(NPC, "CPU");
        final CombatEngine engine = new CombatEngine(first, cpu, 600f, 720f, new CombatConfig());
        final FighterInput[] in = {new FighterInput(), new FighterInput()};

        CombatFighter f(int i) { return engine.fighter(i); }
        CombatConfig config() { return engine.config; }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
        }

        void steps(int n) { for (int i = 0; i < n; i++) step(); }

        /**
         * P1 segura para trás (defesa em pé), já segurando de antes: a defesa comum, não a
         * perfeita (essa fica em JustDefenseTest).
         */
        void holdGuard() {
            in[0].direction = f(0).facing > 0 ? 5 : 1;
            engine.config.justDefenseFrames = -1;
        }

        /** O CPU aperta M e o motor anda até o golpe encostar. O contato (ou null). */
        CombatEngine.HitEvent attackUntilContact() {
            in[1].medium = true;
            for (int i = 0; i < 60; i++) {
                step();
                for (CombatEngine.HitEvent e : engine.events()) if (e.defender == 0) return e;
            }
            return null;
        }
    }

    @Test public void blockingSpendsTheGuardByTheBlockstun() {
        Match m = new Match();
        assertEquals(CombatConfig.GUARD_MAX, m.first.guard);
        m.holdGuard();
        CombatEngine.HitEvent e = m.attackUntilContact();
        assertNotNull(e);
        assertTrue("defendeu", e.blocked);
        AttackDefinition attack = NPC.attack("M");
        assertEquals(CombatConfig.GUARD_MAX - attack.blockstunFrames * m.config().guardDrainPerBlockstunFrame,
            m.first.guard);
        assertFalse(m.engine.cues().contains("GUARD_BREAK"));
    }

    @Test public void theBlockThatEmptiesTheGuardBreaksIt() {
        Match m = new Match();
        m.first.guard = 1;
        m.holdGuard();
        CombatEngine.HitEvent e = m.attackUntilContact();
        assertNotNull(e);
        assertTrue("o golpe da quebra foi defendido: não tira vida", e.blocked);
        assertEquals(m.first.profile.maxLife, m.first.life);
        assertTrue(m.engine.cues().contains("GUARD_BREAK"));
        CombatFighter p = m.f(0);
        assertTrue(p.guardBroken);
        assertEquals("aberto: atordoado, sem defesa", CombatFighter.Status.HITSTUN, p.status);
        assertEquals(m.config().guardBreakStunFrames, p.stunLeft);
        assertEquals("a guarda volta cheia", CombatConfig.GUARD_MAX, m.first.guard);
        assertEquals(CombatFighter.GUARD_NONE, m.engine.guardFromInput(p));
    }

    @Test public void theNextAttackHitsThroughTheBrokenGuard() {
        Match m = new Match();
        m.first.guard = 1;
        m.holdGuard();
        assertNotNull(m.attackUntilContact());
        assertTrue(m.f(0).guardBroken);
        // O CPU termina o golpe e ataca de novo; P1 continua segurando para trás.
        for (int i = 0; i < 40 && m.f(1).attacking(); i++) m.step();
        assertTrue("ainda aberto", m.f(0).guardBroken);
        CombatEngine.HitEvent e = m.attackUntilContact();
        assertNotNull(e);
        assertFalse("segurar para trás não defende com a guarda quebrada", e.blocked);
        assertTrue(m.first.life < m.first.profile.maxLife);
    }

    @Test public void theBreakEndsAndTheFighterCanGuardAgain() {
        Match m = new Match();
        m.first.guard = 1;
        m.holdGuard();
        assertNotNull(m.attackUntilContact());
        m.steps(m.config().guardBreakHitstopFrames + m.config().guardBreakStunFrames + 2);
        CombatFighter p = m.f(0);
        assertFalse(p.guardBroken);
        assertEquals(CombatFighter.Status.NEUTRAL, p.status);
        assertNotEquals(CombatFighter.GUARD_NONE, m.engine.guardFromInput(p));
    }

    @Test public void theGuardComesBackOnlyAfterAPauseFromBlocking() {
        Match m = new Match();
        m.first.guard = 500;
        m.f(0).framesSinceBlock = 0;
        m.steps(m.config().guardRegenDelayFrames - 2);
        assertEquals("defendeu agora há pouco: ainda não volta", 500, m.first.guard);
        m.steps(12);
        assertTrue("depois da pausa ela volta", m.first.guard > 500);
        m.steps(CombatConfig.GUARD_MAX / m.config().guardRegenPerFrame + 10);
        assertEquals("e para no máximo", CombatConfig.GUARD_MAX, m.first.guard);
    }

    @Test public void refillingTheLifeRefillsTheGuard() {
        Match m = new Match();
        m.first.guard = 10;
        m.first.restoreLife();
        assertEquals(CombatConfig.GUARD_MAX, m.first.guard);
    }

    @Test public void aStringOfBlocksDoesNotBreakAFullGuard() {
        // Uma sequência L, M, H defendida gasta, mas está longe de quebrar a guarda cheia.
        int spent = 0;
        for (String id : new String[] {"L", "M", "H"}) {
            AttackDefinition a = NPC.attack(id);
            if (a != null) spent += a.blockstunFrames * new CombatConfig().guardDrainPerBlockstunFrame;
        }
        assertTrue("uma sequência: " + spent, spent > 0 && spent < CombatConfig.GUARD_MAX / 2);
    }
}
