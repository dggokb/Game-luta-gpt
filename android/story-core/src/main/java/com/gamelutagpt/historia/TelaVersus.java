package com.gamelutagpt.historia;

import com.gamelutagpt.render.Rand;
import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A tela de versus antes de cada luta do torneio (e do duelo da regra escondida), montada com as
 * peças de {@code historia/torneio/versus/} (de {@code tools/historia/versus_arte.py}, com as
 * imagens do Codex):
 *
 * <ul>
 *   <li>o templo à noite de fundo, partido na diagonal por um raio: cada metade com a cor do seu
 *       lado (azul do Ryūga, vermelho do Kōga);</li>
 *   <li>o espírito de cada irmão atrás da dupla dele (o dragão do Ryūga, o tigre do Kōga);</li>
 *   <li>os lutadores da cintura para cima: o titular grande perto do meio, o parceiro atrás, do
 *       lado de fora; o da direita espelhado, olhando para o meio;</li>
 *   <li>o "VS" de prata que bate no meio, o logo em cima, a rodada, as placas de nome (P1 / CPU,
 *       com o parceiro em "TAG") e fumaça embaixo.</li>
 * </ul>
 *
 * Tudo entra deslizando e o VS bate com clarão e tremor; no fim, um clarão branco leva para a
 * luta. Sem uma peça, a tela desenha uma reserva (o VS em texto, sem espírito etc.).
 */
final class TelaVersus {
    static final String PASTA = Temporada.PASTA + "/torneio/versus/";

    static final float DURACAO = 4.6f;
    /** O "VS" bate no meio. */
    static final float VS_EM = 0.62f;
    /** Antes disso o toque não pula (a entrada não some num toque sem querer). */
    static final float MIN_TOQUE = 0.9f;
    /** O clarão final, que leva para a luta. */
    static final float SAIDA = 0.35f;

    private static final float VW = PaginaHistoria.VW, VH = PaginaHistoria.VH;
    /** Altura na tela do titular e do parceiro (as peças são da cintura para cima). */
    private static final float ALTURA_TITULAR = 650f, ALTURA_PARCEIRO = 540f;
    private static final int INK = Baloes.INK;
    private static final int WHITE = Baloes.WHITE;
    private static final int GOLD = 0xFFF2C14E;
    private static final int AZUL = 0xFF2F6FD6, AZUL_CLARO = 0xFF7FC0FF;
    private static final int VERMELHO = 0xFFC62F2F, VERMELHO_CLARO = 0xFFFF8A6A;

    /** Um lado da tela: a dupla, a cor e o espírito (null = sem). */
    private static final class Lado {
        String[] lutadores = new String[0];
        boolean azul;
        String espirito;
    }

    private final RenderAssets assets;
    private final Map<String, RenderImage> imagens = new HashMap<>();
    private final Rand rand = new Rand(23);
    private final Lado esq = new Lado(), dir = new Lado();
    private String rotulo = "";

    TelaVersus(RenderAssets assets) {
        this.assets = assets;
    }

    /**
     * Começa a tela para as duplas {jogador, adversário} (lutadores) e o nome da rodada. Cada irmão
     * tem a cor e o espírito dele (Ryūga: azul e dragão; Kōga: vermelho e tigre); quem não é irmão
     * fica com a cor que sobrou, sem espírito.
     */
    void begin(String[][] lados, String rotulo) {
        esq.lutadores = lados[0].clone();
        dir.lutadores = lados[1].clone();
        String irmaoEsq = irmao(esq.lutadores), irmaoDir = irmao(dir.lutadores);
        esq.azul = !"koga".equals(irmaoEsq);
        dir.azul = !esq.azul;
        esq.espirito = espirito(irmaoEsq);
        dir.espirito = espirito(irmaoDir);
        this.rotulo = rotulo == null ? "" : rotulo;
    }

    /** O irmão que é o titular da dupla ("ryuga" / "koga"), ou null. */
    static String irmao(String[] lutadores) {
        if (lutadores.length == 0) return null;
        String c = Rostos.codigo(lutadores[0]);
        return "p01".equals(c) ? "ryuga" : "p03".equals(c) ? "koga" : null;
    }

