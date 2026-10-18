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
 * O narrador do torneio: anima a luta como a narradora do Torneio das Trevas. Fala o começo,
 * os golpes, os combos, o Super e o Ultra, quem está ganhando ou em apuros, o K.O. e o
 * vencedor, sem parar a luta, numa faixa de transmissão no rodapé da tela (o "terço de baixo"
 * de TV, entre o direcional e os botões), com o retrato num quadro de mangá ao lado.
 *
 * O jogo diz o que aconteceu ({@link #narrar}: um evento e as variáveis, como {@code atacante}
 * ou {@code n}); a fala sai de {@link #ARQUIVO}, sorteada sem repetir a última. Cada evento
 * tem prioridade: 3 (começo, Ultra, K.O., vitória) entra na hora, por cima de tudo; 2
 * (Super, guard break, combo grande, virada...) corta um comentário e espera na fila atrás de
 * outra fala importante; 1 (primeiro golpe, combo, agarrão...) só sai depois de um silêncio,
 * para a narração não virar falatório. O mesmo evento não se repete logo em seguida.
 *
 * O personagem ainda vai ser criado: sem {@code historia/retratos/narrador_<expressao>.png} o
 * quadro mostra uma silhueta com microfone (o placeholder). Padrão: docs/historia/narrador.md.
 */
public final class Narracao {
    public static final String ARQUIVO = Temporada.PASTA + "/narrador.json";
    public static final String QUEM = "narrador";

    // Desenho (coordenadas 1280x720): quadro do retrato e faixa do texto no rodapé, no meio.
    // A faixa termina antes do botão OD (borda esquerda em 750) e o quadro fica à direita do
    // direcional (que pega o dedo até 375).
    static final float[] QUADRO = {382f, 574f, 482f, 567f, 488f, 692f, 376f, 696f};
    static final float FAIXA_ESQ = 474f, FAIXA_DIR = 728f, FAIXA_TOPO = 600f, FAIXA_BASE = 688f;
    static final float TEXTO = 18f, TEXTO_GRANDE = 20f, LINHA = 22f, NOME = 14f;
    private static final int FUNDO = 0xE80D1220;
    private static final int AMARELO = 0xFFFFE08A;
    private static final int INK = Baloes.INK;
    private static final int WHITE = Baloes.WHITE;

    static final float ENTRA = 0.22f, SAI = 0.22f;
    /** Letras por segundo da digitação. */
    static final float DIGITA = 45f;
    /** Tempo na tela depois de digitada: base, por letra e o teto; a fala grande fica mais. */
    static final float SEGURA_MIN = 1.2f, SEGURA_POR_LETRA = 0.03f, SEGURA_MAX = 3.2f, SEGURA_GRANDE = 0.6f;
    /** Silêncio antes de um comentário (prioridade 1) e antes de uma fala importante (2). */
    static final float PAUSA_COMENTARIO = 2.0f, PAUSA_IMPORTANTE = 0.4f;
    /** O mesmo evento só volta depois disso (os de prioridade 3 não esperam). */
    static final float REPETE_DEPOIS = 8f;
    /** A faixa fica um pouco depois da última fala antes de descer. */
    static final float SAIR_DEPOIS = 0.8f;

    /** Um evento do arquivo: prioridade, expressão do retrato e as falas (em português). */
    static final class Evento {
        final int prioridade;
        final String expressao;
        final List<String> falas;

        Evento(int prioridade, String expressao, List<String> falas) {
            this.prioridade = prioridade;
            this.expressao = expressao;
            this.falas = falas;
        }
    }

    /** Uma fala na tela ou na fila. */
    static final class Fala {
        final String evento;
        final int prioridade;
        final String expressao;
        final String texto;

        Fala(String evento, int prioridade, String expressao, String texto) {
            this.evento = evento;
            this.prioridade = prioridade;
            this.expressao = expressao;
            this.texto = texto;
        }
    }

    private final RenderAssets assets;
    private final Rand rand = new Rand(1L);
    private final Map<String, Evento> eventos = new LinkedHashMap<>();
    private final Map<String, Integer> ultimaEscolha = new HashMap<>();
    private final Map<String, Float> ultimaVez = new HashMap<>();
    private final Map<String, RenderImage> retratos = new HashMap<>();
    private Temporada temporada;
    private String nome = "NARRADOR";
    private int cor = 0xFFB8202E;

    private Fala atual;
    private Fala pendente;
    private float time, falaDesde, caladoDesde = -999f;
    /** Faixa na tela: quando entrou e quando começou a sair (-1: não está / não saindo). */
    private float entrou = -1f, saiu = -1f, ultimaFala = -999f;
    private String expressao = "normal";
    private long frame;

    public Narracao(RenderAssets assets) {
        this.assets = assets;
    }

    /** Lê as falas do {@link #ARQUIVO}. Falso (e narração muda) se ele faltar ou estiver quebrado. */
    public boolean carregar() {
        try {
            fromJson(assets.readText(ARQUIVO));
        } catch (IOException | RuntimeException ex) {
            eventos.clear();
            return false;
        }
        return true;
    }

    /** As falas a partir do JSON (testes e {@link #carregar}). */
    void fromJson(String json) {
        eventos.clear();
        Map<String, Object> raiz = Episodio.asMap(MiniJson.parse(json));
        nome = Episodio.string(raiz, "nome", nome);
        String c = Episodio.string(raiz, "cor", null);
        if (c != null) cor = Temporada.parseColor(c);
        for (Map.Entry<String, Object> e : Episodio.asMap(raiz.get("eventos")).entrySet()) {
            Map<String, Object> m = Episodio.asMap(e.getValue());
            List<String> falas = new ArrayList<>();
            for (Object f : Episodio.list(m, "falas")) falas.add(String.valueOf(f));
            if (falas.isEmpty()) throw new IllegalArgumentException("evento " + e.getKey() + " sem falas");
            int prioridade = Episodio.number(m, "prioridade", 1).intValue();
            eventos.put(e.getKey(), new Evento(Math.max(1, Math.min(3, prioridade)),
                Episodio.string(m, "expressao", "normal"), Collections.unmodifiableList(falas)));
        }
    }

    /** Tradução das falas (o idioma da temporada); null fala em português. */
    public void setTemporada(Temporada temporada) {
        this.temporada = temporada;
    }

    public boolean tem(String evento) {
        return eventos.containsKey(evento);
    }

    /** Os eventos do arquivo (testes, documentação). */
    Map<String, Evento> eventos() {
        return Collections.unmodifiableMap(eventos);
    }

    /**
     * Pede a fala de {@code evento} com as variáveis do jogo ({@code {nome}} no texto vira o
     * valor de "nome"). Verdadeiro se a fala entrou (na tela ou na fila); falso se o evento
     * não existe ou a regra da prioridade a deixou de fora.
     */
    public boolean narrar(String evento, Map<String, String> vars) {
        Evento ev = eventos.get(evento);
        if (ev == null) return false;
        int p = ev.prioridade;
        Float vez = ultimaVez.get(evento);
        if (p < 3 && vez != null && time - vez < REPETE_DEPOIS) return false;
        if (p == 1 && (atual != null || pendente != null || time - caladoDesde < PAUSA_COMENTARIO)) return false;
        if (p == 2 && atual == null && time - caladoDesde < PAUSA_IMPORTANTE && pendente == null) {
            // Logo depois de outra fala: espera na fila o respiro.
            pendente = fala(evento, ev, vars);
            ultimaVez.put(evento, time);
            return true;
        }
        Fala f = fala(evento, ev, vars);
        ultimaVez.put(evento, time);
        if (atual == null) {
            mostrar(f);
        } else if (p == 3 && atual.prioridade == 3) {
            // Grande atrás de grande (o K.O. e logo a vitória): espera a vez, uma depois da outra.
            pendente = f;
        } else if (p == 3 || p > atual.prioridade) {
            // A grande corta o que estiver falando; a fila de comentários menores cai.
            if (pendente != null && pendente.prioridade <= p) pendente = null;
            mostrar(f);
        } else if (pendente == null || pendente.prioridade <= p) {
            pendente = f;
        } else {
            return false;
        }
        return true;
    }

    private Fala fala(String evento, Evento ev, Map<String, String> vars) {
        int n = ev.falas.size();
        Integer ultima = ultimaEscolha.get(evento);
        rand.reset(frame * 31L + evento.hashCode() * 7L + n);
        int escolha;
        if (n == 1) {
            escolha = 0;
        } else if (ultima == null) {
            escolha = (int)(rand.next() * n) % n;
        } else {
            // Sorteia sem repetir a última.
            escolha = (ultima + 1 + (int)(rand.next() * (n - 1))) % n;
        }
        ultimaEscolha.put(evento, escolha);
        String modelo = temporada != null ? temporada.t(ev.falas.get(escolha)) : ev.falas.get(escolha);
        return new Fala(evento, ev.prioridade, ev.expressao, preencher(modelo, vars));
    }

    static String preencher(String modelo, Map<String, String> vars) {
        if (vars == null || vars.isEmpty() || modelo.indexOf('{') < 0) return modelo;
        String out = modelo;
        for (Map.Entry<String, String> v : vars.entrySet()) {
            out = out.replace("{" + v.getKey() + "}", v.getValue() == null ? "" : v.getValue());
        }
        return out;
    }

    private void mostrar(Fala f) {
        atual = f;
        falaDesde = time;
        expressao = f.expressao;
        carregarRetrato(f.expressao);
        if (entrou < 0f || saiu >= 0f) {
            entrou = time;
            saiu = -1f;
        }
        ultimaFala = time + duracao(f);
    }

    private void carregarRetrato(String expr) {
        for (String e : new String[] {"normal", expr}) {
            String chave = QUEM + "_" + e;
            if (retratos.containsKey(chave)) continue;
            RenderImage img = null;
            try {
                img = assets.loadImage(ConversaLuta.RETRATOS + "/" + chave + ".png");
            } catch (IOException | RuntimeException ex) {
                // Personagem ainda por fazer: o placeholder desenhado.
            }
            retratos.put(chave, img);
        }
    }

    static float duracao(Fala f) {
        int letras = f.texto.length();
        float segura = Math.max(SEGURA_MIN, Math.min(SEGURA_MAX, SEGURA_MIN + letras * SEGURA_POR_LETRA));
        if (f.prioridade == 3) segura += SEGURA_GRANDE;
        return ENTRA + letras / DIGITA + segura;
    }

    public void update(float dt) {
        frame++;
        time += dt;
        if (atual != null && time - falaDesde >= duracao(atual)) {
            atual = null;
            caladoDesde = time;
            if (pendente != null) {
                Fala p = pendente;
                pendente = null;
                mostrar(p);
            }
        }
        if (atual == null && entrou >= 0f && saiu < 0f && time - ultimaFala >= SAIR_DEPOIS) saiu = time;
        if (saiu >= 0f && time - saiu >= SAI) {
            entrou = -1f;
            saiu = -1f;
        }
    }

    /** Fala na tela ou na fila. */
    public boolean falando() {
        return atual != null || pendente != null;
    }

    /** O texto na tela agora (null: calado). */
    public String textoAtual() {
        return atual != null ? atual.texto : null;
    }

    public String eventoAtual() {
        return atual != null ? atual.evento : null;
    }

    /** Some com a faixa e esquece a fila (luta nova, saída da luta). */
    public void stop() {
        atual = null;
        pendente = null;
        entrou = saiu = -1f;
        caladoDesde = time - PAUSA_COMENTARIO;
        ultimaVez.clear();
    }

    // ------------------------------------------------------------ desenho

    public void render(RenderCanvas c) {
        if (entrou < 0f) return;
        float in = Tinta.easeOutBack(Tinta.clamp01((time - entrou) / ENTRA));
        float out = saiu >= 0f ? Tinta.clamp01((time - saiu) / SAI) : 0f;
        float dy = 130f * (1f - in) + 130f * out * out;
        boolean grande = atual != null && atual.prioridade == 3;
        float idade = atual != null ? time - falaDesde : 99f;
        // A fala grande entra com um tranco.
        float tranco = grande && idade < 0.25f ? (float)Math.sin(idade * 80f) * 3f * (1f - idade / 0.25f) : 0f;
        c.save();
        c.translate(tranco, dy);
        drawFaixa(c, grande);
        drawQuadro(c);
        c.restore();
    }

    private void drawFaixa(RenderCanvas c, boolean grande) {
        float x0 = FAIXA_ESQ, x1 = FAIXA_DIR, top = FAIXA_TOPO, base = FAIXA_BASE;
        float ponta = 16f;
        float[] faixa = {x0, top, x1 + ponta, top, x1, base, x0, base};
        float[] sombra = {x0 + 5f, top + 6f, x1 + ponta + 5f, top + 6f, x1 + 5f, base + 6f, x0 + 5f, base + 6f};
        c.fillPolygon(sombra, 0x66000000);
        c.fillPolygon(faixa, FUNDO);
        // Fio na cor do narrador em cima, como uma transmissão.
        c.fillPolygon(new float[] {x0, top, x1 + ponta, top, x1 + ponta - 1.5f, top + 4f, x0, top + 4f}, cor);
        c.strokePolygon(faixa, 1.5f, 0x55FFFFFF);

        // Plaquinha com o nome e a luz de "no ar".
        float nomeW = c.measureText(nome, NOME, true) + 32f;
        float nx = x0 + 20f;
        c.fillRect(nx - 2f, top - 17f, nx + nomeW + 2f, top + 5f, INK);
        c.fillRect(nx, top - 15f, nx + nomeW, top + 3f, cor);
        boolean luz = ((int)(time * 2.5f)) % 2 == 0 || atual != null;
        c.fillCircle(nx + 11f, top - 6f, 4.5f, luz ? 0xFFFF4D5E : 0xFF6B1A22);
        c.strokeCircle(nx + 11f, top - 6f, 4.5f, 1.2f, INK);
        c.drawText(nome, nx + 20f + (nomeW - 24f) * 0.5f, top + 0.5f, NOME, WHITE, INK, 2.5f, true);

        if (atual == null) return;
        float idade = time - falaDesde - ENTRA * 0.5f;
        if (idade <= 0f) return;
        float size = grande ? TEXTO_GRANDE : TEXTO;
        List<String> linhas = Baloes.wrap(c, atual.texto, size, x1 - x0 - 26f);
        if (linhas.size() > 3) linhas = linhas.subList(0, 3);
        int visiveis = (int)Math.max(1f, idade * DIGITA);
        float y = top + 6f + (base - top - 6f - linhas.size() * LINHA) * 0.5f + LINHA - 5f;
        int corTexto = grande ? AMARELO : WHITE;
        for (String linha : linhas) {
            if (visiveis <= 0) break;
            String parte = linha.length() <= visiveis ? linha : linha.substring(0, visiveis);
            visiveis -= linha.length() + 1;
            float pw = c.measureText(parte, size, grande);
            c.drawText(parte, x0 + 22f + pw * 0.5f, y, size, corTexto, INK, 2.4f, grande);
            y += LINHA;
        }
    }

    private void drawQuadro(RenderCanvas c) {
        float[] poly = QUADRO;
        float[] caixa = Layouts.caixa(poly);
        float cx = (caixa[0] + caixa[2]) * 0.5f, cy = (caixa[1] + caixa[3]) * 0.5f;
        boolean falandoAgora = atual != null;
        float amplitude = "chocado".equals(expressao) ? 2.2f : "empolgado".equals(expressao) ? 2f : 1.2f;
        float bob = falandoAgora ? (float)Math.sin(time * 10f) * amplitude : 0f;
        float tremor = falandoAgora && "chocado".equals(expressao) ? (float)Math.sin(time * 47f) * 1.2f : 0f;

        float[] sombra = new float[poly.length];
        for (int i = 0; i < poly.length; i += 2) {
            sombra[i] = poly[i] + 6f;
            sombra[i + 1] = poly[i + 1] + 7f;
        }
        c.fillPolygon(sombra, 0x77000000);
        c.save();
        c.translate(tremor, bob);
        c.save();
        c.clipPolygon(poly);
        c.fillRect(caixa[0], caixa[1], caixa[2], caixa[3], Tinta.darken(cor, 0.45f));
        drawLinhasDeFoco(c, caixa);
        RenderImage img = retratos.get(QUEM + "_" + expressao);
        if (img == null) img = retratos.get(QUEM + "_normal");
        if (img != null) {
            float w = caixa[2] - caixa[0], h = caixa[3] - caixa[1];
            float s = Math.max(w / img.width(), h / img.height()) * 1.02f;
            float iw = img.width() * s, ih = img.height() * s;
            float x = caixa[0] + (w - iw) * 0.5f, yy = caixa[1] + (h - ih) * 0.15f;
            c.drawImage(img, x, yy, x + iw, yy + ih, 255);
        } else {
            drawSilhueta(c, cx, cy);
        }
        c.restore();
        c.strokePolygon(poly, 6f, WHITE);
        c.strokePolygon(poly, 3.5f, INK);
        c.restore();
    }

    /** O placeholder: silhueta de tinta com o microfone na mão e um "?" no rosto. */
    private void drawSilhueta(RenderCanvas c, float cx, float cy) {
        float hy = cy - 14f;
        // Ombros e tronco.
        c.fillPolygon(new float[] {cx - 44f, cy + 60f, cx - 30f, cy + 18f, cx - 10f, cy + 8f, cx + 10f, cy + 8f,
            cx + 30f, cy + 18f, cx + 44f, cy + 60f}, INK);
        // Cabeça.
        c.fillCircle(cx, hy, 18f, INK);
        // Braço com o microfone até a boca.
        c.drawLine(cx + 26f, cy + 30f, cx + 20f, hy + 10f, 7f, INK);
        c.drawLine(cx + 20f, hy + 10f, cx + 12f, hy + 6f, 4f, 0xFF9AA3B5);
        c.fillCircle(cx + 11f, hy + 5f, 5.5f, 0xFFC8CFDB);
        c.strokeCircle(cx + 11f, hy + 5f, 5.5f, 1.2f, INK);
        c.drawText("?", cx - 3f, hy + 8f, 22f, Tinta.withAlpha(WHITE, 220), INK, 2f, true);
    }

    private void drawLinhasDeFoco(RenderCanvas c, float[] caixa) {
        float cx = (caixa[0] + caixa[2]) * 0.5f, cy = caixa[1] + (caixa[3] - caixa[1]) * 0.42f;
        float r = Math.max(caixa[2] - caixa[0], caixa[3] - caixa[1]);
        for (int i = 0; i < 24; i++) {
            double a = Math.PI * 2 * i / 24 + time * 0.4;
            float cos = (float)Math.cos(a), sin = (float)Math.sin(a);
            c.drawLine(cx + cos * r * 0.3f, cy + sin * r * 0.3f, cx + cos * r, cy + sin * r, 2f, 0x55FFE6B0);
        }
    }
}
