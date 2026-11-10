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

    @Test public void linhaDeAcaoDoChuteSaiDoChaoESobeAteOPe() {
        // O chute forte do Ryūga (tools/sprites/linha_de_acao.py): o pé sai de trás, perto do chão,
        // e chega à frente, no alto. O rastro segue esse caminho, não um arco inventado.
        CharacterDefinition.Animation chute = BASE.animation("HEAVY_STRAIGHT");
        assertNotNull(chute.caminhoGolpe);
        assertTrue(chute.caminhoOrdem.length >= 4);
        float[] saida = chute.caminhoGolpe[chute.caminhoOrdem[0]];
        float[] fim = chute.caminhoGolpe[chute.caminhoOrdem[chute.caminhoOrdem.length - 1]];
        assertTrue(saida[0] < 0f && saida[1] < 60f);
        assertTrue(fim[0] > 120f && fim[1] > 180f);
    }

    @Test public void semLinhaOndeAArteJaTemEfeito() {
        // O soco agachado já traz a meia-lua de ar na arte: sem rastro por cima.
        assertEquals(null, BASE.animation("CROUCH_LIGHT").caminhoGolpe);
    }

    @Test public void socoDoBrucySaiDaGuardaParaAFrente() {
        // O soco reto: o caminho é quase horizontal, na altura do ombro (não o arco de um chute).
        CharacterDefinition.Animation soco = GeneratedCharacters.get("p04").animation("MEDIUM_KICK");
        assertNotNull(soco.caminhoGolpe);
        float[] saida = soco.caminhoGolpe[soco.caminhoOrdem[0]];
        float[] fim = soco.caminhoGolpe[soco.caminhoOrdem[soco.caminhoOrdem.length - 1]];
        assertTrue(fim[0] - saida[0] > 60f);
        assertTrue(Math.abs(fim[1] - saida[1]) < 60f && fim[1] > 150f);
    }

    @Test public void quadroSemMedidaFicaReto() {
        // O quadro 0 é a guarda, antes do membro sair: sem caixa de golpe, sem inclinação.
        assertEquals(0f, angulo("HEAVY_STRAIGHT", 0), 0f);
    }
}
