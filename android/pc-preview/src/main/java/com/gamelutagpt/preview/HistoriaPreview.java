package com.gamelutagpt.preview;

import com.gamelutagpt.historia.Episodio;
import com.gamelutagpt.historia.HistoriaListener;
import com.gamelutagpt.historia.PaginaHistoria;
import com.gamelutagpt.historia.Temporada;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Grava em PNG cada página dos episódios do modo história já montada (todos os painéis e
 * balões), o cartão de título e o "próximo episódio", para revisar texto e diagramação
 * sem abrir o jogo. As lutas viram uma vitória automática.
 *
 * <pre>gradle :pc-preview:runHistoria</pre>
 * ou, com argumentos: {@code <pasta assets> <pasta de saída> [id do episódio]}.
 */
public final class HistoriaPreview implements HistoriaListener {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final float DT = 1f / 60f;

    private final PaginaHistoria pagina;
    private final File outDir;
    private String episodeId;
    private int shot;
    private boolean fightRequested;
    private boolean finished;

    private HistoriaPreview(FileRenderAssets assets, File outDir) {
        this.pagina = new PaginaHistoria(this, assets);
        this.outDir = outDir;
    }

    public static void main(String[] args) throws IOException {
        File assetsDir = new File(args.length > 0 ? args[0] : "app/src/main/assets");
        File outDir = new File(args.length > 1 ? args[1] : "build/historia");
        if (!outDir.isDirectory() && !outDir.mkdirs()) throw new IOException("não consegui criar " + outDir);
        FileRenderAssets assets = new FileRenderAssets(assetsDir);
        Temporada temporada = Temporada.load(assets);
        // -Didioma=en grava as páginas em inglês (historia/idiomas/en.json).
        String idioma = System.getProperty("idioma", "pt");
        if (!temporada.setIdioma(assets, idioma)) System.out.println("sem tradução para " + idioma);
        HistoriaPreview preview = new HistoriaPreview(assets, outDir);
        for (String id : temporada.episodios) {
            if (args.length > 2 && !args[2].equals(id)) continue;
            preview.play(temporada, temporada.loadEpisodio(assets, id));
        }
        System.out.println("Páginas gravadas em " + outDir.getAbsolutePath());
    }

    private void play(Temporada temporada, Episodio episodio) throws IOException {
        episodeId = episodio.id;
        shot = 0;
        finished = false;
        pagina.start(temporada, episodio);
        int guard = 0;
        while (!finished && guard++ < 200_000) {
            if (fightRequested) {
                fightRequested = false;
                pagina.resultadoLuta(true);
                continue;
            }
            Episodio.Passo passo = pagina.passo();
            if (passo == null) break;
            float holdFor = passo.tipo == Episodio.TipoPasso.TITULO ? 1.4f : 2.0f;
            if (passo.tipo == Episodio.TipoPasso.PAGINA && passo.paginaPronta()) {
                // Página pronta: um quadro por toque; grava cada quadro em foco.
                step(1.6f);
                if (pagina.passo() == passo) save(passo);
                pagina.tap(640f, 360f);
            } else if (passo.tipo == Episodio.TipoPasso.PAGINA) {
                // Toca até a página estar inteira; o próximo toque já sai dela.
                step(3.0f);
                boolean complete = pagina.pageComplete();
                if (complete) save(passo);
                pagina.tap(640f, 360f);
                if (complete) step(0.4f);
            } else {
                step(holdFor);
                save(passo);
                pagina.tap(640f, 360f);
                step(0.5f);
            }
        }
        for (String warning : pagina.warnings()) System.out.println("  arte provisória: " + warning);
    }

    private void step(float seconds) {
        for (float t = 0f; t < seconds && pagina.coversScreen(); t += DT) pagina.update(DT);
    }

    private void save(Episodio.Passo passo) throws IOException {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.scale(WIDTH / PaginaHistoria.VW, HEIGHT / PaginaHistoria.VH);
        pagina.render(new Java2DRenderCanvas(g, WIDTH, HEIGHT));
        g.dispose();
        String name = String.format(Locale.ROOT, "%s_%02d_%s.png", episodeId, shot++,
            passo.tipo.name().toLowerCase(Locale.ROOT));
        ImageIO.write(image, "png", new File(outDir, name));
    }

    @Override
    public void onHistoriaLuta(Episodio.Passo luta) {
        System.out.println("  " + episodeId + ": luta " + luta.rotulo + " " + String.join("+", luta.dupla) +
            " x " + String.join("+", luta.cpu));
        fightRequested = true;
    }

    @Override
    public void onHistoriaSom(String id) {}

    @Override
    public void onHistoriaFim(Episodio episodio) {
        finished = true;
    }
}
