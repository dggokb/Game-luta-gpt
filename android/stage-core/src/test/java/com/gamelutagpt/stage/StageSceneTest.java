package com.gamelutagpt.stage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import java.io.IOException;
import org.junit.Test;

public class StageSceneTest {
    private static final float GROUND_Y = 565f;
    private static final StageWorld WORLD = new StageWorld(GROUND_Y, 2600f, 1.12f, 700f, 552f);

    private static StageCamera camera() {
        StageCamera camera = new StageCamera(GROUND_Y, 1000f, 288f);
        camera.set(1.12f, 400f, 72f);
        return camera;
    }

    @Test
    public void fightersPlaneMatchesTheGameTransform() {
        StageCamera camera = camera();
        float left = 400f, top = 72f, zoom = 1.12f;
        for (float x : new float[] {100f, 700f, 1500f}) {
            for (float y : new float[] {-200f, 300f, GROUND_Y}) {
                assertEquals((x - left) * zoom, camera.screenX(x, 0f), 0.01f);
                assertEquals((y - top) * zoom, camera.screenY(y, 0f), 0.01f);
            }
        }
    }

    @Test
    public void farLayersMoveAndGrowLessThanTheFightersPlane() {
        StageCamera camera = camera();
        float nearBefore = camera.screenX(900f, 0f);
        float farBefore = camera.screenX(900f, 4000f);
        camera.set(1.12f, 500f, 72f);
        float nearMove = Math.abs(camera.screenX(900f, 0f) - nearBefore);
        float farMove = Math.abs(camera.screenX(900f, 4000f) - farBefore);
        assertEquals(112f, nearMove, 0.01f);
        assertTrue("o fundo anda menos", farMove < nearMove * 0.25f);

        // Zoom: a camada de longe quase não cresce.
        camera.set(0.8f, 500f, 72f);
        float nearOut = camera.scale(0f), farOut = camera.scale(4000f);
        camera.set(1.12f, 500f, 72f);
        float nearIn = camera.scale(0f), farIn = camera.scale(4000f);
        assertTrue(nearIn / nearOut > 1.35f);
        assertTrue(farIn / farOut < 1.1f);
    }

    @Test
    public void floorDepthInvertsTheGroundProjection() {
        StageCamera camera = camera();
        for (float z : new float[] {-300f, 0f, 250f, 550f}) {
            float y = camera.screenY(GROUND_Y, z);
            assertEquals(z, camera.floorDepthAt(y), 0.5f);
        }
        assertTrue(Float.isNaN(camera.floorDepthAt(camera.horizonY() - 1f)));
    }

