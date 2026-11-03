package com.gamelutagpt;

import java.util.*;
import java.util.concurrent.*;

/**
 * Quantas lutas estouram o tempo (TIME OVER, pedido do jogador, 03/11). CPU × CPU no mesmo nível dos
 * dois lados: 1x1 com os 16 todos contra todos (cada par dos dois lados) e duplas sorteadas.
 *
 * <pre>args: [1x1|2x2|tudo] [níveis...] (padrão 2x2 3 5 8)</pre>
 */
public final class BateriaTempo {
    private BateriaTempo() {}

    static final class Res {
        boolean tempo;
        int quadros;
        double vidaVencedor, vidaPerdedor;   // fração da vida do time no fim
        int shinkais, quadrosShinkai;
        String[] t0, t1;
    }

    static double vida(CombatEngine e, int lado) {
        double v = 0, m = 0;
        for (FighterState s : e.teams.sides[lado].members) {
            v += Math.max(0, s.life) + Math.max(0, s.reserva);
            m += s.profile.maxLife;
        }
        return m == 0 ? 0 : v / m;
    }

    static Res luta(String[] t0, String[] t1, int nivel, long semente) {
        BateriaBalanco.Luta l = new BateriaBalanco.Luta(t0, t1, nivel, semente, new CombatConfig());
        Res r = new Res();
        r.t0 = t0;
        r.t1 = t1;
        boolean[] antes = new boolean[2];
        while (!l.acabou() && l.quadro < 60 * 60 * 6) {
            try {
                l.passo();
            } catch (RuntimeException ex) {
                throw new IllegalStateException(Arrays.toString(t0) + " x " + Arrays.toString(t1) + " nível " + nivel
                    + " semente " + semente + " quadro " + l.quadro, ex);
            }
            for (int s = 0; s < 2; s++) {
                boolean em = l.f(s).state.inShinkai();
                if (em && !antes[s]) r.shinkais++;
                if (em) r.quadrosShinkai++;
                antes[s] = em;
            }
        }
        r.tempo = l.flow.timeOver();
        r.quadros = l.quadro;
        double a = vida(l.e, 0), b = vida(l.e, 1);
        r.vidaVencedor = Math.max(a, b);
        r.vidaPerdedor = Math.min(a, b);
        return r;
    }

    public static void main(String[] args) throws Exception {
        List<String> modos = new ArrayList<>(Collections.singletonList("2x2"));
        List<Integer> ns = new ArrayList<>();
        for (String a : args) {
            if (a.equals("tudo")) modos = Arrays.asList("1x1", "2x2");
            else if (a.contains("x")) modos = Collections.singletonList(a);
            else ns.add(Integer.parseInt(a));
        }
        int[] niveis = ns.isEmpty() ? new int[] {3, 5, 8} : ns.stream().mapToInt(Integer::intValue).toArray();
        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);   // um erro numa luta encerra a bateria em vez de deixá-la parada
            return t;
        });
        String[] el = BateriaBalanco.ELENCO;
        System.out.println("# Lutas que estouram o tempo (" + BattleFlow.TIME_LIMIT_SECONDS + " s no relógio, CPU × CPU)");
        System.out.println();
        System.out.println("| modo | nível | lutas | TIME OVER | duração média | 90% das lutas até | SHINKAIs por luta | vida no TIME OVER (vencedor / perdedor) |");
        System.out.println("|---|---|---|---|---|---|---|---|");
        Map<String, int[]> porLutador = new TreeMap<>();   // lutas, time over (todos os níveis)
        for (String modo : modos) {
            for (int nivel : niveis) {
                List<Future<Res>> fs = new ArrayList<>();
                int k = 0;
                if (modo.equals("1x1")) {
                    for (String a : el) for (String b : el) {
                        if (a.equals(b)) continue;
                        long s = 880_000 + nivel * 1000 + k++;
                        fs.add(pool.submit(() -> luta(new String[] {a}, new String[] {b}, nivel, s)));
                    }
                } else {
                    Random rnd = new Random(4242 + nivel);
                    for (int i = 0; i < 240; i++) {
                        List<String> lista = new ArrayList<>(Arrays.asList(el));
                        Collections.shuffle(lista, rnd);
                        String[] a = {lista.get(0), lista.get(1)}, b = {lista.get(2), lista.get(3)};
                        long s = 990_000 + nivel * 1000 + i;
                        fs.add(pool.submit(() -> luta(a, b, nivel, s)));
                    }
                }
                List<Res> rs = new ArrayList<>();
                for (Future<Res> f : fs) rs.add(f.get());
                int tempo = 0;
                long soma = 0;
                double vv = 0, vp = 0, sh = 0;
                int[] dur = new int[rs.size()];
                for (int i = 0; i < rs.size(); i++) {
                    Res r = rs.get(i);
                    dur[i] = r.quadros;
                    soma += r.quadros;
                    sh += r.shinkais;
                    if (r.tempo) {
                        tempo++;
                        vv += r.vidaVencedor;
                        vp += r.vidaPerdedor;
                    }
                    List<String> quem = new ArrayList<>(Arrays.asList(r.t0));
                    quem.addAll(Arrays.asList(r.t1));
                    for (String id : quem) {
                        int[] v = porLutador.computeIfAbsent(id, x -> new int[2]);
                        v[0]++;
                        if (r.tempo) v[1]++;
                    }
                }
                Arrays.sort(dur);
                System.out.printf(Locale.ROOT, "| %s | %d | %d | %d (%.0f%%) | %.0f s | %.0f s | %.1f | %s |%n",
                    modo, nivel, rs.size(), tempo, 100.0 * tempo / rs.size(), soma / 60.0 / rs.size(),
                    dur[(int)(dur.length * 0.9)] / 60.0, sh / rs.size(),
                    tempo == 0 ? "—" : String.format(Locale.ROOT, "%.0f%% / %.0f%%", 100 * vv / tempo, 100 * vp / tempo));
            }
        }
        pool.shutdown();
        if (porLutador.isEmpty()) return;
        System.out.println();
        System.out.println("## Lutas com TIME OVER por lutador (no time de algum dos lados, todos os níveis)");
        System.out.println();
        System.out.println("| lutador | lutas | TIME OVER |");
        System.out.println("|---|---|---|");
        for (Map.Entry<String, int[]> e : porLutador.entrySet())
            System.out.printf(Locale.ROOT, "| %s | %d | %.0f%% |%n", BateriaBalanco.nome(e.getKey()), e.getValue()[0],
                100.0 * e.getValue()[1] / e.getValue()[0]);
    }
}
