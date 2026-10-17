package com.gamelutagpt;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;

/**
 * World-space hit and guard sparks. A fixed pool advanced on the 60 Hz simulation clock:
 * spawning reuses slots, drawing allocates nothing and nothing here touches combat state.
 */
final class ImpactEffects {
    static final int HIT = 0, GUARD = 1, DUST = 2, LAUNCH = 3;
    /** Size class of a contact: 0 light, 1 medium, 2 heavy, 3 special/super. */
    static final int LIGHT = 0, MEDIUM = 1, HEAVY = 2, SPECIAL = 3;

    private static final int BURSTS = 12, DEBRIS = 96, SPEARS = 16;
    /**
     * Raios de luz (como no FighterZ): lanças finas e longas que atravessam a tela pelo ponto
     * do acerto. Quantas por tamanho de golpe, a largura máxima e o comprimento (mundo).
     */
    private static final int[] SPEAR_COUNT = {1, 2, 3, 4};
    /** Largura na ponta de fora (no impacto o raio nasce fino). */
    private static final float[] SPEAR_WIDTH = {16f, 24f, 32f, 40f};
    private static final float SPEAR_LENGTH = 1100f, SPEAR_LIFE = .16f;
    private static final float[] BURST_LIFE = {.20f, .24f, .30f, .36f};
    private static final float[] BURST_SIZE = {46f, 62f, 84f, 104f};
    private static final int[] DEBRIS_COUNT = {7, 10, 15, 20};

    // Bursts: flash core, star spikes and shock ring at the contact point.
    private final float[] bx = new float[BURSTS], by = new float[BURSTS], bAge = new float[BURSTS];
    private final float[] bLife = new float[BURSTS], bSize = new float[BURSTS], bAngle = new float[BURSTS];
    private final int[] bKind = new int[BURSTS], bColor = new int[BURSTS], bDir = new int[BURSTS];
    private int nextBurst;

    // Debris: short streaks thrown away from the impact, with gravity and drag.
    private final float[] dx = new float[DEBRIS], dy = new float[DEBRIS], dvx = new float[DEBRIS], dvy = new float[DEBRIS];
    private final float[] dAge = new float[DEBRIS], dLife = new float[DEBRIS], dWidth = new float[DEBRIS];
    private final int[] dColor = new int[DEBRIS];
    private int nextDebris;

    // Spears: centre, direction, how far the bright point sits along it, age and width.
    private final float[] sx = new float[SPEARS], sy = new float[SPEARS], sAngle = new float[SPEARS];
    private final float[] sAge = new float[SPEARS], sWidth = new float[SPEARS], sShift = new float[SPEARS];
    private final int[] sColor = new int[SPEARS];
    private int nextSpear;
    private int seed = 0x2F6E2B1;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path star = new Path();
    private final Shader[] cores = new Shader[2];

    ImpactEffects() {
        java.util.Arrays.fill(bAge, 99f);
        java.util.Arrays.fill(bLife, 1f);
        java.util.Arrays.fill(dAge, 99f);
        java.util.Arrays.fill(dLife, 1f);
        java.util.Arrays.fill(sAge, 99f);
        // Unit-radius cores, scaled per burst through the canvas: no shader per frame.
        cores[HIT] = new RadialGradient(0f, 0f, 1f,
            new int[]{0xFFFFFFFF, 0xFFFFF4C2, 0xCCFFB23A, 0x00FF5A1E}, new float[]{0f, .28f, .6f, 1f},
            Shader.TileMode.CLAMP);
        cores[GUARD] = new RadialGradient(0f, 0f, 1f,
            new int[]{0xFFFFFFFF, 0xFFD6F6FF, 0xAA4FC8FF, 0x002A7BFF}, new float[]{0f, .3f, .62f, 1f},
            Shader.TileMode.CLAMP);
    }

    void clear() {
        java.util.Arrays.fill(bAge, 99f);
        java.util.Arrays.fill(dAge, 99f);
        java.util.Arrays.fill(sAge, 99f);
    }

    boolean active() {
        for (int i = 0; i < BURSTS; i++) if (bAge[i] < bLife[i]) return true;
        for (int i = 0; i < DEBRIS; i++) if (dAge[i] < dLife[i]) return true;
        for (int i = 0; i < SPEARS; i++) if (sAge[i] < SPEAR_LIFE) return true;
        return false;
    }

