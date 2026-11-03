package com.gamelutagpt;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * As mudanças vindas da comparação com o KOF '97 (docs/balanco/comparacao-kof97.md, 03/11): pulo mais
 * rápido com a mesma altura e queda mais curta. CPU × CPU, 1x1, os 16 todos contra todos (cada par
 * dos dois lados), a mesma semente em cada variante.
 *
 * <pre>args: [-n=5,8] [-r=repetições] variante... (base | pulo=K | queda=N | reserva=permille | combinadas com +)</pre>
 *
 * pulo=K: o pulo normal leva K do tempo (altura e distância iguais): velocidade de subida e de lado /K,
 * gravidade do pulo /K². queda=N: quadros deitado antes do levantar normal.
 */
public final class BateriaKof {
    private BateriaKof() {}

    static void pulo(CombatConfig c, float k) {
        c.jumpSpeed /= k;   // (o jogo corrige a velocidade para o passo de 60 Hz chegar à mesma altura)
        c.airJumpSpeed /= k;
        c.normalJumpRisingGravityScale /= k * k;
        c.normalJumpFallingGravityScale /= k * k;
        c.jumpForwardSpeed /= k;
        c.jumpBackSpeed /= k;
        c.dashJumpSpeed /= k;
    }

    static Consumer<CombatConfig> variante(String spec) {
        Consumer<CombatConfig> a = c -> {};
        if (spec.equals("base")) return a;
        for (String p : spec.split("\\+")) {
            String[] kv = p.split("=");
            Consumer<CombatConfig> prev = a;
            switch (kv[0]) {
                case "pulo": { float k = Float.parseFloat(kv[1]); a = prev.andThen(c -> pulo(c, k)); break; }
                case "queda": { int n = Integer.parseInt(kv[1]); a = prev.andThen(c -> c.knockdownDownFrames = n); break; }
                case "reserva": { int n = Integer.parseInt(kv[1]); a = prev.andThen(c -> c.ryugaReservaPermille = n); break; }
                default: throw new IllegalArgumentException(p);
            }
        }
        return a;
    }

    static final class Est {
        int lutas, pulos, tirado, acertou, defendido;
        long quadros, quadrosNoAr, dano;
        int quedas, levantou, levantouApanhou, levantouDefendeu;
        final Map<String, int[]> vitorias = new TreeMap<>();   // vitórias, lutas

        synchronized void somar(Est o) {
            lutas += o.lutas; pulos += o.pulos; tirado += o.tirado; acertou += o.acertou; defendido += o.defendido;
            quadros += o.quadros; quadrosNoAr += o.quadrosNoAr; dano += o.dano;
            quedas += o.quedas; levantou += o.levantou; levantouApanhou += o.levantouApanhou;
            levantouDefendeu += o.levantouDefendeu;
            for (Map.Entry<String, int[]> en : o.vitorias.entrySet()) {
                int[] v = vitorias.computeIfAbsent(en.getKey(), k -> new int[2]);
                v[0] += en.getValue()[0];
                v[1] += en.getValue()[1];
            }
        }
    }

    /** Depois de levantar: quantos quadros contam como "apanhou/defendeu no levantar". */
    static final int JANELA_LEVANTAR = 20;

