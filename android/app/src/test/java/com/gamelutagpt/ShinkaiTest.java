package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * SHINKAI (substitui o Overdrive): a barra de cada lutador enche, cheia ativa uma vez por round
 * por 8 s, e o tipo dele (POWER, SPEED, ENERGY, TECHNIQUE) muda o que ele ganha.
 */
public class ShinkaiTest {
    private static final class Match {
        final FighterState first, cpuState;
        final CombatEngine engine;
        final FighterInput[] in = {new FighterInput(), new FighterInput()};

        Match(String p1, String cpu, float xa, float xb) {
            first = new FighterState(GeneratedCharacters.get(p1), "P1");
            cpuState = new FighterState(GeneratedCharacters.get(cpu), "CPU");
            engine = new CombatEngine(first, cpuState, xa, xb, new CombatConfig());
        }

        Match(float xa, float xb) { this("player_base", "player_base", xa, xb); }

        CombatFighter f(int i) { return engine.fighter(i); }
        CombatConfig config() { return engine.config; }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
        }

        void steps(int n) { for (int i = 0; i < n; i++) step(); }

        /** Barra cheia e o botão: o SHINKAI acende e o brilho passa. */
        void shinkai() {
            first.shinkai = CombatConfig.SHINKAI_MAX;
            in[0].shinkai = true;
            step();
            assertTrue(engine.cues().contains("SHINKAI"));
            steps(config().shinkaiFlashFrames);
        }

