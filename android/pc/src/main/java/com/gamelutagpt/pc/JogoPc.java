package com.gamelutagpt.pc;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import com.gamelutagpt.BuildConfig;
import com.gamelutagpt.GameView;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * O jogo no PC: uma janela com o {@code GameView} do Android dentro.
 *
 * <p>A imagem é sempre 16:9 (com faixas pretas se a janela tiver outra proporção), o mouse
 * é o dedo nos botões da tela, e teclado/controle chegam ao jogo como teclas do Android
 * (mapa no {@code HardwareInput}). F11 ou Alt+Enter: tela cheia.
 */
public final class JogoPc {
    private static final String TAG = "JogoPc";
    /** Resolução máxima desenhada; janelas maiores ampliam o quadro. */
    private static final int MAX_LARGURA = resolucaoMaxima();

    /** Criado depois que a janela abre (carregar os atlas leva alguns segundos). */
    private volatile GameView jogo;
    private final JFrame janela = new JFrame();
    private final Canvas tela = new Canvas();
    private final QuadroPc quadro;
    private final Map<Integer, Integer> repeticoes = new HashMap<>();
    private Controles controles;
    /** Área da tela onde o quadro 16:9 aparece (atualizada no redimensionamento). */
    private volatile Rectangle area = new Rectangle(0, 0, 1280, 720);
    private boolean telaCheia;
    private Rectangle janelaNormal;
    private boolean mouseApertado;
    /** "CARREGANDO" animado enquanto o GameView é montado (thread da janela). */
    private javax.swing.Timer animacaoCarregando;
    private android.content.SharedPreferences preferencias;
    /** Alt+Enter/F11 segurados não ficam alternando a tela cheia. */
    private boolean atalhoTelaCheia;

    private JogoPc() {
        quadro = new QuadroPc(1280, 720, this::apresentar);
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("sun.java2d.noddraw", "true");
        // Windows: o quadro já sai pronto do desenho por software e o Direct3D do Java2D só o
        // copia para a tela; com alguns drivers essa cópia estraga a imagem até a janela ser
        // recriada (F11 duas vezes). Pelo GDI a cópia é a mesma, sem esse problema.
        // -Dgameluta.d3d=true volta ao Direct3D.
        if (!Boolean.getBoolean("gameluta.d3d")) System.setProperty("sun.java2d.d3d", "false");
        DadosDoJogo dados;
        try {
            dados = DadosDoJogo.localizar();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Não achei os arquivos do jogo.\n" + e.getMessage(),
                "Game Luta GPT", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }
        Log.i(TAG, "Game Luta GPT " + BuildConfig.VERSION_NAME + " — dados em " + dados.origem);
        Context contexto = dados.contexto(DadosDoJogo.pastaDePreferencias());
        JogoPc pc = new JogoPc();
        // Tela cheia: a última escolha (Alt+Enter/F11) ou --tela-cheia / --janela.
        pc.preferencias = contexto.getSharedPreferences("pc_janela", Context.MODE_PRIVATE);
        boolean cheia = pc.preferencias.getBoolean("telaCheia", false);
        for (String a : args) {
            if (a.equals("--tela-cheia")) cheia = true;
            if (a.equals("--janela")) cheia = false;
        }
        boolean telaCheiaInicial = cheia;
        EventQueue.invokeAndWait(() -> pc.abrir(contexto, telaCheiaInicial));
        // Carrega fora da thread da janela; ela já mostra "Carregando…".
        GameView jogo;
        try {
            jogo = new GameView(contexto);
        } catch (RuntimeException | Error e) {
            Log.e(TAG, "falha ao carregar o jogo", e);
            EventQueue.invokeLater(() -> {
                JOptionPane.showMessageDialog(pc.janela, "Falha ao carregar o jogo:\n" + e, "Game Luta GPT", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            });
            return;
        }
        EventQueue.invokeLater(() -> pc.comecar(jogo));
    }

    private void abrir(Context contexto, boolean comecarEmTelaCheia) {
        janela.setTitle("Game Luta GPT v" + BuildConfig.VERSION_NAME.split("-")[0]);
        janela.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        janela.setBackground(Color.BLACK);
        Image icone = icone(contexto);
        if (icone != null) janela.setIconImage(icone);
        tela.setBackground(Color.BLACK);
        tela.setPreferredSize(new Dimension(1280, 720));
        tela.setFocusTraversalKeysEnabled(false);  // TAB é tecla do jogo
        tela.setIgnoreRepaint(true);
        janela.add(tela);
        janela.pack();
        janela.setLocationRelativeTo(null);
        janela.setVisible(true);
        janela.toFront();
        tela.createBufferStrategy(2);
        tela.requestFocus();

        // Alt+Enter e F11 antes de qualquer componente (e com qualquer foco): no Windows o Alt
        // mexe no menu da janela e o atalho podia nem chegar ao jogo.
        java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            boolean atalho = e.getKeyCode() == KeyEvent.VK_F11 || (e.getKeyCode() == KeyEvent.VK_ENTER && e.isAltDown());
            if (e.getID() == KeyEvent.KEY_PRESSED && atalho) {
                if (!atalhoTelaCheia) {
                    atalhoTelaCheia = true;
                    alternarTelaCheia();
                }
                e.consume();
                return true;
            }
            if (e.getID() == KeyEvent.KEY_RELEASED && (e.getKeyCode() == KeyEvent.VK_F11 || e.getKeyCode() == KeyEvent.VK_ENTER)) {
                boolean eraAtalho = atalhoTelaCheia;
                atalhoTelaCheia = false;
                if (eraAtalho) {
                    e.consume();
                    return true;
                }
            }
            // Alt sozinho não abre o menu da janela no Windows (roubaria as próximas teclas).
            if (e.getKeyCode() == KeyEvent.VK_ALT) e.consume();
            return false;
        });

