package com.gamelutagpt.historia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;
import org.junit.Test;

public class SorteioCinemaTest {
    private static final File ASSETS = new File("../app/src/main/assets");

    private static final class GameAssets implements RenderAssets {
        @Override public String readText(String path) throws IOException {
            return new String(Files.readAllBytes(new File(ASSETS, path).toPath()), StandardCharsets.UTF_8);
        }
        @Override public RenderImage loadImage(String path) throws IOException { throw new IOException("sem " + path); }
    }

    private static List<Torneio.Dupla> duplas() throws IOException {
        return Torneio.duplasFromJson(new GameAssets().readText(Torneio.ARQUIVO));
    }

    /** Toda dupla do torneio (os irmãos também) tem o card de mangá e a fala dele. */
    @Test
    public void todaDuplaTemCardEAsSorteadasTemFala() throws IOException {
        for (Torneio.Dupla d : duplas()) {
            assertTrue("sem card: " + d.id, new File(ASSETS, SorteioCinema.PASTA + d.id + ".jpg").isFile());
            assertNotNull("sem fala no sorteio: " + d.id, d.fala);
        }
    }

    /** A cena só existe na história (com rival); o Arcade continua com o sorteio rápido. */
    @Test
    public void cenaSoNaHistoria() throws IOException {
        List<Torneio.Dupla> duplas = duplas();
        Torneio historia = Torneio.sortear(duplas, Torneio.RYUGA, new Random(3));
        assertTrue(SorteioCinema.ativo(historia));
        assertEquals(6, historia.ordemSorteio().length);
        // Primeiro os irmãos, nas pontas; depois as sorteadas.
        assertEquals(8, SorteioCinema.entradas(historia));
        assertEquals(0, SorteioCinema.vagaDo(historia, 0));
        assertEquals(7, SorteioCinema.vagaDo(historia, 1));
        assertFalse(SorteioCinema.gira(1));
        assertTrue(SorteioCinema.gira(2));
        assertEquals(SorteioCinema.duracao(historia), ChaveTorneio.duracaoSorteio(historia), 1e-6f);
        List<Torneio.Dupla> outras = new java.util.ArrayList<>();
        for (int i = 0; i < 15; i++) {
            Torneio.Dupla d = duplas.get(1 + i % (duplas.size() - 1));
            outras.add(new Torneio.Dupla("cpu" + i, d.lutadores, null, true));
        }
        Torneio arcade = Torneio.arcade(duplas.get(0), outras, new Random(3));
        assertFalse(SorteioCinema.ativo(arcade));
    }

    /**
     * Cada toque leva ao próximo marco: giro → card → fala inteira → voo → próxima dupla, sempre
     * para a frente, e uns poucos toques por dupla chegam ao fim da cena.
     */
    @Test
    public void toquesAvancamAteOFim() throws IOException {
        Temporada temporada = Temporada.load(new GameAssets());
        Torneio t = Torneio.sortear(duplas(), Torneio.KOGA, new Random(7));
        float time = 0f;
        int toques = 0;
        while (time < SorteioCinema.duracao(t)) {
            float depois = SorteioCinema.proximoMarco(temporada, t, time);
            assertTrue("o toque tem que avançar: " + time, depois > time);
            time = depois;
            assertTrue("toques demais", ++toques <= 1 + 4 * SorteioCinema.entradas(t) + 1);
        }
        // Cada dupla bate na vaga depois da anterior.
        for (int k = 1; k < SorteioCinema.entradas(t); k++) {
            assertTrue(SorteioCinema.chegada(k) > SorteioCinema.chegada(k - 1));
        }
        // O card estala com o som de impacto.
        assertEquals("impacto", SorteioCinema.som(t, SorteioCinema.inicioCard(2) - 0.01f, SorteioCinema.inicioCard(2) + 0.01f));
        assertEquals("impacto", SorteioCinema.som(t, SorteioCinema.inicioCard(0) - 0.01f, SorteioCinema.inicioCard(0) + 0.01f));
    }
}
