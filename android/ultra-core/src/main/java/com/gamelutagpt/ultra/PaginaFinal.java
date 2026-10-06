package com.gamelutagpt.ultra;

import java.util.ArrayList;
import java.util.List;

/**
 * Cinemática "Página Final": quando o ultra acerta, uma página de mangá se
 * monta painel por painel, o jogador toca no tempo certo para detonar o
 * golpe final e a página estilhaça de volta para a luta.
 *
 * Não depende de plataforma: o jogo chama {@link #update(float)},
 * {@link #render(UltraCanvas)} e {@link #tap()}, e recebe os acertos e o
 * fim pela {@link UltraListener}. Não é thread-safe: chame tudo da mesma
 * thread do loop do jogo.
 */
public final class PaginaFinal {
    public static final float VW = 1280f;
    public static final float VH = 720f;

    // ------------------------------------------------- linha do tempo (s)

    static final float PAGE_FADE = 0.16f;
    /** Quando cada painel bate na página, na ordem de {@link UltraSlot}. */
    static final float[] SLAM_AT = {0.10f, 0.66f, 1.22f, 1.80f, 2.38f};
    static final float SLAM_DURATION = 0.13f;
    static final float NAME_AT = 0.26f;
    static final float HIT1_AT = SLAM_AT[2] + 0.12f;
    static final float HIT2_AT = SLAM_AT[3] + 0.08f;
    static final float FINAL_EXPAND = 0.20f;
    static final float RING_START = SLAM_AT[4] + 0.12f;
    /** Instante (no relógio da cinemática) em que o anel fecha: o toque perfeito. */
    public static final float TARGET_AT = 3.18f;
    static final float PERFECT_WINDOW = 0.075f;
    static final float GOOD_WINDOW = 0.17f;
    static final float SHATTER_DELAY = 0.34f;
    static final float SHATTER_DURATION = 0.62f;

    static final float HIT_STOP = 0.07f;
    static final float FINAL_HIT_STOP = 0.11f;
    static final float INVERT_DURATION = 0.07f;
    static final float FLASH_DURATION = 0.32f;

    /** Divisão do dano total do ultra entre os três acertos. */
    static final float[] HIT_FRACTION = {0.25f, 0.25f, 0.50f};

    static final float RING_START_RADIUS = 300f;
    static final float RING_TARGET_RADIUS = 74f;
    static final float RING_X = VW * 0.5f;
    static final float RING_Y = VH * 0.5f + 20f;

    // ---------------------------------------------------------- layout

    private static final float[][] PANELS = {
        {28f, 28f, 1252f, 28f, 1252f, 206f, 28f, 246f},
        {28f, 266f, 470f, 256f, 420f, 692f, 28f, 692f},
        {490f, 255f, 1252f, 226f, 1252f, 452f, 462f, 470f},
        {460f, 490f, 1252f, 472f, 1252f, 692f, 440f, 692f},
        {-40f, -40f, VW + 40f, -40f, VW + 40f, VH + 40f, -40f, VH + 40f}
    };
    /** Onde cada onomatopeia aparece (x, y, rotação, tamanho). */
    private static final float[][] SFX_SPOT = {
        {0f, 0f, 0f, 0f},
        {250f, 650f, -10f, 64f},
        {960f, 395f, -8f, 104f},
        {1010f, 610f, 7f, 86f},
        {640f, 210f, -6f, 150f}
    };

    private static final int PAPER = 0xFFF1ECE1;
    private static final int INK = ArteProvisoria.INK;
    private static final int WHITE = ArteProvisoria.WHITE;

    private static final int SHARD_COLS = 6;
    private static final int SHARD_ROWS = 4;

    private static final class Shard {
        final float[] poly;
        final float cx;
        final float cy;
        final float vx;
        final float vy;
        final float spin;

        Shard(float[] poly, float cx, float cy, float vx, float vy, float spin) {
            this.poly = poly;
            this.cx = cx;
            this.cy = cy;
            this.vx = vx;
            this.vy = vy;
            this.spin = spin;
        }
    }

    private final UltraListener listener;
    private final ArteProvisoria art = new ArteProvisoria();
    private final Rand rand = new Rand(42);
    private final List<Shard> shards = new ArrayList<>();
    private final float[] panelBuffer = new float[8];

