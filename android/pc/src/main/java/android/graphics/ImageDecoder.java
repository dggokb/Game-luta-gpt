package android.graphics;

import android.content.res.Resources;
import android.util.Size;
import java.io.IOException;

/**
 * PC: o ImageDecoder do Android 9 no que o jogo usa (fonte de um drawable, tamanho alvo).
 * Decodifica inteiro e reduz por média de área, que é o que o redimensionador do libwebp faz
 * ao decodificar em escala no celular.
 */
public final class ImageDecoder {
    public static final int ALLOCATOR_DEFAULT = 0, ALLOCATOR_SOFTWARE = 1, ALLOCATOR_SHARED_MEMORY = 2, ALLOCATOR_HARDWARE = 3;

    public static final class Source {
        final Resources resources;
        final int id;

        Source(Resources resources, int id) { this.resources = resources; this.id = id; }
    }

    public static final class ImageInfo {
        private final Size size;

        ImageInfo(Size size) { this.size = size; }

        public Size getSize() { return size; }

        public String getMimeType() { return "image/webp"; }
    }

    public interface OnHeaderDecodedListener {
        void onHeaderDecoded(ImageDecoder decoder, ImageInfo info, Source source);
    }

    private int targetWidth, targetHeight;

    private ImageDecoder() {}

    public static Source createSource(Resources res, int resId) { return new Source(res, resId); }

    public void setAllocator(int allocator) {}

    public void setTargetSize(int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("tamanho alvo inválido");
        targetWidth = width;
        targetHeight = height;
    }

    public static Bitmap decodeBitmap(Source source, OnHeaderDecodedListener listener) throws IOException {
        Bitmap full = source.resources.decodeDrawable(source.id);
        if (full == null) throw new IOException("não decodificou #" + source.id);
        ImageDecoder decoder = new ImageDecoder();
        if (listener != null) listener.onHeaderDecoded(decoder, new ImageInfo(new Size(full.getWidth(), full.getHeight())), source);
        if (decoder.targetWidth == 0 || (decoder.targetWidth == full.getWidth() && decoder.targetHeight == full.getHeight())) return full;
        Bitmap reduzido = new Bitmap(BitmapFactory.reduzir(full.image(), decoder.targetWidth, decoder.targetHeight));
        full.recycle();
        return reduzido;
    }
}
