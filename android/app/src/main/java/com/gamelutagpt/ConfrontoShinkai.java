package com.gamelutagpt;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import java.util.Random;

/**
 * O desenho do SHINKAI Clash ({@link ShinkaiClash}, coreografia em {@link CoreografiaClash}): a tela escurece com linhas de velocidade
 * saindo do ponto de encontro, cada lutador empurra com o jeito do tipo dele até ali (o raio do
 * ENERGY, as ondas de choque do POWER, a rajada de golpes do SPEED, os cortes e o pulso da
 * TECHNIQUE), o encontro é uma bola de energia nas duas cores que anda conforme a disputa, e uma
 * barra de cabo de guerra no alto mostra quem está ganhando. No ar e na velocidade cada choque
 * estoura num anel com raios e faíscas; no chão o chão racha e as pedras sobem. Espaço do mundo,
 * menos a barra (tela).
 * Só apresentação.
 */
final class ConfrontoShinkai {
    private static final int FAISCAS = 48, VIDA_FAISCA = 22, SAIDA = 24;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Random sorte = new Random(91L);

    private final float[] fx = new float[FAISCAS], fy = new float[FAISCAS], fvx = new float[FAISCAS], fvy = new float[FAISCAS];
    private final int[] fIdade = new int[FAISCAS], fCor = new int[FAISCAS];

    /** Os estouros dos choques da coreografia: anel que abre, com raios, no ar ou no chão. */
    private static final int ANEIS = 10, VIDA_ANEL = 20;
    private final float[] ax = new float[ANEIS], ay = new float[ANEIS], aForca = new float[ANEIS];
    private final int[] aIdade = new int[ANEIS], aCor = new int[ANEIS];
    /** Pedras que sobem do chão no choque (POWER e todos no choque no chão). */
    private static final int PEDRAS = 24;
    private final float[] px = new float[PEDRAS], py = new float[PEDRAS], pvx = new float[PEDRAS], pvy = new float[PEDRAS], pTam = new float[PEDRAS];
    private final int[] pIdade = new int[PEDRAS];

    /** O último estado visto (para sumir devagar quando acaba). */
    private ShinkaiClash clash;
    private int saida;
    private final float[] x = new float[2], mao = new float[2], maoY = new float[2];
    private final int[] cor = new int[2];
    private float encontroX, encontroY;
    private int quadro;

    ConfrontoShinkai() {
        java.util.Arrays.fill(fIdade, -1);
        java.util.Arrays.fill(aIdade, -1);
        java.util.Arrays.fill(pIdade, -1);
    }

