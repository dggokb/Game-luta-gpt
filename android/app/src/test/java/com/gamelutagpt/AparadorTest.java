package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Aparar, o S3 do Ryūga (frente H4): postura que lê qualquer golpe corpo a corpo do 4º ao 13º
 * quadro e responde na hora; no vazio não bate (era um golpe comum de 20 quadros) e fica aberto
 * até o fim.
 */
public class AparadorTest {
    private static final CharacterDefinition RYUGA = GeneratedCharacters.get("player_base");

    /** O Ryūga aperta o Aparar no quadro 0; o rival (Ryūga também) aperta {@code button} no quadro {@code delay}. */
    private static int[] parry(int delay, String button, int gap) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(RYUGA, RYUGA, 600f, 600f + gap);
        CombatFighter me = s.f(0), rival = s.f(1);
        int myLife = me.state.life, rivalLife = rival.state.life;
        boolean cue = false;
        me.commandButton = "M";
        me.buffer.push(InputBuffer.Button.SPECIAL, "S3", false);
        for (int fr = 0; fr < 80; fr++) {
            if (fr == delay) {
                if (button.startsWith("2")) s.in[1].direction = 2;
                switch (button.substring(button.length() - 1)) {
                    case "L": s.in[1].light = true; break;
                    case "M": s.in[1].medium = true; break;
                    default: s.in[1].heavy = true; break;
                }
            }
            s.step();
            s.in[1].direction = 0;
            cue |= s.engine.cues().contains("COUNTER");
        }
        return new int[] {cue ? 1 : 0, myLife - me.state.life, rivalLife - rival.state.life};
    }

    @Test public void theParryReadsAStrikeAndAnswers() {
        for (String b : new String[] {"L", "2L", "M"}) {
            boolean read = false;
            for (int delay = 0; delay < 14 && !read; delay++) {
                int[] r = parry(delay, b, 150);
                if (r[0] == 1) {
                    read = true;
                    assertEquals(b + ": quem aparou não leva nada", 0, r[1]);
                    assertTrue(b + ": a resposta acerta", r[2] > 0);
                }
            }
            assertTrue(b + " é lido (alto e baixo)", read);
        }
    }

    @Test public void aParryInTheEmptyDoesNotStrike() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(RYUGA, RYUGA, 600f, 720f);
        CombatFighter me = s.f(0), rival = s.f(1);
        int life = rival.state.life;
        me.commandButton = "M";
        me.buffer.push(InputBuffer.Button.SPECIAL, "S3", false);
        s.step();
        assertEquals("S3", s.attackId(0));
        int frames = 1;
        while (!me.canAct() && frames < 100) { s.step(); frames++; }
        assertEquals("no vazio não bate no rival colado", life, rival.state.life);
        assertTrue("fica aberto o golpe inteiro: " + frames, frames >= RYUGA.attack("S3").totalFrames - 2);
    }
}
