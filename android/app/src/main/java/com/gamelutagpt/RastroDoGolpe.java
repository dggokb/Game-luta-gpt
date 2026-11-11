package com.gamelutagpt;

import android.graphics.Bitmap;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;

/**
 * O rastro do golpe normal: uma fita de ar que segue a LINHA DE AÇÃO do golpe, o caminho que a
 * ponta do braço ou da perna fez de verdade, de onde saiu até onde chegou (a garra que desce em
 * arco, o chute que sai do chão, o soco que sai da guarda). O caminho é detectado na animação
 * por um modelo de pose (tools/sprites/linha_de_acao.py) e vem em
 * {@link CharacterDefinition.Animation#caminhoGolpe}: a ponta e a junta de trás (o cotovelo, o
 * joelho) em cada desenho do golpe.
 *
 * A fita é PROCEDURAL: não aparece montada. Ela nasce no ponto de onde o membro sai, a ponta da
 * frente vai andando pelo caminho, de forma contínua, até passar um pouco de onde o membro
 * estica, e a ponta de trás vem atrás dela com {@link #ATRASO} quadros de diferença, apagando o
 * fundo. Quando a frente para, o fundo continua vindo até a fita sumir. (A tela mostra os
 * desenhos do golpe aos saltos; a fita não: ela anda por quadro de jogo.)
 *
 * Ela é atravessada no caminho, do lado do membro (larga no balanço, que varre uma área; um feixe
 * em volta do membro na estocada). O desenho segue a receita de reports/VFX de luta estilo
 * anime.md ({@link ErosaoDaFita}): uma forma em cinza gerada no Codex (o véu com riscos finos), um
 * ruído que corre ao longo da fita e come a forma por erosão (o fundo some primeiro, em bordas
 * irregulares), a rampa de cor do personagem (sombra, a cor do membro, branco) e uma base escura
 * sob o corte, como a Arc System Works faz. O tempo da erosão e do ruído anda em passos de
 * {@link #PASSO} quadros (animação limitada); a frente e o fundo andam por quadro, como antes.
 * Ela é exagerada de propósito: nas
 * referências (Marvel Tōkon, docs/efeitos-referencia-tokon.md) o corte de ar é do tamanho do
 * membro ou maior. Na pausa do acerto o golpe não anda e a fita fica parada na tela, como o
 * membro. Só apresentação; golpes sem caminho (os com efeito já desenhado na arte, os especiais)
 * ficam sem.
 */
final class RastroDoGolpe {
    /**
     * Desligado pelo dono (11/11): os rastros passam a ser desenhados à mão na arte de cada golpe
     * (o primeiro é o chute forte do Ryūga, p01). O código e a textura ficam; para voltar, true.
     */
    static final boolean LIGADO = false;
    private static final String FORMA = "rastro_forma";
    /** Quadros de jogo por passo da erosão e do ruído (5: 12 desenhos por segundo, o degrau do anime). */
    private static final int PASSO = 5;
    /**
     * Depois que a frente termina de varrer (os quadros ativos do golpe) a cor desbota até o fim
     * da vida (Ogawa, Arc System Works: a segunda metade não parece ter área de acerto).
     */
    /** Px do mundo que o ruído corre pela fita a cada quadro (para trás, contra o golpe). */
    private static final float ROLAGEM = 7f;
    /** A erosão da fita inteira: parada até esta fração da vida, depois sobe até {@link #EROSAO_MAX}. */
    private static final float EROSAO_INICIO = 0.3f, EROSAO_MAX = 0.55f;
    /** O quanto a mais o fundo é comido que a frente. */
    private static final float VIES_FUNDO = 0.4f;
    /** Pontos do caminho (os desenhos do golpe até o membro esticado). */
    private static final int PONTOS = 8;
    /** Pedaços da fita entre dois pontos do caminho. */
    private static final int PASSOS = 8;
    /** Quadros que a ponta de trás vem atrás da ponta da frente (fraco, médio, forte). */
    private static final int[] ATRASO = {5, 7, 9};
    /** Quadros que a ponta da frente leva do começo ao fim do caminho, no mínimo e no máximo. */
    private static final int[] VARRIDA_MIN = {3, 4, 5};
    private static final int VARRIDA_MAX = 9;
    /** O quanto do membro atravessado no caminho a fita cobre, e os limites em px (fraco, médio, forte). */
    private static final float[] LARGURA = {1.1f, 1.5f, 1.9f};
    private static final float[] LARGURA_MAX = {80f, 120f, 160f};
    private static final float LARGURA_MIN = 44f;
    /** A opacidade da fita (0 a 255): ela passa na frente dos lutadores e o membro ainda se vê por baixo. */
    private static final int OPACIDADE = Math.round(255 * 0.75f);
    /** Amostras a mais na frente da ponta: o rastro passa um pouco dela e termina em pontas. */
    private static final int ALEM = 4;
    private static final int AMOSTRAS = PONTOS * PASSOS + 1 + ALEM;
    /** O quanto da largura fica para fora da ponta no balanço (o punho e o pé têm grossura). */
    private static final float FORA = 0.22f;
    /** Menos que isto (px) de membro atravessado no caminho, em média, é estocada. */
    private static final float ESTOCADA = 24f;
    /** Na estocada: a largura do feixe de riscos em volta do membro (fraco, médio, forte). */
    private static final float[] FEIXE = {64f, 90f, 120f};
    /** O quanto o rastro passa da ponta do membro (px; fraco, médio, forte). */
    private static final float[] PASSA = {26f, 40f, 56f};

