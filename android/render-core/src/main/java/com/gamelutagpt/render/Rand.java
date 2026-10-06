package com.gamelutagpt.render;

/**
 * Gerador pseudoaleatório pequeno e reiniciável. Reiniciar com a mesma
 * semente repete a mesma sequência, o que permite desenhar o mesmo
 * conteúdo várias vezes no mesmo quadro (ex.: em cada caco da página)
 * sem alocar nada.
 */
public final class Rand {
    private long state;

    public Rand(long seed) {
        reset(seed);
    }

    public void reset(long seed) {
        state = (seed ^ 0x5DEECE66DL) & ((1L << 48) - 1);
    }

    /** Valor em [0, 1). */
    public float next() {
        state = (state * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
        return (int)(state >>> 24) / (float)(1 << 24);
    }

    /** Valor em [min, max). */
    public float range(float min, float max) {
        return min + (max - min) * next();
    }
}
