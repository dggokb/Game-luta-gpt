package com.gamelutagpt.efeitos;

import com.gamelutagpt.render.RenderCanvas;

/**
 * Desenho do S1 (projétil) de cada personagem, por código, em Java puro: o mesmo desenho no
 * Android e no PC. Cada tipo tem forma, rastro e giro próprios; a cor do personagem entra só como
 * destaque (brilho, faixas). Coordenadas do mundo; o desenho olha para a direção do voo.
 *
 * <p>Não é thread-safe nem guarda estado: chame da thread que desenha.
 */
public final class ProjetilVisual {
    private ProjetilVisual() {}

    /** O visual do projétil (docs/personagens/auditoria-golpes.md, "S1 por tipo"). */
    public enum Tipo {
        /** p01: a bola de energia (o golpe dele). */
        ONDA,
        /** p03: bola menor e mais rápida, rastro curto. */
        ONDA_CURTA,
        /** p02: onda em forma de palma aberta, translúcida, de alcance curto. */
        PALMA,
        /** p04: estouro em leque logo na frente do punho. */
        ESTOURO,
        /** p05: lâmina escura com argola. */
        KUNAI,
        /** p05 (S2): estrela de quatro pontas girando. */
        SHURIKEN,
        /** p06: sombra roxa que corre colada no chão. */
        SOMBRA,
        /** p08: o chicote esticado da mão até a ponta. */
        CHICOTE,
        /** p09: machado de bombeiro girando. */
        MACHADO,
        /** p10: machadinha girando rápido. */
        MACHADINHO,
        /** p11: lâmina prateada fina e muito rápida. */
        BISTURI,
        /** p11 (S2): seringa com veneno verde, cai em diagonal no chão. */
        SERINGA,
        /** p09 (Super): parede de fogo que corre pelo chão depois do soco no chão. */
        FOGO
    }

    private static final int WHITE = 0xFFFFFFFF;
    private static final int STEEL = 0xFF3A3F4B;
    private static final int STEEL_LIGHT = 0xFFB8C2D0;
    private static final int SILVER = 0xFFDDE3EA;

    /**
     * Desenha o projétil.
     *
     * @param x posição da ponta (centro do projétil) no mundo
     * @param y altura no mundo (para a SOMBRA, o chão)
     * @param direction +1 voando para a direita, -1 para a esquerda
     * @param originX onde saiu (a mão): o CHICOTE vai dela até a ponta
     * @param range alcance: o desenho muda ao longo do voo (PALMA e ESTOURO se desfazem)
     * @param time segundos desde o lançamento (tremulação e brilho)
     * @param accent a cor do personagem
     */
    public static void draw(RenderCanvas c, Tipo tipo, float x, float y, int direction, float originX, float range,
                            float time, int accent) {
        float travelled = Math.abs(x - originX);
        float progress = range > 0f ? Math.min(1f, travelled / range) : 0f;
        c.save();
        c.translate(x, y);
        if (direction < 0) c.scale(-1f, 1f, 0f, 0f);
        switch (tipo) {
            case ONDA: orb(c, 20f, travelled, accent, 4.4f); break;
            case ONDA_CURTA: orb(c, 13f, travelled, accent, 2.6f); break;
            case PALMA: palm(c, progress, accent); break;
            case ESTOURO: burst(c, progress, time, accent); break;
            case KUNAI: kunai(c, travelled, accent); break;
            case SHURIKEN: shuriken(c, travelled); break;
            case SOMBRA: shadow(c, travelled, time, accent); break;
            case CHICOTE: whip(c, travelled, progress, time, accent); break;
            case MACHADO: axe(c, travelled, accent); break;
            case MACHADINHO: hatchet(c, travelled); break;
            case BISTURI: scalpel(c, travelled, time); break;
            case SERINGA: syringe(c, time); break;
            case FOGO: fire(c, travelled, time); break;
            default: break;
        }
        c.restore();
    }

    // ------------------------------------------------------------ energia

