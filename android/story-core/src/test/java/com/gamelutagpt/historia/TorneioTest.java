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
                assertEquals(Torneio.RYUGA, t.vaga(0).irmao);
                assertEquals(Torneio.KOGA, t.vaga(7).irmao);
                assertEquals(irmao, t.jogador().irmao);
                Set<Torneio.Dupla> todas = new HashSet<>();
                for (int v = 0; v < Torneio.VAGAS; v++) {
                    Torneio.Dupla d = t.vaga(v);
                    assertNotNull(d);
                    todas.add(d);
                    boolean ladoDoJogador = (v < 4) == Torneio.RYUGA.equals(irmao);
                    if (!d.pronta) assertFalse("dupla sem golpes no lado do jogador", ladoDoJogador);
                }
                assertEquals(Torneio.VAGAS, todas.size());
                assertEquals(4, t.rodada(0).size());
            }
        }
    }

    @Test
    public void sorteioMudaASorte() throws IOException {
        Set<String> chaves = new HashSet<>();
        for (long seed = 0; seed < 30; seed++) {
            Torneio t = Torneio.sortear(duplas(), Torneio.RYUGA, new Random(seed));
            StringBuilder sb = new StringBuilder();
            for (int v = 0; v < Torneio.VAGAS; v++) sb.append(t.vaga(v).id).append(',');
            chaves.add(sb.toString());
        }
        assertTrue("o sorteio sempre deu a mesma chave", chaves.size() > 5);
    }

    /** Joga a chave inteira; perdeQuartas: o jogador perde a primeira luta. Devolve os nomes das lutas dele. */
    private static List<String> jogar(Torneio t, boolean perdeQuartas) {
        List<String> lutas = new ArrayList<>();
        boolean primeira = true;
        while (t.adversario() != null) {
            lutas.add(t.rodadaAtual());
            assertTrue("o jogador não pega dupla sem golpes", t.adversario().pronta);
            if (!Torneio.FINAL.equals(t.rodadaAtual())) assertNotSame(t.rival(), t.adversario());
            t.registrar(!(primeira && perdeQuartas));
            primeira = false;
            // O rival nunca perde para o CPU.
            for (int r = 0; r < t.disputadas(); r++) {
                for (Torneio.Confronto c : t.rodada(r)) {
                    if (c.tem(t.rival()) && !c.tem(t.jogador())) assertSame(t.rival(), c.vencedor());
                }
            }
        }
        return lutas;
    }

    @Test
    public void caminhoDiretoTemQuatroLutasAteSerCampeao() throws IOException {
        for (String irmao : new String[] {Torneio.RYUGA, Torneio.KOGA}) {
            for (long seed = 0; seed < 50; seed++) {
                Torneio t = Torneio.sortear(duplas(), irmao, new Random(seed));
                List<String> lutas = jogar(t, false);
                assertEquals(java.util.Arrays.asList(Torneio.QUARTAS, Torneio.SEMIFINAL, Torneio.FINAL_DA_CHAVE, Torneio.FINAL), lutas);
                assertFalse(t.pelaRepescagem());
                assertTrue(t.acabou());
                assertSame(t.jogador(), t.campeao());
                Torneio.Confronto fin = t.confronto(3, -1, Torneio.FINAL);
                assertTrue("a final é contra o rival", fin.tem(t.rival()));
            }
        }
    }

    @Test
    public void quemPerdeNasQuartasVaiPelaRepescagemComAsMesmasLutas() throws IOException {
        for (String irmao : new String[] {Torneio.RYUGA, Torneio.KOGA}) {
            for (long seed = 0; seed < 50; seed++) {
                Torneio t = Torneio.sortear(duplas(), irmao, new Random(seed));
                assertTrue(t.derrotaTemSegundaChance());
                List<String> lutas = jogar(t, true);
                assertEquals(java.util.Arrays.asList(Torneio.QUARTAS, Torneio.REPESCAGEM, Torneio.FINAL_DA_CHAVE, Torneio.FINAL), lutas);
                assertTrue(t.pelaRepescagem());
                assertFalse(t.jogadorEliminado());
                assertSame(t.jogador(), t.campeao());
                // Na final da chave, o vencedor da repescagem pega o vencedor da semifinal.
                int lado = Torneio.RYUGA.equals(irmao) ? 0 : 1;
                Torneio.Confronto fc = t.confronto(2, lado, Torneio.FINAL_DA_CHAVE);
                assertSame(t.confronto(1, lado, Torneio.SEMIFINAL).vencedor(), fc.a);
                assertSame(t.jogador(), fc.b);
            }
        }
    }

    @Test
    public void cadaLadoTemSemifinalERepescagemComOsPerdedores() throws IOException {
        Torneio t = Torneio.sortear(duplas(), Torneio.RYUGA, new Random(9));
        t.registrar(true);
        for (int lado = 0; lado < 2; lado++) {
            Torneio.Confronto q1 = t.rodada(0).get(2 * lado), q2 = t.rodada(0).get(2 * lado + 1);
            Torneio.Confronto semi = t.confronto(1, lado, Torneio.SEMIFINAL);
            Torneio.Confronto rep = t.confronto(1, lado, Torneio.REPESCAGEM);
            assertSame(q1.vencedor(), semi.a);
            assertSame(q2.vencedor(), semi.b);
            assertSame(q1.perdedor(), rep.a);
            assertSame(q2.perdedor(), rep.b);
        }
        assertFalse("depois das quartas, perder não tem segunda chance", t.derrotaTemSegundaChance());
    }

    @Test
    public void derrotaDepoisDasQuartasEliminaOJogador() throws IOException {
        Torneio t = Torneio.sortear(duplas(), Torneio.RYUGA, new Random(1));
        t.registrar(true);
        Torneio.Dupla adversario = t.adversario();
        t.registrar(false);
        assertTrue(t.jogadorEliminado());
        assertNull(t.adversario());
        assertSame(adversario, t.confronto(1, 0, Torneio.SEMIFINAL).vencedor());
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
        // Duelo 1x1: o capitão contra o próprio parceiro.
        Episodio duelo = Episodio.fromJson("{\"id\":\"e\",\"passos\":[{\"tipo\":\"luta\",\"duelo\":true}]}");
        String[][] lados = c.lados(duelo.passos.get(0));
        assertEquals(1, lados[0].length);
        assertEquals("player_base", lados[0][0]);
        assertEquals("p02", lados[1][0]);
        assertEquals(Campanha.DUELO, c.rotulo(duelo.passos.get(0)));
        assertFalse("o duelo só vale para a dupla campeã", c.vale(duelo.passos.get(0)));
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

    /**
     * Toca a temporada inteira do jogo como o GameView: escolha, sorteio, lutas e chave. Com
     * perdeQuartas, o jogador perde a primeira luta do torneio e segue pela repescagem.
     */
    private static List<String[][]> temporada(String irmao, boolean perdeQuartas, List<String> rotulos,
                                               List<String> episodiosTocados) throws IOException {
        GameAssets assets = new GameAssets();
        Temporada temporada = Temporada.load(assets);
        Campanha campanha = Campanha.load(assets, new Random(7));
        Recorder rec = new Recorder();
        PaginaHistoria p = new PaginaHistoria(rec, assets);
        p.setCampanha(campanha);
        List<String[][]> lutas = new ArrayList<>();
        int lutasAntesDaEscolha = -1;
        boolean viuSorteio = false;
        boolean perdeu = false;
        for (String id : temporada.episodios) {
            rec.ended = null;
            p.start(temporada, temporada.loadEpisodio(assets, id));
            for (int i = 0; i < 20000 && rec.ended == null; i++) {
                if (p.waitingFight()) {
                    Episodio.Passo luta = p.passo();
                    lutas.add(p.lados(luta));
                    rotulos.add(p.rotulo(luta));
                    boolean venceu = true;
                    if (perdeQuartas && !perdeu && Torneio.QUARTAS.equals(p.rotulo(luta))) {
                        perdeu = true;
                        venceu = false;
                    }
                    p.resultadoLuta(venceu);
                    continue;
                }
                Episodio.Passo passo = p.passo();
                if (passo.tipo == Episodio.TipoPasso.ESCOLHA) {
                    lutasAntesDaEscolha = lutas.size();
                    assertTrue(p.esperandoEscolha());
                    run(p, 0.5f);
                    p.moverEscolha(Torneio.KOGA.equals(irmao) ? 1 : -1);
                    p.confirmarEscolha();
                    run(p, 1f);
                    continue;
                }
                if (passo.tipo == Episodio.TipoPasso.SORTEIO) viuSorteio = true;
                run(p, 0.6f);
                p.tap(640f, 360f);
            }
            assertNotNull(id + " não terminou", rec.ended);
            episodiosTocados.add(id);
        }
        assertTrue(viuSorteio);
        assertEquals(irmao, campanha.irmao());
        // O treino do Prólogo é antes da escolha (o jogador é o Ryūga criança).
        assertEquals(1, lutasAntesDaEscolha);
        assertEquals(perdeQuartas, campanha.torneio().pelaRepescagem());
        assertSame(campanha.torneio().jogador(), campanha.torneio().campeao());
        return lutas.subList(lutasAntesDaEscolha, lutas.size());
    }

    @Test
    public void temporadaDoKogaPelaRepescagemTerminaNoDueloContraOParceiro() throws IOException {
        List<String> rotulos = new ArrayList<>();
        List<String[][]> lutas = temporada(Torneio.KOGA, true, rotulos, new ArrayList<String>());
        // Treino do ep01, quartas (perdida, sem repetir), repescagem, final da chave, final, duelo.
        assertEquals(6, lutas.size());
        assertEquals(java.util.Arrays.asList("TREINO", Torneio.QUARTAS, Torneio.REPESCAGEM, Torneio.FINAL_DA_CHAVE,
            Torneio.FINAL, Campanha.DUELO), rotulos.subList(1, rotulos.size()));
        for (int i = 0; i < 5; i++) assertEquals("p03", lutas.get(i)[0][0]);
        assertEquals("player_base", lutas.get(4)[1][0]);
        String[][] duelo = lutas.get(5);
        assertEquals("p03", duelo[0][0]);
        assertEquals("p04", duelo[1][0]);
        assertEquals(1, duelo[0].length);
    }

    @Test
    public void temporadaDoRyugaPeloCaminhoDireto() throws IOException {
        List<String> rotulos = new ArrayList<>();
        List<String[][]> lutas = temporada(Torneio.RYUGA, false, rotulos, new ArrayList<String>());
        assertEquals(java.util.Arrays.asList("TREINO", Torneio.QUARTAS, Torneio.SEMIFINAL, Torneio.FINAL_DA_CHAVE,
            Torneio.FINAL, Campanha.DUELO), rotulos.subList(1, rotulos.size()));
        assertEquals("p03", lutas.get(4)[1][0]);
        assertEquals("p02", lutas.get(5)[1][0]);
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
