package com.gamelutagpt.historia;

import java.util.List;

/**
 * Nível da CPU nas lutas do modo história e do Arcade: começa no NÍVEL CPU da pausa (o padrão é
 * o 5) na primeira luta e sobe até o 8 na última, repartido pelas lutas que a campanha tem. Quem
 * tem menos lutas sobe mais rápido por luta; quem tem mais, mais devagar.
 *
 * <p>Contam as lutas contra a CPU que valem para o irmão escolhido: o treino-tutorial do Prólogo
 * (sem IA) fica de fora, e nos episódios com uma luta para cada irmão só a dele conta.
 */
public final class NivelCpu {
    /** O nível da última luta da campanha. */
    public static final int TOPO = MenuPausa.NIVEL_MAX;

    private NivelCpu() {}

    /**
     * Nível da luta {@code feitas} (0 = a primeira) de {@code total}: de {@code base} na primeira
     * até {@link #TOPO} na última, arredondando para baixo no meio do caminho.
     */
    public static int nivel(int base, int feitas, int total) {
        base = Math.max(MenuPausa.NIVEL_MIN, Math.min(TOPO, base));
        if (total <= 1 || base >= TOPO) return base;
        feitas = Math.max(0, Math.min(total - 1, feitas));
        return base + (TOPO - base) * feitas / (total - 1);
    }

    /** O passo é uma luta contra a CPU que conta para quem escolheu {@code irmao} (null = todas). */
    public static boolean conta(Episodio.Passo p, String irmao) {
        return p.tipo == Episodio.TipoPasso.LUTA && !p.tutorial() &&
            (p.irmao == null || irmao == null || p.irmao.equals(irmao));
    }

    /** Quantas lutas contam no episódio. */
    public static int lutas(Episodio ep, String irmao) {
        int n = 0;
        for (Episodio.Passo p : ep.passos) if (conta(p, irmao)) n++;
        return n;
    }

    /**
     * Onde está a luta {@code indicePasso} do episódio {@code episodioId} na campanha
     * ({@code episodios}, na ordem): {feitas antes dela, total}. Null se o episódio não está na
     * lista ou o passo não conta.
     */
    public static int[] posicao(List<Episodio> episodios, String episodioId, int indicePasso, String irmao) {
        int total = 0;
        int feitas = -1;
        for (Episodio ep : episodios) {
            for (int i = 0; i < ep.passos.size(); i++) {
                if (!conta(ep.passos.get(i), irmao)) continue;
                if (ep.id.equals(episodioId) && i == indicePasso) feitas = total;
                total++;
            }
        }
        return feitas < 0 ? null : new int[] {feitas, total};
    }
}
