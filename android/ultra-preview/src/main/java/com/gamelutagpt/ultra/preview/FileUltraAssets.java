package com.gamelutagpt.ultra.preview;

import com.gamelutagpt.ultra.UltraAssets;
import com.gamelutagpt.ultra.UltraImage;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.imageio.ImageIO;

/** Lê os arquivos do ultra direto do disco. */
public final class FileUltraAssets implements UltraAssets {
    private final File root;

    public FileUltraAssets(File root) {
        this.root = root;
    }

    @Override
    public String readText(String path) throws IOException {
        return new String(Files.readAllBytes(new File(root, path).toPath()), StandardCharsets.UTF_8);
    }

    @Override
    public UltraImage loadImage(String path) throws IOException {
        BufferedImage image = ImageIO.read(new File(root, path));
        if (image == null) throw new IOException("formato de imagem não suportado");
        return new Java2DImage(image);
    }
}
