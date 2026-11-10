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

    @Test public void quadroSemMedidaFicaReto() {
        // O quadro 0 é a guarda, antes do membro sair: sem caixa de golpe, sem inclinação.
        assertEquals(0f, angulo("HEAVY_STRAIGHT", 0), 0f);
    }
}
