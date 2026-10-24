package com.gamelutagpt;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import com.gamelutagpt.efeitos.ProjetilVisual;
import com.gamelutagpt.render.RenderCanvas;
import java.util.List;

/** Projectiles and Super cinematic overlays, drawn in world space. */
final class EffectsRenderer {
    private final Paint fx = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path tail = new Path();
    /** Unit-radius glows per colour, created once per fighter colour (never per frame). */
    private final java.util.Map<Integer, Shader> glows = new java.util.HashMap<>();
    private final Shader whiteCore = new RadialGradient(0f, 0f, 1f,
        new int[]{0xFFFFFFFF, 0xF0FFFFFF, 0x00FFFFFF}, new float[]{0f, .45f, 1f}, Shader.TileMode.CLAMP);

    /**
     * Projéteis desenhados com uma imagem tirada do vídeo do personagem (o balão da p16), em
     * vez do desenho por código do ProjetilVisual: nome do desenho -> arquivo em assets, em HD
     * (2 px de imagem por px do jogo).
     */
    static final java.util.Map<String, String> IMAGE_VISUALS = new java.util.HashMap<>();
    static { IMAGE_VISUALS.put("BALAO", "efeitos/balao_p16.webp"); }
    private final java.util.Map<String, Bitmap> images = new java.util.HashMap<>();
    private final RectF imageBox = new RectF();
    private final Paint imagePaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    void loadImages(AssetManager assets) {
        for (java.util.Map.Entry<String, String> e : IMAGE_VISUALS.entrySet()) {
            try (java.io.InputStream in = assets.open(e.getValue())) {
                Bitmap b = BitmapFactory.decodeStream(in);
                if (b != null) images.put(e.getKey(), b);
            } catch (java.io.IOException ignored) {
                // Sem a imagem o projétil não aparece, mas o golpe continua valendo.
            }
        }
    }

    /** O balão flutua: sobe e desce devagar e balança um pouco enquanto voa. */
    private void drawImage(Canvas c, Projectile p, Bitmap b) {
        float travelled = Math.abs(p.x - p.startX);
        float bob = 6f * (float)Math.sin(travelled / 40f);
        float w = b.getWidth() / 2f, h = b.getHeight() / 2f;
        c.save();
        c.translate(p.x, p.y + bob);
        c.rotate(4f * (float)Math.sin(travelled / 55f));
        if (p.direction < 0) c.scale(-1f, 1f);
        imageBox.set(-w / 2f, -h / 2f, w / 2f, h / 2f);
        c.drawBitmap(b, null, imageBox, imagePaint);
        c.restore();
    }

    private Shader glow(int color) {
        Shader shader = glows.get(color);
        if (shader == null) {
            int rgb = color & 0xFFFFFF;
            shader = new RadialGradient(0f, 0f, 1f,
                new int[]{0xE0000000 | rgb, 0x80000000 | rgb, 0x00000000 | rgb}, new float[]{0f, .45f, 1f},
                Shader.TileMode.CLAMP);
            glows.put(color, shader);
        }
        return shader;
    }

    /**
     * S1 em voo. A onda é a bola de energia daqui; os outros desenhos (palma, kunai, sombra,
     * chicote...) vêm do ProjetilVisual, o mesmo do visualizador no PC. {@code rc} já desenha
     * no mesmo {@code c}, com a câmera do mundo.
     */
    void drawEnergyProjectiles(Canvas c, RenderCanvas rc, List<Projectile> projectiles) {
        for (Projectile p : projectiles) {
            // A charged shot is bigger: the orb follows the projectile's own radius (24 at rest).
            float scale = p.radius / 24f;
            if (p.visual != null && IMAGE_VISUALS.containsKey(p.visual)) {
                Bitmap b = images.get(p.visual);
                if (b != null) drawImage(c, p, b);
            } else if (p.visual == null || "ONDA".equals(p.visual)) {
                drawOrb(c, p, 20f * scale, false);
            } else if ("ONDA_CURTA".equals(p.visual)) {
                drawOrb(c, p, 13f * scale, false);
            } else {
                float time = Math.abs(p.x - p.startX) / Math.max(1f, p.speed);
                ProjetilVisual.Tipo tipo = ProjetilVisual.Tipo.valueOf(p.visual);
                // A sombra se desenha a partir do chão; o projétil dela voa rente, logo acima.
                float y = tipo == ProjetilVisual.Tipo.SOMBRA ? Arena.GROUND_Y : p.y;
                // Em diagonal (shuriken, seringa): o desenho aponta para onde ele vai.
                float tilt = p.vy == 0f ? 0f : (float)Math.toDegrees(Math.atan2(p.vy, p.speed)) * p.direction;
                if (tilt != 0f) {
                    rc.save();
                    rc.rotate(tilt, p.x, y);
                }
                ProjetilVisual.draw(rc, tipo, p.x, y, p.direction,
                    p.startX, p.range, time, p.color);
                if (tilt != 0f) rc.restore();
            }
        }
    }

