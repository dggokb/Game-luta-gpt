package com.gamelutagpt.historia;

import com.gamelutagpt.render.MiniJson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Um episódio do modo história, lido do {@code episodio.json}: uma lista de passos
 * (cartão de título, páginas de mangá, lutas e o "próximo episódio").
 */
public final class Episodio {
    public final String id;
    public final int numero;
    public final String titulo;
    public final List<Passo> passos;

    Episodio(String id, int numero, String titulo, List<Passo> passos) {
        this.id = id;
        this.numero = numero;
        this.titulo = titulo;
        this.passos = Collections.unmodifiableList(passos);
    }

    public enum TipoPasso { TITULO, PAGINA, LUTA, PROXIMO }

    public enum TipoBalao { FALA, PENSAMENTO, GRITO, NARRACAO }

    /** Um passo do episódio. Só os campos do tipo dele são usados. */
    public static final class Passo {
        public final TipoPasso tipo;
        // PAGINA
        public final String layout;
        public final String tom;
        public final List<Painel> paineis;
        // LUTA
        public final String[] dupla;
        public final String[] cpu;
        public final String rotulo;
        public final boolean repetirSeDerrota;
        // TITULO e PROXIMO
        public final String titulo;
        public final List<String> linhas;
        // PAGINA pronta: a página inteira numa imagem, mostrada quadro a quadro (leitura guiada)
        public final String imagem;
        public final List<Quadro> quadros;

        Passo(TipoPasso tipo, String layout, String tom, List<Painel> paineis, String[] dupla, String[] cpu,
              String rotulo, boolean repetirSeDerrota, String titulo, List<String> linhas,
              String imagem, List<Quadro> quadros) {
            this.tipo = tipo;
            this.layout = layout;
            this.tom = tom;
            this.paineis = paineis;
            this.dupla = dupla;
            this.cpu = cpu;
            this.rotulo = rotulo;
            this.repetirSeDerrota = repetirSeDerrota;
            this.titulo = titulo;
            this.linhas = linhas;
            this.imagem = imagem;
            this.quadros = quadros;
        }

        public boolean flashback() {
            return "flashback".equals(tom);
        }

        /** Página pronta (uma imagem com tudo desenhado), lida quadro a quadro. */
        public boolean paginaPronta() {
            return imagem != null;
        }
    }

    /**
     * Um quadro de uma página pronta: o contorno dele na imagem, em frações da página
     * ({0, 0} topo esquerdo, {1, 1} base direita), na ordem de leitura.
     */
    public static final class Quadro {
        public final float[] area;
        /** O quadro chega tremendo a tela, com um clarão (golpes, gritos). */
        public final boolean impacto;
        /** Narração escrita pelo jogo embaixo da tela enquanto o quadro está em foco, ou null. */
        public final String legenda;

        Quadro(float[] area, boolean impacto, String legenda) {
            this.area = area;
            this.impacto = impacto;
            this.legenda = legenda;
        }
    }

    /** Um quadro da página. Sem imagem, o jogo desenha uma arte provisória a partir de cena e personagens. */
    public static final class Painel {
        /** Arquivo da arte final, relativo à pasta do episódio (pode ainda não existir). */
        public final String imagem;
        public final String cena;
        public final List<String> personagens;
        /** "plano" (corpo inteiro), "close" (rosto) ou "detalhe" (objeto). */
        public final String enquadramento;
        /** "velocidade", "impacto", "chuva", "brilho" ou nada. */
        public final String efeito;
        public final List<Balao> baloes;
        public final String sfx;
        public final float[] posicaoSfx;
        /** Foco vertical do recorte da imagem (0 = topo, 1 = base). */
        public final float foco;

        Painel(String imagem, String cena, List<String> personagens, String enquadramento, String efeito,
               List<Balao> baloes, String sfx, float[] posicaoSfx, float foco) {
            this.imagem = imagem;
            this.cena = cena;
            this.personagens = personagens;
            this.enquadramento = enquadramento;
            this.efeito = efeito;
            this.baloes = baloes;
            this.sfx = sfx;
            this.posicaoSfx = posicaoSfx;
            this.foco = foco;
        }
    }

    /** Balão de fala. Posições em frações da caixa do painel ([0,0] topo esquerdo). */
    public static final class Balao {
        public final TipoBalao tipo;
        public final String quem;
        public final String texto;
        public final float x;
        public final float y;
        /** Para onde o rabicho aponta (a boca de quem fala), ou null. */
        public final float[] rabo;

        Balao(TipoBalao tipo, String quem, String texto, float x, float y, float[] rabo) {
            this.tipo = tipo;
            this.quem = quem;
            this.texto = texto;
            this.x = x;
            this.y = y;
            this.rabo = rabo;
        }
    }

    // ------------------------------------------------------------ leitura

    public static Episodio fromJson(String json) {
        Object root = MiniJson.parse(json);
        if (!(root instanceof Map)) throw new IllegalArgumentException("episodio.json precisa ser um objeto");
        Map<String, Object> map = asMap(root);
        String id = string(map, "id", "episodio");
        List<Passo> passos = new ArrayList<>();
        for (Object item : list(map, "passos")) passos.add(passo(asMap(item)));
        if (passos.isEmpty()) throw new IllegalArgumentException(id + ": episódio sem passos");
        return new Episodio(id, number(map, "numero", 0).intValue(), string(map, "titulo", id), passos);
    }

