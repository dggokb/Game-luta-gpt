package com.gamelutagpt;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Efeitos vindos de vídeo: cada um é uma folha em {@code assets/efeitos/<nome>/} (folha.webp
 * e efeito.json, feitos pelo tools/sprites/efeito_video.py a partir de um vídeo do efeito
 * sozinho no verde). Tocar um efeito põe a origem dele num ponto do mundo; ele anda no relógio
 * de 60 Hz e some no último quadro. Só apresentação.
 *
 * Um efeito que não existe nos assets não toca ({@link #tocar} devolve false): quem chama usa
 * o efeito clássico no lugar. Assim cada vídeo novo entra sozinho, sem mexer no código. As
 * folhas são lidas uma vez ({@link #carregar}, junto com a arte da luta).
 */
final class EfeitosVideo {
    private static final String LOG_TAG = "EfeitosVideo";
    private static final String PASTA = "efeitos";
    private static final int VAGAS = 32;
    private static final float MS_POR_PASSO = 1000f / 60f;

    /** Uma folha carregada: os quadros (x, y, l, a, dx, dy em pixels da folha) e o tempo. */
    static final class Folha {
        final String nome;
        final Bitmap bitmap;
        final float pixelScale;
        final int[][] quadros;
        final float[][] desloc;
        /** Fim de cada quadro (ms desde o começo). */
        final float[] fim;
        /** Quando o vídeo para para algo do jogo entrar por cima (ms; -1: não tem). */
        float marcaMs = -1f;
        /** A folha é uma cena com o movimento dentro dela (não é levada pela tela). */
        boolean cena;
        /** Largura do quadro do vídeo da cena no mundo (px; 0: não tem). */
        float larguraCena;
        /**
         * Quanto a folha soma com o que está embaixo, de 0 (cobre, o normal) a 1 (luz pura): o
         * {@code "soma"} do efeito.json. Os efeitos de luz (impacto, raio, fogo) somam; fumaça não.
         */
        float soma;
        /**
         * A luz que o efeito emite ({@code "luz": {"cor", "alcance", "forca"}} do efeito.json): cor
         * 0xRRGGBB, alcance em alturas de tela e força de 0 a 1. Sem {@code "luz"} o efeito não
         * ilumina ({@link #luzForca} 0). É um ponto de luz que nasce e apaga com o efeito
         * (GameView.luzDoLutador e a poça de luz no chão).
         */
        int luzCor;
        float luzAlcance, luzForca;

        Folha(String nome, Bitmap bitmap, float pixelScale, int[][] quadros, float[][] desloc, float[] fim) {
            this.nome = nome;
            this.bitmap = bitmap;
            this.pixelScale = pixelScale;
            this.quadros = quadros;
            this.desloc = desloc;
            this.fim = fim;
        }

        float duracaoMs() { return fim.length == 0 ? 0f : fim[fim.length - 1]; }

        int quadroEm(float ms) {
            for (int i = 0; i < fim.length; i++) if (ms < fim[i]) return i;
            return -1;
        }
    }

    private final AssetManager assets;
    /** Trocado inteiro ao carregar (a carga pode vir de outra thread que a do desenho). */
    private volatile Map<String, Folha> folhas = new HashMap<>();
    private boolean carregou;

    private final Folha[] tocando = new Folha[VAGAS];
    private final float[] x = new float[VAGAS], y = new float[VAGAS], escala = new float[VAGAS], idadeMs = new float[VAGAS];
    /** Velocidade de cada efeito (1 = a do vídeo; {@link #tocarDurando}). */
    private final float[] ritmo = new float[VAGAS];
    /** A vaga do último efeito tocado. */
    private int ultima = -1;
    /** Efeito que anda (px do mundo por passo) e repete os quadros até {@link #fimMs} (-1: toca uma vez). */
    private final float[] vx = new float[VAGAS], fimMs = new float[VAGAS];
    private final boolean[] espelhado = new boolean[VAGAS];
    /** O efeito vai atrás dos lutadores (a fumaça do pulo), não na frente. */
    private final boolean[] atras = new boolean[VAGAS];
    private final Map<Integer, android.graphics.ColorFilter> tintas = new HashMap<>();
    /** O comprimento do caminho até cada ponto da fita (rascunho de {@link #fita}). */
    private float[] percorrido = new float[80];
    /** O vermelho do corpo de um efeito que se pinta (TINTA_BASE do tools/sprites/efeito_imagem.py). */
    private static final float TINTA_BASE_R = 0.35f;
    /** Inclinação de cada vaga (graus, positivo para cima no sentido do efeito). */
    private final float[] angulo = new float[VAGAS];
    private int proxima;

    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Rect fonte = new Rect();
    private final RectF destino = new RectF();

    EfeitosVideo(AssetManager assets) {
        this.assets = assets;
    }

    /** Lê todas as folhas de {@code assets/efeitos} (na montagem da luta, não no meio dela). */
    synchronized void carregar() {
        if (carregou || assets == null) return;
        carregou = true;
        String[] nomes;
        try {
            nomes = assets.list(PASTA);
        } catch (IOException e) {
            return;
        }
        if (nomes == null) return;
        Map<String, Folha> lidas = new HashMap<>();
        for (String nome : nomes) {
            try {
                Folha f = ler(nome);
                if (f != null) lidas.put(nome, f);
            } catch (Exception e) {
                Log.w(LOG_TAG, "efeito " + nome + " não carregou", e);
            }
        }
        folhas = lidas;
    }

    private Folha ler(String nome) throws Exception {
        String base = PASTA + "/" + nome + "/";
        JSONObject dados;
        try (InputStream in = assets.open(base + "efeito.json")) {
            dados = new JSONObject(texto(in));
        } catch (IOException semJson) {
            return null;
        }
        Bitmap bitmap;
        try (InputStream in = assets.open(base + "folha.webp")) {
            bitmap = BitmapFactory.decodeStream(in);
        }
        if (bitmap == null) return null;
        JSONArray q = dados.getJSONArray("quadros"), ms = dados.getJSONArray("duracoesMs");
        int n = q.length();
        int[][] quadros = new int[n][4];
        float[][] desloc = new float[n][2];
        float[] fim = new float[n];
        float t = 0f;
        for (int i = 0; i < n; i++) {
            JSONArray a = q.getJSONArray(i);
            for (int j = 0; j < 4; j++) quadros[i][j] = a.getInt(j);
            desloc[i][0] = (float)a.getDouble(4);
            desloc[i][1] = (float)a.getDouble(5);
            t += (float)ms.getDouble(Math.min(i, ms.length() - 1));
            fim[i] = t;
        }
        Folha f = new Folha(nome, bitmap, (float)dados.optDouble("pixelScale", 1.0), quadros, desloc, fim);
        f.marcaMs = (float)dados.optDouble("marcaMs", -1.0);
        f.cena = dados.optBoolean("cena", false);
        f.larguraCena = (float)dados.optDouble("larguraCena", 0.0);
        f.soma = Math.max(0f, Math.min(1f, (float)dados.optDouble("soma", 0.0)));
        JSONObject luz = dados.optJSONObject("luz");
        if (luz != null) lerLuz(f, luz, nome);
        return f;
    }

    /** O {@code "luz"} do efeito.json: cor obrigatória, alcance acima de 0, força de 0 a 1. */
    static void lerLuz(Folha f, JSONObject luz, String nome) {
        String cor = luz.optString("cor", "");
        if (!cor.matches("#[0-9A-Fa-f]{6}")) throw new IllegalArgumentException(nome + ": \"luz\" precisa de \"cor\" #RRGGBB");
        float alcance = (float)luz.optDouble("alcance", 0.0), forca = (float)luz.optDouble("forca", 0.0);
        if (!(alcance > 0f)) throw new IllegalArgumentException(nome + ": \"luz\" precisa de \"alcance\" acima de 0");
        if (!(forca >= 0f && forca <= 1f)) throw new IllegalArgumentException(nome + ": \"forca\" da luz entre 0 e 1");
        f.luzCor = Integer.parseInt(cor.substring(1), 16);
        f.luzAlcance = alcance;
        f.luzForca = forca;
    }

    private static String texto(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int lidos;
        while ((lidos = in.read(buffer)) != -1) out.write(buffer, 0, lidos);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    /** Põe uma folha pronta (testes e ferramentas). */
    void adicionar(Folha f) {
        Map<String, Folha> novas = new HashMap<>(folhas);
        novas.put(f.nome, f);
        folhas = novas;
    }

    /** A marca do efeito (ms em que o vídeo para para algo entrar por cima); -1 se não tem. */
    float marcaMs(String nome) {
        Folha f = folhas.get(nome);
        return f == null ? -1f : f.marcaMs;
    }

    /**
     * Até onde o vídeo do efeito vai em cima e embaixo, em px do mundo a partir da origem (no
     * tamanho 1): {topo, base}, a união de todos os quadros. O vídeo que encosta na beira corta
     * ali (a ativação do SHINKAI põe as faixas pretas nesse limite). Null se não existe.
     */
    float[] limitesY(String nome) {
        Folha f = folhas.get(nome);
        if (f == null || f.quadros.length == 0) return null;
        float topo = Float.MAX_VALUE, base = -Float.MAX_VALUE;
        for (int i = 0; i < f.quadros.length; i++) {
            topo = Math.min(topo, f.desloc[i][1] / f.pixelScale);
            base = Math.max(base, (f.desloc[i][1] + f.quadros[i][3]) / f.pixelScale);
        }
        return new float[]{topo, base};
    }

    /**
     * O efeito é uma cena inteira (o vídeo já tem o movimento, como o dragão do Ultra SHINKAI que
     * atravessa a tela): o efeito.json marca "cena": true.
     */
    boolean temCena(String nome) {
        Folha f = folhas.get(nome);
        return f != null && f.cena;
    }

    /** Largura do quadro do vídeo da cena no mundo (px), ou 0. */
    float larguraCena(String nome) {
        Folha f = folhas.get(nome);
        return f == null ? 0f : f.larguraCena;
    }

    /** O efeito existe nos assets (depois de {@link #carregar}). */
    boolean tem(String nome) {
        return folhas.containsKey(nome);
    }

    /**
     * Toca o efeito com a origem em (x, y) do mundo, no tamanho {@code escala} (1 = o do
     * conversor) e espelhado na horizontal se pedido (o vídeo é feito para quem olha para a
     * direita). Devolve false se o efeito não existe.
     */
    boolean tocar(String nome, float px, float py, float tamanho, boolean espelhar) {
        Folha f = folhas.get(nome);
        if (f == null || f.quadros.length == 0) return false;
        int i = proxima;
        proxima = (proxima + 1) % VAGAS;
        tocando[i] = f;
        x[i] = px;
        y[i] = py;
        escala[i] = tamanho;
        espelhado[i] = espelhar;
        atras[i] = false;
        angulo[i] = 0f;
        idadeMs[i] = 0f;
        vx[i] = 0f;
        fimMs[i] = -1f;
        ritmo[i] = 1f;
        ultima = i;
        return true;
    }

    /**
     * Toca o efeito inteiro em {@code duracaoMs} (mais rápido ou mais devagar que o vídeo): a
     * faísca dura a pausa do golpe e some antes de quem apanhou poder agir
     * (docs/revisao-base/direcao.md, regra E6). Devolve a vaga, ou -1 se o efeito não existe.
     */
    int tocarDurando(String nome, float px, float py, float tamanho, boolean espelhar, float duracaoMs) {
        return tocarDurando(nome, px, py, tamanho, espelhar, duracaoMs, 0f);
    }

    /**
     * Como o de cima, inclinado {@code graus} em volta da origem (positivo para cima, no sentido em
     * que o efeito aponta): a faísca segue a linha do golpe (o chute que sobe).
     */
    int tocarDurando(String nome, float px, float py, float tamanho, boolean espelhar, float duracaoMs, float graus) {
        if (!tocar(nome, px, py, tamanho, espelhar)) return -1;
        angulo[ultima] = graus;
        float natural = tocando[ultima].duracaoMs();
        ritmo[ultima] = natural > 0f && duracaoMs > 0f ? natural / duracaoMs : 1f;
        return ultima;
    }

    /**
     * O filtro que pinta de {@code rgb} (0xRRGGBB) uma folha feita para pintar (--tingivel no
     * tools/sprites/efeito_imagem.py): o corpo dela vai do azul de ar ao branco do miolo, com o
     * quanto é branco no canal vermelho; aqui o azul vira a cor e o miolo fica branco.
     */
    private android.graphics.ColorFilter filtroDaCor(int rgb) {
        android.graphics.ColorFilter f = tintas.get(rgb);
        if (f == null) {
            float[] m = new float[20];
            for (int c = 0; c < 3; c++) {
                float t = ((rgb >> (16 - 8 * c)) & 255) / 255f;
                // branco = (R - base) / (1 - base); saída = cor + (1 - cor) * branco.
                m[c * 5] = (1f - t) / (1f - TINTA_BASE_R);
                m[c * 5 + 4] = 255f * (t - (1f - t) * TINTA_BASE_R / (1f - TINTA_BASE_R));
            }
            m[18] = 1f;
            f = Filtro.matriz(m).canvas;
            tintas.put(rgb, f);
        }
        return f;
    }

    /**
     * Desenha a textura {@code nome} (uma fita reta: a ponta velha à esquerda, a nova à direita, a
     * borda nítida em cima) dobrada ao longo de um caminho: dos pontos {@code de} a {@code ate} (sem
     * ele), a borda nítida passa por (fx, fy) e a fita vai até (dx, dy), com a opacidade
     * {@code alfa} de cada ponto, pintada de {@code rgb}. É o rastro do golpe ({@link RastroDoGolpe}):
     * cada trecho entre dois pontos é uma fatia da textura, girada na direção do trecho.
     */
    void fita(Canvas c, String nome, float[] fx, float[] fy, float[] dx, float[] dy, float[] alfa, int de, int ate, int rgb) {
        Folha f = folhas.get(nome);
        if (f == null || f.quadros.length == 0 || ate - de < 2) return;
        int[] r = f.quadros[0];
        // A textura anda pelo comprimento do caminho (não pela conta dos pontos): os riscos não
        // esticam nem encolhem onde os pontos ficam mais juntos ou mais separados.
        if (percorrido.length < ate) percorrido = new float[ate];
        percorrido[de] = 0f;
        for (int i = de + 1; i < ate; i++) {
            float vx = fx[i] - fx[i - 1], vy = fy[i] - fy[i - 1];
            percorrido[i] = percorrido[i - 1] + (float) Math.sqrt(vx * vx + vy * vy);
        }
        float total = percorrido[ate - 1];
        if (total < 1f) return;
        paint.setColorFilter(filtroDaCor(rgb));
        for (int i = de; i < ate - 1; i++) {
            float a = (alfa[i] + alfa[i + 1]) * 0.5f;
            if (a <= 0.01f) continue;
            float vx = fx[i + 1] - fx[i], vy = fy[i + 1] - fy[i];
            float comprimento = (float) Math.sqrt(vx * vx + vy * vy);
            if (comprimento < 0.5f) continue;
            // A largura da fatia e para que lado ela vai: para o lado da borda de dentro.
            float mx = (dx[i] + dx[i + 1]) * 0.5f - (fx[i] + fx[i + 1]) * 0.5f;
            float my = (dy[i] + dy[i + 1]) * 0.5f - (fy[i] + fy[i + 1]) * 0.5f;
            float lado = (vx * my - vy * mx) / comprimento;
            int u0 = r[0] + Math.round(r[2] * percorrido[i] / total);
            int u1 = r[0] + Math.round(r[2] * percorrido[i + 1] / total);
            if (u1 <= u0) continue;
            fonte.set(u0, r[1], u1, r[1] + r[3]);
            // Um pouco de sobra no comprimento: as fatias se encostam nas curvas.
            destino.set(0f, 0f, comprimento + 1f, Math.abs(lado));
            paint.setAlpha(Math.round(255f * a));
            c.save();
            c.translate(fx[i], fy[i]);
            c.rotate((float) Math.toDegrees(Math.atan2(vy, vx)));
            if (lado < 0f) c.scale(1f, -1f);
            c.drawBitmap(f.bitmap, fonte, destino, paint);
            c.restore();
        }
        paint.setAlpha(255);
        paint.setColorFilter(null);
    }

    /** Tira da tela o efeito da vaga, se ainda for o mesmo ({@code nome}). */
    void parar(int vaga, String nome) {
        if (vaga >= 0 && vaga < VAGAS && tocando[vaga] != null && tocando[vaga].nome.equals(nome)) tocando[vaga] = null;
    }

    /**
     * Toca o efeito andando: a origem anda {@code vxPorPasso} px do mundo a cada passo (para a
     * esquerda se espelhado) e os quadros se repetem até {@code duracaoMs} (o dragão do Ultra
     * SHINKAI: um ciclo do corpo ondulando, levado pela tela).
     */
    boolean tocarAndando(String nome, float px, float py, float tamanho, boolean espelhar, float vxPorPasso, float duracaoMs) {
        if (!tocar(nome, px, py, tamanho, espelhar)) return false;
        int i = (proxima + VAGAS - 1) % VAGAS;
        vx[i] = espelhar ? -vxPorPasso : vxPorPasso;
        fimMs[i] = duracaoMs;
        return true;
    }

    /**
     * Leva os efeitos {@code nome} que estão tocando para (x, y): a cena que acompanha a câmera
     * (o dragão do Ultra SHINKAI fica sempre cobrindo a tela, mesmo com a câmera andando).
     */
    void mover(String nome, float px, float py) {
        for (int i = 0; i < VAGAS; i++) {
            if (tocando[i] != null && tocando[i].nome.equals(nome)) {
                x[i] = px;
                y[i] = py;
            }
        }
    }

    /** Um passo de 60 Hz. */
    void passo() {
        for (int i = 0; i < VAGAS; i++) {
            if (tocando[i] == null) continue;
            idadeMs[i] += MS_POR_PASSO * ritmo[i];
            x[i] += vx[i];
            float fim = fimMs[i] >= 0f ? fimMs[i] : tocando[i].duracaoMs();
            if (idadeMs[i] >= fim) tocando[i] = null;
        }
    }

    void limpar() {
        java.util.Arrays.fill(tocando, null);
    }

    /** Números por luz em {@link #luzes}: x, y (mundo), alcance (alturas de tela), r, g, b, força agora. */
    static final int LUZ_N = 7;

    /**
     * As luzes dos efeitos que estão tocando e emitem luz ({@code "luz"} no efeito.json), a partir
     * de {@code out[de]}: x e y da origem no mundo, alcance, cor (0 a 1) e a força vezes a vida do
     * efeito ({@link #vida}). Devolve até onde escreveu (no máximo {@code out.length}).
     */
    int luzes(float[][] out, int de) {
        int n = de;
        for (int j = 0; j < VAGAS && n < out.length; j++) {
            int i = (proxima + j) % VAGAS;
            Folha f = tocando[i];
            if (f == null || f.luzForca <= 0f) continue;
            float fim = fimMs[i] >= 0f ? fimMs[i] : f.duracaoMs();
            float k = f.luzForca * vida(fim > 0f ? idadeMs[i] / fim : 1f);
            if (k <= 0f) continue;
            escreverLuz(out[n++], f, x[i], y[i], k);
        }
        return n;
    }

    /** A luz do projétil que é uma folha ({@link #desenharEm}) em (x, y): a força cheia enquanto ele existe. */
    boolean luzEm(String nome, float px, float py, float[] out) {
        Folha f = folhas.get(nome);
        if (f == null || f.luzForca <= 0f) return false;
        escreverLuz(out, f, px, py, f.luzForca);
        return true;
    }

    private static void escreverLuz(float[] o, Folha f, float px, float py, float forca) {
        o[0] = px; o[1] = py; o[2] = f.luzAlcance;
        o[3] = ((f.luzCor >> 16) & 0xFF) / 255f; o[4] = ((f.luzCor >> 8) & 0xFF) / 255f; o[5] = (f.luzCor & 0xFF) / 255f;
        o[6] = forca;
    }

    /**
     * A vida da luz de um efeito em {@code t} (0 a 1 do tempo dele): acende em 15% do tempo e apaga
     * no resto, mais depressa no fim (a curva da luz do impacto de antes).
     */
    static float vida(float t) {
        if (t < 0f || t >= 1f) return 0f;
        float k = t < 0.15f ? t / 0.15f : (1f - t) / 0.85f;
        return k * k;
    }

    /** Quantos estão tocando (testes). */
    int tocando() {
        int n = 0;
        for (Folha f : tocando) if (f != null) n++;
        return n;
    }

    /**
     * Desenha o quadro de {@code ms} do efeito (em loop) com a origem em (x, y): o projétil que
     * é uma folha de vídeo (o cometa do S1 e a esfera do Super do Ryūga), levado pelo motor.
     * False se o efeito não existe (quem chama desenha o clássico).
     */
    boolean desenharEm(Canvas c, String nome, float ms, float px, float py, float tamanho, boolean espelhar) {
        Folha f = folhas.get(nome);
        if (f == null || f.quadros.length == 0 || f.duracaoMs() <= 0f) return false;
        int q = Math.max(0, f.quadroEm(ms % f.duracaoMs()));
        int[] r = f.quadros[q];
        fonte.set(r[0], r[1], r[0] + r[2], r[1] + r[3]);
        float k = tamanho / f.pixelScale;
        float left = f.desloc[q][0] * k, top = f.desloc[q][1] * k;
        destino.set(px + left, py + top, px + left + r[2] * k, py + top + r[3] * k);
        if (espelhar) {
            c.save();
            c.scale(-1f, 1f, px, 0f);
            pintar(c, f);
            c.restore();
        } else {
            pintar(c, f);
        }
        return true;
    }

    /** O último efeito tocado vai atrás dos lutadores ({@link #desenharAtras}). */
    void atrasDosLutadores() {
        atras[ultima] = true;
    }

    /** Desenha no espaço do mundo (o canvas já com a câmera), do mais velho para o mais novo. */
    void desenhar(Canvas c) {
        desenhar(c, false);
    }

    /** Os efeitos que vão atrás dos lutadores: chamado antes de desenhar os lutadores. */
    void desenharAtras(Canvas c) {
        desenhar(c, true);
    }

    private void desenhar(Canvas c, boolean camadaAtras) {
        for (int j = 0; j < VAGAS; j++) {
            int i = (proxima + j) % VAGAS;
            Folha f = tocando[i];
            if (f == null || atras[i] != camadaAtras) continue;
            float idade = fimMs[i] >= 0f && f.duracaoMs() > 0f ? idadeMs[i] % f.duracaoMs() : idadeMs[i];
            int q = f.quadroEm(idade);
            if (q < 0) continue;
            int[] r = f.quadros[q];
            fonte.set(r[0], r[1], r[0] + r[2], r[1] + r[3]);
            float k = escala[i] / f.pixelScale;
            float left = f.desloc[q][0] * k, top = f.desloc[q][1] * k;
            destino.set(x[i] + left, y[i] + top, x[i] + left + r[2] * k, y[i] + top + r[3] * k);
            if (espelhado[i] || angulo[i] != 0f) {
                c.save();
                // A tela tem y para baixo: para cima é girar ao contrário do relógio (ao contrário,
                // espelhado, porque o efeito aponta para a esquerda).
                if (angulo[i] != 0f) c.rotate(espelhado[i] ? angulo[i] : -angulo[i], x[i], y[i]);
                if (espelhado[i]) c.scale(-1f, 1f, x[i], 0f);
                pintar(c, f);
                c.restore();
            } else {
                pintar(c, f);
            }
        }
    }

    private final Paint paintSoma = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);

    {
        paintSoma.setXfermode(Mistura.SOMA);
    }

    /**
     * O quadro de {@link #fonte} em {@link #destino}, misturado pela {@link Folha#soma}: cobrindo com
     * alfa (1 − soma) e depois somando com alfa soma. As duas passadas dão o mesmo que a mistura
     * parcial do OpenGL (embaixo × (1 − alfa × (1 − soma)) + a cor), em qualquer desenho.
     */
    private void pintar(Canvas c, Folha f) {
        float k = f.soma;
        if (k <= 0f) {
            c.drawBitmap(f.bitmap, fonte, destino, paint);
            return;
        }
        int a = paint.getAlpha();
        if (k < 1f) {
            paint.setAlpha(Math.round(a * (1f - k)));
            c.drawBitmap(f.bitmap, fonte, destino, paint);
            paint.setAlpha(a);
        }
        paintSoma.setAlpha(Math.round(a * k));
        paintSoma.setColorFilter(paint.getColorFilter());
        c.drawBitmap(f.bitmap, fonte, destino, paintSoma);
    }
}
