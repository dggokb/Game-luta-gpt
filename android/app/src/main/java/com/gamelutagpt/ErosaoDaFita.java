package com.gamelutagpt;

/**
 * A imagem do rastro do golpe num instante, pela receita de reports/VFX de luta estilo anime.md
 * (a parte "No Android, uma forma gerada e um shader pequeno"): uma forma em cinza, um ruído que
 * repete, erosão do alfa e rampa de cor do personagem, por cima de uma base escura. Do relatório
 * reports/Pipeline profissional de VFX anime.md: a frente da erosão é uma faixa chapada na cor do
 * personagem (a "erosão com borda luminosa") e a parte clara ganha um halo em duas cores (o Glow A
 * e B do After Effects), que é o que faz o efeito ler como luz. Do Ogawa (Arc System Works): depois
 * do acerto a cor desbota ("a segunda metade deve parecer que não tem área de acerto").
 *
 * <p>O relatório monta isso como shader. Aqui a mesma conta roda na CPU, numa imagem pequena
 * ({@link #LARGURA} x {@link #ALTURA}), e o desenho só estica a imagem na malha da fita: assim o
 * PC (Java2D, onde o dono julga no vídeo) e o celular (OpenGL) mostram os mesmos pixels, sem um
 * shader que daqui não dá para ver rodando. Java puro, sem Android.
 *
 * <p>Entram os três canais da textura {@code assets/efeitos/rastro_forma} (tools/sprites/
 * rastro_forma.py): R a forma (o véu com riscos finos gerado no Codex), G o ruído que come a forma
 * e B a área da base escura. Na imagem, x é o caminho inteiro, do começo (0) ao fim (1), e y
 * atravessa a fita (0 na borda de fora); o desenho mostra só o trecho entre o fundo e a frente.
 */
final class ErosaoDaFita {
    /** A imagem de cada instante (px): o caminho inteiro, do começo (x 0) ao fim; o jogo estica nele. */
    static final int LARGURA = 256, ALTURA = 56;
    /** Px do mundo que uma volta do ruído cobre ao longo do caminho. */
    private static final float PERIODO_RUIDO = 260f;
    /** O quanto o ruído torce a forma (fração da imagem, no comprimento e através). */
    private static final float TORCE_X = 0.02f, TORCE_Y = 0.035f;
    /** Meia largura da borda da erosão: perto de zero é o recorte chapado do anime. */
    private static final float BORDA = 0.035f;
    /**
     * A faixa chapada na cor do personagem logo atrás da frente da erosão (a borda luminosa), e o
     * limiar a partir do qual ela aparece (no começo da vida não há frente de erosão para marcar).
     */
    private static final float FAIXA_BORDA = 0.07f, BORDA_DE = 0.12f, BORDA_ATE = 0.35f;
    /** O halo em volta da parte clara: raio (px da imagem), força e o quanto a faixa conta a mais. */
    private static final int HALO_RAIO = 3;
    private static final float HALO_ALFA = 0.55f, HALO_FAIXA = 1.6f;
    /** O quanto a cor desbota no fim da vida (0: nada; 1: cinza). */
    private static final float DESBOTA_MAX = 0.8f;
    /** O brilho que a frente da fita ganha (a borda que corta, como a Arc System Works faz). */
    private static final float BRILHO_FRENTE = 0.35f;
    /** A base escura sob o corte: a opacidade. Ela some junto com o corte, nunca depois. */
    private static final float BASE_ALFA = 0.35f;
    /**
     * O começo do caminho que a erosão come a mais (fração do caminho, no máximo tantos px): o
     * rastro não nasce num corte reto. Proporcional, senão some inteiro no golpe curto.
     */
    private static final float NASCE = 0.15f, NASCE_MAX = 50f, NASCE_COME = 0.5f;
    /** O contraste do cinza antes da rampa (abaixo de 1: os riscos chegam ao branco). */
    private static final float GAMA = 0.6f;
    /** Os limites da rampa: abaixo do primeiro é sombra, entre os dois é a cor, acima é branco. */
    private static final float FAIXA_COR = 0.34f, FAIXA_BRANCO = 0.74f, FAIXA_MACIA = 0.03f;
    /** A saturação mínima da cor da rampa. */
    private static final float SAT_MIN = 0.3f;

    private final float[] forma, ruido, base;
    private final int tw, th;
    /** A rampa da cor atual: 256 tons, do escuro ao branco; a cor da base, da faixa e do halo (A, B). */
    private final int[] rampa = new int[256], haloCor = new int[64];
    private int rampaDe = -1, corBase, corFaixa, haloA, haloB;
    private float rampaDesbota = -1f;
    /** Rascunho de cada pixel: o alfa e a cor do corte, o alfa da base e a luz que vai para o halo. */
    private final float[] aCorte = new float[LARGURA * ALTURA], aBase = new float[LARGURA * ALTURA];
    private final float[] luz = new float[LARGURA * ALTURA], borrao = new float[LARGURA * ALTURA];
    private final int[] corCorte = new int[LARGURA * ALTURA];

