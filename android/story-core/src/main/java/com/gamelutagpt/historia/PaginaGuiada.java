package com.gamelutagpt.historia;

import com.gamelutagpt.render.Rand;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Leitura guiada de uma página pronta (a página inteira numa imagem): a página já abre no
 * primeiro quadro (sem mostrar a página inteira antes) e a câmera passa quadro a quadro com o
 * resto da página escurecido; depois do último quadro a página sai direto para a próxima, que
 * também já abre no primeiro quadro. É o jeito que os leitores de mangá mostram uma página em
 * pé numa tela deitada.
 *
 * Os balões de fala desenhados na página têm o miolo pintado de branco e o texto escrito
 * pelo jogo, no idioma atual, todo já escrito desde o começo da página (sem digitar). Os
 * efeitos de cada quadro (impacto, tremor, velocidade, zoom) dão o movimento que a arte parada
 * não tem, sempre com a câmera suave: nada de sacudir a tela.
 */
final class PaginaGuiada {
    static final float VW = PaginaHistoria.VW;
    static final float VH = PaginaHistoria.VH;
    /** A página abre no primeiro quadro; o tranco do impacto espera o escuro da virada sair. */
    static final float ENTRY = PaginaHistoria.PAGE_FADE;
    static final float MOVE = 0.5f;
    /** Chegada de supetão nos quadros de impacto. */
    static final float WHIP = 0.24f;
    /** Zoom máximo sobre a imagem (quadros pequenos não ficam borrados demais). */
    static final float MAX_SCALE = 3.0f;
    /** Velocidade do passeio da câmera em quadros mais altos que a tela, em px de tela por segundo. */
    static final float PAN_SPEED = 110f;
    static final int DIM_ALPHA = 220;
    /** Impacto: zoom a mais na chegada, que assenta como mola (uma ou duas oscilações). */
    static final float PUNCH_ZOOM = 0.10f;
    static final float PUNCH_DECAY = 7f;
    static final float PUNCH_FREQ = 15f;
    /** Tremor: o quadro pulsa como batida de coração (duas batidas, uma pausa). */
    static final float BEAT_PERIOD = 1.15f;
    static final float BEAT_ZOOM = 0.022f;
    private static final int CAPTION_BG = 0xD9000000;
    private static final int PAPER = 0xFFFFFFFF;

    private final Rand lines = new Rand(23);
    private Temporada temporada;
    private RenderImage image;
    private List<Episodio.Quadro> quadros;
    private float pageW;
    private float pageH;
    /** Quadro em foco. */
    private int panel;
    private boolean arrived;
    /** O último quadro foi lido: a página já pode sair (ela some congelada no último quadro). */
    private boolean done;
    private float stateTime;
    private float arrivedAt;
    private float time;
    // Câmera: centro (em px da imagem) e escala (px de tela por px da imagem).
    private float camX, camY, camS;
    private float fromX, fromY, fromS;
    private float moveTime = -1f;
    private float moveDuration = MOVE;
    private float dimFrom, dim;
    private float flash;
    private String sound;
    /** Tamanho do texto de cada balão no idioma atual, medido no primeiro desenho. */
    private final Map<Episodio.BalaoPronto, Object[]> fitted = new IdentityHashMap<>();
    private String fittedIdioma;

    void begin(Temporada temporada, Episodio.Passo passo, RenderImage image, float width, float height) {
        this.temporada = temporada;
        this.image = image;
        this.quadros = passo.quadros;
        pageW = width;
        pageH = height;
        panel = 0;
        arrived = false;
        done = false;
        stateTime = 0f;
        time = 0f;
        flash = 0f;
        sound = null;
        fitted.clear();
        // Já no primeiro quadro, parado; ele "chega" (efeitos) quando a virada da página clareia.
        float[] goal = target(0, 0f);
        camX = goal[0];
        camY = goal[1];
        camS = goal[2];
        moveTime = -1f;
        dim = 1f;
        dimFrom = 1f;
    }

