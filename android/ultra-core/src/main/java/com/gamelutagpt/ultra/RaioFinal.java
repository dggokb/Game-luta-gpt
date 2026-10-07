package com.gamelutagpt.ultra;

import com.gamelutagpt.render.Rand;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;

/**
 * Desenha o raio final do ultra no mundo da luta, no estilo Kamehameha: a carga (aura, vento
 * entrando, poeira levantando, esfera crescendo nas mãos), o disparo (onda de choque e
 * rajada de vento), o feixe ondulando com anéis correndo por ele, o vento e a poeira enquanto
 * ele dura, a explosão de cada acerto e a explosão final.
 *
 * <p>Só desenha; o tempo dos acertos e o dano são do motor de combate. Coordenadas do mundo
 * (o jogo já aplicou a câmera). {@link #drawBehind} vai antes dos lutadores e
 * {@link #drawFront} depois. Tudo que é aleatório vem de sementes fixas, então o mesmo
 * quadro sai sempre igual.
 */
public final class RaioFinal {
    /** Velocidade com que a textura do feixe corre para a frente (px do mundo por segundo). */
    static final float SCROLL_SPEED = 1500f;
    /** Duração da explosão final. */
    public static final float BLAST_DURATION = 0.7f;

    /** Pose da folha do personagem (9 quadros: sai da guarda, carga, carga máxima, disparo, 3 de sustentação, recuperação, guarda). */
    public static final int POSE_FRAMES = 9;
    private static final int[] HOLD_CYCLE = {4, 5, 6, 5};

    /** O que o jogo informa a cada quadro. */
    public static final class Frame {
        /** Raiz do atacante (meio dos pés) e altura do corpo. */
        public float bodyX, groundY, bodyHeight = 226f;
        /** Mãos na pose de carga (onde a esfera cresce). */
        public float chargeX, chargeY;
        /** Mãos no disparo (de onde o feixe sai) e x do alvo. */
        public float originX, originY, targetX;
        public int facing = 1;
        /** Segundos desde o começo do raio (anima brilho e vento, não para nas pausas). */
        public float time;
        /** 0 a 1: quanto da carga já passou (1 depois do disparo). */
        public float charge = 1f;
        /** Segundos desde o disparo, ou negativo durante a carga. */
        public float sinceFire = 0f;
        /** 0 a 1: quanto do caminho até o alvo o feixe já percorreu. */
        public float reach;
        /** Segundos desde o último acerto pequeno (pulso no alvo), ou negativo antes do primeiro. */
        public float sinceHit = -1f;
        /** Acertos já dados (varia o desenho de cada impacto). */
        public int hits;
        /** Segundos desde a explosão final, ou negativo antes dela. */
        public float sinceBlast = -1f;
        /** 1 com o feixe cheio; cai até 0 enquanto ele some depois da explosão. */
        public float strength = 1f;
    }

    private final Rand rand = new Rand(7);

    /**
     * Quadro da folha de pose (0 a 8) para um frame da fase do raio: sai da guarda e carrega
     * até o disparo, alterna a sustentação enquanto o feixe dura e volta para a guarda.
     */
    public static int poseFrame(int frame, int fireFrame, int blastFrame, int fadeFrames) {
        if (frame < fireFrame) {
            if (frame < 6) return 0;
            if (frame < fireFrame * 0.45f) return 1;
            return (frame / 4) % 2 == 0 ? 2 : 1;
        }
        if (frame < blastFrame) {
            int firing = frame - fireFrame;
            return firing < 6 ? 3 : HOLD_CYCLE[(firing / 4) % HOLD_CYCLE.length];
        }
        return frame - blastFrame < fadeFrames / 2 ? 7 : 8;
    }

    // ================================================================ atrás dos lutadores

