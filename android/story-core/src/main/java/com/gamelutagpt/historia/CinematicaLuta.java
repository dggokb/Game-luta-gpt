package com.gamelutagpt.historia;

import com.gamelutagpt.render.MiniJson;
import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Cinemática dentro da luta: num momento certo (o gatilho: alguém com pouca vida, o começo, um
 * tempo de luta, o KO), a luta vira cena. Faixas pretas entram em cima e embaixo, o jogador
 * perde o controle e um roteiro guia os dois lutadores (pular para trás, correr um na direção
 * do outro, parar), põe falas nos quadros laterais ({@link ConversaLuta}), estoura
 * onomatopeias de mangá na tela, mexe na câmera e pode terminar a luta. Também pinta a tela
 * de tinta, mostra uma página de mangá por cima e pede quick time events (QTE): acertou, o
 * lutador do jogador bate de verdade e o rival perde o pedaço de vida do roteiro.
 *
 * O roteiro fica num arquivo JSON da história ({@code "cinematica"} no passo de luta, caminho
 * dentro de {@code historia/}). Esta classe é só a linha do tempo e o desenho (faixas e
 * onomatopeias); quem mexe nos lutadores e trava o controle é o jogo, pelo {@link Palco}. As
 * ações rodam em sequência: cada uma espera terminar antes da próxima, a não ser que diga
 * {@code "semEsperar": true}. Padrão completo: docs/historia/motor-cinematicas.md.
 */
public final class CinematicaLuta {
    public static final int JOGADOR = 0;
    public static final int RIVAL = 1;
    static final float VW = 1280f;
    static final float VH = 720f;

    /** Altura padrão de cada faixa preta: o quadro de 1280x720 vira um 2,39:1 de cinema. */
    static final float FAIXA_ALTURA = 92f;
    static final float FAIXA_DURACAO = 0.45f;
    static final float ZOOM_MIN = 0.8f;
    static final float ZOOM_MAX = 2.0f;
    /** Cada palavra da onomatopeia estoura um pouco depois da anterior ("DING"... "DONG!"). */
    static final float PALAVRA_INTERVALO = 0.14f;
    static final float POP = 0.16f;
    static final float SAIDA_ONOMATOPEIA = 0.25f;
    /** Movimento ou fala nunca seguram a cena para sempre (um lutador preso numa queda...). */
    static final float LIMITE_ESPERA = 8f;
    /** QTE: quanto o resultado (BOA!/ERROU!) fica antes do golpe sair. */
    static final float RESULTADO_QTE = 0.4f;
    /** QTE de tempo: folga (s) em volta do instante em que o anel fecha no botão. */
    static final float JANELA_TEMPO = 0.17f;
    /** QTE de deslizar: quanto o dedo anda (px da tela 1280x720) para valer. */
    static final float DESLIZE = 90f;
    static final float BOTAO_RAIO = 62f;
    /** A página pode ser pulada com um toque depois disso. */
    static final float PAGINA_PULAR = 0.6f;
    private static final int INK = Baloes.INK;
    private static final int WHITE = Baloes.WHITE;

    /** Quem executa na luta o que a cinemática pede (o GameView). */
    public interface Palco {
        /** Trava (true) ou devolve (false) o controle do jogador; travado, ninguém usa o pad nem a IA. */
        void travarControle(boolean travar);

        /** Começa um movimento roteirizado do lutador do lado {@link #JOGADOR} ou {@link #RIVAL}. */
        void mover(int lado, Movimento movimento);

        /** O último movimento pedido para esse lado ainda não acabou. */
        boolean movendo(int lado);

        /** Fim da luta pela cinemática: {@code "jogador"}, {@code "rival"} ou {@code "interrompida"}. */
        void terminarLuta(String vencedor);

        /**
         * O lutador do lado {@link #JOGADOR} ou {@link #RIVAL} está agora à direita do outro na
         * tela. A fala dele aparece no quadro desse lado (se os dois trocaram de lado na luta,
         * os quadros trocam junto).
         */
        boolean aDireita(int lado);

        /**
         * O lutador do lado {@code lado} vai até o outro e dá o golpe (o QTE): o alvo perde
         * {@link Golpe#dano} da vida máxima, ou cai de vez com {@link Golpe#finaliza}.
         */
        default void golpear(int lado, Golpe golpe) {}

        /** O golpe pedido para esse lado ainda não acabou. */
        default boolean golpeando(int lado) {
            return false;
        }
    }

    /** Um golpe roteirizado pedido ao {@link Palco}: o acerto (ou o erro) de um QTE. */
    public static final class Golpe {
        /** O botão do golpe: "L", "M", "H", agachado ("2L", "2M", "2H") ou um especial ("S", "S2"...). */
        public final String golpe;
        /** Quanto da vida máxima do alvo ele tira (0 a 1). */
        public final float dano;
        /** O golpe derruba de vez: o alvo cai (KO), tenha a vida que tiver. */
        public final boolean finaliza;

        public Golpe(String golpe, float dano, boolean finaliza) {
            this.golpe = golpe;
            this.dano = dano;
            this.finaliza = finaliza;
        }
    }

    /** Os tipos de QTE: tocar, tocar muitas vezes, deslizar numa direção, tocar na hora do anel. */
    public enum TipoQte { TOQUE, MARTELAR, DESLIZAR, TEMPO }

    /** Textos fixos do QTE (as dicas embaixo do botão e o resultado), para conferir as traduções. */
    public static final List<String> TEXTOS_QTE = Collections.unmodifiableList(Arrays.asList(
        "TOQUE!", "APERTE!", "TOQUE RÁPIDO!", "APERTE RÁPIDO!", "DESLIZE!", "DIRECIONAL!", "NA HORA CERTA!",
        "BOA!", "ERROU!"));

    /** Fases do toque para {@link #toque}. */
    public static final int TOQUE_DESCE = 0;
    public static final int TOQUE_MOVE = 1;
    public static final int TOQUE_SOBE = 2;

    /** Quando a cinemática começa. */
    public static final class Gatilho {
        public enum Tipo { INICIO, VIDA, TEMPO, KO }

        public final Tipo tipo;
        /** VIDA: fração da vida (0..1) do lutador da vez; TEMPO: segundos de luta. */
        public final float valor;
        /** VIDA: de quem ({@link #JOGADOR}, {@link #RIVAL}) ou -1 para qualquer um. */
        public final int lado;

        Gatilho(Tipo tipo, float valor, int lado) {
            this.tipo = tipo;
            this.valor = valor;
            this.lado = lado;
        }
    }

    /** Um movimento de lutador pedido ao {@link Palco}. */
    public static final class Movimento {
        public enum Tipo { TITULAR, PULAR_TRAS, RECUAR, CORRER, ANDAR, PARAR }

        public final Tipo tipo;
        /** CORRER e ANDAR: param quando o vão entre os dois corpos chega a isso (px); negativo = sem limite. */
        public final float distancia;
        /** Tempo máximo do movimento, em segundos (ele acaba antes se chegar). */
        public final float segundos;

        public Movimento(Tipo tipo, float distancia, float segundos) {
            this.tipo = tipo;
            this.distancia = distancia;
            this.segundos = segundos;
        }
    }

    /** Uma ação do roteiro, já lida e validada. */
    static final class Acao {
        enum Tipo { FAIXAS, CONTROLE, MOVER, FALA, ONOMATOPEIA, ESPERAR, CAMERA, VELOCIDADE, TERMINAR_LUTA, TINTA, PAGINA, QTE }

        final Tipo tipo;
        boolean semEsperar;
        /** FAIXAS: entrar; CONTROLE: travar. */
        boolean ligar;
        float altura = FAIXA_ALTURA;
        /** MOVER: quem se move ("jogador", "rival", "ambos" ou um irmão), resolvido na hora. */
        String quemMove = "ambos";
        Movimento movimento;
        String quem;
        String expressao = "normal";
        String texto;
        /** FALA: lado fixo do arquivo; null = o lado da tela onde está o lutador de {@link #time}. */
        Boolean naDireita;
        /** FALA sem lado fixo: de que time é quem fala ("jogador", "rival" ou um irmão). */
        String time;
        float segura = -1f;
        float x = 0.5f;
        float y = 0.42f;
        float tamanho = 140f;
        float tremor = 8f;
        float rotacao = -6f;
        int cor = 0xFFFFD23F;
        float segundos;
        float zoom = 1f;
        /** CAMERA: em quem a câmera foca ("jogador", "rival", "centro" ou um irmão); null = não muda. */
        String foco;
        /** VELOCIDADE: velocidade da luta (1 = normal, 0,3 = câmera lenta, 0 = congela). */
        float valor = 1f;
        String vencedor;
        /** PAGINA: as páginas (caminho dentro de {@code historia/}); uma é sorteada a cada luta. */
        List<String> imagens;
        /** QTE: o tipo, quantos toques (MARTELAR) e a direção (DESLIZAR: frente, tras, cima, baixo). */
        TipoQte qte;
        int vezes = 8;
        String direcao = "frente";
        /** QTE: o golpe de quem acerta ({@link #quemMove} bate) e o contra-ataque de quem erra. */
        Golpe acerto;
        Golpe erro;

