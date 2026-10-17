package com.gamelutagpt.historia;

import com.gamelutagpt.render.RenderCanvas;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lista de comandos (opção COMANDOS da pausa): os golpes de um personagem com o comando de cada
 * um. ◀ ▶ (ou as setas da tela) trocam o personagem; voltar fecha. Quem abre entrega as fichas
 * prontas, com os botões já no esquema do jogador (toque, teclado ou controle).
 *
 * Não depende de plataforma e não é thread-safe: chame tudo da thread do loop do jogo.
 */
public final class TelaComandos {
    public static final String TITULO = "COMANDOS";
    public static final String NO_AR = "No ar";
    public static final String LEGENDA = "[←] → = segure ← e aperte →";
    public static final String DICA = "◀ ▶ troca o personagem";
    public static final String VOLTAR = "VOLTAR";
    public static final String SUPER = "Super";
    public static final String ULTRA = "Ultra";
    public static final String AGARRAO = "Agarrão";
    public static final String GOLPE_ALTO = "Golpe alto";
    public static final String DEFESA_PERFEITA = "Defesa perfeita (no último instante)";
    public static final String[] TEXTOS = {TITULO, NO_AR, LEGENDA, DICA, VOLTAR, SUPER, ULTRA, AGARRAO,
        GOLPE_ALTO, DEFESA_PERFEITA};

    /** Um golpe: nome, comando (setas e botões) e se só sai no ar. */
    public static final class Linha {
        public final String nome;
        public final String comando;
        public final boolean noAr;

        public Linha(String nome, String comando, boolean noAr) {
            this.nome = nome;
            this.comando = comando;
            this.noAr = noAr;
        }
    }

    /** Os golpes de um personagem. */
    public static final class Ficha {
        public final String personagem;
        public final List<Linha> linhas;

        public Ficha(String personagem, List<Linha> linhas) {
            this.personagem = personagem;
            this.linhas = Collections.unmodifiableList(new ArrayList<>(linhas));
        }
    }

    /** Setas de trocar o personagem e o botão VOLTAR, em coordenadas 1280x720. */
    static final float[] ANTERIOR = {300f, 140f, 420f, 214f};
    static final float[] PROXIMO = {860f, 140f, 980f, 214f};
    static final float[] BOTAO_VOLTAR = {1010f, 648f, 1220f, 706f};
    static final float LINHAS_TOPO = 236f;
    static final float LINHA_ALTURA = 56f;
    /** As linhas terminam aqui (acima do rodapé): muitas linhas ficam mais baixas. */
    static final float LINHAS_FIM = 640f;

    private Temporada temporada;
    private List<Ficha> fichas = Collections.emptyList();
    private int atual;
    private boolean open;
    private float time;

    /** As fichas que a tela mostra e qual aparece primeiro (o personagem do jogador). */
    public void setFichas(List<Ficha> fichas, int atual) {
        this.fichas = Collections.unmodifiableList(new ArrayList<>(fichas));
        this.atual = this.fichas.isEmpty() ? 0 : Math.floorMod(atual, this.fichas.size());
    }

    void abrir(Temporada temporada) {
        this.temporada = temporada;
        open = true;
        time = 0f;
    }

