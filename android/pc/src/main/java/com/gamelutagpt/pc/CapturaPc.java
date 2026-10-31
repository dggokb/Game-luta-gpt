package com.gamelutagpt.pc;

import android.content.Context;
import android.view.KeyEvent;
import com.gamelutagpt.GameView;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Joga sozinho, sem janela, e salva quadros em PNG: seleção, entrada na luta, golpes
 * pelo teclado e o Super. Serve para conferir o jogo de PC na CI (onde não há tela) e
 * para medir o tempo de cada quadro.
 *
 * <pre>gradle :pc:capturar   →   pc/build/captura/*.png + tempos.txt</pre>
 */
public final class CapturaPc {
    private final GameView jogo;
    private final QuadroPc quadro;
    private final Object trava = new Object();
    private BufferedImage ultimo;
    private boolean pedido;
    /** Tempo de desenho de cada quadro depois do carregamento (ns). */
    private final List<Long> desenhos = new ArrayList<>();
    private volatile boolean medindo;

    public CapturaPc(Context contexto, int largura, int altura) {
        this(contexto, largura, altura, false);
    }

    /** {@code celular}: a GameView como no celular (menu na hora, arte em segundo plano). */
    public CapturaPc(Context contexto, int largura, int altura, boolean celular) {
        jogo = new GameView(contexto, celular);
        quadro = new QuadroPc(largura, altura, this::recebido);
    }

    /** Chamado na thread do jogo: copia aqui, antes de o próximo quadro reescrever a imagem. */
    private void recebido(BufferedImage imagem) {
        synchronized (trava) {
            if (pedido) {
                BufferedImage copia = new BufferedImage(imagem.getWidth(), imagem.getHeight(), BufferedImage.TYPE_INT_RGB);
                copia.getGraphics().drawImage(imagem, 0, 0, null);
                ultimo = copia;
                pedido = false;
                trava.notifyAll();
            }
        }
        if (medindo) synchronized (trava) { desenhos.add(quadro.ultimoDesenhoNs()); }
    }

    public GameView jogo() { return jogo; }

    public void comecar() {
        jogo.setTouchControlsVisible(Boolean.getBoolean("gameluta.controleNaTela"));
        // -Dgameluta.opengl=true: a luta pela placa de vídeo também sem janela (precisa de uma tela,
        // mesmo virtual, como o Xvfb).
        if (Boolean.getBoolean("gameluta.opengl")) jogo.ligarOpenGl(null);
        jogo.attach(quadro, quadro.largura(), quadro.altura());
    }

    public void parar() {
        jogo.pauseGame();
        jogo.detach();
        jogo.release();
    }

    /** Espera {@code segundos} de jogo (o loop roda na própria thread, a 60 Hz). */
    public void esperar(double segundos) throws InterruptedException {
        Thread.sleep(Math.round(segundos * 1000));
    }

    /** Cópia do próximo quadro desenhado. */
    public BufferedImage quadro() throws InterruptedException {
        synchronized (trava) {
            pedido = true;
            long limite = System.currentTimeMillis() + 10_000;
            while (pedido && System.currentTimeMillis() < limite) trava.wait(100);
            if (pedido) throw new IllegalStateException("o jogo parou de desenhar");
            return ultimo;
        }
    }

    public void tecla(int codigo) throws InterruptedException {
        jogo.onKeyDown(codigo, new KeyEvent(KeyEvent.ACTION_DOWN, codigo));
        Thread.sleep(50);
        jogo.onKeyUp(codigo, new KeyEvent(KeyEvent.ACTION_UP, codigo));
        Thread.sleep(50);
    }

    public void segurar(int codigo) { jogo.onKeyDown(codigo, new KeyEvent(KeyEvent.ACTION_DOWN, codigo)); }

    public void soltar(int codigo) { jogo.onKeyUp(codigo, new KeyEvent(KeyEvent.ACTION_UP, codigo)); }

    public long quadrosDesenhados() { return quadro.quadros(); }

    /** Liga/desliga a medição do tempo de desenho (fora dos carregamentos). */
    public void medir(boolean ligado) { medindo = ligado; }

    /** Percentil (0..100) do tempo de desenho, em ms. */
    public synchronized double percentilMs(double p) {
        List<Long> copia;
        synchronized (trava) {
            copia = new ArrayList<>(desenhos);
        }
        if (copia.isEmpty()) return 0;
        java.util.Collections.sort(copia);
        int i = (int) Math.min(copia.size() - 1, Math.round(p / 100.0 * (copia.size() - 1)));
        return copia.get(i) / 1e6;
    }

