package com.gamelutagpt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.gamelutagpt.stage.StagePack;
import com.gamelutagpt.stage.StageScene;
import com.gamelutagpt.stage.StageWorld;
import com.gamelutagpt.ultra.PaginaFinal;
import com.gamelutagpt.ultra.RaioFinal;
import com.gamelutagpt.ultra.UltraDefinition;
import com.gamelutagpt.ultra.UltraGrade;
import com.gamelutagpt.ultra.UltraListener;
import com.gamelutagpt.ultra.UltraPack;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Android shell of the match: touch → {@link PadInput}, CPU → {@link AiController}, a
 * fixed 60 Hz {@link CombatEngine} step, then rendering. Every combat rule lives in the
 * engine; this class only presents it (sprites, Super and ultra cinematics, tag, HUD, debug).
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable, UltraListener {
    // Team and opponent rules come from the generated character packs.
    private final FighterState[] team = new FighterState[] {
        new FighterState(GeneratedCharacters.get(GeneratedCharacters.TEAM[0]), "PLAYER 1"),
        new FighterState(GeneratedCharacters.get(GeneratedCharacters.TEAM[1]), "PLAYER 2")
    };

    private final FighterState opponentFighter =
        new FighterState(GeneratedCharacters.opponentCharacter(), "CPU");

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

    final CombatEngine engine = new CombatEngine(
        team[0], opponentFighter, PLAYER_START_X, OPPONENT_START_X, new CombatConfig());
    final PadInput pad = new PadInput();
    private final FighterInput playerInput = new FighterInput();
    private final FighterInput opponentInput = new FighterInput();
    private final AiController ai = new AiController(new OpponentAi(new Random()));
    private boolean debugOverlay;

    private String dummyDamageLabel = "";
    private int dummyDamageLabelFrames = 0;
    private static final int DAMAGE_LABEL_FRAMES = 43;

    private final SurfaceHolder holder;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SpriteFighterRenderer spriteFighterRenderer;
    private final SpriteFighterRenderer opponentSpriteRenderer;
    private final StageRenderer stage = new StageRenderer();
    private final EffectsRenderer effects = new EffectsRenderer();
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
    private float superCameraZoom = 1f;
    private int superDarkAlpha = 0;
    private int superFlashAlpha = 0;
    private float superChargeTime = 0f;

    // Ultra (↓ + SUPER): the engine runs activation, rush and hit confirm; when it connects
    // the "Página Final" cinematic (ultra-core) plays while the engine waits.
    private static final String LOG_TAG = "GameView";
    private static final float ULTRA_STARTUP_ZOOM = 0.30f;
    /** Point of the heavy straight (fraction of the move) where the arm is fully extended. */
    private static final float BEAM_POSE = 0.5625f;
    /** Point of the heavy straight where the arm is pulled back (charge, without own art). */
    private static final float BEAM_CHARGE_POSE = 0.25f;
    private static final float BEAM_CHARGE_ZOOM = 0.18f;
    private final UltraPack[] ultraPacks = new UltraPack[team.length];
    private final PaginaFinal paginaFinal = new PaginaFinal(this);
    /** Pincel do Canvas do quadro atual para os motores visuais (cenário e ultra). */
    private final AndroidRenderCanvas renderCanvas = new AndroidRenderCanvas();

    // Cenário com falso 3D (stage-core). Sem ele, o StageRenderer antigo desenha o fundo.
    private static final String STAGE_FOLDER = "stages/templo_lua";
    private final StageScene stageScene;
    /** Desenho pela GPU (Android 8+); cai para o Canvas por software se falhar. */
    private boolean hardwareCanvas = Build.VERSION.SDK_INT >= 26;
    private final UltraSounds ultraSounds;
    private int ultraAttacker = -1;
    private int ultraDamageDealt;
    private float ultraCameraZoom = 1f;
    private int ultraDarkAlpha;
    private float ultraAuraTime;
    private int ultraPhaseSeen = -1;
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
    private final java.util.Random shakeRandom = new java.util.Random(3);

    // Team HUD labels (the rules live in the engine's TeamSystem).
    private String tagCooldownHudLabel = "ASSIST: PRONTO";
    private String tagCooldownButtonLabel = "";
    private int shownAssistTenths = -2;
    private int shownTagSeconds = -2;
    private int shownPoint = 0;
    /** Partner on screen: the assist, or the former point running off after Assist → Tag. */
    private final SpriteFighterRenderer partnerSpriteRenderer;
    private final CombatFighter leavingBody = new CombatFighter(PLAYER, null, 0f, 1);

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
    private volatile boolean surfaceReady;
    private volatile boolean activityActive = true;
    private float accumulatedTime;

    private void clearInput() {
        clearPendingInput();
        pad.reset();
        dpadPointer = lightPointer = mediumPointer = heavyPointer = comboPointer = tagPointer = throwPointer = pushblockPointer = superPointer = -1;
        healPlayerPointer = healOpponentPointer = -1;
    }

    public GameView(Context context) {
        super(context);
        // One decode per atlas for the whole match: team packs (tag never decodes
        // mid-fight) and the opponent share the same cache.
        SpriteAtlasCache atlases = new SpriteAtlasCache(context);
        for (FighterState fighter : team) atlases.preload(fighter.character);
        spriteFighterRenderer = new SpriteFighterRenderer(atlases, team[0].character);
        opponentSpriteRenderer = new SpriteFighterRenderer(atlases, opponentFighter.character);
        partnerSpriteRenderer = new SpriteFighterRenderer(atlases, team[1].character);
        engine.setTeam(PLAYER, team);
        ultraSounds = new UltraSounds(context.getAssets());
        AndroidRenderAssets ultraAssets = new AndroidRenderAssets(context.getAssets());
        stageScene = loadStage(ultraAssets);
        for (int i = 0; i < team.length; i++) {
            ultraPacks[i] = loadUltraPack(ultraAssets, team[i]);
            ultraSounds.loadUltra(ultraFolder(team[i]));
        }
        holder = getHolder();
        holder.addCallback(this);
        setFocusable(true);
        setKeepScreenOn(true);
    }

    /** Each character's ultra lives in assets/ultras/&lt;character id&gt;/. */
    private static String ultraFolder(FighterState fighter) {
        return "ultras/" + fighter.character.id;
    }

    private static StageScene loadStage(AndroidRenderAssets assets) {
        try {
            StagePack pack = StagePack.load(assets, STAGE_FOLDER);
            for (String warning : pack.warnings) Log.w(LOG_TAG, STAGE_FOLDER + ": " + warning);
            StageWorld world = new StageWorld(GROUND_Y, WORLD_WIDTH, CameraRig.CAMERA_ZOOM,
                (PLAYER_START_X + OPPONENT_START_X) * 0.5f, CameraRig.CAMERA_GROUND_SCREEN_Y);
            return new StageScene(pack, world);
        } catch (IOException | RuntimeException ex) {
            Log.w(LOG_TAG, "cenário " + STAGE_FOLDER + " indisponível, usando o fundo simples", ex);
            return null;
        }
    }

    private static UltraPack loadUltraPack(AndroidRenderAssets assets, FighterState fighter) {
        try {
            UltraPack pack = UltraPack.load(assets, ultraFolder(fighter));
            for (String warning : pack.warnings) Log.w(LOG_TAG, ultraFolder(fighter) + ": " + warning);
            return pack;
        } catch (IOException | RuntimeException ex) {
            // Without a valid ultra.json the ultra still works, with the placeholder art.
            Log.w(LOG_TAG, "ultra de " + fighter.character.id + " usando arte provisória", ex);
            return UltraPack.placeholder("ULTRA " + fighter.character.displayName, fighter.profile.color);
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

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
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
        processPendingInput();
        accumulatedTime += clamp(elapsedSeconds, 0f, 0.10f);
        while (accumulatedTime + 0.000001f >= FIXED_STEP) {
            update();
            accumulatedTime = Math.max(0f, accumulatedTime - FIXED_STEP);
        }
    }

    /** One simulation frame: inputs, engine step, then presentation state. */
    private void update() {
        if (stageScene != null) stageScene.update(FIXED_STEP);
        if (paginaFinal.isActive()) {
            // The engine waits: the cinematic deals the damage and ends with the final beam.
            paginaFinal.update(FIXED_STEP);
            if (paginaFinal.isShattering() && ultraAttacker >= 0) {
                // Through the breaking page the fight is already in place for the beam.
                engine.placeForUltraBeam(ultraAttacker);
                updateFightCamera(FIXED_STEP);
            }
            return;
        }
        boolean tagging = isTagAnimationActive();

        pad.drainInto(playerInput);
        if (tagging) {
            opponentInput.clear();
        } else {
            ai.fill(engine, OPPONENT, opponentInput);
        }
        engine.step(playerInput, opponentInput);
        if (engine.ultraConnected() >= 0) beginUltraCinematic(engine.ultraConnected());

        for (CombatEngine.HitEvent event : engine.events()) {
            boolean ultraHit = ultraAttacker >= 0 && event.attacker == ultraAttacker && !event.blocked &&
                "ULTRA".equals(event.moveId);
            if (ultraHit) ultraDamageDealt += event.damage;
            if ("THROW".equals(event.moveId)) showBanner("AGARRÃO!");
            if (event.defender == OPPONENT && !event.blocked) {
                dummyDamageLabel = "-" + (ultraHit ? ultraDamageDealt : event.damage);
                dummyDamageLabelFrames = DAMAGE_LABEL_FRAMES;
            }
        }
        for (String cue : engine.cues()) {
            String banner = bannerFor(cue);
            if (banner != null) showBanner(banner);
            // Bounces shake the screen like the beam's hits.
            if ("WALL_BOUNCE".equals(cue) || "GROUND_BOUNCE".equals(cue)) beamShake = Math.max(beamShake, 14f);
        }
        updateUltraBeam();
        if (dummyDamageLabelFrames > 0) dummyDamageLabelFrames--;
        if (throwBannerFrames > 0) throwBannerFrames--;

        updateSuperVisuals();
        updateUltraVisuals();
        updateFightCamera(FIXED_STEP);
        updateTeamVisuals();
        updateFighterSprite(spriteFighterRenderer, player(), true);
        updateFighterSprite(opponentSpriteRenderer, opponent(), false);
    }

    private final HudRenderer.State hudState = new HudRenderer.State() {
        @Override public FighterState active() { return activeFighter(); }
        @Override public FighterState reserve() { return reserveFighter(); }
        @Override public String versionLabel() { return VERSION_HUD_LABEL; }
        @Override public String stateLabel() { return currentStateLabel(); }
        @Override public int facing() { return player().facing; }
        @Override public boolean aiEnabled() { return ai.enabled(); }
        @Override public boolean debugEnabled() { return debugOverlay; }
        @Override public float tagReadyRatio() {
            TeamSystem.Side team = engine.team(PLAYER);
            if (isTagAnimationActive() || team.assistOut()) return 0f;
            if (team.assistCooldown > 0) {
                return 1f - team.assistCooldown / (float)engine.config.assistCooldownFrames;
            }
            return 1f;
        }
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
                default: return false;
            }
        }
    };

    private CharacterDefinition opponentCharacter() {
        return opponentFighter.character;
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

    /** Activation and rush of the player's ultra (engine state), before any cinematic. */
    private boolean ultraWindupActive() {
        CombatFighter p = player();
        return p.status == CombatFighter.Status.ULTRA &&
            (p.ultraPhase == CombatFighter.ULTRA_STARTUP || p.ultraPhase == CombatFighter.ULTRA_RUSH);
    }

    private void updateUltraVisuals() {
        CombatFighter p = player();
        int phase = p.status == CombatFighter.Status.ULTRA ? p.ultraPhase : -1;
        if (phase != ultraPhaseSeen) {
            if (phase == CombatFighter.ULTRA_STARTUP) ultraSounds.play(ultraFolder(activeFighter()), "ativacao");
            if (phase == CombatFighter.ULTRA_RUSH) ultraSounds.play(ultraFolder(activeFighter()), "investida");
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

    private void beginUltraCinematic(int attacker) {
        ultraAttacker = attacker;
        ultraDamageDealt = 0;
        ultraDarkAlpha = 0;
        ultraCameraZoom = 1f;
        FighterState state = attacker == PLAYER ? activeFighter() : opponentFighter;
        ultraPack = attacker == PLAYER
            ? ultraPacks[engine.team(PLAYER).point]
            : UltraPack.placeholder("ULTRA " + state.character.displayName, state.profile.color);
        paginaFinal.start(ultraPack, engine.fighter(attacker).facing);
    }

    private String ultraAttackerFolder() {
        return ultraAttacker == PLAYER ? ultraFolder(activeFighter()) : ultraFolder(opponentFighter);
    }

    private int ultraTotalDamage() {
        return ultraPack != null ? ultraPack.definition.damage : UltraDefinition.DEFAULT_DAMAGE;
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
            updateFighterSprite(partnerSpriteRenderer, partner, false);
        }
        updateTagCooldownLabels(team);
    }

    /** The partner's body on screen (assist or the former point leaving), or null. */
    private CombatFighter partnerBody(TeamSystem.Side team) {
        if (team.assist != null) return team.assist;
        if (team.leaving == null || team.leavingFrame < 0) return null;
        CombatFighter f = leavingBody;
        float x = engine.teams.leavingX(PLAYER);
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
            : String.format(java.util.Locale.US, "ASSIST: %.1fs", assistTenths / 10f);
        String tag = tagSeconds == 0 ? "  •  \u2193+TAG: TROCA" : "  •  TROCA: " + tagSeconds + "s";
        tagCooldownHudLabel = assist + tag;
        tagCooldownButtonLabel = assistTenths > 0 ? Integer.toString((assistTenths + 9) / 10) : "";
    }

    private void setOpponentAiEnabled(boolean enabled) {
        ai.setEnabled(enabled);
    }

    private void updateFightCamera(float dt) {
        // Frame what is actually drawn: opaque sprite height measured at import time.
        float highestFighterTop = Math.min(visualTop(player()), visualTop(opponent()));
        camera.update(dt, player().x, opponent().x, highestFighterTop,
            player().superJumping || opponent().superJumping);
    }

    /** Highest opaque pixel of a fighter, for camera framing and the opponent HUD. */
    private static float visualTop(CombatFighter f) {
        CharacterDefinition c = f.character();
        return f.y - (f.crouchingBody() ? c.visualCrouchHeight : c.visualStandHeight);
    }

    private float opponentVisualTop() {
        return visualTop(opponent());
    }

    private void drawFrame() {
        if (!holder.getSurface().isValid()) return;
        Canvas canvas = lockFrameCanvas();
        if (canvas == null) return;

        try {
            resetPaintForFrame();

            float sx = canvas.getWidth() / VW;
            float sy = canvas.getHeight() / VH;

            canvas.save();
            canvas.scale(sx, sy);

            float baseZoom = camera.zoom * superCameraZoom;
            float renderZoom = baseZoom * ultraCameraZoom;
            float visibleWorldWidth = VW / renderZoom;
            float renderCameraX = camera.x;
            float renderCameraTop = camera.top;
            if (ultraCameraZoom != 1f) {
                // Ultra activation zooms into the player, keeping it on the same screen spot.
                CombatFighter p = player();
                float baseLeft = clamp(camera.x - VW / baseZoom / 2f, 0f, WORLD_WIDTH - VW / baseZoom);
                float focusY = p.y - 80f;
                float playerScreenX = (p.x - baseLeft) * baseZoom;
                float playerScreenY = (focusY - camera.top) * baseZoom;
                renderCameraX = p.x - playerScreenX / renderZoom + visibleWorldWidth / 2f;
                renderCameraTop = focusY - playerScreenY / renderZoom;
            }
            float cameraLeft = clamp(renderCameraX - visibleWorldWidth / 2f, 0f, WORLD_WIDTH - visibleWorldWidth);
            if (beamShake > 0f) {
                cameraLeft += (shakeRandom.nextFloat() - 0.5f) * beamShake / renderZoom;
                renderCameraTop += (shakeRandom.nextFloat() - 0.5f) * beamShake / renderZoom;
            }
            boolean beam = ultraBeamActive();

            renderCanvas.begin(canvas);
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
            if (beam) {
                drawUltraBeamOverlay(canvas, false);
                raioFinal.drawBehind(renderCanvas, ultraPack, ultraBeamFrame());
            }
            drawDamageDummy(canvas);
            effects.drawEnergyProjectiles(canvas, paint, engine.energyProjectiles);
            effects.drawSuperProjectiles(canvas, paint, engine.superProjectiles);
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
            if (ultraWindupActive()) {
                effects.drawWorldOverlay(canvas, paint, ultraDarkAlpha, 0, 0, 8);
                canvas.save();
                if (player().facing < 0) {
                    canvas.scale(-1f, 1f, player().x, 0f);
                }
                if (player().ultraPhase == CombatFighter.ULTRA_STARTUP) {
                    effects.drawSuperCharge(canvas, paint, player().x, player().y, ultraAuraTime,
                        activeFighter().profile.color);
                } else {
                    effects.drawUltraRush(canvas, paint, player().x, player().y, activeFighter().profile.color);
                }
                canvas.restore();
            }

            drawPartner(canvas);
            drawAirDashTrail(canvas, player());
            drawAirDashTrail(canvas, opponent());
            canvas.save();
            canvas.translate(tagOffset(), 0f);
            drawPlayer(canvas);
            canvas.restore();
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

            boolean pageCoversGame = paginaFinal.isActive() && !paginaFinal.isShattering();
            if (!pageCoversGame) {
                drawHud(canvas);
                drawControls(canvas);
            }
            if (paginaFinal.isActive()) {
                paginaFinal.render(renderCanvas);
            }

            canvas.restore();
        } finally {
            holder.unlockCanvasAndPost(canvas);
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
        drawReflection(c, opponentSpriteRenderer, npc, 0f, alpha);
        CombatFighter partner = partnerBody(engine.team(PLAYER));
        if (partner != null) drawReflection(c, partnerSpriteRenderer, partner, 0f, alpha);
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
        hud.drawHud(c, hudState);
        if (throwBannerFrames > 0) {
            float t = 1f - throwBannerFrames / (float)THROW_BANNER_FRAMES;
            hud.drawBanner(c, throwBanner, Math.round(255f * clamp((1f - t) * 3f, 0f, 1f)), 1f + 0.4f * Math.max(0f, 1f - t * 6f));
        }
        hud.drawCombo(c, engine.session(OPPONENT), engine.lastSession(OPPONENT),
            engine.framesSinceSessionEnd(OPPONENT), true);
        hud.drawCombo(c, engine.session(PLAYER), engine.lastSession(PLAYER),
            engine.framesSinceSessionEnd(PLAYER), false);
        if (debugOverlay) debug.drawPanel(c, engine);
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
        paint.setColor(Color.argb(70, 0, 0, 0));
        c.drawOval(npc.x - 43f, GROUND_Y - 10f, npc.x + 43f, GROUND_Y + 10f, paint);

        drawFighter(c, opponentSpriteRenderer, npc, npc.framesSinceHit < 4 && !npc.ko(), false);

        opponentPanel.x = npc.x;
        opponentPanel.visualTop = opponentVisualTop();
        opponentPanel.lifeRatio = opponentFighter.life / (float)opponentFighter.profile.maxLife;
        opponentPanel.status = opponentStatusLabel();
        opponentPanel.lifeLabel = opponentFighter.life + " / " + opponentFighter.profile.maxLife;
        opponentPanel.superLabel = opponentFighter.superHudLabel;
        opponentPanel.damageLabel = dummyDamageLabel;
        opponentPanel.damageProgress = dummyDamageLabelFrames / (float)DAMAGE_LABEL_FRAMES;
        hud.drawOpponentPanel(c, opponentPanel);
    }

    private String opponentStatusLabel() {
        CombatFighter npc = opponent();
        String name = opponentCharacter().displayName.toUpperCase(java.util.Locale.ROOT);
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
                reactionElapsed = f.knockdownFrame * FIXED_STEP;
                break;
            case AIR_HITSTUN:
                state = SpriteStates.HIT_AIR;
                reactionElapsed = f.launched || f.slammed
                    ? launchPoseTime(f.vy, f.slammed)
                    : f.stunElapsed * FIXED_STEP;
                break;
            case HITSTUN:
                state = f.hitCrouching ? SpriteStates.HIT_CROUCH : SpriteStates.HIT_STAND;
                reactionElapsed = f.stunElapsed * FIXED_STEP;
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
            lean = 1f - clamp(f.knockdownFrame / (float)config.wakeupFrames, 0f, 1f);
        } else {
            return 0f;
        }
        return -88f * lean * f.facing;
    }

    /** Picks the clip of one fighter from its engine state. Same rules for both sides. */
    private void updateFighterSprite(SpriteFighterRenderer renderer, CombatFighter f, boolean isPlayer) {
        CharacterDefinition character = f.character();
        String animation = reactionState(renderer, f);
        float animationElapsed = reactionElapsed;

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
                    animationElapsed = f.move.animationTime(elapsed);
                }
                // Otherwise the move declared a pose: crouch flag or airborne physics below.
            } else {
                CharacterDefinition.Animation special = character.specialAnimations.get(
                    f.attack.kind == AttackDefinition.Kind.SUPER ? "SUPER" : "S");
                if (special != null) {
                    animation = special.id;
                    animationElapsed = special.timeFor(elapsed, total);
                }
            }
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
        CharacterDefinition.Animation beamPose = character.specialAnimations.get("ULTRA");
        if (animation == null && f.firingBeam() && beamPose != null) {
            // Own beam sheet (9 poses): charge, shot, wind-blown hold, recovery.
            CombatConfig config = engine.config;
            int pose = RaioFinal.poseFrame(f.ultraFrame, config.ultraBeamFireFrame(),
                config.ultraBeamBlastFrame(f.beamHits), config.ultraBeamFadeFrames);
            animation = beamPose.id;
            animationElapsed = beamPose.timeOfFrame(pose);
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
            (isPlayer && isTagAnimationActive())
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
     * Throw poses from the character's own moves (no dedicated art yet): the jab reaching
     * out to grab and holding, the heavy straight for the toss, the guard when teched.
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
        if (angle != 0f) c.rotate(angle, f.x, GROUND_Y);
        renderer.draw(c, f.x, f.y, f.facing, damageFlash, guardFlash);
        c.restore();
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
        pendingInput.offer(MotionEvent.obtain(event));
        return true;
    }

    private void processPendingInput() {
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
    private boolean handleTouch(MotionEvent event) {
        if (getWidth() == 0 || getHeight() == 0) return true;
        float sx = getWidth() / VW;
        float sy = getHeight() / VH;
        int action = event.getActionMasked();
        int index = event.getActionIndex();

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            if (paginaFinal.isActive()) {
                // During the "Página Final" any touch counts for the timing.
                paginaFinal.tap();
                return true;
            }
            int pointerId = event.getPointerId(index);
            float x = event.getX(index) / sx;
            float y = event.getY(index) / sy;

            ControlsLayout.Control control = ControlsLayout.controlAt(x, y);
            switch (control) {
                case AI_TOGGLE:
                    setOpponentAiEnabled(!ai.enabled());
                    break;
                case DEBUG_TOGGLE:
                    debugOverlay = !debugOverlay;
                    break;
                case HEAL_PLAYER:
                    // Both team members, so the reserve is also ready after a tag.
                    healPlayerPointer = pointerId;
                    for (FighterState member : team) member.restoreLife();
                    break;
                case HEAL_OPPONENT:
                    healOpponentPointer = pointerId;
                    opponentFighter.restoreLife();
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
            if (pointerId == superPointer) superPointer = -1;
            if (pointerId == healPlayerPointer) healPlayerPointer = -1;
            if (pointerId == healOpponentPointer) healOpponentPointer = -1;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            dpadPointer = lightPointer = mediumPointer = heavyPointer = comboPointer = tagPointer = throwPointer = pushblockPointer = superPointer = -1;
            healPlayerPointer = healOpponentPointer = -1;
            pad.setDirection(0);
        }

        return true;
    }

    private static float clamp(float value, float min, float max) {
        return Arena.clamp(value, min, max);
    }
}
