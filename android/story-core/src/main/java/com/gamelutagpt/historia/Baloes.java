package com.gamelutagpt.historia;

import com.gamelutagpt.render.RenderCanvas;
import java.util.ArrayList;
import java.util.List;

/** Desenha os balões de fala com o texto quebrado em linhas e digitado aos poucos. */
final class Baloes {
    static final int INK = 0xFF111111;
    static final int WHITE = 0xFFFFFFFF;
    private static final int NARRATION = 0xFFFFF4D2;
    private static final float TEXT_SIZE = 25f;
    private static final float NARRATION_SIZE = 22f;
    private static final float LINE = 1.22f;

    private final float[] tail = new float[6];

    /** Linhas do texto que cabem em {@code maxWidth}; palavras longas demais ficam sozinhas na linha. */
    static List<String> wrap(RenderCanvas c, String text, float size, float maxWidth) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                if (word.isEmpty()) continue;
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (line.length() > 0 && c.measureText(candidate, size, false) > maxWidth) {
                    lines.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                } else {
                    line.setLength(0);
                    line.append(candidate);
                }
            }
            lines.add(line.toString());
        }
        return lines;
    }

    /**
     * Desenha o balão na caixa do painel.
     *
     * @param visible quantos caracteres do texto já apareceram (efeito de digitação)
     * @param pop     0 → 1 enquanto o balão entra
     */
    void draw(RenderCanvas c, Episodio.Balao b, Temporada.Personagem speaker, float l, float t, float r, float bottom,
              int visible, float pop) {
        boolean narration = b.tipo == Episodio.TipoBalao.NARRACAO;
        float size = narration ? NARRATION_SIZE : (b.tipo == Episodio.TipoBalao.GRITO ? TEXT_SIZE + 5f : TEXT_SIZE);
        float panelW = r - l;
        float maxWidth = Math.min(narration ? 520f : 360f, panelW * (narration ? 0.86f : 0.70f));
        List<String> lines = wrap(c, b.texto, size, maxWidth);
        float textW = 0f;
        for (String line : lines) textW = Math.max(textW, c.measureText(line, size, false));
        float textH = lines.size() * size * LINE;

        float w;
        float h;
        if (narration) {
            w = textW + 36f;
            h = textH + 24f;
        } else {
            w = textW * 1.22f + 54f;
            h = textH * 1.30f + 40f;
            if (b.tipo == Episodio.TipoBalao.GRITO) {
                // As pontas passam um pouco da elipse: a caixa usada para encaixar cresce junto.
                w *= 1.25f;
                h *= 1.30f;
            }
        }
        // Centro do balão dentro da caixa do painel (e da tela).
        float cx = clamp(l + panelW * b.x, Math.max(l, 0f) + w * 0.5f + 6f, Math.min(r, 1280f) - w * 0.5f - 6f);
        float cy = clamp(t + (bottom - t) * b.y, Math.max(t, 0f) + h * 0.5f + 6f, Math.min(bottom, 720f) - h * 0.5f - 6f);

        float scale = 0.6f + 0.4f * Tinta.easeOutBack(pop);
        c.save();
        c.scale(scale, scale, cx, cy);
        switch (b.tipo) {
            case NARRACAO:
                c.fillRect(cx - w * 0.5f - 4f, cy - h * 0.5f - 4f, cx + w * 0.5f + 4f, cy + h * 0.5f + 4f, INK);
                c.fillRect(cx - w * 0.5f, cy - h * 0.5f, cx + w * 0.5f, cy + h * 0.5f, NARRATION);
                break;
            case PENSAMENTO:
                drawThought(c, b, l, t, r, bottom, cx, cy, w, h);
                break;
            case GRITO:
                drawShout(c, b, l, t, r, bottom, cx, cy, w, h);
                break;
            default:
                drawSpeech(c, b, l, t, r, bottom, cx, cy, w, h);
                break;
        }

        int left = visible;
        float y = cy - textH * 0.5f + size * 0.92f;
        for (String line : lines) {
            if (left <= 0) break;
            String shown = left >= line.length() ? line : line.substring(0, left);
            // Centraliza a linha inteira e escreve só o que já apareceu, para o texto não "andar".
            float full = c.measureText(line, size, false);
            float part = c.measureText(shown, size, false);
            c.drawText(shown, cx - full * 0.5f + part * 0.5f, y, size, INK, 0, 0f, false);
            left -= line.length() + 1;
            y += size * LINE;
        }
        c.restore();

        if (speaker != null && !narration && pop >= 1f) drawNameTag(c, speaker, cx - w * 0.5f + 18f, cy - h * 0.5f);
    }

    private void drawSpeech(RenderCanvas c, Episodio.Balao b, float l, float t, float r, float bottom,
                            float cx, float cy, float w, float h) {
        boolean hasTail = buildTail(b, l, t, r, bottom, cx, cy, w, h, 0.16f);
        if (hasTail) c.strokePolygon(tail, 7f, INK);
        c.fillOval(cx - w * 0.5f - 3.5f, cy - h * 0.5f - 3.5f, cx + w * 0.5f + 3.5f, cy + h * 0.5f + 3.5f, INK);
        if (hasTail) c.fillPolygon(tail, WHITE);
        c.fillOval(cx - w * 0.5f, cy - h * 0.5f, cx + w * 0.5f, cy + h * 0.5f, WHITE);
    }

    private void drawShout(RenderCanvas c, Episodio.Balao b, float l, float t, float r, float bottom,
                           float cx, float cy, float w, float h) {
        int spikes = 22;
        float[] star = new float[spikes * 4];
        for (int i = 0; i < spikes * 2; i++) {
            double a = Math.PI * 2 * i / (spikes * 2);
            float k = (i & 1) == 0 ? 0.48f : 0.41f;
            // Pontas irregulares, sempre iguais para o mesmo balão.
            if ((i & 1) == 0) k += 0.03f * (float)Math.sin(i * 2.3 + b.texto.length());
            star[i * 2] = cx + (float)Math.cos(a) * w * k;
            star[i * 2 + 1] = cy + (float)Math.sin(a) * h * k;
        }
        boolean hasTail = buildTail(b, l, t, r, bottom, cx, cy, w, h, 0.12f);
        if (hasTail) c.strokePolygon(tail, 7f, INK);
        c.strokePolygon(star, 7f, INK);
        if (hasTail) c.fillPolygon(tail, WHITE);
        c.fillPolygon(star, WHITE);
    }

    private void drawThought(RenderCanvas c, Episodio.Balao b, float l, float t, float r, float bottom,
                             float cx, float cy, float w, float h) {
        float puff = h * 0.24f;
        float rx = w * 0.5f - puff * 0.5f;
        float ry = h * 0.5f - puff * 0.5f;
        int puffs = Math.max(10, (int)(w / (puff * 1.1f)));
        for (int pass = 0; pass < 2; pass++) {
            float extra = pass == 0 ? 3.5f : 0f;
            int color = pass == 0 ? INK : WHITE;
            for (int i = 0; i < puffs; i++) {
                double a = Math.PI * 2 * i / puffs;
                c.fillCircle(cx + (float)Math.cos(a) * rx * 0.92f, cy + (float)Math.sin(a) * ry * 0.88f,
                    puff + extra, color);
            }
            c.fillOval(cx - rx - extra, cy - ry - extra, cx + rx + extra, cy + ry + extra, color);
        }
        if (b.rabo == null) return;
        float tx = l + (r - l) * b.rabo[0];
        float ty = t + (bottom - t) * b.rabo[1];
        float[] edge = edgePoint(cx, cy, rx + puff * 0.6f, ry + puff * 0.6f, tx, ty);
        for (int i = 0; i < 3; i++) {
            float k = (i + 1) / 4f;
            float x = edge[0] + (tx - edge[0]) * k * 0.8f;
            float y = edge[1] + (ty - edge[1]) * k * 0.8f;
            float radius = puff * (0.5f - i * 0.13f);
            c.fillCircle(x, y, radius + 3f, INK);
            c.fillCircle(x, y, radius, WHITE);
        }
    }

    /** Rabicho triangular do balão até a boca de quem fala (limitado a 110 px). */
    private boolean buildTail(Episodio.Balao b, float l, float t, float r, float bottom,
                              float cx, float cy, float w, float h, float baseFraction) {
        if (b.rabo == null) return false;
        float tx = l + (r - l) * b.rabo[0];
        float ty = t + (bottom - t) * b.rabo[1];
        float[] edge = edgePoint(cx, cy, w * 0.5f, h * 0.5f, tx, ty);
        float dx = tx - edge[0];
        float dy = ty - edge[1];
        float len = (float)Math.sqrt(dx * dx + dy * dy);
        if (len < 4f) return false;
        float maxLen = 110f;
        if (len > maxLen) {
            tx = edge[0] + dx / len * maxLen;
            ty = edge[1] + dy / len * maxLen;
        }
        // Base do rabicho um pouco para dentro do balão, para a emenda sumir no preenchimento.
        float nx = (cx - edge[0]);
        float ny = (cy - edge[1]);
        float nl = Math.max(1f, (float)Math.sqrt(nx * nx + ny * ny));
        float inX = edge[0] + nx / nl * 14f;
        float inY = edge[1] + ny / nl * 14f;
        float px = -ny / nl;
        float py = nx / nl;
        float half = Math.min(w, h) * baseFraction;
        tail[0] = inX + px * half;
        tail[1] = inY + py * half;
        tail[2] = tx;
        tail[3] = ty;
        tail[4] = inX - px * half * 0.4f;
        tail[5] = inY - py * half * 0.4f;
        return true;
    }

    /** Ponto da borda da elipse (cx, cy, rx, ry) na direção de (tx, ty). */
    private static float[] edgePoint(float cx, float cy, float rx, float ry, float tx, float ty) {
        double a = Math.atan2((ty - cy) / ry, (tx - cx) / rx);
        return new float[] {cx + (float)Math.cos(a) * rx, cy + (float)Math.sin(a) * ry};
    }

    private static void drawNameTag(RenderCanvas c, Temporada.Personagem p, float x, float bottom) {
        float size = 17f;
        float w = c.measureText(p.nome.toUpperCase(java.util.Locale.ROOT), size, true) + 20f;
        float top = bottom - 8f;
        c.fillRect(x - 3f, top - 3f, x + w + 3f, top + 27f, INK);
        c.fillRect(x, top, x + w, top + 24f, p.cor);
        c.drawText(p.nome.toUpperCase(java.util.Locale.ROOT), x + w * 0.5f, top + 19f, size, WHITE, INK, 3f, true);
    }

    private static float clamp(float v, float min, float max) {
        if (min > max) return (min + max) * 0.5f;
        return v < min ? min : (v > max ? max : v);
    }
}
