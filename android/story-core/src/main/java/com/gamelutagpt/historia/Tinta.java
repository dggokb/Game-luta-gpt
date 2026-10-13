package com.gamelutagpt.historia;

/** Contas pequenas de cor e de animação usadas pelas páginas. */
final class Tinta {
    private Tinta() {}

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
