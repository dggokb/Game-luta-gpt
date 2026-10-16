package com.gamelutagpt.ultra;

import static org.junit.Assert.*;

import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** Abertura do Ultra: dura o tempo certo, mostra o nome e o grito, e funciona sem imagens. */
public class AberturaUltraTest {
    private static final float DT = 1f / 60f;

    /** Guarda os textos desenhados e confere que save/restore fecham. */
    private static final class TextCanvas implements RenderCanvas {
        final List<String> texts = new ArrayList<>();
        int depth;
        @Override public void save() { depth++; }
        @Override public void restore() { depth--; assertTrue(depth >= 0); }
        @Override public void translate(float dx, float dy) {}
        @Override public void scale(float sx, float sy, float px, float py) {}
        @Override public void rotate(float degrees, float px, float py) {}
        @Override public void clipPolygon(float[] xy) {}
        @Override public void fillRect(float l, float t, float r, float b, int argb) {}
        @Override public void fillPolygon(float[] xy, int argb) { assertTrue(xy.length >= 6 && xy.length % 2 == 0); }
        @Override public void strokePolygon(float[] xy, float width, int argb) {}
        @Override public void fillCircle(float cx, float cy, float radius, int argb) {}
        @Override public void strokeCircle(float cx, float cy, float radius, float width, int argb) {}
        @Override public void drawLine(float x1, float y1, float x2, float y2, float width, int argb) {}
        @Override public void drawImage(RenderImage image, float l, float t, float r, float b, int alpha) {}
        @Override public void drawImageRegion(RenderImage image, float sl, float st, float sr, float sb,
            float l, float t, float r, float b, int alpha) {}
        @Override public void fillRadialGlow(float cx, float cy, float radius, int argb) {}
        @Override public void fillVerticalGradient(float l, float t, float r, float b, int top, int bottom) {}
        @Override public void fillOval(float l, float t, float r, float b, int argb) {}
        @Override public void drawText(String text, float x, float y, float size, int fill, int stroke, float sw, boolean italic) {
            texts.add(text);
        }
        @Override public void beginInvert() {}
        @Override public void endInvert() {}
    }

    @Test public void theOpeningShowsTheNameAndTheShoutThenEnds() {
        UltraDefinition d = UltraDefinition.fromJson(
            "{\"nome\": \"Dança da Garça\", \"cor\": \"#3FA7D6\", \"abertura\": {\"grito\": \"Você não vai me alcançar!\"}}");
        AberturaUltra abertura = new AberturaUltra();
        assertFalse(abertura.isActive());
        for (int facing : new int[] {1, -1}) {
            abertura.start(new UltraPack(d, new RenderImage[UltraSlot.values().length], new RenderImage[7], java.util.Collections.<String>emptyList()), facing);
            TextCanvas canvas = new TextCanvas();
            int frames = 0;
            while (abertura.isActive() && frames < 600) {
                abertura.render(canvas);
                abertura.update(DT);
                frames++;
            }
            assertEquals("the fight waits about one second", Math.round(AberturaUltra.DURATION * 60), frames, 2);
            assertTrue(canvas.texts.contains("DANÇA DA GARÇA"));
            assertTrue(canvas.texts.contains("ULTRA"));
            assertTrue(canvas.texts.contains("Você não vai me alcançar!"));
            assertEquals(0, canvas.depth);
        }
    }

    @Test public void everyOpeningStyleShowsTheNameWithOrWithoutArt() {
        for (String style : AberturaUltra.STYLES) {
            UltraDefinition d = UltraDefinition.fromJson("{\"nome\": \"Sinfonia Escarlate\", \"estilo\": {\"abertura\": \""
                + style + "\"}, \"abertura\": {\"grito\": \"Dance comigo.\"}}");
            for (RenderImage art : new RenderImage[] {null, new FakeImage(400, 600)}) {
                AberturaUltra abertura = new AberturaUltra();
                abertura.start(new UltraPack(d, new RenderImage[UltraSlot.values().length], new RenderImage[7],
                    art, art, java.util.Collections.<String>emptyList()), -1, art);
                assertEquals(style, abertura.style());
                TextCanvas canvas = new TextCanvas();
                while (abertura.isActive()) {
                    abertura.render(canvas);
                    abertura.update(DT);
                }
                assertEquals(style, 0, canvas.depth);
                assertTrue(style, canvas.texts.contains("ULTRA"));
                assertTrue(style, canvas.texts.contains("Dance comigo."));
                if (AberturaUltra.VERTICAL.equals(style)) {
                    // Uma letra por vez, de cima para baixo: SINFONIA numa coluna, ESCARLATE na outra.
                    assertEquals(style, "SINFONIAESCARLATE", String.join("", canvas.texts.subList(
                        canvas.texts.size() - 18, canvas.texts.size() - 1)));
                } else {
                    assertTrue(style, canvas.texts.contains("SINFONIA ESCARLATE"));
                }
            }
        }
        assertEquals("unknown styles fall back to the band", AberturaUltra.FAIXA, AberturaUltra.styleOf("xyz"));
        assertTrue(AberturaUltra.isKnownStyle("Racha"));
        assertFalse(AberturaUltra.isKnownStyle("xyz"));
    }

    private static final class FakeImage implements RenderImage {
        final int w, h;
        FakeImage(int w, int h) { this.w = w; this.h = h; }
        @Override public int width() { return w; }
        @Override public int height() { return h; }
    }
}
