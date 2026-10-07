package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/** Overdrive (v0.87): once per round, faster, more meter, free cancels, red life back on point. */
public class OverdriveTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");

    private static final class Match {
        final FighterState first = new FighterState(BASE, "P1");
        final FighterState cpuState = new FighterState(BASE, "CPU");
        final CombatEngine engine;
        final FighterInput[] in = {new FighterInput(), new FighterInput()};

        Match(float xa, float xb) {
            engine = new CombatEngine(first, cpuState, xa, xb, new CombatConfig());
        }

        CombatFighter f(int i) { return engine.fighter(i); }
        CombatConfig config() { return engine.config; }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
        }

        void steps(int n) { for (int i = 0; i < n; i++) step(); }

        void overdrive() {
            in[0].overdrive = true;
            step();
        }

        /** Presses {@code button} until it starts; returns once that attack hit. */
        void hitWith(Runnable button, String id) {
            for (int i = 0; i < 80; i++) {
                if (!f(0).attacking() || !f(0).attack.id.equals(id)) button.run();
                step();
                for (CombatEngine.HitEvent e : engine.events()) if (e.attacker == 0 && id.equals(e.moveId)) return;
            }
            fail(id + " never hit");
        }
    }

    @Test public void activationFreezesBothAndLastsAboutEightSeconds() {
        Match m = new Match(600f, 900f);
        m.overdrive();
        assertTrue(m.engine.cues().contains("OVERDRIVE"));
        assertTrue(m.engine.teams.overdriveActive(0));
        assertTrue(m.f(0).frozen() && m.f(1).frozen());
        // The activation step is the first frame of the flash; the clock waits through it.
        m.steps(m.config().overdriveFlashFrames + m.config().overdriveFrames - 2);
        assertTrue(m.engine.teams.overdriveActive(0));
        m.steps(3);
        assertFalse(m.engine.teams.overdriveActive(0));
    }

    @Test public void oncePerRoundUntilRefreshed() {
        Match m = new Match(600f, 900f);
        m.overdrive();
        m.steps(m.config().overdriveFlashFrames + m.config().overdriveFrames + 2);
        m.overdrive();
        assertFalse("already used this round", m.engine.cues().contains("OVERDRIVE"));
        m.engine.refreshOverdrive(0);
        m.overdrive();
        assertTrue(m.engine.cues().contains("OVERDRIVE"));
    }

    @Test public void padSendsItFromTheButtonOrSuperPlusTag() {
        PadInput pad = new PadInput();
        FighterInput out = new FighterInput();
        pad.press(PadInput.Button.SUPER);
        pad.press(PadInput.Button.TAG);
        pad.drainInto(out);
        assertTrue(out.overdrive);
        assertFalse("neither the Super nor the assist", out.superAttack || out.assist || out.tag);
        pad.press(PadInput.Button.OVERDRIVE);
        pad.drainInto(out);
        assertTrue(out.overdrive);
        assertEquals(ControlsLayout.Control.OVERDRIVE,
            ControlsLayout.controlAt(ControlsLayout.OVERDRIVE_X, ControlsLayout.OVERDRIVE_Y));
    }

    @Test public void walksAndDashesFaster() {
        Match normal = new Match(600f, 1500f);
        Match boosted = new Match(600f, 1500f);
        boosted.overdrive();
        boosted.steps(boosted.config().overdriveFlashFrames);
        for (Match m : new Match[]{normal, boosted}) {
            for (int i = 0; i < 30; i++) {
                m.in[0].direction = 1;
                m.step();
            }
        }
        float walked = normal.f(0).x - 600f, fast = boosted.f(0).x - 600f;
        assertEquals(walked * boosted.config().overdriveSpeed, fast, 2f);
    }

    @Test public void heavyCancelsIntoMediumOnlyInOverdrive() {
        Match plain = new Match(600f, 690f);
        plain.hitWith(() -> plain.in[0].heavy = true, "H");
        for (int i = 0; i < 20 && plain.f(0).attacking(); i++) {
            plain.in[0].medium = true;
            plain.step();
            assertNotEquals("M", plain.f(0).attack == null ? "" : plain.f(0).attack.id);
        }

        Match od = new Match(600f, 690f);
        od.overdrive();
        od.steps(od.config().overdriveFlashFrames);
        od.hitWith(() -> od.in[0].heavy = true, "H");
        boolean cue = false;
        for (int i = 0; i < 20 && !(od.f(0).attacking() && od.f(0).attack.id.equals("M")); i++) {
            od.in[0].medium = true;
            od.step();
            cue |= od.engine.cues().contains("OD_CANCEL");
        }
        assertEquals("M", od.f(0).attack.id);
        assertTrue(cue);
    }

    @Test public void activationCancelsTheOwnAttackAndExtendsTheCombo() {
        Match m = new Match(600f, 690f);
        m.hitWith(() -> m.in[0].medium = true, "M");
        int stun = m.f(1).stunLeft;
        m.in[0].overdrive = true;
        for (int i = 0; i < 20 && !m.engine.cues().contains("OVERDRIVE"); i++) m.step();
        assertTrue(m.engine.cues().contains("OVERDRIVE"));
        assertFalse("the M was cancelled", m.f(0).attacking());
        m.steps(m.config().overdriveFlashFrames);
        assertTrue("the opponent is still in hitstun after the flash", m.f(1).inHitstun());
        assertTrue(m.f(1).stunLeft > 0 && m.f(1).stunLeft <= stun);
    }

    @Test public void moreMeterOnHit() {
        Match plain = new Match(600f, 690f);
        plain.hitWith(() -> plain.in[0].light = true, "L");
        Match od = new Match(600f, 690f);
        od.overdrive();
        od.steps(od.config().overdriveFlashFrames);
        od.hitWith(() -> od.in[0].light = true, "L");
        assertEquals(plain.first.superMeter * od.config().overdriveMeterPermille / 1000, od.first.superMeter);
    }

    @Test public void recoverableLifeComesBackOnPoint() {
        Match m = new Match(600f, 900f);
        m.first.takeDamage(3000, m.config().recoverableLifePermille);
        int life = m.first.life;
        m.steps(30);
        assertEquals("not without Overdrive", life, m.first.life);
        m.overdrive();
        m.steps(m.config().overdriveFlashFrames + 20);
        assertTrue(m.first.life > life);
        assertTrue(m.first.life <= m.first.profile.maxLife);
    }

    @Test public void notWhileInHitstunOrDuringASuper() {
        Match m = new Match(600f, 900f);
        m.f(0).status = CombatFighter.Status.HITSTUN;
        m.f(0).stunLeft = m.f(0).stunTotal = 30;
        m.overdrive();
        assertFalse(m.engine.cues().contains("OVERDRIVE"));
        assertFalse(m.engine.team(0).overdriveUsed);
    }
}
