package com.gamelutagpt;

/**
 * SHINKAI Clash: o lutador que liga o SHINKAI com o rival já no dele entra num confronto
 * jogável, um cabo de guerra no meio da tela. Não é QTE: cada um empurra com os próprios
 * botões, e o tipo de cada um muda o jeito de empurrar:
 * <ul>
 *   <li>POWER: força bruta, o H empurra muito (mas tem fôlego entre um e outro), o L pouco;</li>
 *   <li>SPEED: velocidade, o L empurra bem e quase sem espera (bater rápido);</li>
 *   <li>ENERGY: alimentar o raio, segurar um botão carrega e soltar empurra (quanto mais tempo,
 *       muito mais forte), e enquanto segura não empurra;</li>
 *   <li>TECHNIQUE: ritmo, um pulso a cada {@link #BATIDA} quadros; apertar no pulso empurra muito,
 *       fora dele quase nada.</li>
 * </ul>
 * Segurar trás desiste: sai sem dano, mas perde o SHINKAI. Quem leva a marca até o fim
 * ({@link #META}), ou está na frente quando o tempo acaba, ganha: o golpe dele entra forte, o
 * SHINKAI dele volta cheio e o do outro acaba. Empate: os dois se afastam e seguem no SHINKAI.
 * Java puro, determinístico; o {@link CombatEngine} roda um quadro por passo enquanto ele dura.
 */
final class ShinkaiClash {
    /** A marca vai de −META (o lado 1 venceu) a +META (o lado 0 venceu). */
    static final int META = 600;
    /** Quadros de disputa (5 s), depois da entrada. */
    static final int DURACAO = 300;
    /** O pulso da TECHNIQUE e a janela em volta dele que vale como "no ritmo". */
    static final int BATIDA = 24, JANELA_BATIDA = 4;
    /** Quadros segurando trás para desistir. */
    static final int DESISTIR = 30;
    /** A carga máxima do ENERGY. */
    static final int CARGA_MAX = 70;

    enum Fim { NENHUM, VITORIA, EMPATE, DESISTENCIA }

    final Shinkai.Type[] tipo = new Shinkai.Type[2];
    /** Quadros de entrada (o corte do SHINKAI de quem respondeu): ninguém empurra ainda. */
    final int entrada;
    /** Quadros desde o começo (entrada incluída). */
    int quadro;
    /** + favorece o lado 0. */
    int marca;
    /** O último empurrão de cada lado e há quantos quadros (o pulso no desenho). */
    final int[] ultimoEmpurrao = new int[2], idadeEmpurrao = {999, 999};
    /** Espera entre dois empurrões (POWER e SPEED). */
    final int[] espera = new int[2];
    /** ENERGY: quadros segurando. */
    final int[] carga = new int[2];
    /** TECHNIQUE: este pulso já foi usado. */
    final boolean[] batidaUsada = new boolean[2];
    /** Quadros seguidos segurando trás. */
    final int[] recuo = new int[2];
    /** A frente de cada lado (para ler trás). */
    final int[] olhando = new int[2];

    Fim fim = Fim.NENHUM;
    /** Quem venceu (ou desistiu, em DESISTENCIA), ou −1. */
    int lado = -1;

    ShinkaiClash(Shinkai.Type tipo0, Shinkai.Type tipo1, int entrada, int olhando0, int olhando1) {
        tipo[0] = tipo0;
        tipo[1] = tipo1;
        this.entrada = entrada;
        olhando[0] = olhando0;
        olhando[1] = olhando1;
    }

    boolean acabou() { return fim != Fim.NENHUM; }

    /** Já está na disputa (passou da entrada). */
    boolean disputando() { return quadro >= entrada && !acabou(); }

    /** Quadros da disputa que já passaram. */
    int tempo() { return Math.max(0, quadro - entrada); }

    /** Posição do pulso da TECHNIQUE: 0 no pulso, até BATIDA − 1. */
    int fase() { return tempo() % BATIDA; }

    /** Onde o encontro está: +1 é em cima do lado 1 (o lado 0 empurrou tudo), −1 em cima do lado 0. */
    float frente() { return marca / (float)META; }

    void passo(FighterInput a, FighterInput b) {
        if (acabou()) return;
        quadro++;
        for (int i = 0; i < 2; i++) if (idadeEmpurrao[i] < 999) idadeEmpurrao[i]++;
        if (quadro < entrada) return;
        int[] empurrao = {empurrao(0, a), empurrao(1, b)};
        if (acabou()) return;
        marca += empurrao[0] - empurrao[1];
        for (int i = 0; i < 2; i++) {
            if (empurrao[i] > 0) {
                ultimoEmpurrao[i] = empurrao[i];
                idadeEmpurrao[i] = 0;
            }
        }
        if (marca >= META) vence(0);
        else if (marca <= -META) vence(1);
        else if (tempo() >= DURACAO) {
            if (marca > 0) vence(0);
            else if (marca < 0) vence(1);
            else fim = Fim.EMPATE;
        }
    }

    private void vence(int quem) {
        fim = Fim.VITORIA;
        lado = quem;
        marca = Math.max(-META, Math.min(META, marca));
    }

    /** O empurrão do lado {@code i} neste quadro, pelo tipo dele. */
    private int empurrao(int i, FighterInput in) {
        int rel = MotionParser.relative(in.direction, olhando[i]);
        boolean tras = rel == 4 || rel == 5 || rel == 6;
        recuo[i] = tras ? recuo[i] + 1 : 0;
        if (recuo[i] >= DESISTIR) {
            fim = Fim.DESISTENCIA;
            lado = i;
            return 0;
        }
        if (espera[i] > 0) espera[i]--;
        boolean aperto = in.light || in.medium || in.heavy;
        switch (tipo[i]) {
            case POWER:
                if (!aperto || espera[i] > 0) return 0;
                espera[i] = in.heavy ? 12 : in.medium ? 8 : 5;
                return in.heavy ? 58 : in.medium ? 34 : 16;
            case SPEED:
                if (!aperto || espera[i] > 0) return 0;
                espera[i] = 3;
                return in.light ? 36 : in.medium ? 28 : 20;
            case ENERGY: {
                if (in.holding) {
                    carga[i] = Math.min(CARGA_MAX, carga[i] + 1);
                    return 0;
                }
                if (carga[i] > 0) {
                    // Soltou: o raio cresce muito mais do que o tempo segurando.
                    int c = carga[i];
                    carga[i] = 0;
                    return Math.max(8, c * c / 16);
                }
                // Toque sem segurar (teclado sem "segurando"): um empurrãozinho.
                return aperto ? 14 : 0;
            }
            case TECHNIQUE:
            default: {
                int fase = fase();
                boolean noPulso = fase <= JANELA_BATIDA || fase >= BATIDA - JANELA_BATIDA;
                if (fase == BATIDA / 2) batidaUsada[i] = false;
                if (!aperto) return 0;
                if (noPulso && !batidaUsada[i]) {
                    batidaUsada[i] = true;
                    return 105;
                }
                return 10;
            }
        }
    }
}
