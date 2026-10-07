package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/** Pushblock and Guard Cancel Tag (v0.81). */
public class ActiveDefenseTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition TWO = GeneratedCharacters.get("player_two");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();

    private static final class Match {
        final FighterState first = new FighterState(BASE, "P1");
        final FighterState second = new FighterState(TWO, "P2");
        final CombatEngine engine = new CombatEngine(first, new FighterState(NPC, "CPU"), 600f, 720f, new CombatConfig());
        final FighterInput[] in = {new FighterInput(), new FighterInput()};

        Match(boolean team) {
            if (team) engine.setTeam(0, first, second);
        }

        CombatFighter f(int i) { return engine.fighter(i); }
        CombatConfig config() { return engine.config; }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
        }

        void steps(int n) { for (int i = 0; i < n; i++) step(); }

        void blocking(int frames) {
            CombatFighter p = f(0);
            p.status = CombatFighter.Status.BLOCKSTUN;
            p.stunLeft = p.stunTotal = frames;
            p.lastGuard = CombatFighter.GUARD_HIGH;
        }

        boolean event(String id) {
            for (CombatEngine.HitEvent e : engine.events()) if (id.equals(e.moveId)) return true;
            return false;
        }
    }

    @Test public void pushblockShovesTheAttackerAwayForAQuarterBar() {
        Match m = new Match(false);
        m.first.superMeter = CombatConfig.METER_PER_BAR;
        m.blocking(30);
        float gap = m.f(1).x - m.f(0).x;
        m.in[0].pushblock = true;
        m.step();
        assertTrue(m.event("PUSHBLOCK"));
        assertEquals(CombatConfig.METER_PER_BAR - m.config().pushblockCost, m.first.superMeter);
        assertTrue("blockstun ends sooner", m.f(0).stunLeft <= m.config().pushblockStunFrames);
        m.steps(m.config().pushbackFrames + 1);
        assertTrue("the attacker was pushed away", m.f(1).x - m.f(0).x > gap + 100f);
    }

    @Test public void pushblockNeedsMeter() {
        Match m = new Match(false);
        m.blocking(30);
        m.in[0].pushblock = true;
        m.step();
        assertFalse(m.event("PUSHBLOCK"));
        assertEquals(CombatFighter.Status.BLOCKSTUN, m.f(0).status);
    }

    @Test public void mPlusHOutsideBlockstunIsTheHeavy() {
        Match m = new Match(false);
        m.in[0].pushblock = true;
        m.step();
        assertTrue(m.f(0).attacking());
        assertEquals("H", m.f(0).attack.id);
    }

    @Test public void guardCancelBringsThePartnerInInvulnerableForABar() {
        Match m = new Match(true);
        m.first.superMeter = CombatConfig.METER_PER_BAR + 200;
        m.blocking(30);
        m.in[0].assist = true;
        m.step();
        assertTrue(m.event("GUARD_CANCEL"));
        assertSame("the partner took the point", m.second, m.f(0).state);
        assertTrue("it comes in attacking", m.f(0).attacking());
        assertFalse("invulnerable while it comes in", m.f(0).hittable());
        assertTrue("cross-counter flash", m.f(1).frozen());
        assertEquals("the bars stay with the team, minus one", 200, m.second.superMeter);
        assertSame(m.first, m.engine.team(0).leaving);
        m.steps(m.f(0).attack.totalFrames + m.config().guardCancelInvulnPadding + 2);
        assertTrue(m.f(0).hittable());
    }

    @Test public void guardCancelNeedsABarAPartnerAndTheGround() {
        Match poor = new Match(true);
        poor.first.superMeter = CombatConfig.METER_PER_BAR - 1;
        poor.blocking(30);
        poor.in[0].assist = true;
        poor.step();
        assertSame(poor.first, poor.f(0).state);

        Match solo = new Match(false);
        solo.first.superMeter = CombatConfig.METER_PER_BAR;
        solo.blocking(30);
        solo.in[0].assist = true;
        solo.step();
        assertFalse(solo.event("GUARD_CANCEL"));

        Match air = new Match(true);
        air.first.superMeter = CombatConfig.METER_PER_BAR;
        air.blocking(30);
        air.f(0).grounded = false;
        air.f(0).y = Arena.GROUND_Y - 100f;
        air.in[0].assist = true;
        air.step();
        assertSame(air.first, air.f(0).state);
    }

    @Test public void tagWhileBlockingNeverCallsAPlainAssist() {
        Match m = new Match(true);
        m.blocking(30);
        m.in[0].assist = true;
        m.step();
        assertNull(m.engine.team(0).assist);
    }

    @Test public void padSendsThePushblockFromBothButtonsOrTheSpot() {
        PadInput pad = new PadInput();
        FighterInput out = new FighterInput();
        pad.press(PadInput.Button.MEDIUM);
        pad.press(PadInput.Button.HEAVY);
        pad.drainInto(out);
        assertTrue(out.pushblock);
        assertFalse(out.medium || out.heavy);
        pad.press(PadInput.Button.PUSHBLOCK);
        pad.drainInto(out);
        assertTrue(out.pushblock);
        assertEquals(ControlsLayout.Control.PUSHBLOCK,
            ControlsLayout.controlAt(ControlsLayout.PUSHBLOCK_X, ControlsLayout.PUSHBLOCK_Y));
    }
}
