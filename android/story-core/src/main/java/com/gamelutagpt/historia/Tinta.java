package com.gamelutagpt.historia;

/** Contas pequenas de cor e de animação usadas pelas páginas. */
final class Tinta {
    private Tinta() {}

    /**
     * Uma linha de texto digitada até {@code visiveis} letras, alinhada à esquerda em {@code x}.
     * A linha vai sempre inteira e um recorte mostra só o que já foi digitado: escrever o pedaço
     * (um texto novo a cada letra) obrigava o desenho a refazer o texto quase a cada quadro.
     */
    static void digitado(com.gamelutagpt.render.RenderCanvas c, String linha, int visiveis, float x, float y,
                         float size, int fill, int stroke, float strokeWidth, boolean italic) {
        float lw = c.measureText(linha, size, italic);
        if (visiveis >= linha.length()) {
            c.drawText(linha, x + lw * 0.5f, y, size, fill, stroke, strokeWidth, italic);
            return;
        }
        float pw = c.measureText(linha.substring(0, Math.max(0, visiveis)), size, italic);
        float l = x - strokeWidth - 2f, r = x + pw + strokeWidth * 0.5f;
        float t = y - size * 1.4f, b = y + size * 0.6f;
        c.save();
        c.clipPolygon(new float[] {l, t, r, t, r, b, l, b});
        c.drawText(linha, x + lw * 0.5f, y, size, fill, stroke, strokeWidth, italic);
        c.restore();
    }

    static int withAlpha(int argb, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    /** Mistura a cor com preto (k = 0 mantém, k = 1 vira preto). */
    static int darken(int argb, float k) {
        float f = 1f - clamp01(k);
        int r = (int)(((argb >> 16) & 0xFF) * f);
        int g = (int)(((argb >> 8) & 0xFF) * f);
        int b = (int)((argb & 0xFF) * f);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    static float easeOutCubic(float t) {
        float u = 1f - clamp01(t);
        return 1f - u * u * u;
    }

    /** Passa um pouco do ponto e volta, como um balão que "estala" na página. */
    static float easeOutBack(float t) {
        float x = clamp01(t) - 1f;
        float s = 1.9f;
        return 1f + x * x * ((s + 1f) * x + s);
    }
}
