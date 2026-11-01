package com.gamelutagpt;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Perfis dos 16 lutadores (docs/balanco/perfis-16.md): cada um contra os outros 15, 1x1, CPU ×
 * CPU, dos dois lados, e a ficha do que é fixo (vida, golpes, alcance, movimento, projétil). Com
 * as fichas em notas padronizadas (z), mede o quanto cada par é parecido. A mesma bateria rodada
 * com outras sementes dá o "piso de ruído": dois lutadores mais próximos que isso não se
 * distinguem pelo que fazem na luta. Roda à mão:
 *
 * <pre>java -cp ... com.gamelutagpt.BateriaPerfis [sementes por confronto] [nível] [pasta de saída]</pre>
 */
public final class BateriaPerfis {
    private BateriaPerfis() {}

    static final String[] NOMES = {"Ryūga", "Yin-lu", "Kōga", "p04", "p05", "p06", "p07", "p08", "p09", "p10",
        "p11", "Necro", "Krüger", "p14", "Natisuko", "p16"};

    /** Uma ficha: os nomes das colunas e um valor por lutador. */
    static final class Fichas {
        final Map<String, double[]> col = new LinkedHashMap<>();

        double[] c(String nome) { return col.computeIfAbsent(nome, k -> new double[BateriaBalanco.ELENCO.length]); }
    }

    // ------------------------------------------------------------------ o que é fixo

    static Fichas fixas() {
        CombatConfig cfg = new CombatConfig();
        Fichas f = new Fichas();
        String[] el = BateriaBalanco.ELENCO;
        for (int i = 0; i < el.length; i++) {
            CharacterDefinition c = BateriaBalanco.ch(el[i]);
            CharacterDefinition.Fighter p = c.fighter;
            f.c("vida")[i] = p.maxLife;
            f.c("anda para frente (px/s)")[i] = p.walkSpeed > 0 ? p.walkSpeed : cfg.walkSpeed;
            f.c("anda para trás (px/s)")[i] = p.walkBackSpeed > 0 ? p.walkBackSpeed : cfg.walkBackSpeed;
            f.c("dash (% do padrão)")[i] = p.dashPermille / 10.0;
            int bq = p.backdashFrames > 0 ? p.backdashFrames : cfg.backdashFrames;
            float bv = p.backdashSpeed > 0 ? p.backdashSpeed : cfg.backdashSpeed;
            f.c("backdash (px)")[i] = bq * bv / 60.0;
            f.c("air dashes")[i] = p.airDashes;
            f.c("pulo no ar (quadros)")[i] = 41.0 * p.jumpPermille / 1000;
            for (String g : new String[] {"L", "M", "H", "2M"}) {
                CharacterDefinition.Move m = c.move(g, false);
                f.c("início do " + g)[i] = m != null && m.attack != null ? m.attack.startupFrames : 0;
            }
            for (String g : new String[] {"L", "M", "H", "2H"}) {
                CharacterDefinition.Move m = c.move(g, false);
                AttackDefinition at = m == null ? null : m.attack;
                f.c("vantagem na defesa do " + g)[i] = at == null ? 0 : at.blockstunFrames - (at.activeFrames - 1 + at.recoveryFrames);
            }
            double melhor = 0;
            for (String g : BateriaMercado.NORMAIS) {
                float[] r = CpuReach.measured(c, g);
                double alc = r == null ? 0 : r[0];
                if (g.equals("M") || g.equals("H") || g.equals("2M")) f.c("alcance do " + g + " (px)")[i] = alc;
                melhor = Math.max(melhor, alc);
            }
            f.c("melhor alcance (px)")[i] = melhor;
            double dano = 0;
            for (String g : new String[] {"L", "M", "H"}) {
                CharacterDefinition.Move m = c.move(g, false);
                if (m != null) dano += m.damage;
            }
            f.c("dano L+M+H (% da vida)")[i] = 100 * dano / p.maxLife;
            CharacterDefinition.Projectile pr = p.energy;
            // O S de perto (tapa, investida, chute) tem caixa de acerto: não é projétil, mesmo com velocidade no pacote.
            boolean temProjetil = pr != null && pr.speed > 0 && (pr.attack == null || !pr.attack.melee());
            f.c("projétil: velocidade (px/s)")[i] = temProjetil ? pr.speed : 0;
            f.c("projétil: alcance (px)")[i] = temProjetil ? pr.range : 0;
            f.c("agarrão: dano")[i] = p.throwDamage > 0 ? p.throwDamage : cfg.throwDamage;
            f.c("agarrão: alcance a mais (px)")[i] = p.throwRangeBonus;
            int esp = 0;
            double danoEsp = 0;
            for (String s : c.specials.keySet()) {
                CharacterDefinition.Move m = c.moveFor(s);
                if (m == null) continue;
                esp++;
                danoEsp += m.damage;
            }
            f.c("especiais de comando")[i] = esp;
            f.c("dano médio dos especiais (% da vida)")[i] = esp == 0 ? 0 : 100 * danoEsp / esp / p.maxLife;
            f.c("fúria (‰)")[i] = p.furyPermille;
            f.c("couraça (quadros)")[i] = p.armorWindow;
        }
        return f;
    }