    static int sizeOf(AttackDefinition.Strength strength, int damage) {
        if (strength == null) return damage >= 120 ? HEAVY : damage >= 60 ? MEDIUM : LIGHT;
        switch (strength) {
            case LIGHT: return LIGHT;
            case MEDIUM: return MEDIUM;
            case HEAVY: return HEAVY;
            default: return SPECIAL;
        }
    }

    /**
     * @param direction +1 when the blow travels to the right (sparks fly with it)
     * @param accent    attacker colour, used for the outer debris
     */
    void spawn(int kind, float x, float y, int direction, int size, int accent) {
        size = Math.max(LIGHT, Math.min(SPECIAL, size));
        int b = nextBurst;
        nextBurst = (nextBurst + 1) % BURSTS;
        bx[b] = x; by[b] = y; bAge[b] = 0f; bKind[b] = kind; bDir[b] = direction >= 0 ? 1 : -1;
        bLife[b] = BURST_LIFE[size] * (kind == GUARD ? .9f : 1f);
        bSize[b] = BURST_SIZE[size] * (kind == GUARD ? .85f : 1f);
        bAngle[b] = random() * 6.2832f;
        bColor[b] = kind == GUARD ? 0xFF6FD8FF : accent;
        if (kind == HIT) spawnSpears(x, y, size, accent);
        int count = DEBRIS_COUNT[size];
        for (int n = 0; n < count; n++) {
            int d = nextDebris;
            nextDebris = (nextDebris + 1) % DEBRIS;
            // Mostly along the blow, fanned ±70°; guard sparks bounce back towards the attacker.
            float spread = (random() - .5f) * 2.45f;
            float base = kind == GUARD ? (direction >= 0 ? 3.1416f : 0f) : (direction >= 0 ? 0f : 3.1416f);
            float angle = base + spread - .25f;
            float speed = (260f + 520f * random()) * (1f + .22f * size);
            dx[d] = x; dy[d] = y;
            dvx[d] = (float)Math.cos(angle) * speed;
            dvy[d] = (float)Math.sin(angle) * speed - 120f * random();
            dAge[d] = 0f;
            dLife[d] = .16f + .22f * random() + .03f * size;
            dWidth[d] = 3f + random() * (2f + size);
            int pick = (int)(random() * 3);
            dColor[d] = kind == GUARD ? (pick == 0 ? 0xFFFFFFFF : 0xFF7FE3FF)
                : pick == 0 ? 0xFFFFFFFF : pick == 1 ? 0xFFFFD25A : (accent | 0xFF000000);
        }
    }

    /**
     * Raios de um acerto, espalhados em ângulos diferentes e nunca deitados demais: diagonais
     * que cruzam a tela, como as do FighterZ.
     */
    private void spawnSpears(float x, float y, int size, int accent) {
        int count = SPEAR_COUNT[size];
        float base = random() * 3.1416f;
        for (int n = 0; n < count; n++) {
            int s = nextSpear;
            nextSpear = (nextSpear + 1) % SPEARS;
            // Cada raio sai do impacto para um lado; juntos formam um V ou um leque aberto.
            float a = base + n * 6.2832f / count * (.75f + .25f * random()) + (random() - .5f) * .35f;
            // Pelo menos ~12° fora da horizontal.
            float sin = (float)Math.sin(a);
            if (Math.abs(sin) < .2f) a += sin >= 0 ? .25f : -.25f;
            sx[s] = x; sy[s] = y; sAngle[s] = a; sAge[s] = 0f;
            sWidth[s] = SPEAR_WIDTH[size] * (.7f + .5f * random());
            sShift[s] = .8f + .4f * random();
            sColor[s] = n % 3 == 2 ? (accent | 0xFF000000) : 0xFFFFA62B;
        }
    }

