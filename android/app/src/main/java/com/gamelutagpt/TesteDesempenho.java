package com.gamelutagpt;

/**
 * Chaves do Modo Teste para achar, no próprio celular, o que pesa no quadro: cada uma desliga uma
 * parte do desenho (a luta continua igual). Valem só nesta sessão do app.
 */
final class TesteDesempenho {
    /** Enfeites por quadro: desgaste, riscos de mangá, detalhes do SHINKAI, impactos, auras e rastros. */
    static boolean efeitos = true;
    /** O cenário (fundo, chão, reflexos e o que fica na frente); desligado, a luta fica sobre uma cor só. */
    static boolean cenario = true;
    /**
     * O cenário inteiro, mas sem os enfeites dele: brilhos das luzes, neblina, reflexos no chão,
     * brilhos na água e pétalas (o que o LEVE tira), na resolução de agora. Separa o peso dos
     * enfeites do peso das camadas de imagem.
     */
    static boolean cenarioSemEnfeites = false;
    /** O HUD (vidas, barras, retratos, combo); os botões de teste e os controles continuam. */
    static boolean hud = true;
    /**
     * As mensagens da luta: faixas (ROUND, FIGHT, KO, agarrão), contador de combo, números de dano,
     * o "!" do combo breaker e as legendas da conversa e do narrador.
     */
    static boolean mensagens = true;
    /**
     * A luta inteira (cenário, lutadores, efeitos, HUD e botões) desenhada em OpenGL
     * ({@link DesenhoGl}); desligado, tudo no Canvas como antes. Vale a partir da próxima luta.
     */
    static boolean openGl = true;

    /**
     * O motor de luz e sombra (docs/motor-luz-proposta.md): ligado (ON) ou desligado (OFF, a arte
     * como foi pintada). O botão LUZ do Modo Teste troca, para comparar no aparelho. No PC,
     * {@code -Dgameluta.luz=off} ({@code -Pluz=off}) começa desligado.
     */
    static final int LUZ_OFF = 0, LUZ_ON = 1;
    static volatile int luz = "off".equals(System.getProperty("gameluta.luz")) ? LUZ_OFF : LUZ_ON;

    /**
     * O motor de luz e sombra: a arte é o teto. O cenário diz a cor que o branco vira no lado aceso
     * e no lado de sombra, e o shader multiplica a arte por isso; nada soma. Os números da rampa
     * entre sombra e luz: o peso do degradê que atravessa o quadro do lutador na direção da luz, o
     * peso da inclinação do normal map (só quem tem mapa), a largura macia da passagem (0 a 0,5)
     * e o quanto a cor de um ponto de luz (do cenário ou de um efeito) é lavada para o branco. No
     * PC, {@code -Dgameluta.nova=a,b,c,d}.
     */
    private static final float[] NOVA = {0.55f, 1.1f, 0.5f, 0.35f};
    private static final float[] NOVA_DO_PC = numeros(System.getProperty("gameluta.nova"), 4);

    static float[] nova() {
        return NOVA_DO_PC != null ? NOVA_DO_PC : NOVA;
    }

    private static float[] numeros(String v, int n) {
        if (v == null) return null;
        String[] p = v.split(",");
        float[] f = new float[n];
        for (int i = 0; i < n; i++) f[i] = Float.parseFloat(p[i]);
        return f;
    }

    static String luzRotulo() {
        return luz == LUZ_OFF ? "LUZ OFF" : "LUZ ON";
    }

    private TesteDesempenho() {}
}
