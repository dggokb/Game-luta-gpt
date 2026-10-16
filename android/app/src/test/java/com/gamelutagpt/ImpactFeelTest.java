package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/** Peso dos golpes na tela: soco de câmera, escurecida, clarão e câmera lenta do KO. */
public class ImpactFeelTest {
    @Test public void heavierBlowsPunchTheCameraHarderAndItSettles() {
        ImpactFeel light = new ImpactFeel(), heavy = new ImpactFeel();
        light.onHit(500f, 300f, ImpactEffects.LIGHT, false, false);
        heavy.onHit(500f, 300f, ImpactEffects.HEAVY, false, false);
        assertEquals("a jab does not move the camera", 1f, light.zoom(), 0f);
        assertTrue(heavy.zoom() > 1.02f);
        assertEquals(500f, heavy.focusX(), 0f);
        for (int i = 0; i < 60; i++) heavy.step();
        assertEquals("back to the normal framing", 1f, heavy.zoom(), 0.001f);
    }

    @Test public void onlyStrongBlowsDarkenTheStageAndOnlyForAMoment() {
        ImpactFeel f = new ImpactFeel();
        f.onHit(0f, 0f, ImpactEffects.MEDIUM, false, false);
        assertEquals(0, f.darkAlpha());
        f.onHit(0f, 0f, ImpactEffects.HEAVY, false, false);
        assertTrue(f.darkAlpha() > 0);
        for (int i = 0; i < ImpactFeel.DARK_FRAMES[ImpactEffects.HEAVY]; i++) f.step();
        assertEquals(0, f.darkAlpha());
    }

    @Test public void knockoutFlashesAndSlowsTheFightThenRecovers() {
        ImpactFeel f = new ImpactFeel();
        f.onHit(0f, 0f, ImpactEffects.HEAVY, false, true);
        assertTrue(f.flashAlpha() > 150);
        assertTrue(f.timeScale() < 1f);
        for (int i = 0; i < ImpactFeel.KO_SLOW_FRAMES; i++) f.step();
        assertEquals(1f, f.timeScale(), 0f);
        assertEquals(0, f.flashAlpha());
        f.onHit(0f, 0f, ImpactEffects.HEAVY, false, true);
        f.clear();
        assertEquals("a new round starts clean", 1f, f.timeScale(), 0f);
    }

    @Test public void theHitFighterShakesOnlyDuringItsHitstop() {
        CombatFighter d = new CombatEngine(new FighterState(GeneratedCharacters.get("player_base"), "P1"),
            new FighterState(GeneratedCharacters.opponentCharacter(), "P2"), 500f, 640f, new CombatConfig()).fighter(1);
        assertEquals(0f, ImpactFeel.hitShake(d, 0), 0f);
        d.hitstop = 9;
        d.framesSinceHit = 0;
        float a = ImpactFeel.hitShake(d, 0), b = ImpactFeel.hitShake(d, 1);
        assertTrue(Math.abs(a) > 4f);
        assertEquals("alternates sides", -a, b, 0f);
        d.framesSinceHit = 999;
        d.framesSinceBlock = 0;
        assertTrue("a guard shakes less", Math.abs(ImpactFeel.hitShake(d, 0)) < Math.abs(a));
    }

    @Test public void impactFrameShowsTheSilhouetteThenOneNegativeFrame() {
        ImpactFeel f = new ImpactFeel();
        f.impactFrame(ImpactFeel.RED, 600f, 300f);
        f.step();                           // the fight step that asked for it
        for (int i = 0; i < ImpactFeel.STYLE_FRAMES; i++) {
            assertEquals(ImpactFeel.RED, f.impactStyle());
            assertFalse(f.negative());
            f.step();
        }
        assertEquals(ImpactFeel.NONE, f.impactStyle());
        assertTrue("one inverted frame", f.negative());
        f.step();
        assertFalse(f.negative());
        assertEquals(600f, f.impactX(), 0f);
    }

    @Test public void impactFramesDoNotFlashOneAfterAnother() {
        ImpactFeel f = new ImpactFeel();
        f.impactFrame(ImpactFeel.RED, 0f, 0f);
        for (int i = 0; i < 5; i++) f.step();
        f.impactFrame(ImpactFeel.RED, 0f, 0f);
        f.step();
        assertEquals("a second red one waits for the cooldown", ImpactFeel.NONE, f.impactStyle());
        f.impactFrame(ImpactFeel.VOID, 0f, 0f);
        f.step();
        assertEquals("a knockout still gets its own", ImpactFeel.VOID, f.impactStyle());
        for (int i = 0; i < ImpactFeel.IMPACT_COOLDOWN; i++) f.step();
        f.impactFrame(ImpactFeel.RED, 0f, 0f);
        f.step();
        assertEquals(ImpactFeel.RED, f.impactStyle());
    }
}
