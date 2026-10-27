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
    static {
        IMAGE_VISUALS.put("BALAO", "efeitos/balao_p16.webp");
        IMAGE_VISUALS.put("CAIXA", "efeitos/caixa_p16_00.webp");
        IMAGE_VISUALS.put("PEDRA", "efeitos/pedra_p13.webp");
    }
    /**
     * A caixa surpresa abrindo (vídeo s2_2 da p16): uma imagem por pose, todas recortadas na
     * mesma janela, com o centro da caixa no chão em TRAP_ANCHOR (px da imagem HD). TRAP_AT é
     * em que ponto da abertura (0 a 1) cada uma entra, na proporção do vídeo.
     */
    private static final int TRAP_IMAGES = 16;
    private static final float[] TRAP_AT = {0f, 0f, .0625f, .125f, .1875f, .21875f, .28125f, .40625f,
        .59375f, .78125f, .8125f, .84375f, .875f, .90625f, .9375f, .97f};
    private static final float TRAP_ANCHOR_X = 113f, TRAP_ANCHOR_Y = 524f;
    private final Bitmap[] trapImages = new Bitmap[TRAP_IMAGES];
    private final java.util.Map<String, Bitmap> images = new java.util.HashMap<>();
    private final RectF imageBox = new RectF();
    private final Paint imagePaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    void loadImages(AssetManager assets) {
        for (java.util.Map.Entry<String, String> e : IMAGE_VISUALS.entrySet()) {
            Bitmap b = image(assets, e.getValue());
            if (b != null) images.put(e.getKey(), b);
        }
        for (int i = 0; i < TRAP_IMAGES; i++) {
            trapImages[i] = image(assets, String.format(java.util.Locale.ROOT, "efeitos/caixa_p16_%02d.webp", i));
        }
    }

    /** Sem a imagem o desenho não aparece, mas o golpe continua valendo. */
    private static Bitmap image(AssetManager assets, String path) {
        try (java.io.InputStream in = assets.open(path)) {
            return BitmapFactory.decodeStream(in);
        } catch (java.io.IOException e) {
            return null;
        }
    }

    /** Caixas surpresa no chão: fechada no pavio, depois a abertura e a luva saltando. */
    void drawTraps(Canvas c, List<Armadilha> traps) {
        for (Armadilha t : traps) {
            float progress = t.progress();
            int index = 0;
            if (t.open()) {
                index = 1;
                while (index + 1 < TRAP_IMAGES && TRAP_AT[index + 1] <= progress) index++;
            }
            Bitmap b = trapImages[index];
            if (b == null) continue;
            c.save();
            c.translate(t.x, Arena.GROUND_Y);
            if (t.direction < 0) c.scale(-1f, 1f);
            imageBox.set(-TRAP_ANCHOR_X / 2f, -TRAP_ANCHOR_Y / 2f,
                (b.getWidth() - TRAP_ANCHOR_X) / 2f, (b.getHeight() - TRAP_ANCHOR_Y) / 2f);
            c.drawBitmap(b, null, imageBox, imagePaint);
            c.restore();
        }
    }

    /**
     * Centro da caixa na imagem dela (px da imagem HD): a imagem tem a janela inteira da luva
     * saindo, e a caixa fica no canto de baixo. O giro é em volta dele (no próprio eixo).
     */
    private static final float BOX_CENTER_X = 112.5f, BOX_CENTER_Y = 466f, BOX_HALF_HEIGHT = 29.5f;

    /** O balão flutua (sobe, desce e balança); a caixa gira no próprio eixo. */
    private void drawImage(Canvas c, Projectile p, Bitmap b) {
        float travelled = Math.abs(p.x - p.startX);
        boolean box = "CAIXA".equals(p.visual);
        boolean rock = "PEDRA".equals(p.visual);
        float bob = box || rock ? 0f : 6f * (float)Math.sin(travelled / 40f);
        float w = b.getWidth() / 2f, h = b.getHeight() / 2f;
        c.save();
        // A caixa: o centro dela no projétil, meia caixa acima (pousa com o fundo no chão).
        c.translate(p.x, p.y + bob - (box ? BOX_HALF_HEIGHT : 0f));
        // A pedra rola no ar: uma volta a cada ~300 px para a frente.
        c.rotate(box ? boxSpin(p) : rock ? 1.2f * travelled * p.direction : 4f * (float)Math.sin(travelled / 55f));
        if (p.direction < 0) c.scale(-1f, 1f);
        if (box) {
            imageBox.set(-BOX_CENTER_X / 2f, -BOX_CENTER_Y / 2f, w - BOX_CENTER_X / 2f, h - BOX_CENTER_Y / 2f);
        } else {
            imageBox.set(-w / 2f, -h / 2f, w / 2f, h / 2f);
        }
        c.drawBitmap(b, null, imageBox, imagePaint);
        c.restore();
    }

    /**
     * A caixa anda pouco para a frente e cai rápido: o giro segue a queda, duas voltas inteiras
     * até o chão, onde ela pousa em pé como a armadilha.
     */
    static float boxSpin(Projectile p) {
        float fall = Arena.GROUND_Y - p.startY;
        float done = fall > 0f ? Math.max(0f, Math.min(1f, (p.y - p.startY) / fall)) : 0f;
        return 720f * done * p.direction;
    }

    /** Quadros (60 Hz) da explosão do canhão do Ultra da p16. */
    static final int CANNON_BLAST_FRAMES = 78;
    private static final int[] CONFETTI = {0xFFE53935, 0xFFFFD54F, 0xFF42A5F5, 0xFF66BB6A, 0xFFAB47BC,
        0xFFFFFFFF, 0xFFFF7043, 0xFF26C6DA};
    private static final int CONFETTI_PIECES = 56, SMOKE_PUFFS = 10;
    private final RectF blastBox = new RectF();

    /** Número fixo entre 0 e 1 para o pedaço {@code i} (a explosão é igual toda vez). */
    private static float blastNoise(int i, int salt) {
        int h = i * 374761393 + salt * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f;
    }

    /**
     * Explosão do canhão de circo do Ultra da p16, desenhada aqui e não na folha (no vídeo ela
     * passava da borda e saía cortada): clarão na boca, labareda ao longo do cano, onda de
     * choque, fumaça e uma chuva de confete. (x, y): boca do canhão; {@code angle}: graus acima
     * da horizontal, para o lado de {@code direction}; {@code age}: quadros desde o tiro.
     */
    void drawCannonBlast(Canvas c, float x, float y, float angle, int direction, int age) {
        float rad = (float)Math.toRadians(angle);
        float ax = (float)Math.cos(rad) * direction, ay = -(float)Math.sin(rad);
        fx.setShader(null);
        fx.setStyle(Paint.Style.FILL);
        // Fumaça: nuvens cinza (bordas suaves) que saem pela boca, crescem e sobem devagar.
        for (int i = 0; i < SMOKE_PUFFS; i++) {
            float start = 3f + 10f * blastNoise(i, 1);
            float life = age - start;
            if (life < 0f || life > 60f) continue;
            float k = life / 60f;
            float reach = (60f + 170f * blastNoise(i, 2)) * (1f - (1f - k) * (1f - k));
            float side = (blastNoise(i, 3) - 0.5f) * 120f * k;
            float px = x + ax * reach - ay * side * direction, py = y + ay * reach + ax * side * direction - 40f * k;
            float r = 50f + 80f * k + 26f * blastNoise(i, 4);
            int grey = 120 + Math.round(70f * blastNoise(i, 5));
            softBlob(c, px, py, r, r, Color.rgb(grey, grey, grey), Math.round(230f * (1f - k)));
        }
        // Labareda: chamas em camadas ao longo do cano (vermelha por fora, branca no meio).
        if (age < 18) {
            float k = age / 18f;
            float grow = Math.min(1f, (age + 1f) / 4f);
            c.save();
            c.translate(x, y);
            c.rotate(direction > 0 ? -angle : 180f + angle);
            int[] layers = {0xFFE65100, 0xFFFFA000, 0xFFFFEB3B, 0xFFFFFFFF};
            for (int l = 0; l < layers.length; l++) {
                float len = (330f - 60f * l) * grow * (1f - 0.3f * k);
                float half = (95f - 18f * l) * (1f - 0.5f * k);
                softBlob(c, len * 0.42f, 0f, len * 0.6f, half, layers[l], Math.round(255f * (1f - k)));
            }
            c.restore();
        }
        // Clarão na boca.
        if (age < 12) {
            float k = age / 12f;
            float r = 90f + 120f * k;
            c.save();
            c.translate(x, y);
            c.scale(r, r);
            fx.setShader(whiteCore);
            fx.setAlpha(Math.round(255f * (1f - k)));
            c.drawCircle(0f, 0f, 1f, fx);
            fx.setShader(null);
            c.restore();
        }
        // Onda de choque.
        if (age < 22) {
            float k = age / 22f;
            fx.setStyle(Paint.Style.STROKE);
            fx.setStrokeWidth(14f * (1f - k) + 2f);
            fx.setColor(Color.argb(Math.round(220f * (1f - k)), 255, 244, 214));
            c.drawCircle(x + ax * 40f, y + ay * 40f, 50f + 260f * k, fx);
            fx.setStyle(Paint.Style.FILL);
        }
        // Confete: sai em leque pelo cano, freia no ar, cai girando e some no fim.
        float fade = Math.min(1f, (CANNON_BLAST_FRAMES - age) / 20f);
        float t = Math.max(0f, age - 2f);
        float drift = (1f - (float)Math.pow(0.95, t)) / 0.05f;
        for (int i = 0; i < CONFETTI_PIECES; i++) {
            float spread = (float)Math.toRadians(angle + (blastNoise(i, 6) - 0.5f) * 70f);
            float speed = 9f + 11f * blastNoise(i, 7);
            float vx = (float)Math.cos(spread) * speed * direction, vy = -(float)Math.sin(spread) * speed;
            float px = x + vx * drift, py = y + vy * drift + 0.12f * t * t;
            float spin = age * (8f + 14f * blastNoise(i, 8)) + 360f * blastNoise(i, 9);
            float w = 7f + 6f * blastNoise(i, 10), h = 4f + 3f * blastNoise(i, 11);
            c.save();
            c.translate(px, py);
            c.rotate(spin);
            c.scale(1f, (float)Math.cos(Math.toRadians(spin * 1.7f)));   // vira de lado enquanto cai
            fx.setColor(CONFETTI[i % CONFETTI.length]);
            fx.setAlpha(Math.round(255f * Math.max(0f, fade)));
            blastBox.set(-w, -h, w, h);
            c.drawRect(blastBox, fx);
            c.restore();
        }
        fx.setAlpha(255);
    }

    /** Mancha de borda suave (o brilho radial da cor), com raios rx e ry, centro em (x, y). */
    private void softBlob(Canvas c, float x, float y, float rx, float ry, int color, int alpha) {
        if (alpha <= 0) return;
        c.save();
        c.translate(x, y);
        c.scale(rx, ry);
        fx.setShader(glow(color | 0xFF000000));
        fx.setAlpha(Math.min(255, alpha));
        c.drawCircle(0f, 0f, 1f, fx);
        fx.setShader(null);
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
            if ("LUVA".equals(p.visual)) continue;   // a luva da caixa: quem desenha é a armadilha
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

    /**
     * SHINKAI: brilho na cor do tipo em volta do corpo, chamas subindo dos pés e um anel no
     * chão. No fim ({@code left} < 0,2) a aura falha, avisando que vai acabar.
     */
    void drawShinkai(Canvas c, Paint paint, float x, float baseY, float time, int color, float left) {
        if (left < 0.2f && ((int)(time * 20f)) % 3 == 0) return;
        int r = Color.red(color), g = Color.green(color), b = Color.blue(color);
        int lr = (r + 255) / 2, lg = (g + 255) / 2, lb = (b + 255) / 2;
        float centerY = baseY - 105f;
        float pulse = 1f + 0.08f * (float)Math.sin(time * 14f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(70, r, g, b));
        c.drawOval(x - 95f * pulse, centerY - 135f * pulse, x + 95f * pulse, baseY + 10f, paint);
        paint.setColor(Color.argb(60, lr, lg, lb));
        c.drawOval(x - 62f * pulse, centerY - 100f * pulse, x + 62f * pulse, baseY - 2f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(Color.argb(210, lr, lg, lb));
        c.drawOval(x - 78f * pulse, baseY - 11f, x + 78f * pulse, baseY + 11f, paint);
        paint.setStrokeCap(Paint.Cap.ROUND);
        // Chamas subindo dos pés, na cor do tipo e clareando.
        for (int i = 0; i < 14; i++) {
            float phase = (time * 1.6f + i / 14f) % 1f;
            float px = x + (float)Math.sin(i * 2.3f + time * 2.5f) * 70f * (1f - phase * 0.4f);
            float y = baseY - 4f - phase * 240f;
            paint.setStrokeWidth(8f - phase * 5f);
            float light = (i % 3) / 2.5f;
            paint.setColor(Color.argb(Math.round(235f * (1f - phase)),
                Math.round(r + (255 - r) * light), Math.round(g + (255 - g) * light), Math.round(b + (255 - b) * light)));
            c.drawLine(px, y, px, y - 46f * (1f - phase * 0.5f), paint);
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
