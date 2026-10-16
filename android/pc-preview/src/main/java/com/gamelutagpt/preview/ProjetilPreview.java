package com.gamelutagpt.preview;

import com.gamelutagpt.efeitos.ProjetilVisual;
import com.gamelutagpt.render.RenderCanvas;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Grava em PNG o S1 de cada personagem voando ({@link ProjetilVisual}), para revisar o desenho
 * antes de ir para o jogo: à esquerda um close 3x, à direita o voo no tamanho do jogo ao lado da
 * silhueta do lutador. Também um resumo com todos.
 *
 * <pre>gradle :pc-preview:runProjeteis</pre>
 * ou, com argumento: {@code <pasta de saída>}.
 */
public final class ProjetilPreview {
    private static final int W = 1280, H = 420;
    private static final float GROUND = 370f;

    /** Personagem, nome, tipo, altura de saída (acima do chão), alcance, cor, altura do lutador. */
    private static final Object[][] LISTA = {
        {"p01", "Onda", ProjetilVisual.Tipo.ONDA, 168f, 720f, 0xF4B73B, 226f},
        {"p02", "Palma", ProjetilVisual.Tipo.PALMA, 150f, 260f, 0x3FA7D6, 211f},
        {"p03", "Onda curta", ProjetilVisual.Tipo.ONDA_CURTA, 150f, 380f, 0xD8323C, 236f},
        {"p04", "Soco de impacto", ProjetilVisual.Tipo.ESTOURO, 150f, 200f, 0xE0A526, 246f},
        {"p05", "Kunai", ProjetilVisual.Tipo.KUNAI, 120f, 900f, 0x8A4FD8, 190f},
        {"p05", "Shuriken (S2)", ProjetilVisual.Tipo.SHURIKEN, 120f, 900f, 0x8A4FD8, 190f},
        {"p06", "Sombra rastejante", ProjetilVisual.Tipo.SOMBRA, 0f, 800f, 0x8A4FD8, 203f},
        {"p08", "Chicote", ProjetilVisual.Tipo.CHICOTE, 160f, 650f, 0xE0405A, 246f},
        {"p09", "Machado", ProjetilVisual.Tipo.MACHADO, 176f, 900f, 0xE0A526, 242f},
        {"p10", "Machadinho", ProjetilVisual.Tipo.MACHADINHO, 150f, 720f, 0x40B070, 229f},
        {"p11", "Bisturi", ProjetilVisual.Tipo.BISTURI, 170f, 900f, 0x60C0D0, 245f},
        {"p11", "Seringa (S2)", ProjetilVisual.Tipo.SERINGA, 160f, 400f, 0x60C0D0, 245f},
    };

    public static void main(String[] args) throws IOException {
        File out = new File(args.length > 0 ? args[0] : "build/projeteis");
        if (!out.isDirectory() && !out.mkdirs()) throw new IOException("não consegui criar " + out);
        BufferedImage resumo = new BufferedImage(1280, 240 * 3, BufferedImage.TYPE_INT_RGB);
        Graphics2D gr = resumo.createGraphics();
        Java2DRenderCanvas rc = new Java2DRenderCanvas(gr, 1280, 720);
        rc.fillVerticalGradient(0f, 0f, 1280f, 720f, 0xFF1B2030, 0xFF0B0D14);
        for (int i = 0; i < LISTA.length; i++) {
            Object[] item = LISTA[i];
            String id = (String)item[0], nome = (String)item[1];
            ProjetilVisual.Tipo tipo = (ProjetilVisual.Tipo)item[2];
            float altura = (Float)item[3], alcance = (Float)item[4], lutador = (Float)item[6];
            int cor = 0xFF000000 | (Integer)item[5];
            BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            desenhar(new Java2DRenderCanvas(g, W, H), id, nome, tipo, altura, alcance, cor, lutador);
            g.dispose();
            String arquivo = id + "_" + tipo.name().toLowerCase() + ".png";
            ImageIO.write(img, "png", new File(out, arquivo));
            // Resumo: o close de cada um numa grade de 4 por 3.
            int col = i % 4, row = i / 4;
            float cx = col * 320f + 160f, cy = row * 240f + 130f;
            rc.save();
            float l = col * 320f + 4f, tp = row * 240f + 4f;
            rc.clipPolygon(new float[] {l, tp, l + 312f, tp, l + 312f, tp + 196f, l, tp + 196f});
            rc.translate(cx + (tipo == ProjetilVisual.Tipo.SOMBRA || tipo == ProjetilVisual.Tipo.BISTURI ? 60f : 0f), cy);
            rc.scale(2.4f, 2.4f, 0f, 0f);
            float originX = tipo == ProjetilVisual.Tipo.CHICOTE ? -60f : -alcance * 0.3f;
            float y = tipo == ProjetilVisual.Tipo.SOMBRA ? 12f : 0f;
            ProjetilVisual.draw(rc, tipo, 0f, y, 1, originX, alcance, 0.35f, cor);
            rc.restore();
            rc.drawText(id + " · " + nome, cx, row * 240f + 226f, 20f, 0xFFFFFFFF, 0xFF000000, 2f, false);
        }
        gr.dispose();
        ImageIO.write(resumo, "png", new File(out, "resumo.png"));
        efeitos(out);
        System.out.println("Projéteis gravados em " + out.getAbsolutePath());
    }

