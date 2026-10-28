package com.gamelutagpt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Auditoria, frente A (docs/balanco/auditoria.md): identidade, movimento, defesa e CPU dos 16
 * lutadores, medidos no motor de verdade (T01–T06, T22, T23 e a ficha C01). Como a
 * {@link BateriaBalanco}, não é teste da suíte: roda à mão.
 *
 * <pre>java -cp ... com.gamelutagpt.BateriaIdentidade [t01|t02|t03|t04|t05|t06|t22|t23|c01|tudo]</pre>
 */
public final class BateriaIdentidade {
    private BateriaIdentidade() {}

    static final String[] ELENCO = BateriaBalanco.ELENCO;
    static final String[] CALIBRACAO = {"player_base", "p03", "p09", "p11"};
    static final String[] NORMAIS = {"L", "M", "H", "2L", "2M", "2H"};

    static CharacterDefinition ch(String id) { return GeneratedCharacters.get(id); }

    static String nome(String id) { return BateriaBalanco.nome(id); }

    static String pct(double v) { return BateriaBalanco.pct(v); }

    static String f1(double v) { return BateriaBalanco.f1(v); }

    // ------------------------------------------------------------------ cena sem CPU

    /** Dois lutadores sozinhos (sem time, sem relógio), cada um com um roteiro por quadro. */
    static final class Cena {
        final CombatEngine e;
        final FighterInput[] in = {new FighterInput(), new FighterInput()};
        int quadro;

        Cena(String a, String b, float xa, float xb) {
            e = new CombatEngine(new FighterState(ch(a), "P1"), new FighterState(ch(b), "P2"), xa, xb, new CombatConfig());
        }

        CombatFighter f(int i) { return e.fighter(i); }

        /** Um quadro; depois as duas entradas voltam ao neutro (o roteiro diz tudo de novo a cada quadro). */
        void passo() {
            e.step(in[0], in[1]);
            in[0].clear();
            in[1].clear();
            quadro++;
        }
    }

    /** Segura a defesa contra o outro na altura certa (em pé por padrão; agacha contra golpe baixo). */
    static void guarda(Cena c, int lado, FighterInput in) {
        CombatFighter me = c.f(lado), o = c.f(1 - lado);
        boolean direita = o.x >= me.x;
        boolean baixo = o.attacking() && o.attack != null && o.attack.low && o.grounded;
        in.direction = baixo ? (direita ? 4 : 2) : (direita ? 5 : 1);
    }

    /** Um quadro de entrada: direção de tela para quem olha para a direita (0..8) e botão. */
    static final class Toque {
        final int dir;
        final char botao;   // ' ', 'L', 'M', 'H', 'S' (Super), 'G' (agarrão)

        Toque(int dir, char botao) { this.dir = dir; this.botao = botao; }
    }

    /**
     * O golpe {@code g} como o jogador aperta: normais, "S1".."S4" (o comando com M), "SUPER" e
     * "THROW". Direções para quem olha para a direita; {@link #toca} espelha.
     */
    static List<Toque> roteiro(CharacterDefinition c, String g, CombatConfig cfg) {
        List<Toque> r = new ArrayList<>();
        switch (g) {
            case "L": case "M": case "H": r.add(new Toque(0, g.charAt(0))); return r;
            case "2L": case "2M": case "2H": r.add(new Toque(3, g.charAt(1))); return r;
            case "SUPER": r.add(new Toque(0, 'S')); return r;
            case "THROW": r.add(new Toque(0, 'G')); return r;
            default: break;
        }
        int[] cmd;
        boolean hold;
        if (g.equals("S1")) {
            cmd = c.fighter.energyCommand;
            hold = c.fighter.energyHold;
        } else {
            CharacterDefinition.Special s = c.specials.get(g);
            if (s == null) return null;
            cmd = s.command();
            hold = s.hold;
        }
        if (cmd == null || cmd.length == 0) return null;
        if (hold) {
            for (int i = 0; i < cfg.chargeFrames + 2; i++) r.add(new Toque(cmd[0], ' '));
            r.add(new Toque(cmd[1], 'M'));
            return r;
        }
        int ultimo = -1;
        for (int d : cmd) {
            if (d == ultimo) r.add(new Toque(0, ' '));
            r.add(new Toque(d, ' '));
            r.add(new Toque(d, ' '));
            ultimo = d;
        }
        r.add(new Toque(ultimo, 'M'));
        return r;
    }

    static void toca(Toque t, int facing, FighterInput in) {
        in.direction = t.dir == 0 ? 0 : MotionParser.relative(t.dir, facing);
        switch (t.botao) {
            case 'L': in.light = true; break;
            case 'M': in.medium = true; break;
            case 'H': in.heavy = true; break;
            case 'S': in.superAttack = true; break;
            case 'G': in.grab = true; break;
            default: break;
        }
    }

    /** Começou um golpe novo neste quadro (o objeto mudou ou o quadro voltou). */
    static boolean comecou(CombatFighter f, AttackDefinition antes, int antesQuadro) {
        return f.attacking() && f.attack != null && (f.attack != antes || f.attackFrame < antesQuadro);
    }

    static String idDe(String g) { return g.equals("S1") ? "S" : g; }

    // ------------------------------------------------------------------ T02 alcance efetivo

    /** Maior distância entre os eixos em que o golpe acerta o boneco parado (em pé ou agachado); -1 se nunca. */
    static int alcanceEfetivo(String atk, String g, boolean agachado) {
        CharacterDefinition c = ch(atk);
        if (c.attack(g) == null) return -1;
        int melhor = -1;
        for (int gap = 40; gap <= 520; gap += 4) {
            if (acerta(atk, g, gap, agachado)) melhor = gap;
            else if (melhor > 0 && gap > melhor + 60) break;
        }
        return melhor;
    }

    static boolean acerta(String atk, String g, int gap, boolean agachado) {
        return acerta(atk, g, gap, agachado, "player_base");
    }

    static boolean acerta(String atk, String g, int gap, boolean agachado, String alvo) {
        Cena c = new Cena(atk, alvo, 1000f, 1000f + gap);
        // O empurrão não pode afastar antes do golpe: começa já na distância pedida.
        List<Toque> r = roteiro(ch(atk), g, c.e.config);
        for (int fr = 0; fr < 90; fr++) {
            c.in[1].direction = agachado ? 3 : 0;
            if (fr < r.size()) toca(r.get(fr), c.f(0).facing, c.in[0]);
            float x0 = c.f(0).x, x1 = c.f(1).x;
            c.passo();
            if (fr == 0) {
                // Corrige o que o empurrão fez no 1º quadro (corpos encostados).
                c.f(0).x = x0;
                c.f(1).x = x1;
            }
            for (CombatEngine.HitEvent h : c.e.events()) if (h.attacker == 0 && !h.blocked) return true;
            if (fr > r.size() + 2 && !c.f(0).attacking()) break;
        }
        return false;
    }

    /** O que a CPU acha que alcança (CombatRules.reaches), na mesma régua. */
    static int alcancePrevisto(String atk, String g) {
        CharacterDefinition c = ch(atk);
        if (c.move(g, false) == null) return -1;
        int melhor = -1;
        for (int gap = 20; gap <= 700; gap += 2) if (CombatRules.reaches(c, g, false, gap, ch("player_base").fighter.body)) melhor = gap;
        return melhor;
    }

