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

/** Conversa nos quadros laterais durante a luta: os arquivos do jogo valem e as falas andam sozinhas. */
public class ConversaLutaTest {
    private static final File ASSETS = new File("../app/src/main/assets");
    private static final float DT = 1f / 60f;
    private static final String TREINO = "ep00/conversa_treino.json";

    private static final RenderAssets FILES = new RenderAssets() {
        @Override public String readText(String path) throws IOException {
            return new String(Files.readAllBytes(new File(ASSETS, path).toPath()), StandardCharsets.UTF_8);
        }
        @Override public RenderImage loadImage(String path) throws IOException { throw new IOException(path); }
    };

    private static ConversaLuta treino(Temporada temporada) {
        ConversaLuta c = new ConversaLuta(FILES);
        assertTrue(c.carregar(temporada, TREINO));
        return c;
    }

    /** Fala até ninguém mais falar (e passa o silêncio antes de uma reação). */
    private static void esperar(ConversaLuta c) {
        int frames = 0;
        while (c.busy() && frames++ < 60 * 30) c.update(DT);
        for (int i = 0; i < 60 * 2; i++) c.update(DT);
    }

    /**
     * Toda conversa apontada por uma luta dos episódios abre, quem fala tem o rosto "normal" e
     * todo texto tem tradução em todos os idiomas do jogo.
     */
    @Test
    public void conversasDosEpisodiosValem() throws IOException {
        Temporada temporada = Temporada.load(FILES);
        File[] idiomas = new File(ASSETS, Temporada.PASTA + "/idiomas").listFiles();
        assertNotNull(idiomas);
        int conversas = 0;
        for (String id : temporada.episodios) {
            Episodio ep = Episodio.fromJson(FILES.readText(Temporada.PASTA + "/" + id + "/episodio.json"));
            for (Episodio.Passo passo : ep.passos) {
                if (passo.conversa == null) continue;
                conversas++;
                ConversaLuta c = new ConversaLuta(FILES);
                c.fromJson(temporada, FILES.readText(Temporada.PASTA + "/" + passo.conversa));
                for (String quem : c.quem()) {
                    assertTrue(passo.conversa + ": sem o rosto normal de " + quem,
                        new File(ASSETS, ConversaLuta.RETRATOS + "/" + quem + "_normal.png").isFile());
                }
                for (File arquivo : idiomas) {
                    Map<String, String> traducao = Temporada.traducaoFromJson(
                        FILES.readText(Temporada.PASTA + "/idiomas/" + arquivo.getName()));
                    for (String texto : c.textos()) {
                        assertTrue(arquivo.getName() + " sem tradução para: " + texto, traducao.containsKey(texto));
                    }
                }
            }
        }
        assertTrue("o treino do Prólogo tem conversa", conversas > 0);
    }

    /** O treino tem a fala de toda lição e a do fim. */
    @Test
    public void treinoTemTodasAsLicoes() throws IOException {
        ConversaLuta c = treino(Temporada.load(FILES));
        String[] licoes = {"andar", "agachar", "pular", "super-pulo", "dash", "recuo", "golpe-fraco",
            "golpe-medio", "golpe-forte", "fraco-agachado", "medio-agachado", "forte-agachado", "golpe-no-ar",
            "cravada", "combo", "botao-combo", "defesa", "defesa-baixa", "especial-de-energia", "especial-s2",
            "especial-s3", "especial-s4", "agarrao", "super", "ultra", "fim"};
        for (String id : licoes) assertTrue("lição sem fala: " + id, c.tem(id));
    }

    @Test
    public void falaEntraDigitaESaiSemToque() throws IOException {
        ConversaLuta c = treino(Temporada.load(FILES));
        PaginaHistoriaTest.NullCanvas canvas = new PaginaHistoriaTest.NullCanvas();
        c.momento("combo");
        assertEquals("takatoshi", c.atual().quem);
        int frames = 0;
        boolean dafuFalou = false;
        while (c.busy() && frames++ < 60 * 30) {
            c.update(DT);
            c.render(canvas);
            if (c.atual() != null && "dafu".equals(c.atual().quem)) dafuFalou = true;
        }
        assertTrue("a segunda fala vem sozinha", dafuFalou);
        assertFalse("os quadros saem sozinhos", c.busy());
        assertTrue("dá tempo de ler", frames > 60 * 5);
    }

    @Test
    public void momentoNovoTomaOLugarEReacaoNaoAtropela() throws IOException {
        ConversaLuta c = treino(Temporada.load(FILES));
        c.momento("andar");
        c.reacao("impacto");
        assertEquals("a reação não corta a explicação", "takatoshi", c.atual().quem);
        c.momento("agachar");
        assertEquals("dafu", c.atual().quem);

        // Logo depois de uma fala, a reação espera um silêncio.
        esperar(c);
        c.momento("andar");
        int frames = 0;
        while (c.falando() && frames++ < 60 * 30) c.update(DT);
        c.reacao("licao");
        assertFalse("sem falatório", c.falando());
        for (int i = 0; i < 60 * 2; i++) c.update(DT);

        // Reação curta ("Bom."): a fala do momento seguinte espera ela terminar.
        c.reacao("licao");
        ConversaLuta.Fala reacao = c.atual();
        assertNotNull(reacao);
        c.momento("pular");
        assertEquals(reacao, c.atual());
        frames = 0;
        while (c.atual() == reacao && frames++ < 60 * 10) c.update(DT);
        assertEquals("Agora pule. Leve, como se o chão empurrasse você.", c.atual().texto);
    }