    private static void orb(RenderCanvas c, float r, float travelled, int accent, float tailScale) {
        float length = Math.min(travelled, r * tailScale);
        float spin = travelled / (r * 1.7f);
        for (int layer = 0; layer < 2; layer++) {
            float w = r * (layer == 0 ? 0.95f : 0.45f);
            c.fillPolygon(tail(w, length), layer == 0 ? alpha(accent, 0x88) : 0xCCFFFFFF);
        }
        for (int i = 0; i < 5; i++) {
            float t = (spin * 0.35f + i / 5f) % 1f;
            float sx = -r * 0.6f - t * length * 1.1f;
            float sy = (float)Math.sin(i * 2.7f + spin) * r * (0.4f + 0.6f * t);
            c.drawLine(sx, sy, sx - 10f - 12f * t, sy, 3f * (1f - t), alpha(i % 2 == 0 ? WHITE : accent, (int)(230 * (1f - t))));
        }
        c.fillRadialGlow(0f, 0f, r * 2.3f, alpha(accent, 0xE0));
        c.fillCircle(0f, 0f, r, opaque(accent));
        c.fillRadialGlow(r * 0.12f, -r * 0.08f, r * 0.95f, 0xF8FFFFFF);
        arc(c, 0f, 0f, r * 1.28f, r * 1.0f, spin * 57f, 110f, 2.5f, 0xE6FFFFFF);
        arc(c, 0f, 0f, r * 1.28f, r * 1.0f, spin * 57f + 180f, 80f, 2.5f, 0xE6FFFFFF);
        arc(c, 0f, 0f, r * 1.55f, r * 1.55f, -spin * 41f, 70f, 2.5f, opaque(accent));
    }

    /** Rastro de cometa afinando para trás, de largura {@code w}. */
    private static float[] tail(float w, float length) {
        int n = 8;
        float[] p = new float[(n + 1) * 4];
        int k = 0;
        for (int i = 0; i <= n; i++) {
            float t = i / (float)n;
            p[k++] = -length * t;
            p[k++] = -w * (1f - t * t);
        }
        for (int i = n; i >= 0; i--) {
            float t = i / (float)n;
            p[k++] = -length * t;
            p[k++] = w * (1f - t * t);
        }
        return p;
    }

    /** Palma aberta translúcida que cresce e some (alcance curto). */
    private static void palm(RenderCanvas c, float progress, int accent) {
        float s = 34f * (1f + 0.5f * progress);
        int a = (int)(255f * (1f - 0.7f * progress * progress));
        // Ondas de ar atrás da palma.
        for (int k = 0; k < 3; k++) {
            float r = s * (0.9f + 0.32f * k);
            arc(c, -s * 0.15f, 0f, r * 0.55f, r, 140f, 80f, 3f - k * 0.7f, alpha(accent, a * (3 - k) / 5));
        }
        c.fillRadialGlow(s * 0.2f, 0f, s * 1.5f, alpha(accent, a * 3 / 5));
        int fill = alpha(0xFFFFFFFF, a * 3 / 5), edge = alpha(accent, a);
        float[] palm = {-s * 0.35f, -s * 0.42f, s * 0.30f, -s * 0.46f, s * 0.36f, 0f, s * 0.30f, s * 0.46f,
                        -s * 0.35f, s * 0.42f, -s * 0.45f, 0f};
        c.fillPolygon(palm, fill);
        c.strokePolygon(palm, 3f, edge);
        float[] lengths = {0.50f, 0.62f, 0.58f, 0.44f};
        float[] rows = {-0.33f, -0.11f, 0.11f, 0.32f};
        for (int i = 0; i < 4; i++) {
            float y0 = rows[i] * s, h = s * 0.09f, x0 = s * 0.28f, x1 = x0 + lengths[i] * s;
            float[] finger = {x0, y0 - h, x1 - h * 0.9f, y0 - h, x1 - h * 0.25f, y0 - h * 0.7f, x1, y0,
                              x1 - h * 0.25f, y0 + h * 0.7f, x1 - h * 0.9f, y0 + h, x0, y0 + h};
            c.fillPolygon(finger, fill);
            c.strokePolygon(finger, 2.5f, edge);
        }
        // Polegar dobrado de lado, colado na palma (palmada de frente, não "joinha").
        float[] thumb = {-s * 0.10f, -s * 0.42f, s * 0.14f, -s * 0.60f, s * 0.34f, -s * 0.58f, s * 0.20f, -s * 0.42f};
        c.fillPolygon(thumb, fill);
        c.strokePolygon(thumb, 2.5f, edge);
    }

