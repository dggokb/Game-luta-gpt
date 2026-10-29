package com.gamelutagpt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Combo Breaker com outras regras, medido antes de mudar o jogo: a separação depois da quebra
 * ({@code comboBreakerPushGap}) e o atordoamento de quem batia ({@code comboBreakerStunFrames}),
 * com as mesmas sementes e cenários da etapa 1 (o grupo breaker da {@link BateriaAntesDepois}) e
 * mais o troco por golpe, por personagem, a troca com o atacante e o canto. Não é teste da suíte:
 * roda à mão.
 *
 * <pre>java -cp ... com.gamelutagpt.BateriaQuebra [grupo|golpes|troca|canto|tudo]</pre>
 */
public final class BateriaQuebra {
    private BateriaQuebra() {}

    /** Nome, separação (px) e atordoamento (quadros). C é a regra de hoje (etapa 1). */
    static String[][] OPCOES = {
        {"C: hoje (170 px, 20 q)", "170", "20"},
        {"A: 320 px, 20 q", "320", "20"},
        {"B: 170 px, 4 q", "170", "4"},
    };

    static final String[] ELENCO = BateriaBalanco.ELENCO;

    static void usar(String[] op) {
        float gap = Float.parseFloat(op[1]);
        int stun = Integer.parseInt(op[2]);
        BateriaBalanco.AJUSTE_CFG = c -> {
            c.comboBreakerPushGap = gap;
            c.comboBreakerStunFrames = stun;
        };
    }

    // ------------------------------------------------------------------ posições

    /** Meio da arena; quem quebra encostado na parede; quem bate de costas para a parede. */
    static final String[] POSICOES = {"meio", "quem quebra no canto", "quem bate de costas para a parede"};
    static final float[][] XS = {{420f, 980f}, {2100f, Arena.RIGHT_BOUND - 20f}, {Arena.LEFT_BOUND + 30f, Arena.LEFT_BOUND + 230f}};

    // ------------------------------------------------------------------ o troco depois da quebra

    /** O que quem quebrou tenta: L/2L, M/2M, H/2H (correndo até alcançar) ou um especial na hora. */
    static final String[] MODOS = {"L", "M", "H", "S1", "S2", "S3", "S4"};
    static final String[][] NORMAIS = {{"L", "2L"}, {"M", "2M"}, {"H", "2H"}};

    /** Resultado de uma quebra. */
    static final class Caso {
        boolean quebrou;
        /** O troco entrou sem defesa; o golpe nem acertou (errou ou não saiu em 120 quadros). */
        boolean gratis, nada;
        /** Quadros depois da quebra em que cada um pode agir; distância no 1º quadro livre de quem quebrou. */
        int livreQuebra = -1, livreBate = -1;
        float gap, gapBate;
        /** Troca: quem acertou primeiro (1 quem quebrou, 0 quem batia, -1 ninguém). */
        int primeiro = -1;
        /** Canto (CPU × CPU, 3 s): dano de cada lado e distância de quem quebrou da parede no fim. */
        int danoQuebra, danoBate;
        float paredeQuebra;
    }

