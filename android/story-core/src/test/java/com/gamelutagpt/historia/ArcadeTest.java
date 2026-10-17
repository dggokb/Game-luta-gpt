package com.gamelutagpt.historia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.Test;

/**
 * O modo Arcade: a chave de 16 com a dupla oficial ou livre do jogador, as 4 rodadas, o duelo
 * 1x1 da dupla campeã e o cartão do Takatoshi; a CPU sobe de nível a cada rodada.
 */
public class ArcadeTest {
    private static final float DT = 1f / 60f;
    private static final File ASSETS = new File("../app/src/main/assets");
    /** Os lutadores com golpes hoje (os da seleção). */
    private static final List<String> LUTADORES = Arrays.asList("player_base", "p02", "p03", "p04", "p05", "p06",
        "p07", "p08", "p09", "p10", "p11", "p12");

    private static final class GameAssets implements RenderAssets {
        @Override public String readText(String path) throws IOException {
            return new String(Files.readAllBytes(new File(ASSETS, path).toPath()), StandardCharsets.UTF_8);
        }
        @Override public RenderImage loadImage(String path) throws IOException { throw new IOException("sem " + path); }
    }

    private static final class Recorder implements HistoriaListener {
        final List<Episodio.Passo> fights = new ArrayList<>();
        Episodio ended;

        @Override public void onHistoriaLuta(Episodio.Passo luta) { fights.add(luta); }
        @Override public void onHistoriaSom(String id) {}
        @Override public void onHistoriaFim(Episodio episodio) { ended = episodio; }
    }

    private static List<Torneio.Dupla> oficiais() throws IOException {
        return Arcade.oficiais(Torneio.duplasFromJson(new GameAssets().readText(Torneio.ARQUIVO)));
    }

    private static String par(Torneio.Dupla d) {
        String a = d.lutadores[0], b = d.lutadores[1];
        return a.compareTo(b) <= 0 ? a + "+" + b : b + "+" + a;
    }

    @Test
    public void oficiaisSaoAsDuplasQueJaLutam() throws IOException {
        List<Torneio.Dupla> oficiais = oficiais();
        assertEquals("as 6 prontas (p13–p16 ainda sem golpes)", 6, oficiais.size());
        for (Torneio.Dupla d : oficiais) {
            assertTrue(d.pronta);
            assertTrue(LUTADORES.containsAll(Arrays.asList(d.lutadores)));
        }
    }

    @Test
    public void chaveComDuplaOficialTemDezesseisDuplasSemRepetir() throws IOException {
        List<Torneio.Dupla> oficiais = oficiais();
        Torneio.Dupla jogador = oficiais.get(0);
        for (long semente = 0; semente < 40; semente++) {
            Torneio t = Arcade.chave(jogador, oficiais, LUTADORES, new Random(semente));
            assertEquals(16, t.vagas());
            assertEquals(4, t.rodadas());
            assertSame("o jogador na vaga 0", jogador, t.participante(0, 0));
            assertNull("sem rival fixo", t.rival());
            Set<String> pares = new HashSet<>();
            Set<Torneio.Dupla> vistas = new HashSet<>();
            for (int v = 0; v < 16; v++) {
                Torneio.Dupla d = t.participante(0, v);
                assertNotNull(d);
                assertTrue("dupla em duas vagas", vistas.add(d));
                assertTrue("dupla repetida: " + par(d), pares.add(par(d)));
                assertFalse("o mesmo lutador duas vezes na dupla", d.lutadores[0].equals(d.lutadores[1]));
                assertTrue("lutador sem golpes: " + d.id, LUTADORES.containsAll(Arrays.asList(d.lutadores)));
            }
            // As outras 5 oficiais estão na chave.
            for (Torneio.Dupla o : oficiais) assertTrue(o.id, vistas.contains(o));
            // 32 lugares para 12 lutadores, espalhados por igual (2 ou 3 duplas cada).
            java.util.Map<String, Integer> vezes = new java.util.HashMap<>();
            for (Torneio.Dupla d : vistas) for (String l : d.lutadores) vezes.merge(l, 1, Integer::sum);
            assertEquals(LUTADORES.size(), vezes.size());
            int min = java.util.Collections.min(vezes.values()), max = java.util.Collections.max(vezes.values());
            assertTrue("um lutador em " + max + " duplas e outro em " + min, max - min <= 1);
        }
    }

