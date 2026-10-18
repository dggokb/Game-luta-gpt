package com.gamelutagpt.historia;

import static org.junit.Assert.*;

import com.gamelutagpt.render.RenderAssets;
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
