package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/** A finalização de mangá: quais golpes ganham, o pedido esperando, a força e o fim. */
public class FinalizacaoMangaTest {
    @Test public void onlySuperAndUltraFinish() {
        assertEquals(FinalizacaoManga.ULTRA, FinalizacaoManga.tipoDoGolpe("ULTRA", AttackDefinition.Strength.SUPER));
        assertEquals(FinalizacaoManga.SUPER, FinalizacaoManga.tipoDoGolpe("SUPER", AttackDefinition.Strength.SUPER));
        assertEquals("Super de time / projétil do Super",
            FinalizacaoManga.SUPER, FinalizacaoManga.tipoDoGolpe("DHC", AttackDefinition.Strength.SUPER));
        assertEquals(FinalizacaoManga.NENHUM, FinalizacaoManga.tipoDoGolpe("cH", AttackDefinition.Strength.HEAVY));
        assertEquals(FinalizacaoManga.NENHUM, FinalizacaoManga.tipoDoGolpe("S", AttackDefinition.Strength.SPECIAL));
        assertEquals(FinalizacaoManga.NENHUM, FinalizacaoManga.tipoDoGolpe("THROW", null));
    }

    @Test public void theRequestWaitsThenRunsInRealTimeAndEnds() {
        FinalizacaoManga f = new FinalizacaoManga();
        f.pedir(FinalizacaoManga.NENHUM, 1, 0);
        assertFalse("golpe comum não pede", f.pendente());

        f.pedir(FinalizacaoManga.SUPER, 1, 0xFFFF0000);
        assertTrue(f.pendente());
        f.atualizar(1f);
        assertFalse("esperando, não anda", f.ativo());
        assertEquals(0f, f.forca(), 0f);

        f.comecar();
        assertTrue(f.ativo());
        assertFalse(f.pendente());
        assertEquals(1, f.vitima());
        assertEquals(0f, f.forca(), 0f);
        f.atualizar(FinalizacaoManga.ENTRADA);
        assertEquals("cobre o cenário depois da entrada", 1f, f.forca(), 0.001f);
        int antes = f.semente();
        f.atualizar(FinalizacaoManga.TROCA);
        assertNotEquals("as linhas fervem", antes, f.semente());
        f.atualizar(FinalizacaoManga.CHEIO);
        float saindo = f.forca();
        assertTrue(saindo < 1f && saindo > 0f);
        f.atualizar(FinalizacaoManga.SAIDA);
        assertFalse(f.ativo());
        assertEquals(0f, f.forca(), 0f);
    }

    @Test public void ultraWinsOverSuperAndClearResets() {
        FinalizacaoManga f = new FinalizacaoManga();
        f.pedir(FinalizacaoManga.ULTRA, 0, 0xFF00FF00);
        f.pedir(FinalizacaoManga.SUPER, 1, 0xFFFF0000);
        f.comecar();
        assertEquals(FinalizacaoManga.ULTRA, f.tipo());
        assertEquals(0, f.vitima());
        assertEquals(0xFF00FF00, f.cor());
        f.limpar();
        assertFalse(f.ativo());
        assertFalse(f.pendente());
        assertEquals(-1, f.vitima());
    }
}
