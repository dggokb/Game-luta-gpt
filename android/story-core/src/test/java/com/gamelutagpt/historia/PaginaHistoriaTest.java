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
                if (passo.tipo == Episodio.TipoPasso.LUTA) {
                    assertNotNull(id + ": luta sem dupla", passo.dupla);
                    assertNotNull(id + ": luta sem cpu", passo.cpu);
                    for (String fighter : new String[] {passo.dupla[0], passo.dupla[1], passo.cpu[0], passo.cpu[1]}) {
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

    /** Toda tradução que vai no jogo abre, e cobre todos os textos do Prólogo. */
    @Test
    public void traducoesCobremOPrologo() throws IOException {
        Episodio ep = Episodio.fromJson(read("historia/ep00/episodio.json"));
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
        File[] idiomas = new File(ASSETS, "historia/idiomas").listFiles();
        assertNotNull(idiomas);
        for (File arquivo : idiomas) {
            java.util.Map<String, String> traducao = Temporada.traducaoFromJson(read("historia/idiomas/" + arquivo.getName()));
            for (String texto : textos) {
                assertTrue(arquivo.getName() + " sem tradução para: " + texto, traducao.containsKey(texto));
            }
        }
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
