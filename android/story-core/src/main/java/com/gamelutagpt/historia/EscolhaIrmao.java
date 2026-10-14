package com.gamelutagpt.historia;

import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;

/**
 * Passo "escolha": depois do Prólogo o jogador escolhe o caminho, Ryūga (o Dragão, azul, à
 * esquerda) ou Kōga (o Tigre, vermelho, à direita). Cada lado mostra o capitão, o parceiro
 * fixo e a metade do logo com o animal dele. Toque escolhe o lado tocado; controle: ◀▶ e A.
 */
final class EscolhaIrmao {
    static final String TITULO = "ESCOLHA SEU CAMINHO";
    static final String DICA = "Toque num irmão  •  Controle: ◀▶ e A";
    static final String RYUGA_SUB = "O Dragão. Busca o pai e a verdade.";
    static final String KOGA_SUB = "O Tigre. Busca vingança.";
    static final String[] TEXTOS = {TITULO, DICA, RYUGA_SUB, KOGA_SUB};

    /** Depois da escolha, o lado pisca até o passo sair. */
    static final float FLASH = 0.5f;
    /** O toque que virou a página anterior não pode escolher. */
    static final float TAP_DELAY = 0.4f;

    private static final float VW = PaginaHistoria.VW;
    private static final float VH = PaginaHistoria.VH;
    private static final int BLUE = 0xFF2F6FD6;
    private static final int RED = 0xFFC62F2F;

    private final Rostos rostos;
    private int cursor;
    private int escolhido = -1;
    private float time;
    private float escolhidoAt;

    EscolhaIrmao(Rostos rostos) {
        this.rostos = rostos;
    }

    void begin() {
        cursor = 0;
        escolhido = -1;
        time = 0f;
    }

    void update(float dt) {
        time += dt;
    }

    /** O irmão escolhido, ou null. */
    String escolha() {
        return escolhido < 0 ? null : escolhido == 0 ? Torneio.RYUGA : Torneio.KOGA;
    }

    /** Já escolheu e o lado terminou de piscar. */
    boolean terminou() {
        return escolhido >= 0 && time - escolhidoAt >= FLASH;
    }

    void mover(int direcao) {
        if (escolhido < 0 && direcao != 0) cursor = direcao < 0 ? 0 : 1;
    }

    void confirmar() {
        if (escolhido >= 0 || time < TAP_DELAY) return;
        escolhido = cursor;
        escolhidoAt = time;
    }

    void tap(float x, float y) {
        if (escolhido >= 0 || time < TAP_DELAY) return;
        cursor = x < divisa(y) ? 0 : 1;
        confirmar();
    }

    /** A divisa diagonal entre os lados, na altura y. */
    private static float divisa(float y) {
        return 700f - 120f * (y / VH);
    }

