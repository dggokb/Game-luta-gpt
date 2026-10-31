package com.gamelutagpt;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.SurfaceView;
import com.gamelutagpt.render.RenderCanvas;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

/**
 * PC: a luta pela placa de vídeo, com o mesmo desenho em OpenGL do celular ({@link CanvasGl} e
 * {@link GlRenderCanvas}, pelo OpenGL 3.3 do LWJGL). O quadro é desenhado numa imagem da placa
 * (framebuffer), lido de volta e entregue a quem mostra (a janela do {@code JogoPc}), como o
 * quadro do Canvas: janela, teclado, controle e o resto do jogo continuam iguais.
 *
 * <p>O contexto mora numa janela escondida do GLFW, criada e atendida por uma thread só dela (no
 * Windows a janela some junto com a thread que a criou, e a do jogo vai e volta). Sem OpenGL 3.3
 * (ou no macOS, onde o GLFW e o AWT disputam a thread principal), a luta segue no Canvas.
 */
final class DesenhoGl {
    private static final String TAG = "DesenhoGl";

    interface Ouvinte {
        void superficieGl(boolean pronta);
    }

    /** O desenho aberto agora (na thread do jogo), ou null: quem desenha lutador pergunta aqui. */
    private static DesenhoGl aberto;

    static DesenhoGl noQuadro() {
        return aberto;
    }

    /** Folhas que saíram do cache: as texturas delas são apagadas no próximo quadro. */
    private static final ConcurrentLinkedQueue<Bitmap> soltas = new ConcurrentLinkedQueue<>();

    static void soltarDepois(Bitmap folha) {
        // Sem OpenGL no PC (a janela sem placa, os testes) ninguém esvazia a fila.
        if (algum) soltas.add(folha);
    }

    /** Algum desenho em OpenGL foi criado (antes disso não há textura a apagar). */
    private static volatile boolean algum;

    /** O Canvas do quadro como RenderCanvas, se for o do OpenGL. */
    static RenderCanvas comoRenderCanvas(Canvas c) {
        return c instanceof CanvasGl ? ((CanvasGl)c).comoRenderCanvas() : null;
    }

    /** O OpenGL do PC pode ser tentado aqui: fora do macOS e sem -Dgameluta.opengl=false. */
    static boolean permitido() {
        if ("false".equals(System.getProperty("gameluta.opengl"))) return false;
        return !System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("mac");
    }

