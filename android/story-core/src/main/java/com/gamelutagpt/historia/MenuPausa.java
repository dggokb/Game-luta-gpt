package com.gamelutagpt.historia;

import com.gamelutagpt.render.RenderCanvas;

/**
 * Pausa por cima da história ou da luta (botão MENU ou a tecla voltar): CONTINUAR, a VELOCIDADE
 * da luta, o IMPACTO (impact frames ligados ou não: a tela pisca nos golpes grandes), o NÍVEL da CPU
 * (1 a 8), a lista de COMANDOS de cada personagem ({@link TelaComandos}), os GRÁFICOS (quem usa a
 * pausa diz o rótulo e o que muda, {@link Graficos}), o FPS (o contador no canto) ou voltar ao MENU INICIAL. No modo história avisa que o progresso do episódio se perde. Quem chama para
 * o jogo enquanto a pausa está aberta e desenha a pausa por cima da tela parada.
 *
 * Não depende de plataforma e não é thread-safe: chame tudo da thread do loop do jogo.
 */
public final class MenuPausa {
    public enum Opcao { CONTINUAR, VELOCIDADE, IMPACTO, NIVEL_CPU, COMANDOS, GRAFICOS, FPS, MENU }

    public static final String PAUSA = "PAUSA";
    public static final String CONTINUAR = "CONTINUAR";
    public static final String VELOCIDADE = "VELOCIDADE";
    public static final String IMPACTO = "IMPACTO";
    public static final String LIGADO = "LIGADO";
    public static final String DESLIGADO = "DESLIGADO";
    public static final String COMANDOS = "COMANDOS";
    public static final String NIVEL_CPU = "NÍVEL CPU";
    public static final String GRAFICOS = "GRÁFICOS";
    /** O contador de quadros por segundo no canto da tela. */
    public static final String FPS = "FPS";
    /** Níveis dos gráficos (quem usa a pausa monta o rótulo com eles). */
    public static final String[] NIVEIS_GRAFICOS = {"AUTO", "ALTO", "MÉDIO", "LEVE"};
    /** Versão leve: no lugar do menu inicial, recomeça a luta. */
    public static final String MENU_INICIAL = "REINICIAR LUTA";
    public static final String AVISO = "Voltar ao menu perde o progresso deste episódio.";
    public static final String AVISO_ARCADE = "Voltar ao menu encerra o Arcade.";
    /** O botão que abre a pausa na história e nas lutas dela. */
    public static final String MENU = "MENU";
    public static final String[] TEXTOS = {PAUSA, CONTINUAR, VELOCIDADE, IMPACTO, LIGADO, DESLIGADO, NIVEL_CPU,
        COMANDOS, GRAFICOS, "AUTO", "ALTO", "MÉDIO", "LEVE", FPS, MENU_INICIAL, AVISO, AVISO_ARCADE, MENU};

    /** A opção GRÁFICOS: quem usa a pausa diz o que mostrar e o que fazer ao trocar. */
    public interface Graficos {
        /** O valor ao lado de GRÁFICOS ({@code traduzir} usa o idioma da temporada). */
        String rotulo(java.util.function.UnaryOperator<String> traduzir);
        /** ◀ ▶ (ou A, que vai para a frente): o próximo nível. */
        void mudar(int direcao);
    }

    /** Velocidades da luta (1 = original); quem usa a pausa lê e guarda a escolhida. */
    public static final float[] VELOCIDADES = {0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1f};
    public static final float VELOCIDADE_PADRAO = 1f;
    /** Níveis da CPU (o motor tem de 1 a 8); quem usa a pausa lê e guarda o escolhido. */
    public static final int NIVEL_MIN = 1, NIVEL_MAX = 8, NIVEL_PADRAO = 5;

    // Oito opções entre o título e o aviso: 42 de altura, 47 entre uma e outra.
    static final float[][] CAIXAS = {
        {420f, 290f, 860f, 332f},
        {420f, 337f, 860f, 379f},
        {420f, 384f, 860f, 426f},
        {420f, 431f, 860f, 473f},
        {420f, 478f, 860f, 520f},
        {420f, 525f, 860f, 567f},
        {420f, 572f, 860f, 614f},
        {420f, 619f, 860f, 661f},
    };
    /** O mesmo toque que abriu a pausa não pode escolher nada. */
    static final float TAP_DELAY = 0.2f;

    private boolean open;
    /** O aviso embaixo das opções, ou null. */
    private String aviso;
    private int cursor;
    private float time;
    private int velocidade = nivel(VELOCIDADE_PADRAO);
    private boolean impacto = true;
    private boolean fps = true;
    private int nivelCpu = NIVEL_PADRAO;
    private final TelaComandos comandos = new TelaComandos();
    private Graficos graficos;

