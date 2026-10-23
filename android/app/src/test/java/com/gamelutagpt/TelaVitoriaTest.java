package com.gamelutagpt;

import static org.junit.Assert.*;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/** Tela de vitória: só quem tem a cena, toca até o preto e um toque pula para a saída. */
@RunWith(RobolectricTestRunner.class)
public class TelaVitoriaTest {
    private static TelaVitoria tela() {
        TelaVitoria t = new TelaVitoria(RuntimeEnvironment.getApplication().getAssets());
        t.manter("cenario_sem_fundo", "player_base", "p03");
        return t;
    }

    private static int tocarAteOFim(TelaVitoria t, int pularNoPasso) {
        Canvas c = new Canvas(Bitmap.createBitmap(64, 36, Bitmap.Config.ARGB_8888));
        int passos = 0;
        while (t.ativa() && passos < 2000) {
            if (passos == pularNoPasso) t.pular();
            t.desenhar(c);
            t.passo();
            passos++;
        }
        return passos;
    }

    @Test public void soQuemTemACenaTemATela() {
        TelaVitoria t = tela();
        assertTrue(t.tem("player_base"));
        assertFalse(t.tem("p03"));
        t.comecar("p03", 0xFF000000, "KŌGA", s -> s);
        assertFalse(t.ativa());
    }

    @Test public void tocaOVideoInteiroEAcaba() {
        TelaVitoria t = tela();
        t.comecar("player_base", 0xFF2F6FD6, "RYŪGA", s -> s);
        assertTrue(t.ativa());
        int passos = tocarAteOFim(t, -1);
        assertFalse(t.ativa());
        // 124 quadros a 36 fps (~3,4 s) com a entrada, o quadro parado e a saída: ~4,5 s.
        assertTrue("passos " + passos, passos > 250 && passos < 300);
    }

    @Test public void umToqueNoComecoNaoPulaDepoisPula() {
        TelaVitoria cedo = tela();
        cedo.comecar("player_base", 0xFF2F6FD6, "RYŪGA", s -> s);
        assertTrue(tocarAteOFim(cedo, 10) > 250);
        TelaVitoria depois = tela();
        depois.comecar("player_base", 0xFF2F6FD6, "RYŪGA", s -> s);
        int passos = tocarAteOFim(depois, 60);
        // Do toque vai direto para a saída (~0,3 s).
        assertTrue("passos " + passos, passos > 60 && passos < 60 + 25);
    }

    @Test public void quebraAFraseNaLarguraDoQuadro() {
        Paint p = new Paint();
        p.setTextSize(36f);
        List<String> linhas = new ArrayList<>();
        TelaVitoria.quebrar("Eu li cada movimento seu. Treine mais e volte.", p, 410f, linhas);
        assertTrue(linhas.size() >= 1);
        for (String linha : linhas) assertTrue(linha, p.measureText(linha) <= 410f || !linha.contains(" "));
        assertEquals("Eu li cada movimento seu. Treine mais e volte.", String.join(" ", linhas));
    }
}
