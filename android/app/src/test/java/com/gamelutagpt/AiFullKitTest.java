package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.Random;
import org.junit.Test;

/** Batalha (IA ON): o CPU usa assist, troca, Assist → Tag, Guard Cancel, pushblock, Overdrive e Ultra. */
public class AiFullKitTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition TWO = GeneratedCharacters.get("player_two");
    private static final int CPU = 1;

    /** Jogador contra a dupla do CPU, que é controlada pela IA. */
    private static final class Match {
        final FighterState[] cpu = {new FighterState(BASE, "CPU"), new FighterState(TWO, "CPU")};
        final CombatEngine engine;
        final AiController ai;
        final FighterInput out = new FighterInput();

        Match(float distance, boolean fullKit, long seed) {
            FighterState p1 = new FighterState(BASE, "P1");
            engine = new CombatEngine(p1, cpu[0], 500f, 500f + distance, new CombatConfig());
            engine.setTeam(0, p1, new FighterState(TWO, "P2"));
            engine.setTeam(CPU, cpu);
            ai = new AiController(new OpponentAi(new Random(seed)), new Random(seed * 0x9E3779B97F4A7C15L));
            ai.setEnabled(true);
            ai.setFullKit(fullKit);
        }

        CombatFighter me() { return engine.fighter(CPU); }

        /** Chama a IA até ela apertar o que o teste procura, sem andar o motor. */
        boolean pressesWithin(int frames, java.util.function.Predicate<FighterInput> pressed) {
            for (int i = 0; i < frames; i++) {
                ai.fill(engine, CPU, out);
                if (pressed.test(out)) return true;
            }
            return false;
        }
    }

    @Test public void cpuCallsItsAssistFromMidRange() {
        Match m = new Match(400f, true, 1);
        assertTrue(m.pressesWithin(600, in -> in.assist));
    }

    @Test public void cpuGoesForTheUltraWithThreeBarsUpClose() {
        Match m = new Match(250f, true, 2);
        m.cpu[0].superMeter = CombatConfig.ULTRA_COST;
        assertTrue(m.pressesWithin(600, in -> in.ultra));
    }

    @Test public void cpuTagsOutAHurtPointForAHealthyPartner() {
        Match m = new Match(600f, true, 3);
        m.cpu[0].takeDamage(m.cpu[0].life * 3 / 4, 0);
        assertTrue(m.pressesWithin(900, in -> in.tag));
    }

    @Test public void cpuTurnsOverdriveOnWhenHurt() {
        Match m = new Match(600f, true, 4);
        m.cpu[0].takeDamage(m.cpu[0].life * 2 / 3, 0);
        assertTrue(m.pressesWithin(3000, in -> in.overdrive));
    }

    @Test public void blockingCpuGuardCancelsOrPushblocks() {
        boolean guardCancel = false, pushblock = false;
        for (long seed = 1; seed <= 40 && !(guardCancel && pushblock); seed++) {
            Match m = new Match(150f, true, seed);
            m.cpu[0].superMeter = CombatConfig.METER_PER_BAR;
            CombatFighter me = m.me();
            // Cada sorteio vale para uma sequência de bloqueio nova.
            me.status = CombatFighter.Status.NEUTRAL;
            m.ai.fill(m.engine, CPU, m.out);
            me.status = CombatFighter.Status.BLOCKSTUN;
            me.stunLeft = 10;
            m.ai.fill(m.engine, CPU, m.out);
            guardCancel |= m.out.assist;
            pushblock |= m.out.pushblock;
        }
        assertTrue("Guard Cancel Tag", guardCancel);
        assertTrue("pushblock", pushblock);
    }

    @Test public void theRealAssistComesOutInTheEngine() {
        Match m = new Match(400f, true, 5);
        FighterInput player = new FighterInput();
        boolean seen = false;
        for (int i = 0; i < 1200 && !seen; i++) {
            m.ai.fill(m.engine, CPU, m.out);
            m.engine.step(player, m.out);
            seen = m.engine.team(CPU).assistOut();
        }
        assertTrue("o assist do CPU entra de verdade", seen);
    }

    @Test public void trainingCpuNeverUsesTheTeamOrTheUltra() {
        Match m = new Match(300f, false, 6);
        m.cpu[0].superMeter = CombatConfig.ULTRA_COST;
        m.cpu[0].takeDamage(m.cpu[0].life * 3 / 4, 0);
        assertFalse(m.pressesWithin(1500, in -> in.assist || in.tag || in.ultra || in.overdrive || in.pushblock));
    }
}
