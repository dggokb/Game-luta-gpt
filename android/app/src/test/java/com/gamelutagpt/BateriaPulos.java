package com.gamelutagpt;

import java.util.*;

/**
 * Quantas vezes a CPU pula numa luta normal (pedido do jogador, 02/11). CPU × CPU, 1x1, os 16 todos
 * contra todos (cada par dos dois lados), no mesmo nível dos dois lados. Por pulo: tirado do ar
 * (apanhou no ar), acertou (golpe dele entrou no ar), defendido, ou nada.
 *
 * <pre>args: [níveis...] (padrão 5 8)</pre>
 */
public final class BateriaPulos {
    private BateriaPulos() {}

    static final class Est {
        int lutas, pulos, superPulos, tirado, acertou, defendido;
        long quadros;
        final Map<String, int[]> porLutador = new TreeMap<>();   // pulos, quadros em luta (/60)
    }

    static void luta(String a, String b, int nivel, long semente, Est e) {
        BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {a}, new String[] {b}, nivel, semente, new CombatConfig());
        boolean[] noAr = new boolean[2];
        int[] resultado = new int[2];   // 0 nada, 1 defendido, 2 acertou, 3 tirado
        String[] quem = {a, b};
        while (!l.acabou() && l.quadro < 60 * 60 * 4) {
            l.passo();
            for (int s = 0; s < 2; s++) {
                CombatFighter f = l.f(s);
                boolean ar = !f.grounded;
                if (ar && !noAr[s] && f.status == CombatFighter.Status.NEUTRAL) {
                    e.pulos++;
                    if (f.superJumping) e.superPulos++;
                    e.porLutador.computeIfAbsent(quem[s], k -> new int[2])[0]++;
                    resultado[s] = 0;
                    noAr[s] = true;
                } else if (!ar && noAr[s]) {
                    if (resultado[s] == 3) e.tirado++;
                    else if (resultado[s] == 2) e.acertou++;
                    else if (resultado[s] == 1) e.defendido++;
                    noAr[s] = false;
                }
            }
            for (CombatEngine.HitEvent h : l.e.events()) {
                if (noAr[h.attacker] && !l.f(h.attacker).grounded) resultado[h.attacker] = Math.max(resultado[h.attacker], h.blocked ? 1 : 2);
                if (noAr[h.defender] && !h.blocked && resultado[h.defender] < 2) resultado[h.defender] = 3;
            }
        }
        e.lutas++;
        e.quadros += l.quadro;
        for (int s = 0; s < 2; s++) e.porLutador.computeIfAbsent(quem[s], k -> new int[2])[1] += l.quadro;
    }

    public static void main(String[] args) {
        int[] niveis = args.length == 0 ? new int[] {5, 8} : Arrays.stream(args).mapToInt(Integer::parseInt).toArray();
        System.out.println("# Quantas vezes a CPU pula (CPU × CPU, 1x1, os 16 todos contra todos, 240 lutas por nível, cada par dos dois lados)");
        System.out.println();
        System.out.println("| nível | pulos por luta (cada lado) | pulos por minuto | super pulos | duração da luta | tirado do ar | acertou | defendido | nada |");
        System.out.println("|---|---|---|---|---|---|---|---|---|");
        Map<Integer, Est> todos = new LinkedHashMap<>();
        for (int nivel : niveis) {
            Est e = new Est();
            int k = 0;
            for (String a : BateriaBalanco.ELENCO) for (String b : BateriaBalanco.ELENCO) {
                if (a.equals(b)) continue;
                luta(a, b, nivel, 990_000 + k++, e);
            }
            todos.put(nivel, e);
            double min = e.quadros / 3600.0;
            System.out.printf(Locale.ROOT, "| %d | %.1f | %.1f | %.0f%% | %.0f s | %.0f%% | %.0f%% | %.0f%% | %.0f%% |%n", nivel,
                e.pulos / (2.0 * e.lutas), e.pulos / (2.0 * min), 100.0 * e.superPulos / Math.max(1, e.pulos),
                e.quadros / 60.0 / e.lutas, 100.0 * e.tirado / Math.max(1, e.pulos), 100.0 * e.acertou / Math.max(1, e.pulos),
                100.0 * e.defendido / Math.max(1, e.pulos),
                100.0 * (e.pulos - e.tirado - e.acertou - e.defendido) / Math.max(1, e.pulos));
        }
        System.out.println();
        System.out.println("## Pulos por minuto, por lutador");
        System.out.println();
        StringBuilder cab = new StringBuilder("| lutador |"), sep = new StringBuilder("|---|");
        for (int n : todos.keySet()) { cab.append(" nível ").append(n).append(" |"); sep.append("---|"); }
        System.out.println(cab);
        System.out.println(sep);
        for (String id : BateriaBalanco.ELENCO) {
            StringBuilder b = new StringBuilder("| " + id + " |");
            for (Est e : todos.values()) {
                int[] v = e.porLutador.get(id);
                b.append(String.format(Locale.ROOT, " %.1f |", v == null ? 0 : v[0] / (v[1] / 3600.0)));
            }
            System.out.println(b);
        }
    }
}