    private UltraPack pack;
    private int facing = 1;
    private boolean active;
    private float time;
    private float hitStop;
    private float shake;
    private float invertTimer;
    private float flashTimer;
    private float detonateAt = -1f;
    private float shatterTime = -1f;
    private float[] hitAt = {-1f, -1f};
    private UltraGrade grade = UltraGrade.NENHUM;
    private float gradeTimer;
    private long frame;

    public PaginaFinal(UltraListener listener) {
        this.listener = listener;
    }

    /**
     * Começa a cinemática.
     *
     * @param facing 1 se o atacante olha para a direita, -1 para a esquerda
     *               (o conteúdo dos painéis é espelhado; os textos não)
     */
    public void start(UltraPack pack, int facing) {
        this.pack = pack;
        this.facing = facing >= 0 ? 1 : -1;
        active = true;
        time = 0f;
        hitStop = 0f;
        shake = 0f;
        invertTimer = 0f;
        flashTimer = 0f;
        detonateAt = -1f;
        shatterTime = -1f;
        hitAt = new float[]{-1f, -1f};
        grade = UltraGrade.NENHUM;
        gradeTimer = 0f;
        shards.clear();
        listener.onUltraSound("pagina");
    }

    public boolean isActive() {
        return active;
    }

    /** Verdadeiro durante a quebra da página: o jogo por trás já aparece nos buracos. */
    public boolean isShattering() {
        return active && shatterTime >= 0f;
    }

    public UltraGrade grade() {
        return grade;
    }

    /** Relógio da cinemática em segundos (para durante as pausas de impacto). */
    public float clock() {
        return time;
    }

    /** O jogador tocou na tela (qualquer lugar). */
    public void tap() {
        if (!active || detonateAt >= 0f || grade != UltraGrade.NENHUM) return;
        if (time < RING_START) return;

        if (time < TARGET_AT - GOOD_WINDOW) {
            // Tocou cedo: perde o bônus, mas a detonação acontece no tempo normal.
            grade = UltraGrade.ERROU;
            gradeTimer = 0.9f;
            return;
        }
        grade = Math.abs(time - TARGET_AT) <= PERFECT_WINDOW ? UltraGrade.PERFEITO : UltraGrade.BOM;
        detonate();
    }

    public void update(float dt) {
        if (!active) return;
        frame++;

        shake = Math.max(0f, shake - dt * 70f);
        invertTimer = Math.max(0f, invertTimer - dt);
        flashTimer = Math.max(0f, flashTimer - dt);
        gradeTimer = Math.max(0f, gradeTimer - dt);

        if (hitStop > 0f) {
            hitStop = Math.max(0f, hitStop - dt);
            return;
        }

        float previous = time;
        time += dt;

        for (UltraSlot slot : UltraSlot.values()) {
            if (crossed(previous, SLAM_AT[slot.ordinal()])) onSlam(slot);
        }
        if (crossed(previous, HIT1_AT)) impact(0, UltraSlot.GOLPE);
        if (crossed(previous, HIT2_AT)) impact(1, UltraSlot.ATINGIDO);

        if (detonateAt < 0f) {
            boolean lateMiss = grade == UltraGrade.ERROU && time >= TARGET_AT;
            boolean noTap = grade == UltraGrade.NENHUM && time >= TARGET_AT + GOOD_WINDOW;
            if (lateMiss || noTap) detonate();
        } else if (shatterTime < 0f && time >= detonateAt + SHATTER_DELAY) {
            startShatter();
        } else if (shatterTime >= 0f && time - shatterTime >= SHATTER_DURATION) {
            active = false;
            listener.onUltraFinished(grade);
        }
    }

    private boolean crossed(float previous, float at) {
        return previous < at && time >= at;
    }

    private void onSlam(UltraSlot slot) {
        shake = Math.max(shake, slot == UltraSlot.FINAL ? 12f : 7f);
        listener.onUltraSound("painel");
        if (slot == UltraSlot.OLHOS || slot == UltraSlot.CARGA) {
            String sound = pack.definition.panel(slot).soundId;
            if (sound != null && !sound.equals("painel")) listener.onUltraSound(sound);
        }
    }