    private final EfeitosVideo efeitos;
    private final Lado[] lados = {new Lado(), new Lado()};
    /** O caminho amostrado: a ponta, a junta, a normal, o quanto a junta fica para o lado dela e o comprimento até ali. */
    private final float[] px = new float[AMOSTRAS], py = new float[AMOSTRAS], jx = new float[AMOSTRAS], jy = new float[AMOSTRAS];
    private final float[] nx = new float[AMOSTRAS], ny = new float[AMOSTRAS], lado = new float[AMOSTRAS], andado = new float[AMOSTRAS];
    /** A fita: uma borda (fx, fy), a outra (dx, dy) e onde cada ponto cai no comprimento da imagem. */
    private final float[] fx = new float[AMOSTRAS], fy = new float[AMOSTRAS], dx = new float[AMOSTRAS], dy = new float[AMOSTRAS];
    private final float[] u = new float[AMOSTRAS];
    /** A conta da imagem da fita (null até a textura de forma carregar) e o rascunho dela. */
    private ErosaoDaFita erosao;
    private boolean semForma;
    private final int[] pixels = new int[ErosaoDaFita.LARGURA * ErosaoDaFita.ALTURA];
    /** Rascunho de {@link #comecar}: o quadro do golpe em que a tela mostra cada ponto do caminho. */
    private final int[] chegada = new int[PONTOS];
    /** A fração do caminho que a ponta já andou quando o membro conta como esticado. */
    private static final float CHEGOU = 0.85f;

    /** O rastro do golpe em curso de um lutador. */
    private static final class Lado {
        AttackDefinition ataque;
        CharacterDefinition.Animation desenho;
        /** Quantos pontos do caminho o golpe mostra, e o último quadro do golpe visto. */
        int pontos, quadro;
        /** O quadro do golpe em que a ponta da frente sai e quantos quadros ela leva até o fim. */
        int saida, varrida;
        float x, y;
        int facing, forca;
        /** A imagem da fita deste lutador e o quadro do golpe que ela mostra (-1: nenhum). */
        Bitmap bitmap;
        RenderImage imagem;
        int assado = -1;
    }

    RastroDoGolpe(EfeitosVideo efeitos) {
        this.efeitos = efeitos;
    }

    void limpar() {
        for (Lado l : lados) l.ataque = null;
    }

    /** Um passo da luta: {@code x} é onde o lutador {@code i} está desenhado (com a troca de dupla). */
    void passo(int i, CombatFighter f, float x) {
        if (!LIGADO) return;
        Lado l = lados[i];
        CharacterDefinition.Animation a = f.attacking() && f.attack != null ? CombatEngine.desenhoDoGolpe(f) : null;
        if (a == null || a.caminhoGolpe == null) {
            // O golpe acabou (ou outro sem caminho começou): a fita termina a vida dela com relógio
            // próprio, parada onde estava, e o fundo a apaga de trás para a frente. Antes, ela
            // sumia de uma vez no fim do golpe fraco.
            if (l.ataque != null && (l.quadro - l.saida + 1 - ATRASO[l.forca]) < l.varrida) l.quadro++;
            else l.ataque = null;
            return;
        }
        if (f.attack != l.ataque || a != l.desenho || f.attackFrame < l.quadro) {
            comecar(l, f, a);
            l.assado = -1;
        }
        l.quadro = f.attackFrame;
        l.x = x;
        l.y = f.y;
        l.facing = f.facing;
    }

