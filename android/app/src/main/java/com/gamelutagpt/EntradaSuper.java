package com.gamelutagpt;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
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
 * Entrada do Super: um quadro de página de mangá bate na tela, do lado em que o lutador está, e
 * dentro dele o retrato (vídeo no verde) entra sobre linhas de concentração e retícula na cor do
 * lutador, enquanto ele carrega o Super; quando o quadro sai pela borda, o Super sai. Cada lutador que tem a arte põe uma pasta em
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
    /** Tinta e papel da página do Ultra (ArteProvisoria). */
    private static final int TINTA = 0xFF111114, PAPEL = 0xFFFFFFFF;
    /**
     * O quadro no lado esquerdo da tela (cima-esq., cima-dir., baixo-dir., baixo-esq.), entre o
     * HUD de cima e o de baixo, com o lado de dentro inclinado como num corte de página. Do
     * lado direito ele é espelhado.
     */
    private static final float[] QUADRO = {24f, 126f, 552f, 100f, 500f, 638f, 14f, 652f};
    private static final float Q_ESQ = 14f, Q_DIR = 552f, Q_CIMA = 100f, Q_BAIXO = 652f;
    /** O quadro do vídeo um pouco mais alto que o painel (o pé fica no fundo dele). */
    private static final float SOBRA_VIDEO = 1.04f;
    /** Onde as linhas de concentração convergem: o rosto, em fração do quadro do vídeo. */
    private static final float FOCO_X = 0.40f, FOCO_Y = 0.30f;
    private static final int LINHAS = 46;
    /** O quadro bate (desliza da borda) e pisca branco ao pousar. */
    private static final float BATIDA_MS = 160f, CLARAO_MS = 120f;

    /** A entrada de um lutador: os quadros e onde cada um fica (px da tela, do canto de baixo). */
    static final class Pacote {
        final Bitmap[] quadros;
        /** x, y (topo, negativo: acima do chão da tela), largura e altura de cada quadro. */
        final float[][] caixas;
        final float quadroMs, segurarMs, saidaMs, larguraVideo, alturaTela;

        Pacote(Bitmap[] quadros, float[][] caixas, float quadroMs, float segurarMs, float saidaMs, float larguraVideo,
               float alturaTela) {
            this.quadros = quadros;
            this.caixas = caixas;
            this.quadroMs = quadroMs;
            this.segurarMs = segurarMs;
            this.saidaMs = saidaMs;
            this.larguraVideo = larguraVideo;
            this.alturaTela = alturaTela;
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
            (float)dados.optDouble("saidaMs", 150), (float)dados.optDouble("larguraVideo", Arena.VW),
            (float)dados.optDouble("alturaTela", 1.0));
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

    private final Path quadro = new Path(), linha = new Path();
    private final Paint tinta = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RadialGradient reticula;
    private int corReticula;
    private float focoReticula = Float.NaN;

    {
        quadro.moveTo(QUADRO[0], QUADRO[1]);
        for (int i = 2; i < QUADRO.length; i += 2) quadro.lineTo(QUADRO[i], QUADRO[i + 1]);
        quadro.close();
        tinta.setStrokeJoin(Paint.Join.MITER);
    }

    /**
     * Desenha a entrada de {@code id} no passo {@code idade} dela, no espaço da tela (VW x VH):
     * o quadro de mangá na borda esquerda, ou espelhado na direita. {@code cor}: a do lutador
     * (a retícula do fundo do quadro).
     */
    void desenhar(Canvas c, String id, int idade, boolean esquerda, int cor) {
        Pacote p = pacotes.get(id);
        if (p == null) return;
        float ms = idade * MS_POR_PASSO;
        // Entra deslizando da borda e assenta; na saída volta para a borda acelerando.
        float fora = Q_DIR + 60f, dx = 0f;
        if (ms < BATIDA_MS) {
            float k = 1f - ms / BATIDA_MS;
            dx = -fora * k * k * k;
        }
        float saida = p.duracaoMs() - p.saidaMs;
        if (ms > saida && p.saidaMs > 0f) {
            float k = Math.min(1f, (ms - saida) / p.saidaMs);
            dx = -fora * k * k;
        }
        c.save();
        if (!esquerda) c.scale(-1f, 1f, Arena.VW * 0.5f, 0f);
        c.translate(dx, 0f);
        // Sombra, depois o miolo recortado no quadro: papel, retícula, linhas e o retrato.
        c.save();
        c.translate(10f, 12f);
        tinta.setStyle(Paint.Style.FILL);
        tinta.setShader(null);
        tinta.setColor(0x77000000);
        c.drawPath(quadro, tinta);
        c.restore();
        c.save();
        c.clipPath(quadro);
        float s = (Q_BAIXO - Q_CIMA) * SOBRA_VIDEO / (p.alturaTela * Arena.VH);
        float altVideo = p.alturaTela * Arena.VH * s;
        float fx = Q_ESQ + p.larguraVideo * s * FOCO_X, fy = Q_BAIXO - altVideo * (1f - FOCO_Y);
        desenharFundo(c, cor, fx, fy, idade);
        int i = Math.min(p.quadros.length - 1, Math.max(0, (int)(ms / p.quadroMs)));
        float[] r = p.caixas[i];
        float x = Q_ESQ + r[0] * s, y = Q_BAIXO + r[1] * s;
        destino.set(x, y, x + r[2] * s, y + r[3] * s);
        paint.setAlpha(255);
        c.drawBitmap(p.quadros[i], null, destino, paint);
        if (ms >= BATIDA_MS && ms < BATIDA_MS + CLARAO_MS) {
            // Clarão quando o quadro pousa, como os painéis da página do Ultra.
            tinta.setStyle(Paint.Style.FILL);
            tinta.setColor(Color.argb(Math.round(200f * (1f - (ms - BATIDA_MS) / CLARAO_MS)), 255, 255, 255));
            c.drawRect(Q_ESQ, Q_CIMA, Q_DIR, Q_BAIXO, tinta);
        }
        c.restore();
        // Moldura: a margem branca da página e o traço de tinta.
        tinta.setStyle(Paint.Style.STROKE);
        tinta.setColor(PAPEL);
        tinta.setStrokeWidth(18f);
        c.drawPath(quadro, tinta);
        tinta.setColor(TINTA);
        tinta.setStrokeWidth(7f);
        c.drawPath(quadro, tinta);
        c.restore();
    }

    /**
     * Papel com a retícula na cor do lutador (clara no meio, forte na borda) e linhas de
     * concentração em tinta, afinando até o rosto; elas tremem a cada 3 passos.
     */
    private void desenharFundo(Canvas c, int cor, float fx, float fy, int idade) {
        tinta.setStyle(Paint.Style.FILL);
        tinta.setShader(null);
        tinta.setColor(PAPEL);
        c.drawRect(Q_ESQ, Q_CIMA, Q_DIR, Q_BAIXO, tinta);
        if (reticula == null || cor != corReticula || fx + fy != focoReticula) {
            corReticula = cor;
            focoReticula = fx + fy;
            reticula = new RadialGradient(fx, fy, 520f,
                new int[] {Color.argb(0, Color.red(cor), Color.green(cor), Color.blue(cor)),
                    Color.argb(90, Color.red(cor), Color.green(cor), Color.blue(cor)),
                    Color.argb(210, Color.red(cor), Color.green(cor), Color.blue(cor))},
                new float[] {0.25f, 0.6f, 1f}, Shader.TileMode.CLAMP);
        }
        tinta.setShader(reticula);
        c.drawRect(Q_ESQ, Q_CIMA, Q_DIR, Q_BAIXO, tinta);
        tinta.setShader(null);
        tinta.setColor(TINTA);
        long semente = (idade / 3) * 2654435761L + 12345L;
        for (int i = 0; i < LINHAS; i++) {
            semente = semente * 6364136223846793005L + 1442695040888963407L;
            float r1 = ((semente >>> 33) & 0xFFFF) / 65535f;
            float r2 = ((semente >>> 17) & 0xFFFF) / 65535f;
            double a = (i + 0.8 * (r1 - 0.5)) * 2.0 * Math.PI / LINHAS;
            float cos = (float)Math.cos(a), sin = (float)Math.sin(a);
            float perto = 190f + 150f * r2, longe = 900f;
            float meia = 2.5f + 9f * r1;
            linha.rewind();
            linha.moveTo(fx + cos * perto, fy + sin * perto);
            linha.lineTo(fx + cos * longe - sin * meia, fy + sin * longe + cos * meia);
            linha.lineTo(fx + cos * longe + sin * meia, fy + sin * longe - cos * meia);
            linha.close();
            c.drawPath(linha, tinta);
        }
    }
}
