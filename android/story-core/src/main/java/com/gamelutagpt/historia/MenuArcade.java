package com.gamelutagpt.historia;

import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderCanvas;
import java.util.ArrayList;
import java.util.List;

/**
 * Primeira tela do ARCADE: a dupla do jogador. As duplas oficiais (as do torneio que já lutam)
 * aparecem em cards com os dois retratos; o último card, DUPLA LIVRE, leva à seleção de
 * personagens para montar a dupla com quem quiser. Toque escolhe; controle: direcional move,
 * A escolhe, B volta.
 *
 * Não depende de plataforma e não é thread-safe: chame tudo da thread do loop do jogo.
 */
public final class MenuArcade {
    /** Escolhas além do índice da dupla oficial. */
    public static final int LIVRE = -1;
    public static final int VOLTAR = -2;

    public static final String TITULO = "ARCADE";
    public static final String ESCOLHA = "ESCOLHA SUA DUPLA";
    public static final String OFICIAL = "DUPLA OFICIAL";
    public static final String LIVRE_TXT = "DUPLA LIVRE";
    public static final String LIVRE_SUB = "Monte a sua com quem quiser";
    public static final String VOLTAR_TXT = "VOLTAR";
    public static final String DICA = "16 duplas, 4 rodadas, o duelo e o Campeão  •  Controle: direcional e A, B volta";
    public static final String[] TEXTOS = {TITULO, ESCOLHA, OFICIAL, LIVRE_TXT, LIVRE_SUB, VOLTAR_TXT, DICA};

    static final float VW = PaginaHistoria.VW;
    static final float VH = PaginaHistoria.VH;
    static final float CARD_W = 270f, CARD_H = 196f, GAP = 24f;
    static final int POR_LINHA = 4;
    static final float TOPO = 168f;
    static final float[] VOLTAR_BOX = {30f, 640f, 230f, 694f};
    static final float TAP_DELAY = 0.25f;

    private static final int BLUE = 0xFF4FA3FF;
    private static final int GOLD = 0xFFF2C14E;
    private static final int GRAY = 0xFF6E7689;
    private static final int TEXT = 0xFFE9E1CF;

    private final Rostos rostos;
    private final List<Torneio.Dupla> oficiais = new ArrayList<>();
    private Temporada temporada;
    private boolean open;
    private float time;
    /** Item com o cursor: o índice da dupla, {@link #LIVRE} ou {@link #VOLTAR}. */
    private int cursor;

    public MenuArcade(RenderAssets assets) {
        rostos = new Rostos(assets);
    }

    /** Abre com as duplas oficiais que lutam ({@link Arcade#oficiais}) e o cursor na primeira. */
    public void abrir(Temporada temporada, List<Torneio.Dupla> oficiais) {
        this.temporada = temporada;
        this.oficiais.clear();
        this.oficiais.addAll(oficiais);
        open = true;
        time = 0f;
        cursor = this.oficiais.isEmpty() ? LIVRE : 0;
    }

    /** Fecha e solta os retratos. */
    public void fechar() {
        open = false;
        rostos.liberar();
    }

    public boolean isOpen() {
        return open;
    }

    public int cursor() {
        return cursor;
    }

    /** A dupla oficial {@code i}. */
    public Torneio.Dupla oficial(int i) {
        return oficiais.get(i);
    }

    public void update(float dt) {
        if (open) time += dt;
    }

    // ------------------------------------------------------------ itens e navegação

