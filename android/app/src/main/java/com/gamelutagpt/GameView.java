package com.gamelutagpt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    // Team and opponent rules come from the generated character packs.
    private final FighterState[] team = new FighterState[] {
        new FighterState(GeneratedCharacters.get(GeneratedCharacters.TEAM[0]), "PLAYER 1"),
        new FighterState(GeneratedCharacters.get(GeneratedCharacters.TEAM[1]), "PLAYER 2")
    };

    private int activeFighterIndex = 0;
    private static final int AI_OWNER_INDEX = 99;
    private final FighterState opponentFighter =
        new FighterState(GeneratedCharacters.opponentCharacter(), "CPU");
    private final List<Projectile> energyProjectiles = new ArrayList<>();
    private final List<Projectile> superProjectiles = new ArrayList<>();

    private static final float VW = Arena.VW;
    private static final float VH = Arena.VH;
    private static final float GROUND_Y = Arena.GROUND_Y;
    private static final float WORLD_WIDTH = Arena.WORLD_WIDTH;
    private static final float LEFT_BOUND = Arena.LEFT_BOUND;
    private static final float RIGHT_BOUND = Arena.RIGHT_BOUND;
    private static final float WORLD_TOP = Arena.WORLD_TOP;

    private static final float DUMMY_START_X = 980f;
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
    private int dummyLife = opponentFighter.profile.maxLife;
    private String dummyLifeHudLabel = dummyLife + " / " + dummyLife;
    private String dummyDamageLabel = "";
    private float dummyDamageLabelTimer = 0f;
    private float dummyHitReactionTimer = 0f;
    private float dummyKnockbackVelocityX = 0f;

    private boolean opponentAiEnabled = false;
    private float aiAttackCooldownRemaining = 0f;
    private boolean aiMovingForward = false;
    private boolean aiMovingBack = false;
    private boolean aiForwardDashing = false;
    private float aiBackDashTimer = 0f;
    private boolean aiCrouching = false;
    private boolean aiSuperJumping = false;
    private String dummyAttackType = "";
    private float dummyAttackTimer = 0f;
    private float dummyAttackDuration = 0f;
    private CharacterDefinition.Move opponentMove;
    private boolean dummyAttackHitApplied = false;
    private boolean aiChaseLauncher = false;
    private float aiSuperTimer = 0f;
    private String aiStatusLabel = "IA OFF";

    private final SurfaceHolder holder;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SpriteFighterRenderer spriteFighterRenderer;
    private final SpriteFighterRenderer opponentSpriteRenderer;
    private final StageRenderer stage = new StageRenderer();
    private final EffectsRenderer effects = new EffectsRenderer();
    private final HudRenderer hud = new HudRenderer();
    private final HudRenderer.OpponentPanel opponentPanel = new HudRenderer.OpponentPanel();
    private final OpponentAi opponentAi = new OpponentAi(new java.util.Random());
    private final OpponentAi.Situation aiSituation = new OpponentAi.Situation();

    private Thread gameThread;
    private volatile boolean running;

    private float playerX = 420f;
    private float playerY = GROUND_Y;
    private final CameraRig camera = new CameraRig();
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
    // Frame data of the attack in progress; null only for the energy special.
    private CharacterDefinition.Move activeMove;
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
    /** Holding back while airborne: blocks everything that can reach a jumping body. */
    private static final int GUARD_AIR = 3;
    private static final float BLOCKSTUN_DURATION = 0.18f;
    private static final float BLOCK_PUSH_SPEED = 72f;

    private static final float MAX_SUPER_METER = 5f;
    private static final float SUPER_COST = 1f;
    // Single version source: versionName in app/build.gradle.
    private static final String VERSION_HUD_LABEL =
        "SPRITE GPT • v" + BuildConfig.VERSION_NAME.split("-")[0];
    private static final float ENERGY_ATTACK_DURATION = 0.30f;
    private static final float AI_SUPER_WINDUP = 0.58f;

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

    private static final long ENERGY_CONFIRM_WINDOW_MS = 550L;
    private long pendingEnergyUntilMs = -1L;
    private final CommandBuffer commandBuffer = new CommandBuffer();

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
        // One decode per atlas for the whole match: team packs (tag never decodes
        // mid-fight) and the opponent share the same cache.
        SpriteAtlasCache atlases = new SpriteAtlasCache(context);
        for (FighterState fighter : team) atlases.preload(fighter.character);
        spriteFighterRenderer = new SpriteFighterRenderer(atlases, team[0].character);
        opponentSpriteRenderer = new SpriteFighterRenderer(
            atlases,
            opponentFighter.character
        );
        holder = getHolder();
        holder.addCallback(this);
        setFocusable(true);
        setKeepScreenOn(true);
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
        float previousDummyX = dummyX;
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
        updateOpponentSpriteMotion(dt, dummyX - previousDummyX);

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
        } else if (isTagAnimationActive() || (attackTimer > 0f && grounded)) {
            // Troca e ataques no chão travam o deslocamento horizontal.
        } else if (backDashTimer > 0f && grounded) {
            playerX -= facingDirection * backDashSpeed * dt;
            backDashTimer = Math.max(0f, backDashTimer - dt);
        } else if (direction != 0f && !crouching) {
            boolean movingForward = direction == facingDirection;
            float speed = (
                forwardDashing &&
                movingForward &&
                grounded
            ) ? forwardDashSpeed : moveSpeed;
            playerX += direction * speed * dt;
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
        resolveBodyPush();
        updateFacing();

        updateFightCamera(dt);
        updateSpriteMotion(dt,playerX-previousX);
    }

    /** Executes the AI's choices with the same rules a human-driven fighter uses. */
    private final OpponentAi.Actions aiActions = new OpponentAi.Actions() {
        @Override public void stop() {
            aiMovingForward = false;
            aiMovingBack = false;
            aiForwardDashing = false;
            aiCrouching = false;
        }
        @Override public void attack(String type) { startOpponentAttack(type); }
        @Override public void superAttack() { startOpponentSuper(); }
        @Override public void energy(String strength) { fireOpponentEnergy(strength); }
        @Override public void backdash() { aiBackDashTimer = AI_BACKDASH_DURATION; }
        @Override public void moveForward(boolean dash) {
            aiMovingForward = true;
            aiForwardDashing = dash;
        }
        @Override public void jump(boolean superJump) { startOpponentJump(superJump); }
    };

    private final HudRenderer.State hudState = new HudRenderer.State() {
        @Override public FighterState active() { return activeFighter(); }
        @Override public FighterState reserve() { return reserveFighter(); }
        @Override public String versionLabel() { return VERSION_HUD_LABEL; }
        @Override public String stateLabel() { return currentStateLabel(); }
        @Override public int facing() { return facingDirection; }
        @Override public boolean aiEnabled() { return opponentAiEnabled; }
        @Override public float tagReadyRatio() {
            if (isTagAnimationActive()) return 0f;
            if (tagCooldownRemaining > 0f) return 1f - tagCooldownRemaining / TAG_COOLDOWN_SECONDS;
            return 1f;
        }
        @Override public String tagCooldownLabel() { return tagCooldownHudLabel; }
        @Override public String tagButtonLabel() { return tagCooldownButtonLabel; }
        @Override public boolean canTag() { return canStartTag(); }
        @Override public boolean canSuper() { return canStartSuper(); }
        @Override public int dpadDirection() { return dpadDirection; }
        @Override public boolean pressed(ControlsLayout.Control control) {
            switch (control) {
                case LIGHT: return lightPointer != -1;
                case MEDIUM: return mediumPointer != -1;
                case HEAVY: return heavyPointer != -1;
                case COMBO: return comboPointer != -1;
                case TAG: return tagPointer != -1;
                case SUPER: return superPointer != -1;
                default: return false;
            }
        }
    };

    private final float[] pushResult = new float[2];

    /** Bodies never overlap: walking into the opponent pushes both (pushbox from the pack). */
    private void resolveBodyPush() {
        if (activeFighter().life <= 0 && dummyLife <= 0) return;
        if (CombatRules.resolvePush(
            playerX, playerY, activeFighter().profile.body,
            dummyX, dummyY, opponentProfile().body,
            LEFT_BOUND, RIGHT_BOUND, facingDirection, pushResult
        )) {
            playerX = pushResult[0];
            dummyX = pushResult[1];
        }
    }

    private CharacterDefinition.Fighter opponentProfile() {
        return opponentFighter.profile;
    }

    private CharacterDefinition opponentCharacter() {
        return opponentFighter.character;
    }

    private CharacterDefinition activeCharacter() {
        return activeFighter().character;
    }

    private boolean isOpponentCrouching() {
        return aiCrouching || dummyAttackType.startsWith("2");
    }

    private void updateOpponentSpriteMotion(float dt, float travel) {
        CharacterDefinition opponent = opponentCharacter();
        String attackAnimation = null;
        float attackElapsed = 0f;
        float elapsed = Math.max(0f, dummyAttackDuration - dummyAttackTimer);
        String reaction = reactionState(
            opponentSpriteRenderer,
            dummyKnockdownState,
            dummyKnockdownTimer,
            dummyLaunchedByHit,
            dummyGroundSlam,
            dummyVelocityY,
            dummyHitReactionTimer,
            dummyAirborne,
            isOpponentCrouching()
        );
        if (reaction != null) {
            attackAnimation = reaction;
            attackElapsed = reactionElapsed;
        } else if (aiSuperTimer > 0f) {
            CharacterDefinition.Animation roar = opponent.specialAnimations.get("SUPER");
            if (roar != null) {
                attackAnimation = roar.id;
                attackElapsed = roar.timeFor(AI_SUPER_WINDUP - aiSuperTimer, AI_SUPER_WINDUP);
            }
        } else if (dummyAttackTimer > 0f && "S".equals(dummyAttackType)) {
            CharacterDefinition.Animation special = opponent.specialAnimations.get("S");
            if (special != null) {
                attackAnimation = special.id;
                attackElapsed = special.timeFor(elapsed, dummyAttackDuration);
            }
        } else if (dummyAttackTimer > 0f && opponentMove != null && opponentMove.animation != null) {
            // Each input resolves to its own declared move; a move without art keeps
            // its declared pose (crouch flag or airborne physics) instead of borrowing L/M/H.
            attackAnimation = opponentMove.animation.id;
            attackElapsed = opponentMove.animationTime(elapsed);
        }

        boolean forward = Math.abs(travel) > 0.001f &&
            travel * opponentFacingDirection() > 0f;
        boolean combat =
            dummyHitReactionTimer > 0f ||
            aiSuperTimer > 0f ||
            dummyKnockdownState != DUMMY_KD_NONE;

        opponentSpriteRenderer.update(
            dt,
            !dummyAirborne,
            isOpponentCrouching(),
            dummyVelocityY,
            travel,
            forward,
            aiForwardDashing,
            aiBackDashTimer > 0f,
            attackAnimation,
            attackElapsed,
            combat,
            dummyMovementLocked
        );
    }

    private int opponentFacingDirection() {
        return -facingDirection;
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
        opponentAi.reset();
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
        opponentMove = null;
        dummyAttackHitApplied = false;
        aiSuperTimer = 0f;
    }

    private void startOpponentAttack(String type) {
        if (!canOpponentAct() || dummyAttackTimer > 0f) return;

        opponentMove = opponentCharacter().move(type, dummyAirborne);
        dummyAttackType = type;
        dummyAttackDuration = opponentMove.totalTime;
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
        CharacterDefinition.Fighter profile = opponentProfile();
        if (
            !canOpponentAct() ||
            !profile.hasEnergyAttack() ||
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

        opponentMove = null;
        dummyAttackType = "S";
        dummyAttackDuration = ENERGY_ATTACK_DURATION;
        dummyAttackTimer = dummyAttackDuration;
        dummyAttackHitApplied = true;

        if (dummyAirborne) dummyVelocityY *= 0.32f;

        int direction = opponentFacingDirection();
        energyProjectiles.add(new Projectile(
            dummyX + direction * profile.energy.spawnX,
            dummyY - profile.energy.spawnHeight(aiCrouching, dummyAirborne),
            profile.energy.range,
            profile.energy.speed * speedMultiplier,
            Math.round(profile.energy.damage * damageMultiplier),
            profile.color,
            AI_OWNER_INDEX,
            direction
        ));
        aiAttackCooldownRemaining = 0.40f;
    }

    private void startOpponentSuper() {
        CharacterDefinition.Fighter profile = opponentProfile();
        if (
            !canOpponentAct() ||
            !profile.hasSuperAttack() ||
            opponentFighter.superMeter < SUPER_COST
        ) return;

        opponentFighter.superMeter = Math.max(
            0f,
            opponentFighter.superMeter - SUPER_COST
        );
        opponentFighter.refreshHudLabels();
        aiSuperTimer = AI_SUPER_WINDUP;
        aiMovingForward = false;
        aiMovingBack = false;
        aiForwardDashing = false;
        aiBackDashTimer = 0f;
        dummyAttackType = "";
        dummyAttackTimer = 0f;
        opponentMove = null;
    }

    private void spawnOpponentSuperProjectile() {
        CharacterDefinition.Fighter profile = opponentProfile();
        int direction = opponentFacingDirection();
        superProjectiles.add(new Projectile(
            dummyX + direction * profile.superAttack.spawnX,
            dummyY - profile.superAttack.spawnHeight(false, dummyAirborne),
            profile.superAttack.range,
            profile.superAttack.speed,
            profile.superAttack.damage,
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
                opponentMove = null;
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
            return;
        }

        aiSituation.self = opponentCharacter();
        aiSituation.target = activeCharacter().fighter.body;
        aiSituation.distance = distance;
        aiSituation.verticalDistance = verticalDistance;
        aiSituation.selfAirborne = dummyAirborne;
        aiSituation.targetGrounded = grounded;
        aiSituation.targetAlive = activeFighter().life > 0;
        aiSituation.superReady = opponentFighter.superMeter >= SUPER_COST;
        aiSituation.projectileActive = hasActiveEnergyProjectile(AI_OWNER_INDEX);
        aiSituation.attackReady = aiAttackCooldownRemaining <= 0f;
        opponentAi.think(dt, aiSituation, aiActions);

        if (dummyAirborne) {
            if (aiMovingForward) {
                dummyX += opponentFacingDirection() * AI_WALK_SPEED * 0.70f * dt;
            }
        } else if (aiMovingForward) {
            float speed = aiForwardDashing ? AI_DASH_SPEED : AI_WALK_SPEED;
            dummyX += opponentFacingDirection() * speed * dt;
        } else if (aiMovingBack) {
            dummyX -= opponentFacingDirection() * AI_WALK_SPEED * dt;
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

        CharacterDefinition.Move move = opponentMove;
        if (move == null) return;
        if (!move.active(dummyAttackDuration - dummyAttackTimer)) return;

        int direction = opponentFacingDirection();
        if (!CombatRules.meleeConnects(
            dummyX, dummyY, direction, move,
            playerX, playerY, activeCharacter().fighter.body, isPlayerCrouching()
        )) return;

        int damage = move.damage;
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

    /** Highest opaque pixel of the opponent, for camera framing and its HUD. */
    private float opponentVisualTop() {
        CharacterDefinition opponent = opponentCharacter();
        return dummyY - (isOpponentCrouching()
            ? opponent.visualCrouchHeight
            : opponent.visualStandHeight);
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
            activeFighter().profile.hasSuperAttack() &&
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
        CharacterDefinition.Fighter profile = activeFighter().profile;
        if (!profile.hasSuperAttack()) return;

        float spawnY = playerY - profile.superAttack.spawnHeight(false, !grounded);
        superProjectiles.add(new Projectile(
            playerX + facingDirection * profile.superAttack.spawnX,
            spawnY,
            profile.superAttack.range,
            profile.superAttack.speed,
            profile.superAttack.damage,
            profile.color,
            activeFighterIndex,
            facingDirection
        ));
    }

    private void updateSuperProjectiles(float dt) {
        Iterator<Projectile> iterator = superProjectiles.iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
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
                spriteFighterRenderer.setCharacter(activeCharacter().id);
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

    private void startAttack(String type) {
        if (playerMovementLocked) return;

        attackType = type;
        attackHitApplied = false;
        forwardDashing = false;
        backDashTimer = 0f;

        if ("S".equals(type)) {
            activeMove = null;
            attackDuration = ENERGY_ATTACK_DURATION;
        } else {
            // Gameplay timing comes from the pack's frame data, not from the art.
            activeMove = activeCharacter().move(type, !grounded);
            attackDuration = activeMove.totalTime;
        }
        attackTimer = attackDuration;
    }

    private boolean isPlayerCrouching() {
        return crouching || isCrouchAttackActive();
    }

    private void tryApplyMeleeDamage() {
        if (attackHitApplied || attackTimer <= 0f || dummyLife <= 0) return;
        if ("S".equals(attackType)) return;

        CharacterDefinition.Move move = activeMove;
        if (move == null) return;
        if (!move.active(attackDuration - attackTimer)) return;
        if (!CombatRules.meleeConnects(
            playerX, playerY, facingDirection, move,
            dummyX, dummyY, opponentProfile().body, isOpponentCrouching()
        )) return;
        int damage = move.damage;

        addSuperMeter(activeFighter(), CombatRules.superGainForAttack(attackType));
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
        opponentMove = null;
        dummyAttackHitApplied = false;
        aiAttackCooldownRemaining = Math.max(
            aiAttackCooldownRemaining,
            0.35f
        );

        int applied = Math.min(damage, dummyLife);
        dummyLife -= applied;
        opponentFighter.life = dummyLife;
        opponentFighter.refreshHudLabels();
        dummyLifeHudLabel = dummyLife + " / " + opponentProfile().maxLife;
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
        return CombatRules.projectileHits(
            previousX, nextX, y, radius,
            playerX, playerY, activeFighter().profile.body, isPlayerCrouching()
        );
    }

    private boolean projectileHitsDummy(
        float previousX,
        float nextX,
        float y,
        float radius
    ) {
        if (dummyLife <= 0) return false;
        return CombatRules.projectileHits(
            previousX, nextX, y, radius,
            dummyX, dummyY, opponentProfile().body, isOpponentCrouching()
        );
    }

    private boolean hasActiveEnergyProjectile(int ownerIndex) {
        for (Projectile projectile : energyProjectiles) {
            if (projectile.ownerIndex == ownerIndex) return true;
        }
        return false;
    }

    private void fireEnergyAttack(String strength) {
        CharacterDefinition.Fighter profile = activeFighter().profile;
        if (!profile.hasEnergyAttack()) return;
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

        float spawnY = playerY - profile.energy.spawnHeight(crouching, !grounded);
        energyProjectiles.add(new Projectile(
            playerX + facingDirection * profile.energy.spawnX,
            spawnY,
            profile.energy.range,
            profile.energy.speed * speedMultiplier,
            Math.round(profile.energy.damage * damageMultiplier),
            profile.color,
            activeFighterIndex,
            facingDirection
        ));

    }

    private boolean tryFirePendingEnergy(String attackButton, long nowMs) {
        CharacterDefinition.Fighter profile = activeFighter().profile;

        if (
            playerMovementLocked ||
            !profile.hasEnergyAttack() ||
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
        Iterator<Projectile> iterator = energyProjectiles.iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
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
                        CombatRules.superGainForAttack("S")
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
        commandBuffer.record(direction, nowMs);
        tryEnergyCommand(nowMs);
    }

    private void tryEnergyCommand(long nowMs) {
        CharacterDefinition.Fighter profile = activeFighter().profile;
        if (
            !profile.hasEnergyAttack() ||
            profile.energyCommand.length == 0 ||
            hasActiveEnergyProjectile(activeFighterIndex)
        ) {
            pendingEnergyUntilMs = -1L;
            return;
        }
        if (attackTimer > 0f) return;
        if (!commandBuffer.consume(profile.energyCommand, nowMs)) return;

        // A sequência apenas arma o especial. L/M/H decide a força do projétil.
        pendingEnergyUntilMs = nowMs + ENERGY_CONFIRM_WINDOW_MS;
        forwardDashing = false;
        backDashTimer = 0f;
    }

    private void resetCommandBuffer() {
        commandBuffer.reset();
    }

    private void resetAutoCombo() {
        autoComboIndex = 0;
        lastAutoComboTapMs = -1000L;
    }

    private void triggerAutoCombo(long nowMs) {
        String[] combo = activeFighter().profile.autoCombo;
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
        if (!grounded) {
            // Back, down-back or up-back in the air.
            return relative == 4 || relative == 5 || relative == 6 ? GUARD_AIR : GUARD_NONE;
        }
        if (relative == 5) return GUARD_HIGH;
        if (relative == 4) return GUARD_LOW;
        return GUARD_NONE;
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
            return !CombatRules.isLowAttack(type);
        }

        if (guard == GUARD_LOW) {
            return !attackerAirborne;
        }

        // Low attacks cannot reach a jumping body, so the air guard covers the rest.
        return guard == GUARD_AIR;
    }

    private boolean incomingAiProjectileThreat() {
        int guard = currentPlayerGuardState();
        if (guard == GUARD_NONE) return false;

        for (Projectile projectile : energyProjectiles) {
            if (projectile.ownerIndex != AI_OWNER_INDEX) continue;

            float toPlayer = playerX - projectile.x;
            if (toPlayer * projectile.direction >= -24f) {
                return true;
            }
        }

        for (Projectile projectile : superProjectiles) {
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

        if (opponentMove == null) return false;
        return CombatRules.meleeThreatens(
            dummyX,
            opponentFacingDirection(),
            opponentMove,
            dummyAttackDuration - dummyAttackTimer,
            playerX,
            activeCharacter().fighter.body
        );
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
        addSuperMeter(activeFighter(), CombatRules.superGainForGuard(type));

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
        addSuperMeter(opponentFighter, CombatRules.superGainForAttack(type));
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

    private void updateFightCamera(float dt) {
        // Frame what is actually drawn: opaque sprite height measured at import time.
        float playerTop = playerY - (isPlayerCrouching()
            ? activeCharacter().visualCrouchHeight
            : activeCharacter().visualStandHeight);
        float highestFighterTop = Math.min(playerTop, opponentVisualTop());
        camera.update(dt, playerX, dummyX, highestFighterTop, superJumping || aiSuperJumping);
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

            float renderZoom = camera.zoom * superCameraZoom;
            float visibleWorldWidth = VW / renderZoom;
            float cameraLeft = clamp(camera.x - visibleWorldWidth / 2f, 0f, WORLD_WIDTH - visibleWorldWidth);

            canvas.save();
            canvas.scale(renderZoom, renderZoom);
            canvas.translate(-cameraLeft, -camera.top);
            drawScenario(canvas);
            drawDamageDummy(canvas);
            effects.drawEnergyProjectiles(canvas, paint, energyProjectiles);
            effects.drawSuperProjectiles(canvas, paint, superProjectiles);
            if (isSuperCinematicActive()) {
                effects.drawWorldOverlay(canvas, paint, superDarkAlpha, 0, 0, 8);
                canvas.save();
                if (facingDirection < 0) {
                    canvas.scale(-1f, 1f, playerX, 0f);
                }
                effects.drawSuperCharge(
                    canvas, paint, playerX, playerY, superPhaseTimer,
                    activeFighter().profile.color
                );
                canvas.restore();
            }

            canvas.save();
            canvas.translate(tagVisualOffsetX, 0f);
            drawPlayer(canvas);
            canvas.restore();
            if (isSuperCinematicActive()) {
                effects.drawWorldOverlay(canvas, paint, superFlashAlpha, 255, 255, 255);
            }
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
        stage.draw(c, paint);
    }

    private void drawHud(Canvas c) {
        hud.drawHud(c, hudState);
    }

    private String currentStateLabel() {
        if (playerKnockdownState != DUMMY_KD_NONE) return "DERRUBADO";
        if (playerBlockstunTimer > 0f) {
            if (playerLastGuardState == GUARD_AIR) return "BLOQUEIO NO AR";
            return playerLastGuardState == GUARD_LOW
                ? "BLOQUEIO BAIXO"
                : "BLOQUEIO ALTO";
        }
        int anticipatedGuard = anticipatedPlayerGuardPose();
        if (anticipatedGuard == GUARD_LOW) return "DEFENDENDO BAIXO";
        if (anticipatedGuard == GUARD_HIGH) return "DEFENDENDO ALTO";
        if (anticipatedGuard == GUARD_AIR) return "DEFENDENDO NO AR";

        int guard = currentPlayerGuardState();
        if (guard == GUARD_LOW) return "PRONTO BAIXO";
        if (guard == GUARD_HIGH) return "PRONTO ALTO";
        if (guard == GUARD_AIR) return "PRONTO NO AR";
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

    private void drawDamageDummy(Canvas c) {
        float top = opponentVisualTop();
        float baseY = dummyY;

        paint.setColor(Color.argb(70, 0, 0, 0));
        c.drawOval(
            dummyX - 43f,
            GROUND_Y - 10f,
            dummyX + 43f,
            GROUND_Y + 10f,
            paint
        );

        boolean hitFlash =
            dummyHitReactionTimer > DUMMY_HIT_REACTION_DURATION - 0.065f;

        float angle = knockdownAngle(
            opponentSpriteRenderer,
            dummyKnockdownState,
            dummyKnockdownTimer,
            opponentFacingDirection()
        );
        c.save();
        if (angle != 0f) c.rotate(angle, dummyX, GROUND_Y);
        opponentSpriteRenderer.draw(
            c,
            dummyX,
            baseY,
            opponentFacingDirection(),
            hitFlash && dummyLife > 0,
            false
        );
        c.restore();

        opponentPanel.x = dummyX;
        opponentPanel.visualTop = top;
        opponentPanel.lifeRatio = dummyLife / (float)opponentProfile().maxLife;
        opponentPanel.status = opponentStatusLabel();
        opponentPanel.lifeLabel = dummyLifeHudLabel;
        opponentPanel.superLabel = opponentFighter.superHudLabel;
        opponentPanel.damageLabel = dummyDamageLabel;
        opponentPanel.damageProgress = dummyDamageLabelTimer > 0f
            ? dummyDamageLabelTimer / 0.72f
            : 0f;
        hud.drawOpponentPanel(c, opponentPanel);
    }

    private String opponentStatusLabel() {
        String name = opponentCharacter().displayName.toUpperCase(java.util.Locale.ROOT);
        String state;
        if (dummyKnockdownState == DUMMY_KD_FALL) state = "CAINDO";
        else if (dummyKnockdownState == DUMMY_KD_DOWN) state = "NO CHÃO";
        else if (dummyKnockdownState == DUMMY_KD_GETUP) state = "LEVANTANDO";
        else if (aiSuperTimer > 0f) state = "SUPER";
        else if (dummyAttackTimer > 0f) state = dummyAttackType;
        else if (dummyGroundSlam) state = "QUEDA FORÇADA";
        else if (dummyMovementLocked) state = "SEM CONTROLE";
        else if (dummyAirborne) state = "NO AR";
        else state = opponentAiEnabled ? "IA" : "PARADO";
        return name + " • " + state;
    }

    private float reactionElapsed;

    /**
     * Reaction states shared by every fighter: knockdown, launch and hit stun. Returns
     * null when the pack has no art for the current reaction (the caller keeps its pose).
     */
    private String reactionState(
        SpriteFighterRenderer renderer,
        int knockdownState,
        float knockdownTimer,
        boolean launched,
        boolean slam,
        float verticalVelocity,
        float hitTimer,
        boolean airborne,
        boolean crouch
    ) {
        String state = null;
        reactionElapsed = knockdownTimer;
        if (knockdownState == DUMMY_KD_FALL) state = SpriteStates.KNOCKDOWN;
        else if (knockdownState == DUMMY_KD_DOWN) state = SpriteStates.GROUNDED;
        else if (knockdownState == DUMMY_KD_GETUP) state = SpriteStates.GETUP;
        else if (launched || slam) {
            state = SpriteStates.HIT_AIR;
            reactionElapsed = launchPoseTime(verticalVelocity, slam);
        } else if (hitTimer > 0f) {
            state = airborne
                ? SpriteStates.HIT_AIR
                : (crouch ? SpriteStates.HIT_CROUCH : SpriteStates.HIT_STAND);
            reactionElapsed = DUMMY_HIT_REACTION_DURATION - hitTimer;
        }
        return state != null && renderer.hasAnimation(state) ? state : null;
    }

    /**
     * Launch poses follow the physics instead of a clock: thrown up, apex, forced dive
     * (ground slam), then the recovery tuck once control returns on the way down. The
     * times index a HIT_AIR clip of up to four 55 ms frames; shorter clips clamp.
     */
    private static float launchPoseTime(float verticalVelocity, boolean slam) {
        if (slam) return 0.13f;
        if (verticalVelocity < -500f) return 0f;
        if (verticalVelocity < 0f) return 0.07f;
        return 0.19f;
    }

    /** Vector fallback for packs without KNOCKDOWN/GROUNDED/GETUP: fall backwards. */
    private float knockdownAngle(
        SpriteFighterRenderer renderer,
        int knockdownState,
        float knockdownTimer,
        int fighterFacing
    ) {
        if (renderer.hasAnimation(SpriteStates.KNOCKDOWN)) return 0f;
        float lean;
        if (knockdownState == DUMMY_KD_FALL) {
            lean = clamp(knockdownTimer / DUMMY_KD_FALL_DURATION, 0f, 1f);
        } else if (knockdownState == DUMMY_KD_DOWN) {
            lean = 1f;
        } else if (knockdownState == DUMMY_KD_GETUP) {
            lean = 1f - clamp(knockdownTimer / DUMMY_KD_GETUP_DURATION, 0f, 1f);
        } else {
            return 0f;
        }
        return -88f * lean * fighterFacing;
    }

    private void updateSpriteMotion(float dt, float travel) {
        int guard = playerBlockstunTimer > 0f
            ? playerLastGuardState
            : anticipatedPlayerGuardPose();
        CharacterDefinition character = activeCharacter();

        String animation = reactionState(
            spriteFighterRenderer,
            playerKnockdownState,
            playerKnockdownTimer,
            playerLaunchedByHit,
            playerGroundSlam,
            velocityY,
            playerHitReactionTimer,
            !grounded,
            crouching
        );
        float animationElapsed = reactionElapsed;

        if (animation == null && guard != GUARD_NONE) {
            String defense = guard == GUARD_LOW
                ? SpriteStates.DEFENSE_CROUCH
                : guard == GUARD_AIR
                    ? SpriteStates.DEFENSE_AIR
                    : SpriteStates.DEFENSE_STAND;
            if (spriteFighterRenderer.hasAnimation(defense)) {
                animation = defense;
                // Held guard while a threat approaches; impact and recovery during blockstun.
                animationElapsed = character.animation(defense).guardTime(
                    playerBlockstunTimer > 0f,
                    1f - playerBlockstunTimer / BLOCKSTUN_DURATION
                );
            }
        }
        if (animation == null && isSuperPoseActive()) {
            CharacterDefinition.Animation special = character.specialAnimations.get("SUPER");
            if (special != null) {
                animation = special.id;
                animationElapsed = special.timeFor(superPoseElapsed(), SUPER_POSE_TOTAL);
            }
        } else if (animation == null && attackTimer > 0f) {
            float elapsed = attackDuration - attackTimer;
            if (activeMove != null && activeMove.animation != null) {
                animation = activeMove.animation.id;
                animationElapsed = activeMove.animationTime(elapsed);
            } else if (activeMove == null) {
                CharacterDefinition.Animation special = character.specialAnimations.get("S");
                if (special != null) {
                    animation = special.id;
                    animationElapsed = special.timeFor(elapsed, attackDuration);
                }
            }
            // Otherwise the move declared a pose: crouch flag or airborne physics below.
        }

        boolean combatPose =
            animation == null && (
                attackTimer > 0f ||
                guard != GUARD_NONE ||
                isSuperPoseActive() ||
                isTagAnimationActive()
            );

        spriteFighterRenderer.update(
            dt,
            grounded,
            isPlayerCrouching() || guard == GUARD_LOW,
            velocityY,
            travel,
            travel * facingDirection > 0,
            forwardDashing,
            backDashTimer > 0,
            animation,
            animationElapsed,
            combatPose,
            playerMovementLocked
        );
    }

    private static final float SUPER_POSE_TOTAL =
        SUPER_POSE_DURATION + SUPER_FLASH_DURATION + SUPER_RELEASE_DURATION;

    private float superPoseElapsed() {
        if (superPhase == SUPER_POSE) return superPhaseTimer;
        if (superPhase == SUPER_FLASH) return SUPER_POSE_DURATION + superPhaseTimer;
        if (superPhase == SUPER_RELEASE) {
            return SUPER_POSE_DURATION + SUPER_FLASH_DURATION + superPhaseTimer;
        }
        return 0f;
    }

    private void drawPlayer(Canvas c) {
        float angle = knockdownAngle(
            spriteFighterRenderer,
            playerKnockdownState,
            playerKnockdownTimer,
            facingDirection
        );
        c.save();
        if (angle != 0f) c.rotate(angle, playerX, GROUND_Y);
        spriteFighterRenderer.draw(
            c,
            playerX,
            playerY,
            facingDirection,
            playerDamageFlashTimer > 0,
            playerBlockFlashTimer > 0
        );
        c.restore();
    }

    private void drawControls(Canvas c) {
        hud.drawControls(c, hudState);
    }

    private boolean isDownDirection(int direction) {
        return ControlsLayout.isDownDirection(direction);
    }

    private boolean isUpDirection(int direction) {
        return ControlsLayout.isUpDirection(direction);
    }

    private void updateDpad(float x, float y, long nowMs) {
        if (isEnergyAttackActive() || isSuperCinematicActive()) return;

        int previous = dpadDirection;
        int next = ControlsLayout.dpadDirectionAt(x, y);

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

    /** L/M/H confirm a pending energy command, otherwise start the (crouching) normal. */
    private void pressAttack(String button, long nowMs) {
        if (tryFirePendingEnergy(button, nowMs)) return;
        resetAutoCombo();
        startAttack(grounded && crouching ? "2" + button : button);
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

            ControlsLayout.Control control = ControlsLayout.controlAt(x, y);

            if (control == ControlsLayout.Control.AI_TOGGLE) {
                setOpponentAiEnabled(!opponentAiEnabled);
                return true;
            }

            if (isSuperCinematicActive() || isEnergyAttackActive()) {
                return true;
            }

            if (control == ControlsLayout.Control.DPAD) {
                // A second finger on the D-pad is ignored; the first one owns it.
                if (dpadPointer == -1) {
                    dpadPointer = pointerId;
                    updateDpad(x, y, nowMs);
                }
                return true;
            }
            if (isTagAnimationActive()) return true;

            switch (control) {
                case SUPER:
                    if (canStartSuper()) {
                        superPointer = pointerId;
                        startSuperCinematic();
                    }
                    break;
                case LIGHT:
                    lightPointer = pointerId;
                    pressAttack("L", nowMs);
                    break;
                case MEDIUM:
                    mediumPointer = pointerId;
                    pressAttack("M", nowMs);
                    break;
                case HEAVY:
                    heavyPointer = pointerId;
                    pressAttack("H", nowMs);
                    break;
                case COMBO:
                    comboPointer = pointerId;
                    triggerAutoCombo(nowMs);
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
        return Arena.clamp(value, min, max);
    }
}
