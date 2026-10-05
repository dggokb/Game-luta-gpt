package com.gamelutagpt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final class FighterProfile {
        final String name;
        final String[] baseCombo;
        final int maxLife;
        final int color;
        final boolean hasEnergyAttack;
        final float energyRange;
        final float energySpeed;
        final int lightDamage;
        final int mediumDamage;
        final int heavyDamage;
        final int energyDamage;
        final int[] energyCommand;
        final boolean hasSuperAttack;
        final float superRange;
        final float superSpeed;
        final int superDamage;
        final String reserveHudLabel;

        FighterProfile(
            String name,
            String[] baseCombo,
            int maxLife,
            int color,
            boolean hasEnergyAttack,
            float energyRange,
            float energySpeed,
            int lightDamage,
            int mediumDamage,
            int heavyDamage,
            int energyDamage,
            int[] energyCommand,
            boolean hasSuperAttack,
            float superRange,
            float superSpeed,
            int superDamage
        ) {
            this.name = name;
            this.baseCombo = baseCombo;
            this.maxLife = maxLife;
            this.color = color;
            this.hasEnergyAttack = hasEnergyAttack;
            this.energyRange = energyRange;
            this.energySpeed = energySpeed;
            this.lightDamage = lightDamage;
            this.mediumDamage = mediumDamage;
            this.heavyDamage = heavyDamage;
            this.energyDamage = energyDamage;
            this.energyCommand = energyCommand;
            this.hasSuperAttack = hasSuperAttack;
            this.superRange = superRange;
            this.superSpeed = superSpeed;
            this.superDamage = superDamage;
            this.reserveHudLabel = "RESERVA: " + name;
        }
    }

    private static final class FighterState {
        final FighterProfile profile;
        int life;
        float superMeter;
        String lifeHudLabel;
        String superHudLabel;
        String superLevelHudLabel;

        FighterState(FighterProfile profile) {
            this.profile = profile;
            this.life = profile.maxLife;
            this.superMeter = 0f;
            refreshHudLabels();
        }

        void refreshHudLabels() {
            int level = (int)Math.floor(superMeter);
            lifeHudLabel = "HP " + life + " / " + profile.maxLife;
            superHudLabel = String.format(
                java.util.Locale.US,
                "SUPER %.2f / 5  •  LV %d",
                superMeter,
                level
            );
            superLevelHudLabel = "LV " + level;
        }
    }

    private static final class EnergyProjectile {
        float x;
        final float y;
        final float startX;
        final float range;
        final float speed;
        final int damage;
        final int color;
        final int ownerIndex;
        final int direction;

        EnergyProjectile(
            float x,
            float y,
            float range,
            float speed,
            int damage,
            int color,
            int ownerIndex,
            int direction
        ) {
            this.x = x;
            this.y = y;
            this.startX = x;
            this.range = range;
            this.speed = speed;
            this.damage = damage;
            this.color = color;
            this.ownerIndex = ownerIndex;
            this.direction = direction;
        }
    }

    private static final class SuperProjectile {
        float x;
        final float y;
        final float startX;
        final float range;
        final float speed;
        final int damage;
        final int color;
        final int ownerIndex;
        final int direction;

        SuperProjectile(
            float x,
            float y,
            float range,
            float speed,
            int damage,
            int color,
            int ownerIndex,
            int direction
        ) {
            this.x = x;
            this.y = y;
            this.startX = x;
            this.range = range;
            this.speed = speed;
            this.damage = damage;
            this.color = color;
            this.ownerIndex = ownerIndex;
            this.direction = direction;
        }
    }

    private final FighterState[] team = new FighterState[] {
        new FighterState(new FighterProfile(
            "PLAYER 1",
            new String[]{"L", "M", "H"},
            10000,
            Color.rgb(244, 183, 59),
            true,
            720f,
            760f,
            300,
            500,
            800,
            850,
            new int[]{3, 1},
            true,
            1450f,
            1180f,
            3200
        )),
        new FighterState(new FighterProfile(
            "PLAYER 2",
            new String[]{"L", "L", "H", "M"},
            10000,
            Color.rgb(74, 205, 232),
            true,
            560f,
            980f,
            300,
            500,
            800,
            650,
            new int[]{5, 1},
            true,
            1320f,
            1280f,
            2850
        ))
    };

    private int activeFighterIndex = 0;
    private static final int AI_OWNER_INDEX = 99;
    private final FighterState opponentFighter =
        new FighterState(team[1].profile);
    private final List<EnergyProjectile> energyProjectiles = new ArrayList<>();
    private final List<SuperProjectile> superProjectiles = new ArrayList<>();

    private static final float VW = 1280f;
    private static final float VH = 720f;
    private static final float GROUND_Y = 565f;
    private static final float WORLD_WIDTH = 2600f;
    private static final float LEFT_BOUND = 90f;
    private static final float RIGHT_BOUND = WORLD_WIDTH - 90f;
    private static final float CAMERA_ZOOM = 1.12f;
    private static final float CAMERA_MIN_ZOOM = 0.78f;
    private static final float CAMERA_FIGHTER_MARGIN_X = 520f;
    private static final float CAMERA_GROUND_SCREEN_Y = 552f;
    private static final float CAMERA_TOP_MARGIN_SCREEN = 64f;
    private static final float CAMERA_BOTTOM_MARGIN_SCREEN = 45f;
    private static final float GROUND_CAMERA_TOP = 72f;
    private static final float WORLD_TOP = -520f;

    private static final float DUMMY_START_X = 980f;
    private static final float DUMMY_HALF_WIDTH = 34f;
    private static final int DUMMY_MAX_LIFE = 10000;
    private static final float DUMMY_HIT_REACTION_DURATION = 0.22f;
    private static final float DUMMY_LAUNCH_SPEED = 1450f;
    private static final float DUMMY_SLAM_SPEED = 1850f;
    private static final float DUMMY_GRAVITY = 1650f;
    private static final int DUMMY_KD_NONE = 0;
    private static final int DUMMY_KD_FALL = 1;
    private static final int DUMMY_KD_DOWN = 2;
    private static final int DUMMY_KD_GETUP = 3;
    private static final float DUMMY_KD_FALL_DURATION = 0.22f;
    private static final float DUMMY_KD_DOWN_DURATION = 1.20f;
    private static final float DUMMY_KD_GETUP_DURATION = 0.35f;
    private static final float AI_BUTTON_LEFT = 1082f;
    private static final float AI_BUTTON_TOP = 124f;
    private static final float AI_BUTTON_RIGHT = 1248f;
    private static final float AI_BUTTON_BOTTOM = 172f;
    private static final float AI_DECISION_MIN = 0.12f;
    private static final float AI_DECISION_MAX = 0.28f;
    private static final float AI_WALK_SPEED = 300f;
    private static final float AI_DASH_SPEED = 620f;
    private static final float AI_BACKDASH_SPEED = 760f;
    private static final float AI_BACKDASH_DURATION = 0.20f;
    private static final float AI_ATTACK_COOLDOWN = 0.22f;
    private float dummyX = DUMMY_START_X;
    private float dummyY = GROUND_Y;
    private float dummyVelocityY = 0f;
    private boolean dummyAirborne = false;
    private boolean dummyMovementLocked = false;
    private boolean dummyLaunchedByHit = false;
    private boolean dummyGroundSlam = false;
    private int dummyKnockdownState = DUMMY_KD_NONE;
    private float dummyKnockdownTimer = 0f;
    private int dummyLife = DUMMY_MAX_LIFE;
    private String dummyLifeHudLabel = "10000 / 10000";
    private String dummyDamageLabel = "";
    private float dummyDamageLabelTimer = 0f;
    private float dummyHitReactionTimer = 0f;
    private float dummyKnockbackVelocityX = 0f;

    private boolean opponentAiEnabled = false;
    private float aiDecisionTimer = 0f;
    private float aiAttackCooldownRemaining = 0f;
    private boolean aiMovingForward = false;
    private boolean aiMovingBack = false;
    private boolean aiForwardDashing = false;
    private float aiBackDashTimer = 0f;
    private boolean aiCrouching = false;
    private boolean aiSuperJumping = false;
    private float aiWalkTime = 0f;
    private String dummyAttackType = "";
    private float dummyAttackTimer = 0f;
    private float dummyAttackDuration = 0f;
    private boolean dummyAttackHitApplied = false;
    private boolean aiChaseLauncher = false;
    private float aiSuperTimer = 0f;
    private String aiStatusLabel = "IA OFF";

    private final SurfaceHolder holder;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SpriteFighterRenderer spriteFighterRenderer;
    private final LinearGradient skyGradient;
    private final android.graphics.Path[] mountainPaths =
        new android.graphics.Path[15];

    private Thread gameThread;
    private volatile boolean running;

    private float playerX = 420f;
    private float playerY = GROUND_Y;
    private float cameraX = 700f;
    private float cameraTop = GROUND_CAMERA_TOP;
    private float cameraZoom = CAMERA_ZOOM;
    private float velocityY = 0f;
    private int facingDirection = 1;
    private static final float FACING_SWITCH_EPSILON = 6f;

    private boolean movingLeft;
    private boolean movingRight;
    private boolean crouching;
    private boolean grounded = true;
    private boolean superJumping = false;
    private boolean forwardDashing = false;
    private float backDashTimer = 0f;

    private String attackType = "";
    private float attackTimer = 0f;
    private float attackDuration = 0f;
    private CharacterDefinition.Move activeSpriteMove;
    private float walkTime = 0f;
    private boolean attackHitApplied = false;
    private float playerDamageFlashTimer = 0f;
    private float playerBlockFlashTimer = 0f;
    private float playerBlockstunTimer = 0f;
    private int playerLastGuardState = GUARD_NONE;
    private float playerHitReactionTimer = 0f;
    private float playerKnockbackVelocityX = 0f;
    private boolean playerMovementLocked = false;
    private boolean playerLaunchedByHit = false;
    private boolean playerGroundSlam = false;
    private int playerKnockdownState = DUMMY_KD_NONE;
    private float playerKnockdownTimer = 0f;

    private final float moveSpeed = 300f;
    private final float forwardDashSpeed = 620f;
    private final float backDashSpeed = 760f;
    private final float backDashDuration = 0.20f;
    private final float jumpSpeed = 660f;
    private final float superJumpSpeed = 1450f;
    private final float gravity = 1650f;

    private static final int GUARD_NONE = 0;
    private static final int GUARD_HIGH = 1;
    private static final int GUARD_LOW = 2;
    private static final float BLOCKSTUN_DURATION = 0.18f;
    private static final float BLOCK_PUSH_SPEED = 72f;

    private static final float DPAD_X = 175f;
    private static final float DPAD_Y = 555f;
    private static final float DPAD_RADIUS = 122f;
    private static final float DPAD_DEADZONE = 28f;
    private static final float DPAD_DIAGONAL = 0.70710677f;
    private static final String[] DPAD_LABELS = {
        "→", "↘", "↓", "↙", "←", "↖", "↑", "↗"
    };
    private static final float[] DPAD_UNIT_X = {
        1f, DPAD_DIAGONAL, 0f, -DPAD_DIAGONAL,
        -1f, -DPAD_DIAGONAL, 0f, DPAD_DIAGONAL
    };
    private static final float[] DPAD_UNIT_Y = {
        0f, DPAD_DIAGONAL, 1f, DPAD_DIAGONAL,
        0f, -DPAD_DIAGONAL, -1f, -DPAD_DIAGONAL
    };

    private static final float ATTACK_RADIUS = 54f;
    private static final float LIGHT_X = 1005f;
    private static final float LIGHT_Y = 598f;
    private static final float MEDIUM_X = 1100f;
    private static final float MEDIUM_Y = 515f;
    private static final float HEAVY_X = 1195f;
    private static final float HEAVY_Y = 598f;

    private static final float COMBO_X = 1100f;
    private static final float COMBO_Y = 650f;
    private static final float COMBO_RADIUS = 43f;

    private static final float TAG_X = 930f;
    private static final float TAG_Y = 505f;
    private static final float TAG_RADIUS = 43f;

    private static final float SUPER_X = 905f;
    private static final float SUPER_Y = 620f;
    private static final float SUPER_RADIUS = 46f;

    private static final float MAX_SUPER_METER = 5f;
    private static final float SUPER_COST = 1f;
    private static final float SUPER_GAIN_LIGHT = 0.10f;
    private static final float SUPER_GAIN_MEDIUM = 0.15f;
    private static final float SUPER_GAIN_HEAVY = 0.20f;
    private static final float SUPER_GAIN_ENERGY = 0.45f;
    private static final float SUPER_GUARD_GAIN_LIGHT = 0.05f;
    private static final float SUPER_GUARD_GAIN_MEDIUM = 0.075f;
    private static final float SUPER_GUARD_GAIN_HEAVY = 0.10f;
    private static final float SUPER_GUARD_GAIN_ENERGY = 0.20f;
    private static final float SUPER_GUARD_GAIN_SUPER = 0.25f;

    // 0 neutro, 1 direita, 2 baixo-direita, 3 baixo, 4 baixo-esquerda,
    // 5 esquerda, 6 cima-esquerda, 7 cima, 8 cima-direita.
    private int dpadDirection = 0;
    private int dpadPointer = -1;
    private long lastDownInputMs = -1000L;
    private long lastForwardTapMs = -1000L;
    private long lastBackTapMs = -1000L;
    private long launcherChaseUntilMs = -1L;
    private static final long DASH_DOUBLE_TAP_MS = 300L;
    private static final long LAUNCHER_CHASE_WINDOW_MS = 900L;

    private int lightPointer = -1;
    private int mediumPointer = -1;
    private int heavyPointer = -1;
    private int comboPointer = -1;
    private int tagPointer = -1;
    private int superPointer = -1;

    private static final int SUPER_IDLE = 0;
    private static final int SUPER_DARKEN = 1;
    private static final int SUPER_POSE = 2;
    private static final int SUPER_FLASH = 3;
    private static final int SUPER_RELEASE = 4;
    private static final int SUPER_RECOVER = 5;
    private static final float SUPER_DARKEN_DURATION = 0.16f;
    private static final float SUPER_POSE_DURATION = 0.30f;
    private static final float SUPER_FLASH_DURATION = 0.12f;
    private static final float SUPER_RELEASE_DURATION = 0.20f;
    private static final float SUPER_RECOVER_DURATION = 0.22f;
    private int superPhase = SUPER_IDLE;
    private float superPhaseTimer = 0f;
    private float superCameraZoom = 1f;
    private int superDarkAlpha = 0;
    private int superFlashAlpha = 0;
    private float superStoredVelocityY = 0f;

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

    private int autoComboIndex = 0;
    private long lastAutoComboTapMs = -1000L;
    private static final long AUTO_COMBO_RESET_MS = 700L;

    private static final int COMMAND_BUFFER_SIZE = 8;
    private static final long ENERGY_COMMAND_STEP_MS = 420L;
    private static final long ENERGY_CONFIRM_WINDOW_MS = 550L;
    private long pendingEnergyUntilMs = -1L;
    private final int[] commandDirections = new int[COMMAND_BUFFER_SIZE];
    private final long[] commandTimes = new long[COMMAND_BUFFER_SIZE];
    private int commandCount = 0;

    private final ConcurrentLinkedQueue<MotionEvent> pendingInput =
        new ConcurrentLinkedQueue<>();
    private volatile boolean surfaceReady;
    private volatile boolean activityActive = true;
    private float accumulatedTime;
    private static final float FIXED_STEP = 1f/120f;
    private void clearInput() {
        clearPendingInput();
        clearDpad();
        lightPointer=mediumPointer=heavyPointer=comboPointer=tagPointer=superPointer=-1;
        pendingEnergyUntilMs=-1L;
        backDashTimer=0;
        lastDownInputMs=lastForwardTapMs=lastBackTapMs=-1000L;
        resetCommandBuffer();
        resetAutoCombo();
    }

    public GameView(Context context) {
        super(context);
        spriteFighterRenderer = new SpriteFighterRenderer(context);
        holder = getHolder();
        holder.addCallback(this);
        setFocusable(true);
        setKeepScreenOn(true);

        skyGradient = new LinearGradient(
            0f,
            WORLD_TOP,
            0f,
            VH,
            Color.rgb(21, 55, 103),
            Color.rgb(240, 171, 99),
            Shader.TileMode.CLAMP
        );
        buildScenarioGeometry();
    }

    private void buildScenarioGeometry() {
        for (int i = 0; i < mountainPaths.length; i++) {
            float x = -100f + i * 190f;
            float h = 105f + (i % 5) * 24f;

            android.graphics.Path path = new android.graphics.Path();
            path.moveTo(x, GROUND_Y);
            path.lineTo(x + 115f, GROUND_Y - h);
            path.lineTo(x + 245f, GROUND_Y);
            path.close();
            mountainPaths[i] = path;
        }
    }

    private FighterState activeFighter() {
        return team[activeFighterIndex];
    }

    private FighterState reserveFighter() {
        return team[(activeFighterIndex + 1) % team.length];
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

    // Render cadence can vary; movement and attack timing always use the same steps.
    private void advanceSimulation(float elapsedSeconds) {
        processPendingInput();
        accumulatedTime += clamp(elapsedSeconds, 0f, 0.10f);
        while (accumulatedTime + 0.000001f >= FIXED_STEP) {
            update(FIXED_STEP);
            accumulatedTime = Math.max(0f, accumulatedTime - FIXED_STEP);
        }
    }

    private void update(float dt) {
        float previousX = playerX;
        updateSuperState(dt);
        updateTagState(dt);

        if (dummyDamageLabelTimer > 0f) {
            dummyDamageLabelTimer = Math.max(0f, dummyDamageLabelTimer - dt);
        }
        if (playerDamageFlashTimer > 0f) {
            playerDamageFlashTimer = Math.max(0f, playerDamageFlashTimer - dt);
        }

        updateDummyHitReaction(dt);
        updateDummyKnockdown(dt);
        updateDummyAirState(dt);
        updatePlayerReceivedState(dt);
        updateOpponentAi(dt);

        if (isSuperCinematicActive()) {
            updateSuperProjectiles(dt);
            updateSpriteMotion(dt,0f);
            return;
        }

        if (attackTimer > 0f) {
            attackTimer = Math.max(0f, attackTimer - dt);
            tryApplyMeleeDamage();

            if (attackTimer <= 0f) {
                attackType = "";
                attackHitApplied = false;
            }
        }

        updateEnergyProjectiles(dt);
        updateSuperProjectiles(dt);

        int anticipatedGuard = anticipatedPlayerGuardPose();

        float direction = 0f;
        if (
            anticipatedGuard == GUARD_NONE &&
            !playerMovementLocked &&
            !isEnergyAttackActive() &&
            !isTagAnimationActive()
        ) {
            if (movingLeft && !movingRight) direction = -1f;
            if (movingRight && !movingLeft) direction = 1f;
        }

        if (playerHitReactionTimer > 0f && grounded) {
            playerX += playerKnockbackVelocityX * dt;
            playerKnockbackVelocityX *= (float)Math.pow(0.035f, dt);
            walkTime = 0f;
        } else if (isTagAnimationActive()) {
            walkTime = 0f;
        } else if (attackTimer > 0f && grounded) {
            // No chão, ataques travam o deslocamento horizontal.
            walkTime = 0f;
        } else if (backDashTimer > 0f && grounded) {
            playerX -= facingDirection * backDashSpeed * dt;
            backDashTimer = Math.max(0f, backDashTimer - dt);
            walkTime += dt * 13f;
        } else if (direction != 0f && !crouching) {
            boolean movingForward = direction == facingDirection;
            float speed = (
                forwardDashing &&
                movingForward &&
                grounded
            ) ? forwardDashSpeed : moveSpeed;
            playerX += direction * speed * dt;
            walkTime += dt * (forwardDashing ? 14f : 8f);
        } else {
            walkTime = 0f;
        }

        if (!grounded) {
            float activeGravity =
                isEnergyAttackActive()
                    ? gravity * 0.12f
                    : gravity;

            velocityY += activeGravity * dt;
            playerY += velocityY * dt;
            if (playerY >= GROUND_Y) {
                playerY = GROUND_Y;
                velocityY = 0f;
                grounded = true;
                superJumping = false;
                crouching = isDownDirection(dpadDirection);

                if (playerGroundSlam) {
                    playerGroundSlam = false;
                    playerMovementLocked = false;
                }
                if (playerLaunchedByHit) {
                    playerLaunchedByHit = false;
                    playerMovementLocked = false;
                }
            } else if (playerLaunchedByHit && velocityY >= 0f) {
                // Mesmo comportamento do launcher do dummy: recupera controle na descida.
                playerMovementLocked = false;
            }
        }

        playerX = clamp(playerX, LEFT_BOUND, RIGHT_BOUND);
        updateFacing();

        updateFightCamera(dt);
        updateSpriteMotion(dt,playerX-previousX);
    }

    private FighterProfile opponentProfile() {
        return opponentFighter.profile;
    }

    private int opponentFacingDirection() {
        return -facingDirection;
    }

    private float durationForAttack(String type) {
        if ("L".equals(type) || "2L".equals(type)) return 0.16f;
        if ("M".equals(type) || "2M".equals(type)) return 0.26f;
        if ("S".equals(type)) return 0.30f;
        return 0.40f;
    }

    private int damageForAttack(FighterProfile profile, String type) {
        if ("L".equals(type) || "2L".equals(type)) return profile.lightDamage;
        if ("M".equals(type) || "2M".equals(type)) return profile.mediumDamage;
        if ("H".equals(type) || "2H".equals(type)) return profile.heavyDamage;
        return 0;
    }

    private float aiAttackPhase() {
        if (dummyAttackTimer <= 0f || dummyAttackDuration <= 0f) return 0f;
        float t = 1f - dummyAttackTimer / dummyAttackDuration;
        return t < 0.5f ? t * 2f : (1f - t) * 2f;
    }

    private boolean canOpponentAct() {
        return
            opponentAiEnabled &&
            dummyLife > 0 &&
            activeFighter().life > 0 &&
            dummyKnockdownState == DUMMY_KD_NONE &&
            !dummyMovementLocked &&
            dummyHitReactionTimer <= 0f &&
            aiSuperTimer <= 0f &&
            !isTagAnimationActive() &&
            !isSuperCinematicActive();
    }

    private void setOpponentAiEnabled(boolean enabled) {
        opponentAiEnabled = enabled;
        aiStatusLabel = enabled ? "IA ON" : "IA OFF";
        aiDecisionTimer = 0f;
        aiAttackCooldownRemaining = 0f;
        aiMovingForward = false;
        aiMovingBack = false;
        aiForwardDashing = false;
        aiBackDashTimer = 0f;
        aiCrouching = false;
        aiChaseLauncher = false;
        dummyAttackType = "";
        dummyAttackTimer = 0f;
        dummyAttackDuration = 0f;
        dummyAttackHitApplied = false;
        aiSuperTimer = 0f;
    }

    private void startOpponentAttack(String type) {
        if (!canOpponentAct() || dummyAttackTimer > 0f) return;

        dummyAttackType = type;
        dummyAttackDuration = durationForAttack(type);
        dummyAttackTimer = dummyAttackDuration;
        dummyAttackHitApplied = false;
        aiMovingForward = false;
        aiMovingBack = false;
        aiForwardDashing = false;
        aiBackDashTimer = 0f;
        aiCrouching = type.startsWith("2");

    }

    private void startOpponentJump(boolean superJump) {
        if (!canOpponentAct() || dummyAirborne) return;

        dummyAirborne = true;
        dummyLaunchedByHit = false;
        dummyMovementLocked = false;
        dummyGroundSlam = false;
        aiSuperJumping = superJump;
        dummyVelocityY = superJump ? -superJumpSpeed : -jumpSpeed;
        dummyY -= 2f;
        aiCrouching = false;
    }

    private void fireOpponentEnergy(String strength) {
        FighterProfile profile = opponentProfile();
        if (
            !canOpponentAct() ||
            !profile.hasEnergyAttack ||
            hasActiveEnergyProjectile(AI_OWNER_INDEX)
        ) return;

        float speedMultiplier;
        float damageMultiplier;
        if ("L".equals(strength)) {
            speedMultiplier = 0.65f;
            damageMultiplier = 0.60f;
        } else if ("H".equals(strength)) {
            speedMultiplier = 1.35f;
            damageMultiplier = 1.45f;
        } else {
            speedMultiplier = 1f;
            damageMultiplier = 1f;
        }

        dummyAttackType = "S";
        dummyAttackDuration = durationForAttack("S");
        dummyAttackTimer = dummyAttackDuration;
        dummyAttackHitApplied = true;

        if (dummyAirborne) dummyVelocityY *= 0.32f;

        int direction = opponentFacingDirection();
        energyProjectiles.add(new EnergyProjectile(
            dummyX + direction * 62f,
            dummyY - (aiCrouching ? 65f : 82f),
            profile.energyRange,
            profile.energySpeed * speedMultiplier,
            Math.round(profile.energyDamage * damageMultiplier),
            profile.color,
            AI_OWNER_INDEX,
            direction
        ));
        aiAttackCooldownRemaining = 0.40f;
    }

    private void startOpponentSuper() {
        FighterProfile profile = opponentProfile();
        if (
            !canOpponentAct() ||
            !profile.hasSuperAttack ||
            opponentFighter.superMeter < SUPER_COST
        ) return;

        opponentFighter.superMeter = Math.max(
            0f,
            opponentFighter.superMeter - SUPER_COST
        );
        opponentFighter.refreshHudLabels();
        aiSuperTimer = 0.58f;
        aiMovingForward = false;
        aiMovingBack = false;
        aiForwardDashing = false;
        aiBackDashTimer = 0f;
        dummyAttackType = "";
        dummyAttackTimer = 0f;
    }

    private void spawnOpponentSuperProjectile() {
        FighterProfile profile = opponentProfile();
        int direction = opponentFacingDirection();
        superProjectiles.add(new SuperProjectile(
            dummyX + direction * 78f,
            dummyY - 86f,
            profile.superRange,
            profile.superSpeed,
            profile.superDamage,
            profile.color,
            AI_OWNER_INDEX,
            direction
        ));
    }

    private void updateOpponentAi(float dt) {
        if (aiAttackCooldownRemaining > 0f) {
            aiAttackCooldownRemaining = Math.max(
                0f,
                aiAttackCooldownRemaining - dt
            );
        }

        if (!opponentAiEnabled) return;

        if (aiSuperTimer > 0f) {
            float previous = aiSuperTimer;
            aiSuperTimer = Math.max(0f, aiSuperTimer - dt);
            if (previous > 0f && aiSuperTimer <= 0f) {
                spawnOpponentSuperProjectile();
                aiAttackCooldownRemaining = 0.45f;
            }
            return;
        }

        if (!canOpponentAct()) {
            aiMovingForward = false;
            aiMovingBack = false;
            aiForwardDashing = false;
            return;
        }

        if (dummyAttackTimer > 0f) {
            dummyAttackTimer = Math.max(0f, dummyAttackTimer - dt);
            tryApplyOpponentMeleeDamage();

            if (dummyAttackTimer <= 0f) {
                dummyAttackType = "";
                dummyAttackDuration = 0f;
                dummyAttackHitApplied = false;
                aiCrouching = false;
                aiAttackCooldownRemaining = AI_ATTACK_COOLDOWN;

                if (aiChaseLauncher && !dummyAirborne) {
                    aiChaseLauncher = false;
                    startOpponentJump(true);
                }
            }
            return;
        }

        float distance = Math.abs(playerX - dummyX);
        float verticalDistance = Math.abs(playerY - dummyY);

        if (aiBackDashTimer > 0f && !dummyAirborne) {
            dummyX -= opponentFacingDirection() * AI_BACKDASH_SPEED * dt;
            aiBackDashTimer = Math.max(0f, aiBackDashTimer - dt);
            aiWalkTime += dt * 13f;
            return;
        }

        aiDecisionTimer -= dt;
        if (aiDecisionTimer <= 0f) {
            aiDecisionTimer = AI_DECISION_MIN +
                (float)Math.random() * (AI_DECISION_MAX - AI_DECISION_MIN);

            aiMovingForward = false;
            aiMovingBack = false;
            aiForwardDashing = false;
            aiCrouching = false;

            if (dummyAirborne) {
                if (verticalDistance <= 115f && distance <= 175f) {
                    double r = Math.random();
                    if (r < 0.30) startOpponentAttack("L");
                    else if (r < 0.64) startOpponentAttack("M");
                    else startOpponentAttack("H");
                } else {
                    aiMovingForward = distance > 95f;
                }
            } else if (activeFighter().life <= 0) {
                // sem ação
            } else if (
                opponentFighter.superMeter >= SUPER_COST &&
                distance >= 250f &&
                distance <= 720f &&
                Math.random() < 0.18
            ) {
                startOpponentSuper();
            } else if (
                distance >= 230f &&
                distance <= 620f &&
                !hasActiveEnergyProjectile(AI_OWNER_INDEX) &&
                Math.random() < 0.24
            ) {
                double r = Math.random();
                fireOpponentEnergy(r < 0.33 ? "L" : (r < 0.72 ? "M" : "H"));
            } else if (!grounded && distance <= 185f && Math.random() < 0.38) {
                startOpponentAttack("2H");
            } else if (distance <= 178f && aiAttackCooldownRemaining <= 0f) {
                double r = Math.random();
                if (r < 0.18) startOpponentAttack("2L");
                else if (r < 0.36) startOpponentAttack("2M");
                else if (r < 0.53) startOpponentAttack("2H");
                else if (r < 0.70) startOpponentAttack("L");
                else if (r < 0.86) startOpponentAttack("M");
                else startOpponentAttack("H");
            } else if (distance < 82f && Math.random() < 0.38) {
                aiBackDashTimer = AI_BACKDASH_DURATION;
            } else if (distance > 360f && Math.random() < 0.38) {
                aiMovingForward = true;
                aiForwardDashing = true;
            } else if (distance > 145f) {
                aiMovingForward = true;
            } else if (Math.random() < 0.12) {
                startOpponentJump(Math.random() < 0.32);
            }
        }

        if (dummyAirborne) {
            if (aiMovingForward) {
                dummyX += opponentFacingDirection() * AI_WALK_SPEED * 0.70f * dt;
                aiWalkTime += dt * 8f;
            }
        } else if (aiMovingForward) {
            float speed = aiForwardDashing ? AI_DASH_SPEED : AI_WALK_SPEED;
            dummyX += opponentFacingDirection() * speed * dt;
            aiWalkTime += dt * (aiForwardDashing ? 14f : 8f);
        } else if (aiMovingBack) {
            dummyX -= opponentFacingDirection() * AI_WALK_SPEED * dt;
            aiWalkTime += dt * 8f;
        }

        dummyX = clamp(dummyX, LEFT_BOUND, RIGHT_BOUND);
    }

    private void tryApplyOpponentMeleeDamage() {
        if (
            dummyAttackHitApplied ||
            dummyAttackTimer <= 0f ||
            activeFighter().life <= 0 ||
            "S".equals(dummyAttackType)
        ) return;

        int damage = damageForAttack(opponentProfile(), dummyAttackType);
        float reach = reachForAttack(dummyAttackType);
        if (damage <= 0 || reach <= 0f) return;
        if (aiAttackPhase() < 0.72f) return;

        int direction = opponentFacingDirection();
        float horizontalDistance = (playerX - dummyX) * direction;
        if (horizontalDistance < 18f || horizontalDistance > reach) return;

        float opponentCenterY =
            (aiCrouching || dummyAttackType.startsWith("2"))
                ? dummyY - 42f
                : dummyY - 78f;
        float playerCenterY =
            (crouching || isCrouchAttackActive())
                ? playerY - 42f
                : playerY - 78f;
        if (Math.abs(opponentCenterY - playerCenterY) > 92f) return;

        applyPlayerHit(
            damage,
            direction,
            dummyAttackType,
            dummyAirborne,
            aiSuperJumping
        );
        dummyAttackHitApplied = true;

        if ("2H".equals(dummyAttackType)) {
            aiChaseLauncher = true;
        }
    }

    private float dummyTop() {
        return dummyY - 145f;
    }

    private void launchDummy() {
        dummyKnockdownState = DUMMY_KD_NONE;
        dummyKnockdownTimer = 0f;
        dummyAirborne = true;
        dummyMovementLocked = true;
        dummyLaunchedByHit = true;
        dummyGroundSlam = false;
        aiSuperJumping = false;
        dummyVelocityY = -DUMMY_LAUNCH_SPEED;
    }

    private void slamDummyToGround() {
        if (!dummyAirborne) return;

        dummyGroundSlam = true;
        dummyMovementLocked = true;
        dummyVelocityY = DUMMY_SLAM_SPEED;

        // O H do Super Jump prioriza a queda vertical sobre o empurrão lateral.
        dummyKnockbackVelocityX *= 0.35f;
    }

    private void knockDownDummy() {
        dummyAirborne = false;
        dummyLaunchedByHit = false;
        dummyGroundSlam = false;
        dummyVelocityY = 0f;
        dummyY = GROUND_Y;
        dummyMovementLocked = true;
        dummyKnockdownState = DUMMY_KD_FALL;
        dummyKnockdownTimer = 0f;
        dummyKnockbackVelocityX *= 0.55f;
    }

    private void updateDummyKnockdown(float dt) {
        if (dummyKnockdownState == DUMMY_KD_NONE) return;

        dummyMovementLocked = true;
        dummyKnockdownTimer += dt;

        if (
            dummyKnockdownState == DUMMY_KD_FALL &&
            dummyKnockdownTimer >= DUMMY_KD_FALL_DURATION
        ) {
            dummyKnockdownState = DUMMY_KD_DOWN;
            dummyKnockdownTimer = 0f;
        } else if (
            dummyKnockdownState == DUMMY_KD_DOWN &&
            dummyKnockdownTimer >= DUMMY_KD_DOWN_DURATION
        ) {
            dummyKnockdownState = DUMMY_KD_GETUP;
            dummyKnockdownTimer = 0f;
        } else if (
            dummyKnockdownState == DUMMY_KD_GETUP &&
            dummyKnockdownTimer >= DUMMY_KD_GETUP_DURATION
        ) {
            dummyKnockdownState = DUMMY_KD_NONE;
            dummyKnockdownTimer = 0f;
            dummyMovementLocked = false;
        }
    }

    private void updateDummyAirState(float dt) {
        if (
            dummyKnockdownState != DUMMY_KD_NONE ||
            !dummyAirborne
        ) return;

        dummyVelocityY += DUMMY_GRAVITY * dt;
        dummyY += dummyVelocityY * dt;

        // Launcher normal: sem controle na subida e recupera na descida.
        // Ground Slam: queda forçada e sem controle até tocar o chão.
        dummyMovementLocked =
            dummyGroundSlam ||
            (dummyLaunchedByHit && dummyVelocityY < 0f);

        if (dummyY >= GROUND_Y) {
            dummyY = GROUND_Y;
            dummyVelocityY = 0f;
            dummyAirborne = false;
            dummyMovementLocked = false;
            dummyLaunchedByHit = false;
            dummyGroundSlam = false;
            aiSuperJumping = false;
        }
    }

    private void updateDummyHitReaction(float dt) {
        if (dummyHitReactionTimer <= 0f) {
            dummyHitReactionTimer = 0f;
            dummyKnockbackVelocityX = 0f;
            return;
        }

        dummyHitReactionTimer = Math.max(
            0f,
            dummyHitReactionTimer - dt
        );
        dummyX += dummyKnockbackVelocityX * dt;

        // Amortece rapidamente para o recuo ser curto e controlado.
        dummyKnockbackVelocityX *= (float)Math.pow(0.035f, dt);
        dummyX = clamp(
            dummyX,
            LEFT_BOUND + 130f,
            RIGHT_BOUND - 130f
        );

        if (dummyHitReactionTimer <= 0f) {
            dummyKnockbackVelocityX = 0f;
        }
    }

    private void updateFacing() {
        int nextFacing = facingDirection;

        if (playerX < dummyX - FACING_SWITCH_EPSILON) {
            nextFacing = 1;
        } else if (playerX > dummyX + FACING_SWITCH_EPSILON) {
            nextFacing = -1;
        }

        if (nextFacing == facingDirection) return;

        facingDirection = nextFacing;

        // Evita que um comando iniciado de um lado termine do outro lado.
        resetCommandBuffer();
        pendingEnergyUntilMs = -1L;
        forwardDashing = false;
        backDashTimer = 0f;
        lastForwardTapMs = -1000L;
        lastBackTapMs = -1000L;
        lastDownInputMs = -1000L;
    }

    private int relativeDirection(int screenDirection) {
        if (facingDirection > 0) return screenDirection;

        switch (screenDirection) {
            case 1: return 5;
            case 2: return 4;
            case 4: return 2;
            case 5: return 1;
            case 6: return 8;
            case 8: return 6;
            default: return screenDirection;
        }
    }

    private boolean isForwardHorizontalDirection(int screenDirection) {
        return facingDirection > 0
            ? screenDirection == 1
            : screenDirection == 5;
    }

    private boolean isBackHorizontalDirection(int screenDirection) {
        return facingDirection > 0
            ? screenDirection == 5
            : screenDirection == 1;
    }

    private boolean isSuperCinematicActive() {
        return superPhase != SUPER_IDLE;
    }

    private boolean isSuperPoseActive() {
        return superPhase == SUPER_POSE ||
            superPhase == SUPER_FLASH ||
            superPhase == SUPER_RELEASE;
    }

    private boolean canStartSuper() {
        return !playerMovementLocked &&
            activeFighter().profile.hasSuperAttack &&
            activeFighter().superMeter >= SUPER_COST &&
            !isSuperCinematicActive() &&
            !isTagAnimationActive() &&
            attackTimer <= 0f;
    }

    private void startSuperCinematic() {
        if (!canStartSuper()) return;

        activeFighter().superMeter = Math.max(
            0f,
            activeFighter().superMeter - SUPER_COST
        );
        activeFighter().refreshHudLabels();

        superPhase = SUPER_DARKEN;
        superPhaseTimer = 0f;
        superCameraZoom = 1f;
        superDarkAlpha = 0;
        superFlashAlpha = 0;
        superStoredVelocityY = velocityY;
        launcherChaseUntilMs = -1L;

        attackType = "";
        attackTimer = 0f;
        attackDuration = 0f;
        forwardDashing = false;
        backDashTimer = 0f;
        resetAutoCombo();
        resetCommandBuffer();
        pendingEnergyUntilMs = -1L;
    }

    private void updateSuperState(float dt) {
        if (superPhase == SUPER_IDLE) return;

        superPhaseTimer += dt;

        if (superPhase == SUPER_DARKEN) {
            float t = clamp(superPhaseTimer / SUPER_DARKEN_DURATION, 0f, 1f);
            superDarkAlpha = Math.round(190f * t);
            superCameraZoom = 1f + 0.18f * t;

            if (t >= 1f) {
                superPhase = SUPER_POSE;
                superPhaseTimer = 0f;
            }
        } else if (superPhase == SUPER_POSE) {
            float t = clamp(superPhaseTimer / SUPER_POSE_DURATION, 0f, 1f);
            superDarkAlpha = 190;
            superCameraZoom = 1.18f + 0.18f * t;

            if (t >= 1f) {
                superPhase = SUPER_FLASH;
                superPhaseTimer = 0f;
            }
        } else if (superPhase == SUPER_FLASH) {
            float t = clamp(superPhaseTimer / SUPER_FLASH_DURATION, 0f, 1f);
            superDarkAlpha = 190;
            superCameraZoom = 1.36f;
            superFlashAlpha = Math.round(235f * (1f - Math.abs(0.5f - t) * 2f));

            if (t >= 1f) {
                spawnSuperProjectile();
                superPhase = SUPER_RELEASE;
                superPhaseTimer = 0f;
                superFlashAlpha = 255;
            }
        } else if (superPhase == SUPER_RELEASE) {
            float t = clamp(superPhaseTimer / SUPER_RELEASE_DURATION, 0f, 1f);
            superDarkAlpha = Math.round(190f * (1f - 0.40f * t));
            superFlashAlpha = Math.round(255f * (1f - t));
            superCameraZoom = 1.36f - 0.16f * t;

            if (t >= 1f) {
                superPhase = SUPER_RECOVER;
                superPhaseTimer = 0f;
                superFlashAlpha = 0;
            }
        } else if (superPhase == SUPER_RECOVER) {
            float t = clamp(superPhaseTimer / SUPER_RECOVER_DURATION, 0f, 1f);
            superDarkAlpha = Math.round(114f * (1f - t));
            superCameraZoom = 1.20f - 0.20f * t;

            if (t >= 1f) {
                superPhase = SUPER_IDLE;
                superPhaseTimer = 0f;
                superCameraZoom = 1f;
                superDarkAlpha = 0;
                superFlashAlpha = 0;
                velocityY = superStoredVelocityY;
            }
        }
    }

    private void spawnSuperProjectile() {
        FighterProfile profile = activeFighter().profile;
        if (!profile.hasSuperAttack) return;

        float spawnY = playerY - (grounded ? 86f : 82f);
        superProjectiles.add(new SuperProjectile(
            playerX + facingDirection * 78f,
            spawnY,
            profile.superRange,
            profile.superSpeed,
            profile.superDamage,
            profile.color,
            activeFighterIndex,
            facingDirection
        ));
    }

    private void updateSuperProjectiles(float dt) {
        Iterator<SuperProjectile> iterator = superProjectiles.iterator();
        while (iterator.hasNext()) {
            SuperProjectile projectile = iterator.next();
            float previousX = projectile.x;
            projectile.x += projectile.speed * projectile.direction * dt;

            boolean hit = false;
            if (projectile.ownerIndex == AI_OWNER_INDEX) {
                if (projectileHitsPlayer(
                    previousX,
                    projectile.x,
                    projectile.y,
                    58f
                )) {
                    applyPlayerHit(
                        projectile.damage,
                        projectile.direction,
                        "SUPER",
                        false,
                        false
                    );
                    hit = true;
                }
            } else if (
                projectileHitsDummy(
                    previousX,
                    projectile.x,
                    projectile.y,
                    58f
                )
            ) {
                applyDummyDamage(projectile.damage, projectile.direction);
                hit = true;
            }

            if (hit) {
                iterator.remove();
                continue;
            }

            if (
                Math.abs(projectile.x - projectile.startX) >= projectile.range ||
                projectile.x > RIGHT_BOUND + 180f ||
                projectile.x < LEFT_BOUND - 180f
            ) {
                iterator.remove();
            }
        }
    }

    private boolean isEnergyAttackActive() {
        return "S".equals(attackType) && attackTimer > 0f;
    }

    private boolean isCrouchAttackActive() {
        return attackTimer > 0f && (
            "2L".equals(attackType) ||
            "2M".equals(attackType) ||
            "2H".equals(attackType)
        );
    }

    private boolean isTagAnimationActive() {
        return tagPhase != TAG_IDLE;
    }

    private boolean isTagPoseActive() {
        return tagPhase == TAG_POSE;
    }

    private boolean canStartTag() {
        return
            !playerMovementLocked &&
            !isTagAnimationActive() &&
            !isSuperCinematicActive() &&
            !isEnergyAttackActive() &&
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
            tagCooldownHudLabel = String.format(
                java.util.Locale.US,
                "TROCA: %.1fs",
                tenths / 10f
            );
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
            float eased = t * t;
            tagVisualOffsetX =
                tagExitDirection * TAG_TRAVEL_DISTANCE * eased;

            if (t >= 1f) {
                activeFighterIndex = (activeFighterIndex + 1) % team.length;
                spriteFighterRenderer.setCharacter(GeneratedCharacters.TEAM[activeFighterIndex]);
                tagPhase = TAG_ENTER;
                tagPhaseTimer = 0f;
                tagVisualOffsetX =
                    tagExitDirection * TAG_TRAVEL_DISTANCE;
            }
        } else if (tagPhase == TAG_ENTER) {
            float t = clamp(tagPhaseTimer / TAG_ENTER_DURATION, 0f, 1f);
            float eased = 1f - (1f - t) * (1f - t);
            tagVisualOffsetX =
                tagExitDirection * TAG_TRAVEL_DISTANCE * (1f - eased);

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
                tagVisualOffsetX = 0f;
                tagCooldownRemaining = TAG_COOLDOWN_SECONDS;
                restoreHeldDpadAfterTag();
            }
        }
    }

    private void restoreHeldDpadAfterTag() {
        if (dpadPointer == -1 || dpadDirection == 0) {
            movingLeft = false;
            movingRight = false;
            crouching = false;
            forwardDashing = false;
            return;
        }

        movingLeft =
            dpadDirection == 4 ||
            dpadDirection == 5 ||
            dpadDirection == 6;
        movingRight =
            dpadDirection == 1 ||
            dpadDirection == 2 ||
            dpadDirection == 8;
        crouching = grounded && isDownDirection(dpadDirection);
        forwardDashing = false;
    }

    private void startTagAnimation() {
        if (!canStartTag()) return;

        launcherChaseUntilMs = -1L;
        tagPhase = TAG_EXIT;
        tagPhaseTimer = 0f;
        tagVisualOffsetX = 0f;
        tagExitDirection = -facingDirection;
        updateTagCooldownLabels();

        attackType = "";
        attackTimer = 0f;
        attackDuration = 0f;
        forwardDashing = false;
        backDashTimer = 0f;
        // Congela a ação durante a troca, mas preserva ponteiro/direção do D-pad.
        // Assim, se o jogador continuar segurando, o novo personagem retoma ao final.
        movingLeft = false;
        movingRight = false;
        crouching = false;
        forwardDashing = false;
        resetAutoCombo();
        resetCommandBuffer();
        pendingEnergyUntilMs = -1L;
    }

    private void startJump(boolean superJump) {
        if (
            !grounded ||
            playerMovementLocked ||
            isEnergyAttackActive() ||
            isTagAnimationActive() ||
            isSuperCinematicActive()
        ) return;

        grounded = false;
        crouching = false;
        superJumping = superJump;
        velocityY = superJump ? -superJumpSpeed : -jumpSpeed;
        playerY -= 2f;
    }

    private void addSuperMeter(FighterState fighter, float amount) {
        if (amount <= 0f) return;
        fighter.superMeter = clamp(
            fighter.superMeter + amount,
            0f,
            MAX_SUPER_METER
        );
        fighter.refreshHudLabels();
    }

    private float superGainForAttack(String type) {
        if ("L".equals(type) || "2L".equals(type)) return SUPER_GAIN_LIGHT;
        if ("M".equals(type) || "2M".equals(type)) return SUPER_GAIN_MEDIUM;
        if ("H".equals(type) || "2H".equals(type)) return SUPER_GAIN_HEAVY;
        if ("S".equals(type)) return SUPER_GAIN_ENERGY;
        return 0f;
    }

    private float superGainForGuard(String type) {
        if ("L".equals(type) || "2L".equals(type)) return SUPER_GUARD_GAIN_LIGHT;
        if ("M".equals(type) || "2M".equals(type)) return SUPER_GUARD_GAIN_MEDIUM;
        if ("H".equals(type) || "2H".equals(type)) return SUPER_GUARD_GAIN_HEAVY;
        if ("S".equals(type)) return SUPER_GUARD_GAIN_ENERGY;
        if ("SUPER".equals(type)) return SUPER_GUARD_GAIN_SUPER;
        return 0f;
    }

    private void startAttack(String type) {
        if (playerMovementLocked) return;

        attackType = type;
        attackHitApplied = false;
        forwardDashing = false;
        backDashTimer = 0f;

        if ("L".equals(type) || "2L".equals(type)) attackDuration = 0.16f;
        else if ("M".equals(type) || "2M".equals(type)) attackDuration = 0.26f;
        else if ("S".equals(type)) attackDuration = 0.30f;
        else attackDuration = 0.40f;

        activeSpriteMove = grounded && !crouching
            ? GeneratedCharacters.get(GeneratedCharacters.TEAM[activeFighterIndex]).moves.get(type)
            : null;
        if (activeSpriteMove != null) attackDuration = activeSpriteMove.animation.duration;
        attackTimer = attackDuration;

    }

    private int damageForAttack(String type) {
        return damageForAttack(activeFighter().profile, type);
    }

    private float reachForAttack(String type) {
        if ("L".equals(type) || "2L".equals(type)) return 118f;
        if ("M".equals(type) || "2M".equals(type)) return 150f;
        if ("H".equals(type) || "2H".equals(type)) return 182f;
        return 0f;
    }

    private void tryApplyMeleeDamage() {
        if (attackHitApplied || attackTimer <= 0f || dummyLife <= 0) return;
        if ("S".equals(attackType)) return;

        int damage = activeSpriteMove != null ? activeSpriteMove.damage : damageForAttack(attackType);
        float reach = activeSpriteMove != null ? activeSpriteMove.reach : reachForAttack(attackType);
        if (damage <= 0 || reach <= 0f) return;
        if (activeSpriteMove != null) {
            if (!activeSpriteMove.active(attackDuration - attackTimer)) return;
        } else if (attackPhase() < 0.72f) return;

        float horizontalDistance =
            (dummyX - playerX) * facingDirection;
        if (horizontalDistance < 18f || horizontalDistance > reach) return;

        float playerAttackCenterY = (crouching || isCrouchAttackActive())
            ? playerY - 42f
            : playerY - 78f;
        float dummyCenterY = (dummyTop() + dummyY) * 0.5f;
        if (Math.abs(playerAttackCenterY - dummyCenterY) > 92f) return;

        addSuperMeter(activeFighter(), superGainForAttack(attackType));
        applyDummyDamage(damage, facingDirection);

        if ("2M".equals(attackType)) {
            knockDownDummy();
        } else if ("2H".equals(attackType)) {
            launchDummy();
            launcherChaseUntilMs =
                SystemClock.uptimeMillis() + LAUNCHER_CHASE_WINDOW_MS;
        } else if (
            "H".equals(attackType) &&
            !grounded &&
            superJumping &&
            dummyAirborne
        ) {
            slamDummyToGround();
        }

        attackHitApplied = true;
    }

    private void applyDummyDamage(int damage, int hitDirection) {
        if (damage <= 0 || dummyLife <= 0) return;

        dummyAttackTimer = 0f;
        dummyAttackType = "";
        dummyAttackHitApplied = false;
        aiAttackCooldownRemaining = Math.max(
            aiAttackCooldownRemaining,
            0.35f
        );

        int applied = Math.min(damage, dummyLife);
        dummyLife -= applied;
        opponentFighter.life = dummyLife;
        opponentFighter.refreshHudLabels();
        dummyLifeHudLabel = dummyLife + " / " + DUMMY_MAX_LIFE;
        dummyDamageLabel = "-" + applied;
        dummyDamageLabelTimer = 0.72f;

        int direction = hitDirection >= 0 ? 1 : -1;
        float knockbackSpeed = clamp(
            120f + applied * 0.025f,
            135f,
            210f
        );
        dummyKnockbackVelocityX = direction * knockbackSpeed;
        dummyHitReactionTimer = DUMMY_HIT_REACTION_DURATION;
    }

    private boolean projectileHitsPlayer(
        float previousX,
        float nextX,
        float y,
        float radius
    ) {
        if (activeFighter().life <= 0) return false;

        float halfWidth = 34f;
        float playerLeft = playerX - halfWidth;
        float playerRight = playerX + halfWidth;
        float projectileLeft = Math.min(previousX, nextX) - radius;
        float projectileRight = Math.max(previousX, nextX) + radius;

        boolean horizontalHit =
            projectileRight >= playerLeft &&
            projectileLeft <= playerRight;

        float bodyHeight =
            (crouching || isCrouchAttackActive()) ? 90f : 145f;
        float playerTop = playerY - bodyHeight;
        boolean verticalHit =
            y + radius >= playerTop &&
            y - radius <= playerY;

        return horizontalHit && verticalHit;
    }

    private boolean projectileHitsDummy(
        float previousX,
        float nextX,
        float y,
        float radius
    ) {
        if (dummyLife <= 0) return false;

        float dummyLeft = dummyX - DUMMY_HALF_WIDTH;
        float dummyRight = dummyX + DUMMY_HALF_WIDTH;
        float projectileLeft = Math.min(previousX, nextX) - radius;
        float projectileRight = Math.max(previousX, nextX) + radius;

        boolean horizontalHit =
            projectileRight >= dummyLeft &&
            projectileLeft <= dummyRight;
        boolean verticalHit =
            y + radius >= dummyTop() &&
            y - radius <= dummyY;

        return horizontalHit && verticalHit;
    }

    private boolean hasActiveEnergyProjectile(int ownerIndex) {
        for (EnergyProjectile projectile : energyProjectiles) {
            if (projectile.ownerIndex == ownerIndex) return true;
        }
        return false;
    }

    private void fireEnergyAttack(String strength) {
        FighterProfile profile = activeFighter().profile;
        if (!profile.hasEnergyAttack) return;
        if (hasActiveEnergyProjectile(activeFighterIndex)) return;

        float speedMultiplier;
        float damageMultiplier;

        if ("L".equals(strength)) {
            speedMultiplier = 0.65f;
            damageMultiplier = 0.60f;
        } else if ("H".equals(strength)) {
            speedMultiplier = 1.35f;
            damageMultiplier = 1.45f;
        } else {
            speedMultiplier = 1.00f;
            damageMultiplier = 1.00f;
        }

        resetAutoCombo();

        if (!grounded) {
            // Durante o especial aéreo o deslocamento horizontal trava.
            // A componente vertical continua com inércia amortecida e gravidade reduzida.
            velocityY *= 0.32f;
        }

        startAttack("S");

        float spawnY = playerY - (crouching ? 65f : 82f);
        energyProjectiles.add(new EnergyProjectile(
            playerX + facingDirection * 62f,
            spawnY,
            profile.energyRange,
            profile.energySpeed * speedMultiplier,
            Math.round(profile.energyDamage * damageMultiplier),
            profile.color,
            activeFighterIndex,
            facingDirection
        ));

    }

    private boolean tryFirePendingEnergy(String attackButton, long nowMs) {
        FighterProfile profile = activeFighter().profile;

        if (
            playerMovementLocked ||
            !profile.hasEnergyAttack ||
            hasActiveEnergyProjectile(activeFighterIndex) ||
            pendingEnergyUntilMs < nowMs
        ) {
            pendingEnergyUntilMs = -1L;
            return false;
        }

        fireEnergyAttack(attackButton);
        pendingEnergyUntilMs = -1L;
        resetCommandBuffer();
        return true;
    }

    private void updateEnergyProjectiles(float dt) {
        Iterator<EnergyProjectile> iterator = energyProjectiles.iterator();
        while (iterator.hasNext()) {
            EnergyProjectile projectile = iterator.next();
            float previousX = projectile.x;
            projectile.x += projectile.speed * projectile.direction * dt;

            boolean hit = false;
            if (projectile.ownerIndex == AI_OWNER_INDEX) {
                if (projectileHitsPlayer(
                    previousX,
                    projectile.x,
                    projectile.y,
                    24f
                )) {
                    applyPlayerHit(
                        projectile.damage,
                        projectile.direction,
                        "S",
                        false,
                        false
                    );
                    hit = true;
                }
            } else if (
                projectileHitsDummy(
                    previousX,
                    projectile.x,
                    projectile.y,
                    24f
                )
            ) {
                if (
                    projectile.ownerIndex >= 0 &&
                    projectile.ownerIndex < team.length
                ) {
                    addSuperMeter(
                        team[projectile.ownerIndex],
                        superGainForAttack("S")
                    );
                }
                applyDummyDamage(projectile.damage, projectile.direction);
                hit = true;
            }

            if (hit) {
                iterator.remove();
                continue;
            }

            if (
                Math.abs(projectile.x - projectile.startX) >= projectile.range ||
                projectile.x > RIGHT_BOUND + 120f ||
                projectile.x < LEFT_BOUND - 120f
            ) {
                iterator.remove();
            }
        }
    }

    private void recordCommandDirection(int direction, long nowMs) {
        if (direction == 0) return;

        if (commandCount > 0 && commandDirections[commandCount - 1] == direction) {
            commandTimes[commandCount - 1] = nowMs;
            return;
        }

        if (commandCount >= COMMAND_BUFFER_SIZE) {
            for (int i = 1; i < COMMAND_BUFFER_SIZE; i++) {
                commandDirections[i - 1] = commandDirections[i];
                commandTimes[i - 1] = commandTimes[i];
            }
            commandCount = COMMAND_BUFFER_SIZE - 1;
        }

        commandDirections[commandCount] = direction;
        commandTimes[commandCount] = nowMs;
        commandCount++;

        tryEnergyCommand(nowMs);
    }

    private void tryEnergyCommand(long nowMs) {
        FighterProfile profile = activeFighter().profile;
        if (
            !profile.hasEnergyAttack ||
            profile.energyCommand.length == 0 ||
            hasActiveEnergyProjectile(activeFighterIndex)
        ) {
            pendingEnergyUntilMs = -1L;
            return;
        }
        if (attackTimer > 0f) return;
        if (commandCount < profile.energyCommand.length) return;

        int commandIndex = profile.energyCommand.length - 1;
        int bufferIndex = commandCount - 1;
        long lastMatchedTime = -1L;
        long firstMatchedTime = -1L;

        // Procura a sequência de trás para frente e permite diagonais/intermediários.
        // Ex.: ↓ ↘ → continua reconhecendo o comando configurado ↓ →.
        while (commandIndex >= 0 && bufferIndex >= 0) {
            if (commandDirections[bufferIndex] == profile.energyCommand[commandIndex]) {
                long matchedTime = commandTimes[bufferIndex];

                if (
                    lastMatchedTime > 0L &&
                    lastMatchedTime - matchedTime > ENERGY_COMMAND_STEP_MS
                ) {
                    return;
                }

                lastMatchedTime = matchedTime;
                firstMatchedTime = matchedTime;
                commandIndex--;
            }
            bufferIndex--;
        }

        if (commandIndex >= 0) return;
        if (nowMs - commandTimes[commandCount - 1] > ENERGY_COMMAND_STEP_MS) return;
        if (nowMs - firstMatchedTime > ENERGY_COMMAND_STEP_MS * profile.energyCommand.length) return;

        // A sequência apenas arma o especial. L/M/H decide a força do projétil.
        pendingEnergyUntilMs = nowMs + ENERGY_CONFIRM_WINDOW_MS;

        commandCount = 0;
        forwardDashing = false;
        backDashTimer = 0f;
    }

    private void resetCommandBuffer() {
        commandCount = 0;
    }

    private void resetAutoCombo() {
        autoComboIndex = 0;
        lastAutoComboTapMs = -1000L;
    }

    private void triggerAutoCombo(long nowMs) {
        String[] combo = activeFighter().profile.baseCombo;
        if (combo.length == 0) return;

        if (nowMs - lastAutoComboTapMs > AUTO_COMBO_RESET_MS) {
            autoComboIndex = 0;
        }

        String nextAttack = combo[autoComboIndex];
        startAttack(nextAttack);

        autoComboIndex++;
        if (autoComboIndex >= combo.length) {
            autoComboIndex = 0;
        }

        lastAutoComboTapMs = nowMs;
    }

    private void switchFighter() {
        startTagAnimation();
    }

    private void applyDamage(int damage) {
        if (damage <= 0) return;
        FighterState fighter = activeFighter();
        if (fighter.life <= 0) return;

        fighter.life = Math.max(0, fighter.life - damage);
        fighter.refreshHudLabels();
        playerDamageFlashTimer = 0.14f;
    }

    private int currentPlayerGuardState() {
        if (
            !grounded ||
            playerKnockdownState != DUMMY_KD_NONE ||
            playerLaunchedByHit ||
            playerGroundSlam ||
            isTagAnimationActive() ||
            isSuperCinematicActive() ||
            attackTimer > 0f
        ) {
            return GUARD_NONE;
        }

        int relative = relativeDirection(dpadDirection);
        if (relative == 5) return GUARD_HIGH;
        if (relative == 4) return GUARD_LOW;
        return GUARD_NONE;
    }

    private boolean isLowAttack(String type) {
        return "2L".equals(type) || "2M".equals(type);
    }

    private boolean isProjectileOrSuper(String type) {
        return "S".equals(type) || "SUPER".equals(type);
    }

    private boolean playerBlocksAttack(
        String type,
        boolean attackerAirborne
    ) {
        int guard = currentPlayerGuardState();
        if (guard == GUARD_NONE) return false;

        if (isProjectileOrSuper(type)) {
            return true;
        }

        if (guard == GUARD_HIGH) {
            return !isLowAttack(type);
        }

        if (guard == GUARD_LOW) {
            return !attackerAirborne;
        }

        return false;
    }

    private boolean incomingAiProjectileThreat() {
        int guard = currentPlayerGuardState();
        if (guard == GUARD_NONE) return false;

        for (EnergyProjectile projectile : energyProjectiles) {
            if (projectile.ownerIndex != AI_OWNER_INDEX) continue;

            float toPlayer = playerX - projectile.x;
            if (toPlayer * projectile.direction >= -24f) {
                return true;
            }
        }

        for (SuperProjectile projectile : superProjectiles) {
            if (projectile.ownerIndex != AI_OWNER_INDEX) continue;

            float toPlayer = playerX - projectile.x;
            if (toPlayer * projectile.direction >= -58f) {
                return true;
            }
        }

        return false;
    }

    private boolean incomingAiMeleeThreat() {
        if (
            dummyAttackTimer <= 0f ||
            dummyAttackType.length() == 0 ||
            "S".equals(dummyAttackType)
        ) {
            return false;
        }

        if (!playerBlocksAttack(dummyAttackType, dummyAirborne)) {
            return false;
        }

        float reach = reachForAttack(dummyAttackType);
        if (reach <= 0f) return false;

        float phase = aiAttackPhase();
        if (phase < 0.40f) return false;

        int direction = opponentFacingDirection();
        float horizontalDistance = (playerX - dummyX) * direction;

        return
            horizontalDistance >= 0f &&
            horizontalDistance <= reach + 34f;
    }

    private int anticipatedPlayerGuardPose() {
        int guard = currentPlayerGuardState();
        if (guard == GUARD_NONE) return GUARD_NONE;

        if (
            incomingAiProjectileThreat() ||
            incomingAiMeleeThreat()
        ) {
            return guard;
        }

        return GUARD_NONE;
    }

    private void applyPlayerBlock(int hitDirection, String type) {
        int guard = currentPlayerGuardState();
        playerLastGuardState = guard;
        playerBlockFlashTimer = 0.12f;
        playerBlockstunTimer = BLOCKSTUN_DURATION;
        playerMovementLocked = true;
        playerKnockbackVelocityX = hitDirection * BLOCK_PUSH_SPEED;
        addSuperMeter(activeFighter(), superGainForGuard(type));

        attackType = "";
        attackTimer = 0f;
        attackDuration = 0f;
        attackHitApplied = false;
        forwardDashing = false;
        backDashTimer = 0f;
    }

    private void applyPlayerHit(
        int damage,
        int hitDirection,
        String type,
        boolean attackerAirborne,
        boolean attackerSuperJumping
    ) {
        if (playerBlocksAttack(type, attackerAirborne)) {
            applyPlayerBlock(hitDirection, type);
            return;
        }
        addSuperMeter(opponentFighter, superGainForAttack(type));
        applyDamage(damage);
        if (activeFighter().life <= 0) {
            playerMovementLocked = true;
            return;
        }

        if ("2M".equals(type)) {
            knockDownPlayer();
        } else if ("2H".equals(type)) {
            launchPlayer();
        } else if (
            "H".equals(type) &&
            attackerSuperJumping &&
            !grounded
        ) {
            slamPlayerToGround();
        } else {
            playerHitReactionTimer = DUMMY_HIT_REACTION_DURATION;
            playerMovementLocked = true;
            playerKnockbackVelocityX = hitDirection * 155f;
        }

        attackType = "";
        attackTimer = 0f;
        attackDuration = 0f;
        attackHitApplied = false;
        forwardDashing = false;
        backDashTimer = 0f;
    }

    private void launchPlayer() {
        playerKnockdownState = DUMMY_KD_NONE;
        playerKnockdownTimer = 0f;
        grounded = false;
        playerLaunchedByHit = true;
        playerGroundSlam = false;
        playerMovementLocked = true;
        velocityY = -DUMMY_LAUNCH_SPEED;
        playerY -= 2f;
    }

    private void slamPlayerToGround() {
        if (grounded) return;
        playerGroundSlam = true;
        playerMovementLocked = true;
        velocityY = DUMMY_SLAM_SPEED;
        playerKnockbackVelocityX *= 0.35f;
    }

    private void knockDownPlayer() {
        grounded = true;
        playerY = GROUND_Y;
        velocityY = 0f;
        playerLaunchedByHit = false;
        playerGroundSlam = false;
        playerMovementLocked = true;
        playerKnockdownState = DUMMY_KD_FALL;
        playerKnockdownTimer = 0f;
        playerKnockbackVelocityX *= 0.55f;
    }

    private void restoreHeldDirectionAfterBlock() {
        movingLeft =
            dpadDirection == 4 ||
            dpadDirection == 5 ||
            dpadDirection == 6;
        movingRight =
            dpadDirection == 1 ||
            dpadDirection == 2 ||
            dpadDirection == 8;
        crouching = grounded && isDownDirection(dpadDirection);
        forwardDashing = false;
    }

    private void updatePlayerReceivedState(float dt) {
        if (playerBlockFlashTimer > 0f) {
            playerBlockFlashTimer = Math.max(
                0f,
                playerBlockFlashTimer - dt
            );
        }

        if (playerBlockstunTimer > 0f) {
            playerBlockstunTimer = Math.max(
                0f,
                playerBlockstunTimer - dt
            );
            playerX += playerKnockbackVelocityX * dt;
            playerKnockbackVelocityX *= (float)Math.pow(0.018f, dt);

            if (playerBlockstunTimer <= 0f) {
                playerMovementLocked = false;
                playerKnockbackVelocityX = 0f;
                playerLastGuardState = GUARD_NONE;
                restoreHeldDirectionAfterBlock();
            }
        }

        if (playerHitReactionTimer > 0f) {
            playerHitReactionTimer = Math.max(
                0f,
                playerHitReactionTimer - dt
            );
            if (
                playerHitReactionTimer <= 0f &&
                playerBlockstunTimer <= 0f &&
                playerKnockdownState == DUMMY_KD_NONE &&
                !playerLaunchedByHit &&
                !playerGroundSlam
            ) {
                playerMovementLocked = false;
                playerKnockbackVelocityX = 0f;
            }
        }

        if (playerKnockdownState == DUMMY_KD_NONE) return;

        playerMovementLocked = true;
        playerKnockdownTimer += dt;

        if (
            playerKnockdownState == DUMMY_KD_FALL &&
            playerKnockdownTimer >= DUMMY_KD_FALL_DURATION
        ) {
            playerKnockdownState = DUMMY_KD_DOWN;
            playerKnockdownTimer = 0f;
        } else if (
            playerKnockdownState == DUMMY_KD_DOWN &&
            playerKnockdownTimer >= DUMMY_KD_DOWN_DURATION
        ) {
            playerKnockdownState = DUMMY_KD_GETUP;
            playerKnockdownTimer = 0f;
        } else if (
            playerKnockdownState == DUMMY_KD_GETUP &&
            playerKnockdownTimer >= DUMMY_KD_GETUP_DURATION
        ) {
            playerKnockdownState = DUMMY_KD_NONE;
            playerKnockdownTimer = 0f;
            playerMovementLocked = false;
        }
    }

    private float attackPhase() {
        if (attackTimer <= 0f || attackDuration <= 0f) return 0f;
        float t = 1f - attackTimer / attackDuration;
        return t < 0.5f ? t * 2f : (1f - t) * 2f;
    }

    private void updateFightCamera(float dt) {
        float separationX = Math.abs(playerX - dummyX);
        float requiredWorldWidth =
            separationX + CAMERA_FIGHTER_MARGIN_X;

        float targetZoomX =
            VW / Math.max(VW / CAMERA_ZOOM, requiredWorldWidth);

        float playerTop =
            playerY - (
                (crouching || isCrouchAttackActive())
                    ? 90f
                    : 145f
            );
        float opponentTop = dummyTop();
        float highestFighterTop = Math.min(playerTop, opponentTop);
        float heightAboveGround =
            Math.max(1f, GROUND_Y - highestFighterTop);

        // Tenta reservar margem visual acima do lutador mais alto.
        float usableVerticalScreen =
            CAMERA_GROUND_SCREEN_Y - CAMERA_TOP_MARGIN_SCREEN;
        float targetZoomY =
            usableVerticalScreen / heightAboveGround;

        boolean superJumpCameraActive =
            superJumping || aiSuperJumping;

        // No Super Jump, a altura nao abre o enquadramento:
        // a camera sobe/desce junto com o lutador e o zoom continua
        // respondendo somente a separacao horizontal.
        float targetZoom = clamp(
            superJumpCameraActive
                ? targetZoomX
                : Math.min(targetZoomX, targetZoomY),
            CAMERA_MIN_ZOOM,
            CAMERA_ZOOM
        );

        float zoomFollow =
            1f - (float)Math.pow(0.0025f, dt);
        cameraZoom +=
            (targetZoom - cameraZoom) * zoomFollow;

        float visibleWorldWidth = VW / cameraZoom;
        float halfVisible = visibleWorldWidth * 0.5f;

        float fightCenterX = (playerX + dummyX) * 0.5f;
        float targetCameraX = clamp(
            fightCenterX,
            halfVisible,
            WORLD_WIDTH - halfVisible
        );

        float horizontalFollow =
            1f - (float)Math.pow(0.0015f, dt);
        cameraX +=
            (targetCameraX - cameraX) * horizontalFollow;

        // Mantém o chão praticamente na mesma altura da tela enquanto possível.
        float baseTop =
            GROUND_Y - CAMERA_GROUND_SCREEN_Y / cameraZoom;
        float topMarginWorld =
            CAMERA_TOP_MARGIN_SCREEN / cameraZoom;

        float targetCameraTop = baseTop;
        if (
            highestFighterTop <
            targetCameraTop + topMarginWorld
        ) {
            targetCameraTop =
                highestFighterTop - topMarginWorld;
        }

        // Fora do Super Jump, preserva uma faixa do chao na tela.
        // Durante o Super Jump essa trava e removida para a camera poder
        // acompanhar verticalmente sem precisar afastar o zoom.
        if (!superJumpCameraActive) {
            float lowestAllowedTop =
                GROUND_Y -
                (VH - CAMERA_BOTTOM_MARGIN_SCREEN) / cameraZoom;
            targetCameraTop = Math.max(
                targetCameraTop,
                lowestAllowedTop
            );
        }
        targetCameraTop = clamp(
            targetCameraTop,
            WORLD_TOP,
            GROUND_CAMERA_TOP
        );

        float verticalFollow =
            1f - (float)Math.pow(0.0007f, dt);
        cameraTop +=
            (targetCameraTop - cameraTop) * verticalFollow;
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

            float renderZoom = cameraZoom * superCameraZoom;
            float visibleWorldWidth = VW / renderZoom;
            float cameraLeft = clamp(cameraX - visibleWorldWidth / 2f, 0f, WORLD_WIDTH - visibleWorldWidth);

            canvas.save();
            canvas.scale(renderZoom, renderZoom);
            canvas.translate(-cameraLeft, -cameraTop);
            drawScenario(canvas);
            drawDamageDummy(canvas);
            drawEnergyProjectiles(canvas);
            drawSuperProjectiles(canvas);
            drawSuperDarkening(canvas);

            canvas.save();
            if (facingDirection < 0) {
                canvas.scale(-1f, 1f, playerX, 0f);
            }
            drawSuperChargeEffects(canvas);
            canvas.restore();

            canvas.save();
            canvas.translate(tagVisualOffsetX, 0f);
            if (facingDirection < 0) {
                canvas.scale(-1f, 1f, playerX, 0f);
            }
            drawPlayer(canvas);
            canvas.restore();
            drawSuperFlash(canvas);
            canvas.restore();

            drawHud(canvas);
            drawControls(canvas);

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
        paint.setShader(skyGradient);
        c.drawRect(0, WORLD_TOP, WORLD_WIDTH, VH, paint);
        paint.setShader(null);

        paint.setColor(Color.argb(130, 255, 244, 201));
        c.drawCircle(2050, -40, 58, paint);

        paint.setColor(Color.argb(75, 255, 255, 255));
        for (int i = 0; i < 12; i++) {
            float cloudX = 110f + i * 215f;
            float cloudY = -365f + (i % 4) * 92f;
            c.drawOval(cloudX, cloudY, cloudX + 120f, cloudY + 38f, paint);
        }

        paint.setColor(Color.rgb(53, 73, 88));
        for (android.graphics.Path mountainPath : mountainPaths) {
            c.drawPath(mountainPath, paint);
        }

        paint.setColor(Color.rgb(116, 81, 50));
        c.drawRect(0, GROUND_Y, WORLD_WIDTH, VH, paint);
        paint.setColor(Color.rgb(148, 108, 67));
        for (int i = 0; i < 38; i++) {
            float x = (i * 83f) % WORLD_WIDTH;
            c.drawRoundRect(
                x,
                GROUND_Y + 35 + (i % 3) * 38,
                x + 55,
                GROUND_Y + 40 + (i % 3) * 38,
                3,
                3,
                paint
            );
        }

        paint.setColor(Color.argb(90, 255, 255, 255));
        c.drawRect(LEFT_BOUND, 190, LEFT_BOUND + 3, GROUND_Y, paint);
        c.drawRect(RIGHT_BOUND - 3, 190, RIGHT_BOUND, GROUND_Y, paint);
    }

    private void drawHud(Canvas c) {
        FighterState active = activeFighter();
        FighterState reserve = reserveFighter();

        paint.setColor(Color.argb(185, 10, 15, 27));
        c.drawRoundRect(32, 28, 560, 234, 18, 18, paint);

        paint.setColor(active.profile.color);
        c.drawCircle(78, 74, 29, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setColor(Color.WHITE);
        c.drawCircle(78, 74, 29, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(Color.WHITE);
        paint.setTextSize(22);
        paint.setFakeBoldText(true);
        c.drawText(active.profile.name, 122, 58, paint);
        paint.setFakeBoldText(false);

        drawLifeBar(c, active, 122f, 70f, 525f, 98f, true);
        drawSuperMeter(c, active, 122f, 106f, 525f, 118f, true);
        drawTagCooldown(c, 122f, 139f, 525f, 149f);

        // Reserva: indicador menor com vida e Super próprios.
        paint.setColor(reserve.profile.color);
        c.drawCircle(78, 187, 14, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        c.drawText(reserve.profile.reserveHudLabel, 105, 184, paint);
        paint.setFakeBoldText(false);

        drawLifeBar(c, reserve, 105f, 193f, 525f, 206f, false);
        drawSuperMeter(c, reserve, 105f, 213f, 525f, 223f, false);

        paint.setColor(Color.argb(180, 10, 15, 27));
        c.drawRoundRect(945, 28, 1248, 112, 18, 18, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(20);
        paint.setFakeBoldText(true);
        c.drawText("SPRITE ASTRA • v0.53", 975, 59, paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(16);

        c.drawText("Estado:", 975, 88, paint);
        c.drawText(currentStateLabel(), 1032, 88, paint);

        paint.setTextSize(14f);
        c.drawText(
            facingDirection > 0 ? "FACING: →" : "FACING: ←",
            975,
            106,
            paint
        );

        drawAiToggle(c);
    }

    private void drawAiToggle(Canvas c) {
        paint.setColor(
            opponentAiEnabled
                ? Color.rgb(74, 205, 232)
                : Color.argb(180, 45, 53, 62)
        );
        c.drawRoundRect(
            AI_BUTTON_LEFT,
            AI_BUTTON_TOP,
            AI_BUTTON_RIGHT,
            AI_BUTTON_BOTTOM,
            12f,
            12f,
            paint
        );

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f);
        paint.setColor(Color.WHITE);
        c.drawRoundRect(
            AI_BUTTON_LEFT,
            AI_BUTTON_TOP,
            AI_BUTTON_RIGHT,
            AI_BUTTON_BOTTOM,
            12f,
            12f,
            paint
        );
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(
            opponentAiEnabled
                ? Color.rgb(18, 35, 48)
                : Color.WHITE
        );
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(19f);
        float cy = (AI_BUTTON_TOP + AI_BUTTON_BOTTOM) * 0.5f;
        c.drawText(
            opponentAiEnabled ? "IA ON" : "IA OFF",
            (AI_BUTTON_LEFT + AI_BUTTON_RIGHT) * 0.5f,
            cy - (paint.ascent() + paint.descent()) * 0.5f,
            paint
        );
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private String currentStateLabel() {
        if (playerKnockdownState != DUMMY_KD_NONE) return "DERRUBADO";
        if (playerBlockstunTimer > 0f) {
            return playerLastGuardState == GUARD_LOW
                ? "BLOQUEIO BAIXO"
                : "BLOQUEIO ALTO";
        }
        int anticipatedGuard = anticipatedPlayerGuardPose();
        if (anticipatedGuard == GUARD_LOW) return "DEFENDENDO BAIXO";
        if (anticipatedGuard == GUARD_HIGH) return "DEFENDENDO ALTO";

        int guard = currentPlayerGuardState();
        if (guard == GUARD_LOW) return "PRONTO BAIXO";
        if (guard == GUARD_HIGH) return "PRONTO ALTO";
        if (playerGroundSlam) return "QUEDA FORÇADA";
        if (playerLaunchedByHit) return "LANÇADO";
        if (playerHitReactionTimer > 0f) return "HIT";
        if (isSuperCinematicActive()) return "SUPER";
        if (attackTimer > 0f) {
            if ("L".equals(attackType)) return "ATAQUE L";
            if ("M".equals(attackType)) return "ATAQUE M";
            if ("H".equals(attackType)) return "ATAQUE H";
            if ("S".equals(attackType)) return "ATAQUE S";
            if ("2L".equals(attackType)) return "ATAQUE 2L";
            if ("2M".equals(attackType)) return "ATAQUE 2M";
            if ("2H".equals(attackType)) return "ATAQUE 2H";
            return "ATAQUE";
        }
        if (backDashTimer > 0f) return "BACKDASH";
        if (forwardDashing) return "DASH";
        if (superJumping) return "SUPER JUMP";
        if (crouching) return "AGACHADO";
        if (!grounded) return "NO AR";
        if (movingLeft || movingRight) return "ANDANDO";
        return "PARADO";
    }

    private void drawSuperMeter(
        Canvas c,
        FighterState fighter,
        float left,
        float top,
        float right,
        float bottom,
        boolean showText
    ) {
        float gap = 4f;
        float totalWidth = right - left;
        float segmentWidth = (totalWidth - gap * 4f) / 5f;

        for (int i = 0; i < 5; i++) {
            float segmentLeft = left + i * (segmentWidth + gap);
            float segmentRight = segmentLeft + segmentWidth;

            paint.setColor(Color.rgb(45, 53, 62));
            c.drawRoundRect(
                segmentLeft,
                top,
                segmentRight,
                bottom,
                4f,
                4f,
                paint
            );

            float fill = clamp(fighter.superMeter - i, 0f, 1f);
            if (fill > 0f) {
                paint.setColor(fighter.profile.color);
                c.drawRoundRect(
                    segmentLeft,
                    top,
                    segmentLeft + segmentWidth * fill,
                    bottom,
                    4f,
                    4f,
                    paint
                );
            }

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.4f);
            paint.setColor(Color.argb(210, 255, 255, 255));
            c.drawRoundRect(
                segmentLeft,
                top,
                segmentRight,
                bottom,
                4f,
                4f,
                paint
            );
            paint.setStyle(Paint.Style.FILL);
        }

        if (showText) {
            paint.setColor(Color.WHITE);
            paint.setTextSize(12f);
            c.drawText(fighter.superHudLabel, left, bottom + 13f, paint);
        }
    }

    private void drawTagCooldown(Canvas c, float left, float top, float right, float bottom) {
        float ratio;

        if (isTagAnimationActive()) {
            ratio = 0f;
        } else if (tagCooldownRemaining > 0f) {
            ratio = 1f - (tagCooldownRemaining / TAG_COOLDOWN_SECONDS);
        } else {
            ratio = 1f;
        }

        paint.setColor(Color.rgb(45, 53, 62));
        c.drawRoundRect(left, top, right, bottom, 5f, 5f, paint);

        if (ratio > 0f) {
            paint.setColor(reserveFighter().profile.color);
            c.drawRoundRect(left, top, left + (right - left) * ratio, bottom, 5f, 5f, paint);
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f);
        paint.setColor(Color.argb(210, 255, 255, 255));
        c.drawRoundRect(left, top, right, bottom, 5f, 5f, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(Color.WHITE);
        paint.setTextSize(12f);
        c.drawText(tagCooldownHudLabel, left, bottom + 13f, paint);
    }

    private void drawLifeBar(
        Canvas c,
        FighterState fighter,
        float left,
        float top,
        float right,
        float bottom,
        boolean showText
    ) {
        float ratio = clamp(fighter.life / (float)fighter.profile.maxLife, 0f, 1f);
        float width = right - left;

        paint.setColor(Color.rgb(45, 53, 62));
        c.drawRoundRect(left, top, right, bottom, 8, 8, paint);

        int lifeColor;
        if (ratio > 0.55f) lifeColor = Color.rgb(111, 223, 105);
        else if (ratio > 0.25f) lifeColor = Color.rgb(240, 190, 72);
        else lifeColor = Color.rgb(229, 82, 82);

        if (ratio > 0f) {
            paint.setColor(lifeColor);
            c.drawRoundRect(left, top, left + width * ratio, bottom, 8, 8, paint);
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(showText ? 2f : 1.5f);
        paint.setColor(Color.argb(210, 255, 255, 255));
        c.drawRoundRect(left, top, right, bottom, 8, 8, paint);
        paint.setStyle(Paint.Style.FILL);

        if (showText) {
            paint.setColor(Color.WHITE);
            paint.setTextSize(15);
            c.drawText(
                fighter.lifeHudLabel,
                left + 6,
                bottom - 7,
                paint
            );
        }
    }

    private void drawDamageDummy(Canvas c) {
        float top = dummyTop();
        float baseY = dummyY;

        paint.setColor(Color.argb(70, 0, 0, 0));
        c.drawOval(
            dummyX - 43f,
            GROUND_Y - 10f,
            dummyX + 43f,
            GROUND_Y + 10f,
            paint
        );

        int dummyColor = dummyLife > 0
            ? opponentProfile().color
            : Color.rgb(95, 95, 105);

        c.save();
        if (facingDirection > 0) {
            // Jogador à esquerda: dummy olha para a esquerda.
            c.scale(-1f, 1f, dummyX, 0f);
        }

        float knockdownAngle = 0f;
        if (dummyKnockdownState == DUMMY_KD_FALL) {
            float t = clamp(
                dummyKnockdownTimer / DUMMY_KD_FALL_DURATION,
                0f,
                1f
            );
            knockdownAngle = 88f * t;
        } else if (dummyKnockdownState == DUMMY_KD_DOWN) {
            knockdownAngle = 88f;
        } else if (dummyKnockdownState == DUMMY_KD_GETUP) {
            float t = clamp(
                dummyKnockdownTimer / DUMMY_KD_GETUP_DURATION,
                0f,
                1f
            );
            knockdownAngle = 88f * (1f - t);
        }

        if (knockdownAngle > 0f) {
            c.rotate(knockdownAngle, dummyX, GROUND_Y);
        }

        float hitProgress = dummyHitReactionTimer > 0f
            ? 1f - dummyHitReactionTimer / DUMMY_HIT_REACTION_DURATION
            : 1f;
        float recoilPose = dummyHitReactionTimer > 0f
            ? (float)Math.sin(hitProgress * Math.PI)
            : 0f;

        if (
            dummyKnockdownState == DUMMY_KD_NONE &&
            dummyAirborne
        ) {
            float airTilt = dummyMovementLocked ? 1f : 0.55f;
            recoilPose = Math.max(recoilPose, airTilt);
        }
        boolean hitFlash =
            dummyHitReactionTimer > DUMMY_HIT_REACTION_DURATION - 0.065f;

        if (hitFlash && dummyLife > 0) {
            dummyColor = Color.rgb(255, 235, 235);
        }

        float headX = dummyX - 13f * recoilPose;
        float shoulderX = dummyX - 9f * recoilPose;
        float hipX = dummyX - 3f * recoilPose;

        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(dummyColor);
        c.drawCircle(headX, top + 20f - 2f * recoilPose, 25f, paint);

        paint.setStrokeWidth(25f);
        c.drawLine(
            shoulderX,
            top + 48f,
            hipX,
            baseY - 45f,
            paint
        );

        paint.setStrokeWidth(18f);
        if (dummyAttackTimer > 0f || aiSuperTimer > 0f) {
            float phase = aiAttackPhase();

            if (aiSuperTimer > 0f) {
                float pulse = 1f + 0.10f * (float)Math.sin(aiSuperTimer * 26f);
                c.drawLine(
                    shoulderX - 3f,
                    top + 58f,
                    dummyX - 54f * pulse,
                    top + 38f,
                    paint
                );
                c.drawLine(
                    shoulderX + 3f,
                    top + 58f,
                    dummyX + 70f * pulse,
                    top + 38f,
                    paint
                );
                c.drawLine(hipX - 4f, baseY - 45f, dummyX - 38f, baseY, paint);
                c.drawLine(hipX + 4f, baseY - 45f, dummyX + 38f, baseY, paint);
            } else if ("2L".equals(dummyAttackType)) {
                c.drawLine(shoulderX - 3f, top + 58f, dummyX - 30f, top + 96f, paint);
                c.drawLine(shoulderX + 3f, top + 58f, dummyX + 76f * phase, top + 90f, paint);
                c.drawLine(hipX - 4f, baseY - 45f, dummyX - 42f, baseY - 8f, paint);
                c.drawLine(hipX + 4f, baseY - 45f, dummyX + 39f, baseY - 8f, paint);
            } else if ("2M".equals(dummyAttackType)) {
                c.drawLine(shoulderX - 3f, top + 58f, dummyX - 30f, top + 96f, paint);
                c.drawLine(shoulderX + 3f, top + 58f, dummyX + 34f, top + 94f, paint);
                c.drawLine(hipX - 4f, baseY - 45f, dummyX - 36f, baseY - 8f, paint);
                c.drawLine(hipX + 4f, baseY - 45f, dummyX + 44f + 72f * phase, baseY - 12f, paint);
            } else if ("2H".equals(dummyAttackType)) {
                c.drawLine(shoulderX - 3f, top + 58f, dummyX - 32f, top + 96f, paint);
                c.drawLine(shoulderX + 3f, top + 58f, dummyX + 42f + 82f * phase, top + 54f - 16f * phase, paint);
                c.drawLine(hipX - 4f, baseY - 45f, dummyX - 46f, baseY - 8f, paint);
                c.drawLine(hipX + 4f, baseY - 45f, dummyX + 44f, baseY - 8f, paint);
            } else if ("M".equals(dummyAttackType)) {
                c.drawLine(shoulderX - 3f, top + 58f, dummyX - 28f, top + 97f, paint);
                c.drawLine(shoulderX + 3f, top + 58f, dummyX + 30f, top + 94f, paint);
                c.drawLine(hipX - 4f, baseY - 45f, dummyX - 25f, baseY, paint);
                c.drawLine(hipX + 4f, baseY - 45f, dummyX + 34f + 76f * phase, baseY - 48f * phase, paint);
            } else if ("H".equals(dummyAttackType)) {
                c.drawLine(shoulderX - 3f, top + 58f, dummyX - 26f, top + 98f, paint);
                c.drawLine(shoulderX + 3f, top + 58f, dummyX + 28f + 92f * phase, top + 84f + 22f * phase, paint);
                c.drawLine(hipX - 4f, baseY - 45f, dummyX - 30f, baseY, paint);
                c.drawLine(hipX + 4f, baseY - 45f, dummyX + 30f, baseY, paint);
            } else {
                c.drawLine(shoulderX - 3f, top + 58f, dummyX - 30f, top + 96f, paint);
                c.drawLine(shoulderX + 3f, top + 58f, dummyX + 34f + 72f * phase, top + 66f, paint);
                c.drawLine(hipX - 4f, baseY - 45f, dummyX - 26f, baseY, paint);
                c.drawLine(hipX + 4f, baseY - 45f, dummyX + 26f, baseY, paint);
            }
        } else if (dummyHitReactionTimer > 0f) {
            // Braços abrem e o tronco recua no impacto.
            c.drawLine(
                shoulderX - 3f,
                top + 58f,
                dummyX - 52f - 18f * recoilPose,
                top + 82f,
                paint
            );
            c.drawLine(
                shoulderX + 3f,
                top + 58f,
                dummyX - 15f - 28f * recoilPose,
                top + 112f,
                paint
            );
            c.drawLine(
                hipX - 4f,
                baseY - 45f,
                dummyX - 34f,
                baseY,
                paint
            );
            c.drawLine(
                hipX + 4f,
                baseY - 45f,
                dummyX + 31f,
                baseY,
                paint
            );
        } else {
            c.drawLine(dummyX - 3f, top + 58f, dummyX - 34f, top + 100f, paint);
            c.drawLine(dummyX + 3f, top + 58f, dummyX + 34f, top + 100f, paint);
            c.drawLine(dummyX - 4f, baseY - 45f, dummyX - 28f, baseY, paint);
            c.drawLine(dummyX + 4f, baseY - 45f, dummyX + 28f, baseY, paint);
        }

        // Marca frontal para ficar visualmente claro quando o dummy vira.
        paint.setColor(Color.rgb(24, 35, 48));
        paint.setStrokeWidth(4f);
        c.drawLine(
            headX + 5f,
            top + 15f - 2f * recoilPose,
            headX + 13f,
            top + 15f - 2f * recoilPose,
            paint
        );
        c.restore();
        paint.setStrokeCap(Paint.Cap.BUTT);

        float barLeft = dummyX - 125f;
        float barRight = dummyX + 125f;
        float barTop = top - 54f;
        float barBottom = barTop + 16f;
        float lifeRatio = dummyLife / (float)DUMMY_MAX_LIFE;

        paint.setColor(Color.argb(205, 12, 16, 28));
        c.drawRoundRect(
            barLeft - 8f,
            barTop - 28f,
            barRight + 8f,
            barBottom + 23f,
            10f,
            10f,
            paint
        );

        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(15f);
        String dummyStatusLabel;
        if (dummyKnockdownState == DUMMY_KD_FALL) {
            dummyStatusLabel = "NPC TESTE • CAINDO";
        } else if (dummyKnockdownState == DUMMY_KD_DOWN) {
            dummyStatusLabel = "NPC TESTE • NO CHÃO";
        } else if (dummyKnockdownState == DUMMY_KD_GETUP) {
            dummyStatusLabel = "NPC TESTE • LEVANTANDO";
        } else if (aiSuperTimer > 0f) {
            dummyStatusLabel = "PLAYER 2 • SUPER";
        } else if (dummyAttackTimer > 0f) {
            dummyStatusLabel = "PLAYER 2 • " + dummyAttackType;
        } else if (dummyGroundSlam) {
            dummyStatusLabel = "NPC TESTE • QUEDA FORÇADA";
        } else if (dummyMovementLocked) {
            dummyStatusLabel = "NPC TESTE • SEM CONTROLE";
        } else if (dummyAirborne) {
            dummyStatusLabel = "NPC TESTE • DESCENDO";
        } else {
            dummyStatusLabel =
                opponentAiEnabled
                    ? "PLAYER 2 • IA"
                    : "PLAYER 2 • DUMMY";
        }

        c.drawText(
            dummyStatusLabel,
            dummyX,
            barTop - 9f,
            paint
        );
        paint.setFakeBoldText(false);

        paint.setColor(Color.rgb(45, 53, 62));
        c.drawRoundRect(barLeft, barTop, barRight, barBottom, 6f, 6f, paint);

        if (lifeRatio > 0f) {
            int lifeColor;
            if (lifeRatio > 0.55f) lifeColor = Color.rgb(111, 223, 105);
            else if (lifeRatio > 0.25f) lifeColor = Color.rgb(240, 190, 72);
            else lifeColor = Color.rgb(229, 82, 82);

            paint.setColor(lifeColor);
            c.drawRoundRect(
                barLeft,
                barTop,
                barLeft + (barRight - barLeft) * lifeRatio,
                barBottom,
                6f,
                6f,
                paint
            );
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f);
        paint.setColor(Color.WHITE);
        c.drawRoundRect(barLeft, barTop, barRight, barBottom, 6f, 6f, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(Color.WHITE);
        paint.setTextSize(12f);
        c.drawText(dummyLifeHudLabel, dummyX, barBottom + 16f, paint);
        paint.setTextSize(11f);
        c.drawText(
            opponentFighter.superHudLabel,
            dummyX,
            barBottom + 30f,
            paint
        );

        if (dummyDamageLabelTimer > 0f) {
            float progress = dummyDamageLabelTimer / 0.72f;
            paint.setColor(Color.WHITE);
            paint.setAlpha(Math.round(255f * progress));
            paint.setFakeBoldText(true);
            paint.setTextSize(24f);
            c.drawText(
                dummyDamageLabel,
                dummyX,
                top - 72f - (1f - progress) * 28f,
                paint
            );
            paint.setFakeBoldText(false);
            paint.setAlpha(255);
        }

        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawEnergyProjectiles(Canvas c) {
        for (EnergyProjectile projectile : energyProjectiles) {
            c.save();
            if (projectile.direction < 0) {
                c.scale(-1f, 1f, projectile.x, projectile.y);
            }

            paint.setColor(Color.argb(75, 255, 255, 255));
            c.drawCircle(projectile.x, projectile.y, 29f, paint);

            paint.setColor(projectile.color);
            c.drawCircle(projectile.x, projectile.y, 20f, paint);

            paint.setColor(Color.WHITE);
            c.drawCircle(projectile.x + 5f, projectile.y - 5f, 8f, paint);
            c.restore();
        }
    }

    private void drawSuperProjectiles(Canvas c) {
        for (SuperProjectile projectile : superProjectiles) {
            c.save();
            if (projectile.direction < 0) {
                c.scale(-1f, 1f, projectile.x, projectile.y);
            }

            paint.setColor(Color.argb(70, 255, 255, 255));
            c.drawCircle(projectile.x, projectile.y, 62f, paint);

            paint.setColor(Color.argb(120, Color.red(projectile.color), Color.green(projectile.color), Color.blue(projectile.color)));
            c.drawOval(
                projectile.x - 72f,
                projectile.y - 34f,
                projectile.x + 34f,
                projectile.y + 34f,
                paint
            );

            paint.setColor(projectile.color);
            c.drawCircle(projectile.x, projectile.y, 40f, paint);

            paint.setColor(Color.WHITE);
            c.drawCircle(projectile.x + 12f, projectile.y - 10f, 17f, paint);

            paint.setColor(Color.argb(100, 255, 255, 255));
            c.drawRect(
                projectile.x - 135f,
                projectile.y - 9f,
                projectile.x - 38f,
                projectile.y + 9f,
                paint
            );
            c.restore();
        }
    }

    private void drawSuperDarkening(Canvas c) {
        if (!isSuperCinematicActive() || superDarkAlpha <= 0) return;
        paint.setColor(Color.argb(superDarkAlpha, 0, 0, 8));
        c.drawRect(0f, WORLD_TOP, WORLD_WIDTH, VH + 120f, paint);
    }

    private void drawSuperChargeEffects(Canvas c) {
        if (!isSuperCinematicActive()) return;

        float centerY = playerY - 78f;
        float pulse = 1f + 0.16f * (float)Math.sin(superPhaseTimer * 28f);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);
        paint.setColor(Color.argb(185, 255, 255, 255));
        c.drawCircle(playerX, centerY, 58f * pulse, paint);

        paint.setStrokeWidth(3f);
        paint.setColor(activeFighter().profile.color);
        c.drawCircle(playerX, centerY, 82f * pulse, paint);

        for (int i = 0; i < 7; i++) {
            float y = centerY - 105f + i * 34f;
            float length = 90f + (i % 3) * 42f;
            paint.setStrokeWidth(4f);
            paint.setColor(Color.argb(145, 255, 255, 255));
            c.drawLine(playerX - 150f - length, y + 24f, playerX - 78f, y, paint);
        }

        paint.setStyle(Paint.Style.FILL);
    }

    private void drawSuperFlash(Canvas c) {
        if (!isSuperCinematicActive() || superFlashAlpha <= 0) return;
        paint.setColor(Color.argb(superFlashAlpha, 255, 255, 255));
        c.drawRect(0f, WORLD_TOP, WORLD_WIDTH, VH + 120f, paint);
    }

    private void updateSpriteMotion(float dt, float travel) {
        int guard = playerBlockstunTimer > 0f ? playerLastGuardState : anticipatedPlayerGuardPose();
        String attackAnimation = attackTimer > 0f && activeSpriteMove != null
            ? activeSpriteMove.animation.id : null;
        boolean combatPose =
            (attackTimer > 0f && attackAnimation == null) ||
            guard != GUARD_NONE || isSuperPoseActive() || isTagAnimationActive();
        spriteFighterRenderer.update(dt,grounded,crouching || isCrouchAttackActive() || guard == GUARD_LOW,
            velocityY,travel,travel*facingDirection > 0,forwardDashing,backDashTimer>0,
            attackAnimation,attackDuration-attackTimer,combatPose,playerMovementLocked);
    }

    private void drawPlayer(Canvas c) {
        float baseY = playerY;

        float playerKnockdownAngle = 0f;
        if (playerKnockdownState == DUMMY_KD_FALL) {
            float t = clamp(
                playerKnockdownTimer / DUMMY_KD_FALL_DURATION,
                0f,
                1f
            );
            playerKnockdownAngle = -88f * t;
        } else if (playerKnockdownState == DUMMY_KD_DOWN) {
            playerKnockdownAngle = -88f;
        } else if (playerKnockdownState == DUMMY_KD_GETUP) {
            float t = clamp(
                playerKnockdownTimer / DUMMY_KD_GETUP_DURATION,
                0f,
                1f
            );
            playerKnockdownAngle = -88f * (1f - t);
        }

        if (playerKnockdownAngle != 0f) {
            c.rotate(
                playerKnockdownAngle,
                playerX,
                GROUND_Y
            );
        }

        int guardPose = playerBlockstunTimer > 0f
            ? playerLastGuardState
            : anticipatedPlayerGuardPose();

        spriteFighterRenderer.draw(c,paint,playerX,baseY,
            playerDamageFlashTimer>0,playerBlockFlashTimer>0);
    }

    private void drawControls(Canvas c) {
        paint.setColor(Color.argb(105, 7, 13, 26));
        c.drawCircle(DPAD_X, DPAD_Y, DPAD_RADIUS, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(Color.argb(215, 255, 255, 255));
        c.drawCircle(DPAD_X, DPAD_Y, DPAD_RADIUS, paint);

        paint.setStrokeWidth(2f);
        paint.setColor(Color.argb(90, 255, 255, 255));
        for (int i = 0; i < DPAD_LABELS.length; i++) {
            float x = DPAD_X + DPAD_UNIT_X[i] * DPAD_RADIUS;
            float y = DPAD_Y + DPAD_UNIT_Y[i] * DPAD_RADIUS;
            c.drawLine(DPAD_X, DPAD_Y, x, y, paint);
        }
        paint.setStyle(Paint.Style.FILL);

        if (dpadDirection != 0) {
            int directionIndex = dpadDirection - 1;
            float hx = DPAD_X + DPAD_UNIT_X[directionIndex] * 72f;
            float hy = DPAD_Y + DPAD_UNIT_Y[directionIndex] * 72f;
            paint.setColor(Color.argb(165, 255, 255, 255));
            c.drawCircle(hx, hy, 31f, paint);
        }

        paint.setColor(Color.argb(175, 10, 18, 32));
        c.drawCircle(DPAD_X, DPAD_Y, DPAD_DEADZONE, paint);

        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(27f);

        float textCenterOffset = -(paint.ascent() + paint.descent()) / 2f;
        for (int i = 0; i < DPAD_LABELS.length; i++) {
            float tx = DPAD_X + DPAD_UNIT_X[i] * 78f;
            float ty = DPAD_Y + DPAD_UNIT_Y[i] * 78f + textCenterOffset;
            c.drawText(DPAD_LABELS[i], tx, ty, paint);
        }

        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);

        drawAttackButton(c, LIGHT_X, LIGHT_Y, "L", lightPointer != -1);
        drawAttackButton(c, MEDIUM_X, MEDIUM_Y, "M", mediumPointer != -1);
        drawAttackButton(c, HEAVY_X, HEAVY_Y, "H", heavyPointer != -1);
        drawComboButton(c);
        drawTagButton(c);
        drawSuperButton(c);
    }

    private void drawAttackButton(Canvas c, float x, float y, String label, boolean pressed) {
        paint.setColor(
            pressed
                ? Color.argb(195, 255, 255, 255)
                : Color.argb(120, 7, 13, 26)
        );
        c.drawCircle(x, y, ATTACK_RADIUS, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(Color.argb(220, 255, 255, 255));
        c.drawCircle(x, y, ATTACK_RADIUS, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(pressed ? Color.rgb(25, 35, 48) : Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(31f);
        paint.setFakeBoldText(true);
        float textY = y - (paint.ascent() + paint.descent()) / 2f;
        c.drawText(label, x, textY, paint);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawComboButton(Canvas c) {
        boolean pressed = comboPointer != -1;

        paint.setColor(
            pressed
                ? Color.argb(205, 255, 255, 255)
                : Color.argb(135, 7, 13, 26)
        );
        c.drawCircle(COMBO_X, COMBO_Y, COMBO_RADIUS, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(Color.argb(225, 255, 255, 255));
        c.drawCircle(COMBO_X, COMBO_Y, COMBO_RADIUS, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(pressed ? Color.rgb(25, 35, 48) : Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(18f);
        paint.setFakeBoldText(true);
        float textY = COMBO_Y - (paint.ascent() + paint.descent()) / 2f;
        c.drawText("COMBO", COMBO_X, textY, paint);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawTagButton(Canvas c) {
        boolean pressed = tagPointer != -1;
        boolean enabled = canStartTag();

        if (!enabled) {
            paint.setColor(Color.argb(95, 55, 60, 68));
        } else {
            paint.setColor(
                pressed
                    ? reserveFighter().profile.color
                    : Color.argb(145, 7, 13, 26)
            );
        }
        c.drawCircle(TAG_X, TAG_Y, TAG_RADIUS, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(
            enabled
                ? reserveFighter().profile.color
                : Color.argb(120, 180, 180, 180)
        );
        c.drawCircle(TAG_X, TAG_Y, TAG_RADIUS, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(enabled ? Color.WHITE : Color.argb(155, 220, 220, 220));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(enabled ? 17f : 14f);
        paint.setFakeBoldText(true);
        float textY = TAG_Y - (paint.ascent() + paint.descent()) / 2f;
        String label = enabled
            ? "TROCA"
            : tagCooldownButtonLabel;
        c.drawText(label, TAG_X, textY, paint);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawSuperButton(Canvas c) {
        boolean pressed = superPointer != -1;
        boolean enabled = canStartSuper();

        paint.setColor(
            enabled
                ? (pressed
                    ? activeFighter().profile.color
                    : Color.argb(170, 42, 16, 68))
                : Color.argb(90, 55, 60, 68)
        );
        c.drawCircle(SUPER_X, SUPER_Y, SUPER_RADIUS, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(
            enabled
                ? Color.argb(235, 255, 220, 90)
                : Color.argb(120, 180, 180, 180)
        );
        c.drawCircle(SUPER_X, SUPER_Y, SUPER_RADIUS, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(enabled ? Color.WHITE : Color.argb(150, 220, 220, 220));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(15f);
        paint.setFakeBoldText(true);
        c.drawText("SUPER", SUPER_X, SUPER_Y - 4f, paint);
        paint.setTextSize(11f);
        c.drawText(
            activeFighter().superLevelHudLabel,
            SUPER_X,
            SUPER_Y + 14f,
            paint
        );
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private boolean insideRect(
        float x,
        float y,
        float left,
        float top,
        float right,
        float bottom
    ) {
        return x >= left && x <= right && y >= top && y <= bottom;
    }

    private boolean insideCircle(
        float x,
        float y,
        float cx,
        float cy,
        float radius
    ) {
        float dx = x - cx;
        float dy = y - cy;
        return dx * dx + dy * dy <= radius * radius;
    }

    private boolean isDownDirection(int direction) {
        return direction == 2 || direction == 3 || direction == 4;
    }

    private boolean isUpDirection(int direction) {
        return direction == 6 || direction == 7 || direction == 8;
    }

    private void updateDpad(float x, float y, long nowMs) {
        if (isEnergyAttackActive() || isSuperCinematicActive()) return;

        float dx = x - DPAD_X;
        float dy = y - DPAD_Y;
        float distanceSquared = dx * dx + dy * dy;

        int previous = dpadDirection;
        int next = 0;

        if (distanceSquared >= DPAD_DEADZONE * DPAD_DEADZONE) {
            double degrees = Math.toDegrees(Math.atan2(dy, dx));
            if (degrees < 0) degrees += 360.0;
            int sector = ((int)Math.floor((degrees + 22.5) / 45.0)) % 8;
            next = sector + 1;
        }

        dpadDirection = next;

        if (playerMovementLocked) {
            movingLeft = false;
            movingRight = false;
            crouching = false;
            forwardDashing = false;
            backDashTimer = 0f;
            return;
        }

        if (isTagAnimationActive()) {
            // Durante a animação só memorizamos a direção atualmente segurada.
            // Nenhum movimento, dash, pulo ou comando é executado até o fim da pose.
            movingLeft = false;
            movingRight = false;
            crouching = false;
            forwardDashing = false;
            backDashTimer = 0f;
            return;
        }

        movingLeft = next == 4 || next == 5 || next == 6;
        movingRight = next == 1 || next == 2 || next == 8;
        crouching = grounded && isDownDirection(next);

        if (next != 0 && next != previous) {
            recordCommandDirection(relativeDirection(next), nowMs);
        }

        boolean nextForward = isForwardHorizontalDirection(next);
        boolean previousForward = isForwardHorizontalDirection(previous);
        if (nextForward && !previousForward) {
            if (
                !"S".equals(attackType) &&
                pendingEnergyUntilMs < nowMs &&
                grounded &&
                nowMs - lastForwardTapMs <= DASH_DOUBLE_TAP_MS
            ) {
                forwardDashing = true;
                backDashTimer = 0f;
            }
            lastForwardTapMs = nowMs;
        }

        if (!nextForward) {
            forwardDashing = false;
        }

        boolean nextBack = isBackHorizontalDirection(next);
        boolean previousBack = isBackHorizontalDirection(previous);
        if (nextBack && !previousBack) {
            if (
                !"S".equals(attackType) &&
                grounded &&
                nowMs - lastBackTapMs <= DASH_DOUBLE_TAP_MS
            ) {
                backDashTimer = backDashDuration;
                forwardDashing = false;
            }
            lastBackTapMs = nowMs;
        }

        if (isDownDirection(next) && !isDownDirection(previous)) {
            lastDownInputMs = nowMs;
        }

        if (isUpDirection(next) && !isUpDirection(previous) && grounded) {
            boolean launcherChase = nowMs <= launcherChaseUntilMs;
            boolean superJump =
                launcherChase ||
                nowMs - lastDownInputMs <= 360L;

            if (launcherChase) {
                // Jump-cancel do 2H confirmado: sobe direto atrás do adversário.
                attackType = "";
                attackTimer = 0f;
                attackDuration = 0f;
                attackHitApplied = false;
                launcherChaseUntilMs = -1L;
                lastDownInputMs = -1000L;
            }

            startJump(superJump);
        }
    }

    private void clearDpad() {
        dpadDirection = 0;
        dpadPointer = -1;
        movingLeft = false;
        movingRight = false;
        crouching = false;
        forwardDashing = false;
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

    private boolean handleTouch(MotionEvent event) {
        if (getWidth()==0 || getHeight()==0) return true;
        float sx = getWidth() / VW;
        float sy = getHeight() / VH;
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        long nowMs = event.getEventTime();

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            int pointerId = event.getPointerId(index);
            float x = event.getX(index) / sx;
            float y = event.getY(index) / sy;

            float dx = x - DPAD_X;
            float dy = y - DPAD_Y;

            if (
                insideRect(
                    x,
                    y,
                    AI_BUTTON_LEFT,
                    AI_BUTTON_TOP,
                    AI_BUTTON_RIGHT,
                    AI_BUTTON_BOTTOM
                )
            ) {
                setOpponentAiEnabled(!opponentAiEnabled);
                return true;
            }

            if (isSuperCinematicActive() || isEnergyAttackActive()) {
                return true;
            }

            if (
                dpadPointer == -1 &&
                dx * dx + dy * dy <= DPAD_RADIUS * DPAD_RADIUS
            ) {
                dpadPointer = pointerId;
                updateDpad(x, y, nowMs);
            } else if (isTagAnimationActive()) {
                return true;
            } else if (insideCircle(x, y, SUPER_X, SUPER_Y, SUPER_RADIUS)) {
                if (canStartSuper()) {
                    superPointer = pointerId;
                    startSuperCinematic();
                }
            } else if (insideCircle(x, y, LIGHT_X, LIGHT_Y, ATTACK_RADIUS)) {
                lightPointer = pointerId;
                if (!tryFirePendingEnergy("L", nowMs)) {
                    resetAutoCombo();
                    startAttack(grounded && crouching ? "2L" : "L");
                }
            } else if (insideCircle(x, y, MEDIUM_X, MEDIUM_Y, ATTACK_RADIUS)) {
                mediumPointer = pointerId;
                if (!tryFirePendingEnergy("M", nowMs)) {
                    resetAutoCombo();
                    startAttack(grounded && crouching ? "2M" : "M");
                }
            } else if (insideCircle(x, y, HEAVY_X, HEAVY_Y, ATTACK_RADIUS)) {
                heavyPointer = pointerId;
                if (!tryFirePendingEnergy("H", nowMs)) {
                    resetAutoCombo();
                    startAttack(grounded && crouching ? "2H" : "H");
                }
            } else if (insideCircle(x, y, COMBO_X, COMBO_Y, COMBO_RADIUS)) {
                comboPointer = pointerId;
                triggerAutoCombo(nowMs);
            } else if (insideCircle(x, y, TAG_X, TAG_Y, TAG_RADIUS)) {
                if (canStartTag()) {
                    tagPointer = pointerId;
                    switchFighter();
                }
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (
                !isEnergyAttackActive() &&
                !isSuperCinematicActive() &&
                dpadPointer != -1
            ) {
                int pointerIndex = event.findPointerIndex(dpadPointer);
                if (pointerIndex >= 0) {
                    float x = event.getX(pointerIndex) / sx;
                    float y = event.getY(pointerIndex) / sy;
                    updateDpad(x, y, nowMs);
                }
            }
        } else if (
            action == MotionEvent.ACTION_UP ||
            action == MotionEvent.ACTION_POINTER_UP
        ) {
            int pointerId = event.getPointerId(index);

            if (pointerId == dpadPointer) {
                if (isEnergyAttackActive()) {
                    dpadPointer = -1;
                    dpadDirection = 0;
                    movingLeft = false;
                    movingRight = false;
                    crouching = false;
                    forwardDashing = false;
                } else {
                    clearDpad();
                }
            }
            if (pointerId == lightPointer) lightPointer = -1;
            if (pointerId == mediumPointer) mediumPointer = -1;
            if (pointerId == heavyPointer) heavyPointer = -1;
            if (pointerId == comboPointer) comboPointer = -1;
            if (pointerId == tagPointer) tagPointer = -1;
            if (pointerId == superPointer) superPointer = -1;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            clearDpad();
            lightPointer = -1;
            mediumPointer = -1;
            heavyPointer = -1;
            comboPointer = -1;
            tagPointer = -1;
            superPointer = -1;
        }

        return true;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