        Acao(Tipo tipo) {
            this.tipo = tipo;
        }
    }

    /** O arquivo da cinemática lido: o gatilho e as ações em ordem. */
    public static final class Roteiro {
        public final Gatilho gatilho;
        final List<Acao> acoes;

        Roteiro(Gatilho gatilho, List<Acao> acoes) {
            this.gatilho = gatilho;
            this.acoes = acoes;
        }

        /** Falas e onomatopeias (em português), para conferir as traduções. */
        public List<String> textos() {
            List<String> out = new ArrayList<>();
            for (Acao a : acoes) {
                if (a.texto != null) out.add(a.texto);
            }
            return out;
        }

        /** Quem fala no roteiro (para conferir os rostos). */
        public List<String> quemFala() {
            List<String> out = new ArrayList<>();
            for (Acao a : acoes) {
                if (a.tipo == Acao.Tipo.FALA && !out.contains(a.quem)) out.add(a.quem);
            }
            return out;
        }

        /** Todas as páginas que a cena pode sortear (para conferir os arquivos). */
        public List<String> paginas() {
            List<String> out = new ArrayList<>();
            for (Acao a : acoes) {
                if (a.imagens != null) out.addAll(a.imagens);
            }
            return out;
        }

        /** O roteiro termina a luta (senão, ela continua depois da cena). */
        public boolean terminaLuta() {
            for (Acao a : acoes) {
                if (a.tipo == Acao.Tipo.TERMINAR_LUTA) return true;
            }
            return false;
        }
    }

    private enum Estado { NENHUMA, ARMADA, RODANDO, ACABOU }

    /** Uma onomatopeia na tela. */
    private static final class Estouro {
        final Acao acao;
        final float desde;
        /** As palavras do texto (dividido uma vez, não a cada quadro). */
        String textoDividido;
        String[] palavras;

        Estouro(Acao acao, float desde) {
            this.acao = acao;
            this.desde = desde;
        }
    }

    private final RenderAssets assets;
    private final ConversaLuta conversa;
    private final Palco palco;
    private Temporada temporada;
    private Roteiro roteiro;
    private String irmao = Torneio.RYUGA;
    private Estado estado = Estado.NENHUMA;
    private int indice;
    private Acao acao;
    private float acaoDesde;
    private float time;
    private boolean travado;
    private final Transicao faixas = new Transicao(0f);
    private float faixaAltura = FAIXA_ALTURA;
    private final Transicao zoom = new Transicao(1f);
    /** Peso do foco da câmera em cada lutador (0 = no centro da luta, 1 = nele). */
    private final Transicao focoJogador = new Transicao(0f);
    private final Transicao focoRival = new Transicao(0f);
    private final Transicao velocidade = new Transicao(1f);

    /** Um valor que vai suavemente de um ponto a outro num tempo (faixas, zoom, foco, velocidade). */
    private final class Transicao {
        private final float repouso;
        private float de;
        private float para;
        private float desde = -999f;
        private float duracao;

        Transicao(float repouso) {
            this.repouso = de = para = repouso;
        }

        float valor() {
            float k = duracao <= 0f ? 1f : Tinta.clamp01((time - desde) / duracao);
            return de + (para - de) * suave(k);
        }

        void ir(float alvo, float segundos) {
            de = valor();
            para = alvo;
            desde = time;
            duracao = segundos;
        }

        float alvo() {
            return para;
        }

        void zerar() {
            de = para = repouso;
            desde = -999f;
            duracao = 0f;
        }
    }
    private final List<Estouro> estouros = new ArrayList<>();

    /** Uma pincelada da tinta: o caminho (de onde ela entra até onde sai) e a grossura. */
    private static final class Pincelada {
        final float[] xs;
        final float[] ys;
        final float largura;
        /** Quando ela começa, na fração da animação toda (0..1). */
        final float atraso;
        /** Gotas que respingam quando a ponta passa: fração do caminho, desvio para o lado e raio. */
        final float[] gotas;

        Pincelada(float[] xs, float[] ys, float largura, float atraso, float[] gotas) {
            this.xs = xs;
            this.ys = ys;
            this.largura = largura;
            this.atraso = atraso;
            this.gotas = gotas;
        }
    }

    /** Parte da animação que cada pincelada leva para atravessar a tela. */
    static final float PINCELADA = 0.55f;
    private enum ModoTinta { NADA, COBRINDO, DESCOBRINDO }
    private ModoTinta tintaModo = ModoTinta.NADA;
    private float tintaDesde;
    private float tintaDuracao = 1f;
    private int tintaCor = WHITE;
    /** A tela está toda pintada: a luta fica atrás de um fundo liso da cor da tinta. */
    private boolean coberta;
    private List<Pincelada> pinceladas = Collections.emptyList();
    private int pinturas;

    /** A página sorteada de cada ação PAGINA (escolhida ao carregar a luta). */
    private final Map<Acao, RenderImage> paginas = new HashMap<>();
    private RenderImage pagina;
    private float paginaDesde;
    private boolean paginaPulada;

    /** O QTE da ação atual: em que fase está, o que o jogador já fez e o resultado. */
    private enum FaseQte { PEDINDO, RESULTADO, GOLPE }
    private Acao qte;
    private FaseQte qteFase;
    private float qteDesde;
    private boolean qteAcertou;
    private int qteToques;
    private float qteUltimoToque = -999f;
    /** DESLIZAR: a direção pedida na tela (-1, 0 ou 1 em cada eixo) e onde o dedo desceu. */
    private int qteDx;
    private int qteDy;
    private boolean dedoNaTela;
    private float dedoX0;
    private float dedoY0;
    private float dedoX;
    private float dedoY;
    /** De que lado é quem bate agora (o acerto: o jogador; o erro: o rival). */
    private int qteBatedor;
    private int qteTentativas;
    /** O jogador está no toque (as dicas dizem TOQUE) ou no teclado/controle (APERTE). */
    private boolean dicasDeToque = true;

    /**
     * {@code conversa}: os quadros laterais onde as falas aparecem (pode ser null: a cena roda
     * sem falas). {@code palco}: quem mexe na luta.
     */
    public CinematicaLuta(RenderAssets assets, ConversaLuta conversa, Palco palco) {
        this.assets = assets;
        this.conversa = conversa;
        this.palco = palco;
    }

    /**
     * Carrega a cinemática de uma luta ({@code arquivo} dentro de {@code historia/}) e a deixa
     * esperando o gatilho. {@code irmao}: o irmão do jogador (fala no quadro da esquerda; o
     * outro, no da direita). Falso se o arquivo não abre: a luta segue sem cinemática.
     */
    public boolean carregar(Temporada temporada, String arquivo, String irmao) {
        descarregar();
        try {
            roteiro = ler(temporada, assets.readText(Temporada.PASTA + "/" + arquivo));
        } catch (IOException | RuntimeException ex) {
            descarregar();
            return false;
        }
        this.temporada = temporada;
        this.irmao = Torneio.KOGA.equals(irmao) ? Torneio.KOGA : Torneio.RYUGA;
        // A página de cada ação já abre agora (não na hora da cena: a imagem é grande).
        Random sorteio = new Random();
        for (Acao a : roteiro.acoes) {
            if (a.tipo != Acao.Tipo.PAGINA) continue;
            List<String> opcoes = new ArrayList<>(a.imagens);
            while (!opcoes.isEmpty()) {
                String escolhida = opcoes.remove(sorteio.nextInt(opcoes.size()));
                try {
                    RenderImage img = assets.loadImage(Temporada.PASTA + "/" + escolhida);
                    if (img != null) {
                        paginas.put(a, img);
                        break;
                    }
                } catch (IOException | RuntimeException ex) {
                    // Essa não abriu: sorteia outra (sem nenhuma, a cena pula a página).
                }
            }
        }
        estado = Estado.ARMADA;
        return true;
    }

    /** Esquece a cinemática e tudo o que ela desenha (fim da luta, luta nova). */
    public void descarregar() {
        roteiro = null;
        estado = Estado.NENHUMA;
        acao = null;
        indice = 0;
        travado = false;
        faixas.zerar();
        faixaAltura = FAIXA_ALTURA;
        zoom.zerar();
        focoJogador.zerar();
        focoRival.zerar();
        velocidade.zerar();
        estouros.clear();
        tintaModo = ModoTinta.NADA;
        coberta = false;
        pinceladas = Collections.emptyList();
        paginas.clear();
        pagina = null;
        qte = null;
        dedoNaTela = false;
    }

    // ------------------------------------------------------------ leitura

