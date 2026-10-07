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
        + " \"espessura\": 120, \"aura\": {\"imagem\": \"a.png\", \"quadros\": 4},"
        + " \"vento\": {\"imagem\": \"v.png\", \"quadros\": 4}, \"poeira\": {\"imagem\": \"p.png\", \"quadros\": 4},"
        + " \"maos\": {\"carga\": [-26, 124], \"disparo\": [112, 152]}}}";

    @Test
    public void definitionReadsTheBeam() {
        UltraDefinition def = UltraDefinition.fromJson(JSON);
        assertEquals("c.png", def.beam.bodyPath);
        assertEquals(12, def.beam.hits);
        assertEquals(120f, def.beam.thickness, 0f);
        assertEquals(0.44f, def.beam.startCenter[0], 1e-6f);
        assertEquals("a.png", def.beam.aura.path);
        assertEquals(4, def.beam.wind.frames);
        assertEquals(-26f, def.beam.chargeHands[0], 0f);
        assertEquals(152f, def.beam.fireHands[1], 0f);

        UltraDefinition plain = UltraDefinition.fromJson("{\"nome\": \"X\"}");
        assertNull(plain.beam.bodyPath);
        assertNull(plain.beam.aura);
        assertNull(plain.beam.fireHands);
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
        assertNotNull(pack.beamAura);
        assertNotNull(pack.beamDust);
        assertEquals(1, pack.warnings.size());
    }

    /** Toda a vida do raio, com e sem imagens, para os dois lados, sem desbalancear o Canvas. */
    @Test
    public void drawsEveryMomentWithAndWithoutArt() {
        UltraDefinition def = UltraDefinition.fromJson(JSON);
        RenderImage[] art = {image(1024, 280), image(1024, 340), image(1024, 576), image(768, 768),
            image(1460, 512), image(1980, 256), image(2720, 384)};
        UltraPack withArt = new UltraPack(def, new RenderImage[UltraSlot.values().length], art,
            Collections.<String>emptyList());
        UltraPack noArt = UltraPack.placeholder("Y", 0xFF4ACDE8);
        RaioFinal raio = new RaioFinal();
        Canvas canvas = new Canvas();
        for (UltraPack pack : new UltraPack[]{withArt, noArt}) {
            for (int facing : new int[]{1, -1}) {
                for (int frame = 0; frame < 160; frame++) {
                    RaioFinal.Frame f = new RaioFinal.Frame();
                    int fired = frame - 36;
                    f.bodyX = 500f;
                    f.groundY = 565f;
                    f.chargeX = 500f + facing * 20f;
                    f.chargeY = 440f;
                    f.originX = 500f + facing * 110f;
                    f.originY = 410f;
                    f.targetX = 500f + facing * (frame < 80 ? 520f : 60f);
                    f.facing = facing;
                    f.time = frame / 60f;
                    f.charge = Math.min(1f, frame / 36f);
                    f.sinceFire = fired >= 0 ? fired / 60f : -1f;
                    f.reach = Math.max(0f, Math.min(1f, fired / 8f));
                    f.sinceHit = fired > 8 ? (fired % 4) / 60f : -1f;
                    f.hits = Math.max(0, fired / 4);
                    f.sinceBlast = fired >= 90 ? (fired - 90) / 60f : -1f;
                    f.strength = fired >= 90 ? Math.max(0f, 1f - (fired - 90) / 18f) : 1f;
                    raio.drawBehind(canvas, pack, f);
                    assertEquals(0, canvas.depth);
                    raio.drawFront(canvas, pack, f);
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

    @Test
    public void poseGoesFromChargeToHoldToGuard() {
        int fire = 36, blast = 126, fade = 18;
        assertEquals(0, RaioFinal.poseFrame(0, fire, blast, fade));
        int charge = RaioFinal.poseFrame(fire - 1, fire, blast, fade);
        assertTrue(charge == 1 || charge == 2);
        assertEquals(3, RaioFinal.poseFrame(fire, fire, blast, fade));
        for (int frame = fire + 6; frame < blast; frame++) {
            int pose = RaioFinal.poseFrame(frame, fire, blast, fade);
            assertTrue("sustentação", pose >= 4 && pose <= 6);
        }
        assertEquals(7, RaioFinal.poseFrame(blast, fire, blast, fade));
        assertEquals(8, RaioFinal.poseFrame(blast + fade - 1, fire, blast, fade));
    }
}
