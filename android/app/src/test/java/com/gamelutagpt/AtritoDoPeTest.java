package com.gamelutagpt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * O atrito do pé no chão: andando, o corpo anda o que o pé de apoio empurra no desenho
 * (CharacterDefinition.Animation.apoio, medido por tools/sprites/apoio.py), e a velocidade de
 * andar do lutador dá o ritmo da passada.
 */
public class AtritoDoPeTest {
    private static final CharacterDefinition YINLU = GeneratedCharacters.get("p02");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();
    private static final int FRENTE = 1, TRAS = 5;

    private static CombatEngineTest.Sim andando(int direcao, float x) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(YINLU, NPC, x, 1700f);
        s.in[0].direction = direcao;
        return s;
    }

    @Test public void oPeDeApoioFicaCravadoNoMundo() {
        for (int direcao : new int[]{FRENTE, TRAS}) {
            CharacterDefinition.Animation a = YINLU.animation(direcao == FRENTE ? SpriteStates.WALK_FORWARD : SpriteStates.WALK_BACK);
            assertNotNull("a Yin-lu tem o pé de apoio medido", a.apoio);
            CombatEngineTest.Sim s = andando(direcao, 900f);
            int quadro = 0, voltas = 0;
            float antes = 900f;
            for (int i = 0; i < 90; i++) {
                s.step();
                CombatFighter f = s.f(0);
                assertTrue("andando pelo pé", f.passada >= 0f);
                int agora = (int) f.passada;
                if (agora < quadro % a.apoio.length) voltas++;
                int contado = voltas * a.apoio.length + agora;
                // O corpo andou exatamente o que o pé de apoio empurrou entre o desenho de antes
                // e o de agora: o pé não saiu do lugar no chão.
                assertEquals("quadro " + i, a.avanco(quadro, contado), (f.x - antes) * f.facing, 0.01f);
                quadro = contado;
                antes = f.x;
            }
        }
    }

    @Test public void aVelocidadeDeAndarDoLutadorDaORitmoDaPassada() {
        CombatEngineTest.Sim frente = andando(FRENTE, 500f);
        CombatEngineTest.Sim tras = andando(TRAS, 1200f);
        frente.steps(120);
        tras.steps(120);
        float passoFrente = Math.abs(YINLU.animation(SpriteStates.WALK_FORWARD).passo);
        float passoTras = Math.abs(YINLU.animation(SpriteStates.WALK_BACK).passo);
        // Em 2 s anda a velocidade dela, com a folga de um passo (o corpo vai aos passos).
        assertEquals(YINLU.fighter.walkSpeed * 2f, frente.f(0).x - 500f, passoFrente * 0.5f);
        assertEquals(YINLU.fighter.walkBackSpeed * 2f, 1200f - tras.f(0).x, passoTras * 0.5f);
    }

    @Test public void pararDeAndarAcabaAPassada() {
        CombatEngineTest.Sim s = andando(FRENTE, 500f);
        s.steps(10);
        assertTrue(s.f(0).passada >= 0f);
        s.in[0].direction = 0;
        s.step();
        assertEquals(-1f, s.f(0).passada, 0f);
        float x = s.f(0).x;
        s.steps(5);
        assertEquals("parado não escorrega", x, s.f(0).x, 0f);
    }

    /** Corre até o embalo valer e aperta o golpe; devolve o quanto andou do aperto até parar. */
    private static float golpeNaCorrida(CharacterDefinition quem, boolean medio) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(quem, NPC, 300f, 1800f);
        s.in[0].direction = FRENTE;
        s.in[0].dash = true;
        s.steps(12);
        assertTrue("correndo", s.f(0).forwardDashing);
        float x = s.f(0).x;
        s.in[0].direction = 0;
        if (medio) s.in[0].medium = true; else s.in[0].light = true;
        s.step();
        assertTrue("o golpe saiu", s.f(0).attacking());
        s.until(() -> s.f(0).embalo == 0f && !s.f(0).attacking(), 90);
        return s.f(0).x - x;
    }

    @Test public void oGolpeDadoNaCorridaLevaOEmbaloEOAtritoFreia() {
        CombatConfig c = new CombatConfig();
        for (CharacterDefinition quem : new CharacterDefinition[]{YINLU, GeneratedCharacters.get("player_base")}) {
            // v² / (2 × atrito × gravidade): o quanto desliza até parar, igual para qualquer golpe.
            float corrida = c.dashSpeed * quem.fighter.dashPermille / 1000f;
            float desliza = corrida * corrida / (2f * c.atritoDoChao * c.gravity);
            float leve = golpeNaCorrida(quem, false), medio = golpeNaCorrida(quem, true);
            assertEquals(quem.id + " L", desliza, leve, 12f);
            assertEquals(quem.id + " M: o mesmo deslize, sem número por golpe", leve, medio, 1f);
        }
    }

    @Test public void oGolpeDadoParadoNaoDesliza() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(YINLU, NPC, 300f, 1800f);
        s.in[0].medium = true;
        s.step();
        assertTrue(s.f(0).attacking());
        s.until(() -> !s.f(0).attacking(), 60);
        assertEquals(300f, s.f(0).x, 0.01f);
    }

    @Test public void oEmbaloParaAoEncostarNoRival() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(YINLU, NPC, 300f, 620f);
        s.in[0].direction = FRENTE;
        s.in[0].dash = true;
        s.until(() -> s.f(1).x - s.f(0).x <= 200f, 60);
        s.in[0].direction = 0;
        s.in[0].light = true;
        s.step();
        float rival = s.f(1).x;
        s.until(() -> s.f(0).embalo == 0f, 60);
        assertTrue("não atravessa nem empurra o rival pela arena: " + (s.f(1).x - rival), s.f(1).x - rival < 60f);
        assertTrue("fica na frente dele", s.f(0).x < s.f(1).x);
    }
}
