package com.gamelutagpt.ultra;

import java.util.Locale;

/**
 * Modelo da página de mangá do Ultra: onde cada painel fica, como ele entra, onde vai a faixa
 * com o nome e a cor do papel. Escolhido no ultra.json ({@code "estilo": {"pagina": "diagonal"}}).
 *
 * <p>Em todos os modelos cada painel mantém o formato da imagem que o
 * {@code tools/ultra/preparar_ultra.py} recorta (olhos 5.6:1, carga quadrada, golpe 1.75:1,
 * final 16:9): os modelos só mudam posição, inclinação e acabamento, então
 * trocar o modelo não pede imagens novas. A imagem é desenhada alinhada com o painel (se o
 * painel é inclinado, ela também é).
 */
final class PaginaLayout {
    static final float VW = PaginaFinal.VW;
    static final float VH = PaginaFinal.VH;

    /** Como o painel bate na página. */
    static final int SLAM_SCALE = 0, SLAM_SLIDE = 1, SLAM_SPIN = 2;
    /** Desenho atrás dos painéis. */
    static final int BACK_NONE = 0, BACK_SLASH = 1, BACK_RAYS = 2, BACK_CRACKS = 3;
    /** Cor da faixa do nome: tinta preta ou a cor do Ultra. */
    static final int STRIP_INK = 0, STRIP_COLOR = 1;

    static final String CLASSICA = "classica", DIAGONAL = "diagonal", LEQUE = "leque", QUEBRADA = "quebrada";
    static final String[] NAMES = {CLASSICA, DIAGONAL, LEQUE, QUEBRADA};

    final String name;
    /** Os 4 painéis (quadriláteros x0,y0..x3,y3, na ordem de {@link UltraSlot}); o último é a tela toda. */
    final float[][] panels;
    /** Onomatopeia de cada painel: x, y, rotação, tamanho. */
    final float[][] sfx;
    /** Centro e inclinação da faixa do nome; {@code nameSide} = de que lado ela entra (1 direita, -1 esquerda). */
    final float nameX, nameY, nameAngle;
    final int nameSide;
    final int paper, border, strip, slam, backdrop;
    final float borderWidth;

    private PaginaLayout(String name, float[][] panels, float[][] sfx, float nameX, float nameY, float nameAngle,
                         int nameSide, int paper, int border, float borderWidth, int strip, int slam, int backdrop) {
        this.name = name;
        this.panels = panels;
        this.sfx = sfx;
        this.nameX = nameX;
        this.nameY = nameY;
        this.nameAngle = nameAngle;
        this.nameSide = nameSide;
        this.paper = paper;
        this.border = border;
        this.borderWidth = borderWidth;
        this.strip = strip;
        this.slam = slam;
        this.backdrop = backdrop;
    }

    /** O modelo com esse nome; nome desconhecido (ou null) cai na página clássica. */
    static PaginaLayout byName(String name) {
        String key = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        if (DIAGONAL.equals(key)) return DIAGONAL_LAYOUT;
        if (LEQUE.equals(key)) return LEQUE_LAYOUT;
        if (QUEBRADA.equals(key)) return QUEBRADA_LAYOUT;
        return CLASSICA_LAYOUT;
    }

    static boolean isKnown(String name) {
        if (name == null) return true;
        String key = name.trim().toLowerCase(Locale.ROOT);
        for (String n : NAMES) if (n.equals(key)) return true;
        return false;
    }

    // ------------------------------------------------------------ modelos

    private static final int CREAM = 0xFFF1ECE1;
    private static final int INK = ArteProvisoria.INK;
    private static final int WHITE = ArteProvisoria.WHITE;
    private static final float[] FULL = {-40f, -40f, VW + 40f, -40f, VW + 40f, VH + 40f, -40f, VH + 40f};

