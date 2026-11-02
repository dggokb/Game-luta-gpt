package com.gamelutagpt;

import java.util.*;

/**
 * Ritmo da luta (GameView.RITMO_BASE; docs/balanco/ritmo.md). O motor conta quadros: CPU × CPU dá
 * igual em qualquer ritmo. O que muda é o tempo de verdade, e com ele quem tem tempo de reação em
 * milissegundos (uma pessoa). Mede:
 * <ol>
 *   <li>os golpes e o movimento em milissegundos, a 100% (antigo) e a 95% (agora), ao lado do
 *   mercado (todos a 60 quadros por segundo);</li>
 *   <li>a duração das lutas em segundos;</li>
 *   <li>uma "pessoa" (a CPU de um nível, com a reação dela fixa em milissegundos) contra a CPU de
 *   cada nível do arcade: no ritmo mais lento ela reage em menos quadros.</li>
 * </ol>
 * Roda à mão: {@code java ... com.gamelutagpt.BateriaRitmo [lutas por nível]}
 */
public final class BateriaRitmo {
    private BateriaRitmo() {}

    static final float[] RITMOS = {1f, 0.95f, 0.90f};

    static double ms(double quadros, float ritmo) { return quadros * 1000.0 / (60.0 * ritmo); }

    static double med(List<Integer> v) {
        Collections.sort(v);
        return v.isEmpty() ? 0 : v.get(v.size() / 2);
    }

    static void golpes() {
        System.out.println("## Golpes e movimento em milissegundos (mediana do elenco)");
        System.out.println();
        StringBuilder cab = new StringBuilder("| | quadros |");
        StringBuilder sep = new StringBuilder("|---|---|");
        for (float r : RITMOS) { cab.append(String.format(Locale.ROOT, " a %.0f%% |", r * 100)); sep.append("---|"); }
        System.out.println(cab.append(" mercado (60 fps) |"));
        System.out.println(sep.append("---|"));
        String[][] golpes = {{"L", "início do L (jab)", "SF6 4–5 q (67–83 ms)"}, {"2L", "início do 2L", "SF6 4–5 q"},
            {"M", "início do M", "SF6 6–8 q (100–133 ms)"}, {"2M", "início da rasteira (2M)", "SF6 7–9 q"},
            {"H", "início do H", "SF6 8–12 q (133–200 ms)"}, {"2H", "início do 2H (lançador)", "–"}};
        for (String[] g : golpes) {
            List<Integer> v = new ArrayList<>();
            for (String id : BateriaBalanco.ELENCO) {
                AttackDefinition a = BateriaBalanco.ch(id).attack(g[0]);
                if (a != null) v.add(a.startupFrames);
            }
            linha(g[1], med(v), g[2]);
        }
        List<Integer> s = new ArrayList<>(), total = new ArrayList<>();
        for (String id : BateriaBalanco.ELENCO) {
            AttackDefinition a = BateriaBalanco.ch(id).attack("S1");
            if (a == null) a = BateriaBalanco.ch(id).attack("S");
            if (a != null && a.kind == AttackDefinition.Kind.PROJECTILE) { s.add(a.startupFrames); total.add(a.totalFrames); }
        }
        linha("início da bola (S)", med(s), "Hadoken 12–16 q (200–267 ms)");
        linha("bola inteira (S)", med(total), "Hadoken ~46 q (767 ms)");
        CombatConfig c = new CombatConfig();
        linha("pulo no ar (~42 quadros)", 42, "SF6 ~45 q (750 ms)");
        linha("reação humana boa (referência)", 15, "~250 ms");
        linha("CPU nível 8 (reação)", 13, "–");
        linha("CPU nível 5 (reação)", 16, "–");
        System.out.println();
        System.out.println("Um golpe é \"de reagir\" quando o início dele passa da reação de quem defende. A 95%, cada quadro");
        System.out.println("dura 17,5 ms (era 16,7): quem tem 250 ms reage em 14,25 quadros, não em 15.");
        // Quantos golpes do elenco viram "dá para reagir" (início ≥ reação humana) em cada ritmo.
        System.out.println();
        System.out.println("| golpes do elenco (normais e especiais de perto) com início ≥ 250 ms (dá para defender reagindo) | a 100% | a 95% | a 90% |");
        System.out.println("|---|---|---|---|");
        int[] conta = new int[RITMOS.length];
        int todos = 0;
        for (String id : BateriaBalanco.ELENCO) {
            CharacterDefinition ch = BateriaBalanco.ch(id);
            for (String g : new String[] {"L", "M", "H", "2L", "2M", "2H", "6M", "S1", "S2", "S3", "S4"}) {
                AttackDefinition a = ch.attack(g);
                if (a == null || !a.melee()) continue;
                todos++;
                for (int i = 0; i < RITMOS.length; i++) if (ms(a.startupFrames, RITMOS[i]) >= 250.0) conta[i]++;
            }
        }
        System.out.printf(Locale.ROOT, "| de %d | %d | %d | %d |%n", todos, conta[0], conta[1], conta[2]);
    }

