package com.gamelutagpt;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.UnaryOperator;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Tela de vitória: quando o lutador do jogador vence, a luta some atrás do cenário borrado, ele
 * entra pela esquerda e faz a cena (vídeo no verde, quadro a quadro), "VITÓRIA" bate à direita e
 * um quadro de mangá traz uma frase dele. Cada lutador que tem a cena põe uma pasta em
 * {@code assets/vitorias/<id>/} (vitoria.json e os quadros q000.webp..., feitos pelo
 * tools/sprites/vitoria_video.py); quem não tem fica sem a tela, como antes. O fundo é o do
 * cenário da luta ({@code vitorias/fundos/<cenário>.webp}) ou o padrão.
 *
 * O vídeo tem muitos quadros grandes: na montagem da luta só os primeiros são lidos; o resto é
 * lido numa thread à parte, alguns à frente do que está na tela, e solto depois de mostrado.
 */
final class TelaVitoria {
    private static final String LOG_TAG = "TelaVitoria";
    private static final String PASTA = "vitorias";
    private static final String FUNDO_PADRAO = "padrao";
    private static final float MS_POR_PASSO = 1000f / 60f;
    /** Tinta e papel da página do Ultra (ArteProvisoria). */
    private static final int TINTA = 0xFF111114, PAPEL = 0xFFFFFFFF;
    /** Quadros lidos na montagem (a cena começa sem esperar) e quantos são lidos à frente. */
    private static final int INICIO = 6, A_FRENTE = 12;
    /** O fundo borrado cobre a luta nesse tempo. */
    private static final float FUNDO_MS = 200f;
    /** O lutador entra deslizando da esquerda, já tocando o vídeo. */
    private static final float ENTRA_MS = 80f, DESLIZA_MS = 260f, DESLIZA_PX = 180f;
    /** "VITÓRIA" bate (cai grande e assenta), pisca a tela e treme. */
    private static final float TITULO_MS = 340f, BATIDA_MS = 130f, CLARAO_MS = 120f, TREME_MS = 220f;
    private static final float TITULO_X = 985f, TITULO_Y = 222f, TITULO_TAM = 132f, TITULO_LARGURA = 540f;
    /** O quadro de mangá com a frase. */
    private static final float QUADRO_MS = 800f, POP_MS = 160f;
    private static final float QUADRO_X = 990f, QUADRO_Y = 548f;
    private static final float[] QUADRO = {-245f, -82f, 238f, -96f, 230f, 88f, -250f, 80f};
    private static final float TEXTO_LARGURA = 410f, TEXTO_TAM = 36f;
    /** A tela escurece até o preto e então a história (ou a revanche) segue. */
    private static final float SAIDA_MS = 280f;
    /** Antes disso um toque não pula (o toque que acabou a luta não engole a cena). */
    private static final float PULAR_DEPOIS_MS = 600f;

    /** A cena de um lutador: os quadros (nomes e onde cada um fica) e as frases. */
    static final class Pacote {
        final String base;
        final String[] imagens;
        /** x, y (topo, negativo: acima do chão da tela), largura e altura de cada quadro. */
        final float[][] caixas;
        final float quadroMs, segurarMs;
        /** O x mais à esquerda do desenho em todos os quadros: ele encosta na borda da tela. */
        final float esquerda;
        final String[] frases;
        /** Os primeiros quadros, já lidos. */
        final Bitmap[] inicio;

        Pacote(String base, String[] imagens, float[][] caixas, float quadroMs, float segurarMs, String[] frases,
               Bitmap[] inicio) {
            this.base = base;
            this.imagens = imagens;
            this.caixas = caixas;
            this.quadroMs = quadroMs;
            this.segurarMs = segurarMs;
            this.frases = frases;
            this.inicio = inicio;
            float e = Float.MAX_VALUE;
            for (float[] r : caixas) e = Math.min(e, r[0]);
            esquerda = e;
        }

        /** Do começo da tela até o preto da saída. */
        float duracaoMs() {
            return ENTRA_MS + imagens.length * quadroMs + segurarMs + SAIDA_MS;
        }
    }