    void update(float dt) {
        time += dt;
        stateTime += dt;
        flash = Math.max(0f, flash - dt);
        if (moveTime >= 0f) moveTime += dt;
        boolean entrando = panel == 0 && moveTime < 0f && stateTime < ENTRY;
        if (!done && !arrived && !entrando && moving() < 0f) arrive();
        updateCamera();
    }

    /** Toque: passa para o próximo quadro (o texto já está todo escrito); no último, fecha a página. */
    void tap() {
        if (done) return;
        if (panel < quadros.size() - 1) {
            goTo(panel + 1);
        } else {
            done = true;
        }
    }

    /** O último quadro foi lido: a página pode sair. */
    boolean finished() {
        return done;
    }

    boolean waitingTap() {
        return !done && arrived;
    }

    /** Último quadro em foco (o próximo toque fecha a página). */
    boolean onLastPanel() {
        return waitingTap() && panel == quadros.size() - 1;
    }

    /** Som pedido desde a última consulta ("painel", "impacto"), ou null. */
    String takeSound() {
        String s = sound;
        sound = null;
        return s;
    }

    private Episodio.Quadro current() {
        return quadros.get(panel);
    }

    private void goTo(int index) {
        panel = index;
        arrived = false;
        stateTime = 0f;
        dimFrom = dim;
        startMove(quadros.get(index).impacto ? WHIP : MOVE);
        sound = "painel";
    }

    private void arrive() {
        arrived = true;
        arrivedAt = time;
        Episodio.Quadro q = current();
        if (q.impacto) {
            flash = 0.14f;
            sound = "impacto";
        }
    }

    private void startMove(float duration) {
        fromX = camX;
        fromY = camY;
        fromS = camS;
        moveTime = 0f;
        moveDuration = duration;
    }

    /** Fração [0, 1) do movimento atual da câmera, ou -1 parada. */
    private float moving() {
        if (moveTime < 0f) return -1f;
        float t = moveTime / moveDuration;
        return t >= 1f ? -1f : t;
    }

    // ------------------------------------------------------------ câmera

    /** Onde a câmera deve estar agora para o quadro (passeio em quadros altos, zoom e tranco). */
    private float[] target(int index, float since) {
        float[] box = box(quadros.get(index).area);
        Episodio.Quadro q = quadros.get(index);
        float bw = box[2] - box[0];
        float bh = box[3] - box[1];
        float s = Math.min(Math.min(VW * 0.92f / bw, VH * 0.90f / bh), MAX_SCALE);
        float cx = (box[0] + box[2]) * 0.5f;
        float cy = (box[1] + box[3]) * 0.5f;
        if (s * bw < VW * 0.62f) {
            // Quadro alto: mais zoom e a câmera desce devagar por ele.
            s = Math.min(VW * 0.62f / bw, MAX_SCALE);
            float visible = VH * 0.90f / s;
            if (bh > visible) {
                float travel = (bh - visible) * s;
                float duration = Math.max(2.5f, travel / PAN_SPEED);
                float k = Tinta.clamp01(since / duration);
                k = k * k * (3f - 2f * k);
                float top = box[1] + visible * 0.5f;
                float bottom = box[3] - visible * 0.5f;
                cy = top + (bottom - top) * k;
            }
        } else {
            // Zoom lento enquanto o quadro está em foco (mais forte com "zoom").
            float amount = q.zoom ? 0.12f : 0.035f;
            float duration = q.zoom ? 3f : 5f;
            float k = Tinta.clamp01(since / duration);
            s *= 1f + amount * (q.zoom ? 1f - (1f - k) * (1f - k) : k);
        }
        s *= 1f + punch(q, since) + heartbeat(q, since);
        return new float[] {cx, cy, s};
    }

    /**
     * Impacto: o quadro chega com zoom a mais e assenta como mola, uma ou duas oscilações
     * largas (no lugar do tremor aleatório da tela, que incomodava).
     */
    static float punch(Episodio.Quadro q, float since) {
        if (!q.impacto || since < 0f || since > 1f) return 0f;
        return PUNCH_ZOOM * (float)(Math.exp(-since * PUNCH_DECAY) * Math.cos(since * PUNCH_FREQ));
    }

