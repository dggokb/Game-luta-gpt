package com.gamelutagpt.historia;

import static org.junit.Assert.assertArrayEquals;
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
        // Na história o duelo é sempre do líder, mesmo que o parceiro tenha vencido a final.
        c.venceuCom("p02");
        String[][] lider = c.lados(duelo.passos.get(0));
        assertEquals("player_base", lider[0][0]);
        assertEquals("p02", lider[1][0]);
        // "solo": o treino do ep01 é 1x1, só os irmãos (sem troca).
        Episodio solo = Episodio.fromJson("{\"id\":\"e\",\"passos\":[" +
            "{\"tipo\":\"luta\",\"dupla\":\"jogador\",\"cpu\":\"rival\",\"solo\":true}]}");
        String[][] irmaos = c.lados(solo.passos.get(0));
        assertEquals(1, irmaos[0].length);
        assertEquals(1, irmaos[1].length);
        assertEquals("player_base", irmaos[0][0]);
        assertEquals("p03", irmaos[1][0]);
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
        // Treino do ep01, convite do parceiro (ep02), desafio no porto (ep03), quartas, emboscada
        // fora da arena, semifinal, final, duelo.
        assertEquals(java.util.Arrays.asList("TREINO", "CONVITE", "DESAFIO", Torneio.QUARTAS, Campanha.EMBOSCADA,
            Torneio.SEMIFINAL, Torneio.FINAL, Campanha.DUELO), rotulos);
        for (int i = 0; i < 7; i++) assertEquals("p03", lutas.get(i)[0][0]);
        // O convite é 1x1: Kōga contra o p04, que vira o parceiro dele.
        assertEquals(1, lutas.get(1)[0].length);
        assertEquals("p04", lutas.get(1)[1][0]);
        assertEquals(1, lutas.get(1)[1].length);
        // O desafio no porto é 2x2: a dupla do Kōga contra a dupla escondida do lado do rival.
        assertArrayEquals(new String[] {"p03", "p04"}, lutas.get(2)[0]);
        assertArrayEquals(campanha.adversarioPorto().lutadores, lutas.get(2)[1]);
        // A emboscada é a dupla que perdeu a outra quarta do lado do Kōga (vagas 4 e 5).
        String[] emboscada = lutas.get(4)[1];
        Torneio t = campanha.torneio();
        Torneio.Dupla outraQuartaA = t.participante(0, 4), outraQuartaB = t.participante(0, 5);
        Torneio.Dupla perdedora = t.participante(1, 2) == outraQuartaA ? outraQuartaB : outraQuartaA;
        assertEquals(perdedora.lutadores[0], emboscada[0]);
        assertEquals("player_base", lutas.get(6)[1][0]);
        String[][] duelo = lutas.get(7);
        assertEquals("p03", duelo[0][0]);
        assertEquals("p04", duelo[1][0]);
        assertEquals(1, duelo[0].length);
    }

    @Test
    public void temporadaDoRyuga() throws IOException {
        List<String> rotulos = new ArrayList<>();
        Campanha campanha = Campanha.load(new GameAssets(), 3L);
        List<String[][]> lutas = temporada(Torneio.RYUGA, rotulos, campanha);
        assertEquals(java.util.Arrays.asList("TREINO", "CONVITE", "DESAFIO", Torneio.QUARTAS, Campanha.EMBOSCADA,
            Torneio.SEMIFINAL, Torneio.FINAL, Campanha.DUELO), rotulos);
        // O convite é 1x1: Ryūga (P01) contra o P02, que vira o parceiro dele.
        assertEquals("player_base", lutas.get(1)[0][0]);
        assertEquals(1, lutas.get(1)[0].length);
        assertEquals("p02", lutas.get(1)[1][0]);
        assertEquals(1, lutas.get(1)[1].length);
        assertArrayEquals(new String[] {"player_base", "p02"}, lutas.get(2)[0]);
        assertArrayEquals(campanha.adversarioPorto().lutadores, lutas.get(2)[1]);
        // Quem lutou no porto não volta: quartas, emboscada e semifinal são outras duplas.
        for (int i = 3; i <= 5; i++) assertNotEquals(lutas.get(2)[1][0], lutas.get(i)[1][0]);
        assertEquals("p03", lutas.get(6)[1][0]);
        assertEquals("p02", lutas.get(7)[1][0]);
    }

    @Test
    public void adversarioDoPortoEhDoLadoDoRivalENaoMudaComOSorteio() throws IOException {
        Set<String> vistos = new HashSet<>();
        for (String irmao : new String[] {Torneio.RYUGA, Torneio.KOGA}) {
            for (long semente = 0; semente < 200; semente++) {
                Campanha c = Campanha.load(new GameAssets(), semente);
                c.escolher(irmao);
                Torneio.Dupla porto = c.adversarioPorto();
                assertNotNull(porto);
                assertTrue(porto.id + " sem golpes no porto", porto.pronta);
                assertNull(porto.irmao);
                vistos.add(porto.id);
                // O sorteio (depois do porto) dá a mesma chave e o mesmo adversário.
                Torneio t = c.torneio();
                assertSame(porto, c.adversarioPorto());
                assertTrue(t.prontasDoLadoDoRival().contains(porto));
                // Fica na metade do rival: o jogador só o encontraria na final, contra o rival.
                int metade = Torneio.VAGAS / 2;
                int lado = Torneio.KOGA.equals(irmao) ? 0 : metade;
                boolean achou = false;
                for (int i = lado; i < lado + metade; i++) achou |= t.participante(0, i) == porto;
                assertTrue(achou);
            }
        }
        // Todas as duplas com golpes podem aparecer no porto.
        assertEquals(new HashSet<>(java.util.Arrays.asList("p05", "p07", "p09", "p11")), vistos);
    }

    /** As páginas prontas (e "LUTA") do episódio {@code id}, com o irmão já escolhido. */
    private static List<String> paginas(String id, Campanha campanha) throws IOException {
        GameAssets assets = new GameAssets();
        Temporada temporada = Temporada.load(assets);
        Recorder rec = new Recorder();
        PaginaHistoria p = new PaginaHistoria(rec, assets);
        p.setCampanha(campanha);
        p.start(temporada, temporada.loadEpisodio(assets, id));
        List<String> paginas = new ArrayList<>();
        for (int i = 0; i < 20000 && rec.ended == null; i++) {
            if (p.waitingFight()) {
                paginas.add("LUTA");
                p.resultadoLuta(true);
                continue;
            }
            Episodio.Passo passo = p.passo();
            String nome = passo.imagem != null ? passo.imagem
                : passo.tipo == Episodio.TipoPasso.PAGINA ? passo.paineis.get(0).imagem : null;
            if (nome != null && (paginas.isEmpty() || !nome.equals(paginas.get(paginas.size() - 1)))) paginas.add(nome);
            run(p, 0.6f);
            p.tap(640f, 360f);
        }
        assertNotNull(id + " não terminou", rec.ended);
        return paginas;
    }

    /** Uma campanha do irmão com {@code adversario} no porto. */
    private static Campanha campanhaComPorto(String irmao, String adversario) throws IOException {
        for (long semente = 0; semente < 500; semente++) {
            Campanha c = Campanha.load(new GameAssets(), semente);
            c.escolher(irmao);
            if (adversario.equals(c.adversarioPorto().id)) return c;
        }
        throw new AssertionError("nenhuma semente com " + adversario + " no porto");
    }

    @Test
    public void ep03TemAsPaginasDoAdversarioDoPorto() throws IOException {
        List<String> chegada = java.util.Arrays.asList("pag1_ryuga.jpg", "pag2_ryuga.jpg", "pag3_ryuga.jpg", "pag4_ryuga.jpg");
        List<String> p05 = paginas("ep03", campanhaComPorto(Torneio.RYUGA, "p05"));
        assertEquals(chegada, p05.subList(0, 4));
        assertEquals(java.util.Arrays.asList("pag5_ryuga_p05.jpg", "LUTA"), p05.subList(4, 6));
        assertFalse(p05.contains("pag3_q1.jpg"));
        // Sem páginas prontas para o adversário: o desafio provisório.
        List<String> p07 = paginas("ep03", campanhaComPorto(Torneio.RYUGA, "p07"));
        assertEquals(chegada, p07.subList(0, 4));
        assertEquals(java.util.Arrays.asList("pag3_q1.jpg", "LUTA"), p07.subList(4, 6));
        assertFalse(p07.contains("pag5_ryuga_p05.jpg"));
        // As páginas do Ryūga não aparecem para o Kōga, nem contra o p05.
        List<String> koga = paginas("ep03", campanhaComPorto(Torneio.KOGA, "p05"));
        assertFalse(koga.contains("pag5_ryuga_p05.jpg"));
        assertEquals(java.util.Arrays.asList("pag1_q1.jpg", "pag2_koga.jpg", "pag3_q1.jpg", "LUTA"), koga.subList(0, 4));
    }

    /**
     * Como o CONTINUAR: cada episódio começa com a campanha refeita pelo save
     * ({@link ProgressoHistoria#campanhaNoInicio}), não com a do episódio anterior.
     */
    @Test
    public void temporadaRetomadaPeloSaveACadaEpisodioTemAEmboscada() throws IOException {
        GameAssets assets = new GameAssets();
        Temporada temporada = Temporada.load(assets);
        ProgressoHistoria progresso = new ProgressoHistoria(3L);
        Recorder rec = new Recorder();
        PaginaHistoria p = new PaginaHistoria(rec, assets);
        List<String> rotulos = new ArrayList<>();
        for (int ep = 0; ep < temporada.episodios.size(); ep++) {
            // Grava e relê o save, como o app entre uma sessão e outra.
            progresso = ProgressoHistoria.fromJson(progresso.toJson(), 99L);
            p.setCampanha(progresso.campanhaNoInicio(ep, duplas()));
            progresso.comecou(ep, p.campanha());
            rec.ended = null;
            p.start(temporada, temporada.loadEpisodio(assets, temporada.episodios.get(ep)));
            for (int i = 0; i < 20000 && rec.ended == null; i++) {
                if (p.waitingFight()) {
                    rotulos.add(p.rotulo(p.passo()));
                    p.resultadoLuta(true);
                    continue;
                }
                if (p.passo().tipo == Episodio.TipoPasso.ESCOLHA) {
                    run(p, 0.5f);
                    p.moverEscolha(-1);
                    p.confirmarEscolha();
                    run(p, 1f);
                    continue;
                }
                run(p, 0.6f);
                p.tap(640f, 360f);
            }
            assertNotNull(temporada.episodios.get(ep) + " não terminou", rec.ended);
            progresso.terminou(ep, p.campanha());
        }
        rotulos.remove(0); // o treino do Prólogo
        assertEquals(java.util.Arrays.asList("TREINO", "CONVITE", "DESAFIO", Torneio.QUARTAS, Campanha.EMBOSCADA,
            Torneio.SEMIFINAL, Torneio.FINAL, Campanha.DUELO), rotulos);
    }

    /** As lutas da temporada a partir do CONTINUAR de um save, com o rótulo de cada uma. */
    private static List<String> lutasDoSave(String json) throws IOException {
        GameAssets assets = new GameAssets();
        Temporada temporada = Temporada.load(assets);
        ProgressoHistoria progresso = ProgressoHistoria.fromJson(json, 9L);
        Recorder rec = new Recorder();
        PaginaHistoria p = new PaginaHistoria(rec, assets);
        p.setCampanha(progresso.campanhaNoInicio(progresso.atual(), duplas()));
        List<String> lutas = new ArrayList<>();
        for (int ep = progresso.atual(); ep < temporada.episodios.size(); ep++) {
            rec.ended = null;
            p.start(temporada, temporada.loadEpisodio(assets, temporada.episodios.get(ep)));
            for (int i = 0; i < 20000 && rec.ended == null; i++) {
                if (p.waitingFight()) {
                    lutas.add(temporada.episodios.get(ep) + " " + p.rotulo(p.passo()));
                    p.resultadoLuta(true);
                    continue;
                }
                run(p, 0.6f);
                p.tap(640f, 360f);
            }
            assertNotNull(temporada.episodios.get(ep) + " não terminou", rec.ended);
        }
        return lutas;
    }

    @Test
    public void saveDeAntesDoParceiroContinuaNoMesmoPontoDoTorneio() throws IOException {
        // Versão 1: terminou o antigo ep02 (O Sorteio, com as quartas) e ia para Fora da Arena.
        List<String> lutas = lutasDoSave("{\"versao\":1,\"semente\":\"3\",\"atual\":3,\"concluidos\":3,\"fotos\":{"
            + "\"2\":{\"irmao\":\"ryuga\",\"disputadas\":-1},\"3\":{\"irmao\":\"ryuga\",\"disputadas\":1}}}");
        assertEquals(java.util.Arrays.asList("ep05 " + Campanha.EMBOSCADA, "ep06 " + Torneio.SEMIFINAL,
            "ep07 " + Torneio.FINAL, "ep08 " + Campanha.DUELO), lutas);
    }

    @Test
    public void saveDeAntesDoParceiroParadoNoSorteioJogaOParceiro() throws IOException {
        // Versão 1: terminou o ep01 e ia para o antigo ep02 (O Sorteio).
        ProgressoHistoria p = ProgressoHistoria.fromJson("{\"versao\":1,\"semente\":\"3\",\"atual\":2,\"concluidos\":2,"
            + "\"fotos\":{\"2\":{\"irmao\":\"koga\",\"disputadas\":-1}}}", 9L);
        assertEquals(2, p.atual());
        assertEquals(Torneio.KOGA, p.campanhaNoInicio(2, duplas()).irmaoEscolhido());
        assertEquals(Torneio.KOGA, p.campanhaNoInicio(3, duplas()).irmaoEscolhido());
        // Gravado de novo, é da versão atual e não anda mais.
        assertEquals(2, ProgressoHistoria.fromJson(p.toJson(), 9L).atual());
        assertEquals(java.util.Arrays.asList("ep02 CONVITE", "ep03 DESAFIO", "ep04 " + Torneio.QUARTAS,
            "ep05 " + Campanha.EMBOSCADA, "ep06 " + Torneio.SEMIFINAL, "ep07 " + Torneio.FINAL, "ep08 " + Campanha.DUELO),
            lutasDoSave(p.toJson()));
    }

    @Test
    public void saveDeAntesDaChegadaParadoNoSorteioJogaAChegada() throws IOException {
        // Versão 2: terminou O Parceiro (ep02) e ia para O Sorteio, que era o ep03.
        ProgressoHistoria p = ProgressoHistoria.fromJson("{\"versao\":2,\"semente\":\"3\",\"atual\":3,\"concluidos\":3,"
            + "\"fotos\":{\"3\":{\"irmao\":\"ryuga\",\"disputadas\":-1}}}", 9L);
        assertEquals(3, p.atual());
        assertEquals(3, p.concluidos());
        assertEquals(Torneio.RYUGA, p.campanhaNoInicio(3, duplas()).irmaoEscolhido());
        assertEquals(Torneio.RYUGA, p.campanhaNoInicio(4, duplas()).irmaoEscolhido());
        assertEquals(java.util.Arrays.asList("ep03 DESAFIO", "ep04 " + Torneio.QUARTAS, "ep05 " + Campanha.EMBOSCADA,
            "ep06 " + Torneio.SEMIFINAL, "ep07 " + Torneio.FINAL, "ep08 " + Campanha.DUELO), lutasDoSave(p.toJson()));
    }

    @Test
    public void saveDeAntesDaChegadaContinuaNoMesmoPontoDoTorneio() throws IOException {
        // Versão 2: terminou O Sorteio (ep03, com as quartas) e ia para Fora da Arena (ep04).
        List<String> lutas = lutasDoSave("{\"versao\":2,\"semente\":\"3\",\"atual\":4,\"concluidos\":4,\"fotos\":{"
            + "\"3\":{\"irmao\":\"koga\",\"disputadas\":-1},\"4\":{\"irmao\":\"koga\",\"disputadas\":1}}}");
        assertEquals(java.util.Arrays.asList("ep05 " + Campanha.EMBOSCADA, "ep06 " + Torneio.SEMIFINAL,
            "ep07 " + Torneio.FINAL, "ep08 " + Campanha.DUELO), lutas);
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
        p.start(temporada, temporada.loadEpisodio(assets, "ep04"));
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
