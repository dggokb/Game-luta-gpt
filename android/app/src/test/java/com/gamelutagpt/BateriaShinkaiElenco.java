package com.gamelutagpt;

import java.util.*;

/**
 * A regra nova do SHINKAI (vida nova, sem cronômetro, limite, Ruptura, transformação quebrada)
 * para todos, cada tipo com a sua característica (pedido de 02/11: "mantém as características
 * dos outros mas põe a nova regra"). Os 16 contra todos (cada par dos dois lados), CPU × CPU, com
 * o SHINKAI Clash (o jogo de verdade). Três colunas, mesmas sementes:
 * <ul>
 *   <li>relógio (antes): os 8 s de sempre para os outros e o desgaste para o Ryūga;</li>
 *   <li>vida nova (agora): a reserva de uma vida inteira para todos;</li>
 *   <li>sem SHINKAI: a referência.</li>
 * </ul>
 *
 * <pre>args: [lutas por par] [níveis...] (padrão 2, níveis 5 e 8)</pre>
 */
public final class BateriaShinkaiElenco {
    private BateriaShinkaiElenco() {}

    static final String[] MODOS = {"relógio (antes)", "vida nova (agora)", "sem SHINKAI"};

    static final class PorTipo {
        long ativacoes, quadros, danoDado, danoLevado, vidaPerdida, quebras, rupturas, clashes, lados;
        final Map<String, Integer> fim = new TreeMap<>();
    }

    static final class Est {
        int lutas, decididas, viradas, soUm, soUmVence, primeiro, primeiroVence, quebras, quebraVence, ambos;
        long quadros, clashes;
        final Map<String, int[]> porLutador = new TreeMap<>();   // id → {vitórias, lutas}
        final Map<Shinkai.Type, PorTipo> porTipo = new EnumMap<>(Shinkai.Type.class);

        PorTipo tipo(Shinkai.Type t) { return porTipo.computeIfAbsent(t, k -> new PorTipo()); }
    }

    static void configurar(int modo) {
        BateriaBalanco.AJUSTE_CFG = modo == 0 ? c -> { c.ryugaReserva = false; c.shinkaiReservaTodos = false; } : null;
    }

