package com.gamelutagpt.ultra;

/**
 * Arte desenhada por código para cada painel. Serve para testar o ritmo
 * da cinemática antes de existir imagem, e também desenha os efeitos que
 * vão por cima das imagens geradas (partículas, linhas de velocidade,
 * estouros), para a ilustração parada parecer viva.
 *
 * Todas as funções desenham dentro da caixa (l, t, r, b) do painel.
 */
final class ArteProvisoria {
    static final int BLACK = 0xFF000000;
    static final int WHITE = 0xFFFFFFFF;
    static final int INK = 0xFF111114;

    private final Rand rand = new Rand(1);
    /**
     * Versão leve, sem linhas de velocidade: usada nos cacos da página, que
     * redesenham o painel final dezenas de vezes no mesmo quadro.
     */
    boolean lite;

    // ---------------------------------------------------------------- arte

    void drawContent(
        UltraCanvas c,
        UltraSlot slot,
        float l, float t, float r, float b,
        float age,
        float fx,
        int color,
        int accent,
        long seed
    ) {
        rand.reset(seed);
        switch (slot) {
            case OLHOS: drawOlhos(c, l, t, r, b, age, color, accent); break;
            case CARGA: drawCarga(c, l, t, r, b, age, color, accent); break;
            case GOLPE: drawGolpe(c, l, t, r, b, age, fx, color, accent); break;
            case ATINGIDO: drawAtingido(c, l, t, r, b, age, color); break;
            case FINAL: drawFinal(c, l, t, r, b, age, fx, color, accent); break;
        }
    }

    /** Efeitos por cima do conteúdo (imagem ou provisório). */
    void drawOverlay(
        UltraCanvas c,
        UltraSlot slot,
        float l, float t, float r, float b,
        float age,
        float fx,
        int color,
        int accent,
        long seed
    ) {
        rand.reset(seed * 31 + 7);
        float cx = (l + r) * 0.5f;
        float cy = (t + b) * 0.5f;
        switch (slot) {
            case OLHOS:
                // Brilho que corta o painel quando ele chega.
                if (age < 0.35f) {
                    float p = age / 0.35f;
                    float x = l + (r - l) * p;
                    c.drawLine(x - 160f, b, x + 40f, t, 26f, withAlpha(WHITE, (int)(150 * (1f - p))));
                }
                break;
            case CARGA:
                risingParticles(c, l, t, r, b, age, accent, 26);
                break;
            case GOLPE:
                if (fx >= 0f) impactBurst(c, cx, cy, fx, (b - t) * 0.9f, accent);
                break;
            case ATINGIDO:
                horizontalSpeedLines(c, l, t, r, b, age, withAlpha(INK, 70), 14);
                break;
            case FINAL:
                if (!lite) radialLines(c, cx, cy, Math.max(r - l, b - t), 36, withAlpha(WHITE, 90), 0.18f);
                break;
        }
    }

    private void drawOlhos(UltraCanvas c, float l, float t, float r, float b, float age, int color, int accent) {
        c.fillRect(l, t, r, b, 0xFF11131C);
        horizontalSpeedLines(c, l, t, r, b, age, withAlpha(color, 60), 18);

        float cx = (l + r) * 0.5f;
        float cy = (t + b) * 0.5f + 8f;
        float k = (b - t) / 220f;
        drawEye(c, cx - 250f * k, cy, k, -1, color, accent, age);
        drawEye(c, cx + 250f * k, cy, k, 1, color, accent, age);
    }

    private void drawEye(UltraCanvas c, float ex, float ey, float k, int side, int color, int accent, float age) {
        float s = side * k;
        float[] eye = {
            ex - 150f * s, ey + 8f * k,
            ex - 60f * s, ey - 38f * k,
            ex + 90f * s, ey - 34f * k,
            ex + 155f * s, ey - 4f * k,
            ex + 60f * s, ey + 30f * k,
            ex - 80f * s, ey + 26f * k
        };
        // Brilho da íris aparece aos poucos.
        float glow = clamp01(age / 0.5f);
        c.fillCircle(ex, ey - 4f * k, 70f * k, withAlpha(accent, (int)(70 * glow)));
        c.fillPolygon(eye, 0xFFF4F1EA);
        c.save();
        c.clipPolygon(eye);
        c.fillCircle(ex, ey - 4f * k, 36f * k, color);
        c.fillCircle(ex, ey - 4f * k, 24f * k, mix(color, BLACK, 0.45f));
        c.fillCircle(ex, ey - 4f * k, 13f * k, BLACK);
        c.fillCircle(ex + 10f * s, ey - 16f * k, 7f * k, WHITE);
        c.restore();
        c.strokePolygon(eye, 7f * k, BLACK);
        // Pálpebra superior grossa e sobrancelha brava.
        c.drawLine(ex - 150f * s, ey + 8f * k, ex - 60f * s, ey - 40f * k, 12f * k, BLACK);
        c.drawLine(ex - 60f * s, ey - 40f * k, ex + 90f * s, ey - 36f * k, 12f * k, BLACK);
        c.drawLine(ex - 175f * s, ey - 46f * k, ex + 130f * s, ey - 88f * k, 20f * k, BLACK);
    }

