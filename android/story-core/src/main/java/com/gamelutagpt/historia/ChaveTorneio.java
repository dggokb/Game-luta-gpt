package com.gamelutagpt.historia;

import com.gamelutagpt.render.RenderCanvas;

/**
 * Desenha a chave do torneio (passos "sorteio" e "chave") em caixas de confronto. Cada lado
 * (Dragão à esquerda, Tigre à direita, espelhados): as duas quartas, a semifinal e a
 * repescagem, a final da chave. No meio, a FINAL e, em cima dela, os campeões.
 *
 * A dupla do jogador tem borda azul, a do rival vermelha; quem venceu ganha a faixa dourada,
 * quem perdeu fica apagado e riscado. Vencedor segue pela linha dourada; perdedor das quartas
 * desce pela linha vermelha até a repescagem.
 *
 * No sorteio, as duplas dos irmãos aparecem nas pontas e as outras caem uma a uma nas vagas,
 * na ordem sorteada. Na chave, os confrontos da rodada nova aparecem por último.
 */
final class ChaveTorneio {
    /** Textos da chave que passam pela tradução (mais os nomes das rodadas, em {@link Torneio}). */
    static final String[] TEXTOS = {"O SORTEIO", "CHAVE DO TORNEIO", "CAMPEÕES", "PRÓXIMA LUTA",
        "LADO DO DRAGÃO", "LADO DO TIGRE",
        "Os irmãos ficam em lados opostos: só se encontram na final.", "Fim do torneio para a sua dupla."};

    static final float VW = PaginaHistoria.VW;
    static final float VH = PaginaHistoria.VH;

    static final float BOX_W = 150f;
    static final float HEADER = 20f;
    static final float ROW_H = 54f;
    static final float BOX_H = HEADER + 2f * ROW_H + 4f;
    /** Colunas do lado esquerdo: quartas, semifinal/repescagem, final da chave (o direito é o espelho). */
    private static final float[] COLUNAS = {10f, 200f, 390f};
    private static final float TOPO_CIMA = 140f, TOPO_BAIXO = 450f, TOPO_MEIO = 295f;
    private static final float CENTRO_X = (VW - BOX_W) * 0.5f;
    private static final float CAMPEAO_Y = 140f;

    /** Animação do sorteio: as pontas, depois uma dupla por vez. */
    static final float SORTEIO_INICIO = 0.9f;
    static final float SORTEIO_PASSO = 0.45f;
    static final float SLAM = 0.18f;
    /** Rodada nova da chave: um confronto a cada tanto. */
    static final float REVELA_PASSO = 0.2f;
    static final float REVELA_INICIO = 0.3f;

    private static final int BLUE = 0xFF4FA3FF;
    private static final int RED = 0xFFE5404C;
    private static final int GOLD = 0xFFF2C14E;
    private static final int GRAY = 0xFF6E7689;
    private static final int CARD = 0xEE141724;
    private static final int TEXT = 0xFFE9E1CF;

    private final Rostos rostos;

    ChaveTorneio(Rostos rostos) {
        this.rostos = rostos;
    }

    /** Quanto dura a animação do sorteio (as 6 duplas sorteadas). */
    static float duracaoSorteio() {
        return SORTEIO_INICIO + (Torneio.VAGAS - 2) * SORTEIO_PASSO + SLAM;
    }

    /** Quanto dura a revelação da rodada nova. */
    static float duracaoRevelacao(Torneio t) {
        if (t.disputadas() == 0) return 0f;
        return REVELA_INICIO + t.rodada(Math.min(t.disputadas(), Torneio.RODADAS - 1)).size() * REVELA_PASSO + SLAM;
    }