    @Test
    public void definitionReadsLayersAndValidates() {
        StageDefinition def = StageDefinition.fromJson("{\"nome\": \"T\", \"camadas\": ["
            + "{\"imagem\": \"a.png\", \"profundidade\": 300, \"noChao\": true, \"altura\": 0.5,"
            + " \"repetir\": \"espelhar\", \"luzes\": [{\"x\": 0.2}]},"
            + "{\"tipo\": \"agua\", \"perto\": 100, \"longe\": 900}],"
            + "\"chao\": {\"imagem\": \"c.jpg\"}}");
        assertEquals(2, def.layers.size());
        assertEquals(StageDefinition.Repeat.MIRROR, def.layers.get(0).repeat);
        assertEquals(1, def.layers.get(0).lights.size());
        assertEquals(StageDefinition.Kind.WATER, def.layers.get(1).kind);
        assertEquals(900f, def.layers.get(1).sortDepth(), 0f);
        assertNull(def.petals);
        assertEquals(1000f, def.cameraDistance, 0f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void layerNeedsPlacement() {
        StageDefinition.fromJson("{\"nome\": \"T\", \"camadas\": [{\"imagem\": \"a.png\", \"profundidade\": 10}]}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void floorNeedsOrderedDepths() {
        StageDefinition.fromJson("{\"nome\": \"T\", \"chao\": {\"imagem\": \"c.jpg\", \"perto\": 100, \"longe\": 50}}");
    }

    @Test
    public void chuvaCaiRespingaNoPisoESomeNoModoLeve() {
        String json = "{\"nome\": \"Chuva\", \"chao\": {\"imagem\": \"chao.jpg\", \"perto\": -400, \"longe\": 600},"
            + "\"chuva\": {\"quantidade\": 200, \"velocidade\": 1800, \"vento\": -150, \"respingos\": 20}}";
        StageDefinition def = StageDefinition.fromJson(json);
        assertEquals(200, def.rain.count);
        assertEquals(-150f, def.rain.wind, 0f);
        assertEquals(20, def.rain.splashes);
        assertNull("sem o campo, sem chuva", StageDefinition.fromJson("{\"nome\": \"S\"}").rain);

        StagePack pack = new StagePack(def, new java.util.HashMap<String, RenderImage>(),
            java.util.Collections.<String>emptyList());
        StageScene scene = new StageScene(pack, WORLD);
        scene.setCamera(1.12f, 140f, 72f);
        CountingCanvas canvas = new CountingCanvas();
        for (int frame = 0; frame < 120; frame++) {
            scene.update(1f / 60f);
            scene.drawBackground(canvas);
            scene.drawForeground(canvas);
            assertEquals(0, canvas.depth);
        }
        assertTrue("os riscos de chuva são desenhados", canvas.lines > 120 * 50);
        assertTrue("as gotas que batem no piso respingam", canvas.rings > 0);

        CountingCanvas cheio = new CountingCanvas();
        scene.update(1f / 60f);
        scene.drawBackground(cheio);
        scene.drawForeground(cheio);
        scene.setDetalhe(false);
        CountingCanvas leve = new CountingCanvas();
        scene.drawBackground(leve);
        scene.drawForeground(leve);
        // No leve chove menos (o cenário de chuva sem chuva perdia a cara), sem respingos.
        assertTrue("no modo leve ainda chove", leve.lines > 0);
        assertTrue("mas bem menos: " + leve.lines + " de " + cheio.lines, leve.lines * 2 < cheio.lines);
        assertEquals("sem respingos", 0, leve.rings);
    }

    @Test(expected = IllegalArgumentException.class)
    public void chuvaComLongeAntesDePertoEhErro() {
        StageDefinition.fromJson("{\"nome\": \"X\", \"chuva\": {\"perto\": 500, \"longe\": 100}}");
    }

    @Test
    public void sceneDrawsWithAndWithoutImagesAndKeepsTheCanvasBalanced() throws IOException {
        String json = "{\"nome\": \"T\", \"camadas\": ["
            + "{\"imagem\": \"ceu.jpg\", \"profundidade\": 100000, \"topo\": -0.1, \"base\": 0.6, \"repetir\": \"espelhar\","
            + " \"corAcima\": \"#000000\"},"
            + "{\"tipo\": \"neblina\", \"profundidade\": 2000, \"topo\": 0.3, \"base\": 0.5, \"cor\": \"#334455\"},"
            + "{\"tipo\": \"agua\", \"perto\": 1400, \"longe\": 4000},"
            + "{\"imagem\": \"pilar.png\", \"profundidade\": 300, \"noChao\": true, \"altura\": 1, \"repetir\": \"espelhar\","
            + " \"periodo\": 0.8, \"reflexo\": 0.3, \"luzes\": [{\"x\": 0.9, \"y\": 0.3}], \"fogos\": [{\"x\": 0.5}],"
            + " \"bandeiras\": [{\"imagem\": \"bandeira.png\", \"x\": 0.3, \"y\": 0.3, \"largura\": 0.3, \"altura\": 0.5}]},"
            + "{\"imagem\": \"frente.png\", \"profundidade\": -200, \"topo\": 0.8, \"base\": 1.1},"
            + "{\"imagem\": \"falta.png\", \"profundidade\": 50, \"topo\": 0.1, \"base\": 0.2}],"
            + "\"chao\": {\"imagem\": \"chao.jpg\", \"imagemLajotas\": \"lajotas.jpg\", \"reflexoLutadores\": 0.2},"
            + "\"petalas\": {\"quantidade\": 30}}";
        StagePack pack = StagePack.load(new FakeAssets(json), "stages/t");
        assertEquals("a imagem que falta vira aviso", 1, pack.warnings.size());

        StageScene scene = new StageScene(pack, WORLD);
        assertEquals(0.2f, scene.fighterReflection(), 0f);
        CountingCanvas canvas = new CountingCanvas();
        float[][] cameras = {{1.12f, 140f, 72f}, {0.78f, 0f, -100f}, {1.46f, 1900f, -500f}};
        for (float[] cam : cameras) {
            for (int frame = 0; frame < 30; frame++) {
                scene.setCamera(cam[0], cam[1], cam[2]);
                scene.update(1f / 60f);
                scene.drawBackground(canvas);
                scene.drawForeground(canvas);
                assertEquals(0, canvas.depth);
            }
        }
        assertTrue("o piso é desenhado em faixas", canvas.regions > 50);
        assertTrue(canvas.images > 0);

        StagePack empty = new StagePack(StageDefinition.fromJson(json), new java.util.HashMap<String, RenderImage>(),
            java.util.Collections.<String>emptyList());
        StageScene noArt = new StageScene(empty, WORLD);
        noArt.drawBackground(canvas);
        noArt.drawForeground(canvas);
        assertEquals("sem imagem do piso não há reflexo", 0f, noArt.fighterReflection(), 0f);
    }

    /**
     * Uma ilha apoiada no mar ({@code "apoio"}) não desliza sobre ele quando a câmera sobe no
     * super pulo: a base fica no mesmo ponto do mar. Sem o apoio, ela se separa.
     */
    @Test
    public void layerRestingOnTheSeaFollowsItWhenTheCameraRises() throws IOException {
        final FakeImage mar = new FakeImage(1600, 600), ilha = new FakeImage(1600, 900);
        for (boolean apoio : new boolean[] {true, false}) {
            final String json = "{\"nome\": \"T\", \"camadas\": ["
                + "{\"imagem\": \"mar.png\", \"profundidade\": 9000, \"topo\": 0.2, \"base\": 0.8},"
                + "{\"imagem\": \"ilha.png\", \"profundidade\": 1500, \"topo\": 0.1, \"base\": 0.45"
                + (apoio ? ", \"apoio\": 9000" : "") + "}]}";
            StagePack pack = StagePack.load(new RenderAssets() {
                @Override public String readText(String path) { return json; }
                @Override public RenderImage loadImage(String path) { return path.endsWith("mar.png") ? mar : ilha; }
            }, "stages/t");
            StageScene scene = new StageScene(pack, WORLD);
            float[] repouso = baseSobreOMar(scene, mar, ilha, 72f);
            float[] pulo = baseSobreOMar(scene, mar, ilha, -400f);
            // Onde a base da ilha cai dentro da imagem do mar (fração da altura dela).
            float antes = (repouso[1] - repouso[0]) / (repouso[2] - repouso[0]);
            float depois = (pulo[1] - pulo[0]) / (pulo[2] - pulo[0]);
            if (apoio) assertEquals("a base fica no mesmo ponto do mar", antes, depois, 0.002f);
            else assertTrue("sem apoio ela desliza", Math.abs(antes - depois) > 0.05f);
        }
    }

    /** {topo do mar, base da ilha, base do mar} na tela, com a câmera no topo {@code top}. */
    private static float[] baseSobreOMar(StageScene scene, final RenderImage mar, final RenderImage ilha, float top) {
        scene.setCamera(1.12f, 140f, top);
        final float[] out = new float[3];
        scene.drawBackground(new CountingCanvas() {
            @Override public void drawImage(RenderImage image, float l, float t, float r, float b, int alpha) {
                if (image == mar) { out[0] = t; out[2] = b; }
                if (image == ilha) out[1] = b;
            }
        });
        return out;
    }

    /** A plateia pula só dentro da faixa: nem na festa do KO ela invade o que está em volta. */
    @Test
    public void crowdJumpsOnlyInsideItsBand() throws IOException {
        final FakeImage arquibancada = new FakeImage(2000, 500);
        final String json = "{\"nome\": \"T\", \"camadas\": ["
            + "{\"imagem\": \"a.png\", \"profundidade\": 600, \"noChao\": true, \"altura\": 0.4,"
            + " \"plateia\": [{\"topo\": 0.3, \"base\": 0.6, \"pulo\": 0.02, \"colunas\": 50}]}]}";
        StagePack pack = StagePack.load(new RenderAssets() {
            @Override public String readText(String path) { return json; }
            @Override public RenderImage loadImage(String path) { return arquibancada; }
        }, "stages/t");
        StageScene scene = new StageScene(pack, WORLD);
        scene.setCamera(1.12f, 140f, 72f);
        scene.cheer(1f);
        final float[] layer = new float[2];
        final java.util.List<float[]> columns = new java.util.ArrayList<>();
        for (int frame = 0; frame < 30; frame++) {
            scene.update(1f / 60f);
            scene.drawBackground(new CountingCanvas() {
                @Override public void drawImage(RenderImage image, float l, float t, float r, float b, int alpha) {
                    layer[0] = t;
                    layer[1] = b;
                }
                @Override public void drawImageRegion(RenderImage image, float sl, float st, float sr, float sb,
                                                      float l, float t, float r, float b, int alpha) {
                    if (image == arquibancada) columns.add(new float[] {t, b});
                }
            });
        }
        assertTrue("a plateia se mexe", columns.size() > 100);
        float h = layer[1] - layer[0];
        for (float[] col : columns) {
            assertTrue("não invade o que está acima", col[0] >= layer[0] + 0.3f * h - 0.01f);
            assertTrue("não invade o que está abaixo", col[1] <= layer[0] + 0.6f * h + 0.01f);
        }
    }

    /** Plateia recortada: a gente (só na máscara) é desenhada inteira em todo quadro, pulando ou não. */
    @Test
    public void cutoutCrowdIsAlwaysDrawnWhole() throws IOException {
        final FakeImage arquibancada = new FakeImage(2000, 500), gente = new FakeImage(2000, 500);
        final String json = "{\"nome\": \"T\", \"camadas\": ["
            + "{\"imagem\": \"a.png\", \"profundidade\": 600, \"noChao\": true, \"altura\": 0.4,"
            + " \"plateia\": [{\"topo\": 0.3, \"base\": 0.6, \"pulo\": 0.02, \"colunas\": 50,"
            + " \"mascara\": \"g.png\", \"recortada\": true}]}]}";
        StagePack pack = StagePack.load(new RenderAssets() {
            @Override public String readText(String path) { return json; }
            @Override public RenderImage loadImage(String path) { return path.endsWith("g.png") ? gente : arquibancada; }
        }, "stages/t");
        StageScene scene = new StageScene(pack, WORLD);
        scene.setCamera(1.12f, 140f, 72f);
        for (int frame = 0; frame < 20; frame++) {
            scene.update(1f / 60f);
            final int[] columns = {0};
            scene.drawBackground(new CountingCanvas() {
                @Override public void drawImageRegion(RenderImage image, float sl, float st, float sr, float sb,
                                                      float l, float t, float r, float b, int alpha) {
                    if (image == gente) columns[0]++;
                }
            });
            assertEquals("todas as colunas da plateia, em todo quadro", 50, columns[0]);
        }
    }

    /** A bandeira é desenhada em faixas: presa em cima, a ponta de baixo balança com o tempo. */
    @Test
    public void bannersHangFromTheTopAndSwayAtTheBottom() throws IOException {
        final FakeImage pilar = new FakeImage(1000, 1500), bandeira = new FakeImage(300, 800);
        String json = "{\"nome\": \"T\", \"camadas\": ["
            + "{\"imagem\": \"pilar.png\", \"profundidade\": 300, \"noChao\": true, \"altura\": 1, \"x\": 0.4,"
            + " \"bandeiras\": [{\"imagem\": \"bandeira.png\", \"x\": 0.3, \"y\": 0.25, \"largura\": 0.3, \"altura\": 0.53}]}]}";
        StagePack pack = StagePack.load(new RenderAssets() {
            @Override public String readText(String path) { return json; }
            @Override public RenderImage loadImage(String path) {
                return path.endsWith("bandeira.png") ? bandeira : pilar;
            }
        }, "stages/t");
        assertEquals(1, pack.definition.layers.get(0).banners.size());
        StageScene scene = new StageScene(pack, WORLD);
        scene.setCamera(1.12f, 140f, 72f);

        float topMin = Float.MAX_VALUE, topMax = -Float.MAX_VALUE, tipMin = Float.MAX_VALUE, tipMax = -Float.MAX_VALUE;
        int strips = 0;
        for (int frame = 0; frame < 240; frame++) {
            scene.update(1f / 60f);
            CountingCanvas canvas = new CountingCanvas();
            canvas.watch = bandeira;
            scene.drawBackground(canvas);
            assertTrue("desenhada em faixas", canvas.watched.size() >= 6);
            float[] top = canvas.watched.get(0), tip = canvas.watched.get(canvas.watched.size() - 1);
            assertEquals("a primeira faixa começa no topo do pano", 0f, top[0], 0.01f);
            assertEquals("a última termina na ponta", bandeira.height(), tip[1], 0.01f);
            topMin = Math.min(topMin, (top[2] + top[3]) / 2);
            topMax = Math.max(topMax, (top[2] + top[3]) / 2);
            tipMin = Math.min(tipMin, (tip[2] + tip[3]) / 2);
            tipMax = Math.max(tipMax, (tip[2] + tip[3]) / 2);
            strips = canvas.watched.size();
        }
        assertTrue("presa em cima", topMax - topMin < 1.5f);
        assertTrue("a ponta balança: " + (tipMax - tipMin), tipMax - tipMin > 8f);

        scene.setDetalhe(false);
        CountingCanvas leve = new CountingCanvas();
        leve.watch = bandeira;
        scene.drawBackground(leve);
        assertTrue("menos faixas no modo leve", leve.watched.size() < strips);
    }

    /** Relâmpagos: caem de tempos em tempos, piscam duas vezes, clareiam o céu e o cenário. */
    @Test
    public void lightningStrikesFromTimeToTimeAndFlashes() throws IOException {
        String json = "{\"nome\": \"T\", \"camadas\": ["
            + "{\"imagem\": \"ceu.jpg\", \"profundidade\": 100000, \"topo\": -0.1, \"base\": 0.6},"
            + "{\"imagem\": \"pilar.png\", \"profundidade\": 300, \"noChao\": true, \"altura\": 1}],"
            + "\"chao\": {\"imagem\": \"chao.jpg\"},"
            + "\"relampagos\": {\"intervaloMinimo\": 2, \"intervaloMaximo\": 3, \"profundidade\": 9000}}";
        StagePack pack = StagePack.load(new FakeAssets(json), "stages/t");
        assertEquals(9000f, pack.definition.lightning.depth, 0f);
        StageScene scene = new StageScene(pack, WORLD);
        scene.setCamera(1.12f, 140f, 72f);
        int lit = 0, lines = 0;
        float lastFlash = 0f;
        for (int frame = 0; frame < 60 * 10; frame++) {
            scene.update(1f / 60f);
            CountingCanvas canvas = new CountingCanvas();
            scene.drawBackground(canvas);
            assertEquals(0, canvas.depth);
            if (scene.lightningFlash() > 0f) {
                lit++;
                lines += canvas.lines;
            }
            lastFlash = scene.lightningFlash();
        }
        assertTrue("caíram uns relâmpagos em 10 s: " + scene.strikes(), scene.strikes() >= 3 && scene.strikes() <= 5);
        assertTrue("o clarão dura um pouco", lit > scene.strikes() * 10);
        assertTrue("o raio é desenhado", lines > lit * 10);
        // Pisca: forte, quase apaga, forte de novo e some.
        assertEquals(1f, StageScene.lightningEnvelope(0.02f), 0f);
        assertTrue(StageScene.lightningEnvelope(0.09f) < 0.5f);
        assertTrue(StageScene.lightningEnvelope(0.15f) > 0.5f);
        assertEquals(0f, StageScene.lightningEnvelope(2f), 0f);
        assertEquals(0f, StageScene.lightningEnvelope(-1f), 0f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void lightningWithTheMaximumBelowTheMinimumIsAnError() {
        StageDefinition.fromJson("{\"nome\": \"X\", \"relampagos\": {\"intervaloMinimo\": 8, \"intervaloMaximo\": 3}}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void aBannerWithoutSizeIsAnError() {
        StageDefinition.fromJson("{\"nome\": \"X\", \"camadas\": [{\"imagem\": \"p.png\", \"profundidade\": 300,"
            + " \"noChao\": true, \"altura\": 1, \"bandeiras\": [{\"imagem\": \"b.png\"}]}]}");
    }

    @Test
    public void anExtraDrawingGoesBetweenTheDeeperAndTheShallowerLayers() throws IOException {
        final FakeImage parede = new FakeImage(800, 800), lanterna = new FakeImage(200, 400);
        String json = "{\"nome\": \"T\", \"camadas\": ["
            + "{\"imagem\": \"parede.png\", \"profundidade\": 400, \"noChao\": true, \"altura\": 0.8},"
            + "{\"imagem\": \"lanterna.png\", \"profundidade\": 120, \"noChao\": true, \"altura\": 0.3}],"
            + "\"chao\": {\"imagem\": \"chao.jpg\", \"longe\": 700}}";
        StagePack pack = StagePack.load(new RenderAssets() {
            @Override public String readText(String path) { return json; }
            @Override public RenderImage loadImage(String path) {
                return path.endsWith("parede.png") ? parede : path.endsWith("lanterna.png") ? lanterna : new FakeImage(512, 512);
            }
        }, "stages/t");
        StageScene scene = new StageScene(pack, WORLD);
        scene.setCamera(1.12f, 400f, 72f);
        final CountingCanvas canvas = new CountingCanvas();
        scene.drawBackground(canvas, 200f, () -> canvas.ordem.add("narrador"));
        int p = canvas.ordem.indexOf(parede), n = canvas.ordem.indexOf("narrador"), l = canvas.ordem.indexOf(lanterna);
        assertTrue("na frente da parede", p >= 0 && p < n);
        assertTrue("atrás da lanterna", n < l);
        assertEquals("uma vez só", 1, java.util.Collections.frequency(canvas.ordem, "narrador"));

        // A projeção na profundidade dele: anda e cresce menos que o plano dos lutadores.
        assertTrue(scene.scale(200f) < scene.scale(0f));
        assertEquals(700f, scene.worldX(scene.screenX(700f, 200f), 200f), 0.01f);
        assertTrue("o chão dele fica acima do chão dos lutadores", scene.groundScreenY(200f) < scene.groundScreenY(0f));
    }

    private static final class FakeImage implements RenderImage {
        final int w, h;

        FakeImage(int w, int h) {
            this.w = w;
            this.h = h;
        }

        @Override public int width() { return w; }
        @Override public int height() { return h; }
    }

    private static final class FakeAssets implements RenderAssets {
        final String json;

        FakeAssets(String json) {
            this.json = json;
        }

        @Override
        public String readText(String path) {
            assertEquals("stages/t/stage.json", path);
            return json;
        }

        @Override
        public RenderImage loadImage(String path) throws IOException {
            if (path.endsWith("falta.png")) throw new IOException("não existe");
            if (path.endsWith("lajotas.jpg")) return new FakeImage(512, 512);
            return path.endsWith("pilar.png") ? new FakeImage(700, 1400) : new FakeImage(1536, 1024);
        }
    }

    private static class CountingCanvas implements RenderCanvas {
        int depth, images, regions, lines, rings;
        final java.util.List<Object> ordem = new java.util.ArrayList<>();
        /** Recortes desenhados da imagem {@code watch}: topo e base no recorte, esquerda e direita na tela. */
        RenderImage watch;
        final java.util.List<float[]> watched = new java.util.ArrayList<>();

        @Override public void save() { depth++; }
        @Override public void restore() { depth--; assertTrue(depth >= 0); }
        @Override public void translate(float dx, float dy) {}
        @Override public void scale(float sx, float sy, float px, float py) {}
        @Override public void rotate(float degrees, float px, float py) {}
        @Override public void clipPolygon(float[] xy) {}
        @Override public void fillRect(float l, float t, float r, float b, int argb) {}
        @Override public void fillPolygon(float[] xy, int argb) { assertTrue(xy.length >= 6); }
        @Override public void strokePolygon(float[] xy, float width, int argb) { rings++; }
        @Override public void fillCircle(float cx, float cy, float radius, int argb) {}
        @Override public void strokeCircle(float cx, float cy, float radius, float width, int argb) {}
        @Override public void drawLine(float x1, float y1, float x2, float y2, float width, int argb) { lines++; }
        @Override public void drawImage(RenderImage image, float l, float t, float r, float b, int alpha) {
            images++;
            ordem.add(image);
        }
        @Override public void drawImageRegion(RenderImage image, float sl, float st, float sr, float sb,
                                              float l, float t, float r, float b, int alpha) {
            regions++;
            if (image == watch) watched.add(new float[] {st, sb, l, r});
            assertTrue("recorte dentro da imagem", sl >= -0.01f && sr <= image.width() + 0.01f && sr > sl);
            assertTrue(st >= -0.01f && sb <= image.height() + 0.01f && sb > st);
        }
        @Override public void fillRadialGlow(float cx, float cy, float radius, int argb) {}
        @Override public void fillVerticalGradient(float l, float t, float r, float b, int top, int bottom) {}
        @Override public void fillOval(float l, float t, float r, float b, int argb) {}
        @Override public void drawText(String text, float x, float y, float size, int fill, int stroke, float sw, boolean italic) {}
        @Override public void beginInvert() {}
        @Override public void endInvert() {}
    }
}
