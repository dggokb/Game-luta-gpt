package com.gamelutagpt;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import java.util.Random;

/**
 * Desgaste da luta (battle damage) sem refazer sprite: conforme a vida cai o lutador passa por
 * 3 estágios (66%, 33% e 10%). Cada estágio deixa o desenho mais apagado e sujo, com poeira
 * subindo dos pés; na troca de estágio estoura poeira e farpas de roupa. Os golpes fortes
 * rasgam farpas da roupa na cor do próprio lutador, e quem já está cansado espirra suor. No
 * vermelho ele fica exausto: respira ofegante e sua. O sangue existe, mas está desligado
 * ({@link #SANGUE}). Só
 * apresentação (espaço do mundo): nada aqui muda a luta.
 */
final class DesgasteDaLuta {
    /**
     * Sangue: desligado (ficou pesado demais para o tom do jogo). Ligado, os golpes soltam gotas
     * que caem e mancham o chão, e no vermelho ele pinga.
     */
    static final boolean SANGUE = false;
    /** Vida (fração) em que cada estágio começa: 1 abaixo de 66%, 2 abaixo de 33%, 3 abaixo de 10%. */
    private static final float[] LIMITES = {0.66f, 0.33f, 0.10f};

    private static final int GOTAS = 90, MANCHAS = 26, FARPAS = 36, SUORES = 24, BAFOS = 8, POEIRAS = 30;
    private static final int VIDA_MANCHA = 420, VIDA_FARPA = 80, VIDA_SUOR = 40, VIDA_BAFO = 44, VIDA_POEIRA = 34;
    private static final float GRAVIDADE = 2600f, PASSO = 1f / 60f;

    private final Random sorte = new Random(47L);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    // Gotas de sangue no ar: posição, velocidade, tamanho, idade (−1 livre). Caem e viram mancha.
    private final float[] gx = new float[GOTAS], gy = new float[GOTAS], gvx = new float[GOTAS],
        gvy = new float[GOTAS], gs = new float[GOTAS];
    private final int[] gIdade = livres(GOTAS);
    // Manchas no chão: x, tamanho, achatamento.
    private final float[] mx = new float[MANCHAS], ms = new float[MANCHAS], ma = new float[MANCHAS];
    private final int[] mIdade = livres(MANCHAS);
    // Farpas de roupa: posição, velocidade, giro, tamanho, cor.
    private final float[] fx = new float[FARPAS], fy = new float[FARPAS], fvx = new float[FARPAS],
        fvy = new float[FARPAS], fr = new float[FARPAS], fvr = new float[FARPAS], fs = new float[FARPAS];
    private final int[] fIdade = livres(FARPAS), fCor = new int[FARPAS];
    // Suor: gotinhas claras, com gravidade.
    private final float[] sx = new float[SUORES], sy = new float[SUORES], svx = new float[SUORES], svy = new float[SUORES];
    private final int[] sIdade = livres(SUORES);
    // Bafo ofegante: nuvenzinha saindo da boca.
    private final float[] bx = new float[BAFOS], by = new float[BAFOS], bvx = new float[BAFOS];
    private final int[] bIdade = livres(BAFOS);
    // Poeira da troca de estágio.
    private final float[] px = new float[POEIRAS], py = new float[POEIRAS], pvx = new float[POEIRAS],
        pvy = new float[POEIRAS], ps = new float[POEIRAS];
    private final int[] pIdade = livres(POEIRAS);

    /** O estágio de cada lado no quadro passado, e de quem (a troca de dupla não estoura nada). */
    private final int[] estagioVisto = new int[2];
    private final FighterState[] donoVisto = new FighterState[2];

    /** Filtros do desenho por estágio (0 é o desenho limpo) e da poeira nas pernas. */
    private final ColorFilter[] tintas = {null, tinta(0.93f, 0.90f, 6f), tinta(0.85f, 0.78f, 10f), tinta(0.77f, 0.66f, 14f)};
    private final ColorFilter[] sujeira = new ColorFilter[4];

