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
 * Takatoshi e Dafu falando durante o treino, sem parar a luta: cada um num quadro de mangá
 * pequeno na lateral da tela (Takatoshi à esquerda, Dafu à direita), logo abaixo da barra de
 * vida, com um balão que entra digitando e some sozinho. O quadro troca de expressão conforme
 * a fala e sai deslizando quando a pessoa para de falar.
 *
 * As falas ficam em {@code historia/tutorial/falas.json}: as de cada lição (pelo id da lição,
 * mais "fim") e as reações sorteadas ao que o jogador faz ("licao", "impacto", "levou"). Os
 * rostos ficam em {@code historia/tutorial/retratos/<quem>_<expressao>.png}; uma expressão sem
 * imagem usa a "normal".
 */
public final class RetratosTreino {
    public static final String PASTA = Temporada.PASTA + "/tutorial";
    public static final String ARQUIVO = PASTA + "/falas.json";

    // Quadros nas laterais, entre a barra de vida e os controles (coordenadas 1280x720).
    static final float[] QUADRO_ESQ = {14f, 202f, 192f, 193f, 186f, 376f, 10f, 388f};
    static final float[] QUADRO_DIR = {1088f, 193f, 1266f, 202f, 1270f, 388f, 1094f, 376f};
    // Caixa do balão ao lado de cada quadro (o balão aponta para o quadro).
    static final float[] BALAO_ESQ = {188f, 304f, 498f, 412f};
    static final float[] BALAO_DIR = {782f, 304f, 1092f, 412f};

    static final float ENTRA = 0.24f;
    static final float SAI = 0.22f;
    /** Velocidade da digitação (letras por segundo). */
    static final float DIGITA = 40f;
    /** A fala fica na tela pelo menos isso depois de digitada (mais um pouco por letra). */
    static final float SEGURA_MIN = 1.6f;
    static final float SEGURA_POR_LETRA = 0.035f;
    static final float SEGURA_MAX = 4.5f;
    /** Reação é curta: fica menos na tela e não segura a fala da lição seguinte. */
    static final float SEGURA_REACAO = 0.9f;
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

