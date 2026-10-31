package com.gamelutagpt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Comparação com o mercado (docs/balanco/mercado.md): os números do jogo que dá para pôr lado a
 * lado com SF6, Strive, Tekken 8, KOF XV, DBFZ, UMvC3 e 2XKO. Lutas CPU × CPU no motor de verdade
 * (como a {@link BateriaBalanco}), 2x2 e 1x1, e os dados fixos dos golpes e do movimento. Roda à
 * mão:
 *
 * <pre>java -cp ... com.gamelutagpt.BateriaMercado [fixos|lutas|chao|tudo] [lutas por modo] [nível]</pre>
 */
public final class BateriaMercado {
    private BateriaMercado() {}

    static final String[] NORMAIS = {"L", "M", "H", "2L", "2M", "2H"};

    // ------------------------------------------------------------------ dados fixos

    static void fixos() {
        CombatConfig cfg = new CombatConfig();
        BateriaBalanco.titulo("Dados fixos dos golpes (mediana do elenco; mín–máx)");
        System.out.println("| golpe | início | ativos | recuperação | total | vantagem na defesa | vantagem no acerto | parada (hitstop) | dano (% da vida) |");
        System.out.println("|---|---|---|---|---|---|---|---|---|");
        for (String g : NORMAIS) {
            List<double[]> v = new ArrayList<>();
            for (String id : BateriaBalanco.ELENCO) {
                CharacterDefinition c = BateriaBalanco.ch(id);
                CharacterDefinition.Move m = c.move(g, false);
                if (m == null || m.attack == null) continue;
                AttackDefinition a = m.attack;
                int depois = a.activeFrames - 1 + a.recoveryFrames;
                v.add(new double[] {a.startupFrames, a.activeFrames, a.recoveryFrames, a.totalFrames,
                    a.blockstunFrames - depois, a.hitstunFrames - depois, a.hitstopFrames,
                    100.0 * m.damage / c.fighter.maxLife});
            }
            StringBuilder sb = new StringBuilder("| " + g);
            for (int k = 0; k < 8; k++) {
                double[] col = new double[v.size()];
                for (int i = 0; i < v.size(); i++) col[i] = v.get(i)[k];
                Arrays.sort(col);
                sb.append(String.format(Locale.ROOT, " | %s (%s a %s)", n(med(col)), n(col[0]), n(col[col.length - 1])));
            }
            System.out.println(sb.append(" |"));
        }
        System.out.println();
        System.out.println("Vantagem = atordoamento − (ativos − 1 + recuperação), contando do 1º quadro ativo.");

        BateriaBalanco.titulo("Vida, corpo, movimento e sistema");
        int[] vidas = Arrays.stream(BateriaBalanco.ELENCO).mapToInt(id -> BateriaBalanco.ch(id).fighter.maxLife).sorted().toArray();
        CharacterDefinition.Body corpo = BateriaBalanco.ch("player_base").fighter.body;
        float largura = 2 * corpo.halfWidth;
        System.out.println("| item | valor |");
        System.out.println("|---|---|");
        linha("vida (mediana; mín–máx)", vidas[vidas.length / 2] + " (" + vidas[0] + "–" + vidas[vidas.length - 1] + ")");
        linha("tela / mundo", (int) Arena.VW + " × " + (int) Arena.VH + " / " + (int) Arena.WORLD_WIDTH + " px (" + f1(Arena.WORLD_WIDTH / Arena.VW) + " telas)");
        linha("corpo (Ryūga): largura / altura", (int) largura + " / " + (int) corpo.standHeight + " px (altura = " + pct(corpo.standHeight / Arena.VH) + " da tela)");
        linha("corpos que cabem na tela", f1(Arena.VW / largura));
        linha("andar para frente / trás", (int) cfg.walkSpeed + " / " + (int) cfg.walkBackSpeed + " px/s = " + pct(cfg.walkSpeed / Arena.VW) + " / " + pct(cfg.walkBackSpeed / Arena.VW) + " da tela por s = " + f1(cfg.walkSpeed / largura) + " corpos/s");
        linha("pulo (preparação + ar + aterrissagem)", cfg.jumpSquatFrames + " + medido no T03 (41 no ar) + " + cfg.jumpLandingFrames + " quadros");
        linha("super pulo", cfg.superJumpFrames + " quadros de entrada; ~104 no ar (T03)");
        linha("backdash", cfg.backdashFrames + " quadros + " + cfg.backdashRecoveryFrames + " de recuperação");
        linha("agarrão: início / janela de escape / dano", cfg.throwStartupFrames + " / " + cfg.throwTechWindow + " quadros / " + cfg.throwDamage);
        linha("buffer: normal / especial / movimento", cfg.bufferFrames + " / " + cfg.specialBufferFrames + " / " + cfg.motionWindowFrames + " quadros");
        linha("derrubado: queda + chão + levantar", cfg.knockdownFallFrames + " + " + cfg.knockdownDownFrames + " + " + cfg.wakeupFrames + " quadros");
        linha("defesa perfeita (janela)", cfg.justDefenseFrames + " quadros");
        linha("relógio", BattleFlow.TIME_LIMIT_SECONDS + " s por luta (2x2 numa luta só, sem rounds)");

        BateriaBalanco.titulo("Tempos e proporções (sem unidade, para comparar com outros jogos)");
        System.out.println("| item | valor |");
        System.out.println("|---|---|");
        for (String g : new String[] {"S", "S2", "S3", "S4", "SUPER"}) {
            List<Integer> tot = new ArrayList<>();
            for (String id : BateriaBalanco.ELENCO) {
                CharacterDefinition c = BateriaBalanco.ch(id);
                CharacterDefinition.Move m = c.moveFor(g);
                if (m == null) m = c.move(g, false);
                if (m != null && m.attack != null) tot.add(m.attack.totalFrames);
            }
            if (tot.isEmpty()) continue;
            double[] v = tot.stream().mapToDouble(Integer::doubleValue).sorted().toArray();
            linha("duração total do " + g + " (mediana; mín–máx)", n(med(v)) + " quadros (" + n(v[0]) + "–" + n(v[v.length - 1]) + ")");
        }
        double andar = cfg.walkSpeed / 60.0;
        linha("atravessar a tela andando / andando para trás", f1(Arena.VW / cfg.walkSpeed) + " s / " + f1(Arena.VW / cfg.walkBackSpeed) + " s");
        linha("trás ÷ frente (andar)", f2(cfg.walkBackSpeed / cfg.walkSpeed));
        // Alcance real (o do desenho, da tabela da CPU): ponta do golpe a partir do eixo do atacante.
        for (String g : new String[] {"2M", "M", "H", "2L"}) {
            List<Double> r = new ArrayList<>();
            for (String id : BateriaBalanco.ELENCO) {
                float[] m = CpuReach.measured(BateriaBalanco.ch(id), g);
                if (m != null && m[0] > 0) r.add((double) m[0] + corpo.halfWidth);
            }
            double[] v = r.stream().mapToDouble(Double::doubleValue).sorted().toArray();
            double alc = med(v);
            linha("alcance do " + g + " entre eixos (mediana)", (int) alc + " px = " + pct(alc / Arena.VW) + " da tela = "
                + f1(alc / (2 * corpo.pushHalfWidth)) + " corpos (caixa de empurrão) = " + n(Math.round(alc / andar)) + " quadros andando");
        }
        List<Double> melhor = new ArrayList<>();
        for (String id : BateriaBalanco.ELENCO) {
            double b = 0;
            for (String g : NORMAIS) {
                float[] m = CpuReach.measured(BateriaBalanco.ch(id), g);
                if (m != null) b = Math.max(b, m[0] + corpo.halfWidth);
            }
            melhor.add(b);
        }
        double mb = med(arr(melhor));
        linha("melhor normal de alcance (mediana do elenco)", (int) mb + " px = " + pct(mb / Arena.VW) + " da tela = " + n(Math.round(mb / andar)) + " quadros andando");
        linha("dash: distância ÷ alcance do 2M", "207 px em 20 quadros (T03) ÷ alcance do 2M (acima)");
        float agarra = 2 * corpo.halfWidth + cfg.throwRange;
        linha("agarrão entre eixos ÷ largura da caixa de empurrão", (int) agarra + " ÷ " + (int) (2 * corpo.pushHalfWidth) + " = " + f2(agarra / (2 * corpo.pushHalfWidth)));
        linha("pulo à frente ÷ tela", "280 px (T03) = " + pct(280 / Arena.VW));
    }

