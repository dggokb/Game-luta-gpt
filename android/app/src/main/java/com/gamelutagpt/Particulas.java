package com.gamelutagpt;

import com.gamelutagpt.render.RenderCanvas;
import java.util.List;

/**
 * Partículas dos poderes: as fagulhas que a bola de energia solta no voo, o estouro dela quando
 * acerta (ou é defendida, ou acaba no ar) e a fita que um objeto arremessado deixa no ar (kunai,
 * machado, bisturi, pedra...). Golpe comum não passa por aqui: pancada é ar deslocado
 * (docs/revisao-base/direcao.md); isto é só para quem solta poder.
 *
 * <p>No traço do jogo: borda dura e tons chapados. Cada partícula é um caco (losango esticado no
 * sentido em que anda) na cor do poder com o miolo branco; ela não desbota, encolhe até sumir. A
 * fita é uma faixa que afina até a ponta, clara, com o fio branco no meio.
 *
 * <p>Fila fixa no relógio de 60 Hz da luta, como o ImpactEffects: nascer reaproveita lugar,
 * desenhar não aloca nada e nada aqui mexe no combate. Desenha só com polígonos do RenderCanvas
 * (em OpenGL, um leque de triângulos por caco), sem nada do Android: roda igual no celular, no PC
 * e nos testes.
 */
final class Particulas {
    static final int NENHUM = 0, ENERGIA = 1, OBJETO = 2;

    private static final int MAX = 224;
    private final float[] x = new float[MAX], y = new float[MAX], vx = new float[MAX], vy = new float[MAX];
    private final float[] idade = new float[MAX], vida = new float[MAX], tam = new float[MAX];
    /** Quanto da velocidade sobra depois de 1 s (o atrito) e a gravidade (negativa: sobe, a brasa). */
    private final float[] sobra = new float[MAX], peso = new float[MAX];
    private final int[] cor = new int[MAX];
    private int proxima;
    private int semente = 0x51F15E;

    /** Projéteis acompanhados: quem é, de que tipo, onde estava e a fita (os últimos pontos). */
    private static final int DONOS = 10, PONTOS = 9;
    private final Projectile[] dono = new Projectile[DONOS];
    private final int[] tipo = new int[DONOS], donoCor = new int[DONOS], pontos = new int[DONOS];
    private final float[] donoX = new float[DONOS], donoY = new float[DONOS], donoRaio = new float[DONOS];
    private final int[] donoDirecao = new int[DONOS];
    private final boolean[] visto = new boolean[DONOS], acertou = new boolean[DONOS];
    private final float[][] fitaX = new float[DONOS][PONTOS], fitaY = new float[DONOS][PONTOS];

    /** A fita de ar de um objeto: lilás claro por fora (a borda fria dos estouros de ar), branco no fio. */
    private static final int FITA_COR = 0xB4BEC8FF, FITA_FIO = 0xFFFFFFFF;

    Particulas() {
        java.util.Arrays.fill(idade, 99f);
        java.util.Arrays.fill(vida, 1f);
    }

    void limpar() {
        java.util.Arrays.fill(idade, 99f);
        for (int i = 0; i < DONOS; i++) {
            dono[i] = null;
            tipo[i] = NENHUM;
            pontos[i] = 0;
        }
    }

    /** O que o projétil solta: energia (fagulhas e estouro), objeto (fita) ou nada. */
    static int tipoDe(Projectile p) {
        if (p.returning || p.speed <= 0f) return NENHUM;
        String v = p.visual;
        if (v == null || "ONDA".equals(v) || "ONDA_CURTA".equals(v) || "PALMA".equals(v)) return ENERGIA;
        if ("KUNAI".equals(v) || "SHURIKEN".equals(v) || "MACHADO".equals(v) || "MACHADINHO".equals(v) ||
            "BISTURI".equals(v) || "SERINGA".equals(v) || "PEDRA".equals(v)) {
            return OBJETO;
        }
        return NENHUM;
    }

