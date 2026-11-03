package com.gamelutagpt;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Lutas 2x2 inteiras CPU × CPU sem erro no motor. A primeira é a da BateriaTempo (03/11) em que o
 * Ryūga no SHINKAI (PERCEPÇÃO) fez a defesa perfeita no 2H do assist do p15: a leitura desequilibrava
 * quem atacou, que era o assist, e ele ficava no golpe sem golpe (NullPointerException no quadro 5290).
 */
public class Luta2x2SemErroTest {
    private static void lutaInteira(String[] t0, String[] t1, int nivel, long semente) {
        BateriaBalanco.Luta l = new BateriaBalanco.Luta(t0, t1, nivel, semente, new CombatConfig());
        while (!l.acabou() && l.quadro < 60 * 60 * 6) l.passo();
        assertTrue("a luta terminou", l.acabou());
    }

    @Test public void aLeituraNoAssistNaoDeixaOAssistNoGolpeSemGolpe() {
        lutaInteira(new String[] {"p11", "player_base"}, new String[] {"p07", "p15"}, 3, 993038);
    }

    @Test public void duplasSorteadasVaoAteOFim() {
        java.util.Random r = new java.util.Random(77);
        String[] el = BateriaBalanco.ELENCO;
        for (int i = 0; i < 6; i++) {
            java.util.List<String> l = new java.util.ArrayList<>(java.util.Arrays.asList(el));
            java.util.Collections.shuffle(l, r);
            lutaInteira(new String[] {l.get(0), "player_base".equals(l.get(1)) ? l.get(4) : l.get(1)},
                new String[] {l.get(2), l.get(3)}, i % 2 == 0 ? 3 : 8, 5000 + i);
        }
    }
}