    private static String espirito(String irmao) {
        return "ryuga".equals(irmao) ? "dragao.png" : "koga".equals(irmao) ? "tigre.png" : null;
    }

    void liberar() {
        imagens.clear();
    }

    /** O som de um marco cruzado entre {@code antes} e {@code agora}, ou null. */
    static String som(float antes, float agora) {
        return antes < VS_EM && agora >= VS_EM ? "impacto" : null;
    }

    /** Para onde o toque leva o tempo: direto ao clarão final (antes de {@link #MIN_TOQUE}, nada). */
    static float toque(float time) {
        if (time < MIN_TOQUE) return time;
        return Math.max(time, DURACAO - SAIDA);
    }

    private RenderImage imagem(String arquivo) {
        if (arquivo == null) return null;
        if (imagens.containsKey(arquivo)) return imagens.get(arquivo);
        RenderImage image = null;
        try {
            image = assets.loadImage(PASTA + arquivo);
        } catch (IOException | RuntimeException ex) {
            // Sem a peça: a reserva desenhada.
        }
        imagens.put(arquivo, image);
        return image;
    }

    // ------------------------------------------------------------ desenho

    void draw(RenderCanvas c, Temporada temporada, float time) {
        float empurra = 1f + 0.04f * Tinta.clamp01(time / DURACAO);
        c.save();
        c.scale(empurra, empurra, VW * 0.5f, VH * 0.6f);
        drawFundo(c, time);
        drawEspirito(c, esq, false, time);
        drawEspirito(c, dir, true, time);
        float entra = Tinta.easeOutBack(Tinta.clamp01((time - 0.06f) / 0.42f));
        float entraParceiro = Tinta.easeOutBack(Tinta.clamp01((time - 0.16f) / 0.42f));
        float dx = (1f - entra) * 700f, dxp = (1f - entraParceiro) * 700f;
        if (esq.lutadores.length > 1) drawLutador(c, esq.lutadores[1], 175f - dxp, ALTURA_PARCEIRO, false);
        if (dir.lutadores.length > 1) drawLutador(c, dir.lutadores[1], VW - 175f + dxp, ALTURA_PARCEIRO, true);
        if (esq.lutadores.length > 0) drawLutador(c, esq.lutadores[0], 420f - dx, ALTURA_TITULAR, false);
        if (dir.lutadores.length > 0) drawLutador(c, dir.lutadores[0], VW - 420f + dx, ALTURA_TITULAR, true);
        drawFumaca(c, time);
        drawFaiscas(c, time);
        c.restore();

        drawRaio(c, time);
        drawLogo(c, temporada, time);
        drawPlaca(c, temporada, esq, Tinta.easeOutCubic((time - 0.85f) / 0.3f), false, "P1");
        drawPlaca(c, temporada, dir, Tinta.easeOutCubic((time - 0.95f) / 0.3f), true, "CPU");
        drawVs(c, time);

        // Clarões: o da abertura, o do VS e o da saída para a luta.
        int flash = (int)(255 * (1f - Tinta.clamp01(time / 0.22f)));
        float pos = time - VS_EM;
        if (pos >= 0f) flash = Math.max(flash, (int)(200 * (1f - Tinta.clamp01(pos / 0.3f))));
        float saida = (time - (DURACAO - SAIDA)) / SAIDA;
        if (saida > 0f) flash = Math.max(flash, (int)(255 * Tinta.clamp01(saida)));
        if (flash > 0) c.fillRect(-40f, -40f, VW + 40f, VH + 40f, Tinta.withAlpha(WHITE, flash));
    }

