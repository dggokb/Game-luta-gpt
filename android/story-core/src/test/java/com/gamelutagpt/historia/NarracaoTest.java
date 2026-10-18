package com.gamelutagpt.historia;

import static org.junit.Assert.*;

import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/** O narrador do torneio: as falas do arquivo, a regra da prioridade e as traduções. */
public class NarracaoTest {
    private static final File ASSETS = new File("../app/src/main/assets");
    private static final float DT = 1f / 60f;

    /** Os eventos que a luta pede (NarradorLuta): todos precisam de fala. */
    static final String[] EVENTOS = {"inicio", "primeiro_golpe", "combo", "combo_grande", "super", "ultra",
        "guard_break", "combo_breaker", "defesa_perfeita", "agarrao", "punicao", "troca", "apuros", "dominando",
        "virada", "tempo", "ko", "ko_ultra", "vitoria", "perfeito", "tempo_esgotado", "empate"};

    private static final RenderAssets ARQUIVOS = new RenderAssets() {
        @Override public String readText(String path) throws IOException {
            return new String(Files.readAllBytes(new File(ASSETS, path).toPath()), StandardCharsets.UTF_8);
        }

        @Override public RenderImage loadImage(String path) throws IOException {
            throw new IOException("sem imagem no teste");
        }
    };

    private static Narracao narrador() {
        Narracao n = new Narracao(ARQUIVOS);
        assertTrue("narrador.json abre", n.carregar());
        return n;
    }

    private static Map<String, String> vars() {
        Map<String, String> v = new HashMap<>();
        for (String k : new String[] {"p1", "p2", "atacante", "defensor", "entra", "nome", "vencedor", "perdedor"}) {
            v.put(k, "Ryūga");
        }
        v.put("n", "7");
        return v;
    }

    private static void passa(Narracao n, float segundos) {
        for (int i = 0; i < Math.round(segundos * 60f); i++) n.update(DT);
    }

    @Test public void everyEventTheFightAsksForHasLinesAndAllVariablesFill() {
        Narracao n = narrador();
        for (String evento : EVENTOS) {
            assertTrue("fala para " + evento, n.tem(evento));
            for (String fala : n.eventos().get(evento).falas) {
                String cheia = Narracao.preencher(fala, vars());
                assertFalse(evento + ": variável sem valor em \"" + fala + "\"", cheia.contains("{"));
            }
        }
    }

    @Test public void theBoxIsCenteredAndClearOfTheTouchButtons() {
        float esq = Narracao.QUADRO[0];
        for (int i = 0; i < Narracao.QUADRO.length; i += 2) esq = Math.min(esq, Narracao.QUADRO[i]);
        float dir = Narracao.FAIXA_DIR + Narracao.PONTA;
        assertEquals("centralizada na tela de 1280", 640f, (esq + dir) * 0.5f, 1f);
        assertTrue("depois do direcional", esq > 375f);
        // Botão SUPER (870, 628, raio 54): a ponta da faixa não entra nele.
        float dy = 628f - Narracao.FAIXA_TOPO;
        assertTrue("antes do SUPER", dir < 870f - (float)Math.sqrt(54f * 54f - dy * dy));
    }

    @Test public void longLinesShrinkInsteadOfLosingTheirEnd() throws IOException {
        // O nome mais comprido do elenco em todas as variáveis, nos três idiomas.
        Map<String, String> v = new HashMap<>();
        for (String k : new String[] {"p1", "p2", "atacante", "defensor", "entra", "nome", "vencedor", "perdedor"}) {
            v.put(k, "Takatoshi");
        }
        v.put("n", "12");
        for (String idioma : new String[] {null, "en", "es"}) {
            Narracao n = narrador();
            if (idioma != null) {
                Temporada t = Temporada.load(ARQUIVOS);
                assertTrue(t.setIdioma(ARQUIVOS, idioma));
                n.setTemporada(t);
            }
            for (Map.Entry<String, Narracao.Evento> e : n.eventos().entrySet()) {
                for (int i = 0; i < e.getValue().falas.size() * 4; i++) {
                    n.stop();
                    passa(n, Narracao.REPETE_DEPOIS + 0.1f);
                    assertTrue(n.narrar(e.getKey(), v));
                    String texto = n.textoAtual();
                    String junto = String.join(" ", n.linhasNaFaixa(MEDIDA));
                    assertEquals(idioma + ": a fala inteira na faixa", texto.replaceAll("\\s+", " "), junto.replaceAll("\\s+", " "));
                }
            }
        }
    }

