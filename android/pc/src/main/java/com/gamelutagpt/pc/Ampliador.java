package com.gamelutagpt.pc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.stream.IntStream;

/**
 * Amplia o quadro do jogo para o tamanho da tela (bilinear, inteiro, em paralelo), num buffer
 * reaproveitado. O Java2D amplia a imagem inteira com filtro por um caminho genérico de uma
 * thread só: 1280×720 → 1920×1080 custava ~18 ms por quadro, mais que desenhar a luta.
 */
final class Ampliador {
    private BufferedImage destino;
    private int origemLargura, origemAltura;
    /** Colunas: os dois vizinhos na origem e o peso (0..256) do segundo. */
    private int[] xa, xb, xf;
    /** Linhas: o mesmo, já multiplicado pela largura da origem. */
    private int[] ya, yb, yf;

    /** {@code origem} (TYPE_INT_RGB) ampliada para {@code largura}×{@code altura}. */
    BufferedImage ampliar(BufferedImage origem, int largura, int altura) {
        int sw = origem.getWidth(), sh = origem.getHeight();
        if (destino == null || destino.getWidth() != largura || destino.getHeight() != altura
            || sw != origemLargura || sh != origemAltura) {
            destino = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
            origemLargura = sw;
            origemAltura = sh;
            xa = new int[largura]; xb = new int[largura]; xf = new int[largura];
            tabela(sw, largura, 1, xa, xb, xf);
            ya = new int[altura]; yb = new int[altura]; yf = new int[altura];
            tabela(sh, altura, sw, ya, yb, yf);
        }
        final int[] src = ((DataBufferInt) origem.getRaster().getDataBuffer()).getData();
        final int[] dst = ((DataBufferInt) destino.getRaster().getDataBuffer()).getData();
        final int w = largura;
        final int[] xa = this.xa, xb = this.xb, xf = this.xf, ya = this.ya, yb = this.yb, yf = this.yf;
        IntStream.range(0, altura).parallel().forEach(y -> {
            int ra = ya[y], rb = yb[y], fy = yf[y], iy = 256 - fy, o = y * w;
            for (int x = 0; x < w; x++) {
                int fx = xf[x], ix = 256 - fx;
                int p00 = src[ra + xa[x]], p10 = src[ra + xb[x]];
                int p01 = src[rb + xa[x]], p11 = src[rb + xb[x]];
                // Dois canais por vez (00RR00BB e 000000GG), pesos de 8 bits.
                int top02 = (((p00 & 0xFF00FF) * ix + (p10 & 0xFF00FF) * fx) >>> 8) & 0xFF00FF;
                int top1 = (((p00 & 0xFF00) * ix + (p10 & 0xFF00) * fx) >>> 8) & 0xFF00;
                int bot02 = (((p01 & 0xFF00FF) * ix + (p11 & 0xFF00FF) * fx) >>> 8) & 0xFF00FF;
                int bot1 = (((p01 & 0xFF00) * ix + (p11 & 0xFF00) * fx) >>> 8) & 0xFF00;
                dst[o + x] = (((top02 * iy + bot02 * fy) >>> 8) & 0xFF00FF) | (((top1 * iy + bot1 * fy) >>> 8) & 0xFF00);
            }
        });
        return destino;
    }

    private static void tabela(int origem, int destino, int passo, int[] a, int[] b, int[] f) {
        for (int i = 0; i < destino; i++) {
            double u = (i + 0.5) * origem / destino - 0.5;
            int u0 = (int) Math.floor(u);
            f[i] = (int) Math.round((u - u0) * 256);
            a[i] = Math.max(0, Math.min(origem - 1, u0)) * passo;
            b[i] = Math.max(0, Math.min(origem - 1, u0 + 1)) * passo;
        }
    }
}
