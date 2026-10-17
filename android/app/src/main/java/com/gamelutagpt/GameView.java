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
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.gamelutagpt.historia.Campanha;
import com.gamelutagpt.historia.Cenas;
import com.gamelutagpt.historia.Episodio;
import com.gamelutagpt.historia.HistoriaListener;
import com.gamelutagpt.historia.MenuCapitulos;
import com.gamelutagpt.historia.MenuInicial;
import com.gamelutagpt.historia.MenuPausa;
import com.gamelutagpt.historia.PaginaHistoria;
import com.gamelutagpt.historia.ProgressoHistoria;
import com.gamelutagpt.historia.Torneio;
import com.gamelutagpt.historia.CinematicaLuta;
import com.gamelutagpt.historia.ConversaLuta;
import com.gamelutagpt.historia.Temporada;
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
    implements SurfaceHolder.Callback, Runnable, UltraListener, HistoriaListener {
    // Team and opponent rules come from the generated character packs; DUPLA opens the
    // character select, where the player picks the pair.
    private int teamIndex;
    private FighterState[] team = membersOf(0);
    private final CharacterSelect characterSelect = new CharacterSelect(selectionRoster());
    private final ConcurrentLinkedQueue<Integer> selectionKeys = new ConcurrentLinkedQueue<>();
    private CharacterSelectRenderer characterSelectRenderer;

    // Training opponent: our point character (same body and moves), in its normal colors.
    // É o titular escolhido para o CPU; quem está em campo agora é opponentPoint().
    private FighterState opponentFighter = new FighterState(team[0].character, "CPU");
    // O parceiro do CPU: no treino (IA OFF) só aparece no seletor; no modo batalha (IA ON) luta.
    private String opponentTagId = "p04";

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
    /** Demo buttons: while one plays it drives both fighters (pad and CPU brain are ignored). */
    private final DemoDirector demo = new DemoDirector();
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

    // Super cinematic (presentation of the SUPER move; timing comes from its frames).
    private static final float SUPER_DARKEN_DURATION = 0.16f;
    private static final float SUPER_POSE_DURATION = 0.30f;
    private static final float SUPER_FLASH_DURATION = 0.12f;
    private static final float SUPER_RELEASE_DURATION = 0.20f;
    private static final float SUPER_RECOVER_DURATION = 0.22f;
    /** A cinemática do Super corpo a corpo toca mais rápido (as pancadas vêm logo depois do flash). */
    private static final float STRIKING_SUPER_CINEMATIC_SPEED = 2.4f;
    private float superCameraZoom = 1f;
    private int superDarkAlpha = 0;
    private int superFlashAlpha = 0;
    private float superChargeTime = 0f;

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
    private final UltraPack[] ultraPacks = new UltraPack[2];
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
    private static final String DEFAULT_STAGE = "templo_lua";
    private StageScene stageScene;
    /** Cenário carregado agora (pasta em assets/stages). Cada luta pode pedir o seu. */
    private String stageId;
    /** Desenho pela GPU (Android 8+); cai para o Canvas por software se falhar. */
    private boolean hardwareCanvas = Build.VERSION.SDK_INT >= 26;
    private final UltraSounds ultraSounds;

    // Modo história (story-core): páginas de mangá entre as lutas do episódio.
    private final PaginaHistoria historia;
    private Temporada temporada;
    /** Cenas chamadas pelo nome (historia/cenas.json), carregadas na primeira vez. */
    private Cenas cenas;
    /** O que fazer quando a cena que está tocando acabar (playScene); null no modo história. */
    private Runnable sceneAfter;
    /** Modo tutorial: o treino guiado dos golpes no dojo (o boneco não usa a IA). */
    private final TutorialDirector tutorial = new TutorialDirector();
    /**
     * Conversa nos quadros laterais durante uma luta da história (o "conversa" do passo de
     * luta): no treino, Takatoshi e Dafu a cada lição; nas lutas, o parceiro de tag e o
     * adversário que espera. Não para a luta. Padrão: docs/historia/motor-conversas.md.
     */
    private ConversaLuta conversa;
    /** Lição do treino que já teve a fala da conversa (-1: nenhuma). */
    private int conversaLicao = -1;
    /** O KO desta luta já chamou o momento "vitoria" ou "derrota". */
    private boolean conversaKo;
    /**
     * Cinemática dentro da luta da história (o "cinematica" do passo de luta): num gatilho
     * (vida baixa, começo, tempo, KO) faixas pretas entram, o controle trava e um roteiro guia
     * os dois lutadores e fala nos quadros da conversa. Padrão: docs/historia/motor-cinematicas.md.
     */
    private CinematicaLuta cinematica;
    /** Os comandos dos dois lutadores enquanto a cinemática tem o controle. */
    private final CinematicaDirector cinematicaDirector = new CinematicaDirector();
    /** A cinemática terminou a luta ("jogador", "rival" ou "interrompida"); null = não terminou. */
    private String cinematicaFim;
    /** Quadros de luta desde o "LUTEM!" (gatilho de tempo da cinemática). */
    private int storyFightFrames;
    /** Câmera lenta da cinemática: quanto da próxima passada da luta já se acumulou. */
    private float cinematicaPassada;
    /** Menu inicial (abre com o app): MODO HISTÓRIA ou MODO TESTE. */
    private final MenuInicial menuInicial;
    /** MODO HISTÓRIA com save: CONTINUAR e os capítulos já liberados. */
    private final MenuCapitulos capitulos = new MenuCapitulos();
    /** O save do modo história (lido na primeira vez que o modo abre). */
    private ProgressoHistoria progresso;
    private SharedPreferences storyPrefs;
    private static final String STORY_PREFS = "historia_v1";
    private static final String STORY_SAVE_KEY = "progresso";
    /** Pausa (botão MENU ou tecla voltar): continuar ou voltar ao menu inicial. */
    private final MenuPausa pausa = new MenuPausa();
    /**
     * MODO HISTÓRIA do menu inicial: a temporada desde o Prólogo, sem os botões de teste nem o
     * DEBUG; voltar (botão MENU ou tecla) abre a pausa e leva ao menu, nunca à seleção de teste.
     * Falso no MODO TESTE (inclusive na história tocada pelo botão HISTÓRIA de teste).
     */
    private boolean storyMode;
    /** Texto do botão do topo da luta: "◀ SELEÇÃO" no teste, "◀ MENU" na história. */
    private String selectButtonLabel = SELECT_LABEL;
    private static final String SELECT_LABEL = "◀ SELEÇÃO";
    /** A última escolha da seleção no modo teste (titular, parceiro, CPU, parceiro do CPU). */
    private String[] testPicks = {"player_base", "p08", "p03", "p06"};
    /**
     * Entrou no MODO TESTE pelo menu e a luta de trás ainda é a de antes (da história, ou a do
     * construtor): se a seleção for cancelada, a luta de teste começa com {@link #testPicks}.
     */
    private boolean testFightPending;
    /** Duelo 1x1 da história: um lutador por lado (sem parceiro no motor nem no HUD). */
    private boolean duel;
    /** Episódio da temporada tocando agora, ou -1 fora do modo história. */
    private int storyEpisode = -1;
    /** Frames desde o KO de uma luta da história; a página volta depois da comemoração. */
    private int storyKoFrames;
    private static final int STORY_KO_FRAMES = 150;
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
    private String assistHudStatus = "ASSIST: PRONTO";
    private String swapHudStatus = "TROCA: PRONTA (↓+TAG)";
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
    private final java.util.Map<String, UltraPack> cpuUltraPacks = new java.util.HashMap<>();

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
    }

    private void releaseAllPointers() {
        dpadPointer = lightPointer = mediumPointer = heavyPointer = comboPointer = tagPointer = throwPointer = pushblockPointer = overdrivePointer = superPointer = -1;
        healPlayerPointer = healOpponentPointer = -1;
    }

    /** Frees the sound pool when the Activity is destroyed. */
    public void release() {
        ultraSounds.release();
    }

    public GameView(Context context) {
        this(context, false);
    }

    /**
     * {@code emSegundoPlano}: a arte pesada (folhas dos lutadores, seleção, cenário, ultras) é
     * decodificada na thread "Carregando", com a tela de carregamento na frente. Decodificar
     * isso na thread principal levava vários segundos, e um toque nesse meio-tempo virava
     * "não está respondendo" (ANR). Testes e PC continuam carregando tudo na hora.
     */
    public GameView(Context context, boolean emSegundoPlano) {
        super(context);
        AjusteTamanho.load(context);
        AjusteCaixas.load(context);
        speedPrefs = context.getSharedPreferences(SPEED_PREFS, Context.MODE_PRIVATE);
        storyPrefs = context.getSharedPreferences(STORY_PREFS, Context.MODE_PRIVATE);
        pausa.setVelocidade(speedPrefs.getFloat(SPEED_KEY, MenuPausa.VELOCIDADE_PADRAO));
        pausa.setImpacto(speedPrefs.getBoolean(IMPACT_KEY, true));
        pausa.setNivelCpu(speedPrefs.getInt(CPU_LEVEL_KEY, MenuPausa.NIVEL_PADRAO));
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
        historia = new PaginaHistoria(this, ultraAssets);
        menuInicial = new MenuInicial(ultraAssets);
        menuInicial.setVersao(VERSION_HUD_LABEL);
        conversa = new ConversaLuta(ultraAssets);
        cinematica = new CinematicaLuta(ultraAssets, conversa, palcoCinematica);
        stageId = DEFAULT_STAGE;
        holder = getHolder();
        holder.addCallback(this);
        setFocusable(true);
        setKeepScreenOn(true);
        if (emSegundoPlano) {
            // O loop do jogo começa por esta fila: tela de carregamento e a thread "Carregando".
            loadQueue.add(() -> carregarArte(context));
        } else {
            carregarArte(context);
        }
    }

    /** A parte pesada da montagem: só decodifica arte e sons, nada que a thread da UI leia. */
    private void carregarArte(Context context) {
        hud.loadArt(context);
        // One decode per atlas for the whole match: team packs (tag never decodes
        // mid-fight) and the opponent share the same cache.
        for (FighterState fighter : team) atlases.preload(fighter.character);
        spriteFighterRenderer = new SpriteFighterRenderer(atlases, team[0].character);
        // O adversário aparece com as cores normais da arte (sem o filtro pálido de antes).
        opponentSpriteRenderer = new SpriteFighterRenderer(atlases, opponentFighter.character);
        partnerSpriteRenderer = new SpriteFighterRenderer(atlases, team[1].character);
        echoRenderer = new SpriteFighterRenderer(atlases, team[0].character);
        echoRenderer.setTint(SpriteFighterRenderer.shadowEcho());
        henchmanRenderer = new SpriteFighterRenderer(atlases, team[0].character);
        opponentPartnerRenderer = new SpriteFighterRenderer(atlases, opponentFighter.character);
        stageScene = loadStage(ultraAssets, stageId);
        characterSelectRenderer = new CharacterSelectRenderer(context);
        loadTeamUltras();
    }

    private static String[] selectionRoster() {
        java.util.ArrayList<String> ids=new java.util.ArrayList<>();
        String[] visualOrder={"player_base","p03","p06","p02","p08","p04","p05","p07","p09","p10","p12"};
        for(String wanted:visualOrder) for(String available:GeneratedCharacters.SELECTABLE)
            if(wanted.equals(available)) ids.add(available);
        // P09–P16 without gameplay yet stay as locked concept slots after these.
        return ids.toArray(new String[0]);
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
        for (int i = 0; i < team.length; i++) {
            ultraPacks[i] = loadUltraPack(ultraAssets, team[i]);
            ultraSounds.loadUltra(ultraFolder(team[i]));
        }
    }

    /**
     * DUPLA: the next pair of the roster takes the fight. Fresh round with the intro; the
     * training CPU becomes the new point character, and the atlases of whoever left are
     * freed (each character is ~135 MB decoded).
     */
    private void selectTeam(int index) {
        teamIndex = index % GeneratedCharacters.TEAMS.length;
        String[] ids = GeneratedCharacters.TEAMS[teamIndex];
        selectPair(ids[0], ids[1]);
    }

    /** Compatibilidade com troca de dupla existente. */
    private void selectPair(String point, String partner) {
        // Legacy team preset: training CPU continues mirroring the chosen point.
        selectTeams(point, partner, point, opponentTagId);
    }

    /** A seleção muda a dupla P1 e o titular do adversário de treino. */
    private void selectTeams(String point, String partner, String cpuPoint, String cpuPartner) {
        selectTeams(point, partner, cpuPoint, cpuPartner, false);
    }

    /**
     * Duelo 1x1 (a regra escondida do torneio): um lutador de cada lado, sem tag nem assist.
     * O "parceiro" guardado no time do jogador é o próprio adversário, só para a arte e o ultra
     * dele já estarem carregados; o motor recebe um membro por lado.
     */
    private void selectDuel(String player, String cpu) {
        selectTeams(player, cpu, cpu, player, true);
    }

    private void selectTeams(String point, String partner, String cpuPoint, String cpuPartner, boolean solo) {
        // Duplas novas decodificam a arte delas (segundos): com a tela de carregamento.
        carregando(() -> selectTeamsNow(point, partner, cpuPoint, cpuPartner, solo));
    }

    private void selectTeamsNow(String point, String partner, String cpuPoint, String cpuPartner, boolean solo) {
        hud.resetEffects();
        duel = solo;
        team = membersOf(point, partner);
        opponentFighter = new FighterState(GeneratedCharacters.get(cpuPoint), "CPU");
        opponentTagId = cpuPartner;
        CharacterDefinition cpuPartnerCharacter = GeneratedCharacters.get(cpuPartner);
        if (flow.battle() && !duel) {
            // Batalha 2x2: o parceiro do CPU entra de assist, na troca e quando o titular cai,
            // então a arte dele fica carregada (sem decodificar no meio da luta).
            atlases.retainOnly(team[0].character, team[1].character, opponentFighter.character, cpuPartnerCharacter);
            atlases.preload(cpuPartnerCharacter);
        } else {
            atlases.retainOnly(team[0].character, team[1].character, opponentFighter.character);
        }
        spriteFighterRenderer.setCharacter(team[0].character.id);
        partnerSpriteRenderer.setCharacter(team[1].character.id);
        opponentSpriteRenderer.setCharacter(opponentFighter.character.id);
        engine.setTeam(PLAYER, duel ? new FighterState[] {team[0]} : team);
        if (flow.battle() && !duel) {
            engine.setTeam(OPPONENT, opponentFighter, new FighterState(cpuPartnerCharacter, "CPU"));
        } else {
            engine.setTeam(OPPONENT, opponentFighter);
        }
        engine.refreshOverdrive(PLAYER);
        engine.refreshOverdrive(OPPONENT);
        engine.restage(PLAYER_START_X, OPPONENT_START_X);
        loadTeamUltras();
        loadCpuUltras();
        startIntro();
    }

    /**
     * O Ultra do CPU usa a arte do personagem dele. Quem também está no time do jogador
     * reaproveita o pack já carregado; os outros carregam aqui, antes da luta.
     */
    private void loadCpuUltras() {
        cpuUltraPacks.clear();
        if (!flow.battle()) return;
        for (FighterState member : engine.team(OPPONENT).members) {
            String id = member.character.id;
            if (teamUltraPack(id) != null || cpuUltraPacks.containsKey(id)) continue;
            cpuUltraPacks.put(id, loadUltraPack(ultraAssets, member));
            ultraSounds.loadUltra(ultraFolder(member));
        }
    }

    private UltraPack teamUltraPack(String characterId) {
        for (int i = 0; i < team.length; i++) {
            if (team[i].character.id.equals(characterId)) return ultraPacks[i];
        }
        return null;
    }

    /** DUPLA (and the first launch): pause the fight and pick the pair. */
    private void openCharacterSelect() {
        if (demo.active() || paginaFinal.isActive() || abertura.isActive() || ultraAttacker >= 0) return;
        // Voltar à seleção abandona o episódio, a cena e o treino que estiverem tocando.
        historia.stop();
        storyEpisode = -1;
        sceneAfter = null;
        tutorial.stop();
        descarregarConversa();
        useStage(null);
        hud.resetEffects();
        characterSelect.open(team[0].character.id, team[1].character.id,
            opponentFighter.character.id, opponentTagId);
        selectionKeys.clear();
        pad.reset();
        releaseAllPointers();
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        notarEsquema(keyCode, event);
        if (menuInicial.isOpen()) {
            // No menu inicial a tecla voltar fecha o app, como em toda tela raiz do Android.
            if (keyCode == KeyEvent.KEYCODE_BACK) return super.onKeyDown(keyCode, event);
            offerMenuKey(keyCode, event);
            return true;
        }
        if (capitulos.isOpen()) {
            // Nos capítulos, voltar leva ao menu inicial.
            offerMenuKey(keyCode, event);
            return true;
        }
        // Na luta, voltar/START abre a pausa (o loop do jogo lê a fila).
        if (pausa.isOpen() || ajusteTamanho.isOpen() || ajusteCaixas.isOpen() || characterSelect.isOpen() ||
            historia.coversScreen() || isPauseKey(keyCode)) {
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
                if (storyMode) openPause();
                else openCharacterSelect();
            } else if (!storyMode) {
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

    private void handleSelectionKey(int keyCode) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT: characterSelect.moveCursor(-1,0); break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: characterSelect.moveCursor(1,0); break;
            case KeyEvent.KEYCODE_DPAD_UP: characterSelect.moveCursor(0,-1); break;
            case KeyEvent.KEYCODE_DPAD_DOWN: characterSelect.moveCursor(0,1); break;
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if(characterSelect.ready()) confirmCharacterSelection();
                else characterSelect.selectCursor();
                break;
            case KeyEvent.KEYCODE_BUTTON_B:
            case KeyEvent.KEYCODE_ESCAPE:
            case KeyEvent.KEYCODE_BACK:
                if(characterSelect.back()==CharacterSelect.Result.CANCEL) closeCharacterSelect();
                break;
            case KeyEvent.KEYCODE_BUTTON_X: characterSelect.randomFocused(); break;
            case KeyEvent.KEYCODE_BUTTON_Y: characterSelect.toggleSettings(); break;
            case KeyEvent.KEYCODE_BUTTON_L1:
            case KeyEvent.KEYCODE_BUTTON_R1: characterSelect.swapFocusedTeam(); break;
            case KeyEvent.KEYCODE_BUTTON_START:
                if(characterSelect.ready()) confirmCharacterSelection();
                else characterSelect.selectCursor();
                break;
            default: break;
        }
    }

    private void handleCharacterSelectTap(float x, float y) {
        switch (characterSelect.tap(x, y)) {
            case CONFIRM:
                confirmCharacterSelection();
                break;
            case CANCEL:
                closeCharacterSelect();
                break;
            default:
                break;
        }
    }

    private void confirmCharacterSelection() {
        if(!characterSelect.ready())return;
        characterSelectRenderer.beginFightTransition();
        characterSelect.close();
        testFightPending = false;
        testPicks = new String[] {characterSelect.point(), characterSelect.partner(),
            characterSelect.opponentPoint(), characterSelect.opponentPartner()};
        selectTeams(testPicks[0], testPicks[1], testPicks[2], testPicks[3]);
    }

    /** Seleção cancelada: volta para a luta (a de teste do zero, se veio do menu inicial). */
    private void closeCharacterSelect() {
        characterSelect.close();
        if (testFightPending) {
            testFightPending = false;
            selectTeams(testPicks[0], testPicks[1], testPicks[2], testPicks[3]);
        }
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
            Runnable abrir = () -> {
                startIntro();
                // O app abre no menu inicial: MODO HISTÓRIA ou MODO TESTE (a luta de teste de antes).
                openMainMenu();
            };
            // Arte ainda na fila (o loop ainda não começou): o menu abre depois dela, na mesma
            // thread de carregamento, e não aqui na thread da UI.
            if (!running && !loadQueue.isEmpty()) loadQueue.add(abrir);
            else abrir.run();
        }
        surfaceReady = true;
        startLoopIfReady();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {}

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
            advanceSimulation(elapsedSeconds);
            if (!running) break;
            drawFrame();
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
        processPendingInput();
        // VELOCIDADE (pausa): o relógio da luta anda mais devagar, então golpes, pulos, IA,
        // timer e efeitos desaceleram juntos sem mexer no frame data dos packs. Menus,
        // pausa, seleção, páginas da história e a Página Final ficam na velocidade normal.
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
        return !menuInicial.isOpen() && !pausa.isOpen() && !ajusteTamanho.isOpen() &&
            !ajusteCaixas.isOpen() && !characterSelect.isOpen() && !historia.coversScreen() &&
            !paginaFinal.isActive();
    }

    /** One simulation frame: inputs, engine step, then presentation state. */
    private void update() {
        if (stageScene != null) stageScene.update(FIXED_STEP);
        if (menuInicial.isOpen()) {
            // A luta fica parada (e escondida) atrás do menu.
            pad.drainInto(ignoredInput);
            Integer key;
            while ((key = selectionKeys.poll()) != null) handleMenuKey(key);
            MenuInicial.Opcao escolha = menuInicial.update(FIXED_STEP);
            if (escolha == MenuInicial.Opcao.HISTORIA) startStoryMode();
            else if (escolha == MenuInicial.Opcao.TESTE) startTestMode();
            return;
        }
        if (capitulos.isOpen()) {
            pad.drainInto(ignoredInput);
            Integer key;
            while (capitulos.isOpen() && (key = selectionKeys.poll()) != null) handleChaptersKey(key);
            capitulos.update(FIXED_STEP);
            return;
        }
        if (pausa.isOpen()) {
            // Tudo para (página, luta, treino, conversa) até CONTINUAR ou MENU INICIAL.
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
        if (characterSelect.isOpen()) {
            // Freeze combat while processing queued hardware controls on this same game thread.
            pad.drainInto(ignoredInput);
            Integer key;
            while ((key = selectionKeys.poll()) != null) handleSelectionKey(key);
            return;
        }
        if (historia.coversScreen()) {
            // The fight waits behind the story page; any key turns the page, B skips.
            pad.drainInto(ignoredInput);
            Integer key;
            while ((key = selectionKeys.poll()) != null) handleStoryKey(key);
            historia.update(FIXED_STEP);
            return;
        }
        updateUltraBars();
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
        if (cinematicaLenta()) return;
        boolean tagging = isTagAnimationActive();

        boolean intro = introFrame >= 0;
        if (intro || flow.holdsFighters()) {
            // Both fighters play their INTRO, then stand in the READY; nobody moves until GO.
            pad.drainInto(ignoredInput);
            playerInput.clear();
            opponentInput.clear();
        } else if (demo.active()) {
            // Touches never reach the fight during a demo.
            pad.drainInto(ignoredInput);
            demo.fill(playerInput, opponentInput);
        } else if (cinematica.controleTravado()) {
            // Cinemática: o roteiro move os dois; o pad e a IA ficam de fora.
            pad.drainInto(ignoredInput);
            cinematicaDirector.fill(engine, playerInput, opponentInput);
        } else if (tutorial.active()) {
            // Treino: o jogador joga com o pad; o boneco só faz o que a lição pede.
            pad.setHolding(attackHeld());
            pad.drainInto(playerInput);
            tutorial.fillDummy(opponentInput);
        } else {
            pad.setHolding(attackHeld());
            pad.drainInto(playerInput);
            if (tagging) {
                opponentInput.clear();
            } else {
                ai.fill(engine, OPPONENT, opponentInput);
            }
        }
        if (player().ko() || opponent().ko()) {
            // Inputs must not start a new attack/move once the match is over.
            playerInput.clear();
            opponentInput.clear();
        }
        // Com a cinemática esperando a vida baixa (ou rodando), ninguém cai antes da cena.
        protegerVida(cinematica.protegeVida());
        engine.step(playerInput, opponentInput);
        // O parceiro só entra depois do KO quando o raio do ultra já saiu da tela.
        flow.update(engine, ultraAttacker < 0 && !engine.ultraSequenceRunning());
        updateOpponentVisuals();
        if (flow.restartDue() && !historia.waitingFight() && !tutorial.active()) restartBattle();
        demo.afterStep();
        // O "treino completo" espera a conversa do fim (ela anda sozinha, sem toque).
        tutorial.setHoldEnd(conversa.falando());
        tutorial.afterStep();
        // Quem assiste no fundo (Dafu e Takatoshi) reage ao que aconteceu no treino.
        if (stageScene != null) {
            for (String reaction : tutorial.reactions()) stageScene.react(reaction);
        }
        updateConversa();
        updateCinematica(intro);
        // A lição do Ultra termina com o raio ainda no ar: o treino sai depois dele.
        if (tutorial.finished() && ultraAttacker < 0) finishTutorial();
        if (engine.ultraConnected() >= 0) beginUltraCinematic(engine.ultraConnected());

        boolean punishNow = engine.cues().contains("PUNISH");
        for (CombatEngine.HitEvent event : engine.events()) {
            boolean ultraHit = ultraAttacker >= 0 && event.attacker == ultraAttacker && !event.blocked &&
                "ULTRA".equals(event.moveId);
            if (ultraHit) ultraDamageDealt += event.damage;
            if ("THROW".equals(event.moveId)) showBanner("AGARRÃO!");
            CombatFighter hitFighter = engine.fighter(event.defender);
            boolean knockout = !event.blocked && hitFighter.ko() && koVictim != hitFighter;
            if (knockout) koVictim = hitFighter;
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
            if ("WALL_BOUNCE".equals(cue) || "GROUND_BOUNCE".equals(cue) || "PUNISH".equals(cue)) {
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
            if ("ULTRA_FINISH".equals(cue)) {
                beamShake = Math.max(beamShake, 22f);
                // Último dano do Ultra em sequência: impact frame em quem apanhou e câmera lenta.
                feel.slow(ImpactFeel.ULTRA_SLOW_FRAMES, ultraSlowScale);
                CombatFighter hit = ultraVictim();
                if (pausa.impacto() && hit != null) feel.impactFrame(ultraFinalImpact, hit.x, chestY(hit));
            }
        }
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
        updateStoryFight();
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
        for (CharacterDefinition character : keep) atlases.preload(character);
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
            if (duel) return null;
            FighterState partner = engine.team(OPPONENT).partner();
            return partner != null ? partner.character : GeneratedCharacters.get(opponentTagId);
        }
        @Override public FighterState opponentReserve() { return engine.team(OPPONENT).partner(); }
        @Override public boolean battleActive() { return flow.battle(); }
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
        @Override public int presentationEpoch() { return demo.demo() * 100 + demo.stepIndex(); }
        @Override public boolean assistInUse() { return isTagAnimationActive() || engine.team(PLAYER).assistOut(); }
        @Override public String assistStatusLabel() { return assistHudStatus; }
        @Override public String swapStatusLabel() { return swapHudStatus; }
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
        @Override public int demoPlaying() { return demo.demo(); }
        @Override public String demoTitle() { return demo.title(); }
        @Override public String demoCaption() { return demo.caption(); }
        @Override public boolean storyActive() { return historia.isActive() && !tutorial.active(); }
        @Override public boolean tutorialActive() { return tutorial.active(); }
        @Override public boolean hideTestButtons() { return testButtonsHidden(); }
        @Override public String selectButtonLabel() { return selectButtonLabel; }
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
                startUltraOpening(p);
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
        return f == engine.fighter(PLAYER) ? ultraPacks[engine.team(PLAYER).point] : cpuUltraPack(f.state);
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
        ultraPack = attacker == PLAYER ? ultraPacks[engine.team(PLAYER).point] : cpuUltraPack(state);
        paginaFinal.start(ultraPack, engine.fighter(attacker).facing);
    }

    /** O pack do Ultra do CPU: o do time do jogador se é o mesmo personagem, o carregado na luta, ou o provisório. */
    private UltraPack cpuUltraPack(FighterState state) {
        UltraPack pack = teamUltraPack(state.character.id);
        if (pack == null) pack = cpuUltraPacks.get(state.character.id);
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
        ultraDamageDealt += engine.applyUltraHit(ultraAttacker, Math.round(ultraTotalDamage() * damageFraction));
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

    private boolean isSuperCinematicActive() {
        CombatFighter p = player();
        return p.attacking() && p.attack.kind == AttackDefinition.Kind.SUPER;
    }

    /** Seconds since the player's Super started (its cinematic clock). */
    private float superElapsed() {
        return (player().attackFrame + 1) * FIXED_STEP;
    }

    private void updateSuperVisuals() {
        if (!isSuperCinematicActive()) {
            superCameraZoom = 1f;
            superDarkAlpha = 0;
            superFlashAlpha = 0;
            superChargeTime = 0f;
            return;
        }
        float t = superElapsed();
        // Super que bate corpo a corpo: a cinemática passa no tempo do flash (o rival já está
        // livre) e a câmera abre para a sequência de pancadas aparecer inteira.
        if (player().attack.melee()) t *= STRIKING_SUPER_CINEMATIC_SPEED;
        float poseStart = SUPER_DARKEN_DURATION;
        float flashStart = poseStart + SUPER_POSE_DURATION;
        float releaseStart = flashStart + SUPER_FLASH_DURATION;
        float recoverStart = releaseStart + SUPER_RELEASE_DURATION;
        superChargeTime = Math.max(0f, t - poseStart);
        if (t < poseStart) {
            float k = clamp(t / SUPER_DARKEN_DURATION, 0f, 1f);
            superDarkAlpha = Math.round(190f * k);
            superCameraZoom = 1f + 0.18f * k;
            superFlashAlpha = 0;
        } else if (t < flashStart) {
            float k = clamp((t - poseStart) / SUPER_POSE_DURATION, 0f, 1f);
            superDarkAlpha = 190;
            superCameraZoom = 1.18f + 0.18f * k;
            superFlashAlpha = 0;
        } else if (t < releaseStart) {
            float k = clamp((t - flashStart) / SUPER_FLASH_DURATION, 0f, 1f);
            superDarkAlpha = 190;
            superCameraZoom = 1.36f;
            superFlashAlpha = Math.round(235f * (1f - Math.abs(0.5f - k) * 2f));
        } else if (t < recoverStart) {
            float k = clamp((t - releaseStart) / SUPER_RELEASE_DURATION, 0f, 1f);
            superDarkAlpha = Math.round(190f * (1f - 0.40f * k));
            superFlashAlpha = Math.round(255f * (1f - k));
            superCameraZoom = 1.36f - 0.16f * k;
        } else {
            float k = clamp((t - recoverStart) / SUPER_RECOVER_DURATION, 0f, 1f);
            superDarkAlpha = Math.round(114f * (1f - k));
            superFlashAlpha = 0;
            superCameraZoom = 1.20f - 0.20f * k;
        }
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

    private boolean isTagAnimationActive() {
        return engine.team(PLAYER).tagging();
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
            assistHudStatus = "ASSIST: INDISPONIVEL";
            swapHudStatus = "TROCA: EM ANDAMENTO";
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
        assistHudStatus = assist;
        swapHudStatus = tagSeconds == 0 ? "TROCA: PRONTA (↓+TAG)" : "TROCA: " + tagSeconds + "s";
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
        if (canvas == null) return;

        try {
            renderFrame(canvas);
        } finally {
            holder.unlockCanvasAndPost(canvas);
        }
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
            if (menuInicial.isOpen()) {
                renderCanvas.begin(canvas);
                menuInicial.render(renderCanvas);
                canvas.restore();
                return;
            }
            if (capitulos.isOpen()) {
                renderCanvas.begin(canvas);
                capitulos.render(renderCanvas);
                canvas.restore();
                return;
            }
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
            // Renderização exclusiva do menu: nenhum lutador/HUD fica sob a interface.
            if (characterSelect.isOpen()) {
                characterSelectRenderer.draw(canvas, paint, characterSelect);
                canvas.restore();
                return;
            }
            if (historia.coversScreen()) {
                renderCanvas.begin(canvas);
                historia.render(renderCanvas);
                pausa.render(renderCanvas);
                canvas.restore();
                return;
            }

            float cinematicZoom = cinematica.zoom();
            float baseZoom = camera.zoom * superCameraZoom * cinematicZoom;
            float punchZoom = feel.zoom();
            float renderZoom = baseZoom * ultraCameraZoom * punchZoom;
            float visibleWorldWidth = VW / renderZoom;
            float renderCameraX = camera.x;
            float renderCameraTop = camera.top;
            // Zoom e foco da cinemática. Sem foco, o zoom segura o chão no mesmo lugar da tela (os
            // pés não somem na faixa); focando alguém (quem fala), segura o peito dele: um plano
            // médio, com o rosto na tela e os pés podendo sair. O centro vai até ele.
            float focoJogador = cinematica.foco(CinematicaLuta.JOGADOR);
            float focoRival = cinematica.foco(CinematicaLuta.RIVAL);
            if (cinematicZoom != 1f || focoJogador > 0f || focoRival > 0f) {
                float pivotY = GROUND_Y + focoJogador * (chestY(player()) - GROUND_Y) +
                    focoRival * (chestY(opponent()) - GROUND_Y);
                float pivotScreenY = (pivotY - camera.top) * camera.zoom * superCameraZoom;
                renderCameraTop = pivotY - pivotScreenY / baseZoom;
                renderCameraX += focoJogador * (player().x + tagOffset() - camera.x) +
                    focoRival * (opponent().x + engine.teams.tagOffset(OPPONENT) - camera.x);
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
                stageScene.setCamera(renderZoom, cameraLeft, renderCameraTop);
                stageScene.drawBackground(renderCanvas);
            }

            canvas.save();
            canvas.scale(renderZoom, renderZoom);
            canvas.translate(-cameraLeft, -renderCameraTop);
            if (stageScene == null) drawScenario(canvas);
            drawFloorReflections(canvas);
            // Golpe forte: o cenário escurece por um instante e os lutadores ficam na frente.
            effects.drawWorldOverlay(canvas, paint, feel.darkAlpha(), 0, 0, 8);
            if (beam) {
                drawUltraBeamOverlay(canvas, false);
                raioFinal.drawBehind(renderCanvas, ultraPack, ultraBeamFrame());
            }
            drawDamageDummy(canvas);
            float effectTime = engine.frame() * FIXED_STEP;
            effects.drawPuddles(renderCanvas, engine.puddles, effectTime);
            effects.drawEnergyProjectiles(canvas, renderCanvas, engine.energyProjectiles);
            for (int i = 0; i < 2; i++) effects.drawSpecialEffect(renderCanvas, engine.fighter(i), effectTime);
            effects.drawSuperProjectiles(canvas, renderCanvas, engine.superProjectiles);
            if (isSuperCinematicActive()) {
                effects.drawWorldOverlay(canvas, paint, superDarkAlpha, 0, 0, 8);
                canvas.save();
                if (player().facing < 0) {
                    canvas.scale(-1f, 1f, player().x, 0f);
                }
                effects.drawSuperCharge(
                    canvas, paint, player().x, player().y, superChargeTime,
                    activeFighter().profile.color
                );
                canvas.restore();
            }
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
                effects.drawWorldOverlay(canvas, paint, superFlashAlpha, 255, 255, 255);
            }
            if (debugOverlay) debug.drawWorld(canvas, engine);
            canvas.restore();
            if (stageScene != null) stageScene.drawForeground(renderCanvas);
            if (negativeLayer >= 0) canvas.restoreToCount(negativeLayer);
            if (feel.impactStyle() != ImpactFeel.NONE) drawImpactFrame(canvas, renderZoom, cameraLeft, renderCameraTop);
            if (cinemaLayer >= 0) canvas.restoreToCount(cinemaLayer);
            if (ultraBars < 0.001f && cinematica.faixas() < 0.001f && !isSuperCinematicActive()) {
                drawGroundMarkers(canvas, renderZoom, cameraLeft, renderCameraTop);
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
                // Na cinemática o HUD some enquanto as faixas entram, e o controle de toque sai.
                float faixas = Math.max(cinematica.faixas(), ultraBars);
                if (faixas < 0.999f) {
                    int layer = faixas > 0.001f
                        ? canvas.saveLayerAlpha(0f, 0f, VW, VH, Math.round(255f * (1f - faixas))) : -1;
                    drawHud(canvas);
                    if (layer >= 0) canvas.restoreToCount(layer);
                }
                if (touchControls && !cinematica.controleTravado() && faixas < 0.001f) drawControls(canvas);
                cinematica.renderFaixas(renderCanvas);
                conversa.render(renderCanvas);
                cinematica.renderEfeitos(renderCanvas);
            }
            if (abertura.isActive()) abertura.render(renderCanvas);
            if (paginaFinal.isActive()) {
                paginaFinal.render(renderCanvas);
            }
            // A pausa por cima da luta parada.
            pausa.render(renderCanvas);

            canvas.restore();
            characterSelectRenderer.drawFightTransition(canvas);
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
        int layer = c.saveLayerAlpha(f.x - 280f, f.y - 520f, f.x + 280f, f.y + 30f, alpha);
        drawFighter(c, renderer, f, false, false);
        c.restoreToCount(layer);
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
        hud.drawHud(c, hudState, characterSelectRenderer);
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
        hud.drawTutorial(c, tutorial, esquema);
        if (debugOverlay) {
            debug.drawPanel(c, engine);
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
        if (demo.active() || paginaFinal.isActive() || abertura.isActive() || ultraAttacker >= 0) return true;
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
        boolean celebrating = animation == null && onPoint && matchWon && rival.ko() && !f.ko() && f.grounded &&
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
        renderer.draw(c, f.x, visualY, f.facing, damageFlash, guardFlash);
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

    /** Linhas finas no centro e largas na borda, apontando para o impacto. */
    private void drawSpeedLines(Canvas canvas, float x, float y, int color, int count, float maxWidth) {
        Random lines = new Random(feel.impactSeed() * 7919L);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        for (int i = 0; i < count; i++) {
            double a = lines.nextDouble() * Math.PI * 2;
            float r0 = 150f + lines.nextFloat() * 160f, r1 = 2200f;
            float w = (1f + lines.nextFloat() * (maxWidth - 1f)) * 3f;
            float cos = (float)Math.cos(a), sin = (float)Math.sin(a);
            impactPath.reset();
            impactPath.moveTo(x + cos * r0, y + sin * r0);
            impactPath.lineTo(x + cos * r1 - sin * w, y + sin * r1 + cos * w);
            impactPath.lineTo(x + cos * r1 + sin * w, y + sin * r1 - cos * w);
            impactPath.close();
            canvas.drawPath(impactPath, paint);
        }
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

        if (menuInicial.isOpen()) {
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                menuInicial.tap(event.getX(index) / sx, event.getY(index) / sy);
            }
            return;
        }
        if (capitulos.isOpen()) {
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                applyChapter(capitulos.tap(event.getX(index) / sx, event.getY(index) / sy));
            }
            return;
        }
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
        if (characterSelect.isOpen()) {
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                handleCharacterSelectTap(event.getX(index) / sx, event.getY(index) / sy);
            }
            return;
        }
        if (historia.coversScreen()) {
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                float x = event.getX(index) / sx;
                float y = event.getY(index) / sy;
                // MENU (só no modo história) abre a pausa; o resto da tela vira a página.
                if (historia.menuAt(x, y)) {
                    openPause();
                } else {
                    historia.tap(x, y);
                }
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
            if (tutorial.active() && ControlsLayout.tutorialSkipAt(x, y)) {
                // PULAR no fim do treino não espera a conversa terminar.
                if (tutorial.completing()) conversa.stop();
                tutorial.skip();
                return;
            }

            ControlsLayout.Control control = ControlsLayout.controlAt(x, y);
            if (!touchControls && isPadControl(control)) control = ControlsLayout.Control.NONE;
            // No tutorial e nas lutas com conversa os botões de teste não aparecem nem respondem.
            if (testButtonsHidden() && ControlsLayout.isTestButton(control)) control = ControlsLayout.Control.NONE;
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
                    // No modo história o botão do topo é o MENU (pausa), não a seleção de teste.
                    if (storyMode) {
                        openPause();
                    } else {
                        openCharacterSelect();
                    }
                    break;
                case HEAL_PLAYER:
                    // Both team members, so the reserve is also ready after a tag.
                    healPlayerPointer = pointerId;
                    healPlayerTeam();
                    break;
                case TEAM_SELECT:
                    openCharacterSelect();
                    break;
                case STORY:
                    startStory();
                    break;
                case TUTORIAL:
                    startTutorialMode();
                    break;
                case HEAL_OPPONENT:
                    healOpponentPointer = pointerId;
                    healOpponentTeam();
                    break;
                case DPAD:
                    // A second finger on the D-pad is ignored; the first one owns it.
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

    // ------------------------------------------------------------ menu inicial e pausa

    /**
     * Menu inicial: abandona o que estiver tocando (episódio, cena, treino, seleção, pausa) e
     * mostra o logo com MODO HISTÓRIA e MODO TESTE. A luta fica parada atrás.
     */
    private void openMainMenu() {
        historia.stop();
        historia.setBotaoMenu(false);
        storyEpisode = -1;
        sceneAfter = null;
        storyMode = false;
        selectButtonLabel = SELECT_LABEL;
        tutorial.stop();
        descarregarConversa();
        pausa.fechar();
        capitulos.fechar();
        characterSelect.close();
        testFightPending = false;
        hud.resetEffects();
        // A temporada dá o logo e o idioma dos textos do menu (sem ela, logo padrão e português).
        loadTemporada();
        menuInicial.abrir(temporada);
        selectionKeys.clear();
        pad.reset();
        releaseAllPointers();
    }

    private void handleMenuKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                menuInicial.mover(-1);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                menuInicial.mover(1);
                break;
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_BUTTON_START:
                menuInicial.confirmar();
                break;
            default:
                break;
        }
    }

    /** MODO HISTÓRIA: a temporada desde o Prólogo, sem botões de teste; voltar leva ao menu. */
    private void startStoryMode() {
        if (!loadTemporada()) {
            openMainMenu();
            return;
        }
        storyMode = true;
        selectButtonLabel = "◀ " + temporada.t(MenuPausa.MENU);
        debugOverlay = false;
        historia.setBotaoMenu(true);
        loadProgresso();
        if (progresso.temSave()) {
            // Já jogou: CONTINUAR de onde parou ou escolher um capítulo liberado.
            openChapters();
        } else {
            startStoryAt(0);
        }
    }

    /** O save do modo história; sem save (ou estragado), um novo com uma sorte nova para a chave. */
    private void loadProgresso() {
        if (progresso != null) return;
        progresso = ProgressoHistoria.fromJson(storyPrefs.getString(STORY_SAVE_KEY, ""), new Random().nextLong());
        if (temporada != null) progresso.limitar(temporada.episodios.size());
    }

    private void saveProgresso() {
        if (progresso == null) return;
        storyPrefs.edit().putString(STORY_SAVE_KEY, progresso.toJson()).apply();
    }

    /** Tela dos capítulos: CONTINUAR e os episódios (os não liberados ficam trancados). */
    private void openChapters() {
        java.util.List<MenuCapitulos.Capitulo> lista = new java.util.ArrayList<>();
        for (int i = 0; i < temporada.episodios.size(); i++) {
            String id = temporada.episodios.get(i);
            try {
                Episodio ep = temporada.loadEpisodio(ultraAssets, id);
                lista.add(new MenuCapitulos.Capitulo(ep.numero, ep.titulo));
            } catch (IOException | RuntimeException ex) {
                Log.w(LOG_TAG, "episódio " + id + " inválido", ex);
                lista.add(new MenuCapitulos.Capitulo(i, id));
            }
        }
        capitulos.abrir(temporada, lista, progresso);
        selectionKeys.clear();
        pad.reset();
        releaseAllPointers();
    }

    private void handleChaptersKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT: capitulos.mover(-1, 0); break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: capitulos.mover(1, 0); break;
            case KeyEvent.KEYCODE_DPAD_UP: capitulos.mover(0, -1); break;
            case KeyEvent.KEYCODE_DPAD_DOWN: capitulos.mover(0, 1); break;
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_BUTTON_START:
                applyChapter(capitulos.confirmar());
                break;
            case KeyEvent.KEYCODE_BUTTON_B:
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
                applyChapter(capitulos.voltar());
                break;
            default:
                break;
        }
    }

    private void applyChapter(Integer escolha) {
        if (escolha == null) return;
        if (escolha == MenuCapitulos.VOLTAR) {
            openMainMenu();
        } else if (escolha == MenuCapitulos.CONTINUAR) {
            startStoryAt(progresso.atual());
        } else {
            startStoryAt(escolha);
        }
    }

    /**
     * MODO HISTÓRIA a partir do episódio {@code index}, com a campanha (irmão e chave) como
     * estava no começo dele no save. Daí em diante a temporada segue normalmente.
     */
    private void startStoryAt(int index) {
        if (!loadTemporada()) {
            openMainMenu();
            return;
        }
        loadProgresso();
        tutorial.stop();
        descarregarConversa();
        sceneAfter = null;
        hud.resetEffects();
        pad.reset();
        releaseAllPointers();
        selectionKeys.clear();
        Campanha campanha = null;
        try {
            campanha = progresso.campanhaNoInicio(index, Torneio.duplasFromJson(ultraAssets.readText(Torneio.ARQUIVO)));
        } catch (IOException | RuntimeException ex) {
            Log.w(LOG_TAG, "torneio indisponível", ex);
        }
        historia.setCampanha(campanha);
        playStoryEpisode(Math.max(0, Math.min(index, temporada.episodios.size() - 1)));
    }

    /**
     * MODO TESTE: o que o app abria antes do menu, a luta de teste no cenário padrão com a
     * seleção de dupla, os botões de teste e o DEBUG.
     */
    private void startTestMode() {
        storyMode = false;
        selectButtonLabel = SELECT_LABEL;
        historia.setBotaoMenu(false);
        // Treino (IA OFF) no cenário padrão; o botão IA liga o modo batalha.
        setOpponentAiEnabled(false);
        openCharacterSelect();
        characterSelect.open(testPicks[0], testPicks[1], testPicks[2], testPicks[3]);
        testFightPending = true;
    }

    /** Pausa (MENU ou voltar) por cima da página ou da luta. Não abre no meio de um ultra. */
    private void openPause() {
        if (demo.active() || paginaFinal.isActive() || abertura.isActive() || ultraAttacker >= 0) return;
        loadTemporada();
        // COMANDOS: os golpes de cada personagem, começando pelo do jogador.
        int[] atual = new int[1];
        pausa.comandos().setFichas(ListaComandos.fichas(selectionRoster(), player().character(), esquema, atual), atual[0]);
        // O aviso de progresso perdido só vale para um episódio do modo história.
        pausa.abrir(temporada, storyMode && historia.isActive());
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
        if (opcao == MenuPausa.Opcao.MENU) openMainMenu();
    }

    // ------------------------------------------------------------ modo história

    /** HISTÓRIA: toca a temporada desde o primeiro episódio. */
    private void startStory() {
        if (demo.active() || paginaFinal.isActive() || abertura.isActive() || ultraAttacker >= 0) return;
        if (!loadTemporada()) return;
        tutorial.stop();
        descarregarConversa();
        sceneAfter = null;
        hud.resetEffects();
        pad.reset();
        releaseAllPointers();
        selectionKeys.clear();
        // Partida nova: a escolha do irmão (depois do Prólogo) e a chave do torneio começam do zero.
        historia.setCampanha(newCampanha());
        playStoryEpisode(0);
    }

    /** Campanha nova com as duplas do torneio; sem o torneio.json, a história pula a escolha e a chave. */
    private Campanha newCampanha() {
        try {
            return Campanha.load(ultraAssets, new Random().nextLong());
        } catch (IOException | RuntimeException ex) {
            Log.w(LOG_TAG, "torneio indisponível", ex);
            return null;
        }
    }

    /** TUTORIAL: a cena "tutorial" (a intro do Prólogo e o treino no dojo); no fim, a seleção. */
    private void startTutorialMode() {
        if (demo.active() || paginaFinal.isActive() || abertura.isActive() || ultraAttacker >= 0) return;
        tutorial.stop();
        descarregarConversa();
        playScene("tutorial", this::openCharacterSelect);
    }

    /**
     * Toca a cena {@code id} de historia/cenas.json (um episódio ou um trecho dele) por cima da
     * luta e, quando ela acaba, chama {@code after}. É o jeito de chamar uma cena de qualquer
     * ponto do jogo: {@code playScene("intro", () -> ...)}. As lutas e o treino que a cena
     * tiver acontecem no meio, como no modo história. Cena que não abre chama {@code after} direto.
     */
    void playScene(String id, Runnable after) {
        if (!loadTemporada()) {
            if (after != null) after.run();
            return;
        }
        Episodio cena;
        try {
            if (cenas == null) cenas = Cenas.load(ultraAssets);
            cena = cenas.carregar(temporada, ultraAssets, id);
        } catch (IOException | RuntimeException ex) {
            Log.w(LOG_TAG, "cena " + id + " indisponível", ex);
            if (after != null) after.run();
            return;
        }
        storyEpisode = -1;
        sceneAfter = after;
        if (historia.campanha() == null) historia.setCampanha(newCampanha());
        hud.resetEffects();
        pad.reset();
        releaseAllPointers();
        selectionKeys.clear();
        historia.start(temporada, cena);
    }

    /** A temporada (elenco, episódios) no idioma do aparelho; falso se não abriu. */
    private boolean loadTemporada() {
        if (temporada != null) return true;
        try {
            temporada = Temporada.load(ultraAssets);
            // Textos da história no idioma do aparelho; sem tradução, em português.
            temporada.setIdioma(ultraAssets, Locale.getDefault().getLanguage());
            return true;
        } catch (IOException | RuntimeException ex) {
            Log.w(LOG_TAG, "modo história indisponível", ex);
            temporada = null;
            return false;
        }
    }

    private void playStoryEpisode(int index) {
        storyEpisode = index;
        if (storyMode && progresso != null) {
            // Save: quem sair no meio volta ao começo deste episódio, com a campanha de agora.
            progresso.comecou(index, historia.campanha());
            saveProgresso();
        }
        String id = temporada.episodios.get(index);
        try {
            historia.start(temporada, temporada.loadEpisodio(ultraAssets, id));
        } catch (IOException | RuntimeException ex) {
            Log.w(LOG_TAG, "episódio " + id + " inválido", ex);
            storyEpisode = -1;
            leaveStory();
        }
    }

    private void handleStoryKey(int keyCode) {
        if (storyMode && isPauseKey(keyCode)) {
            // Modo história: voltar/START abre a pausa (dali, o menu inicial); B continua pulando.
            openPause();
            return;
        }
        if (historia.esperandoEscolha()) {
            // Escolha do irmão: ◀ Ryūga, ▶ Kōga, A confirma (não se pula).
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT: historia.moverEscolha(-1); break;
                case KeyEvent.KEYCODE_DPAD_RIGHT: historia.moverEscolha(1); break;
                case KeyEvent.KEYCODE_BUTTON_A:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_BUTTON_START:
                    historia.confirmarEscolha();
                    break;
                default:
                    break;
            }
            return;
        }
        boolean skip = keyCode == KeyEvent.KEYCODE_BUTTON_B || keyCode == KeyEvent.KEYCODE_BACK ||
            keyCode == KeyEvent.KEYCODE_ESCAPE;
        if (skip) {
            historia.tap((PaginaHistoria.SKIP_LEFT + PaginaHistoria.SKIP_RIGHT) * 0.5f,
                (PaginaHistoria.SKIP_TOP + PaginaHistoria.SKIP_BOTTOM) * 0.5f);
        } else {
            historia.tap(VW * 0.5f, VH * 0.5f);
        }
    }

    /** Luta de um episódio: depois do KO e da comemoração, a história continua. */
    private void updateStoryFight() {
        if (!historia.waitingFight()) return;
        if (cinematicaFim != null) {
            // A cinemática encerrou a luta: "rival" conta como derrota; "jogador" e
            // "interrompida", a história segue.
            boolean won = !"rival".equals(cinematicaFim);
            storyKoFrames = 0;
            descarregarConversa();
            pad.reset();
            releaseAllPointers();
            historia.resultadoLuta(won);
            return;
        }
        // No 2x2 a luta só acaba quando uma dupla inteira cai.
        boolean ended = flow.battle() ? flow.over() : player().ko() || opponent().ko();
        if (!ended || cinematica.seguraFimDaLuta()) {
            // A cena do KO (gatilho "ko") toca antes do resultado.
            storyKoFrames = 0;
            return;
        }
        boolean won = flow.battle() ? flow.winner() == PLAYER : opponent().ko() && !player().ko();
        if (!conversaKo && !tutorial.active()) {
            // Quem está de fora comenta o resultado; a história espera a fala acabar.
            conversaKo = true;
            conversa.momento(won ? "vitoria" : "derrota");
        }
        if (++storyKoFrames < STORY_KO_FRAMES || ultraAttacker >= 0 || conversa.falando()) return;
        storyKoFrames = 0;
        descarregarConversa();
        pad.reset();
        releaseAllPointers();
        historia.resultadoLuta(won);
    }

    @Override
    public void onHistoriaLuta(Episodio.Passo luta) {
        // Cenário e duplas da luta carregam com a tela de carregamento; o resto vem depois deles.
        carregando(() -> startStoryFight(luta));
    }

    private void startStoryFight(Episodio.Passo luta) {
        // Cada luta pode pedir o seu cenário (o treino é no dojo).
        useStage(luta.cenario);
        // Antes das duplas: luta de verdade (IA, 2x2) ou o treino, onde o boneco não usa a IA
        // e as lições dizem o que ele faz.
        setOpponentAiEnabled(!luta.tutorial());
        // A mesma rotina da seleção: duplas novas, vida cheia e a intro de luta. As duplas vêm
        // do passo, do irmão escolhido ("jogador"/"rival") ou da chave do torneio.
        String[][] lados = historia.lados(luta);
        if (lados != null && lados[0].length == 1 && fightersExist(lados[0][0], lados[1][0])) {
            // Duelo 1x1: um de cada lado.
            selectDuel(lados[0][0], lados[1][0]);
        } else if (lados != null && lados[0].length == 2 && fightersExist(lados[0][0], lados[0][1], lados[1][0], lados[1][1])) {
            selectTeams(lados[0][0], lados[0][1], lados[1][0], lados[1][1]);
        } else {
            if (lados != null) Log.w(LOG_TAG, "dupla da história sem golpes ainda; usando as duplas atuais");
            selectTeams(team[0].character.id, team[1].character.id, opponentFighter.character.id, opponentTagId);
        }
        String rotulo = historia.rotulo(luta);
        if (rotulo != null && temporada != null) rotulo = temporada.t(rotulo);
        // A conversa da luta (nos quadros laterais, sem parar a luta) começa com o "inicio".
        descarregarConversa();
        conversaLicao = -1;
        conversaKo = false;
        if (luta.conversa != null && temporada != null && !conversa.carregar(temporada, luta.conversa)) {
            Log.w(LOG_TAG, "conversa " + luta.conversa + " não abriu");
        }
        conversa.momento("inicio");
        storyFightFrames = 0;
        String irmao = historia.campanha() != null ? historia.campanha().irmao() : null;
        if (luta.cinematica != null && temporada != null && !cinematica.carregar(temporada, luta.cinematica, irmao)) {
            Log.w(LOG_TAG, "cinemática " + luta.cinematica + " não abriu");
        }
        if (luta.tutorial()) {
            // As lições não esperam quadro nenhum: a conversa fala por cima da luta.
            tutorial.setPanels(false);
            tutorial.start(engine, team, PLAYER_START_X);
            storyKoFrames = 0;
            pad.reset();
            releaseAllPointers();
            if (rotulo != null) showBanner(rotulo);
            return;
        }
        tutorial.stop();
        storyKoFrames = 0;
        pad.reset();
        releaseAllPointers();
        if (rotulo != null) showBanner(rotulo);
    }

    /** Todos têm pack de luta (p13–p16 ainda não têm). */
    private static boolean fightersExist(String... ids) {
        for (String id : ids) {
            try {
                GeneratedCharacters.get(id);
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onHistoriaSom(String id) {
        ultraSounds.play(Temporada.PASTA, id);
    }

    /**
     * A conversa da luta: no treino, a fala de cada lição quando ela começa, a do fim e as
     * reações ao que o jogador fez; numa luta da história, as reações aos golpes. Nada disso
     * para a luta.
     */
    private void updateConversa() {
        if (tutorial.active()) {
            int lesson = tutorial.lessonIndex();
            if (lesson != conversaLicao && tutorial.lesson() != null) {
                conversaLicao = lesson;
                conversa.momento(tutorial.lesson().id);
            }
            for (String reaction : tutorial.reactions()) {
                if ("fim".equals(reaction)) {
                    conversa.momento("fim");
                } else {
                    conversa.reacao(reaction);
                }
            }
        } else if (conversa.ativa() && !cinematica.rodando() && !player().ko() && !opponent().ko()) {
            // Golpe forte do jogador que entrou ("impacto") e golpe do adversário que pegou ("levou").
            for (CombatEngine.HitEvent e : engine.events()) {
                if (e.blocked) continue;
                if (e.attacker == PLAYER && TutorialDirector.BIG_MOVES.contains(e.moveId)) conversa.reacao("impacto");
                if (e.attacker == OPPONENT) conversa.reacao("levou");
            }
        }
        conversa.update(FIXED_STEP);
    }

    /** Fim da conversa e da cinemática da luta (luta nova, fim da luta, saída da história). */
    private void descarregarConversa() {
        conversa.descarregar();
        cinematica.descarregar();
        cinematicaDirector.parar();
        cinematicaFim = null;
    }

    /**
     * A cinemática da luta: confere o gatilho (vida de quem está lutando de cada lado, tempo
     * desde o "LUTEM!", KO) quando nada mais está tocando e anda a linha do tempo.
     */
    private void updateCinematica(boolean intro) {
        if (!cinematica.ativa()) return;
        boolean ko = flow.battle() ? flow.over() : player().ko() || opponent().ko();
        boolean lutando = !intro && !flow.holdsFighters();
        if (lutando && !ko) storyFightFrames++;
        // Não começa por cima de um Super, de um Ultra, de uma troca ou da Página Final.
        boolean livre = lutando && historia.waitingFight() && ultraAttacker < 0 && !engine.ultraSequenceRunning() &&
            !isSuperCinematicActive() && !isTagAnimationActive() && !paginaFinal.isActive();
        if (cinematica.armada() && livre) {
            cinematica.gatilho(vida(player()), vida(opponent()), storyFightFrames * FIXED_STEP, ko);
        }
        cinematica.update(FIXED_STEP);
    }

    /**
     * Câmera lenta (ou luta congelada) da cinemática: a luta só dá um passo quando a velocidade
     * acumulada chega a um quadro; nos outros quadros só a cena, as falas e as onomatopeias
     * andam. Verdadeiro se este quadro não tem passo da luta.
     */
    private boolean cinematicaLenta() {
        float velocidade = cinematica.velocidade();
        if (velocidade >= 1f) {
            cinematicaPassada = 0f;
            return false;
        }
        cinematicaPassada += velocidade;
        if (cinematicaPassada >= 1f) {
            cinematicaPassada -= 1f;
            return false;
        }
        cinematica.update(FIXED_STEP);
        conversa.update(FIXED_STEP);
        impacts.update(FIXED_STEP * velocidade);
        updateStoryFight();
        return true;
    }

    private static float vida(CombatFighter f) {
        return f.state.life / (float)Math.max(1, f.state.profile.maxLife);
    }

    /** Segura (ou solta) o último ponto de vida de todos os lutadores dos dois lados. */
    private void protegerVida(boolean proteger) {
        int piso = proteger ? 1 : 0;
        for (int side = 0; side < 2; side++) {
            for (FighterState member : engine.team(side).members) member.lifeFloor = piso;
        }
    }

    /** O que a cinemática pede à luta. */
    private final CinematicaLuta.Palco palcoCinematica = new CinematicaLuta.Palco() {
        @Override public void travarControle(boolean travar) {
            pad.reset();
            releaseAllPointers();
            playerInput.clear();
            opponentInput.clear();
            if (travar) {
                // Golpes à distância ainda no ar não atropelam a cena, e um TAG apertado no
                // último quadro (a IA trocando com a vida baixa) não sai depois.
                engine.energyProjectiles.clear();
                engine.superProjectiles.clear();
                engine.puddles.clear();
                engine.teams.dropRequests();
            } else {
                cinematicaDirector.parar();
            }
        }

        @Override public void mover(int lado, CinematicaLuta.Movimento movimento) {
            cinematicaDirector.mover(lado, movimento);
        }

        @Override public boolean movendo(int lado) {
            return cinematicaDirector.movendo(lado);
        }

        @Override public void terminarLuta(String vencedor) {
            cinematicaDirector.parar();
            cinematicaFim = vencedor;
        }

        @Override public boolean aDireita(int lado) {
            return engine.fighter(lado).x > engine.fighter(1 - lado).x;
        }
    };

    /**
     * Botões de teste escondidos: no modo história (o jogo de verdade), no tutorial e nas lutas
     * com conversa (o canto é dos quadros).
     */
    private boolean testButtonsHidden() {
        return storyMode || tutorial.active() || conversa.ativa() || cinematica.ativa();
    }

    /** Fim do treino (todas as lições, ou PULAR no fim): a cena ou a história continua. */
    private void finishTutorial() {
        tutorial.stop();
        descarregarConversa();
        pad.reset();
        releaseAllPointers();
        if (historia.waitingFight()) historia.resultadoLuta(true);
    }

    @Override
    public void onHistoriaFim(Episodio episodio) {
        if (sceneAfter != null) {
            // Cena chamada por playScene: quem chamou decide o que vem depois.
            Runnable after = sceneAfter;
            sceneAfter = null;
            after.run();
            return;
        }
        if (storyMode && progresso != null && storyEpisode >= 0) {
            // Episódio terminado: o próximo capítulo fica liberado.
            progresso.terminou(storyEpisode, historia.campanha());
            saveProgresso();
        }
        // Depois do "Próximo episódio" a temporada emenda no episódio seguinte (ep00 → ep01).
        int next = storyEpisode + 1;
        if (temporada != null && next < temporada.episodios.size()) {
            playStoryEpisode(next);
        } else {
            storyEpisode = -1;
            leaveStory();
        }
    }

    /** Fim (ou erro) da temporada: o modo história volta ao menu; o de teste, à seleção. */
    private void leaveStory() {
        if (storyMode) {
            openMainMenu();
        } else {
            openCharacterSelect();
        }
    }

    private static float clamp(float value, float min, float max) {
        return Arena.clamp(value, min, max);
    }
}