    public void zerarMedidas() {
        synchronized (trava) {
            desenhos.clear();
        }
    }

    public int quadrosMedidos() {
        synchronized (trava) {
            return desenhos.size();
        }
    }

    public static void main(String[] args) throws Exception {
        File saida = new File(args.length > 0 ? args[0] : "build/captura");
        if (!saida.isDirectory() && !saida.mkdirs()) throw new IOException("não criou " + saida);
        int largura = 1280, altura = 720;
        if (args.length > 1) {
            String[] r = args[1].toLowerCase(Locale.ROOT).split("x");
            largura = Integer.parseInt(r[0]);
            altura = Integer.parseInt(r[1]);
        }
        DadosDoJogo dados = DadosDoJogo.localizar();
        CapturaPc c = new CapturaPc(dados.contexto(null), largura, altura);
        List<String> linhas = new ArrayList<>();
        c.comecar();
        c.esperar(1.5);
        salvar(c.quadro(), saida, "01-menu");
        // MODO TESTE (o último item: cima dá a volta até ele) abre a seleção de duplas.
        c.tecla(KeyEvent.KEYCODE_W);
        c.tecla(KeyEvent.KEYCODE_J);
        c.esperar(1.5);
        salvar(c.quadro(), saida, "02-selecao");
        // A seleção parada por 3 s: quadros por segundo e tempo de desenho (-Dgameluta.esperaSelecao:
        // segundos antes, para a carga dos lutadores em segundo plano acabar).
        c.esperar(Double.parseDouble(System.getProperty("gameluta.esperaSelecao", "0")));
        long antes = c.quadrosDesenhados();
        c.medir(true);
        c.esperar(3.0);
        c.medir(false);
        linhas.add(String.format(Locale.ROOT, "selecao: %.1f quadros/s desenho mediana=%.2fms p95=%.2fms max=%.2fms",
            (c.quadrosDesenhados() - antes) / 3.0, c.percentilMs(50), c.percentilMs(95), c.percentilMs(100)));
        c.zerarMedidas();
        // P1, TAG, P2, TAG e confirmar (J = A do controle na seleção).
        for (int i = 0; i < 4; i++) {
            c.tecla(KeyEvent.KEYCODE_J);
            c.esperar(0.3);
        }
        salvar(c.quadro(), saida, "03-selecao-pronta");
        c.tecla(KeyEvent.KEYCODE_ENTER);
        c.esperar(0.4);
        salvar(c.quadro(), saida, "04-entrada");
        c.esperar(5.0);
        c.medir(true);
        // -Dgameluta.luta=<segundos>: mais tempo de luta parada para medir/perfilar.
        c.esperar(Double.parseDouble(System.getProperty("gameluta.luta", "0")));
        salvar(c.quadro(), saida, "05-luta");
        // Anda para frente e solta L, M, H.
        c.segurar(KeyEvent.KEYCODE_D);
        c.esperar(0.6);
        c.soltar(KeyEvent.KEYCODE_D);
        for (int tecla : new int[] {KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_L}) {
            c.tecla(tecla);
            c.esperar(0.12);
        }
        salvar(c.quadro(), saida, "06-golpes");
        c.esperar(1.0);
        c.tecla(KeyEvent.KEYCODE_I);
        c.esperar(0.5);
        salvar(c.quadro(), saida, "07-super");
        c.esperar(2.0);
        c.tecla(KeyEvent.KEYCODE_F1);
        c.esperar(0.5);
        salvar(c.quadro(), saida, "08-debug");
        c.tecla(KeyEvent.KEYCODE_F1);
        // Esc: pausa.
        c.tecla(KeyEvent.KEYCODE_ESCAPE);
        c.esperar(0.6);
        salvar(c.quadro(), saida, "09-pausa");
        c.medir(false);
        c.parar();
        linhas.add(String.format(Locale.ROOT, "resolucao=%dx%d quadros_medidos=%d desenho_mediana=%.2fms p95=%.2fms max=%.2fms",
            largura, altura, c.quadrosMedidos(), c.percentilMs(50), c.percentilMs(95), c.percentilMs(100)));
        java.nio.file.Files.write(new File(saida, "tempos.txt").toPath(), linhas);
        for (String l : linhas) System.out.println(l);
        System.exit(0);
    }

    static void salvar(BufferedImage img, File dir, String nome) throws IOException {
        ImageIO.write(img, "png", new File(dir, nome + ".png"));
        System.out.println("salvo " + nome + ".png");
    }
}