        Fala(String quem, String expressao, String texto, boolean reacao) {
            this.quem = quem;
            this.expressao = expressao;
            this.texto = texto;
            this.reacao = reacao;
        }
    }

    /** Quem pode falar: lado da tela, cor do fundo do quadro e as expressões com imagem. */
    static final class Retrato {
        final String id;
        final boolean direita;
        final int fundo;
        final List<String> expressoes;

        Retrato(String id, boolean direita, int fundo, List<String> expressoes) {
            this.id = id;
            this.direita = direita;
            this.fundo = fundo;
            this.expressoes = expressoes;
        }
    }

    /** Estado de um quadro na tela. */
    private static final class Lado {
        Retrato retrato;
        String expressao = "normal";
        float entrou = -1f;
        float saiu = -1f;
        float ultimaFala = -1f;
        float troca = -1f;
    }

    private final RenderAssets assets;
    private final Baloes baloes = new Baloes();
    private final Rand rand = new Rand(41);
    private final Map<String, RenderImage> imagens = new HashMap<>();
    private Map<String, Retrato> retratos = Collections.emptyMap();
    private Map<String, List<Fala>> licoes = Collections.emptyMap();
    private Map<String, List<Fala>> reacoes = Collections.emptyMap();
    private final Map<String, Integer> sorteio = new HashMap<>();

    private Temporada temporada;
    private final List<Fala> fila = new ArrayList<>();
    private Fala atual;
    private float falaDesde;
    private float time;
    private final Lado esq = new Lado();
    private final Lado dir = new Lado();
    private long frame;

    public RetratosTreino(RenderAssets assets) {
        this.assets = assets;
    }

    /** Lê as falas e os rostos; sem o arquivo, ninguém fala (o treino segue igual). */
    public void load() {
        try {
            fromJson(assets.readText(ARQUIVO));
        } catch (IOException | RuntimeException ex) {
            retratos = Collections.emptyMap();
            licoes = Collections.emptyMap();
            reacoes = Collections.emptyMap();
            return;
        }
        imagens.clear();
        for (Retrato r : retratos.values()) {
            for (String e : r.expressoes) {
                String nome = r.id + "_" + e;
                try {
                    imagens.put(nome, assets.loadImage(PASTA + "/retratos/" + nome + ".png"));
                } catch (IOException | RuntimeException ex) {
                    // Expressão ainda sem imagem: usa a normal.
                }
            }
        }
    }

    void fromJson(String json) {
        Map<String, Object> root = Episodio.asMap(MiniJson.parse(json));
        Map<String, Retrato> rs = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : Episodio.asMap(root.get("retratos")).entrySet()) {
            Map<String, Object> m = Episodio.asMap(e.getValue());
            List<String> expressoes = new ArrayList<>();
            for (Object x : Episodio.list(m, "expressoes")) expressoes.add(String.valueOf(x));
            if (!expressoes.contains("normal")) expressoes.add(0, "normal");
            rs.put(e.getKey(), new Retrato(e.getKey(), "direita".equals(Episodio.string(m, "lado", "esquerda")),
                Temporada.parseColor(Episodio.string(m, "fundo", "#2B3F8F")), expressoes));
        }
        retratos = rs;
        licoes = falas(root.get("licoes"), rs, false);
        reacoes = falas(root.get("reacoes"), rs, true);
    }

    private static Map<String, List<Fala>> falas(Object json, Map<String, Retrato> rs, boolean reacao) {
        Map<String, List<Fala>> out = new LinkedHashMap<>();
        if (json == null) return out;
        for (Map.Entry<String, Object> e : Episodio.asMap(json).entrySet()) {
            if (e.getKey().startsWith("_")) continue;
            List<Fala> lista = new ArrayList<>();
            for (Object item : (List<?>)e.getValue()) {
                Map<String, Object> m = Episodio.asMap(item);
                String quem = Episodio.string(m, "quem", null);
                if (!rs.containsKey(quem)) throw new IllegalArgumentException(e.getKey() + ": quem fala? " + quem);
                String texto = Episodio.string(m, "texto", "");
                if (texto.isEmpty()) throw new IllegalArgumentException(e.getKey() + ": fala sem texto");
                String expressao = Episodio.string(m, "expressao", "normal");
                if (!rs.get(quem).expressoes.contains(expressao)) {
                    throw new IllegalArgumentException(e.getKey() + ": " + quem + " não tem a expressão " + expressao);
                }
                lista.add(new Fala(quem, expressao, texto, reacao));
            }
            out.put(e.getKey(), Collections.unmodifiableList(lista));
        }
        return out;
    }

    /**
     * Falas de uma lição (ou de "fim"): entram no lugar do que estiver sendo dito. Uma reação
     * que ainda está na tela ("Bom.") termina antes, ela é curta.
     */
    public void licao(Temporada temporada, String id) {
        List<Fala> falas = licoes.get(id);
        if (falas == null || falas.isEmpty()) return;
        this.temporada = temporada;
        fila.clear();
        fila.addAll(falas);
        if (atual == null || !atual.reacao) proxima();
    }

    /**
     * Reação ao que o jogador fez ("licao" = cumpriu a lição, "impacto", "levou"): uma fala
     * sorteada, só quando ninguém está falando (não atropela a explicação da lição).
     */
    public void reacao(Temporada temporada, String tipo) {
        List<Fala> falas = reacoes.get(tipo);
        if (falas == null || falas.isEmpty() || atual != null || !fila.isEmpty()) return;
        this.temporada = temporada;
        int n = sorteio.containsKey(tipo) ? sorteio.get(tipo) : -1;
        // Sorteia sem repetir a última.
        rand.reset(frame * 31L + tipo.hashCode());
        int escolha = falas.size() == 1 ? 0 : (n + 1 + (int)(rand.next() * (falas.size() - 1))) % falas.size();
        sorteio.put(tipo, escolha);
        fila.add(falas.get(escolha));
        proxima();
    }

    public boolean has(String licao) {
        return licoes.containsKey(licao);
    }

    /** Alguém falando ou com fala na fila (o fim do treino espera a conversa acabar). */
    public boolean falando() {
        return atual != null || !fila.isEmpty();
    }

    /** Alguém falando ou um quadro ainda na tela. */
    public boolean busy() {
        return atual != null || visivel(esq) || visivel(dir);
    }

    public Fala atual() {
        return atual;
    }

    public void stop() {
        fila.clear();
        atual = null;
        esq.retrato = dir.retrato = null;
        esq.entrou = dir.entrou = -1f;
    }

    public void update(float dt) {
        frame++;
        time += dt;
        if (atual != null && time - falaDesde >= duracao(atual)) {
            atual = null;
            proxima();
        }
        for (Lado l : new Lado[] {esq, dir}) {
            boolean falando = atual != null && l.retrato != null && atual.quem.equals(l.retrato.id);
            if (!falando && l.entrou >= 0f && l.saiu < 0f && time - l.ultimaFala >= SAIR_DEPOIS) l.saiu = time;
            if (l.saiu >= 0f && time - l.saiu >= SAI) {
                l.entrou = -1f;
                l.saiu = -1f;
                l.retrato = null;
            }
        }
    }

    private void proxima() {
        if (fila.isEmpty()) return;
        atual = fila.remove(0);
        falaDesde = time;
        Retrato r = retratos.get(atual.quem);
        Lado l = r.direita ? dir : esq;
        if (l.retrato != r || l.entrou < 0f || l.saiu >= 0f) {
            l.retrato = r;
            l.entrou = time;
            l.saiu = -1f;
        } else if (!l.expressao.equals(atual.expressao)) {
            l.troca = time;
        }
        l.expressao = atual.expressao;
        l.ultimaFala = time + duracao(atual);
    }

    private float duracao(Fala f) {
        int letras = texto(f).length();
        float segura = f.reacao ? SEGURA_REACAO
            : Math.max(SEGURA_MIN, Math.min(SEGURA_MAX, SEGURA_MIN + letras * SEGURA_POR_LETRA));
        return ENTRA + letras / DIGITA + segura;
    }

    private String texto(Fala f) {
        return temporada != null ? temporada.t(f.texto) : f.texto;
    }

    private boolean visivel(Lado l) {
        return l.retrato != null && l.entrou >= 0f;
    }

    // ------------------------------------------------------------ desenho

    public void render(RenderCanvas c) {
        for (Lado l : new Lado[] {esq, dir}) {
            if (visivel(l)) drawQuadro(c, l);
        }
        if (atual != null) drawBalao(c);
    }

    private void drawQuadro(RenderCanvas c, Lado l) {
        boolean direita = l.retrato.direita;
        float[] poly = direita ? QUADRO_DIR : QUADRO_ESQ;
        float[] caixa = Layouts.caixa(poly);
        float in = Tinta.easeOutBack(Tinta.clamp01((time - l.entrou) / ENTRA));
        float out = l.saiu >= 0f ? Tinta.clamp01((time - l.saiu) / SAI) : 0f;
        float largura = caixa[2] - caixa[0] + 40f;
        float dx = (direita ? 1f : -1f) * largura * ((1f - in) + out * out);
        boolean falando = atual != null && atual.quem.equals(l.retrato.id);
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
        c.fillRect(caixa[0], caixa[1], caixa[2], caixa[3], l.retrato.fundo);
        drawLinhasDeFoco(c, caixa);
        RenderImage img = imagem(l.retrato.id, l.expressao);
        if (img != null) {
            // Cobre o quadro, ancorado no alto (o rosto fica no terço de cima).
            float w = caixa[2] - caixa[0], h = caixa[3] - caixa[1];
            float s = Math.max(w / img.width(), h / img.height()) * 1.02f;
            float iw = img.width() * s, ih = img.height() * s;
            float x = caixa[0] + (w - iw) * 0.5f;
            float y = caixa[1] + (h - ih) * 0.15f;
            c.drawImage(img, x, y, x + iw, y + ih, 255);
        } else {
            c.drawText(nome(l.retrato.id), cx, cy + 8f, 22f, WHITE, INK, 4f, true);
        }
        if (troca < 1f) c.fillRect(caixa[0], caixa[1], caixa[2], caixa[3], Tinta.withAlpha(WHITE, (int)(160f * (1f - troca))));
        c.restore();
        c.strokePolygon(poly, 6f, WHITE);
        c.strokePolygon(poly, 3.5f, INK);
        drawNome(c, l, poly);
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

    private void drawNome(RenderCanvas c, Lado l, float[] poly) {
        String nome = nome(l.retrato.id).toUpperCase(java.util.Locale.ROOT);
        float size = 15f;
        float w = c.measureText(nome, size, true) + 14f;
        boolean direita = l.retrato.direita;
        float x = direita ? poly[4] - w - 10f : poly[6] + 6f;
        float y = (direita ? poly[5] : poly[7]) - 26f;
        Temporada.Personagem p = temporada != null ? temporada.personagem(l.retrato.id) : null;
        int cor = p != null ? p.cor : l.retrato.fundo;
        c.fillRect(x - 3f, y - 3f, x + w + 3f, y + 23f, INK);
        c.fillRect(x, y, x + w, y + 20f, cor);
        c.drawText(nome, x + w * 0.5f, y + 15f, size, WHITE, INK, 3f, true);
    }

    private String nome(String id) {
        Temporada.Personagem p = temporada != null ? temporada.personagem(id) : null;
        return p != null ? p.nome : id;
    }

    private RenderImage imagem(String quem, String expressao) {
        RenderImage img = imagens.get(quem + "_" + expressao);
        return img != null ? img : imagens.get(quem + "_normal");
    }

    private void drawBalao(RenderCanvas c) {
        Retrato r = retratos.get(atual.quem);
        float idade = time - falaDesde - ENTRA * 0.6f;
        if (idade <= 0f) return;
        String texto = texto(atual);
        float[] b = r.direita ? BALAO_DIR : BALAO_ESQ;
        // Rabicho para o quadro de quem fala.
        float[] rabo = r.direita ? new float[] {1.06f, 0.30f} : new float[] {-0.06f, 0.30f};
        Episodio.Balao balao = new Episodio.Balao(Episodio.TipoBalao.FALA, atual.quem, texto,
            r.direita ? 0.62f : 0.38f, 0.42f, rabo);
        int visiveis = (int)Math.max(1f, (idade - PaginaHistoria.BALLOON_POP) * DIGITA);
        float some = time - falaDesde > duracao(atual) - 0.18f ? 1f - Tinta.clamp01((duracao(atual) - (time - falaDesde)) / 0.18f) : 0f;
        c.save();
        if (some > 0f) c.scale(1f - 0.4f * some, 1f - 0.4f * some, (b[0] + b[2]) * 0.5f, (b[1] + b[3]) * 0.5f);
        baloes.draw(c, balao, texto, null, b[0], b[1], b[2], b[3], Math.min(visiveis, texto.length()),
            Tinta.clamp01(idade / PaginaHistoria.BALLOON_POP));
        c.restore();
    }
}