    /**
     * A CPU (nível 8) bate em {@code quebra}, que não defende e quebra na 1ª chance (1 barra). Depois:
     * modo 0–6 = quem quebrou tenta aquele troco e quem batia defende perfeito assim que pode;
     * modo 7/8 = troca: quem quebrou faz o melhor normal que alcança e quem batia ataca na hora com o
     * S1 (7) ou correndo com o L (8); modo 9 = as duas CPUs livres por 3 s; modo 10 = os dois parados
     * (só os quadros em que cada um volta a agir e a distância).
     */
    static Caso caso(String atk, String quebra, long semente, int modo, int posicao) {
        float[] x = XS[posicao];
        BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {atk}, new String[] {quebra}, BateriaBalanco.NIVEL, semente,
            new CombatConfig(), x[0], x[1]);
        l.f(1).state.superMeter = CombatConfig.METER_PER_BAR;
        Caso c = new Caso();
        final int[] quebrou = {-1};
        l.mao[1] = (lu, lado, in) -> {
            CombatFighter me = lu.f(1), o = lu.f(0);
            if (quebrou[0] < 0) {
                in.clear();
                if (lu.e.canBreakCombo(me)) in.light = true;
                return;
            }
            if (modo == 9) return;
            in.clear();
            if (modo == 10 || !me.canAct()) return;
            if (modo >= 3 && modo <= 6) {
                if (modo == 3) in.special = "M";
                else BateriaCombos.apertaEspecial(MODOS[modo], me, in);
                return;
            }
            normalQueAlcanca(me, o, NORMAIS[modo >= 7 ? 0 : modo], in);
        };
        l.mao[0] = (lu, lado, in) -> {
            if (quebrou[0] < 0 || modo == 9) return;
            in.clear();
            if (modo == 10) return;
            CombatFighter me = lu.f(0), o = lu.f(1);
            if (modo == 7 || modo == 8) {
                if (!me.canAct()) return;
                if (modo == 7) in.special = "M";
                else normalQueAlcanca(me, o, NORMAIS[0], in);
                return;
            }
            BateriaBalanco.defender(lu, 0, in);
        };
        for (int fr = 0; fr < 60 * 25; fr++) {
            l.passo();
            if (l.acabou()) break;
            if (quebrou[0] < 0) {
                if (l.e.cues().contains("COMBO_BREAKER") && l.f(1).comboBroke) {
                    quebrou[0] = l.quadro;
                    c.quebrou = true;
                }
                continue;
            }
            int depois = l.quadro - quebrou[0];
            if (c.livreQuebra < 0 && l.f(1).canAct()) {
                c.livreQuebra = depois;
                c.gap = Math.abs(l.f(0).x - l.f(1).x);
            }
            if (c.livreBate < 0 && l.f(0).canAct()) {
                c.livreBate = depois;
                c.gapBate = Math.abs(l.f(0).x - l.f(1).x);
            }
            if (modo == 10) {
                if (c.livreQuebra >= 0 && c.livreBate >= 0 || depois > 200) return c;
                continue;
            }
            if (modo == 9) {
                for (CombatEngine.HitEvent h : l.e.events()) {
                    if (h.blocked) continue;
                    if (h.attacker == 1) c.danoQuebra += h.damage; else c.danoBate += h.damage;
                }
                if (depois >= 180) {
                    CombatFighter q = l.f(1);
                    c.paredeQuebra = Math.min(q.x - Arena.LEFT_BOUND, Arena.RIGHT_BOUND - q.x);
                    return c;
                }
                continue;
            }
            for (CombatEngine.HitEvent h : l.e.events()) {
                if (modo >= 7) {
                    if (c.primeiro < 0 && !h.blocked) c.primeiro = h.attacker;
                    continue;
                }
                if (h.attacker != 1) continue;
                c.gratis = !h.blocked;
                return c;
            }
            if (modo >= 7 && c.primeiro >= 0) return c;
            if (depois > 120) {
                c.nada = modo < 7;
                return c;
            }
        }
        return c;
    }

    /** Aperta o primeiro dos golpes que alcança daqui; senão corre até o outro. */
    static void normalQueAlcanca(CombatFighter me, CombatFighter o, String[] golpes, FighterInput in) {
        float gap = Math.abs(o.x - me.x);
        CharacterDefinition ch = me.character();
        for (String g : golpes) {
            if (ch.attack(g) != null && CombatRules.reaches(ch, g, false, gap, o.body())) {
                if (g.startsWith("2")) in.direction = me.facing > 0 ? 2 : 4;
                if (g.endsWith("L")) in.light = true; else if (g.endsWith("M")) in.medium = true; else in.heavy = true;
                return;
            }
        }
        in.direction = o.x > me.x ? 1 : 5;
        in.dash = true;
    }

    // ------------------------------------------------------------------ relatórios

    static String pct(double v) { return BateriaBalanco.pct(v); }

    static String nome(String id) { return BateriaBalanco.nome(id); }

    /** O grupo breaker da etapa 1 (mesmas sementes), em cada opção. */
    static void grupo() {
        System.out.println("## Grupo breaker da BateriaAntesDepois (as sementes da etapa 1)");
        System.out.println();
        Map<String, Map<String, Double>> r = new LinkedHashMap<>();
        for (String[] op : OPCOES) {
            usar(op);
            BateriaAntesDepois.M.clear();
            BateriaAntesDepois.breaker();
            r.put(op[0], new LinkedHashMap<>(BateriaAntesDepois.M));
        }
        BateriaBalanco.AJUSTE_CFG = null;
        StringBuilder cab = new StringBuilder("| medida |"), sep = new StringBuilder("|---|");
        for (String[] op : OPCOES) { cab.append(' ').append(op[0]).append(" |"); sep.append("---|"); }
        System.out.println();
        System.out.println(cab);
        System.out.println(sep);
        for (String k : r.get(OPCOES[0][0]).keySet()) {
            StringBuilder b = new StringBuilder("| " + k + " |");
            for (String[] op : OPCOES) {
                double v = r.get(op[0]).get(k);
                boolean p = v <= 1.0 && !k.endsWith("casos");
                b.append(' ').append(p ? pct(v) : String.format(Locale.ROOT, "%.0f", v)).append(" |");
            }
            System.out.println(b);
        }
    }

    /** Pares de quem bate × quem quebra (16 × 16, sem espelho) e 2 sementes. */
    static List<String[]> pares() {
        List<String[]> l = new ArrayList<>();
        for (String atk : ELENCO) for (String q : ELENCO) for (long s = 1; s <= 2; s++) l.add(new String[] {atk, q, Long.toString(s)});
        return l;
    }

    /** Troco grátis por golpe e por personagem (quem quebra e quem toma), no meio e nos dois cantos. */
    static void golpes() {
        golpes(POSICOES.length);
    }

    static void golpes(int posicoes) {
        for (int pos = 0; pos < posicoes; pos++) {
            System.out.println();
            System.out.println("## Troco grátis por golpe — " + POSICOES[pos]);
            System.out.println();
            System.out.println("Quem quebrou tenta o golpe (L = L ou 2L, o que alcançar; correndo até alcançar) ou o especial na hora;");
            System.out.println("quem batia defende perfeito assim que sai do atordoamento. 16 × 16 pares, 2 sementes; \"qualquer\" =");
            System.out.println("algum dos sete entra. Vantagem = quadros entre quem quebrou poder agir e quem batia poder agir; distância entre os");
            System.out.println("centros nesses dois quadros (o empurrão só anda depois da pausa da quebra).");
            System.out.println();
            StringBuilder resumo = new StringBuilder();
            resumo.append("| opção | quebras | L | M | H | S1 | S2 | S3 | S4 | qualquer | nenhum golpe sai/acerta (L) | vantagem média | distância (quem quebrou livre → quem batia livre) |\n");
            resumo.append("|---|---|---|---|---|---|---|---|---|---|---|---|---|\n");
            for (String[] op : OPCOES) {
                usar(op);
                final int p = pos;
                // por personagem: [quem quebra][modo + qualquer] e [quem bate][qualquer], mais o total
                Map<String, Caso[]> res = new ConcurrentHashMap<>();
                pares().parallelStream().forEach(par -> {
                    Caso[] cs = new Caso[MODOS.length + 1];
                    for (int m = 0; m < MODOS.length; m++) cs[m] = caso(par[0], par[1], Long.parseLong(par[2]), m, p);
                    cs[MODOS.length] = caso(par[0], par[1], Long.parseLong(par[2]), 10, p);
                    res.put(String.join("|", par), cs);
                });
                int[][] q = new int[ELENCO.length][MODOS.length + 2];
                int[][] b = new int[ELENCO.length][2];
                int[] tot = new int[MODOS.length + 2];
                int nada = 0;
                double vant = 0, dist = 0, distB = 0;
                int nv = 0;
                for (Map.Entry<String, Caso[]> e : res.entrySet()) {
                    String[] par = e.getKey().split("\\|");
                    Caso[] cs = e.getValue();
                    if (!cs[0].quebrou) continue;
                    int iq = indice(par[1]), ib = indice(par[0]);
                    boolean algum = false;
                    for (int m = 0; m < MODOS.length; m++) {
                        if (cs[m].gratis) { q[iq][m]++; tot[m]++; algum = true; }
                    }
                    if (cs[0].nada) nada++;
                    q[iq][MODOS.length + 1]++;
                    tot[MODOS.length + 1]++;
                    if (algum) { q[iq][MODOS.length]++; tot[MODOS.length]++; b[ib][0]++; }
                    b[ib][1]++;
                    Caso pa = cs[MODOS.length];
                    if (pa.livreQuebra >= 0 && pa.livreBate >= 0) {
                        vant += pa.livreBate - pa.livreQuebra;
                        dist += pa.gap;
                        distB += pa.gapBate;
                        nv++;
                    }
                }
                int n = Math.max(1, tot[MODOS.length + 1]);
                StringBuilder lin = new StringBuilder("| " + op[0] + " | " + tot[MODOS.length + 1] + " |");
                for (int m = 0; m <= MODOS.length; m++) lin.append(' ').append(pct(tot[m] / (double)n)).append(" |");
                lin.append(' ').append(pct(nada / (double)n)).append(" | ").append(String.format(Locale.ROOT, "%+.1f q", vant / Math.max(1, nv)))
                    .append(" | ").append(String.format(Locale.ROOT, "%.0f px → %.0f px", dist / Math.max(1, nv), distB / Math.max(1, nv))).append(" |");
                resumo.append(lin).append('\n');
                System.out.println("### " + op[0] + " — por personagem");
                System.out.println();
                System.out.println("| personagem | quebrando: L | M | H | S1 | S2 | S3 | S4 | qualquer | tomando: troco grátis que leva |");
                System.out.println("|---|---|---|---|---|---|---|---|---|---|");
                for (int i = 0; i < ELENCO.length; i++) {
                    int nq = Math.max(1, q[i][MODOS.length + 1]);
                    StringBuilder l = new StringBuilder("| " + nome(ELENCO[i]) + " |");
                    for (int m = 0; m <= MODOS.length; m++) l.append(' ').append(pct(q[i][m] / (double)nq)).append(" |");
                    l.append(' ').append(pct(b[i][0] / (double)Math.max(1, b[i][1]))).append(" (").append(b[i][1]).append(") |");
                    System.out.println(l);
                }
                System.out.println();
            }
            BateriaBalanco.AJUSTE_CFG = null;
            System.out.println("### Resumo — " + POSICOES[pos]);
            System.out.println();
            System.out.print(resumo);
        }
    }

    static int indice(String id) {
        for (int i = 0; i < ELENCO.length; i++) if (ELENCO[i].equals(id)) return i;
        throw new IllegalArgumentException(id);
    }

    /** A troca: quem quebrou vem com o normal e quem batia ataca na hora (S1 ou correndo com o L). */
    static void troca() {
        System.out.println();
        System.out.println("## Troca depois da quebra (o atacante alcança com projétil ou avanço?)");
        System.out.println();
        System.out.println("Quem quebrou corre com o L/2L; quem batia, assim que pode agir, solta o S1 (projétil ou avanço) ou corre com o L.");
        System.out.println("Quem acertou primeiro sem defesa (16 × 16 pares, 2 sementes, no meio).");
        System.out.println();
        System.out.println("| opção | atacante com S1: quem quebrou acerta antes | atacante acerta antes | atacante correndo com L: quem quebrou antes | atacante antes |");
        System.out.println("|---|---|---|---|---|");
        for (String[] op : OPCOES) {
            usar(op);
            int[][] t = new int[2][3];
            Map<String, Caso[]> res = new ConcurrentHashMap<>();
            pares().parallelStream().forEach(par -> res.put(String.join("|", par), new Caso[] {
                caso(par[0], par[1], Long.parseLong(par[2]), 7, 0), caso(par[0], par[1], Long.parseLong(par[2]), 8, 0)}));
            for (Caso[] cs : res.values()) {
                for (int k = 0; k < 2; k++) {
                    if (!cs[k].quebrou) continue;
                    t[k][2]++;
                    if (cs[k].primeiro == 1) t[k][0]++; else if (cs[k].primeiro == 0) t[k][1]++;
                }
            }
            System.out.printf(Locale.ROOT, "| %s | %s | %s | %s | %s |%n", op[0], pct(t[0][0] / (double)Math.max(1, t[0][2])),
                pct(t[0][1] / (double)Math.max(1, t[0][2])), pct(t[1][0] / (double)Math.max(1, t[1][2])), pct(t[1][1] / (double)Math.max(1, t[1][2])));
        }
        BateriaBalanco.AJUSTE_CFG = null;
    }

    /** As duas CPUs livres por 3 s depois da quebra: no meio e com quem quebrou no canto. */
    static void canto() {
        System.out.println();
        System.out.println("## 3 s depois da quebra, CPU × CPU (quem quebrou no canto fica preso?)");
        System.out.println();
        System.out.println("Até a quebra o lado que quebra só apanha; depois as duas CPUs (nível 8) jogam livres por 180 quadros.");
        System.out.println();
        System.out.println("| opção | posição | quebras | dano de quem quebrou | dano de quem batia | saldo para quem quebrou | quem quebrou ainda a < 150 px da parede |");
        System.out.println("|---|---|---|---|---|---|---|");
        for (String[] op : OPCOES) {
            usar(op);
            for (int pos = 0; pos < 2; pos++) {
                final int p = pos;
                Map<String, Caso> res = new ConcurrentHashMap<>();
                pares().parallelStream().forEach(par -> res.put(String.join("|", par), caso(par[0], par[1], Long.parseLong(par[2]), 9, p)));
                int n = 0, preso = 0;
                double dq = 0, db = 0;
                for (Caso c : res.values()) {
                    if (!c.quebrou) continue;
                    n++;
                    dq += c.danoQuebra;
                    db += c.danoBate;
                    if (c.paredeQuebra < 150f) preso++;
                }
                int nn = Math.max(1, n);
                System.out.printf(Locale.ROOT, "| %s | %s | %d | %.0f | %.0f | %+.0f | %s |%n", op[0], POSICOES[pos], n, dq / nn, db / nn, (dq - db) / nn,
                    pct(preso / (double)nn));
            }
        }
        BateriaBalanco.AJUSTE_CFG = null;
    }

    /** O troco da CPU (o grupo breaker): com que golpe a CPU que quebrou abre o troco, e quanto rende. */
    static void trocoCpu() {
        System.out.println();
        System.out.println("## Troco da CPU (nível 8) depois da quebra: o golpe que abre");
        System.out.println();
        System.out.println("Cenário do grupo breaker (cada atacante × Ryūga quebrando, sementes 1–16): golpe que acertou primeiro depois da quebra.");
        System.out.println();
        System.out.println("| opção | trocos | dano médio por troco | golpes que abrem (vezes, dano médio do troco) |");
        System.out.println("|---|---|---|---|");
        for (String[] op : OPCOES) {
            usar(op);
            Map<String, double[]> porGolpe = new java.util.TreeMap<>();
            int n = 0;
            double soma = 0;
            for (String atk : ELENCO) {
                for (long sd = 1; sd <= 16; sd++) {
                    BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {atk}, new String[] {"player_base"}, BateriaBalanco.NIVEL, sd, new CombatConfig());
                    l.f(1).state.superMeter = CombatConfig.METER_PER_BAR;
                    final boolean[] solto = {false};
                    l.mao[1] = (lu, lado, in) -> {
                        if (solto[0]) return;
                        in.clear();
                        if (lu.e.canBreakCombo(lu.f(1))) in.light = true;
                    };
                    int depois = -1;
                    String abre = null;
                    for (int fr = 0; fr < 60 * 40; fr++) {
                        l.passo();
                        if (depois < 0 && l.e.cues().contains("COMBO_BREAKER")) { solto[0] = true; depois = fr; continue; }
                        if (depois < 0) continue;
                        for (CombatEngine.HitEvent h : l.e.events()) if (abre == null && h.attacker == 1 && !h.blocked) abre = h.moveId;
                        ComboSession t = l.e.session(0);
                        ComboSession fim = l.e.lastSession(0);
                        if (fim != null && fim.attacker == 1 && !(t != null && t.active) && fr > depois + 5 && l.e.framesSinceSessionEnd(0) == 1) {
                            n++;
                            soma += fim.comboDamage;
                            double[] v = porGolpe.computeIfAbsent(abre == null ? "?" : abre, k -> new double[2]);
                            v[0]++;
                            v[1] += fim.comboDamage;
                            break;
                        }
                        if (fr > depois + 240) break;
                    }
                }
            }
            StringBuilder b = new StringBuilder();
            porGolpe.entrySet().stream().sorted((x, y) -> Double.compare(y.getValue()[0], x.getValue()[0])).forEach(e ->
                b.append(b.length() > 0 ? ", " : "").append(e.getKey()).append(" ").append((int)e.getValue()[0]).append(" (")
                    .append(Math.round(e.getValue()[1] / e.getValue()[0])).append(")"));
            System.out.printf(Locale.ROOT, "| %s | %d | %.0f | %s |%n", op[0], n, soma / Math.max(1, n), b);
        }
        BateriaBalanco.AJUSTE_CFG = null;
    }

    public static void main(String[] args) {
        String q = args.length > 0 ? args[0] : "tudo";
        long t0 = System.nanoTime();
        System.out.println("# Combo Breaker: separação 320 px × atordoamento 4 × hoje");
        for (String id : ELENCO) BateriaBalanco.danoUltra(id);
        if (q.equals("grupo") || q.equals("tudo")) grupo();
        if (q.equals("golpes") || q.equals("tudo")) golpes();
        if (q.equals("troca") || q.equals("tudo")) troca();
        if (q.equals("canto") || q.equals("tudo")) canto();
        if (q.equals("trococpu") || q.equals("tudo")) trocoCpu();
        if (q.equals("curva")) {
            // Outras opções só no meio: "nome,separação,atordoamento;..."
            String[] ops = args[1].split(";");
            OPCOES = new String[ops.length][];
            for (int i = 0; i < ops.length; i++) OPCOES[i] = ops[i].split(",");
            golpes(1);
        }
        if (q.equals("um")) {
            usar(OPCOES[Integer.parseInt(args[5])]);
            Caso c = caso(args[1], args[2], Long.parseLong(args[3]), Integer.parseInt(args[4]), 0);
            System.out.println("quebrou " + c.quebrou + " grátis " + c.gratis + " nada " + c.nada + " livre q/b " + c.livreQuebra + "/" + c.livreBate
                + " gap " + c.gap + "/" + c.gapBate + " primeiro " + c.primeiro);
        }
        System.out.printf(Locale.ROOT, "%n(%.0f s)%n", (System.nanoTime() - t0) / 1e9);
    }
}
