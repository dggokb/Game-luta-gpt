package com.gamelutagpt;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import java.util.Random;

/**
 * Raios estalando no cenário enquanto os dois lutadores estão no SHINKAI (pedido do jogador, 03/11):
 * de vez em quando um raio cai no fundo, na cor do SHINKAI de um ou do outro (alternando), e às vezes
 * um estala entre os dois, como as auras se chocando no ar. Cada um vive uns 0,15 s.
 *
 * Leve de propósito: no máximo {@link #VAGAS} raios ao mesmo tempo, cada um um caminho de ~10
 * pedaços desenhado duas vezes (o brilho largo e o miolo); os caminhos são montados só quando o raio
 * nasce e reaproveitados. No gráfico LEVE eles caem com metade da frequência.
 */
final class RaiosShinkai {
    static final int VAGAS = 4;
    /** Quadros de vida de cada raio (60 Hz). */
    static final int VIDA = 9;
    /** Entre um raio e o próximo (quadros, sorteado). */
    static final int INTERVALO_MIN = 18, INTERVALO_MAX = 48;

    private final Path[] caminhos = new Path[VAGAS];
    private final int[] idade = new int[VAGAS], cor = new int[VAGAS];
    private final boolean[] vivo = new boolean[VAGAS];
    private final Random sorteio;
    private final Paint brilho = new Paint(Paint.ANTI_ALIAS_FLAG), miolo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int proximo = 12, vez;

    RaiosShinkai(long semente) {
        sorteio = new Random(semente);
        for (int i = 0; i < VAGAS; i++) caminhos[i] = new Path();
        for (Paint p : new Paint[] {brilho, miolo}) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
        }
    }

    /** Quantos raios estão na tela agora (testes). */
    int vivos() {
        int n = 0;
        for (boolean v : vivo) if (v) n++;
        return n;
    }

    /**
     * Um passo de 60 Hz. {@code ativos}: os dois no SHINKAI. As cores são as dos tipos; os peitos dos
     * dois e a parte do mundo que a câmera mostra decidem onde os raios caem.
     */
    void passo(boolean ativos, int corA, int corB, float xA, float yA, float xB, float yB,
               float esquerda, float direita, float topo, float chao, boolean leve) {
        for (int i = 0; i < VAGAS; i++) {
            if (vivo[i] && ++idade[i] >= VIDA) vivo[i] = false;
        }
        if (!ativos) {
            proximo = 12;
            return;
        }
        if (--proximo > 0) return;
        int base = INTERVALO_MIN + sorteio.nextInt(INTERVALO_MAX - INTERVALO_MIN + 1);
        proximo = leve ? base * 2 : base;
        int vaga = livre();
        if (vaga < 0) return;
        int c = (vez++ % 2 == 0) ? corA : corB;
        Path p = caminhos[vaga];
        p.rewind();
        if (sorteio.nextFloat() < 0.3f && Math.abs(xB - xA) > 80f) {
            // Entre os dois: de um peito ao outro, torto, como as auras se encontrando.
            ziguezague(p, xA, yA, xB, yB, 10, 34f);
        } else {
            // No fundo: do alto da tela até um pouco acima do chão, com um galho.
            float x = esquerda + (direita - esquerda) * (0.08f + 0.84f * sorteio.nextFloat());
            float fim = topo + (chao - topo) * (0.45f + 0.3f * sorteio.nextFloat());
            float desvio = (sorteio.nextFloat() - 0.5f) * 160f;
            ziguezague(p, x, topo - 20f, x + desvio, fim, 11, 42f);
            float meio = topo + (fim - topo) * (0.35f + 0.25f * sorteio.nextFloat());
            float gx = x + desvio * (meio - topo) / Math.max(1f, fim - topo);
            ziguezague(p, gx, meio, gx + (sorteio.nextBoolean() ? 1 : -1) * (70f + 60f * sorteio.nextFloat()),
                meio + 60f + 60f * sorteio.nextFloat(), 5, 22f);
        }
        vivo[vaga] = true;
        idade[vaga] = 0;
        cor[vaga] = c;
    }

    private int livre() {
        for (int i = 0; i < VAGAS; i++) if (!vivo[i]) return i;
        return -1;
    }

    /** Linha quebrada de (x0, y0) a (x1, y1) em {@code pedacos}, cada canto desviado até {@code amplitude}. */
    private void ziguezague(Path p, float x0, float y0, float x1, float y1, int pedacos, float amplitude) {
        float dx = x1 - x0, dy = y1 - y0;
        float len = (float)Math.hypot(dx, dy);
        if (len < 1f) return;
        float nx = -dy / len, ny = dx / len;
        p.moveTo(x0, y0);
        for (int k = 1; k < pedacos; k++) {
            float t = k / (float)pedacos;
            float d = (sorteio.nextFloat() * 2f - 1f) * amplitude;
            p.lineTo(x0 + dx * t + nx * d, y0 + dy * t + ny * d);
        }
        p.lineTo(x1, y1);
    }

    /** Desenha no espaço do mundo (o canvas já com a câmera), atrás dos lutadores. */
    void desenhar(Canvas c) {
        for (int i = 0; i < VAGAS; i++) {
            if (!vivo[i]) continue;
            // Estala: forte no começo, pisca uma vez e some.
            float t = idade[i] / (float)VIDA;
            float a = (1f - t) * (idade[i] == 3 ? 0.45f : 1f);
            int rgb = cor[i] & 0x00FFFFFF;
            brilho.setColor(rgb | (Math.round(110 * a) << 24));
            brilho.setStrokeWidth(13f);
            c.drawPath(caminhos[i], brilho);
            // O miolo: quase branco, puxado para a cor.
            int r = (((rgb >> 16) & 0xFF) + 255 * 2) / 3, g = (((rgb >> 8) & 0xFF) + 255 * 2) / 3, b = ((rgb & 0xFF) + 255 * 2) / 3;
            miolo.setColor((Math.round(255 * a) << 24) | (r << 16) | (g << 8) | b);
            miolo.setStrokeWidth(3.5f);
            c.drawPath(caminhos[i], miolo);
        }
    }

    void limpar() {
        for (int i = 0; i < VAGAS; i++) vivo[i] = false;
        proximo = 12;
    }
}