    private final SurfaceView jogo;
    private final GlRenderCanvas canvas = new GlRenderCanvas();
    private final CanvasGl canvasGl = new CanvasGl(canvas);
    private final Matrix matriz = new Matrix();
    private final float[] valores = new float[9];
    /** O CanvasGl guarda a transformação e as camadas no Canvas do PC, que precisa de um destino. */
    private final BufferedImage nada = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB_PRE);
    private Graphics2D nadaG;

    private long janela;
    private GLCapabilities capacidades;
    private boolean corrente, falhou, contextoNovo, recriado, quadroAberto;
    private String motivoFalha;
    private final List<SpriteAtlasCache.Folha> prontas = new ArrayList<>();

    /** A imagem da placa onde o quadro é desenhado (cor + stencil) e o tamanho dela. */
    private int fbo, rbCor, rbStencil, fboLargura, fboAltura;
    /** O quadro lido de volta (de baixo para cima) e a imagem entregue (de cima para baixo). */
    private IntBuffer leitura;
    private BufferedImage quadro;
    private float msEnvio, msLotes, msLeitura;

    DesenhoGl(SurfaceView view, SurfaceView jogo, Ouvinte ouvinte) {
        this.jogo = jogo;
        algum = true;
        // Não há superfície separada: o quadro sai pela mesma janela do jogo.
        ouvinte.superficieGl(true);
    }

    void tamanho(int largura, int altura) {}

    /** No PC o quadro do OpenGL sai pela mesma superfície do Canvas (não há camada por baixo). */
    boolean camadaPorBaixo() { return false; }

    /** A troca do quadro não espera a batida da tela (quem mostra é a janela do AWT). */
    boolean trocaEsperaTela() { return false; }

    boolean disponivel() {
        return !falhou;
    }

    void desligar(Throwable motivo) {
        Log.e(TAG, "desenho em OpenGL desligado", motivo);
        motivoFalha = String.valueOf(motivo);
        falhou = true;
        quadroAberto = false;
        aberto = null;
    }

    String motivoFalha() {
        return falhou ? motivoFalha : null;
    }

    void tentarDeNovo() {
        falhou = false;
        motivoFalha = null;
    }

    boolean contextoRecriado() {
        boolean r = recriado;
        recriado = false;
        return r;
    }

    // ---- Contexto (thread do jogo) ----

    private boolean preparar() {
        if (falhou) return false;
        try {
            if (janela == 0) {
                if (!permitido()) return falhar("OpenGL desligado neste sistema");
                janela = JanelaGl.criar();
                if (janela == 0) return falhar("OpenGL 3.3 não abriu (" + JanelaGl.erro + ")");
                contextoNovo = true;
                corrente = false;
            }
            if (!corrente) {
                GLFW.glfwMakeContextCurrent(janela);
                if (capacidades == null) {
                    capacidades = GL.createCapabilities();
                    Log.i(TAG, "OpenGL " + GL11.glGetString(GL11.GL_VERSION) + " · " + GL11.glGetString(GL11.GL_RENDERER));
                } else {
                    GL.setCapabilities(capacidades);
                }
                corrente = true;
            }
            if (contextoNovo) {
                canvas.criar();
                canvasGl.esquecerTextos();
                contextoNovo = false;
            }
            return true;
        } catch (RuntimeException | LinkageError e) {
            Log.e(TAG, "OpenGL falhou", e);
            motivoFalha = "OpenGL não iniciou: " + e;
            falhou = true;
            return false;
        }
    }

    private boolean falhar(String motivo) {
        Log.w(TAG, motivo);
        motivoFalha = motivo;
        falhou = true;
        return false;
    }

    /** O loop do jogo vai parar: o contexto sai desta thread (a próxima o pega de volta). */
    void soltarThread() {
        if (!corrente) return;
        GLFW.glfwMakeContextCurrent(MemoryUtil.NULL);
        GL.setCapabilities(null);
        corrente = false;
    }

    /** O jogo acabou: tudo do OpenGL é solto. Com o loop parado. */
    void liberar() {
        if (janela == 0) return;
        soltarThread();
        JanelaGl.destruir(janela);
        janela = 0;
        capacidades = null;
        fbo = rbCor = rbStencil = 0;
        if (leitura != null) MemoryUtil.memFree(leitura);
        leitura = null;
    }

    // ---- Quadro ----

    Canvas comecarQuadro() {
        if (!preparar()) return null;
        SurfaceView.Target destino = jogo.alvo();
        if (destino == null) return null;
        int w = destino.larguraGl(), h = destino.alturaGl();
        if (w <= 0 || h <= 0) return null;
        Bitmap b;
        while ((b = soltas.poll()) != null) canvas.soltar(b);
        imagemDoQuadro(w, h);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        if (nadaG == null) nadaG = nada.createGraphics();
        nadaG.setClip(null);
        nadaG.setTransform(new java.awt.geom.AffineTransform());
        canvasGl.begin(nadaG, w, h);
        canvas.comecar(w, h);
        canvasGl.comecar(w, h);
        quadroAberto = true;
        aberto = this;
        return canvasGl;
    }

    /** A imagem da placa (e a de leitura) do tamanho do quadro. */
    private void imagemDoQuadro(int w, int h) {
        if (fbo != 0 && w == fboLargura && h == fboAltura) return;
        if (fbo == 0) {
            fbo = GL30.glGenFramebuffers();
            rbCor = GL30.glGenRenderbuffers();
            rbStencil = GL30.glGenRenderbuffers();
        }
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, rbCor);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL11.GL_RGBA8, w, h);
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, rbStencil);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH24_STENCIL8, w, h);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_RENDERBUFFER, rbCor);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER, rbStencil);
        int estado = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (estado != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("framebuffer incompleto: 0x" + Integer.toHexString(estado));
        }
        fboLargura = w;
        fboAltura = h;
        if (leitura != null) MemoryUtil.memFree(leitura);
        leitura = MemoryUtil.memAllocInt(w * h);
        quadro = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    }

    /** Fecha o quadro, lê de volta e entrega a quem mostra. */
    void terminarQuadro() {
        if (!quadroAberto) return;
        quadroAberto = false;
        aberto = null;
        long inicio = System.nanoTime();
        canvas.terminar();
        msLotes += (canvas.nsChamadas / 1e6f - msLotes) * 0.1f;
        long antesDaLeitura = System.nanoTime();
        int w = fboLargura, h = fboAltura;
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 4);
        leitura.clear();
        GL11.glReadPixels(0, 0, w, h, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, leitura);
        // O OpenGL guarda de baixo para cima: cada linha vai para o lugar dela na imagem.
        int[] destino = ((DataBufferInt) quadro.getRaster().getDataBuffer()).getData();
        for (int y = 0; y < h; y++) leitura.get((h - 1 - y) * w, destino, y * w, w);
        msLeitura += ((System.nanoTime() - antesDaLeitura) / 1e6f - msLeitura) * 0.1f;
        SurfaceView.Target alvo = jogo.alvo();
        if (alvo != null) alvo.mostrarPronto(quadro);
        msEnvio += ((System.nanoTime() - inicio) / 1e6f - msEnvio) * 0.1f;
    }

    void manutencao() {
        if (soltas.isEmpty() || quadroAberto || !preparar()) return;
        Bitmap b;
        while ((b = soltas.poll()) != null) canvas.soltar(b);
    }

    void semMundo() {}

    // ---- O que o quadro desenha (igual ao do celular) ----

    void sprite(Canvas c, Bitmap folha, Rect fonte, RectF destino, int alpha, Filtro filtro) {
        if (c instanceof CanvasGl) {
            ((CanvasGl)c).prepararGl();
        } else {
            c.getMatrix(matriz);
            matriz.getValues(valores);
            canvas.matriz(valores);
        }
        canvas.filtro(filtro != null ? filtro.matriz : null);
        canvas.desenharBitmap(folha, true, fonte.left, fonte.top, fonte.right, fonte.bottom,
            destino.left, destino.top, destino.right, destino.bottom, alpha);
    }

    boolean preSubir(com.gamelutagpt.render.RenderImage imagem) {
        return quadroAberto && canvas.subirImagem(imagem);
    }

    void aquecer(com.gamelutagpt.stage.StageScene cenario) {
        Canvas c = comecarQuadro();
        if (c == null) return;
        RenderCanvas rc = ((CanvasGl)c).comoRenderCanvas();
        cenario.drawBackground(rc);
        cenario.drawForeground(rc);
        canvas.terminar();
        quadroAberto = false;
        aberto = null;
    }

    int subir(SpriteAtlasCache atlases, long orcamentoNs) {
        if (!preparar()) return 0;
        long ate = System.nanoTime() + orcamentoNs;
        atlases.prontas(prontas);
        int faltam = 0;
        for (SpriteAtlasCache.Folha folha : prontas) {
            if (canvas.temTextura(folha.bitmap) || folha.bitmap.isRecycled()) continue;
            if (System.nanoTime() >= ate) {
                faltam++;
                continue;
            }
            canvas.textura(folha.bitmap, true);
        }
        prontas.clear();
        return faltam;
    }

    int[] contagem() {
        return new int[] {canvasGl.textosNovos + canvas.textosNovosRc, canvasGl.formasStencil, canvasGl.desenhos,
            Math.round(canvas.telasPintadas() * 10f), Math.round(canvas.msPlaca * 10f), canvas.quadrosAtrasPlaca};
    }

    String resumo() {
        return "OpenGL " + canvasGl.desenhos + " desenhos · " + (canvasGl.textosNovos + canvas.textosNovosRc) + " textos novos · "
            + canvasGl.formasStencil + " formas · " + canvas.chamadas + " chamadas · " + canvas.verticesQuadro + " vértices · lotes "
            + Math.round(msLotes * 10f) / 10f + " ms · leitura " + Math.round(msLeitura * 10f) / 10f + " ms · envio "
            + Math.round(msEnvio * 10f) / 10f + " ms · " + canvas.quantasTexturas() + " texturas " + (canvas.bytesTexturas() >> 20) + " MB";
    }

    /**
     * A janela escondida que guarda o contexto, numa thread só dela: criada uma vez, ela atende as
     * mensagens do sistema (no Windows, uma janela que nunca responde trava quem manda mensagem a
     * todas) até o jogo fechar.
     */
    private static final class JanelaGl {
        static volatile String erro = "";
        private static Thread thread;
        private static boolean iniciado;
        private static final ConcurrentLinkedQueue<Runnable> pedidos = new ConcurrentLinkedQueue<>();

        /** Cria a janela (com o contexto solto) e devolve o identificador dela; 0 se não deu. */
        static synchronized long criar() {
            iniciarThread();
            long[] resultado = new long[1];
            CountDownLatch pronto = new CountDownLatch(1);
            pedidos.add(() -> {
                try {
                    resultado[0] = abrir();
                } catch (RuntimeException | LinkageError e) {
                    erro = String.valueOf(e);
                } finally {
                    pronto.countDown();
                }
            });
            GLFW.glfwPostEmptyEvent();
            try {
                if (!pronto.await(10, TimeUnit.SECONDS)) erro = "a janela do OpenGL não respondeu";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return resultado[0];
        }

        static void destruir(long janela) {
            pedidos.add(() -> GLFW.glfwDestroyWindow(janela));
            GLFW.glfwPostEmptyEvent();
        }

        private static long abrir() {
            GLFW.glfwDefaultWindowHints();
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
            long j = GLFW.glfwCreateWindow(16, 16, "Game Luta GPT (OpenGL)", MemoryUtil.NULL, MemoryUtil.NULL);
            if (j == MemoryUtil.NULL && erro.isEmpty()) erro = "glfwCreateWindow";
            return j;
        }

        private static void iniciarThread() {
            if (thread != null) {
                if (!iniciado) throw new IllegalStateException(erro);
                return;
            }
            CountDownLatch iniciou = new CountDownLatch(1);
            boolean[] ok = new boolean[1];
            thread = new Thread(() -> {
                GLFW.glfwSetErrorCallback((codigo, descricao) -> {
                    erro = GLFWErrorCallback.getDescription(descricao) + " (0x" + Integer.toHexString(codigo) + ")";
                    Log.w(TAG, "GLFW: " + erro);
                });
                ok[0] = GLFW.glfwInit();
                iniciou.countDown();
                if (!ok[0]) return;
                while (true) {
                    Runnable r;
                    while ((r = pedidos.poll()) != null) r.run();
                    GLFW.glfwWaitEventsTimeout(0.5);
                }
            }, "OpenGL");
            thread.setDaemon(true);
            thread.start();
            try {
                iniciou.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            iniciado = ok[0];
            if (!ok[0]) {
                if (erro.isEmpty()) erro = "glfwInit";
                throw new IllegalStateException(erro);
            }
        }
    }
}