    /** Tremor: o quadro pulsa num ritmo de coração (duas batidas suaves e uma pausa). */
    static float heartbeat(Episodio.Quadro q, float since) {
        if (!q.tremor || since <= 0f) return 0f;
        float t = since % BEAT_PERIOD;
        return BEAT_ZOOM * (bump(t, 0.25f) + 0.6f * bump(t, 0.49f));
    }

    private static float bump(float t, float center) {
        float d = (t - center) / 0.085f;
        return (float)Math.exp(-d * d);
    }

    private float[] box(float[] a) {
        float l = 1f, t = 1f, r = 0f, b = 0f;
        for (int i = 0; i + 1 < a.length; i += 2) {
            l = Math.min(l, a[i]);
            r = Math.max(r, a[i]);
            t = Math.min(t, a[i + 1]);
            b = Math.max(b, a[i + 1]);
        }
        return new float[] {l * pageW, t * pageH, r * pageW, b * pageH};
    }

    private void updateCamera() {
        float[] goal = target(panel, arrived ? time - arrivedAt : 0f);
        float goalDim = 1f;
        float k = moving();
        if (k < 0f) {
            camX = goal[0];
            camY = goal[1];
            camS = goal[2];
            dim = goalDim;
            return;
        }
        boolean whip = moveDuration == WHIP;
        // Impacto: acelera até o fim (chega batendo); o resto entra e sai suave.
        float e = whip ? k * k * k : (k < 0.5f ? 4f * k * k * k : 1f - (float)Math.pow(-2f * k + 2f, 3) / 2f);
        camX = fromX + (goal[0] - fromX) * e;
        camY = fromY + (goal[1] - fromY) * e;
        camS = fromS + (goal[2] - fromS) * e;
        dim = dimFrom + (goalDim - dimFrom) * e;
    }

    // ------------------------------------------------------------ desenho

    void render(RenderCanvas c) {
        c.fillRect(-40f, -40f, VW + 40f, VH + 40f, 0xFF000000);
        c.save();
        drawLayer(c, false);
        if (dim > 0f) {
            c.fillRect(-40f, -40f, VW + 40f, VH + 40f, Tinta.withAlpha(0xFF000000, (int)(DIM_ALPHA * dim)));
            float[] poly = screen(current().area);
            c.save();
            c.clipPolygon(poly);
            drawLayer(c, true);
            if (current().velocidade && arrived && !done) drawSpeedLines(c, poly);
            c.restore();
            if (image == null) c.strokePolygon(poly, 5f, Baloes.INK);
        }
        c.restore();
        if (flash > 0f) c.fillRect(-40f, -40f, VW + 40f, VH + 40f, Tinta.withAlpha(0xFFFFFFFF, (int)(210f * flash / 0.14f)));
        drawCaption(c);
    }

    /** A página com os balões, todos já com o texto inteiro. */
    private void drawLayer(RenderCanvas c, boolean focusOnly) {
        float left = sx(0f), top = sy(0f), right = sx(pageW), bottom = sy(pageH);
        if (image != null) {
            c.drawImage(image, left, top, right, bottom, 255);
        } else {
            c.fillRect(left, top, right, bottom, 0xFFF1ECE1);
        }
        for (int i = 0; i < quadros.size(); i++) {
            if (focusOnly && i != panel) continue;
            List<Episodio.BalaoPronto> baloes = quadros.get(i).baloes;
            for (int j = 0; j < baloes.size(); j++) {
                Episodio.BalaoPronto b = baloes.get(j);
                c.fillPolygon(screen(b.area), PAPER);
                if (b.moldura) c.strokePolygon(screen(b.area), 3f * camS, Baloes.INK);
                drawBalloonText(c, b);
            }
        }
    }

    private void drawBalloonText(RenderCanvas c, Episodio.BalaoPronto b) {
        Object[] fit = fit(c, b);
        float size = (Float)fit[0];
        @SuppressWarnings("unchecked")
        List<String> lines = (List<String>)fit[1];
        float lineH = size * 1.12f;
        // Texto medido em px da página; na tela acompanha o zoom da câmera.
        float k = camS;
        float cx = sx((b.caixa[0] + b.caixa[2]) * 0.5f * pageW);
        float cy = sy((b.caixa[1] + b.caixa[3]) * 0.5f * pageH);
        float y = cy - lines.size() * lineH * k * 0.5f + size * k * 0.86f;
        for (String line : lines) {
            c.drawText(line, cx, y, size * k, Baloes.INK, 0, 0f, false);
            y += lineH * k;
        }
    }