    static final Map<String, int[][]> ALCANCE = new java.util.concurrent.ConcurrentHashMap<>();   // id -> [golpe][em pé, agachado, previsto, nominal]

    static int[][] alcances(String id) {
        return ALCANCE.computeIfAbsent(id, k -> {
            int[][] r = new int[NORMAIS.length][4];
            for (int i = 0; i < NORMAIS.length; i++) {
                AttackDefinition a = ch(k).attack(NORMAIS[i]);
                r[i][0] = alcanceEfetivo(k, NORMAIS[i], false);
                r[i][1] = alcanceEfetivo(k, NORMAIS[i], true);
                r[i][2] = alcancePrevisto(k, NORMAIS[i]);
                r[i][3] = a == null ? -1 : Math.round(a.reach);
            }
            return r;
        });
    }

    static void t02() {
        BateriaBalanco.titulo("T02 Controle de espaço: alcance efetivo × nominal");
        System.out.println("Distância entre os eixos (px) em que o golpe ainda acerta o Ryūga parado, em pé / agachado; entre");
        System.out.println("parênteses o que a CPU acha que alcança (CombatRules.reaches) e o alcance nominal do pacote.");
        System.out.println("Corpo do alvo: meia-largura " + Math.round(ch("player_base").fighter.body.halfWidth) + " px.");
        System.out.println();
        StringBuilder cab = new StringBuilder("| lutador |"), sep = new StringBuilder("|---|");
        for (String g : NORMAIS) { cab.append(' ').append(g).append(" |"); sep.append("---|"); }
        cab.append(" projétil (S1) |");
        sep.append("---|");
        System.out.println(cab);
        System.out.println(sep);
        List<String> linhas = Arrays.stream(ELENCO).parallel().map(id -> {
            int[][] r = alcances(id);
            StringBuilder l = new StringBuilder("| " + nome(id) + " |");
            for (int i = 0; i < NORMAIS.length; i++) {
                l.append(r[i][0] < 0 ? " – |" : String.format(Locale.ROOT, " %d / %s (%d, %d) |", r[i][0],
                    r[i][1] < 0 ? "erra" : Integer.toString(r[i][1]), r[i][2], r[i][3]));
            }
            CharacterDefinition.Projectile p = ch(id).fighter.energy;
            boolean melee = p != null && p.attack.melee() && p.speed <= 0f;
            l.append(p == null ? " não |" : String.format(Locale.ROOT, " %s |", p.speed > 0f ? Math.round(p.speed) + " px/s, " + Math.round(p.range) + " px" : "corpo a corpo"));
            return l.toString();
        }).collect(Collectors.toList());
        for (String l : linhas) System.out.println(l);
        // Divergências entre o previsto e o real (a CPU erra a conta).
        System.out.println();
        int dif = 0, tot = 0;
        StringBuilder piores = new StringBuilder();
        for (String id : ELENCO) {
            int[][] r = alcances(id);
            for (int i = 0; i < NORMAIS.length; i++) {
                if (r[i][0] < 0 || r[i][2] < 0) continue;
                tot++;
                int d = r[i][2] - r[i][0];
                if (Math.abs(d) > 30) {
                    dif++;
                    piores.append(String.format(Locale.ROOT, "%s %s %+d; ", nome(id), NORMAIS[i], d));
                }
            }
        }
        System.out.printf(Locale.ROOT, "Previsto − real acima de 30 px: %d de %d golpes. %s%n", dif, tot, piores);
        // Golpes que não acertam agachado.
        StringBuilder erra = new StringBuilder();
        for (String id : ELENCO) {
            int[][] r = alcances(id);
            for (int i = 0; i < NORMAIS.length; i++) if (r[i][0] > 0 && r[i][1] < 0) erra.append(nome(id)).append(' ').append(NORMAIS[i]).append(", ");
        }
        System.out.println("Não acertam o Ryūga agachado: " + (erra.length() == 0 ? "nenhum" : erra.substring(0, erra.length() - 2)) + ".");
        System.out.println();
        System.out.println("Contra cada um dos 16 agachados (alvos que o golpe acerta em alguma distância de 60 a 240 px):");
        System.out.println();
        System.out.println("| atacante | L | M | H |");
        System.out.println("|---|---|---|---|");
        List<String> ag = Arrays.stream(ELENCO).parallel().map(id -> {
            StringBuilder l = new StringBuilder("| " + nome(id) + " |");
            for (String g : new String[] {"L", "M", "H"}) {
                int n = 0;
                for (String alvo : ELENCO) {
                    boolean ok = false;
                    for (int gap = 60; gap <= 240 && !ok; gap += 12) ok = acerta(id, g, gap, true, alvo);
                    if (ok) n++;
                }
                l.append(' ').append(n).append("/16 |");
            }
            return l.toString();
        }).collect(Collectors.toList());
        for (String l : ag) System.out.println(l);
    }

    // ------------------------------------------------------------------ T05 punição

    /** Quadro (desde o aperto) em que quem defendeu fica livre, e a distância nesse quadro. -1 se não defendeu. */
    static int[] livreDepoisDaDefesa(String atk, String g, String def, int gap) {
        Cena c = new Cena(atk, def, 1000f, 1000f + gap);
        List<Toque> r = roteiro(ch(atk), g, c.e.config);
        if (r == null) return null;
        c.f(0).state.superMeter = CombatConfig.METER_PER_BAR;
        boolean defendeu = false;
        for (int fr = 0; fr < 150; fr++) {
            if (fr < r.size()) toca(r.get(fr), c.f(0).facing, c.in[0]);
            guarda(c, 1, c.in[1]);
            c.passo();
            for (CombatEngine.HitEvent h : c.e.events()) {
                if (h.attacker == 0 && h.blocked) defendeu = true;
                if (h.attacker == 0 && !h.blocked) return null;
            }
            if (defendeu && c.f(1).canAct()) return new int[] {fr + 1, Math.round(Math.abs(c.f(0).x - c.f(1).x))};
        }
        return null;
    }

    /**
     * Depois de defender {@code g}, quem defendeu aperta {@code pune} para sair no 1º quadro livre
     * (o comando entra antes, durante a defesa). Quem atacou defende assim que pode. Verdadeiro
     * se o castigo entrou.
     */
    static boolean pune(String atk, String g, String def, String pune, int gap, int livre) {
        Cena c = new Cena(atk, def, 1000f, 1000f + gap);
        List<Toque> r = roteiro(ch(atk), g, c.e.config);
        List<Toque> p = roteiro(ch(def), pune, c.e.config);
        if (r == null || p == null) return false;
        c.f(0).state.superMeter = CombatConfig.METER_PER_BAR;
        c.f(1).state.superMeter = CombatConfig.METER_PER_BAR;
        int inicio = livre - (p.size() - 1);
        for (int fr = 0; fr < livre + 70; fr++) {
            if (fr < r.size()) toca(r.get(fr), c.f(0).facing, c.in[0]);
            else if (c.f(0).canAct() || c.f(0).status == CombatFighter.Status.BLOCKSTUN) guarda(c, 0, c.in[0]);
            if (fr >= inicio && fr - inicio < p.size()) {
                // O comando de quem defendeu: enquanto ainda defende, a guarda fica (o blockstun a guarda).
                toca(p.get(fr - inicio), c.f(1).facing, c.in[1]);
            } else if (fr < inicio) {
                guarda(c, 1, c.in[1]);
            }
            c.passo();
            for (CombatEngine.HitEvent h : c.e.events()) if (h.attacker == 1 && !h.blocked) return true;
        }
        return false;
    }

