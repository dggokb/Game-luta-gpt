package android.graphics;

import java.io.File;
import java.nio.file.Files;
import java.security.MessageDigest;

/**
 * Só no modo sem Gradle (tools/sem-gradle), onde não há o plugin de WEBP do TwelveMonkeys: converte
 * pelo Pillow (python3) e guarda o PNG por hash do arquivo (-Dgameluta.webpcache, padrão no tmp).
 */
final class WebpPonte {
    private static final File CACHE = new File(System.getProperty("gameluta.webpcache", System.getProperty("java.io.tmpdir") + "/sokon-webp"));
    static byte[] png(byte[] data) {
        if (data.length < 12 || data[0] != 'R' || data[1] != 'I' || data[8] != 'W' || data[9] != 'E') return data;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            StringBuilder h = new StringBuilder();
            for (byte b : md.digest(data)) h.append(String.format("%02x", b));
            File png = new File(CACHE, h + ".png");
            CACHE.mkdirs();
            if (!png.isFile()) {
                File in = File.createTempFile("webp", ".webp", CACHE), out = new File(CACHE, in.getName() + ".png");
                Files.write(in.toPath(), data);
                Process p = new ProcessBuilder("python3", "-c",
                    "import sys\nfrom PIL import Image\nImage.open(sys.argv[1]).convert('RGBA').save(sys.argv[2],compress_level=1)",
                    in.getPath(), out.getPath()).inheritIO().start();
                if (p.waitFor() != 0) throw new IllegalStateException("conversão falhou");
                in.delete();
                out.renameTo(png);
            }
            return Files.readAllBytes(png.toPath());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
