package com.gamelutagpt.historia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class PaginaHistoriaTest {
    private static final float DT = 1f / 60f;
    private static final File ASSETS = new File("../app/src/main/assets");

    private static final String TEMPORADA = "{\"titulo\":\"T\",\"elenco\":{\"a\":{\"nome\":\"A\",\"cor\":\"#FF0000\"}}," +
        "\"episodios\":[\"e1\"]}";
    private static final String EPISODIO = "{\"id\":\"e1\",\"numero\":1,\"titulo\":\"Teste\",\"passos\":[" +
        "{\"tipo\":\"titulo\"}," +
        "{\"tipo\":\"pagina\",\"paineis\":[" +
        "  {\"imagem\":\"x.jpg\",\"personagens\":[\"a\"],\"baloes\":[" +
        "    {\"tipo\":\"fala\",\"quem\":\"a\",\"texto\":\"Oi\",\"pos\":[0.5,0.2],\"rabo\":[0.5,0.5]}," +
        "    {\"tipo\":\"grito\",\"quem\":\"a\",\"texto\":\"HA!\"}]}," +
        "  {\"cena\":\"dojo\"}]}," +
        "{\"tipo\":\"luta\",\"dupla\":[\"p1\",\"p2\"],\"cpu\":[\"p3\",\"p4\"]}," +
        "{\"tipo\":\"proximo\",\"titulo\":\"Depois\",\"linhas\":[\"...\"]}]}";

    private static final class Recorder implements HistoriaListener {
        final List<Episodio.Passo> fights = new ArrayList<>();
        final List<String> sounds = new ArrayList<>();
        Episodio ended;

        @Override public void onHistoriaLuta(Episodio.Passo luta) { fights.add(luta); }
        @Override public void onHistoriaSom(String id) { sounds.add(id); }
        @Override public void onHistoriaFim(Episodio episodio) { ended = episodio; }
    }

    private static final class NoImages implements RenderAssets {
        @Override public String readText(String path) throws IOException { throw new IOException(path); }
        @Override public RenderImage loadImage(String path) throws IOException { throw new IOException("sem " + path); }
    }

    private static void run(PaginaHistoria p, float seconds) {
        for (float t = 0f; t < seconds; t += DT) p.update(DT);
    }

    @Test
    public void lutaUmContraUmComUmLutadorDeCadaLado() {
        Episodio e = Episodio.fromJson("{\"id\":\"x\",\"numero\":1,\"titulo\":\"X\",\"passos\":[" +
            "{\"tipo\":\"luta\",\"dupla\":[\"player_base\"],\"cpu\":[\"p02\"]}]}");
        assertEquals(1, e.passos.get(0).dupla.length);
        assertEquals("p02", e.passos.get(0).cpu[0]);
        for (String lados : new String[] {"\"dupla\":[\"a\"],\"cpu\":[\"b\",\"c\"]", "\"dupla\":\"jogador\",\"cpu\":[\"b\"]"}) {
            try {
                Episodio.fromJson("{\"id\":\"x\",\"numero\":1,\"titulo\":\"X\",\"passos\":[{\"tipo\":\"luta\"," + lados + "}]}");
                fail("1x1 de um lado só: " + lados);
            } catch (IllegalArgumentException esperado) {
                // os dois lados precisam ter o mesmo tamanho
            }
        }
    }

    @Test
    public void tocaPaginaLutaEProximo() {
        Recorder rec = new Recorder();
        PaginaHistoria p = new PaginaHistoria(rec, new NoImages());
        p.start(Temporada.fromJson(TEMPORADA), Episodio.fromJson(EPISODIO));
        assertEquals(Episodio.TipoPasso.TITULO, p.passo().tipo);

        run(p, PaginaHistoria.TITLE_DURATION + PaginaHistoria.EXIT_DURATION + 0.05f);
        assertEquals(Episodio.TipoPasso.PAGINA, p.passo().tipo);
        // O primeiro balão entra sozinho; o resto é no toque.
        run(p, 3f);
        assertFalse(p.pageComplete());
        p.tap(640f, 360f); // segundo balão
        run(p, 3f);
        p.tap(640f, 360f); // segundo painel
        run(p, 1f);
        assertTrue(p.pageComplete());
        p.tap(640f, 360f);
        run(p, 1f);

        assertTrue(p.waitingFight());
        assertFalse(p.coversScreen());
        assertEquals(1, rec.fights.size());
        assertEquals("p3", rec.fights.get(0).cpu[0]);

        p.resultadoLuta(false); // repetirSeDerrota é o padrão
        assertEquals(2, rec.fights.size());
        p.resultadoLuta(true);
        assertEquals(Episodio.TipoPasso.PROXIMO, p.passo().tipo);

        p.tap(640f, 360f); // cedo demais: ignorado
        run(p, 1f);
        p.tap(640f, 360f);
        run(p, 1f);
        assertFalse(p.isActive());
        assertNotNull(rec.ended);
        assertFalse(p.warnings().isEmpty());
        assertTrue(rec.sounds.contains("painel"));
    }

    @Test
    public void toqueDuranteADigitacaoMostraOTextoInteiro() {
        PaginaHistoria p = new PaginaHistoria(new Recorder(), new NoImages());
        p.start(Temporada.fromJson(TEMPORADA), Episodio.fromJson(EPISODIO));
        p.tap(640f, 360f);
        run(p, PaginaHistoria.TITLE_MIN_TAP + PaginaHistoria.EXIT_DURATION + 0.1f);
        p.tap(640f, 360f);
        run(p, PaginaHistoria.EXIT_DURATION + 0.05f);
        assertEquals(Episodio.TipoPasso.PAGINA, p.passo().tipo);
        run(p, PaginaHistoria.FIRST_BALLOON_DELAY + 0.02f);
        assertFalse(p.waitingTap());
        p.tap(640f, 360f);
        p.update(DT);
        assertTrue(p.waitingTap());
    }

    @Test
    public void pularVaiDiretoParaALuta() {
        Recorder rec = new Recorder();
        PaginaHistoria p = new PaginaHistoria(rec, new NoImages());
        p.start(Temporada.fromJson(TEMPORADA), Episodio.fromJson(EPISODIO));
        p.tap(1200f, 16f);
        assertTrue(p.waitingFight());
        assertEquals(1, rec.fights.size());
    }

    private static final String EPISODIO_PRONTO = "{\"id\":\"e2\",\"passos\":[" +
        "{\"tipo\":\"pagina\",\"imagem\":\"pag1.jpg\",\"quadros\":[" +
        "  {\"area\":[0,0,1,0,1,0.5,0,0.5],\"legenda\":\"Tóquio.\"}," +
        "  {\"area\":[0,0.5,1,0.5,1,1,0,1],\"impacto\":true}]}," +
        "{\"tipo\":\"proximo\",\"titulo\":\"Depois\"}]}";

    @Test
    public void paginaProntaLeQuadroAQuadroEVoltaParaAPaginaInteira() {
        Recorder rec = new Recorder();
        PaginaHistoria p = new PaginaHistoria(rec, new NoImages());
        Episodio ep = Episodio.fromJson(EPISODIO_PRONTO);
        assertTrue(ep.passos.get(0).paginaPronta());
        assertEquals(2, ep.passos.get(0).quadros.size());
        assertTrue(ep.passos.get(0).quadros.get(1).impacto);

        p.start(Temporada.fromJson(TEMPORADA), ep);
        RenderCanvas canvas = new NullCanvas();
        // A página inteira aparece e a câmera entra sozinha no primeiro quadro.
        assertFalse(p.waitingTap());
        run(p, PaginaGuiada.OVERVIEW_HOLD + PaginaGuiada.MOVE + 0.1f);
        p.render(canvas);
        assertTrue(p.waitingTap());
        assertFalse(p.pageComplete());
        p.tap(640f, 360f);
        run(p, PaginaGuiada.MOVE + 0.1f);
        assertTrue(p.pageComplete());
        // Depois do último quadro a página sai direto (sem voltar para a página inteira).
        p.tap(640f, 360f);
        run(p, PaginaHistoria.EXIT_DURATION + 0.05f);
        assertEquals(Episodio.TipoPasso.PROXIMO, p.passo().tipo);
        p.render(canvas);
        assertTrue(rec.sounds.contains("painel"));
    }

    private static final String EPISODIO_BALOES = "{\"id\":\"e3\",\"passos\":[" +
        "{\"tipo\":\"pagina\",\"imagem\":\"pag1.jpg\",\"quadros\":[" +
        "  {\"area\":[0,0,1,0,1,1,0,1],\"efeitos\":[\"impacto\",\"tremor\",\"velocidade\"],\"baloes\":[" +
        "    {\"texto\":\"Meninos!\",\"caixa\":[0.1,0.1,0.3,0.2],\"area\":[0.1,0.1,0.3,0.1,0.3,0.2,0.1,0.2]}," +
        "    {\"texto\":\"SIM!\",\"caixa\":[0.6,0.1,0.8,0.2],\"area\":[0.6,0.1,0.8,0.1,0.8,0.2,0.6,0.2]}]}]}]}";

    @Test
    public void baloesDaPaginaProntaAbremUmPorToqueNoIdiomaAtual() {
        Recorder rec = new Recorder();
        PaginaHistoria p = new PaginaHistoria(rec, new NoImages());
        Temporada temporada = Temporada.fromJson(TEMPORADA);
        RenderAssets traducao = new RenderAssets() {
            @Override public String readText(String path) throws IOException {
                if (!path.equals("historia/idiomas/en.json")) throw new IOException(path);
                return "{\"_sobre\":\"x\",\"Meninos!\":\"Boys!\"}";
            }
            @Override public RenderImage loadImage(String path) throws IOException { throw new IOException(path); }
        };
        assertTrue(temporada.setIdioma(traducao, "en"));
        assertEquals("Boys!", temporada.t("Meninos!"));
        assertEquals("SIM!", temporada.t("SIM!"));
        assertFalse(temporada.setIdioma(traducao, "fr"));
        assertEquals("Meninos!", temporada.t("Meninos!"));
        temporada.setIdioma(traducao, "en");

        Episodio ep = Episodio.fromJson(EPISODIO_BALOES);
        Episodio.Quadro q = ep.passos.get(0).quadros.get(0);
        assertTrue(q.impacto && q.tremor && q.velocidade && !q.zoom);
        assertEquals(2, q.baloes.size());

        p.start(temporada, ep);
        RenderCanvas canvas = new NullCanvas();
        // Chega no quadro (impacto) e o primeiro balão abre sozinho.
        run(p, PaginaGuiada.OVERVIEW_HOLD + PaginaGuiada.WHIP + PaginaGuiada.FIRST_BALLOON_DELAY + 0.05f);
        assertTrue(rec.sounds.contains("impacto"));
        assertFalse(p.waitingTap()); // digitando
        p.tap(640f, 360f);           // completa o texto
        p.update(DT);
        p.render(canvas);
        assertTrue(p.waitingTap());
        assertFalse(p.pageComplete()); // falta o segundo balão
        p.tap(640f, 360f);
        run(p, 1f);
        assertTrue(p.pageComplete());
        p.render(canvas);
    }

    @Test
    public void efeitoDesconhecidoFalha() {
        try {
            Episodio.fromJson("{\"passos\":[{\"tipo\":\"pagina\",\"imagem\":\"a.jpg\",\"quadros\":[" +
                "{\"area\":[0,0,1,0,1,1],\"efeitos\":[\"explosao\"]}]}]}");
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("explosao"));
        }
    }

    @Test
    public void paginaProntaSemQuadrosFalha() {
        try {
            Episodio.fromJson("{\"passos\":[{\"tipo\":\"pagina\",\"imagem\":\"a.jpg\"}]}");
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("sem quadros"));
        }
    }

    @Test
    public void layoutComQuantidadeErradaDePaineisFalha() {
        try {
            Episodio.fromJson("{\"passos\":[{\"tipo\":\"pagina\",\"layout\":\"splash\",\"paineis\":[{},{}]}]}");
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("splash"));
        }
    }

    @Test
    public void desenhaTodosOsPassosSemErro() {
        PaginaHistoria p = new PaginaHistoria(new Recorder(), new NoImages());
        p.start(Temporada.fromJson(TEMPORADA), Episodio.fromJson(EPISODIO));
        RenderCanvas canvas = new NullCanvas();
        for (int i = 0; i < 4000 && p.isActive(); i++) {
            p.update(DT);
            p.render(canvas);
            if (i % 40 == 0) p.tap(640f, 360f);
            if (p.waitingFight()) p.resultadoLuta(true);
        }
        assertFalse(p.isActive());
    }

    /** Valida os episódios que vão no jogo: JSON, layouts, elenco dos balões e lutadores das lutas. */
    @Test
    public void episodiosDoJogoSaoValidos() throws IOException {
        Temporada temporada = Temporada.fromJson(read("historia/temporada.json"));
        String roster = new String(Files.readAllBytes(new File("../../characters/roster.json").toPath()),
            StandardCharsets.UTF_8);
        for (String id : temporada.episodios) {
            Episodio ep = Episodio.fromJson(read("historia/" + id + "/episodio.json"));
            assertEquals(id, ep.id);
            for (Episodio.Passo passo : ep.passos) {
                if (passo.tipo == Episodio.TipoPasso.LUTA && !passo.torneio && !passo.duelo && !passo.emboscada) {
                    // Duplas da luta: fixas no passo ou a dupla fixa de um irmão ("jogador" / "rival").
                    assertTrue(id + ": luta sem dupla", passo.dupla != null || passo.duplaRef != null);
                    assertTrue(id + ": luta sem cpu", passo.cpu != null || passo.cpuRef != null);
                    List<String> fighters = new ArrayList<>();
                    if (passo.dupla != null) fighters.addAll(java.util.Arrays.asList(passo.dupla));
                    if (passo.cpu != null) fighters.addAll(java.util.Arrays.asList(passo.cpu));
                    for (String fighter : fighters) {
                        assertTrue(id + ": lutador fora do roster: " + fighter, roster.contains("\"" + fighter + "\""));
                    }
                    if (passo.cenario != null) {
                        assertTrue(id + ": cenário não existe: " + passo.cenario,
                            new File(ASSETS, "stages/" + passo.cenario + "/stage.json").isFile());
                    }
                }
                if (passo.paginaPronta()) {
                    assertTrue(id + ": imagem da página pronta não existe: " + passo.imagem,
                        new File(ASSETS, "historia/" + id + "/" + passo.imagem).isFile());
                }
                for (Episodio.Painel painel : passo.paineis) {
                    for (String who : painel.personagens) {
                        assertTrue(id + ": personagem fora do elenco: " + who, temporada.elenco.containsKey(who));
                    }
                    for (Episodio.Balao b : painel.baloes) {
                        if (b.quem != null) {
                            assertTrue(id + ": balão de alguém fora do elenco: " + b.quem, temporada.elenco.containsKey(b.quem));
                        }
                    }
                }
            }
        }
    }

    /**
     * Toda tradução que vai no jogo abre e cobre todos os textos dos episódios da temporada
     * (o modo história toca todos eles em sequência) e os do menu inicial e da pausa.
     */
    @Test
    public void traducoesCobremOsEpisodiosEOMenu() throws IOException {
        Temporada temporada = Temporada.fromJson(read("historia/temporada.json"));
        List<String> textos = new ArrayList<>();
        for (String id : temporada.episodios) {
            Episodio ep = Episodio.fromJson(read("historia/" + id + "/episodio.json"));
            textos.addAll(textosDoEpisodio(ep));
            // As falas e onomatopeias das cinemáticas das lutas também.
            for (Episodio.Passo passo : ep.passos) {
                if (passo.cinematica != null) {
                    textos.addAll(CinematicaLuta.ler(temporada, read("historia/" + passo.cinematica)).textos());
                }
            }
        }
        // O episódio do Arcade fica fora da temporada.
        textos.addAll(textosDoEpisodio(Episodio.fromJson(read(Arcade.EPISODIO))));
        textos.addAll(java.util.Arrays.asList(MenuArcade.TEXTOS));
        textos.addAll(java.util.Arrays.asList(MenuInicial.TEXTOS));
        textos.addAll(java.util.Arrays.asList(MenuPausa.TEXTOS));
        textos.addAll(java.util.Arrays.asList(TelaComandos.TEXTOS));
        textos.addAll(java.util.Arrays.asList(EscolhaIrmao.TEXTOS));
        textos.addAll(java.util.Arrays.asList(MenuCapitulos.TEXTOS));
        textos.addAll(java.util.Arrays.asList(ChaveTorneio.TEXTOS));
        textos.addAll(java.util.Arrays.asList(Torneio.NOMES_RODADAS));
        File[] idiomas = new File(ASSETS, "historia/idiomas").listFiles();
        assertNotNull(idiomas);
        for (File arquivo : idiomas) {
            java.util.Map<String, String> traducao = Temporada.traducaoFromJson(read("historia/idiomas/" + arquivo.getName()));
            for (String texto : textos) {
                assertTrue(arquivo.getName() + " sem tradução para: " + texto, traducao.containsKey(texto));
            }
        }
    }

    private static List<String> textosDoEpisodio(Episodio ep) {
        List<String> textos = new ArrayList<>();
        textos.add(ep.titulo);
        for (Episodio.Passo passo : ep.passos) {
            if (passo.titulo != null) textos.add(passo.titulo);
            textos.addAll(passo.linhas);
            for (Episodio.Quadro q : passo.quadros) {
                if (q.legenda != null) textos.add(q.legenda);
                for (Episodio.BalaoPronto b : q.baloes) textos.add(b.texto);
            }
            for (Episodio.Painel painel : passo.paineis) {
                for (Episodio.Balao b : painel.baloes) textos.add(b.texto);
            }
        }
        return textos;
    }

    /**
     * O modo história toca a temporada em ordem: o Prólogo termina no "Próximo episódio" que
     * anuncia o episódio seguinte da lista, e o jogo emenda nele.
     */
    @Test
    public void prologoTerminaAnunciandoOEpisodioSeguinte() throws IOException {
        Temporada temporada = Temporada.fromJson(read("historia/temporada.json"));
        assertEquals("ep00", temporada.episodios.get(0));
        assertTrue("a temporada precisa de um episódio depois do Prólogo", temporada.episodios.size() > 1);
        for (int i = 0; i + 1 < temporada.episodios.size(); i++) {
            Episodio ep = Episodio.fromJson(read("historia/" + temporada.episodios.get(i) + "/episodio.json"));
            Episodio seguinte = Episodio.fromJson(read("historia/" + temporada.episodios.get(i + 1) + "/episodio.json"));
            Episodio.Passo ultimo = ep.passos.get(ep.passos.size() - 1);
            assertEquals(ep.id + " termina no próximo episódio", Episodio.TipoPasso.PROXIMO, ultimo.tipo);
            assertEquals(ep.id + " anuncia o episódio seguinte", seguinte.titulo, ultimo.titulo);
            assertEquals(ep.numero + 1, seguinte.numero);
        }
    }

    /** No fim do Prólogo o toque no "Próximo episódio" termina o episódio (o jogo emenda o seguinte). */
    @Test
    public void toqueNoProximoEpisodioTerminaOEpisodio() {
        Recorder rec = new Recorder();
        PaginaHistoria p = new PaginaHistoria(rec, new NoImages());
        p.start(Temporada.fromJson(TEMPORADA), Episodio.fromJson(EPISODIO));
        p.tap((PaginaHistoria.SKIP_LEFT + PaginaHistoria.SKIP_RIGHT) * 0.5f, 10f); // PULAR até a luta
        p.resultadoLuta(true);
        assertEquals(Episodio.TipoPasso.PROXIMO, p.passo().tipo);
        run(p, PaginaHistoria.NEXT_MIN_TAP + 0.1f);
        p.tap(640f, 360f);
        run(p, PaginaHistoria.EXIT_DURATION + 0.1f);
        assertFalse(p.isActive());
        assertNotNull(rec.ended);
    }

    @Test
    public void botaoMenuSoRespondeQuandoLigado() {
        PaginaHistoria p = new PaginaHistoria(new Recorder(), new NoImages());
        p.start(Temporada.fromJson(TEMPORADA), Episodio.fromJson(EPISODIO));
        float x = (PaginaHistoria.MENU_LEFT + PaginaHistoria.MENU_RIGHT) * 0.5f;
        float y = (PaginaHistoria.MENU_TOP + PaginaHistoria.MENU_BOTTOM) * 0.5f;
        assertFalse(p.menuAt(x, y));
        p.setBotaoMenu(true);
        assertTrue(p.menuAt(x, y));
        assertFalse(p.menuAt(640f, 360f));
        p.render(new NullCanvas());
        // Durante a luta do episódio a página não está na tela: o botão é o do HUD.
        p.tap((PaginaHistoria.SKIP_LEFT + PaginaHistoria.SKIP_RIGHT) * 0.5f, 10f);
        assertTrue(p.waitingFight());
        assertFalse(p.menuAt(x, y));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(new File(ASSETS, path).toPath()), StandardCharsets.UTF_8);
    }

    /** Canvas que não desenha nada, só para exercitar o código de desenho. */
    static final class NullCanvas implements RenderCanvas {
        @Override public void save() {}
        @Override public void restore() {}
        @Override public void translate(float dx, float dy) {}
        @Override public void scale(float sx, float sy, float px, float py) {}
        @Override public void rotate(float degrees, float px, float py) {}
        @Override public void clipPolygon(float[] xy) {}
        @Override public void fillRect(float l, float t, float r, float b, int argb) {}
        @Override public void fillPolygon(float[] xy, int argb) {}
        @Override public void strokePolygon(float[] xy, float width, int argb) {}
        @Override public void fillCircle(float cx, float cy, float radius, int argb) {}
        @Override public void strokeCircle(float cx, float cy, float radius, float width, int argb) {}
        @Override public void drawLine(float x1, float y1, float x2, float y2, float width, int argb) {}
        @Override public void drawImage(RenderImage image, float l, float t, float r, float b, int alpha) {}
        @Override public void drawImageRegion(RenderImage image, float sl, float st, float sr, float sb,
                                              float l, float t, float r, float b, int alpha) {}
        @Override public void fillRadialGlow(float cx, float cy, float radius, int argbCenter) {}
        @Override public void fillVerticalGradient(float l, float t, float r, float b, int top, int bottom) {}
        @Override public void fillOval(float l, float t, float r, float b, int argb) {}
        @Override public void drawText(String text, float x, float y, float size, int fill, int stroke,
                                       float strokeWidth, boolean italic) {}
        @Override public void beginInvert() {}
        @Override public void endInvert() {}
    }
}