    static final String[] GOLPES_PUNIR = {"L", "M", "H", "2L", "2M", "2H", "S1", "S2", "S3", "S4"};
    static final String[] CASTIGOS = {"L", "2L", "M", "S1", "S2", "SUPER"};

    /** [atacante][golpe] -> {vantagem média, quantos defensores punem com L, com M, com especial, com Super, com qualquer}. */
    static final Map<String, double[][]> PUNICAO = new LinkedHashMap<>();

    static void t05() {
        BateriaBalanco.titulo("T05 Punição dos golpes defendidos");
        System.out.println("Cada golpe de cada atacante, defendido de perto; cada um dos 16 defensores tenta castigar no primeiro");
        System.out.println("quadro livre com L, 2L, M, S1, S2 ou Super (1 barra), e quem atacou defende assim que pode.");
        System.out.println("Célula: vantagem média na defesa (quadros; − é o defensor livre antes) e quantos dos 16 punem (qualquer castigo).");
        System.out.println();
        List<String> linhas = Arrays.stream(ELENCO).parallel().map(atk -> {
            double[][] res = new double[GOLPES_PUNIR.length][6];
            StringBuilder l = new StringBuilder("| " + nome(atk) + " |");
            for (int gi = 0; gi < GOLPES_PUNIR.length; gi++) {
                String g = GOLPES_PUNIR[gi];
                if (roteiro(ch(atk), g, new CombatConfig()) == null || (!g.startsWith("S") && ch(atk).attack(g) == null)) {
                    l.append(" – |");
                    res[gi][0] = Double.NaN;
                    continue;
                }
                double somaV = 0;
                int nV = 0;
                int[] pun = new int[5];
                for (String def : ELENCO) {
                    int gap = Math.round(ch(atk).fighter.body.pushHalfWidth + ch(def).fighter.body.pushHalfWidth) + 4;
                    int[] livre = livreDepoisDaDefesa(atk, g, def, gap);
                    if (livre == null) continue;
                    // Vantagem: quem atacou livre (aproximado pela mesma cena) menos quem defendeu.
                    int atacanteLivre = atacanteLivre(atk, g, def, gap);
                    if (atacanteLivre >= 0) { somaV += livre[0] - atacanteLivre; nV++; }
                    boolean qualquer = false;
                    for (String p : CASTIGOS) {
                        if (!pune(atk, g, def, p, gap, livre[0])) continue;
                        qualquer = true;
                        int k = p.equals("L") || p.equals("2L") ? 0 : p.equals("M") ? 1 : p.equals("SUPER") ? 3 : 2;
                        pun[k]++;
                    }
                    if (qualquer) pun[4]++;
                }
                res[gi][0] = nV == 0 ? Double.NaN : somaV / nV;
                for (int k = 0; k < 5; k++) res[gi][k + 1] = pun[k];
                l.append(nV == 0 ? " não defendido |" : String.format(Locale.ROOT, " %+.0f / %d |", somaV / nV, pun[4]));
            }
            synchronized (PUNICAO) { PUNICAO.put(atk, res); }
            return l.toString();
        }).collect(Collectors.toList());
        StringBuilder cab = new StringBuilder("| atacante |"), sep = new StringBuilder("|---|");
        for (String g : GOLPES_PUNIR) { cab.append(' ').append(g).append(" |"); sep.append("---|"); }
        System.out.println(cab);
        System.out.println(sep);
        for (String l : linhas) System.out.println(l);
        // Resumo: quem pune com o quê.
        System.out.println();
        System.out.println("Golpes do elenco punidos por pelo menos metade dos defensores, por ferramenta (L/2L, M, especial, Super):");
        int[] porFerramenta = new int[4];
        int inseguros = 0, total = 0;
        for (Map.Entry<String, double[][]> en : PUNICAO.entrySet()) {
            for (double[] r : en.getValue()) {
                if (Double.isNaN(r[0])) continue;
                total++;
                for (int k = 0; k < 4; k++) if (r[k + 1] >= 8) porFerramenta[k]++;
                if (r[5] >= 8) inseguros++;
            }
        }
        System.out.printf(Locale.ROOT, "L/2L %d, M %d, especial %d, Super %d; inseguros (qualquer) %d de %d golpes.%n",
            porFerramenta[0], porFerramenta[1], porFerramenta[2], porFerramenta[3], inseguros, total);
    }

    /** Quadro em que quem atacou fica livre depois do golpe defendido. */
    static int atacanteLivre(String atk, String g, String def, int gap) {
        Cena c = new Cena(atk, def, 1000f, 1000f + gap);
        List<Toque> r = roteiro(ch(atk), g, c.e.config);
        c.f(0).state.superMeter = CombatConfig.METER_PER_BAR;
        boolean saiu = false;
        for (int fr = 0; fr < 200; fr++) {
            if (fr < r.size()) toca(r.get(fr), c.f(0).facing, c.in[0]);
            guarda(c, 1, c.in[1]);
            c.passo();
            if (c.f(0).attacking()) saiu = true;
            if (saiu && fr >= r.size() && c.f(0).canAct()) return fr + 1;
        }
        return -1;
    }

    /**
     * Castigo de golpe errado: o golpe sai a 40 px além do alcance; o rival aperta M, 2M, S1 ou
     * Super chegando no fim do ativo (sem tempo de reação) ou 13 quadros depois (reação humana boa).
     */
    static void t05Erro() {
        System.out.println();
        System.out.println("#### Golpe errado (whiff a 40 px além do alcance)");
        System.out.println();
        System.out.println("Quadros de recuperação depois do ativo; quantos dos quatro de calibração castigam (M, 2M, S1 ou Super) apertando");
        System.out.println("no fim do ativo (sem reação) / 13 quadros depois (reação humana boa).");
        System.out.println();
        System.out.println("| atacante | " + String.join(" | ", NORMAIS) + " |");
        System.out.println("|---|---|---|---|---|---|---|");
        List<String> linhas = Arrays.stream(ELENCO).parallel().map(atk -> {
            StringBuilder l = new StringBuilder("| " + nome(atk) + " |");
            int[][] alc = alcances(atk);
            for (int gi = 0; gi < NORMAIS.length; gi++) {
                String g = NORMAIS[gi];
                AttackDefinition a = ch(atk).attack(g);
                if (a == null || alc[gi][0] < 0) { l.append(" – |"); continue; }
                int gap = alc[gi][0] + 40;
                int[] quem = new int[2];
                for (int k = 0; k < 2; k++) {
                    int t = a.startupFrames + a.activeFrames + (k == 0 ? 0 : 13);
                    for (String def : CALIBRACAO) {
                        for (String p : new String[] {"M", "2M", "S1", "SUPER"}) {
                            if (puneErro(atk, g, def, p, gap, t)) { quem[k]++; break; }
                        }
                    }
                }
                l.append(String.format(Locale.ROOT, " %d q: %d / %d |", a.recoveryFrames, quem[0], quem[1]));
            }
            return l.toString();
        }).collect(Collectors.toList());
        for (String l : linhas) System.out.println(l);
    }