    static void luta(String a, String b, int nivel, long semente, int modo, Est e) {
        BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {a}, new String[] {b}, nivel, semente, new CombatConfig());
        if (modo == 2) {
            BateriaBalanco.Mao m = (lu, ld, in) -> in.shinkai = false;
            l.mao[0] = m;
            l.mao[1] = m;
        }
        String[] quem = {a, b};
        int max = 10000;
        boolean[] dentro = new boolean[2];
        int[] ativ = new int[2], inicio = new int[2], absAntes = new int[2];
        int primeiro = -1;
        int[] maiorAtras = new int[2];
        List<Integer> quebrouQuem = new ArrayList<>();
        while (!l.acabou() && l.quadro < 60 * 60 * 5) {
            l.passo();
            List<String> cues = l.e.cues();
            if (cues.contains("SHINKAI_CLASH")) e.clashes++;
            for (int s = 0; s < 2; s++) {
                FighterState st = l.f(s).state;
                PorTipo t = e.tipo(st.shinkaiType);
                boolean agora = st.inShinkai();
                if (agora && !dentro[s]) {
                    ativ[s]++;
                    t.ativacoes++;
                    inicio[s] = l.quadro;
                    absAntes[s] = st.reservaAbsorveu;
                    if (primeiro < 0) primeiro = s;
                }
                if (!agora && dentro[s]) {
                    t.quadros += l.quadro - inicio[s];
                    t.vidaPerdida -= st.reservaAbsorveu - absAntes[s];
                    String como = cues.contains("SHINKAI_QUEBRA") ? "quebrou" : cues.contains("RUPTURA_FIM") ? "depois da Ruptura"
                        : cues.contains("SHINKAI_FIM") ? "desligou" : cues.contains("LIMITE_FIM") ? "no limite"
                        : l.f(1 - s).ko() ? "derrubou o rival" : l.f(s).ko() ? "caiu" : "o tempo acabou";
                    t.fim.merge(como, 1, Integer::sum);
                    if (como.equals("quebrou")) {
                        t.quebras++;
                        quebrouQuem.add(1 - s);
                    }
                }
                if (cues.contains("RUPTURA") && st.emRuptura() && st.rupturaQuadros <= 2) t.rupturas++;
                maiorAtras[s] = Math.max(maiorAtras[s], l.f(1 - s).state.life - st.life);
            }
            for (CombatEngine.HitEvent h : l.e.events()) {
                if (h.damage <= 0 || h.attacker == h.defender) continue;
                if (dentro[h.attacker] || l.f(h.attacker).state.inShinkai()) e.tipo(l.f(h.attacker).state.shinkaiType).danoDado += h.damage;
                if (dentro[h.defender] || l.f(h.defender).state.inShinkai()) {
                    PorTipo t = e.tipo(l.f(h.defender).state.shinkaiType);
                    t.danoLevado += h.damage;
                    t.vidaPerdida += h.damage;
                }
            }
            for (int s = 0; s < 2; s++) dentro[s] = l.f(s).state.inShinkai();
        }
        for (int s = 0; s < 2; s++) {
            FighterState st = l.f(s).state;
            PorTipo t = e.tipo(st.shinkaiType);
            t.lados++;
            if (dentro[s]) {
                t.quadros += l.quadro - inicio[s];
                t.vidaPerdida -= st.reservaAbsorveu - absAntes[s];
                t.fim.merge(l.f(1 - s).ko() ? "derrubou o rival" : l.f(s).ko() ? "caiu" : "a luta acabou", 1, Integer::sum);
            }
        }
        e.lutas++;
        e.quadros += l.quadro;
        int v = l.flow.winner();
        for (int s = 0; s < 2; s++) {
            int[] p = e.porLutador.computeIfAbsent(quem[s], k -> new int[2]);
            if (v == 0 || v == 1) {
                p[1]++;
                if (v == s) p[0]++;
            }
        }
        if (v != 0 && v != 1) return;
        e.decididas++;
        if (maiorAtras[v] >= max * 3 / 10) e.viradas++;
        int ligaram = (ativ[0] > 0 ? 1 : 0) + (ativ[1] > 0 ? 1 : 0);
        if (ligaram == 2) e.ambos++;
        if (ligaram == 1) {
            e.soUm++;
            if (ativ[v] > 0) e.soUmVence++;
        }
        if (primeiro >= 0) {
            e.primeiro++;
            if (primeiro == v) e.primeiroVence++;
        }
        for (int q : quebrouQuem) {
            e.quebras++;
            if (q == v) e.quebraVence++;
        }
    }

    static String pc(long a, long b) { return BateriaVoadora.pc((int)a, (int)Math.max(1, b)); }
    static String f1(double v) { return String.format(Locale.ROOT, "%.1f", v); }
    static String f2(double v) { return String.format(Locale.ROOT, "%.2f", v); }
    static String f0(double v) { return String.format(Locale.ROOT, "%.0f", v); }

    static double desvio(Est e) {
        double soma = 0, soma2 = 0;
        int n = 0;
        for (int[] p : e.porLutador.values()) {
            double x = p[0] / (double)Math.max(1, p[1]);
            soma += x;
            soma2 += x * x;
            n++;
        }
        double m = soma / n;
        return Math.sqrt(Math.max(0, soma2 / n - m * m));
    }

    public static void main(String[] args) {
        int k = args.length > 0 ? Integer.parseInt(args[0]) : 2;
        int[] niveis = args.length > 1 ? Arrays.stream(args, 1, args.length).mapToInt(Integer::parseInt).toArray() : new int[] {5, 8};
        StringBuilder out = new StringBuilder("# SHINKAI: a regra da vida nova para todos (02/11)\n\n");
        out.append("CPU × CPU, os 16 contra todos (cada par dos dois lados, ").append(k)
            .append(" luta(s) por par), com o SHINKAI Clash. Mesmas sementes nas três colunas.\n");
        String[] elenco = BateriaBalanco.ELENCO;
        for (int nivel : niveis) {
            Est[] e = {new Est(), new Est(), new Est()};
            for (int modo = 0; modo < 3; modo++) {
                configurar(modo);
                long semente = 1_300_000L + nivel * 100_000L;
                for (String a : elenco) for (String b : elenco) {
                    if (a.equals(b)) continue;
                    for (int i = 0; i < k; i++) luta(a, b, nivel, semente++, modo, e[modo]);
                }
            }
            BateriaBalanco.AJUSTE_CFG = null;
            out.append("\n## Nível ").append(nivel).append(" (").append(e[0].lutas).append(" lutas por coluna)\n\n");
            out.append("### A luta\n\n| medida | ").append(String.join(" | ", MODOS)).append(" |\n|---|---|---|---|\n");
            row(out, "duração média", x -> f1(x.quadros / 60.0 / x.lutas) + " s", e);
            row(out, "viradas (o vencedor esteve 30% atrás)", x -> pc(x.viradas, x.decididas), e);
            row(out, "os dois se transformam", x -> pc(x.ambos, x.decididas), e);
            row(out, "SHINKAI Clash por luta", x -> f2(x.clashes / (double)x.lutas), e);
            row(out, "só um se transformou: ele vence", x -> pc(x.soUmVence, x.soUm), e);
            row(out, "quem se transforma primeiro vence", x -> pc(x.primeiroVence, x.primeiro), e);
            row(out, "transformações quebradas por luta", x -> f2(x.quebras / (double)x.lutas), e);
            row(out, "quem quebra a do outro vence", x -> pc(x.quebraVence, x.quebras), e);
            row(out, "equilíbrio do elenco: desvio das vitórias / pior–melhor", x -> {
                int lo = 100, hi = 0;
                for (int[] p : x.porLutador.values()) { int w = Math.round(100f * p[0] / Math.max(1, p[1])); lo = Math.min(lo, w); hi = Math.max(hi, w); }
                return f1(100 * desvio(x)) + " pontos / " + lo + "%–" + hi + "%";
            }, e);

            out.append("\n### Por tipo de SHINKAI (relógio → vida nova)\n\n");
            out.append("| tipo | lutadores | SHINKAIs por luta | duração | dano dado por s dentro | vida perdida por s dentro | saldo na vida por SHINKAI | quebradas / Rupturas por SHINKAI | como acaba (vida nova) |\n");
            out.append("|---|---|---|---|---|---|---|---|---|\n");
            for (Shinkai.Type t : Shinkai.Type.values()) {
                PorTipo a = e[0].porTipo.get(t), b = e[1].porTipo.get(t);
                if (a == null || b == null || b.lados == 0) continue;
                StringBuilder nomes = new StringBuilder();
                for (String id : elenco) if (Shinkai.typeOf(id) == t) nomes.append(nomes.length() > 0 ? ", " : "").append(id);
                StringBuilder fins = new StringBuilder();
                long tot = 0;
                for (int v : b.fim.values()) tot += v;
                for (Map.Entry<String, Integer> en : b.fim.entrySet()) fins.append(en.getKey()).append(' ').append(pc(en.getValue(), tot)).append("; ");
                out.append("| ").append(t.label).append(" | ").append(nomes).append(" | ")
                    .append(f2(a.ativacoes / (double)a.lados)).append(" → ").append(f2(b.ativacoes / (double)b.lados)).append(" | ")
                    .append(f1(a.quadros / 60.0 / Math.max(1, a.ativacoes))).append(" → ").append(f1(b.quadros / 60.0 / Math.max(1, b.ativacoes))).append(" s | ")
                    .append(f0(a.danoDado / Math.max(1, a.quadros / 60.0))).append(" → ").append(f0(b.danoDado / Math.max(1, b.quadros / 60.0))).append(" | ")
                    .append(f0(a.vidaPerdida / Math.max(1, a.quadros / 60.0))).append(" → ").append(f0(b.vidaPerdida / Math.max(1, b.quadros / 60.0))).append(" | ")
                    .append(f0((a.danoDado - a.vidaPerdida) / (double)Math.max(1, a.ativacoes))).append(" → ")
                    .append(f0((b.danoDado - b.vidaPerdida) / (double)Math.max(1, b.ativacoes))).append(" | ")
                    .append(f2(b.quebras / (double)Math.max(1, b.ativacoes))).append(" / ").append(f2(b.rupturas / (double)Math.max(1, b.ativacoes))).append(" | ")
                    .append(fins).append(" |\n");
            }

            out.append("\n### Vitórias por lutador\n\n| lutador | tipo | ").append(String.join(" | ", MODOS)).append(" |\n|---|---|---|---|---|\n");
            for (String id : elenco) {
                out.append("| ").append(id).append(" | ").append(Shinkai.typeOf(id).label);
                for (Est x : e) { int[] p = x.porLutador.get(id); out.append(" | ").append(pc(p[0], p[1])); }
                out.append(" |\n");
            }
        }
        System.out.print(out);
    }

    interface Col { String de(Est e); }

    static void row(StringBuilder out, String nome, Col c, Est[] e) {
        out.append("| ").append(nome);
        for (Est x : e) out.append(" | ").append(c.de(x));
        out.append(" |\n");
    }
}