    /** Itens na ordem: as duplas oficiais, a dupla livre e o voltar. */
    private List<Integer> itens() {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < oficiais.size(); i++) out.add(i);
        out.add(LIVRE);
        out.add(VOLTAR);
        return out;
    }

    /** {esquerda, topo, direita, base} do item. A dupla livre é o último card da grade. */
    float[] caixa(int item) {
        if (item == VOLTAR) return VOLTAR_BOX;
        int pos = item == LIVRE ? oficiais.size() : item;
        int total = oficiais.size() + 1;
        int linha = pos / POR_LINHA;
        int naLinha = Math.min(POR_LINHA, total - linha * POR_LINHA);
        float largura = naLinha * CARD_W + (naLinha - 1) * GAP;
        float l = (VW - largura) * 0.5f + (pos % POR_LINHA) * (CARD_W + GAP);
        float t = TOPO + linha * (CARD_H + 26f);
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

    /** Controle: escolhe o item do cursor (a tela fecha). Null antes de poder escolher. */
    public Integer confirmar() {
        if (!open || time < TAP_DELAY) return null;
        return escolher(cursor);
    }

    /** Controle: B / voltar. */
    public Integer voltar() {
        if (!open) return null;
        return escolher(VOLTAR);
    }

    /** Toque em (x, y), em coordenadas 1280x720: o item tocado, ou null. */
    public Integer tap(float x, float y) {
        if (!open || time < TAP_DELAY) return null;
        for (int item : itens()) {
            float[] b = caixa(item);
            if (x >= b[0] && x <= b[2] && y >= b[1] && y <= b[3]) {
                cursor = item;
                return escolher(item);
            }
        }
        return null;
    }

    private Integer escolher(int item) {
        fechar();
        return item;
    }

    private String t(String texto) {
        return temporada != null ? temporada.t(texto) : texto;
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
        String escolha = t(ESCOLHA);
        c.drawText(escolha, VW * 0.5f, 136f, BotaoMenu.fit(c, escolha, 28f, 900f, true), GOLD, 0xFF0A0A0A, 4f, true);

        for (int i = 0; i < oficiais.size(); i++) drawCard(c, i, i);
        drawCard(c, LIVRE, oficiais.size());

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

    /** Card de uma dupla oficial (os dois retratos e os nomes) ou da dupla livre. */
    private void drawCard(RenderCanvas c, int item, int ordem) {
        float[] b = caixa(item);
        int alpha = (int)(255f * Tinta.clamp01((time - 0.1f - ordem * 0.05f) / 0.25f));
        if (alpha <= 0) return;
        boolean sel = cursor == item;
        boolean livre = item == LIVRE;
        float l = b[0], t = b[1], r = b[2], bo = b[3];
        float[] caixa = {l, t, r, t, r, bo, l, bo};
        if (sel) c.fillRadialGlow((l + r) * 0.5f, (t + bo) * 0.5f, CARD_W * 0.7f, Tinta.withAlpha(livre ? BotaoMenu.RED : BLUE, alpha / 3));
        c.fillPolygon(new float[] {l + 6f, t + 7f, r + 6f, t + 7f, r + 6f, bo + 7f, l + 6f, bo + 7f}, Tinta.withAlpha(0xFF000000, alpha / 2));
        c.fillPolygon(caixa, Tinta.withAlpha(0xEE141724, alpha));
        c.fillRect(l, t, r, t + 28f, Tinta.withAlpha(livre ? 0xFF8F1E26 : 0xFF1E4F8F, alpha));
        c.drawText(t(livre ? LIVRE_TXT : OFICIAL), (l + r) * 0.5f, t + 21f, 17f, Tinta.withAlpha(TEXT, alpha), 0, 0f, true);
        float lado = 112f, margem = (CARD_W - 2f * lado) / 3f, topo = t + 36f;
        if (livre) {
            for (int k = 0; k < 2; k++) {
                float x = l + margem + k * (lado + margem);
                float[] q = {x, topo, x + lado, topo, x + lado, topo + lado, x, topo + lado};
                c.fillPolygon(q, Tinta.withAlpha(0xFF2A2F3D, alpha));
                c.strokePolygon(q, 2f, Tinta.withAlpha(0xFF0A0A0A, alpha));
                c.drawText("?", x + lado * 0.5f, topo + lado * 0.68f, 64f, Tinta.withAlpha(GRAY, alpha), 0, 0f, true);
            }
            String sub = t(LIVRE_SUB);
            c.drawText(sub, (l + r) * 0.5f, bo - 16f, BotaoMenu.fit(c, sub, 17f, CARD_W - 20f, false),
                Tinta.withAlpha(0xFFFFFFFF, alpha), 0, 0f, false);
        } else {
            Torneio.Dupla d = oficiais.get(item);
            rostos.draw(c, d.lutadores[0], l + margem, topo, lado, 0xFF0A0A0A, 2f, alpha);
            rostos.draw(c, d.lutadores[1], l + 2f * margem + lado, topo, lado, 0xFF0A0A0A, 2f, alpha);
            String nome = Rostos.nomeDupla(temporada, d);
            c.drawText(nome, (l + r) * 0.5f, bo - 16f, BotaoMenu.fit(c, nome, 20f, CARD_W - 20f, true),
                Tinta.withAlpha(0xFFFFFFFF, alpha), Tinta.withAlpha(0xFF0A0A0A, alpha), 3f, true);
        }
        int borda = sel ? 0xFFFFFFFF : livre ? BotaoMenu.RED : GRAY;
        c.strokePolygon(caixa, sel ? 4f : 2f, Tinta.withAlpha(borda, alpha));
    }
}