        tela.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { teclaApertada(e); }
            @Override public void keyReleased(KeyEvent e) { teclaSolta(e); }
        });
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) return;
                mouseApertado = true;
                toque(MotionEvent.ACTION_DOWN, e);
            }
            @Override public void mouseDragged(MouseEvent e) { if (mouseApertado) toque(MotionEvent.ACTION_MOVE, e); }
            @Override public void mouseReleased(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1 || !mouseApertado) return;
                mouseApertado = false;
                toque(MotionEvent.ACTION_UP, e);
            }
        };
        tela.addMouseListener(mouse);
        tela.addMouseMotionListener(mouse);
        tela.addFocusListener(new FocusAdapter() {
            // Sem foco a janela não recebe o "soltar": solta tudo para nada ficar preso.
            @Override public void focusLost(FocusEvent e) { soltarTudo(); }
        });
        tela.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { redimensionar(); }
        });
        janela.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { fechar(); }
            @Override public void windowActivated(WindowEvent e) { tela.requestFocusInWindow(); }
            @Override public void windowIconified(WindowEvent e) { if (jogo != null) jogo.pauseGame(); }
            @Override public void windowDeiconified(WindowEvent e) { if (jogo != null) jogo.resumeGame(); }
        });

        if (comecarEmTelaCheia) alternarTelaCheia();
        redimensionar();
        long inicio = System.currentTimeMillis();
        animacaoCarregando = new javax.swing.Timer(33, ev -> carregando(System.currentTimeMillis() - inicio));
        animacaoCarregando.start();
    }

    /** Liga o jogo carregado à janela (thread da janela). */
    private void comecar(GameView carregado) {
        if (animacaoCarregando != null) animacaoCarregando.stop();
        // No PC o controle virtual da tela some: é teclado/controle (-Dgameluta.controleNaTela=true mostra).
        carregado.setTouchControlsVisible(Boolean.getBoolean("gameluta.controleNaTela"));
        jogo = carregado;
        controles = new Controles(carregado);
        redimensionar();
        carregado.attach(quadro, quadro.largura(), quadro.altura());
        controles.iniciar();
    }

    private final com.gamelutagpt.TelaCarregando telaCarregando = new com.gamelutagpt.TelaCarregando();
    private final android.graphics.Canvas canvasCarregando = new android.graphics.Canvas();

    /** A mesma tela de carregamento do jogo, enquanto os atlas carregam. */
    private void carregando(long ms) {
        if (jogo != null) return;
        BufferedImage img = new BufferedImage(quadro.largura(), quadro.altura(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        canvasCarregando.begin(g, img, img.getWidth(), img.getHeight());
        telaCarregando.draw(canvasCarregando, ms);
        canvasCarregando.end().dispose();
        apresentar(img);
    }

    private void redimensionar() {
        if (jogo == null) {
            area = encaixar(tela.getWidth(), tela.getHeight());
            return;
        }
        Rectangle a = encaixar(tela.getWidth(), tela.getHeight());
        area = a;
        int qw = Math.min(a.width, MAX_LARGURA);
        int qh = Math.round(qw * 9f / 16f);
        quadro.setTamanho(qw, qh);
        jogo.resize(qw, qh);
    }

    /** Maior 16:9 que cabe na janela, centralizado. */
    private static Rectangle encaixar(int largura, int altura) {
        int w = Math.max(1, largura), h = Math.max(1, altura);
        int aw = w, ah = Math.round(w * 9f / 16f);
        if (ah > h) { ah = h; aw = Math.round(h * 16f / 9f); }
        return new Rectangle((w - aw) / 2, (h - ah) / 2, aw, ah);
    }

    /** Chamado na thread do jogo com o quadro pronto. */
    private void apresentar(BufferedImage imagem) {
        BufferStrategy estrategia = tela.getBufferStrategy();
        if (estrategia == null) return;
        Rectangle a = area;
        try {
            do {
                do {
                    Graphics2D g = (Graphics2D) estrategia.getDrawGraphics();
                    try {
                        g.setColor(Color.BLACK);
                        g.fillRect(0, 0, tela.getWidth(), tela.getHeight());
                        if (a.width != imagem.getWidth()) {
                            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        }
                        g.drawImage(imagem, a.x, a.y, a.width, a.height, null);
                    } finally {
                        g.dispose();
                    }
                } while (estrategia.contentsRestored());
                estrategia.show();
            } while (estrategia.contentsLost());
            java.awt.Toolkit.getDefaultToolkit().sync();
        } catch (IllegalStateException e) {
            // A janela está sendo recriada (tela cheia): pula este quadro.
        }
    }

    /**
     * Tela cheia de verdade (GraphicsDevice.setFullScreenWindow), sem borda e por cima da barra
     * de tarefas; sem suporte, janela sem borda do tamanho do monitor. A escolha fica salva.
     */
    private void alternarTelaCheia() {
        java.awt.GraphicsDevice monitor = janela.getGraphicsConfiguration().getDevice();
        if (jogo != null) jogo.pauseGame();
        telaCheia = !telaCheia;
        if (telaCheia) {
            janelaNormal = janela.getBounds();
            janela.dispose();
            janela.setUndecorated(true);
            if (monitor.isFullScreenSupported()) {
                janela.setVisible(true);
                monitor.setFullScreenWindow(janela);
            } else {
                janela.setBounds(monitor.getDefaultConfiguration().getBounds());
                janela.setVisible(true);
            }
        } else {
            if (monitor.getFullScreenWindow() == janela) monitor.setFullScreenWindow(null);
            janela.dispose();
            janela.setUndecorated(false);
            if (janelaNormal != null) janela.setBounds(janelaNormal);
            janela.setVisible(true);
        }
        janela.toFront();
        tela.createBufferStrategy(2);
        tela.requestFocus();
        redimensionar();
        if (preferencias != null) preferencias.edit().putBoolean("telaCheia", telaCheia).apply();
        if (jogo != null) jogo.resumeGame();
    }

    private void fechar() {
        GameView j = jogo;
        if (j != null) {
            controles.parar();
            j.pauseGame();
            j.detach();
            j.release();
        }
        janela.dispose();
        System.exit(0);
    }

    // ------------------------------------------------------------------ entrada

    private void teclaApertada(KeyEvent e) {
        if (jogo == null) return;
        int codigo = Teclas.paraAndroid(e.getKeyCode(), e.getExtendedKeyCode());
        Log.d(TAG, "tecla " + KeyEvent.getKeyText(e.getKeyCode()) + " ext=" + e.getExtendedKeyCode() + " → " + codigo);
        if (codigo == 0) return;
        Integer vezes = repeticoes.get(codigo);
        int repeticao = vezes == null ? 0 : vezes + 1;
        repeticoes.put(codigo, repeticao);
        jogo.onKeyDown(codigo, new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, codigo, repeticao));
    }

    private void teclaSolta(KeyEvent e) {
        if (jogo == null) return;
        int codigo = Teclas.paraAndroid(e.getKeyCode(), e.getExtendedKeyCode());
        if (codigo == 0 || repeticoes.remove(codigo) == null) return;
        jogo.onKeyUp(codigo, new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, codigo));
    }

    private void soltarTudo() {
        if (jogo == null) return;
        for (Integer codigo : repeticoes.keySet()) {
            if (codigo > 0) jogo.onKeyUp(codigo, new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, codigo));
        }
        repeticoes.clear();
        if (mouseApertado) {
            mouseApertado = false;
            jogo.onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0, 0, 0));
        }
    }

    /** Mouse em pixels da janela → pixels do quadro (o GameView converte para 1280x720). */
    private void toque(int acao, MouseEvent e) {
        if (jogo == null) return;
        Rectangle a = area;
        float x = (e.getX() - a.x) * quadro.largura() / (float) a.width;
        float y = (e.getY() - a.y) * quadro.altura() / (float) a.height;
        long agora = System.currentTimeMillis();
        jogo.onTouchEvent(MotionEvent.obtain(agora, agora, acao, x, y, 0));
    }

    private static Image icone(Context contexto) {
        try (InputStream in = contexto.getAssets().open("selection/portraits/p01.png")) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) return null;
            int lado = Math.min(img.getWidth(), img.getHeight());
            return img.getSubimage((img.getWidth() - lado) / 2, 0, lado, lado).getScaledInstance(64, 64, Image.SCALE_SMOOTH);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static int resolucaoMaxima() {
        String r = System.getProperty("gameluta.resolucao");
        if (r != null) {
            try {
                return Math.max(320, Integer.parseInt(r.split("x")[0].trim()));
            } catch (NumberFormatException ignored) {
                Log.w(TAG, "gameluta.resolucao inválida: " + r);
            }
        }
        return 1920;
    }
}
