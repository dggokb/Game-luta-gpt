package com.gamelutagpt;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import java.util.Random;

/**
 * O desenho do SHINKAI Clash ({@link ShinkaiClash}): a tela escurece com linhas de velocidade
 * saindo do ponto de encontro, cada lutador empurra com o jeito do tipo dele até ali (o raio do
 * ENERGY, as ondas de choque do POWER, a rajada de golpes do SPEED, os cortes e o pulso da
 * TECHNIQUE), o encontro é uma bola de energia nas duas cores que anda conforme a disputa, e uma
 * barra de cabo de guerra no alto mostra quem está ganhando. Espaço do mundo, menos a barra (tela).
 * Só apresentação.
 */
final class ConfrontoShinkai {
    private static final int FAISCAS = 48, VIDA_FAISCA = 22, SAIDA = 24;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Random sorte = new Random(91L);

    private final float[] fx = new float[FAISCAS], fy = new float[FAISCAS], fvx = new float[FAISCAS], fvy = new float[FAISCAS];
    private final int[] fIdade = new int[FAISCAS], fCor = new int[FAISCAS];

    /** O último estado visto (para sumir devagar quando acaba). */
    private ShinkaiClash clash;
    private int saida;
    private final float[] x = new float[2], mao = new float[2], maoY = new float[2];
    private final int[] cor = new int[2];
    private float encontroX, encontroY;
    private int quadro;

    ConfrontoShinkai() {
        java.util.Arrays.fill(fIdade, -1);
    }

    boolean visivel() { return clash != null; }

    /** Um passo de 60 Hz: segue o confronto do motor, faíscas nos empurrões. */
    void passo(CombatEngine engine) {
        quadro++;
        ShinkaiClash c = engine.shinkaiClash();
        if (c != null) {
            clash = c;
            saida = SAIDA;
            for (int i = 0; i < 2; i++) {
                CombatFighter f = engine.fighter(i);
                x[i] = f.x;
                cor[i] = c.tipo[i].color;
                mao[i] = f.x + f.facing * 58f;
                maoY[i] = f.y - f.character().visualStandHeight * 0.6f;
            }
            float meio = (x[0] + x[1]) / 2f, metade = Math.abs(x[1] - x[0]) / 2f - 60f;
            float dir = Math.signum(x[1] - x[0]);
            float alvo = meio + dir * c.frente() * metade;
            encontroX = encontroX == 0f ? alvo : encontroX + (alvo - encontroX) * 0.35f;
            encontroY = (maoY[0] + maoY[1]) / 2f;
            for (int i = 0; i < 2; i++) {
                if (c.idadeEmpurrao[i] == 0 && c.disputando()) faiscas(i, Math.min(14, 3 + c.ultimoEmpurrao[i] / 10));
            }
        } else if (clash != null && --saida <= 0) {
            clash = null;
            encontroX = 0f;
        }
        for (int i = 0; i < FAISCAS; i++) {
            if (fIdade[i] < 0) continue;
            if (++fIdade[i] >= VIDA_FAISCA) { fIdade[i] = -1; continue; }
            fvy[i] += 1400f / 60f;
            fx[i] += fvx[i] / 60f;
            fy[i] += fvy[i] / 60f;
        }
    }

    private void faiscas(int lado, int n) {
        for (int k = 0; k < n; k++) {
            int i = livre();
            double a = Math.toRadians(-90 + (sorte.nextFloat() * 2f - 1f) * 80f);
            float v = 300f + sorte.nextFloat() * 700f;
            // Saem do encontro para o lado de quem levou o empurrão.
            float lado1 = Math.signum(x[1 - lado] - x[lado]);
            fx[i] = encontroX;
            fy[i] = encontroY;
            fvx[i] = (float)Math.cos(a) * v * 0.6f + lado1 * 160f;
            fvy[i] = (float)Math.sin(a) * v;
            fCor[i] = cor[lado];
            fIdade[i] = 0;
        }
    }

    private int livre() {
        int velho = 0;
        for (int i = 0; i < FAISCAS; i++) {
            if (fIdade[i] < 0) return i;
            if (fIdade[i] > fIdade[velho]) velho = i;
        }
        return velho;
    }

    /** Quanto do confronto está na tela (entra e sai suave). */
    private float presenca() {
        if (clash == null) return 0f;
        if (!clash.acabou() && saida == SAIDA) return Math.min(1f, clash.quadro / 14f);
        return saida / (float)SAIDA;
    }

