package com.gamelutagpt;

import java.util.*;

/**
 * Combos de mais de 8 golpes (pedido do jogador, 02/11: "simula quantas vezes aparecem mais de 8
 * combos"). CPU × CPU, 1x1, os 16 todos contra todos (cada par dos dois lados), por nível. Um combo
 * é a sequência de acertos sem o rival sair do atordoamento (ou da queda logo depois dele).
 *
 * <pre>args: [níveis...] (padrão 5 8)</pre>
 */
public final class BateriaCombosLongos {
    private BateriaCombosLongos() {}

    static final class Est {
        int lutas, combos, longos, longosSemSuper, maior;
        long quadros;
        final int[] faixa = new int[5];   // 1-2, 3-5, 6-8, 9-12, 13+
        final Map<String, Integer> porLutador = new TreeMap<>();
    }

    static boolean preso(CombatFighter f) {
        return f.inHitstun() || f.status == CombatFighter.Status.KNOCKDOWN && f.knockdownFrame < 4 ||
            f.status == CombatFighter.Status.THROWN;
    }

    static void luta(String a, String b, int nivel, long semente, Est e) {
        BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {a}, new String[] {b}, nivel, semente, new CombatConfig());
        String[] quem = {a, b};
        int[] n = new int[2];
        boolean[] sup = new boolean[2];
        while (!l.acabou() && l.quadro < 60 * 60 * 4) {
            boolean[] antes = {preso(l.f(0)), preso(l.f(1))};
            l.passo();
            for (CombatEngine.HitEvent h : l.e.events()) {
                if (h.blocked || h.attacker == h.defender) continue;
                int s = h.attacker;
                if (!antes[h.defender] && n[s] > 0) fechar(e, n[s], sup[s], quem[s]);
                if (!antes[h.defender]) { n[s] = 0; sup[s] = false; }
                n[s]++;
                if ("SUPER".equals(h.moveId) || "ULTRA".equals(h.moveId)) sup[s] = true;
                antes[h.defender] = true;
            }
            for (int s = 0; s < 2; s++) {
                if (n[s] > 0 && !preso(l.f(1 - s))) {
                    fechar(e, n[s], sup[s], quem[s]);
                    n[s] = 0;
                    sup[s] = false;
                }
            }
        }
        e.lutas++;
        e.quadros += l.quadro;
    }

    static void fechar(Est e, int n, boolean sup, String quem) {
        e.combos++;
        e.maior = Math.max(e.maior, n);
        e.faixa[n <= 2 ? 0 : n <= 5 ? 1 : n <= 8 ? 2 : n <= 12 ? 3 : 4]++;
        if (n > 8) {
            e.longos++;
            if (!sup) e.longosSemSuper++;
            e.porLutador.merge(quem, 1, Integer::sum);
        }
    }

    public static void main(String[] args) {
        int[] niveis = args.length == 0 ? new int[] {5, 8} : Arrays.stream(args).mapToInt(Integer::parseInt).toArray();
        System.out.println("# Combos de mais de 8 golpes (CPU × CPU, 1x1, os 16 todos contra todos, 240 lutas por nível)");
        System.out.println();
        System.out.println("| nível | combos por luta | 1–2 / 3–5 / 6–8 / 9–12 / 13+ golpes | mais de 8 por luta | lutas com algum | sem Super nem Ultra | maior |");
        System.out.println("|---|---|---|---|---|---|---|");
        Map<Integer, Est> todos = new LinkedHashMap<>();
        for (int nivel : niveis) {
            Est e = new Est();
            int k = 0, comLongo = 0;
            for (String a : BateriaBalanco.ELENCO) for (String b : BateriaBalanco.ELENCO) {
                if (a.equals(b)) continue;
                int antes = e.longos;
                luta(a, b, nivel, 990_500 + k++, e);
                if (e.longos > antes) comLongo++;
            }
            todos.put(nivel, e);
            int c = Math.max(1, e.combos);
            System.out.printf(Locale.ROOT, "| %d | %.1f | %s / %s / %s / %s / %s | %.2f | %s | %s | %d |%n", nivel, e.combos / (double) e.lutas,
                BateriaVoadora.pc(e.faixa[0], c), BateriaVoadora.pc(e.faixa[1], c), BateriaVoadora.pc(e.faixa[2], c),
                BateriaVoadora.pc(e.faixa[3], c), BateriaVoadora.pc(e.faixa[4], c), e.longos / (double) e.lutas,
                BateriaVoadora.pc(comLongo, e.lutas), BateriaVoadora.pc(e.longosSemSuper, Math.max(1, e.longos)), e.maior);
        }
        System.out.println();
        System.out.println("## Quem faz os combos de mais de 8 (soma dos níveis)");
        System.out.println();
        Map<String, Integer> soma = new TreeMap<>();
        for (Est e : todos.values()) e.porLutador.forEach((k, v) -> soma.merge(k, v, Integer::sum));
        soma.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue())
            .forEach(en -> System.out.println("- " + en.getKey() + ": " + en.getValue()));
    }
}
