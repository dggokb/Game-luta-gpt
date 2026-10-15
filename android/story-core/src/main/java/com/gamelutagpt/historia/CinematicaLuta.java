package com.gamelutagpt.historia;

import com.gamelutagpt.render.MiniJson;
import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderCanvas;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Cinemática dentro da luta: num momento certo (o gatilho: alguém com pouca vida, o começo, um
 * tempo de luta, o KO), a luta vira cena. Faixas pretas entram em cima e embaixo, o jogador
 * perde o controle e um roteiro guia os dois lutadores (pular para trás, correr um na direção
 * do outro, parar), põe falas nos quadros laterais ({@link ConversaLuta}), estoura
 * onomatopeias de mangá na tela, mexe na câmera e pode terminar a luta.
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
    }

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
        enum Tipo { FAIXAS, CONTROLE, MOVER, FALA, ONOMATOPEIA, ESPERAR, CAMERA, VELOCIDADE, TERMINAR_LUTA }

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
    }

    // ------------------------------------------------------------ leitura

    /** Lê e valida o roteiro (com a temporada, confere quem fala contra o elenco). */
    public static Roteiro ler(Temporada temporada, String json) {
        Map<String, Object> root = Episodio.asMap(MiniJson.parse(json));
        Gatilho gatilho = gatilho(root.get("gatilho"));
        List<Acao> acoes = new ArrayList<>();
        int n = 0;
        for (Object item : Episodio.list(root, "acoes")) {
            n++;
            try {
                acoes.add(acao(temporada, Episodio.asMap(item)));
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
            default:
                throw new IllegalArgumentException("ação desconhecida: " + nome +
                    " (use faixas, controle, mover, fala, onomatopeia, esperar, camera, velocidade, terminarLuta)");
        }
        a.semEsperar = Boolean.TRUE.equals(m.get("semEsperar"));
        return a;
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
                return idade >= a.segundos;
            default:
                return true;
        }
    }

    /** Fim do roteiro sem terminar a luta: o controle volta, as faixas saem e a câmera volta. */
    private void acabar() {
        estado = Estado.ACABOU;
        acao = null;
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

    /** Faixas e onomatopeias (sem quadros de conversa entre elas). */
    public void render(RenderCanvas c) {
        renderFaixas(c);
        renderEfeitos(c);
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
        String[] palavras = texto.trim().split(" +");
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
            String p = palavras[i];
            c.save();
            c.translate(px + dx, py + dy);
            c.rotate(rot, 0f, 0f);
            c.scale(escala, escala, 0f, 0f);
            // Sombra, contorno branco por fora e preto por dentro: o letreiro de mangá.
            c.drawText(p, size * 0.05f, size * 0.07f, size, Tinta.withAlpha(INK, vis * 3 / 4), Tinta.withAlpha(INK, vis * 3 / 4), size * 0.30f, true);
            c.drawText(p, 0f, 0f, size, Tinta.withAlpha(WHITE, vis), Tinta.withAlpha(WHITE, vis), size * 0.34f, true);
            c.drawText(p, 0f, 0f, size, Tinta.withAlpha(a.cor, vis), Tinta.withAlpha(INK, vis), size * 0.17f, true);
            c.restore();
        }
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "CinematicaLuta[%s, ação %d]", estado, indice);
    }
}