    static boolean puneErro(String atk, String g, String def, String pune, int gap, int t) {
        Cena c = new Cena(atk, def, 1000f, 1000f + gap);
        List<Toque> r = roteiro(ch(atk), g, c.e.config);
        List<Toque> p = roteiro(ch(def), pune, c.e.config);
        if (p == null) return false;
        c.f(1).state.superMeter = CombatConfig.METER_PER_BAR;
        int inicio = Math.max(0, t - (p.size() - 1));
        for (int fr = 0; fr < 120; fr++) {
            if (fr < r.size()) toca(r.get(fr), c.f(0).facing, c.in[0]);
            else if (c.f(0).canAct() || c.f(0).status == CombatFighter.Status.BLOCKSTUN) guarda(c, 0, c.in[0]);
            if (fr >= inicio && fr - inicio < p.size()) toca(p.get(fr - inicio), c.f(1).facing, c.in[1]);
            c.passo();
            for (CombatEngine.HitEvent h : c.e.events()) {
                if (h.attacker == 1 && !h.blocked) return true;
                if (h.attacker == 0 && !h.blocked) return false;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ T03 mobilidade

    static final class Voo {
        float altura, distancia;
        int quadros;
    }

    /** Pulo (dir de tela 7/8/6), super pulo ou pulo + air dash no topo. */
    static Voo voo(String id, String tipo) {
        Cena c = new Cena(id, "player_base", 800f, 2200f);
        CombatFighter f = c.f(0);
        float x0 = f.x, topo = f.y;
        int sobe = -1;
        boolean dash = false;
        Voo v = new Voo();
        for (int fr = 0; fr < 240; fr++) {
            FighterInput in = c.in[0];
            switch (tipo) {
                case "neutro": if (fr < 2) in.direction = 7; break;
                case "frente": if (fr < 2) in.direction = 8; break;
                case "trás": if (fr < 2) in.direction = 6; break;
                case "super": if (fr < 2) in.direction = 3; else if (fr < 4) in.direction = 8; break;
                case "dash":
                    if (fr < 2) in.direction = 8;
                    if (sobe >= 0 && !dash && f.vy >= 0f) { in.dash = true; in.direction = 1; dash = true; }
                    break;
                case "corrida":
                    if (fr == 0) in.dash = true;
                    if (fr < 10) in.direction = 1;
                    else if (fr < 12) in.direction = 8;
                    break;
                case "dash2":
                    if (fr < 2) in.direction = 8;
                    if (sobe >= 0 && !dash && f.vy >= 0f) { in.dash = true; in.direction = 1; dash = true; }
                    else if (dash && f.airDashFrames == 0 && f.airDashCount == 1 && !f.grounded) { in.dash = true; in.direction = 1; }
                    break;
                default: break;
            }
            c.passo();
            if (!f.grounded && sobe < 0) { sobe = fr; x0 = tipo.equals("corrida") ? f.x : x0; }
            topo = Math.min(topo, f.y);
            if (sobe >= 0 && f.grounded) {
                v.quadros = fr - sobe;
                break;
            }
        }
        v.altura = Arena.GROUND_Y - topo;
        v.distancia = f.x - x0;
        return v;
    }

    static final Map<String, float[]> MOBILIDADE = new java.util.concurrent.ConcurrentHashMap<>();   // id -> anda, corre(dist dash), pulo frente dist, airtime, super altura, air dash extra, dashes

    static void t03() {
        BateriaBalanco.titulo("T03 Mobilidade (andar, dash, pulo, super pulo, air dash)");
        System.out.println("| lutador | anda (px/s) | dash: distância em 20 q | pulo neutro: altura / quadros | pulo à frente: distância | pulo atrás | super pulo: altura / distância / quadros | air dash: +distância | air dashes (2º: +distância) | pulo correndo: distância no ar | pulo na parede |");
        System.out.println("|---|---|---|---|---|---|---|---|---|---|---|");
        CombatConfig cfg = new CombatConfig();
        for (String id : ELENCO) {
            CharacterDefinition c = ch(id);
            float anda = c.fighter.walkSpeed > 0 ? c.fighter.walkSpeed : cfg.walkSpeed;
            // Dash: → → e segura → por 20 quadros.
            Cena d = new Cena(id, "player_base", 800f, 2200f);
            float x0 = d.f(0).x;
            for (int fr = 0; fr < 20; fr++) {
                d.in[0].direction = 1;
                if (fr == 0) d.in[0].dash = true;
                d.passo();
            }
            float dash = d.f(0).x - x0;
            Voo n = voo(id, "neutro"), fr = voo(id, "frente"), tr = voo(id, "trás"), su = voo(id, "super"), ad = voo(id, "dash"),
                ad2 = voo(id, "dash2"), co = voo(id, "corrida");
            int dashes = c.fighter.airDashes;
            System.out.printf(Locale.ROOT, "| %s | %.0f | %.0f | %.0f / %d | %.0f | %.0f | %.0f / %.0f / %d | %+.0f | %d%s | %.0f | %s |%n", nome(id), anda, dash,
                n.altura, n.quadros, fr.distancia, tr.distancia, su.altura, su.distancia, su.quadros, ad.distancia - fr.distancia,
                Math.max(1, dashes), dashes > 1 ? String.format(Locale.ROOT, " (%+.0f)", ad2.distancia - ad.distancia) : "", co.distancia,
                c.fighter.wallJump ? "sim" : "não");
            MOBILIDADE.put(id, new float[] {anda, dash, fr.distancia, n.quadros, su.altura, ad.distancia - fr.distancia, Math.max(1, dashes), c.fighter.wallJump ? 1 : 0});
        }
    }

    // ------------------------------------------------------------------ T04 antiaéreo

    static final String[] PULOS = {"pulo à frente", "jH tarde (180 px)", "pulo longo (corrida)", "cruzado", "super pulo", "mergulho (Kōga S4)"};

    /**
     * Quem pula (o Ryūga, ou o Kōga no mergulho) vem com jH (o S4 no mergulho) apertado ao descer;
     * quem defende aperta {@code resposta} terminando no quadro {@code t}. 1: tirou do ar sem levar;
     * 0: nada; -1: levou.
     */
    static int antiAereo(String def, String pulo, String resposta, int t) {
        String jumper = pulo.startsWith("mergulho") ? "p03" : "player_base";
        float gap = pulo.equals("cruzado") ? 150f : pulo.startsWith("pulo longo") ? 520f : pulo.equals("super pulo") ? 330f : pulo.startsWith("mergulho") ? 330f : 300f;
        Cena c = new Cena(jumper, def, 1000f, 1000f + gap);
        List<Toque> r = roteiro(ch(def), resposta, c.e.config);
        if (r == null) return 0;
        CombatFighter j = c.f(0);
        int inicio = t - (r.size() - 1);
        boolean golpe = false, saiu = false;
        int resultado = 0;
        List<Toque> s4 = pulo.startsWith("mergulho") ? roteiro(ch("p03"), "S4", c.e.config) : null;
        int s4Inicio = -1;
        for (int fr = 0; fr < 140; fr++) {
            FighterInput in = c.in[0];
            switch (pulo) {
                case "pulo à frente": case "jH tarde (180 px)": case "cruzado": case "mergulho (Kōga S4)":
                    if (fr < 2) in.direction = 8;
                    break;
                case "pulo longo (corrida)":
                    if (fr == 0) in.dash = true;
                    if (fr < 10) in.direction = 1;
                    else if (fr < 12) in.direction = 8;
                    break;
                case "super pulo":
                    if (fr < 2) in.direction = 3; else if (fr < 4) in.direction = 8;
                    break;
                default: break;
            }
            if (!j.grounded) saiu = true;
            if (saiu && !golpe && !j.grounded && j.vy > 0f) {
                if (s4 != null) {
                    if (s4Inicio < 0) s4Inicio = fr;
                } else if (Arena.GROUND_Y - j.y < (pulo.startsWith("jH tarde") ? 180f : 230f)) {
                    in.heavy = true;
                    golpe = true;
                }
            }
            if (s4Inicio >= 0 && fr - s4Inicio < s4.size()) {
                toca(s4.get(fr - s4Inicio), j.facing, in);
                if (fr - s4Inicio == s4.size() - 1) golpe = true;
            }
            if (fr >= inicio && fr - inicio < r.size()) toca(r.get(fr - inicio), c.f(1).facing, c.in[1]);
            boolean noAr = !j.grounded;
            c.passo();
            for (CombatEngine.HitEvent h : c.e.events()) {
                if (h.blocked) continue;
                // Tirar do ar é acertar quem ainda estava no ar (no chão já é outro castigo).
                if (h.attacker == 1 && resultado == 0) resultado = noAr ? 1 : 0;
                if (h.attacker == 0 && resultado == 0) resultado = -1;
                if (resultado == 0 && h.attacker == 1) return 0;
            }
            if (resultado != 0) return resultado;
            if (saiu && j.grounded && fr > inicio + r.size() + 20) break;
        }
        return resultado;
    }

    static void t04() {
        BateriaBalanco.titulo("T04 Antiaéreo");
        System.out.println("O Ryūga pula com jH apertado na descida (a 230 px do chão, ou a 180 px no \"jH tarde\"); no mergulho é o S4 do Kōga. Quem defende");
        System.out.println("aperta 2H, S2 ou S1 em cada quadro de 0 a 90; a célula é a janela (quadros em que tira do ar sem levar)");
        System.out.println("da melhor das três respostas, e qual foi. 0 = nenhuma resposta funciona em nenhum quadro.");
        System.out.println();
        StringBuilder cab = new StringBuilder("| defensor |"), sep = new StringBuilder("|---|");
        for (String p : PULOS) { cab.append(' ').append(p).append(" |"); sep.append("---|"); }
        System.out.println(cab);
        System.out.println(sep);
        List<String> linhas = Arrays.stream(ELENCO).parallel().map(def -> {
            StringBuilder l = new StringBuilder("| " + nome(def) + " |");
            for (String p : PULOS) {
                int melhor = 0;
                String qual = "";
                for (String resp : new String[] {"2H", "S2", "S1"}) {
                    int janela = 0;
                    for (int t = 0; t <= 90; t++) if (antiAereo(def, p, resp, t) == 1) janela++;
                    if (janela > melhor) { melhor = janela; qual = resp; }
                }
                l.append(melhor == 0 ? " 0 |" : " " + melhor + " (" + qual + ") |");
                synchronized (AA) { AA.merge(def, (double)melhor, Double::sum); }
            }
            return l.toString();
        }).collect(Collectors.toList());
        for (String l : linhas) System.out.println(l);
    }

    static final Map<String, Double> AA = new HashMap<>();

    // ------------------------------------------------------------------ T01 aproximação (CPU)

    static final Map<String, double[]> APROXIMA = new HashMap<>();

    static void t01() {
        BateriaBalanco.titulo("T01 Aproximação");
        System.out.println("CPU nível 8 contra o Ryūga CPU nível 8 (o da bola), 1x1, começando a 80/180/350/600 px. Mede os quadros até");
        System.out.println("o primeiro contato (acerto ou defesa) do lutador, o dano levado antes disso e as barras gastas. 12 sementes por distância.");
        System.out.println();
        int[] dists = {80, 180, 350, 600};
        StringBuilder cab = new StringBuilder("| lutador |"), sep = new StringBuilder("|---|");
        for (int d : dists) { cab.append(" ").append(d).append(" px: quadros / dano levado |"); sep.append("---|"); }
        cab.append(" barra gasta (600 px) |");
        sep.append("---|");
        System.out.println(cab);
        System.out.println(sep);
        List<String> linhas = Arrays.stream(ELENCO).parallel().map(id -> {
            StringBuilder l = new StringBuilder("| " + nome(id) + " |");
            double barra = 0;
            double[] guarda = new double[dists.length];
            for (int di = 0; di < dists.length; di++) {
                double q = 0, dano = 0;
                int n = 0, nunca = 0;
                for (long s = 1; s <= 12; s++) {
                    BateriaBalanco.Luta lu = new BateriaBalanco.Luta(new String[] {id}, new String[] {"player_base"}, 8, 300_000 + s * 17 + di, new CombatConfig());
                    lu.f(0).x = 1300f - dists[di] / 2f;
                    lu.f(1).x = 1300f + dists[di] / 2f;
                    lu.f(0).state.superMeter = CombatConfig.METER_PER_BAR;
                    int barraAntes = lu.f(0).state.superMeter;
                    int gasto = 0, contato = -1, vida = lu.f(0).state.life;
                    // O controle volta no GO (quadro 60 da luta).
                    while (lu.quadro < 60 + 60 * 20) {
                        int antes = lu.f(0).state.superMeter;
                        lu.passo();
                        int depois = lu.f(0).state.superMeter;
                        if (depois < antes) gasto += antes - depois;
                        boolean ok = false;
                        for (CombatEngine.HitEvent h : lu.e.events()) if (h.attacker == 0 && !h.projectile) ok = true;
                        if (ok) { contato = lu.quadro - 60; break; }
                    }
                    if (contato < 0) { nunca++; contato = 60 * 20; }
                    q += contato;
                    dano += vida - lu.f(0).state.life;
                    if (di == dists.length - 1) barra += gasto / (double)CombatConfig.METER_PER_BAR;
                    n++;
                }
                guarda[di] = q / n;
                l.append(String.format(Locale.ROOT, " %.0f / %.0f%s |", q / n, dano / n, nunca > 0 ? " (" + nunca + " nunca)" : ""));
            }
            synchronized (APROXIMA) { APROXIMA.put(id, guarda); }
            l.append(String.format(Locale.ROOT, " %.2f |", barra / 12));
            return l.toString();
        }).collect(Collectors.toList());
        for (String l : linhas) System.out.println(l);
    }

    // ------------------------------------------------------------------ T06 canto × centro

    static void t06() {
        BateriaBalanco.titulo("T06 Canto × centro");
        System.out.println("A CPU nível 8 ataca o Ryūga, que defende perfeito e, depois de cada sequência defendida, tenta a saída.");
        System.out.println("No centro (1200 × 1400) e no canto (o Ryūga encostado na borda esquerda). 12 lutas de 20 s por caso.");
        System.out.println("Escapou = 45 quadros sem levar nada; preso = fração do tempo a menos de 250 px em defesa ou apanhando.");
        System.out.println();
        System.out.println("| atacante | onde | dano levado / 20 s (só defende) | quebras de guarda | preso | backdash escapa | pulo p/ trás escapa | pushblock: distância ganha |");
        System.out.println("|---|---|---|---|---|---|---|---|");
        List<String> linhas = Arrays.stream(ELENCO).parallel().flatMap(atk -> {
            List<String> r = new ArrayList<>();
            for (boolean canto : new boolean[] {false, true}) {
                double[] so = canto(atk, canto, 0), bd = canto(atk, canto, 1), pj = canto(atk, canto, 2), pb = canto(atk, canto, 3);
                r.add(String.format(Locale.ROOT, "| %s | %s | %.0f | %.2f | %s | %s | %s | %.0f px |", nome(atk), canto ? "canto" : "centro", so[0], so[1], pct(so[2]),
                    pct(bd[3]), pct(pj[3]), pb[4]));
            }
            return r.stream();
        }).collect(Collectors.toList());
        for (String l : linhas) System.out.println(l);
    }

    /** saida: 0 só defende, 1 backdash, 2 pulo para trás, 3 pushblock. Devolve {dano, quebras, preso, escapou, distância do pushblock}. */
    static double[] canto(String atk, boolean canto, int saida) {
        double dano = 0, quebras = 0, preso = 0, quadros = 0, tent = 0, esc = 0, pbDist = 0, pbN = 0;
        for (long s = 1; s <= 12; s++) {
            BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {atk}, new String[] {"player_base"}, 8, 400_000 + s, new CombatConfig());
            l.f(0).state.life = l.f(1).state.life = 1_000_000;
            if (canto) {
                l.f(1).x = Arena.LEFT_BOUND + 10f;
                l.f(0).x = Arena.LEFT_BOUND + 260f;
            } else {
                l.f(1).x = 1200f;
                l.f(0).x = 1400f;
            }
            final int[] st = {0, -1, 0, 0};   // vinha defendendo, quadro da tentativa, distância antes do pushblock, pushblock feito
            final double[] acc = {0, 0, 0, 0};
            l.mao[1] = (lu, lado, in) -> {
                in.clear();
                CombatFighter me = lu.f(1), o = lu.f(0);
                me.state.superMeter = 2 * CombatConfig.METER_PER_BAR;
                BateriaBalanco.defender(lu, 1, in);
                if (me.status == CombatFighter.Status.BLOCKSTUN) {
                    if (st[0] == 0) st[3] = 0;
                    if (saida == 3 && st[3] == 0 && st[1] < 0 && !me.frozen()) {
                        in.pushblock = true;
                        st[2] = Math.round(Math.abs(o.x - me.x));
                        st[1] = lu.quadro;
                        st[3] = 1;
                    }
                    st[0] = 1;
                    return;
                }
                if (saida == 0 || saida == 3 || st[0] == 0 || st[1] >= 0 || !me.canAct()) { if (me.canAct()) st[0] = 0; return; }
                st[0] = 0;
                if (Math.abs(o.x - me.x) > 300) return;
                st[1] = lu.quadro;
                if (saida == 1) in.backdash = true;
                else in.direction = o.x >= me.x ? 6 : 8;
            };
            int vida = l.f(1).state.life;
            while (l.quadro < 100 + 60 * 20) {
                l.passo();
                if (l.quadro < 100) continue;
                quadros++;
                CombatFighter d = l.f(1);
                boolean perto = Math.abs(l.f(0).x - d.x) < 250f;
                if (perto && (d.status == CombatFighter.Status.BLOCKSTUN || d.inHitstun())) preso++;
                if (l.e.cues().contains("GUARD_BREAK")) quebras++;
                if (st[1] >= 0) {
                    boolean levou = false;
                    for (CombatEngine.HitEvent h : l.e.events()) if (h.attacker == 0 && !h.blocked) levou = true;
                    if (saida == 3 && l.quadro - st[1] == 20) {
                        pbDist += Math.abs(l.f(0).x - d.x) - st[2];
                        pbN++;
                    }
                    if (levou || l.quadro - st[1] >= 45) {
                        tent++;
                        if (!levou) esc++;
                        st[1] = -1;
                    }
                }
            }
            dano += vida - l.f(1).state.life;
        }
        return new double[] {dano / 12, quebras / 12, preso / Math.max(1, quadros), esc / Math.max(1, tent), pbN == 0 ? 0 : pbDist / pbN};
    }

    // ------------------------------------------------------------------ T22 identidade da CPU

    static final class Perfil {
        final Map<String, Integer> golpes = new HashMap<>();
        int inicios, pulos, agarroes, quadros, defendendo, recebidos, defendidos, agachado, perto, longe, venenos, vidaPaga, teleportes, tiros;
        double distancia;
    }

    static final Map<String, Perfil> PERFIS = new LinkedHashMap<>();

    static Perfil perfilCpu(String id, int lutas, int nivel) {
        Perfil p = new Perfil();
        for (int i = 0; i < lutas; i++) {
            String rival = ELENCO[(Arrays.asList(ELENCO).indexOf(id) + 1 + i) % ELENCO.length];
            if (rival.equals(id)) rival = "player_base".equals(id) ? "p02" : "player_base";
            boolean troca = (i & 1) == 1;
            int lado = troca ? 1 : 0;
            BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {troca ? rival : id}, new String[] {troca ? id : rival}, nivel, 500_000 + i * 31L + id.hashCode(), new CombatConfig());
            AttackDefinition antes = null;
            int antesQ = -1;
            boolean noChao = true;
            int vidaAntes = l.f(lado).state.life;
            while (!l.acabou() && l.quadro < 60 * 60 * 3) {
                l.passo();
                CombatFighter me = l.f(lado), o = l.f(1 - lado);
                if (l.flow.phase() != BattleFlow.Phase.FIGHT) { antes = me.attack; antesQ = me.attackFrame; continue; }
                p.quadros++;
                float d = Math.abs(me.x - o.x);
                p.distancia += d;
                if (d < 200) p.perto++;
                if (d > 450) p.longe++;
                if (me.status == CombatFighter.Status.BLOCKSTUN) p.defendendo++;
                if (me.crouching) p.agachado++;
                if (comecou(me, antes, antesQ)) {
                    p.inicios++;
                    p.golpes.merge(me.attack.id, 1, Integer::sum);
                    if (me.attack.mechanics.lifeCost > 0) p.vidaPaga += me.attack.mechanics.lifeCost;
                    if (me.attack.mechanics.teleportFrame >= 0 && me.attack.mechanics.teleportFrame > 0) p.teleportes++;
                }
                if (me.status == CombatFighter.Status.THROW && (antes == null || me.throwPhase == CombatFighter.THROW_STARTUP && me.attackFrame <= 1)) { }
                antes = me.attack;
                antesQ = me.attackFrame;
                if (noChao && !me.grounded && (me.status == CombatFighter.Status.NEUTRAL || me.status == CombatFighter.Status.ATTACK)) p.pulos++;
                noChao = me.grounded;
                for (CombatEngine.HitEvent h : l.e.events()) {
                    if (h.defender == lado) {
                        if (h.blocked) p.defendidos++; else p.recebidos++;
                    }
                    if (h.attacker == lado && "THROW".equals(h.moveId)) p.agarroes++;
                    if (h.attacker == lado && h.projectile && !h.blocked) p.tiros++;
                }
                if (l.e.cues().contains("POISON") && o.state.poisoned()) p.venenos++;
            }
        }
        return p;
    }

    static void t22() {
        BateriaBalanco.titulo("T22 Identidade da CPU");
        System.out.println("CPU nível 8, 1x1, 24 lutas por lutador contra o elenco em rodízio (metade de cada lado). Golpes iniciados por");
        System.out.println("minuto de luta, fração de especiais (S1–S4) entre eles, pulos e agarrões por minuto, distância média, tempo");
        System.out.println("colado (< 200 px) e longe (> 450 px), defesa (fração dos golpes recebidos que ela defendeu).");
        System.out.println();
        System.out.println("| lutador | golpes/min | S1 / S2 / S3 / S4 por min | Super/min | agarrões que entraram/min | pulos/min | distância média | colado / longe | defendeu | 3 golpes mais usados | mecânica própria |");
        System.out.println("|---|---|---|---|---|---|---|---|---|---|---|");
        List<String> linhas = Arrays.stream(ELENCO).parallel().map(id -> {
            Perfil p = perfilCpu(id, 24, 8);
            synchronized (PERFIS) { PERFIS.put(id, p); }
            double min = p.quadros / 3600.0;
            int s1 = p.golpes.getOrDefault("S", 0), s2 = p.golpes.getOrDefault("S2", 0), s3 = p.golpes.getOrDefault("S3", 0), s4 = p.golpes.getOrDefault("S4", 0);
            String top = p.golpes.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(3)
                .map(e -> e.getKey() + " " + pct(e.getValue() / (double)p.inicios)).collect(Collectors.joining(", "));
            String mec;
            switch (id) {
                case "p09": mec = String.format(Locale.ROOT, "agarrões %.1f/min (S2 cotovelada %.1f/min)", p.agarroes / min, s2 / min); break;
                case "p11": mec = String.format(Locale.ROOT, "envenenou %.1f vezes/min", p.venenos / min); break;
                case "p05": mec = String.format(Locale.ROOT, "teleporte %.1f/min (S3 %.1f/min)", p.teleportes / min, s3 / min); break;
                case "p06": mec = String.format(Locale.ROOT, "puxão (S3) %.1f/min, sombra S1 %.1f/min", s3 / min, s1 / min); break;
                case "p12": mec = String.format(Locale.ROOT, "vida paga %.0f/min", p.vidaPaga / min); break;
                default: mec = p.teleportes > 0 ? String.format(Locale.ROOT, "teleporte %.1f/min", p.teleportes / min) : ""; break;
            }
            return String.format(Locale.ROOT, "| %s | %.0f | %.1f / %.1f / %.1f / %.1f | %.1f | %.1f | %.1f | %.0f | %s / %s | %s | %s | %s |", nome(id),
                p.inicios / min, s1 / min, s2 / min, s3 / min, s4 / min, p.golpes.getOrDefault("SUPER", 0) / min, p.agarroes / min, p.pulos / min,
                p.distancia / Math.max(1, p.quadros), pct(p.perto / (double)p.quadros), pct(p.longe / (double)p.quadros),
                pct(p.defendidos / (double)Math.max(1, p.defendidos + p.recebidos)), top, mec);
        }).collect(Collectors.toList());
        for (String l : linhas) System.out.println(l);
    }

    // ------------------------------------------------------------------ T23 curva da CPU

    static void t23() {
        BateriaBalanco.titulo("T23 Curva da CPU por nível");
        System.out.println("Nível N contra o nível 5 (1x1, lutadores sorteados, os mesmos dos dois lados), 80 lutas por nível,");
        System.out.println("metade de cada lado. Defesa = fração dos golpes recebidos que defendeu; combo médio = acertos por combo.");
        System.out.println();
        System.out.println("| nível | vence o nível 5 | defendeu | golpes/min | especiais (S1–S4) | combo médio | maior combo | Breakers/luta | pulos/min |");
        System.out.println("|---|---|---|---|---|---|---|---|---|");
        List<String> linhas = IntStream.rangeClosed(1, 8).parallel().mapToObj(nivel -> {
            int vence = 0, jogos = 80, inicios = 0, especiais = 0, pulos = 0, def = 0, rec = 0, quadros = 0, breakers = 0, combos = 0, acertosCombo = 0, maior = 0;
            for (int i = 0; i < jogos; i++) {
                Random r = new Random(600_000 + i);
                String c = ELENCO[r.nextInt(ELENCO.length)];
                boolean troca = (i & 1) == 1;
                int lado = troca ? 1 : 0;
                BateriaBalanco.Luta l = new BateriaBalanco.Luta(new String[] {c}, new String[] {c}, 5, 600_000 + i, new CombatConfig());
                l.ai[lado].setLevel(nivel);
                AttackDefinition antes = null;
                int antesQ = -1;
                boolean noChao = true;
                ComboSession visto = null;
                while (!l.acabou() && l.quadro < 60 * 60 * 3) {
                    l.passo();
                    CombatFighter me = l.f(lado);
                    if (l.flow.phase() != BattleFlow.Phase.FIGHT) continue;
                    quadros++;
                    if (comecou(me, antes, antesQ)) {
                        inicios++;
                        if (me.attack.id.startsWith("S") && !me.attack.id.equals("SUPER")) especiais++;
                    }
                    antes = me.attack;
                    antesQ = me.attackFrame;
                    if (noChao && !me.grounded && (me.status == CombatFighter.Status.NEUTRAL || me.status == CombatFighter.Status.ATTACK)) pulos++;
                    noChao = me.grounded;
                    for (CombatEngine.HitEvent h : l.e.events()) if (h.defender == lado) { if (h.blocked) def++; else rec++; }
                    ComboSession s = l.e.lastSession(1 - lado);
                    if (s != null && s != visto && s.attacker == lado) {
                        visto = s;
                        combos++;
                        acertosCombo += s.hitCount;
                        maior = Math.max(maior, s.hitCount);
                    }
                }
                breakers += l.est[lado].breakers;
                if (l.flow.winner() == lado) vence++;
            }
            double min = quadros / 3600.0;
            return String.format(Locale.ROOT, "| %d | %s | %s | %.0f | %s | %.1f | %d | %.2f | %.1f |", nivel, pct(vence / (double)jogos),
                pct(def / (double)Math.max(1, def + rec)), inicios / min, pct(especiais / (double)Math.max(1, inicios)),
                acertosCombo / (double)Math.max(1, combos), maior, breakers / (double)jogos, pulos / min);
        }).collect(Collectors.toList());
        for (String l : linhas) System.out.println(l);
    }

    // ------------------------------------------------------------------ C01 ficha

    static void c01() {
        BateriaBalanco.titulo("C01 Fichas de identidade (notas 1–5 por quintil do elenco; números brutos ao lado)");
        // Alcance: média do alcance efetivo em pé de M, H, 2M.
        Map<String, Double> alcance = new HashMap<>(), mob = new HashMap<>(), pressao = new HashMap<>(), defesa = new HashMap<>(),
            explosivo = new HashMap<>(), espaco = new HashMap<>(), sinergia = new HashMap<>();
        Map<String, String> bruto = new HashMap<>();
        CombatConfig cfg = new CombatConfig();
        List<String[]> combos = Arrays.stream(ELENCO).parallel().map(id -> {
            BateriaBalanco.Candidata c = BateriaBalanco.buscar(id, 0, false, 40, 14);
            return new String[] {id, Integer.toString(c == null ? 0 : c.x.dano)};
        }).collect(Collectors.toList());
        for (String[] c : combos) explosivo.put(c[0], Double.parseDouble(c[1]));
        for (String id : ELENCO) {
            int[][] a = alcances(id);
            alcance.put(id, (a[1][0] + a[2][0] + a[4][0]) / 3.0);
            float[] m = MOBILIDADE.get(id);
            mob.put(id, m[0] / 300.0 + m[1] / 300.0 + m[2] / 300.0 + (m[6] - 1) * 0.5 + m[7] * 0.3);
            double[][] pu = PUNICAO.get(id);
            double adv = 0;
            int n = 0;
            for (int i = 0; i < 6; i++) if (pu != null && !Double.isNaN(pu[i][0])) { adv += pu[i][0]; n++; }
            Perfil p = PERFIS.get(id);
            double colado = p == null ? 0 : p.perto / (double)p.quadros;
            pressao.put(id, (n == 0 ? 0 : adv / n) + 10 * colado);
            CharacterDefinition c = ch(id);
            int jab = c.attack("L") == null ? 9 : c.attack("L").startupFrames;
            defesa.put(id, c.fighter.maxLife / 1000.0 + AA.getOrDefault(id, 0.0) / 10.0 - jab * 0.3);
            CharacterDefinition.Projectile pr = c.fighter.energy;
            double tiro = pr != null && pr.speed > 0 ? 2 + pr.speed / 1000.0 : 0;
            double longe = p == null ? 0 : p.longe / (double)p.quadros;
            espaco.put(id, tiro + a[4][0] / 100.0 + 5 * longe);
            sinergia.put(id, 0.0);
            bruto.put(id, String.format(Locale.ROOT, "alcance M/H/2M %d/%d/%d px; anda %.0f, dash %.0f, pulo %.0f px, %d air dash%s; vantagem média dos normais %+.1f, colado %s; vida %d, janela antiaérea total %.0f, L em %d q; maior combo sem barra %.0f; projétil %s, longe %s",
                a[1][0], a[2][0], a[4][0], m[0], m[1], m[2], (int)m[6], m[7] > 0 ? ", pulo na parede" : "", n == 0 ? 0 : adv / n, pct(colado),
                c.fighter.maxLife, AA.getOrDefault(id, 0.0), jab, explosivo.get(id), pr != null && pr.speed > 0 ? Math.round(pr.speed) + " px/s" : "não", pct(longe)));
        }
        System.out.println("| lutador | alcance | mobilidade | pressão | defesa | dano explosivo | controle de espaço | números brutos |");
        System.out.println("|---|---|---|---|---|---|---|---|");
        for (String id : ELENCO) {
            System.out.printf(Locale.ROOT, "| %s | %d | %d | %d | %d | %d | %d | %s |%n", nome(id), nota(alcance, id), nota(mob, id), nota(pressao, id),
                nota(defesa, id), nota(explosivo, id), nota(espaco, id), bruto.get(id));
        }
        System.out.println();
        System.out.println("Sinergia (assist e dupla) fica com a frente C (T17–T21).");
    }

    /** Nota 1–5 pela posição no elenco (quintis). */
    static int nota(Map<String, Double> m, String id) {
        double v = m.get(id);
        long abaixo = m.values().stream().filter(x -> x < v).count();
        return (int)Math.min(5, 1 + abaixo * 5 / m.size());
    }

    public static void main(String[] args) {
        String qual = args.length > 0 ? args[0] : "tudo";
        long t0 = System.nanoTime();
        System.out.println("# Auditoria, frente A");
        // O dano do Ultra fica num HashMap da BateriaBalanco: lido antes, as lutas em paralelo só leem.
        for (String id : ELENCO) BateriaBalanco.danoUltra(id);
        boolean tudo = qual.equals("tudo") || qual.equals("c01");
        if (qual.equals("t02") || tudo) t02();
        if (qual.equals("t03") || tudo) t03();
        if (qual.equals("t01") || tudo) t01();
        if (qual.equals("t04") || tudo) t04();
        if (qual.equals("t05") || tudo) { t05(); t05Erro(); }
        if (qual.equals("t06") || qual.equals("tudo")) t06();
        if (qual.equals("t22") || tudo) t22();
        if (qual.equals("t23") || qual.equals("tudo")) t23();
        if (tudo) c01();
        if (qual.equals("dbg04")) {
            for (String pulo : new String[] {"pulo à frente", "jH tarde (180 px)"}) {
                StringBuilder sb = new StringBuilder(pulo + ": ");
                for (int t = 0; t <= 90; t++) sb.append(antiAereo("player_base", pulo, "2H", t) == 1 ? '+' : antiAereo("player_base", pulo, "2H", t) == -1 ? 'x' : '.');
                System.out.println(sb);
            }
            // Quando o jH sai e quando ele cai, sem resposta.
            Cena c = new Cena("player_base", "player_base", 1000f, 1300f);
            for (int fr = 0; fr < 60; fr++) {
                if (fr < 2) c.in[0].direction = 8;
                if (!c.f(0).grounded && c.f(0).vy > 0 && Arena.GROUND_Y - c.f(0).y < 90f && !c.f(0).attacking()) c.in[0].heavy = true;
                c.passo();
                System.out.printf(Locale.ROOT, "%d y=%.0f vy=%.0f x=%.0f st=%s at=%s %n", fr, Arena.GROUND_Y - c.f(0).y, c.f(0).vy, c.f(0).x, c.f(0).status, c.f(0).attack == null ? "-" : c.f(0).attack.id);
            }
        }
        if (qual.equals("dbg01")) {
            BateriaBalanco.Luta lu = new BateriaBalanco.Luta(new String[] {"p03"}, new String[] {"player_base"}, 8, 300_000 + 17, new CombatConfig());
            lu.f(0).x = 1300f - 90f;
            lu.f(1).x = 1300f + 90f;
            for (int i = 0; i < 900; i++) {
                lu.passo();
                if (i % 60 == 0) System.out.printf(Locale.ROOT, "%d x %.0f %.0f st %s %s fase %s in0 dir %d L%s%n", i, lu.f(0).x, lu.f(1).x, lu.f(0).status, lu.f(1).status, lu.flow.phase(), lu.in[0].direction, lu.in[0].light);
            }
        }
        if (qual.equals("agacha")) {
            for (String id : new String[] {"p03", "player_base", "p07"}) for (String g : new String[] {"L", "M", "H"}) {
                StringBuilder sb = new StringBuilder(id + " " + g + ":");
                for (int gap = 60; gap <= 220; gap += 20) sb.append(' ').append(gap).append(acerta(id, g, gap, true) ? "+" : "-");
                Cena c = new Cena(id, "player_base", 1000f, 1100f);
                for (int fr = 0; fr < 3; fr++) { c.in[1].direction = 3; c.passo(); }
                sb.append("  alvo agachado: crouching=" + c.f(1).crouching + " altura " + c.f(1).body().height(true));
                System.out.println(sb);
            }
        }
        System.out.printf(Locale.ROOT, "%n(%.0f s)%n", (System.nanoTime() - t0) / 1e9);
    }
}