    private void drawCarga(UltraCanvas c, float l, float t, float r, float b, float age, int color, int accent) {
        c.fillRect(l, t, r, b, mix(color, BLACK, 0.78f));
        float cx = (l + r) * 0.5f;
        float k = (b - t) / 436f;
        float cy = (t + b) * 0.5f + 40f * k;

        // Aura em labaredas, pulsando.
        float pulse = 1f + 0.06f * (float)Math.sin(age * 38f);
        spikyStar(c, cx, cy - 40f * k, 220f * k * pulse, 150f * k * pulse, 18, age * 0.6f, withAlpha(color, 210));
        spikyStar(c, cx, cy - 40f * k, 165f * k * pulse, 110f * k * pulse, 14, -age * 0.9f, withAlpha(accent, 230));

        // Pedras subindo.
        for (int i = 0; i < 7; i++) {
            float px = l + (r - l) * rand.next();
            float speed = rand.range(60f, 140f);
            float py = b - ((rand.next() * (b - t)) + age * speed) % (b - t);
            float size = rand.range(6f, 14f) * k;
            c.fillPolygon(new float[]{
                px - size, py, px, py - size * 0.8f, px + size, py + size * 0.2f, px + size * 0.2f, py + size
            }, INK);
        }

        humanoidPower(c, cx, cy, k);
    }

    private void humanoidPower(UltraCanvas c, float x, float y, float k) {
        float w = 22f * k;
        // Pernas abertas.
        limb(c, x, y - 20f * k, x - 42f * k, y + 50f * k, x - 64f * k, y + 122f * k, w * 1.15f);
        limb(c, x, y - 20f * k, x + 42f * k, y + 50f * k, x + 64f * k, y + 122f * k, w * 1.15f);
        // Tronco.
        c.drawLine(x, y - 120f * k, x, y - 22f * k, 40f * k, INK);
        c.drawLine(x - 40f * k, y - 106f * k, x + 40f * k, y - 106f * k, 26f * k, INK);
        // Braços dobrados, punhos fechados para baixo.
        limb(c, x - 40f * k, y - 106f * k, x - 84f * k, y - 60f * k, x - 62f * k, y - 14f * k, w);
        limb(c, x + 40f * k, y - 106f * k, x + 84f * k, y - 60f * k, x + 62f * k, y - 14f * k, w);
        c.fillCircle(x - 62f * k, y - 12f * k, 17f * k, INK);
        c.fillCircle(x + 62f * k, y - 12f * k, 17f * k, INK);
        // Cabeça com olhos brilhando.
        c.fillCircle(x, y - 152f * k, 28f * k, INK);
        c.drawLine(x - 15f * k, y - 156f * k, x - 4f * k, y - 152f * k, 4f * k, WHITE);
        c.drawLine(x + 4f * k, y - 152f * k, x + 15f * k, y - 156f * k, 4f * k, WHITE);
    }

    private void drawGolpe(UltraCanvas c, float l, float t, float r, float b, float age, float fx, int color, int accent) {
        c.fillRect(l, t, r, b, mix(color, WHITE, 0.25f));
        float cx = (l + r) * 0.5f;
        float cy = (t + b) * 0.5f;
        float k = (b - t) / 230f;
        radialLines(c, cx, cy, Math.max(r - l, b - t), 44, withAlpha(INK, 200), 0.32f);

        // Punho vindo na direção da câmera: cresce até o impacto.
        float approach = fx >= 0f ? 1f : clamp01(age / 0.10f);
        float fist = (60f + 40f * approach) * k;
        c.drawLine(cx - 520f * k, cy + 200f * k, cx - 20f * k, cy + 10f * k, 120f * k, INK);
        c.fillCircle(cx, cy, fist, INK);
        for (int i = 0; i < 4; i++) {
            float kx = cx - fist * 0.75f + i * fist * 0.5f;
            c.strokeCircle(kx, cy - fist * 0.55f, fist * 0.28f, 4f * k, 0xFF55555C);
        }
        c.drawLine(cx - fist * 0.8f, cy + fist * 0.15f, cx + fist * 0.7f, cy + fist * 0.05f, 4f * k, 0xFF55555C);
    }

