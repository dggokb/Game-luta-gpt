package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Test;

/**
 * A CPU levanta (e sai do atordoamento) defendendo, e troca para a defesa baixa quando o rival cai
 * do pulo: o laço da luta real (rasteira, rasteira ou jM no levantar, rasteira depois do jM) entra
 * bem menos do que entrava (registro de 01/11; docs/balanco/registro-luta.md).
 */
public class CpuLevantarTest {
    @After public void religa() {
        AiController.saidaLigada = true;
        AiController.trocaBaixaLigada = true;
    }

    static double[] laco(boolean ligado) {
        AiController.saidaLigada = ligado;
        AiController.trocaBaixaLigada = ligado;
        BateriaLevantar.Est[] porLuta = new BateriaLevantar.Est[BateriaVoadora.NIVEIS.length];
        for (int i = 0; i < porLuta.length; i++) porLuta[i] = new BateriaLevantar.Est();
        for (int k = 0; k < 8; k++) BateriaLevantar.arcade(k, porLuta);
        int meaty = 0, meatyAcerta = 0, depois = 0, depoisAcerta = 0, levantou = 0, defendendo = 0;
        for (BateriaLevantar.Est e : porLuta) {
            meaty += e.meaty2MAcerta + e.meaty2MDefende + e.meatyJMAcerta + e.meatyJMDefende;
            meatyAcerta += e.meaty2MAcerta + e.meatyJMAcerta;
            depois += e.jmDepois2M;
            depoisAcerta += e.jmDepois2MAcerta;
            levantou += e.levantou;
            defendendo += e.levantouDefendendo;
        }
        return new double[] {meatyAcerta / (double) meaty, depoisAcerta / (double) depois, defendendo / (double) levantou};
    }

    @Test public void oLacoDoLevantarEntraMenos() {
        double[] antes = laco(false), agora = laco(true);
        assertTrue("levanta defendendo: " + antes[2] + " → " + agora[2], agora[2] > antes[2] + 0.12);
        assertTrue("golpe em cima da hora no levantar: " + antes[0] + " → " + agora[0], agora[0] < antes[0] - 0.12);
        assertTrue("rasteira depois do jM: " + antes[1] + " → " + agora[1], agora[1] < antes[1] - 0.08);
    }
}
