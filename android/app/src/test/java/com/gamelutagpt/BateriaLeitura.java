package com.gamelutagpt;

import java.util.Locale;
import java.util.Random;

/**
 * A CPU que lê o jogador (docs/balanco/cpu-adaptativa.md): a CPU nível 8, com a leitura ligada e
 * desligada, contra um "jogador" com um hábito fixo (a CPU nível 6 com uma mão que força o hábito).
 * Mesmas sementes nos dois lados da comparação. Roda à mão:
 *
 * <pre>java -cp ... com.gamelutagpt.BateriaLeitura [lutas por hábito]</pre>
 */
public final class BateriaLeitura {
    private BateriaLeitura() {}

    enum Habito {
        NENHUM("sem hábito forçado"),
        PULA_DEPOIS("pula para frente sempre que sai da defesa"),
        APERTA_AO_LEVANTAR("aperta L sempre que levanta"),
        SO_DEFENDE("colado, só defende (não ataca)"),
        PULA_A_BOLA("pula sempre que a bola vem");

        final String descricao;

        Habito(String d) { descricao = d; }
    }

    /** O hábito, por cima do que a CPU nível 6 decidiu. */
    static BateriaBalanco.Mao mao(Habito h) {
        CombatFighter.Status[] antes = {CombatFighter.Status.NEUTRAL};
        int[] segura = {0};
        return (luta, lado, in) -> {
            CombatFighter me = luta.f(lado), rival = luta.f(1 - lado);
            CombatFighter.Status st = me.status;
            if (segura[0] > 0) {
                segura[0]--;
                in.direction = MotionParser.relative(8, me.facing);
                in.light = in.medium = in.heavy = false;
            }
            switch (h) {
                case PULA_DEPOIS:
                    if (antes[0] == CombatFighter.Status.BLOCKSTUN && st == CombatFighter.Status.NEUTRAL) {
                        segura[0] = 3;
                        in.clear();
                        in.direction = MotionParser.relative(8, me.facing);
                    }
                    break;
                case APERTA_AO_LEVANTAR:
                    if (antes[0] == CombatFighter.Status.WAKEUP && st == CombatFighter.Status.NEUTRAL) {
                        in.clear();
                        in.light = true;
                    }
                    break;
                case SO_DEFENDE:
                    if (Math.abs(rival.x - me.x) < 260f && st == CombatFighter.Status.NEUTRAL && me.grounded) {
                        in.clear();
                        in.direction = MotionParser.relative(rival.crouching ? 4 : 5, me.facing);
                    }
                    break;
                case PULA_A_BOLA:
                    for (Projectile p : luta.e.energyProjectiles) {
                        if (p.ownerIndex != 1 - lado) continue;
                        float vem = (me.x - p.x) * p.direction;
                        if (vem > 0 && vem < 260f && me.grounded && st == CombatFighter.Status.NEUTRAL) {
                            in.clear();
                            in.direction = MotionParser.relative(8, me.facing);
                        }
                    }
                    break;
                default:
                    break;
            }
            antes[0] = st;
        };
    }

    static final class Placar {
        int lutas, vitorias, antiaereos, agarroes, contraNoLevantar, punicoes;
        long danoFeito, danoLevado;
        double quadros;

        String linha() {
            double min = quadros / 3600.0;
            return String.format(Locale.ROOT, "%.0f%% | %.2f | %.1f | %.1f | %.1f",
                100.0 * vitorias / lutas, danoLevado == 0 ? 0 : (double) danoFeito / danoLevado,
                antiaereos / min, agarroes / min, contraNoLevantar / min);
        }
    }

