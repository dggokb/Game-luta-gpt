package com.gamelutagpt.ultra;

import java.io.IOException;

/** Acesso aos arquivos de um ultra. Cada plataforma implementa o seu. */
public interface UltraAssets {
    String readText(String path) throws IOException;

    UltraImage loadImage(String path) throws IOException;
}