    private void impact(int index, UltraSlot slot) {
        hitAt[index] = time;
        hitStop = HIT_STOP;
        invertTimer = INVERT_DURATION;
        shake = Math.max(shake, 16f);
        listener.onUltraSound(pack.definition.panel(slot).soundId);
        listener.onUltraHit(index, HIT_FRACTION[index]);
    }

    private void detonate() {
        detonateAt = time;
        hitStop = FINAL_HIT_STOP;
        invertTimer = INVERT_DURATION + 0.03f;
        flashTimer = FLASH_DURATION;
        shake = 30f;
        if (grade != UltraGrade.NENHUM) gradeTimer = 1.0f;
        listener.onUltraSound(pack.definition.panel(UltraSlot.FINAL).soundId);
        listener.onUltraHit(2, HIT_FRACTION[2] * (1f + grade.finalBonus));
    }

    private void startShatter() {
        shatterTime = time;
        shards.clear();
        listener.onUltraSound("quebra");

        float cellW = VW / SHARD_COLS;
        float cellH = VH / SHARD_ROWS;
        float[][] px = new float[SHARD_ROWS + 1][SHARD_COLS + 1];
        float[][] py = new float[SHARD_ROWS + 1][SHARD_COLS + 1];
        rand.reset(frame);
        for (int row = 0; row <= SHARD_ROWS; row++) {
            for (int col = 0; col <= SHARD_COLS; col++) {
                boolean edgeX = col == 0 || col == SHARD_COLS;
                boolean edgeY = row == 0 || row == SHARD_ROWS;
                float jx = edgeX ? 0f : rand.range(-0.35f, 0.35f) * cellW;
                float jy = edgeY ? 0f : rand.range(-0.35f, 0.35f) * cellH;
                px[row][col] = col * cellW + jx + (edgeX ? (col == 0 ? -40f : 40f) : 0f);
                py[row][col] = row * cellH + jy + (edgeY ? (row == 0 ? -40f : 40f) : 0f);
            }
        }
        for (int row = 0; row < SHARD_ROWS; row++) {
            for (int col = 0; col < SHARD_COLS; col++) {
                boolean flip = ((row + col) & 1) == 0;
                float ax = px[row][col], ay = py[row][col];
                float bx = px[row][col + 1], by = py[row][col + 1];
                float cx = px[row + 1][col + 1], cy = py[row + 1][col + 1];
                float dx = px[row + 1][col], dy = py[row + 1][col];
                if (flip) {
                    addShard(new float[]{ax, ay, bx, by, cx, cy});
                    addShard(new float[]{ax, ay, cx, cy, dx, dy});
                } else {
                    addShard(new float[]{ax, ay, bx, by, dx, dy});
                    addShard(new float[]{bx, by, cx, cy, dx, dy});
                }
            }
        }
    }

    private void addShard(float[] poly) {
        float cx = (poly[0] + poly[2] + poly[4]) / 3f;
        float cy = (poly[1] + poly[3] + poly[5]) / 3f;
        float dx = cx - RING_X;
        float dy = cy - RING_Y;
        float len = Math.max(1f, (float)Math.sqrt(dx * dx + dy * dy));
        float speed = rand.range(700f, 1500f);
        shards.add(new Shard(
            poly,
            cx,
            cy,
            dx / len * speed,
            dy / len * speed - rand.range(150f, 450f),
            rand.range(-540f, 540f)
        ));
    }

    // ------------------------------------------------------------ render

    public void render(UltraCanvas c) {
        if (!active) return;

        boolean invert = invertTimer > 0f;
        if (invert) c.beginInvert();

        c.save();
        if (shake > 0f) {
            rand.reset(frame * 7919L);
            c.translate(rand.range(-shake, shake), rand.range(-shake, shake));
        }

        if (shatterTime < 0f) {
            drawPage(c);
        } else {
            drawShards(c);
        }
        drawSfx(c, UltraSlot.FINAL, detonateAt);
        drawGrade(c);

        c.restore();
        if (invert) c.endInvert();

        if (flashTimer > 0f) {
            int alpha = (int)(255f * flashTimer / FLASH_DURATION);
            c.fillRect(-60f, -60f, VW + 60f, VH + 60f, ArteProvisoria.withAlpha(WHITE, alpha));
        }
    }

