package com.gamelutagpt.stage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** Atores de fundo: parados em loop, reações que tocam uma vez e voltam, e sem arte também. */
public class StageActorTest {
    private static final StageWorld WORLD = new StageWorld(565f, 2600f, 1.12f, 700f, 552f);
    private static final float DT = 1f / 60f;

    private static final String ATOR = "{\"nome\": \"Mestre\", \"parado\": \"parado\", \"animacoes\": {"
        + "\"parado\": {\"folha\": \"parado.png\", \"quadros\": 8, \"colunas\": 4, \"fps\": 8, \"loop\": true},"
        + "\"aplaudir\": {\"folha\": \"aplaudir.png\", \"quadros\": 6, \"colunas\": 3, \"fps\": 12},"
        + "\"rir\": {\"segundos\": 0.5}},"
        + "\"reacoes\": {\"licao\": [\"aplaudir\"], \"levou\": \"rir\"}}";

    private static final String STAGE = "{\"nome\": \"T\", \"camadas\": ["
        + "{\"id\": \"mestre\", \"tipo\": \"ator\", \"pasta\": \"atores/mestre\", \"profundidade\": 800,"
        + " \"noChao\": true, \"altura\": 0.4, \"x\": 0.3, \"virar\": true},"
        + "{\"id\": \"sumido\", \"tipo\": \"ator\", \"pasta\": \"atores/sumido\", \"profundidade\": 800,"
        + " \"noChao\": true, \"altura\": 0.4, \"x\": 0.7}]}";

    @Test
    public void reacaoTocaUmaVezEVoltaAoParado() throws IOException {
        StagePack pack = StagePack.load(new Assets(), "stages/t");
        assertEquals("o ator sem ator.json vira aviso", 1, pack.warnings.size());
        StageScene scene = new StageScene(pack, WORLD);
        assertEquals(1, scene.actorIds().size());
        assertEquals("parado", scene.actorAnimation("mestre"));
        assertNull(scene.actorAnimation("sumido"));

        scene.react("licao");
        assertEquals("a reação espera um pouquinho", "parado", scene.actorAnimation("mestre"));
        run(scene, 0.4f);
        assertEquals("aplaudir", scene.actorAnimation("mestre"));
        run(scene, 0.6f);
        assertEquals("6 quadros a 12 fps = meio segundo, e volta", "parado", scene.actorAnimation("mestre"));

        // Sem folha: movimento provisório pelo tempo de "segundos".
        assertTrue(scene.actorPlay("mestre", "rir"));
        Canvas canvas = new Canvas();
        for (int i = 0; i < 20; i++) {
            scene.update(DT);
            scene.drawBackground(canvas);
        }
        assertEquals("rir", scene.actorAnimation("mestre"));
        assertTrue("desenha a parada no lugar da arte que falta", canvas.sheets.contains("parado"));
        run(scene, 0.3f);
        assertEquals("parado", scene.actorAnimation("mestre"));

        assertFalse(scene.actorPlay("mestre", "dancar"));
        assertFalse(scene.actorPlay("ninguem", "rir"));
        scene.react("evento-sem-reacao");
        run(scene, 0.5f);
        assertEquals("parado", scene.actorAnimation("mestre"));
    }

    @Test
    public void quadrosSaemDentroDaFolhaEmQualquerCamera() throws IOException {
        StageScene scene = new StageScene(StagePack.load(new Assets(), "stages/t"), WORLD);
        Canvas canvas = new Canvas();
        float[][] cameras = {{1.12f, 140f, 72f}, {0.78f, 0f, -100f}, {1.46f, 900f, -500f}};
        for (float[] cam : cameras) {
            scene.actorPlay("mestre", "aplaudir");
            for (int i = 0; i < 90; i++) {
                scene.setCamera(cam[0], cam[1], cam[2]);
                scene.update(DT);
                scene.drawBackground(canvas);
                scene.drawForeground(canvas);
                assertEquals(0, canvas.depth);
            }
        }
        assertTrue(canvas.regions > 100);
    }

    @Test
    public void atorJsonInvalidoAvisa() {
        String[] ruins = {
            "{\"animacoes\": {\"rir\": {\"folha\": \"r.png\"}}}",
            "{\"animacoes\": {\"parado\": {\"segundos\": 1}}}",
            "{\"animacoes\": {\"parado\": {\"folha\": \"p.png\"}}, \"reacoes\": {\"licao\": [\"dancar\"]}}",
            "{\"animacoes\": {\"parado\": {\"folha\": \"p.png\", \"fps\": 0}}}",
        };
        for (String json : ruins) {
            try {
                StageActor.fromJson(json);
                fail(json);
            } catch (IllegalArgumentException expected) {}
        }
        try {
            StageDefinition.fromJson("{\"nome\": \"T\", \"camadas\": [{\"tipo\": \"ator\", \"profundidade\": 1,"
                + " \"noChao\": true, \"altura\": 0.3}]}");
            fail("ator sem pasta");
        } catch (IllegalArgumentException expected) {}
    }

