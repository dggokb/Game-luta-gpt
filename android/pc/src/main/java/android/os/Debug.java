package android.os;

/** PC: o "heap nativo" é o que os bitmaps decodificados ocupam (onde o Android 8+ os guarda). */
public final class Debug {
    private Debug() {}

    public static long getNativeHeapAllocatedSize() { return android.graphics.Bitmap.bytesVivos(); }

    /** PC: sem as contas do ART (o coletor do PC aparece no -Pgc). */
    public static String getRuntimeStat(String nome) { return null; }
}
