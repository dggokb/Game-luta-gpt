package com.gamelutagpt;

import java.util.*;

/**
 * Protótipo do SHINKAI individual do Ryūga (PERCEPCAO; docs/balanco/shinkai-ryuga.md) contra o
 * SHINKAI antigo. As mesmas lutas (sementes, lados, rivais) rodam duas vezes: com o Ryūga no
 * protótipo e com ele no ENERGY antigo. O rival usa o SHINKAI antigo do tipo dele nas duas.
 *
 * <pre>
 * args: [lutas por rival] [nível] [narra]
 *   lutas por rival (padrão 8), nível da CPU (padrão 8), "narra" imprime o log de 3 lutas
 * </pre>
 */
public class BateriaShinkaiRyuga {
    static final class Est {
        int lutas, decididas, vitorias, janelas, clashes;
        long quadrosShinkai, danoDado, danoLevado;            // dentro do SHINKAI do Ryūga
        long danoDadoFora, danoLevadoFora, quadrosFora;       // fora dele
        long danoDadoRival, danoLevadoRival, quadrosRival;    // dentro do SHINKAI do rival
        long leituras, esquivas, precisos, rupturas, voluntarios, limites, reativacoes;
        long quadrosExausto, danoLevadoExausto, dor;
        long quadrosLuta, rupturasRival, leiturasRival;
        long clashDecididos, clashVenceLuta, clashComVencedor, clashMedidoVence;
        long desgasteNoClash, clashesDoRyuga, segundosNoClash;   // o corpo dele quando o Clash começa   // quem venceu o Clash venceu a luta?
        long danoPreciso, defesas, perfeitas, golposRival;
        final Map<String, int[]> porRival = new TreeMap<>();  // rival → {vitórias, decididas}
    }

    static final String RYUGA = "player_base";
    /** Quem é medido (o lado r): o Ryūga, ou cada um no modo elenco. */
    static String medido = RYUGA;
    /** Modo elenco: todos contra todos (os 16), para o que muda o Clash de todo mundo. */
    static boolean elenco;
    /** Modo perdedor: a parte do SHINKAI que o perdedor do Clash guarda, por coluna. */
    static boolean modoPerdedor;
    static final String[] VARIANTES_PERDEDOR = {"perdedor perde tudo (antes)", "fica com metade do que tinha", "fica com tudo (agora)",
        "tudo, Ryūga sem Ruptura", "tudo, sem reação rápida", "antigo (ENERGY), perde tudo", "antigo (ENERGY), fica com tudo"};
    /** Modo assinatura: (a) leitura na defesa comum, (b) contra-golpe preciso. */
    static boolean modoAssinatura;
    static final String[] VARIANTES_ASSINATURA = {"antigo (ENERGY)", "protótipo (hoje)", "+ (a) defesa lê", "+ (b) contra-golpe preciso", "+ (a) e (b)"};
    static final int[] PERDEDOR = {0, 500, 1000, 1000, 1000, 0, 1000};
    static boolean semClash;
    /** Espelho: o Ryūga contra ele mesmo, os dois com o mesmo SHINKAI (o da coluna). */
    static boolean espelho;

