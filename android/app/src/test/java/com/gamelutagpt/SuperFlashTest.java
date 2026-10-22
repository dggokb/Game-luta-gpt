package com.gamelutagpt;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * A cinemática do Super segue quem o ativou: a CPU também escurece a tela, aproxima a câmera e
 * avisa na ativação; quem fica parado no congelamento escurece e volta depois.
 */
public class SuperFlashTest {
    private static final CharacterDefinition P01 = GeneratedCharacters.get("player_base");

    private static final class Watch {
        int user = -1, starts, firstStart = -1, frozen = -1, maxDark, signalFrames;
        float maxZoom = 1f, maxDim;
        boolean endsClean;
    }

    /** O lutador {@code who} solta o Super; acompanha a apresentação por {@code frames} passos. */
    private static Watch superBy(int who, int frames) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(P01, P01, 500f, 900f);
        s.f(who).state.superMeter = CombatConfig.SUPER_COST;
        SuperFlash flash = new SuperFlash();
        Watch w = new Watch();
        for (int i = 0; i < frames; i++) {
            s.in[who].superAttack = i == 0;
            s.step();
            flash.update(s.engine);
            if (flash.started) {
                w.starts++;
                if (w.firstStart < 0) w.firstStart = i;
            }
            if (flash.active()) {
                w.user = flash.user;
                w.maxDark = Math.max(w.maxDark, flash.darkAlpha);
                w.maxZoom = Math.max(w.maxZoom, flash.zoom);
                if (flash.signal > 0f) w.signalFrames++;
            }
            if (flash.frozen >= 0) w.frozen = flash.frozen;
            w.maxDim = Math.max(w.maxDim, flash.frozenDim);
        }
        w.endsClean = !flash.active() && flash.frozen < 0 && flash.frozenDim == 0f && flash.zoom == 1f;
        return w;
    }

    @Test public void theCpuSuperPlaysTheSameCinematic() {
        Watch w = superBy(1, 240);
        assertEquals("the CPU is who activated", 1, w.user);
        assertEquals("one activation", 1, w.starts);
        assertTrue("the stage darkens: " + w.maxDark, w.maxDark >= SuperFlash.DARK);
        assertTrue("the camera closes in: " + w.maxZoom, w.maxZoom > 1.3f);
        assertEquals("the player is held and dims", 0, w.frozen);
        assertTrue("dims for real: " + w.maxDim, w.maxDim >= 1f);
        assertTrue("back to normal after the Super", w.endsClean);
    }

    @Test public void thePlayerSuperHoldsTheCpu() {
        Watch w = superBy(0, 240);
        assertEquals(0, w.user);
        assertEquals(1, w.frozen);
        assertTrue(w.endsClean);
    }

    @Test public void theActivationSignalComesFirstAndIsShort() {
        Watch w = superBy(1, 240);
        assertTrue("the warning starts with the Super: " + w.firstStart, w.firstStart >= 0 && w.firstStart <= 2);
        assertTrue("the body glows for a few frames: " + w.signalFrames,
            w.signalFrames > 0 && w.signalFrames <= SuperFlash.SIGNAL_FRAMES);
    }

    /**
     * Entrada do Super (o retrato ao fundo): quem ativou segura o primeiro quadro do Super
     * durante a entrada, o rival espera ela inteira mais o congelamento de sempre, e o golpe sai
     * depois, no mesmo tempo de um Super sem entrada.
     */
    @Test public void theSuperIntroHoldsTheSuperUntilThePortraitLeaves() {
        final int intro = 80;
        int[] releaseAt = new int[2];
        int maxHeld = 0, introFrames = 0;
        boolean leftSide = false;
        for (int run = 0; run < 2; run++) {
            CombatEngineTest.Sim s = new CombatEngineTest.Sim(P01, P01, 500f, 900f);
            if (run == 1) s.engine.setSuperIntro(P01.id, intro);
            s.f(0).state.superMeter = CombatConfig.SUPER_COST;
            SuperFlash flash = new SuperFlash();
            releaseAt[run] = -1;
            for (int i = 0; i < 240 && releaseAt[run] < 0; i++) {
                s.in[0].superAttack = i == 0;
                s.step();
                flash.update(s.engine);
                CombatFighter f = s.f(0);
                if (run == 1 && flash.introShowing()) {
                    introFrames++;
                    leftSide = flash.introLeft;
                    // O golpe ainda não andou (o desenho fica no 1º quadro do Super).
                    assertTrue("held before the first frame during the portrait: " + f.attackFrame,
                        f.attacking() && f.attackFrame <= 0);
                    maxHeld = Math.max(maxHeld, s.f(1).hitstop);
                }
                if (f.attacking() && f.attackFrame >= f.attack.startupFrames) releaseAt[run] = i;
            }
        }
        assertEquals("the portrait shows for the whole intro", intro, introFrames);
        assertTrue("the portrait sits on the left edge (she is on the left)", leftSide);
        assertTrue("the Super comes out after the intro", releaseAt[0] > 0 && releaseAt[1] > 0);
        assertEquals("then it comes out on its usual time", releaseAt[0] + intro, releaseAt[1]);
        assertTrue("the opponent waits for the intro and the release: " + maxHeld, maxHeld > intro);
    }

    @Test public void nothingWithoutASuper() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(P01, P01, 500f, 900f);
        SuperFlash flash = new SuperFlash();
        for (int i = 0; i < 60; i++) {
            s.in[0].heavy = i == 0;
            s.step();
            flash.update(s.engine);
            assertFalse(flash.active());
            assertFalse(flash.started);
            assertEquals(1f, flash.zoom, 0f);
        }
    }

    @Test public void aFullBarSpendBlinksOnTheMeter() {
        FighterState f = new FighterState(P01, "P1");
        HudEffects e = new HudEffects();
        HudEffects.Meter m = new HudEffects.Meter();
        m.seed(f);
        f.superMeter = 2 * CombatConfig.METER_PER_BAR;
        m.update(f, 1);
        f.superMeter -= CombatConfig.METER_PER_BAR / 4;   // pushblock: só o flash curto
        m.update(f, 2);
        assertEquals(-100, m.superAt, 0);
        f.superMeter -= CombatConfig.SUPER_COST;
        m.update(f, 3);
        assertEquals(3, m.superAt, 0);
        e.now = 3.01;
        assertTrue(e.superBlink(m) > 0.9f);
        e.now = 3.1;
        assertTrue("blinks", e.superBlink(m) < 0.5f);
        e.now = 3 + HudEffects.SUPER_FLASH;
        assertEquals(0f, e.superBlink(m), 0f);
    }
}
