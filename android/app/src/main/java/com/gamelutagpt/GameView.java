package com.gamelutagpt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.gamelutagpt.ultra.PaginaFinal;
import com.gamelutagpt.ultra.UltraGrade;
import com.gamelutagpt.ultra.UltraListener;
import com.gamelutagpt.ultra.UltraPack;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Android shell of the match: touch → {@link PadInput}, CPU → {@link AiController}, a
 * fixed 60 Hz {@link CombatEngine} step, then rendering. Every combat rule lives in the
 * engine; this class only presents it (sprites, Super cinematic, tag, HUD, debug).
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable, UltraListener {
    // Team and opponent rules come from the generated character packs.
    private final FighterState[] team = new FighterState[] {
        new FighterState(GeneratedCharacters.get(GeneratedCharacters.TEAM[0]), "PLAYER 1"),
        new FighterState(GeneratedCharacters.get(GeneratedCharacters.TEAM[1]), "PLAYER 2")
    };

    private int activeFighterIndex = 0;
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

    // Ultra "Página Final" portado da w5j169 para o shell do CombatEngine V2.
    private static final int ULTRA_COST = 3 * CombatConfig.METER_PER_BAR;
    private static final int ULTRA_IDLE = 0;
    private static final int ULTRA_STARTUP = 1;
    private static final int ULTRA_RUSH = 2;
    private static final int ULTRA_WHIFF = 3;
    private static final int ULTRA_CINEMATIC = 4;
    private static final float ULTRA_STARTUP_DURATION = 0.42f;
    private static final float ULTRA_RUSH_DURATION = 0.30f;
    private static final float ULTRA_RUSH_SPEED = 2000f;
    private static final float ULTRA_RUSH_REACH = 110f;
    private static final float ULTRA_WHIFF_DURATION = 0.50f;
    private static final float ULTRA_STARTUP_ZOOM = 0.30f;
    private static final String LOG_TAG = "GameView";
    private static final String[] ULTRA_FOLDERS = {"ultras/player1", "ultras/player2"};
    private static final int[] ULTRA_DAMAGE = {4200, 3900};

    private int ultraPhase = ULTRA_IDLE;
    private float ultraPhaseTimer;
    private float ultraCameraZoom = 1f;
    private int ultraDarkAlpha;
    private int ultraDamageDealt;
    private boolean ultraStartRequested;
    private boolean ultraTapRequested;
    private final UltraPack[] ultraPacks = new UltraPack[team.length];
    private final PaginaFinal paginaFinal = new PaginaFinal(this);
    private final AndroidUltraCanvas ultraCanvas = new AndroidUltraCanvas();
    private final UltraSounds ultraSounds;

    private static final int TAG_IDLE = 0;
    private static final int TAG_EXIT = 1;
    private static final int TAG_ENTER = 2;
    private static final int TAG_POSE = 3;
    private static final float TAG_EXIT_DURATION = 0.34f;
    private static final float TAG_ENTER_DURATION = 0.38f;
    private static final float TAG_POSE_DURATION = 0.45f;
    private static final float TAG_TRAVEL_DISTANCE = 760f;
    private static final float TAG_COOLDOWN_SECONDS = 10f;
    private int tagPhase = TAG_IDLE;
    private float tagPhaseTimer = 0f;
    private float tagVisualOffsetX = 0f;
    private int tagExitDirection = -1;
    private float tagCooldownRemaining = 0f;
    private String tagCooldownHudLabel = "TROCA: PRONTA";
    private String tagCooldownButtonLabel = "";
    private int tagCooldownDisplayedTenths = -1;
    private int tagCooldownDisplayedSeconds = -1;

    // Single version source: versionName in app/build.gradle.
    private static final String VERSION_HUD_LABEL =
        "SPRITE GPT • v" + BuildConfig.VERSION_NAME.split("-")[0];

    private int dpadPointer = -1;
    private int lightPointer = -1;
    private int mediumPointer = -1;
    private int heavyPointer = -1;
    private int comboPointer = -1;
    private int tagPointer = -1;
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
        dpadPointer = lightPointer = mediumPointer = heavyPointer = comboPointer = tagPointer = superPointer = -1;
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
        holder = getHolder();
        holder.addCallback(this);
        setFocusable(true);
        setKeepScreenOn(true);

        ultraSounds = new UltraSounds(context.getAssets());
        AndroidUltraAssets ultraAssets = new AndroidUltraAssets(context.getAssets());
        for (int i = 0; i < team.length; i++) {
            ultraPacks[i] = loadUltraPack(ultraAssets, i);
            ultraSounds.loadUltra(ULTRA_FOLDERS[i]);
        }
    }

    private UltraPack loadUltraPack(AndroidUltraAssets assets, int teamIndex) {
        FighterState fighter = team[teamIndex];
        try {
            UltraPack pack = UltraPack.load(assets, ULTRA_FOLDERS[teamIndex]);
            for (String warning : pack.warnings) {
                Log.w(LOG_TAG, ULTRA_FOLDERS[teamIndex] + ": " + warning);
            }
            return pack;
        } catch (IOException | RuntimeException ex) {
            Log.w(LOG_TAG, "Ultra de " + fighter.character.displayName + " usando arte provisória", ex);
            return UltraPack.placeholder(
                "ULTRA " + fighter.character.displayName.toUpperCase(java.util.Locale.ROOT),
                fighter.profile.color
            );
        }
    }

    private FighterState activeFighter() {
        return team[activeFighterIndex];
    }

    private FighterState reserveFighter() {
        return team[(activeFighterIndex + 1) % team.length];
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
        if (ultraStartRequested) {
            ultraStartRequested = false;
            startUltra();
        }

        boolean ultraFreeze =
            ultraPhase == ULTRA_STARTUP ||
            ultraPhase == ULTRA_RUSH ||
            ultraPhase == ULTRA_CINEMATIC;

        if (ultraFreeze) {
            player().locked = true;
            opponent().locked = true;
            playerInput.clear();
            opponentInput.clear();
            updateUltra(FIXED_STEP);
            if (dummyDamageLabelFrames > 0) dummyDamageLabelFrames--;
            updateSuperVisuals();
            updateFightCamera(FIXED_STEP);
            updateFighterSprite(spriteFighterRenderer, player(), true);
            updateFighterSprite(opponentSpriteRenderer, opponent(), false);
            return;
        }

        if (ultraPhase == ULTRA_WHIFF) updateUltra(FIXED_STEP);

        updateTagState(FIXED_STEP);
        boolean tagging = isTagAnimationActive();
        boolean ultraWhiff = ultraPhase == ULTRA_WHIFF;
        player().locked = tagging || ultraWhiff;
        opponent().locked = false;

        pad.drainInto(playerInput);
        if (tagging) {
            opponentInput.clear();
        } else {
            ai.fill(engine, OPPONENT, opponentInput);
        }
        engine.step(playerInput, opponentInput);

        for (CombatEngine.HitEvent event : engine.events()) {
            if (event.defender == OPPONENT && !event.blocked) {
                dummyDamageLabel = "-" + event.damage;
                dummyDamageLabelFrames = DAMAGE_LABEL_FRAMES;
            }
        }
        if (dummyDamageLabelFrames > 0) dummyDamageLabelFrames--;

        updateSuperVisuals();
        updateFightCamera(FIXED_STEP);
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
            if (isTagAnimationActive()) return 0f;
            if (tagCooldownRemaining > 0f) return 1f - tagCooldownRemaining / TAG_COOLDOWN_SECONDS;
            return 1f;
        }
        @Override public String tagCooldownLabel() { return tagCooldownHudLabel; }
        @Override public String tagButtonLabel() { return tagCooldownButtonLabel; }
        @Override public boolean canTag() { return canStartTag(); }
        @Override public boolean canSuper() { return ultraReady() || superAvailable(); }
        @Override public String superButtonLabel() { return ultraReady() ? "ULTRA" : "SUPER"; }
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
            !isUltraActive() &&
            (p.canAct() || CancelSystem.canCancel(p, "SUPER", engine.session(OPPONENT)));
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


    private boolean isUltraActive() {
        return ultraPhase != ULTRA_IDLE;
    }

    private boolean ultraReady() {
        return ControlsLayout.isDownDirection(pad.direction()) && canStartUltra();
    }

    private boolean canStartUltra() {
        CombatFighter p = player();
        return p.canAct() &&
            p.grounded &&
            !opponent().ko() &&
            activeFighter().superMeter >= ULTRA_COST &&
            !isTagAnimationActive() &&
            !isSuperCinematicActive() &&
            !paginaFinal.isActive();
    }

    private void startUltra() {
        if (!canStartUltra()) return;

        FighterState fighter = activeFighter();
        fighter.superMeter = Math.max(0, fighter.superMeter - ULTRA_COST);
        fighter.refreshHudLabels();

        clearInput();
        playerInput.clear();
        opponentInput.clear();

        CombatFighter p = player();
        p.clearAttack();
        p.status = CombatFighter.Status.NEUTRAL;
        p.forwardDashing = false;
        p.backdashFrames = 0;
        p.hitstop = 0;
        p.travel = 0f;
        p.locked = true;
        opponent().locked = true;

        ultraPhase = ULTRA_STARTUP;
        ultraPhaseTimer = 0f;
        ultraCameraZoom = 1f;
        ultraDarkAlpha = 0;
        ultraDamageDealt = 0;
        ultraTapRequested = false;

        ultraSounds.play(ULTRA_FOLDERS[activeFighterIndex], "ativacao");
    }

    private void updateUltra(float dt) {
        ultraPhaseTimer += dt;

        if (ultraPhase == ULTRA_STARTUP) {
            float t = clamp(ultraPhaseTimer / ULTRA_STARTUP_DURATION, 0f, 1f);
            float eased = 1f - (1f - t) * (1f - t);
            ultraDarkAlpha = Math.round(205f * Math.min(1f, t * 3f));
            ultraCameraZoom = 1f + ULTRA_STARTUP_ZOOM * eased;
            if (t >= 1f) {
                ultraPhase = ULTRA_RUSH;
                ultraPhaseTimer = 0f;
                ultraSounds.play(ULTRA_FOLDERS[activeFighterIndex], "investida");
            }
            return;
        }

        if (ultraPhase == ULTRA_RUSH) {
            CombatFighter p = player();
            float before = p.x;
            p.x = clamp(
                p.x + p.facing * ULTRA_RUSH_SPEED * dt,
                Arena.LEFT_BOUND,
                Arena.RIGHT_BOUND
            );
            p.travel = p.x - before;

            float t = clamp(ultraPhaseTimer / ULTRA_RUSH_DURATION, 0f, 1f);
            ultraDarkAlpha = Math.round(205f * (1f - t));
            ultraCameraZoom = 1f + ULTRA_STARTUP_ZOOM * (1f - t);

            if (ultraRushConnects()) {
                beginUltraCinematic();
            } else if (t >= 1f) {
                ultraPhase = ULTRA_WHIFF;
                ultraPhaseTimer = 0f;
                ultraDarkAlpha = 0;
                ultraCameraZoom = 1f;
                opponent().locked = false;
            }
            return;
        }

        if (ultraPhase == ULTRA_WHIFF) {
            ultraDarkAlpha = 0;
            ultraCameraZoom = 1f;
            if (ultraPhaseTimer >= ULTRA_WHIFF_DURATION) {
                ultraPhase = ULTRA_IDLE;
                ultraPhaseTimer = 0f;
                player().locked = false;
            }
            return;
        }

        if (ultraPhase == ULTRA_CINEMATIC) {
            if (ultraTapRequested) {
                ultraTapRequested = false;
                paginaFinal.tap();
            }
            paginaFinal.update(dt);
        }
    }

    private boolean ultraRushConnects() {
        CombatFighter p = player();
        CombatFighter o = opponent();
        if (o.ko()) return false;

        float horizontalDistance = (o.x - p.x) * p.facing;
        if (horizontalDistance < -20f || horizontalDistance > ULTRA_RUSH_REACH) return false;

        float playerCenterY = (p.hurtTop() + p.y) * 0.5f;
        float opponentCenterY = (o.hurtTop() + o.y) * 0.5f;
        return Math.abs(playerCenterY - opponentCenterY) <= 120f;
    }

    private void beginUltraCinematic() {
        CombatFighter p = player();
        CombatFighter o = opponent();

        ultraPhase = ULTRA_CINEMATIC;
        ultraPhaseTimer = 0f;
        ultraDarkAlpha = 0;
        ultraCameraZoom = 1f;
        p.x = clamp(o.x - p.facing * 80f, Arena.LEFT_BOUND, Arena.RIGHT_BOUND);
        p.travel = 0f;
        p.locked = true;
        o.locked = true;

        p.clearAttack();
        p.status = CombatFighter.Status.NEUTRAL;
        o.clearAttack();
        o.hitstop = 0;
        engine.energyProjectiles.clear();
        engine.superProjectiles.clear();

        paginaFinal.start(ultraPacks[activeFighterIndex], p.facing);
    }

    @Override
    public void onUltraHit(int hitIndex, float damageFraction) {
        int total = ULTRA_DAMAGE[Math.min(activeFighterIndex, ULTRA_DAMAGE.length - 1)];
        int damage = Math.round(total * damageFraction);
        int before = opponentFighter.life;
        opponentFighter.life = Math.max(0, opponentFighter.life - damage);
        opponentFighter.refreshHudLabels();
        ultraDamageDealt += before - opponentFighter.life;
        dummyDamageLabel = "-" + ultraDamageDealt;
        dummyDamageLabelFrames = DAMAGE_LABEL_FRAMES;
        opponent().framesSinceHit = 0;
    }

    @Override
    public void onUltraSound(String soundId) {
        ultraSounds.play(ULTRA_FOLDERS[activeFighterIndex], soundId);
    }

    @Override
    public void onUltraFinished(UltraGrade grade) {
        ultraPhase = ULTRA_IDLE;
        ultraPhaseTimer = 0f;
        ultraCameraZoom = 1f;
        ultraDarkAlpha = 0;
        ultraTapRequested = false;

        CombatFighter p = player();
        CombatFighter o = opponent();
        p.locked = false;
        p.status = CombatFighter.Status.NEUTRAL;
        p.clearAttack();

        o.locked = false;
        o.clearAttack();
        o.status = CombatFighter.Status.AIR_HITSTUN;
        o.grounded = false;
        o.y = Math.min(o.y, GROUND_Y - 2f);
        o.vy = -900f;
        o.launched = true;
        o.slammed = true;
        o.stunLeft = o.stunTotal = 90;
        o.stunElapsed = 0;
        o.knockdownFrame = 0;
        o.pushRemaining = p.facing * 280f;
        o.pushFramesLeft = 18;
        o.pushSource = p;
        o.framesSinceHit = 0;

        dummyDamageLabel = "-" + ultraDamageDealt;
        dummyDamageLabelFrames = DAMAGE_LABEL_FRAMES;
    }

    private boolean isTagAnimationActive() {
        return tagPhase != TAG_IDLE;
    }

    private boolean canStartTag() {
        return player().canAct() &&
            !isTagAnimationActive() &&
            !isUltraActive() &&
            tagCooldownRemaining <= 0f;
    }

    private void updateTagCooldownLabels() {
        if (isTagAnimationActive()) {
            tagCooldownHudLabel = "TROCA: EM ANDAMENTO";
            tagCooldownButtonLabel = "...";
            tagCooldownDisplayedTenths = -1;
            tagCooldownDisplayedSeconds = -1;
            return;
        }

        if (tagCooldownRemaining <= 0f) {
            tagCooldownHudLabel = "TROCA: PRONTA";
            tagCooldownButtonLabel = "";
            tagCooldownDisplayedTenths = -1;
            tagCooldownDisplayedSeconds = -1;
            return;
        }

        int tenths = (int)Math.ceil(tagCooldownRemaining * 10f);
        if (tenths != tagCooldownDisplayedTenths) {
            tagCooldownDisplayedTenths = tenths;
            tagCooldownHudLabel = String.format(java.util.Locale.US, "TROCA: %.1fs", tenths / 10f);
        }

        int seconds = (int)Math.ceil(tagCooldownRemaining);
        if (seconds != tagCooldownDisplayedSeconds) {
            tagCooldownDisplayedSeconds = seconds;
            tagCooldownButtonLabel = Integer.toString(seconds);
        }
    }

    private void updateTagState(float dt) {
        if (tagCooldownRemaining > 0f) {
            tagCooldownRemaining = Math.max(0f, tagCooldownRemaining - dt);
        }

        updateTagCooldownLabels();

        if (tagPhase == TAG_IDLE) return;

        tagPhaseTimer += dt;

        if (tagPhase == TAG_EXIT) {
            float t = clamp(tagPhaseTimer / TAG_EXIT_DURATION, 0f, 1f);
            tagVisualOffsetX = tagExitDirection * TAG_TRAVEL_DISTANCE * t * t;

            if (t >= 1f) {
                activeFighterIndex = (activeFighterIndex + 1) % team.length;
                engine.setFighterState(PLAYER, activeFighter());
                spriteFighterRenderer.setCharacter(activeCharacter().id);
                tagPhase = TAG_ENTER;
                tagPhaseTimer = 0f;
                tagVisualOffsetX = tagExitDirection * TAG_TRAVEL_DISTANCE;
            }
        } else if (tagPhase == TAG_ENTER) {
            float t = clamp(tagPhaseTimer / TAG_ENTER_DURATION, 0f, 1f);
            float eased = 1f - (1f - t) * (1f - t);
            tagVisualOffsetX = tagExitDirection * TAG_TRAVEL_DISTANCE * (1f - eased);

            if (t >= 1f) {
                tagPhase = TAG_POSE;
                tagPhaseTimer = 0f;
                tagVisualOffsetX = 0f;
            }
        } else if (tagPhase == TAG_POSE) {
            tagVisualOffsetX = 0f;

            if (tagPhaseTimer >= TAG_POSE_DURATION) {
                tagPhase = TAG_IDLE;
                tagPhaseTimer = 0f;
                tagCooldownRemaining = TAG_COOLDOWN_SECONDS;
            }
        }
    }

    private void startTagAnimation() {
        if (!canStartTag()) return;
        tagPhase = TAG_EXIT;
        tagPhaseTimer = 0f;
        tagVisualOffsetX = 0f;
        tagExitDirection = -player().facing;
        updateTagCooldownLabels();
    }

    private void switchFighter() {
        startTagAnimation();
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
        Canvas canvas = holder.lockCanvas();
        if (canvas == null) return;

        try {
            resetPaintForFrame();

            float sx = canvas.getWidth() / VW;
            float sy = canvas.getHeight() / VH;

            canvas.save();
            canvas.scale(sx, sy);

            float renderZoom = camera.zoom * superCameraZoom * ultraCameraZoom;
            float visibleWorldWidth = VW / renderZoom;
            float cameraLeft = clamp(camera.x - visibleWorldWidth / 2f, 0f, WORLD_WIDTH - visibleWorldWidth);

            canvas.save();
            canvas.scale(renderZoom, renderZoom);
            canvas.translate(-cameraLeft, -camera.top);
            drawScenario(canvas);
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
            drawUltraDarkening(canvas);
            drawUltraAura(canvas);

            canvas.save();
            canvas.translate(tagVisualOffsetX, 0f);
            drawPlayer(canvas);
            canvas.restore();
            if (isSuperCinematicActive()) {
                effects.drawWorldOverlay(canvas, paint, superFlashAlpha, 255, 255, 255);
            }
            if (debugOverlay) debug.drawWorld(canvas, engine);
            canvas.restore();

            boolean pageCoversGame = paginaFinal.isActive() && !paginaFinal.isShattering();
            if (!pageCoversGame) {
                drawHud(canvas);
                drawControls(canvas);
            }
            if (paginaFinal.isActive()) {
                ultraCanvas.begin(canvas);
                paginaFinal.render(ultraCanvas);
            }

            canvas.restore();
        } finally {
            holder.unlockCanvasAndPost(canvas);
        }
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
        if (ultraPhase == ULTRA_WHIFF) return "ULTRA ERROU";
        if (isUltraActive()) return "ULTRA";
        CombatFighter p = player();
        switch (p.status) {
            case KNOCKDOWN:
            case WAKEUP:
                return "DERRUBADO";
            case BLOCKSTUN:
                return guardLabel(p.lastGuard, "BLOQUEIO");
            case AIR_HITSTUN:
                return p.slammed ? "QUEDA FORÇADA" : p.launched ? "LANÇADO" : "HIT";
            case HITSTUN:
                return "HIT";
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
        if (p.backdashFrames > 0) return "BACKDASH";
        if (p.forwardDashing) return "DASH";
        if (p.superJumping) return "SUPER JUMP";
        if (p.crouching) return "AGACHADO";
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

        boolean combatPose = animation == null && (
            f.attacking() ||
            guard != CombatFighter.GUARD_NONE ||
            (isPlayer && (isTagAnimationActive() || isUltraActive()))
        );
        boolean locked = f.status != CombatFighter.Status.NEUTRAL && !f.attacking();

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

    private void drawFighter(Canvas c, SpriteFighterRenderer renderer, CombatFighter f,
                             boolean damageFlash, boolean guardFlash) {
        float angle = knockdownAngle(renderer, f);
        c.save();
        if (angle != 0f) c.rotate(angle, f.x, GROUND_Y);
        renderer.draw(c, f.x, f.y, f.facing, damageFlash, guardFlash);
        c.restore();
    }


    private void drawUltraDarkening(Canvas c) {
        if (ultraDarkAlpha <= 0) return;
        effects.drawWorldOverlay(c, paint, ultraDarkAlpha, 0, 0, 8);
    }

    private void drawUltraAura(Canvas c) {
        if (ultraPhase != ULTRA_STARTUP && ultraPhase != ULTRA_RUSH) return;

        CombatFighter p = player();
        int color = activeFighter().profile.color;
        float centerY = p.y - activeFighter().character.visualStandHeight * 0.52f;
        paint.setStrokeCap(Paint.Cap.ROUND);

        if (ultraPhase == ULTRA_STARTUP) {
            float t = clamp(ultraPhaseTimer / ULTRA_STARTUP_DURATION, 0f, 1f);
            float pulse = 1f + 0.18f * (float)Math.sin(ultraPhaseTimer * 40f);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(
                Math.round(110f * t),
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            ));
            c.drawCircle(p.x, centerY, 125f * pulse, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(7f);
            paint.setColor(Color.argb(220, 255, 255, 255));
            c.drawCircle(p.x, centerY, 70f * pulse, paint);
            paint.setStrokeWidth(4f);
            paint.setColor(color);
            c.drawCircle(p.x, centerY, 102f * pulse, paint);

            paint.setStrokeWidth(4f);
            paint.setColor(Color.argb(180, 255, 255, 255));
            for (int i = 0; i < 10; i++) {
                float x = p.x - 92f + i * 20f;
                float length = 40f + (i % 3) * 28f;
                float y = centerY + 90f - ((ultraPhaseTimer * 620f + i * 37f) % 230f);
                c.drawLine(x, y, x, y - length, paint);
            }
        } else {
            paint.setStyle(Paint.Style.STROKE);
            for (int i = 0; i < 7; i++) {
                float y = centerY - 72f + i * 24f;
                float length = 130f + (i % 3) * 70f;
                paint.setStrokeWidth(i % 2 == 0 ? 6f : 3f);
                paint.setColor(i % 2 == 0 ? color : Color.argb(200, 255, 255, 255));
                float from = p.x - p.facing * (40f + length);
                float to = p.x - p.facing * 40f;
                c.drawLine(from, y, to, y, paint);
            }
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);
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
            int pointerId = event.getPointerId(index);
            float x = event.getX(index) / sx;
            float y = event.getY(index) / sy;

            if (ultraPhase == ULTRA_CINEMATIC) {
                ultraTapRequested = true;
                return true;
            }
            if (isUltraActive()) return true;

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
                    if (ultraReady()) {
                        ultraStartRequested = true;
                    } else {
                        pad.press(PadInput.Button.SUPER);
                    }
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
                case TAG:
                    if (canStartTag()) {
                        tagPointer = pointerId;
                        switchFighter();
                    }
                    break;
                default:
                    break;
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (isUltraActive()) return true;
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
            if (pointerId == superPointer) superPointer = -1;
            if (pointerId == healPlayerPointer) healPlayerPointer = -1;
            if (pointerId == healOpponentPointer) healOpponentPointer = -1;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            dpadPointer = lightPointer = mediumPointer = heavyPointer = comboPointer = tagPointer = superPointer = -1;
            healPlayerPointer = healOpponentPointer = -1;
            pad.setDirection(0);
        }

        return true;
    }

    private static float clamp(float value, float min, float max) {
        return Arena.clamp(value, min, max);
    }
}