    /** Lê e valida o roteiro (com a temporada, confere quem fala contra o elenco). */
    public static Roteiro ler(Temporada temporada, String json) {
        Map<String, Object> root = Episodio.asMap(MiniJson.parse(json));
        Gatilho gatilho = gatilho(root.get("gatilho"));
        List<Acao> acoes = new ArrayList<>();
        int n = 0;
        boolean travado = false;
        for (Object item : Episodio.list(root, "acoes")) {
            n++;
            try {
                Acao a = acao(temporada, Episodio.asMap(item));
                if (a.tipo == Acao.Tipo.CONTROLE) travado = a.ligar;
                // Quem bate no QTE é o roteiro: o pad e a IA precisam estar de fora.
                if (a.tipo == Acao.Tipo.QTE && !travado) {
                    throw new IllegalArgumentException("qte com o controle solto (antes: {\"acao\": \"controle\", \"travar\": true})");
                }
                acoes.add(a);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("ação " + n + ": " + ex.getMessage(), ex);
            }
        }
        if (acoes.isEmpty()) throw new IllegalArgumentException("cinemática sem ações");
        return new Roteiro(gatilho, Collections.unmodifiableList(acoes));
    }

    private static Gatilho gatilho(Object json) {
        if (json == null) throw new IllegalArgumentException("cinemática sem gatilho");
        if (json instanceof String) {
            if (json.equals("inicio")) return new Gatilho(Gatilho.Tipo.INICIO, 0f, -1);
            if (json.equals("ko")) return new Gatilho(Gatilho.Tipo.KO, 0f, -1);
            throw new IllegalArgumentException("gatilho " + json + " (use \"inicio\", \"ko\", {\"vida\": 0.25} ou {\"tempo\": 20})");
        }
        Map<String, Object> m = Episodio.asMap(json);
        if (m.get("vida") instanceof Number) {
            float vida = ((Number)m.get("vida")).floatValue();
            if (vida <= 0f || vida >= 1f) throw new IllegalArgumentException("gatilho vida precisa ficar entre 0 e 1: " + vida);
            String lado = Episodio.string(m, "lado", "qualquer");
            int quem;
            if (lado.equals("qualquer")) quem = -1;
            else if (lado.equals("jogador")) quem = JOGADOR;
            else if (lado.equals("rival")) quem = RIVAL;
            else throw new IllegalArgumentException("lado do gatilho " + lado + " (use qualquer, jogador ou rival)");
            return new Gatilho(Gatilho.Tipo.VIDA, vida, quem);
        }
        if (m.get("tempo") instanceof Number) {
            float tempo = ((Number)m.get("tempo")).floatValue();
            if (tempo < 0f) throw new IllegalArgumentException("gatilho tempo negativo");
            return new Gatilho(Gatilho.Tipo.TEMPO, tempo, -1);
        }
        throw new IllegalArgumentException("gatilho sem \"vida\" nem \"tempo\"");
    }

    private static Acao acao(Temporada temporada, Map<String, Object> m) {
        String nome = Episodio.string(m, "acao", null);
        if (nome == null) throw new IllegalArgumentException("sem \"acao\"");
        Acao a;
        switch (nome) {
            case "faixas":
                a = new Acao(Acao.Tipo.FAIXAS);
                a.ligar = booleano(m, "entrar");
                a.altura = numero(m, "altura", FAIXA_ALTURA, 0f, VH * 0.3f);
                a.segundos = numero(m, "segundos", FAIXA_DURACAO, 0f, 5f);
                break;
            case "controle":
                a = new Acao(Acao.Tipo.CONTROLE);
                a.ligar = booleano(m, "travar");
                break;
            case "mover":
                a = new Acao(Acao.Tipo.MOVER);
                a.quemMove = Episodio.string(m, "quem", "ambos");
                if (!a.quemMove.equals("ambos") && !time(a.quemMove)) {
                    throw new IllegalArgumentException("quem se move: " + a.quemMove + " (use jogador, rival, ambos, ryuga ou koga)");
                }
                a.movimento = movimento(m);
                break;
            case "fala":
                a = new Acao(Acao.Tipo.FALA);
                a.quem = Episodio.string(m, "quem", null);
                if (a.quem == null) throw new IllegalArgumentException("fala sem \"quem\"");
                if (temporada != null && !temporada.elenco.containsKey(a.quem)) {
                    throw new IllegalArgumentException("fora do elenco da temporada: " + a.quem);
                }
                a.texto = texto(m);
                a.expressao = Episodio.string(m, "expressao", "normal");
                if (!a.expressao.matches("[a-z]+")) throw new IllegalArgumentException("expressão " + a.expressao);
                String lado = Episodio.string(m, "lado", null);
                if (lado == null && time(a.quem)) {
                    // Os irmãos falam do lado da tela onde está o lutador do time deles.
                    a.time = a.quem;
                } else if (lado == null) {
                    throw new IllegalArgumentException(a.quem + " fala sem \"lado\" (esquerda, direita, jogador ou rival)");
                } else if (lado.equals("esquerda") || lado.equals("direita")) {
                    a.naDireita = lado.equals("direita");
                } else if (lado.equals("jogador") || lado.equals("rival")) {
                    a.time = lado;
                } else {
                    throw new IllegalArgumentException("lado " + lado + " (use esquerda, direita, jogador ou rival)");
                }
                a.segura = numero(m, "segura", -1f, -1f, 10f);
                break;
            case "onomatopeia":
                a = new Acao(Acao.Tipo.ONOMATOPEIA);
                a.texto = texto(m);
                List<Object> pos = Episodio.list(m, "pos");
                if (!pos.isEmpty()) {
                    if (pos.size() != 2 || !(pos.get(0) instanceof Number) || !(pos.get(1) instanceof Number)) {
                        throw new IllegalArgumentException("pos precisa ser [x, y]");
                    }
                    a.x = ((Number)pos.get(0)).floatValue();
                    a.y = ((Number)pos.get(1)).floatValue();
                    if (a.x < 0f || a.x > 1f || a.y < 0f || a.y > 1f) throw new IllegalArgumentException("pos fora da tela");
                }
                a.tamanho = numero(m, "tamanho", a.tamanho, 20f, 400f);
                a.tremor = numero(m, "tremor", a.tremor, 0f, 60f);
                a.rotacao = numero(m, "rotacao", a.rotacao, -90f, 90f);
                a.segundos = numero(m, "segundos", 1.6f, 0.2f, 10f);
                String cor = Episodio.string(m, "cor", null);
                if (cor != null) a.cor = Temporada.parseColor(cor);
                break;
            case "esperar":
                a = new Acao(Acao.Tipo.ESPERAR);
                a.segundos = numero(m, "segundos", -1f, 0f, 30f);
                if (a.segundos < 0f) throw new IllegalArgumentException("esperar sem \"segundos\"");
                break;
            case "camera":
                a = new Acao(Acao.Tipo.CAMERA);
                a.zoom = numero(m, "zoom", 1f, ZOOM_MIN, ZOOM_MAX);
                a.segundos = numero(m, "segundos", 0.5f, 0f, 10f);
                a.foco = Episodio.string(m, "foco", null);
                if (a.foco != null && !a.foco.equals("centro") && !time(a.foco)) {
                    throw new IllegalArgumentException("foco " + a.foco + " (use centro, jogador, rival, ryuga ou koga)");
                }
                break;
            case "velocidade":
                a = new Acao(Acao.Tipo.VELOCIDADE);
                Object v = m.get("valor");
                if (!(v instanceof Number)) throw new IllegalArgumentException("velocidade sem \"valor\" (1 = normal, 0,3 = lenta, 0 = congela)");
                a.valor = numero(m, "valor", 1f, 0f, 1f);
                a.segundos = numero(m, "segundos", 0.25f, 0f, 10f);
                break;
            case "terminarLuta":
                a = new Acao(Acao.Tipo.TERMINAR_LUTA);
                a.vencedor = Episodio.string(m, "vencedor", "interrompida");
                if (!a.vencedor.equals("jogador") && !a.vencedor.equals("rival") && !a.vencedor.equals("interrompida")) {
                    throw new IllegalArgumentException("vencedor " + a.vencedor + " (use jogador, rival ou interrompida)");
                }
                break;
            case "tinta":
                a = new Acao(Acao.Tipo.TINTA);
                a.ligar = booleano(m, "cobrir");
                a.segundos = numero(m, "segundos", 0.9f, 0.1f, 5f);
                a.cor = WHITE;
                String tinta = Episodio.string(m, "cor", null);
                if (tinta != null) a.cor = Temporada.parseColor(tinta);
                break;
            case "pagina":
                a = new Acao(Acao.Tipo.PAGINA);
                a.imagens = new ArrayList<>();
                String uma = Episodio.string(m, "imagem", null);
                if (uma != null) a.imagens.add(uma);
                for (Object img : Episodio.list(m, "imagens")) a.imagens.add(String.valueOf(img));
                if (a.imagens.isEmpty()) throw new IllegalArgumentException("pagina sem \"imagem\" nem \"imagens\"");
                a.segundos = numero(m, "segundos", 3f, 0.3f, 15f);
                break;
            case "qte":
                a = qte(m);
                break;
            default:
                throw new IllegalArgumentException("ação desconhecida: " + nome +
                    " (use faixas, controle, mover, fala, onomatopeia, esperar, camera, velocidade, terminarLuta, tinta, pagina, qte)");
        }
        // O QTE sempre espera o jogador.
        a.semEsperar = a.tipo != Acao.Tipo.QTE && Boolean.TRUE.equals(m.get("semEsperar"));
        return a;
    }