    static void luta(String rival, int lado, int nivel, long semente, Est e, StringBuilder log) {
        BateriaBalanco.Luta l = new BateriaBalanco.Luta(
            lado == 0 ? new String[] {medido} : new String[] {rival},
            lado == 0 ? new String[] {rival} : new String[] {medido}, nivel, semente, new CombatConfig());
        int r = lado, o = 1 - lado;
        if (semClash) {
            // Ninguém responde o SHINKAI do outro com o seu (o Clash esconde a assinatura).
            BateriaBalanco.Mao m = (lu, ld, in) -> {
                if (lu.f(1 - ld).state.inShinkai() && !lu.f(ld).state.inShinkai()) in.shinkai = false;
            };
            l.mao[0] = m;
            l.mao[1] = m;
        }
        boolean dentro = false, dentroRival = false;
        int venceuClash = -1;
        long quadrosDesdeLigou = 0;
        int vidaR = l.f(r).state.life;
        while (!l.acabou() && l.quadro < 60 * 60 * 4) {
            int lifeR = l.f(r).state.life;
            l.passo();
            CombatFighter fr = l.f(r), fo = l.f(o);
            FighterState s = fr.state;
            boolean agora = s.inShinkai(), agoraRival = fo.state.inShinkai();
            List<String> cues = l.e.cues();
            if (agora && !dentro) quadrosDesdeLigou = 0;
            else if (agora) quadrosDesdeLigou++;
            if (agora && !dentro) {
                e.janelas++;
                if (log != null) nota(log, l, "Ryūga liga o SHINKAI " + s.shinkaiType.label);
            }
            if (!agora && dentro && log != null) {
                String como = cues.contains("LIMITE_FIM") ? "no limite (exausto)" : cues.contains("RUPTURA_FIM") ? "depois da Ruptura (exausto)"
                    : cues.contains("SHINKAI_FIM") ? "desligado por ele" : "acabou";
                nota(log, l, "SHINKAI do Ryūga " + como);
            }
            if (agoraRival && !dentroRival && log != null) nota(log, l, "rival liga o SHINKAI " + fo.state.shinkaiType.label);
            for (String c : cues) {
                if (c.equals("SHINKAI_CLASH")) {
                    e.clashes++;
                    if (s.shinkaiType == Shinkai.Type.PERCEPCAO) {
                        e.clashesDoRyuga++;
                        e.desgasteNoClash += s.sobrecarga;
                        e.segundosNoClash += quadrosDesdeLigou;
                    }
                }
                if (c.equals("SHINKAI_CLASH_WIN") && venceuClash < 0) venceuClash = vencedorDoClash(l);
                if (log != null && (c.equals("LEITURA") || c.equals("ESQUIVA") || c.equals("PRECISO") || c.equals("LIMITE") ||
                    c.equals("RUPTURA") || c.startsWith("SHINKAI_CLASH"))) nota(log, l, c + quem(l, c, r));
            }
            if (agora) e.quadrosShinkai++;
            else e.quadrosFora++;
            if (agoraRival) e.quadrosRival++;
            if (s.exaustaoQuadros > 0) e.quadrosExausto++;
            // A dor da Ruptura: vida que caiu sem evento de dano.
            int caiu = lifeR - s.life;
            if (agora && cues.contains("JUST_DEFENSE")) e.perfeitas++;
            if (agora && fo.attacking() && fo.attackFrame == 1) e.golposRival++;
            for (CombatEngine.HitEvent h : l.e.events()) {
                if (agora && h.defender == r && h.blocked) e.defesas++;
                if (h.defender == r) caiu -= h.damage;
                if (h.damage <= 0) continue;
                if (h.attacker == r) {
                    if (agora || dentro) e.danoDado += h.damage; else e.danoDadoFora += h.damage;
                    if (agoraRival || dentroRival) e.danoDadoRival += h.damage;
                } else {
                    if (agora || dentro) e.danoLevado += h.damage; else e.danoLevadoFora += h.damage;
                    if (agoraRival || dentroRival) e.danoLevadoRival += h.damage;
                    if (s.exaustaoQuadros > 0) e.danoLevadoExausto += h.damage;
                }
            }
            if (s.emRuptura() && caiu > 0) e.dor += caiu;
            if (cues.contains("PRECISO")) {
                for (CombatEngine.HitEvent h : l.e.events()) if (h.attacker == r) e.danoPreciso += h.damage;
            }
            dentro = agora;
            dentroRival = agoraRival;
        }
        FighterState s = l.f(r).state;
        e.leituras += s.leituras;
        e.esquivas += s.esquivas;
        e.precisos += s.precisos;
        e.rupturas += s.rupturas;
        e.voluntarios += s.finsVoluntarios;
        e.limites += s.finsNoLimite;
        e.lutas++;
        e.quadrosLuta += l.quadro;
        e.rupturasRival += l.f(o).state.rupturas;
        e.leiturasRival += l.f(o).state.leituras;
        int v = l.flow.winner();
        if (venceuClash >= 0) {
            e.clashComVencedor++;
            if (venceuClash == r) e.clashMedidoVence++;
        }
        if (venceuClash >= 0 && (v == 0 || v == 1)) {
            e.clashDecididos++;
            if (v == venceuClash) e.clashVenceLuta++;
        }
        int[] pr = e.porRival.computeIfAbsent(rival, k -> new int[2]);
        if (v == 0 || v == 1) {
            e.decididas++;
            pr[1]++;
            if (v == r) {
                e.vitorias++;
                pr[0]++;
            }
        }
        if (log != null) {
            nota(log, l, (v == r ? "Ryūga vence" : v == o ? "rival vence" : "empate") + String.format(Locale.ROOT,
                " (leituras %d, esquivas %d, precisos %d, Rupturas %d)", s.leituras, s.esquivas, s.precisos, s.rupturas));
        }
    }

