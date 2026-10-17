package com.gamelutagpt;

import android.util.Log;
import java.util.Locale;

/**
 * Tempo e memória de cada parte de um carregamento pesado (arte da luta, troca de dupla).
 * Vai para o log com a tag {@value #TAG}: no celular, {@code adb logcat -s Carregamento}
 * mostra uma linha por carregamento, por exemplo
 * <pre>arte: hud 12ms; folhas 3400ms +1210MB; ... total 5100ms; atlas 1210MB em 120 folhas</pre>
 * A memória de cada parte é o que o heap nativo cresceu nela (onde o Android 8+ guarda os
 * pixels dos bitmaps); no PC é o que os bitmaps decodificados ocupam.
 */
final class MedicaoCarregamento {
    static final String TAG = "Carregamento";
    private static volatile String ultima = "";

    private final String nome;
    private final long inicio;
    private long marca;
    private long memoriaMarca;
    private final StringBuilder partes = new StringBuilder();

    MedicaoCarregamento(String nome) {
        this.nome = nome;
        inicio = marca = System.nanoTime();
        memoriaMarca = memoria();
    }

    /** Fecha a parte que terminou agora (desde a anterior). */
    void parte(String parte) {
        long agora = System.nanoTime();
        long memoriaAgora = memoria();
        if (partes.length() > 0) partes.append("; ");
        partes.append(parte).append(' ').append((agora - marca) / 1_000_000L).append("ms");
        long delta = memoriaAgora - memoriaMarca;
        if (Math.abs(delta) >= 1L << 20) partes.append(String.format(Locale.ROOT, " %+dMB", delta >> 20));
        marca = agora;
        memoriaMarca = memoriaAgora;
    }

    /** Escreve a linha no log; {@code atlases} (opcional) diz quanto as folhas ocupam. */
    void fim(SpriteAtlasCache atlases) {
        String linha = nome + ": " + partes + "; total " + (System.nanoTime() - inicio) / 1_000_000L + "ms"
            + (atlases == null ? "" : String.format(Locale.ROOT, "; atlas %dMB em %d folhas",
                atlases.decodedBytes() >> 20, atlases.size()));
        ultima = linha;
        Log.i(TAG, linha);
    }

    /** A última linha escrita (para as ferramentas de medição do PC). */
    static String ultima() { return ultima; }

    private static long memoria() {
        return android.os.Debug.getNativeHeapAllocatedSize();
    }
}