    /** A página de sempre: tiras quase retas, painéis que batem crescendo. */
    static final PaginaLayout CLASSICA_LAYOUT = new PaginaLayout(CLASSICA,
        new float[][] {
            {28f, 28f, 1252f, 28f, 1252f, 206f, 28f, 246f},
            {28f, 266f, 470f, 256f, 420f, 692f, 28f, 692f},
            {490f, 255f, 1252f, 226f, 1252f, 692f, 440f, 692f},
            FULL
        },
        new float[][] {
            {0f, 0f, 0f, 0f},
            {250f, 650f, -10f, 64f},
            {1000f, 610f, -8f, 110f},
            {640f, 210f, -6f, 150f}
        },
        941f, 234f, 0f, 1, CREAM, INK, 8f, STRIP_INK, SLAM_SCALE, BACK_NONE);

    /**
     * Agressiva: as tiras cortam a página na diagonal, paralelas, com um rasgo na cor do Ultra
     * atrás, e cada painel entra deslizando como um golpe.
     */
    static final PaginaLayout DIAGONAL_LAYOUT = stacked(DIAGONAL, -8f,
        640f, 112f, 1180f, 200f,     // olhos
        400f,                        // carga (lado)
        750f, 428f,                  // golpe (largura, altura)
        18f, 250f,                   // vão entre as tiras, deslocamento do golpe
        CREAM, INK, 8f, STRIP_INK, SLAM_SLIDE, BACK_SLASH);

    /**
     * Elegante: os painéis se abrem em leque, cada um num ângulo, com borda branca grossa como
     * fotos jogadas na mesa, raios suaves na cor do Ultra atrás, e entram girando.
     */
    static final PaginaLayout LEQUE_LAYOUT = build(LEQUE,
        new float[][] {
            rect(640f, 116f, 1190f, 212f, -2.5f),
            rect(292f, 468f, 430f, 430f, 5f),
            rect(870f, 470f, 790f, 452f, -5f),
            FULL
        },
        905f, 232f, -2.5f, 1, 0xFFF8F3EA, WHITE, 12f, STRIP_INK, SLAM_SPIN, BACK_RAYS);

    /**
     * Sombria: página preta, painéis tortos como vidro quebrado com borda branca, rachaduras
     * entre eles e a faixa do nome na cor do Ultra.
     */
    static final PaginaLayout QUEBRADA_LAYOUT = build(QUEBRADA,
        new float[][] {
            jitter(rect(640f, 122f, 1210f, 216f, 1.5f), -14f, 8f, 18f, -10f, -8f, 12f, 10f, -14f),
            jitter(rect(272f, 470f, 432f, 432f, -4f), 12f, -16f, -10f, 10f, 16f, -6f, -12f, 8f),
            jitter(rect(858f, 472f, 800f, 456f, 2.5f), -16f, 10f, 12f, -8f, -8f, 10f, 12f, 8f),
            FULL
        },
        928f, 236f, 1.5f, 1, 0xFF0C0B10, WHITE, 7f, STRIP_COLOR, SLAM_SCALE, BACK_CRACKS);

    /**
     * Tiras paralelas inclinadas: olhos em cima ocupando a largura, carga num lado e o golpe
     * no outro, todos no mesmo ângulo.
     */
    private static PaginaLayout stacked(String name, float angle, float eyesX, float eyesY, float eyesW, float eyesH,
                                        float cargaSize, float golpeW, float golpeH, float gap, float shift,
                                        int paper, int border, float borderWidth, int strip, int slam, int backdrop) {
        double a = Math.toRadians(angle);
        float tx = (float)Math.cos(a), ty = (float)Math.sin(a);   // ao longo da tira
        float nx = -ty, ny = tx;                                    // para baixo da tira
        float g = eyesH * 0.5f + gap + golpeH * 0.5f;
        float golpeX = eyesX + nx * g + tx * shift, golpeY = eyesY + ny * g + ty * shift;
        float side = eyesH * 0.5f + gap + cargaSize * 0.5f + 10f;
        float cargaX = eyesX + nx * side - tx * (eyesW * 0.5f - cargaSize * 0.5f - 34f);
        float cargaY = eyesY + ny * side - ty * (eyesW * 0.5f - cargaSize * 0.5f - 34f);
        float nameD = eyesH * 0.5f + 6f;
        return build(name,
            new float[][] {
                rect(eyesX, eyesY, eyesW, eyesH, angle),
                rect(cargaX, cargaY, cargaSize, cargaSize, angle),
                rect(golpeX, golpeY, golpeW, golpeH, angle),
                FULL
            },
            eyesX + nx * nameD + tx * 300f, eyesY + ny * nameD + ty * 300f, angle, 1,
            paper, border, borderWidth, strip, slam, backdrop);
    }