    /** Aura atrás do atacante, brilho no chão e poeira. */
    public void drawBehind(RenderCanvas c, UltraPack pack, Frame f) {
        UltraDefinition def = pack.definition;
        float strength = clamp(f.strength, 0f, 1f);
        boolean fired = f.sinceFire >= 0f;
        c.save();
        if (f.facing < 0) c.scale(-1f, 1f, f.bodyX, 0f);
        float h = f.bodyHeight;
        float origin = local(f, f.originX);
        float tip = origin + Math.abs(f.targetX - f.originX) * clamp(f.reach, 0f, 1f);

        float aura = fired ? strength : Math.min(1f, f.charge * 2.5f);
        if (aura > 0f) {
            int alpha = Math.round(235f * aura);
            // Chão iluminado pela aura.
            c.fillOval(f.bodyX - h * 0.55f, f.groundY - h * 0.06f, f.bodyX + h * 0.55f, f.groundY + h * 0.07f,
                withAlpha(def.color, Math.round(90f * aura)));
            float auraH = h * 1.5f * (1f + 0.04f * (float)Math.sin(f.time * 21f));
            if (pack.beamAura != null) {
                int frame = (int)(f.time * 14f);
                drawSheet(c, pack.beamAura, def.beam.aura.frames, frame, f.bodyX, f.groundY + h * 0.06f, auraH, true, alpha);
            } else {
                c.fillRadialGlow(f.bodyX, f.groundY - h * 0.5f, h * 0.75f, withAlpha(def.color, alpha * 2 / 3));
            }
        }
        if (fired && strength > 0f && tip > origin) {
            // O raio ilumina o chão embaixo dele.
            c.fillOval(origin, f.groundY - h * 0.05f, tip + h * 0.2f, f.groundY + h * 0.06f,
                withAlpha(def.accentColor, Math.round(70f * strength)));
        }
        drawDust(c, pack, f, origin, tip, strength);
        // O vento do recuo passa atrás do corpo, para a pose continuar visível.
        if (fired && strength > 0f) drawBackWind(c, pack, f, origin, Math.round(255f * strength));
        c.restore();
    }

    private void drawDust(RenderCanvas c, UltraPack pack, Frame f, float origin, float tip, float strength) {
        float h = f.bodyHeight;
        if (f.sinceFire < 0f) {
            // Carga: a poeira levanta dos dois lados dos pés, cada vez maior.
            float size = h * (0.55f + 0.55f * f.charge);
            for (int side = 0; side < 2; side++) {
                float age = (f.time + side * 0.27f) % 0.6f;
                float x = f.bodyX + (side == 0 ? -1f : 1f) * h * 0.42f;
                dustPuff(c, pack, x, f.groundY, size, age / 0.6f, Math.round(210f * Math.min(1f, f.charge * 3f)), side == 0);
            }
            return;
        }
        if (f.sinceFire < 0.6f) {
            // Disparo: uma nuvem grande explode atrás dos pés.
            dustPuff(c, pack, f.bodyX - h * 0.35f, f.groundY, h * 1.7f, f.sinceFire / 0.6f, 235, true);
        }
        if (strength <= 0f) return;
        int alpha = Math.round(220f * strength);
        // Recuo: o pé de trás arrasta e solta poeira para trás.
        int last = (int)(f.sinceFire / 0.2f);
        for (int j = Math.max(0, last - 2); j <= last; j++) {
            float age = f.sinceFire - j * 0.2f;
            if (age < 0f || age > 0.6f) continue;
            dustPuff(c, pack, f.bodyX - h * 0.3f - age * 140f, f.groundY, h * 0.75f, age / 0.6f, alpha, true);
        }
        // Embaixo do raio: nuvens rolando para a frente ao longo do feixe.
        if (tip - origin < h * 0.5f) return;
        last = (int)(f.sinceFire / 0.09f);
        for (int j = Math.max(0, last - 6); j <= last; j++) {
            float age = f.sinceFire - j * 0.09f;
            if (age < 0f || age > 0.55f) continue;
            rand.reset(1000L + j);
            float x = origin + (tip - origin) * rand.range(0.15f, 0.95f) + age * 200f;
            if (x > tip + h * 0.3f) continue;
            dustPuff(c, pack, x, f.groundY, h * rand.range(0.6f, 0.95f), age / 0.55f, alpha, rand.next() < 0.5f);
        }
    }

