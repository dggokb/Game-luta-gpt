package com.gamelutagpt.ultra;

import static org.junit.Assert.*;

import org.junit.Test;

/** Modelos da página de mangá: todos aceitam as mesmas imagens e cabem na tela. */
public class PaginaLayoutTest {
    /** Formato (largura / altura) que o tools/ultra/preparar_ultra.py recorta para cada painel. */
    private static final float[] IMAGE_ASPECT = {5.6f, 1f, 3.2f, 3.6f};

    @Test public void everyLayoutKeepsThePanelShapesOfTheImages() {
        for (String name : PaginaLayout.NAMES) {
            PaginaLayout layout = PaginaLayout.byName(name);
            assertEquals(name, layout.name);
            assertEquals(5, layout.panels.length);
            for (int i = 0; i < 4; i++) {
                PaginaLayout.Frame f = PaginaLayout.frame(layout.panels[i], new PaginaLayout.Frame());
                float aspect = f.w / f.h;
                assertEquals(name + " painel " + i + " no formato da imagem", IMAGE_ASPECT[i], aspect,
                    IMAGE_ASPECT[i] * 0.22f);
                assertTrue(name + " painel " + i + " na tela", f.cx > 0f && f.cx < PaginaLayout.VW
                    && f.cy > 0f && f.cy < PaginaLayout.VH);
            }
            PaginaLayout.Frame full = PaginaLayout.frame(layout.panels[4], new PaginaLayout.Frame());
            assertTrue(name + ": o painel final cobre a tela", full.w > PaginaLayout.VW && full.h > PaginaLayout.VH);
        }
    }

    @Test public void tiltedPanelsCarryTheirAngle() {
        PaginaLayout.Frame f = PaginaLayout.frame(PaginaLayout.rect(400f, 300f, 600f, 150f, -8f), new PaginaLayout.Frame());
        assertEquals(-8f, f.angle, 0.01f);
        assertEquals(600f, f.w, 0.5f);
        assertEquals(150f, f.h, 0.5f);
        assertEquals(400f, f.cx, 0.5f);
        assertEquals(300f, f.cy, 0.5f);
    }

    @Test public void unknownNamesFallBackToTheClassicPage() {
        assertSame(PaginaLayout.CLASSICA_LAYOUT, PaginaLayout.byName(null));
        assertSame(PaginaLayout.CLASSICA_LAYOUT, PaginaLayout.byName("nada"));
        assertSame(PaginaLayout.LEQUE_LAYOUT, PaginaLayout.byName(" Leque "));
        assertTrue(PaginaLayout.isKnown("quebrada"));
        assertFalse(PaginaLayout.isKnown("nada"));
    }
}
