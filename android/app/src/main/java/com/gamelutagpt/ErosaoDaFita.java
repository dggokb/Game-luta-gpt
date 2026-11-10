package com.gamelutagpt;

/**
 * A imagem do rastro do golpe num instante, pela receita de reports/VFX de luta estilo anime.md
 * (a parte "No Android, uma forma gerada e um shader pequeno"): uma forma em cinza, um ruído que
 * repete, erosão do alfa e rampa de cor do personagem, por cima de uma base escura.
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
    /** A faixa clara logo atrás da frente da erosão (o "fio" que está se desfazendo). */
    private static final float FIO = 0.03f;
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
    /** A rampa da cor atual: 256 tons, do escuro ao branco, e a cor da base. */
    private final int[] rampa = new int[256];
    private int rampaDe = -1, corBase;

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
     * comido (o fundo some primeiro, em bordas irregulares).
     */
    void assar(int[] saida, int rgb, float totalPx, float fundoPx, float frentePx, float rolagemPx, float erosao, float vies) {
        if (rgb != rampaDe) montarRampa(rgb);
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
                    saida[k] = 0;
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
                float fio = limiar > 0.02f ? fica * (1f - suave(corte + BORDA, corte + BORDA + FIO, campo)) : 0f;
                float g = limita(claro + 0.6f * fio + colBrilho[i] * claro);
                // O véu fica meio transparente; os riscos, cheios.
                float aCorte = fica * Math.min(1f, s * 2.2f);
                // A base: a mesma erosão do corte, mais macia, e apagando com ela (não sobra sozinha).
                float resto = 1f - limiar;
                float aBase = BASE_ALFA * b * suave(corte - 0.08f, corte + 0.08f, b * (0.45f + 0.55f * n) * 0.6f) * resto * resto;
                saida[k] = sobre(rampa[(int) (g * 255f + 0.5f)], aCorte, corBase, aBase);
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
     * em faixas chapadas. A cor medida costuma vir quase cinza (pele, roupa clara): fica o tom
     * dela, com saturação de pelo menos {@link #SAT_MIN}, senão o véu sai branco e os riscos
     * brancos somem nele.
     */
    private void montarRampa(int rgb) {
        rampaDe = rgb;
        float[] hsv = hsv(rgb);
        float sat = Math.max(SAT_MIN, Math.min(0.8f, hsv[1] * 2.5f));
        int cor = rgb(hsv[0], sat, 0.92f);
        int sombra = rgb(hsv[0], Math.min(1f, sat + 0.15f), 0.58f);
        corBase = rgb(hsv[0], Math.min(1f, sat + 0.2f), 0.16f);
        for (int k = 0; k < 256; k++) {
            float g = k / 255f;
            int c = mistura(sombra, cor, suave(FAIXA_COR - FAIXA_MACIA, FAIXA_COR + FAIXA_MACIA, g));
            rampa[k] = mistura(c, 0xFFFFFF, suave(FAIXA_BRANCO - FAIXA_MACIA, FAIXA_BRANCO + FAIXA_MACIA, g));
        }
    }

    /** O corte (cor c1, alfa a1) por cima da base (c0, a0): ARGB sem pré-multiplicar. */
    private static int sobre(int c1, float a1, int c0, float a0) {
        float a = a1 + a0 * (1f - a1);
        if (a <= 0.002f) return 0;
        float k1 = a1 / a, k0 = a0 * (1f - a1) / a;
        int r = (int) (((c1 >> 16) & 255) * k1 + ((c0 >> 16) & 255) * k0 + 0.5f);
        int g = (int) (((c1 >> 8) & 255) * k1 + ((c0 >> 8) & 255) * k0 + 0.5f);
        int b = (int) ((c1 & 255) * k1 + (c0 & 255) * k0 + 0.5f);
        return (Math.min(255, (int) (a * 255f + 0.5f)) << 24) | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
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
