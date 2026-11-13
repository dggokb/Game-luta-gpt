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
                for (StageDefinition.Banner b : layer.banners) existe(pasta, b.image);
                for (StageDefinition.Crowd cr : layer.crowds) if (cr.mask != null) existe(pasta, cr.mask);
            }
            // O motor de luz e sombra: todo cenário declara a sua. O lado aceso perto da arte (de
            // noite perto de 0,85 de valor), a sombra com a cor do lugar sem apagar o lutador (de
            // noite perto de 0,5 a 0,6), os pés não mais escuros que isso e a borda de 0,35 a 0,6.
            StageDefinition.FighterLight luz = def.fighterLight;
            assertTrue(pasta.getName() + " sem luzLutadores", luz != null);
            float luzV = valor(luz.lit), sombraV = valor(luz.shade), chaoV = valor(luz.ground);
            assertTrue(pasta.getName() + " luz " + luzV, luzV >= 0.75f);
            assertTrue(pasta.getName() + " sombra " + sombraV, sombraV >= 0.45f && sombraV <= 0.95f);
            assertTrue(pasta.getName() + " chão " + chaoV, chaoV >= 0.4f && chaoV <= sombraV + 0.06f);
            assertTrue(pasta.getName() + " borda " + luz.edge, luz.edge >= 0.3f && luz.edge <= 0.65f);
            if (def.floor != null) {
                existe(pasta, def.floor.image);
                if (def.floor.plainImage != null) existe(pasta, def.floor.plainImage);
            }
        }
    }

    /** O valor (HSV) de uma cor 0xRRGGBB: o maior canal, de 0 a 1. */
    private static float valor(int cor) {
        return Math.max(((cor >> 16) & 0xFF), Math.max((cor >> 8) & 0xFF, cor & 0xFF)) / 255f;
    }

    @Test
    public void dojoDaYinLuExiste() {
        assertTrue(new File(STAGES, "dojo_yinlu/stage.json").isFile());
    }

    private static void existe(File pasta, String imagem) {
        assertTrue(pasta.getName() + ": falta " + imagem, new File(pasta, imagem).isFile());
    }
}
