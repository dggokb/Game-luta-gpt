package com.gamelutagpt;

import java.util.Locale;

/**
 * O jogador que só solta bola (docs/balanco/cpu-contra-bola.md): o Ryūga apertando a Onda sempre
 * que pode, contra a CPU de cada um dos 16, nos níveis 5 (o começo da história) e 8. Mede quanto
 * a CPU perde e o que ela faz contra a bola. Roda à mão:
 *
 * <pre>java -cp ... com.gamelutagpt.BateriaBola [lutas por lutador] [2x2]</pre>
 */
public final class BateriaBola {
    private BateriaBola() {}

    /** O "jogador": Onda sempre que não tem bola dele na tela; entre uma e outra, segura trás (defende). */
    static BateriaBalanco.Mao spam(boolean defende) {
        int[] k = {0};
        return (luta, lado, in) -> {
            CombatFighter me = luta.f(lado);
            in.clear();
            if (!me.grounded || me.status != CombatFighter.Status.NEUTRAL) return;
            boolean minhaNaTela = false;
            for (Projectile p : luta.e.energyProjectiles) if (p.ownerIndex == lado) minhaNaTela = true;
            if (!minhaNaTela) {
                in.special = (k[0]++ % 3 == 0) ? "L" : (k[0] % 3 == 1 ? "M" : "H");
            } else if (defende) {
                in.direction = MotionParser.relative(5, me.facing);
            }
        };
    }

    static final class Placar {
        int lutas, vitoriasCpu, pulos, chegou, acertosCpu;
        long danoCpu, danoBola;
        double quadros;
    }

    static Placar jogar(String cpu, int nivel, int n, boolean dupla, boolean defende, long base) {
        Placar p = new Placar();
        for (int s = 0; s < n; s++) {
            String[] t0 = dupla ? new String[] {"player_base", "p02"} : new String[] {"player_base"};
            String[] t1 = dupla ? new String[] {cpu, cpu.equals("p03") ? "p04" : "p03"} : new String[] {cpu};
            BateriaBalanco.Luta l = new BateriaBalanco.Luta(t0, t1, nivel, base + s, new CombatConfig());
            l.mao[0] = spam(defende);
            boolean noAr = false;
            while (!l.acabou() && l.quadro < 60 * 60 * 4) {
                l.passo();
                CombatFighter c = l.f(1), j = l.f(0);
                if (c.grounded == false && !noAr && c.status == CombatFighter.Status.NEUTRAL) p.pulos++;
                noAr = !c.grounded;
                for (CombatEngine.HitEvent ev : l.e.events()) {
                    if (ev.attacker == 1 && !ev.blocked) {
                        p.danoCpu += ev.damage;
                        p.acertosCpu++;
                    }
                    if (ev.attacker == 0) p.danoBola += ev.damage;
                }
                if (Math.abs(c.x - j.x) < 160f) p.chegou++;
            }
            p.lutas++;
            p.quadros += l.quadro;
            if (l.flow.winner() == 1) p.vitoriasCpu++;
        }
        return p;
    }

