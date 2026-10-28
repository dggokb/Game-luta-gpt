package com.gamelutagpt.historia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import org.junit.Test;

/** As cenas chamadas pelo nome: trechos de episódio entre marcas, e as do jogo abrem. */
public class CenasTest {
    private static final File ASSETS = new File("../app/src/main/assets");

    private static final String EPISODIO = "{\"id\":\"e1\",\"passos\":[" +
        "{\"tipo\":\"titulo\"}," +
        "{\"tipo\":\"proximo\",\"marca\":\"a\",\"titulo\":\"A\"}," +
        "{\"tipo\":\"luta\",\"marca\":\"treino\",\"modo\":\"tutorial\",\"cenario\":\"dojo\"}," +
        "{\"tipo\":\"proximo\",\"marca\":\"b\",\"titulo\":\"B\"}" +
        "]}";

    @Test
    public void trechoEntreMarcasIncluiAsDuas() {
        Episodio ep = Episodio.fromJson(EPISODIO);
        Episodio t = ep.trecho("a", "treino");
        assertEquals(2, t.passos.size());
        assertEquals("a", t.passos.get(0).marca);
        assertTrue(t.passos.get(1).tutorial());
        assertEquals("dojo", t.passos.get(1).cenario);
        assertEquals("e1", t.id);
        assertEquals(4, ep.trecho(null, null).passos.size());
        assertEquals(2, ep.trecho(null, "a").passos.size());
        assertEquals(2, ep.trecho("treino", null).passos.size());
    }

    @Test
    public void lutaSemModoNaoETutorialESemDuplaVale() {
        Episodio ep = Episodio.fromJson("{\"id\":\"e\",\"passos\":[{\"tipo\":\"luta\"}]}");
        assertTrue(!ep.passos.get(0).tutorial());
        assertNull(ep.passos.get(0).dupla);
    }

    @Test
    public void marcasErradasAvisam() {
        Episodio ep = Episodio.fromJson(EPISODIO);
        try {
            ep.trecho("nao-existe", null);
            fail();
        } catch (IllegalArgumentException expected) {}
        try {
            ep.trecho("b", "a");
            fail();
        } catch (IllegalArgumentException expected) {}
        try {
            Episodio.fromJson("{\"id\":\"e\",\"passos\":[{\"tipo\":\"titulo\",\"marca\":\"x\"},{\"tipo\":\"titulo\",\"marca\":\"x\"}]}");
            fail();
        } catch (IllegalArgumentException expected) {}
        try {
            Episodio.fromJson("{\"id\":\"e\",\"passos\":[{\"tipo\":\"luta\",\"modo\":\"outro\"}]}");
            fail();
        } catch (IllegalArgumentException expected) {}
    }

    @Test
    public void nomeForaDoArquivoEOEpisodioInteiro() {
        Cenas cenas = Cenas.fromJson("{\"_comentario\":\"x\",\"intro\":{\"episodio\":\"ep00\",\"ate\":\"intro\"}}");
        assertEquals(1, cenas.todas().size());
        assertEquals("ep00", cenas.get("intro").episodio);
        assertEquals("intro", cenas.get("intro").ate);
        Cenas.Cena ep01 = cenas.get("ep01");
        assertEquals("ep01", ep01.episodio);
        assertNull(ep01.de);
        assertNull(ep01.ate);
    }

    /** Toda cena do cenas.json abre; a intro termina na página 4 e o tutorial no treino do dojo. */
    @Test
    public void cenasDoJogoAbrem() throws IOException {
        Temporada temporada = Temporada.fromJson(read("historia/temporada.json"));
        Cenas cenas = Cenas.fromJson(read("historia/cenas.json"));
        RenderAssets assets = new RenderAssets() {
            @Override public String readText(String path) throws IOException { return read(path); }
            @Override public RenderImage loadImage(String path) throws IOException { throw new IOException(path); }
        };
        for (Map.Entry<String, Cenas.Cena> e : cenas.todas().entrySet()) {
            Episodio cena = cenas.carregar(temporada, assets, e.getKey());
            assertTrue(e.getKey() + " vazia", !cena.passos.isEmpty());
        }
        Episodio intro = cenas.carregar(temporada, assets, "intro");
        assertEquals("pag4.jpg", intro.passos.get(intro.passos.size() - 1).imagem);
        Episodio tutorial = cenas.carregar(temporada, assets, "tutorial");
        Episodio.Passo treino = tutorial.passos.get(tutorial.passos.size() - 1);
        assertTrue(treino.tutorial());
        assertEquals("dojo_shinshinkan", treino.cenario);
        // A escolha do irmão e o treino, logo depois da intro.
        assertEquals(intro.passos.size() + 2, tutorial.passos.size());
        assertEquals(Episodio.TipoPasso.ESCOLHA, tutorial.passos.get(intro.passos.size()).tipo);
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(new File(ASSETS, path).toPath()), StandardCharsets.UTF_8);
    }
}
