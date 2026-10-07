package com.gamelutagpt.ultra;

import com.gamelutagpt.render.Rand;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;

/**
 * Desenha o raio final do ultra no mundo da luta: a esfera nas mãos, o feixe correndo até o
 * alvo, a explosão de cada acerto e a explosão final. Só desenha; quem decide o tempo dos
 * acertos e o dano é o motor de combate. Coordenadas do mundo (o jogo já aplicou a câmera).
 */
public final class RaioFinal {
    /** Velocidade com que a textura do feixe corre para a frente (px do mundo por segundo). */
    static final float SCROLL_SPEED = 1500f;
    /** Duração da explosão final. */
    public static final float BLAST_DURATION = 0.7f;

    /** O que o jogo informa a cada quadro. */
    public static final class Frame {
        /** Ponto de saída (as mãos) e x do alvo. */
        public float originX, originY, targetX;
        public int facing = 1;
        /** Segundos desde o disparo (anima a textura e o brilho). */
        public float time;
        /** 0 a 1: quanto do caminho até o alvo o feixe já percorreu. */
        public float reach;
        /** Segundos desde o último acerto pequeno (pulso no alvo), ou negativo antes do primeiro. */
        public float sinceHit = -1f;
        /** Acertos já dados (varia o desenho de cada impacto). */
        public int hits;
        /** Segundos desde a explosão final, ou negativo antes dela. */
        public float sinceBlast = -1f;
        /** 1 com o feixe cheio; cai até 0 enquanto ele some depois da explosão. */
        public float strength = 1f;
    }

    private final Rand rand = new Rand(7);

    public void draw(RenderCanvas c, UltraPack pack, Frame f) {
        UltraDefinition def = pack.definition;
        float strength = clamp(f.strength, 0f, 1f);
        if (strength <= 0f && f.sinceBlast < 0f) return;

        c.save();
        // Desenha sempre indo para a direita; virado para a esquerda, espelha em volta das mãos.
        if (f.facing < 0) c.scale(-1f, 1f, f.originX, 0f);
        float distance = Math.abs(f.targetX - f.originX) * clamp(f.reach, 0f, 1f);
        float tipX = f.originX + distance;
        float pulse = 1f + 0.05f * (float)Math.sin(f.time * 52f) + 0.03f * (float)Math.sin(f.time * 23f);
        float thickness = def.beam.thickness * pulse * (0.35f + 0.65f * strength);
        if (f.sinceBlast >= 0f && f.sinceBlast < 0.18f) thickness *= 1f + 0.3f * (1f - f.sinceBlast / 0.18f);
        int alpha = Math.round(255f * strength);

        if (strength > 0f && distance > 1f) {
            drawBody(c, pack, f, tipX, thickness, alpha);
        }
        if (strength > 0f) drawStart(c, pack, f, thickness, alpha);
        if (f.reach >= 1f && f.sinceBlast < 0f) drawHitImpact(c, pack, f, tipX, thickness);
        if (strength > 0f) drawSparks(c, def, f, tipX, thickness, alpha);
        if (f.sinceBlast >= 0f && f.sinceBlast < BLAST_DURATION) drawBlast(c, pack, f, tipX, thickness);
        c.restore();
    }

    // ------------------------------------------------------------ feixe

    private void drawBody(RenderCanvas c, UltraPack pack, Frame f, float tipX, float thickness, int alpha) {
        RenderImage body = pack.beamBody;
        RenderImage tip = pack.beamTip;
        float top = f.originY - thickness * 0.5f;
        float bottom = f.originY + thickness * 0.5f;
        if (body == null) {
            drawPlainBody(c, pack.definition, f.originX, tipX, f.originY, thickness, alpha);
            return;
        }
        float scale = thickness / body.height();
        float end = tipX;
        if (tip != null) {
            // A ponta tem o próprio pedaço de feixe: o corpo vai até o meio dela.
            float tipW = tip.width() * scale;
            float tipH = tip.height() * scale;
            float tipRight = tipX + tipW * 0.10f;
            float tipLeft = tipRight - tipW;
            end = Math.max(f.originX, tipLeft + tipW * 0.45f);
            float srcLeft = Math.max(0f, (f.originX - tipLeft) / scale);
            if (srcLeft < tip.width() - 1f) {
                drawTiles(c, body, scale, f, end, top, bottom, alpha);
                c.drawImageRegion(tip, srcLeft, 0f, tip.width(), tip.height(),
                    tipLeft + srcLeft * scale, f.originY - tipH * 0.5f, tipRight, f.originY + tipH * 0.5f, alpha);
                return;
            }
        }
        drawTiles(c, body, scale, f, end, top, bottom, alpha);
    }