    /** Estouro em leque na frente do punho: estrela de pontas que abre e some. */
    private static void burst(RenderCanvas c, float progress, float time, int accent) {
        float r = 26f + 34f * progress;
        int a = (int)(255f * (1f - progress * progress));
        c.fillRadialGlow(0f, 0f, r * 1.6f, alpha(accent, a * 4 / 5));
        c.fillPolygon(star(12, r, r * 0.45f, time * 90f), alpha(accent, a));
        c.fillPolygon(star(12, r * 0.6f, r * 0.3f, time * 90f + 15f), alpha(WHITE, a));
        for (int i = -2; i <= 2; i++) {
            float ang = (float)Math.toRadians(i * 16f);
            float x0 = r * 0.9f * (float)Math.cos(ang), y0 = r * 0.9f * (float)Math.sin(ang);
            c.drawLine(x0, y0, x0 + 26f * (float)Math.cos(ang), y0 + 26f * (float)Math.sin(ang), 3f, alpha(WHITE, a));
        }
    }

    // ------------------------------------------------------------ lâminas

    private static void kunai(RenderCanvas c, float travelled, int accent) {
        // Rastro de vento atrás.
        for (int i = 0; i < 4; i++) {
            float y = (i - 1.5f) * 4.5f, x0 = -30f - i * 6f;
            c.drawLine(x0, y, x0 - 34f + i * 4f, y, 1.6f, alpha(WHITE, 140 - i * 25));
        }
        c.fillPolygon(new float[] {26f, 0f, 9f, -6.5f, 0f, -3f, 0f, 3f, 9f, 6.5f}, STEEL);
        c.drawLine(26f, 0f, 9f, -6.5f, 1.6f, STEEL_LIGHT);
        c.drawLine(24f, 0f, 2f, 0f, 1.2f, 0xFF5A6070);
        c.fillRect(-18f, -2.4f, 0f, 2.4f, 0xFF201A18);
        for (int i = 0; i < 4; i++) c.drawLine(-16f + i * 4.5f, -2.4f, -13.5f + i * 4.5f, 2.4f, 1.4f, opaque(accent));
        c.strokeCircle(-23f, 0f, 5f, 2.2f, STEEL);
    }

    private static void shuriken(RenderCanvas c, float travelled) {
        float angle = travelled / 9f;
        for (int ghost = 2; ghost >= 0; ghost--) {
            int a = ghost == 0 ? 255 : 70 / ghost;
            float gx = -ghost * 11f;
            float[] blade = star(4, 18f, 5f, (float)Math.toDegrees(angle) - ghost * 20f);
            for (int i = 0; i < blade.length; i += 2) blade[i] += gx;
            c.fillPolygon(blade, alpha(STEEL, a));
            if (ghost == 0) c.strokePolygon(blade, 1.4f, STEEL_LIGHT);
        }
        c.strokeCircle(0f, 0f, 17f, 1.2f, 0x30FFFFFF);
        c.fillCircle(0f, 0f, 4f, 0xFF1A1C22);
        c.fillCircle(0f, 0f, 1.6f, 0xFFE0E6EE);
    }

