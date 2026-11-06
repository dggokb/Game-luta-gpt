package com.gamelutagpt.historia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class TelaVersusTest {
    private static final float DT = 1f / 60f;
    private static final File ASSETS = new File("../app/src/main/assets");

    private static final class GameAssets implements RenderAssets {
        @Override public String readText(String path) throws IOException {
            return new String(Files.readAllBytes(new File(ASSETS, path).toPath()), StandardCharsets.UTF_8);
        }
        @Override public RenderImage loadImage(String path) throws IOException { throw new IOException("sem " + path); }
    }

    private static final class Recorder implements HistoriaListener {
        final List<Episodio.Passo> fights = new ArrayList<>();
        @Override public void onHistoriaLuta(Episodio.Passo luta) { fights.add(luta); }
        @Override public void onHistoriaSom(String id) {}
        @Override public void onHistoriaFim(Episodio episodio) {}
    }

    private static void run(PaginaHistoria p, float seconds) {
        for (float t = 0f; t < seconds; t += DT) {
            p.update(DT);
            p.render(new PaginaHistoriaTest.NullCanvas());
        }
    }

    /** Toca o episódio com toques até o passo de luta (sem tocar nele). */
    private static PaginaHistoria ateALuta(String ep, Recorder rec) throws IOException {
        GameAssets assets = new GameAssets();
        Temporada temporada = Temporada.load(assets);
        Campanha campanha = Campanha.load(assets, 5L);
        campanha.escolher(Torneio.RYUGA);
        PaginaHistoria p = new PaginaHistoria(rec, assets);
        p.setCampanha(campanha);
        p.start(temporada, temporada.loadEpisodio(assets, ep));
        for (int i = 0; i < 5000 && p.passo().tipo != Episodio.TipoPasso.LUTA; i++) {
            // Para no primeiro quadro do passo de luta (a tela de versus começa do zero).
            for (float t = 0f; t < 0.6f && p.passo().tipo != Episodio.TipoPasso.LUTA; t += DT) p.update(DT);
            if (p.passo().tipo != Episodio.TipoPasso.LUTA) p.tap(640f, 360f);
        }
        assertEquals(Episodio.TipoPasso.LUTA, p.passo().tipo);
        return p;
    }

    /** Toda pose da tela de versus existe (todos os lutadores do torneio). */
    @Test
    public void todoLutadorDoTorneioTemPose() throws IOException {
        for (Torneio.Dupla d : Torneio.duplasFromJson(new GameAssets().readText(Torneio.ARQUIVO))) {
            for (int i = 0; i < 2; i++) {
                assertTrue("sem pose: " + d.rosto(i), new File(ASSETS, TelaVersus.PASTA + d.rosto(i) + ".png").isFile());
            }
        }
    }

    /** A luta do torneio espera a tela de versus; o toque leva direto ao clarão que abre a luta. */
    @Test
    public void lutaDoTorneioComecaDepoisDoVersus() throws IOException {
        Recorder rec = new Recorder();
        PaginaHistoria p = ateALuta("ep04", rec);
        assertTrue(p.passo().torneio);
        assertTrue("a tela de versus cobre a tela", p.coversScreen());
        assertEquals(0, rec.fights.size());
        run(p, TelaVersus.DURACAO - 0.2f);
        assertEquals("a luta ainda não começou", 0, rec.fights.size());
        run(p, 0.4f);
        assertEquals(1, rec.fights.size());
        assertTrue(p.waitingFight());

        // De novo, pulando com um toque (depois do mínimo): o clarão final e a luta.
        Recorder rec2 = new Recorder();
        PaginaHistoria q = ateALuta("ep04", rec2);
        run(q, TelaVersus.MIN_TOQUE + 0.05f);
        q.tap(640f, 360f);
        run(q, TelaVersus.SAIDA + 0.1f);
        assertEquals(1, rec2.fights.size());
    }

    /** O desafio no porto não é do torneio: a luta começa direto, sem versus. */
    @Test
    public void desafioNoPortoNaoTemVersus() throws IOException {
        Recorder rec = new Recorder();
        PaginaHistoria p = ateALuta("ep03", rec);
        assertEquals(1, rec.fights.size());
        assertTrue(p.waitingFight());
    }
}