    /** O templo de fundo, cada metade tingida da cor do seu lado e entrando pela borda dela. */
    private void drawFundo(RenderCanvas c, float time) {
        c.fillRect(-60f, -60f, VW + 60f, VH + 60f, 0xFF05060B);
        float abre = Tinta.easeOutCubic(time / 0.3f);
        float topo = VW * 0.5f + 60f, base = VW * 0.5f - 60f;
        float[][] metades = {
            {-60f, -60f, topo, -60f, base, VH + 60f, -60f, VH + 60f},
            {topo, -60f, VW + 60f, -60f, VW + 60f, VH + 60f, base, VH + 60f}};
        RenderImage templo = imagem("templo.jpg");
        for (int m = 0; m < 2; m++) {
            Lado lado = m == 0 ? esq : dir;
            c.save();
            c.translate((m == 0 ? -1f : 1f) * (1f - abre) * 700f, 0f);
            c.clipPolygon(metades[m]);
            if (templo != null) {
                float h = VH + 80f, w = h * templo.width() / templo.height();
                c.drawImage(templo, VW * 0.5f - w * 0.5f, -40f, VW * 0.5f + w * 0.5f, -40f + h, 255);
            }
            int cor = lado.azul ? 0xFF0B2A6E : 0xFF6E0B0B;
            c.fillVerticalGradient(-60f, -60f, VW + 60f, VH + 60f, Tinta.withAlpha(cor, templo != null ? 150 : 255),
                Tinta.withAlpha(0xFF020308, templo != null ? 120 : 255));
            c.fillRadialGlow(m == 0 ? 300f : VW - 300f, 330f, 560f, lado.azul ? 0x554FA3FF : 0x55FF5A4F);
            linhasDeVelocidade(c, time, m == 0 ? 1f : -1f, lado.azul ? AZUL_CLARO : VERMELHO_CLARO);
            c.restore();
        }
    }

    private void linhasDeVelocidade(RenderCanvas c, float time, float sentido, int cor) {
        for (int i = 0; i < 18; i++) {
            rand.reset(i * 7919L + (sentido > 0 ? 1 : 2));
            float y = rand.range(-20f, VH + 20f);
            float len = rand.range(160f, 520f);
            float vel = rand.range(900f, 1700f);
            float x = ((rand.range(0f, 2000f) + time * vel) % 2000f) - 400f;
            if (sentido < 0) x = VW - x;
            c.drawLine(x, y, x - sentido * len, y, rand.range(1.5f, 4f), Tinta.withAlpha(cor, (int)rand.range(30f, 80f)));
        }
    }

    /** O dragão ou o tigre atrás da dupla, crescendo e respirando devagar. */
    private void drawEspirito(RenderCanvas c, Lado lado, boolean direita, float time) {
        RenderImage im = imagem(lado.espirito);
        if (im == null) return;
        float in = Tinta.easeOutCubic((time - 0.12f) / 0.5f);
        if (in <= 0f) return;
        float w = 760f, h = w * im.height() / im.width();
        float respira = 1f + 0.02f * (float)Math.sin(time * 2.2f);
        float escala = (1.15f - 0.15f * in) * respira;
        float cx = direita ? VW - 300f : 300f, cy = 210f + 6f * (float)Math.sin(time * 1.7f);
        c.save();
        if (direita) c.scale(-1f, 1f, cx, 0f);
        c.scale(escala, escala, cx, cy);
        c.drawImage(im, cx - w * 0.5f, cy - h * 0.5f, cx + w * 0.5f, cy + h * 0.5f, (int)(235 * in));
        c.restore();
    }

    /** Da cintura para cima, com a base na borda de baixo da tela; o da direita espelhado. */
    private void drawLutador(RenderCanvas c, String lutador, float x, float altura, boolean espelha) {
        RenderImage im = imagem(Rostos.codigo(lutador) + ".png");
        float h = altura, w = im != null ? h * im.width() / im.height() : 360f;
        float base = VH + 6f, l = x - w * 0.5f, t = base - h;
        c.save();
        if (espelha) c.scale(-1f, 1f, x, 0f);
        if (im != null) {
            c.drawImage(im, l, t, l + w, base, 255);
        } else {
            c.fillOval(l + w * 0.15f, t + h * 0.1f, l + w * 0.85f, base + h * 0.3f, 0xFF000000);
        }
        c.restore();
    }

    /** Fumaça rolando embaixo, das duas cores. */
    private void drawFumaca(RenderCanvas c, float time) {
        for (int i = 0; i < 10; i++) {
            rand.reset(i * 3571L + 9L);
            float vel = rand.range(20f, 60f) * (i % 2 == 0 ? 1f : -1f);
            float x = (((rand.range(0f, VW + 400f) + time * vel) % (VW + 400f)) + VW + 400f) % (VW + 400f) - 200f;
            float y = VH - rand.range(10f, 90f);
            float r = rand.range(140f, 260f);
            boolean azul = x < VW * 0.5f ? esq.azul : dir.azul;
            c.fillRadialGlow(x, y, r, Tinta.withAlpha(azul ? 0xFFB8D4FF : 0xFFFFC4B8, 46));
        }
    }

