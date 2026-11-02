package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/** O registro de luta: a linha do tempo sai completa e o resumo lê o mesmo formato. */
public class RegistroLutaTest {
    static String lutaGravada(String a, String b, int nivel, long semente) {
        BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {a}, new String[] {b}, nivel, semente, new CombatConfig());
        RegistroLuta r = new RegistroLuta();
        r.comecar("teste | CPU nível " + nivel + " | JOG: " + a + " | CPU: " + b);
        while (!l.acabou() && l.quadro < 60 * 60 * 4) {
            l.passo();
            r.quadro(l.e, l.flow, l.in[0], l.ai[1].lastReason);
        }
        return r.exportar("Registro de teste");
    }

    @Test public void aLinhaDoTempoTemMovimentosGolpesDecisoesEOFim() {
        String t = lutaGravada("player_base", "p03", 8, 11);
        assertTrue(t.contains("=== LUTA teste"));
        assertTrue("movimentos", t.contains("anda para frente") || t.contains("corre"));
        assertTrue("golpes", t.contains("golpe "));
        assertTrue("acertos", t.contains("ACERTA"));
        assertTrue("o que a CPU pensa", t.contains("CPU  pensa: "));
        assertTrue("entradas do jogador", t.contains("JOG  direção") || t.contains("JOG  aperta"));
        assertTrue("vidas a cada segundo", t.contains("~  vidas"));
        assertTrue("o fim", t.contains("=== FIM: "));
        ResumoRegistro.Luta[] lutas = ResumoRegistro.ler(t);
        assertEquals(1, lutas.length);
        assertTrue(lutas[0].pulos[0] + lutas[0].pulos[1] > 0);
    }

    @Test public void guardaAsUltimasLutas() {
        RegistroLuta r = new RegistroLuta();
        for (int i = 0; i < RegistroLuta.LUTAS_GUARDADAS + 3; i++) {
            r.comecar("luta " + i);
            BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {"p02"}, new String[] {"p09"}, 5, 100 + i, new CombatConfig());
            while (!l.acabou() && l.quadro < 60 * 60 * 4) {
                l.passo();
                r.quadro(l.e, l.flow, l.in[0], l.ai[1].lastReason);
            }
        }
        assertEquals(RegistroLuta.LUTAS_GUARDADAS, r.lutas());
        String t = r.exportar("x");
        assertFalse(t.contains("=== LUTA luta 0\n"));
        assertTrue(t.contains("=== LUTA luta " + (RegistroLuta.LUTAS_GUARDADAS + 2)));
    }
}
