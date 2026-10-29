package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * O repel da p15 (S2) que desfaz um projétil (frente H4): a aura apaga logo depois e ela fica
 * livre, como quem defendeu. Antes, desfazer a bola custava o golpe inteiro (64 quadros) e o
 * zoner atirava de novo antes de ela sair do lugar.
 */
public class RepelProjetilTest {
    /** Ryūga atira o S1 de {@code gap} px; a p15 aperta o S2 {@code delay} quadros depois. */
    private static int[] repelAShot(int delay) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get("player_base"), GeneratedCharacters.get("p15"), 400f, 850f);
        s.in[0].special = "M";
        int repelled = -1, free = -1;
        for (int fr = 0; fr < 200 && free < 0; fr++) {
            if (fr == delay) {
                CombatFighter p15 = s.f(1);
                p15.commandButton = "M";
                p15.buffer.push(InputBuffer.Button.SPECIAL, "S2", false);
            }
            s.step();
            if (repelled < 0 && s.engine.cues().contains("REPEL")) repelled = fr;
            if (repelled >= 0 && s.f(1).canAct()) free = fr;
        }
        return new int[] {repelled, free};
    }

    @Test public void theRepelThatDissolvesAShotEndsSoon() {
        boolean any = false;
        for (int delay = 0; delay < 40; delay++) {
            int[] r = repelAShot(delay);
            if (r[0] < 0) continue;
            any = true;
            assertTrue("delay " + delay + ": livre " + (r[1] - r[0]) + " quadros depois de desfazer",
                r[1] >= 0 && r[1] - r[0] <= new CombatConfig().repelShotFreeFrames + 2);
        }
        assertTrue("em algum tempo o repel desfaz a bola", any);
    }

    @Test public void aWhiffedRepelStillCostsTheWholeMove() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get("player_base"), GeneratedCharacters.get("p15"), 400f, 850f);
        CombatFighter p15 = s.f(1);
        p15.commandButton = "M";
        p15.buffer.push(InputBuffer.Button.SPECIAL, "S2", false);
        int frames = 0;
        s.step();
        while (!p15.canAct() && frames < 200) { s.step(); frames++; }
        assertTrue("o repel no vazio vai inteiro: " + frames, frames >= p15.character().attack("S2").totalFrames - 3);
    }
}
