package com.gamelutagpt.historia;

import com.gamelutagpt.render.RenderCanvas;

/**
 * Desenha a chave do torneio (passos "sorteio" e "chave"): as 8 duplas nas quartas dos dois
 * lados, as semifinais, a final no meio e o campeão em cima. A dupla do jogador tem borda
 * azul, a do rival vermelha; quem perdeu fica apagado e riscado.
 *
 * No sorteio, as duplas dos irmãos aparecem nas pontas e as outras caem uma a uma nas vagas,
 * na ordem sorteada. Na chave, a rodada que acabou de ser decidida aparece por último.
 */
final class ChaveTorneio {
    /** Textos da chave que passam pela tradução (mais os nomes das rodadas, em {@link Torneio}). */
    static final String[] TEXTOS = {"O SORTEIO", "CHAVE DO TORNEIO", "FINAL", "CAMPEÕES", "PRÓXIMA LUTA",
        "Os irmãos ficam em lados opostos: só se encontram na final.", "Fim do torneio para a sua dupla."};

    static final float VW = PaginaHistoria.VW;
    static final float VH = PaginaHistoria.VH;

    static final float CARD_W = 170f, CARD_H = 100f;
    /** Colunas: quartas, semifinal e finalista de cada lado. */
    private static final float[] COL_ESQ = {30f, 240f, 450f};
    private static final float[] COL_DIR = {1080f, 870f, 660f};
    private static final float[] TOPO_QUARTAS = {120f, 230f, 400f, 510f};
    private static final float[] TOPO_SEMI = {175f, 455f};
    private static final float TOPO_FINAL = 315f;
    private static final float CAMPEAO_X = (VW - CARD_W) * 0.5f, CAMPEAO_Y = 160f;

    /** Animação do sorteio: as pontas, depois uma dupla por vez. */
    static final float SORTEIO_INICIO = 0.9f;
    static final float SORTEIO_PASSO = 0.45f;
    static final float SLAM = 0.18f;
    /** Rodada nova da chave: um resultado a cada tanto. */
    static final float REVELA_PASSO = 0.18f;

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

    /** Quanto dura a revelação da rodada que acabou de ser decidida. */
    static float duracaoRevelacao(Torneio t) {
        int d = t.disputadas();
        return d == 0 ? 0f : 0.3f + (Torneio.VAGAS >> d) * REVELA_PASSO + SLAM;
    }

    /** Topo esquerdo do card da posição {@code i} na rodada {@code r} (3 = campeão). */
    static float[] posicao(int r, int i) {
        if (r >= Torneio.RODADAS) return new float[] {CAMPEAO_X, CAMPEAO_Y};
        int porLado = (Torneio.VAGAS >> r) / 2;
        boolean esquerda = i < porLado;
        int k = i % porLado;
        float x = esquerda ? COL_ESQ[r] : COL_DIR[r];
        float y = r == 0 ? TOPO_QUARTAS[k] : r == 1 ? TOPO_SEMI[k] : TOPO_FINAL;
        return new float[] {x, y};
    }

