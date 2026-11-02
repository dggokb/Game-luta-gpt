package com.gamelutagpt;

import java.util.*;

/**
 * Pressão da CPU e o que ela faz depois do antiaéreo (pedido de 02/11: "ela não pressiona, não
 * pune"; no registro real ela atacava 22–26 vezes por minuto e o jogador 43–51). A CPU (níveis
 * 5 a 8, lutador sorteado) contra dois jogadores roteirizados: o laço do registro (rasteira,
 * pulo com jM; BateriaLevantar) e um que defende mais (segura a defesa de perto, cutuca, pula
 * às vezes). Mede por minuto os golpes e agarrões dela, se ela segue em cima depois da
 * sequência defendida, o dano e as vitórias; no antiaéreo que acertou, o dano dela nos 4 s
 * seguintes (emenda no ar ou em cima de quem caiu).
 *
 * <pre>args: [lutas por nível] [modo: antes|pressao|antiaereo|agora] [jogador: laco|guarda|ambos] [níveis...]</pre>
 * Modo "duelo": a CPU nova contra a antiga, mesmo nível, os 16 sorteados.
 */
public final class BateriaPressao {
    private BateriaPressao() {}

    static final class Est {
        int lutas, vitorias, ataques, agarroes, defendidas, seguiu, antiAereos;
        long quadros, danoCpu, danoJog, danoDepoisAA, golpesDepoisAA;
        /** Por comportamento novo (pelo motivo dela): [tentou, acertou, defendido]. */
        final Map<String, int[]> novos = new TreeMap<>();
    }

    static String tipo(String razao) {
        if (razao == null) return null;
        if (razao.startsWith("antiaéreo: emenda")) return "emenda do antiaéreo";
        if (razao.startsWith("antiaéreo: pula atrás e bate")) return "pulo atrás do antiaéreo";
        if (razao.startsWith("golpe em cima do levantar")) return "golpe no levantar";
        if (razao.startsWith("pressão: emenda")) return "pressão: golpe";
        if (razao.startsWith("pressão: ") || razao.startsWith("espera ele levantar para agarrar")) return "pressão/levantar: agarrão";
        return null;
    }

    /** O jogador que defende mais: segura a defesa de perto, cutuca com 2M e L, pula de meia distância. */
    static BateriaBalanco.Mao guarda(long semente) {
        Random r = new Random(semente);
        int[] plano = {0}, resta = {0};
        boolean[] noArBateu = {false};
        return (luta, lado, in) -> {
            CombatFighter me = luta.f(lado), cpu = luta.f(1 - lado);
            in.clear();
            float gap = Math.abs(cpu.x - me.x);
            if (!me.grounded) {
                if (!noArBateu[0] && me.vy > 0f && gap < 200f && me.canAct()) {
                    in.medium = true;
                    noArBateu[0] = true;
                }
                return;
            }
            noArBateu[0] = false;
            if (resta[0] > 0) resta[0]--;
            if (resta[0] == 0) {
                double x = r.nextDouble();
                if (gap > 330f) plano[0] = x < 0.6 ? 1 : 2;                       // anda / espera defendendo
                else if (gap > 160f) plano[0] = x < 0.3 ? 3 : x < 0.55 ? 1 : 2;   // pula / anda / defende
                else plano[0] = x < 0.5 ? 4 : x < 0.75 ? 5 : x < 0.88 ? 6 : x < 0.95 ? 7 : 8; // defende baixo, 2M, L, agarra, recua
                resta[0] = 18 + r.nextInt(30);
            }
            if (!me.canAct() && me.status != CombatFighter.Status.BLOCKSTUN) return;
            // Viu o golpe dela (com a reação de gente, 14 quadros) ou está defendendo: segura a
            // defesa (baixa contra quem está no chão) até ela parar.
            if (cpu.attacking() && cpu.attackFrame >= 14 || me.status == CombatFighter.Status.BLOCKSTUN) {
                in.direction = MotionParser.relative(cpu.grounded ? 4 : 5, me.facing);
                return;
            }
            switch (plano[0]) {
                case 1: in.direction = MotionParser.relative(1, me.facing); break;
                case 2: in.direction = MotionParser.relative(cpu.grounded ? 5 : 5, me.facing); break;
                case 3:
                    in.jump = true;
                    in.direction = MotionParser.relative(8, me.facing);
                    plano[0] = 2;
                    break;
                case 4: in.direction = MotionParser.relative(cpu.grounded ? 4 : 5, me.facing); break;
                case 5:
                    in.direction = MotionParser.relative(3, me.facing);
                    in.medium = true;
                    plano[0] = 4;
                    break;
                case 6:
                    in.light = true;
                    plano[0] = 4;
                    break;
                case 7:
                    in.grab = true;
                    plano[0] = 4;
                    break;
                default: in.direction = MotionParser.relative(5, me.facing);
            }
        };
    }

