package com.gamelutagpt;

/**
 * A coreografia do SHINKAI Clash (estilo Dragon Ball): enquanto a disputa ({@link ShinkaiClash})
 * corre, os dois não ficam parados um na frente do outro. A luta passa por três cenas, e quem está
 * ganhando o cabo de guerra é quem leva a melhor em cada uma:
 * <ul>
 *   <li>CHOQUE, no chão: os dois se chocam no meio e empurram; o ponto do choque anda para o lado
 *       de quem está perdendo (o chão racha, pedras sobem);</li>
 *   <li>AR: sobem e trocam golpes no ar indo e voltando pela arena, cruzando um por cima do outro;
 *       quem está na frente acerta e o outro se encolhe;</li>
 *   <li>VELOCIDADE: somem e reaparecem em pontos diferentes da arena (no chão e no alto), cada
 *       aparição é um choque; entre uma e outra só os estouros no ar.</li>
 * </ul>
 * Começa e termina no CHOQUE; a ordem das outras duas depende dos tipos (SPEED abre na
 * VELOCIDADE). Java puro e determinístico: o motor põe os lutadores onde a coreografia manda, o
 * desenho ({@link ConfrontoShinkai}) lê os choques e as poses.
 */
final class CoreografiaClash {
    enum Cena { CHOQUE, AR, VELOCIDADE }

    /** A pose de cada um neste quadro (o desenho escolhe a animação). */
    enum Pose { EMPURRA, SOCO_L, SOCO_M, SOCO_H, AR_L, AR_M, AR_H, APANHA_AR, PULO }

    /** Fim de cada cena, em quadros da disputa (o resto até o fim é o choque final). */
    static final int FIM_ABERTURA = 80, FIM_SEGUNDA = 160, FIM_TERCEIRA = 240;
    /** Uma troca no ar e uma aparição da velocidade, em quadros. */
    static final int TROCA = 16, APARICAO = 16, VISIVEL = 8;
    /** Altura média no ar e a distância entre os dois no choque (px do jogo). */
    static final float ALTURA_AR = 115f, PERTO = 70f;

    private final float meio, metade, esquerda, direita;
    private final boolean velocidadePrimeiro;

    /** Onde cada um está: x, altura acima do chão, para que lado olha, se aparece e a pose. */
    final float[] x = new float[2], altura = new float[2];
    final int[] olha = new int[2];
    final boolean[] visivel = {true, true};
    final Pose[] pose = {Pose.EMPURRA, Pose.EMPURRA};
    Cena cena = Cena.CHOQUE;
    /** Choque neste quadro (o estouro no ar ou o encontro das mãos): onde e de que tamanho (0 a 1). */
    boolean choque;
    float choqueX, choqueY, forca;
    /** O último lugar de choque (o encontro que o desenho segue). */
    float encontroX, encontroY;
    /** Quadros desde a última troca de cena (o desenho anuncia a cena nova). */
    int naCena;

    /**
     * @param meio centro da arena onde começou; {@code metade} metade da distância inicial; os
     *             limites são os da arena, com folga para o corpo.
     */
    CoreografiaClash(Shinkai.Type tipo0, Shinkai.Type tipo1, float meio, float metade, float esquerda, float direita,
                     float x0, float x1) {
        this.meio = meio;
        this.metade = metade;
        this.esquerda = esquerda;
        this.direita = direita;
        velocidadePrimeiro = tipo0 == Shinkai.Type.SPEED || tipo1 == Shinkai.Type.SPEED;
        x[0] = x0;
        x[1] = x1;
        olha[0] = x0 <= x1 ? 1 : -1;
        olha[1] = -olha[0];
        encontroX = meio;
        encontroY = 120f;
    }

    /** A cena no quadro {@code t} da disputa. */
    Cena cenaEm(int t) {
        if (t < FIM_ABERTURA || t >= FIM_TERCEIRA) return Cena.CHOQUE;
        boolean segunda = t < FIM_SEGUNDA;
        return segunda == velocidadePrimeiro ? Cena.VELOCIDADE : Cena.AR;
    }