    /** Um quadro da luta: anda as partículas e acompanha os projéteis que estão no ar. */
    void passo(float dt, List<Projectile> energia, List<Projectile> supers) {
        for (int i = 0; i < MAX; i++) {
            if (idade[i] >= vida[i]) continue;
            idade[i] += dt;
            float atrito = (float)Math.pow(sobra[i], dt);
            vx[i] *= atrito;
            vy[i] = vy[i] * atrito + peso[i] * dt;
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;
        }
        for (int i = 0; i < DONOS; i++) visto[i] = false;
        for (int i = 0; i < energia.size(); i++) acompanhar(energia.get(i), false);
        for (int i = 0; i < supers.size(); i++) acompanhar(supers.get(i), true);
        for (int i = 0; i < DONOS; i++) {
            if (visto[i] || tipo[i] == NENHUM) continue;
            if (dono[i] != null) {
                // Saiu de cena. A energia que acabou no ar (fim do alcance, choque) se desfaz em fagulhas;
                // a que acertou já estourou no acerto.
                if (tipo[i] == ENERGIA && !acertou[i]) {
                    soltar(donoX[i], donoY[i], donoDirecao[i], donoCor[i], 6, donoRaio[i] / 24f, 0.5f, false);
                }
                dono[i] = null;
            }
            // A fita de quem sumiu recolhe ponto a ponto, não some de uma vez.
            if (pontos[i] > 0) pontos[i]--;
            if (pontos[i] == 0) tipo[i] = NENHUM;
        }
    }

    private void acompanhar(Projectile p, boolean superGolpe) {
        int t = tipoDe(p);
        if (t == NENHUM) return;
        int i = lugarDe(p);
        if (i < 0) return;
        if (dono[i] != p) {
            dono[i] = p;
            pontos[i] = 0;
        }
        tipo[i] = t;
        visto[i] = true;
        acertou[i] = false;
        donoX[i] = p.x;
        donoY[i] = p.y;
        donoRaio[i] = p.radius;
        donoDirecao[i] = p.direction;
        donoCor[i] = p.color;
        float k = Math.max(0.45f, p.radius / 24f);
        if (t == OBJETO) {
            // A fita: o ponto novo entra na frente e os antigos andam uma casa.
            int n = Math.min(PONTOS - 1, pontos[i]);
            for (int j = n; j > 0; j--) {
                fitaX[i][j] = fitaX[i][j - 1];
                fitaY[i][j] = fitaY[i][j - 1];
            }
            fitaX[i][0] = p.x;
            fitaY[i][0] = p.y;
            pontos[i] = n + 1;
            return;
        }
        if (p.holding) {
            // A onda do Super parada no rival entre um acerto e outro: solta fagulhas para todo lado.
            if (sorte() < 0.25f) soltar(p.x, p.y, p.direction, p.color, 1, Math.min(1.25f, k), 0.8f, false);
            return;
        }
        // Em voo: fagulhas se soltam da borda do cometa (em cima e embaixo, ao longo da cauda), abrem para
        // fora e ficam para trás (andam mais devagar que ele). Dentro da bola não se veria nada.
        int quantas = sorte() < (superGolpe ? 1f : 0.8f) ? 1 : 0;
        for (int n = 0; n < quantas; n++) {
            int j = nova();
            float lado = sorte() < 0.5f ? -1f : 1f, cauda = sorte();
            x[j] = p.x - p.direction * p.radius * (0.2f + 3.2f * cauda);
            y[j] = p.y + lado * p.radius * (1.15f - 0.45f * cauda);
            vx[j] = p.direction * p.speed * (0.15f + 0.25f * sorte());
            vy[j] = lado * (70f + 130f * sorte()) - 20f;
            idade[j] = 0f;
            vida[j] = 0.18f + 0.16f * sorte();
            tam[j] = (7f + 5f * sorte()) * Math.min(1.25f, k);
            sobra[j] = 0.12f;
            peso[j] = -60f;
            cor[j] = p.color;
        }
    }

    private int lugarDe(Projectile p) {
        int livre = -1;
        for (int i = 0; i < DONOS; i++) {
            if (dono[i] == p) return i;
            if (livre < 0 && dono[i] == null && tipo[i] == NENHUM) livre = i;
        }
        return livre;
    }