    /** Uma nuvem de poeira em {@code progress} (0 nasce, 1 some), apoiada no chão. */
    private static void dustPuff(RenderCanvas c, UltraPack pack, float x, float ground, float width, float progress,
                                 int alpha, boolean flip) {
        if (progress < 0f || progress >= 1f || alpha <= 0) return;
        int fade = progress > 0.75f ? Math.round(alpha * (1f - progress) / 0.25f) : alpha;
        RenderImage dust = pack.beamDust;
        if (dust == null) {
            float r = width * (0.25f + 0.25f * progress);
            c.fillOval(x - r, ground - r * 0.9f, x + r, ground + r * 0.1f, withAlpha(0xFFD8CBB4, fade * 3 / 4));
            return;
        }
        int frames = pack.definition.beam.dust.frames;
        int frame = Math.min(frames - 1, (int)(progress * frames));
        float cellW = dust.width() / (float)frames;
        float height = width * dust.height() / cellW;
        if (flip) {
            c.save();
            c.scale(-1f, 1f, x, 0f);
        }
        drawSheet(c, dust, frames, frame, x, ground + height * 0.04f, height, false, fade);
        if (flip) c.restore();
    }

    // ================================================================ na frente

    /** Esfera de carga, feixe, vento, anéis, impactos e explosão final. */
    public void drawFront(RenderCanvas c, UltraPack pack, Frame f) {
        UltraDefinition def = pack.definition;
        float strength = clamp(f.strength, 0f, 1f);
        c.save();
        if (f.facing < 0) c.scale(-1f, 1f, f.bodyX, 0f);
        if (f.sinceFire < 0f) {
            drawCharge(c, pack, f);
            c.restore();
            return;
        }
        float origin = local(f, f.originX);
        float distance = Math.abs(f.targetX - f.originX) * clamp(f.reach, 0f, 1f);
        float tipX = origin + distance;
        float pulse = 1f + 0.05f * (float)Math.sin(f.time * 52f) + 0.03f * (float)Math.sin(f.time * 23f);
        float thickness = def.beam.thickness * pulse * (0.35f + 0.65f * strength);
        if (f.sinceFire < 0.15f) thickness *= 0.6f + 0.4f * f.sinceFire / 0.15f;
        if (f.sinceBlast >= 0f && f.sinceBlast < 0.18f) thickness *= 1f + 0.3f * (1f - f.sinceBlast / 0.18f);
        int alpha = Math.round(255f * strength);

        if (strength > 0f) {
            if (distance > 1f) {
                drawBody(c, pack, f, origin, tipX, thickness, alpha);
                drawRings(c, def, f, origin, tipX, thickness, alpha);
                drawBeamWind(c, pack, f, origin, tipX, thickness, alpha);
            }
            drawStart(c, pack, f, origin, thickness, alpha);
        }
        if (f.sinceFire < 0.32f) drawShockwave(c, def, origin, f.originY, f.sinceFire / 0.32f, f.bodyHeight);
        if (f.reach >= 1f && f.sinceBlast < 0f) drawHitImpact(c, pack, f, tipX, thickness);
        if (strength > 0f) drawSparks(c, def, f, origin, tipX, thickness, alpha);
        if (f.sinceBlast >= 0f && f.sinceBlast < BLAST_DURATION) drawBlast(c, pack, f, tipX, thickness);
        c.restore();
    }

    /** Desenha o raio inteiro numa chamada (para quem não separa atrás/na frente). */
    public void draw(RenderCanvas c, UltraPack pack, Frame f) {
        drawBehind(c, pack, f);
        drawFront(c, pack, f);
    }

    // ------------------------------------------------------------ carga