    // ------------------------------------------------------------------ lutas

    /** Uma sequência de acertos sem o defensor sair do atordoamento (a ComboSession do motor). */
    static final class Sessao {
        double danoPct;
        int acertos, quadros;
    }

    static final class Medidas {
        final List<Double> duracao = new ArrayList<>(), interacao = new ArrayList<>(), distanciaNeutro = new ArrayList<>();
        final List<Double> danoPorSegundo = new ArrayList<>(), aberturasPorQueda = new ArrayList<>();
        final List<Sessao> sessoes = new ArrayList<>();
        int lutas, tempo, quedas, supers, ultras, breakers, assists, trocas, shinkais, defendidos, agarroes, quebrasGuarda;
        int vitoriaComMaisVida;
        // Jogo no chão: dano por origem, contra-golpes, antiaéreos, golpes no vazio, pulos.
        final java.util.Map<String, Integer> danoPorOrigem = new java.util.TreeMap<>();
        int acertos, contraGolpes, antiaereos, entradasDoAr, entradasDoArDefendidas, pulos;
        int normaisNoChao, normaisNoChaoComContato, quadrosLuta;
    }

    /** Luta só no chão: sem super pulo, air dash, SHINKAI, Ultra, Breaker, assist e troca. */
    static final java.util.function.Consumer<CombatConfig> SO_CHAO = cfg -> {
        cfg.superJumpFrames = -1;
        cfg.launcherChaseFrames = 0;
        cfg.airDashMinHeight = 1e9f;
        cfg.comboBreakerCost = Integer.MAX_VALUE;
    };

