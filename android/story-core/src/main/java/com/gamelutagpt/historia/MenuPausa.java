package com.gamelutagpt.historia;

import com.gamelutagpt.render.RenderCanvas;

/**
 * Pausa por cima da história ou da luta (botão MENU ou a tecla voltar): CONTINUAR ou voltar ao
 * MENU INICIAL. No modo história avisa que o progresso do episódio se perde. Quem chama para
 * o jogo enquanto a pausa está aberta e desenha a pausa por cima da tela parada.
 *
 * Não depende de plataforma e não é thread-safe: chame tudo da thread do loop do jogo.
 */
public final class MenuPausa {
    public enum Opcao { CONTINUAR, MENU }

    public static final String PAUSA = "PAUSA";
    public static final String CONTINUAR = "CONTINUAR";
    public static final String MENU_INICIAL = "MENU INICIAL";
    public static final String AVISO = "Voltar ao menu perde o progresso deste episódio.";
    /** O botão que abre a pausa na história e nas lutas dela. */
    public static final String MENU = "MENU";
    public static final String[] TEXTOS = {PAUSA, CONTINUAR, MENU_INICIAL, AVISO, MENU};

    static final float[][] CAIXAS = {
        {420f, 300f, 860f, 366f},
        {420f, 390f, 860f, 456f},
    };
    /** O mesmo toque que abriu a pausa não pode escolher nada. */
    static final float TAP_DELAY = 0.2f;

    private Temporada temporada;
    private boolean open;
    private boolean aviso;
    private int cursor;
    private float time;

    /** Abre com o cursor em CONTINUAR. {@code aviso}: mostra que o episódio se perde. */
    public void abrir(Temporada temporada, boolean aviso) {
        this.temporada = temporada;
        this.aviso = aviso;
        open = true;
        cursor = 0;
        time = 0f;
    }

    public void fechar() {
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    public Opcao cursor() {
        return Opcao.values()[cursor];
    }

    public void update(float dt) {
        if (open) time += dt;
    }

    public void mover(int direcao) {
        if (!open || direcao == 0) return;
        int n = Opcao.values().length;
        cursor = ((cursor + (direcao > 0 ? 1 : -1)) % n + n) % n;
    }

    /** Controle: a opção do cursor. A pausa fecha; null se ainda não pode escolher. */
    public Opcao confirmar() {
        if (!open || time < TAP_DELAY) return null;
        open = false;
        return cursor();
    }

    /** Voltar de novo = continuar. */
    public Opcao voltar() {
        if (!open) return null;
        open = false;
        return Opcao.CONTINUAR;
    }

    /** Toque em (x, y), em coordenadas 1280x720: a opção tocada, ou null (a pausa continua aberta). */
    public Opcao tap(float x, float y) {
        if (!open || time < TAP_DELAY) return null;
        for (int i = 0; i < CAIXAS.length; i++) {
            if (BotaoMenu.dentro(CAIXAS[i], x, y)) {
                cursor = i;
                open = false;
                return Opcao.values()[i];
            }
        }
        return null;
    }

    private String t(String texto) {
        return temporada != null ? temporada.t(texto) : texto;
    }

    public void render(RenderCanvas c) {
        if (!open) return;
        float in = Tinta.easeOutCubic(time / 0.18f);
        c.fillRect(-40f, -40f, PaginaHistoria.VW + 40f, PaginaHistoria.VH + 40f, Tinta.withAlpha(0xFF000000, (int)(185f * in)));
        float cx = PaginaHistoria.VW * 0.5f;
        // Pincelada vermelha atrás do título, como no cartão de título dos episódios.
        float[] band = {cx - 330f, 196f, cx + 330f, 178f, cx + 330f, 262f, cx - 330f, 280f};
        c.fillPolygon(band, Tinta.withAlpha(BotaoMenu.RED, (int)(255f * in)));
        String titulo = t(PAUSA);
        float size = BotaoMenu.fit(c, titulo, 64f, 600f, true);
        c.drawText(titulo, cx, 252f, size, Tinta.withAlpha(BotaoMenu.WHITE, (int)(255f * in)),
            Tinta.withAlpha(BotaoMenu.INK, (int)(255f * in)), size * 0.14f, true);
        String[] labels = {t(CONTINUAR), t(MENU_INICIAL)};
        for (int i = 0; i < CAIXAS.length; i++) {
            BotaoMenu.draw(c, CAIXAS[i], labels[i], null, i == cursor, time, 0f, (int)(255f * in));
        }
        if (aviso) {
            String texto = t(AVISO);
            c.drawText(texto, cx, 506f, BotaoMenu.fit(c, texto, 20f, 900f, false),
                Tinta.withAlpha(0xFFB9C0D0, (int)(255f * in)), 0, 0f, false);
        }
    }
}
