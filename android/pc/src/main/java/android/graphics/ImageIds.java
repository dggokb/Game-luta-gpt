package android.graphics;

import java.awt.image.BufferedImage;
import java.util.WeakHashMap;

/**
 * Número de cada imagem para as chaves dos caches, sem segurar a imagem: um atlas que saiu
 * de cena (troca de dupla) é liberado mesmo com entradas antigas nos caches.
 */
final class ImageIds {
    private static final WeakHashMap<BufferedImage, Integer> IDS = new WeakHashMap<>();
    private static int next;

    private ImageIds() {}

    static synchronized int of(BufferedImage image) {
        Integer id = IDS.get(image);
        if (id == null) IDS.put(image, id = ++next);
        return id;
    }
}
