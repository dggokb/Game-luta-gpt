package com.gamelutagpt.historia;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class NivelCpuTest {
    private static final File ASSETS = new File("../app/src/main/assets");

    private static final class GameAssets implements RenderAssets {
        @Override public String readText(String path) throws IOException {
            return new String(Files.readAllBytes(new File(ASSETS, path).toPath()), StandardCharsets.UTF_8);
        }
        @Override public RenderImage loadImage(String path) throws IOException { throw new IOException("sem " + path); }
    }

    @Test
    public void sobeDaBaseAteO8NaUltimaLuta() {
        assertEquals(5, NivelCpu.nivel(5, 0, 7));
        assertEquals(8, NivelCpu.nivel(5, 6, 7));
        assertEquals("uma luta só: fica na base", 5, NivelCpu.nivel(5, 0, 1));
        assertEquals("base 8: sempre 8", 8, NivelCpu.nivel(8, 0, 5));
        assertEquals("base 1 também chega no 8", 8, NivelCpu.nivel(1, 4, 5));
        assertEquals("fora da faixa vira o mais próximo", 8, NivelCpu.nivel(5, 99, 5));
    }

    @Test
    public void historiaDeCadaIrmaoTemSeteLutasDo5Ao8() throws IOException {
        GameAssets assets = new GameAssets();
        Temporada t = Temporada.load(assets);
        List<Episodio> eps = new ArrayList<>();
        for (String id : t.episodios) eps.add(t.loadEpisodio(assets, id));
        for (String irmao : new String[] {Torneio.RYUGA, Torneio.KOGA}) {
            List<Integer> niveis = new ArrayList<>();
            for (Episodio ep : eps) {
                for (int i = 0; i < ep.passos.size(); i++) {
                    int[] pos = NivelCpu.posicao(eps, ep.id, i, irmao);
                    if (pos == null) continue;
                    assertEquals(niveis.size(), pos[0]);
                    niveis.add(NivelCpu.nivel(5, pos[0], pos[1]));
                }
            }
            int[] got = new int[niveis.size()];
            for (int i = 0; i < got.length; i++) got[i] = niveis.get(i);
            // Treino do ep01, convite, quartas, emboscada, semifinal, final e o duelo.
            assertArrayEquals(irmao, new int[] {5, 5, 6, 6, 7, 7, 8}, got);
        }
        // O treino-tutorial do Prólogo não conta (não tem IA).
        Episodio prologo = eps.get(0);
        for (int i = 0; i < prologo.passos.size(); i++) {
            assertNull(NivelCpu.posicao(eps, prologo.id, i, Torneio.RYUGA));
        }
    }
}
