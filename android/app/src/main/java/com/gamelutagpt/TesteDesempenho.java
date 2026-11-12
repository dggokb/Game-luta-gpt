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
     * O normal map dos lutadores na luz (motor de luz 4, só no OpenGL): a luz do cenário com
     * volume, os pontos de luz do cenário (braseiros, lanternas) e a luz do impacto. O botão LUZ do
     * Modo Teste liga e desliga, para comparar no aparelho. Lutador sem mapa fica como era. No PC,
     * {@code -Dgameluta.luz=off} ({@code -Pluz=off}) começa desligado.
     */
    static final int LUZ_OFF = 0, LUZ_SUAVE = 1, LUZ_NOVA = 2;
    static volatile int luz = "off".equals(System.getProperty("gameluta.luz")) ? LUZ_OFF
        : "nova".equals(System.getProperty("gameluta.luz")) ? LUZ_NOVA : LUZ_SUAVE;

    /**
     * Motor de luz e sombra novo (docs/motor-luz-proposta.md, protótipo de 12/11): a arte é o
     * teto. O cenário diz a cor que o branco vira no lado aceso e no lado de sombra, e o shader
     * multiplica a arte por isso; nada soma. Os números da rampa entre sombra e luz: o peso do
     * degradê que atravessa o quadro do lutador na direção da luz, o peso da inclinação do normal
     * map (só quem tem mapa), a largura macia da passagem (0 a 0,5) e o quanto a cor de um ponto
     * de luz do cenário é lavada para o branco. No PC, {@code -Dgameluta.nova=a,b,c,d}.
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

    /**
     * A luz do cenário com volume: quanto a inclinação da normal é exagerada (o mapa guarda só as
     * formas grandes, quase de frente: sem exagero a luz mal muda), a força da sombra do lado
     * oposto à luz e a força da luz do lado dela. É o nível que o dono aprovou no aparelho (12/11:
     * "o leve ficou bom"; o forte, {3,2; 0,75; 0,85}, "ficou ruim" e saiu).
     */
    private static final float[] VOLUME = {2.3f, 0.5f, 0.55f};

    /** {exagero da normal, sombra, luz}, ou null com a luz desligada. */
    static float[] volume() {
        if (luz != LUZ_SUAVE) return null;
        return VOLUME_DO_PC != null ? VOLUME_DO_PC : VOLUME;
    }

    /** Só para afinar no PC: {@code -Dgameluta.volume=exagero,sombra,luz} ({@code -Pvolume=...}). */
    private static final float[] VOLUME_DO_PC = volumeDoPc();

    private static float[] volumeDoPc() {
        String v = System.getProperty("gameluta.volume");
        if (v == null) return null;
        String[] p = v.split(",");
        return new float[] {Float.parseFloat(p[0]), Float.parseFloat(p[1]), Float.parseFloat(p[2])};
    }

    static String luzRotulo() {
        return luz == LUZ_OFF ? "LUZ OFF" : luz == LUZ_NOVA ? "LUZ NOVA" : "LUZ ON";
    }

    private TesteDesempenho() {}
}
