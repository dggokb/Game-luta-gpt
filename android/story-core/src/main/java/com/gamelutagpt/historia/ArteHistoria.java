package com.gamelutagpt.historia;

import com.gamelutagpt.render.Rand;
import com.gamelutagpt.render.RenderCanvas;
import java.util.List;

/**
 * Arte provisória dos painéis: cenário em degradê, silhuetas na cor de cada personagem e
 * o nome do arquivo que a arte final deve ter. Serve para testar o ritmo e o texto do
 * episódio antes de qualquer imagem existir.
 */
final class ArteHistoria {
    private static final int INK = Baloes.INK;
    private static final int WHITE = Baloes.WHITE;
    private final Rand rand = new Rand(7);

    void drawScene(RenderCanvas c, String cena, float l, float t, float r, float b) {
        int top;
        int bottom;
        switch (cena) {
            case "dojo": top = 0xFFD8AE70; bottom = 0xFF6B4423; break;
            case "dojo-noite": top = 0xFF2B3350; bottom = 0xFF11141F; break;
            case "casa": top = 0xFFEBDDBA; bottom = 0xFFA88F68; break;
            case "estrada": top = 0xFFF2B66B; bottom = 0xFF6D4C7D; break;
            case "templo": top = 0xFF3A2350; bottom = 0xFF120A1C; break;
            case "ceu": top = 0xFF9ED0F0; bottom = 0xFFE8F4FA; break;
            case "chuva": top = 0xFF4B5563; bottom = 0xFF1F2933; break;
            case "carta": top = 0xFF8A5A32; bottom = 0xFF3E2513; break;
            default: top = 0xFFD9D4C7; bottom = 0xFFA9A49A; break;
        }
        c.fillVerticalGradient(l, t, r, b, top, bottom);
        float h = b - t;
        float w = r - l;
        if (cena.startsWith("dojo")) {
            // Ripas de madeira e o pergaminho da escola na parede.
            for (float x = l + 40f; x < r; x += 70f) c.drawLine(x, t, x, t + h * 0.62f, 3f, 0x33000000);
            c.fillRect(l, t + h * 0.62f, r, b, Tinta.withAlpha(bottom, 200));
            float sx = l + w * 0.5f;
            c.fillRect(sx - 34f, t + h * 0.08f, sx + 34f, t + h * 0.42f, 0xFFF3E9D2);
            c.drawText("光明", sx, t + h * 0.30f, 34f, 0xFF2A2A2A, 0, 0f, false);
        } else if ("templo".equals(cena)) {
            c.fillRadialGlow(l + w * 0.75f, t + h * 0.25f, h * 0.35f, 0x88E9E2FF);
            c.fillCircle(l + w * 0.75f, t + h * 0.25f, h * 0.10f, 0xFFF4F0FF);
        } else if ("estrada".equals(cena)) {
            c.fillCircle(l + w * 0.5f, t + h * 0.62f, h * 0.18f, 0xFFFFD58A);
            c.fillRect(l, t + h * 0.64f, r, b, 0xFF3B2A40);
        } else if ("chuva".equals(cena)) {
            rand.reset(11);
            for (int i = 0; i < 60; i++) {
                float x = l + rand.next() * w;
                float y = t + rand.next() * h;
                c.drawLine(x, y, x - 12f, y + 40f, 2f, 0x66C9D6E8);
            }
        }
    }

    /** Silhuetas dos personagens do painel, com o nome embaixo. */
    void drawCharacters(RenderCanvas c, Temporada temporada, Episodio.Painel p, float l, float t, float r, float b) {
        List<String> ids = p.personagens;
        float w = r - l;
        float h = b - t;
        if ("detalhe".equals(p.enquadramento)) {
            drawLetter(c, l + w * 0.5f, t + h * 0.52f, Math.min(w, h) * 0.55f);
            return;
        }
        if (ids.isEmpty()) return;
        if ("close".equals(p.enquadramento)) {
            float slot = w / ids.size();
            for (int i = 0; i < ids.size(); i++) {
                Temporada.Personagem who = temporada.personagem(ids.get(i));
                drawFace(c, who, l + slot * (i + 0.5f), t + h * 0.52f, Math.min(slot * 0.26f, h * 0.24f), i);
            }
            return;
        }
        float slot = w / ids.size();
        for (int i = 0; i < ids.size(); i++) {
            Temporada.Personagem who = temporada.personagem(ids.get(i));
            float height = Math.min(h * 0.72f, slot * 1.1f);
            drawFigure(c, who, l + slot * (i + 0.5f), b - h * 0.06f, height, i);
        }
    }

    private static void drawFigure(RenderCanvas c, Temporada.Personagem who, float x, float feet, float height, int index) {
        int body = who.cor;
        int dark = Tinta.darken(body, 0.45f);
        float head = height * 0.11f;
        float shoulder = height * 0.20f;
        float neck = feet - height + head * 2f;
        float hip = feet - height * 0.45f;
        float side = index % 2 == 0 ? 1f : -1f;
        // Pernas, tronco e cabeça: contorno de tinta por baixo, cor por cima.
        float[] legs = {x - shoulder * 0.7f, hip, x + shoulder * 0.7f, hip, x + shoulder * 0.9f, feet,
            x + shoulder * 0.25f, feet, x, hip + height * 0.12f, x - shoulder * 0.25f, feet, x - shoulder * 0.9f, feet};
        float[] torso = {x - shoulder, neck + head * 0.4f, x + shoulder, neck + head * 0.4f,
            x + shoulder * 0.72f, hip + 4f, x - shoulder * 0.72f, hip + 4f};
        c.strokePolygon(legs, 7f, INK);
        c.fillPolygon(legs, dark);
        c.strokePolygon(torso, 7f, INK);
        c.fillPolygon(torso, body);
        // Braço da guarda, para o lado de quem ele encara.
        c.drawLine(x + side * shoulder * 0.8f, neck + head, x + side * shoulder * 1.5f, neck + head * 2.6f, 16f, INK);
        c.drawLine(x + side * shoulder * 0.8f, neck + head, x + side * shoulder * 1.5f, neck + head * 2.6f, 9f, body);
        c.fillCircle(x, neck - head * 0.6f, head + 3.5f, INK);
        c.fillCircle(x, neck - head * 0.6f, head, 0xFFF0D2B0);
        c.fillRect(x - head, neck - head * 1.7f, x + head, neck - head * 1.05f, dark);
        drawLabel(c, who.nome, x, feet - height * 0.02f, body);
    }

