package com.gamelutagpt;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.KeyEvent;
import com.gamelutagpt.pc.CapturaPc;
import com.gamelutagpt.pc.DadosDoJogo;
import java.awt.image.BufferedImage;
import java.io.File;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * O jogo de verdade no PC, sem janela: abre na seleção, escolhe as duplas pelo teclado,
 * entra na luta e anda/ataca. Mesmo GameView do Android, pela camada de compatibilidade.
 */
public class GameViewPcTest {
    private static CapturaPc jogo;

    @BeforeClass
    public static void abrir() throws Exception {
        File repo = new File(System.getProperty("gameluta.repo", "../.."));
        jogo = new CapturaPc(DadosDoJogo.doRepositorio(repo).contexto(null), 640, 360);
        jogo.comecar();
        jogo.esperar(1.0);
    }

    @AfterClass
    public static void fechar() {
        if (jogo != null) jogo.parar();
    }

    @Test
    public void selecaoLutaEControles() throws Exception {
        GameView v = jogo.jogo();
        assertFalse("menu inicial desenhado", uniforme(jogo.quadro()));
        // MODO TESTE: baixo + A no menu inicial.
        jogo.tecla(KeyEvent.KEYCODE_S);
        jogo.tecla(KeyEvent.KEYCODE_J);
        jogo.esperar(1.5);
        assertFalse("seleção desenhada", uniforme(jogo.quadro()));
        for (int i = 0; i < 4; i++) {
            jogo.tecla(KeyEvent.KEYCODE_J);
            jogo.esperar(0.15);
        }
        jogo.tecla(KeyEvent.KEYCODE_ENTER);
        jogo.esperar(1.0);
        assertFalse("luta desenhada", uniforme(jogo.quadro()));

        // Segurando D: anda assim que a intro (READY/GO) libera os lutadores.
        float antes = v.engine.fighter(GameView.PLAYER).x;
        jogo.segurar(KeyEvent.KEYCODE_D);
        long limite = System.currentTimeMillis() + 25_000;
        while (v.engine.fighter(GameView.PLAYER).x < antes + 60f && System.currentTimeMillis() < limite) jogo.esperar(0.1);
        jogo.soltar(KeyEvent.KEYCODE_D);
        jogo.esperar(0.2);
        float depois = v.engine.fighter(GameView.PLAYER).x;
        assertTrue("andou para a frente: " + antes + " → " + depois, depois >= antes + 60f);

        jogo.segurar(KeyEvent.KEYCODE_A);
        jogo.esperar(0.5);
        jogo.soltar(KeyEvent.KEYCODE_A);
        jogo.esperar(0.2);
        assertTrue("voltou", v.engine.fighter(GameView.PLAYER).x < depois - 20f);

        // Esc abre a pausa: a luta para.
        jogo.tecla(KeyEvent.KEYCODE_ESCAPE);
        jogo.esperar(0.3);
        float pausado = v.engine.fighter(GameView.PLAYER).x;
        jogo.segurar(KeyEvent.KEYCODE_D);
        jogo.esperar(0.4);
        jogo.soltar(KeyEvent.KEYCODE_D);
        assertTrue("parado na pausa", Math.abs(v.engine.fighter(GameView.PLAYER).x - pausado) < 1f);
        assertTrue(jogo.quadrosDesenhados() > 100);
    }

    private static boolean uniforme(BufferedImage img) {
        int first = img.getRGB(0, 0);
        int diferentes = 0;
        for (int y = 0; y < img.getHeight(); y += 7)
            for (int x = 0; x < img.getWidth(); x += 7)
                if (img.getRGB(x, y) != first) diferentes++;
        return diferentes < 50;
    }
}
