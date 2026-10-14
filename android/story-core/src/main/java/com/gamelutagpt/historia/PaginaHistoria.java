package com.gamelutagpt.historia;

import com.gamelutagpt.render.Rand;
import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Toca um episódio do modo história como mangá animado, com a mesma linguagem da Página
 * Final do ultra: cada página se monta painel por painel (o painel "bate" na página), os
 * balões estalam e o texto é digitado; o toque avança. Entre as páginas o episódio pode
 * pedir uma luta: o jogo assume a tela e devolve o resultado em {@link #resultadoLuta}.
 *
 * Não depende de plataforma e não é thread-safe: chame tudo da thread do loop do jogo.
 */
public final class PaginaHistoria {
    public static final float VW = 1280f;
    public static final float VH = 720f;

    static final float PAGE_FADE = 0.22f;
    static final float SLAM_DURATION = 0.14f;
    /** Depois que o painel pousa, o primeiro balão dele entra sozinho. */
    static final float FIRST_BALLOON_DELAY = 0.32f;
    static final float BALLOON_POP = 0.16f;
    /** Caracteres por segundo na digitação dos balões. */
    static final float TYPE_SPEED = 48f;
    static final float EXIT_DURATION = 0.26f;
    static final float TITLE_DURATION = 2.8f;
    static final float TITLE_MIN_TAP = 0.5f;
    static final float NEXT_MIN_TAP = 0.8f;

    /** Botão PULAR (canto de cima à direita). */
    public static final float SKIP_LEFT = 1132f, SKIP_TOP = 0f, SKIP_RIGHT = 1270f, SKIP_BOTTOM = 34f;
    /** Botão MENU (canto de cima à esquerda), só quando o jogo liga com {@link #setBotaoMenu}. */
    public static final float MENU_LEFT = 10f, MENU_TOP = 0f, MENU_RIGHT = 148f, MENU_BOTTOM = 34f;

    private static final int PAPER = 0xFFF1ECE1;
    private static final int FLASHBACK_PAPER = 0xFF0C0C0C;
    private static final int SEPIA = 0x4A7A4A1E;
    private static final int INK = Baloes.INK;
    private static final int WHITE = Baloes.WHITE;
    private static final int RED = 0xFFC8202C;

    private final HistoriaListener listener;
    private final RenderAssets assets;
    private final ArteHistoria arte = new ArteHistoria();
    private final Baloes baloes = new Baloes();
    private final PaginaGuiada guiada = new PaginaGuiada();
    private final Rand rand = new Rand(5);
    private final Map<String, RenderImage> images = new HashMap<>();
    private final List<String> warnings = new ArrayList<>();

    private Temporada temporada;
    private RenderImage logo;
    private String logoPath;
    private Episodio episodio;
    private int stepIndex = -1;
    private float time;
    private boolean active;
    private boolean waitingFight;
    private float exitAt = -1f;
    private float shake;
    private long frame;
    private boolean botaoMenu;

    // Página atual: quando cada painel e cada balão apareceu (-1 = ainda não).
    private float[] panelAt = new float[0];
    private float[][] balloonAt = new float[0][];
    /** Sequência de batidas da página: {painel, balão} (balão -1 = o painel em si). */
    private final List<int[]> beats = new ArrayList<>();
    private int beat = -1;

    public PaginaHistoria(HistoriaListener listener, RenderAssets assets) {
        this.listener = listener;
        this.assets = assets;
    }

    public void start(Temporada temporada, Episodio episodio) {
        this.temporada = temporada;
        this.episodio = episodio;
        active = true;
        waitingFight = false;
        warnings.clear();
        loadLogo();
        stepIndex = -1;
        nextStep();
    }

    private void loadLogo() {
        String path = temporada.logo != null ? Temporada.PASTA + "/" + temporada.logo : null;
        if (path == null ? logoPath == null : path.equals(logoPath)) return;
        logoPath = path;
        logo = null;
        if (path == null) return;
        try {
            logo = assets.loadImage(path);
        } catch (IOException | RuntimeException ex) {
            warnings.add(path + ": " + ex.getMessage());
        }
    }

    /** Abandona o episódio sem avisar o jogo (ex.: o jogador voltou para a seleção). */
    public void stop() {
        active = false;
        waitingFight = false;
        releaseImages();
    }

    /** Mostra o botão MENU (o modo história do menu inicial; o jogo abre a pausa no toque). */
    public void setBotaoMenu(boolean visivel) {
        botaoMenu = visivel;
    }

    /** O toque em (x, y) caiu no botão MENU (visível, com a página na tela). */
    public boolean menuAt(float x, float y) {
        return botaoMenu && coversScreen() && x >= MENU_LEFT && x <= MENU_RIGHT && y >= MENU_TOP && y <= MENU_BOTTOM;
    }

    /** Verdadeiro do começo ao fim do episódio, inclusive durante as lutas dele. */
    public boolean isActive() {
        return active;
    }

    /** Verdadeiro enquanto a história ocupa a tela (falso durante uma luta). */
    public boolean coversScreen() {
        return active && !waitingFight;
    }

    public boolean waitingFight() {
        return active && waitingFight;
    }

    public Episodio episodio() {
        return episodio;
    }

    public Episodio.Passo passo() {
        return active ? episodio.passos.get(stepIndex) : null;
    }

    /** Imagens que faltaram ou não abriram (os painéis delas usam a arte provisória). */
    public List<String> warnings() {
        return warnings;
    }

    /** O jogo terminou a luta pedida pelo passo atual. */
    public void resultadoLuta(boolean venceu) {
        if (!waitingFight()) return;
        Episodio.Passo luta = passo();
        if (!venceu && luta.repetirSeDerrota) {
            listener.onHistoriaLuta(luta);
            return;
        }
        waitingFight = false;
        nextStep();
    }

    /** Toque em (x, y), em coordenadas de tela 1280x720. */
    public void tap(float x, float y) {
        if (!coversScreen() || exitAt >= 0f) return;
        Episodio.Passo p = passo();
        if (p.tipo != Episodio.TipoPasso.PROXIMO && x >= SKIP_LEFT && x <= SKIP_RIGHT && y >= SKIP_TOP && y <= SKIP_BOTTOM) {
            skip();
            return;
        }
        switch (p.tipo) {
            case TITULO:
                if (time >= TITLE_MIN_TAP) exit();
                break;
            case PROXIMO:
                if (time >= NEXT_MIN_TAP) exit();
                break;
            case PAGINA:
                tapPage();
                break;
            default:
                break;
        }
    }

    public void update(float dt) {
        if (!coversScreen()) return;
        frame++;
        time += dt;
        shake = Math.max(0f, shake - dt * 60f);
        if (exitAt >= 0f) {
            if (time - exitAt >= EXIT_DURATION) nextStep();
            return;
        }
        Episodio.Passo p = passo();
        if (p.tipo == Episodio.TipoPasso.TITULO && time >= TITLE_DURATION) {
            exit();
        } else if (p.tipo == Episodio.TipoPasso.PAGINA && p.paginaPronta()) {
            guiada.update(dt);
            String sound = guiada.takeSound();
            if (sound != null) listener.onHistoriaSom(sound);
            if (guiada.finished()) exit();
        } else if (p.tipo == Episodio.TipoPasso.PAGINA) {
            int[] current = beats.get(beat);
            boolean panelBeat = current[1] < 0;
            Episodio.Painel panel = p.paineis.get(current[0]);
            boolean hasNext = beat + 1 < beats.size();
            // O painel pousa e o primeiro balão dele entra sozinho; o resto espera o toque.
            if (panelBeat && !panel.baloes.isEmpty() && hasNext &&
                time - panelAt[current[0]] >= FIRST_BALLOON_DELAY) {
                advanceBeat();
            }
        }
    }

    // ------------------------------------------------------------ passos

    private void nextStep() {
        exitAt = -1f;
        time = 0f;
        stepIndex++;
        if (stepIndex >= episodio.passos.size()) {
            finish();
            return;
        }
        Episodio.Passo p = passo();
        switch (p.tipo) {
            case PAGINA:
                beginPage(p);
                break;
            case LUTA:
                waitingFight = true;
                releaseImages();
                listener.onHistoriaLuta(p);
                break;
            default:
                releaseImages();
                listener.onHistoriaSom("pagina");
                break;
        }
    }

    private void finish() {
        active = false;
        waitingFight = false;
        releaseImages();
        listener.onHistoriaFim(episodio);
    }

    /** PULAR: vai direto para a próxima luta ou para o "próximo episódio". */
    private void skip() {
        for (int i = stepIndex + 1; i < episodio.passos.size(); i++) {
            Episodio.TipoPasso tipo = episodio.passos.get(i).tipo;
            if (tipo == Episodio.TipoPasso.LUTA || tipo == Episodio.TipoPasso.PROXIMO) {
                stepIndex = i - 1;
                nextStep();
                return;
            }
        }
        stepIndex = episodio.passos.size() - 1;
        nextStep();
    }

    private void exit() {
        exitAt = time;
    }

    private void beginPage(Episodio.Passo p) {
        releaseImages();
        String folder = Temporada.pasta(episodio.id);
        if (p.paginaPronta()) {
            RenderImage page = null;
            try {
                page = assets.loadImage(folder + "/" + p.imagem);
            } catch (IOException | RuntimeException ex) {
                warnings.add(folder + "/" + p.imagem + ": " + ex.getMessage());
            }
            images.put(p.imagem, page);
            // Sem a imagem, a página vira um papel do tamanho de uma página do GPT (2:3).
            guiada.begin(temporada, p, page, page != null ? page.width() : 1024f, page != null ? page.height() : 1536f);
            listener.onHistoriaSom("pagina");
            return;
        }
        for (Episodio.Painel panel : p.paineis) {
            if (panel.imagem == null || images.containsKey(panel.imagem)) continue;
            RenderImage image = null;
            try {
                image = assets.loadImage(folder + "/" + panel.imagem);
            } catch (IOException | RuntimeException ex) {
                warnings.add(folder + "/" + panel.imagem + ": " + ex.getMessage());
            }
            images.put(panel.imagem, image);
        }
        int count = p.paineis.size();
        panelAt = new float[count];
        balloonAt = new float[count][];
        beats.clear();
        for (int i = 0; i < count; i++) {
            panelAt[i] = -1f;
            int balloons = p.paineis.get(i).baloes.size();
            balloonAt[i] = new float[balloons];
            beats.add(new int[] {i, -1});
            for (int j = 0; j < balloons; j++) {
                balloonAt[i][j] = -1f;
                beats.add(new int[] {i, j});
            }
        }
        beat = -1;
        listener.onHistoriaSom("pagina");
        advanceBeat();
    }

    private void releaseImages() {
        images.clear();
    }

    private void tapPage() {
        if (passo().paginaPronta()) {
            guiada.tap();
            String sound = guiada.takeSound();
            if (sound != null) listener.onHistoriaSom(sound);
            if (guiada.finished()) exit();
            return;
        }
        // Toque com texto ainda sendo digitado: mostra o texto inteiro.
        int[] current = beats.get(beat);
        if (current[1] >= 0) {
            Episodio.Balao b = passo().paineis.get(current[0]).baloes.get(current[1]);
            float typed = (time - balloonAt[current[0]][current[1]] - BALLOON_POP) * TYPE_SPEED;
            int length = temporada.t(b.texto).length();
            if (typed < length) {
                balloonAt[current[0]][current[1]] = time - BALLOON_POP - length / TYPE_SPEED;
                return;
            }
        } else if (time - panelAt[current[0]] < SLAM_DURATION) {
            return;
        }
        if (beat + 1 < beats.size()) {
            advanceBeat();
        } else {
            exit();
        }
    }

    private void advanceBeat() {
        beat++;
        int[] next = beats.get(beat);
        if (next[1] < 0) {
            panelAt[next[0]] = time;
            shake = Math.max(shake, 6f);
            listener.onHistoriaSom("painel");
        } else {
            balloonAt[next[0]][next[1]] = time;
        }
    }

    /** A página está inteira na tela: último painel e último balão, com o texto todo digitado. */
    public boolean pageComplete() {
        Episodio.Passo p = passo();
        if (p == null || p.tipo != Episodio.TipoPasso.PAGINA || waitingFight || exitAt >= 0f) return false;
        if (p.paginaPronta()) return guiada.onLastPanel();
        return beat + 1 >= beats.size() && waitingTap();
    }

    /** Esperando o toque do jogador (mostra a setinha). */
    boolean waitingTap() {
        if (!coversScreen() || exitAt >= 0f) return false;
        Episodio.Passo p = passo();
        if (p.tipo == Episodio.TipoPasso.PROXIMO) return time >= NEXT_MIN_TAP;
        if (p.tipo != Episodio.TipoPasso.PAGINA) return false;
        if (p.paginaPronta()) return guiada.waitingTap();
        int[] current = beats.get(beat);
        if (current[1] < 0) {
            boolean auto = !p.paineis.get(current[0]).baloes.isEmpty();
            return !auto && time - panelAt[current[0]] >= SLAM_DURATION;
        }
        Episodio.Balao b = p.paineis.get(current[0]).baloes.get(current[1]);
        return (time - balloonAt[current[0]][current[1]] - BALLOON_POP) * TYPE_SPEED >= temporada.t(b.texto).length();
    }

    // ------------------------------------------------------------ desenho

    public void render(RenderCanvas c) {
        if (!coversScreen()) return;
        Episodio.Passo p = passo();
        c.save();
        if (shake > 0f) {
            rand.reset(frame * 31L);
            c.translate(rand.range(-shake, shake), rand.range(-shake, shake));
        }
        switch (p.tipo) {
            case TITULO:
                drawTitle(c, p);
                break;
            case PROXIMO:
                drawNext(c, p);
                break;
            case PAGINA:
                if (p.paginaPronta()) {
                    guiada.render(c);
                } else {
                    drawPage(c, p);
                }
                break;
            default:
                break;
        }
        c.restore();
        if (p.tipo != Episodio.TipoPasso.PROXIMO) drawSkip(c);
        if (botaoMenu) drawMenu(c);
        if (waitingTap()) drawTapArrow(c);

        float fadeIn = 1f - Tinta.clamp01(time / PAGE_FADE);
        float fadeOut = exitAt >= 0f ? Tinta.clamp01((time - exitAt) / EXIT_DURATION) : 0f;
        float fade = Math.max(fadeIn * 0.85f, fadeOut);
        if (fade > 0f) c.fillRect(-40f, -40f, VW + 40f, VH + 40f, Tinta.withAlpha(0xFF000000, (int)(255f * fade)));
    }

    private void drawPage(RenderCanvas c, Episodio.Passo p) {
        c.fillRect(-40f, -40f, VW + 40f, VH + 40f, p.flashback() ? FLASHBACK_PAPER : PAPER);
        float[][] polys = Layouts.get(p.layout);
        for (int i = 0; i < polys.length; i++) {
            if (panelAt[i] >= 0f) drawPanel(c, p, i, polys[i], time - panelAt[i]);
        }
        // Balões por cima de todos os painéis (podem atravessar a divisa, como no mangá).
        for (int i = 0; i < polys.length; i++) {
            if (panelAt[i] < 0f) continue;
            float[] box = Layouts.caixa(polys[i]);
            Episodio.Painel panel = p.paineis.get(i);
            drawSfx(c, panel, box, time - panelAt[i]);
            for (int j = 0; j < panel.baloes.size(); j++) {
                float at = balloonAt[i][j];
                if (at < 0f) continue;
                Episodio.Balao b = panel.baloes.get(j);
                float age = time - at;
                int visible = (int)Math.max(0f, (age - BALLOON_POP) * TYPE_SPEED);
                if (age >= BALLOON_POP) visible = Math.max(visible, 1);
                Temporada.Personagem speaker = b.quem != null ? temporada.personagem(b.quem) : null;
                String texto = temporada.t(b.texto);
                baloes.draw(c, b, texto, speaker, box[0], box[1], box[2], box[3], Math.min(visible, texto.length()),
                    Tinta.clamp01(age / BALLOON_POP));
            }
        }
    }

    private void drawPanel(RenderCanvas c, Episodio.Passo p, int index, float[] poly, float age) {
        float[] box = Layouts.caixa(poly);
        float cx = (box[0] + box[2]) * 0.5f;
        float cy = (box[1] + box[3]) * 0.5f;
        float t = Tinta.clamp01(age / SLAM_DURATION);
        float u = 1f - t;

        c.save();
        c.scale(1f + 0.22f * u * u, 1f + 0.22f * u * u, cx, cy);
        c.rotate((index % 2 == 0 ? 4f : -4f) * u * u, cx, cy);

        float[] shadow = new float[poly.length];
        for (int i = 0; i < poly.length; i += 2) {
            shadow[i] = poly[i] + 8f;
            shadow[i + 1] = poly[i + 1] + 10f;
        }
        c.fillPolygon(shadow, p.flashback() ? 0x55FFFFFF : 0x55000000);

        Episodio.Painel panel = p.paineis.get(index);
        c.save();
        c.clipPolygon(poly);
        drawPanelContent(c, p, panel, box, age);
        c.restore();
        c.strokePolygon(poly, 7f, p.flashback() ? 0xFFE9E1CF : INK);

        float land = age - SLAM_DURATION;
        if (land >= 0f && land < 0.12f) {
            c.fillPolygon(poly, Tinta.withAlpha(WHITE, (int)(170f * (1f - land / 0.12f))));
        }
        c.restore();
    }

    /** Conteúdo com um zoom lento de câmera; sem imagem, a arte provisória. */
    private void drawPanelContent(RenderCanvas c, Episodio.Passo p, Episodio.Painel panel, float[] box, float age) {
        float l = box[0], t = box[1], r = box[2], b = box[3];
        float zoom = 1.02f + 0.07f * Tinta.clamp01(age / 4f);
        c.save();
        c.scale(zoom, zoom, (l + r) * 0.5f, (t + b) * 0.5f);
        RenderImage image = panel.imagem != null ? images.get(panel.imagem) : null;
        if (image != null) {
            drawImageCover(c, image, l, t, r, b, panel.foco);
        } else {
            arte.drawScene(c, panel.cena, l, t, r, b);
            arte.drawCharacters(c, temporada, panel, l, t, r, b);
        }
        arte.drawEffect(c, panel.efeito, l, t, r, b, age, frame / 3);
        if (p.flashback()) c.fillRect(l - 20f, t - 20f, r + 20f, b + 20f, SEPIA);
        c.restore();
        if (image == null) arte.drawFileLabel(c, panel.imagem, l, b);
    }

    private static void drawImageCover(RenderCanvas c, RenderImage image, float l, float t, float r, float b, float foco) {
        float boxW = r - l;
        float boxH = b - t;
        float scale = Math.max(boxW / image.width(), boxH / image.height());
        float w = image.width() * scale;
        float h = image.height() * scale;
        float x = l + (boxW - w) * 0.5f;
        float y = t + (boxH - h) * Tinta.clamp01(foco);
        c.drawImage(image, x, y, x + w, y + h, 255);
    }

    private void drawSfx(RenderCanvas c, Episodio.Painel panel, float[] box, float age) {
        if (panel.sfx == null || age < SLAM_DURATION) return;
        float[] pos = panel.posicaoSfx != null ? panel.posicaoSfx : new float[] {0.78f, 0.25f};
        float x = box[0] + (box[2] - box[0]) * pos[0];
        float y = box[1] + (box[3] - box[1]) * pos[1];
        float pop = Tinta.clamp01((age - SLAM_DURATION) / 0.12f);
        float scale = 1.6f - 0.6f * Tinta.easeOutCubic(pop);
        float size = fit(c, panel.sfx, Math.min(110f, (box[3] - box[1]) * 0.30f), (box[2] - box[0]) * 0.75f, true);
        c.save();
        c.rotate(-8f, x, y);
        c.scale(scale, scale, x, y);
        c.drawText(panel.sfx, x, y, size, WHITE, INK, size * 0.16f, true);
        c.restore();
    }

    private void drawTitle(RenderCanvas c, Episodio.Passo p) {
        // Preto puro, o mesmo fundo do logo.
        c.fillRect(-40f, -40f, VW + 40f, VH + 40f, 0xFF000000);
        float in = Tinta.easeOutCubic(time / 0.35f);
        float offset = (1f - in) * -1400f;
        // Pincelada vermelha atravessando a tela.
        float[] band = {offset - 60f, 300f, offset + 1340f, 250f, offset + 1340f, 430f, offset - 60f, 480f};
        c.fillPolygon(band, RED);
        c.drawLine(offset - 60f, 486f, offset + 1340f, 436f, 6f, 0xFF7A0F17);

        float pop = Tinta.clamp01((time - 0.25f) / 0.25f);
        if (logo != null && time >= 0.4f) {
            // Logo do jogo embaixo da pincelada, aparecendo depois do título.
            float alpha = Tinta.clamp01((time - 0.4f) / 0.35f);
            float h = 200f;
            float w = h * logo.width() / logo.height();
            c.drawImage(logo, VW * 0.5f - w * 0.5f, 500f, VW * 0.5f + w * 0.5f, 500f + h, (int)(255f * alpha));
        }
        if (pop > 0f) {
            String label = episodio.numero == 0 ? temporada.t("PRÓLOGO") : temporada.t("EPISÓDIO") + " " + episodio.numero;
            c.drawText(label, VW * 0.5f, 220f, 40f, WHITE, INK, 6f, true);
            String title = temporada.t(p.titulo != null ? p.titulo : episodio.titulo);
            float scale = 1.5f - 0.5f * Tinta.easeOutCubic(pop);
            float size = fit(c, title, 96f, 1180f, true);
            c.save();
            c.scale(scale, scale, VW * 0.5f, 380f);
            c.drawText(title, VW * 0.5f, 400f, size, WHITE, INK, size * 0.14f, true);
            c.restore();
        }
        if (logo == null && time >= 0.6f) {
            c.drawText(temporada.titulo, VW - 250f, VH - 60f, 30f, WHITE, INK, 5f, true);
            if (!temporada.subtitulo.isEmpty()) c.drawText(temporada.subtitulo, VW - 250f, VH - 22f, 26f, RED, INK, 4f, false);
        }
    }

    private void drawNext(RenderCanvas c, Episodio.Passo p) {
        c.fillRect(-40f, -40f, VW + 40f, VH + 40f, PAPER);
        // Retícula de pontos, como no fim de capítulo dos mangás.
        for (float y = 20f; y < VH; y += 26f) {
            for (float x = (y % 52f == 20f ? 20f : 33f); x < VW; x += 26f) c.fillCircle(x, y, 3.2f, 0x1A000000);
        }
        float in = Tinta.easeOutCubic(time / 0.3f);
        float[] strip = {-20f, 70f, 760f * in, 70f, 720f * in, 150f, -20f, 150f};
        c.fillPolygon(strip, INK);
        if (in > 0.6f) c.drawText(temporada.t("PRÓXIMO EPISÓDIO"), 340f * in, 128f, 46f, WHITE, INK, 0f, true);

        float pop = Tinta.clamp01((time - 0.25f) / 0.25f);
        if (pop > 0f) {
            String title = p.titulo != null ? temporada.t(p.titulo) : "";
            float size = fit(c, title, 84f, 1160f, true);
            float scale = 1.4f - 0.4f * Tinta.easeOutCubic(pop);
            c.save();
            c.scale(scale, scale, VW * 0.5f, 290f);
            c.drawText(title, VW * 0.5f, 300f, size, RED, INK, size * 0.12f, true);
            c.restore();
        }
        float y = 400f;
        for (int i = 0; i < p.linhas.size(); i++) {
            float lineIn = Tinta.clamp01((time - 0.45f - i * 0.18f) / 0.2f);
            if (lineIn <= 0f) break;
            String line = temporada.t(p.linhas.get(i));
            c.drawText(line, VW * 0.5f, y, fit(c, line, 32f, 1140f, false),
                Tinta.withAlpha(INK, (int)(255 * lineIn)), 0, 0f, false);
            y += 46f;
        }
        c.drawText(temporada.t("CONTINUA..."), VW - 250f, VH - 40f, 40f, WHITE, INK, 7f, true);
    }

    private void drawSkip(RenderCanvas c) {
        c.fillRect(SKIP_LEFT, SKIP_TOP, SKIP_RIGHT, SKIP_BOTTOM, 0xB0000000);
        c.drawText(temporada.t("PULAR") + " ▶▶", (SKIP_LEFT + SKIP_RIGHT) * 0.5f, SKIP_BOTTOM - 9f, 19f, WHITE, 0, 0f, true);
    }

    private void drawMenu(RenderCanvas c) {
        c.fillRect(MENU_LEFT, MENU_TOP, MENU_RIGHT, MENU_BOTTOM, 0xB0000000);
        c.drawText("◀ " + temporada.t(MenuPausa.MENU), (MENU_LEFT + MENU_RIGHT) * 0.5f, MENU_BOTTOM - 9f, 19f, WHITE, 0, 0f, true);
    }

    private void drawTapArrow(RenderCanvas c) {
        float bob = (float)Math.sin(time * 7f) * 5f;
        float x = VW - 52f;
        float y = VH - 50f + bob;
        float[] tri = {x - 18f, y - 14f, x + 18f, y - 14f, x, y + 14f};
        c.strokePolygon(tri, 6f, INK);
        c.fillPolygon(tri, WHITE);
    }

    private static float fit(RenderCanvas c, String text, float size, float width, boolean italic) {
        float w = c.measureText(text, size, italic);
        return w <= width ? size : size * width / w;
    }
}