    /**
     * Poeira e pedrinhas quando um corpo bate no chão com força (queda, quique). {@code power}
     * 0..1.
     */
    void spawnDust(float x, float groundY, float power) {
        power = Math.max(0f, Math.min(1f, power));
        int b = nextBurst;
        nextBurst = (nextBurst + 1) % BURSTS;
        bx[b] = x; by[b] = groundY; bAge[b] = 0f; bKind[b] = DUST; bDir[b] = 1;
        bLife[b] = .45f + .25f * power;
        bSize[b] = 70f + 70f * power;
        bAngle[b] = 0f;
        bColor[b] = 0xFFCDBFA6;
        int count = 10 + Math.round(14 * power);
        for (int n = 0; n < count; n++) {
            int d = nextDebris;
            nextDebris = (nextDebris + 1) % DEBRIS;
            float angle = -3.1416f * (.12f + .76f * random());
            float speed = (220f + 520f * random()) * (.7f + .5f * power);
            dx[d] = x + (random() - .5f) * 60f; dy[d] = groundY - 4f;
            dvx[d] = (float)Math.cos(angle) * speed;
            dvy[d] = (float)Math.sin(angle) * speed;
            dAge[d] = 0f;
            dLife[d] = .28f + .3f * random();
            dWidth[d] = 4f + random() * 6f;
            int pick = (int)(random() * 3);
            dColor[d] = pick == 0 ? 0xFF8E8172 : pick == 1 ? 0xFFB8A88F : 0xFF6B6157;
        }
    }

    /**
     * Saída do super pulo: um anel de choque achatado no chão que se abre, uma coluna de luz
     * curta subindo dos pés, riscos de velocidade para cima e a poeira do chão. {@code accent}
     * é a cor do lutador.
     */
    void spawnSuperJump(float x, float groundY, int accent) {
        spawnDust(x, groundY, .5f);
        int b = nextBurst;
        nextBurst = (nextBurst + 1) % BURSTS;
        bx[b] = x; by[b] = groundY; bAge[b] = 0f; bKind[b] = LAUNCH; bDir[b] = 1;
        bLife[b] = .42f;
        bSize[b] = 120f;
        bAngle[b] = 0f;
        bColor[b] = accent | 0xFF000000;
        for (int n = 0; n < 9; n++) {
            int d = nextDebris;
            nextDebris = (nextDebris + 1) % DEBRIS;
            dx[d] = x + (random() - .5f) * 110f; dy[d] = groundY - 10f - random() * 30f;
            dvx[d] = (random() - .5f) * 120f;
            dvy[d] = -(1300f + 700f * random());
            dAge[d] = 0f;
            dLife[d] = .16f + .12f * random();
            dWidth[d] = 3f + random() * 4f;
            dColor[d] = n % 3 == 0 ? (accent | 0xFF000000) : 0xFFF4F8FF;
        }
    }

    void update(float dt) {
        for (int i = 0; i < SPEARS; i++) if (sAge[i] < SPEAR_LIFE) sAge[i] += dt;
        for (int i = 0; i < BURSTS; i++) if (bAge[i] < bLife[i]) bAge[i] += dt;
        for (int i = 0; i < DEBRIS; i++) {
            if (dAge[i] >= dLife[i]) continue;
            dAge[i] += dt;
            float drag = (float)Math.pow(.02, dt);
            dvx[i] *= drag;
            dvy[i] = dvy[i] * drag + 1500f * dt;
            dx[i] += dvx[i] * dt;
            dy[i] += dvy[i] * dt;
        }
    }