    private static void drawFace(RenderCanvas c, Temporada.Personagem who, float x, float y, float radius, int index) {
        int dark = Tinta.darken(who.cor, 0.45f);
        c.fillOval(x - radius * 1.5f, y + radius * 0.7f, x + radius * 1.5f, y + radius * 2.6f, INK);
        c.fillOval(x - radius * 1.45f, y + radius * 0.75f, x + radius * 1.45f, y + radius * 2.6f, who.cor);
        c.fillCircle(x, y, radius + 4f, INK);
        c.fillCircle(x, y, radius, 0xFFF0D2B0);
        c.fillOval(x - radius, y - radius * 1.05f, x + radius, y - radius * 0.25f, dark);
        float eye = radius * 0.36f;
        float dir = index % 2 == 0 ? 1f : -1f;
        for (int s = -1; s <= 1; s += 2) {
            float ex = x + s * eye + dir * radius * 0.08f;
            c.drawLine(ex - eye * 0.5f, y + radius * 0.02f, ex + eye * 0.5f, y - s * dir * radius * 0.04f, 7f, INK);
        }
        c.drawLine(x - radius * 0.22f, y + radius * 0.55f, x + radius * 0.22f, y + radius * 0.55f, 5f, INK);
        drawLabel(c, who.nome, x, y + radius * 2.2f, who.cor);
    }

    private static void drawLetter(RenderCanvas c, float x, float y, float size) {
        float w = size;
        float h = size * 0.62f;
        float[] env = {x - w * 0.5f, y - h * 0.5f, x + w * 0.5f, y - h * 0.5f, x + w * 0.5f, y + h * 0.5f, x - w * 0.5f, y + h * 0.5f};
        float[] flap = {x - w * 0.5f, y - h * 0.5f, x + w * 0.5f, y - h * 0.5f, x, y + h * 0.08f};
        c.strokePolygon(env, 8f, INK);
        c.fillPolygon(env, 0xFFF4ECD8);
        c.strokePolygon(flap, 5f, INK);
        c.fillCircle(x, y + h * 0.06f, size * 0.08f + 3f, INK);
        c.fillCircle(x, y + h * 0.06f, size * 0.08f, 0xFFB3202A);
    }

    private static void drawLabel(RenderCanvas c, String text, float x, float y, int color) {
        c.drawText(text, x, y, 20f, WHITE, Tinta.darken(color, 0.6f), 5f, true);
    }

    /** Faixa com o arquivo da arte final, para quem for gerar a imagem saber qual é. */
    void drawFileLabel(RenderCanvas c, String file, float l, float b) {
        if (file == null) return;
        String text = "ARTE: " + file;
        float size = 16f;
        float w = c.measureText(text, size, false) + 18f;
        float x = Math.max(l, 0f) + 10f;
        c.fillRect(x, b - 34f, x + w, b - 10f, 0xAA000000);
        c.drawText(text, x + w * 0.5f, b - 16f, size, 0xFFFFE08A, 0, 0f, false);
    }

    /** Efeitos por cima do conteúdo (com ou sem imagem). */
    void drawEffect(RenderCanvas c, String efeito, float l, float t, float r, float b, float age, long seed) {
        if (efeito == null) return;
        float cx = (l + r) * 0.5f;
        float cy = (t + b) * 0.5f;
        float reach = Math.max(r - l, b - t);
        switch (efeito) {
            case "velocidade": {
                rand.reset(seed);
                for (int i = 0; i < 46; i++) {
                    double a = Math.PI * 2 * rand.next();
                    float inner = reach * rand.range(0.30f, 0.42f);
                    float cos = (float)Math.cos(a);
                    float sin = (float)Math.sin(a);
                    c.drawLine(cx + cos * inner, cy + sin * inner, cx + cos * reach, cy + sin * reach,
                        rand.range(2f, 6f), 0xCC111111);
                }
                break;
            }
            case "impacto": {
                float k = Tinta.clamp01(age / 0.25f);
                rand.reset(seed / 3);
                for (int i = 0; i < 18; i++) {
                    double a = Math.PI * 2 * (i + rand.next() * 0.5f) / 18;
                    float len = reach * (0.18f + 0.30f * k) * rand.range(0.6f, 1.2f);
                    c.drawLine(cx, cy, cx + (float)Math.cos(a) * len, cy + (float)Math.sin(a) * len, 7f, 0xEEFFFFFF);
                }
                break;
            }
            case "chuva": {
                rand.reset(seed / 4);
                for (int i = 0; i < 70; i++) {
                    float x = l + rand.next() * (r - l);
                    float y = t + rand.next() * (b - t);
                    c.drawLine(x, y, x - 10f, y + 36f, 2f, 0x88DDE6F0);
                }
                break;
            }
            case "brilho":
                c.fillRadialGlow(cx, t + (b - t) * 0.35f, reach * 0.45f, 0x66FFF2C4);
                break;
            default:
                break;
        }
    }
}