    private static void scalpel(RenderCanvas c, float travelled, float time) {
        // Risco longo e fino atrás: some aos poucos.
        for (int i = 0; i < 6; i++) {
            float x0 = -22f - i * 16f;
            c.drawLine(x0, 0f, x0 - 16f, 0f, 1.5f, alpha(WHITE, 200 - i * 32));
        }
        c.fillRect(-22f, -1.8f, -2f, 1.8f, 0xFFB0B8C2);
        for (int i = 0; i < 4; i++) c.drawLine(-19f + i * 4f, -1.8f, -19f + i * 4f, 1.8f, 1f, 0xFF7A828C);
        c.fillPolygon(new float[] {22f, 0f, 4f, -3.2f, -2f, -2f, -2f, 2f, 4f, 1.4f}, SILVER);
        c.drawLine(22f, 0f, 4f, -3.2f, 1.2f, WHITE);
        float glint = 5f + 3f * (float)Math.abs(Math.sin(time * 18f + travelled * 0.05f));
        c.fillPolygon(star(4, glint, glint * 0.22f, 45f, 22f, 0f), WHITE);
    }

    /** Seringa: corpo de vidro com o veneno verde, êmbolo atrás e a agulha na frente. */
    private static void syringe(RenderCanvas c, float time) {
        for (int i = 0; i < 4; i++) {
            float x0 = -24f - i * 10f;
            c.drawLine(x0, 0f, x0 - 10f, 0f, 1.4f, alpha(0xFF7CFF6A, 150 - i * 35));
        }
        c.fillRect(-14f, -4.5f, 8f, 4.5f, 0xC0E8F4F0);
        c.fillRect(-12f, -3.2f, 6f, 3.2f, 0xFF4CD43A);
        c.fillRadialGlow(-3f, 0f, 14f, 0x707CFF6A);
        for (int i = 0; i < 4; i++) c.drawLine(-9f + i * 4.5f, -4.5f, -9f + i * 4.5f, -2f, 1f, 0xFF2A5A30);
        c.strokePolygon(new float[] {-14f, -4.5f, 8f, -4.5f, 8f, 4.5f, -14f, 4.5f}, 1.2f, 0xFFB8C2D0);
        c.fillRect(-20f, -1.4f, -14f, 1.4f, 0xFFB8C2D0);
        c.fillRect(-23f, -5f, -20f, 5f, 0xFF8A929C);
        c.fillRect(8f, -2.2f, 11f, 2.2f, 0xFF8A929C);
        c.drawLine(11f, 0f, 24f, 0f, 1.2f, SILVER);
        float drop = 1.6f + 0.6f * (float)Math.abs(Math.sin(time * 14f));
        c.fillCircle(25f, 0f, drop, 0xFF7CFF6A);
    }

    /**
     * Fogo pelo chão (y é o chão): línguas de fogo altas na frente que baixam para trás, brasas
     * subindo e o chão em brasa por onde passou.
     */
    private static void fire(RenderCanvas c, float travelled, float time) {
        float length = Math.min(200f, 60f + travelled * 0.5f);
        c.fillRadialGlow(-length * 0.3f, -50f, length * 0.9f, 0x90FF7A1A);
        c.fillRect(-length - 20f, -5f, 30f, 3f, 0xB0FF9A30);
        int n = 11;
        for (int layer = 0; layer < 3; layer++) {
            int color = layer == 0 ? 0xE0C8200A : layer == 1 ? 0xF0FF7A10 : 0xF8FFE070;
            float scale = layer == 0 ? 1f : layer == 1 ? 0.72f : 0.42f;
            for (int i = 0; i < n; i++) {
                float t = i / (float)(n - 1);
                float cx = 24f - (length + 24f) * t;
                float tall = (150f * (1f - 0.65f * t) + 22f * (float)Math.sin(time * 13f + i * 2.1f)) * scale;
                float w = (24f - 8f * t) * (0.6f + 0.4f * scale);
                float lean = -10f * (float)Math.sin(time * 9f + i);
                c.fillPolygon(new float[] {cx - w, 0f, cx - w * 0.5f, -tall * 0.55f, cx + lean, -tall,
                    cx + w * 0.5f, -tall * 0.5f, cx + w, 0f}, color);
            }
        }
        for (int i = 0; i < 7; i++) {
            float t = (time * 1.4f + i * 0.17f) % 1f;
            float ex = -length * (i / 7f) + 8f * (float)Math.sin(time * 6f + i);
            c.fillCircle(ex, -40f - t * 140f, 3f * (1f - t) + 1f, alpha(0xFFFFC040, (int)(230 * (1f - t))));
        }
    }