    static int rastroAA;

    static boolean preso(CombatFighter f) {
        return f.inHitstun() || f.status == CombatFighter.Status.AIR_HITSTUN;
    }

    static void luta(int nivel, long semente, String jogador, String modo, Est e) {
        Random r = new Random(semente);
        String cpu = BateriaBalanco.ELENCO[r.nextInt(BateriaBalanco.ELENCO.length)];
        String jog = BateriaBalanco.ELENCO[r.nextInt(BateriaBalanco.ELENCO.length)];
        BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {jog}, new String[] {cpu}, nivel, semente, new CombatConfig());
        AiController ai = l.ai[1];
        ai.pressaoLigada = modo.equals("pressao") || modo.equals("agora");
        ai.seguirAntiAereoLigado = modo.equals("antiaereo") || modo.equals("agora");
        if (jogador.equals("laco")) {
            AttackDefinition m2 = BateriaBalanco.ch(jog).attack("2M");
            l.mao[0] = BateriaLevantar.oki(new BateriaLevantar.Roteiro(semente * 7 + 1), m2 != null ? m2.startupFrames : 8);
        } else {
            l.mao[0] = guarda(semente * 13 + 5);
        }
        int esperaSeguir = -1;     // quadros desde que ele saiu da defesa (sequência dela defendida)
        boolean defendendo = false;
        int aaQuadro = -100_000;
        CombatFighter.Status antesC = CombatFighter.Status.NEUTRAL;
        String tipoAtual = null;
        CombatFighter.Status antesJ = CombatFighter.Status.NEUTRAL;
        while (!l.acabou() && l.quadro < 60 * 60 * 4) {
            CombatFighter j = l.f(0), c = l.f(1);
            boolean jNoAr = !j.grounded, cNoChao = c.grounded;
            l.passo();
            String t = tipo(ai.lastReason);
            if (t != null && c.attacking() && c.attackFrame == 1) { e.novos.computeIfAbsent(t, k -> new int[3])[0]++; tipoAtual = t; }
            else if (!c.attacking() && c.status != CombatFighter.Status.THROW) tipoAtual = null;
            if (agarrouAgora(c, antesC) && t != null) e.novos.computeIfAbsent(t, k -> new int[3])[0]++;
            if (j.status == CombatFighter.Status.THROWN && antesJ != CombatFighter.Status.THROWN && t != null && t.endsWith("agarrão"))
                e.novos.computeIfAbsent(t, k -> new int[3])[1]++;
            antesJ = j.status;
            for (CombatEngine.HitEvent h : l.e.events()) {
                if (h.attacker == 1 && tipoAtual != null && !tipoAtual.endsWith("agarrão")) {
                    int[] v = e.novos.computeIfAbsent(tipoAtual, k -> new int[3]);
                    if (h.blocked) v[2]++; else v[1]++;
                    tipoAtual = null;
                }
                if (h.attacker == 1) {
                    if (h.blocked) { defendendo = true; continue; }
                    e.danoCpu += h.damage;
                    if (l.quadro - aaQuadro <= 240) { e.danoDepoisAA += h.damage; e.golpesDepoisAA++; }
                    if (jNoAr && cNoChao && l.quadro - aaQuadro > 240) {
                        e.antiAereos++;
                        aaQuadro = l.quadro;
                        if (rastroAA > 0) { rastroAA--; System.out.println("--- AA " + h.moveId + " " + cpu + " x " + jog); }
                    }
                } else if (!h.blocked) {
                    e.danoJog += h.damage;
                }
            }
            if (rastroAA >= 0 && l.quadro - aaQuadro <= 150 && l.quadro - aaQuadro >= 0 && (l.quadro - aaQuadro) % 3 == 0)
                System.out.printf(Locale.ROOT, "  +%d J[%s y=%.0f stun=%d kd=%d] C[%s %s f%d] gap=%.0f razão=%s%n", l.quadro - aaQuadro,
                    j.status, j.y, j.stunLeft, j.knockdownFrame, c.status, c.attack != null ? c.attack.id : "-", c.attackFrame,
                    Math.abs(c.x - j.x), ai.lastReason);
            boolean agarrou = c.status == CombatFighter.Status.THROW && antesC != CombatFighter.Status.THROW;
            antesC = c.status;
            boolean comeca = c.attacking() && c.attackFrame == 1 || agarrou;
            if (c.attacking() && c.attackFrame == 1) e.ataques++;
            if (agarrou) e.agarroes++;
            if (defendendo && j.status != CombatFighter.Status.BLOCKSTUN) {
                defendendo = false;
                if (Math.abs(c.x - j.x) < 240f && j.grounded) {
                    e.defendidas++;
                    esperaSeguir = 0;
                }
            } else if (esperaSeguir >= 0) {
                if (j.status == CombatFighter.Status.BLOCKSTUN || preso(j) || comeca) {
                    e.seguiu++;
                    esperaSeguir = -1;
                } else if (++esperaSeguir > 30) {
                    esperaSeguir = -1;
                }
            }
        }
        e.lutas++;
        e.quadros += l.quadro;
        if (l.flow.winner() == 1) e.vitorias++;
    }

    static boolean agarrouAgora(CombatFighter c, CombatFighter.Status antes) {
        return c.status == CombatFighter.Status.THROW && antes != CombatFighter.Status.THROW;
    }

    static String linha(int nivel, String modo, Est e) {
        double min = e.quadros / 3600.0;
        return String.format(Locale.ROOT, "| %d | %s | %s | %.0f | %.1f | %s de %d | %.0f | %.0f | %d | %.0f | %.1f |", nivel, modo,
            BateriaVoadora.pc(e.vitorias, e.lutas), e.ataques / min, e.agarroes / min,
            BateriaVoadora.pc(e.seguiu, Math.max(1, e.defendidas)), e.defendidas, e.danoCpu / min, e.danoJog / min,
            e.antiAereos, e.danoDepoisAA / (double)Math.max(1, e.antiAereos), e.golpesDepoisAA / (double)Math.max(1, e.antiAereos));
    }

    /** A CPU nova (lado 1) contra a antiga (lado 0), mesmo nível; devolve as vitórias da nova. */
    static String dueloModo = "agora";

    static int[] duelo(int nivel, int n) {
        int novas = 0, lutas = 0;
        for (int i = 0; i < n; i++) {
            Random r = new Random(995_000 + nivel * 1000L + i);
            String a = BateriaBalanco.ELENCO[r.nextInt(BateriaBalanco.ELENCO.length)];
            String b = BateriaBalanco.ELENCO[r.nextInt(BateriaBalanco.ELENCO.length)];
            for (int lado = 0; lado < 2; lado++) {
                // Cada par dos dois lados: a nova uma vez com cada lutador.
                String[] t0 = {lado == 0 ? a : b}, t1 = {lado == 0 ? b : a};
                BateriaBalanco.Luta l = new BateriaBalanco.Luta(t0, t1, nivel, 996_000 + nivel * 1000L + i * 2 + lado, new CombatConfig());
                l.ai[0].pressaoLigada = l.ai[0].seguirAntiAereoLigado = false;
                l.ai[1].pressaoLigada = !dueloModo.equals("antiaereo");
                l.ai[1].seguirAntiAereoLigado = !dueloModo.equals("pressao");
                while (!l.acabou() && l.quadro < 60 * 60 * 4) l.passo();
                lutas++;
                if (l.flow.winner() == 1) novas++;
            }
        }
        return new int[] {novas, lutas};
    }

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 80;
        String modo = args.length > 1 ? args[1] : "comparar";
        String jogadores = args.length > 2 ? args[2] : "ambos";
        int[] niveis = args.length > 3 ? Arrays.stream(args, 3, args.length).mapToInt(Integer::parseInt).toArray() : new int[] {5, 7, 8};
        if (modo.equals("rastroAA")) {
            rastroAA = 4;
            Est e = new Est();
            for (int i = 0; i < 12 && rastroAA > 0; i++) luta(8, 990_000L + 80_000L + i, "laco", jogadores, e);
            return;
        }
        rastroAA = -1;
        if (jogadores.contains(":")) {
            for (String kv : jogadores.substring(jogadores.indexOf(':') + 1).split(",")) {
                String[] a = kv.split("=");
                double v = Double.parseDouble(a[1]);
                switch (a[0]) {
                    case "p7": AiController.PRESSAO_7 = v; break;
                    case "p8": AiController.PRESSAO_8 = v; break;
                    case "agarra": AiController.PRESSAO_AGARRA = v; break;
                    case "oki": AiController.OKI_AGARRA = v; break;
                    default: throw new IllegalArgumentException(a[0]);
                }
            }
            jogadores = jogadores.substring(0, jogadores.indexOf(':'));
        }
        if (modo.equals("duelo")) {
            dueloModo = jogadores.equals("ambos") ? "agora" : jogadores.replace("-sempulo", "");
            AiController.puloAtrasLigado = !jogadores.contains("-sempulo");
            System.out.println("# CPU nova (" + dueloModo + ") contra a antiga (mesmo nível, " + n + " pares × 2 lados)");
            System.out.println();
            for (int nivel : niveis) {
                int[] d = duelo(nivel, n);
                System.out.println("- nível " + nivel + ": a nova vence " + BateriaVoadora.pc(d[0], d[1]) + " (" + d[0] + " de " + d[1] + ")");
            }
            return;
        }
        String[] modos = modo.equals("comparar") ? new String[] {"antes", "pressao", "antiaereo", "agora"} : new String[] {modo};
        for (String jog : jogadores.equals("ambos") ? new String[] {"laco", "guarda"} : new String[] {jogadores}) {
            System.out.println();
            System.out.println("## Contra o jogador \"" + jog + "\" (" + n + " lutas por nível)");
            System.out.println();
            System.out.println("| nível | CPU | CPU vence | golpes/min | agarrões/min | segue em cima depois da sequência defendida | dano dela/min | dano dele/min | antiaéreos | dano depois do antiaéreo | golpes depois |");
            System.out.println("|---|---|---|---|---|---|---|---|---|---|---|");
            for (int nivel : niveis) {
                for (String m : modos) {
                    Est e = new Est();
                    for (int i = 0; i < n; i++) luta(nivel, 990_000L + nivel * 10_000L + i, jog, m, e);
                    System.out.println(linha(nivel, m, e));
                    if (!e.novos.isEmpty()) {
                        StringBuilder b = new StringBuilder("  " + nivel + " " + m + ": ");
                        e.novos.forEach((k, v) -> b.append(k).append(' ').append(v[0]).append(" (acerta ").append(v[1]).append(", defendido ").append(v[2]).append("); "));
                        System.err.println(b);
                    }
                }
            }
        }
    }
}
