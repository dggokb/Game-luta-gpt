package com.gamelutagpt;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.os.Build;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import java.nio.FloatBuffer;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * {@link RenderCanvas} em OpenGL ES 3 (o desenho novo): tudo vira triângulos num buffer só e sai
 * em poucas chamadas, trocando de textura só quando a imagem muda. Brilhos, círculos e degradês
 * são feitos no shader, sem textura. As coordenadas saem em pixels da tela pela transformação
 * atual, que começa na tela virtual 1280x720 ou é copiada do Canvas ({@link #matriz}).
 *
 * <p>Além do {@link RenderCanvas}: as folhas dos lutadores ({@link #desenharBitmap}) com o filtro
 * de cor do Canvas ({@link #filtro}, a matriz 4x5 do ColorMatrix), recorte convexo (até 8 lados
 * somando os recortes), cores invertidas e texto (cada texto vira uma textura guardada).
 */
final class GlRenderCanvas implements RenderCanvas {
    /**
     * Uma textura e o tamanho do bitmap de onde ela veio (o recorte da fonte é em pixels dele). Não
     * segura o bitmap: quando ele some (o cenário ou o Ultra de outra luta), a textura é apagada.
     */
    static final class Textura extends WeakReference<Bitmap> {
        int id;
        final int largura, altura;
        final long bytes;
        boolean repetir;

        Textura(Bitmap bitmap, ReferenceQueue<Bitmap> fila, int id, int largura, int altura, long bytes) {
            super(bitmap, fila);
            this.id = id;
            this.largura = largura;
            this.altura = altura;
            this.bytes = bytes;
        }
    }

    private static final String VERTICES =
        "#version 300 es\n" +
        "uniform vec2 uTela;\n" +
        "layout(location=0) in vec2 aPos;\n" +
        "layout(location=1) in vec2 aUv;\n" +
        "layout(location=2) in vec4 aCor;\n" +
        "layout(location=3) in float aTipo;\n" +
        "out vec2 vUv; out vec4 vCor; out float vTipo;\n" +
        "void main() {\n" +
        "  vUv = aUv; vCor = aCor; vTipo = aTipo;\n" +
        "  gl_Position = vec4(aPos.x / uTela.x * 2.0 - 1.0, 1.0 - aPos.y / uTela.y * 2.0, 0.0, 1.0);\n" +
        "}\n";
    // O tipo de cada vértice: a parte inteira diz o que é e a fração leva um número dele. 0: cor
    // lisa; 1: imagem (uTex); 2: círculo (uv de -1 a 1, borda suave); 3: brilho (do centro para
    // fora, de cheio a transparente); 4: degradê linear (u é a posição nele) e 5: radial (uv vai do
    // centro, 1 na borda), com a linha do degradê na textura dos degradês na fração; 6: anel (a
    // fração é o raio de dentro); 7: texto (na textura dos textos); 8: texto com degradê linear
    // (a forma do texto na textura dos textos; a posição no degradê vem no vermelho da cor). Cores já multiplicadas pelo
    // alfa. O filtro vale só para imagem e trabalha na cor sem o alfa multiplicado, como o
    // ColorMatrix do Android. O recorte são semiplanos em pixels da janela (y para cima).
    private static final String FRAGMENTOS =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "uniform sampler2D uTex; uniform sampler2D uTexto; uniform sampler2D uDegrade;\n" +
        "uniform mat4 uCor; uniform vec4 uCorSoma; uniform bool uFiltro; uniform bool uInverte;\n" +
        "uniform highp vec3 uRecorte[32]; uniform int uNRecorte;\n" +
        "in highp vec2 vUv; in vec4 vCor; in highp float vTipo;\n" +
        "out vec4 cor;\n" +
        "void main() {\n" +
        "  highp vec3 p = vec3(gl_FragCoord.xy, 1.0);\n" +
        "  for (int i = 0; i < 32; i++) {\n" +
        "    if (i >= uNRecorte) break;\n" +
        "    if (dot(uRecorte[i], p) < 0.0) discard;\n" +
        "  }\n" +
        "  highp float k = floor(vTipo + 0.0005);\n" +
        "  highp float q = vTipo - k;\n" +
        "  vec4 c;\n" +
        "  if (k < 0.5) { c = vCor; }\n" +
        "  else if (k < 1.5) {\n" +
        "    vec4 t = texture(uTex, vUv);\n" +
        "    if (uFiltro) {\n" +
        "      vec3 s = t.a > 0.0 ? t.rgb / t.a : vec3(0.0);\n" +
        "      vec4 u = clamp(uCor * vec4(s, t.a) + uCorSoma, 0.0, 1.0);\n" +
        "      t = vec4(u.rgb * u.a, u.a);\n" +
        "    }\n" +
        "    c = t * vCor;\n" +
        "  } else if (k < 3.5) {\n" +
        "    float d = length(vUv);\n" +
        "    float a = k < 2.5 ? clamp((1.0 - d) / max(fwidth(d), 1e-4), 0.0, 1.0) : clamp(1.0 - d, 0.0, 1.0);\n" +
        "    c = vCor * a;\n" +
        "  } else if (k < 5.5) {\n" +
        "    highp float t = k < 4.5 ? vUv.x : length(vUv);\n" +
        "    c = texture(uDegrade, vec2(clamp(t, 0.0, 1.0) * 0.99609375 + 0.001953125, q / 0.99)) * vCor;\n" +
        "  } else if (k < 6.5) {\n" +
        "    float d = length(vUv), w = max(fwidth(d), 1e-4);\n" +
        "    c = vCor * (clamp((1.0 - d) / w, 0.0, 1.0) * clamp((d - q) / w, 0.0, 1.0));\n" +
        "  } else if (k < 7.5) {\n" +
        "    c = texture(uTexto, vUv) * vCor;\n" +
        "  } else {\n" +
        "    float m = texture(uTexto, vUv).a;\n" +
        "    vec4 g = texture(uDegrade, vec2(clamp(vCor.r, 0.0, 1.0) * 0.99609375 + 0.001953125, q / 0.99));\n" +
        "    c = g * (m * vCor.a);\n" +
        "  }\n" +
        "  if (uInverte) c.rgb = vec3(c.a) - c.rgb;\n" +
        "  cor = c;\n" +
        "}\n";

    private static final int FLOATS = 9;
    /** Vértices de um quadro inteiro (passando disso, o que já tem vai e o quadro continua). */
    private static final int MAX_VERTICES = 6 * 32768;
    private static final int MAX_PLANOS = 32;
    /**
     * Os vértices do lote num float[] comum, que vai inteiro para o OpenGL na hora de desenhar
     * (antes eram postos um número por vez num FloatBuffer direto: no celular isso custava
     * vários ms por quadro só no cenário).
     */
    private final float[] dados = new float[MAX_VERTICES * FLOATS];
    private final FloatBuffer envio = FloatBuffer.wrap(dados);
    private int vertices, posicao, inicioLote;
    /** Por quadro, para o contador: vértices e o tempo nas chamadas ao OpenGL. */
    int verticesQuadro;
    long nsChamadas;
    private int texturaAtual;
    private int programa, uTela, uTex, uTexto, uDegrade, uCor, uCorSoma, uFiltro, uInverte, uRecorte, uNRecorte, vbo, vao;
    /** Textura de todos os textos (2048x2048) e de todos os degradês (uma linha cada). */
    private int atlasTexto, atlasDegrade;
    private int larguraTela, alturaTela;
    private int maxTextura = 4096;
    /** Chamadas de desenho do quadro (para o contador). */
    int chamadas;

    // Transformação afim atual: x' = a*x + c*y + e; y' = b*x + d*y + f (pixels da tela).
    private float a = 1f, b, c, d = 1f, e, f;
    private static final int PILHA = 64;
    private final float[][] pilhaMatriz = new float[PILHA][6];
    private final float[][] pilhaRecorte = new float[PILHA][];
    private final int[] pilhaNPlanos = new int[PILHA];
    private int topo;

    /** O estado do lote atual: o que muda aqui fecha o lote antes ({@link #descarregar}). */
    private float[] filtro;
    private boolean inverte;
    private float[] recorte;
    private int nPlanos;
    private final float[] matrizCor = new float[16];
    /** Transparência que vale para tudo o que vier (camada com alfa do Canvas), de 0 a 1. */
    private float alfaGlobal = 1f;

    /** Por bitmap (o Bitmap não redefine equals: a chave é o próprio objeto), sem segurá-lo. */
    private final Map<Bitmap, Textura> texturas = new WeakHashMap<>();
    private final ReferenceQueue<Bitmap> esquecidas = new ReferenceQueue<>();
    private long bytesTexturas;

    /** Na thread do OpenGL, com o contexto criado (de novo a cada contexto novo). */
    void criar() {
        programa = GLES30.glCreateProgram();
        GLES30.glAttachShader(programa, shader(GLES30.GL_VERTEX_SHADER, VERTICES));
        GLES30.glAttachShader(programa, shader(GLES30.GL_FRAGMENT_SHADER, FRAGMENTOS));
        GLES30.glLinkProgram(programa);
        int[] ok = new int[1];
        GLES30.glGetProgramiv(programa, GLES30.GL_LINK_STATUS, ok, 0);
        if (ok[0] == 0) throw new IllegalStateException("shader: " + GLES30.glGetProgramInfoLog(programa));
        uTela = GLES30.glGetUniformLocation(programa, "uTela");
        uTex = GLES30.glGetUniformLocation(programa, "uTex");
        uTexto = GLES30.glGetUniformLocation(programa, "uTexto");
        uDegrade = GLES30.glGetUniformLocation(programa, "uDegrade");
        uCor = GLES30.glGetUniformLocation(programa, "uCor");
        uCorSoma = GLES30.glGetUniformLocation(programa, "uCorSoma");
        uFiltro = GLES30.glGetUniformLocation(programa, "uFiltro");
        uInverte = GLES30.glGetUniformLocation(programa, "uInverte");
        uRecorte = GLES30.glGetUniformLocation(programa, "uRecorte");
        uNRecorte = GLES30.glGetUniformLocation(programa, "uNRecorte");
        int[] ids = new int[1];
        GLES30.glGenBuffers(1, ids, 0);
        vbo = ids[0];
        // O formato dos vértices fica guardado no VAO: cada lote só manda os dados e desenha.
        GLES30.glGenVertexArrays(1, ids, 0);
        vao = ids[0];
        GLES30.glBindVertexArray(vao);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
        int passo = FLOATS * 4;
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, passo, 0);
        GLES30.glEnableVertexAttribArray(1);
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, passo, 8);
        GLES30.glEnableVertexAttribArray(2);
        GLES30.glVertexAttribPointer(2, 4, GLES30.GL_FLOAT, false, passo, 16);
        GLES30.glEnableVertexAttribArray(3);
        GLES30.glVertexAttribPointer(3, 1, GLES30.GL_FLOAT, false, passo, 32);
        GLES30.glGetIntegerv(GLES30.GL_MAX_TEXTURE_SIZE, ids, 0);
        if (ids[0] > 0) maxTextura = ids[0];
        // O contexto é novo: as texturas antigas não existem mais.
        for (Textura t : texturas.values()) t.id = 0;
        texturas.clear();
        textosProprios.clear();
        texturasDeTextoVivas.clear();
        textos.clear();
        ultimoDoTexto.clear();
        degrades.clear();
        linhaDoObjeto.clear();
        // 4096 onde a placa aceita (quase todas): 2048 enchia algumas vezes por luta, e a cada vez
        // todos os textos da tela eram refeitos no mesmo quadro.
        ladoTexto = Math.min(4096, maxTextura);
        atlasTexto = novaTextura(ladoTexto, ladoTexto);
        atlasDegrade = novaTextura(256, LINHAS_DEGRADE);
        geracaoTexto++;
        prateleiraX = prateleiraY = alturaPrateleira = 0;
        comandos = 0;
        vertices = posicao = inicioLote = 0;
        bytesTexturas = 0;
    }

    private static int shader(int tipo, String fonte) {
        int s = GLES30.glCreateShader(tipo);
        GLES30.glShaderSource(s, fonte);
        GLES30.glCompileShader(s);
        int[] ok = new int[1];
        GLES30.glGetShaderiv(s, GLES30.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) throw new IllegalStateException("shader: " + GLES30.glGetShaderInfoLog(s));
        return s;
    }

    /** Começa o quadro numa tela de {@code largura}x{@code altura} pixels, preta, na tela virtual. */
    void comecar(int largura, int altura) {
        textosNovosRc = 0;
        areaPintada = 0;
        cantoTri = 0;
        larguraTela = largura;
        alturaTela = altura;
        GLES30.glViewport(0, 0, largura, altura);
        GLES30.glClearColor(0f, 0f, 0f, 1f);
        GLES30.glClearStencil(0);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_STENCIL_BUFFER_BIT);
        GLES30.glDisable(GLES30.GL_STENCIL_TEST);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        // Texturas de bitmaps que já não existem (cenário e Ultra de lutas passadas).
        java.lang.ref.Reference<? extends Bitmap> ref;
        while ((ref = esquecidas.poll()) != null) apagar((Textura)ref);
        GLES30.glUseProgram(programa);
        GLES30.glUniform2f(uTela, largura, altura);
        GLES30.glUniform1i(uTex, 0);
        GLES30.glUniform1i(uTexto, 1);
        GLES30.glUniform1i(uDegrade, 2);
        a = largura / Arena.VW; b = 0f; c = 0f; d = altura / Arena.VH; e = 0f; f = 0f;
        topo = 0;
        vertices = 0;
        posicao = 0;
        inicioLote = 0;
        comandos = 0;
        texturaAtual = 0;
        chamadas = 0;
        verticesQuadro = 0;
        nsChamadas = 0;
        filtro = null;
        inverte = false;
        recorte = null;
        nPlanos = 0;
        alfaGlobal = 1f;
    }

    void terminar() {
        descarregar();
        executar();
    }

    int largura() { return larguraTela; }
    int altura() { return alturaTela; }
    long bytesTexturas() { return bytesTexturas; }
    int quantasTexturas() { return texturas.size(); }

    // ---- Estado ----

    /** A transformação atual vira a do Canvas ({@code v}: os 9 valores do Matrix do Android). */
    void matriz(float[] v) {
        a = v[0]; c = v[1]; e = v[2];
        b = v[3]; d = v[4]; f = v[5];
    }

    /** Os 9 valores do Matrix do Android com a transformação atual. */
    void matrizAtual(float[] v) {
        v[0] = a; v[1] = c; v[2] = e;
        v[3] = b; v[4] = d; v[5] = f;
        v[6] = 0f; v[7] = 0f; v[8] = 1f;
    }

    /** Filtro de cor das próximas imagens: a matriz 4x5 do ColorMatrix (somas de 0 a 255), ou null. */
    void filtro(float[] m) {
        if (m == filtro) return;
        descarregar();
        filtro = m;
    }

    /** Tira todo recorte (fora da pilha de save/restore: o desenho dos lutadores). */
    void semRecorte() {
        if (nPlanos == 0) return;
        descarregar();
        recorte = null;
        nPlanos = 0;
    }

    @Override public void save() {
        if (topo >= PILHA) throw new IllegalStateException("save demais no OpenGL");
        float[] m = pilhaMatriz[topo];
        m[0] = a; m[1] = b; m[2] = c; m[3] = d; m[4] = e; m[5] = f;
        pilhaRecorte[topo] = recorte;
        pilhaNPlanos[topo] = nPlanos;
        topo++;
    }

    @Override public void restore() {
        if (topo == 0) return;
        topo--;
        float[] m = pilhaMatriz[topo];
        a = m[0]; b = m[1]; c = m[2]; d = m[3]; e = m[4]; f = m[5];
        if (pilhaRecorte[topo] != recorte || pilhaNPlanos[topo] != nPlanos) {
            descarregar();
            recorte = pilhaRecorte[topo];
            nPlanos = pilhaNPlanos[topo];
        }
        pilhaRecorte[topo] = null;
    }

    @Override public void translate(float dx, float dy) {
        e += a * dx + c * dy;
        f += b * dx + d * dy;
    }

    @Override public void scale(float sx, float sy, float px, float py) {
        translate(px, py);
        a *= sx; b *= sx; c *= sy; d *= sy;
        translate(-px, -py);
    }

    @Override public void rotate(float graus, float px, float py) {
        translate(px, py);
        double r = Math.toRadians(graus);
        float cos = (float)Math.cos(r), sin = (float)Math.sin(r);
        float na = a * cos + c * sin, nb = b * cos + d * sin, nc = -a * sin + c * cos, nd = -b * sin + d * cos;
        a = na; b = nb; c = nc; d = nd;
        translate(-px, -py);
    }

    /**
     * Recorte a um polígono convexo, somado aos de antes. Cada lado vira um semiplano em pixels da
     * janela do OpenGL (y para cima), do lado de dentro do polígono.
     */
    @Override public void clipPolygon(float[] xy) {
        int n = xy.length / 2;
        if (n < 3) return;
        descarregar();
        float[] px = new float[n * 2];
        float area = 0f;
        for (int i = 0; i < n; i++) {
            float x = xy[i * 2], y = xy[i * 2 + 1];
            px[i * 2] = a * x + c * y + e;
            px[i * 2 + 1] = alturaTela - (b * x + d * y + f);
        }
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            area += px[i * 2] * px[j * 2 + 1] - px[j * 2] * px[i * 2 + 1];
        }
        float sinal = area >= 0f ? 1f : -1f;
        int total = Math.min(MAX_PLANOS, nPlanos + n);
        float[] planos = new float[MAX_PLANOS * 3];
        if (recorte != null) System.arraycopy(recorte, 0, planos, 0, nPlanos * 3);
        for (int i = 0, k = nPlanos; i < n && k < total; i++, k++) {
            int j = (i + 1) % n;
            float pxi = px[i * 2], pyi = px[i * 2 + 1];
            float qa = -(px[j * 2 + 1] - pyi) * sinal, qb = (px[j * 2] - pxi) * sinal;
            planos[k * 3] = qa;
            planos[k * 3 + 1] = qb;
            planos[k * 3 + 2] = -(qa * pxi + qb * pyi);
        }
        recorte = planos;
        nPlanos = total;
    }

    @Override public void beginInvert() {
        if (inverte) return;
        descarregar();
        inverte = true;
    }

    @Override public void endInvert() {
        if (!inverte) return;
        descarregar();
        inverte = false;
    }

    // ---- Lotes ----

    // O quadro é gravado: cada lote vira um comando (de onde a onde nos vértices, a textura, o
    // filtro, a inversão, o recorte e o stencil dele) e no fim tudo sobe de uma vez e os comandos
    // são desenhados. Mandar os vértices a cada troca de estado (centenas por quadro com o HUD)
    // fazia a placa de vídeo parar a cada envio.
    private int comandos;
    private int[] cmdInicio = new int[512], cmdQuantos = new int[512], cmdTextura = new int[512], cmdStencil = new int[512];
    private int[] cmdNPlanos = new int[512];
    private boolean[] cmdInverte = new boolean[512];
    private float[][] cmdFiltro = new float[512][], cmdRecorte = new float[512][];
    /** Stencil do lote: 0 nenhum; 1 marca (soma); 2 marca (par/ímpar); 3 pinta onde marcou. */
    private int stencil;

    /** Fecha o lote atual como um comando (a troca de estado vem depois). */
    private void descarregar() {
        if (vertices == inicioLote) return;
        if (comandos == cmdInicio.length) {
            int n = comandos * 2;
            cmdInicio = java.util.Arrays.copyOf(cmdInicio, n);
            cmdQuantos = java.util.Arrays.copyOf(cmdQuantos, n);
            cmdTextura = java.util.Arrays.copyOf(cmdTextura, n);
            cmdStencil = java.util.Arrays.copyOf(cmdStencil, n);
            cmdNPlanos = java.util.Arrays.copyOf(cmdNPlanos, n);
            cmdInverte = java.util.Arrays.copyOf(cmdInverte, n);
            cmdFiltro = java.util.Arrays.copyOf(cmdFiltro, n);
            cmdRecorte = java.util.Arrays.copyOf(cmdRecorte, n);
        }
        int k = comandos++;
        cmdInicio[k] = inicioLote;
        cmdQuantos[k] = vertices - inicioLote;
        cmdTextura[k] = texturaAtual;
        cmdStencil[k] = stencil;
        cmdFiltro[k] = filtro;
        cmdInverte[k] = inverte;
        cmdRecorte[k] = recorte;
        cmdNPlanos[k] = nPlanos;
        inicioLote = vertices;
    }

    /** Sobe os vértices gravados de uma vez e desenha os comandos. */
    private void executar() {
        if (comandos == 0) {
            vertices = posicao = inicioLote = 0;
            return;
        }
        long inicio = System.nanoTime();
        envio.position(0);
        GLES30.glBindVertexArray(vao);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, posicao * 4, envio, GLES30.GL_STREAM_DRAW);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, atlasTexto);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE2);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, atlasDegrade);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        int tex = -1, sten = 0;
        float[] fil = null, rec = null;
        int planos = -1;
        boolean primeiro = true, inv = false;
        for (int k = 0; k < comandos; k++) {
            if (cmdTextura[k] != tex) {
                tex = cmdTextura[k];
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex);
            }
            if (primeiro || cmdFiltro[k] != fil) {
                fil = cmdFiltro[k];
                enviarFiltro(fil);
            }
            if (primeiro || cmdInverte[k] != inv) {
                inv = cmdInverte[k];
                GLES30.glUniform1i(uInverte, inv ? 1 : 0);
            }
            if (primeiro || cmdRecorte[k] != rec || cmdNPlanos[k] != planos) {
                rec = cmdRecorte[k];
                planos = cmdNPlanos[k];
                GLES30.glUniform1i(uNRecorte, planos);
                if (planos > 0) GLES30.glUniform3fv(uRecorte, planos, rec, 0);
            }
            if (cmdStencil[k] != sten) {
                sten = cmdStencil[k];
                aplicarStencil(sten);
            }
            primeiro = false;
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, cmdInicio[k], cmdQuantos[k]);
            chamadas++;
        }
        if (sten != 0) aplicarStencil(0);
        verticesQuadro += vertices;
        for (int k = 0; k < comandos; k++) {
            cmdFiltro[k] = null;
            cmdRecorte[k] = null;
        }
        comandos = 0;
        vertices = posicao = inicioLote = 0;
        nsChamadas += System.nanoTime() - inicio;
    }

    private void aplicarStencil(int modo) {
        if (modo == 0) {
            GLES30.glColorMask(true, true, true, true);
            GLES30.glDisable(GLES30.GL_STENCIL_TEST);
            return;
        }
        GLES30.glEnable(GLES30.GL_STENCIL_TEST);
        GLES30.glStencilMask(0xFF);
        if (modo == 3) {
            GLES30.glColorMask(true, true, true, true);
            GLES30.glStencilFunc(GLES30.GL_NOTEQUAL, 0, 0xFF);
            GLES30.glStencilOp(GLES30.GL_ZERO, GLES30.GL_ZERO, GLES30.GL_ZERO);
            return;
        }
        GLES30.glColorMask(false, false, false, false);
        GLES30.glStencilFunc(GLES30.GL_ALWAYS, 0, 0xFF);
        if (modo == 2) {
            GLES30.glStencilOp(GLES30.GL_KEEP, GLES30.GL_KEEP, GLES30.GL_INVERT);
        } else {
            GLES30.glStencilOpSeparate(GLES30.GL_FRONT, GLES30.GL_KEEP, GLES30.GL_KEEP, GLES30.GL_INCR_WRAP);
            GLES30.glStencilOpSeparate(GLES30.GL_BACK, GLES30.GL_KEEP, GLES30.GL_KEEP, GLES30.GL_DECR_WRAP);
        }
    }

    private void enviarFiltro(float[] m) {
        GLES30.glUniform1i(uFiltro, m != null ? 1 : 0);
        if (m == null) return;
        // ColorMatrix é por linhas (4x5); o mat4 do GLSL é por colunas.
        for (int linha = 0; linha < 4; linha++) {
            for (int coluna = 0; coluna < 4; coluna++) matrizCor[coluna * 4 + linha] = m[linha * 5 + coluna];
        }
        GLES30.glUniformMatrix4fv(uCor, 1, false, matrizCor, 0);
        GLES30.glUniform4f(uCorSoma, m[4] / 255f, m[9] / 255f, m[14] / 255f, m[19] / 255f);
    }

    private void usar(int textura) {
        if (textura == texturaAtual) return;
        // Só troca de textura entre chamadas; cor lisa e brilhos não usam textura e seguem juntos.
        descarregar();
        texturaAtual = textura;
    }

    /** Área pintada no quadro (pixels, somando cada triângulo): o teste automático lê. */
    double areaPintada;
    private int cantoTri;
    private float triX0, triY0, triX1, triY1;

    private void area(float x, float y) {
        if (cantoTri == 0) {
            triX0 = x; triY0 = y;
        } else if (cantoTri == 1) {
            triX1 = x; triY1 = y;
        } else {
            double t = areaNaTela(triX0, triY0, triX1, triY1, x, y);
            areaPintada += t;
            if (TesteAutomatico.areaPorOrigem && t > 0.002 * larguraTela * alturaTela) {
                TesteAutomatico.areaGrande(t / ((double)larguraTela * alturaTela));
            }
        }
        cantoTri = cantoTri == 2 ? 0 : cantoTri + 1;
    }

    private final float[] poliA = new float[32], poliB = new float[32];

    /** Área do triângulo dentro da tela (a placa de vídeo não pinta o que fica fora). */
    private double areaNaTela(float x0, float y0, float x1, float y1, float x2, float y2) {
        float w = larguraTela, h = alturaTela;
        float minX = Math.min(x0, Math.min(x1, x2)), maxX = Math.max(x0, Math.max(x1, x2));
        float minY = Math.min(y0, Math.min(y1, y2)), maxY = Math.max(y0, Math.max(y1, y2));
        if (maxX <= 0 || maxY <= 0 || minX >= w || minY >= h) return 0;
        if (minX >= 0 && minY >= 0 && maxX <= w && maxY <= h) {
            return Math.abs((x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)) * 0.5;
        }
        // Sutherland–Hodgman nos quatro lados da tela.
        float[] a = poliA, b = poliB;
        a[0] = x0; a[1] = y0; a[2] = x1; a[3] = y1; a[4] = x2; a[5] = y2;
        int n = 3;
        for (int lado = 0; lado < 4 && n > 0; lado++) {
            int m = 0;
            for (int i = 0; i < n; i++) {
                float px = a[i * 2], py = a[i * 2 + 1];
                float qx = a[((i + 1) % n) * 2], qy = a[((i + 1) % n) * 2 + 1];
                float dp = lado == 0 ? px : lado == 1 ? w - px : lado == 2 ? py : h - py;
                float dq = lado == 0 ? qx : lado == 1 ? w - qx : lado == 2 ? qy : h - qy;
                if (dp >= 0) { b[m * 2] = px; b[m * 2 + 1] = py; m++; }
                if ((dp >= 0) != (dq >= 0) && m < 15) {
                    float f = dp / (dp - dq);
                    b[m * 2] = px + (qx - px) * f; b[m * 2 + 1] = py + (qy - py) * f; m++;
                }
            }
            float[] t = a; a = b; b = t;
            n = m;
        }
        double s = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            s += a[i * 2] * a[j * 2 + 1] - a[j * 2] * a[i * 2 + 1];
        }
        return Math.abs(s) * 0.5;
    }

    /** Telas inteiras pintadas no último quadro (1 = cada pixel uma vez). */
    float telasPintadas() {
        return larguraTela > 0 && alturaTela > 0 ? (float)(areaPintada / ((double)larguraTela * alturaTela)) : 0f;
    }

    private void vertice(float x, float y, float u, float v, float r, float g, float bl, float al, float tipo) {
        if (alfaGlobal != 1f) {
            r *= alfaGlobal; g *= alfaGlobal; bl *= alfaGlobal; al *= alfaGlobal;
        }
        float[] o = dados;
        int k = posicao;
        o[k] = a * x + c * y + e;
        o[k + 1] = b * x + d * y + f;
        area(o[k], o[k + 1]);
        o[k + 2] = u;
        o[k + 3] = v;
        o[k + 4] = r;
        o[k + 5] = g;
        o[k + 6] = bl;
        o[k + 7] = al;
        o[k + 8] = tipo;
        posicao = k + FLOATS;
        vertices++;
    }

    private void reservar(int n) {
        if (vertices + n > MAX_VERTICES) {
            descarregar();
            executar();
        }
    }

    /** Dois triângulos; cor ARGB sem multiplicar (aqui é multiplicada pelo alfa). */
    private void quad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                      float u0, float v0, float u1, float v1, int argb, float tipo) {
        float al = (argb >>> 24) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f * al, g = ((argb >> 8) & 0xFF) / 255f * al, bl = (argb & 0xFF) / 255f * al;
        if (nPlanos > 0 && quadRecortado(x0, y0, x1, y1, x2, y2, x3, y3, u0, v0, u1, v1, r, g, bl, al, tipo)) return;
        reservar(6);
        vertice(x0, y0, u0, v0, r, g, bl, al, tipo);
        vertice(x1, y1, u1, v0, r, g, bl, al, tipo);
        vertice(x2, y2, u1, v1, r, g, bl, al, tipo);
        vertice(x0, y0, u0, v0, r, g, bl, al, tipo);
        vertice(x2, y2, u1, v1, r, g, bl, al, tipo);
        vertice(x3, y3, u0, v1, r, g, bl, al, tipo);
    }

    /** Polígono do quadrilátero recortado: x, y (tela) e u, v de cada canto, nos dois vetores. */
    private final float[] corteA = new float[(4 + MAX_PLANOS) * 4], corteB = new float[(4 + MAX_PLANOS) * 4];

    /**
     * Com recorte: o quadrilátero já sai cortado nos semiplanos, para a placa de vídeo pintar só
     * o pedaço que aparece. Pelo shader (que descarta pixel a pixel) ela pagava a imagem inteira:
     * cada painel da página do Ultra desenha a imagem do tamanho da página e mostra uma fatia,
     * e eram 30 telas pintadas por quadro. As cores e o uv variam em linha reta no quadrilátero,
     * então o corte é exato. False se nenhum semiplano corta (o caminho de sempre desenha).
     */
    private boolean quadRecortado(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                                  float u0, float v0, float u1, float v1, float r, float g, float bl, float al, float tipo) {
        float[] p = corteA, q = corteB;
        canto(p, 0, x0, y0, u0, v0);
        canto(p, 1, x1, y1, u1, v0);
        canto(p, 2, x2, y2, u1, v1);
        canto(p, 3, x3, y3, u0, v1);
        int n = 4;
        boolean cortou = false;
        for (int k = 0; k < nPlanos && n >= 3; k++) {
            float pa = recorte[k * 3], pb = recorte[k * 3 + 1], pc = recorte[k * 3 + 2];
            int m = 0;
            boolean tudoDentro = true;
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                float di = pa * p[i * 4] + pb * (alturaTela - p[i * 4 + 1]) + pc;
                float dj = pa * p[j * 4] + pb * (alturaTela - p[j * 4 + 1]) + pc;
                if (di >= 0f) {
                    System.arraycopy(p, i * 4, q, m * 4, 4);
                    m++;
                } else {
                    tudoDentro = false;
                }
                if ((di >= 0f) != (dj >= 0f)) {
                    float t = di / (di - dj);
                    for (int w = 0; w < 4; w++) q[m * 4 + w] = p[i * 4 + w] + (p[j * 4 + w] - p[i * 4 + w]) * t;
                    m++;
                }
            }
            if (tudoDentro) continue;
            cortou = true;
            float[] troca = p; p = q; q = troca;
            n = m;
        }
        if (!cortou) return false;
        if (n < 3) return true;
        reservar((n - 2) * 3);
        if (alfaGlobal != 1f) {
            r *= alfaGlobal; g *= alfaGlobal; bl *= alfaGlobal; al *= alfaGlobal;
        }
        for (int i = 1; i < n - 1; i++) {
            verticeTela(p[0], p[1], p[2], p[3], r, g, bl, al, tipo);
            verticeTela(p[i * 4], p[i * 4 + 1], p[i * 4 + 2], p[i * 4 + 3], r, g, bl, al, tipo);
            verticeTela(p[(i + 1) * 4], p[(i + 1) * 4 + 1], p[(i + 1) * 4 + 2], p[(i + 1) * 4 + 3], r, g, bl, al, tipo);
        }
        return true;
    }

    private void canto(float[] p, int i, float x, float y, float u, float v) {
        p[i * 4] = a * x + c * y + e;
        p[i * 4 + 1] = b * x + d * y + f;
        p[i * 4 + 2] = u;
        p[i * 4 + 3] = v;
    }

    /** Vértice já em pixels da tela (sem a transformação), com a cor pronta. */
    private void verticeTela(float x, float y, float u, float v, float r, float g, float bl, float al, float tipo) {
        float[] o = dados;
        int k = posicao;
        o[k] = x;
        o[k + 1] = y;
        area(x, y);
        o[k + 2] = u;
        o[k + 3] = v;
        o[k + 4] = r;
        o[k + 5] = g;
        o[k + 6] = bl;
        o[k + 7] = al;
        o[k + 8] = tipo;
        posicao = k + FLOATS;
        vertices++;
    }

    private void retangulo(float l, float t, float r, float bt, float u0, float v0, float u1, float v1, int argb, float tipo) {
        quad(l, t, r, t, r, bt, l, bt, u0, v0, u1, v1, argb, tipo);
    }

    // ---- Para o CanvasGl: estado vindo do Canvas e triângulos com uv ----

    /** Transparência de tudo o que vier (as camadas com alfa do Canvas), de 0 a 1. */
    void alfa(float a) {
        alfaGlobal = Math.max(0f, Math.min(1f, a));
    }

    /** O recorte de agora: {@code n} semiplanos já em pixels da janela (null ou 0: sem recorte). */
    void recorte(float[] planos, int n) {
        if (planos == recorte && n == nPlanos) return;
        // O mesmo recorte noutro vetor (cada clipRect faz o seu): não fecha o lote.
        int m = planos == null ? 0 : Math.min(n, MAX_PLANOS);
        if (m == nPlanos && planos != null && recorte != null) {
            boolean igual = true;
            for (int i = 0; i < m * 3 && igual; i++) igual = planos[i] == recorte[i];
            if (igual) return;
        }
        descarregar();
        recorte = planos;
        nPlanos = planos == null ? 0 : Math.min(n, MAX_PLANOS);
    }

    /** Cores invertidas (a camada do impact frame). */
    void inverter(boolean sim) {
        if (sim) beginInvert(); else endInvert();
    }

    /** Liga a textura (ou o degradê) dos próximos triângulos. */
    void textura(int id) {
        usar(id);
    }

    /** Um triângulo com uv; cor ARGB sem multiplicar. */
    void triangulo(float x0, float y0, float u0, float v0, float x1, float y1, float u1, float v1,
                   float x2, float y2, float u2, float v2, int argb, float tipo) {
        float al = (argb >>> 24) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f * al, g = ((argb >> 8) & 0xFF) / 255f * al, bl = (argb & 0xFF) / 255f * al;
        reservar(3);
        vertice(x0, y0, u0, v0, r, g, bl, al, tipo);
        vertice(x1, y1, u1, v1, r, g, bl, al, tipo);
        vertice(x2, y2, u2, v2, r, g, bl, al, tipo);
    }

    /** Retângulo com uv de (u0, v0) a (u1, v1). */
    void retanguloUv(float l, float t, float r, float b, float u0, float v0, float u1, float v1, int argb, float tipo) {
        retangulo(l, t, r, b, u0, v0, u1, v1, argb, tipo);
    }

    /**
     * Forma qualquer (côncava, com buracos, que se cruza) pelo stencil: entre
     * {@link #comecarStencil} e {@link #cobrirStencil} vão os leques de cada contorno (só marcam);
     * depois, o que cobre a forma inteira sai só onde ficou marcado, e a marca é apagada.
     */
    void comecarStencil(boolean parImpar) {
        descarregar();
        stencil = parImpar ? 2 : 1;
    }

    void cobrirStencil() {
        descarregar();
        stencil = 3;
    }

    void terminarStencil() {
        descarregar();
        stencil = 0;
    }

    // Degradês: uma linha de 256 pixels por combinação de cores, todas numa textura só.
    private static final int LINHAS_DEGRADE = 128;
    private final java.util.HashMap<String, Integer> degrades = new java.util.HashMap<>();
    private final StringBuilder chaveDegrade = new StringBuilder();
    private final java.nio.ByteBuffer linhaDegrade = java.nio.ByteBuffer.allocateDirect(256 * 4);

    private static int novaTextura(int largura, int altura) {
        int[] id = new int[1];
        GLES30.glGenTextures(1, id, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id[0]);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, largura, altura, 0, GLES30.GL_RGBA,
            GLES30.GL_UNSIGNED_BYTE, null);
        return id[0];
    }

    /**
     * O tipo do vértice para o degradê (4 linear, 5 radial, com a linha dele na fração). A linha
     * é feita na primeira vez (as cores, já com o alfa multiplicado).
     */
    /** A linha de cada degradê pelo próprio objeto (os do HUD são campos): sem montar a chave por desenho. */
    private final java.util.IdentityHashMap<Degrade.Dados, Integer> linhaDoObjeto = new java.util.IdentityHashMap<>();

    float tipoDegrade(Degrade.Dados dados) {
        Integer pronta = linhaDoObjeto.get(dados);
        if (pronta != null) return (dados.radial() ? 5f : 4f) + (pronta + 0.5f) / LINHAS_DEGRADE * 0.99f;
        int[] cores = dados.cores();
        float[] pos = dados.posicoes();
        chaveDegrade.setLength(0);
        for (int cor : cores) chaveDegrade.append(cor).append(',');
        chaveDegrade.append('|');
        if (pos != null) for (float p : pos) chaveDegrade.append(p).append(',');
        String chave = chaveDegrade.toString();
        Integer linha = degrades.get(chave);
        if (linha == null) {
            if (degrades.size() >= LINHAS_DEGRADE) {
                // Cheia: o que já foi gravado sai antes de as linhas serem reaproveitadas.
                descarregar();
                executar();
                degrades.clear();
                linhaDoObjeto.clear();
            }
            linha = degrades.size();
            degrades.put(chave, linha);
            escreverDegrade(cores, pos, linha);
        }
        if (linhaDoObjeto.size() > 512) linhaDoObjeto.clear();
        linhaDoObjeto.put(dados, linha);
        float q = (linha + 0.5f) / LINHAS_DEGRADE * 0.99f;
        return (dados.radial() ? 5f : 4f) + q;
    }

    private void escreverDegrade(int[] cores, float[] pos, int linha) {
        int n = 256;
        java.nio.ByteBuffer px = linhaDegrade;
        px.clear();
        for (int i = 0; i < n; i++) {
            float t = i / (float)(n - 1);
            int k = 0;
            float t0 = 0f, t1 = 1f;
            int m = cores.length;
            // O trecho do degradê onde t cai.
            for (int j = 0; j < m - 1; j++) {
                float pa = pos != null ? pos[j] : j / (float)(m - 1);
                float pb = pos != null ? pos[j + 1] : (j + 1) / (float)(m - 1);
                if (t <= pb || j == m - 2) {
                    k = j;
                    t0 = pa;
                    t1 = pb;
                    break;
                }
            }
            float f = m < 2 || t1 <= t0 ? 0f : Math.max(0f, Math.min(1f, (t - t0) / (t1 - t0)));
            int ca = cores[Math.min(k, m - 1)], cb = cores[Math.min(k + 1, m - 1)];
            float al = mistura(ca >>> 24, cb >>> 24, f) / 255f;
            px.put((byte)Math.round(mistura((ca >> 16) & 0xFF, (cb >> 16) & 0xFF, f) * al));
            px.put((byte)Math.round(mistura((ca >> 8) & 0xFF, (cb >> 8) & 0xFF, f) * al));
            px.put((byte)Math.round(mistura(ca & 0xFF, cb & 0xFF, f) * al));
            px.put((byte)Math.round(al * 255f));
        }
        px.position(0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, atlasDegrade);
        GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, linha, n, 1, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, px);
    }

    /**
     * Texto grande (a onomatopeia da página do Ultra, o SHINKAI do corte, o nome do Ultra): numa
     * textura só dele, fora da dos textos. Uns poucos desses enchiam a textura compartilhada, e
     * cada vez que ela recomeçava todos os textos da tela eram refeitos no mesmo quadro. Ficam os
     * {@link #MAX_TEXTOS_PROPRIOS} mais novos.
     */
    private static final int TEXTO_PROPRIO = 96 * 1024, MAX_TEXTOS_PROPRIOS = 24;
    private final java.util.ArrayDeque<int[]> textosProprios = new java.util.ArrayDeque<>();
    private final java.util.HashSet<Integer> texturasDeTextoVivas = new java.util.HashSet<>();

    /** O lugar {u0, v0, u1, v1, id} de um texto com textura própria. */
    private float[] guardarTextoProprio(Bitmap bmp) {
        int w = bmp.getWidth(), h = bmp.getHeight();
        TesteAutomatico.textoGuardadoFora(w * h);
        int[] id = new int[1];
        GLES30.glGenTextures(1, id, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id[0]);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0);
        bmp.recycle();
        // O bind acima não atrapalha o lote: quem descarrega liga a textura dele de novo.
        int bytes = w * h * 4;
        bytesTexturas += bytes;
        textosProprios.addLast(new int[] {id[0], bytes});
        texturasDeTextoVivas.add(id[0]);
        while (textosProprios.size() > MAX_TEXTOS_PROPRIOS) {
            int[] velho = textosProprios.removeFirst();
            // Algum desenho gravado pode usar esta textura: sai antes.
            descarregar();
            executar();
            if (velho[0] == texturaAtual) texturaAtual = 0;
            GLES30.glDeleteTextures(1, velho, 0);
            texturasDeTextoVivas.remove(velho[0]);
            bytesTexturas -= velho[1];
        }
        return new float[] {0f, 0f, 1f, 1f, id[0]};
    }

    /** O lugar guardado de um texto ainda vale (a textura dele existe). */
    boolean textoVale(float[] uv, int geracao) {
        return uv.length > 4 ? texturasDeTextoVivas.contains((int)uv[4]) : geracao == geracaoTexto;
    }

    // Textos: todos numa textura só (prateleiras); cheia, recomeça (os textos são refeitos).
    private int ladoTexto = 2048;
    private int prateleiraX, prateleiraY, alturaPrateleira;
    /** Muda quando a textura dos textos recomeça: quem guardou um lugar nela faz de novo. */
    int geracaoTexto;

    /**
     * Guarda o bitmap de um texto na textura dos textos e devolve o lugar dele ({u0, v0, u1, v1}),
     * ou null se não cabe. O bitmap é solto.
     */
    float[] guardarTexto(Bitmap bmp) {
        int w = bmp.getWidth(), h = bmp.getHeight();
        if ((w + 2) * (h + 2) > TEXTO_PROPRIO && w <= maxTextura && h <= maxTextura) return guardarTextoProprio(bmp);
        TesteAutomatico.textoGuardado((w + 2) * (h + 2));
        if (w + 2 > ladoTexto || h + 2 > ladoTexto) {
            bmp.recycle();
            return null;
        }
        if (prateleiraX + w + 2 > ladoTexto) {
            prateleiraX = 0;
            prateleiraY += alturaPrateleira;
            alturaPrateleira = 0;
        }
        if (prateleiraY + h + 2 > ladoTexto) {
            // Cheia: o que já foi gravado sai com os textos de agora, e ela recomeça.
            descarregar();
            executar();
            prateleiraX = prateleiraY = alturaPrateleira = 0;
            geracaoTexto++;
            TesteAutomatico.atlasDeTextoCheio();
            textos.clear();
            ultimoDoTexto.clear();
        }
        int x = prateleiraX + 1, y = prateleiraY + 1;
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, atlasTexto);
        GLUtils.texSubImage2D(GLES30.GL_TEXTURE_2D, 0, x, y, bmp);
        bmp.recycle();
        prateleiraX += w + 2;
        alturaPrateleira = Math.max(alturaPrateleira, h + 2);
        float k = 1f / ladoTexto;
        return new float[] {x * k, y * k, (x + w) * k, (y + h) * k};
    }

    /**
     * A forma de um texto guardado pintada por um degradê linear: {@code t0..t3} é a posição no
     * degradê em cada canto (esquerda-cima, direita-cima, direita-baixo, esquerda-baixo).
     */
    void desenharTextoDegrade(float[] uv, float l, float t, float r, float b,
                              float t0, float t1, float t2, float t3, float tipoDegrade, int alpha) {
        float tipo = 8f + (tipoDegrade - (float)Math.floor(tipoDegrade));
        float al = alpha / 255f * alfaGlobal;
        reservar(6);
        verticeCru(l, t, uv[0], uv[1], t0, al, tipo);
        verticeCru(r, t, uv[2], uv[1], t1, al, tipo);
        verticeCru(r, b, uv[2], uv[3], t2, al, tipo);
        verticeCru(l, t, uv[0], uv[1], t0, al, tipo);
        verticeCru(r, b, uv[2], uv[3], t2, al, tipo);
        verticeCru(l, b, uv[0], uv[3], t3, al, tipo);
    }

    /** Vértice com o vermelho levando um número (não uma cor) e o alfa já pronto. */
    private void verticeCru(float x, float y, float u, float v, float vermelho, float al, float tipo) {
        float[] o = dados;
        int k = posicao;
        o[k] = a * x + c * y + e;
        o[k + 1] = b * x + d * y + f;
        area(o[k], o[k + 1]);
        o[k + 2] = u;
        o[k + 3] = v;
        o[k + 4] = vermelho;
        o[k + 5] = 0f;
        o[k + 6] = 0f;
        o[k + 7] = al;
        o[k + 8] = tipo;
        posicao = k + FLOATS;
        vertices++;
    }

    /** Um texto guardado ({@link #guardarTexto}) no retângulo, sem trocar de textura. */
    void desenharTexto(float[] uv, float l, float t, float r, float b, int argb) {
        if (uv.length > 4) {
            // Textura própria: desenhado como imagem, sem filtro (o texto já tem as cores dele).
            filtro(null);
            usar((int)uv[4]);
            retangulo(l, t, r, b, 0f, 0f, 1f, 1f, argb, 1f);
            return;
        }
        retangulo(l, t, r, b, uv[0], uv[1], uv[2], uv[3], argb, 7f);
    }

    private static float mistura(float a, float b, float f) {
        return a + (b - a) * f;
    }

    // ---- Texturas ----

    /**
     * A textura do bitmap, subindo na primeira vez. {@code soltarBitmap}: depois de subir, o bitmap
     * da memória comum é reciclado (as folhas dos lutadores: só a placa de vídeo guarda). Um
     * bitmap da placa de vídeo (HARDWARE) sobe por uma cópia e continua como está. Null se não
     * deu (bitmap já reciclado sem textura).
     */
    Textura textura(Bitmap bitmap, boolean soltarBitmap) {
        Textura t = texturas.get(bitmap);
        if (t != null) return t;
        if (bitmap.isRecycled()) return null;
        int largura = bitmap.getWidth(), altura = bitmap.getHeight();
        boolean hardware = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.getConfig() == Bitmap.Config.HARDWARE;
        Bitmap fonte = hardware ? bitmap.copy(Bitmap.Config.ARGB_8888, false) : bitmap;
        if (fonte == null) return null;
        if (largura > maxTextura || altura > maxTextura) {
            // Maior que a textura do aparelho: sobe reduzida (as coordenadas são frações do todo).
            float k = Math.min(maxTextura / (float)largura, maxTextura / (float)altura);
            Bitmap menor = Bitmap.createScaledBitmap(fonte, Math.max(1, (int)(largura * k)), Math.max(1, (int)(altura * k)), true);
            if (fonte != bitmap) fonte.recycle();
            fonte = menor;
        }
        int[] id = new int[1];
        GLES30.glGenTextures(1, id, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id[0]);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, fonte, 0);
        long bytes = (long)fonte.getWidth() * fonte.getHeight() * 4L;
        if (fonte != bitmap) fonte.recycle();
        if (soltarBitmap && !hardware) bitmap.recycle();
        // O bind acima não atrapalha o lote: quem descarrega liga a textura dele de novo.
        TesteAutomatico.subiu(bytes);
        t = new Textura(bitmap, esquecidas, id[0], largura, altura, bytes);
        texturas.put(bitmap, t);
        bytesTexturas += bytes;
        return t;
    }

    boolean temTextura(Bitmap bitmap) {
        return texturas.containsKey(bitmap);
    }

    /** Apaga a textura do bitmap (a folha saiu do cache). */
    void soltar(Bitmap bitmap) {
        Textura t = texturas.remove(bitmap);
        if (t != null) apagar(t);
    }

    private void apagar(Textura t) {
        if (t.id == 0) return;
        // Algum desenho gravado pode usar esta textura: sai antes.
        descarregar();
        executar();
        if (t.id == texturaAtual) {
            descarregar();
            texturaAtual = 0;
        }
        GLES30.glDeleteTextures(1, new int[] {t.id}, 0);
        t.id = 0;
        bytesTexturas -= t.bytes;
    }

    /** Sobe a imagem agora (antes de ela aparecer); false se ela já estava na placa. */
    boolean subirImagem(RenderImage image) {
        if (!(image instanceof AndroidRenderAssets.AndroidRenderImage)) return false;
        if (texturas.get(((AndroidRenderAssets.AndroidRenderImage)image).bruto()) != null) return false;
        return texturaDe(image) != null;
    }

    private Textura texturaDe(RenderImage image) {
        if (!(image instanceof AndroidRenderAssets.AndroidRenderImage)) return null;
        AndroidRenderAssets.AndroidRenderImage im = (AndroidRenderAssets.AndroidRenderImage)image;
        // Já subiu (e talvez foi solta): a textura é achada pelo mesmo Bitmap.
        Textura t = texturas.get(im.bruto());
        if (t != null) return t;
        return textura(im.bitmap(), im.soltaNoOpenGl());
    }

    /** Recorte {@code s*} (pixels do bitmap) esticado no retângulo de destino, com o filtro atual. */
    void desenharTextura(Textura t, float sl, float st, float sr, float sb,
                         float l, float top, float r, float bt, int alpha) {
        if (t == null || alpha <= 0) return;
        usar(t.id);
        float w = t.largura, h = t.altura;
        retangulo(l, top, r, bt, sl / w, st / h, sr / w, sb / h, (alpha << 24) | 0xFFFFFF, 1f);
    }

    /** Um bitmap qualquer (as folhas dos lutadores), subindo se ainda não subiu. */
    void desenharBitmap(Bitmap bitmap, boolean soltarBitmap, float sl, float st, float sr, float sb,
                        float l, float top, float r, float bt, int alpha) {
        desenharTextura(textura(bitmap, soltarBitmap), sl, st, sr, sb, l, top, r, bt, alpha);
    }

    // ---- RenderCanvas ----

    @Override public void fillRect(float l, float t, float r, float bt, int argb) {
        if ((argb >>> 24) == 0) return;
        retangulo(l, t, r, bt, 0f, 0f, 0f, 0f, argb, 0f);
    }

    @Override public void fillPolygon(float[] xy, int argb) {
        int n = xy.length / 2;
        if ((argb >>> 24) == 0 || n < 3) return;
        float al = (argb >>> 24) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f * al, g = ((argb >> 8) & 0xFF) / 255f * al, bl = (argb & 0xFF) / 255f * al;
        if (n == 3) {
            reservar(3);
            vertice(xy[0], xy[1], 0f, 0f, r, g, bl, al, 0f);
            vertice(xy[2], xy[3], 0f, 0f, r, g, bl, al, 0f);
            vertice(xy[4], xy[5], 0f, 0f, r, g, bl, al, 0f);
            return;
        }
        // Leque a partir do centro: certo para convexos e para estrelas em volta do centro.
        float cx = 0f, cy = 0f;
        for (int i = 0; i < n; i++) { cx += xy[i * 2]; cy += xy[i * 2 + 1]; }
        cx /= n; cy /= n;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            reservar(3);
            vertice(cx, cy, 0f, 0f, r, g, bl, al, 0f);
            vertice(xy[i * 2], xy[i * 2 + 1], 0f, 0f, r, g, bl, al, 0f);
            vertice(xy[j * 2], xy[j * 2 + 1], 0f, 0f, r, g, bl, al, 0f);
        }
    }

    /** {@code n} triângulos soltos ({x0, y0, x1, y1, x2, y2} cada) numa cor. */
    void fillTriangles(float[] xy, int n, int argb) {
        if ((argb >>> 24) == 0 || n <= 0) return;
        float al = (argb >>> 24) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f * al, g = ((argb >> 8) & 0xFF) / 255f * al, bl = (argb & 0xFF) / 255f * al;
        for (int i = 0; i < n; i++) {
            reservar(3);
            int k = i * 6;
            vertice(xy[k], xy[k + 1], 0f, 0f, r, g, bl, al, 0f);
            vertice(xy[k + 2], xy[k + 3], 0f, 0f, r, g, bl, al, 0f);
            vertice(xy[k + 4], xy[k + 5], 0f, 0f, r, g, bl, al, 0f);
        }
    }

    @Override public void strokePolygon(float[] xy, float width, int argb) {
        for (int i = 0; i + 1 < xy.length; i += 2) {
            int j = (i + 2) % xy.length;
            drawLine(xy[i], xy[i + 1], xy[j], xy[j + 1], width, argb);
        }
    }

    @Override public void fillCircle(float cx, float cy, float radius, int argb) {
        if (radius <= 0f || (argb >>> 24) == 0) return;
        retangulo(cx - radius, cy - radius, cx + radius, cy + radius, -1f, -1f, 1f, 1f, argb, 2f);
    }

    @Override public void strokeCircle(float cx, float cy, float radius, float width, int argb) {
        if ((argb >>> 24) == 0 || width <= 0f) return;
        // Anel no shader (tipo 6, a fração é o raio de dentro): um retângulo em vez de 24 traços.
        float h = width * 0.5f, fora = radius + h, dentro = Math.max(0f, radius - h) / fora;
        retanguloUv(cx - fora, cy - fora, cx + fora, cy + fora, -1f, -1f, 1f, 1f, argb, 6f + Math.min(0.999f, dentro));
    }

    @Override public void drawLine(float x1, float y1, float x2, float y2, float width, int argb) {
        if ((argb >>> 24) == 0) return;
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float)Math.sqrt(dx * dx + dy * dy);
        if (len <= 0f) return;
        float nx = -dy / len * width * 0.5f, ny = dx / len * width * 0.5f;
        quad(x1 + nx, y1 + ny, x2 + nx, y2 + ny, x2 - nx, y2 - ny, x1 - nx, y1 - ny, 0f, 0f, 0f, 0f, argb, 0f);
    }

    @Override public void drawImage(RenderImage image, float l, float t, float r, float bt, int alpha) {
        Textura tx = alpha > 0 ? texturaDe(image) : null;
        if (tx == null) return;
        filtro(null);
        desenharTextura(tx, 0f, 0f, tx.largura, tx.altura, l, t, r, bt, alpha);
    }

    @Override
    public void drawImageRegion(RenderImage image, float sl, float st, float sr, float sb,
                                float l, float t, float r, float bt, int alpha) {
        Textura tx = alpha > 0 ? texturaDe(image) : null;
        if (tx == null) return;
        filtro(null);
        desenharTextura(tx, sl, st, sr, sb, l, t, r, bt, alpha);
    }

    @Override public boolean desenhaFaixas() { return true; }

    @Override public void drawImageStrip(RenderImage image, float[] xy, float[] uv, int linhas, int alpha) {
        if (linhas < 2 || alpha <= 0) return;
        Textura tx = texturaDe(image);
        if (tx == null) return;
        filtro(null);
        usar(tx.id);
        if (!tx.repetir) {
            // A faixa repete a imagem (o chão, o pano): a textura passa a repetir.
            tx.repetir = true;
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tx.id);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);
        }
        float w = tx.largura, h = tx.altura, al = alpha / 255f;
        // Cada linha traz a esquerda e a direita; entre duas linhas, um quadrilátero.
        for (int i = 0; i < linhas - 1; i++) {
            int p = i * 4, q = p + 4;
            reservar(6);
            vertice(xy[p], xy[p + 1], uv[p] / w, uv[p + 1] / h, al, al, al, al, 1f);
            vertice(xy[p + 2], xy[p + 3], uv[p + 2] / w, uv[p + 3] / h, al, al, al, al, 1f);
            vertice(xy[q + 2], xy[q + 3], uv[q + 2] / w, uv[q + 3] / h, al, al, al, al, 1f);
            vertice(xy[p], xy[p + 1], uv[p] / w, uv[p + 1] / h, al, al, al, al, 1f);
            vertice(xy[q + 2], xy[q + 3], uv[q + 2] / w, uv[q + 3] / h, al, al, al, al, 1f);
            vertice(xy[q], xy[q + 1], uv[q] / w, uv[q + 1] / h, al, al, al, al, 1f);
        }
    }

    @Override public void fillRadialGlow(float cx, float cy, float radius, int argbCenter) {
        if (radius <= 0f || (argbCenter >>> 24) == 0) return;
        retangulo(cx - radius, cy - radius, cx + radius, cy + radius, -1f, -1f, 1f, 1f, argbCenter, 3f);
    }

    @Override public void fillVerticalGradient(float l, float t, float r, float bt, int argbTop, int argbBottom) {
        float at = (argbTop >>> 24) / 255f, ab = (argbBottom >>> 24) / 255f;
        if (at == 0f && ab == 0f) return;
        float rt = ((argbTop >> 16) & 0xFF) / 255f * at, gt = ((argbTop >> 8) & 0xFF) / 255f * at, btop = (argbTop & 0xFF) / 255f * at;
        float rb = ((argbBottom >> 16) & 0xFF) / 255f * ab, gb = ((argbBottom >> 8) & 0xFF) / 255f * ab, bb = (argbBottom & 0xFF) / 255f * ab;
        reservar(6);
        vertice(l, t, 0f, 0f, rt, gt, btop, at, 0f);
        vertice(r, t, 0f, 0f, rt, gt, btop, at, 0f);
        vertice(r, bt, 0f, 0f, rb, gb, bb, ab, 0f);
        vertice(l, t, 0f, 0f, rt, gt, btop, at, 0f);
        vertice(r, bt, 0f, 0f, rb, gb, bb, ab, 0f);
        vertice(l, bt, 0f, 0f, rb, gb, bb, ab, 0f);
    }

    @Override public void fillOval(float l, float t, float r, float bt, int argb) {
        if ((argb >>> 24) == 0) return;
        retangulo(l, t, r, bt, -1f, -1f, 1f, 1f, argb, 2f);
    }

    // ---- Texto: cada texto desenhado vira uma textura (guardadas as últimas) ----

    private final Paint textoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    {
        textoPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textoPaint.setTextAlign(Paint.Align.LEFT);
    }
    /** Um texto pronto: o lugar dele na textura dos textos e o ponto (x, linha de base), em pixels. */
    private static final class Texto {
        float[] uv;
        int largura, altura;
        float origemX, origemY, escala;
        /** Tamanho da fonte em que foi feito (o reaproveitado em outro tamanho estica por ele). */
        float base;
        /** A escala pedida (antes do teto de pixels) e o contorno com que foi feito. */
        float escalaPedida, traco;
    }
    private final java.util.HashMap<String, Texto> textos = new java.util.HashMap<>();
    /** Como no CanvasGl: no máximo tantos textos feitos por quadro; os outros reaproveitam o último. */
    private static final int NOVOS_POR_QUADRO = 6;
    int textosNovosRc;
    private final java.util.HashMap<String, Texto> ultimoDoTexto = new java.util.HashMap<>();

    @Override
    public void drawText(String text, float x, float y, float size, int fillArgb, int strokeArgb, float strokeWidth, boolean italic) {
        if (text == null || text.isEmpty() || size <= 0f) return;
        // O tamanho num degrau (um quarto de oitava) esticado por kz no desenho, e a transparência
        // comum ao preenchimento e ao contorno na hora de desenhar: texto que pulsa ou some aos
        // poucos não vira um bitmap novo a cada quadro.
        float base = (float)Math.pow(2.0, Math.round(Math.log(Math.max(1f, size)) / Math.log(2) * 4.0) / 4.0);
        float kz = size / base;
        int alfa = fillArgb >>> 24;
        boolean alfaComum = strokeWidth <= 0f || (strokeArgb >>> 24) == alfa;
        if (alfaComum) {
            fillArgb |= 0xFF000000;
            strokeArgb |= 0xFF000000;
        }
        // Na resolução em que vai aparecer: o maior eixo da escala atual (a faixa da legenda que abre
        // esticando só na largura não muda a altura das letras), em degraus de um quarto de oitava.
        float escala = Math.max(Math.max((float)Math.hypot(a, c), (float)Math.hypot(b, d)),
            Math.max((float)Math.hypot(a, b), (float)Math.hypot(c, d))) * kz;
        escala = (float)Math.pow(2.0, Math.ceil(Math.log(Math.max(0.25f, escala)) / Math.log(2) * 4.0) / 4.0);
        // O contorno no tamanho do degrau, em degraus de 1/8 de oitava (como no CanvasGl).
        float traco = strokeWidth <= 0f ? 0f
            : (float)Math.pow(2.0, Math.round(Math.log(strokeWidth / kz) / Math.log(2) * 8.0) / 8.0);
        String chave = text + '\u0000' + base + '\u0000' + fillArgb + '\u0000' + strokeArgb + '\u0000'
            + traco + '\u0000' + italic + '\u0000' + escala;
        Texto t = textos.get(chave);
        if (t != null && !textoVale(t.uv, geracaoTexto)) t = null;
        float k = kz;
        if (t == null) {
            if (falhos.contains(chave)) return;
            String solta = text + '\u0000' + fillArgb + '\u0000' + strokeArgb + '\u0000' + italic;
            Texto ultimo = ultimoDoTexto.get(solta);
            if (ultimo != null && !textoVale(ultimo.uv, geracaoTexto)) ultimo = null;
            if (ultimo != null && ultimo.base == base && ultimo.traco == traco
                && ultimo.escalaPedida >= escala * 0.999f && ultimo.escalaPedida <= escala * 2.01f) {
                // Entra grande e encolhe (o nome do Ultra, a onomatopeia, o FINAL): o bitmap do
                // quadro maior serve aos menores.
                t = ultimo;
            } else if (ultimo != null && textosNovosRc >= NOVOS_POR_QUADRO) {
                t = ultimo;
                k = size / ultimo.base;
            } else {
                // No máximo ~0,4 Mpx e o lado da textura dos textos: a placa de vídeo amplia o resto.
                float pedida = escala;
                float px = estimarPixels(text, base * escala, traco * escala, italic);
                float reduz = px > MAX_PIXELS_TEXTO ? (float)Math.sqrt(MAX_PIXELS_TEXTO / px) : 1f;
                reduz = Math.min(reduz, Math.min(2000f / Math.max(1f, ultimaLargura), 2000f / Math.max(1f, ultimaAltura)));
                if (reduz < 1f) escala *= reduz;
                TesteAutomatico.textoPedido(text);
                t = criarTexto(text, base * escala, fillArgb, strokeArgb, traco * escala, italic);
                if (t == null) {
                    // Não cabe nem reduzido: não tenta de novo a cada quadro.
                    if (falhos.size() > 256) falhos.clear();
                    falhos.add(chave);
                    return;
                }
                t.escala = escala;
                t.escalaPedida = pedida;
                t.traco = traco;
                t.base = base;
                textosNovosRc++;
                textos.put(chave, t);
                if (ultimoDoTexto.size() > 320) ultimoDoTexto.clear();
                ultimoDoTexto.put(solta, t);
            }
        }
        k /= t.escala;
        // Centralizado em x, com a linha de base em y (como no Canvas).
        float l = x - t.origemX * k, top = y - t.origemY * k;
        desenharTexto(t.uv, l, top, l + t.largura * k, top + t.altura * k, alfaComum ? (alfa << 24) | 0xFFFFFF : 0xFFFFFFFF);
    }

    private static final float MAX_PIXELS_TEXTO = 400_000f;
    private final java.util.HashSet<String> falhos = new java.util.HashSet<>();
    private float ultimaLargura, ultimaAltura;

    private float estimarPixels(String text, float size, float strokeWidth, boolean italic) {
        textoPaint.setTextSize(size);
        textoPaint.setTextSkewX(italic ? -0.22f : 0f);
        float largura = textoPaint.measureText(text);
        Paint.FontMetrics fm = textoPaint.getFontMetrics();
        textoPaint.setTextSkewX(0f);
        float margem = strokeWidth + size * 0.25f + 2f;
        ultimaLargura = largura + margem * 2f;
        ultimaAltura = fm.bottom - fm.top + margem * 2f;
        return ultimaLargura * ultimaAltura;
    }

    private Texto criarTexto(String text, float size, int fill, int stroke, float strokeWidth, boolean italic) {
        textoPaint.setTextSize(size);
        textoPaint.setTextSkewX(italic ? -0.22f : 0f);
        float largura = textoPaint.measureText(text);
        Paint.FontMetrics fm = textoPaint.getFontMetrics();
        int margem = (int)Math.ceil(strokeWidth + size * 0.25f) + 2;
        int w = (int)Math.ceil(largura) + margem * 2, h = (int)Math.ceil(fm.bottom - fm.top) + margem * 2;
        if (w <= 0 || h <= 0 || w > maxTextura || h > maxTextura) return null;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        float bx = margem, by = margem - fm.top;
        if (strokeWidth > 0f) {
            textoPaint.setStyle(Paint.Style.STROKE);
            textoPaint.setStrokeWidth(strokeWidth);
            textoPaint.setStrokeJoin(Paint.Join.ROUND);
            textoPaint.setStrokeCap(Paint.Cap.ROUND);
            textoPaint.setColor(stroke);
            cv.drawText(text, bx, by, textoPaint);
        }
        textoPaint.setStyle(Paint.Style.FILL);
        textoPaint.setColor(fill);
        cv.drawText(text, bx, by, textoPaint);
        textoPaint.setTextSkewX(0f);
        float[] uv = guardarTexto(bmp);
        if (uv == null) return null;
        Texto t = new Texto();
        t.uv = uv;
        t.largura = w;
        t.altura = h;
        // A âncora: o meio do texto na horizontal e a linha de base.
        t.origemX = bx + largura / 2f;
        t.origemY = by;
        return t;
    }

    @Override public float measureText(String text, float size, boolean italic) {
        textoPaint.setTextSize(size);
        textoPaint.setTextSkewX(italic ? -0.22f : 0f);
        float w = textoPaint.measureText(text);
        textoPaint.setTextSkewX(0f);
        return w;
    }
}
