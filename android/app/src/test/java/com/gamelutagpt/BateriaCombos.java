package com.gamelutagpt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Auditoria, frente B (docs/balanco): combos, recurso, pressão defendida, especiais, defesa
 * perfeita e quebra de guarda (T07–T11, T13, T14), medidos no motor de verdade com cenários
 * fixos. Não é teste da suíte: roda à mão.
 *
 * <pre>java -cp ... com.gamelutagpt.BateriaCombos [t07|t08|t09|t10|t11|t13|t14|tudo]</pre>
 */
public final class BateriaCombos {
    private BateriaCombos() {}

    static final String[] ELENCO = BateriaBalanco.ELENCO;
    /** O boneco (quem apanha e quem defende) é sempre o Ryūga. */
    static final String BONECO = "player_base";

    static CharacterDefinition ch(String id) { return GeneratedCharacters.get(id); }

    static String nome(String id) { return BateriaBalanco.nome(id); }

    static String pct(double v) { return BateriaBalanco.pct(v); }

    static String f1(double v) { return BateriaBalanco.f1(v); }

    static double vidaMedia() {
        double v = 0;
        for (String id : ELENCO) v += ch(id).fighter.maxLife;
        return v / ELENCO.length;
    }

    // ================================================================== executor de rotas

    /** Como a rota começa: distância, o rival pulando (antiaéreo), recurso e SHINKAI. */
    static final class Cenario {
        float gap = 140f;
        boolean rivalPula;
        int barras;
        Shinkai.Type shinkai;
        /** O combo precisa ter começado até este passo (1 = o primeiro golpe acerta). */
        int primeiroAcertoAte = 1;
        /** Vida real do boneco (para a vida vermelha e o K.O.); senão, vida "infinita". */
        boolean vidaReal;
        /** O boneco começa encostado na parede da direita (senão, no meio da arena). */
        boolean canto;

        Cenario copia() {
            Cenario c = new Cenario();
            c.gap = gap; c.rivalPula = rivalPula; c.barras = barras; c.shinkai = shinkai;
            c.primeiroAcertoAte = primeiroAcertoAte; c.vidaReal = vidaReal; c.canto = canto;
            return c;
        }
    }

    static final class Exec {
        boolean valida;
        int dano, acertos, parede, chao, duracao, recuperavel, barraGasta;
        boolean assist, superUsado, ultraUsado, dhc, ko;
        float distParede, gapFinal;
        String estadoFinal = "";
    }

    static final String[] BASE = {"L", "M", "H", "2L", "2M", "2H", "S", "SUPER", "A", "J", "SJ", "D", "jL", "jM", "jH", "j2H", "W6", "W14"};

    // ------------------------------------------------------------------ S1 L/H e S2–S4 na busca

    /**
     * As ações da busca ampliada: as de {@code base} mais o S1 fraco e forte (quando o S1 é projétil:
     * o botão muda velocidade e dano) e os especiais de comando S2–S4 que o lutador tem, com L e H
     * à parte só quando o botão muda o golpe (a distância da pedra, o tempo da armadilha).
     */
    static String[] ampliadas(String atk, String[] base) {
        CharacterDefinition c = ch(atk);
        List<String> l = new ArrayList<>(Arrays.asList(base));
        AttackDefinition s1 = c.attack("S");
        if (s1 != null && s1.kind == AttackDefinition.Kind.PROJECTILE) {
            l.add("S1L");
            l.add("S1H");
        }
        for (String k : new String[] {"S2", "S3", "S4"}) {
            CharacterDefinition.Special sp = c.specials.get(k);
            if (sp == null) continue;
            l.add(k);
            if (sp.projectile != null && (sp.projectile.strengthSpeed != null || sp.projectile.trapFuse != null)) {
                if (botoes(sp).contains("L")) l.add(k + "L");
                if (botoes(sp).contains("H")) l.add(k + "H");
            }
        }
        return l.toArray(new String[0]);
    }

    static String botoes(CharacterDefinition.Special sp) {
        return sp.buttons == null || sp.buttons.isEmpty() ? "LMH" : sp.buttons;
    }

    /**
     * Aperta o S1 fraco/forte ou um especial de comando já resolvido ("S2", "S3H"...), como o comando
     * completo faria (o mesmo registro no buffer, com o botão que escolhe a força). Não confere se o
     * comando cabia (a carga de [←] →, por exemplo): isso fica marcado na rota. False se não é um deles.
     */
    static boolean apertaEspecial(String acao, CombatFighter me, FighterInput in) {
        if (acao.equals("S1L")) { in.special = "L"; return true; }
        if (acao.equals("S1H")) { in.special = "H"; return true; }
        if (!comando(acao)) return false;
        String key = acao.substring(0, 2);
        CharacterDefinition.Special sp = me.character().specials.get(key);
        if (sp == null) return false;
        String b = botoes(sp);
        me.commandButton = acao.length() > 2 ? acao.substring(2) : b.contains("M") ? "M" : b.substring(0, 1);
        me.buffer.push(InputBuffer.Button.SPECIAL, key, false);
        return true;
    }

    static boolean comando(String acao) {
        return acao.length() >= 2 && acao.charAt(0) == 'S' && acao.charAt(1) >= '2' && acao.charAt(1) <= '4';
    }

    /** Se o especial ampliado pedido saiu agora; null se a ação não é um deles. */
    static Boolean saiuEspecial(String acao, boolean novo, CombatFighter me) {
        if (acao.equals("S1L") || acao.equals("S1H")) {
            return novo && me.attack.id.startsWith("S") && me.attack.kind != AttackDefinition.Kind.SUPER;
        }
        if (comando(acao)) return novo && me.attack.id.equals(acao.substring(0, 2));
        return null;
    }