    static final BateriaBalanco.Mao MAO_CHAO = (luta, lado, in) -> {
        in.superJump = in.shinkai = in.ultra = in.assist = in.tag = false;
        if (!luta.f(lado).grounded) in.dash = in.backdash = false;
    };

    static String origem(CombatEngine.HitEvent h) {
        String id = h.moveId == null ? "?" : h.moveId;
        if (id.equals("THROW")) return "agarrão";
        if (id.startsWith("SUPER") || id.startsWith("ULTRA")) return "Super";
        if (h.projectile) return "projétil";
        if (id.startsWith("j")) return "normal no ar";
        if (id.startsWith("S")) return "especial";
        return "normal no chão";
    }

    static boolean normalNoChao(AttackDefinition a) {
        String id = a.id;
        return id != null && !id.startsWith("j") && !id.startsWith("S") && !id.equals("THROW") && !id.startsWith("ULTRA")
            && !id.equals(AttackDefinition.JUMP);
    }


    static Medidas lutas(int tamanho, int n, int nivel, long base) {
        return lutas(tamanho, n, nivel, base, false);
    }

    static Medidas lutas(int tamanho, int n, int nivel, long base, boolean chao) {
        Medidas md = new Medidas();
        java.util.function.Consumer<CombatConfig> ajusteAntes = BateriaBalanco.AJUSTE_CFG;
        if (chao) BateriaBalanco.AJUSTE_CFG = SO_CHAO;
        for (int i = 0; i < n; i++) {
            Random r = new Random(base + i);
            String[] t0 = tamanho == 2 ? BateriaBalanco.par(r) : new String[] {BateriaBalanco.ELENCO[r.nextInt(BateriaBalanco.ELENCO.length)]};
            String[] t1 = tamanho == 2 ? BateriaBalanco.par(r) : new String[] {BateriaBalanco.ELENCO[r.nextInt(BateriaBalanco.ELENCO.length)]};
            BateriaBalanco.Luta l = new BateriaBalanco.Luta(t0, t1, nivel, base + i, new CombatConfig());
            if (chao) l.mao[0] = l.mao[1] = MAO_CHAO;
            CombatFighter.Status[] statusAntes = {CombatFighter.Status.NEUTRAL, CombatFighter.Status.NEUTRAL};
            boolean[] noChaoAntes = {true, true}, contato = new boolean[2];
            AttackDefinition[] golpeAntes = new AttackDefinition[2];
            ComboSession[] vista = new ComboSession[2];
            int[] barraAntes = new int[2];
            boolean[] assistAntes = new boolean[2];
            FighterState[] titular = {l.e.team(0).pointState(), l.e.team(1).pointState()};
            int luta = 0, interage = 0, neutro = 0, sessoesLuta = 0, quedasLuta = 0;
            double soma = 0;
            while (!l.acabou() && l.quadro < 60 * 60 * 4) {
                l.passo();
                if (l.flow.phase() != BattleFlow.Phase.FIGHT && l.flow.phase() != BattleFlow.Phase.GO) continue;
                luta++;
                CombatFighter a = l.f(0), b = l.f(1);
                if (ocupado(a) || ocupado(b)) interage++;
                else if (a.status == CombatFighter.Status.NEUTRAL && b.status == CombatFighter.Status.NEUTRAL) {
                    neutro++;
                    soma += Math.abs(a.x - b.x);
                }
                for (int lado = 0; lado < 2; lado++) {
                    ComboSession s = l.e.lastSession(1 - lado);
                    if (s != null && s != vista[lado] && s.attacker == lado && !s.active) {
                        vista[lado] = s;
                        if (s.comboDamage > 0) {
                            Sessao x = new Sessao();
                            x.danoPct = 100.0 * s.comboDamage / l.f(1 - lado).state.profile.maxLife;
                            x.acertos = s.hitCount;
                            x.quadros = s.comboDuration;
                            md.sessoes.add(x);
                            sessoesLuta++;
                        }
                    }
                    CombatFighter f = l.f(lado);
                    int barra = f.state.superMeter;
                    if (barra < barraAntes[lado] && f.state == titular[lado]
                        && barraAntes[lado] - barra >= CombatConfig.METER_PER_BAR / 2) md.supers++;
                    barraAntes[lado] = barra;
                    boolean as = l.e.team(lado).assistOut();
                    if (as && !assistAntes[lado]) md.assists++;
                    assistAntes[lado] = as;
                    if (l.e.team(lado).pointState() != titular[lado]) {
                        if (titular[lado].life <= 0) quedasLuta++;
                        else md.trocas++;
                        titular[lado] = l.e.team(lado).pointState();
                        barraAntes[lado] = titular[lado].superMeter;
                    }
                }
                for (String c : l.e.cues()) {
                    if ("SHINKAI".equals(c)) md.shinkais++;
                    else if ("GUARD_BREAK".equals(c)) md.quebrasGuarda++;
                }
                for (CombatEngine.HitEvent h : l.e.events()) {
                    if (h.blocked) md.defendidos++;
                    if ("THROW".equals(h.moveId)) md.agarroes++;
                    contato[h.attacker] = true;
                    boolean doAr = !noChaoAntes[h.attacker] && !h.projectile, noAr = !noChaoAntes[h.defender];
                    if (doAr && !noAr) {
                        if (h.blocked) md.entradasDoArDefendidas++;
                        else md.entradasDoAr++;
                    }
                    if (h.blocked) continue;
                    md.acertos++;
                    md.danoPorOrigem.merge(origem(h), h.damage, Integer::sum);
                    if (statusAntes[h.defender] == CombatFighter.Status.ATTACK) md.contraGolpes++;
                    if (noAr && noChaoAntes[h.attacker] && statusAntes[h.defender] != CombatFighter.Status.AIR_HITSTUN) md.antiaereos++;
                }
                for (int lado = 0; lado < 2; lado++) {
                    CombatFighter f = l.f(lado);
                    // Golpe novo: fecha o anterior (acertou ou defendido = contato; senão, no vazio).
                    if (f.attack != golpeAntes[lado]) {
                        if (golpeAntes[lado] != null && normalNoChao(golpeAntes[lado])) {
                            md.normaisNoChao++;
                            if (contato[lado]) md.normaisNoChaoComContato++;
                        }
                        golpeAntes[lado] = f.attack;
                        contato[lado] = false;
                    }
                    if (noChaoAntes[lado] && !f.grounded && (statusAntes[lado] == CombatFighter.Status.NEUTRAL)) md.pulos++;
                    statusAntes[lado] = f.status;
                    noChaoAntes[lado] = f.grounded;
                }
                if (l.e.ultraConnected() >= 0) md.ultras++;
            }
            // O último a cair não troca de titular.
            for (int lado = 0; lado < 2; lado++) if (l.f(lado).state.life <= 0 || l.f(lado).ko()) quedasLuta++;
            md.lutas++;
            md.quadrosLuta += luta;
            md.duracao.add(luta / 60.0);
            md.interacao.add(luta == 0 ? 0 : (double) interage / luta);
            if (neutro > 0) md.distanciaNeutro.add(soma / neutro);
            int dano = l.est[0].dano + l.est[1].dano;
            md.danoPorSegundo.add(luta == 0 ? 0 : dano / (luta / 60.0));
            md.quedas += quedasLuta;
            if (quedasLuta > 0) md.aberturasPorQueda.add((double) sessoesLuta / quedasLuta);
            if (l.flow.timeOver()) md.tempo++;
            md.breakers += l.est[0].breakers + l.est[1].breakers;
        }
        BateriaBalanco.AJUSTE_CFG = ajusteAntes;
        return md;
    }

