package com.gamelutagpt;

import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;

/**
 * {@link RenderCanvas} em OpenGL ES 3 (fase B0 do desenho novo): tudo vira triângulos num buffer
 * só e sai em poucas chamadas, trocando de textura só quando a imagem muda. Brilhos, círculos e
 * degradês são feitos no shader, sem textura. Coordenadas na tela virtual 1280x720, como no
 * Canvas. Texto, recorte e inversão de cores ainda não existem aqui (o cenário quase não usa).
 */
final class GlRenderCanvas implements RenderCanvas {
    /** Imagem para o OpenGL: o bitmap (memória comum) até subir para a placa de vídeo. */
    static final class Imagem implements RenderImage {
        private Bitmap bitmap;
        private final int largura, altura;
        final boolean repetir;
        int textura;
        long bytes;

        Imagem(Bitmap bitmap, boolean repetir) {
            this.bitmap = bitmap;
            this.largura = bitmap.getWidth();
            this.altura = bitmap.getHeight();
            this.repetir = repetir;
        }

        @Override public int width() { return largura; }
        @Override public int height() { return altura; }

        /** Sobe para a placa de vídeo na primeira vez (na thread do OpenGL) e solta o bitmap. */
        int textura() {
            if (textura != 0 || bitmap == null) return textura;
            int[] id = new int[1];
            GLES30.glGenTextures(1, id, 0);
            textura = id[0];
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textura);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            int wrap = repetir ? GLES30.GL_REPEAT : GLES30.GL_CLAMP_TO_EDGE;
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, wrap);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, wrap);
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
            bytes = (long)largura * altura * 4L;
            bitmap.recycle();
            bitmap = null;
            return textura;
        }

        void soltar() {
            if (textura != 0) GLES30.glDeleteTextures(1, new int[] {textura}, 0);
            textura = 0;
            if (bitmap != null) bitmap.recycle();
            bitmap = null;
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
    // para fora, de cheio a transparente). Cores já multiplicadas pelo alfa.
    private static final String FRAGMENTOS =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "uniform sampler2D uTex;\n" +
        "in vec2 vUv; in vec4 vCor; in float vTipo;\n" +
        "out vec4 cor;\n" +
        "void main() {\n" +
        "  if (vTipo < 0.5) { cor = vCor; }\n" +
        "  else if (vTipo < 1.5) { cor = texture(uTex, vUv) * vCor; }\n" +
        "  else {\n" +
        "    float d = length(vUv);\n" +
        "    float a = vTipo < 2.5 ? clamp((1.0 - d) / max(fwidth(d), 1e-4), 0.0, 1.0) : clamp(1.0 - d, 0.0, 1.0);\n" +
        "    cor = vCor * a;\n" +
        "  }\n" +
        "}\n";

    private static final int FLOATS = 9;
    private static final int MAX_VERTICES = 6 * 8192;
    private final FloatBuffer buffer = ByteBuffer.allocateDirect(MAX_VERTICES * FLOATS * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer();
    private int vertices;
    private int texturaAtual;
    private int programa, uTela, uTex, vbo;
    private int larguraTela, alturaTela;
    /** Chamadas de desenho do quadro (para o contador da tela de teste). */
    int chamadas;

    // Transformação afim atual: x' = a*x + c*y + e; y' = b*x + d*y + f.
    private float a = 1f, b, c, d = 1f, e, f;
    private final ArrayDeque<float[]> pilha = new ArrayDeque<>();

    /** Na thread do OpenGL, com o contexto criado. */
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
        int[] ids = new int[1];
        GLES30.glGenBuffers(1, ids, 0);
        vbo = ids[0];
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

    void tamanho(int largura, int altura) {
        larguraTela = largura;
        alturaTela = altura;
    }

    /** Começa o quadro: tela inteira, coordenadas 1280x720. */
    void comecar() {
        GLES30.glViewport(0, 0, larguraTela, alturaTela);
        GLES30.glClearColor(0f, 0f, 0f, 1f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        GLES30.glUseProgram(programa);
        GLES30.glUniform2f(uTela, 1280f, 720f);
        GLES30.glUniform1i(uTex, 0);
        a = 1f; b = 0f; c = 0f; d = 1f; e = 0f; f = 0f;
        pilha.clear();
        vertices = 0;
        texturaAtual = 0;
        chamadas = 0;
    }

    void terminar() {
        descarregar();
    }

    private void descarregar() {
        if (vertices == 0) return;
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

    private void usar(int textura) {
        if (textura == texturaAtual) return;
        // Só troca de textura entre chamadas; cor lisa e brilhos não usam textura e seguem juntos.
        descarregar();
        texturaAtual = textura;
    }

    private void vertice(float x, float y, float u, float v, float r, float g, float bl, float al, float tipo) {
        if (vertices >= MAX_VERTICES) descarregar();
        buffer.put(a * x + c * y + e).put(b * x + d * y + f).put(u).put(v)
            .put(r).put(g).put(bl).put(al).put(tipo);
        vertices++;
    }

    /** Dois triângulos; cor ARGB sem multiplicar (aqui é multiplicada pelo alfa). */
    private void quad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                      float u0, float v0, float u1, float v1, int argb, float tipo) {
        float al = (argb >>> 24) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f * al, g = ((argb >> 8) & 0xFF) / 255f * al, bl = (argb & 0xFF) / 255f * al;
        if (vertices + 6 > MAX_VERTICES) descarregar();
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

    @Override public void save() { pilha.push(new float[] {a, b, c, d, e, f}); }

    @Override public void restore() {
        float[] m = pilha.pop();
        a = m[0]; b = m[1]; c = m[2]; d = m[3]; e = m[4]; f = m[5];
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

    @Override public void clipPolygon(float[] xy) {
        // Fase B0: sem recorte (o cenário quase não recorta).
    }

    @Override public void fillRect(float l, float t, float r, float bt, int argb) {
        if ((argb >>> 24) == 0) return;
        usar(texturaAtual);
        retangulo(l, t, r, bt, 0f, 0f, 0f, 0f, argb, 0f);
    }

    @Override public void fillPolygon(float[] xy, int argb) {
        if ((argb >>> 24) == 0 || xy.length < 6) return;
        float al = (argb >>> 24) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f * al, g = ((argb >> 8) & 0xFF) / 255f * al, bl = (argb & 0xFF) / 255f * al;
        // Leque a partir do primeiro ponto (os polígonos do cenário são convexos).
        for (int i = 2; i + 3 < xy.length; i += 2) {
            if (vertices + 3 > MAX_VERTICES) descarregar();
            vertice(xy[0], xy[1], 0f, 0f, r, g, bl, al, 0f);
            vertice(xy[i], xy[i + 1], 0f, 0f, r, g, bl, al, 0f);
            vertice(xy[i + 2], xy[i + 3], 0f, 0f, r, g, bl, al, 0f);
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
        // Fase B0: o contorno vira um anel de 16 traços.
        int n = 16;
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
        if (alpha <= 0) return;
        usar(((Imagem)image).textura());
        retangulo(l, t, r, bt, 0f, 0f, 1f, 1f, (alpha << 24) | 0xFFFFFF, 1f);
    }

    @Override
    public void drawImageRegion(RenderImage image, float sl, float st, float sr, float sb,
                                float l, float t, float r, float bt, int alpha) {
        if (alpha <= 0) return;
        Imagem im = (Imagem)image;
        usar(im.textura());
        float w = im.width(), h = im.height();
        retangulo(l, t, r, bt, sl / w, st / h, sr / w, sb / h, (alpha << 24) | 0xFFFFFF, 1f);
    }

    @Override public boolean desenhaFaixas() { return true; }

    @Override public void drawImageStrip(RenderImage image, float[] xy, float[] uv, int linhas, int alpha) {
        if (linhas < 2 || alpha <= 0) return;
        Imagem im = (Imagem)image;
        usar(im.textura());
        float w = im.width(), h = im.height(), al = alpha / 255f;
        // Cada linha traz a esquerda e a direita; entre duas linhas, um quadrilátero.
        for (int i = 0; i < linhas - 1; i++) {
            int p = i * 4, q = p + 4;
            if (vertices + 6 > MAX_VERTICES) descarregar();
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
        if (vertices + 6 > MAX_VERTICES) descarregar();
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

    @Override
    public void drawText(String text, float x, float y, float size, int fillArgb, int strokeArgb, float strokeWidth, boolean italic) {
        // Fase B0: o texto fica no Canvas por cima (HUD).
    }

    @Override public void beginInvert() {}
    @Override public void endInvert() {}
}
