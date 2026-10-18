package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.Random;
import org.junit.Test;

/** Perto do fim do tempo a CPU fica com pressa: quer terminar a luta, não deixar o tempo acabar. */
public class PressaDoTempoTest {

    @Test public void urgencyGrowsAsTheClockRunsOut() {
        assertEquals("sem relógio (treino): calma", 0f, OpponentAi.urgency(-1), 0f);
        assertEquals("começo da luta: calma", 0f, OpponentAi.urgency(BattleFlow.TIME_LIMIT_SECONDS), 0f);
        assertEquals(0f, OpponentAi.urgency(OpponentAi.URGENCY_START_SECONDS), 0f);
        float middle = OpponentAi.urgency((OpponentAi.URGENCY_START_SECONDS + OpponentAi.URGENCY_FULL_SECONDS) / 2);
        assertTrue(middle > 0.3f && middle < 0.7f);
        assertEquals("últimos segundos: pressa toda", 1f, OpponentAi.urgency(OpponentAi.URGENCY_FULL_SECONDS), 0f);
        assertEquals(1f, OpponentAi.urgency(0), 0f);
    }

    @Test public void aHurriedCpuAttacksMoreAndWaitsLess() {
        int[] calm = fight(-1), hurried = fight(5);
        assertTrue("ataca mais: " + calm[0] + " → " + hurried[0], hurried[0] > calm[0]);
        assertTrue("bate mais: " + calm[1] + " → " + hurried[1], hurried[1] > calm[1]);
        assertTrue("fica menos defendendo: " + calm[2] + " → " + hurried[2], hurried[2] < calm[2]);
    }

    /** CPU nível 5 contra outra CPU calma: golpes começados, dano dado e quadros defendendo. */
    private static int[] fight(int secondsLeft) {
        String[] ids = {"player_base", "p03", "p05", "p07", "p09", "p12", "p02", "p10"};
        int starts = 0, damage = 0, guarding = 0;
        for (int seed = 1; seed <= 8; seed++) {
            Random r = new Random(seed);
            FighterState p = new FighterState(GeneratedCharacters.get(ids[r.nextInt(ids.length)]), "P1");
            FighterState c = new FighterState(GeneratedCharacters.get(ids[r.nextInt(ids.length)]), "CPU");
            CombatEngine e = new CombatEngine(p, c, 400f, 900f, new CombatConfig());
            AiController calm = new AiController(new OpponentAi(new Random(seed)), new Random(seed * 31L));
            AiController cpu = new AiController(new OpponentAi(new Random(seed + 99)), new Random(seed * 77L));
            calm.setEnabled(true);
            cpu.setEnabled(true);
            FighterInput a = new FighterInput(), b = new FighterInput();
            for (int f = 0; f < 600 && p.life > 0 && c.life > 0; f++) {
                cpu.setSecondsLeft(secondsLeft);
                calm.fill(e, 0, a);
                cpu.fill(e, 1, b);
                e.step(a, b);
                CombatFighter me = e.fighter(1);
                if (me.attacking() && me.attackFrame == 0) starts++;
                // Escolhendo esperar: segurando a defesa sem estar levando golpe (o bloqueio de
                // quem avança e leva o golpe na defesa não é esperar).
                boolean holdsBack = me.facing > 0 ? b.direction == 5 || b.direction == 4 : b.direction == 1 || b.direction == 2;
                if (holdsBack && me.status != CombatFighter.Status.BLOCKSTUN) guarding++;
            }
            damage += p.profile.maxLife - p.life;
        }
        return new int[] {starts, damage, guarding};
    }
}
