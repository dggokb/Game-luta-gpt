package com.gamelutagpt.ultra;

import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;

/**
 * Abertura cinematográfica do Ultra (antes da investida, com a luta parada): faixas pretas,
 * o palco do Ultra toma a tela, o personagem entra recortado numa faixa diagonal com o nome
 * do golpe e o grito, e tudo sai num clarão de volta para a luta.
 *
 * Imagens do {@link UltraPack}: o corte ({@link UltraPack#cutIn}, virado para a direita) e o
 * palco ({@link UltraPack#stage}); sem elas, o palco é desenhado com a cor do ultra e a faixa
 * vem sem o personagem. Não depende de plataforma nem é thread-safe.
 */
public final class AberturaUltra {
    public static final float VW = 1280f;
    public static final float VH = 720f;

    /** Duração total (s); a luta fica parada esse tempo. */
    public static final float DURATION = 1.0f;
    static final float FLASH_IN = 0.10f;
    static final float SLIDE_IN = 0.22f;
    static final float NAME_AT = 0.16f;
    static final float NAME_SLAM = 0.10f;
    static final float SHOUT_AT = 0.42f;
    static final float EXIT_AT = 0.84f;
    /** Altura das faixas pretas de cinema, em cima e embaixo. */
    public static final float BAR = 72f;

    private static final int WHITE = 0xFFFFFFFF;
    private static final int INK = 0xFF0B0E16;

    private UltraPack pack;
    private int facing = 1;
    private float time = -1f;
    private final float[] band = new float[8];
    private final float[] stripe = new float[8];

    /** @param facing 1 se o atacante olha para a direita, -1 para a esquerda */
    public void start(UltraPack pack, int facing) {
        this.pack = pack;
        this.facing = facing >= 0 ? 1 : -1;
        time = 0f;
    }

    public boolean isActive() {
        return time >= 0f && time < DURATION;
    }

    public void update(float dt) {
        if (time < 0f) return;
        time += dt;
        if (time >= DURATION) time = -1f;
    }

    /** 0..1 dentro da abertura (testes e sons). */
    public float progress() {
        return time < 0f ? 1f : Math.min(1f, time / DURATION);
    }

    public void render(RenderCanvas c) {
        if (!isActive() || pack == null) return;
        UltraDefinition d = pack.definition;
        float t = time;

        drawStage(c, d, t);

        // Entrada e saída: tudo desliza do lado de onde o personagem olha.
        float in = easeOut(clamp01(t / SLIDE_IN));
        float out = t > EXIT_AT ? easeIn(clamp01((t - EXIT_AT) / (DURATION - EXIT_AT))) : 0f;
        float slide = (1f - in) * -1500f * facing + out * 1700f * facing;

        drawBand(c, d, slide);
        drawCutIn(c, slide, t);
        drawName(c, d, slide, t);

        // Faixas pretas de cinema (ficam também depois, na luta, enquanto o Ultra roda).
        c.fillRect(-40f, -40f, VW + 40f, BAR, INK);
        c.fillRect(-40f, VH - BAR, VW + 40f, VH + 40f, INK);

        // Clarão de entrada e o branco que corta de volta para a luta.
        if (t < FLASH_IN) c.fillRect(0f, 0f, VW, VH, withAlpha(WHITE, (int)(255 * (1f - t / FLASH_IN))));
        if (out > 0f) c.fillRect(0f, 0f, VW, VH, withAlpha(WHITE, (int)(230 * out)));
    }

    private void drawStage(RenderCanvas c, UltraDefinition d, float t) {
        if (pack.stage != null) {
            // Palco do Ultra com um zoom lento (a câmera "respira").
            float k = 1f + 0.06f * t / DURATION;
            float w = VW * k, h = VH * k;
            float imgRatio = pack.stage.width() / (float)Math.max(1, pack.stage.height());
            if (w / h > imgRatio) h = w / imgRatio; else w = h * imgRatio;
            c.drawImage(pack.stage, (VW - w) * 0.5f, (VH - h) * 0.5f, (VW + w) * 0.5f, (VH + h) * 0.5f, 255);
            c.fillRect(0f, 0f, VW, VH, 0x55000000);
        } else {
            c.fillVerticalGradient(0f, 0f, VW, VH, 0xFF05060C, darken(d.color, 0.35f));
            c.fillRadialGlow(VW * 0.5f, VH * 0.55f, 760f, withAlpha(d.color, 170));
        }
        // Linhas de velocidade saindo do centro, girando devagar.
        float cx = VW * 0.5f, cy = VH * 0.55f;
        for (int i = 0; i < 46; i++) {
            double a = i * Math.PI * 2 / 46 + t * 0.35 + (i % 3) * 0.05;
            float r0 = 260f + (i * 53 % 140), r1 = 1400f;
            float w = 2f + (i * 7 % 5);
            c.drawLine(cx + (float)Math.cos(a) * r0, cy + (float)Math.sin(a) * r0,
                cx + (float)Math.cos(a) * r1, cy + (float)Math.sin(a) * r1, w, withAlpha(WHITE, 46));
        }
    }

