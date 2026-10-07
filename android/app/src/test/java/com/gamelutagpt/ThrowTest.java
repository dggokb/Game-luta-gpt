package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.Random;
import org.junit.Test;

/** Throw (L + M) and throw tech (v0.80). */
public class ThrowTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();

    private static final class Duel {
        final CombatEngine engine;
        final FighterInput[] in = {new FighterInput(), new FighterInput()};

        Duel(float gap) {
            CharacterDefinition.Body a = BASE.fighter.body, b = NPC.fighter.body;
            float xa = 600f;
            engine = new CombatEngine(new FighterState(BASE, "P1"), new FighterState(NPC, "CPU"),
                xa, xa + a.halfWidth + b.halfWidth + gap, new CombatConfig());
        }

        CombatFighter f(int i) { return engine.fighter(i); }
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

        void grab(int who) { in[who].grab = true; step(); }

        boolean thrown() { return f(1).status == CombatFighter.Status.THROWN; }
    }

    @Test public void throwUpCloseLandsDamageAndKnockdown() {
        Duel d = new Duel(10f);
        int life = d.f(1).state.life;
        d.grab(0);
        assertEquals(CombatFighter.Status.THROW, d.f(0).status);
        d.until(d::thrown, d.config().throwStartupFrames + 2);
        assertFalse("nothing hits a held fighter", d.f(1).hittable());
        d.until(() -> d.f(1).state.life < life, d.config().throwTechWindow + 2);
        assertEquals(d.config().throwDamage, life - d.f(1).state.life);
        d.until(() -> d.f(1).status == CombatFighter.Status.KNOCKDOWN, 30);
        d.until(() -> d.f(0).status == CombatFighter.Status.NEUTRAL, 60);
        assertEquals("the thrower acts while the defender is still down (oki)",
            CombatFighter.Status.KNOCKDOWN, d.f(1).status);
    }

    @Test public void techInsideTheWindowCancelsTheThrow() {
        Duel d = new Duel(10f);
        int life = d.f(1).state.life;
        float gap = d.f(1).x - d.f(0).x;
        d.grab(0);
        d.until(d::thrown, 10);
        d.steps(4);
        d.grab(1);
        assertEquals(CombatFighter.THROW_TECH, d.f(0).throwPhase);
        assertEquals(CombatFighter.THROW_TECH, d.f(1).throwPhase);
        d.until(() -> d.f(0).status == CombatFighter.Status.NEUTRAL, d.config().throwTechFrames + 2);
        assertEquals("no damage", life, d.f(1).state.life);
        assertTrue("pushed apart", d.f(1).x - d.f(0).x > gap);
    }

    @Test public void lateTechDoesNothing() {
        Duel d = new Duel(10f);
        int life = d.f(1).state.life;
        d.grab(0);
        d.until(d::thrown, 10);
        d.steps(d.config().throwTechWindow + 1);
        d.grab(1);
        assertTrue(d.f(1).state.life < life);
    }

    @Test public void throwWhiffsOutOfReachAndCanBePunished() {
        Duel d = new Duel(120f);
        d.grab(0);
        d.steps(d.config().throwStartupFrames + d.config().throwActiveFrames + 1);
        assertEquals(CombatFighter.THROW_WHIFF, d.f(0).throwPhase);
        assertTrue("a whiffed throw can be hit", d.f(0).hittable());
        d.until(() -> d.f(0).status == CombatFighter.Status.NEUTRAL, d.config().throwWhiffFrames + 2);
    }

    @Test public void noThrowInBlockstunOrRightAfterIt() {
        Duel d = new Duel(10f);
        d.f(1).status = CombatFighter.Status.BLOCKSTUN;
        d.f(1).stunLeft = d.f(1).stunTotal = 40;
        d.grab(0);
        d.steps(d.config().throwStartupFrames + d.config().throwActiveFrames + 1);
        assertFalse(d.thrown());
        assertEquals(CombatFighter.THROW_WHIFF, d.f(0).throwPhase);

        // A throw started during the stun reaches a defender who just left it: protected.
        Duel after = new Duel(10f);
        after.f(1).status = CombatFighter.Status.HITSTUN;
        after.f(1).stunLeft = after.f(1).stunTotal = 3;
        after.grab(0);
        after.until(() -> after.f(1).status == CombatFighter.Status.NEUTRAL, 6);
        assertTrue("throw protection after the stun", after.f(1).throwProtect > 0);
        after.steps(after.config().throwStartupFrames + after.config().throwActiveFrames);
        assertFalse(after.thrown());
        assertEquals(CombatFighter.THROW_WHIFF, after.f(0).throwPhase);
    }

    @Test public void throwsOnTheSameFrameTech() {
        Duel d = new Duel(10f);
        d.in[0].grab = true;
        d.in[1].grab = true;
        d.step();
        d.until(() -> d.f(0).throwPhase == CombatFighter.THROW_TECH, d.config().throwStartupFrames + 2);
        assertEquals(CombatFighter.THROW_TECH, d.f(1).throwPhase);
    }

    @Test public void padSendsTheThrowFromBothButtonsOrTheSpotBetween() {
        PadInput pad = new PadInput();
        FighterInput out = new FighterInput();
        pad.press(PadInput.Button.LIGHT);
        pad.press(PadInput.Button.MEDIUM);
        pad.drainInto(out);
        assertTrue(out.grab);
        assertFalse("the jab does not come out too", out.light || out.medium);
        pad.press(PadInput.Button.THROW);
        pad.drainInto(out);
        assertTrue(out.grab);
        assertEquals(ControlsLayout.Control.THROW,
            ControlsLayout.controlAt(ControlsLayout.THROW_X, ControlsLayout.THROW_Y));
        assertEquals(ControlsLayout.Control.LIGHT,
            ControlsLayout.controlAt(ControlsLayout.LIGHT_X, ControlsLayout.LIGHT_Y));
    }

    @Test public void twoFingersAFrameApartStillThrow() {
        Duel d = new Duel(10f);
        PadInput pad = new PadInput();
        pad.press(PadInput.Button.LIGHT);
        pad.drainInto(d.in[0]);
        d.step();
        assertTrue("the jab starts first", d.f(0).attacking());
        pad.press(PadInput.Button.MEDIUM);
        pad.drainInto(d.in[0]);
        assertTrue(d.in[0].grab);
        d.step();
        assertEquals("the early jab became the throw", CombatFighter.Status.THROW, d.f(0).status);
        d.until(d::thrown, d.config().throwStartupFrames + 2);

        PadInput slow = new PadInput();
        FighterInput out = new FighterInput();
        slow.press(PadInput.Button.LIGHT);
        slow.drainInto(out);
        for (int i = 0; i <= PadInput.THROW_LENIENCY_FRAMES; i++) slow.drainInto(out);
        slow.press(PadInput.Button.MEDIUM);
        slow.drainInto(out);
        assertFalse("too far apart: just M", out.grab);
        assertTrue(out.medium);
    }

    @Test public void cpuTechsSomeThrows() {
        int techs = 0, landed = 0;
        for (int seed = 0; seed < 40; seed++) {
            Duel d = new Duel(10f);
            AiController ai = new AiController(new OpponentAi(new Random(seed)));
            ai.setEnabled(true);
            d.in[0].grab = true;
            boolean done = false;
            for (int frame = 0; frame < 40 && !done; frame++) {
                ai.fill(d.engine, 1, d.in[1]);
                if (!d.thrown()) d.in[1].clear();
                d.step();
                for (CombatEngine.HitEvent e : d.engine.events()) {
                    if ("TECH".equals(e.moveId)) { techs++; done = true; }
                    if ("THROW".equals(e.moveId)) { landed++; done = true; }
                }
            }
        }
        assertTrue("the CPU techs sometimes: " + techs, techs > 5);
        assertTrue("and gets thrown sometimes: " + landed, landed > 5);
    }

    @Test public void cpuThrowsWhenRightNextToTheTarget() {
        OpponentAi ai = new OpponentAi(new Random(3));
        OpponentAi.Situation s = new OpponentAi.Situation();
        s.self = NPC;
        s.target = BASE.fighter.body;
        s.distance = 60f;
        s.throwReach = 100f;
        s.targetGrounded = true;
        s.targetAlive = true;
        s.attackReady = true;
        final int[] grabs = {0};
        OpponentAi.Actions actions = new OpponentAi.Actions() {
            @Override public void stop() {}
            @Override public void attack(String type) {}
            @Override public void superAttack() {}
            @Override public void energy(String strength) {}
            @Override public void backdash() {}
            @Override public void moveForward(boolean dash) {}
            @Override public void jump(boolean superJump) {}
            @Override public void grab() { grabs[0]++; }
        };
        for (int i = 0; i < 200; i++) ai.think(1f, s, actions);
        assertTrue(grabs[0] > 10);
    }
}
