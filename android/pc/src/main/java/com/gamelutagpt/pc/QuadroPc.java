package com.gamelutagpt.pc;

import android.graphics.Canvas;
import android.view.SurfaceView;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Destino dos quadros do jogo no PC: uma imagem fora da tela onde o {@code GameView}
 * desenha pelo Canvas, entregue pronta a quem mostra (a janela, ou um teste que salva PNG).
 * A janela troca o tamanho com {@link #setTamanho}; o próximo quadro já sai no novo.
 */
public final class QuadroPc implements SurfaceView.Target {
    private final Canvas canvas = new Canvas();
    private final Consumer<BufferedImage> apresentar;
    private volatile int largura, altura;
    private BufferedImage imagem;
    private Graphics2D g;
    private long quadros;
    private long inicio;
    private volatile long ultimoDesenhoNs;

    public QuadroPc(int largura, int altura, Consumer<BufferedImage> apresentar) {
        this.largura = largura;
        this.altura = altura;
        this.apresentar = apresentar;
    }

    public void setTamanho(int largura, int altura) {
        this.largura = Math.max(16, largura);
        this.altura = Math.max(9, altura);
    }

    public int largura() { return largura; }

    public int altura() { return altura; }

    public long quadros() { return quadros; }

    /** Quanto o último quadro levou para ser desenhado (sem a espera do loop de 60 Hz). */
    public long ultimoDesenhoNs() { return ultimoDesenhoNs; }

    @Override public Canvas beginFrame() {
        inicio = System.nanoTime();
        int w = largura, h = altura;
        if (imagem == null || imagem.getWidth() != w || imagem.getHeight() != h) {
            imagem = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        }
        g = imagem.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);
        canvas.begin(g, imagem, w, h);
        return canvas;
    }

    @Override public void endFrame(Canvas c) {
        canvas.end().dispose();
        g = null;
        quadros++;
        ultimoDesenhoNs = System.nanoTime() - inicio;
        apresentar.accept(imagem);
    }
}