    /** Topo esquerdo da caixa: rodada, lado e nome do confronto (índice da quarta em {@code quarta}). */
    static float[] caixa(int rodada, int lado, String nome, int quarta) {
        if (lado < 0) return new float[] {CENTRO_X, TOPO_MEIO};
        float x = COLUNAS[rodada == 0 ? 0 : rodada == 1 ? 1 : 2];
        if (lado == 1) x = VW - x - BOX_W;
        float y;
        if (rodada == 0) y = quarta % 2 == 0 ? TOPO_CIMA : TOPO_BAIXO;
        else if (rodada == 1) y = Torneio.SEMIFINAL.equals(nome) ? TOPO_CIMA : TOPO_BAIXO;
        else y = TOPO_MEIO;
        return new float[] {x, y};
    }

    /**
     * @param sorteio true: animação do sorteio (só as quartas); false: a chave com os resultados
     * @param time segundos desde que o passo começou
     */
    void draw(RenderCanvas c, Temporada temporada, Torneio t, boolean sorteio, float time) {
        c.fillVerticalGradient(-40f, -40f, VW + 40f, VH + 40f, 0xFF10131F, 0xFF050508);
        c.fillRadialGlow(120f, 380f, 420f, 0x332F6FD6);
        c.fillRadialGlow(VW - 120f, 380f, 420f, 0x33C62F2F);

        // Título numa pincelada vermelha.
        float in = Tinta.easeOutCubic(time / 0.35f);
        float[] band = {VW * 0.5f - 300f * in, 30f, VW * 0.5f + 300f * in, 18f, VW * 0.5f + 300f * in, 80f, VW * 0.5f - 300f * in, 92f};
        c.fillPolygon(band, 0xFFC8202C);
        String titulo = temporada.t(sorteio ? "O SORTEIO" : "CHAVE DO TORNEIO");
        c.drawText(titulo, VW * 0.5f, 72f, BotaoMenu.fit(c, titulo, 44f, 560f, true), 0xFFFFFFFF, 0xFF0A0A0A, 6f, true);
        c.drawText(temporada.t("LADO DO DRAGÃO"), 275f, 122f, 18f, BLUE, 0xFF0A0A0A, 3f, true);
        c.drawText(temporada.t("LADO DO TIGRE"), VW - 275f, 122f, 18f, RED, 0xFF0A0A0A, 3f, true);

        int nova = sorteio ? -1 : Math.min(t.disputadas(), Torneio.RODADAS - 1);
        drawLinhas(c, t, sorteio);

        // Quartas (no sorteio, cada vaga na sua hora).
        for (int m = 0; m < Torneio.VAGAS / 2; m++) {
            Torneio.Confronto q = t.rodada(0).get(m);
            float[] p = caixa(0, q.lado, q.nome, m);
            float[] idade = new float[2];
            for (int k = 0; k < 2; k++) idade[k] = sorteio ? time - horaDaVaga(t, 2 * m + k) : (nova == 0 ? time - revela(m) : 99f);
            drawCaixa(c, temporada, t, q, q.nome, p[0], p[1], idade, sorteio);
        }
        // Rodadas seguintes: as já montadas com as duplas; as outras vazias.
        drawRodada(c, temporada, t, 1, sorteio, nova, time);
        drawRodada(c, temporada, t, 2, sorteio, nova, time);
        drawRodada(c, temporada, t, 3, sorteio, nova, time);

        Torneio.Dupla campeao = sorteio ? null : t.campeao();
        if (campeao != null) {
            float[] caixa = {CENTRO_X, CAMPEAO_Y, CENTRO_X + BOX_W, CAMPEAO_Y, CENTRO_X + BOX_W, CAMPEAO_Y + HEADER + ROW_H + 4f,
                CENTRO_X, CAMPEAO_Y + HEADER + ROW_H + 4f};
            c.fillPolygon(caixa, CARD);
            c.fillRect(CENTRO_X, CAMPEAO_Y, CENTRO_X + BOX_W, CAMPEAO_Y + HEADER, GOLD);
            c.drawText(temporada.t("CAMPEÕES"), CENTRO_X + BOX_W * 0.5f, CAMPEAO_Y + 15f, 14f, 0xFF1A1206, 0, 0f, true);
            drawLinha(c, temporada, t, campeao, CENTRO_X, CAMPEAO_Y + HEADER, 255, 1f, true);
            c.strokePolygon(caixa, 3f, GOLD);
        }
        drawRodape(c, temporada, t, sorteio, time);
    }