    /** Os pixels da textura de forma (ARGB, os canais lidos como números de 0 a 1). */
    ErosaoDaFita(int[] argb, int largura, int altura) {
        tw = largura;
        th = altura;
        forma = new float[largura * altura];
        ruido = new float[largura * altura];
        base = new float[largura * altura];
        for (int i = 0; i < forma.length; i++) {
            int p = argb[i];
            forma[i] = ((p >> 16) & 255) / 255f;
            ruido[i] = ((p >> 8) & 255) / 255f;
            base[i] = (p & 255) / 255f;
        }
    }

    /**
     * Pinta em {@code saida} (ARGB sem pré-multiplicar, {@link #LARGURA} x {@link #ALTURA}) a fita
     * no caminho de {@code totalPx} px (do começo dele ao fim), na cor {@code rgb} (0xRRGGBB, a do
     * membro), com o fundo em {@code fundoPx} e a frente em {@code frentePx}. A forma tem o
     * comprimento do caminho e a ponta dela fica na frente: ela desliza com a frente (a janela que
     * avança, do Cyanilux) em vez de se espremer no trecho já andado. Antes do fundo e depois da
     * frente não há nada. {@code rolagemPx}: o quanto o ruído já correu pela fita. {@code erosao}
     * (0 a 1): o quanto a fita inteira já foi comida. {@code vies}: o quanto a mais o fundo é
     * comido (o fundo some primeiro, em bordas irregulares). {@code desbota} (0 a 1): o quanto a
     * cor já perdeu (depois do acerto).
     */
    void assar(int[] saida, int rgb, float totalPx, float fundoPx, float frentePx, float rolagemPx, float erosao, float vies,
               float desbota) {
        if (rgb != rampaDe || desbota != rampaDesbota) montarRampa(rgb, desbota);
        float formaDe = frentePx - totalPx, janela = Math.max(1f, frentePx - fundoPx);
        float nasce = Math.max(1f, Math.min(NASCE_MAX, NASCE * totalPx));
        // O que só depende da coluna sai do laço: se ela está no trecho, onde cai no ruído e na
        // forma, o limiar e o brilho da frente.
        for (int i = 0; i < LARGURA; i++) {
            float noCaminho = (i + 0.5f) / LARGURA * totalPx;
            colDentro[i] = noCaminho >= fundoPx && noCaminho <= frentePx;
            // O ruído vai preso ao caminho e corre por ele.
            float nu = (noCaminho + rolagemPx) / PERIODO_RUIDO;
            colRuido[i] = (nu - (float) Math.floor(nu)) * tw;
            float xf = (noCaminho - formaDe) / totalPx;
            colForma[i] = xf * tw;
            // Erosão: o limiar sobe com o tempo, no fundo do trecho e no começo do caminho.
            float x = (noCaminho - fundoPx) / janela;
            colLimiar[i] = limita(erosao + (1f - x) * vies + NASCE_COME * Math.max(0f, 1f - noCaminho / nasce));
            colBrilho[i] = BRILHO_FRENTE * suave(0.72f, 1f, xf);
        }
        float torceX = TORCE_X * tw, torceY = TORCE_Y * th;
        for (int j = 0, k = 0; j < ALTURA; j++) {
            float vy = (j + 0.5f) / ALTURA * th;
            for (int i = 0; i < LARGURA; i++, k++) {
                // Fora do trecho, ou fora da base (que cobre a forma, alargada), não há nada.
                float b = colDentro[i] ? trava(base, colForma[i], vy) : 0f;
                if (b < 0.004f) {
                    aCorte[k] = aBase[k] = luz[k] = 0f;
                    continue;
                }
                float n = repete(ruido, colRuido[i], vy);
                float s = trava(forma, colForma[i] + (n - 0.5f) * torceX, vy + (n - 0.5f) * torceY);
                float limiar = colLimiar[i];
                float corte = -BORDA + limiar * (1f + 2f * BORDA);
                float claro = GAMA_TABELA[(int) (s * 1023f + 0.5f)];
                // Forma vezes ruído contra o limiar.
                float campo = claro * (0.45f + 0.55f * n);
                float fica = suave(corte - BORDA, corte + BORDA, campo);
                // A borda luminosa: o que acabou de escapar da erosão, numa faixa chapada.
                float faixa = fica * (1f - suave(corte + BORDA, corte + BORDA + FAIXA_BORDA, campo))
                    * suave(BORDA_DE, BORDA_ATE, limiar);
                float g = limita(claro + colBrilho[i] * claro);
                int cor = rampa[(int) (g * 255f + 0.5f)];
                if (faixa > 0.01f) cor = mistura(cor, corFaixa, suave(0.35f, 0.65f, faixa));
                // O véu fica meio transparente; os riscos e a faixa, cheios.
                float a = fica * Math.min(1f, Math.max(s * 2.2f, faixa));
                aCorte[k] = a;
                corCorte[k] = cor;
                // A luz que vai para o halo: a parte clara e, mais forte, a faixa.
                luz[k] = a * Math.min(1f, Math.max(g - 0.3f, 0f) * 1.4f + faixa * HALO_FAIXA);
                // A base: a mesma erosão do corte, mais macia, e apagando com ela (não sobra sozinha).
                float resto = 1f - limiar;
                aBase[k] = BASE_ALFA * b * suave(corte - 0.08f, corte + 0.08f, b * (0.45f + 0.55f * n) * 0.6f) * resto * resto;
            }
        }
        borrar(luz, borrao, HALO_RAIO);
        for (int k = 0; k < saida.length; k++) {
            float h = Math.min(1f, borrao[k] * 2f);
            float aHalo = HALO_ALFA * h;
            if (aCorte[k] <= 0f && aBase[k] <= 0f && aHalo <= 0.004f) {
                saida[k] = 0;
                continue;
            }
            // Do fundo para cima: a base escura, o halo (B por fora, A perto da luz) e o corte.
            saida[k] = sobre(corCorte[k], aCorte[k], haloCor[(int) (h * 63f)], aHalo, corBase, aBase[k]);
        }
    }

