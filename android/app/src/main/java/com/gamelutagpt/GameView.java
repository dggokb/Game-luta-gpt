package com.gamelutagpt;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.gamelutagpt.historia.MenuPausa;
import com.gamelutagpt.stage.StagePack;
import com.gamelutagpt.stage.StageScene;
import com.gamelutagpt.stage.StageWorld;
import com.gamelutagpt.ultra.AberturaUltra;
import com.gamelutagpt.ultra.PaginaFinal;
import com.gamelutagpt.ultra.RaioFinal;
import com.gamelutagpt.ultra.UltraDefinition;
import com.gamelutagpt.ultra.UltraGrade;
import com.gamelutagpt.ultra.UltraListener;
import com.gamelutagpt.ultra.UltraPack;
import java.io.IOException;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Android shell of the match: touch → {@link PadInput}, CPU → {@link AiController}, a
 * fixed 60 Hz {@link CombatEngine} step, then rendering. Every combat rule lives in the
 * engine; this class only presents it (sprites, Super and ultra cinematics, tag, HUD, debug).
 */
public class GameView extends SurfaceView
    implements SurfaceHolder.Callback, Runnable, UltraListener {
    private FighterState[] team = membersOf(0);
    /** Teclas dos menus por cima da luta (pausa e telas de ajuste), lidas na thread do jogo. */
    private final ConcurrentLinkedQueue<Integer> selectionKeys = new ConcurrentLinkedQueue<>();

    // Training opponent: our point character (same body and moves), in its normal colors.
    // É o titular escolhido para o CPU; quem está em campo agora é opponentPoint().
    private FighterState opponentFighter = new FighterState(team[0].character, "CPU");
    // O parceiro do CPU: no treino (IA OFF) não luta; no modo batalha (IA ON) entra no 2x2.
    // Versão leve: todos são o p01.
    private String opponentTagId = "player_base";

    private static final float VW = Arena.VW;
    private static final float VH = Arena.VH;
    private static final float GROUND_Y = Arena.GROUND_Y;
    private static final float WORLD_WIDTH = Arena.WORLD_WIDTH;
    private static final float PLAYER_START_X = 420f;
    private static final float OPPONENT_START_X = 980f;
    static final int PLAYER = 0;
    static final int OPPONENT = 1;

    /** Fixed simulation step; rendering runs at whatever rate the device manages. */
    static final float FIXED_STEP = CombatConfig.DT;
    /** Onde fica a VELOCIDADE escolhida na pausa (vale para a próxima vez que o app abrir). */
    private static final String SPEED_PREFS = "jogo_v1";
    private static final String SPEED_KEY = "velocidade";
    /** IMPACTO da pausa (impact frames ligados), salvo junto com a velocidade. */
    private static final String IMPACT_KEY = "impacto";
    private static final String FPS_KEY = "contadorFps";
    /** Nível da CPU (1 a 8) escolhido na pausa. */
    private static final String CPU_LEVEL_KEY = "nivel_cpu";

    final CombatEngine engine = new CombatEngine(
        team[0], opponentFighter, PLAYER_START_X, OPPONENT_START_X, new CombatConfig());
    final PadInput pad = new PadInput();
    private final FighterInput playerInput = new FighterInput();
    private final FighterInput opponentInput = new FighterInput();
    private final AiController ai = new AiController(new OpponentAi(new Random()));
    /** READY → GO depois da intro e, com a IA ligada, a batalha de verdade (2x2). */
    private final BattleFlow flow = new BattleFlow();
    private final FighterInput ignoredInput = new FighterInput();
    private boolean debugOverlay;
    /** Screen-space controls shown only in DEBUG, away from combat buttons. */
    private static final float TUNE_LEFT = 944f, TUNE_TOP = 176f, TUNE_RIGHT = 1076f, TUNE_BOTTOM = 266f;
    private static final float BOXES_TOP = 274f, BOXES_BOTTOM = 364f;
    private final Paint tuningPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private String dummyDamageLabel = "";
    private int dummyDamageLabelFrames = 0;
    private static final int DAMAGE_LABEL_FRAMES = 43;

    private final SurfaceHolder holder;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SpriteAtlasCache atlases;
    /** DEBUG > AJUSTAR TAMANHOS: tamanho de cada folha no olho, exportado para o Drive. */
    private final TelaAjusteTamanho ajusteTamanho;
    /** DEBUG > AJUSTAR CAIXAS: hitbox, hurtbox e empurrão no olho, exportados para o Drive. */
    private final TelaAjusteCaixas ajusteCaixas;
    private final AndroidRenderAssets ultraAssets;
    private SpriteFighterRenderer spriteFighterRenderer;
    private SpriteFighterRenderer opponentSpriteRenderer;
    private final StageRenderer stage = new StageRenderer();
    private final EffectsRenderer effects = new EffectsRenderer();
    /** Hit and guard sparks at the real contact point (presentation only). */
    private final ImpactEffects impacts = new ImpactEffects();
    /** Soco de câmera, escurecida do cenário e câmera lenta do KO. */
    private final ImpactFeel feel = new ImpactFeel();
    /** Golpe que mata e ganha a luta com Super ou Ultra: o quadro de mangá com linhas de ação. */
    private final FinalizacaoManga finalizacao = new FinalizacaoManga();
    private final Random finalizacaoLinhas = new Random();
    private final Random soundRandom = new Random(11);
    /** Golpe no ar: o golpe de cada lado que já tocou o "vush", e o quadro em que estava. */
    private final AttackDefinition[] swingAttack = new AttackDefinition[2];
    private final int[] swingFrame = new int[2];
    private final boolean[] swingPlayed = new boolean[2];
    private final boolean[] lyingSeen = new boolean[2];
    private final boolean[] downSeen = new boolean[2];
    private final boolean[] wasAirborne = new boolean[2];
    /** Já mostrou o efeito de saída deste super pulo (um por pulo). */
    private final boolean[] superJumpSeen = new boolean[2];
    /** Quem já levou o golpe do KO (o clarão e a câmera lenta saem uma vez por nocaute). */
    private CombatFighter koVictim;
    private final HudRenderer hud = new HudRenderer();
    private final DebugOverlay debug = new DebugOverlay();
    private final HudRenderer.OpponentPanel opponentPanel = new HudRenderer.OpponentPanel();

    private Thread gameThread;
    private volatile boolean running;

    private final CameraRig camera = new CameraRig();

    // Super cinematic of whoever activates it (presentation only; timing comes from its frames).
    private final SuperFlash superFlash = new SuperFlash();

    // Ultra (↓ + SUPER): the engine runs activation, rush and hit confirm; when it connects
    // the "Página Final" cinematic (ultra-core) plays while the engine waits.
    private static final String LOG_TAG = "GameView";
    private static final float ULTRA_STARTUP_ZOOM = 0.30f;
    /** Zoom da câmera durante a sequência do Ultra (personagens sem raio). */
    /** Point of the heavy straight (fraction of the move) where the arm is fully extended. */
    private static final float BEAM_POSE = 0.5625f;
    /** Point of the heavy straight where the arm is pulled back (charge, without own art). */
    private static final float BEAM_CHARGE_POSE = 0.25f;
    private static final float BEAM_CHARGE_ZOOM = 0.18f;
    /**
     * Packs do Ultra por personagem, prontos ou carregando numa thread à parte. No celular só
     * começam quando alguém chega a {@link #ULTRA_PREPARO} de barra (o Ultra pede 3): a luta não
     * espera por eles, e a memória só vai para quem pode usar.
     */
    private final java.util.Map<String, java.util.concurrent.FutureTask<UltraPack>> ultras =
        new java.util.concurrent.ConcurrentHashMap<>();
    private java.util.concurrent.ExecutorService carregadorDeUltras;
    /** Barra a partir da qual o pack do Ultra começa a carregar em segundo plano. */
    static final int ULTRA_PREPARO = 2 * CombatConfig.METER_PER_BAR;
    /** Ativou o Ultra antes do pack ficar pronto: a luta espera parada (sem travar a tela). */
    private CombatFighter ultraEsperando;
    private long ultraEsperandoDesde;
    private final PaginaFinal paginaFinal = new PaginaFinal(this);
    /** Abertura cinematográfica do Ultra (a luta fica parada enquanto ela roda). */
    private final AberturaUltra abertura = new AberturaUltra();
    /** Plano de câmera do Ultra em sequência, e se ele está valendo agora. */
    private final UltraDirector.Shot ultraShot = new UltraDirector.Shot();
    /** Direção do Ultra em andamento (do "estilo" do ultra.json), escolhida na ativação. */
    private UltraDirector.Script ultraScript = UltraDirector.PADRAO;
    private int ultraFinalImpact = ImpactFeel.VOID;
    private float ultraSlowScale = ImpactFeel.ULTRA_SLOW_SCALE;
    private boolean ultraShotActive;
    private float ultraTilt;
    /** Faixas pretas de cinema do Ultra (0 = fora, 1 = inteiras; o HUD some junto). */
    private float ultraBars;
    /** Pincel do Canvas do quadro atual para os motores visuais (cenário e ultra). */
    private final AndroidRenderCanvas renderCanvas = new AndroidRenderCanvas();

    // Cenário com falso 3D (stage-core). Sem ele, o StageRenderer antigo desenha o fundo.
    private static final String DEFAULT_STAGE = "vazio";
    private StageScene stageScene;
    /** Cenário carregado agora (pasta em assets/stages). Cada luta pode pedir o seu. */
    private String stageId;
    /** Desenho pela GPU (Android 8+); cai para o Canvas por software se falhar. */
    private boolean hardwareCanvas = Build.VERSION.SDK_INT >= 26;
    private final UltraSounds ultraSounds;

    /** Pausa (botão MENU ou tecla voltar): continuar ou voltar ao menu inicial. */
    private final MenuPausa pausa = new MenuPausa();
    private int ultraAttacker = -1;
    private int ultraDamageDealt;
    private float ultraCameraZoom = 1f;
    private int ultraDarkAlpha;
    private float ultraAuraTime;
    private int ultraPhaseSeen = -1;
    /** Intro clock (frames) while both fighters play INTRO; -1 when the fight is on. */
    private int introFrame = -1;
    private int introTotalFrames;
    private boolean introPlayed;
    /** Frames the winner has been celebrating (player, opponent); 0 while nobody is KO. */
    private final int[] victoryFrames = new int[2];
    private final int[] defeatFrames = new int[2];
    /** A DEFEAT do vídeo dura ~4 s: tocada mais rápida, termina antes do resultado sair da tela. */
    private static final float DEFEAT_SPEED = 2f;
    /** Ultra playing now (cinematic, then the final beam). */
    private UltraPack ultraPack;
    // Final beam: drawn from the engine state, with its own clock (keeps flickering in hitstop).
    private final RaioFinal raioFinal = new RaioFinal();
    private final RaioFinal.Frame beamFrame = new RaioFinal.Frame();
    private float beamClock;
    private float beamLastHitClock = -1f;
    private float beamBlastClock = -1f;
    private int beamHitsSeen;
    private boolean beamFired;
    private float beamTargetX;
    private float beamShake;
    private final Random shakeRandom = new Random(3);

    // Team HUD labels (the rules live in the engine's TeamSystem).
    private String tagCooldownHudLabel = "ASSIST: PRONTO";
    private String tagCooldownButtonLabel = "";
    private int shownAssistTenths = -2;
    private int shownTagSeconds = -2;
    private int shownPoint = 0;
    /** Partner on screen: the assist, or the former point running off after Assist → Tag. */
    private SpriteFighterRenderer partnerSpriteRenderer;
    /** Sombra do eco (p06): cópia roxa e translúcida dos golpes, desenhada atrás do lutador. */
    private SpriteFighterRenderer echoRenderer;
    /** Capangas do Super (p07): as animações CAPANGA_* ficam no pacote de quem os chama. */
    private SpriteFighterRenderer henchmanRenderer;
    private final CombatFighter leavingBody = new CombatFighter(PLAYER, null, 0f, 1);
    /** O parceiro do CPU em campo (assist, ou o titular saindo depois do Assist → Tag). */
    private SpriteFighterRenderer opponentPartnerRenderer;
    private final CombatFighter opponentLeavingBody = new CombatFighter(OPPONENT, null, 0f, -1);
    /** Ultra do CPU: os packs dos personagens dele que não estão no time do jogador. */

    // Single version source: versionName in app/build.gradle.
    private static final String VERSION_HUD_LABEL =
        "SPRITE GPT • v" + BuildConfig.VERSION_NAME.split("-")[0];

    private int dpadPointer = -1;
    private int lightPointer = -1;
    private int mediumPointer = -1;
    private int heavyPointer = -1;
    private int comboPointer = -1;
    private int tagPointer = -1;
    private int throwPointer = -1;
    private int pushblockPointer = -1;
    private int overdrivePointer = -1;
    /** "AGARRÃO!" / "TECH!" over the fight, and how many frames it still shows. */
    private String throwBanner;
    private int throwBannerFrames;
    private static final int THROW_BANNER_FRAMES = 42;
    private float throwPoseTime;
    private int superPointer = -1;
    private int healPlayerPointer = -1;
    private int healOpponentPointer = -1;

    private final ConcurrentLinkedQueue<MotionEvent> pendingInput =
        new ConcurrentLinkedQueue<>();
    /**
     * Carregamento pesado (atlas das duplas, cenário, ultras): roda numa thread à parte enquanto
     * o loop só desenha a {@link TelaCarregando}, para o jogo nunca parecer travado. Durante o
     * carregamento o loop não lê toque nem tecla e não roda a luta: só o carregador mexe no
     * estado. Fila e thread são da thread do jogo.
     */
    private final java.util.ArrayList<Runnable> loadQueue = new java.util.ArrayList<>();
    private volatile Thread loader;
    private volatile Throwable loadError;
    private long loadingSince;
    private final TelaCarregando telaCarregando = new TelaCarregando();
    /** Controle virtual (D-pad e botões de golpe) na tela; o PC desliga, lá é teclado/controle. */
    private volatile boolean touchControls = true;
    /**
     * O que o jogador está usando, para o tutorial mostrar os botões certos: o último entre
     * toque, teclado e controle (o controle pelo tipo: Xbox, PlayStation, Nintendo).
     */
    private volatile Comandos.Esquema esquema = Comandos.Esquema.TOQUE;

    /** Teclado/controle físico na luta (PC e Android): +código ao apertar, -código ao soltar. */
    private final ConcurrentLinkedQueue<Integer> hardwareKeys = new ConcurrentLinkedQueue<>();
    private final HardwareInput hardware = new HardwareInput();
    private volatile boolean surfaceReady;
    private volatile boolean activityActive = true;
    private float accumulatedTime;
    private SharedPreferences speedPrefs;

    private void clearInput() {
        clearPendingInput();
        hardwareKeys.clear();
        hardware.reset();
        pad.reset();
        releaseAllPointers();
        // O app saiu da tela (pausa da Activity): os dedos de antes não contam mais; ao voltar,
        // o direcional só vale com um toque novo (nada de andar sozinho).
        touchCount = 0;
    }

    private void releaseAllPointers() {
        dpadPointer = lightPointer = mediumPointer = heavyPointer = comboPointer = tagPointer = throwPointer = pushblockPointer = overdrivePointer = superPointer = -1;
        healPlayerPointer = healOpponentPointer = -1;
    }

    // Dedos na tela agora (coordenadas virtuais), sempre atualizados, com qualquer tela aberta:
    // o controle zera em várias horas (cena, página, troca de luta) e o dedo que continuou
    // no direcional volta a valer sozinho, sem tirar e apertar de novo.
    private static final int MAX_TOUCHES = 10;
    private final int[] touchIds = new int[MAX_TOUCHES];
    private final float[] touchX = new float[MAX_TOUCHES], touchY = new float[MAX_TOUCHES];
    private int touchCount;

    /** Anota onde está cada dedo (e quais saíram) a cada evento de toque. */
    private void trackTouches(MotionEvent event, int action, int index, float sx, float sy) {
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            touchCount = 0;
            return;
        }
        touchCount = 0;
        for (int i = 0; i < event.getPointerCount() && touchCount < MAX_TOUCHES; i++) {
            if (action == MotionEvent.ACTION_POINTER_UP && i == index) continue;
            touchIds[touchCount] = event.getPointerId(i);
            touchX[touchCount] = event.getX(i) / sx;
            touchY[touchCount] = event.getY(i) / sy;
            touchCount++;
        }
    }

    private int touchSlot(int pointerId) {
        for (int i = 0; i < touchCount; i++) if (touchIds[i] == pointerId) return i;
        return -1;
    }

    private boolean ownedByButton(int pointerId) {
        return pointerId == lightPointer || pointerId == mediumPointer || pointerId == heavyPointer ||
            pointerId == comboPointer || pointerId == tagPointer || pointerId == throwPointer ||
            pointerId == pushblockPointer || pointerId == overdrivePointer || pointerId == superPointer ||
            pointerId == healPlayerPointer || pointerId == healOpponentPointer;
    }

    /**
     * Uma vez por quadro: solta os botões e o direcional de dedos que já saíram da tela (a
     * soltura pode ter chegado com uma tela por cima, que não repassa o toque) e, na luta,
     * devolve o direcional ao dedo que está em cima dele sem dono (depois de o controle ter
     * zerado, ou de um dedo que escorregou para dentro dele).
     */
    private void syncHeldTouches() {
        if (dpadPointer != -1 && touchSlot(dpadPointer) < 0) {
            dpadPointer = -1;
            pad.setDirection(0);
        }
        if (lightPointer != -1 && touchSlot(lightPointer) < 0) lightPointer = -1;
        if (mediumPointer != -1 && touchSlot(mediumPointer) < 0) mediumPointer = -1;
        if (heavyPointer != -1 && touchSlot(heavyPointer) < 0) heavyPointer = -1;
        if (comboPointer != -1 && touchSlot(comboPointer) < 0) comboPointer = -1;
        if (tagPointer != -1 && touchSlot(tagPointer) < 0) tagPointer = -1;
        if (throwPointer != -1 && touchSlot(throwPointer) < 0) throwPointer = -1;
        if (pushblockPointer != -1 && touchSlot(pushblockPointer) < 0) pushblockPointer = -1;
        if (overdrivePointer != -1 && touchSlot(overdrivePointer) < 0) overdrivePointer = -1;
        if (superPointer != -1 && touchSlot(superPointer) < 0) superPointer = -1;
        if (healPlayerPointer != -1 && touchSlot(healPlayerPointer) < 0) healPlayerPointer = -1;
        if (healOpponentPointer != -1 && touchSlot(healOpponentPointer) < 0) healOpponentPointer = -1;
        if (dpadPointer != -1 || !touchControls || !fightClockRunning()) return;
        for (int i = 0; i < touchCount; i++) {
            if (ownedByButton(touchIds[i]) || !ControlsLayout.dpadCaptures(touchX[i], touchY[i])) continue;
            dpadPointer = touchIds[i];
            pad.setDirection(ControlsLayout.dpadDirectionAt(touchX[i], touchY[i]));
            return;
        }
    }

    /** Frees the sound pool when the Activity is destroyed. */
    public void release() {
        ultraSounds.release();
    }

    public GameView(Context context) {
        this(context, false);
    }

    /**
     * {@code emSegundoPlano}: a arte pesada (folhas dos lutadores, seleção, cenário, ultras) só
     * é decodificada quando o jogador sai do menu inicial ({@link #comArteDaLuta}), na thread
     * "Carregando", com a tela de carregamento na frente. O menu não usa nada disso: carregar
     * tudo antes dele deixava a abertura do app lenta (as folhas de dois lutadores sozinhas
     * levam vários segundos), e decodificar na thread principal virava "não está respondendo"
     * (ANR). Testes e PC continuam carregando tudo na hora.
     */
    public GameView(Context context, boolean emSegundoPlano) {
        super(context);
        AjusteTamanho.load(context);
        AjusteCaixas.load(context);
        speedPrefs = context.getSharedPreferences(SPEED_PREFS, Context.MODE_PRIVATE);
        qualidade = new QualidadeGrafica(speedPrefs);
        pausa.setVelocidade(speedPrefs.getFloat(SPEED_KEY, MenuPausa.VELOCIDADE_PADRAO));
        pausa.setImpacto(speedPrefs.getBoolean(IMPACT_KEY, true));
        pausa.setFps(speedPrefs.getBoolean(FPS_KEY, true));
        pausa.setNivelCpu(speedPrefs.getInt(CPU_LEVEL_KEY, MenuPausa.NIVEL_PADRAO));
        pausa.setGraficos(new MenuPausa.Graficos() {
            @Override public String rotulo(java.util.function.UnaryOperator<String> traduzir) {
                return qualidade.rotulo(traduzir);
            }

            @Override public void mudar(int direcao) {
                qualidade.proximoModo(direcao);
                aplicarQualidade();
            }
        });
        ai.setLevel(pausa.nivelCpu());
        atlases = new SpriteAtlasCache(context);
        ajusteTamanho = new TelaAjusteTamanho(atlases, new TelaAjusteTamanho.Host() {
            @Override public void manterAtlas(CharacterDefinition... manter) {
                // Só as folhas do personagem em ajuste e da referência: a luta está parada e volta a
                // decodificar ao sair.
                atlases.retainOnly(manter);
            }
            @Override public void salvar() { post(GameView.this::exportSizeTuning); }
            @Override public void fechar() { keepFightAtlases(); }
        });
        ajusteCaixas = new TelaAjusteCaixas(atlases, new TelaAjusteTamanho.Host() {
            @Override public void manterAtlas(CharacterDefinition... manter) { atlases.retainOnly(manter); }
            @Override public void salvar() { post(GameView.this::exportBoxTuning); }
            @Override public void fechar() { keepFightAtlases(); }
        });
        engine.setTeam(PLAYER, team);
        ultraSounds = new UltraSounds(context.getAssets());
        ultraAssets = new AndroidRenderAssets(context.getAssets());
        // Ultra cinematográfico: a sequência do vídeo espera a página de mangá antes do golpe final.
        engine.config.ultraSequencePage = true;
        stageId = DEFAULT_STAGE;
        holder = getHolder();
        holder.addCallback(this);
        setFocusable(true);
        setKeepScreenOn(true);
        contextoArte = context;
        carregarFolhasNaHora = !emSegundoPlano;
        if (!emSegundoPlano) carregarArte(context);
        else anteciparArteDaLuta(context);
    }

    /**
     * PC e testes: a arte inteira na construção, folhas da dupla inicial incluídas. No celular
     * as folhas de cada luta são decodificadas (ou já foram antecipadas) quando ela é montada.
     */
    private boolean carregarFolhasNaHora;
    /** Os rostos do HUD (recortados das folhas de conceito). */
    private HudConceptPortraits rostos;
    private java.util.concurrent.FutureTask<StageScene> cenarioAntecipado;
    private String cenarioAntecipadoId;
    /** A última combinação de lutadores antecipada (não repete a cada quadro). */
    private String lutadoresAntecipados = "";

    /**
     * Começa, em segundo plano, o que a luta vai pedir: o cenário e as folhas do p01. O
     * carregamento depois só espera o que ainda faltar.
     */
    private void anteciparArteDaLuta(Context context) {
        final String id = stageId;
        cenarioAntecipadoId = id;
        cenarioAntecipado = new java.util.concurrent.FutureTask<>(() -> loadStage(ultraAssets, id));
        Thread t = new Thread(cenarioAntecipado, "Antecipando");
        t.setDaemon(true);
        t.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 2));
        t.start();
        anteciparLutadores(team[0].character.id);
    }

    /** O resultado de um trabalho antecipado (esperando o que faltar), ou null se ele falhou. */
    private static <T> T resultado(java.util.concurrent.FutureTask<T> tarefa) {
        if (tarefa == null) return null;
        try {
            return tarefa.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (java.util.concurrent.ExecutionException e) {
            Log.w(LOG_TAG, "carregamento antecipado falhou; carregando de novo", e.getCause());
            return null;
        }
    }

    /** Começa a decodificar as folhas desses lutadores em segundo plano (sem esperar). */
    private void anteciparLutadores(String... ids) {
        String chave = String.join(",", ids);
        if (chave.equals(lutadoresAntecipados)) return;
        lutadoresAntecipados = chave;
        java.util.ArrayList<CharacterDefinition> lutadores = new java.util.ArrayList<>();
        for (String id : ids) {
            if (id == null) continue;
            try {
                lutadores.add(GeneratedCharacters.get(id));
            } catch (IllegalArgumentException e) {
                // Lutador sem pack (ainda): nada para antecipar.
            }
        }
        if (!atlases.antecipar(lutadores.toArray(new CharacterDefinition[0])))
            Log.i(MedicaoCarregamento.TAG, "antecipação parcial (memória): " + chave);
    }

    /** Contexto para a arte da luta, carregada na primeira saída do menu inicial. */
    private final Context contextoArte;
    /** A arte da luta (HUD, folhas, cenário, seleção, ultras) já foi carregada. */
    private volatile boolean arteCarregada;

    /**
     * Roda {@code acao} com a arte da luta pronta: na primeira vez, carrega antes (com a tela
     * de carregamento); depois, na hora.
     */
    private void comArteDaLuta(Runnable acao) {
        if (arteCarregada) {
            acao.run();
        } else {
            carregando(() -> {
                if (!arteCarregada) carregarArte(contextoArte);
                acao.run();
            });
        }
    }

    /** A parte pesada da montagem: só decodifica arte e sons, nada que a thread da UI leia. */
    private void carregarArte(Context context) {
        MedicaoCarregamento medicao = new MedicaoCarregamento("arte");
        hud.loadArt(context);
        medicao.parte("hud");
        // One decode per atlas for the whole match: team packs (tag never decodes
        // mid-fight) and the opponent share the same cache. No celular só os renderers: as
        // folhas vêm com a luta (selectTeamsNow), muitas vezes já antecipadas.
        boolean folhas = carregarFolhasNaHora;
        if (folhas) atlases.preload(team[0].character, team[1].character, opponentFighter.character);
        spriteFighterRenderer = new SpriteFighterRenderer(atlases, team[0].character, folhas);
        // O adversário aparece com as cores normais da arte (sem o filtro pálido de antes).
        opponentSpriteRenderer = new SpriteFighterRenderer(atlases, opponentFighter.character, folhas);
        partnerSpriteRenderer = new SpriteFighterRenderer(atlases, team[1].character, folhas);
        echoRenderer = new SpriteFighterRenderer(atlases, team[0].character, folhas);
        echoRenderer.setTint(SpriteFighterRenderer.shadowEcho());
        henchmanRenderer = new SpriteFighterRenderer(atlases, team[0].character, folhas);
        opponentPartnerRenderer = new SpriteFighterRenderer(atlases, opponentFighter.character, folhas);
        medicao.parte("folhas");
        StageScene cenario = stageId.equals(cenarioAntecipadoId) ? resultado(cenarioAntecipado) : null;
        stageScene = cenario != null ? cenario : loadStage(ultraAssets, stageId);
        cenarioAntecipado = null;
        medicao.parte("cenario");
        rostos = new HudConceptPortraits(context);
        prepararRostosDoHud(team[0].character, team[1].character, opponentFighter.character);
        medicao.parte("rostos");
        loadTeamUltras();
        medicao.parte("ultras");
        medicao.fim(atlases);
        arteCarregada = true;
    }

    /** Quem aparece na lista de COMANDOS da pausa (versão leve: só o p01). */
    private static String[] selectionRoster() {
        return GeneratedCharacters.SELECTABLE.clone();
    }

    private static FighterState[] membersOf(int index) {
        String[] ids = GeneratedCharacters.TEAMS[index];
        return membersOf(ids[0], ids[1]);
    }

    private static FighterState[] membersOf(String point, String partner) {
        return new FighterState[] {
            new FighterState(GeneratedCharacters.get(point), "PLAYER 1"),
            new FighterState(GeneratedCharacters.get(partner), "PLAYER 2")
        };
    }

    private void loadTeamUltras() {
        for (FighterState member : team) {
            // PC e testes: na hora, como sempre; no celular, quando chegar perto do Ultra.
            if (carregarFolhasNaHora) pedirUltra(member, true);
            ultraSounds.loadUltra(ultraFolder(member));
        }
        manterUltras();
    }

    /** O pack do Ultra do personagem: pedido agora (bloqueando) ou numa thread à parte. */
    private java.util.concurrent.FutureTask<UltraPack> pedirUltra(FighterState fighter, boolean agora) {
        String id = fighter.character.id;
        java.util.concurrent.FutureTask<UltraPack> tarefa = ultras.get(id);
        if (tarefa == null) {
            final FighterState quem = fighter;
            tarefa = new java.util.concurrent.FutureTask<>(() -> loadUltraPack(ultraAssets, quem));
            ultras.put(id, tarefa);
            if (agora) {
                tarefa.run();
            } else {
                synchronized (ultras) {
                    if (carregadorDeUltras == null) {
                        carregadorDeUltras = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                            Thread t = new Thread(r, "Ultras");
                            t.setDaemon(true);
                            t.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 2));
                            return t;
                        });
                    }
                }
                carregadorDeUltras.execute(tarefa);
            }
        } else if (agora && !tarefa.isDone()) {
            resultado(tarefa);
        }
        return tarefa;
    }

    /** O pack pronto do personagem, ou null se ainda não foi pedido ou está carregando. */
    private UltraPack ultraPronto(String characterId) {
        java.util.concurrent.FutureTask<UltraPack> tarefa = ultras.get(characterId);
        return tarefa != null && tarefa.isDone() ? resultado(tarefa) : null;
    }

    /** Solta os packs de quem saiu da luta. */
    private void manterUltras() {
        java.util.Set<String> ficam = new java.util.HashSet<>();
        for (FighterState member : team) ficam.add(member.character.id);
        if (flow.battle()) for (FighterState member : engine.team(OPPONENT).members) ficam.add(member.character.id);
        ultras.keySet().retainAll(ficam);
    }

    /**
     * No celular: quem tem {@link #ULTRA_PREPARO} de barra pode chegar ao Ultra, então o pack
     * dele (e o do rival em campo, que aparece na abertura dividida) começa a carregar agora.
     */
    private void anteciparUltras() {
        for (int side = 0; side < 2; side++) {
            for (FighterState member : engine.team(side).members) {
                if (member.superMeter < ULTRA_PREPARO) continue;
                if (usaPackReal(member, side)) pedirUltra(member, false);
                FighterState rival = engine.team(1 - side).pointState();
                if (usaPackReal(rival, 1 - side)) pedirUltra(rival, false);
            }
        }
    }

    /**
     * O personagem usa o pack de verdade: o jogador sempre; o CPU na batalha, ou no treino se for
     * alguém da dupla do jogador (fora disso, o Ultra do CPU de treino usa o provisório).
     */
    private boolean usaPackReal(FighterState f, int side) {
        return side == PLAYER || flow.battle() || teamHas(f.character.id);
    }

    private boolean teamHas(String characterId) {
        for (FighterState member : team) if (member.character.id.equals(characterId)) return true;
        return false;
    }

    /** Os packs que a abertura do Ultra de {@code p} mostra (o dele e o do rival) estão prontos. */
    private boolean ultraProntoParaAbertura(CombatFighter p) {
        return prontoOuSemPack(p.state) && prontoOuSemPack(engine.fighter(1 - p.index).state);
    }

    private boolean prontoOuSemPack(FighterState f) {
        java.util.concurrent.FutureTask<UltraPack> tarefa = ultras.get(f.character.id);
        return tarefa == null || tarefa.isDone();
    }

    /** Monta a dupla P1 e a do CPU e começa um round novo com a intro. */
    private void selectTeams(String point, String partner, String cpuPoint, String cpuPartner) {
        // Duplas novas decodificam a arte delas (segundos): com a tela de carregamento.
        carregando(() -> selectTeamsNow(point, partner, cpuPoint, cpuPartner));
    }

    private void selectTeamsNow(String point, String partner, String cpuPoint, String cpuPartner) {
        MedicaoCarregamento medicao = new MedicaoCarregamento("dupla " + point + "+" + partner + " x " + cpuPoint + "+" + cpuPartner);
        ultraEsperando = null;
        hud.resetEffects();
        team = membersOf(point, partner);
        opponentFighter = new FighterState(GeneratedCharacters.get(cpuPoint), "CPU");
        opponentTagId = cpuPartner;
        CharacterDefinition cpuPartnerCharacter = GeneratedCharacters.get(cpuPartner);
        if (flow.battle()) {
            // Batalha 2x2: o parceiro do CPU entra de assist, na troca e quando o titular cai,
            // então a arte dele fica carregada (sem decodificar no meio da luta).
            atlases.retainOnly(team[0].character, team[1].character, opponentFighter.character, cpuPartnerCharacter);
            atlases.preload(team[0].character, team[1].character, opponentFighter.character, cpuPartnerCharacter);
        } else {
            atlases.retainOnly(team[0].character, team[1].character, opponentFighter.character);
            // As folhas dos três de uma vez, em paralelo; as antecipadas só são esperadas.
            atlases.preload(team[0].character, team[1].character, opponentFighter.character);
        }
        lutadoresAntecipados = "";
        // Os rostos do HUD saem de folhas de conceito grandes: decodificados aqui, e não no
        // primeiro quadro da luta (que engasgava).
        prepararRostosDoHud(team[0].character, team[1].character, opponentFighter.character, cpuPartnerCharacter);
        spriteFighterRenderer.setCharacter(team[0].character.id);
        partnerSpriteRenderer.setCharacter(team[1].character.id);
        opponentSpriteRenderer.setCharacter(opponentFighter.character.id);
        engine.setTeam(PLAYER, team);
        if (flow.battle()) {
            engine.setTeam(OPPONENT, opponentFighter, new FighterState(cpuPartnerCharacter, "CPU"));
        } else {
            engine.setTeam(OPPONENT, opponentFighter);
        }
        engine.refreshOverdrive(PLAYER);
        engine.refreshOverdrive(OPPONENT);
        medicao.parte("folhas");
        engine.restage(PLAYER_START_X, OPPONENT_START_X);
        loadTeamUltras();
        loadCpuUltras();
        medicao.parte("ultras");
        medicao.fim(atlases);
        startIntro();
    }

    /**
     * O Ultra do CPU usa a arte do personagem dele. Quem também está no time do jogador
     * reaproveita o pack já carregado; os outros carregam aqui (PC e testes) ou perto do Ultra.
     */
    private void loadCpuUltras() {
        if (!flow.battle()) return;
        for (FighterState member : engine.team(OPPONENT).members) {
            if (carregarFolhasNaHora) pedirUltra(member, true);
            ultraSounds.loadUltra(ultraFolder(member));
        }
    }

    private UltraPack teamUltraPack(String characterId) {
        return teamHas(characterId) ? ultraPronto(characterId) : null;
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        notarEsquema(keyCode, event);
        // Na luta, voltar/START abre a pausa (o loop do jogo lê a fila).
        if (pausa.isOpen() || ajusteTamanho.isOpen() || ajusteCaixas.isOpen() || isPauseKey(keyCode)) {
            offerMenuKey(keyCode, event);
            return true;
        }
        if (HardwareInput.handles(keyCode)) {
            // Teclado/controle na luta. Segurar repete o evento: só o primeiro aperta o botão.
            // Direção repetida passa (vale como "segurando", mesmo se apertou ainda num menu).
            if (event.getRepeatCount() == 0 || HardwareInput.isDirectionKey(keyCode)) hardwareKeys.offer(keyCode);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Soltar sempre chega, até com um menu aberto, para nada ficar segurado.
        if (HardwareInput.handles(keyCode)) {
            hardwareKeys.offer(-keyCode);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Teclas dos menus: as letras do teclado viram os botões do controle (WASD, J, K...).
     * Segurar uma direção anda o cursor; os botões valem uma vez por aperto.
     */
    private void offerMenuKey(int keyCode, KeyEvent event) {
        int key = HardwareInput.menuKey(keyCode);
        if (event.getRepeatCount() == 0 || HardwareInput.isDpad(key)) selectionKeys.offer(key);
    }

    /** Teclado/controle na thread do jogo: direção segurada, botões e comandos de teste. */
    private void processHardwareKeys() {
        Integer queued;
        while ((queued = hardwareKeys.poll()) != null) {
            boolean pressed = queued > 0;
            int keyCode = Math.abs(queued);
            hardware.setButtonHeld(keyCode, pressed);
            if (hardware.setHeld(keyCode, pressed)) {
                // O D-pad da tela, quando segurado, continua mandando na direção.
                if (dpadPointer == -1) pad.setDirection(hardware.direction());
                continue;
            }
            if (!pressed) continue;
            PadInput.Button button = HardwareInput.button(keyCode);
            if (button != null) {
                // Na "Página Final" do jogador qualquer botão vale como o toque no tempo certo.
                if (paginaFinal.isActive()) {
                    if (ultraAttacker == PLAYER) paginaFinal.tap();
                } else {
                    pad.press(button);
                }
                continue;
            }
            // Os comandos de teste seguem os botões de teste: fora do modo história.
            HardwareInput.Action action = HardwareInput.action(keyCode);
            if (action == HardwareInput.Action.CHARACTER_SELECT) {
                openPause();
            } else {
                switch (action) {
                    case DEBUG: debugOverlay = !debugOverlay; break;
                    case AI: setOpponentAiEnabled(!ai.enabled()); break;
                    case HEAL_PLAYER: healPlayerTeam(); break;
                    case HEAL_OPPONENT: healOpponentTeam(); break;
                    default: break;
                }
            }
        }
    }

    /** VIDA P1: os dois do time, para o reserva também estar pronto depois do tag. */
    private void healPlayerTeam() {
        hud.resetEffects();
        for (FighterState member : team) member.restoreLife();
        engine.refreshOverdrive(PLAYER);
    }

    /** VIDA CPU: a dupla inteira do CPU no 2x2 (só o titular no treino). */
    private void healOpponentTeam() {
        hud.resetEffects();
        for (FighterState member : engine.team(OPPONENT).members) member.restoreLife();
        engine.refreshOverdrive(OPPONENT);
    }

    private static boolean isPauseKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE ||
            keyCode == KeyEvent.KEYCODE_BUTTON_START;
    }

    private String teamLabel() {
        return displayId(team[0]) + "+" + displayId(team[1]);
    }

    /** p01 is the player_base pack. */
    private static String displayId(FighterState fighter) {
        String id = fighter.character.id;
        return "player_base".equals(id) ? "p01" : id;
    }

    /** Each character's ultra lives in assets/ultras/&lt;character id&gt;/. */
    private static String ultraFolder(FighterState fighter) {
        return "ultras/" + ("p01_training".equals(fighter.character.id) ? "player_base" : fighter.character.id);
    }

    /**
     * Troca o cenário (pasta em assets/stages; null = o padrão). As imagens do anterior são
     * soltas antes de carregar o novo, para os dois não ocuparem a memória juntos.
     */
    private void useStage(String id) {
        String wanted = id != null ? id : DEFAULT_STAGE;
        if (wanted.equals(stageId)) return;
        stageScene = null;
        stageId = wanted;
        stageScene = loadStage(ultraAssets, wanted);
    }

    private static StageScene loadStage(AndroidRenderAssets assets, String id) {
        String folder = "stages/" + id;
        try {
            StagePack pack = StagePack.load(assets, folder);
            for (String warning : pack.warnings) Log.w(LOG_TAG, folder + ": " + warning);
            StageWorld world = new StageWorld(GROUND_Y, WORLD_WIDTH, CameraRig.CAMERA_ZOOM,
                (PLAYER_START_X + OPPONENT_START_X) * 0.5f, CameraRig.CAMERA_GROUND_SCREEN_Y);
            return new StageScene(pack, world);
        } catch (IOException | RuntimeException ex) {
            Log.w(LOG_TAG, "cenário " + folder + " indisponível, usando o fundo simples", ex);
            return null;
        }
    }

    private static UltraPack loadUltraPack(AndroidRenderAssets assets, FighterState fighter) {
        try {
            UltraPack pack = UltraPack.load(assets, ultraFolder(fighter));
            for (String warning : pack.warnings) Log.w(LOG_TAG, ultraFolder(fighter) + ": " + warning);
            return pack.withCutIn(selectionArt(assets, fighter));
        } catch (IOException | RuntimeException ex) {
            // Without a valid ultra.json the ultra still works, with the placeholder art.
            Log.w(LOG_TAG, "ultra de " + fighter.character.id + " usando arte provisória", ex);
            return UltraPack.placeholder("ULTRA " + fighter.character.displayName, fighter.profile.color);
        }
    }

    /**
     * Arte de seleção do personagem (fundo transparente): faz o papel do corte da abertura do
     * Ultra enquanto o pack não tem o seu. Null quando não existe.
     */
    private static com.gamelutagpt.render.RenderImage selectionArt(AndroidRenderAssets assets, FighterState fighter) {
        String id = fighter.character.id;
        String art = "player_base".equals(id) || "p01_training".equals(id) ? "p01" : id;
        try {
            return assets.loadImage("selection/art/hero_" + art + ".png");
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    private FighterState activeFighter() {
        return engine.team(PLAYER).pointState();
    }

    private FighterState reserveFighter() {
        FighterState partner = engine.team(PLAYER).partner();
        return partner != null ? partner : activeFighter();
    }

    private CombatFighter player() {
        return engine.fighter(PLAYER);
    }

    private CombatFighter opponent() {
        return engine.fighter(OPPONENT);
    }

    /** Quem do CPU está em campo agora (no 2x2 muda quando o titular cai). */
    private FighterState opponentPoint() {
        return engine.team(OPPONENT).pointState();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (!introPlayed) {
            introPlayed = true;
            // Versão leve: o app abre direto no treino do p01 (sem menu inicial). No celular a
            // arte carrega no primeiro passo do loop, com a tela de carregamento.
            startIntro();
        }
        surfaceReady = true;
        // Sempre: até no ALTO a resolução tem teto (tela 1440p+ desenhava 2–4× os pixels à toa).
        aplicarQualidade();
        startLoopIfReady();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {}

    /**
     * Direcional e botões ficam colados nas bordas da tela: no Android com navegação por
     * gestos, o polegar que escorrega para a borda vira o gesto de voltar e o sistema cancela
     * o toque (o dedo para de valer até sair e voltar). Essas áreas ficam fora dos gestos.
     */
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (android.os.Build.VERSION.SDK_INT < 29 || w == 0 || h == 0) return;
        float sx = w / VW, sy = h / VH;
        float r = ControlsLayout.DPAD_CAPTURE_RADIUS;
        java.util.List<android.graphics.Rect> rects = new java.util.ArrayList<>();
        rects.add(new android.graphics.Rect(0, Math.round((ControlsLayout.DPAD_Y - r) * sy),
            Math.round((ControlsLayout.DPAD_X + r) * sx), h));
        rects.add(new android.graphics.Rect(Math.round((ControlsLayout.HEAVY_X - ControlsLayout.ATTACK_RADIUS - 20f) * sx),
            Math.round((ControlsLayout.MEDIUM_Y - ControlsLayout.ATTACK_RADIUS - 20f) * sy), w, h));
        setSystemGestureExclusionRects(rects);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        surfaceReady = false;
        stopLoop();
    }

    public void resumeGame() {
        activityActive = true;
        startLoopIfReady();
    }

    public void pauseGame() {
        activityActive = false;
        stopLoop();
    }

    private void startLoopIfReady() {
        if (!surfaceReady || !activityActive || running) return;
        accumulatedTime = 0f;
        running = true;
        gameThread = new Thread(this, "GameLoop");
        gameThread.start();
    }

    private void stopLoop() {
        running = false;
        Thread stopped = gameThread;
        if (stopped != null) {
            stopped.interrupt();
            boolean interrupted = false;
            while (stopped.isAlive()) {
                try {
                    stopped.join();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            if (interrupted) Thread.currentThread().interrupt();
            gameThread = null;
        }
        clearInput();
        accumulatedTime = 0f;
    }

    @Override
    public void run() {
        long previous = System.nanoTime();
        final long targetFrame = 16_666_667L;
        while (running) {
            long frameStart = System.nanoTime();
            float elapsedSeconds = (frameStart - previous) / 1_000_000_000f;
            previous = frameStart;
            perfil.inicio();
            advanceSimulation(elapsedSeconds);
            perfil.marca(PerfilQuadros.ATUALIZA);
            if (!running) break;
            drawFrame();
            if (perfil.fim() && qualidade.janela(perfil.fps(), perfil.fracaoLuta())) {
                Log.i(PerfilQuadros.TAG, "gráficos mudaram para " + QualidadeGrafica.NOMES[qualidade.nivel()]
                    + " (luta a " + Math.round(perfil.fps()) + " fps)");
                aplicarQualidade();
            }
            long remaining = targetFrame - (System.nanoTime() - frameStart);
            if (remaining > 0) {
                try {
                    Thread.sleep(remaining / 1_000_000L, (int)(remaining % 1_000_000L));
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    // Render cadence can vary; the simulation always advances in whole 60 Hz frames.
    private void advanceSimulation(float elapsedSeconds) {
        if (updateLoading()) return;
        if (!arteCarregada) {
            // Celular: a arte da luta ainda não foi decodificada; o próximo quadro já carrega.
            comArteDaLuta(this::startIntro);
            return;
        }
        processPendingInput();
        // VELOCIDADE (pausa): o relógio da luta anda mais devagar, então golpes, pulos, IA,
        // timer e efeitos desaceleram juntos sem mexer no frame data dos packs. Menus,
        // pausa, seleção, páginas da história e a Página Final ficam na velocidade normal.
        if (fightClockRunning()) {
            // Rede de segurança: o Ultra acabou sem passar pelo golpe final.
            if (finalizacao.segurando() && !ultraEmAndamento()) finalizacao.liberar();
            if (finalizacao.pronta()) {
                // A tela ficou livre e chegou o golpe final: o quadro de mangá entra.
                finalizacao.comecar();
                feel.slow(FinalizacaoManga.LENTA_QUADROS, FinalizacaoManga.LENTA_ESCALA);
                beamShake = Math.max(beamShake, FinalizacaoManga.TREMIDA);
            }
            finalizacao.atualizar(clamp(elapsedSeconds, 0f, 0.10f) * pausa.velocidade());
        }
        float speed = fightClockRunning() ? pausa.velocidade() * feel.timeScale() : 1f;
        accumulatedTime += clamp(elapsedSeconds, 0f, 0.10f) * speed;
        while (accumulatedTime + 0.000001f >= FIXED_STEP) {
            // Pediu carregamento: para aqui; o próximo quadro já é a tela de carregamento.
            if (!loadQueue.isEmpty()) {
                accumulatedTime = 0f;
                return;
            }
            update();
            accumulatedTime = Math.max(0f, accumulatedTime - FIXED_STEP);
        }
    }

    /**
     * Pede um carregamento pesado. Roda no próximo quadro, fora do loop, com a tela de
     * carregamento na frente; chamado de dentro de outro carregamento, roda na hora.
     */
    private void carregando(Runnable work) {
        Thread current = Thread.currentThread();
        // Fora do loop do jogo (testes que chamam advanceSimulation, construção) roda na hora,
        // como sempre rodou.
        if (current == loader || current != gameThread) {
            work.run();
        } else {
            loadQueue.add(work);
        }
    }

    private boolean loadingActive() {
        return loader != null || !loadQueue.isEmpty();
    }

    /** True enquanto carrega (o loop não simula nem lê entrada). Começa e termina carregamentos. */
    private boolean updateLoading() {
        Thread running = loader;
        if (running != null) {
            if (running.isAlive()) return true;
            loader = null;
            accumulatedTime = 0f;
            Throwable error = loadError;
            loadError = null;
            if (error instanceof RuntimeException) throw (RuntimeException) error;
            if (error instanceof Error) throw (Error) error;
            if (error != null) throw new IllegalStateException(error);
        }
        if (loadQueue.isEmpty()) return false;
        final java.util.ArrayList<Runnable> works = new java.util.ArrayList<>(loadQueue);
        loadQueue.clear();
        loadingSince = android.os.SystemClock.uptimeMillis();
        Thread thread = new Thread(() -> {
            try {
                for (Runnable work : works) work.run();
            } catch (Throwable t) {
                loadError = t;
            }
        }, "Carregando");
        thread.setDaemon(true);
        loader = thread;
        thread.start();
        return true;
    }

    /** PC: sem o controle virtual na tela (o mouse também não aperta onde ele ficaria). */
    public void setTouchControlsVisible(boolean visible) {
        touchControls = visible;
        if (!visible && esquema == Comandos.Esquema.TOQUE) esquema = Comandos.Esquema.TECLADO;
    }

    /**
     * Controle conectado (o nome ou o tipo dele) ou desconectado (null). Conectado, o tutorial
     * já mostra os botões dele; desconectado, volta ao teclado (ou ao toque).
     */
    public void setControleConectado(String nomeOuTipo) {
        if (nomeOuTipo != null) {
            esquema = Comandos.doControle(nomeOuTipo);
        } else if (esquema != Comandos.Esquema.TOQUE && esquema != Comandos.Esquema.TECLADO) {
            esquema = touchControls ? Comandos.Esquema.TOQUE : Comandos.Esquema.TECLADO;
        }
    }

    /** A tecla mostra o que o jogador usa: botão de controle, ou tecla que só teclado tem. */
    private void notarEsquema(int keyCode, KeyEvent event) {
        if (Comandos.isBotaoDeControle(keyCode)) {
            android.view.InputDevice device = event.getDevice();
            esquema = Comandos.doControle(device != null ? device.getName() : null);
        } else if (Comandos.isTeclaDeTeclado(keyCode)) {
            esquema = Comandos.Esquema.TECLADO;
        }
    }

    /** True quando o próximo update() é a luta (ou treino) e não uma tela por cima dela. */
    private boolean fightClockRunning() {
        return !pausa.isOpen() && !ajusteTamanho.isOpen() && !ajusteCaixas.isOpen() && !paginaFinal.isActive();
    }

    /** One simulation frame: inputs, engine step, then presentation state. */
    private void update() {
        if (stageScene != null) {
            stageScene.update(FIXED_STEP);
            updateTrovao();
        }
        if (pausa.isOpen()) {
            // A luta para até CONTINUAR ou REINICIAR LUTA.
            pad.drainInto(ignoredInput);
            Integer key;
            while (pausa.isOpen() && (key = selectionKeys.poll()) != null) handlePauseKey(key);
            pausa.update(FIXED_STEP);
            return;
        }
        if (ajusteTamanho.isOpen()) {
            // A luta fica parada atrás da tela de ajuste.
            pad.drainInto(ignoredInput);
            Integer key;
            while ((key = selectionKeys.poll()) != null) ajusteTamanho.key(key);
            ajusteTamanho.update(FIXED_STEP);
            return;
        }
        if (ajusteCaixas.isOpen()) {
            pad.drainInto(ignoredInput);
            Integer key;
            while ((key = selectionKeys.poll()) != null) ajusteCaixas.key(key);
            ajusteCaixas.update(FIXED_STEP);
            return;
        }
        updateUltraBars();
        if (ultraEsperando != null) {
            // Ultra ativado antes do pack ficar pronto: a tela segue (faixas, escurecido) e a
            // luta espera, sem travar, até ele chegar.
            selectionKeys.clear();
            if (!ultraProntoParaAbertura(ultraEsperando)) return;
            CombatFighter quem = ultraEsperando;
            ultraEsperando = null;
            Log.i(MedicaoCarregamento.TAG, "ultra de " + quem.state.character.id + " esperou o pack "
                + (android.os.SystemClock.uptimeMillis() - ultraEsperandoDesde) + "ms");
            startUltraOpening(quem);
            return;
        }
        if (!carregarFolhasNaHora) anteciparUltras();
        if (abertura.isActive()) {
            // Abertura do Ultra: a luta espera parada (sem pausa no meio).
            selectionKeys.clear();
            abertura.update(FIXED_STEP);
            return;
        }
        if (paginaFinal.isActive()) {
            // The engine waits: the cinematic deals the damage and ends with the final beam.
            // Sem pausa no meio da Página Final.
            selectionKeys.clear();
            paginaFinal.update(FIXED_STEP);
            if (paginaFinal.isShattering() && ultraAttacker >= 0 && !engine.ultraWaitingPage(ultraAttacker)) {
                // Through the breaking page the fight is already in place for the beam.
                engine.placeForUltraBeam(ultraAttacker);
                updateFightCamera(FIXED_STEP);
            }
            return;
        }
        Integer fightKey;
        while ((fightKey = selectionKeys.poll()) != null) {
            if (isPauseKey(fightKey)) {
                openPause();
                if (pausa.isOpen()) return;
            }
        }
        boolean tagging = isTagAnimationActive();

        boolean intro = introFrame >= 0;
        if (intro || flow.holdsFighters()) {
            // Both fighters play their INTRO, then stand in the READY; nobody moves until GO.
            pad.drainInto(ignoredInput);
            playerInput.clear();
            opponentInput.clear();
        } else {
            pad.setHolding(attackHeld());
            pad.drainInto(playerInput);
            if (tagging) {
                opponentInput.clear();
            } else {
                ai.setSecondsLeft(flow.timed() ? flow.clockSeconds() : -1);
                ai.fill(engine, OPPONENT, opponentInput);
            }
        }
        if (player().ko() || opponent().ko() || flow.over()) {
            // Inputs must not start a new attack/move once the match is over (K.O. or TIME OVER).
            playerInput.clear();
            opponentInput.clear();
        }
        // Depois do TIME OVER o resultado já está decidido: ninguém cai mais.
        protegerVida(flow.timeOver());
        engine.step(playerInput, opponentInput);
        // O parceiro só entra depois do KO quando o raio do ultra já saiu da tela.
        boolean fightFree = ultraAttacker < 0 && !engine.ultraSequenceRunning();
        // O relógio para no congelamento do Super/Ultra.
        boolean clockRuns = fightFree && !engine.superFreezeActive();
        flow.update(engine, fightFree, clockRuns);
        updateOpponentVisuals();
        if (flow.restartDue()) restartBattle();
        if (engine.ultraConnected() >= 0) beginUltraCinematic(engine.ultraConnected());

        // A voadora da troca que acerta gira o rival como o punish: mesmo impacto e tremor.
        boolean punishNow = engine.cues().contains("PUNISH") || engine.cues().contains("SPIN");
        for (CombatEngine.HitEvent event : engine.events()) {
            boolean ultraHit = ultraAttacker >= 0 && event.attacker == ultraAttacker && !event.blocked &&
                "ULTRA".equals(event.moveId);
            if (ultraHit) ultraDamageDealt += event.damage;
            if ("THROW".equals(event.moveId)) showBanner("AGARRÃO!");
            CombatFighter hitFighter = engine.fighter(event.defender);
            // Zerado no meio do Ultra: o K.O. (câmera lenta, close, finalização) fica para o golpe final.
            boolean knockout = !event.blocked && hitFighter.ko() && koVictim != hitFighter &&
                !engine.ultraHoldsKo(event.defender);
            if (knockout) koVictim = hitFighter;
            if (knockout) pedirFinalizacao(FinalizacaoManga.tipoDoGolpe(event.moveId, event.strength),
                event.attacker, event.defender);
            spawnImpact(event, ultraHit, punishNow, knockout);
            if (event.defender == OPPONENT && !event.blocked) {
                dummyDamageLabel = "-" + (ultraHit ? ultraDamageDealt : event.damage);
                dummyDamageLabelFrames = DAMAGE_LABEL_FRAMES;
            }
        }
        for (String cue : engine.cues()) {
            String banner = bannerFor(cue);
            if (banner != null) showBanner(banner);
            // Bounces shake the screen like the beam's hits.
            if ("WALL_BOUNCE".equals(cue) || "GROUND_BOUNCE".equals(cue) || "PUNISH".equals(cue) ||
                "SPIN".equals(cue)) {
                beamShake = Math.max(beamShake, 14f);
            }
            if ("GROUND_BOUNCE".equals(cue)) {
                for (int side = 0; side < 2; side++) {
                    CombatFighter f = engine.fighter(side);
                    if (f.bounce == CombatFighter.BOUNCE_NONE && f.hardFall && f.y > GROUND_Y - 40f) {
                        impacts.spawnDust(f.x, GROUND_Y, 1f);
                    }
                }
            }
            if ("GUARD_BREAK".equals(cue)) spawnGuardBreak();
            if ("JUST_DEFENSE".equals(cue)) spawnJustDefense();
            if ("COMBO_BREAKER".equals(cue)) spawnComboBreaker();
            if ("OVERHEAD".equals(cue)) spawnOverheadWarning();
            if ("ULTRA_FINISH".equals(cue)) {
                // O último golpe do Ultra em sequência: a finalização de mangá entra aqui.
                finalizacao.liberar();
                beamShake = Math.max(beamShake, 22f);
                // Último dano do Ultra em sequência: impact frame em quem apanhou e câmera lenta.
                feel.slow(ImpactFeel.ULTRA_SLOW_FRAMES, ultraSlowScale);
                CombatFighter hit = ultraVictim();
                if (pausa.impacto() && hit != null) feel.impactFrame(ultraFinalImpact, hit.x, chestY(hit));
            }
        }
        updateBreakerSkid();
        for (CombatEngine.ProjectileClash clash : engine.clashes()) spawnClash(clash);
        updateUltraBeam();
        impacts.update(FIXED_STEP);
        feel.step();
        playMotionSounds();
        if (dummyDamageLabelFrames > 0) dummyDamageLabelFrames--;
        if (throwBannerFrames > 0) throwBannerFrames--;

        updateSuperVisuals();
        updateUltraVisuals();
        updateFightCamera(FIXED_STEP);
        updateTeamVisuals();
        updateFighterSprite(spriteFighterRenderer, player());
        updateFighterSprite(opponentSpriteRenderer, opponent());
        if (intro && ++introFrame >= introTotalFrames) {
            introFrame = -1;
            flow.startCountdown();
        }
    }

    /** No 2x2 o CPU troca de lutador quando o titular cai: o desenho acompanha. */
    private void updateOpponentVisuals() {
        CharacterDefinition onPoint = opponentPoint().character;
        if (opponentSpriteRenderer.character() == onPoint) return;
        keepFightAtlases();
        opponentSpriteRenderer.setCharacter(onPoint.id);
    }

    /**
     * Arte decodificada da luta: a dupla do jogador e, do CPU, o titular e quem ainda está de pé
     * (no 2x2 o parceiro entra de assist e na troca). Quem caiu sai da memória.
     */
    private void keepFightAtlases() {
        java.util.List<CharacterDefinition> keep = new java.util.ArrayList<>();
        for (FighterState member : team) keep.add(member.character);
        for (FighterState member : engine.team(OPPONENT).members) {
            if (member.life > 0 || member == opponentPoint()) keep.add(member.character);
        }
        atlases.retainOnly(keep.toArray(new CharacterDefinition[0]));
        atlases.preload(keep.toArray(new CharacterDefinition[0]));
    }

    /** Modo batalha: depois do resultado, uma luta nova com as mesmas duplas. */
    private void restartBattle() {
        selectTeams(team[0].character.id, team[1].character.id, opponentFighter.character.id, opponentTagId);
    }

    /** Starts the match intro, as long as the longest INTRO clip of the two fighters. */
    void startIntro() {
        hud.resetEffects();
        impacts.clear();
        feel.clear();
        finalizacao.limpar();
        koVictim = null;
        float longest = 0f;
        for (CombatFighter f : new CombatFighter[] {player(), opponent()}) {
            CharacterDefinition.Animation clip = f.character().animations.get(SpriteStates.INTRO);
            if (clip != null) longest = Math.max(longest, clip.duration);
        }
        introTotalFrames = (int)Math.ceil(longest / FIXED_STEP);
        introFrame = introTotalFrames > 0 ? 0 : -1;
        flow.reset();
        if (introFrame < 0) flow.startCountdown();
    }

    private final HudRenderer.State hudState = new HudRenderer.State() {
        @Override public FighterState active() { return activeFighter(); }
        @Override public FighterState reserve() { return reserveFighter(); }
        @Override public FighterState opponent() { return opponentPoint(); }
        @Override public CharacterDefinition opponentReserveCharacter() {
            FighterState partner = engine.team(OPPONENT).partner();
            return partner != null ? partner.character : GeneratedCharacters.get(opponentTagId);
        }
        @Override public FighterState opponentReserve() { return engine.team(OPPONENT).partner(); }
        @Override public boolean battleActive() { return flow.battle(); }
        @Override public String clockLabel() { return flow.clockLabel(); }
        @Override public boolean clockUrgent() { return flow.clockUrgent(); }
        @Override public float opponentTagReadyRatio() {
            TeamSystem.Side cpu = engine.team(OPPONENT);
            if (!cpu.partnerStanding() || cpu.tagging() || cpu.assistOut()) return 0f;
            return 1f - cpu.assistCooldown / (float)engine.config.assistCooldownFrames;
        }
        @Override public String versionLabel() { return VERSION_HUD_LABEL; }
        @Override public String stateLabel() { return currentStateLabel(); }
        @Override public int facing() { return player().facing; }
        @Override public boolean aiEnabled() { return ai.enabled(); }
        @Override public boolean debugEnabled() { return debugOverlay; }
        @Override public String teamLabel() { return GameView.this.teamLabel(); }
        @Override public float tagReadyRatio() {
            TeamSystem.Side team = engine.team(PLAYER);
            if (isTagAnimationActive() || team.assistOut()) return 0f;
            if (team.assistCooldown > 0) {
                return 1f - team.assistCooldown / (float)engine.config.assistCooldownFrames;
            }
            return 1f;
        }
        @Override public boolean assistInUse() { return isTagAnimationActive() || engine.team(PLAYER).assistOut(); }
        @Override public String tagCooldownLabel() { return tagCooldownHudLabel; }
        @Override public String tagButtonLabel() { return tagCooldownButtonLabel; }
        @Override public String tagButtonTitle() {
            if (engine.teams.guardCancelReady(PLAYER, player())) return "CANCEL";
            if (engine.teams.dhcReady(PLAYER, player())) return "DHC";
            return engine.team(PLAYER).conversionOpen() ? "TROCA" : "ASSIST";
        }
        @Override public boolean canTag() {
            TeamSystem.Side team = engine.team(PLAYER);
            return team.conversionOpen() || engine.teams.canAssist(PLAYER, player()) ||
                engine.teams.guardCancelReady(PLAYER, player()) || engine.teams.dhcReady(PLAYER, player());
        }
        @Override public boolean canSuper() { return superAvailable(); }
        @Override public boolean ultraReady() { return ultraAvailable(); }
        @Override public int dpadDirection() { return pad.direction(); }
        @Override public float overdriveRatio() {
            return engine.team(PLAYER).overdriveFrames / (float)engine.config.overdriveFrames;
        }
        @Override public float overdriveSeconds() { return engine.team(PLAYER).overdriveFrames * FIXED_STEP; }
        @Override public boolean overdriveReady() { return !engine.team(PLAYER).overdriveUsed; }
        @Override public String selectButtonLabel() { return "◀ MENU"; }
        @Override public boolean pressed(ControlsLayout.Control control) {
            switch (control) {
                case LIGHT: return lightPointer != -1;
                case MEDIUM: return mediumPointer != -1;
                case HEAVY: return heavyPointer != -1;
                case COMBO: return comboPointer != -1;
                case TAG: return tagPointer != -1;
                case SUPER: return superPointer != -1;
                case HEAL_PLAYER: return healPlayerPointer != -1;
                case HEAL_OPPONENT: return healOpponentPointer != -1;
                case THROW: return throwPointer != -1;
                case PUSHBLOCK: return pushblockPointer != -1;
                case OVERDRIVE: return overdrivePointer != -1;
                default: return false;
            }
        }
    };

    private CharacterDefinition opponentCharacter() {
        return opponentPoint().character;
    }

    private CharacterDefinition activeCharacter() {
        return activeFighter().character;
    }

    /** Super button lights up when a Super could start now or from the current cancel. */
    private boolean superAvailable() {
        CombatFighter p = player();
        return activeFighter().profile.hasSuperAttack() &&
            activeFighter().superMeter >= CombatConfig.SUPER_COST &&
            !isTagAnimationActive() &&
            (p.canAct() || CancelSystem.canCancel(p, "SUPER", engine.session(OPPONENT)));
    }

    /** ↓ held, three bars and free to act: the SUPER button starts the ultra. */
    private boolean ultraAvailable() {
        CombatFighter p = player();
        return ControlsLayout.isDownDirection(pad.direction()) &&
            activeFighter().superMeter >= CombatConfig.ULTRA_COST &&
            p.canAct() && p.grounded &&
            !isTagAnimationActive();
    }

    /** Who is doing an ultra now (the player or, in battle, the CPU); the player when nobody is. */
    private CombatFighter ultraFighter() {
        if (ultraAttacker >= 0) return engine.fighter(ultraAttacker);
        CombatFighter cpu = opponent();
        return player().status != CombatFighter.Status.ULTRA && cpu.status == CombatFighter.Status.ULTRA ? cpu : player();
    }

    /** Activation and rush of an ultra (engine state), before any cinematic. */
    private boolean ultraWindupActive() {
        CombatFighter p = ultraFighter();
        return p.status == CombatFighter.Status.ULTRA &&
            (p.ultraPhase == CombatFighter.ULTRA_STARTUP || p.ultraPhase == CombatFighter.ULTRA_RUSH);
    }

    private void updateUltraVisuals() {
        ultraShotActive = false;
        ultraTilt = 0f;
        CombatFighter p = ultraFighter();
        int phase = p.status == CombatFighter.Status.ULTRA ? p.ultraPhase : -1;
        if (phase != ultraPhaseSeen) {
            if (phase == CombatFighter.ULTRA_STARTUP) {
                ultraSounds.play(ultraFolder(p.state), "ativacao");
                if (usaPackReal(p.state, p.index)) pedirUltra(p.state, false);
                FighterState rival = engine.fighter(1 - p.index).state;
                if (usaPackReal(rival, 1 - p.index)) pedirUltra(rival, false);
                // Pack ainda carregando: a luta espera parada no começo do Ultra (update).
                if (ultraProntoParaAbertura(p)) {
                    startUltraOpening(p);
                } else {
                    ultraEsperando = p;
                    ultraEsperandoDesde = android.os.SystemClock.uptimeMillis();
                }
            }
            if (phase == CombatFighter.ULTRA_RUSH) ultraSounds.play(ultraFolder(p.state), "investida");
            ultraPhaseSeen = phase;
        }
        if (phase == CombatFighter.ULTRA_STARTUP) {
            float t = clamp(p.ultraFrame / (float)engine.config.ultraStartupFrames, 0f, 1f);
            ultraDarkAlpha = Math.round(205f * Math.min(1f, t * 3f));
            ultraCameraZoom = 1f + ULTRA_STARTUP_ZOOM * (1f - (1f - t) * (1f - t));
            ultraAuraTime = p.ultraFrame * FIXED_STEP;
        } else if (phase == CombatFighter.ULTRA_RUSH) {
            float t = clamp(p.ultraFrame / (float)engine.config.ultraRushFrames, 0f, 1f);
            ultraDarkAlpha = Math.round(205f * (1f - t));
            ultraCameraZoom = 1f + ULTRA_STARTUP_ZOOM * (1f - t);
        } else if (phase == CombatFighter.ULTRA_SEQUENCE) {
            // Sequência do vídeo: um plano de câmera por golpe (UltraDirector), corte seco a cada
            // acerto, fundo escuro; depois do golpe final a câmera da luta volta.
            CharacterDefinition.UltraSequence seq = p.state.profile.ultraSequence;
            if (seq != null) {
                CombatFighter v = engine.fighter(1 - p.index);
                UltraDirector.shot(ultraShot, seq.hits, p.ultraFrame, p.x, chestY(p), v.x, chestY(v), p.facing,
                    ultraScript);
                ultraShotActive = ultraShot.kind != UltraDirector.FINAL;
            } else {
                ultraShotActive = false;
            }
            ultraDarkAlpha = ultraShotActive ? ultraScript.darkAlpha : 0;
            ultraCameraZoom = ultraShotActive ? ultraShot.zoom : 1f;
            ultraTilt = ultraShotActive ? ultraShot.tilt : 0f;
        } else if (phase == CombatFighter.ULTRA_BEAM) {
            // Charge: the camera closes in on the player; the shot opens it back fast.
            int fire = engine.config.ultraBeamFireFrame();
            float t = p.ultraFrame < fire
                ? clamp(p.ultraFrame / (float)fire, 0f, 1f)
                : clamp(1f - (p.ultraFrame - fire) / 8f, 0f, 1f);
            ultraDarkAlpha = 0;
            ultraCameraZoom = 1f + BEAM_CHARGE_ZOOM * (1f - (1f - t) * (1f - t));
        } else {
            ultraDarkAlpha = 0;
            ultraCameraZoom = 1f;
        }
    }

    /** Começa a abertura do Ultra de quem acabou de ativar (a luta para até ela acabar). */
    private void startUltraOpening(CombatFighter p) {
        UltraPack pack = ultraPackOf(p);
        ultraScript = UltraDirector.PADRAO;
        ultraFinalImpact = ImpactFeel.VOID;
        ultraSlowScale = ImpactFeel.ULTRA_SLOW_SCALE;
        if (pack == null) return;
        // A direção de cena deste Ultra: câmera da sequência e impacto do golpe final.
        com.gamelutagpt.ultra.UltraDefinition.Style style = pack.definition.style;
        ultraScript = UltraDirector.script(style.camera, style.shots);
        if ("vermelho".equalsIgnoreCase(style.impact)) ultraFinalImpact = ImpactFeel.RED;
        if (style.slowMotion > 0f) ultraSlowScale = Math.max(0.1f, Math.min(1f, style.slowMotion));
        // Na abertura dividida o rival aparece do outro lado: o corte dele (ou a arte de seleção).
        UltraPack rival = ultraPackOf(engine.fighter(1 - p.index));
        abertura.start(pack, p.facing, rival != null ? rival.cutIn : null);
        ultraSounds.playFight("punish", 1f, 0.9f);
    }

    /** O pack do Ultra de quem está na luta (o do time do jogador ou o do CPU). */
    private UltraPack ultraPackOf(CombatFighter f) {
        return f == engine.fighter(PLAYER) ? ultraPronto(f.state.character.id) : cpuUltraPack(f.state);
    }

    /** Faixas pretas do Ultra: entram rápido na ativação e saem quando a luta volta. */
    private void updateUltraBars() {
        boolean ultra = false;
        for (int side = 0; side < 2; side++) {
            CombatFighter f = engine.fighter(side);
            if (f.status == CombatFighter.Status.ULTRA && f.ultraPhase != CombatFighter.ULTRA_RECOVERY) ultra = true;
        }
        boolean on = ultra || abertura.isActive() || paginaFinal.isActive();
        ultraBars = on ? Math.min(1f, ultraBars + FIXED_STEP / 0.15f) : Math.max(0f, ultraBars - FIXED_STEP / 0.3f);
    }

    private void beginUltraCinematic(int attacker) {
        ultraAttacker = attacker;
        ultraDamageDealt = 0;
        ultraDarkAlpha = 0;
        ultraCameraZoom = 1f;
        FighterState state = attacker == PLAYER ? activeFighter() : opponentPoint();
        // A abertura já esperou o pack; sem abertura (raro), espera aqui.
        if (usaPackReal(state, attacker)) resultado(pedirUltra(state, true));
        ultraPack = attacker == PLAYER ? ultraPronto(state.character.id) : cpuUltraPack(state);
        paginaFinal.start(ultraPack, engine.fighter(attacker).facing);
    }

    /** O pack do Ultra do CPU: o do time do jogador se é o mesmo personagem, o carregado na luta, ou o provisório. */
    private UltraPack cpuUltraPack(FighterState state) {
        UltraPack pack = teamUltraPack(state.character.id);
        if (pack == null && flow.battle()) pack = ultraPronto(state.character.id);
        return pack != null ? pack : UltraPack.placeholder("ULTRA " + state.character.displayName, state.profile.color);
    }

    private String ultraAttackerFolder() {
        return ultraAttacker == PLAYER ? ultraFolder(activeFighter()) : ultraFolder(opponentPoint());
    }

    private int ultraTotalDamage() {
        // Only called during the cinematic, which always runs with its pack.
        return ultraPack.definition.damage;
    }

    @Override
    public void onUltraHit(int hitIndex, float damageFraction) {
        int dealt = engine.applyUltraHit(ultraAttacker, Math.round(ultraTotalDamage() * damageFraction));
        ultraDamageDealt += dealt;
        // Os acertos da página saem fora do passo da luta: o K.O. aqui não passa pelos eventos.
        CombatFighter victim = engine.fighter(1 - ultraAttacker);
        if (dealt > 0 && victim.ko()) pedirFinalizacao(FinalizacaoManga.ULTRA, ultraAttacker, victim.index);
    }

    /**
     * O golpe {@code tipo} de {@code attacker} derrubou {@code defender}: se o time dele caiu
     * inteiro (matou e ganhou a luta) com Super ou Ultra, a finalização de mangá.
     */
    private void pedirFinalizacao(int tipo, int attacker, int defender) {
        if (tipo == FinalizacaoManga.NENHUM || !engine.teams.defeated(defender)) return;
        finalizacao.pedir(tipo, defender, attacker == PLAYER ? activeFighter().profile.color : opponentPoint().profile.color);
        // K.O. no meio do Ultra (na página, num golpe da sequência, num acerto do raio): espera
        // o golpe final dele, que é o que fecha a luta na tela.
        if (tipo == FinalizacaoManga.ULTRA && ultraEmAndamento()) finalizacao.segurar();
    }

    /** Relâmpagos do cenário já ouvidos e quadros até o trovão do último. */
    private int relampagosOuvidos, trovaoEm = -1;
    private com.gamelutagpt.stage.StageScene trovaoCena;

    /** Relâmpago novo no cenário: o trovão chega um pouco depois do clarão, como de verdade. */
    private void updateTrovao() {
        int strikes = stageScene.strikes();
        if (stageScene != trovaoCena) {
            // Cenário novo: a contagem dele começa de novo, sem trovão atrasado do anterior.
            trovaoCena = stageScene;
            relampagosOuvidos = strikes;
            trovaoEm = -1;
        }
        if (strikes != relampagosOuvidos) {
            relampagosOuvidos = strikes;
            trovaoEm = 12 + soundRandom.nextInt(36);
        }
        if (trovaoEm >= 0 && trovaoEm-- == 0) {
            ultraSounds.playFight("trovao", 0.55f, 0.9f + soundRandom.nextFloat() * 0.2f);
        }
    }

    /** Algum lado está no Ultra: na página, no raio ou nos golpes da sequência. */
    private boolean ultraEmAndamento() {
        if (ultraAttacker >= 0 || paginaFinal.isActive()) return true;
        for (int side = 0; side < 2; side++) {
            CombatFighter f = engine.fighter(side);
            if (f.status == CombatFighter.Status.ULTRA || f.firingBeam() || f.inUltraSequence()) return true;
        }
        return false;
    }

    @Override
    public void onUltraSound(String soundId) {
        ultraSounds.play(ultraAttackerFolder(), soundId);
    }

    @Override
    public void onUltraFinished(UltraGrade grade) {
        if (ultraAttacker >= 0 && engine.ultraWaitingPage(ultraAttacker)) {
            // Ultra em sequência: a página quebra e o golpe final sai na luta, com a nota.
            engine.resumeUltraSequence(ultraAttacker, grade.finalBonus);
            ultraAttacker = -1;
            ultraPack = null;
            return;
        }
        // The page breaks into the fight with the attacker firing the final beam.
        int beamDamage = Math.round(ultraTotalDamage() * PaginaFinal.beamFraction(grade));
        engine.startUltraBeam(ultraAttacker, beamDamage, ultraPack.definition.beam.hits);
        beamClock = 0f;
        beamLastHitClock = -1f;
        beamBlastClock = -1f;
        beamHitsSeen = 0;
        beamFired = false;
        beamShake = 10f;
        CombatFighter target = engine.fighter(1 - ultraAttacker);
        beamTargetX = target.x - engine.fighter(ultraAttacker).facing * target.body().halfWidth * 0.4f;
        ultraSounds.play(ultraAttackerFolder(), "carga");
        if (ultraAttacker == PLAYER) {
            dummyDamageLabel = "-" + ultraDamageDealt;
            dummyDamageLabelFrames = DAMAGE_LABEL_FRAMES;
        }
    }

    /** Follows the engine's beam: hit pulses, the blast, the shake and the end of the ultra. */
    private void updateUltraBeam() {
        beamShake = Math.max(0f, beamShake - FIXED_STEP * 60f);
        if (ultraAttacker < 0 || paginaFinal.isActive()) return;
        CombatFighter a = engine.fighter(ultraAttacker);
        if (!a.firingBeam()) {
            ultraAttacker = -1;
            ultraPack = null;
            return;
        }
        beamClock += FIXED_STEP;
        if (!beamFired) {
            if (a.ultraFrame >= engine.config.ultraBeamFireFrame()) {
                // "HA!": the beam leaves the hands.
                beamFired = true;
                beamShake = 16f;
                ultraSounds.play(ultraAttackerFolder(), "raio");
            } else {
                beamShake = Math.max(beamShake, 3f);
            }
        }
        if (a.beamHitsDone != beamHitsSeen) {
            beamHitsSeen = a.beamHitsDone;
            if (beamHitsSeen > a.beamHits) {
                beamBlastClock = beamClock;
                // A explosão do raio é o golpe final: a finalização de mangá entra aqui.
                finalizacao.liberar();
                beamShake = 26f;
                ultraSounds.play(ultraAttackerFolder(), "explosao");
                // A explosão do raio é o último dano do Ultra: impact frame em quem apanhou.
                CombatFighter d = engine.fighter(1 - ultraAttacker);
                if (pausa.impacto()) feel.impactFrame(ultraFinalImpact, d.x, chestY(d));
            } else {
                beamLastHitClock = beamClock;
                beamShake = Math.max(beamShake, 5f);
            }
        }
        if (beamBlastClock < 0f) {
            CombatFighter d = engine.fighter(1 - ultraAttacker);
            beamTargetX = d.x - a.facing * d.body().halfWidth * 0.4f;
        }
    }

    /** Quem apanha do Ultra em andamento (o outro de quem está no Ultra), ou null. */
    private CombatFighter ultraVictim() {
        for (int side = 0; side < 2; side++) {
            if (engine.fighter(side).status == CombatFighter.Status.ULTRA) return engine.fighter(1 - side);
        }
        return null;
    }

    private boolean ultraBeamActive() {
        return ultraAttacker >= 0 && !paginaFinal.isActive() && engine.fighter(ultraAttacker).firingBeam();
    }

    /** Beam state for this frame, in world coordinates (from the engine and the ultra.json). */
    private RaioFinal.Frame ultraBeamFrame() {
        CombatFighter a = engine.fighter(ultraAttacker);
        CombatConfig config = engine.config;
        float height = a.character().visualStandHeight;
        UltraDefinition.Beam beam = ultraPack.definition.beam;
        RaioFinal.Frame f = beamFrame;
        f.bodyX = a.x;
        f.groundY = a.y;
        f.bodyHeight = height;
        f.facing = a.facing;
        // Hands from the ultra.json (measured on the beam sprite); without it, the heavy
        // straight: arm pulled back while charging, extended fist when firing.
        float chargeAhead = beam.chargeHands != null ? beam.chargeHands[0] : height * 0.13f;
        float chargeUp = beam.chargeHands != null ? beam.chargeHands[1] : height * 0.53f;
        float fireAhead = beam.fireHands != null ? beam.fireHands[0] : height * 0.68f;
        float fireUp = beam.fireHands != null ? beam.fireHands[1] : height * 0.67f;
        f.chargeX = a.x + a.facing * chargeAhead;
        f.chargeY = a.y - chargeUp;
        f.originX = a.x + a.facing * fireAhead;
        f.originY = a.y - fireUp;
        f.targetX = beamTargetX;
        f.time = beamClock;
        int fire = config.ultraBeamFireFrame();
        f.charge = clamp(a.ultraFrame / (float)fire, 0f, 1f);
        f.sinceFire = a.ultraFrame >= fire ? (a.ultraFrame - fire) * FIXED_STEP : -1f;
        float extend = clamp((a.ultraFrame - fire) / (float)config.ultraBeamExtendFrames, 0f, 1f);
        f.reach = 1f - (1f - extend) * (1f - extend);
        f.hits = beamHitsSeen;
        f.sinceHit = beamLastHitClock >= 0f ? beamClock - beamLastHitClock : -1f;
        f.sinceBlast = beamBlastClock >= 0f ? beamClock - beamBlastClock : -1f;
        int afterBlast = a.ultraFrame - config.ultraBeamBlastFrame(a.beamHits);
        f.strength = afterBlast <= 0 ? 1f : 1f - afterBlast / (float)config.ultraBeamFadeFrames;
        return f;
    }

    /** Darkens the world while the beam charges and fires, and flashes white on the blast. */
    private void drawUltraBeamOverlay(Canvas canvas, boolean flash) {
        CombatFighter a = engine.fighter(ultraAttacker);
        int afterBlast = a.ultraFrame - engine.config.ultraBeamBlastFrame(a.beamHits);
        float fade = afterBlast <= 0 ? 1f : clamp(1f - afterBlast / (float)engine.config.ultraBeamFadeFrames, 0f, 1f);
        fade *= clamp(a.ultraFrame / 12f, 0f, 1f);
        if (!flash) {
            effects.drawWorldOverlay(canvas, paint, Math.round(110f * fade), 8, 6, 20);
        } else if (beamBlastClock >= 0f) {
            float since = beamClock - beamBlastClock;
            effects.drawWorldOverlay(canvas, paint, Math.round(230f * clamp(1f - since / 0.22f, 0f, 1f)), 255, 250, 225);
        }
    }

    /**
     * Super: the stage darkens behind both fighters (they stay lit; the one held by the freeze
     * dims in {@link #drawFighter}) and the charge aura grows behind whoever activated it.
     */
    private void drawSuperDarkness(Canvas canvas) {
        effects.drawWorldOverlay(canvas, paint, superFlash.darkAlpha, 0, 0, 8);
        CombatFighter u = engine.fighter(superFlash.user);
        float x = u.x + (u.index == PLAYER ? tagOffset() : engine.teams.tagOffset(OPPONENT));
        canvas.save();
        if (u.facing < 0) canvas.scale(-1f, 1f, x, 0f);
        effects.drawSuperCharge(canvas, paint, x, u.y, superFlash.chargeTime, u.state.profile.color);
        canvas.restore();
    }

    /** Levels of the two Super looks: the activation glow and the dim of whoever is held. */
    private static final int SUPER_LOOK_LEVELS = 8;
    private final ColorFilter[] frozenLooks = new ColorFilter[SUPER_LOOK_LEVELS + 1];
    private final ColorFilter[] signalLooks = new ColorFilter[SUPER_LOOK_LEVELS + 1];
    private int signalLookColor;

    /**
     * Super look of a fighter body, or null. Who activated glows in its color for the first
     * frames; who is held by the freeze turns darker and colder (still readable, not stuck).
     */
    private ColorFilter superLook(CombatFighter f) {
        if (f == null || !superFlash.active() && superFlash.frozen < 0) return null;
        if (superFlash.active() && f == engine.fighter(superFlash.user) && superFlash.signal > 0f) {
            int color = f.state.profile.color;
            if (color != signalLookColor) {
                java.util.Arrays.fill(signalLooks, null);
                signalLookColor = color;
            }
            int level = Math.round(superFlash.signal * SUPER_LOOK_LEVELS);
            if (level == 0) return null;
            if (signalLooks[level] == null) {
                // Lighter toward the fighter's color: the body flares, the art still shows.
                int a = Math.round(200f * level / SUPER_LOOK_LEVELS);
                int glow = Color.argb(a, (Color.red(color) + 255 * 2) / 3, (Color.green(color) + 255 * 2) / 3,
                    (Color.blue(color) + 255 * 2) / 3);
                signalLooks[level] = new PorterDuffColorFilter(glow, PorterDuff.Mode.SRC_ATOP);
            }
            return signalLooks[level];
        }
        if (superFlash.frozen >= 0 && f == engine.fighter(superFlash.frozen)) {
            int level = Math.round(superFlash.frozenDim * SUPER_LOOK_LEVELS);
            if (level == 0) return null;
            if (frozenLooks[level] == null) {
                float k = level / (float)SUPER_LOOK_LEVELS;
                float keep = 1f - 0.5f * k, grey = 0.35f * k;
                // Half the light and a third of the color gone, with a cold tint.
                float r = keep * (1f - grey), o = keep * grey / 2f;
                frozenLooks[level] = new ColorMatrixColorFilter(new ColorMatrix(new float[] {
                    r, o, o, 0f, 0f,
                    o, r, o, 0f, 4f * k,
                    o, o, r, 0f, 18f * k,
                    0f, 0f, 0f, 1f, 0f}));
            }
            return frozenLooks[level];
        }
        return null;
    }

    private boolean isSuperCinematicActive() {
        return superFlash.active();
    }

    /**
     * The Super cinematic follows whoever activated it. On activation: the glint at the chest and
     * the sound, the warning that the Super is coming before the screen holds still.
     */
    private void updateSuperVisuals() {
        superFlash.update(engine);
        if (!superFlash.started) return;
        CombatFighter f = engine.fighter(superFlash.user);
        float x = f.x + (f.index == PLAYER ? tagOffset() : engine.teams.tagOffset(OPPONENT));
        impacts.spawnSuperGlint(x, chestY(f), f.state.profile.color);
        ultraSounds.playFight("super", 1f, 1f);
    }

    /**
     * Spark, ring and debris at the contact point, the hit sound, and for heavier blows the
     * camera punch and shake; the KO blow adds the flash and the slow motion.
     */
    private void spawnImpact(CombatEngine.HitEvent event, boolean ultraHit, boolean punish, boolean knockout) {
        CombatFighter attacker = engine.fighter(event.attacker), defender = engine.fighter(event.defender);
        float x = event.impactX, y = event.impactY;
        if (Float.isNaN(x) || Float.isNaN(y)) {
            // Throws and the ultra have no hitbox: use the defender's chest.
            int side = defender.x >= attacker.x ? -1 : 1;
            x = defender.x + side * defender.body().halfWidth * .5f;
            y = defender.y - defender.body().height(defender.crouchingBody()) * .6f;
        }
        int direction = defender.x >= attacker.x ? 1 : -1;
        int size = ultraHit ? ImpactEffects.SPECIAL : ImpactEffects.sizeOf(event.strength, event.damage);
        if (event.projectile) size = Math.max(size, ImpactEffects.MEDIUM);
        int accent = event.attacker == PLAYER ? activeFighter().profile.color : opponentPoint().profile.color;
        impacts.spawn(event.blocked ? ImpactEffects.GUARD : ImpactEffects.HIT, x, y, direction, size, accent);
        if (!event.blocked && size >= ImpactEffects.HEAVY) beamShake = Math.max(beamShake, size == ImpactEffects.HEAVY ? 7f : 10f);
        else if (!event.blocked && size == ImpactEffects.MEDIUM) beamShake = Math.max(beamShake, 3f);
        if (knockout) beamShake = Math.max(beamShake, 18f);
        if (!event.blocked) feel.onHit(x, y, size, punish && size >= ImpactEffects.HEAVY, knockout);
        // A plateia do cenário vibra com o golpe (forte no pesado, festa no KO).
        if (!event.blocked && stageScene != null) {
            stageScene.cheer(knockout ? 1f : size >= ImpactEffects.HEAVY ? 0.65f : size == ImpactEffects.MEDIUM ? 0.35f : 0.15f);
        }
        if (!event.blocked && pausa.impacto()) feel.impactFrame(impactStyleFor(event, defender, size, punish, knockout), x, y);
        if (!event.blocked && !ultraHit && ultraAttacker < 0 && !isSuperCinematicActive() &&
            (knockout || bigBlow(attacker, defender))) {
            // O H que joga na parede ganha um close mais aberto que o lançador e o KO.
            boolean wall = !knockout && attacker.attack != null &&
                attacker.attack.launch == AttackDefinition.Launch.WALL_BOUNCE;
            feel.cinema(x, y, direction, knockout, wall ? ImpactFeel.CINEMA_ZOOM_WALL : ImpactFeel.CINEMA_ZOOM);
        }
        String sound = event.blocked ? "defesa"
            : knockout ? "ko"
            : punish && size >= ImpactEffects.HEAVY ? "punish"
            : HIT_SOUNDS[Math.max(0, Math.min(HIT_SOUNDS.length - 1, size))];
        ultraSounds.playFight(sound, knockout ? 1f : 0.85f, 0.94f + soundRandom.nextFloat() * 0.12f);
    }

    /**
     * Dois projéteis se anularam: uma faísca de cada lado, cada uma na cor de quem lançou e
     * voando no sentido do próprio projétil, como se as duas energias se empurrassem.
     */
    /** A defesa estilhaçou: estalo no peito de quem quebrou, tremida e o som dos cacos. */
    private void spawnGuardBreak() {
        for (int side = 0; side < 2; side++) {
            CombatFighter broken = engine.fighter(side);
            if (!broken.guardBroken || broken.stunElapsed > 0) continue;
            CombatFighter other = engine.fighter(1 - side);
            int direction = broken.x >= other.x ? 1 : -1;
            float x = broken.x - direction * broken.body().halfWidth * .4f, y = chestY(broken);
            impacts.spawn(ImpactEffects.GUARD, x, y, direction, ImpactEffects.SPECIAL, 0xFFFFD84A);
            impacts.spawn(ImpactEffects.HIT, x, y, direction, ImpactEffects.HEAVY, other.state.profile.color);
        }
        beamShake = Math.max(beamShake, 14f);
        ultraSounds.playFight("guarda_quebrada", 1f, 1f);
    }

    /** Defesa perfeita: um brilho branco no peito de quem defendeu e o som da defesa mais agudo. */
    private void spawnJustDefense() {
        for (int side = 0; side < 2; side++) {
            CombatFighter f = engine.fighter(side);
            if (!f.justDefended || f.status != CombatFighter.Status.BLOCKSTUN || f.stunElapsed > 0) continue;
            impacts.spawnSuperGlint(f.x, chestY(f), 0xFFFFFFFF);
        }
        ultraSounds.playFight("defesa", 1f, 1.45f);
    }

    /**
     * Combo breaker: brilho na cor de quem quebrou no peito dele, estalo em quem teve o combo
     * quebrado, tremida e o som do punish.
     */
    static final int BREAKER_SLOW_FRAMES = 36;
    static final float BREAKER_SLOW_SCALE = 0.5f;

    private void spawnComboBreaker() {
        for (int side = 0; side < 2; side++) {
            CombatFighter breaker = engine.fighter(side), broken = engine.fighter(1 - side);
            if (!breaker.comboBroke || !broken.inHitstun() || broken.stunElapsed > 0) continue;
            breakerSkid = 1 - side;
            int accent = breaker.state.profile.color;
            impacts.spawnSuperGlint(breaker.x, chestY(breaker), accent);
            int direction = broken.x >= breaker.x ? 1 : -1;
            impacts.spawn(ImpactEffects.HIT, broken.x, chestY(broken), direction, ImpactEffects.SPECIAL, accent);
        }
        beamShake = Math.max(beamShake, 16f);
        ultraSounds.playFight("punish", 1f, 1.15f);
        // Câmera lenta leve: dá para ver o que aconteceu (o golpe que não entrou e o estalo).
        feel.slow(BREAKER_SLOW_FRAMES, BREAKER_SLOW_SCALE);
    }

    /** Quem teve o combo quebrado e está deslizando para trás (fumacinha do pé), ou -1. */
    private int breakerSkid = -1;

    /** Enquanto o estalo do combo breaker o empurra no chão, o pé arrasta e solta fumacinha. */
    private void updateBreakerSkid() {
        if (breakerSkid < 0) return;
        CombatFighter f = engine.fighter(breakerSkid);
        if (f.frozen()) return;
        if (f.pushFramesLeft <= 0 || !f.inHitstun()) {
            breakerSkid = -1;
            return;
        }
        if (!f.grounded || f.pushFramesLeft % 2 != 0) return;
        float offset = breakerSkid == PLAYER ? tagOffset() : engine.teams.tagOffset(OPPONENT);
        int direction = f.pushRemaining >= 0f ? 1 : -1;
        // Os dois pés: o de trás vai na frente do deslize, o da frente fica para trás.
        impacts.spawnSkid(f.x + offset + direction * 16f, GROUND_Y, direction);
        impacts.spawnSkid(f.x + offset - direction * 18f, GROUND_Y, direction);
    }

    /**
     * Golpe alto saindo: um brilho pequeno e curto perto da cabeça de quem bate (dá para ler e
     * ficar em pé). O aviso principal é a pose de preparo segurada.
     */
    private void spawnOverheadWarning() {
        for (int side = 0; side < 2; side++) {
            CombatFighter f = engine.fighter(side);
            if (!f.attacking() || !f.attack.overhead || f.attackFrame > 1) continue;
            impacts.spawnWarningGlint(f.x + f.facing * f.body().halfWidth * .4f, visualTop(f) + 24f, 0xFFFFE9A0);
        }
    }

    private void spawnClash(CombatEngine.ProjectileClash clash) {
        int size = clash.big ? ImpactEffects.SPECIAL : ImpactEffects.MEDIUM;
        impacts.spawn(ImpactEffects.HIT, clash.x, clash.y, clash.firstDirection, size, clash.firstColor);
        impacts.spawn(ImpactEffects.HIT, clash.x, clash.y, -clash.firstDirection, size, clash.secondColor);
        beamShake = Math.max(beamShake, clash.big ? 7f : 3f);
        ultraSounds.playFight(clash.big ? "acerto_s" : "acerto_m", 0.8f, 0.94f + soundRandom.nextFloat() * 0.12f);
    }

    /**
     * Golpe que ganha o close de cinema: o lançador (manda o rival para o alto) e o que o joga
     * na parede ou no chão.
     */
    private static boolean bigBlow(CombatFighter attacker, CombatFighter defender) {
        if (!attacker.attacking() || attacker.attack == null) return false;
        AttackDefinition.Launch launch = attacker.attack.launch;
        if (launch == AttackDefinition.Launch.LAUNCH) return !defender.grounded && !defender.spinning;
        boolean smash = launch == AttackDefinition.Launch.WALL_BOUNCE || launch == AttackDefinition.Launch.GROUND_BOUNCE ||
            launch == AttackDefinition.Launch.SLAM;
        return smash && (defender.bounce != CombatFighter.BOUNCE_NONE || defender.slammed);
    }

    /** Combo a partir deste tamanho: o golpe que o encerra derrubando ganha o impact frame. */
    private static final int IMPACT_COMBO_HITS = 5;

    /**
     * Impact frame de um acerto: VOID no KO e no Super, RED no punish counter e no golpe que
     * fecha um combo longo jogando o rival longe (queda, parede, chão, giro); NONE no resto.
     */
    private int impactStyleFor(CombatEngine.HitEvent event, CombatFighter defender, int size,
                               boolean punish, boolean knockout) {
        if (knockout || event.strength == AttackDefinition.Strength.SUPER) return ImpactFeel.VOID;
        if (punish && size >= ImpactEffects.HEAVY) return ImpactFeel.RED;
        boolean sentFlying = defender.status == CombatFighter.Status.KNOCKDOWN || defender.hardFall ||
            defender.spinning || defender.bounce != CombatFighter.BOUNCE_NONE;
        if (!sentFlying) return ImpactFeel.NONE;
        ComboSession combo = engine.session(event.defender);
        if (combo == null && engine.framesSinceSessionEnd(event.defender) == 0) combo = engine.lastSession(event.defender);
        return combo != null && combo.hitCount >= IMPACT_COMBO_HITS ? ImpactFeel.RED : ImpactFeel.NONE;
    }

    private static final String[] HIT_SOUNDS = {"acerto_l", "acerto_m", "acerto_h", "acerto_s"};
    private static final String[] SWING_SOUNDS = {"ar_l", "ar_m", "ar_h"};

    /** O "vush" de cada golpe pouco antes de ele bater, e o baque de quem cai no chão. */
    private void playMotionSounds() {
        for (int i = 0; i < 2; i++) {
            CombatFighter f = engine.fighter(i);
            if (f.attacking() && f.attack.kind == AttackDefinition.Kind.NORMAL) {
                if (f.attack != swingAttack[i] || f.attackFrame < swingFrame[i]) swingPlayed[i] = false;
                swingAttack[i] = f.attack;
                swingFrame[i] = f.attackFrame;
                if (!swingPlayed[i] && f.attackFrame >= Math.max(0, f.attack.startupFrames - 2)) {
                    swingPlayed[i] = true;
                    int weight = f.attack.strength == AttackDefinition.Strength.LIGHT ? 0
                        : f.attack.strength == AttackDefinition.Strength.MEDIUM ? 1 : 2;
                    ultraSounds.playFight(SWING_SOUNDS[weight], 0.5f, 0.92f + soundRandom.nextFloat() * 0.16f);
                }
            } else {
                swingAttack[i] = null;
            }
            boolean down = f.status == CombatFighter.Status.KNOCKDOWN;
            if (down && !downSeen[i]) {
                // Bateu no chão: poeira e pedrinhas, mais forte quando vem do alto.
                impacts.spawnDust(f.x + (i == PLAYER ? tagOffset() : engine.teams.tagOffset(OPPONENT)), GROUND_Y,
                    wasAirborne[i] ? 1f : 0.45f);
            }
            downSeen[i] = down;
            wasAirborne[i] = !f.grounded;
            if (f.superJumping && !superJumpSeen[i]) {
                // Saída do super pulo: anel de choque e poeira nos pés, na cor do lutador.
                impacts.spawnSuperJump(f.x + (i == PLAYER ? tagOffset() : engine.teams.tagOffset(OPPONENT)),
                    GROUND_Y, f.state.profile.color);
            }
            superJumpSeen[i] = f.superJumping;
            boolean lying = f.status == CombatFighter.Status.KNOCKDOWN &&
                f.knockdownFrame >= engine.config.knockdownFallFrames;
            if (lying && !lyingSeen[i]) ultraSounds.playFight("queda", 0.8f, 0.95f + soundRandom.nextFloat() * 0.1f);
            lyingSeen[i] = lying;
        }
    }

    private void showBanner(String text) {
        throwBanner = text;
        throwBannerFrames = THROW_BANNER_FRAMES;
    }

    /** Call-out for a system notice of the engine (throw tech, active defense), or null. */
    private static String bannerFor(String cue) {
        switch (cue) {
            case "TECH": return "TECH!";
            case "AIR_TECH": return "AIR TECH!";
            case "PUSHBLOCK": return "PUSHBLOCK!";
            case "GUARD_CANCEL": return "GUARD CANCEL!";
            case "GUARD_BREAK": return "GUARD BREAK!";
            case "JUST_DEFENSE": return "DEFESA PERFEITA!";
            case "COMBO_BREAKER": return "COMBO BREAKER!";
            case "DHC": return "TEAM SUPER!";
            case "OVERDRIVE": return "OVERDRIVE!";
            case "COUNTER": return "CONTRA-GOLPE!";
            case "PUNISH": return "PUNISH COUNTER!";
            case "ARMOR": return "ARMADURA!";
            case "POISON": return "VENENO!";
            case "CHARGED": return "ONDA CARREGADA!";
            case "ECHO": return "ECO DE SOMBRA!";
            case "HENCHMEN": return "COBRANÇA!";
            case "CLONES": return "LEGIÃO DE SOMBRAS!";
            default: return null;
        }
    }

    /** A troca do jogador no desenho (a voadora da entrada já é luta: o CPU defende ou tira do ar). */
    private boolean isTagAnimationActive() {
        return engine.team(PLAYER).tagAnimating();
    }

    /** Sprite offset of the point during a raw tag (it runs off screen and back). */
    private float tagOffset() {
        return engine.teams.tagOffset(PLAYER);
    }

    /** After the engine's team step: point sprite, partner sprite and the HUD labels. */
    private void updateTeamVisuals() {
        TeamSystem.Side team = engine.team(PLAYER);
        if (team.point != shownPoint) {
            shownPoint = team.point;
            spriteFighterRenderer.setCharacter(activeCharacter().id);
        }
        CombatFighter partner = partnerBody(team);
        if (partner != null) {
            if (!partnerSpriteRenderer.character().id.equals(partner.state.character.id)) {
                partnerSpriteRenderer.setCharacter(partner.state.character.id);
            }
            updateFighterSprite(partnerSpriteRenderer, partner);
        }
        CombatFighter cpuPartner = partnerBody(engine.team(OPPONENT));
        if (cpuPartner != null) {
            if (opponentPartnerRenderer.character() != cpuPartner.state.character) {
                opponentPartnerRenderer.setCharacter(cpuPartner.state.character.id);
            }
            updateFighterSprite(opponentPartnerRenderer, cpuPartner);
        }
        updateTagCooldownLabels(team);
    }

    /** The partner's body on screen (assist or the former point leaving), or null. */
    private CombatFighter partnerBody(TeamSystem.Side team) {
        if (team.assist != null) return team.assist;
        if (team.leaving == null || team.leavingFrame < 0) return null;
        CombatFighter f = team.index == PLAYER ? leavingBody : opponentLeavingBody;
        float x = engine.teams.leavingX(team.index);
        f.travel = x - f.x;
        f.state = team.leaving;
        f.x = x;
        f.y = team.leavingY;
        f.facing = team.leavingFacing;
        f.grounded = true;
        f.status = CombatFighter.Status.NEUTRAL;
        return f;
    }

    private void updateTagCooldownLabels(TeamSystem.Side team) {
        int assistTenths = team.assistOut() ? -1 : (int)Math.ceil(team.assistCooldown / 6f);
        int tagSeconds = (int)Math.ceil(team.tagCooldown / (float)CombatConfig.FPS);
        if (isTagAnimationActive()) {
            tagCooldownHudLabel = "TROCA: EM ANDAMENTO";
            tagCooldownButtonLabel = "...";
            shownAssistTenths = shownTagSeconds = -2;
            return;
        }
        if (assistTenths == shownAssistTenths && tagSeconds == shownTagSeconds) return;
        shownAssistTenths = assistTenths;
        shownTagSeconds = tagSeconds;
        String assist = assistTenths < 0 ? (team.conversionOpen() ? "ASSIST: TAG p/ TROCAR" : "ASSIST: EM CAMPO")
            : assistTenths == 0 ? "ASSIST: PRONTO"
            : String.format(Locale.US, "ASSIST: %.1fs", assistTenths / 10f);
        String tag = tagSeconds == 0 ? "  •  \u2193+TAG: TROCA" : "  •  TROCA: " + tagSeconds + "s";
        tagCooldownHudLabel = assist + tag;
        tagCooldownButtonLabel = assistTenths > 0 ? Integer.toString((assistTenths + 9) / 10) : "";
    }

    /** A IA ligada é o modo batalha (2x2, derrota quando a dupla cai); desligada, o treino. */
    private void setOpponentAiEnabled(boolean enabled) {
        ai.setEnabled(enabled);
        // Na batalha o CPU usa o kit inteiro: assist, troca, DHC, Guard Cancel, Overdrive, Ultra.
        ai.setFullKit(enabled);
        flow.setBattle(enabled);
    }

    private void updateFightCamera(float dt) {
        // Frame what is actually drawn: opaque sprite height measured at import time.
        float highestFighterTop = Math.min(visualTop(player()), visualTop(opponent()));
        // KO leaves the winner's world X fixed, so its screen X must also
        // be fixed. Continue to update the camera only while BOTH fight.
        camera.updateForRound(dt, player().x, opponent().x, highestFighterTop,
            player().superJumping || opponent().superJumping,
            player().ko() || opponent().ko());
    }

    /** Highest opaque pixel of a fighter, for camera framing and the opponent HUD. */
    private static float visualTop(CombatFighter f) {
        CharacterDefinition c = f.character();
        return f.y - (f.crouchingBody() ? c.visualCrouchHeight : c.visualStandHeight);
    }

    /** Altura do peito de um lutador (onde o close da cinemática se apoia). */
    /**
     * No super pulo a câmera sobe com quem pulou e quem ficou no chão some por baixo: uma seta
     * na borda de baixo da tela, na cor dele, marca onde ele está (e acompanha quando anda).
     */
    private void drawGroundMarkers(Canvas canvas, float zoom, float left, float top) {
        for (int i = 0; i < 2; i++) {
            CombatFighter f = engine.fighter(i);
            CombatFighter other = engine.fighter(1 - i);
            if (!other.superJumping || !f.grounded || f.ko()) continue;
            // Só quando o peito dele já saiu da tela por baixo.
            float chestScreenY = (chestY(f) - top) * zoom;
            if (chestScreenY < VH - 8f) continue;
            float x = f.x + (i == PLAYER ? tagOffset() : engine.teams.tagOffset(OPPONENT));
            float sx = clamp((x - left) * zoom, 44f, VW - 44f);
            float pulse = 0.5f + 0.5f * (float)Math.sin(engine.frame() * 0.25f);
            // Logo acima da faixa das barras de super (embaixo da tela).
            float tipY = VH - GROUND_MARKER_BOTTOM - 6f * pulse, size = 26f;
            int color = f.state.profile.color | 0xFF000000;
            groundMarker.reset();
            groundMarker.moveTo(sx - size, tipY - size * 1.15f);
            groundMarker.lineTo(sx + size, tipY - size * 1.15f);
            groundMarker.lineTo(sx, tipY);
            groundMarker.close();
            paint.setShader(null);
            // Brilho em volta, a seta na cor dele e o contorno branco.
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            paint.setAlpha(Math.round(70 + 60 * pulse));
            canvas.drawCircle(sx, tipY - size * 0.6f, size * 1.6f, paint);
            paint.setAlpha(255);
            canvas.drawPath(groundMarker, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f);
            paint.setColor(0xFFFFFFFF);
            canvas.drawPath(groundMarker, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    /**
     * Guarda de cada lutador, uma barrinha embaixo dos pés: só aparece quando ela não está cheia.
     * Amarela; vermelha e piscando quando está quase quebrando; no guard break, o contorno
     * vermelho vazio piscando.
     */
    private void drawGuardGauges(Canvas canvas, float zoom, float left, float top) {
        int warning = engine.config.guardWarningPermille * CombatConfig.GUARD_MAX / 1000;
        boolean blink = (engine.frame() / 4) % 2 == 0;
        for (int i = 0; i < 2; i++) {
            CombatFighter f = engine.fighter(i);
            int guard = f.state.guard;
            if (f.ko() || (guard >= CombatConfig.GUARD_MAX && !f.guardBroken)) continue;
            float x = f.x + (i == PLAYER ? tagOffset() : engine.teams.tagOffset(OPPONENT));
            float sx = clamp((x - left) * zoom, GUARD_GAUGE_W, VW - GUARD_GAUGE_W);
            float groundY = Math.max(f.y, GROUND_Y);
            float sy = Math.min((groundY - top) * zoom + 14f, VH - GROUND_MARKER_BOTTOM - 4f);
            float l = sx - GUARD_GAUGE_W * .5f, r = sx + GUARD_GAUGE_W * .5f, b = sy + GUARD_GAUGE_H;
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xCC070B19);
            canvas.drawRoundRect(l - 2f, sy - 2f, r + 2f, b + 2f, 5f, 5f, paint);
            boolean danger = f.guardBroken || guard <= warning;
            if (!f.guardBroken) {
                float ratio = guard / (float)CombatConfig.GUARD_MAX;
                paint.setColor(danger ? (blink ? 0xFFFF354D : 0xFFB0122A) : 0xFFFFD84A);
                canvas.drawRoundRect(l, sy, l + (r - l) * ratio, b, 3f, 3f, paint);
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f);
            paint.setColor(danger && blink ? 0xFFFF354D : 0xFFFFFFFF);
            canvas.drawRoundRect(l - 2f, sy - 2f, r + 2f, b + 2f, 5f, 5f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private static final float GUARD_GAUGE_W = 96f, GUARD_GAUGE_H = 7f;

    /** Zoom do Super em que o foco já chegou inteiro em quem ativou (o máximo do SuperFlash). */
    private static final float SUPER_FOCUS_ZOOM = 0.36f;
    /** Altura na tela (fração) onde fica o peito de quem fez o Super: o corpo inteiro cabe. */
    private static final float SUPER_FOCUS_Y = 0.52f;

    private final Path groundMarker = new Path();
    /** Distância da ponta da seta até a borda de baixo: acima das barras de super do HUD. */
    private static final float GROUND_MARKER_BOTTOM = 88f;

    private static float chestY(CombatFighter f) {
        return f.y - 0.6f * (f.y - visualTop(f));
    }

    private float opponentVisualTop() {
        return visualTop(opponent());
    }

    private void drawFrame() {
        if (!holder.getSurface().isValid()) return;
        Canvas canvas = lockFrameCanvas();
        perfil.marca(PerfilQuadros.TRAVA);
        if (canvas == null) return;

        try {
            renderFrame(canvas);
            if (pausa.fps()) drawFpsCounter(canvas);
            perfil.marca(PerfilQuadros.TELA);
        } finally {
            holder.unlockCanvasAndPost(canvas);
            perfil.marca(PerfilQuadros.ENVIO);
        }
    }

    // Contador de FPS (pausa > FPS): quadros desenhados por segundo, o nível de gráficos e a
    // altura em que o jogo está desenhando. Conta a cada 0,5 s; o texto só muda aí (sem alocar
    // por quadro).
    private static final long FPS_JANELA_NS = 500_000_000L;
    private long fpsInicio = System.nanoTime();
    private int fpsQuadros;
    private float fpsAtual = -1f;
    private String fpsTexto = "";
    private final Paint fpsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF fpsCaixa = new RectF();

    private void drawFpsCounter(Canvas canvas) {
        fpsQuadros++;
        long agora = System.nanoTime();
        if (agora - fpsInicio >= FPS_JANELA_NS) {
            fpsAtual = fpsQuadros * 1e9f / (agora - fpsInicio);
            fpsQuadros = 0;
            fpsInicio = agora;
            String nivel = QualidadeGrafica.NOMES[qualidade.nivel()];
            if (qualidade.modo() == QualidadeGrafica.Modo.AUTO) nivel = "AUTO " + nivel;
            fpsTexto = Math.round(fpsAtual) + " FPS  ·  " + nivel + "  ·  " + canvas.getHeight() + "p";
        }
        if (fpsAtual < 0f) return;
        canvas.save();
        canvas.scale(canvas.getWidth() / VW, canvas.getHeight() / VH);
        fpsPaint.setTextSize(17f);
        fpsPaint.setTypeface(Typeface.DEFAULT_BOLD);
        fpsPaint.setTextAlign(Paint.Align.CENTER);
        float w = fpsPaint.measureText(fpsTexto) + 22f;
        float cx = VW * 0.5f, top = VH - 34f;
        fpsCaixa.set(cx - w * 0.5f, top, cx + w * 0.5f, top + 26f);
        fpsPaint.setStyle(Paint.Style.FILL);
        fpsPaint.setColor(0xB0000000);
        canvas.drawRoundRect(fpsCaixa, 8f, 8f, fpsPaint);
        // Verde a 55+, amarelo a 45+, vermelho abaixo.
        fpsPaint.setColor(fpsAtual >= 55f ? 0xFF6CFF7A : fpsAtual >= 45f ? 0xFFFFD84A : 0xFFFF5A5A);
        canvas.drawText(fpsTexto, cx, top + 19f, fpsPaint);
        canvas.restore();
    }

    private void prepararRostosDoHud(CharacterDefinition... lutadores) {
        if (rostos == null) return;
        for (CharacterDefinition c : lutadores) rostos.get(c.id);
    }

    /** Tempo de cada parte do quadro (log "Quadros"). */
    final PerfilQuadros perfil = new PerfilQuadros();
    /** Nível gráfico (AUTO aprende o do aparelho; a pausa troca). */
    private final QualidadeGrafica qualidade;

    /**
     * Desenha no tamanho do nível gráfico: abaixo do ALTO o buffer da superfície fica menor que a
     * tela e o sistema (ou a janela do PC) amplia, sem custo para o jogo. Toque e layout não mudam
     * (seguem o tamanho da view). Os efeitos leves valem a partir do próximo quadro.
     */
    private void aplicarQualidade() {
        final float escala = qualidade.escala();
        post(() -> {
            int w = getWidth(), h = getHeight();
            float e = QualidadeGrafica.escalaDaTela(escala, Math.min(w, h));
            if (e >= 0.999f || w <= 0 || h <= 0) {
                holder.setSizeFromLayout();
            } else {
                holder.setFixedSize(Math.max(320, Math.round(w * e)), Math.max(180, Math.round(h * e)));
            }
        });
    }

    /** Whole frame into any canvas: the surface in production, a bitmap in visual review tests. */
    private void renderFrame(Canvas canvas) {
        if (loadingActive()) {
            telaCarregando.draw(canvas, android.os.SystemClock.uptimeMillis() - loadingSince);
            return;
        }
        {
            resetPaintForFrame();

            float sx = canvas.getWidth() / VW;
            float sy = canvas.getHeight() / VH;

            canvas.save();
            canvas.scale(sx, sy);
            if (ajusteTamanho.isOpen()) {
                ajusteTamanho.draw(canvas);
                canvas.restore();
                return;
            }
            if (ajusteCaixas.isOpen()) {
                ajusteCaixas.draw(canvas);
                canvas.restore();
                return;
            }
            float baseZoom = camera.zoom * superFlash.zoom;
            float punchZoom = feel.zoom();
            float renderZoom = baseZoom * ultraCameraZoom * punchZoom;
            float visibleWorldWidth = VW / renderZoom;
            float renderCameraX = camera.x;
            float renderCameraTop = camera.top;
            if (superFlash.active() && superFlash.zoom > 1f) {
                // Super: o zoom vai para quem ativou. O peito dele sai de onde estava na tela e
                // chega perto do meio conforme o zoom cresce (e volta junto quando ele solta).
                CombatFighter u = engine.fighter(superFlash.user);
                float ux = u.x + (u.index == PLAYER ? tagOffset() : engine.teams.tagOffset(OPPONENT));
                float uy = chestY(u);
                float plainZoom = camera.zoom;
                float plainLeft = clamp(renderCameraX - VW / plainZoom / 2f, 0f, WORLD_WIDTH - VW / plainZoom);
                float fromX = (ux - plainLeft) * plainZoom, fromY = (uy - renderCameraTop) * plainZoom;
                float k = clamp((superFlash.zoom - 1f) / SUPER_FOCUS_ZOOM, 0f, 1f);
                k = k * k * (3f - 2f * k);
                float screenX = fromX + (VW * 0.5f - fromX) * k;
                float screenY = fromY + (VH * SUPER_FOCUS_Y - fromY) * k;
                renderCameraX = ux - screenX / renderZoom + visibleWorldWidth / 2f;
                renderCameraTop = uy - screenY / renderZoom;
            }
            if (ultraShotActive) {
                // Plano do Ultra em sequência: o ponto escolhido vai para o seu lugar na tela.
                renderCameraX = ultraShot.focusX - VW * ultraShot.anchorX / renderZoom + visibleWorldWidth / 2f;
                renderCameraTop = ultraShot.focusY - VH * ultraShot.anchorY / renderZoom;
            } else if (ultraCameraZoom != 1f) {
                // Ultra activation zooms into whoever does it, keeping it on the same screen spot.
                CombatFighter p = ultraFighter();
                float baseLeft = clamp(camera.x - VW / baseZoom / 2f, 0f, WORLD_WIDTH - VW / baseZoom);
                float focusY = p.y - 80f;
                float playerScreenX = (p.x - baseLeft) * baseZoom;
                float playerScreenY = (focusY - camera.top) * baseZoom;
                renderCameraX = p.x - playerScreenX / renderZoom + visibleWorldWidth / 2f;
                renderCameraTop = focusY - playerScreenY / renderZoom;
            }
            float cameraLeft = clamp(renderCameraX - visibleWorldWidth / 2f, 0f, WORLD_WIDTH - visibleWorldWidth);
            if (punchZoom != 1f) {
                // Soco de câmera: aproxima segurando o ponto do impacto no mesmo lugar da tela.
                // No close de cinema o impacto vai para o meio da tela.
                float plainZoom = renderZoom / punchZoom;
                float plainLeft = clamp(renderCameraX - VW / plainZoom / 2f, 0f, WORLD_WIDTH - VW / plainZoom);
                float screenX = feel.cinema() ? VW * 0.5f : (feel.focusX() - plainLeft) * plainZoom;
                float screenY = feel.cinema() ? VH * 0.5f : (feel.focusY() - renderCameraTop) * plainZoom;
                cameraLeft = clamp(feel.focusX() - screenX / renderZoom, 0f, WORLD_WIDTH - visibleWorldWidth);
                renderCameraTop = feel.focusY() - screenY / renderZoom;
            }
            if (beamShake > 0f) {
                cameraLeft += (shakeRandom.nextFloat() - 0.5f) * beamShake / renderZoom;
                renderCameraTop += (shakeRandom.nextFloat() - 0.5f) * beamShake / renderZoom;
            }
            boolean beam = ultraBeamActive();

            renderCanvas.begin(canvas);
            // Close de cinema: a cena inteira inclinada, com sobra para não mostrar os cantos.
            int cinemaLayer = -1;
            float tilt = feel.tilt() + ultraTilt;
            if (tilt != 0f) {
                cinemaLayer = canvas.save();
                canvas.rotate(tilt, VW * 0.5f, VH * 0.5f);
                canvas.scale(ImpactFeel.CINEMA_OVERSCAN, ImpactFeel.CINEMA_OVERSCAN, VW * 0.5f, VH * 0.5f);
            }
            // Impact frame, último quadro: a cena com as cores invertidas.
            int negativeLayer = feel.negative() ? canvas.saveLayer(0f, 0f, VW, VH, negativePaint) : -1;
            if (stageScene != null) {
                // The stage projects every layer with the same camera the fighters use.
                perfil.marca(PerfilQuadros.TELA);
                stageScene.setDetalhe(!qualidade.efeitosLeves());
                stageScene.setCamera(renderZoom, cameraLeft, renderCameraTop);
                stageScene.drawBackground(renderCanvas);
            }

            canvas.save();
            canvas.scale(renderZoom, renderZoom);
            canvas.translate(-cameraLeft, -renderCameraTop);
            if (stageScene == null) {
                drawScenario(canvas);
            }
            drawFloorReflections(canvas);
            perfil.marca(PerfilQuadros.FUNDO);
            // Golpe forte: o cenário escurece por um instante e os lutadores ficam na frente.
            effects.drawWorldOverlay(canvas, paint, feel.darkAlpha(), 0, 0, 8);
            if (finalizacao.ativo()) drawFinalizacao(canvas, renderZoom, cameraLeft, renderCameraTop);
            if (beam) {
                drawUltraBeamOverlay(canvas, false);
                raioFinal.drawBehind(renderCanvas, ultraPack, ultraBeamFrame());
            }
            if (isSuperCinematicActive()) drawSuperDarkness(canvas);
            drawDamageDummy(canvas);
            float effectTime = engine.frame() * FIXED_STEP;
            effects.drawPuddles(renderCanvas, engine.puddles, effectTime);
            effects.drawEnergyProjectiles(canvas, renderCanvas, engine.energyProjectiles);
            for (int i = 0; i < 2; i++) effects.drawSpecialEffect(renderCanvas, engine.fighter(i), effectTime);
            effects.drawSuperProjectiles(canvas, renderCanvas, engine.superProjectiles);
            if (ultraFighter().inUltraSequence()) {
                effects.drawWorldOverlay(canvas, paint, ultraDarkAlpha, 0, 0, 8);
            }
            if (ultraWindupActive()) {
                effects.drawWorldOverlay(canvas, paint, ultraDarkAlpha, 0, 0, 8);
                canvas.save();
                CombatFighter u = ultraFighter();
                if (u.facing < 0) {
                    canvas.scale(-1f, 1f, u.x, 0f);
                }
                if (u.ultraPhase == CombatFighter.ULTRA_STARTUP) {
                    effects.drawSuperCharge(canvas, paint, u.x, u.y, ultraAuraTime, u.state.profile.color);
                } else {
                    effects.drawUltraRush(canvas, paint, u.x, u.y, u.state.profile.color);
                }
                canvas.restore();
            }

            drawOverdriveAura(canvas, opponent(), engine.teams.tagOffset(OPPONENT));
            drawOverdriveAura(canvas, player(), tagOffset());
            drawMechanicAura(canvas, opponent(), engine.teams.tagOffset(OPPONENT));
            drawMechanicAura(canvas, player(), tagOffset());
            drawEchoes(canvas);
            drawHenchmen(canvas);
            drawPartner(canvas);
            drawAirDashTrail(canvas, player());
            drawAirDashTrail(canvas, opponent());
            canvas.save();
            canvas.translate(tagOffset(), 0f);
            drawPlayer(canvas);
            canvas.restore();
            impacts.draw(canvas);
            effects.drawWorldOverlay(canvas, paint, feel.flashAlpha(), 255, 255, 255);
            if (beam) {
                raioFinal.drawFront(renderCanvas, ultraPack, ultraBeamFrame());
                drawUltraBeamOverlay(canvas, true);
            }
            if (isSuperCinematicActive()) {
                effects.drawWorldOverlay(canvas, paint, superFlash.flashAlpha, 255, 255, 255);
            }
            if (debugOverlay) debug.drawWorld(canvas, engine);
            canvas.restore();
            perfil.marca(PerfilQuadros.LUTA);
            // Na finalização a frente do cenário some junto com o resto dele.
            if (stageScene != null && finalizacao.forca() <= 0f) stageScene.drawForeground(renderCanvas);
            perfil.marca(PerfilQuadros.FRENTE);
            if (negativeLayer >= 0) canvas.restoreToCount(negativeLayer);
            if (feel.impactStyle() != ImpactFeel.NONE) drawImpactFrame(canvas, renderZoom, cameraLeft, renderCameraTop);
            if (cinemaLayer >= 0) canvas.restoreToCount(cinemaLayer);
            if (ultraBars < 0.001f && !isSuperCinematicActive()) {
                drawGroundMarkers(canvas, renderZoom, cameraLeft, renderCameraTop);
                drawGuardGauges(canvas, renderZoom, cameraLeft, renderCameraTop);
            }
            if (ultraBars > 0.001f) {
                // Faixas pretas de cinema durante o Ultra.
                float bar = AberturaUltra.BAR * ultraBars;
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xFF0B0E16);
                canvas.drawRect(-40f, -40f, VW + 40f, bar, paint);
                canvas.drawRect(-40f, VH - bar, VW + 40f, VH + 40f, paint);
            }

            boolean pageCoversGame = paginaFinal.isActive() && !paginaFinal.isShattering();
            if (!pageCoversGame) {
                // No Ultra o HUD some enquanto as faixas entram, e o controle de toque sai.
                float faixas = ultraBars;
                if (faixas < 0.999f) {
                    int layer = faixas > 0.001f
                        ? canvas.saveLayerAlpha(0f, 0f, VW, VH, Math.round(255f * (1f - faixas))) : -1;
                    drawHud(canvas);
                    if (layer >= 0) canvas.restoreToCount(layer);
                }
                if (touchControls && faixas < 0.001f) drawControls(canvas);
            }
            if (abertura.isActive()) abertura.render(renderCanvas);
            if (paginaFinal.isActive()) {
                paginaFinal.render(renderCanvas);
            }
            // A pausa por cima da luta parada.
            pausa.render(renderCanvas);

            canvas.restore();
            perfil.marca(PerfilQuadros.HUD);
        }
    }

    private Canvas lockFrameCanvas() {
        if (hardwareCanvas && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                return holder.lockHardwareCanvas();
            } catch (RuntimeException ex) {
                Log.w(LOG_TAG, "Canvas pela GPU indisponível, usando software", ex);
                hardwareCanvas = false;
            }
        }
        return holder.lockCanvas();
    }

    /** Wet floor: each fighter mirrored around the ground line, faded. */
    private void drawFloorReflections(Canvas c) {
        float strength = stageScene != null ? stageScene.fighterReflection() : 0f;
        if (strength <= 0f) return;
        int alpha = Math.round(255f * strength);
        CombatFighter npc = opponent();
        drawReflection(c, opponentSpriteRenderer, npc, engine.teams.tagOffset(OPPONENT), alpha);
        CombatFighter partner = partnerBody(engine.team(PLAYER));
        if (partner != null) drawReflection(c, partnerSpriteRenderer, partner, 0f, alpha);
        CombatFighter cpuPartner = partnerBody(engine.team(OPPONENT));
        if (cpuPartner != null) drawReflection(c, opponentPartnerRenderer, cpuPartner, 0f, alpha);
        drawReflection(c, spriteFighterRenderer, player(), tagOffset(), alpha);
    }

    private void drawReflection(Canvas c, SpriteFighterRenderer renderer, CombatFighter f, float offsetX, int alpha) {
        c.save();
        c.translate(offsetX, 0f);
        c.scale(1f, -1f, 0f, GROUND_Y);
        // Transparência direto no desenho do sprite (sem saveLayer: uma camada fora da tela por
        // lutador por quadro pesa na GPU de celular fraco). Só muda onde dois quadros se cruzam
        // (a troca suave do P01), e num reflexo apagado isso não aparece.
        renderer.setAlphaGlobal(alpha);
        drawFighter(c, renderer, f, false, false);
        renderer.setAlphaGlobal(255);
        c.restore();
    }

    private void resetPaintForFrame() {
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(1f);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextScaleX(1f);
        paint.setTextSkewX(0f);
    }

    private void drawScenario(Canvas c) {
        stage.draw(c, paint);
    }

    private void drawHud(Canvas c) {
        hud.drawHud(c, hudState, rostos);
        if (throwBannerFrames > 0) {
            float t = 1f - throwBannerFrames / (float)THROW_BANNER_FRAMES;
            hud.drawBanner(c, throwBanner, Math.round(255f * clamp((1f - t) * 3f, 0f, 1f)), 1f + 0.4f * Math.max(0f, 1f - t * 6f));
        }
        String call = flow.banner();
        if (call != null) {
            float t = flow.bannerProgress();
            hud.drawBanner(c, call, Math.round(255f * clamp((1f - t) * 5f, 0f, 1f)), 1f + 0.4f * Math.max(0f, 1f - t * 6f));
        }
        hud.drawCombo(c, engine.session(OPPONENT), engine.lastSession(OPPONENT),
            engine.framesSinceSessionEnd(OPPONENT), true);
        hud.drawCombo(c, engine.session(PLAYER), engine.lastSession(PLAYER),
            engine.framesSinceSessionEnd(PLAYER), false);
        if (debugOverlay) {
            debug.drawPanel(c, engine, perfil.ultima());
            drawSizeTunerButton(c);
        }
    }

    private void drawSizeTunerButton(Canvas c) {
        Paint p=tuningPaint;
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(225,20,60,90));
        c.drawRoundRect(TUNE_LEFT,TUNE_TOP,TUNE_RIGHT,TUNE_BOTTOM,10f,10f,p);
        p.setColor(Color.rgb(245,225,170));
        p.setFakeBoldText(true);
        p.setTextSize(16f);
        p.setTextAlign(Paint.Align.CENTER);
        float cx=(TUNE_LEFT+TUNE_RIGHT)/2f;
        c.drawText("AJUSTAR",cx,TUNE_TOP+30f,p);
        c.drawText("TAMANHOS",cx,TUNE_TOP+50f,p);
        p.setFakeBoldText(false);
        p.setTextSize(11f);
        p.setColor(Color.LTGRAY);
        c.drawText("todas as animações",cx,TUNE_TOP+72f,p);
        // Logo abaixo: caixas de combate (hitbox, hurtbox, empurrão).
        p.setColor(Color.argb(225,90,30,40));
        c.drawRoundRect(TUNE_LEFT,BOXES_TOP,TUNE_RIGHT,BOXES_BOTTOM,10f,10f,p);
        p.setColor(Color.rgb(245,225,170));
        p.setFakeBoldText(true);
        p.setTextSize(16f);
        c.drawText("AJUSTAR",cx,BOXES_TOP+30f,p);
        c.drawText("CAIXAS",cx,BOXES_TOP+50f,p);
        p.setFakeBoldText(false);
        p.setTextSize(11f);
        p.setColor(Color.LTGRAY);
        c.drawText("hitbox e hurtbox",cx,BOXES_TOP+72f,p);
        p.setTextAlign(Paint.Align.LEFT);
    }

    /** DEBUG: os botões abrem as telas de ajuste de tamanho e de caixas (a luta fica parada atrás). */
    private boolean handleSizeTunerTap(float x,float y) {
        if (x<TUNE_LEFT || x>TUNE_RIGHT || y<TUNE_TOP || y>BOXES_BOTTOM) return false;
        boolean boxes = y >= BOXES_TOP;
        if (!boxes && y > TUNE_BOTTOM) return true;
        if (paginaFinal.isActive() || abertura.isActive() || ultraAttacker >= 0) return true;
        selectionKeys.clear();
        pad.reset();
        releaseAllPointers();
        if (boxes) ajusteCaixas.open(team[0].character.id);
        else ajusteTamanho.open(team[0].character.id);
        return true;
    }

    /** SALVAR NO DRIVE das caixas: arquivo com os ajustes de todos os personagens. */
    private void exportBoxTuning() {
        String date = new java.text.SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new java.util.Date());
        String json = AjusteCaixas.exportJson(BuildConfig.VERSION_NAME, date);
        try {
            ArquivosCompartilhados.compartilhar(getContext(), "ajustes-caixas-" + date + ".json", "application/json", json);
            ajusteCaixas.say("Escolha o Drive para salvar o arquivo");
        } catch (IOException | RuntimeException e) {
            Log.e("GameLuta", "ajustes de caixas", e);
            ajusteCaixas.say("Não deu para salvar: " + e.getMessage());
        }
    }

    /** SALVAR NO DRIVE: arquivo com os ajustes de todos os personagens no menu Compartilhar. */
    private void exportSizeTuning() {
        String date = new java.text.SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new java.util.Date());
        String json = AjusteTamanho.exportJson(BuildConfig.VERSION_NAME, date);
        try {
            ArquivosCompartilhados.compartilhar(getContext(), "ajustes-tamanho-" + date + ".json", "application/json", json);
            ajusteTamanho.say("Escolha o Drive para salvar o arquivo");
        } catch (IOException | RuntimeException e) {
            Log.e("GameLuta", "ajustes de tamanho", e);
            ajusteTamanho.say("Não deu para salvar: " + e.getMessage());
        }
    }

    private static String guardLabel(int guard, String prefix) {
        if (guard == CombatFighter.GUARD_AIR) return prefix + " NO AR";
        return guard == CombatFighter.GUARD_LOW ? prefix + " BAIXO" : prefix + " ALTO";
    }

    private String currentStateLabel() {
        CombatFighter p = player();
        switch (p.status) {
            case KNOCKDOWN:
                return p.hardKnockdown ? "DERRUBADO" : "DERRUBADO  •  \u2191 RÁPIDO  \u2190\u2192 ROLA  \u2193 DEMORA";
            case WAKEUP:
                return p.rolling() ? "ROLAMENTO" : "LEVANTANDO";
            case BLOCKSTUN:
                return guardLabel(p.lastGuard, "BLOQUEIO") +
                    (p.state.superMeter >= engine.config.pushblockCost ? "  •  M+H EMPURRA" : "");
            case AIR_HITSTUN:
                if (p.stunLeft == 0 && !p.slammed && !p.hardFall && !p.ultraFall) return "CAINDO: L/M/H = AIR TECH";
                return p.slammed ? "QUEDA FORÇADA" : p.launched ? "LANÇADO" : "HIT";
            case HITSTUN:
                return "HIT";
            case THROW:
                return p.throwPhase == CombatFighter.THROW_TECH ? "TECH"
                    : p.throwPhase == CombatFighter.THROW_WHIFF ? "AGARRÃO ERROU" : "AGARRÃO";
            case THROWN:
                return "AGARRADO: L+M!";
            default:
                break;
        }
        if (p.anticipatedGuard != CombatFighter.GUARD_NONE) return guardLabel(p.anticipatedGuard, "DEFENDENDO");
        int guard = engine.guardFromInput(p);
        if (guard != CombatFighter.GUARD_NONE) return guardLabel(guard, "PRONTO");
        if (p.attacking()) {
            if (p.attack.kind == AttackDefinition.Kind.SUPER) return "SUPER";
            return "ATAQUE " + (p.move != null && p.move.binding.startsWith("j") ? p.move.binding.substring(1) : p.attack.id);
        }
        if (p.status == CombatFighter.Status.ULTRA) {
            return p.ultraPhase == CombatFighter.ULTRA_RECOVERY ? "ULTRA ERROU" : "ULTRA";
        }
        if (p.backdashFrames > 0) return "BACKDASH";
        if (p.forwardDashing) return "DASH";
        if (p.superJumping) return "SUPER JUMP";
        if (p.crouching) return "AGACHADO";
        if (p.airDashFrames > 0) return p.airDashBack ? "AIR BACKDASH" : "AIR DASH";
        if (!p.grounded) return "NO AR";
        if (Math.abs(p.travel) > 0.001f) return "ANDANDO";
        return "PARADO";
    }

    private void drawDamageDummy(Canvas c) {
        CombatFighter npc = opponent();
        // No 2x2 o parceiro do CPU entra correndo depois do KO, como na troca.
        c.save();
        c.translate(engine.teams.tagOffset(OPPONENT), 0f);
        paint.setColor(Color.argb(70, 0, 0, 0));
        c.drawOval(npc.x - 43f, GROUND_Y - 10f, npc.x + 43f, GROUND_Y + 10f, paint);

        drawFighter(c, opponentSpriteRenderer, npc, npc.framesSinceHit < 4 && !npc.ko(), false);
        c.restore();

        opponentPanel.x = npc.x;
        opponentPanel.visualTop = opponentVisualTop();
        FighterState cpu = opponentPoint();
        opponentPanel.lifeRatio = cpu.life / (float)cpu.profile.maxLife;
        opponentPanel.status = opponentStatusLabel();
        opponentPanel.lifeLabel = cpu.life + " / " + cpu.profile.maxLife;
        opponentPanel.superLabel = cpu.superHudLabel;
        opponentPanel.damageLabel = dummyDamageLabel;
        opponentPanel.damageProgress = dummyDamageLabelFrames / (float)DAMAGE_LABEL_FRAMES;
        if (debugOverlay) hud.drawOpponentPanel(c, opponentPanel);
    }

    private String opponentStatusLabel() {
        CombatFighter npc = opponent();
        String name = opponentCharacter().displayName.toUpperCase(Locale.ROOT);
        String state;
        if (npc.status == CombatFighter.Status.KNOCKDOWN) {
            state = npc.knockdownFrame < engine.config.knockdownFallFrames ? "CAINDO" : "NO CHÃO";
        } else if (npc.status == CombatFighter.Status.WAKEUP) state = "LEVANTANDO";
        else if (npc.attacking()) state = npc.attack.id;
        else if (npc.status == CombatFighter.Status.THROWN) state = "AGARRADO";
        else if (npc.status == CombatFighter.Status.THROW) {
            state = npc.throwPhase == CombatFighter.THROW_TECH ? "TECH" : "AGARRÃO";
        }
        else if (npc.slammed) state = "QUEDA FORÇADA";
        else if (npc.inHitstun() || npc.status == CombatFighter.Status.BLOCKSTUN) state = "SEM CONTROLE";
        else if (!npc.grounded) state = "NO AR";
        else state = ai.enabled() ? "IA" : "PARADO";
        return name + " • " + state;
    }

    private float reactionElapsed;

    /**
     * Reaction clip of a fighter: knockdown, wake-up, launch and hitstun. Returns null when
     * the pack has no art for it (the caller keeps a pose).
     */
    private String reactionState(SpriteFighterRenderer renderer, CombatFighter f) {
        String state = null;
        CombatConfig config = engine.config;
        switch (f.status) {
            case KNOCKDOWN:
                if (f.knockdownFrame < config.knockdownFallFrames) {
                    state = SpriteStates.KNOCKDOWN;
                    reactionElapsed = f.knockdownFrame * FIXED_STEP;
                } else {
                    state = SpriteStates.GROUNDED;
                    reactionElapsed = (f.knockdownFrame - config.knockdownFallFrames) * FIXED_STEP;
                }
                break;
            case WAKEUP:
                state = SpriteStates.GETUP;
                // The whole clip spread over the engine's wake-up (a long one plays faster).
                CharacterDefinition.Animation getup = f.character().animations.get(state);
                reactionElapsed = getup == null ? f.knockdownFrame * FIXED_STEP
                    : Math.min(getup.duration - 0.0001f, getup.timeFor(f.knockdownFrame, engine.wakeupFrames(f)));
                break;
            case AIR_HITSTUN:
                state = SpriteStates.HIT_AIR;
                reactionElapsed = f.launched || f.slammed
                    ? launchPoseTime(f.vy, f.slammed)
                    : f.stunElapsed * FIXED_STEP;
                break;
            case HITSTUN:
                state = f.hitCrouching ? SpriteStates.HIT_CROUCH : SpriteStates.HIT_STAND;
                // Stretched over the hitstun, like the guard reaction: the clip ends back in
                // guard as control returns, whether the stun is short or long.
                reactionElapsed = hitReactionTime(f.character().animations.get(state), f.stunElapsed, f.stunTotal);
                break;
            case THROWN:
                // Held by the collar: the first frame of the hit reaction.
                state = SpriteStates.HIT_STAND;
                reactionElapsed = 0.02f;
                break;
            default:
                break;
        }
        return state != null && renderer.hasAnimation(state) ? state : null;
    }

    /** Clip time of a ground hit reaction: the whole clip spread over the hitstun. */
    static float hitReactionTime(CharacterDefinition.Animation clip, int stunElapsed, int stunTotal) {
        if (clip == null) return stunElapsed * FIXED_STEP;
        return Math.min(clip.duration - 0.0001f, clip.timeFor(stunElapsed + 0.5f, Math.max(1, stunTotal)));
    }

    /**
     * Launch poses follow the physics instead of a clock: thrown up, apex, forced dive
     * (ground slam), then the recovery tuck on the way down. The times index a HIT_AIR
     * clip of up to four 55 ms frames; shorter clips clamp.
     */
    private static float launchPoseTime(float verticalVelocity, boolean slam) {
        if (slam) return 0.13f;
        if (verticalVelocity < -500f) return 0f;
        if (verticalVelocity < 0f) return 0.07f;
        return 0.19f;
    }

    /** Vector fallback for packs without KNOCKDOWN/GROUNDED/GETUP: fall backwards. */
    private float knockdownAngle(SpriteFighterRenderer renderer, CombatFighter f) {
        if (renderer.hasAnimation(SpriteStates.KNOCKDOWN)) return 0f;
        CombatConfig config = engine.config;
        float lean;
        if (f.status == CombatFighter.Status.KNOCKDOWN) {
            lean = clamp(f.knockdownFrame / (float)config.knockdownFallFrames, 0f, 1f);
        } else if (f.status == CombatFighter.Status.WAKEUP) {
            lean = 1f - clamp(f.knockdownFrame / (float)engine.wakeupFrames(f), 0f, 1f);
        } else {
            return 0f;
        }
        return -88f * lean * f.facing;
    }

    /** Picks the clip of one fighter from its engine state. Same rules for both sides. */
    private void updateFighterSprite(SpriteFighterRenderer renderer, CombatFighter f) {
        CharacterDefinition character = f.character();
        String animation = reactionState(renderer, f);
        float animationElapsed = reactionElapsed;
        CharacterDefinition.Animation introClip = introFrame >= 0 ? character.animations.get(SpriteStates.INTRO) : null;
        if (introClip != null) {
            // A shorter intro holds its last frame (the guard) until the longest one ends.
            animation = SpriteStates.INTRO;
            animationElapsed = Math.min(introClip.duration - 0.0001f, introFrame * FIXED_STEP);
        }

        int guard = f.status == CombatFighter.Status.BLOCKSTUN ? f.lastGuard : f.anticipatedGuard;
        if (animation == null && guard != CombatFighter.GUARD_NONE) {
            String defense = guard == CombatFighter.GUARD_LOW
                ? SpriteStates.DEFENSE_CROUCH
                : guard == CombatFighter.GUARD_AIR
                    ? SpriteStates.DEFENSE_AIR
                    : SpriteStates.DEFENSE_STAND;
            if (renderer.hasAnimation(defense)) {
                animation = defense;
                // Held guard while a threat approaches; impact and recovery during blockstun.
                boolean blocking = f.status == CombatFighter.Status.BLOCKSTUN;
                animationElapsed = character.animation(defense).guardTime(
                    blocking, blocking ? f.stunElapsed / (float)Math.max(1, f.stunTotal) : 0f);
            }
        }
        if (animation == null && f.attacking()) {
            float elapsed = (f.attackFrame + 0.5f) * FIXED_STEP;
            float total = f.attack.totalFrames * FIXED_STEP;
            if (f.move != null) {
                if (f.move.animation != null) {
                    animation = f.move.animation.id;
                    // Conectou: segura a pose do impacto (no congelamento) e volta seco.
                    animationElapsed = ImpactFeel.snapBackTime(f.move, f.attackFrame,
                        f.outcome != CombatFighter.Outcome.NONE, FIXED_STEP);
                }
                // Otherwise the move declared a pose: crouch flag or airborne physics below.
            } else {
                CharacterDefinition.Animation special = character.specialAnimations.get(
                    f.attack.kind == AttackDefinition.Kind.SUPER ? "SUPER" : "S");
                if (f.attack.kind == AttackDefinition.Kind.PROJECTILE) {
                    // Onda carregável: o loop de carga enquanto segura, e o disparo carregado depois.
                    CharacterDefinition.Animation charge = character.animations.get(SpriteStates.SPECIAL_CHARGE);
                    CharacterDefinition.Animation charged = character.animations.get(SpriteStates.SPECIAL_ENERGY_CHARGED);
                    if (f.charging && charge != null) {
                        animation = charge.id;
                        animationElapsed = f.charge * FIXED_STEP;
                    } else if (f.charged() && charged != null) {
                        special = charged;
                    }
                }
                if (animation == null && special != null) {
                    animation = special.id;
                    animationElapsed = special.timeFor(elapsed, total);
                }
            }
        }

        if (animation == null && f.wallJumpFrames > 0 && f.status == CombatFighter.Status.NEUTRAL &&
            renderer.hasAnimation(SpriteStates.WALL_JUMP)) {
            animation = SpriteStates.WALL_JUMP;
            animationElapsed = (engine.config.wallJumpFrames - f.wallJumpFrames) * FIXED_STEP;
        }
        if (animation == null && f.airDashFrames > 0 && f.status == CombatFighter.Status.NEUTRAL) {
            // Air dash: the ground dash clip (or backdash), played on the burst's clock.
            String clip = f.airDashBack ? SpriteStates.BACKDASH : SpriteStates.DASH;
            if (renderer.hasAnimation(clip)) {
                int total = f.airDashBack ? engine.config.backAirDashFrames : engine.config.airDashFrames;
                animation = clip;
                animationElapsed = (total - f.airDashFrames) * FIXED_STEP;
            }
        }
        if (animation == null && f.status == CombatFighter.Status.THROW) {
            animation = throwPose(f, character);
            animationElapsed = throwPoseTime;
        }
        CombatFighter rival = engine.fighter(1 - f.index);
        int side = f.index;
        // Só o titular comemora; o parceiro em campo (assist, saindo) não mexe no relógio dele.
        boolean onPoint = f == engine.fighter(side);
        // No 2x2 só se comemora quando a dupla inteira do outro lado caiu.
        boolean matchWon = !flow.battle() || flow.over();
        // No TIME OVER o rival fica de pé: comemora quem tinha mais vida.
        boolean beatRival = rival.ko() || (flow.timeOver() && flow.winner() == side);
        boolean celebrating = animation == null && onPoint && matchWon && beatRival && !f.ko() && f.grounded &&
            f.status == CombatFighter.Status.NEUTRAL && !f.attacking() && renderer.hasAnimation(SpriteStates.VICTORY);
        if (celebrating) {
            // Opponent KO: the winner plays VICTORY once and holds its last pose.
            CharacterDefinition.Animation win = character.animations.get(SpriteStates.VICTORY);
            animation = SpriteStates.VICTORY;
            // P01 source frames 0-6 include a walk toward the camera.
            // Celebrate from the planted stance, frame 7, without translation.
            boolean p01Victory = "player_base".equals(character.id) ||
                "p01_training".equals(character.id);
            float start = p01Victory ? win.timeOfFrame(7) : 0f;
            animationElapsed = Math.min(win.duration - 0.0001f,
                start + victoryFrames[side]++ * FIXED_STEP);
        } else if (onPoint) {
            victoryFrames[side] = 0;
        }
        // KO de pé (o golpe final não derrubou): toca a DEFEAT uma vez e fica na última pose.
        // Quem foi nocauteado no chão ou no ar continua deitado (o motor não o levanta).
        boolean defeated = animation == null && onPoint && f.ko() && f.grounded &&
            f.status == CombatFighter.Status.NEUTRAL && renderer.hasAnimation(SpriteStates.DEFEAT);
        if (defeated) {
            CharacterDefinition.Animation lose = character.animations.get(SpriteStates.DEFEAT);
            animation = SpriteStates.DEFEAT;
            animationElapsed = Math.min(lose.duration - 0.0001f, defeatFrames[side]++ * FIXED_STEP * DEFEAT_SPEED);
        } else if (onPoint) {
            defeatFrames[side] = 0;
        }
        CharacterDefinition.Animation beamPose =
            animation == null && f.firingBeam() ? character.specialAnimations.get("ULTRA") : null;
        if (beamPose != null) {
            // Own beam sheet (9 poses): charge, shot, wind-blown hold, recovery.
            CombatConfig config = engine.config;
            int pose = RaioFinal.poseFrame(f.ultraFrame, config.ultraBeamFireFrame(),
                config.ultraBeamBlastFrame(f.beamHits), config.ultraBeamFadeFrames);
            animation = beamPose.id;
            animationElapsed = beamPose.timeOfFrame(pose);
        }
        CharacterDefinition.UltraSequence sequence = f.state.profile.ultraSequence;
        CharacterDefinition.Animation sequenceArt =
            sequence != null ? character.animations.get(SpriteStates.ULTRA_SEQ) : null;
        if (animation == null && f.status == CombatFighter.Status.ULTRA && sequenceArt != null &&
            f.ultraPhase != CombatFighter.ULTRA_RECOVERY) {
            // Ultra do vídeo: a ativação toca até a pose da investida, a investida segura essa
            // pose e a sequência anda no relógio do motor (os golpes caem nos quadros dela).
            CombatConfig config = engine.config;
            float frames = f.ultraPhase == CombatFighter.ULTRA_STARTUP
                ? sequence.startupFrame * clamp(f.ultraFrame / (float)config.ultraStartupFrames, 0f, 1f)
                : f.ultraPhase == CombatFighter.ULTRA_SEQUENCE ? sequence.startupFrame + f.ultraFrame
                : sequence.startupFrame;
            animation = sequenceArt.id;
            animationElapsed = Math.min(sequenceArt.duration - 0.0001f, frames * FIXED_STEP);
        }
        if (animation == null && f.status == CombatFighter.Status.ULTRA) {
            // Activation uses the Super charge clip; rush and recovery reuse the heavy strike.
            CharacterDefinition.Animation charge = character.specialAnimations.get("SUPER");
            CharacterDefinition.Move strike = character.moves.get("H");
            CombatConfig config = engine.config;
            if (f.ultraPhase == CombatFighter.ULTRA_STARTUP && charge != null) {
                animation = charge.id;
                animationElapsed = charge.timeFor(f.ultraFrame * FIXED_STEP, config.ultraStartupFrames * FIXED_STEP);
            } else if (f.ultraPhase != CombatFighter.ULTRA_STARTUP && strike != null && strike.animation != null) {
                AttackDefinition hit = strike.attack;
                float frames = f.ultraPhase == CombatFighter.ULTRA_RECOVERY
                    ? hit.startupFrames + hit.activeFrames +
                        hit.recoveryFrames * clamp(f.ultraFrame / (float)config.ultraRecoveryFrames, 0f, 1f)
                    : hit.startupFrames + 0.5f;
                animation = strike.animation.id;
                animationElapsed = strike.animationTime(Math.min(frames, hit.totalFrames - 0.5f) * FIXED_STEP);
                if (f.firingBeam()) {
                    // Without own art: arm pulled back while charging, fully extended while
                    // firing, then back to guard as the beam fades.
                    int afterBlast = f.ultraFrame - config.ultraBeamBlastFrame(f.beamHits);
                    float back = clamp(afterBlast / (float)config.ultraBeamFadeFrames, 0f, 1f);
                    float pose = f.ultraFrame < config.ultraBeamFireFrame()
                        ? BEAM_CHARGE_POSE : BEAM_POSE + (1f - BEAM_POSE) * back;
                    animationElapsed = strike.animationTime(strike.totalTime * pose);
                }
            }
        }

        boolean combatPose = animation == null && (
            f.status == CombatFighter.Status.ULTRA ||
            f.status == CombatFighter.Status.THROW ||
            f.attacking() ||
            guard != CombatFighter.GUARD_NONE ||
            (onPoint && engine.team(side).tagging())
        );
        boolean locked = f.status != CombatFighter.Status.NEUTRAL && !f.attacking() &&
            f.status != CombatFighter.Status.ULTRA;

        renderer.update(
            FIXED_STEP,
            f.grounded,
            f.crouchingBody() || guard == CombatFighter.GUARD_LOW,
            f.vy,
            f.travel,
            f.travel * f.facing > 0,
            f.forwardDashing,
            f.backdashFrames > 0,
            animation,
            animationElapsed,
            combatPose,
            locked
        );
    }

    /**
     * Throw poses: the character's THROW_GRAB/THROW_TOSS clips when it has them, otherwise
     * its own moves (the jab reaching out to grab and holding, the heavy straight for the
     * toss); the guard when teched.
     * Returns the animation id (its clip time in {@link #throwPoseTime}), or null for the
     * combat pose.
     */
    private String throwPose(CombatFighter f, CharacterDefinition character) {
        CombatConfig config = engine.config;
        if (f.throwPhase == CombatFighter.THROW_TECH) {
            if (!character.animations.containsKey(SpriteStates.DEFENSE_STAND)) return null;
            float progress = clamp(f.throwFrame / (float)config.throwTechFrames, 0f, 1f);
            throwPoseTime = character.animation(SpriteStates.DEFENSE_STAND).guardTime(true, progress);
            return SpriteStates.DEFENSE_STAND;
        }
        boolean toss = f.throwPhase == CombatFighter.THROW_EXECUTE;
        CharacterDefinition.Animation grab = character.animations.get(SpriteStates.THROW_GRAB);
        CharacterDefinition.Animation thrown = character.animations.get(SpriteStates.THROW_TOSS);
        if (grab != null && thrown != null) {
            // Dedicated art: reach and hold on the grab clip, the toss on its own clip; a
            // whiff reaches out and comes back.
            if (f.throwPhase == CombatFighter.THROW_STARTUP) {
                float k = clamp(f.throwFrame / (float)config.throwStartupFrames, 0f, 1f);
                throwPoseTime = grab.duration * k * 0.999f;
                return SpriteStates.THROW_GRAB;
            }
            if (f.throwPhase == CombatFighter.THROW_HOLD) {
                throwPoseTime = grab.duration * 0.999f;
                return SpriteStates.THROW_GRAB;
            }
            if (toss) {
                float k = clamp(f.throwFrame / (float)config.throwExecuteFrames, 0f, 1f);
                throwPoseTime = thrown.duration * k * 0.999f;
                return SpriteStates.THROW_TOSS;
            }
            float k = clamp(f.throwFrame / (float)config.throwWhiffFrames, 0f, 1f);
            throwPoseTime = grab.duration * (k < 0.5f ? 2f * k : 2f - 2f * k) * 0.999f;
            return SpriteStates.THROW_GRAB;
        }
        CharacterDefinition.Move move = character.moves.get(toss ? "H" : "L");
        if (move == null || move.animation == null) return null;
        AttackDefinition a = move.attack;
        float frames;
        if (f.throwPhase == CombatFighter.THROW_STARTUP) {
            frames = a.startupFrames * clamp(f.throwFrame / (float)config.throwStartupFrames, 0f, 1f);
        } else if (f.throwPhase == CombatFighter.THROW_HOLD) {
            frames = a.startupFrames + 0.5f;
        } else {
            int length = toss ? config.throwExecuteFrames : config.throwWhiffFrames;
            float k = clamp(f.throwFrame / (float)length, 0f, 1f);
            frames = a.startupFrames + (a.totalFrames - 0.5f - a.startupFrames) * k;
        }
        throwPoseTime = move.animationTime(frames * FIXED_STEP);
        return move.animation.id;
    }

    private void drawFighter(Canvas c, SpriteFighterRenderer renderer, CombatFighter f,
                             boolean damageFlash, boolean guardFlash) {
        float angle = knockdownAngle(renderer, f);
        c.save();
        // Quem apanhou treme durante o congelamento do acerto.
        float shake = ImpactFeel.hitShake(f, engine.frame());
        if (shake != 0f) c.translate(shake, 0f);
        if (angle != 0f) c.rotate(angle, f.x, GROUND_Y);
        if (f.spinning && !f.grounded) spinTransform(c, f);
        // Shared backward-hop arc, without changing ground hitboxes or jump inputs.
        float visualY = f.y;
        if (f.grounded && f.status == CombatFighter.Status.NEUTRAL) {
            visualY -= engine.config.backdashVisualLift(f.backdashFrames);
        }
        ColorFilter look = renderer.override() == null ? superLook(f) : null;
        if (look != null) renderer.setOverride(look);
        renderer.draw(c, f.x, visualY, f.facing, damageFlash, guardFlash);
        if (look != null) renderer.setOverride(null);
        c.restore();
    }

    /** Graus por frame do giro do punish counter (~3 voltas no voo). */
    private static final float SPIN_DEGREES_PER_FRAME = 20f;
    /** Inclinação do corpo no voo: a cabeça vai na frente, para o lado em que ele voa. */
    private static final float SPIN_TILT = 38f;

    /**
     * Punish counter: o corpo gira em torno do próprio eixo (como um parafuso) enquanto voa,
     * inclinado para trás. O giro é a largura do desenho indo e voltando (cos), que vira o
     * sprite do avesso a cada meia volta, com o centro do corpo como pivô.
     */
    private void spinTransform(Canvas c, CombatFighter f) {
        float pivotY = f.y - f.character().visualStandHeight * 0.5f;
        float lean = clamp(f.stunElapsed / 8f, 0f, 1f);
        c.rotate(Math.signum(f.vx) * SPIN_TILT * lean, f.x, pivotY);
        float turn = (float)Math.cos(Math.toRadians(f.stunElapsed * SPIN_DEGREES_PER_FRAME));
        // Nunca some de vez de perfil: fica uma lâmina fina.
        float width = Math.copySign(Math.max(0.12f, Math.abs(turn)), turn);
        c.scale(width, 1f, f.x, pivotY);
    }

    // Impact frame: cores dos dois estilos e o que se reaproveita entre quadros.
    private static final int IMPACT_RED = 0xFFCD1220, IMPACT_RED_LINES = 0xFF78000C;
    private static final int IMPACT_VOID = 0xFF060608, IMPACT_VOID_LINES = 0xFFEBEBF0;
    private final ColorFilter inkSilhouette = new PorterDuffColorFilter(0xFF08080A, PorterDuff.Mode.SRC_IN);
    private final ColorFilter whiteSilhouette = new PorterDuffColorFilter(0xFFFAFAFC, PorterDuff.Mode.SRC_IN);
    private final Paint negativePaint = new Paint();
    {
        negativePaint.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(new float[] {
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f})));
    }
    private final Path impactPath = new Path();
    /** Contorno branco das silhuetas pretas (RED), em pixels do mundo. */
    private static final float IMPACT_OUTLINE = 5f;

    /**
     * Impact frame: a cena vira um quadro de mangá por um instante. Fundo chapado, linhas de
     * velocidade saindo do impacto, os dois lutadores em silhueta (preta com contorno branco no
     * RED, branca no VOID) e a estrela do impacto. Mesma câmera da luta (com o soco de zoom).
     */
    private void drawImpactFrame(Canvas canvas, float zoom, float left, float top) {
        boolean red = feel.impactStyle() == ImpactFeel.RED;
        float x = feel.impactX(), y = feel.impactY();
        canvas.drawColor(red ? IMPACT_RED : IMPACT_VOID);
        canvas.save();
        canvas.scale(zoom, zoom);
        canvas.translate(-left, -top);
        drawSpeedLines(canvas, x, y, red ? IMPACT_RED_LINES : IMPACT_VOID_LINES, red ? 80 : 70, red ? 9f : 5f);
        drawImpactSilhouettes(canvas, red);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        drawStar(canvas, x, y, red ? 120f : 125f);
        if (!red) {
            // VOID: estrela vazada, com o miolo branco.
            paint.setColor(IMPACT_VOID);
            drawStar(canvas, x, y, 100f);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(x, y, 34f, paint);
        } else {
            canvas.drawCircle(x, y, 38f, paint);
        }
        canvas.restore();
    }

    private static final int FINAL_PAPEL = 0xFFF1EBDA, FINAL_TINTA = 0xFF141218, FINAL_PRETO = 0xFF08080C;

    /**
     * Finalização de mangá ({@link FinalizacaoManga}): fundo chapado cobrindo o cenário e linhas
     * de concentração convergindo em quem caiu, atrás dos lutadores. Desenhada no mundo, com
     * os tamanhos em pixels de tela (divididos pelo zoom).
     */
    private void drawFinalizacao(Canvas canvas, float zoom, float left, float top) {
        float k = finalizacao.forca();
        int side = finalizacao.vitima();
        if (k <= 0f || side < 0) return;
        boolean ultra = finalizacao.tipo() == FinalizacaoManga.ULTRA;
        int alpha = Math.round(255f * k);
        float w = VW / zoom, h = VH / zoom;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(comAlfa(ultra ? FINAL_PRETO : FINAL_PAPEL, alpha));
        // Sobra em volta para o close inclinado e a tremida.
        canvas.drawRect(left - w * 0.4f, top - h * 0.4f, left + w * 1.4f, top + h * 1.4f, paint);
        CombatFighter v = engine.fighter(side);
        // No peito de quem caiu, mas dentro da tela (o raio do Ultra joga o rival para fora dela).
        float x = clamp(v.x + (side == PLAYER ? tagOffset() : engine.teams.tagOffset(OPPONENT)),
            left + w * 0.18f, left + w * 0.82f);
        float y = clamp(chestY(v), top + h * 0.25f, top + h * 0.8f);
        finalizacaoLinhas.setSeed(finalizacao.semente() * 7919L + 17L);
        // Muitos traços finos e algumas cunhas grossas, como o 集中線 desenhado à mão.
        int tinta = comAlfa(ultra ? Color.WHITE : FINAL_TINTA, alpha);
        drawLinhasDeConcentracao(canvas, x, y, zoom, tinta, 190, 1.6f);
        drawLinhasDeConcentracao(canvas, x, y, zoom, tinta, ultra ? 50 : 64, 14f);
        if (ultra) drawLinhasDeConcentracao(canvas, x, y, zoom, comAlfa(finalizacao.cor(), alpha), 46, 9f);
    }

    /**
     * Linhas de concentração do mangá: cunhas que vêm da borda da tela e afinam até a ponta,
     * deixando um miolo vazio em volta de (x, y). Larguras e tamanhos em pixels de tela.
     */
    private void drawLinhasDeConcentracao(Canvas canvas, float x, float y, float zoom, int color, int count,
                                          float maxWidth) {
        Random r = finalizacaoLinhas;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        float outer = 1800f / zoom;
        // Todas as cunhas num caminho só: um desenho por cor (antes, um por cunha: centenas).
        impactPath.reset();
        for (int i = 0; i < count; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            float inner = (170f + r.nextFloat() * r.nextFloat() * 260f) / zoom;
            float t = r.nextFloat();
            float half = (0.8f + t * t * maxWidth) / zoom;
            float cos = (float)Math.cos(a), sin = (float)Math.sin(a);
            impactPath.moveTo(x + cos * inner, y + sin * inner);
            impactPath.lineTo(x + cos * outer - sin * half, y + sin * outer + cos * half);
            impactPath.lineTo(x + cos * outer + sin * half, y + sin * outer - cos * half);
            impactPath.close();
        }
        canvas.drawPath(impactPath, paint);
    }

    private static int comAlfa(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    /** Linhas finas no centro e largas na borda, apontando para o impacto. */
    private void drawSpeedLines(Canvas canvas, float x, float y, int color, int count, float maxWidth) {
        Random lines = new Random(feel.impactSeed() * 7919L);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        impactPath.reset();
        for (int i = 0; i < count; i++) {
            double a = lines.nextDouble() * Math.PI * 2;
            float r0 = 150f + lines.nextFloat() * 160f, r1 = 2200f;
            float w = (1f + lines.nextFloat() * (maxWidth - 1f)) * 3f;
            float cos = (float)Math.cos(a), sin = (float)Math.sin(a);
            impactPath.moveTo(x + cos * r0, y + sin * r0);
            impactPath.lineTo(x + cos * r1 - sin * w, y + sin * r1 + cos * w);
            impactPath.lineTo(x + cos * r1 + sin * w, y + sin * r1 - cos * w);
            impactPath.close();
        }
        canvas.drawPath(impactPath, paint);
    }

    private void drawStar(Canvas canvas, float x, float y, float size) {
        Random jitter = new Random(feel.impactSeed() * 31L + Math.round(size));
        int spikes = 14;
        impactPath.reset();
        for (int i = 0; i < spikes * 2; i++) {
            double a = i * Math.PI / spikes + (jitter.nextFloat() - 0.5f) * 0.16f;
            float r = i % 2 == 0 ? size * (0.75f + jitter.nextFloat() * 0.5f) : size * 0.32f;
            float px = x + (float)Math.cos(a) * r, py = y + (float)Math.sin(a) * r;
            if (i == 0) impactPath.moveTo(px, py); else impactPath.lineTo(px, py);
        }
        impactPath.close();
        canvas.drawPath(impactPath, paint);
    }

    /** Os dois lutadores em campo como silhueta, na mesma pose deste quadro. */
    private void drawImpactSilhouettes(Canvas canvas, boolean red) {
        SpriteFighterRenderer[] renderers = {opponentSpriteRenderer, spriteFighterRenderer};
        CombatFighter[] fighters = {opponent(), player()};
        float[] offsets = {engine.teams.tagOffset(OPPONENT), tagOffset()};
        for (int i = 0; i < 2; i++) {
            canvas.save();
            canvas.translate(offsets[i], 0f);
            if (red) {
                // Contorno: a silhueta branca deslocada em volta, a preta por cima.
                renderers[i].setOverride(whiteSilhouette);
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4;
                    float dx = (float)Math.cos(a) * IMPACT_OUTLINE, dy = (float)Math.sin(a) * IMPACT_OUTLINE;
                    canvas.translate(dx, dy);
                    drawFighter(canvas, renderers[i], fighters[i], false, false);
                    canvas.translate(-dx, -dy);
                }
            }
            renderers[i].setOverride(red ? inkSilhouette : whiteSilhouette);
            drawFighter(canvas, renderers[i], fighters[i], false, false);
            renderers[i].setOverride(null);
            canvas.restore();
        }
    }

    /** Glow and sparks around a fighter whose team is in Overdrive. */
    private void drawOverdriveAura(Canvas c, CombatFighter f, float offset) {
        if (!engine.teams.overdriveActive(f.index)) return;
        effects.drawOverdrive(c, paint, f.x + offset, f.y, engine.frame() * FIXED_STEP);
    }

    /** Algum botão de ataque continua sob um dedo (carrega a Onda do p01). */
    private boolean attackHeld() {
        return lightPointer != -1 || mediumPointer != -1 || heavyPointer != -1 || hardware.attackHeld();
    }

    /** Eco de sombra: cada cópia de golpe que já começou, roxa e translúcida, onde a dona estava. */
    private void drawEchoes(Canvas c) {
        for (CombatEngine.Echo e : engine.echoes) {
            if (!e.visible() || e.move.animation == null) continue;
            CharacterDefinition owner = engine.fighter(e.owner).character();
            if (!echoRenderer.character().id.equals(owner.id)) echoRenderer.setCharacter(owner.id);
            echoRenderer.pose(e.move.animation.id, e.move.animationTime((e.frame + 0.5f) * FIXED_STEP), 0f);
            echoRenderer.draw(c, e.x, e.y, e.facing, false, false);
        }
    }

    /** Capangas: correndo (loop), o soco ou a ombrada no tempo do golpe, e correndo de volta. */
    private void drawHenchmen(Canvas c) {
        for (CombatEngine.Henchman h : engine.henchmen) {
            if (!h.visible()) continue;
            CharacterDefinition owner = engine.fighter(h.owner).character();
            String clip = h.phase == CombatEngine.Henchman.STRIKE
                ? (h.shoulder ? SpriteStates.CAPANGA_SHOULDER : SpriteStates.CAPANGA_PUNCH)
                : SpriteStates.CAPANGA_RUN;
            CharacterDefinition.Animation animation = owner.animations.get(clip);
            if (animation == null) continue;
            if (!henchmanRenderer.character().id.equals(owner.id)) henchmanRenderer.setCharacter(owner.id);
            float time = h.phase == CombatEngine.Henchman.STRIKE
                ? animation.duration * Math.min(0.999f, h.frame / (float)engine.config.henchmanStrikeFrames)
                : h.frame * FIXED_STEP;
            henchmanRenderer.pose(clip, time, 0f);
            henchmanRenderer.draw(c, h.x, h.y, h.direction, false, false);
        }
    }

    /** Mecânica própria visível: veneno, armadura, postura de contra-golpe, esquiva ou fúria. */
    private void drawMechanicAura(Canvas c, CombatFighter f, float offset) {
        float time = engine.frame() * FIXED_STEP;
        float x = f.x + offset;
        AttackDefinition.Mechanics m = f.mechanics();
        int frame = f.attackFrame;
        if (m.armorAt(frame) && f.armorUsed < m.armorHits) {
            effects.drawMechanicAura(c, paint, x, f.y, time, EffectsRenderer.AURA_ARMOR, 1f);
        } else if (m.counterAt(frame)) {
            effects.drawMechanicAura(c, paint, x, f.y, time, EffectsRenderer.AURA_COUNTER, 1f);
        } else if (m.evadeAt(frame)) {
            effects.drawMechanicAura(c, paint, x, f.y, time, EffectsRenderer.AURA_EVADE, 1f);
        }
        if (f.state.poisoned()) {
            effects.drawMechanicAura(c, paint, x, f.y, time, EffectsRenderer.AURA_POISON, 1f);
        }
        CharacterDefinition.Fighter profile = f.state.profile;
        if (profile.furyPermille > 0 && !f.ko()) {
            // Fury shows once a third of the life is gone and burns brighter as it drops.
            float lost = 1f - f.state.life / (float)profile.maxLife;
            effects.drawMechanicAura(c, paint, x, f.y, time, EffectsRenderer.AURA_FURY, (lost - 0.33f) * 1.5f);
        }
    }

    /** Speed lines behind a fighter during its air dash. */
    private void drawAirDashTrail(Canvas c, CombatFighter f) {
        if (f.airDashFrames <= 0) return;
        c.save();
        // The trail is drawn for a rightward motion; mirror it for a leftward dash.
        if (f.airDashDirection < 0) c.scale(-1f, 1f, f.x, 0f);
        effects.drawAirDash(c, paint, f.x, f.y, f.state.profile.color);
        c.restore();
    }

    /** The partner behind the point: the assist doing its move, or the former point leaving. */
    private void drawPartner(Canvas c) {
        CombatFighter partner = partnerBody(engine.team(PLAYER));
        if (partner != null) drawFighter(c, partnerSpriteRenderer, partner, false, false);
        CombatFighter cpuPartner = partnerBody(engine.team(OPPONENT));
        if (cpuPartner != null) drawFighter(c, opponentPartnerRenderer, cpuPartner, false, false);
    }

    private void drawPlayer(Canvas c) {
        CombatFighter p = player();
        drawFighter(c, spriteFighterRenderer, p, p.framesSinceHit < 8, p.framesSinceBlock < 7);
    }

    private void drawControls(Canvas c) {
        hud.drawControls(c, hudState);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!activityActive) return true;
        // Tocou na tela com o controle virtual à vista: o tutorial volta a falar do toque.
        if (touchControls && event.getActionMasked() == MotionEvent.ACTION_DOWN) esquema = Comandos.Esquema.TOQUE;
        pendingInput.offer(MotionEvent.obtain(event));
        return true;
    }

    /** Partes do controle virtual (o resto, como IA/DEBUG/SELEÇÃO, continua clicável). */
    private static boolean isPadControl(ControlsLayout.Control control) {
        switch (control) {
            case DPAD: case SUPER: case LIGHT: case MEDIUM: case HEAVY: case COMBO:
            case TAG: case THROW: case PUSHBLOCK: case OVERDRIVE:
                return true;
            default:
                return false;
        }
    }

    private void processPendingInput() {
        processHardwareKeys();
        MotionEvent latestMove = null;
        MotionEvent event;
        while ((event = pendingInput.poll()) != null) {
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                if (latestMove != null) latestMove.recycle();
                latestMove = event;
                continue;
            }

            if (latestMove != null) {
                handleTouch(latestMove);
                latestMove.recycle();
                latestMove = null;
            }
            handleTouch(event);
            event.recycle();
        }

        if (latestMove != null) {
            handleTouch(latestMove);
            latestMove.recycle();
        }
        syncHeldTouches();
    }

    private void clearPendingInput() {
        MotionEvent event;
        while ((event = pendingInput.poll()) != null) event.recycle();
    }

    /**
     * Touch only records what the player holds and presses. Whether a press starts an
     * attack is decided by the engine (buffer, state machine and cancel windows).
     */
    private void handleTouch(MotionEvent event) {
        if (getWidth() == 0 || getHeight() == 0) return;
        float sx = getWidth() / VW;
        float sy = getHeight() / VH;
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        trackTouches(event, action, index, sx, sy);

        if (pausa.isOpen()) {
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                applyPause(pausa.tap(event.getX(index) / sx, event.getY(index) / sy));
            }
            return;
        }
        if (ajusteTamanho.isOpen()) {
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                ajusteTamanho.tap(event.getX(index) / sx, event.getY(index) / sy);
            }
            return;
        }
        if (ajusteCaixas.isOpen()) {
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                ajusteCaixas.tap(event.getX(index) / sx, event.getY(index) / sy);
            }
            return;
        }
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            if (paginaFinal.isActive()) {
                // During the "Página Final" any touch counts for the timing (only for the player's
                // own ultra: the CPU's one plays without the bonus).
                if (ultraAttacker == PLAYER) paginaFinal.tap();
                return;
            }
            int pointerId = event.getPointerId(index);
            float x = event.getX(index) / sx;
            float y = event.getY(index) / sy;
            if(debugOverlay && handleSizeTunerTap(x,y))return;

            ControlsLayout.Control control = ControlsLayout.controlAt(x, y);
            if (!touchControls && isPadControl(control)) control = ControlsLayout.Control.NONE;
            switch (control) {
                case AI_TOGGLE:
                    // IA ON: a batalha de verdade (2x2) começa do zero; IA OFF volta ao treino.
                    setOpponentAiEnabled(!ai.enabled());
                    restartBattle();
                    break;
                case DEBUG_TOGGLE:
                    debugOverlay = !debugOverlay;
                    break;
                case CHARACTER_SELECT:
                    // Versão leve: o botão do topo é o MENU (pausa).
                    openPause();
                    break;
                case HEAL_PLAYER:
                    // Both team members, so the reserve is also ready after a tag.
                    healPlayerPointer = pointerId;
                    healPlayerTeam();
                    break;
                case HEAL_OPPONENT:
                    healOpponentPointer = pointerId;
                    healOpponentTeam();
                    break;
                case DPAD:
                    // A second finger on the D-pad is ignored; the first one owns it (a dono que
                    // já saiu da tela não conta: syncHeldTouches o solta).
                    if (dpadPointer != -1 && touchSlot(dpadPointer) < 0) dpadPointer = -1;
                    if (dpadPointer == -1) {
                        dpadPointer = pointerId;
                        pad.setDirection(ControlsLayout.dpadDirectionAt(x, y));
                    }
                    break;
                case SUPER:
                    superPointer = pointerId;
                    pad.press(PadInput.Button.SUPER);
                    break;
                case LIGHT:
                    lightPointer = pointerId;
                    pad.press(PadInput.Button.LIGHT);
                    break;
                case MEDIUM:
                    mediumPointer = pointerId;
                    pad.press(PadInput.Button.MEDIUM);
                    break;
                case HEAVY:
                    heavyPointer = pointerId;
                    pad.press(PadInput.Button.HEAVY);
                    break;
                case COMBO:
                    comboPointer = pointerId;
                    pad.press(PadInput.Button.AUTO);
                    break;
                case THROW:
                    throwPointer = pointerId;
                    pad.press(PadInput.Button.THROW);
                    break;
                case PUSHBLOCK:
                    pushblockPointer = pointerId;
                    pad.press(PadInput.Button.PUSHBLOCK);
                    break;
                case OVERDRIVE:
                    overdrivePointer = pointerId;
                    pad.press(PadInput.Button.OVERDRIVE);
                    break;
                case TAG:
                    // TAG calls the assist (again: Assist -> Tag); with down it is the raw tag.
                    tagPointer = pointerId;
                    pad.press(PadInput.Button.TAG);
                    break;
                default:
                    break;
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (dpadPointer != -1) {
                int pointerIndex = event.findPointerIndex(dpadPointer);
                if (pointerIndex >= 0) {
                    pad.setDirection(ControlsLayout.dpadDirectionAt(
                        event.getX(pointerIndex) / sx, event.getY(pointerIndex) / sy));
                }
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            int pointerId = event.getPointerId(index);
            if (pointerId == dpadPointer) {
                dpadPointer = -1;
                pad.setDirection(0);
            }
            if (pointerId == lightPointer) lightPointer = -1;
            if (pointerId == mediumPointer) mediumPointer = -1;
            if (pointerId == heavyPointer) heavyPointer = -1;
            if (pointerId == comboPointer) comboPointer = -1;
            if (pointerId == tagPointer) tagPointer = -1;
            if (pointerId == throwPointer) throwPointer = -1;
            if (pointerId == pushblockPointer) pushblockPointer = -1;
            if (pointerId == overdrivePointer) overdrivePointer = -1;
            if (pointerId == superPointer) superPointer = -1;
            if (pointerId == healPlayerPointer) healPlayerPointer = -1;
            if (pointerId == healOpponentPointer) healOpponentPointer = -1;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            releaseAllPointers();
            pad.setDirection(0);
        }
    }

    // ------------------------------------------------------------ pausa

    /** Pausa (MENU ou voltar) por cima da luta. Não abre no meio de um ultra. */
    private void openPause() {
        if (paginaFinal.isActive() || abertura.isActive() || ultraAttacker >= 0) return;
        // COMANDOS: os golpes de cada personagem, começando pelo do jogador.
        int[] atual = new int[1];
        pausa.comandos().setFichas(ListaComandos.fichas(selectionRoster(), player().character(), esquema, atual), atual[0]);
        pausa.abrir(null);
        selectionKeys.clear();
        pad.reset();
        releaseAllPointers();
    }

    private void handlePauseKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                pausa.mover(-1);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                pausa.mover(1);
                break;
            // ◀ ▶ mudam a VELOCIDADE quando o cursor está nela; fora dela, andam como ▲▼.
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (!pausa.ajustar(-1)) pausa.mover(-1);
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (!pausa.ajustar(1)) pausa.mover(1);
                break;
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                applyPause(pausa.confirmar());
                break;
            case KeyEvent.KEYCODE_BUTTON_B:
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
            case KeyEvent.KEYCODE_BUTTON_START:
                applyPause(pausa.voltar());
                break;
            default:
                break;
        }
    }

    private void applyPause(MenuPausa.Opcao opcao) {
        if (opcao == null) return;
        // A pausa fechou: guarda a VELOCIDADE para a próxima vez que o app abrir.
        if (speedPrefs != null && speedPrefs.getFloat(SPEED_KEY, MenuPausa.VELOCIDADE_PADRAO) != pausa.velocidade()) {
            speedPrefs.edit().putFloat(SPEED_KEY, pausa.velocidade()).apply();
        }
        if (speedPrefs != null && speedPrefs.getBoolean(FPS_KEY, true) != pausa.fps()) {
            speedPrefs.edit().putBoolean(FPS_KEY, pausa.fps()).apply();
        }
        if (speedPrefs != null && speedPrefs.getBoolean(IMPACT_KEY, true) != pausa.impacto()) {
            speedPrefs.edit().putBoolean(IMPACT_KEY, pausa.impacto()).apply();
        }
        // O NÍVEL da CPU vale já na luta que continua, e fica salvo.
        ai.setLevel(pausa.nivelCpu());
        if (speedPrefs != null && speedPrefs.getInt(CPU_LEVEL_KEY, MenuPausa.NIVEL_PADRAO) != pausa.nivelCpu()) {
            speedPrefs.edit().putInt(CPU_LEVEL_KEY, pausa.nivelCpu()).apply();
        }
        pad.reset();
        releaseAllPointers();
        // Versão leve: no lugar do menu inicial, REINICIAR LUTA.
        if (opcao == MenuPausa.Opcao.MENU) restartBattle();
    }

    // ------------------------------------------------------------ modo história

    // ------------------------------------------------------------ arcade

    /** Segura (ou solta) o último ponto de vida de todos os lutadores dos dois lados. */
    private void protegerVida(boolean proteger) {
        int piso = proteger ? 1 : 0;
        for (int side = 0; side < 2; side++) {
            for (FighterState member : engine.team(side).members) member.lifeFloor = piso;
        }
    }

    private static float clamp(float value, float min, float max) {
        return Arena.clamp(value, min, max);
    }
}
