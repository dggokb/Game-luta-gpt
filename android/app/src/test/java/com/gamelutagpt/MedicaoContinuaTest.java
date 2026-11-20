package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;

/**
 * O botão MEDIR (20/11): mede cada luta separada pelas duplas e numera na ordem em que foram
 * jogadas, a de agora marcada com ">".
 */
public class MedicaoContinuaTest {
    @Test public void separaAsLutasPelasDuplas() throws Exception {
        MedicaoContinua m = new MedicaoContinua();
        PerfilQuadros p = new PerfilQuadros();
        for (int i = 0; i < 40; i++) { m.quadro(p, "p03+p02 x player_base+p04", 0); Thread.sleep(1); }
        for (int i = 0; i < 40; i++) { m.quadro(p, "p03+p02 x p13+p14", 0); Thread.sleep(1); }
        List<String> l = m.linhas();
        assertEquals(3, l.size());
        assertTrue(l.get(1), l.get(1).startsWith("  luta 1 p03+p02 x player_base+p04: "));
        assertTrue(l.get(2), l.get(2).startsWith("> luta 2 p03+p02 x p13+p14: "));
        assertTrue(l.get(2), l.get(2).contains(" fps, quadro "));
    }
}
