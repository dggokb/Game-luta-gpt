package com.gamelutagpt.stage;

import com.gamelutagpt.render.Rand;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    /** Altura de cada faixa da bandeira na tela (o dobro no modo leve). */
    private static final float BANNER_STRIP = 3f;

    /** Camada de imagem já convertida para o mundo 3D na sua profundidade. */
    /** Profundidade do céu, para o reflexo do sol na água (o sol não anda com a câmera). */
    private static final float SKY_DEPTH = 100000f;

    private static final class Placed {
        final StageDefinition.Layer layer;
        final RenderImage image;
        final float z;
        final float top, bottom, width, firstCenterX, period;
        /** Com {@code apoio}: y do mundo, na profundidade do apoio, onde fica a base. */
        float supportBaseY = Float.NaN;
        /** Água com reflexo do sol: x do sol no mundo, na profundidade do céu. */
        float sunWorldX = Float.NaN;
        /** Imagens das bandeiras da camada, na ordem dela (null = não carregou). */
        final RenderImage[] banners;

        Placed(StageDefinition.Layer layer, RenderImage image, float z,
               float top, float bottom, float width, float firstCenterX, float period, RenderImage[] banners) {
            this.layer = layer;
            this.image = image;
            this.banners = banners;
            this.z = z;
            this.top = top;
            this.bottom = bottom;
            this.width = width;
            this.firstCenterX = firstCenterX;
            this.period = period;
        }
    }

    /** Ator em cena: a animação que toca agora e o tempo dela. */
    private static final class ActorState {
        final Placed placed;
        final StageActor actor;
        final String folder;
        final RenderImage idleSheet;
        String anim;
        float animTime;
        /** Reação esperando para começar (cada ator reage num tempo um pouco diferente). */
        String queued;
        float delay;
        /** Troca de animação: o último quadro da anterior some aos poucos por cima da nova. */
        RenderImage fadeSheet;
        float fadeLeft, fadeTop, fadeDx, fadeDy, fadeGrow, fadeCellW, fadeCellH;
        float fade;
        RenderImage lastSheet;
        float lastDx, lastDy, lastGrow, lastCellW, lastCellH, lastSl, lastSt;

        ActorState(Placed placed, StageActor actor, String folder, RenderImage idleSheet) {
            this.placed = placed;
            this.actor = actor;
            this.folder = folder;
            this.idleSheet = idleSheet;
            this.anim = actor.idle;
        }
    }

    private final StagePack pack;
    /** Atores pelo id da camada, na ordem do stage.json. */
    private final Map<String, ActorState> actors = new LinkedHashMap<>();
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
    // Chuva: gotas no mundo 3D (y é a ponta de baixo do risco) e respingos no piso.
    private final float[] rainX, rainY, rainZ, rainSpeed;
    private final float[] splashX, splashZ, splashAge;
    private int nextSplash;
    private final Rand rainRand = new Rand(31);
    private static final float SPLASH_LIFE = 0.32f;
    /** No modo leve só uma gota em cada tantas é desenhada. */
    static final int LIGHT_RAIN_STEP = 3;
    /** Pontos do anel do respingo, reaproveitados a cada desenho (sem alocar por quadro). */
    private final float[] splashRing = new float[16];
    private float time;

    // Relâmpagos: o raio de agora (pontos no mundo, na profundidade dele), quando caiu e quando
    // cai o próximo.
    private static final int BOLT_POINTS = 14, BRANCH_POINTS = 6, BRANCHES = 2;
    private final float[] boltX = new float[BOLT_POINTS], boltY = new float[BOLT_POINTS];
    private final float[][] branchX = new float[BRANCHES][BRANCH_POINTS], branchY = new float[BRANCHES][BRANCH_POINTS];
    private final int[] branchLength = new int[BRANCHES];
    private float strikeTime = -100f, nextStrike;
    private int strikes;
    private final Rand lightningRand = new Rand(57);

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
            StageActor actor = null;
            if (layer.kind == StageDefinition.Kind.ACTOR) {
                actor = pack.actor(layer.actor);
                StageActor.Animation idle = actor != null ? actor.animations.get(actor.idle) : null;
                image = idle != null ? pack.image(StagePack.actorImage(layer.actor, idle.sheet)) : null;
                if (image == null) continue;
            }
            float z = layer.depth;
            float scale = rest.scale(z);
            float top, bottom;
            if ((layer.kind == StageDefinition.Kind.IMAGE || layer.kind == StageDefinition.Kind.ACTOR) && layer.onGround) {
                bottom = world.groundY;
                top = bottom - layer.height * VH / scale;
            } else if (layer.kind == StageDefinition.Kind.WATER) {
                top = bottom = world.groundY;
            } else {
                top = rest.worldY(layer.top * VH, z);
                bottom = rest.worldY(layer.bottom * VH, z);
            }
            float aspect = image != null ? image.width() / (float)image.height() : 0f;
            if (actor != null) aspect = cellAspect(actor.animations.get(actor.idle), image);
            float width = (bottom - top) * aspect;
            float centerX = rest.worldX(layer.x * VW, z);
            float period = Float.isNaN(layer.period) ? width : layer.period * VW / scale;
            RenderImage[] banners = new RenderImage[layer.banners.size()];
            for (int i = 0; i < banners.length; i++) banners[i] = pack.image(layer.banners.get(i).image);
            Placed p = new Placed(layer, image, z, top, bottom, width, centerX, Math.max(1f, period), banners);
            if (layer.kind == StageDefinition.Kind.WATER && !Float.isNaN(layer.sunX)) {
                p.sunWorldX = rest.worldX(layer.sunX * VW, SKY_DEPTH);
            }
            if (!Float.isNaN(layer.support)) {
                p.supportBaseY = rest.worldY(rest.screenY(bottom, z), layer.support);
            }
            placed.add(p);
            if (actor != null) actors.put(layer.id, new ActorState(p, actor, layer.actor, image));
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

        StageDefinition.Rain rain = def.rain;
        int drops = rain != null ? rain.count : 0;
        rainX = new float[drops];
        rainY = new float[drops];
        rainZ = new float[drops];
        rainSpeed = new float[drops];
        for (int i = 0; i < drops; i++) {
            rainZ[i] = rainRand.range(rain.near, rain.far);
            float half = VW * 0.6f / camera.scale(rainZ[i]);
            rainX[i] = world.restCenterX + rainRand.range(-half, half);
            rainY[i] = camera.worldY(rainRand.range(-0.1f, 1f) * VH, rainZ[i]);
            rainSpeed[i] = rain.speed * rainRand.range(0.85f, 1.15f);
        }
        int splashes = rain != null ? rain.splashes : 0;
        splashX = new float[splashes];
        splashZ = new float[splashes];
        splashAge = new float[splashes];
        java.util.Arrays.fill(splashAge, SPLASH_LIFE);
        if (def.lightning != null) nextStrike = lightningRand.range(1.5f, def.lightning.minInterval);
    }

    public StageDefinition definition() {
        return pack.definition;
    }

    /** Opacidade do reflexo dos lutadores no piso (0 = sem reflexo). */
    public float fighterReflection() {
        if (!detalhe) return 0f;
        return pack.definition.floor != null && floorImage != null ? pack.definition.floor.fighterReflection : 0f;
    }

    /** Copia a câmera da luta: zoom final, esquerda e topo visíveis no mundo. */
    public void setCamera(float zoom, float cameraLeft, float top) {
        camera.set(zoom, cameraLeft, top);
    }

    /** Onde fica na tela o ponto {@code worldX} do mundo na profundidade {@code z}. */
    public float screenX(float worldX, float z) {
        return camera.screenX(worldX, z);
    }

    /** Altura do chão na tela na profundidade {@code z}. */
    public float groundScreenY(float z) {
        return camera.screenY(world.groundY, z);
    }

    /** Pixels de tela por unidade do mundo na profundidade {@code z}. */
    public float scale(float z) {
        return camera.scale(z);
    }

    /** O ponto do mundo na profundidade {@code z} que aparece em {@code screenX} da tela. */
    public float worldX(float screenX, float z) {
        return camera.worldX(screenX, z);
    }

    // ------------------------------------------------------------- atores

    /** Ids (das camadas) dos atores em cena. */
    public List<String> actorIds() {
        return new ArrayList<>(actors.keySet());
    }

    /** Animação que o ator está tocando agora, ou null se ele não existe. */
    public String actorAnimation(String id) {
        ActorState a = actors.get(id);
        return a == null ? null : a.anim;
    }

    /** O ator toca {@code anim} uma vez e volta à parada. Falso se o ator ou a animação não existe. */
    public boolean actorPlay(String id, String anim) {
        ActorState a = actors.get(id);
        if (a == null || !a.actor.animations.containsKey(anim)) return false;
        startFade(a);
        a.anim = anim;
        a.animTime = 0f;
        a.queued = null;
        return true;
    }

    /**
     * Um acontecimento do jogo ("licao", "impacto", "levou", "fim"...): cada ator que tem
     * reação para ele escolhe uma das animações da lista e toca, com um atraso pequeno e
     * diferente para cada um, para não reagirem como robôs.
     */
    public void react(String event) {
        for (ActorState a : actors.values()) {
            List<String> options = a.actor.reactions.get(event);
            if (options == null || options.isEmpty()) continue;
            int pick = Math.min(options.size() - 1, (int)(rand.next() * options.size()));
            a.queued = options.get(pick);
            a.delay = rand.range(0.05f, 0.35f);
        }
    }

    private static float cellAspect(StageActor.Animation a, RenderImage sheet) {
        int rows = (a.frames + a.columns - 1) / a.columns;
        return (sheet.width() / (float)a.columns) / (sheet.height() / (float)rows);
    }

    /** Tempo da transição entre duas animações do ator (a pose de uma não emenda na outra). */
    private static final float ACTOR_FADE = 0.22f;

    private static void startFade(ActorState a) {
        if (a.lastSheet == null) return;
        a.fadeSheet = a.lastSheet;
        a.fadeLeft = a.lastSl;
        a.fadeTop = a.lastSt;
        a.fadeCellW = a.lastCellW;
        a.fadeCellH = a.lastCellH;
        a.fadeDx = a.lastDx;
        a.fadeDy = a.lastDy;
        a.fadeGrow = a.lastGrow;
        a.fade = ACTOR_FADE;
    }

    private void updateActors(float dt) {
        for (ActorState a : actors.values()) {
            if (a.fade > 0f) a.fade = Math.max(0f, a.fade - dt);
            if (a.queued != null) {
                a.delay -= dt;
                if (a.delay <= 0f) actorPlay(a.placed.layer.id, a.queued);
            }
            if (a.anim.equals(a.actor.idle)) continue;
            a.animTime += dt;
            StageActor.Animation anim = a.actor.animations.get(a.anim);
            if (!anim.loop && a.animTime >= animDuration(a, anim)) {
                startFade(a);
                a.anim = a.actor.idle;
                a.animTime = 0f;
            }
        }
    }

    /** Duração da animação; sem folha carregada, o tempo do movimento provisório. */
    private float animDuration(ActorState a, StageActor.Animation anim) {
        RenderImage sheet = anim.sheet != null ? pack.image(StagePack.actorImage(a.folder, anim.sheet)) : null;
        return sheet != null ? anim.frames / anim.fps : anim.fallbackSeconds;
    }

    private void drawActor(RenderCanvas c, Placed p) {
        ActorState a = actors.get(p.layer.id);
        if (a == null) return;
        float z = p.z;
        float scale = camera.scale(z);
        float top = camera.screenY(p.top, z);
        float bottom = camera.screenY(p.bottom, z);
        float halfWidth = p.width * 0.5f * scale;
        float cx = camera.screenX(p.firstCenterX, z);
        if (bottom < 0f || top > VH || cx + halfWidth < 0f || cx - halfWidth > VW) return;

        StageActor.Animation anim = a.actor.animations.get(a.anim);
        RenderImage sheet = anim.sheet != null ? pack.image(StagePack.actorImage(a.folder, anim.sheet)) : null;
        float dx = 0f, dy = 0f, grow = 1f;
        int frame;
        if (sheet == null) {
            // Sem a arte da reação: a parada com um movimento provisório.
            StageActor.Animation idle = a.actor.animations.get(a.actor.idle);
            float[] m = StageActor.provisorio(a.anim, Math.min(1f, a.animTime / Math.max(0.01f, anim.fallbackSeconds)));
            float h = bottom - top;
            dx = m[0] * h;
            dy = m[1] * h;
            grow = m[2];
            anim = idle;
            sheet = a.idleSheet;
            frame = (int)(time * idle.fps) % idle.frames;
        } else if (anim.name.equals(a.actor.idle) || anim.loop) {
            float t = anim.name.equals(a.actor.idle) ? time : a.animTime;
            frame = (int)(t * anim.fps) % anim.frames;
        } else {
            frame = Math.min(anim.frames - 1, (int)(a.animTime * anim.fps));
        }
        int rows = (anim.frames + anim.columns - 1) / anim.columns;
        float cellW = sheet.width() / (float)anim.columns;
        float cellH = sheet.height() / (float)rows;
        float sl = (frame % anim.columns) * cellW;
        float st = (frame / anim.columns) * cellH;
        float h = (bottom - top) * grow;
        float w = h * cellW / cellH;
        float l = cx + dx - w * 0.5f, r = cx + dx + w * 0.5f;
        float b = bottom + dy, t = b - h;
        int alpha = Math.round(255f * p.layer.alpha);
        // Sombra de contato no chão (fica no lugar quando o ator pula de surpresa).
        float shadowW = w * 0.42f;
        c.fillOval(cx - shadowW, bottom - h * 0.018f, cx + shadowW, bottom + h * 0.018f, 0x55000000);
        if (p.layer.flip) {
            c.save();
            c.scale(-1f, 1f, cx, 0f);
        }
        c.drawImageRegion(sheet, sl, st, sl + cellW, st + cellH, l, t, r, b, alpha);
        if (a.fade > 0f && a.fadeSheet != null) {
            float fh = (bottom - top) * a.fadeGrow;
            float fw = fh * a.fadeCellW / a.fadeCellH;
            float fb = bottom + a.fadeDy * (bottom - top);
            float fl = cx + a.fadeDx * (bottom - top) - fw * 0.5f;
            c.drawImageRegion(a.fadeSheet, a.fadeLeft, a.fadeTop, a.fadeLeft + a.fadeCellW, a.fadeTop + a.fadeCellH,
                fl, fb - fh, fl + fw, fb, Math.round(alpha * a.fade / ACTOR_FADE));
        }
        if (p.layer.flip) c.restore();
        // O quadro de agora, para a transição se a animação trocar.
        float hh = Math.max(1f, bottom - top);
        a.lastSheet = sheet;
        a.lastSl = sl;
        a.lastSt = st;
        a.lastCellW = cellW;
        a.lastCellH = cellH;
        a.lastDx = dx / hh;
        a.lastDy = dy / hh;
        a.lastGrow = grow;
    }

    public void update(float dt) {
        time += dt;
        updateActors(dt);
        updateRain(dt);
        updateLightning(dt);
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

    private void updateRain(float dt) {
        StageDefinition.Rain rain = pack.definition.rain;
        if (rain == null) return;
        for (int i = 0; i < splashAge.length; i++) splashAge[i] += dt;
        StageDefinition.Floor floor = pack.definition.floor;
        float center = camera.centerX();
        for (int i = 0; i < rainX.length; i++) {
            float z = rainZ[i];
            rainY[i] += rainSpeed[i] * dt;
            rainX[i] += rain.wind * dt;
            float half = VW * 0.6f / camera.scale(z);
            if (rainX[i] < center - half) rainX[i] += 2f * half;
            if (rainX[i] > center + half) rainX[i] -= 2f * half;
            if (rainY[i] >= world.groundY) {
                // A gota bateu no chão: respinga se cair no piso e volta para cima da tela.
                if (splashX.length > 0 && floor != null && z >= floor.near && z <= floor.far) {
                    splashX[nextSplash] = rainX[i];
                    splashZ[nextSplash] = z;
                    splashAge[nextSplash] = 0f;
                    nextSplash = (nextSplash + 1) % splashX.length;
                }
                rainY[i] = camera.worldY(rainRand.range(-0.25f, -0.02f) * VH, z);
                rainX[i] = center + rainRand.range(-half, half);
            } else if (camera.screenY(rainY[i], z) > VH + 40f) {
                rainY[i] = camera.worldY(rainRand.range(-0.25f, -0.02f) * VH, z);
            }
        }
    }

    // ------------------------------------------------------------- render

    /**
     * Detalhe completo (padrão) ou leve: no leve ficam de fora os efeitos que só enfeitam e
     * custam encher a tela de novo — reflexos das camadas e das luzes no piso, neblina, brilhos
     * na água, pétalas e o halo das luzes (o fogo dos braseiros continua).
     */
    public void setDetalhe(boolean completo) {
        detalhe = completo;
    }

    public boolean detalhe() {
        return detalhe;
    }

    private boolean detalhe = true;

    /** Tudo o que fica atrás dos lutadores, inclusive o piso. */
    public void drawBackground(RenderCanvas c) {
        drawBackground(c, Float.NaN, null);
    }

    /**
     * Tudo o que fica atrás dos lutadores, com mais um desenho ({@code between}, como o
     * narrador no palco) na profundidade {@code z}: na frente das camadas mais fundas que ele
     * (e do piso, se {@code z} está dentro dele) e atrás das mais rasas.
     */
    public void drawBackground(RenderCanvas c, float z, Runnable between) {
        boolean pending = between != null;
        c.fillRect(-10f, -10f, VW + 10f, VH + 10f, pack.definition.backgroundColor);
        StageDefinition.Floor floor = pack.definition.floor;
        float floorFar = floor != null ? floor.far : 0f;

        StageDefinition.Lightning lightning = pack.definition.lightning;
        boolean bolt = lightning != null;
        for (Placed p : placed) {
            if (p.layer.sortDepth() <= floorFar) continue;
            if (bolt && p.layer.sortDepth() <= lightning.depth) {
                drawLightning(c);
                bolt = false;
            }
            if (pending && p.layer.sortDepth() <= z) {
                between.run();
                pending = false;
            }
            drawLayer(c, p);
        }
        if (bolt) drawLightning(c);
        if (pending && z > floorFar) {
            between.run();
            pending = false;
        }
        drawFloor(c);
        drawFloorReflections(c);
        for (Placed p : placed) {
            float depth = p.layer.sortDepth();
            if (depth <= floorFar && depth > 0f) {
                if (pending && depth <= z) {
                    between.run();
                    pending = false;
                }
                drawLayer(c, p);
            }
        }
        if (pending) between.run();
        drawSceneFlash(c);
        drawSplashes(c, true);
        drawRain(c, true);
        drawPetals(c, true);
    }

    /** O que fica na frente dos lutadores (profundidade ≤ 0). */
    public void drawForeground(RenderCanvas c) {
        for (Placed p : placed) {
            if (p.layer.sortDepth() <= 0f) drawLayer(c, p);
        }
        drawSplashes(c, false);
        drawRain(c, false);
        drawPetals(c, false);
    }

    private void drawLayer(RenderCanvas c, Placed p) {
        switch (p.layer.kind) {
            case IMAGE: drawImageLayer(c, p); break;
            case FOG: drawFog(c, p); break;
            case WATER: drawWater(c, p); break;
            case ACTOR: drawActor(c, p); break;
        }
    }

    private void drawImageLayer(RenderCanvas c, Placed p) {
        StageDefinition.Layer layer = p.layer;
        float z = p.z;
        float scale = camera.scale(z);
        float top = camera.screenY(p.top, z);
        float bottom = camera.screenY(p.bottom, z);
        if (!Float.isNaN(p.supportBaseY)) {
            // Apoiada noutra camada (o mar): a base anda na vertical junto com ela.
            bottom = camera.screenY(p.supportBaseY, layer.support);
            top = bottom - (p.bottom - p.top) * scale;
        }
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
            drawBanners(c, p, l, top, r, bottom, mirrored, alpha, k);
            if (layer.reflection > 0f && detalhe) {
                c.save();
                c.scale(1f, -1f, 0f, bottom);
                int reflected = Math.round(alpha * layer.reflection);
                drawImage(c, p.image, l, top, r, bottom, mirrored, reflected);
                drawBanners(c, p, l, top, r, bottom, mirrored, reflected, k);
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

    /**
     * Bandeiras da camada balançando: o pano é desenhado em faixas horizontais e cada faixa
     * vai para o lado conforme o vento. Presa em cima (a faixa do topo não sai do lugar),
     * a ponta de baixo é a que mais anda; uma onda desce pelo pano e as faixas estreitam um
     * pouco onde ele dobra. Cada cópia da camada balança num tempo diferente.
     */
    private void drawBanners(RenderCanvas c, Placed p, float l, float t, float r, float b,
                             boolean mirrored, int alpha, int instance) {
        List<StageDefinition.Banner> list = p.layer.banners;
        float w = r - l, h = b - t;
        for (int i = 0; i < list.size(); i++) {
            RenderImage image = p.banners[i];
            if (image == null) continue;
            StageDefinition.Banner banner = list.get(i);
            float bw = banner.width * w, bh = banner.height * h;
            float bl = l + (mirrored ? 1f - banner.x - banner.width : banner.x) * w;
            float bt = t + banner.y * h;
            float amp = banner.sway * bw;
            if (bl - amp > VW || bl + bw + amp < 0f || bt > VH || bt + bh < 0f) continue;

            float seed = instance * 1.9f + i * 2.7f;
            float gust = 0.8f + 0.2f * (float)Math.sin(time * 0.41f + seed);
            float swing = (float)Math.sin(time * 1.25f * banner.speed + seed);
            int rows = Math.round(clamp(bh / (detalhe ? BANNER_STRIP : BANNER_STRIP * 2f), 6f, 120f));
            float srcW = image.width(), srcH = image.height();
            float centerX = bl + bw * 0.5f;
            // Espelhada, a cópia vira em torno do próprio centro; o vento continua do mesmo lado.
            float side = mirrored ? -1f : 1f;
            if (mirrored) {
                c.save();
                c.scale(-1f, 1f, centerX, 0f);
            }
            for (int j = 0; j < rows; j++) {
                float v0 = j / (float)rows, v1 = (j + 1) / (float)rows;
                float v = (v0 + v1) * 0.5f;
                float weight = v * (float)Math.sqrt(v);
                float phase = 6.2832f * banner.waves * v - time * 3f * banner.speed + seed * 1.3f;
                float dx = side * amp * gust * weight * (0.6f * swing + 0.4f * (float)Math.sin(phase));
                float half = bw * 0.5f * (1f - 0.05f * weight * (float)Math.cos(phase));
                float cx = centerX + dx;
                c.drawImageRegion(image, 0f, v0 * srcH, srcW, v1 * srcH,
                    cx - half, bt + v0 * bh, cx + half, bt + v1 * bh + 0.75f, alpha);
            }
            if (mirrored) c.restore();
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
            // O halo largo é o que mais enche a tela; o miolo da luz fica no leve.
            if (detalhe) c.fillRadialGlow(lx, ly, light.radius * h * 2.2f, withAlpha(light.color, Math.round(150 * pulse)));
            c.fillRadialGlow(lx, ly, light.radius * h * 0.8f, withAlpha(light.color, Math.round(235 * pulse)));
        }
        for (int i = 0; i < layer.fires.size(); i++) {
            StageDefinition.Fire fire = layer.fires.get(i);
            float fx = l + (mirrored ? 1f - fire.x : fire.x) * (r - l);
            float fy = t + fire.y * h;
            drawFire(c, fx, fy, fire.size * h, instance * 31 + i);
        }
    }

    // --------------------------------------------------------- relâmpagos

    /** Quantos relâmpagos já caíram (para o jogo tocar o trovão, por exemplo). */
    public int strikes() {
        return strikes;
    }

    /** Força do clarão agora: pisca forte, quase apaga, pisca de novo e some (0 fora dele). */
    public float lightningFlash() {
        return lightningEnvelope(time - strikeTime);
    }

    static float lightningEnvelope(float t) {
        if (t < 0f) return 0f;
        if (t < 0.06f) return 1f;
        if (t < 0.12f) return 0.25f;
        if (t < 0.18f) return 0.85f;
        float k = 0.85f * (float)Math.exp(-(t - 0.18f) * 7f);
        return k < 0.02f ? 0f : k;
    }

    private void updateLightning(float dt) {
        StageDefinition.Lightning l = pack.definition.lightning;
        if (l == null) return;
        nextStrike -= dt;
        if (nextStrike > 0f) return;
        nextStrike = lightningRand.range(l.minInterval, l.maxInterval);
        strike(l);
    }

    /**
     * Um raio novo: cai de cima, em zigue-zague, num ponto da tela de agora (guardado no
     * mundo, então anda com a câmera como o resto da profundidade dele), com um ou dois galhos.
     */
    private void strike(StageDefinition.Lightning l) {
        strikeTime = time;
        strikes++;
        Rand r = lightningRand;
        float z = l.depth;
        float scale = camera.scale(z);
        float startX = camera.worldX(r.range(0.12f, 0.88f) * VW, z);
        float top = camera.worldY(l.top * VH, z), bottom = camera.worldY(l.bottom * VH, z);
        float step = 26f / scale;
        float x = startX;
        for (int i = 0; i < BOLT_POINTS; i++) {
            boltX[i] = x;
            boltY[i] = top + (bottom - top) * i / (BOLT_POINTS - 1f);
            x += r.range(-1f, 1f) * step;
        }
        for (int b = 0; b < BRANCHES; b++) {
            // O segundo galho só às vezes.
            branchLength[b] = b == 0 || r.next() < 0.5f ? BRANCH_POINTS : 0;
            if (branchLength[b] == 0) continue;
            int from = 3 + (int)(r.next() * (BOLT_POINTS - 7));
            float side = r.next() < 0.5f ? -1f : 1f;
            float bx = boltX[from], by = boltY[from];
            float segment = (bottom - top) / (BOLT_POINTS - 1f);
            for (int i = 0; i < BRANCH_POINTS; i++) {
                branchX[b][i] = bx;
                branchY[b][i] = by;
                bx += side * r.range(0.4f, 1.3f) * step;
                by += segment * r.range(0.5f, 0.9f);
            }
        }
    }

    /** Clarão do céu e o raio, na profundidade do raio (o que está na frente cobre). */
    private void drawLightning(RenderCanvas c) {
        StageDefinition.Lightning l = pack.definition.lightning;
        float k = lightningFlash();
        if (k <= 0f) return;
        c.fillRect(-10f, -10f, VW + 10f, VH + 10f, withAlpha(l.color, Math.round(255f * l.skyFlash * k)));
        float z = l.depth;
        float scale = camera.scale(z);
        drawBoltPath(c, boltX, boltY, BOLT_POINTS, z, l.color, k, Math.max(1.6f, 3.2f * scale * 1.6f));
        for (int b = 0; b < BRANCHES; b++) {
            if (branchLength[b] > 0) {
                drawBoltPath(c, branchX[b], branchY[b], branchLength[b], z, l.color, k * 0.8f,
                    Math.max(1f, 1.8f * scale * 1.6f));
            }
        }
    }

    private void drawBoltPath(RenderCanvas c, float[] xs, float[] ys, int n, float z, int color, float k, float width) {
        for (int i = 0; i + 1 < n; i++) {
            float x1 = camera.screenX(xs[i], z), y1 = camera.screenY(ys[i], z);
            float x2 = camera.screenX(xs[i + 1], z), y2 = camera.screenY(ys[i + 1], z);
            // Brilho largo em volta (só no modo completo) e o miolo quase branco.
            if (detalhe) c.drawLine(x1, y1, x2, y2, width * 5f, withAlpha(color, Math.round(70f * k)));
            c.drawLine(x1, y1, x2, y2, width * 2f, withAlpha(color, Math.round(170f * k)));
            c.drawLine(x1, y1, x2, y2, width, withAlpha(0xFFFFFFFF, Math.round(255f * k)));
        }
    }

    /** O cenário todo clareia um pouco no relâmpago (atrás dos lutadores). */
    private void drawSceneFlash(RenderCanvas c) {
        StageDefinition.Lightning l = pack.definition.lightning;
        if (l == null || l.sceneFlash <= 0f) return;
        float k = lightningFlash();
        if (k <= 0f) return;
        c.fillRect(-10f, -10f, VW + 10f, VH + 10f, withAlpha(l.color, Math.round(255f * l.sceneFlash * k)));
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
        if (!detalhe) return;
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
        if (p.image != null) {
            drawWaterSurface(c, p, Math.max(yFar, camera.horizonY() + 0.5f), Math.min(yNear, VH + 1f));
            // Lá longe a água some na cor do horizonte.
            float haze = yFar + (yNear - yFar) * 0.4f;
            c.fillVerticalGradient(-10f, yFar - 1f, VW + 10f, haze, withAlpha(layer.color, 230), layer.color & 0x00FFFFFF);
        } else {
            c.fillVerticalGradient(-10f, yFar, VW + 10f, yNear + 1f,
                withAlpha(layer.color, alpha), withAlpha(layer.colorBottom, alpha));
        }

        if (!detalhe) return;
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

    /**
     * Superfície da água deitada em perspectiva, em faixas como o piso: a textura (vista de
     * cima) se repete nas duas direções, anda com a corrente e, no fundo, as ondas ficam
     * pequenas e juntas. Sem isso o mar era uma foto em pé lá no fundo: as ondas grandes da
     * parte de baixo da foto apareciam logo atrás do piso, enormes perto de tudo.
     */
    private void drawWaterSurface(RenderCanvas c, Placed p, float top, float bottom) {
        StageDefinition.Layer layer = p.layer;
        RenderImage image = p.image;
        float imgW = image.width(), imgH = image.height();
        float tileW = layer.waterTile;
        float tileD = tileW * imgH / imgW;
        int alpha = Math.round(255f * layer.alpha);
        float shift = time * layer.drift;
        for (float y0 = top; y0 < bottom; y0 += FLOOR_STRIP) {
            float y1 = Math.min(bottom, y0 + FLOOR_STRIP);
            float zTop = camera.floorDepthAt(y0), zBottom = camera.floorDepthAt(y1);
            if (Float.isNaN(zTop) || Float.isNaN(zBottom)) continue;
            zTop = Math.min(zTop, layer.waterFar);
            zBottom = Math.max(zBottom, layer.waterNear);
            float zMid = (zTop + zBottom) * 0.5f;
            float d = (layer.waterFar - zTop) / tileD;
            float v0 = (d - (float)Math.floor(d)) * imgH;
            float v1 = Math.min(imgH, v0 + Math.max(1f, (zTop - zBottom) / tileD * imgH));
            if (v1 - v0 < 1f) v0 = v1 - 1f;
            float uLeft = (camera.worldX(0f, zMid) + shift) / tileW * imgW;
            float uRight = (camera.worldX(VW, zMid) + shift) / tileW * imgW;
            int tile = (int)Math.floor(uLeft / imgW);
            float u = uLeft;
            while (u < uRight) {
                float tileEnd = Math.min(uRight, (tile + 1) * imgW);
                float sx0 = (u - uLeft) / (uRight - uLeft) * VW;
                float sx1 = (tileEnd - uLeft) / (uRight - uLeft) * VW;
                c.drawImageRegion(image, u - tile * imgW, v0, tileEnd - tile * imgW, v1, sx0, y0, sx1 + 0.5f, y1, alpha);
                u = tileEnd;
                tile++;
            }
        }
        if (Float.isNaN(p.sunWorldX) || !detalhe) return;
        // Caminho de luz do sol: risquinhos que piscam embaixo dele na tela, poucos e juntos
        // no horizonte, mais largos e espalhados perto.
        float sx = camera.screenX(p.sunWorldX, SKY_DEPTH);
        float horizon = camera.horizonY();
        rand.reset(777);
        for (float y = top; y < bottom; y += 2.5f) {
            float k = Math.max(0f, (y - horizon) / Math.max(1f, VH - horizon));
            float spread = 10f + 160f * k;
            int dashes = 2 + (int)(5f * k);
            for (int i = 0; i < dashes; i++) {
                float off = (rand.next() + rand.next() - 1f) * spread;
                float phase = rand.range(0f, 6.283f);
                float twinkle = 0.5f + 0.5f * (float)Math.sin(time * (2f + 2f * rand.next()) + phase);
                int a = Math.round(255f * layer.sunAlpha * twinkle * (1f - Math.abs(off) / (spread * 1.2f)));
                if (a <= 4) continue;
                float len = (3f + 22f * k) * (0.5f + rand.next());
                c.drawLine(sx + off - len, y, sx + off + len, y, 1f + 2f * k, withAlpha(layer.sunColor, a));
            }
        }
    }

    private void drawFog(RenderCanvas c, Placed p) {
        if (!detalhe) return;
        float top = camera.screenY(p.top, p.z);
        float bottom = camera.screenY(p.bottom, p.z);
        if (bottom < 0f || top > VH) return;
        int color = withAlpha(p.layer.color, Math.round(255f * p.layer.alpha));
        c.fillVerticalGradient(-10f, top, VW + 10f, bottom, color & 0x00FFFFFF, color);
    }

    // ------------------------------------------------------------ efeitos

    private void drawPetals(RenderCanvas c, boolean behind) {
        if (!detalhe) return;
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

    /** Riscos de chuva inclinados pelo vento; os de longe são finos e apagados. */
    private void drawRain(RenderCanvas c, boolean behind) {
        StageDefinition.Rain rain = pack.definition.rain;
        if (rain == null) return;
        float slant = rain.wind / rain.speed;
        // No modo leve chove menos (uma gota em cada LIGHT_RAIN_STEP), sem respingos: os riscos
        // são linhas finas e pesam pouco, e o cenário de chuva sem chuva perdia a cara.
        int step = detalhe ? 1 : LIGHT_RAIN_STEP;
        for (int i = 0; i < rainX.length; i += step) {
            float z = rainZ[i];
            if ((z > 0f) != behind || z <= camera.nearestDepth() + 50f) continue;
            float scale = camera.scale(z);
            float sx = camera.screenX(rainX[i], z);
            float sy = camera.screenY(rainY[i], z);
            float len = rain.length * scale * (rainSpeed[i] / rain.speed);
            if (sx < -60f || sx > VW + 60f || sy < -10f || sy - len > VH + 10f) continue;
            int alpha = Math.round(255f * rain.alpha * clamp(scale / camera.zoom() * 1.1f, 0.3f, 1f));
            c.drawLine(sx - slant * len, sy - len, sx, sy, Math.max(0.6f, rain.width * scale),
                withAlpha(rain.color, alpha));
        }
    }

    /** Respingo: um anel achatado que abre e some, e duas gotinhas que pulam. */
    private void drawSplashes(RenderCanvas c, boolean behind) {
        if (!detalhe) return;
        StageDefinition.Rain rain = pack.definition.rain;
        if (rain == null) return;
        for (int i = 0; i < splashX.length; i++) {
            float age = splashAge[i];
            float z = splashZ[i];
            if (age >= SPLASH_LIFE || (z > 0f) != behind || z <= camera.nearestDepth() + 50f) continue;
            float k = age / SPLASH_LIFE;
            float scale = camera.scale(z);
            float sx = camera.screenX(splashX[i], z);
            float sy = camera.screenY(world.groundY, z);
            if (sx < -30f || sx > VW + 30f || sy > VH + 10f) continue;
            float r = (3f + 14f * k) * scale;
            int alpha = Math.round(255f * rain.alpha * (1f - k));
            int color = withAlpha(rain.color, alpha);
            float w = Math.max(0.6f, 1.4f * scale);
            float[] ring = splashRing;
            for (int j = 0; j < 8; j++) {
                double a = j * Math.PI / 4.0;
                ring[2 * j] = sx + (float)Math.cos(a) * r;
                ring[2 * j + 1] = sy + (float)Math.sin(a) * r * 0.28f;
            }
            c.strokePolygon(ring, w, color);
            float hop = (float)Math.sin(k * Math.PI) * 9f * scale;
            c.fillCircle(sx - r * 0.5f, sy - hop, w, color);
            c.fillCircle(sx + r * 0.4f, sy - hop * 0.8f, w, color);
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
