package com.gamelutagpt.preview;

import com.gamelutagpt.render.RenderImage;
import java.awt.image.BufferedImage;

final class Java2DImage implements RenderImage {
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