    /** A cena da coreografia agora (choque no chão quando não há coreografia). */
    private CoreografiaClash.Cena cena() {
        return clash == null || clash.coreo == null ? CoreografiaClash.Cena.CHOQUE : clash.coreo.cena;
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
            CoreografiaClash k = c.coreo;
            if (k != null && c.disputando()) {
                // O encontro é o da coreografia (o choque no chão, a troca no ar, a aparição).
                encontroX = k.encontroX;
                encontroY = Arena.GROUND_Y - k.encontroY;
                if (k.choque) anel(k.choqueX, Arena.GROUND_Y - k.choqueY, k.forca, cor[c.frente() >= 0f ? 0 : 1]);
                if (k.cena == CoreografiaClash.Cena.CHOQUE && k.naCena % 9 == 0) pedras(2);
            } else {
                float meio = (x[0] + x[1]) / 2f, metade = Math.abs(x[1] - x[0]) / 2f - 60f;
                float dir = Math.signum(x[1] - x[0]);
                float alvo = meio + dir * c.frente() * metade;
                encontroX = encontroX == 0f ? alvo : encontroX + (alvo - encontroX) * 0.35f;
                encontroY = (maoY[0] + maoY[1]) / 2f;
            }
            for (int i = 0; i < 2; i++) {
                if (c.idadeEmpurrao[i] == 0 && c.disputando() && cena() == CoreografiaClash.Cena.CHOQUE) {
                    faiscas(i, Math.min(14, 3 + c.ultimoEmpurrao[i] / 10));
                }
            }
        } else if (clash != null && --saida <= 0) {
            clash = null;
            encontroX = 0f;
        }
        for (int i = 0; i < ANEIS; i++) if (aIdade[i] >= 0 && ++aIdade[i] >= VIDA_ANEL) aIdade[i] = -1;
        for (int i = 0; i < PEDRAS; i++) {
            if (pIdade[i] < 0) continue;
            if (++pIdade[i] >= 70) { pIdade[i] = -1; continue; }
            pvy[i] += 260f / 60f;   // sobem devagar e param no ar (a força segura)
            px[i] += pvx[i] / 60f;
            py[i] += pvy[i] / 60f;
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

    private void anel(float ox, float oy, float forca, int c) {
        int i = 0;
        for (int k = 0; k < ANEIS; k++) if (aIdade[k] < 0 || aIdade[k] > aIdade[i]) i = k;
        for (int k = 0; k < ANEIS; k++) if (aIdade[k] < 0) { i = k; break; }
        ax[i] = ox;
        ay[i] = oy;
        aForca[i] = forca;
        aCor[i] = c;
        aIdade[i] = 0;
        // Faíscas para todo lado no estouro.
        for (int n = 0; n < 6 + Math.round(8 * forca); n++) {
            int f = livre();
            double a = sorte.nextDouble() * Math.PI * 2.0;
            float v = 400f + sorte.nextFloat() * 700f * forca;
            fx[f] = ox;
            fy[f] = oy;
            fvx[f] = (float)Math.cos(a) * v;
            fvy[f] = (float)Math.sin(a) * v;
            fCor[f] = n % 2 == 0 ? c : 0xFFFFFFFF;
            fIdade[f] = 0;
        }
    }

    /** Pedras e terra subindo do chão em volta do choque. */
    private void pedras(int n) {
        for (int k = 0; k < n; k++) {
            int i = 0;
            for (int j = 0; j < PEDRAS; j++) if (pIdade[j] < 0) { i = j; break; }
            px[i] = encontroX + (sorte.nextFloat() * 2f - 1f) * 260f;
            py[i] = Arena.GROUND_Y - 4f;
            pvx[i] = (sorte.nextFloat() * 2f - 1f) * 30f;
            pvy[i] = -(160f + sorte.nextFloat() * 220f);
            pTam[i] = 4f + sorte.nextFloat() * 10f;
            pIdade[i] = 0;
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

    /** O mesmo fundo no desenho em OpenGL ({@link DesenhoGl}): atrás dos lutadores, que estão lá. */
    void desenharFundo(com.gamelutagpt.render.RenderCanvas c) {
        float k = presenca();
        if (k <= 0f) return;
        c.fillRect(-3000f, -2000f, 6000f, 3000f, Math.round(150 * k) << 24 | 0x05060C);
        int esq = x[0] <= x[1] ? 0 : 1;
        c.fillRect(-3000f, -2000f, encontroX, 3000f, Math.round(38 * k) << 24 | (cor[esq] & 0xFFFFFF));
        c.fillRect(encontroX, -2000f, 6000f, 3000f, Math.round(38 * k) << 24 | (cor[1 - esq] & 0xFFFFFF));
        Random r = new Random(quadro / 2);
        for (int i = 0; i < 46; i++) {
            double a = r.nextDouble() * Math.PI * 2.0;
            float de = 160f + r.nextFloat() * 120f, ate = de + 260f + r.nextFloat() * 700f;
            float ca = (float)Math.cos(a), sa = (float)Math.sin(a);
            float largura = 1.5f + r.nextFloat() * 4f;
            int lado = ca < 0f == (x[esq] < encontroX) ? esq : 1 - esq;
            int argb = Math.round((60 + r.nextInt(90)) * k) << 24 | (i % 3 == 0 ? 0xFFFFFF : cor[lado] & 0xFFFFFF);
            c.drawLine(encontroX + ca * de, encontroY + sa * de * 0.8f, encontroX + ca * ate, encontroY + sa * ate * 0.8f, largura, argb);
        }
    }

    /** Na frente: o empurrão de cada um até o encontro, o encontro e as faíscas. */
    void desenharFrente(Canvas c) {
        float k = presenca();
        if (k <= 0f || clash == null) return;
        boolean disputa = clash.disputando();
        boolean noChao = cena() == CoreografiaClash.Cena.CHOQUE;
        pedrasEChao(c, k, disputa && noChao);
        for (int i = 0; i < 2; i++) {
            if (disputa && !noChao) continue;
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
        if (disputa && noChao) encontro(c, k);
        aneis(c, k);
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

    /** As pedras flutuando e, no choque no chão, as rachaduras saindo do encontro. */
    private void pedrasEChao(Canvas c, float k, boolean rachar) {
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < PEDRAS; i++) {
            if (pIdade[i] < 0) continue;
            float v = pIdade[i] < 55 ? 1f : 1f - (pIdade[i] - 55) / 15f;
            paint.setColor(Math.round(230 * v * k) << 24 | 0x3A2E24);
            c.drawRect(px[i] - pTam[i] / 2f, py[i] - pTam[i] / 2f, px[i] + pTam[i] / 2f, py[i] + pTam[i] * 0.4f, paint);
            paint.setColor(Math.round(160 * v * k) << 24 | 0x8A7A66);
            c.drawRect(px[i] - pTam[i] / 2f, py[i] - pTam[i] / 2f, px[i] + pTam[i] / 4f, py[i] - pTam[i] / 6f, paint);
        }
        if (!rachar || clash.coreo == null) return;
        // As rachaduras crescem enquanto dura o choque no chão.
        float cresce = Math.min(1f, clash.coreo.naCena / 50f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        Random r = new Random(77);
        for (int n = 0; n < 9; n++) {
            float lado = n % 2 == 0 ? 1f : -1f;
            float len = (70f + r.nextFloat() * 230f) * cresce;
            float ox = encontroX, oy = Arena.GROUND_Y + 2f;
            path.reset();
            path.moveTo(ox, oy);
            for (int s = 1; s <= 4; s++) {
                ox += lado * len / 4f;
                oy += (r.nextFloat() - 0.4f) * 8f;
                path.lineTo(ox, oy);
            }
            paint.setStrokeWidth(3f + r.nextFloat() * 3f);
            paint.setColor(Math.round(220 * k) << 24 | 0x120E0A);
            c.drawPath(path, paint);
            paint.setStrokeWidth(1.2f);
            paint.setColor(Math.round(170 * k) << 24 | (cor[n % 2] & 0xFFFFFF));
            c.drawPath(path, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    /** Os estouros: anel que abre e some, raios curtos e um clarão branco no começo. */
    private void aneis(Canvas c, float k) {
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < ANEIS; i++) {
            if (aIdade[i] < 0) continue;
            float t = aIdade[i] / (float)VIDA_ANEL, f = aForca[i];
            float r = (30f + 170f * f) * (float)Math.sqrt(t);
            paint.setStyle(Paint.Style.FILL);
            if (aIdade[i] < 3) {
                paint.setColor(Math.round(230 * k) << 24 | 0xFFFFFF);
                c.drawCircle(ax[i], ay[i], 40f + 50f * f, paint);
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth((10f + 10f * f) * (1f - t));
            paint.setColor(Math.round(240 * (1f - t) * k) << 24 | 0xFFFFFF);
            c.drawOval(ax[i] - r, ay[i] - r * 0.7f, ax[i] + r, ay[i] + r * 0.7f, paint);
            paint.setStrokeWidth((5f + 5f * f) * (1f - t));
            paint.setColor(Math.round(220 * (1f - t) * k) << 24 | (aCor[i] & 0xFFFFFF));
            c.drawOval(ax[i] - r * 1.15f, ay[i] - r * 0.8f, ax[i] + r * 1.15f, ay[i] + r * 0.8f, paint);
            Random rr = new Random(i * 13L + (long)(ax[i]));
            paint.setStrokeWidth(3f * (1f - t) + 1f);
            for (int n = 0; n < 10; n++) {
                double a = rr.nextDouble() * Math.PI * 2.0;
                float d0 = r * 0.6f, d1 = r * (1.1f + rr.nextFloat() * 0.8f);
                float ca = (float)Math.cos(a), sa = (float)Math.sin(a);
                paint.setColor(Math.round(230 * (1f - t) * k) << 24 | (n % 3 == 0 ? aCor[i] & 0xFFFFFF : 0xFFFFFF));
                c.drawLine(ax[i] + ca * d0, ay[i] + sa * d0, ax[i] + ca * d1, ay[i] + sa * d1, paint);
            }
        }
        paint.setStyle(Paint.Style.FILL);
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
