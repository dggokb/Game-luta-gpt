package com.gamelutagpt.ultra;

/**
 * Tudo o que o motor precisa para desenhar. As coordenadas são sempre as
 * virtuais do jogo (1280x720); cada plataforma converte para a tela real.
 * Cores são ARGB empacotadas em int.
 */
public interface UltraCanvas {
    void save();

    void restore();

    void translate(float dx, float dy);

    void scale(float sx, float sy, float px, float py);

    void rotate(float degrees, float px, float py);

    /** Recorta os próximos desenhos ao polígono {x0, y0, x1, y1, ...}. */
    void clipPolygon(float[] xy);

    void fillRect(float left, float top, float right, float bottom, int argb);

    void fillPolygon(float[] xy, int argb);

    void strokePolygon(float[] xy, float width, int argb);

    void fillCircle(float cx, float cy, float radius, int argb);

    void strokeCircle(float cx, float cy, float radius, float width, int argb);

    /** Linha com pontas arredondadas. */
    void drawLine(float x1, float y1, float x2, float y2, float width, int argb);

    void drawImage(UltraImage image, float left, float top, float right, float bottom, int alpha);

    /** Texto centralizado em (x, y), em negrito, com contorno opcional. */
    void drawText(
        String text,
        float x,
        float y,
        float size,
        int fillArgb,
        int strokeArgb,
        float strokeWidth,
        boolean italic
    );

    /** Tudo entre beginInvert e endInvert sai com as cores invertidas (quadro de impacto). */
    void beginInvert();

    void endInvert();
}
