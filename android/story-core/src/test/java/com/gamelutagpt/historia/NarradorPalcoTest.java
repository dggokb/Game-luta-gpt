package com.gamelutagpt.historia;

import static org.junit.Assert.*;

import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import org.junit.Test;

/** O narrador no palco: o lado que escolhe, o andar até lá e a pose de cada fala. */
public class NarradorPalcoTest {
    private static final float DT = 1f / 60f;

    private static void passa(NarradorPalco n, float segundos, String texto, String expressao, int prioridade) {
        for (int i = 0; i < Math.round(segundos * 60f); i++) n.update(DT, texto, expressao, prioridade, texto != null);
    }

    @Test public void heStandsOutsideThePairOnTheRoomierSide() {
        NarradorPalco n = new NarradorPalco();
        assertTrue("antes do primeiro alvo, não está em cena", Float.isNaN(n.x()));
        // Dupla à esquerda da tela: ele fica à direita, de fora, olhando para eles.
        n.alvo(100f, 200f, 0f, 1600f);
        assertEquals(1, n.lado());
        assertEquals(200f + NarradorPalco.AFASTA, n.x(), 0.01f);
        assertEquals(-1f, n.facing(), 0f);
        // A dupla vai para a direita e encosta na borda: o lado dele não cabe mais, ele troca
        // e corre por trás até o outro lado.
        n.alvo(1300f, 1450f, 0f, 1600f);
        assertEquals(-1, n.lado());
        passa(n, 0.3f, null, null, 0);
        assertTrue("longe: corre", n.correndo());
        assertEquals("correndo, olha para onde vai", 1f, n.facing(), 0f);
        passa(n, 4f, null, null, 0);
        assertEquals(1300f - NarradorPalco.AFASTA, n.x(), 3f);
        assertFalse(n.andando());
        assertEquals("parado, olha para a luta", 1f, n.facing(), 0f);
    }

    @Test public void smallMovesDoNotMakeHimSwitchSides() {
        NarradorPalco n = new NarradorPalco();
        n.alvo(600f, 800f, 0f, 1600f);
        int lado = n.lado();
        for (float d = 0f; d < 200f; d += 20f) {
            n.alvo(600f + d * lado, 800f + d * lado, 0f, 1600f);
            assertEquals("a dupla chegou um pouco perto, ele fica", lado, n.lado());
        }
    }

    @Test public void heFollowsTheFightWalkingAndStopsAtTheEdge() {
        NarradorPalco n = new NarradorPalco();
        n.alvo(300f, 500f, 0f, 1600f);
        float antes = n.x();
        n.alvo(400f, 600f, 0f, 1600f);
        passa(n, 0.2f, null, null, 0);
        assertTrue("anda (perto: sem correr)", n.andando() && !n.correndo());
        passa(n, 3f, null, null, 0);
        assertEquals(antes + 100f, n.x(), 3f);
        // Perto da borda da tela (ainda sem trocar de lado): para nela, não sai da tela.
        // (o lado dele com um pouco mais que o aperto: fica, mas não cabe na distância certa)
        float hi = 1600f - NarradorPalco.APERTADO - 10f;
        n.alvo(hi - 150f, hi, 0f, 1600f);
        passa(n, 6f, null, null, 0);
        assertEquals(1, n.lado());
        assertEquals(1600f - NarradorPalco.BORDA, n.x(), 3f);
    }