    /** Quem venceu o Clash neste quadro: quem acertou o golpe do Clash (o perdedor pode seguir no SHINKAI). */
    static int vencedorDoClash(BateriaBalanco.Luta l) {
        for (CombatEngine.HitEvent h : l.e.events()) if ("SHINKAI_CLASH".equals(h.moveId)) return h.attacker;
        return -1;
    }

    /** De quem é o aviso (os avisos do motor não dizem o lado). */
    static String quem(BateriaBalanco.Luta l, String c, int r) {
        int lado = -1;
        for (int i = 0; i < 2; i++) {
            FighterState s = l.f(i).state;
            boolean dele = c.equals("LIMITE") ? s.limiteQuadros > 0 : c.equals("RUPTURA") ? s.emRuptura()
                : c.equals("LEITURA") || c.equals("ESQUIVA") ? s.precisoQuadros == l.e.config.ryugaPrecisoJanela
                : c.equals("SHINKAI_CLASH_WIN") && vencedorDoClash(l) == i;
            if (dele) lado = i;
        }
        return lado < 0 ? "" : lado == r ? " (Ryūga medido)" : " (rival)";
    }

    static void nota(StringBuilder log, BateriaBalanco.Luta l, String o) {
        FighterState a = l.f(0).state, b = l.f(1).state;
        log.append(String.format(Locale.ROOT, "  %6.2f s  vida %5d × %5d  %s%n", l.quadro / 60.0, a.life, b.life, o));
    }

    /** As variantes comparadas (colunas). */
    static String[] VARIANTES = {"antigo (ENERGY)", "protótipo", "sem reação rápida da CPU", "sem Ruptura", "sem os dois"};
    /** Modo clash: o quanto do fôlego o Ryūga recupera ao vencer o Clash, por coluna. */
    static boolean modoClash;
    static final String[] VARIANTES_CLASH = {"antigo (ENERGY)", "fôlego 100% (hoje)", "fôlego 60%", "fôlego 40%", "fôlego 20%", "fôlego 0%"};
    static final int[] FOLEGOS = {0, 1000, 600, 400, 200, 0};

    static Est rodar(int variante, int n, int nivel) {
        Shinkai.prototipoDesligado = variante == 0;
        AiController.percepcaoReage = variante != 2 && variante != 4;
        java.util.function.Consumer<CombatConfig> antes = BateriaBalanco.AJUSTE_CFG;
        // Sem Ruptura: a janela do limite some antes de dar tempo de escolher (a CPU espera 12 quadros).
        if (!modoClash && (variante == 3 || variante == 4)) BateriaBalanco.AJUSTE_CFG = c -> c.ryugaLimiteJanela = 1;
        if (modoAssinatura) {
            AiController.percepcaoReage = true;
            boolean la = variante == 2 || variante == 4, lb = variante == 3 || variante == 4;
            BateriaBalanco.AJUSTE_CFG = c -> {
                if (la) c.ryugaLeituraLeveAbre = 14;
                c.ryugaContraPreciso = lb;
            };
        }
        if (modoPerdedor) {
            AiController.percepcaoReage = variante != 4;
            Shinkai.prototipoDesligado = variante >= 5;
            int parte = PERDEDOR[variante];
            boolean semRuptura = variante == 3;
            BateriaBalanco.AJUSTE_CFG = c -> {
                c.shinkaiClashPerdedorPermille = parte;
                if (semRuptura) c.ryugaLimiteJanela = 1;
            };
        }
        if (modoClash) {
            AiController.percepcaoReage = true;
            int folego = FOLEGOS[variante];
            BateriaBalanco.AJUSTE_CFG = c -> c.ryugaClashFolegoPermille = folego;
        }
        Est e = new Est();
        int k = 0;
        try {
            if (elenco) {
                // Os 16 todos contra todos, 2 lutas por par (uma de cada lado).
                for (String a : BateriaBalanco.ELENCO) for (String b : BateriaBalanco.ELENCO) {
                    if (a.equals(b)) continue;
                    medido = a;
                    for (int i = 0; i < 2; i++, k++) luta(b, i, nivel, 77_000 + k, e, null);
                }
                medido = RYUGA;
                return e;
            }
            for (String rival : espelho ? new String[] {RYUGA} : BateriaBalanco.ELENCO) {
                if (rival.equals(RYUGA) && !espelho) continue;
                if (espelho) n *= 15;   // o mesmo tanto de lutas que contra os 15
                for (int i = 0; i < n; i++, k++) luta(rival, i & 1, nivel, 77_000 + k, e, null);
            }
        } finally {
            Shinkai.prototipoDesligado = false;
            AiController.percepcaoReage = true;
            BateriaBalanco.AJUSTE_CFG = antes;
        }
        return e;
    }

