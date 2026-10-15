package com.gamelutagpt.historia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** Cinemática dentro da luta: os arquivos do jogo valem e a linha do tempo roda sozinha. */
public class CinematicaLutaTest {
    private static final File ASSETS = new File("../app/src/main/assets");
    private static final float DT = 1f / 60f;
    private static final String TREINO_EP01 = "ep01/cinematica_treino.json";

    private static final RenderAssets FILES = new RenderAssets() {
        @Override public String readText(String path) throws IOException {
            return new String(Files.readAllBytes(new File(ASSETS, path).toPath()), StandardCharsets.UTF_8);
        }
        @Override public RenderImage loadImage(String path) throws IOException { throw new IOException(path); }
    };

    /** Palco de mentira: anota o que a cena pediu; cada movimento leva alguns quadros. */
    private static final class Palco implements CinematicaLuta.Palco {
        final List<String> pedidos = new ArrayList<>();
        final int[] restante = new int[2];
        int quadrosPorMovimento = 30;
        boolean travado;
        String vencedor;
        /** O lutador do jogador está à direita do rival na tela (os dois trocaram de lado). */
        boolean jogadorADireita;

        @Override public void travarControle(boolean travar) {
            travado = travar;
            pedidos.add(travar ? "travar" : "destravar");
        }
        @Override public void mover(int lado, CinematicaLuta.Movimento m) {
            restante[lado] = m.tipo == CinematicaLuta.Movimento.Tipo.PARAR ? 0 : quadrosPorMovimento;
            pedidos.add((lado == CinematicaLuta.JOGADOR ? "jogador " : "rival ") + m.tipo);
        }
        @Override public boolean movendo(int lado) { return restante[lado] > 0; }
        @Override public void terminarLuta(String vencedor) {
            this.vencedor = vencedor;
            pedidos.add("fim " + vencedor);
        }
        @Override public boolean aDireita(int lado) {
            return lado == CinematicaLuta.JOGADOR ? jogadorADireita : !jogadorADireita;
        }
        void quadro() {
            for (int i = 0; i < 2; i++) if (restante[i] > 0) restante[i]--;
        }
    }

    private static void rodar(CinematicaLuta cena, ConversaLuta conversa, Palco palco, int segundos) {
        for (int i = 0; i < segundos * 60 && !cena.acabou(); i++) {
            palco.quadro();
            cena.update(DT);
            conversa.update(DT);
        }
    }

    /**
     * Toda cinemática apontada por uma luta dos episódios abre, só aparece em passo de luta,
     * quem fala tem o rosto "normal" e todo texto tem tradução em todos os idiomas.
     */
    @Test
    public void cinematicasDosEpisodiosValem() throws IOException {
        Temporada temporada = Temporada.load(FILES);
        File[] idiomas = new File(ASSETS, Temporada.PASTA + "/idiomas").listFiles();
        assertNotNull(idiomas);
        int cinematicas = 0;
        for (String id : temporada.episodios) {
            Episodio ep = Episodio.fromJson(FILES.readText(Temporada.PASTA + "/" + id + "/episodio.json"));
            for (Episodio.Passo passo : ep.passos) {
                if (passo.cinematica == null) continue;
                cinematicas++;
                assertEquals(passo.cinematica + " fora de um passo de luta", Episodio.TipoPasso.LUTA, passo.tipo);
                CinematicaLuta.Roteiro r = CinematicaLuta.ler(temporada, FILES.readText(Temporada.PASTA + "/" + passo.cinematica));
                for (String quem : r.quemFala()) {
                    assertTrue(passo.cinematica + ": sem o rosto normal de " + quem,
                        new File(ASSETS, ConversaLuta.RETRATOS + "/" + quem + "_normal.png").isFile());
                }
                for (File arquivo : idiomas) {
                    Map<String, String> traducao = Temporada.traducaoFromJson(
                        FILES.readText(Temporada.PASTA + "/idiomas/" + arquivo.getName()));
                    for (String texto : r.textos()) {
                        assertTrue(arquivo.getName() + " sem tradução para: " + texto, traducao.containsKey(texto));
                    }
                }
            }
        }
        assertTrue("o treino do Episódio 1 tem cinemática", cinematicas > 0);
    }

    /** Os rostos do olhar.json existem (o quadro espelha quem olharia para fora da tela). */
    @Test
    public void olharApontaRostosQueExistem() throws IOException {
        Map<String, Boolean> olhar = ConversaLuta.olharFromJson(FILES.readText(ConversaLuta.OLHAR));
        assertTrue(olhar.containsKey("ryuga_bravo"));
        for (String rosto : olhar.keySet()) {
            assertTrue("sem a imagem de " + rosto, new File(ASSETS, ConversaLuta.RETRATOS + "/" + rosto + ".png").isFile());
        }
    }

