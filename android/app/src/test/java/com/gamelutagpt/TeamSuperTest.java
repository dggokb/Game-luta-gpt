package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/** DHC / Team Super and recoverable life (v0.85). */
public class TeamSuperTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition TWO = GeneratedCharacters.get("player_two");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();

    private static final class Match {
        final FighterState first = new FighterState(BASE, "P1");
        final FighterState second = new FighterState(TWO, "P2");
        final CombatEngine engine = new CombatEngine(first, new FighterState(NPC, "CPU"), 600f, 900f, new CombatConfig());
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

        /** Starts the point's Super and runs until it released. */
        void superReleased() {
            in[0].superAttack = true;
            step();
            assertEquals(AttackDefinition.Kind.SUPER, f(0).attack.kind);
            for (int i = 0; i < 80 && f(0).attackFrame < f(0).attack.startupFrames; i++) step();
        }
    }

    @Test public void tagDuringTheReleasedSuperCallsThePartnersSuper() {
        Match m = new Match(true);
        m.first.superMeter = 2 * CombatConfig.METER_PER_BAR;
        m.superReleased();
        assertTrue(m.engine.teams.dhcReady(0, m.f(0)));
        m.in[0].assist = true;
        m.step();
        assertTrue(m.engine.cues().contains("DHC"));
        assertSame("the partner took the point", m.second, m.f(0).state);
        assertTrue(m.f(0).attacking());
        assertEquals(AttackDefinition.Kind.SUPER, m.f(0).attack.kind);
        assertEquals("two bars spent in all", 0, m.second.superMeter);
        assertSame(m.first, m.engine.team(0).leaving);
        assertTrue("its own Super freeze", m.engine.superFreezeActive());
    }

    @Test public void noDhcBeforeTheSuperFiresOrWithoutABar() {
        Match early = new Match(true);
        early.first.superMeter = 2 * CombatConfig.METER_PER_BAR;
        early.in[0].superAttack = true;
        early.step();
        early.in[0].assist = true;
        early.step();
        assertSame(early.first, early.f(0).state);

        Match poor = new Match(true);
        poor.first.superMeter = CombatConfig.METER_PER_BAR;
        poor.superReleased();
        assertFalse(poor.engine.teams.dhcReady(0, poor.f(0)));
        poor.in[0].assist = true;
        poor.step();
        assertSame(poor.first, poor.f(0).state);
    }

    @Test public void hitsLeaveRecoverableLifeThatComesBackOffPoint() {
        Match m = new Match(true);
        int max = m.first.profile.maxLife;
        m.first.takeDamage(3000, m.config().recoverableLifePermille);
        assertEquals(max - 3000, m.first.life);
        assertEquals(3000 * m.config().recoverableLifePermille / 1000, m.first.recoverableLife);
        m.steps(30);
        assertEquals("no recovery on point", max - 3000, m.first.life);

        // Tag out: the first fighter waits off screen and gets it back.
        m.engine.setTeam(0, m.second, m.first);
        m.steps(30);
        assertEquals(max - 3000 + 30 * m.config().recoverableLifePerFrame, m.first.life);
        m.steps(400);
        assertEquals("only the recoverable part comes back", max - 3000 + 810, m.first.life);
        assertEquals(0, m.first.recoverableLife);
    }

    @Test public void recoverableLifeNeverPassesTheMaxAndAKoClearsIt() {
        FighterState f = new FighterState(BASE, "P1");
        f.takeDamage(100, 1000);
        assertEquals(100, f.recoverableLife);
        f.takeDamage(f.life, 270);
        assertEquals(0, f.life);
        assertEquals(0, f.recoverableLife);
        f.regenerate(500);
        assertEquals("a KO'd fighter does not come back", 0, f.life);
    }

    @Test public void theDummyHasNoTeamAndNoRegeneration() {
        Match m = new Match(false);
        FighterState cpu = m.f(1).state;
        cpu.takeDamage(2000, m.config().recoverableLifePermille);
        int life = cpu.life;
        m.steps(60);
        assertEquals(life, cpu.life);
    }
}
