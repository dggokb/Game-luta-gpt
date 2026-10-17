package com.gamelutagpt.stage;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;

/** Todo cenário em assets/stages lê sem erro e acha as imagens que cita. */
public class StagesDoJogoTest {
    private static final File STAGES = new File("../app/src/main/assets/stages");

    @Test
    public void todosOsCenariosLeemEAchamAsImagens() throws IOException {
        File[] pastas = STAGES.listFiles(File::isDirectory);
        assertTrue("sem cenários em " + STAGES.getAbsolutePath(), pastas != null && pastas.length > 0);
        for (File pasta : pastas) {
            File json = new File(pasta, "stage.json");
            assertTrue(pasta.getName() + " sem stage.json", json.isFile());
            StageDefinition def = StageDefinition.fromJson(
                new String(Files.readAllBytes(json.toPath()), StandardCharsets.UTF_8));
            for (StageDefinition.Layer layer : def.layers) {
                if (layer.image != null) existe(pasta, layer.image);
            }
            if (def.floor != null) {
                existe(pasta, def.floor.image);
                if (def.floor.plainImage != null) existe(pasta, def.floor.plainImage);
            }
        }
    }

    @Test
    public void dojoDaYinLuExiste() {
        assertTrue(new File(STAGES, "dojo_yinlu/stage.json").isFile());
    }

    private static void existe(File pasta, String imagem) {
        assertTrue(pasta.getName() + ": falta " + imagem, new File(pasta, imagem).isFile());
    }
}
