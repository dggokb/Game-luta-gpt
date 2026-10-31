package android.content.res;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * PC: os drawables (atlas dos lutadores) são as imagens (WEBP/PNG) de uma pasta, como
 * {@code res/drawable-nodpi}. O id de cada um é a posição na lista, a partir de 1.
 *
 * <p>Pré-carga: o jogo pede os atlas de um personagem um por um. Quando chega o primeiro
 * de um prefixo (ex.: {@code p03_}), todos os irmãos começam a ser decodificados em
 * paralelo, e os pedidos seguintes já encontram a imagem pronta.
 */
public class Resources {
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String name) { super(name); }
    }

    /** Pré-carga não usada em 2 minutos é descartada (memória). */
    private static final long PREFETCH_TTL_MS = 120_000;
    private static final boolean PREFETCH = !Boolean.getBoolean("gameluta.semPrecarga");

    private static final class Prefetched {
        final Future<Bitmap> bitmap;
        final long createdAt = System.currentTimeMillis();

        Prefetched(Future<Bitmap> bitmap) { this.bitmap = bitmap; }
    }

    private final File drawableDir;
    private final Map<String, Integer> ids = new HashMap<>();
    private final List<File> files = new ArrayList<>();
    private final Map<String, Prefetched> prefetched = new HashMap<>();
    /**
     * Quando a pré-carga de cada personagem começou. Um pedido avulso depois disso (a folha da
     * vitória no meio da luta) decodificava de novo todas as outras folhas dele, que ficavam 2
     * minutos esperando e viravam lixo: 9 GB alocados em 30 s de luta, e o coletor parava o jogo.
     */
    private final Map<String, Long> prefixosIniciados = new HashMap<>();
    /**
     * Duas filas de pré-carga. Rápida (um por núcleo): alguém espera pelas folhas (a tela de
     * carregamento). Lenta (metade dos núcleos até 4, na prioridade mais baixa): só antecipação,
     * com a seleção na tela; com um por núcleo ela caía para 20–30 quadros por segundo. Um
     * pedido com pressa passa o que estava na fila lenta para a rápida.
     */
    private ExecutorService poolRapido;
    private java.util.concurrent.ThreadPoolExecutor poolLento;

    private final android.util.DisplayMetrics metrics = android.util.DisplayMetrics.doPc();

    public Resources(File drawableDir) {
        this.drawableDir = drawableDir;
    }

    public android.util.DisplayMetrics getDisplayMetrics() { return metrics; }

    public int getIdentifier(String name, String defType, String defPackage) {
        if (!"drawable".equals(defType) || name == null) return 0;
        synchronized (ids) {
            Integer id = ids.get(name);
            if (id != null) return id;
            File f = drawableFile(name);
            if (f == null) return 0;
            files.add(f);
            ids.put(name, files.size());
            return files.size();
        }
    }

    public InputStream openRawResource(int id) {
        File f = fileOf(id);
        try {
            return Files.newInputStream(f.toPath());
        } catch (IOException e) {
            throw new NotFoundException(f.getPath());
        }
    }

    /** Extensão do PC: decodifica o drawable, usando a pré-carga paralela quando houver. */
    public Bitmap decodeDrawable(int id) {
        File f = fileOf(id);
        String name = baseName(f);
        if (!PREFETCH) return BitmapFactory.decodeFile(f.getPath());
        Prefetched p;
        synchronized (prefetched) {
            dropStale();
            p = prefetched.remove(name);
            // Pedido que não estava na pré-carga: começa o personagem dele (os irmãos que faltam).
            if (p == null) startSiblings(name);
        }
        if (p == null) return BitmapFactory.decodeFile(f.getPath());
        if (!antecipando() && !p.bitmap.isDone()) apressar();
        try {
            return p.bitmap.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return BitmapFactory.decodeFile(f.getPath());
        } catch (ExecutionException e) {
            return BitmapFactory.decodeFile(f.getPath());
        }
    }

    private void startSiblings(String name) {
        int cut = name.indexOf('_');
        if (cut <= 0) return;
        String prefix = name.substring(0, cut + 1);
        long agora = System.currentTimeMillis();
        Long desde = prefixosIniciados.get(prefix);
        if (desde != null && agora - desde < PREFETCH_TTL_MS) return;
        prefixosIniciados.put(prefix, agora);
        String[] names = drawableDir.list();
        if (names == null) return;
        boolean devagar = antecipando();
        ExecutorService pool = devagar ? poolLento() : poolRapido();
        if (!devagar) apressar();
        for (String file : names) {
            if (!file.startsWith(prefix) || !(file.endsWith(".png") || file.endsWith(".webp"))) continue;
            String sibling = baseName(new File(file));
            if (sibling.equals(name) || prefetched.containsKey(sibling)) continue;
            String path = new File(drawableDir, file).getPath();
            prefetched.put(sibling, new Prefetched(pool.submit(() -> BitmapFactory.decodeFile(path))));
        }
    }

    /** A thread decodifica só uma antecipação (o SpriteAtlasCache marca no nome dela). */
    private static boolean antecipando() {
        return Thread.currentThread().getName().endsWith(" antecipando");
    }

    private ExecutorService poolRapido() {
        if (poolRapido == null) {
            int threads = Math.max(2, Runtime.getRuntime().availableProcessors());
            poolRapido = Executors.newFixedThreadPool(threads, r -> {
                Thread t = new Thread(r, "PreCargaAtlas");
                t.setDaemon(true);
                t.setPriority(Thread.NORM_PRIORITY - 1);
                return t;
            });
        }
        return poolRapido;
    }

    private ExecutorService poolLento() {
        if (poolLento == null) {
            int nucleos = Runtime.getRuntime().availableProcessors();
            // -Dgameluta.precarga=N fixa quantas.
            int threads = Math.max(1, Integer.getInteger("gameluta.precarga", Math.min(4, nucleos / 2)));
            poolLento = new java.util.concurrent.ThreadPoolExecutor(threads, threads, 0L, java.util.concurrent.TimeUnit.MILLISECONDS,
                new java.util.concurrent.LinkedBlockingQueue<>(), r -> {
                    Thread t = new Thread(r, "PreCargaAtlas lenta");
                    t.setDaemon(true);
                    t.setPriority(Thread.MIN_PRIORITY);
                    return t;
                });
        }
        return poolLento;
    }

    /** Alguém espera: o que ainda está na fila lenta vai para a rápida (o mesmo Future). */
    private void apressar() {
        if (poolLento == null || poolLento.getQueue().isEmpty()) return;
        List<Runnable> fila = new ArrayList<>();
        poolLento.getQueue().drainTo(fila);
        ExecutorService rapido = poolRapido();
        for (Runnable r : fila) rapido.execute(r);
    }

    /** Joga fora pré-cargas velhas (atlas que o jogo não chegou a pedir). */
    private void dropStale() {
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<String, Prefetched>> it = prefetched.entrySet().iterator(); it.hasNext(); ) {
            Prefetched p = it.next().getValue();
            if (now - p.createdAt > PREFETCH_TTL_MS) {
                p.bitmap.cancel(false);
                it.remove();
            }
        }
    }

    private File fileOf(int id) {
        synchronized (ids) {
            if (id <= 0 || id > files.size()) throw new NotFoundException("#" + id);
            return files.get(id - 1);
        }
    }

    private File drawableFile(String name) {
        for (String ext : new String[] {".png", ".webp", ".jpg"}) {
            File f = new File(drawableDir, name + ext);
            if (f.isFile()) return f;
        }
        return null;
    }

    private static String baseName(File f) {
        String n = f.getName();
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }
}
