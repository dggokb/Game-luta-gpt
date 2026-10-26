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
import com.gamelutagpt.historia.Arcade;
import com.gamelutagpt.historia.Campanha;
import com.gamelutagpt.historia.Cenas;
import com.gamelutagpt.historia.Episodio;
import com.gamelutagpt.historia.HistoriaListener;
import com.gamelutagpt.historia.MenuArcade;
import com.gamelutagpt.historia.MenuCapitulos;
import com.gamelutagpt.historia.MenuInicial;
import com.gamelutagpt.historia.MenuPausa;
import com.gamelutagpt.historia.NivelCpu;
import com.gamelutagpt.historia.PaginaHistoria;
import com.gamelutagpt.historia.ProgressoHistoria;
import com.gamelutagpt.historia.Torneio;
import com.gamelutagpt.historia.CinematicaLuta;
import com.gamelutagpt.historia.ConversaLuta;
import com.gamelutagpt.historia.Temporada;
import com.gamelutagpt.motion.Pose;
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
    /** Riscos de mangá dos golpes fortes (no corpo de quem apanhou e no rastro do golpe). */
    private final RiscosDeImpacto riscos = new RiscosDeImpacto();
    private final CombatFighter[] riscosLutadores = new CombatFighter[2];
    private final float[] riscosDeslocamento = new float[2];
    private final boolean[] riscosVisiveis = new boolean[2];
    /**
     * Efeitos vindos de vídeo (assets/efeitos, feitos pelo tools/sprites/efeito_video.py). Cada
     * evento tenta o dele e, se ainda não existe, usa o efeito clássico do ImpactEffects.
     */
    private final EfeitosVideo efeitos;
    private final EntradaSuper entradaSuper;
    /** A tela de vitória do lutador do jogador (só quem tem a cena em assets/vitorias). */
    private final TelaVitoria telaVitoria;
    /** A tela de vitória desta luta já tocou (ou não tinha cena). */
    private boolean vitoriaTocada;
    /** Nome do efeito de golpe por tamanho (fraco, médio, forte, especial). */
    private static final String[] EFEITO_GOLPE = {"impacto_fraco", "impacto_medio", "impacto_forte", "impacto_especial"};
    /** Janela ativa do golpe no passo anterior (o arco do golpe forte sai quando ela abre). */
    private final boolean[] ativoAntes = new boolean[2];
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
    /** Quadros que o plano fixo do Ultra (a p16) segura depois do golpe final. */
    private static final int ULTRA_FIXED_CAMERA_HOLD = 54;
    /** Explosão do canhão da p16 (desenhada pelo jogo, fora da folha): onde, para onde e desde quando. */
    private boolean ultraBlastArmed;
    private int ultraBlastStart = -1, ultraBlastDirection = 1;
    private float ultraBlastX, ultraBlastY, ultraBlastAngle;
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
    /**
     * O narrador do torneio: fala o que acontece na luta numa faixa de transmissão no rodapé
     * (sem parar a luta). Só nas lutas de verdade (batalha, Arcade e as lutas do torneio na
     * história). Padrão: docs/historia/narrador.md.
     */
    private com.gamelutagpt.historia.Narracao narracao;
    private final NarradorLuta narradorLuta = new NarradorLuta();
    private boolean narracaoPronta;
    /** O narrador em pessoa no cenário, atrás dos lutadores (placeholder animado). */
    private final com.gamelutagpt.historia.NarradorPalco narradorPalco = new com.gamelutagpt.historia.NarradorPalco();
    /** Luta narrada: o narrador está no palco. */
    private boolean narradorNoPalco;
    /** A câmera deste quadro é a da luta (sem close de Super, Ultra, soco de câmera ou cinemática). */
    private boolean cameraDaLuta;
    private final Runnable desenhaNarradorPalco = this::drawNarradorPalco;
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
    /** ARCADE: a escolha da dupla (oficial ou livre). */
    private final MenuArcade menuArcade;
    /** As duplas oficiais que lutam (os cards do {@link #menuArcade} e o resto da chave). */
    private final java.util.List<Torneio.Dupla> arcadeOficiais = new java.util.ArrayList<>();
    /** A dupla livre do Arcade está sendo montada na seleção (só a dupla do jogador). */
    private boolean arcadeSelectPending;
    /** Um Arcade em andamento: a CPU sobe de nível a cada rodada. */
    private boolean arcadeMode;
    /** O save do modo história (lido na primeira vez que o modo abre). */
    private ProgressoHistoria progresso;
    private SharedPreferences storyPrefs;
    private static final String STORY_PREFS = "historia_v1";
    private static final String STORY_SAVE_KEY = "progresso";
    /** Rótulo das ondas seguintes de uma luta com ondas (traduzido pela temporada). */
    private static final String ONDA = "ONDA";
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
    private String[] testPicks = {"player_base", "p02", "p03", "p04"};
    /** Cenário da luta de teste, escolhido no painel embaixo da grade da seleção. */
    private String testStage = DEFAULT_STAGE;
    private boolean stageChoicesLoaded;
    /**
     * Entrou no MODO TESTE pelo menu e a luta de trás ainda é a de antes (da história, ou a do
     * construtor): se a seleção for cancelada, a luta de teste começa com {@link #testPicks}.
     */
    private boolean testFightPending;
    /** Duelo 1x1 da história: um lutador por lado (sem parceiro no motor nem no HUD). */
    private boolean duel;
    /** Episódio da temporada tocando agora, ou -1 fora do modo história. */
    private int storyEpisode = -1;
    /** Os episódios da temporada, carregados uma vez para contar as lutas (nível da CPU). */
    private java.util.List<Episodio> episodiosTemporada;
    /** Frames desde o KO de uma luta da história; a página volta depois da comemoração. */
    private int storyKoFrames;
    /**
     * Luta da história com {@code "ondas"} (o "Sob a Mesma Lua"): a onda atual (0 = a primeira)
     * e a vida e o Super da dupla do jogador no fim da onda anterior (null na primeira).
     */
    private Episodio.Passo lutaComOndas;
    private int ondaDaLuta;
    private int[] vidaEntreOndas, superEntreOndas;
    /** Quem caiu numa onda volta na seguinte com esta parte da vida (fôlego). */
    private static final float FOLEGO_ENTRE_ONDAS = 0.2f;
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
    /** O telão da arena (semifinal e final): a luta de perto e o replay dos golpes fortes. */
    private final TelaoLuta telao = new TelaoLuta();
    /** Desenha os quadros do telão (de qualquer lutador; não mexe nos renderers da luta). */
    private SpriteFighterRenderer telaoRenderer;
    /** O Canvas do quadro, para o telão desenhar quando o cenário chega na tela dele. */
    private Canvas telaoCanvas;
    private final com.gamelutagpt.stage.StageScene.Telao desenhaTelao = (rc, l, t, r, b) -> desenharTelao(l, t, r, b);

    private void desenharTelao(float l, float t, float r, float b) {
        if (telaoCanvas == null || telaoRenderer == null) return;
        telao.desenhar(telaoCanvas, telaoRenderer, l, t, r, b, GROUND_Y, qualidade.efeitosLeves(), engine.frame() / 60f);
    }
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
        efeitos = new EfeitosVideo(context.getAssets());
        effects.loadImages(context.getAssets());
        entradaSuper = new EntradaSuper(context.getAssets());
        telaVitoria = new TelaVitoria(context.getAssets());
        AjusteTamanho.load(context);
        AjusteCaixas.load(context);
        speedPrefs = context.getSharedPreferences(SPEED_PREFS, Context.MODE_PRIVATE);
        qualidade = new QualidadeGrafica(speedPrefs);
        storyPrefs = context.getSharedPreferences(STORY_PREFS, Context.MODE_PRIVATE);
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
        historia = new PaginaHistoria(this, ultraAssets);
        menuInicial = new MenuInicial(ultraAssets);
        menuInicial.setVersao(VERSION_HUD_LABEL);
        menuArcade = new MenuArcade(ultraAssets);
        conversa = new ConversaLuta(ultraAssets);
        narracao = new com.gamelutagpt.historia.Narracao(ultraAssets);
        cinematica = new CinematicaLuta(ultraAssets, conversa, palcoCinematica);
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
    /** Seleção e cenário montados em segundo plano enquanto o menu está na tela. */
    private java.util.concurrent.FutureTask<CharacterSelectRenderer> selecaoAntecipada;
    private java.util.concurrent.FutureTask<StageScene> cenarioAntecipado;
    private String cenarioAntecipadoId;
    /** Sorteio do cenário de cada luta do Arcade. */
    private final Random sorteioCenario = new Random();
    /** A última combinação de lutadores antecipada (não repete a cada quadro). */
    private String lutadoresAntecipados = "";
    /** A luta da história cujas duplas já foram antecipadas. */
    private Episodio.Passo lutaAntecipada;

    /**
     * Começa, em segundo plano e sem travar o menu, o que a primeira saída dele vai pedir: a
     * tela de seleção, o cenário e as folhas da dupla de teste. O carregamento depois só espera
     * o que ainda faltar.
     */
    private void anteciparArteDaLuta(Context context) {
        final String[] picks = testPicks.clone();
        selecaoAntecipada = new java.util.concurrent.FutureTask<>(() -> {
            CharacterSelectRenderer selecao = new CharacterSelectRenderer(context);
            selecao.prepararHerois(picks);
            return selecao;
        });
        final String id = stageId;
        cenarioAntecipadoId = id;
        cenarioAntecipado = new java.util.concurrent.FutureTask<>(() -> loadStage(ultraAssets, id));
        Thread t = new Thread(() -> {
            selecaoAntecipada.run();
            cenarioAntecipado.run();
        }, "Antecipando");
        t.setDaemon(true);
        t.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 2));
        t.start();
        anteciparLutadores(testPicks);
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

    /** Na seleção: as escolhas da tela (as que ainda vão mudar também; o orçamento segura). */
    private void anteciparSelecao() {
        if (characterSelect.solo()) anteciparLutadores(characterSelect.point(), characterSelect.partner());
        else anteciparLutadores(characterSelect.point(), characterSelect.partner(),
            characterSelect.opponentPoint(), characterSelect.opponentPartner());
    }

    /** Nas páginas da história (e do Arcade): as duplas da próxima luta, enquanto o jogador lê. */
    private void anteciparHistoria() {
        Episodio.Passo luta = historia.proximaLuta();
        if (luta == null || luta == lutaAntecipada) return;
        lutaAntecipada = luta;
        String[][] lados = historia.lados(luta);
        if (lados == null) return;
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        for (String[] lado : lados) for (String id : lado) ids.add(id);
        anteciparLutadores(ids.toArray(new String[0]));
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
        efeitos.carregar();
        medicao.parte("efeitos");
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
        telaoRenderer = new SpriteFighterRenderer(atlases, team[0].character, false);
        medicao.parte("folhas");
        StageScene cenario = stageId.equals(cenarioAntecipadoId) ? resultado(cenarioAntecipado) : null;
        stageScene = cenario != null ? cenario : loadStage(ultraAssets, stageId);
        cenarioAntecipado = null;
        medicao.parte("cenario");
        CharacterSelectRenderer selecao = resultado(selecaoAntecipada);
        characterSelectRenderer = selecao != null ? selecao : new CharacterSelectRenderer(context);
        prepararRostosDoHud(team[0].character, team[1].character, opponentFighter.character);
        // PC e testes: os retratos na hora, como antes (a seleção desenhada já sai completa).
        if (carregarFolhasNaHora) characterSelectRenderer.carregarHerois(GeneratedCharacters.SELECTABLE);
        selecaoAntecipada = null;
        medicao.parte("selecao");
        loadTeamUltras();
        medicao.parte("ultras");
        medicao.fim(atlases);
        arteCarregada = true;
    }

    private static String[] selectionRoster() {
        java.util.ArrayList<String> ids=new java.util.ArrayList<>();
        for(String wanted:CharacterSelect.PAIR_ORDER) for(String available:GeneratedCharacters.SELECTABLE)
            if(wanted.equals(available)) ids.add(available);
        // Those without gameplay yet stay as locked concept slots after these.
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
        MedicaoCarregamento medicao = new MedicaoCarregamento("dupla " + point + "+" + partner + " x " + cpuPoint + "+" + cpuPartner);
        ultraEsperando = null;
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
        engine.setTeam(PLAYER, duel ? new FighterState[] {team[0]} : team);
        if (flow.battle() && !duel) {
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
        loadSuperIntros();
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

    /** A entrada do Super (retrato ao fundo) de quem está na luta, dos dois lados. */
    private void loadSuperIntros() {
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (FighterState member : engine.team(PLAYER).members) ids.add(member.character.id);
        for (FighterState member : engine.team(OPPONENT).members) ids.add(member.character.id);
        entradaSuper.manter(engine, ids.toArray(new String[0]));
        // A tela de vitória só é do jogador.
        java.util.List<String> nossos = new java.util.ArrayList<>();
        for (FighterState member : engine.team(PLAYER).members) nossos.add(member.character.id);
        telaVitoria.manter(stageId, nossos.toArray(new String[0]));
    }

    private UltraPack teamUltraPack(String characterId) {
        return teamHas(characterId) ? ultraPronto(characterId) : null;
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
        useStage(testStage);
        hud.resetEffects();
        characterSelect.open(team[0].character.id, team[1].character.id,
            opponentFighter.character.id, opponentTagId);
        loadStageChoices();
        characterSelect.selectStage(testStage);
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
        if (capitulos.isOpen() || menuArcade.isOpen()) {
            // Nos capítulos e na escolha da dupla do Arcade, voltar leva ao menu inicial.
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
                } else if (telaVitoria.ativa()) {
                    telaVitoria.pular();
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
                    case ANIMACAO: AnimacaoLutador.proximoNivel(); break;
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
            case KeyEvent.KEYCODE_BUTTON_L2: characterSelect.moveStage(-1); break;
            case KeyEvent.KEYCODE_BUTTON_R2: characterSelect.moveStage(1); break;
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
        if (arcadeSelectPending) {
            // Dupla livre do Arcade: os adversários vêm do sorteio da chave.
            arcadeSelectPending = false;
            startArcade(Arcade.livre(characterSelect.point(), characterSelect.partner()));
            return;
        }
        testFightPending = false;
        testPicks = new String[] {characterSelect.point(), characterSelect.partner(),
            characterSelect.opponentPoint(), characterSelect.opponentPartner()};
        // O cenário escolhido no painel entra junto, na tela de carregamento.
        final String[] picks = testPicks;
        final String cenario = testStage = characterSelect.stage();
        carregando(() -> {
            useStage(cenario);
            selectTeamsNow(picks[0], picks[1], picks[2], picks[3], false);
        });
    }

    /** Os cenários do painel da seleção: as pastas de assets/stages com stage.json, pelo nome. */
    private void loadStageChoices() {
        if (stageChoicesLoaded) return;
        stageChoicesLoaded = true;
        java.util.List<String> ids = new java.util.ArrayList<>(), names = new java.util.ArrayList<>();
        try {
            String[] pastas = getContext().getAssets().list("stages");
            if (pastas != null) {
                java.util.Arrays.sort(pastas);
                for (String id : pastas) {
                    try (java.io.InputStream in = getContext().getAssets().open("stages/" + id + "/stage.json")) {
                        String json = new String(readAll(in), java.nio.charset.StandardCharsets.UTF_8);
                        ids.add(id);
                        names.add(new org.json.JSONObject(json).optString("nome", id));
                    } catch (java.io.IOException | org.json.JSONException semCenario) {
                        // pasta sem stage.json: não é cenário
                    }
                }
            }
        } catch (java.io.IOException ex) {
            Log.w(LOG_TAG, "sem a lista de cenários da seleção", ex);
        }
        if (!ids.isEmpty()) characterSelect.setStages(ids.toArray(new String[0]), names.toArray(new String[0]));
    }

    private static byte[] readAll(java.io.InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        for (int n; (n = in.read(buffer)) > 0;) out.write(buffer, 0, n);
        return out.toByteArray();
    }

    /** Seleção cancelada: volta para a luta (a de teste do zero, se veio do menu inicial). */
    private void closeCharacterSelect() {
        characterSelect.close();
        if (arcadeSelectPending) {
            arcadeSelectPending = false;
            openArcadeMenu();
            return;
        }
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

    /**
     * Arcade: um cenário sorteado entre as pastas de assets/stages que têm stage.json, sem
     * repetir o da luta anterior (quando há mais de um). Null (o padrão) se não achar nenhum.
     */
    private String sortearCenario() {
        java.util.List<String> opcoes = new java.util.ArrayList<>();
        try {
            String[] pastas = getContext().getAssets().list("stages");
            if (pastas != null) {
                for (String id : pastas) {
                    String[] arquivos = getContext().getAssets().list("stages/" + id);
                    if (arquivos != null && java.util.Arrays.asList(arquivos).contains("stage.json")) opcoes.add(id);
                }
            }
        } catch (IOException ex) {
            Log.w(LOG_TAG, "sem a lista de cenários para o Arcade", ex);
        }
        if (opcoes.size() > 1) opcoes.remove(stageId);
        return opcoes.isEmpty() ? null : opcoes.get(sorteioCenario.nextInt(opcoes.size()));
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
            // O app abre no menu inicial: MODO HISTÓRIA ou MODO TESTE (a luta de teste de antes).
            // O menu é leve; a arte da luta carrega quando o jogador sair dele.
            startIntro();
            openMainMenu();
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
        return !menuInicial.isOpen() && !menuArcade.isOpen() && !pausa.isOpen() && !ajusteTamanho.isOpen() &&
            !ajusteCaixas.isOpen() && !characterSelect.isOpen() && !historia.coversScreen() &&
            !paginaFinal.isActive();
    }

    /** One simulation frame: inputs, engine step, then presentation state. */
    private void update() {
        if (stageScene != null) {
            stageScene.update(FIXED_STEP);
            updateTrovao();
        }
        if (menuInicial.isOpen()) {
            // A luta fica parada (e escondida) atrás do menu.
            pad.drainInto(ignoredInput);
            Integer key;
            while ((key = selectionKeys.poll()) != null) handleMenuKey(key);
            MenuInicial.Opcao escolha = menuInicial.update(FIXED_STEP);
            // A arte da luta só carrega aqui, na primeira saída do menu (o menu abre na hora).
            if (escolha == MenuInicial.Opcao.HISTORIA) comArteDaLuta(this::startStoryMode);
            else if (escolha == MenuInicial.Opcao.ARCADE) comArteDaLuta(this::openArcadeMenu);
            else if (escolha == MenuInicial.Opcao.TESTE) comArteDaLuta(this::startTestMode);
            return;
        }
        if (menuArcade.isOpen()) {
            pad.drainInto(ignoredInput);
            Integer key;
            while (menuArcade.isOpen() && (key = selectionKeys.poll()) != null) handleArcadeMenuKey(key);
            menuArcade.update(FIXED_STEP);
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
            if (characterSelect.isOpen()) anteciparSelecao();
            return;
        }
        if (historia.coversScreen()) {
            // The fight waits behind the story page; any key turns the page, B skips.
            pad.drainInto(ignoredInput);
            Integer key;
            while ((key = selectionKeys.poll()) != null) handleStoryKey(key);
            historia.update(FIXED_STEP);
            if (historia.coversScreen()) anteciparHistoria();
            return;
        }
        if (telaVitoria.ativa()) {
            // A tela de vitória cobre a luta parada; uma tecla (ou um toque) pula para o fim.
            pad.drainInto(ignoredInput);
            if (!selectionKeys.isEmpty()) {
                selectionKeys.clear();
                telaVitoria.pular();
            }
            telaVitoria.passo();
            if (!telaVitoria.ativa()) depoisDaVitoria();
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
                ai.setSecondsLeft(flow.timed() ? flow.clockSeconds() : -1);
                ai.fill(engine, OPPONENT, opponentInput);
            }
        }
        if (player().ko() || opponent().ko() || flow.over()) {
            // Inputs must not start a new attack/move once the match is over (K.O. or TIME OVER).
            playerInput.clear();
            opponentInput.clear();
        }
        // Com a cinemática esperando a vida baixa (ou rodando), ninguém cai antes da cena.
        // Depois do TIME OVER o resultado já está decidido: ninguém cai mais.
        protegerVida(cinematica.protegeVida() || flow.timeOver());
        engine.step(playerInput, opponentInput);
        // O parceiro só entra depois do KO quando o raio do ultra já saiu da tela.
        boolean fightFree = ultraAttacker < 0 && !engine.ultraSequenceRunning();
        // Cena da história esperando a vida baixar (ou o K.O.), ou rodando: o relógio espera.
        flow.setClockHeld(cinematica.protegeVida() || cinematica.seguraFimDaLuta() || demo.active());
        // O relógio para no congelamento do Super/Ultra e enquanto a cena tem o controle.
        boolean clockRuns = fightFree && !engine.superFreezeActive() && !cinematica.controleTravado();
        flow.update(engine, fightFree, clockRuns);
        updateOpponentVisuals();
        // Fora da história, a tela de vitória entra quando o K.O. sai; a revanche espera ela.
        if (flow.showingResult() && flow.winner() == PLAYER && !historia.waitingFight() && !tutorial.active()) {
            tocarVitoria();
        }
        if (flow.restartDue() && !historia.waitingFight() && !tutorial.active()) restartBattle();
        demo.afterStep();
        // O "treino completo" espera a conversa do fim (ela anda sozinha, sem toque).
        tutorial.setHoldEnd(conversa.falando());
        tutorial.afterStep();
        // Quem assiste no fundo (Dafu e Takatoshi) reage ao que aconteceu no treino.
        if (stageScene != null) {
            for (String reaction : tutorial.reactions()) stageScene.react(reaction);
        }
        updateGritoDaTroca();
        updateConversa();
        updateNarracao();
        gravarTelao();
        updateCinematica(intro);
        // A lição do Ultra termina com o raio ainda no ar: o treino sai depois dele.
        if (tutorial.finished() && ultraAttacker < 0) finishTutorial();
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
        for (float[] p : engine.repelled) {
            // O projétil desfeito no repel (o S2 da p15) estoura vermelho onde a aura o pegou.
            impacts.spawn(ImpactEffects.GUARD, p[0], p[1], p[2] >= 0f ? -1 : 1, ImpactEffects.SPECIAL, 0xFFE0281E);
            beamShake = Math.max(beamShake, 6f);
        }
        for (float[] p : engine.landed) {
            // A pedra do p13 quebra no chão: poeira cinza e um tranco.
            impacts.spawn(ImpactEffects.GUARD, p[0], Arena.GROUND_Y - 20f, p[1] >= 0f ? 1 : -1, ImpactEffects.SPECIAL, 0xFF8A8F98);
            beamShake = Math.max(beamShake, 5f);
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
        efeitos.passo();
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
        poeiraDoMotor(spriteFighterRenderer, player(), tagOffset());
        poeiraDoMotor(opponentSpriteRenderer, opponent(), engine.teams.tagOffset(OPPONENT));
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
        atlases.preload(keep.toArray(new CharacterDefinition[0]));
    }

    /** Modo batalha: depois do resultado, uma luta nova com as mesmas duplas. */
    private void restartBattle() {
        selectTeams(team[0].character.id, team[1].character.id, opponentFighter.character.id, opponentTagId);
    }

    /** Starts the match intro, as long as the longest INTRO clip of the two fighters. */
    void startIntro() {
        telao.reset();
        hud.resetEffects();
        impacts.clear();
        riscos.clear();
        secretoStart = -1;
        efeitos.limpar();
        feel.clear();
        finalizacao.limpar();
        koVictim = null;
        telaVitoria.parar();
        vitoriaTocada = false;
        // Motor de animação: o round novo começa em repouso.
        if (spriteFighterRenderer != null) spriteFighterRenderer.animacao.zerar();
        if (opponentSpriteRenderer != null) opponentSpriteRenderer.animacao.zerar();
        float longest = 0f;
        for (CombatFighter f : new CombatFighter[] {player(), opponent()}) {
            CharacterDefinition.Animation clip = f.character().animations.get(SpriteStates.INTRO);
            if (clip != null) longest = Math.max(longest, clip.duration);
        }
        introTotalFrames = (int)Math.ceil(longest / FIXED_STEP);
        introFrame = introTotalFrames > 0 ? 0 : -1;
        flow.reset();
        if (introFrame < 0) flow.startCountdown();
        narradorLuta.reset();
        gritoDaTroca.reset();
        narradorPalco.reset();
        if (narracao != null) narracao.stop();
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
        @Override public int presentationEpoch() { return demo.demo() * 100 + demo.stepIndex(); }
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
        @Override public int demoPlaying() { return demo.demo(); }
        @Override public String demoTitle() { return demo.title(); }
        @Override public String demoCaption() { return demo.caption(); }
        @Override public boolean storyActive() { return historia.isActive() && !tutorial.active(); }
        @Override public boolean tutorialActive() { return tutorial.active(); }
        @Override public boolean hideTestButtons() { return testButtonsHidden(); }
        @Override public String selectButtonLabel() { return selectButtonLabel; }
        @Override public String animacaoLabel() { return AnimacaoLutador.rotulo(); }
        @Override public boolean animacaoLigada() { return AnimacaoLutador.AJUSTES.ligado; }
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
        // O Ultra com a câmera da luta (o Mugen Rendan da p15) não fecha nela na ativação.
        CharacterDefinition.UltraSequence own = p.state.profile.ultraSequence;
        float startupZoom = own != null && own.fightCamera ? 0f : ULTRA_STARTUP_ZOOM;
        if (phase == CombatFighter.ULTRA_STARTUP) {
            float t = clamp(p.ultraFrame / (float)engine.config.ultraStartupFrames, 0f, 1f);
            ultraDarkAlpha = Math.round(205f * Math.min(1f, t * 3f));
            ultraCameraZoom = 1f + startupZoom * (1f - (1f - t) * (1f - t));
            ultraAuraTime = p.ultraFrame * FIXED_STEP;
        } else if (phase == CombatFighter.ULTRA_RUSH) {
            float t = clamp(p.ultraFrame / (float)engine.config.ultraRushFrames, 0f, 1f);
            ultraDarkAlpha = Math.round(205f * (1f - t));
            ultraCameraZoom = 1f + startupZoom * (1f - t);
        } else if (phase == CombatFighter.ULTRA_SEQUENCE) {
            // Sequência do vídeo: um plano de câmera por golpe (UltraDirector), corte seco a cada
            // acerto, fundo escuro; depois do golpe final a câmera da luta volta.
            CharacterDefinition.UltraSequence seq = p.state.profile.ultraSequence;
            if (seq != null) {
                CombatFighter v = engine.fighter(1 - p.index);
                UltraDirector.shot(ultraShot, seq.hits, p.ultraFrame, p.x, chestY(p), v.x, chestY(v), p.facing,
                    ultraScript);
                ultraShotActive = ultraShot.kind != UltraDirector.FINAL && !seq.fightCamera;
                // O plano fixo segura até a explosão do golpe final passar, e só então a câmera da luta volta.
                if (seq.camera != null && p.ultraFrame < seq.hits[seq.hits.length - 1] + ULTRA_FIXED_CAMERA_HOLD) {
                    ultraShotActive = true;
                    // Planos fixos do Ultra (a p16): enquadrados dentro do vídeo, a borda da folha (a
                    // cartola subindo) fica fora da tela; o do canhão abre para o tiro. O zoom é o da
                    // tela, não sobre o da luta.
                    float[] shot = seq.shotAt(p.ultraFrame);
                    ultraShot.focusX = p.x + p.facing * shot[1];
                    ultraShot.focusY = GROUND_Y - shot[2];
                    ultraShot.anchorX = 0.5f;
                    ultraShot.anchorY = 0.5f;
                    ultraShot.tilt = 0f;
                    ultraShot.zoom = shot[3] / Math.max(0.01f, camera.zoom);
                }
                if (seq.blast != null) {
                    // A explosão do canhão sai uma vez, quando a sequência passa do quadro dela.
                    if (p.ultraFrame < seq.blast[0]) {
                        ultraBlastArmed = true;
                    } else if (ultraBlastArmed) {
                        ultraBlastArmed = false;
                        ultraBlastStart = engine.frame();
                        ultraBlastX = p.x + p.facing * seq.blast[1];
                        ultraBlastY = GROUND_Y - seq.blast[2];
                        ultraBlastAngle = seq.blast[3];
                        ultraBlastDirection = p.facing;
                    }
                }
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
     * dims in {@link #drawFighter}) and the charge aura grows behind whoever activated it. With a
     * Super entrance ({@link EntradaSuper}) its portrait shows first, behind the fighters.
     */
    private void drawSuperDarkness(Canvas canvas, float zoom, float left, float top) {
        effects.drawWorldOverlay(canvas, paint, superFlash.darkAlpha, 0, 0, 8);
        CombatFighter u = engine.fighter(superFlash.user);
        if (superFlash.introShowing()) {
            // Entrada do Super: o quadro de mangá com o retrato, na tela (não no mundo), do lado dele.
            canvas.save();
            canvas.translate(left, top);
            canvas.scale(1f / zoom, 1f / zoom);
            entradaSuper.desenhar(canvas, u.character().id, superFlash.introAge, superFlash.introLeft,
                u.state.profile.color);
            canvas.restore();
        }
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
        if (!efeitoDeGolpe(event.blocked, x, y, direction, size)) {
            impacts.spawn(event.blocked ? ImpactEffects.GUARD : ImpactEffects.HIT, x, y, direction, size, accent);
        }
        // No escuro do Ultra (o sumiço do Mugen Rendan da p15): cada golpe é um clarão, como o secreto do Akuma.
        if (!event.blocked && ultraBlackout() > 0) {
            secretoStart = engine.frame();
            secretoX = x;
            secretoY = y;
            secretoVictim = event.defender;
            secretoSeed++;
        }
        // Golpe forte (H, especiais, Super, Ultra): os riscos de mangá no corpo e no rastro do golpe.
        if (!event.blocked && size >= ImpactEffects.HEAVY) {
            riscos.spawn(engine.frame(), event.defender, x, y, direction, size == ImpactEffects.HEAVY ? 1f : 1.25f,
                defender.y);
        }
        // O tremor do golpe tem direção: a tela vai no sentido do golpe e volta (o aleatório fica menor).
        // Só nos normais (o especial de vários acertos balançaria a tela a cada um) e no KO.
        boolean normal = !event.projectile && !ultraHit && (event.strength == AttackDefinition.Strength.LIGHT ||
            event.strength == AttackDefinition.Strength.MEDIUM || event.strength == AttackDefinition.Strength.HEAVY);
        if (!event.blocked && (normal || knockout)) feel.kick(direction, size);
        if (!event.blocked && size >= ImpactEffects.HEAVY) beamShake = Math.max(beamShake, size == ImpactEffects.HEAVY ? 4f : 6f);
        else if (!event.blocked && size == ImpactEffects.MEDIUM) beamShake = Math.max(beamShake, 2f);
        if (knockout) beamShake = Math.max(beamShake, 18f);
        if (!event.blocked) feel.onHit(x, y, size, punish && size >= ImpactEffects.HEAVY, knockout);
        // A plateia do cenário vibra com o golpe (forte no pesado, festa no KO).
        if (!event.blocked && stageScene != null) {
            stageScene.cheer(knockout ? 1f : size >= ImpactEffects.HEAVY ? 0.65f : size == ImpactEffects.MEDIUM ? 0.35f : 0.15f);
        }
        if (!event.blocked && pausa.impacto()) {
            // O golpe que segura cada pancada (o Super da p16) ganha um impact frame em todas.
            boolean everyStrike = !event.projectile && attacker.attacking() && attacker.attack.strikeStop > 0;
            feel.impactFrame(impactStyleFor(event, defender, size, punish, knockout), x, y, everyStrike,
                event.finalStrike);
        }
        // A pancada que fecha o golpe (o chute do Super da p15) treme a tela como um KO.
        if (!event.blocked && event.finalStrike) beamShake = Math.max(beamShake, 14f);
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

    /** Quadro do motor em que o "!" do combo breaker apareceu (o estalo de entrada), ou -1. */
    private int breakerCueStart = -1;
    private final Path breakerCuePath = new Path();
    private static final int BREAKER_CUE_POP_FRAMES = 6;
    /** Tamanho do "!" (o desenho base tem ~50 px de altura). */
    private static final float BREAKER_CUE_SCALE = 1.5f;
    private static final int BREAKER_CUE_INK = 0xFF111114;
    private static final int BREAKER_CUE_HOT = 0xFFE0262B;

    /**
     * O aviso do combo breaker, em estilo mangá: um "!" de tinta sobre a cabeça do jogador quando
     * o golpe que fecha o combo dele está vindo e dá para quebrar (M + H, com barra). Entra com um
     * estalo; na janela do breaker fica vermelho e treme. O contorno branco fino o separa do cenário.
     */
    private void drawBreakerCue(Canvas c) {
        CombatFighter f = player();
        int until = engine.breakerCue(f);
        if (until < 0) {
            breakerCueStart = -1;
            return;
        }
        int now = engine.frame();
        if (breakerCueStart < 0) breakerCueStart = now;
        int age = now - breakerCueStart;
        boolean window = until <= engine.config.comboBreakerFrames;
        float t = Math.min(1f, age / (float)BREAKER_CUE_POP_FRAMES);
        float scale = BREAKER_CUE_SCALE * (1.45f - 0.45f * (1f - (1f - t) * (1f - t)));
        float shake = window ? ((now & 1) == 0 ? 2.5f : -2.5f) : 0f;
        float cx = f.x + tagOffset() - f.facing * 18f + shake;
        float cy = visualTop(f) - 52f;

        c.save();
        c.translate(cx, cy);
        c.scale(scale, scale);
        // O "!": cunha grossa inclinada e o ponto.
        c.skew(-0.18f, 0f);
        breakerCuePath.reset();
        breakerCuePath.moveTo(-8f, -25f);
        breakerCuePath.lineTo(8f, -25f);
        breakerCuePath.lineTo(3f, 7f);
        breakerCuePath.lineTo(-3f, 7f);
        breakerCuePath.close();
        breakerCuePath.addCircle(0f, 17f, 5.5f, Path.Direction.CW);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(4f);
        paint.setColor(Color.WHITE);
        c.drawPath(breakerCuePath, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(window ? BREAKER_CUE_HOT : BREAKER_CUE_INK);
        c.drawPath(breakerCuePath, paint);
        paint.setStrokeJoin(Paint.Join.MITER);
        c.restore();
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
                float dx = f.x + (i == PLAYER ? tagOffset() : engine.teams.tagOffset(OPPONENT));
                float forca = wasAirborne[i] ? 1f : 0.45f;
                if (!efeitos.tocar("impacto_chao", dx, GROUND_Y, 0.7f + 0.3f * forca, false)) {
                    impacts.spawnDust(dx, GROUND_Y, forca);
                }
            }
            downSeen[i] = down;
            efeitosDeMovimento(i, f);
            wasAirborne[i] = !f.grounded;
            if (f.superJumping && !superJumpSeen[i]) {
                // Saída do super pulo: anel de choque e poeira nos pés, na cor do lutador.
                float sx = f.x + (i == PLAYER ? tagOffset() : engine.teams.tagOffset(OPPONENT));
                if (!efeitos.tocar("super_pulo", sx, GROUND_Y, 1f, false)) {
                    impacts.spawnSuperJump(sx, GROUND_Y, f.state.profile.color);
                }
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
            case "REPEL": return "REPEL!";
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
        // A câmera se adianta um pouco para o lado de quem corre (volta ao parar).
        boolean fighting = !player().ko() && !opponent().ko();
        camera.updateLead(fighting ? runDirection(player()) + runDirection(opponent()) : 0);
    }

    private static int runDirection(CombatFighter f) {
        return f.forwardDashing && f.grounded ? f.facing : 0;
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
        if (demo.active() || tutorial.active()) return;
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
        if (characterSelectRenderer == null) return;
        for (CharacterDefinition c : lutadores) characterSelectRenderer.hudPortrait(c);
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
            if (menuArcade.isOpen()) {
                renderCanvas.begin(canvas);
                menuArcade.render(renderCanvas);
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
            if (telaVitoria.cobreTudo()) {
                // A luta parada fica inteira atrás do fundo da tela de vitória.
                telaVitoria.desenhar(canvas);
                canvas.restore();
                return;
            }

            float cinematicZoom = cinematica.zoom();
            float baseZoom = camera.zoom * superFlash.zoom * cinematicZoom;
            float punchZoom = feel.zoom();
            float renderZoom = baseZoom * ultraCameraZoom * punchZoom;
            float visibleWorldWidth = VW / renderZoom;
            float renderCameraX = camera.x + camera.lead();
            float renderCameraTop = camera.top;
            // Zoom e foco da cinemática. Sem foco, o zoom segura o chão no mesmo lugar da tela (os
            // pés não somem na faixa); focando alguém (quem fala), segura o peito dele: um plano
            // médio, com o rosto na tela e os pés podendo sair. O centro vai até ele.
            float focoJogador = cinematica.foco(CinematicaLuta.JOGADOR);
            float focoRival = cinematica.foco(CinematicaLuta.RIVAL);
            if (cinematicZoom != 1f || focoJogador > 0f || focoRival > 0f) {
                float pivotY = GROUND_Y + focoJogador * (chestY(player()) - GROUND_Y) +
                    focoRival * (chestY(opponent()) - GROUND_Y);
                float pivotScreenY = (pivotY - camera.top) * camera.zoom * superFlash.zoom;
                renderCameraTop = pivotY - pivotScreenY / baseZoom;
                renderCameraX += focoJogador * (player().x + tagOffset() - camera.x) +
                    focoRival * (opponent().x + engine.teams.tagOffset(OPPONENT) - camera.x);
            }
            if (superFlash.active() && superFlash.zoom > 1f) {
                // Super: o zoom vai para quem ativou. O peito dele sai de onde estava na tela e
                // chega perto do meio conforme o zoom cresce (e volta junto quando ele solta).
                CombatFighter u = engine.fighter(superFlash.user);
                float ux = u.x + (u.index == PLAYER ? tagOffset() : engine.teams.tagOffset(OPPONENT));
                float uy = chestY(u);
                float plainZoom = camera.zoom * cinematicZoom;
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
            // O tranco do golpe forte: a câmera vai no sentido do golpe e volta por mola.
            cameraLeft -= feel.kickX() / renderZoom;
            if (beamShake > 0f) {
                cameraLeft += (shakeRandom.nextFloat() - 0.5f) * beamShake / renderZoom;
                renderCameraTop += (shakeRandom.nextFloat() - 0.5f) * beamShake / renderZoom;
            }
            boolean beam = ultraBeamActive();
            cameraDaLuta = !ultraShotActive && renderZoom == camera.zoom && focoJogador <= 0f && focoRival <= 0f;

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
                stageScene.setTelao(desenhaTelao);
                telaoCanvas = canvas;
                if (narradorNoPalco) {
                    stageScene.drawBackground(renderCanvas, com.gamelutagpt.historia.NarradorPalco.PROFUNDIDADE,
                        desenhaNarradorPalco);
                } else {
                    stageScene.drawBackground(renderCanvas);
                }
            }

            canvas.save();
            canvas.scale(renderZoom, renderZoom);
            canvas.translate(-cameraLeft, -renderCameraTop);
            if (stageScene == null) {
                drawScenario(canvas);
                if (narradorNoPalco) {
                    // Sem o palco 3D: um pouco acima do chão dos lutadores e menor.
                    if (cameraDaLuta) {
                        narradorPalco.alvo(player().x + tagOffset(), opponent().x + engine.teams.tagOffset(OPPONENT),
                            cameraLeft, cameraLeft + visibleWorldWidth);
                    }
                    narradorPalco.render(renderCanvas, narradorPalco.x(), GROUND_Y - 28f, 0.75f);
                }
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
            if (isSuperCinematicActive()) drawSuperDarkness(canvas, renderZoom, cameraLeft, renderCameraTop);
            // As sombras dos dois antes de qualquer corpo (uma não pinta o pé do outro).
            canvas.save();
            canvas.translate(tagOffset(), 0f);
            drawContactShadow(canvas, spriteFighterRenderer, player());
            canvas.restore();
            drawDamageDummy(canvas);
            float effectTime = engine.frame() * FIXED_STEP;
            effects.drawPuddles(renderCanvas, engine.puddles, effectTime);
            effects.drawTraps(canvas, engine.traps);
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
            for (int i = 0; i < 2; i++) {
                riscosLutadores[i] = engine.fighter(i);
                riscosVisiveis[i] = !hiddenByUltra(riscosLutadores[i]);
            }
            riscosDeslocamento[PLAYER] = tagOffset();
            riscosDeslocamento[OPPONENT] = engine.teams.tagOffset(OPPONENT);
            riscos.draw(canvas, engine.frame(), riscosLutadores, riscosDeslocamento, riscosVisiveis);
            impacts.draw(canvas);
            if (ultraBlastStart >= 0) {
                int age = engine.frame() - ultraBlastStart;
                if (age >= 0 && age < EffectsRenderer.CANNON_BLAST_FRAMES) {
                    effects.drawCannonBlast(canvas, ultraBlastX, ultraBlastY, ultraBlastAngle, ultraBlastDirection, age);
                } else {
                    ultraBlastStart = -1;   // acabou (ou a luta recomeçou o relógio)
                }
            }
            drawBreakerCue(canvas);
            efeitos.desenhar(canvas);
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
            int blackout = ultraBlackout();
            if (blackout > 0) {
                // Tela preta do Ultra (a p16: da cartola para o canhão).
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(blackout << 24);
                canvas.drawRect(-40f, -40f, VW + 40f, VH + 40f, paint);
                drawSecreto(canvas, renderZoom, cameraLeft, renderCameraTop);
            }
            if (ultraBars < 0.001f && cinematica.faixas() < 0.001f && !isSuperCinematicActive()) {
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
                if (narracao != null && faixas < 0.5f) narracao.render(renderCanvas);
                cinematica.renderEfeitos(renderCanvas);
            }
            telaVitoria.desenhar(canvas);
            if (abertura.isActive()) abertura.render(renderCanvas);
            if (paginaFinal.isActive()) {
                paginaFinal.render(renderCanvas);
            }
            // A pausa por cima da luta parada.
            pausa.render(renderCanvas);

            canvas.restore();
            characterSelectRenderer.drawFightTransition(canvas);
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
        hud.drawHud(c, hudState, characterSelectRenderer);
        if (throwBannerFrames > 0) {
            float t = 1f - throwBannerFrames / (float)THROW_BANNER_FRAMES;
            hud.drawBanner(c, throwBanner, Math.round(255f * clamp((1f - t) * 3f, 0f, 1f)), 1f + 0.4f * Math.max(0f, 1f - t * 6f));
        }
        String call = flow.banner();
        // Com a tela de vitória entrando, o "VITÓRIA" é o dela.
        if (call != null && !telaVitoria.ativa()) {
            float t = flow.bannerProgress();
            hud.drawBanner(c, call, Math.round(255f * clamp((1f - t) * 5f, 0f, 1f)), 1f + 0.4f * Math.max(0f, 1f - t * 6f));
        }
        hud.drawCombo(c, engine.session(OPPONENT), engine.lastSession(OPPONENT),
            engine.framesSinceSessionEnd(OPPONENT), true);
        hud.drawCombo(c, engine.session(PLAYER), engine.lastSession(PLAYER),
            engine.framesSinceSessionEnd(PLAYER), false);
        hud.drawTutorial(c, tutorial, esquema);
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
        drawContactShadow(c, opponentSpriteRenderer, npc);

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
            // Agarrão de especial que não pegou (o S3 do p13): a pegada no vazio, sem as cabeçadas,
            // no tempo do golpe (a recuperação longa é a punição).
            CharacterDefinition.Animation miss = f.grabMissed && f.move != null && f.move.animation != null
                ? character.animations.get(f.move.animation.id + "_ERRO") : null;
            if (miss != null) {
                animation = miss.id;
                animationElapsed = miss.timeFor(elapsed, total);
            } else if (f.move != null) {
                if (f.move.animation != null) {
                    animation = f.move.animation.id;
                    // Recuperação cortada no contato: volta seco à guarda até o corte. Sem corte
                    // (errou, defendido, derrubou) a animação vai inteira.
                    animationElapsed = ImpactFeel.snapBackTime(f.move, f.attackFrame,
                        f.hitCutFrame >= 0, FIXED_STEP);
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

        // Parado e cansado (pouca vida), o idle anda mais devagar (motor de animação).
        float clipStep = FIXED_STEP;
        if (animation == null && f.grounded && f.status == CombatFighter.Status.NEUTRAL && Math.abs(f.travel) < 0.001f) {
            clipStep *= renderer.animacao.pose().ritmo;
        }
        renderer.update(
            clipStep,
            f.grounded,
            // Na preparação do pulo ele agacha (o começo do clipe de agachar).
            f.crouchingBody() || guard == CombatFighter.GUARD_LOW || f.jumpSquat > 0,
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
        renderer.animacao.passo(f, renderer.motion, engine, fighterVisualY(f));
    }

    /** Força da poeira do motor a partir da qual a aterrissagem levanta a nuvem inteira (personagem pesado). */
    private static final float POEIRA_NUVEM = 0.5f;

    /**
     * Partículas que o motor de animação pede no passo: a poeira da aterrissagem e o pé
     * deslizando na largada e na freada do dash (as mesmas do combo breaker).
     */
    private void poeiraDoMotor(SpriteFighterRenderer renderer, CombatFighter f, float offset) {
        Pose pose = renderer.animacao.pose();
        float x = f.x + offset;
        // Efeitos de vídeo do pouso e do pé no chão, quando existem.
        if (pose.poeira > 0f && efeitos.tocar("fumaca_pouso", x, GROUND_Y, 0.6f + 0.6f * pose.poeira, false)) {
            pose.poeira = 0f;
        }
        if (pose.deslize != 0) {
            int d = pose.deslize;
            // O vídeo é de quem olha (e corre) para a direita: a poeira sai para trás, à esquerda.
            boolean largada = f.forwardDashing || f.backdashFrames > 0;
            String nome = largada ? "poeira_corrida" : "poeira_freada";
            if (efeitos.tocar(nome, x, GROUND_Y, 1f, d < 0)) pose.deslize = 0;
        }
        if (pose.poeira >= POEIRA_NUVEM) {
            impacts.spawnDust(x, GROUND_Y, pose.poeira);
        } else if (pose.poeira > 0f) {
            // Aterrissagem comum: só um sopro de cada lado dos pés (a nuvem é da queda pesada).
            impacts.spawnSkid(x - 22f, GROUND_Y, 1);
            impacts.spawnSkid(x + 22f, GROUND_Y, -1);
        }
        if (pose.deslize != 0) {
            int d = pose.deslize;
            impacts.spawnSkid(x + d * 16f, GROUND_Y, d);
            impacts.spawnSkid(x - d * 18f, GROUND_Y, d);
        }
    }

    /**
     * Golpe que acertou (ou defendido) com o efeito de vídeo: o do tamanho dele, ou o do tamanho
     * de baixo maior. O vídeo é de um golpe para a direita (espelha para a esquerda). False:
     * não há o efeito, e o clássico entra.
     */
    private boolean efeitoDeGolpe(boolean defendido, float x, float y, int direction, int size) {
        boolean esquerda = direction < 0;
        if (defendido) return efeitos.tocar("defesa", x, y, 1f, esquerda);
        for (int s = Math.min(size, EFEITO_GOLPE.length - 1); s >= 0; s--) {
            if (efeitos.tocar(EFEITO_GOLPE[s], x, y, 1f + 0.25f * (size - s), esquerda)) return true;
        }
        return false;
    }

    /**
     * Efeitos de vídeo do movimento (só quando o efeito existe): fumaça na decolagem e o arco do
     * golpe forte quando a janela ativa abre.
     */
    private void efeitosDeMovimento(int i, CombatFighter f) {
        float x = f.x + (i == PLAYER ? tagOffset() : engine.teams.tagOffset(OPPONENT));
        if (!wasAirborne[i] && !f.grounded && f.vy < 0f && !f.superJumping) {
            efeitos.tocar("fumaca_pulo", x, GROUND_Y, 1f, false);
        }
        AttackDefinition a = f.attacking() ? f.attack : null;
        boolean ativo = a != null && a.melee() && a.isActive(f.attackFrame);
        if (ativo && !ativoAntes[i] && a.strength != AttackDefinition.Strength.LIGHT &&
            a.strength != AttackDefinition.Strength.MEDIUM) {
            // A origem do arco é o ombro (o vídeo: um golpe de baixo para cima, para a direita).
            efeitos.tocar("arco_golpe", x + f.facing * 12f, f.y - f.character().visualStandHeight * 0.6f, 1f, f.facing < 0);
        }
        ativoAntes[i] = ativo;
    }

    /** Altura em que o lutador é desenhado (o pulinho do backdash sobe só o desenho). */
    private float fighterVisualY(CombatFighter f) {
        float visualY = f.y;
        if (f.grounded && f.status == CombatFighter.Status.NEUTRAL) {
            visualY -= engine.config.backdashVisualLift(f.backdashFrames, engine.backdashFrames(f));
        }
        if (f.attacking() && f.grounded) visualY -= f.attack.mechanics.hopLift(f.attackFrame);
        return visualY;
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

    /** Rival que o Ultra em sequência esconde agora (debaixo da cartola, dentro do canhão da p16). */
    private boolean hiddenByUltra(CombatFighter f) {
        for (int i = 0; i < 2; i++) {
            CombatFighter u = engine.fighter(i);
            if (!u.inUltraSequence() || engine.fighter(1 - i) != f) continue;
            CharacterDefinition.UltraSequence seq = u.state.profile.ultraSequence;
            if (seq != null && seq.hidesVictim(u.ultraFrame)) return true;
        }
        return false;
    }

    /** Quadros do zanzoken: a imagem que fica some nesse tanto; o reaparecer pisca nesse tanto. */
    private static final int ZANZOKEN_FADE = 16, ZANZOKEN_ARRIVE = 8;

    /**
     * Opacidade de quem está no Ultra com zanzoken (o Mugen Rendan da p15): 1 visível, 0 sumida.
     * Ao sumir a imagem fica onde ela estava, piscando e apagando; ao reaparecer ela pisca.
     */
    private float zanzokenAlpha(CombatFighter f) {
        if (!f.inUltraSequence()) return 1f;
        CharacterDefinition.UltraSequence seq = f.state.profile.ultraSequence;
        if (seq == null || seq.vanish.length == 0) return 1f;
        float frame = f.ultraFrame;
        boolean blink = (engine.frame() / 2) % 2 == 0;
        if (seq.hidesAttacker(frame)) {
            float since = seq.sinceVanish(frame);
            if (since >= ZANZOKEN_FADE) return 0f;
            return (blink ? 0.8f : 0.35f) * (1f - since / ZANZOKEN_FADE);
        }
        float since = seq.sinceReappear(frame);
        if (since >= 0f && since < ZANZOKEN_ARRIVE) return blink ? 0.9f : 0.4f;
        return 1f;
    }

    /** O desenho dela clareado e translúcido (alfa de 0 a 1 em passos de 0,1), para o zanzoken. */
    private final ColorFilter[] zanzokenTints = zanzokenTints();

    private static ColorFilter[] zanzokenTints() {
        ColorFilter[] out = new ColorFilter[11];
        for (int i = 0; i < out.length; i++) {
            float a = i / 10f;
            out[i] = new ColorMatrixColorFilter(new float[] {
                0.85f, 0f, 0f, 0f, 30f,
                0f, 0.85f, 0f, 0f, 34f,
                0f, 0f, 0.85f, 0f, 46f,
                0f, 0f, 0f, a, 0f,
            });
        }
        return out;
    }

    /** Opacidade da tela preta do Ultra em sequência (a p16 entre a cartola e o canhão). */
    private int ultraBlackout() {
        for (int i = 0; i < 2; i++) {
            CombatFighter u = engine.fighter(i);
            CharacterDefinition.UltraSequence seq = u.state.profile.ultraSequence;
            if (u.inUltraSequence() && seq != null) return seq.blackoutAlpha(u.ultraFrame);
        }
        return 0;
    }

    private void drawFighter(Canvas c, SpriteFighterRenderer renderer, CombatFighter f,
                             boolean damageFlash, boolean guardFlash) {
        if (hiddenByUltra(f)) return;
        float zanzoken = zanzokenAlpha(f);
        if (zanzoken <= 0f) return;
        if (zanzoken < 1f && renderer.override() == null) {
            // Zanzoken: a imagem que fica piscando onde ela estava (ou chegando, no reaparecer).
            renderer.setOverride(zanzokenTints[Math.min(zanzokenTints.length - 1, Math.round(zanzoken * 10f))]);
            renderer.draw(c, f.x, fighterVisualY(f), f.facing, false, false);
            renderer.setOverride(null);
            return;
        }
        float angle = knockdownAngle(renderer, f);
        // Rastro do motor de animação: atrás do lutador, onde ele estava.
        renderer.drawRastro(c);
        c.save();
        // Quem apanhou treme durante o congelamento do acerto.
        float shake = ImpactFeel.hitShake(f, engine.frame());
        if (shake != 0f) c.translate(shake, 0f);
        if (angle != 0f) c.rotate(angle, f.x, GROUND_Y);
        if (f.spinning && !f.grounded) spinTransform(c, f);
        // Shared backward-hop arc, without changing ground hitboxes or jump inputs.
        float visualY = fighterVisualY(f);
        ColorFilter look = renderer.override() == null ? superLook(f) : null;
        if (look != null) renderer.setOverride(look);
        if (look == null && renderer.override() == null && ghostArmsActive(f)) drawGhostArms(c, renderer, f, visualY);
        renderer.draw(c, f.x, visualY, f.facing, damageFlash, guardFlash);
        if (look != null) renderer.setOverride(null);
        c.restore();
    }

    /** Cópias azuladas e translúcidas dos braços fantasmas, da mais forte à mais fraca; o alfa baixo vai a zero. */
    private final ColorFilter[] ghostTints = {ghostTint(0.95f), ghostTint(0.75f), ghostTint(0.55f)};

    private static ColorFilter ghostTint(float strength) {
        // As cores do próprio desenho, clareadas e puxadas para o azul; o borrão que o vídeo já
        // tem nos braços da rajada vira rastro de movimento (tingido chapado, virava um bloco).
        return new ColorMatrixColorFilter(new float[] {
            0.7f, 0f, 0f, 0f, 40f,
            0f, 0.75f, 0f, 0f, 55f,
            0f, 0f, 0.8f, 0f, 80f,
            0f, 0f, 0f, 1.3f * strength, -90f * strength,
        });
    }

    /**
     * Braços fantasmas (efeito "FANTASMAS", a rajada da p15): durante os socos, antes do chute
     * que fecha o golpe.
     */
    private boolean ghostArmsActive(CombatFighter f) {
        if (!f.attacking() || f.attack == null) return false;
        CharacterDefinition.Special special = f.character().specials.get(f.attack.id);
        if (special == null || !"FANTASMAS".equals(special.effect)) return false;
        int strikes = f.attack.hitboxCount();
        int end = strikes > 1 ? f.attack.hitbox(strikes - 1).firstFrame - 8 : f.attack.startupFrames + f.attack.activeFrames;
        return f.attackFrame >= f.attack.startupFrames - 2 && f.attackFrame < end;
    }

    /**
     * Cópias claras e translúcidas do desenho, atrás dela e recortadas na faixa dos braços (do
     * ombro à cintura), deslocadas para a frente e para cima e para baixo, trocando a cada 2
     * quadros: muitos braços socando ao mesmo tempo.
     */
    private void drawGhostArms(Canvas c, SpriteFighterRenderer renderer, CombatFighter f, float visualY) {
        // Atrás dela (o corpo esconde o tronco das cópias), só na faixa dos ombros à cintura: os
        // braços extras aparecem acima e abaixo do braço de verdade, sem cabeça nem pernas.
        float h = f.body().height(false);
        float back = f.x - f.facing * (f.body().halfWidth + 40f), reach = f.attack.reach + 80f;
        float front = f.x + f.facing * reach;
        float left = Math.min(back, front), right = Math.max(back, front);
        float top = visualY - h * 0.86f, bottom = visualY - h * 0.5f;
        int seed = engine.frame() / 2;
        for (int k = 0; k < ghostTints.length; k++) {
            // Um acima, um no meio e um abaixo do braço de verdade, com um pouco de tremor.
            float dx = f.facing * (4f + 10f * ghostNoise(seed, k));
            float dy = (k - 1) * 24f + (ghostNoise(seed, k + 5) - 0.5f) * 12f;
            c.save();
            c.clipRect(left, top, right, bottom);
            c.translate(dx, dy);
            renderer.setOverride(ghostTints[k]);
            renderer.draw(c, f.x, visualY, f.facing, false, false);
            c.restore();
        }
        renderer.setOverride(null);
    }

    private static float ghostNoise(int seed, int k) {
        int x = seed * 374761393 + k * 668265263;
        x = (x ^ (x >>> 13)) * 1274126177;
        return ((x ^ (x >>> 16)) & 0xFFFF) / 65535f;
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
    /** Clarão de cada golpe no escuro do Ultra (o sumiço da p15): quadros que ele dura. */
    private static final int SECRETO_FRAMES = 9;
    private int secretoStart = -1, secretoVictim, secretoSeed;
    private float secretoX, secretoY;

    /**
     * O secreto do Akuma no escuro: a cada golpe a tela pisca branca, o rival aparece em
     * silhueta branca, cortes brancos com borda vermelha atravessam o corpo e a estrela do
     * impacto estoura e encolhe. Por cima da tela preta.
     */
    private void drawSecreto(Canvas canvas, float zoom, float left, float top) {
        if (secretoStart < 0) return;
        int age = engine.frame() - secretoStart;
        if (age < 0 || age >= SECRETO_FRAMES) return;
        if (age < 2) canvas.drawColor(age == 0 ? 0xB4FFFFFF : 0x50FFFFFF);
        canvas.save();
        canvas.scale(zoom, zoom);
        canvas.translate(-left, -top);
        if (age < 5) {
            boolean mine = secretoVictim == PLAYER;
            SpriteFighterRenderer renderer = mine ? spriteFighterRenderer : opponentSpriteRenderer;
            canvas.save();
            canvas.translate(mine ? tagOffset() : engine.teams.tagOffset(OPPONENT), 0f);
            renderer.setOverride(whiteSilhouette);
            CombatFighter victim = mine ? player() : opponent();
            renderer.draw(canvas, victim.x, fighterVisualY(victim), victim.facing, false, false);
            renderer.setOverride(null);
            canvas.restore();
        }
        Random r = new Random(secretoSeed * 7919L);
        float t = age / (float)SECRETO_FRAMES;
        paint.setStyle(Paint.Style.FILL);
        for (int k = 0; k < 3; k++) {
            // Corte longo e fino cruzando o corpo, de borda vermelha.
            double a = Math.PI * (r.nextFloat() - 0.5f) * 0.9f;
            float len = 170f + 120f * r.nextFloat(), w = 9f * (1f - t);
            float cx = secretoX + (r.nextFloat() - 0.5f) * 50f, cy = secretoY + (r.nextFloat() - 0.5f) * 90f;
            float dx = (float)Math.cos(a) * len, dy = (float)Math.sin(a) * len;
            paint.setColor(0xFFE0141E);
            drawSlash(canvas, cx, cy, dx, dy, w + 5f);
            paint.setColor(Color.WHITE);
            drawSlash(canvas, cx, cy, dx, dy, w);
        }
        float size = 95f * (1f - t * 0.7f);
        paint.setColor(0xFFE0141E);
        drawStar(canvas, secretoX, secretoY, size);
        paint.setColor(Color.WHITE);
        drawStar(canvas, secretoX, secretoY, size * 0.6f);
        canvas.restore();
    }

    /** Losango fino de (cx - dx, cy - dy) a (cx + dx, cy + dy), largura w no meio. */
    private void drawSlash(Canvas canvas, float cx, float cy, float dx, float dy, float w) {
        float l = (float)Math.sqrt(dx * dx + dy * dy);
        if (l < 1f || w <= 0f) return;
        float px = -dy / l * w * 0.5f, py = dx / l * w * 0.5f;
        impactPath.reset();
        impactPath.moveTo(cx - dx, cy - dy);
        impactPath.lineTo(cx + px, cy + py);
        impactPath.lineTo(cx + dx, cy + dy);
        impactPath.lineTo(cx - px, cy - py);
        impactPath.close();
        canvas.drawPath(impactPath, paint);
    }

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

    /**
     * O passo para o telão (só nos cenários com telão): onde cada um em campo estava e qual
     * quadro mostrava, para a transmissão e o replay.
     */
    private void gravarTelao() {
        if (stageScene == null || !stageScene.temTelao()) return;
        CombatFighter p = player(), o = opponent();
        gravarTelao(0, spriteFighterRenderer, p, p.x + tagOffset());
        gravarTelao(1, opponentSpriteRenderer, o, o.x + engine.teams.tagOffset(OPPONENT));
        CombatFighter a = engine.team(PLAYER).assist, b = engine.team(OPPONENT).assist;
        gravarTelao(2, a != null ? partnerSpriteRenderer : null, a, a != null ? a.x : 0f);
        gravarTelao(3, b != null ? opponentPartnerRenderer : null, b, b != null ? b.x : 0f);
        telao.passo(engine);
    }

    private void gravarTelao(int lugar, SpriteFighterRenderer r, CombatFighter f, float x) {
        if (r == null || f == null || hiddenByUltra(f) || zanzokenAlpha(f) <= 0f) {
            telao.gravar(lugar, null, null, 0, 0f, 0f, 1);
            return;
        }
        telao.gravar(lugar, r.character(), r.motion.clip, r.motion.frame(), x, fighterVisualY(f), f.facing);
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

    /** Altura (pixels do mundo) em que a sombra de contato chega ao menor e mais clara. */
    private static final float SHADOW_FADE_HEIGHT = 420f;

    /**
     * Sombra de contato no chão: encolhe e clareia com a altura (lê o pulo e o pouso) e abre
     * junto com o achatar da aterrissagem.
     */
    private void drawContactShadow(Canvas c, SpriteFighterRenderer renderer, CombatFighter f) {
        if (hiddenByUltra(f) || zanzokenAlpha(f) < 1f) return;
        float height = Math.max(0f, GROUND_Y - fighterVisualY(f));
        float k = clamp(1f - height / SHADOW_FADE_HEIGHT, 0.35f, 1f);
        float half = 43f * k * renderer.animacao.pose().escalaX;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(Math.round(70 * k), 0, 0, 0));
        c.drawOval(f.x - half, GROUND_Y - 10f * k, f.x + half, GROUND_Y + 10f * k, paint);
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
        if (menuArcade.isOpen()) {
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                applyArcadeChoice(menuArcade.tap(event.getX(index) / sx, event.getY(index) / sy));
            }
            return;
        }
        if (pausa.isOpen()) {
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                applyPause(pausa.tap(event.getX(index) / sx, event.getY(index) / sy));
            }
            return;
        }
        if (telaVitoria.ativa()) {
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) telaVitoria.pular();
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
                case ANIMACAO:
                    AnimacaoLutador.proximoNivel();
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
        // Fora do Arcade a CPU volta ao nível da pausa.
        arcadeMode = false;
        arcadeSelectPending = false;
        menuArcade.fechar();
        ai.setLevel(pausa.nivelCpu());
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
        episodioSorteio = episodioPorto = -1;
        for (int i = 0; i < temporada.episodios.size(); i++) {
            String id = temporada.episodios.get(i);
            try {
                Episodio ep = temporada.loadEpisodio(ultraAssets, id);
                lista.add(new MenuCapitulos.Capitulo(ep.numero, ep.titulo));
                for (Episodio.Passo passo : ep.passos) {
                    if (passo.tipo == Episodio.TipoPasso.SORTEIO && episodioSorteio < 0) episodioSorteio = i;
                    if (passo.porto && episodioPorto < 0) episodioPorto = i;
                }
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
            novoSorteio(escolha);
            startStoryAt(escolha);
        }
    }

    /** Episódios com o passo "sorteio" e com o desafio no porto (-1: não há), achados em {@link #openChapters}. */
    private int episodioSorteio = -1, episodioPorto = -1;

    /**
     * Jogar de novo um capítulo até o do sorteio sorteia outro torneio (o CONTINUAR mantém a
     * chave). Se o porto já passou, a chave nova mantém o adversário de lá.
     */
    private void novoSorteio(int episodio) {
        try {
            if (progresso.novoSorteio(episodio, episodioSorteio, episodioPorto,
                    Torneio.duplasFromJson(ultraAssets.readText(Torneio.ARQUIVO)), new Random())) {
                saveProgresso();
            }
        } catch (IOException | RuntimeException ex) {
            Log.w(LOG_TAG, "novo sorteio indisponível", ex);
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
        characterSelect.selectStage(testStage);
        testFightPending = true;
    }

    /** Pausa (MENU ou voltar) por cima da página ou da luta. Não abre no meio de um ultra. */
    private void openPause() {
        if (demo.active() || paginaFinal.isActive() || abertura.isActive() || ultraAttacker >= 0) return;
        loadTemporada();
        // COMANDOS: os golpes de cada personagem, começando pelo do jogador.
        int[] atual = new int[1];
        pausa.comandos().setFichas(ListaComandos.fichas(selectionRoster(), player().character(), esquema, atual), atual[0]);
        // O aviso de progresso perdido só vale para um episódio do modo história ou um Arcade.
        if (arcadeMode && historia.isActive()) pausa.abrir(temporada, MenuPausa.AVISO_ARCADE);
        else pausa.abrir(temporada, storyMode && historia.isActive());
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
        // O NÍVEL da CPU vale já na luta que continua, e fica salvo. Na história e no Arcade ele é
        // a base: a CPU sobe dele até o 8 na última luta.
        ai.setLevel(storyMode && historia.waitingFight() ? nivelDaLuta(historia.passo()) : pausa.nivelCpu());
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
        if (won && proximaOnda()) return;
        if (won && tocarVitoria()) return;
        storyKoFrames = 0;
        descarregarConversa();
        pad.reset();
        releaseAllPointers();
        if (won) registrarVencedor();
        historia.resultadoLuta(won);
    }

    /**
     * Começa a tela de vitória de quem venceu pelo jogador, uma vez por luta. Verdadeiro se ela
     * começou agora: a história (ou a revanche) espera ela acabar ({@link #depoisDaVitoria}).
     */
    private boolean tocarVitoria() {
        if (vitoriaTocada) return false;
        vitoriaTocada = true;
        FighterState winner = vencedorEmCampo();
        String id = winner.character.id;
        if (!telaVitoria.tem(id)) return false;
        String nome = temporada != null ? temporada.nomeDoLutador(id) : null;
        if (nome == null) nome = winner.character.displayName;
        telaVitoria.comecar(id, winner.profile.color, nome.toUpperCase(Locale.ROOT),
            texto -> temporada != null ? temporada.t(texto) : texto);
        return true;
    }

    /** A tela de vitória acabou: a história segue com o resultado, ou a luta recomeça. */
    private void depoisDaVitoria() {
        if (historia.waitingFight()) updateStoryFight();
        else if (!tutorial.active()) restartBattle();
    }

    /** O titular caído esperando a troca não venceu: quem está de pé leva o crédito. */
    private FighterState vencedorEmCampo() {
        FighterState point = engine.team(PLAYER).pointState();
        FighterState partner = engine.team(PLAYER).partner();
        return point.life <= 0 && partner != null && partner.life > 0 ? partner : point;
    }

    /**
     * A campanha guarda quem da dupla do jogador estava em campo na vitória: no Arcade, o duelo
     * 1x1 depois da final é com ele (contra o parceiro); na história é sempre com o líder.
     */
    private void registrarVencedor() {
        Campanha campanha = historia.campanha();
        if (campanha == null) return;
        campanha.venceuCom(vencedorEmCampo().character.id);
    }

    @Override
    public void onHistoriaLuta(Episodio.Passo luta) {
        // Uma luta nova (ou repetida depois da derrota) começa na primeira onda, de vida cheia.
        lutaComOndas = luta.ondas > 1 ? luta : null;
        ondaDaLuta = 0;
        vidaEntreOndas = superEntreOndas = null;
        if (historia.campanha() != null) historia.campanha().onda(0);
        // Cenário e duplas da luta carregam com a tela de carregamento; o resto vem depois deles.
        carregando(() -> startStoryFight(luta));
    }

    /**
     * A onda acabou com a vitória e ainda há outra: a mesma dupla do jogador, com a vida e o
     * Super de agora, contra os próximos agentes. Sem a tela de vitória entre as ondas.
     */
    private boolean proximaOnda() {
        Episodio.Passo luta = lutaComOndas;
        if (luta == null || ondaDaLuta + 1 >= luta.ondas) return false;
        vidaEntreOndas = new int[team.length];
        superEntreOndas = new int[team.length];
        for (int i = 0; i < team.length; i++) {
            vidaEntreOndas[i] = team[i].life;
            superEntreOndas[i] = team[i].superMeter;
        }
        ondaDaLuta++;
        if (historia.campanha() != null) historia.campanha().onda(ondaDaLuta);
        storyKoFrames = 0;
        descarregarConversa();
        pad.reset();
        releaseAllPointers();
        carregando(() -> startStoryFight(luta));
        return true;
    }

    /** A dupla do jogador começa a onda com a vida e o Super do fim da anterior. */
    private void aplicarVidaEntreOndas() {
        if (vidaEntreOndas == null) return;
        for (int i = 0; i < team.length && i < vidaEntreOndas.length; i++) {
            FighterState f = team[i];
            int folego = Math.round(f.profile.maxLife * FOLEGO_ENTRE_ONDAS);
            f.life = Math.min(f.profile.maxLife, Math.max(vidaEntreOndas[i], folego));
            f.recoverableLife = 0;
            f.superMeter = superEntreOndas[i];
            f.refreshHudLabels();
        }
        hud.resetEffects();
        engine.refreshOverdrive(PLAYER);
    }

    private void startStoryFight(Episodio.Passo luta) {
        // Cada luta pode pedir o seu cenário (o treino é no dojo). No Arcade, por enquanto,
        // a luta sem cenário próprio sorteia um.
        useStage(luta.cenario != null || !arcadeMode ? luta.cenario : sortearCenario());
        // Antes das duplas: luta de verdade (IA, 2x2) ou o treino, onde o boneco não usa a IA
        // e as lições dizem o que ele faz.
        setOpponentAiEnabled(!luta.tutorial());
        if (storyMode) ai.setLevel(nivelDaLuta(luta));
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
        aplicarVidaEntreOndas();
        String rotulo = historia.rotulo(luta);
        if (rotulo != null && temporada != null) rotulo = temporada.t(rotulo);
        if (lutaComOndas == luta && ondaDaLuta > 0) {
            // Da segunda onda em diante: "ONDA 2 / 3".
            rotulo = (temporada != null ? temporada.t(ONDA) : ONDA) + " " + (ondaDaLuta + 1) + " / " + luta.ondas;
        }
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

    // ------------------------------------------------------------ arcade

    /** ARCADE: a escolha da dupla (oficial ou livre); depois, a chave de 16 e as lutas. */
    private void openArcadeMenu() {
        if (!loadTemporada()) {
            openMainMenu();
            return;
        }
        storyMode = false;
        arcadeMode = false;
        arcadeSelectPending = false;
        arcadeOficiais.clear();
        try {
            for (Torneio.Dupla d : Arcade.oficiais(Torneio.duplasFromJson(ultraAssets.readText(Torneio.ARQUIVO)))) {
                if (fightersExist(d.lutadores)) arcadeOficiais.add(d);
            }
        } catch (IOException | RuntimeException ex) {
            Log.w(LOG_TAG, "torneio indisponível; o Arcade fica só com a dupla livre", ex);
        }
        menuArcade.abrir(temporada, arcadeOficiais);
        selectionKeys.clear();
        pad.reset();
        releaseAllPointers();
    }

    private void handleArcadeMenuKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT: menuArcade.mover(-1, 0); break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: menuArcade.mover(1, 0); break;
            case KeyEvent.KEYCODE_DPAD_UP: menuArcade.mover(0, -1); break;
            case KeyEvent.KEYCODE_DPAD_DOWN: menuArcade.mover(0, 1); break;
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_BUTTON_START:
                applyArcadeChoice(menuArcade.confirmar());
                break;
            case KeyEvent.KEYCODE_BUTTON_B:
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
                applyArcadeChoice(menuArcade.voltar());
                break;
            default:
                break;
        }
    }

    /** Dupla oficial: o Arcade começa; DUPLA LIVRE: a seleção só da dupla do jogador. */
    private void applyArcadeChoice(Integer escolha) {
        if (escolha == null) return;
        if (escolha == MenuArcade.VOLTAR) {
            openMainMenu();
        } else if (escolha == MenuArcade.LIVRE) {
            openCharacterSelect();
            characterSelect.openSolo(testPicks[0], testPicks[1]);
            arcadeSelectPending = true;
        } else {
            startArcade(arcadeOficiais.get(escolha));
        }
    }

    /**
     * O Arcade com a dupla escolhida: a chave de 16 (as oficiais que sobraram e duplas livres do
     * CPU) e o episódio do Arcade (sorteio, as 4 rodadas, o duelo e o Takatoshi), tocado como
     * as cenas da história. No fim volta ao menu inicial.
     */
    private void startArcade(Torneio.Dupla dupla) {
        Episodio arcade;
        try {
            if (!loadTemporada()) throw new IOException("sem temporada");
            arcade = Episodio.fromJson(ultraAssets.readText(Arcade.EPISODIO));
        } catch (IOException | RuntimeException ex) {
            Log.w(LOG_TAG, "Arcade indisponível", ex);
            openMainMenu();
            return;
        }
        Torneio chave = Arcade.chave(dupla, arcadeOficiais, java.util.Arrays.asList(characterSelect.ids), new Random());
        // Como o modo história: o botão do topo é o MENU (pausa) e não há botões de teste.
        storyMode = true;
        arcadeMode = true;
        selectButtonLabel = "◀ " + temporada.t(MenuPausa.MENU);
        debugOverlay = false;
        tutorial.stop();
        descarregarConversa();
        storyEpisode = -1;
        sceneAfter = this::openMainMenu;
        historia.setCampanha(Campanha.arcade(chave));
        historia.setBotaoMenu(true);
        hud.resetEffects();
        pad.reset();
        releaseAllPointers();
        selectionKeys.clear();
        historia.start(temporada, arcade);
    }

    /**
     * Nível da CPU na luta da história ou do Arcade: do NÍVEL CPU da pausa (padrão 5) na primeira
     * luta até o 8 na última, repartido pelas lutas da campanha ({@link NivelCpu}), menos 1 a
     * cada 2 derrotas seguidas. Fora delas (uma cena solta, um episódio que não abriu), o da pausa.
     */
    private int nivelDaLuta(Episodio.Passo luta) {
        int base = pausa.nivelCpu();
        Episodio ep = historia.episodio();
        if (luta == null || ep == null) return base;
        int indice = ep.passos.indexOf(luta);
        java.util.List<Episodio> campanha;
        String irmao = null;
        if (arcadeMode) {
            campanha = java.util.Collections.singletonList(ep);
        } else {
            if (storyEpisode < 0) return base;
            campanha = episodiosTemporada();
            if (historia.campanha() != null) irmao = historia.campanha().irmao();
        }
        int[] pos = campanha == null || indice < 0 ? null : NivelCpu.posicao(campanha, ep.id, indice, irmao);
        int alivio = historia.campanha() != null ? historia.campanha().alivio() : 0;
        int nivel = pos == null ? Math.max(MenuPausa.NIVEL_MIN, base - alivio) : NivelCpu.nivel(base, pos[0], pos[1], alivio);
        // Nas ondas (Sob a Mesma Lua), as primeiras são mais fáceis; a última é a da luta.
        return luta == lutaComOndas ? NivelCpu.onda(nivel, ondaDaLuta, luta.ondas) : nivel;
    }

    private java.util.List<Episodio> episodiosTemporada() {
        if (episodiosTemporada != null || temporada == null) return episodiosTemporada;
        java.util.List<Episodio> lista = new java.util.ArrayList<>();
        try {
            for (String id : temporada.episodios) lista.add(temporada.loadEpisodio(ultraAssets, id));
        } catch (IOException | RuntimeException ex) {
            Log.w(LOG_TAG, "temporada sem a lista de lutas; a CPU fica no nível da pausa", ex);
            return null;
        }
        episodiosTemporada = lista;
        return lista;
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

    private final GritoDaTroca gritoDaTroca = new GritoDaTroca();

    /**
     * Vida de quem está lutando entrou no vermelho: o parceiro que espera grita o nome dele e
     * "troca comigo!" no quadro da conversa do lado do time (não no tutorial nem na cena).
     */
    private void updateGritoDaTroca() {
        int gritos = gritoDaTroca.update(engine);
        if (gritos == 0 || tutorial.active() || cinematica.rodando() || demo.active() ||
            flow.phase() != BattleFlow.Phase.FIGHT) {
            return;
        }
        loadTemporada();
        for (int side = 0; side < 2; side++) {
            if ((gritos & (1 << side)) != 0) gritarTroca(side);
        }
    }

    private void gritarTroca(int side) {
        TeamSystem.Side team = engine.team(side);
        FighterState parceiro = team.partner();
        if (parceiro == null) return;
        String quem = falante(parceiro.character.id);
        String codigo = "player_base".equals(parceiro.character.id) ? "p01" : parceiro.character.id;
        conversa.avulso(quem, codigo.toUpperCase(Locale.ROOT), parceiro.profile.color,
            "selection/rostos/" + codigo + ".webp");
        String nome = temporada != null ? temporada.nomeDoLutador(team.pointState().character.id) : null;
        String texto = GritoDaTroca.texto(nome, t -> temporada != null ? temporada.t(t) : t);
        conversa.dizer(temporada, quem, side == OPPONENT, "bravo", texto, GritoDaTroca.SEGURA);
    }

    /** Quem fala na conversa por um lutador: o id do elenco da história (com os rostos dele) ou o próprio pack. */
    private String falante(String lutador) {
        if (temporada != null) {
            for (Temporada.Personagem p : temporada.elenco.values()) {
                if (lutador.equals(p.lutador)) return p.id;
            }
        }
        return lutador;
    }

    /**
     * O narrador: nas lutas de verdade olha o que aconteceu e pede a fala; a faixa anda
     * sempre (para sair da tela quando a luta deixa de ser narrada).
     */
    private void updateNarracao() {
        if (narracao == null) return;
        if (narracaoAtiva()) {
            if (!narracaoPronta) {
                narracaoPronta = true;
                if (!narracao.carregar()) Log.w(LOG_TAG, "narrador.json não abriu");
                if (loadTemporada()) {
                    narracao.setTemporada(temporada);
                    narradorLuta.setNomes(c -> {
                        String nome = temporada != null ? temporada.nomeDoLutador(c.id) : null;
                        return nome != null ? nome : c.displayName;
                    });
                }
            }
            narradorLuta.update(engine, flow, narracao::narrar);
        }
        narracao.update(FIXED_STEP);
        narradorNoPalco = narracaoAtiva();
        if (narradorNoPalco) {
            narradorPalco.update(FIXED_STEP, narracao.textoAtual(), narracao.expressaoAtual(),
                narracao.prioridadeAtual(), narracao.digitando());
        }
    }

    /**
     * O narrador no palco, entre o fundo e os lutadores (chamado pelo cenário na profundidade
     * dele). Escolhe o lado só com a câmera da luta: num close ele não sai correndo.
     */
    private void drawNarradorPalco() {
        float z = com.gamelutagpt.historia.NarradorPalco.PROFUNDIDADE;
        if (cameraDaLuta) {
            float a = stageScene.worldX(stageScene.screenX(player().x + tagOffset(), 0f), z);
            float b = stageScene.worldX(stageScene.screenX(opponent().x + engine.teams.tagOffset(OPPONENT), 0f), z);
            narradorPalco.alvo(a, b, stageScene.worldX(0f, z), stageScene.worldX(VW, z));
        }
        float x = narradorPalco.x();
        if (Float.isNaN(x)) return;
        narradorPalco.render(renderCanvas, stageScene.screenX(x, z), stageScene.groundScreenY(z), stageScene.scale(z));
    }

    /** Luta narrada: a batalha (IA ON), o Arcade e as lutas do torneio da história. */
    private boolean narracaoAtiva() {
        if (!flow.battle() || tutorial.active() || demo.active()) return false;
        if (arcadeMode || !storyMode) return true;
        Episodio.Passo passo = historia.waitingFight() ? historia.passo() : null;
        return passo != null && passo.torneio;
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
