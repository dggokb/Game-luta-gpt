package com.gamelutagpt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final class FighterProfile {
        final String name;
        final String[] baseCombo;
        final int maxLife;
        final int color;
        final boolean hasEnergyAttack;
        final float energyRange;
        final float energySpeed;
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

        EnergyProjectile(
            float x,
            float y,
            float range,
            float speed,
            int damage,
            int color,
            int ownerIndex
        ) {
            this.x = x;
            this.y = y;
            this.startX = x;
            this.range = range;
            this.speed = speed;
            this.damage = damage;
            this.color = color;
            this.ownerIndex = ownerIndex;
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

        SuperProjectile(
            float x,
            float y,
            float range,
            float speed,
            int damage,
            int color,
            int ownerIndex
        ) {
            this.x = x;
            this.y = y;
            this.startX = x;
            this.range = range;
            this.speed = speed;
            this.damage = damage;
            this.color = color;
            this.ownerIndex = ownerIndex;
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
            650,
            new int[]{5, 1},
            true,
            1320f,
            1280f,
            2850
        ))
    };

    private int activeFighterIndex = 0;
    private final List<EnergyProjectile> energyProjectiles = new ArrayList<>();
    private final List<SuperProjectile> superProjectiles = new ArrayList<>();

    private static final float VW = 1280f;
    private static final float VH = 720f;
    private static final float GROUND_Y = 565f;
    private static final float WORLD_WIDTH = 2600f;
    private static final float LEFT_BOUND = 90f;
    private static final float RIGHT_BOUND = WORLD_WIDTH - 90f;
    private static final float CAMERA_ZOOM = 1.12f;
    private static final float GROUND_CAMERA_TOP = 72f;
    private static final float WORLD_TOP = -520f;

    private final SurfaceHolder holder;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final LinearGradient skyGradient;
    private final android.graphics.Path[] mountainPaths =
        new android.graphics.Path[15];

    private Thread gameThread;
    private volatile boolean running;

    private float playerX = 420f;
    private float playerY = GROUND_Y;
    private float cameraX = 420f;
    private float cameraTop = GROUND_CAMERA_TOP;
    private float velocityY = 0f;

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
    private float walkTime = 0f;

    private final float moveSpeed = 300f;
    private final float forwardDashSpeed = 620f;
    private final float backDashSpeed = 760f;
    private final float backDashDuration = 0.20f;
    private final float jumpSpeed = 660f;
    private final float superJumpSpeed = 1450f;
    private final float gravity = 1650f;

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

    // 0 neutro, 1 direita, 2 baixo-direita, 3 baixo, 4 baixo-esquerda,
    // 5 esquerda, 6 cima-esquerda, 7 cima, 8 cima-direita.
    private int dpadDirection = 0;
    private int dpadPointer = -1;
    private long lastDownInputMs = -1000L;
    private long lastForwardTapMs = -1000L;
    private long lastBackTapMs = -1000L;
    private static final long DASH_DOUBLE_TAP_MS = 300L;

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

    public GameView(Context context) {
        super(context);
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
        if (running) return;

        running = true;
        gameThread = new Thread(this, "GameLoop");
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        running = false;

        Thread thread = gameThread;
        if (thread != null) {
            try {
                thread.join(800);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        gameThread = null;
    }

    @Override
    public void run() {
        long previous = System.nanoTime();
        final long targetFrame = 16_666_667L;

        while (running) {
            long frameStart = System.nanoTime();
            float dt = Math.min(0.033f, (frameStart - previous) / 1_000_000_000f);
            previous = frameStart;

            update(dt);
            drawFrame();

            long elapsed = System.nanoTime() - frameStart;
            long remaining = targetFrame - elapsed;
            if (remaining > 0) {
                try {
                    long ms = remaining / 1_000_000L;
                    int ns = (int)(remaining % 1_000_000L);
                    Thread.sleep(ms, ns);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void update(float dt) {
        updateSuperState(dt);
        updateTagState(dt);

        if (isSuperCinematicActive()) {
            updateSuperProjectiles(dt);
            return;
        }

        if (attackTimer > 0f) {
            attackTimer = Math.max(0f, attackTimer - dt);
            if (attackTimer <= 0f) attackType = "";
        }

        updateEnergyProjectiles(dt);
        updateSuperProjectiles(dt);

        float direction = 0f;
        if (!isEnergyAttackActive() && !isTagAnimationActive()) {
            if (movingLeft && !movingRight) direction = -1f;
            if (movingRight && !movingLeft) direction = 1f;
        }

        if (isTagAnimationActive()) {
            walkTime = 0f;
        } else if (attackTimer > 0f && grounded) {
            // No chão, ataques travam o deslocamento horizontal.
            walkTime = 0f;
        } else if (backDashTimer > 0f && grounded) {
            playerX -= backDashSpeed * dt;
            backDashTimer = Math.max(0f, backDashTimer - dt);
            walkTime += dt * 13f;
        } else if (direction != 0f && !crouching) {
            float speed = (forwardDashing && direction > 0f && grounded) ? forwardDashSpeed : moveSpeed;
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
            }
        }

        playerX = clamp(playerX, LEFT_BOUND, RIGHT_BOUND);

        float visibleWorldWidth = VW / CAMERA_ZOOM;
        float halfVisible = visibleWorldWidth / 2f;
        float targetCameraX = clamp(playerX, halfVisible, WORLD_WIDTH - halfVisible);
        float follow = 1f - (float)Math.pow(0.001f, dt);
        cameraX += (targetCameraX - cameraX) * follow;

        float targetCameraTop = GROUND_CAMERA_TOP;
        if (!grounded && playerY < 250f) {
            targetCameraTop = clamp(playerY - 185f, WORLD_TOP, GROUND_CAMERA_TOP);
        }
        float verticalFollow = 1f - (float)Math.pow(0.00035f, dt);
        cameraTop += (targetCameraTop - cameraTop) * verticalFollow;
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
        return activeFighter().profile.hasSuperAttack &&
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
            playerX + 78f,
            spawnY,
            profile.superRange,
            profile.superSpeed,
            profile.superDamage,
            profile.color,
            activeFighterIndex
        ));
    }

    private void updateSuperProjectiles(float dt) {
        Iterator<SuperProjectile> iterator = superProjectiles.iterator();
        while (iterator.hasNext()) {
            SuperProjectile projectile = iterator.next();
            projectile.x += projectile.speed * dt;

            if (
                projectile.x - projectile.startX >= projectile.range ||
                projectile.x > RIGHT_BOUND + 180f
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
            tagVisualOffsetX = -TAG_TRAVEL_DISTANCE * eased;

            if (t >= 1f) {
                activeFighterIndex = (activeFighterIndex + 1) % team.length;
                tagPhase = TAG_ENTER;
                tagPhaseTimer = 0f;
                tagVisualOffsetX = -TAG_TRAVEL_DISTANCE;
            }
        } else if (tagPhase == TAG_ENTER) {
            float t = clamp(tagPhaseTimer / TAG_ENTER_DURATION, 0f, 1f);
            float eased = 1f - (1f - t) * (1f - t);
            tagVisualOffsetX = -TAG_TRAVEL_DISTANCE * (1f - eased);

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

        tagPhase = TAG_EXIT;
        tagPhaseTimer = 0f;
        tagVisualOffsetX = 0f;
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
        return 0f;
    }

    private void startAttack(String type) {
        attackType = type;
        forwardDashing = false;
        backDashTimer = 0f;

        if ("L".equals(type) || "2L".equals(type)) attackDuration = 0.16f;
        else if ("M".equals(type) || "2M".equals(type)) attackDuration = 0.26f;
        else if ("S".equals(type)) attackDuration = 0.30f;
        else attackDuration = 0.40f;

        attackTimer = attackDuration;

        float superGain = superGainForAttack(type);
        if (superGain > 0f) {
            addSuperMeter(activeFighter(), superGain);
        }
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
            playerX + 62f,
            spawnY,
            profile.energyRange,
            profile.energySpeed * speedMultiplier,
            Math.round(profile.energyDamage * damageMultiplier),
            profile.color,
            activeFighterIndex
        ));

        addSuperMeter(activeFighter(), SUPER_GAIN_ENERGY);
    }

    private boolean tryFirePendingEnergy(String attackButton, long nowMs) {
        FighterProfile profile = activeFighter().profile;

        if (
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
            projectile.x += projectile.speed * dt;

            if (
                projectile.x - projectile.startX >= projectile.range ||
                projectile.x > RIGHT_BOUND + 120f
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
        fighter.life = Math.max(0, fighter.life - damage);
        fighter.refreshHudLabels();
    }

    private float attackPhase() {
        if (attackTimer <= 0f || attackDuration <= 0f) return 0f;
        float t = 1f - attackTimer / attackDuration;
        return t < 0.5f ? t * 2f : (1f - t) * 2f;
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

            float renderZoom = CAMERA_ZOOM * superCameraZoom;
            float visibleWorldWidth = VW / renderZoom;
            float cameraLeft = clamp(cameraX - visibleWorldWidth / 2f, 0f, WORLD_WIDTH - visibleWorldWidth);

            canvas.save();
            canvas.scale(renderZoom, renderZoom);
            canvas.translate(-cameraLeft, -cameraTop);
            drawScenario(canvas);
            drawEnergyProjectiles(canvas);
            drawSuperProjectiles(canvas);
            drawSuperDarkening(canvas);
            drawSuperChargeEffects(canvas);
            canvas.save();
            canvas.translate(tagVisualOffsetX, 0f);
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
        c.drawText("CORE REFINEMENT • v0.23", 975, 59, paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(16);

        c.drawText("Estado:", 975, 88, paint);
        c.drawText(currentStateLabel(), 1032, 88, paint);
    }

    private String currentStateLabel() {
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

    private void drawEnergyProjectiles(Canvas c) {
        for (EnergyProjectile projectile : energyProjectiles) {
            paint.setColor(Color.argb(75, 255, 255, 255));
            c.drawCircle(projectile.x, projectile.y, 29f, paint);

            paint.setColor(projectile.color);
            c.drawCircle(projectile.x, projectile.y, 20f, paint);

            paint.setColor(Color.WHITE);
            c.drawCircle(projectile.x + 5f, projectile.y - 5f, 8f, paint);
        }
    }

    private void drawSuperProjectiles(Canvas c) {
        for (SuperProjectile projectile : superProjectiles) {
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
            c.drawRect(projectile.x - 135f, projectile.y - 9f, projectile.x - 38f, projectile.y + 9f, paint);
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

    private void drawPlayer(Canvas c) {
        float bob = (grounded && !crouching && (movingLeft || movingRight))
            ? (float)Math.sin(walkTime) * 3f
            : 0f;

        float baseY = playerY + bob;
        boolean crouchPose = crouching || isCrouchAttackActive();
        float bodyHeight = crouchPose ? 90f : 145f;
        float top = baseY - bodyHeight;

        paint.setColor(Color.argb(70, 0, 0, 0));
        c.drawOval(playerX - 43, GROUND_Y - 10, playerX + 43, GROUND_Y + 10, paint);

        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(activeFighter().profile.color);
        c.drawCircle(playerX, top + 20, 25, paint);

        paint.setStrokeWidth(crouchPose ? 22 : 25);
        c.drawLine(playerX, top + 48, playerX, baseY - 45, paint);

        float legSwing = (grounded && !crouching && (movingLeft || movingRight))
            ? (float)Math.sin(walkTime) * 18f
            : 0f;

        float armSwing = -legSwing * 0.75f;

        paint.setStrokeWidth(18);
        if (crouchPose) {
            float phase = attackPhase();

            if ("2L".equals(attackType) && attackTimer > 0f) {
                // Fraco agachado: golpe curto e rápido na linha baixa.
                c.drawLine(playerX - 4, baseY - 70, playerX - 30, baseY - 39, paint);
                c.drawLine(
                    playerX + 4,
                    baseY - 70,
                    playerX + 34 + 52f * phase,
                    baseY - 48,
                    paint
                );
                c.drawLine(playerX - 5, baseY - 42, playerX - 42, baseY - 8, paint);
                c.drawLine(playerX + 5, baseY - 42, playerX + 39, baseY - 8, paint);
            } else if ("2M".equals(attackType) && attackTimer > 0f) {
                // Médio agachado: chute baixo com alcance maior.
                c.drawLine(playerX - 4, baseY - 70, playerX - 30, baseY - 39, paint);
                c.drawLine(playerX + 4, baseY - 70, playerX + 34, baseY - 43, paint);
                c.drawLine(playerX - 5, baseY - 42, playerX - 36, baseY - 8, paint);
                c.drawLine(
                    playerX + 5,
                    baseY - 42,
                    playerX + 44 + 72f * phase,
                    baseY - 12,
                    paint
                );
            } else if ("2H".equals(attackType) && attackTimer > 0f) {
                // Forte agachado: golpe mais amplo e pesado.
                c.drawLine(playerX - 4, baseY - 70, playerX - 32, baseY - 38, paint);
                c.drawLine(
                    playerX + 4,
                    baseY - 70,
                    playerX + 42 + 82f * phase,
                    baseY - 34 - 16f * phase,
                    paint
                );
                c.drawLine(playerX - 5, baseY - 42, playerX - 46, baseY - 8, paint);
                c.drawLine(playerX + 5, baseY - 42, playerX + 44, baseY - 8, paint);
            } else {
                c.drawLine(playerX - 4, baseY - 70, playerX - 35, baseY - 38, paint);
                c.drawLine(playerX + 4, baseY - 70, playerX + 38, baseY - 43, paint);
                c.drawLine(playerX - 5, baseY - 42, playerX - 42, baseY - 8, paint);
                c.drawLine(playerX + 5, baseY - 42, playerX + 42, baseY - 8, paint);
            }
        } else {
            float phase = attackPhase();

            if (isSuperPoseActive()) {
                float power = 1f + 0.10f * (float)Math.sin(superPhaseTimer * 24f);
                c.drawLine(playerX - 3, top + 58, playerX - 58f * power, top + 38, paint);
                c.drawLine(playerX + 3, top + 58, playerX + 74f * power, top + 36, paint);
                c.drawLine(playerX - 4, baseY - 45, playerX - 40, baseY, paint);
                c.drawLine(playerX + 4, baseY - 45, playerX + 42, baseY - 5, paint);
            } else if (isTagPoseActive()) {
                // Pose curta de prontidão ao terminar a entrada.
                float poseWave = (float)Math.sin(tagPhaseTimer * 16f) * 4f;
                c.drawLine(playerX - 3, top + 58, playerX - 42, top + 88 - poseWave, paint);
                c.drawLine(playerX + 3, top + 58, playerX + 48, top + 72 + poseWave, paint);
                c.drawLine(playerX - 4, baseY - 45, playerX - 34, baseY, paint);
                c.drawLine(playerX + 4, baseY - 45, playerX + 32, baseY - 4, paint);
            } else if ("L".equals(attackType) && attackTimer > 0f) {
                c.drawLine(playerX - 3, top + 58, playerX - 30, top + 96, paint);
                c.drawLine(playerX + 3, top + 58, playerX + 34 + 72f * phase, top + 66, paint);
                c.drawLine(playerX - 4, baseY - 45, playerX - 26, baseY, paint);
                c.drawLine(playerX + 4, baseY - 45, playerX + 26, baseY, paint);
            } else if ("M".equals(attackType) && attackTimer > 0f) {
                c.drawLine(playerX - 3, top + 58, playerX - 28, top + 97, paint);
                c.drawLine(playerX + 3, top + 58, playerX + 30, top + 94, paint);
                c.drawLine(playerX - 4, baseY - 45, playerX - 25, baseY, paint);
                c.drawLine(
                    playerX + 4,
                    baseY - 45,
                    playerX + 34 + 76f * phase,
                    baseY - 48f * phase,
                    paint
                );
            } else if ("S".equals(attackType) && attackTimer > 0f) {
                c.drawLine(playerX - 3, top + 58, playerX + 28, top + 76, paint);
                c.drawLine(playerX + 3, top + 58, playerX + 58 + 34f * phase, top + 76, paint);
                c.drawLine(playerX - 4, baseY - 45, playerX - 26, baseY, paint);
                c.drawLine(playerX + 4, baseY - 45, playerX + 26, baseY, paint);
            } else if ("H".equals(attackType) && attackTimer > 0f) {
                c.drawLine(playerX - 3, top + 58, playerX - 26, top + 98, paint);
                c.drawLine(
                    playerX + 3,
                    top + 58,
                    playerX + 28 + 92f * phase,
                    top + 84 + 22f * phase,
                    paint
                );
                c.drawLine(playerX - 4, baseY - 45, playerX - 30, baseY, paint);
                c.drawLine(playerX + 4, baseY - 45, playerX + 30, baseY, paint);
            } else {
                c.drawLine(playerX - 3, top + 58, playerX - 34 + armSwing, top + 100, paint);
                c.drawLine(playerX + 3, top + 58, playerX + 34 - armSwing, top + 100, paint);
                c.drawLine(playerX - 4, baseY - 45, playerX - 28 + legSwing, baseY, paint);
                c.drawLine(playerX + 4, baseY - 45, playerX + 28 - legSwing, baseY, paint);
            }
        }

        paint.setColor(Color.rgb(24, 35, 48));
        paint.setStrokeWidth(4);
        c.drawLine(playerX - 10, top + 15, playerX - 4, top + 15, paint);
        c.drawLine(playerX + 4, top + 15, playerX + 10, top + 15, paint);
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
            recordCommandDirection(next, nowMs);
        }

        if (next == 1 && previous != 1) {
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

        if (next != 1) {
            forwardDashing = false;
        }

        if (next == 5 && previous != 5) {
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
            boolean superJump = nowMs - lastDownInputMs <= 360L;
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
        float sx = getWidth() / VW;
        float sy = getHeight() / VH;
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        long nowMs = System.currentTimeMillis();

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            int pointerId = event.getPointerId(index);
            float x = event.getX(index) / sx;
            float y = event.getY(index) / sy;

            float dx = x - DPAD_X;
            float dy = y - DPAD_Y;

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