    // ------------------------------------------------------------------ o que faz na luta

    static final class Lado {
        int lutas, vitorias, quadros, quadrosNeutro, avanca, recua, atacando, acertos, defendidos, contra, pulos;
        double somaDist, danoFeito, danoSofrido, supers, shinkais, combos, danoCombos;
        final Map<String, Double> origem = new LinkedHashMap<>();
    }

    static Fichas comportamento(int sementes, int nivel, long base) {
        String[] el = BateriaBalanco.ELENCO;
        Lado[] st = new Lado[el.length];
        for (int i = 0; i < el.length; i++) st[i] = new Lado();
        for (int a = 0; a < el.length; a++) {
            for (int b = 0; b < el.length; b++) {
                if (a == b) continue;
                for (int s = 0; s < sementes; s++) {
                    long semente = base + (a * 31L + b) * 1000 + s;
                    luta(el, a, b, nivel, semente, st);
                }
            }
        }
        Fichas f = new Fichas();
        String[] origens = {"normal no chão", "normal no ar", "especial", "projétil", "Super", "agarrão"};
        for (int i = 0; i < el.length; i++) {
            Lado x = st[i];
            double min = x.quadros / 3600.0;
            f.c("vitórias (%)")[i] = 100.0 * x.vitorias / x.lutas;
            f.c("duração da luta (s)")[i] = x.quadros / 60.0 / x.lutas;
            f.c("distância no neutro (px)")[i] = x.quadrosNeutro == 0 ? 0 : x.somaDist / x.quadrosNeutro;
            f.c("avança no neutro (%)")[i] = x.quadrosNeutro == 0 ? 0 : 100.0 * x.avanca / x.quadrosNeutro;
            f.c("recua no neutro (%)")[i] = x.quadrosNeutro == 0 ? 0 : 100.0 * x.recua / x.quadrosNeutro;
            f.c("tempo atacando (%)")[i] = 100.0 * x.atacando / x.quadros;
            f.c("acertos por minuto")[i] = x.acertos / min;
            f.c("dano feito por minuto (% de uma vida)")[i] = 100 * x.danoFeito / min / 10000;
            f.c("dano sofrido por minuto (% da vida)")[i] = x.danoSofrido / min;
            double total = x.origem.values().stream().mapToDouble(Double::doubleValue).sum();
            for (String o : origens) f.c("dano de " + o + " (%)")[i] = total == 0 ? 0 : 100 * x.origem.getOrDefault(o, 0.0) / total;
            f.c("combos de 3+ por minuto")[i] = x.combos / min;
            f.c("dano médio do combo de 3+ (% da vida)")[i] = x.combos == 0 ? 0 : x.danoCombos / x.combos;
            f.c("contra-golpes (% dos acertos)")[i] = x.acertos == 0 ? 0 : 100.0 * x.contra / x.acertos;
            f.c("pulos por minuto")[i] = x.pulos / min;
            f.c("defende por minuto")[i] = x.defendidos / min;
            f.c("Supers por luta")[i] = x.supers / x.lutas;
            f.c("SHINKAI por luta")[i] = x.shinkais / x.lutas;
        }
        return f;
    }