    /**
     * O treino do Episódio 1: com 25% de vida a cena começa, trava o controle, faixas entram,
     * os dois pulam para trás, gritam (o irmão do jogador à esquerda), correm, a campainha toca
     * e a luta termina interrompida, com as faixas já fora.
     */
    @Test
    public void treinoDoEpisodio1() throws IOException {
        for (int caso = 0; caso < 4; caso++) {
            String irmao = caso % 2 == 0 ? Torneio.RYUGA : Torneio.KOGA;
            boolean trocaram = caso >= 2;
            Temporada temporada = Temporada.load(FILES);
            ConversaLuta conversa = new ConversaLuta(FILES);
            Palco palco = new Palco();
            palco.jogadorADireita = trocaram;
            CinematicaLuta cena = new CinematicaLuta(FILES, conversa, palco);
            assertTrue(cena.carregar(temporada, TREINO_EP01, irmao));
            assertTrue(cena.armada());
            assertTrue("o gatilho de vida segura o KO", cena.protegeVida());

            assertFalse(cena.gatilho(1f, 0.6f, 12f, false));
            assertFalse(cena.gatilho(0.3f, 0.26f, 20f, false));
            assertTrue("25% de qualquer lado", cena.gatilho(0.8f, 0.25f, 30f, false));
            assertTrue(cena.rodando());
            assertTrue(palco.travado);
            assertTrue(cena.controleTravado());
            assertTrue(cena.protegeVida());
            // Primeiro só trava: o golpe que levou aos 25% termina em câmera lenta.
            assertEquals("[travar]", palco.pedidos.toString());
            assertTrue("câmera lenta", cena.velocidade() <= 1f);

            List<String> falas = new ArrayList<>();
            List<Boolean> direita = new ArrayList<>();
            float maxFaixas = 0f;
            float maxZoom = 1f;
            float minVelocidade = 1f;
            float maxFocoRyuga = 0f;
            int ladoRyuga = irmao.equals(Torneio.RYUGA) ? CinematicaLuta.JOGADOR : CinematicaLuta.RIVAL;
            float duracao = 0f;
            boolean campainha = false;
            boolean correuAntes = false;
            PaginaHistoriaTest.NullCanvas canvas = new PaginaHistoriaTest.NullCanvas();
            for (int i = 0; i < 60 * 30 && !cena.acabou(); i++) {
                palco.quadro();
                cena.update(DT);
                conversa.update(DT);
                cena.render(canvas);
                conversa.render(canvas);
                maxFaixas = Math.max(maxFaixas, cena.faixas());
                maxZoom = Math.max(maxZoom, cena.zoom());
                minVelocidade = Math.min(minVelocidade, cena.velocidade());
                if ("KOGAAAAA!".equals(conversa.atual() != null ? conversa.atual().texto : null)) {
                    maxFocoRyuga = Math.max(maxFocoRyuga, cena.foco(ladoRyuga));
                }
                duracao += DT;
                ConversaLuta.Fala f = conversa.atual();
                if (f != null && (falas.isEmpty() || !falas.get(falas.size() - 1).equals(f.texto))) {
                    falas.add(f.texto);
                    direita.add(f.naDireita);
                    assertFalse("ninguém corre antes dos gritos", palco.pedidos.contains("jogador CORRER"));
                }
                if (cena.estouros() > 0 && !campainha) {
                    campainha = true;
                    correuAntes = palco.pedidos.contains("jogador CORRER") && palco.pedidos.contains("rival CORRER");
                }
            }
            assertTrue("a cena termina", cena.acabou());
            List<String> ordem = new ArrayList<>();
            for (String p : palco.pedidos) {
                if (p.startsWith("jogador ")) ordem.add(p.substring(8));
            }
            assertEquals("correm, a corrida fica lenta, a campainha para tudo", "[TITULAR, PULAR_TRAS, RECUAR, CORRER, CORRER, PARAR]", ordem.toString());
            assertEquals("interrompida", palco.vencedor);
            assertEquals("fim interrompida", palco.pedidos.get(palco.pedidos.size() - 1));
            assertEquals(1f, maxFaixas, 1e-3f);
            assertEquals("as faixas saem antes do fim", 0f, cena.faixas(), 1e-3f);
            assertTrue("a câmera chega mais perto", maxZoom > 1.05f);
            assertTrue("DING DONG", campainha);
            assertTrue("a campainha toca com os dois correndo", correuAntes);
            assertEquals(2, falas.size());
            assertEquals("KOGAAAAA!", falas.get(0));
            assertEquals("RYŪGAAAAA!", falas.get(1));
            // Cada irmão fala no quadro do lado da tela onde o lutador dele está.
            boolean ryugaADireita = palco.aDireita(ladoRyuga);
            assertEquals(irmao + "/" + trocaram + ": lado do Ryūga", ryugaADireita, direita.get(0));
            assertEquals(irmao + "/" + trocaram + ": lado do Kōga", !ryugaADireita, direita.get(1));
            assertTrue("a câmera vai no Ryūga enquanto ele grita", maxFocoRyuga > 0.9f);
            assertEquals("a campainha congela a luta", 0f, minVelocidade, 1e-3f);
            assertEquals("a luta volta à velocidade normal", 1f, cena.velocidade(), 1e-3f);
            assertTrue("dá tempo de sentir a cena: " + duracao, duracao > 12f);
            assertFalse(cena.protegeVida());
            assertFalse(cena.controleTravado());
        }
    }