    private float revela(int i) {
        return REVELA_INICIO + i * REVELA_PASSO;
    }

    private void drawRodada(RenderCanvas c, Temporada temporada, Torneio t, int r, boolean sorteio, int nova, float time) {
        String[] nomes = r == 1 ? new String[] {Torneio.SEMIFINAL, Torneio.REPESCAGEM}
            : r == 2 ? new String[] {Torneio.FINAL_DA_CHAVE} : new String[] {Torneio.FINAL};
        int[] lados = r == 3 ? new int[] {-1} : new int[] {0, 1};
        int i = 0;
        for (int lado : lados) {
            for (String nome : nomes) {
                Torneio.Confronto conf = sorteio ? null : t.confronto(r, lado, nome);
                float[] p = caixa(r, lado, nome, 0);
                float idade = r == nova ? time - revela(i) : 99f;
                drawCaixa(c, temporada, t, conf, nome, p[0], p[1], new float[] {idade, idade}, false);
                i++;
            }
        }
    }

    /** Quando a vaga aparece no sorteio: as pontas logo, as outras na ordem sorteada. */
    private static float horaDaVaga(Torneio t, int vaga) {
        if (vaga == 0 || vaga == Torneio.VAGAS - 1) return 0.25f;
        int[] ordem = t.ordemSorteio();
        for (int k = 0; k < ordem.length; k++) {
            if (ordem[k] == vaga) return SORTEIO_INICIO + k * SORTEIO_PASSO;
        }
        return SORTEIO_INICIO;
    }

    /**
     * Caixa de um confronto (null = ainda não montado: "?" nas duas linhas).
     *
     * @param idade segundos desde que cada linha apareceu (negativo = ainda "?")
     * @param sorteio no sorteio não há resultado
     */
    private void drawCaixa(RenderCanvas c, Temporada temporada, Torneio t, Torneio.Confronto conf, String nome,
                           float l, float top, float[] idade, boolean sorteio) {
        float[] caixa = {l, top, l + BOX_W, top, l + BOX_W, top + BOX_H, l, top + BOX_H};
        boolean montado = conf != null;
        c.fillPolygon(new float[] {l + 6f, top + 7f, l + BOX_W + 6f, top + 7f, l + BOX_W + 6f, top + BOX_H + 7f, l + 6f, top + BOX_H + 7f}, 0x66000000);
        c.fillPolygon(caixa, montado ? CARD : 0x99101320);
        int corTopo = Torneio.REPESCAGEM.equals(nome) ? 0xFF7A2A20 : Torneio.FINAL.equals(nome) || Torneio.FINAL_DA_CHAVE.equals(nome) ? 0xFF8A6A1A : 0xFF2A3044;
        c.fillRect(l, top, l + BOX_W, top + HEADER, corTopo);
        String rotulo = temporada.t(nome);
        c.drawText(rotulo, l + BOX_W * 0.5f, top + 15f, BotaoMenu.fit(c, rotulo, 13f, BOX_W - 8f, true), TEXT, 0, 0f, true);
        for (int k = 0; k < 2; k++) {
            float y = top + HEADER + k * ROW_H;
            Torneio.Dupla d = montado && idade[k] >= 0f ? (k == 0 ? conf.a : conf.b) : null;
            if (d == null) {
                c.drawText("?", l + BOX_W * 0.5f, y + ROW_H * 0.72f, 30f, 0x885A6275, 0, 0f, true);
                continue;
            }
            Torneio.Dupla vencedor = sorteio ? null : conf.vencedor();
            boolean perdeu = vencedor != null && vencedor != d;
            int alpha = perdeu ? 100 : 255;
            float pop = Tinta.clamp01(idade[k] / SLAM);
            float scale = 1.4f - 0.4f * Tinta.easeOutCubic(pop);
            c.save();
            c.scale(scale, scale, l + BOX_W * 0.5f, y + ROW_H * 0.5f);
            drawLinha(c, temporada, t, d, l, y, alpha, pop, vencedor == d);
            if (perdeu) c.drawLine(l + 8f, y + ROW_H - 6f, l + BOX_W - 8f, y + 6f, 4f, 0xC0E5404C);
            c.restore();
        }
        c.strokePolygon(caixa, 2f, montado ? 0xFF3A4258 : 0x885A6275);
    }