    @Test
    public void reacaoNaoRepeteASeguida() throws IOException {
        ConversaLuta c = treino(Temporada.load(FILES));
        String antes = null;
        for (int i = 0; i < 12; i++) {
            esperar(c);
            for (int k = 0; k < i; k++) c.update(DT);
            c.reacao("levou");
            String texto = c.atual().texto;
            assertNotEquals(antes, texto);
            antes = texto;
        }
    }

    /** Duas pessoas do mesmo lado (o parceiro de tag e o adversário que espera trocam de quadro). */
    @Test
    public void mesmoLadoTrocaORosto() throws IOException {
        Temporada temporada = Temporada.load(FILES);
        ConversaLuta c = new ConversaLuta(FILES);
        c.fromJson(temporada, "{\"lados\":{\"ryuga\":\"esquerda\",\"takatoshi\":\"esquerda\",\"koga\":\"direita\"}," +
            "\"momentos\":{\"inicio\":[{\"quem\":\"koga\",\"expressao\":\"bravo\",\"texto\":\"Vem!\"}," +
            "{\"quem\":\"ryuga\",\"texto\":\"Vou.\"},{\"quem\":\"takatoshi\",\"texto\":\"Calma.\"}]}}");
        assertTrue(c.ativa());
        c.momento("inicio");
        PaginaHistoriaTest.NullCanvas canvas = new PaginaHistoriaTest.NullCanvas();
        java.util.List<String> ordem = new java.util.ArrayList<>();
        int frames = 0;
        while (c.busy() && frames++ < 60 * 30) {
            if (c.atual() != null && (ordem.isEmpty() || !ordem.get(ordem.size() - 1).equals(c.atual().quem))) {
                ordem.add(c.atual().quem);
            }
            c.update(DT);
            c.render(canvas);
        }
        assertEquals(java.util.Arrays.asList("koga", "ryuga", "takatoshi"), ordem);
        c.descarregar();
        assertFalse(c.ativa());
    }

    @Test
    public void arquivoQuebradoAvisa() throws IOException {
        Temporada temporada = Temporada.load(FILES);
        ConversaLuta c = new ConversaLuta(FILES);
        String base = "{\"lados\":{\"dafu\":\"direita\"},\"momentos\":{\"x\":[";
        c.fromJson(temporada, base + "{\"quem\":\"dafu\",\"expressao\":\"rindo\",\"texto\":\"oi\"}]}}");
        assertTrue(c.tem("x"));
        String[] ruins = {
            base + "{\"quem\":\"koga\",\"texto\":\"oi\"}]}}",
            base + "{\"quem\":\"dafu\",\"texto\":\"\"}]}}",
            base + "{\"quem\":\"dafu\",\"expressao\":\"Bravo!\",\"texto\":\"oi\"}]}}",
            "{\"lados\":{\"dafu\":\"meio\"}}",
            "{\"lados\":{\"ninguem\":\"direita\"}}"};
        for (String ruim : ruins) {
            try {
                c.fromJson(temporada, ruim);
                throw new AssertionError("devia recusar: " + ruim);
            } catch (IllegalArgumentException expected) {
                // ok
            }
        }
    }

    /**
     * Fora do elenco (o parceiro de tag de qualquer luta): nome, cor e o rosto de reserva do
     * avulso; no elenco, o nome e a cor da história continuam valendo.
     */
    @Test
    public void avulsoFalaComNomeCorERostoDeReserva() throws IOException {
        java.util.List<String> pedidos = new java.util.ArrayList<>();
        RenderAssets assets = new RenderAssets() {
            @Override public String readText(String path) throws IOException { return FILES.readText(path); }
            @Override public RenderImage loadImage(String path) throws IOException {
                pedidos.add(path);
                if (!path.startsWith("selection/")) throw new IOException(path);
                return new RenderImage() {
                    @Override public int width() { return 100; }
                    @Override public int height() { return 120; }
                };
            }
        };
        Temporada temporada = Temporada.load(FILES);
        ConversaLuta c = new ConversaLuta(assets);
        c.avulso("p05", "P05", 0xFF112233, "selection/rostos/p05.webp");
        c.dizer(temporada, "p05", true, "bravo", "Troca comigo!", 1f);
        assertEquals("P05", c.nome("p05"));
        assertEquals(0xFF112233, c.cor("p05"));
        assertTrue("tenta o retrato e cai no de reserva", pedidos.contains("selection/rostos/p05.webp"));
        c.avulso("ryuga", "P01", 0xFF000000, "selection/rostos/p01.webp");
        assertEquals("Ryūga", c.nome("ryuga"));
        assertEquals(temporada.personagem("ryuga").cor, c.cor("ryuga"));
        PaginaHistoriaTest.NullCanvas canvas = new PaginaHistoriaTest.NullCanvas();
        int frames = 0;
        while (c.busy() && frames++ < 60 * 10) {
            c.update(DT);
            c.render(canvas);
        }
        assertFalse(c.busy());
    }

    /** Sem o arquivo ninguém fala e a luta segue igual. */
    @Test
    public void semArquivoNinguemFala() throws IOException {
        ConversaLuta c = new ConversaLuta(FILES);
        assertFalse(c.carregar(Temporada.load(FILES), "ep00/nao-existe.json"));
        assertFalse(c.ativa());
        c.momento("andar");
        assertFalse(c.falando());
        assertFalse(c.busy());
    }
}
