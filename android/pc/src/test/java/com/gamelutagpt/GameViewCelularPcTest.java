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
        assertTrue("seleção aberta", selecao.isOpen());
        jogo.esperar(0.5);
        for (int i = 0; i < 4; i++) {
            jogo.tecla(KeyEvent.KEYCODE_J);
            jogo.esperar(0.15);
        }
        jogo.tecla(KeyEvent.KEYCODE_ENTER);
        // Espera a luta: as folhas da dupla decodificam (em paralelo) com a tela de carregamento.
        long limite = System.currentTimeMillis() + 240_000;
        float antes = v.engine.fighter(GameView.PLAYER).x;
        jogo.segurar(KeyEvent.KEYCODE_D);
        while (v.engine.fighter(GameView.PLAYER).x < antes + 60f && System.currentTimeMillis() < limite) jogo.esperar(0.1);
        jogo.soltar(KeyEvent.KEYCODE_D);
        assertTrue("a luta começou", v.engine.fighter(GameView.PLAYER).x >= antes + 60f);
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
