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
    public void sceneDrawsWithAndWithoutImagesAndKeepsTheCanvasBalanced() throws IOException {
        String json = "{\"nome\": \"T\", \"camadas\": ["
            + "{\"imagem\": \"ceu.jpg\", \"profundidade\": 100000, \"topo\": -0.1, \"base\": 0.6, \"repetir\": \"espelhar\","
            + " \"corAcima\": \"#000000\"},"
            + "{\"tipo\": \"neblina\", \"profundidade\": 2000, \"topo\": 0.3, \"base\": 0.5, \"cor\": \"#334455\"},"
            + "{\"tipo\": \"agua\", \"perto\": 1400, \"longe\": 4000},"
            + "{\"imagem\": \"pilar.png\", \"profundidade\": 300, \"noChao\": true, \"altura\": 1, \"repetir\": \"espelhar\","
            + " \"periodo\": 0.8, \"reflexo\": 0.3, \"luzes\": [{\"x\": 0.9, \"y\": 0.3}], \"fogos\": [{\"x\": 0.5}]},"
            + "{\"imagem\": \"frente.png\", \"profundidade\": -200, \"topo\": 0.8, \"base\": 1.1},"
            + "{\"imagem\": \"falta.png\", \"profundidade\": 50, \"topo\": 0.1, \"base\": 0.2}],"
            + "\"chao\": {\"imagem\": \"chao.jpg\", \"reflexoLutadores\": 0.2},"
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
            return path.endsWith("pilar.png") ? new FakeImage(700, 1400) : new FakeImage(1536, 1024);
        }
    }

    private static final class CountingCanvas implements RenderCanvas {
        int depth, images, regions;

        @Override public void save() { depth++; }
        @Override public void restore() { depth--; assertTrue(depth >= 0); }
        @Override public void translate(float dx, float dy) {}
        @Override public void scale(float sx, float sy, float px, float py) {}
        @Override public void rotate(float degrees, float px, float py) {}
        @Override public void clipPolygon(float[] xy) {}
        @Override public void fillRect(float l, float t, float r, float b, int argb) {}
        @Override public void fillPolygon(float[] xy, int argb) { assertTrue(xy.length >= 6); }
        @Override public void strokePolygon(float[] xy, float width, int argb) {}
        @Override public void fillCircle(float cx, float cy, float radius, int argb) {}
        @Override public void strokeCircle(float cx, float cy, float radius, float width, int argb) {}
        @Override public void drawLine(float x1, float y1, float x2, float y2, float width, int argb) {}
        @Override public void drawImage(RenderImage image, float l, float t, float r, float b, int alpha) { images++; }
        @Override public void drawImageRegion(RenderImage image, float sl, float st, float sr, float sb,
                                              float l, float t, float r, float b, int alpha) {
            regions++;
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
