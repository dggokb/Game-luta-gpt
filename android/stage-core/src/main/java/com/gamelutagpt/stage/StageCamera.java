package com.gamelutagpt.stage;

/**
 * Câmera com profundidade ligada à câmera 2D da luta.
 *
 * <p>O jogo desenha os lutadores com {@code tela = (mundo - esquerda) * zoom}. Esta
 * câmera reproduz isso exatamente no plano dos lutadores (profundidade {@code z = 0})
 * e, para os outros planos, aplica perspectiva: o zoom da luta vira a distância da
 * câmera (ela se aproxima), então o que está longe anda e cresce menos que o que está
 * perto. É isso que dá a sensação de 3D.
 *
 * <ul>
 *   <li>{@code z > 0}: atrás dos lutadores (fundo); {@code z < 0}: na frente.</li>
 *   <li>Distância da câmera ao plano dos lutadores: {@code distancia / zoom}.</li>
 *   <li>O horizonte fica sempre na mesma altura da tela.</li>
 * </ul>
 */
public final class StageCamera {
    private static final float MIN_DEPTH = 1f;

    private final float groundY;
    private final float baseDistance;
    private final float horizonY;

    private float zoom = 1f;
    private float centerX;
    private float eyeY;
    private float distance;

    /**
     * @param baseDistance distância da câmera ao plano dos lutadores com zoom 1 (unidades do mundo)
     * @param horizonScreenY altura do horizonte na tela (0..VH)
     */
    public StageCamera(float groundY, float baseDistance, float horizonScreenY) {
        this.groundY = groundY;
        this.baseDistance = baseDistance;
        this.horizonY = horizonScreenY;
    }

    /** Copia a câmera da luta: zoom final, esquerda visível e topo visível (mundo). */
    public void set(float zoom, float cameraLeft, float top) {
        this.zoom = zoom;
        this.centerX = cameraLeft + StageWorld.VW / (2f * zoom);
        this.eyeY = top + horizonY / zoom;
        this.distance = baseDistance / zoom;
    }

    public float zoom() {
        return zoom;
    }

    public float centerX() {
        return centerX;
    }

    public float horizonY() {
        return horizonY;
    }

    /** Pixels de tela por unidade do mundo na profundidade z (no plano dos lutadores, = zoom). */
    public float scale(float z) {
        return baseDistance / Math.max(MIN_DEPTH, distance + z);
    }

    /** Profundidade mais próxima que ainda fica na frente da câmera. */
    public float nearestDepth() {
        return -distance + MIN_DEPTH;
    }

    public float screenX(float worldX, float z) {
        return StageWorld.VW * 0.5f + (worldX - centerX) * scale(z);
    }

    public float screenY(float worldY, float z) {
        return horizonY + (worldY - eyeY) * scale(z);
    }

    public float worldX(float screenX, float z) {
        return centerX + (screenX - StageWorld.VW * 0.5f) / scale(z);
    }

    public float worldY(float screenY, float z) {
        return eyeY + (screenY - horizonY) / scale(z);
    }

    /**
     * Profundidade do chão visto numa linha da tela, ou {@code Float.NaN} acima do
     * horizonte (ali o chão não aparece).
     */
    public float floorDepthAt(float screenY) {
        float eyeHeight = groundY - eyeY;
        if (eyeHeight <= 0f || screenY <= horizonY) return Float.NaN;
        float scale = (screenY - horizonY) / eyeHeight;
        return baseDistance / scale - distance;
    }
}
