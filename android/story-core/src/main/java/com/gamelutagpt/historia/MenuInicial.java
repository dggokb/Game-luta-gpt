package com.gamelutagpt.historia;

import com.gamelutagpt.render.Rand;
import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import java.io.IOException;

/**
 * Menu inicial do jogo: o logo da temporada e as entradas MODO HISTÓRIA (o jogo de verdade, do
 * Prólogo em diante), ARCADE (o torneio de 16 duplas, veja {@link Arcade}) e MODO TESTE (a luta
 * de teste com a seleção de dupla, os botões de teste e o DEBUG). Os textos passam pela tradução da temporada
 * ({@code historia/idiomas/<código>.json}), como as falas da história.
 *
 * Toque: a opção tocada é escolhida. Controle: {@link #mover} e {@link #confirmar}. A escolha
 * pisca, a tela escurece e {@link #update} devolve a opção uma vez, já com o menu fechado.
 *
 * Não depende de plataforma e não é thread-safe: chame tudo da thread do loop do jogo.
 */
public final class MenuInicial {
    public enum Opcao { HISTORIA, ARCADE, TESTE }

    public static final float VW = PaginaHistoria.VW;
    public static final float VH = PaginaHistoria.VH;

    /** Textos do menu (chaves da tradução). */
    public static final String HISTORIA = "MODO HISTÓRIA";
    public static final String HISTORIA_SUB = "A jornada dos irmãos Komyo, desde o Prólogo";
    public static final String ARCADE = "ARCADE";
    public static final String ARCADE_SUB = "Torneio de 16 duplas: monte a sua";
    public static final String TESTE = "MODO TESTE";
    public static final String TESTE_SUB = "Luta livre para testar golpes, sprites e ajustes";
    public static final String DICA = "Toque numa opção  •  Controle: ▲▼ e A";
    /** Todos os textos que precisam de tradução. */
    public static final String[] TEXTOS = {HISTORIA, HISTORIA_SUB, ARCADE, ARCADE_SUB, TESTE, TESTE_SUB, DICA};
    private static final String[] ROTULOS = {HISTORIA, ARCADE, TESTE};
    private static final String[] SUBTITULOS = {HISTORIA_SUB, ARCADE_SUB, TESTE_SUB};

    /** Caixas das opções: {esquerda, topo, direita, base}, na ordem de {@link Opcao}. */
    static final float[][] CAIXAS = {
        {370f, 436f, 910f, 504f},
        {370f, 514f, 910f, 582f},
        {370f, 592f, 910f, 660f},
    };
    static final float LOGO_TOP = 20f;
    static final float LOGO_HEIGHT = 400f;

    /** Antes disso nem o toque nem o controle escolhem (o toque que abriu o menu não pode valer). */
    static final float TAP_DELAY = 0.3f;
    static final float FLASH = 0.16f;
    static final float FADE_OUT = 0.34f;

    private static final String LOGO_PADRAO = Temporada.PASTA + "/logo.jpg";
    private static final int BLUE = 0xFF2F6FD6;
    private static final int GRAY = 0xFF8A93A8;

    private final RenderAssets assets;
    private final Rand rand = new Rand(11);
    private Temporada temporada;
    private RenderImage logo;
    private String logoPath;
    private String versao = "";
    private boolean open;
    private int cursor;
    private float time;
    /** Opção escolhida (piscando e escurecendo), ou null. */
    private Opcao escolha;
    private float escolhaAt;

    public MenuInicial(RenderAssets assets) {
        this.assets = assets;
    }

    /** Abre o menu. A temporada dá o logo e o idioma; sem ela, o logo padrão e o português. */
    public void abrir(Temporada temporada) {
        this.temporada = temporada;
        open = true;
        time = 0f;
        escolha = null;
        loadLogo(temporada != null && temporada.logo != null ? Temporada.PASTA + "/" + temporada.logo : LOGO_PADRAO);
    }

    private void loadLogo(String path) {
        if (path.equals(logoPath) && logo != null) return;
        logoPath = path;
        try {
            logo = assets.loadImage(path);
        } catch (IOException | RuntimeException ex) {
            // Sem a imagem, o menu escreve o nome do jogo.
            logo = null;
        }
    }

    /** Fecha sem escolher e solta o logo da memória. */
    public void fechar() {
        open = false;
        escolha = null;
        logo = null;
        logoPath = null;
    }

    public boolean isOpen() {
        return open;
    }

    /** Texto pequeno no canto de baixo (a versão do jogo). */
    public void setVersao(String versao) {
        this.versao = versao == null ? "" : versao;
    }

    public Opcao cursor() {
        return Opcao.values()[cursor];
    }

    /** Controle: -1 sobe, +1 desce (dá a volta nas pontas). */
    public void mover(int direcao) {
        if (!open || escolha != null || direcao == 0) return;
        int n = Opcao.values().length;
        cursor = ((cursor + (direcao > 0 ? 1 : -1)) % n + n) % n;
    }

    /** Controle: escolhe a opção do cursor. */
    public void confirmar() {
        if (!open || escolha != null || time < TAP_DELAY) return;
        escolher(cursor());
    }

    /** Toque em (x, y), em coordenadas de tela 1280x720: a opção tocada é escolhida. */
    public void tap(float x, float y) {
        if (!open || escolha != null || time < TAP_DELAY) return;
        for (int i = 0; i < CAIXAS.length; i++) {
            if (BotaoMenu.dentro(CAIXAS[i], x, y)) {
                cursor = i;
                escolher(Opcao.values()[i]);
                return;
            }
        }
    }

    private void escolher(Opcao opcao) {
        escolha = opcao;
        escolhaAt = time;
    }