    private void drawCharge(RenderCanvas c, UltraPack pack, Frame f) {
        UltraDefinition def = pack.definition;
        float x = local(f, f.chargeX), y = f.chargeY;
        float k = clamp(f.charge, 0f, 1f);
        float radius = (12f + 44f * (float)Math.pow(k, 0.8f)) * (1f + 0.08f * (float)Math.sin(f.time * 37f));

        // Vento girando em volta e entrando na esfera.
        if (pack.beamWind != null && k > 0.1f) {
            int frames = def.beam.wind.frames;
            for (int i = 0; i < 3; i++) {
                float cycle = (f.time * 1.8f + i / 3f) % 1f;
                float angle = f.time * 160f + i * 120f;
                float width = f.bodyHeight * (0.95f - 0.5f * cycle);
                c.save();
                c.rotate(angle, x, y);
                float cellW = pack.beamWind.width() / (float)frames;
                float height = width * pack.beamWind.height() / cellW;
                // A cabeça do redemoinho (à direita na folha) aponta para a esfera.
                float right = x - radius * 0.4f - f.bodyHeight * 0.35f * (1f - cycle);
                drawSheetBox(c, pack.beamWind, frames, (int)(f.time * 16f) + i, right - width, y - height * 0.5f,
                    right, y + height * 0.5f, Math.round(200f * Math.min(1f, (1f - cycle) * 3f) * k));
                c.restore();
            }
        }
        // Faíscas sendo sugadas para a esfera.
        int spark = withAlpha(def.accentColor, Math.round(230f * Math.min(1f, k * 2f)));
        for (int i = 0; i < 18; i++) {
            rand.reset(500L + i);
            float angle = rand.range(0f, 6.2832f);
            float p = (f.time * rand.range(1.2f, 2.2f) + rand.next()) % 1f;
            float far = radius + (1f - p) * rand.range(90f, 170f);
            float near = far - 14f - 22f * (1f - p);
            float cos = (float)Math.cos(angle), sin = (float)Math.sin(angle);
            c.drawLine(x + cos * far, y + sin * far, x + cos * Math.max(radius, near), y + sin * Math.max(radius, near),
                2.5f, spark);
        }
        c.fillRadialGlow(x, y, radius * 2.6f, withAlpha(def.color, 210));
        c.fillRadialGlow(x, y, radius * 1.4f, withAlpha(def.accentColor, 240));
        c.fillRadialGlow(x, y, radius * 0.75f, 0xFFFFFFFF);
        c.fillCircle(x, y, radius * 0.32f, 0xFFFFFFFF);
    }

    // ------------------------------------------------------------ feixe

    private void drawBody(RenderCanvas c, UltraPack pack, Frame f, float origin, float tipX, float thickness, int alpha) {
        RenderImage body = pack.beamBody;
        RenderImage tip = pack.beamTip;
        if (body == null) {
            drawPlainBody(c, pack.definition, origin, tipX, f.originY, thickness, alpha);
            return;
        }
        float scale = thickness / body.height();
        float end = tipX;
        if (tip != null) {
            // A ponta tem o próprio pedaço de feixe: o corpo vai até o meio dela.
            float tipW = tip.width() * scale;
            float tipH = tip.height() * scale;
            float tipRight = tipX + tipW * 0.10f;
            float tipLeft = tipRight - tipW;
            end = Math.max(origin, tipLeft + tipW * 0.45f);
            float srcLeft = Math.max(0f, (origin - tipLeft) / scale);
            if (srcLeft < tip.width() - 1f) {
                drawWavyTiles(c, body, scale, f, origin, end, thickness, alpha);
                c.drawImageRegion(tip, srcLeft, 0f, tip.width(), tip.height(),
                    tipLeft + srcLeft * scale, f.originY - tipH * 0.5f, tipRight, f.originY + tipH * 0.5f, alpha);
                return;
            }
        }
        drawWavyTiles(c, body, scale, f, origin, end, thickness, alpha);
    }