    /**
     * O projétil acertou (ou foi defendido) em (x, y). Se era uma bola de energia, ela estoura na cor
     * dela: as fagulhas seguem o sentido do golpe (atravessam quem apanhou); na defesa, voltam e
     * sobem. Objeto não estoura (o acerto dele já é o impacto de ar). True: estourou.
     */
    boolean acerto(float ax, float ay, int direcao, boolean defendido, boolean superGolpe) {
        int melhor = -1;
        float perto = 220f * 220f;
        for (int i = 0; i < DONOS; i++) {
            if (dono[i] == null) continue;
            float dx = donoX[i] - ax, dy = donoY[i] - ay, d = dx * dx + dy * dy;
            if (d < perto) {
                perto = d;
                melhor = i;
            }
        }
        if (melhor < 0 || tipo[melhor] != ENERGIA) return false;
        acertou[melhor] = true;
        float k = Math.min(1.25f, Math.max(0.6f, donoRaio[melhor] / 24f));
        // A onda de vários acertos (a Onda Máxima) solta pouco a cada pancada e guarda o estouro para a última.
        Projectile bola = dono[melhor];
        boolean meio = bola.totalHits > 1 && bola.hitsDone < bola.totalHits;
        soltar(ax, ay, direcao, donoCor[melhor], defendido ? 8 : meio ? 6 : superGolpe ? 20 : 14, k, 1f, defendido);
        // Brasas: poucas, lentas, sobem e somem depois das fagulhas.
        int brasas = meio ? 1 : defendido ? 2 : superGolpe ? 6 : 4;
        for (int n = 0; n < brasas; n++) {
            int j = nova();
            x[j] = ax + (sorte() * 2f - 1f) * 44f * k;
            y[j] = ay + (sorte() * 2f - 1f) * 44f * k;
            vx[j] = direcao * (30f + 110f * sorte());
            vy[j] = -40f - 90f * sorte();
            idade[j] = 0f;
            vida[j] = 0.42f + 0.3f * sorte();
            tam[j] = (6f + 4f * sorte()) * k;
            sobra[j] = 0.25f;
            peso[j] = -140f;
            cor[j] = donoCor[melhor];
        }
        return true;
    }

    /** Fagulhas em leque a partir de um ponto; {@code forca} 0..1 encurta o voo; {@code volta}: rebatidas. */
    private void soltar(float ox, float oy, int direcao, int c, int quantas, float k, float forca, boolean volta) {
        for (int n = 0; n < quantas; n++) {
            int j = nova();
            // Três em cada quatro vão para o lado do golpe, num leque de ±65°; o resto para qualquer lado.
            float a = sorte() < 0.75f ? (sorte() * 2f - 1f) * 1.13f : (sorte() * 2f - 1f) * 3.1416f;
            float v = (420f + 520f * sorte()) * forca;
            float sentido = volta ? -direcao : direcao;
            x[j] = ox;
            y[j] = oy;
            vx[j] = sentido * (float)Math.cos(a) * v;
            vy[j] = (float)Math.sin(a) * v - (volta ? 160f : 40f);
            idade[j] = 0f;
            vida[j] = 0.26f + 0.2f * sorte();
            tam[j] = (9f + 7f * sorte()) * k;
            sobra[j] = 0.03f;
            peso[j] = 520f;
            cor[j] = c;
        }
    }

    private int nova() {
        int j = proxima;
        proxima = (proxima + 1) % MAX;
        return j;
    }

    /** Quantas partículas estão vivas (teste e depuração). */
    int vivas() {
        int n = 0;
        for (int i = 0; i < MAX; i++) if (idade[i] < vida[i]) n++;
        return n;
    }

    /** Quantos pontos tem a fita do projétil (0: não tem fita). */
    int pontosDaFita(Projectile p) {
        for (int i = 0; i < DONOS; i++) if (dono[i] == p && tipo[i] == OBJETO) return pontos[i];
        return 0;
    }

    /** Menor e maior x das partículas vivas, ou null sem nenhuma (teste). */
    float[] faixaX() {
        float a = Float.MAX_VALUE, b = -Float.MAX_VALUE;
        for (int i = 0; i < MAX; i++) {
            if (idade[i] >= vida[i]) continue;
            a = Math.min(a, x[i]);
            b = Math.max(b, x[i]);
        }
        return a > b ? null : new float[] {a, b};
    }