    /** Uma dupla dentro da caixa: os dois rostos e os nomes. */
    private void drawLinha(RenderCanvas c, Temporada temporada, Torneio t, Torneio.Dupla d, float l, float y,
                           int alpha, float pop, boolean venceu) {
        float[] linha = {l + 2f, y + 2f, l + BOX_W - 2f, y + 2f, l + BOX_W - 2f, y + ROW_H - 2f, l + 2f, y + ROW_H - 2f};
        if (venceu) c.fillPolygon(linha, 0x33F2C14E);
        rostos.draw(c, d.lutadores[0], l + 4f, y + 5f, 42f, 0xFF0A0A0A, 1.5f, alpha);
        rostos.draw(c, d.lutadores[1], l + 48f, y + 5f, 42f, 0xFF0A0A0A, 1.5f, alpha);
        float nx = l + 92f + (BOX_W - 96f) * 0.5f;
        String n1 = Rostos.nome(temporada, d.lutadores[0]);
        String n2 = Rostos.nome(temporada, d.lutadores[1]);
        c.drawText(n1, nx, y + 24f, BotaoMenu.fit(c, n1, 13f, BOX_W - 98f, false), Tinta.withAlpha(TEXT, alpha), 0, 0f, false);
        c.drawText(n2, nx, y + 42f, BotaoMenu.fit(c, n2, 13f, BOX_W - 98f, false), Tinta.withAlpha(TEXT, alpha), 0, 0f, false);
        int borda = d == t.jogador() ? BLUE : d == t.rival() ? RED : venceu ? GOLD : 0;
        if (borda != 0) c.strokePolygon(linha, d == t.jogador() || d == t.rival() ? 3f : 2f, Tinta.withAlpha(borda, alpha));
        if (pop < 1f) c.fillPolygon(linha, Tinta.withAlpha(0xFFFFFFFF, (int)(200f * (1f - pop))));
    }