    private void drawBand(RenderCanvas c, UltraDefinition d, float slide) {
        // Faixa diagonal com bordas brancas, inclinada a favor do golpe.
        float tilt = 34f * facing;
        float top = 300f, bottom = 500f;
        setQuad(band, -200f + slide, top + tilt, VW + 200f + slide, top - tilt,
            VW + 200f + slide, bottom - tilt, -200f + slide, bottom + tilt);
        c.fillPolygon(band, withAlpha(d.color, 236));
        setQuad(stripe, -200f + slide, top + tilt - 14f, VW + 200f + slide, top - tilt - 14f,
            VW + 200f + slide, top - tilt, -200f + slide, top + tilt);
        c.fillPolygon(stripe, WHITE);
        setQuad(stripe, -200f + slide, bottom + tilt, VW + 200f + slide, bottom - tilt,
            VW + 200f + slide, bottom - tilt + 14f, -200f + slide, bottom + tilt + 14f);
        c.fillPolygon(stripe, WHITE);
    }

    private void drawCutIn(RenderCanvas c, float slide, float t) {
        RenderImage art = pack.cutIn;
        if (art == null) return;
        // O personagem fica do lado de trás e olha para o nome; cresce devagar enquanto segura.
        float h = 640f * (1f + 0.04f * t / DURATION);
        float w = h * art.width() / (float)Math.max(1, art.height());
        float centerX = (facing > 0 ? VW * 0.30f : VW * 0.70f) + slide * 1.15f;
        float bottom = VH + 30f;
        c.save();
        if (facing < 0) c.scale(-1f, 1f, centerX, 0f);
        c.drawImage(art, centerX - w * 0.5f, bottom - h, centerX + w * 0.5f, bottom, 255);
        c.restore();
    }

    private void drawName(RenderCanvas c, UltraDefinition d, float slide, float t) {
        if (t < NAME_AT) return;
        float k = clamp01((t - NAME_AT) / NAME_SLAM);
        // Bate na tela: começa grande e encolhe até o tamanho final.
        float scale = 1f + 0.6f * (1f - easeOut(k));
        int alpha = (int)(255 * Math.min(1f, k * 2f));
        float cx = (facing > 0 ? VW * 0.68f : VW * 0.32f) + slide * 0.85f;
        float y = 418f;
        float size = fit(c, d.name, 88f, 620f);
        c.save();
        c.scale(scale, scale, cx, y - size * 0.35f);
        c.drawText("ULTRA", cx, y - size - 6f, 34f, withAlpha(d.accentColor, alpha), withAlpha(INK, alpha), 5f, true);
        c.drawText(d.name, cx, y, size, withAlpha(WHITE, alpha), withAlpha(INK, alpha), size * 0.12f, true);
        c.restore();
        String shout = d.opening.shout;
        if (shout != null && t >= SHOUT_AT) {
            float s = clamp01((t - SHOUT_AT) / 0.08f);
            c.drawText(shout, cx, y + 62f, fit(c, shout, 34f, 600f),
                withAlpha(WHITE, (int)(255 * s)), withAlpha(INK, (int)(255 * s)), 5f, true);
        }
    }

    private static float fit(RenderCanvas c, String text, float size, float maxWidth) {
        float w = c.measureText(text, size, true);
        return w > maxWidth ? size * maxWidth / w : size;
    }

    private static void setQuad(float[] q, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3) {
        q[0] = x0; q[1] = y0; q[2] = x1; q[3] = y1; q[4] = x2; q[5] = y2; q[6] = x3; q[7] = y3;
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : v > 1f ? 1f : v;
    }

    private static float easeOut(float k) {
        float u = 1f - k;
        return 1f - u * u * u;
    }

    private static float easeIn(float k) {
        return k * k * k;
    }

    static int withAlpha(int argb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0x00FFFFFF);
    }

    private static int darken(int argb, float k) {
        int r = (int)(((argb >> 16) & 0xFF) * k), g = (int)(((argb >> 8) & 0xFF) * k), b = (int)((argb & 0xFF) * k);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
