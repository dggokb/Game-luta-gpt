package com.gamelutagpt;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;

/**
 * O corte de ativação do SHINKAI, no estilo dos animes: uma faixa inclinada de página de mangá
 * atravessa a tela durante o congelamento, com os olhos do lutador (o rosto do HUD) do lado dele,
 * linhas de velocidade na cor do tipo e "SHINKAI" grande com o tipo embaixo. Não usa animação
 * nova: só o retrato que o HUD já tem. Espaço da tela (1280 × 720), por cima da luta e por baixo
 * do HUD.
 */
final class CorteShinkai {
    /** Quadros do corte: entra, segura e sai (o mesmo que {@link CombatConfig#shinkaiFlashFrames}). */
    static final int QUADROS = 48;
    private static final int ENTRA = 7, SAI = 8;
    /** A faixa: em cima à esquerda, em cima à direita (mais alto), embaixo à direita, embaixo à esquerda. */
    private static final float ESQ_CIMA = 232f, DIR_CIMA = 176f, ALTURA = 196f;
    /** O pedaço do rosto que aparece (dos olhos ao nariz), em fração da altura do retrato. */
    private static final float OLHOS_DE = 0.24f, OLHOS_ATE = 0.60f;
    private static final int LINHAS = 34;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path faixa = new Path();
    private final Rect fonte = new Rect();
    private final RectF destino = new RectF();
    private final float[] linhaY = new float[LINHAS], linhaX = new float[LINHAS], linhaL = new float[LINHAS];

    private int quadro = QUADROS;
    private boolean direita;
    private Bitmap rosto;
    private int cor;
    private String tipo = "";

    CorteShinkai() {
        // Linhas de velocidade fixas (sem sorteio por quadro): só correm de lado.
        long s = 7L;
        for (int i = 0; i < LINHAS; i++) {
            s = s * 6364136223846793005L + 1442695040888963407L;
            linhaY[i] = ((s >>> 33) % 1000) / 1000f;
            linhaX[i] = ((s >>> 13) % 1000) / 1000f;
            linhaL[i] = 0.15f + ((s >>> 45) % 1000) / 1000f * 0.35f;
        }
    }

    /** Começa o corte de quem ativou: o lado da tela dele, o rosto (pode ser null) e o tipo. */
    void comecar(boolean ladoDireito, Bitmap retrato, Shinkai.Type type) {
        quadro = 0;
        direita = ladoDireito;
        rosto = retrato;
        cor = type.color;
        tipo = type.label;
    }

    boolean ativo() { return quadro < QUADROS; }

    /** Um passo de 60 Hz. */
    void passo() {
        if (quadro < QUADROS) quadro++;
    }

