package com.gamelutagpt;

/**
 * "Ryūga! Troca comigo!": quando a vida de quem está lutando entra no vermelho da barra, o
 * parceiro que espera fora grita o nome dele e pede a troca, no quadro de mangá da conversa
 * (como Takatoshi, Dafu, Ryūga e Kōga no treino e no episódio 1). Vale para os dois lados e
 * toda vez que a vida cruza a linha — não quando o titular já entra no vermelho pela troca, nem
 * no golpe que derruba, nem sem parceiro de pé.
 */
final class GritoDaTroca {
    /** A linha do vermelho na barra de vida (por mil da vida máxima), a mesma do HUD. */
    static final int VERMELHO = 250;
    /** O grito, com o nome de quem está lutando (o texto em português é a chave da tradução). */
    static final String COM_NOME = "{nome}! Troca comigo!";
    /** Sem nome na história para quem está lutando (ainda): só o pedido. */
    static final String SEM_NOME = "Troca comigo!";
    /** Segundos na tela depois de digitado (é um grito, curto). */
    static final float SEGURA = 1.2f;

    private final FighterState[] ponto = new FighterState[2];
    private final int[] vida = new int[2];

    /** Luta nova: ninguém visto ainda. */
    void reset() {
        ponto[0] = ponto[1] = null;
    }

    /**
     * Um quadro, depois do passo do motor. Devolve os lados cujo parceiro grita agora
     * (bit 0 = jogador, bit 1 = adversário).
     */
    int update(CombatEngine engine) {
        int gritos = 0;
        for (int s = 0; s < 2; s++) {
            TeamSystem.Side side = engine.team(s);
            FighterState p = side.pointState();
            int v = permille(p);
            boolean cruzou = p == ponto[s] && vida[s] > VERMELHO && v <= VERMELHO && v > 0;
            if (cruzou && side.partnerStanding() && !side.tagging() && !engine.fighter(s).ko()) gritos |= 1 << s;
            ponto[s] = p;
            vida[s] = v;
        }
        return gritos;
    }

    /** O texto do grito: com o nome de quem está lutando, ou só o pedido. */
    static String texto(String nome, java.util.function.UnaryOperator<String> traduz) {
        if (nome == null || nome.isEmpty()) return traduz.apply(SEM_NOME);
        return traduz.apply(COM_NOME).replace("{nome}", nome);
    }

    static int permille(FighterState s) {
        return s.profile.maxLife <= 0 ? 0 : (int)(s.life * 1000L / s.profile.maxLife);
    }
}
