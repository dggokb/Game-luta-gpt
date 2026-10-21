package com.gamelutagpt.motion;

/**
 * Quanto de cada efeito o motor aplica. Tudo pode ser desligado sozinho (para comparar com e
 * sem) e {@link #intensidade} escala todos de uma vez. Os valores padrão são "firmes" de
 * propósito: o desenho é pintado, e achatar muito ou balançar (mola que passa do ponto e volta)
 * vira borracha. As molas do corpo voltam sem balançar; só o tamanho muda com a intensidade.
 */
public final class Ajustes {
    /** Liga o motor inteiro (desligado, a pose é sempre a neutra). */
    public boolean ligado = true;
    /** Multiplica todos os efeitos de forma e inclinação (0 = nada, 1 = padrão). */
    public float intensidade = 1f;

    /** Achatar e esticar mantêm a área do desenho (mais baixo = mais largo na mesma medida). */
    public boolean volume = false;

    /**
     * Mola das formas (achatar da aterrissagem, da queda e do corpo dobrando) e do arrasto: volta
     * firme, sem balançar. {@code Mola.ELASTICA} aqui deixa tudo com cara de borracha.
     */
    public Mola molaForma = new Mola(4.5f, 0.9f), molaArrasto = new Mola(3.5f, 0.8f);

    /** Achata ao aterrissar e estica ao sair do chão. */
    public boolean pulo = true;
    /** Maior achatar da aterrissagem (fração da altura) e o esticar da decolagem. */
    public float achatarAterrissagem = 0.035f, esticarDecolagem = 0.025f;

    /** Caído: o corpo bate no chão, achata mais e dá um quique pequeno. */
    public boolean quique = true;
    /** Achatar da queda e a altura do quique (pixels do mundo). */
    public float achatarQueda = 0.05f, alturaQuique = 6f;

    /** Ao virar de lado o desenho estreita e abre de volta (como se girasse). */
    public boolean virada = true;
    /** Quanto estreita no quadro da virada (fração da largura). */
    public float estreitarVirada = 0.12f;

    /** O corpo cede para o lado do empurrão ao apanhar (e um pouco ao defender). */
    public boolean impacto = true;
    /** Inclinação (graus) por peso do golpe recebido: fraco, médio, forte, especial. */
    public float[] inclinacaoApanhando = {2f, 3f, 4f, 5.5f};
    public float inclinacaoDefendendo = 2f;
    /**
     * A mola por peso do golpe (recebido ou dado): o fraco volta ligeiro, o forte mais devagar,
     * todas sem balançar.
     */
    public Mola[] molaPorForca = {new Mola(4.5f, 0.85f), new Mola(4f, 0.8f), new Mola(3.4f, 0.78f), new Mola(3f, 0.75f)};

    /** Reação pela altura: na cabeça o tronco vai para trás, no corpo dobra, nas pernas os pés vão e o tronco fica. */
    public boolean alturaReacao = true;
    /** Quanto da inclinação vai no golpe no corpo (dobrando) e no baixo (arrasto), e o achatar do corpo dobrando. */
    public float fatorCorpo = 0.7f, fatorBaixo = 0.8f, dobrarCorpo = 0.015f;
    /** Cada reação varia um pouco (±20%), sempre igual no mesmo jogo: golpes iguais não parecem um loop. */
    public float variacao = 0.2f;

    /** Defender um golpe forte arrasta o pé para trás (poeirinha), duas vezes no empurrão. */
    public boolean deslizeDefesa = true;
    /** Peso mínimo do golpe defendido para o pé deslizar (2 = forte). */
    public int forcaDeslizeDefesa = 2;

    /** Acertar trava (o corpo para no rival e recua um pouco); errar passa do ponto e demora a voltar. */
    public boolean acertoTrava = true;
    /** Graus de recuo ao acertar e de passada ao errar, por peso do golpe. */
    public float[] recuoAcerto = {1f, 1.5f, 2.5f, 3f};
    public float[] grausErro = {1f, 1.5f, 2.5f, 3.5f};

