package com.gamelutagpt;

import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.Shader;

/**
 * Degradês que guardam como foram feitos: o Canvas do Android não deixa ler as cores de um
 * gradiente, e o desenho em OpenGL ({@link CanvasGl}) precisa delas. São os mesmos
 * LinearGradient e RadialGradient de sempre para quem desenha no Canvas.
 */
final class Degrade {
    private Degrade() {}

    /** O que o OpenGL precisa para refazer o degradê, nas coordenadas locais do desenho. */
    interface Dados {
        boolean radial();
        /** Linear: {x0, y0, x1, y1}; radial: {cx, cy, raio}. */
        float[] geometria();
        int[] cores();
        /** Null: cores espalhadas por igual. */
        float[] posicoes();
        Shader.TileMode modo();
    }

    static final class Linear extends LinearGradient implements Dados {
        private final float[] geometria;
        private final int[] cores;
        private final float[] posicoes;
        private final Shader.TileMode modo;

        Linear(float x0, float y0, float x1, float y1, int[] cores, float[] posicoes, Shader.TileMode modo) {
            super(x0, y0, x1, y1, cores, posicoes, modo);
            geometria = new float[] {x0, y0, x1, y1};
            this.cores = cores.clone();
            this.posicoes = posicoes != null ? posicoes.clone() : null;
            this.modo = modo;
        }

        Linear(float x0, float y0, float x1, float y1, int cor0, int cor1, Shader.TileMode modo) {
            this(x0, y0, x1, y1, new int[] {cor0, cor1}, null, modo);
        }

        @Override public boolean radial() { return false; }
        @Override public float[] geometria() { return geometria; }
        @Override public int[] cores() { return cores; }
        @Override public float[] posicoes() { return posicoes; }
        @Override public Shader.TileMode modo() { return modo; }
    }

    static final class Radial extends RadialGradient implements Dados {
        private final float[] geometria;
        private final int[] cores;
        private final float[] posicoes;
        private final Shader.TileMode modo;

        Radial(float cx, float cy, float raio, int[] cores, float[] posicoes, Shader.TileMode modo) {
            super(cx, cy, raio, cores, posicoes, modo);
            geometria = new float[] {cx, cy, raio};
            this.cores = cores.clone();
            this.posicoes = posicoes != null ? posicoes.clone() : null;
            this.modo = modo;
        }

        Radial(float cx, float cy, float raio, int corCentro, int corBorda, Shader.TileMode modo) {
            this(cx, cy, raio, new int[] {corCentro, corBorda}, null, modo);
        }

        @Override public boolean radial() { return true; }
        @Override public float[] geometria() { return geometria; }
        @Override public int[] cores() { return cores; }
        @Override public float[] posicoes() { return posicoes; }
        @Override public Shader.TileMode modo() { return modo; }
    }
}