    /** Poça de veneno da seringa (p11) e jato de vapor da p10, em três momentos cada. */
    private static void efeitos(File out) throws IOException {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        Java2DRenderCanvas c = new Java2DRenderCanvas(g, W, H);
        c.fillVerticalGradient(0f, 0f, W, H, 0xFF1B2030, 0xFF0B0D14);
        c.fillRect(0f, GROUND, W, H, 0xFF2A2420);
        c.drawLine(0f, GROUND, W, GROUND, 2f, 0xFF5A4A3A);
        c.drawText("p11 · poça de veneno (recém-caída, metade, sumindo)", 320f, 34f, 20f, 0xFFFFFFFF, 0xFF000000, 2f, false);
        float[] vidas = {1f, 0.5f, 0.12f};
        for (int i = 0; i < 3; i++) {
            ProjetilVisual.puddle(c, 120f + i * 200f, GROUND, 60f, vidas[i], 0.3f + i * 0.4f, 0xFF60C0D0);
        }
        c.drawText("p10 · jato de vapor", 960f, 34f, 20f, 0xFFFFFFFF, 0xFF000000, 2f, false);
        float[] momentos = {0.15f, 0.45f, 0.85f};
        for (int i = 0; i < 3; i++) {
            float x = 720f + i * 190f;
            c.fillRect(x - 60f, GROUND - 229f, x - 10f, GROUND, 0x60B9C0D0);
            ProjetilVisual.steam(c, x, GROUND - 160f, 1, momentos[i], 0.2f + i * 0.3f);
        }
        g.dispose();
        ImageIO.write(img, "png", new File(out, "efeitos_poca_vapor.png"));
    }

    private static void desenhar(RenderCanvas c, String id, String nome, ProjetilVisual.Tipo tipo, float altura,
                                 float alcance, int cor, float lutador) {
        c.fillVerticalGradient(0f, 0f, W, H, 0xFF1B2030, 0xFF0B0D14);
        c.fillRect(0f, GROUND, W, H, 0xFF2A2420);
        c.drawLine(0f, GROUND, W, GROUND, 2f, 0xFF5A4A3A);
        c.drawText(id + " · " + nome, 210f, 34f, 26f, 0xFFFFFFFF, 0xFF000000, 2f, true);
        c.drawText("close 3x", 210f, 64f, 16f, 0xFFB9C0D0, 0, 0f, false);
        c.drawText("no tamanho do jogo", 860f, 34f, 18f, 0xFFB9C0D0, 0, 0f, false);
        c.drawLine(420f, 0f, 420f, H, 2f, 0xFF3A4050);

        // Close 3x, no meio do voo.
        boolean chicote = tipo == ProjetilVisual.Tipo.CHICOTE, sombra = tipo == ProjetilVisual.Tipo.SOMBRA;
        c.save();
        c.translate(chicote ? 330f : 230f, sombra ? GROUND - 20f : 220f);
        c.scale(3f, 3f, 0f, 0f);
        float closeOrigin = chicote ? -80f : -alcance * 0.4f;
        ProjetilVisual.draw(c, tipo, 0f, sombra ? 6f : 0f, 1, closeOrigin, alcance, 0.4f, cor);
        c.restore();

        // No tamanho do jogo: o lutador (silhueta) e o projétil em quatro momentos do voo.
        float hand = 500f, ground = GROUND;
        c.fillRect(hand - 70f, ground - lutador, hand - 10f, ground, 0x60B9C0D0);
        c.drawText("lutador", hand - 40f, ground + 26f, 14f, 0xFFB9C0D0, 0, 0f, false);
        float y = sombra ? ground : ground - altura;
        float maxX = W - 40f;
        if (chicote) {
            float[] momentos = {0.35f, 1f};
            for (int i = 0; i < momentos.length; i++) {
                float x = hand + Math.min(alcance * momentos[i], maxX - hand);
                ProjetilVisual.draw(c, tipo, x, y - i * 70f, 1, hand, alcance, 0.2f * i, cor);
            }
            return;
        }
        float[] momentos = {0.12f, 0.35f, 0.62f, 0.92f};
        for (int i = 0; i < momentos.length; i++) {
            float x = hand + 30f + (maxX - hand - 30f) * momentos[i] * Math.min(1f, alcance / 760f);
            ProjetilVisual.draw(c, tipo, x, y, 1, hand, alcance, 0.08f + 0.15f * i, cor);
        }
    }
}
