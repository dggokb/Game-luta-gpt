package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/** Câmera do Ultra em sequência: um plano por golpe, close antes do golpe final, luta no fim. */
public class UltraDirectorTest {
    @Test public void everyBlowCutsToAnotherShot() {
        int[] hits = {4, 24, 40, 60, 74, 114};
        assertEquals(UltraDirector.OPEN, UltraDirector.kind(hits, 0));
        assertEquals(UltraDirector.SIDE, UltraDirector.kind(hits, 4));
        assertEquals(UltraDirector.LOW, UltraDirector.kind(hits, 24));
        assertEquals(UltraDirector.WIDE, UltraDirector.kind(hits, 40));
        assertEquals(UltraDirector.SIDE, UltraDirector.kind(hits, 60));
        assertEquals("the wind-up before the final blow", UltraDirector.CLOSE, UltraDirector.kind(hits, 74));
        assertEquals(UltraDirector.CLOSE, UltraDirector.kind(hits, 113));
        assertEquals("the fight camera after the final blow", UltraDirector.FINAL, UltraDirector.kind(hits, 114));
    }

    @Test public void shotsLeanWithTheBlowAndPushInSlowly() {
        int[] hits = {4, 24, 40};
        UltraDirector.Shot right = new UltraDirector.Shot(), left = new UltraDirector.Shot();
        UltraDirector.shot(right, hits, 5, 500f, 300f, 640f, 310f, 1);
        UltraDirector.shot(left, hits, 5, 500f, 300f, 360f, 310f, -1);
        assertEquals(UltraDirector.SIDE, right.kind);
        assertEquals("mirrored for the other side", -right.tilt, left.tilt, 0f);
        assertEquals("framed on the contact", 570f, right.focusX, 0.01f);
        UltraDirector.Shot later = new UltraDirector.Shot();
        UltraDirector.shot(later, hits, 20, 500f, 300f, 640f, 310f, 1);
        assertTrue("slow push-in inside the shot", later.zoom > right.zoom);
        UltraDirector.Shot end = new UltraDirector.Shot();
        UltraDirector.shot(end, hits, 50, 500f, 300f, 640f, 310f, 1);
        assertEquals(1f, end.zoom, 0f);
        assertEquals(0f, end.tilt, 0f);
    }
}