    private static Acao qte(Map<String, Object> m) {
        Acao a = new Acao(Acao.Tipo.QTE);
        String tipo = Episodio.string(m, "tipo", "toque");
        switch (tipo) {
            case "toque": a.qte = TipoQte.TOQUE; break;
            case "martelar": a.qte = TipoQte.MARTELAR; break;
            case "deslizar": a.qte = TipoQte.DESLIZAR; break;
            case "tempo": a.qte = TipoQte.TEMPO; break;
            default: throw new IllegalArgumentException("qte " + tipo + " (use toque, martelar, deslizar ou tempo)");
        }
        a.texto = texto(m);
        float padrao = a.qte == TipoQte.MARTELAR ? 2.6f : a.qte == TipoQte.TEMPO ? 1.3f : 1.8f;
        a.segundos = numero(m, "segundos", padrao, 0.4f, 10f);
        a.vezes = Math.round(numero(m, "vezes", 8f, 2f, 40f));
        a.direcao = Episodio.string(m, "direcao", "frente");
        if (!a.direcao.matches("frente|tras|cima|baixo")) {
            throw new IllegalArgumentException("direcao " + a.direcao + " (use frente, tras, cima ou baixo)");
        }
        a.quemMove = Episodio.string(m, "quem", "jogador");
        if (!time(a.quemMove)) throw new IllegalArgumentException("quem bate: " + a.quemMove + " (use jogador, rival, ryuga ou koga)");
        a.x = 0.5f;
        a.y = 0.6f;
        List<Object> pos = Episodio.list(m, "pos");
        if (!pos.isEmpty()) {
            if (pos.size() != 2 || !(pos.get(0) instanceof Number) || !(pos.get(1) instanceof Number)) {
                throw new IllegalArgumentException("pos precisa ser [x, y]");
            }
            a.x = ((Number)pos.get(0)).floatValue();
            a.y = ((Number)pos.get(1)).floatValue();
            if (a.x < 0f || a.x > 1f || a.y < 0f || a.y > 1f) throw new IllegalArgumentException("pos fora da tela");
        }
        boolean finaliza = Boolean.TRUE.equals(m.get("final"));
        a.acerto = golpe(m, "golpe", "H", finaliza);
        Object erro = m.get("erro");
        if (erro != null) a.erro = golpe(Episodio.asMap(erro), "golpe", "M", false);
        return a;
    }

    private static Golpe golpe(Map<String, Object> m, String chave, String padrao, boolean finaliza) {
        String g = Episodio.string(m, chave, padrao);
        if (!g.matches("2?[LMH]|S[1-9]?")) {
            throw new IllegalArgumentException("golpe " + g + " (use L, M, H, 2L, 2M, 2H ou um especial: S, S2, S3...)");
        }
        return new Golpe(g, numero(m, "dano", 0.15f, 0f, 1f), finaliza);
    }

    private static Movimento movimento(Map<String, Object> m) {
        String nome = Episodio.string(m, "movimento", null);
        Movimento.Tipo tipo;
        float segundos;
        if ("titular".equals(nome)) {
            tipo = Movimento.Tipo.TITULAR;
            segundos = 4f;
        } else if ("pular-tras".equals(nome)) {
            tipo = Movimento.Tipo.PULAR_TRAS;
            segundos = 2f;
        } else if ("recuar".equals(nome)) {
            tipo = Movimento.Tipo.RECUAR;
            segundos = 1f;
        } else if ("correr".equals(nome)) {
            tipo = Movimento.Tipo.CORRER;
            segundos = 3f;
        } else if ("andar".equals(nome)) {
            tipo = Movimento.Tipo.ANDAR;
            segundos = 3f;
        } else if ("parar".equals(nome)) {
            tipo = Movimento.Tipo.PARAR;
            segundos = 0f;
        } else {
            throw new IllegalArgumentException("movimento " + nome + " (use titular, pular-tras, recuar, correr, andar ou parar)");
        }
        return new Movimento(tipo, numero(m, "distancia", -1f, -1f, 2000f), numero(m, "segundos", segundos, 0f, 10f));
    }

    /** Nome de um time: "jogador", "rival" ou um irmão (o time dele depende da campanha). */
    private static boolean time(String quem) {
        return quem.equals("jogador") || quem.equals("rival") || quem.equals(Torneio.RYUGA) || quem.equals(Torneio.KOGA);
    }

    /** O lado ({@link #JOGADOR} ou {@link #RIVAL}) de um time; o irmão escolhido é o jogador. */
    private int lado(String time) {
        if (time.equals("jogador")) return JOGADOR;
        if (time.equals("rival")) return RIVAL;
        return time.equals(irmao) ? JOGADOR : RIVAL;
    }

    private int[] lados(String quem) {
        return quem.equals("ambos") ? new int[] {JOGADOR, RIVAL} : new int[] {lado(quem)};
    }

    private static String texto(Map<String, Object> m) {
        String texto = Episodio.string(m, "texto", "");
        if (texto.isEmpty()) throw new IllegalArgumentException("sem texto");
        return texto;
    }

    private static boolean booleano(Map<String, Object> m, String chave) {
        Object v = m.get(chave);
        if (!(v instanceof Boolean)) throw new IllegalArgumentException("precisa de \"" + chave + "\": true ou false");
        return (Boolean)v;
    }

    private static float numero(Map<String, Object> m, String chave, float padrao, float min, float max) {
        Object v = m.get(chave);
        if (v == null) return padrao;
        if (!(v instanceof Number)) throw new IllegalArgumentException(chave + " precisa ser um número");
        float f = ((Number)v).floatValue();
        if (f < min || f > max) throw new IllegalArgumentException(chave + " fora do intervalo " + min + " a " + max + ": " + f);
        return f;
    }

    // ------------------------------------------------------------ linha do tempo

    /** Há uma cinemática carregada nesta luta (esperando, rodando ou já tocada). */
    public boolean ativa() {
        return estado != Estado.NENHUMA;
    }

    /** Carregada e esperando o gatilho. */
    public boolean armada() {
        return estado == Estado.ARMADA;
    }

    /** A cena está rodando agora. */
    public boolean rodando() {
        return estado == Estado.RODANDO;
    }

    /** Já tocou (a luta seguiu ou foi encerrada por ela). */
    public boolean acabou() {
        return estado == Estado.ACABOU;
    }

    public Roteiro roteiro() {
        return roteiro;
    }

    /**
     * Ninguém pode cair agora: com o gatilho de vida armado (um golpe grande não pula a cena
     * indo direto ao KO) e durante a cena (um projétil perdido não derruba ninguém). O jogo
     * segura o último ponto de vida enquanto isso for verdade.
     */
    public boolean protegeVida() {
        if (roteiro == null || roteiro.gatilho.tipo == Gatilho.Tipo.KO) return false;
        return estado == Estado.RODANDO || (estado == Estado.ARMADA && roteiro.gatilho.tipo == Gatilho.Tipo.VIDA);
    }

    /** O fim da luta espera: a cena está rodando ou é a do KO e ainda não começou. */
    public boolean seguraFimDaLuta() {
        return estado == Estado.RODANDO || (estado == Estado.ARMADA && roteiro.gatilho.tipo == Gatilho.Tipo.KO);
    }

    /** O controle está com a cena (o jogador não joga e a IA não pensa). */
    public boolean controleTravado() {
        return travado;
    }

    /**
     * Confere o gatilho com o estado da luta e começa a cena se for a hora. {@code vidaJogador}
     * e {@code vidaRival}: fração da vida de quem está lutando de cada lado; {@code segundos}:
     * tempo de luta desde o "LUTEM!"; {@code ko}: a luta acabou. Verdadeiro se começou agora.
     */
    public boolean gatilho(float vidaJogador, float vidaRival, float segundos, boolean ko) {
        if (estado != Estado.ARMADA) return false;
        Gatilho g = roteiro.gatilho;
        boolean dispara;
        switch (g.tipo) {
            case INICIO:
                dispara = !ko;
                break;
            case TEMPO:
                dispara = !ko && segundos >= g.valor;
                break;
            case VIDA:
                dispara = !ko && ((g.lado != RIVAL && vidaJogador <= g.valor) || (g.lado != JOGADOR && vidaRival <= g.valor));
                break;
            default:
                dispara = ko;
                break;
        }
        if (dispara) comecar();
        return dispara;
    }

    /** Começa a cena agora, sem esperar o gatilho (o gatilho chama isto; os testes também). */
    public void comecar() {
        if (roteiro == null || estado == Estado.RODANDO) return;
        estado = Estado.RODANDO;
        indice = 0;
        acao = null;
        // Quem estava falando na luta se cala: a cena fala por cima.
        if (conversa != null) conversa.calar();
        avancar();
    }

