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

    /**
     * Trecho do episódio entre os passos marcados {@code de} e {@code ate} (os dois entram;
     * null = do primeiro / até o último). Usa a mesma pasta de imagens do episódio. É assim
     * que uma cena ({@link Cenas}) reaproveita parte de um episódio.
     */
    public Episodio trecho(String de, String ate) {
        int first = de == null ? 0 : indiceDaMarca(de);
        int last = ate == null ? passos.size() - 1 : indiceDaMarca(ate);
        if (last < first) throw new IllegalArgumentException(id + ": a marca \"" + ate + "\" vem antes de \"" + de + "\"");
        return new Episodio(id, numero, titulo, new ArrayList<>(passos.subList(first, last + 1)));
    }

    /** Posição do passo com essa {@code marca}. */
    public int indiceDaMarca(String marca) {
        for (int i = 0; i < passos.size(); i++) {
            if (marca.equals(passos.get(i).marca)) return i;
        }
        throw new IllegalArgumentException(id + ": nenhum passo com a marca \"" + marca + "\"");
    }

    /**
     * ESCOLHA: o jogador escolhe o irmão (Ryūga ou Kōga). SORTEIO: o sorteio animado da chave
     * do torneio. CHAVE: a chave com os resultados até aqui e a próxima luta do jogador.
     */
    public enum TipoPasso { TITULO, PAGINA, LUTA, PROXIMO, ESCOLHA, SORTEIO, CHAVE }

    public enum TipoBalao { FALA, PENSAMENTO, GRITO, NARRACAO }

    /** Um passo do episódio. Só os campos do tipo dele são usados. */
    public static final class Passo {
        public final TipoPasso tipo;
        /** Nome opcional do passo, para uma cena começar ou terminar nele ({@link #trecho}). */
        public final String marca;
        // PAGINA
        public final String layout;
        public final String tom;
        public final List<Painel> paineis;
        // LUTA
        public final String[] dupla;
        public final String[] cpu;
        public final String rotulo;
        public final boolean repetirSeDerrota;
        /** "tutorial" (o treino guiado dos golpes) ou null (luta normal). */
        public final String modo;
        /** Cenário da luta (pasta em assets/stages), ou null para o cenário padrão. */
        public final String cenario;
        /** Conversa nos quadros laterais durante a luta (caminho dentro de historia/), ou null. */
        public final String conversa;
        /** Luta do torneio: o jogador contra o próximo adversário da chave. */
        public final boolean torneio;
        /** {@code "dupla": "jogador"} ou {@code "rival"}: a dupla fixa do irmão escolhido ou do outro. */
        public final String duplaRef;
        public final String cpuRef;
        /** Só toca para quem escolheu este irmão ("ryuga" / "koga"); null = para os dois. */
        public final String irmao;
        /** Duelo 1x1 da regra escondida: o capitão da dupla do jogador contra o próprio parceiro. */
        public final boolean duelo;
        /**
         * Só toca por esse caminho na chave: "direto" (venceu as quartas) ou "repescagem"
         * (perdeu as quartas); null = nos dois.
         */
        public final String caminho;
        // TITULO e PROXIMO
        public final String titulo;
        public final List<String> linhas;
        // PAGINA pronta: a página inteira numa imagem, mostrada quadro a quadro (leitura guiada)
        public final String imagem;
        public final List<Quadro> quadros;

        Passo(TipoPasso tipo, String marca, String layout, String tom, List<Painel> paineis, String[] dupla,
              String[] cpu, String rotulo, boolean repetirSeDerrota, String modo, String cenario, String conversa,
              String titulo, List<String> linhas, String imagem, List<Quadro> quadros, boolean torneio,
              String duplaRef, String cpuRef, String irmao, boolean duelo, String caminho) {
            this.tipo = tipo;
            this.duelo = duelo;
            this.caminho = caminho;
            this.torneio = torneio;
            this.duplaRef = duplaRef;
            this.cpuRef = cpuRef;
            this.irmao = irmao;
            this.marca = marca;
            this.layout = layout;
            this.tom = tom;
            this.paineis = paineis;
            this.dupla = dupla;
            this.cpu = cpu;
            this.rotulo = rotulo;
            this.repetirSeDerrota = repetirSeDerrota;
            this.modo = modo;
            this.cenario = cenario;
            this.conversa = conversa;
            this.titulo = titulo;
            this.linhas = linhas;
            this.imagem = imagem;
            this.quadros = quadros;
        }

        public boolean flashback() {
            return "flashback".equals(tom);
        }

        /** Luta que é o treino guiado dos golpes (modo tutorial). */
        public boolean tutorial() {
            return tipo == TipoPasso.LUTA && "tutorial".equals(modo);
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
        /** A câmera chega de supetão: tranco de zoom, tela tremendo e clarão (golpes, gritos). */
        public final boolean impacto;
        /** A tela treme de leve o tempo todo enquanto o quadro está em foco (tensão, força). */
        public final boolean tremor;
        /** Linhas de velocidade correndo por cima do quadro (ação, movimento). */
        public final boolean velocidade;
        /** A câmera avança devagar sobre o quadro (olhares, momentos dramáticos). */
        public final boolean zoom;
        /** Narração escrita pelo jogo embaixo da tela enquanto o quadro está em foco, ou null. */
        public final String legenda;
        /** Balões de fala desenhados na página, na ordem de leitura. */
        public final List<BalaoPronto> baloes;

        Quadro(float[] area, boolean impacto, boolean tremor, boolean velocidade, boolean zoom, String legenda,
               List<BalaoPronto> baloes) {
            this.area = area;
            this.impacto = impacto;
            this.tremor = tremor;
            this.velocidade = velocidade;
            this.zoom = zoom;
            this.legenda = legenda;
            this.baloes = baloes;
        }
    }

    /**
     * Balão desenhado na página pronta. O jogo pinta o miolo ({@link #area}) de branco, apagando
     * o texto da arte, e escreve {@link #texto} (traduzido) dentro da {@link #caixa}.
     */
    public static final class BalaoPronto {
        /** Contorno do miolo branco, em frações da página. */
        public final float[] area;
        /** Onde o texto cabe: {esquerda, topo, direita, base} em frações da página. */
        public final float[] caixa;
        public final String texto;

        BalaoPronto(float[] area, float[] caixa, String texto) {
            this.area = area;
            this.caixa = caixa;
            this.texto = texto;
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
        java.util.Set<String> marcas = new java.util.HashSet<>();
        for (Passo p : passos) {
            if (p.marca != null && !marcas.add(p.marca)) {
                throw new IllegalArgumentException(id + ": marca repetida \"" + p.marca + "\"");
            }
        }
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
        String modo = string(map, "modo", null);
        if (modo != null && !modo.equals("tutorial")) {
            throw new IllegalArgumentException("modo de luta desconhecido: " + modo + " (use tutorial)");
        }
        List<String> linhas = new ArrayList<>();
        for (Object item : list(map, "linhas")) linhas.add(String.valueOf(item));
        String irmao = string(map, "irmao", null);
        if (irmao != null && !irmao.equals("ryuga") && !irmao.equals("koga")) {
            throw new IllegalArgumentException("irmao desconhecido: " + irmao + " (use ryuga ou koga)");
        }
        String caminho = string(map, "caminho", null);
        if (caminho != null && !caminho.equals("direto") && !caminho.equals("repescagem")) {
            throw new IllegalArgumentException("caminho desconhecido: " + caminho + " (use direto ou repescagem)");
        }
        String duplaRef = ref(map, "dupla");
        String cpuRef = ref(map, "cpu");
        return new Passo(tipo, string(map, "marca", null), layout, string(map, "tom", "papel"),
            Collections.unmodifiableList(paineis), duplaRef != null ? null : pair(map, "dupla"),
            cpuRef != null ? null : pair(map, "cpu"), string(map, "rotulo", null),
            !Boolean.FALSE.equals(map.get("repetirSeDerrota")), modo, string(map, "cenario", null),
            string(map, "conversa", null), string(map, "titulo", null),
            Collections.unmodifiableList(linhas), imagem, Collections.unmodifiableList(quadros),
            Boolean.TRUE.equals(map.get("torneio")), duplaRef, cpuRef, irmao,
            Boolean.TRUE.equals(map.get("duelo")), caminho);
    }

    /** {@code "dupla": "jogador"} ou {@code "rival"} (dupla fixa de um irmão); null se for uma lista. */
    private static String ref(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof String)) return null;
        if (!value.equals("jogador") && !value.equals("rival")) {
            throw new IllegalArgumentException(key + " precisa ser [titular, parceiro], \"jogador\" ou \"rival\"");
        }
        return (String)value;
    }

    private static Quadro quadro(Map<String, Object> map) {
        float[] area = fractions(map, "area", 6, "area do quadro");
        List<String> efeitos = new ArrayList<>();
        for (Object item : list(map, "efeitos")) efeitos.add(String.valueOf(item));
        for (String efeito : efeitos) {
            if (!efeito.equals("impacto") && !efeito.equals("tremor") && !efeito.equals("velocidade") &&
                !efeito.equals("zoom")) {
                throw new IllegalArgumentException("efeito desconhecido: " + efeito + " (use impacto, tremor, velocidade, zoom)");
            }
        }
        List<BalaoPronto> baloes = new ArrayList<>();
        for (Object item : list(map, "baloes")) {
            Map<String, Object> b = asMap(item);
            String texto = string(b, "texto", "");
            if (texto.isEmpty()) throw new IllegalArgumentException("balão da página pronta sem texto");
            float[] caixa = fractions(b, "caixa", 4, "caixa do balão");
            if (caixa.length != 4) throw new IllegalArgumentException("caixa do balão precisa ser [esquerda, topo, direita, base]");
            baloes.add(new BalaoPronto(fractions(b, "area", 6, "area do balão"), caixa, texto));
        }
        boolean impacto = efeitos.contains("impacto") || Boolean.TRUE.equals(map.get("impacto"));
        return new Quadro(area, impacto, efeitos.contains("tremor"), efeitos.contains("velocidade"),
            efeitos.contains("zoom"), string(map, "legenda", null), Collections.unmodifiableList(baloes));
    }

    /** Lista de números entre 0 e 1 (frações da página), com tamanho par. */
    private static float[] fractions(Map<String, Object> map, String key, int min, String what) {
        List<Object> values = list(map, key);
        if (values.size() < min || values.size() % 2 != 0) {
            throw new IllegalArgumentException(what + " precisa ser [x0, y0, x1, y1, ...]");
        }
        float[] out = new float[values.size()];
        for (int i = 0; i < out.length; i++) {
            Object v = values.get(i);
            if (!(v instanceof Number)) throw new IllegalArgumentException(what + " com valor que não é número");
            out[i] = ((Number)v).floatValue();
            if (out[i] < -0.01f || out[i] > 1.01f) throw new IllegalArgumentException(what + " fora da página: " + out[i]);
        }
        return out;
    }

    /** Um painel avulso no formato dos episódios (quadros do tutorial). */
    static Painel painelDe(Map<String, Object> map) {
        return painel(map);
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
