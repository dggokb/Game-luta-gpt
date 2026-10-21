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
    /**
     * Arrasto em graus: cisalha o desenho com os pés parados (a cabeça anda, o chão não);
     * positivo leva a cabeça para onde o lutador olha.
     */
    public float arrasto;
    /** Deslocamento em pixels do mundo (x para onde ele olha, y para baixo). */
    public float deslocX, deslocY;

    /** Transição: o quadro de antes, desenhado por cima com esta opacidade (0 = nenhum). */
    public String clipeAnterior;
    public int quadroAnterior;
    public float alfaAnterior;

    /**
     * Smear do golpe forte: opacidade (0 = nenhum) das cópias esticadas por baixo do desenho e
     * quanto a mais longa estica na horizontal.
     */
    public float smear, smearEsticar;

    /** Eventos do passo para as partículas do chão: força da poeira (0 = nenhuma). */
    public float poeira;
    /** O pé desliza no chão neste passo: direção do deslize no mundo (1 direita, -1 esquerda; 0 = não). */
    public int deslize;

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
        return escalaX == 1f && escalaY == 1f && inclinacao == 0f && arrasto == 0f && deslocX == 0f && deslocY == 0f;
    }

    void neutralizar() {
        escalaX = escalaY = 1f;
        inclinacao = arrasto = deslocX = deslocY = 0f;
        smear = smearEsticar = poeira = 0f;
        deslize = 0;
        clipeAnterior = null;
        alfaAnterior = 0f;
        fantasmas = 0;
    }
}
