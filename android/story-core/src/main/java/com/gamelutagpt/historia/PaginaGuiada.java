package com.gamelutagpt.historia;

import com.gamelutagpt.render.Rand;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import java.util.List;

/**
 * Leitura guiada de uma página pronta (a página inteira numa imagem, com balões e
 * onomatopeias já desenhados): a câmera mostra a página toda, entra quadro a quadro com o
 * resto da página escurecido e, depois do último, volta para a página inteira. É o jeito
 * que os leitores de mangá mostram uma página em pé numa tela deitada.
 */
final class PaginaGuiada {
    static final float VW = PaginaHistoria.VW;
    static final float VH = PaginaHistoria.VH;
    /** A página inteira fica na tela antes do primeiro quadro. */
    static final float OVERVIEW_HOLD = 0.9f;
    static final float MOVE = 0.5f;
    static final float OUTRO_MOVE = 0.55f;
    static final float OUTRO_HOLD = 0.45f;
    /** Zoom máximo sobre a imagem (quadros pequenos não ficam borrados demais). */
    static final float MAX_SCALE = 3.0f;
    /** Velocidade do passeio da câmera em quadros mais altos que a tela, em px de tela por segundo. */
    static final float PAN_SPEED = 110f;
    static final int DIM_ALPHA = 185;
    private static final int CAPTION_BG = 0xD9000000;

    private final Rand rand = new Rand(17);
    private RenderImage image;
    private List<Episodio.Quadro> quadros;
    private float pageW;
    private float pageH;
    /** Quadro em foco, ou -1 na página inteira (começo e fim). */
    private int panel = -1;
    private boolean outro;
    private float stateTime;
    private float time;
    // Câmera: centro (em px da imagem) e escala (px de tela por px da imagem).
    private float camX, camY, camS;
    private float fromX, fromY, fromS;
    private float moveTime = -1f;
    private float moveDuration = MOVE;
    private float dimFrom, dim;
    private float shake;
    private float flash;

    void begin(Episodio.Passo passo, RenderImage image, float width, float height) {
        this.image = image;
        this.quadros = passo.quadros;
        pageW = width;
        pageH = height;
        panel = -1;
        outro = false;
        stateTime = 0f;
        time = 0f;
        shake = 0f;
        flash = 0f;
        float[] overview = overview();
        camX = overview[0];
        camY = overview[1];
        camS = overview[2];
        moveTime = -1f;
        dim = 0f;
        dimFrom = 0f;
    }

    void update(float dt) {
        time += dt;
        stateTime += dt;
        shake = Math.max(0f, shake - dt * 60f);
        flash = Math.max(0f, flash - dt);
        if (moveTime >= 0f) moveTime += dt;
        if (panel < 0 && !outro && stateTime >= OVERVIEW_HOLD) goTo(0);
        updateCamera();
    }

    /** Avança um quadro; depois do último, a câmera volta para a página inteira. */
    void tap() {
        if (outro) return;
        if (panel < quadros.size() - 1) {
            goTo(panel + 1);
        } else {
            startOutro();
        }
    }

    /** A volta para a página inteira terminou: a página pode sair. */
    boolean finished() {
        return outro && stateTime >= OUTRO_MOVE + OUTRO_HOLD;
    }

    boolean waitingTap() {
        return panel >= 0 && !outro && moving() < 0f;
    }

    /** Último quadro em foco e parado (o próximo toque fecha a página). */
    boolean onLastPanel() {
        return waitingTap() && panel == quadros.size() - 1;
    }

    private void goTo(int index) {
        startMove(MOVE);
        panel = index;
        stateTime = 0f;
        dimFrom = dim;
        Episodio.Quadro q = quadros.get(index);
        if (q.impacto) {
            shake = 14f;
            flash = 0.12f;
        }
    }

    private void startOutro() {
        startMove(OUTRO_MOVE);
        outro = true;
        stateTime = 0f;
        dimFrom = dim;
    }

    private void startMove(float duration) {
        fromX = camX;
        fromY = camY;
        fromS = camS;
        moveTime = 0f;
        moveDuration = duration;
    }

    /** Fração [0, 1] do movimento atual da câmera, ou -1 parada. */
    private float moving() {
        if (moveTime < 0f) return -1f;
        float t = moveTime / moveDuration;
        return t >= 1f ? -1f : t;
    }

    // ------------------------------------------------------------ câmera