    /**
     * Golpe novo: quando a ponta da frente sai e quanto leva. A tela mostra o primeiro desenho do
     * caminho no quadro {@code t0} do golpe e o último (o membro esticado) no {@code tN}; a ponta
     * anda o caminho inteiro de forma contínua e chega ao fim um quadro depois do membro esticar.
     */
    private void comecar(Lado l, CombatFighter f, CharacterDefinition.Animation a) {
        l.ataque = f.attack;
        l.desenho = a;
        AttackDefinition.Strength s = f.attack.strength;
        l.forca = s == AttackDefinition.Strength.LIGHT ? 0 : s == AttackDefinition.Strength.MEDIUM ? 1 : 2;
        int[] ordem = a.caminhoOrdem;
        // Em que quadro do golpe a tela mostra cada ponto do caminho (o clipe pula desenhos).
        l.pontos = 0;
        for (int quadro = 0; quadro < f.attack.totalFrames && l.pontos < ordem.length && l.pontos < PONTOS; quadro++) {
            int agora = a.ordemDe(CombatEngine.quadroDoDesenho(f, quadro));
            while (l.pontos < ordem.length && l.pontos < PONTOS && a.ordemDe(ordem[l.pontos]) <= agora) chegada[l.pontos++] = quadro;
        }
        if (l.pontos < 2) {
            l.ataque = null;   // o golpe não chega a mostrar o caminho
            return;
        }
        // O membro "chegou" quando a ponta já andou quase o caminho todo: os últimos pontos costumam
        // ser o membro esticado se acomodando, e a fita não espera por eles.
        float total = 0f;
        for (int j = 1; j < l.pontos; j++) total += passo(a, ordem, j);
        int chegou = l.pontos - 1;
        float andou = 0f;
        for (int j = 1; j < l.pontos; j++) {
            andou += passo(a, ordem, j);
            if (andou >= total * CHEGOU) { chegou = j; break; }
        }
        int t0 = chegada[0], tN = chegada[chegou];
        l.varrida = Math.max(VARRIDA_MIN[l.forca], Math.min(VARRIDA_MAX, tN - t0 + 1));
        l.saida = tN + 1 - l.varrida;
    }

    /** A distância (px) entre o ponto j-1 e o ponto j do caminho. */
    private static float passo(CharacterDefinition.Animation a, int[] ordem, int j) {
        float[] p = a.caminhoGolpe[ordem[j - 1]], q = a.caminhoGolpe[ordem[j]];
        return (float) Math.hypot(q[0] - p[0], q[1] - p[1]);
    }

    /** Desenha no espaço do mundo (o canvas já com a câmera), na frente dos lutadores. */
    void desenhar(RenderCanvas rc) {
        if (erosao == null && !carregarForma()) return;
        for (Lado l : lados) {
            if (l.ataque == null) continue;
            // Onde estão a ponta da frente e a de trás, de 0 a 1 do caminho.
            float frente = Math.min(1f, (l.quadro - l.saida + 1) / (float) l.varrida);
            float fundo = Math.max(0f, (l.quadro - l.saida + 1 - ATRASO[l.forca]) / (float) l.varrida);
            if (frente <= 0f || fundo >= 1f) continue;
            CharacterDefinition.Animation a = l.desenho;
            int[] ordem = a.caminhoOrdem;
            int n = 0;
            for (int j = 0; j < l.pontos - 1; j++) {
                for (int k = 0; k < PASSOS; k++) n = amostra(l, a, ordem, j, k / (float) PASSOS, n);
            }
            n = amostra(l, a, ordem, l.pontos - 2, 1f, n);
            float atravessado = normais(n);
            n = alem(l, n);
            andado[0] = 0f;
            for (int k = 1; k < n; k++) {
                float vx = px[k] - px[k - 1], vy = py[k] - py[k - 1];
                andado[k] = andado[k - 1] + (float) Math.sqrt(vx * vx + vy * vy);
            }
            float total = andado[n - 1];
            if (total < 1f) continue;
            // Só o trecho entre as duas pontas: as amostras de dentro e uma em cada ponta exata.
            float de = fundo * total, ate = frente * total;
            int primeira = 0, ultima = n - 1;
            while (primeira < n - 1 && andado[primeira + 1] <= de) primeira++;
            while (ultima > 0 && andado[ultima - 1] >= ate) ultima--;
            if (ultima <= primeira) continue;
            mover(primeira, primeira + 1, de);
            mover(ultima, ultima - 1, ate);
            // A imagem cobre o caminho inteiro; a malha mostra só o trecho entre as pontas.
            for (int k = primeira; k <= ultima; k++) u[k] = andado[k] / total;
            if (Math.abs(atravessado) >= ESTOCADA) {
                // O balanço (o chute em arco, a garra): o membro vai atravessado no caminho e a fita
                // cobre a área que ele varreu, do lado dele, um pouco para fora da ponta.
                float sinal = atravessado < 0f ? -1f : 1f;
                for (int k = primeira; k <= ultima; k++) {
                    float largura = Math.max(LARGURA_MIN, Math.min(LARGURA_MAX[l.forca], Math.abs(lado[k]) * LARGURA[l.forca]));
                    borda(k, -sinal * largura * FORA, sinal * largura * (1f - FORA));
                }
            } else {
                // A estocada (o soco reto, o chute de frente): o membro vai na direção do caminho. Um
                // feixe de riscos em volta dele, com o caminho no meio.
                float meia = FEIXE[l.forca] * 0.5f;
                for (int k = primeira; k <= ultima; k++) borda(k, -meia, meia);
            }
            if (assar(l, a.caminhoCor, de, ate, total)) rc.imagemMudou(l.imagem);
            efeitos.fita(rc, l.imagem, ErosaoDaFita.LARGURA, ErosaoDaFita.ALTURA, fx, fy, dx, dy, u, primeira, ultima + 1, OPACIDADE);
        }
    }

