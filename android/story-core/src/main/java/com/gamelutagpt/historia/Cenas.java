package com.gamelutagpt.historia;

import com.gamelutagpt.render.MiniJson;
import com.gamelutagpt.render.RenderAssets;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * As cenas que o jogo chama pelo nome, em vários momentos (abrir o tutorial, antes de uma
 * luta, depois de um chefe...). Ficam em {@code historia/cenas.json}:
 *
 * <pre>
 * {
 *   "intro": { "episodio": "ep00", "ate": "intro" },
 *   "pai-parte": { "episodio": "ep00", "de": "pai-parte" },
 *   "ep01": { "episodio": "ep01" }
 * }
 * </pre>
 *
 * Uma cena é um episódio inteiro ou um trecho dele, entre os passos com {@code "marca"}
 * {@code de} e {@code ate} (os dois entram). Um nome que não está no arquivo, mas é a pasta
 * de um episódio, toca o episódio inteiro. No jogo: {@code playScene("intro", depois)}.
 */
public final class Cenas {
    public static final String ARQUIVO = Temporada.PASTA + "/cenas.json";

    /** Onde a cena está: o episódio e as marcas do trecho (null = do começo / até o fim). */
    public static final class Cena {
        public final String id;
        public final String episodio;
        public final String de;
        public final String ate;

        Cena(String id, String episodio, String de, String ate) {
            this.id = id;
            this.episodio = episodio;
            this.de = de;
            this.ate = ate;
        }
    }

    private final Map<String, Cena> cenas;

    Cenas(Map<String, Cena> cenas) {
        this.cenas = Collections.unmodifiableMap(cenas);
    }

    public static Cenas load(RenderAssets assets) throws IOException {
        return fromJson(assets.readText(ARQUIVO));
    }

    public static Cenas fromJson(String json) {
        Map<String, Cena> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : Episodio.asMap(MiniJson.parse(json)).entrySet()) {
            if (entry.getKey().startsWith("_")) continue; // comentários
            Map<String, Object> c = Episodio.asMap(entry.getValue());
            String episodio = Episodio.string(c, "episodio", null);
            if (episodio == null) throw new IllegalArgumentException("cena " + entry.getKey() + " sem \"episodio\"");
            out.put(entry.getKey(), new Cena(entry.getKey(), episodio,
                Episodio.string(c, "de", null), Episodio.string(c, "ate", null)));
        }
        return new Cenas(out);
    }

    public Map<String, Cena> todas() {
        return cenas;
    }

    /** A cena pelo nome; um nome fora do arquivo vira o episódio inteiro com esse id. */
    public Cena get(String id) {
        Cena cena = cenas.get(id);
        return cena != null ? cena : new Cena(id, id, null, null);
    }

    /** Lê o episódio da cena e corta o trecho dela, pronto para {@link PaginaHistoria#start}. */
    public Episodio carregar(Temporada temporada, RenderAssets assets, String id) throws IOException {
        Cena cena = get(id);
        return temporada.loadEpisodio(assets, cena.episodio).trecho(cena.de, cena.ate);
    }
}