    /**
     * Corpo repetido de {@code from} até {@code end}, com a textura correndo para a frente e
     * a espessura ondulando ao longo do feixe (fatias verticais de alturas diferentes).
     */
    private static void drawWavyTiles(RenderCanvas c, RenderImage body, float scale, Frame f, float from, float end,
                                      float thickness, int alpha) {
        float tileW = body.width() * scale;
        if (end <= from || tileW <= 0f) return;
        float offset = (f.time * SCROLL_SPEED) % tileW;
        float slice = 24f;
        for (float left = from; left < end; left += slice) {
            float right = Math.min(left + slice, end);
            float mid = (left + right) * 0.5f;
            float wave = 1f + 0.08f * (float)Math.sin(mid * 0.03f - f.time * 28f)
                + 0.04f * (float)Math.sin(mid * 0.071f + f.time * 17f);
            // Perto das mãos o feixe ainda está se abrindo.
            float open = Math.min(1f, 0.55f + (mid - from) / (thickness * 1.5f));
            float half = thickness * 0.5f * wave * open;
            // Recorte da textura (que se repete) para esta fatia.
            float u0 = ((left - from - offset) % tileW + tileW) % tileW;
            float u1 = u0 + (right - left);
            if (u1 <= tileW) {
                c.drawImageRegion(body, u0 / scale, 0f, u1 / scale, body.height(),
                    left, f.originY - half, right, f.originY + half, alpha);
            } else {
                float split = left + (tileW - u0);
                c.drawImageRegion(body, u0 / scale, 0f, body.width(), body.height(),
                    left, f.originY - half, split, f.originY + half, alpha);
                c.drawImageRegion(body, 0f, 0f, (u1 - tileW) / scale, body.height(),
                    split, f.originY - half, right, f.originY + half, alpha);
            }
        }
    }

    private static void drawPlainBody(RenderCanvas c, UltraDefinition def, float from, float to, float y,
                                      float thickness, int alpha) {
        int outer = withAlpha(def.color, alpha * 3 / 4);
        int clear = withAlpha(def.color, 0);
        float half = thickness * 0.38f;
        c.fillVerticalGradient(from, y - half, to, y, clear, outer);
        c.fillVerticalGradient(from, y, to, y + half, outer, clear);
        float core = thickness * 0.11f;
        c.fillRect(from, y - core, to, y + core, withAlpha(def.accentColor, alpha));
        c.fillRect(from, y - core * 0.5f, to, y + core * 0.5f, withAlpha(0xFFFFFFFF, alpha));
        c.fillRadialGlow(to, y, thickness * 0.6f, withAlpha(def.accentColor, alpha));
    }

    /** Anéis de energia saindo das mãos e correndo pelo feixe (como no painel final). */
    private static void drawRings(RenderCanvas c, UltraDefinition def, Frame f, float origin, float tipX,
                                  float thickness, int alpha) {
        float spacing = 0.16f, speed = 1100f;
        int last = (int)(f.sinceFire / spacing);
        for (int j = Math.max(0, last - 8); j <= last; j++) {
            float age = f.sinceFire - j * spacing;
            float x = origin + thickness * 0.6f + age * speed;
            if (age < 0f || x > tipX) continue;
            float along = (x - origin) / Math.max(1f, tipX - origin);
            int a = Math.round(alpha * 0.8f * (1f - along * along));
            float r = thickness * (0.4f + 0.12f * along);
            c.save();
            c.scale(0.3f, 1f, x, f.originY);
            c.strokeCircle(x, f.originY, r, 7f, withAlpha(def.accentColor, a));
            c.strokeCircle(x, f.originY, r * 0.92f, 3f, withAlpha(0xFFFFFFFF, a));
            c.restore();
        }
    }

    /** Rajadas de vento correndo junto com o feixe, nas bordas de cima e de baixo. */
    private static void drawBeamWind(RenderCanvas c, UltraPack pack, Frame f, float origin, float tipX,
                                     float thickness, int alpha) {
        RenderImage wind = pack.beamWind;
        if (wind == null) return;
        int frames = pack.definition.beam.wind.frames;
        float spacing = 0.1f, speed = 1300f, width = f.bodyHeight * 1.1f;
        float height = width * wind.height() / (wind.width() / (float)frames);
        int last = (int)(f.sinceFire / spacing);
        for (int j = Math.max(0, last - 8); j <= last; j++) {
            float age = f.sinceFire - j * spacing;
            float right = origin + age * speed;
            if (age < 0f || right - width > tipX) continue;
            float edge = (j % 2 == 0 ? -1f : 1f) * thickness * 0.42f;
            float along = Math.min(1f, (right - origin) / Math.max(1f, tipX - origin));
            drawSheetBox(c, wind, frames, j + (int)(age * 14f), right - width, f.originY + edge - height * 0.5f,
                right, f.originY + edge + height * 0.5f, Math.round(alpha * 0.75f * (1f - along * 0.6f)));
        }
    }

