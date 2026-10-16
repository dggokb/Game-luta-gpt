package com.gamelutagpt;

import static org.junit.Assert.*;

import com.gamelutagpt.ultra.UltraDefinition;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

    @Test public void eachUltraPicksItsOwnCameraScript() {
        int[] hits = {4, 24, 40, 60, 74, 114};
        UltraDirector.Script heavy = UltraDirector.script("pesada", null);
        assertSame(UltraDirector.PESADA, heavy);
        assertEquals(UltraDirector.LOW, UltraDirector.kind(hits, 4, heavy));
        assertEquals("still the wind-up close before the final blow", UltraDirector.CLOSE,
            UltraDirector.kind(hits, 74, heavy));
        UltraDirector.Script custom = UltraDirector.script("pesada", new String[] {"aberto", "xyz", "baixo"});
        assertEquals("a written list wins over the preset", UltraDirector.WIDE, UltraDirector.kind(hits, 4, custom));
        assertEquals("unknown shot names are skipped", UltraDirector.LOW, UltraDirector.kind(hits, 24, custom));
        assertSame(UltraDirector.PADRAO, UltraDirector.script("nada", null));

        // Ágil: a câmera troca o lado da inclinação a cada corte.
        UltraDirector.Shot a = new UltraDirector.Shot(), b = new UltraDirector.Shot();
        UltraDirector.shot(a, hits, 4, 500f, 300f, 640f, 310f, 1, UltraDirector.AGIL);
        UltraDirector.shot(b, hits, 24, 500f, 300f, 640f, 310f, 1, UltraDirector.AGIL);
        assertEquals(UltraDirector.WIDE, a.kind);
        assertEquals(UltraDirector.SIDE, b.kind);
        assertEquals(UltraDirector.TILT[UltraDirector.WIDE] * UltraDirector.AGIL.tiltScale, a.tilt, 0.001f);
        assertEquals("the next cut leans the other way", -UltraDirector.TILT[UltraDirector.SIDE] * UltraDirector.AGIL.tiltScale,
            b.tilt, 0.001f);
    }

    @Test public void everyUltraJsonUsesAKnownCameraScript() throws Exception {
        File dir = null;
        for (String path : new String[] {"src/main/assets/ultras", "app/src/main/assets/ultras",
            "android/app/src/main/assets/ultras"}) {
            if (new File(path).isDirectory()) dir = new File(path);
        }
        assertNotNull("pasta de ultras", dir);
        int checked = 0;
        for (File folder : dir.listFiles()) {
            File json = new File(folder, "ultra.json");
            if (!json.isFile()) continue;
            UltraDefinition d = UltraDefinition.fromJson(new String(Files.readAllBytes(json.toPath()), StandardCharsets.UTF_8));
            assertTrue(folder.getName() + ": câmera " + d.style.camera, UltraDirector.isKnownPreset(d.style.camera));
            if (d.style.shots != null) {
                for (String shot : d.style.shots) assertTrue(folder.getName() + ": " + shot, UltraDirector.shotKind(shot) >= 0);
            }
            checked++;
        }
        assertTrue(checked >= 12);
    }
}
