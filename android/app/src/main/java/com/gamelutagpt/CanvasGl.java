package com.gamelutagpt;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.os.Build;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Um Canvas do Android que desenha no OpenGL ({@link GlRenderCanvas}): o quadro inteiro da luta
 * (HUD, efeitos, botões, contador) sai pela placa de vídeo sem reescrever quem desenha. A
 * transformação e o save/restore ficam no Canvas de verdade (sem bitmap, ele só faz as contas); o
 * recorte, a transparência das camadas e as cores invertidas ficam numa pilha daqui, por nível
 * de save. Cada desenho vira triângulos:
 *
 * <ul>
 *   <li>retângulos, círculos, ovais, arcos e retângulos arredondados direto (contornos viram um
 *       anel de triângulos);</li>
 *   <li>caminhos: um contorno convexo vira leque; o resto (côncavo, com buracos, traços) passa
 *       pelo stencil, com o traço calculado pelo próprio Android ({@link Paint#getFillPath});</li>
 *   <li>texto: desenhado uma vez num bitmap com o Paint de verdade e guardado como textura;</li>
 *   <li>degradês: só os {@link Degrade} (os do Android não deixam ler as cores).</li>
 * </ul>
 */
final class CanvasGl extends Canvas {
    private final GlRenderCanvas gl;
    private int largura, altura;

    // Estado por nível de save (o índice é o getSaveCount() do Canvas de verdade).
    private float[][] recortes = new float[64][];
    private int[] nRecortes = new int[64];
    private float[] alfas = new float[64];
    private boolean[] invertidos = new boolean[64];

    private final Matrix matriz = new Matrix();
    private final float[] v = new float[9];
    private static final float[] IDENTIDADE = {1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f};
    // Buffers reaproveitados entre desenhos (nada de array novo por forma a cada quadro).
    private float[] bufA = new float[256], bufB = new float[256], bufC = new float[64], bufPx = new float[64];
    private final float[] ret8 = new float[8], ret8b = new float[8];

    private static float[] caber(float[] buf, int n) {
        return buf.length >= n ? buf : new float[Math.max(n, buf.length * 2)];
    }
    private final Matrix local = new Matrix(), inversa = new Matrix();
    private final float[] ponto = new float[2];
    private final Path caminho = new Path();
    private final RectF caixa = new RectF();
    private final ComoRenderCanvas comoRenderCanvas = new ComoRenderCanvas();

    CanvasGl(GlRenderCanvas gl) {
        this.gl = gl;
    }

    /** Por quadro, para o contador: desenhos, textos feitos agora e formas pelo stencil. */
    int desenhos, textosNovos, formasStencil;

    /** Começo do quadro: sem transformação, recorte ou camadas. */
    void comecar(int largura, int altura) {
        desenhos = textosNovos = formasStencil = 0;
        this.largura = largura;
        this.altura = altura;
        super.restoreToCount(1);
        super.setMatrix(null);
        matrizSuja = true;
        recortes[1] = null;
        nRecortes[1] = 0;
        alfas[1] = 1f;
        invertidos[1] = false;
    }

    /** Este Canvas como {@link RenderCanvas} (o cenário, os Ultras e as páginas desenham por ele). */
    RenderCanvas comoRenderCanvas() {
        return comoRenderCanvas;
    }

    @Override public int getWidth() { return largura; }
    @Override public int getHeight() { return altura; }
    // isHardwareAccelerated() fica o do Canvas (false): o construtor do Canvas pergunta isso e,
    // com true, não cria o canvas nativo que faz as contas de transformação (e o jogo fechava).

    // ---- Pilha ----

    private void empilhar() {
        int k = getSaveCount();
        if (k >= recortes.length) {
            int n = recortes.length * 2;
            recortes = java.util.Arrays.copyOf(recortes, n);
            nRecortes = java.util.Arrays.copyOf(nRecortes, n);
            alfas = java.util.Arrays.copyOf(alfas, n);
            invertidos = java.util.Arrays.copyOf(invertidos, n);
        }
        recortes[k] = recortes[k - 1];
        nRecortes[k] = nRecortes[k - 1];
        alfas[k] = alfas[k - 1];
        invertidos[k] = invertidos[k - 1];
    }

    @Override public int save() {
        int r = super.save();
        empilhar();
        return r;
    }

    /** Camada: no OpenGL vira só um save com a transparência (e a inversão) do pincel dela. */
    private int camada(Paint p) {
        int r = save();
        int k = getSaveCount();
        if (p != null) {
            alfas[k] *= p.getAlpha() / 255f;
            if (inverte(p.getColorFilter())) invertidos[k] = !invertidos[k];
        }
        return r;
    }

    private int camadaAlfa(int alpha) {
        int r = save();
        alfas[getSaveCount()] *= Math.max(0, Math.min(255, alpha)) / 255f;
        return r;
    }

    @Override public int saveLayer(RectF bounds, Paint paint, int flags) { return camada(paint); }
    @Override public int saveLayer(RectF bounds, Paint paint) { return camada(paint); }
    @Override public int saveLayer(float l, float t, float r, float b, Paint paint, int flags) { return camada(paint); }
    @Override public int saveLayer(float l, float t, float r, float b, Paint paint) { return camada(paint); }
    @Override public int saveLayerAlpha(RectF bounds, int alpha, int flags) { return camadaAlfa(alpha); }
    @Override public int saveLayerAlpha(RectF bounds, int alpha) { return camadaAlfa(alpha); }
    @Override public int saveLayerAlpha(float l, float t, float r, float b, int alpha, int flags) { return camadaAlfa(alpha); }
    @Override public int saveLayerAlpha(float l, float t, float r, float b, int alpha) { return camadaAlfa(alpha); }

    /** O filtro de inverter as cores (o negativo do impact frame). */
    private static boolean inverte(ColorFilter f) {
        float[] m = matrizDoFiltro(f);
        return m != null && m[0] < -0.5f && m[6] < -0.5f && m[12] < -0.5f;
    }

    // ---- Recorte ----

    @Override public boolean clipRect(float l, float t, float r, float b) {
        float[] q = ret8;
        q[0] = l; q[1] = t; q[2] = r; q[3] = t; q[4] = r; q[5] = b; q[6] = l; q[7] = b;
        recortarPoligono(q, 4);
        return true;
    }

    @Override public boolean clipRect(int l, int t, int r, int b) { return clipRect((float)l, (float)t, (float)r, (float)b); }
    @Override public boolean clipRect(Rect r) { return clipRect(r.left, r.top, r.right, r.bottom); }
    @Override public boolean clipRect(RectF r) { return clipRect(r.left, r.top, r.right, r.bottom); }

    @Override public boolean clipRect(float l, float t, float r, float b, Region.Op op) {
        return op == Region.Op.INTERSECT ? clipRect(l, t, r, b) : true;
    }

    @Override public boolean clipRect(RectF r, Region.Op op) { return clipRect(r.left, r.top, r.right, r.bottom, op); }
    @Override public boolean clipRect(Rect r, Region.Op op) { return clipRect(r.left, r.top, r.right, r.bottom, op); }

    @Override public boolean clipPath(Path path) {
        float[][] contornos = aproximar(path);
        if (contornos.length == 0) return true;
        float[] pts = contornos[0];
        // Os recortes do jogo são polígonos convexos; um que não for vira o contorno convexo dele.
        if (!convexo(pts, pts.length / 2)) pts = envoltoria(pts);
        recortarPoligono(pts, pts.length / 2);
        return true;
    }

    @Override public boolean clipPath(Path path, Region.Op op) {
        return op == Region.Op.INTERSECT ? clipPath(path) : true;
    }

    /** Recorta ao polígono convexo {@code xy} (espaço atual), somado ao recorte de agora. */
    void recortarPoligono(float[] xy, int n) {
        if (n < 3) return;
        int k = getSaveCount();
        lerMatriz();
        float[] px = bufPx = caber(bufPx, n * 2);
        float area = 0f;
        for (int i = 0; i < n; i++) {
            float x = xy[i * 2], y = xy[i * 2 + 1];
            px[i * 2] = v[0] * x + v[1] * y + v[2];
            px[i * 2 + 1] = altura - (v[3] * x + v[4] * y + v[5]);
        }
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            area += px[i * 2] * px[j * 2 + 1] - px[j * 2] * px[i * 2 + 1];
        }
        float sinal = area >= 0f ? 1f : -1f;
        int antes = nRecortes[k];
        int total = Math.min(32, antes + n);
        float[] planos = new float[32 * 3];
        if (recortes[k] != null) System.arraycopy(recortes[k], 0, planos, 0, antes * 3);
        for (int i = 0, p = antes; i < n && p < total; i++, p++) {
            int j = (i + 1) % n;
            float xi = px[i * 2], yi = px[i * 2 + 1];
            float qa = -(px[j * 2 + 1] - yi) * sinal, qb = (px[j * 2] - xi) * sinal;
            planos[p * 3] = qa;
            planos[p * 3 + 1] = qb;
            planos[p * 3 + 2] = -(qa * xi + qb * yi);
        }
        recortes[k] = planos;
        nRecortes[k] = total;
    }

    // ---- Estado para o OpenGL ----

    /**
     * A matriz só é lida do Canvas de verdade (duas chamadas nativas) quando alguma coisa a
     * mudou: as transformações e o restore marcam.
     */
    private boolean matrizSuja = true;

    private void lerMatriz() {
        if (!matrizSuja) return;
        super.getMatrix(matriz);
        matriz.getValues(v);
        matrizSuja = false;
    }

    @Override public void translate(float dx, float dy) { super.translate(dx, dy); matrizSuja = true; }
    @Override public void scale(float sx, float sy) { super.scale(sx, sy); matrizSuja = true; }
    @Override public void rotate(float graus) { super.rotate(graus); matrizSuja = true; }
    @Override public void skew(float sx, float sy) { super.skew(sx, sy); matrizSuja = true; }
    @Override public void concat(Matrix m) { super.concat(m); matrizSuja = true; }
    @Override public void setMatrix(Matrix m) { super.setMatrix(m); matrizSuja = true; }
    @Override public void restore() { super.restore(); matrizSuja = true; }
    @Override public void restoreToCount(int n) { super.restoreToCount(n); matrizSuja = true; }

    /** Leva a transformação, o recorte, a transparência e a inversão de agora para o OpenGL. */
    void prepararGl() {
        desenhos++;
        lerMatriz();
        gl.matriz(v);
        int k = getSaveCount();
        gl.recorte(recortes[k], nRecortes[k]);
        gl.alfa(alfas[k]);
        gl.inverter(invertidos[k]);
        // O filtro de cor só vale para imagem, e cada caminho de imagem põe o seu (drawBitmap pelo
        // Paint, o sprite pelo do lutador, o RenderCanvas sem filtro): zerar aqui fechava o lote a
        // cada sprite tingido (o contorno do SHINKAI, o rastro, os ecos).
    }

    // ---- Gravação de pedaços que não mudam (o controle de toque, os botões de teste) ----

    private final java.util.HashMap<Object, GlRenderCanvas.Gravacao> gravacoes = new java.util.HashMap<>();
    private Object gravando;
    private int textosAntesDaGravacao;
    /** Textos que ainda estavam na thread dos textos (faltaram ou saíram esticados): não se grava. */
    private int textosAdiados;

    /**
     * Repete o pedaço {@code chave} gravado com o mesmo {@code estado} (e a mesma transformação,
     * transparência e tamanho de tela): true, e quem chama pula o desenho. False: quem chama
     * desenha e depois chama {@link #gravado}, que guarda o que foi desenhado para os próximos
     * quadros. O controle de toque redesenhava círculos, arcos, linhas e textos a cada quadro
     * (1,7 ms no celular) para mudar só quando um botão é apertado.
     */
    boolean repetir(Object chave, long estado) {
        prepararGl();
        GlRenderCanvas.Gravacao g = gravacoes.get(chave);
        if (g != null && g.assinatura == estado && g.alfa == gl.alfaGlobal() && gl.nPlanos() == 0
            && g.matriz[0] == v[0] && g.matriz[1] == v[1] && g.matriz[2] == v[2]
            && g.matriz[3] == v[3] && g.matriz[4] == v[4] && g.matriz[5] == v[5] && gl.reproduzir(g)) {
            return true;
        }
        gravando = gl.nPlanos() == 0 ? chave : null;
        textosAntesDaGravacao = textosNovos + textosAdiados + gl.textosNovosRc + gl.textosAdiadosRc;
        if (gravando != null) gl.gravarComecar();
        return false;
    }

    /** Fecha a gravação aberta por {@link #repetir} (o desenho de agora vale também para os próximos quadros). */
    void gravado(Object chave, long estado) {
        if (gravando != chave) return;
        gravando = null;
        GlRenderCanvas.Gravacao g = gl.gravarTerminar();
        // Com texto feito agora (talvez um provisório, esticado de outro tamanho) grava no próximo.
        if (g == null || textosNovos + textosAdiados + gl.textosNovosRc + gl.textosAdiadosRc != textosAntesDaGravacao) {
            gravacoes.remove(chave);
            return;
        }
        g.assinatura = estado;
        g.alfa = gl.alfaGlobal();
        System.arraycopy(v, 0, g.matriz, 0, 6);
        gravacoes.put(chave, g);
    }

    /** Escala da transformação atual (pixels da tela por unidade). */
    private float escala() {
        return (float)Math.sqrt(Math.abs(v[0] * v[4] - v[1] * v[3]));
    }

    // ---- Cor e degradê de um pincel ----

    private Degrade.Dados degrade;
    private boolean degradeLocal;
    private int corPincel;
    private float tipoPincel;

    /** Prepara a cor (ou o degradê) do pincel para os vértices que vêm. */
    private void pincel(Paint p) {
        Shader s = p.getShader();
        degrade = s instanceof Degrade.Dados ? (Degrade.Dados)s : null;
        if (degrade == null) {
            corPincel = p.getColor();
            tipoPincel = 0f;
            return;
        }
        corPincel = (p.getAlpha() << 24) | 0xFFFFFF;
        tipoPincel = gl.tipoDegrade(degrade);
        degradeLocal = s.getLocalMatrix(local) && !local.isIdentity();
        if (degradeLocal) local.invert(inversa);
    }

    /** O uv do degradê no ponto (x, y) do espaço atual. */
    private float uvU, uvV;

    private void uv(float x, float y) {
        if (degrade == null) {
            uvU = 0f;
            uvV = 0f;
            return;
        }
        if (degradeLocal) {
            ponto[0] = x;
            ponto[1] = y;
            inversa.mapPoints(ponto);
            x = ponto[0];
            y = ponto[1];
        }
        float[] g = degrade.geometria();
        if (degrade.radial()) {
            float r = Math.max(1e-4f, g[2]);
            uvU = (x - g[0]) / r;
            uvV = (y - g[1]) / r;
        } else {
            float dx = g[2] - g[0], dy = g[3] - g[1];
            float l2 = Math.max(1e-6f, dx * dx + dy * dy);
            uvU = ((x - g[0]) * dx + (y - g[1]) * dy) / l2;
            uvV = 0.5f;
        }
    }

    private void tri(float x0, float y0, float x1, float y1, float x2, float y2) {
        uv(x0, y0);
        float u0 = uvU, v0 = uvV;
        uv(x1, y1);
        float u1 = uvU, v1 = uvV;
        uv(x2, y2);
        gl.triangulo(x0, y0, u0, v0, x1, y1, u1, v1, x2, y2, uvU, uvV, corPincel, tipoPincel);
    }

    /** Leque de um polígono convexo (pontos {x, y, ...}), a partir do centro. */
    private void leque(float[] pts, int n) {
        if (n < 3) return;
        float cx = 0f, cy = 0f;
        for (int i = 0; i < n; i++) {
            cx += pts[i * 2];
            cy += pts[i * 2 + 1];
        }
        cx /= n;
        cy /= n;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            tri(cx, cy, pts[i * 2], pts[i * 2 + 1], pts[j * 2], pts[j * 2 + 1]);
        }
    }

    /** O anel entre dois contornos fechados com o mesmo número de pontos. */
    private void anel(float[] fora, float[] dentro, int n) {
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            tri(fora[i * 2], fora[i * 2 + 1], fora[j * 2], fora[j * 2 + 1], dentro[j * 2], dentro[j * 2 + 1]);
            tri(fora[i * 2], fora[i * 2 + 1], dentro[j * 2], dentro[j * 2 + 1], dentro[i * 2], dentro[i * 2 + 1]);
        }
    }

    private float largura(Paint p) {
        float w = p.getStrokeWidth();
        // Traço 0 do Android: um pixel na tela.
        return w > 0f ? w : 1f / Math.max(1e-4f, escala());
    }

    // ---- Formas ----

    @Override public void drawColor(int color) {
        cobrirTela(color);
    }

    @Override public void drawColor(int color, PorterDuff.Mode mode) {
        if (mode == PorterDuff.Mode.CLEAR) return;
        cobrirTela(color);
    }

    private void cobrirTela(int color) {
        prepararGl();
        gl.matriz(IDENTIDADE);
        gl.fillRect(0f, 0f, largura, altura, color);
    }

    @Override public void drawRect(float l, float t, float r, float b, Paint p) {
        prepararGl();
        pincel(p);
        Paint.Style st = p.getStyle();
        if (st == Paint.Style.STROKE) {
            float h = largura(p) * 0.5f;
            float[] fo = ret8, de = ret8b;
            fo[0] = l - h; fo[1] = t - h; fo[2] = r + h; fo[3] = t - h; fo[4] = r + h; fo[5] = b + h; fo[6] = l - h; fo[7] = b + h;
            de[0] = l + h; de[1] = t + h; de[2] = r - h; de[3] = t + h; de[4] = r - h; de[5] = b - h; de[6] = l + h; de[7] = b - h;
            anel(fo, de, 4);
            return;
        }
        if (st == Paint.Style.FILL_AND_STROKE) {
            float h = p.getStrokeWidth() * 0.5f;
            l -= h; t -= h; r += h; b += h;
        }
        tri(l, t, r, t, r, b);
        tri(l, t, r, b, l, b);
    }

    @Override public void drawRect(RectF r, Paint p) { drawRect(r.left, r.top, r.right, r.bottom, p); }
    @Override public void drawRect(Rect r, Paint p) { drawRect(r.left, r.top, r.right, r.bottom, p); }

    /** Contorno de retângulo arredondado ({@code seg} pontos por canto), recuado {@code d} para fora. */
    /** Cosseno e seno dos 7 pontos de cada canto (os 4 cantos em sequência), calculados uma vez. */
    private static final float[] CANTO_COS = new float[28], CANTO_SEN = new float[28];
    static {
        for (int c = 0; c < 4; c++) {
            for (int i = 0; i < 7; i++) {
                double a = -Math.PI / 2 + c * Math.PI / 2 + (Math.PI / 2) * i / 6;
                CANTO_COS[c * 7 + i] = (float)Math.cos(a);
                CANTO_SEN[c * 7 + i] = (float)Math.sin(a);
            }
        }
    }

    private float[] arredondado(float l, float t, float r, float b, float rx, float ry, float d, float[] pts) {
        rx = Math.max(0f, Math.min(rx, (r - l) * 0.5f));
        ry = Math.max(0f, Math.min(ry, (b - t) * 0.5f));
        int k = 0;
        for (int c = 0; c < 4; c++) {
            float cx = c < 2 ? r - rx : l + rx, cy = c == 1 || c == 2 ? b - ry : t + ry;
            for (int i = 0; i < 7; i++) {
                pts[k++] = cx + CANTO_COS[c * 7 + i] * (rx + d);
                pts[k++] = cy + CANTO_SEN[c * 7 + i] * (ry + d);
            }
        }
        return pts;
    }

    @Override public void drawRoundRect(float l, float t, float r, float b, float rx, float ry, Paint p) {
        prepararGl();
        pincel(p);
        int n = 28;
        bufA = caber(bufA, n * 2);
        bufB = caber(bufB, n * 2);
        if (p.getStyle() == Paint.Style.STROKE) {
            float h = largura(p) * 0.5f;
            anel(arredondado(l, t, r, b, rx, ry, h, bufA), arredondado(l, t, r, b, rx, ry, -Math.min(h, Math.min(rx, ry)), bufB), n);
            return;
        }
        float d = p.getStyle() == Paint.Style.FILL_AND_STROKE ? p.getStrokeWidth() * 0.5f : 0f;
        leque(arredondado(l, t, r, b, rx, ry, d, bufA), n);
    }

    @Override public void drawRoundRect(RectF r, float rx, float ry, Paint p) {
        drawRoundRect(r.left, r.top, r.right, r.bottom, rx, ry, p);
    }

    /** Pontos de uma elipse (ou arco dela), de {@code inicio} a {@code inicio + varre} graus. */
    private static float[] elipse(float cx, float cy, float rx, float ry, float inicio, float varre, int n, boolean fechado,
                                  float[] pts) {
        int m = fechado ? n : n + 1;
        for (int i = 0; i < m; i++) {
            double a = Math.toRadians(inicio + varre * i / n);
            pts[i * 2] = cx + rx * (float)Math.cos(a);
            pts[i * 2 + 1] = cy + ry * (float)Math.sin(a);
        }
        return pts;
    }

    /** Segmentos para uma curva de raio {@code r} (mais na tela grande, sem passar do razoável). */
    private int segmentos(float r, float graus) {
        float px = r * escala() * (float)Math.toRadians(Math.abs(graus));
        return Math.max(8, Math.min(96, (int)(px / 6f)));
    }

    @Override public void drawCircle(float cx, float cy, float r, Paint p) {
        drawOval(cx - r, cy - r, cx + r, cy + r, p);
    }

    @Override public void drawOval(float l, float t, float r, float b, Paint p) {
        prepararGl();
        pincel(p);
        float cx = (l + r) * 0.5f, cy = (t + b) * 0.5f, rx = (r - l) * 0.5f, ry = (b - t) * 0.5f;
        Paint.Style st = p.getStyle();
        boolean circulo = Math.abs(rx - ry) < 0.01f;
        if (st == Paint.Style.STROKE) {
            float h = largura(p) * 0.5f;
            if (degrade == null && circulo) {
                // Anel no shader: borda suave dos dois lados.
                float fora = rx + h, dentro = Math.max(0f, rx - h) / fora;
                gl.retanguloUv(cx - fora, cy - fora, cx + fora, cy + fora, -1f, -1f, 1f, 1f, corPincel, 6f + Math.min(0.999f, dentro));
                return;
            }
            int n = segmentos(Math.max(rx, ry), 360f);
            bufA = caber(bufA, (n + 1) * 2);
            bufB = caber(bufB, (n + 1) * 2);
            anel(elipse(cx, cy, rx + h, ry + h, 0f, 360f, n, true, bufA),
                elipse(cx, cy, Math.max(0f, rx - h), Math.max(0f, ry - h), 0f, 360f, n, true, bufB), n);
            return;
        }
        if (st == Paint.Style.FILL_AND_STROKE) {
            float h = p.getStrokeWidth() * 0.5f;
            rx += h;
            ry += h;
        }
        if (degrade == null) {
            gl.retanguloUv(cx - rx, cy - ry, cx + rx, cy + ry, -1f, -1f, 1f, 1f, corPincel, 2f);
            return;
        }
        int n = segmentos(Math.max(rx, ry), 360f);
        bufA = caber(bufA, (n + 1) * 2);
        leque(elipse(cx, cy, rx, ry, 0f, 360f, n, true, bufA), n);
    }

    @Override public void drawOval(RectF r, Paint p) { drawOval(r.left, r.top, r.right, r.bottom, p); }

    @Override public void drawArc(float l, float t, float r, float b, float inicio, float varre, boolean centro, Paint p) {
        if (varre == 0f) return;
        if (Math.abs(varre) >= 360f) {
            drawOval(l, t, r, b, p);
            return;
        }
        prepararGl();
        pincel(p);
        float cx = (l + r) * 0.5f, cy = (t + b) * 0.5f, rx = (r - l) * 0.5f, ry = (b - t) * 0.5f;
        int n = segmentos(Math.max(rx, ry), varre);
        if (p.getStyle() == Paint.Style.STROKE) {
            float h = largura(p) * 0.5f;
            if (centro) {
                // Fatia em contorno: pelo traço do Android.
                caminho.reset();
                caminho.moveTo(cx, cy);
                caminho.arcTo(l, t, r, b, inicio, varre, false);
                caminho.close();
                tracar(caminho, p);
                return;
            }
            bufA = caber(bufA, (n + 1) * 2);
            bufB = caber(bufB, (n + 1) * 2);
            float[] fora = elipse(cx, cy, rx + h, ry + h, inicio, varre, n, false, bufA);
            float[] dentro = elipse(cx, cy, Math.max(0f, rx - h), Math.max(0f, ry - h), inicio, varre, n, false, bufB);
            for (int i = 0; i < n; i++) {
                int j = i + 1;
                tri(fora[i * 2], fora[i * 2 + 1], fora[j * 2], fora[j * 2 + 1], dentro[j * 2], dentro[j * 2 + 1]);
                tri(fora[i * 2], fora[i * 2 + 1], dentro[j * 2], dentro[j * 2 + 1], dentro[i * 2], dentro[i * 2 + 1]);
            }
            if (p.getStrokeCap() == Paint.Cap.ROUND) {
                double a0 = Math.toRadians(inicio), a1 = Math.toRadians(inicio + varre);
                ponta(cx + rx * (float)Math.cos(a0), cy + ry * (float)Math.sin(a0), h);
                ponta(cx + rx * (float)Math.cos(a1), cy + ry * (float)Math.sin(a1), h);
            }
            return;
        }
        bufA = caber(bufA, (n + 1) * 2);
        float[] arco = elipse(cx, cy, rx, ry, inicio, varre, n, false, bufA);
        // Com centro, leque do centro (fatia); sem, leque da corda (o arco fechado pela reta).
        float ox = centro ? cx : arco[0], oy = centro ? cy : arco[1];
        for (int i = 0; i < n; i++) tri(ox, oy, arco[i * 2], arco[i * 2 + 1], arco[i * 2 + 2], arco[i * 2 + 3]);
    }

    @Override public void drawArc(RectF r, float inicio, float varre, boolean centro, Paint p) {
        drawArc(r.left, r.top, r.right, r.bottom, inicio, varre, centro, p);
    }

    /** Ponta redonda de traço (um círculo na cor do pincel). */
    private void ponta(float x, float y, float raio) {
        if (degrade == null) {
            gl.retanguloUv(x - raio, y - raio, x + raio, y + raio, -1f, -1f, 1f, 1f, corPincel, 2f);
        } else {
            int n = 16;
            bufC = caber(bufC, (n + 1) * 2);
            leque(elipse(x, y, raio, raio, 0f, 360f, n, true, bufC), n);
        }
    }

    @Override public void drawLine(float x1, float y1, float x2, float y2, Paint p) {
        prepararGl();
        pincel(p);
        float w = largura(p) * 0.5f;
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float)Math.sqrt(dx * dx + dy * dy);
        if (len <= 1e-5f) {
            if (p.getStrokeCap() != Paint.Cap.BUTT) ponta(x1, y1, w);
            return;
        }
        float ux = dx / len, uy = dy / len;
        if (p.getStrokeCap() == Paint.Cap.SQUARE) {
            x1 -= ux * w; y1 -= uy * w; x2 += ux * w; y2 += uy * w;
        }
        float nx = -uy * w, ny = ux * w;
        tri(x1 + nx, y1 + ny, x2 + nx, y2 + ny, x2 - nx, y2 - ny);
        tri(x1 + nx, y1 + ny, x2 - nx, y2 - ny, x1 - nx, y1 - ny);
        if (p.getStrokeCap() == Paint.Cap.ROUND) {
            ponta(x1, y1, w);
            ponta(x2, y2, w);
        }
    }

    // ---- Caminhos ----

    @Override public void drawPath(Path path, Paint p) {
        prepararGl();
        pincel(p);
        Paint.Style st = p.getStyle();
        if (st != Paint.Style.FILL) {
            if (st == Paint.Style.FILL_AND_STROKE) preencher(path, path.getFillType());
            tracar(path, p);
            return;
        }
        preencher(path, path.getFillType());
    }

    private final Path traco = new Path();

    /** O traço do caminho pelo próprio Android (pontas, junções e tracejado), preenchido. */
    private void tracar(Path path, Paint p) {
        if (tracarRapido(path, p)) return;
        traco.reset();
        Paint.Style antes = p.getStyle();
        if (antes != Paint.Style.STROKE) p.setStyle(Paint.Style.STROKE);
        float w = p.getStrokeWidth();
        if (w <= 0f) p.setStrokeWidth(largura(p));
        p.getFillPath(path, traco);
        p.setStrokeWidth(w);
        if (antes != Paint.Style.STROKE) p.setStyle(antes);
        preencher(traco, Path.FillType.WINDING);
    }

    /**
     * Traço sem tracejado (raios, trincas, riscos, brilhos, contornos): cada contorno vira uma
     * tira de triângulos com junção em esquadria, direto. O caminho do Android (getFillPath, um
     * polígono enorme com as junções redondas, e depois o stencil, que corta o lote) custava
     * caro por traço; o SHINKAI desenha dezenas por quadro.
     */
    private boolean tracarRapido(Path path, Paint p) {
        if (Build.VERSION.SDK_INT < 26 || p.getPathEffect() != null) return false;
        lerMatriz();
        float[] a = path.approximate(0.5f / Math.max(1e-3f, escala()));
        int n = a.length / 3;
        float w = largura(p) * 0.5f;
        boolean redonda = p.getStrokeCap() == Paint.Cap.ROUND;
        int inicio = 0;
        for (int i = 1; i <= n; i++) {
            // Um ponto com a mesma fração do anterior começa outro contorno.
            if (i == n || a[i * 3] == a[(i - 1) * 3]) {
                faixa(a, inicio, i, w, redonda);
                inicio = i;
            }
        }
        return true;
    }

    private float[] faixaPts = new float[64];

    /** Um contorno do traço: a tira (esquerda/direita de cada ponto) e as pontas. */
    private void faixa(float[] a, int de, int ate, float w, boolean redonda) {
        // Pontos sem os repetidos.
        int n = 0;
        if (faixaPts.length < (ate - de) * 2) faixaPts = new float[(ate - de) * 2 + 16];
        float[] pt = faixaPts;
        for (int i = de; i < ate; i++) {
            float x = a[i * 3 + 1], y = a[i * 3 + 2];
            if (n > 0 && Math.abs(x - pt[n * 2 - 2]) < 1e-4f && Math.abs(y - pt[n * 2 - 1]) < 1e-4f) continue;
            pt[n * 2] = x;
            pt[n * 2 + 1] = y;
            n++;
        }
        if (n == 0) return;
        if (n == 1) {
            if (redonda) ponta(pt[0], pt[1], w);
            return;
        }
        float plx = 0f, ply = 0f, prx = 0f, pry = 0f;
        for (int i = 0; i < n; i++) {
            float x = pt[i * 2], y = pt[i * 2 + 1];
            // Normais do segmento que chega e do que sai deste ponto.
            float ax = 0f, ay = 0f, bx = 0f, by = 0f;
            if (i > 0) {
                float dx = x - pt[i * 2 - 2], dy = y - pt[i * 2 - 1];
                float l = (float)Math.sqrt(dx * dx + dy * dy);
                ax = -dy / l; ay = dx / l;
            }
            if (i + 1 < n) {
                float dx = pt[i * 2 + 2] - x, dy = pt[i * 2 + 3] - y;
                float l = (float)Math.sqrt(dx * dx + dy * dy);
                bx = -dy / l; by = dx / l;
            }
            if (i == 0) { ax = bx; ay = by; }
            if (i + 1 == n) { bx = ax; by = ay; }
            // Esquadria: a soma das normais, esticada para manter a largura (no máximo 2×).
            float mx = ax + bx, my = ay + by;
            float ml = (float)Math.sqrt(mx * mx + my * my);
            if (ml < 1e-4f) { mx = ax; my = ay; ml = 1f; }
            mx /= ml; my /= ml;
            float cos = mx * ax + my * ay;
            if (cos < 0.5f) {
                // Volta fechada (mais de 120°): a esquadria ficaria comprida demais. A tira fecha
                // com a normal do segmento que chega e recomeça com a do que sai.
                float lx = x + ax * w, ly = y + ay * w, rx = x - ax * w, ry = y - ay * w;
                tri(plx, ply, lx, ly, rx, ry);
                tri(plx, ply, rx, ry, prx, pry);
                if (redonda) ponta(x, y, w);
                plx = x + bx * w; ply = y + by * w; prx = x - bx * w; pry = y - by * w;
                continue;
            }
            float k = w / cos;
            float lx = x + mx * k, ly = y + my * k, rx = x - mx * k, ry = y - my * k;
            if (i > 0) {
                tri(plx, ply, lx, ly, rx, ry);
                tri(plx, ply, rx, ry, prx, pry);
            }
            plx = lx; ply = ly; prx = rx; pry = ry;
        }
        if (redonda) {
            boolean fechado = n > 2 && Math.abs(pt[0] - pt[n * 2 - 2]) < 1e-3f && Math.abs(pt[1] - pt[n * 2 - 1]) < 1e-3f;
            if (!fechado) {
                ponta(pt[0], pt[1], w);
                ponta(pt[n * 2 - 2], pt[n * 2 - 1], w);
            }
        }
    }

    private void preencher(Path path, Path.FillType tipo) {
        float[][] contornos = aproximar(path);
        if (contornos.length == 0) return;
        if (tipo != Path.FillType.INVERSE_WINDING && tipo != Path.FillType.INVERSE_EVEN_ODD && semStencil(contornos)) {
            for (float[] c : contornos) leque(c, c.length / 2);
            return;
        }
        // Qualquer outra forma: os leques marcam o stencil e a caixa da forma pinta onde ficou marcado.
        path.computeBounds(caixa, true);
        formasStencil++;
        int cor = corPincel;
        float tipoAntes = tipoPincel;
        Degrade.Dados dg = degrade;
        gl.comecarStencil(tipo == Path.FillType.EVEN_ODD || tipo == Path.FillType.INVERSE_EVEN_ODD);
        degrade = null;
        corPincel = 0xFFFFFFFF;
        tipoPincel = 0f;
        for (float[] c : contornos) {
            int n = c.length / 2;
            for (int i = 1; i + 1 < n; i++) tri(c[0], c[1], c[i * 2], c[i * 2 + 1], c[i * 2 + 2], c[i * 2 + 3]);
        }
        gl.cobrirStencil();
        degrade = dg;
        corPincel = cor;
        tipoPincel = tipoAntes;
        tri(caixa.left, caixa.top, caixa.right, caixa.top, caixa.right, caixa.bottom);
        tri(caixa.left, caixa.top, caixa.right, caixa.bottom, caixa.left, caixa.bottom);
        gl.terminarStencil();
    }

    /** Os contornos do caminho como polígonos ({x, y, ...}), com o detalhe que a tela pede. */
    private float[][] aproximar(Path path) {
        lerMatriz();
        float tol = 0.5f / Math.max(1e-3f, escala());
        java.util.ArrayList<float[]> saida = new java.util.ArrayList<>();
        if (Build.VERSION.SDK_INT >= 26) {
            float[] a = path.approximate(tol);
            int inicio = 0, n = a.length / 3;
            for (int i = 1; i <= n; i++) {
                // Um ponto com a mesma fração do anterior começa outro contorno.
                if (i == n || a[i * 3] == a[(i - 1) * 3]) {
                    adicionar(saida, a, inicio, i);
                    inicio = i;
                }
            }
        } else {
            PathMeasure pm = new PathMeasure(path, false);
            float[] pos = new float[2];
            float passo = 3f / Math.max(1e-3f, escala());
            do {
                float l = pm.getLength();
                if (l <= 0f) continue;
                int n = Math.max(2, (int)Math.ceil(l / passo));
                float[] c = new float[(n + 1) * 2];
                for (int i = 0; i <= n; i++) {
                    pm.getPosTan(l * i / n, pos, null);
                    c[i * 2] = pos[0];
                    c[i * 2 + 1] = pos[1];
                }
                if (c.length >= 6) saida.add(c);
            } while (pm.nextContour());
        }
        return saida.toArray(new float[0][]);
    }

    private static void adicionar(java.util.List<float[]> saida, float[] a, int de, int ate) {
        int n = ate - de;
        // O contorno fechado repete o primeiro ponto no fim: fora (desviava o centro do leque).
        if (n > 3 && a[de * 3 + 1] == a[(ate - 1) * 3 + 1] && a[de * 3 + 2] == a[(ate - 1) * 3 + 2]) n--;
        if (n < 3) return;
        float[] c = new float[n * 2];
        for (int i = 0; i < n; i++) {
            c[i * 2] = a[(de + i) * 3 + 1];
            c[i * 2 + 1] = a[(de + i) * 3 + 2];
        }
        saida.add(c);
    }

    /**
     * A forma sai em leques, sem o stencil: cada contorno convexo ou estrelado a partir do centro
     * (estrelas de impacto, chamas do nível, farpas do desgaste, a cauda do projétil) e, com mais
     * de um, sem as caixas deles se encostarem (o "!" do combo breaker: a cunha e o ponto).
     */
    private static boolean semStencil(float[][] contornos) {
        for (float[] c : contornos) {
            int n = c.length / 2;
            if (!convexo(c, n) && !estrelado(c, n)) return false;
        }
        if (contornos.length == 1) return true;
        for (int i = 0; i < contornos.length; i++) {
            for (int j = i + 1; j < contornos.length; j++) {
                if (caixasSeTocam(contornos[i], contornos[j])) return false;
            }
        }
        return true;
    }

    /** Todo ponto vê o centro (a média dos pontos, de onde o leque sai): o leque cobre a forma certa. */
    private static boolean estrelado(float[] p, int n) {
        if (n < 3) return false;
        float cx = 0f, cy = 0f;
        for (int i = 0; i < n; i++) {
            cx += p[i * 2];
            cy += p[i * 2 + 1];
        }
        cx /= n;
        cy /= n;
        int sinal = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            float ax = p[i * 2] - cx, ay = p[i * 2 + 1] - cy, bx = p[j * 2] - cx, by = p[j * 2 + 1] - cy;
            float z = ax * by - ay * bx;
            if (Math.abs(z) <= 1e-5f * (Math.abs(ax) + Math.abs(ay)) * (Math.abs(bx) + Math.abs(by))) continue;
            int s = z > 0 ? 1 : -1;
            if (sinal == 0) sinal = s;
            else if (s != sinal) return false;
        }
        return sinal != 0;
    }

    private static boolean caixasSeTocam(float[] a, float[] b) {
        float al = Float.MAX_VALUE, at = Float.MAX_VALUE, ar = -Float.MAX_VALUE, ab = -Float.MAX_VALUE;
        for (int i = 0; i + 1 < a.length; i += 2) {
            al = Math.min(al, a[i]); ar = Math.max(ar, a[i]); at = Math.min(at, a[i + 1]); ab = Math.max(ab, a[i + 1]);
        }
        float bl = Float.MAX_VALUE, bt = Float.MAX_VALUE, br = -Float.MAX_VALUE, bb = -Float.MAX_VALUE;
        for (int i = 0; i + 1 < b.length; i += 2) {
            bl = Math.min(bl, b[i]); br = Math.max(br, b[i]); bt = Math.min(bt, b[i + 1]); bb = Math.max(bb, b[i + 1]);
        }
        return al <= br && bl <= ar && at <= bb && bt <= ab;
    }

    private static boolean convexo(float[] p, int n) {
        if (n < 3) return false;
        int sinal = 0;
        for (int i = 0; i < n; i++) {
            float ax = p[((i + 1) % n) * 2] - p[i * 2], ay = p[((i + 1) % n) * 2 + 1] - p[i * 2 + 1];
            float bx = p[((i + 2) % n) * 2] - p[((i + 1) % n) * 2], by = p[((i + 2) % n) * 2 + 1] - p[((i + 1) % n) * 2 + 1];
            float z = ax * by - ay * bx;
            // Tolerância relativa ao tamanho dos lados (forma pequena desenhada ampliada).
            if (Math.abs(z) <= 1e-5f * (Math.abs(ax) + Math.abs(ay)) * (Math.abs(bx) + Math.abs(by))) continue;
            int s = z > 0 ? 1 : -1;
            if (sinal == 0) sinal = s;
            else if (s != sinal) return false;
        }
        return true;
    }

    /** Envoltória convexa (cadeia monótona). */
    private static float[] envoltoria(float[] p) {
        int n = p.length / 2;
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        java.util.Arrays.sort(idx, (i, j) -> p[i * 2] != p[j * 2] ? Float.compare(p[i * 2], p[j * 2]) : Float.compare(p[i * 2 + 1], p[j * 2 + 1]));
        int[] h = new int[2 * n];
        int k = 0;
        for (int t = 0; t < n; t++) {
            int i = idx[t];
            while (k >= 2 && cruz(p, h[k - 2], h[k - 1], i) <= 0) k--;
            h[k++] = i;
        }
        for (int t = n - 2, base = k + 1; t >= 0; t--) {
            int i = idx[t];
            while (k >= base && cruz(p, h[k - 2], h[k - 1], i) <= 0) k--;
            h[k++] = i;
        }
        int m = Math.max(0, k - 1);
        float[] out = new float[m * 2];
        for (int i = 0; i < m; i++) {
            out[i * 2] = p[h[i] * 2];
            out[i * 2 + 1] = p[h[i] * 2 + 1];
        }
        return out;
    }

    private static float cruz(float[] p, int o, int a, int b) {
        return (p[a * 2] - p[o * 2]) * (p[b * 2 + 1] - p[o * 2 + 1]) - (p[a * 2 + 1] - p[o * 2 + 1]) * (p[b * 2] - p[o * 2]);
    }

    // ---- Imagens ----

    private static final Map<ColorFilter, float[]> MATRIZES = new WeakHashMap<>();

    /** A matriz 4x5 de um filtro de cor: os do {@link Filtro} e os ColorMatrixColorFilter. */
    private static float[] matrizDoFiltro(ColorFilter f) {
        if (f == null) return null;
        float[] m = Filtro.matrizDe(f);
        if (m != null) return m;
        m = MATRIZES.get(f);
        if (m == null && f instanceof ColorMatrixColorFilter && Build.VERSION.SDK_INT >= 26) {
            ColorMatrix cm = new ColorMatrix();
            ((ColorMatrixColorFilter)f).getColorMatrix(cm);
            m = cm.getArray().clone();
            MATRIZES.put(f, m);
        }
        return m;
    }

    private void imagem(Bitmap b, float sl, float st, float sr, float sb, float l, float t, float r, float bt, Paint p) {
        if (b == null || b.isRecycled()) return;
        prepararGl();
        GlRenderCanvas.Textura tx = gl.textura(b, false);
        if (tx == null) return;
        gl.filtro(p != null ? matrizDoFiltro(p.getColorFilter()) : null);
        // O recorte da fonte é em pixels do bitmap; a textura sabe o tamanho dele.
        gl.desenharTextura(tx, sl, st, sr, sb, l, t, r, bt, p != null ? p.getAlpha() : 255);
    }

    @Override public void drawBitmap(Bitmap b, Rect src, RectF dst, Paint p) {
        if (src == null) imagem(b, 0, 0, b.getWidth(), b.getHeight(), dst.left, dst.top, dst.right, dst.bottom, p);
        else imagem(b, src.left, src.top, src.right, src.bottom, dst.left, dst.top, dst.right, dst.bottom, p);
    }

    @Override public void drawBitmap(Bitmap b, Rect src, Rect dst, Paint p) {
        if (src == null) imagem(b, 0, 0, b.getWidth(), b.getHeight(), dst.left, dst.top, dst.right, dst.bottom, p);
        else imagem(b, src.left, src.top, src.right, src.bottom, dst.left, dst.top, dst.right, dst.bottom, p);
    }

    @Override public void drawBitmap(Bitmap b, float left, float top, Paint p) {
        imagem(b, 0, 0, b.getWidth(), b.getHeight(), left, top, left + b.getWidth(), top + b.getHeight(), p);
    }

    @Override public void drawBitmap(Bitmap b, Matrix m, Paint p) {
        save();
        concat(m);
        imagem(b, 0, 0, b.getWidth(), b.getHeight(), 0, 0, b.getWidth(), b.getHeight(), p);
        restore();
    }

    // ---- Texto: cada texto vira um bitmap feito pelo Paint de verdade, guardado como textura ----

    private static final int TEXTOS = 512;
    private static final float MAX_PIXELS_TEXTO = 400_000f;

    private static final class Texto {
        /** O lugar na textura dos textos e a geração dela (recomeçou: faz de novo). */
        float[] uv;
        int geracao;
        /** Onde fica o ponto (x, linha de base) no bitmap, e o tamanho dele, no espaço local. */
        float origemX, origemY, largura, altura;
        /** Tamanho da fonte em que foi feito (o reaproveitado em outro tamanho estica por ele). */
        float base;
        /** Escala pedida e contorno com que foi feito (o reaproveitamento do maior confere). */
        float esc, traco;
        /** O bitmap desenhado na thread dos textos, até subir para a placa. */
        Bitmap bitmap;
    }

    /**
     * Textos feitos por quadro, no máximo: passou disso, o mesmo texto noutro tamanho ou contorno
     * reaproveita o último bitmap dele, esticado. Uma animação de texto que ninguém previu custa
     * no máximo isso por quadro, em vez de um bitmap por texto por quadro.
     */
    private static final int NOVOS_POR_QUADRO = 6;
    /** O último bitmap de cada texto (pela palavra, cor e estilo; sem tamanho, escala e contorno). */
    private final java.util.HashMap<String, Texto> ultimoDoTexto = new java.util.HashMap<>();
    private final StringBuilder chaveSolta = new StringBuilder();

    // Na ordem de uso (LRU): na de inserção, cada texto passageiro (relógio, combo, dano) tirava
    // um texto do HUD usado em todo quadro, refeito no quadro seguinte.
    private final LinkedHashMap<String, Texto> textos = new LinkedHashMap<String, Texto>(TEXTOS, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Texto> velho) {
            return size() > TEXTOS;
        }
    };
    private final StringBuilder chave = new StringBuilder();

    /** Contexto novo: as texturas dos textos se foram. */
    void esquecerTextos() {
        textos.clear();
        emPreparo.clear();
        textosFalhos.clear();
        ultimoDoTexto.clear();
        gravacoes.clear();
    }

    @Override public void drawText(String text, float x, float y, Paint p) {
        texto(text, x, y, p);
    }

    @Override public void drawText(String text, int start, int end, float x, float y, Paint p) {
        texto(text.substring(start, end), x, y, p);
    }

    @Override public void drawText(char[] text, int index, int count, float x, float y, Paint p) {
        texto(new String(text, index, count), x, y, p);
    }

    @Override public void drawText(CharSequence text, int start, int end, float x, float y, Paint p) {
        texto(text.subSequence(start, end).toString(), x, y, p);
    }

    private final Paint pincelTexto = new Paint();

    /**
     * Escala em que o texto é feito: poucos degraus (um quarto de oitava), e a placa de vídeo
     * estica o resto. Texto que cresce ou encolhe (o aviso de golpe forte, o contador) não vira
     * um bitmap novo a cada quadro.
     */
    private static float degrauDeEscala(float e) {
        double d = Math.ceil(Math.log(Math.max(0.25f, e)) / Math.log(2) * 4.0) / 4.0;
        return (float)Math.pow(2.0, d);
    }

    /** O tamanho em que o texto é feito: o degrau (um quarto de oitava) mais perto do pedido. */
    private static float degrauDeTamanho(float tamanho) {
        double d = Math.round(Math.log(Math.max(1f, tamanho)) / Math.log(2) * 4.0) / 4.0;
        return (float)Math.pow(2.0, d);
    }

    private static float degrauDeContorno(float largura) {
        if (largura <= 0f) return 0f;
        return (float)Math.pow(2.0, Math.round(Math.log(largura) / Math.log(2) * 8.0) / 8.0);
    }

    /** Os textos de um caractere só, sem criar String a cada quadro. */
    private static final String[] CARACTERES = new String[128];
    private static final java.util.HashMap<Character, String> OUTROS = new java.util.HashMap<>();
    static {
        for (char ch = 0; ch < 128; ch++) CARACTERES[ch] = String.valueOf(ch);
    }

    private static String caractere(char ch) {
        if (ch < 128) return CARACTERES[ch];
        String s = OUTROS.get(ch);
        if (s == null) {
            s = String.valueOf(ch);
            OUTROS.put(ch, s);
        }
        return s;
    }

    /**
     * Textos de diagnóstico (o contador de FPS e o relatório do teste): cada linha numa textura
     * só dela, fora da textura dos textos. As linhas compridas, novas a cada 2 s, ocupavam um
     * quarto dela (o medidor pesava no que mede). Letra por letra resolvia isso mas custava uma
     * chave e uma busca por letra: 23 ms por quadro no celular.
     */
    void linhasProprias(boolean ligado) {
        gl.textoDeLinha = ligado;
    }

    /** Só algarismos e os sinais que andam com eles (dano, combo, tempo, porcentagem). */
    private static boolean numero(String s) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch < '0' || ch > '9') && ch != '+' && ch != '-' && ch != '.' && ch != ',' && ch != '%' && ch != 'x') return false;
        }
        return true;
    }

    /**
     * Número (o dano que pula a cada golpe, o combo, o tempo): montado com um bitmap por
     * algarismo, como uma fonte de jogo. Inteiro, cada valor novo era um bitmap novo (e grande:
     * o dano do SHINKAI tem três camadas e cresce ao bater), e eles enchiam a textura dos textos
     * várias vezes por luta; a cada vez todos os textos da tela eram refeitos de uma vez.
     */
    private boolean textoPorAlgarismo(String s, float x, float y, Paint p) {
        if (s.length() < 2) {
            // Um algarismo só (o contador de combo até 9, o nível da barra): já é um glifo.
            glifo = numero(s);
            return false;
        }
        // Sem shader, ou com o degradê linear que a placa de vídeo põe por cima da forma (o do
        // contador de combo): ele vai pela posição na tela, igual letra por letra.
        Shader sh = p.getShader();
        if (sh != null && !(sh instanceof Degrade.Dados && !((Degrade.Dados)sh).radial())) return false;
        if (!numero(s)) return false;
        float total = 0f;
        for (int i = 0; i < s.length(); i++) total += p.measureText(caractere(s.charAt(i)));
        glifo = true;
        Paint.Align alinhamento = p.getTextAlign();
        float xi = alinhamento == Paint.Align.CENTER ? x - total * 0.5f : alinhamento == Paint.Align.RIGHT ? x - total : x;
        p.setTextAlign(Paint.Align.LEFT);
        for (int i = 0; i < s.length(); i++) {
            String ch = caractere(s.charAt(i));
            if (ch.charAt(0) != ' ') texto(ch, xi, y, p);
            xi += p.measureText(ch);
        }
        p.setTextAlign(alinhamento);
        glifo = false;
        return true;
    }

    /**
     * Desenhando letra por letra: o tamanho e a escala do bitmap vão em degraus de uma oitava,
     * sempre para cima (a placa de vídeo reduz até a metade). O número de dano que bate grande,
     * com a câmera aproximando, fazia cada algarismo em dezenas de tamanhos.
     */
    private boolean glifo;

    private static float meiaOitavaAcima(float v) {
        return (float)Math.pow(2.0, Math.ceil(Math.log(Math.max(1f, v)) / Math.log(2) * 2.0 - 1e-4) / 2.0);
    }

    private static float quartoDeOitava(float v) {
        if (v <= 0f) return 0f;
        return (float)Math.pow(2.0, Math.round(Math.log(v) / Math.log(2) * 4.0) / 4.0);
    }

    private void texto(String s, float x, float y, Paint p) {
        if (s == null || s.isEmpty()) return;
        boolean deFora = !glifo;
        if (textoPorAlgarismo(s, x, y, p)) return;
        try {
            textoInteiro(s, x, y, p);
        } finally {
            // O glifo de um caractere só vale para esta chamada (letra por letra, quem chama limpa).
            if (deFora) glifo = false;
        }
    }

    private void textoInteiro(String s, float x, float y, Paint p) {
        prepararGl();
        // Texto que pulsa pelo tamanho da fonte (o contador de golpes, a cada acerto) é feito num
        // degrau de tamanho e esticado por k no desenho: antes cada quadro era um bitmap novo.
        float tamanho = p.getTextSize();
        float base, k, esc;
        if (glifo) {
            // Um degrau só: o tamanho final em pixels da tela (fonte × escala), em meias oitavas
            // para cima; o bitmap é feito nele e a placa de vídeo reduz o resto.
            base = meiaOitavaAcima(tamanho * escala());
            k = tamanho / base;
            esc = 1f;
        } else {
            base = degrauDeTamanho(tamanho);
            k = tamanho / base;
            esc = degrauDeEscala(escala() * k);
        }
        // O contorno no tamanho do degrau, em degraus de 1/8 de oitava (~9%): tanto o proporcional
        // à fonte (o número de dano) quanto o fixo com a fonte pulsando (o contador de combo, 12 px)
        // ficam em um ou dois valores, e não num valor novo a cada quadro.
        float traco = glifo ? quartoDeOitava(p.getStrokeWidth() / k) : degrauDeContorno(p.getStrokeWidth() / k);
        Shader sh = p.getShader();
        // Degradê linear: o texto vira só a forma (branca) e o degradê é posto pela placa de vídeo.
        boolean mascara = sh instanceof Degrade.Dados && !((Degrade.Dados)sh).radial();
        int cor = p.getColor();
        // Atalho: o mesmo texto (o mesmo objeto String: rótulos fixos, os algarismos) com o mesmo
        // pincel de antes acha o bitmap comparando alguns números, sem montar a chave (que
        // escrevia vários floats a cada texto de cada quadro: 30% do HUD).
        boolean tingido = sh == null || mascara;
        int estilo = p.getStyle().ordinal(), alinhamento = p.getTextAlign().ordinal();
        float inclinacao = p.getTextSkewX(), largura = p.getTextScaleX();
        float espacamento = Build.VERSION.SDK_INT >= 21 ? p.getLetterSpacing() : 0f;
        boolean negrito = p.isFakeBoldText();
        android.graphics.Typeface fonte = p.getTypeface();
        Atalho atalho = tingido ? atalhos.get(s) : null;
        for (Atalho a = atalho; a != null; a = a.proximo) {
            if (a.base == base && a.esc == esc && a.traco == traco && a.estilo == estilo && a.alinhamento == alinhamento
                && a.inclinacao == inclinacao && a.largura == largura && a.espacamento == espacamento
                && a.negrito == negrito && a.fonte == fonte && a.mascara == mascara && gl.textoVale(a.texto.uv, a.texto.geracao)) {
                desenharTextoPronto(a.texto, x, y, k, mascara, sh, cor, p);
                return;
            }
        }
        chave.setLength(0);
        // A transparência da cor fica de fora: entra na hora de desenhar (o texto que some aos
        // poucos é o mesmo bitmap).
        // Sem shader o texto é feito branco e a cor entra no desenho (a placa de vídeo multiplica,
        // o resultado é o mesmo): um bitmap serve a todas as cores (o contorno escuro, a sombra
        // colorida e o branco do mesmo número; os rótulos do HUD em cada cor).
        chave.append(s).append('\u0000').append(base).append(',')
            .append(mascara || sh == null ? 0 : (cor | 0xFF000000)).append(',')
            .append(p.getStyle().ordinal()).append(',').append(traco).append(',')
            .append(p.getTextAlign().ordinal()).append(',').append(p.getTextSkewX()).append(',')
            .append(p.isFakeBoldText()).append(',').append(p.getTextScaleX()).append(',')
            .append(System.identityHashCode(p.getTypeface())).append(',').append(esc);
        if (Build.VERSION.SDK_INT >= 21) chave.append(',').append(p.getLetterSpacing());
        if (mascara) {
            chave.append(",M");
        } else if (sh != null) {
            // Outro shader: o desenho depende de onde o texto está.
            chave.append(',').append(System.identityHashCode(sh)).append(',').append(x).append(',').append(y);
        }
        String kt = chave.toString();
        Texto t = textos.get(kt);
        if (t != null && !gl.textoVale(t.uv, t.geracao)) t = null;
        String solta = null;
        boolean esticado = false;
        if (t == null) {
            chaveSolta.setLength(0);
            chaveSolta.append(s).append('\u0000').append(mascara || sh == null ? 0 : (cor | 0xFF000000)).append(',')
                .append(p.getStyle().ordinal()).append(',').append(p.getTextAlign().ordinal()).append(',')
                // A mesma fonte (o reaproveitado só muda de tamanho): com a cor fora da chave, o
                // "S" em negrito do HUD servia de "S" do relatório.
                .append(System.identityHashCode(p.getTypeface())).append(',').append(p.isFakeBoldText()).append(',')
                .append(p.getTextSkewX()).append(',').append(p.getTextScaleX()).append(',')
                .append(mascara ? "M" : sh != null ? String.valueOf(System.identityHashCode(sh)) : "");
            solta = chaveSolta.toString();
            Texto ultimo = ultimoDoTexto.get(solta);
            if (ultimo != null && gl.textoVale(ultimo.uv, ultimo.geracao)) {
                if (ultimo.base == base && ultimo.traco == traco && ultimo.esc >= esc * 0.999f && ultimo.esc <= esc * 2.01f) {
                    // O texto que entra grande e encolhe (o SHINKAI, a vitória, o nome do Ultra):
                    // o bitmap do quadro maior serve aos menores, a placa de vídeo reduz.
                    t = ultimo;
                } else if (textosNovos >= NOVOS_POR_QUADRO) {
                    t = ultimo;
                    k = tamanho / ultimo.base;
                    esticado = true;
                }
            }
        }
        if (t == null) {
            if (textosFalhos.contains(kt)) return;
            // O bitmap do texto é desenhado na thread dos textos; aqui ele só sobe para a placa
            // quando fica pronto. Desenhar na hora custava 2–5 ms por texto (o SHINKAI do corte, os
            // números do dano, o COMBO BREAKER!) e os quadros que faziam vários passavam de 40 ms.
            java.util.concurrent.Future<Texto> pedido = emPreparo.get(kt);
            if (pedido == null) {
                // Uma cópia só dela: a thread dos textos desenha com o pincel enquanto este muda.
                Paint feito = new Paint(p);
                if (mascara) {
                    feito.setShader(null);
                    feito.setColor(0xFFFFFFFF);
                } else if (sh == null) {
                    feito.setColor(0xFFFFFFFF);
                } else {
                    feito.setAlpha(255);
                }
                feito.setTextSize(base);
                feito.setStrokeWidth(traco);
                final String texto = s;
                final float px = x, py = y, pesc = esc;
                pedido = textosAFazer().submit(() -> rasterizar(texto, px, py, feito, pesc));
                if (emPreparo.size() > 256) emPreparo.clear();
                emPreparo.put(kt, pedido);
            }
            Texto pronto = null;
            if (pedido.isDone()) {
                emPreparo.remove(kt);
                try {
                    pronto = pedido.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException e) {
                    pronto = null;
                }
                if (pronto == null) {
                    if (textosFalhos.size() > 256) textosFalhos.clear();
                    textosFalhos.add(kt);
                    return;
                }
            }
            if (pronto != null) {
                TesteAutomatico.textoPedido(s);
                long antesDoTexto = System.nanoTime();
                boolean guardou = guardar(pronto);
                TesteAutomatico.tempoDeTexto(System.nanoTime() - antesDoTexto);
                if (!guardou) return;
                t = pronto;
                t.base = base;
                t.esc = esc;
                t.traco = traco;
                textosNovos++;
                textos.put(kt, t);
                if (ultimoDoTexto.size() > TEXTOS * 2) ultimoDoTexto.clear();
                ultimoDoTexto.put(solta, t);
            } else {
                // Ainda na thread dos textos: o mesmo texto em outro tamanho, esticado, ou nada
                // neste quadro (ele aparece no próximo ou no outro).
                textosAdiados++;
                Texto ultimo = ultimoDoTexto.get(solta);
                if (ultimo == null || !gl.textoVale(ultimo.uv, ultimo.geracao)) return;
                t = ultimo;
                k = tamanho / ultimo.base;
                esticado = true;
            }
        }
        // O reaproveitado esticado (passou dos textos novos do quadro) é provisório: fica fora do atalho.
        if (tingido && !esticado) guardarAtalho(s, atalho, t, base, esc, traco, estilo, alinhamento, inclinacao,
            largura, espacamento, negrito, fonte, mascara);
        desenharTextoPronto(t, x, y, k, mascara, sh, cor, p);
    }

    /** Até {@link #ATALHOS_POR_TEXTO} pincéis por texto (o "2" do combo e o do HUD no mesmo quadro). */
    private static final int ATALHOS_POR_TEXTO = 4;
    private final java.util.IdentityHashMap<String, Atalho> atalhos = new java.util.IdentityHashMap<>();

    private static final class Atalho {
        Texto texto;
        float base, esc, traco, inclinacao, largura, espacamento;
        int estilo, alinhamento;
        boolean negrito, mascara;
        android.graphics.Typeface fonte;
        Atalho proximo;
    }

    private void guardarAtalho(String s, Atalho primeiro, Texto t, float base, float esc, float traco, int estilo,
                               int alinhamento, float inclinacao, float largura, float espacamento, boolean negrito,
                               android.graphics.Typeface fonte, boolean mascara) {
        // Textos que mudam a cada quadro (outro objeto String a cada vez) não podem crescer a tabela.
        if (atalhos.size() > 1024) atalhos.clear();
        Atalho a = new Atalho();
        a.texto = t; a.base = base; a.esc = esc; a.traco = traco; a.estilo = estilo; a.alinhamento = alinhamento;
        a.inclinacao = inclinacao; a.largura = largura; a.espacamento = espacamento; a.negrito = negrito;
        a.fonte = fonte; a.mascara = mascara;
        // O novo na frente; a corrente fica com no máximo ATALHOS_POR_TEXTO.
        a.proximo = primeiro;
        int n = 1;
        for (Atalho x = a; x.proximo != null; x = x.proximo) {
            if (++n > ATALHOS_POR_TEXTO) {
                x.proximo = null;
                break;
            }
        }
        atalhos.put(s, a);
    }

    private void desenharTextoPronto(Texto t, float x, float y, float k, boolean mascara, Shader sh, int cor, Paint p) {
        float l = x - t.origemX * k, top = y - t.origemY * k, r = l + t.largura * k, b = top + t.altura * k;
        if (mascara) {
            pincel(p);
            uv(l, top);
            float t0 = uvU;
            uv(r, top);
            float t1 = uvU;
            uv(r, b);
            float t2 = uvU;
            uv(l, b);
            gl.desenharTextoDegrade(t.uv, l, top, r, b, t0, t1, t2, uvU, tipoPincel, p.getAlpha());
            return;
        }
        gl.desenharTexto(t.uv, l, top, r, b, sh == null ? cor : (p.getAlpha() << 24) | 0xFFFFFF);
    }

    /** A thread que desenha os bitmaps dos textos (só CPU: a placa de vídeo fica nesta). */
    private static java.util.concurrent.ExecutorService textosAFazer;
    private final java.util.HashMap<String, java.util.concurrent.Future<Texto>> emPreparo = new java.util.HashMap<>();
    private final java.util.HashSet<String> textosFalhos = new java.util.HashSet<>();

    static synchronized java.util.concurrent.ExecutorService textosAFazer() {
        if (textosAFazer == null) {
            textosAFazer = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread th = new Thread(r, "Textos");
                th.setDaemon(true);
                return th;
            });
        }
        return textosAFazer;
    }

    /** Sobe o bitmap desenhado na thread dos textos para a textura dos textos (nesta thread). */
    private boolean guardar(Texto texto) {
        Bitmap bmp = texto.bitmap;
        texto.bitmap = null;
        if (bmp == null) return false;
        texto.uv = gl.guardarTexto(bmp);
        if (texto.uv == null) return false;
        texto.geracao = gl.geracaoTexto;
        return true;
    }

    /** O bitmap do texto, desenhado em qualquer thread (não mexe no OpenGL). Null se não dá. */
    private static Texto rasterizar(String s, float x, float y, Paint p, float esc) {
        float w = p.measureText(s);
        Paint.FontMetrics fm = p.getFontMetrics();
        float margem = p.getTextSize() * 0.35f + p.getStrokeWidth() + 2f;
        float esquerda;
        switch (p.getTextAlign()) {
            case CENTER: esquerda = x - w * 0.5f; break;
            case RIGHT: esquerda = x - w; break;
            default: esquerda = x; break;
        }
        float l = esquerda - margem, t = y + fm.top - margem;
        float lw = w + margem * 2f, lh = (fm.bottom - fm.top) + margem * 2f;
        // Texto enorme (o número que bate grande) num bitmap de no máximo ~0,4 Mpx: a placa de
        // vídeo estica o resto. Poucos assim enchiam a textura dos textos, que era refeita.
        float px = lw * lh * esc * esc;
        if (px > MAX_PIXELS_TEXTO) esc *= (float)Math.sqrt(MAX_PIXELS_TEXTO / px);
        // E cabendo na textura dos textos (2048): maior que ela, o texto nem aparecia e era feito
        // de novo a cada quadro.
        esc = Math.min(esc, Math.min(2000f / Math.max(1f, lw), 2000f / Math.max(1f, lh)));
        int bw = (int)Math.ceil(lw * esc), bh = (int)Math.ceil(lh * esc);
        if (bw <= 0 || bh <= 0 || bw > 4096 || bh > 4096) return null;
        Bitmap bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        cv.scale(esc, esc);
        cv.translate(-l, -t);
        cv.drawText(s, x, y, p);
        Texto texto = new Texto();
        texto.bitmap = bmp;
        texto.origemX = x - l;
        texto.origemY = y - t;
        texto.largura = bw / esc;
        texto.altura = bh / esc;
        return texto;
    }

    // ---- Como RenderCanvas: as transformações no Canvas, o desenho no OpenGL ----

    private final class ComoRenderCanvas implements RenderCanvas {
        @Override public void save() { CanvasGl.this.save(); }
        @Override public void restore() { CanvasGl.this.restore(); }
        @Override public void translate(float dx, float dy) { CanvasGl.this.translate(dx, dy); }
        @Override public void scale(float sx, float sy, float px, float py) { CanvasGl.this.scale(sx, sy, px, py); }
        @Override public void rotate(float graus, float px, float py) { CanvasGl.this.rotate(graus, px, py); }
        @Override public void clipPolygon(float[] xy) { recortarPoligono(xy, xy.length / 2); }

        @Override public void fillRect(float l, float t, float r, float b, int argb) { prepararGl(); gl.fillRect(l, t, r, b, argb); }
        @Override public void fillPolygon(float[] xy, int argb) { prepararGl(); gl.fillPolygon(xy, argb); }
        @Override public void strokePolygon(float[] xy, float w, int argb) { prepararGl(); gl.strokePolygon(xy, w, argb); }
        @Override public void fillCircle(float cx, float cy, float r, int argb) { prepararGl(); gl.fillCircle(cx, cy, r, argb); }
        @Override public void strokeCircle(float cx, float cy, float r, float w, int argb) { prepararGl(); gl.strokeCircle(cx, cy, r, w, argb); }
        @Override public void drawLine(float x1, float y1, float x2, float y2, float w, int argb) { prepararGl(); gl.drawLine(x1, y1, x2, y2, w, argb); }
        @Override public void drawImage(RenderImage im, float l, float t, float r, float b, int a) { prepararGl(); gl.drawImage(im, l, t, r, b, a); }

        @Override public void drawImageRegion(RenderImage im, float sl, float st, float sr, float sb,
                                              float l, float t, float r, float b, int a) {
            prepararGl();
            gl.drawImageRegion(im, sl, st, sr, sb, l, t, r, b, a);
        }

        @Override public boolean desenhaFaixas() { return true; }

        @Override public void drawImageStrip(RenderImage im, float[] xy, float[] uv, int linhas, int a) {
            prepararGl();
            gl.drawImageStrip(im, xy, uv, linhas, a);
        }

        @Override public void fillRadialGlow(float cx, float cy, float r, int argb) { prepararGl(); gl.fillRadialGlow(cx, cy, r, argb); }

        @Override public void fillVerticalGradient(float l, float t, float r, float b, int cima, int baixo) {
            prepararGl();
            gl.fillVerticalGradient(l, t, r, b, cima, baixo);
        }

        @Override public void fillOval(float l, float t, float r, float b, int argb) { prepararGl(); gl.fillOval(l, t, r, b, argb); }

        @Override public void drawText(String text, float x, float y, float size, int fill, int stroke, float sw, boolean italic) {
            prepararGl();
            gl.drawText(text, x, y, size, fill, stroke, sw, italic);
        }

        @Override public float measureText(String text, float size, boolean italic) { return gl.measureText(text, size, italic); }

        @Override public void beginInvert() {
            CanvasGl.this.save();
            int k = getSaveCount();
            invertidos[k] = !invertidos[k];
        }

        @Override public void endInvert() { CanvasGl.this.restore(); }
    }
}
