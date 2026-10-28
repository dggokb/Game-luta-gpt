package com.gamelutagpt;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * O que faltou da auditoria, frente D (docs/balanco/auditoria.md): a seção 6 do documento (as
 * comparações diretas, cada grupo lado a lado nos mesmos cenários), a dimensão sinergia das fichas
 * C01 e a matriz 1x1 16 × 16 com os espelhos (seção 7.3). Roda à mão:
 *
 * <pre>java -cp ... com.gamelutagpt.BateriaComparacoes [secao6|sinergia|matriz|tudo]</pre>
 */
public final class BateriaComparacoes {
    private BateriaComparacoes() {}

    static final String[] ELENCO = BateriaBalanco.ELENCO;
    static final String[] ESPECIAIS = {"S1", "S2", "S3", "S4"};
    static final int NIVEL = BateriaBalanco.NIVEL;

    static CharacterDefinition ch(String id) { return GeneratedCharacters.get(id); }

    static String nome(String id) { return BateriaBalanco.nome(id); }

    static String pct(double v) { return BateriaBalanco.pct(v); }

    static String f1(double v) { return BateriaBalanco.f1(v); }

    /** Os grupos da seção 6 e o que o documento diz que deve ficar diferente. */
    static final String[][] GRUPOS = {
        {"Shotos técnicos", "Energia e punição à distância × defesa técnica, repel e combos de karatê", "player_base", "p15"},
        {"Lutadores de chutes", "Contra-ataque e pressão próxima × chutes longos e controle de trajetória", "p02", "p14"},
        {"Pressão", "Rekka e avanço × esquiva/brawler × mobilidade aérea", "p03", "p04", "p10"},
        {"Controle de espaço", "Sombra e puxão × chicote × veneno e armadilhas", "p06", "p08", "p11"},
        {"Resistência", "Reação e capangas × agarrão × couraça e impacto", "p07", "p09", "p13"},
        {"Mobilidade", "Teleporte e parede × air dash e acrobacia", "p05", "p10"},
        {"Imprevisibilidade", "Troca de vida por poder × armadilhas e enganos", "p12", "p16"},
    };

    /** Roda {@code r} sem deixar imprimir (as funções das outras baterias imprimem tabelas). */
    static void quieto(Runnable r) {
        PrintStream antes = System.out;
        System.setOut(new PrintStream(new OutputStream() { @Override public void write(int b) {} }));
        try { r.run(); } finally { System.setOut(antes); }
    }

    // ------------------------------------------------------------------ especiais: o que cada um faz

    static String nomeEspecial(CharacterDefinition c, String g) {
        if (g.equals("S1")) return c.fighter.energyName != null ? c.fighter.energyName : "S1";
        CharacterDefinition.Special s = c.specials.get(g);
        return s == null ? null : s.name != null ? s.name : g;
    }

    static boolean temEspecial(CharacterDefinition c, String g) {
        if (g.equals("S1")) return c.fighter.energyCommand != null && c.fighter.energyCommand.length > 0;
        return c.specials.get(g) != null;
    }

    static boolean noAr(CharacterDefinition c, String g) {
        CharacterDefinition.Special s = g.equals("S1") ? null : c.specials.get(g);
        return s != null && s.air;
    }

    /** As mecânicas declaradas do especial (o que o motor faz de diferente com ele). */
    static String mecanicas(CharacterDefinition c, String g) {
        AttackDefinition a = c.attack(BateriaIdentidade.idDe(g));
        List<String> m = new ArrayList<>();
        CharacterDefinition.Projectile p = g.equals("S1") ? c.fighter.energy : c.specials.get(g) == null ? null : c.specials.get(g).projectile;
        if (p != null && (a == null || !a.melee() || p.trapDuration > 0)) m.add(p.trapDuration > 0 ? "armadilha" : "projétil " + Math.round(p.speed) + " px/s");
        if (a != null) {
            AttackDefinition.Mechanics k = a.mechanics;
            if (k.counter != AttackDefinition.Reads.NONE) m.add("contra-golpe (" + k.counter + ")");
            if (k.repel) m.add("repel");
            if (k.evade != AttackDefinition.Reads.NONE) m.add("esquiva (" + k.evade + ")");
            if (k.armorHits > 0) m.add("armadura " + k.armorHits);
            if (k.grab) m.add("agarrão");
            if (k.advanceSpeed > 0) m.add("avanço" + (k.passThrough ? " atravessa" : ""));
            if (k.teleportFrame >= 0) m.add("teleporte");
            if (k.pull > 0) m.add("puxão " + Math.round(k.pull));
            if (k.poisonDamage > 0) m.add("veneno " + k.poisonDamage);
            if (k.lifeCost > 0) m.add("custa " + k.lifeCost + " de vida");
            if (k.chargeFrames > 0) m.add("carregável");
            if (k.echoFrames > 0) m.add("eco");
            if (k.dives()) m.add("mergulho");
            if (k.pound) m.add("pancada no chão");
            if (k.rebound) m.add("rebote");
            if (a.maxHits > 1) m.add(a.maxHits + " acertos");
        }
        return m.isEmpty() ? "golpe simples" : String.join(", ", m);
    }