    /**
     * Desfoque de caixa em x e em y, duas vezes (fica perto de um desfoque gaussiano): {@code m}
     * vai borrado para {@code saida}. Usa {@code m} de rascunho.
     */
    private static void borrar(float[] m, float[] saida, int r) {
        for (int vez = 0; vez < 2; vez++) {
            caixa(m, saida, r, LARGURA, ALTURA, 1, LARGURA);
            caixa(saida, m, r, ALTURA, LARGURA, LARGURA, 1);
        }
        System.arraycopy(m, 0, saida, 0, m.length);
    }

    /** Média móvel de 2r+1 ao longo de {@code n} pontos (passo {@code d}), em {@code linhas} linhas (passo {@code dl}). */
    private static void caixa(float[] de, float[] para, int r, int n, int linhas, int d, int dl) {
        float k = 1f / (2 * r + 1);
        for (int l = 0; l < linhas; l++) {
            int o = l * dl;
            float soma = 0f;
            for (int i = -r; i <= r; i++) soma += de[o + Math.max(0, Math.min(n - 1, i)) * d];
            for (int i = 0; i < n; i++) {
                para[o + i * d] = soma * k;
                soma += de[o + Math.min(n - 1, i + r + 1) * d] - de[o + Math.max(0, i - r) * d];
            }
        }
    }

    private final boolean[] colDentro = new boolean[LARGURA];
    private final float[] colRuido = new float[LARGURA], colForma = new float[LARGURA];
    private final float[] colLimiar = new float[LARGURA], colBrilho = new float[LARGURA];
    /** s elevado a {@link #GAMA}, de 0 a 1 em 1024 passos ({@code Math.pow} por pixel custava caro). */
    private static final float[] GAMA_TABELA = new float[1024];
    static {
        for (int i = 0; i < GAMA_TABELA.length; i++) GAMA_TABELA[i] = (float) Math.pow(i / 1023.0, GAMA);
    }

    /**
     * A rampa do personagem a partir da cor do membro: sombra (a cor escurecida), a cor e branco,
     * em faixas chapadas; e a cor da faixa da erosão e do halo (A perto da luz, B por fora). A cor
     * medida costuma vir quase cinza (pele, roupa clara): fica o tom dela, com saturação de pelo
     * menos {@link #SAT_MIN}, senão o véu sai branco e os riscos brancos somem nele. {@code desbota}
     * tira a saturação (depois do acerto).
     */
    private void montarRampa(int rgb, float desbota) {
        rampaDe = rgb;
        rampaDesbota = desbota;
        float[] hsv = hsv(rgb);
        float k = 1f - DESBOTA_MAX * desbota;
        float sat = Math.max(SAT_MIN, Math.min(0.8f, hsv[1] * 2.5f));
        int cor = rgb(hsv[0], sat * k, 0.92f);
        int sombra = rgb(hsv[0], Math.min(1f, sat + 0.15f) * k, 0.58f);
        corBase = rgb(hsv[0], Math.min(1f, sat + 0.2f) * k, 0.16f);
        corFaixa = rgb(hsv[0], Math.min(1f, sat + 0.35f) * k, 1f);
        haloA = rgb(hsv[0], Math.min(1f, sat + 0.1f) * k, 1f);
        haloB = rgb(hsv[0] + 0.04f, Math.min(1f, sat + 0.45f) * k, 0.85f);
        for (int i = 0; i < haloCor.length; i++) haloCor[i] = mistura(haloB, haloA, i / (haloCor.length - 1f));
        for (int i = 0; i < 256; i++) {
            float g = i / 255f;
            int c = mistura(sombra, cor, suave(FAIXA_COR - FAIXA_MACIA, FAIXA_COR + FAIXA_MACIA, g));
            rampa[i] = mistura(c, 0xFFFFFF, suave(FAIXA_BRANCO - FAIXA_MACIA, FAIXA_BRANCO + FAIXA_MACIA, g));
        }
    }

