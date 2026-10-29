package com.gamelutagpt;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * As métricas fixas do antes/depois (docs/balanco/ajustes.md): cada mudança aprovada roda isto nas
 * mesmas sementes antes e depois, e a tabela compara. Uma linha por métrica, "nome<TAB>valor".
 *
 * <pre>java -cp ... com.gamelutagpt.BateriaAntesDepois &lt;arquivo.tsv&gt; [grupos: breaker,clash,tag,elenco,p09,combos]</pre>
 */
public final class BateriaAntesDepois {
    private BateriaAntesDepois() {}

    static final Map<String, Double> M = new LinkedHashMap<>();

    static void put(String k, double v) {
        M.put(k, v);
        System.out.printf(Locale.ROOT, "%s\t%.3f%n", k, v);
    }

    // ---------------------------------------------------------------- Breaker

    static void breaker() {
        double evitado = 0, troco = 0, total = 0;
        int n = 0;
        for (String atk : BateriaBalanco.ELENCO) {
            for (long s = 1; s <= 16; s++) {
                BateriaBalanco.ComboUnico sem = BateriaBalanco.comboNoBoneco(atk, s, false, 0);
                BateriaBalanco.ComboUnico com = BateriaBalanco.comboNoBoneco(atk, s, true, 0);
                if (sem.acertosTotal < 2 || !com.quebrou) continue;
                total += sem.danoTotal;
                evitado += sem.danoTotal - com.danoAntes;
                troco += com.troco;
                n++;
            }
        }
        put("breaker.casos", n);
        put("breaker.troco_garantido_script", trocoGarantido());
        put("breaker.combo_inteiro", total / n);
        put("breaker.dano_evitado", evitado / n);
        put("breaker.troco", troco / n);
        double[] r = BateriaBalanco.confronto(BateriaBalanco.SEMPRE_QUEBRA, BateriaBalanco.NUNCA_QUEBRA, 300, 10_000);
        put("breaker.sempre_vs_nunca", r[0]);
        double[] c = BateriaBalanco.confronto(null, BateriaBalanco.NUNCA_QUEBRA, 300, 10_000);
        put("breaker.cpu_vs_nunca", c[0]);
        put("breaker.cpu_quebras_por_luta", c[2]);
        // Quantas vezes a CPU quebra das chances que teve (o "!" apareceu com barra).
        int chances = 0, quebras = 0;
        for (int i = 0; i < 200; i++) {
            Random rr = new Random(11_000 + i);
            String[] d = BateriaBalanco.par(rr);
            BateriaBalanco.Luta l = new BateriaBalanco.Luta(d, d, BateriaBalanco.NIVEL, 11_000 + i, new CombatConfig());
            ComboSession vista = null;
            boolean contou = false;
            while (!l.acabou() && l.quadro < 60 * 60 * 4) {
                l.passo();
                ComboSession s = l.e.session(1);
                if (s != vista) { vista = s; contou = false; }
                if (!contou && s != null && l.e.canBreakCombo(l.f(1))) { chances++; contou = true; }
                if (l.e.cues().contains("COMBO_BREAKER") && l.f(1).comboBroke) quebras++;
            }
        }
        put("breaker.cpu_quebra_das_chances", quebras / (double)Math.max(1, chances));
    }