    private float[] overview() {
        float s = Math.min(VW * 0.96f / pageW, VH * 0.96f / pageH);
        return new float[] {pageW * 0.5f, pageH * 0.5f, s};
    }

    /** Onde a câmera deve estar agora para o quadro (com o passeio em quadros altos). */
    private float[] target(int index, float since) {
        float[] box = box(index);
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
            // Zoom lento enquanto o quadro está em foco.
            s *= 1f + 0.035f * Tinta.clamp01(since / 5f);
        }
        return new float[] {cx, cy, s};
    }

    private float[] box(int index) {
        float[] a = quadros.get(index).area;
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
        float[] goal;
        float goalDim;
        if (outro || panel < 0) {
            goal = overview();
            goalDim = 0f;
        } else {
            float sinceArrival = moving() < 0f ? stateTime - moveDuration : 0f;
            goal = target(panel, Math.max(0f, sinceArrival));
            goalDim = 1f;
        }
        float k = moving();
        if (k < 0f) {
            camX = goal[0];
            camY = goal[1];
            camS = goal[2];
            dim = goalDim;
            return;
        }
        float e = k < 0.5f ? 4f * k * k * k : 1f - (float)Math.pow(-2f * k + 2f, 3) / 2f;
        camX = fromX + (goal[0] - fromX) * e;
        camY = fromY + (goal[1] - fromY) * e;
        camS = fromS + (goal[2] - fromS) * e;
        dim = dimFrom + (goalDim - dimFrom) * e;
    }

    // ------------------------------------------------------------ desenho

    void render(RenderCanvas c) {
        c.fillRect(-40f, -40f, VW + 40f, VH + 40f, 0xFF000000);
        c.save();
        if (shake > 0f) {
            rand.reset((long)(time * 600f));
            c.translate(rand.range(-shake, shake), rand.range(-shake, shake));
        }
        float left = sx(0f);
        float top = sy(0f);
        float right = sx(pageW);
        float bottom = sy(pageH);
        if (image != null) {
            c.drawImage(image, left, top, right, bottom, 255);
        } else {
            c.fillRect(left, top, right, bottom, 0xFFF1ECE1);
        }
        if (dim > 0f) {
            c.fillRect(-40f, -40f, VW + 40f, VH + 40f, Tinta.withAlpha(0xFF000000, (int)(DIM_ALPHA * dim)));
            if (panel >= 0) {
                float[] poly = screenPolygon(panel);
                c.save();
                c.clipPolygon(poly);
                if (image != null) {
                    c.drawImage(image, left, top, right, bottom, 255);
                } else {
                    c.fillRect(left, top, right, bottom, 0xFFF1ECE1);
                }
                c.restore();
                if (image == null) c.strokePolygon(poly, 5f, Baloes.INK);
            }
        }
        c.restore();
        if (flash > 0f) c.fillRect(-40f, -40f, VW + 40f, VH + 40f, Tinta.withAlpha(0xFFFFFFFF, (int)(200f * flash / 0.12f)));
        drawCaption(c);
    }

    private void drawCaption(RenderCanvas c) {
        if (panel < 0 || outro) return;
        String text = quadros.get(panel).legenda;
        if (text == null || moving() >= 0f) return;
        float alpha = Tinta.clamp01((stateTime - moveDuration) / 0.25f);
        if (alpha <= 0f) return;
        float size = 26f;
        List<String> lines = Baloes.wrap(c, text, size, VW - 200f);
        float lineH = size * 1.25f;
        float h = lines.size() * lineH + 26f;
        c.fillRect(0f, VH - h - 18f, VW, VH - 18f, Tinta.withAlpha(CAPTION_BG, (int)(0xD9 * alpha)));
        float y = VH - h - 18f + 13f + size * 0.9f;
        for (String line : lines) {
            c.drawText(line, VW * 0.5f, y, size, Tinta.withAlpha(0xFFFFFFFF, (int)(255 * alpha)), 0, 0f, false);
            y += lineH;
        }
    }

    private float sx(float px) {
        return (px - camX) * camS + VW * 0.5f;
    }

    private float sy(float py) {
        return (py - camY) * camS + VH * 0.5f;
    }

    private float[] screenPolygon(int index) {
        float[] a = quadros.get(index).area;
        float[] out = new float[a.length];
        for (int i = 0; i + 1 < a.length; i += 2) {
            out[i] = sx(a[i] * pageW);
            out[i + 1] = sy(a[i + 1] * pageH);
        }
        return out;
    }
}
