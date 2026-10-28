package com.gamelutagpt.preview;

import com.gamelutagpt.historia.Episodio;
import com.gamelutagpt.historia.HistoriaListener;
import com.gamelutagpt.historia.MenuInicial;
import com.gamelutagpt.historia.MenuPausa;
import com.gamelutagpt.historia.PaginaHistoria;
import com.gamelutagpt.historia.Temporada;
import com.gamelutagpt.render.RenderCanvas;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Grava em PNG o menu inicial (com o cursor em cada opção), a pausa por cima da primeira
 * página do Prólogo, a tela de capítulos do modo história e as telas do Arcade (a escolha da
 * dupla, o sorteio e a chave de 16, o cartão do Takatoshi), para revisar o visual e as traduções
 * sem abrir o jogo.
 *
 * <pre>gradle :pc-preview:runMenu</pre>
 * ou, com argumentos: {@code <pasta assets> <pasta de saída>}; {@code -Didioma=en} troca o idioma.
 */
public final class MenuPreview {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final float DT = 1f / 60f;

    private interface Cena {
        void render(RenderCanvas c);
    }

    public static void main(String[] args) throws IOException {
        File assetsDir = new File(args.length > 0 ? args[0] : "app/src/main/assets");
        File outDir = new File(args.length > 1 ? args[1] : "build/menu");
        if (!outDir.isDirectory() && !outDir.mkdirs()) throw new IOException("não consegui criar " + outDir);
        FileRenderAssets assets = new FileRenderAssets(assetsDir);
        Temporada temporada = Temporada.load(assets);
        String idioma = System.getProperty("idioma", "pt");
        if (!temporada.setIdioma(assets, idioma)) System.out.println("sem tradução para " + idioma);

        MenuInicial menu = new MenuInicial(assets);
        menu.setVersao("SPRITE GPT • v0.000");
        menu.abrir(temporada);
        step(menu, 1.6f);
        save(menu::render, new File(outDir, "menu_" + idioma + "_historia.png"));
        menu.mover(1);
        step(menu, 0.3f);
        save(menu::render, new File(outDir, "menu_" + idioma + "_arcade.png"));
        menu.mover(1);
        step(menu, 0.3f);
        save(menu::render, new File(outDir, "menu_" + idioma + "_teste.png"));

        // A pausa por cima da primeira página do Prólogo, com o botão MENU.
        PaginaHistoria pagina = new PaginaHistoria(new HistoriaListener() {
            @Override public void onHistoriaLuta(Episodio.Passo luta) {}
            @Override public void onHistoriaSom(String id) {}
            @Override public void onHistoriaFim(Episodio episodio) {}
        }, assets);
        pagina.setBotaoMenu(true);
        pagina.start(temporada, temporada.loadEpisodio(assets, temporada.episodios.get(0)));
        for (float t = 0f; t < 3.4f; t += DT) pagina.update(DT);
        save(pagina::render, new File(outDir, "menu_" + idioma + "_pagina.png"));
        MenuPausa pausa = new MenuPausa();
        pausa.abrir(temporada, true);
        for (float t = 0f; t < 0.5f; t += DT) pausa.update(DT);
        save(c -> {
            pagina.render(c);
            pausa.render(c);
        }, new File(outDir, "menu_" + idioma + "_pausa.png"));
        // COMANDOS da pausa, com a ficha do P01 (o jogo monta as fichas de todos a partir dos dados).
        java.util.List<com.gamelutagpt.historia.TelaComandos.Linha> linhas = java.util.Arrays.asList(
            new com.gamelutagpt.historia.TelaComandos.Linha("Onda", "↓ ↘ → + [J]/[K]/[L]", false),
            new com.gamelutagpt.historia.TelaComandos.Linha("Punho ascendente", "→ ↓ ↘ + [J]/[K]/[L]", false),
            new com.gamelutagpt.historia.TelaComandos.Linha("Aparar", "↓ ↓ + [J]/[K]/[L]", false),
            new com.gamelutagpt.historia.TelaComandos.Linha("Mergulho", "↓ ↘ → + [J]/[K]/[L]", true),
            new com.gamelutagpt.historia.TelaComandos.Linha("Super", "[I]", false),
            new com.gamelutagpt.historia.TelaComandos.Linha("Ultra", "↓ + [I]", false),
            new com.gamelutagpt.historia.TelaComandos.Linha("Agarrão", "[H] / [J] + [K]", false));
        pausa.comandos().setFichas(java.util.Arrays.asList(
            new com.gamelutagpt.historia.TelaComandos.Ficha("p01", linhas),
            new com.gamelutagpt.historia.TelaComandos.Ficha("p02", linhas)), 0);
        for (int i = 0; i < 2; i++) pausa.mover(1);
        pausa.confirmar();
        for (float t = 0f; t < 0.5f; t += DT) pausa.update(DT);
        save(c -> {
            pagina.render(c);
            pausa.render(c);
        }, new File(outDir, "menu_" + idioma + "_comandos.png"));
        // Capítulos: um save no começo do ep03 (Prólogo, ep01 e ep02 feitos).
        java.util.List<com.gamelutagpt.historia.MenuCapitulos.Capitulo> caps = new java.util.ArrayList<>();
        for (String id : temporada.episodios) {
            Episodio ep = temporada.loadEpisodio(assets, id);
            caps.add(new com.gamelutagpt.historia.MenuCapitulos.Capitulo(ep.numero, ep.titulo));
        }
        com.gamelutagpt.historia.ProgressoHistoria save = new com.gamelutagpt.historia.ProgressoHistoria(1L);
        for (int i = 0; i < 3; i++) save.terminou(i, null);
        com.gamelutagpt.historia.MenuCapitulos menuCaps = new com.gamelutagpt.historia.MenuCapitulos();
        menuCaps.abrir(temporada, caps, save);
        for (float t = 0f; t < 1.2f; t += DT) menuCaps.update(DT);
        save(menuCaps::render, new File(outDir, "menu_" + idioma + "_capitulos.png"));
        arcade(assets, temporada, idioma, outDir);
        System.out.println("Menu gravado em " + outDir.getAbsolutePath());
    }

