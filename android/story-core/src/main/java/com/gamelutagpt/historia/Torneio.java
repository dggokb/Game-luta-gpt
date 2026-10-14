package com.gamelutagpt.historia;

import com.gamelutagpt.render.MiniJson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A chave do torneio do modo história: 8 duplas fixas, 4 por lado, com repescagem.
 *
 * <pre>
 * Em cada lado (o do Dragão à esquerda, o do Tigre à direita):
 *   quartas   A × B        C × D
 *   rodada 2  semifinal: vencedores das quartas    repescagem: perdedores das quartas
 *   rodada 3  final da chave: vencedor da semifinal × vencedor da repescagem
 * No meio:
 *   rodada 4  FINAL (2x2): o campeão de cada lado
 * </pre>
 *
 * Qualquer caminho até a final tem 3 lutas: quem vence as quartas faz semifinal e final da
 * chave; quem perde faz repescagem e final da chave. Por isso a derrota nas quartas não repete
 * a luta: manda para a repescagem.
 *
 * A dupla do Ryūga fica fixa na vaga 0 e a do Kōga na vaga 7 (lados opostos: só se encontram
 * na final). As outras são sorteadas nas vagas 1 a 6; dupla que ainda não luta
 * ({@link Dupla#pronta} falso) vai para o lado do rival. O rival sempre vence; as lutas entre
 * duas duplas do CPU têm o vencedor sorteado.
 */
public final class Torneio {
    public static final String ARQUIVO = Temporada.PASTA + "/torneio/torneio.json";
    public static final int VAGAS = 8;
    /** Quartas, semifinal/repescagem, final da chave e final. */
    public static final int RODADAS = 4;
    public static final String QUARTAS = "QUARTAS DE FINAL";
    public static final String SEMIFINAL = "SEMIFINAL";
    public static final String REPESCAGEM = "REPESCAGEM";
    public static final String FINAL_DA_CHAVE = "FINAL DA CHAVE";
    public static final String FINAL = "FINAL";
    public static final String[] NOMES_RODADAS = {QUARTAS, SEMIFINAL, REPESCAGEM, FINAL_DA_CHAVE, FINAL};
    public static final String RYUGA = "ryuga";
    public static final String KOGA = "koga";

    /** Uma dupla fixa da história. */
    public static final class Dupla {
        public final String id;
        /** Titular e parceiro (packs de luta). */
        public final String[] lutadores;
        /** Irmão capitão desta dupla ("ryuga" / "koga"), ou null. */
        public final String irmao;
        /** Falso enquanto um dos dois ainda não tem golpes (fica longe do jogador). */
        public final boolean pronta;

        public Dupla(String id, String[] lutadores, String irmao, boolean pronta) {
            if (lutadores == null || lutadores.length != 2) throw new IllegalArgumentException(id + ": dupla precisa de 2 lutadores");
            this.id = id;
            this.lutadores = lutadores.clone();
            this.irmao = irmao;
            this.pronta = pronta;
        }

        /** Código do retrato (historia/torneio/rostos/pNN.jpg) de cada lutador. */
        public String rosto(int i) {
            return Rostos.codigo(lutadores[i]);
        }
    }

    /** Um confronto da chave. */
    public static final class Confronto {
        public final int rodada;
        /** 0 = lado esquerdo (Dragão), 1 = lado direito (Tigre), -1 = a final. */
        public final int lado;
        public final String nome;
        public final Dupla a;
        public final Dupla b;
        private Dupla vencedor;

        Confronto(int rodada, int lado, String nome, Dupla a, Dupla b) {
            this.rodada = rodada;
            this.lado = lado;
            this.nome = nome;
            this.a = a;
            this.b = b;
        }

        public Dupla vencedor() {
            return vencedor;
        }

        public Dupla perdedor() {
            return vencedor == null ? null : vencedor == a ? b : a;
        }

        public boolean tem(Dupla d) {
            return a == d || b == d;
        }
    }

    /** As duplas de {@code torneio.json}, na ordem do arquivo. */
    public static List<Dupla> duplasFromJson(String json) {
        Map<String, Object> map = Episodio.asMap(MiniJson.parse(json));
        List<Dupla> out = new ArrayList<>();
        for (Object item : Episodio.list(map, "duplas")) {
            Map<String, Object> d = Episodio.asMap(item);
            List<Object> lutadores = Episodio.list(d, "lutadores");
            if (lutadores.size() != 2) throw new IllegalArgumentException("dupla precisa de \"lutadores\": [titular, parceiro]");
            String id = Episodio.string(d, "id", String.valueOf(lutadores.get(0)));
            out.add(new Dupla(id, new String[] {String.valueOf(lutadores.get(0)), String.valueOf(lutadores.get(1))},
                Episodio.string(d, "irmao", null), !Boolean.FALSE.equals(d.get("pronta"))));
        }
        return out;
    }

    private final Dupla[] vagas;
    private final Dupla jogador;
    private final Dupla rival;
    private final Random random;
    /** Ordem em que as vagas 1 a 6 foram sorteadas (para a animação do sorteio). */
    private final int[] ordemSorteio;
    /** Confrontos de cada rodada, criados quando a rodada anterior termina. */
    private final List<List<Confronto>> rodadas = new ArrayList<>();
    /** Rodadas já disputadas (0 a 4). */
    private int disputadas;
    private boolean eliminado;

    private Torneio(Dupla[] vagas, Dupla jogador, Dupla rival, int[] ordemSorteio, Random random) {
        this.vagas = vagas;
        this.jogador = jogador;
        this.rival = rival;
        this.ordemSorteio = ordemSorteio;
        this.random = random;
        List<Confronto> quartas = new ArrayList<>();
        for (int m = 0; m < VAGAS / 2; m++) quartas.add(new Confronto(0, m / 2, QUARTAS, vagas[2 * m], vagas[2 * m + 1]));
        rodadas.add(quartas);
    }

    /**
     * O sorteio: o Ryūga na vaga 0, o Kōga na 7, as outras 6 duplas embaralhadas nas vagas 1 a 6
     * (as que não estão prontas no lado do rival).
     *
     * @param irmao quem o jogador escolheu ({@link #RYUGA} ou {@link #KOGA})
     */
    public static Torneio sortear(List<Dupla> duplas, String irmao, Random random) {
        Dupla ryuga = null, koga = null;
        List<Dupla> prontas = new ArrayList<>();
        List<Dupla> semGolpes = new ArrayList<>();
        for (Dupla d : duplas) {
            if (RYUGA.equals(d.irmao)) ryuga = d;
            else if (KOGA.equals(d.irmao)) koga = d;
            else if (d.pronta) prontas.add(d);
            else semGolpes.add(d);
        }
        if (ryuga == null || koga == null) throw new IllegalArgumentException("o torneio precisa das duplas do Ryūga e do Kōga");
        if (prontas.size() + semGolpes.size() != VAGAS - 2) {
            throw new IllegalArgumentException("o torneio precisa de " + VAGAS + " duplas, tem " + duplas.size());
        }
        boolean jogaRyuga = !KOGA.equals(irmao);
        // Lado do rival: vagas 4-6 se o jogador é o Ryūga, 1-3 se é o Kōga.
        int[] ladoJogador = jogaRyuga ? new int[] {1, 2, 3} : new int[] {4, 5, 6};
        int[] ladoRival = jogaRyuga ? new int[] {4, 5, 6} : new int[] {1, 2, 3};
        if (semGolpes.size() > ladoRival.length) throw new IllegalArgumentException("duplas sem golpes demais para o lado do rival");

        Dupla[] vagas = new Dupla[VAGAS];
        vagas[0] = ryuga;
        vagas[VAGAS - 1] = koga;
        // As sem golpes primeiro, em vagas sorteadas do lado do rival; o resto completa tudo.
        List<Integer> livresRival = new ArrayList<>();
        for (int v : ladoRival) livresRival.add(v);
        Collections.shuffle(livresRival, random);
        Collections.shuffle(semGolpes, random);
        for (int i = 0; i < semGolpes.size(); i++) vagas[livresRival.get(i)] = semGolpes.get(i);
        List<Integer> livres = new ArrayList<>();
        for (int v : ladoJogador) livres.add(v);
        for (int i = semGolpes.size(); i < livresRival.size(); i++) livres.add(livresRival.get(i));
        Collections.shuffle(livres, random);
        Collections.shuffle(prontas, random);
        for (int i = 0; i < prontas.size(); i++) vagas[livres.get(i)] = prontas.get(i);

        List<Integer> ordem = new ArrayList<>();
        for (int v = 1; v < VAGAS - 1; v++) ordem.add(v);
        Collections.shuffle(ordem, random);
        int[] ordemSorteio = new int[ordem.size()];
        for (int i = 0; i < ordemSorteio.length; i++) ordemSorteio[i] = ordem.get(i);
        return new Torneio(vagas, jogaRyuga ? ryuga : koga, jogaRyuga ? koga : ryuga, ordemSorteio, random);
    }

    public Dupla jogador() {
        return jogador;
    }

    public Dupla rival() {
        return rival;
    }

    /** Dupla da vaga {@code i} (0 a 7) nas quartas. */
    public Dupla vaga(int i) {
        return vagas[i];
    }

    /** Lado da vaga: 0 esquerdo (Dragão), 1 direito (Tigre). */
    public static int ladoDaVaga(int vaga) {
        return vaga < VAGAS / 2 ? 0 : 1;
    }

    /** Confrontos da rodada {@code r} (vazio se ela ainda não foi montada). */
    public List<Confronto> rodada(int r) {
        return r < rodadas.size() ? Collections.unmodifiableList(rodadas.get(r)) : Collections.<Confronto>emptyList();
    }

    /** O confronto da rodada {@code r} com esse nome nesse lado, ou null se ainda não existe. */
    public Confronto confronto(int r, int lado, String nome) {
        for (Confronto c : rodada(r)) {
            if (c.lado == lado && c.nome.equals(nome)) return c;
        }
        return null;
    }

    /** Rodadas já disputadas: 0 antes das quartas, 4 com o campeão decidido. */
    public int disputadas() {
        return disputadas;
    }

    /** Ordem das vagas 1 a 6 no sorteio. */
    public int[] ordemSorteio() {
        return ordemSorteio.clone();
    }

    public boolean acabou() {
        return disputadas >= RODADAS;
    }

    public boolean jogadorEliminado() {
        return eliminado;
    }

    /** O jogador perdeu nas quartas e seguiu pela repescagem. */
    public boolean pelaRepescagem() {
        Confronto quartas = confrontoDe(0, jogador);
        return quartas != null && quartas.perdedor() == jogador;
    }

    public Dupla campeao() {
        Confronto finalCentral = confronto(RODADAS - 1, -1, FINAL);
        return finalCentral != null ? finalCentral.vencedor() : null;
    }

    private Confronto confrontoDe(int r, Dupla d) {
        for (Confronto c : rodada(r)) {
            if (c.tem(d)) return c;
        }
        return null;
    }

    /** O confronto do jogador na rodada em disputa, ou null se não há mais luta para ele. */
    public Confronto confrontoDoJogador() {
        if (eliminado || acabou()) return null;
        return confrontoDe(disputadas, jogador);
    }

    /** O próximo adversário do jogador, ou null se não há mais luta para ele. */
    public Dupla adversario() {
        Confronto c = confrontoDoJogador();
        return c == null ? null : c.a == jogador ? c.b : c.a;
    }

    /** Nome da próxima luta do jogador (QUARTAS DE FINAL, SEMIFINAL, REPESCAGEM...), ou null. */
    public String rodadaAtual() {
        Confronto c = confrontoDoJogador();
        return c == null ? null : c.nome;
    }

    /** Perder agora não repete a luta: a derrota nas quartas leva para a repescagem. */
    public boolean derrotaTemSegundaChance() {
        return disputadas == 0 && confrontoDoJogador() != null;
    }

    /**
     * Fecha a rodada atual com o resultado da luta do jogador. As outras lutas da rodada se
     * resolvem (o rival sempre vence; entre duas duplas do CPU, sorteio) e a próxima rodada é
     * montada.
     */
    public void registrar(boolean jogadorVenceu) {
        if (acabou()) return;
        for (Confronto c : rodadas.get(disputadas)) {
            if (c.tem(jogador)) {
                c.vencedor = jogadorVenceu ? jogador : (c.a == jogador ? c.b : c.a);
                // Derrota nas quartas: ainda há a repescagem. Depois disso, a dupla sai.
                if (!jogadorVenceu && disputadas > 0) eliminado = true;
            } else if (c.tem(rival)) {
                c.vencedor = rival;
            } else {
                c.vencedor = random.nextBoolean() ? c.a : c.b;
            }
        }
        disputadas++;
        if (!acabou()) rodadas.add(montar(disputadas));
    }

    private List<Confronto> montar(int r) {
        List<Confronto> out = new ArrayList<>();
        List<Confronto> antes = rodadas.get(r - 1);
        if (r == 1) {
            for (int lado = 0; lado < 2; lado++) {
                Confronto q1 = antes.get(2 * lado), q2 = antes.get(2 * lado + 1);
                out.add(new Confronto(1, lado, SEMIFINAL, q1.vencedor(), q2.vencedor()));
                out.add(new Confronto(1, lado, REPESCAGEM, q1.perdedor(), q2.perdedor()));
            }
        } else if (r == 2) {
            for (int lado = 0; lado < 2; lado++) {
                Confronto semi = confronto(1, lado, SEMIFINAL), rep = confronto(1, lado, REPESCAGEM);
                out.add(new Confronto(2, lado, FINAL_DA_CHAVE, semi.vencedor(), rep.vencedor()));
            }
        } else {
            out.add(new Confronto(3, -1, FINAL, antes.get(0).vencedor(), antes.get(1).vencedor()));
        }
        return out;
    }
}
