package com.gamelutagpt.historia;

import com.gamelutagpt.render.Rand;
import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A tela de versus antes de cada luta do torneio (e do duelo da regra escondida): a tela se
 * parte na diagonal, azul do lado do jogador e vermelha do lado do adversário, as duplas entram
 * deslizando das bordas (o titular na frente, o parceiro atrás), o "VS" bate no meio com clarão e
 * tremor, os nomes entram em pinceladas e, no fim, um clarão branco leva para a luta.
 *
 * As poses ficam em {@code historia/torneio/versus/pNN.png} (de
 * {@code tools/historia/versus_poses.py}): PNG transparente, virado para a direita, pés na base,
 * 2 pixels por pixel do mundo. Para trocar por uma ilustração, basta gravar no mesmo nome.
 */
final class TelaVersus {
    static final String PASTA = Temporada.PASTA + "/torneio/versus/";

    static final float DURACAO = 4.4f;
    /** O "VS" bate no meio. */
    static final float VS_EM = 0.62f;
    /** Antes disso o toque não pula (a entrada não some num toque sem querer). */
    static final float MIN_TOQUE = 0.9f;
    /** O clarão final, que leva para a luta. */
    static final float SAIDA = 0.35f;

    private static final float VW = PaginaHistoria.VW, VH = PaginaHistoria.VH;
    /** Pixels da tela por pixel da pose: o titular, o parceiro (atrás e menor). */
    private static final float ESCALA_TITULAR = 1.5f, ESCALA_PARCEIRO = 1.16f;
    private static final int INK = Baloes.INK;
    private static final int WHITE = Baloes.WHITE;
    private static final int GOLD = 0xFFF2C14E;
    private static final int AZUL = 0xFF2F6FD6;
    private static final int VERMELHO = 0xFFC62F2F;

    private final RenderAssets assets;
    private final Map<String, RenderImage> poses = new HashMap<>();
    private final Rand rand = new Rand(23);
    private String[] jogador = new String[0], adversario = new String[0];
    private String rotulo = "";

    TelaVersus(RenderAssets assets) {
        this.assets = assets;
    }

    /** Começa a tela para as duplas {jogador, adversário} (lutadores) e o nome da rodada. */
    void begin(String[][] lados, String rotulo) {
        jogador = lados[0].clone();
        adversario = lados[1].clone();
        this.rotulo = rotulo == null ? "" : rotulo;
    }

