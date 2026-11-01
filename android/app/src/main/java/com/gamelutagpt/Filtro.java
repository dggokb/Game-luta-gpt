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
        synchronized (MATRIZES) {
            MATRIZES.put(canvas, matriz);
        }
    }

    /** A matriz de cada ColorFilter feito aqui (o PorterDuffColorFilter não deixa ler a dele). */
    private static final java.util.Map<ColorFilter, float[]> MATRIZES = new java.util.WeakHashMap<>();

    /** A matriz 4x5 de um ColorFilter feito por {@link Filtro}, ou null. */
    static float[] matrizDe(ColorFilter f) {
        if (f == null) return null;
        synchronized (MATRIZES) {
            return MATRIZES.get(f);
        }
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

    /**
     * Cor alternativa (os dois lutadores são o mesmo personagem): no OpenGL, o sombreador gira só
     * a cor das partes coloridas ({@code giro}, em voltas) e tinge os escuros (roupa preta, cabelo)
     * para o matiz {@code matizEscuros} na força {@code escuros}, deixando a pele como está, e
     * depois aplica a matriz de {@code base} (o desgaste da luta). A matriz vai com 24 números: os 20
     * da base e 1, giro, escuros, matizEscuros. No Canvas (sem sombreador), a cor inteira gira.
     */
    static Filtro corAlternativa(Filtro base, float giro, float escuros, float matizEscuros) {
        float[] b = base != null && base.matriz.length >= 20 ? base.matriz : new float[] {
            1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0};
        float[] m = new float[24];
        System.arraycopy(b, 0, m, 0, 20);
        m[20] = 1f;
        m[21] = giro;
        m[22] = escuros;
        m[23] = matizEscuros;
        ColorMatrix cm = new ColorMatrix(giroDeMatiz(giro * 360f));
        cm.postConcat(new ColorMatrix(java.util.Arrays.copyOf(b, 20)));
        return new Filtro(new ColorMatrixColorFilter(cm), m);
    }

    /** Giro de matiz (graus) que mantém o brilho: a conta do ColorMatrix de giro de cor. */
    static float[] giroDeMatiz(float graus) {
        double a = Math.toRadians(graus);
        float c = (float)Math.cos(a), s = (float)Math.sin(a);
        float lr = 0.213f, lg = 0.715f, lb = 0.072f;
        return new float[] {
            lr + c * (1 - lr) - s * lr, lg - c * lg - s * lg, lb - c * lb + s * (1 - lb), 0, 0,
            lr - c * lr + s * 0.143f, lg + c * (1 - lg) + s * 0.140f, lb - c * lb - s * 0.283f, 0, 0,
            lr - c * lr - s * (1 - lr), lg - c * lg + s * lg, lb + c * (1 - lb) + s * lb, 0, 0,
            0, 0, 0, 1, 0};
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
