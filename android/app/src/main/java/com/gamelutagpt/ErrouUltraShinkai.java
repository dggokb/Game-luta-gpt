package com.gamelutagpt;

import java.util.Random;

/**
 * "Droga!": o Ultra SHINKAI acabou sem encostar no rival (o motor avisa ULTRA_SHINKAI_ERROU_<lado>)
 * e quem soltou reclama no quadro de mangá da conversa do lado dele, como o grito da troca
 * ({@link GritoDaTroca}): o rosto, o nome e a legenda digitada (o dono, 18/11: o balão branco não é
 * o padrão de fala do jogo). Os textos em português são a chave da tradução.
 */
final class ErrouUltraShinkai {
    /** O que ele diz e a cara dele (as expressões dos retratos da história). */
    static final String[][] FALAS = {
        {"Droga!", "bravo"},
        {"O quê?!", "surpreso"},
        {"Errei!", "bravo"},
    };
    /** Segundos na tela depois de digitado (uma reação curta, como o grito da troca). */
    static final float SEGURA = 1.2f;

    private ErrouUltraShinkai() {}

    /** Uma fala sorteada: {texto, expressão}. */
    static String[] sortear(Random random) {
        return FALAS[random.nextInt(FALAS.length)];
    }
}