    /** Onomatopeias nos lugares de sempre dentro de cada painel (calculados do próprio painel). */
    private static PaginaLayout build(String name, float[][] panels, float nameX, float nameY, float nameAngle,
                                      int nameSide, int paper, int border, float borderWidth, int strip, int slam,
                                      int backdrop) {
        float[][] sfx = new float[4][];
        sfx[0] = new float[] {0f, 0f, 0f, 0f};
        sfx[1] = spot(panels[1], 0f, 0.36f, -10f, 64f);
        sfx[2] = spot(panels[2], 0.2f, 0.34f, -8f, 110f);
        sfx[3] = new float[] {640f, 210f, -6f, 150f};
        return new PaginaLayout(name, panels, sfx, nameX, nameY, nameAngle, nameSide, paper, border, borderWidth,
            strip, slam, backdrop);
    }

    /** Ponto (u, v) do painel no quadro dele (-0.5..0.5), com a rotação do painel somada à dada. */
    private static float[] spot(float[] poly, float u, float v, float rotation, float size) {
        Frame f = frame(poly, new Frame());
        double a = Math.toRadians(f.angle);
        float lx = u * f.w, ly = v * f.h;
        float x = f.cx + lx * (float)Math.cos(a) - ly * (float)Math.sin(a);
        float y = f.cy + lx * (float)Math.sin(a) + ly * (float)Math.cos(a);
        return new float[] {x, y, rotation + f.angle, size};
    }

    /** Retângulo w x h centrado em (cx, cy), girado {@code degrees} graus. */
    static float[] rect(float cx, float cy, float w, float h, float degrees) {
        double a = Math.toRadians(degrees);
        float c = (float)Math.cos(a), s = (float)Math.sin(a);
        float[] local = {-w / 2, -h / 2, w / 2, -h / 2, w / 2, h / 2, -w / 2, h / 2};
        float[] out = new float[8];
        for (int i = 0; i < 8; i += 2) {
            out[i] = cx + local[i] * c - local[i + 1] * s;
            out[i + 1] = cy + local[i] * s + local[i + 1] * c;
        }
        return out;
    }

    private static float[] jitter(float[] poly, float... d) {
        float[] out = poly.clone();
        for (int i = 0; i < 8; i++) out[i] += d[i];
        return out;
    }

    // ------------------------------------------------- quadro do painel

    /** Quadro de um painel: centro, inclinação (da borda de cima) e tamanho nesse ângulo. */
    static final class Frame {
        float cx, cy, angle, w, h;
    }

    static Frame frame(float[] poly, Frame out) {
        float cx = (poly[0] + poly[2] + poly[4] + poly[6]) * 0.25f;
        float cy = (poly[1] + poly[3] + poly[5] + poly[7]) * 0.25f;
        double a = Math.atan2(poly[3] - poly[1], poly[2] - poly[0]);
        float c = (float)Math.cos(-a), s = (float)Math.sin(-a);
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i < 8; i += 2) {
            float dx = poly[i] - cx, dy = poly[i + 1] - cy;
            float x = dx * c - dy * s, y = dx * s + dy * c;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        out.cx = cx + (minX + maxX) * 0.5f * (float)Math.cos(a) - (minY + maxY) * 0.5f * (float)Math.sin(a);
        out.cy = cy + (minX + maxX) * 0.5f * (float)Math.sin(a) + (minY + maxY) * 0.5f * (float)Math.cos(a);
        out.angle = (float)Math.toDegrees(a);
        out.w = maxX - minX;
        out.h = maxY - minY;
        return out;
    }
}