    @Test
    public void movimentoSeguraACenaAteAcabar() {
        Palco palco = new Palco();
        palco.quadrosPorMovimento = 90;
        CinematicaLuta cena = cena(palco, "{\"gatilho\":\"inicio\",\"acoes\":[" +
            "{\"acao\":\"mover\",\"quem\":\"rival\",\"movimento\":\"recuar\"}," +
            "{\"acao\":\"controle\",\"travar\":true}]}");
        assertTrue(cena.gatilho(1f, 1f, 0f, false));
        for (int i = 0; i < 80; i++) {
            palco.quadro();
            cena.update(DT);
        }
        assertFalse("ainda recuando", palco.travado);
        for (int i = 0; i < 20; i++) {
            palco.quadro();
            cena.update(DT);
        }
        assertEquals("rival RECUAR", palco.pedidos.get(0));
        assertEquals("travar", palco.pedidos.get(1));
    }

    @Test
    public void semEsperarSegueNaHora() {
        Palco palco = new Palco();
        CinematicaLuta cena = cena(palco, "{\"gatilho\":{\"tempo\":5},\"acoes\":[" +
            "{\"acao\":\"esperar\",\"segundos\":2,\"semEsperar\":true}," +
            "{\"acao\":\"mover\",\"quem\":\"ambos\",\"movimento\":\"correr\",\"distancia\":200,\"semEsperar\":true}," +
            "{\"acao\":\"controle\",\"travar\":true}," +
            "{\"acao\":\"esperar\",\"segundos\":1}]}");
        assertFalse(cena.protegeVida());
        assertFalse(cena.gatilho(1f, 1f, 4.9f, false));
        assertTrue(cena.gatilho(1f, 1f, 5f, false));
        assertEquals(3, palco.pedidos.size());
        assertTrue(palco.travado);
    }

    /** Roteiro sem terminarLuta: no fim o controle volta, as faixas saem e a câmera volta. */
    @Test
    public void fimSemTerminarDevolveALuta() {
        Palco palco = new Palco();
        ConversaLuta conversa = new ConversaLuta(FILES);
        CinematicaLuta cena = new CinematicaLuta(FILES, conversa, palco);
        cena.descarregar();
        CinematicaLuta.Roteiro r = CinematicaLuta.ler(null, "{\"gatilho\":\"ko\",\"acoes\":[" +
            "{\"acao\":\"controle\",\"travar\":true}," +
            "{\"acao\":\"faixas\",\"entrar\":true}," +
            "{\"acao\":\"camera\",\"zoom\":1.3,\"segundos\":0.2}," +
            "{\"acao\":\"fala\",\"quem\":\"dafu\",\"lado\":\"direita\",\"texto\":\"Acabou!\"}]}");
        assertFalse(r.terminaLuta());
        assertEquals(1, r.quemFala().size());
        CinematicaLuta comArquivo = cena(palco, "{\"gatilho\":\"ko\",\"acoes\":[" +
            "{\"acao\":\"controle\",\"travar\":true}," +
            "{\"acao\":\"faixas\",\"entrar\":true}," +
            "{\"acao\":\"camera\",\"zoom\":1.3,\"segundos\":0.2}]}");
        assertFalse("ko não segura a vida", comArquivo.protegeVida());
        assertFalse(comArquivo.gatilho(0.1f, 0.1f, 60f, false));
        assertTrue(comArquivo.gatilho(0f, 1f, 60f, true));
        for (int i = 0; i < 120; i++) comArquivo.update(DT);
        assertTrue(comArquivo.acabou());
        assertFalse(palco.travado);
        assertEquals("destravar", palco.pedidos.get(palco.pedidos.size() - 1));
        assertEquals(0f, comArquivo.faixas(), 1e-3f);
        assertEquals(1f, comArquivo.zoom(), 1e-3f);
        assertTrue("continua carregada até a luta acabar", comArquivo.ativa());
    }