    private void drawAtingido(UltraCanvas c, float l, float t, float r, float b, float age, int color) {
        c.fillRect(l, t, r, b, 0xFFE9E5DA);
        float cx = (l + r) * 0.5f;
        float cy = (t + b) * 0.5f;
        float k = (b - t) / 220f;
        horizontalSpeedLines(c, l, t, r, b, age, withAlpha(INK, 150), 26);

        // Oponente dobrado para trás, arremessado para a direita.
        float drift = age * 60f * k;
        float x = cx + 40f * k + drift;
        float y = cy + 10f * k;
        int body = 0xFF2A3446;
        c.drawLine(x - 70f * k, y + 10f * k, x + 30f * k, y - 30f * k, 40f * k, body);
        c.fillCircle(x + 64f * k, y - 52f * k, 27f * k, body);
        limb(c, x + 20f * k, y - 26f * k, x + 60f * k, y + 30f * k, x + 110f * k, y + 50f * k, 20f * k, body);
        limb(c, x + 10f * k, y - 30f * k, x + 40f * k, y - 90f * k, x + 100f * k, y - 110f * k, 20f * k, body);
        limb(c, x - 70f * k, y + 10f * k, x - 140f * k, y + 40f * k, x - 220f * k, y + 30f * k, 24f * k, body);
        limb(c, x - 70f * k, y + 10f * k, x - 150f * k, y - 20f * k, x - 230f * k, y - 10f * k, 24f * k, body);

        // Ponto de contato.
        float[] contact = spikyStar(c, x - 20f * k, y - 8f * k, 70f * k, 26f * k, 12, 0f, WHITE);
        c.strokePolygon(contact, 5f * k, INK);
        for (int i = 0; i < 6; i++) {
            float a = rand.range(-0.8f, 0.8f);
            float d = rand.range(70f, 160f) * k + age * 200f * k;
            c.fillCircle(x + (float)Math.cos(a) * d, y - 8f * k + (float)Math.sin(a) * d, rand.range(3f, 7f) * k, withAlpha(color, 230));
        }
    }

    private void drawFinal(UltraCanvas c, float l, float t, float r, float b, float age, float burst, int color, int accent) {
        c.fillRect(l, t, r, b, mix(color, BLACK, 0.86f));
        float h = b - t;
        float k = h / 720f;
        float cy = t + h * 0.52f;
        float originX = l + (r - l) * 0.20f;
        if (!lite) radialLines(c, originX + 300f * k, cy, Math.max(r - l, h), 40, withAlpha(color, 120), 0.25f);

        // Raio crescendo e ondulando.
        float grow = clamp01(age / 0.35f);
        float wobble = (float)Math.sin(age * 50f) * 6f * k;
        float outer = (95f + 25f * grow) * k + wobble;
        float inner = (60f + 15f * grow) * k + wobble * 0.5f;
        float core = 28f * k;
        float endX = originX + (r - originX + 200f) * grow;
        beam(c, originX + 70f * k, endX, cy, outer, withAlpha(color, 235));
        beam(c, originX + 70f * k, endX, cy, inner, accent);
        beam(c, originX + 70f * k, endX, cy, core, WHITE);
        c.fillCircle(originX + 80f * k, cy, outer * 1.15f, withAlpha(accent, 220));
        c.fillCircle(originX + 80f * k, cy, inner * 1.05f, WHITE);

        // Oponente engolido pelo raio.
        float tx = l + (r - l) * 0.74f;
        if (grow >= 1f) {
            c.fillCircle(tx, cy - 40f * k, 24f * k, withAlpha(INK, 200));
            c.drawLine(tx, cy - 20f * k, tx + 20f * k, cy + 60f * k, 34f * k, withAlpha(INK, 200));
        }

        // Atacante empurrando o raio com os dois braços.
        float x = originX - 40f * k;
        float y = cy + 90f * k;
        limb(c, x, y - 30f * k, x - 50f * k, y + 60f * k, x - 90f * k, y + 140f * k, 26f * k);
        limb(c, x, y - 30f * k, x + 20f * k, y + 70f * k, x + 10f * k, y + 150f * k, 26f * k);
        c.drawLine(x - 10f * k, y - 150f * k, x, y - 30f * k, 46f * k, INK);
        limb(c, x - 5f * k, y - 130f * k, x + 60f * k, y - 110f * k, x + 110f * k, y - 92f * k, 24f * k);
        limb(c, x - 5f * k, y - 120f * k, x + 55f * k, y - 92f * k, x + 112f * k, y - 82f * k, 24f * k);
        c.fillCircle(x - 6f * k, y - 182f * k, 32f * k, INK);

        // Explosão no ponto de impacto depois da detonação.
        if (burst >= 0f) {
            float e = easeOutCubic(clamp01(burst / 0.25f));
            spikyStar(c, tx, cy, (240f + 900f * e) * k, (140f + 600f * e) * k, 16, burst * 2f, withAlpha(accent, 240));
            spikyStar(c, tx, cy, (160f + 700f * e) * k, (90f + 480f * e) * k, 16, -burst * 3f, WHITE);
        }
    }

