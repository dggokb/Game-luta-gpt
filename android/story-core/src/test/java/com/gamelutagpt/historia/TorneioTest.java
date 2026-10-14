package com.gamelutagpt.historia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.Test;

public class TorneioTest {
    private static final float DT = 1f / 60f;
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

    @Test
    public void torneioDoJogoTemAsOitoDuplasFixasComRetratos() throws IOException {
        List<Torneio.Dupla> duplas = duplas();
        assertEquals(Torneio.VAGAS, duplas.size());
        Set<String> lutadores = new HashSet<>();
        for (Torneio.Dupla d : duplas) {
            for (int i = 0; i < 2; i++) {
                assertTrue("lutador em duas duplas: " + d.lutadores[i], lutadores.add(d.lutadores[i]));
                assertTrue("sem retrato: " + d.rosto(i), new File(ASSETS, Rostos.PASTA + d.rosto(i) + ".jpg").isFile());
            }
        }
        assertEquals("ryuga", duplas.get(0).irmao);
        assertEquals("player_base", duplas.get(0).lutadores[0]);
        assertEquals("p02", duplas.get(0).lutadores[1]);
        assertEquals("koga", duplas.get(1).irmao);
        assertEquals("p03", duplas.get(1).lutadores[0]);
    }

    @Test
    public void irmaosNasPontasESemGolpesNoLadoDoRival() throws IOException {
        for (String irmao : new String[] {Torneio.RYUGA, Torneio.KOGA}) {
            for (long seed = 0; seed < 200; seed++) {
                Torneio t = Torneio.sortear(duplas(), irmao, new Random(seed));
                assertEquals(Torneio.RYUGA, t.participante(0, 0).irmao);
                assertEquals(Torneio.KOGA, t.participante(0, 7).irmao);
                assertEquals(irmao, t.jogador().irmao);
                Set<Torneio.Dupla> todas = new HashSet<>();
                for (int v = 0; v < Torneio.VAGAS; v++) {
                    Torneio.Dupla d = t.participante(0, v);
                    assertNotNull(d);
                    todas.add(d);
                    boolean ladoDoJogador = (v < 4) == Torneio.RYUGA.equals(irmao);
                    if (!d.pronta) assertFalse("dupla sem golpes no lado do jogador", ladoDoJogador);
                }
                assertEquals(Torneio.VAGAS, todas.size());
            }
        }
    }

    @Test
    public void sorteioMudaASorte() throws IOException {
        Set<String> chaves = new HashSet<>();
        for (long seed = 0; seed < 30; seed++) {
            Torneio t = Torneio.sortear(duplas(), Torneio.RYUGA, new Random(seed));
            StringBuilder sb = new StringBuilder();
            for (int v = 0; v < Torneio.VAGAS; v++) sb.append(t.participante(0, v).id).append(',');
            chaves.add(sb.toString());
        }
        assertTrue("o sorteio sempre deu a mesma chave", chaves.size() > 5);
    }

    @Test
    public void jogadorVaiAteAFinalContraORival() throws IOException {
        for (String irmao : new String[] {Torneio.RYUGA, Torneio.KOGA}) {
            for (long seed = 0; seed < 50; seed++) {
                Torneio t = Torneio.sortear(duplas(), irmao, new Random(seed));
                assertEquals("QUARTAS DE FINAL", t.rodadaAtual());
                Torneio.Dupla quartas = t.adversario();
                assertNotSame(t.rival(), quartas);
                assertTrue("o jogador não pega dupla sem golpes", quartas.pronta);
                t.registrar(true);
                assertEquals("SEMIFINAL", t.rodadaAtual());
                assertTrue(t.adversario().pronta);
                // O rival sempre avança.
                boolean rivalNaSemi = false;
                for (int i = 0; i < 4; i++) rivalNaSemi |= t.participante(1, i) == t.rival();
                assertTrue(rivalNaSemi);
                t.registrar(true);
                assertEquals("FINAL", t.rodadaAtual());
                assertSame(t.rival(), t.adversario());
                t.registrar(true);
                assertTrue(t.acabou());
                assertSame(t.jogador(), t.campeao());
                assertNull(t.adversario());
                assertNull(t.rodadaAtual());
            }
        }
    }

    @Test
    public void derrotaEliminaOJogador() throws IOException {
        Torneio t = Torneio.sortear(duplas(), Torneio.RYUGA, new Random(1));
        Torneio.Dupla adversario = t.adversario();
        t.registrar(false);
        assertTrue(t.jogadorEliminado());
        assertNull(t.adversario());
        assertSame(adversario, t.participante(1, 0));
    }

    @Test
    public void campanhaResolveJogadorRivalETorneio() throws IOException {
        Campanha c = new Campanha(duplas(), new Random(3));
        assertEquals(Torneio.RYUGA, c.irmao());
        assertFalse(c.escolheu());
        Episodio ep = Episodio.fromJson("{\"id\":\"e\",\"passos\":[" +
            "{\"tipo\":\"luta\",\"dupla\":\"jogador\",\"cpu\":\"rival\"}," +
            "{\"tipo\":\"luta\",\"torneio\":true}]}");
        c.escolher(Torneio.KOGA);
        String[][] treino = c.lados(ep.passos.get(0));
        assertEquals("p03", treino[0][0]);
        assertEquals("player_base", treino[1][0]);
        String[][] quartas = c.lados(ep.passos.get(1));
        assertEquals("p03", quartas[0][0]);
        assertEquals("p04", quartas[0][1]);
        assertEquals("QUARTAS DE FINAL", c.rotulo(ep.passos.get(1)));
        // Trocar de irmão desfaz a chave.
        Torneio antes = c.torneio();
        c.escolher(Torneio.RYUGA);
        assertNotSame(antes, c.torneio());
        assertEquals("player_base", c.torneio().jogador().lutadores[0]);
    }

