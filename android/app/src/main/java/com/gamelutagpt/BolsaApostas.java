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
    /** Kon por ponto de dano que entra (a vida de um lutador fica perto de 10.000). */
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
    /** Quadros desde o último patamar de cada lado. */
    private final int[] patamarIdade = {999, 999};
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
            patamarIdade[s] = 999;
            ultimoGolpe[s] = null;
            emUltra[s] = emShinkai[s] = caido[s] = false;
            ultimaSessao[s] = null;
            ultimoX[s] = Float.NaN;
        }
        favorita = -1;
        comecou = primeiroGolpe = acabou = false;
        quadro = 0;
        rosto = rostoQuadros = 0;
        for (int i = 0; i < VOOS; i++) vooIdade[i] = -1;
        for (int s = 0; s < 2; s++) {
            mostrado[s] = dinheiro[s];
            subiuIdade[s] = 999;
        }
    }

    // ------------------------------------------------------------ leitura

    long dinheiro(int side) { return dinheiro[side]; }

    /** A odd da dupla (quanto paga cada kon apostado nela). */
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
        // A barra sobe quando a moeda chega nela (o rastro leva o olho até lá).
        voar(side, SUPER_DO_PATAMAR * novos);
        voz.narrar("aposta_patamar", vars("nome", nome(engine, side), "valor", formatarFala(dinheiro[side])));
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

    /**
     * A moeda do SOKON, o kon (o K com a chama; o símbolo é a imagem ui/hud/moeda/kon_*.png,
     * desenhada antes do número: a fonte não tem ele).
     */
    static final String NOME_MOEDA = "kon";

    /** "1.250.000 kon": o valor nas falas (texto, sem o símbolo). */
    static String formatarFala(long valor) {
        return formatar(valor) + " " + NOME_MOEDA;
    }

    /** "1.250.000": o número; no HUD o símbolo do kon vem antes, como imagem. */
    static String formatar(long valor) {
        String n = Long.toString(Math.max(0L, valor));
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < n.length(); i++) {
            if (i > 0 && (n.length() - i) % 3 == 0) out.append('.');
            out.append(n.charAt(i));
        }
        return out.toString();
    }

    /** "x2.4". */
    static String formatarOdd(double odd) {
        int dez = (int)Math.round(odd * 10.0);
        return "x" + (dez / 10) + "." + (dez % 10);
    }

    // ------------------------------------------------------------ desenho (1280x720)

    /** Pasta da arte (montada por tools/hud/montar_apostas.py a partir das imagens do Codex). */
    static final String PASTA = "ui/hud/apostas/";
    /** O placar na tela: largura e o centro (no celular, o vão entre o direcional e os botões). */
    static final float LARGURA = 450f, CENTRO_TELA = 640f, CENTRO_TOQUE = 575f;
    /** A faixa da borda de baixo, de ponta a ponta. */
    static final float FAIXA_ALTURA = 32f;
    /** O quanto a caixa do narrador sobe para ficar em cima do placar. */
    static final float NARRADOR_SOBE = 64f;
    /** O centro da barra de Super de cada lado no HUD de cima (HudRenderer.RAIOS_P1/P2). */
    static final float[] SUPER_X = {501f, 876f};
    static final float SUPER_Y = 114f;
    /** O voo da moeda até a barra de Super: quadros até chegar, o atraso do fim do rastro e o estouro. */
    static final int VOO_QUADROS = 36, RABO_QUADROS = 14, CHEGADA_QUADROS = 24;
    private static final int VOOS = 6;
    private static final int INK = 0xFF0B0E16, OURO = 0xFFFFD54A, FRIO = 0xFF8FE3FF, CAI_COR = 0xFFFF5A5A;
    private static final int AZUL_LUZ = 0xFF6FC3FF, VERMELHO_LUZ = 0xFFFF6A6A, SOBE = 0xFF4CE07A;
    private static final String[] PECAS = {"letreiro", "faixa", "rastro", "chegada", "moeda_luz"};
    private static final String[] ROSTOS = {"normal", "empolgado", "gargalhada"};
    /** As cores do símbolo do kon (tools/hud/montar_moeda.py), uma por cor de número. */
    private static final String[] CORES_KON = {"branco", "azul", "vermelho", "frio", "cai", "ouro"};

    private final Map<String, com.gamelutagpt.render.RenderImage> arte = new HashMap<>();
    private boolean carregada;
    /** Medidas do letreiro.png (letreiro.json). */
    private float largura = 780f, altura = 180f, topoFaixa = 135f;
    private float[] hexagono, barra;
    private final float[][] dinheiroCaixa = new float[2][], oddCaixa = new float[2][];

    /** O rosto do Apostador no medalhão: qual e por quantos quadros ainda. */
    private int rosto, rostoQuadros;
    /** O valor que aparece (corre até o de verdade) e há quantos quadros subiu. */
    private final double[] mostrado = new double[2];
    private final int[] subiuIdade = {999, 999};

    /** Moedas voando até a barra de Super: o lado, a barra que levam e a idade (-1: livre). */
    private final int[] vooLado = new int[VOOS], vooBarra = new int[VOOS], vooIdade = new int[VOOS];
    {
        java.util.Arrays.fill(vooIdade, -1);
    }

    /** Abre a arte (o letreiro, a faixa, o rastro, a moeda e os rostos). Chame uma vez, fora do desenho. */
    void carregar(com.gamelutagpt.render.RenderAssets assets) {
        if (carregada) return;
        carregada = true;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>)com.gamelutagpt.render.MiniJson.parse(
                assets.readText(PASTA + "letreiro.json"));
            largura = ((Number)m.get("largura")).floatValue();
            altura = ((Number)m.get("altura")).floatValue();
            topoFaixa = ((Number)m.get("topoFaixa")).floatValue();
            hexagono = pontos(m.get("hexagono"));
            barra = pontos(m.get("barra"));
            for (int s = 0; s < 2; s++) {
                dinheiroCaixa[s] = numeros(((java.util.List<?>)m.get("dinheiro")).get(s));
                oddCaixa[s] = numeros(((java.util.List<?>)m.get("odd")).get(s));
            }
            for (String p : PECAS) arte.put(p, assets.loadImage(PASTA + p + ".png"));
            for (String cor : CORES_KON) arte.put("kon_" + cor, assets.loadImage("ui/hud/moeda/kon_" + cor + ".png"));
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

    boolean temArte() { return arte.containsKey("letreiro"); }

    /** Moedas a caminho da barra de Super deste lado (testes). */
    int voando(int side) {
        int n = 0;
        for (int i = 0; i < VOOS; i++) if (vooIdade[i] >= 0 && vooIdade[i] < VOO_QUADROS && vooLado[i] == side) n++;
        return n;
    }

    /** Uma moeda sai do placar do lado até a barra de Super dele, levando {@code barra} de Super. */
    private void voar(int side, int barra) {
        int livre = -1;
        for (int i = 0; i < VOOS && livre < 0; i++) if (vooIdade[i] < 0) livre = i;
        if (livre < 0) livre = 0;
        vooLado[livre] = side;
        vooBarra[livre] = barra;
        vooIdade[livre] = 0;
        patamarIdade[side] = 0;
        rosto = 2;
        rostoQuadros = 120;
    }

    private float relogioTela;

    /**
     * O tempo da tela (não o da luta, que fica lento nos impactos): anda o rosto, o valor e as
     * moedas, que são HUD. Chame a cada quadro com os segundos de verdade, enquanto a luta corre.
     */
    void passarTempo(CombatEngine engine, float segundos) {
        relogioTela += Math.max(0f, Math.min(0.1f, segundos)) * 60f;
        while (relogioTela >= 0.999f) {
            relogioTela -= 1f;
            animar(engine);
        }
    }

    /** Anda o rosto, o valor que aparece e as moedas; paga a barra quando a moeda chega. */
    private void animar(CombatEngine engine) {
        if (rostoQuadros > 0 && --rostoQuadros == 0) rosto = 0;
        for (int s = 0; s < 2; s++) {
            if (subiuIdade[s] < 999) subiuIdade[s]++;
            double alvo = dinheiro[s];
            // O valor corre até o de verdade (sobe contando; cai na hora, junto com o dreno).
            mostrado[s] = alvo < mostrado[s] ? alvo : mostrado[s] + Math.max(1000.0, (alvo - mostrado[s]) * 0.12);
            if (mostrado[s] > alvo) mostrado[s] = alvo;
        }
        for (int i = 0; i < VOOS; i++) {
            if (vooIdade[i] < 0) continue;
            vooIdade[i]++;
            if (vooIdade[i] == VOO_QUADROS) {
                FighterState titular = engine.team(vooLado[i]).pointState();
                if (titular != null && titular.life > 0) titular.addSuperMeter(vooBarra[i]);
            }
            if (vooIdade[i] > VOO_QUADROS + Math.max(RABO_QUADROS, CHEGADA_QUADROS)) vooIdade[i] = -1;
        }
    }

    /** O dinheiro entrou: o valor sobe contando e a seta verde aparece; o rosto reage num ganho grande. */
    private void efeitoDoGanho(int side, long valor, boolean patamar) {
        subiuIdade[side] = 0;
        if (!patamar && valor >= 150_000L && rostoQuadros < 60) {
            rosto = 1;
            rostoQuadros = 60;
        }
    }

    /** A virada das odds: o Apostador gargalha. */
    private void efeitoDaVirada() {
        rosto = 2;
        rostoQuadros = 120;
    }

    /**
     * O letreiro do rodapé (o concept M1): a faixa de ponta a ponta e o placar que sobe no meio,
     * centrado em {@code centro} (o vão entre o direcional e os botões no celular); e as moedas
     * voando até a barra de Super, com o rastro de luz.
     */
    void desenhar(RenderCanvas c, float tempo, float centro) {
        if (!temArte()) {
            desenharSimples(c);
            return;
        }
        com.gamelutagpt.render.RenderImage faixa = arte.get("faixa");
        float faixaTopo = 720f - FAIXA_ALTURA;
        c.drawImage(faixa, 0f, faixaTopo, 1280f, 720f, 255);
        float s = LARGURA / largura;
        float topo = faixaTopo + 2f - topoFaixa * s, esquerda = centro - LARGURA * 0.5f;
        // O rótulo AO VIVO · APOSTAS na faixa, à esquerda do placar.
        float rotuloX = esquerda - 8f, rotuloY = 715f;
        String rotulo = "APOSTAS";
        float rw = c.measureText(rotulo, 12f, false);
        c.drawText(rotulo, rotuloX - rw * 0.5f, rotuloY, 12f, 0xFFEFE6C8, INK, 2f, false);
        String vivo = "AO VIVO";
        float vw = c.measureText(vivo, 12f, false);
        float vx = rotuloX - rw - 12f - vw * 0.5f;
        c.drawText(vivo, vx, rotuloY, 12f, 0xFFFFFFFF, INK, 2f, false);
        if (((int)(tempo * 2f)) % 2 == 0) c.fillCircle(vx - vw * 0.5f - 8f, rotuloY - 4.5f, 4f, 0xFFE53935);

        c.save();
        c.translate(esquerda, topo);
        c.scale(s, s, 0f, 0f);
        desenharMedalhao(c, tempo);
        desenharBarra(c, tempo);
        c.drawImage(arte.get("letreiro"), 0f, 0f, largura, altura, 255);
        for (int lado = 0; lado < 2; lado++) desenharLado(c, lado, tempo, s);
        c.restore();
        ultimaEsquerda = esquerda;
        ultimoTopo = topo;
        ultimaEscala = s;
    }

    /** Onde o placar foi desenhado por último (as moedas saem dele). */
    private float ultimaEsquerda = CENTRO_TELA - LARGURA * 0.5f, ultimoTopo = 600f, ultimaEscala = 0.577f;

    /** As moedas voando até a barra de Super, por cima de tudo (chame depois dos controles). */
    void desenharVoos(RenderCanvas c) {
        if (temArte()) desenharVoos(c, ultimaEsquerda, ultimoTopo, ultimaEscala);
    }

    private void desenharMedalhao(RenderCanvas c, float tempo) {
        float[] caixa = caixa(hexagono);
        c.save();
        c.clipPolygon(hexagono);
        c.fillVerticalGradient(caixa[0], caixa[1], caixa[2], caixa[3], 0xFF2A1840, 0xFF0B0E1C);
        if (rosto == 2) {
            float cx = (caixa[0] + caixa[2]) * 0.5f, cy = (caixa[1] + caixa[3]) * 0.5f;
            float raio = (caixa[3] - caixa[1]) * 0.9f;
            for (int k = 0; k < 12; k++) {
                double a = Math.toRadians(k * 30 + tempo * 40f), b = a + Math.toRadians(9);
                c.fillPolygon(new float[] {cx, cy, cx + (float)Math.cos(a) * raio, cy + (float)Math.sin(a) * raio,
                    cx + (float)Math.cos(b) * raio, cy + (float)Math.sin(b) * raio}, 0x66FFD54A);
            }
        }
        com.gamelutagpt.render.RenderImage img = arte.get("rosto_" + ROSTOS[rosto]);
        if (img != null) {
            float w = caixa[2] - caixa[0], h = caixa[3] - caixa[1];
            float esc = Math.max(w / img.width(), h / img.height()) * 1.04f;
            float iw = img.width() * esc, ih = img.height() * esc;
            float tremor = rosto == 2 ? (float)Math.sin(tempo * 55f) * 1.5f : 0f;
            float x = caixa[0] + (w - iw) * 0.5f + tremor, y = caixa[1] + (h - ih) * 0.2f;
            c.drawImage(img, x, y, x + iw, y + ih, 255);
        }
        c.restore();
    }

    /** A barra fina: azul da esquerda e vermelho da direita, encontrando-se na parte de cada um. */
    private void desenharBarra(RenderCanvas c, float tempo) {
        float[] caixa = caixa(barra);
        c.save();
        c.clipPolygon(barra);
        float meio = caixa[0] + (caixa[2] - caixa[0]) * parcela(0);
        c.fillVerticalGradient(caixa[0], caixa[1], meio, caixa[3], 0xFF8FD0FF, 0xFF1C5BC8);
        c.fillVerticalGradient(meio, caixa[1], caixa[2], caixa[3], 0xFFFF9C9C, 0xFFC4222C);
        c.fillRect(meio - 2.5f, caixa[1], meio + 2.5f, caixa[3], 0xFFFFF4C0);
        c.fillRect(caixa[0], caixa[1], caixa[2], caixa[1] + (caixa[3] - caixa[1]) * 0.3f, 0x30FFFFFF);
        c.restore();
    }

    private void desenharLado(RenderCanvas c, int lado, float tempo, float s) {
        float[] d = dinheiroCaixa[lado], o = oddCaixa[lado];
        boolean frio = quieto[lado] >= ESFRIA;
        if (frio) {
            float k = Math.min(1f, (quieto[lado] - ESFRIA) / 40f);
            c.fillRect(d[0] + 6f, d[1] + 4f, d[2] - 6f, d[3] - 4f, ((int)(0x50 * k) << 24) | 0x7FD8FF);
        }
        boolean pisca = caindo(lado) && ((int)(tempo * 4f)) % 2 == 0;
        int cor = caindo(lado) ? (pisca ? CAI_COR : 0xFFFFFFFF) : frio ? FRIO : lado == 0 ? AZUL_LUZ : VERMELHO_LUZ;
        String corKon = caindo(lado) ? (pisca ? "cai" : "branco") : frio ? "frio" : lado == 0 ? "azul" : "vermelho";
        float tam = (d[3] - d[1]) * 0.66f;
        float base = d[3] - (d[3] - d[1] - tam * 0.72f) * 0.5f;
        String valor = formatar(Math.round(mostrado[lado] / 1000.0) * 1000L);
        // O símbolo do kon antes do número, os dois centrados juntos na placa.
        com.gamelutagpt.render.RenderImage kon = arte.get("kon_" + corKon);
        float konAlto = tam * 1.12f, konLargo = kon != null ? konAlto * kon.width() / kon.height() : 0f;
        float largoTexto = c.measureText(valor, tam, false), vao = tam * 0.12f;
        float inicio = (d[0] + d[2]) * 0.5f - 8f - (konLargo + vao + largoTexto) * 0.5f;
        if (kon != null) {
            float ky = base - tam * 0.72f * 0.5f - konAlto * 0.5f;
            c.drawImage(kon, inicio, ky, inicio + konLargo, ky + konAlto, 255);
        }
        float cx = inicio + konLargo + vao + largoTexto * 0.5f;
        // Um halo da cor do time atrás dos dígitos (o letreiro de LED).
        c.drawText(valor, cx, base, tam, cor, (cor & 0xFFFFFF) | 0x40000000, 9f, false);
        c.drawText(valor, cx, base, tam, cor, INK, 3.5f, false);
        // A seta: verde subindo (logo depois de ganhar), vermelha caindo (perdendo aposta).
        float sx = cx + largoTexto * 0.5f + 16f, sy = (d[1] + d[3]) * 0.5f;
        if (caindo(lado)) {
            c.fillPolygon(new float[] {sx - 8f, sy - 6f, sx + 8f, sy - 6f, sx, sy + 8f}, CAI_COR);
        } else if (subiuIdade[lado] < 90) {
            int alfa = (int)(255 * Math.min(1f, (90 - subiuIdade[lado]) / 30f));
            c.fillPolygon(new float[] {sx - 8f, sy + 6f, sx + 8f, sy + 6f, sx, sy - 8f}, (alfa << 24) | (SOBE & 0xFFFFFF));
        }
        float ot = (o[3] - o[1]) * 0.9f;
        c.drawText(formatarOdd(odd[lado]), (o[0] + o[2]) * 0.5f, o[3] - (o[3] - o[1] - ot * 0.72f) * 0.5f, ot,
            OURO, INK, 3f, false);
    }

    /** Onde a moeda sai: o meio do valor do lado, na tela. */
    private float origemX(int lado, float esquerda, float s) {
        float[] d = dinheiroCaixa[lado];
        return esquerda + (d[0] + d[2]) * 0.5f * s;
    }

    private float origemY(int lado, float topo, float s) {
        float[] d = dinheiroCaixa[lado];
        return topo + (d[1] + d[3]) * 0.5f * s;
    }

    private final float[] px = new float[RASTRO_PONTOS + 1], py = new float[RASTRO_PONTOS + 1];
    private static final int RASTRO_PONTOS = 24;
    /** Largura do rastro (px na tela) na ponta e o tamanho da moeda e do estouro. */
    private static final float RASTRO_LARGURA = 48f, MOEDA = 46f, ESTOURO = 130f;

    /**
     * As moedas voando: uma curva do placar até a barra de Super, arqueada para fora. A ponta
     * (a moeda) anda com aceleração; o fim do rastro vem {@link #RABO_QUADROS} atrás e o apaga de
     * trás para a frente. Na chegada, o estouro de luz na barra.
     */
    private void desenharVoos(RenderCanvas c, float esquerda, float topo, float s) {
        com.gamelutagpt.render.RenderImage rastro = arte.get("rastro"), moeda = arte.get("moeda_luz"),
            chegada = arte.get("chegada");
        for (int i = 0; i < VOOS; i++) {
            int idade = vooIdade[i];
            if (idade < 0) continue;
            int lado = vooLado[i];
            float x0 = origemX(lado, esquerda, s), y0 = origemY(lado, topo, s);
            float x2 = SUPER_X[lado], y2 = SUPER_Y;
            // O ponto de controle puxa a curva para fora da tela (o lado do time) e para cima.
            float fora = lado == 0 ? -1f : 1f;
            float x1 = (x0 + x2) * 0.5f + fora * 260f, y1 = (y0 + y2) * 0.5f + 40f;
            float frente = curvaDoVoo(idade / (float)VOO_QUADROS);
            float fundo = curvaDoVoo((idade - RABO_QUADROS) / (float)VOO_QUADROS);
            if (frente > fundo + 0.005f) {
                for (int k = 0; k <= RASTRO_PONTOS; k++) {
                    float t = fundo + (frente - fundo) * k / RASTRO_PONTOS;
                    float u = 1f - t;
                    px[k] = u * u * x0 + 2f * u * t * x1 + t * t * x2;
                    py[k] = u * u * y0 + 2f * u * t * y1 + t * t * y2;
                }
                fita(c, rastro, px, py, RASTRO_PONTOS + 1, RASTRO_LARGURA);
            }
            if (idade < VOO_QUADROS) {
                float t = frente, u = 1f - t;
                float mx = u * u * x0 + 2f * u * t * x1 + t * t * x2, my = u * u * y0 + 2f * u * t * y1 + t * t * y2;
                float tam = MOEDA * (1f + 0.15f * (float)Math.sin(idade * 0.9f));
                float mw = tam, mh = tam * moeda.height() / moeda.width();
                c.drawImage(moeda, mx - mw * 0.5f, my - mh * 0.5f, mx + mw * 0.5f, my + mh * 0.5f, 255);
            } else if (idade - VOO_QUADROS < CHEGADA_QUADROS) {
                float k = (idade - VOO_QUADROS) / (float)CHEGADA_QUADROS;
                float tam = ESTOURO * (0.45f + 0.75f * (float)Math.sqrt(k));
                int alfa = (int)(255 * Math.min(1f, (1f - k) * 1.6f));
                c.save();
                c.rotate(k * 25f * fora, x2, y2);
                c.drawImage(chegada, x2 - tam * 0.5f, y2 - tam * 0.5f, x2 + tam * 0.5f, y2 + tam * 0.5f, alfa);
                c.restore();
            }
        }
    }

    /** Sai devagar e chega rápido (a moeda é puxada pela barra). */
    private static float curvaDoVoo(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (1.6f - 0.6f * t);
    }

    /**
     * A textura do rastro esticada ao longo dos pontos (do fim para a ponta), em fatias giradas
     * que se encostam; a ponta da textura (a cabeça branca, à direita) fica na moeda.
     */
    private static void fita(RenderCanvas c, com.gamelutagpt.render.RenderImage img, float[] x, float[] y, int n, float largura) {
        float total = 0f;
        for (int k = 1; k < n; k++) total += (float)Math.hypot(x[k] - x[k - 1], y[k] - y[k - 1]);
        if (total < 2f) return;
        float andado = 0f, h = largura;
        for (int k = 0; k < n - 1; k++) {
            float vx = x[k + 1] - x[k], vy = y[k + 1] - y[k];
            float comp = (float)Math.hypot(vx, vy);
            if (comp < 0.3f) continue;
            float u0 = img.width() * andado / total;
            andado += comp;
            float u1 = img.width() * andado / total;
            c.save();
            c.translate(x[k], y[k]);
            c.rotate((float)Math.toDegrees(Math.atan2(vy, vx)), 0f, 0f);
            c.drawImageRegion(img, u0, 0f, Math.max(u0 + 1f, u1), img.height(), 0f, -h * 0.5f, comp + 1f, h * 0.5f, 255);
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

    /** Sem a arte (arquivo faltando): o placar simples, desenhado, no rodapé. */
    private void desenharSimples(RenderCanvas c) {
        float l = CENTRO_TELA - 165f, r = CENTRO_TELA + 165f, t = 720f - 52f, b = 720f;
        c.fillPolygon(new float[] {l + 10f, t, r - 10f, t, r, b, l, b}, 0xE0101420);
        for (int s = 0; s < 2; s++) {
            String valor = formatarFala(dinheiro[s]);
            float w = c.measureText(valor, 15f, false);
            float cx = s == 0 ? l + 16f + w * 0.5f : r - 16f - w * 0.5f;
            c.drawText(valor, cx, t + 20f, 15f, caindo(s) ? CAI_COR : quieto[s] >= ESFRIA ? FRIO : 0xFFFFFFFF, INK, 2.5f, false);
        }
        float bl = l + 60f, br = r - 60f, bt = t + 30f, bb = bt + 10f;
        float meio = bl + (br - bl) * parcela(0);
        c.fillRect(bl, bt, meio, bb, 0xFF2F7BE0);
        c.fillRect(meio, bt, br, bb, 0xFFD8343C);
    }

    /** O letreiro do telão: as odds ao vivo. */
    String letreiro() {
        return "ODDS  " + formatarOdd(odd[0]) + "  |  " + formatarOdd(odd[1]);
    }
}