    /** Os atores do dojo abrem e as reações do tutorial existem nos dois. */
    @Test
    public void atoresDoDojo() throws IOException {
        File dojo = new File("../app/src/main/assets/stages/dojo_shinshinkan");
        StageDefinition def = StageDefinition.fromJson(read(new File(dojo, "stage.json")));
        int count = 0;
        for (StageDefinition.Layer layer : def.layers) {
            if (layer.kind != StageDefinition.Kind.ACTOR) continue;
            count++;
            StageActor actor = StageActor.fromJson(read(new File(dojo, layer.actor + "/ator.json")));
            for (String event : new String[] {"licao", "impacto", "levou", "fim"}) {
                assertTrue(actor.name + " sem reação para " + event, actor.reactions.containsKey(event));
            }
            for (StageActor.Animation a : actor.animations.values()) {
                if (a.sheet != null) {
                    assertTrue(actor.name + ": folha não existe: " + a.sheet, new File(dojo, layer.actor + "/" + a.sheet).isFile());
                }
            }
        }
        assertEquals("Dafu e Takatoshi", 2, count);
    }

    private static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    private static void run(StageScene scene, float seconds) {
        for (float t = 0f; t < seconds; t += DT) scene.update(DT);
    }

    private static final class Img implements RenderImage {
        final String name;
        final int w, h;
        Img(String name, int w, int h) { this.name = name; this.w = w; this.h = h; }
        @Override public int width() { return w; }
        @Override public int height() { return h; }
    }

    private static final class Assets implements RenderAssets {
        @Override public String readText(String path) throws IOException {
            if (path.equals("stages/t/stage.json")) return STAGE;
            if (path.equals("stages/t/atores/mestre/ator.json")) return ATOR;
            throw new IOException(path);
        }
        @Override public RenderImage loadImage(String path) throws IOException {
            if (path.endsWith("parado.png")) return new Img("parado", 4 * 200, 2 * 400);
            if (path.endsWith("aplaudir.png")) return new Img("aplaudir", 3 * 200, 2 * 400);
            throw new IOException(path);
        }
    }

    private static final class Canvas implements RenderCanvas {
        int depth, regions;
        final List<String> sheets = new ArrayList<>();
        @Override public void save() { depth++; }
        @Override public void restore() { depth--; assertTrue(depth >= 0); }
        @Override public void translate(float dx, float dy) {}
        @Override public void scale(float sx, float sy, float px, float py) {}
        @Override public void rotate(float degrees, float px, float py) {}
        @Override public void clipPolygon(float[] xy) {}
        @Override public void fillRect(float l, float t, float r, float b, int argb) {}
        @Override public void fillPolygon(float[] xy, int argb) {}
        @Override public void strokePolygon(float[] xy, float width, int argb) {}
        @Override public void fillCircle(float cx, float cy, float radius, int argb) {}
        @Override public void strokeCircle(float cx, float cy, float radius, float width, int argb) {}
        @Override public void drawLine(float x1, float y1, float x2, float y2, float width, int argb) {}
        @Override public void drawImage(RenderImage image, float l, float t, float r, float b, int alpha) {}
        @Override public void drawImageRegion(RenderImage image, float sl, float st, float sr, float sb,
                                              float l, float t, float r, float b, int alpha) {
            regions++;
            sheets.add(((Img)image).name);
            assertTrue("recorte dentro da folha", sl >= -0.01f && sr <= image.width() + 0.01f && sr > sl);
            assertTrue(st >= -0.01f && sb <= image.height() + 0.01f && sb > st);
            assertEquals("célula da folha", 200f, sr - sl, 0.01f);
        }
        @Override public void fillRadialGlow(float cx, float cy, float radius, int argb) {}
        @Override public void fillVerticalGradient(float l, float t, float r, float b, int top, int bottom) {}
        @Override public void fillOval(float l, float t, float r, float b, int argb) {}
        @Override public void drawText(String text, float x, float y, float size, int fill, int stroke, float sw, boolean italic) {}
        @Override public void beginInvert() {}
        @Override public void endInvert() {}
    }
}