    /** O que o especial faz no boneco parado a {@code gap}: [dano, eu andei, rival puxado, trocou de lado, envenenou, vida paga, quadros até agir]. */
    static int[] efeito(String id, String g, int gap) {
        BateriaIdentidade.Cena c = new BateriaIdentidade.Cena(id, "player_base", 1000f, 1000f + gap);
        List<BateriaIdentidade.Toque> r = BateriaIdentidade.roteiro(ch(id), g, c.e.config);
        if (r == null) return null;
        CombatFighter me = c.f(0), o = c.f(1);
        float x0 = me.x, ox0 = o.x;
        int vida0 = me.state.life, dano = 0, maxAnda = 0, puxou = 0, livre = -1;
        boolean trocou = false, veneno = false, comecou = false;
        for (int fr = 0; fr < 400; fr++) {
            if (fr < r.size()) BateriaIdentidade.toca(r.get(fr), me.facing, c.in[0]);
            c.passo();
            if (me.attacking()) comecou = true;
            for (CombatEngine.HitEvent h : c.e.events()) if (h.attacker == 0 && !h.blocked) dano += h.damage;
            maxAnda = Math.max(maxAnda, Math.round(me.x - x0));
            puxou = Math.max(puxou, Math.round(ox0 - o.x));
            if (o.x < me.x) trocou = true;
            if (o.state.poisoned()) veneno = true;
            if (comecou && fr >= r.size() && livre < 0 && me.canAct()) livre = fr + 1 - (r.size() - 1);
            if (livre >= 0 && fr > r.size() + 90) break;
        }
        if (!comecou) return null;
        return new int[] {dano, maxAnda, puxou, trocou ? 1 : 0, veneno ? 1 : 0, vida0 - me.state.life, livre};
    }

    /**
     * Janela de reação: o rival faz {@code golpe} (aperto no quadro 6); quem testa aperta o
     * especial {@code g} com o botão em 6 + k, k de −30 a +40. Quantos k o anulam (quem testa não
     * leva nada) e quantos anulam e ainda acertam o rival.
     */
    static int[] janela(String id, String g, String rival, String golpe, int gap) {
        // Controle: parado, quem testa leva o golpe? Se nem parado leva, a janela não diz nada.
        if (!levaParado(id, rival, golpe, gap)) return new int[] {-1, -1, -1};
        int anula = 0, pune = 0, tentou = 0;
        for (int k = -30; k <= 40; k++) {
            BateriaIdentidade.Cena c = new BateriaIdentidade.Cena(id, rival, 1000f, 1000f + gap);
            List<BateriaIdentidade.Toque> eu = BateriaIdentidade.roteiro(ch(id), g, c.e.config);
            List<BateriaIdentidade.Toque> ele = BateriaIdentidade.roteiro(ch(rival), golpe, c.e.config);
            if (eu == null || ele == null) return null;
            int inicioEle = 6 - (ele.size() - 1), inicioEu = 6 + k - (eu.size() - 1);
            if (inicioEle < 0) { int s = -inicioEle; inicioEle += s; inicioEu += s; }
            if (inicioEu < 0) continue;
            tentou++;
            boolean levou = false, acertou = false, eleSoltou = false;
            for (int fr = 0; fr < 160; fr++) {
                if (fr >= inicioEle && fr - inicioEle < ele.size()) BateriaIdentidade.toca(ele.get(fr - inicioEle), c.f(1).facing, c.in[1]);
                if (fr >= inicioEu && fr - inicioEu < eu.size()) BateriaIdentidade.toca(eu.get(fr - inicioEu), c.f(0).facing, c.in[0]);
                c.passo();
                if (c.f(1).attacking()) eleSoltou = true;
                for (CombatEngine.HitEvent h : c.e.events()) {
                    if (h.attacker == 1 && !h.blocked && h.damage > 0) levou = true;
                    if (h.attacker == 0 && !h.blocked) acertou = true;
                }
            }
            if (!eleSoltou) continue;
            if (!levou) {
                anula++;
                if (acertou) pune++;
            }
        }
        return new int[] {anula, pune, tentou};
    }

