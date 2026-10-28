package com.gamelutagpt;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bateria de balanço (docs/balanco/bateria.md): lutas CPU × CPU no motor de verdade, com o kit
 * inteiro e sementes fixas, para responder perguntas de balanço com números. Não é teste da
 * suíte (demora minutos): roda à mão.
 *
 * <pre>java -cp ... com.gamelutagpt.BateriaBalanco [breaker|shinkai|p09|pressao|rotas|busca|tag|tudo]</pre>
 *
 * Ferramentas soltas: {@code rota <lutador> L,M,H,...} executa uma rota passo a passo,
 * {@code buscaum <lutador> <barras> [alvo]} busca a maior rota (com "alvo": assist + dois quiques),
 * {@code meaty} confere o golpe no quadro em que o caído fica de pé e {@code clashdebug} mostra um
 * SHINKAI Clash da CPU por par de tipos.
 *
 * Tudo é determinístico: a mesma semente dá a mesma luta. O Ultra (a página de mangá fica no
 * GameView) entra com o dano cheio do ultra.json, sem a nota do toque.
 */
public final class BateriaBalanco {
    private BateriaBalanco() {}

    static final int NIVEL = 8;
    static final String[] ELENCO = {"player_base", "p02", "p03", "p04", "p05", "p06", "p07", "p08",
        "p09", "p10", "p11", "p12", "p13", "p14", "p15", "p16"};

    static CharacterDefinition ch(String id) { return GeneratedCharacters.get(id); }

    static String nome(String id) {
        switch (id) {
            case "player_base": return "Ryūga (p01)";
            case "p02": return "Yin-lu (p02)";
            case "p03": return "Kōga (p03)";
            default: return id;
        }
    }

    // ------------------------------------------------------------------ a luta

    /** O que acontece no lado da CPU num quadro (o "sempre quebra" e o "nunca quebra"). */
    interface Mao {
        void ajustar(Luta luta, int lado, FighterInput in);
    }

    /** Uma batalha de verdade: dois times, a CPU dos dois lados, o relógio e o K.O. do BattleFlow. */
    static final class Luta {
        final CombatEngine e;
        final AiController[] ai = new AiController[2];
        final FighterInput[] in = {new FighterInput(), new FighterInput()};
        final BattleFlow flow = new BattleFlow();
        final Mao[] mao = new Mao[2];
        final Estat[] est = {new Estat(), new Estat()};
        int quadro;

        Luta(String[] time0, String[] time1, int nivel, long semente, CombatConfig cfg) {
            FighterState[] a = membros(time0, "P1"), b = membros(time1, "P2");
            e = new CombatEngine(a[0], b[0], 420f, 980f, cfg);
            e.setTeam(0, a);
            e.setTeam(1, b);
            for (int lado = 0; lado < 2; lado++) {
                long s = semente * 1_000_003L + lado * 7919L;
                ai[lado] = new AiController(new OpponentAi(new Random(s)), new Random(s * 31 + 7));
                ai[lado].setEnabled(true);
                ai[lado].setFullKit(true);
                ai[lado].setLevel(nivel);
            }
            flow.setBattle(true);
            flow.reset();
            flow.startCountdown();
        }

        static FighterState[] membros(String[] ids, String tag) {
            FighterState[] m = new FighterState[ids.length];
            for (int i = 0; i < ids.length; i++) m[i] = new FighterState(ch(ids[i]), tag);
            return m;
        }

        CombatFighter f(int lado) { return e.fighter(lado); }

        boolean acabou() { return flow.over(); }

        void passo() {
            boolean segura = flow.holdsFighters();
            for (int lado = 0; lado < 2; lado++) {
                in[lado].clearPresses();
                if (segura) {
                    in[lado].clear();
                    continue;
                }
                ai[lado].setSecondsLeft(flow.clockSeconds());
                ai[lado].fill(e, lado, in[lado]);
                if (mao[lado] != null) mao[lado].ajustar(this, lado, in[lado]);
            }
            if (f(0).ko() || f(1).ko() || flow.over()) {
                in[0].clear();
                in[1].clear();
            }
            e.step(in[0], in[1]);
            quadro++;
            if (e.ultraConnected() >= 0) ultraSemPagina(e, e.ultraConnected());
            for (CombatEngine.HitEvent h : e.events()) est[h.attacker].acerto(h);
            for (String c : e.cues()) {
                if ("COMBO_BREAKER".equals(c)) {
                    // Quem quebrou é quem estava apanhando.
                    for (int lado = 0; lado < 2; lado++) if (f(lado).comboBroke) est[lado].breakers++;
                }
            }
            boolean livre = !e.ultraSequenceRunning();
            flow.update(e, livre, livre && !e.superFreezeActive());
        }

        /** Até acabar (K.O. ou tempo); devolve o lado que venceu, 2 no empate. */
        int rodar() {
            while (!acabou() && quadro < 60 * 60 * 4) passo();
            return flow.winner();
        }
    }

    /** A página do Ultra sem tela: os três painéis (50%) e o raio (50%), sem a nota do toque. */
    static void ultraSemPagina(CombatEngine e, int atacante) {
        String id = e.fighter(atacante).state.character.id;
        int dano = danoUltra(id);
        e.applyUltraHit(atacante, Math.round(dano * 0.5f));
        if (e.ultraWaitingPage(atacante)) e.resumeUltraSequence(atacante, 0f);
        else e.startUltraBeam(atacante, Math.round(dano * 0.5f), 5);
    }

    private static final Map<String, Integer> ULTRA = new HashMap<>();

    static int danoUltra(String id) {
        return ULTRA.computeIfAbsent(id, k -> {
            Path p = Paths.get("src/main/assets/ultras", k, "ultra.json");
            try {
                if (Files.exists(p)) {
                    Matcher m = Pattern.compile("\"dano\"\\s*:\\s*(\\d+)").matcher(new String(Files.readAllBytes(p), "UTF-8"));
                    if (m.find()) return Integer.parseInt(m.group(1));
                }
            } catch (IOException ignored) {
            }
            return com.gamelutagpt.ultra.UltraDefinition.DEFAULT_DAMAGE;
        });
    }

    /** O que cada lado fez na luta. */
    static final class Estat {
        int dano, acertos, defendidos, breakers, danoChip;
        final Map<String, Integer> porGolpe = new HashMap<>();

        void acerto(CombatEngine.HitEvent h) {
            if (h.blocked) {
                defendidos++;
                danoChip += h.damage;
                return;
            }
            acertos++;
            dano += h.damage;
            porGolpe.merge(h.moveId, h.damage, Integer::sum);
        }
    }