    /** Três camadas, de cima para baixo (cor, alfa): ARGB sem pré-multiplicar. */
    private static int sobre(int c2, float a2, int c1, float a1, int c0, float a0) {
        // Pré-multiplicado, de baixo para cima.
        float r = ((c0 >> 16) & 255) * a0, g = ((c0 >> 8) & 255) * a0, b = (c0 & 255) * a0, a = a0;
        r = ((c1 >> 16) & 255) * a1 + r * (1f - a1);
        g = ((c1 >> 8) & 255) * a1 + g * (1f - a1);
        b = (c1 & 255) * a1 + b * (1f - a1);
        a = a1 + a * (1f - a1);
        r = ((c2 >> 16) & 255) * a2 + r * (1f - a2);
        g = ((c2 >> 8) & 255) * a2 + g * (1f - a2);
        b = (c2 & 255) * a2 + b * (1f - a2);
        a = a2 + a * (1f - a2);
        if (a <= 0.002f) return 0;
        float d = 1f / a;
        return (Math.min(255, (int) (a * 255f + 0.5f)) << 24) | (Math.min(255, (int) (r * d + 0.5f)) << 16)
            | (Math.min(255, (int) (g * d + 0.5f)) << 8) | Math.min(255, (int) (b * d + 0.5f));
    }

    /** Amostra suave travada nas bordas (a forma não se repete). */
    private float trava(float[] m, float x, float y) {
        x = Math.max(0f, Math.min(tw - 1f, x - 0.5f));
        y = Math.max(0f, Math.min(th - 1f, y - 0.5f));
        int x0 = (int) x, y0 = (int) y;
        int x1 = Math.min(tw - 1, x0 + 1), y1 = Math.min(th - 1, y0 + 1);
        return bilinear(m, x0, y0, x1, y1, x - x0, y - y0);
    }

    /** Amostra suave que dá a volta no comprimento (o ruído repete) e trava através. */
    private float repete(float[] m, float x, float y) {
        x -= 0.5f;
        float fx = x - (float) Math.floor(x);
        int x0 = Math.floorMod((int) Math.floor(x), tw), x1 = (x0 + 1) % tw;
        y = Math.max(0f, Math.min(th - 1f, y - 0.5f));
        int y0 = (int) y, y1 = Math.min(th - 1, y0 + 1);
        return bilinear(m, x0, y0, x1, y1, fx, y - y0);
    }

    private float bilinear(float[] m, int x0, int y0, int x1, int y1, float fx, float fy) {
        float a = m[y0 * tw + x0], b = m[y0 * tw + x1], c = m[y1 * tw + x0], d = m[y1 * tw + x1];
        return (a + (b - a) * fx) * (1f - fy) + (c + (d - c) * fx) * fy;
    }

    private static float limita(float v) {
        return v < 0f ? 0f : v > 1f ? 1f : v;
    }

    private static float suave(float a, float b, float v) {
        float t = limita((v - a) / (b - a));
        return t * t * (3f - 2f * t);
    }

    private static int mistura(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static float[] hsv(int rgb) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float h = 0f;
        if (d > 1e-5f) {
            if (max == r) h = ((g - b) / d) % 6f;
            else if (max == g) h = (b - r) / d + 2f;
            else h = (r - g) / d + 4f;
            h /= 6f;
            if (h < 0f) h += 1f;
        }
        return new float[] {h, max <= 0f ? 0f : d / max, max};
    }

    private static int rgb(float h, float s, float v) {
        float h6 = (h - (float) Math.floor(h)) * 6f;
        int i = (int) h6;
        float f = h6 - i, p = v * (1f - s), q = v * (1f - s * f), t = v * (1f - s * (1f - f));
        float r, g, b;
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return (Math.round(r * 255f) << 16) | (Math.round(g * 255f) << 8) | Math.round(b * 255f);
    }
}