    /**
     * Poça de veneno no chão (y é o chão): mancha verde rasa que borbulha e encolhe ao sumir.
     *
     * @param life quanto resta da poça (1 recém-caída, 0 sumindo)
     */
    public static void puddle(RenderCanvas c, float x, float groundY, float halfWidth, float life, float time,
                              int accent) {
        float w = halfWidth * (0.55f + 0.45f * Math.min(1f, life * 3f));
        int a = (int)(235f * Math.min(1f, life * 4f));
        c.fillRadialGlow(x, groundY - 6f, w * 1.3f, alpha(0xFF5CFF4A, a / 3));
        c.fillOval(x - w, groundY - 9f, x + w, groundY + 5f, alpha(0xFF1E6A1E, a));
        c.fillOval(x - w * 0.8f, groundY - 7f, x + w * 0.75f, groundY + 2f, alpha(0xFF3FBF34, a));
        c.fillOval(x - w * 0.45f, groundY - 6f, x + w * 0.2f, groundY - 2f, alpha(0xFFA8FF9A, a * 2 / 3));
        for (int i = 0; i < 4; i++) {
            float t = (time * 0.9f + i * 0.27f) % 1f;
            float bx = x + w * 0.7f * (float)Math.sin(i * 2.3f + 1f);
            float by = groundY - 6f - t * 26f;
            c.strokeCircle(bx, by, 2f + 3f * t, 1.3f, alpha(0xFF8CFF7A, (int)(a * (1f - t))));
        }
        c.drawLine(x - w * 0.6f, groundY - 1f, x + w * 0.6f, groundY - 1f, 1.5f, alpha(accent, a / 3));
    }

    /**
     * Jato de vapor saindo da palma em diagonal para cima e para a frente (S2 da p10): nuvens
     * brancas que sobem pelo jato, crescem e se desfazem.
     *
     * @param progress 0 quando a palma abre, 1 quando o jato termina de se desfazer
     */
    public static void steam(RenderCanvas c, float x, float y, int direction, float progress, float time) {
        float grow = Math.min(1f, progress * 3f);
        float fade = progress < 0.6f ? 1f : Math.max(0f, 1f - (progress - 0.6f) / 0.4f);
        float length = 190f * grow;
        c.save();
        c.translate(x, y);
        if (direction < 0) c.scale(-1f, 1f, 0f, 0f);
        c.rotate(-50f, 0f, 0f);
        c.fillRadialGlow(length * 0.4f, 0f, length * 0.6f + 10f, alpha(0xFFE8F4FF, (int)(110 * fade)));
        for (int i = 0; i < 9; i++) {
            float t = ((time * 2.2f + i / 9f) % 1f) * grow;
            float px = length * t;
            float r = 8f + 26f * t;
            float sway = 6f * (float)Math.sin(time * 9f + i * 1.7f) * t;
            int a = (int)(210f * (1f - t * 0.75f) * fade);
            c.fillCircle(px, sway, r, alpha(i % 3 == 0 ? 0xFFFFFFFF : 0xFFD8E6F0, a));
        }
        for (int k = -1; k <= 1; k++) {
            c.drawLine(4f, k * 4f, length * 0.85f, k * 14f, 2f, alpha(WHITE, (int)(150 * fade)));
        }
        c.restore();
    }

    // ------------------------------------------------------------ armas que giram