    /** Poças de veneno no chão (seringa da p11). */
    void drawPuddles(RenderCanvas rc, List<Puddle> puddles, float time) {
        for (Puddle pd : puddles) {
            ProjetilVisual.puddle(rc, pd.x, Arena.GROUND_Y, pd.halfWidth, pd.life(), time, pd.color);
        }
    }

    /**
     * Efeito do especial enquanto ele sai (o vapor da palma da p10): da preparação até um
     * pouco depois dos quadros ativos, saindo da mão (frente do alcance, na altura do golpe).
     */
    void drawSpecialEffect(RenderCanvas rc, CombatFighter f, float time) {
        if (!f.attacking() || f.attack == null) return;
        CharacterDefinition.Special special = f.character().specials.get(f.attack.id);
        if (special == null || !"VAPOR".equals(special.effect)) return;
        int start = f.attack.startupFrames - 1;
        float progress = (f.attackFrame - start) / (float)(f.attack.activeFrames + 14);
        if (progress < 0f || progress > 1f) return;
        float handX = f.x + f.facing * f.attack.reach * 0.55f;
        float handY = f.y - f.attack.hitHeight * 0.85f;
        ProjetilVisual.steam(rc, handX, handY, f.facing, progress, time);
    }

    /** Onda do Super (a do p01 acerta várias vezes) ou o desenho próprio dele (o fogo do p09, no chão). */
    void drawSuperProjectiles(Canvas c, RenderCanvas rc, List<Projectile> projectiles) {
        for (Projectile p : projectiles) {
            if (p.visual == null || "ONDA".equals(p.visual)) {
                // Parada no rival entre um acerto e outro, a onda pulsa.
                float pulse = p.holding ? 1f + 0.12f * (float)Math.sin(p.hitCooldown * 1.3f) : 1f;
                drawOrb(c, p, 40f * pulse, true);
                continue;
            }
            ProjetilVisual.Tipo tipo = ProjetilVisual.Tipo.valueOf(p.visual);
            float y = tipo == ProjetilVisual.Tipo.FOGO || tipo == ProjetilVisual.Tipo.SOMBRA ? Arena.GROUND_Y : p.y;
            float time = Math.abs(p.x - p.startX) / Math.max(1f, p.speed);
            ProjetilVisual.draw(rc, tipo, p.x, y, p.direction, p.startX, p.range, time, p.color);
        }
    }

    /** Energy orb: comet tail, soft glow, hot core and spinning rings. Canvas faces the travel. */
    private void drawOrb(Canvas c, Projectile p, float r, boolean big) {
        float travelled = Math.abs(p.x - p.startX);
        float spin = travelled / (big ? 55f : 34f);
        c.save();
        c.translate(p.x, p.y);
        if (p.direction < 0) c.scale(-1f, 1f);
        int rgb = p.color & 0xFFFFFF;

        // Tapered comet tail behind the orb, two layers.
        float length = Math.min(travelled, r * (big ? 5.2f : 4.4f));
        for (int layer = 0; layer < 2; layer++) {
            float w = r * (layer == 0 ? .95f : .45f);
            tail.reset();
            tail.moveTo(0f, -w);
            tail.cubicTo(-length * .45f, -w * .9f, -length * .8f, -w * .25f, -length, 0f);
            tail.cubicTo(-length * .8f, w * .25f, -length * .45f, w * .9f, 0f, w);
            tail.close();
            fx.setStyle(Paint.Style.FILL);
            fx.setColor(layer == 0 ? (0x88000000 | rgb) : 0xCCFFFFFF);
            c.drawPath(tail, fx);
        }
        // Trailing sparks peeling off the tail.
        fx.setStrokeCap(Paint.Cap.ROUND);
        fx.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < (big ? 8 : 5); i++) {
            float t = (spin * .35f + i / (big ? 8f : 5f)) % 1f;
            float sx = -r * .6f - t * length * 1.1f;
            float sy = (float)Math.sin(i * 2.7f + spin) * r * (.4f + .6f * t);
            fx.setStrokeWidth((big ? 4f : 3f) * (1f - t));
            fx.setColor(i % 2 == 0 ? 0xFFFFFFFF : (0xFF000000 | rgb));
            fx.setAlpha(Math.round(230 * (1f - t)));
            c.drawLine(sx, sy, sx - 10f - 12f * t, sy, fx);
        }