    static boolean levaParado(String id, String rival, String golpe, int gap) {
        BateriaIdentidade.Cena c = new BateriaIdentidade.Cena(id, rival, 1000f, 1000f + gap);
        List<BateriaIdentidade.Toque> ele = BateriaIdentidade.roteiro(ch(rival), golpe, c.e.config);
        if (ele == null) return false;
        for (int fr = 0; fr < 160; fr++) {
            if (fr < ele.size()) BateriaIdentidade.toca(ele.get(fr), c.f(1).facing, c.in[1]);
            c.passo();
            for (CombatEngine.HitEvent h : c.e.events()) if (h.attacker == 1 && !h.blocked && h.damage > 0) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ de onde vem o dano (CPU)

    static String categoria(String moveId) {
        if (moveId == null) return "outros";
        switch (moveId) {
            case "S": return "S1";
            case "S2": case "S3": case "S4": return "S2–S4";
            case "SUPER": return "Super";
            case "ULTRA": return "Ultra";
            case "THROW": return "agarrão";
            case "SHINKAI_CLASH": return "Clash";
            default:
                if (moveId.matches("j?2?[LMH]|6M|j2H")) return "normais";
                return "outros";
        }
    }

    static final String[] CATEGORIAS = {"normais", "S1", "S2–S4", "Super", "Ultra", "agarrão", "Clash", "outros"};

    /** Lutas 1x1 da CPU contra o elenco: vitórias, dano por categoria e distância média. */
    static final class Fonte {
        int lutas, vitorias;
        double dano, distancia, quadros;
        final Map<String, Double> porCategoria = new LinkedHashMap<>();
        final Map<String, Integer> porGolpe = new HashMap<>();
    }

    static Fonte fonte(String id, int porRival) {
        Fonte f = new Fonte();
        for (String cat : CATEGORIAS) f.porCategoria.put(cat, 0.0);
        int r = 0;
        for (String rival : ELENCO) {
            if (rival.equals(id)) continue;
            r++;
            for (int s = 0; s < porRival; s++) {
                boolean troca = (s & 1) == 1;
                int lado = troca ? 1 : 0;
                BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {troca ? rival : id}, new String[] {troca ? id : rival}, NIVEL,
                    700_000 + r * 100 + s, new CombatConfig());
                while (!l.acabou() && l.quadro < 60 * 60 * 4) {
                    l.passo();
                    if (l.flow.phase() == BattleFlow.Phase.FIGHT) {
                        f.distancia += Math.abs(l.f(0).x - l.f(1).x);
                        f.quadros++;
                    }
                }
                f.lutas++;
                if (l.flow.winner() == lado) f.vitorias++;
                for (Map.Entry<String, Integer> g : l.est[lado].porGolpe.entrySet()) {
                    f.porCategoria.merge(categoria(g.getKey()), (double)g.getValue(), Double::sum);
                    f.porGolpe.merge(g.getKey(), g.getValue(), Integer::sum);
                    f.dano += g.getValue();
                }
            }
        }
        return f;
    }

    // ------------------------------------------------------------------ seção 6

    static void secao6() {
        BateriaBalanco.titulo("Seção 6: comparações diretas");
        for (String id : ELENCO) BateriaBalanco.danoUltra(id);
        // Os perfis da CPU, a mobilidade, o alcance e a punição (das funções da frente A).
        quieto(() -> {
            BateriaIdentidade.t02();
            BateriaIdentidade.t03();
            BateriaIdentidade.t05();
            BateriaIdentidade.t22();
        });
        List<String> todos = new ArrayList<>();
        for (String[] g : GRUPOS) for (int i = 2; i < g.length; i++) if (!todos.contains(g[i])) todos.add(g[i]);
        Map<String, Fonte> fontes = new HashMap<>();
        todos.parallelStream().forEach(id -> {
            Fonte f = fonte(id, 4);
            synchronized (fontes) { fontes.put(id, f); }
        });
        for (String[] g : GRUPOS) {
            System.out.println();
            System.out.println("### " + g[0] + ": " + Arrays.stream(g, 2, g.length).map(BateriaComparacoes::nome).collect(Collectors.joining(" × ")));
            System.out.println();
            System.out.println("Promessa do documento: " + g[1] + ".");
            System.out.println();
            String[] ids = Arrays.copyOfRange(g, 2, g.length);
            // 1. Os especiais.
            System.out.println("**Especiais** (mecânica declarada; alcance efetivo no boneco em pé; vantagem na defesa; o que fazem no boneco a 250 px):");
            System.out.println();
            System.out.println("| lutador | especial | mecânica | alcance | vantagem na defesa | efeito a 250 px |");
            System.out.println("|---|---|---|---|---|---|");
            for (String id : ids) {
                CharacterDefinition c = ch(id);
                double[][] pu = BateriaIdentidade.PUNICAO.get(id);
                for (int k = 0; k < ESPECIAIS.length; k++) {
                    String s = ESPECIAIS[k];
                    if (!temEspecial(c, s)) continue;
                    int alc = noAr(c, s) ? -1 : BateriaIdentidade.alcanceEfetivo(id, s, false);
                    double adv = pu == null ? Double.NaN : pu[6 + k][0];
                    int[] ef = noAr(c, s) ? null : efeito(id, s, 250);
                    String efs = ef == null ? (noAr(c, s) ? "só no ar" : "não sai parado") : String.format(Locale.ROOT, "dano %d, anda %+d, puxa %d%s%s%s, livre em %d q",
                        ef[0], ef[1], ef[2], ef[3] == 1 ? ", passa para trás" : "", ef[4] == 1 ? ", envenena" : "", ef[5] > 0 ? ", paga " + ef[5] + " de vida" : "", ef[6]);
                    System.out.printf(Locale.ROOT, "| %s | %s %s | %s | %s | %s | %s |%n", nome(id), s, nomeEspecial(c, s), mecanicas(c, s),
                        alc < 0 ? (c.attack(BateriaIdentidade.idDe(s)) != null && !c.attack(BateriaIdentidade.idDe(s)).melee() ? "projétil" : "–") : alc + " px",
                        Double.isNaN(adv) ? "–" : String.format(Locale.ROOT, "%+.0f", adv), efs);
                }
            }
            System.out.println();
            // 2. Corpo, alcance e mobilidade.
            System.out.println("**Corpo, alcance e mobilidade:**");
            System.out.println();
            System.out.println("| lutador | vida | L (início) | alcance M / H / 2M | anda | dash 20 q | pulo à frente | air dashes | pulo na parede | super pulo |");
            System.out.println("|---|---|---|---|---|---|---|---|---|---|");
            for (String id : ids) {
                CharacterDefinition c = ch(id);
                int[][] a = BateriaIdentidade.alcances(id);
                float[] m = BateriaIdentidade.MOBILIDADE.get(id);
                System.out.printf(Locale.ROOT, "| %s | %d | %d q | %d / %d / %d | %.0f | %.0f | %.0f | %d | %s | %.0f |%n", nome(id), c.fighter.maxLife,
                    c.attack("L").startupFrames, a[1][0], a[2][0], a[4][0], m[0], m[1], m[2], (int)m[6], m[7] > 0 ? "sim" : "não", m[4]);
            }
            System.out.println();
            // 3. Como a CPU joga com cada um.
            System.out.println("**Como a CPU joga** (1x1 nível 8 contra os outros 15, 4 lutas por rival; depende da CPU):");
            System.out.println();
            StringBuilder cab = new StringBuilder("| lutador | vence | distância média | colado / longe | pulos/min | agarrões/min | S1/min | S2–S4/min |");
            StringBuilder sep = new StringBuilder("|---|---|---|---|---|---|---|---|");
            for (String cat : CATEGORIAS) { cab.append(" dano: ").append(cat).append(" |"); sep.append("---|"); }
            System.out.println(cab);
            System.out.println(sep);
            for (String id : ids) {
                Fonte f = fontes.get(id);
                BateriaIdentidade.Perfil p = BateriaIdentidade.PERFIS.get(id);
                double min = p.quadros / 3600.0;
                int s1 = p.golpes.getOrDefault("S", 0), s24 = p.golpes.getOrDefault("S2", 0) + p.golpes.getOrDefault("S3", 0) + p.golpes.getOrDefault("S4", 0);
                StringBuilder l = new StringBuilder(String.format(Locale.ROOT, "| %s | %s | %.0f px | %s / %s | %.1f | %.1f | %.1f | %.1f |", nome(id),
                    pct(f.vitorias / (double)f.lutas), f.distancia / f.quadros, pct(p.perto / (double)p.quadros), pct(p.longe / (double)p.quadros),
                    p.pulos / min, p.agarroes / min, s1 / min, s24 / min));
                for (String cat : CATEGORIAS) l.append(' ').append(pct(f.porCategoria.get(cat) / Math.max(1, f.dano))).append(" |");
                System.out.println(l);
            }
            System.out.println();
            // 4. Janelas de reação dos especiais defensivos contra o outro do grupo.
            List<String> jl = new ArrayList<>();
            for (String id : ids) {
                CharacterDefinition c = ch(id);
                for (String s : ESPECIAIS) {
                    if (!temEspecial(c, s) || noAr(c, s)) continue;
                    AttackDefinition a = c.attack(BateriaIdentidade.idDe(s));
                    if (a == null) continue;
                    AttackDefinition.Mechanics k = a.mechanics;
                    boolean defensivo = k.counter != AttackDefinition.Reads.NONE || k.repel || k.evade != AttackDefinition.Reads.NONE || k.armorHits > 0;
                    if (!defensivo) continue;
                    for (String rival : ids) {
                        if (rival.equals(id)) continue;
                        for (String[] gg : new String[][] {{"M", "150"}, {"2M", "150"}, {"S1", "450"}}) {
                            CharacterDefinition rc = ch(rival);
                            if (gg[0].equals("S1") && (rc.fighter.energy == null)) continue;
                            int[] w = janela(id, s, rival, gg[0], Integer.parseInt(gg[1]));
                            if (w == null) continue;
                            if (w[0] < 0) {
                                jl.add(String.format(Locale.ROOT, "| %s %s %s | %s %s a %s px | não alcança quem está parado | – |", nome(id), s,
                                    nomeEspecial(c, s), nome(rival), gg[0], gg[1]));
                                continue;
                            }
                            jl.add(String.format(Locale.ROOT, "| %s %s %s | %s %s a %s px | %d de %d | %d |", nome(id), s, nomeEspecial(c, s), nome(rival), gg[0], gg[1],
                                w[0], w[2], w[1]));
                        }
                    }
                }
            }
            if (!jl.isEmpty()) {
                System.out.println("**Janelas de reação** (o especial defensivo apertado de 30 quadros antes a 40 depois do aperto do rival; em quantos quadros ele anula o golpe, e em quantos ainda acerta):");
                System.out.println();
                System.out.println("| quem reage | contra | anula em | anula e acerta em |");
                System.out.println("|---|---|---|---|");
                for (String l : jl) System.out.println(l);
                System.out.println();
            } else {
                System.out.println("Sem especial defensivo (contra-golpe, repel, esquiva, armadura) no grupo.");
                System.out.println();
            }
            // 5. Os do grupo entre si.
            if (ids.length >= 2) {
                System.out.println("**Entre si** (1x1, 20 lutas por par, metade de cada lado):");
                System.out.println();
                for (int i = 0; i < ids.length; i++) {
                    for (int j = i + 1; j < ids.length; j++) {
                        int va = 0, n = 20;
                        for (int s = 0; s < n; s++) {
                            boolean troca = (s & 1) == 1;
                            BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {troca ? ids[j] : ids[i]}, new String[] {troca ? ids[i] : ids[j]}, NIVEL,
                                800_000 + i * 100 + j * 10 + s, new CombatConfig());
                            if (l.rodar() == (troca ? 1 : 0)) va++;
                        }
                        System.out.printf(Locale.ROOT, "- %s vence %s de %s%n", nome(ids[i]), pct(va / (double)n), nome(ids[j]));
                    }
                }
                System.out.println();
            }
        }
    }

