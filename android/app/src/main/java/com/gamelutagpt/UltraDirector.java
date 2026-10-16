package com.gamelutagpt;

/**
 * Câmera do Ultra em sequência (o "diretor"): a cada golpe que acerta a câmera corta seco para
 * outro plano, como nos Ultras do FighterZ. Os planos saem dos quadros em que cada golpe da
 * animação acerta ({@link CharacterDefinition.UltraSequence#hits}), então todo Ultra em
 * sequência ganha a montagem sem configurar nada:
 *
 * <ul>
 *   <li>antes do 1º golpe: plano médio nos dois;</li>
 *   <li>do 1º ao penúltimo golpe: close de lado, câmera baixa e plano aberto, em rodízio;</li>
 *   <li>o último plano antes do golpe final é o close de preparação (a página de mangá entra
 *       logo depois);</li>
 *   <li>depois do golpe final: a câmera da luta volta (com a câmera lenta do fim).</li>
 * </ul>
 *
 * Só apresentação; Java puro.
 */
final class UltraDirector {
    static final int OPEN = 0, SIDE = 1, LOW = 2, WIDE = 3, CLOSE = 4, FINAL = 5;
    private static final int[] CYCLE = {SIDE, LOW, WIDE};

    /** Zoom (sobre o da luta), inclinação (graus, a favor de quem bate) e ponto na tela. */
    static final float[] ZOOM = {1.25f, 1.65f, 1.9f, 1.3f, 2.15f, 1f};
    static final float[] TILT = {0f, -6f, 9f, -3f, 4f, 0f};
    /** Aproximação lenta dentro de cada plano (até +4%). */
    static final float PUSH = 0.04f, PUSH_FRAMES = 40f;

    /** Um plano: zoom, inclinação, ponto do mundo que vai para (anchorX, anchorY) da tela. */
    static final class Shot {
        int kind;
        float zoom = 1f, tilt, focusX, focusY, anchorX = 0.5f, anchorY = 0.5f;
    }

    /** Qual plano vale no quadro {@code frame} da sequência. */
    static int kind(int[] hits, int frame) {
        int done = 0;
        while (done < hits.length && hits[done] <= frame) done++;
        if (done == 0) return OPEN;
        if (done >= hits.length) return FINAL;
        if (done == hits.length - 1) return CLOSE;
        return CYCLE[(done - 1) % CYCLE.length];
    }

    /**
     * Preenche {@code out} para o quadro dado. (ax, ay): peito do atacante; (vx, vy): peito de
     * quem apanha; {@code facing}: para onde o atacante olha.
     */
    static void shot(Shot out, int[] hits, int frame, float ax, float ay, float vx, float vy, int facing) {
        int kind = kind(hits, frame);
        int cutAt = 0;
        for (int hit : hits) if (hit <= frame) cutAt = hit;
        float push = 1f + PUSH * Math.min(1f, (frame - cutAt) / PUSH_FRAMES);
        out.kind = kind;
        out.zoom = kind == FINAL ? 1f : ZOOM[kind] * push;
        out.tilt = TILT[kind] * (facing >= 0 ? 1f : -1f);
        float midX = (ax + vx) * 0.5f, midY = (ay + vy) * 0.5f;
        out.anchorX = 0.5f;
        out.anchorY = 0.52f;
        switch (kind) {
            case LOW:
                // Câmera no chão olhando para cima: o golpe fica alto na tela.
                out.focusX = midX;
                out.focusY = vy + 40f;
                out.anchorY = 0.44f;
                break;
            case CLOSE:
                // Preparação: o rosto e o tronco de quem bate, um pouco para trás do centro.
                out.focusX = ax;
                out.focusY = ay - 30f;
                out.anchorX = 0.5f - 0.1f * (facing >= 0 ? 1f : -1f);
                out.anchorY = 0.5f;
                break;
            default:
                out.focusX = midX;
                out.focusY = midY;
                break;
        }
    }
}