    public void update(float dt) {
        time += dt;
        for (Iterator<Estouro> it = estouros.iterator(); it.hasNext(); ) {
            Estouro e = it.next();
            if (time - e.desde >= e.acao.segundos) it.remove();
        }
        if (tintaModo != ModoTinta.NADA && time - tintaDesde >= tintaDuracao) {
            // Terminou de cobrir: a tela é um fundo liso, e a página que estava por baixo some.
            if (tintaModo == ModoTinta.COBRINDO) {
                coberta = true;
                pagina = null;
            }
            tintaModo = ModoTinta.NADA;
        }
        if (estado == Estado.RODANDO) avancar();
    }

    /** Executa as ações até uma que precise esperar. */
    private void avancar() {
        int guarda = 0;
        while (estado == Estado.RODANDO && guarda++ < 256) {
            if (acao == null) {
                if (indice >= roteiro.acoes.size()) {
                    acabar();
                    return;
                }
                acao = roteiro.acoes.get(indice++);
                acaoDesde = time;
                executar(acao);
                if (estado != Estado.RODANDO) return;
            }
            if (!acao.semEsperar && !pronta(acao)) return;
            acao = null;
        }
    }

    private void executar(Acao a) {
        switch (a.tipo) {
            case FAIXAS:
                faixas.ir(a.ligar ? 1f : 0f, a.segundos);
                if (a.ligar) faixaAltura = a.altura;
                break;
            case CONTROLE:
                travado = a.ligar;
                palco.travarControle(a.ligar);
                break;
            case MOVER:
                for (int lado : lados(a.quemMove)) palco.mover(lado, a.movimento);
                break;
            case FALA:
                if (conversa != null) conversa.dizer(temporada, a.quem, naDireita(a), a.expressao, a.texto, a.segura);
                break;
            case ONOMATOPEIA:
                estouros.add(new Estouro(a, time));
                break;
            case CAMERA:
                zoom.ir(a.zoom, a.segundos);
                if (a.foco != null) {
                    int alvo = a.foco.equals("centro") ? -1 : lado(a.foco);
                    focoJogador.ir(alvo == JOGADOR ? 1f : 0f, a.segundos);
                    focoRival.ir(alvo == RIVAL ? 1f : 0f, a.segundos);
                }
                break;
            case VELOCIDADE:
                velocidade.ir(a.valor, a.segundos);
                break;
            case TERMINAR_LUTA:
                estado = Estado.ACABOU;
                travado = false;
                palco.terminarLuta(a.vencedor);
                break;
            case TINTA:
                pintar(a.ligar, a.segundos, a.cor);
                break;
            case PAGINA:
                pagina = paginas.get(a);
                paginaDesde = time;
                paginaPulada = false;
                break;
            case QTE:
                qte = a;
                qteTentativas = 0;
                pedirQte();
                break;
            default:
                break;
        }
    }

    private boolean pronta(Acao a) {
        float idade = time - acaoDesde;
        switch (a.tipo) {
            case FAIXAS:
                return idade >= a.segundos;
            case MOVER:
                if (idade >= LIMITE_ESPERA) return true;
                for (int lado : lados(a.quemMove)) {
                    if (palco.movendo(lado)) return false;
                }
                return true;
            case FALA:
                return conversa == null || !conversa.falando() || idade >= LIMITE_ESPERA;
            case ONOMATOPEIA:
            case ESPERAR:
            case CAMERA:
            case VELOCIDADE:
            case TINTA:
                return idade >= a.segundos;
            case PAGINA:
                return pagina == null || paginaPulada || idade >= a.segundos;
            case QTE:
                return qtePronto();
            default:
                return true;
        }
    }

    // ------------------------------------------------------------ tinta

    /** Começa a pintar a tela (cobrir) ou a tirar a tinta dela (descobrir). */
    private void pintar(boolean cobrir, float segundos, int cor) {
        tintaModo = cobrir ? ModoTinta.COBRINDO : ModoTinta.DESCOBRINDO;
        tintaDesde = time;
        tintaDuracao = segundos;
        if (cobrir) {
            tintaCor = cor;
            pinceladas = pinceladas(pinturas++);
        } else {
            coberta = false;
            pagina = null;
            if (pinceladas.isEmpty()) pinceladas = pinceladas(pinturas++);
        }
    }

    /**
     * Seis pinceladas largas, uma embaixo da outra, cada uma atravessando a tela inteira (de um
     * lado para o outro, alternando), um pouco tortas e onduladas. Grossas o bastante para,
     * juntas, cobrirem a tela toda.
     */
    static List<Pincelada> pinceladas(int semente) {
        Random r = new Random(1234L + semente * 7919L);
        int faixas = 6;
        float passo = (VH + 60f) / faixas;
        List<Pincelada> out = new ArrayList<>();
        for (int i = 0; i < faixas; i++) {
            boolean paraDireita = (i % 2 == 0) == (semente % 2 == 0);
            float yc = -30f + passo * (i + 0.5f);
            float inclinacao = (r.nextFloat() * 2f - 1f) * 22f;
            float fase = r.nextFloat() * 6.28f;
            int pontos = 12;
            float[] xs = new float[pontos];
            float[] ys = new float[pontos];
            for (int k = 0; k < pontos; k++) {
                float u = k / (float)(pontos - 1);
                float x = -220f + (VW + 440f) * u;
                xs[k] = paraDireita ? x : VW - x;
                ys[k] = yc + inclinacao * (u * 2f - 1f) + (float)Math.sin(fase + u * 7f) * 9f;
            }
            float largura = 236f + r.nextFloat() * 24f;
            float atraso = i * 0.075f + r.nextFloat() * 0.03f;
            int n = 3 + r.nextInt(3);
            float[] gotas = new float[n * 3];
            for (int g = 0; g < n; g++) {
                gotas[g * 3] = 0.15f + 0.8f * r.nextFloat();
                gotas[g * 3 + 1] = (r.nextBoolean() ? 1f : -1f) * (largura * 0.5f + 10f + r.nextFloat() * 40f);
                gotas[g * 3 + 2] = 4f + r.nextFloat() * 11f;
            }
            out.add(new Pincelada(xs, ys, largura, atraso, gotas));
        }
        return out;
    }

    /** A tela está toda pintada (a luta não aparece). */
    public boolean telaCoberta() {
        return coberta;
    }

    // ------------------------------------------------------------ QTE

    /** O QTE pede de novo (o primeiro pedido ou depois de um erro). */
    private void pedirQte() {
        qteFase = FaseQte.PEDINDO;
        qteDesde = time;
        qteToques = 0;
        qteAcertou = false;
        dedoNaTela = false;
        // "frente" é na direção do rival na tela, do lado de quem bate.
        int frente = palco.aDireita(lado(qte.quemMove)) ? -1 : 1;
        qteDx = qte.direcao.equals("frente") ? frente : qte.direcao.equals("tras") ? -frente : 0;
        qteDy = qte.direcao.equals("cima") ? -1 : qte.direcao.equals("baixo") ? 1 : 0;
    }

    private float limiteQte(Acao a) {
        return a.qte == TipoQte.TEMPO ? a.segundos + JANELA_TEMPO : a.segundos;
    }

    private void resolverQte(boolean acertou) {
        if (qte == null || qteFase != FaseQte.PEDINDO) return;
        qteAcertou = acertou;
        qteFase = FaseQte.RESULTADO;
        qteDesde = time;
    }

    /**
     * Pedindo: espera o jogador (ou o tempo acabar, que é erro). Resultado: mostra BOA!/ERROU!
     * e solta o golpe (o jogador bate no acerto; no erro, o rival bate nele). Golpe: espera o
     * golpe acabar; no acerto a ação termina, no erro o QTE pede de novo.
     */
    private boolean qtePronto() {
        float idade = time - qteDesde;
        switch (qteFase) {
            case PEDINDO:
                if (idade >= limiteQte(qte)) resolverQte(false);
                return false;
            case RESULTADO:
                if (idade < RESULTADO_QTE) return false;
                int quemBate = lado(qte.quemMove);
                qteBatedor = qteAcertou ? quemBate : 1 - quemBate;
                Golpe g = qteAcertou ? qte.acerto : qte.erro;
                qteFase = FaseQte.GOLPE;
                qteDesde = time;
                if (g != null) palco.golpear(qteBatedor, g);
                return false;
            default:
                if (palco.golpeando(qteBatedor) && idade < LIMITE_ESPERA) return false;
                if (qteAcertou) return true;
                qteTentativas++;
                pedirQte();
                return false;
        }
    }

    /** A cena quer os toques e os botões agora (um QTE na tela, uma página que pode ser pulada). */
    public boolean pegaToque() {
        if (estado != Estado.RODANDO || acao == null) return false;
        return acao.tipo == Acao.Tipo.QTE || (acao.tipo == Acao.Tipo.PAGINA && pagina != null);
    }

    /** Um QTE está esperando o jogador agora. */
    public boolean qtePedindo() {
        return pegaToque() && acao.tipo == Acao.Tipo.QTE && qteFase == FaseQte.PEDINDO;
    }

    /** Quantas vezes o jogador já errou o QTE atual. */
    public int qteErros() {
        return qteTentativas;
    }