    /**
     * Um passo: {@code t} quadros de disputa, {@code frente} de −1 (o lado 0 perdendo tudo) a +1
     * (o lado 0 ganhando), quem empurrou agora (para o golpe do choque) e o lado que começou à
     * esquerda.
     */
    void passo(int t, float frente, boolean[] empurrou) {
        Cena antes = cena;
        cena = cenaEm(t);
        naCena = cena == antes && t > 0 ? naCena + 1 : 0;
        choque = false;
        visivel[0] = visivel[1] = true;
        switch (cena) {
            case AR: ar(t - inicio(t), frente); break;
            case VELOCIDADE: velocidade(t - inicio(t), frente); break;
            default: choqueNoChao(frente, empurrou); break;
        }
    }

    private int inicio(int t) {
        return t < FIM_SEGUNDA ? FIM_ABERTURA : FIM_SEGUNDA;
    }

    /** Lado que está à esquerda agora (as cenas cruzam um por cima do outro). */
    private int esq() { return x[0] <= x[1] ? 0 : 1; }

    /** No chão, frente a frente; o choque anda para o lado de quem perde. */
    private void choqueNoChao(float frente, boolean[] empurrou) {
        int e = esq();
        // + frente favorece o lado 0: o choque vai em direção ao lado 1.
        float rumo = e == 0 ? 1f : -1f;
        float alvo = Math.max(esquerda + PERTO, Math.min(direita - PERTO, meio + rumo * frente * metade * 0.8f));
        float cx = naCena == 0 ? alvo : encontroX + (alvo - encontroX) * 0.25f;
        mover(e, cx - PERTO, 0f, 0.3f);
        mover(1 - e, cx + PERTO, 0f, 0.3f);
        olha[e] = 1;
        olha[1 - e] = -1;
        pose[0] = pose[1] = Pose.EMPURRA;
        encontroX = cx;
        encontroY = 120f;
        if (empurrou[0] || empurrou[1]) {
            choque = true;
            choqueX = cx;
            choqueY = encontroY;
            forca = 0.6f;
        }
    }

    /**
     * No ar: os dois sobem e andam juntos pela arena; a cada troca se aproximam, o golpe estoura no
     * meio, e a cada duas cruzam um por cima do outro. Quem está ganhando acerta; o outro apanha.
     */
    private void ar(int u, float frente) {
        int troca = u / TROCA, dentro = u % TROCA;
        float centro = meio + (float)Math.sin(u * 0.045f) * Math.min(220f, (direita - esquerda) * 0.25f);
        centro = Math.max(esquerda + 120f, Math.min(direita - 120f, centro));
        // Afastados no começo e no fim de cada troca, colados no meio dela.
        float perto = (float)Math.abs(Math.cos(Math.PI * dentro / TROCA));
        float sep = 55f + 95f * perto;
        // Cruzam: em trocas ímpares o lado 0 vai para a direita.
        boolean cruzado = troca % 2 == 1;
        int e = cruzado ? 1 : 0;
        float sobe = ALTURA_AR + 22f * (float)Math.sin(u * 0.09f);
        // Na virada de lado um passa por cima do outro.
        float arco = troca > 0 && dentro < TROCA / 2 ? 40f * (float)Math.sin(Math.PI * dentro / (TROCA / 2f)) : 0f;
        mover(e, centro - sep, sobe + arco, naCena < 10 ? 0.25f : 0.5f);
        mover(1 - e, centro + sep, sobe - arco * 0.4f, naCena < 10 ? 0.25f : 0.5f);
        olha[e] = 1;
        olha[1 - e] = -1;
        int forte = frente > 0.12f ? 0 : frente < -0.12f ? 1 : -1;
        Pose golpe = new Pose[] {Pose.AR_L, Pose.AR_M, Pose.AR_H}[troca % 3];
        if (dentro >= TROCA / 2 - 3 && dentro <= TROCA / 2 + 4) {
            pose[0] = forte == 1 && dentro > TROCA / 2 ? Pose.APANHA_AR : golpe;
            pose[1] = forte == 0 && dentro > TROCA / 2 ? Pose.APANHA_AR : golpe;
        } else {
            pose[0] = pose[1] = Pose.PULO;
        }
        encontroX = (x[0] + x[1]) / 2f;
        encontroY = sobe + 110f;
        if (dentro == TROCA / 2) {
            choque = true;
            choqueX = encontroX;
            choqueY = encontroY;
            forca = troca % 3 == 2 ? 1f : 0.7f;
        }
    }

