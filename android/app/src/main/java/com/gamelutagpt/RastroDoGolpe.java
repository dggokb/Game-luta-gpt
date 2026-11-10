package com.gamelutagpt;

import android.graphics.Canvas;

/**
 * O rastro do golpe normal: uma fita de ar que segue a LINHA DE AÇÃO do golpe, o caminho que a
 * ponta do braço ou da perna fez de verdade, de onde saiu até onde chegou (a garra que desce em
 * arco, o chute que sai do chão, o soco que sai da guarda). O caminho é detectado na animação
 * por um modelo de pose (tools/sprites/linha_de_acao.py) e vem em
 * {@link CharacterDefinition.Animation#caminhoGolpe}: a ponta e a junta de trás (o cotovelo, o
 * joelho) em cada desenho do golpe.
 *
 * Aqui a fita nasce junto com o movimento: a cada desenho que a tela mostra, a cabeça dela vai
 * para onde a ponta do membro está, e o que ficou para trás some em {@link #VIDA} quadros. Ela
 * é atravessada no caminho, do lado do membro (larga no balanço, que varre uma área; fina na
 * estocada), e sai da cor do membro. É exagerada de propósito: nas referências (Marvel Tōkon,
 * docs/efeitos-referencia-tokon.md) o corte de ar é do tamanho do membro ou maior e fica na tela.
 * Na pausa do acerto o golpe não anda e a fita fica parada na tela, como o membro. Só apresentação; golpes sem caminho (os com efeito já desenhado na arte, os especiais)
 * ficam sem.
 */
final class RastroDoGolpe {
    private static final String FITA = "rastro_fita";
    /** Pontos do caminho (os desenhos do golpe até o membro esticado). */
    private static final int PONTOS = 8;
    /** Pedaços da fita entre dois pontos do caminho. */
    private static final int PASSOS = 8;
    /** Quadros que um ponto da fita dura depois que a ponta passou por ele (fraco, médio, forte). */
    private static final int[] VIDA = {8, 10, 13};
    /** O quanto do membro atravessado no caminho a fita cobre, e os limites em px (fraco, médio, forte). */
    private static final float[] LARGURA = {1.1f, 1.5f, 1.9f};
    private static final float[] LARGURA_MAX = {80f, 120f, 160f};
    private static final float LARGURA_MIN = 44f;

    private final EfeitosVideo efeitos;
    private final Lado[] lados = {new Lado(), new Lado()};
    /** A opacidade da fita cheia: ela passa na frente dos lutadores e o membro ainda se vê por baixo. */
    private static final float OPACIDADE = 0.9f;
    /** Amostras a mais na frente da ponta: o rastro passa um pouco dela e termina em pontas. */
    private static final int ALEM = 4;
    private static final int AMOSTRAS = PONTOS * PASSOS + 1 + ALEM;
    /** O caminho amostrado: a ponta, a junta, a normal do caminho e o quanto a junta fica para o lado dela. */
    private final float[] px = new float[AMOSTRAS], py = new float[AMOSTRAS], jx = new float[AMOSTRAS], jy = new float[AMOSTRAS];
    private final float[] nx = new float[AMOSTRAS], ny = new float[AMOSTRAS], lado = new float[AMOSTRAS];
    /** A fita: a borda nítida (fx, fy), a outra (dx, dy) e a opacidade. */
    private final float[] fx = new float[AMOSTRAS], fy = new float[AMOSTRAS], dx = new float[AMOSTRAS], dy = new float[AMOSTRAS];
    private final float[] alfa = new float[AMOSTRAS];
    /** O quanto da largura fica para fora da ponta no balanço (o punho e o pé têm grossura). */
    private static final float FORA = 0.22f;
    /** Menos que isto (px) de membro atravessado no caminho, em média, é estocada. */
    private static final float ESTOCADA = 24f;
    /** Na estocada: a largura do feixe de riscos em volta do membro (fraco, médio, forte). */
    private static final float[] FEIXE = {64f, 90f, 120f};
    /** O quanto o rastro passa da ponta do membro (px; fraco, médio, forte). */
    private static final float[] PASSA = {26f, 40f, 56f};