    /** O dedo na tela ({@link #TOQUE_DESCE}, {@link #TOQUE_MOVE}, {@link #TOQUE_SOBE}), na tela 1280x720. */
    public void toque(int fase, float x, float y) {
        if (!pegaToque()) return;
        if (acao.tipo == Acao.Tipo.PAGINA) {
            if (fase == TOQUE_DESCE) pularPagina();
            return;
        }
        if (qteFase != FaseQte.PEDINDO) return;
        if (fase == TOQUE_DESCE) {
            dedoNaTela = true;
            dedoX0 = dedoX = x;
            dedoY0 = dedoY = y;
            if (qte.qte != TipoQte.DESLIZAR) apertar();
        } else if (fase == TOQUE_MOVE) {
            if (!dedoNaTela) return;
            dedoX = x;
            dedoY = y;
            if (qte.qte == TipoQte.DESLIZAR) conferirDeslize(x - dedoX0, y - dedoY0);
        } else {
            dedoNaTela = false;
        }
    }

    /** Um botão do teclado ou do controle (qualquer um vale como o toque). */
    public void botao() {
        if (!pegaToque()) return;
        if (acao.tipo == Acao.Tipo.PAGINA) {
            pularPagina();
        } else if (qteFase == FaseQte.PEDINDO && qte.qte != TipoQte.DESLIZAR) {
            apertar();
        }
    }

    /** Uma direção do teclado ou do controle ({@code dx}, {@code dy}: -1, 0 ou 1, na tela). */
    public void direcao(int dx, int dy) {
        if (!qtePedindo() || qte.qte != TipoQte.DESLIZAR) return;
        int ao = dx * qteDx + dy * qteDy;
        if (ao > 0) resolverQte(true);
        else if (ao < 0) resolverQte(false);
    }

    /** As dicas embaixo do botão falam do toque (TOQUE!) ou do teclado e controle (APERTE!). */
    public void setToque(boolean toque) {
        dicasDeToque = toque;
    }

    private void pularPagina() {
        if (time - paginaDesde >= PAGINA_PULAR) paginaPulada = true;
    }

    private void apertar() {
        switch (qte.qte) {
            case TOQUE:
                resolverQte(true);
                break;
            case MARTELAR:
                qteToques++;
                qteUltimoToque = time;
                if (qteToques >= qte.vezes) resolverQte(true);
                break;
            case TEMPO:
                resolverQte(Math.abs(time - qteDesde - qte.segundos) <= JANELA_TEMPO);
                break;
            default:
                break;
        }
    }

    /** O dedo andou (dx, dy) desde que desceu: valeu na direção pedida, errou na contrária. */
    private void conferirDeslize(float dx, float dy) {
        float ao = dx * qteDx + dy * qteDy;
        float deLado = Math.abs(dx * qteDy) + Math.abs(dy * qteDx);
        if (ao >= DESLIZE && ao > deLado) resolverQte(true);
        else if (ao <= -DESLIZE && -ao > deLado) resolverQte(false);
    }

    /** Fim do roteiro sem terminar a luta: o controle volta, as faixas saem e a câmera volta. */
    private void acabar() {
        estado = Estado.ACABOU;
        acao = null;
        qte = null;
        // A tinta que ficou na tela sai sozinha.
        if (coberta || tintaModo == ModoTinta.COBRINDO) pintar(false, 0.6f, tintaCor);
        if (travado) {
            travado = false;
            palco.travarControle(false);
        }
        if (faixas.alvo() > 0f) faixas.ir(0f, FAIXA_DURACAO);
        if (zoom.alvo() != 1f) zoom.ir(1f, 0.4f);
        focoJogador.ir(0f, 0.4f);
        focoRival.ir(0f, 0.4f);
        velocidade.zerar();
    }

    /** A fala vai para a direita: o lado fixo do arquivo ou o lado da tela do lutador do time. */
    private boolean naDireita(Acao a) {
        return a.naDireita != null ? a.naDireita : palco.aDireita(lado(a.time));
    }

    /** Quanto as faixas entraram (0 = fora, 1 = inteiras). */
    public float faixas() {
        return faixas.valor();
    }

    /** Zoom extra da câmera pedido pela cena (1 = o da luta). */
    public float zoom() {
        return zoom.valor();
    }

    /**
     * Foco da câmera em cada lutador: {@code [jogador, rival]}, de 0 (a câmera fica no centro
     * da luta) a 1 (centrada nele). O jogo leva o centro da câmera até lá.
     */
    public float foco(int lado) {
        return lado == JOGADOR ? focoJogador.valor() : focoRival.valor();
    }

    /**
     * Velocidade da luta pedida pela cena: 1 = normal, menos que 1 = câmera lenta, 0 = a luta
     * congela. A cena, as falas e as onomatopeias andam no tempo normal.
     */
    public float velocidade() {
        return estado == Estado.RODANDO ? velocidade.valor() : 1f;
    }

    private static float suave(float k) {
        return k * k * (3f - 2f * k);
    }

    /** Onomatopeias na tela agora (para os testes). */
    int estouros() {
        return estouros.size();
    }

    // ------------------------------------------------------------ desenho

    /** Faixas, onomatopeias, QTE, tinta e página (sem quadros de conversa entre eles). */
    public void render(RenderCanvas c) {
        renderFaixas(c);
        renderEfeitos(c);
        renderTela(c);
    }

    /** As faixas pretas: por cima da luta e do HUD, por baixo dos quadros da conversa. */
    public void renderFaixas(RenderCanvas c) {
        float k = faixas();
        if (k <= 0.001f) return;
        float h = faixaAltura * k;
        c.fillRect(-40f, -40f, VW + 40f, h, 0xFF000000);
        c.fillRect(-40f, VH - h, VW + 40f, VH + 40f, 0xFF000000);
    }

    /** As onomatopeias, por cima de tudo. */
    public void renderEfeitos(RenderCanvas c) {
        for (Estouro e : estouros) drawOnomatopeia(c, e);
    }

    private void drawOnomatopeia(RenderCanvas c, Estouro e) {
        Acao a = e.acao;
        float idade = time - e.desde;
        float resta = a.segundos - idade;
        float alpha = resta < SAIDA_ONOMATOPEIA ? Tinta.clamp01(resta / SAIDA_ONOMATOPEIA) : 1f;
        if (alpha <= 0f) return;
        String texto = temporada != null ? temporada.t(a.texto) : a.texto;
        if (!texto.equals(e.textoDividido)) {
            e.textoDividido = texto;
            e.palavras = texto.trim().split(" +");
        }
        String[] palavras = e.palavras;
        float cx = a.x * VW;
        float cy = a.y * VH;
        float size = a.tamanho;

        // Linhas de som estourando do centro, no primeiro estalo.
        float burst = Tinta.clamp01(idade / 0.4f);
        if (burst < 1f) {
            int linhas = 22;
            int cor = Tinta.withAlpha(WHITE, (int)(230f * (1f - burst) * alpha));
            int sombra = Tinta.withAlpha(INK, (int)(200f * (1f - burst) * alpha));
            for (int i = 0; i < linhas; i++) {
                double ang = Math.PI * 2 * i / linhas + 0.13 * (i % 3);
                float cos = (float)Math.cos(ang), sin = (float)Math.sin(ang);
                float r0 = size * (0.9f + 1.3f * Tinta.easeOutCubic(burst));
                float r1 = r0 + size * (0.5f + 0.35f * (i % 2));
                c.drawLine(cx + cos * r0, cy + sin * r0 * 0.7f, cx + cos * r1, cy + sin * r1 * 0.7f, 10f, sombra);
                c.drawLine(cx + cos * r0, cy + sin * r0 * 0.7f, cx + cos * r1, cy + sin * r1 * 0.7f, 5f, cor);
            }
        }

        int n = palavras.length;
        for (int i = 0; i < n; i++) {
            float t = idade - i * PALAVRA_INTERVALO;
            if (t < 0f) continue;
            float pop = Tinta.clamp01(t / POP);
            // Bate grande e assenta (passa um pouco do ponto, como o balão que estala).
            float escala = 2.1f - 1.1f * Tinta.easeOutBack(pop);
            float desloca = i - (n - 1) * 0.5f;
            // Em escada: cada palavra mais para a direita e para baixo.
            float px = cx + desloca * size * 0.9f;
            float py = cy + desloca * size * 0.85f;
            float tremor = a.tremor * (0.35f + 0.65f * (1f - Tinta.clamp01(t / 0.6f)));
            float dx = (float)Math.sin(time * 61f + i * 1.7f) * tremor;
            float dy = (float)Math.cos(time * 53f + i * 2.3f) * tremor;
            float rot = a.rotacao + (n > 1 ? (i % 2 == 0 ? -5f : 5f) : 0f);
            int vis = (int)(255f * alpha * Tinta.clamp01(pop * 3f));
            c.save();
            c.translate(px + dx, py + dy);
            c.rotate(rot, 0f, 0f);
            c.scale(escala, escala, 0f, 0f);
            letreiro(c, palavras[i], 0f, 0f, size, a.cor, vis);
            c.restore();
        }
    }

