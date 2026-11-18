package com.gamelutagpt;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import java.util.Random;

/**
 * Balão de fala de quadrinho em cima da cabeça do lutador (o dono, 18/11: errou o Ultra SHINKAI,
 * aparece "Droga!", "O quê?!" ou "Errei!"). Desenhado no mundo, junto dos números do SHINKAI: segue o
 * lutador, entra crescendo e some no fim. Um balão por lutador; uma fala nova troca a anterior.
 */
final class BalaoDeFala {
    /** O que o lutador diz quando o Ultra SHINKAI passa sem pegar ninguém. */
    static final String[] ERROU = {"Droga!", "O quê?!", "Errei!"};
    /** Quadros na tela (1,6 s), a entrada (crescendo) e a saída (sumindo). */
    static final int VIDA = 96, ENTRA = 7, SAI = 14;
    /** Tamanho da letra e folgas, em px do mundo. */
    static final float LETRA = 48f, FOLGA_X = 24f, FOLGA_Y = 15f, RABO = 28f, ACIMA = 30f;

    private final String[] texto = new String[2];
    private final int[] inicio = new int[2];
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path rabo = new Path();
    private final RectF caixa = new RectF();

    /** Uma fala sorteada de {@code falas}. */
    static String sortear(String[] falas, Random random) {
        return falas[random.nextInt(falas.length)];
    }

    /** O lutador {@code lado} (0 ou 1) diz {@code fala} a partir do quadro {@code frame}. */
    void falar(int lado, String fala, int frame) {
        if (lado < 0 || lado > 1) return;
        texto[lado] = fala;
        inicio[lado] = frame;
    }

    /** O texto que o lutador {@code lado} está dizendo agora, ou null (testes). */
    String falando(int lado, int frame) {
        return texto[lado] != null && frame - inicio[lado] >= 0 && frame - inicio[lado] < VIDA ? texto[lado] : null;
    }

    void limpar() {
        texto[0] = texto[1] = null;
    }

    void desenhar(Canvas c, CombatEngine engine) {
        int frame = engine.frame();
        for (int lado = 0; lado < 2; lado++) {
            if (texto[lado] == null) continue;
            int idade = frame - inicio[lado];
            if (idade < 0 || idade >= VIDA) {
                texto[lado] = null;
                continue;
            }
            desenharUm(c, engine.fighter(lado), texto[lado], idade);
        }
    }

    private void desenharUm(Canvas c, CombatFighter f, String s, int idade) {
        float entra = Math.min(1f, (idade + 1) / (float) ENTRA);
        // Entra passando um pouco do tamanho e assenta (o "pop" do quadrinho).
        float escala = entra < 1f ? 0.4f + 0.75f * entra : 1f + 0.08f * Math.max(0f, 1f - (idade - ENTRA) / 5f);
        float alfa = idade > VIDA - SAI ? (VIDA - idade) / (float) SAI : 1f;
        int a = Math.round(255 * Math.max(0f, Math.min(1f, alfa)));

        float cabeca = f.y - f.body().height(f.crouchingBody());
        float cx = f.x + f.facing * 26f, base = cabeca - ACIMA;

        c.save();
        c.translate(cx, base);
        c.scale(escala, escala);
        paint.setTextSize(LETRA);
        paint.setFakeBoldText(true);
        paint.setTextSkewX(-0.12f);
        paint.setTextAlign(Paint.Align.CENTER);
        float w = paint.measureText(s) + FOLGA_X * 2f, h = LETRA + FOLGA_Y * 2f;
        caixa.set(-w / 2f, -RABO - h, w / 2f, -RABO);
        // O rabo aponta para a cabeça (para trás de quem fala, embaixo).
        float rx = -f.facing * w * 0.18f;
        rabo.reset();
        rabo.moveTo(rx - 14f, -RABO - 2f);
        rabo.lineTo(rx + 14f, -RABO - 2f);
        rabo.lineTo(rx - f.facing * 22f, 0f);
        rabo.close();

        // Contorno preto grosso, depois o branco por cima (o contorno do rabo some na junção).
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(7f);
        paint.setColor(Color.argb(a, 10, 10, 16));
        c.drawRoundRect(caixa, h * 0.45f, h * 0.45f, paint);
        c.drawPath(rabo, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(a, 255, 255, 255));
        c.drawRoundRect(caixa, h * 0.45f, h * 0.45f, paint);
        c.drawPath(rabo, paint);

        paint.setColor(Color.argb(a, 14, 14, 20));
        c.drawText(s, 0f, -RABO - FOLGA_Y - LETRA * 0.18f, paint);
        paint.setTextSkewX(0f);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
        c.restore();
    }
}