    /** As telas do Arcade: o sorteio, a chave depois de 2 rodadas e o fim. */
    private static void arcade(FileRenderAssets assets, Temporada temporada, String idioma, File outDir) throws IOException {
        java.util.List<com.gamelutagpt.historia.Torneio.Dupla> oficiais = com.gamelutagpt.historia.Arcade.oficiais(
            com.gamelutagpt.historia.Torneio.duplasFromJson(assets.readText(com.gamelutagpt.historia.Torneio.ARQUIVO)));
        java.util.List<String> lutadores = java.util.Arrays.asList("player_base", "p02", "p03", "p04", "p05", "p06",
            "p07", "p08", "p09", "p10", "p11", "p12");
        com.gamelutagpt.historia.Campanha campanha = com.gamelutagpt.historia.Campanha.arcade(com.gamelutagpt.historia.Arcade.chave(
            com.gamelutagpt.historia.Arcade.livre("p05", "p12"), oficiais, lutadores, new java.util.Random(4)));
        PaginaHistoria pagina = new PaginaHistoria(new HistoriaListener() {
            @Override public void onHistoriaLuta(Episodio.Passo luta) {}
            @Override public void onHistoriaSom(String id) {}
            @Override public void onHistoriaFim(Episodio episodio) {}
        }, assets);
        pagina.setCampanha(campanha);
        pagina.start(temporada, Episodio.fromJson(assets.readText(com.gamelutagpt.historia.Arcade.EPISODIO)));
        for (float t = 0f; t < 1.2f; t += DT) pagina.update(DT);
        save(pagina::render, new File(outDir, "arcade_" + idioma + "_titulo.png"));
        // Cada passo: espera a animação, grava os que interessam e toca para seguir; as lutas
        // são vencidas na hora.
        for (int guard = 0; guard < 200 && pagina.isActive(); guard++) {
            if (pagina.waitingFight()) {
                pagina.resultadoLuta(true);
                continue;
            }
            Episodio.Passo passo = pagina.passo();
            float espera = passo.tipo == Episodio.TipoPasso.TITULO ? 1.2f : 5f;
            for (float t = 0f; t < espera && pagina.passo() == passo; t += DT) pagina.update(DT);
            if (pagina.passo() != passo) continue;
            int disputadas = campanha.torneio().disputadas();
            String nome = null;
            if (passo.tipo == Episodio.TipoPasso.SORTEIO) nome = "sorteio";
            else if (passo.tipo == Episodio.TipoPasso.CHAVE && disputadas == 2) nome = "chave";
            else if (passo.tipo == Episodio.TipoPasso.CHAVE && campanha.torneio().acabou()) nome = "campeao";
            else if (passo.tipo == Episodio.TipoPasso.TITULO && "TAKATOSHI".equals(passo.titulo)) nome = "takatoshi";
            if (nome != null) save(pagina::render, new File(outDir, "arcade_" + idioma + "_" + nome + ".png"));
            pagina.tap(640f, 360f);
            for (float t = 0f; t < 1f && pagina.passo() == passo && pagina.isActive(); t += DT) pagina.update(DT);
        }
    }

    private static void step(MenuInicial menu, float seconds) {
        for (float t = 0f; t < seconds; t += DT) menu.update(DT);
    }

    private static void save(Cena cena, File file) throws IOException {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.scale(WIDTH / PaginaHistoria.VW, HEIGHT / PaginaHistoria.VH);
        cena.render(new Java2DRenderCanvas(g, WIDTH, HEIGHT));
        g.dispose();
        ImageIO.write(image, "png", file);
    }
}
