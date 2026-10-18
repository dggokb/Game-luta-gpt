package com.gamelutagpt.historia;

import com.gamelutagpt.render.MiniJson;
import com.gamelutagpt.render.RenderAssets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A temporada do modo história ({@code historia/temporada.json}): o elenco que aparece nos
 * balões e a ordem dos episódios. Cada episódio fica em {@code historia/<id>/episodio.json},
 * com as imagens dos painéis na mesma pasta.
 */
public final class Temporada {
    public static final String PASTA = "historia";

    public final String titulo;
    public final String subtitulo;
    /** Imagem do logo (em {@code historia/}) para o cartão de título, ou null. */
    public final String logo;
    public final Map<String, Personagem> elenco;
    public final List<String> episodios;
    /** Tradução do idioma atual (texto em português → texto traduzido); vazia em português. */
    private Map<String, String> traducao = Collections.emptyMap();
    private String idioma = "pt";

    /** Quem aparece na história: nome nos balões, cor na arte provisória e o pack de luta, se tiver. */
    public static final class Personagem {
        public final String id;
        public final String nome;
        public final int cor;
        public final String lutador;

        Personagem(String id, String nome, int cor, String lutador) {
            this.id = id;
            this.nome = nome;
            this.cor = cor;
            this.lutador = lutador;
        }
    }

    Temporada(String titulo, String subtitulo, String logo, Map<String, Personagem> elenco, List<String> episodios) {
        this.titulo = titulo;
        this.subtitulo = subtitulo;
        this.logo = logo;
        this.elenco = Collections.unmodifiableMap(elenco);
        this.episodios = Collections.unmodifiableList(episodios);
    }

    public static Temporada load(RenderAssets assets) throws IOException {
        return fromJson(assets.readText(PASTA + "/temporada.json"));
    }

    /**
     * Troca o idioma dos textos (balões, legendas, títulos). A tradução fica em
     * {@code historia/idiomas/<idioma>.json}: um objeto com o texto em português como chave e
     * o texto traduzido como valor. Idioma sem arquivo (ou português) mostra o original;
     * devolve falso nesse caso.
     */
    public boolean setIdioma(RenderAssets assets, String codigo) {
        idioma = codigo == null ? "pt" : codigo;
        traducao = Collections.emptyMap();
        if ("pt".equals(idioma)) return true;
        try {
            traducao = traducaoFromJson(assets.readText(PASTA + "/idiomas/" + idioma + ".json"));
            return true;
        } catch (IOException | RuntimeException ex) {
            return false;
        }
    }

    public String idioma() {
        return idioma;
    }

    /** O texto no idioma atual; sem tradução, o original em português. */
    public String t(String texto) {
        if (texto == null) return null;
        String traduzido = traducao.get(texto);
        return traduzido != null ? traduzido : texto;
    }

    static Map<String, String> traducaoFromJson(String json) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : Episodio.asMap(MiniJson.parse(json)).entrySet()) {
            if (entry.getKey().startsWith("_")) continue; // comentários
            out.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return out;
    }

    public Episodio loadEpisodio(RenderAssets assets, String id) throws IOException {
        return Episodio.fromJson(assets.readText(pasta(id) + "/episodio.json"));
    }

    public static String pasta(String episodio) {
        return PASTA + "/" + episodio;
    }

    /** Personagem do elenco; um id desconhecido vira um figurante cinza com o próprio id como nome. */
    public Personagem personagem(String id) {
        Personagem p = elenco.get(id);
        return p != null ? p : new Personagem(id, id, 0xFF8A8F99, null);
    }

    /** Nome do elenco de quem luta com o pack {@code lutador} (o narrador, o HUD), ou null. */
    public String nomeDoLutador(String lutador) {
        if (lutador == null) return null;
        for (Personagem p : elenco.values()) if (lutador.equals(p.lutador)) return p.nome;
        return null;
    }

    public static Temporada fromJson(String json) {
        Map<String, Object> map = Episodio.asMap(MiniJson.parse(json));
        Map<String, Personagem> elenco = new LinkedHashMap<>();
        Object elencoJson = map.get("elenco");
        if (elencoJson != null) {
            for (Map.Entry<String, Object> entry : Episodio.asMap(elencoJson).entrySet()) {
                Map<String, Object> p = Episodio.asMap(entry.getValue());
                elenco.put(entry.getKey(), new Personagem(entry.getKey(),
                    Episodio.string(p, "nome", entry.getKey()),
                    parseColor(Episodio.string(p, "cor", "#8A8F99")),
                    Episodio.string(p, "lutador", null)));
            }
        }
        List<String> episodios = new ArrayList<>();
        for (Object item : Episodio.list(map, "episodios")) episodios.add(String.valueOf(item));
        if (episodios.isEmpty()) throw new IllegalArgumentException("temporada.json sem episódios");
        return new Temporada(Episodio.string(map, "titulo", "MODO HISTÓRIA"),
            Episodio.string(map, "subtitulo", ""), Episodio.string(map, "logo", null), elenco, episodios);
    }

    static int parseColor(String text) {
        String hex = text.startsWith("#") ? text.substring(1) : text;
        if (hex.length() != 6 && hex.length() != 8) throw new IllegalArgumentException("cor inválida: " + text);
        long value = Long.parseLong(hex, 16);
        return hex.length() == 6 ? (int)(0xFF000000L | value) : (int)value;
    }
}
