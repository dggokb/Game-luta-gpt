package com.gamelutagpt.stage;

import com.gamelutagpt.render.Rand;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Cenário em execução: projeta as camadas com a {@link StageCamera} a cada quadro.
 *
 * <p>O jogo chama, nesta ordem: {@link #setCamera} com a câmera da luta,
 * {@link #drawBackground} (tudo atrás dos lutadores, inclusive o piso), desenha os
 * lutadores e então {@link #drawForeground} (o que fica na frente). {@link #update}
 * avança as animações (pétalas, fogo, luzes) no passo da simulação.
 *
 * <p>Desenha em coordenadas virtuais de tela (1280x720), sem a transformação do mundo.
 */
public final class StageScene {
    private static final float VW = StageWorld.VW;
    private static final float VH = StageWorld.VH;
    /** Altura de cada faixa do piso na tela; menor = perspectiva mais lisa, mais desenhos. */
    private static final float FLOOR_STRIP = 3f;

    /** Camada de imagem já convertida para o mundo 3D na sua profundidade. */
    private static final class Placed {
        final StageDefinition.Layer layer;
        final RenderImage image;
        final float z;
        final float top, bottom, width, firstCenterX, period;

        Placed(StageDefinition.Layer layer, RenderImage image, float z,
               float top, float bottom, float width, float firstCenterX, float period) {
            this.layer = layer;
            this.image = image;
            this.z = z;
            this.top = top;
            this.bottom = bottom;
            this.width = width;
            this.firstCenterX = firstCenterX;
            this.period = period;
        }
    }

    private final StagePack pack;
    private final StageWorld world;
    private final StageCamera camera;
    private final List<Placed> placed = new ArrayList<>();
    private final RenderImage floorImage;
    /** Lajotas sem emblema para as cópias fora do centro (ou a própria imagem do piso). */
    private final RenderImage floorPlainImage;
    private final float floorTileWidth;
    private final float floorCenterX;
    private final Rand rand = new Rand(7);

    // Pétalas (posições no mundo 3D).
    private final float[] petalX, petalY, petalZ, petalPhase, petalSpin;
    private float time;

    public StageScene(StagePack pack, StageWorld world) {
        this.pack = pack;
        this.world = world;
        StageDefinition def = pack.definition;
        this.camera = new StageCamera(world.groundY, def.cameraDistance, def.horizon * VH);

        // A câmera de repouso converte o que o stage.json descreve em tela para o mundo.
        StageCamera rest = new StageCamera(world.groundY, def.cameraDistance, def.horizon * VH);
        rest.set(world.restZoom, world.restLeft(), world.restTop());
        camera.set(world.restZoom, world.restLeft(), world.restTop());

        for (StageDefinition.Layer layer : def.layers) {
            RenderImage image = pack.image(layer.image);
            if (layer.kind == StageDefinition.Kind.IMAGE && image == null) continue;
            float z = layer.depth;
            float scale = rest.scale(z);
            float top, bottom;
            if (layer.kind == StageDefinition.Kind.IMAGE && layer.onGround) {
                bottom = world.groundY;
                top = bottom - layer.height * VH / scale;
            } else if (layer.kind == StageDefinition.Kind.WATER) {
                top = bottom = world.groundY;
            } else {
                top = rest.worldY(layer.top * VH, z);
                bottom = rest.worldY(layer.bottom * VH, z);
            }
            float width = image != null ? (bottom - top) * image.width() / (float)image.height() : 0f;
            float centerX = rest.worldX(layer.x * VW, z);
            float period = Float.isNaN(layer.period) ? width : layer.period * VW / scale;
            placed.add(new Placed(layer, image, z, top, bottom, width, centerX, Math.max(1f, period)));
        }
        Collections.sort(placed, new Comparator<Placed>() {
            @Override
            public int compare(Placed a, Placed b) {
                return Float.compare(b.layer.sortDepth(), a.layer.sortDepth());
            }
        });

        StageDefinition.Floor floor = def.floor;
        floorImage = floor != null ? pack.image(floor.image) : null;
        RenderImage plain = floor != null ? pack.image(floor.plainImage) : null;
        floorPlainImage = plain != null ? plain : floorImage;
        floorTileWidth = floorImage != null
            ? (floor.far - floor.near) * floorImage.width() / (float)floorImage.height()
            : 1f;
        floorCenterX = floor != null && !Float.isNaN(floor.centerX) ? floor.centerX : world.restCenterX;

        int petals = def.petals != null ? def.petals.count : 0;
        petalX = new float[petals];
        petalY = new float[petals];
        petalZ = new float[petals];
        petalPhase = new float[petals];
        petalSpin = new float[petals];
        rand.reset(99);
        for (int i = 0; i < petals; i++) {
            petalZ[i] = rand.range(def.petals.near, def.petals.far);
            float half = VW * 0.6f / camera.scale(petalZ[i]);
            petalX[i] = world.restCenterX + rand.range(-half, half);
            petalY[i] = camera.worldY(rand.range(-0.1f, 1f) * VH, petalZ[i]);
            petalPhase[i] = rand.range(0f, 6.283f);
            petalSpin[i] = rand.range(-200f, 200f);
        }
    }

    public StageDefinition definition() {
        return pack.definition;
    }

    /** Opacidade do reflexo dos lutadores no piso (0 = sem reflexo). */
    public float fighterReflection() {
        return pack.definition.floor != null && floorImage != null ? pack.definition.floor.fighterReflection : 0f;
    }

    /** Copia a câmera da luta: zoom final, esquerda e topo visíveis no mundo. */
    public void setCamera(float zoom, float cameraLeft, float top) {
        camera.set(zoom, cameraLeft, top);
    }

    public void update(float dt) {
        time += dt;
        StageDefinition.Petals petals = pack.definition.petals;
        if (petals == null) return;
        for (int i = 0; i < petalX.length; i++) {
            float z = petalZ[i];
            petalY[i] += petals.fall * dt;
            petalX[i] += (petals.wind + (float)Math.sin(time * 1.3f + petalPhase[i]) * 35f) * dt;
            float half = VW * 0.6f / camera.scale(z);
            float center = camera.centerX();
            if (petalX[i] < center - half) petalX[i] += 2f * half;
            if (petalX[i] > center + half) petalX[i] -= 2f * half;
            if (petalY[i] > world.groundY || camera.screenY(petalY[i], z) > VH + 20f) {
                petalY[i] = camera.worldY(-20f, z);
            }
        }
    }

    // ------------------------------------------------------------- render

    /** Tudo o que fica atrás dos lutadores, inclusive o piso. */
    public void drawBackground(RenderCanvas c) {
        c.fillRect(-10f, -10f, VW + 10f, VH + 10f, pack.definition.backgroundColor);
        StageDefinition.Floor floor = pack.definition.floor;
        float floorFar = floor != null ? floor.far : 0f;

        for (Placed p : placed) {
            if (p.layer.sortDepth() > floorFar) drawLayer(c, p);
        }
        drawFloor(c);
        drawFloorReflections(c);
        for (Placed p : placed) {
            float depth = p.layer.sortDepth();
            if (depth <= floorFar && depth > 0f) drawLayer(c, p);
        }
        drawPetals(c, true);
    }

    /** O que fica na frente dos lutadores (profundidade ≤ 0). */
    public void drawForeground(RenderCanvas c) {
        for (Placed p : placed) {
            if (p.layer.sortDepth() <= 0f) drawLayer(c, p);
        }
        drawPetals(c, false);
    }

    private void drawLayer(RenderCanvas c, Placed p) {
        switch (p.layer.kind) {
            case IMAGE: drawImageLayer(c, p); break;
            case FOG: drawFog(c, p); break;
            case WATER: drawWater(c, p); break;
        }
    }

    private void drawImageLayer(RenderCanvas c, Placed p) {
        StageDefinition.Layer layer = p.layer;
        float z = p.z;
        float scale = camera.scale(z);
        float top = camera.screenY(p.top, z);
        float bottom = camera.screenY(p.bottom, z);
        float halfWidth = p.width * 0.5f * scale;
        if (layer.fillAbove != 0 && top > -10f) c.fillRect(-10f, -10f, VW + 10f, top + 1f, layer.fillAbove);
        if (bottom < 0f || top > VH) return;
        int alpha = Math.round(255f * layer.alpha);

        int first = 0, last = 0;
        if (layer.repeat != StageDefinition.Repeat.NONE) {
            float left = camera.worldX(0f, z) - p.width;
            float right = camera.worldX(VW, z) + p.width;
            first = (int)Math.floor((left - p.firstCenterX) / p.period);
            last = (int)Math.ceil((right - p.firstCenterX) / p.period);
        }
        for (int k = first; k <= last; k++) {
            float cx = camera.screenX(p.firstCenterX + k * p.period, z);
            float l = cx - halfWidth, r = cx + halfWidth;
            if (r < 0f || l > VW) continue;
            boolean mirrored = layer.repeat == StageDefinition.Repeat.MIRROR && (k & 1) != 0;
            drawImage(c, p.image, l, top, r, bottom, mirrored, alpha);
            if (layer.reflection > 0f) {
                c.save();
                c.scale(1f, -1f, 0f, bottom);
                drawImage(c, p.image, l, top, r, bottom, mirrored, Math.round(alpha * layer.reflection));
                c.restore();
            }
        }
        for (int k = first; k <= last; k++) {
            float cx = camera.screenX(p.firstCenterX + k * p.period, z);
            float l = cx - halfWidth, r = cx + halfWidth;
            if (r < -200f || l > VW + 200f) continue;
            boolean mirrored = layer.repeat == StageDefinition.Repeat.MIRROR && (k & 1) != 0;
            drawLights(c, layer, l, top, r, bottom, mirrored, k);
        }
    }

    private static void drawImage(RenderCanvas c, RenderImage image, float l, float t, float r, float b,
                                  boolean mirrored, int alpha) {
        if (mirrored) {
            c.save();
            c.scale(-1f, 1f, (l + r) * 0.5f, 0f);
            c.drawImage(image, l, t, r, b, alpha);
            c.restore();
        } else {
            c.drawImage(image, l, t, r, b, alpha);
        }
    }

    private void drawLights(RenderCanvas c, StageDefinition.Layer layer, float l, float t, float r, float b,
                            boolean mirrored, int instance) {
        float h = b - t;
        for (int i = 0; i < layer.lights.size(); i++) {
            StageDefinition.Light light = layer.lights.get(i);
            float lx = l + (mirrored ? 1f - light.x : light.x) * (r - l);
            float ly = t + light.y * h;
            float pulse = 1f + light.pulse * (float)Math.sin(time * 6.5f + instance * 1.7f + i * 2.3f);
            c.fillRadialGlow(lx, ly, light.radius * h * 2.2f, withAlpha(light.color, Math.round(150 * pulse)));
            c.fillRadialGlow(lx, ly, light.radius * h * 0.8f, withAlpha(light.color, Math.round(235 * pulse)));
        }
        for (int i = 0; i < layer.fires.size(); i++) {
            StageDefinition.Fire fire = layer.fires.get(i);
            float fx = l + (mirrored ? 1f - fire.x : fire.x) * (r - l);
            float fy = t + fire.y * h;
            drawFire(c, fx, fy, fire.size * h, instance * 31 + i);
        }
    }

    // -------------------------------------------------------------- piso

    private void drawFloor(RenderCanvas c) {
        StageDefinition.Floor floor = pack.definition.floor;
        float groundTop = camera.screenY(world.groundY, floor != null ? floor.far : 0f);
        if (floorImage == null) {
            // Sem imagem: um chão liso, para o cenário continuar jogável.
            float y = Math.max(groundTop, camera.horizonY());
            c.fillVerticalGradient(-10f, y, VW + 10f, VH + 10f, 0xFF2A2230, 0xFF120E18);
            return;
        }
        float imgW = floorImage.width();
        float imgH = floorImage.height();
        float start = Math.max(groundTop, camera.horizonY() + 0.5f);
        for (float y0 = start; y0 < VH; y0 += FLOOR_STRIP) {
            float y1 = Math.min(VH + 1f, y0 + FLOOR_STRIP);
            float zTop = camera.floorDepthAt(y0);
            float zBottom = camera.floorDepthAt(y1);
            if (Float.isNaN(zTop) || Float.isNaN(zBottom)) continue;
            float zMid = (zTop + zBottom) * 0.5f;

            // Linha da textura: a borda de cima da imagem é a profundidade "longe".
            float v0 = clamp((floor.far - zTop) / (floor.far - floor.near), 0f, 1f) * imgH;
            float v1 = clamp((floor.far - zBottom) / (floor.far - floor.near), 0f, 1f) * imgH;
            if (v1 - v0 < 1f) {
                float mid = Math.min(imgH - 0.5f, Math.max(0.5f, (v0 + v1) * 0.5f));
                v0 = mid - 0.5f;
                v1 = mid + 0.5f;
            }

            // Coluna da textura: x do mundo visível nessa profundidade, repetindo a imagem.
            float worldLeft = camera.worldX(0f, zMid);
            float worldRight = camera.worldX(VW, zMid);
            float uLeft = (worldLeft - (floorCenterX - floorTileWidth * 0.5f)) / floorTileWidth * imgW;
            float uRight = (worldRight - (floorCenterX - floorTileWidth * 0.5f)) / floorTileWidth * imgW;
            int tile = (int)Math.floor(uLeft / imgW);
            float u = uLeft;
            while (u < uRight) {
                float tileEnd = Math.min(uRight, (tile + 1) * imgW);
                float sx0 = (u - uLeft) / (uRight - uLeft) * VW;
                float sx1 = (tileEnd - uLeft) / (uRight - uLeft) * VW;
                // A cópia do centro tem o emblema; as outras usam só lajotas, se houver.
                RenderImage image = tile == 0 ? floorImage : floorPlainImage;
                float kx = image.width() / imgW;
                float ky = image.height() / imgH;
                c.drawImageRegion(image, (u - tile * imgW) * kx, v0 * ky, (tileEnd - tile * imgW) * kx, v1 * ky,
                    sx0, y0, sx1 + 0.5f, y1, 255);
                u = tileEnd;
                tile++;
            }
        }
    }

    /** Luzes e fogos que estão sobre o piso viram faixas de brilho no chão molhado. */
    private void drawFloorReflections(RenderCanvas c) {
        StageDefinition.Floor floor = pack.definition.floor;
        if (floor == null || floorImage == null || floor.lightReflection <= 0f) return;
        for (Placed p : placed) {
            if (p.layer.kind != StageDefinition.Kind.IMAGE || p.z < floor.near || p.z > floor.far) continue;
            if (p.layer.lights.isEmpty() && p.layer.fires.isEmpty()) continue;
            int first = 0, last = 0;
            if (p.layer.repeat != StageDefinition.Repeat.NONE) {
                first = (int)Math.floor((camera.worldX(0f, p.z) - p.width - p.firstCenterX) / p.period);
                last = (int)Math.ceil((camera.worldX(VW, p.z) + p.width - p.firstCenterX) / p.period);
            }
            float h = p.bottom - p.top;
            for (int k = first; k <= last; k++) {
                boolean mirrored = p.layer.repeat == StageDefinition.Repeat.MIRROR && (k & 1) != 0;
                float left = p.firstCenterX + k * p.period - p.width * 0.5f;
                for (StageDefinition.Light light : p.layer.lights) {
                    if (!light.reflect) continue;
                    float wx = left + (mirrored ? 1f - light.x : light.x) * p.width;
                    float wy = p.top + light.y * h;
                    reflection(c, wx, wy, p.z, light.radius * h, light.color, floor.lightReflection);
                }
                for (StageDefinition.Fire fire : p.layer.fires) {
                    float wx = left + (mirrored ? 1f - fire.x : fire.x) * p.width;
                    float wy = p.top + fire.y * h - fire.size * h * 0.4f;
                    reflection(c, wx, wy, p.z, fire.size * h * 0.6f, 0xFFFF8A2A, floor.lightReflection);
                }
            }
        }
    }

    private void reflection(RenderCanvas c, float wx, float wy, float z, float radiusWorld, int color, float strength) {
        float mirrorY = world.groundY + (world.groundY - wy);
        float sx = camera.screenX(wx, z);
        float sy = camera.screenY(mirrorY, z);
        if (sx < -200f || sx > VW + 200f || sy > VH + 200f) return;
        float radius = Math.max(4f, radiusWorld * camera.scale(z) * 1.6f);
        float pulse = 1f + 0.08f * (float)Math.sin(time * 9f + wx * 0.01f);
        c.save();
        c.scale(0.55f, 2.6f, sx, sy);
        c.fillRadialGlow(sx, sy, radius, withAlpha(color, Math.round(255f * strength * pulse)));
        c.restore();
    }

    // ------------------------------------------------------ água e neblina

    private void drawWater(RenderCanvas c, Placed p) {
        StageDefinition.Layer layer = p.layer;
        float yFar = camera.screenY(world.groundY, layer.waterFar);
        float yNear = camera.screenY(world.groundY, layer.waterNear);
        if (yNear <= yFar) return;
        int alpha = Math.round(255f * layer.alpha);
        c.fillVerticalGradient(-10f, yFar, VW + 10f, yNear + 1f,
            withAlpha(layer.color, alpha), withAlpha(layer.colorBottom, alpha));

        // Brilhos que piscam na superfície, presos ao mundo (andam com a câmera).
        rand.reset(4242);
        float span = 2400f;
        for (int i = 0; i < layer.shimmer; i++) {
            float z = layer.waterNear + (layer.waterFar - layer.waterNear) * rand.next();
            float wx = rand.next() * span;
            float phase = rand.range(0f, 6.283f);
            float scale = camera.scale(z);
            float half = VW * 0.5f / scale;
            float offset = camera.centerX() - half;
            float x = offset + ((wx - offset) % span + span) % span;
            if (x > camera.centerX() + half) continue;
            float sx = camera.screenX(x, z);
            float sy = camera.screenY(world.groundY, z);
            float twinkle = 0.5f + 0.5f * (float)Math.sin(time * 2.6f + phase);
            float length = (18f + 30f * rand.next()) * scale;
            c.drawLine(sx - length, sy, sx + length, sy, Math.max(1f, 2.2f * scale),
                withAlpha(0xFFFFE6C0, Math.round(150 * twinkle)));
        }
    }

    private void drawFog(RenderCanvas c, Placed p) {
        float top = camera.screenY(p.top, p.z);
        float bottom = camera.screenY(p.bottom, p.z);
        if (bottom < 0f || top > VH) return;
        int color = withAlpha(p.layer.color, Math.round(255f * p.layer.alpha));
        c.fillVerticalGradient(-10f, top, VW + 10f, bottom, color & 0x00FFFFFF, color);
    }

    // ------------------------------------------------------------ efeitos

    private void drawPetals(RenderCanvas c, boolean behind) {
        StageDefinition.Petals petals = pack.definition.petals;
        if (petals == null) return;
        for (int i = 0; i < petalX.length; i++) {
            float z = petalZ[i];
            if ((z > 0f) != behind || z <= camera.nearestDepth() + 50f) continue;
            float scale = camera.scale(z);
            float size = petals.size * scale;
            if (size < 0.7f) continue;
            float sx = camera.screenX(petalX[i], z);
            float sy = camera.screenY(petalY[i], z);
            if (sx < -40f || sx > VW + 40f || sy < -40f || sy > VH + 40f) continue;
            // Longe fica mais apagado (profundidade atmosférica).
            int alpha = Math.round(255f * clamp(scale / camera.zoom() * 1.2f, 0.35f, 1f));
            float flip = (float)Math.abs(Math.sin(time * 2.2f + petalPhase[i]));
            c.save();
            c.rotate(petalSpin[i] * time * 0.3f + petalPhase[i] * 57f, sx, sy);
            c.fillOval(sx - size, sy - size * (0.25f + 0.35f * flip), sx + size, sy + size * (0.25f + 0.35f * flip),
                withAlpha(petals.color, alpha));
            c.restore();
        }
    }

    /** Fogo de braseiro: três línguas de chama tremulando, brilho e brasas subindo. */
    private void drawFire(RenderCanvas c, float x, float baseY, float size, int seed) {
        if (size < 2f) return;
        float flicker = 1f + 0.10f * (float)Math.sin(time * 11f + seed);
        c.fillRadialGlow(x, baseY - size * 0.45f, size * 1.6f * flicker, 0x55FF7A20);
        drawFlame(c, x, baseY, size * 1.00f, size * 0.42f, 0xEEFF5A14, seed);
        drawFlame(c, x, baseY, size * 0.78f, size * 0.30f, 0xF2FFA126, seed + 7);
        drawFlame(c, x, baseY, size * 0.48f, size * 0.17f, 0xFFFFF0B0, seed + 13);
        for (int e = 0; e < 6; e++) {
            float phase = (time * 0.7f + e / 6f + seed * 0.13f) % 1f;
            float ex = x + (float)Math.sin(time * 3f + e * 2.1f + seed) * size * 0.35f * phase;
            float ey = baseY - size * (0.6f + phase * 2.2f);
            c.fillCircle(ex, ey, Math.max(1f, size * 0.035f), withAlpha(0xFFFFB050, Math.round(230 * (1f - phase))));
        }
    }

    private final float[] flame = new float[2 * 2 * 9];

    private void drawFlame(RenderCanvas c, float x, float baseY, float height, float halfWidth, int color, int seed) {
        int steps = 9;
        float h = height * (1f + 0.14f * (float)Math.sin(time * 13f + seed));
        for (int i = 0; i < steps; i++) {
            float u = i / (float)(steps - 1);
            // Gota: estreita na base, mais larga logo acima, fechando na ponta.
            float width = halfWidth * (float)Math.sin(Math.PI * (0.1f + 0.9f * u)) * (1f - 0.3f * u);
            width *= 1f + 0.18f * (float)Math.sin(time * 17f + u * 9f + seed);
            float sway = (float)Math.sin(time * 5f + seed + u * 3f) * halfWidth * 0.35f * u;
            float y = baseY - h * u;
            flame[i * 2] = x + sway - width;
            flame[i * 2 + 1] = y;
            int j = 2 * steps - 1 - i;
            flame[j * 2] = x + sway + width;
            flame[j * 2 + 1] = y;
        }
        c.fillPolygon(flame, color);
    }

    // --------------------------------------------------------------- util

    static int withAlpha(int color, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        int base = (color >>> 24) & 0xFF;
        return ((a * base / 255) << 24) | (color & 0x00FFFFFF);
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }
}