    void draw(Canvas c) {
        drawSpears(c);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < DEBRIS; i++) {
            if (dAge[i] >= dLife[i]) continue;
            float k = dAge[i] / dLife[i];
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dWidth[i] * (1f - .6f * k));
            paint.setColor(dColor[i]);
            paint.setAlpha(Math.round(255 * (1f - k * k)));
            // Streak along velocity: fast sparks read as lines, slow ones as dots.
            float tail = .045f;
            c.drawLine(dx[i], dy[i], dx[i] - dvx[i] * tail, dy[i] - dvy[i] * tail, paint);
        }
        paint.setAlpha(255);
        for (int i = 0; i < BURSTS; i++) {
            if (bAge[i] >= bLife[i]) continue;
            float k = bAge[i] / bLife[i];
            drawBurst(c, i, k);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setShader(null);
        paint.setAlpha(255);
    }

    /**
     * Raio saindo do impacto: nasce fino no ponto do golpe e abre até a ponta, com um brilho
     * largo e translúcido em volta e o miolo claro. Cresce rápido e afina sumindo.
     */
    private void drawSpears(Canvas c) {
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < SPEARS; i++) {
            if (sAge[i] >= SPEAR_LIFE) continue;
            float k = sAge[i] / SPEAR_LIFE;
            float reach = Math.min(1f, k / .25f);
            float len = SPEAR_LENGTH * sShift[i] * (.4f + .6f * reach);
            float width = sWidth[i] * (1f - .7f * k * k);
            float ux = (float)Math.cos(sAngle[i]), uy = (float)Math.sin(sAngle[i]);
            float x0 = sx[i] + ux * 18f, y0 = sy[i] + uy * 18f;
            float x1 = sx[i] + ux * len, y1 = sy[i] + uy * len;
            float fade = 1f - k;
            for (int layer = 0; layer < 3; layer++) {
                float w = layer == 0 ? width * 1.9f : layer == 1 ? width : width * .36f;
                float w0 = layer == 0 ? 6f : layer == 1 ? 2.5f : 1f;
                star.reset();
                star.moveTo(x0 - uy * w0, y0 + ux * w0);
                star.lineTo(x1 - uy * w, y1 + ux * w);
                star.lineTo(x1 + ux * w * 1.5f, y1 + uy * w * 1.5f);
                star.lineTo(x1 + uy * w, y1 - ux * w);
                star.lineTo(x0 + uy * w0, y0 - ux * w0);
                star.close();
                paint.setColor(layer == 0 ? 0xFFFF6A10 : layer == 1 ? sColor[i] : 0xFFFFF8DA);
                paint.setAlpha(Math.round((layer == 0 ? 80 : layer == 1 ? 225 : 255) * fade));
                c.drawPath(star, paint);
            }
        }
        paint.setAlpha(255);
    }

    private void drawBurst(Canvas c, int i, float k) {
        if (bKind[i] == DUST) {
            drawDust(c, i, k);
            return;
        }
        if (bKind[i] == LAUNCH) {
            drawLaunch(c, i, k);
            return;
        }
        float size = bSize[i];
        float grow = 1f - (1f - k) * (1f - k) * (1f - k);
        float fade = 1f - k;
        c.save();
        c.translate(bx[i], by[i]);
        if (bDir[i] < 0) c.scale(-1f, 1f);

        // Shock ring: expands fast and thins out.
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, size * .14f * fade));
        paint.setColor(bKind[i] == GUARD ? 0xFF9BE9FF : 0xFFFFE6A6);
        paint.setAlpha(Math.round(170 * fade));
        c.drawOval(-size * (.35f + 1.05f * grow), -size * (.28f + .8f * grow),
            size * (.35f + 1.05f * grow), size * (.28f + .8f * grow), paint);

        if (bKind[i] == GUARD) {
            // Barrier arc facing the attacker, plus a hexagon flash.
            paint.setStrokeWidth(size * .16f * fade + 2f);
            paint.setColor(0xFFBFF2FF);
            paint.setAlpha(Math.round(255 * fade));
            float r = size * (.55f + .3f * grow);
            c.drawArc(-r, -r * 1.25f, r, r * 1.25f, 120f, 120f, false, paint);
            paint.setColor(bColor[i]);
            paint.setStrokeWidth(Math.max(1.5f, size * .07f));
            star.reset();
            for (int s = 0; s < 6; s++) {
                double a = Math.PI / 3 * s + Math.PI / 6;
                float hx = (float)Math.cos(a) * r * .78f, hy = (float)Math.sin(a) * r * .78f;
                if (s == 0) star.moveTo(hx, hy); else star.lineTo(hx, hy);
            }
            star.close();
            c.drawPath(star, paint);
        }

        // Star spikes: long rays, longest along the blow direction.
        int spikes = bKind[i] == GUARD ? 8 : 9;
        float inner = size * .24f, outer = size * (.55f + .7f * grow) * (k < .12f ? .6f + 3.3f * k : 1f);
        star.reset();
        for (int s = 0; s < spikes * 2; s++) {
            double a = bAngle[i] + Math.PI * s / spikes;
            float len = s % 2 == 0 ? outer * (s % 4 == 0 ? 1f : .62f) : inner;
            float along = (float)Math.cos(a);
            if (s % 2 == 0 && along > .6f && bKind[i] == HIT) len *= 1.35f;
            float px = (float)Math.cos(a) * len, py = (float)Math.sin(a) * len * .82f;
            if (s == 0) star.moveTo(px, py); else star.lineTo(px, py);
        }
        star.close();
        paint.setStyle(Paint.Style.FILL);
        // Hot orange outer star, then the white one inside it.
        paint.setColor(bKind[i] == GUARD ? 0xFF7CDFFF : 0xFFFF9A2A);
        paint.setAlpha(Math.round(235 * fade));
        c.save(); c.scale(1.3f, 1.3f); c.drawPath(star, paint); c.restore();
        paint.setColor(0xFFFFFFFF);
        paint.setAlpha(Math.round(255 * Math.min(1f, fade * 1.6f)));
        c.drawPath(star, paint);

        // Hot core: radial gradient, brightest in the first frames.
        float core = size * (.42f + .18f * grow) * (k < .5f ? 1f : 1.6f - 1.2f * k);
        if (core > 1f) {
            c.save();
            c.scale(core, core);
            paint.setShader(cores[bKind[i]]);
            paint.setAlpha(Math.round(255 * Math.min(1f, fade * 1.4f)));
            c.drawCircle(0f, 0f, 1f, paint);
            paint.setShader(null);
            c.restore();
        }
        c.restore();
    }

    /** Saída do super pulo: anel achatado no chão que se abre e coluna de luz que sobe e some. */
    private void drawLaunch(Canvas c, int i, float k) {
        float size = bSize[i];
        float grow = 1f - (1f - k) * (1f - k) * (1f - k);
        float fade = 1f - k;
        paint.setShader(null);
        // Coluna de luz: sobe dos pés no começo e some rápido.
        float column = Math.max(0f, 1f - k * 2.2f);
        if (column > 0f) {
            paint.setStyle(Paint.Style.FILL);
            float h = size * (1.2f + 1.6f * grow);
            for (int layer = 0; layer < 2; layer++) {
                float w = size * (layer == 0 ? .42f : .16f) * (1f - .5f * k);
                paint.setColor(layer == 0 ? bColor[i] : 0xFFFFFFFF);
                paint.setAlpha(Math.round((layer == 0 ? 90 : 170) * column));
                c.drawOval(bx[i] - w, by[i] - h, bx[i] + w, by[i] + 6f, paint);
            }
        }
        // Anéis de choque achatados no chão (perspectiva), o de fora na cor do lutador.
        paint.setStyle(Paint.Style.STROKE);
        for (int ring = 0; ring < 2; ring++) {
            float r = size * (.35f + (ring == 0 ? 1.5f : 1.05f) * grow);
            paint.setStrokeWidth((ring == 0 ? 9f : 5f) * (1f - .6f * k));
            paint.setColor(ring == 0 ? bColor[i] : 0xFFFFFFFF);
            paint.setAlpha(Math.round((ring == 0 ? 210 : 240) * fade));
            c.drawOval(bx[i] - r, by[i] - r * .22f, bx[i] + r, by[i] + r * .22f, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
    }

    /** Nuvem de poeira: elipses baixas que se abrem para os lados e somem. */
    private void drawDust(Canvas c, int i, float k) {
        float size = bSize[i];
        float grow = 1f - (1f - k) * (1f - k);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        for (int puff = 0; puff < 3; puff++) {
            float spread = size * (.6f + 1.6f * grow) * (1f + .25f * puff);
            float height = size * (.22f + .35f * grow) * (1f - .2f * puff);
            paint.setColor(bColor[i]);
            paint.setAlpha(Math.round(150 * (1f - k) * (1f - .25f * puff)));
            c.drawOval(bx[i] - spread, by[i] - height * 1.6f, bx[i] + spread, by[i] + height * .4f, paint);
        }
        paint.setAlpha(255);
    }

    /** Small deterministic LCG: no Random object, reproducible review frames. */
    private float random() {
        seed = seed * 1103515245 + 12345;
        return ((seed >>> 8) & 0xFFFF) / 65536f;
    }
}