    DesgasteDaLuta() {
        for (int k = 1; k < 4; k++) {
            sujeira[k] = new PorterDuffColorFilter(Color.argb(18 + 22 * k, 92, 70, 48), PorterDuff.Mode.SRC_ATOP);
        }
    }

    private static int[] livres(int n) {
        int[] a = new int[n];
        java.util.Arrays.fill(a, -1);
        return a;
    }

    /** Apagado (brilho), sem cor (saturação) e puxado para o marrom de poeira. */
    private static ColorFilter tinta(float brilho, float saturacao, float terra) {
        ColorMatrix m = new ColorMatrix();
        m.setSaturation(saturacao);
        ColorMatrix escuro = new ColorMatrix(new float[] {
            brilho, 0f, 0f, 0f, terra,
            0f, brilho, 0f, 0f, terra * 0.55f,
            0f, 0f, brilho, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        });
        m.postConcat(escuro);
        return new ColorMatrixColorFilter(m);
    }

    /** 0 (inteiro) a 3 (no fim) pela fração de vida. */
    static int estagio(FighterState s) {
        float vida = s.life / (float)Math.max(1, s.profile.maxLife);
        int k = 0;
        while (k < LIMITES.length && vida < LIMITES[k]) k++;
        return k;
    }

    /** O filtro do desenho do lutador (null quando inteiro). */
    ColorFilter tinta(FighterState s) {
        return tintas[estagio(s)];
    }

    /** O filtro da poeira nas pernas (null quando inteiro). */
    ColorFilter sujeira(FighterState s) {
        return sujeira[estagio(s)];
    }

    /** Respiração ofegante no vermelho: quanto o corpo infla agora (1 = parado). */
    static float folego(FighterState s, int quadro) {
        if (estagio(s) < 3) return 1f;
        return 1f + 0.016f * (float)Math.sin(quadro * 0.16f);
    }

    /** Um passo de 60 Hz: troca de estágio, exaustão e a física de tudo no ar. */
    void passo(CombatEngine engine, int quadro, int[] corRoupa) {
        for (int i = 0; i < 2; i++) {
            CombatFighter f = engine.fighter(i);
            int e = estagio(f.state);
            if (donoVisto[i] == f.state && e > estagioVisto[i] && f.vanishFrames <= 0) estouro(f, e, corRoupa[i]);
            donoVisto[i] = f.state;
            estagioVisto[i] = e;
            if (f.vanishFrames > 0) continue;
            float alto = f.character().visualStandHeight;
            float bocaX = f.x + f.facing * alto * 0.10f, bocaY = f.y - alto * 0.86f;
            if (e >= 3 && !f.ko()) {
                // Exausto: bafo ofegante, suor e sangue pingando.
                if (quadro % 40 == 0 && !f.attacking()) bafo(bocaX, bocaY, f.facing);
                if (quadro % 23 == 0) suor(f.x + (sorte.nextFloat() - 0.5f) * 40f, f.y - alto * (0.70f + sorte.nextFloat() * 0.2f), 0f, 0f);
                if (SANGUE && quadro % 37 == 0) gota(bocaX, bocaY + 14f, f.facing * 20f, 0f, 3f);
            } else if (e >= 2 && quadro % 47 == 0) {
                suor(f.x + (sorte.nextFloat() - 0.5f) * 40f, f.y - alto * 0.8f, 0f, 0f);
            }
        }
        // Gotas: caem e viram mancha no chão.
        for (int i = 0; i < GOTAS; i++) {
            if (gIdade[i] < 0) continue;
            gIdade[i]++;
            gvy[i] += GRAVIDADE * 0.75f * PASSO;
            gx[i] += gvx[i] * PASSO;
            gy[i] += gvy[i] * PASSO;
            if (gy[i] >= Arena.GROUND_Y) {
                if (gs[i] >= 2.5f || sorte.nextInt(3) == 0) mancha(gx[i], gs[i]);
                gIdade[i] = -1;
            } else if (gIdade[i] > 120) {
                gIdade[i] = -1;
            }
        }
        for (int i = 0; i < FARPAS; i++) {
            if (fIdade[i] < 0) continue;
            if (++fIdade[i] >= VIDA_FARPA) { fIdade[i] = -1; continue; }
            if (fy[i] < Arena.GROUND_Y - 2f) {
                // Pano: cai devagar, balançando de lado.
                fvy[i] = Math.min(fvy[i] + GRAVIDADE * 0.35f * PASSO, 260f);
                fvx[i] *= 0.95f;
                fx[i] += (fvx[i] + (float)Math.sin(fIdade[i] * 0.3f + i) * 60f) * PASSO;
                fy[i] += fvy[i] * PASSO;
                fr[i] += fvr[i] * PASSO;
            } else {
                fy[i] = Arena.GROUND_Y - 2f;
            }
        }
        for (int i = 0; i < SUORES; i++) {
            if (sIdade[i] < 0) continue;
            if (++sIdade[i] >= VIDA_SUOR || sy[i] > Arena.GROUND_Y) { sIdade[i] = -1; continue; }
            svy[i] += GRAVIDADE * 0.8f * PASSO;
            sx[i] += svx[i] * PASSO;
            sy[i] += svy[i] * PASSO;
        }
        for (int i = 0; i < BAFOS; i++) {
            if (bIdade[i] < 0) continue;
            if (++bIdade[i] >= VIDA_BAFO) { bIdade[i] = -1; continue; }
            bx[i] += bvx[i] * PASSO;
            by[i] -= 14f * PASSO;
        }
        for (int i = 0; i < POEIRAS; i++) {
            if (pIdade[i] < 0) continue;
            if (++pIdade[i] >= VIDA_POEIRA) { pIdade[i] = -1; continue; }
            pvx[i] *= 0.9f;
            pvy[i] *= 0.9f;
            px[i] += pvx[i] * PASSO;
            py[i] += pvy[i] * PASSO;
        }
        for (int i = 0; i < MANCHAS; i++) {
            if (mIdade[i] >= 0 && ++mIdade[i] >= VIDA_MANCHA) mIdade[i] = -1;
        }
    }