    /** Atrás dos lutadores: a tela escurece e as linhas de velocidade saem do encontro. */
    void desenharFundo(Canvas c) {
        float k = presenca();
        if (k <= 0f) return;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Math.round(150 * k) << 24 | 0x05060C);
        c.drawRect(-3000f, -2000f, 6000f, 3000f, paint);
        // Cada metade da tela puxa para a cor de quem está daquele lado.
        int esq = x[0] <= x[1] ? 0 : 1;
        paint.setColor(Math.round(38 * k) << 24 | (cor[esq] & 0xFFFFFF));
        c.drawRect(-3000f, -2000f, encontroX, 3000f, paint);
        paint.setColor(Math.round(38 * k) << 24 | (cor[1 - esq] & 0xFFFFFF));
        c.drawRect(encontroX, -2000f, 6000f, 3000f, paint);
        // Linhas de velocidade de mangá, saindo do encontro.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        Random r = new Random(quadro / 2);
        for (int i = 0; i < 46; i++) {
            double a = r.nextDouble() * Math.PI * 2.0;
            float de = 160f + r.nextFloat() * 120f, ate = de + 260f + r.nextFloat() * 700f;
            float ca = (float)Math.cos(a), sa = (float)Math.sin(a);
            paint.setStrokeWidth(1.5f + r.nextFloat() * 4f);
            int lado = ca < 0f == (x[esq] < encontroX) ? esq : 1 - esq;
            paint.setColor(Math.round((60 + r.nextInt(90)) * k) << 24 | (i % 3 == 0 ? 0xFFFFFF : cor[lado] & 0xFFFFFF));
            c.drawLine(encontroX + ca * de, encontroY + sa * de * 0.8f, encontroX + ca * ate, encontroY + sa * ate * 0.8f, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    /** Na frente: o empurrão de cada um até o encontro, o encontro e as faíscas. */
    void desenharFrente(Canvas c) {
        float k = presenca();
        if (k <= 0f || clash == null) return;
        boolean disputa = clash.disputando();
        for (int i = 0; i < 2; i++) {
            if (disputa) {
                switch (clash.tipo[i]) {
                    case ENERGY: raio(c, i); break;
                    case POWER: ondas(c, i); break;
                    case SPEED: rajada(c, i); break;
                    default: cortes(c, i); break;
                }
            } else if (!clash.acabou()) {
                // Entrada: as duas forças crescendo nas mãos.
                float t = clash.quadro / (float)Math.max(1, clash.entrada);
                bola(c, mao[i], maoY[i], 8f + 22f * t, cor[i], k);
            }
        }
        if (disputa) encontro(c, k);
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < FAISCAS; i++) {
            if (fIdade[i] < 0) continue;
            float v = 1f - fIdade[i] / (float)VIDA_FAISCA;
            paint.setColor(Math.round(255 * v) << 24 | 0xFFFFFF);
            c.drawCircle(fx[i], fy[i], 2.5f * v + 1f, paint);
            paint.setColor(Math.round(200 * v) << 24 | (fCor[i] & 0xFFFFFF));
            c.drawCircle(fx[i], fy[i], 5f * v + 1.5f, paint);
        }
    }

    /** ENERGY: o raio da mão até o encontro (engrossa no empurrão; carregando, a bola cresce na mão). */
    private void raio(Canvas c, int i) {
        float carga = clash.carga[i] / (float)ShinkaiClash.CARGA_MAX;
        float pulso = Math.max(0f, 1f - clash.idadeEmpurrao[i] / 10f);
        float largura = 14f + 26f * pulso + 6f * (float)Math.sin(quadro * 0.6f + i);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        path.reset();
        path.moveTo(mao[i], maoY[i]);
        int partes = 8;
        for (int p = 1; p <= partes; p++) {
            float t = p / (float)partes;
            float bx = mao[i] + (encontroX - mao[i]) * t;
            float by = maoY[i] + (encontroY - maoY[i]) * t + (p < partes ? (float)Math.sin(quadro * 0.9f + p * 1.7f + i) * 6f : 0f);
            path.lineTo(bx, by);
        }
        paint.setStrokeWidth(largura * 2.2f);
        paint.setColor(0x55000000 | (cor[i] & 0xFFFFFF));
        c.drawPath(path, paint);
        paint.setStrokeWidth(largura);
        paint.setColor(0xDD000000 | (cor[i] & 0xFFFFFF));
        c.drawPath(path, paint);
        paint.setStrokeWidth(largura * 0.38f);
        paint.setColor(0xFFFFFFFF);
        c.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
        bola(c, mao[i], maoY[i], 16f + 30f * carga + 8f * pulso, cor[i], 1f);
    }

    /** POWER: ondas de choque em arco andando do corpo até o encontro, e o chão tremendo. */
    private void ondas(Canvas c, int i) {
        float dir = Math.signum(encontroX - x[i]);
        float dist = Math.abs(encontroX - mao[i]);
        float pulso = Math.max(0f, 1f - clash.idadeEmpurrao[i] / 14f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int k = 0; k < 4; k++) {
            float t = ((quadro * 0.035f + k * 0.25f) % 1f);
            float ox = mao[i] + dir * dist * t;
            float alto = 46f + 40f * t + 30f * pulso;
            paint.setStrokeWidth(5f + 6f * pulso);
            paint.setColor(Math.round(210 * (1f - t * 0.6f)) << 24 | (cor[i] & 0xFFFFFF));
            path.reset();
            path.moveTo(ox - dir * 14f, maoY[i] - alto);
            path.quadTo(ox + dir * 22f, maoY[i], ox - dir * 14f, maoY[i] + alto);
            c.drawPath(path, paint);
        }
        // Rachaduras curtas no chão debaixo dele quando empurra.
        if (pulso > 0f) {
            paint.setStrokeWidth(3f);
            paint.setColor(Math.round(220 * pulso) << 24 | 0x1A1410);
            Random r = new Random(i * 31 + clash.quadro / 12);
            for (int k = 0; k < 5; k++) {
                float gx = x[i] + (r.nextFloat() * 2f - 1f) * 70f;
                c.drawLine(gx, Arena.GROUND_Y, gx + (r.nextFloat() * 2f - 1f) * 40f, Arena.GROUND_Y + 4f, paint);
            }
        }
        paint.setStyle(Paint.Style.FILL);
        bola(c, mao[i], maoY[i], 14f + 14f * pulso, cor[i], 1f);
    }

    /** SPEED: uma rajada de golpes riscando o ar entre ele e o encontro. */
    private void rajada(Canvas c, int i) {
        float pulso = Math.max(0f, 1f - clash.idadeEmpurrao[i] / 6f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        Random r = new Random(i * 77 + quadro);
        int n = 5 + Math.round(9 * pulso);
        for (int k = 0; k < n; k++) {
            float t = r.nextFloat();
            float sx = mao[i] + (encontroX - mao[i]) * t;
            float sy = encontroY + (r.nextFloat() * 2f - 1f) * 70f;
            float len = 40f + r.nextFloat() * 80f;
            float dir = Math.signum(encontroX - mao[i]);
            paint.setStrokeWidth(2f + r.nextFloat() * 4f);
            paint.setColor((150 + r.nextInt(100)) << 24 | (k % 3 == 0 ? 0xFFFFFF : cor[i] & 0xFFFFFF));
            c.drawLine(sx, sy, sx + dir * len, sy + (r.nextFloat() - 0.5f) * 10f, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    /** TECHNIQUE: o pulso (um anel fechando no corpo, no ritmo) e cortes dourados no encontro. */
    private void cortes(Canvas c, int i) {
        int fase = clash.fase();
        float t = fase / (float)ShinkaiClash.BATIDA;
        // O anel fecha até o corpo bem no pulso.
        float raio = 40f + 150f * (1f - t);
        float perto = 1f - Math.min(fase, ShinkaiClash.BATIDA - fase) / (float)(ShinkaiClash.BATIDA / 2);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f + 5f * perto);
        paint.setColor(Math.round(90 + 165 * perto) << 24 | (cor[i] & 0xFFFFFF));
        c.drawCircle(x[i], maoY[i], fase == 0 ? 40f : raio, paint);
        float pulso = Math.max(0f, 1f - clash.idadeEmpurrao[i] / 12f);
        if (pulso > 0f) {
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(4f + 6f * pulso);
            float big = clash.ultimoEmpurrao[i] > 50 ? 1f : 0.45f;
            for (int k = 0; k < 3; k++) {
                double a = Math.toRadians(25 + k * 55 + i * 30);
                float len = (60f + 70f * big) * pulso;
                float ca = (float)Math.cos(a) * len, sa = (float)Math.sin(a) * len;
                paint.setColor(Math.round(255 * pulso) << 24 | 0xFFFFFF);
                c.drawLine(encontroX - ca, encontroY - sa, encontroX + ca, encontroY + sa, paint);
            }
        }
        paint.setStyle(Paint.Style.FILL);
        bola(c, mao[i], maoY[i], 12f + 10f * perto, cor[i], 1f);
    }

    /** O encontro: duas metades de energia, uma de cada cor, raios em volta e o miolo branco. */
    private void encontro(Canvas c, float k) {
        float pulso = Math.max(Math.max(0f, 1f - clash.idadeEmpurrao[0] / 8f), Math.max(0f, 1f - clash.idadeEmpurrao[1] / 8f));
        float r = 36f + 18f * pulso + 4f * (float)Math.sin(quadro * 0.8f);
        for (int i = 0; i < 2; i++) {
            float lado = Math.signum(x[i] - encontroX);
            paint.setColor(Math.round(110 * k) << 24 | (cor[i] & 0xFFFFFF));
            c.drawCircle(encontroX + lado * r * 0.35f, encontroY, r * 1.6f, paint);
            paint.setColor(Math.round(220 * k) << 24 | (cor[i] & 0xFFFFFF));
            c.drawCircle(encontroX + lado * r * 0.3f, encontroY, r, paint);
        }
        paint.setColor(Math.round(255 * k) << 24 | 0xFFFFFF);
        c.drawCircle(encontroX, encontroY, r * 0.55f, paint);
        // Raios estalando em volta, alternando as cores.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(2.5f);
        Random rr = new Random(quadro / 2);
        for (int n = 0; n < 6; n++) {
            double a = rr.nextDouble() * Math.PI * 2.0;
            float px = encontroX, py = encontroY;
            path.reset();
            path.moveTo(px, py);
            for (int s = 0; s < 4; s++) {
                float d = r * (0.7f + s * 0.45f);
                double aa = a + (rr.nextDouble() - 0.5) * 0.7;
                px = encontroX + (float)Math.cos(aa) * d;
                py = encontroY + (float)Math.sin(aa) * d * 0.8f;
                path.lineTo(px, py);
            }
            paint.setColor(Math.round(230 * k) << 24 | (n % 2 == 0 ? cor[0] : cor[1]) & 0xFFFFFF);
            c.drawPath(path, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void bola(Canvas c, float bx, float by, float r, int cor, float k) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Math.round(90 * k) << 24 | (cor & 0xFFFFFF));
        c.drawCircle(bx, by, r * 1.7f, paint);
        paint.setColor(Math.round(220 * k) << 24 | (cor & 0xFFFFFF));
        c.drawCircle(bx, by, r, paint);
        paint.setColor(Math.round(255 * k) << 24 | 0xFFFFFF);
        c.drawCircle(bx, by, r * 0.45f, paint);
    }

    /**
     * A barra do cabo de guerra (espaço da tela, 1280 × 720): cada lado na cor de quem está
     * daquele lado da tela, a marca no encontro, e o tempo acabando embaixo.
     */
    void desenharBarra(Canvas c) {
        float k = presenca();
        if (k <= 0f || clash == null) return;
        float largura = 560f, alto = 22f, cx = 640f, y = 232f;
        float esqX = cx - largura / 2f;
        int esq = x[0] <= x[1] ? 0 : 1;
        float meio = (x[0] + x[1]) / 2f, metade = Math.max(1f, Math.abs(x[1] - x[0]) / 2f - 60f);
        float f = 0.5f + (encontroX - meio) / metade * 0.5f;
        f = Math.max(0f, Math.min(1f, f));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Math.round(200 * k) << 24 | 0x07080E);
        c.drawRect(esqX - 6f, y - 6f, esqX + largura + 6f, y + alto + 6f, paint);
        paint.setColor(Math.round(255 * k) << 24 | (cor[esq] & 0xFFFFFF));
        c.drawRect(esqX, y, esqX + largura * f, y + alto, paint);
        paint.setColor(Math.round(255 * k) << 24 | (cor[1 - esq] & 0xFFFFFF));
        c.drawRect(esqX + largura * f, y, esqX + largura, y + alto, paint);
        // A marca: um losango branco tremendo.
        float mx = esqX + largura * f + (float)Math.sin(quadro * 1.7f) * 1.5f;
        path.reset();
        path.moveTo(mx, y - 12f);
        path.lineTo(mx + 11f, y + alto / 2f);
        path.lineTo(mx, y + alto + 12f);
        path.lineTo(mx - 11f, y + alto / 2f);
        path.close();
        paint.setColor(Math.round(255 * k) << 24 | 0xFFFFFF);
        c.drawPath(path, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(Math.round(255 * k) << 24 | 0x07080E);
        c.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
        // O tempo: uma linha embaixo que encolhe para o meio.
        if (clash.disputando()) {
            float resta = 1f - clash.tempo() / (float)ShinkaiClash.DURACAO;
            paint.setColor(Math.round(220 * k) << 24 | 0xFFFFFF);
            c.drawRect(cx - largura / 2f * resta, y + alto + 9f, cx + largura / 2f * resta, y + alto + 13f, paint);
        }
        // O nome do confronto, em mangá, em cima.
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSkewX(-0.25f);
        paint.setTextSize(40f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(7f);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(Math.round(255 * k) << 24 | 0x07080E);
        c.drawText("SHINKAI CLASH", cx, y - 18f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Math.round(255 * k) << 24 | 0xFFFFFF);
        c.drawText("SHINKAI CLASH", cx, y - 18f, paint);
        paint.setTextSkewX(0f);
        paint.setFakeBoldText(false);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    /** O ponto do encontro (o estouro do fim sai daqui). */
    float encontroX() { return encontroX; }
    float encontroY() { return encontroY; }
    int cor(int lado) { return cor[lado]; }
}