    private final AssetManager assets;
    /** Trocados inteiros ao carregar (a montagem roda fora da thread do desenho). */
    private volatile Map<String, Pacote> pacotes = new HashMap<>();
    private volatile Bitmap fundo;
    private String fundoNome;
    private final ExecutorService leitor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "tela-vitoria");
        t.setDaemon(true);
        return t;
    });
    private final Random sorteio = new Random();

    // A cena tocando agora (só na thread do jogo, menos os quadros que o leitor entrega).
    private Pacote atual;
    private AtomicReferenceArray<Bitmap> lidos;
    private boolean[] pedidos;
    /** Troca a cada cena: um quadro lido para uma cena que já acabou é jogado fora. */
    private volatile int geracao;
    private Bitmap mostrado;
    private int mostradoIndice;
    private int idade = -1;
    private boolean pularPedido;
    private String titulo, frase, nome;
    private int cor;
    private final List<String> linhas = new ArrayList<>();

    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint tinta = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path forma = new Path();
    private final RectF destino = new RectF();
    private final Typeface negrito = Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC);
    private final Typeface texto = Typeface.create("sans-serif-condensed", Typeface.BOLD);
    private final Shader brilhoTitulo = new Degrade.Linear(0f, TITULO_Y - TITULO_TAM * 0.75f, 0f, TITULO_Y,
        new int[] {0xFFFFFFFF, 0xFFFFF1B8, 0xFFFFC23A, 0xFFF0781E}, new float[] {0f, .4f, .75f, 1f},
        Shader.TileMode.CLAMP);

    TelaVitoria(AssetManager assets) {
        this.assets = assets;
    }

    /**
     * Deixa lida só a cena dos lutadores {@code ids} (os do jogador nesta luta) e o fundo do
     * {@code cenario}. Quem já estava carregado não é lido de novo.
     */
    synchronized void manter(String cenario, String... ids) {
        Map<String, Pacote> antes = pacotes, novos = new HashMap<>();
        for (String id : ids) {
            if (id == null || novos.containsKey(id)) continue;
            Pacote p = antes.get(id);
            if (p == null) {
                try {
                    p = ler(id);
                } catch (Exception e) {
                    Log.w(LOG_TAG, "vitória de " + id + " não carregou", e);
                }
            }
            if (p != null) novos.put(id, p);
        }
        pacotes = novos;
        if (novos.isEmpty()) {
            fundo = null;
            fundoNome = null;
            return;
        }
        String nomeFundo = existe(PASTA + "/fundos/" + cenario + ".webp") ? cenario : FUNDO_PADRAO;
        if (fundo != null && nomeFundo.equals(fundoNome)) return;
        try (InputStream in = assets.open(PASTA + "/fundos/" + nomeFundo + ".webp")) {
            fundo = BitmapFactory.decodeStream(in);
            fundoNome = nomeFundo;
        } catch (IOException e) {
            // Sem o fundo, a tela fica no escuro.
            fundo = null;
            fundoNome = null;
        }
    }

    private boolean existe(String caminho) {
        try (InputStream in = assets.open(caminho)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private Pacote ler(String id) throws Exception {
        String base = PASTA + "/" + id + "/";
        JSONObject dados;
        try (InputStream in = assets.open(base + "vitoria.json")) {
            dados = new JSONObject(texto(in));
        } catch (IOException semPasta) {
            return null;
        }
        JSONArray lista = dados.getJSONArray("quadros");
        int n = lista.length();
        if (n == 0) return null;
        String[] imagens = new String[n];
        float[][] caixas = new float[n][4];
        for (int i = 0; i < n; i++) {
            JSONObject q = lista.getJSONObject(i);
            imagens[i] = q.getString("imagem");
            caixas[i][0] = (float)q.getDouble("x");
            caixas[i][1] = (float)q.getDouble("y");
            caixas[i][2] = (float)q.getDouble("largura");
            caixas[i][3] = (float)q.getDouble("altura");
        }
        JSONArray f = dados.optJSONArray("frases");
        String[] frases = new String[f != null ? f.length() : 0];
        for (int i = 0; i < frases.length; i++) frases[i] = f.getString(i);
        Bitmap[] inicio = new Bitmap[Math.min(INICIO, n)];
        for (int i = 0; i < inicio.length; i++) {
            inicio[i] = quadro(base + imagens[i]);
            if (inicio[i] == null) throw new IOException("quadro " + imagens[i] + " ilegível");
        }
        return new Pacote(base, imagens, caixas, (float)dados.getDouble("quadroMs"),
            (float)dados.optDouble("segurarMs", 0), frases, inicio);
    }

    private Bitmap quadro(String caminho) {
        try (InputStream in = assets.open(caminho)) {
            return BitmapFactory.decodeStream(in);
        } catch (IOException e) {
            return null;
        }
    }

    private static String texto(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int lidos;
        while ((lidos = in.read(buffer)) != -1) out.write(buffer, 0, lidos);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    boolean tem(String id) {
        return id != null && pacotes.containsKey(id);
    }

    /**
     * Começa a cena de {@code id}. {@code cor}: a do lutador (a faixa do título e a etiqueta do
     * nome); {@code nome}: o nome na etiqueta (null, sem etiqueta); {@code traduzir}: os textos no
     * idioma do jogo.
     */
    void comecar(String id, int cor, String nome, UnaryOperator<String> traduzir) {
        Pacote p = pacotes.get(id);
        if (p == null) return;
        atual = p;
        geracao++;
        lidos = new AtomicReferenceArray<>(p.imagens.length);
        pedidos = new boolean[p.imagens.length];
        for (int i = 0; i < p.inicio.length; i++) {
            lidos.set(i, p.inicio[i]);
            pedidos[i] = true;
        }
        mostrado = null;
        idade = 0;
        pularPedido = false;
        this.cor = cor;
        this.nome = nome;
        titulo = traduzir.apply("VITÓRIA");
        frase = p.frases.length > 0 ? traduzir.apply(p.frases[sorteio.nextInt(p.frases.length)]) : null;
        linhas.clear();
        pedirAFrente(0);
    }

    boolean ativa() {
        return atual != null;
    }

    /** O fundo já cobre a tela inteira: a luta não precisa ser desenhada. */
    boolean cobreTudo() {
        return atual != null && ms() >= FUNDO_MS;
    }

    /** Toque ou tecla: depois do começo, a cena vai direto para a saída. */
    void pular() {
        if (atual != null) pularPedido = true;
    }

    void parar() {
        atual = null;
        lidos = null;
        pedidos = null;
        mostrado = null;
        idade = -1;
        geracao++;
    }

    private float ms() {
        return idade * MS_POR_PASSO;
    }

    /** Um passo de 60 Hz; no fim da saída a cena para ({@link #ativa()} fica falso). */
    void passo() {
        if (atual == null) return;
        float saida = atual.duracaoMs() - SAIDA_MS;
        if (pularPedido) {
            pularPedido = false;
            if (ms() >= PULAR_DEPOIS_MS && ms() < saida) idade = (int)Math.ceil(saida / MS_POR_PASSO);
        }
        idade++;
        if (ms() >= atual.duracaoMs()) {
            parar();
            return;
        }
        int i = indice();
        // Solta o que já passou (os primeiros ficam no pacote para a próxima vitória).
        for (int j = atual.inicio.length; j < i - 1; j++) lidos.set(j, null);
        pedirAFrente(i);
    }

    private int indice() {
        return Math.max(0, Math.min(atual.imagens.length - 1, (int)((ms() - ENTRA_MS) / atual.quadroMs)));
    }

    private void pedirAFrente(int i) {
        int fim = Math.min(atual.imagens.length, i + A_FRENTE);
        for (int j = i; j < fim; j++) {
            if (pedidos[j]) continue;
            pedidos[j] = true;
            final int indice = j, minha = geracao;
            final String caminho = atual.base + atual.imagens[j];
            final AtomicReferenceArray<Bitmap> destinoLido = lidos;
            leitor.execute(() -> {
                if (minha != geracao) return;
                Bitmap b = quadro(caminho);
                if (b != null && minha == geracao) destinoLido.set(indice, b);
            });
        }
    }

    /** Desenha a tela por cima de tudo, no espaço da tela (VW x VH). */
    void desenhar(Canvas c) {
        Pacote p = atual;
        if (p == null) return;
        float ms = ms();
        float cobre = clamp(ms / FUNDO_MS);
        float saida = p.duracaoMs() - SAIDA_MS;
        // Fundo: o cenário borrado e escurecido cobre a luta.
        paint.setAlpha(Math.round(255f * cobre));
        if (fundo != null) {
            destino.set(0f, 0f, Arena.VW, Arena.VH);
            c.drawBitmap(fundo, null, destino, paint);
            tinta.setColor(Color.argb(Math.round(70f * cobre), 0, 0, 10));
        } else {
            tinta.setColor(Color.argb(Math.round(235f * cobre), 12, 14, 24));
        }
        tinta.setStyle(Paint.Style.FILL);
        tinta.setShader(null);
        c.drawRect(0f, 0f, Arena.VW, Arena.VH, tinta);
        desenharRetrato(c, p, ms);
        desenharTitulo(c, ms);
        desenharQuadro(c, ms);
        if (ms >= TITULO_MS + BATIDA_MS && ms < TITULO_MS + BATIDA_MS + CLARAO_MS) {
            // Clarão quando o título assenta.
            float k = (ms - TITULO_MS - BATIDA_MS) / CLARAO_MS;
            tinta.setColor(Color.argb(Math.round(150f * (1f - k)), 255, 255, 255));
            c.drawRect(0f, 0f, Arena.VW, Arena.VH, tinta);
        }
        if (ms > saida) {
            tinta.setColor(Color.argb(Math.round(255f * clamp((ms - saida) / SAIDA_MS)), 0, 0, 0));
            c.drawRect(0f, 0f, Arena.VW, Arena.VH, tinta);
        }
    }

    /**
     * O quadro do vídeo na hora certa. O jogo roda a 60 Hz e o vídeo é mais lento: entre um
     * quadro e o próximo os dois se misturam, para o movimento sair contínuo, sem degraus.
     */
    private void desenharRetrato(Canvas c, Pacote p, float ms) {
        if (ms < ENTRA_MS) return;
        int i = indice();
        Bitmap b = lidos.get(i);
        // O próximo ainda não chegou: fica o último que estava na tela.
        if (b != null) {
            mostrado = b;
            mostradoIndice = i;
        }
        if (mostrado == null) return;
        float k = clamp((ms - ENTRA_MS) / DESLIZA_MS);
        float dx = -DESLIZA_PX * (1f - k) * (1f - k) * (1f - k);
        float alfa = 255f * clamp(k * 2.5f);
        desenharQuadroDoVideo(c, p, mostrado, mostradoIndice, dx, Math.round(alfa));
        float entre = (ms - ENTRA_MS) / p.quadroMs - i;
        Bitmap proximo = mostradoIndice == i && i + 1 < p.imagens.length ? lidos.get(i + 1) : null;
        if (proximo != null && entre > 0f) desenharQuadroDoVideo(c, p, proximo, i + 1, dx, Math.round(alfa * clamp(entre)));
    }

    private void desenharQuadroDoVideo(Canvas c, Pacote p, Bitmap b, int i, float dx, int alfa) {
        float[] r = p.caixas[i];
        float x = r[0] - p.esquerda + dx, y = Arena.VH + r[1];
        destino.set(x, y, x + r[2], y + r[3]);
        paint.setAlpha(alfa);
        c.drawBitmap(b, null, destino, paint);
    }

    private void desenharTitulo(Canvas c, float ms) {
        if (ms < TITULO_MS || titulo == null) return;
        float k = clamp((ms - TITULO_MS) / BATIDA_MS);
        float escala = 1f + 1.4f * (1f - k) * (1f - k);
        float depois = ms - TITULO_MS - BATIDA_MS;
        float treme = depois > 0f && depois < TREME_MS ? 7f * (1f - depois / TREME_MS) : 0f;
        float tx = (float)Math.sin(ms * 0.21f) * treme, ty = (float)Math.cos(ms * 0.27f) * treme;
        int alfa = Math.round(255f * clamp(k * 3f));
        // Faixa de tinta atrás, entrando pela direita, com o traço na cor do lutador.
        float faixa = Arena.VW + 40f - (Arena.VW + 40f - 640f) * (1f - (1f - k) * (1f - k));
        forma.rewind();
        forma.moveTo(faixa, TITULO_Y - 128f);
        forma.lineTo(Arena.VW + 40f, TITULO_Y - 150f);
        forma.lineTo(Arena.VW + 40f, TITULO_Y + 30f);
        forma.lineTo(faixa + 44f, TITULO_Y + 46f);
        forma.close();
        tinta.setStyle(Paint.Style.FILL);
        tinta.setShader(null);
        tinta.setColor(Color.argb(Math.round(0.82f * alfa), 10, 10, 16));
        c.drawPath(forma, tinta);
        tinta.setStyle(Paint.Style.STROKE);
        tinta.setStrokeWidth(5f);
        tinta.setColor(Color.argb(alfa, Color.red(cor), Color.green(cor), Color.blue(cor)));
        c.drawLine(faixa, TITULO_Y - 128f, Arena.VW + 40f, TITULO_Y - 150f, tinta);
        c.drawLine(faixa + 44f, TITULO_Y + 46f, Arena.VW + 40f, TITULO_Y + 30f, tinta);
        // O título: contorno na cor do lutador, tinta e o miolo dourado.
        tinta.setTypeface(negrito);
        tinta.setTextSize(TITULO_TAM);
        tinta.setTextAlign(Paint.Align.CENTER);
        tinta.setTextSkewX(-0.12f);
        float largura = tinta.measureText(titulo);
        float caber = largura > TITULO_LARGURA ? TITULO_LARGURA / largura : 1f;
        c.save();
        c.translate(tx, ty);
        c.scale(escala * caber, escala * caber, TITULO_X, TITULO_Y - TITULO_TAM * 0.35f);
        tinta.setStyle(Paint.Style.STROKE);
        tinta.setStrokeJoin(Paint.Join.ROUND);
        tinta.setStrokeWidth(30f);
        tinta.setColor(Color.argb(alfa, Color.red(cor), Color.green(cor), Color.blue(cor)));
        c.drawText(titulo, TITULO_X, TITULO_Y, tinta);
        tinta.setStrokeWidth(15f);
        tinta.setColor(Color.argb(alfa, 17, 17, 20));
        c.drawText(titulo, TITULO_X, TITULO_Y, tinta);
        tinta.setStyle(Paint.Style.FILL);
        tinta.setShader(brilhoTitulo);
        tinta.setAlpha(alfa);
        c.drawText(titulo, TITULO_X, TITULO_Y, tinta);
        tinta.setShader(null);
        c.restore();
        tinta.setStrokeJoin(Paint.Join.MITER);
        tinta.setTextSkewX(0f);
        tinta.setTextAlign(Paint.Align.LEFT);
        tinta.setAlpha(255);
    }

    /** O quadro de mangá: papel, moldura branca com traço de tinta, a frase e o nome dele. */
    private void desenharQuadro(Canvas c, float ms) {
        if (ms < QUADRO_MS || frase == null) return;
        float k = clamp((ms - QUADRO_MS) / POP_MS);
        // Salta um pouco além e assenta.
        float s = 0.7f + 0.3f * (1f + 2.2f * (float)Math.pow(k - 1f, 3) + 1.2f * (float)Math.pow(k - 1f, 2));
        int alfa = Math.round(255f * clamp(k * 2f));
        c.save();
        c.translate(QUADRO_X, QUADRO_Y);
        c.rotate(-2f);
        c.scale(s, s);
        forma.rewind();
        forma.moveTo(QUADRO[0], QUADRO[1]);
        for (int i = 2; i < QUADRO.length; i += 2) forma.lineTo(QUADRO[i], QUADRO[i + 1]);
        forma.close();
        tinta.setShader(null);
        tinta.setStyle(Paint.Style.FILL);
        c.save();
        c.translate(10f, 12f);
        tinta.setColor(Color.argb(Math.round(0.47f * alfa), 0, 0, 0));
        c.drawPath(forma, tinta);
        c.restore();
        tinta.setColor(Color.argb(alfa, 255, 255, 255));
        c.drawPath(forma, tinta);
        tinta.setStyle(Paint.Style.STROKE);
        tinta.setStrokeWidth(16f);
        tinta.setColor(Color.argb(alfa, Color.red(PAPEL), Color.green(PAPEL), Color.blue(PAPEL)));
        c.drawPath(forma, tinta);
        tinta.setStrokeWidth(6f);
        tinta.setColor(Color.argb(alfa, Color.red(TINTA), Color.green(TINTA), Color.blue(TINTA)));
        c.drawPath(forma, tinta);
        // A frase, quebrada em linhas no meio do quadro.
        tinta.setStyle(Paint.Style.FILL);
        tinta.setTypeface(texto);
        tinta.setTextSize(TEXTO_TAM);
        tinta.setTextAlign(Paint.Align.CENTER);
        if (linhas.isEmpty()) quebrar(frase, tinta, TEXTO_LARGURA, linhas);
        float passoLinha = TEXTO_TAM * 1.22f;
        float y = -passoLinha * (linhas.size() - 1) * 0.5f + TEXTO_TAM * 0.36f + 4f;
        for (String linha : linhas) {
            c.drawText(linha, 0f, y, tinta);
            y += passoLinha;
        }
        if (nome != null) {
            // A etiqueta do nome no canto de cima, na cor do lutador.
            tinta.setTypeface(negrito);
            tinta.setTextSize(26f);
            tinta.setTextAlign(Paint.Align.LEFT);
            float w = tinta.measureText(nome) + 36f;
            forma.rewind();
            forma.moveTo(QUADRO[0] + 18f, QUADRO[1] - 22f);
            forma.lineTo(QUADRO[0] + 18f + w, QUADRO[1] - 26f);
            forma.lineTo(QUADRO[0] + 10f + w, QUADRO[1] + 12f);
            forma.lineTo(QUADRO[0] + 10f, QUADRO[1] + 16f);
            forma.close();
            tinta.setColor(Color.argb(alfa, Color.red(cor), Color.green(cor), Color.blue(cor)));
            c.drawPath(forma, tinta);
            tinta.setStyle(Paint.Style.STROKE);
            tinta.setStrokeWidth(4f);
            tinta.setColor(Color.argb(alfa, 17, 17, 20));
            c.drawPath(forma, tinta);
            tinta.setStyle(Paint.Style.FILL);
            tinta.setColor(Color.argb(alfa, 255, 255, 255));
            c.drawText(nome, QUADRO[0] + 30f, QUADRO[1] + 2f, tinta);
        }
        tinta.setTextAlign(Paint.Align.LEFT);
        c.restore();
    }

    /** Quebra {@code texto} em linhas de até {@code largura} px na fonte de {@code p}. */
    static void quebrar(String texto, Paint p, float largura, List<String> saida) {
        StringBuilder linha = new StringBuilder();
        for (String palavra : texto.trim().split("\\s+")) {
            String tentativa = linha.length() == 0 ? palavra : linha + " " + palavra;
            if (linha.length() > 0 && p.measureText(tentativa) > largura) {
                saida.add(linha.toString());
                linha.setLength(0);
                linha.append(palavra);
            } else {
                linha.setLength(0);
                linha.append(tentativa);
            }
        }
        if (linha.length() > 0) saida.add(linha.toString());
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