    static boolean ocupado(CombatFighter f) {
        switch (f.status) {
            case HITSTUN: case BLOCKSTUN: case AIR_HITSTUN: case KNOCKDOWN: case THROWN: case ULTRA: return true;
            default: return false;
        }
    }

    static void relatorio(String nome, Medidas md, int tamanho) {
        BateriaBalanco.titulo(nome + " (" + md.lutas + " lutas)");
        double[] dur = arr(md.duracao);
        double[] dano = md.sessoes.stream().mapToDouble(s -> s.danoPct).sorted().toArray();
        double[] acertos = md.sessoes.stream().mapToDouble(s -> s.acertos).sorted().toArray();
        double[] segs = md.sessoes.stream().mapToDouble(s -> s.quadros / 60.0).sorted().toArray();
        double[] combos = md.sessoes.stream().filter(s -> s.acertos >= 3).mapToDouble(s -> s.danoPct).sorted().toArray();
        long soltos = md.sessoes.stream().filter(s -> s.acertos == 1).count();
        double mediaDano = Arrays.stream(dano).average().orElse(0);
        System.out.println("| medida | valor |");
        System.out.println("|---|---|");
        linha("duração da luta: mediana (p10–p90)", f1(med(dur)) + " s (" + f1(q(dur, .1)) + "–" + f1(q(dur, .9)) + ")");
        linha("duração média por personagem derrubado", f1(Arrays.stream(dur).sum() / Math.max(1, md.quedas)) + " s (" + md.quedas + " quedas)");
        linha("acaba no tempo", pct((double) md.tempo / md.lutas));
        linha("tempo em interação (alguém apanhando, defendendo ou caído)", pct(med(arr(md.interacao))));
        linha("distância no neutro (mediana)", (int) med(arr(md.distanciaNeutro)) + " px = " + pct(med(arr(md.distanciaNeutro)) / Arena.VW) + " da tela");
        linha("dano por segundo de luta (os dois lados)", (int) med(arr(md.danoPorSegundo)) + " (" + pct(med(arr(md.danoPorSegundo)) / 10000) + " de uma vida por s)");
        linha("aberturas (sequências com dano) por queda", f1(med(arr(md.aberturasPorQueda))));
        linha("toques para derrubar (100% ÷ dano médio por abertura)", f1(100 / Math.max(.01, mediaDano)));
        linha("dano por abertura: média / mediana / p90", f1(mediaDano) + "% / " + f1(med(dano)) + "% / " + f1(q(dano, .9)) + "% da vida");
        linha("combos de 3+ acertos: dano mediano / p90 / máx", f1(med(combos)) + "% / " + f1(q(combos, .9)) + "% / " + f1(combos.length == 0 ? 0 : combos[combos.length - 1]) + "%");
        linha("acertos por abertura: mediana / p90 / máx", n(med(acertos)) + " / " + n(q(acertos, .9)) + " / " + n(acertos.length == 0 ? 0 : acertos[acertos.length - 1]));
        linha("duração da abertura: mediana / p90", f2(med(segs)) + " / " + f2(q(segs, .9)) + " s");
        double[] segsCombo = md.sessoes.stream().filter(s -> s.acertos >= 3).mapToDouble(s -> s.quadros / 60.0).sorted().toArray();
        double[] acertosCombo = md.sessoes.stream().filter(s -> s.acertos >= 3).mapToDouble(s -> s.acertos).sorted().toArray();
        linha("combos de 3+: duração mediana / p90 / máx; acertos medianos", f2(med(segsCombo)) + " / " + f2(q(segsCombo, .9)) + " / "
            + f2(segsCombo.length == 0 ? 0 : segsCombo[segsCombo.length - 1]) + " s; " + n(med(acertosCombo)) + " acertos");
        linha("aberturas de 1 acerto só", pct((double) soltos / Math.max(1, md.sessoes.size())));
        double total = md.sessoes.stream().mapToDouble(s -> s.danoPct).sum();
        double d1 = md.sessoes.stream().filter(s -> s.acertos == 1).mapToDouble(s -> s.danoPct).sum();
        double d3 = md.sessoes.stream().filter(s -> s.acertos >= 3).mapToDouble(s -> s.danoPct).sum();
        linha("de onde vem o dano: 1 acerto / 2 acertos / combos de 3+", pct(d1 / total) + " / " + pct((total - d1 - d3) / total) + " / " + pct(d3 / total));
        linha("combos de 3+ por queda", f1((double) md.sessoes.stream().filter(s -> s.acertos >= 3).count() / Math.max(1, md.quedas)));
        double l = md.lutas;
        linha("por luta: defendidos / agarrões", f1(md.defendidos / l) + " / " + f1(md.agarroes / l));
        linha("por luta: Supers / Ultras / Breakers", f1(md.supers / l) + " / " + f1(md.ultras / l) + " / " + f1(md.breakers / l));
        if (tamanho == 2) linha("por luta: assists / trocas / SHINKAI", f1(md.assists / l) + " / " + f1(md.trocas / l) + " / " + f1(md.shinkais / l));
        linha("por luta: quebras de guarda", f1(md.quebrasGuarda / l));
        if (md.acertos == 0) return;
        double min = md.quadrosLuta / 3600.0;
        int totalDano = md.danoPorOrigem.values().stream().mapToInt(Integer::intValue).sum();
        StringBuilder origem = new StringBuilder();
        md.danoPorOrigem.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue())
            .forEach(e -> origem.append(origem.length() == 0 ? "" : " · ").append(e.getKey()).append(' ').append(pct((double) e.getValue() / totalDano)));
        linha("dano por origem", origem.toString());
        linha("acertos em quem estava atacando (contra-golpe/punição)", pct((double) md.contraGolpes / md.acertos) + " dos acertos");
        linha("normais no chão que encostam (acerto ou defesa)", pct((double) md.normaisNoChaoComContato / Math.max(1, md.normaisNoChao)) + " (" + f1(md.normaisNoChao / min) + " por minuto)");
        linha("pulos por minuto (os dois lados)", f1(md.pulos / min));
        linha("entrada do ar: acerta / defendida / derrubada no antiaéreo", f1(md.entradasDoAr / min) + " / " + f1(md.entradasDoArDefendidas / min) + " / " + f1(md.antiaereos / min) + " por minuto");
    }

    // ------------------------------------------------------------------ util

    static void linha(String a, String b) { System.out.println("| " + a + " | " + b + " |"); }

    static double[] arr(List<Double> v) { return v.stream().mapToDouble(Double::doubleValue).sorted().toArray(); }

    static double med(double[] v) { return q(v, .5); }

    static double q(double[] v, double p) {
        if (v.length == 0) return 0;
        double[] s = v.clone();
        Arrays.sort(s);
        return s[Math.min(s.length - 1, (int) Math.floor(p * (s.length - 1) + .5))];
    }

    static String n(double v) { return v == Math.rint(v) ? Integer.toString((int) v) : f1(v); }

    static String f1(double v) { return String.format(Locale.ROOT, "%.1f", v); }

    static String f2(double v) { return String.format(Locale.ROOT, "%.2f", v); }

    static String pct(double v) { return String.format(Locale.ROOT, "%.0f%%", 100 * v); }

    public static void main(String[] args) {
        String o = args.length > 0 ? args[0] : "tudo";
        int n = args.length > 1 ? Integer.parseInt(args[1]) : 300;
        int nivel = args.length > 2 ? Integer.parseInt(args[2]) : BateriaBalanco.NIVEL;
        System.out.println("# Bateria do mercado (CPU nível " + nivel + ")");
        if (o.equals("fixos") || o.equals("tudo")) fixos();
        if (o.equals("chao")) {
            fixos();
            relatorio("1x1 só no chão (sem super pulo, air dash, SHINKAI, Ultra, Breaker)", lutas(1, n, nivel, 950_000, true), 1);
            relatorio("1x1 com tudo (mesmas sementes)", lutas(1, n, nivel, 950_000, false), 1);
        }
        if (o.equals("lutas") || o.equals("tudo")) {
            relatorio("2x2 (o jogo de verdade)", lutas(2, n, nivel, 900_000), 2);
            relatorio("1x1 (para comparar com os jogos de um personagem)", lutas(1, n, nivel, 950_000), 1);
        }
    }
}