        // Soft outer glow and the orb body.
        fx.setStyle(Paint.Style.FILL);
        fx.setAlpha(255);
        c.save(); c.scale(r * 2.3f, r * 2.3f);
        fx.setShader(glow(p.color)); c.drawCircle(0f, 0f, 1f, fx);
        c.restore();
        fx.setShader(null);
        fx.setColor(0xFF000000 | rgb);
        c.drawCircle(0f, 0f, r, fx);
        c.save(); c.scale(r * .95f, r * .95f); c.translate(.12f, -.08f);
        fx.setShader(whiteCore); c.drawCircle(0f, 0f, 1f, fx);
        c.restore();
        fx.setShader(null);

        // Two counter-rotating energy rings.
        fx.setStyle(Paint.Style.STROKE);
        fx.setStrokeWidth(big ? 4f : 2.5f);
        float ring = r * 1.28f;
        fx.setColor(0xE6FFFFFF);
        c.drawArc(-ring, -ring * .78f, ring, ring * .78f, (spin * 57f) % 360f, 110f, false, fx);
        c.drawArc(-ring, -ring * .78f, ring, ring * .78f, (spin * 57f + 180f) % 360f, 80f, false, fx);
        fx.setColor(0xFF000000 | rgb);
        float ring2 = r * 1.55f;
        c.drawArc(-ring2, -ring2, ring2, ring2, (-spin * 41f) % 360f, 70f, false, fx);
        if (big) {
            // Crackling bolts around the Super orb.
            fx.setStrokeWidth(2.2f);
            fx.setColor(0xFFFFFFFF);
            for (int i = 0; i < 3; i++) {
                double a = spin * 1.7 + i * 2.094;
                float x0 = (float)Math.cos(a) * r * .9f, y0 = (float)Math.sin(a) * r * .9f;
                float x1 = (float)Math.cos(a + .25) * r * 1.5f, y1 = (float)Math.sin(a + .25) * r * 1.5f;
                float x2 = (float)Math.cos(a - .1) * r * 1.95f, y2 = (float)Math.sin(a - .1) * r * 1.95f;
                c.drawLine(x0, y0, x1, y1, fx);
                c.drawLine(x1, y1, x2, y2, fx);
            }
        }
        fx.setStrokeCap(Paint.Cap.BUTT);
        fx.setStyle(Paint.Style.FILL);
        fx.setAlpha(255);
        c.restore();
    }

    /** Full-world tint; used for the Super darkening and the release flash. */
    void drawWorldOverlay(Canvas c, Paint paint, int alpha, int red, int green, int blue) {
        if (alpha <= 0) return;
        paint.setColor(Color.argb(alpha, red, green, blue));
        c.drawRect(0f, Arena.WORLD_TOP, Arena.WORLD_WIDTH, Arena.VH + 120f, paint);
    }

    /** Ultra rush trail behind the fighter; canvas is already mirrored to its facing. */
    void drawUltraRush(Canvas c, Paint paint, float x, float baseY, int color) {
        float centerY = baseY - 78f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 7; i++) {
            float y = centerY - 72f + i * 24f;
            float length = 130f + (i % 3) * 70f;
            paint.setStrokeWidth(i % 2 == 0 ? 6f : 3f);
            paint.setColor(i % 2 == 0 ? color : Color.argb(200, 255, 255, 255));
            c.drawLine(x - 40f - length, y, x - 40f, y, paint);
        }
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStyle(Paint.Style.FILL);
    }

    /** Overdrive: a red glow around the body and sparks rising from the feet. */
    void drawOverdrive(Canvas c, Paint paint, float x, float baseY, float time) {
        float centerY = baseY - 100f;
        float pulse = 1f + 0.07f * (float)Math.sin(time * 14f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(55, 255, 60, 20));
        c.drawOval(x - 82f * pulse, centerY - 118f * pulse, x + 82f * pulse, baseY + 8f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 7; i++) {
            float phase = (time * 1.4f + i / 7f) % 1f;
            float px = x + (float)Math.sin(i * 2.3f + time * 2.5f) * 62f;
            float y = baseY - 6f - phase * 200f;
            paint.setStrokeWidth(5f - phase * 3f);
            paint.setColor(Color.argb(Math.round(210f * (1f - phase)), 255, 120 + i * 15, 40));
            c.drawLine(px, y, px, y - 30f, paint);
        }
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStyle(Paint.Style.FILL);
    }

    static final int AURA_POISON = 0, AURA_ARMOR = 1, AURA_COUNTER = 2, AURA_EVADE = 3, AURA_FURY = 4;

    /**
     * Character mechanic around a fighter (docs/personagens/mecanicas.md): green bubbles for
     * poison, a gold shell for armor, a cyan ring for a counter stance, a pale veil for an
     * evasion and red embers for fury. {@code strength} 0..1 scales the effect.
     */
    void drawMechanicAura(Canvas c, Paint paint, float x, float baseY, float time, int kind, float strength) {
        if (strength <= 0f) return;
        float centerY = baseY - 100f;
        int a = Math.round(255f * Math.min(1f, strength));
        float pulse = 1f + 0.06f * (float)Math.sin(time * 16f);
        paint.setStyle(Paint.Style.FILL);
        switch (kind) {
            case AURA_POISON:
                paint.setColor(Color.argb(a * 45 / 255, 90, 230, 70));
                c.drawOval(x - 70f, centerY - 105f, x + 70f, baseY + 6f, paint);
                for (int i = 0; i < 6; i++) {
                    float phase = (time * 0.9f + i / 6f) % 1f;
                    float px = x + (float)Math.sin(i * 1.9f + time * 3f) * 46f;
                    paint.setColor(Color.argb(Math.round(a * (1f - phase) * 0.85f), 120, 255, 90));
                    c.drawCircle(px, baseY - 20f - phase * 190f, 5f + (i % 3) * 2f, paint);
                }
                break;
            case AURA_ARMOR:
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(6f);
                paint.setColor(Color.argb(a * 200 / 255, 255, 205, 70));
                c.drawOval(x - 76f * pulse, centerY - 112f * pulse, x + 76f * pulse, baseY + 6f, paint);
                paint.setStrokeWidth(2f);
                paint.setColor(Color.argb(a * 150 / 255, 255, 250, 210));
                c.drawOval(x - 68f * pulse, centerY - 104f * pulse, x + 68f * pulse, baseY, paint);
                break;
            case AURA_COUNTER:
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(4f);
                paint.setColor(Color.argb(a * 210 / 255, 120, 230, 255));
                c.drawCircle(x, centerY, 78f * pulse, paint);
                paint.setColor(Color.argb(a * 120 / 255, 255, 255, 255));
                c.drawArc(x - 92f, centerY - 92f, x + 92f, centerY + 92f, time * 400f % 360f, 70f, false, paint);
                break;
            case AURA_EVADE:
                paint.setColor(Color.argb(a * 60 / 255, 230, 240, 255));
                c.drawOval(x - 64f, centerY - 108f, x + 64f, baseY + 4f, paint);
                break;
            case AURA_FURY:
                paint.setColor(Color.argb(a * 40 / 255, 255, 40, 30));
                c.drawOval(x - 74f * pulse, centerY - 110f * pulse, x + 74f * pulse, baseY + 8f, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeCap(Paint.Cap.ROUND);
                for (int i = 0; i < 5; i++) {
                    float phase = (time * 1.6f + i / 5f) % 1f;
                    float px = x + (float)Math.sin(i * 2.7f + time * 4f) * 54f;
                    paint.setStrokeWidth(4f - phase * 2f);
                    paint.setColor(Color.argb(Math.round(a * (1f - phase)), 255, 70 + i * 20, 30));
                    c.drawLine(px, baseY - 10f - phase * 170f, px, baseY - 30f - phase * 170f, paint);
                }
                paint.setStrokeCap(Paint.Cap.BUTT);
                break;
            default:
                break;
        }
        paint.setStyle(Paint.Style.FILL);
    }

    /** Air dash streaks behind the body; canvas is mirrored so the motion goes right. */
    void drawAirDash(Canvas c, Paint paint, float x, float baseY, int color) {
        float centerY = baseY - 90f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 4; i++) {
            float y = centerY - 48f + i * 32f;
            float length = 70f + (i % 2) * 45f;
            paint.setStrokeWidth(i % 2 == 0 ? 4f : 2.5f);
            paint.setColor(i % 2 == 0 ? Color.argb(170, 255, 255, 255) : (color & 0x00FFFFFF) | 0xB4000000);
            c.drawLine(x - 30f - length, y, x - 30f, y, paint);
        }
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStyle(Paint.Style.FILL);
    }

    /** Charge aura around the fighter: glow, spinning rune rings, converging lines and rising motes. */
    void drawSuperCharge(Canvas c, Paint paint, float x, float baseY, float phaseTimer, int color) {
        float centerY = baseY - 96f;
        int rgb = color & 0xFFFFFF;
        float pulse = 1f + 0.10f * (float)Math.sin(phaseTimer * 28f);
        c.save();
        c.translate(x, centerY);

        // Vertical aura column and the soft glow behind the body.
        fx.setStyle(Paint.Style.FILL);
        c.save(); c.scale(150f * pulse, 190f * pulse);
        fx.setShader(glow(color)); c.drawCircle(0f, 0f, 1f, fx);
        c.restore();
        fx.setShader(null);

        // Lines rushing inward from the edge of the screen area.
        fx.setStyle(Paint.Style.STROKE);
        fx.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 14; i++) {
            double a = i * 0.4488 + 0.21;
            float t = (phaseTimer * 2.6f + i * 0.37f) % 1f;
            float outer = 200f - 120f * t, inner = outer - 34f - 18f * (i % 3);
            fx.setStrokeWidth(i % 3 == 0 ? 3.5f : 2f);
            fx.setColor(i % 2 == 0 ? 0xFFFFFFFF : (0xFF000000 | rgb));
            fx.setAlpha(Math.round(220 * (float)Math.sin(Math.PI * t)));
            float cos = (float)Math.cos(a), sin = (float)Math.sin(a) * 1.15f;
            c.drawLine(cos * outer, sin * outer, cos * inner, sin * inner, fx);
        }
        fx.setAlpha(255);

        // Two elliptic rings tilted like a halo, dashed by arcs.
        for (int ring = 0; ring < 2; ring++) {
            float rx = (ring == 0 ? 92f : 122f) * pulse, ry = rx * (ring == 0 ? .34f : .28f);
            float yOffset = ring == 0 ? 62f : -8f;
            float spin = phaseTimer * (ring == 0 ? 420f : -300f);
            // Faint full ellipse, then bright travelling dashes on it.
            fx.setStrokeWidth(ring == 0 ? 3f : 2f);
            fx.setColor(0x66000000 | rgb);
            c.drawOval(-rx, yOffset - ry, rx, yOffset + ry, fx);
            fx.setStrokeWidth(ring == 0 ? 5f : 3f);
            for (int arc = 0; arc < 4; arc++) {
                fx.setColor(arc % 2 == 0 ? 0xF0FFFFFF : (0xFF000000 | rgb));
                c.drawArc(-rx, yOffset - ry, rx, yOffset + ry, spin + arc * 90f, 58f, false, fx);
            }
        }

        // Motes rising through the aura.
        for (int i = 0; i < 12; i++) {
            float t = (phaseTimer * 1.6f + i / 12f) % 1f;
            float px = (float)Math.sin(i * 2.3f + phaseTimer * 3f) * (40f + 9f * (i % 5));
            float py = 120f - t * 250f;
            fx.setStrokeWidth(4f - 2.5f * t);
            fx.setColor(i % 3 == 0 ? 0xFFFFFFFF : (0xFF000000 | rgb));
            fx.setAlpha(Math.round(255 * (1f - t)));
            c.drawLine(px, py, px, py + 16f + 18f * (1f - t), fx);
        }
        fx.setAlpha(255);
        fx.setStrokeCap(Paint.Cap.BUTT);
        fx.setStyle(Paint.Style.FILL);
        c.restore();
    }
}
