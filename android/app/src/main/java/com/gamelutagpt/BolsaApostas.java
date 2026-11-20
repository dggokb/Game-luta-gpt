package com.gamelutagpt;

import com.gamelutagpt.render.RenderCanvas;
import java.util.HashMap;
import java.util.Map;

/**
 * A Bolsa de Apostas da Semifinal (o Apostador apresenta a luta): os apostadores pagam pelo
 * espetáculo, e o dinheiro vai para a dupla que dá show.
 *
 * <ul>
 *   <li><b>Entra dinheiro</b> com o dano que entra, o primeiro golpe, o contra-golpe, o combo
 *       grande, a defesa perfeita, o guard break, o combo breaker, o K.O. e, muito mais, o Super,
 *       o SHINKAI, o Ultra e o Ultra SHINKAI.</li>
 *   <li><b>Odds</b>: pela vida das duplas e pelo dinheiro de cada uma. A zebra (odd alta) ganha
 *       mais pelo mesmo golpe que a favorita, então a virada rende muito e a luta não morre
 *       cedo.</li>
 *   <li><b>Patamar</b>: a cada {@link #PATAMAR} novos na bolsa (acima do maior valor que a dupla
 *       já teve), o titular dela ganha {@link #SUPER_DO_PATAMAR} de barra de Super. O show vira
 *       barra, a barra vira mais show.</li>
 *   <li><b>Quem esfria perde</b>: o titular que passa {@link #ESFRIA} sem atacar (parado,
 *       recuando ou só defendendo) esfria; depois de {@link #CAI} os apostadores tiram dinheiro
 *       dele ({@link #DRENO_POR_SEGUNDO} do que tem, por segundo) até ele voltar a atacar. Apanhar
 *       (hitstun, chão) não conta: aí quem paga é quem bate. O dinheiro tirado precisa ser
 *       recuperado antes do próximo patamar.</li>
 * </ul>
 *
 * O total fica só na luta (não vai para a história). O Apostador comenta pelo narrador
 * ({@link com.gamelutagpt.historia.Narracao}, eventos {@code aposta_*}). A CPU lê
 * {@link #pressa} e {@link #show}. Java puro (o desenho é pelo {@link RenderCanvas}).
 */
final class BolsaApostas {
    /** A aposta de abertura em cada dupla (odds iguais). */
    static final long INICIAL = 500_000L;
    /** ¥ por ponto de dano que entra (a vida de um lutador fica perto de 10.000). */
    static final long POR_DANO = 40L;
    static final long PRIMEIRO_GOLPE = 80_000L, CONTRA_GOLPE = 40_000L;
    /** Combo fechado: por acerto, a partir de {@link NarradorLuta#COMBO} e de {@link NarradorLuta#COMBO_GRANDE}. */
    static final long COMBO_POR_ACERTO = 15_000L, COMBO_GRANDE_POR_ACERTO = 30_000L;
    static final long DEFESA_PERFEITA = 60_000L, GUARD_BREAK = 120_000L, COMBO_BREAKER = 150_000L;
    static final long KO = 300_000L;
    static final long SUPER = 200_000L, SHINKAI = 250_000L, ULTRA = 450_000L, ULTRA_SHINKAI = 900_000L;

    /** A cada tanto acima do maior valor já alcançado, o titular ganha barra. */
    static final long PATAMAR = 750_000L;
    /** Barra de Super por patamar (um quarto de barra). */
    static final int SUPER_DO_PATAMAR = CombatConfig.METER_PER_BAR / 4;

    /** Quadros sem atacar até esfriar (aviso) e até o dinheiro começar a cair. */
    static final int ESFRIA = 3 * 60, CAI = 5 * 60;
    /** Fração do dinheiro que sai por segundo de quem está frio. */
    static final double DRENO_POR_SEGUNDO = 0.06;