    public void setGraficos(Graficos graficos) {
        this.graficos = graficos;
    }

    /** Abre com o cursor em CONTINUAR e o {@code aviso} embaixo (null = sem aviso). */
    public void abrir(String aviso) {
        this.aviso = aviso;
        open = true;
        cursor = 0;
        time = 0f;
        comandos.fechar();
    }

    public void fechar() {
        open = false;
        comandos.fechar();
    }

    /** A lista de COMANDOS: quem abre a pausa entrega as fichas dos personagens. */
    public TelaComandos comandos() {
        return comandos;
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

    /** Impact frames ligados (a tela vira silhueta por 2–3 quadros nos golpes grandes). */
    public boolean impacto() {
        return impacto;
    }

    public void setImpacto(boolean ligado) {
        impacto = ligado;
    }

    /** O contador de quadros por segundo no canto da tela. */
    public boolean fps() {
        return fps;
    }

    public void setFps(boolean ligado) {
        fps = ligado;
    }

    /** Nível da CPU, de 1 a 8. */
    public int nivelCpu() {
        return nivelCpu;
    }

    /** Restaura um nível salvo (fora de 1 a 8 vira o mais próximo). */
    public void setNivelCpu(int nivel) {
        nivelCpu = Math.max(NIVEL_MIN, Math.min(NIVEL_MAX, nivel));
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
        if (open && comandos.isOpen()) {
            // Na lista de comandos, ◀ ▶ trocam o personagem.
            comandos.trocar(direcao);
            return true;
        }
        if (open && cursor() == Opcao.IMPACTO) {
            if (direcao != 0) impacto = !impacto;
            return true;
        }
        if (open && cursor() == Opcao.FPS) {
            if (direcao != 0) fps = !fps;
            return true;
        }
        if (open && cursor() == Opcao.NIVEL_CPU) {
            if (direcao != 0) setNivelCpu(nivelCpu + (direcao > 0 ? 1 : -1));
            return true;
        }
        if (open && cursor() == Opcao.GRAFICOS) {
            if (direcao != 0 && graficos != null) graficos.mudar(direcao);
            return true;
        }
        if (!open || cursor() != Opcao.VELOCIDADE) return false;
        if (direcao != 0) velocidade = Math.max(0, Math.min(VELOCIDADES.length - 1, velocidade + (direcao > 0 ? 1 : -1)));
        return true;
    }

    public void update(float dt) {
        if (open) time += dt;
        comandos.update(dt);
    }

    public void mover(int direcao) {
        if (!open || direcao == 0 || comandos.isOpen()) return;
        int n = Opcao.values().length;
        cursor = ((cursor + (direcao > 0 ? 1 : -1)) % n + n) % n;
    }

    /**
     * Controle: a opção do cursor. A pausa fecha; null se ainda não pode escolher. Em
     * VELOCIDADE a pausa fica aberta e a velocidade passa para a próxima (dá a volta).
     */
    public Opcao confirmar() {
        if (!open || time < TAP_DELAY) return null;
        if (comandos.isOpen()) {
            comandos.trocar(1);
            return null;
        }
        if (cursor() == Opcao.VELOCIDADE) {
            velocidade = (velocidade + 1) % VELOCIDADES.length;
            return null;
        }
        if (cursor() == Opcao.IMPACTO) {
            impacto = !impacto;
            return null;
        }
        if (cursor() == Opcao.FPS) {
            fps = !fps;
            return null;
        }
        if (cursor() == Opcao.NIVEL_CPU) {
            nivelCpu = nivelCpu % NIVEL_MAX + 1;   // dá a volta: 8 → 1
            return null;
        }
        if (cursor() == Opcao.COMANDOS) {
            comandos.abrir();
            return null;
        }
        if (cursor() == Opcao.GRAFICOS) {
            if (graficos != null) graficos.mudar(1);
            return null;
        }
        open = false;
        return cursor();
    }

    /** Voltar de novo = continuar (na lista de comandos, volta para a pausa). */
    public Opcao voltar() {
        if (!open) return null;
        if (comandos.isOpen()) {
            comandos.fechar();
            return null;
        }
        open = false;
        return Opcao.CONTINUAR;
    }

    /**
     * Toque em (x, y), em coordenadas 1280x720: a opção tocada, ou null (a pausa continua aberta).
     * Em VELOCIDADE a metade esquerda deixa mais devagar e a direita mais rápido.
     */
    public Opcao tap(float x, float y) {
        if (!open || time < TAP_DELAY) return null;
        if (comandos.isOpen()) {
            // VOLTAR fecha a lista; o mesmo toque não pode escolher uma opção da pausa.
            if (comandos.tap(x, y)) time = 0f;
            return null;
        }
        for (int i = 0; i < CAIXAS.length; i++) {
            if (BotaoMenu.dentro(CAIXAS[i], x, y)) {
                cursor = i;
                if (cursor() == Opcao.VELOCIDADE) {
                    ajustar(x < (CAIXAS[i][0] + CAIXAS[i][2]) * 0.5f ? -1 : 1);
                    return null;
                }
                if (cursor() == Opcao.IMPACTO) {
                    impacto = !impacto;
                    return null;
                }
                if (cursor() == Opcao.FPS) {
                    fps = !fps;
                    return null;
                }
                if (cursor() == Opcao.NIVEL_CPU) {
                    ajustar(x < (CAIXAS[i][0] + CAIXAS[i][2]) * 0.5f ? -1 : 1);
                    return null;
                }
                if (cursor() == Opcao.COMANDOS) {
                    comandos.abrir();
                    return null;
                }
                if (cursor() == Opcao.GRAFICOS) {
                    if (graficos != null) graficos.mudar(x < (CAIXAS[i][0] + CAIXAS[i][2]) * 0.5f ? -1 : 1);
                    return null;
                }
                open = false;
                return Opcao.values()[i];
            }
        }
        return null;
    }

    private String t(String texto) {
        return texto;
    }

    public void render(RenderCanvas c) {
        if (!open) return;
        if (comandos.isOpen()) {
            comandos.render(c);
            return;
        }
        float in = Tinta.easeOutCubic(time / 0.18f);
        c.fillRect(-40f, -40f, TelaComandos.VW + 40f, TelaComandos.VH + 40f, Tinta.withAlpha(0xFF000000, (int)(185f * in)));
        float cx = TelaComandos.VW * 0.5f;
        // Pincelada vermelha atrás do título, como no cartão de título dos episódios.
        float[] band = {cx - 330f, 196f, cx + 330f, 178f, cx + 330f, 262f, cx - 330f, 280f};
        c.fillPolygon(band, Tinta.withAlpha(BotaoMenu.RED, (int)(255f * in)));
        String titulo = t(PAUSA);
        float size = BotaoMenu.fit(c, titulo, 64f, 600f, true);
        c.drawText(titulo, cx, 252f, size, Tinta.withAlpha(BotaoMenu.WHITE, (int)(255f * in)),
            Tinta.withAlpha(BotaoMenu.INK, (int)(255f * in)), size * 0.14f, true);
        String[] labels = {t(CONTINUAR), rotuloVelocidade(), t(IMPACTO) + ":  " + t(impacto ? LIGADO : DESLIGADO),
            rotuloNivel(), t(COMANDOS), rotuloGraficos(), t(FPS) + ":  " + t(fps ? LIGADO : DESLIGADO),
            t(MENU_INICIAL)};
        for (int i = 0; i < CAIXAS.length; i++) {
            BotaoMenu.draw(c, CAIXAS[i], labels[i], null, i == cursor, time, 0f, (int)(255f * in));
        }
        if (aviso != null) {
            String texto = t(aviso);
            c.drawText(texto, cx, 690f, BotaoMenu.fit(c, texto, 20f, 900f, false),
                Tinta.withAlpha(0xFFB9C0D0, (int)(255f * in)), 0, 0f, false);
        }
    }

    /** "GRÁFICOS  ◀ AUTO (ALTO) ▶" (sem quem diga o valor, só o nome). */
    private String rotuloGraficos() {
        if (graficos == null) return t(GRAFICOS);
        return t(GRAFICOS) + "  ◀ " + graficos.rotulo(this::t) + " ▶";
    }

    /** "NÍVEL CPU  ◀ 5 ▶": as setas somem na ponta em que não dá mais para ir. */
    private String rotuloNivel() {
        String esq = nivelCpu > NIVEL_MIN ? "◀ " : "   ";
        String dir = nivelCpu < NIVEL_MAX ? " ▶" : "   ";
        return t(NIVEL_CPU) + "  " + esq + nivelCpu + dir;
    }

    /** "VELOCIDADE  ◀ 80% ▶": as setas somem na ponta em que não dá mais para ir. */
    private String rotuloVelocidade() {
        String esq = velocidade > 0 ? "◀ " : "   ";
        String dir = velocidade < VELOCIDADES.length - 1 ? " ▶" : "   ";
        return t(VELOCIDADE) + "  " + esq + Math.round(velocidade() * 100f) + "%" + dir;
    }
}