    static void luta(String[] el, int a, int b, int nivel, long semente, Lado[] st) {
        BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {el[a]}, new String[] {el[b]}, nivel, semente, new CombatConfig());
        int[] quem = {a, b};
        Lado[] lado = {st[a], st[b]};
        CombatFighter.Status[] antes = {CombatFighter.Status.NEUTRAL, CombatFighter.Status.NEUTRAL};
        boolean[] noChao = {true, true};
        float[] xAntes = {l.f(0).x, l.f(1).x};
        int[] barra = {0, 0};
        boolean[] shinkaiAntes = new boolean[2];
        ComboSession[] vista = new ComboSession[2];
        int quadros = 0;
        while (!l.acabou() && l.quadro < 60 * 60 * 4) {
            l.passo();
            if (l.flow.phase() != BattleFlow.Phase.FIGHT && l.flow.phase() != BattleFlow.Phase.GO) continue;
            quadros++;
            boolean neutro = l.f(0).status == CombatFighter.Status.NEUTRAL && l.f(1).status == CombatFighter.Status.NEUTRAL;
            for (CombatEngine.HitEvent h : l.e.events()) {
                if (h.blocked) {
                    lado[h.defender].defendidos++;
                    continue;
                }
                Lado x = lado[h.attacker];
                x.acertos++;
                x.danoFeito += h.damage;
                lado[h.defender].danoSofrido += 100.0 * h.damage / l.f(h.defender).state.profile.maxLife;
                x.origem.merge(BateriaMercado.origem(h), (double) h.damage, Double::sum);
                if (antes[h.defender] == CombatFighter.Status.ATTACK) x.contra++;
            }
            for (int s = 0; s < 2; s++) {
                CombatFighter f = l.f(s), o = l.f(1 - s);
                Lado x = lado[s];
                if (f.attacking()) x.atacando++;
                if (neutro && f.grounded) {
                    x.quadrosNeutro++;
                    x.somaDist += Math.abs(f.x - o.x);
                    float rumo = (f.x - xAntes[s]) * Math.signum(o.x - f.x);
                    if (rumo > 0.5f) x.avanca++;
                    else if (rumo < -0.5f) x.recua++;
                }
                if (noChao[s] && !f.grounded && antes[s] == CombatFighter.Status.NEUTRAL) x.pulos++;
                int m = f.state.superMeter;
                if (m < barra[s] && barra[s] - m >= CombatConfig.METER_PER_BAR / 2) x.supers++;
                barra[s] = m;
                ComboSession cs = l.e.lastSession(1 - s);
                if (cs != null && cs != vista[s] && cs.attacker == s && !cs.active) {
                    vista[s] = cs;
                    if (cs.hitCount >= 3) {
                        x.combos++;
                        x.danoCombos += 100.0 * cs.comboDamage / o.state.profile.maxLife;
                    }
                }
                boolean sk = f.state.inShinkai();
                if (sk && !shinkaiAntes[s]) x.shinkais++;
                shinkaiAntes[s] = sk;
                antes[s] = f.status;
                noChao[s] = f.grounded;
                xAntes[s] = f.x;
            }
        }
        int venceu = l.flow.winner();
        for (int s = 0; s < 2; s++) {
            lado[s].lutas++;
            lado[s].quadros += quadros;
            if (venceu == s) lado[s].vitorias++;
        }
    }

    // ------------------------------------------------------------------ semelhança

    /**
     * Escala congelada (média e desvio por coluna, de uma rodada de referência): com ela, as notas
     * de rodadas diferentes ficam comparáveis (se o elenco se espalha, as distâncias crescem). O
     * desvio tem piso de 10% da média, para uma coluna quase igual na referência não explodir.
     */
    static Map<String, double[]> ESCALA;

    static void lerOuGravarEscala(String arquivo, Fichas... fichas) throws IOException {
        java.nio.file.Path p = Paths.get(arquivo);
        ESCALA = new LinkedHashMap<>();
        if (Files.exists(p)) {
            for (String l : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                String[] c = l.split(";");
                if (c.length == 3) ESCALA.put(c[0], new double[] {Double.parseDouble(c[1]), Double.parseDouble(c[2])});
            }
            return;
        }
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(p, StandardCharsets.UTF_8))) {
            for (Fichas f : fichas) for (Map.Entry<String, double[]> e : f.col.entrySet()) {
                double[] v = e.getValue();
                double media = Arrays.stream(v).average().orElse(0);
                double dp = Math.sqrt(Arrays.stream(v).map(x -> (x - media) * (x - media)).sum() / v.length);
                dp = Math.max(dp, Math.max(0.1 * Math.abs(media), 1e-3));
                ESCALA.put(e.getKey(), new double[] {media, dp});
                w.println(e.getKey() + ";" + media + ";" + dp);
            }
        }
    }

    /** Notas z por coluna; as colunas iguais para todos ficam de fora (e são listadas). */
    static double[][] notas(Fichas f, List<String> usadas, List<String> iguais) {
        int n = BateriaBalanco.ELENCO.length;
        List<double[]> cols = new ArrayList<>();
        for (Map.Entry<String, double[]> e : f.col.entrySet()) {
            double[] v = e.getValue();
            if (ESCALA != null) {
                double[] mdp = ESCALA.get(e.getKey());
                if (mdp == null) {
                    iguais.add(e.getKey() + " (fora da escala)");
                    continue;
                }
                usadas.add(e.getKey());
                double[] z = new double[n];
                for (int i = 0; i < n; i++) z[i] = (v[i] - mdp[0]) / mdp[1];
                cols.add(z);
                continue;
            }
            double media = Arrays.stream(v).average().orElse(0);
            double dp = Math.sqrt(Arrays.stream(v).map(x -> (x - media) * (x - media)).sum() / n);
            if (dp < 1e-9 || dp / Math.max(1e-9, Math.abs(media)) < 0.005) {
                iguais.add(e.getKey());
                continue;
            }
            usadas.add(e.getKey());
            double[] z = new double[n];
            for (int i = 0; i < n; i++) z[i] = (v[i] - media) / dp;
            cols.add(z);
        }
        double[][] z = new double[n][cols.size()];
        for (int j = 0; j < cols.size(); j++) for (int i = 0; i < n; i++) z[i][j] = cols.get(j)[i];
        return z;
    }

    /** Distância média por coluna (raiz da média dos quadrados das diferenças de nota). */
    static double[][] distancias(double[][] z) {
        int n = z.length;
        double[][] d = new double[n][n];
        for (int i = 0; i < n; i++) for (int k = 0; k < n; k++) {
            double s = 0;
            for (int j = 0; j < z[i].length; j++) s += (z[i][j] - z[k][j]) * (z[i][j] - z[k][j]);
            d[i][k] = z[i].length == 0 ? 0 : Math.sqrt(s / z[i].length);
        }
        return d;
    }

    // ------------------------------------------------------------------ saída

    static String f1(double v) { return String.format(Locale.ROOT, "%.1f", v); }

    static String f2(double v) { return String.format(Locale.ROOT, "%.2f", v); }

    static void tabelaFichas(String titulo, Fichas f) {
        BateriaBalanco.titulo(titulo);
        StringBuilder h = new StringBuilder("| | ");
        for (String n : NOMES) h.append(n).append(" | ");
        System.out.println(h);
        System.out.println("|---" + "|---".repeat(NOMES.length) + "|");
        for (Map.Entry<String, double[]> e : f.col.entrySet()) {
            double[] v = e.getValue();
            double mn = Arrays.stream(v).min().orElse(0), mx = Arrays.stream(v).max().orElse(0);
            StringBuilder l = new StringBuilder("| " + e.getKey() + " | ");
            for (double x : v) {
                String s = Math.abs(x) >= 100 || x == Math.rint(x) ? Long.toString(Math.round(x)) : f1(x);
                // Destaca o maior e o menor de cada linha (quando variam).
                if (mx - mn > 1e-9 && (x == mx || x == mn)) s = "**" + s + "**";
                l.append(s).append(" | ");
            }
            System.out.println(l);
        }
    }

    static void matriz(String titulo, double[][] d) {
        BateriaBalanco.titulo(titulo);
        StringBuilder h = new StringBuilder("| | ");
        for (String n : NOMES) h.append(n).append(" | ");
        System.out.println(h);
        System.out.println("|---" + "|---".repeat(NOMES.length) + "|");
        for (int i = 0; i < d.length; i++) {
            StringBuilder l = new StringBuilder("| " + NOMES[i] + " | ");
            for (int k = 0; k < d.length; k++) l.append(i == k ? "—" : f2(d[i][k])).append(" | ");
            System.out.println(l);
        }
    }

    static void vizinhos(double[][] d, double[] ruido) {
        System.out.println();
        System.out.println("| lutador | mais parecido | distância | mais diferente | distância | piso de ruído do próprio lutador |");
        System.out.println("|---|---|---|---|---|---|");
        for (int i = 0; i < d.length; i++) {
            int perto = -1, longe = -1;
            for (int k = 0; k < d.length; k++) {
                if (k == i) continue;
                if (perto < 0 || d[i][k] < d[i][perto]) perto = k;
                if (longe < 0 || d[i][k] > d[i][longe]) longe = k;
            }
            System.out.println("| " + NOMES[i] + " | " + NOMES[perto] + " | " + f2(d[i][perto]) + " | " + NOMES[longe] + " | "
                + f2(d[i][longe]) + " | " + (ruido == null ? "—" : f2(ruido[i])) + " |");
        }
    }

    /** O que mais distingue cada lutador: as colunas com nota mais longe do meio do elenco. */
    static void marcas(double[][] z, List<String> nomes, Fichas f) {
        BateriaBalanco.titulo("O que mais distingue cada um (nota z mais longe da média do elenco)");
        System.out.println("| lutador | marcas (nota z: + acima, − abaixo da média) |");
        System.out.println("|---|---|");
        for (int i = 0; i < z.length; i++) {
            Integer[] ord = new Integer[nomes.size()];
            for (int j = 0; j < ord.length; j++) ord[j] = j;
            final int li = i;
            Arrays.sort(ord, (x, y) -> Double.compare(Math.abs(z[li][y]), Math.abs(z[li][x])));
            StringBuilder s = new StringBuilder();
            for (int k = 0; k < Math.min(4, ord.length); k++) {
                int j = ord[k];
                if (Math.abs(z[i][j]) < 1.0) break;
                if (s.length() > 0) s.append(" · ");
                double v = f.col.get(nomes.get(j))[i];
                s.append(nomes.get(j)).append(' ').append(z[i][j] > 0 ? "+" : "−").append(f1(Math.abs(z[i][j])))
                    .append(" (").append(Math.abs(v) >= 100 ? Long.toString(Math.round(v)) : f1(v)).append(')');
            }
            System.out.println("| " + NOMES[i] + " | " + (s.length() == 0 ? "nada acima de 1 desvio: perto da média em tudo" : s) + " |");
        }
    }

    static void espalhamento(String titulo, Fichas f, List<String> iguais) {
        BateriaBalanco.titulo(titulo);
        System.out.println("| coluna | mín | mediana | máx | variação (máx ÷ mín) | lutadores fora da mediana (±10%) |");
        System.out.println("|---|---|---|---|---|---|");
        for (Map.Entry<String, double[]> e : f.col.entrySet()) {
            double[] v = e.getValue().clone();
            Arrays.sort(v);
            double med = (v[7] + v[8]) / 2;
            long fora = Arrays.stream(e.getValue()).filter(x -> Math.abs(x - med) > Math.max(0.1 * Math.abs(med), 1e-9)).count();
            String razao = v[0] > 0 ? f2(v[15] / v[0]) : "—";
            System.out.println("| " + e.getKey() + " | " + f1(v[0]) + " | " + f1(med) + " | " + f1(v[15]) + " | " + razao + " | " + fora + " de 16 |");
        }
        if (!iguais.isEmpty()) System.out.println("\nIguais para os 16: " + String.join("; ", iguais) + ".");
    }

    static double media(double[][] d, int[] quem) {
        double s = 0;
        int n = 0;
        for (int a : quem) for (int b : quem) if (a < b) {
            s += d[a][b];
            n++;
        }
        return n == 0 ? 0 : s / n;
    }

    /** O resumo de uma rodada, para pôr lado a lado com as outras (docs/balanco/identidade.md). */
    static void resumo(double[][] dF, double[][] dL, double[][] dT, double[] ruido, Fichas luta, Fichas fixa) {
        int[] todos = new int[16];
        for (int i = 0; i < 16; i++) todos[i] = i;
        int[] nucleo = {0, 1, 2, 3, 4, 9};
        int perto = 0;
        double menor = 9;
        String par = "";
        for (int a = 0; a < 16; a++) for (int b = a + 1; b < 16; b++) {
            if (dT[a][b] < 0.9) perto++;
            if (dT[a][b] < menor) {
                menor = dT[a][b];
                par = NOMES[a] + " e " + NOMES[b];
            }
        }
        double[] v = luta.col.get("vitórias (%)");
        double mv = Arrays.stream(v).average().orElse(0);
        double dpv = Math.sqrt(Arrays.stream(v).map(x -> (x - mv) * (x - mv)).sum() / 16);
        BateriaBalanco.titulo("RESUMO" + (ESCALA != null ? " (escala congelada)" : ""));
        System.out.println("| medida | valor |");
        System.out.println("|---|---|");
        System.out.println("| distância média entre pares: fixa / luta / total | " + f2(media(dF, todos)) + " / " + f2(media(dL, todos)) + " / " + f2(media(dT, todos)) + " |");
        System.out.println("| núcleo (Ryūga, Yin-lu, Kōga, p04, p05, p10): fixa / luta / total | " + f2(media(dF, nucleo)) + " / " + f2(media(dL, nucleo)) + " / " + f2(media(dT, nucleo)) + " |");
        System.out.println("| pares com total abaixo de 0,9 | " + perto + " de 120 |");
        System.out.println("| par mais parecido | " + par + " (" + f2(menor) + ") |");
        System.out.println("| piso de ruído (média) | " + f2(Arrays.stream(ruido).average().orElse(0)) + " |");
        System.out.println("| vitórias: desvio / mín / máx | " + f1(dpv) + " / " + f1(Arrays.stream(v).min().orElse(0)) + " / " + f1(Arrays.stream(v).max().orElse(0)) + " |");
        for (String k : new String[] {"dano de normal no ar (%)", "dano de agarrão (%)", "dano de projétil (%)", "dano de especial (%)",
            "distância no neutro (px)", "avança no neutro (%)", "pulos por minuto"}) {
            double[] c = luta.col.get(k);
            System.out.println("| " + k + ": média / mín / máx | " + f1(Arrays.stream(c).average().orElse(0)) + " / " + f1(Arrays.stream(c).min().orElse(0)) + " / " + f1(Arrays.stream(c).max().orElse(0)) + " |");
        }
    }

    static void csv(String arquivo, double[][] d) throws IOException {
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(Paths.get(arquivo), StandardCharsets.UTF_8))) {
            w.println("," + String.join(",", NOMES));
            for (int i = 0; i < d.length; i++) {
                StringBuilder l = new StringBuilder(NOMES[i]);
                for (double x : d[i]) l.append(',').append(f2(x));
                w.println(l);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        int sementes = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        int nivel = args.length > 1 ? Integer.parseInt(args[1]) : BateriaBalanco.NIVEL;
        String pasta = args.length > 2 ? args[2] : null;
        String escala = args.length > 3 ? args[3] : null;
        long t0 = System.currentTimeMillis();
        System.out.println("# Perfis dos 16 (CPU nível " + nivel + ", " + sementes + " sementes por confronto, dos dois lados)");

        Fichas fixa = fixas();
        Fichas luta = comportamento(sementes, nivel, 7_000_000);
        Fichas luta2 = comportamento(sementes, nivel, 8_000_000);

        tabelaFichas("Ficha fixa (o que está no pacote)", fixa);
        tabelaFichas("Ficha de luta (o que faz contra os outros 15)", luta);

        if (escala != null) lerOuGravarEscala(escala, fixa, luta);
        List<String> usF = new ArrayList<>(), igF = new ArrayList<>(), usL = new ArrayList<>(), igL = new ArrayList<>();
        double[][] zF = notas(fixa, usF, igF), zL = notas(luta, usL, igL);
        espalhamento("O quanto cada coluna fixa varia", fixa, igF);
        espalhamento("O quanto cada coluna de luta varia", luta, igL);

        // Piso de ruído: o mesmo lutador nas duas rodadas (sementes diferentes), nas notas da 1ª.
        double[] ruido = new double[NOMES.length];
        for (int i = 0; i < 16; i++) {
            double s = 0;
            int nc = 0;
            for (String k : usL) {
                double[] v = luta.col.get(k);
                double media = Arrays.stream(v).average().orElse(0);
                double dp = ESCALA != null ? ESCALA.get(k)[1] : Math.sqrt(Arrays.stream(v).map(x -> (x - media) * (x - media)).sum() / 16);
                double dz = (luta.col.get(k)[i] - luta2.col.get(k)[i]) / dp;
                s += dz * dz;
                nc++;
            }
            ruido[i] = Math.sqrt(s / Math.max(1, nc));
        }

        double[][] dF = distancias(zF), dL = distancias(zL);
        double[][] zT = new double[16][];
        for (int i = 0; i < 16; i++) {
            zT[i] = new double[zF[i].length + zL[i].length];
            System.arraycopy(zF[i], 0, zT[i], 0, zF[i].length);
            System.arraycopy(zL[i], 0, zT[i], zF[i].length, zL[i].length);
        }
        double[][] dT = distancias(zT);

        matriz("Distância na ficha fixa (0 = iguais; ~1,4 = tão diferentes quanto dois lutadores ao acaso)", dF);
        vizinhos(dF, null);
        matriz("Distância na ficha de luta", dL);
        vizinhos(dL, ruido);
        matriz("Distância total (fixa + luta)", dT);
        vizinhos(dT, null);
        marcas(zF, usF, fixa);
        marcas(zL, usL, luta);

        resumo(dF, dL, dT, ruido, luta, fixa);
        if (pasta != null) {
            csv(pasta + "/perfis_fixa.csv", dF);
            csv(pasta + "/perfis_luta.csv", dL);
            csv(pasta + "/perfis_total.csv", dT);
        }
        System.out.println("\n(" + (System.currentTimeMillis() - t0) / 1000 + " s)");
    }
}
