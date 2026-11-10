package com.gamelutagpt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * A inclinação do membro que bate, medida no desenho (tools/sprites/angulo_golpe.py), que a faísca
 * de acerto segue: o chute alto do player_base sobe, o soco reto fica reto, o chute agachado desce.
 */
public class AnguloDoGolpeTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");

    private static float angulo(String animacao, int quadro) {
        float[] a = BASE.animation(animacao).anguloGolpe;
        assertNotNull(animacao + " sem a medida", a);
        return a[quadro];
    }

    @Test public void chuteAltoSobe() {
        assertTrue(angulo("HEAVY_STRAIGHT", 5) > 15f);
    }

    @Test public void socoRetoFicaReto() {
        assertEquals(0f, angulo("LIGHT_JAB", 6), 6f);
    }

    @Test public void chuteAgachadoDesce() {
        assertTrue(angulo("CROUCH_MEDIUM", 9) < -10f);
    }

    @Test public void rastroSoOndeAArteNaoTemEfeito() {
        // O chute alto não tem efeito desenhado: a ponta do pé, à frente e acima da cintura, subindo.
        float[] pe = BASE.animation("HEAVY_STRAIGHT").rastroGolpe[5];
        assertNotNull(pe);
        assertTrue(pe[0] > 120f && pe[1] > 150f && pe[2] > 15f);
        // ... com a perna inteira de raio e o arco de quem subiu até ali.
        assertTrue(pe[3] > 100f);
        assertEquals(1f, pe[4], 0f);
        // O jab é curto e reto: sem arco, só a onda na ponta.
        assertEquals(0f, BASE.animation("LIGHT_JAB").rastroGolpe[6][4], 0f);
        // O soco agachado já traz a meia-lua de ar na arte: sem rastro por cima.
        assertEquals(null, BASE.animation("CROUCH_LIGHT").rastroGolpe);
    }

    @Test public void socoRetoNaoTemArcoDeChute() {
        // O Brucy (p04) é boxeador: o soco médio e o forte são retos (a onda na linha do braço),
        // não o arco de quem sobe a perna do chão.
        CharacterDefinition brucy = GeneratedCharacters.get("p04");
        for (String golpe : new String[] {"MEDIUM_KICK", "HEAVY_STRAIGHT"}) {
            int com = 0;
            for (float[] r : brucy.animation(golpe).rastroGolpe) {
                if (r == null || r[1] < 150f) continue;   // só o braço esticado na altura do ombro
                assertEquals(golpe, 0f, r[4], 0f);
                com++;
            }
            assertTrue(golpe, com > 0);
        }
    }

    @Test public void quadroSemMedidaFicaReto() {
        // O quadro 0 é a guarda, antes do membro sair: sem caixa de golpe, sem inclinação.
        assertEquals(0f, angulo("HEAVY_STRAIGHT", 0), 0f);
    }
}