    /** Depuração: o que a CPU faz (quadros por razão) e onde ela fica, contra a bola. */
    static void depurar(String cpu, int nivel) {
        java.util.Map<String, Integer> razoes = new java.util.TreeMap<>();
        java.util.Map<String, Integer> estados = new java.util.TreeMap<>();
        double dist = 0;
        int q = 0;
        for (int s = 0; s < 5; s++) {
            BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {"player_base"}, new String[] {cpu}, nivel, 500 + s, new CombatConfig());
            l.mao[0] = spam(true);
            while (!l.acabou() && l.quadro < 60 * 60 * 2) {
                l.passo();
                razoes.merge(l.ai[1].lastReason == null ? "" : l.ai[1].lastReason, 1, Integer::sum);
                estados.merge(l.f(1).status + (l.f(1).grounded ? "" : " no ar"), 1, Integer::sum);
                dist += Math.abs(l.f(1).x - l.f(0).x);
                q++;
            }
        }
        System.out.println("## " + cpu + " nível " + nivel + ": distância média " + Math.round(dist / q));
        comoApanha(cpu, nivel);
        razoes.forEach((k, v) -> System.out.println("- razão '" + k + "': " + v));
        estados.forEach((k, v) -> System.out.println("- estado " + k + ": " + v));
    }

    /** Em que situação cada bola pega a CPU (o estado dela no quadro antes do acerto). */
    static void comoApanha(String cpu, int nivel) {
        java.util.Map<String, Integer> pegou = new java.util.TreeMap<>();
        int defendidas = 0, acertos = 0;
        java.util.Map<String, Integer> planos = new java.util.TreeMap<>();
        for (int s = 0; s < 5; s++) {
            BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {"player_base"}, new String[] {cpu}, nivel, 600 + s, new CombatConfig());
            l.mao[0] = spam(true);
            String antes = "";
            while (!l.acabou() && l.quadro < 60 * 60 * 2) {
                l.passo();
                for (CombatEngine.HitEvent ev : l.e.events()) {
                    if (ev.attacker != 0) continue;
                    if (ev.blocked) defendidas++;
                    else {
                        acertos++;
                        pegou.merge(antes, 1, Integer::sum);
                    }
                }
                CombatFighter c = l.f(1);
                CpuLeitura.PlanoBola pb = l.ai[1].planoBola();
                if (pb != null && !antes.startsWith(pb.name())) planos.merge(pb.name(), 1, Integer::sum);
                antes = (pb == null ? "" : pb.name() + " ") + c.status + (c.grounded ? "" : " no ar") + (c.forwardDashing ? " correndo" : "") +
                    " | " + (l.ai[1].lastReason == null ? "" : l.ai[1].lastReason) + " | dist " + Math.round(Math.abs(c.x - l.f(0).x) / 100f) * 100;
            }
            System.out.println("leitura: " + l.ai[1].leitura().resumo());
        }
        System.out.println("bolas defendidas " + defendidas + ", acertos do Ryūga " + acertos);
        System.out.println("planos: " + planos);
        pegou.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue()).limit(14)
            .forEach(e -> System.out.println("- " + e.getKey() + ": " + e.getValue()));
    }

    /** O spammer do CpuTest (só L, espera 4–11 quadros): bolas soltas, acertos e onde pegou. */
    static void comoNoTeste(int level) {
        java.util.Map<String, Integer> pegou = new java.util.TreeMap<>();
        int shots = 0, hits = 0;
        double perdeu = 0;
        int lutas = 0;
        for (String ch : new String[] {"p03", "player_two", "p05"}) {
            for (long seed = 1; seed <= 3; seed++) {
                CombatEngine e = new CombatEngine(new FighterState(GeneratedCharacters.get("player_base"), "P1"),
                    new FighterState(GeneratedCharacters.get(ch), "CPU"), 250f, 1000f, new CombatConfig());
                AiController cpu = new AiController(new OpponentAi(new java.util.Random(seed)), new java.util.Random(seed * 31 + 7));
                cpu.setEnabled(true);
                cpu.setLevel(level);
                FighterInput p = new FighterInput(), out = new FighterInput();
                java.util.Random r = new java.util.Random(seed * 13 + 5);
                int vida = e.fighter(0).state.life, espera = 0;
                boolean viva = false;
                String antes = "";
                for (int f = 0; f < 60 * 60 && !e.fighter(0).ko() && !e.fighter(1).ko(); f++) {
                    CombatFighter me = e.fighter(0);
                    p.clear();
                    if (me.canAct() && me.grounded && !e.energyProjectileAlive(0) && espera-- <= 0) {
                        p.special = "L";
                        espera = 4 + r.nextInt(8);
                    } else {
                        p.direction = me.facing > 0 ? 5 : 1;
                    }
                    cpu.fill(e, 1, out);
                    e.step(p, out);
                    for (CombatEngine.HitEvent ev : e.events()) {
                        if (ev.attacker == 0 && ev.projectile && !ev.blocked) {
                            hits++;
                            pegou.merge(antes, 1, Integer::sum);
                        }
                    }
                    if (e.energyProjectileAlive(0) && !viva) shots++;
                    viva = e.energyProjectileAlive(0);
                    CombatFighter c = e.fighter(1);
                    CpuLeitura.PlanoBola pb = cpu.planoBola();
                    antes = (pb == null ? "-" : pb.name()) + " " + c.status + (c.grounded ? "" : " ar") +
                        (c.forwardDashing ? " correndo" : "") + " | " + cpu.lastReason + " | " +
                        Math.round(Math.abs(c.x - e.fighter(0).x) / 100f) * 100;
                }
                perdeu += 1.0 - e.fighter(0).state.life / (double)vida;
                lutas++;
            }
        }
        System.out.println(String.format(Locale.ROOT, "nível %d: bolas %d, acertos %d (%.2f), vida do spammer perdida %.2f",
            level, shots, hits, hits / (double)shots, perdeu / lutas));
        pegou.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue()).limit(16)
            .forEach(en -> System.out.println("- " + en.getKey() + ": " + en.getValue()));
    }

    /** CPU × CPU: os planos contra a bola que o lado 0 escolheu e os especiais que ele começou. */
    static void cpuXcpu(String a, String b, int nivel) {
        java.util.Map<String, Integer> planos = new java.util.TreeMap<>(), especiais = new java.util.TreeMap<>();
        for (int i = 0; i < 6; i++) {
            BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {a}, new String[] {b}, nivel, 500 + i, new CombatConfig());
            CpuLeitura.PlanoBola antes = null;
            AttackDefinition golpe = null;
            while (!l.acabou() && l.quadro < 60 * 60 * 2) {
                l.passo();
                CpuLeitura.PlanoBola pb = l.ai[0].planoBola();
                if (pb != null && pb != antes) planos.merge(pb.name(), 1, Integer::sum);
                antes = pb;
                AttackDefinition g = l.f(0).attacking() ? l.f(0).attack : null;
                if (g != null && g != golpe && g.id.matches("S[234]")) especiais.merge(g.id + " " + l.ai[0].lastReason, 1, Integer::sum);
                golpe = g;
            }
            System.out.println("leitura: " + l.ai[0].leitura().resumo());
        }
        System.out.println("planos " + planos + "\nespeciais " + especiais);
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("cpu")) {
            cpuXcpu(args[1], args[2], Integer.parseInt(args[3]));
            return;
        }
        if (args.length > 0 && args[0].equals("teste")) {
            comoNoTeste(Integer.parseInt(args[1]));
            return;
        }
        if (args.length > 0 && args[0].equals("depurar")) {
            depurar(args.length > 1 ? args[1] : "p02", args.length > 2 ? Integer.parseInt(args[2]) : 8);
            return;
        }
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 20;
        boolean dupla = args.length > 1 && args[1].equals("2x2");
        System.out.println("# O Ryūga que só solta bola" + (dupla ? " (2x2)" : " (1x1)"));
        System.out.println();
        System.out.println("| CPU | nível 5: CPU vence | nível 8: CPU vence | nível 8: pulos/min | nível 8: tempo colado | nível 8: dano da bola por min (% de vida) |");
        System.out.println("|---|---|---|---|---|---|");
        int v5 = 0, v8 = 0, tot = 0;
        for (String id : BateriaBalanco.ELENCO) {
            Placar a = jogar(id, 5, n, dupla, true, 31_000);
            Placar b = jogar(id, 8, n, dupla, true, 31_000);
            double min = b.quadros / 3600.0;
            v5 += a.vitoriasCpu;
            v8 += b.vitoriasCpu;
            tot += n;
            System.out.println(String.format(Locale.ROOT, "| %s | %.0f%% | %.0f%% | %.1f | %.0f%% | %.0f |", id,
                100.0 * a.vitoriasCpu / a.lutas, 100.0 * b.vitoriasCpu / b.lutas, b.pulos / min,
                100.0 * b.chegou / b.quadros, b.danoBola / min / 140.0));
        }
        System.out.println(String.format(Locale.ROOT, "| **todos** | **%.0f%%** | **%.0f%%** | | | |", 100.0 * v5 / tot, 100.0 * v8 / tot));
    }
}
