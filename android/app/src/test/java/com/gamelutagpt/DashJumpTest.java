package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/** Pulo saído da corrida (dash jump, como no KOF): o embalo do dash leva o pulo mais longe. */
public class DashJumpTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();
    private static final int FRENTE = 1, CIMA = 7, CIMA_FRENTE = 8;

    private static final class Sim {
        final CombatEngine engine = new CombatEngine(new FighterState(BASE, "P1"), new FighterState(NPC, "CPU"),
            300f, 1900f, new CombatConfig());
        final FighterInput[] in = {new FighterInput(), new FighterInput()};

        CombatFighter p() { return engine.fighter(0); }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
        }

        void run(int frames) {
            in[0].direction = FRENTE;
            in[0].dash = true;
            step();
            for (int i = 1; i < frames; i++) step();
            assertTrue("correndo", p().forwardDashing);
        }

        /** Pula segurando {@code direcao} e devolve quanto andou para frente no ar. */
        float jump(int direcao) {
            in[0].direction = direcao;
            in[0].jump = true;
            float x = p().x;
            step();
            assertFalse("saiu do chão", p().grounded);
            for (int i = 0; i < 200 && !p().grounded; i++) step();
            assertTrue("voltou ao chão", p().grounded);
            in[0].direction = 0;
            return p().x - x;
        }
    }

    @Test public void puloDaCorridaVaiMaisLongeQueOPuloNormal() {
        Sim normal = new Sim();
        float andou = normal.jump(CIMA_FRENTE);
        assertFalse(normal.p().dashJumping);

        Sim correndo = new Sim();
        correndo.run(8);
        float longe = correndo.jump(CIMA_FRENTE);
        assertTrue("pulo da corrida " + longe + " x normal " + andou, longe > andou * 1.6f);
        assertTrue("mas não atravessa a tela: " + longe, longe < 500f);
    }

    @Test public void oEmbaloValeMesmoSoltandoAFrenteNoPulo() {
        Sim s = new Sim();
        s.run(8);
        // ↑ puro: solta a frente no mesmo quadro do pulo; o embalo do dash continua.
        float andou = s.jump(CIMA);
        float esperado = s.engine.config.walkSpeed * 42 * CombatConfig.DT;
        assertTrue("embalo de " + andou, andou > esperado * 1.6f);
    }

    @Test public void acabaAoTocarOChao() {
        Sim s = new Sim();
        s.run(8);
        s.jump(CIMA_FRENTE);
        assertFalse(s.p().dashJumping);
        assertFalse("a corrida também acabou", s.p().forwardDashing);
        // O próximo pulo, parado, é o normal.
        float andou = s.jump(CIMA_FRENTE);
        float normal = s.engine.config.walkSpeed * 44 * CombatConfig.DT;
        assertTrue("pulo normal de novo: " + andou, andou <= normal);
    }
}