    /** Sombra, contorno branco por fora e preto por dentro: o letreiro de mangá. */
    private static void letreiro(RenderCanvas c, String p, float x, float y, float size, int cor, int vis) {
        c.drawText(p, x + size * 0.05f, y + size * 0.07f, size, Tinta.withAlpha(INK, vis * 3 / 4), Tinta.withAlpha(INK, vis * 3 / 4), size * 0.30f, true);
        c.drawText(p, x, y, size, Tinta.withAlpha(WHITE, vis), Tinta.withAlpha(WHITE, vis), size * 0.34f, true);
        c.drawText(p, x, y, size, Tinta.withAlpha(cor, vis), Tinta.withAlpha(INK, vis), size * 0.17f, true);
    }

    /**
     * O que cobre a luta, por cima de tudo (HUD, quadros, onomatopeias): o QTE, a tinta e a
     * página de mangá. Com a tela coberta, um fundo liso da cor da tinta esconde a luta.
     */
    public void renderTela(RenderCanvas c) {
        if (qte != null && acao == qte) desenharQte(c);
        if (coberta) c.fillRect(-40f, -40f, VW + 40f, VH + 40f, tintaCor);
        if (pagina != null) desenharPagina(c);
        if (tintaModo == ModoTinta.NADA) return;
        float k = Tinta.clamp01((time - tintaDesde) / tintaDuracao);
        float ultimo = 0f;
        for (Pincelada p : pinceladas) ultimo = Math.max(ultimo, p.atraso);
        for (Pincelada p : pinceladas) {
            if (tintaModo == ModoTinta.COBRINDO) {
                // A ponta do pincel atravessa a tela; quem começa antes fica embaixo.
                float ate = Tinta.easeOutCubic(Tinta.clamp01((k - p.atraso) / PINCELADA));
                desenharPincelada(c, p, 0f, ate, true);
            } else {
                // A tinta sai na ordem contrária: o rabo da pincelada corre atrás da ponta.
                float de = Tinta.easeOutCubic(Tinta.clamp01((k - (ultimo - p.atraso)) / PINCELADA));
                desenharPincelada(c, p, de, 1f, false);
            }
        }
    }

    /** O trecho {@code de..ate} (frações do caminho) de uma pincelada, com a borda e os fios do pincel. */
    private void desenharPincelada(RenderCanvas c, Pincelada p, float de, float ate, boolean ponta) {
        if (ate - de <= 0.001f) return;
        int borda = Tinta.darken(tintaCor, 0.13f);
        int fio = Tinta.withAlpha(Tinta.darken(tintaCor, 0.07f), 200);
        caminho(c, p, de, ate, 0f, p.largura + 14f, borda);
        caminho(c, p, de, ate, 0f, p.largura, tintaCor);
        // Fios do pincel seco ao longo da pincelada.
        for (int i = -2; i <= 2; i++) {
            if (i == 0) continue;
            caminho(c, p, de, ate, i * p.largura * 0.19f + (i % 2) * 7f, 3f, fio);
        }
        // Respingos onde a ponta já passou.
        for (int g = 0; g < p.gotas.length; g += 3) {
            float f = p.gotas[g];
            if (f < de || f > ate) continue;
            float[] xy = ponto(p, f);
            float r = p.gotas[g + 2];
            c.fillCircle(xy[0], xy[1] + p.gotas[g + 1], r + 2f, borda);
            c.fillCircle(xy[0], xy[1] + p.gotas[g + 1], r, tintaCor);
        }
        if (ponta && ate < 1f) {
            // A ponta: cerdas mais grossas e manchas saindo na frente.
            float[] xy = ponto(p, ate);
            for (int i = -3; i <= 3; i++) {
                float y = xy[1] + i * p.largura * 0.13f;
                c.fillCircle(xy[0] + (i % 2 == 0 ? 14f : -6f) * Math.signum(p.xs[p.xs.length - 1] - p.xs[0]), y, p.largura * 0.1f, tintaCor);
            }
        }
    }

    /** Desenha o caminho da pincelada entre as frações {@code de} e {@code ate}, deslocado {@code dy}. */
    private static void caminho(RenderCanvas c, Pincelada p, float de, float ate, float dy, float largura, int cor) {
        int n = p.xs.length - 1;
        float[] a = ponto(p, de);
        int k0 = Math.min(n - 1, (int)(de * n));
        for (int k = k0; k < n; k++) {
            float fim = (k + 1) / (float)n;
            float[] b = fim >= ate ? ponto(p, ate) : new float[] {p.xs[k + 1], p.ys[k + 1]};
            c.drawLine(a[0], a[1] + dy, b[0], b[1] + dy, largura, cor);
            if (fim >= ate) return;
            a = b;
        }
    }

    /** O ponto na fração {@code f} (0..1) do caminho da pincelada. */
    private static float[] ponto(Pincelada p, float f) {
        int n = p.xs.length - 1;
        float u = Tinta.clamp01(f) * n;
        int k = Math.min(n - 1, (int)u);
        float t = u - k;
        return new float[] {p.xs[k] + (p.xs[k + 1] - p.xs[k]) * t, p.ys[k] + (p.ys[k + 1] - p.ys[k]) * t};
    }

    /** A página de mangá: bate na tela (grande e torta, e assenta), com linhas de impacto atrás. */
    private void desenharPagina(RenderCanvas c) {
        float idade = time - paginaDesde;
        float h = VH - 56f;
        float w = h * pagina.width() / (float)Math.max(1, pagina.height());
        if (w > VW - 56f) {
            w = VW - 56f;
            h = w * pagina.height() / (float)Math.max(1, pagina.width());
        }
        float bate = Tinta.clamp01(idade / 0.28f);
        int alpha = (int)(255f * Tinta.clamp01(idade / 0.1f));
        float linhas = Tinta.clamp01(idade / 0.5f);
        if (linhas < 1f) {
            int cor = Tinta.withAlpha(INK, (int)(150f * (1f - linhas)));
            for (int i = 0; i < 28; i++) {
                double ang = Math.PI * 2 * i / 28 + 0.07 * (i % 3);
                float cos = (float)Math.cos(ang), sin = (float)Math.sin(ang);
                float r0 = 300f + 200f * Tinta.easeOutCubic(linhas);
                c.drawLine(VW / 2f + cos * r0, VH / 2f + sin * r0, VW / 2f + cos * (r0 + 520f), VH / 2f + sin * (r0 + 520f),
                    4f + 5f * (i % 2), cor);
            }
        }
        float escala = (1.25f - 0.25f * Tinta.easeOutBack(bate)) * (1f + 0.04f * Tinta.clamp01(idade / 4f));
        float rot = -4f + 3f * Tinta.easeOutCubic(bate);
        c.save();
        c.translate(VW / 2f, VH / 2f);
        c.rotate(rot, 0f, 0f);
        c.scale(escala, escala, 0f, 0f);
        c.fillRect(-w / 2f + 12f, -h / 2f + 14f, w / 2f + 12f, h / 2f + 14f, Tinta.withAlpha(INK, alpha / 4));
        c.fillRect(-w / 2f - 5f, -h / 2f - 5f, w / 2f + 5f, h / 2f + 5f, Tinta.withAlpha(INK, alpha));
        c.drawImage(pagina, -w / 2f, -h / 2f, w / 2f, h / 2f, alpha);
        c.restore();
    }

    private static int corQte(TipoQte tipo) {
        switch (tipo) {
            case MARTELAR: return 0xFFFF7A2F;
            case DESLIZAR: return 0xFF3FB6FF;
            case TEMPO: return 0xFFE84AFF;
            default: return 0xFFFFD23F;
        }
    }

    private String dica(TipoQte tipo) {
        switch (tipo) {
            case MARTELAR: return dicasDeToque ? "TOQUE RÁPIDO!" : "APERTE RÁPIDO!";
            case DESLIZAR: return dicasDeToque ? "DESLIZE!" : "DIRECIONAL!";
            case TEMPO: return "NA HORA CERTA!";
            default: return dicasDeToque ? "TOQUE!" : "APERTE!";
        }
    }

    private String t(String texto) {
        return temporada != null ? temporada.t(texto) : texto;
    }