    /** Com pouca vida o lutador cansa: respira fundo, ombros caídos e o idle mais lento. */
    public boolean cansaco = true;
    /** Vida abaixo da qual cansa, quanto a respiração abaixa, os graus caídos e a velocidade do idle. */
    public float limiarCansaco = 0.25f, respiracao = 0.006f, grausCansaco = 1.5f, ritmoCansado = 0.8f;

    /** Agacha na preparação do pulo (o jump squat do jogo) antes do esticar da decolagem. */
    public float agacharPulo = 0.03f;

    /** Inclina para a frente correndo (e para trás no backdash). */
    public boolean inclinacaoDash = true;
    public float grausDash = 3f;

    /**
     * Arrasto: o tronco fica para trás na largada do dash e passa da conta na freada, com os pés
     * parados (cisalhamento, não rotação).
     */
    public boolean arrasto = true;
    /** Graus do arrasto na freada; a largada usa a metade. */
    public float grausArrasto = 3f;

    /** Partículas no chão: poeira ao aterrissar, o pé deslizando na largada e na freada do dash. */
    public boolean poeira = true;
    /** Força da poeira da aterrissagem mais forte (0 a 1; o knockdown já tem a dele). */
    public float poeiraAterrissagem = 0.35f;

    /** Antecipação: na preparação do golpe o corpo recua e abaixa um pouco (sem quadro a mais). */
    public boolean antecipacao = true;
    /** Graus para trás por peso do golpe e quanto abaixa. */
    public float[] grausAntecipacao = {1f, 1.5f, 2.5f, 3f};
    public float achatarAntecipacao = 0.01f;

    /** Um tranco de esticar quando o golpe próprio fica ativo (estala). */
    public boolean estalo = true;
    /** Esticar na direção do golpe por peso: fraco, médio, forte, especial. */
    public float[] esticarGolpe = {0.008f, 0.012f, 0.018f, 0.022f};

    /** Continuidade: quando o golpe fica ativo, o corpo segue um pouco para a frente e assenta. */
    public boolean continuidade = true;
    /** Graus para a frente por peso do golpe. */
    public float[] grausContinuidade = {1f, 1.5f, 2f, 3f};

    /**
     * Smear nos golpes fortes: nos primeiros quadros ativos o desenho deixa cópias esticadas na
     * horizontal por baixo dele (o soco "borra" de tão rápido).
     */
    public boolean smear = true;
    /** Peso mínimo do golpe (2 = forte), quadros do smear e o esticar da cópia mais longa. */
    public int forcaSmear = 2, quadrosSmear = 2;
    public float esticarSmear = 0.18f;

    /** No ar: estica conforme a velocidade vertical e inclina acompanhando o arco do pulo. */
    public boolean ar = true;
    /** Esticar na velocidade de {@link #vyCheia} e os graus do arco (o pulo da corrida inclina mais). */
    public float esticarAr = 0.015f, grausArco = 2.5f, arcoEmbalo = 1.5f;
    /** Velocidade vertical (px/s) que dá o efeito inteiro no ar. */
    public float vyCheia = 1600f;

    /**
     * Mistura curta do quadro de antes ao trocar de clipe (fora de golpes, reações, decolagem e
     * aterrissagem). Curta e fraca: com poses muito diferentes ela vira dupla exposição.
     */
    public boolean transicao = true;
    /** Quadros da mistura e a opacidade do quadro de antes no começo dela. */
    public int quadrosTransicao = 2;
    public float alfaTransicao = 0.35f;

    /** Cópias translúcidas atrás do dash, do backdash e do pulo com embalo da corrida. */
    public boolean rastro = true;
    /** Rastro também nos golpes (desligado: só nos deslocamentos rápidos). */
    public boolean rastroNosGolpes = false;
    /** Peso mínimo do golpe para deixar rastro, com {@link #rastroNosGolpes} (2 = forte). */
    public int forcaRastro = 2;
    /** Uma cópia a cada tantos quadros, quantas no máximo e quanto cada uma vive. */
    public int intervaloRastro = 2, copiasRastro = 3, vidaRastro = 8;
    public float alfaRastro = 0.35f;

    /** Tudo desligado (a referência para comparar). */
    public static Ajustes desligado() {
        Ajustes a = new Ajustes();
        a.ligado = false;
        return a;
    }
}
