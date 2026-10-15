package com.gamelutagpt.pc;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;

/**
 * Onde estão os arquivos do jogo no PC.
 *
 * <ul>
 *   <li>Desenvolvimento ({@code -Dgameluta.repo=<repositório>}, o {@code gradle :pc:run}):
 *       lê direto de {@code android/app/src/main/assets}, {@code res/drawable-nodpi} e
 *       {@code art/concepts}, sem copiar nada.</li>
 *   <li>Jogo exportado: a pasta {@code data/} ao lado dos jars ({@code data/assets} e
 *       {@code data/drawable}), ou a indicada por {@code -Dgameluta.dados}.</li>
 * </ul>
 */
public final class DadosDoJogo {
    final AssetManager assets;
    final Resources resources;
    final String origem;

    private DadosDoJogo(AssetManager assets, Resources resources, String origem) {
        this.assets = assets;
        this.resources = resources;
        this.origem = origem;
    }

    /** Arquivos direto do repositório (gradle :pc:run e testes). */
    public static DadosDoJogo doRepositorio(File repo) throws FileNotFoundException {
        File main = new File(repo, "android/app/src/main");
        File assetsDir = exigir(new File(main, "assets"));
        AssetManager assets = new AssetManager()
            .addRoot("", assetsDir)
            .addRoot("selection/portraits", exigir(new File(repo, "art/concepts")));
        return new DadosDoJogo(assets, new Resources(exigir(new File(main, "res/drawable-nodpi"))), repo.getPath());
    }

    /** Pasta {@code data/} do jogo exportado. */
    public static DadosDoJogo daPasta(File data) throws FileNotFoundException {
        AssetManager assets = new AssetManager().addRoot("", exigir(new File(data, "assets")));
        return new DadosDoJogo(assets, new Resources(exigir(new File(data, "drawable"))), data.getPath());
    }

    /** Procura os dados: propriedades do sistema, depois a pasta do jogo instalado. */
    public static DadosDoJogo localizar() throws FileNotFoundException {
        String repo = System.getProperty("gameluta.repo");
        if (repo != null) return doRepositorio(new File(repo));
        String dados = System.getProperty("gameluta.dados");
        if (dados != null) return daPasta(new File(dados));
        File base = pastaDoJar();
        for (File candidate = base; candidate != null; candidate = candidate.getParentFile()) {
            File data = new File(candidate, "data");
            if (new File(data, "assets").isDirectory()) return daPasta(data);
            // Rodando de dentro do repositório (IDE): usa os arquivos do Android.
            if (new File(candidate, "android/app/src/main/assets").isDirectory()) return doRepositorio(candidate);
        }
        throw new FileNotFoundException("dados do jogo não encontrados perto de " + base
            + " (use -Dgameluta.dados=<pasta data> ou -Dgameluta.repo=<repositório>)");
    }

    /** Context do "aplicativo": assets, drawables e preferências do usuário. */
    public Context contexto(File preferencias) {
        return new Context(assets, resources, preferencias);
    }

    /** Preferências por usuário, como o SharedPreferences do Android. */
    public static File pastaDePreferencias() {
        String appData = System.getenv("APPDATA");
        File base = appData != null ? new File(appData) : new File(System.getProperty("user.home"), ".config");
        return new File(base, "GameLutaGPT");
    }

    private static File pastaDoJar() {
        try {
            File f = new File(DadosDoJogo.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return f.isFile() ? f.getParentFile() : f;
        } catch (URISyntaxException | RuntimeException e) {
            return new File(System.getProperty("user.dir"));
        }
    }

    private static File exigir(File dir) throws FileNotFoundException {
        if (!dir.isDirectory()) throw new FileNotFoundException(dir.getPath());
        return dir;
    }
}
