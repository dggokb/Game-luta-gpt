package com.gamelutagpt.historia;

import com.gamelutagpt.render.RenderCanvas;
import java.util.ArrayList;
import java.util.List;

/**
 * Tela do MODO HISTÓRIA quando já existe save: CONTINUAR (do começo do episódio onde o jogador
 * parou) e os capítulos da temporada. Os já terminados e o seguinte a eles podem ser escolhidos;
 * os outros aparecem trancados. Toque escolhe; controle: direcional move, A escolhe, B volta.
 *
 * Não depende de plataforma e não é thread-safe: chame tudo da thread do loop do jogo.
 */
public final class MenuCapitulos {
    /** Escolhas além do índice do capítulo. */
    public static final int CONTINUAR = -1;
    public static final int VOLTAR = -2;

    public static final String TITULO = "MODO HISTÓRIA";
    public static final String CONTINUAR_TXT = "CONTINUAR";
    public static final String CONCLUIDO = "CONCLUÍDO";
    public static final String ATUAL = "ATUAL";
    public static final String BLOQUEADO = "BLOQUEADO";
    public static final String VOLTAR_TXT = "VOLTAR";
    public static final String COMPLETA = "TEMPORADA COMPLETA";
    public static final String DICA = "Toque num capítulo  •  Controle: direcional e A, B volta";
    public static final String[] TEXTOS = {TITULO, CONTINUAR_TXT, CONCLUIDO, ATUAL, BLOQUEADO, VOLTAR_TXT, COMPLETA, DICA,
        "PRÓLOGO", "EPISÓDIO"};

    /** Um capítulo: o número do episódio (0 = Prólogo) e o título. */
    public static final class Capitulo {
        public final int numero;
        public final String titulo;

        public Capitulo(int numero, String titulo) {
            this.numero = numero;
            this.titulo = titulo;
        }
    }

    static final float VW = PaginaHistoria.VW;
    static final float VH = PaginaHistoria.VH;
    static final float CARD_W = 228f, CARD_H = 140f, GAP = 18f;
    static final int POR_LINHA = 5;
    static final float[] CONTINUAR_BOX = {350f, 116f, 930f, 188f};
    static final float[] VOLTAR_BOX = {30f, 640f, 230f, 694f};
    static final float TAP_DELAY = 0.25f;
    /** Tremida do capítulo trancado quando o jogador tenta escolher. */
    static final float NEGADO = 0.35f;

    private static final int GOLD = 0xFFF2C14E;
    private static final int BLUE = 0xFF4FA3FF;
    private static final int GRAY = 0xFF6E7689;
    private static final int TEXT = 0xFFE9E1CF;

    private Temporada temporada;
    private final List<Capitulo> capitulos = new ArrayList<>();
    private ProgressoHistoria progresso;
    private boolean open;
    private float time;
    /** Item com o cursor: {@link #CONTINUAR}, {@link #VOLTAR} ou o índice do capítulo. */
    private int cursor;
    private int negado = Integer.MIN_VALUE;
    private float negadoAt;

    public void abrir(Temporada temporada, List<Capitulo> capitulos, ProgressoHistoria progresso) {
        this.temporada = temporada;
        this.capitulos.clear();
        this.capitulos.addAll(capitulos);
        this.progresso = progresso;
        open = true;
        time = 0f;
        negado = Integer.MIN_VALUE;
        cursor = podeContinuar() ? CONTINUAR : Math.min(progresso.concluidos(), capitulos.size() - 1);
    }