    /**
     * Um golpe entrou: sangue saindo do ponto de contato para longe de quem bateu (mais no forte),
     * farpas da roupa no golpe forte, e suor espirrando em quem já está cansado.
     */
    void golpe(CombatEngine.HitEvent e, CombatFighter apanhou, int corRoupa) {
        if (e.blocked || e.damage <= 0) return;
        float alto = apanhou.character().visualStandHeight;
        float x = Float.isNaN(e.impactX) ? apanhou.x : e.impactX;
        float y = Float.isNaN(e.impactY) ? apanhou.y - alto * 0.65f : e.impactY;
        // Para longe de quem bateu: o lado oposto ao que ele olha.
        int lado = -apanhou.facing;
        boolean forte = e.strength == AttackDefinition.Strength.HEAVY || e.strength == AttackDefinition.Strength.SUPER
            || "SUPER".equals(e.moveId) || "ULTRA".equals(e.moveId) || e.damage >= 900;
        if (SANGUE) {
            int n = Math.min(18, 3 + e.damage / 140);
            for (int k = 0; k < n; k++) {
                float v = 240f + sorte.nextFloat() * (forte ? 560f : 320f);
                double a = Math.toRadians(-75 + sorte.nextFloat() * 70);
                gota(x, y, lado * v * (float)Math.cos(a), v * (float)Math.sin(a), 2f + sorte.nextFloat() * (forte ? 4f : 2.4f));
            }
        }
        if (forte) {
            int n = 2 + sorte.nextInt(2);
            for (int k = 0; k < n; k++) farpa(x, y, lado, corRoupa);
        }
        if (estagio(apanhou.state) >= 2) {
            for (int k = 0; k < 4; k++) {
                suor(x, y - 20f, lado * (120f + sorte.nextFloat() * 220f), -200f - sorte.nextFloat() * 200f);
            }
        }
    }

