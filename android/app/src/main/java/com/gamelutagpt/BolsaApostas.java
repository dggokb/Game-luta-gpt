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
        rosto = rostoQuadros = quedaTimer = 0;
        java.util.Arrays.fill(fViva, false);
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
        animar();
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
            if (favorita >= 0 && agora != favorita) {
                efeitoDaVirada();
                voz.narrar("aposta_virada", vars("nome", nome(engine, agora)));
            }
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
        boolean patamar = pagarPatamar(engine, side, voz);
        efeitoDoGanho(side, valor, patamar);
    }

    /** Passou do maior valor e de um patamar novo: barra de Super para o titular. */
    private boolean pagarPatamar(CombatEngine engine, int side, NarradorLuta.Voz voz) {
        if (dinheiro[side] <= recorde[side]) return false;
        recorde[side] = dinheiro[side];
        int devidos = (int)((recorde[side] - INICIAL) / PATAMAR);
        if (devidos <= patamares[side]) return false;
        int novos = devidos - patamares[side];
        patamares[side] = devidos;
        patamarIdade[side] = 0;
        FighterState titular = engine.team(side).pointState();
        if (titular != null && titular.life > 0) titular.addSuperMeter(SUPER_DO_PATAMAR * novos);
        voz.narrar("aposta_patamar", vars("nome", nome(engine, side), "valor", formatar(dinheiro[side])));
        return true;
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

    /** Pasta da arte (montada por tools/hud/montar_apostas.py a partir das imagens do Codex). */
    static final String PASTA = "ui/hud/apostas/";
    /** A placa na tela: centro, topo (logo abaixo do MENU) e largura. */
    static final float CENTRO = 640f, TOPO = 166f, LARGURA = 500f;
    /** A pilha de fichas de cada lado: de 2 a {@link #FICHAS_MAX} fichas, pela parte da bolsa. */
    static final int FICHAS_MAX = 6;
    private static final int INK = 0xFF0B0E16, OURO = 0xFFFFD54A, FRIO = 0xFF8FE3FF, CAI_COR = 0xFFFF5A5A;
    private static final String[] PECAS = {"placa", "pilha_azul", "pilha_vermelha", "ficha_azul", "ficha_vermelha",
        "moeda", "voa_azul", "voa_vermelha", "brilho"};
    private static final String[] ROSTOS = {"normal", "empolgado", "gargalhada"};

    private final Map<String, com.gamelutagpt.render.RenderImage> arte = new HashMap<>();
    private boolean carregada;
    /** Medidas da placa.png (placa.json): o hexágono, os medidores e as placas, em pixels dela. */
    private float largura = 1000f, altura = 218f;
    private float[] hexagono;
    private final float[][] medidor = new float[2][], dinheiroCaixa = new float[2][], oddCaixa = new float[2][];

    /** O rosto do Apostador no medalhão: qual e por quantos quadros ainda. */
    private int rosto, rostoQuadros;

    /** Fichas voando (do medalhão para a pilha), caindo (de quem esfriou) e a moeda do patamar. */
    private static final int MAX_FICHAS = 24;
    private final float[] fx = new float[MAX_FICHAS], fy = new float[MAX_FICHAS], fvx = new float[MAX_FICHAS],
        fvy = new float[MAX_FICHAS], fIdade = new float[MAX_FICHAS], fVida = new float[MAX_FICHAS];
    /** 0 voando para a pilha, 1 caindo, 2 a moeda indo para a barra de Super. */
    private final int[] fTipo = new int[MAX_FICHAS], fLado = new int[MAX_FICHAS];
    private final boolean[] fViva = new boolean[MAX_FICHAS];
    private int quedaTimer;

    /**
     * Abre a arte (a placa, as fichas e os rostos do Apostador). Sem ela, a placa sai no
     * desenho simples de antes. Chame uma vez, fora do desenho.
     */
    void carregar(com.gamelutagpt.render.RenderAssets assets) {
        if (carregada) return;
        carregada = true;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>)com.gamelutagpt.render.MiniJson.parse(
                assets.readText(PASTA + "placa.json"));
            largura = ((Number)m.get("largura")).floatValue();
            altura = ((Number)m.get("altura")).floatValue();
            hexagono = pontos(m.get("hexagono"));
            for (int s = 0; s < 2; s++) {
                medidor[s] = pontos(((java.util.List<?>)m.get("medidor")).get(s));
                dinheiroCaixa[s] = numeros(((java.util.List<?>)m.get("dinheiro")).get(s));
                oddCaixa[s] = numeros(((java.util.List<?>)m.get("odd")).get(s));
            }
            for (String p : PECAS) arte.put(p, assets.loadImage(PASTA + p + ".png"));
            for (String r : ROSTOS) {
                arte.put("rosto_" + r, assets.loadImage("historia/retratos/apostador_" + r + ".png"));
            }
        } catch (java.io.IOException | RuntimeException ex) {
            arte.clear();
        }
    }

    /** [[x, y], ...] → {x, y, x, y, ...}. */
    private static float[] pontos(Object lista) {
        java.util.List<?> l = (java.util.List<?>)lista;
        float[] out = new float[l.size() * 2];
        for (int i = 0; i < l.size(); i++) {
            java.util.List<?> p = (java.util.List<?>)l.get(i);
            out[i * 2] = ((Number)p.get(0)).floatValue();
            out[i * 2 + 1] = ((Number)p.get(1)).floatValue();
        }
        return out;
    }

    private static float[] numeros(Object lista) {
        java.util.List<?> l = (java.util.List<?>)lista;
        float[] out = new float[l.size()];
        for (int i = 0; i < l.size(); i++) out[i] = ((Number)l.get(i)).floatValue();
        return out;
    }

    boolean temArte() { return arte.containsKey("placa"); }

    /** A escala da placa.png para a tela virtual. */
    private float escala() { return LARGURA / largura; }

    /** O centro da pilha de fichas do lado (em pixels da placa.png): na ponta de fora da placa do dinheiro. */
    private float pilhaX(int s) { return s == 0 ? dinheiroCaixa[0][0] + 40f : dinheiroCaixa[1][2] - 40f; }

    private float pilhaBase(int s) { return dinheiroCaixa[s][3] - 2f; }

    /** Fichas na pilha: de 2 a {@link #FICHAS_MAX}, pela parte da bolsa. */
    int fichasNaPilha(int s) {
        return Math.max(2, Math.min(FICHAS_MAX, Math.round(parcela(s) * FICHAS_MAX * 1.2f)));
    }

    /** Anda as fichas e o rosto (um quadro da luta). */
    private void animar() {
        if (rostoQuadros > 0 && --rostoQuadros == 0) rosto = 0;
        for (int i = 0; i < MAX_FICHAS; i++) {
            if (!fViva[i]) continue;
            fIdade[i] += 1f / 60f;
            if (fIdade[i] >= fVida[i]) {
                fViva[i] = false;
                continue;
            }
            if (fTipo[i] == 1) fvy[i] += 900f / 60f;
            fx[i] += fvx[i] / 60f;
            fy[i] += fvy[i] / 60f;
        }
        // Quem está perdendo aposta solta uma ficha da pilha de tempos em tempos.
        if (++quedaTimer >= 14) {
            quedaTimer = 0;
            for (int s = 0; s < 2; s++) {
                if (!caindo(s) || dinheiroCaixa[s] == null) continue;
                float dir = s == 0 ? -1f : 1f;
                soltar(1, s, pilhaX(s), pilhaBase(s) - 30f, dir * (60f + (quadro % 5) * 25f), -180f, 0.9f);
            }
        }
    }

    private void soltar(int tipo, int lado, float x, float y, float vx, float vy, float vida) {
        for (int i = 0; i < MAX_FICHAS; i++) {
            if (fViva[i]) continue;
            fViva[i] = true;
            fTipo[i] = tipo;
            fLado[i] = lado;
            fx[i] = x;
            fy[i] = y;
            fvx[i] = vx;
            fvy[i] = vy;
            fIdade[i] = 0f;
            fVida[i] = vida;
            return;
        }
    }

    /** O dinheiro entrou: fichas saem do medalhão para a pilha; o rosto reage. */
    private void efeitoDoGanho(int side, long valor, boolean patamar) {
        if (dinheiroCaixa[side] == null) return;
        float cx = largura * 0.5f, cy = altura * 0.45f;
        int n = (int)Math.max(1, Math.min(5, valor / 80_000L));
        float vida = 0.45f;
        for (int k = 0; k < n; k++) {
            float tx = pilhaX(side) + (k - n * 0.5f) * 6f, ty = pilhaBase(side) - 40f;
            // Velocidade para chegar na pilha em "vida" segundos, partindo um pouco atrasada.
            soltar(0, side, cx, cy - k * 6f, (tx - cx) / vida, (ty - cy) / vida - 40f * k, vida);
        }
        if (patamar) {
            // A moeda vai da pilha até a barra de Super do HUD (em pixels da placa).
            float s = escala();
            float alvoX = ((side == 0 ? 490f : 790f) - (CENTRO - LARGURA * 0.5f)) / s;
            float alvoY = (113f - TOPO) / s;
            float x0 = pilhaX(side), y0 = pilhaBase(side) - 50f;
            soltar(2, side, x0, y0, (alvoX - x0) / 0.7f, (alvoY - y0) / 0.7f, 0.7f);
            rosto = 2;
            rostoQuadros = 120;
        } else if (valor >= 150_000L && rostoQuadros < 60) {
            rosto = 1;
            rostoQuadros = 60;
        }
    }

    /** A virada das odds: o Apostador gargalha. */
    private void efeitoDaVirada() {
        rosto = 2;
        rostoQuadros = 120;
    }

    /** A placa da bolsa embaixo do MENU: o rosto do Apostador, o dinheiro, as odds e as fichas. */
    void desenhar(RenderCanvas c, float tempo) {
        if (!temArte()) {
            desenharSimples(c, tempo);
            return;
        }
        float s = escala();
        c.save();
        c.translate(CENTRO - LARGURA * 0.5f, TOPO);
        c.scale(s, s, 0f, 0f);
        desenharMedalhao(c, tempo);
        for (int lado = 0; lado < 2; lado++) desenharMedidor(c, lado, tempo);
        c.drawImage(arte.get("placa"), 0f, 0f, largura, altura, 255);
        for (int lado = 0; lado < 2; lado++) desenharLado(c, lado, tempo);
        desenharFichas(c);
        c.restore();
    }

    private void desenharMedalhao(RenderCanvas c, float tempo) {
        float[] caixa = caixa(hexagono);
        c.save();
        c.clipPolygon(hexagono);
        c.fillVerticalGradient(caixa[0], caixa[1], caixa[2], caixa[3], 0xFF2A1840, 0xFF0B0E1C);
        // Raios dourados girando devagar atrás do rosto (mais forte quando ele gargalha).
        float cx = (caixa[0] + caixa[2]) * 0.5f, cy = (caixa[1] + caixa[3]) * 0.5f;
        float raio = (caixa[3] - caixa[1]) * 0.9f;
        int alfa = rosto == 2 ? 0x70 : 0x30;
        for (int k = 0; k < 12; k++) {
            double a = Math.toRadians(k * 30 + tempo * 12f), b = a + Math.toRadians(9);
            c.fillPolygon(new float[] {cx, cy, cx + (float)Math.cos(a) * raio, cy + (float)Math.sin(a) * raio,
                cx + (float)Math.cos(b) * raio, cy + (float)Math.sin(b) * raio}, (alfa << 24) | 0xFFD54A);
        }
        com.gamelutagpt.render.RenderImage img = arte.get("rosto_" + ROSTOS[rosto]);
        if (img != null) {
            float w = caixa[2] - caixa[0], h = caixa[3] - caixa[1];
            float esc = Math.max(w / img.width(), h / img.height()) * 1.04f;
            float iw = img.width() * esc, ih = img.height() * esc;
            // Gargalhando ele treme um pouco.
            float tremor = rosto == 2 ? (float)Math.sin(tempo * 55f) * 2.5f : 0f;
            float x = caixa[0] + (w - iw) * 0.5f + tremor, y = caixa[1] + (h - ih) * 0.2f;
            c.drawImage(img, x, y, x + iw, y + ih, 255);
        }
        c.restore();
    }

    private void desenharMedidor(RenderCanvas c, int lado, float tempo) {
        float[] poly = medidor[lado];
        float[] caixa = caixa(poly);
        c.save();
        c.clipPolygon(poly);
        c.fillRect(caixa[0], caixa[1], caixa[2], caixa[3], 0xFF070A14);
        // Enche da ponta de fora para o medalhão, pela parte da bolsa.
        float w = caixa[2] - caixa[0];
        float cheio = w * Math.max(0.03f, Math.min(1f, parcela(lado)));
        float l = lado == 0 ? caixa[0] : caixa[2] - cheio, r = lado == 0 ? caixa[0] + cheio : caixa[2];
        boolean frio = quieto[lado] >= ESFRIA;
        int cima, baixo;
        if (caindo(lado) && ((int)(tempo * 4f)) % 2 == 0) {
            cima = 0xFFFF8A8A; baixo = 0xFFB0202A;
        } else if (frio) {
            cima = 0xFFCFF3FF; baixo = 0xFF4FA8D8;
        } else if (lado == 0) {
            cima = 0xFF7FC0FF; baixo = 0xFF1C5BC8;
        } else {
            cima = 0xFFFF9090; baixo = 0xFFC4222C;
        }
        c.fillVerticalGradient(l, caixa[1], r, caixa[3], cima, baixo);
        // Listras brancas de ficha andando devagar.
        float h = caixa[3] - caixa[1];
        float passo = h * 1.6f, desloca = (tempo * 40f) % passo;
        for (float x = l - passo + (lado == 0 ? desloca : -desloca); x < r; x += passo) {
            c.fillPolygon(new float[] {x, caixa[3], x + h * 0.5f, caixa[1], x + h * 0.9f, caixa[1], x + h * 0.4f, caixa[3]},
                0x38FFFFFF);
        }
        // A ponta que avança brilha.
        float ponta = lado == 0 ? r : l;
        c.fillRect(ponta - 3f, caixa[1], ponta + 3f, caixa[3], 0xDDFFF4C0);
        c.fillRect(caixa[0], caixa[1], caixa[2], caixa[1] + h * 0.18f, 0x30FFFFFF);
        c.restore();
    }

    private void desenharLado(RenderCanvas c, int lado, float tempo) {
        float[] d = dinheiroCaixa[lado], o = oddCaixa[lado];
        boolean frio = quieto[lado] >= ESFRIA;
        // O gelo de quem esfriou cobre a placa do dinheiro.
        if (frio) {
            float k = Math.min(1f, (quieto[lado] - ESFRIA) / 40f);
            c.fillRect(d[0], d[1], d[2], d[3], ((int)(0x55 * k) << 24) | 0x7FD8FF);
        }
        // A pilha de fichas na ponta de fora (cresce com a parte da bolsa).
        com.gamelutagpt.render.RenderImage pilha = arte.get(lado == 0 ? "pilha_azul" : "pilha_vermelha");
        float pw = 62f, ph = pw * pilha.height() / pilha.width();
        float porFicha = ph / 6.6f;
        int n = fichasNaPilha(lado);
        float visivel = porFicha * (n + 0.6f);
        float px = pilhaX(lado), base = pilhaBase(lado);
        // Recorte de baixo da pilha: as fichas de cima somem quando a parte é pequena.
        float srcTop = pilha.height() * (1f - visivel / ph);
        c.drawImageRegion(pilha, 0f, Math.max(0f, srcTop), pilha.width(), pilha.height(),
            px - pw * 0.5f, base - visivel, px + pw * 0.5f, base, 255);
        if (patamarIdade[lado] < 60) {
            com.gamelutagpt.render.RenderImage brilho = arte.get("brilho");
            float k = patamarIdade[lado] / 60f, tam = 70f * (0.6f + 0.6f * k);
            int alfa = (int)(255 * (1f - k));
            c.drawImage(brilho, px - tam * 0.5f, base - visivel - tam * 0.6f, px + tam * 0.5f, base - visivel + tam * 0.4f, alfa);
        }
        // O dinheiro, centrado no resto da placa.
        float tl = lado == 0 ? px + 34f : d[0], tr = lado == 0 ? d[2] : px - 34f;
        int cor = caindo(lado) ? (((int)(tempo * 4f)) % 2 == 0 ? CAI_COR : 0xFFFFFFFF) : frio ? FRIO : 0xFFFFF4D6;
        float tam = (d[3] - d[1]) * 0.78f;
        c.drawText(formatar(dinheiro[lado]), (tl + tr) * 0.5f, d[3] - (d[3] - d[1] - tam * 0.72f) * 0.5f - 1f,
            tam, cor, INK, 5f, false);
        // A odd na plaquinha.
        float ot = (o[3] - o[1]) * 1.05f;
        c.drawText(formatarOdd(odd[lado]), (o[0] + o[2]) * 0.5f, o[3] - (o[3] - o[1] - ot * 0.72f) * 0.5f, ot,
            OURO, INK, 4f, false);
        // Embaixo da placa: o ganho subindo, o +SUPER e os avisos de quem esfria.
        float lx = (d[0] + d[2]) * 0.5f, baixo = altura + 30f;
        if (ganhoIdade[lado] < 60 && ultimoGanho[lado] > 0) {
            float k = ganhoIdade[lado] / 60f;
            int alfa = (int)(255 * (1f - k * k));
            c.drawText("+" + formatar(ultimoGanho[lado]), lx, baixo - 24f * k, 30f, (alfa << 24) | (OURO & 0xFFFFFF),
                (alfa << 24) | (INK & 0xFFFFFF), 6f, false);
        }
        if (patamarIdade[lado] < 90) {
            float k = patamarIdade[lado] / 90f;
            float pulso = 1f + 0.3f * Math.max(0f, 1f - k * 5f);
            int alfa = (int)(255 * Math.min(1f, (1f - k) * 2f));
            c.save();
            c.scale(pulso, pulso, lx, baixo + 34f);
            c.drawText("+SUPER", lx, baixo + 34f, 40f, (alfa << 24) | 0xFFE066, (alfa << 24) | (INK & 0xFFFFFF), 7f, true);
            c.restore();
        } else if (frio && ganhoIdade[lado] >= 60) {
            String aviso = caindo(lado) ? "APOSTAS CAINDO ▼" : "ESFRIANDO";
            c.drawText(aviso, lx, baixo, 28f, caindo(lado) ? CAI_COR : FRIO, INK, 6f, false);
        }
    }

    private void desenharFichas(RenderCanvas c) {
        for (int i = 0; i < MAX_FICHAS; i++) {
            if (!fViva[i]) continue;
            float k = fIdade[i] / fVida[i];
            com.gamelutagpt.render.RenderImage img;
            float tam;
            int alfa = 255;
            if (fTipo[i] == 2) {
                img = arte.get("moeda");
                tam = 44f + 20f * (float)Math.sin(k * Math.PI);
            } else if (fTipo[i] == 1) {
                img = arte.get(fLado[i] == 0 ? "ficha_azul" : "ficha_vermelha");
                tam = 30f;
                alfa = (int)(255 * (1f - k * k));
            } else {
                img = arte.get(fLado[i] == 0 ? "voa_azul" : "voa_vermelha");
                tam = 64f;
            }
            float w = tam, h = tam * img.height() / img.width();
            c.save();
            // A ficha voando aponta para onde vai (a arte voa para a direita).
            if (fTipo[i] == 0 && fvx[i] < 0f) c.scale(-1f, 1f, fx[i], fy[i]);
            if (fTipo[i] == 1) c.rotate(fIdade[i] * 540f * (fLado[i] == 0 ? -1f : 1f), fx[i], fy[i]);
            c.drawImage(img, fx[i] - w * 0.5f, fy[i] - h * 0.5f, fx[i] + w * 0.5f, fy[i] + h * 0.5f, alfa);
            c.restore();
        }
    }

    private static float[] caixa(float[] poly) {
        float l = Float.MAX_VALUE, t = Float.MAX_VALUE, r = -Float.MAX_VALUE, b = -Float.MAX_VALUE;
        for (int i = 0; i < poly.length; i += 2) {
            l = Math.min(l, poly[i]);
            r = Math.max(r, poly[i]);
            t = Math.min(t, poly[i + 1]);
            b = Math.max(b, poly[i + 1]);
        }
        return new float[] {l, t, r, b};
    }

    /** Sem a arte (testes, arquivo faltando): a placa simples, desenhada. */
    private void desenharSimples(RenderCanvas c, float tempo) {
        float l = CENTRO - 165f, r = CENTRO + 165f, t = TOPO + 8f, b = t + 50f;
        c.fillPolygon(new float[] {l + 10f, t, r - 10f, t, r, b, l, b}, 0xE0101420);
        c.drawText("APOSTAS", CENTRO, t + 14f, 11f, OURO, INK, 2f, false);
        for (int s = 0; s < 2; s++) {
            String valor = formatar(dinheiro[s]);
            float w = c.measureText(valor, 15f, false);
            float cx = s == 0 ? l + 16f + w * 0.5f : r - 16f - w * 0.5f;
            c.drawText(valor, cx, t + 17f, 15f, caindo(s) ? CAI_COR : quieto[s] >= ESFRIA ? FRIO : 0xFFFFFFFF, INK, 2.5f, false);
        }
        float bl = l + 60f, br = r - 60f, bt = t + 23f, bb = bt + 12f;
        float meio = bl + (br - bl) * parcela(0);
        c.fillRect(bl, bt, meio, bb, 0xFF2F7BE0);
        c.fillRect(meio, bt, br, bb, 0xFFD8343C);
    }

    /** O letreiro do telão: as odds ao vivo. */
    String letreiro() {
        return "ODDS  " + formatarOdd(odd[0]) + "  |  " + formatarOdd(odd[1]);
    }
}