    /** Avança o relógio; devolve a opção escolhida quando a tela termina de escurecer (uma vez). */
    public Opcao update(float dt) {
        if (!open) return null;
        time += dt;
        if (escolha != null && time - escolhaAt >= FLASH + FADE_OUT) {
            Opcao escolhida = escolha;
            fechar();
            return escolhida;
        }
        return null;
    }

    private String t(String texto) {
        return temporada != null ? temporada.t(texto) : texto;
    }

    // ------------------------------------------------------------ desenho

    public void render(RenderCanvas c) {
        if (!open) return;
        c.fillRect(-40f, -40f, VW + 40f, VH + 40f, 0xFF000000);
        // As cores do logo: o dragão azul à esquerda, o tigre vermelho à direita. Os brilhos
        // ficam nas laterais para o fundo preto do logo emendar no preto da tela.
        float breathe = 0.85f + 0.15f * (float)Math.sin(time * 1.3f);
        c.fillRadialGlow(30f, 330f, 330f, Tinta.withAlpha(BLUE, (int)(90 * breathe)));
        c.fillRadialGlow(VW - 30f, 330f, 330f, Tinta.withAlpha(BotaoMenu.RED, (int)(90 * breathe)));
        drawLogo(c);
        drawEmbers(c);

        Opcao[] opcoes = Opcao.values();
        for (int i = 0; i < opcoes.length; i++) {
            float in = Tinta.easeOutCubic((time - 0.35f - i * 0.1f) / 0.3f);
            if (in <= 0f) continue;
            boolean chosen = escolha == opcoes[i];
            float flash = chosen ? 1f - Tinta.clamp01((time - escolhaAt) / FLASH) : 0f;
            c.save();
            c.translate(-(1f - in) * 260f, 0f);
            BotaoMenu.draw(c, CAIXAS[i], t(ROTULOS[i]), t(SUBTITULOS[i]),
                i == cursor, time, flash, (int)(255f * in));
            c.restore();
        }

        float hintIn = Tinta.clamp01((time - 0.8f) / 0.4f);
        if (hintIn > 0f) {
            String dica = t(DICA);
            c.drawText(dica, VW * 0.5f, 692f, BotaoMenu.fit(c, dica, 18f, 900f, false),
                Tinta.withAlpha(GRAY, (int)(255f * hintIn)), 0, 0f, false);
        }
        if (!versao.isEmpty()) c.drawText(versao, VW - 120f, VH - 12f, 13f, 0xFF4E5568, 0, 0f, false);

        float fadeIn = 1f - Tinta.clamp01(time / 0.3f);
        float fadeOut = escolha != null ? Tinta.clamp01((time - escolhaAt - FLASH) / FADE_OUT) : 0f;
        float fade = Math.max(fadeIn, fadeOut);
        if (fade > 0f) c.fillRect(-40f, -40f, VW + 40f, VH + 40f, Tinta.withAlpha(0xFF000000, (int)(255f * fade)));
    }

    private void drawLogo(RenderCanvas c) {
        float in = Tinta.easeOutCubic(time / 0.6f);
        float scale = 1.06f - 0.06f * in;
        c.save();
        c.scale(scale, scale, VW * 0.5f, LOGO_TOP + LOGO_HEIGHT * 0.5f);
        if (logo != null) {
            float w = LOGO_HEIGHT * logo.width() / logo.height();
            c.drawImage(logo, VW * 0.5f - w * 0.5f, LOGO_TOP, VW * 0.5f + w * 0.5f, LOGO_TOP + LOGO_HEIGHT, (int)(255f * in));
        } else {
            // Sem a imagem: o nome do jogo numa pincelada vermelha.
            String titulo = temporada != null ? temporada.titulo : "SŌKON: BLOODLINES";
            float[] band = {120f, 200f, 1160f, 170f, 1160f, 300f, 120f, 330f};
            c.fillPolygon(band, Tinta.withAlpha(BotaoMenu.RED, (int)(255f * in)));
            float size = BotaoMenu.fit(c, titulo, 88f, 1000f, true);
            c.drawText(titulo, VW * 0.5f, 282f, size, Tinta.withAlpha(BotaoMenu.WHITE, (int)(255f * in)),
                Tinta.withAlpha(BotaoMenu.INK, (int)(255f * in)), size * 0.12f, true);
            if (temporada != null && !temporada.subtitulo.isEmpty()) {
                c.drawText(temporada.subtitulo, VW * 0.5f, 390f, 44f, Tinta.withAlpha(BotaoMenu.RED, (int)(255f * in)),
                    Tinta.withAlpha(BotaoMenu.WHITE, (int)(255f * in)), 3f, false);
            }
        }
        c.restore();
    }

    /** Fagulhas azuis e vermelhas subindo devagar, como as tintas do logo. */
    private void drawEmbers(RenderCanvas c) {
        for (int i = 0; i < 40; i++) {
            rand.reset(i * 7919L + 3L);
            boolean blue = i % 2 == 0;
            float x0 = blue ? rand.range(0f, VW * 0.55f) : rand.range(VW * 0.45f, VW);
            float speed = rand.range(18f, 55f);
            float phase = rand.range(0f, 800f);
            float radius = rand.range(1.4f, 3.6f);
            float travel = (phase + time * speed) % 800f;
            float y = VH + 30f - travel;
            float x = x0 + (float)Math.sin((time + phase) * 0.8f) * 14f;
            int alpha = (int)(170f * Tinta.clamp01(y / VH) * Tinta.clamp01(travel / 60f));
            if (alpha <= 0) continue;
            c.fillCircle(x, y, radius, Tinta.withAlpha(blue ? 0xFF7FC0FF : 0xFFFF6A50, alpha));
        }
    }
}