    /**
     * Velocidade: aparecem juntos num ponto (alguns quadros, golpes trocados), somem e
     * reaparecem em outro. Enquanto somem, estouros no ar onde estão se batendo sem ninguém ver.
     */
    private void velocidade(int u, float frente) {
        int k = u / APARICAO, dentro = u % APARICAO;
        float px = pontoX(k), alto = pontoAltura(k);
        boolean aparece = dentro < VISIVEL;
        int lado0 = k % 2 == 0 ? 0 : 1;   // a cada aparição trocam de lado
        float sep = dentro < 2 ? 90f : 58f;
        x[lado0] = px - sep;
        x[1 - lado0] = px + sep;
        altura[0] = altura[1] = alto;
        olha[lado0] = 1;
        olha[1 - lado0] = -1;
        visivel[0] = visivel[1] = aparece;
        int forte = frente > 0.12f ? 0 : frente < -0.12f ? 1 : -1;
        Pose golpe = alto > 20f ? new Pose[] {Pose.AR_M, Pose.AR_H, Pose.AR_L}[k % 3]
            : new Pose[] {Pose.SOCO_M, Pose.SOCO_H, Pose.SOCO_L}[k % 3];
        pose[0] = pose[1] = golpe;
        if (aparece && dentro >= 4 && forte >= 0) pose[1 - forte] = alto > 20f ? Pose.APANHA_AR : Pose.SOCO_L;
        encontroX = px;
        encontroY = alto + 110f;
        if (dentro == 2) {
            choque = true;
            choqueX = px;
            choqueY = encontroY;
            forca = 1f;
        } else if (!aparece && dentro == VISIVEL + 4) {
            // O estouro no meio do caminho até o próximo ponto, sem ninguém aparecer.
            choque = true;
            choqueX = (px + pontoX(k + 1)) / 2f + (float)Math.sin(k * 2.3f) * 80f;
            choqueY = Math.max(pontoAltura(k), pontoAltura(k + 1)) + 150f;
            forca = 0.5f;
        }
    }

    /** O ponto da aparição {@code k}: espalhado pela arena, longe das bordas. */
    float pontoX(int k) {
        float r = (float)((Math.sin(k * 12.9898 + 4.1) * 43758.5453) % 1.0);
        r = r < 0f ? r + 1f : r;
        float min = esquerda + 140f, max = direita - 140f;
        return min + (max - min) * r;
    }

    /** No chão nas aparições pares, no alto nas ímpares (mais alto a cada uma). */
    float pontoAltura(int k) {
        return k % 2 == 0 ? 0f : 90f + (k % 3) * 35f;
    }

    /** Vai até o alvo aos poucos (sem pular de um lugar para o outro, menos na velocidade). */
    private void mover(int i, float alvoX, float alvoAltura, float passo) {
        alvoX = Math.max(esquerda, Math.min(direita, alvoX));
        x[i] += (alvoX - x[i]) * passo;
        altura[i] += (alvoAltura - altura[i]) * passo;
        if (Math.abs(altura[i]) < 1f) altura[i] = 0f;
    }
}
