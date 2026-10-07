package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/** Every demo button (v0.80…v0.85, v0.87) really shows its feature, and gives the match back. */
public class DemoDirectorTest {
    private static final float HOME_P1 = 420f, HOME_CPU = 980f;

    /** The match as GameView builds it: the team against a CPU with the base character. */
    private static final class Match {
        final FighterState[] team = {
            new FighterState(GeneratedCharacters.get(GeneratedCharacters.TEAM[0]), "PLAYER 1"),
            new FighterState(GeneratedCharacters.get(GeneratedCharacters.TEAM[1]), "PLAYER 2")
        };
        final FighterState cpu = new FighterState(GeneratedCharacters.get(GeneratedCharacters.TEAM[0]), "CPU");
        final CombatEngine engine = new CombatEngine(team[0], cpu, HOME_P1, HOME_CPU, new CombatConfig());
        final FighterInput p1 = new FighterInput(), npc = new FighterInput();
        final DemoDirector demo = new DemoDirector();

        Match() { engine.setTeam(0, team); }

        /** Plays demo {@code index} to the end; returns how many frames it took. */
        int play(int index) {
            demo.start(index, engine, team, cpu, HOME_P1, HOME_CPU);
            int frames = 0;
            while (demo.active() && frames < 5000) {
                demo.fill(p1, npc);
                engine.step(p1, npc);
                demo.afterStep();
                frames++;
            }
            assertFalse("demo " + index + " never ends", demo.active());
            return frames;
        }
    }

    private static void assertEveryStepShown(int index) {
        Match m = new Match();
        m.play(index);
        boolean[] results = m.demo.results();
        DemoDirector.Step[] steps = DemoDirector.steps(index);
        for (int i = 0; i < results.length; i++) {
            assertTrue("v0." + DemoDirector.VERSIONS[index] + " " + steps[i].title + " was not shown", results[i]);
        }
    }

    @Test public void v80ThrowTechAndWhiff() { assertEveryStepShown(0); }
    @Test public void v81PushblockAndGuardCancel() { assertEveryStepShown(1); }
    @Test public void v82AirDashes() { assertEveryStepShown(2); }
    @Test public void v83WallAndGroundBounce() { assertEveryStepShown(3); }
    @Test public void v84AirTechAndWakeUps() { assertEveryStepShown(4); }
    @Test public void v85DhcAndRecoverableLife() { assertEveryStepShown(5); }
    @Test public void v87Overdrive() { assertEveryStepShown(6); }

    @Test public void theMatchComesBackAsItWas() {
        Match m = new Match();
        m.team[0].superMeter = 1234;
        m.team[0].life -= 700;
        m.cpu.life -= 300;
        int life = m.team[0].life, cpuLife = m.cpu.life;
        for (int i = 0; i < DemoDirector.VERSIONS.length; i++) m.play(i);
        assertEquals(1234, m.team[0].superMeter);
        assertEquals(life, m.team[0].life);
        assertEquals(cpuLife, m.cpu.life);
        assertSame(m.team[0], m.engine.fighter(0).state);
        assertEquals(HOME_P1, m.engine.fighter(0).x, 0f);
        assertEquals(HOME_CPU, m.engine.fighter(1).x, 0f);
        assertEquals(CombatFighter.Status.NEUTRAL, m.engine.fighter(0).status);
        assertFalse(m.engine.team(0).tagging() || m.engine.team(0).assistOut());
    }

    @Test public void startingAnotherDemoReplacesTheOneThatPlays() {
        Match m = new Match();
        m.demo.start(0, m.engine, m.team, m.cpu, HOME_P1, HOME_CPU);
        for (int i = 0; i < 60; i++) {
            m.demo.fill(m.p1, m.npc);
            m.engine.step(m.p1, m.npc);
            m.demo.afterStep();
        }
        m.demo.start(3, m.engine, m.team, m.cpu, HOME_P1, HOME_CPU);
        assertEquals(3, m.demo.demo());
        assertEquals(0, m.demo.stepIndex());
        assertTrue(m.demo.title().startsWith("DEMO v0.83"));
    }
}
