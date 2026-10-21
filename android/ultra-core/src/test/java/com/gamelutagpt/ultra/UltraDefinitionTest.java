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
        // Versão leve: só o p01.
        String[] fighters = {"player_base"};
        for (String id : fighters) {
            File json = new File(ultras(), id + "/" + UltraPack.DEFINITION_FILE);
            assertTrue(id + " sem ultra.json", json.isFile());
            UltraDefinition d = UltraDefinition.fromJson(new String(Files.readAllBytes(json.toPath()), StandardCharsets.UTF_8));
            assertFalse(id, d.name.trim().isEmpty());
            assertEquals(id + ": corte da abertura", "corte.png", d.opening.cutInPath);
            assertEquals(id + ": palco da abertura", "palco.jpg", d.opening.stagePath);
            assertNotNull(id + ": grito", d.opening.shout);
            assertTrue(id + ": página " + d.style.page, d.style.page != null && PaginaLayout.isKnown(d.style.page));
            assertTrue(id + ": abertura " + d.style.opening,
                d.style.opening != null && AberturaUltra.isKnownStyle(d.style.opening));
            assertNotNull(id + ": câmera", d.style.camera);
            assertTrue(id + ": impacto " + d.style.impact,
                "vazio".equals(d.style.impact) || "vermelho".equals(d.style.impact));
            assertTrue(id + ": câmera lenta", d.style.slowMotion > 0f && d.style.slowMotion <= 1f);
        }
    }

    @Test public void theStyleIsOptionalAndTheCameraCanBeAListOfShots() {
        UltraDefinition plain = UltraDefinition.fromJson("{\"nome\": \"Teste\"}");
        assertSame(UltraDefinition.Style.DEFAULT, plain.style);
        UltraDefinition d = UltraDefinition.fromJson("{\"nome\": \"Teste\", \"estilo\": {\"pagina\": \"leque\", "
            + "\"camera\": [\"baixo\", \"close\"], \"camaraLenta\": 0.25}}");
        assertEquals("leque", d.style.page);
        assertNull(d.style.camera);
        assertArrayEquals(new String[] {"baixo", "close"}, d.style.shots);
        assertEquals(0.25f, d.style.slowMotion, 0f);
        assertNull(d.style.opening);
    }

    @Test public void theOpeningIsOptional() {
        UltraDefinition d = UltraDefinition.fromJson("{\"nome\": \"Teste\"}");
        assertNull(d.opening.cutInPath);
        assertNull(d.opening.stagePath);
        assertNull(d.opening.shout);
        assertEquals("TESTE", d.name);
    }
}