    /**
     * O QTE: o nome do golpe em letreiro, o botão redondo com o anel do tempo (no de tempo, o
     * anel que fecha até o botão; no de martelar, o anel enche a cada toque), a dica embaixo e,
     * depois, o resultado.
     */
    private void desenharQte(RenderCanvas c) {
        Acao a = qte;
        float cx = a.x * VW, cy = a.y * VH;
        float idade = time - qteDesde;
        // Tempo desde o resultado (negativo: ainda pedindo).
        float desde = qteFase == FaseQte.PEDINDO ? -1f : qteFase == FaseQte.RESULTADO ? idade : RESULTADO_QTE + idade;
        float entrada = desde < 0f ? Tinta.clamp01(idade / 0.22f) : 1f;
        float sumir = desde < 0f ? 0f : Tinta.clamp01((desde - 0.25f) / 0.4f);
        float vis = entrada * (1f - sumir);
        if (vis > 0.001f) {
            int alpha = (int)(255f * vis);
            c.fillRect(-40f, -40f, VW + 40f, VH + 40f, Tinta.withAlpha(INK, (int)(80f * vis)));
            float escala = (0.4f + 0.6f * Tinta.easeOutBack(entrada)) * (1f + 0.25f * sumir);
            float tremor = desde >= 0f && !qteAcertou ? (float)Math.sin(time * 70f) * 12f * (1f - Tinta.clamp01(desde / 0.4f)) : 0f;
            float aperto = a.qte == TipoQte.MARTELAR ? 1f - 0.12f * (1f - Tinta.clamp01((time - qteUltimoToque) / 0.12f)) : 1f;
            int cor = desde >= 0f && !qteAcertou ? 0xFF9A3030 : corQte(a.qte);
            float r = BOTAO_RAIO;
            float pedido = desde < 0f ? Tinta.clamp01(idade / a.segundos) : 1f;

            c.save();
            c.translate(cx + tremor, cy);
            c.scale(escala, escala, 0f, 0f);
            // O nome do golpe em cima.
            letreiro(c, t(a.texto), 0f, -r - 62f, 58f, 0xFFFFD23F, alpha);
            // O anel de fora: o tempo que falta (no de martelar, quanto já encheu).
            if (desde < 0f && a.qte != TipoQte.TEMPO) {
                c.strokeCircle(0f, 0f, r + 22f, 13f, Tinta.withAlpha(INK, alpha * 2 / 3));
                if (a.qte == TipoQte.MARTELAR) {
                    arco(c, r + 22f, qteToques / (float)a.vezes, 8f, Tinta.withAlpha(0xFFFFD23F, alpha));
                } else {
                    int corAnel = pedido > 0.7f ? 0xFFFF4A3A : WHITE;
                    arco(c, r + 22f, 1f - pedido, 8f, Tinta.withAlpha(corAnel, alpha));
                }
            }
            c.scale(aperto, aperto, 0f, 0f);
            c.fillCircle(5f, 7f, r + 10f, Tinta.withAlpha(INK, alpha / 2));
            c.fillCircle(0f, 0f, r + 8f, Tinta.withAlpha(INK, alpha));
            c.fillCircle(0f, 0f, r + 2f, Tinta.withAlpha(WHITE, alpha));
            c.fillCircle(0f, 0f, r - 6f, Tinta.withAlpha(cor, alpha));
            c.fillCircle(-r * 0.25f, -r * 0.3f, r * 0.33f, Tinta.withAlpha(WHITE, alpha * 80 / 255));
            // O miolo: a seta do deslize, quantos toques faltam ou o "!".
            if (a.qte == TipoQte.DESLIZAR) {
                seta(c, qteDx, qteDy, r * 0.62f, Tinta.withAlpha(INK, alpha));
            } else {
                String miolo = a.qte == TipoQte.MARTELAR && desde < 0f ? String.valueOf(Math.max(0, a.vezes - qteToques)) : "!";
                c.drawText(miolo, 0f, r * 0.03f, r * 1.05f, Tinta.withAlpha(INK, alpha), Tinta.withAlpha(WHITE, alpha), 8f, false);
            }
            c.scale(1f / aperto, 1f / aperto, 0f, 0f);
            if (desde < 0f && a.qte == TipoQte.TEMPO) {
                // O anel que fecha: tocar quando ele chega no contorno do botão.
                boolean naHora = Math.abs(idade - a.segundos) <= JANELA_TEMPO;
                float alvo = r + 9f;
                c.strokeCircle(0f, 0f, alvo, 6f, Tinta.withAlpha(naHora ? 0xFF7CFF4F : WHITE, alpha));
                float rr = alvo + r * 2.4f * Math.max(0f, 1f - idade / a.segundos) - (idade > a.segundos ? (idade - a.segundos) * 60f : 0f);
                c.strokeCircle(0f, 0f, rr, 13f, Tinta.withAlpha(INK, alpha * 2 / 3));
                c.strokeCircle(0f, 0f, rr, 7f, Tinta.withAlpha(naHora ? 0xFF7CFF4F : 0xFFFFD23F, alpha));
            }
            if (desde < 0f && a.qte == TipoQte.DESLIZAR) {
                // Setas correndo para o lado pedido, mostrando o movimento.
                for (int i = 0; i < 3; i++) {
                    float f = ((time * 1.6f + i / 3f) % 1f);
                    float d = r + 40f + f * 120f;
                    int va = (int)(alpha * Math.sin(Math.PI * f));
                    c.save();
                    c.translate(qteDx * d, qteDy * d);
                    seta(c, qteDx, qteDy, 22f, Tinta.withAlpha(WHITE, va));
                    c.restore();
                }
            }
            if (desde < 0f) {
                c.drawText(t(dica(a.qte)), 0f, r + 58f, 28f, Tinta.withAlpha(WHITE, alpha), Tinta.withAlpha(INK, alpha), 7f, false);
                if (a.qte == TipoQte.MARTELAR) {
                    // No de martelar o tempo é a barrinha embaixo.
                    float bw = 170f;
                    c.fillRect(-bw / 2f - 3f, r + 80f, bw / 2f + 3f, r + 93f, Tinta.withAlpha(INK, alpha));
                    c.fillRect(-bw / 2f, r + 83f, -bw / 2f + bw * (1f - pedido), r + 90f,
                        Tinta.withAlpha(pedido > 0.7f ? 0xFFFF4A3A : WHITE, alpha));
                }
            }
            c.restore();
        }
        if (desde >= 0f) desenharResultado(c, cx, cy, desde);
    }

    /** BOA! (com o anel estourando) ou ERROU!, por cima do botão. */
    private void desenharResultado(RenderCanvas c, float cx, float cy, float desde) {
        if (desde >= 0.8f) return;
        int alpha = (int)(255f * (1f - Tinta.clamp01((desde - 0.55f) / 0.25f)));
        if (qteAcertou) {
            float k = Tinta.clamp01(desde / 0.35f);
            if (k < 1f) {
                float rr = BOTAO_RAIO + 170f * Tinta.easeOutCubic(k);
                c.strokeCircle(cx, cy, rr, 16f * (1f - k), Tinta.withAlpha(WHITE, (int)(255f * (1f - k))));
                for (int i = 0; i < 16; i++) {
                    double ang = Math.PI * 2 * i / 16;
                    float cos = (float)Math.cos(ang), sin = (float)Math.sin(ang);
                    float r0 = BOTAO_RAIO + 30f + 120f * Tinta.easeOutCubic(k);
                    c.drawLine(cx + cos * r0, cy + sin * r0, cx + cos * (r0 + 60f), cy + sin * (r0 + 60f), 6f,
                        Tinta.withAlpha(0xFFFFD23F, (int)(255f * (1f - k))));
                }
            }
        }
        float pop = Tinta.easeOutBack(Tinta.clamp01(desde / 0.16f));
        float s = 1.8f - 0.8f * pop;
        c.save();
        c.translate(cx, cy);
        c.rotate(qteAcertou ? -6f : 5f, 0f, 0f);
        c.scale(s, s, 0f, 0f);
        letreiro(c, t(qteAcertou ? "BOA!" : "ERROU!"), 0f, 0f, 76f, qteAcertou ? 0xFF7CFF4F : 0xFFFF4A3A, alpha);
        c.restore();
    }

    /** Arco de {@code frac} da volta (de cima, no sentido do relógio), feito de segmentos. */
    private static void arco(RenderCanvas c, float raio, float frac, float largura, int cor) {
        if (frac <= 0.001f) return;
        int n = Math.max(2, (int)(56 * frac));
        double a0 = -Math.PI / 2;
        float px = 0f, py = -raio;
        for (int i = 1; i <= n; i++) {
            double ang = a0 + Math.PI * 2 * frac * i / n;
            float x = (float)Math.cos(ang) * raio, y = (float)Math.sin(ang) * raio;
            c.drawLine(px, py, x, y, largura, cor);
            px = x;
            py = y;
        }
    }

    /** Uma seta cheia apontando para (dx, dy), centrada em (0, 0). */
    private static void seta(RenderCanvas c, int dx, int dy, float tamanho, int cor) {
        float ux = dx, uy = dy;
        float len = (float)Math.sqrt(ux * ux + uy * uy);
        if (len < 0.001f) return;
        ux /= len;
        uy /= len;
        float nx = -uy, ny = ux;
        float t = tamanho;
        float[] xy = {
            ux * t, uy * t,
            ux * 0.05f * t + nx * 0.8f * t, uy * 0.05f * t + ny * 0.8f * t,
            ux * 0.05f * t + nx * 0.32f * t, uy * 0.05f * t + ny * 0.32f * t,
            -ux * t + nx * 0.32f * t, -uy * t + ny * 0.32f * t,
            -ux * t - nx * 0.32f * t, -uy * t - ny * 0.32f * t,
            ux * 0.05f * t - nx * 0.32f * t, uy * 0.05f * t - ny * 0.32f * t,
            ux * 0.05f * t - nx * 0.8f * t, uy * 0.05f * t - ny * 0.8f * t,
        };
        c.fillPolygon(xy, cor);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "CinematicaLuta[%s, ação %d]", estado, indice);
    }
}
