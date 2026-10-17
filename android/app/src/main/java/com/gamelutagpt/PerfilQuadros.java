package com.gamelutagpt;

import android.util.Log;
import java.util.Locale;

/**
 * Para onde vai o tempo de cada quadro, por parte, somado a cada {@link #JANELA_NS}. Vai para o
 * log com a tag {@value #TAG} (no celular: {@code adb logcat -s Quadros}), uma linha por janela:
 * <pre>Quadros: 48 fps | quadro 20,8 ms (pior 41) | atualiza 1,2 trava 0,1 fundo 9,3 ...</pre>
 * "trava" e "envio" são a espera pela tela (lockCanvas / unlockCanvasAndPost): no celular, quando
 * a GPU não dá conta, o tempo aparece nelas. Só a thread do jogo mexe aqui.
 */
final class PerfilQuadros {
    static final String TAG = "Quadros";
    static final long JANELA_NS = 2_000_000_000L;

    static final int ATUALIZA = 0, TRAVA = 1, TELA = 2, FUNDO = 3, LUTA = 4, FRENTE = 5, HUD = 6, ENVIO = 7;
    private static final String[] NOMES = {"atualiza", "trava", "tela", "fundo", "luta", "frente", "hud", "envio"};

    private final long[] soma = new long[NOMES.length];
    private long marca, inicioQuadro, inicioJanela = System.nanoTime(), pior, quadros, quadrosLuta;
    private boolean luta;
    private volatile String ultima = "";
    /** A última janela fechada: quadros por segundo e a fração deles que foi luta (com cenário). */
    private float fps, fracaoLuta;

    /** Começo de um quadro (antes de atualizar a simulação). */
    void inicio() {
        marca = inicioQuadro = System.nanoTime();
    }

    /** Fecha a parte {@code parte}: o tempo desde a marca anterior vai para ela. */
    void marca(int parte) {
        if (parte == FUNDO) luta = true;
        long agora = System.nanoTime();
        soma[parte] += agora - marca;
        marca = agora;
    }

    /** Fim do quadro: conta e, a cada janela, escreve a linha no log. True quando uma janela fechou. */
    boolean fim() {
        long agora = System.nanoTime();
        pior = Math.max(pior, agora - inicioQuadro);
        quadros++;
        if (luta) quadrosLuta++;
        luta = false;
        long janela = agora - inicioJanela;
        if (janela < JANELA_NS) return false;
        fps = (float) (quadros * 1e9 / janela);
        fracaoLuta = quadrosLuta / (float) quadros;
        StringBuilder linha = new StringBuilder(String.format(Locale.ROOT, "%.0f fps | quadro %.1f ms (pior %.0f) |",
            quadros * 1e9 / janela, ocupado() / 1e6 / quadros, pior / 1e6));
        for (int i = 0; i < soma.length; i++) {
            if (soma[i] == 0) continue;
            linha.append(String.format(Locale.ROOT, " %s %.1f", NOMES[i], soma[i] / 1e6 / quadros));
        }
        ultima = linha.toString();
        Log.i(TAG, ultima);
        java.util.Arrays.fill(soma, 0);
        pior = quadros = quadrosLuta = 0;
        inicioJanela = agora;
        return true;
    }

    /** Quadros por segundo da última janela. */
    float fps() { return fps; }

    /** Fração dos quadros da última janela que foram luta (0..1). */
    float fracaoLuta() { return fracaoLuta; }

    /** Recomeça a janela (depois de um carregamento ou de voltar do segundo plano). */
    void recomecar() {
        java.util.Arrays.fill(soma, 0);
        pior = quadros = quadrosLuta = 0;
        luta = false;
        inicioJanela = System.nanoTime();
    }

    private long ocupado() {
        long total = 0;
        for (long s : soma) total += s;
        return total;
    }

    /** A última linha (para o painel de DEBUG e as ferramentas do PC). */
    String ultima() { return ultima; }
}