    /** Lê a textura de forma (uma vez); false se ela não existe (o rastro não aparece). */
    private boolean carregarForma() {
        if (semForma) return false;
        int[] tamanho = new int[2];
        int[] px = efeitos.pixels(FORMA, tamanho);
        if (px == null) {
            semForma = efeitos.carregado();
            return false;
        }
        erosao = new ErosaoDaFita(px, tamanho[0], tamanho[1]);
        return true;
    }

    /**
     * Refaz a imagem da fita do lutador para o quadro atual do golpe ({@code de} a {@code ate}: o
     * trecho do caminho, em px, entre o fundo e a frente; {@code total}: o caminho inteiro). A
     * frente e o fundo andam por quadro; a erosão e o ruído, em passos de {@link #PASSO}. Na pausa
     * do acerto o quadro não anda e a imagem fica a mesma. True se a imagem mudou.
     */
    private boolean assar(Lado l, int cor, float de, float ate, float total) {
        if (l.assado == l.quadro && l.bitmap != null) return false;
        if (l.bitmap == null) {
            l.bitmap = Bitmap.createBitmap(ErosaoDaFita.LARGURA, ErosaoDaFita.ALTURA, Bitmap.Config.ARGB_8888);
            l.imagem = new AndroidRenderAssets.AndroidRenderImage(l.bitmap);
        }
        int idade = Math.max(0, l.quadro - l.saida + 1);
        int passo = idade / PASSO * PASSO;
        float vida = l.varrida + ATRASO[l.forca];
        float t = passo / vida;
        float comida = t <= EROSAO_INICIO ? 0f : Math.min(1f, (t - EROSAO_INICIO) / (1f - EROSAO_INICIO)) * EROSAO_MAX;
        float desbota = Math.max(0f, Math.min(1f, (passo - l.varrida) / (float) ATRASO[l.forca]));
        erosao.assar(pixels, cor, total, de, ate, -ROLAGEM * passo, comida, VIES_FUNDO, desbota);
        l.bitmap.setPixels(pixels, 0, ErosaoDaFita.LARGURA, 0, 0, ErosaoDaFita.LARGURA, ErosaoDaFita.ALTURA);
        l.assado = l.quadro;
        return true;
    }

    /** Leva a amostra {@code k} para o ponto a {@code onde} px do começo, entre ela e a vizinha {@code v}. */
    private void mover(int k, int v, float onde) {
        float vao = andado[v] - andado[k];
        if (Math.abs(vao) < 0.001f) return;
        float t = Math.max(0f, Math.min(1f, (onde - andado[k]) / vao));
        px[k] += (px[v] - px[k]) * t;
        py[k] += (py[v] - py[k]) * t;
        lado[k] += (lado[v] - lado[k]) * t;
        andado[k] = onde;
    }

    /**
     * O ponto do caminho a {@code t} (0 a 1) do trecho que vai do ponto j ao j+1: a ponta em (px,
     * py) e a junta em (jx, jy), no mundo.
     */
    private int amostra(Lado l, CharacterDefinition.Animation a, int[] ordem, int j, float t, int n) {
        float[] p0 = a.caminhoGolpe[ordem[Math.max(0, j - 1)]], p1 = a.caminhoGolpe[ordem[j]];
        float[] p2 = a.caminhoGolpe[ordem[j + 1]], p3 = a.caminhoGolpe[ordem[Math.min(l.pontos - 1, j + 2)]];
        px[n] = l.x + l.facing * curva(p0[0], p1[0], p2[0], p3[0], t);
        py[n] = l.y - curva(p0[1], p1[1], p2[1], p3[1], t);
        jx[n] = l.x + l.facing * curva(p0[2], p1[2], p2[2], p3[2], t);
        jy[n] = l.y - curva(p0[3], p1[3], p2[3], p3[3], t);
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
            n++;
        }
        return n;
    }

    /** As duas bordas da fita no ponto k: uma a {@code fora} px do caminho e a outra a {@code dentro}. */
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
