package com.gamelutagpt;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import java.util.Random;

/**
 * Os detalhes de cada tipo do SHINKAI em volta do corpo, sem animação nova (espaço do mundo):
 * POWER levanta pedrinhas do chão e trinca o chão nos golpes; ENERGY solta raios curtos em volta
 * do corpo; TECHNIQUE brilha na ponta do golpe. O rastro do SPEED é desenhado com o sprite, no
 * GameView. Só apresentação: nada aqui muda a luta.
 */
final class ShinkaiDetalhes {
    private static final int PEDRAS = 40, RAIOS = 12, TRINCAS = 6, BRILHOS = 6;
    private static final int VIDA_PEDRA = 56, VIDA_RAIO = 7, VIDA_TRINCA = 70, VIDA_BRILHO = 14;

    private final Random sorte = new Random(31L);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    // Pedrinhas: x, y, velocidade de subida, giro, tamanho, idade (−1 livre).
    private final float[] px = new float[PEDRAS], py = new float[PEDRAS], pv = new float[PEDRAS],
        pr = new float[PEDRAS], ps = new float[PEDRAS];
    private final int[] pIdade = fill(new int[PEDRAS]), pCor = new int[PEDRAS];
    // Raios: 6 pontos cada, idade, cor.
    private final float[][] raio = new float[RAIOS][12];
    private final int[] rIdade = fill(new int[RAIOS]), rCor = new int[RAIOS];
    // Trincas no chão: centro, lado, semente.
    private final float[] tx = new float[TRINCAS];
    private final int[] tIdade = fill(new int[TRINCAS]), tSemente = new int[TRINCAS], tCor = new int[TRINCAS];
    // Brilhos de ponta de golpe.
    private final float[] bx = new float[BRILHOS], by = new float[BRILHOS];
    private final int[] bIdade = fill(new int[BRILHOS]), bCor = new int[BRILHOS];

    private static int[] fill(int[] a) {
        java.util.Arrays.fill(a, -1);
        return a;
    }

    /** Um passo de 60 Hz: cada lutador no SHINKAI solta o detalhe do tipo dele. */
    void passo(CombatEngine engine, long quadro) {
        for (int i = 0; i < 2; i++) {
            CombatFighter f = engine.fighter(i);
            if (!f.state.inShinkai() || f.vanishFrames > 0) continue;
            int cor = f.state.shinkaiType.color;
            switch (f.state.shinkaiType) {
                case POWER:
                    if (quadro % 3 == 0) pedra(f.x + (sorte.nextFloat() * 2f - 1f) * 80f, Arena.GROUND_Y, cor);
                    break;
                case ENERGY:
                    if (quadro % 3 == 0) raio(f, cor);
                    break;
                case TECHNIQUE:
                    // A ponta do golpe brilha no primeiro quadro em que ele acerta.
                    if (f.attacking() && f.attack.hitboxCount() > 0 && f.attackFrame == f.attack.hitbox(0).firstFrame) {
                        AttackDefinition.Box b = f.attack.hitbox(0);
                        brilho(f.x + f.facing * b.x1, f.y - (b.y0 + b.y1) / 2f, cor);
                    }
                    break;
                default:
                    break;
            }
        }
        envelhecer(pIdade, VIDA_PEDRA);
        envelhecer(rIdade, VIDA_RAIO);
        envelhecer(tIdade, VIDA_TRINCA);
        envelhecer(bIdade, VIDA_BRILHO);
        for (int i = 0; i < PEDRAS; i++) {
            if (pIdade[i] < 0) continue;
            py[i] -= pv[i] / 60f;
            pv[i] *= 0.985f;
        }
    }

    /** POWER acertou: o chão trinca debaixo de quem apanhou. */
    void trinca(float x, int cor) {
        int i = livre(tIdade);
        tx[i] = x;
        tIdade[i] = 0;
        tSemente[i] = sorte.nextInt();
        tCor[i] = cor;
        for (int k = 0; k < 6; k++) pedra(x + (sorte.nextFloat() * 2f - 1f) * 50f, Arena.GROUND_Y, cor);
    }

    private void pedra(float x, float y, int cor) {
        int i = livre(pIdade);
        px[i] = x;
        py[i] = y;
        pv[i] = 40f + sorte.nextFloat() * 70f;
        pr[i] = sorte.nextFloat() * 360f;
        ps[i] = 3f + sorte.nextFloat() * 5f;
        pIdade[i] = 0;
        pCor[i] = cor;
    }

    private void raio(CombatFighter f, int cor) {
        int i = livre(rIdade);
        // Começa na borda de uma elipse em volta do corpo e anda em zigue-zague por ela.
        double a = sorte.nextDouble() * Math.PI * 2.0;
        float cx = f.x, cy = f.y - 110f, rx = 58f, ry = 112f;
        double passo = (sorte.nextBoolean() ? 1 : -1) * (0.18 + sorte.nextDouble() * 0.12);
        for (int k = 0; k < 6; k++) {
            float desvio = (sorte.nextFloat() * 2f - 1f) * 14f;
            raio[i][k * 2] = cx + (float)Math.cos(a) * (rx + desvio);
            raio[i][k * 2 + 1] = cy + (float)Math.sin(a) * (ry + desvio);
            a += passo;
        }
        rIdade[i] = 0;
        rCor[i] = cor;
    }