    public void fechar() {
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    public int cursor() {
        return cursor;
    }

    /** Há onde continuar (a temporada ainda não foi terminada). */
    public boolean podeContinuar() {
        return progresso != null && progresso.atual() < capitulos.size();
    }

    public void update(float dt) {
        if (open) time += dt;
    }

    // ------------------------------------------------------------ itens e navegação

    /** Itens da tela, na ordem: continuar (se houver), capítulos, voltar. */
    private List<Integer> itens() {
        List<Integer> out = new ArrayList<>();
        if (podeContinuar()) out.add(CONTINUAR);
        for (int i = 0; i < capitulos.size(); i++) out.add(i);
        out.add(VOLTAR);
        return out;
    }

    /** {esquerda, topo, direita, base} do item. */
    float[] caixa(int item) {
        if (item == CONTINUAR) return CONTINUAR_BOX;
        if (item == VOLTAR) return VOLTAR_BOX;
        int linha = item / POR_LINHA;
        int naLinha = Math.min(POR_LINHA, capitulos.size() - linha * POR_LINHA);
        float largura = naLinha * CARD_W + (naLinha - 1) * GAP;
        float l = (VW - largura) * 0.5f + (item % POR_LINHA) * (CARD_W + GAP);
        float t = 226f + linha * (CARD_H + 26f);
        return new float[] {l, t, l + CARD_W, t + CARD_H};
    }

    /** Controle: move o cursor para o item mais perto na direção (dx, dy). */
    public void mover(int dx, int dy) {
        if (!open || (dx == 0 && dy == 0)) return;
        float[] atual = caixa(cursor);
        float cx = (atual[0] + atual[2]) * 0.5f, cy = (atual[1] + atual[3]) * 0.5f;
        int melhor = cursor;
        float melhorCusto = Float.MAX_VALUE;
        for (int item : itens()) {
            if (item == cursor) continue;
            float[] b = caixa(item);
            float x = (b[0] + b[2]) * 0.5f - cx, y = (b[1] + b[3]) * 0.5f - cy;
            float custo;
            if (dx != 0) {
                if (Math.abs(y) > 40f || x * dx <= 0f) continue;
                custo = Math.abs(x);
            } else {
                if (y * dy <= 20f) continue;
                custo = Math.abs(y) + Math.abs(x) * 0.6f;
            }
            if (custo < melhorCusto) {
                melhorCusto = custo;
                melhor = item;
            }
        }
        cursor = melhor;
    }

    /**
     * Controle: escolhe o item do cursor. Devolve {@link #CONTINUAR}, {@link #VOLTAR} ou o
     * índice do capítulo (a tela fecha), ou null (capítulo trancado: ele treme).
     */
    public Integer confirmar() {
        if (!open || time < TAP_DELAY) return null;
        return escolher(cursor);
    }

    /** Controle: B / voltar. */
    public Integer voltar() {
        if (!open) return null;
        open = false;
        return VOLTAR;
    }

    /** Toque em (x, y), em coordenadas 1280x720. */
    public Integer tap(float x, float y) {
        if (!open || time < TAP_DELAY) return null;
        for (int item : itens()) {
            float[] b = caixa(item);
            if (x >= b[0] - 14f && x <= b[2] + 14f && y >= b[1] && y <= b[3]) {
                cursor = item;
                return escolher(item);
            }
        }
        return null;
    }

    private Integer escolher(int item) {
        if (item >= 0 && !progresso.liberado(item)) {
            negado = item;
            negadoAt = time;
            return null;
        }
        open = false;
        return item;
    }

    private String t(String texto) {
        return temporada != null ? temporada.t(texto) : texto;
    }

    /** "PRÓLOGO" ou "EPISÓDIO 3". */
    private String rotulo(Capitulo cap) {
        return cap.numero == 0 ? t("PRÓLOGO") : t("EPISÓDIO") + " " + cap.numero;
    }

    // ------------------------------------------------------------ desenho

    public void render(RenderCanvas c) {
        if (!open) return;
        c.fillVerticalGradient(-40f, -40f, VW + 40f, VH + 40f, 0xFF10131F, 0xFF050508);
        c.fillRadialGlow(60f, 380f, 380f, 0x442F6FD6);
        c.fillRadialGlow(VW - 60f, 380f, 380f, 0x44C62F2F);
        float in = Tinta.easeOutCubic(time / 0.35f);
        float[] band = {VW * 0.5f - 300f * in, 30f, VW * 0.5f + 300f * in, 18f, VW * 0.5f + 300f * in, 80f,
            VW * 0.5f - 300f * in, 92f};
        c.fillPolygon(band, BotaoMenu.RED);
        String titulo = t(TITULO);
        c.drawText(titulo, VW * 0.5f, 72f, BotaoMenu.fit(c, titulo, 44f, 560f, true), 0xFFFFFFFF, 0xFF0A0A0A, 6f, true);

        if (podeContinuar()) {
            Capitulo cap = capitulos.get(progresso.atual());
            String sub = rotulo(cap) + "  ·  " + t(cap.titulo);
            BotaoMenu.draw(c, CONTINUAR_BOX, t(CONTINUAR_TXT), sub, cursor == CONTINUAR, time, 0f, (int)(255f * in));
        } else {
            c.drawText(t(COMPLETA), VW * 0.5f, 166f, 34f, GOLD, 0xFF0A0A0A, 5f, true);
        }
        for (int i = 0; i < capitulos.size(); i++) drawCapitulo(c, i, in);

        // VOLTAR
        float[] v = VOLTAR_BOX;
        boolean sel = cursor == VOLTAR;
        c.fillRect(v[0], v[1], v[2], v[3], sel ? 0xE6C8202C : 0xC0101320);
        c.strokePolygon(new float[] {v[0], v[1], v[2], v[1], v[2], v[3], v[0], v[3]}, sel ? 4f : 2f, sel ? 0xFFFFFFFF : GRAY);
        c.drawText("◀ " + t(VOLTAR_TXT), (v[0] + v[2]) * 0.5f, v[3] - 17f, 22f, 0xFFFFFFFF, 0, 0f, true);

        String dica = t(DICA);
        c.drawText(dica, 760f, 675f, BotaoMenu.fit(c, dica, 17f, 920f, false), 0xFF8A93A8, 0, 0f, false);
        float fadeIn = 1f - Tinta.clamp01(time / 0.25f);
        if (fadeIn > 0f) c.fillRect(-40f, -40f, VW + 40f, VH + 40f, Tinta.withAlpha(0xFF000000, (int)(255f * fadeIn)));
    }

    private void drawCapitulo(RenderCanvas c, int i, float in) {
        Capitulo cap = capitulos.get(i);
        float[] b = caixa(i);
        boolean liberado = progresso.liberado(i);
        boolean concluido = progresso.concluido(i);
        boolean atual = podeContinuar() && progresso.atual() == i;
        boolean sel = cursor == i;
        int alpha = (int)((liberado ? 255f : 130f) * Tinta.clamp01((time - 0.1f - i * 0.05f) / 0.25f));
        if (alpha <= 0) return;
        float shake = 0f;
        if (negado == i && time - negadoAt < NEGADO) shake = (float)Math.sin((time - negadoAt) * 60f) * 8f * (1f - (time - negadoAt) / NEGADO);
        float l = b[0] + shake, t = b[1], r = b[2] + shake, bo = b[3];
        float[] caixa = {l, t, r, t, r, bo, l, bo};
        if (sel) c.fillRadialGlow((l + r) * 0.5f, (t + bo) * 0.5f, CARD_W * 0.7f, Tinta.withAlpha(BotaoMenu.RED, alpha / 3));
        c.fillPolygon(new float[] {l + 6f, t + 7f, r + 6f, t + 7f, r + 6f, bo + 7f, l + 6f, bo + 7f}, Tinta.withAlpha(0xFF000000, alpha / 2));
        c.fillPolygon(caixa, Tinta.withAlpha(0xEE141724, alpha));
        int faixa = concluido ? 0xFF8A6A1A : atual ? 0xFF1E4F8F : 0xFF2A3044;
        c.fillRect(l, t, r, t + 28f, Tinta.withAlpha(faixa, alpha));
        c.drawText(rotulo(cap), (l + r) * 0.5f, t + 21f, 17f, Tinta.withAlpha(TEXT, alpha), 0, 0f, true);
        String titulo = t(cap.titulo);
        c.drawText(titulo, (l + r) * 0.5f, t + 80f, BotaoMenu.fit(c, titulo, 28f, CARD_W - 24f, true),
            Tinta.withAlpha(0xFFFFFFFF, alpha), Tinta.withAlpha(0xFF0A0A0A, alpha), 4f, true);
        String status;
        int cor;
        if (!liberado) {
            status = t(BLOQUEADO);
            cor = GRAY;
            drawCadeado(c, (l + r) * 0.5f - 72f, bo - 20f, alpha);
        } else if (concluido) {
            status = "✓ " + t(CONCLUIDO);
            cor = GOLD;
        } else {
            status = "▶ " + t(ATUAL);
            cor = BLUE;
        }
        c.drawText(status, (l + r) * 0.5f, bo - 14f, 16f, Tinta.withAlpha(cor, alpha), 0, 0f, true);
        int borda = sel ? 0xFFFFFFFF : concluido ? GOLD : atual ? BLUE : GRAY;
        c.strokePolygon(caixa, sel ? 4f : 2f, Tinta.withAlpha(borda, alpha));
    }

    /** Cadeado simples: arco e corpo. */
    private static void drawCadeado(RenderCanvas c, float x, float y, int alpha) {
        c.strokeCircle(x, y - 9f, 6f, 2.5f, Tinta.withAlpha(GRAY, alpha));
        c.fillRect(x - 8f, y - 7f, x + 8f, y + 5f, Tinta.withAlpha(GRAY, alpha));
    }
}
