package com.gamelutagpt.motion;

/**
 * O que o motor pede ao desenho num quadro. O renderer aplica tudo em volta da raiz (os pés):
 * escala, depois inclinação, depois o deslocamento. Na pose neutra nada muda.
 */
public final class Pose {
    /** Escala horizontal e vertical do sprite, com os pés parados. */
    public float escalaX = 1f, escalaY = 1f;
    /** Graus; positivo inclina a cabeça para onde o lutador olha. */
    public float inclinacao;
    /** Deslocamento em pixels do mundo (x para onde ele olha, y para baixo). */
    public float deslocX, deslocY;

    /** Transição: o quadro de antes, desenhado por cima com esta opacidade (0 = nenhum). */
    public String clipeAnterior;
    public int quadroAnterior;
    public float alfaAnterior;

    /** Rastro: cópias de quadros recentes (as primeiras são as mais velhas). */
    public final Fantasma[] rastro;
    public int fantasmas;

    /** Uma cópia do lutador num quadro recente. */
    public static final class Fantasma {
        public String clipe;
        public int quadro;
        public float x, y;
        public int olhando;
        public float alfa;
    }

    Pose(int maxFantasmas) {
        rastro = new Fantasma[maxFantasmas];
        for (int i = 0; i < maxFantasmas; i++) rastro[i] = new Fantasma();
    }

    public boolean neutra() {
        return escalaX == 1f && escalaY == 1f && inclinacao == 0f && deslocX == 0f && deslocY == 0f;
    }

    void neutralizar() {
        escalaX = escalaY = 1f;
        inclinacao = deslocX = deslocY = 0f;
        clipeAnterior = null;
        alfaAnterior = 0f;
        fantasmas = 0;
    }
}