    /**
     * @param sorteio true: animação do sorteio (só as vagas); false: a chave com os resultados
     * @param time segundos desde que o passo começou
     */
    void draw(RenderCanvas c, Temporada temporada, Torneio t, boolean sorteio, float time) {
        c.fillVerticalGradient(-40f, -40f, VW + 40f, VH + 40f, 0xFF10131F, 0xFF050508);
        c.fillRadialGlow(120f, 380f, 420f, 0x332F6FD6);
        c.fillRadialGlow(VW - 120f, 380f, 420f, 0x33C62F2F);

        // Título numa pincelada vermelha.
        float in = Tinta.easeOutCubic(time / 0.35f);
        float[] band = {VW * 0.5f - 360f * in, 30f, VW * 0.5f + 360f * in, 18f, VW * 0.5f + 360f * in, 80f, VW * 0.5f - 360f * in, 92f};
        c.fillPolygon(band, 0xFFC8202C);
        String titulo = temporada.t(sorteio ? "O SORTEIO" : "CHAVE DO TORNEIO");
        c.drawText(titulo, VW * 0.5f, 72f, BotaoMenu.fit(c, titulo, 44f, 680f, true), 0xFFFFFFFF, 0xFF0A0A0A, 6f, true);

        int revelada = sorteio ? 0 : t.disputadas();
        float revelaDesde = 0.3f;
        drawLinhas(c, t, sorteio, time, revelada, revelaDesde);

        // Quartas: no sorteio, cada vaga na sua hora.
        for (int i = 0; i < Torneio.VAGAS; i++) {
            float aparece = 0f;
            if (sorteio) aparece = horaDaVaga(t, i);
            drawCard(c, temporada, t, 0, i, time - aparece, sorteio && time < aparece);
        }
        if (!sorteio) {
            for (int r = 1; r <= Torneio.RODADAS; r++) {
                int n = Torneio.VAGAS >> r;
                for (int i = 0; i < n; i++) {
                    float idade = r < revelada ? 99f : r == revelada ? time - revelaDesde - i * REVELA_PASSO : -1f;
                    if (r == Torneio.RODADAS && t.campeao() == null) continue;
                    drawCard(c, temporada, t, r, i, idade, idade < 0f);
                }
            }
            if (t.campeao() != null) {
                c.drawText(temporada.t("CAMPEÕES"), VW * 0.5f, CAMPEAO_Y - 10f, 22f, GOLD, 0xFF0A0A0A, 4f, true);
            }
        }
        c.drawText(temporada.t("FINAL"), VW * 0.5f, TOPO_FINAL + CARD_H + 30f, 24f, GOLD, 0xFF0A0A0A, 4f, true);
        c.drawText("×", VW * 0.5f, TOPO_FINAL + CARD_H * 0.5f + 12f, 34f, 0xFFFFFFFF, 0xFF0A0A0A, 4f, true);
        drawRodape(c, temporada, t, sorteio, time);
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
     * @param idade segundos desde que o card apareceu (negativo = ainda não)
     * @param vazio desenha a vaga com "?"
     */
    private void drawCard(RenderCanvas c, Temporada temporada, Torneio t, int r, int i, float idade, boolean vazio) {
        float[] p = posicao(r, i);
        float l = p[0], top = p[1];
        Torneio.Dupla d = vazio ? null : t.participante(r, i);
        float[] caixa = {l, top, l + CARD_W, top, l + CARD_W, top + CARD_H, l, top + CARD_H};
        if (d == null) {
            c.fillPolygon(caixa, 0x99101320);
            c.strokePolygon(caixa, 2f, 0x885A6275);
            c.drawText("?", l + CARD_W * 0.5f, top + CARD_H * 0.66f, 46f, 0x885A6275, 0, 0f, true);
            return;
        }
        float pop = Tinta.clamp01(idade / SLAM);
        float scale = 1.45f - 0.45f * Tinta.easeOutCubic(pop);
        boolean perdeu = r < t.disputadas() && r < Torneio.RODADAS && t.participante(r + 1, i / 2) != d;
        int alpha = perdeu ? 105 : 255;
        int borda = d == t.jogador() ? BLUE : d == t.rival() ? RED : GRAY;
        float cx = l + CARD_W * 0.5f, cy = top + CARD_H * 0.5f;
        c.save();
        c.scale(scale, scale, cx, cy);
        c.fillPolygon(new float[] {l + 6f, top + 7f, l + CARD_W + 6f, top + 7f, l + CARD_W + 6f, top + CARD_H + 7f, l + 6f, top + CARD_H + 7f},
            Tinta.withAlpha(0xFF000000, alpha / 2));
        c.fillPolygon(caixa, Tinta.withAlpha(CARD, alpha));
        rostos.draw(c, d.lutadores[0], l + 6f, top + 6f, 76f, 0xFF0A0A0A, 2f, alpha);
        rostos.draw(c, d.lutadores[1], l + 88f, top + 6f, 76f, 0xFF0A0A0A, 2f, alpha);
        String nome = Rostos.nomeDupla(temporada, d);
        c.drawText(nome, cx, top + CARD_H - 4f, BotaoMenu.fit(c, nome, 14f, CARD_W - 10f, false),
            Tinta.withAlpha(TEXT, alpha), 0, 0f, false);
        boolean destaque = d == t.jogador() || d == t.rival();
        c.strokePolygon(caixa, destaque ? 4f : 2f, Tinta.withAlpha(borda, alpha));
        if (perdeu) c.drawLine(l + 10f, top + CARD_H - 10f, l + CARD_W - 10f, top + 10f, 5f, 0xC0E5404C);
        if (pop < 1f) c.fillPolygon(caixa, Tinta.withAlpha(0xFFFFFFFF, (int)(200f * (1f - pop))));
        c.restore();
    }

    /** Linhas da chave; o caminho de quem avançou fica dourado. */
    private void drawLinhas(RenderCanvas c, Torneio t, boolean sorteio, float time, int revelada, float revelaDesde) {
        for (int r = 0; r < Torneio.RODADAS; r++) {
            int n = Torneio.VAGAS >> r;
            for (int i = 0; i < n; i++) {
                float[] filho = posicao(r, i);
                float[] pai = posicao(r + 1, i / 2);
                boolean esquerda = filho[0] < VW * 0.5f;
                float fy = filho[1] + CARD_H * 0.5f;
                boolean decidido = !sorteio && r + 1 <= revelada &&
                    (r + 1 < revelada || time - revelaDesde - (i / 2) * REVELA_PASSO >= 0f);
                boolean venceu = decidido && t.participante(r + 1, i / 2) == t.participante(r, i);
                int cor = venceu ? GOLD : 0x996E7689;
                float w = venceu ? 4f : 2.5f;
                if (r == Torneio.RODADAS - 1) {
                    // Finalistas: sobe do meio da final até o campeão (só depois da final).
                    if (sorteio || t.campeao() == null) continue;
                    float fx = esquerda ? filho[0] + CARD_W : filho[0];
                    float mx = VW * 0.5f;
                    c.drawLine(fx, fy - CARD_H * 0.5f + 12f, mx, fy - CARD_H * 0.5f + 12f, w, cor);
                    c.drawLine(mx, fy - CARD_H * 0.5f + 12f, mx, CAMPEAO_Y + CARD_H, w, cor);
                    continue;
                }
                float fx = esquerda ? filho[0] + CARD_W : filho[0];
                float px = esquerda ? pai[0] : pai[0] + CARD_W;
                float mx = (fx + px) * 0.5f;
                float py = pai[1] + CARD_H * 0.5f;
                c.drawLine(fx, fy, mx, fy, w, cor);
                c.drawLine(mx, fy, mx, py, w, cor);
                c.drawLine(mx, py, px, py, w, cor);
            }
        }
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