    static Est luta(String a, String b, int nivel, long semente, Consumer<CombatConfig> ajuste) {
        CombatConfig cfg = new CombatConfig();
        ajuste.accept(cfg);
        BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {a}, new String[] {b}, nivel, semente, cfg);
        Est e = new Est();
        boolean[] noAr = new boolean[2], noChao = {true, true};
        int[] resultado = new int[2], inicioAr = new int[2], levantouEm = {-1, -1}, depoisLevantar = new int[2];
        CombatFighter.Status[] antes = {CombatFighter.Status.NEUTRAL, CombatFighter.Status.NEUTRAL};
        while (!l.acabou() && l.quadro < 60 * 60 * 4) {
            l.passo();
            for (int s = 0; s < 2; s++) {
                CombatFighter f = l.f(s);
                if (!f.grounded && noChao[s] && f.status == CombatFighter.Status.NEUTRAL && !f.superJumping
                    && l.e.shinkaiClash() == null) {
                    e.pulos++;
                    resultado[s] = 0;
                    noAr[s] = true;
                    inicioAr[s] = l.quadro;
                } else if (f.grounded && noAr[s]) {
                    if (resultado[s] == 3) e.tirado++;
                    else if (resultado[s] == 2) e.acertou++;
                    else if (resultado[s] == 1) e.defendido++;
                    e.quadrosNoAr += l.quadro - inicioAr[s];
                    noAr[s] = false;
                }
                noChao[s] = f.grounded;
                if (f.status == CombatFighter.Status.KNOCKDOWN && antes[s] != CombatFighter.Status.KNOCKDOWN) e.quedas++;
                if (antes[s] == CombatFighter.Status.WAKEUP && f.status == CombatFighter.Status.NEUTRAL) {
                    e.levantou++;
                    levantouEm[s] = l.quadro;
                    depoisLevantar[s] = 0;
                }
                antes[s] = f.status;
            }
            for (CombatEngine.HitEvent h : l.e.events()) {
                if (!h.blocked) e.dano += h.damage;
                if (noAr[h.attacker] && !l.f(h.attacker).grounded) resultado[h.attacker] = Math.max(resultado[h.attacker], h.blocked ? 1 : 2);
                if (noAr[h.defender] && !h.blocked && resultado[h.defender] < 2) resultado[h.defender] = 3;
                int d = h.defender;
                if (levantouEm[d] >= 0 && l.quadro - levantouEm[d] <= JANELA_LEVANTAR && depoisLevantar[d] == 0) {
                    depoisLevantar[d] = h.blocked ? 1 : 2;
                    if (h.blocked) e.levantouDefendeu++;
                    else e.levantouApanhou++;
                }
            }
        }
        e.lutas++;
        e.quadros += l.quadro;
        int w = l.flow.winner();
        String[] quem = {a, b};
        for (int s = 0; s < 2; s++) {
            int[] v = e.vitorias.computeIfAbsent(quem[s], k -> new int[2]);
            v[1]++;
            if (w == s) v[0]++;
        }
        return e;
    }

    public static void main(String[] args) throws Exception {
        int[] niveis = {5, 8};
        int reps = 1;
        List<String> variantes = new ArrayList<>();
        for (String a : args) {
            if (a.startsWith("-n=")) niveis = Arrays.stream(a.substring(3).split(",")).mapToInt(Integer::parseInt).toArray();
            else if (a.startsWith("-r=")) reps = Integer.parseInt(a.substring(3));
            else variantes.add(a);
        }
        if (variantes.isEmpty()) variantes.add("base");
        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        System.out.println("# KOF '97: pulo e queda (CPU × CPU, 1x1, 16 todos contra todos, " + 240 * reps + " lutas por nível)");
        Map<String, double[]> porLutador = new LinkedHashMap<>();
        System.out.println();
        System.out.println("| variante | nível | duração | dano/min | pulos/min (cada) | quadros no ar | tirado | acertou | defendido | quedas/luta | levantou e apanhou | levantou e defendeu | vitória pior–melhor | desvio |");
        System.out.println("|---|---|---|---|---|---|---|---|---|---|---|---|---|---|");
        for (String v : variantes) {
            Consumer<CombatConfig> ajuste = variante(v);
            for (int nivel : niveis) {
                Est total = new Est();
                List<Future<Est>> fs = new ArrayList<>();
                int k = 0;
                for (int r = 0; r < reps; r++) for (String a : BateriaBalanco.ELENCO) for (String b : BateriaBalanco.ELENCO) {
                    if (a.equals(b)) continue;
                    long s = 770_000 + nivel * 1000 + r * 100_000 + k++;
                    final int n = nivel;
                    fs.add(pool.submit(() -> luta(a, b, n, s, ajuste)));
                }
                for (Future<Est> f : fs) total.somar(f.get());
                double min = total.quadros / 3600.0;
                double[] pct = total.vitorias.values().stream().mapToDouble(x -> 100.0 * x[0] / x[1]).toArray();
                porLutador.put(v + " n" + nivel, pct);
                double media = Arrays.stream(pct).average().orElse(0);
                double desvio = Math.sqrt(Arrays.stream(pct).map(x -> (x - media) * (x - media)).average().orElse(0));
                int p = Math.max(1, total.pulos), lv = Math.max(1, total.levantou);
                System.out.printf(Locale.ROOT,
                    "| %s | %d | %.0f s | %.0f | %.1f | %.1f | %.0f%% | %.0f%% | %.0f%% | %.1f | %.0f%% | %.0f%% | %.0f–%.0f%% | %.1f |%n",
                    v, nivel, total.quadros / 60.0 / total.lutas, total.dano / min, total.pulos / (2.0 * min),
                    total.quadrosNoAr / (double)p, 100.0 * total.tirado / p, 100.0 * total.acertou / p,
                    100.0 * total.defendido / p, total.quedas / (double)total.lutas,
                    100.0 * total.levantouApanhou / lv, 100.0 * total.levantouDefendeu / lv,
                    Arrays.stream(pct).min().orElse(0), Arrays.stream(pct).max().orElse(0), desvio);
            }
        }
        pool.shutdown();
        System.out.println();
        System.out.println("## Vitórias por lutador");
        System.out.println();
        StringBuilder cab = new StringBuilder("| lutador |"), sep = new StringBuilder("|---|");
        for (String c : porLutador.keySet()) { cab.append(' ').append(c).append(" |"); sep.append("---|"); }
        System.out.println(cab);
        System.out.println(sep);
        String[] ids = new TreeMap<String, Integer>() {{ for (String id : BateriaBalanco.ELENCO) put(id, 0); }}.keySet().toArray(new String[0]);
        for (int i = 0; i < ids.length; i++) {
            StringBuilder b = new StringBuilder("| " + ids[i] + " |");
            for (double[] pct : porLutador.values()) b.append(String.format(Locale.ROOT, " %.0f%% |", pct[i]));
            System.out.println(b);
        }
    }
}