    /** O recuo sopra vento para trás em volta do atacante. */
    private static void drawBackWind(RenderCanvas c, UltraPack pack, Frame f, float origin, int alpha) {
        RenderImage wind = pack.beamWind;
        if (wind == null) return;
        int frames = pack.definition.beam.wind.frames;
        float spacing = 0.11f, speed = 950f;
        int last = (int)(f.sinceFire / spacing);
        for (int j = Math.max(0, last - 5); j <= last; j++) {
            float age = f.sinceFire - j * spacing;
            if (age < 0f || age > 0.45f) continue;
            float width = f.bodyHeight * (j < 3 ? 1.6f : 0.95f);
            float height = width * wind.height() / (wind.width() / (float)frames);
            float y = f.groundY - f.bodyHeight * (0.25f + 0.55f * ((j * 37) % 10) / 10f);
            float left = origin - f.bodyHeight * 0.3f - age * speed;
            int a = Math.round(alpha * 0.85f * (1f - age / 0.45f));
            // Na folha o vento vai para a direita; aqui ele sopra para trás.
            c.save();
            c.scale(-1f, 1f, left + width * 0.5f, 0f);
            drawSheetBox(c, wind, frames, j + (int)(age * 14f), left, y - height * 0.5f, left + width,
                y + height * 0.5f, a);
            c.restore();
        }
    }

    /** Onda de choque do disparo: anéis abrindo nas mãos e um clarão. */
    private static void drawShockwave(RenderCanvas c, UltraDefinition def, float x, float y, float t, float bodyHeight) {
        if (t < 0f || t >= 1f) return;
        c.fillRadialGlow(x, y, bodyHeight * 0.9f * (1f - t), withAlpha(0xFFFFFFFF, Math.round(230f * (1f - t))));
        for (int k = 0; k < 3; k++) {
            float r = bodyHeight * (0.25f + 1.2f * t) * (1f - k * 0.18f);
            int a = Math.round(255f * (1f - t) * (1f - k * 0.25f));
            c.save();
            c.scale(0.42f, 1f, x, y);
            c.strokeCircle(x, y, r, 10f * (1f - t) + 2f, withAlpha(k == 0 ? 0xFFFFFFFF : def.accentColor, a));
            c.restore();
        }
    }

    // ------------------------------------------------------------ mãos, alvo, explosão

    private static void drawStart(RenderCanvas c, UltraPack pack, Frame f, float origin, float thickness, int alpha) {
        RenderImage start = pack.beamStart;
        float size = thickness * 1.15f * (1f + 0.07f * (float)Math.sin(f.time * 31f));
        if (start == null) {
            c.fillRadialGlow(origin, f.originY, size * 0.6f, withAlpha(pack.definition.color, alpha));
            c.fillRadialGlow(origin, f.originY, size * 0.25f, withAlpha(0xFFFFFFFF, alpha));
            return;
        }
        float h = size;
        float w = h * start.width() / start.height();
        float[] center = pack.definition.beam.startCenter;
        float left = origin - w * center[0];
        float top = f.originY - h * center[1];
        c.drawImage(start, left, top, left + w, top + h, alpha);
    }

    private void drawHitImpact(RenderCanvas c, UltraPack pack, Frame f, float tipX, float thickness) {
        float kick = f.sinceHit >= 0f ? Math.max(0f, 1f - f.sinceHit / 0.08f) : 0f;
        float size = thickness * (1.05f + 0.45f * kick);
        drawExplosion(c, pack, tipX, f.originY, size, 170 + Math.round(85f * kick), (f.hits * 47) % 360);
        if (kick > 0f) {
            // Cada acerto solta um anel de choque no alvo.
            c.save();
            c.scale(0.45f, 1f, tipX, f.originY);
            c.strokeCircle(tipX, f.originY, thickness * (0.9f - 0.4f * kick), 5f,
                withAlpha(0xFFFFFFFF, Math.round(220f * kick)));
            c.restore();
        }
    }