    // ------------------------------------------------------------ primitivas

    private void limb(UltraCanvas c, float x1, float y1, float x2, float y2, float x3, float y3, float width) {
        limb(c, x1, y1, x2, y2, x3, y3, width, INK);
    }

    private void limb(UltraCanvas c, float x1, float y1, float x2, float y2, float x3, float y3, float width, int color) {
        c.drawLine(x1, y1, x2, y2, width, color);
        c.drawLine(x2, y2, x3, y3, width * 0.9f, color);
    }

    private void beam(UltraCanvas c, float x1, float x2, float cy, float halfHeight, int color) {
        c.fillRect(x1, cy - halfHeight, x2, cy + halfHeight, color);
    }

    void radialLines(UltraCanvas c, float cx, float cy, float reach, int count, int color, float innerFraction) {
        for (int i = 0; i < count; i++) {
            float a = (float)(Math.PI * 2 * (i + rand.next() * 0.8f) / count);
            float inner = reach * (innerFraction + rand.next() * 0.12f);
            float cos = (float)Math.cos(a);
            float sin = (float)Math.sin(a);
            c.drawLine(cx + cos * inner, cy + sin * inner, cx + cos * reach, cy + sin * reach, rand.range(2f, 7f), color);
        }
    }

    private void horizontalSpeedLines(UltraCanvas c, float l, float t, float r, float b, float age, int color, int count) {
        float w = r - l;
        for (int i = 0; i < count; i++) {
            float y = t + (b - t) * rand.next();
            float len = rand.range(0.15f, 0.45f) * w;
            float speed = rand.range(900f, 1800f);
            float x = r - ((rand.next() * (w + len)) + age * speed) % (w + len);
            c.drawLine(x, y, x + len, y, rand.range(2f, 5f), color);
        }
    }

    private void risingParticles(UltraCanvas c, float l, float t, float r, float b, float age, int color, int count) {
        float h = b - t;
        for (int i = 0; i < count; i++) {
            float x = l + (r - l) * rand.next();
            float speed = rand.range(160f, 420f);
            float y = b - ((rand.next() * h) + age * speed) % h;
            float len = rand.range(10f, 28f);
            c.drawLine(x, y, x, y + len, rand.range(3f, 6f), withAlpha(color, 210));
        }
    }

    void impactBurst(UltraCanvas c, float cx, float cy, float age, float size, int accent) {
        if (age > 0.35f) return;
        float e = easeOutCubic(clamp01(age / 0.18f));
        float fade = 1f - clamp01((age - 0.15f) / 0.20f);
        spikyStar(c, cx, cy, size * (0.5f + 0.7f * e), size * (0.2f + 0.3f * e), 14, 0.3f, withAlpha(accent, (int)(240 * fade)));
        spikyStar(c, cx, cy, size * (0.3f + 0.5f * e), size * (0.1f + 0.2f * e), 14, 0.1f, withAlpha(WHITE, (int)(255 * fade)));
    }

    /** Estrela de pontas (labareda/estouro). Devolve o polígono desenhado. */
    float[] spikyStar(UltraCanvas c, float cx, float cy, float outer, float inner, int points, float spin, int color) {
        float[] poly = new float[points * 4];
        for (int i = 0; i < points * 2; i++) {
            float a = spin + (float)(Math.PI * i / points);
            float radius = (i % 2 == 0) ? outer * rand.range(0.8f, 1.1f) : inner;
            poly[i * 2] = cx + (float)Math.cos(a) * radius;
            poly[i * 2 + 1] = cy + (float)Math.sin(a) * radius;
        }
        c.fillPolygon(poly, color);
        return poly;
    }

    // ---------------------------------------------------------------- cores

    static int withAlpha(int color, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        int base = (color >>> 24) & 0xFF;
        return ((a * base / 255) << 24) | (color & 0x00FFFFFF);
    }

    static int mix(int c1, int c2, float t) {
        int a = (int)(((c1 >>> 24) & 0xFF) + (((c2 >>> 24) & 0xFF) - ((c1 >>> 24) & 0xFF)) * t);
        int r = (int)(((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t);
        int g = (int)(((c1 >> 8) & 0xFF) + (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t);
        int b = (int)((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    static float easeOutCubic(float t) {
        float u = 1f - t;
        return 1f - u * u * u;
    }
}
