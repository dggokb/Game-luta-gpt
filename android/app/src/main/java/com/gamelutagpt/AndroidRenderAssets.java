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
        private Bitmap bitmap;
        private final int largura, altura;
        /** De onde ela veio (para decodificar de novo depois de solta) e quem a carregou. */
        private final String caminho;
        private final AndroidRenderAssets dono;

        AndroidRenderImage(Bitmap bitmap) {
            this(bitmap, null, null);
        }

        AndroidRenderImage(Bitmap bitmap, String caminho, AndroidRenderAssets dono) {
            this.bitmap = bitmap;
            this.largura = bitmap.getWidth();
            this.altura = bitmap.getHeight();
            this.caminho = caminho;
            this.dono = dono;
        }

        /**
         * O bitmap para desenhar. Uma imagem da luta solta depois de subir para o OpenGL é
         * decodificada de novo se o Canvas pedir (o OpenGL desligado na luta seguinte).
         */
        Bitmap bitmap() {
            if (bitmap.isRecycled() && dono != null && caminho != null) {
                try {
                    bitmap = dono.decodificar(caminho);
                } catch (IOException e) {
                    android.util.Log.w("AndroidRenderAssets", "imagem não voltou: " + caminho, e);
                }
            }
            return bitmap;
        }

        /** O bitmap como está (talvez já solto): o OpenGL acha a textura por ele. */
        Bitmap bruto() {
            return bitmap;
        }

        /** Pode ser solta depois de subir para o OpenGL (cenário e Ultra). */
        boolean soltaNoOpenGl() {
            return dono != null && dono.daLuta;
        }

        @Override
        public int width() {
            return largura;
        }

        @Override
        public int height() {
            return altura;
        }
    }

    private final AssetManager assets;
    /**
     * Imagens da luta (cenário, Ultras): com a luta em OpenGL elas são decodificadas na memória
     * comum, sobem para a placa de vídeo e o bitmap é solto, como as folhas (antes ficavam na
     * placa de vídeo duas vezes: o Bitmap HARDWARE e a textura).
     */
    final boolean daLuta;

    AndroidRenderAssets(AssetManager assets) {
        this(assets, false);
    }

    AndroidRenderAssets(AssetManager assets, boolean daLuta) {
        this.assets = assets;
        this.daLuta = daLuta;
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
        return new AndroidRenderImage(decodificar(path), path, this);
    }

    private Bitmap decodificar(String path) throws IOException {
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
        Bitmap bitmap = null;
        boolean placa = SpriteAtlasCache.telaPelaGpu && !(daLuta && SpriteAtlasCache.paraOpenGl);
        if (placa && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Cenário (camadas do tamanho da tela, várias por quadro), Ultra e páginas: direto na
            // placa de vídeo, como as folhas dos lutadores. Na memória comum o sistema as copia
            // para a placa ao desenhar e de novo sempre que saem do cache de texturas.
            options.inPreferredConfig = Bitmap.Config.HARDWARE;
            try (InputStream in = assets.open(path)) {
                bitmap = BitmapFactory.decodeStream(in, null, options);
            } catch (RuntimeException | OutOfMemoryError e) {
                bitmap = null;
            }
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        }
        if (bitmap == null) {
            try (InputStream in = assets.open(path)) {
                bitmap = BitmapFactory.decodeStream(in, null, options);
            }
        }
        if (bitmap == null) throw new IOException("formato de imagem não suportado");
        return bitmap;
    }
}