    void desenhar(Canvas c) {
        if (!ativo()) return;
        float t = quadro;
        // Entra deslizando do lado de quem ativou e sai pelo outro; a tela escurece atrás.
        float entra = Math.min(1f, t / ENTRA);
        float sai = Math.max(0f, (t - (QUADROS - SAI)) / SAI);
        float desliza = (1f - suave(entra)) * -1300f + suave(sai) * 1300f;
        int escuro = Math.round(120f * Math.min(entra, 1f - sai));
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(escuro << 24);
        c.drawRect(-40f, -40f, 1320f, 760f, paint);
        paint.setColor(Color.WHITE);

        c.save();
        if (direita) c.scale(-1f, 1f, 640f, 0f);
        c.translate(desliza, 0f);
        faixa.reset();
        faixa.moveTo(-60f, ESQ_CIMA);
        faixa.lineTo(1340f, DIR_CIMA);
        faixa.lineTo(1340f, DIR_CIMA + ALTURA);
        faixa.lineTo(-60f, ESQ_CIMA + ALTURA);
        faixa.close();

        // Fundo: escuro do lado do rosto para a cor do tipo do outro lado.
        paint.setShader(new Degrade.Linear(0f, 0f, 1280f, 0f,
            new int[] {0xFF07080E, mistura(0xFF07080E, cor, 0.55f), cor}, new float[] {0f, 0.45f, 1f},
            Shader.TileMode.CLAMP));
        c.drawPath(faixa, paint);
        paint.setShader(null);

        c.save();
        c.clipPath(faixa);
        // Linhas de velocidade correndo para o lado do rosto.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < LINHAS; i++) {
            float y = DIR_CIMA - 20f + linhaY[i] * (ALTURA + 100f);
            float largura = linhaL[i] * 1280f;
            float x = ((linhaX[i] * 1.6f - t * 0.045f) % 1.6f + 1.6f) % 1.6f * 1280f - 300f;
            paint.setStrokeWidth(1.5f + (i % 4));
            paint.setColor(i % 3 == 0 ? 0x88FFFFFF : (0x66 << 24) | (mistura(cor, Color.WHITE, 0.5f) & 0xFFFFFF));
            c.drawLine(x, y, x + largura, y - largura * 0.04f, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        // Os olhos: o meio do rosto do HUD, do lado de quem ativou, andando devagar.
        if (rosto != null) {
            int w = rosto.getWidth(), h = rosto.getHeight();
            fonte.set(0, Math.round(h * OLHOS_DE), w, Math.round(h * OLHOS_ATE));
            float alto = ALTURA + 70f;
            float largo = alto * fonte.width() / Math.max(1f, fonte.height());
            float x0 = 20f + t * 0.7f;
            destino.set(x0, DIR_CIMA - 10f, x0 + largo, DIR_CIMA - 10f + alto);
            if (direita) {
                // O retrato olha para o centro: espelhado de volta dentro da faixa espelhada.
                c.save();
                c.scale(-1f, 1f, destino.centerX(), 0f);
                c.drawBitmap(rosto, fonte, destino, paint);
                c.restore();
            } else {
                c.drawBitmap(rosto, fonte, destino, paint);
            }
            // Um véu da cor do tipo por cima do rosto e a borda de dentro sumindo no fundo.
            paint.setColor((0x38 << 24) | (cor & 0xFFFFFF));
            c.drawRect(destino, paint);
            paint.setShader(new Degrade.Linear(destino.right - 170f, 0f, destino.right, 0f,
                0x0007080E, 0xFF07080E, Shader.TileMode.CLAMP));
            c.drawRect(destino.right - 170f, destino.top, destino.right + 2f, destino.bottom, paint);
            paint.setShader(null);
        }
        // Clarão branco quando a faixa pousa.
        float clarao = Math.max(0f, 1f - Math.abs(t - ENTRA) / 4f);
        if (clarao > 0f) {
            paint.setColor(Math.round(200f * clarao) << 24 | 0xFFFFFF);
            c.drawPath(faixa, paint);
        }
        c.restore();

        // Moldura de tinta: preto grosso por fora, branco fino por dentro.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setStrokeWidth(12f);
        paint.setColor(0xFF07080E);
        c.drawPath(faixa, paint);
        paint.setStrokeWidth(3f);
        paint.setColor(Color.WHITE);
        c.drawPath(faixa, paint);
        paint.setStyle(Paint.Style.FILL);
        c.restore();

        // O nome (nunca espelhado), batendo grande e assentando.
        float bate = Math.max(0f, 1f - Math.max(0f, t - 3f) / 6f);
        float escala = 1f + 0.7f * bate * bate;
        float local = 820f + desliza;
        float centro = ESQ_CIMA + (DIR_CIMA - ESQ_CIMA) * (local + 60f) / 1400f + ALTURA / 2f;
        float meio = direita ? 1280f - local : local;
        float base = centro + 10f;
        c.save();
        c.translate(meio, base);
        c.scale(escala, escala);
        texto(c, "SHINKAI", 0f, 0f, 104f, Color.WHITE, cor, 8f);
        texto(c, tipo, 0f, 54f, 40f, mistura(cor, Color.WHITE, 0.45f), 0xFF07080E, 0f);
        c.restore();
        paint.setTextSkewX(0f);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    /** Texto de mangá: inclinado, contorno preto grosso e uma sombra deslocada na cor. */
    private void texto(Canvas c, String s, float x, float y, float tamanho, int fill, int sombra, float desloca) {
        paint.setShader(null);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSkewX(-0.28f);
        paint.setTextSize(tamanho);
        if (desloca > 0f) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(sombra);
            c.drawText(s, x + desloca, y + desloca, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(tamanho * 0.12f);
        paint.setColor(0xFF07080E);
        c.drawText(s, x, y, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fill);
        c.drawText(s, x, y, paint);
    }

    private static float suave(float k) {
        k = Math.max(0f, Math.min(1f, k));
        return k * k * (3f - 2f * k);
    }

    private static int mistura(int a, int b, float k) {
        return Color.argb(255,
            Math.round(Color.red(a) + (Color.red(b) - Color.red(a)) * k),
            Math.round(Color.green(a) + (Color.green(b) - Color.green(a)) * k),
            Math.round(Color.blue(a) + (Color.blue(b) - Color.blue(a)) * k));
    }
}
