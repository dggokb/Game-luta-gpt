package com.gamelutagpt.historia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
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
                assertEquals(Torneio.QUARTAS, t.rodadaAtual());
                assertNull("a emboscada só existe depois das quartas", t.emboscada());
                Torneio.Dupla quartas = t.adversario();
                assertNotSame(t.rival(), quartas);
                assertTrue("o jogador não pega dupla sem golpes", quartas.pronta);
                t.registrar(true);
                assertEquals(Torneio.SEMIFINAL, t.rodadaAtual());
                assertTrue(t.adversario().pronta);
                boolean rivalNaSemi = false;
                for (int i = 0; i < 4; i++) rivalNaSemi |= t.participante(1, i) == t.rival();
                assertTrue("o rival sempre avança", rivalNaSemi);
                t.registrar(true);
                assertEquals(Torneio.FINAL, t.rodadaAtual());
                assertSame(t.rival(), t.adversario());
                assertNull(t.emboscada());
                t.registrar(true);
                assertTrue(t.acabou());
                assertSame(t.jogador(), t.campeao());
                assertNull(t.adversario());
            }
        }
    }

    @Test
    public void emboscadaEhQuemPerdeuAOutraQuartaDoLadoDoJogador() throws IOException {
        for (String irmao : new String[] {Torneio.RYUGA, Torneio.KOGA}) {
            for (long seed = 0; seed < 50; seed++) {
                Torneio t = Torneio.sortear(duplas(), irmao, new Random(seed));
                t.registrar(true);
                Torneio.Dupla emboscada = t.emboscada();
                assertNotNull(emboscada);
                assertTrue("a emboscada é de uma dupla que luta", emboscada.pronta);
                assertNotSame(t.jogador(), emboscada);
                assertNotSame(t.rival(), emboscada);
                // Perdeu as quartas: não está na semifinal.
                for (int i = 0; i < 4; i++) assertNotSame(emboscada, t.participante(1, i));
                // É do lado do jogador e não foi o adversário dele nas quartas.
                int vaga = -1;
                for (int v = 0; v < Torneio.VAGAS; v++) if (t.participante(0, v) == emboscada) vaga = v;
                boolean ladoEsquerdo = vaga < 4;
                assertEquals(Torneio.RYUGA.equals(irmao), ladoEsquerdo);
                int vagaJogador = Torneio.RYUGA.equals(irmao) ? 0 : 7;
                assertNotEquals("não é quem o jogador derrotou", vagaJogador / 2, vaga / 2);
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
        assertNull(t.emboscada());
        assertSame(adversario, t.participante(1, 0));
    }

    @Test
    public void campanhaResolveJogadorRivalETorneio() throws IOException {
        Campanha c = new Campanha(duplas(), 3L);
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

    /** Toca a temporada inteira do jogo como o GameView: escolha, sorteio, lutas e chave. */
    private static List<String[][]> temporada(String irmao, List<String> rotulos, Campanha campanha) throws IOException {
        GameAssets assets = new GameAssets();
        Temporada temporada = Temporada.load(assets);
        Recorder rec = new Recorder();
        PaginaHistoria p = new PaginaHistoria(rec, assets);
        p.setCampanha(campanha);
        List<String[][]> lutas = new ArrayList<>();
        int lutasAntesDaEscolha = -1;
        boolean viuSorteio = false;
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
        }
        assertTrue(viuSorteio);
        assertEquals(irmao, campanha.irmao());
        // O treino do Prólogo é antes da escolha (o jogador é o Ryūga criança).
        assertEquals(1, lutasAntesDaEscolha);
        rotulos.remove(0);
        assertSame(campanha.torneio().jogador(), campanha.torneio().campeao());
        return lutas.subList(lutasAntesDaEscolha, lutas.size());
    }

    @Test
    public void temporadaDoKogaComEmboscadaETerminaNoDueloContraOParceiro() throws IOException {
        List<String> rotulos = new ArrayList<>();
        Campanha campanha = Campanha.load(new GameAssets(), 7L);
        List<String[][]> lutas = temporada(Torneio.KOGA, rotulos, campanha);
        // Treino do ep01, convite do parceiro (ep02), quartas, emboscada fora da arena, semifinal,
        // final, duelo.
        assertEquals(java.util.Arrays.asList("TREINO", "CONVITE", Torneio.QUARTAS, Campanha.EMBOSCADA,
            Torneio.SEMIFINAL, Torneio.FINAL, Campanha.DUELO), rotulos);
        for (int i = 0; i < 6; i++) assertEquals("p03", lutas.get(i)[0][0]);
        // O convite é 1x1: Kōga contra o p04, que vira o parceiro dele.
        assertEquals(1, lutas.get(1)[0].length);
        assertEquals("p04", lutas.get(1)[1][0]);
        assertEquals(1, lutas.get(1)[1].length);
        // A emboscada é a dupla que perdeu a outra quarta do lado do Kōga (vagas 4 e 5).
        String[] emboscada = lutas.get(3)[1];
        Torneio t = campanha.torneio();
        Torneio.Dupla outraQuartaA = t.participante(0, 4), outraQuartaB = t.participante(0, 5);
        Torneio.Dupla perdedora = t.participante(1, 2) == outraQuartaA ? outraQuartaB : outraQuartaA;
        assertEquals(perdedora.lutadores[0], emboscada[0]);
        assertEquals("player_base", lutas.get(5)[1][0]);
        String[][] duelo = lutas.get(6);
        assertEquals("p03", duelo[0][0]);
        assertEquals("p04", duelo[1][0]);
        assertEquals(1, duelo[0].length);
    }

    @Test
    public void temporadaDoRyuga() throws IOException {
        List<String> rotulos = new ArrayList<>();
        List<String[][]> lutas = temporada(Torneio.RYUGA, rotulos, Campanha.load(new GameAssets(), 3L));
        assertEquals(java.util.Arrays.asList("TREINO", "CONVITE", Torneio.QUARTAS, Campanha.EMBOSCADA,
            Torneio.SEMIFINAL, Torneio.FINAL, Campanha.DUELO), rotulos);
        // O convite é 1x1: Ryūga (P01) contra o P02, que vira o parceiro dele.
        assertEquals("player_base", lutas.get(1)[0][0]);
        assertEquals(1, lutas.get(1)[0].length);
        assertEquals("p02", lutas.get(1)[1][0]);
        assertEquals(1, lutas.get(1)[1].length);
        assertEquals("p03", lutas.get(5)[1][0]);
        assertEquals("p02", lutas.get(6)[1][0]);
    }

    /** As páginas prontas que o ep01 mostra, na ordem, para quem escolheu esse irmão. */
    private static List<String> paginasDoEp01(String irmao) throws IOException {
        GameAssets assets = new GameAssets();
        Temporada temporada = Temporada.load(assets);
        Recorder rec = new Recorder();
        PaginaHistoria p = new PaginaHistoria(rec, assets);
        p.setCampanha(Campanha.load(assets, 1L));
        p.start(temporada, temporada.loadEpisodio(assets, "ep01"));
        List<String> paginas = new ArrayList<>();
        for (int i = 0; i < 20000 && rec.ended == null; i++) {
            if (p.waitingFight()) {
                paginas.add("LUTA");
                p.resultadoLuta(true);
                continue;
            }
            Episodio.Passo passo = p.passo();
            if (passo.tipo == Episodio.TipoPasso.ESCOLHA) {
                run(p, 0.5f);
                p.moverEscolha(Torneio.KOGA.equals(irmao) ? 1 : -1);
                p.confirmarEscolha();
                run(p, 1f);
                continue;
            }
            if (passo.imagem != null && (paginas.isEmpty() || !passo.imagem.equals(paginas.get(paginas.size() - 1)))) {
                paginas.add(passo.imagem);
            }
            run(p, 0.6f);
            p.tap(640f, 360f);
        }
        assertNotNull("o ep01 não terminou", rec.ended);
        return paginas;
    }

    @Test
    public void ep01TemComecoEFimDeCadaIrmao() throws IOException {
        List<String> meio = java.util.Arrays.asList("pag4.jpg", "pag5.jpg", "pag6.jpg", "LUTA",
            "pag7.jpg", "pag8.jpg", "pag9.jpg", "pag10.jpg");
        List<String> ryuga = new ArrayList<>(java.util.Arrays.asList("pag1.jpg", "pag2.jpg", "pag3.jpg"));
        ryuga.addAll(meio);
        ryuga.add("pag11.jpg");
        List<String> koga = new ArrayList<>(java.util.Arrays.asList("pag1_koga.jpg", "pag2_koga.jpg", "pag3_koga.jpg"));
        koga.addAll(meio);
        koga.add("pag11_koga.jpg");
        assertEquals(ryuga, paginasDoEp01(Torneio.RYUGA));
        assertEquals(koga, paginasDoEp01(Torneio.KOGA));
    }

    @Test
    public void semCampanhaAsEtapasDoTorneioSaoPuladas() throws IOException {
        GameAssets assets = new GameAssets();
        Temporada temporada = Temporada.load(assets);
        Recorder rec = new Recorder();
        PaginaHistoria p = new PaginaHistoria(rec, assets);
        p.start(temporada, temporada.loadEpisodio(assets, "ep03"));
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
        Campanha campanha = Campanha.load(assets, 1L);
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
