package com.gamelutagpt;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Lê os ultras de {@code app/src/main/assets}. */
final class AndroidRenderAssets implements RenderAssets {
    /** Imagens maiores que isso são reduzidas ao carregar, para poupar memória. */
    private static final int MAX_IMAGE_SIDE = 2048;

    static final class AndroidRenderImage implements RenderImage {
        final Bitmap bitmap;

        AndroidRenderImage(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        @Override
        public int width() {
            return bitmap.getWidth();
        }

        @Override
        public int height() {
            return bitmap.getHeight();
        }
    }

    private final AssetManager assets;

    AndroidRenderAssets(AssetManager assets) {
        this.assets = assets;
    }

    @Override
    public String readText(String path) throws IOException {
        try (InputStream in = assets.open(path)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    @Override
    public RenderImage loadImage(String path) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = assets.open(path)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / options.inSampleSize > MAX_IMAGE_SIDE) {
            options.inSampleSize *= 2;
        }
        Bitmap bitmap;
        try (InputStream in = assets.open(path)) {
            bitmap = BitmapFactory.decodeStream(in, null, options);
        }
        if (bitmap == null) throw new IOException("formato de imagem não suportado");
        return new AndroidRenderImage(bitmap);
    }
}
