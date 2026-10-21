package com.gamelutagpt;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Efeitos vindos de vídeo: cada um é uma folha em {@code assets/efeitos/<nome>/} (folha.webp
 * e efeito.json, feitos pelo tools/sprites/efeito_video.py a partir de um vídeo do efeito
 * sozinho no verde). Tocar um efeito põe a origem dele num ponto do mundo; ele anda no relógio
 * de 60 Hz e some no último quadro. Só apresentação.
 *
 * Um efeito que não existe nos assets não toca ({@link #tocar} devolve false): quem chama usa
 * o efeito clássico no lugar. Assim cada vídeo novo entra sozinho, sem mexer no código. As
 * folhas são lidas uma vez ({@link #carregar}, junto com a arte da luta).
 */
final class EfeitosVideo {
    private static final String LOG_TAG = "EfeitosVideo";
    private static final String PASTA = "efeitos";
    private static final int VAGAS = 32;
    private static final float MS_POR_PASSO = 1000f / 60f;

    /** Uma folha carregada: os quadros (x, y, l, a, dx, dy em pixels da folha) e o tempo. */
    static final class Folha {
        final String nome;
        final Bitmap bitmap;
        final float pixelScale;
        final int[][] quadros;
        final float[][] desloc;
        /** Fim de cada quadro (ms desde o começo). */
        final float[] fim;

        Folha(String nome, Bitmap bitmap, float pixelScale, int[][] quadros, float[][] desloc, float[] fim) {
            this.nome = nome;
            this.bitmap = bitmap;
            this.pixelScale = pixelScale;
            this.quadros = quadros;
            this.desloc = desloc;
            this.fim = fim;
        }

        float duracaoMs() { return fim.length == 0 ? 0f : fim[fim.length - 1]; }

        int quadroEm(float ms) {
            for (int i = 0; i < fim.length; i++) if (ms < fim[i]) return i;
            return -1;
        }
    }

    private final AssetManager assets;
    /** Trocado inteiro ao carregar (a carga pode vir de outra thread que a do desenho). */
    private volatile Map<String, Folha> folhas = new HashMap<>();
    private boolean carregou;

    private final Folha[] tocando = new Folha[VAGAS];
    private final float[] x = new float[VAGAS], y = new float[VAGAS], escala = new float[VAGAS], idadeMs = new float[VAGAS];
    private final boolean[] espelhado = new boolean[VAGAS];
    private int proxima;

    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Rect fonte = new Rect();
    private final RectF destino = new RectF();

    EfeitosVideo(AssetManager assets) {
        this.assets = assets;
    }

    /** Lê todas as folhas de {@code assets/efeitos} (na montagem da luta, não no meio dela). */
    synchronized void carregar() {
        if (carregou || assets == null) return;
        carregou = true;
        String[] nomes;
        try {
            nomes = assets.list(PASTA);
        } catch (IOException e) {
            return;
        }
        if (nomes == null) return;
        Map<String, Folha> lidas = new HashMap<>();
        for (String nome : nomes) {
            try {
                Folha f = ler(nome);
                if (f != null) lidas.put(nome, f);
            } catch (Exception e) {
                Log.w(LOG_TAG, "efeito " + nome + " não carregou", e);
            }
        }
        folhas = lidas;
    }

    private Folha ler(String nome) throws Exception {
        String base = PASTA + "/" + nome + "/";
        JSONObject dados;
        try (InputStream in = assets.open(base + "efeito.json")) {
            dados = new JSONObject(texto(in));
        } catch (IOException semJson) {
            return null;
        }
        Bitmap bitmap;
        try (InputStream in = assets.open(base + "folha.webp")) {
            bitmap = BitmapFactory.decodeStream(in);
        }
        if (bitmap == null) return null;
        JSONArray q = dados.getJSONArray("quadros"), ms = dados.getJSONArray("duracoesMs");
        int n = q.length();
        int[][] quadros = new int[n][4];
        float[][] desloc = new float[n][2];
        float[] fim = new float[n];
        float t = 0f;
        for (int i = 0; i < n; i++) {
            JSONArray a = q.getJSONArray(i);
            for (int j = 0; j < 4; j++) quadros[i][j] = a.getInt(j);
            desloc[i][0] = (float)a.getDouble(4);
            desloc[i][1] = (float)a.getDouble(5);
            t += (float)ms.getDouble(Math.min(i, ms.length() - 1));
            fim[i] = t;
        }
        return new Folha(nome, bitmap, (float)dados.optDouble("pixelScale", 1.0), quadros, desloc, fim);
    }

    private static String texto(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int lidos;
        while ((lidos = in.read(buffer)) != -1) out.write(buffer, 0, lidos);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    /** Põe uma folha pronta (testes e ferramentas). */
    void adicionar(Folha f) {
        Map<String, Folha> novas = new HashMap<>(folhas);
        novas.put(f.nome, f);
        folhas = novas;
    }

    /** O efeito existe nos assets (depois de {@link #carregar}). */
    boolean tem(String nome) {
        return folhas.containsKey(nome);
    }

    /**
     * Toca o efeito com a origem em (x, y) do mundo, no tamanho {@code escala} (1 = o do
     * conversor) e espelhado na horizontal se pedido (o vídeo é feito para quem olha para a
     * direita). Devolve false se o efeito não existe.
     */
    boolean tocar(String nome, float px, float py, float tamanho, boolean espelhar) {
        Folha f = folhas.get(nome);
        if (f == null || f.quadros.length == 0) return false;
        int i = proxima;
        proxima = (proxima + 1) % VAGAS;
        tocando[i] = f;
        x[i] = px;
        y[i] = py;
        escala[i] = tamanho;
        espelhado[i] = espelhar;
        idadeMs[i] = 0f;
        return true;
    }

    /** Um passo de 60 Hz. */
    void passo() {
        for (int i = 0; i < VAGAS; i++) {
            if (tocando[i] == null) continue;
            idadeMs[i] += MS_POR_PASSO;
            if (idadeMs[i] >= tocando[i].duracaoMs()) tocando[i] = null;
        }
    }

    void limpar() {
        java.util.Arrays.fill(tocando, null);
    }

    /** Quantos estão tocando (testes). */
    int tocando() {
        int n = 0;
        for (Folha f : tocando) if (f != null) n++;
        return n;
    }

    /** Desenha no espaço do mundo (o canvas já com a câmera), do mais velho para o mais novo. */
    void desenhar(Canvas c) {
        for (int j = 0; j < VAGAS; j++) {
            int i = (proxima + j) % VAGAS;
            Folha f = tocando[i];
            if (f == null) continue;
            int q = f.quadroEm(idadeMs[i]);
            if (q < 0) continue;
            int[] r = f.quadros[q];
            fonte.set(r[0], r[1], r[0] + r[2], r[1] + r[3]);
            float k = escala[i] / f.pixelScale;
            float left = f.desloc[q][0] * k, top = f.desloc[q][1] * k;
            destino.set(x[i] + left, y[i] + top, x[i] + left + r[2] * k, y[i] + top + r[3] * k);
            if (espelhado[i]) {
                c.save();
                c.scale(-1f, 1f, x[i], 0f);
                c.drawBitmap(f.bitmap, fonte, destino, paint);
                c.restore();
            } else {
                c.drawBitmap(f.bitmap, fonte, destino, paint);
            }
        }
    }
}
