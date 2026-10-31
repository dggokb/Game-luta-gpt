package com.gamelutagpt;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.gamelutagpt.render.RenderCanvas;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Desenho novo: a luta inteira (cenário, lutadores, efeitos, HUD e botões) em OpenGL ES 3, numa
 * superfície embaixo da do jogo. Quem desenha continua escrevendo no Canvas: o {@link CanvasGl}
 * transforma cada desenho em triângulos. Menus, seleção e páginas seguem no Canvas comum, na
 * superfície de cima (que na luta fica transparente).
 *
 * <p>Tudo roda na thread do jogo: o {@link GameView} abre o quadro ({@link #comecarQuadro}), desenha
 * e fecha ({@link #terminarQuadro}). O contexto do OpenGL vive enquanto o jogo vive (as texturas
 * ficam); a superfície vai e volta com a tela. Os lutadores chegam pelo {@link SpriteFighterRenderer}
 * ({@link #sprite}), com o filtro de cor de cada um.
 */
final class DesenhoGl implements SurfaceHolder.Callback {
    private static final String TAG = "DesenhoGl";

    /** O jogo fica sabendo quando a superfície do OpenGL aparece ou some (na thread da UI). */
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
        soltas.add(folha);
    }

    private final SurfaceView view;
    private final Ouvinte ouvinte;
    private final GlRenderCanvas canvas = new GlRenderCanvas();
    private final CanvasGl canvasGl = new CanvasGl(canvas);
    private final Matrix matriz = new Matrix();
    private final float[] valores = new float[9];

    private volatile Surface janela;
    private EGLDisplay tela = EGL14.EGL_NO_DISPLAY;
    private EGLContext contexto = EGL14.EGL_NO_CONTEXT;
    private EGLSurface superficie = EGL14.EGL_NO_SURFACE;
    private EGLConfig config;
    private boolean corrente, falhou, contextoNovo, recriado, quadroAberto, ultimoComMundo;
    private final List<SpriteAtlasCache.Folha> prontas = new ArrayList<>();

    /**
     * {@code view}: a superfície do mundo, embaixo; {@code jogo}: a do Canvas, que passa a ficar por
     * cima dela e transparente onde não desenha.
     */
    DesenhoGl(SurfaceView view, SurfaceView jogo, Ouvinte ouvinte) {
        this.view = view;
        this.ouvinte = ouvinte;
        view.getHolder().setFormat(PixelFormat.RGBA_8888);
        view.getHolder().addCallback(this);
        jogo.setZOrderMediaOverlay(true);
        jogo.getHolder().setFormat(PixelFormat.TRANSLUCENT);
    }

    // ---- Superfície (thread da UI) ----

    @Override public void surfaceCreated(SurfaceHolder holder) {
        janela = holder.getSurface();
        ouvinte.superficieGl(true);
    }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // A luta é a 60 quadros, como a superfície do jogo.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                holder.getSurface().setFrameRate(60f, Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE);
            } catch (RuntimeException e) {
                Log.w(TAG, "taxa de quadros não mudou", e);
            }
        }
    }

    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        // O jogo para o loop (e solta o contexto da thread dele) antes de a superfície sumir.
        ouvinte.superficieGl(false);
        janela = null;
        destruirSuperficie();
    }

    /** Mesmo tamanho da tela do jogo (o nível gráfico reduz as duas juntas). Na thread da UI. */
    void tamanho(int largura, int altura) {
        if (largura <= 0 || altura <= 0) view.getHolder().setSizeFromLayout();
        else view.getHolder().setFixedSize(largura, altura);
    }

    private void destruirSuperficie() {
        if (superficie != EGL14.EGL_NO_SURFACE && tela != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglDestroySurface(tela, superficie);
        }
        superficie = EGL14.EGL_NO_SURFACE;
        corrente = false;
    }

    // ---- Contexto (thread do jogo) ----

    /** O mundo sai numa superfície embaixo da do Canvas (no PC, pela mesma). */
    boolean camadaPorBaixo() { return true; }

    /** A troca do quadro espera a batida da tela (o loop não precisa dormir). */
    boolean trocaEsperaTela() { return true; }

    /** O OpenGL funciona neste aparelho (até agora). */
    boolean disponivel() {
        return !falhou;
    }

    /** Um quadro deu erro: o OpenGL fica desligado (a luta volta para o Canvas). */
    void desligar(Throwable motivo) {
        Log.e(TAG, "desenho em OpenGL desligado", motivo);
        motivoFalha = String.valueOf(motivo);
        falhou = true;
        quadroAberto = false;
        aberto = null;
    }

    private boolean preparar() {
        if (falhou) return false;
        Surface s = janela;
        if (s == null || !s.isValid()) return false;
        try {
            if (tela == EGL14.EGL_NO_DISPLAY) {
                tela = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
                int[] versao = new int[2];
                if (tela == EGL14.EGL_NO_DISPLAY || !EGL14.eglInitialize(tela, versao, 0, versao, 1)) {
                    return falhar("eglInitialize");
                }
            }
            if (contexto == EGL14.EGL_NO_CONTEXT) {
                // Com stencil (as formas côncavas do HUD). Sem antisserrilhado: custava 10–30% da
                // placa de vídeo na tela inteira, e círculos, anéis e brilhos já têm a borda suave
                // no shader.
                config = null;
                int[][] pedidos = {{8, 0}, {0, 0}};
                for (int[] p : pedidos) {
                    int[] pedido = {
                        EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8,
                        EGL14.EGL_STENCIL_SIZE, p[0],
                        EGL14.EGL_SAMPLE_BUFFERS, p[1] > 0 ? 1 : 0, EGL14.EGL_SAMPLES, p[1],
                        EGL14.EGL_RENDERABLE_TYPE, 0x40 /* EGL_OPENGL_ES3_BIT_KHR */,
                        EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                        EGL14.EGL_NONE};
                    EGLConfig[] configs = new EGLConfig[1];
                    int[] n = new int[1];
                    if (EGL14.eglChooseConfig(tela, pedido, 0, configs, 0, 1, n, 0) && n[0] > 0) {
                        config = configs[0];
                        break;
                    }
                }
                if (config == null) return falhar("eglChooseConfig");
                contexto = EGL14.eglCreateContext(tela, config, EGL14.EGL_NO_CONTEXT,
                    new int[] {EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE}, 0);
                if (contexto == null || contexto == EGL14.EGL_NO_CONTEXT) {
                    contexto = EGL14.EGL_NO_CONTEXT;
                    return falhar("eglCreateContext");
                }
                contextoNovo = true;
            }
            if (superficie == EGL14.EGL_NO_SURFACE) {
                superficie = EGL14.eglCreateWindowSurface(tela, config, s, new int[] {EGL14.EGL_NONE}, 0);
                if (superficie == null || superficie == EGL14.EGL_NO_SURFACE) {
                    superficie = EGL14.EGL_NO_SURFACE;
                    Log.w(TAG, "eglCreateWindowSurface: " + EGL14.eglGetError());
                    return false;
                }
                corrente = false;
            }
            if (!corrente) {
                if (!EGL14.eglMakeCurrent(tela, superficie, superficie, contexto)) {
                    Log.w(TAG, "eglMakeCurrent: " + EGL14.eglGetError());
                    destruirSuperficie();
                    return false;
                }
                corrente = true;
                // A luta inteira sai por aqui: esta superfície marca o ritmo da tela (ver ritmoDaTela).
                intervaloTroca = 0;
                ritmoDaTela();
            }
            if (contextoNovo) {
                // Contexto novo: as texturas de antes (e as folhas que só existiam nelas) se foram.
                if (canvas.quantasTexturas() > 0) recriado = true;
                canvas.criar();
                canvasGl.esquecerTextos();
                contextoNovo = false;
            }
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "OpenGL falhou", e);
            motivoFalha = "OpenGL não iniciou: " + e;
            falhou = true;
            return false;
        }
    }

    private boolean falhar(String onde) {
        int erro = EGL14.eglGetError();
        Log.e(TAG, onde + " falhou: " + erro);
        motivoFalha = "OpenGL não iniciou: " + onde + " falhou (erro EGL 0x" + Integer.toHexString(erro) + ")";
        falhou = true;
        return false;
    }

    /** Por que o OpenGL não funciona agora (para mostrar na tela), ou null. */
    private String motivoFalha;

    String motivoFalha() {
        return falhou ? motivoFalha : null;
    }

    /** O jogador religou o OpenGL: tenta de novo. */
    void tentarDeNovo() {
        falhou = false;
        motivoFalha = null;
    }

    /**
     * O contexto foi criado de novo depois de ter texturas (o aparelho perdeu o anterior): as
     * folhas que só estavam lá precisam ser decodificadas de novo. Responde uma vez.
     */
    private float taxaDaTela() {
        try {
            android.view.Display tela = view.getDisplay();
            return tela != null ? tela.getRefreshRate() : 60f;
        } catch (RuntimeException e) {
            return 60f;
        }
    }

    boolean contextoRecriado() {
        boolean r = recriado;
        recriado = false;
        return r;
    }

    /** O loop do jogo vai parar: o contexto sai desta thread (a próxima o pega de volta). */
    void soltarThread() {
        if (!corrente || tela == EGL14.EGL_NO_DISPLAY) return;
        EGL14.eglMakeCurrent(tela, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        corrente = false;
    }

    /** O jogo acabou: tudo do OpenGL é solto. Com o loop parado. */
    void liberar() {
        if (tela == EGL14.EGL_NO_DISPLAY) return;
        destruirSuperficie();
        if (contexto != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(tela, contexto);
        contexto = EGL14.EGL_NO_CONTEXT;
        EGL14.eglTerminate(tela);
        tela = EGL14.EGL_NO_DISPLAY;
    }

    // ---- Quadro ----

    /**
     * Abre o quadro: o Canvas que desenha no OpenGL, do tamanho da superfície, começando preto.
     * Null se o OpenGL não está pronto (o quadro sai no Canvas comum).
     */
    /** Batidas da tela por troca de quadro (0 = ainda não definido) e quadros até reler a taxa. */
    private int intervaloTroca, quadrosAteReler;

    /**
     * Numa tela que ficou em 120 Hz (o pedido de 60 é só uma preferência), uma troca a cada duas
     * batidas: o jogo anda a 60, e desenhar 120 quadros era o dobro de trabalho. A taxa é relida a
     * cada meio segundo: lida uma vez só, a tela que depois foi para 60 Hz ficava em 30 FPS.
     */
    private void ritmoDaTela() {
        if (--quadrosAteReler > 0 && intervaloTroca != 0) return;
        quadrosAteReler = 30;
        int intervalo = taxaDaTela() >= 100f ? 2 : 1;
        if (intervalo != intervaloTroca) {
            intervaloTroca = intervalo;
            EGL14.eglSwapInterval(tela, intervalo);
        }
    }

    Canvas comecarQuadro() {
        if (!preparar()) return null;
        ritmoDaTela();
        Bitmap b;
        while ((b = soltas.poll()) != null) canvas.soltar(b);
        int[] w = new int[1], h = new int[1];
        EGL14.eglQuerySurface(tela, superficie, EGL14.EGL_WIDTH, w, 0);
        EGL14.eglQuerySurface(tela, superficie, EGL14.EGL_HEIGHT, h, 0);
        if (w[0] <= 0 || h[0] <= 0) return null;
        canvas.comecar(w[0], h[0]);
        canvasGl.comecar(w[0], h[0]);
        quadroAberto = true;
        aberto = this;
        return canvasGl;
    }

    /** Fecha e mostra o quadro do mundo. */
    void terminarQuadro() {
        if (!quadroAberto) return;
        quadroAberto = false;
        aberto = null;
        long inicio = System.nanoTime();
        canvas.terminar();
        // Tempo nas chamadas ao OpenGL durante o quadro (mandar os lotes e desenhar).
        msLotes += (canvas.nsChamadas / 1e6f - msLotes) * 0.1f;
        mostrar();
        // Média do tempo para mandar o mundo (o que a placa de vídeo segura aparece aqui).
        msEnvio += ((System.nanoTime() - inicio) / 1e6f - msEnvio) * 0.1f;
        ultimoComMundo = true;
    }

    private float msEnvio, msLotes;

    /**
     * Fora de um quadro do mundo (luta no Canvas, menus): apaga as texturas das folhas que saíram
     * do cache. Sem isso, ao desligar o OpenGL, as texturas da luta anterior ficavam ocupando a
     * placa de vídeo junto com as folhas novas.
     */
    void manutencao() {
        if (soltas.isEmpty() || quadroAberto || !preparar()) return;
        Bitmap b;
        while ((b = soltas.poll()) != null) canvas.soltar(b);
    }

    /** Uma tela que não é a luta: a superfície do mundo fica preta (uma vez). */
    void semMundo() {
        if (!ultimoComMundo || !preparar()) return;
        ultimoComMundo = false;
        canvas.comecar(Math.max(1, view.getWidth()), Math.max(1, view.getHeight()));
        canvas.terminar();
        mostrar();
    }

    private void mostrar() {
        if (EGL14.eglSwapBuffers(tela, superficie)) return;
        int erro = EGL14.eglGetError();
        Log.w(TAG, "eglSwapBuffers: " + erro);
        if (erro == 0x300E /* EGL_CONTEXT_LOST */) {
            // O aparelho perdeu o contexto: tudo de novo no próximo quadro.
            EGL14.eglMakeCurrent(tela, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            destruirSuperficie();
            EGL14.eglDestroyContext(tela, contexto);
            contexto = EGL14.EGL_NO_CONTEXT;
            if (canvas.quantasTexturas() > 0) recriado = true;
        } else {
            destruirSuperficie();
        }
    }

    // ---- O que o quadro desenha ----

    /** O Canvas do quadro como RenderCanvas, se for o do OpenGL ({@link AndroidRenderCanvas} passa a ele). */
    static RenderCanvas comoRenderCanvas(Canvas c) {
        return c instanceof CanvasGl ? ((CanvasGl)c).comoRenderCanvas() : null;
    }

    /**
     * Um quadro de folha de lutador ({@code fonte} em pixels do bitmap) no retângulo de destino,
     * pela transformação (e recorte) de agora do Canvas, com o filtro de cor. A folha sobe na
     * primeira vez e o bitmap da memória comum é solto.
     */
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

    /** Sobe uma imagem antes de ela aparecer (o pack do Ultra, aos poucos), com o quadro aberto. */
    boolean preSubir(com.gamelutagpt.render.RenderImage imagem) {
        return quadroAberto && canvas.subirImagem(imagem);
    }

    /** O cenário uma vez, escondido (as camadas sobem antes da luta, e não no primeiro quadro). */
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

    /**
     * Sobe as folhas prontas do cache que ainda não estão na placa de vídeo, até gastar
     * {@code orcamentoNs}. Devolve quantas faltam. Com o quadro aberto ou o contexto pronto.
     */
    int subir(SpriteAtlasCache atlases, long orcamentoNs) {
        return subir(atlases, orcamentoNs, Integer.MAX_VALUE);
    }

    private int subir(SpriteAtlasCache atlases, long orcamentoNs, int maximo) {
        if (!preparar()) return 0;
        long ate = System.nanoTime() + orcamentoNs;
        atlases.prontas(prontas);
        int faltam = 0;
        for (SpriteAtlasCache.Folha folha : prontas) {
            if (canvas.temTextura(folha.bitmap) || folha.bitmap.isRecycled()) continue;
            if (maximo <= 0 || System.nanoTime() >= ate) {
                faltam++;
                continue;
            }
            canvas.textura(folha.bitmap, true);
            maximo--;
        }
        prontas.clear();
        return faltam;
    }

    /** Teste automático: textos que viraram bitmap, formas com stencil e desenhos do último quadro. */
    int[] contagem() {
        return new int[] {canvasGl.textosNovos + canvas.textosNovosRc, canvasGl.formasStencil, canvasGl.desenhos,
            Math.round(canvas.telasPintadas() * 10f), Math.round(canvas.msPlaca * 10f), canvas.quadrosAtrasPlaca};
    }

    /** Para o contador: chamadas do último quadro, texturas e memória delas. */
    String resumo() {
        return "OpenGL " + canvasGl.desenhos + " desenhos · " + (canvasGl.textosNovos + canvas.textosNovosRc) + " textos novos · "
            + canvasGl.formasStencil + " formas · " + canvas.chamadas + " chamadas · " + canvas.verticesQuadro + " vértices · lotes "
            + Math.round(msLotes * 10f) / 10f + " ms · envio " + Math.round(msEnvio * 10f) / 10f + " ms · "
            + canvas.quantasTexturas() + " texturas " + (canvas.bytesTexturas() >> 20) + " MB";
    }
}
