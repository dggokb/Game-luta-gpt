package com.gamelutagpt.render;

/**
 * Tudo o que os motores visuais (ultra, cenário) precisam para desenhar. As coordenadas são sempre as
 * virtuais do jogo (1280x720); cada plataforma converte para a tela real.
 * Cores são ARGB empacotadas em int.
 */
public interface RenderCanvas {
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

    void drawImage(RenderImage image, float left, float top, float right, float bottom, int alpha);

    /** Desenha o recorte (em pixels da imagem) {@code src} esticado no retângulo de destino. */
    void drawImageRegion(
        RenderImage image,
        float srcLeft,
        float srcTop,
        float srcRight,
        float srcBottom,
        float left,
        float top,
        float right,
        float bottom,
        int alpha
    );

    /**
     * Desenha numa chamada só a imagem esticada numa faixa de quadriláteros empilhados (o chão em
     * perspectiva, o pano da bandeira): {@code linhas} linhas de vértices, cada uma com o ponto
     * da esquerda e o da direita ({x, y}: 4 números por linha em {@code xy}) e o ponto da imagem
     * que cai em cada um ({u, v} em pixels da imagem, 4 por linha em {@code uv}; fora da imagem
     * ela se repete). Só se chama quando {@link #desenhaFaixas()}.
     */
    default void drawImageStrip(RenderImage image, float[] xy, float[] uv, int linhas, int alpha) {
        throw new UnsupportedOperationException("drawImageStrip");
    }

    /**
     * A plataforma desenha {@link #drawImageStrip} de uma vez (no celular, uma chamada no lugar
     * de centenas de recortes por quadro). Sem isso o cenário desenha recorte por recorte.
     */
    default boolean desenhaFaixas() {
        return false;
    }

    /** Brilho circular: a cor no centro some até ficar transparente na borda. */
    void fillRadialGlow(float cx, float cy, float radius, int argbCenter);

    void fillVerticalGradient(float left, float top, float right, float bottom, int argbTop, int argbBottom);

    void fillOval(float left, float top, float right, float bottom, int argb);

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

    /**
     * Largura do texto como o {@link #drawText} desenha (negrito). A versão padrão é só uma
     * estimativa; as plataformas medem com a fonte de verdade.
     */
    default float measureText(String text, float size, boolean italic) {
        return text.length() * size * 0.56f;
    }

    /** Tudo entre beginInvert e endInvert sai com as cores invertidas (quadro de impacto). */
    void beginInvert();

    void endInvert();
}
