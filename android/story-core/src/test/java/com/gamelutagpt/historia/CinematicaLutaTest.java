package com.gamelutagpt.historia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** Cinemática dentro da luta: os arquivos do jogo valem e a linha do tempo roda sozinha. */
public class CinematicaLutaTest {
    private static final File ASSETS = new File("../app/src/main/assets");
    private static final float DT = 1f / 60f;
    private static final String TREINO_EP01 = "ep01/cinematica_treino.json";
    private static final String CONVITE_EP02 = "ep02/cinematica_convite_ryuga_qte.json";

    private static final RenderAssets FILES = new RenderAssets() {
        @Override public String readText(String path) throws IOException {
            return new String(Files.readAllBytes(new File(ASSETS, path).toPath()), StandardCharsets.UTF_8);
        }
        @Override public RenderImage loadImage(String path) throws IOException { throw new IOException(path); }
    };

    /** Os mesmos arquivos, com imagens de mentira (só o tamanho) para as que existem. */
    private static final RenderAssets COM_IMAGENS = new RenderAssets() {
        @Override public String readText(String path) throws IOException { return FILES.readText(path); }
        @Override public RenderImage loadImage(String path) throws IOException {
            if (!new File(ASSETS, path).isFile()) throw new IOException(path);
            return new RenderImage() {
                @Override public int width() { return 1024; }
                @Override public int height() { return 1536; }
            };
        }
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
        @Override public void golpear(int lado, CinematicaLuta.Golpe g) {
            restante[lado] = quadrosPorMovimento;
            pedidos.add((lado == CinematicaLuta.JOGADOR ? "jogador " : "rival ") + g.golpe + (g.finaliza ? " final" : ""));
        }
        @Override public boolean golpeando(int lado) { return restante[lado] > 0; }
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
                conferirCinematica(temporada, passo.cinematica, idiomas);
            }
        }
        assertTrue("o treino do Episódio 1 tem cinemática", cinematicas > 0);
    }

    /** As histórias das duplas do torneio (Torneio.Dupla.cinematica) abrem como as dos episódios. */
    @Test
    public void cinematicasDasDuplasValem() throws IOException {
        Temporada temporada = Temporada.load(FILES);
        File[] idiomas = new File(ASSETS, Temporada.PASTA + "/idiomas").listFiles();
        int com = 0;
        for (Torneio.Dupla d : Torneio.duplasFromJson(FILES.readText(Torneio.ARQUIVO))) {
            if (d.cinematica == null) continue;
            com++;
            assertNull(d.id + ": dupla de irmão não tem história de adversário", d.irmao);
            conferirCinematica(temporada, d.cinematica, idiomas);
        }
        assertTrue("p09 e p11 têm história", com >= 2);
    }

    /** O gatilho "ultimo": no 2x2 a cena espera o parceiro cair; até lá o rival perde normalmente. */
    @Test
    public void gatilhoDoUltimoDePe() throws IOException {
        Temporada temporada = Temporada.load(FILES);
        Palco palco = new Palco();
        CinematicaLuta cena = new CinematicaLuta(COM_IMAGENS, new ConversaLuta(FILES), palco);
        assertTrue(cena.carregar(temporada, "torneio/cinematica_p09.json", Torneio.RYUGA));
        cena.sozinhos(true, false);
        assertFalse("com o parceiro de pé, o rival da vez pode cair", cena.protegeVida(CinematicaLuta.RIVAL));
        assertFalse("o parceiro ainda está de pé", cena.gatilho(1f, 0.1f, 10f, false));
        cena.sozinhos(true, true);
        assertTrue("o último de pé fica com o último ponto até a cena", cena.protegeVida(CinematicaLuta.RIVAL));
        assertFalse(cena.gatilho(1f, 0.3f, 12f, false));
        assertTrue(cena.gatilho(1f, 0.25f, 12f, false));
    }

    /** A cinemática abre, quem fala tem rosto, as páginas existem e todo texto tem tradução. */
    private static void conferirCinematica(Temporada temporada, String arquivo, File[] idiomas) throws IOException {
        CinematicaLuta.Roteiro r = CinematicaLuta.ler(temporada, FILES.readText(Temporada.PASTA + "/" + arquivo));
        for (String quem : r.quemFala()) {
            // "jogador"/"rival": quem estiver em campo, com o rosto da seleção.
            if (quem.equals("jogador") || quem.equals("rival")) continue;
            // Sem retrato da história, quem luta fala com o rosto da seleção.
            String lutador = temporada.elenco.get(quem).lutador;
            String codigo = "player_base".equals(lutador) ? "p01" : lutador;
            assertTrue(arquivo + ": sem o rosto normal de " + quem,
                new File(ASSETS, ConversaLuta.RETRATOS + "/" + quem + "_normal.png").isFile() ||
                lutador != null && new File(ASSETS, "selection/rostos/" + codigo + ".webp").isFile());
        }
        for (String pagina : r.paginas()) {
            assertTrue(arquivo + ": sem a página " + pagina, new File(ASSETS, Temporada.PASTA + "/" + pagina).isFile());
        }
        for (String pagina : r.paginasDosEpisodios()) {
            // A página é lida com os quadros e balões do passo dela no episódio.
            String epId = pagina.substring(0, pagina.indexOf('/'));
            Episodio epDaPagina = Episodio.fromJson(FILES.readText(Temporada.pasta(epId) + "/episodio.json"));
            assertNotNull(arquivo + ": " + pagina + " não é uma página pronta (com quadros) do " + epId,
                CinematicaLuta.paginaDoEpisodio(epDaPagina, pagina.substring(pagina.indexOf('/') + 1)));
        }
        List<String> textos = new ArrayList<>(r.textos());
        textos.addAll(CinematicaLuta.TEXTOS_QTE);
        for (File idioma : idiomas) {
            Map<String, String> traducao = Temporada.traducaoFromJson(
                FILES.readText(Temporada.PASTA + "/idiomas/" + idioma.getName()));
            for (String texto : textos) {
                assertTrue(idioma.getName() + " sem tradução para: " + texto, traducao.containsKey(texto));
            }
        }
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
            assertTrue("de qualquer lado: os dois", cena.protegeVida(CinematicaLuta.JOGADOR) && cena.protegeVida(CinematicaLuta.RIVAL));

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

    /**
     * O convite do Episódio 2 do Ryūga (a coreografia de 13/11): a Yin-lu fica com 20% da vida, a
     * tinta cobre, as páginas são lidas quadro a quadro no toque, a tinta cobre de novo e sai; o
     * final pede 6 QTE (esquiva, combo, defesa e a sequência de precisão), todos sem golpe próprio
     * (o que acontece no acerto é a troca de golpes do roteiro). Os golpes que pegam são do
     * roteiro: a palma e o agarrão dela (o chute giratório dele ela defende). Deixar o tempo acabar e deslizar
     * para o lado errado são erros: o rival bate e o QTE pede de novo. Ninguém cai: a Yin-lu
     * defende o último golpe e a luta acaba interrompida, com a tela limpa.
     */
    @Test
    public void qteDoConviteDoRyuga() throws IOException {
        Palco palco = jogarConvite(CONVITE_EP02, Torneio.RYUGA, 6);
        assertEquals("cada erro um golpe do rival; os golpes do roteiro",
            "[rival 2H, rival S1, rival A, rival M]", golpes(palco).toString());
        assertTrue("a luta acaba sem vencedor", palco.pedidos.contains("fim interrompida"));
    }

    /**
     * O convite do Ryūga animado à mão: a animação para na pose do golpe e cada um dos 6 QTE
     * pede ali; errar toca o contra-ataque dela (na animação, sem golpe do motor de luta) e pede
     * de novo. Ninguém bate pelo motor e a luta acaba interrompida.
     */
    @Test
    public void qteDoConviteAnimadoDoRyuga() throws IOException {
        Palco palco = jogarConvite("ep02/cinematica_convite_ryuga.json", Torneio.RYUGA, 6);
        assertEquals("os golpes são da animação, não do motor de luta", "[]", golpes(palco).toString());
        assertTrue("a luta acaba sem vencedor", palco.pedidos.contains("fim interrompida"));
    }

    /** O convite do Kōga: o Brucy bate primeiro (sem QTE) e o mergulho é o golpe final. */
    @Test
    public void qteDoConviteDoKoga() throws IOException {
        Palco palco = jogarConvite("ep02/cinematica_convite_koga.json", Torneio.KOGA, 4);
        assertEquals("[rival S2, rival M, jogador H, rival S4, jogador S3, jogador S2, jogador S4 final]", golpes(palco).toString());
    }

    private static List<String> golpes(Palco palco) {
        List<String> golpes = new ArrayList<>();
        for (String p : palco.pedidos) {
            if (p.matches("(jogador|rival) (2?[LMH]|S\\d?|A)( final)?")) golpes.add(p);
        }
        return golpes;
    }

    /**
     * Joga a cena de um convite: o rival com 20% dispara, as páginas no toque e cada QTE pelo
     * tipo dele (o primeiro toque e o primeiro deslize erram de propósito).
     */
    private static Palco jogarConvite(String arquivo, String irmao, int qtesEsperados) throws IOException {
        Temporada temporada = Temporada.load(FILES);
        ConversaLuta conversa = new ConversaLuta(FILES);
        Palco palco = new Palco();
        palco.quadrosPorMovimento = 20;
        CinematicaLuta cena = new CinematicaLuta(COM_IMAGENS, conversa, palco);
        assertTrue(cena.carregar(temporada, arquivo, irmao));
        assertTrue("esperando, o rival não cai antes da cena", cena.protegeVida(CinematicaLuta.RIVAL));
        assertFalse("o jogador pode perder antes da cena", cena.protegeVida(CinematicaLuta.JOGADOR));
        assertFalse("o gatilho é a vida do rival", cena.gatilho(0.1f, 0.5f, 10f, false));
        assertFalse("ele ainda tem mais de 20%", cena.gatilho(1f, 0.25f, 10f, false));
        assertTrue(cena.gatilho(1f, 0.2f, 12f, false));
        assertTrue(palco.travado);
        assertTrue("com a cena rodando ninguém cai", cena.protegeVida(CinematicaLuta.JOGADOR) && cena.protegeVida(CinematicaLuta.RIVAL));

        PaginaHistoriaTest.NullCanvas canvas = new PaginaHistoriaTest.NullCanvas();
        boolean cobriu = false, pedia = false, paginaSumiu = false, congelou = false;
        boolean errouToque = false, errouDeslize = false;
        int toquesNaPagina = 0, qtes = 0, quadrosNoQte = 0, toques = 0, qteQueErra = -1;
        for (int i = 0; i < 60 * 120 && !cena.acabou(); i++) {
            palco.quadro();
            cena.update(DT);
            conversa.update(DT);
            cena.render(canvas);
            conversa.render(canvas);
            cobriu |= cena.telaCoberta();
            congelou |= cena.velocidade() == 0f;
            if (cena.paginaNaTela() && cena.pegaToque() && i % 20 == 0) {
                // A página: um toque por vez, quadro a quadro, como na história.
                cena.toque(CinematicaLuta.TOQUE_DESCE, 640f, 360f);
                toquesNaPagina++;
                continue;
            }
            paginaSumiu |= toquesNaPagina > 0 && !cena.paginaNaTela();
            boolean pede = cena.qtePedindo();
            if (pede && !pedia) {
                if (cena.qteErros() == 0) qtes++;
                quadrosNoQte = 0;
                toques = 0;
            }
            pedia = pede;
            if (!pede) continue;
            quadrosNoQte++;
            switch (cena.qteTipo()) {
                case TOQUE:
                    // O primeiro QTE de toque deixa o tempo acabar (erro), depois toca.
                    if (qteQueErra < 0 && cena.qteErros() == 0) qteQueErra = qtes;
                    if (qtes == qteQueErra && cena.qteErros() == 0) errouToque = true;
                    else if (quadrosNoQte == 10) cena.toque(CinematicaLuta.TOQUE_DESCE, 300f, 300f);
                    break;
                case MARTELAR:
                    if (quadrosNoQte % 4 == 0) {
                        cena.toque(CinematicaLuta.TOQUE_DESCE, 640f, 400f);
                        cena.toque(CinematicaLuta.TOQUE_SOBE, 640f, 400f);
                        toques++;
                    }
                    break;
                case DESLIZAR:
                    if (quadrosNoQte == 6) {
                        // Primeiro para o lado contrário (erro), depois para o pedido.
                        int sinal = !errouDeslize && cena.qteErros() == 0 ? -1 : 1;
                        int dx = cena.qteDirecao()[0] * sinal, dy = cena.qteDirecao()[1] * sinal;
                        errouDeslize |= sinal < 0;
                        cena.toque(CinematicaLuta.TOQUE_DESCE, 640f, 420f);
                        cena.toque(CinematicaLuta.TOQUE_MOVE, 640f + dx * 60f, 420f + dy * 60f);
                        cena.toque(CinematicaLuta.TOQUE_MOVE, 640f + dx * 130f, 420f + dy * 130f);
                        cena.toque(CinematicaLuta.TOQUE_SOBE, 640f + dx * 130f, 420f + dy * 130f);
                    }
                    break;
                default:
                    // O anel fecha no tempo do QTE: toca na hora.
                    if (quadrosNoQte == Math.round(cena.qteSegundos() * 60f)) cena.botao();
                    break;
            }
        }
        assertTrue(arquivo + ": a cena termina", cena.acabou());
        assertTrue("a tinta cobriu a tela", cobriu);
        assertTrue("as páginas foram lidas em vários toques: " + toquesNaPagina, toquesNaPagina >= 6);
        assertTrue("a tinta nova cobriu a página", paginaSumiu);
        assertTrue(errouToque);
        assertTrue(errouDeslize);
        assertTrue("o impacto congela a luta", congelou);
        assertEquals(qtesEsperados, qtes);
        assertTrue("a recuada antes da tinta sair", palco.pedidos.contains("jogador RECUAR"));
        assertFalse("a tela limpa no fim", cena.telaCoberta());
        assertFalse(cena.controleTravado());
        assertEquals("a luta volta à velocidade normal", 1f, cena.velocidade(), 1e-3f);
        assertEquals("a câmera volta", 1f, cena.zoom(), 1e-3f);
        return palco;
    }

    /**
     * A briga dos irmãos (Prólogo) animada à mão, nas duas campanhas: as chaves são por irmão, então
     * cada menino faz o seu papel dos dois lados da tela (o Ryūga fica do lado dele e o soco final é
     * dele, como na página 5). Três QTE (o primeiro erra: os dois batem a cabeça e ele pede de
     * novo), o vídeo da troca de golpes no meio, ninguém bate de verdade e a tinta cobre o soco.
     */
    @Test
    public void brigaDosIrmaosAnimada() throws IOException {
        for (String irmao : new String[] {Torneio.RYUGA, Torneio.KOGA}) {
            Temporada temporada = Temporada.load(FILES);
            ConversaLuta conversa = new ConversaLuta(FILES);
            Palco palco = new Palco();
            CinematicaLuta cena = new CinematicaLuta(COM_IMAGENS, conversa, palco);
            assertTrue(cena.carregar(temporada, "ep00/cinematica_briga.json", irmao));
            assertEquals("o vídeo da troca de golpes, tocado de dentro da animação", Arrays.asList("ep00/qt1"), cena.videos());
            int ryuga = Torneio.RYUGA.equals(irmao) ? CinematicaLuta.JOGADOR : CinematicaLuta.RIVAL, koga = 1 - ryuga;
            assertTrue("as folhas do soco final do Ryūga carregam antes",
                Arrays.asList(cena.clipesAnimados(ryuga)).contains("CROUCH_LIGHT"));
            assertFalse("o Kōga não se abaixa no começo", Arrays.asList(cena.clipesAnimados(koga)).contains("CROUCH_HEAVY"));
            assertTrue(cena.gatilho(1f, 0.5f, 12f, false));
            int qtes = 0, quadrosNoQte = 0, quadrosDeVideo = 0;
            boolean pedia = false, errou = false, ladoCerto = true;
            String finalRyuga = null, finalKoga = null;
            PaginaHistoriaTest.NullCanvas canvas = new PaginaHistoriaTest.NullCanvas();
            for (int i = 0; i < 60 * 90 && !cena.acabou(); i++) {
                palco.quadro();
                cena.update(DT);
                conversa.update(DT);
                cena.render(canvas);
                if ("ep00/qt1".equals(cena.video())) {
                    quadrosDeVideo++;
                    assertEquals("o Kōga é quem está à esquerda no vídeo", koga, cena.videoEsquerda());
                }
                if (cena.animando()) {
                    // O Ryūga fica do lado dele (na campanha do Kōga, à direita).
                    float xr = cena.xDaPose(cena.poseAnimada(ryuga)), xk = cena.xDaPose(cena.poseAnimada(koga));
                    ladoCerto &= (xr < xk) == (palco.xDoLutador(ryuga) < palco.xDoLutador(koga));
                    finalRyuga = cena.poseAnimada(ryuga).clip;
                    finalKoga = cena.poseAnimada(koga).clip;
                }
                boolean pede = cena.qtePedindo();
                if (pede && !pedia && cena.qteErros() == 0) qtes++;
                pedia = pede;
                if (!pede) {
                    quadrosNoQte = 0;
                    continue;
                }
                quadrosNoQte++;
                if (qtes == 1 && cena.qteErros() == 0) {
                    errou = true;
                } else if (cena.qteTipo() == CinematicaLuta.TipoQte.MARTELAR && quadrosNoQte % 4 == 0) {
                    cena.toque(CinematicaLuta.TOQUE_DESCE, 640f, 400f);
                    cena.toque(CinematicaLuta.TOQUE_SOBE, 640f, 400f);
                } else if (cena.qteTipo() == CinematicaLuta.TipoQte.TEMPO && quadrosNoQte == Math.round(cena.qteSegundos() * 60f)) {
                    cena.botao();
                } else if (cena.qteTipo() == CinematicaLuta.TipoQte.TOQUE && quadrosNoQte == 20) {
                    cena.toque(CinematicaLuta.TOQUE_DESCE, 300f, 300f);
                    cena.toque(CinematicaLuta.TOQUE_SOBE, 300f, 300f);
                }
            }
            assertTrue(irmao + ": a cena termina", cena.acabou());
            assertEquals(3, qtes);
            assertTrue(errou);
            assertTrue("o vídeo toca uma vez inteiro (4,3 s): " + quadrosDeVideo, quadrosDeVideo > 230 && quadrosDeVideo < 300);
            assertTrue(irmao + ": cada um do seu lado", ladoCerto);
            assertEquals(irmao + ": o soco final é do Ryūga", "CROUCH_LIGHT", finalRyuga);
            assertEquals(irmao + ": o Kōga leva na cara", "HIT_STAND", finalKoga);
            assertEquals("interrompida", palco.vencedor);
            for (String pedido : palco.pedidos) {
                assertFalse("ninguém bate de verdade: " + pedido, pedido.matches("(jogador|rival) (2?[LMH]|S\\d?)( final)?"));
            }
            assertTrue("a tinta cobre o soco (a pag5 começa nele)", cena.telaCoberta());
        }
    }

    /** O treino e a briga dos irmãos (as lutas das crianças no Prólogo) não têm SUPER, ULTRA nem SHINKAI. */
    @Test
    public void lutasDasCriancasSemPoderes() throws IOException {
        Temporada temporada = Temporada.load(FILES);
        Episodio ep = temporada.loadEpisodio(FILES, "ep00");
        int lutas = 0;
        for (Episodio.Passo p : ep.passos) {
            if (p.tipo != Episodio.TipoPasso.LUTA) continue;
            lutas++;
            assertTrue((p.marca) + " sem poderes", p.semPoderes);
        }
        assertEquals("treino e briga, nas duas campanhas", 4, lutas);
        // A primeira luta dos irmãos adultos (o treino do Episódio 1): sem SHINKAI, com SUPER.
        Episodio.Passo treino = null;
        for (Episodio.Passo p : temporada.loadEpisodio(FILES, "ep01").passos) {
            if (p.tipo == Episodio.TipoPasso.LUTA && treino == null) treino = p;
        }
        assertTrue("o treino do Episódio 1 sem SHINKAI", treino.semShinkai);
        assertFalse("mas com SUPER", treino.semPoderes);
        assertFalse("fora do Prólogo os poderes continuam",
            Episodio.fromJson("{\"passos\":[{\"tipo\":\"luta\",\"dupla\":[\"p02\"],\"cpu\":[\"p03\"]}]}").passos.get(0).semPoderes);
    }

    /** O QTE precisa do controle travado (quem bate é o roteiro). */
    @Test
    public void qteSemControleTravadoNaoAbre() {
        try {
            CinematicaLuta.ler(null, "{\"gatilho\":\"inicio\",\"acoes\":[{\"acao\":\"qte\",\"texto\":\"SOCO!\"}]}");
            fail("abriu sem travar");
        } catch (IllegalArgumentException esperado) {
            assertTrue(esperado.getMessage(), esperado.getMessage().contains("controle"));
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
