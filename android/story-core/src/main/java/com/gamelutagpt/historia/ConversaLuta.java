package com.gamelutagpt.historia;

import com.gamelutagpt.render.MiniJson;
import com.gamelutagpt.render.Rand;
import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Conversa durante uma luta, sem parar a luta: quem está de fora (o parceiro de tag esperando,
 * o adversário que aguarda a vez, o mestre assistindo o treino) fala num quadro de mangá
 * pequeno na lateral da tela, logo abaixo da barra de vida: o lado do jogador à esquerda, o
 * do adversário à direita. O balão entra digitando e some sozinho; ninguém precisa tocar. O
 * quadro troca de expressão conforme a fala e sai deslizando quando a pessoa para de falar.
 *
 * Cada luta da história pode apontar um arquivo de conversa ({@code "conversa"} no passo de
 * luta, caminho dentro de {@code historia/}). Nele, {@code lados} diz de que lado cada um
 * aparece, {@code momentos} são as falas de momentos certos da luta (o jogo chama
 * {@link #momento}: "inicio", "vitoria", "derrota", as lições do treino, "fim") e
 * {@code reacoes} são falas sorteadas quando algo acontece ("impacto", "levou", "licao"),
 * só quando ninguém está falando. Os rostos são do elenco inteiro e ficam em
 * {@code historia/retratos/<quem>_<expressao>.png}; uma expressão sem imagem usa a "normal".
 * Padrão completo: docs/historia/motor-conversas.md.
 *
 * A cinemática da luta ({@link CinematicaLuta}) também fala por aqui, com {@link #dizer}: o
 * lado vem dela, não do arquivo da conversa.
 */
public final class ConversaLuta {
    public static final String RETRATOS = Temporada.PASTA + "/retratos";
    /**
     * Para onde olha cada rosto que não é de frente ({@code "<quem>_<expressao>": "esquerda"}
     * ou {@code "direita"} da imagem). O quadro espelha o rosto que olharia para fora da tela.
     */
    public static final String OLHAR = RETRATOS + "/olhar.json";

    // Quadros nas laterais, entre a barra de vida e os controles (coordenadas 1280x720).
    static final float[] QUADRO_ESQ = {14f, 198f, 150f, 192f, 146f, 330f, 10f, 338f};
    static final float[] QUADRO_DIR = {1130f, 192f, 1266f, 198f, 1270f, 338f, 1134f, 330f};
    /**
     * Legenda colada no quadro (sem balão: uma faixa escura com o nome e o texto, como nos
     * jogos de luta). Começa na borda de dentro do quadro e cresce para o centro até a largura
     * máxima; o texto quebra em linhas.
     */
    static final float LEGENDA_ESQ = 140f;
    static final float LEGENDA_DIR = 1140f;
    static final float LEGENDA_TOPO = 300f;
    static final float LEGENDA_LARGURA = 330f;
    static final float LEGENDA_TEXTO = 18f;
    static final float LEGENDA_LINHA = 22f;
    private static final int LEGENDA_FUNDO = 0xE00D1220;

    static final float ENTRA = 0.24f;
    static final float SAI = 0.22f;
    /** Velocidade da digitação (letras por segundo). */
    static final float DIGITA = 40f;
    /** A fala fica na tela pelo menos isso depois de digitada (mais um pouco por letra). */
    static final float SEGURA_MIN = 1.6f;
    static final float SEGURA_POR_LETRA = 0.035f;
    static final float SEGURA_MAX = 4.5f;
    /** Reação é curta: fica menos na tela e não segura a fala do momento seguinte. */
    static final float SEGURA_REACAO = 0.9f;
    /** Silêncio mínimo antes de uma reação (a conversa não vira falatório numa luta). */
    static final float PAUSA_REACAO = 1.5f;
    /** Quem parou de falar fica um pouco antes de sair (conversa de dois não pisca). */
    static final float SAIR_DEPOIS = 1.0f;
    private static final int INK = Baloes.INK;
    private static final int WHITE = Baloes.WHITE;

    /** Uma fala: quem diz, com que cara e o quê (em português; a tradução vem da temporada). */
    public static final class Fala {
        public final String quem;
        public final String expressao;
        public final String texto;
        final boolean reacao;
        /** Lado escolhido por quem pediu a fala (a cinemática); null = o lado do arquivo. */
        final Boolean naDireita;
        /** Segundos na tela depois de digitada; negativo = a conta padrão pelo tamanho. */
        final float segura;

        Fala(String quem, String expressao, String texto, boolean reacao) {
            this(quem, expressao, texto, reacao, null, -1f);
        }

        Fala(String quem, String expressao, String texto, boolean reacao, Boolean naDireita, float segura) {
            this.quem = quem;
            this.expressao = expressao;
            this.texto = texto;
            this.reacao = reacao;
            this.naDireita = naDireita;
            this.segura = segura;
        }
    }

    /** Estado de um quadro na tela. */
    private static final class Lado {
        final boolean direita;
        String quem;
        String expressao = "normal";
        float entrou = -1f;
        float saiu = -1f;
        float ultimaFala = -1f;
        float troca = -1f;

        Lado(boolean direita) {
            this.direita = direita;
        }
    }

    private final RenderAssets assets;
    private final Rand rand = new Rand(41);
    /** Rostos já lidos (o elenco se repete de luta em luta); null = não existe. */
    private final Map<String, RenderImage> imagens = new HashMap<>();
    /** {@link #OLHAR}, lido uma vez; null = ainda não lido. */
    private Map<String, Boolean> olhaEsquerda;
    private Map<String, Boolean> direita = Collections.emptyMap();
    private Map<String, List<Fala>> momentos = Collections.emptyMap();
    private Map<String, List<Fala>> reacoes = Collections.emptyMap();
    private final Map<String, Integer> sorteio = new HashMap<>();
    /** Quem fala sem estar no elenco ou sem rosto nos retratos (o parceiro de tag de qualquer luta). */
    private final Map<String, Avulso> avulsos = new HashMap<>();

    private static final class Avulso {
        final String nome;
        final int cor;
        final String retrato;

        Avulso(String nome, int cor, String retrato) {
            this.nome = nome;
            this.cor = cor;
            this.retrato = retrato;
        }
    }

    private Temporada temporada;
    private final List<Fala> fila = new ArrayList<>();
    private Fala atual;
    private float falaDesde;
    private float caladoDesde = -999f;
    private float time;
    private final Lado esq = new Lado(false);
    private final Lado dir = new Lado(true);
    private long frame;

    public ConversaLuta(RenderAssets assets) {
        this.assets = assets;
    }

    /**
     * Carrega a conversa de uma luta ({@code arquivo} dentro de {@code historia/}) e os rostos
     * de quem fala nela. Falso (e ninguém fala) se o arquivo não abre: a luta segue igual.
     */
    public boolean carregar(Temporada temporada, String arquivo) {
        descarregar();
        try {
            fromJson(temporada, assets.readText(Temporada.PASTA + "/" + arquivo));
        } catch (IOException | RuntimeException ex) {
            descarregar();
            return false;
        }
        for (List<Fala> falas : momentos.values()) carregarRostos(falas);
        for (List<Fala> falas : reacoes.values()) carregarRostos(falas);
        return true;
    }

    private void carregarRostos(List<Fala> falas) {
        if (olhaEsquerda == null) {
            try {
                olhaEsquerda = olharFromJson(assets.readText(OLHAR));
            } catch (IOException | RuntimeException ex) {
                olhaEsquerda = Collections.emptyMap();
            }
        }
        for (Fala f : falas) {
            for (String nome : new String[] {f.quem + "_normal", f.quem + "_" + f.expressao}) {
                if (imagens.containsKey(nome)) continue;
                RenderImage img = carregarImagem(RETRATOS + "/" + nome + ".png");
                Avulso a = avulsos.get(f.quem);
                if (img == null && a != null && a.retrato != null && nome.equals(f.quem + "_normal")) {
                    // Sem retrato da história: o rosto de reserva (o da seleção de lutadores).
                    img = carregarImagem(a.retrato);
                }
                imagens.put(nome, img);
            }
        }
    }

    private RenderImage carregarImagem(String caminho) {
        try {
            return assets.loadImage(caminho);
        } catch (IOException | RuntimeException ex) {
            // Expressão ainda sem imagem: usa a normal (ou o nome, sem nenhuma).
            return null;
        }
    }

    /**
     * Alguém que pode falar fora do elenco da história (ou sem retrato nele): {@code nome} e
     * {@code cor} valem quando o elenco não tem, {@code retrato} (caminho nos assets) quando
     * não há {@code historia/retratos/<quem>_normal.png}.
     */
    public void avulso(String quem, String nome, int cor, String retrato) {
        avulsos.put(quem, new Avulso(nome, cor, retrato));
        // Um rosto que não abriu antes (sem o de reserva) tenta de novo com ele.
        if (imagens.containsKey(quem + "_normal") && imagens.get(quem + "_normal") == null) imagens.remove(quem + "_normal");
    }

    /** Lê o {@link #OLHAR}: rosto → olha para a esquerda da imagem. */
    static Map<String, Boolean> olharFromJson(String json) {
        Map<String, Boolean> out = new HashMap<>();
        for (Map.Entry<String, Object> e : Episodio.asMap(MiniJson.parse(json)).entrySet()) {
            if (e.getKey().startsWith("_")) continue;
            String lado = String.valueOf(e.getValue());
            if (!lado.equals("esquerda") && !lado.equals("direita")) {
                throw new IllegalArgumentException(e.getKey() + ": olhar " + lado + " (use esquerda ou direita)");
            }
            out.put(e.getKey(), lado.equals("esquerda"));
        }
        return out;
    }

    /** Lê e valida a conversa (quem fala tem lado e está no elenco; toda fala tem texto). */
    void fromJson(Temporada temporada, String json) {
        Map<String, Object> root = Episodio.asMap(MiniJson.parse(json));
        Map<String, Boolean> lados = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : Episodio.asMap(root.get("lados")).entrySet()) {
            String lado = String.valueOf(e.getValue());
            if (!lado.equals("esquerda") && !lado.equals("direita")) {
                throw new IllegalArgumentException(e.getKey() + ": lado " + lado + " (use esquerda ou direita)");
            }
            if (temporada != null && !temporada.elenco.containsKey(e.getKey())) {
                throw new IllegalArgumentException("fora do elenco da temporada: " + e.getKey());
            }
            lados.put(e.getKey(), lado.equals("direita"));
        }
        Map<String, List<Fala>> ms = falas(root.get("momentos"), lados, false);
        Map<String, List<Fala>> rs = falas(root.get("reacoes"), lados, true);
        this.temporada = temporada;
        direita = lados;
        momentos = ms;
        reacoes = rs;
    }

    private static Map<String, List<Fala>> falas(Object json, Map<String, Boolean> lados, boolean reacao) {
        Map<String, List<Fala>> out = new LinkedHashMap<>();
        if (json == null) return out;
        for (Map.Entry<String, Object> e : Episodio.asMap(json).entrySet()) {
            if (e.getKey().startsWith("_")) continue;
            List<Fala> lista = new ArrayList<>();
            for (Object item : (List<?>)e.getValue()) {
                Map<String, Object> m = Episodio.asMap(item);
                String quem = Episodio.string(m, "quem", null);
                if (!lados.containsKey(quem)) throw new IllegalArgumentException(e.getKey() + ": quem fala sem lado? " + quem);
                String texto = Episodio.string(m, "texto", "");
                if (texto.isEmpty()) throw new IllegalArgumentException(e.getKey() + ": fala sem texto");
                String expressao = Episodio.string(m, "expressao", "normal");
                if (!expressao.matches("[a-z]+")) throw new IllegalArgumentException(e.getKey() + ": expressão " + expressao);
                lista.add(new Fala(quem, expressao, texto, reacao));
            }
            out.put(e.getKey(), Collections.unmodifiableList(lista));
        }
        return out;
    }

    /** Esquece a conversa (luta sem conversa, ou fim da luta). */
    public void descarregar() {
        stop();
        direita = Collections.emptyMap();
        momentos = Collections.emptyMap();
        reacoes = Collections.emptyMap();
        sorteio.clear();
    }

    /** Há uma conversa carregada para a luta atual. */
    public boolean ativa() {
        return !direita.isEmpty();
    }

    /** Todos os textos (em português), para conferir as traduções. */
    public List<String> textos() {
        List<String> out = new ArrayList<>();
        for (List<Fala> falas : momentos.values()) for (Fala f : falas) out.add(f.texto);
        for (List<Fala> falas : reacoes.values()) for (Fala f : falas) out.add(f.texto);
        return out;
    }

    /** Quem fala nesta conversa. */
    public java.util.Set<String> quem() {
        return direita.keySet();
    }

    /**
     * Falas de um momento ("inicio", uma lição, "fim", "vitoria"...): entram no lugar do que
     * estiver sendo dito. Uma reação que ainda está na tela ("Bom.") termina antes, ela é curta.
     */
    public void momento(String id) {
        List<Fala> falas = momentos.get(id);
        if (falas == null || falas.isEmpty()) return;
        fila.clear();
        fila.addAll(falas);
        if (atual == null || !atual.reacao) proxima();
    }

    /**
     * Reação ao que aconteceu ("impacto", "levou", "licao"...): uma fala sorteada, só depois de
     * um silêncio (não atropela um momento nem vira falatório).
     */
    public void reacao(String tipo) {
        List<Fala> falas = reacoes.get(tipo);
        if (falas == null || falas.isEmpty() || falando() || time - caladoDesde < PAUSA_REACAO) return;
        int n = sorteio.containsKey(tipo) ? sorteio.get(tipo) : -1;
        // Sorteia sem repetir a última.
        rand.reset(frame * 31L + tipo.hashCode());
        int escolha = falas.size() == 1 ? 0 : (n + 1 + (int)(rand.next() * (falas.size() - 1))) % falas.size();
        sorteio.put(tipo, escolha);
        fila.add(falas.get(escolha));
        proxima();
    }

    /**
     * Uma fala avulsa, com o lado escolhido por quem pede (a cinemática da luta): entra na fila
     * depois do que já foi pedido assim. {@code segura}: segundos na tela depois de digitada
     * (negativo = a conta padrão). Funciona sem arquivo de conversa carregado.
     */
    public void dizer(Temporada temporada, String quem, boolean naDireita, String expressao, String texto, float segura) {
        if (this.temporada == null) this.temporada = temporada;
        Fala f = new Fala(quem, expressao == null ? "normal" : expressao, texto, false, naDireita, segura);
        carregarRostos(Collections.singletonList(f));
        if (atual != null && atual.naDireita == null) {
            // Fala da conversa no meio: a da cinemática toma o lugar.
            calar();
        }
        fila.add(f);
        if (atual == null) proxima();
    }

    /**
     * Para a conversa sem arrancar os quadros: a fila esvazia, a fala atual some e os quadros
     * saem deslizando (a cinemática começou, a luta acabou...).
     */
    public void calar() {
        fila.clear();
        if (atual != null) {
            atual = null;
            caladoDesde = time;
        }
        for (Lado l : new Lado[] {esq, dir}) l.ultimaFala = Math.min(l.ultimaFala, time - SAIR_DEPOIS);
    }

    public boolean tem(String momento) {
        return momentos.containsKey(momento);
    }

    /** Alguém falando ou com fala na fila (o fim da luta espera a conversa acabar). */
    public boolean falando() {
        return atual != null || !fila.isEmpty();
    }

    /** Alguém falando ou um quadro ainda na tela. */
    public boolean busy() {
        return falando() || visivel(esq) || visivel(dir);
    }

    public Fala atual() {
        return atual;
    }

    /** Cala todo mundo e tira os quadros (a conversa continua carregada). */
    public void stop() {
        fila.clear();
        atual = null;
        caladoDesde = -999f;
        for (Lado l : new Lado[] {esq, dir}) {
            l.quem = null;
            l.entrou = l.saiu = -1f;
        }
    }

    public void update(float dt) {
        frame++;
        time += dt;
        if (atual != null && time - falaDesde >= duracao(atual)) {
            atual = null;
            caladoDesde = time;
            proxima();
        }
        for (Lado l : new Lado[] {esq, dir}) {
            boolean falandoAgora = atual != null && atual.quem.equals(l.quem);
            if (!falandoAgora && l.entrou >= 0f && l.saiu < 0f && time - l.ultimaFala >= SAIR_DEPOIS) l.saiu = time;
            if (l.saiu >= 0f && time - l.saiu >= SAI) {
                l.entrou = -1f;
                l.saiu = -1f;
                l.quem = null;
            }
        }
    }

    private void proxima() {
        if (fila.isEmpty()) return;
        atual = fila.remove(0);
        falaDesde = time;
        Lado l = naDireita(atual) ? dir : esq;
        if (!atual.quem.equals(l.quem) || l.entrou < 0f || l.saiu >= 0f) {
            // Outra pessoa do mesmo lado: o quadro entra de novo com o rosto dela.
            l.quem = atual.quem;
            l.entrou = time;
            l.saiu = -1f;
        } else if (!l.expressao.equals(atual.expressao)) {
            l.troca = time;
        }
        l.expressao = atual.expressao;
        l.ultimaFala = time + duracao(atual);
    }

    private boolean naDireita(Fala f) {
        return f.naDireita != null ? f.naDireita : Boolean.TRUE.equals(direita.get(f.quem));
    }

    private float duracao(Fala f) {
        int letras = texto(f).length();
        float segura = f.segura >= 0f ? f.segura : f.reacao ? SEGURA_REACAO
            : Math.max(SEGURA_MIN, Math.min(SEGURA_MAX, SEGURA_MIN + letras * SEGURA_POR_LETRA));
        return ENTRA + letras / DIGITA + segura;
    }

    private String texto(Fala f) {
        return temporada != null ? temporada.t(f.texto) : f.texto;
    }

    private boolean visivel(Lado l) {
        return l.quem != null && l.entrou >= 0f;
    }

    // ------------------------------------------------------------ desenho

    public void render(RenderCanvas c) {
        // A legenda fica por baixo do quadro, que a "segura" pela borda.
        if (atual != null) drawLegenda(c);
        for (Lado l : new Lado[] {esq, dir}) {
            if (visivel(l)) drawQuadro(c, l);
        }
    }

    private void drawQuadro(RenderCanvas c, Lado l) {
        boolean direita = l.direita;
        float[] poly = direita ? QUADRO_DIR : QUADRO_ESQ;
        float[] caixa = Layouts.caixa(poly);
        float in = Tinta.easeOutBack(Tinta.clamp01((time - l.entrou) / ENTRA));
        float out = l.saiu >= 0f ? Tinta.clamp01((time - l.saiu) / SAI) : 0f;
        float largura = caixa[2] - caixa[0] + 40f;
        float dx = (direita ? 1f : -1f) * largura * ((1f - in) + out * out);
        boolean falando = atual != null && atual.quem.equals(l.quem);
        float bob = falando ? (float)Math.sin(time * 9f) * 1.5f : 0f;
        float cx = (caixa[0] + caixa[2]) * 0.5f, cy = (caixa[1] + caixa[3]) * 0.5f;
        float troca = l.troca >= 0f ? Tinta.clamp01((time - l.troca) / 0.18f) : 1f;
        float pop = 1f + 0.06f * (1f - troca);

        c.save();
        c.translate(dx, bob);
        c.scale(pop, pop, cx, cy);
        float[] sombra = new float[poly.length];
        for (int i = 0; i < poly.length; i += 2) {
            sombra[i] = poly[i] + 6f;
            sombra[i + 1] = poly[i + 1] + 8f;
        }
        c.fillPolygon(sombra, 0x77000000);
        c.save();
        c.clipPolygon(poly);
        c.fillRect(caixa[0], caixa[1], caixa[2], caixa[3], cor(l.quem));
        drawLinhasDeFoco(c, caixa);
        RenderImage img = imagem(l.quem, l.expressao);
        if (img != null) {
            // Cobre o quadro, ancorado no alto (o rosto fica no terço de cima).
            float w = caixa[2] - caixa[0], h = caixa[3] - caixa[1];
            float s = Math.max(w / img.width(), h / img.height()) * 1.02f;
            float iw = img.width() * s, ih = img.height() * s;
            float x = caixa[0] + (w - iw) * 0.5f;
            float y = caixa[1] + (h - ih) * 0.15f;
            if (espelhar(l.quem, l.expressao, direita)) {
                // O rosto olharia para fora da tela: vira para o centro.
                c.save();
                c.scale(-1f, 1f, cx, cy);
                c.drawImage(img, x, y, x + iw, y + ih, 255);
                c.restore();
            } else {
                c.drawImage(img, x, y, x + iw, y + ih, 255);
            }
        } else {
            c.drawText(nome(l.quem), cx, cy + 8f, 22f, WHITE, INK, 4f, true);
        }
        if (troca < 1f) c.fillRect(caixa[0], caixa[1], caixa[2], caixa[3], Tinta.withAlpha(WHITE, (int)(160f * (1f - troca))));
        c.restore();
        c.strokePolygon(poly, 6f, WHITE);
        c.strokePolygon(poly, 3.5f, INK);
        c.restore();
    }

    private void drawLinhasDeFoco(RenderCanvas c, float[] caixa) {
        float cx = (caixa[0] + caixa[2]) * 0.5f, cy = caixa[1] + (caixa[3] - caixa[1]) * 0.42f;
        float r = Math.max(caixa[2] - caixa[0], caixa[3] - caixa[1]);
        for (int i = 0; i < 28; i++) {
            double a = Math.PI * 2 * i / 28;
            float cos = (float)Math.cos(a), sin = (float)Math.sin(a);
            c.drawLine(cx + cos * r * 0.25f, cy + sin * r * 0.25f, cx + cos * r, cy + sin * r, 2f, 0x55FFF0C8);
        }
    }

    String nome(String id) {
        Temporada.Personagem p = temporada != null ? temporada.elenco.get(id) : null;
        if (p != null) return p.nome;
        Avulso a = avulsos.get(id);
        return a != null ? a.nome : id;
    }

    /** Cor do elenco (ou do avulso): pinta o fundo do quadro e a plaquinha do nome. */
    int cor(String id) {
        Temporada.Personagem p = temporada != null ? temporada.elenco.get(id) : null;
        if (p != null) return p.cor;
        Avulso a = avulsos.get(id);
        if (a != null) return a.cor;
        return temporada != null ? temporada.personagem(id).cor : 0xFF2B3F8F;
    }

    /** O rosto desenhado ({@link #imagem}) olha para fora da tela neste lado. */
    private boolean espelhar(String quem, String expressao, boolean direita) {
        if (olhaEsquerda == null) return false;
        String nome = imagens.get(quem + "_" + expressao) != null ? quem + "_" + expressao : quem + "_normal";
        Boolean esquerda = olhaEsquerda.get(nome);
        return esquerda != null && esquerda != direita;
    }

    private RenderImage imagem(String quem, String expressao) {
        RenderImage img = imagens.get(quem + "_" + expressao);
        return img != null ? img : imagens.get(quem + "_normal");
    }

    private void drawLegenda(RenderCanvas c) {
        boolean naDireita = naDireita(atual);
        float idade = time - falaDesde - ENTRA * 0.6f;
        if (idade <= 0f) return;
        String texto = texto(atual);
        float size = LEGENDA_TEXTO;
        List<String> linhas = Baloes.wrap(c, texto, size, LEGENDA_LARGURA - 34f);
        float larguraTexto = 0f;
        for (String linha : linhas) larguraTexto = Math.max(larguraTexto, c.measureText(linha, size, false));
        String nome = nome(atual.quem).toUpperCase(java.util.Locale.ROOT);
        float nomeW = c.measureText(nome, 13f, true) + 14f;
        float w = Math.max(larguraTexto, nomeW) + 34f;
        float h = 14f + linhas.size() * LEGENDA_LINHA;
        float top = LEGENDA_TOPO;
        float x0 = naDireita ? LEGENDA_DIR - w : LEGENDA_ESQ;
        float x1 = x0 + w;
        // Abre a partir do quadro (0,15 s) e fecha de volta para ele no fim da fala.
        float abre = Tinta.easeOutCubic(Tinta.clamp01(idade / 0.15f));
        float resta = duracao(atual) - (time - falaDesde);
        float fecha = resta < 0.15f ? Tinta.clamp01(resta / 0.15f) : 1f;
        float sx = Math.max(0.02f, abre * fecha);
        float ancora = naDireita ? x1 : x0;

        c.save();
        c.scale(sx, 1f, ancora, top);
        // Faixa escura com a ponta inclinada do lado do centro da tela.
        float ponta = 14f;
        float[] faixa = naDireita
            ? new float[] {x0 - ponta, top, x1, top, x1, top + h, x0, top + h}
            : new float[] {x0, top, x1 + ponta, top, x1, top + h, x0, top + h};
        c.fillPolygon(faixa, LEGENDA_FUNDO);
        c.strokePolygon(faixa, 1.5f, 0x55FFFFFF);
        // Barra na cor de quem fala, encostada no quadro.
        int cor = cor(atual.quem);
        if (naDireita) {
            c.fillRect(x1 - 5f, top, x1, top + h, cor);
        } else {
            c.fillRect(x0, top, x0 + 5f, top + h, cor);
        }
        // Nome numa plaquinha em cima da faixa.
        float nx = naDireita ? x1 - 14f - nomeW : x0 + 14f;
        c.fillRect(nx - 2f, top - 13f, nx + nomeW + 2f, top + 5f, INK);
        c.fillRect(nx, top - 11f, nx + nomeW, top + 3f, cor);
        c.drawText(nome, nx + nomeW * 0.5f, top + 1f, 13f, WHITE, INK, 2.5f, true);
        // Texto digitado, alinhado à esquerda.
        int visiveis = (int)Math.max(1f, idade * DIGITA);
        float tx = naDireita ? x1 - 17f - larguraTexto : x0 + 17f;
        float y = top + 10f + LEGENDA_LINHA - 5f;
        for (String linha : linhas) {
            if (visiveis <= 0) break;
            String parte = linha.length() <= visiveis ? linha : linha.substring(0, visiveis);
            visiveis -= linha.length() + 1;
            float pw = c.measureText(parte, size, false);
            c.drawText(parte, tx + pw * 0.5f, y, size, WHITE, INK, 2f, false);
            y += LEGENDA_LINHA;
        }
        c.restore();
    }
}