        /** Presses {@code button} until it starts; returns once that attack hit. */
        void hitWith(int side, Runnable button, String id) {
            for (int i = 0; i < 80; i++) {
                if (!f(side).attacking() || !f(side).attack.id.equals(id)) button.run();
                step();
                for (CombatEngine.HitEvent e : engine.events()) if (e.attacker == side && id.equals(e.moveId)) return;
            }
            fail(id + " never hit");
        }
    }

    @Test public void theRosterSplitsIntoTheFourTypes() {
        assertEquals(Shinkai.Type.POWER, Shinkai.typeOf("p13"));
        assertEquals(Shinkai.Type.SPEED, Shinkai.typeOf("p16"));
        assertEquals(Shinkai.Type.ENERGY, Shinkai.typeOf("player_base"));
        assertEquals(Shinkai.Type.TECHNIQUE, Shinkai.typeOf("p15"));
        assertEquals(Shinkai.Type.TECHNIQUE, Shinkai.typeOf("monster_npc"));
    }

    @Test public void theBarFillsTakingDamageFasterThanDealingIt() {
        Match m = new Match(600f, 690f);
        m.hitWith(0, () -> m.in[0].heavy = true, "H");
        assertTrue(m.cpuState.shinkai > 0);
        assertTrue("who got hit fills more", m.cpuState.shinkai > m.first.shinkai);
        assertTrue(m.first.shinkai > 0);
        m.cpuState.chargeShinkai(CombatConfig.SHINKAI_MAX * 2);
        assertEquals(CombatConfig.SHINKAI_MAX, m.cpuState.shinkai);
        assertTrue(m.cpuState.shinkaiReady());
    }

    @Test public void needsAFullBar() {
        Match m = new Match(600f, 900f);
        m.first.shinkai = CombatConfig.SHINKAI_MAX - 1;
        m.in[0].shinkai = true;
        m.step();
        assertFalse(m.engine.cues().contains("SHINKAI"));
        assertFalse(m.first.inShinkai());
    }

    @Test public void activationFreezesBothEmptiesTheBarAndLastsAboutEightSeconds() {
        Match m = new Match(600f, 900f);
        m.first.shinkai = CombatConfig.SHINKAI_MAX;
        m.in[0].shinkai = true;
        m.step();
        assertTrue(m.engine.cues().contains("SHINKAI"));
        assertTrue(m.first.inShinkai());
        assertEquals(0, m.first.shinkai);
        assertTrue(m.f(0).frozen() && m.f(1).frozen());
        // The activation step is the first frame of the flash; the clock waits through it.
        m.steps(m.config().shinkaiFlashFrames + m.config().shinkaiFrames - 2);
        assertTrue(m.first.inShinkai());
        m.steps(3);
        assertFalse(m.first.inShinkai());
    }

    @Test public void oncePerCharacterPerRound() {
        Match m = new Match(600f, 900f);
        m.shinkai();
        m.steps(m.config().shinkaiFrames + 2);
        m.first.chargeShinkai(CombatConfig.SHINKAI_MAX);
        assertEquals("the bar does not fill again in the round", 0, m.first.shinkai);
        m.first.shinkai = CombatConfig.SHINKAI_MAX;
        m.in[0].shinkai = true;
        m.step();
        assertFalse("already used this round", m.engine.cues().contains("SHINKAI"));
        m.engine.refreshShinkai(0);
        m.shinkai();
        assertTrue(m.first.inShinkai());
    }

    @Test public void eachMemberHasItsOwnAndLeavingThePointEndsIt() {
        Match m = new Match(600f, 900f);
        FighterState partner = new FighterState(GeneratedCharacters.get("p15"), "P1");
        m.engine.setTeam(0, new FighterState[]{m.first, partner});
        m.shinkai();
        assertTrue(m.first.inShinkai());
        m.engine.teams.sides[0].point = 1;
        m.step();
        assertFalse("off point, its SHINKAI ends", m.first.inShinkai());
        assertTrue(m.first.shinkaiUsed);
        assertFalse("the partner still has its own", partner.shinkaiUsed);
    }

    @Test public void padSendsItFromTheButtonOrSuperPlusTag() {
        PadInput pad = new PadInput();
        FighterInput out = new FighterInput();
        pad.press(PadInput.Button.SUPER);
        pad.press(PadInput.Button.TAG);
        pad.drainInto(out);
        assertTrue(out.shinkai);
        assertFalse("neither the Super nor the assist", out.superAttack || out.assist || out.tag);
        pad.press(PadInput.Button.SHINKAI);
        pad.drainInto(out);
        assertTrue(out.shinkai);
        assertEquals(ControlsLayout.Control.SHINKAI,
            ControlsLayout.controlAt(ControlsLayout.SHINKAI_X, ControlsLayout.SHINKAI_Y));
    }

    @Test public void activationCancelsTheOwnAttackAndExtendsTheCombo() {
        Match m = new Match(600f, 690f);
        m.hitWith(0, () -> m.in[0].medium = true, "M");
        int stun = m.f(1).stunLeft;
        m.first.shinkai = CombatConfig.SHINKAI_MAX;
        m.in[0].shinkai = true;
        for (int i = 0; i < 20 && !m.engine.cues().contains("SHINKAI"); i++) m.step();
        assertTrue(m.engine.cues().contains("SHINKAI"));
        assertFalse("the M was cancelled", m.f(0).attacking());
        m.steps(m.config().shinkaiFlashFrames);
        assertTrue("the opponent is still in hitstun after the flash", m.f(1).inHitstun());
        assertTrue(m.f(1).stunLeft > 0 && m.f(1).stunLeft <= stun);
    }

    @Test public void recoverableLifeComesBackOnPoint() {
        Match m = new Match(600f, 900f);
        m.first.takeDamage(3000, m.config().recoverableLifePermille);
        int life = m.first.life;
        m.steps(30);
        assertEquals("not without SHINKAI", life, m.first.life);
        m.shinkai();
        m.steps(20);
        assertTrue(m.first.life > life);
        assertTrue(m.first.life <= m.first.profile.maxLife);
    }

    @Test public void notWhileInHitstun() {
        Match m = new Match(600f, 900f);
        m.first.shinkai = CombatConfig.SHINKAI_MAX;
        m.f(0).status = CombatFighter.Status.HITSTUN;
        m.f(0).stunLeft = m.f(0).stunTotal = 30;
        m.in[0].shinkai = true;
        m.step();
        assertFalse(m.engine.cues().contains("SHINKAI"));
        assertFalse(m.first.shinkaiUsed);
    }

    // ------------------------------------------------------------------ POWER

    @Test public void powerTakesTheHitWithoutLosingItsMoveAndHitsHarder() {
        Match m = new Match("p13", "p15", 600f, 690f);
        m.shinkai();
        int life = m.first.life;
        m.hitWith(1, () -> m.in[1].heavy = true, "H");
        assertTrue(m.engine.cues().contains("SHINKAI_ARMOR"));
        assertFalse("not stunned", m.f(0).inHitstun());
        assertTrue("but it hurts", m.first.life < life);

        Match plain = new Match("p13", "p15", 600f, 690f);
        plain.hitWith(0, () -> plain.in[0].light = true, "L");
        Match power = new Match("p13", "p15", 600f, 690f);
        power.shinkai();
        power.hitWith(0, () -> power.in[0].light = true, "L");
        int normal = plain.cpuState.profile.maxLife - plain.cpuState.life;
        int strong = power.cpuState.profile.maxLife - power.cpuState.life;
        assertEquals(normal * power.config().shinkaiPowerDamagePermille / 1000, strong, 2);
    }

    // ------------------------------------------------------------------ SPEED

    @Test public void speedWalksFaster() {
        Match normal = new Match("p16", "p15", 600f, 1500f);
        Match boosted = new Match("p16", "p15", 600f, 1500f);
        boosted.shinkai();
        for (Match m : new Match[]{normal, boosted}) {
            for (int i = 0; i < 30; i++) {
                m.in[0].direction = 1;
                m.step();
            }
        }
        float walked = normal.f(0).x - 600f, fast = boosted.f(0).x - 600f;
        assertEquals(walked * boosted.config().shinkaiSpeed, fast, 2f);
    }

    @Test public void speedVanishesAndAppearsBehindTheRival() {
        Match m = new Match("p16", "p15", 600f, 900f);
        m.shinkai();
        int left = m.first.shinkaiFrames;
        m.in[0].dash = true;
        m.step();
        assertTrue(m.engine.cues().contains("VANISH"));
        assertTrue(m.f(0).vanishFrames > 0);
        assertFalse("nothing hits it while gone", m.f(0).hittable());
        m.steps(m.config().shinkaiVanishFrames + 1);
        assertTrue("behind the rival", m.f(0).x > m.f(1).x);
        assertEquals("facing the rival again", -1, m.f(0).facing);
        assertTrue("it costs SHINKAI time", m.first.shinkaiFrames < left - m.config().shinkaiVanishCost);
    }

    // ------------------------------------------------------------------ ENERGY

    @Test public void energyFillsTheSuperMeterAndFiresTwoAtOnce() {
        Match m = new Match("player_base", "p15", 300f, 1500f);
        m.shinkai();
        int meter = m.first.superMeter;
        m.steps(60);
        assertEquals(meter + 60 * m.config().shinkaiEnergyMeterPerFrame, m.first.superMeter);
        int most = 0;
        for (int i = 0; i < 120; i++) {
            if (m.f(0).canAct()) m.in[0].special = "L";
            m.step();
            most = Math.max(most, m.engine.countEnergyProjectiles(0));
        }
        assertEquals(2, most);

        Match plain = new Match("player_base", "p15", 300f, 1500f);
        int count = 0;
        for (int i = 0; i < 120; i++) {
            if (plain.f(0).canAct()) plain.in[0].special = "L";
            plain.step();
            count = Math.max(count, plain.engine.countEnergyProjectiles(0));
        }
        assertEquals("one at a time without it", 1, count);
    }

    // ------------------------------------------------------------------ TECHNIQUE

    @Test public void techniqueCancelsHeavyIntoMedium() {
        Match plain = new Match("p15", "player_base", 600f, 690f);
        plain.hitWith(0, () -> plain.in[0].heavy = true, "H");
        for (int i = 0; i < 20 && plain.f(0).attacking(); i++) {
            plain.in[0].medium = true;
            plain.step();
            assertNotEquals("M", plain.f(0).attack == null ? "" : plain.f(0).attack.id);
        }

        Match t = new Match("p15", "player_base", 600f, 690f);
        t.shinkai();
        t.hitWith(0, () -> t.in[0].heavy = true, "H");
        boolean cue = false;
        for (int i = 0; i < 20 && !(t.f(0).attacking() && t.f(0).attack.id.equals("M")); i++) {
            t.in[0].medium = true;
            t.step();
            cue |= t.engine.cues().contains("SHINKAI_CANCEL");
        }
        assertEquals("M", t.f(0).attack.id);
        assertTrue(cue);
    }

    @Test public void techniqueParriesWithATapForward() {
        Match m = new Match("p15", "player_base", 600f, 690f);
        m.shinkai();
        int life = m.first.life;
        boolean parried = false;
        for (int i = 0; i < 80 && !parried; i++) {
            // O CPU solta o H; o P1 toca → a cada poucos quadros (solta e aperta de novo).
            if (!m.f(1).attacking()) m.in[1].heavy = true;
            m.in[0].direction = i % 4 < 2 ? 1 : 0;
            m.step();
            parried = m.engine.cues().contains("PARRY");
        }
        assertTrue(parried);
        assertEquals("nothing went in", life, m.first.life);
        assertFalse(m.f(0).inHitstun());

        // Without the tap the same H hits.
        Match hit = new Match("p15", "player_base", 600f, 690f);
        hit.shinkai();
        hit.hitWith(1, () -> hit.in[1].heavy = true, "H");
        assertTrue(hit.first.life < life);
    }
}
