package com.gamelutagpt;

import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

/**
 * Filtro de cor do desenho de um lutador nos dois desenhos: o {@link ColorFilter} do Canvas e a
 * mesma conta como matriz 4x5 do ColorMatrix (somas de 0 a 255, na cor sem o alfa multiplicado),
 * que o OpenGL aplica ({@link DesenhoGl}).
 */
final class Filtro {
    final ColorFilter canvas;
    final float[] matriz;

    private Filtro(ColorFilter canvas, float[] matriz) {
        this.canvas = canvas;
        this.matriz = matriz;
    }

    static Filtro matriz(float[] m) {
        return new Filtro(new ColorMatrixColorFilter(m), m.clone());
    }

    static Filtro matriz(ColorMatrix m) {
        return matriz(m.getArray());
    }

    /** SRC_IN: a cor no lugar do desenho, com o alfa dos dois multiplicado (silhuetas). */
    static Filtro silhueta(int argb) {
        float a = (argb >>> 24) / 255f;
        return new Filtro(new PorterDuffColorFilter(argb, PorterDuff.Mode.SRC_IN), new float[] {
            0f, 0f, 0f, 0f, (argb >> 16) & 0xFF,
            0f, 0f, 0f, 0f, (argb >> 8) & 0xFF,
            0f, 0f, 0f, 0f, argb & 0xFF,
            0f, 0f, 0f, a, 0f});
    }

    /** SRC_ATOP: a cor por cima do desenho, na força do alfa dela (clarões). */
    static Filtro porCima(int argb) {
        float a = (argb >>> 24) / 255f, k = 1f - a;
        return new Filtro(new PorterDuffColorFilter(argb, PorterDuff.Mode.SRC_ATOP), new float[] {
            k, 0f, 0f, 0f, ((argb >> 16) & 0xFF) * a,
            0f, k, 0f, 0f, ((argb >> 8) & 0xFF) * a,
            0f, 0f, k, 0f, (argb & 0xFF) * a,
            0f, 0f, 0f, 1f, 0f});
    }

    /** MULTIPLY com uma cor opaca: o desenho tingido por ela. */
    static Filtro multiplica(int rgb) {
        return new Filtro(new PorterDuffColorFilter(0xFF000000 | rgb, PorterDuff.Mode.MULTIPLY), new float[] {
            ((rgb >> 16) & 0xFF) / 255f, 0f, 0f, 0f, 0f,
            0f, ((rgb >> 8) & 0xFF) / 255f, 0f, 0f, 0f,
            0f, 0f, (rgb & 0xFF) / 255f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f});
    }
}
