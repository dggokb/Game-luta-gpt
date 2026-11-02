package com.gamelutagpt;

import java.util.*;

/**
 * Ryūga contra Ryūga com o SHINKAI e sem o SHINKAI Clash (pedido de 02/11, antes de mexer no
 * SHINKAI de novo): o retrato de como a luta corre hoje. CPU contra CPU, o mesmo nível dos dois
 * lados. Coluna "com SHINKAI": os dois podem ligar, mas ninguém liga com o outro já no dele (o
 * Clash fica de fora). Coluna "sem SHINKAI": ninguém liga (a referência).
 *
 * Mede a luta (tempo, KO ou tempo, vida do vencedor, viradas), o SHINKAI (quando liga, com quanta
 * vida, quanto dura, como acaba, o que faz dentro, o dano por segundo dentro e fora, quem liga e
 * vence), os combos (tamanho, dano, dentro e fora) e os golpes e poderes (usos por minuto,
 * acertos, defendidos, dano), dentro e fora do SHINKAI.
 *
 * <pre>args: [lutas por nível] [níveis...] (padrão 200, níveis 5 e 8)</pre>
 */
public final class BateriaEspelhoShinkai {
    private BateriaEspelhoShinkai() {}

    static final String RYUGA = "player_base";

    /** Os golpes contados um a um (o resto vai em "outros"). */
    static final String[] GOLPES = {"L", "M", "H", "2L", "2M", "2H", "jL", "jM", "jH", "j2H", "S", "S2", "S3", "S4", "SUPER", "ULTRA"};

    static final class Uso {
        long usos, acertos, defendidos, dano;
    }

    static final class Est {
        int lutas, ko, tempo, empates, vitoriasLado0, viradas;
        long quadros, vidaVencedor, vidaMax;
        final List<Integer> duracoes = new ArrayList<>();
        // SHINKAI
        int ativacoes, lutasNenhum, lutasUm, lutasDois, lutasBarraSemUso;
        long quadrosDentro, quadrosFora, quadrosExausto;
        long segundoAtiv, vidaPropriaAtiv, vidaRivalAtiv, superAtiv, atrasNaVida;
        long duracaoShinkai, danoDadoDentro, danoLevadoDentro, danoDadoFora, danoLevadoFora, danoLevadoExausto;
        long quadrosAlguemDentro;
        long koDentro, morreuDentro, leituras, esquivas, precisos, rupturas, limites, dor;
        long barraCheia, segundoBarraCheia;
        int primeiroLigouVence, primeiroLigouDecididas, soUmLigouVence, soUmLigou, atrasLigouVence, atrasLigou;
        final Map<String, Integer> fim = new TreeMap<>();
        final List<Integer> segundos = new ArrayList<>(), duracoesShinkai = new ArrayList<>();
        // Combos (os dois lados juntos)
        long combos, golpesCombo, danoCombo, maiorCombo, combosComSuper, combosComUltra, breakers;
        long combosDentro, golpesDentro, danoComboDentro;
        final long[] faixa = new long[6], faixaDentro = new long[6];
        // Golpes
        final Map<String, Uso> fora = new LinkedHashMap<>(), dentro = new LinkedHashMap<>();
        long tirosShinkai;
        long agarroes, agarroesCertos, techs, perfeitas, guardBreaks, choquesBola, superGratis, barrasGastas;

        Est() {
            for (String g : GOLPES) { fora.put(g, new Uso()); dentro.put(g, new Uso()); }
            fora.put("outros", new Uso());
            dentro.put("outros", new Uso());
        }

        Uso uso(boolean noShinkai, String id) {
            Map<String, Uso> m = noShinkai ? dentro : fora;
            Uso u = m.get(id);
            return u != null ? u : m.get("outros");
        }
    }

    static boolean preso(CombatFighter f) {
        return f.inHitstun() || f.status == CombatFighter.Status.KNOCKDOWN && f.knockdownFrame < 4 ||
            f.status == CombatFighter.Status.THROWN;
    }