    /**
     * Depois da quebra, quem quebrou faz o melhor troco possível (corre e aperta o golpe que alcança)
     * e quem batia defende perfeito assim que pode: a fração de quebras em que o troco entra mesmo
     * assim (garantido).
     */
    static double trocoGarantido() {
        int quebras = 0, entrou = 0;
        for (String atk : BateriaBalanco.ELENCO) {
            for (long s = 1; s <= 8; s++) {
                BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {atk}, new String[] {"player_base"}, BateriaBalanco.NIVEL, s, new CombatConfig());
                l.f(1).state.superMeter = CombatConfig.METER_PER_BAR;
                final int[] quebrou = {-1};
                l.mao[1] = (lu, lado, in) -> {
                    in.clear();
                    CombatFighter me = lu.f(1), o = lu.f(0);
                    if (quebrou[0] < 0) {
                        if (lu.e.canBreakCombo(me)) in.light = true;
                        return;
                    }
                    if (!me.canAct()) return;
                    float gap = Math.abs(o.x - me.x);
                    CharacterDefinition c = me.character();
                    for (String g : new String[] {"2M", "M", "L", "2L"}) {
                        if (c.attack(g) != null && CombatRules.reaches(c, g, false, gap, o.body())) {
                            if (g.startsWith("2")) in.direction = me.facing > 0 ? 2 : 4;
                            if (g.endsWith("L")) in.light = true; else in.medium = true;
                            return;
                        }
                    }
                    in.direction = o.x > me.x ? 1 : 5;
                    in.dash = true;
                };
                l.mao[0] = (lu, lado, in) -> {
                    if (quebrou[0] >= 0) { in.clear(); BateriaBalanco.defender(lu, 0, in); }
                };
                for (int fr = 0; fr < 60 * 30; fr++) {
                    l.passo();
                    if (quebrou[0] < 0 && l.e.cues().contains("COMBO_BREAKER")) { quebrou[0] = l.quadro; quebras++; continue; }
                    if (quebrou[0] >= 0) {
                        boolean fim = false;
                        for (CombatEngine.HitEvent h : l.e.events()) {
                            if (h.attacker != 1) continue;
                            if (!h.blocked) entrou++;
                            fim = true;
                        }
                        if (fim || l.quadro - quebrou[0] > 90) break;
                    }
                }
            }
        }
        return entrou / (double)Math.max(1, quebras);
    }

    // ---------------------------------------------------------------- SHINKAI Clash

    static void clash() {
        Shinkai.Type[] tipos = Shinkai.Type.values();
        for (Shinkai.Type ta : tipos) {
            int va = 0, jogos = 0;
            for (Shinkai.Type tb : tipos) {
                if (ta == tb) continue;
                for (int i = 0; i < 60; i++) {
                    String c = BateriaBalanco.ELENCO[i % BateriaBalanco.ELENCO.length];
                    boolean troca = (i & 1) == 1;
                    int la = troca ? 1 : 0;
                    BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {c}, new String[] {c}, BateriaBalanco.NIVEL, 30_000 + i, new CombatConfig());
                    BateriaBalanco.forcarTipo(l.f(la).state, ta);
                    BateriaBalanco.forcarTipo(l.f(1 - la).state, tb);
                    l.f(0).state.shinkai = l.f(1).state.shinkai = CombatConfig.SHINKAI_MAX;
                    final boolean[] foi = {false, false};
                    BateriaBalanco.Mao liga = (lu, lado, in) -> {
                        if (!foi[lado] && lu.e.teams.canShinkai(lu.f(lado)) && (lado == la || lu.f(la).state.inShinkai())) {
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
                                jogos++;
                                if (h.attacker == la) va++;
                                fim = true;
                            }
                        }
                        if (fim || l.e.shinkaiClash() != null && l.e.shinkaiClash().acabou()) break;
                    }
                }
            }
            put("clash.cpu_vence." + ta.label, va / (double)Math.max(1, jogos));
        }
        for (Shinkai.Type t : tipos) {
            for (int toques : new int[] {6, 8, 12}) put("clash.humano." + t.label + "." + toques + "toques_s", BateriaBalanco.empurraoHumano(t, toques, false));
            put("clash.humano." + t.label + ".sabendo_8", BateriaBalanco.empurraoHumano(t, 8, true));
        }
        // Toque de tela de verdade (o dedo fica 3 quadros apertando) e quem sabe o ritmo (só no pulso).
        for (Shinkai.Type t : tipos) {
            put("clash.tela." + t.label + ".8toques_s", empurraoTela(t, 8, false));
            put("clash.tela." + t.label + ".12toques_s", empurraoTela(t, 12, false));
            put("clash.tela." + t.label + ".melhor_jeito", empurraoTela(t, 8, true));
        }
        // Lutas 2x2 com o tipo forçado: média de vitórias de cada tipo contra os outros três.
        for (Shinkai.Type ta : tipos) {
            int va = 0, decididas = 0;
            for (Shinkai.Type tb : tipos) {
                if (ta == tb) continue;
                for (int i = 0; i < 100; i++) {
                    Random r = new Random(40_000 + i);
                    String[] dupla = BateriaBalanco.par(r);
                    int la = i & 1;
                    BateriaBalanco.Luta l = new BateriaBalanco.Luta(dupla, dupla, BateriaBalanco.NIVEL, 40_000 + i, new CombatConfig());
                    for (FighterState m : l.e.team(la).members) BateriaBalanco.forcarTipo(m, ta);
                    for (FighterState m : l.e.team(1 - la).members) BateriaBalanco.forcarTipo(m, tb);
                    int v = l.rodar();
                    if (v == la) va++;
                    if (v == 0 || v == 1) decididas++;
                }
            }
            put("shinkai.2x2_vence." + ta.label, va / (double)Math.max(1, decididas));
        }
    }

    /**
     * Empurrão por segundo com o dedo na tela: cada toque fica 3 quadros apertado (o motor vê o
     * botão segurando). {@code jeito}: POWER no H, SPEED no L, ENERGY segurando a carga cheia e
     * soltando, TECHNIQUE só no pulso.
     */
    static int empurraoTela(Shinkai.Type tipo, double porSegundo, boolean jeito) {
        ShinkaiClash c = new ShinkaiClash(tipo, Shinkai.Type.POWER, 0, 1, -1);
        FighterInput a = new FighterInput(), b = new FighterInput();
        double intervalo = 60.0 / porSegundo, proximo = 0;
        int dedo = 0, segurando = 0, fr = 0;
        for (; fr < ShinkaiClash.DURACAO && !c.acabou(); fr++) {
            a.clear();
            boolean toque = false;
            if (jeito && tipo == Shinkai.Type.ENERGY) {
                if (segurando < ShinkaiClash.CARGA_MAX) { a.holding = true; segurando++; } else segurando = 0;
            } else if (jeito && tipo == Shinkai.Type.TECHNIQUE) {
                toque = (fr + 1) % ShinkaiClash.BATIDA == 0;
            } else if (fr >= proximo) {
                toque = true;
                proximo += intervalo;
            }
            if (toque) {
                dedo = 3;
                if (tipo == Shinkai.Type.POWER) a.heavy = true;
                else if (tipo == Shinkai.Type.SPEED) a.light = true;
                else a.medium = true;
            }
            if (dedo > 0) { a.holding = true; dedo--; }
            c.passo(a, b);
        }
        return Math.round(c.marca * 60f / Math.max(1, fr));
    }

    // ---------------------------------------------------------------- TAG

    static void tag() {
        int janela = 0, latencia1 = -1;
        for (int k = 1; k <= 90; k++) {
            int[] r = BateriaBalanco.trocaDepoisDoAssist(k, false, 1);
            if (r[0] >= 0) janela = k;
            if (k == 1) latencia1 = r[0];
        }
        put("tag.janela_quadros", janela);
        put("tag.latencia_tag_cedo", latencia1);
        int falhas = 0, aceitos = 0;
        for (long s = 1; s <= 300; s++) {
            int[] r = BateriaBalanco.trocaDepoisDoAssist(10, true, s);
            if (r[1] == 1) {
                aceitos++;
                if (r[0] < 0) falhas++;
            }
        }
        put("tag.falha_com_rival_batendo", falhas / (double)Math.max(1, aceitos));
        int pedidos = 0, perdidos = 0;
        for (int i = 0; i < 300; i++) {
            Random r = new Random(95_000 + i);
            String[] d0 = BateriaBalanco.par(r), d1 = BateriaBalanco.par(r);
            BateriaBalanco.Luta lu = new BateriaBalanco.Luta(d0, d1, BateriaBalanco.NIVEL, 95_000 + i, new CombatConfig());
            boolean[] convertia = new boolean[2];
            while (!lu.acabou() && lu.quadro < 60 * 60 * 4) {
                lu.passo();
                for (int lado = 0; lado < 2; lado++) {
                    TeamSystem.Side s = lu.e.team(lado);
                    if (s.convert && !convertia[lado]) pedidos++;
                    if (convertia[lado] && !s.convert && s.leavingFrame != 0) perdidos++;
                    convertia[lado] = s.convert;
                }
            }
        }
        put("tag.cpu_pedidos", pedidos);
        put("tag.cpu_perdidos", perdidos / (double)Math.max(1, pedidos));
    }

    // ---------------------------------------------------------------- elenco 1x1 e 2x2

    static void elenco() {
        String[] ids = BateriaBalanco.ELENCO;
        int n = ids.length;
        double[] vit = new double[n];
        int[] jogos = new int[n];
        double v09x02 = 0;
        for (int a = 0; a < n; a++) {
            for (int b = a + 1; b < n; b++) {
                int va = 0, decid = 0;
                for (int i = 0; i < 40; i++) {
                    boolean troca = (i & 1) == 1;
                    String[] t0 = {troca ? ids[b] : ids[a]}, t1 = {troca ? ids[a] : ids[b]};
                    BateriaBalanco.Luta l = new BateriaBalanco.Luta(t0, t1, BateriaBalanco.NIVEL, 50_000 + a * 1000 + b * 50 + i, new CombatConfig());
                    int v = l.rodar();
                    if (v == (troca ? 1 : 0)) va++;
                    if (v == 0 || v == 1) decid++;
                }
                double ta = va / (double)Math.max(1, decid);
                vit[a] += ta;
                vit[b] += 1 - ta;
                jogos[a]++;
                jogos[b]++;
                if (ids[a].equals("p02") && ids[b].equals("p09")) v09x02 = 1 - ta;
            }
        }
        for (int a = 0; a < n; a++) put("1x1." + ids[a], vit[a] / jogos[a]);
        put("1x1.p09_vs_p02", v09x02);
        // 2x2 com duplas sorteadas: vitórias de cada lutador (em qualquer posição).
        double[] v2 = new double[n];
        int[] j2 = new int[n];
        List<String> lista = java.util.Arrays.asList(ids);
        for (int i = 0; i < 600; i++) {
            Random r = new Random(60_000 + i);
            String[] d0 = BateriaBalanco.par(r), d1 = BateriaBalanco.par(r);
            BateriaBalanco.Luta l = new BateriaBalanco.Luta(d0, d1, BateriaBalanco.NIVEL, 60_000 + i, new CombatConfig());
            int v = l.rodar();
            if (v != 0 && v != 1) continue;
            for (String id : d0) { int k = lista.indexOf(id); j2[k]++; if (v == 0) v2[k]++; }
            for (String id : d1) { int k = lista.indexOf(id); j2[k]++; if (v == 1) v2[k]++; }
        }
        for (int a = 0; a < n; a++) put("2x2." + ids[a], v2[a] / Math.max(1, j2[a]));
    }

    // ---------------------------------------------------------------- p09 em detalhe

    static void p09() {
        int v09 = 0, jogos = 400;
        double dano09 = 0, agarra = 0, ac = 0;
        for (int i = 0; i < jogos; i++) {
            boolean troca = (i & 1) == 1;
            int l09 = troca ? 1 : 0;
            BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {troca ? "p02" : "p09"}, new String[] {troca ? "p09" : "p02"},
                BateriaBalanco.NIVEL, 60_000 + i, new CombatConfig());
            if (l.rodar() == l09) v09++;
            BateriaBalanco.Estat e9 = l.est[l09];
            dano09 += e9.dano;
            ac += e9.acertos;
            for (Map.Entry<String, Integer> g : e9.porGolpe.entrySet()) {
                AttackDefinition at = BateriaBalanco.ch("p09").attack(g.getKey());
                if ("THROW".equals(g.getKey()) || at != null && at.mechanics.grab) agarra += g.getValue();
            }
            Integer m2 = e9.porGolpe.get("2M");
            if (m2 != null) M.merge("p09.dano_2M_total", (double)m2, Double::sum);
        }
        put("p09.vs_p02_400", v09 / (double)jogos);
        put("p09.dano_por_acerto", dano09 / Math.max(1, ac));
        put("p09.parte_agarroes", agarra / Math.max(1, dano09));
        put("p09.parte_2M", M.getOrDefault("p09.dano_2M_total", 0.0) / Math.max(1, dano09));
        M.remove("p09.dano_2M_total");
    }

    // ---------------------------------------------------------------- combos

    static void combos() {
        List<String> ids = java.util.Arrays.asList(BateriaBalanco.ELENCO);
        Map<String, double[]> r = new LinkedHashMap<>();
        ids.parallelStream().forEach(id -> {
            BateriaBalanco.Candidata c0 = BateriaBalanco.buscar(id, 0, false, 60, 16);
            BateriaBalanco.Candidata c1 = BateriaBalanco.buscar(id, 1, false, 60, 16);
            BateriaBalanco.Candidata a0 = BateriaBalanco.buscar(id, 0, true, 60, 16);
            synchronized (r) {
                r.put(id, new double[] {c0 == null ? 0 : c0.x.dano, c1 == null ? 0 : c1.x.dano, a0 == null ? 0 : a0.x.dano});
            }
        });
        for (String id : ids) {
            double[] v = r.get(id);
            put("combo.max_0barras." + id, v[0]);
            put("combo.max_1barra." + id, v[1]);
            put("combo.assist_2quiques_0barras." + id, v[2]);
        }
        // A rota do Ryūga que achamos antes, exatamente igual.
        List<String> rota = java.util.Arrays.asList("M H A S SUPER D J j2H jM S SUPER W14 W14 S 2L S".split(" "));
        BateriaBalanco.Execucao x = BateriaBalanco.executar("player_base", rota, 0, true);
        put("combo.rota_ryuga_0barras", x.valida ? x.dano : 0);
        put("combo.rota_ryuga_barra_ganha", x.barraGanha);
        put("combo.rota_ryuga_supers", x.supers);
        // A maior rota sem barra de cada um: quanta barra o próprio combo gerou e quantos Supers pagou.
        double ganho = 0, supers = 0;
        for (String id : ids) {
            BateriaBalanco.Candidata c = BateriaBalanco.buscar(id, 0, false, 60, 16);
            if (c == null) continue;
            ganho += c.x.barraGanha;
            supers += c.x.supers;
        }
        put("combo.barra_ganha_media_0barras", ganho / ids.size());
        put("combo.supers_media_0barras", supers / ids.size());
        // Com o SHINKAI ligado (8 s) desde o começo: os combos que passavam da vida inteira (frente B).
        for (String tipo : new String[] {"ENERGY", "TECHNIQUE", "POWER", "SPEED"}) {
            for (String id : new String[] {"player_base", "p09"}) {
                Shinkai.Type t = Shinkai.Type.valueOf(tipo);
                BateriaBalanco.PREPARO = e -> {
                    FighterState st = e.fighter(0).state;
                    BateriaBalanco.forcarTipo(st, t);
                    st.shinkaiFrames = e.config.shinkaiFrames;
                };
                BateriaBalanco.Candidata c = BateriaBalanco.buscar(id, 0, false, 60, 16);
                BateriaBalanco.PREPARO = null;
                put("combo.shinkai_" + tipo + "." + id, c == null ? 0 : c.x.dano);
            }
        }
    }

    /**
     * O grupo combos com a busca ampliada (S1 fraco/forte e S2–S4, {@link BateriaCombos#ampliadas}): a
     * melhor entre a busca antiga e a ampliada, nas mesmas três medidas. O grupo "combos" fica igual.
     */
    static void combosAmpliados() {
        List<String> ids = java.util.Arrays.asList(BateriaBalanco.ELENCO);
        Map<String, double[]> r = new LinkedHashMap<>();
        ids.parallelStream().forEach(id -> {
            String[] amp = BateriaCombos.ampliadas(id, BateriaBalanco.ACOES);
            double[] v = new double[3];
            int[][] modos = {{0, 0}, {1, 0}, {0, 1}};
            for (int i = 0; i < 3; i++) {
                int barras = modos[i][0];
                boolean alvo = modos[i][1] == 1;
                BateriaBalanco.Candidata velha = BateriaBalanco.buscar(id, barras, alvo, 60, 16);
                BateriaBalanco.Candidata nova = BateriaBalanco.buscar(id, barras, alvo, 60, 16, true, amp);
                v[i] = Math.max(velha == null ? 0 : velha.x.dano, nova == null ? 0 : nova.x.dano);
            }
            synchronized (r) {
                r.put(id, v);
            }
        });
        for (String id : ids) {
            double[] v = r.get(id);
            put("combo_amp.max_0barras." + id, v[0]);
            put("combo_amp.max_1barra." + id, v[1]);
            put("combo_amp.assist_2quiques_0barras." + id, v[2]);
        }
    }

    public static void main(String[] args) throws IOException {
        String arquivo = args[0];
        String grupos = args.length > 1 ? args[1] : "breaker,clash,tag,elenco,p09,combos";
        long t0 = System.nanoTime();
        for (String g : grupos.split(",")) {
            switch (g) {
                case "breaker": breaker(); break;
                case "clash": clash(); break;
                case "tag": tag(); break;
                case "elenco": elenco(); break;
                case "p09": p09(); break;
                case "combos": combos(); break;
                case "combos_amp": combosAmpliados(); break;
                default: throw new IllegalArgumentException(g);
            }
        }
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(Paths.get(arquivo), StandardCharsets.UTF_8))) {
            for (Map.Entry<String, Double> e : M.entrySet()) w.printf(Locale.ROOT, "%s\t%.4f%n", e.getKey(), e.getValue());
        }
        System.out.printf(Locale.ROOT, "(%.0f s)%n", (System.nanoTime() - t0) / 1e9);
    }
}
