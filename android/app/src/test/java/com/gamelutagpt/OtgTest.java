package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.Arrays;
import org.junit.Test;

/**
 * OTG (12/11, como o Marvel vs. Capcom): dentro de um combo, o ↓ + M pega o rival que acabou de
 * cair e o levanta um pouco para continuar. Um por combo, para todos. Uma rasteira solta não dá
 * OTG (precisa de 2 acertos antes), e o caído não rola nem levanta rápido enquanto ele pode vir.
 */
public class OtgTest {
    private static BateriaCombos.Exec combo(String quem, String rota) {
        BateriaCombos.Cenario cn = new BateriaCombos.Cenario();
        cn.barras = 3;
        cn.gap = 100f;
        return BateriaCombos.executar(quem, "p02".equals(quem) ? "player_base" : "p02", Arrays.asList(rota.split(" ")), cn);
    }

    private static void emenda(String quem, String rota, int acertos) {
        BateriaCombos.Exec x = combo(quem, rota);
        assertTrue(quem + " " + rota + ": o combo emenda", x.valida);
        assertEquals(quem + " " + rota + ": acertos", acertos, x.acertos);
    }

    @Test public void aRasteiraDerrubaEOtgLevantaParaContinuar() {
        // 2L, M, rasteira (cai) → ↓ + M no caído (sobe) → L, M, M, S2.
        emenda("player_base", "2L M 2M 2M L M M S2", 7);
        emenda("player_base", "2L 2M 2M L M M S2", 6);
    }

    @Test public void depoisDoArcoDoShoryukenCorreEPega() {
        emenda("player_base", "L M M S2 W30 C120 2M S2", 6);
    }

    @Test public void valeParaTodos() {
        for (String id : new String[] {"p03", "p09", "p12", "p13"}) emenda(id, "2L M 2M 2M L M", 6);
        // Rasteira curta empurra para longe: um passo à frente e o ↓ + M pega.
        for (String id : new String[] {"p02", "p05"}) emenda(id, "2L M 2M C120 2M L M", 6);
    }

    @Test public void soDentroDeComboEUmaVezPorCombo() {
        // Uma rasteira sozinha não dá OTG: o rival levanta e o combo não existe.
        assertFalse("rasteira solta não abre OTG", combo("player_base", "2M 2M").valida);
        // O segundo ↓ + M no caído não levanta de novo: o combo acaba ali.
        assertFalse("um OTG por combo", combo("player_base", "2L M 2M 2M L M 2M 2M").valida);
        CombatConfig c = new CombatConfig();
        assertEquals("o OTG é o ↓ + M", "2M", c.otgMove);
        assertTrue("a janela cabe antes de levantar", c.otgWindowFrames < c.knockdownFallFrames + c.knockdownDownFrames);
    }
}