    @Test public void eachKindOfLineHasItsPose() {
        NarradorPalco n = new NarradorPalco();
        n.alvo(300f, 500f, 0f, 1600f);
        passa(n, 1f, null, null, 0);
        assertFalse("calado: microfone no peito", n.microfoneNaBoca());
        passa(n, 0.6f, "Que golpe!", "normal", 1);
        assertTrue(n.microfoneNaBoca());
        assertFalse(n.bracoLevantado() || n.apontando());
        passa(n, 0.6f, "A guarda quebrou!", "serio", 2);
        assertTrue("sério: aponta para a luta", n.apontando());
        // Fala grande nova: pula e levanta o braço.
        n.update(DT, "K.O.!", "empolgado", 3, true);
        passa(n, 0.1f, "K.O.!", "empolgado", 3);
        assertTrue("pulou", n.noAr());
        passa(n, 0.8f, "K.O.!", "empolgado", 3);
        assertFalse("caiu de volta no chão", n.noAr());
        assertTrue(n.bracoLevantado());
        passa(n, 1f, null, null, 0);
        assertFalse(n.microfoneNaBoca() || n.bracoLevantado() || n.apontando());
    }

    @Test public void drawingKeepsTheCanvasBalanced() {
        NarradorPalco n = new NarradorPalco();
        Contador c = new Contador();
        n.render(c, 640f, 560f, 1f);
        assertEquals("fora de cena: nada", 0, c.desenhos);
        n.alvo(300f, 500f, 0f, 1600f);
        String[][] falas = {{null, null}, {"a", "normal"}, {"b", "empolgado"}, {"c", "serio"}};
        for (String[] f : falas) {
            for (int i = 0; i < 40; i++) {
                n.update(DT, f[0], f[1], f[0] == null ? 0 : 3, true);
                n.render(c, 640f, 560f, 0.9f);
                assertEquals(0, c.nivel);
            }
        }
        assertTrue(c.desenhos > 0);
    }

    @Test public void limbsReachTheirTargetOrStretchToward() {
        float[] out = new float[4];
        NarradorPalco.ik(0f, 0f, 30f, 40f, 30f, 30f, 1f, out);
        assertEquals(30f, (float)Math.hypot(out[0], out[1]), 0.01f);
        assertEquals(30f, (float)Math.hypot(30f - out[0], 40f - out[1]), 0.01f);
        NarradorPalco.ik(0f, 0f, 300f, 0f, 30f, 30f, 1f, out);
        assertEquals("longe demais: estica", 60f, out[2], 0.05f);
    }

    private static final class Contador implements RenderCanvas {
        int nivel, desenhos;

        @Override public void save() { nivel++; }
        @Override public void restore() { nivel--; assertTrue(nivel >= 0); }
        @Override public void translate(float dx, float dy) {}
        @Override public void scale(float sx, float sy, float px, float py) { assertTrue(sx != 0f && sy != 0f); }
        @Override public void rotate(float degrees, float px, float py) {}
        @Override public void clipPolygon(float[] xy) {}
        @Override public void fillRect(float l, float t, float r, float b, int argb) { desenhos++; }
        @Override public void fillPolygon(float[] xy, int argb) { assertTrue(xy.length >= 6); desenhos++; }
        @Override public void strokePolygon(float[] xy, float width, int argb) { desenhos++; }
        @Override public void fillCircle(float cx, float cy, float radius, int argb) { assertFalse(Float.isNaN(cx)); desenhos++; }
        @Override public void strokeCircle(float cx, float cy, float radius, float width, int argb) { desenhos++; }
        @Override public void drawLine(float x1, float y1, float x2, float y2, float width, int argb) {
            assertFalse(Float.isNaN(x1 + y1 + x2 + y2));
            desenhos++;
        }
        @Override public void drawImage(RenderImage image, float l, float t, float r, float b, int alpha) {}
        @Override public void drawImageRegion(RenderImage image, float sl, float st, float sr, float sb,
                                              float l, float t, float r, float b, int alpha) {}
        @Override public void fillRadialGlow(float cx, float cy, float radius, int argb) { desenhos++; }
        @Override public void fillVerticalGradient(float l, float t, float r, float b, int top, int bottom) {}
        @Override public void fillOval(float l, float t, float r, float b, int argb) { desenhos++; }
        @Override public void drawText(String text, float x, float y, float size, int fill, int stroke, float sw, boolean italic) {}
        @Override public void beginInvert() {}
        @Override public void endInvert() {}
    }
}