    /** Passou de estágio: um estouro de poeira, farpas e sangue para o jogador perceber. */
    private void estouro(CombatFighter f, int estagio, int corRoupa) {
        float alto = f.character().visualStandHeight;
        for (int k = 0; k < 10; k++) {
            double a = sorte.nextDouble() * Math.PI * 2.0;
            float v = 140f + sorte.nextFloat() * 160f;
            int i = livre(pIdade);
            px[i] = f.x + (sorte.nextFloat() - 0.5f) * 60f;
            py[i] = f.y - alto * (0.15f + sorte.nextFloat() * 0.6f);
            pvx[i] = (float)Math.cos(a) * v;
            pvy[i] = (float)Math.sin(a) * v * 0.6f;
            ps[i] = 16f + sorte.nextFloat() * 18f;
            pIdade[i] = 0;
        }
        for (int k = 0; k < 1 + estagio; k++) farpa(f.x, f.y - alto * 0.55f, sorte.nextBoolean() ? 1 : -1, corRoupa);
        if (SANGUE) {
            for (int k = 0; k < 4 + 2 * estagio; k++) {
                float v = 160f + sorte.nextFloat() * 240f;
                double a = Math.toRadians(-150 + sorte.nextFloat() * 120);
                gota(f.x, f.y - alto * 0.7f, v * (float)Math.cos(a), v * (float)Math.sin(a), 1.5f + sorte.nextFloat() * 2.5f);
            }
        }
    }

    private void gota(float x, float y, float vx, float vy, float tamanho) {
        int i = livre(gIdade);
        gx[i] = x;
        gy[i] = y;
        gvx[i] = vx;
        gvy[i] = vy;
        gs[i] = tamanho;
        gIdade[i] = 0;
    }

    private void mancha(float x, float tamanho) {
        int i = livre(mIdade);
        mx[i] = x;
        ms[i] = tamanho * (1.6f + sorte.nextFloat());
        ma[i] = 0.28f + sorte.nextFloat() * 0.12f;
        mIdade[i] = 0;
    }

    private void farpa(float x, float y, int lado, int cor) {
        int i = livre(fIdade);
        fx[i] = x + (sorte.nextFloat() - 0.5f) * 30f;
        fy[i] = y + (sorte.nextFloat() - 0.5f) * 50f;
        fvx[i] = lado * (140f + sorte.nextFloat() * 260f);
        fvy[i] = -260f - sorte.nextFloat() * 260f;
        fr[i] = sorte.nextFloat() * 360f;
        fvr[i] = (sorte.nextFloat() * 2f - 1f) * 720f;
        fs[i] = 3.5f + sorte.nextFloat() * 3.5f;
        fCor[i] = cor;
        fIdade[i] = 0;
    }

    private void suor(float x, float y, float vx, float vy) {
        int i = livre(sIdade);
        sx[i] = x;
        sy[i] = y;
        svx[i] = vx;
        svy[i] = vy;
        sIdade[i] = 0;
    }

    private void bafo(float x, float y, int lado) {
        int i = livre(bIdade);
        bx[i] = x;
        by[i] = y;
        bvx[i] = lado * 40f;
        bIdade[i] = 0;
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

    /** Atrás dos lutadores: as manchas de sangue no chão, sumindo devagar. */
    void desenharChao(Canvas c) {
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < MANCHAS; i++) {
            if (mIdade[i] < 0) continue;
            float k = Math.min(1f, (VIDA_MANCHA - mIdade[i]) / 90f);
            float r = ms[i];
            paint.setColor(Color.argb(Math.round(190 * k), 96, 6, 10));
            c.drawOval(mx[i] - r, Arena.GROUND_Y - r * ma[i], mx[i] + r, Arena.GROUND_Y + r * ma[i], paint);
            paint.setColor(Color.argb(Math.round(120 * k), 150, 18, 20));
            c.drawOval(mx[i] - r * 0.55f, Arena.GROUND_Y - r * ma[i] * 0.5f, mx[i] + r * 0.35f, Arena.GROUND_Y + r * ma[i] * 0.4f, paint);
        }
    }

