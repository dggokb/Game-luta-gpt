package com.gamelutagpt.motion;

/**
 * Quanto de cada efeito o motor aplica. Tudo pode ser desligado sozinho (para comparar com e
 * sem) e {@link #intensidade} escala todos de uma vez. Os valores padrão são discretos de
 * propósito: o desenho é pintado, e muito achatar vira borracha.
 */
public final class Ajustes {
    /** Liga o motor inteiro (desligado, a pose é sempre a neutra). */
    public boolean ligado = true;
    /** Multiplica todos os efeitos de forma e inclinação (0 = nada, 1 = padrão). */
    public float intensidade = 1f;

    /** Achata ao aterrissar e estica ao sair do chão. */
    public boolean pulo = true;
    /** Maior achatar da aterrissagem (fração da altura) e o esticar da decolagem. */
    public float achatarAterrissagem = 0.09f, esticarDecolagem = 0.06f;

    /** O corpo cede para o lado do empurrão ao apanhar (e um pouco ao defender). */
    public boolean impacto = true;
    /** Inclinação (graus) por peso do golpe recebido: fraco, médio, forte, especial. */
    public float[] inclinacaoApanhando = {3f, 4.5f, 6f, 8f};
    public float inclinacaoDefendendo = 2f;

    /** Inclina para a frente correndo (e para trás no backdash). */
    public boolean inclinacaoDash = true;
    public float grausDash = 4f;

    /** Um tranco de esticar quando o golpe próprio fica ativo (estala). */
    public boolean estalo = true;
    /** Esticar na direção do golpe por peso: fraco, médio, forte, especial. */
    public float[] esticarGolpe = {0.015f, 0.025f, 0.035f, 0.045f};

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
