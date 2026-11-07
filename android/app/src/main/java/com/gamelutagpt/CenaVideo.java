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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A cena de vídeo de uma cinemática (ação {@code video} do motor de cinemáticas): os dois
 * lutadores num vídeo só, trocando golpes, caindo, o soco final. Cada clipe é uma folha em
 * {@code assets/historia/<episódio>/<nome>/} (folha.webp e efeito.json, do
 * {@code tools/sprites/efeito_video.py --saida}), com a origem no chão, no meio dos dois.
 *
 * Enquanto o vídeo toca, o jogo esconde os lutadores de verdade e desenha o quadro do vídeo no
 * chão da luta, entre eles (a câmera, as faixas, os letreiros e o QTE continuam por cima). O
 * vídeo espelha quando quem está à esquerda nele está à direita na luta: a mesma cena vale para
 * as duas campanhas.
 *
 * As folhas são lidas numa thread à parte quando a luta carrega a cinemática (são grandes) e
 * soltas quando ela descarrega. Só apresentação: o tempo de cada quadro vem da cinemática.
 */
final class CenaVideo {
    private static final String LOG_TAG = "CenaVideo";
    private static final String PASTA = "historia";
    /** O vídeo não sai da arena: a origem fica a pelo menos isso das bordas (meia largura da cena). */
    static final float MARGEM = 200f;

    /** Uma folha lida: os quadros (x, y, l, a em px da folha), o deslocamento até a origem e o fim de cada um (ms). */
    static final class Folha {
        final Bitmap bitmap;
        final float pixelScale;
        final int[][] quadros;
        final float[][] desloc;
        final float[] fim;

        Folha(Bitmap bitmap, float pixelScale, int[][] quadros, float[][] desloc, float[] fim) {
            this.bitmap = bitmap;
            this.pixelScale = pixelScale;
            this.quadros = quadros;
            this.desloc = desloc;
            this.fim = fim;
        }

        /** O quadro em {@code ms} desde o começo (o último, depois do fim). */
        int quadroEm(float ms) {
            for (int i = 0; i < fim.length; i++) if (ms < fim[i]) return i;
            return fim.length - 1;
        }
    }

    private final AssetManager assets;
    private final Map<String, Folha> folhas = new ConcurrentHashMap<>();
    /** Aumenta a cada carga: a thread de uma carga antiga não grava folhas na nova. */
    private volatile int geracao;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Rect fonte = new Rect();
    private final RectF destino = new RectF();

    CenaVideo(AssetManager assets) {
        this.assets = assets;
    }

    /** Lê as folhas dos clipes ({@code ep00/qt1}...) numa thread à parte; esquece as da luta anterior. */
    void carregar(List<String> clipes) {
        soltar();
        if (assets == null || clipes.isEmpty()) return;
        final int minha = geracao;
        Thread t = new Thread(() -> {
            for (String clipe : clipes) {
                if (geracao != minha) return;
                try {
                    Folha f = ler(clipe);
                    if (f != null && geracao == minha) folhas.put(clipe, f);
                } catch (Exception e) {
                    Log.w(LOG_TAG, "cena " + clipe + " não carregou", e);
                }
            }
        }, "cena-video");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    /** Solta as folhas (a luta acabou ou trocou de cinemática). */
    void soltar() {
        geracao++;
        folhas.clear();
    }

    /** A folha do clipe já está pronta para desenhar. */
    boolean pronta(String clipe) {
        return clipe != null && folhas.containsKey(clipe);
    }

    private Folha ler(String clipe) throws Exception {
        String base = PASTA + "/" + clipe + "/";
        JSONObject dados;
        try (InputStream in = assets.open(base + "efeito.json")) {
            dados = new JSONObject(texto(in));
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
        return new Folha(bitmap, (float)dados.optDouble("pixelScale", 1.0), quadros, desloc, fim);
    }

    private static String texto(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int lidos;
        while ((lidos = in.read(buffer)) != -1) out.write(buffer, 0, lidos);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    /** Onde fica a origem do vídeo: no meio dos dois, sem a cena sair da arena. */
    static float origemX(float xa, float xb) {
        return Math.max(Arena.LEFT_BOUND + MARGEM, Math.min(Arena.RIGHT_BOUND - MARGEM, (xa + xb) * 0.5f));
    }

    /**
     * Desenha o quadro de {@code ms} do clipe no espaço do mundo (o canvas já com a câmera), com a
     * origem em ({@code x}, {@code y}), espelhado na horizontal se pedido. Falso se a folha
     * ainda não está pronta (quem chama desenha os lutadores no lugar).
     */
    boolean desenhar(Canvas c, String clipe, float ms, float x, float y, boolean espelhar) {
        Folha f = clipe != null ? folhas.get(clipe) : null;
        if (f == null || f.quadros.length == 0) return false;
        int q = f.quadroEm(ms);
        int[] r = f.quadros[q];
        fonte.set(r[0], r[1], r[0] + r[2], r[1] + r[3]);
        float k = 1f / f.pixelScale;
        float left = f.desloc[q][0] * k, top = f.desloc[q][1] * k;
        destino.set(x + left, y + top, x + left + r[2] * k, y + top + r[3] * k);
        if (espelhar) {
            c.save();
            c.scale(-1f, 1f, x, 0f);
            c.drawBitmap(f.bitmap, fonte, destino, paint);
            c.restore();
        } else {
            c.drawBitmap(f.bitmap, fonte, destino, paint);
        }
        return true;
    }
}