    private void drawPage(UltraCanvas c) {
        int paperAlpha = (int)(255f * ArteProvisoria.clamp01(time / PAGE_FADE));
        c.fillRect(-60f, -60f, VW + 60f, VH + 60f, ArteProvisoria.withAlpha(PAPER, paperAlpha));

        boolean finalCoversAll = time >= SLAM_AT[4] + FINAL_EXPAND;
        if (!finalCoversAll) {
            for (int i = 0; i < 4; i++) {
                UltraSlot slot = UltraSlot.values()[i];
                if (time >= SLAM_AT[i]) drawPanel(c, slot, PANELS[i], time - SLAM_AT[i]);
            }
            drawName(c);
            drawSfx(c, UltraSlot.CARGA, SLAM_AT[1] + 0.10f);
            drawSfx(c, UltraSlot.GOLPE, hitAt[0]);
            drawSfx(c, UltraSlot.ATINGIDO, hitAt[1]);
        }

        if (time >= SLAM_AT[4]) {
            float age = time - SLAM_AT[4];
            float e = ArteProvisoria.easeOutCubic(ArteProvisoria.clamp01(age / FINAL_EXPAND));
            float[] from = PANELS[3];
            float[] to = PANELS[4];
            for (int i = 0; i < 8; i++) panelBuffer[i] = from[i] + (to[i] - from[i]) * e;
            drawPanelBody(c, UltraSlot.FINAL, panelBuffer.clone(), age);
            drawRing(c);
            drawCracks(c);
        }
    }

    private void drawPanel(UltraCanvas c, UltraSlot slot, float[] poly, float age) {
        float t = ArteProvisoria.clamp01(age / SLAM_DURATION);
        float u = 1f - t;
        float cx = (poly[0] + poly[2] + poly[4] + poly[6]) * 0.25f;
        float cy = (poly[1] + poly[3] + poly[5] + poly[7]) * 0.25f;

        c.save();
        c.scale(1f + 0.30f * u * u, 1f + 0.30f * u * u, cx, cy);
        c.rotate((slot.ordinal() % 2 == 0 ? 6f : -6f) * u * u, cx, cy);

        float[] shadow = new float[8];
        for (int i = 0; i < 8; i += 2) {
            shadow[i] = poly[i] + 9f;
            shadow[i + 1] = poly[i + 1] + 11f;
        }
        c.fillPolygon(shadow, 0x66000000);
        drawPanelBody(c, slot, poly, age);

        // Clarão branco no instante em que o painel "pousa".
        float land = age - SLAM_DURATION;
        if (land >= 0f && land < 0.12f) {
            c.fillPolygon(poly, ArteProvisoria.withAlpha(WHITE, (int)(200f * (1f - land / 0.12f))));
        }
        c.restore();
    }

    private void drawPanelBody(UltraCanvas c, UltraSlot slot, float[] poly, float age) {
        float l = Math.min(Math.min(poly[0], poly[2]), Math.min(poly[4], poly[6]));
        float r = Math.max(Math.max(poly[0], poly[2]), Math.max(poly[4], poly[6]));
        float t = Math.min(Math.min(poly[1], poly[3]), Math.min(poly[5], poly[7]));
        float b = Math.max(Math.max(poly[1], poly[3]), Math.max(poly[5], poly[7]));

        c.save();
        c.clipPolygon(poly);
        drawSlotContent(c, slot, l, t, r, b, age);
        c.restore();
        c.strokePolygon(poly, slot == UltraSlot.FINAL ? 10f : 8f, INK);
    }