    /** As fitas dos objetos, atrás deles (chamar antes de desenhar os projéteis). */
    void desenharFitas(RenderCanvas c) {
        for (int i = 0; i < DONOS; i++) {
            if (tipo[i] != OBJETO || pontos[i] < 3) continue;
            // Fina: é o risco do ar atrás de uma lâmina, não pode esconder o objeto.
            float larg = Math.max(3f, Math.min(6f, donoRaio[i] * 0.22f));
            faixa(c, i, larg, FITA_COR);
            faixa(c, i, larg * 0.38f, FITA_FIO);
        }
    }

    /**
     * A faixa que afina do projétil até a ponta, um trecho (quadrilátero) por vez: cada trecho é
     * convexo mesmo com a fita em arco (a pedra), e o desenho em OpenGL não aloca nada.
     */
    private void faixa(RenderCanvas c, int i, float meia, int argb) {
        int n = pontos[i];
        float[] px = fitaX[i], py = fitaY[i];
        float ax = 0f, ay = 0f, bx = 0f, by = 0f;
        for (int j = 0; j < n; j++) {
            int antes = Math.max(0, j - 1), depois = Math.min(n - 1, j + 1);
            float dx = px[antes] - px[depois], dy = py[antes] - py[depois];
            float len = (float)Math.sqrt(dx * dx + dy * dy);
            if (len < 1e-3f) { dx = 1f; dy = 0f; len = 1f; }
            float w = meia * (1f - j / (float)(n - 1));
            float cx = px[j] - dy / len * w, cy = py[j] + dx / len * w;
            float ex = px[j] + dy / len * w, ey = py[j] - dx / len * w;
            if (j > 0) {
                quad[0] = ax; quad[1] = ay; quad[2] = cx; quad[3] = cy;
                quad[4] = ex; quad[5] = ey; quad[6] = bx; quad[7] = by;
                c.fillPolygon(quad, argb);
            }
            ax = cx; ay = cy; bx = ex; by = ey;
        }
    }

    /** As partículas, na frente dos lutadores (junto com os efeitos de impacto). */
    void desenhar(RenderCanvas c) {
        // O fundo colorido de todas e, por cima, o miolo branco das que ainda estão quentes.
        for (int j = 0; j < MAX; j++) {
            if (idade[j] >= vida[j]) continue;
            float k = idade[j] / vida[j];
            caco(c, j, 1f - k * k, cor[j] | 0xFF000000);
        }
        for (int j = 0; j < MAX; j++) {
            if (idade[j] >= vida[j]) continue;
            float k = idade[j] / vida[j];
            if (k < 0.55f) caco(c, j, 0.5f * (1f - k / 0.55f) + 0.12f, 0xFFFFFFFF);
        }
    }

    private final float[] quad = new float[8];

    /** O caco da partícula j: losango esticado no sentido em que ela anda, na escala pedida. */
    private void caco(RenderCanvas c, int j, float escala, int argb) {
        float v = (float)Math.sqrt(vx[j] * vx[j] + vy[j] * vy[j]);
        float ux = v < 1f ? 1f : vx[j] / v, uy = v < 1f ? 0f : vy[j] / v;
        float meia = tam[j] * 0.4f * escala;
        float comprido = (tam[j] * 1.7f + Math.min(46f, v * 0.05f)) * escala;
        float cx = x[j], cy = y[j];
        quad[0] = cx + ux * comprido * 0.4f; quad[1] = cy + uy * comprido * 0.4f;
        quad[2] = cx - uy * meia; quad[3] = cy + ux * meia;
        quad[4] = cx - ux * comprido * 0.6f; quad[5] = cy - uy * comprido * 0.6f;
        quad[6] = cx + uy * meia; quad[7] = cy - ux * meia;
        c.fillPolygon(quad, argb);
    }

    /** Sorteio pequeno e reprodutível (sem Random): os quadros de revisão saem iguais. */
    private float sorte() {
        semente = semente * 1103515245 + 12345;
        return ((semente >>> 8) & 0xFFFF) / 65536f;
    }
}
