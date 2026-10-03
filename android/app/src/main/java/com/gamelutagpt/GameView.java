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

        FighterProfile(
            String name,
            String[] baseCombo,
            int maxLife,
            int color,
            boolean hasEnergyAttack,
            float energyRange,
            float energySpeed,
            int energyDamage
        ) {
            this.name = name;
            this.baseCombo = baseCombo;
            this.maxLife = maxLife;
            this.color = color;
            this.hasEnergyAttack = hasEnergyAttack;
            this.energyRange = energyRange;
            this.energySpeed = energySpeed;
            this.energyDamage = energyDamage;
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
            850
        )),
        new FighterState(new FighterProfile(
            "PLAYER 2",
            new String[]{"L", "L", "H", "M"},
            10000,
            Color.rgb(74, 205, 232),
            false,
            0f,
            0f,
            0
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

    private static final float ENERGY_X = 1205f;
    private static final float ENERGY_Y = 475f;
    private static final float ENERGY_RADIUS = 43f;

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
    private int energyPointer = -1;

    private int autoComboIndex = 0;
    private long lastAutoComboTapMs = -1000L;
    private static final long AUTO_COMBO_RESET_MS = 700L;

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
        running = true;
        gameThread = new Thread(this, "GameLoop");
        gameThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        running = false;
        if (gameThread != null) {
            try {
                gameThread.join(800);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
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
        if (attackTimer > 0f) {
            attackTimer = Math.max(0f, attackTimer - dt);
            if (attackTimer <= 0f) attackType = "";
        }

        updateEnergyProjectiles(dt);

        float direction = 0f;
        if (movingLeft && !movingRight) direction = -1f;
        if (movingRight && !movingLeft) direction = 1f;

        if (attackTimer > 0f && grounded) {
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
            velocityY += gravity * dt;
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

    private void startJump(boolean superJump) {
        if (!grounded) return;

        grounded = false;
        crouching = false;
        superJumping = superJump;
        velocityY = superJump ? -superJumpSpeed : -jumpSpeed;
        playerY -= 2f;
    }

    private void startAttack(String type) {
        attackType = type;
        forwardDashing = false;
        backDashTimer = 0f;

        if ("L".equals(type)) attackDuration = 0.16f;
        else if ("M".equals(type)) attackDuration = 0.26f;
        else if ("S".equals(type)) attackDuration = 0.30f;
        else attackDuration = 0.40f;

        attackTimer = attackDuration;
    }

    private void fireEnergyAttack() {
        FighterProfile profile = activeFighter().profile;
        if (!profile.hasEnergyAttack) return;

        resetAutoCombo();
        startAttack("S");

        float spawnY = playerY - (crouching ? 65f : 82f);
        energyProjectiles.add(new EnergyProjectile(
            playerX + 62f,
            spawnY,
            profile.energyRange,
            profile.energySpeed,
            profile.energyDamage,
            profile.color,
            activeFighterIndex
        ));
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
        activeFighterIndex = (activeFighterIndex + 1) % team.length;

        attackType = "";
        attackTimer = 0f;
        attackDuration = 0f;
        forwardDashing = false;
        backDashTimer = 0f;
        resetAutoCombo();

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
        return t < 0.5f ? t * 2f : (1f - t) * 2f;
    }

    private void drawFrame() {
        if (!holder.getSurface().isValid()) return;
        Canvas canvas = holder.lockCanvas();
        if (canvas == null) return;

        try {
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
        } finally {
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawScenario(Canvas c) {
        paint.setShader(new LinearGradient(
            0, WORLD_TOP, 0, VH,
            Color.rgb(21, 55, 103),
            Color.rgb(240, 171, 99),
            Shader.TileMode.CLAMP
        ));
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
            android.graphics.Path p = new android.graphics.Path();
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
        c.drawText("ENERGY • v0.10", 975, 59, paint);
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
            paint.setColor(Color.argb(75, 255, 255, 255));
            c.drawCircle(projectile.x, projectile.y, 29f, paint);

            paint.setColor(projectile.color);
            c.drawCircle(projectile.x, projectile.y, 20f, paint);

            paint.setColor(Color.WHITE);
            c.drawCircle(projectile.x + 5f, projectile.y - 5f, 8f, paint);
        }
    }

    private void drawPlayer(Canvas c) {
        float bob = (grounded && !crouching && (movingLeft || movingRight))
            ? (float)Math.sin(walkTime) * 3f
            : 0f;

        float baseY = playerY + bob;
        float bodyHeight = crouching ? 90f : 145f;
        float top = baseY - bodyHeight;

        paint.setColor(Color.argb(70, 0, 0, 0));
        c.drawOval(playerX - 43, GROUND_Y - 10, playerX + 43, GROUND_Y + 10, paint);

        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(activeFighter().profile.color);
        c.drawCircle(playerX, top + 20, 25, paint);

        paint.setStrokeWidth(crouching ? 22 : 25);
        c.drawLine(playerX, top + 48, playerX, baseY - 45, paint);

        float legSwing = (grounded && !crouching && (movingLeft || movingRight))
            ? (float)Math.sin(walkTime) * 18f
            : 0f;

        float armSwing = -legSwing * 0.75f;

        paint.setStrokeWidth(18);
        if (crouching) {
            c.drawLine(playerX - 4, baseY - 70, playerX - 35, baseY - 38, paint);
            c.drawLine(playerX + 4, baseY - 70, playerX + 38, baseY - 43, paint);
            c.drawLine(playerX - 5, baseY - 42, playerX - 42, baseY - 8, paint);
            c.drawLine(playerX + 5, baseY - 42, playerX + 42, baseY - 8, paint);
        } else {
            float phase = attackPhase();

            if ("L".equals(attackType) && attackTimer > 0f) {
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

        String[] labels = {"→", "↘", "↓", "↙", "←", "↖", "↑", "↗"};
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45.0);
            float tx = DPAD_X + (float)Math.cos(a) * 78f;
            float ty = DPAD_Y + (float)Math.sin(a) * 78f
                - (paint.ascent() + paint.descent()) / 2f;
            c.drawText(labels[i], tx, ty, paint);
        }

        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);

        drawAttackButton(c, LIGHT_X, LIGHT_Y, "L", lightPointer != -1);
        drawAttackButton(c, MEDIUM_X, MEDIUM_Y, "M", mediumPointer != -1);
        drawAttackButton(c, HEAVY_X, HEAVY_Y, "H", heavyPointer != -1);
        drawComboButton(c);
        drawTagButton(c);
        drawEnergyButton(c);
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

    private void drawEnergyButton(Canvas c) {
        FighterProfile profile = activeFighter().profile;
        boolean enabled = profile.hasEnergyAttack;
        boolean pressed = energyPointer != -1;

        if (!enabled) {
            paint.setColor(Color.argb(80, 55, 60, 68));
        } else if (pressed) {
            paint.setColor(profile.color);
        } else {
            paint.setColor(Color.argb(145, 7, 13, 26));
        }
        c.drawCircle(ENERGY_X, ENERGY_Y, ENERGY_RADIUS, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(enabled ? profile.color : Color.argb(100, 180, 180, 180));
        c.drawCircle(ENERGY_X, ENERGY_Y, ENERGY_RADIUS, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(enabled ? Color.WHITE : Color.argb(130, 200, 200, 200));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(16f);
        paint.setFakeBoldText(true);
        float textY = ENERGY_Y - (paint.ascent() + paint.descent()) / 2f;
        c.drawText(enabled ? "S / KI" : "SEM KI", ENERGY_X, textY, paint);
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

        if (next == 1 && previous != 1) {
            if (grounded && nowMs - lastForwardTapMs <= DASH_DOUBLE_TAP_MS) {
                forwardDashing = true;
                backDashTimer = 0f;
            }
            lastForwardTapMs = nowMs;
        }

        if (next != 1) {
            forwardDashing = false;
        }

        if (next == 5 && previous != 5) {
            if (grounded && nowMs - lastBackTapMs <= DASH_DOUBLE_TAP_MS) {
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

            if (
                dpadPointer == -1 &&
                dx * dx + dy * dy <= DPAD_RADIUS * DPAD_RADIUS
            ) {
                dpadPointer = pointerId;
                updateDpad(x, y, nowMs);
            } else if (insideCircle(x, y, LIGHT_X, LIGHT_Y, ATTACK_RADIUS)) {
                lightPointer = pointerId;
                resetAutoCombo();
                startAttack("L");
            } else if (insideCircle(x, y, MEDIUM_X, MEDIUM_Y, ATTACK_RADIUS)) {
                mediumPointer = pointerId;
                resetAutoCombo();
                startAttack("M");
            } else if (insideCircle(x, y, HEAVY_X, HEAVY_Y, ATTACK_RADIUS)) {
                heavyPointer = pointerId;
                resetAutoCombo();
                startAttack("H");
            } else if (insideCircle(x, y, COMBO_X, COMBO_Y, COMBO_RADIUS)) {
                comboPointer = pointerId;
                triggerAutoCombo(nowMs);
            } else if (insideCircle(x, y, TAG_X, TAG_Y, TAG_RADIUS)) {
                tagPointer = pointerId;
                switchFighter();
            } else if (
                insideCircle(x, y, ENERGY_X, ENERGY_Y, ENERGY_RADIUS) &&
                activeFighter().profile.hasEnergyAttack
            ) {
                energyPointer = pointerId;
                fireEnergyAttack();
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

            if (pointerId == dpadPointer) clearDpad();
            if (pointerId == lightPointer) lightPointer = -1;
            if (pointerId == mediumPointer) mediumPointer = -1;
            if (pointerId == heavyPointer) heavyPointer = -1;
            if (pointerId == comboPointer) comboPointer = -1;
            if (pointerId == tagPointer) tagPointer = -1;
            if (pointerId == energyPointer) energyPointer = -1;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            clearDpad();
            lightPointer = -1;
            mediumPointer = -1;
            heavyPointer = -1;
            comboPointer = -1;
            tagPointer = -1;
            energyPointer = -1;
        }

        return true;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