    @Test
    public void duplaLivreEntraComTodasAsOficiaisContra() throws IOException {
        List<Torneio.Dupla> oficiais = oficiais();
        Torneio.Dupla livre = Arcade.livre("p05", "p12");
        assertEquals("p05", livre.lutadores[0]);
        assertEquals("p12", livre.lutadores[1]);
        Torneio t = Arcade.chave(livre, oficiais, LUTADORES, new Random(7));
        assertSame(livre, t.jogador());
        int oficiaisNaChave = 0;
        for (int v = 0; v < 16; v++) if (oficiais.contains(t.participante(0, v))) oficiaisNaChave++;
        assertEquals(6, oficiaisNaChave);
        // A dupla livre de quem joga igual a uma oficial: a oficial não entra de novo.
        Torneio.Dupla igual = Arcade.livre("p02", "player_base");
        Torneio u = Arcade.chave(igual, oficiais, LUTADORES, new Random(7));
        Set<String> pares = new HashSet<>();
        for (int v = 0; v < 16; v++) assertTrue(pares.add(par(u.participante(0, v))));
    }

    @Test
    public void quatroRodadasDasOitavasAteOCampeao() throws IOException {
        List<Torneio.Dupla> oficiais = oficiais();
        Torneio t = Arcade.chave(oficiais.get(1), oficiais, LUTADORES, new Random(3));
        String[] nomes = {Torneio.OITAVAS, Torneio.QUARTAS, Torneio.SEMIFINAL, Torneio.FINAL};
        for (int r = 0; r < 4; r++) {
            assertEquals(nomes[r], t.rodadaAtual());
            assertNotNull(t.adversario());
            assertNull("o Arcade não tem emboscada", t.emboscada());
            t.registrar(true);
            assertEquals(16 >> (r + 1), countDecided(t, r + 1));
        }
        assertTrue(t.acabou());
        assertSame(t.jogador(), t.campeao());
        assertNull(t.adversario());
    }

    private static int countDecided(Torneio t, int rodada) {
        int n = 0;
        for (int i = 0; i < (t.vagas() >> rodada); i++) if (t.participante(rodada, i) != null) n++;
        return n;
    }

    @Test
    public void historiaContinuaComOitoVagasEOsMesmosNomes() throws IOException {
        Campanha c = Campanha.load(new GameAssets(), 5L);
        c.escolher(Torneio.RYUGA);
        Torneio t = c.torneio();
        assertEquals(8, t.vagas());
        assertEquals(3, t.rodadas());
        assertEquals(Torneio.QUARTAS, t.rodadaAtual());
        assertFalse(c.arcade());
    }

    @Test
    public void campanhaDoArcadeDaAsDuplasDasLutasEODuelo() throws IOException {
        List<Torneio.Dupla> oficiais = oficiais();
        Torneio.Dupla jogador = Arcade.livre("p07", "p03");
        Campanha c = Campanha.arcade(Arcade.chave(jogador, oficiais, LUTADORES, new Random(11)));
        assertTrue(c.arcade());
        Episodio ep = Episodio.fromJson(new GameAssets().readText(Arcade.EPISODIO));
        Episodio.Passo luta = null, duelo = null;
        for (Episodio.Passo p : ep.passos) {
            if (p.torneio && luta == null) luta = p;
            if (p.duelo) duelo = p;
        }
        assertNotNull(luta);
        assertNotNull(duelo);
        String[][] lados = c.lados(luta);
        assertEquals(Arrays.asList("p07", "p03"), Arrays.asList(lados[0]));
        assertEquals(Torneio.OITAVAS, c.rotulo(luta));
        assertFalse("o duelo só depois do título", c.vale(duelo));
        for (int r = 0; r < 4; r++) c.torneio().registrar(true);
        assertTrue(c.vale(duelo));
        String[][] duelLados = c.lados(duelo);
        assertEquals("sem registro: o titular contra o parceiro", "p07", duelLados[0][0]);
        assertEquals("p03", duelLados[1][0]);
        assertEquals(Campanha.DUELO, c.rotulo(duelo));
        // A final foi vencida com o parceiro em campo: o duelo é com ele.
        c.venceuCom("p03");
        duelLados = c.lados(duelo);
        assertEquals("quem venceu a final joga o duelo", "p03", duelLados[0][0]);
        assertEquals("contra o outro da dupla", "p07", duelLados[1][0]);
        c.venceuCom("p07");
        assertEquals("p07", c.lados(duelo)[0][0]);
        assertEquals("p03", c.lados(duelo)[1][0]);
        // Lutador de fora da dupla (não deve acontecer): volta ao titular.
        c.venceuCom("p99");
        assertEquals("p07", c.lados(duelo)[0][0]);
    }