    /** Mede o texto como a fonte do jogo, mais ou menos (0,56 da altura por letra). */
    private static final RenderCanvas MEDIDA = new RenderCanvas() {
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
        @Override public void fillRadialGlow(float cx, float cy, float radius, int argb) {}
        @Override public void fillVerticalGradient(float l, float t, float r, float b, int top, int bottom) {}
        @Override public void fillOval(float l, float t, float r, float b, int argb) {}
        @Override public void drawText(String text, float x, float y, float size, int fill, int stroke, float sw, boolean italic) {}
        @Override public void beginInvert() {}
        @Override public void endInvert() {}
    };

    @Test public void everyExpressionHasItsPortrait() {
        Narracao n = narrador();
        for (Map.Entry<String, Narracao.Evento> e : n.eventos().entrySet()) {
            File retrato = new File(ASSETS, ConversaLuta.RETRATOS + "/" + Narracao.QUEM + "_" + e.getValue().expressao + ".png");
            assertTrue(e.getKey() + ": falta " + retrato, retrato.isFile());
        }
    }

    @Test public void aBigMomentCutsInAndACommentWaitsForSilence() {
        Narracao n = narrador();
        assertTrue(n.narrar("inicio", vars()));
        assertEquals("inicio", n.eventoAtual());
        // Comentário com alguém falando: fica de fora.
        assertFalse(n.narrar("primeiro_golpe", vars()));
        // O Ultra (grande) entra quando já está calado e corta um importante na hora.
        passa(n, 8f);
        assertNull(n.textoAtual());
        assertTrue(n.narrar("super", vars()));
        assertEquals("super", n.eventoAtual());
        assertTrue(n.narrar("ultra", vars()));
        assertEquals("o Ultra corta o Super", "ultra", n.eventoAtual());
    }

    @Test public void theKnockoutAndTheVictoryComeOneAfterTheOther() {
        Narracao n = narrador();
        n.narrar("ko", vars());
        n.narrar("vitoria", vars());
        assertEquals("ko", n.eventoAtual());
        for (int i = 0; i < 600 && "ko".equals(n.eventoAtual()); i++) n.update(DT);
        assertEquals("a vitória depois do K.O.", "vitoria", n.eventoAtual());
        assertTrue(n.textoAtual().contains("Ryūga"));
    }

    @Test public void theSameEventDoesNotRepeatRightAway() {
        Narracao n = narrador();
        assertTrue(n.narrar("super", vars()));
        passa(n, 5f);
        assertFalse("o mesmo evento espera", n.narrar("super", vars()));
        passa(n, Narracao.REPETE_DEPOIS);
        assertTrue(n.narrar("super", vars()));
    }

    @Test public void consecutiveLinesOfAnEventVary() {
        Narracao n = narrador();
        String antes = null;
        for (int i = 0; i < 4; i++) {
            assertTrue(n.narrar("vitoria", vars()));
            String agora = n.textoAtual();
            assertNotEquals("sorteia sem repetir a última", antes, agora);
            antes = agora;
            n.stop();
        }
    }

    @Test public void everyLineHasEnglishAndSpanish() throws IOException {
        Narracao n = narrador();
        for (String idioma : new String[] {"en", "es"}) {
            Map<String, String> traducao = Temporada.traducaoFromJson(
                ARQUIVOS.readText("historia/idiomas/" + idioma + ".json"));
            assertTrue(idioma + ": nome", traducao.containsKey("NARRADOR"));
            for (Map.Entry<String, Narracao.Evento> e : n.eventos().entrySet()) {
                for (String fala : e.getValue().falas) {
                    assertTrue(idioma + " sem tradução para: " + fala, traducao.containsKey(fala));
                    String t = traducao.get(fala);
                    // As variáveis continuam lá na tradução.
                    for (String var : new String[] {"{p1}", "{p2}", "{atacante}", "{defensor}", "{entra}", "{nome}",
                        "{vencedor}", "{perdedor}", "{n}"}) {
                        assertEquals(idioma + ": " + var + " em \"" + fala + "\"", fala.contains(var), t.contains(var));
                    }
                }
            }
        }
    }
}
