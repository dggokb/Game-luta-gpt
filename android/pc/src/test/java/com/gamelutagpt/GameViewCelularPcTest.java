package com.gamelutagpt;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;
import com.gamelutagpt.pc.CapturaPc;
import com.gamelutagpt.pc.DadosDoJogo;
import java.io.File;
import java.lang.reflect.Field;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * O caminho do celular: a arte carrega em segundo plano (menu na hora, folhas antecipadas) e
 * o pack do Ultra só vem quando alguém chega perto das 3 barras. Ativado antes do pack ficar
 * pronto, o Ultra espera com a tela andando e depois abre normalmente.
 */
public class GameViewCelularPcTest {
    private static CapturaPc jogo;

    @BeforeClass
    public static void abrir() throws Exception {
        File repo = new File(System.getProperty("gameluta.repo", "../.."));
        jogo = new CapturaPc(DadosDoJogo.doRepositorio(repo).contexto(null), 640, 360, true);
        jogo.comecar();
        jogo.esperar(1.0);
    }

    @AfterClass
    public static void fechar() {
        if (jogo != null) jogo.parar();
        // O próximo teste abre outro jogo na mesma JVM: este não pode ficar segurando a arte.
        jogo = null;
    }

    /**
     * Onde o jogo estava quando a luta não começou (a falha só aparece no runner Linux da CI):
     * telas abertas, carregamento, o lutador e o que as threads do jogo estavam fazendo.
     */
    private static String estado(GameView v, float antes) {
        StringBuilder sb = new StringBuilder();
        CombatFighter p = v.engine.fighter(GameView.PLAYER);
        sb.append("x ").append(antes).append(" -> ").append(p.x).append(", status ").append(p.status)
            .append(", locked ").append(p.locked);
        for (String nome : new String[] {"menuInicial", "characterSelect", "pausa", "capitulos"}) {
            try {
                Object o = campo(v, nome);
                java.lang.reflect.Method aberto = o.getClass().getDeclaredMethod("isOpen");
                aberto.setAccessible(true);
                sb.append(", ").append(nome).append(aberto.invoke(o));
            } catch (Exception e) {
                sb.append(", ").append(nome).append("?");
            }
        }
        try {
            java.lang.reflect.Method m = GameView.class.getDeclaredMethod("loadingActive");
            m.setAccessible(true);
            sb.append(", carregando ").append(m.invoke(v));
            sb.append(", arteCarregada ").append(campo(v, "arteCarregada"));
            sb.append(", folhas pendentes ").append(((SpriteAtlasCache) campo(v, "atlases")).pendentes());
        } catch (Exception e) {
            sb.append(", carregamento ?");
        }
        for (java.util.Map.Entry<Thread, StackTraceElement[]> t : Thread.getAllStackTraces().entrySet()) {
            String nome = t.getKey().getName();
            if (!nome.equals("GameLoop") && !nome.equals("Carregando") && !nome.equals("Folhas") &&
                !nome.equals("Antecipando")) continue;
            sb.append(" | ").append(nome).append(' ').append(t.getKey().getState());
            StackTraceElement[] pilha = t.getValue();
            for (int i = 0; i < Math.min(6, pilha.length); i++) sb.append(" < ").append(pilha[i]);
        }
        return sb.toString();
    }

    private static Object campo(GameView v, String nome) throws Exception {
        Field f = GameView.class.getDeclaredField(nome);
        f.setAccessible(true);
        return f.get(v);
    }

    @Test
    public void lutaEUltraComArteEmSegundoPlano() throws Exception {
        GameView v = jogo.jogo();
        // MODO TESTE: a arte da luta carrega (o que faltar da antecipação) e abre a seleção.
        jogo.tecla(KeyEvent.KEYCODE_W);
        jogo.tecla(KeyEvent.KEYCODE_J);
        CharacterSelect selecao = (CharacterSelect) campo(v, "characterSelect");
        long abre = System.currentTimeMillis() + 120_000;
        while (!selecao.isOpen() && System.currentTimeMillis() < abre) jogo.esperar(0.1);
        assertTrue("seleção aberta: " + estado(v, 0f), selecao.isOpen());
        jogo.esperar(0.5);
        for (int i = 0; i < 4; i++) {
            jogo.tecla(KeyEvent.KEYCODE_J);
            jogo.esperar(0.15);
        }
        jogo.tecla(KeyEvent.KEYCODE_ENTER);
        // Espera a luta: as folhas da dupla decodificam (em paralelo) com a tela de carregamento.
        long limite = System.currentTimeMillis() + 240_000;
        float antes = v.engine.fighter(GameView.PLAYER).x;
        // Segura a direita e repete o aperto de tempos em tempos, como a repetição do teclado:
        // a entrada da luta limpa as teclas seguradas (um aperto só, antes do fim do
        // carregamento, se perdia e o lutador nunca andava).
        jogo.segurar(KeyEvent.KEYCODE_D);
        long repete = System.currentTimeMillis() + 1000;
        while (v.engine.fighter(GameView.PLAYER).x < antes + 60f && System.currentTimeMillis() < limite) {
            jogo.esperar(0.1);
            if (System.currentTimeMillis() >= repete) {
                jogo.segurar(KeyEvent.KEYCODE_D);
                repete = System.currentTimeMillis() + 1000;
            }
        }
        jogo.soltar(KeyEvent.KEYCODE_D);
        assertTrue("a luta começou; " + estado(v, antes), v.engine.fighter(GameView.PLAYER).x >= antes + 60f);
        SpriteAtlasCache atlases = (SpriteAtlasCache) campo(v, "atlases");
        assertTrue("folhas da luta prontas", atlases.size() > 0);

        // O pack só é pedido quando alguém chega perto das 3 barras (no treino a barra pode
        // encher sozinha, então aqui não dá para exigir que ainda não haja nenhum).
        java.util.Map<?, ?> ultras = (java.util.Map<?, ?>) campo(v, "ultras");

        // 3 barras de uma vez e ↓ + SUPER no mesmo instante: o pack ainda não está pronto.
        v.engine.fighter(GameView.PLAYER).state.superMeter = CombatConfig.ULTRA_COST;
        long quadros = jogo.quadrosDesenhados();
        jogo.segurar(KeyEvent.KEYCODE_S);
        jogo.tecla(KeyEvent.KEYCODE_I);
        jogo.soltar(KeyEvent.KEYCODE_S);
        com.gamelutagpt.ultra.AberturaUltra abertura = (com.gamelutagpt.ultra.AberturaUltra) campo(v, "abertura");
        limite = System.currentTimeMillis() + 60_000;
        while (!abertura.isActive() && System.currentTimeMillis() < limite) jogo.esperar(0.02);
        assertTrue("a abertura do Ultra começou depois do pack", abertura.isActive());
        assertFalse("pack do jogador carregado", ultras.isEmpty());
        assertNotNull(ultras.get(v.engine.fighter(GameView.PLAYER).state.character.id));
        assertTrue("a tela não travou enquanto esperava", jogo.quadrosDesenhados() > quadros);
    }
}
