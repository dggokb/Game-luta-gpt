package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Test;

/**
 * Ar contra ar (segundo registro real, 02/11): pulando para dentro e vendo o rival pular também, a
 * CPU disputa o ar com o golpe que sai primeiro e alcança (antes ela esperava descer e soltava o
 * jH, lento e curto, e o jM dele ganhava quase sempre).
 */
public class CpuArArTest {
    @After public void religa() { AiController.arArLigado = true; }

    @Test public void contraOJmElaDisputaOAr() {
        int[] antes = BateriaArAr.porGolpe(false, BateriaArAr.Golpe.JM, 8);
        int[] agora = BateriaArAr.porGolpe(true, BateriaArAr.Golpe.JM, 8);
        int tot = 0;
        for (int v : agora) tot += v;
        double cpuAntes = antes[2] / (double) tot, cpuAgora = agora[2] / (double) tot;
        assertTrue("CPU acerta no ar contra o jM: " + cpuAntes + " → " + cpuAgora, cpuAgora > cpuAntes + 0.12);
        assertTrue("o jM dele não entra mais: " + antes[1] + " → " + agora[1], agora[1] <= antes[1] + tot / 50);
    }
}
