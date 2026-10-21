package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Preparação do pulo (jump squat) e aterrissagem do pulo vazio: o corpo agacha antes de sair do
 * chão e assenta ao voltar, como nos jogos de luta clássicos.
 */
public class PreparacaoDoPuloTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();
    private static final int CIMA = 7;

    private static final class Sim {
        final CombatEngine engine;
        final FighterInput[] in = {new FighterInput(), new FighterInput()};

        Sim(float p2x) {
            engine = new CombatEngine(new FighterState(BASE, "P1"), new FighterState(NPC, "CPU"),
                700f, p2x, new CombatConfig());
        }

        CombatFighter p() { return engine.fighter(0); }
        CombatConfig config() { return engine.config; }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
        }

        /** Pula para cima e espera voltar ao chão (o quadro em que toca). */
        void puloVazio() {
            in[0].direction = CIMA;
            in[0].jump = true;
            step();
            in[0].direction = 0;
            for (int i = 0; i < 200 && (p().grounded || p().jumpSquat > 0); i++) step();
            for (int i = 0; i < 200 && !p().grounded; i++) step();
            assertTrue("voltou ao chão", p().grounded);
        }
    }

    @Test public void agachaAlgunsQuadrosAntesDeSairDoChao() {
        Sim s = new Sim(1900f);
        s.in[0].direction = CIMA;
        s.in[0].jump = true;
        s.step();
        for (int i = 1; i < s.config().jumpSquatFrames; i++) {
            assertTrue("ainda no chão no quadro " + i, s.p().grounded);
            assertTrue(s.p().busyOnGround());
            s.step();
        }
        s.step();
        assertFalse("saiu do chão", s.p().grounded);
    }

    @Test public void soltarODirecionalNaPreparacaoNaoMudaOPulo() {
        Sim s = new Sim(1900f);
        s.in[0].direction = 8;  // ↗
        s.in[0].jump = true;
        s.step();
        s.in[0].direction = 0;
        float x = s.p().x;
        for (int i = 0; i < 20; i++) s.step();
        assertTrue("o pulo para a frente saiu para a frente", s.p().x > x + 20f);
    }

    @Test public void apanharNaPreparacaoCancelaOPulo() {
        Sim s = new Sim(1900f);
        s.in[0].direction = CIMA;
        s.in[0].jump = true;
        s.step();
        assertTrue(s.p().jumpSquat > 0);
        // Levou um golpe no meio da preparação: o pulo não sai mais.
        s.p().status = CombatFighter.Status.HITSTUN;
        s.p().stunLeft = 20;
        s.in[0].direction = 0;
        for (int i = 0; i < 10; i++) {
            s.step();
            assertTrue("não pulou", s.p().grounded);
        }
        assertEquals(0, s.p().jumpSquat);
    }

    @Test public void oPuloVazioAssentaAntesDeOGolpeSair() {
        Sim s = new Sim(1900f);
        s.puloVazio();
        assertEquals(s.config().jumpLandingFrames, s.p().landingLag);
        s.in[0].light = true;
        s.step();
        assertEquals("o golpe espera a aterrissagem", CombatFighter.Status.NEUTRAL, s.p().status);
        for (int i = 0; i < s.config().jumpLandingFrames + 1 && !s.p().attacking(); i++) s.step();
        assertTrue("e sai do buffer logo depois", s.p().attacking());
    }

    @Test public void naAterrissagemNaoAnda() {
        Sim s = new Sim(1900f);
        s.puloVazio();
        s.in[0].direction = 1;
        float x = s.p().x;
        s.step();
        assertEquals(x, s.p().x, 0.001f);
        for (int i = 0; i < s.config().jumpLandingFrames + 2; i++) s.step();
        assertTrue("depois anda", s.p().x > x);
    }

    @Test public void golpeNoArQueAterrissaNaoTemAterrissagem() {
        Sim s = new Sim(1900f);
        s.in[0].direction = CIMA;
        s.in[0].jump = true;
        s.step();
        s.in[0].direction = 0;
        for (int i = 0; i < 200 && s.p().grounded; i++) s.step();
        // Golpe aéreo perto do chão: o pouso encerra o golpe e ele fica livre na hora.
        for (int i = 0; i < 200 && (Arena.GROUND_Y - s.p().y > 60f || s.p().vy < 0f); i++) s.step();
        s.in[0].heavy = true;
        s.step();
        assertTrue("golpe no ar", s.p().attacking());
        for (int i = 0; i < 200 && !s.p().grounded; i++) s.step();
        assertEquals(0, s.p().landingLag);
    }

    @Test public void zeroDesligaOsDois() {
        CombatConfig config = new CombatConfig();
        config.jumpSquatFrames = 0;
        config.jumpLandingFrames = 0;
        CombatEngine e = new CombatEngine(new FighterState(BASE, "P1"), new FighterState(NPC, "CPU"), 700f, 1900f, config);
        FighterInput p = new FighterInput(), cpu = new FighterInput();
        p.direction = CIMA;
        p.jump = true;
        e.step(p, cpu);
        assertFalse("pula no quadro do comando", e.fighter(0).grounded);
    }
}