    /** O rastro do golpe em curso de um lutador. */
    private static final class Lado {
        AttackDefinition ataque;
        CharacterDefinition.Animation desenho;
        /** O ponto do caminho em que a ponta está (-1: o golpe ainda não saiu) e o último quadro visto. */
        int cabeca, quadro;
        /** Em que quadro do golpe a ponta chegou a cada ponto do caminho. */
        final int[] chegada = new int[PONTOS];
        float x, y;
        int facing, forca;
    }

    RastroDoGolpe(EfeitosVideo efeitos) {
        this.efeitos = efeitos;
    }

    void limpar() {
        for (Lado l : lados) l.ataque = null;
    }

    /** Um passo da luta: {@code x} é onde o lutador {@code i} está desenhado (com a troca de dupla). */
    void passo(int i, CombatFighter f, float x) {
        Lado l = lados[i];
        CharacterDefinition.Animation a = f.attacking() && f.attack != null ? CombatEngine.desenhoDoGolpe(f) : null;
        if (a == null || a.caminhoGolpe == null) {
            l.ataque = null;
            return;
        }
        if (f.attack != l.ataque || a != l.desenho || f.attackFrame < l.quadro) {
            l.ataque = f.attack;
            l.desenho = a;
            l.cabeca = -1;
            AttackDefinition.Strength s = f.attack.strength;
            l.forca = s == AttackDefinition.Strength.LIGHT ? 0 : s == AttackDefinition.Strength.MEDIUM ? 1 : 2;
        }
        l.quadro = f.attackFrame;
        l.x = x;
        l.y = f.y;
        l.facing = f.facing;
        // A cabeça da fita vai até o ponto do caminho do desenho que a tela mostra agora. O clipe é
        // esticado sobre o golpe e a tela pula desenhos: os pontos pulados chegam junto.
        int agora = a.ordemDe(CombatEngine.quadroDoDesenho(f, f.attackFrame));
        int[] ordem = a.caminhoOrdem;
        while (l.cabeca + 1 < ordem.length && l.cabeca + 1 < PONTOS && a.ordemDe(ordem[l.cabeca + 1]) <= agora) {
            l.chegada[++l.cabeca] = f.attackFrame;
        }
    }

    /** Desenha no espaço do mundo (o canvas já com a câmera), na frente dos lutadores. */
    void desenhar(Canvas c) {
        for (Lado l : lados) {
            if (l.ataque == null || l.cabeca < 1) continue;
            int vida = VIDA[l.forca];
            if (l.quadro - l.chegada[l.cabeca] >= vida) continue;   // o membro parou e a fita já sumiu
            CharacterDefinition.Animation a = l.desenho;
            int[] ordem = a.caminhoOrdem;
            int n = 0;
            for (int j = 0; j < l.cabeca; j++) {
                for (int k = 0; k < PASSOS; k++) n = amostra(l, a, ordem, j, k / (float) PASSOS, vida, n);
            }
            n = amostra(l, a, ordem, l.cabeca - 1, 1f, vida, n);
            float atravessado = normais(n);
            n = alem(l, n);
            // Só o trecho que ainda se vê, para a textura ir da ponta velha à ponta nova nele.
            int de = 0;
            while (de < n - 1 && alfa[de + 1] <= 0f) de++;
            if (n - de < 2) continue;
            if (Math.abs(atravessado) >= ESTOCADA) {
                // O balanço (o chute em arco, a garra): o membro vai atravessado no caminho e a fita
                // cobre a área que ele varreu, do lado dele, um pouco para fora da ponta.
                float sinal = atravessado < 0f ? -1f : 1f;
                for (int k = 0; k < n; k++) {
                    float largura = Math.max(LARGURA_MIN, Math.min(LARGURA_MAX[l.forca], Math.abs(lado[k]) * LARGURA[l.forca]));
                    borda(k, -sinal * largura * FORA, sinal * largura * (1f - FORA));
                }
                efeitos.fita(c, FITA, fx, fy, dx, dy, alfa, de, n, a.caminhoCor);
            } else {
                // A estocada (o soco reto, o chute de frente): o membro vai na direção do caminho. Um
                // feixe de riscos em volta dele, com o caminho no meio (o membro é desenhado por cima).
                float meia = FEIXE[l.forca] * 0.5f;
                for (int k = 0; k < n; k++) borda(k, -meia, meia);
                efeitos.fita(c, FITA, fx, fy, dx, dy, alfa, de, n, a.caminhoCor);
            }
        }
    }

