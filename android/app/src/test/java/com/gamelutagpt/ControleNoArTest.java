package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Controle de lado no ar, como no FighterZ e no MvC: o pulo normal é comprometido (a direção
 * da saída fica) e o super pulo tem drift (o stick puxa aos poucos).
 */
public class ControleNoArTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();
    private static final int FRENTE = 1, TRAS = 5, CIMA = 7, CIMA_FRENTE = 8;

    private static final class Sim {
        final CombatEngine engine = new CombatEngine(new FighterState(BASE, "P1"), new FighterState(NPC, "CPU"),
            700f, 1900f, new CombatConfig());
        final FighterInput[] in = {new FighterInput(), new FighterInput()};

        CombatFighter p() { return engine.fighter(0); }
        CombatConfig config() { return engine.config; }
        /** Andar do lutador: o do bloco "movimento" dele, ou o do motor. */
        float andar() { return BASE.fighter.walkSpeed > 0f ? BASE.fighter.walkSpeed : config().walkSpeed; }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
        }

        void steps(int n) { for (int i = 0; i < n; i++) step(); }

        void pula(int direcao, boolean superPulo) {
            in[0].direction = direcao;
            if (superPulo) in[0].superJump = true; else in[0].jump = true;
            step();
            assertFalse("saiu do chão", p().grounded);
        }
    }

    private static float porQuadro(float velocidade) { return velocidade * CombatConfig.DT; }

    @Test public void puloNormalParaCimaNaoAndaSegurandoFrenteNoAr() {
        Sim s = new Sim();
        s.pula(CIMA, false);
        assertEquals(CombatFighter.PULO_COMPROMETIDO, s.p().jumpMode);
        s.steps(s.config().jumpDirectionFrames);
        float x = s.p().x;
        s.in[0].direction = FRENTE;
        s.steps(15);
        assertEquals("o pulo para cima fica para cima", x, s.p().x, 0.01f);
    }

    @Test public void puloParaFrenteNaoFreiaNemViraNoAr() {
        Sim s = new Sim();
        s.pula(CIMA_FRENTE, false);
        s.steps(4);
        float x = s.p().x;
        s.in[0].direction = TRAS;
        s.steps(10);
        assertEquals("segue para frente na velocidade de andar",
            x + 10 * porQuadro(s.andar()), s.p().x, 0.5f);
    }

    @Test public void logoDepoisDeSairDoChaoAindaDaParaEscolherADirecao() {
        Sim s = new Sim();
        s.pula(CIMA, false);
        s.in[0].direction = CIMA_FRENTE;
        s.step();
        s.in[0].direction = 0;
        float x = s.p().x;
        s.steps(10);
        assertEquals("virou pulo para frente", x + 10 * porQuadro(s.andar()), s.p().x, 0.5f);
    }

    @Test public void superPuloTemDriftQueMudaAosPoucos() {
        Sim s = new Sim();
        s.pula(CIMA, true);
        assertEquals(CombatFighter.PULO_SUPER, s.p().jumpMode);
        s.in[0].direction = FRENTE;
        float x = s.p().x;
        s.step();
        float primeiro = s.p().x - x;
        assertTrue("começa devagar: " + primeiro, primeiro > 0f && primeiro < porQuadro(s.andar()) * 0.5f);
        s.steps(30);
        x = s.p().x;
        s.step();
        assertEquals("chega na velocidade de andar", porQuadro(s.andar()), s.p().x - x, 0.01f);
    }

    @Test public void superPuloViraNoArMasNaoDeUmaVez() {
        Sim s = new Sim();
        s.pula(CIMA_FRENTE, true);
        s.in[0].direction = TRAS;
        float x = s.p().x;
        s.step();
        assertTrue("no primeiro quadro ainda vai para frente", s.p().x > x);
        s.steps(40);
        x = s.p().x;
        s.step();
        assertTrue("depois volta para trás", s.p().x < x);
    }

    @Test public void superPuloSoltoGuardaOEmbalo() {
        Sim s = new Sim();
        s.pula(CIMA_FRENTE, true);
        s.in[0].direction = 0;
        s.steps(5);
        float x = s.p().x;
        s.steps(10);
        assertEquals(x + 10 * porQuadro(s.andar()), s.p().x, 0.5f);
    }

    @Test public void apanharNoArDeixaOArLivre() {
        Sim s = new Sim();
        s.pula(CIMA_FRENTE, false);
        s.p().status = CombatFighter.Status.AIR_HITSTUN;
        s.step();
        assertEquals(CombatFighter.PULO_LIVRE, s.p().jumpMode);
    }

    @Test public void pousarDeixaOArLivre() {
        Sim s = new Sim();
        s.pula(CIMA_FRENTE, false);
        for (int i = 0; i < 200 && !s.p().grounded; i++) s.step();
        assertTrue(s.p().grounded);
        assertEquals(CombatFighter.PULO_LIVRE, s.p().jumpMode);
    }
}