    /** Corpo repetido de {@code originX} até {@code end}, com a textura correndo para a frente. */
    private static void drawTiles(RenderCanvas c, RenderImage body, float scale, Frame f, float end,
                                  float top, float bottom, int alpha) {
        float tileW = body.width() * scale;
        if (end <= f.originX || tileW <= 0f) return;
        float offset = (f.time * SCROLL_SPEED) % tileW;
        for (float x = f.originX + offset - tileW; x < end; x += tileW) {
            float left = Math.max(x, f.originX);
            float right = Math.min(x + tileW, end);
            if (right <= left) continue;
            c.drawImageRegion(body, (left - x) / scale, 0f, (right - x) / scale, body.height(),
                left, top, right, bottom, alpha);
        }
    }

    private static void drawPlainBody(RenderCanvas c, UltraDefinition def, float from, float to, float y,
                                      float thickness, int alpha) {
        int outer = withAlpha(def.color, alpha * 3 / 4);
        int clear = withAlpha(def.color, 0);
        float half = thickness * 0.38f;
        c.fillVerticalGradient(from, y - half, to, y, clear, outer);
        c.fillVerticalGradient(from, y, to, y + half, outer, clear);
        float core = thickness * 0.11f;
        c.fillRect(from, y - core, to, y + core, withAlpha(def.accentColor, alpha));
        c.fillRect(from, y - core * 0.5f, to, y + core * 0.5f, withAlpha(0xFFFFFFFF, alpha));
        c.fillRadialGlow(to, y, thickness * 0.6f, withAlpha(def.accentColor, alpha));
    }

    // ------------------------------------------------------------ mãos, alvo, explosão

    private static void drawStart(RenderCanvas c, UltraPack pack, Frame f, float thickness, int alpha) {
        RenderImage start = pack.beamStart;
        float size = thickness * 1.9f * (1f + 0.07f * (float)Math.sin(f.time * 31f));
        if (start == null) {
            c.fillRadialGlow(f.originX, f.originY, size * 0.6f, withAlpha(pack.definition.color, alpha));
            c.fillRadialGlow(f.originX, f.originY, size * 0.25f, withAlpha(0xFFFFFFFF, alpha));
            return;
        }
        float h = size;
        float w = h * start.width() / start.height();
        float[] center = pack.definition.beam.startCenter;
        float left = f.originX - w * center[0];
        float top = f.originY - h * center[1];
        c.drawImage(start, left, top, left + w, top + h, alpha);
    }

    private void drawHitImpact(RenderCanvas c, UltraPack pack, Frame f, float tipX, float thickness) {
        float kick = f.sinceHit >= 0f ? Math.max(0f, 1f - f.sinceHit / 0.08f) : 0f;
        float size = thickness * (1.05f + 0.45f * kick);
        drawExplosion(c, pack, tipX, f.originY, size, 170 + Math.round(85f * kick), (f.hits * 47) % 360);
    }

    private void drawBlast(RenderCanvas c, UltraPack pack, Frame f, float tipX, float thickness) {
        float t = f.sinceBlast / BLAST_DURATION;
        float grow = 1f - (1f - Math.min(1f, t * 2.4f)) * (1f - Math.min(1f, t * 2.4f));
        float size = thickness * (1.6f + 2.6f * grow);
        int alpha = Math.round(255f * (1f - t * t));
        c.fillRadialGlow(tipX, f.originY, size * 0.9f, withAlpha(0xFFFFFFFF, Math.round(alpha * (1f - t))));
        drawExplosion(c, pack, tipX, f.originY, size, alpha, 15f + 40f * t);
    }

    private static void drawExplosion(RenderCanvas c, UltraPack pack, float x, float y, float size, int alpha,
                                      float rotation) {
        RenderImage impact = pack.beamImpact;
        if (impact == null) {
            c.fillRadialGlow(x, y, size * 0.55f, withAlpha(pack.definition.color, alpha));
            c.fillRadialGlow(x, y, size * 0.28f, withAlpha(0xFFFFFFFF, alpha));
            return;
        }
        c.save();
        c.rotate(rotation, x, y);
        float half = size * 0.5f;
        c.drawImage(impact, x - half, y - half, x + half, y + half, alpha);
        c.restore();
    }

    /** Faíscas que escapam do feixe perto do alvo e das mãos. */
    private void drawSparks(RenderCanvas c, UltraDefinition def, Frame f, float tipX, float thickness, int alpha) {
        rand.reset((long)(f.time * 30f));
        int color = withAlpha(def.accentColor, alpha);
        for (int i = 0; i < 14; i++) {
            boolean nearTip = i < 9;
            float x = nearTip ? tipX - rand.range(0f, thickness * 1.2f) : f.originX + rand.range(0f, thickness * 2f);
            float side = rand.next() < 0.5f ? -1f : 1f;
            float y = f.originY + side * rand.range(thickness * 0.15f, thickness * 0.55f);
            float len = rand.range(10f, 34f);
            float dy = side * rand.range(2f, 14f);
            c.drawLine(x, y, x - len, y + dy, rand.range(2f, 4f), color);
        }
    }

    private static int withAlpha(int argb, int alpha) {
        int a = Math.max(0, Math.min(255, alpha)) * ((argb >>> 24) & 0xFF) / 255;
        return (a << 24) | (argb & 0xFFFFFF);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