    /**
     * Estica o caminho amostrado para além da ponta do membro, reto, na direção em que ele vinha: é
     * onde a textura termina em pontas soltas de tamanhos diferentes (o corte de ar passa do punho).
     */
    private int alem(Lado l, int n) {
        int u = n - 1;
        float tx = ny[u], ty = -nx[u];
        for (int i = 1; i <= ALEM; i++) {
            float d = PASSA[l.forca] * i / ALEM;
            px[n] = px[u] + tx * d;
            py[n] = py[u] + ty * d;
            nx[n] = nx[u];
            ny[n] = ny[u];
            lado[n] = lado[u];
            alfa[n] = alfa[u];
            n++;
        }
        return n;
    }

    /**
     * O ponto do caminho a {@code t} (0 a 1) do trecho que vai do ponto j ao j+1: a ponta em (fx,
     * fy) e a junta em (dx, dy), no mundo, e a opacidade pela idade.
     */
    private int amostra(Lado l, CharacterDefinition.Animation a, int[] ordem, int j, float t, int vida, int n) {
        float[] p0 = a.caminhoGolpe[ordem[Math.max(0, j - 1)]], p1 = a.caminhoGolpe[ordem[j]];
        float[] p2 = a.caminhoGolpe[ordem[j + 1]], p3 = a.caminhoGolpe[ordem[Math.min(l.cabeca, j + 2)]];
        px[n] = l.x + l.facing * curva(p0[0], p1[0], p2[0], p3[0], t);
        py[n] = l.y - curva(p0[1], p1[1], p2[1], p3[1], t);
        jx[n] = l.x + l.facing * curva(p0[2], p1[2], p2[2], p3[2], t);
        jy[n] = l.y - curva(p0[3], p1[3], p2[3], p3[3], t);
        float idade = l.quadro - (l.chegada[j] + (l.chegada[j + 1] - l.chegada[j]) * t);
        // Cheia na primeira metade da vida, depois some: o corte de ar fica na tela, não pisca.
        alfa[n] = Math.max(0f, Math.min(OPACIDADE, 2f * (1f - idade / vida)));
        return n + 1;
    }

    /**
     * A normal do caminho em cada ponto (nx, ny) e o quanto a junta fica para o lado dela (lado).
     * Devolve a média disso: grande quando o membro vai atravessado no caminho (o balanço), perto de
     * zero quando vai na direção dele (a estocada); o sinal diz de que lado o membro fica.
     */
    private float normais(int n) {
        float soma = 0f;
        for (int i = 0; i < n; i++) {
            int a = Math.max(0, i - 1), b = Math.min(n - 1, i + 1);
            float tx = px[b] - px[a], ty = py[b] - py[a];
            float comprimento = (float) Math.sqrt(tx * tx + ty * ty);
            if (comprimento < 0.01f) { tx = 1f; ty = 0f; comprimento = 1f; }
            nx[i] = -ty / comprimento;
            ny[i] = tx / comprimento;
            lado[i] = nx[i] * (jx[i] - px[i]) + ny[i] * (jy[i] - py[i]);
            soma += lado[i];
        }
        return soma / n;
    }

    /** As duas bordas da fita no ponto k: a nítida a {@code fora} px do caminho e a outra a {@code dentro}. */
    private void borda(int k, float fora, float dentro) {
        fx[k] = px[k] + nx[k] * fora;
        fy[k] = py[k] + ny[k] * fora;
        dx[k] = px[k] + nx[k] * dentro;
        dy[k] = py[k] + ny[k] * dentro;
    }

    /** Catmull-Rom: a curva suave que passa pelos pontos do caminho. */
    private static float curva(float a, float b, float c, float d, float t) {
        float t2 = t * t, t3 = t2 * t;
        return 0.5f * (2f * b + (c - a) * t + (2f * a - 5f * b + 4f * c - d) * t2 + (3f * b - a - 3f * c + d) * t3);
    }
}
