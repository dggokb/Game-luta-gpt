package com.gamelutagpt;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * Riscos de mangá dos golpes fortes (H, especiais, Super e Ultra), como no Hajime no Ippo:
 * traços pretos curtos que saem do contorno de quem apanhou, longos e cheios nas costas (por
 * onde a força do golpe sai) e poucos e curtos no resto do corpo; e traços paralelos ao golpe
 * atrás do ponto de contato, o rastro do braço ou da perna que passou. Pretos, sem contorno.
 * Duram {@link #FRAMES} quadros, trocam a cada 2 (tremem) e somem nos últimos. Só apresentação.
 */
final class RiscosDeImpacto {
    static final int FRAMES = 16;
    private static final int SLOTS = 4, BACK = 38, AROUND = 14, STREAKS = 9, FADE = 5;

    private final boolean[] used = new boolean[SLOTS];
    private final int[] start = new int[SLOTS], defender = new int[SLOTS], direction = new int[SLOTS];
    private final float[] impactX = new float[SLOTS], impactY = new float[SLOTS], scale = new float[SLOTS];
    private int next;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path wedge = new Path();

    RiscosDeImpacto() {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF000000);
    }

    /**
     * Um golpe forte acertou {@code defender} (0 ou 1) no ponto (x, y), indo para o lado de
     * {@code dir}; {@code size}: 1 no H, maior nos especiais, Super e Ultra.
     */
    void spawn(int frame, int defenderIndex, float x, float y, int dir, float size) {
        int i = next;
        next = (next + 1) % SLOTS;
        used[i] = true;
        start[i] = frame;
        defender[i] = defenderIndex;
        impactX[i] = x;
        impactY[i] = y;
        direction[i] = dir >= 0 ? 1 : -1;
        scale[i] = size;
    }

    void clear() {
        java.util.Arrays.fill(used, false);
    }

    /**
     * Desenha no mundo, por cima dos lutadores. {@code fighters} e {@code offsets}: os dois
     * lutadores e o deslocamento do desenho de cada um; {@code visible}: quem aparece (o Ultra
     * da p16 esconde o rival debaixo da cartola).
     */
    void draw(Canvas c, int frame, CombatFighter[] fighters, float[] offsets, boolean[] visible) {
        for (int i = 0; i < SLOTS; i++) {
            if (!used[i]) continue;
            int age = frame - start[i];
            if (age < 0 || age >= FRAMES) {
                used[i] = false;
                continue;
            }
            int alpha = Math.round(255f * Math.min(1f, (FRAMES - age) / (float)FADE));
            paint.setAlpha(alpha);
            int seed = start[i] * 31 + age / 2;
            drawStreaks(c, i, seed, age);
            int d = defender[i];
            if (visible[d]) drawBody(c, i, seed, fighters[d], offsets[d]);
        }
        paint.setAlpha(255);
    }

    /** Traços que saem do contorno do corpo: a elipse do tronco, cheia nas costas. */
    private void drawBody(Canvas c, int i, int seed, CombatFighter f, float offset) {
        float h = f.body().height(f.crouchingBody());
        // O tronco (do quadril à cabeça): os riscos não descem pelas pernas.
        float cx = f.x + offset, cy = f.y - h * 0.62f;
        float rx = f.body().halfWidth * 1.1f + 8f, ry = h * 0.38f;
        float s = scale[i];
        double back = direction[i] > 0 ? 0.0 : Math.PI;
        for (int k = 0; k < BACK + AROUND; k++) {
            boolean behind = k < BACK;
            double theta = behind
                ? back + (noise(seed, k, 1) - 0.5f) * 2.2f
                : noise(seed, k, 1) * Math.PI * 2.0;
            float cos = (float)Math.cos(theta), sin = (float)Math.sin(theta);
            // Normal da elipse naquele ponto: o traço sai reto do contorno.
            float nx = cos / rx, ny = sin / ry, n = (float)Math.sqrt(nx * nx + ny * ny);
            nx /= n;
            ny /= n;
            float gap = 3f + 8f * noise(seed, k, 2);
            float len = (behind ? 18f + 38f * noise(seed, k, 3) : 9f + 16f * noise(seed, k, 3)) * s;
            float width = (behind ? 2.2f + 2.4f * noise(seed, k, 4) : 1.6f + 1.4f * noise(seed, k, 4)) * s;
            float x0 = cx + rx * cos + nx * gap, y0 = cy + ry * sin + ny * gap;
            wedgeTo(c, x0, y0, x0 + nx * len, y0 + ny * len, width);
        }
    }

    /** Traços paralelos ao golpe, atrás do contato (o rastro do braço que passou). */
    private void drawStreaks(Canvas c, int i, int seed, int age) {
        if (age >= FRAMES - 3) return;
        float s = scale[i];
        int dir = direction[i];
        for (int k = 0; k < STREAKS; k++) {
            // Na faixa do braço (ou da perna) que bateu, do contato para trás.
            float y = impactY[i] + (noise(seed, k, 5) - 0.5f) * 56f * s;
            float x0 = impactX[i] - dir * (10f + 26f * noise(seed, k, 6));
            float len = (60f + 110f * noise(seed, k, 7)) * s;
            float width = (1.6f + 2.4f * noise(seed, k, 8)) * s;
            // Grosso perto do contato, afinando para trás.
            wedgeTo(c, x0, y, x0 - dir * len, y, width);
        }
    }

    /** Cunha preta de (x0, y0), com a largura {@code width}, até a ponta em (x1, y1). */
    private void wedgeTo(Canvas c, float x0, float y0, float x1, float y1, float width) {
        float dx = x1 - x0, dy = y1 - y0, l = (float)Math.sqrt(dx * dx + dy * dy);
        if (l < 0.5f) return;
        float px = -dy / l * width * 0.5f, py = dx / l * width * 0.5f;
        wedge.reset();
        wedge.moveTo(x0 + px, y0 + py);
        wedge.lineTo(x1, y1);
        wedge.lineTo(x0 - px, y0 - py);
        wedge.close();
        c.drawPath(wedge, paint);
    }

    /** Número fixo entre 0 e 1 (o mesmo desenho no mesmo quadro). */
    private static float noise(int seed, int k, int salt) {
        int h = seed * 374761393 + k * 668265263 + salt * 2147483629;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f;
    }
}