    /** Na frente: poeira, farpas, sangue no ar, suor e bafo. */
    void desenhar(Canvas c) {
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < POEIRAS; i++) {
            if (pIdade[i] < 0) continue;
            float t = pIdade[i] / (float)VIDA_POEIRA;
            float r = ps[i] * (0.6f + t);
            paint.setColor(Color.argb(Math.round(120 * (1f - t)), 150, 128, 104));
            c.drawCircle(px[i], py[i], r, paint);
        }
        for (int i = 0; i < FARPAS; i++) {
            if (fIdade[i] < 0) continue;
            float k = Math.min(1f, (VIDA_FARPA - fIdade[i]) / 20f);
            float s = fs[i];
            c.save();
            c.rotate(fr[i], fx[i], fy[i]);
            // Um retalho torto: largo de um lado, rasgado do outro.
            path.reset();
            path.moveTo(fx[i] - s, fy[i] - s * 0.5f);
            path.lineTo(fx[i] + s * 0.9f, fy[i] - s * 0.7f);
            path.lineTo(fx[i] + s * 0.4f, fy[i] - s * 0.1f);
            path.lineTo(fx[i] + s, fy[i] + s * 0.5f);
            path.lineTo(fx[i] - s * 0.7f, fy[i] + s * 0.6f);
            path.close();
            paint.setColor((Math.round(255 * k) << 24) | (fCor[i] & 0xFFFFFF));
            c.drawPath(path, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.2f);
            paint.setColor(Color.argb(Math.round(160 * k), 20, 16, 14));
            c.drawPath(path, paint);
            paint.setStyle(Paint.Style.FILL);
            c.restore();
        }
        for (int i = 0; i < GOTAS; i++) {
            if (gIdade[i] < 0) continue;
            float r = gs[i];
            // Esticada na direção em que voa.
            float v = (float)Math.hypot(gvx[i], gvy[i]);
            float dx = v > 1f ? gvx[i] / v : 0f, dy = v > 1f ? gvy[i] / v : 1f;
            float rabo = Math.min(r * 1.6f, v * 0.004f);
            paint.setColor(0xFF8E0A10);
            c.drawCircle(gx[i], gy[i], r, paint);
            if (rabo > 0.5f) {
                // Um rabinho curto e fino atrás da gota (não um traço).
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeWidth(r * 0.9f);
                c.drawLine(gx[i], gy[i], gx[i] - dx * rabo, gy[i] - dy * rabo, paint);
                paint.setStyle(Paint.Style.FILL);
            }
            paint.setColor(0xCCD8323C);
            c.drawCircle(gx[i] - r * 0.3f, gy[i] - r * 0.3f, r * 0.45f, paint);
        }
        for (int i = 0; i < SUORES; i++) {
            if (sIdade[i] < 0) continue;
            float k = 1f - sIdade[i] / (float)VIDA_SUOR;
            paint.setColor(Color.argb(Math.round(220 * k), 214, 236, 255));
            c.drawCircle(sx[i], sy[i], 2.6f, paint);
            paint.setColor(Color.argb(Math.round(255 * k), 255, 255, 255));
            c.drawCircle(sx[i] - 0.8f, sy[i] - 0.8f, 1f, paint);
        }
        for (int i = 0; i < BAFOS; i++) {
            if (bIdade[i] < 0) continue;
            float t = bIdade[i] / (float)VIDA_BAFO;
            // Três fiapos claros que abrem e somem (sem bola branca na cara).
            float r = 3f + 9f * t;
            int a = Math.round(60 * (1f - t) * Math.min(1f, t * 5f));
            paint.setColor(Color.argb(a, 236, 240, 246));
            float frente = bvx[i] * 0.25f * (0.4f + t);
            c.drawCircle(bx[i] + frente, by[i], r, paint);
            c.drawCircle(bx[i] + frente * 1.5f, by[i] - r * 0.5f, r * 0.75f, paint);
            c.drawCircle(bx[i] + frente * 0.6f, by[i] + r * 0.4f, r * 0.6f, paint);
        }
        paint.setStrokeCap(Paint.Cap.BUTT);
    }
}