    @Test
    public void episodioTocaSorteioQuatroRodadasDueloETakatoshi() throws IOException {
        Temporada temporada = Temporada.load(new GameAssets());
        List<Torneio.Dupla> oficiais = oficiais();
        Campanha c = Campanha.arcade(Arcade.chave(oficiais.get(0), oficiais, LUTADORES, new Random(2)));
        Recorder rec = new Recorder();
        PaginaHistoria p = new PaginaHistoria(rec, new GameAssets());
        p.setCampanha(c);
        p.start(temporada, Episodio.fromJson(new GameAssets().readText(Arcade.EPISODIO)));
        PaginaHistoriaTest.NullCanvas canvas = new PaginaHistoriaTest.NullCanvas();
        List<Episodio.TipoPasso> vistos = new ArrayList<>();
        int derrotas = 0;
        for (int guard = 0; guard < 20000 && p.isActive(); guard++) {
            if (p.waitingFight()) {
                // Uma derrota no meio: a luta se repete (o "continuar" do Arcade).
                if (rec.fights.size() == 2 && derrotas == 0) {
                    derrotas++;
                    p.resultadoLuta(false);
                } else {
                    p.resultadoLuta(true);
                }
                continue;
            }
            Episodio.TipoPasso tipo = p.passo().tipo;
            if (vistos.isEmpty() || vistos.get(vistos.size() - 1) != tipo) vistos.add(tipo);
            p.render(canvas);
            p.update(DT);
            if (guard % 30 == 0) p.tap(640f, 360f);
        }
        assertFalse("o episódio acabou", p.isActive());
        assertNotNull(rec.ended);
        assertEquals("4 rodadas + a repetida + o duelo", 6, rec.fights.size());
        assertTrue(rec.fights.get(5).duelo);
        assertTrue(c.torneio().acabou());
        assertSame(c.torneio().jogador(), c.torneio().campeao());
        assertEquals(Episodio.TipoPasso.SORTEIO, vistos.get(1));
        assertEquals("o cartão do Takatoshi por último", Episodio.TipoPasso.TITULO, vistos.get(vistos.size() - 1));
    }

    @Test
    public void cpuSobeUmNivelPorRodada() {
        assertEquals(3, Arcade.nivelCpu(5, 0));
        assertEquals(6, Arcade.nivelCpu(5, 3));
        assertEquals("o duelo", 7, Arcade.nivelCpu(5, Arcade.DUELO));
        assertEquals("nunca abaixo de 1", 1, Arcade.nivelCpu(1, 0));
        assertEquals("nunca acima de 8", 8, Arcade.nivelCpu(8, Arcade.DUELO));
    }

    @Test
    public void chaveDeDezesseisCabeNaTelaSemCardsSobrepostos() throws IOException {
        List<Torneio.Dupla> oficiais = oficiais();
        Torneio t = Arcade.chave(oficiais.get(0), oficiais, LUTADORES, new Random(1));
        ChaveTorneio.Layout l = ChaveTorneio.layout(t);
        List<float[]> cards = new ArrayList<>();
        for (int r = 0; r <= t.rodadas(); r++) {
            int n = r == t.rodadas() ? 1 : t.vagas() >> r;
            for (int i = 0; i < n; i++) {
                float[] p = ChaveTorneio.posicao(t, r, i);
                float[] card = {p[0], p[1], p[0] + l.cardW, p[1] + l.cardH};
                assertTrue("dentro da tela", card[0] >= 0f && card[2] <= ChaveTorneio.VW && card[1] >= 96f && card[3] <= l.rodapeY - 20f);
                for (float[] o : cards) {
                    boolean separados = card[2] <= o[0] || o[2] <= card[0] || card[3] <= o[1] || o[3] <= card[1];
                    assertTrue("cards sobrepostos na rodada " + r, separados);
                }
                cards.add(card);
            }
        }
        // A sorte do Arcade cabe em poucos segundos mesmo com 15 duplas.
        assertTrue(ChaveTorneio.duracaoSorteio(t) < 4.5f);
    }

    @Test
    public void menuArcadeEscolheOficialLivreOuVolta() throws IOException {
        List<Torneio.Dupla> oficiais = oficiais();
        MenuArcade m = new MenuArcade(new GameAssets());
        m.abrir(null, oficiais);
        assertEquals(0, m.cursor());
        assertNull("cedo demais", m.confirmar());
        m.update(MenuArcade.TAP_DELAY + 0.05f);
        m.render(new PaginaHistoriaTest.NullCanvas());
        // A dupla livre é o último card: ▶ até o fim da primeira linha, ▼ e ▶ até ela.
        for (int i = 0; i < 6; i++) m.mover(1, 0);
        m.mover(0, 1);
        for (int i = 0; i < 6; i++) m.mover(1, 0);
        assertEquals(MenuArcade.LIVRE, m.cursor());
        assertEquals(Integer.valueOf(MenuArcade.LIVRE), m.confirmar());
        assertFalse(m.isOpen());

        m.abrir(null, oficiais);
        m.update(MenuArcade.TAP_DELAY + 0.05f);
        float[] b = m.caixa(2);
        assertEquals(Integer.valueOf(2), m.tap((b[0] + b[2]) * 0.5f, (b[1] + b[3]) * 0.5f));
        assertSame(oficiais.get(2), m.oficial(2));

        m.abrir(null, oficiais);
        assertEquals(Integer.valueOf(MenuArcade.VOLTAR), m.voltar());
    }
}