    void liberar() {
        poses.clear();
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

    private RenderImage pose(String lutador) {
        String codigo = Rostos.codigo(lutador);
        if (poses.containsKey(codigo)) return poses.get(codigo);
        RenderImage image = null;
        try {
            image = assets.loadImage(PASTA + codigo + ".png");
        } catch (IOException | RuntimeException ex) {
            // Sem a pose: só o nome e as cores.
        }
        poses.put(codigo, image);
        return image;
    }

    // ------------------------------------------------------------ desenho

    void draw(RenderCanvas c, Temporada temporada, float time) {
        float empurra = 1f + 0.035f * Tinta.clamp01(time / DURACAO);
        c.save();
        c.scale(empurra, empurra, VW * 0.5f, VH * 0.55f);
        drawFundo(c, time);
        // Os parceiros atrás, os titulares na frente; o adversário espelhado (olhando para o meio).
        float entra = Tinta.clamp01((time - 0.08f) / 0.42f);
        float entraParceiro = Tinta.clamp01((time - 0.18f) / 0.42f);
        float dx = (1f - Tinta.easeOutBack(entra)) * 620f, dxp = (1f - Tinta.easeOutBack(entraParceiro)) * 620f;
        if (jogador.length > 1) drawLutador(c, jogador[1], 190f - dxp, VH - 10f, ESCALA_PARCEIRO, false, 170);
        if (adversario.length > 1) drawLutador(c, adversario[1], VW - 190f + dxp, VH - 10f, ESCALA_PARCEIRO, true, 170);
        if (jogador.length > 0) drawLutador(c, jogador[0], 380f - dx, VH + 70f, ESCALA_TITULAR, false, 255);
        if (adversario.length > 0) drawLutador(c, adversario[0], VW - 380f + dx, VH + 70f, ESCALA_TITULAR, true, 255);
        drawFaiscas(c, time);
        c.restore();
        drawCinema(c);

        drawRotulo(c, temporada, time);
        drawNomes(c, temporada, time);
        drawVs(c, time);

        // Clarões: o da abertura, o do VS e o da saída para a luta.
        int flash = (int)(255 * (1f - Tinta.clamp01(time / 0.22f)));
        float pos = time - VS_EM;
        if (pos >= 0f) flash = Math.max(flash, (int)(210 * (1f - Tinta.clamp01(pos / 0.3f))));
        float saida = (time - (DURACAO - SAIDA)) / SAIDA;
        if (saida > 0f) flash = Math.max(flash, (int)(255 * Tinta.clamp01(saida)));
        if (flash > 0) c.fillRect(-40f, -40f, VW + 40f, VH + 40f, Tinta.withAlpha(WHITE, flash));
    }

    /** A tela partida na diagonal, com linhas de velocidade correndo para o meio. */
    private void drawFundo(RenderCanvas c, float time) {
        c.fillRect(-60f, -60f, VW + 60f, VH + 60f, 0xFF07080D);
        float abre = Tinta.easeOutCubic(time / 0.3f);
        float topo = VW * 0.5f + 70f, base = VW * 0.5f - 70f;
        float[] esq = {-60f, -60f, topo, -60f, base, VH + 60f, -60f, VH + 60f};
        float[] dir = {topo, -60f, VW + 60f, -60f, VW + 60f, VH + 60f, base, VH + 60f};
        c.save();
        c.translate(-(1f - abre) * 700f, 0f);
        c.clipPolygon(esq);
        c.fillVerticalGradient(-60f, -60f, VW + 60f, VH + 60f, 0xFF153B86, 0xFF060B1C);
        c.fillRadialGlow(330f, 420f, 520f, 0x664FA3FF);
        linhasDeVelocidade(c, time, 1f, 0xFF8FC4FF);
        c.restore();
        c.save();
        c.translate((1f - abre) * 700f, 0f);
        c.clipPolygon(dir);
        c.fillVerticalGradient(-60f, -60f, VW + 60f, VH + 60f, 0xFF861515, 0xFF1C0606);
        c.fillRadialGlow(VW - 330f, 420f, 520f, 0x66FF5A4F);
        linhasDeVelocidade(c, time, -1f, 0xFFFFA08F);
        c.restore();
        // A divisa: uma lâmina de luz que treme.
        rand.reset((long)(time * 24f) * 131L);
        float w = 9f + rand.range(0f, 6f);
        c.drawLine(topo, -60f, base, VH + 60f, w + 14f, Tinta.withAlpha(WHITE, 70));
        c.drawLine(topo, -60f, base, VH + 60f, w, WHITE);
    }

    private void linhasDeVelocidade(RenderCanvas c, float time, float sentido, int cor) {
        for (int i = 0; i < 26; i++) {
            rand.reset(i * 7919L + (sentido > 0 ? 1 : 2));
            float y = rand.range(-20f, VH + 20f);
            float len = rand.range(160f, 520f);
            float vel = rand.range(900f, 1700f);
            float x = ((rand.range(0f, 2000f) + time * vel) % 2000f) - 400f;
            if (sentido < 0) x = VW - x;
            float largura = rand.range(1.5f, 5f);
            c.drawLine(x, y, x - sentido * len, y, largura, Tinta.withAlpha(cor, (int)rand.range(40f, 110f)));
        }
    }

    /** A pose com os pés em (x, base), espelhada para o adversário; sem a pose, uma silhueta. */
    private void drawLutador(RenderCanvas c, String lutador, float x, float base, float escala, boolean espelha, int alpha) {
        RenderImage im = pose(lutador);
        float w = im != null ? im.width() * escala : 300f, h = im != null ? im.height() * escala : 520f;
        float l = x - w * 0.5f, t = base - h;
        c.save();
        if (espelha) c.scale(-1f, 1f, x, 0f);
        // Sombra no chão e um brilho atrás (o lutador se destaca do fundo).
        c.fillRadialGlow(x, base - h * 0.45f, Math.max(w, h) * 0.55f, Tinta.withAlpha(espelha ? 0xFFFF6A5A : 0xFF6AA8FF, alpha / 4));
        if (im != null) {
            c.drawImage(im, l, t, l + w, t + h, alpha);
        } else {
            c.fillOval(l + w * 0.2f, t, l + w * 0.8f, base, Tinta.withAlpha(0xFF000000, alpha));
        }
        c.restore();
    }

    /** Faixas pretas de cinema em cima e embaixo e as bordas escurecidas. */
    private void drawCinema(RenderCanvas c) {
        c.fillRadialGlow(0f, VH * 0.5f, 420f, 0x99000000);
        c.fillRadialGlow(VW, VH * 0.5f, 420f, 0x99000000);
        c.fillVerticalGradient(-40f, VH - 190f, VW + 40f, VH + 40f, 0x00000000, 0xCC000000);
        c.fillRect(-40f, -40f, VW + 40f, 34f, 0xFF000000);
        c.fillRect(-40f, VH - 34f, VW + 40f, VH + 40f, 0xFF000000);
    }

    /** Brasas subindo pela tela. */
    private void drawFaiscas(RenderCanvas c, float time) {
        for (int i = 0; i < 40; i++) {
            rand.reset(i * 104729L + 5L);
            float x = rand.range(0f, VW);
            float vel = rand.range(60f, 180f);
            float y = VH + 20f - ((rand.range(0f, VH + 40f) + time * vel) % (VH + 40f));
            float r = rand.range(1.5f, 4f);
            int cor = x < VW * 0.5f ? 0xFFBFE0FF : 0xFFFFD08A;
            c.fillCircle(x + (float)Math.sin(time * 2f + i) * 12f, y, r, Tinta.withAlpha(cor, (int)rand.range(90f, 220f)));
        }
    }

    /** O nome da rodada numa pincelada vermelha em cima. */
    private void drawRotulo(RenderCanvas c, Temporada temporada, float time) {
        if (rotulo.isEmpty()) return;
        float in = Tinta.easeOutCubic((time - 0.75f) / 0.3f);
        if (in <= 0f) return;
        float[] faixa = {VW * 0.5f - 330f * in, 26f, VW * 0.5f + 330f * in, 16f, VW * 0.5f + 330f * in, 78f, VW * 0.5f - 330f * in, 86f};
        c.fillPolygon(faixa, 0xFFC8202C);
        String texto = temporada.t(rotulo);
        c.drawText(texto, VW * 0.5f, 66f, BotaoMenu.fit(c, texto, 38f, 620f, true),
            Tinta.withAlpha(WHITE, (int)(255 * in)), INK, 6f, true);
    }

    /** Os nomes das duplas em pinceladas embaixo, cada um do seu lado. */
    private void drawNomes(RenderCanvas c, Temporada temporada, float time) {
        drawNome(c, nomes(temporada, jogador), Tinta.easeOutCubic((time - 0.85f) / 0.3f), false);
        drawNome(c, nomes(temporada, adversario), Tinta.easeOutCubic((time - 0.95f) / 0.3f), true);
    }

    private static String nomes(Temporada temporada, String[] lutadores) {
        StringBuilder sb = new StringBuilder();
        for (String l : lutadores) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(Rostos.nome(temporada, l));
        }
        return sb.toString();
    }

