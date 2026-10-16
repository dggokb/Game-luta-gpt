package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/** Punish counter: um H em pé no rival que está golpeando o joga girando no ar. */
public class PunishCounterTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();

    private static final class Sim {
        final CombatEngine engine = new CombatEngine(new FighterState(BASE, "P1"), new FighterState(NPC, "P2"),
            500f, 640f, new CombatConfig());
        final FighterInput[] in = {new FighterInput(), new FighterInput()};
        boolean punishCue;
        /** Puts the rival in its recovery just before the strike lands. */
        boolean rivalBusy;

        CombatFighter p() { return engine.fighter(0); }
        CombatFighter d() { return engine.fighter(1); }

        void step() {
            engine.step(in[0], in[1]);
            if (engine.cues().contains("PUNISH")) punishCue = true;
            in[0].clearPresses();
            in[1].clearPresses();
        }

        /** The rival is in the recovery of its own heavy (the hit box is gone, the body is open). */
        private void rivalRecovering() {
            CombatFighter f = d();
            f.clearAttack();
            f.status = CombatFighter.Status.ATTACK;
            f.attack = NPC.attack("H");
            f.attackFrame = f.attack.startupFrames + f.attack.activeFrames;
        }

        /** Presses a button and steps until the first hit lands (or the limit runs out). */
        boolean strike(boolean heavy) {
            if (heavy) in[0].heavy = true; else in[0].light = true;
            int life = d().state.life;
            for (int i = 0; i < 40; i++) {
                if (rivalBusy && p().attacking() && p().attackFrame == p().attack.startupFrames - 2) rivalRecovering();
                step();
                if (d().state.life < life) return true;
            }
            return false;
        }
    }

    @Test public void heavyOnARecoveringRivalSendsItSpinning() {
        Sim s = new Sim();
        s.rivalBusy = true;
        assertTrue("the heavy connects", s.strike(true));
        assertTrue(s.punishCue);
        assertTrue(s.d().spinning);
        float from = s.d().x;
        int flight = 0;
        while (!s.d().grounded && flight < 120) {
            assertEquals(CombatFighter.Status.AIR_HITSTUN, s.d().status);
            s.step();
            flight++;
        }
        float distance = s.d().x - from;
        assertTrue("medium distance away from the attacker: " + distance, distance > 250f && distance < 500f);
        assertTrue("a real flight: " + flight, flight > 40);
        assertEquals("lands lying down", CombatFighter.Status.KNOCKDOWN, s.d().status);
        assertFalse(s.d().spinning);
    }

    @Test public void punishAndKnockoutFreezeTheImpactLonger() {
        Sim plain = new Sim();
        assertTrue(plain.strike(true));
        int normal = plain.d().hitstop;
        Sim punish = new Sim();
        punish.rivalBusy = true;
        assertTrue(punish.strike(true));
        assertEquals(normal + punish.engine.config.punishHitstopBonus, punish.d().hitstop);
        Sim ko = new Sim();
        ko.d().state.life = 1;
        assertTrue(ko.strike(true));
        assertEquals(normal + ko.engine.config.koHitstopBonus, ko.d().hitstop);
    }

    @Test public void heavyOnAnIdleRivalIsAPlainHit() {
        Sim s = new Sim();
        assertTrue(s.strike(true));
        assertFalse(s.punishCue);
        assertFalse(s.d().spinning);
        assertTrue(s.d().grounded);
    }

    @Test public void lightOnARecoveringRivalDoesNotSpin() {
        Sim s = new Sim();
        s.p().x = 560f;                     // the jab is short
        s.rivalBusy = true;
        assertTrue(s.strike(false));
        assertFalse(s.punishCue);
        assertFalse(s.d().spinning);
    }

    @Test public void theSpinCanBeJuggledAndEndsWithTheNextHit() {
        Sim s = new Sim();
        s.rivalBusy = true;
        assertTrue(s.strike(true));
        assertTrue(s.d().spinning);
        assertTrue("open to a follow-up", s.d().hittable());
    }
}
