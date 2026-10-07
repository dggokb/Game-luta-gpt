package com.gamelutagpt.ultra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import java.io.IOException;
import java.util.Collections;
import org.junit.Test;

public class RaioFinalTest {
    private static final String JSON = "{\"nome\": \"X\", \"raio\": {\"corpo\": \"c.png\", \"ponta\": \"p.png\","
        + " \"inicio\": \"i.png\", \"centroInicio\": [0.44, 0.48], \"impacto\": \"falta.png\", \"hits\": 12,"
        + " \"espessura\": 120}}";

    @Test
    public void definitionReadsTheBeam() {
        UltraDefinition def = UltraDefinition.fromJson(JSON);
        assertEquals("c.png", def.beam.bodyPath);
        assertEquals(12, def.beam.hits);
        assertEquals(120f, def.beam.thickness, 0f);
        assertEquals(0.44f, def.beam.startCenter[0], 1e-6f);

        UltraDefinition plain = UltraDefinition.fromJson("{\"nome\": \"X\"}");
        assertNull(plain.beam.bodyPath);
        assertEquals(UltraDefinition.Beam.DEFAULT_HITS, plain.beam.hits);
    }

    @Test(expected = IllegalArgumentException.class)
    public void beamNeedsAtLeastOneHit() {
        UltraDefinition.fromJson("{\"nome\": \"X\", \"raio\": {\"hits\": 0}}");
    }

    @Test
    public void packLoadsBeamImagesAndWarnsAboutMissingOnes() throws IOException {
        UltraPack pack = UltraPack.load(new RenderAssets() {
            @Override public String readText(String path) { return JSON; }
            @Override public RenderImage loadImage(String path) throws IOException {
                if (path.endsWith("falta.png")) throw new IOException("não existe");
                return image(path.endsWith("c.png") ? 1024 : 1024, path.endsWith("c.png") ? 280 : 340);
            }
        }, "ultras/x");
        assertNotNull(pack.beamBody);
        assertNotNull(pack.beamTip);
        assertNotNull(pack.beamStart);
        assertNull(pack.beamImpact);
        assertEquals(1, pack.warnings.size());
    }

    /** Toda a vida do raio, com e sem imagens, para os dois lados, sem desbalancear o Canvas. */
    @Test
    public void drawsEveryMomentWithAndWithoutArt() {
        UltraDefinition def = UltraDefinition.fromJson(JSON);
        RenderImage[] art = {image(1024, 280), image(1024, 340), image(1024, 576), image(768, 768)};
        UltraPack withArt = new UltraPack(def, new RenderImage[UltraSlot.values().length], art,
            Collections.<String>emptyList());
        UltraPack noArt = UltraPack.placeholder("Y", 0xFF4ACDE8);
        RaioFinal raio = new RaioFinal();
        Canvas canvas = new Canvas();
        for (UltraPack pack : new UltraPack[]{withArt, noArt}) {
            for (int facing : new int[]{1, -1}) {
                for (int frame = 0; frame < 120; frame++) {
                    RaioFinal.Frame f = new RaioFinal.Frame();
                    f.originX = 600f;
                    f.originY = 410f;
                    f.targetX = 600f + facing * (frame < 40 ? 360f : 40f);
                    f.facing = facing;
                    f.time = frame / 60f;
                    f.reach = Math.min(1f, frame / 8f);
                    f.sinceHit = frame > 8 ? (frame % 4) / 60f : -1f;
                    f.hits = frame / 4;
                    f.sinceBlast = frame >= 90 ? (frame - 90) / 60f : -1f;
                    f.strength = frame >= 90 ? Math.max(0f, 1f - (frame - 90) / 18f) : 1f;
                    raio.draw(canvas, pack, f);
                    assertEquals(0, canvas.depth);
                }
            }
        }
        assertTrue(canvas.images > 0);
        assertTrue(canvas.regions > 0);
    }

    private static RenderImage image(final int w, final int h) {
        return new RenderImage() {
            @Override public int width() { return w; }
            @Override public int height() { return h; }
        };
    }

    private static final class Canvas implements RenderCanvas {
        int depth, images, regions;

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
        @Override public void drawImage(RenderImage image, float l, float t, float r, float b, int alpha) {
            images++;
            assertTrue(r > l && b > t);
        }
        @Override public void drawImageRegion(RenderImage image, float sl, float st, float sr, float sb,
                                              float l, float t, float r, float b, int alpha) {
            regions++;
            assertTrue("recorte dentro da imagem", sl >= -0.01f && sr <= image.width() + 0.01f && sr > sl);
            assertTrue(r > l);
        }
        @Override public void fillRadialGlow(float cx, float cy, float radius, int argb) {}
        @Override public void fillVerticalGradient(float l, float t, float r, float b, int top, int bottom) {}
        @Override public void fillOval(float l, float t, float r, float b, int argb) {}
        @Override public void drawText(String text, float x, float y, float size, int fill, int stroke, float sw, boolean italic) {}
        @Override public void beginInvert() {}
        @Override public void endInvert() {}
    }
}