    /** Conteúdo do painel com movimento de câmera lento (zoom contínuo). */
    private void drawSlotContent(UltraCanvas c, UltraSlot slot, float l, float t, float r, float b, float age) {
        float cx = (l + r) * 0.5f;
        float cy = (t + b) * 0.5f;
        float zoom = slot == UltraSlot.FINAL
            ? 1.02f + 0.10f * ArteProvisoria.clamp01(age / 1.2f)
            : 1.03f + 0.09f * ArteProvisoria.clamp01(age / 1.6f);
        int color = pack.definition.color;
        int accent = pack.definition.accentColor;
        long seed = frame / 2;
        float fx = slotFx(slot);

        c.save();
        c.scale(zoom, zoom, cx, cy);
        if (facing < 0) c.scale(-1f, 1f, cx, cy);

        UltraImage image = pack.image(slot);
        if (image != null) {
            drawImageCover(c, image, l, t, r, b);
        } else {
            art.drawContent(c, slot, l, t, r, b, age, fx, color, accent, seed);
        }
        art.drawOverlay(c, slot, l, t, r, b, age, fx, color, accent, seed);
        c.restore();
    }

    private float slotFx(UltraSlot slot) {
        if (slot == UltraSlot.GOLPE) return hitAt[0] >= 0f ? time - hitAt[0] : -1f;
        if (slot == UltraSlot.FINAL) return detonateAt >= 0f ? time - detonateAt : -1f;
        return -1f;
    }

    private void drawImageCover(UltraCanvas c, UltraImage image, float l, float t, float r, float b) {
        float boxW = r - l;
        float boxH = b - t;
        float scale = Math.max(boxW / image.width(), boxH / image.height());
        float w = image.width() * scale;
        float h = image.height() * scale;
        float x = l + (boxW - w) * 0.5f;
        float y = t + (boxH - h) * 0.5f;
        c.drawImage(image, x, y, x + w, y + h, 255);
    }

    private void drawName(UltraCanvas c) {
        if (time < NAME_AT) return;
        float t = ArteProvisoria.easeOutCubic(ArteProvisoria.clamp01((time - NAME_AT) / 0.18f));
        float offset = (1f - t) * 900f;

        // A faixa atravessa a borda de baixo do primeiro painel, como nos mangás.
        float[] strip = {
            650f + offset, 206f,
            1266f + offset, 188f,
            1266f + offset, 262f,
            616f + offset, 280f
        };
        c.fillPolygon(strip, INK);
        c.drawText(
            pack.definition.name,
            945f + offset,
            255f,
            fitTextSize(pack.definition.name, 58f, 580f),
            pack.definition.color,
            WHITE,
            5f,
            true
        );
        c.drawText("ULTRA", 708f + offset, 204f, 26f, WHITE, INK, 8f, true);
    }

    private static float fitTextSize(String text, float max, float width) {
        // Aproximação: cada caractere em negrito ocupa ~0.62 do tamanho da fonte.
        float estimate = text.length() * 0.62f * max;
        return estimate <= width ? max : max * width / estimate;
    }

    private void drawSfx(UltraCanvas c, UltraSlot slot, float at) {
        if (at < 0f || time < at) return;
        String text = pack.definition.panel(slot).onomatopoeia;
        if (text == null || text.isEmpty()) return;

        float age = time - at;
        if (slot != UltraSlot.FINAL && time >= SLAM_AT[4] + FINAL_EXPAND) return;
        float[] spot = SFX_SPOT[slot.ordinal()];
        float x = spot[0];
        float y = spot[1];
        float[] custom = pack.definition.panel(slot).onomatopoeiaPosition;
        if (custom != null) {
            float[] poly = PANELS[slot.ordinal()];
            float l = Math.max(0f, Math.min(Math.min(poly[0], poly[2]), Math.min(poly[4], poly[6])));
            float r = Math.min(VW, Math.max(Math.max(poly[0], poly[2]), Math.max(poly[4], poly[6])));
            float t = Math.max(0f, Math.min(Math.min(poly[1], poly[3]), Math.min(poly[5], poly[7])));
            float b = Math.min(VH, Math.max(Math.max(poly[1], poly[3]), Math.max(poly[5], poly[7])));
            x = l + (r - l) * custom[0];
            y = t + (b - t) * custom[1];
        }
        float pop = ArteProvisoria.clamp01(age / 0.10f);
        float scale = 1.7f - 0.7f * ArteProvisoria.easeOutCubic(pop);
        rand.reset(frame * 13L + slot.ordinal());
        float jitter = 2.5f;

        c.save();
        c.translate(rand.range(-jitter, jitter), rand.range(-jitter, jitter));
        c.rotate(spot[2], x, y);
        c.scale(scale, scale, x, y);
        int fill = slot == UltraSlot.FINAL ? pack.definition.accentColor : WHITE;
        c.drawText(text, x, y, spot[3], fill, INK, spot[3] * 0.16f, true);
        c.restore();
    }

