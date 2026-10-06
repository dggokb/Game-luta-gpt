package com.gamelutagpt.render;

import java.io.IOException;

/** Acesso aos arquivos (ultras, cenários). Cada plataforma implementa o seu. */
public interface RenderAssets {
    String readText(String path) throws IOException;

    RenderImage loadImage(String path) throws IOException;
}
