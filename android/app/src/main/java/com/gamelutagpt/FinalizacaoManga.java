package com.gamelutagpt;

/**
 * Finalização de mangá: o golpe que mata e ganha a luta com Super ou Ultra vira um quadro de
 * mangá. O cenário some atrás de um fundo chapado e linhas de concentração (集中線) convergem
 * em quem apanhou, redesenhadas várias vezes por segundo como numa animação de mangá; os
 * lutadores continuam coloridos na frente. Super: papel claro e linhas pretas. Ultra: fundo
 * preto, linhas brancas e linhas na cor de quem finalizou.
 *
 * <p>Só apresentação: o motor não lê nada daqui. Anda no tempo real (a câmera lenta da
 * finalização não estica o efeito), parado quando a luta está parada (pausa, menus, página do
 * Ultra). Se o K.O. sai com a página do Ultra cobrindo a tela, o pedido espera ela acabar.
 * O K.O. no meio do Ultra espera o golpe final dele (a explosão do raio, o último golpe da
 * sequência): {@link #segurar()} e {@link #liberar()}.
 */
final class FinalizacaoManga {
    static final int NENHUM = 0, SUPER = 1, ULTRA = 2;

    /** Entrada (o fundo cobre o cenário), o quadro cheio e a saída, em segundos. */
    static final float ENTRADA = 0.08f, CHEIO = 1.6f, SAIDA = 0.9f;
    static final float DURACAO = ENTRADA + CHEIO + SAIDA;
    /** De quanto em quanto tempo as linhas são redesenhadas (o "ferver" do mangá). */
    static final float TROCA = 0.06f;
    /** Câmera lenta da finalização (quadros da luta e a velocidade deles): mais longa que a do K.O. */
    static final int LENTA_QUADROS = 54;
    static final float LENTA_ESCALA = 0.3f;
    /** Tremida da tela no golpe. */
    static final float TREMIDA = 26f;

    private int tipo = NENHUM, pedido = NENHUM;
    private int vitima = -1, cor;
    private float tempo;
    /** O pedido espera o golpe final do Ultra. */
    private boolean segurando;

    /**
     * Tipo de finalização para o golpe {@code moveId} (força {@code strength}): Ultra, Super ou
     * nenhum (golpe comum).
     */
    static int tipoDoGolpe(String moveId, AttackDefinition.Strength strength) {
        if ("ULTRA".equals(moveId)) return ULTRA;
        if ("SUPER".equals(moveId) || strength == AttackDefinition.Strength.SUPER) return SUPER;
        return NENHUM;
    }

    /**
     * O golpe {@code tipo} matou e fechou a luta: {@code vitima} é o lado de quem caiu e
     * {@code cor} a cor de quem finalizou (as linhas extras do Ultra). Começa em {@link #comecar()}.
     */
    void pedir(int tipo, int vitima, int cor) {
        if (tipo == NENHUM || tipo < pedido) return;
        pedido = tipo;
        this.vitima = vitima;
        this.cor = cor;
    }

    /** O pedido só começa depois de {@link #liberar()} (o K.O. saiu no meio do Ultra). */
    void segurar() {
        if (pedido != NENHUM) segurando = true;
    }

    /** Chegou o golpe final do Ultra (ou ele acabou): o pedido pode começar. */
    void liberar() { segurando = false; }

    boolean segurando() { return segurando && pedido != NENHUM; }

    /** Há uma finalização esperando a tela ficar livre. */
    boolean pendente() { return pedido != NENHUM; }

    /** Pode começar agora: pedida e não esperando o golpe final do Ultra. */
    boolean pronta() { return pedido != NENHUM && !segurando; }

    /** A tela está livre: o efeito começa agora. */
    void comecar() {
        if (pedido == NENHUM || segurando) return;
        tipo = pedido;
        pedido = NENHUM;
        tempo = 0f;
    }

    /** Avança {@code dt} segundos de tempo real. */
    void atualizar(float dt) {
        if (tipo == NENHUM) return;
        tempo += Math.max(0f, dt);
        if (tempo >= DURACAO) tipo = NENHUM;
    }

    void limpar() {
        tipo = pedido = NENHUM;
        segurando = false;
        vitima = -1;
        tempo = 0f;
    }

    boolean ativo() { return tipo != NENHUM; }

    int tipo() { return tipo; }

    int vitima() { return vitima; }

    int cor() { return cor; }

    /** Quanto o quadro de mangá cobre agora: sobe na entrada, 1 no meio e desce na saída. */
    float forca() {
        if (tipo == NENHUM) return 0f;
        if (tempo < ENTRADA) return tempo / ENTRADA;
        if (tempo < ENTRADA + CHEIO) return 1f;
        return Math.max(0f, 1f - (tempo - ENTRADA - CHEIO) / SAIDA);
    }

    /** Muda a cada {@link #TROCA}: cada valor desenha as linhas de um jeito. */
    int semente() { return (int)(tempo / TROCA); }
}
