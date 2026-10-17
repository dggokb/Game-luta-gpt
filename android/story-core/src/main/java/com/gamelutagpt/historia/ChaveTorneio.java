package com.gamelutagpt.historia;

import com.gamelutagpt.render.RenderCanvas;

/**
 * Desenha a chave do torneio (passos "sorteio" e "chave"): as duplas da primeira rodada dos
 * dois lados, as rodadas seguintes indo para o meio, a final no meio e o campeão em cima. Na
 * história são 8 duplas (quartas, semi, final); no Arcade, 16 (oitavas antes), em cards
 * menores. A dupla do jogador tem borda azul, a do rival vermelha; quem perdeu fica apagado e
 * riscado.
 *
 * No sorteio, as duplas fixas (os irmãos; no Arcade, a do jogador) aparecem primeiro e as
 * outras caem uma a uma nas vagas, na ordem sorteada. Na chave, a rodada que acabou de ser
 * decidida aparece por último.
 */
final class ChaveTorneio {
    /** Textos da chave que passam pela tradução (mais os nomes das rodadas, em {@link Torneio}). */
    /** Rodapé do sorteio do Arcade. */
    static final String ARCADE_SORTEIO = "Quatro vitórias até o título. Depois, a regra escondida.";
    static final String[] TEXTOS = {"O SORTEIO", "CHAVE DO TORNEIO", "FINAL", "CAMPEÕES", "PRÓXIMA LUTA",
        "Os irmãos ficam em lados opostos: só se encontram na final.", "Fim do torneio para a sua dupla.",
        ARCADE_SORTEIO};

    static final float VW = PaginaHistoria.VW;
    static final float VH = PaginaHistoria.VH;

    /**
     * Medidas de uma chave: o card, os retratos, as colunas de cada lado (uma por rodada, da
     * primeira à final) e o topo dos cards da primeira rodada de um lado (as rodadas seguintes
     * ficam no meio das duas que se enfrentaram).
     */
    static final class Layout {
        final float cardW, cardH, rosto, nome;
        final float[] colEsq, colDir, topo;
        final float campeaoY, rodapeY;

        Layout(float cardW, float cardH, float rosto, float nome, float[] colEsq, float[] colDir, float[] topo,
               float campeaoY, float rodapeY) {
            this.cardW = cardW;
            this.cardH = cardH;
            this.rosto = rosto;
            this.nome = nome;
            this.colEsq = colEsq;
            this.colDir = colDir;
            this.topo = topo;
            this.campeaoY = campeaoY;
            this.rodapeY = rodapeY;
        }

        float campeaoX() {
            return (VW - cardW) * 0.5f;
        }

        /** Topo do card {@code k} de um lado na rodada {@code r}. */
        float topo(int r, int k) {
            if (r == 0) return topo[k];
            return (topo(r - 1, 2 * k) + topo(r - 1, 2 * k + 1)) * 0.5f;
        }
    }

    /** A chave da história: 8 duplas, cards grandes. */
    static final Layout OITO = new Layout(170f, 100f, 76f, 14f,
        new float[] {30f, 240f, 450f}, new float[] {1080f, 870f, 660f},
        new float[] {120f, 230f, 400f, 510f}, 160f, 668f);
    /** A chave do Arcade: 16 duplas, 8 de cada lado. */
    static final Layout DEZESSEIS = new Layout(112f, 66f, 50f, 11f,
        new float[] {12f, 146f, 280f, 414f}, new float[] {1156f, 1022f, 888f, 754f},
        new float[] {100f, 170f, 240f, 310f, 400f, 470f, 540f, 610f}, 200f, 706f);

    static Layout layout(Torneio t) {
        return t.vagas() > Torneio.VAGAS ? DEZESSEIS : OITO;
    }

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

    /** Intervalo entre duas duplas sorteadas: 0,45 s na história; mais rápido com 15 vagas. */
    static float passoSorteio(Torneio t) {
        return Math.min(SORTEIO_PASSO, 2.7f / Math.max(1, t.ordemSorteio().length));
    }

    /** Quanto dura a animação do sorteio (as duplas sorteadas, uma a uma). */
    static float duracaoSorteio(Torneio t) {
        return SORTEIO_INICIO + t.ordemSorteio().length * passoSorteio(t) + SLAM;
    }

