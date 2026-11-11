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
     * O normal map dos lutadores na luz (motor de luz 4, só no OpenGL), para o dono comparar no
     * aparelho (botão LUZ do Modo Teste): desligado (como era antes do motor 4); a luz do cenário
     * com volume, suave; e forte. Ligado, a luz do impacto também acende o corpo pelo mapa.
     * Lutador sem mapa fica como era. No PC, {@code -Dgameluta.luz=off|suave|forte} ({@code -Pluz=...}; sem nada, forte).
     */
    static final int LUZ_OFF = 0, LUZ_SUAVE = 1, LUZ_FORTE = 2;
    static volatile int luz = luzInicial();

    /**
     * A luz do cenário com volume em cada nível: quanto a inclinação da normal é exagerada (o
     * mapa guarda só as formas grandes, quase de frente: sem exagero a luz mal muda), a força da
     * sombra do lado oposto à luz e a força da luz do lado dela.
     */
    private static final float[] VOLUME_SUAVE = {2.3f, 0.5f, 0.55f}, VOLUME_FORTE = {3.2f, 0.75f, 0.85f};

    /** {exagero da normal, sombra, luz} do nível de agora, ou null com a luz desligada. */
    static float[] volume() {
        int l = luz;
        if (l == LUZ_OFF) return null;
        if (VOLUME_DO_PC != null) return VOLUME_DO_PC;
        return l == LUZ_SUAVE ? VOLUME_SUAVE : VOLUME_FORTE;
    }

    /** Só para afinar no PC: {@code -Dgameluta.volume=exagero,sombra,luz} ({@code -Pvolume=...}). */
    private static final float[] VOLUME_DO_PC = volumeDoPc();

    private static float[] volumeDoPc() {
        String v = System.getProperty("gameluta.volume");
        if (v == null) return null;
        String[] p = v.split(",");
        return new float[] {Float.parseFloat(p[0]), Float.parseFloat(p[1]), Float.parseFloat(p[2])};
    }

    private static int luzInicial() {
        // Começa em FORTE: é onde a diferença se vê de primeira (o dono não viu nenhuma com a luz
        // só no impacto); o botão desce para SUAVE e OFF.
        String v = System.getProperty("gameluta.luz", "forte");
        switch (v) {
            case "off": return LUZ_OFF;
            case "suave": return LUZ_SUAVE;
            default: return LUZ_FORTE;
        }
    }

    static String luzRotulo() {
        switch (luz) {
            case LUZ_OFF: return "LUZ OFF";
            case LUZ_FORTE: return "LUZ FORTE";
            default: return "LUZ SUAVE";
        }
    }

    private TesteDesempenho() {}
}
