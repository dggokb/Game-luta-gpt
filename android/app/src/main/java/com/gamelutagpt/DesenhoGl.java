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
 * Desenho novo (B1): o mundo da luta (cenário, lutadores e o que escurece por trás deles) em
 * OpenGL ES 3, numa superfície embaixo da do jogo; o HUD, os efeitos e os controles continuam no
 * Canvas, que fica por cima e transparente onde não desenha.
 *
 * <p>Tudo roda na thread do jogo: o {@link GameView} abre o quadro ({@link #comecarQuadro}), desenha
 * o mundo aqui e o resto no Canvas, e fecha ({@link #terminarQuadro}) antes de mandar o Canvas.
 * O contexto do OpenGL vive enquanto o jogo vive (as texturas ficam); a superfície vai e volta com
 * a tela. Os lutadores chegam pelo {@link SpriteFighterRenderer}, com a transformação do Canvas
 * de agora copiada para cá a cada desenho (as poses, o zoom e a câmera continuam sendo feitos no
 * Canvas, que só não desenha).
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

    /** O OpenGL funciona neste aparelho (até agora). */
    boolean disponivel() {
        return !falhou;
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
                int[] pedido = {
                    EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_RENDERABLE_TYPE, 0x40 /* EGL_OPENGL_ES3_BIT_KHR */,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                    EGL14.EGL_NONE};
                EGLConfig[] configs = new EGLConfig[1];
                int[] n = new int[1];
                if (!EGL14.eglChooseConfig(tela, pedido, 0, configs, 0, 1, n, 0) || n[0] < 1) return falhar("eglChooseConfig");
                config = configs[0];
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
                // Sem esperar a batida da tela aqui: quem marca o ritmo é o Canvas por cima (duas
                // esperas seguidas, uma de cada superfície, derrubavam o quadro para 30).
                EGL14.eglSwapInterval(tela, 0);
            }
            if (contextoNovo) {
                // Contexto novo: as texturas de antes (e as folhas que só existiam nelas) se foram.
                if (canvas.quantasTexturas() > 0) recriado = true;
                canvas.criar();
                contextoNovo = false;
            }
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "OpenGL falhou", e);
            falhou = true;
            return false;
        }
    }

    private boolean falhar(String onde) {
        Log.e(TAG, onde + " falhou: " + EGL14.eglGetError());
        falhou = true;
        return false;
    }

    /**
     * O contexto foi criado de novo depois de ter texturas (o aparelho perdeu o anterior): as
     * folhas que só estavam lá precisam ser decodificadas de novo. Responde uma vez.
     */
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
     * Abre o quadro do mundo numa tela de {@code largura}x{@code altura} (a do Canvas por cima),
     * começando preta. False se o OpenGL não está pronto (o quadro sai todo no Canvas).
     */
    boolean comecarQuadro(int largura, int altura) {
        if (!preparar()) return false;
        Bitmap b;
        while ((b = soltas.poll()) != null) canvas.soltar(b);
        canvas.comecar(largura, altura);
        quadroAberto = true;
        aberto = this;
        return true;
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

    // ---- O que o mundo desenha ----

    /** O canvas do OpenGL com a transformação atual do Canvas, sem filtro nem recorte. */
    RenderCanvas mundo(Canvas c) {
        copiarMatriz(c);
        canvas.filtro(null);
        canvas.semRecorte();
        return canvas;
    }

    /** Triângulos soltos numa cor ({x0, y0, x1, y1, x2, y2} cada), no espaço atual do Canvas. */
    void fillTriangles(Canvas c, float[] xy, int n, int argb) {
        mundo(c);
        canvas.fillTriangles(xy, n, argb);
    }

    private void copiarMatriz(Canvas c) {
        c.getMatrix(matriz);
        matriz.getValues(valores);
        canvas.matriz(valores);
    }

    /**
     * Um quadro de folha de lutador ({@code fonte} em pixels do bitmap) no retângulo de destino,
     * pela transformação atual do Canvas, com o filtro de cor. A folha sobe na primeira vez e o
     * bitmap da memória comum é solto.
     */
    void sprite(Canvas c, Bitmap folha, Rect fonte, RectF destino, int alpha, Filtro filtro) {
        copiarMatriz(c);
        canvas.filtro(filtro != null ? filtro.matriz : null);
        canvas.desenharBitmap(folha, true, fonte.left, fonte.top, fonte.right, fonte.bottom,
            destino.left, destino.top, destino.right, destino.bottom, alpha);
    }

    /** Recorta os próximos lutadores ao retângulo (no espaço atual do Canvas), até {@link #semRecorte}. */
    void recorte(Canvas c, float l, float t, float r, float b) {
        copiarMatriz(c);
        canvas.semRecorte();
        canvas.clipPolygon(new float[] {l, t, r, t, r, b, l, b});
    }

    void semRecorte() {
        canvas.semRecorte();
    }

    /** O Canvas passa a ter a transformação atual do OpenGL (o telão desenha lutadores dentro do cenário). */
    void matrizNoCanvas(Canvas c) {
        canvas.matrizAtual(valores);
        matriz.setValues(valores);
        c.setMatrix(matriz);
    }

    /**
     * Sobe as folhas prontas do cache que ainda não estão na placa de vídeo, até gastar
     * {@code orcamentoNs}. Devolve quantas faltam. Com o quadro aberto ou o contexto pronto.
     */
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

    /** Para o contador: chamadas do último quadro, texturas e memória delas. */
    String resumo() {
        return "OpenGL " + canvas.chamadas + " chamadas · " + canvas.verticesQuadro + " vértices · lotes "
            + Math.round(msLotes * 10f) / 10f + " ms · envio " + Math.round(msEnvio * 10f) / 10f + " ms · "
            + canvas.quantasTexturas() + " texturas " + (canvas.bytesTexturas() >> 20) + " MB";
    }
}
