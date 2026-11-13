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

    /**
     * Tinta de mangá (o impact frame dos animes): o próprio desenho em preto e branco duro, pela
     * claridade de cada ponto, para o traço e as sombras dele aparecerem (em vez da silhueta
     * chapada). Claridade abaixo de {@code limiar} (0 a 1) vira tinta, acima vira papel; {@code
     * ganho} é o quão dura é a passagem (8: uma faixa de 1/8 da escala). Uma matriz de cor só,
     * igual no OpenGL, no Canvas e no PC; o alfa fica o do desenho. {@code invertido}: o claro vira
     * tinta e o escuro, papel.
     */
    static Filtro tinta(float limiar, float ganho, boolean invertido) {
        float k = invertido ? -ganho : ganho;
        float soma = (0.5f - k * limiar) * 255f;
        float r = 0.299f * k, g = 0.587f * k, b = 0.114f * k;
        return matriz(new float[] {
            r, g, b, 0f, soma,
            r, g, b, 0f, soma,
            r, g, b, 0f, soma,
            0f, 0f, 0f, 1f, 0f});
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
        // A luz do cenário (contraluz e degradê, depois dos 24) segue junto.
        float[] m = new float[Math.max(24, b.length)];
        System.arraycopy(b, 0, m, 0, b.length);
        m[20] = 1f;
        m[21] = giro;
        m[22] = escuros;
        m[23] = matizEscuros;
        ColorMatrix cm = new ColorMatrix(giroDeMatiz(giro * 360f));
        cm.postConcat(new ColorMatrix(java.util.Arrays.copyOf(b, 20)));
        return new Filtro(new ColorMatrixColorFilter(cm), m);
    }

    /** Onde começa a luz do cenário na matriz do filtro (depois dos 20 da matriz e dos 4 da troca). */
    static final int LUZ = 24;
    /** Números do filtro com a luz: 24 + ligada, cor da contraluz (3), força, direção (2), pés (3), contorno (4). */
    static final int COM_LUZ = 38;
    /** O contorno do corpo (só no OpenGL): a cor (3, de 0 a 1) e a largura (fração da altura da tela; 0 desliga). */
    static final int CONTORNO = 34;

    /**
     * {@code base} (a tinta, a luz e a cor alternativa, como já vinham) com o contorno do corpo:
     * um fio de {@code largura} (fração da altura da tela) na cor {@code rgb} por fora da silhueta,
     * no mesmo desenho do lutador. O corpo da arte já tem a linha de dentro; o fio de fora separa o
     * lutador do cenário (o recorte das referências). No Canvas fica só a matriz.
     */
    static Filtro comContorno(Filtro base, int rgb, float largura) {
        float[] b = base != null && base.matriz.length >= 20 ? base.matriz : new float[] {
            1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0};
        float[] m = new float[COM_LUZ];
        System.arraycopy(b, 0, m, 0, Math.min(b.length, CONTORNO));
        m[CONTORNO] = ((rgb >> 16) & 0xFF) / 255f;
        m[CONTORNO + 1] = ((rgb >> 8) & 0xFF) / 255f;
        m[CONTORNO + 2] = (rgb & 0xFF) / 255f;
        m[CONTORNO + 3] = largura;
        // Cor alternativa: no Canvas o giro vai junto na matriz (o filtro guarda só os números).
        ColorMatrix cm = new ColorMatrix(java.util.Arrays.copyOf(b, 20));
        if (b.length >= 24 && b[20] > 0.5f) {
            cm = new ColorMatrix(giroDeMatiz(b[21] * 360f));
            cm.postConcat(new ColorMatrix(java.util.Arrays.copyOf(b, 20)));
        }
        return new Filtro(new ColorMatrixColorFilter(cm), m);
    }

    /**
     * A matriz {@code base} (a tinta do lugar e o desgaste) com a luz do cenário que só o OpenGL
     * desenha: a contraluz (um fio de luz de {@code rim} na força {@code forca} na borda do corpo do
     * lado da tela para onde aponta {@code dirX, dirY}) e o degradê dos pés (os pés multiplicados
     * por {@code pes}). No Canvas fica só a matriz.
     */
    static Filtro comLuz(float[] base, float[] rim, float forca, float dirX, float dirY, float[] pes) {
        float[] m = new float[COM_LUZ];
        System.arraycopy(base, 0, m, 0, Math.min(20, base.length));
        m[LUZ] = 1f;
        m[LUZ + 1] = rim[0]; m[LUZ + 2] = rim[1]; m[LUZ + 3] = rim[2];
        m[LUZ + 4] = forca;
        m[LUZ + 5] = dirX; m[LUZ + 6] = dirY;
        m[LUZ + 7] = pes[0]; m[LUZ + 8] = pes[1]; m[LUZ + 9] = pes[2];
        return new Filtro(new ColorMatrixColorFilter(java.util.Arrays.copyOf(m, 20)), m);
    }

    /**
     * Sombra de verdade no chão (só no OpenGL; no Canvas é a silhueta lisa): a silhueta na cor
     * {@code rgb}, dura nos pés e abrindo (borrada) e clareando até a cabeça, que é a ponta da
     * sombra deitada. {@code suave} de 0 a 1: o quanto ela abre e clareia. O {@code m[LUZ]} = 2
     * avisa o sombreador (a contraluz é 1).
     */
    static Filtro sombra(int rgb, float suave) {
        float[] m = new float[COM_LUZ];
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        m[4] = r; m[9] = g; m[14] = b; m[18] = 1f;
        m[LUZ] = 2f;
        m[LUZ + 1] = 1f - 0.6f * suave;    // o escuro que sobra na ponta
        m[LUZ + 4] = 0.06f * suave;        // o raio da penumbra na ponta (fração da altura do quadro)
        return new Filtro(new PorterDuffColorFilter(0xFF000000 | rgb, PorterDuff.Mode.SRC_IN), m);
    }

    /**
     * A graduação do cenário: o cenário fica atrás dos lutadores em valor e saturação. É a regra
     * da direção de arte de Street Fighter IV/V (Toshiyuki Kamei, CEDEC 2016 / GDC 2017): o
     * personagem em cor cheia e o cenário mais escuro, de paleta limitada e pouca saturação, para o
     * lutador ler na frente de qualquer fundo (docs/revisao-base/direcao.md).
     */
    static Filtro cenario(float saturacao, float valor) {
        ColorMatrix m = new ColorMatrix();
        m.setSaturation(saturacao);
        ColorMatrix escuro = new ColorMatrix();
        escuro.setScale(valor, valor, valor, 1f);
        m.postConcat(escuro);
        return matriz(m);
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