    static String pc(long a, long b) { return String.format(Locale.ROOT, "%.0f%%", 100.0 * a / Math.max(1, b)); }

    static String porLuta(long v, Est e) { return String.format(Locale.ROOT, "%.2f", v / (double)Math.max(1, e.lutas)); }

    static String porQuadro(long dano, long quadros) {
        return String.format(Locale.ROOT, "%.1f", 60.0 * dano / Math.max(1, quadros));
    }

    public static void main(String[] args) {
        long t0 = System.nanoTime();
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 8;
        int nivel = args.length > 1 ? Integer.parseInt(args[1]) : 8;
        boolean narra = Arrays.asList(args).contains("narra");
        semClash = Arrays.asList(args).contains("semClash");
        espelho = Arrays.asList(args).contains("espelho");
        modoClash = Arrays.asList(args).contains("clash");
        if (modoClash) VARIANTES = VARIANTES_CLASH;
        modoPerdedor = Arrays.asList(args).contains("perdedor");
        if (modoPerdedor) VARIANTES = VARIANTES_PERDEDOR;
        elenco = Arrays.asList(args).contains("elenco");
        modoAssinatura = Arrays.asList(args).contains("assinatura");
        if (modoAssinatura) VARIANTES = VARIANTES_ASSINATURA;
        Est[] v = new Est[VARIANTES.length];
        for (int i = 0; i < v.length; i++) v[i] = rodar(i, n, nivel);
        System.out.println("# SHINKAI do Ryūga: protótipo (PERCEPÇÃO) × antigo (ENERGY)");
        System.out.println();
        if (elenco) System.out.printf(Locale.ROOT, "**Elenco:** os 16 todos contra todos, 2 lutas por par (%d lutas por coluna); as medidas \"dele\" são do lado medido.%n%n", 16 * 15 * 2);
        if (espelho) System.out.printf(Locale.ROOT, "**Espelho:** o Ryūga contra ele mesmo, os dois com o SHINKAI da coluna (%d lutas por coluna).%n%n", n * 15);
        System.out.printf(Locale.ROOT, "CPU nível %d dos dois lados, 1x1, o Ryūga contra os 15 (%d lutas cada, metade de cada lado),"
            + " as mesmas sementes em todas as colunas. O rival usa o SHINKAI antigo do tipo dele.%s%n%n", nivel, n,
            semClash ? " **Sem Clash**: ninguém liga o SHINKAI com o outro já no dele." : " Jogo normal (com Clash).");
        StringBuilder cab = new StringBuilder("| medida |"), sep = new StringBuilder("|---|");
        for (String x : VARIANTES) { cab.append(' ').append(x).append(" |"); sep.append("---|"); }
        System.out.println(cab);
        System.out.println(sep);
        if (espelho) {
            linha("vitórias do lado medido (os dois iguais: ~50%)", v, e -> pc(e.vitorias, e.decididas));
            linha("duração média da luta (s)", v, e -> String.format(Locale.ROOT, "%.1f", e.quadrosLuta / 60.0 / Math.max(1, e.lutas)));
            linha("quem vence o Clash vence a luta", v, e -> e.clashDecididos == 0 ? "–" : pc(e.clashVenceLuta, e.clashDecididos));
            linha("Rupturas por luta (os dois lados)", v, e -> porLuta(e.rupturas + e.rupturasRival, e));
            linha("leituras por luta (os dois lados)", v, e -> porLuta(e.leituras + e.leiturasRival, e));
        } else {
            linha(elenco ? "lado medido vence (todos contra todos: ~50%)" : "Ryūga vence", v, e -> pc(e.vitorias, e.decididas));
            linha("duração média da luta (s)", v, e -> String.format(Locale.ROOT, "%.1f", e.quadrosLuta / 60.0 / Math.max(1, e.lutas)));
            linha("quem vence o Clash vence a luta", v, e -> e.clashDecididos == 0 ? "–" : pc(e.clashVenceLuta, e.clashDecididos));
            linha(elenco ? "o lado medido vence o Clash" : "o Ryūga vence o Clash", v, e -> e.clashComVencedor == 0 ? "–" : pc(e.clashMedidoVence, e.clashComVencedor));
        }
        linha("Clash: desgaste do Ryūga quando começa (s desde que ele ligou)", v, e -> e.clashesDoRyuga == 0 ? "–"
            : String.format(Locale.ROOT, "%.0f%% (%.1f s)", 100.0 * e.desgasteNoClash / e.clashesDoRyuga / CombatConfig.RYUGA_SOBRECARGA_MAX,
                e.segundosNoClash / 60.0 / e.clashesDoRyuga));
        linha("SHINKAIs ligados por luta", v, e -> porLuta(e.janelas, e));
        linha("duração média do SHINKAI (s)", v, e -> seg(e.quadrosShinkai, e.janelas));
        linha("dano que ele dá por s, no SHINKAI", v, e -> porQuadro(e.danoDado, e.quadrosShinkai));
        linha("dano que ele leva por s, no SHINKAI", v, e -> porQuadro(e.danoLevado, e.quadrosShinkai));
        linha("saldo por SHINKAI (dá − leva)", v, BateriaShinkaiRyuga::saldo);
        linha("dano que ele dá / leva por s, fora", v, e -> porQuadro(e.danoDadoFora, e.quadrosFora) + " / " + porQuadro(e.danoLevadoFora, e.quadrosFora));
        linha("dano que ele leva por s, no SHINKAI do rival", v, e -> porQuadro(e.danoLevadoRival, e.quadrosRival));
        linha("SHINKAI Clash por luta", v, e -> porLuta(e.clashes, e));
        linha("no SHINKAI dele: golpes do rival / defendidos / perfeitas (por luta)", v,
            e -> porLuta(e.golposRival, e) + " / " + porLuta(e.defesas, e) + " / " + porLuta(e.perfeitas, e));
        linha("leituras / esquivas / precisos por luta", v, e -> porLuta(e.leituras, e) + " / " + porLuta(e.esquivas, e) + " / " + porLuta(e.precisos, e));
        linha("dano médio do golpe preciso", v, e -> String.valueOf(e.danoPreciso / Math.max(1, e.precisos)));
        linha("fim: desligou / no limite / Ruptura (total)", v, e -> e.voluntarios + " / " + e.limites + " / " + e.rupturas);
        linha("s exausto por luta (dano levado exausto por s)", v, e -> String.format(Locale.ROOT, "%.1f (%s)",
            e.quadrosExausto / 60.0 / Math.max(1, e.lutas), porQuadro(e.danoLevadoExausto, e.quadrosExausto)));
        linha("dor da Ruptura por luta", v, e -> porLuta(e.dor, e));
        System.out.println();
        System.out.println("## Por rival (Ryūga vence)");
        System.out.println();
        System.out.println(cab.toString().replace("| medida |", "| rival | tipo |"));
        System.out.println(sep.append("---|"));
        for (String rival : v[0].porRival.keySet()) {
            StringBuilder b = new StringBuilder("| " + rival + " | " + Shinkai.typeOfAntigo(rival).label + " |");
            for (Est e : v) { int[] x = e.porRival.get(rival); b.append(' ').append(pc(x[0], x[1])).append(" |"); }
            System.out.println(b);
        }
        if (narra) {
            System.out.println();
            System.out.println("## Log de 4 lutas (protótipo)");
            String[] rivais = espelho ? new String[] {RYUGA, RYUGA, RYUGA, RYUGA} : new String[] {"p03", "p09", "p07", "p11"};
            for (int i = 0; i < rivais.length; i++) {
                StringBuilder log = new StringBuilder();
                luta(rivais[i], i & 1, nivel, 91_000 + i, new Est(), log);
                System.out.println();
                System.out.println("### Ryūga (lado " + (i & 1) + ") × " + (espelho ? "Ryūga (PERCEPÇÃO)" : rivais[i] + " (" + Shinkai.typeOfAntigo(rivais[i]).label + ")"));
                System.out.println();
                System.out.println("```");
                System.out.print(log);
                System.out.println("```");
            }
        }
        System.out.printf(Locale.ROOT, "%n(%.0f s)%n", (System.nanoTime() - t0) / 1e9);
    }

    static void linha(String nome, Est[] v, java.util.function.Function<Est, String> f) {
        StringBuilder b = new StringBuilder("| " + nome + " |");
        for (Est e : v) b.append(' ').append(f.apply(e)).append(" |");
        System.out.println(b);
    }

    static String seg(long quadros, long janelas) { return String.format(Locale.ROOT, "%.1f", quadros / 60.0 / Math.max(1, janelas)); }

    static String saldo(Est e) { return String.valueOf((e.danoDado - e.danoLevado) / Math.max(1, e.janelas)); }

}
