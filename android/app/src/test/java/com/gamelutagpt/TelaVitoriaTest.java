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
        t.manter("cenario_sem_fundo", "player_base", "p03", "koga_8");
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
        assertTrue(t.tem("p03"));
        assertFalse(t.tem("koga_8"));
        t.comecar("koga_8", 0xFF000000, "KŌGA", s -> s);
        assertFalse(t.ativa());
    }

    @Test public void imagemParadaFicaNaTelaEAcaba() {
        TelaVitoria t = tela();
        t.comecar("p03", 0xFFC62F2F, "KŌGA", s -> s);
        assertTrue(t.ativa());
        int passos = tocarAteOFim(t, -1);
        // 80 ms de entrada + 3,6 s parada + 280 ms de saída, a 60 Hz.
        assertTrue("passos " + passos, passos > 220 && passos < 260);
    }

    @Test public void emDuplaOParceiroEntraAtrasEACenaAcabaIgual() {
        TelaVitoria t = tela();
        t.comecar("player_base", "p03", 0xFF2F6FD6, "RYŪGA", s -> s);
        assertTrue(t.ativa());
        int passos = tocarAteOFim(t, -1);
        assertFalse(t.ativa());
        assertTrue("passos " + passos, passos > 220 && passos < 260);
        // Parceiro sem cena ou o mesmo lutador: sozinho, sem erro.
        TelaVitoria s2 = tela();
        s2.comecar("p03", "koga_8", 0xFFC62F2F, "KŌGA", s -> s);
        assertTrue(tocarAteOFim(s2, -1) > 220);
    }

    @Test public void umToqueNoComecoNaoPulaDepoisPula() {
        TelaVitoria cedo = tela();
        cedo.comecar("player_base", 0xFF2F6FD6, "RYŪGA", s -> s);
        assertTrue(tocarAteOFim(cedo, 10) > 220);
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
