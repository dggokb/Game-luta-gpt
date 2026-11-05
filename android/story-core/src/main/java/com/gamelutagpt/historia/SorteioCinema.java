package com.gamelutagpt.historia;

import com.gamelutagpt.render.Rand;
import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * O sorteio da história em cena de cinema (ep04), por cima da chave de {@link ChaveTorneio}.
 * Primeiro os cards dos irmãos (sem giro: as vagas deles são as pontas), depois cada uma das
 * outras duplas sai assim:
 *
 * <ol>
 *   <li><b>Giro</b>: a tela escurece, a vaga da vez pisca e, no meio, os rostos das duplas que
 *       ainda não saíram passam cada vez mais devagar até parar na sorteada;</li>
 *   <li><b>Card</b>: o card de mangá da dupla ({@code torneio/cards/<id>.jpg}, de
 *       {@code tools/historia/cards_sorteio.py}) estala no meio com linhas de foco, o nome numa
 *       pincelada e um balão com a fala dela ({@code "fala"} no {@code torneio.json});</li>
 *   <li><b>Voo</b>: o card encolhe e voa até a vaga, que bate na chave.</li>
 * </ol>
 *
 * Tudo depende só do tempo do passo: o toque pula para o próximo marco ({@link #proximoMarco}).
 * O Arcade (sem rival) continua com o sorteio rápido da chave.
 */
final class SorteioCinema {
    /** Textos da cena que passam pela tradução (as falas vêm do {@code torneio.json}). */
    static final String SORTEANDO = "SORTEANDO...";
    static final String LADO_RYUGA = "LADO DO RYŪGA";
    static final String LADO_KOGA = "LADO DO KŌGA";
    static final String[] TEXTOS = {SORTEANDO, LADO_RYUGA, LADO_KOGA};

    static final String PASTA = Temporada.PASTA + "/torneio/cards/";

    /** Os holofotes na chave vazia, antes do primeiro card. */
    static final float INTRO = 1.0f;
    static final float GIRO = 1.1f;
    static final float CARD = 3.1f;
    static final float VOO = 0.4f;
    static final float POR_DUPLA = GIRO + CARD + VOO;
    /** As duplas dos irmãos entram primeiro (Ryūga na ponta esquerda, Kōga na direita), sem giro. */
    static final int FIXAS = 2;
    /** O balão abre um pouco depois do card e o texto é digitado nesta velocidade. */
    static final float BALAO = 0.35f;
    static final float DIGITA = PaginaHistoria.TYPE_SPEED;

    private static final float VW = PaginaHistoria.VW;
    private static final float CARD_W = 600f, CARD_H = 380f;
    private static final float CARD_CX = VW * 0.5f, CARD_CY = 410f;
    private static final int INK = Baloes.INK;
    private static final int WHITE = Baloes.WHITE;
    private static final int GOLD = 0xFFF2C14E;
    private static final int BLUE = 0xFF4FA3FF;
    private static final int RED = 0xFFE5404C;

    private final RenderAssets assets;
    private final Rostos rostos;
    private final Map<String, RenderImage> cards = new HashMap<>();
    private final Rand rand = new Rand(11);

    SorteioCinema(RenderAssets assets, Rostos rostos) {
        this.assets = assets;
        this.rostos = rostos;
    }

    /** A chave tem a cena (história: os irmãos fixos e um rival); o Arcade não. */
    static boolean ativo(Torneio t) {
        return t != null && t.rival() != null;
    }

    /** Quantos cards a cena mostra: os dois irmãos e as duplas sorteadas. */
    static int entradas(Torneio t) {
        return FIXAS + t.ordemSorteio().length;
    }

    /** A vaga do card {@code k}: 0 = o Ryūga (vaga 0), 1 = o Kōga (última vaga), depois o sorteio. */
    static int vagaDo(Torneio t, int k) {
        if (k == 0) return 0;
        if (k == 1) return t.vagas() - 1;
        return t.ordemSorteio()[k - FIXAS];
    }

    /** Os irmãos não giram: as vagas deles são fixas. */
    static boolean gira(int k) {
        return k >= FIXAS;
    }

    /** Quando o card {@code k} começa (o giro, ou o card direto nos irmãos). */
    static float inicio(int k) {
        return INTRO + Math.min(k, FIXAS) * (CARD + VOO) + Math.max(0, k - FIXAS) * POR_DUPLA;
    }

    /** Quando o card {@code k} estala no meio da tela. */
    static float inicioCard(int k) {
        return inicio(k) + (gira(k) ? GIRO : 0f);
    }

    /** Quando o card {@code k} bate na vaga dele. */
    static float chegada(int k) {
        return inicio(k + 1);
    }

    /** Quando a dupla da vaga bate nela (para a chave desenhar a vaga cheia). */
    static float chegadaDaVaga(Torneio t, int vaga) {
        for (int k = 0; k < entradas(t); k++) {
            if (vagaDo(t, k) == vaga) return chegada(k);
        }
        return 0f;
    }

    static float duracao(Torneio t) {
        return chegada(entradas(t) - 1) + ChaveTorneio.SLAM;
    }

    /** O tempo em que a fala do card {@code k} termina de ser digitada. */
    static float falaCompleta(Temporada temporada, Torneio t, int k) {
        String fala = fala(temporada, t, k);
        return inicioCard(k) + BALAO + (fala == null ? 0f : fala.length() / DIGITA);
    }

    /**
     * Para onde o toque leva o tempo: do giro ao card, do card com o texto pela metade ao texto
     * inteiro, do texto inteiro ao voo e do voo ao card seguinte. Depois do fim, devolve o próprio
     * tempo (o passo segue).
     */
    static float proximoMarco(Temporada temporada, Torneio t, float time) {
        if (time < INTRO) return INTRO;
        for (int k = 0; k < entradas(t); k++) {
            if (time >= chegada(k)) continue;
            if (time < inicioCard(k)) return inicioCard(k);
            float completa = falaCompleta(temporada, t, k);
            if (time < completa) return completa;
            if (time < inicioCard(k) + CARD) return inicioCard(k) + CARD;
            return chegada(k);
        }
        return Math.max(time, duracao(t));
    }

    /** O som de um marco cruzado entre {@code antes} e {@code agora} (o último deles), ou null. */
    static String som(Torneio t, float antes, float agora) {
        String som = null;
        for (int k = 0; k < entradas(t); k++) {
            if (gira(k) && cruzou(antes, agora, inicio(k))) som = "painel";
            if (cruzou(antes, agora, inicioCard(k))) som = "impacto";
            if (cruzou(antes, agora, chegada(k))) som = "painel";
        }
        return som;
    }

    private static boolean cruzou(float antes, float agora, float marco) {
        return antes < marco && agora >= marco;
    }

    private static String fala(Temporada temporada, Torneio t, int k) {
        Torneio.Dupla d = t.participante(0, vagaDo(t, k));
        return d == null || d.fala == null ? null : temporada.t(d.fala);
    }

    void liberar() {
        cards.clear();
    }

    RenderImage card(String id) {
        if (cards.containsKey(id)) return cards.get(id);
        RenderImage image = null;
        try {
            image = assets.loadImage(PASTA + id + ".jpg");
        } catch (IOException | RuntimeException ex) {
            // Sem o card: os dois retratos grandes.
        }
        cards.put(id, image);
        return image;
    }

    // ------------------------------------------------------------ desenho

    /** Por cima da chave desenhada por {@link ChaveTorneio#draw}. */
    void draw(RenderCanvas c, Temporada temporada, Torneio t, float time) {
        if (time < INTRO) {
            drawHolofotes(c, time);
            return;
        }
        for (int k = 0; k < entradas(t); k++) {
            if (time < inicio(k) || time >= chegada(k)) continue;
            int vaga = vagaDo(t, k);
            Torneio.Dupla d = t.participante(0, vaga);
            float card = time - inicioCard(k);
            if (card < 0f) {
                drawGiro(c, temporada, t, k - FIXAS, (time - inicio(k)) / GIRO, time);
            } else if (card < CARD) {
                drawCard(c, temporada, t, d, vaga, card, time);
            } else {
                drawVoo(c, temporada, t, d, vaga, (card - CARD) / VOO);
            }
            return;
        }
    }

    /** Dois holofotes varrendo a chave vazia antes do primeiro card. */
    private void drawHolofotes(RenderCanvas c, float time) {
        float a = Tinta.clamp01(time / 0.4f) * Tinta.clamp01((INTRO - time) / 0.4f);
        float x1 = 200f + 880f * Tinta.easeOutCubic(time / INTRO);
        float x2 = 1080f - 880f * Tinta.easeOutCubic(time / INTRO);
        c.fillRadialGlow(x1, 380f, 260f, Tinta.withAlpha(0xFF6FA8FF, (int)(70 * a)));
        c.fillRadialGlow(x2, 380f, 260f, Tinta.withAlpha(0xFFFF6F6F, (int)(70 * a)));
    }

    private void escurece(RenderCanvas c, int alpha) {
        c.fillRect(-40f, -40f, VW + 40f, PaginaHistoria.VH + 40f, Tinta.withAlpha(0xFF000000, alpha));
    }

    /**
     * A vaga da vez pisca e os rostos das duplas que faltam passam até parar na sorteada.
     *
     * @param k a dupla na ordem do sorteio (sem os irmãos)
     */
    private void drawGiro(RenderCanvas c, Temporada temporada, Torneio t, int k, float u, float time) {
        int[] ordem = t.ordemSorteio();
        escurece(c, (int)(165 * Tinta.clamp01(u * 4f)));
        // A vaga que vai receber a dupla, acesa.
        ChaveTorneio.Layout lay = ChaveTorneio.layout(t);
        float[] p = ChaveTorneio.posicao(t, 0, ordem[k]);
        float pisca = 0.55f + 0.45f * (float)Math.sin(time * 18f);
        float[] vaga = {p[0], p[1], p[0] + lay.cardW, p[1], p[0] + lay.cardW, p[1] + lay.cardH, p[0], p[1] + lay.cardH};
        c.fillPolygon(vaga, Tinta.withAlpha(GOLD, (int)(60 * pisca)));
        c.strokePolygon(vaga, 4f, Tinta.withAlpha(GOLD, (int)(255 * pisca)));
        c.drawText("?", p[0] + lay.cardW * 0.5f, p[1] + lay.cardH * 0.66f, lay.cardH * 0.46f,
            Tinta.withAlpha(GOLD, (int)(255 * pisca)), 0, 0f, true);

        // A roleta: as duplas que ainda não saíram, cada vez mais devagar, parando na da vez.
        int resta = ordem.length - k;
        float fase = (2f * resta + 0.6f) * Tinta.easeOutCubic(Math.min(1f, u / 0.85f));
        int idx = ((int)fase) % resta;
        Torneio.Dupla d = t.participante(0, ordem[k + idx]);
        boolean parou = u >= 0.85f;
        float w = 420f, h = 266f, cx = CARD_CX, cy = 380f;
        float pop = parou ? Tinta.easeOutBack(Tinta.clamp01((u - 0.85f) / 0.15f)) : 0f;
        float escala = 0.9f + 0.1f * pop;
        float[] moldura = retangulo(cx, cy, w * escala, h * escala);
        linhasDeFoco(c, cx, cy, 260f, time, parou ? 200 : 110);
        c.fillPolygon(moldura, 0xF0101420);
        c.strokePolygon(moldura, 5f, parou ? GOLD : 0xFFE9E1CF);
        float lado = 150f * escala;
        float m = (w * escala - 2f * lado) / 3f, l = cx - w * escala * 0.5f;
        rostos.draw(c, d.lutadores[0], l + m, cy - lado * 0.5f - 12f, lado, INK, 3f, 255);
        rostos.draw(c, d.lutadores[1], l + 2f * m + lado, cy - lado * 0.5f - 12f, lado, INK, 3f, 255);
        String nome = Rostos.nomeDupla(temporada, d);
        c.drawText(nome, cx, cy + h * escala * 0.5f - 16f, BotaoMenu.fit(c, nome, 24f, w - 40f, true), WHITE, INK, 4f, true);
        String sorteando = temporada.t(SORTEANDO);
        c.drawText(sorteando, cx, cy - h * 0.5f - 22f, 30f, parou ? GOLD : WHITE, INK, 5f, true);
        if (parou) c.fillPolygon(moldura, Tinta.withAlpha(WHITE, (int)(200 * (1f - Tinta.clamp01((u - 0.85f) / 0.15f)))));
    }

    /** O card de mangá da dupla no meio, com o nome, o lado da chave e a fala. */
    private void drawCard(RenderCanvas c, Temporada temporada, Torneio t, Torneio.Dupla d, int vaga, float v, float time) {
        escurece(c, 190);
        linhasDeFoco(c, CARD_CX, CARD_CY, 330f, time, 170);
        float pop = Tinta.clamp01(v / 0.22f);
        float escala = 1.35f - 0.35f * Tinta.easeOutBack(pop);
        c.save();
        c.rotate(-2.5f * Tinta.easeOutCubic(pop), CARD_CX, CARD_CY);
        c.scale(escala, escala, CARD_CX, CARD_CY);
        desenhaCard(c, d, CARD_CX, CARD_CY, CARD_W, CARD_H, 255);
        // Nome numa pincelada vermelha embaixo, o lado da chave numa etiqueta em cima.
        float l = CARD_CX - CARD_W * 0.5f, base = CARD_CY + CARD_H * 0.5f;
        String nome = Rostos.nomeDupla(temporada, d);
        float tam = BotaoMenu.fit(c, nome, 34f, CARD_W - 80f, true);
        float nw = c.measureText(nome, tam, true) + 70f;
        c.fillPolygon(new float[] {l - 24f, base - 30f, l - 24f + nw, base - 42f, l - 6f + nw, base + 22f, l - 36f, base + 30f}, 0xFFC8202C);
        c.drawText(nome, l - 26f + nw * 0.5f, base + 4f, tam, WHITE, INK, 5f, true);
        boolean ladoRyuga = vaga < t.vagas() / 2;
        String lado = temporada.t(ladoRyuga ? LADO_RYUGA : LADO_KOGA);
        float lw = c.measureText(lado, 20f, true) + 28f;
        // A etiqueta vai embaixo, à direita: em cima fica o balão.
        float lx = l + CARD_W - lw - 18f;
        c.fillRect(lx - 3f, base - 17f, lx + lw + 3f, base + 17f, INK);
        c.fillRect(lx, base - 14f, lx + lw, base + 14f, ladoRyuga ? BLUE : RED);
        c.drawText(lado, lx + lw * 0.5f, base + 7f, 20f, WHITE, INK, 3f, true);
        if (pop < 1f) c.fillPolygon(retangulo(CARD_CX, CARD_CY, CARD_W, CARD_H), Tinta.withAlpha(WHITE, (int)(230 * (1f - pop))));
        c.restore();

        if (d.fala != null && v >= BALAO) {
            String fala = temporada.t(d.fala);
            int visiveis = (int)((v - BALAO) * DIGITA);
            float balaoPop = Tinta.clamp01((v - BALAO) / 0.2f);
            drawBalao(c, fala, visiveis, balaoPop, d.quemFala);
        }
    }

    /** O card encolhe e voa do meio da tela até a vaga dele na chave. */
    private void drawVoo(RenderCanvas c, Temporada temporada, Torneio t, Torneio.Dupla d, int vaga, float w) {
        float e = w * w * w;
        escurece(c, (int)(190 * (1f - e)));
        ChaveTorneio.Layout lay = ChaveTorneio.layout(t);
        float[] p = ChaveTorneio.posicao(t, 0, vaga);
        float tx = p[0] + lay.cardW * 0.5f, ty = p[1] + lay.cardH * 0.5f;
        float cx = CARD_CX + (tx - CARD_CX) * e, cy = CARD_CY + (ty - CARD_CY) * e;
        float cw = CARD_W + (lay.cardW - CARD_W) * e, ch = CARD_H + (lay.cardH - CARD_H) * e;
        c.save();
        c.rotate(-2.5f * (1f - e), cx, cy);
        desenhaCard(c, d, cx, cy, cw, ch, 255);
        c.restore();
    }

    /** A arte do card (ou, sem ela, os dois retratos) com contorno e sombra. */
    private void desenhaCard(RenderCanvas c, Torneio.Dupla d, float cx, float cy, float w, float h, int alpha) {
        float[] caixa = retangulo(cx, cy, w, h);
        c.fillPolygon(retangulo(cx + 10f, cy + 12f, w, h), Tinta.withAlpha(0xFF000000, alpha * 3 / 5));
        RenderImage arte = card(d.id);
        if (arte != null) {
            c.drawImage(arte, cx - w * 0.5f, cy - h * 0.5f, cx + w * 0.5f, cy + h * 0.5f, alpha);
        } else {
            c.fillPolygon(caixa, Tinta.withAlpha(0xFF141724, alpha));
            float lado = Math.min(h * 0.8f, w * 0.42f);
            float m = (w - 2f * lado) / 3f, l = cx - w * 0.5f;
            rostos.draw(c, d.lutadores[0], l + m, cy - lado * 0.5f, lado, INK, 3f, alpha);
            rostos.draw(c, d.lutadores[1], l + 2f * m + lado, cy - lado * 0.5f, lado, INK, 3f, alpha);
        }
        c.strokePolygon(caixa, Math.max(3f, w / 90f), Tinta.withAlpha(INK, alpha));
    }

    /** Balão de fala em cima do card, com o bico no lado de quem fala. */
    private void drawBalao(RenderCanvas c, String texto, int visiveis, float pop, int quem) {
        float size = 27f;
        List<String> linhas = Baloes.wrap(c, texto, size, 400f);
        float textW = 0f;
        for (String linha : linhas) textW = Math.max(textW, c.measureText(linha, size, false));
        float textH = linhas.size() * size * 1.22f;
        float w = textW * 1.18f + 56f, h = textH * 1.25f + 40f;
        float lado = quem == 0 ? -1f : 1f;
        float cx = CARD_CX + lado * Math.max(40f, CARD_W * 0.5f - w * 0.5f - 10f);
        float cy = CARD_CY - CARD_H * 0.5f - h * 0.5f + 30f;
        float tx = CARD_CX + lado * CARD_W * 0.28f, ty = CARD_CY - CARD_H * 0.5f + 90f;
        float escala = 0.6f + 0.4f * Tinta.easeOutBack(pop);
        c.save();
        c.scale(escala, escala, cx, cy);
        float bx = cx + lado * w * 0.12f;
        float[] bico = {bx - 22f, cy + h * 0.32f, tx, ty, bx + 22f, cy + h * 0.32f};
        c.strokePolygon(bico, 7f, INK);
        c.fillOval(cx - w * 0.5f - 3.5f, cy - h * 0.5f - 3.5f, cx + w * 0.5f + 3.5f, cy + h * 0.5f + 3.5f, INK);
        c.fillPolygon(bico, WHITE);
        c.fillOval(cx - w * 0.5f, cy - h * 0.5f, cx + w * 0.5f, cy + h * 0.5f, WHITE);
        int resta = visiveis;
        float y = cy - textH * 0.5f + size * 0.92f;
        for (String linha : linhas) {
            if (resta <= 0) break;
            float lw = c.measureText(linha, size, false);
            Tinta.digitado(c, linha, resta, cx - lw * 0.5f, y, size, INK, 0, 0f, false);
            resta -= linha.length() + 1;
            y += size * 1.22f;
        }
        c.restore();
    }

    /** Linhas de foco de mangá da borda da tela para o centro, tremeluzindo. */
    private void linhasDeFoco(RenderCanvas c, float cx, float cy, float raio, float time, int alpha) {
        rand.reset((long)(time * 20f) * 977L);
        for (int i = 0; i < 70; i++) {
            float a = rand.range(0f, (float)(Math.PI * 2));
            float dentro = raio * rand.range(1.0f, 1.35f);
            float cos = (float)Math.cos(a), sin = (float)Math.sin(a);
            float largura = rand.range(2f, 7f);
            c.drawLine(cx + cos * dentro, cy + sin * dentro * 0.75f, cx + cos * 1100f, cy + sin * 820f, largura,
                Tinta.withAlpha(i % 3 == 0 ? GOLD : WHITE, alpha / (i % 3 == 0 ? 2 : 1)));
        }
    }

    private static float[] retangulo(float cx, float cy, float w, float h) {
        float l = cx - w * 0.5f, t = cy - h * 0.5f, r = cx + w * 0.5f, b = cy + h * 0.5f;
        return new float[] {l, t, r, t, r, b, l, b};
    }
}
