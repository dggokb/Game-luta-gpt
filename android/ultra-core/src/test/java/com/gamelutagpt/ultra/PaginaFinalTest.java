package com.gamelutagpt.ultra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class PaginaFinalTest {
    private static final float DT = 1f / 60f;

    private static final class Recorder implements UltraListener {
        final List<Integer> hits = new ArrayList<>();
        final List<String> sounds = new ArrayList<>();
        float damage;
        UltraGrade finished;

        @Override
        public void onUltraHit(int hitIndex, float damageFraction) {
            hits.add(hitIndex);
            damage += damageFraction;
        }

        @Override
        public void onUltraSound(String soundId) {
            sounds.add(soundId);
        }

        @Override
        public void onUltraFinished(UltraGrade grade) {
            finished = grade;
        }
    }

    /** Canvas que não desenha nada, só valida que o render não quebra. */
    private static final class NullCanvas implements UltraCanvas {
        int depth;
        int layers;

        @Override public void save() { depth++; }
        @Override public void restore() { depth--; assertTrue(depth >= 0); }
        @Override public void translate(float dx, float dy) {}
        @Override public void scale(float sx, float sy, float px, float py) {}
        @Override public void rotate(float degrees, float px, float py) {}
        @Override public void clipPolygon(float[] xy) { assertTrue(xy.length >= 6); }
        @Override public void fillRect(float l, float t, float r, float b, int argb) {}
        @Override public void fillPolygon(float[] xy, int argb) {}
        @Override public void strokePolygon(float[] xy, float width, int argb) {}
        @Override public void fillCircle(float cx, float cy, float radius, int argb) {}
        @Override public void strokeCircle(float cx, float cy, float radius, float width, int argb) {}
        @Override public void drawLine(float x1, float y1, float x2, float y2, float width, int argb) {}
        @Override public void drawImage(UltraImage image, float l, float t, float r, float b, int alpha) {}
        @Override public void drawText(String text, float x, float y, float size, int fill, int stroke, float sw, boolean italic) {}
        @Override public void beginInvert() { layers++; }
        @Override public void endInvert() { layers--; }
    }

    private static Recorder run(Float tapAt) {
        Recorder recorder = new Recorder();
        PaginaFinal pagina = new PaginaFinal(recorder);
        NullCanvas canvas = new NullCanvas();
        pagina.start(UltraPack.placeholder("TESTE", 0xFFF4B73B), 1);
        boolean tapped = false;
        for (int i = 0; i < 1000 && pagina.isActive(); i++) {
            pagina.update(DT);
            if (!tapped && tapAt != null && pagina.clock() >= tapAt) {
                pagina.tap();
                tapped = true;
            }
            pagina.render(canvas);
            assertEquals(0, canvas.depth);
            assertEquals(0, canvas.layers);
        }
        assertFalse("a cinemática precisa terminar", pagina.isActive());
        return recorder;
    }

    @Test
    public void perfectTapGivesFullBonus() {
        Recorder r = run(PaginaFinal.TARGET_AT);
        assertEquals(UltraGrade.PERFEITO, r.finished);
        assertEquals(3, r.hits.size());
        assertEquals(1f + 0.5f * UltraGrade.PERFEITO.finalBonus, r.damage, 1e-4f);
    }

    @Test
    public void lateButInsideWindowIsGood() {
        Recorder r = run(PaginaFinal.TARGET_AT + 0.12f);
        assertEquals(UltraGrade.BOM, r.finished);
        assertEquals(1f + 0.5f * UltraGrade.BOM.finalBonus, r.damage, 1e-4f);
    }

    @Test
    public void earlyTapLosesBonusButStillDetonates() {
        Recorder r = run(PaginaFinal.TARGET_AT - 0.5f);
        assertEquals(UltraGrade.ERROU, r.finished);
        assertEquals(1f, r.damage, 1e-4f);
    }

    @Test
    public void noTapStillFinishes() {
        Recorder r = run(null);
        assertEquals(UltraGrade.NENHUM, r.finished);
        assertEquals(1f, r.damage, 1e-4f);
        assertTrue(r.sounds.contains("quebra"));
    }

    @Test
    public void hitsArriveInOrder() {
        Recorder r = run(PaginaFinal.TARGET_AT);
        assertEquals(0, (int)r.hits.get(0));
        assertEquals(1, (int)r.hits.get(1));
        assertEquals(2, (int)r.hits.get(2));
    }

    @Test
    public void tapBeforeRingIsIgnored() {
        Recorder recorder = new Recorder();
        PaginaFinal pagina = new PaginaFinal(recorder);
        pagina.start(UltraPack.placeholder("TESTE", 0xFFF4B73B), -1);
        pagina.update(0.5f);
        pagina.tap();
        assertEquals(UltraGrade.NENHUM, pagina.grade());
    }

    @Test
    public void definitionReadsJsonWithDefaults() {
        UltraDefinition def = UltraDefinition.fromJson(
            "{\"nome\": \"Explosão Solar\", \"cor\": \"#102030\","
                + " \"paineis\": {\"golpe\": {\"imagem\": \"golpe.png\", \"onomatopeia\": \"POW\","
                + " \"posicaoOnomatopeia\": [0.25, 1.5]}}}"
        );
        assertEquals("EXPLOSÃO SOLAR", def.name);
        assertEquals(0xFF102030, def.color);
        assertEquals("golpe.png", def.panel(UltraSlot.GOLPE).imagePath);
        assertEquals("POW", def.panel(UltraSlot.GOLPE).onomatopoeia);
        assertEquals(0.25f, def.panel(UltraSlot.GOLPE).onomatopoeiaPosition[0], 1e-6f);
        assertEquals(1f, def.panel(UltraSlot.GOLPE).onomatopoeiaPosition[1], 1e-6f);
        assertNull(def.panel(UltraSlot.FINAL).onomatopoeiaPosition);
        assertNull(def.panel(UltraSlot.FINAL).imagePath);
        assertEquals(UltraSlot.FINAL.defaultOnomatopoeia, def.panel(UltraSlot.FINAL).onomatopoeia);
    }

    @Test(expected = IllegalArgumentException.class)
    public void definitionRejectsBadPosition() {
        UltraDefinition.fromJson("{\"nome\": \"X\", \"paineis\": {\"golpe\": {\"posicaoOnomatopeia\": [0.5]}}}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void definitionRequiresName() {
        UltraDefinition.fromJson("{\"cor\": \"#FFFFFF\"}");
    }

    @Test
    public void miniJsonParsesNestedValues() {
        Object value = MiniJson.parse("{\"a\": [1, 2.5, true, null, \"x\\u00e9\"]}");
        assertNotNull(value);
        assertEquals("{a=[1.0, 2.5, true, null, xé]}", value.toString());
    }

    @Test
    public void packLoadFallsBackWhenImageMissing() throws Exception {
        UltraAssets assets = new UltraAssets() {
            @Override
            public String readText(String path) {
                assertEquals("ultras/p1/ultra.json", path);
                return "{\"nome\": \"X\", \"paineis\": {\"olhos\": {\"imagem\": \"olhos.png\"}}}";
            }

            @Override
            public UltraImage loadImage(String path) throws java.io.IOException {
                throw new java.io.IOException("não existe");
            }
        };
        UltraPack pack = UltraPack.load(assets, "ultras/p1");
        assertNull(pack.image(UltraSlot.OLHOS));
        assertEquals(1, pack.warnings.size());
    }
}