    private static final class Recorder implements HistoriaListener {
        final List<Episodio.Passo> fights = new ArrayList<>();
        Episodio ended;
        @Override public void onHistoriaLuta(Episodio.Passo luta) { fights.add(luta); }
        @Override public void onHistoriaSom(String id) {}
        @Override public void onHistoriaFim(Episodio episodio) { ended = episodio; }
    }

    private static void run(PaginaHistoria p, float seconds) {
        for (float t = 0f; t < seconds; t += DT) {
            p.update(DT);
            p.render(new PaginaHistoriaTest.NullCanvas());
        }
    }

    /** Toca a temporada inteira do jogo como o GameView: escolha, sorteio, lutas e chave. */
    @Test
    public void temporadaInteiraComKogaChegaAFinalContraORyuga() throws IOException {
        GameAssets assets = new GameAssets();
        Temporada temporada = Temporada.load(assets);
        Campanha campanha = Campanha.load(assets, new Random(7));
        Recorder rec = new Recorder();
        PaginaHistoria p = new PaginaHistoria(rec, assets);
        p.setCampanha(campanha);
        List<String[][]> lutas = new ArrayList<>();
        List<String> rotulos = new ArrayList<>();
        boolean viuEscolha = false, viuSorteio = false;
        int lutasAntesDaEscolha = -1;
        for (String id : temporada.episodios) {
            rec.ended = null;
            p.start(temporada, temporada.loadEpisodio(assets, id));
            for (int i = 0; i < 20000 && rec.ended == null; i++) {
                if (p.waitingFight()) {
                    Episodio.Passo luta = p.passo();
                    lutas.add(p.lados(luta));
                    rotulos.add(p.rotulo(luta));
                    p.resultadoLuta(true);
                    continue;
                }
                Episodio.Passo passo = p.passo();
                if (passo.tipo == Episodio.TipoPasso.ESCOLHA) {
                    viuEscolha = true;
                    lutasAntesDaEscolha = lutas.size();
                    assertTrue(p.esperandoEscolha());
                    run(p, 0.5f);
                    p.moverEscolha(1);
                    p.confirmarEscolha();
                    run(p, 1f);
                    continue;
                }
                if (passo.tipo == Episodio.TipoPasso.SORTEIO) viuSorteio = true;
                run(p, 0.6f);
                p.tap(640f, 360f);
            }
            assertNotNull(id + " não terminou", rec.ended);
        }
        assertTrue(viuEscolha);
        assertTrue(viuSorteio);
        assertEquals(Torneio.KOGA, campanha.irmao());
        // O treino do Prólogo é antes da escolha (o jogador é o Ryūga criança). Depois: o
        // treino do ep01 e as três lutas do torneio, sempre com a dupla do Kōga.
        assertEquals(1, lutasAntesDaEscolha);
        assertEquals(4, lutas.size() - lutasAntesDaEscolha);
        for (int i = lutasAntesDaEscolha; i < lutas.size(); i++) {
            assertNotNull(lutas.get(i));
            assertEquals("p03", lutas.get(i)[0][0]);
        }
        assertTrue(rotulos.contains("QUARTAS DE FINAL"));
        assertTrue(rotulos.contains("SEMIFINAL"));
        assertEquals("FINAL", rotulos.get(rotulos.size() - 1));
        assertEquals("player_base", lutas.get(lutas.size() - 1)[1][0]);
        assertTrue(campanha.torneio().acabou());
        assertSame(campanha.torneio().jogador(), campanha.torneio().campeao());
    }

    @Test
    public void semCampanhaAsEtapasDoTorneioSaoPuladas() throws IOException {
        GameAssets assets = new GameAssets();
        Temporada temporada = Temporada.load(assets);
        Recorder rec = new Recorder();
        PaginaHistoria p = new PaginaHistoria(rec, assets);
        p.start(temporada, temporada.loadEpisodio(assets, "ep02"));
        for (int i = 0; i < 5000 && rec.ended == null; i++) {
            Episodio.Passo passo = p.passo();
            if (passo != null) {
                assertFalse(passo.tipo == Episodio.TipoPasso.SORTEIO || passo.tipo == Episodio.TipoPasso.CHAVE || passo.torneio);
                assertTrue("passo de um irmão só sem campanha", passo.irmao == null);
            }
            run(p, 0.6f);
            p.tap(640f, 360f);
        }
        assertNotNull(rec.ended);
    }

    @Test
    public void escolhaNaoSePulaEPularVaiAteEla() throws IOException {
        GameAssets assets = new GameAssets();
        Temporada temporada = Temporada.load(assets);
        PaginaHistoria p = new PaginaHistoria(new Recorder(), assets);
        Campanha campanha = Campanha.load(assets, new Random(1));
        p.setCampanha(campanha);
        p.start(temporada, temporada.loadEpisodio(assets, "ep01"));
        assertEquals(Episodio.TipoPasso.ESCOLHA, p.passo().tipo);
        p.tap((PaginaHistoria.SKIP_LEFT + PaginaHistoria.SKIP_RIGHT) * 0.5f, 10f); // PULAR não vale aqui
        run(p, 2f);
        assertEquals(Episodio.TipoPasso.ESCOLHA, p.passo().tipo);
        p.tap(200f, 400f); // lado esquerdo: Ryūga
        run(p, 1f);
        assertEquals(Torneio.RYUGA, campanha.irmao());
        assertTrue(campanha.escolheu());
        assertEquals(Episodio.TipoPasso.TITULO, p.passo().tipo);
    }
}
