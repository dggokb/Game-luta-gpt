package com.gamelutagpt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
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

        FighterProfile(
            String name,
            String[] baseCombo,
            int maxLife,
            int color,
            boolean hasEnergyAttack,
            float energyRange,
            float energySpeed,
            int energyDamage,
            int[] energyCommand
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
        }
    }

    private static final class FighterState {
        final FighterProfile profile;
        int life;

        FighterState(FighterProfile profile) {
            this.profile = profile;
            this.life = profile.maxLife;
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
            new int[]{3, 1}
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
            new int[]{5, 1}
        ))
    };

    private int activeFighterIndex = 0;
    private final List<EnergyProjectile> energyProjectiles = new ArrayList<>();

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

    private final Object stateLock = new Object();
    private final LinearGradient skyGradient = new LinearGradient(
        0, WORLD_TOP, 0, VH, Color.rgb(21, 55, 103), Color.rgb(240, 171, 99), Shader.TileMode.CLAMP);
    private final Path mountainPath = new Path();
    private static final String[] DIRECTION_LABELS = {"→", "↘", "↓", "↙", "←", "↖", "↑", "↗"};
    private static final float FIXED_STEP = 1f / 120f;
    private float accumulatedTime;
    private boolean surfaceReady;
    private boolean activityActive = true;
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
    private boolean attackCrouched;
    private String bufferedAttack;
    private boolean bufferedAutoCombo;
    private long bufferedAttackTime;
    private static final float ATTACK_BUFFER_SECONDS = 0.10f;
    private float crouchBlend;
    private float locomotionBlend;
    private float landingTimer;
    private final float[] pose = new float[22];

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
        synchronized (stateLock) {
            clearInput();
            accumulatedTime = 0f;
        }
    }

    @Override
    public void run() {
        long previous = System.nanoTime();
        final long targetFrame = 16_666_667L;
        while (running) {
            long frameStart = System.nanoTime();
            float elapsedSeconds = (frameStart - previous) / 1_000_000_000f;
            previous = frameStart;
              synchronized (stateLock) {
                advanceSimulation(elapsedSeconds);
            }
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
        accumulatedTime += clamp(elapsedSeconds, 0f, 0.10f);
        while (accumulatedTime + 0.000001f >= FIXED_STEP) {
            update(FIXED_STEP);
            accumulatedTime = Math.max(0f, accumulatedTime - FIXED_STEP);
        }
    }

    private void update(float dt) {
        // Split at recovery boundaries so neither movement nor queued attacks start early.
        if (attackTimer > 0f && attackTimer < dt) {
            float recovery = attackTimer;
            update(recovery);
            update(dt - recovery);
            return;
        }
        float previousX = playerX;
        crouching = grounded && isDownDirection(dpadDirection);
        landingTimer = Math.max(0f, landingTimer - dt);

        updateEnergyProjectiles(dt);

        float direction = 0f;
        if (!isEnergyAttackActive()) {
            if (movingLeft && !movingRight) direction = -1f;
            if (movingRight && !movingLeft) direction = 1f;
        }

        if (attackTimer > 0f && grounded) {
            // No chão, ataques travam o deslocamento horizontal.
            walkTime = 0f;
        } else if (backDashTimer > 0f && grounded) {
            playerX -= backDashSpeed * Math.min(dt, backDashTimer);
            backDashTimer = Math.max(0f, backDashTimer - dt);
            walkTime += dt * 13f;
        } else if (direction != 0f && (!grounded || !crouching)) {
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

            playerY += velocityY * dt + 0.5f * activeGravity * dt * dt;
            velocityY += activeGravity * dt;
            if (playerY >= GROUND_Y) {
                playerY = GROUND_Y;
                velocityY = 0f;
                grounded = true;
                superJumping = false;
                crouching = isDownDirection(dpadDirection);
                landingTimer = 0.14f;
            }
        }

        playerX = clamp(playerX, LEFT_BOUND, RIGHT_BOUND);
        float movement = Math.abs(playerX - previousX) / Math.max(dt, 0.00001f);
        locomotionBlend += ((grounded ? Math.min(1f, movement / moveSpeed) : 0f)
            - locomotionBlend) * (1f - (float)Math.exp(-25f * dt));
        float crouchTarget = (attackTimer > 0f ? attackCrouched : crouching) ? 1f : 0f;
        crouchBlend += (crouchTarget - crouchBlend) * (1f - (float)Math.exp(-30f * dt));
        if (attackTimer > 0f) {
            attackTimer = Math.max(0f, attackTimer - dt);
            if (attackTimer <= 0f) {
                attackType = "";
                if (bufferedAttack != null) {
                    String next = bufferedAttack;
                    boolean auto = bufferedAutoCombo;
                    long time = bufferedAttackTime;
                    bufferedAttack = null;
                    requestAttack(next, auto, time);
                }
            }
        }

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

    private boolean isEnergyAttackActive() {
        return "S".equals(attackType) && attackTimer > 0f;
    }

    private void startJump(boolean superJump) {
        if (!grounded || attackTimer > 0f) return;

        grounded = false;
        crouching = false;
        forwardDashing = false;
        backDashTimer = 0f;
        superJumping = superJump;
        velocityY = superJump ? -superJumpSpeed : -jumpSpeed;
        playerY -= 2f;
    }

    private void startAttack(String type) {
        attackType = type;
        attackCrouched = grounded && (type.startsWith("2") || ("S".equals(type) && crouching));
        forwardDashing = false;
        backDashTimer = 0f;

        if ("L".equals(type) || "2L".equals(type)) attackDuration = 0.16f;
        else if ("M".equals(type) || "2M".equals(type)) attackDuration = 0.26f;
        else if ("S".equals(type)) attackDuration = 0.30f;
        else attackDuration = 0.40f;

        attackTimer = attackDuration;
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
        if (attackTimer > 0f || hasActiveEnergyProjectile(activeFighterIndex)) return;

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
        // A new unrelated direction must not re-arm an old motion.
        if (commandDirections[commandCount - 1] != profile.energyCommand[profile.energyCommand.length - 1]) return;

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

    private void requestAttack(String button, boolean auto, long nowMs) {
        if (isEnergyAttackActive()) return;
        if (attackTimer > 0f) {
            // One short recovery buffer: mashing cannot restart or skip animations.
            if (attackTimer <= ATTACK_BUFFER_SECONDS && bufferedAttack == null) {
                bufferedAttack = button;
                bufferedAutoCombo = auto;
                bufferedAttackTime = nowMs;
            }
            return;
        }
        if (!auto && tryFirePendingEnergy(button, nowMs)) return;
        String type = button;
        if (auto) {
            String[] combo = activeFighter().profile.baseCombo;
            if (combo.length == 0) return;
            if (nowMs - lastAutoComboTapMs > AUTO_COMBO_RESET_MS) autoComboIndex = 0;
            type = combo[autoComboIndex];
            autoComboIndex = (autoComboIndex + 1) % combo.length;
            lastAutoComboTapMs = nowMs;
        } else {
            resetAutoCombo();
        }
        startAttack(grounded && crouching ? "2" + type : type);
    }

    private void switchFighter() {
        activeFighterIndex = (activeFighterIndex + 1) % team.length;

        bufferedAttack = null;
        attackType = "";
        attackTimer = 0f;
        attackDuration = 0f;
        forwardDashing = false;
        backDashTimer = 0f;
        resetAutoCombo();
        resetCommandBuffer();
        pendingEnergyUntilMs = -1L;

        // Posição, altura, velocidade vertical e câmera são compartilhadas.
        // Assim a troca mantém exatamente o mesmo estado de movimento.
    }

    private void applyDamage(int damage) {
        if (damage <= 0) return;
        FighterState fighter = activeFighter();
        fighter.life = Math.max(0, fighter.life - damage);
    }

    private float attackPhase() {
        if (attackTimer <= 0f || attackDuration <= 0f) return 0f;
        float t = 1f - attackTimer / attackDuration;
        // Anticipation, fast extension, brief contact pose, controlled recovery.
        if (t < 0.18f) return -0.14f * (float)Math.sin(Math.PI * t / 0.18f);
        if (t < 0.38f) return smoothStep((t - 0.18f) / 0.20f);
        if (t < 0.48f) return 1f;
        return 1f - smoothStep((t - 0.48f) / 0.52f);
    }

    private void drawFrame() {
        if (!holder.getSurface().isValid()) return;
        Canvas canvas = holder.lockCanvas();
        if (canvas == null) return;

        try {
            synchronized (stateLock) {
            resetPaintForFrame();

            float sx = canvas.getWidth() / VW;
            float sy = canvas.getHeight() / VH;

            canvas.save();
            canvas.scale(sx, sy);

            float visibleWorldWidth = VW / CAMERA_ZOOM;
            float cameraLeft = clamp(cameraX - visibleWorldWidth / 2f, 0f, WORLD_WIDTH - visibleWorldWidth);

            canvas.save();
            canvas.scale(CAMERA_ZOOM, CAMERA_ZOOM);
            canvas.translate(-cameraLeft, -cameraTop);
            drawScenario(canvas);
            drawEnergyProjectiles(canvas);
            drawPlayer(canvas);
            canvas.restore();

            drawHud(canvas);
            drawControls(canvas);

            canvas.restore();
            }
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
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
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
        for (int i = 0; i < 15; i++) {
            float x = -100f + i * 190f;
            float h = 105f + (i % 5) * 24f;
            Path p = mountainPath;
            p.rewind();
            p.moveTo(x, GROUND_Y);
            p.lineTo(x + 115, GROUND_Y - h);
            p.lineTo(x + 245, GROUND_Y);
            p.close();
            c.drawPath(p, paint);
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
        c.drawRoundRect(32, 28, 560, 154, 18, 18, paint);

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

        // Reserva: indicador menor com vida própria.
        paint.setColor(reserve.profile.color);
        c.drawCircle(78, 127, 14, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(14);
        paint.setFakeBoldText(true);
        c.drawText("RESERVA: " + reserve.profile.name, 105, 124, paint);
        paint.setFakeBoldText(false);

        drawLifeBar(c, reserve, 105f, 132f, 525f, 145f, false);

        paint.setColor(Color.argb(180, 10, 15, 27));
        c.drawRoundRect(945, 28, 1248, 112, 18, 18, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(20);
        paint.setFakeBoldText(true);
        c.drawText("ASTRA • v0.18.1", 975, 59, paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(16);

        String state = attackTimer > 0f
            ? "ATAQUE " + attackType
            : (backDashTimer > 0f
                ? "BACKDASH"
                : (forwardDashing
                    ? "DASH"
                    : (superJumping
                        ? "SUPER JUMP"
                        : (crouching
                            ? "AGACHADO"
                            : (!grounded
                                ? "NO AR"
                                : (movingLeft || movingRight ? "ANDANDO" : "PARADO"))))));

        c.drawText("Estado: " + state, 975, 88, paint);
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
                "HP " + fighter.life + " / " + fighter.profile.maxLife,
                left + 6,
                bottom - 7,
                paint
            );
        }
    }

    private void drawEnergyProjectiles(Canvas c) {
        for (EnergyProjectile projectile : energyProjectiles) {
            paint.setColor(Color.argb(90, Color.red(projectile.color), Color.green(projectile.color), Color.blue(projectile.color)));
            float tail = clamp(projectile.speed * 0.065f, 25f, 80f);
            c.drawOval(projectile.x - tail, projectile.y - 12f, projectile.x + 8f, projectile.y + 12f, paint);
            paint.setColor(Color.argb(75, 255, 255, 255));
            c.drawCircle(projectile.x, projectile.y, 29f, paint);

            paint.setColor(projectile.color);
            c.drawCircle(projectile.x, projectile.y, 20f, paint);

            paint.setColor(Color.WHITE);
            c.drawCircle(projectile.x + 5f, projectile.y - 5f, 8f, paint);
        }
    }

    private void drawPlayer(Canvas c) {
        float height = Math.max(0f, GROUND_Y - playerY);
        float shadowScale = 1f / (1f + height / 450f);
        paint.setColor(Color.argb((int)(70 * shadowScale), 0, 0, 0));
        c.drawOval(playerX - 43 * shadowScale, GROUND_Y - 9 * shadowScale,
            playerX + 43 * shadowScale, GROUND_Y + 9 * shadowScale, paint);

        buildPose();
        c.save();
        c.translate(playerX, playerY);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(activeFighter().profile.color);
        // Rear limbs are dimmed to keep overlapping poses readable.
        paint.setAlpha(170);
        drawLimb(c, 1, 3, 4, 15f);
        drawLimb(c, 2, 7, 8, 18f);
        paint.setAlpha(255);
        paint.setStrokeWidth(24f);
        c.drawLine(pose[2], pose[3], pose[4], pose[5], paint);
        drawLimb(c, 2, 9, 10, 19f);
        drawLimb(c, 1, 5, 6, 17f);
        c.drawCircle(pose[12], pose[13], 11f, paint);
        c.drawCircle(pose[0], pose[1], 24f, paint);
        paint.setColor(Color.rgb(24, 35, 48));
        paint.setStrokeWidth(4f);
        c.drawLine(pose[0] + 3, pose[1] - 3, pose[0] + 12, pose[1] - 3, paint);
        paint.setStrokeWidth(8f);
        c.drawLine(pose[16] - 8, pose[17], pose[16] + 9, pose[17], paint);
        c.drawLine(pose[20] - 8, pose[21], pose[20] + 11, pose[21], paint);
        c.restore();
    }

    // Head, shoulder, hip, rear elbow/fist, front elbow/fist, rear knee/foot,
    // front knee/foot. Reused storage avoids allocating skeletons every frame.
    private void buildPose() {
        float crouch = crouchBlend;
        float stride = (float)Math.sin(walkTime) * locomotionBlend * (1f - crouch);
        float travel = movingLeft && !movingRight ? -1f : 1f;
        float lean = forwardDashing && grounded ? 13f : (backDashTimer > 0f ? -10f : 0f);
        float landing = landingTimer > 0f ? (float)Math.sin(Math.PI * landingTimer / 0.14f) * 9f : 0f;
        float bob = Math.abs(stride) * 3f + landing;
        float hipY = -49f + crouch * 16f + bob;
        float shoulderY = -96f + crouch * 36f + bob;
        point(0, lean, -126f + crouch * 45f + bob);
        point(1, lean, shoulderY);
        point(2, 0f, hipY);
        point(3, -27f - stride * 10f, shoulderY + 22f);
        point(4, -14f - stride * 15f, shoulderY - 4f);
        point(5, 26f + stride * 9f, shoulderY + 20f);
        point(6, 38f + stride * 14f, shoulderY - 7f);
        point(7, -19f - crouch * 13f + stride * 17f * travel, hipY + 21f);
        point(8, -25f - crouch * 15f + stride * 27f * travel, -3f - Math.max(0f, stride) * 9f);
        point(9, 20f + crouch * 16f - stride * 17f * travel, hipY + 21f);
        point(10, 27f + crouch * 16f - stride * 27f * travel, -3f - Math.max(0f, -stride) * 9f);
        if (!grounded) {
            float tuck = velocityY < 0f ? 18f : 7f;
            point(7, -22f, hipY + 14f);
            point(8, -33f, -12f - tuck * 0.4f);
            point(9, 27f, hipY + 10f - tuck);
            point(10, 36f, -12f - tuck);
        }
        float extension = attackPhase();
        if (attackTimer <= 0f) return;
        char kind = attackType.charAt(attackType.length() - 1);
        boolean low = attackCrouched;
        if (kind == 'L') {
            blendPoint(5, 49f, shoulderY + 4f, extension);
            blendPoint(6, low ? 91f : 104f, shoulderY + (low ? 16f : 2f), extension);
            blendPoint(0, 7f, pose[1], extension);
            blendPoint(1, 8f, shoulderY, extension);
        } else if (kind == 'M') {
            blendPoint(9, 61f, low ? -19f : -61f, extension);
            blendPoint(10, low ? 122f : 115f, low ? -8f : -65f, extension);
            blendPoint(0, -13f, pose[1] - 4f, extension);
            blendPoint(1, -10f, shoulderY, extension);
        } else if (kind == 'H') {
            blendPoint(0, 15f, pose[1] - (low ? 20f : 2f), extension);
            blendPoint(1, 13f, shoulderY - (low ? 14f : 0f), extension);
            blendPoint(5, 54f, low ? -103f : shoulderY + 17f, extension);
            blendPoint(6, low ? 65f : 124f, low ? -153f : shoulderY + 25f, extension);
            blendPoint(8, -39f, pose[17], extension);
        } else if (kind == 'S') {
            blendPoint(3, 26f, shoulderY + 23f, extension);
            blendPoint(4, 61f, shoulderY + 12f, extension);
            blendPoint(5, 42f, shoulderY + 12f, extension);
            blendPoint(6, 81f, shoulderY + 8f, extension);
        }
    }

    private void point(int joint, float x, float y) {
        pose[joint * 2] = x;
        pose[joint * 2 + 1] = y;
    }

    private void blendPoint(int joint, float x, float y, float amount) {
        int i = joint * 2;
        pose[i] += (x - pose[i]) * amount;
        pose[i + 1] += (y - pose[i + 1]) * amount;
    }

    private void drawLimb(Canvas c, int root, int middle, int end, float width) {
        paint.setStrokeWidth(width);
        c.drawLine(pose[root * 2], pose[root * 2 + 1], pose[middle * 2], pose[middle * 2 + 1], paint);
        c.drawLine(pose[middle * 2], pose[middle * 2 + 1], pose[end * 2], pose[end * 2 + 1], paint);
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
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45.0);
            float x = DPAD_X + (float)Math.cos(a) * DPAD_RADIUS;
            float y = DPAD_Y + (float)Math.sin(a) * DPAD_RADIUS;
            c.drawLine(DPAD_X, DPAD_Y, x, y, paint);
        }
        paint.setStyle(Paint.Style.FILL);

        if (dpadDirection != 0) {
            double angle = directionAngle(dpadDirection);
            float hx = DPAD_X + (float)Math.cos(angle) * 72f;
            float hy = DPAD_Y + (float)Math.sin(angle) * 72f;
            paint.setColor(Color.argb(165, 255, 255, 255));
            c.drawCircle(hx, hy, 31f, paint);
        }

        paint.setColor(Color.argb(175, 10, 18, 32));
        c.drawCircle(DPAD_X, DPAD_Y, DPAD_DEADZONE, paint);

        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(27f);


        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45.0);
            float tx = DPAD_X + (float)Math.cos(a) * 78f;
            float ty = DPAD_Y + (float)Math.sin(a) * 78f
                - (paint.ascent() + paint.descent()) / 2f;
            c.drawText(DIRECTION_LABELS[i], tx, ty, paint);
        }

        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);

        drawAttackButton(c, LIGHT_X, LIGHT_Y, "L", lightPointer != -1);
        drawAttackButton(c, MEDIUM_X, MEDIUM_Y, "M", mediumPointer != -1);
        drawAttackButton(c, HEAVY_X, HEAVY_Y, "H", heavyPointer != -1);
        drawComboButton(c);
        drawTagButton(c);
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

        paint.setColor(
            pressed
                ? reserveFighter().profile.color
                : Color.argb(145, 7, 13, 26)
        );
        c.drawCircle(TAG_X, TAG_Y, TAG_RADIUS, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(reserveFighter().profile.color);
        c.drawCircle(TAG_X, TAG_Y, TAG_RADIUS, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(17f);
        paint.setFakeBoldText(true);
        float textY = TAG_Y - (paint.ascent() + paint.descent()) / 2f;
        c.drawText("TROCA", TAG_X, textY, paint);
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

    private double directionAngle(int direction) {
        switch (direction) {
            case 1: return Math.toRadians(0);
            case 2: return Math.toRadians(45);
            case 3: return Math.toRadians(90);
            case 4: return Math.toRadians(135);
            case 5: return Math.toRadians(180);
            case 6: return Math.toRadians(225);
            case 7: return Math.toRadians(270);
            case 8: return Math.toRadians(315);
            default: return 0;
        }
    }

    private boolean isDownDirection(int direction) {
        return direction == 2 || direction == 3 || direction == 4;
    }

    private boolean isUpDirection(int direction) {
        return direction == 6 || direction == 7 || direction == 8;
    }

    private void updateDpad(float x, float y, long nowMs) {
        float dx = x - DPAD_X;
        float dy = y - DPAD_Y;
        float distance = (float)Math.sqrt(dx * dx + dy * dy);

        int previous = dpadDirection;
        int next = 0;

        if (distance >= DPAD_DEADZONE) {
            double degrees = Math.toDegrees(Math.atan2(dy, dx));
            if (degrees < 0) degrees += 360.0;
            int sector = ((int)Math.floor((degrees + 22.5) / 45.0)) % 8;
            next = sector + 1;
        }

        dpadDirection = next;

        movingLeft = next == 4 || next == 5 || next == 6;
        movingRight = next == 1 || next == 2 || next == 8;
        crouching = grounded && isDownDirection(next);

        // Track the thumb even while energy locks movement. Suppress actions, not input.
        if (isEnergyAttackActive()) return;
        if (next != 0 && next != previous) {
            recordCommandDirection(next, nowMs);
        }

        if (next == 1 && previous != 1) {
            if (
                attackTimer <= 0f &&
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
                attackTimer <= 0f &&
                grounded &&
                nowMs - lastBackTapMs <= DASH_DOUBLE_TAP_MS
            ) {
                backDashTimer = backDashDuration;
                forwardDashing = false;
            }
            lastBackTapMs = nowMs;
        }

        if (isDownDirection(previous) && !isDownDirection(next)) {
            lastDownInputMs = nowMs;
        }

        if (isUpDirection(next) && !isUpDirection(previous) && grounded && attackTimer <= 0f) {
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
        synchronized (stateLock) {
            return handleTouch(event);
        }
    }

    private boolean handleTouch(MotionEvent event) {
        if (getWidth() <= 0 || getHeight() <= 0) return true;
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
                dpadPointer == -1 &&
                dx * dx + dy * dy <= DPAD_RADIUS * DPAD_RADIUS
            ) {
                dpadPointer = pointerId;
                updateDpad(x, y, nowMs);
            } else if (insideCircle(x, y, LIGHT_X, LIGHT_Y, ATTACK_RADIUS)) {
                lightPointer = pointerId;
                requestAttack("L", false, nowMs);
            } else if (insideCircle(x, y, MEDIUM_X, MEDIUM_Y, ATTACK_RADIUS)) {
                mediumPointer = pointerId;
                requestAttack("M", false, nowMs);
            } else if (insideCircle(x, y, HEAVY_X, HEAVY_Y, ATTACK_RADIUS)) {
                heavyPointer = pointerId;
                requestAttack("H", false, nowMs);
            } else if (insideCircle(x, y, COMBO_X, COMBO_Y, COMBO_RADIUS)) {
                comboPointer = pointerId;
                requestAttack("", true, nowMs);
            } else if (insideCircle(x, y, TAG_X, TAG_Y, TAG_RADIUS)) {
                tagPointer = pointerId;
                switchFighter();
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (dpadPointer != -1) {
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
                if (isDownDirection(dpadDirection)) lastDownInputMs = nowMs;
                clearDpad();
            }
            if (pointerId == lightPointer) lightPointer = -1;
            if (pointerId == mediumPointer) mediumPointer = -1;
            if (pointerId == heavyPointer) heavyPointer = -1;
            if (pointerId == comboPointer) comboPointer = -1;
            if (pointerId == tagPointer) tagPointer = -1;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            clearInput();
        }

        return true;
    }

    private void clearInput() {
        clearDpad();
        lightPointer = mediumPointer = heavyPointer = comboPointer = tagPointer = -1;
        lastDownInputMs = lastForwardTapMs = lastBackTapMs = -1000L;
        backDashTimer = 0f;
        bufferedAttack = null;
        pendingEnergyUntilMs = -1L;
        resetCommandBuffer();
        resetAutoCombo();
    }

    private static float smoothStep(float t) {
        t = clamp(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
