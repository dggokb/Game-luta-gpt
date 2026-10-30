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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
    // Tipo 0: cor lisa; 1: imagem; 2: círculo (uv de -1 a 1, borda suave); 3: brilho (do centro
    // para fora, de cheio a transparente). Cores já multiplicadas pelo alfa. O filtro vale só para
    // imagem e trabalha na cor sem o alfa multiplicado, como o ColorMatrix do Android. O recorte
    // são semiplanos em pixels da janela (y para cima).
    private static final String FRAGMENTOS =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "uniform sampler2D uTex;\n" +
        "uniform mat4 uCor; uniform vec4 uCorSoma; uniform bool uFiltro; uniform bool uInverte;\n" +
        "uniform highp vec3 uRecorte[8]; uniform int uNRecorte;\n" +
        "in highp vec2 vUv; in vec4 vCor; in float vTipo;\n" +
        "out vec4 cor;\n" +
        "void main() {\n" +
        "  highp vec3 p = vec3(gl_FragCoord.xy, 1.0);\n" +
        "  for (int i = 0; i < 8; i++) {\n" +
        "    if (i >= uNRecorte) break;\n" +
        "    if (dot(uRecorte[i], p) < 0.0) discard;\n" +
        "  }\n" +
        "  vec4 c;\n" +
        "  if (vTipo < 0.5) { c = vCor; }\n" +
        "  else if (vTipo < 1.5) {\n" +
        "    vec4 t = texture(uTex, vUv);\n" +
        "    if (uFiltro) {\n" +
        "      vec3 s = t.a > 0.0 ? t.rgb / t.a : vec3(0.0);\n" +
        "      vec4 u = clamp(uCor * vec4(s, t.a) + uCorSoma, 0.0, 1.0);\n" +
        "      t = vec4(u.rgb * u.a, u.a);\n" +
        "    }\n" +
        "    c = t * vCor;\n" +
        "  } else {\n" +
        "    float d = length(vUv);\n" +
        "    float a = vTipo < 2.5 ? clamp((1.0 - d) / max(fwidth(d), 1e-4), 0.0, 1.0) : clamp(1.0 - d, 0.0, 1.0);\n" +
        "    c = vCor * a;\n" +
        "  }\n" +
        "  if (uInverte) c.rgb = vec3(c.a) - c.rgb;\n" +
        "  cor = c;\n" +
        "}\n";

    private static final int FLOATS = 9;
    private static final int MAX_VERTICES = 6 * 8192;
    private static final int MAX_PLANOS = 8;
    private final FloatBuffer buffer = ByteBuffer.allocateDirect(MAX_VERTICES * FLOATS * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer();
    private int vertices;
    private int texturaAtual;
    private int programa, uTela, uTex, uCor, uCorSoma, uFiltro, uInverte, uRecorte, uNRecorte, vbo;
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
    private boolean uniformesSujos = true;
    private final float[] matrizCor = new float[16];

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
        uCor = GLES30.glGetUniformLocation(programa, "uCor");
        uCorSoma = GLES30.glGetUniformLocation(programa, "uCorSoma");
        uFiltro = GLES30.glGetUniformLocation(programa, "uFiltro");
        uInverte = GLES30.glGetUniformLocation(programa, "uInverte");
        uRecorte = GLES30.glGetUniformLocation(programa, "uRecorte");
        uNRecorte = GLES30.glGetUniformLocation(programa, "uNRecorte");
        int[] ids = new int[1];
        GLES30.glGenBuffers(1, ids, 0);
        vbo = ids[0];
        GLES30.glGetIntegerv(GLES30.GL_MAX_TEXTURE_SIZE, ids, 0);
        if (ids[0] > 0) maxTextura = ids[0];
        // O contexto é novo: as texturas antigas não existem mais.
        for (Textura t : texturas.values()) t.id = 0;
        texturas.clear();
        textos.clear();
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
        larguraTela = largura;
        alturaTela = altura;
        GLES30.glViewport(0, 0, largura, altura);
        GLES30.glClearColor(0f, 0f, 0f, 1f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        // Texturas de bitmaps que já não existem (cenário e Ultra de lutas passadas).
        java.lang.ref.Reference<? extends Bitmap> ref;
        while ((ref = esquecidas.poll()) != null) apagar((Textura)ref);
        GLES30.glUseProgram(programa);
        GLES30.glUniform2f(uTela, largura, altura);
        GLES30.glUniform1i(uTex, 0);
        a = largura / Arena.VW; b = 0f; c = 0f; d = altura / Arena.VH; e = 0f; f = 0f;
        topo = 0;
        vertices = 0;
        texturaAtual = 0;
        chamadas = 0;
        filtro = null;
        inverte = false;
        recorte = null;
        nPlanos = 0;
        uniformesSujos = true;
    }

    void terminar() {
        descarregar();
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
        uniformesSujos = true;
    }

    /** Tira todo recorte (fora da pilha de save/restore: o desenho dos lutadores). */
    void semRecorte() {
        if (nPlanos == 0) return;
        descarregar();
        recorte = null;
        nPlanos = 0;
        uniformesSujos = true;
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
            uniformesSujos = true;
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
        uniformesSujos = true;
    }

    @Override public void beginInvert() {
        if (inverte) return;
        descarregar();
        inverte = true;
        uniformesSujos = true;
    }

    @Override public void endInvert() {
        if (!inverte) return;
        descarregar();
        inverte = false;
        uniformesSujos = true;
    }

    // ---- Lotes ----

    private void descarregar() {
        if (vertices == 0) return;
        if (uniformesSujos) enviarUniformes();
        buffer.position(0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices * FLOATS * 4, buffer, GLES30.GL_STREAM_DRAW);
        int passo = FLOATS * 4;
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, passo, 0);
        GLES30.glEnableVertexAttribArray(1);
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, passo, 8);
        GLES30.glEnableVertexAttribArray(2);
        GLES30.glVertexAttribPointer(2, 4, GLES30.GL_FLOAT, false, passo, 16);
        GLES30.glEnableVertexAttribArray(3);
        GLES30.glVertexAttribPointer(3, 1, GLES30.GL_FLOAT, false, passo, 32);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texturaAtual);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertices);
        chamadas++;
        vertices = 0;
        buffer.clear();
    }

    private void enviarUniformes() {
        uniformesSujos = false;
        GLES30.glUniform1i(uFiltro, filtro != null ? 1 : 0);
        if (filtro != null) {
            // ColorMatrix é por linhas (4x5); o mat4 do GLSL é por colunas.
            for (int linha = 0; linha < 4; linha++) {
                for (int coluna = 0; coluna < 4; coluna++) matrizCor[coluna * 4 + linha] = filtro[linha * 5 + coluna];
            }
            GLES30.glUniformMatrix4fv(uCor, 1, false, matrizCor, 0);
            GLES30.glUniform4f(uCorSoma, filtro[4] / 255f, filtro[9] / 255f, filtro[14] / 255f, filtro[19] / 255f);
        }
        GLES30.glUniform1i(uInverte, inverte ? 1 : 0);
        GLES30.glUniform1i(uNRecorte, nPlanos);
        if (nPlanos > 0) GLES30.glUniform3fv(uRecorte, nPlanos, recorte, 0);
    }

    private void usar(int textura) {
        if (textura == texturaAtual) return;
        // Só troca de textura entre chamadas; cor lisa e brilhos não usam textura e seguem juntos.
        descarregar();
        texturaAtual = textura;
    }

    private void vertice(float x, float y, float u, float v, float r, float g, float bl, float al, float tipo) {
        buffer.put(a * x + c * y + e).put(b * x + d * y + f).put(u).put(v)
            .put(r).put(g).put(bl).put(al).put(tipo);
        vertices++;
    }

    private void reservar(int n) {
        if (vertices + n > MAX_VERTICES) descarregar();
    }

    /** Dois triângulos; cor ARGB sem multiplicar (aqui é multiplicada pelo alfa). */
    private void quad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                      float u0, float v0, float u1, float v1, int argb, float tipo) {
        float al = (argb >>> 24) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f * al, g = ((argb >> 8) & 0xFF) / 255f * al, bl = (argb & 0xFF) / 255f * al;
        reservar(6);
        vertice(x0, y0, u0, v0, r, g, bl, al, tipo);
        vertice(x1, y1, u1, v0, r, g, bl, al, tipo);
        vertice(x2, y2, u1, v1, r, g, bl, al, tipo);
        vertice(x0, y0, u0, v0, r, g, bl, al, tipo);
        vertice(x2, y2, u1, v1, r, g, bl, al, tipo);
        vertice(x3, y3, u0, v1, r, g, bl, al, tipo);
    }

    private void retangulo(float l, float t, float r, float bt, float u0, float v0, float u1, float v1, int argb, float tipo) {
        quad(l, t, r, t, r, bt, l, bt, u0, v0, u1, v1, argb, tipo);
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
        if (t.id == texturaAtual) {
            descarregar();
            texturaAtual = 0;
        }
        GLES30.glDeleteTextures(1, new int[] {t.id}, 0);
        t.id = 0;
        bytesTexturas -= t.bytes;
    }

    private Textura texturaDe(RenderImage image) {
        if (!(image instanceof AndroidRenderAssets.AndroidRenderImage)) return null;
        return textura(((AndroidRenderAssets.AndroidRenderImage)image).bitmap, false);
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
        // O contorno vira um anel de 24 traços.
        int n = 24;
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
            drawLine(cx + radius * (float)Math.cos(a0), cy + radius * (float)Math.sin(a0),
                cx + radius * (float)Math.cos(a1), cy + radius * (float)Math.sin(a1), width, argb);
        }
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

    private static final int TEXTOS = 48;
    private final Paint textoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    {
        textoPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textoPaint.setTextAlign(Paint.Align.LEFT);
    }
    /** Um texto pronto: a textura e onde fica o ponto (x, linha de base) dentro dela, em pixels. */
    private static final class Texto {
        int id;
        int largura, altura;
        float origemX, origemY, escala;
    }
    private final LinkedHashMap<String, Texto> textos = new LinkedHashMap<String, Texto>(TEXTOS, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Texto> velho) {
            if (size() <= TEXTOS) return false;
            GLES30.glDeleteTextures(1, new int[] {velho.getValue().id}, 0);
            return true;
        }
    };

    @Override
    public void drawText(String text, float x, float y, float size, int fillArgb, int strokeArgb, float strokeWidth, boolean italic) {
        if (text == null || text.isEmpty() || size <= 0f) return;
        // Na resolução em que vai aparecer (a escala atual), arredondada para reaproveitar.
        float escala = (float)Math.sqrt(Math.abs(a * d - b * c));
        escala = Math.max(0.25f, Math.round(escala * 8f) / 8f);
        String chave = text + '\u0000' + size + '\u0000' + fillArgb + '\u0000' + strokeArgb + '\u0000'
            + strokeWidth + '\u0000' + italic + '\u0000' + escala;
        Texto t = textos.get(chave);
        if (t == null) {
            t = criarTexto(text, size * escala, fillArgb, strokeArgb, strokeWidth * escala, italic);
            if (t == null) return;
            t.escala = escala;
            textos.put(chave, t);
        }
        usar(t.id);
        filtro(null);
        float k = 1f / t.escala;
        // Centralizado em x, com a linha de base em y (como no Canvas).
        float l = x - t.origemX * k, top = y - t.origemY * k;
        retangulo(l, top, l + t.largura * k, top + t.altura * k, 0f, 0f, 1f, 1f, 0xFFFFFFFF, 1f);
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
        int[] id = new int[1];
        GLES30.glGenTextures(1, id, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id[0]);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0);
        bmp.recycle();
        Texto t = new Texto();
        t.id = id[0];
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