    private void drawBlast(RenderCanvas c, UltraPack pack, Frame f, float tipX, float thickness) {
        float t = f.sinceBlast / BLAST_DURATION;
        float grow = 1f - (1f - Math.min(1f, t * 2.4f)) * (1f - Math.min(1f, t * 2.4f));
        float size = thickness * (1.6f + 2.6f * grow);
        int alpha = Math.round(255f * (1f - t * t));
        c.fillRadialGlow(tipX, f.originY, size * 0.9f, withAlpha(0xFFFFFFFF, Math.round(alpha * (1f - t))));
        drawExplosion(c, pack, tipX, f.originY, size, alpha, 15f + 40f * t);
        if (pack.beamDust != null) {
            dustPuff(c, pack, tipX, f.groundY, f.bodyHeight * 2.2f, t, 240, false);
        }
    }

    private static void drawExplosion(RenderCanvas c, UltraPack pack, float x, float y, float size, int alpha,
                                      float rotation) {
        RenderImage impact = pack.beamImpact;
        if (impact == null) {
            c.fillRadialGlow(x, y, size * 0.55f, withAlpha(pack.definition.color, alpha));
            c.fillRadialGlow(x, y, size * 0.28f, withAlpha(0xFFFFFFFF, alpha));
            return;
        }
        c.save();
        c.rotate(rotation, x, y);
        float half = size * 0.5f;
        c.drawImage(impact, x - half, y - half, x + half, y + half, alpha);
        c.restore();
    }

    /** Faíscas que escapam do feixe perto do alvo e das mãos. */
    private void drawSparks(RenderCanvas c, UltraDefinition def, Frame f, float origin, float tipX, float thickness,
                            int alpha) {
        rand.reset((long)(f.time * 30f));
        int color = withAlpha(def.accentColor, alpha);
        for (int i = 0; i < 14; i++) {
            boolean nearTip = i < 9;
            float x = nearTip ? tipX - rand.range(0f, thickness * 1.2f) : origin + rand.range(0f, thickness * 2f);
            float side = rand.next() < 0.5f ? -1f : 1f;
            float y = f.originY + side * rand.range(thickness * 0.15f, thickness * 0.55f);
            float len = rand.range(10f, 34f);
            float dy = side * rand.range(2f, 14f);
            c.drawLine(x, y, x - len, y + dy, rand.range(2f, 4f), color);
        }
    }

    // ------------------------------------------------------------ folhas

    /** Quadro de uma folha, centrado em x; apoiado em {@code y} (base) ou centrado nele. */
    private static void drawSheet(RenderCanvas c, RenderImage sheet, int frames, int frame, float x, float y,
                                  float height, boolean base, int alpha) {
        float cellW = sheet.width() / (float)frames;
        float width = height * cellW / sheet.height();
        float top = base ? y - height : y - height * 0.5f;
        drawSheetBox(c, sheet, frames, frame, x - width * 0.5f, top, x + width * 0.5f, top + height, alpha);
    }

    private static void drawSheetBox(RenderCanvas c, RenderImage sheet, int frames, int frame, float left, float top,
                                     float right, float bottom, int alpha) {
        if (alpha <= 0 || right <= left || bottom <= top) return;
        float cellW = sheet.width() / (float)frames;
        int index = ((frame % frames) + frames) % frames;
        c.drawImageRegion(sheet, index * cellW, 0f, (index + 1) * cellW, sheet.height(), left, top, right, bottom,
            Math.min(255, alpha));
    }

    /** x do mundo no espaço local (virado para a direita, espelhado em volta da raiz). */
    private static float local(Frame f, float x) {
        return f.facing >= 0 ? x : 2f * f.bodyX - x;
    }

    private static int withAlpha(int argb, int alpha) {
        int a = Math.max(0, Math.min(255, alpha)) * ((argb >>> 24) & 0xFF) / 255;
        return (a << 24) | (argb & 0xFFFFFF);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