    /** Muda o tipo de SHINKAI de um lutador (o tipo vem do personagem; aqui o tipo é a variável). */
    static void forcarTipo(FighterState s, Shinkai.Type tipo) {
        try {
            Field f = FighterState.class.getDeclaredField("shinkaiType");
            f.setAccessible(true);
            f.set(s, tipo);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    // ------------------------------------------------------------------ util

    static String pct(double v) { return String.format(Locale.ROOT, "%.0f%%", 100 * v); }

    static String f1(double v) { return String.format(Locale.ROOT, "%.1f", v); }

    static void titulo(String s) {
        System.out.println();
        System.out.println("## " + s);
        System.out.println();
    }

    // ------------------------------------------------------------------ mãos (o que o lado faz)

    /** Nunca quebra o combo (o resto do kit fica igual). */
    static final Mao NUNCA_QUEBRA = (l, lado, in) -> {
        if (l.e.canBreakCombo(l.f(lado))) in.light = in.medium = in.heavy = false;
    };

    /** Quebra na primeira chance (o 2º acerto do combo). */
    static final Mao SEMPRE_QUEBRA = (l, lado, in) -> {
        if (l.e.canBreakCombo(l.f(lado))) in.light = true;
    };

    /** Boneco parado: não aperta nada. */
    static final Mao PARADO = (l, lado, in) -> in.clear();

    /** Defesa perfeita: sempre segura trás na altura certa (sem reagir, sem errar). */
    static void defender(Luta l, int lado, FighterInput in) {
        CombatFighter me = l.f(lado), o = l.f(1 - lado);
        boolean rivalADireita = o.x >= me.x;
        boolean alto = !o.grounded || o.attacking() && o.attack != null && o.attack.overhead;
        in.direction = alto ? (rivalADireita ? 5 : 1) : (rivalADireita ? 4 : 2);
    }

    // ------------------------------------------------------------------ 1. combo breaker

    static String[] par(Random r) {
        String a = ELENCO[r.nextInt(ELENCO.length)], b;
        do b = ELENCO[r.nextInt(ELENCO.length)]; while (b.equals(a));
        return new String[] {a, b};
    }

    /** Vitórias do lado 0 em n lutas espelhadas (a mesma dupla dos dois lados), trocando de lado. */
    static double[] confronto(Mao a, Mao b, int n, long base) {
        int vence = 0, empates = 0;
        long breakersA = 0, breakersB = 0;
        for (int i = 0; i < n; i++) {
            Random r = new Random(base + i);
            String[] dupla = par(r);
            boolean troca = (i & 1) == 1;
            Luta l = new Luta(dupla, dupla, NIVEL, base + i, new CombatConfig());
            l.mao[troca ? 1 : 0] = a;
            l.mao[troca ? 0 : 1] = b;
            int v = l.rodar();
            int ladoA = troca ? 1 : 0;
            if (v == ladoA) vence++;
            else if (v == BattleFlow.DRAW || v == BattleFlow.NONE) empates++;
            breakersA += l.est[ladoA].breakers;
            breakersB += l.est[1 - ladoA].breakers;
        }
        return new double[] {vence / (double)n, empates / (double)n, breakersA / (double)n, breakersB / (double)n};
    }

    /** Um combo do atacante (CPU nível 8) num boneco que não defende; o boneco tem 1 barra. */
    static final class ComboUnico {
        int danoAntes, danoTotal, troco, acertosAntes, acertosTotal;
        boolean quebrou, superUsado;
    }

    static ComboUnico comboNoBoneco(String atacante, long semente, boolean quebra, int barraAtacante) {
        Luta l = new Luta(new String[] {atacante}, new String[] {"player_base"}, NIVEL, semente, new CombatConfig());
        l.f(1).state.superMeter = CombatConfig.METER_PER_BAR;
        l.f(0).state.superMeter = barraAtacante;
        ComboUnico c = new ComboUnico();
        final boolean[] solto = {false};
        l.mao[1] = (lu, lado, in) -> {
            if (solto[0]) return;
            in.clear();
            if (quebra && lu.e.canBreakCombo(lu.f(1))) {
                in.light = true;
                c.quebrou = true;
            }
        };
        ComboSession contra = null;
        int depois = -1;
        for (int fr = 0; fr < 60 * 40; fr++) {
            l.passo();
            ComboSession s = l.e.session(1);
            if (contra == null && s != null && s.active) contra = s;
            if (contra != null && l.e.cues().contains("COMBO_BREAKER")) {
                c.danoAntes = contra.comboDamage;
                c.acertosAntes = contra.hitCount;
                solto[0] = true;
                depois = fr;
            }
            if (contra != null && !contra.active && depois < 0) {
                if (contra.hitCount >= 2) {
                    c.superUsado = contra.usesOf("SUPER") > 0;
                    c.danoTotal = contra.comboDamage;
                    c.acertosTotal = contra.hitCount;
                    return c;
                }
                contra = null;
            }
            if (depois >= 0) {
                ComboSession t = l.e.session(0);
                if (t != null && !t.active) t = null;
                ComboSession fim = l.e.lastSession(0);
                if (fim != null && fim.attacker == 1 && !(t != null && t.active) && fr > depois + 5 &&
                    l.e.framesSinceSessionEnd(0) == 1) {
                    c.troco = fim.comboDamage;
                    return c;
                }
                if (fr > depois + 240) return c;
            }
        }
        return c;
    }

    static void breaker() {
        titulo("1. Combo Breaker com 1 barra");
        System.out.println("### O valor de uma quebra (CPU nível 8 batendo num boneco que não defende)");
        System.out.println();
        System.out.println("| atacante | combo inteiro | levou até quebrar | dano evitado | troco de quem quebrou | combo com Super (o que a barra rende a mais) |");
        System.out.println("|---|---|---|---|---|---|");
        double somaEvitado = 0, somaTroco = 0, somaSuper = 0;
        int casos = 0, casosSuper = 0;
        for (String atk : ELENCO) {
            double tot = 0, antes = 0, troco = 0, comSuper = 0;
            int n = 0, nSuper = 0;
            for (long s = 1; s <= 16; s++) {
                ComboUnico sem = comboNoBoneco(atk, s, false, 0);
                ComboUnico com = comboNoBoneco(atk, s, true, 0);
                ComboUnico comBarra = comboNoBoneco(atk, s, false, CombatConfig.METER_PER_BAR);
                if (comBarra.superUsado) { comSuper += comBarra.danoTotal; nSuper++; }
                if (sem.acertosTotal < 2 || !com.quebrou) continue;
                tot += sem.danoTotal;
                antes += com.danoAntes;
                troco += com.troco;
                n++;
            }
            if (n == 0) continue;
            casos++;
            somaEvitado += (tot - antes) / n;
            somaTroco += troco / n;
            String sup = "–";
            if (nSuper > 0) {
                double extra = comSuper / nSuper - tot / n;
                somaSuper += extra;
                casosSuper++;
                sup = String.format(Locale.ROOT, "%.0f (%+.0f)", comSuper / nSuper, extra);
            }
            System.out.printf(Locale.ROOT, "| %s | %.0f | %.0f | %.0f | %.0f | %s |%n", nome(atk), tot / n, antes / n,
                (tot - antes) / n, troco / n, sup);
        }
        System.out.printf(Locale.ROOT, "%nMédia: a quebra vale %.0f (evitado) + %.0f (troco) = %.0f de vida; a mesma barra num Super dentro do combo rende +%.0f.%n",
            somaEvitado / casos, somaTroco / casos, (somaEvitado + somaTroco) / casos, somaSuper / Math.max(1, casosSuper));
        System.out.println();
        System.out.println("### Lutas inteiras 2x2 (a mesma dupla dos dois lados, nível 8, 300 lutas cada)");
        System.out.println();
        System.out.println("| lado A | lado B | A vence | empate | quebras de A por luta | quebras de B por luta |");
        System.out.println("|---|---|---|---|---|---|");
        Object[][] casosLuta = {
            {"sempre quebra", SEMPRE_QUEBRA, "nunca quebra", NUNCA_QUEBRA},
            {"sempre quebra", SEMPRE_QUEBRA, "CPU normal (45%)", null},
            {"CPU normal (45%)", null, "nunca quebra", NUNCA_QUEBRA},
            {"nunca quebra", NUNCA_QUEBRA, "nunca quebra", NUNCA_QUEBRA},
        };
        for (Object[] c : casosLuta) {
            double[] r = confronto((Mao)c[1], (Mao)c[3], 300, 10_000);
            System.out.printf(Locale.ROOT, "| %s | %s | %s | %s | %s | %s |%n", c[0], c[2], pct(r[0]), pct(r[1]), f1(r[2]), f1(r[3]));
        }
    }

    // ------------------------------------------------------------------ 2. SHINKAI × SHINKAI

    /** Não responde ao SHINKAI do rival com o próprio (sem Clash). */
    static final Mao SEM_CLASH = (l, lado, in) -> {
        if (l.f(1 - lado).state.inShinkai()) in.shinkai = false;
    };

    static void shinkai() {
        titulo("2. SHINKAI contra SHINKAI");
        Shinkai.Type[] tipos = Shinkai.Type.values();
        System.out.println("### Valor do SHINKAI sozinho (1x1 espelhado, só o lado A liga no GO; 8 s de SHINKAI + 2 s)");
        System.out.println();
        System.out.println("| tipo | dano de A | dano de B | saldo | A vence a luta (sem barra do outro) |");
        System.out.println("|---|---|---|---|---|");
        for (Shinkai.Type t : tipos) {
            double da = 0, db = 0;
            int vit = 0, n = 240;
            for (int i = 0; i < n; i++) {
                String c = ELENCO[i % ELENCO.length];
                Luta l = new Luta(new String[] {c}, new String[] {c}, NIVEL, 20_000 + i, new CombatConfig());
                forcarTipo(l.f(0).state, t);
                l.f(0).state.shinkai = CombatConfig.SHINKAI_MAX;
                final int[] ligou = {-1};
                l.mao[0] = (lu, lado, in) -> {
                    if (ligou[0] < 0 && lu.e.teams.canShinkai(lu.f(0))) {
                        in.shinkai = true;
                        ligou[0] = lu.quadro;
                    } else in.shinkai = false;
                };
                l.mao[1] = (lu, lado, in) -> {
                    in.shinkai = false;
                    lu.f(1).state.shinkai = 0;
                };
                while (!l.acabou() && (ligou[0] < 0 || l.quadro < ligou[0] + 600)) l.passo();
                da += l.est[0].dano + l.est[0].danoChip;
                db += l.est[1].dano + l.est[1].danoChip;
                int v = l.rodar();
                if (v == 0) vit++;
            }
            System.out.printf(Locale.ROOT, "| %s | %.0f | %.0f | %+.0f | %s |%n", t.label, da / n, db / n, (da - db) / n, pct(vit / (double)n));
        }

        System.out.println();
        System.out.println("### Confronto direto (SHINKAI Clash: os dois ligam, 1x1 espelhado, 120 por par)");
        System.out.println();
        StringBuilder cab = new StringBuilder("| linha vence coluna |");
        StringBuilder sep = new StringBuilder("|---|");
        for (Shinkai.Type t : tipos) { cab.append(' ').append(t.label).append(" |"); sep.append("---|"); }
        System.out.println(cab);
        System.out.println(sep);
        for (Shinkai.Type ta : tipos) {
            StringBuilder linha = new StringBuilder("| " + ta.label + " |");
            for (Shinkai.Type tb : tipos) {
                int va = 0, vb = 0, n = 120;
                for (int i = 0; i < n; i++) {
                    String c = ELENCO[i % ELENCO.length];
                    boolean troca = (i & 1) == 1;
                    int la = troca ? 1 : 0;
                    Luta l = new Luta(new String[] {c}, new String[] {c}, NIVEL, 30_000 + i, new CombatConfig());
                    forcarTipo(l.f(la).state, ta);
                    forcarTipo(l.f(1 - la).state, tb);
                    l.f(0).state.shinkai = l.f(1).state.shinkai = CombatConfig.SHINKAI_MAX;
                    final boolean[] foi = {false, false};
                    Mao liga = (lu, lado, in) -> {
                        if (!foi[lado] && lu.e.teams.canShinkai(lu.f(lado)) && (lado == la || lu.f(la).state.inShinkai() || lu.e.shinkaiClash() != null)) {
                            in.shinkai = true;
                            foi[lado] = true;
                        }
                    };
                    l.mao[0] = liga;
                    l.mao[1] = liga;
                    for (int fr = 0; fr < 900 && !l.acabou(); fr++) {
                        l.passo();
                        boolean fim = false;
                        for (CombatEngine.HitEvent h : l.e.events()) {
                            if ("SHINKAI_CLASH".equals(h.moveId)) {
                                if (h.attacker == la) va++; else vb++;
                                fim = true;
                            }
                        }
                        if (fim) break;
                    }
                }
                linha.append(' ').append(ta == tb ? "–" : pct(va / (double)Math.max(1, va + vb)) + " (" + (va + vb) + ")").append(" |");
            }
            System.out.println(linha);
        }
        clashHumano();

        System.out.println();
        for (boolean clash : new boolean[] {true, false}) {
            System.out.println();
            System.out.println("### Lutas inteiras 2x2 " + (clash ? "com" : "sem") + " Clash (a mesma dupla, cada lado com um tipo para os dois, 160 lutas por par)");
            System.out.println();
            System.out.println(cab);
            System.out.println(sep);
            int[] ligacoes = new int[1], clashes = new int[1];
            int lutas = 0;
            for (Shinkai.Type ta : tipos) {
                StringBuilder linha = new StringBuilder("| " + ta.label + " |");
                for (Shinkai.Type tb : tipos) {
                    int va = 0, vb = 0, n = 160;
                    for (int i = 0; i < n; i++) {
                        Random r = new Random(40_000 + i);
                        String[] dupla = par(r);
                        boolean troca = (i & 1) == 1;
                        int la = troca ? 1 : 0;
                        Luta l = new Luta(dupla, dupla, NIVEL, 40_000 + i, new CombatConfig());
                        for (FighterState m : l.e.team(la).members) forcarTipo(m, ta);
                        for (FighterState m : l.e.team(1 - la).members) forcarTipo(m, tb);
                        if (!clash) l.mao[0] = l.mao[1] = SEM_CLASH;
                        int v = 0;
                        while (!l.acabou() && l.quadro < 60 * 60 * 4) {
                            l.passo();
                            if (l.e.cues().contains("SHINKAI")) ligacoes[0]++;
                            if (l.e.cues().contains("SHINKAI_CLASH")) clashes[0]++;
                        }
                        v = l.flow.winner();
                        lutas++;
                        if (v == la) va++; else if (v == 1 - la) vb++;
                    }
                    linha.append(' ').append(ta == tb ? pct(va / (double)Math.max(1, va + vb)) : pct(va / (double)Math.max(1, va + vb))).append(" |");
                }
                System.out.println(linha);
            }
            System.out.printf(Locale.ROOT, "%nSHINKAI ligados por luta (média): %.2f; Clash por luta: %.2f%n", ligacoes[0] / (double)lutas,
                clashes[0] / (double)lutas);
        }
    }

    // ------------------------------------------------------------------ 3. p09 × Yin-lu (e o elenco)

    static void p09() {
        titulo("3. p09 (grappler) contra Yin-lu (p02)");
        String[] ids = ELENCO;
        int n = ids.length, porPar = 40;
        double[][] taxa = new double[n][n];
        double[] geral = new double[n];
        for (int a = 0; a < n; a++) {
            for (int b = a + 1; b < n; b++) {
                int va = 0, jogos = 0;
                for (int i = 0; i < porPar; i++) {
                    boolean troca = (i & 1) == 1;
                    String[] t0 = {troca ? ids[b] : ids[a]}, t1 = {troca ? ids[a] : ids[b]};
                    Luta l = new Luta(t0, t1, NIVEL, 50_000 + a * 1000 + b * 50 + i, new CombatConfig());
                    int v = l.rodar();
                    int ladoA = troca ? 1 : 0;
                    if (v == ladoA) va++;
                    if (v == 0 || v == 1) jogos++;
                }
                taxa[a][b] = va / (double)Math.max(1, jogos);
                taxa[b][a] = 1 - taxa[a][b];
            }
        }
        Integer[] ordem = new Integer[n];
        for (int a = 0; a < n; a++) {
            double s = 0;
            for (int b = 0; b < n; b++) if (b != a) s += taxa[a][b];
            geral[a] = s / (n - 1);
            ordem[a] = a;
        }
        java.util.Arrays.sort(ordem, (x, y) -> Double.compare(geral[y], geral[x]));
        System.out.println("### Todos contra todos, 1x1, nível 8 (40 lutas por par, metade de cada lado)");
        System.out.println();
        System.out.println("| # | lutador | vitórias contra o elenco | contra p09 | contra Yin-lu |");
        System.out.println("|---|---|---|---|---|");
        int i09 = java.util.Arrays.asList(ids).indexOf("p09"), i02 = java.util.Arrays.asList(ids).indexOf("p02");
        for (int k = 0; k < n; k++) {
            int a = ordem[k];
            System.out.printf(Locale.ROOT, "| %d | %s | %s | %s | %s |%n", k + 1, nome(ids[a]), pct(geral[a]),
                a == i09 ? "–" : pct(taxa[a][i09]), a == i02 ? "–" : pct(taxa[a][i02]));
        }

        System.out.println();
        System.out.println("### p09 × Yin-lu em detalhe (1x1, 400 lutas por nível, metade de cada lado)");
        System.out.println();
        System.out.println("| nível | p09 vence | dano por acerto p09 / Yin-lu | acertos por luta p09 / Yin-lu | dano do p09 em agarrões | tempo colados (< 200 px) | tempo longe (> 450 px) | duração |");
        System.out.println("|---|---|---|---|---|---|---|---|");
        Map<String, Integer> golpes09 = new HashMap<>(), golpes02 = new HashMap<>();
        for (int nivel : new int[] {5, 8}) {
            int v09 = 0, jogos = 400;
            double dano09 = 0, dano02 = 0, ac09 = 0, ac02 = 0, agarra = 0, perto = 0, longe = 0, quadros = 0;
            for (int i = 0; i < jogos; i++) {
                boolean troca = (i & 1) == 1;
                int l09 = troca ? 1 : 0;
                Luta l = new Luta(new String[] {troca ? "p02" : "p09"}, new String[] {troca ? "p09" : "p02"}, nivel, 60_000 + i, new CombatConfig());
                while (!l.acabou() && l.quadro < 60 * 60 * 4) {
                    l.passo();
                    if (l.flow.phase() == BattleFlow.Phase.FIGHT) {
                        float d = Math.abs(l.f(0).x - l.f(1).x);
                        if (d < 200) perto++;
                        if (d > 450) longe++;
                        quadros++;
                    }
                }
                if (l.flow.winner() == l09) v09++;
                Estat e9 = l.est[l09], e2 = l.est[1 - l09];
                dano09 += e9.dano; dano02 += e2.dano; ac09 += e9.acertos; ac02 += e2.acertos;
                if (nivel == 8) {
                    e9.porGolpe.forEach((k, v) -> golpes09.merge(k, v, Integer::sum));
                    e2.porGolpe.forEach((k, v) -> golpes02.merge(k, v, Integer::sum));
                }
                for (Map.Entry<String, Integer> g : e9.porGolpe.entrySet()) {
                    AttackDefinition at = ch("p09").attack(g.getKey());
                    if ("THROW".equals(g.getKey()) || at != null && at.mechanics.grab) agarra += g.getValue();
                }
            }
            System.out.printf(Locale.ROOT, "| %d | %s | %.0f / %.0f | %.1f / %.1f | %s | %s | %s | %.0f s |%n", nivel, pct(v09 / (double)jogos),
                dano09 / Math.max(1, ac09), dano02 / Math.max(1, ac02), ac09 / jogos, ac02 / jogos,
                pct(agarra / Math.max(1, dano09)), pct(perto / quadros), pct(longe / quadros), quadros / jogos / 60);
        }
        CharacterDefinition c9 = ch("p09"), c2 = ch("p02");
        CombatConfig cfg = new CombatConfig();
        System.out.printf(Locale.ROOT, "%nVida: p09 %d, Yin-lu %d. Andar: p09 %.0f, Yin-lu %.0f px/s.%n",
            c9.fighter.maxLife, c2.fighter.maxLife, c9.fighter.walkSpeed > 0 ? c9.fighter.walkSpeed : cfg.walkSpeed,
            c2.fighter.walkSpeed > 0 ? c2.fighter.walkSpeed : cfg.walkSpeed);
        for (Object[] q : new Object[][] {{"p09", golpes09}, {"Yin-lu", golpes02}}) {
            @SuppressWarnings("unchecked") Map<String, Integer> m = (Map<String, Integer>)q[1];
            int total = m.values().stream().mapToInt(Integer::intValue).sum();
            StringBuilder sb = new StringBuilder("De onde vem o dano (" + q[0] + ", nível 8): ");
            m.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue()).limit(6)
                .forEach(en -> sb.append(en.getKey()).append(' ').append(pct(en.getValue() / (double)total)).append(", "));
            System.out.println(sb.substring(0, sb.length() - 2) + ".");
        }
    }

    // ------------------------------------------------------------------ 4. pressão (Kōga)

    static final String[] SAIDAS = {"só defende", "L", "2L", "agarrão", "backdash", "pulo para trás", "pushblock"};

    /** Resultado de um atacante contra a defesa perfeita que tenta uma saída depois de cada sequência. */
    static final class Pressao {
        int tentativas, ganhou, neutro, punido;
        int sequencias, semBuraco, buracoJab, quebras, acertosNaDefesa, danoNaDefesa, chip;
        double segundos;
    }

    static Pressao pressao(String atk, int saida, int sementes) {
        Pressao p = new Pressao();
        int jab = ch("player_base").attack("L").startupFrames;
        for (long s = 1; s <= sementes; s++) {
            Luta l = new Luta(new String[] {atk}, new String[] {"player_base"}, NIVEL, 70_000 + s, new CombatConfig());
            l.f(0).state.life = l.f(1).state.life = 1_000_000;
            final int[] st = {0, -1, 0, -1};   // [vinha defendendo, quadro da tentativa, pushblock feito, saiu do bloqueio em]
            l.mao[1] = (lu, lado, in) -> {
                CombatFighter me = lu.f(1), o = lu.f(0);
                in.clear();
                me.state.superMeter = 2 * CombatConfig.METER_PER_BAR;
                defender(lu, 1, in);
                boolean bloqueando = me.status == CombatFighter.Status.BLOCKSTUN;
                if (bloqueando) {
                    if (st[0] == 0) st[2] = 0;
                    st[0] = 1;
                    if (saida == 6 && st[2] == 0 && st[1] < 0 && !me.frozen()) {
                        in.pushblock = true;
                        st[2] = 1;
                        st[1] = lu.quadro;
                    }
                    return;
                }
                if (saida == 0 || saida == 6 || st[1] >= 0 || st[0] == 0 || !me.canAct()) return;
                st[0] = 0;
                if (Math.abs(o.x - me.x) > 300) return;
                st[1] = lu.quadro;
                int tras = o.x >= me.x ? 5 : 1;
                switch (saida) {
                    case 1: in.direction = 0; in.light = true; break;
                    case 2: in.direction = 3; in.light = true; break;
                    case 3: in.direction = 0; in.grab = true; break;
                    case 4: in.direction = 0; in.backdash = true; break;
                    case 5: in.direction = tras == 5 ? 6 : 8; break;
                    default: break;
                }
            };
            int fim = 60 * 20 + 100;
            int ultimoBloqueio = -1;
            boolean emSequencia = false;
            while (l.quadro < fim) {
                l.passo();
                CombatFighter d = l.f(1);
                for (CombatEngine.HitEvent h : l.e.events()) {
                    if (h.attacker == 0 && h.blocked) {
                        p.chip += h.damage;
                        if (!emSequencia) p.sequencias++;
                        else if (st[3] >= 0) {
                            int buraco = l.quadro - st[3];
                            if (buraco <= 1) p.semBuraco++;
                            else if (buraco <= jab) p.buracoJab++;
                        }
                        emSequencia = true;
                        st[3] = -1;
                        ultimoBloqueio = l.quadro;
                    }
                    if (h.attacker == 0 && !h.blocked && st[1] < 0) {
                        p.acertosNaDefesa++;
                        p.danoNaDefesa += h.damage;
                    }
                }
                if (l.e.cues().contains("GUARD_BREAK")) p.quebras++;
                if (d.status != CombatFighter.Status.BLOCKSTUN && st[3] < 0 && emSequencia && d.canAct()) st[3] = l.quadro;
                if (emSequencia && ultimoBloqueio >= 0 && l.quadro - ultimoBloqueio > 40) emSequencia = false;
                if (st[1] >= 0) {
                    int r = 0;
                    for (CombatEngine.HitEvent h : l.e.events()) {
                        if (h.blocked) continue;
                        if (h.attacker == 1) r = 1;
                        else if (r == 0) r = 2;
                    }
                    if (r == 1) p.ganhou++;
                    else if (r == 2) p.punido++;
                    else if (l.quadro - st[1] >= 45) p.neutro++;
                    if (r != 0 || l.quadro - st[1] >= 45) {
                        p.tentativas++;
                        st[1] = -1;
                    }
                }
            }
            p.segundos += 20;
        }
        return p;
    }

    /** Vantagem na defesa: quadros que o atacante fica livre antes de quem defendeu (negativo: quem defendeu pune). */
    static int[] vantagem(String atk, String golpe) {
        CombatConfig cfg = new CombatConfig();
        FighterState a = new FighterState(ch(atk), "P1"), d = new FighterState(ch("player_base"), "P2");
        CombatEngine e = new CombatEngine(a, d, 900f, 900f + 60f, cfg);
        FighterInput in0 = new FighterInput(), in1 = new FighterInput();
        CombatFighter fa = e.fighter(0), fd = e.fighter(1);
        // Encosta (o empurrão separa os corpos).
        for (int i = 0; i < 10; i++) { in0.clear(); in1.clear(); e.step(in0, in1); }
        int defendeu = -1, livreA = -1, livreD = -1;
        for (int fr = 0; fr < 150; fr++) {
            in0.clear();
            in1.clear();
            // Em pé, só agacha contra golpe baixo (golpes altos de pé podem passar por cima de quem agacha).
            boolean baixo = golpe.startsWith("2") || fa.attacking() && fa.attack != null && fa.attack.low;
            boolean rivalADireita = fa.x >= fd.x;
            in1.direction = baixo ? (rivalADireita ? 4 : 2) : (rivalADireita ? 5 : 1);
            if (fr == 0) {
                switch (golpe) {
                    case "L": in0.light = true; break;
                    case "M": in0.medium = true; break;
                    case "H": in0.heavy = true; break;
                    case "2L": in0.direction = 2; in0.light = true; break;
                    case "2M": in0.direction = 2; in0.medium = true; break;
                    case "2H": in0.direction = 2; in0.heavy = true; break;
                    case "S": in0.special = "M"; break;
                    default: break;
                }
            }
            e.step(in0, in1);
            for (CombatEngine.HitEvent h : e.events()) if (h.attacker == 0 && h.blocked && defendeu < 0) defendeu = fr;
            if (defendeu >= 0) {
                if (livreA < 0 && fa.canAct() && !e.energyProjectileAlive(0)) livreA = fr;
                else if (livreA < 0 && fa.canAct()) livreA = fr;
                if (livreD < 0 && fd.canAct()) livreD = fr;
            }
            if (livreA >= 0 && livreD >= 0) break;
        }
        if (defendeu < 0 || livreA < 0 || livreD < 0) return null;
        return new int[] {livreD - livreA, Math.round(Math.abs(fa.x - fd.x))};
    }

    static void pressao() {
        titulo("4. Pressão do Kōga (p03) na defesa");
        System.out.println("A CPU nível 8 ataca; quem defende é o Ryūga com a defesa perfeita (altura sempre certa) e, depois de");
        System.out.println("cada sequência defendida, tenta uma saída. Ganhou = acertou primeiro; punido = levou um golpe antes;");
        System.out.println("neutro = ninguém acertou em 45 quadros. Média do elenco ao lado (20 lutas de 20 s por saída).");
        System.out.println();
        String[] golpes = {"L", "M", "H", "2L", "2M", "2H", "S"};
        System.out.println("Vantagem na defesa (quadros; + o atacante age primeiro, − quem defendeu pune) e distância depois:");
        System.out.println();
        StringBuilder cab = new StringBuilder("| atacante |"), sep = new StringBuilder("|---|");
        for (String g : golpes) { cab.append(' ').append(g).append(" |"); sep.append("---|"); }
        System.out.println(cab);
        System.out.println(sep);
        double[] somaV = new double[golpes.length];
        int[] nV = new int[golpes.length];
        String linhaKoga = null;
        for (String atk : ELENCO) {
            StringBuilder linha = new StringBuilder("| " + nome(atk) + " |");
            for (int g = 0; g < golpes.length; g++) {
                int[] v = vantagem(atk, golpes[g]);
                if (v == null) { linha.append(" – |"); continue; }
                somaV[g] += v[0];
                nV[g]++;
                linha.append(String.format(Locale.ROOT, " %+d (%d px) |", v[0], v[1]));
            }
            if (atk.equals("p03")) linhaKoga = linha.toString();
        }
        System.out.println(linhaKoga);
        StringBuilder media = new StringBuilder("| média do elenco |");
        for (int g = 0; g < golpes.length; g++) media.append(nV[g] == 0 ? " – |" : String.format(Locale.ROOT, " %+.1f |", somaV[g] / nV[g]));
        System.out.println(media);
        System.out.println();
        Map<String, Pressao[]> todos = new java.util.LinkedHashMap<>();
        for (String atk : ELENCO) {
            Pressao[] r = new Pressao[SAIDAS.length];
            for (int k = 0; k < SAIDAS.length; k++) r[k] = pressao(atk, k, 20);
            todos.put(atk, r);
        }
        Pressao[] koga = todos.get("p03");
        System.out.println("| saída | Kōga: ganhou / neutro / punido | elenco: ganhou / neutro / punido | pior atacante para essa saída |");
        System.out.println("|---|---|---|---|");
        for (int k = 1; k < SAIDAS.length; k++) {
            double g = 0, ne = 0, pu = 0;
            String pior = "";
            double piorTaxa = -1;
            for (Map.Entry<String, Pressao[]> e : todos.entrySet()) {
                Pressao q = e.getValue()[k];
                double t = Math.max(1, q.tentativas);
                g += q.ganhou / t; ne += q.neutro / t; pu += q.punido / t;
                if (q.punido / t > piorTaxa) { piorTaxa = q.punido / t; pior = nome(e.getKey()) + " " + pct(piorTaxa); }
            }
            int m = todos.size();
            Pressao q = koga[k];
            double t = Math.max(1, q.tentativas);
            System.out.printf(Locale.ROOT, "| %s | %s / %s / %s (%d) | %s / %s / %s | %s |%n", SAIDAS[k], pct(q.ganhou / t), pct(q.neutro / t),
                pct(q.punido / t), q.tentativas, pct(g / m), pct(ne / m), pct(pu / m), pior);
        }
        System.out.println();
        System.out.println("Só defendendo (defesa perfeita, 20 lutas de 20 s):");
        System.out.println();
        System.out.println("| atacante | sequências / 20 s | emendas sem buraco | buraco menor que o L (jab perde) | quebras de guarda | golpes que passaram a defesa | dano deles | chip |");
        System.out.println("|---|---|---|---|---|---|---|---|");
        for (Map.Entry<String, Pressao[]> e : todos.entrySet()) {
            Pressao q = e.getValue()[0];
            int n = 20;
            System.out.printf(Locale.ROOT, "| %s | %.1f | %.1f | %.1f | %.2f | %.1f | %.0f | %.0f |%n", nome(e.getKey()), q.sequencias / (double)n,
                q.semBuraco / (double)n, q.buracoJab / (double)n, q.quebras / (double)n, q.acertosNaDefesa / (double)n,
                q.danoNaDefesa / (double)n, q.chip / (double)n);
        }
    }

    // ------------------------------------------------------------------ 5. rotas com assist e quiques

    static String parceiro(String id) {
        switch (id) {
            case "player_base": return "p02";
            case "p02": return "player_base";
            case "p13": return "p14";
            case "p14": return "p13";
            case "p15": return "p16";
            case "p16": return "p15";
            default: {
                int n = Integer.parseInt(id.substring(1));
                return String.format(Locale.ROOT, "p%02d", n % 2 == 1 ? n + 1 : n - 1);
            }
        }
    }

    static final class Rota {
        String quem;
        int dano, acertos, duracao, parede, chao;
        boolean assist, superUsado, ultraUsado;
    }

    static final Map<String, Integer> ESTADOS = new java.util.TreeMap<>();

    static void rotas() {
        titulo("5. Combos com assist e dois quiques");
        System.out.println("A CPU nível 8 (com o parceiro de assist) bate num boneco que não defende, 20 lutas de 60 s por lutador.");
        System.out.println("Um quique de parede e um de chão por combo no máximo (config). Dano em % da vida média do elenco.");
        System.out.println();
        double vidaMedia = 0;
        for (String id : ELENCO) vidaMedia += ch(id).fighter.maxLife;
        vidaMedia /= ELENCO.length;
        List<Rota> todas = new ArrayList<>();
        int cadeiaMax = 0;
        String cadeiaQuem = "";
        int semRecuperar = 0;
        for (int barras : new int[] {0, 3}) {
            for (String atk : ELENCO) {
                for (long s = 1; s <= 20; s++) {
                    Luta l = new Luta(new String[] {atk, parceiro(atk)}, new String[] {"player_base"}, NIVEL, 80_000 + s, new CombatConfig());
                    l.f(1).state.life = 10_000_000;
                    l.mao[1] = PARADO;
                    ComboSession viva = null;
                    boolean assist = false;
                    int cadeia = 0;
                    boolean agiu = true;
                    final int b = barras;
                    l.mao[0] = (lu, lado, in) -> {
                        FighterState st = lu.f(0).state;
                        if (b == 0) st.superMeter = 0;
                        else if (lu.e.session(1) == null || !lu.e.session(1).active) st.superMeter = Math.max(st.superMeter, b * CombatConfig.METER_PER_BAR);
                    };
                    String antesStatus = "";
                    while (l.quadro < 60 * 62) {
                        CombatFighter bo = l.f(1);
                        antesStatus = bo.status + (bo.grounded ? "" : " no ar") + (bo.frozen() ? " congelado" : "") + (bo.locked ? " preso" : "");
                        l.passo();
                        ComboSession sess = l.e.session(1);
                        if (sess != null && sess.active) {
                            if (viva != sess) {
                                viva = sess;
                                assist = false;
                                if (agiu) cadeia = 0;
                                else {
                                    semRecuperar++;
                                    String k = antesStatus + " por " + (sess.lastMove() == null ? "?" : sess.lastMove());
                                    ESTADOS.merge(k, 1, Integer::sum);
                                }
                            }
                            assist |= l.e.team(0).assistOut();
                            agiu = false;
                        } else if (viva != null) {
                            Rota r = new Rota();
                            r.quem = atk + (barras > 0 ? " (3 barras)" : "");
                            r.dano = viva.comboDamage; r.acertos = viva.hitCount; r.duracao = viva.comboDuration;
                            r.parede = viva.wallBounces; r.chao = viva.groundBounces; r.assist = assist;
                            r.superUsado = viva.usesOf("SUPER") > 0; r.ultraUsado = viva.usesOf("ULTRA") > 0;
                            if (r.acertos >= 2) todas.add(r);
                            cadeia += r.dano;
                            if (cadeia > cadeiaMax) { cadeiaMax = cadeia; cadeiaQuem = r.quem; }
                            viva = null;
                        }
                        if (viva == null && l.f(1).canAct()) agiu = true;
                    }
                }
            }
        }
        String[] grupos = {"sem assist, sem quique", "com assist", "dois quiques (parede + chão)", "assist + dois quiques"};
        System.out.println("| grupo | combos | dano médio | maior dano | quem | acertos | duração do maior |");
        System.out.println("|---|---|---|---|---|---|---|");
        for (int g = 0; g < 4; g++) {
            int n = 0;
            double soma = 0;
            Rota max = null;
            for (Rota r : todas) {
                if (r.ultraUsado) continue;
                boolean dois = r.parede > 0 && r.chao > 0;
                boolean ok = g == 0 ? !r.assist && r.parede == 0 && r.chao == 0 : g == 1 ? r.assist : g == 2 ? dois : r.assist && dois;
                if (!ok) continue;
                n++;
                soma += r.dano;
                if (max == null || r.dano > max.dano) max = r;
            }
            if (max == null) {
                System.out.printf(Locale.ROOT, "| %s | 0 | – | – | – | – | – |%n", grupos[g]);
                continue;
            }
            System.out.printf(Locale.ROOT, "| %s | %d | %.0f (%s) | %d (%s) | %s%s | %d | %.1f s |%n", grupos[g], n, soma / n, pct(soma / n / vidaMedia),
                max.dano, pct(max.dano / vidaMedia), nome(max.quem.split(" ")[0]), max.quem.contains("barras") ? " com 3 barras" + (max.superUsado ? " (Super)" : "") : "",
                max.acertos, max.duracao / 60.0);
        }
        Rota longa = null;
        for (Rota r : todas) if (longa == null || r.duracao > longa.duracao) longa = r;
        System.out.printf(Locale.ROOT, "%nCombo mais longo: %.1f s (%d acertos, %s). Vida média do elenco: %.0f.%n", longa.duracao / 60.0, longa.acertos,
            nome(longa.quem.split(" ")[0]), vidaMedia);
        System.out.printf(Locale.ROOT, "Combos que começaram sem o boneco poder agir desde o anterior: %d; maior dano somado assim: %d (%s, %s).%n",
            semRecuperar, cadeiaMax, nome(cadeiaQuem.split(" ")[0]), pct(cadeiaMax / vidaMedia));
        System.out.println("Estado do boneco quando o combo seguinte começou: " + ESTADOS);
    }

    // ------------------------------------------------------------------ 6. TAG depois da assistência

    /** Chama o assist e aperta TAG de novo {@code k} quadros depois; quadros até o novo titular agir (-1: não trocou). */
    static int[] trocaDepoisDoAssist(int k, boolean rivalBate, long semente) {
        Luta l = new Luta(new String[] {"player_base", "p02"}, new String[] {"player_base"}, NIVEL, 90_000 + semente, new CombatConfig());
        if (!rivalBate) l.mao[1] = PARADO;
        final int[] f = {-1, -1, 0};   // [quadro do assist, quadro do TAG, pedido aceito]
        l.mao[0] = (lu, lado, in) -> {
            in.clear();
            if (f[0] < 0 && lu.f(0).canAct() && lu.flow.phase() == BattleFlow.Phase.FIGHT) {
                in.assist = true;
                f[0] = lu.quadro;
            } else if (f[0] >= 0 && f[1] < 0 && lu.quadro == f[0] + k) {
                in.assist = true;
                f[1] = lu.quadro;
            }
        };
        FighterState antes = l.e.team(0).pointState();
        while (l.quadro < 60 * 8) {
            l.passo();
            if (f[1] >= 0 && l.e.team(0).convert) f[2] = 1;
            if (f[1] >= 0 && l.e.team(0).pointState() != antes && l.f(0).canAct()) return new int[] {l.quadro - f[1], f[2]};
            if (f[1] >= 0 && l.quadro > f[1] + 200) break;
        }
        return new int[] {-1, f[2]};
    }

    static void tag() {
        titulo("6. TAG depois da assistência");
        CombatConfig c = new CombatConfig();
        AttackDefinition golpe = ch("p02").attack(ch("p02").fighter.assistMove);
        System.out.printf(Locale.ROOT, "Assist da Yin-lu: %s, %d quadros (entra em %d, sai em %d).%n%n", golpe.id, golpe.totalFrames,
            c.assistEnterFrames, c.assistLeaveFrames);
        System.out.println("Ryūga chama a Yin-lu (assist) e aperta TAG de novo k quadros depois (boneco parado do outro lado).");
        System.out.println();
        System.out.println("| TAG k quadros depois do assist | a troca sai? | quadros do TAG até a Yin-lu poder agir |");
        System.out.println("|---|---|---|");
        for (int k = 1; k <= 75; k += (k < 60 ? 3 : 1)) {
            int[] r = trocaDepoisDoAssist(k, false, 1);
            String linha = r[0] < 0 ? (r[1] == 1 ? "aceito, mas não trocou" : "não (ignorado)") : "sim";
            System.out.printf(Locale.ROOT, "| %d | %s | %s |%n", k, linha, r[0] < 0 ? "–" : r[0] + " (" + f1(r[0] / 60.0) + " s)");
        }
        System.out.println();
        int falhas = 0, aceitos = 0, n = 300;
        for (long s = 1; s <= n; s++) {
            int[] r = trocaDepoisDoAssist(10, true, s);
            if (r[1] == 1) {
                aceitos++;
                if (r[0] < 0) falhas++;
            }
        }
        System.out.printf(Locale.ROOT, "Com o rival (CPU nível 8) batendo: TAG 10 quadros depois do assist, %d de %d pedidos aceitos não viraram troca (%s).%n",
            falhas, aceitos, pct(falhas / (double)Math.max(1, aceitos)));

        // A troca direta (↓ + TAG), para comparar.
        Luta l = new Luta(new String[] {"player_base", "p02"}, new String[] {"player_base"}, NIVEL, 1, new CombatConfig());
        l.mao[1] = PARADO;
        final int[] t = {-1};
        l.mao[0] = (lu, lado, in) -> {
            in.clear();
            if (t[0] < 0 && lu.f(0).canAct() && lu.flow.phase() == BattleFlow.Phase.FIGHT) { in.tag = true; t[0] = lu.quadro; }
        };
        FighterState antes = l.e.team(0).pointState();
        int direta = -1;
        while (l.quadro < 600) {
            l.passo();
            if (t[0] >= 0 && l.e.team(0).pointState() != antes && l.f(0).canAct()) { direta = l.quadro - t[0]; break; }
        }
        System.out.printf(Locale.ROOT, "Troca direta (↓+TAG) até o parceiro agir: %d quadros (%s s). Recarga do assist %s s, da troca %s s, depois do Assist → Tag %s s.%n",
            direta, f1(direta / 60.0), f1(c.assistCooldownFrames / 60.0), f1(c.tagCooldownFrames / 60.0), f1(c.assistTagCooldownFrames / 60.0));

        // Nas lutas da CPU.
        int pedidos = 0, trocas = 0, perdidos = 0;
        for (int i = 0; i < 300; i++) {
            Random r = new Random(95_000 + i);
            String[] d0 = par(r), d1 = par(r);
            Luta lu = new Luta(d0, d1, NIVEL, 95_000 + i, new CombatConfig());
            boolean[] convertia = new boolean[2];
            while (!lu.acabou() && lu.quadro < 60 * 60 * 4) {
                lu.passo();
                for (int lado = 0; lado < 2; lado++) {
                    TeamSystem.Side s = lu.e.team(lado);
                    if (s.convert && !convertia[lado]) pedidos++;
                    if (s.leavingFrame == 0) trocas++;
                    if (convertia[lado] && !s.convert && s.leavingFrame != 0) perdidos++;
                    convertia[lado] = s.convert;
                }
            }
        }
        System.out.printf(Locale.ROOT, "Lutas CPU × CPU (300): %d pedidos de Assist → Tag, %d viraram troca, %d sumiram sem trocar (%s).%n",
            pedidos, trocas, perdidos, pct(perdidos / (double)Math.max(1, pedidos)));
    }


    // ------------------------------------------------------------------ 5b. busca de rotas

    static final String[] ACOES = {"L", "M", "H", "2L", "2M", "2H", "S", "SUPER", "A", "J", "SJ", "D", "jL", "jM", "jH", "j2H", "W6", "W14"};

    static final class Execucao {
        boolean valida;
        int dano, acertos, parede, chao, duracao;
        boolean assist, superUsado;
    }

    /** Executa a rota (cada passo apertado até sair, até 45 quadros) e deixa o combo acabar. */
    static Execucao executar(String atk, List<String> rota, int barras, boolean tech) {
        CombatConfig cfg = new CombatConfig();
        FighterState a = new FighterState(ch(atk), "P1"), p = new FighterState(ch(parceiro(atk)), "P1");
        FighterState d = new FighterState(ch("player_base"), "P2");
        CombatEngine e = new CombatEngine(a, d, 900f, 1040f, cfg);
        e.setTeam(0, a, p);
        d.life = 10_000_000;
        a.superMeter = barras * CombatConfig.METER_PER_BAR;
        FighterInput in0 = new FighterInput(), in1 = new FighterInput();
        Execucao x = new Execucao();
        ComboSession sess = null;
        CombatFighter me = e.fighter(0), dummy = e.fighter(1);
        for (int k = 0; k < rota.size(); k++) {
            String acao = rota.get(k);
            int espera = acao.equals("W6") ? 6 : acao.equals("W14") ? 14 : 0;
            AttackDefinition antes = me.attack;
            int antesQuadro = me.attackFrame;
            boolean feito = false;
            for (int t = 0; t < 45 && !feito; t++) {
                in0.clear();
                in1.clear();
                if (tech && dummy.status == CombatFighter.Status.AIR_HITSTUN && dummy.stunLeft == 0) in1.light = true;
                int frente = me.facing > 0 ? 1 : 5, baixo = 3;
                if (espera == 0) {
                    switch (acao) {
                        case "L": in0.light = true; break;
                        case "M": in0.medium = true; break;
                        case "H": in0.heavy = true; break;
                        case "2L": in0.direction = me.facing > 0 ? 2 : 4; in0.light = true; break;
                        case "2M": in0.direction = me.facing > 0 ? 2 : 4; in0.medium = true; break;
                        case "2H": in0.direction = me.facing > 0 ? 2 : 4; in0.heavy = true; break;
                        case "S": in0.special = "M"; break;
                        case "SUPER": in0.superAttack = true; break;
                        case "A": in0.assist = true; break;
                        case "J": in0.jump = true; in0.direction = me.facing > 0 ? 8 : 6; break;
                        case "SJ": in0.superJump = true; in0.direction = me.facing > 0 ? 8 : 6; break;
                        case "D": in0.dash = true; in0.direction = frente; break;
                        case "jL": in0.light = true; break;
                        case "jM": in0.medium = true; break;
                        case "jH": in0.heavy = true; break;
                        case "j2H": in0.direction = baixo; in0.heavy = true; break;
                        default: break;
                    }
                }
                boolean noChao = me.grounded;
                boolean assistAntes = e.team(0).assistOut();
                e.step(in0, in1);
                ComboSession s = e.session(1);
                if (sess == null && s != null && s.active) sess = s;
                if (sess != null && s != sess && k > 0) return x;   // o combo caiu antes do passo
                if (espera > 0) {
                    feito = t + 1 >= espera;
                    continue;
                }
                boolean novo = me.attacking() && me.attack != null && (me.attack != antes || me.attackFrame < antesQuadro);
                switch (acao) {
                    case "J": case "SJ": feito = noChao && !me.grounded; break;
                    case "D": feito = me.forwardDashing; break;
                    case "A": feito = !assistAntes && e.team(0).assistOut(); break;
                    case "S": feito = novo && me.attack.id.startsWith("S") && me.attack.kind != AttackDefinition.Kind.SUPER; break;
                    case "SUPER": feito = novo && me.attack.kind == AttackDefinition.Kind.SUPER; break;
                    default: feito = novo && me.attack.id.equals(acao); break;
                }
                if (feito) {
                    antes = me.attack;
                    antesQuadro = me.attackFrame;
                }
            }
            if (!feito) return x;
            if (k == 0 && sess == null) {
                // O primeiro golpe precisa acertar: espera o contato.
                for (int t = 0; t < 30 && sess == null; t++) {
                    in0.clear();
                    e.step(in0, in1);
                    ComboSession s = e.session(1);
                    if (s != null && s.active) sess = s;
                }
                if (sess == null) return x;
            }
            if (sess != null && !sess.active) return x;
            x.assist |= acao.equals("A");
        }
        for (int t = 0; t < 400 && sess != null && sess.active; t++) {
            in0.clear();
            in1.clear();
            if (tech && dummy.status == CombatFighter.Status.AIR_HITSTUN && dummy.stunLeft == 0) in1.light = true;
            e.step(in0, in1);
        }
        if (sess == null) return x;
        x.valida = sess.hitCount >= 2;
        x.dano = sess.comboDamage;
        x.acertos = sess.hitCount;
        x.parede = sess.wallBounces;
        x.chao = sess.groundBounces;
        x.duracao = sess.comboDuration;
        x.superUsado = sess.usesOf("SUPER") > 0;
        return x;
    }

    static final class Candidata {
        final List<String> rota;
        final Execucao x;
        final double nota;

        Candidata(List<String> rota, Execucao x, double nota) { this.rota = rota; this.x = x; this.nota = nota; }
    }

    /** Busca em feixe: estende as melhores rotas um passo por vez. */
    static Candidata buscar(String atk, int barras, boolean comBonus, int largura, int fundo) {
        return buscar(atk, barras, comBonus, largura, fundo, true);
    }

    static Candidata buscar(String atk, int barras, boolean comBonus, int largura, int fundo, boolean comSuper) {
        List<Candidata> feixe = new ArrayList<>();
        feixe.add(new Candidata(new ArrayList<>(), new Execucao(), 0));
        Candidata melhor = null;
        java.util.Set<String> vistos = new java.util.HashSet<>();
        for (int nivel = 0; nivel < fundo; nivel++) {
            List<Candidata> filhos = new ArrayList<>();
            for (Candidata c : feixe) {
                for (String acao : ACOES) {
                    if (!comSuper && acao.equals("SUPER")) continue;
                    if (nivel == 0 && !(acao.equals("L") || acao.equals("2L") || acao.equals("M") || acao.equals("2M") || acao.equals("H"))) continue;
                    List<String> r = new ArrayList<>(c.rota);
                    r.add(acao);
                    Execucao x = executar(atk, r, barras, true);
                    if (!x.valida && nivel > 0) continue;
                    int n = r.size();
                    String chave = x.dano + "/" + x.acertos + "/" + x.parede + x.chao + (x.assist ? "A" : "") + "/" + n + "/" +
                        String.join(",", r.subList(Math.max(0, n - 3), n));
                    if (!vistos.add(chave)) continue;
                    double nota = x.dano + (comBonus ? 4000 * x.parede + 4000 * x.chao + (x.assist ? 3000 : 0) : 0);
                    Candidata f = new Candidata(r, x, nota);
                    filhos.add(f);
                    boolean alvo = !comBonus || x.assist && x.parede > 0 && x.chao > 0;
                    if (x.valida && alvo && (melhor == null || x.dano > melhor.x.dano)) melhor = f;
                }
            }
            if (filhos.isEmpty()) break;
            // Empate (passos de preparação não tiram nada): o mais curto e o que já acertou mais vêm antes.
            filhos.sort((p, q) -> q.nota != p.nota ? Double.compare(q.nota, p.nota) : p.x.acertos != q.x.acertos ? q.x.acertos - p.x.acertos
                : String.join(",", p.rota).compareTo(String.join(",", q.rota)));
            feixe = filhos.subList(0, Math.min(largura, filhos.size()));
        }
        return melhor;
    }

    static void buscaRotas() {
        titulo("5b. Busca de rotas (assist + dois quiques)");
        System.out.println("Busca em feixe (largura 60, até 16 passos) sobre L/M/H, agachados, S1, Super, assist, pulo, super pulo,");
        System.out.println("dash, golpes no ar e esperas. O boneco não defende mas cai de pé (tech no ar) assim que pode.");
        System.out.println();
        double vidaMedia = 0;
        for (String id : ELENCO) vidaMedia += ch(id).fighter.maxLife;
        vidaMedia /= ELENCO.length;
        final double vm = vidaMedia;
        System.out.println("| lutador | barras | maior combo | % da vida | acertos | quiques P/C | assist | rota | assist + 2 quiques: dano | rota | sem Super nenhum: maior | assist + 2 quiques sem Super |");
        System.out.println("|---|---|---|---|---|---|---|---|---|---|---|---|");
        List<String[]> casos = new ArrayList<>();
        for (String atk : ELENCO) for (int b : new int[] {0, 1}) casos.add(new String[] {atk, Integer.toString(b)});
        List<String> linhas = casos.parallelStream().map(c -> {
            int b = Integer.parseInt(c[1]);
            Candidata max = buscar(c[0], b, false, 60, 16);
            Candidata alvo = buscar(c[0], b, true, 60, 16);
            Candidata seco = b == 0 ? buscar(c[0], 0, false, 60, 16, false) : null;
            Candidata secoAlvo = b == 0 ? buscar(c[0], 0, true, 60, 16, false) : null;
            return String.format(Locale.ROOT, "| %s | %d | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s |", nome(c[0]), b,
                max == null ? "–" : Integer.toString(max.x.dano), max == null ? "–" : pct(max.x.dano / vm),
                max == null ? "–" : Integer.toString(max.x.acertos), max == null ? "–" : max.x.parede + "/" + max.x.chao,
                max == null ? "–" : max.x.assist ? "sim" : "não", max == null ? "–" : String.join(" ", max.rota),
                alvo == null ? "não achou" : alvo.x.dano + " (" + pct(alvo.x.dano / vm) + ", " + alvo.x.acertos + " acertos, " + f1(alvo.x.duracao / 60.0) + " s)",
                alvo == null ? "–" : String.join(" ", alvo.rota),
                b > 0 ? "" : seco == null ? "–" : seco.x.dano + " (" + pct(seco.x.dano / vm) + ")",
                b > 0 ? "" : secoAlvo == null ? "não achou" : secoAlvo.x.dano + " (" + pct(secoAlvo.x.dano / vm) + ")");
        }).collect(java.util.stream.Collectors.toList());
        for (String l : linhas) System.out.println(l);
    }

    /** O boneco só segura a defesa enquanto está caído ou levantando: o golpe no 1º quadro em pé entra? */
    static void meaty() {
        int deWakeup = 0, defendidos = 0, total = 0;
        for (String atk : ELENCO) {
            for (long s = 1; s <= 10; s++) {
                Luta l = new Luta(new String[] {atk, parceiro(atk)}, new String[] {"player_base"}, NIVEL, 80_000 + s, new CombatConfig());
                l.f(1).state.life = 10_000_000;
                final boolean[] levantou = {false};
                final int[] janela = {0};
                l.mao[1] = (lu, lado, in) -> {
                    in.clear();
                    CombatFighter b = lu.f(1);
                    boolean caido = b.status == CombatFighter.Status.KNOCKDOWN || b.status == CombatFighter.Status.WAKEUP;
                    if (caido) janela[0] = 4;
                    if (caido || janela[0]-- > 0) defender(lu, 1, in);
                };
                while (l.quadro < 60 * 62) {
                    boolean estava = l.f(1).status == CombatFighter.Status.WAKEUP;
                    l.passo();
                    if (!estava) continue;
                    for (CombatEngine.HitEvent h : l.e.events()) {
                        if (h.attacker != 0) continue;
                        total++;
                        if (h.blocked) defendidos++; else deWakeup++;
                    }
                }
            }
        }
        System.out.printf(Locale.ROOT, "Golpes no quadro em que o boneco fica de pé (segurando defesa): %d, defendidos %d, entraram %d%n", total, defendidos, deWakeup);
    }

    /** Empurrão de cada tipo no Clash com uma pessoa apertando {@code porSegundo} vezes por segundo (5 s de disputa). */
    static int empurraoHumano(Shinkai.Type tipo, double porSegundo, boolean sabe) {
        ShinkaiClash c = new ShinkaiClash(tipo, Shinkai.Type.POWER, 0, 1, -1);
        FighterInput a = new FighterInput(), b = new FighterInput();
        double intervalo = 60.0 / porSegundo, proximo = 0;
        int segurando = 0, fr = 0;
        for (; fr < ShinkaiClash.DURACAO && !c.acabou(); fr++) {
            a.clear();
            boolean toque = fr >= proximo;
            if (toque) proximo += intervalo;
            switch (tipo) {
                case POWER: if (toque) a.heavy = true; break;
                case SPEED: if (toque) a.light = true; break;
                case ENERGY:
                    if (sabe) {
                        // Segura até a carga cheia e solta.
                        if (segurando < ShinkaiClash.CARGA_MAX) { a.holding = true; segurando++; } else segurando = 0;
                    } else if (toque) a.medium = true;
                    break;
                default:
                    if (sabe) { int fase = (fr + 1) % ShinkaiClash.BATIDA; if (fase == 0) a.medium = true; else if (toque) a.medium = true; }
                    else if (toque) a.medium = true;
                    break;
            }
            c.passo(a, b);
        }
        // Empurrão por segundo (até levar a marca ao fim, sem resistência).
        return Math.round(c.marca * 60f / Math.max(1, fr));
    }

    static void clashHumano() {
        System.out.println();
        System.out.println("### Clash com uma pessoa (empurrão por segundo; a marca vai a 600, a disputa dura 5 s)");
        System.out.println();
        System.out.println("| tipo | 6 toques/s | 8 toques/s | 10 toques/s | 12 toques/s | sabendo o jeito do tipo (8 toques/s) | CPU nível 8 |");
        System.out.println("|---|---|---|---|---|---|---|");
        String[] cpu = {"~175-210", "~190-245", "~245", "~260-280"};
        Shinkai.Type[] tipos = Shinkai.Type.values();
        for (int i = 0; i < tipos.length; i++) {
            Shinkai.Type t = tipos[i];
            System.out.printf(Locale.ROOT, "| %s | %d | %d | %d | %d | %d | %s |%n", t.label, empurraoHumano(t, 6, false), empurraoHumano(t, 8, false),
                empurraoHumano(t, 10, false), empurraoHumano(t, 12, false), empurraoHumano(t, 8, true), cpu[i]);
        }
    }

    static void clashDebug() {
        Shinkai.Type[] tipos = Shinkai.Type.values();
        for (Shinkai.Type ta : tipos) for (Shinkai.Type tb : tipos) {
            if (ta == tb) continue;
            Luta l = new Luta(new String[] {"p05"}, new String[] {"p05"}, NIVEL, 1, new CombatConfig());
            forcarTipo(l.f(0).state, ta);
            forcarTipo(l.f(1).state, tb);
            l.f(0).state.shinkai = l.f(1).state.shinkai = CombatConfig.SHINKAI_MAX;
            final boolean[] foi = {false, false};
            Mao liga = (lu, lado, in) -> {
                if (!foi[lado] && lu.e.teams.canShinkai(lu.f(lado)) && (lado == 0 || lu.f(0).state.inShinkai())) {
                    in.shinkai = true;
                    foi[lado] = true;
                }
            };
            l.mao[0] = liga;
            l.mao[1] = liga;
            int[] pushes = new int[2], soma = new int[2];
            ShinkaiClash c = null;
            for (int fr = 0; fr < 900; fr++) {
                l.passo();
                if (l.e.shinkaiClash() != null) c = l.e.shinkaiClash();
                if (c != null && c.disputando()) {
                    for (int i = 0; i < 2; i++) if (c.idadeEmpurrao[i] == 0) { pushes[i]++; soma[i] += c.ultimoEmpurrao[i]; }
                }
                if (c != null && c.acabou()) break;
            }
            System.out.printf(Locale.ROOT, "%s x %s: marca %d tempo %d fim %s lado %d | pushes %d/%d soma %d/%d%n", ta, tb,
                c == null ? 0 : c.marca, c == null ? 0 : c.tempo(), c == null ? "-" : c.fim, c == null ? -2 : c.lado, pushes[0], pushes[1], soma[0], soma[1]);
        }
    }

    public static void main(String[] args) throws Exception {
        String qual = args.length > 0 ? args[0] : "tudo";
        long t0 = System.nanoTime();
        System.out.println("# Bateria de balanço");
        if (qual.equals("clashdebug")) clashDebug();
        if (qual.equals("meaty")) meaty();
        if (qual.equals("clashhumano")) clashHumano();
        if (qual.equals("breaker") || qual.equals("tudo")) breaker();
        if (qual.equals("shinkai") || qual.equals("tudo")) shinkai();
        if (qual.equals("p09") || qual.equals("tudo")) p09();
        if (qual.equals("pressao") || qual.equals("tudo")) pressao();
        if (qual.equals("rotas") || qual.equals("tudo")) rotas();
        if (qual.equals("busca") || qual.equals("tudo")) buscaRotas();
        if (qual.equals("rota")) { for (int i = 2; i < args.length; i++) { List<String> r = java.util.Arrays.asList(args[i].split(",")); for (int k = 1; k <= r.size(); k++) { Execucao x = executar(args[1], r.subList(0, k), 1, true); System.out.println(r.subList(0, k) + " -> valida " + x.valida + " dano " + x.dano + " acertos " + x.acertos + " P/C " + x.parede + "/" + x.chao); } } }
        if (qual.equals("buscaum")) { Candidata c = buscar(args[1], Integer.parseInt(args[2]), args.length > 3, Integer.parseInt(System.getProperty("largura", "60")), 16); System.out.println(c == null ? "nada" : c.x.dano + " " + c.x.acertos + " " + c.x.parede + "/" + c.x.chao + " " + c.x.assist + " " + c.rota); }
        if (qual.equals("tag") || qual.equals("tudo")) tag();
        System.out.printf(Locale.ROOT, "%n(%.0f s)%n", (System.nanoTime() - t0) / 1e9);
    }
}
