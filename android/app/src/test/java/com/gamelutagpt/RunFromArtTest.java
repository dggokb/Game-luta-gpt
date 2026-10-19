package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Corrida tirada do vídeo (bloco "corrida" do p01): o corpo acelera na arrancada e desliza na
 * freada, no mesmo passo do desenho, para o pé não patinar. Sem o bloco, a corrida sai e para
 * na hora, como antes.
 */
public class RunFromArtTest {
    private static final CharacterDefinition P01 = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();
    private static final int FRENTE = 1;

    private static final class Sim {
        final CombatEngine engine;
        final FighterInput[] in = {new FighterInput(), new FighterInput()};
        Sim(CharacterDefinition c) {
            engine = new CombatEngine(new FighterState(c, "P1"), new FighterState(NPC, "CPU"), 300f, 1900f, new CombatConfig());
        }
        CombatFighter p() { return engine.fighter(0); }
        /** Um passo; devolve quanto o lutador andou nele. */
        float step() {
            float x = p().x;
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
            return p().x - x;
        }
        /** Corre {@code frames} passos e devolve o passo do último. */
        float run(int frames) {
            in[0].direction = FRENTE;
            in[0].dash = true;
            float dx = 0;
            for (int i = 0; i < frames; i++) dx = step();
            assertTrue("correndo", p().forwardDashing);
            return dx;
        }
    }

    @Test public void theRunAcceleratesOverTheStartAndThenKeepsItsSpeed() {
        int start = P01.fighter.runStartFrames;
        assertTrue("p01 tem arrancada", start > 1);
        Sim s = new Sim(P01);
        float first = s.run(1);
        float full = s.run(start + 5);
        assertTrue("sai devagar: " + first + " de " + full, first > 0f && first <= full / start + 0.01f);
        assertEquals("depois da arrancada, velocidade constante", full, s.step(), 0.001f);
    }

    @Test public void releasingTheRunSlidesToAStopInTheBrakeFrames() {
        int brake = P01.fighter.runBrakeFrames;
        assertTrue("p01 tem freada", brake > 1);
        Sim s = new Sim(P01);
        float full = s.run(P01.fighter.runStartFrames + 10);
        s.in[0].direction = 0;
        float slid = 0, last = full;
        for (int i = 0; i < brake; i++) {
            float dx = s.step();
            assertTrue("desacelera", dx > 0f && dx <= last + 0.001f);
            last = dx;
            slid += dx;
        }
        assertEquals("parou", 0f, s.step(), 0f);
        // Desaceleração linear: brake passos de full*brake/brake até full*1/brake.
        assertEquals(full * (brake + 1) / 2f, slid, 0.01f);
    }

    @Test public void anyActionCutsTheBrake() {
        Sim s = new Sim(P01);
        s.run(P01.fighter.runStartFrames + 10);
        s.in[0].direction = 0;
        s.step();
        assertTrue("freando", s.p().dashBrakeFrames > 0);
        s.in[0].light = true;
        s.step();
        assertEquals("o golpe corta a freada", 0, s.p().dashBrakeFrames);
    }

    @Test public void withoutRunDataTheRunStartsAndStopsAtOnce() {
        CharacterDefinition plain = GeneratedCharacters.get("player_two");
        assertEquals(0, plain.fighter.runStartFrames);
        assertEquals(0, plain.fighter.runBrakeFrames);
        Sim s = new Sim(plain);
        float first = s.run(1);
        assertEquals("velocidade cheia no primeiro passo", first, s.run(5), 0.001f);
        s.in[0].direction = 0;
        assertEquals("para na hora", 0f, s.step(), 0f);
    }
}
