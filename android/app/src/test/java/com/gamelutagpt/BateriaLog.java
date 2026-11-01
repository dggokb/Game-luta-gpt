package com.gamelutagpt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Log de luta (docs/balanco/tres-frentes.md): o "jogador" roteirizado contra a CPU de cada nível do
 * arcade (5 a 8), com o que aconteceu em cada bola, cada pulo, cada combo e cada Super, não só quem
 * venceu. Três jogadores:
 *
 * <ul>
 *   <li><b>meialua</b>: solta a Onda sempre que pode, segura a defesa baixa entre uma e outra e tira
 *   do ar com o 2H quem pula perto (o que o jogador fez para zerar o arcade).</li>
 *   <li><b>pulador</b>: anda até a meia distância, pula para frente, solta o jH descendo e, se
 *   acertou, emenda L, M e H ao cair.</li>
 *   <li><b>cpu</b>: a CPU nível 8 no lugar do jogador (a referência de quem joga bem).</li>
 * </ul>
 *
 * Roda à mão: {@code java ... com.gamelutagpt.BateriaLog [lutas por nível] [jogador ...]}
 */
public final class BateriaLog {
    private BateriaLog() {}

    static final String[] RIVAIS = {"p02", "p03", "p04", "p05", "p06", "p07", "p08", "p09",
        "p10", "p11", "p12", "p13", "p14", "p15", "p16", "player_base"};

    // ---------------------------------------------------------------- os jogadores

    static BateriaBalanco.Mao meiaLua() {
        int[] k = {0};
        return (luta, lado, in) -> {
            CombatFighter me = luta.f(lado), cpu = luta.f(1 - lado);
            in.clear();
            if (!me.grounded || !me.canAct()) return;
            float gap = Math.abs(cpu.x - me.x);
            // Ele pulou perto: 2H (antiaéreo).
            if (!cpu.grounded && gap < 260f && cpu.status == CombatFighter.Status.NEUTRAL) {
                in.direction = 3;
                in.heavy = true;
                return;
            }
            boolean minha = false;
            for (Projectile p : luta.e.energyProjectiles) if (p.ownerIndex == lado) minha = true;
            if (!minha && gap > 140f) {
                in.special = (k[0]++ % 3 == 0) ? "L" : (k[0] % 3 == 1 ? "M" : "H");
                return;
            }
            // Colado ou com bola na tela: defende baixo.
            in.direction = MotionParser.relative(4, me.facing);
        };
    }

    static BateriaBalanco.Mao pulador() {
        // estado: 0 chão, 1 no ar (sem apertar), 2 apertou o jH, 3..5 emendando L/M/H
        int[] st = {0, 0};
        return (luta, lado, in) -> {
            CombatFighter me = luta.f(lado), cpu = luta.f(1 - lado);
            in.clear();
            float gap = Math.abs(cpu.x - me.x);
            if (!me.grounded) {
                if (st[0] == 1 && me.vy > 0f && gap < 210f && me.canAct()) {
                    in.heavy = true;
                    st[0] = 2;
                }
                return;
            }
            boolean preso = cpu.status == CombatFighter.Status.HITSTUN || cpu.status == CombatFighter.Status.AIR_HITSTUN;
            if (st[0] >= 2) {
                // Caiu: emenda enquanto ele está preso.
                if (!preso && st[0] > 2) { st[0] = 0; return; }
                if (++st[1] % 6 == 0 || st[0] == 2) {
                    if (st[0] == 2) in.light = true;
                    else if (st[0] == 3) in.medium = true;
                    else if (st[0] == 4) in.heavy = true;
                    st[0] = st[0] >= 5 ? 0 : st[0] + 1;
                    if (st[0] == 0 || !preso && st[0] > 3) st[0] = 0;
                }
                return;
            }
            st[0] = 0;
            if (!me.canAct()) return;
            if (gap > 330f) {
                in.direction = MotionParser.relative(1, me.facing);
            } else if (gap >= 200f) {
                in.jump = true;
                in.direction = MotionParser.relative(8, me.facing);
                st[0] = 1;
            } else {
                in.direction = MotionParser.relative(4, me.facing);
                if ((luta.quadro / 20) % 3 == 0) in.light = true;
            }
        };
    }

