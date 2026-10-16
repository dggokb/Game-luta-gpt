package com.gamelutagpt.ultra;

import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;

/**
 * Abertura cinematográfica do Ultra (antes da investida, com a luta parada): faixas pretas,
 * o palco do Ultra toma a tela, o personagem entra recortado com o nome do golpe e o grito, e
 * tudo sai num clarão de volta para a luta. O estilo vem do ultra.json
 * ({@code "estilo": {"abertura": ...}}):
 *
 * <ul>
 *   <li>{@code faixa}: o personagem atravessa a tela numa faixa diagonal com o nome;</li>
 *   <li>{@code vertical}: uma coluna na cor do Ultra com o nome escrito de cima para baixo;</li>
 *   <li>{@code dividida}: a tela partida na diagonal, o personagem de um lado e o rival do
 *       outro, se encarando;</li>
 *   <li>{@code racha}: a tela preta racha ao meio, abre, e o personagem sobe pela luz.</li>
 * </ul>
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

    public static final String FAIXA = "faixa", VERTICAL = "vertical", DIVIDIDA = "dividida", RACHA = "racha";
    public static final String[] STYLES = {FAIXA, VERTICAL, DIVIDIDA, RACHA};

    private UltraPack pack;
    private RenderImage rival;
    private String style = FAIXA;
    private int facing = 1;
    private float time = -1f;
    private final float[] band = new float[8];
    private final float[] stripe = new float[8];
    private final float[] crack = new float[2 * (CRACK_POINTS + 2)];
    private final float[] half = new float[2 * (CRACK_POINTS + 2)];
    private static final int CRACK_POINTS = 9;

    /** @param facing 1 se o atacante olha para a direita, -1 para a esquerda */
    public void start(UltraPack pack, int facing) {
        start(pack, facing, null);
    }

    /**
     * @param rival arte do adversário (corte ou arte de seleção, virada para a direita) para a
     *              abertura dividida; null desenha só a sombra dele
     */
    public void start(UltraPack pack, int facing, RenderImage rival) {
        this.pack = pack;
        this.rival = rival;
        this.style = styleOf(pack.definition.style.opening);
        this.facing = facing >= 0 ? 1 : -1;
        time = 0f;
    }

    /** O estilo com esse nome; desconhecido (ou null) vira a faixa. */
    public static String styleOf(String name) {
        if (name != null) {
            String key = name.trim().toLowerCase(java.util.Locale.ROOT);
            for (String s : STYLES) if (s.equals(key)) return s;
        }
        return FAIXA;
    }

    public static boolean isKnownStyle(String name) {
        if (name == null) return true;
        String key = name.trim().toLowerCase(java.util.Locale.ROOT);
        for (String s : STYLES) if (s.equals(key)) return true;
        return false;
    }

    /** O estilo da abertura em andamento (testes). */
    public String style() {
        return style;
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

        // Entrada e saída: tudo desliza do lado de onde o personagem olha.
        float in = easeOut(clamp01(t / SLIDE_IN));
        float out = t > EXIT_AT ? easeIn(clamp01((t - EXIT_AT) / (DURATION - EXIT_AT))) : 0f;
        float slide = (1f - in) * -1500f * facing + out * 1700f * facing;

        if (VERTICAL.equals(style)) {
            renderVertical(c, d, t, in, out);
        } else if (DIVIDIDA.equals(style)) {
            renderSplit(c, d, t, in, out);
        } else if (RACHA.equals(style)) {
            renderCrack(c, d, t, out);
        } else {
            drawStage(c, d, t);
            drawBand(c, d, slide);
            drawCutIn(c, slide, t);
            drawName(c, d, slide, t);
        }

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
        // O personagem fica do lado de trás e olha para o nome; cresce devagar enquanto segura.
        float centerX = (facing > 0 ? VW * 0.30f : VW * 0.70f) + slide * 1.15f;
        drawFigure(c, pack.cutIn, centerX, VH + 30f, 640f * (1f + 0.04f * t / DURATION), facing, 255);
    }

    /** Uma figura (virada para a direita na imagem) com os pés em (centerX, bottom). */
    private static void drawFigure(RenderCanvas c, RenderImage art, float centerX, float bottom, float h,
                                   int facing, int alpha) {
        if (art == null) return;
        float w = h * art.width() / (float)Math.max(1, art.height());
        c.save();
        if (facing < 0) c.scale(-1f, 1f, centerX, 0f);
        c.drawImage(art, centerX - w * 0.5f, bottom - h, centerX + w * 0.5f, bottom, alpha);
        c.restore();
    }

    // ------------------------------------------------------------ vertical

    /**
     * Uma coluna na cor do Ultra desce do lado para onde o personagem olha, com o nome escrito
     * de cima para baixo (uma coluna por palavra); o personagem sobe do outro lado.
     */
    private void renderVertical(RenderCanvas c, UltraDefinition d, float t, float in, float out) {
        drawStage(c, d, t);
        String[] words = d.name.trim().split("\\s+");
        int longest = 1;
        for (String w : words) longest = Math.max(longest, w.length());
        float size = Math.min(78f, (VH - 2 * BAR - 90f) / longest);
        float columnW = size * 1.18f;
        float bandW = columnW * words.length + 70f;
        float bandX = (facing > 0 ? VW * 0.70f : VW * 0.30f) - bandW * 0.5f;
        float drop = (1f - in) * -(VH + 40f) + out * (VH + 40f);

        // A coluna, com bordas brancas e um rasgo de luz.
        c.fillRect(bandX - 12f, -40f + drop, bandX + bandW + 12f, VH + 40f + drop, WHITE);
        c.fillRect(bandX, -40f + drop, bandX + bandW, VH + 40f + drop, withAlpha(d.color, 245));
        c.fillRect(bandX + bandW * 0.5f - 3f, -40f + drop, bandX + bandW * 0.5f + 3f, VH + 40f + drop,
            withAlpha(d.accentColor, 120));

        // O personagem sobe de baixo, do lado de trás.
        float rise = (1f - in) * 500f + out * -200f;
        drawFigure(c, pack.cutIn, (facing > 0 ? VW * 0.30f : VW * 0.70f), VH + 40f + rise,
            700f * (1f + 0.05f * t / DURATION), facing, 255);

        if (t >= NAME_AT) {
            float k = clamp01((t - NAME_AT) / NAME_SLAM);
            int alpha = (int)(255 * Math.min(1f, k * 2f));
            float top = BAR + 58f + drop;
            c.drawText("ULTRA", bandX + bandW * 0.5f, top - 8f, 28f, withAlpha(INK, alpha), withAlpha(WHITE, alpha),
                5f, true);
            for (int col = 0; col < words.length; col++) {
                String word = words[col];
                float x = bandX + 35f + columnW * (col + 0.5f);
                for (int i = 0; i < word.length(); i++) {
                    // Cada letra bate um pouco depois da anterior.
                    float letter = clamp01((t - NAME_AT - 0.012f * (i + col * 3)) / NAME_SLAM);
                    if (letter <= 0f) continue;
                    float scale = 1f + 0.8f * (1f - easeOut(letter));
                    float y = top + size * (i + 1);
                    c.save();
                    c.scale(scale, scale, x, y - size * 0.35f);
                    c.drawText(word.substring(i, i + 1), x, y, size, withAlpha(WHITE, alpha), withAlpha(INK, alpha),
                        size * 0.13f, true);
                    c.restore();
                }
            }
        }
        drawShout(c, d.opening.shout, facing > 0 ? VW * 0.30f : VW * 0.70f, VH - BAR - 24f, t, 420f);
    }

    // ------------------------------------------------------------ dividida

    /**
     * A tela partida na diagonal: o palco e o personagem de um lado, o rival escurecido do
     * outro, os dois se encarando; o nome passa pelo corte numa faixa preta.
     */
    private void renderSplit(RenderCanvas c, UltraDefinition d, float t, float in, float out) {
        float open = in * (1f - out);
        float mid = VW * 0.5f;
        float lean = 150f * facing;
        // Metade do atacante: o palco do Ultra.
        drawStage(c, d, t);
        // Metade do rival: escura, com um brilho frio.
        float sx = (1f - open) * 900f * facing;
        setQuad(band, mid + lean + sx, -40f, mid + 2000f * facing + sx, -40f,
            mid + 2000f * facing + sx, VH + 40f, mid - lean + sx, VH + 40f);
        c.fillPolygon(band, 0xFF07080D);
        c.save();
        c.clipPolygon(band);
        c.fillRadialGlow(mid + VW * 0.28f * facing + sx, VH * 0.55f, 520f, 0x553A5A9A);
        float rivalX = mid + VW * 0.27f * facing + sx;
        // O rival olha de volta para o atacante.
        drawFigure(c, rival, rivalX, VH + 30f, 620f, -facing, 255);
        c.fillRect(-40f, -40f, VW + 40f, VH + 40f, 0x66000814);
        if (rival == null) c.fillOval(rivalX - 130f, VH * 0.25f, rivalX + 130f, VH + 60f, 0xFF000000);
        c.restore();
        // O corte: uma linha branca com a cor do Ultra por baixo.
        c.drawLine(mid + lean + sx, -40f, mid - lean + sx, VH + 40f, 22f, withAlpha(d.color, 230));
        c.drawLine(mid + lean + sx, -40f, mid - lean + sx, VH + 40f, 7f, WHITE);

        float slide = (1f - in) * -1400f * facing + out * 1600f * facing;
        drawFigure(c, pack.cutIn, mid - VW * 0.26f * facing + slide, VH + 30f, 660f * (1f + 0.04f * t / DURATION),
            facing, 255);

        if (t >= NAME_AT) {
            float k = clamp01((t - NAME_AT) / NAME_SLAM);
            int alpha = (int)(255 * Math.min(1f, k * 2f));
            float y = VH - BAR - 40f;
            c.fillRect(-40f, y - 74f, VW + 40f, y + 22f, withAlpha(INK, (int)(alpha * 0.85f)));
            c.fillRect(-40f, y - 78f, VW + 40f, y - 74f, withAlpha(d.color, alpha));
            float size = fit(c, d.name, 70f, 900f);
            float scale = 1f + 0.5f * (1f - easeOut(k));
            c.save();
            c.scale(scale, scale, mid, y - 20f);
            c.drawText(d.name, mid, y, size, withAlpha(d.color, alpha), withAlpha(WHITE, alpha), 4f, true);
            c.restore();
            c.drawText("ULTRA", mid, y - 84f, 30f, withAlpha(WHITE, alpha), withAlpha(INK, alpha), 6f, true);
        }
        drawShout(c, d.opening.shout, mid - VW * 0.26f * facing, BAR + 50f, t, 520f);
    }

    // --------------------------------------------------------------- racha

    /**
     * A tela fica preta, uma rachadura de luz corre de cima a baixo, as duas metades se abrem e
     * o personagem sobe pela luz; o nome bate embaixo.
     */
    private void renderCrack(RenderCanvas c, UltraDefinition d, float t, float out) {
        float grow = clamp01((t - 0.04f) / 0.14f);
        float split = easeOut(clamp01((t - 0.20f) / 0.22f)) * (1f - out);
        float mid = VW * 0.5f;

        // Atrás das metades: o palco e uma luz forte na cor do Ultra.
        drawStage(c, d, t);
        c.fillRadialGlow(mid, VH * 0.5f, 420f + 500f * split, withAlpha(d.accentColor, 110));
        float rise = (1f - easeOut(clamp01((t - 0.24f) / 0.24f))) * 520f;
        drawFigure(c, pack.cutIn, mid - 170f * facing, VH + 40f + rise, 700f * (1f + 0.05f * t / DURATION), facing,
            255);

        // A rachadura (fixa para o Ultra todo) e as metades pretas abrindo para os lados.
        long seed = d.name.hashCode();
        int n = CRACK_POINTS;
        for (int i = 0; i < n; i++) {
            float y = -40f + (VH + 80f) * i / (n - 1);
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            float jag = ((seed >>> 33) % 140) - 70f;
            crack[2 * i] = mid + jag + (i - n / 2f) * 18f * facing;
            crack[2 * i + 1] = y;
        }
        float gap = 1150f * split;
        for (int sideIndex = 0; sideIndex < 2; sideIndex++) {
            float dir = sideIndex == 0 ? -1f : 1f;
            for (int i = 0; i < n; i++) {
                half[2 * i] = crack[2 * i] + dir * gap * 0.5f;
                half[2 * i + 1] = crack[2 * i + 1];
            }
            half[2 * n] = dir * 3000f;
            half[2 * n + 1] = VH + 40f;
            half[2 * n + 2] = dir * 3000f;
            half[2 * n + 3] = -40f;
            c.fillPolygon(trim(half, 2 * n + 4), INK);
        }
        // A linha de luz enquanto a rachadura corre.
        int shown = (int)(grow * (n - 1));
        for (int i = 0; i < shown && split < 0.95f; i++) {
            c.drawLine(crack[2 * i], crack[2 * i + 1], crack[2 * i + 2], crack[2 * i + 3], 16f * (1f - split),
                withAlpha(d.accentColor, 255));
            c.drawLine(crack[2 * i], crack[2 * i + 1], crack[2 * i + 2], crack[2 * i + 3], 5f * (1f - split), WHITE);
        }

        if (t >= NAME_AT + 0.14f) {
            float k = clamp01((t - NAME_AT - 0.14f) / NAME_SLAM);
            int alpha = (int)(255 * Math.min(1f, k * 2f));
            float cx = mid + 230f * facing, y = VH - BAR - 60f;
            float size = fit(c, d.name, 84f, 640f);
            float scale = 1f + 0.7f * (1f - easeOut(k));
            c.save();
            c.scale(scale, scale, cx, y - size * 0.35f);
            c.drawText("ULTRA", cx, y - size - 4f, 32f, withAlpha(d.accentColor, alpha), withAlpha(INK, alpha), 5f, true);
            c.drawText(d.name, cx, y, size, withAlpha(WHITE, alpha), withAlpha(INK, alpha), size * 0.13f, true);
            c.restore();
        }
        drawShout(c, d.opening.shout, mid + 230f * facing, BAR + 56f, t, 560f);
    }

    private float[] trimmed = new float[0];

    /** As primeiras {@code length} posições de {@code poly} (fillPolygon usa o vetor inteiro). */
    private float[] trim(float[] poly, int length) {
        if (trimmed.length != length) trimmed = new float[length];
        System.arraycopy(poly, 0, trimmed, 0, length);
        return trimmed;
    }

    private static void drawShout(RenderCanvas c, String shout, float cx, float y, float t, float maxWidth) {
        if (shout == null || t < SHOUT_AT) return;
        float s = clamp01((t - SHOUT_AT) / 0.08f);
        c.drawText(shout, cx, y, fit(c, shout, 34f, maxWidth), withAlpha(WHITE, (int)(255 * s)),
            withAlpha(INK, (int)(255 * s)), 5f, true);
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
