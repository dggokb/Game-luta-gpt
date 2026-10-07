package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.Random;
import org.junit.Test;

/** Air tech and wake-up options (v0.84). */
public class RecoveryTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();

    private static final class Sim {
        final CombatEngine engine = new CombatEngine(new FighterState(BASE, "P1"), new FighterState(NPC, "CPU"),
            800f, 1100f, new CombatConfig());
        final FighterInput[] in = {new FighterInput(), new FighterInput()};

        CombatFighter p() { return engine.fighter(0); }
        CombatConfig config() { return engine.config; }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
        }

        void steps(int n) { for (int i = 0; i < n; i++) step(); }

        /** The player high in the air, its air hitstun about to end. */
        void airborneHitstun(int stunLeft) {
            CombatFighter f = p();
            f.grounded = false;
            f.y = Arena.GROUND_Y - 260f;
            f.vy = -200f;
            f.status = CombatFighter.Status.AIR_HITSTUN;
            f.stunLeft = f.stunTotal = stunLeft;
        }

        /** The player just knocked down (lying from the first frame). */
        void knockedDown() {
            CombatFighter f = p();
            f.status = CombatFighter.Status.KNOCKDOWN;
            f.knockdownFrame = 0;
        }

        /** Frames until the player can act again. */
        int framesUntilFree(int limit) {
            for (int i = 0; i < limit; i++) {
                if (p().status == CombatFighter.Status.NEUTRAL) return i;
                step();
            }
            fail("never free");
            return -1;
        }
    }

    @Test public void airTechBackEscapesInvulnerable() {
        Sim s = new Sim();
        s.airborneHitstun(2);
        float x = s.p().x;
        s.in[0].direction = 5;              // ← (facing right): back
        s.in[0].light = true;
        s.step();
        s.steps(2);
        assertEquals(CombatFighter.Status.NEUTRAL, s.p().status);
        assertTrue(s.engine.cues().contains("AIR_TECH") || s.p().invulnFrames > 0);
        assertFalse("invulnerable for a moment", s.p().hittable());
        s.in[0].direction = 5;
        s.steps(s.config().pushbackFrames);
        assertTrue("moved away", s.p().x < x - 100f);
        assertFalse("the tech button did not come out as an air normal", s.p().attacking());
    }

    @Test public void neutralAirTechPopsUp() {
        Sim s = new Sim();
        s.airborneHitstun(1);
        s.in[0].light = true;
        s.steps(2);
        assertEquals(CombatFighter.Status.NEUTRAL, s.p().status);
        assertTrue(s.p().vy < 0f);
    }

    @Test public void withoutATechItFreeFallsThenRecoversLater() {
        Sim s = new Sim();
        s.airborneHitstun(1);
        s.steps(3);
        assertEquals("free fall: still open", CombatFighter.Status.AIR_HITSTUN, s.p().status);
        assertTrue(s.p().hittable());
        int free = s.framesUntilFree(40);
        assertTrue(free > 0 && free <= s.config().airTechWindow);
        assertEquals(0, s.p().invulnFrames);
    }

    @Test public void quickRiseIsFasterAndDelayIsSlower() {
        Sim normal = new Sim();
        normal.knockedDown();
        int normalFrames = normal.framesUntilFree(300);

        Sim quick = new Sim();
        quick.knockedDown();
        quick.in[0].direction = 7;          // ↑
        int quickFrames = quick.framesUntilFree(300);
        assertTrue(quickFrames + 40 < normalFrames);

        Sim late = new Sim();
        late.knockedDown();
        late.in[0].direction = 3;           // ↓
        int lateFrames = late.framesUntilFree(300);
        assertEquals(normalFrames + late.config().delayWakeupFrames, lateFrames);
    }

    @Test public void rollsMoveInvulnerableAndPassThroughTheOpponent() {
        Sim back = new Sim();
        back.knockedDown();
        float x = back.p().x;
        back.in[0].direction = 5;
        back.steps(back.config().knockdownFallFrames + 2);
        assertTrue(back.p().rolling());
        assertFalse(back.p().hittable());
        back.framesUntilFree(60);
        assertTrue("rolled away from the opponent", back.p().x < x - 150f);

        Sim forward = new Sim();
        forward.p().x = 1000f;              // right next to the opponent at 1100
        forward.knockedDown();
        forward.in[0].direction = 1;
        forward.framesUntilFree(200);
        assertTrue("rolled through to the other side", forward.p().x > 1100f);
    }

    @Test public void ultraKnockdownHasNoOptions() {
        Sim s = new Sim();
        s.p().ultraFall = true;
        s.p().grounded = false;
        s.p().y = Arena.GROUND_Y - 5f;
        s.p().vy = 300f;
        s.p().status = CombatFighter.Status.AIR_HITSTUN;
        s.p().stunLeft = 50;
        s.in[0].direction = 7;
        for (int i = 0; i < 10 && s.p().status != CombatFighter.Status.KNOCKDOWN; i++) s.step();
        assertTrue(s.p().hardKnockdown);
        s.steps(s.config().knockdownFallFrames + s.config().quickRiseDownFrames + 2);
        assertEquals("no quick rise after the ultra", CombatFighter.Status.KNOCKDOWN, s.p().status);
    }

    @Test public void cpuUsesTheRecoveryOptions() {
        int airTechs = 0, rolls = 0, quick = 0;
        // One generator for every trial, as in a match (fresh seeds 0, 1, 2... start almost alike).
        OpponentAi brain = new OpponentAi(new Random(42));
        for (int trial = 0; trial < 30; trial++) {
            Sim s = new Sim();
            AiController ai = new AiController(brain);
            ai.setEnabled(true);
            CombatFighter cpu = s.engine.fighter(1);
            cpu.grounded = false;
            cpu.y = Arena.GROUND_Y - 260f;
            cpu.status = CombatFighter.Status.AIR_HITSTUN;
            cpu.stunLeft = cpu.stunTotal = 2;
            for (int i = 0; i < 8; i++) {
                ai.fill(s.engine, 1, s.in[1]);
                s.step();
                if (s.engine.cues().contains("AIR_TECH")) airTechs++;
            }
            Sim k = new Sim();
            AiController ai2 = new AiController(brain);
            ai2.setEnabled(true);
            CombatFighter down = k.engine.fighter(1);
            down.status = CombatFighter.Status.KNOCKDOWN;
            for (int i = 0; i < 30; i++) {
                ai2.fill(k.engine, 1, k.in[1]);
                k.step();
            }
            if (down.wakeup == CombatFighter.WAKE_BACK_ROLL || down.wakeup == CombatFighter.WAKE_FORWARD_ROLL) rolls++;
            if (down.wakeup == CombatFighter.WAKE_QUICK) quick++;
        }
        assertTrue("air techs: " + airTechs, airTechs > 10);
        assertTrue("rolls: " + rolls, rolls > 3);
        assertTrue("quick rises: " + quick, quick > 3);
    }
}
