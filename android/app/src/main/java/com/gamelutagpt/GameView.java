package com.gamelutagpt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final float VW = 1280f;
    private static final float VH = 720f;
    private static final float GROUND_Y = 565f;
    private static final float WORLD_WIDTH = 2600f;
    private static final float LEFT_BOUND = 90f;
    private static final float RIGHT_BOUND = WORLD_WIDTH - 90f;
    private static final float CAMERA_ZOOM = 1.12f;
    private static final float CAMERA_TOP = 72f;

    private final SurfaceHolder holder;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Thread gameThread;
    private volatile boolean running;

    private float playerX = 420f;
    private float playerY = GROUND_Y;
    private float cameraX = 420f;
    private float velocityY = 0f;
    private boolean movingLeft;
    private boolean movingRight;
    private boolean crouching;
    private boolean grounded = true;
    private float walkTime = 0f;

    private final float moveSpeed = 300f;
    private final float jumpSpeed = 660f;
    private final float gravity = 1650f;

    private final RectF leftButton = new RectF(45, 575, 145, 675);
    private final RectF rightButton = new RectF(165, 575, 265, 675);
    private final RectF downButton = new RectF(105, 465, 205, 555);
    private final RectF jumpButton = new RectF(1090, 555, 1230, 685);

    private int leftPointer = -1;
    private int rightPointer = -1;
    private int downPointer = -1;

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        setFocusable(true);
        setKeepScreenOn(true);
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
                    int ns = (int) (remaining % 1_000_000L);
                    Thread.sleep(ms, ns);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void update(float dt) {
        float direction = 0f;
        if (movingLeft && !movingRight) direction = -1f;
        if (movingRight && !movingLeft) direction = 1f;

        if (direction != 0f && !crouching) {
            playerX += direction * moveSpeed * dt;
            walkTime += dt * 8f;
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
            }
        }

        playerX = clamp(playerX, LEFT_BOUND, RIGHT_BOUND);

        float visibleWorldWidth = VW / CAMERA_ZOOM;
        float halfVisible = visibleWorldWidth / 2f;
        float targetCameraX = clamp(playerX, halfVisible, WORLD_WIDTH - halfVisible);
        float follow = 1f - (float)Math.pow(0.001f, dt);
        cameraX += (targetCameraX - cameraX) * follow;
    }

    private void jump() {
        if (grounded && !crouching) {
            grounded = false;
            velocityY = -jumpSpeed;
            playerY -= 2f;
        }
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
            canvas.translate(-cameraLeft, -CAMERA_TOP);
            drawScenario(canvas);
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
        paint.setShader(new LinearGradient(0, 0, 0, VH, Color.rgb(43, 97, 148), Color.rgb(240, 171, 99), Shader.TileMode.CLAMP));
        c.drawRect(0, 0, WORLD_WIDTH, VH, paint);
        paint.setShader(null);

        paint.setColor(Color.argb(130, 255, 244, 201));
        c.drawCircle(2050, 120, 58, paint);

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
            c.drawRoundRect(x, GROUND_Y + 35 + (i % 3) * 38, x + 55, GROUND_Y + 40 + (i % 3) * 38, 3, 3, paint);
        }

        paint.setColor(Color.argb(90, 255, 255, 255));
        c.drawRect(LEFT_BOUND, 190, LEFT_BOUND + 3, GROUND_Y, paint);
        c.drawRect(RIGHT_BOUND - 3, 190, RIGHT_BOUND, GROUND_Y, paint);
    }

    private void drawHud(Canvas c) {
        paint.setColor(Color.argb(185, 10, 15, 27));
        c.drawRoundRect(32, 28, 540, 122, 18, 18, paint);

        paint.setColor(Color.rgb(244, 183, 59));
        c.drawCircle(78, 74, 29, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setColor(Color.WHITE);
        c.drawCircle(78, 74, 29, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(Color.WHITE);
        paint.setTextSize(22);
        paint.setFakeBoldText(true);
        c.drawText("PLAYER", 122, 58, paint);
        paint.setFakeBoldText(false);

        paint.setColor(Color.rgb(45, 53, 62));
        c.drawRoundRect(122, 70, 505, 98, 8, 8, paint);
        paint.setColor(Color.rgb(111, 223, 105));
        c.drawRoundRect(122, 70, 505, 98, 8, 8, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(15);
        c.drawText("HP 10000 / 10000", 128, 91, paint);

        paint.setColor(Color.argb(180, 10, 15, 27));
        c.drawRoundRect(945, 28, 1248, 112, 18, 18, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(20);
        paint.setFakeBoldText(true);
        c.drawText("MOVEMENT TEST", 975, 59, paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(16);
        String state = crouching ? "AGACHADO" : (!grounded ? "NO AR" : (movingLeft || movingRight ? "ANDANDO" : "PARADO"));
        c.drawText("Estado: " + state, 975, 88, paint);
    }

    private void drawPlayer(Canvas c) {
        float bob = (grounded && !crouching && (movingLeft || movingRight)) ? (float)Math.sin(walkTime) * 3f : 0f;
        float baseY = playerY + bob;
        float bodyHeight = crouching ? 90f : 145f;
        float top = baseY - bodyHeight;

        paint.setColor(Color.argb(70, 0, 0, 0));
        c.drawOval(playerX - 43, GROUND_Y - 10, playerX + 43, GROUND_Y + 10, paint);

        paint.setStrokeCap(Paint.Cap.ROUND);

        paint.setColor(Color.rgb(244, 183, 59));
        c.drawCircle(playerX, top + 20, 25, paint);

        paint.setStrokeWidth(crouching ? 22 : 25);
        c.drawLine(playerX, top + 48, playerX, baseY - 45, paint);

        float legSwing = (grounded && !crouching && (movingLeft || movingRight)) ? (float)Math.sin(walkTime) * 18f : 0f;
        float armSwing = -legSwing * 0.75f;

        paint.setStrokeWidth(18);
        if (crouching) {
            c.drawLine(playerX - 4, baseY - 70, playerX - 35, baseY - 38, paint);
            c.drawLine(playerX + 4, baseY - 70, playerX + 38, baseY - 43, paint);
            c.drawLine(playerX - 5, baseY - 42, playerX - 42, baseY - 8, paint);
            c.drawLine(playerX + 5, baseY - 42, playerX + 42, baseY - 8, paint);
        } else {
            c.drawLine(playerX - 3, top + 58, playerX - 34 + armSwing, top + 100, paint);
            c.drawLine(playerX + 3, top + 58, playerX + 34 - armSwing, top + 100, paint);
            c.drawLine(playerX - 4, baseY - 45, playerX - 28 + legSwing, baseY, paint);
            c.drawLine(playerX + 4, baseY - 45, playerX + 28 - legSwing, baseY, paint);
        }

        paint.setColor(Color.rgb(24, 35, 48));
        paint.setStrokeWidth(4);
        c.drawLine(playerX - 10, top + 15, playerX - 4, top + 15, paint);
        c.drawLine(playerX + 4, top + 15, playerX + 10, top + 15, paint);
    }

    private void drawControls(Canvas c) {
        drawControl(c, leftButton, "◀", movingLeft);
        drawControl(c, rightButton, "▶", movingRight);
        drawControl(c, downButton, "▼", crouching);
        drawControl(c, jumpButton, "PULAR", false);
    }

    private void drawControl(Canvas c, RectF rect, String label, boolean active) {
        paint.setColor(active ? Color.argb(175, 255, 255, 255) : Color.argb(110, 7, 13, 26));
        c.drawRoundRect(rect, 18, 18, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(Color.argb(205, 255, 255, 255));
        c.drawRoundRect(rect, 18, 18, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(label.length() > 2 ? 22 : 38);
        paint.setFakeBoldText(true);
        float y = rect.centerY() - (paint.ascent() + paint.descent()) / 2f;
        c.drawText(label, rect.centerX(), y, paint);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float sx = getWidth() / VW;
        float sy = getHeight() / VH;
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        int pointerId = event.getPointerId(index);
        float x = event.getX(index) / sx;
        float y = event.getY(index) / sy;

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            if (leftButton.contains(x, y)) {
                leftPointer = pointerId;
                movingLeft = true;
            } else if (rightButton.contains(x, y)) {
                rightPointer = pointerId;
                movingRight = true;
            } else if (downButton.contains(x, y)) {
                downPointer = pointerId;
                crouching = grounded;
            } else if (jumpButton.contains(x, y)) {
                jump();
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
            if (pointerId == leftPointer) {
                movingLeft = false;
                leftPointer = -1;
            }
            if (pointerId == rightPointer) {
                movingRight = false;
                rightPointer = -1;
            }
            if (pointerId == downPointer) {
                crouching = false;
                downPointer = -1;
            }

            if (action == MotionEvent.ACTION_CANCEL) {
                movingLeft = false;
                movingRight = false;
                crouching = false;
                leftPointer = rightPointer = downPointer = -1;
            }
        }

        return true;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