    static void linha(String nome, double q, String mercado) {
        StringBuilder b = new StringBuilder(String.format(Locale.ROOT, "| %s | %.0f |", nome, q));
        for (float r : RITMOS) b.append(String.format(Locale.ROOT, " %.0f ms |", ms(q, r)));
        System.out.println(b.append(' ').append(mercado).append(" |"));
    }

    /** Uma "pessoa" (nível {@code humano}, reação em ms fixa) contra a CPU {@code cpu}, no ritmo {@code ritmo}. */
    static double[] pessoa(int humano, int cpu, float ritmo, int n) {
        int venceu = 0, decididas = 0;
        long quadros = 0;
        for (int i = 0; i < n; i++) {
            String[] dupla = BateriaBalanco.par(new Random(500_000 + i));
            String[] a = {dupla[0]}, b = {dupla[1]};
            int lado = i & 1;
            BateriaBalanco.Luta l = new BateriaBalanco.Luta(lado == 0 ? a : b, lado == 0 ? b : a, cpu, 510_000 + i, new CombatConfig());
            l.ai[lado].setLevel(humano);
            l.ai[lado].escalaReacao = ritmo;
            while (!l.acabou() && l.quadro < 60 * 60 * 4) l.passo();
            quadros += l.quadro;
            int v = l.flow.winner();
            if (v == 0 || v == 1) {
                decididas++;
                if (v == lado) venceu++;
            }
        }
        return new double[] {100.0 * venceu / Math.max(1, decididas), quadros / (double) n};
    }

    public static void main(String[] args) {
        long t0 = System.nanoTime();
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 200;
        System.out.println("# Ritmo da luta: 100% (antigo) × 95% (agora)");
        System.out.println();
        golpes();
        System.out.println();
        System.out.println("## Uma pessoa contra a CPU do arcade (1x1, " + n + " lutas por célula, duplas sorteadas, as mesmas sementes)");
        System.out.println();
        System.out.println("A \"pessoa\" é a CPU de um nível com a reação dela em milissegundos fixa: no ritmo mais lento ela reage em");
        System.out.println("menos quadros (nível 6 = 15 quadros = 250 ms; nível 4 = 19 quadros = 317 ms). A CPU rival reage nos mesmos quadros.");
        for (int humano : new int[] {6, 4}) {
            System.out.println();
            System.out.printf(Locale.ROOT, "### Pessoa com reação de nível %d%n%n", humano);
            System.out.println("| CPU do arcade | pessoa vence a 100% | a 95% | a 90% | duração da luta a 100% / 95% |");
            System.out.println("|---|---|---|---|---|");
            for (int cpu = 5; cpu <= 8; cpu++) {
                double[] r100 = pessoa(humano, cpu, 1f, n), r95 = pessoa(humano, cpu, 0.95f, n), r90 = pessoa(humano, cpu, 0.90f, n);
                System.out.printf(Locale.ROOT, "| nível %d | %.0f%% | %.0f%% | %.0f%% | %.1f s / %.1f s |%n", cpu, r100[0], r95[0], r90[0],
                    r100[1] / 60.0, r95[1] / (60.0 * 0.95));
            }
        }
        System.out.printf(Locale.ROOT, "%n(%.0f s)%n", (System.nanoTime() - t0) / 1e9);
    }
}
