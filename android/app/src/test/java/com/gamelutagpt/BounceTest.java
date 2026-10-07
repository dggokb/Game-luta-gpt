package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/** Wall bounce and ground bounce (v0.83). */
public class BounceTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();

    private static final class Sim {
        final CombatEngine engine;
        final FighterInput[] in = {new FighterInput(), new FighterInput()};

        Sim(float xa, float xb) {
            this(xa, xb, new CombatConfig());
        }

        Sim(float xa, float xb, CombatConfig config) {
            engine = new CombatEngine(new FighterState(BASE, "P1"), new FighterState(NPC, "CPU"), xa, xb, config);
        }

        CombatFighter f(int i) { return engine.fighter(i); }
        CombatConfig config() { return engine.config; }
        String attackId(int i) { return f(i).attacking() ? f(i).attack.id : ""; }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
        }

        boolean cue(String id) { return engine.cues().contains(id); }

        /** L > M > H chain up close; returns once H has hit. */
        void chainToHeavy() {
            in[0].light = true;
            for (int i = 0; i < 120; i++) {
                if (attackId(0).equals("L")) in[0].medium = true;
                if (attackId(0).equals("M")) in[0].heavy = true;
                step();
                for (CombatEngine.HitEvent e : engine.events()) if ("H".equals(e.moveId)) return;
            }
            fail("H never hit");
        }

        /** Both in the air, the defender in air hitstun right in front; the attacker swings jH. */
        void airHeavy() {
            for (int i = 0; i < 2; i++) {
                f(i).grounded = false;
                f(i).y = Arena.GROUND_Y - 160f;
                f(i).vy = 0f;
            }
            f(1).status = CombatFighter.Status.AIR_HITSTUN;
            f(1).stunLeft = f(1).stunTotal = 60;
            in[0].heavy = true;
            step();
            assertEquals("jH", attackId(0));
        }
    }

    @Test public void heavyInAComboBouncesOffTheWallAndCanBeFollowedUp() {
        Sim s = new Sim(500f, 600f);
        s.chainToHeavy();
        CombatFighter d = s.f(1);
        assertEquals(CombatFighter.Status.AIR_HITSTUN, d.status);
        assertEquals(CombatFighter.BOUNCE_WALL, d.bounce);
        assertEquals(1, s.engine.session(1).wallBounces);
        float wall = d.bounceWallX;
        assertEquals("the screen-edge wall", s.f(0).x + s.config().wallBounceDistance, wall, 40f);
        boolean bounced = false;
        for (int i = 0; i < 80 && !bounced; i++) {
            s.step();
            bounced = s.cue("WALL_BOUNCE");
        }
        assertTrue(bounced);
        assertEquals(wall, d.x, 0.5f);
        assertTrue("pops up", d.vy < 0f);
        assertTrue("comes back toward the attacker", d.vx < 0f);
        assertTrue("still in the combo", s.engine.session(1) != null && d.inHitstun());
        for (int i = 0; i < 200 && d.status != CombatFighter.Status.KNOCKDOWN; i++) s.step();
        assertEquals("no air recovery after a bounce: it lands knocked down", CombatFighter.Status.KNOCKDOWN, d.status);
    }

    @Test public void aLoneHeavyInNeutralIsAPlainHit() {
        Sim s = new Sim(500f, 600f);
        s.in[0].heavy = true;
        for (int i = 0; i < 40 && s.f(1).framesSinceHit > 0; i++) s.step();
        assertEquals(CombatFighter.BOUNCE_NONE, s.f(1).bounce);
        assertEquals(CombatFighter.Status.HITSTUN, s.f(1).status);
    }

    @Test public void oneWallBouncePerCombo() {
        Sim s = new Sim(500f, 600f);
        s.in[0].light = true;
        for (int i = 0; i < 120; i++) {
            if (s.engine.session(1) != null) s.engine.session(1).wallBounces = s.config().maxWallBounces;
            if (s.attackId(0).equals("L")) s.in[0].medium = true;
            if (s.attackId(0).equals("M")) s.in[0].heavy = true;
            s.step();
            boolean heavy = false;
            for (CombatEngine.HitEvent e : s.engine.events()) heavy |= "H".equals(e.moveId);
            if (heavy) break;
        }
        assertEquals(CombatFighter.BOUNCE_NONE, s.f(1).bounce);
        assertEquals(0f, s.f(1).vx, 0f);
    }

    @Test public void airHeavyDrivesTheDefenderIntoTheFloorAndItBouncesUp() {
        Sim s = new Sim(600f, 690f);
        s.airHeavy();
        CombatFighter d = s.f(1);
        for (int i = 0; i < 30 && d.bounce != CombatFighter.BOUNCE_GROUND; i++) s.step();
        assertEquals(CombatFighter.BOUNCE_GROUND, d.bounce);
        assertTrue("driven down", d.vy > 0f);
        boolean bounced = false;
        for (int i = 0; i < 60 && !bounced; i++) {
            s.step();
            bounced = s.cue("GROUND_BOUNCE");
        }
        assertTrue(bounced);
        assertFalse("back in the air", d.grounded);
        assertTrue(d.vy < 0f);
        for (int i = 0; i < 200 && d.status != CombatFighter.Status.KNOCKDOWN; i++) s.step();
        assertEquals(CombatFighter.Status.KNOCKDOWN, d.status);
    }

    @Test public void groundBounceSpentFromASuperJumpEndsLikeTheSlam() {
        CombatConfig config = new CombatConfig();
        config.maxGroundBounces = 0;            // as if this combo already used it
        Sim s = new Sim(600f, 690f, config);
        s.f(0).superJumping = true;
        s.airHeavy();
        CombatFighter d = s.f(1);
        for (int i = 0; i < 30 && d.framesSinceHit > 0; i++) s.step();
        assertEquals(CombatFighter.BOUNCE_NONE, d.bounce);
        assertTrue("slammed", d.slammed);
    }

    @Test public void spentGroundBounceWithoutSuperJumpIsAPlainAirHit() {
        CombatConfig config = new CombatConfig();
        config.maxGroundBounces = 0;
        Sim s = new Sim(600f, 690f, config);
        s.airHeavy();
        CombatFighter d = s.f(1);
        for (int i = 0; i < 30 && d.framesSinceHit > 0; i++) s.step();
        assertEquals(CombatFighter.BOUNCE_NONE, d.bounce);
        assertFalse(d.slammed);
        assertEquals(CombatFighter.Status.AIR_HITSTUN, d.status);
    }
}