    // ---------------------------------------------------------------- o log

    static final class Placar {
        int lutas, vitoriasJogador;
        double segundos;
        // Bolas do jogador
        int bolas, bolaAcertou, bolaDefendida, bolaAbsorvida;
        // Pulos (do jogador e da CPU)
        int[] pulos = new int[2], superPulos = new int[2], puloTirado = new int[2], puloAcertou = new int[2],
            puloDefendido = new int[2], puloNada = new int[2], puloCombo = new int[2];
        long[] danoPulo = new long[2];
        // Aberturas (sequências de acertos sem o rival se recuperar)
        int[] aberturas = new int[2], aberturasSoltas = new int[2], combos3 = new int[2];
        long[] acertosEmCombo = new long[2];
        int[] supers = new int[2], supersCru = new int[2];
        long[] dano = new long[2];
    }

    static final class Rastreio {
        boolean[] noAr = new boolean[2];
        int[] puloAte = new int[2];
        boolean[] puloResolvido = {true, true};
        int[] combo = new int[2];          // acertos seguidos do lado (no rival)
        int[] comboDoPulo = new int[2];
        boolean[] comboVeioDoPulo = new boolean[2];
    }

    static String nomeLado(int lado) { return lado == 0 ? "J" : "CPU"; }

    /** Uma luta com o log (quando {@code narra}) e o placar somado. */
    static void luta(String rival, int nivel, long semente, String jogador, Placar p, boolean narra) {
        BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {"player_base"}, new String[] {rival}, nivel, semente,
            new CombatConfig());
        if (jogador.equals("meialua")) l.mao[0] = meiaLua();
        else if (jogador.equals("pulador")) l.mao[0] = pulador();
        else l.ai[0].setLevel(8);
        Rastreio r = new Rastreio();
        List<String> log = new ArrayList<>();
        java.util.Set<Projectile> vistas = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        while (!l.acabou() && l.quadro < 60 * 60 * 4) {
            l.passo();
            String t = String.format(Locale.ROOT, "%5.1fs", l.quadro / 60.0);
            for (Projectile pr : l.e.energyProjectiles) {
                if (pr.ownerIndex == 0 && !pr.mini && vistas.add(pr)) {
                    p.bolas++;
                    if (narra) log.add(t + " J solta a bola");
                }
            }
            if (l.e.cues().contains("JUST_DEFENSE_ABSORB")) {
                p.bolaAbsorvida++;
                if (narra) log.add(t + " CPU absorve a bola (defesa perfeita)");
            }
            if (narra && l.e.cues().contains("ENERGY_RUN")) log.add(t + " CPU corrida de energia");
            for (int lado = 0; lado < 2; lado++) {
                CombatFighter f = l.f(lado);
                // Pulo começou
                if (!f.grounded && !r.noAr[lado] && f.status == CombatFighter.Status.NEUTRAL) {
                    if (f.superJumping) p.superPulos[lado]++;
                    else p.pulos[lado]++;
                    r.puloResolvido[lado] = false;
                    r.puloAte[lado] = -1;
                    if (narra) log.add(t + " " + nomeLado(lado) + (f.superJumping ? " super pulo" : " pula") +
                        (lado == 1 ? " [" + l.ai[1].lastReason + "] a " + Math.round(Math.abs(l.f(0).x - l.f(1).x)) + " px" : ""));
                }
                if (f.grounded && r.noAr[lado] && r.puloAte[lado] < 0) r.puloAte[lado] = l.quadro + 12;
                if (r.puloAte[lado] >= 0 && l.quadro > r.puloAte[lado] && !r.puloResolvido[lado]) {
                    r.puloResolvido[lado] = true;
                    p.puloNada[lado]++;
                }
                r.noAr[lado] = !f.grounded;
            }
            for (CombatEngine.HitEvent h : l.e.events()) {
                int a = h.attacker, d = h.defender;
                if (h.projectile && a == 0) {
                    if (h.blocked) p.bolaDefendida++; else p.bolaAcertou++;
                }
                if (narra) log.add(String.format(Locale.ROOT, "%s   %s %s %s (%s) a %.0f px", t, nomeLado(a), h.blocked ? "defendido:" : "acerta", h.moveId,
                    h.projectile ? "bola" : "golpe", Math.abs(l.f(0).x - l.f(1).x)) + (a == 0 ? " [CPU: " + l.ai[1].lastReason + "]" : ""));
                if (!h.blocked) p.dano[a] += h.damage;
                // Pulo de alguém resolvido por este contato
                if (!r.puloResolvido[d] && (r.noAr[d] || r.puloAte[d] >= 0) && !h.blocked) {
                    r.puloResolvido[d] = true;
                    p.puloTirado[d]++;
                    if (narra) log.add(t + " " + nomeLado(d) + " tirado do ar (" + h.moveId + ")");
                }
                if (!r.puloResolvido[a] && (r.noAr[a] || r.puloAte[a] >= 0)) {
                    r.puloResolvido[a] = true;
                    if (h.blocked) {
                        p.puloDefendido[a]++;
                        if (narra) log.add(t + " " + nomeLado(a) + " entra pulando: defendido");
                    } else {
                        p.puloAcertou[a]++;
                        r.comboVeioDoPulo[a] = true;
                        p.danoPulo[a] += h.damage;
                        if (narra) log.add(t + " " + nomeLado(a) + " entra pulando: acertou " + h.moveId);
                    }
                }
                if (h.blocked) continue;
                if (r.combo[a] == 0) p.aberturas[a]++;
                r.combo[a]++;
                if ("SUPER".equals(h.moveId)) {
                    p.supers[a]++;
                    if (r.combo[a] == 1) {
                        p.supersCru[a]++;
                        if (narra) log.add(t + " " + nomeLado(a) + " Super cru");
                    } else if (narra) log.add(t + " " + nomeLado(a) + " fecha com Super");
                }
                if (r.comboVeioDoPulo[a] && !h.moveId.startsWith("j")) p.danoPulo[a] += h.damage;
            }
            // Combo acaba quando o rival volta a agir
            for (int a = 0; a < 2; a++) {
                CombatFighter d = l.f(1 - a);
                if (r.combo[a] > 0 && (d.canAct() || d.status == CombatFighter.Status.BLOCKSTUN)) {
                    if (r.combo[a] == 1) p.aberturasSoltas[a]++;
                    if (r.combo[a] >= 3) p.combos3[a]++;
                    p.acertosEmCombo[a] += r.combo[a];
                    if (r.comboVeioDoPulo[a] && r.combo[a] >= 2) p.puloCombo[a]++;
                    if (narra && r.combo[a] >= 2) log.add(String.format(Locale.ROOT, "%5.1fs %s combo de %d", l.quadro / 60.0, nomeLado(a), r.combo[a]));
                    r.combo[a] = 0;
                    r.comboVeioDoPulo[a] = false;
                }
            }
        }
        p.lutas++;
        p.segundos += l.quadro / 60.0;
        if (l.flow.winner() == 0) p.vitoriasJogador++;
        if (narra) {
            System.out.println("#### " + jogador + " (Ryūga) × " + rival + ", CPU nível " + nivel + ": " +
                (l.flow.winner() == 0 ? "jogador vence" : "CPU vence") + String.format(Locale.ROOT, " em %.0f s", l.quadro / 60.0));
            System.out.println("```");
            for (String s : log.subList(0, Math.min(log.size(), 90))) System.out.println(s);
            if (log.size() > 90) System.out.println("  ... (" + (log.size() - 90) + " linhas a mais)");
            System.out.println("```");
        }
    }

    static String pct(int a, int b) { return b == 0 ? "—" : String.format(Locale.ROOT, "%.0f%%", 100.0 * a / b); }

    static void tabela(String jogador, int n) {
        System.out.println("### Jogador: " + jogador + " (Ryūga), " + n + " lutas por nível (os 16 rivais)");
        System.out.println();
        System.out.println("| medida | nível 5 | nível 6 | nível 7 | nível 8 |");
        System.out.println("|---|---|---|---|---|");
        Placar[] ps = new Placar[4];
        for (int i = 0; i < 4; i++) {
            ps[i] = new Placar();
            for (int s = 0; s < n; s++) luta(RIVAIS[s % RIVAIS.length], 5 + i, 7000 + s, jogador, ps[i], false);
        }
        linha("jogador vence", ps, p -> pct(p.vitoriasJogador, p.lutas));
        linha("duração média (s)", ps, p -> String.format(Locale.ROOT, "%.0f", p.segundos / p.lutas));
        if (jogador.equals("meialua")) {
            linha("bolas por luta", ps, p -> String.format(Locale.ROOT, "%.1f", p.bolas / (double) p.lutas));
            linha("bola acerta / defendida / absorvida", ps, p -> pct(p.bolaAcertou, p.bolas) + " / " + pct(p.bolaDefendida, p.bolas) + " / " + pct(p.bolaAbsorvida, p.bolas));
        }
        for (int lado = 0; lado < 2; lado++) {
            final int s = lado;
            String quem = lado == 0 ? "jogador" : "CPU";
            linha(quem + ": pulos / super pulos por minuto", ps, p -> String.format(Locale.ROOT, "%.1f / %.1f",
                p.pulos[s] / (p.segundos / 60), p.superPulos[s] / (p.segundos / 60)));
            linha(quem + ": pulo tirado do ar / acertou / defendido / nada", ps, p -> {
                int t = Math.max(1, p.pulos[s] + p.superPulos[s]);
                return pct(p.puloTirado[s], t) + " / " + pct(p.puloAcertou[s], t) + " / " + pct(p.puloDefendido[s], t) + " / " + pct(p.puloNada[s], t);
            });
            linha(quem + ": pulo que acertou virou combo", ps, p -> pct(p.puloCombo[s], p.puloAcertou[s]));
            linha(quem + ": aberturas de 1 acerto só / combos de 3+", ps, p -> pct(p.aberturasSoltas[s], p.aberturas[s]) + " / " + pct(p.combos3[s], p.aberturas[s]));
            linha(quem + ": acertos por abertura", ps, p -> String.format(Locale.ROOT, "%.1f", p.acertosEmCombo[s] / (double) Math.max(1, p.aberturas[s])));
            linha(quem + ": Supers por luta (crus)", ps, p -> String.format(Locale.ROOT, "%.1f (%.1f)", p.supers[s] / (double) p.lutas, p.supersCru[s] / (double) p.lutas));
        }
        System.out.println();
    }

    interface Col { String v(Placar p); }

    static void linha(String nome, Placar[] ps, Col c) {
        StringBuilder b = new StringBuilder("| " + nome + " |");
        for (Placar p : ps) b.append(' ').append(c.v(p)).append(" |");
        System.out.println(b);
    }

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 48;
        String[] jogadores = args.length > 1 ? java.util.Arrays.copyOfRange(args, 1, args.length) : new String[] {"meialua", "pulador", "cpu"};
        System.out.println("# Log de luta: o jogador contra a CPU do arcade (níveis 5 a 8)");
        System.out.println();
        for (String j : jogadores) {
            if (j.startsWith("narra:")) {
                String[] q = j.split(":");   // narra:jogador:rival:nivel
                luta(q[2], Integer.parseInt(q[3]), 7001, q[1], new Placar(), true);
                continue;
            }
            tabela(j, n);
        }
    }
}
