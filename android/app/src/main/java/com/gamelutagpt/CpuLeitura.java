package com.gamelutagpt;

/**
 * O que a CPU aprendeu do rival e das próprias tentativas (docs/balanco/cpu-adaptativa.md).
 *
 * <ul>
 *   <li><b>Hábitos do rival:</b> em cada situação (depois de defender a sequência dela, ao
 *   levantar, contra a bola dela), o que ele fez logo em seguida — atacou, pulou, recuou, avançou
 *   ou ficou esperando. Tudo pelo que aparece na tela ({@link CpuPerception}), nunca pelo botão.</li>
 *   <li><b>O que não funciona:</b> cada jeito de chegar dela (correr, pular, soltar bola,
 *   cutucar) e quantas vezes ela foi punida fazendo isso ou se deu bem.</li>
 * </ul>
 *
 * A leitura só puxa as escolhas: o estilo do lutador ({@link CpuStyle}) continua decidindo o
 * jeito, e os fatores ficam numa faixa estreita (o zoner continua soltando bola mais que os
 * outros). As observações antigas pesam menos a cada nova ({@link #ESQUECE}), e entre uma luta e
 * outra a memória esmaece ({@link #novaLuta}) mas não some: o chefe lembra do que você fez.
 * Pure Java.
 */
final class CpuLeitura {
    enum Situacao { DEPOIS_DE_DEFENDER, AO_LEVANTAR, CONTRA_BOLA }

    enum Resposta { ATACA, PULA, RECUA, AVANCA, ESPERA }

    enum Abordagem { CORRE, PULA, BOLA, CUTUCA }

    /** Peso que as observações de antes ficam a cada observação nova da mesma situação. */
    static final double ESQUECE = 0.85;
    /** Quanto da memória fica de uma luta para a outra. */
    static final double ENTRE_LUTAS = 0.6;
    /** Observações (já com o esquecimento) a partir das quais a leitura vale por inteiro. */
    static final double OBSERVACOES_CHEIAS = 3.0;
    /**
     * Faixa dos fatores das abordagens: só puxa para baixo o que tem sido punido (até 0,45), nunca
     * acima do estilo (puxar para cima o que funcionou fazia todo mundo correr e cutucar mais).
     */
    static final double FATOR_MIN = 0.45, FATOR_MAX = 1.0;

    private final double[][] conta = new double[Situacao.values().length][Resposta.values().length];
    private final double[] falhas = new double[Abordagem.values().length];
    private final double[] acertos = new double[Abordagem.values().length];

    /** O rival fez {@code r} na situação {@code s}. */
    void observa(Situacao s, Resposta r) {
        double[] c = conta[s.ordinal()];
        for (int i = 0; i < c.length; i++) c[i] *= ESQUECE;
        c[r.ordinal()] += 1.0;
    }

    double observacoes(Situacao s) {
        double t = 0;
        for (double v : conta[s.ordinal()]) t += v;
        return t;
    }

    /** Chance de ele fazer {@code r} na situação (com meio ponto de partida em cada resposta). */
    double chance(Situacao s, Resposta r) {
        int n = Resposta.values().length;
        return (conta[s.ordinal()][r.ordinal()] + 0.5) / (observacoes(s) + 0.5 * n);
    }

    /** O que ele mais faz na situação, ou null sem observação. */
    Resposta previsto(Situacao s) {
        if (observacoes(s) <= 0) return null;
        Resposta melhor = null;
        for (Resposta r : Resposta.values()) if (melhor == null || chance(s, r) > chance(s, melhor)) melhor = r;
        return melhor;
    }

    /**
     * O quanto ela confia no hábito previsto: 0 sem padrão (tudo igual) ou sem observação, 1 quando
     * ele sempre faz a mesma coisa e ela já viu bastante.
     */
    double certeza(Situacao s) {
        Resposta r = previsto(s);
        if (r == null) return 0;
        double base = 1.0 / Resposta.values().length;
        double destaque = (chance(s, r) - base) / (1.0 - base);
        return Math.max(0, destaque) * Math.min(1.0, observacoes(s) / OBSERVACOES_CHEIAS);
    }

    /** A abordagem {@code a} acabou com ela apanhando (ele estava esperando aquilo). */
    void falhou(Abordagem a) {
        falhas[a.ordinal()] = falhas[a.ordinal()] * ESQUECE + 1.0;
        acertos[a.ordinal()] *= ESQUECE;
    }

    /** A abordagem {@code a} deu certo (acertou): as falhas dela vão sendo esquecidas. */
    void deuCerto(Abordagem a) {
        acertos[a.ordinal()] = acertos[a.ordinal()] * ESQUECE + 1.0;
        falhas[a.ordinal()] *= ESQUECE;
    }

    /** Quanto puxar a abordagem: abaixo de 1 a que tem sido punida, acima a que tem funcionado. */
    double fator(Abordagem a) {
        double f = (acertos[a.ordinal()] + 1.0) / (falhas[a.ordinal()] + 1.0);
        return Math.max(FATOR_MIN, Math.min(FATOR_MAX, f));
    }

    /** Começo de uma luta nova contra o mesmo jogador: a memória fica, mais fraca. */
    void novaLuta() {
        for (double[] c : conta) for (int i = 0; i < c.length; i++) c[i] *= ENTRE_LUTAS;
        for (int i = 0; i < falhas.length; i++) {
            falhas[i] *= ENTRE_LUTAS;
            acertos[i] *= ENTRE_LUTAS;
        }
    }

    /** Campanha nova (ou outro jogador): esquece tudo. */
    void esquecerTudo() {
        for (double[] c : conta) java.util.Arrays.fill(c, 0);
        java.util.Arrays.fill(falhas, 0);
        java.util.Arrays.fill(acertos, 0);
    }

    /** Uma linha para o depurador e as baterias. */
    String resumo() {
        StringBuilder s = new StringBuilder();
        for (Situacao st : Situacao.values()) {
            Resposta r = previsto(st);
            s.append(st.name().toLowerCase()).append('=').append(r == null ? "—" : r.name().toLowerCase())
                .append(String.format(java.util.Locale.ROOT, "(%.2f) ", certeza(st)));
        }
        for (Abordagem a : Abordagem.values()) {
            s.append(a.name().toLowerCase()).append(String.format(java.util.Locale.ROOT, "×%.2f ", fator(a)));
        }
        return s.toString().trim();
    }
}