    /** Odds: a casa fica com um pouco ({@link #MARGEM}); a odd vai de {@link #ODD_MIN} a {@link #ODD_MAX}. */
    static final double MARGEM = 0.92, ODD_MIN = 1.05, ODD_MAX = 9.9;
    /** Peso da vida (o resto é o dinheiro) no favoritismo. */
    static final double PESO_VIDA = 0.6;
    /** Odd das duas iguais: o ganho é multiplicado por odd / {@link #ODD_PAR}, entre os limites. */
    static final double ODD_PAR = MARGEM / 0.5, MULT_MIN = 0.6, MULT_MAX = 3.0;
    /** Diferença de odd que define a favorita (a virada só conta passando dela). */
    static final double FAVORITA = 0.3;

    // ------------------------------------------------------------ estado

    private final long[] dinheiro = new long[2];
    /** O maior valor que cada dupla já teve (o patamar conta a partir dele). */
    private final long[] recorde = new long[2];
    private final int[] patamares = new int[2];
    private final double[] odd = {ODD_PAR, ODD_PAR};
    /** Quadros sem atacar do titular de cada lado. */
    private final int[] quieto = new int[2];
    private final boolean[] avisouEsfriando = new boolean[2], avisouCaindo = new boolean[2];
    /** O último ganho de cada lado (para o "+¥" no HUD) e há quantos quadros. */
    private final long[] ultimoGanho = new long[2];
    private final int[] ganhoIdade = {999, 999}, patamarIdade = {999, 999};
    private int favorita = -1;

    private boolean comecou, primeiroGolpe, acabou;
    private final AttackDefinition[] ultimoGolpe = new AttackDefinition[2];
    private final boolean[] emUltra = new boolean[2], emShinkai = new boolean[2], caido = new boolean[2];
    private final ComboSession[] ultimaSessao = new ComboSession[2];
    private final float[] ultimoX = new float[2];
    private int quadro;

    private java.util.function.Function<CharacterDefinition, String> nomes = c -> c.displayName;

    void setNomes(java.util.function.Function<CharacterDefinition, String> nomes) {
        this.nomes = nomes != null ? nomes : c -> c.displayName;
    }

    /** Luta nova: as duas duplas com a aposta de abertura. */
    void reset() {
        for (int s = 0; s < 2; s++) {
            dinheiro[s] = recorde[s] = INICIAL;
            patamares[s] = 0;
            odd[s] = ODD_PAR;
            quieto[s] = 0;
            avisouEsfriando[s] = avisouCaindo[s] = false;
            ultimoGanho[s] = 0;
            ganhoIdade[s] = patamarIdade[s] = 999;
            ultimoGolpe[s] = null;
            emUltra[s] = emShinkai[s] = caido[s] = false;
            ultimaSessao[s] = null;
            ultimoX[s] = Float.NaN;
        }
        favorita = -1;
        comecou = primeiroGolpe = acabou = false;
        quadro = 0;
    }

    // ------------------------------------------------------------ leitura

    long dinheiro(int side) { return dinheiro[side]; }

    /** A odd da dupla (quanto paga cada ¥ apostado nela). */
    double odd(int side) { return odd[side]; }

    /** A parte da bolsa que é desta dupla (0 a 1), para a barra. */
    float parcela(int side) {
        long total = dinheiro[0] + dinheiro[1];
        return total <= 0 ? 0.5f : dinheiro[side] / (float)total;
    }

    int patamares(int side) { return patamares[side]; }

    /** Esfriando: de 0 (atacou agora) a 1 (o dinheiro já está caindo). */
    float frio(int side) {
        return Math.min(1f, quieto[side] / (float)CAI);
    }

    boolean caindo(int side) { return quieto[side] >= CAI; }

