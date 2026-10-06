package com.gamelutagpt.ultra.preview;

import com.gamelutagpt.ultra.UltraImage;
import java.awt.image.BufferedImage;

final class Java2DImage implements UltraImage {
    final BufferedImage image;

    Java2DImage(BufferedImage image) {
        this.image = image;
    }

    @Override
    public int width() {
        return image.getWidth();
    }

    @Override
    public int height() {
        return image.getHeight();
    }
}