    /** Linhas entre as caixas: dourada para quem venceu, vermelha para quem caiu na repescagem. */
    private void drawLinhas(RenderCanvas c, Torneio t, boolean sorteio) {
        for (int lado = 0; lado < 2; lado++) {
            boolean esq = lado == 0;
            Torneio.Confronto semi = sorteio ? null : t.confronto(1, lado, Torneio.SEMIFINAL);
            Torneio.Confronto rep = sorteio ? null : t.confronto(1, lado, Torneio.REPESCAGEM);
            Torneio.Confronto fc = sorteio ? null : t.confronto(2, lado, Torneio.FINAL_DA_CHAVE);
            for (int m = 0; m < 2; m++) {
                float[] q = caixa(0, lado, Torneio.QUARTAS, m);
                boolean decidido = semi != null;
                // Vencedor das quartas sobe para a semifinal, perdedor desce para a repescagem.
                ligar(c, esq, q, 0.36f, caixa(1, lado, Torneio.SEMIFINAL, 0), m == 0 ? 0.33f : 0.67f, 26f,
                    decidido ? GOLD : 0x996E7689, decidido ? 3.5f : 2f);
                ligar(c, esq, q, 0.80f, caixa(1, lado, Torneio.REPESCAGEM, 0), m == 0 ? 0.33f : 0.67f, 14f,
                    decidido ? 0xCCE5404C : 0x666E7689, 2f);
            }
            ligar(c, esq, caixa(1, lado, Torneio.SEMIFINAL, 0), 0.5f, caixa(2, lado, Torneio.FINAL_DA_CHAVE, 0), 0.36f, 20f,
                fc != null ? GOLD : 0x996E7689, fc != null ? 3.5f : 2f);
            ligar(c, esq, caixa(1, lado, Torneio.REPESCAGEM, 0), 0.5f, caixa(2, lado, Torneio.FINAL_DA_CHAVE, 0), 0.80f, 20f,
                fc != null ? GOLD : 0x996E7689, fc != null ? 3.5f : 2f);
            Torneio.Confronto fin = sorteio ? null : t.confronto(3, -1, Torneio.FINAL);
            ligar(c, esq, caixa(2, lado, Torneio.FINAL_DA_CHAVE, 0), 0.5f, caixa(3, -1, Torneio.FINAL, 0), esq ? 0.36f : 0.80f, 12f,
                fin != null ? GOLD : 0x996E7689, fin != null ? 3.5f : 2f);
        }
        if (!sorteio && t.campeao() != null) {
            c.drawLine(VW * 0.5f, TOPO_MEIO, VW * 0.5f, CAMPEAO_Y + HEADER + ROW_H + 4f, 3.5f, GOLD);
        }
    }

    /**
     * Linha em cotovelo da caixa {@code de} (saindo pela lateral interna, na altura {@code fy}
     * da caixa) até a caixa {@code para} (entrando pela lateral externa, na altura {@code py}).
     */
    private static void ligar(RenderCanvas c, boolean esq, float[] de, float fy, float[] para, float py,
                              float cotovelo, int cor, float largura) {
        float x1 = esq ? de[0] + BOX_W : de[0];
        float y1 = de[1] + BOX_H * fy;
        float x2 = esq ? para[0] : para[0] + BOX_W;
        float y2 = para[1] + BOX_H * py;
        float mx = esq ? x1 + cotovelo : x1 - cotovelo;
        c.drawLine(x1, y1, mx, y1, largura, cor);
        c.drawLine(mx, y1, mx, y2, largura, cor);
        c.drawLine(mx, y2, x2, y2, largura, cor);
    }

    private void drawRodape(RenderCanvas c, Temporada temporada, Torneio t, boolean sorteio, float time) {
        float y = 668f;
        if (sorteio) {
            if (time < duracaoSorteio()) return;
            float a = Tinta.clamp01((time - duracaoSorteio()) / 0.3f);
            String texto = temporada.t("Os irmãos ficam em lados opostos: só se encontram na final.");
            c.drawText(texto, VW * 0.5f, y, BotaoMenu.fit(c, texto, 24f, 1100f, false), Tinta.withAlpha(TEXT, (int)(255 * a)), 0, 0f, false);
            return;
        }
        String texto;
        int cor = TEXT;
        Torneio.Dupla adv = t.adversario();
        if (adv != null) {
            texto = temporada.t("PRÓXIMA LUTA") + " — " + temporada.t(t.rodadaAtual()) + ":  " +
                Rostos.nomeDupla(temporada, t.jogador()) + "  ×  " + Rostos.nomeDupla(temporada, adv);
        } else if (t.campeao() != null) {
            texto = temporada.t("CAMPEÕES") + ":  " + Rostos.nomeDupla(temporada, t.campeao());
            cor = GOLD;
        } else {
            texto = temporada.t("Fim do torneio para a sua dupla.");
        }
        c.drawText(texto, VW * 0.5f, y, BotaoMenu.fit(c, texto, 24f, 1180f, true), cor, 0xFF0A0A0A, 3f, true);
    }
}