    private static void axe(RenderCanvas c, float travelled, int accent) {
        float deg = (float)Math.toDegrees(travelled / 20f);
        // Rastro do giro: dois arcos atrás da lâmina.
        arc(c, 0f, 0f, 40f, 40f, (float)Math.toDegrees(travelled / 20f) - 100f, 80f, 4f, 0x50FFFFFF);
        arc(c, 0f, 0f, 40f, 40f, (float)Math.toDegrees(travelled / 20f) + 80f, 60f, 3f, 0x30FFFFFF);
        c.save();
        c.rotate(deg, 0f, 0f);
        // Cabo ao longo de x; a cabeça atravessa o cabo na ponta: lâmina curva para um lado,
        // picareta para o outro (machado de bombeiro).
        c.fillRect(-30f, -2.8f, 24f, 2.8f, 0xFF6B4423);
        c.fillRect(-32f, -3.4f, -26f, 3.4f, 0xFF2A1A10);
        for (int i = 0; i < 3; i++) c.drawLine(-22f + i * 5f, -2.8f, -20f + i * 5f, 2.8f, 1.2f, opaque(accent));
        float[] blade = {12f, -3f, 27f, -3f, 30f, -12f, 35f, -27f, 26f, -25f, 18f, -24f, 8f, -27f, 11f, -12f};
        c.fillPolygon(blade, 0xFFC8202C);
        c.fillPolygon(new float[] {35f, -27f, 26f, -25f, 18f, -24f, 8f, -27f, 9f, -23f, 18f, -20.5f, 26f, -21.5f, 33.5f, -23f},
            0xFFD8DEE6);
        c.strokePolygon(blade, 1.6f, 0xFF5A0A10);
        c.fillPolygon(new float[] {13f, 3f, 26f, 3f, 19.5f, 19f}, 0xFFC8202C);
        c.strokePolygon(new float[] {13f, 3f, 26f, 3f, 19.5f, 19f}, 1.4f, 0xFF5A0A10);
        c.restore();
    }

    private static void hatchet(RenderCanvas c, float travelled) {
        float deg = (float)Math.toDegrees(travelled / 12f);
        arc(c, 0f, 0f, 24f, 24f, (float)Math.toDegrees(travelled / 12f) - 110f, 90f, 3f, 0x50FFFFFF);
        c.save();
        c.rotate(deg, 0f, 0f);
        c.fillRect(-17f, -2f, 14f, 2f, 0xFF8A5A2E);
        float[] head = {6f, -2f, 15f, -2f, 18f, -9f, 21f, -17f, 13f, -15f, 4f, -17f, 7f, -9f};
        c.fillPolygon(head, 0xFF9AA3AE);
        c.drawLine(21f, -17f, 13f, -15f, 1.8f, 0xFFE6EBF0);
        c.drawLine(13f, -15f, 4f, -17f, 1.8f, 0xFFE6EBF0);
        c.strokePolygon(head, 1.2f, 0xFF4A5058);
        c.fillRect(6f, -2f, 15f, 4f, 0xFF4A5058);
        c.restore();
    }

    // ------------------------------------------------------------ sombra e chicote

    /**
     * Sombra roxa colada no chão (y é o chão): uma massa escura que corre rente, mais alta na
     * frente, com pontas de sombra subindo e tremulando, e a cauda que se desfaz.
     */
    private static void shadow(RenderCanvas c, float travelled, float time, int accent) {
        float length = Math.min(170f, 40f + travelled * 0.7f);
        c.fillRadialGlow(-10f, -14f, 70f, 0x608A4FD8);
        int n = 12;
        for (int i = n - 1; i >= 0; i--) {
            float t = i / (float)(n - 1);
            float cx = -length * t;
            float h = 34f * (1f - 0.75f * t) + 3f * (float)Math.sin(time * 9f + i);
            float w = 22f + 6f * (1f - t);
            c.fillOval(cx - w, -h, cx + w, h * 0.35f, alpha(0xFF1A0B2A, (int)(235 * (1f - 0.8f * t))));
        }
        for (int i = 0; i < 8; i++) {
            float base = 10f - i * 16f;
            float tall = (40f + 22f * (float)Math.abs(Math.sin(time * 7f + i * 1.9f))) * (1f - i * 0.1f);
            float sway = 8f * (float)Math.sin(time * 5f + i);
            c.fillPolygon(new float[] {base - 7f, -4f, base + sway, -tall, base + 7f, -4f},
                alpha(i % 2 == 0 ? 0xFF4A2A6E : 0xFF2E1648, 230 - i * 22));
        }
        // Borda de luz roxa na frente da massa, rente ao chão.
        arc(c, -6f, -10f, 26f, 24f, -70f, 140f, 3f, alpha(0xFFB07AF0, 180));
        c.fillRect(-length, -2f, 18f, 2f, alpha(accent, 60));
    }