    /** Brasas subindo pela tela. */
    private void drawFaiscas(RenderCanvas c, float time) {
        for (int i = 0; i < 40; i++) {
            rand.reset(i * 104729L + 5L);
            float x = rand.range(0f, VW);
            float vel = rand.range(60f, 180f);
            float y = VH + 20f - ((rand.range(0f, VH + 40f) + time * vel) % (VH + 40f));
            float r = rand.range(1.5f, 4f);
            boolean azul = x < VW * 0.5f ? esq.azul : dir.azul;
            c.fillCircle(x + (float)Math.sin(time * 2f + i) * 12f, y, r,
                Tinta.withAlpha(azul ? 0xFFBFE0FF : 0xFFFFC08A, (int)rand.range(90f, 220f)));
        }
    }

    /** O raio que parte a tela na diagonal, com a cor de cada lado na borda. */
    private void drawRaio(RenderCanvas c, float time) {
        float topo = VW * 0.5f + 60f, base = VW * 0.5f - 60f;
        float forte = 1f + 1.5f * Math.max(0f, 1f - Math.abs(time - VS_EM) / 0.3f);
        rand.reset((long)(time * 20f) * 131L);
        float px = topo, py = -20f;
        for (int s = 1; s <= 10; s++) {
            float k = s / 10f;
            float qx = topo + (base - topo) * k + (s < 10 ? rand.range(-16f, 16f) : 0f), qy = -20f + (VH + 40f) * k;
            c.drawLine(px - 3f, py, qx - 3f, qy, 6f * forte, Tinta.withAlpha(esq.azul ? AZUL_CLARO : VERMELHO_CLARO, 170));
            c.drawLine(px + 3f, py, qx + 3f, qy, 6f * forte, Tinta.withAlpha(dir.azul ? AZUL_CLARO : VERMELHO_CLARO, 170));
            c.drawLine(px, py, qx, qy, 2.5f * forte, WHITE);
            px = qx;
            py = qy;
        }
    }

    /** O logo em cima e a rodada embaixo dele. */
    private void drawLogo(RenderCanvas c, Temporada temporada, float time) {
        float in = Tinta.easeOutCubic((time - 0.75f) / 0.35f);
        if (in <= 0f) return;
        RenderImage logo = imagem("logo.png");
        float topoRotulo = 46f;
        if (logo != null) {
            float w = 250f, h = w * logo.height() / logo.width();
            c.drawImage(logo, VW * 0.5f - w * 0.5f, 6f, VW * 0.5f + w * 0.5f, 6f + h, (int)(255 * in));
            topoRotulo = 6f + h + 8f;
        }
        if (rotulo.isEmpty()) return;
        String texto = temporada.t(rotulo);
        c.drawText(texto, VW * 0.5f, topoRotulo + 26f, BotaoMenu.fit(c, texto, 28f, 420f, true),
            Tinta.withAlpha(WHITE, (int)(255 * in)), INK, 6f, true);
    }

