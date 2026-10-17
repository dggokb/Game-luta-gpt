package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/** READY → GO depois da intro, a batalha 2x2 (derrota quando os dois da dupla caem) e o tempo. */
public class BattleFlowTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition TWO = GeneratedCharacters.get("player_two");

    /** Duas duplas, uma de cada lado, e o fluxo da luta. */
    private static final class Match {
        final FighterState[] p1 = {new FighterState(BASE, "P1"), new FighterState(TWO, "P2")};
        final FighterState[] cpu = {new FighterState(BASE, "CPU"), new FighterState(TWO, "CPU")};
        final CombatEngine engine = new CombatEngine(p1[0], cpu[0], 500f, 900f, new CombatConfig());
        final FighterInput[] in = {new FighterInput(), new FighterInput()};
        final BattleFlow flow = new BattleFlow();

        Match(boolean battle) {
            engine.setTeam(0, p1);
            engine.setTeam(1, cpu);
            flow.setBattle(battle);
            flow.reset();
            flow.startCountdown();
        }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
            flow.update(engine, true);
        }

        void steps(int n) { for (int i = 0; i < n; i++) step(); }

        /** Quadros sem ninguém apertar nada, com o relógio correndo ou não. */
        void wait(int n, boolean clockRuns) {
            for (int i = 0; i < n; i++) {
                engine.step(in[0], in[1]);
                flow.update(engine, true, clockRuns);
            }
        }

        void fight() { steps(BattleFlow.READY_FRAMES + BattleFlow.GO_FRAMES); }

        /** Até o GO: o controle volta e o relógio começa a contar. */
        void go() { steps(BattleFlow.READY_FRAMES); }

        static void knockOut(FighterState f) { f.takeDamage(f.life, 0); }
    }

    @Test public void readyHoldsTheFightersThenGoReleasesThem() {
        Match m = new Match(false);
        assertEquals("READY", m.flow.banner());
        assertTrue(m.flow.holdsFighters());
        m.steps(BattleFlow.READY_FRAMES - 1);
        assertTrue(m.flow.holdsFighters());
        m.step();
        assertEquals(BattleFlow.Phase.GO, m.flow.phase());
        assertEquals("GO!", m.flow.banner());
        assertFalse("no GO o controle já volta", m.flow.holdsFighters());
        m.steps(BattleFlow.GO_FRAMES);
        assertEquals(BattleFlow.Phase.FIGHT, m.flow.phase());
        assertNull(m.flow.banner());
    }

    @Test public void theIntroComesBeforeTheReady() {
        BattleFlow flow = new BattleFlow();
        flow.reset();
        assertEquals(BattleFlow.Phase.INTRO, flow.phase());
        assertFalse(flow.holdsFighters());
        assertNull(flow.banner());
    }

    @Test public void theFallenPointIsReplacedByThePartnerAndTheFightGoesOn() {
        Match m = new Match(true);
        m.fight();
        Match.knockOut(m.cpu[0]);
        m.step();
        assertFalse("um lado ainda tem quem lute", m.flow.over());
        assertSame("o caído fica no chão um pouco", m.cpu[0], m.engine.fighter(1).state);
        m.steps(m.engine.config.koReplaceFrames);
        CombatFighter in = m.engine.fighter(1);
        assertSame("o parceiro entra", m.cpu[1], in.state);
        assertFalse(in.ko());
        assertTrue("entra correndo, sem controle", in.locked);
        assertFalse("ninguém bate em quem está entrando", in.hittable());
        assertTrue(m.engine.team(1).tagging());
        assertEquals(BattleFlow.NONE, m.flow.winner());
        int entry = m.engine.config.tagEnterFrames + m.engine.config.tagPoseFrames;
        m.steps(entry + 1);
        assertFalse("depois da entrada ele luta", m.engine.fighter(1).locked);
        assertFalse(m.flow.over());
    }

    @Test public void theTeamLosesOnlyWhenBothFall() {
        Match m = new Match(true);
        m.fight();
        Match.knockOut(m.cpu[0]);
        m.steps(m.engine.config.koReplaceFrames + 1);
        assertFalse(m.flow.over());
        Match.knockOut(m.cpu[1]);
        m.step();
        assertTrue(m.flow.over());
        assertEquals("o jogador venceu", 0, m.flow.winner());
        assertEquals("K.O.", m.flow.banner());
        m.steps(BattleFlow.KO_FRAMES);
        assertEquals("VITÓRIA", m.flow.banner());
        assertFalse(m.flow.restartDue());
        m.steps(BattleFlow.RESULT_FRAMES);
        assertTrue(m.flow.restartDue());
    }

    @Test public void theFallenPartnerCannotBeCalled() {
        Match m = new Match(true);
        m.fight();
        Match.knockOut(m.p1[1]);
        m.in[0].assist = true;
        m.step();
        assertFalse("parceiro caído não vem de assist", m.engine.team(0).assistOut());
        m.in[0].tag = true;
        m.step();
        assertFalse("nem na troca", m.engine.team(0).tagging());
        Match.knockOut(m.p1[0]);
        m.step();
        assertTrue("sem ninguém de pé, a dupla perdeu", m.flow.over());
        assertEquals(1, m.flow.winner());
    }

    @Test public void bothTeamsFallingTogetherIsADraw() {
        Match m = new Match(true);
        m.fight();
        for (FighterState f : m.p1) Match.knockOut(f);
        for (FighterState f : m.cpu) Match.knockOut(f);
        m.step();
        assertEquals(BattleFlow.DRAW, m.flow.winner());
    }

    @Test public void trainingKeepsTheOldRulesWithoutReplacing() {
        Match m = new Match(false);
        m.fight();
        Match.knockOut(m.cpu[0]);
        m.steps(m.engine.config.koReplaceFrames + 10);
        assertSame("no treino ninguém entra no lugar", m.cpu[0], m.engine.fighter(1).state);
        assertFalse(m.flow.over());
    }

    @Test public void theClockStartsAt120AndRunsOnlyFromTheGo() {
        Match m = new Match(true);
        assertEquals("120", m.flow.clockLabel());
        m.steps(BattleFlow.READY_FRAMES);
        assertEquals("o READY não gasta tempo", BattleFlow.TIME_LIMIT_SECONDS, m.flow.clockSeconds());
        m.steps(BattleFlow.FRAMES_PER_SECOND);
        assertEquals(BattleFlow.TIME_LIMIT_SECONDS - 1, m.flow.clockSeconds());
        assertEquals("119", m.flow.clockLabel());
        assertFalse(m.flow.clockUrgent());
    }

    @Test public void trainingHasNoClock() {
        Match m = new Match(false);
        m.fight();
        m.steps(BattleFlow.TIME_LIMIT_FRAMES + 10);
        assertEquals("∞", m.flow.clockLabel());
        assertFalse(m.flow.over());
        assertFalse(m.flow.timeOver());
    }

    @Test public void timeOverGivesTheWinToTheTeamWithMoreLife() {
        Match m = new Match(true);
        m.go();
        m.cpu[1].takeDamage(100, 0);
        m.steps(BattleFlow.TIME_LIMIT_FRAMES - 1);
        assertFalse("falta um quadro", m.flow.over());
        assertTrue("últimos segundos", m.flow.clockUrgent());
        m.step();
        assertTrue(m.flow.over());
        assertTrue(m.flow.timeOver());
        assertEquals("0", m.flow.clockLabel());
        assertEquals("a dupla com mais vida venceu", 0, m.flow.winner());
        assertEquals("TIME OVER", m.flow.banner());
        m.steps(BattleFlow.KO_FRAMES);
        assertEquals("VITÓRIA", m.flow.banner());
        m.steps(BattleFlow.RESULT_FRAMES);
        assertTrue(m.flow.restartDue());
    }

    @Test public void timeOverCountsTheWholeTeamNotOnlyThePoint() {
        Match m = new Match(true);
        m.fight();
        // O titular do jogador está pior, mas a dupla dele tem mais vida no total.
        m.p1[0].takeDamage(300, 0);
        m.cpu[0].takeDamage(100, 0);
        m.cpu[1].takeDamage(m.cpu[1].life - 1, 0);
        m.wait(BattleFlow.TIME_LIMIT_FRAMES, true);
        assertTrue(m.flow.timeOver());
        assertEquals(0, m.flow.winner());
    }

    @Test public void equalLifeAtTimeOverIsADraw() {
        Match m = new Match(true);
        m.fight();
        m.steps(BattleFlow.TIME_LIMIT_FRAMES);
        assertTrue(m.flow.timeOver());
        assertEquals(BattleFlow.DRAW, m.flow.winner());
        m.steps(BattleFlow.KO_FRAMES);
        assertEquals("EMPATE", m.flow.banner());
    }

    @Test public void aKnockOutOnTheLastFrameBeatsTheClock() {
        Match m = new Match(true);
        m.go();
        m.steps(BattleFlow.TIME_LIMIT_FRAMES - 1);
        for (FighterState f : m.p1) Match.knockOut(f);
        m.step();
        assertTrue(m.flow.over());
        assertFalse("acabou por K.O.", m.flow.timeOver());
        assertEquals(1, m.flow.winner());
        assertEquals("K.O.", m.flow.banner());
    }

    @Test public void theClockStopsWhileFrozen() {
        Match m = new Match(true);
        m.fight();
        m.wait(BattleFlow.TIME_LIMIT_FRAMES + 30, false);
        assertEquals("congelado (Super/Ultra) o relógio não anda", BattleFlow.TIME_LIMIT_SECONDS, m.flow.clockSeconds());
        assertFalse(m.flow.over());
    }

    @Test public void aStorySceneHoldingTheFightHidesTheClock() {
        Match m = new Match(true);
        m.flow.setClockHeld(true);
        m.fight();
        m.steps(BattleFlow.TIME_LIMIT_FRAMES + 30);
        assertEquals("∞", m.flow.clockLabel());
        assertFalse("a cena decide o fim, não o tempo", m.flow.over());
        m.flow.setClockHeld(false);
        assertEquals("o relógio volta de onde estava", "120", m.flow.clockLabel());
        m.flow.reset();
        assertFalse(m.flow.timeOver());
    }

    @Test public void restartingRefillsTheClock() {
        Match m = new Match(true);
        m.fight();
        m.steps(BattleFlow.TIME_LIMIT_FRAMES);
        assertTrue(m.flow.timeOver());
        m.flow.reset();
        assertFalse(m.flow.timeOver());
        assertEquals(BattleFlow.TIME_LIMIT_SECONDS, m.flow.clockSeconds());
    }
}
