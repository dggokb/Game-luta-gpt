package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/** O AUTO dos gráficos: desce quando a luta pesa, sobe de novo quando sobra, e o teto da resolução. */
public class QualidadeGraficaTest {
    private static void janelas(QualidadeGrafica q, int n, float fps) {
        for (int i = 0; i < n; i++) q.janela(fps, 1f);
    }

    @Test public void autoDropsAfterTwoHeavyWindows() {
        QualidadeGrafica q = new QualidadeGrafica(null);
        assertEquals(QualidadeGrafica.ALTO, q.nivel());
        assertFalse(q.janela(40f, 1f));
        assertTrue("duas janelas ruins seguidas", q.janela(40f, 1f));
        assertEquals(QualidadeGrafica.MEDIO, q.nivel());
        assertFalse("fora da luta não conta", q.janela(20f, 0.5f));
    }

    /** O nível guardado podia ser de um aperto passageiro (ou de uma versão mais pesada) e ficava para sempre. */
    @Test public void autoClimbsBackWhenTheFightRunsWithRoom() {
        QualidadeGrafica q = new QualidadeGrafica(null);
        janelas(q, 4, 40f);
        assertEquals(QualidadeGrafica.LEVE, q.nivel());
        janelas(q, QualidadeGrafica.JANELAS_PARA_SUBIR - 1, 60f);
        assertEquals("ainda não", QualidadeGrafica.LEVE, q.nivel());
        assertTrue(q.janela(60f, 1f));
        assertEquals(QualidadeGrafica.MEDIO, q.nivel());
        janelas(q, QualidadeGrafica.JANELAS_PARA_SUBIR, 60f);
        assertEquals(QualidadeGrafica.ALTO, q.nivel());
        janelas(q, 40, 60f);
        assertEquals("o ALTO é o teto", QualidadeGrafica.ALTO, q.nivel());
    }

    @Test public void aClimbThatFailsIsNotTriedAgain() {
        QualidadeGrafica q = new QualidadeGrafica(null);
        janelas(q, 2, 40f);
        assertEquals(QualidadeGrafica.MEDIO, q.nivel());
        janelas(q, QualidadeGrafica.JANELAS_PARA_SUBIR, 60f);
        assertEquals(QualidadeGrafica.ALTO, q.nivel());
        janelas(q, 2, 40f);   // subiu e logo não aguentou
        assertEquals(QualidadeGrafica.MEDIO, q.nivel());
        janelas(q, 3 * QualidadeGrafica.JANELAS_PARA_SUBIR, 60f);
        assertEquals("sem ficar indo e voltando", QualidadeGrafica.MEDIO, q.nivel());
        q.proximoModo(1);   // ALTO fixo
        q.proximoModo(-1);  // de volta ao AUTO: recomeça do ALTO
        assertEquals(QualidadeGrafica.ALTO, q.nivel());
    }

    @Test public void resolutionHasACeilingOf1080Lines() {
        assertEquals("tela 720p: inteira", 1f, QualidadeGrafica.escalaDaTela(1f, 720), 0f);
        assertEquals("1080p: inteira", 1f, QualidadeGrafica.escalaDaTela(1f, 1080), 0f);
        assertEquals("1440p: desenha em 1080", 0.75f, QualidadeGrafica.escalaDaTela(1f, 1440), 0.001f);
        assertEquals("o LEVE é proporcional ao teto", 0.45f,
            QualidadeGrafica.escalaDaTela(QualidadeGrafica.ESCALA[QualidadeGrafica.LEVE], 1440), 0.001f);
    }
}
