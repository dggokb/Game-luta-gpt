package android.content.res;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * PC: os assets vêm de pastas no disco. Cada raiz cobre um prefixo de caminho, para o
 * jogo em desenvolvimento ler direto do repositório (ex.: {@code selection/portraits/}
 * sai de {@code art/concepts}) e o jogo exportado ler da sua pasta {@code data/}.
 */
public class AssetManager {
    private static final class Root {
        final String prefix;
        final File dir;

        Root(String prefix, File dir) {
            this.prefix = prefix;
            this.dir = dir;
        }
    }

    private final List<Root> roots = new ArrayList<>();

    /** Extensão do PC: tudo sob {@code prefix} ("" para a raiz) mora em {@code dir}. */
    public AssetManager addRoot(String prefix, File dir) {
        String p = prefix.isEmpty() || prefix.endsWith("/") ? prefix : prefix + "/";
        roots.add(0, new Root(p, dir));
        return this;
    }

    /** Extensão do PC: o arquivo do asset, ou null. */
    public File file(String path) {
        String clean = normalize(path);
        for (Root root : roots) {
            if (!clean.startsWith(root.prefix)) continue;
            File f = new File(root.dir, clean.substring(root.prefix.length()));
            if (f.isFile()) return f;
        }
        return null;
    }

    public InputStream open(String path) throws IOException {
        File f = file(path);
        if (f == null) throw new FileNotFoundException(path);
        return Files.newInputStream(f.toPath());
    }

    public AssetFileDescriptor openFd(String path) throws IOException {
        File f = file(path);
        if (f == null) throw new FileNotFoundException(path);
        return new AssetFileDescriptor(f);
    }

    /** Como no Android: nomes dentro da pasta, vazio quando ela não existe. */
    public String[] list(String path) throws IOException {
        String clean = normalize(path);
        String folder = clean.isEmpty() || clean.endsWith("/") ? clean : clean + "/";
        TreeSet<String> names = new TreeSet<>();
        for (Root root : roots) {
            File dir;
            if (folder.startsWith(root.prefix)) {
                dir = new File(root.dir, folder.substring(root.prefix.length()));
            } else if (root.prefix.startsWith(folder)) {
                // A pasta pedida contém o prefixo desta raiz: aparece a primeira parte dele.
                String rest = root.prefix.substring(folder.length());
                names.add(rest.substring(0, rest.indexOf('/')));
                continue;
            } else {
                continue;
            }
            String[] children = dir.list();
            if (children != null) for (String child : children) names.add(child);
        }
        return names.toArray(new String[0]);
    }

    public void close() {}

    private static String normalize(String path) {
        String p = path.replace('\\', '/');
        while (p.startsWith("/")) p = p.substring(1);
        if (p.contains("..")) throw new IllegalArgumentException("caminho de asset inválido: " + path);
        return p;
    }
}
