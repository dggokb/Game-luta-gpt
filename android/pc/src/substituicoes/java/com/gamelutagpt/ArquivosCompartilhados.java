package com.gamelutagpt;

import android.content.Context;
import android.util.Log;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Versão de PC do "Compartilhar" do Android: grava o arquivo em
 * {@code Documentos/GameLutaGPT} (ou {@code ~/GameLutaGPT}) e abre a pasta no explorador,
 * de onde ele vai para o Drive como qualquer arquivo.
 *
 * <p>Substitui {@code app/.../ArquivosCompartilhados.java} só no build de PC (ver
 * {@code pc/src/substituicoes}); a mesma assinatura que o GameView chama.
 */
public final class ArquivosCompartilhados {
    private ArquivosCompartilhados() {}

    static void compartilhar(Context context, String fileName, String mime, String text) throws IOException {
        if (fileName.contains("/") || fileName.contains("\\") || fileName.startsWith(".")) throw new IOException("nome inválido");
        File dir = pasta();
        Files.createDirectories(dir.toPath());
        File file = new File(dir, fileName);
        Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
        Log.i("Arquivos", "salvo em " + file.getAbsolutePath());
        if (!java.awt.GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported()
            && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            // O explorador abre fora da thread do jogo.
            Thread t = new Thread(() -> {
                try {
                    Desktop.getDesktop().open(dir);
                } catch (IOException | RuntimeException e) {
                    Log.w("Arquivos", "não abriu a pasta " + dir, e);
                }
            }, "AbrirPasta");
            t.setDaemon(true);
            t.start();
        }
    }

    static File pasta() {
        String custom = System.getProperty("gameluta.arquivos");
        if (custom != null) return new File(custom);
        File home = new File(System.getProperty("user.home"));
        File docs = new File(home, "Documents");
        return new File(docs.isDirectory() ? docs : home, "GameLutaGPT");
    }
}
