package com.gamelutagpt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** A leitura da CPU (CpuLeitura): o hábito que se repete vira certeza, esquece aos poucos e passa de luta. */
public class CpuLeituraTest {
    private static final CpuLeitura.Situacao DEFESA = CpuLeitura.Situacao.DEPOIS_DE_DEFENDER;

    @Test public void semObservacaoNaoPreveNada() {
        CpuLeitura l = new CpuLeitura();
        assertNull(l.previsto(DEFESA));
        assertEquals(0.0, l.certeza(DEFESA), 0.0);
    }

    @Test public void oHabitoQueSeRepeteViraCerteza() {
        CpuLeitura l = new CpuLeitura();
        l.observa(DEFESA, CpuLeitura.Resposta.PULA);
        double uma = l.certeza(DEFESA);
        for (int i = 0; i < 5; i++) l.observa(DEFESA, CpuLeitura.Resposta.PULA);
        assertEquals(CpuLeitura.Resposta.PULA, l.previsto(DEFESA));
        assertTrue("mais vezes, mais certeza: " + uma + " → " + l.certeza(DEFESA), l.certeza(DEFESA) > uma);
        assertTrue(l.certeza(DEFESA) > 0.6);
    }

    @Test public void quemVariaNaoTemPadrao() {
        CpuLeitura l = new CpuLeitura();
        for (int i = 0; i < 4; i++) for (CpuLeitura.Resposta r : CpuLeitura.Resposta.values()) l.observa(DEFESA, r);
        assertTrue("sem padrão: " + l.certeza(DEFESA), l.certeza(DEFESA) < 0.1);
    }

    @Test public void oHabitoNovoTomaOLugarDoVelho() {
        CpuLeitura l = new CpuLeitura();
        for (int i = 0; i < 6; i++) l.observa(DEFESA, CpuLeitura.Resposta.PULA);
        for (int i = 0; i < 8; i++) l.observa(DEFESA, CpuLeitura.Resposta.ATACA);
        assertEquals(CpuLeitura.Resposta.ATACA, l.previsto(DEFESA));
    }

    @Test public void aAbordagemPunidaPesaMenosSemSumir() {
        CpuLeitura l = new CpuLeitura();
        assertEquals(1.0, l.fator(CpuLeitura.Abordagem.PULA), 1e-9);
        for (int i = 0; i < 10; i++) l.falhou(CpuLeitura.Abordagem.PULA);
        assertEquals("nunca abaixo do piso: o estilo continua", CpuLeitura.FATOR_MIN, l.fator(CpuLeitura.Abordagem.PULA), 1e-9);
        for (int i = 0; i < 10; i++) l.deuCerto(CpuLeitura.Abordagem.CORRE);
        assertEquals("o que funcionou não passa do estilo", CpuLeitura.FATOR_MAX, l.fator(CpuLeitura.Abordagem.CORRE), 1e-9);
    }

    @Test public void aMemoriaPassaDeLutaMaisFraca() {
        CpuLeitura l = new CpuLeitura();
        for (int i = 0; i < 6; i++) l.observa(DEFESA, CpuLeitura.Resposta.PULA);
        double antes = l.observacoes(DEFESA);
        l.novaLuta();
        assertEquals(CpuLeitura.Resposta.PULA, l.previsto(DEFESA));
        assertEquals(antes * CpuLeitura.ENTRE_LUTAS, l.observacoes(DEFESA), 1e-9);
        l.esquecerTudo();
        assertNull(l.previsto(DEFESA));
    }

    @Test public void oPlanoContraABolaValeOQueRendeuPorUltimo() {
        CpuLeitura l = new CpuLeitura();
        CpuLeitura.PlanoBola pula = CpuLeitura.PlanoBola.PULA;
        assertEquals(0.0, l.valor(pula), 0.0);
        for (int i = 0; i < 4; i++) l.resultado(pula, -1.0);
        assertEquals(-1.0, l.valor(pula), 1e-9);
        l.resultado(pula, 1.0);
        double depois = l.valor(pula);
        assertTrue("o resultado novo puxa: " + depois, depois > -1.0 && depois < 0.0);
        assertEquals(0.0, l.vezes(CpuLeitura.PlanoBola.DEFENDE), 0.0);
        // Luta nova: o valor fica, a confiança cai (vale tentar de novo).
        double vezes = l.vezes(pula);
        l.novaLuta();
        assertEquals(depois, l.valor(pula), 1e-9);
        assertEquals(vezes * CpuLeitura.ENTRE_LUTAS, l.vezes(pula), 1e-9);
        l.esquecerTudo();
        assertEquals(0.0, l.valor(pula), 0.0);
        assertEquals(0.0, l.vezes(pula), 0.0);
    }

    @Test public void cadaFaixaAprendeSeparado() {
        CpuLeitura l = new CpuLeitura();
        l.resultado(CpuLeitura.Faixa.PERTO, CpuLeitura.PlanoMeio.ESPERA, 1.0);
        l.resultado(CpuLeitura.Faixa.MEIO, CpuLeitura.PlanoMeio.ESPERA, -1.0);
        assertEquals(1.0, l.valor(CpuLeitura.Faixa.PERTO, CpuLeitura.PlanoMeio.ESPERA), 1e-9);
        assertEquals(-1.0, l.valor(CpuLeitura.Faixa.MEIO, CpuLeitura.PlanoMeio.ESPERA), 1e-9);
        assertTrue(l.resumo().contains("perto:espera=+1.00"));
    }
}
