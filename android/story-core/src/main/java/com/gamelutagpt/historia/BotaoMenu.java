package com.gamelutagpt.historia;

import com.gamelutagpt.render.RenderCanvas;

/**
 * Opção dos menus (menu inicial e pausa): uma faixa inclinada de mangá. A escolhida fica
 * vermelha, com borda branca grossa, a setinha e um brilho pulsando; as outras ficam escuras.
 */
final class BotaoMenu {
    private BotaoMenu() {}

    static final int RED = 0xFFC8202C;
    static final int INK = Baloes.INK;
    static final int WHITE = Baloes.WHITE;
    private static final int DARK = 0xE6101018;
    private static final int GRAY = 0xFFB9C0D0;
    /** Quanto a faixa se inclina (topo para a direita, base para a esquerda). */
    private static final float SLANT = 18f;

    /** Faixa da opção: {esquerda, topo, direita, base}. */
    static boolean dentro(float[] caixa, float x, float y) {
        return x >= caixa[0] - SLANT && x <= caixa[2] + SLANT && y >= caixa[1] && y <= caixa[3];
    }

    /**
     * @param escolhida o cursor está nela
     * @param pulso relógio do brilho (segundos)
     * @param flash 0 a 1: clarão branco quando acabou de ser escolhida
     * @param alpha 0 a 255: entrada da opção na tela
     */
    static void draw(RenderCanvas c, float[] caixa, String titulo, String subtitulo, boolean escolhida,
                     float pulso, float flash, int alpha) {
        if (alpha <= 0) return;
        float l = caixa[0], t = caixa[1], r = caixa[2], b = caixa[3];
        float shift = escolhida ? 14f : 0f;
        l += shift;
        r += shift;
        float[] faixa = {l + SLANT, t, r + SLANT, t, r - SLANT, b, l - SLANT, b};
        float[] sombra = {l + SLANT + 8f, t + 8f, r + SLANT + 8f, t + 8f, r - SLANT + 8f, b + 8f, l - SLANT + 8f, b + 8f};
        float cy = (t + b) * 0.5f;
        if (escolhida) {
            float glow = 0.55f + 0.45f * (float)Math.sin(pulso * 5f);
            c.fillRadialGlow((l + r) * 0.5f, cy, (r - l) * 0.55f, Tinta.withAlpha(RED, (int)(alpha * 0.35f * glow)));
        }
        c.fillPolygon(sombra, Tinta.withAlpha(0xFF000000, alpha * 2 / 3));
        c.fillPolygon(faixa, Tinta.withAlpha(escolhida ? RED : DARK, alpha));
        c.strokePolygon(faixa, escolhida ? 5f : 2.5f, Tinta.withAlpha(escolhida ? WHITE : GRAY, alpha));
        if (flash > 0f) c.fillPolygon(faixa, Tinta.withAlpha(WHITE, (int)(alpha * Tinta.clamp01(flash))));

        boolean comSub = subtitulo != null && !subtitulo.isEmpty();
        float tituloY = comSub ? cy + 4f : cy + 13f;
        float size = fit(c, titulo, 36f, r - l - 90f, true);
        c.drawText(titulo, (l + r) * 0.5f, tituloY, size, Tinta.withAlpha(WHITE, alpha),
            Tinta.withAlpha(INK, alpha), size * 0.14f, true);
        if (comSub) {
            float subSize = fit(c, subtitulo, 17f, r - l - 70f, false);
            c.drawText(subtitulo, (l + r) * 0.5f, b - 11f, subSize,
                Tinta.withAlpha(escolhida ? 0xFFFFE3E0 : GRAY, alpha), 0, 0f, false);
        }
        if (escolhida) {
            // Setinha de cursor batendo para a direita, antes da faixa.
            float bob = (float)Math.sin(pulso * 8f) * 4f;
            float x = l - SLANT - 26f + bob;
            float[] seta = {x - 14f, cy - 16f, x + 12f, cy, x - 14f, cy + 16f};
            c.fillPolygon(seta, Tinta.withAlpha(WHITE, alpha));
            c.strokePolygon(seta, 3f, Tinta.withAlpha(INK, alpha));
        }
    }

    static float fit(RenderCanvas c, String text, float size, float width, boolean italic) {
        float w = c.measureText(text, size, italic);
        return w <= width ? size : size * width / w;
    }
}