    private void drawRing(UltraCanvas c) {
        if (detonateAt >= 0f || time < RING_START) return;

        float p = ArteProvisoria.clamp01((time - RING_START) / (TARGET_AT - RING_START));
        float radius = RING_START_RADIUS + (RING_TARGET_RADIUS - RING_START_RADIUS) * p;
        boolean inWindow = Math.abs(time - TARGET_AT) <= GOOD_WINDOW;

        if (inWindow) c.fillCircle(RING_X, RING_Y, RING_TARGET_RADIUS, 0x88FFFFFF);
        c.strokeCircle(RING_X, RING_Y, RING_TARGET_RADIUS, 12f, INK);
        c.strokeCircle(RING_X, RING_Y, RING_TARGET_RADIUS, 5f, pack.definition.accentColor);
        c.strokeCircle(RING_X, RING_Y, radius, 16f, INK);
        c.strokeCircle(RING_X, RING_Y, radius, 8f, WHITE);

        if (grade == UltraGrade.NENHUM) {
            float pulse = 1f + 0.08f * (float)Math.sin(time * 24f);
            c.save();
            c.scale(pulse, pulse, RING_X, RING_Y);
            c.drawText("TOQUE!", RING_X, RING_Y + 16f, 44f, WHITE, INK, 10f, true);
            c.restore();
        }
    }

    private void drawCracks(UltraCanvas c) {
        if (detonateAt < 0f) return;
        float grow = ArteProvisoria.clamp01((time - detonateAt) / 0.16f);
        rand.reset(9001);
        for (int i = 0; i < 11; i++) {
            float angle = (float)(Math.PI * 2 * (i + rand.next() * 0.6f) / 11);
            float length = rand.range(500f, 900f) * grow;
            float x = RING_X;
            float y = RING_Y;
            int segments = 4;
            for (int s = 0; s < segments; s++) {
                float a = angle + rand.range(-0.35f, 0.35f);
                float step = length / segments;
                float nx = x + (float)Math.cos(a) * step;
                float ny = y + (float)Math.sin(a) * step;
                c.drawLine(x, y, nx, ny, 9f - s * 1.5f, INK);
                c.drawLine(x, y, nx, ny, 3f, WHITE);
                x = nx;
                y = ny;
            }
        }
    }

    private void drawShards(UltraCanvas c) {
        float age = time - shatterTime;
        float detonationAge = time - detonateAt;
        art.lite = true;
        for (Shard shard : shards) {
            float dx = shard.vx * age;
            float dy = shard.vy * age + 1400f * age * age;
            float shrink = 1f - 0.45f * ArteProvisoria.clamp01(age / SHATTER_DURATION);

            c.save();
            c.translate(dx, dy);
            c.rotate(shard.spin * age, shard.cx, shard.cy);
            c.scale(shrink, shrink, shard.cx, shard.cy);
            c.save();
            c.clipPolygon(shard.poly);
            drawSlotContent(c, UltraSlot.FINAL, -40f, -40f, VW + 40f, VH + 40f, detonationAge + 0.8f);
            c.restore();
            c.strokePolygon(shard.poly, 5f, INK);
            c.restore();
        }
        art.lite = false;
    }

    private void drawGrade(UltraCanvas c) {
        if (gradeTimer <= 0f || grade == UltraGrade.NENHUM) return;
        float age = 1.0f - gradeTimer;
        float pop = ArteProvisoria.easeOutCubic(ArteProvisoria.clamp01(age / 0.12f));
        float scale = 1.5f - 0.5f * pop;
        int fill = grade == UltraGrade.PERFEITO
            ? 0xFFFFD23A
            : (grade == UltraGrade.BOM ? WHITE : 0xFFB8BCC6);

        c.save();
        c.scale(scale, scale, RING_X, VH - 120f);
        c.drawText(grade.label, RING_X, VH - 100f, 84f, fill, INK, 14f, true);
        c.restore();
    }
}