    /** Executa a rota (cada passo apertado até sair, até 45 quadros) e deixa o combo acabar. */
    static Exec executar(String atk, String parceiro, List<String> rota, Cenario cn) {
        CombatConfig cfg = new CombatConfig();
        FighterState a = new FighterState(ch(atk), "P1"), p = new FighterState(ch(parceiro), "P1");
        FighterState d = new FighterState(ch(BONECO), "P2");
        float x0 = cn.canto ? Arena.RIGHT_BOUND - cn.gap : 900f;
        CombatEngine e = new CombatEngine(a, d, x0, x0 + cn.gap, cfg);
        e.setTeam(0, a, p);
        if (!cn.vidaReal) d.life = 10_000_000;
        FighterInput in0 = new FighterInput(), in1 = new FighterInput();
        CombatFighter me = e.fighter(0), dummy = e.fighter(1);
        Exec x = new Exec();
        if (cn.shinkai != null) {
            BateriaBalanco.forcarTipo(a, cn.shinkai);
            a.shinkai = CombatConfig.SHINKAI_MAX;
            for (int t = 0; t < 120 && !a.inShinkai(); t++) {
                in0.clear();
                in0.shinkai = true;
                in1.clear();
                e.step(in0, in1);
            }
            for (int t = 0; t < 120 && !me.canAct(); t++) {
                in0.clear();
                in1.clear();
                e.step(in0, in1);
            }
            if (!a.inShinkai()) return x;
        }
        a.superMeter = cn.barras * CombatConfig.METER_PER_BAR;
        ComboSession sess = null;
        boolean rivalPulou = false;
        for (int k = 0; k < rota.size(); k++) {
            String acao = rota.get(k);
            int espera = acao.equals("W6") ? 6 : acao.equals("W14") ? 14 : acao.equals("W3") ? 3 : 0;
            AttackDefinition antes = me.attack;
            int antesQuadro = me.attackFrame;
            boolean feito = false;
            for (int t = 0; t < 45 && !feito; t++) {
                in0.clear();
                in1.clear();
                if (dummy.status == CombatFighter.Status.AIR_HITSTUN && dummy.stunLeft == 0) in1.light = true;
                if (cn.rivalPula && !rivalPulou) {
                    in1.jump = true;
                    in1.direction = dummy.facing > 0 ? 8 : 6;
                    if (!dummy.grounded) rivalPulou = true;
                }
                int frente = me.facing > 0 ? 1 : 5;
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
                        case "ULTRA": in0.ultra = true; in0.direction = 3; break;
                        case "A": case "DHC": in0.assist = true; break;
                        case "J": in0.jump = true; in0.direction = me.facing > 0 ? 8 : 6; break;
                        case "SJ": in0.superJump = true; in0.direction = me.facing > 0 ? 8 : 6; break;
                        case "D": in0.dash = true; in0.direction = frente; break;
                        case "jL": in0.light = true; break;
                        case "jM": in0.medium = true; break;
                        case "jH": in0.heavy = true; break;
                        case "j2H": in0.direction = 3; in0.heavy = true; break;
                        default: apertaEspecial(acao, me, in0); break;
                    }
                }
                boolean noChao = me.grounded;
                boolean assistAntes = e.team(0).assistOut();
                int titular = e.team(0).point;
                e.step(in0, in1);
                if (e.ultraConnected() >= 0) BateriaBalanco.ultraSemPagina(e, e.ultraConnected());
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
                    case "DHC": feito = e.cues().contains("DHC") || e.team(0).point != titular; break;
                    case "ULTRA": feito = me.status == CombatFighter.Status.ULTRA || me.inUltraSequence() || e.ultraConnected() >= 0; break;
                    case "S": feito = novo && me.attack.id.startsWith("S") && me.attack.kind != AttackDefinition.Kind.SUPER; break;
                    case "SUPER": feito = novo && me.attack.kind == AttackDefinition.Kind.SUPER; break;
                    default:
                        Boolean esp = saiuEspecial(acao, novo, me);
                        feito = esp != null ? esp : novo && me.attack.id.equals(acao);
                        break;
                }
                if (feito) {
                    antes = me.attack;
                    antesQuadro = me.attackFrame;
                }
            }
            if (!feito) return x;
            x.assist |= acao.equals("A");
            x.dhc |= acao.equals("DHC");
            if (sess == null && k >= cn.primeiroAcertoAte - 1) {
                // O golpe que abre o combo precisa acertar: espera o contato.
                for (int t = 0; t < 40 && sess == null; t++) {
                    in0.clear();
                    in1.clear();
                    e.step(in0, in1);
                    if (e.ultraConnected() >= 0) BateriaBalanco.ultraSemPagina(e, e.ultraConnected());
                    ComboSession s = e.session(1);
                    if (s != null && s.active) sess = s;
                }
                if (sess == null) return x;
            }
            if (sess != null && !sess.active) return x;
        }
        for (int t = 0; t < 600 && sess != null && sess.active; t++) {
            in0.clear();
            in1.clear();
            if (dummy.status == CombatFighter.Status.AIR_HITSTUN && dummy.stunLeft == 0) in1.light = true;
            e.step(in0, in1);
            if (e.ultraConnected() >= 0) BateriaBalanco.ultraSemPagina(e, e.ultraConnected());
        }
        if (sess == null) return x;
        x.valida = sess.hitCount >= 2;
        x.dano = sess.comboDamage;
        x.acertos = sess.hitCount;
        x.parede = sess.wallBounces;
        x.chao = sess.groundBounces;
        x.duracao = sess.comboDuration;
        x.superUsado = sess.usesOf("SUPER") > 0;
        x.ultraUsado = sess.usesOf("ULTRA") > 0;
        x.recuperavel = d.recoverableLife;
        x.ko = d.life <= 0;
        x.barraGasta = sess.usesOf("SUPER") * CombatConfig.SUPER_COST + sess.usesOf("ULTRA") * CombatConfig.ULTRA_COST;
        x.distParede = Math.min(dummy.x - Arena.LEFT_BOUND, Arena.RIGHT_BOUND - dummy.x);
        x.gapFinal = Math.abs(dummy.x - e.fighter(0).x);
        x.estadoFinal = dummy.status.name();
        return x;
    }

    static final class Cand {
        final List<String> rota;
        final Exec x;
        final double nota;

        Cand(List<String> rota, Exec x, double nota) { this.rota = rota; this.x = x; this.nota = nota; }

        String texto() {
            return String.join(" ", rota.stream().map(s -> s.equals("A") ? "assist" : s.equals("D") ? "dash" : s.equals("J") ? "pulo"
                : s.equals("SJ") ? "superpulo" : s.startsWith("W") ? "espera" : s).collect(Collectors.toList()));
        }
    }

    enum Meta { DANO, DURACAO }

    /** Busca em feixe com um começo fixo ({@code prefixo}) e as ações permitidas. */
    static Cand buscar(String atk, Cenario cn, List<String> prefixo, String[] acoes, Meta meta, int largura, int fundo) {
        String parceiro = BateriaBalanco.parceiro(atk);
        List<Cand> feixe = new ArrayList<>();
        feixe.add(new Cand(new ArrayList<>(prefixo), new Exec(), 0));
        Cand melhor = null;
        if (!prefixo.isEmpty()) {
            Exec x = executar(atk, parceiro, prefixo, cn);
            if (x.valida) melhor = new Cand(new ArrayList<>(prefixo), x, x.dano);
        }
        Set<String> vistos = new HashSet<>();
        for (int nivel = prefixo.size(); nivel < fundo; nivel++) {
            List<Cand> filhos = new ArrayList<>();
            for (Cand c : feixe) {
                for (String acao : acoes) {
                    List<String> r = new ArrayList<>(c.rota);
                    r.add(acao);
                    Exec x = executar(atk, parceiro, r, cn);
                    boolean cedo = r.size() < cn.primeiroAcertoAte;
                    if (!x.valida && !(cedo || r.size() <= 1)) continue;
                    int n = r.size();
                    String chave = x.dano + "/" + x.acertos + "/" + x.parede + x.chao + (x.assist ? "A" : "") + "/" + n + "/" +
                        String.join(",", r.subList(Math.max(0, n - 3), n));
                    if (!vistos.add(chave)) continue;
                    double nota = meta == Meta.DANO ? x.dano : x.duracao * 10.0 + x.dano * 0.001;
                    Cand f = new Cand(r, x, nota);
                    filhos.add(f);
                    if (x.valida && (melhor == null || nota > melhor.nota)) melhor = f;
                }
            }
            if (filhos.isEmpty()) break;
            filhos.sort((p, q) -> q.nota != p.nota ? Double.compare(q.nota, p.nota) : p.x.acertos != q.x.acertos ? q.x.acertos - p.x.acertos
                : String.join(",", p.rota).compareTo(String.join(",", q.rota)));
            feixe = filhos.subList(0, Math.min(largura, filhos.size()));
        }
        return melhor;
    }

    static String[] semSuper() {
        return Arrays.stream(BASE).filter(s -> !s.equals("SUPER")).toArray(String[]::new);
    }

    static String[] comExtras(String... extras) {
        List<String> l = new ArrayList<>(Arrays.asList(BASE));
        l.addAll(Arrays.asList(extras));
        return l.toArray(new String[0]);
    }

    static void titulo(String s) {
        System.out.println();
        System.out.println("## " + s);
        System.out.println();
    }

    // ================================================================== T07 diversidade de rotas

    static final String[][] STARTERS = {
        {"L", "140", "L"}, {"M", "140", "M"}, {"H", "140", "H"}, {"2M", "140", "2M"},
        {"pulo", "330", "J"}, {"assist", "260", "A"}, {"antiaéreo", "320", ""},
    };

    /** A assinatura de uma rota: os golpes depois do começo, sem esperas e movimentos. */
    static String assinatura(Cand c, int pular) {
        TreeSet<String> s = new TreeSet<>();
        for (int i = pular; i < c.rota.size(); i++) {
            String a = c.rota.get(i);
            if (a.startsWith("W") || a.equals("D") || a.equals("J") || a.equals("SJ")) continue;
            s.add(a);
        }
        return String.join("+", s);
    }

    /** O 2M (rasteira, KNOCKDOWN em todos): dano e se o cancelamento em H ainda acerta. */
    static int[] rasteira(String atk) {
        CombatEngine e = new CombatEngine(new FighterState(ch(atk), "P1"), new FighterState(ch(BONECO), "P2"), 900f, 1040f, new CombatConfig());
        FighterInput a = new FighterInput(), b = new FighterInput();
        int dano = 0, h = 0;
        for (int fr = 0; fr < 90; fr++) {
            a.clear();
            b.clear();
            // 2M no quadro 0; depois H em todo quadro (o cancelamento sai assim que o golpe deixar).
            if (fr == 0) { a.direction = 2; a.medium = true; } else if (fr < 40) a.heavy = true;
            e.step(a, b);
            for (CombatEngine.HitEvent ev : e.events()) {
                if (ev.attacker != 0 || ev.blocked) continue;
                if ("2M".equals(ev.moveId)) dano += ev.damage; else if ("H".equals(ev.moveId)) h++;
            }
        }
        return new int[] {dano, h};
    }

    static void t07() {
        titulo("T07 Diversidade de rotas (0 barra, sem Super)");
        System.out.println("Melhor rota (busca em feixe, largura 40, até 14 passos, sem Super) por começo. O boneco cai de pé no ar assim que pode.");
        System.out.println("Começos: L, M, H, 2M a 140 px; pulo a 330 px; assist a 260 px; antiaéreo com o rival pulando de 320 px.");
        System.out.println();
        StringBuilder cab = new StringBuilder("| lutador |"), sep = new StringBuilder("|---|");
        for (String[] s : STARTERS) { cab.append(' ').append(s[0]).append(" |"); sep.append("---|"); }
        cab.append(" rotas distintas |");
        sep.append("---|");
        System.out.println(cab);
        System.out.println(sep);
        List<String[]> casos = new ArrayList<>();
        for (String atk : ELENCO) for (String[] s : STARTERS) casos.add(new String[] {atk, s[0], s[1], s[2]});
        Map<String, Cand> res = casos.parallelStream().collect(Collectors.toConcurrentMap(c -> c[0] + "|" + c[1], c -> {
            Cenario cn = new Cenario();
            cn.gap = Float.parseFloat(c[2]);
            List<String> pre = new ArrayList<>();
            if (c[1].equals("antiaéreo")) {
                cn.rivalPula = true;
                cn.primeiroAcertoAte = 3;
            } else if (c[1].equals("pulo")) {
                pre.add("J");
                cn.primeiroAcertoAte = 3;
            } else if (c[1].equals("assist")) {
                pre.add("A");
                cn.primeiroAcertoAte = 3;
            } else {
                pre.add(c[3]);
            }
            Cand r = buscar(c[0], cn, pre, semSuper(), Meta.DANO, 40, 14);
            return r == null ? new Cand(pre, new Exec(), -1) : r;
        }));
        StringBuilder detalhes = new StringBuilder();
        int somaDistintas = 0;
        for (String atk : ELENCO) {
            StringBuilder linha = new StringBuilder("| " + nome(atk) + " |");
            Set<String> assin = new java.util.LinkedHashSet<>();
            int validas = 0;
            for (String[] s : STARTERS) {
                Cand c = res.get(atk + "|" + s[0]);
                if (s[0].equals("2M") && (c.nota < 0 || !c.x.valida)) {
                    int[] r = rasteira(atk);
                    linha.append(" derruba (").append(r[0]).append(r[1] > 0 ? ", →H emenda" : ", nada emenda").append(") |");
                    continue;
                }
                if (c.nota < 0 || !c.x.valida) { linha.append(" – |"); continue; }
                validas++;
                int pular = s[0].equals("antiaéreo") || s[0].equals("pulo") || s[0].equals("assist") ? 0 : 1;
                assin.add(assinatura(c, pular));
                linha.append(' ').append(c.x.dano).append(" (").append(c.x.acertos).append(") |");
                detalhes.append("- ").append(nome(atk)).append(", ").append(s[0]).append(": ").append(c.texto())
                    .append(" → ").append(c.x.dano).append('\n');
            }
            somaDistintas += assin.size();
            linha.append(' ').append(assin.size()).append(" de ").append(validas).append(" |");
            System.out.println(linha);
        }
        System.out.printf(Locale.ROOT, "%nMédia de rotas distintas por lutador: %.1f de %d começos.%n", somaDistintas / (double)ELENCO.length, STARTERS.length);
        System.out.println();
        System.out.println("Rotas:");
        System.out.println();
        System.out.print(detalhes);
    }

    // ================================================================== T08 dano por recurso

    static void t08() {
        titulo("T08 Dano por recurso (0, 1 e 3 barras; Ultra e DHC entram na busca)");
        System.out.println("Começo livre (L/M/H/2L/2M a 140 px), largura 40, até 16 passos. Vida real do boneco (14000) para medir a vida vermelha;");
        System.out.println("o Ultra entra com o dano cheio do ultra.json (sem a nota do toque). Barra gasta conta a que o combo gerou.");
        System.out.println();
        System.out.println("| lutador | 0 barra | 1 barra | 3 barras | rota com 3 barras | vida vermelha (3 b.) | barra gasta (3 b.) | distância da parede no fim | Ultra / DHC usados |");
        System.out.println("|---|---|---|---|---|---|---|---|---|");
        List<String[]> casos = new ArrayList<>();
        for (String atk : ELENCO) for (int b : new int[] {0, 1, 3}) casos.add(new String[] {atk, Integer.toString(b)});
        // Por barra: a busca com tudo, a busca sem Ultra/DHC e, para cada rota achada, a mesma rota fechando
        // com o Ultra em link. Fica a maior; uma barra a mais nunca rende menos que a rota de menos barra.
        Map<String, Cand> res = new java.util.concurrent.ConcurrentHashMap<>();
        for (int b : new int[] {0, 1, 3}) {
            final int barras = b;
            Arrays.stream(ELENCO).parallel().forEach(atk -> {
                Cenario cn = new Cenario();
                cn.barras = barras;
                cn.vidaReal = true;
                String p = BateriaBalanco.parceiro(atk);
                List<Cand> opcoes = new ArrayList<>();
                String[] acoes = barras >= 3 ? comExtras("ULTRA", "DHC") : barras >= 1 ? comExtras("DHC") : BASE;
                opcoes.add(buscar(atk, cn, new ArrayList<>(), acoes, Meta.DANO, 40, 16));
                if (barras > 0) opcoes.add(buscar(atk, cn, new ArrayList<>(), BASE, Meta.DANO, 40, 16));
                for (int menos : new int[] {0, 1}) {
                    Cand c = menos < barras ? res.get(atk + "|" + menos) : null;
                    if (c != null && c.nota >= 0) {
                        Exec x = executar(atk, p, c.rota, cn);
                        if (x.valida) opcoes.add(new Cand(c.rota, x, x.dano));
                    }
                }
                if (barras >= 3) {
                    for (Cand c : new ArrayList<>(opcoes)) {
                        if (c == null || c.x.ultraUsado) continue;
                        for (String[] fim : new String[][] {{"ULTRA"}, {"W3", "ULTRA"}, {"W6", "ULTRA"}}) {
                            List<String> r = new ArrayList<>(c.rota);
                            r.addAll(Arrays.asList(fim));
                            Exec x = executar(atk, p, r, cn);
                            if (x.valida && x.ultraUsado) opcoes.add(new Cand(r, x, x.dano));
                        }
                    }
                }
                Cand melhor = null;
                for (Cand c : opcoes) if (c != null && c.x.valida && (melhor == null || c.x.dano > melhor.x.dano)) melhor = c;
                res.put(atk + "|" + barras, melhor == null ? new Cand(new ArrayList<>(), new Exec(), -1) : melhor);
            });
        }
        double vm = 14000;
        for (String atk : ELENCO) {
            Cand c0 = res.get(atk + "|0"), c1 = res.get(atk + "|1"), c3 = res.get(atk + "|3");
            System.out.printf(Locale.ROOT, "| %s | %s | %s | %s | %s | %s | %s | %s | %s |%n", nome(atk), fmt(c0, vm), fmt(c1, vm), fmt(c3, vm),
                c3.nota < 0 ? "–" : c3.texto(), c3.nota < 0 ? "–" : Integer.toString(c3.x.recuperavel),
                c3.nota < 0 ? "–" : f1(c3.x.barraGasta / 1000.0), c3.nota < 0 ? "–" : Math.round(c3.x.distParede) + " px",
                c3.nota < 0 ? "–" : (c3.x.ultraUsado ? "Ultra" : "") + (c3.x.dhc ? " DHC" : "") + (c3.x.ultraUsado || c3.x.dhc ? "" : "nenhum"));
        }
        // Super isolado × Super + DHC × Ultra a partir do mesmo começo (L M H).
        System.out.println();
        System.out.println("Mesmo começo (L M H) fechando de três jeitos (3 barras no início):");
        System.out.println();
        System.out.println("| lutador | L M H SUPER | L M H SUPER DHC | L M H + Ultra (link) | melhor fecho |");
        System.out.println("|---|---|---|---|---|");
        List<String> linhas = Arrays.stream(ELENCO).parallel().map(atk -> {
            Cenario cn = new Cenario();
            cn.barras = 3;
            cn.vidaReal = true;
            String p = BateriaBalanco.parceiro(atk);
            Exec s = executar(atk, p, Arrays.asList("L", "M", "H", "SUPER"), cn);
            Exec d = executar(atk, p, Arrays.asList("L", "M", "H", "SUPER", "DHC"), cn);
            Exec u = null;
            for (String w : new String[] {"", "W3", "W6"}) {
                List<String> r = new ArrayList<>(Arrays.asList("L", "M", "H"));
                if (!w.isEmpty()) r.add(w);
                r.add("ULTRA");
                Exec t = executar(atk, p, r, cn);
                if (t.valida && t.ultraUsado && (u == null || t.dano > u.dano)) u = t;
            }
            String us = u == null ? "não emenda" : Integer.toString(u.dano);
            int bs = s.valida ? s.dano : -1, bd = d.valida && d.dhc ? d.dano : -1, bu = u == null ? -1 : u.dano;
            String melhor = bu >= bd && bu >= bs ? "Ultra" : bd >= bs ? "DHC" : "Super";
            return String.format(Locale.ROOT, "| %s | %s | %s | %s | %s |", nome(atk), s.valida ? Integer.toString(s.dano) : "não emenda",
                d.valida && d.dhc ? d.dano + " (+" + (d.dano - (s.valida ? s.dano : 0)) + ")" : "não emenda", us, melhor);
        }).collect(Collectors.toList());
        for (String l : linhas) System.out.println(l);
    }

    // ================================================================== G3: busca com S1 L/H e S2–S4

    /** A melhor das buscas (largura 40 e 80); a busca em feixe não é monótona na largura. */
    static Cand melhorBusca(String atk, Cenario cn, String[] acoes, Cand... outras) {
        Cand m = null;
        List<Cand> todas = new ArrayList<>(Arrays.asList(outras));
        for (int largura : new int[] {40, 80}) todas.add(buscar(atk, cn, new ArrayList<>(), acoes, Meta.DANO, largura, 16));
        for (Cand c : todas) if (c != null && c.x.valida && (m == null || c.x.dano > m.x.dano)) m = c;
        return m;
    }

    /** Os especiais ampliados (S1 fraco/forte, S2–S4) que a rota usa, com "carga" nos de segurar. */
    static String especiaisDaRota(String atk, Cand c) {
        if (c == null) return "";
        Set<String> s = new java.util.LinkedHashSet<>();
        for (String a : c.rota) {
            if (a.equals("S1L") || a.equals("S1H")) s.add(a);
            else if (comando(a)) {
                CharacterDefinition.Special sp = ch(atk).specials.get(a.substring(0, 2));
                s.add(a + (sp != null && sp.hold ? " (carga)" : ""));
            }
        }
        return String.join(", ", s);
    }

    /** O mesmo especial três vezes ou mais na rota (candidato a loop). */
    static String repetido(Cand c) {
        if (c == null) return "";
        Map<String, Integer> n = new LinkedHashMap<>();
        for (String a : c.rota) if (a.startsWith("S") && !a.equals("SUPER") && !a.equals("SJ")) n.merge(a.length() > 2 && comando(a) ? a.substring(0, 2) : a.startsWith("S1") ? "S" : a, 1, Integer::sum);
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, Integer> e : n.entrySet()) if (e.getValue() >= 3) b.append(b.length() > 0 ? ", " : "").append(e.getKey()).append("×").append(e.getValue());
        return b.toString();
    }

    static void g3combos() {
        titulo("G3b Busca de combos com S1 fraco/forte e S2–S4");
        System.out.println("Busca em feixe (larguras 40 e 80, fica a melhor; até 16 passos), boneco com vida infinita que cai de pé no ar.");
        System.out.println("Começo a 140 px, no meio (x 900) ou com o boneco na parede da direita. \"antiga\" = as ações da frente B");
        System.out.println("(L/M/H, agachados, S1 médio, Super, assist, pulos, dash, golpes no ar, esperas); \"ampliada\" = as mesmas + S1 fraco/forte");
        System.out.println("+ S2–S4 (L/H à parte só quando o botão muda o golpe) e nunca fica abaixo da antiga. \"sem assist\" tira o assist.");
        System.out.println();
        String[][] cenarios = {{"meio", "0", "A"}, {"meio", "1", "A"}, {"canto", "0", "A"}, {"canto", "1", "A"},
            {"meio", "0", ""}, {"meio", "1", ""}, {"canto", "0", ""}, {"canto", "1", ""}};
        List<String[]> casos = new ArrayList<>();
        for (String atk : ELENCO) for (String[] c : cenarios) casos.add(new String[] {atk, c[0], c[1], c[2]});
        Map<String, Cand[]> res = casos.parallelStream().collect(Collectors.toConcurrentMap(c -> String.join("|", c), c -> {
            Cenario cn = new Cenario();
            cn.canto = c[1].equals("canto");
            cn.barras = Integer.parseInt(c[2]);
            String[] velhas = c[3].isEmpty() ? Arrays.stream(BASE).filter(a -> !a.equals("A")).toArray(String[]::new) : BASE;
            Cand antiga = melhorBusca(c[0], cn, velhas);
            String[] tudo = ampliadas(c[0], velhas);
            String[] soComando = Arrays.stream(tudo).filter(a -> !a.equals("S1L") && !a.equals("S1H")).toArray(String[]::new);
            Cand s24 = melhorBusca(c[0], cn, soComando, antiga);
            Cand ampliada = melhorBusca(c[0], cn, tudo, s24);
            return new Cand[] {antiga, s24, ampliada};
        }));
        double vidaBoneco = ch(BONECO).fighter.maxLife;
        System.out.println("| lutador | cenário | antiga | + S2–S4 | + S1 fraco/forte | Δ total | especiais novos na rota | rota ampliada |");
        System.out.println("|---|---|---|---|---|---|---|---|");
        StringBuilder resumo = new StringBuilder();
        StringBuilder alertas = new StringBuilder();
        for (String atk : ELENCO) {
            int melhorDelta = 0, usos = 0;
            Set<String> usados = new TreeSet<>();
            for (String[] cc : cenarios) {
                Cand[] r = res.get(atk + "|" + String.join("|", cc));
                Cand a = r[0], m = r[1], b = r[2];
                int da = a == null ? 0 : a.x.dano, dm = m == null ? 0 : m.x.dano, db = b == null ? 0 : b.x.dano;
                String esp = especiaisDaRota(atk, b);
                boolean nova = b != null && b != a && db > da;
                if (nova && !esp.isEmpty()) {
                    usos++;
                    for (String e : esp.split(", ")) usados.add(e);
                }
                melhorDelta = Math.max(melhorDelta, db - da);
                String cen = cc[0] + ", " + cc[1] + " b" + (cc[2].isEmpty() ? ", sem assist" : "");
                System.out.printf(Locale.ROOT, "| %s | %s | %s | %s | %s | %+d | %s | %s |%n", nome(atk), cen, da, dm, db, db - da,
                    nova ? esp : "–", nova ? b.texto() : "(igual à antiga)");
                if (b != null && (db >= vidaBoneco || b.x.duracao > 600 || !repetido(b).isEmpty())) {
                    alertas.append("- ").append(nome(atk)).append(", ").append(cen).append(": ").append(db).append(" (")
                        .append(pct(db / vidaBoneco)).append(" da vida do boneco), ").append(f1(b.x.duracao / 60.0)).append(" s")
                        .append(repetido(b).isEmpty() ? "" : ", repete " + repetido(b)).append(" — ").append(b.texto()).append('\n');
                }
            }
            resumo.append(String.format(Locale.ROOT, "| %s | %d de %d | %+d | %s |%n", nome(atk), usos, cenarios.length, melhorDelta,
                usados.isEmpty() ? "nenhum" : String.join(", ", usados)));
        }
        System.out.println();
        System.out.println("Resumo por lutador (cenários em que a rota ampliada é melhor E usa um especial novo):");
        System.out.println();
        System.out.println("| lutador | cenários com rota nova | maior ganho | especiais novos usados |");
        System.out.println("|---|---|---|---|");
        System.out.print(resumo);
        System.out.println();
        System.out.println("Alertas (≥ 100% da vida do boneco " + Math.round(vidaBoneco) + ", mais de 10 s ou o mesmo especial 3 vezes):");
        System.out.println();
        System.out.print(alertas.length() == 0 ? "- nenhum\n" : alertas.toString());
    }

    static String fmt(Cand c, double vm) {
        if (c == null || c.nota < 0 || !c.x.valida) return "–";
        return c.x.dano + " (" + pct(c.x.dano / vm) + (c.x.ko ? ", K.O." : "") + ")";
    }

    // ================================================================== T09 pressão defendida

    /** Uma sequência na defesa perfeita: buracos entre as defesas e a vantagem no fim. */
    static final class Sequencia {
        List<Integer> buracos = new ArrayList<>();
        int defendidos, vantagemFinal = Integer.MIN_VALUE, guardaGasta, saiuDoAlcance = -1;
        float distFinal;
    }

    /** Defesa: 0 nada, 1 segura, 2 pushblock no 1º bloqueio, 3 defesa perfeita (oráculo). */
    static Sequencia sequencia(String atk, String[] golpes, int modo) {
        CombatConfig cfg = new CombatConfig();
        FighterState a = new FighterState(ch(atk), "P1"), d = new FighterState(ch(BONECO), "P2");
        CombatEngine e = new CombatEngine(a, d, 900f, 960f, cfg);
        FighterInput in0 = new FighterInput(), in1 = new FighterInput();
        CombatFighter fa = e.fighter(0), fd = e.fighter(1);
        d.superMeter = 2 * CombatConfig.METER_PER_BAR;
        for (int i = 0; i < 10; i++) { in0.clear(); in1.clear(); e.step(in0, in1); }
        Sequencia sq = new Sequencia();
        int k = 0, livreDesde = -1, ultimoBloqueio = -1, livreA = -1, livreD = -1;
        boolean pushFeito = false;
        AttackDefinition antes = null;
        int antesQuadro = 0, inicioGolpe = -1, startup = 0;
        int guardaAntes = d.guard;
        for (int fr = 0; fr < 400; fr++) {
            in0.clear();
            in1.clear();
            // Atacante: aperta o próximo da lista até ele sair.
            if (k < golpes.length) aperta(in0, golpes[k], fa);
            // Defensor.
            boolean baixo = fa.attacking() && fa.attack != null && fa.attack.low || k < golpes.length && golpes[k].startsWith("2");
            int tras = baixo ? (fa.x >= fd.x ? 4 : 2) : (fa.x >= fd.x ? 5 : 1);
            if (modo == 3) {
                // Solta a defesa no começo de cada golpe e aperta 2 quadros antes do contato.
                boolean soltar = inicioGolpe >= 0 && fr >= inicioGolpe && fr < inicioGolpe + startup - 2 && fd.status != CombatFighter.Status.BLOCKSTUN;
                in1.direction = soltar ? 0 : tras;
            } else if (modo >= 1) {
                in1.direction = tras;
                if (modo == 2 && !pushFeito && fd.status == CombatFighter.Status.BLOCKSTUN && !fd.frozen()) {
                    in1.pushblock = true;
                    pushFeito = true;
                }
            }
            e.step(in0, in1);
            boolean novo = fa.attacking() && fa.attack != null && (fa.attack != antes || fa.attackFrame < antesQuadro);
            if (novo && k < golpes.length && (fa.attack.id.equals(golpes[k]) || golpes[k].equals("S") && fa.attack.id.startsWith("S"))) {
                k++;
                inicioGolpe = fr;
                startup = fa.attack.startupFrames;
            }
            if (fa.attacking()) { antes = fa.attack; antesQuadro = fa.attackFrame; }
            for (CombatEngine.HitEvent h : e.events()) {
                if (h.attacker != 0) continue;
                if (h.blocked) {
                    sq.defendidos++;
                    if (ultimoBloqueio >= 0) sq.buracos.add(livreDesde >= 0 ? fr - livreDesde : 0);
                    livreDesde = -1;
                    ultimoBloqueio = fr;
                }
            }
            if (fd.canAct() && livreDesde < 0 && ultimoBloqueio >= 0) livreDesde = fr;
            if (k >= golpes.length && ultimoBloqueio >= 0) {
                if (livreA < 0 && fa.canAct()) livreA = fr;
                if (livreD < 0 && fd.canAct()) livreD = fr;
                if (livreA >= 0 && livreD >= 0) break;
            }
            if (k < golpes.length && ultimoBloqueio >= 0 && fr - ultimoBloqueio > 50) { sq.saiuDoAlcance = k; break; }
        }
        if (livreA >= 0 && livreD >= 0) sq.vantagemFinal = livreD - livreA;
        sq.guardaGasta = guardaAntes - d.guard;
        sq.distFinal = Math.abs(fa.x - fd.x);
        return sq;
    }

    static void aperta(FighterInput in, String g, CombatFighter me) {
        switch (g) {
            case "L": in.light = true; break;
            case "M": in.medium = true; break;
            case "H": in.heavy = true; break;
            case "2L": in.direction = me.facing > 0 ? 2 : 4; in.light = true; break;
            case "2M": in.direction = me.facing > 0 ? 2 : 4; in.medium = true; break;
            case "2H": in.direction = me.facing > 0 ? 2 : 4; in.heavy = true; break;
            case "S": in.special = "M"; break;
            default: break;
        }
    }

    static void t09() {
        titulo("T09 Pressão defendida");
        String[] golpes = {"L", "M", "H", "2L", "2M", "2H", "S"};
        System.out.println("Vantagem na defesa de cada golpe (quadros; + o atacante age primeiro) e a distância depois; defensor Ryūga com a defesa certa.");
        System.out.println("Guarda gasta = blockstun × 10 (GUARD_MAX 1000; CombatConfig.guardDrainPerBlockstunFrame).");
        System.out.println();
        StringBuilder cab = new StringBuilder("| atacante |"), sep = new StringBuilder("|---|");
        for (String g : golpes) { cab.append(' ').append(g).append(" |"); sep.append("---|"); }
        cab.append(" positivos |");
        sep.append("---|");
        System.out.println(cab);
        System.out.println(sep);
        int positivosTotal = 0, maior = Integer.MIN_VALUE;
        String maiorQuem = "";
        for (String atk : ELENCO) {
            StringBuilder linha = new StringBuilder("| " + nome(atk) + " |");
            int pos = 0;
            for (String g : golpes) {
                int[] v = BateriaBalanco.vantagem(atk, g);
                AttackDefinition at = ch(atk).attack(g);
                if (v == null || at == null) { linha.append(" – |"); continue; }
                if (v[0] > 0) pos++;
                if (v[0] > maior) { maior = v[0]; maiorQuem = nome(atk) + " " + g; }
                linha.append(String.format(Locale.ROOT, " %+d / %dpx / g%d |", v[0], v[1], at.blockstunFrames * 10));
            }
            positivosTotal += pos;
            linha.append(' ').append(pos).append(" |");
            System.out.println(linha);
        }
        System.out.printf(Locale.ROOT, "%nGolpes positivos na defesa no elenco: %d; o mais positivo: %s (%+d).%n", positivosTotal, maiorQuem, maior);

        System.out.println();
        System.out.println("Sequências apertando o próximo golpe o mais cedo possível (buracos = quadros livres de quem defende entre duas defesas; 0 = sem buraco):");
        System.out.println();
        System.out.println("| atacante | sequência | defendidos | buracos | vantagem no fim | distância no fim | guarda gasta | com pushblock no 1º: defendidos / distância |");
        System.out.println("|---|---|---|---|---|---|---|---|");
        String[][] seqs = {{"L", "M", "H"}, {"2L", "2M", "2H"}, {"L", "L", "L"}, {"M", "H", "S"}};
        int semBuraco = 0, total = 0;
        for (String atk : ELENCO) {
            for (String[] s : seqs) {
                Sequencia n = sequencia(atk, s, 1), p = sequencia(atk, s, 2);
                for (int b : n.buracos) { total++; if (b <= 1) semBuraco++; }
                System.out.printf(Locale.ROOT, "| %s | %s | %d | %s | %s | %.0f px | %d | %d / %.0f px |%n", nome(atk), String.join(" ", s), n.defendidos,
                    n.buracos, n.vantagemFinal == Integer.MIN_VALUE ? "–" : String.format(Locale.ROOT, "%+d", n.vantagemFinal), n.distFinal, n.guardaGasta,
                    p.defendidos, p.distFinal);
            }
        }
        System.out.printf(Locale.ROOT, "%nEmendas sem buraco (≤ 1 quadro) entre defesas: %d de %d.%n", semBuraco, total);
    }

    // ================================================================== T10 combos extremos

    static void t10() {
        titulo("T10 Combos extremos (assist, quiques, launcher, wall chase, repetição; SHINKAI forçado)");
        System.out.println("Busca em feixe (largura 40, até 16 passos, 1 barra no início) pelo maior dano e, à parte, pela maior duração.");
        System.out.println("Com SHINKAI: o atacante liga o tipo forçado antes do primeiro golpe (todos ganham +25% de dano; SPEED repete o golpe pelo eco).");
        System.out.println();
        Shinkai.Type[] tipos = {null, Shinkai.Type.POWER, Shinkai.Type.SPEED, Shinkai.Type.ENERGY, Shinkai.Type.TECHNIQUE};
        System.out.println("| lutador | sem SHINKAI | POWER | SPEED | ENERGY | TECHNIQUE | maior duração (tipo) | rota mais longa |");
        System.out.println("|---|---|---|---|---|---|---|---|");
        List<String[]> casos = new ArrayList<>();
        for (String atk : ELENCO) for (int t = 0; t < tipos.length; t++) for (String m : new String[] {"D", "T"}) casos.add(new String[] {atk, Integer.toString(t), m});
        Map<String, Cand> res = casos.parallelStream().collect(Collectors.toConcurrentMap(c -> c[0] + "|" + c[1] + "|" + c[2], c -> {
            Cenario cn = new Cenario();
            cn.barras = 1;
            cn.shinkai = tipos[Integer.parseInt(c[1])];
            Cand r = buscar(c[0], cn, new ArrayList<>(), BASE, c[2].equals("D") ? Meta.DANO : Meta.DURACAO, 40, 16);
            return r == null ? new Cand(new ArrayList<>(), new Exec(), -1) : r;
        }));
        double vm = vidaMedia();
        int maxDano = 0, maxDur = 0;
        String maxDanoQuem = "", maxDurQuem = "";
        double[] somaTipo = new double[tipos.length];
        for (String atk : ELENCO) {
            StringBuilder linha = new StringBuilder("| " + nome(atk) + " |");
            Cand longa = null;
            String longaTipo = "";
            for (int t = 0; t < tipos.length; t++) {
                Cand d = res.get(atk + "|" + t + "|D");
                Cand du = res.get(atk + "|" + t + "|T");
                if (d.nota < 0) linha.append(" – |");
                else {
                    somaTipo[t] += d.x.dano;
                    linha.append(' ').append(d.x.dano).append(" (").append(pct(d.x.dano / vm)).append(d.x.parede + d.x.chao > 0 ? ", P" + d.x.parede + "/C" + d.x.chao : "")
                        .append(d.x.assist ? ", A" : "").append(") |");
                    if (d.x.dano > maxDano) { maxDano = d.x.dano; maxDanoQuem = nome(atk) + " " + (tipos[t] == null ? "sem SHINKAI" : tipos[t].label) + ": " + d.texto(); }
                }
                if (du.nota >= 0 && (longa == null || du.x.duracao > longa.x.duracao)) { longa = du; longaTipo = tipos[t] == null ? "sem" : tipos[t].label; }
            }
            if (longa != null && longa.x.duracao > maxDur) { maxDur = longa.x.duracao; maxDurQuem = nome(atk) + " (" + longaTipo + "): " + longa.texto(); }
            linha.append(longa == null ? " – | – |" : String.format(Locale.ROOT, " %.1f s, %d acertos (%s) | %s |", longa.x.duracao / 60.0, longa.x.acertos, longaTipo, longa.texto()));
            System.out.println(linha);
        }
        System.out.print("\nMédia do maior dano por tipo:");
        for (int t = 0; t < tipos.length; t++) System.out.printf(Locale.ROOT, " %s %.0f (%s);", tipos[t] == null ? "sem SHINKAI" : tipos[t].label, somaTipo[t] / ELENCO.length, pct(somaTipo[t] / ELENCO.length / vm));
        System.out.printf(Locale.ROOT, "%nMaior dano: %d (%s) — %s.%nMaior duração: %.1f s — %s.%nVida média do elenco: %.0f.%n", maxDano, pct(maxDano / vm), maxDanoQuem,
            maxDur / 60.0, maxDurQuem, vm);
    }

    // ================================================================== T11 especiais

    /** Faz o comando do especial (direções de tela, olhando para a direita) e devolve o golpe que saiu. */
    static String fazerEspecial(CombatEngine e, FighterInput in, FighterInput npc, String key, String botao) {
        CombatFighter p = e.fighter(0);
        CharacterDefinition c = p.character();
        int[] command;
        boolean hold, air = false;
        if (key.equals("S1")) {
            command = c.fighter.energyCommand;
            hold = c.fighter.energyHold;
        } else {
            CharacterDefinition.Special s = c.specials.get(key);
            if (s == null) return null;
            command = s.command();
            hold = s.hold;
            air = s.air;
        }
        if (command == null || command.length == 0) return null;
        if (air) {
            in.clear();
            in.direction = 7;
            passo(e, in, npc);
            in.direction = 0;
            for (int i = 0; i < 6; i++) passo(e, in, npc);
        }
        if (hold) {
            in.direction = command[0];
            for (int i = 0; i < e.config.chargeFrames + 2; i++) passo(e, in, npc);
            in.direction = command[1];
            aperta(in, botao, p);
            passo(e, in, npc);
        } else {
            int last = -1;
            for (int d : command) {
                if (d == last) {
                    in.direction = 0;
                    passo(e, in, npc);
                }
                in.direction = d;
                passo(e, in, npc);
                passo(e, in, npc);
                last = d;
            }
            aperta(in, botao, p);
            passo(e, in, npc);
        }
        in.direction = 0;
        for (int i = 0; i < 6 && !p.attacking(); i++) passo(e, in, npc);
        return p.attacking() ? p.attack.id : "";
    }

    static void passo(CombatEngine e, FighterInput in, FighterInput npc) {
        e.step(in, npc);
        in.clearPresses();
        npc.clearPresses();
        if (e.ultraConnected() >= 0) BateriaBalanco.ultraSemPagina(e, e.ultraConnected());
    }

    /** Resultado de um especial: 0 acerto, 1 defesa, 2 erro. */
    static final class Especial {
        String id = "";
        int dano, vantagem = Integer.MIN_VALUE, duracao;
        float dist;
        boolean acertou, derrubou, defendeu, multi;
        String punivel = "";
    }

    static final String[] PUNICOES = {"L", "2L", "M", "2M", "H", "S", "SUPER"};

    /** O comando do S1 no chão não solta o S1 (sai um normal): o S1 é no ar. */
    static boolean s1NoAr(String atk) {
        CombatEngine e = new CombatEngine(new FighterState(ch(atk), "P1"), new FighterState(ch(BONECO), "P2"), 700f, 1500f, new CombatConfig());
        FighterInput in = new FighterInput(), npc = new FighterInput();
        String id = fazerEspecial(e, in, npc, "S1", "M");
        return id != null && !id.isEmpty() && !id.startsWith("S");
    }

    static Especial especial(String atk, String key, int caso, String punicao) {
        CombatConfig cfg = new CombatConfig();
        FighterState a = new FighterState(ch(atk), "P1"), d = new FighterState(ch(BONECO), "P2");
        float gap = caso == 2 ? 1400f : 120f;
        CombatEngine e = new CombatEngine(a, d, 700f, 700f + gap, cfg);
        d.life = 1_000_000;
        d.superMeter = CombatConfig.METER_PER_BAR;
        FighterInput in = new FighterInput(), npc = new FighterInput();
        CombatFighter fa = e.fighter(0), fd = e.fighter(1);
        for (int i = 0; i < 6; i++) passo(e, in, npc);
        Especial r = new Especial();
        // O defensor: na defesa (caso 1) segura trás na altura do golpe.
        final boolean defende = caso == 1;
        // Defendendo: segura trás (agachado contra golpe baixo; em pé contra o resto e contra quem pula).
        java.util.function.Consumer<FighterInput> def = n -> {
            if (!defende) return;
            boolean alto = !fa.grounded || fa.attacking() && fa.attack != null && fa.attack.overhead;
            boolean baixo = fa.attacking() && fa.attack != null && fa.attack.low;
            n.direction = baixo && !alto ? 2 : 1;
        };
        def.accept(npc);
        boolean noAr = key.equals("S1") && s1NoAr(atk);
        if (noAr) {
            // O S1 deste lutador é no ar (a p14 mergulha): pula antes do comando.
            in.direction = 7;
            passo(e, in, npc);
            in.direction = 0;
            for (int i = 0; i < 6; i++) passo(e, in, npc);
        }
        String id = fazerEspecial(e, in, npc, key, "M");
        if (noAr && id != null) id += " (no ar)";
        if (id == null || id.isEmpty()) return null;
        r.id = id;
        int inicio = 0, contato = -1, livreA = -1, livreD = -1, hits = 0;
        boolean contatoBloq = false;
        for (int fr = 0; fr < 400; fr++) {
            npc.clear();
            def.accept(npc);
            // Livre, quem atacou defende (agachado): a punição só vale se entra antes disso.
            if (fa.canAct()) in.direction = 4;
            if (contato >= 0 && livreD < 0 && fd.canAct() && punicao != null) {
                // Pune na primeira chance.
                aperta(npc, punicao, fd);
                if (punicao.equals("SUPER")) npc.superAttack = true;
                livreD = fr;
            }
            passo(e, in, npc);
            for (CombatEngine.HitEvent h : e.events()) {
                if (h.attacker == 0) {
                    if (contato < 0) { contato = fr; contatoBloq = h.blocked; }
                    hits++;
                    r.dano += h.damage;
                    if (h.blocked) r.defendeu = true; else r.acertou = true;
                }
                if (h.attacker == 1 && !h.blocked && punicao != null) r.punivel = punicao;
            }
            if (fd.status == CombatFighter.Status.KNOCKDOWN) r.derrubou = true;
            if (livreA < 0 && fa.canAct()) { livreA = fr; r.duracao = fr + 1; }
            if (punicao == null && contato >= 0 && livreD < 0 && fd.canAct()) livreD = fr;
            if (livreA >= 0 && (livreD >= 0 || caso == 2) && (punicao == null || fr > livreD + 30)) break;
        }
        r.multi = hits > 1;
        if (livreA >= 0 && livreD >= 0) r.vantagem = livreD - livreA;
        r.dist = Math.abs(fa.x - fd.x);
        return r;
    }

    static void t11() {
        titulo("T11 Risco dos especiais (S1–S4, botão M)");
        System.out.println("Cada especial contra o Ryūga a 120 px: no acerto (sem defesa), na defesa (defesa certa) e no erro (rival a 1400 px).");
        System.out.println("Vantagem: quadros que o atacante fica livre antes do rival (+ bom para quem atacou). Punível: o golpe mais rápido do Ryūga");
        System.out.println("(L, 2L, M, 2M, H, S, Super) que, apertado na primeira chance depois da defesa, acerta.");
        System.out.println();
        System.out.println("| lutador | especial | golpe | dano no acerto | vantagem no acerto | vantagem na defesa | distância na defesa | punível com | duração no erro |");
        System.out.println("|---|---|---|---|---|---|---|---|---|");
        List<String[]> casos = new ArrayList<>();
        for (String atk : ELENCO) for (String k : new String[] {"S1", "S2", "S3", "S4"}) casos.add(new String[] {atk, k});
        List<String> linhas = casos.parallelStream().map(c -> {
            Especial hit = especial(c[0], c[1], 0, null);
            if (hit == null) return String.format("| %s | %s | não saiu | | | | | | |", nome(c[0]), c[1]);
            Especial blk = especial(c[0], c[1], 1, null);
            Especial erro = especial(c[0], c[1], 2, null);
            String pun = "–";
            if (blk != null && blk.defendeu) {
                pun = "nada";
                for (String p : PUNICOES) {
                    Especial t = especial(c[0], c[1], 1, p);
                    if (t != null && !t.punivel.isEmpty()) { pun = p; break; }
                }
            }
            String vh = !hit.acertou ? "não acerta de perto" : hit.vantagem == Integer.MIN_VALUE ? "–" : String.format(Locale.ROOT, "%+d%s", hit.vantagem, hit.derrubou ? " (derruba)" : "");
            String vb = blk == null || !blk.defendeu ? "não toca" : blk.vantagem == Integer.MIN_VALUE ? "–" : String.format(Locale.ROOT, "%+d", blk.vantagem);
            return String.format(Locale.ROOT, "| %s | %s | %s | %s | %s | %s | %s | %s | %s |", nome(c[0]), c[1], hit.id + (hit.multi ? " (vários acertos)" : ""),
                hit.acertou ? Integer.toString(hit.dano) : "–", vh, vb, blk == null || !blk.defendeu ? "–" : Math.round(blk.dist) + " px", pun,
                erro == null ? "–" : erro.duracao + " q");
        }).collect(Collectors.toList());
        for (String l : linhas) System.out.println(l);
    }

    // ================================================================== T13 defesa perfeita

    /** Aperta trás {@code offset} quadros em relação ao contato (negativo: antes). */
    static String[] defesaPerfeita(String atk, String golpe, int offset, int contato) {
        CombatConfig cfg = new CombatConfig();
        FighterState a = new FighterState(ch(atk), "P1"), d = new FighterState(ch(BONECO), "P2");
        CombatEngine e = new CombatEngine(a, d, 900f, golpe.equals("S") ? 1300f : 960f, cfg);
        FighterInput in0 = new FighterInput(), in1 = new FighterInput();
        CombatFighter fa = e.fighter(0), fd = e.fighter(1);
        for (int i = 0; i < 10; i++) { in0.clear(); in1.clear(); e.step(in0, in1); }
        d.life = d.profile.maxLife - 2000;
        int vidaAntes = d.life;
        int guardaAntes = d.guard;
        boolean baixo = golpe.startsWith("2");
        String res = "acerto";
        int blockstun = 0;
        for (int fr = 0; fr < 200; fr++) {
            in0.clear();
            in1.clear();
            // O golpe sai no quadro 30: dá tempo de o aperto vir antes do contato até nos golpes rápidos.
            if (fr == 30) aperta(in0, golpe, fa);
            if (contato >= 0 && fr >= contato + offset) in1.direction = baixo ? 2 : 1;
            // A sonda (contato < 0) fica parada: o contato é o quadro em que o golpe chega em quem não defende.
            e.step(in0, in1);
            boolean fim = false;
            for (CombatEngine.HitEvent h : e.events()) {
                if (h.attacker != 0) continue;
                if (contato < 0) return new String[] {Integer.toString(fr)};
                res = h.blocked ? (e.cues().contains("JUST_DEFENSE") ? "perfeita" : "defesa") : "acerto";
                blockstun = fd.stunTotal;
                fim = true;
            }
            if (fim) break;
        }
        return new String[] {res, Integer.toString(blockstun), Integer.toString(d.life - vidaAntes), Integer.toString(guardaAntes - d.guard)};
    }

    static void t13() {
        titulo("T13 Defesa perfeita (aperto de trás de −8 a +2 quadros do contato)");
        System.out.println("O Ryūga fica parado e aperta trás N quadros antes (−) ou depois (+) do contato. P = defesa perfeita, D = defesa normal, X = levou.");
        System.out.println("Config: janela 5 quadros, bloqueio de 20 quadros entre apertos, blockstun 40%, vida +1,2% da máxima (CombatConfig.justDefense*).");
        System.out.println();
        int[] offs = {-8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2};
        StringBuilder cab = new StringBuilder("| atacante | golpe |"), sep = new StringBuilder("|---|---|");
        for (int o : offs) { cab.append(String.format(Locale.ROOT, " %+d |", o)); sep.append("---|"); }
        cab.append(" blockstun normal → perfeita | vida de volta |");
        sep.append("---|---|");
        System.out.println(cab);
        System.out.println(sep);
        String[] atks = {"player_base", "p03", "p09", "p11", "p15"};
        String[] golpes = {"L", "M", "H", "2M", "S"};
        int janelaTotal = 0, casos = 0;
        for (String atk : atks) {
            for (String g : golpes) {
                String[] probe = defesaPerfeita(atk, g, 0, -1);
                if (probe.length != 1) continue;
                int contato = Integer.parseInt(probe[0]);
                StringBuilder linha = new StringBuilder("| " + nome(atk) + " | " + g + " |");
                String bsNormal = "", bsPerf = "", cura = "";
                int janela = 0;
                for (int o : offs) {
                    String[] r = defesaPerfeita(atk, g, o, contato);
                    String m = r[0].equals("perfeita") ? "P" : r[0].equals("defesa") ? "D" : "X";
                    if (m.equals("P")) { janela++; bsPerf = r[1]; cura = r[2]; }
                    if (m.equals("D")) bsNormal = r[1];
                    linha.append(' ').append(m).append(" |");
                }
                janelaTotal += janela;
                casos++;
                linha.append(' ').append(bsNormal.isEmpty() ? "?" : bsNormal).append(" → ").append(bsPerf.isEmpty() ? "–" : bsPerf).append(" | ").append(cura.isEmpty() ? "–" : "+" + cura).append(" |");
                System.out.println(linha);
            }
        }
        System.out.printf(Locale.ROOT, "%nJanela média: %.1f quadros.%n", janelaTotal / (double)casos);
        // Sequência: dá para fazer duas defesas perfeitas seguidas?
        System.out.println();
        System.out.println("Defesa perfeita em sequência (oráculo: solta no começo de cada golpe e aperta 2 quadros antes do contato):");
        System.out.println();
        System.out.println("| atacante | sequência | defesas | perfeitas |");
        System.out.println("|---|---|---|---|");
        for (String atk : atks) {
            for (String[] s : new String[][] {{"L", "M", "H"}, {"L", "L", "L"}}) {
                int[] r = perfeitasSeguidas(atk, s);
                System.out.printf(Locale.ROOT, "| %s | %s | %d | %d |%n", nome(atk), String.join(" ", s), r[0], r[1]);
            }
        }
    }

    static int[] perfeitasSeguidas(String atk, String[] golpes) {
        CombatConfig cfg = new CombatConfig();
        FighterState a = new FighterState(ch(atk), "P1"), d = new FighterState(ch(BONECO), "P2");
        CombatEngine e = new CombatEngine(a, d, 900f, 960f, cfg);
        FighterInput in0 = new FighterInput(), in1 = new FighterInput();
        CombatFighter fa = e.fighter(0), fd = e.fighter(1);
        for (int i = 0; i < 10; i++) { in0.clear(); in1.clear(); e.step(in0, in1); }
        int k = 0, defesas = 0, perfeitas = 0, inicio = -1, startup = 0, ultimo = -1;
        AttackDefinition antes = null;
        int antesQ = 0;
        for (int fr = 0; fr < 300; fr++) {
            in0.clear();
            in1.clear();
            if (k < golpes.length) aperta(in0, golpes[k], fa);
            // Antes do primeiro golpe fica parado (um aperto cedo trancaria a janela por 20 quadros).
            boolean soltar = inicio < 0 || fr < inicio + startup - 2;
            in1.direction = soltar ? 0 : 1;
            e.step(in0, in1);
            boolean novo = fa.attacking() && fa.attack != null && (fa.attack != antes || fa.attackFrame < antesQ);
            if (novo && k < golpes.length && fa.attack.id.equals(golpes[k])) { k++; inicio = fr; startup = fa.attack.startupFrames; }
            if (fa.attacking()) { antes = fa.attack; antesQ = fa.attackFrame; }
            for (CombatEngine.HitEvent h : e.events()) {
                if (h.attacker == 0 && h.blocked) { defesas++; ultimo = fr; }
            }
            if (e.cues().contains("JUST_DEFENSE")) perfeitas++;
            if (k >= golpes.length && ultimo >= 0 && fr - ultimo > 40) break;
        }
        return new int[] {defesas, perfeitas};
    }

    // ================================================================== T14 quebra de guarda

    /** Quanto tempo a pressão leva para quebrar a guarda perfeita (−1: não quebrou em 30 s). */
    static int[] quebra(String atk, String tipo, int defesa) {
        CombatConfig cfg = new CombatConfig();
        FighterState a = new FighterState(ch(atk), "P1"), d = new FighterState(ch(BONECO), "P2");
        FighterState parceiro = new FighterState(ch(BateriaBalanco.parceiro(atk)), "P1");
        boolean proj = tipo.equals("projétil");
        CombatEngine e = new CombatEngine(a, d, 900f, proj ? 1300f : 960f, cfg);
        if (tipo.equals("TAG")) e.setTeam(0, a, parceiro);
        d.life = 1_000_000;
        FighterInput in0 = new FighterInput(), in1 = new FighterInput();
        CombatFighter fa = e.fighter(0), fd = e.fighter(1);
        int defendidos = 0, inicio = -1, startup = 0;
        boolean pushNesta = false;
        AttackDefinition antes = null;
        int antesQ = 0, passoSeq = 0;
        String[] seq = tipo.equals("L") ? new String[] {"L"} : tipo.equals("H") ? new String[] {"H"} : tipo.equals("L M H") ? new String[] {"L", "M", "H"}
            : tipo.equals("projétil") ? new String[] {"S"} : new String[] {"L", "M", "H"};
        for (int fr = 0; fr < 60 * 30; fr++) {
            in0.clear();
            in1.clear();
            a.superMeter = 3 * CombatConfig.METER_PER_BAR;
            if (defesa == 1) d.superMeter = 2 * CombatConfig.METER_PER_BAR;
            // Atacante: mantém a distância (anda até perto) e aperta a sequência.
            float gap = Math.abs(fa.x - fd.x);
            if (tipo.equals("Super")) {
                if (fa.canAct()) in0.superAttack = true;
            } else if (!proj && !CombatRules.reaches(fa.character(), seq[passoSeq % seq.length], false, gap, fd.body())) {
                // Fora do alcance: anda até ele (quem defende segura trás e recua).
                in0.direction = fa.x < fd.x ? 1 : 5;
            } else {
                aperta(in0, seq[passoSeq % seq.length], fa);
            }
            if (tipo.equals("TAG") && fa.canAct()) in0.assist = true;
            // Defesa.
            boolean alto = !fa.grounded || fa.attacking() && fa.attack != null && fa.attack.overhead;
            boolean baixo = fa.attacking() && fa.attack != null && fa.attack.low;
            int tras = baixo ? 2 : 1;
            if (alto) tras = 1;
            if (defesa == 2) {
                boolean soltar = inicio >= 0 && fr < inicio + startup - 2;
                in1.direction = soltar ? 0 : tras;
            } else {
                in1.direction = tras;
            }
            if (defesa == 1 && fd.status == CombatFighter.Status.BLOCKSTUN && !pushNesta && !fd.frozen()) {
                in1.pushblock = true;
                pushNesta = true;
            }
            if (fd.status != CombatFighter.Status.BLOCKSTUN) pushNesta = false;
            e.step(in0, in1);
            boolean novo = fa.attacking() && fa.attack != null && (fa.attack != antes || fa.attackFrame < antesQ);
            if (novo) { passoSeq++; inicio = fr; startup = fa.attack.startupFrames; }
            if (fa.attacking()) { antes = fa.attack; antesQ = fa.attackFrame; }
            for (CombatEngine.HitEvent h : e.events()) if (h.attacker == 0 && h.blocked) defendidos++;
            if (DEBUG && fr % 20 == 0) System.out.printf(Locale.ROOT, "%d gap %.0f a %s %s d %s guarda %d def %d dir %d%n", fr, Math.abs(fa.x - fd.x), fa.status, fa.attack == null ? "-" : fa.attack.id, fd.status, d.guard, defendidos, in1.direction);
            if (e.cues().contains("GUARD_BREAK")) {
                // A punição: L M H S na sequência, contando o dano até o combo acabar.
                int dano = punirQuebra(e, in0, in1);
                return new int[] {fr, defendidos, dano};
            }
        }
        return new int[] {-1, defendidos, 0};
    }

    static boolean DEBUG;

    static int punirQuebra(CombatEngine e, FighterInput in0, FighterInput in1) {
        CombatFighter fa = e.fighter(0);
        String[] rota = {"L", "M", "H", "S"};
        int k = 0;
        AttackDefinition antes = fa.attack;
        int antesQ = fa.attackFrame, dano = 0;
        boolean comecou = false;
        for (int fr = 0; fr < 240; fr++) {
            in0.clear();
            in1.clear();
            if (k < rota.length) aperta(in0, rota[k], fa);
            e.step(in0, in1);
            boolean novo = fa.attacking() && fa.attack != null && (fa.attack != antes || fa.attackFrame < antesQ);
            if (novo && k < rota.length && (fa.attack.id.equals(rota[k]) || rota[k].equals("S") && fa.attack.id.startsWith("S"))) k++;
            if (fa.attacking()) { antes = fa.attack; antesQ = fa.attackFrame; }
            for (CombatEngine.HitEvent h : e.events()) if (h.attacker == 0 && !h.blocked) { dano += h.damage; comecou = true; }
            ComboSession s = e.session(1);
            if (comecou && (s == null || !s.active) && k >= rota.length) break;
        }
        return dano;
    }

    static void t14() {
        titulo("T14 Quebra de guarda (defesa sempre certa)");
        CombatConfig c = new CombatConfig();
        System.out.printf(Locale.ROOT, "Config: guarda %d, gasto = blockstun × %d (meio nas batidas antes da última), volta %d/quadro depois de %d quadros sem defender (cheia em %.1f s);%n",
            CombatConfig.GUARD_MAX, c.guardDrainPerBlockstunFrame, c.guardRegenPerFrame, c.guardRegenDelayFrames,
            (c.guardRegenDelayFrames + CombatConfig.GUARD_MAX / (double)c.guardRegenPerFrame) / 60.0);
        System.out.printf(Locale.ROOT, "quebrou: aberto %d quadros e a guarda volta cheia. Tempo até quebrar (s) / golpes defendidos / dano da punição L M H S.%n%n",
            c.guardBreakStunFrames);
        String[] tipos = {"L", "H", "L M H", "projétil", "Super", "TAG"};
        String[] defesas = {"só defende", "pushblock", "defesa perfeita"};
        StringBuilder cab = new StringBuilder("| atacante | defesa |"), sep = new StringBuilder("|---|---|");
        for (String t : tipos) { cab.append(' ').append(t).append(" |"); sep.append("---|"); }
        System.out.println(cab);
        System.out.println(sep);
        String[] atks = {"player_base", "p03", "p09", "p11", "p05", "p13", "p16"};
        List<String[]> casos = new ArrayList<>();
        for (String atk : atks) for (int dfs = 0; dfs < defesas.length; dfs++) casos.add(new String[] {atk, Integer.toString(dfs)});
        List<String> linhas = casos.parallelStream().map(cs -> {
            StringBuilder linha = new StringBuilder("| " + nome(cs[0]) + " | " + defesas[Integer.parseInt(cs[1])] + " |");
            for (String t : tipos) {
                int[] r = quebra(cs[0], t, Integer.parseInt(cs[1]));
                linha.append(r[0] < 0 ? " não quebra (" + r[1] + ") |" : String.format(Locale.ROOT, " %.1f s / %d / %d |", r[0] / 60.0, r[1], r[2]));
            }
            return linha.toString();
        }).collect(Collectors.toList());
        for (String l : linhas) System.out.println(l);
    }

    public static void main(String[] args) {
        String q = args.length > 0 ? args[0] : "tudo";
        long t0 = System.nanoTime();
        System.out.println("# Auditoria — frente B (combos, recurso, defesa)");
        if (q.equals("t07") || q.equals("tudo")) t07();
        if (q.equals("t08") || q.equals("tudo")) t08();
        if (q.equals("t09") || q.equals("tudo")) t09();
        if (q.equals("t10") || q.equals("tudo")) t10();
        if (q.equals("t11") || q.equals("tudo")) t11();
        if (q.equals("t13") || q.equals("tudo")) t13();
        if (q.equals("t14") || q.equals("tudo")) t14();
        if (q.equals("dbgrota")) { Cenario cn = new Cenario(); cn.gap = Float.parseFloat(args[2]); List<String> r = Arrays.asList(args[3].split(","));
            for (int k = 1; k <= r.size(); k++) { Exec x = executar(args[1], BateriaBalanco.parceiro(args[1]), r.subList(0, k), cn); System.out.println(r.subList(0, k) + " valida " + x.valida + " dano " + x.dano + " acertos " + x.acertos); } }
        if (q.equals("g3")) g3combos();
        if (q.equals("amp")) for (String id : ELENCO) System.out.println(id + " " + String.join(" ", ampliadas(id, new String[0])) + " | " + ch(id).specials.keySet()
            + " S1=" + (ch(id).attack("S") == null ? "-" : ch(id).attack("S").id + "/" + ch(id).attack("S").kind) + " noAr=" + s1NoAr(id));
        if (q.equals("tempo")) {
            Cenario cn = new Cenario();
            cn.barras = Integer.parseInt(args[2]);
            cn.canto = args.length > 3;
            for (String[] acoes : new String[][] {BASE, ampliadas(args[1], BASE)}) {
                long t = System.nanoTime();
                Cand c = buscar(args[1], cn, new ArrayList<>(), acoes, Meta.DANO, Integer.getInteger("largura", 40), 16);
                System.out.printf(Locale.ROOT, "%d ações: %s → %s (%.1f s)%n", acoes.length, c == null ? "–" : c.texto(), c == null ? "–" : c.x.dano + "",
                    (System.nanoTime() - t) / 1e9);
            }
        }
        if (q.equals("dbg14")) { DEBUG = true; System.out.println(Arrays.toString(quebra(args[1], args[2], 0))); }
        if (q.equals("dbg13")) { String[] p = defesaPerfeita(args[1], args[2], 0, -1); System.out.println("contato " + p[0]);
            for (int o = -12; o <= 2; o++) System.out.println(o + " " + String.join(" ", defesaPerfeita(args[1], args[2], o, Integer.parseInt(p[0])))); }
        System.out.printf(Locale.ROOT, "%n(%.0f s)%n", (System.nanoTime() - t0) / 1e9);
    }
}