    // ------------------------------------------------------------------ sinergia (C01)

    static void sinergia() {
        BateriaBalanco.titulo("C01 sinergia: o assist de cada um e o que o assist rende para cada um");
        for (String id : ELENCO) BateriaBalanco.danoUltra(id);
        String[] calibracao = {"player_base", "p03", "p09", "p11"};
        Map<String, double[]> r = new LinkedHashMap<>();
        Arrays.stream(ELENCO).parallel().forEach(x -> {
            // Como assist (chamado por p01; o de p01 chamado pela Yin-lu): alcance, antiaéreo, cobertura.
            int min = -1, max = -1, inicio = -1;
            for (int g = 100; g <= 1000; g += 50) {
                int[] a = BateriaEquipe.chamarAssist(x, g, 0, 0);
                if (a == null) continue;
                if (min < 0) { min = g; inicio = a[0]; }
                max = g;
            }
            int aa = 0;
            for (int atraso = 0; atraso < 48; atraso += 4) {
                int[] a = BateriaEquipe.chamarAssist(x, 250, 2, atraso);
                if (a != null && a[4] == 1) aa++;
            }
            int sem = BateriaEquipe.vantagemCobertura(x, false), com = BateriaEquipe.vantagemCobertura(x, true);
            double cobertura = sem == Integer.MIN_VALUE || com == Integer.MIN_VALUE ? 0 : com - sem;
            // Como parceiro: o que o assist dele soma ao maior combo dos quatro de calibração.
            double soma = 0;
            int n = 0;
            for (String t : calibracao) {
                if (t.equals(x)) continue;
                BateriaEquipe.Cand c = BateriaEquipe.buscar(t, x, 0, true, false, 40, 14);
                int semA = SEM_ASSIST.computeIfAbsent(t, BateriaEquipe::semAssistMax);
                soma += (c == null ? 0 : c.x.dano) - semA;
                n++;
            }
            // Como titular: o que o parceiro oficial soma ao maior combo dele.
            BateriaEquipe.Cand oficial = BateriaEquipe.buscar(x, BateriaBalanco.parceiro(x), 0, true, false, 40, 14);
            int semX = SEM_ASSIST.computeIfAbsent(x, BateriaEquipe::semAssistMax);
            double ganhoTitular = (oficial == null ? 0 : oficial.x.dano) - semX;
            synchronized (r) {
                r.put(x, new double[] {min, max, inicio, aa / 12.0, cobertura, soma / n, ganhoTitular, semX});
            }
        });
        System.out.println("Como assist: alcance (distância do titular ao boneco em que acerta), início, antiaéreo (acerta o boneco");
        System.out.println("pulando a 250 px em quantos de 12 tempos), cobertura (vantagem do titular num H defendido com o assist");
        System.out.println("chamado junto, menos sem). Como parceiro: quanto o assist soma ao maior combo (sem barra, sem Super) de");
        System.out.println("Ryūga, Kōga, p09 e p11. Como titular: quanto o parceiro oficial soma ao maior combo dele.");
        System.out.println();
        Map<String, Double> nota = new HashMap<>();
        for (String x : ELENCO) {
            double[] v = r.get(x);
            double alcance = v[0] < 0 ? 0 : (v[1] - v[0] + 50) / 900.0;
            nota.put(x, alcance + v[3] + v[4] / 20.0 + v[5] / 2000.0 + v[6] / 2000.0);
        }
        System.out.println("| lutador | assist | alcance | início | antiaéreo | cobertura | soma ao combo de outros (média) | o oficial soma ao dele | nota sinergia (1–5) |");
        System.out.println("|---|---|---|---|---|---|---|---|---|");
        for (String x : ELENCO) {
            double[] v = r.get(x);
            CharacterDefinition c = ch(x);
            System.out.printf(Locale.ROOT, "| %s | %s | %s | %s | %s | %+.0f | %+.0f | %+.0f (%s) | %d |%n", nome(x), c.fighter.assistMove,
                v[0] < 0 ? "não acerta" : (int)v[0] + "–" + (int)v[1] + " px", v[2] < 0 ? "–" : (int)v[2] + " q", pct(v[3]), v[4], v[5], v[6],
                nome(BateriaBalanco.parceiro(x)), BateriaIdentidade.nota(nota, x));
        }
        NOTA_SINERGIA.putAll(nota);
    }

