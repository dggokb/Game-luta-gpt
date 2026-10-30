package com.gamelutagpt;

/** PC: se a luta pode ir pela placa de vídeo aqui (fora do macOS, sem -Dgameluta.opengl=false). */
public final class OpenGlPc {
    private OpenGlPc() {}

    public static boolean permitido() {
        return DesenhoGl.permitido();
    }
}