    /** A fala de quem não é irmão precisa dizer o lado; ações, movimentos e gatilhos inválidos falham. */
    @Test
    public void roteiroInvalidoNaoAbre() throws IOException {
        Temporada temporada = Temporada.load(FILES);
        String[] ruins = {
            "{\"acoes\":[{\"acao\":\"esperar\",\"segundos\":1}]}",
            "{\"gatilho\":{\"vida\":1.5},\"acoes\":[{\"acao\":\"esperar\",\"segundos\":1}]}",
            "{\"gatilho\":\"agora\",\"acoes\":[{\"acao\":\"esperar\",\"segundos\":1}]}",
            "{\"gatilho\":\"inicio\",\"acoes\":[]}",
            "{\"gatilho\":\"inicio\",\"acoes\":[{\"acao\":\"voar\"}]}",
            "{\"gatilho\":\"inicio\",\"acoes\":[{\"acao\":\"fala\",\"quem\":\"dafu\",\"texto\":\"Oi\"}]}",
            "{\"gatilho\":\"inicio\",\"acoes\":[{\"acao\":\"fala\",\"quem\":\"ninguem\",\"lado\":\"esquerda\",\"texto\":\"Oi\"}]}",
            "{\"gatilho\":\"inicio\",\"acoes\":[{\"acao\":\"fala\",\"quem\":\"ryuga\"}]}",
            "{\"gatilho\":\"inicio\",\"acoes\":[{\"acao\":\"mover\",\"quem\":\"ambos\",\"movimento\":\"voar\"}]}",
            "{\"gatilho\":\"inicio\",\"acoes\":[{\"acao\":\"mover\",\"quem\":\"todos\",\"movimento\":\"correr\"}]}",
            "{\"gatilho\":\"inicio\",\"acoes\":[{\"acao\":\"faixas\"}]}",
            "{\"gatilho\":\"inicio\",\"acoes\":[{\"acao\":\"esperar\"}]}",
            "{\"gatilho\":\"inicio\",\"acoes\":[{\"acao\":\"camera\",\"zoom\":5}]}",
            "{\"gatilho\":\"inicio\",\"acoes\":[{\"acao\":\"onomatopeia\",\"texto\":\"PÁ\",\"pos\":[2,0]}]}",
            "{\"gatilho\":\"inicio\",\"acoes\":[{\"acao\":\"terminarLuta\",\"vencedor\":\"empate\"}]}",
            "{\"gatilho\":\"inicio\",\"acoes\":[{\"acao\":\"camera\",\"foco\":\"dafu\"}]}",
            "{\"gatilho\":\"inicio\",\"acoes\":[{\"acao\":\"velocidade\"}]}",
            "{\"gatilho\":\"inicio\",\"acoes\":[{\"acao\":\"velocidade\",\"valor\":2}]}",
            "{\"gatilho\":\"inicio\",\"acoes\":[{\"acao\":\"fala\",\"quem\":\"dafu\",\"lado\":\"cima\",\"texto\":\"Oi\"}]}",
        };
        for (String json : ruins) {
            try {
                CinematicaLuta.ler(temporada, json);
                fail("deveria falhar: " + json);
            } catch (IllegalArgumentException expected) {
                // ok
            }
        }
        CinematicaLuta cena = new CinematicaLuta(FILES, null, new Palco());
        assertFalse("arquivo que não existe: a luta segue sem cena", cena.carregar(temporada, "ep01/nao_existe.json", "ryuga"));
        assertFalse(cena.ativa());
    }

    /** Uma cena sem conversa (sem quadros) ainda roda, e a fala não trava nada. */
    @Test
    public void semConversaAFalaPassaDireto() {
        Palco palco = new Palco();
        CinematicaLuta cena = new CinematicaLuta(new RenderAssets() {
            @Override public String readText(String path) {
                return "{\"gatilho\":\"inicio\",\"acoes\":[" +
                    "{\"acao\":\"fala\",\"quem\":\"koga\",\"texto\":\"Vem!\"}," +
                    "{\"acao\":\"onomatopeia\",\"texto\":\"PÁ!\",\"segundos\":0.5}," +
                    "{\"acao\":\"terminarLuta\",\"vencedor\":\"rival\"}]}";
            }
            @Override public RenderImage loadImage(String path) throws IOException { throw new IOException(path); }
        }, null, palco);
        assertTrue(cena.carregar(null, "x.json", "koga"));
        cena.gatilho(1f, 1f, 0f, false);
        rodar(cena, new ConversaLuta(FILES), palco, 3);
        assertEquals("rival", palco.vencedor);
    }

    private static CinematicaLuta cena(Palco palco, final String json) {
        CinematicaLuta cena = new CinematicaLuta(new RenderAssets() {
            @Override public String readText(String path) { return json; }
            @Override public RenderImage loadImage(String path) throws IOException { throw new IOException(path); }
        }, null, palco);
        assertTrue(cena.carregar(null, "x.json", Torneio.RYUGA));
        return cena;
    }
}
