package com.gamelutagpt;

/**
 * Peso dos golpes na tela (só apresentação; o motor não lê nada daqui): um soco de câmera
 * no ponto do impacto, o cenário escurecendo por alguns quadros nos golpes fortes (os
 * "impact frames" do Garou e do FighterZ), e no KO um clarão branco com câmera lenta.
 * Anda no relógio de 60 Hz da luta.
 */
final class ImpactFeel {
    /** Zoom extra por tamanho de golpe (LIGHT, MEDIUM, HEAVY, SPECIAL), punish counter e KO. */
    static final float[] ZOOM = {0f, 0.012f, 0.035f, 0.05f};
    static final float PUNISH_ZOOM = 0.07f, KO_ZOOM = 0.09f;
    /** Quanto do zoom sobra a cada quadro (volta suave para o enquadramento normal). */
    static final float ZOOM_DECAY = 0.86f;
    /** Cenário escurecido (alfa 0..255) e quantos quadros dura, por tamanho de golpe. */
    static final int[] DARK = {0, 0, 70, 100};
    static final int[] DARK_FRAMES = {0, 0, 3, 4};
    static final int PUNISH_DARK = 140, PUNISH_DARK_FRAMES = 6;
    /** KO: clarão branco (alfa) e a câmera lenta (quadros da luta e a velocidade deles). */
    static final int KO_FLASH = 210;
    static final int KO_SLOW_FRAMES = 42;
    static final float KO_SLOW_SCALE = 0.35f;

    private float zoom;
    private float focusX, focusY;
    private int dark, darkFrames, darkTotal;
    private float flash;
    private int slowFrames;

    void clear() {
        zoom = 0f;
        dark = darkFrames = darkTotal = 0;
        flash = 0f;
        slowFrames = 0;
    }

    /**
     * Um golpe que acertou (defesas não chamam). {@code size} é a classe do {@link ImpactEffects};
     * {@code punish}: punish counter; {@code ko}: o golpe que nocauteou.
     */
    void onHit(float x, float y, int size, boolean punish, boolean ko) {
        size = Math.max(ImpactEffects.LIGHT, Math.min(ImpactEffects.SPECIAL, size));
        float kick = ko ? KO_ZOOM : punish ? PUNISH_ZOOM : ZOOM[size];
        if (kick >= zoom) {
            zoom = kick;
            focusX = x;
            focusY = y;
        }
        int alpha = punish || ko ? PUNISH_DARK : DARK[size];
        int frames = punish || ko ? PUNISH_DARK_FRAMES : DARK_FRAMES[size];
        if (alpha > 0 && alpha >= darkAlpha()) {
            dark = alpha;
            darkFrames = darkTotal = frames;
        }
        if (ko) {
            flash = KO_FLASH;
            slowFrames = KO_SLOW_FRAMES;
        }
    }

    /** Um passo da luta. */
    void step() {
        zoom *= ZOOM_DECAY;
        if (zoom < 0.0005f) zoom = 0f;
        if (darkFrames > 0) darkFrames--;
        flash *= 0.88f;
        if (flash < 2f) flash = 0f;
        if (slowFrames > 0) slowFrames--;
    }

    /** Multiplicador do zoom da câmera (1 = normal). */
    float zoom() { return 1f + zoom; }

    /** Ponto do mundo que o soco de câmera segura no mesmo lugar da tela. */
    float focusX() { return focusX; }
    float focusY() { return focusY; }

    /** Alfa do escurecimento do cenário agora (atrás dos lutadores). */
    int darkAlpha() {
        if (darkFrames <= 0 || darkTotal <= 0) return 0;
        return Math.round(dark * darkFrames / (float)darkTotal);
    }

    /** Alfa do clarão branco do KO (na frente de tudo no mundo). */
    int flashAlpha() { return Math.round(flash); }

    /** Velocidade do relógio da luta: abaixo de 1 na câmera lenta do KO. */
    float timeScale() { return slowFrames > 0 ? KO_SLOW_SCALE : 1f; }

    /**
     * Tremida de quem apanhou durante o congelamento do acerto (hitstop), alternando de lado a
     * cada quadro; menor na defesa. 0 fora dele.
     */
    static float hitShake(CombatFighter f, int frame) {
        if (f.hitstop <= 0) return 0f;
        float amplitude;
        if (f.framesSinceHit == 0) amplitude = Math.min(9f, 2.5f + f.hitstop * 0.7f);
        else if (f.framesSinceBlock == 0) amplitude = 2.5f;
        else return 0f;
        return (frame % 2 == 0 ? 1f : -1f) * amplitude;
    }
}