    private void brilho(float x, float y, int cor) {
        int i = livre(bIdade);
        bx[i] = x;
        by[i] = y;
        bIdade[i] = 0;
        bCor[i] = cor;
    }

    private static void envelhecer(int[] idade, int vida) {
        for (int i = 0; i < idade.length; i++) if (idade[i] >= 0 && ++idade[i] >= vida) idade[i] = -1;
    }

    /** Um lugar livre, ou o mais velho. */
    private static int livre(int[] idade) {
        int velho = 0;
        for (int i = 0; i < idade.length; i++) {
            if (idade[i] < 0) return i;
            if (idade[i] > idade[velho]) velho = i;
        }
        return velho;
    }

    /** Atrás dos lutadores: as trincas no chão. */
    void desenharChao(Canvas c) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < TRINCAS; i++) {
            if (tIdade[i] < 0) continue;
            float k = 1f - tIdade[i] / (float)VIDA_TRINCA;
            Random r = new Random(tSemente[i]);
            for (int linha = 0; linha < 7; linha++) {
                float x = tx[i], y = Arena.GROUND_Y + 2f;
                float dir = (r.nextFloat() * 2f - 1f);
                path.reset();
                path.moveTo(x, y);
                for (int s = 0; s < 4; s++) {
                    x += dir * (18f + r.nextFloat() * 22f);
                    y += (r.nextFloat() * 2f - 1f) * 4f + 1.5f;
                    path.lineTo(x, y);
                }
                paint.setStrokeWidth(4f);
                paint.setColor(Color.argb(Math.round(220 * k), 20, 16, 14));
                c.drawPath(path, paint);
                paint.setStrokeWidth(1.5f);
                paint.setColor((Math.round(200 * k) << 24) | (tCor[i] & 0xFFFFFF));
                c.drawPath(path, paint);
            }
        }
        paint.setStyle(Paint.Style.FILL);
    }

    /** Na frente: pedrinhas, raios e brilhos. */
    void desenhar(Canvas c) {
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < PEDRAS; i++) {
            if (pIdade[i] < 0) continue;
            float k = 1f - pIdade[i] / (float)VIDA_PEDRA;
            c.save();
            c.rotate(pr[i] + pIdade[i] * 4f, px[i], py[i]);
            float s = ps[i];
            path.reset();
            path.moveTo(px[i] - s, py[i] - s * 0.4f);
            path.lineTo(px[i] - s * 0.2f, py[i] - s);
            path.lineTo(px[i] + s, py[i] - s * 0.3f);
            path.lineTo(px[i] + s * 0.5f, py[i] + s * 0.8f);
            path.lineTo(px[i] - s * 0.7f, py[i] + s * 0.6f);
            path.close();
            paint.setColor(Color.argb(Math.round(235 * k), 58, 50, 44));
            c.drawPath(path, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f);
            paint.setColor((Math.round(220 * k) << 24) | (pCor[i] & 0xFFFFFF));
            c.drawPath(path, paint);
            paint.setStyle(Paint.Style.FILL);
            c.restore();
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        for (int i = 0; i < RAIOS; i++) {
            if (rIdade[i] < 0) continue;
            float k = 1f - rIdade[i] / (float)VIDA_RAIO;
            path.reset();
            path.moveTo(raio[i][0], raio[i][1]);
            for (int p = 1; p < 6; p++) path.lineTo(raio[i][p * 2], raio[i][p * 2 + 1]);
            paint.setStrokeWidth(6f);
            paint.setColor((Math.round(150 * k) << 24) | (rCor[i] & 0xFFFFFF));
            c.drawPath(path, paint);
            paint.setStrokeWidth(2f);
            paint.setColor(Color.argb(Math.round(255 * k), 255, 255, 255));
            c.drawPath(path, paint);
        }
        for (int i = 0; i < BRILHOS; i++) {
            if (bIdade[i] < 0) continue;
            float k = 1f - bIdade[i] / (float)VIDA_BRILHO;
            float r = 10f + 34f * (1f - k * k);
            // Estrela de quatro pontas: o brilho do golpe certeiro.
            path.reset();
            path.moveTo(bx[i] - r, by[i]);
            path.lineTo(bx[i] + r, by[i]);
            path.moveTo(bx[i], by[i] - r * 0.7f);
            path.lineTo(bx[i], by[i] + r * 0.7f);
            paint.setStrokeWidth(7f * k + 1f);
            paint.setColor((Math.round(170 * k) << 24) | (bCor[i] & 0xFFFFFF));
            c.drawPath(path, paint);
            paint.setStrokeWidth(2.5f * k + 0.5f);
            paint.setColor(Color.argb(Math.round(255 * k), 255, 255, 255));
            c.drawPath(path, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeJoin(Paint.Join.MITER);
    }
}