    /** Maior tamanho de letra (em px da página) em que o texto cabe na caixa do balão. */
    private Object[] fit(RenderCanvas c, Episodio.BalaoPronto b) {
        if (!temporada.idioma().equals(fittedIdioma)) {
            fitted.clear();
            fittedIdioma = temporada.idioma();
        }
        Object[] cached = fitted.get(b);
        if (cached != null) return cached;
        String text = temporada.t(b.texto);
        float w = (b.caixa[2] - b.caixa[0]) * pageW;
        float h = (b.caixa[3] - b.caixa[1]) * pageH;
        float size = Math.min(40f, h);
        List<String> lines = Baloes.wrap(c, text, size, w);
        while (size > 9f) {
            lines = Baloes.wrap(c, text, size, w);
            float widest = 0f;
            for (String line : lines) widest = Math.max(widest, c.measureText(line, size, false));
            if (widest <= w * 1.04f && lines.size() * size * 1.12f <= h * 1.04f) break;
            size -= 1f;
        }
        Object[] out = {size, lines};
        fitted.put(b, out);
        return out;
    }

    /**
     * Linhas de foco de mangá: riscos da borda do quadro em direção ao centro, sorteados de
     * novo a cada poucos quadros para tremeluzir como nos animes.
     */
    private void drawSpeedLines(RenderCanvas c, float[] poly) {
        float[] bb = Layouts.caixa(poly);
        float cx = (bb[0] + bb[2]) * 0.5f;
        float cy = (bb[1] + bb[3]) * 0.5f;
        float rx = (bb[2] - bb[0]) * 0.62f;
        float ry = (bb[3] - bb[1]) * 0.62f;
        float fade = Tinta.clamp01((time - arrivedAt) / 0.15f);
        lines.reset((long)(time * 20f));
        for (int i = 0; i < 46; i++) {
            double a = Math.PI * 2 * (i + lines.next() * 0.8f) / 46;
            float cos = (float)Math.cos(a);
            float sin = (float)Math.sin(a);
            float inner = lines.range(0.50f, 0.82f);
            float width = lines.range(2f, 7f);
            int alpha = (int)(lines.range(110f, 190f) * fade);
            c.drawLine(cx + cos * rx * 1.1f, cy + sin * ry * 1.1f, cx + cos * rx * inner, cy + sin * ry * inner,
                width, Tinta.withAlpha(0xFFFFFFFF, alpha));
        }
    }

    private void drawCaption(RenderCanvas c) {
        if (done || !arrived) return;
        String text = temporada.t(current().legenda);
        if (text == null) return;
        float alpha = Tinta.clamp01((time - arrivedAt) / 0.25f);
        if (alpha <= 0f) return;
        float size = 26f;
        List<String> rows = Baloes.wrap(c, text, size, VW - 200f);
        float lineH = size * 1.25f;
        float h = rows.size() * lineH + 26f;
        c.fillRect(0f, VH - h - 18f, VW, VH - 18f, Tinta.withAlpha(CAPTION_BG, (int)(0xD9 * alpha)));
        float y = VH - h - 18f + 13f + size * 0.9f;
        for (String row : rows) {
            c.drawText(row, VW * 0.5f, y, size, Tinta.withAlpha(0xFFFFFFFF, (int)(255 * alpha)), 0, 0f, false);
            y += lineH;
        }
    }

    private float sx(float px) {
        return (px - camX) * camS + VW * 0.5f;
    }

    private float sy(float py) {
        return (py - camY) * camS + VH * 0.5f;
    }

    private float[] screen(float[] a) {
        float[] out = new float[a.length];
        for (int i = 0; i + 1 < a.length; i += 2) {
            out[i] = sx(a[i] * pageW);
            out[i + 1] = sy(a[i + 1] * pageH);
        }
        return out;
    }
}