    /** Quanto dura a revelação da rodada que acabou de ser decidida. */
    static float duracaoRevelacao(Torneio t) {
        int d = t.disputadas();
        return d == 0 ? 0f : 0.3f + (t.vagas() >> d) * REVELA_PASSO + SLAM;
    }

    /** Topo esquerdo do card da posição {@code i} na rodada {@code r} ({@code rodadas()} = campeão). */
    static float[] posicao(Torneio t, int r, int i) {
        Layout l = layout(t);
        if (r >= t.rodadas()) return new float[] {l.campeaoX(), l.campeaoY};
        int porLado = (t.vagas() >> r) / 2;
        boolean esquerda = i < porLado;
        int k = i % porLado;
        return new float[] {esquerda ? l.colEsq[r] : l.colDir[r], l.topo(r, k)};
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

        // Primeira rodada: no sorteio, cada vaga na sua hora.
        Layout l = layout(t);
        for (int i = 0; i < t.vagas(); i++) {
            float aparece = 0f;
            if (sorteio) aparece = horaDaVaga(t, i);
            drawCard(c, temporada, t, 0, i, time - aparece, sorteio && time < aparece);
        }
        if (!sorteio) {
            for (int r = 1; r <= t.rodadas(); r++) {
                int n = t.vagas() >> r;
                for (int i = 0; i < n; i++) {
                    float idade = r < revelada ? 99f : r == revelada ? time - revelaDesde - i * REVELA_PASSO : -1f;
                    if (r == t.rodadas() && t.campeao() == null) continue;
                    drawCard(c, temporada, t, r, i, idade, idade < 0f);
                }
            }
            if (t.campeao() != null) {
                c.drawText(temporada.t("CAMPEÕES"), VW * 0.5f, l.campeaoY - 10f, 22f, GOLD, 0xFF0A0A0A, 4f, true);
            }
        }
        float topoFinal = l.topo(t.rodadas() - 1, 0);
        c.drawText(temporada.t("FINAL"), VW * 0.5f, topoFinal + l.cardH + 30f, 24f, GOLD, 0xFF0A0A0A, 4f, true);
        c.drawText("×", VW * 0.5f, topoFinal + l.cardH * 0.5f + 12f, 34f, 0xFFFFFFFF, 0xFF0A0A0A, 4f, true);
        drawRodape(c, temporada, t, sorteio, time);
    }

    /** Quando a vaga aparece no sorteio: as fixas logo, as outras na ordem sorteada. */
    private static float horaDaVaga(Torneio t, int vaga) {
        int[] ordem = t.ordemSorteio();
        for (int k = 0; k < ordem.length; k++) {
            if (ordem[k] == vaga) return SORTEIO_INICIO + k * passoSorteio(t);
        }
        return 0.25f;
    }

