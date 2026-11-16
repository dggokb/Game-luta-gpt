package com.gamelutagpt;

/**
 * Casca de banana no chão (o S3 da p16, 16/11): fica até {@link #framesLeft} acabar. A dona que
 * passa por cima (andando ou correndo) escorrega e sai a rasteira deslizando ({@link #SLIDE});
 * o rival que pisa escorrega e cai. Criada e contada pelo CombatEngine.
 */
final class Casca {
    /** O golpe da rasteira da dona que escorrega na casca (move do pacote). */
    static final String SLIDE = "SC";
    /** Meia largura do pé em cima da casca. */
    static final float PISADA = 28f;

    final float x;
    final int ownerIndex;
    final int direction;
    int framesLeft;
    /** O rival estava em cima quando ela caiu: só escorrega depois de sair e pisar de novo. */
    boolean rivalEmCima;

    Casca(float x, int ownerIndex, int direction, int frames) {
        this.x = x;
        this.ownerIndex = ownerIndex;
        this.direction = direction;
        this.framesLeft = frames;
    }
}
