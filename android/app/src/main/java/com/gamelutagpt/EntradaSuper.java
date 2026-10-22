package com.gamelutagpt;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
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
 * Entrada do Super: o retrato do lutador, vindo de um vídeo no verde, passa ao fundo da tela
 * enquanto ele carrega o Super, grudado no canto de baixo do lado em que ele está; quando o
 * retrato volta para a borda e some, o Super sai. Cada lutador que tem a arte põe uma pasta em
 * {@code assets/supers/<id>/} (super.json e os quadros q00.webp..., feitos pelo
 * tools/sprites/super_video.py); quem não tem fica com o Super de sempre, sem mexer no código.
 *
 * Só apresentação: o tempo da entrada vai para o motor ({@link CombatEngine#setSuperIntro}), que
 * congela a luta por ele. A arte é lida na montagem da luta, só a dos lutadores dela.
 */
final class EntradaSuper {
    private static final String LOG_TAG = "EntradaSuper";
    private static final String PASTA = "supers";
    private static final float MS_POR_PASSO = 1000f / 60f;
    /** Na saída o retrato volta esse tanto da largura do vídeo para a borda. */
    private static final float RECUO_SAIDA = 0.35f;

    /** A entrada de um lutador: os quadros e onde cada um fica (px da tela, do canto de baixo). */
    static final class Pacote {
        final Bitmap[] quadros;
        /** x, y (topo, negativo: acima do chão da tela), largura e altura de cada quadro. */
        final float[][] caixas;
        final float quadroMs, segurarMs, saidaMs, larguraVideo;

        Pacote(Bitmap[] quadros, float[][] caixas, float quadroMs, float segurarMs, float saidaMs, float larguraVideo) {
            this.quadros = quadros;
            this.caixas = caixas;
            this.quadroMs = quadroMs;
            this.segurarMs = segurarMs;
            this.saidaMs = saidaMs;
            this.larguraVideo = larguraVideo;
        }

        float duracaoMs() {
            return quadros.length * quadroMs + segurarMs + saidaMs;
        }

        /** Passos de 60 Hz que a luta fica parada. */
        int passos() {
            return (int)Math.ceil(duracaoMs() / MS_POR_PASSO);
        }
    }

    private final AssetManager assets;
    /** Trocado inteiro ao carregar (a montagem roda fora da thread do desenho). */
    private volatile Map<String, Pacote> pacotes = new HashMap<>();
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final RectF destino = new RectF();

    EntradaSuper(AssetManager assets) {
        this.assets = assets;
    }

    /**
     * Deixa carregada só a entrada dos lutadores {@code ids} (os da luta) e passa o tempo de
     * cada uma ao motor. Quem tem a pasta e já estava carregado não é lido de novo.
     */
    synchronized void manter(CombatEngine engine, String... ids) {
        Map<String, Pacote> antes = pacotes, novos = new HashMap<>();
        for (String id : ids) {
            if (id == null || novos.containsKey(id)) continue;
            Pacote p = antes.get(id);
            if (p == null) {
                try {
                    p = ler(id);
                } catch (Exception e) {
                    Log.w(LOG_TAG, "entrada do Super de " + id + " não carregou", e);
                }
            }
            if (p != null) novos.put(id, p);
        }
        pacotes = novos;
        engine.clearSuperIntros();
        for (Map.Entry<String, Pacote> e : novos.entrySet()) engine.setSuperIntro(e.getKey(), e.getValue().passos());
    }

    private Pacote ler(String id) throws Exception {
        String base = PASTA + "/" + id + "/";
        JSONObject dados;
        try (InputStream in = assets.open(base + "super.json")) {
            dados = new JSONObject(texto(in));
        } catch (IOException semPasta) {
            return null;
        }
        JSONArray lista = dados.getJSONArray("quadros");
        int n = lista.length();
        if (n == 0) return null;
        Bitmap[] quadros = new Bitmap[n];
        float[][] caixas = new float[n][4];
        for (int i = 0; i < n; i++) {
            JSONObject q = lista.getJSONObject(i);
            try (InputStream in = assets.open(base + q.getString("imagem"))) {
                quadros[i] = BitmapFactory.decodeStream(in);
            }
            if (quadros[i] == null) throw new IOException("quadro " + q.getString("imagem") + " ilegível");
            caixas[i][0] = (float)q.getDouble("x");
            caixas[i][1] = (float)q.getDouble("y");
            caixas[i][2] = (float)q.getDouble("largura");
            caixas[i][3] = (float)q.getDouble("altura");
        }
        return new Pacote(quadros, caixas, (float)dados.getDouble("quadroMs"), (float)dados.optDouble("segurarMs", 0),
            (float)dados.optDouble("saidaMs", 150), (float)dados.optDouble("larguraVideo", Arena.VW));
    }

    private static String texto(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int lidos;
        while ((lidos = in.read(buffer)) != -1) out.write(buffer, 0, lidos);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    boolean tem(String id) {
        return pacotes.containsKey(id);
    }

    /**
     * Desenha a entrada de {@code id} no passo {@code idade} dela, no espaço da tela (VW x VH):
     * grudada no canto de baixo à esquerda, ou espelhada à direita.
     */
    void desenhar(Canvas c, String id, int idade, boolean esquerda) {
        Pacote p = pacotes.get(id);
        if (p == null) return;
        float ms = idade * MS_POR_PASSO;
        int i = Math.min(p.quadros.length - 1, Math.max(0, (int)(ms / p.quadroMs)));
        float saida = p.duracaoMs() - p.saidaMs;
        float recuo = 0f;
        int alfa = 255;
        if (ms > saida && p.saidaMs > 0f) {
            // Saída: volta para a borda acelerando e some; aí o Super sai.
            float k = Math.min(1f, (ms - saida) / p.saidaMs);
            recuo = k * k * RECUO_SAIDA * p.larguraVideo;
            alfa = Math.round(255f * (1f - k));
        }
        if (alfa <= 0) return;
        float[] r = p.caixas[i];
        float x = r[0] - recuo, y = Arena.VH + r[1];
        destino.set(x, y, x + r[2], y + r[3]);
        paint.setAlpha(alfa);
        if (esquerda) {
            c.drawBitmap(p.quadros[i], null, destino, paint);
        } else {
            c.save();
            c.scale(-1f, 1f, Arena.VW * 0.5f, 0f);
            c.drawBitmap(p.quadros[i], null, destino, paint);
            c.restore();
        }
    }
}
