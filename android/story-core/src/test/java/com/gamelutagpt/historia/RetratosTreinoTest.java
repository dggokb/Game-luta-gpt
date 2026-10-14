package com.gamelutagpt.historia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import org.junit.Test;

/** Takatoshi e Dafu falando no treino: o arquivo do jogo vale e as falas andam sozinhas. */
public class RetratosTreinoTest {
    private static final File ASSETS = new File("../app/src/main/assets");
    private static final float DT = 1f / 60f;

    private static final RenderAssets FILES = new RenderAssets() {
        @Override public String readText(String path) throws IOException {
            return new String(Files.readAllBytes(new File(ASSETS, path).toPath()), StandardCharsets.UTF_8);
        }
        @Override public RenderImage loadImage(String path) throws IOException { throw new IOException(path); }
    };

    private static RetratosTreino carregado() {
        RetratosTreino r = new RetratosTreino(FILES);
        r.load();
        return r;
    }

    /** Toda lição do treino tem fala, quem fala está no elenco e tem o rosto "normal". */
    @Test
    public void falasDoJogoValem() throws IOException {
        Temporada temporada = Temporada.load(FILES);
        RetratosTreino r = carregado();
        String[] licoes = {"andar", "agachar", "pular", "super-pulo", "dash", "recuo", "golpe-fraco",
            "golpe-medio", "golpe-forte", "fraco-agachado", "medio-agachado", "forte-agachado", "golpe-no-ar",
            "cravada", "combo", "botao-combo", "defesa", "defesa-baixa", "especial-de-energia", "especial-s2",
            "especial-s3", "especial-s4", "agarrao", "super", "ultra", "fim"};
        for (String id : licoes) assertTrue("lição sem fala: " + id, r.has(id));
        Map<String, Object> root = Episodio.asMap(com.gamelutagpt.render.MiniJson.parse(FILES.readText(RetratosTreino.ARQUIVO)));
        for (String quem : Episodio.asMap(root.get("retratos")).keySet()) {
            assertTrue("fora do elenco: " + quem, temporada.elenco.containsKey(quem));
            assertTrue("sem o rosto normal: " + quem,
                new File(ASSETS, RetratosTreino.PASTA + "/retratos/" + quem + "_normal.png").isFile());
        }
        // Toda fala tem tradução em todos os idiomas do jogo.
        File[] idiomas = new File(ASSETS, Temporada.PASTA + "/idiomas").listFiles();
        assertNotNull(idiomas);
        for (File arquivo : idiomas) {
            Map<String, String> traducao = Temporada.traducaoFromJson(
                FILES.readText(Temporada.PASTA + "/idiomas/" + arquivo.getName()));
            for (String grupo : new String[] {"licoes", "reacoes"}) {
                for (Object falas : Episodio.asMap(root.get(grupo)).values()) {
                    for (Object fala : (java.util.List<?>)falas) {
                        String texto = Episodio.string(Episodio.asMap(fala), "texto", "");
                        assertTrue(arquivo.getName() + " sem tradução para: " + texto, traducao.containsKey(texto));
                    }
                }
            }
        }
    }

    @Test
    public void falaEntraDigitaESaiSemToque() throws IOException {
        Temporada temporada = Temporada.load(FILES);
        RetratosTreino r = carregado();
        PaginaHistoriaTest.NullCanvas canvas = new PaginaHistoriaTest.NullCanvas();
        r.licao(temporada, "combo");
        assertEquals("takatoshi", r.atual().quem);
        int frames = 0;
        boolean dafuFalou = false;
        while (r.busy() && frames++ < 60 * 30) {
            r.update(DT);
            r.render(canvas);
            if (r.atual() != null && "dafu".equals(r.atual().quem)) dafuFalou = true;
        }
        assertTrue("a segunda fala vem sozinha", dafuFalou);
        assertFalse("os quadros saem sozinhos", r.busy());
        assertTrue("dá tempo de ler", frames > 60 * 5);
    }

    @Test
    public void licaoNovaTomaOLugarEReacaoNaoAtropela() throws IOException {
        Temporada temporada = Temporada.load(FILES);
        RetratosTreino r = carregado();
        r.licao(temporada, "andar");
        r.reacao(temporada, "impacto");
        assertEquals("a reação não corta a explicação", "takatoshi", r.atual().quem);
        r.licao(temporada, "agachar");
        assertEquals("dafu", r.atual().quem);

        // Reação curta ("Bom."): a fala da próxima lição espera ela terminar.
        r.stop();
        r.reacao(temporada, "licao");
        RetratosTreino.Fala reacao = r.atual();
        assertNotNull(reacao);
        r.licao(temporada, "pular");
        assertEquals(reacao, r.atual());
        int frames = 0;
        while (r.atual() == reacao && frames++ < 60 * 10) r.update(DT);
        assertEquals("Agora pule. Leve, como se o chão empurrasse você.", r.atual().texto);
        assertTrue(r.falando());
    }

    @Test
    public void reacaoNaoRepeteASeguida() throws IOException {
        Temporada temporada = Temporada.load(FILES);
        RetratosTreino r = carregado();
        String antes = null;
        for (int i = 0; i < 12; i++) {
            r.stop();
            for (int k = 0; k < i; k++) r.update(DT);
            r.reacao(temporada, "levou");
            String texto = r.atual().texto;
            assertNotEquals(antes, texto);
            antes = texto;
        }
    }

    @Test
    public void arquivoQuebradoAvisa() {
        RetratosTreino r = new RetratosTreino(FILES);
        String base = "{\"retratos\":{\"dafu\":{\"lado\":\"direita\",\"expressoes\":[\"rindo\"]}},\"licoes\":{\"x\":[";
        r.fromJson(base + "{\"quem\":\"dafu\",\"expressao\":\"rindo\",\"texto\":\"oi\"}]}}");
        assertTrue(r.has("x"));
        for (String ruim : new String[] {
            "{\"quem\":\"koga\",\"texto\":\"oi\"}",
            "{\"quem\":\"dafu\",\"expressao\":\"bravo\",\"texto\":\"oi\"}",
            "{\"quem\":\"dafu\",\"texto\":\"\"}"}) {
            try {
                r.fromJson(base + ruim + "]}}");
                throw new AssertionError("devia recusar: " + ruim);
            } catch (IllegalArgumentException expected) {
                // ok
            }
        }
    }

    /** Sem o arquivo ninguém fala e o treino segue igual. */
    @Test
    public void semArquivoNinguemFala() throws IOException {
        RetratosTreino r = new RetratosTreino(new RenderAssets() {
            @Override public String readText(String path) throws IOException { throw new IOException(path); }
            @Override public RenderImage loadImage(String path) throws IOException { throw new IOException(path); }
        });
        r.load();
        r.licao(Temporada.load(FILES), "andar");
        assertFalse(r.falando());
        assertFalse(r.busy());
    }
}
