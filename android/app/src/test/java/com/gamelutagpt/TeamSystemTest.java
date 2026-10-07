package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/** Team rules in the engine: raw tag, assist, Assist → Tag and the team meter (v0.79). */
public class TeamSystemTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition TWO = GeneratedCharacters.get("player_two");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();

    /** Player team (base on point, player_two as partner) against the single training dummy. */
    private static final class Match {
        final FighterState first = new FighterState(BASE, "P1");
        final FighterState second = new FighterState(TWO, "P2");
        final CombatEngine engine;
        final FighterInput[] in = {new FighterInput(), new FighterInput()};

        Match(float xa, float xb) {
            engine = new CombatEngine(first, new FighterState(NPC, "CPU"), xa, xb, new CombatConfig());
            engine.setTeam(0, first, second);
        }

        CombatFighter f(int i) { return engine.fighter(i); }
        TeamSystem.Side team() { return engine.team(0); }
        CombatConfig config() { return engine.config; }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
        }

        void steps(int n) { for (int i = 0; i < n; i++) step(); }

        void until(java.util.function.BooleanSupplier condition, int limit) {
            for (int i = 0; i < limit && !condition.getAsBoolean(); i++) step();
            assertTrue("condition not reached in " + limit + " frames", condition.getAsBoolean());
        }

        void assist() { in[0].assist = true; step(); }

        void rawTag() { in[0].tag = true; step(); }
    }

    @Test public void rawTagSwapsThePointAndCarriesTheTeamMeter() {
        Match m = new Match(500f, 1100f);
        m.first.superMeter = 2 * CombatConfig.METER_PER_BAR;
        m.rawTag();
        assertTrue(m.team().tagging());
        assertTrue("the point cannot act while it runs off", m.f(0).locked);
        m.steps(m.config().tagExitFrames);
        assertSame(m.second, m.f(0).state);
        assertEquals("the bars go with the team", 2 * CombatConfig.METER_PER_BAR, m.second.superMeter);
        assertEquals(0, m.first.superMeter);
        m.until(() -> !m.team().tagging(), m.config().tagEnterFrames + m.config().tagPoseFrames + 2);
        assertFalse(m.f(0).locked);
        assertTrue(m.team().tagCooldown > 0);
        m.rawTag();
        assertFalse("cooldown", m.team().tagging());
    }

    @Test public void assistComesInHitsTheOpponentAndLeaves() {
        Match m = new Match(500f, 600f);
        float pointX = m.f(0).x;
        int life = m.f(1).state.life;
        m.assist();
        assertNotNull(m.team().assist);
        assertSame(m.second, m.team().assist.state);
        assertSame("the point stays on point", m.first, m.f(0).state);
        m.until(() -> m.f(1).state.life < life, 60);
        assertNotNull(m.engine.session(1));
        assertEquals("the assist stands behind the point", pointX - m.config().assistBehind, m.team().assist.x, 0.5f);
        m.until(() -> m.team().assist == null, 120);
        assertEquals(m.config().assistCooldownFrames, m.team().assistCooldown, 1);
        m.assist();
        assertNull("cooldown", m.team().assist);
        m.steps(m.config().assistCooldownFrames);
        m.assist();
        assertNotNull(m.team().assist);
    }

    @Test public void assistMeterJoinsThePoint() {
        Match m = new Match(500f, 600f);
        int life = m.f(1).state.life;
        m.assist();
        m.until(() -> m.f(1).state.life < life, 60);
        m.step();
        assertEquals("a single team pool", 0, m.second.superMeter);
        assertTrue(m.first.superMeter > 0);
    }

    @Test public void projectileAssistThrowsThePartnersEnergy() {
        Match m = new Match(500f, 1000f);
        // player_two on point calls player_base, whose assist is its projectile ("S").
        m.engine.setTeam(0, m.second, m.first);
        m.assist();
        m.until(() -> !m.engine.energyProjectiles.isEmpty(), 60);
        assertEquals(0, m.engine.energyProjectiles.get(0).ownerIndex);
        int life = m.f(1).state.life;
        m.until(() -> m.f(1).state.life < life, 90);
    }

    @Test public void assistToTagLeavesTheAssistAsTheNewPoint() {
        Match m = new Match(500f, 600f);
        m.assist();
        float assistX = m.team().assistToX;
        m.steps(2);
        m.assist();
        assertTrue(m.team().convert);
        m.until(() -> m.team().assist == null, 120);
        assertSame("the assist took the point", m.second, m.f(0).state);
        assertEquals(assistX, m.f(0).x, 0.5f);
        assertSame(m.first, m.team().leaving);
        assertTrue(m.team().leavingFrame >= 0);
        assertEquals(CombatFighter.Status.NEUTRAL, m.f(0).status);
        assertTrue(m.team().assistCooldown > 0 && m.team().tagCooldown > 0);
        m.until(() -> m.team().leaving == null, m.config().assistLeaveFrames + 2);
    }

    @Test public void assistToTagIsRefusedWhileThePointIsBeingHit() {
        Match m = new Match(500f, 600f);
        m.assist();
        m.steps(2);
        m.assist();
        m.f(0).status = CombatFighter.Status.HITSTUN;
        m.f(0).stunLeft = m.f(0).stunTotal = 200;
        m.until(() -> m.team().assistPhase == TeamSystem.ASSIST_LEAVE, 120);
        assertSame("no tag: the assist just leaves", m.first, m.f(0).state);
    }

    @Test public void noAssistDuringRawTagOrUltraAndNoTeamForTheDummy() {
        Match m = new Match(500f, 1100f);
        m.rawTag();
        m.assist();
        assertNull(m.team().assist);

        Match solo = new Match(500f, 1100f);
        solo.in[1].assist = true;
        solo.in[1].tag = true;
        solo.step();
        assertNull(solo.engine.team(1).assist);
        assertFalse(solo.engine.team(1).tagging());
    }

    @Test public void teamPlayIsDeterministic() {
        long a = run(), b = run();
        assertEquals(a, b);
    }

    private static long run() {
        Match m = new Match(500f, 640f);
        long hash = 7;
        for (int frame = 0; frame < 400; frame++) {
            if (frame % 37 == 3) m.in[0].assist = true;
            if (frame % 90 == 50) m.in[0].light = true;
            if (frame == 250) m.in[0].tag = true;
            m.step();
            hash = hash * 31 + Float.floatToIntBits(m.f(0).x);
            hash = hash * 31 + m.f(1).state.life;
            hash = hash * 31 + m.team().point;
        }
        return hash;
    }
}