    static int faixa(long n) {
        return n <= 1 ? 0 : n == 2 ? 1 : n == 3 ? 2 : n <= 5 ? 3 : n <= 8 ? 4 : 5;
    }

    static void luta(int nivel, long semente, boolean comShinkai, Est e) {
        BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {RYUGA}, new String[] {RYUGA}, nivel, semente, new CombatConfig());
        BateriaBalanco.Mao m = (lu, ld, in) -> {
            // Sem Clash: ninguém liga com o outro já no SHINKAI; sem SHINKAI: ninguém liga.
            if (!comShinkai || lu.f(1 - ld).state.inShinkai() && !lu.f(ld).state.inShinkai()) in.shinkai = false;
        };
        l.mao[0] = m;
        l.mao[1] = m;
        ultimoAtras[0] = ultimoAtras[1] = false;
        int max = l.f(0).state.profile.maxLife;
        boolean[] dentro = new boolean[2];
        int[] ligouEm = {-1, -1}, ativacoes = new int[2], barraEm = {-1, -1};
        int[] episodioInicio = new int[2];
        int[] antesLeit = new int[2], antesEsq = new int[2], antesPrec = new int[2], antesRup = new int[2];
        // Combos: golpes, dano, Super, Ultra, começou no SHINKAI
        long[] cn = new long[2], cd = new long[2];
        boolean[] cs = new boolean[2], cu = new boolean[2], cdentro = new boolean[2];
        // Golpe em curso (para o uso contar dentro ou fora de onde começou)
        boolean[] golpeDentro = new boolean[2];
        CombatFighter.Status[] antes = {CombatFighter.Status.NEUTRAL, CombatFighter.Status.NEUTRAL};
        int[] maiorAtras = new int[2];   // a maior desvantagem de vida de cada lado na luta
        int primeiro = -1;
        boolean houveKo = false, alguemDentroNoKo = false;
        while (!l.acabou() && l.quadro < 60 * 60 * 4) {
            boolean[] presoAntes = {preso(l.f(0)), preso(l.f(1))};
            int[] vidaAntes = {l.f(0).state.life, l.f(1).state.life};
            l.passo();
            List<String> cues = l.e.cues();
            for (int s = 0; s < 2; s++) {
                CombatFighter f = l.f(s), o = l.f(1 - s);
                FighterState st = f.state;
                boolean agora = st.inShinkai();
                if (barraEm[s] < 0 && st.shinkai >= CombatConfig.SHINKAI_MAX) {
                    barraEm[s] = l.quadro;
                    e.barraCheia++;
                    e.segundoBarraCheia += l.quadro;
                }
                if (agora && !dentro[s]) {
                    e.ativacoes++;
                    ativacoes[s]++;
                    if (ligouEm[s] < 0) { ligouEm[s] = l.quadro; ultimoAtras[s] = st.life < o.state.life; }
                    if (primeiro < 0) primeiro = s;
                    episodioInicio[s] = l.quadro;
                    e.segundoAtiv += l.quadro;
                    e.segundos.add(l.quadro / 60);
                    e.vidaPropriaAtiv += st.life * 1000L / max;
                    e.vidaRivalAtiv += o.state.life * 1000L / max;
                    e.superAtiv += st.superMeter;
                    if (st.life < o.state.life) e.atrasNaVida++;
                    antesLeit[s] = st.leituras; antesEsq[s] = st.esquivas; antesPrec[s] = st.precisos; antesRup[s] = st.rupturas;
                }
                if (!agora && dentro[s]) fecharEpisodio(e, l, s, cues, episodioInicio[s], antesLeit, antesEsq, antesPrec, antesRup);
                if (agora) e.quadrosDentro++; else e.quadrosFora++;
                if (st.exaustaoQuadros > 0) e.quadrosExausto++;
                if (st.emRuptura()) {
                    int caiu = vidaAntes[s] - st.life;
                    for (CombatEngine.HitEvent h : l.e.events()) if (h.defender == s) caiu -= h.damage;
                    if (caiu > 0) e.dor += caiu;
                }
                // Golpes e poderes: o uso conta onde o golpe começou.
                if (f.attacking() && f.attackFrame == 1 && f.attack != null) {
                    golpeDentro[s] = agora;
                    e.uso(agora, f.attack.id).usos++;
                }
                if (f.status == CombatFighter.Status.ULTRA && antes[s] != CombatFighter.Status.ULTRA) {
                    golpeDentro[s] = agora;
                    e.uso(agora, "ULTRA").usos++;
                }
                if (f.status == CombatFighter.Status.THROW && antes[s] != CombatFighter.Status.THROW) e.agarroes++;
                if (o.status == CombatFighter.Status.THROWN && antes[1 - s] != CombatFighter.Status.THROWN) e.agarroesCertos++;
                maiorAtras[s] = Math.max(maiorAtras[s], o.state.life - st.life);
            }
            antes[0] = l.f(0).status;
            antes[1] = l.f(1).status;
            for (String c : cues) {
                if (c.equals("COMBO_BREAKER")) e.breakers++;
                else if (c.equals("TECH")) e.techs++;
                else if (c.equals("JUST_DEFENSE")) e.perfeitas++;
                else if (c.equals("GUARD_BREAK")) e.guardBreaks++;
                else if (c.equals("PROJECTILE_CLASH")) e.choquesBola++;
                else if (c.equals("SHINKAI_SUPER")) e.superGratis++;
                else if (c.equals("SHINKAI_SHOT")) e.tirosShinkai++;
            }
            for (CombatEngine.HitEvent h : l.e.events()) {
                if (h.attacker == h.defender) continue;
                int s = h.attacker;
                boolean noShinkai = l.f(s).state.inShinkai() || dentro[s];
                Uso u = e.uso(h.projectile ? noShinkai : golpeDentro[s] || noShinkai, h.moveId);
                if (h.blocked) u.defendidos++; else u.acertos++;
                u.dano += h.damage;
                if (h.damage > 0) {
                    if (noShinkai) { e.danoDadoDentro += h.damage; } else e.danoDadoFora += h.damage;
                    boolean defDentro = l.f(h.defender).state.inShinkai() || dentro[h.defender];
                    if (defDentro) e.danoLevadoDentro += h.damage; else e.danoLevadoFora += h.damage;
                    if (l.f(h.defender).state.exaustaoQuadros > 0) e.danoLevadoExausto += h.damage;
                }
                if (h.blocked) continue;
                // Combo: acerto com o rival já preso continua; senão começa outro.
                if (!presoAntes[h.defender] && cn[s] > 0) {
                    fecharCombo(e, cn[s], cd[s], cs[s], cu[s], cdentro[s]);
                    cn[s] = cd[s] = 0;
                }
                if (cn[s] == 0) { cs[s] = cu[s] = false; cdentro[s] = noShinkai; }
                cn[s]++;
                cd[s] += h.damage;
                if ("SUPER".equals(h.moveId)) cs[s] = true;
                if ("ULTRA".equals(h.moveId)) cu[s] = true;
                presoAntes[h.defender] = true;
            }
            for (int s = 0; s < 2; s++) {
                if (cn[s] > 0 && !preso(l.f(1 - s))) {
                    fecharCombo(e, cn[s], cd[s], cs[s], cu[s], cdentro[s]);
                    cn[s] = cd[s] = 0;
                }
            }
            if (!houveKo && (l.f(0).ko() || l.f(1).ko())) {
                houveKo = true;
                for (int s = 0; s < 2; s++) {
                    boolean estava = dentro[s] || l.f(s).state.inShinkai();
                    if (estava && l.f(s).ko()) e.morreuDentro++;
                    if (estava && l.f(1 - s).ko()) e.koDentro++;
                    if (dentro[s] || l.f(s).state.inShinkai()) alguemDentroNoKo = true;
                }
            }
            if (dentro[0] || dentro[1]) e.quadrosAlguemDentro++;
            for (int s = 0; s < 2; s++) dentro[s] = l.f(s).state.inShinkai();
        }
        for (int s = 0; s < 2; s++) {
            if (dentro[s]) fecharEpisodio(e, l, s, Collections.singletonList(l.f(s).ko() ? "KO_PROPRIO" : l.f(1 - s).ko() ? "KO_RIVAL" : "FIM_DA_LUTA"),
                episodioInicio[s], antesLeit, antesEsq, antesPrec, antesRup);
            if (cn[s] > 0) fecharCombo(e, cn[s], cd[s], cs[s], cu[s], cdentro[s]);
            if (barraEm[s] >= 0 && ativacoes[s] == 0) e.lutasBarraSemUso++;
        }
        e.lutas++;
        e.quadros += l.quadro;
        e.duracoes.add(l.quadro);
        int v = l.flow.winner();
        if (l.flow.timeOver()) e.tempo++; else e.ko++;
        int ligaram = (ativacoes[0] > 0 ? 1 : 0) + (ativacoes[1] > 0 ? 1 : 0);
        if (ligaram == 0) e.lutasNenhum++; else if (ligaram == 1) e.lutasUm++; else e.lutasDois++;
        if (v != 0 && v != 1) {
            e.empates++;
            return;
        }
        if (v == 0) e.vitoriasLado0++;
        e.vidaVencedor += Math.max(0, l.f(v).state.life);
        e.vidaMax += max;
        if (maiorAtras[v] >= max * 3 / 10) e.viradas++;
        if (primeiro >= 0) {
            e.primeiroLigouDecididas++;
            if (primeiro == v) e.primeiroLigouVence++;
        }
        if (ligaram == 1) {
            e.soUmLigou++;
            if (ativacoes[v] > 0) e.soUmLigouVence++;
        }
        for (int s = 0; s < 2; s++) {
            // Ligou com menos vida que o rival (no primeiro SHINKAI dele).
            if (ligouEm[s] >= 0 && atrasAoLigar(e, s)) {
                e.atrasLigou++;
                if (v == s) e.atrasLigouVence++;
            }
        }
    }

    /** O lado ligou o primeiro SHINKAI da luta com menos vida que o rival (anotado na ativação). */
    static final boolean[] ultimoAtras = new boolean[2];

    static boolean atrasAoLigar(Est e, int s) {
        return ultimoAtras[s];
    }

    static void fecharEpisodio(Est e, BateriaBalanco.Luta l, int s, List<String> cues, int inicio,
                               int[] antesLeit, int[] antesEsq, int[] antesPrec, int[] antesRup) {
        FighterState st = l.f(s).state;
        int dur = l.quadro - inicio;
        e.duracaoShinkai += dur;
        e.duracoesShinkai.add(dur);
        String como = cues.contains("LIMITE_FIM") ? "no limite do corpo (exausto)"
            : cues.contains("RUPTURA_FIM") ? "depois da Ruptura (exausto)"
            : cues.contains("SHINKAI_FIM") ? "desligou ele mesmo"
            : cues.contains("KO_RIVAL") || l.f(1 - s).ko() ? "derrubou o rival (KO) dentro"
            : cues.contains("KO_PROPRIO") || l.f(s).ko() || st.life <= 0 ? "caiu (KO) dentro"
            : cues.contains("FIM_DA_LUTA") ? "o tempo da luta acabou dentro" : "acabou o tempo do SHINKAI";
        e.fim.merge(como, 1, Integer::sum);
        e.leituras += st.leituras - antesLeit[s];
        e.esquivas += st.esquivas - antesEsq[s];
        e.precisos += st.precisos - antesPrec[s];
        e.rupturas += st.rupturas - antesRup[s];
        if (como.startsWith("no limite")) e.limites++;
    }

    static void fecharCombo(Est e, long n, long dano, boolean sup, boolean ult, boolean dentro) {
        e.combos++;
        e.golpesCombo += n;
        e.danoCombo += dano;
        e.maiorCombo = Math.max(e.maiorCombo, n);
        if (sup) e.combosComSuper++;
        if (ult) e.combosComUltra++;
        e.faixa[faixa(n)]++;
        if (dentro) {
            e.combosDentro++;
            e.golpesDentro += n;
            e.danoComboDentro += dano;
            e.faixaDentro[faixa(n)]++;
        }
    }

    // ------------------------------------------------------------------ relatório

    static String pc(long a, long b) { return BateriaVoadora.pc((int)a, (int)Math.max(1, b)); }
    static String f1(double v) { return String.format(Locale.ROOT, "%.1f", v); }
    static String f2(double v) { return String.format(Locale.ROOT, "%.2f", v); }
    static String f0(double v) { return String.format(Locale.ROOT, "%.0f", v); }

    static double mediana(List<Integer> v) {
        if (v.isEmpty()) return 0;
        List<Integer> c = new ArrayList<>(v);
        Collections.sort(c);
        return c.get(c.size() / 2);
    }

    static void linha(StringBuilder b, String nome, String... cols) {
        b.append("| ").append(nome);
        for (String c : cols) b.append(" | ").append(c);
        b.append(" |\n");
    }

    static String relatorio(int nivel, Est com, Est sem) {
        StringBuilder b = new StringBuilder();
        b.append("\n## Nível ").append(nivel).append(" (").append(com.lutas).append(" lutas em cada coluna, CPU × CPU)\n\n");
        b.append("### A luta\n\n");
        b.append("| medida | com SHINKAI (sem Clash) | sem SHINKAI |\n|---|---|---|\n");
        Est[] ab = {com, sem};
        linha(b, "duração média (s)", f1(com.quadros / 60.0 / com.lutas), f1(sem.quadros / 60.0 / sem.lutas));
        linha(b, "duração mediana (s)", f0(mediana(com.duracoes) / 60.0), f0(mediana(sem.duracoes) / 60.0));
        linha(b, "acaba em KO / no tempo", pc(com.ko, com.lutas) + " / " + pc(com.tempo, com.lutas), pc(sem.ko, sem.lutas) + " / " + pc(sem.tempo, sem.lutas));
        linha(b, "vida que sobra ao vencedor", pc(com.vidaVencedor, com.vidaMax), pc(sem.vidaVencedor, sem.vidaMax));
        linha(b, "viradas (o vencedor esteve 30% atrás)", pc(com.viradas, com.lutas - com.empates), pc(sem.viradas, sem.lutas - sem.empates));
        linha(b, "vence o lado esquerdo (controle: ~50%)", pc(com.vitoriasLado0, com.lutas - com.empates), pc(sem.vitoriasLado0, sem.lutas - sem.empates));
        double minC = com.quadros / 3600.0, minS = sem.quadros / 3600.0;
        linha(b, "dano por minuto (cada lado)", f0((com.danoDadoDentro + com.danoDadoFora) / minC / 2), f0((sem.danoDadoDentro + sem.danoDadoFora) / minS / 2));

        b.append("\n### O SHINKAI (com SHINKAI, sem Clash)\n\n");
        b.append("| medida | valor |\n|---|---|\n");
        int lados = com.lutas * 2;
        linha(b, "barra cheia (lados) / segundo médio em que encheu", pc(com.barraCheia, lados) + " / " + f1(com.segundoBarraCheia / 60.0 / Math.max(1, com.barraCheia)) + " s");
        linha(b, "barra cheia e não usou na luta", pc(com.lutasBarraSemUso, lados));
        linha(b, "SHINKAIs por luta (os dois lados)", f2(com.ativacoes / (double)com.lutas));
        linha(b, "lutas: ninguém / um / os dois ligam", pc(com.lutasNenhum, com.lutas) + " / " + pc(com.lutasUm, com.lutas) + " / " + pc(com.lutasDois, com.lutas));
        long a = Math.max(1, com.ativacoes);
        linha(b, "segundo da luta em que liga (média / mediana)", f1(com.segundoAtiv / 60.0 / a) + " / " + f0(mediana(com.segundos)) + " s");
        linha(b, "vida dele / do rival ao ligar", pc(com.vidaPropriaAtiv, a * 1000L) + " / " + pc(com.vidaRivalAtiv, a * 1000L));
        linha(b, "liga atrás na vida", pc(com.atrasNaVida, a));
        linha(b, "barras de Super ao ligar", f2(com.superAtiv / (double)CombatConfig.METER_PER_BAR / a));
        linha(b, "duração (média / mediana)", f1(com.duracaoShinkai / 60.0 / a) + " / " + f1(mediana(com.duracoesShinkai) / 60.0) + " s");
        StringBuilder fins = new StringBuilder();
        com.fim.forEach((k, v) -> fins.append(k).append(' ').append(pc(v, a)).append("; "));
        linha(b, "como acaba", fins.toString());
        linha(b, "por SHINKAI: leituras / esquivas / golpes precisos / Rupturas", f2(com.leituras / (double)a) + " / " + f2(com.esquivas / (double)a) + " / " +
            f2(com.precisos / (double)a) + " / " + f2(com.rupturas / (double)a));
        double sD = com.quadrosDentro / 60.0, sF = com.quadrosFora / 60.0;
        linha(b, "dano que dá por s: dentro / fora", f0(com.danoDadoDentro / Math.max(1, sD)) + " / " + f0(com.danoDadoFora / Math.max(1, sF)));
        linha(b, "dano que leva por s: dentro / fora", f0(com.danoLevadoDentro / Math.max(1, sD)) + " / " + f0(com.danoLevadoFora / Math.max(1, sF)));
        linha(b, "tempo da luta com alguém no SHINKAI", pc(com.quadrosAlguemDentro, com.quadros));
        linha(b, "dano que dá / leva por SHINKAI", f0(com.danoDadoDentro / (double)a) + " / " + f0(com.danoLevadoDentro / (double)a));
        linha(b, "saldo por SHINKAI (dá − leva)", f0((com.danoDadoDentro - com.danoLevadoDentro) / (double)a));
        linha(b, "a luta acaba com o vencedor no SHINKAI / com o perdedor no SHINKAI", pc(com.koDentro, com.lutas) + " / " + pc(com.morreuDentro, com.lutas));
        linha(b, "s exausto por luta (dano levado exausto por s)", f1(com.quadrosExausto / 60.0 / com.lutas) + " (" + f0(com.danoLevadoExausto / Math.max(1, com.quadrosExausto / 60.0)) + ")");
        linha(b, "dor da Ruptura por luta", f0(com.dor / (double)com.lutas));
        linha(b, "quem liga primeiro vence", pc(com.primeiroLigouVence, com.primeiroLigouDecididas));
        linha(b, "só um ligou: ele vence", pc(com.soUmLigouVence, com.soUmLigou));
        linha(b, "Super grátis do SHINKAI usado (por SHINKAI)", f2(com.superGratis / (double)a));
        linha(b, "tiros de energia do SHINKAI (por SHINKAI)", f2(com.tirosShinkai / (double)a));

        b.append("\n### Combos (os dois lados juntos)\n\n");
        b.append("| medida | com SHINKAI: todos | dentro do SHINKAI | sem SHINKAI |\n|---|---|---|---|\n");
        linha(b, "combos por minuto (cada lado)", f1(com.combos / minC / 2), f1(com.combosDentro / Math.max(0.01, sD / 60.0)), f1(sem.combos / minS / 2));
        linha(b, "golpes por combo", f2(com.golpesCombo / (double)Math.max(1, com.combos)), f2(com.golpesDentro / (double)Math.max(1, com.combosDentro)),
            f2(sem.golpesCombo / (double)Math.max(1, sem.combos)));
        linha(b, "dano por combo", f0(com.danoCombo / (double)Math.max(1, com.combos)), f0(com.danoComboDentro / (double)Math.max(1, com.combosDentro)),
            f0(sem.danoCombo / (double)Math.max(1, sem.combos)));
        String[] nomes = {"1 golpe", "2", "3", "4–5", "6–8", "9 ou mais"};
        for (int i = 0; i < 6; i++) {
            linha(b, nomes[i], pc(com.faixa[i], com.combos), pc(com.faixaDentro[i], com.combosDentro), pc(sem.faixa[i], sem.combos));
        }
        linha(b, "maior combo", String.valueOf(com.maiorCombo), "", String.valueOf(sem.maiorCombo));
        linha(b, "combos com Super / com Ultra", pc(com.combosComSuper, com.combos) + " / " + pc(com.combosComUltra, com.combos), "",
            pc(sem.combosComSuper, sem.combos) + " / " + pc(sem.combosComUltra, sem.combos));
        linha(b, "combo breakers por luta", f2(com.breakers / (double)com.lutas), "", f2(sem.breakers / (double)sem.lutas));

        b.append("\n### Golpes e poderes (cada lado, por minuto; acerto e defesa contam cada batida)\n\n");
        b.append("| golpe | fora do SHINKAI: usos/min | acertos / defendidos | dano por uso | dentro do SHINKAI: usos/min | acertos / defendidos | dano por uso | sem SHINKAI: usos/min | dano por uso |\n");
        b.append("|---|---|---|---|---|---|---|---|---|\n");
        double minFora = sF / 60.0, minDentro = Math.max(0.01, sD / 60.0);
        for (String g : com.fora.keySet()) {
            Uso uf = com.fora.get(g), ud = com.dentro.get(g), us = sem.fora.get(g);
            if (uf.usos + ud.usos + us.usos == 0 && uf.acertos + ud.acertos == 0) continue;
            linha(b, g, f1(uf.usos / minFora), uf.acertos + " / " + uf.defendidos, f0(uf.dano / (double)Math.max(1, uf.usos)),
                f1(ud.usos / minDentro), ud.acertos + " / " + ud.defendidos, f0(ud.dano / (double)Math.max(1, ud.usos)),
                f1(us.usos / (sem.quadrosFora / 3600.0)), f0(us.dano / (double)Math.max(1, us.usos)));
        }
        b.append("\n| outros | com SHINKAI | sem SHINKAI |\n|---|---|---|\n");
        linha(b, "agarrões por luta (pegaram)", f2(com.agarroes / (double)com.lutas) + " (" + f2(com.agarroesCertos / (double)com.lutas) + ")",
            f2(sem.agarroes / (double)sem.lutas) + " (" + f2(sem.agarroesCertos / (double)sem.lutas) + ")");
        linha(b, "techs de agarrão por luta", f2(com.techs / (double)com.lutas), f2(sem.techs / (double)sem.lutas));
        linha(b, "defesas perfeitas por luta", f2(com.perfeitas / (double)com.lutas), f2(sem.perfeitas / (double)sem.lutas));
        linha(b, "guard breaks por luta", f2(com.guardBreaks / (double)com.lutas), f2(sem.guardBreaks / (double)sem.lutas));
        linha(b, "ondas que se anulam por luta", f2(com.choquesBola / (double)com.lutas), f2(sem.choquesBola / (double)sem.lutas));
        return b.toString();
    }

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 200;
        // "antigo": o Ryūga com o SHINKAI do sistema antigo (ENERGY) no lugar da PERCEPÇÃO.
        int primeiroNivel = 1;
        if (args.length > 1 && args[1].equals("antigo")) {
            Shinkai.prototipoDesligado = true;
            primeiroNivel = 2;
        }
        int[] niveis = args.length > primeiroNivel ? Arrays.stream(args, primeiroNivel, args.length).mapToInt(Integer::parseInt).toArray() : new int[] {5, 8};
        StringBuilder out = new StringBuilder();
        out.append("# Ryūga × Ryūga: SHINKAI sem Clash (retrato antes da mudança, 02/11)\n\n");
        out.append("CPU contra CPU, os dois o Ryūga (player_base, SHINKAI ").append(Shinkai.typeOf(BateriaBalanco.ch(RYUGA)).label)
            .append("), o mesmo nível. Com SHINKAI: os dois podem ligar, ninguém liga com o outro já no dele. ")
            .append("Sem SHINKAI: ninguém liga. Mesmas sementes nas duas colunas.\n");
        for (int nivel : niveis) {
            Est com = new Est(), sem = new Est();
            for (int i = 0; i < n; i++) {
                long semente = 1_200_000L + nivel * 10_000L + i;
                luta(nivel, semente, true, com);
                luta(nivel, semente, false, sem);
            }
            out.append(relatorio(nivel, com, sem));
        }
        System.out.print(out);
    }
}
