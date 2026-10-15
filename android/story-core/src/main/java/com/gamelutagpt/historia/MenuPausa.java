package com.gamelutagpt.historia;

import com.gamelutagpt.render.RenderCanvas;

/**
 * Pausa por cima da história ou da luta (botão MENU ou a tecla voltar): CONTINUAR, a VELOCIDADE
 * da luta ou voltar ao MENU INICIAL. No modo história avisa que o progresso do episódio se perde. Quem chama para
 * o jogo enquanto a pausa está aberta e desenha a pausa por cima da tela parada.
 *
 * Não depende de plataforma e não é thread-safe: chame tudo da thread do loop do jogo.
 */
public final class MenuPausa {
    public enum Opcao { CONTINUAR, VELOCIDADE, MENU }

    public static final String PAUSA = "PAUSA";
    public static final String CONTINUAR = "CONTINUAR";
    public static final String VELOCIDADE = "VELOCIDADE";
    public static final String MENU_INICIAL = "MENU INICIAL";
    public static final String AVISO = "Voltar ao menu perde o progresso deste episódio.";
    /** O botão que abre a pausa na história e nas lutas dela. */
    public static final String MENU = "MENU";
    public static final String[] TEXTOS = {PAUSA, CONTINUAR, VELOCIDADE, MENU_INICIAL, AVISO, MENU};

    /** Velocidades da luta (1 = original); quem usa a pausa lê e guarda a escolhida. */
    public static final float[] VELOCIDADES = {0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1f};
    public static final float VELOCIDADE_PADRAO = 0.8f;

    static final float[][] CAIXAS = {
        {420f, 300f, 860f, 366f},
        {420f, 390f, 860f, 456f},
        {420f, 480f, 860f, 546f},
    };
    /** O mesmo toque que abriu a pausa não pode escolher nada. */
    static final float TAP_DELAY = 0.2f;

    private Temporada temporada;
    private boolean open;
    private boolean aviso;
    private int cursor;
    private float time;
    private int velocidade = nivel(VELOCIDADE_PADRAO);

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

    /** Multiplicador do relógio da luta. */
    public float velocidade() {
        return VELOCIDADES[velocidade];
    }

    /** Restaura uma velocidade salva (a mais próxima da lista). */
    public void setVelocidade(float valor) {
        velocidade = nivel(valor);
    }

    private static int nivel(float valor) {
        int melhor = 0;
        for (int i = 1; i < VELOCIDADES.length; i++) {
            if (Math.abs(VELOCIDADES[i] - valor) < Math.abs(VELOCIDADES[melhor] - valor)) melhor = i;
        }
        return melhor;
    }

    /**
     * ◀ ▶ com o cursor em VELOCIDADE: mais devagar ou mais rápido (para nas pontas).
     * Devolve false com o cursor em outra opção, para quem chama mover o cursor.
     */
    public boolean ajustar(int direcao) {
        if (!open || cursor() != Opcao.VELOCIDADE) return false;
        if (direcao != 0) velocidade = Math.max(0, Math.min(VELOCIDADES.length - 1, velocidade + (direcao > 0 ? 1 : -1)));
        return true;
    }

    public void update(float dt) {
        if (open) time += dt;
    }

    public void mover(int direcao) {
        if (!open || direcao == 0) return;
        int n = Opcao.values().length;
        cursor = ((cursor + (direcao > 0 ? 1 : -1)) % n + n) % n;
    }

    /**
     * Controle: a opção do cursor. A pausa fecha; null se ainda não pode escolher. Em
     * VELOCIDADE a pausa fica aberta e a velocidade passa para a próxima (dá a volta).
     */
    public Opcao confirmar() {
        if (!open || time < TAP_DELAY) return null;
        if (cursor() == Opcao.VELOCIDADE) {
            velocidade = (velocidade + 1) % VELOCIDADES.length;
            return null;
        }
        open = false;
        return cursor();
    }

    /** Voltar de novo = continuar. */
    public Opcao voltar() {
        if (!open) return null;
        open = false;
        return Opcao.CONTINUAR;
    }

    /**
     * Toque em (x, y), em coordenadas 1280x720: a opção tocada, ou null (a pausa continua aberta).
     * Em VELOCIDADE a metade esquerda deixa mais devagar e a direita mais rápido.
     */
    public Opcao tap(float x, float y) {
        if (!open || time < TAP_DELAY) return null;
        for (int i = 0; i < CAIXAS.length; i++) {
            if (BotaoMenu.dentro(CAIXAS[i], x, y)) {
                cursor = i;
                if (cursor() == Opcao.VELOCIDADE) {
                    ajustar(x < (CAIXAS[i][0] + CAIXAS[i][2]) * 0.5f ? -1 : 1);
                    return null;
                }
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
        String[] labels = {t(CONTINUAR), rotuloVelocidade(), t(MENU_INICIAL)};
        for (int i = 0; i < CAIXAS.length; i++) {
            BotaoMenu.draw(c, CAIXAS[i], labels[i], null, i == cursor, time, 0f, (int)(255f * in));
        }
        if (aviso) {
            String texto = t(AVISO);
            c.drawText(texto, cx, 596f, BotaoMenu.fit(c, texto, 20f, 900f, false),
                Tinta.withAlpha(0xFFB9C0D0, (int)(255f * in)), 0, 0f, false);
        }
    }

    /** "VELOCIDADE  ◀ 80% ▶": as setas somem na ponta em que não dá mais para ir. */
    private String rotuloVelocidade() {
        String esq = velocidade > 0 ? "◀ " : "   ";
        String dir = velocidade < VELOCIDADES.length - 1 ? " ▶" : "   ";
        return t(VELOCIDADE) + "  " + esq + Math.round(velocidade() * 100f) + "%" + dir;
    }
}
