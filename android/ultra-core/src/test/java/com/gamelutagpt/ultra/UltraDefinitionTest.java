package com.gamelutagpt.ultra;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

/** Todo Ultra do jogo tem um ultra.json válido, com nome (fácil de trocar) e abertura. */
public class UltraDefinitionTest {
    private static File ultras() {
        for (String path : new String[] {"../app/src/main/assets/ultras", "android/app/src/main/assets/ultras",
            "app/src/main/assets/ultras"}) {
            File dir = new File(path);
            if (dir.isDirectory()) return dir;
        }
        String repo = System.getProperty("gameluta.repo");
        if (repo != null) return new File(repo, "android/app/src/main/assets/ultras");
        throw new AssertionError("pasta de ultras não encontrada");
    }

    @Test public void everyFighterHasANamedCinematicUltra() throws Exception {
        String[] fighters = {"player_base", "p02", "p03", "p04", "p05", "p06", "p07", "p08", "p09", "p10", "p11", "p12"};
        for (String id : fighters) {
            File json = new File(ultras(), id + "/" + UltraPack.DEFINITION_FILE);
            assertTrue(id + " sem ultra.json", json.isFile());
            UltraDefinition d = UltraDefinition.fromJson(new String(Files.readAllBytes(json.toPath()), StandardCharsets.UTF_8));
            assertFalse(id, d.name.trim().isEmpty());
            assertEquals(id + ": corte da abertura", "corte.png", d.opening.cutInPath);
            assertEquals(id + ": palco da abertura", "palco.jpg", d.opening.stagePath);
            assertNotNull(id + ": grito", d.opening.shout);
        }
    }

    @Test public void theOpeningIsOptional() {
        UltraDefinition d = UltraDefinition.fromJson("{\"nome\": \"Teste\"}");
        assertNull(d.opening.cutInPath);
        assertNull(d.opening.stagePath);
        assertNull(d.opening.shout);
        assertEquals("TESTE", d.name);
    }
}