    void draw(RenderCanvas c, Temporada temporada, Campanha campanha, RenderImage logo) {
        c.fillRect(-40f, -40f, VW + 40f, VH + 40f, 0xFF000000);
        float[] esq = {-40f, -40f, divisa(-40f), -40f, divisa(VH + 40f), VH + 40f, -40f, VH + 40f};
        float[] dir = {divisa(-40f), -40f, VW + 40f, -40f, VW + 40f, VH + 40f, divisa(VH + 40f), VH + 40f};
        for (int lado = 0; lado < 2; lado++) {
            float[] area = lado == 0 ? esq : dir;
            int cor = lado == 0 ? BLUE : RED;
            c.save();
            c.clipPolygon(area);
            c.fillVerticalGradient(-40f, -40f, VW + 40f, VH + 40f, Tinta.darken(cor, 0.55f), 0xFF050508);
            if (logo != null) {
                // Metade do logo: o dragão à esquerda, o tigre à direita.
                float h = 620f;
                float w = h * (logo.width() * 0.5f) / logo.height();
                float x = lado == 0 ? 40f : VW - 40f - w;
                if (lado == 0) {
                    c.drawImageRegion(logo, 0f, 0f, logo.width() * 0.5f, logo.height(), x, 60f, x + w, 60f + h, 70);
                } else {
                    c.drawImageRegion(logo, logo.width() * 0.5f, 0f, logo.width(), logo.height(), x, 60f, x + w, 60f + h, 70);
                }
            }
            drawLado(c, temporada, campanha, lado);
            boolean apagado = (escolhido >= 0 ? escolhido : cursor) != lado;
            if (apagado) c.fillRect(-40f, -40f, VW + 40f, VH + 40f, 0x8C000000);
            if (escolhido == lado) {
                float flash = 1f - Tinta.clamp01((time - escolhidoAt) / FLASH);
                if (flash > 0f) c.fillRect(-40f, -40f, VW + 40f, VH + 40f, Tinta.withAlpha(0xFFFFFFFF, (int)(170f * flash)));
            }
            c.restore();
        }
        c.drawLine(divisa(-40f), -40f, divisa(VH + 40f), VH + 40f, 6f, 0xFFFFFFFF);

        float in = Tinta.easeOutCubic(time / 0.35f);
        String titulo = temporada.t(TITULO);
        float size = BotaoMenu.fit(c, titulo, 46f, 900f, true);
        c.save();
        c.scale(1.3f - 0.3f * in, 1.3f - 0.3f * in, VW * 0.5f, 62f);
        c.drawText(titulo, VW * 0.5f, 78f, size, 0xFFFFFFFF, 0xFF0A0A0A, size * 0.14f, true);
        c.restore();
        if (time > 0.8f) {
            String dica = temporada.t(DICA);
            c.drawText(dica, VW * 0.5f, VH - 18f, BotaoMenu.fit(c, dica, 18f, 900f, false), 0xFFB9C0D0, 0xFF000000, 3f, false);
        }
    }

    private void drawLado(RenderCanvas c, Temporada temporada, Campanha campanha, int lado) {
        String irmao = lado == 0 ? Torneio.RYUGA : Torneio.KOGA;
        Torneio.Dupla dupla = campanha != null ? campanha.duplaDo(irmao) : null;
        String capitao = dupla != null ? dupla.lutadores[0] : (lado == 0 ? "player_base" : "p03");
        String parceiro = dupla != null ? dupla.lutadores[1] : (lado == 0 ? "p02" : "p04");
        boolean ativo = (escolhido >= 0 ? escolhido : cursor) == lado;
        float cx = lado == 0 ? 330f : 950f;
        float bob = ativo ? (float)Math.sin(time * 3f) * 4f : 0f;
        int cor = lado == 0 ? BLUE : RED;
        rostos.draw(c, capitao, cx - 130f, 150f + bob, 240f, ativo ? 0xFFFFFFFF : cor, ativo ? 6f : 3f, 255);
        rostos.draw(c, parceiro, cx + 70f, 300f + bob, 120f, cor, 4f, 255);
        Temporada.Personagem p = temporada.personagem(irmao);
        String nome = p.nome.toUpperCase(java.util.Locale.ROOT);
        c.drawText(nome, cx, 470f, BotaoMenu.fit(c, nome, 76f, 500f, true), 0xFFFFFFFF, Tinta.darken(cor, 0.5f), 9f, true);
        String sub = temporada.t(lado == 0 ? RYUGA_SUB : KOGA_SUB);
        c.drawText(sub, cx, 515f, BotaoMenu.fit(c, sub, 24f, 520f, false), 0xFFE9E1CF, 0xFF000000, 3f, false);
        String duplaTxt = Rostos.nome(temporada, capitao) + " + " + Rostos.nome(temporada, parceiro);
        c.drawText(duplaTxt, cx, 560f, 22f, Tinta.withAlpha(0xFFFFFFFF, 210), 0xFF000000, 3f, true);
    }
}