    /**
     * O chicote esticado da mão (originX) até a ponta: couro que afina, uma onda pequena que
     * diminui ao esticar e o estalo na ponta.
     */
    private static void whip(RenderCanvas c, float travelled, float progress, float time, int accent) {
        int n = 24;
        float amp = 9f * (1f - progress) + 2f;
        // Contorno claro por baixo: o couro aparece em qualquer cenário.
        float ox = -travelled, oy = 0f;
        for (int i = 1; i <= n; i++) {
            float t = i / (float)n;
            float px = -travelled + travelled * t;
            float py = (float)Math.sin(t * Math.PI * 2f + time * 20f) * amp * t * (1f - t) * 4f;
            c.drawLine(ox, oy, px, py, 9.5f - 6.5f * t, 0x66F0E0C8);
            ox = px;
            oy = py;
        }
        float prevX = -travelled, prevY = 0f;
        for (int i = 1; i <= n; i++) {
            float t = i / (float)n;
            float px = -travelled + travelled * t;
            float py = (float)Math.sin(t * Math.PI * 2f + time * 20f) * amp * t * (1f - t) * 4f;
            float width = 7f - 4.5f * t;
            c.drawLine(prevX, prevY, px, py, width, 0xFF4A2B1A);
            c.drawLine(prevX, prevY - width * 0.25f, px, py - width * 0.25f, Math.max(0.8f, width * 0.3f), 0xFF8A5A3A);
            prevX = px;
            prevY = py;
        }
        c.fillRadialGlow(0f, prevY, 22f, alpha(accent, 0xC0));
        c.fillPolygon(star(6, 11f, 3.5f, time * 400f, 0f, prevY), WHITE);
    }

    // ------------------------------------------------------------ ajudantes

    private static float[] star(int points, float outer, float inner, float rotationDeg) {
        return star(points, outer, inner, rotationDeg, 0f, 0f);
    }

    private static float[] star(int points, float outer, float inner, float rotationDeg, float cx, float cy) {
        float[] p = new float[points * 4];
        double rot = Math.toRadians(rotationDeg);
        for (int i = 0; i < points * 2; i++) {
            double a = rot + Math.PI * i / points;
            float r = i % 2 == 0 ? outer : inner;
            p[i * 2] = cx + r * (float)Math.cos(a);
            p[i * 2 + 1] = cy + r * (float)Math.sin(a);
        }
        return p;
    }

    /** Arco de elipse em segmentos (graus, sentido horário na tela). */
    private static void arc(RenderCanvas c, float cx, float cy, float rx, float ry, float startDeg, float sweepDeg,
                            float width, int argb) {
        int n = Math.max(4, (int)(Math.abs(sweepDeg) / 8f));
        double a0 = Math.toRadians(startDeg);
        float px = cx + rx * (float)Math.cos(a0), py = cy + ry * (float)Math.sin(a0);
        for (int i = 1; i <= n; i++) {
            double a = Math.toRadians(startDeg + sweepDeg * i / n);
            float x = cx + rx * (float)Math.cos(a), y = cy + ry * (float)Math.sin(a);
            c.drawLine(px, py, x, y, width, argb);
            px = x;
            py = y;
        }
    }

    private static int opaque(int argb) {
        return 0xFF000000 | (argb & 0xFFFFFF);
    }

    private static int alpha(int argb, int a) {
        return (Math.max(0, Math.min(255, a)) << 24) | (argb & 0xFFFFFF);
    }
}