    /**
     * A placa do lado, como nas telas de versus: a caixinha P1 (ou CPU) na ponta de fora, o nome do
     * titular grande numa caixa inclinada e, embaixo, o parceiro com "TAG".
     */
    private void drawPlaca(RenderCanvas c, Temporada temporada, Lado lado, float in, boolean direita, String jogador) {
        if (in <= 0f || lado.lutadores.length == 0) return;
        int cor = lado.azul ? AZUL : VERMELHO, clara = lado.azul ? AZUL_CLARO : VERMELHO_CLARO;
        float w = 470f, y = VH - 112f;
        float sai = (1f - in) * (w + 80f);
        // Tudo desenhado como o lado esquerdo e espelhado para o direito (só as formas).
        float x0 = 24f - sai;
        float[] caixa = {x0 + 86f, y, x0 + w, y, x0 + w - 22f, y + 62f, x0 + 64f, y + 62f};
        float[] tag = {x0, y + 4f, x0 + 96f, y + 4f, x0 + 78f, y + 58f, x0 - 18f, y + 58f};
        float[] faixa = {x0 + 120f, y + 66f, x0 + w - 60f, y + 66f, x0 + w - 72f, y + 96f, x0 + 108f, y + 96f};
        c.save();
        if (direita) c.scale(-1f, 1f, VW * 0.5f, 0f);
        c.fillPolygon(caixa, Tinta.withAlpha(Tinta.darken(cor, 0.55f), 235));
        c.strokePolygon(caixa, 3f, clara);
        c.fillPolygon(tag, Tinta.withAlpha(Tinta.darken(cor, 0.2f), 245));
        c.strokePolygon(tag, 3f, WHITE);
        if (lado.lutadores.length > 1) {
            c.fillPolygon(faixa, Tinta.withAlpha(0xFF0A0A12, 230));
            c.strokePolygon(faixa, 2f, GOLD);
        }
        c.restore();
        // Os textos sem espelhar, no lugar certo de cada lado.
        float meioNome = espelha(x0 + 86f + (w - 86f) * 0.5f, direita);
        float meioTag = espelha(x0 + 39f, direita);
        String nome = Rostos.nome(temporada, lado.lutadores[0]);
        c.drawText(jogador, meioTag, y + 44f, BotaoMenu.fit(c, jogador, 34f, 74f, true), WHITE, INK, 5f, true);
        c.drawText(nome, meioNome, y + 48f, BotaoMenu.fit(c, nome, 46f, 330f, true), WHITE, INK, 6f, true);
        if (lado.lutadores.length > 1) {
            String parceiro = Rostos.nome(temporada, lado.lutadores[1]);
            float tagX = espelha(x0 + 150f, direita);
            c.drawText("TAG", tagX, y + 89f, 18f, GOLD, INK, 3f, true);
            float pw = c.measureText(parceiro, 24f, true);
            float px = direita ? tagX - 34f - pw * 0.5f : tagX + 34f + pw * 0.5f;
            c.drawText(parceiro, px, y + 90f, BotaoMenu.fit(c, parceiro, 24f, 240f, true), WHITE, INK, 4f, true);
        }
    }

    private static float espelha(float x, boolean direita) {
        return direita ? VW - x : x;
    }

    /** O "VS" de prata que bate no meio, com raios em volta. */
    private void drawVs(RenderCanvas c, float time) {
        float u = time - VS_EM;
        if (u < 0f) return;
        float pop = Tinta.clamp01(u / 0.22f);
        float escala = (2.6f - 1.6f * Tinta.easeOutBack(pop)) * (1f + 0.025f * (float)Math.sin(time * 9f));
        float cx = VW * 0.5f, cy = VH * 0.5f + 40f;
        rand.reset((long)(time * 18f) * 37L);
        for (int i = 0; i < 8; i++) {
            float a = rand.range(0f, (float)(Math.PI * 2));
            float r0 = 110f, r1 = rand.range(190f, 290f);
            float px = cx + (float)Math.cos(a) * r0, py = cy + (float)Math.sin(a) * r0 * 0.8f;
            for (int s = 1; s <= 4; s++) {
                float k = r0 + (r1 - r0) * s / 4f;
                float qx = cx + (float)Math.cos(a) * k + rand.range(-18f, 18f), qy = cy + (float)Math.sin(a) * k * 0.8f + rand.range(-18f, 18f);
                c.drawLine(px, py, qx, qy, 3f, Tinta.withAlpha(0xFFE8F4FF, 190));
                px = qx;
                py = qy;
            }
        }
        RenderImage vs = imagem("vs.png");
        c.save();
        c.scale(escala, escala, cx, cy);
        c.fillRadialGlow(cx, cy, 200f, 0x66FFFFFF);
        if (vs != null) {
            float w = 330f, h = w * vs.height() / vs.width();
            c.drawImage(vs, cx - w * 0.5f, cy - h * 0.5f, cx + w * 0.5f, cy + h * 0.5f, 255);
        } else {
            c.drawText("VS", cx, cy + 60f, 190f, 0xFFE6E9F0, INK, 14f, true);
        }
        c.restore();
    }
}