    /**
     * @param idade segundos desde que o card apareceu (negativo = ainda não)
     * @param vazio desenha a vaga com "?"
     */
    private void drawCard(RenderCanvas c, Temporada temporada, Torneio t, int r, int i, float idade, boolean vazio) {
        Layout lay = layout(t);
        float cardW = lay.cardW, cardH = lay.cardH;
        float[] p = posicao(t, r, i);
        float l = p[0], top = p[1];
        Torneio.Dupla d = vazio ? null : t.participante(r, i);
        float[] caixa = {l, top, l + cardW, top, l + cardW, top + cardH, l, top + cardH};
        if (d == null) {
            c.fillPolygon(caixa, 0x99101320);
            c.strokePolygon(caixa, 2f, 0x885A6275);
            c.drawText("?", l + cardW * 0.5f, top + cardH * 0.66f, cardH * 0.46f, 0x885A6275, 0, 0f, true);
            return;
        }
        float pop = Tinta.clamp01(idade / SLAM);
        float scale = 1.45f - 0.45f * Tinta.easeOutCubic(pop);
        boolean perdeu = r < t.disputadas() && r < t.rodadas() && t.participante(r + 1, i / 2) != d;
        int alpha = perdeu ? 105 : 255;
        int borda = d == t.jogador() ? BLUE : d == t.rival() ? RED : GRAY;
        float cx = l + cardW * 0.5f, cy = top + cardH * 0.5f;
        c.save();
        c.scale(scale, scale, cx, cy);
        c.fillPolygon(new float[] {l + 6f, top + 7f, l + cardW + 6f, top + 7f, l + cardW + 6f, top + cardH + 7f, l + 6f, top + cardH + 7f},
            Tinta.withAlpha(0xFF000000, alpha / 2));
        c.fillPolygon(caixa, Tinta.withAlpha(CARD, alpha));
        // Os dois retratos lado a lado, com a mesma margem nas bordas e no meio.
        float margem = (cardW - 2f * lay.rosto) / 3f;
        rostos.draw(c, d.lutadores[0], l + margem, top + margem, lay.rosto, 0xFF0A0A0A, 2f, alpha);
        rostos.draw(c, d.lutadores[1], l + 2f * margem + lay.rosto, top + margem, lay.rosto, 0xFF0A0A0A, 2f, alpha);
        String nome = Rostos.nomeDupla(temporada, d);
        c.drawText(nome, cx, top + cardH - 4f, BotaoMenu.fit(c, nome, lay.nome, cardW - 10f, false),
            Tinta.withAlpha(TEXT, alpha), 0, 0f, false);
        boolean destaque = d == t.jogador() || d == t.rival();
        c.strokePolygon(caixa, destaque ? 4f : 2f, Tinta.withAlpha(borda, alpha));
        if (perdeu) c.drawLine(l + 10f, top + cardH - 10f, l + cardW - 10f, top + 10f, 5f, 0xC0E5404C);
        if (pop < 1f) c.fillPolygon(caixa, Tinta.withAlpha(0xFFFFFFFF, (int)(200f * (1f - pop))));
        c.restore();
    }

    /** Linhas da chave; o caminho de quem avançou fica dourado. */
    private void drawLinhas(RenderCanvas c, Torneio t, boolean sorteio, float time, int revelada, float revelaDesde) {
        Layout l = layout(t);
        float cardW = l.cardW, cardH = l.cardH;
        for (int r = 0; r < t.rodadas(); r++) {
            int n = t.vagas() >> r;
            for (int i = 0; i < n; i++) {
                float[] filho = posicao(t, r, i);
                float[] pai = posicao(t, r + 1, i / 2);
                boolean esquerda = filho[0] < VW * 0.5f;
                float fy = filho[1] + cardH * 0.5f;
                boolean decidido = !sorteio && r + 1 <= revelada &&
                    (r + 1 < revelada || time - revelaDesde - (i / 2) * REVELA_PASSO >= 0f);
                boolean venceu = decidido && t.participante(r + 1, i / 2) == t.participante(r, i);
                int cor = venceu ? GOLD : 0x996E7689;
                float w = venceu ? 4f : 2.5f;
                if (r == t.rodadas() - 1) {
                    // Finalistas: sobe do meio da final até o campeão (só depois da final).
                    if (sorteio || t.campeao() == null) continue;
                    float fx = esquerda ? filho[0] + cardW : filho[0];
                    float mx = VW * 0.5f;
                    c.drawLine(fx, fy - cardH * 0.5f + 12f, mx, fy - cardH * 0.5f + 12f, w, cor);
                    c.drawLine(mx, fy - cardH * 0.5f + 12f, mx, l.campeaoY + cardH, w, cor);
                    continue;
                }
                float fx = esquerda ? filho[0] + cardW : filho[0];
                float px = esquerda ? pai[0] : pai[0] + cardW;
                float mx = (fx + px) * 0.5f;
                float py = pai[1] + cardH * 0.5f;
                c.drawLine(fx, fy, mx, fy, w, cor);
                c.drawLine(mx, fy, mx, py, w, cor);
                c.drawLine(mx, py, px, py, w, cor);
            }
        }
    }

    private void drawRodape(RenderCanvas c, Temporada temporada, Torneio t, boolean sorteio, float time) {
        float y = layout(t).rodapeY;
        if (sorteio) {
            if (time < duracaoSorteio(t)) return;
            float a = Tinta.clamp01((time - duracaoSorteio(t)) / 0.3f);
            String texto = temporada.t(t.rival() == null ? ARCADE_SORTEIO
                : "Os irmãos ficam em lados opostos: só se encontram na final.");
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