    /**
     * Pressa da CPU deste lado (0 a 1): sobe de quando esfria até quando o dinheiro cai, para
     * ela voltar a atacar antes de perder (a mesma pressa do relógio no fim).
     */
    float pressa(int side) {
        if (quieto[side] <= ESFRIA / 2) return 0f;
        return Math.min(1f, (quieto[side] - ESFRIA / 2) / (float)(CAI - ESFRIA / 2));
    }

    /**
     * Quanto o espetáculo vale para esta dupla agora (a CPU arrisca o Super, o Ultra e o SHINKAI
     * nessa proporção): 1 com as odds iguais, mais na zebra.
     */
    double show(int side) {
        return multiplicador(side);
    }

    /** O ganho é multiplicado por isto: a zebra ganha mais pelo mesmo golpe. */
    double multiplicador(int side) {
        return Math.max(MULT_MIN, Math.min(MULT_MAX, odd[side] / ODD_PAR));
    }

    // ------------------------------------------------------------ luta

    /** Um quadro, depois do passo do motor e do {@link BattleFlow#update}. */
    void update(CombatEngine engine, BattleFlow flow, NarradorLuta.Voz voz) {
        quadro++;
        for (int s = 0; s < 2; s++) {
            if (ganhoIdade[s] < 999) ganhoIdade[s]++;
            if (patamarIdade[s] < 999) patamarIdade[s]++;
        }
        BattleFlow.Phase fase = flow.phase();
        if (!comecou && (fase == BattleFlow.Phase.GO || fase == BattleFlow.Phase.FIGHT) && flow.battle()) {
            comecou = true;
            voz.narrar("aposta_inicio", vars("p1", nome(engine, 0), "p2", nome(engine, 1)));
        }
        if (fase == BattleFlow.Phase.OVER) acabou = true;
        if (!comecou || acabou) {
            lembrar(engine);
            return;
        }
        calcularOdds(engine);

        // Golpes que entraram neste quadro.
        boolean contra = engine.cues().contains("COUNTER_HIT");
        for (CombatEngine.HitEvent e : engine.events()) {
            if (e.attacker < 0 || e.attacker > 1) continue;
            quieto[e.attacker] = 0;
            if (e.blocked) continue;
            long base = Math.max(0, e.damage) * POR_DANO;
            if (!primeiroGolpe) {
                primeiroGolpe = true;
                base += PRIMEIRO_GOLPE;
            }
            if (contra) {
                base += CONTRA_GOLPE;
                contra = false;
            }
            ganhar(engine, e.attacker, base, voz);
        }
        for (String cue : engine.cues()) {
            switch (cue) {
                case "GUARD_BREAK":
                    for (int s = 0; s < 2; s++) if (engine.fighter(s).guardBroken) ganhar(engine, 1 - s, GUARD_BREAK, voz);
                    break;
                case "COMBO_BREAKER":
                    for (int s = 0; s < 2; s++) {
                        if (engine.fighter(s).comboBroke && engine.fighter(1 - s).inHitstun()) ganhar(engine, s, COMBO_BREAKER, voz);
                    }
                    break;
                case "JUST_DEFENSE":
                case "JUST_DEFENSE_ABSORB":
                    for (int s = 0; s < 2; s++) {
                        CombatFighter f = engine.fighter(s);
                        if (f.justDefended && f.status == CombatFighter.Status.BLOCKSTUN && f.stunElapsed == 0) {
                            quieto[s] = 0;
                            ganhar(engine, s, DEFESA_PERFEITA, voz);
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        for (int s = 0; s < 2; s++) {
            CombatFighter f = engine.fighter(s);
            // Golpe novo (qualquer um, mesmo no ar): atacou, não está frio.
            boolean golpeNovo = f.attacking() && f.attack != ultimoGolpe[s];
            if (golpeNovo || f.status == CombatFighter.Status.THROW) quieto[s] = 0;
            if (golpeNovo && f.attack.strength == AttackDefinition.Strength.SUPER && f.status != CombatFighter.Status.ULTRA) {
                ganhar(engine, s, SUPER, voz);
            }
            boolean ultra = f.status == CombatFighter.Status.ULTRA;
            if (ultra && !emUltra[s]) {
                quieto[s] = 0;
                ganhar(engine, s, f.ultraShinkai ? ULTRA_SHINKAI : ULTRA, voz);
            }
            boolean shinkai = engine.team(s).pointState().inShinkai();
            if (shinkai && !emShinkai[s]) {
                quieto[s] = 0;
                ganhar(engine, s, SHINKAI, voz);
            }
            // Combo que fechou contra o outro lado.
            ComboSession sessao = engine.lastSession(1 - s);
            if (sessao != null && sessao != ultimaSessao[1 - s]) {
                ultimaSessao[1 - s] = sessao;
                if (sessao.hitCount >= NarradorLuta.COMBO && sessao.attacker >= 0 && sessao.attacker <= 1) {
                    long por = sessao.hitCount >= NarradorLuta.COMBO_GRANDE ? COMBO_GRANDE_POR_ACERTO : COMBO_POR_ACERTO;
                    ganhar(engine, sessao.attacker, por * sessao.hitCount, voz);
                }
            }
            // K.O. do titular do outro lado.
            CombatFighter rival = engine.fighter(1 - s);
            if (rival.ko() && !caido[1 - s]) ganhar(engine, s, KO, voz);
        }

        // Quem esfria: parado, recuando ou defendendo (apanhando não conta).
        for (int s = 0; s < 2; s++) {
            CombatFighter f = engine.fighter(s), rival = engine.fighter(1 - s);
            boolean livre = f.canAct() || f.status == CombatFighter.Status.BLOCKSTUN;
            boolean avancou = !Float.isNaN(ultimoX[s]) && (f.x - ultimoX[s]) * Math.signum(rival.x - f.x) > 0.8f;
            if (livre && !avancou && !f.ko()) quieto[s]++;
            if (quieto[s] < ESFRIA) {
                avisouEsfriando[s] = avisouCaindo[s] = false;
            } else if (!avisouEsfriando[s]) {
                avisouEsfriando[s] = true;
                voz.narrar("aposta_esfriando", vars("nome", nome(engine, s)));
            }
            if (quieto[s] >= CAI) {
                if (!avisouCaindo[s]) {
                    avisouCaindo[s] = true;
                    voz.narrar("aposta_caindo", vars("nome", nome(engine, s)));
                }
                long sai = Math.round(dinheiro[s] * DRENO_POR_SEGUNDO / 60.0);
                dinheiro[s] = Math.max(0L, dinheiro[s] - Math.max(1L, sai));
            }
        }

        // A favorita virou?
        int agora = odd[0] + FAVORITA < odd[1] ? 0 : odd[1] + FAVORITA < odd[0] ? 1 : -1;
        if (agora >= 0) {
            if (favorita >= 0 && agora != favorita) voz.narrar("aposta_virada", vars("nome", nome(engine, agora)));
            favorita = agora;
        }
        lembrar(engine);
    }

    /** O lado {@code side} ganha {@code base} vezes o multiplicador da odd dele; paga o patamar. */
    void ganhar(CombatEngine engine, int side, long base, NarradorLuta.Voz voz) {
        if (base <= 0) return;
        long valor = Math.round(base * multiplicador(side) / 1000.0) * 1000L;
        if (valor <= 0) return;
        dinheiro[side] += valor;
        ultimoGanho[side] = ganhoIdade[side] < 40 ? ultimoGanho[side] + valor : valor;
        ganhoIdade[side] = 0;
        if (dinheiro[side] <= recorde[side]) return;
        recorde[side] = dinheiro[side];
        int devidos = (int)((recorde[side] - INICIAL) / PATAMAR);
        if (devidos <= patamares[side]) return;
        int novos = devidos - patamares[side];
        patamares[side] = devidos;
        patamarIdade[side] = 0;
        FighterState titular = engine.team(side).pointState();
        if (titular != null && titular.life > 0) titular.addSuperMeter(SUPER_DO_PATAMAR * novos);
        voz.narrar("aposta_patamar", vars("nome", nome(engine, side), "valor", formatar(dinheiro[side])));
    }

    private void calcularOdds(CombatEngine engine) {
        double v0 = vida(engine, 0), v1 = vida(engine, 1);
        double vidaTotal = v0 + v1;
        long total = dinheiro[0] + dinheiro[1];
        for (int s = 0; s < 2; s++) {
            double vida = vidaTotal <= 0 ? 0.5 : (s == 0 ? v0 : v1) / vidaTotal;
            double grana = total <= 0 ? 0.5 : dinheiro[s] / (double)total;
            double favor = Math.max(0.01, PESO_VIDA * vida + (1.0 - PESO_VIDA) * grana);
            odd[s] = Math.max(ODD_MIN, Math.min(ODD_MAX, MARGEM / favor));
        }
    }

    /** Vida da dupla inteira (0 a 1). */
    private static double vida(CombatEngine engine, int side) {
        long vida = 0, max = 0;
        FighterState[] membros = engine.team(side).members;
        if (membros == null) return 0.5;
        for (FighterState m : membros) {
            vida += Math.max(0, m.life);
            max += m.profile.maxLife;
        }
        return max <= 0 ? 0.0 : vida / (double)max;
    }

    private void lembrar(CombatEngine engine) {
        for (int s = 0; s < 2; s++) {
            CombatFighter f = engine.fighter(s);
            ultimoGolpe[s] = f.attacking() ? f.attack : null;
            emUltra[s] = f.status == CombatFighter.Status.ULTRA;
            emShinkai[s] = engine.team(s).pointState().inShinkai();
            caido[s] = f.ko();
            ultimoX[s] = f.x;
            if (ultimaSessao[s] == null) ultimaSessao[s] = engine.lastSession(s);
        }
    }

    private String nome(CombatEngine engine, int side) {
        return nomes.apply(engine.team(side).pointState().character);
    }

    private static Map<String, String> vars(String... pares) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i + 1 < pares.length; i += 2) m.put(pares[i], pares[i + 1]);
        return m;
    }

    /** "¥ 1.250.000". */
    static String formatar(long valor) {
        String n = Long.toString(Math.max(0L, valor));
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < n.length(); i++) {
            if (i > 0 && (n.length() - i) % 3 == 0) out.append('.');
            out.append(n.charAt(i));
        }
        return "¥ " + out;
    }

    /** "x2.4". */
    static String formatarOdd(double odd) {
        int dez = (int)Math.round(odd * 10.0);
        return "x" + (dez / 10) + "." + (dez % 10);
    }

    // ------------------------------------------------------------ desenho (1280x720)

    static final float CENTRO = 640f, TOPO = 178f, LARGURA = 330f, ALTURA_BARRA = 12f;
    private static final int INK = 0xFF0B0E16, OURO = 0xFFFFD54A, OURO_ESCURO = 0xFF8A6A12;
    private static final int AZUL = 0xFF2F7BE0, VERMELHO = 0xFFD8343C, FRIO = 0xFF7FD3FF, CAI_COR = 0xFFFF5A5A;

    /** A placa da bolsa embaixo do relógio: o dinheiro e a odd de cada dupla e a barra. */
    void desenhar(RenderCanvas c, float tempo) {
        float l = CENTRO - LARGURA * 0.5f, r = CENTRO + LARGURA * 0.5f;
        float t = TOPO, b = TOPO + 50f;
        float[] placa = {l + 10f, t, r - 10f, t, r, b, l, b};
        c.fillPolygon(new float[] {l + 14f, t + 4f, r - 6f, t + 4f, r + 4f, b + 4f, l + 4f, b + 4f}, 0x66000000);
        c.fillPolygon(placa, 0xE0101420);
        c.strokePolygon(placa, 2f, OURO_ESCURO);
        // Título no meio, uma moedinha de cada lado.
        c.drawText("APOSTAS", CENTRO, t + 14f, 11f, OURO, INK, 2f, false);
        // Dinheiro de cada lado.
        for (int s = 0; s < 2; s++) {
            String valor = formatar(dinheiro[s]);
            float size = 15f;
            float w = c.measureText(valor, size, false);
            float cx = s == 0 ? l + 16f + w * 0.5f : r - 16f - w * 0.5f;
            int cor = caindo(s) ? (((int)(tempo * 4f)) % 2 == 0 ? CAI_COR : 0xFFFFFFFF)
                : quieto[s] >= ESFRIA ? FRIO : 0xFFFFFFFF;
            c.drawText(valor, cx, t + 17f, size, cor, INK, 2.5f, false);
            String o = formatarOdd(odd[s]);
            float ow = c.measureText(o, 12f, false);
            float ox = s == 0 ? l + 16f + ow * 0.5f : r - 16f - ow * 0.5f;
            c.drawText(o, ox, b - 4f, 12f, OURO, INK, 2f, false);
        }
        // A barra de cabo de guerra.
        float bl = l + 60f, br = r - 60f, bt = t + 23f, bb = bt + ALTURA_BARRA;
        c.fillRect(bl - 2f, bt - 2f, br + 2f, bb + 2f, INK);
        float meio = bl + (br - bl) * parcela(0);
        c.fillVerticalGradient(bl, bt, meio, bb, 0xFF6FB0FF, AZUL);
        c.fillVerticalGradient(meio, bt, br, bb, 0xFFFF8A8A, VERMELHO);
        c.fillRect(meio - 1.5f, bt - 3f, meio + 1.5f, bb + 3f, OURO);
        c.fillCircle(meio, (bt + bb) * 0.5f, 5f, OURO);
        c.strokeCircle(meio, (bt + bb) * 0.5f, 5f, 1.5f, INK);
        // Ganho recente ("+¥ ...") subindo do lado de quem ganhou; o patamar vira +SUPER.
        for (int s = 0; s < 2; s++) {
            float lx = s == 0 ? l + 40f : r - 40f;
            if (ganhoIdade[s] < 60 && ultimoGanho[s] > 0) {
                float k = ganhoIdade[s] / 60f;
                int alfa = (int)(255 * (1f - k * k));
                c.drawText("+" + formatar(ultimoGanho[s]), lx, b + 18f - 10f * k, 14f, (alfa << 24) | (OURO & 0xFFFFFF),
                    (alfa << 24) | (INK & 0xFFFFFF), 2.5f, false);
            }
            if (patamarIdade[s] < 90) {
                float k = patamarIdade[s] / 90f;
                float pulso = 1f + 0.25f * Math.max(0f, 1f - k * 5f);
                int alfa = (int)(255 * Math.min(1f, (1f - k) * 2f));
                c.save();
                c.scale(pulso, pulso, lx, b + 38f);
                c.drawText("+SUPER", lx, b + 38f, 17f, (alfa << 24) | 0xFFE066, (alfa << 24) | (INK & 0xFFFFFF), 3f, true);
                c.restore();
            } else if (quieto[s] >= ESFRIA && ganhoIdade[s] >= 60) {
                String aviso = caindo(s) ? "APOSTAS CAINDO ▼" : "ESFRIANDO";
                c.drawText(aviso, lx + (s == 0 ? 30f : -30f), b + 18f, 12f, caindo(s) ? CAI_COR : FRIO, INK, 2f, false);
            }
        }
    }

    /** O letreiro do telão: as odds ao vivo. */
    String letreiro() {
        return "ODDS  " + formatarOdd(odd[0]) + "  |  " + formatarOdd(odd[1]);
    }
}