    /** {@code seguidas} lutas com a mesma CPU (a leitura passa de uma para a outra se {@code memoria}). */
    static Placar[] jogar(Habito h, boolean ler, boolean memoria, int sequencias, int seguidas, long base) {
        Placar[] p = new Placar[seguidas];
        for (int k = 0; k < seguidas; k++) p[k] = new Placar();
        for (int s = 0; s < sequencias; s++) {
            Random r = new Random(base + s);
            String a = BateriaBalanco.ELENCO[r.nextInt(16)], b = BateriaBalanco.ELENCO[r.nextInt(16)];
            CpuLeitura leitura = new CpuLeitura();
            for (int k = 0; k < seguidas; k++) {
                if (!memoria) leitura = new CpuLeitura();
                else leitura.novaLuta();
                BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {a}, new String[] {b}, 8, base * 7 + s * 31 + k, new CombatConfig());
                l.ai[0].setLeitura(leitura);
                l.ai[0].setLeituraAtiva(ler);
                l.ai[1].setLevel(6);
                l.mao[1] = mao(h);
                CombatFighter.Status rivalAntes = CombatFighter.Status.NEUTRAL;
                int levantou = -1000;
                int quadros = 0;
                while (!l.acabou() && l.quadro < 60 * 60 * 3) {
                    l.passo();
                    quadros++;
                    CombatFighter rival = l.f(1);
                    if (rivalAntes == CombatFighter.Status.WAKEUP && rival.status != CombatFighter.Status.WAKEUP) levantou = l.quadro;
                    for (CombatEngine.HitEvent ev : l.e.events()) {
                        if (ev.attacker == 0 && !ev.blocked) {
                            p[k].danoFeito += ev.damage;
                            if (!rival.grounded && l.f(0).grounded && !ev.projectile) p[k].antiaereos++;
                            if ("THROW".equals(ev.moveId)) p[k].agarroes++;
                            if (l.quadro - levantou < 30) p[k].contraNoLevantar++;
                        }
                        if (ev.attacker == 1 && !ev.blocked) p[k].danoLevado += ev.damage;
                    }
                    rivalAntes = rival.status;
                }
                p[k].lutas++;
                p[k].quadros += quadros;
                if (l.flow.winner() == 0) p[k].vitorias++;
            }
        }
        return p;
    }

    /** Depuração: o que a CPU leu e o que ela fez por causa disso, num hábito. */
    static void depurar(Habito h, int lutas) {
        java.util.Map<String, Integer> razoes = new java.util.TreeMap<>();
        CpuLeitura leitura = new CpuLeitura();
        for (int s = 0; s < lutas; s++) {
            Random r = new Random(9_000 + s);
            String a = BateriaBalanco.ELENCO[r.nextInt(16)], b = BateriaBalanco.ELENCO[r.nextInt(16)];
            leitura.novaLuta();
            BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {a}, new String[] {b}, 8, 77 + s, new CombatConfig());
            l.ai[0].setLeitura(leitura);
            l.ai[1].setLevel(6);
            l.mao[1] = mao(h);
            String antes = "";
            while (!l.acabou() && l.quadro < 60 * 60 * 3) {
                l.passo();
                String rz = l.ai[0].lastReason;
                if (rz != null && rz.startsWith("leu") && !rz.equals(antes)) razoes.merge(rz, 1, Integer::sum);
                antes = rz == null ? "" : rz;
            }
        }
        System.out.println("## " + h.descricao + ": " + leitura.resumo());
        razoes.forEach((k, v) -> System.out.println("- " + k + ": " + v));
    }

    /** Depuração da punição: o rival só solta a rasteira (2M) de perto; o que a CPU faz depois de defender. */
    static void depurarPunicao(int lutas) {
        java.util.Map<String, Integer> razoes = new java.util.TreeMap<>();
        int defendidas = 0, punidas = 0;
        java.util.List<Float> gaps = new java.util.ArrayList<>();
        for (int s = 0; s < lutas; s++) {
            Random r = new Random(12_000 + s);
            String a = BateriaBalanco.ELENCO[r.nextInt(16)], b = BateriaBalanco.ELENCO[r.nextInt(16)];
            BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {a}, new String[] {b}, 8, 99 + s, new CombatConfig());
            l.mao[1] = (luta, lado, in) -> {
                CombatFighter me = luta.f(lado), o = luta.f(1 - lado);
                if (me.status == CombatFighter.Status.NEUTRAL && me.grounded && Math.abs(o.x - me.x) < 260f) {
                    in.clear();
                    in.direction = 3;
                    in.medium = true;
                } else if (me.status == CombatFighter.Status.NEUTRAL && me.grounded) {
                    in.clear();
                    in.direction = MotionParser.relative(1, me.facing);
                }
            };
            int defendeu = -1000;
            boolean contou = false;
            String razaoAntes = "";
            AttackDefinition golpe = null;
            float gapPunicao = 0;
            int restava = 0;
            CombatFighter.Outcome deu = CombatFighter.Outcome.NONE;
            while (!l.acabou() && l.quadro < 60 * 60 * 2) {
                l.passo();
                for (CombatEngine.HitEvent ev : l.e.events()) {
                    if (ev.attacker == 1 && ev.blocked && "2M".equals(ev.moveId)) {
                        defendeu = l.quadro;
                        defendidas++;
                        contou = false;
                    }
                    if (ev.attacker == 0 && !ev.blocked && !contou && l.quadro - defendeu <= 30) {
                        punidas++;
                        contou = true;
                    }
                }
                String rzAgora = l.ai[0].lastReason == null ? "" : l.ai[0].lastReason;
                if (rzAgora.startsWith("punir na defesa") && !rzAgora.equals(razaoAntes)) {
                    gapPunicao = Math.abs(l.f(1).x - l.f(0).x);
                    restava = l.f(1).attacking() ? l.f(1).attack.totalFrames - l.f(1).attackFrame : -1;
                    golpe = null;
                }
                if (golpe == null && rzAgora.startsWith("punir na defesa") && l.f(0).attacking()) {
                    golpe = l.f(0).attack;
                    deu = CombatFighter.Outcome.NONE;
                }
                if (golpe != null && l.f(0).attack == golpe && l.f(0).outcome != CombatFighter.Outcome.NONE) deu = l.f(0).outcome;
                if (golpe != null && (!l.f(0).attacking() || l.f(0).attack != golpe)) {
                    razoes.merge(String.format(Locale.ROOT, "  resultado %s %s (dist %.0f, restavam %d)", golpe.id,
                        deu, Math.round(gapPunicao / 40f) * 40f, restava), 1, Integer::sum);
                    golpe = null;
                }
                razaoAntes = rzAgora;
                if (l.quadro - defendeu > 0 && l.quadro - defendeu <= 30 && l.f(0).canAct() && l.f(0).status == CombatFighter.Status.NEUTRAL
                    && l.quadro - defendeu < 31) {
                    String rz = l.ai[0].lastReason;
                    razoes.merge(rz == null ? "" : rz, 1, Integer::sum);
                    if (l.quadro - defendeu == 1 || l.f(1).attacking()) gaps.add(Math.abs(l.f(1).x - l.f(0).x));
                }
            }
        }
        System.out.println("2M defendidas: " + defendidas + ", punidas em 30 quadros: " + punidas);
        gaps.sort(null);
        if (!gaps.isEmpty()) System.out.println("distância ao sair da defesa (mediana): " + gaps.get(gaps.size() / 2));
        razoes.forEach((k, v) -> System.out.println("- " + k + ": " + v));
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("punir")) {
            depurarPunicao(40);
            return;
        }
        if (args.length > 0 && args[0].equals("depurar")) {
            for (Habito h : Habito.values()) depurar(h, 20);
            return;
        }
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 200;
        System.out.println("# A CPU que lê o jogador");
        System.out.println();
        System.out.println("CPU nível 8 contra a CPU nível 6 com um hábito forçado, 1x1, " + n + " lutas por linha, mesmas sementes.");
        System.out.println("Colunas: vitórias da CPU 8 | dano feito ÷ levado | antiaéreos por minuto | agarrões por minuto | acertos logo depois de ele levantar, por minuto.");
        System.out.println();
        System.out.println("| hábito do jogador | leitura | vitórias | dano feito ÷ levado | antiaéreos/min | agarrões/min | acertos no levantar/min |");
        System.out.println("|---|---|---|---|---|---|---|");
        for (Habito h : Habito.values()) {
            for (boolean ler : new boolean[] {false, true}) {
                Placar todas = soma(jogar(h, ler, true, n / 5, 5, 4_000_000L + h.ordinal() * 10_000));
                System.out.println("| " + h.descricao + " | " + (ler ? "ligada" : "desligada") + " | " + todas.linha().replace(" | ", " | ") + " |");
            }
        }
        System.out.println();
        System.out.println("## Memória entre lutas (5 lutas seguidas contra o mesmo jogador; hábito: pula depois de defender)");
        System.out.println();
        System.out.println("| luta | sem memória (cada luta do zero) | com memória (a leitura passa) |");
        System.out.println("|---|---|---|");
        Placar[] sem = jogar(Habito.PULA_DEPOIS, true, false, n / 5, 5, 5_000_000L);
        Placar[] com = jogar(Habito.PULA_DEPOIS, true, true, n / 5, 5, 5_000_000L);
        for (int k = 0; k < 5; k++) {
            System.out.println("| " + (k + 1) + "ª | " + sem[k].linha() + " | " + com[k].linha() + " |");
        }
    }

    static Placar soma(Placar[] ps) {
        Placar t = new Placar();
        for (Placar p : ps) {
            t.lutas += p.lutas;
            t.vitorias += p.vitorias;
            t.antiaereos += p.antiaereos;
            t.agarroes += p.agarroes;
            t.contraNoLevantar += p.contraNoLevantar;
            t.danoFeito += p.danoFeito;
            t.danoLevado += p.danoLevado;
            t.quadros += p.quadros;
        }
        return t;
    }
}