    static final Map<String, Integer> SEM_ASSIST = new java.util.concurrent.ConcurrentHashMap<>();
    static final Map<String, Double> NOTA_SINERGIA = new HashMap<>();

    /** As fichas C01 da frente A com a sinergia (as outras seis notas recalculadas no código atual). */
    static void fichas() {
        BateriaBalanco.titulo("C01 fichas completas (sete dimensões, notas 1–5 por quintil do elenco)");
        // As seis da frente A, com os mapas dela.
        PrintStream antes = System.out;
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        System.setOut(new PrintStream(buf));
        try {
            if (BateriaIdentidade.ALCANCE.isEmpty()) BateriaIdentidade.t02();
            if (BateriaIdentidade.MOBILIDADE.isEmpty()) BateriaIdentidade.t03();
            if (BateriaIdentidade.AA.isEmpty()) BateriaIdentidade.t04();
            if (BateriaIdentidade.PUNICAO.isEmpty()) BateriaIdentidade.t05();
            if (BateriaIdentidade.PERFIS.isEmpty()) BateriaIdentidade.t22();
            buf.reset();
            BateriaIdentidade.c01();
        } finally {
            System.setOut(antes);
        }
        // Pega a tabela da frente A e acrescenta a coluna sinergia.
        String[] linhas = buf.toString().split("\n");
        for (String l : linhas) {
            if (l.startsWith("| lutador |")) {
                System.out.println("| lutador | alcance | mobilidade | pressão | defesa | dano explosivo | controle de espaço | sinergia | números brutos |");
                System.out.println("|---|---|---|---|---|---|---|---|---|");
                continue;
            }
            if (l.startsWith("|---")) continue;
            if (!l.startsWith("| ")) continue;
            String[] c = l.split(" \\| ");
            String quem = c[0].substring(2).trim();
            String id = null;
            for (String x : ELENCO) if (nome(x).equals(quem)) id = x;
            if (id == null) continue;
            int s = NOTA_SINERGIA.isEmpty() ? 0 : BateriaIdentidade.nota(NOTA_SINERGIA, id);
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < 7; i++) b.append(i == 0 ? c[0] : c[i]).append(" | ");
            b.append(s).append(" | ");
            for (int i = 7; i < c.length; i++) b.append(c[i]).append(i < c.length - 1 ? " | " : "");
            System.out.println(b);
        }
    }

    // ------------------------------------------------------------------ matriz 16 × 16

    static void matriz() {
        BateriaBalanco.titulo("Matriz 1x1 16 × 16 (seção 7.3)");
        for (String id : ELENCO) BateriaBalanco.danoUltra(id);
        int n = ELENCO.length, sementes = 10;
        int[][] vit = new int[n][n], dec = new int[n][n];
        List<int[]> celulas = new ArrayList<>();
        for (int a = 0; a < n; a++) for (int b = 0; b < n; b++) celulas.add(new int[] {a, b});
        celulas.parallelStream().forEach(cel -> {
            int a = cel[0], b = cel[1], v = 0, d = 0;
            for (int s = 0; s < sementes; s++) {
                BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {ELENCO[a]}, new String[] {ELENCO[b]}, NIVEL, 900_000 + a * 1000 + b * 20 + s, new CombatConfig());
                int w = l.rodar();
                if (w == 0) v++;
                if (w == 0 || w == 1) d++;
            }
            vit[a][b] = v;
            dec[a][b] = d;
        });
        System.out.println("CPU nível 8 dos dois lados, 10 sementes por célula. Linha = lado esquerdo (P1), coluna = lado direito (P2);");
        System.out.println("célula = vitórias do lado esquerdo (de 10). Empates e tempo esgotado sem vencedor contam como não vitória.");
        System.out.println();
        StringBuilder cab = new StringBuilder("| P1 \\ P2 |"), sep = new StringBuilder("|---|");
        for (String b : ELENCO) { cab.append(' ').append(b.equals("player_base") ? "p01" : b).append(" |"); sep.append("---|"); }
        cab.append(" total P1 |");
        sep.append("---|");
        System.out.println(cab);
        System.out.println(sep);
        int esq = 0, jogos = 0, espelhoEsq = 0, espelhoJogos = 0;
        for (int a = 0; a < n; a++) {
            StringBuilder l = new StringBuilder("| " + (ELENCO[a].equals("player_base") ? "p01" : ELENCO[a]) + " |");
            int ta = 0;
            for (int b = 0; b < n; b++) {
                l.append(' ').append(a == b ? "**" + vit[a][b] + "**" : Integer.toString(vit[a][b])).append(" |");
                ta += vit[a][b];
                esq += vit[a][b];
                jogos += sementes;
                if (a == b) { espelhoEsq += vit[a][b]; espelhoJogos += sementes; }
            }
            l.append(' ').append(pct(ta / (double)(n * sementes))).append(" |");
            System.out.println(l);
        }
        System.out.println();
        System.out.printf(Locale.ROOT, "Viés de lado: o lado esquerdo vence %s das %d lutas (%s nos espelhos: %d de %d).%n", pct(esq / (double)jogos), jogos,
            pct(espelhoEsq / (double)espelhoJogos), espelhoEsq, espelhoJogos);
        System.out.println();
        System.out.println("Espelhos (lado esquerdo vence, de 10):");
        System.out.println();
        StringBuilder es = new StringBuilder();
        for (int a = 0; a < n; a++) es.append(nome(ELENCO[a])).append(' ').append(vit[a][a]).append(a < n - 1 ? "; " : ".");
        System.out.println(es);
        System.out.println();
        // Força geral (os dois lados somados) e a assimetria de lado por par.
        System.out.println("Força contra o elenco (os dois lados somados, sem os espelhos) e a maior diferença de lado nos pares:");
        System.out.println();
        System.out.println("| # | lutador | vence | como P1 | como P2 |");
        System.out.println("|---|---|---|---|---|");
        double[] forca = new double[n];
        Integer[] ordem = new Integer[n];
        double[][] comoP = new double[n][2];
        for (int a = 0; a < n; a++) {
            int v = 0, j = 0, v1 = 0, v2 = 0;
            for (int b = 0; b < n; b++) {
                if (a == b) continue;
                v += vit[a][b] + (sementes - vit[b][a]);
                v1 += vit[a][b];
                v2 += sementes - vit[b][a];
                j += 2 * sementes;
            }
            forca[a] = v / (double)j;
            comoP[a][0] = v1 / (double)((n - 1) * sementes);
            comoP[a][1] = v2 / (double)((n - 1) * sementes);
            ordem[a] = a;
        }
        Arrays.sort(ordem, (x, y) -> Double.compare(forca[y], forca[x]));
        for (int k = 0; k < n; k++) {
            int a = ordem[k];
            System.out.printf(Locale.ROOT, "| %d | %s | %s | %s | %s |%n", k + 1, nome(ELENCO[a]), pct(forca[a]), pct(comoP[a][0]), pct(comoP[a][1]));
        }
        System.out.println();
        List<String> assim = new ArrayList<>();
        for (int a = 0; a < n; a++) for (int b = a + 1; b < n; b++) {
            int ab = vit[a][b], baInv = sementes - vit[b][a];
            if (Math.abs(ab - baInv) >= 6) assim.add(String.format(Locale.ROOT, "%s × %s: %s vence %d de 10 pela esquerda e %d de 10 pela direita", nome(ELENCO[a]), nome(ELENCO[b]),
                nome(ELENCO[a]), ab, baInv));
        }
        System.out.println("Pares em que o lado muda o resultado em 6 ou mais de 10 (" + assim.size() + " de " + n * (n - 1) / 2 + "):");
        System.out.println();
        for (String s : assim) System.out.println("- " + s);
    }

    public static void main(String[] args) {
        String qual = args.length > 0 ? args[0] : "tudo";
        long t0 = System.nanoTime();
        System.out.println("# Auditoria, frente D");
        if (qual.equals("secao6") || qual.equals("tudo")) secao6();
        if (qual.equals("sinergia") || qual.equals("tudo")) { sinergia(); fichas(); }
        if (qual.equals("matriz") || qual.equals("tudo")) matriz();
        System.out.printf(Locale.ROOT, "%n(%.0f s)%n", (System.nanoTime() - t0) / 1e9);
    }
}
