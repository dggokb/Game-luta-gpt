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
     * A luz do impacto no corpo dos lutadores (motores de luz 3 e 4, só no OpenGL), para o dono
     * comparar no aparelho (botão LUZ do Modo Teste): desligada (como era antes); com a normal da
     * silhueta (só a borda do corpo sabe para onde aponta); com o normal map da folha (o miolo
     * ganha volume; lutador sem mapa usa a silhueta); e o normal map com a luz em dois tons
     * chapados. No PC, {@code -Dgameluta.luz=off|silhueta|normal|faixas} ({@code -Pluz=...}).
     */
    static final int LUZ_OFF = 0, LUZ_SILHUETA = 1, LUZ_NORMAL = 2, LUZ_FAIXAS = 3;
    static volatile int luz = luzInicial();

    private static int luzInicial() {
        String v = System.getProperty("gameluta.luz", "normal");
        switch (v) {
            case "off": return LUZ_OFF;
            case "silhueta": return LUZ_SILHUETA;
            case "faixas": return LUZ_FAIXAS;
            default: return LUZ_NORMAL;
        }
    }

    static String luzRotulo() {
        switch (luz) {
            case LUZ_OFF: return "LUZ OFF";
            case LUZ_SILHUETA: return "LUZ SILH.";
            case LUZ_FAIXAS: return "LUZ FAIXAS";
            default: return "LUZ NORMAL";
        }
    }

    private TesteDesempenho() {}
}
