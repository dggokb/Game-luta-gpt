package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/** Air dash (v0.82). */
public class AirDashTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();

    private static final class Sim {
        final CombatEngine engine = new CombatEngine(new FighterState(BASE, "P1"), new FighterState(NPC, "CPU"),
            500f, 1700f, new CombatConfig());
        final FighterInput[] in = {new FighterInput(), new FighterInput()};

        CombatFighter p() { return engine.fighter(0); }
        CombatConfig config() { return engine.config; }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
        }

        void steps(int n) { for (int i = 0; i < n; i++) step(); }

        void jumpAndRise() {
            in[0].jump = true;
            step();
            steps(8);
            assertTrue("high enough", Arena.GROUND_Y - p().y > config().airDashMinHeight);
        }

        void dash() { in[0].dash = true; step(); }

        void land() {
            for (int i = 0; i < 200 && !p().grounded; i++) step();
            assertTrue(p().grounded);
        }
    }

    @Test public void airDashBurstsForwardAndHoldsTheHeight() {
        Sim s = new Sim();
        s.jumpAndRise();
        float x = s.p().x;
        s.dash();
        assertTrue(s.p().airDashFrames > 0);
        float y = s.p().y;
        s.steps(s.config().airDashFrames - 2);
        assertEquals("holds the height", y, s.p().y, 0.01f);
        float expected = s.config().airDashSpeed * (s.config().airDashFrames - 1) * CombatConfig.DT;
        assertEquals(expected, s.p().x - x, 20f);
        s.steps(3);
        assertEquals(0, s.p().airDashFrames);
        s.step();
        assertTrue("then it falls", s.p().y > y);
    }

    @Test public void oneAirDashPerJumpGivenBackOnLanding() {
        Sim s = new Sim();
        s.jumpAndRise();
        s.dash();
        s.steps(s.config().airDashFrames + 1);
        s.dash();
        assertEquals("no second air dash", 0, s.p().airDashFrames);
        s.land();
        s.jumpAndRise();
        s.dash();
        assertTrue(s.p().airDashFrames > 0);
    }

    @Test public void backAirDashGoesBack() {
        Sim s = new Sim();
        s.jumpAndRise();
        float x = s.p().x;
        s.in[0].backdash = true;
        s.step();
        assertTrue(s.p().airDashBack);
        s.steps(s.config().backAirDashFrames);
        assertTrue("away from the opponent", s.p().x < x - 80f);
    }

    @Test public void noAirDashRightOffTheGroundAndItIsNotSpent() {
        Sim s = new Sim();
        s.in[0].jump = true;
        s.step();
        s.dash();
        assertEquals(0, s.p().airDashFrames);
        assertFalse(s.p().airDashUsed);
    }

    @Test public void airNormalOutOfTheDashKeepsTheMomentumAndGravityReturns() {
        Sim s = new Sim();
        s.jumpAndRise();
        s.dash();
        s.steps(2);
        float x = s.p().x, y = s.p().y;
        s.in[0].light = true;
        s.step();
        assertTrue(s.p().attacking());
        s.steps(3);
        assertTrue("momentum", s.p().x - x > 40f);
        assertTrue("gravity is back", s.p().y > y);
    }

    @Test public void aHitStopsTheDash() {
        Sim s = new Sim();
        s.jumpAndRise();
        s.dash();
        s.p().status = CombatFighter.Status.AIR_HITSTUN;
        s.p().stunLeft = s.p().stunTotal = 20;
        s.step();
        assertEquals(0, s.p().airDashFrames);
    }
}
