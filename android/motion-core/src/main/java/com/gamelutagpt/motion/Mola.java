package com.gamelutagpt.motion;

/**
 * Mola amortecida em forma fechada: o valor em qualquer instante sai de uma conta, sem simular
 * os quadros de antes. Por isso o movimento é o mesmo em toda execução (e num rollback), e
 * dá para pedir o quadro 812 direto.
 *
 * Duas respostas, as duas indo para zero:
 * <ul>
 *   <li>{@link #degrau}: começa deslocada em 1 e volta (com um pouco de passada se a mola for
 *   pouco amortecida). Serve para um tranco instantâneo, como o achatar da aterrissagem.</li>
 *   <li>{@link #impulso}: começa em 0, sai até 1 no pico e volta. Serve para um empurrão que
 *   cresce, como o tronco indo para trás quando apanha.</li>
 * </ul>
 */
public final class Mola {
    /** Botões, bordas: chega rápido, quase sem passar do ponto. */
    public static final Mola RAPIDA = new Mola(4.5f, 0.75f);
    /** O padrão: corpo, câmera. */
    public static final Mola PADRAO = new Mola(3.0f, 0.70f);
    /** Coisas grandes: devagar e sem balanço. */
    public static final Mola PESADA = new Mola(2.0f, 1.0f);
    /** Borracha: passa do ponto e balança (aterrissagem, impacto). */
    public static final Mola ELASTICA = new Mola(3.4f, 0.38f);

    /** Frequência natural (Hz) e fator de amortecimento (1 = crítico, sem balanço). */
    public final float frequencia, amortecimento;
    private final double w, wd, zw;
    private final boolean sub, critica;
    /** Raízes do caso superamortecido. */
    private final double r1, r2;
    /** Velocidade inicial que leva o pico do impulso a 1. */
    private final double ganhoImpulso;
    /** Tempo (s) a partir do qual a resposta já é desprezível (a mola acabou). */
    final double fim;

    public Mola(float frequencia, float amortecimento) {
        if (frequencia <= 0f || amortecimento <= 0f) throw new IllegalArgumentException("mola precisa de frequência e amortecimento positivos");
        this.frequencia = frequencia;
        this.amortecimento = amortecimento;
        w = 2.0 * Math.PI * frequencia;
        zw = amortecimento * w;
        sub = amortecimento < 0.999f;
        critica = !sub && amortecimento <= 1.001f;
        if (sub) {
            wd = w * Math.sqrt(1.0 - amortecimento * amortecimento);
            r1 = r2 = 0.0;
        } else if (critica) {
            wd = 0.0;
            r1 = r2 = -w;
        } else {
            double s = w * Math.sqrt(amortecimento * amortecimento - 1.0);
            wd = 0.0;
            r1 = -zw + s;
            r2 = -zw - s;
        }
        ganhoImpulso = 1.0 / pico(this);
        // A envoltória lenta cai abaixo de 1/1000 do começo (a raiz mais lenta, no superamortecido).
        double lenta = sub || critica ? zw : -r1;
        fim = Math.log(1000.0) / lenta + (critica || !sub ? 3.0 / w : 0.0);
    }

    /** Começa em 1 parada e volta para 0. */
    public float degrau(float t) {
        if (t <= 0f) return 1f;
        if (t >= fim) return 0f;
        if (sub) {
            double e = Math.exp(-zw * t);
            return (float)(e * (Math.cos(wd * t) + zw / wd * Math.sin(wd * t)));
        }
        if (critica) return (float)((1.0 + w * t) * Math.exp(-w * t));
        return (float)((r1 * Math.exp(r2 * t) - r2 * Math.exp(r1 * t)) / (r1 - r2));
    }

    /** Começa em 0, chega a 1 no pico e volta para 0. */
    public float impulso(float t) {
        if (t <= 0f || t >= fim) return 0f;
        return (float)(ganhoImpulso * impulsoBruto(t));
    }

    /** Resposta a uma velocidade inicial 1, sem normalizar. */
    private double impulsoBruto(double t) {
        if (sub) return Math.exp(-zw * t) * Math.sin(wd * t) / wd;
        if (critica) return t * Math.exp(-w * t);
        return (Math.exp(r1 * t) - Math.exp(r2 * t)) / (r1 - r2);
    }

    /** O pico do impulso bruto (onde a derivada zera). */
    private static double pico(Mola m) {
        double tp;
        if (m.sub) tp = Math.atan2(m.wd, m.zw) / m.wd;
        else if (m.critica) tp = 1.0 / m.w;
        else tp = Math.log(m.r2 / m.r1) / (m.r1 - m.r2);
        return m.impulsoBruto(tp);
    }
}