    private static Passo passo(Map<String, Object> map) {
        String tipoTexto = string(map, "tipo", "pagina");
        TipoPasso tipo;
        try {
            tipo = TipoPasso.valueOf(tipoTexto.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("tipo de passo desconhecido: " + tipoTexto);
        }
        List<Painel> paineis = new ArrayList<>();
        for (Object item : list(map, "paineis")) paineis.add(painel(asMap(item)));
        String layout = string(map, "layout", null);
        String imagem = string(map, "imagem", null);
        List<Quadro> quadros = new ArrayList<>();
        for (Object item : list(map, "quadros")) quadros.add(quadro(asMap(item)));
        if (tipo == TipoPasso.PAGINA && imagem != null) {
            if (quadros.isEmpty()) throw new IllegalArgumentException("página pronta " + imagem + " sem quadros");
        } else if (tipo == TipoPasso.PAGINA) {
            if (paineis.isEmpty()) throw new IllegalArgumentException("página sem painéis");
            if (layout == null) layout = Layouts.padrao(paineis.size());
            if (Layouts.get(layout).length != paineis.size()) {
                throw new IllegalArgumentException("o layout " + layout + " tem " + Layouts.get(layout).length +
                    " painéis, mas a página tem " + paineis.size());
            }
        }
        List<String> linhas = new ArrayList<>();
        for (Object item : list(map, "linhas")) linhas.add(String.valueOf(item));
        return new Passo(tipo, layout, string(map, "tom", "papel"), Collections.unmodifiableList(paineis),
            pair(map, "dupla"), pair(map, "cpu"), string(map, "rotulo", null),
            !Boolean.FALSE.equals(map.get("repetirSeDerrota")), string(map, "titulo", null),
            Collections.unmodifiableList(linhas), imagem, Collections.unmodifiableList(quadros));
    }

    private static Quadro quadro(Map<String, Object> map) {
        List<Object> values = list(map, "area");
        if (values.size() < 6 || values.size() % 2 != 0) {
            throw new IllegalArgumentException("area do quadro precisa ser [x0, y0, x1, y1, x2, y2, ...]");
        }
        float[] area = new float[values.size()];
        for (int i = 0; i < area.length; i++) {
            Object v = values.get(i);
            if (!(v instanceof Number)) throw new IllegalArgumentException("area do quadro com valor que não é número");
            area[i] = ((Number)v).floatValue();
            if (area[i] < -0.01f || area[i] > 1.01f) throw new IllegalArgumentException("area do quadro fora da página: " + area[i]);
        }
        return new Quadro(area, Boolean.TRUE.equals(map.get("impacto")), string(map, "legenda", null));
    }

    private static Painel painel(Map<String, Object> map) {
        List<String> personagens = new ArrayList<>();
        for (Object item : list(map, "personagens")) personagens.add(String.valueOf(item));
        List<Balao> baloes = new ArrayList<>();
        for (Object item : list(map, "baloes")) baloes.add(balao(asMap(item)));
        return new Painel(string(map, "imagem", null), string(map, "cena", "neutra"),
            Collections.unmodifiableList(personagens), string(map, "enquadramento", "plano"),
            string(map, "efeito", null), Collections.unmodifiableList(baloes), string(map, "sfx", null),
            point(map, "posicaoSfx"), number(map, "foco", 0.5).floatValue());
    }

    private static Balao balao(Map<String, Object> map) {
        String tipoTexto = string(map, "tipo", "fala");
        TipoBalao tipo;
        try {
            tipo = TipoBalao.valueOf(tipoTexto.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("tipo de balão desconhecido: " + tipoTexto);
        }
        String texto = string(map, "texto", "");
        if (texto.isEmpty()) throw new IllegalArgumentException("balão sem texto");
        float[] pos = point(map, "pos");
        if (pos == null) pos = new float[] {0.5f, 0.2f};
        return new Balao(tipo, string(map, "quem", null), texto, pos[0], pos[1], point(map, "rabo"));
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map)) throw new IllegalArgumentException("esperava um objeto JSON");
        return (Map<String, Object>)value;
    }

    @SuppressWarnings("unchecked")
    static List<Object> list(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return Collections.emptyList();
        if (!(value instanceof List)) throw new IllegalArgumentException(key + " precisa ser uma lista");
        return (List<Object>)value;
    }

    static String string(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    static Number number(Map<String, Object> map, String key, Number fallback) {
        Object value = map.get(key);
        return value instanceof Number ? (Number)value : fallback;
    }

    private static float[] point(Map<String, Object> map, String key) {
        List<Object> values = list(map, key);
        if (values.isEmpty()) return null;
        if (values.size() != 2 || !(values.get(0) instanceof Number) || !(values.get(1) instanceof Number)) {
            throw new IllegalArgumentException(key + " precisa ser [x, y]");
        }
        return new float[] {((Number)values.get(0)).floatValue(), ((Number)values.get(1)).floatValue()};
    }

    private static String[] pair(Map<String, Object> map, String key) {
        List<Object> values = list(map, key);
        if (values.isEmpty()) return null;
        if (values.size() != 2) throw new IllegalArgumentException(key + " precisa ser [titular, parceiro]");
        return new String[] {String.valueOf(values.get(0)), String.valueOf(values.get(1))};
    }
}