    void fechar() {
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    /** A ficha na tela (null sem fichas). */
    public Ficha ficha() {
        return fichas.isEmpty() ? null : fichas.get(atual);
    }

    /** ◀ (-1) ou ▶ (+1): o personagem anterior ou o próximo (dá a volta). */
    public void trocar(int direcao) {
        if (!open || fichas.isEmpty() || direcao == 0) return;
        atual = Math.floorMod(atual + (direcao > 0 ? 1 : -1), fichas.size());
    }

    void update(float dt) {
        if (open) time += dt;
    }

    /** Toque: as setas trocam o personagem, VOLTAR fecha. Devolve true quando fechou. */
    boolean tap(float x, float y) {
        if (!open || time < MenuPausa.TAP_DELAY) return false;
        if (BotaoMenu.dentro(ANTERIOR, x, y)) trocar(-1);
        else if (BotaoMenu.dentro(PROXIMO, x, y)) trocar(1);
        else if (BotaoMenu.dentro(BOTAO_VOLTAR, x, y)) {
            open = false;
            return true;
        }
        return false;
    }

    private String t(String texto) {
        return temporada != null ? temporada.t(texto) : texto;
    }

    void render(RenderCanvas c) {
        if (!open) return;
        float in = Tinta.easeOutCubic(time / 0.18f);
        int a = (int)(255f * in);
        c.fillRect(-40f, -40f, PaginaHistoria.VW + 40f, PaginaHistoria.VH + 40f, Tinta.withAlpha(0xFF000000, (int)(225f * in)));
        float cx = PaginaHistoria.VW * 0.5f;
        float[] band = {cx - 300f, 34f, cx + 300f, 20f, cx + 300f, 98f, cx - 300f, 112f};
        c.fillPolygon(band, Tinta.withAlpha(BotaoMenu.RED, a));
        String titulo = t(TITULO);
        float size = BotaoMenu.fit(c, titulo, 54f, 540f, true);
        c.drawText(titulo, cx, 84f, size, Tinta.withAlpha(BotaoMenu.WHITE, a), Tinta.withAlpha(BotaoMenu.INK, a),
            size * 0.14f, true);
        Ficha ficha = ficha();
        if (ficha != null) {
            String nome = ficha.personagem.toUpperCase();
            float nomeSize = BotaoMenu.fit(c, nome, 44f, 400f, true);
            c.drawText(nome, cx, 194f, nomeSize, Tinta.withAlpha(BotaoMenu.WHITE, a), Tinta.withAlpha(BotaoMenu.INK, a),
                nomeSize * 0.14f, true);
            if (fichas.size() > 1) {
                seta(c, ANTERIOR, -1, a);
                seta(c, PROXIMO, 1, a);
            }
            float altura = alturaDaLinha(ficha.linhas.size());
            for (int i = 0; i < ficha.linhas.size(); i++) linha(c, ficha.linhas.get(i), LINHAS_TOPO + i * altura, altura, i, a);
        }
        rodape(c, t(LEGENDA) + "    ·    " + t(DICA), 120f, 663f, 840f, a);
        BotaoMenu.draw(c, BOTAO_VOLTAR, t(VOLTAR), null, true, time, 0f, a);
    }

    private void seta(RenderCanvas c, float[] caixa, int lado, int a) {
        float mx = (caixa[0] + caixa[2]) * 0.5f, my = (caixa[1] + caixa[3]) * 0.5f;
        float bob = (float)Math.sin(time * 6f) * 4f * lado;
        float[] p = lado < 0
            ? new float[] {mx + 18f + bob, my - 26f, mx - 22f + bob, my, mx + 18f + bob, my + 26f}
            : new float[] {mx - 18f + bob, my - 26f, mx + 22f + bob, my, mx - 18f + bob, my + 26f};
        c.fillPolygon(p, Tinta.withAlpha(BotaoMenu.RED, a));
        c.strokePolygon(p, 3f, Tinta.withAlpha(BotaoMenu.WHITE, a));
    }

    /** Altura de cada linha: a de sempre, ou menos para caberem todas acima do rodapé. */
    static float alturaDaLinha(int linhas) {
        return linhas <= 0 ? LINHA_ALTURA : Math.min(LINHA_ALTURA, (LINHAS_FIM - LINHAS_TOPO) / linhas);
    }

    private void linha(RenderCanvas c, Linha linha, float top, float altura, int i, int a) {
        float left = 150f, right = 1130f, bottom = top + altura - 8f;
        c.fillRect(left, top, right, bottom, Tinta.withAlpha(i % 2 == 0 ? 0xFF1A1C2A : 0xFF141622, a));
        c.fillRect(left, top, left + 6f, bottom, Tinta.withAlpha(BotaoMenu.RED, a));
        float baseline = top + (altura - 8f) * 0.5f + 10f;
        float letra = Math.min(28f, altura * 0.56f);
        String nome = t(linha.nome);
        esquerda(c, nome, left + 26f, baseline, BotaoMenu.fit(c, nome, letra, 380f, true), BotaoMenu.WHITE, true, a);
        String comando = linha.noAr ? t(NO_AR) + ": " + linha.comando : linha.comando;
        esquerda(c, comando, 560f, baseline, BotaoMenu.fit(c, comando, letra, right - 580f, false), 0xFFFFE08A, false, a);
    }

    private static void rodape(RenderCanvas c, String texto, float left, float y, float width, int a) {
        float size = BotaoMenu.fit(c, texto, 20f, width, false);
        esquerda(c, texto, left, y + 7f, size, 0xFFB9C0D0, false, a);
    }

    /** O drawText centraliza: o meio fica meia largura depois de {@code x}. */
    private static void esquerda(RenderCanvas c, String texto, float x, float y, float size, int cor, boolean italic, int a) {
        float w = c.measureText(texto, size, italic);
        c.drawText(texto, x + w * 0.5f, y, size, Tinta.withAlpha(cor, a), Tinta.withAlpha(BotaoMenu.INK, a),
            size * 0.08f, italic);
    }
}
