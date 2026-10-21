package com.gamelutagpt.motion;

/**
 * Um valor animado por molas (a inclinação, o achatar...), em função do quadro.
 *
 * O valor é o alvo mais a soma de componentes, cada um uma mola que começou num quadro. Um
 * tranco ({@link #degrau}, {@link #impulso}) soma um componente sem mudar o alvo. Mudar o alvo
 * ({@link #alvo}) não reinicia nada: soma um degrau com a diferença, e o valor segue contínuo
 * de onde estava até o alvo novo. Assim o canal continua sendo uma conta do quadro (sem
 * integrar velocidade), igual em toda execução.
 */
public final class Canal {
    /** Componentes ao mesmo tempo; o mais velho sai quando enche (já quase parado). */
    static final int MAXIMO = 12;

    private final Mola molaDoAlvo;
    private float alvo;
    private final int[] inicio = new int[MAXIMO];
    private final float[] amplitude = new float[MAXIMO];
    private final boolean[] eImpulso = new boolean[MAXIMO];
    private final Mola[] mola = new Mola[MAXIMO];
    private int quantos;

    /** {@code molaDoAlvo}: como o valor anda quando o alvo muda. */
    public Canal(Mola molaDoAlvo) {
        this.molaDoAlvo = molaDoAlvo;
    }

    public float alvo() {
        return alvo;
    }

    /** O alvo passa a ser {@code novo} a partir do quadro; o valor vai até ele pela mola do canal. */
    public void alvo(int quadro, float novo) {
        if (novo == alvo) return;
        // O valor no quadro continua o mesmo: a diferença vira um degrau que some com a mola.
        adicionar(quadro, alvo - novo, false, molaDoAlvo);
        alvo = novo;
    }

    /** Tranco instantâneo de {@code amplitude} que volta pela mola. */
    public void degrau(int quadro, float amplitude, Mola mola) {
        adicionar(quadro, amplitude, false, mola);
    }

    /** Empurrão que cresce até {@code amplitude} e volta pela mola. */
    public void impulso(int quadro, float amplitude, Mola mola) {
        adicionar(quadro, amplitude, true, mola);
    }

    /** O valor no quadro (quadros de 1/60 s). */
    public float valor(int quadro) {
        float v = alvo;
        for (int i = 0; i < quantos; i++) {
            float t = (quadro - inicio[i]) / 60f;
            v += amplitude[i] * (eImpulso[i] ? mola[i].impulso(t) : mola[i].degrau(t));
        }
        return v;
    }

    /** Solta os componentes que já pararam no quadro (não muda o valor). */
    public void limpar(int quadro) {
        int n = 0;
        for (int i = 0; i < quantos; i++) {
            if ((quadro - inicio[i]) / 60.0 >= mola[i].fim) continue;
            copiar(i, n++);
        }
        quantos = n;
    }

    /** Volta ao repouso no alvo zero, sem componentes. */
    public void zerar() {
        alvo = 0f;
        quantos = 0;
    }

    /** Componentes ainda vivos (testes). */
    int componentes() {
        return quantos;
    }

    private void adicionar(int quadro, float amp, boolean impulso, Mola m) {
        if (amp == 0f) return;
        if (quantos == MAXIMO) {
            // Cheio: o mais velho sai (componentes velhos já estão quase parados).
            for (int i = 1; i < quantos; i++) copiar(i, i - 1);
            quantos--;
        }
        inicio[quantos] = quadro;
        amplitude[quantos] = amp;
        eImpulso[quantos] = impulso;
        mola[quantos] = m;
        quantos++;
    }

    private void copiar(int de, int para) {
        if (de == para) return;
        inicio[para] = inicio[de];
        amplitude[para] = amplitude[de];
        eImpulso[para] = eImpulso[de];
        mola[para] = mola[de];
    }
}