    private void drawNome(RenderCanvas c, String nome, float in, boolean direita) {
        if (in <= 0f || nome.isEmpty()) return;
        float tam = BotaoMenu.fit(c, nome, 40f, 470f, true);
        float tw = c.measureText(nome, tam, true);
        float w = tw + 130f;
        float y = VH - 92f;
        // A faixa entra pela borda do lado dela; o texto fica a 50 px da borda.
        float sai = (1f - in) * (w + 60f);
        float x0 = direita ? VW - w + 40f + sai : -40f - sai;
        float[] faixa = {x0, y - 30f, x0 + w, y - 40f, x0 + w - 14f, y + 26f, x0 + 10f, y + 34f};
        c.fillPolygon(new float[] {faixa[0] + 6f, faixa[1] + 8f, faixa[2] + 6f, faixa[3] + 8f, faixa[4] + 6f, faixa[5] + 8f, faixa[6] + 6f, faixa[7] + 8f},
            0xAA000000);
        c.fillPolygon(faixa, direita ? VERMELHO : AZUL);
        float cx = direita ? x0 + w - 40f - 50f - tw * 0.5f : x0 + 40f + 50f + tw * 0.5f;
        c.drawText(nome, cx, y + 13f, tam, WHITE, INK, 6f, true);
    }

    /** O "VS" que bate no meio, com raios em volta. */
    private void drawVs(RenderCanvas c, float time) {
        float u = time - VS_EM;
        if (u < 0f) return;
        float pop = Tinta.clamp01(u / 0.22f);
        float escala = 2.6f - 1.6f * Tinta.easeOutBack(pop);
        float pulsa = 1f + 0.03f * (float)Math.sin(time * 9f);
        float cx = VW * 0.5f, cy = VH * 0.5f + 20f;
        // Raios ao redor, tremeluzindo.
        rand.reset((long)(time * 18f) * 37L);
        for (int i = 0; i < 9; i++) {
            float a = rand.range(0f, (float)(Math.PI * 2));
            float r0 = 90f, r1 = rand.range(170f, 280f);
            float px = cx + (float)Math.cos(a) * r0, py = cy + (float)Math.sin(a) * r0 * 0.8f;
            for (int s = 1; s <= 4; s++) {
                float k = r0 + (r1 - r0) * s / 4f;
                float qx = cx + (float)Math.cos(a) * k + rand.range(-18f, 18f), qy = cy + (float)Math.sin(a) * k * 0.8f + rand.range(-18f, 18f);
                c.drawLine(px, py, qx, qy, 3.5f, Tinta.withAlpha(0xFFFFF2B0, 200));
                px = qx;
                py = qy;
            }
        }
        c.save();
        c.scale(escala * pulsa, escala * pulsa, cx, cy);
        c.fillRadialGlow(cx, cy - 40f, 190f, 0x88FFC94A);
        c.drawText("VS", cx + 8f, cy + 58f, 190f, 0xFF000000, 0xFF000000, 22f, true);
        c.drawText("VS", cx, cy + 50f, 190f, GOLD, INK, 14f, true);
        c.restore();
    }
}
