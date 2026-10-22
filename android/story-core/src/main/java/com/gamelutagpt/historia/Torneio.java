package com.gamelutagpt.historia;

import com.gamelutagpt.render.MiniJson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A chave do torneio do modo história: 8 duplas fixas, quartas de final, semifinal e final.
 *
 * <pre>
 *  vaga 0  Ryūga ─┐                              ┌─ vaga 4
 *  vaga 1  ...   ─┴─ semi ─┐            ┌─ semi ─┴─ vaga 5
 *                          ├── FINAL ───┤
 *  vaga 2  ...   ─┬─ semi ─┘            └─ semi ─┬─ vaga 6
 *  vaga 3  ...   ─┘                              └─ vaga 7  Kōga
 * </pre>
 *
 * A dupla do Ryūga fica fixa na vaga 0 e a do Kōga na vaga 7: lados opostos, só se encontram
 * na final. Entre as quartas e a semifinal, fora da arena, a dupla que perdeu a outra quarta do
 * lado do jogador não aceita a derrota e embosca o jogador ({@link #emboscada()}): acham que, se
 * vencerem, ficam com a vaga dele. Não muda a chave (os organizadores só assistem). As outras são sorteadas nas vagas 1 a 6. Dupla que ainda não luta
 * ({@link Dupla#pronta} falso) vai para o lado do rival, para nunca cruzar com o jogador.
 * O rival sempre avança; as lutas entre duas duplas do CPU têm o vencedor sorteado.
 *
 * O modo Arcade usa a mesma chave com 16 vagas ({@link #arcade}): oitavas, quartas, semifinal e
 * final, a dupla do jogador na vaga 0 e as outras 15 sorteadas; não há rival fixo.
 */
public final class Torneio {
    public static final String ARQUIVO = Temporada.PASTA + "/torneio/torneio.json";
    /** Vagas da chave do modo história. */
    public static final int VAGAS = 8;
    /** Quartas, semifinal e final (modo história). */
    public static final int RODADAS = 3;
    /** Vagas da chave do Arcade: oitavas, quartas, semifinal e final. */
    public static final int VAGAS_ARCADE = 16;
    public static final String OITAVAS = "OITAVAS DE FINAL";
    public static final String QUARTAS = "QUARTAS DE FINAL";
    public static final String SEMIFINAL = "SEMIFINAL";
    public static final String FINAL = "FINAL";
    public static final String[] NOMES_RODADAS = {OITAVAS, QUARTAS, SEMIFINAL, FINAL};
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
            return "player_base".equals(lutadores[i]) ? "p01" : lutadores[i];
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

    /**
     * Participantes de cada rodada: [0] = todas as vagas, depois a metade a cada rodada até
     * [rodadas()] = o campeão. Na história, 8 → 4 semifinalistas → 2 finalistas → campeão.
     */
    private final Dupla[][] rodadas;
    private final int vagas;
    private final Dupla jogador;
    private final Dupla rival;
    private final Random random;
    /** Ordem em que as vagas 1 a 6 foram sorteadas (para a animação do sorteio). */
    private final int[] ordemSorteio;
    /** Rodadas já disputadas (0 a 3). */
    private int disputadas;
    private boolean eliminado;

    private Torneio(Dupla[] vagas, Dupla jogador, Dupla rival, int[] ordemSorteio, Random random) {
        this.vagas = vagas.length;
        int n = Integer.numberOfTrailingZeros(this.vagas);
        if (this.vagas != 1 << n || n < 1) throw new IllegalArgumentException("a chave precisa de 2, 4, 8, 16... vagas");
        rodadas = new Dupla[n + 1][];
        for (int r = 0; r <= n; r++) rodadas[r] = new Dupla[this.vagas >> r];
        System.arraycopy(vagas, 0, rodadas[0], 0, this.vagas);
        this.jogador = jogador;
        this.rival = rival;
        this.ordemSorteio = ordemSorteio;
        this.random = random;
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

    /**
     * A chave do Arcade: a dupla do jogador na vaga 0 e as outras 15 sorteadas nas vagas 1 a 15
     * (sem rival fixo: entre duas duplas do CPU, vencedor sorteado).
     *
     * @param outras as 15 duplas do CPU
     */
    public static Torneio arcade(Dupla jogador, List<Dupla> outras, Random random) {
        if (jogador == null) throw new IllegalArgumentException("o Arcade precisa da dupla do jogador");
        if (outras.size() != VAGAS_ARCADE - 1) {
            throw new IllegalArgumentException("o Arcade precisa de " + (VAGAS_ARCADE - 1) + " duplas do CPU, tem " + outras.size());
        }
        List<Dupla> embaralhadas = new ArrayList<>(outras);
        Collections.shuffle(embaralhadas, random);
        Dupla[] vagas = new Dupla[VAGAS_ARCADE];
        vagas[0] = jogador;
        for (int i = 0; i < embaralhadas.size(); i++) vagas[i + 1] = embaralhadas.get(i);
        List<Integer> ordem = new ArrayList<>();
        for (int v = 1; v < VAGAS_ARCADE; v++) ordem.add(v);
        Collections.shuffle(ordem, random);
        int[] ordemSorteio = new int[ordem.size()];
        for (int i = 0; i < ordemSorteio.length; i++) ordemSorteio[i] = ordem.get(i);
        return new Torneio(vagas, jogador, null, ordemSorteio, random);
    }

    /** Quantas duplas a chave tem (8 na história, 16 no Arcade). */
    public int vagas() {
        return vagas;
    }

    /** Quantas rodadas até o campeão (3 na história, 4 no Arcade). */
    public int rodadas() {
        return rodadas.length - 1;
    }

    /** Nome da rodada {@code r}: a final é a última, antes dela semifinal, quartas e oitavas. */
    public String nomeRodada(int r) {
        int faltam = rodadas() - r;
        return NOMES_RODADAS[Math.max(0, NOMES_RODADAS.length - faltam)];
    }

    public Dupla jogador() {
        return jogador;
    }

    /** O outro irmão (história), ou null no Arcade. */
    public Dupla rival() {
        return rival;
    }

    /** Quem está na posição {@code i} da rodada {@code r} (null = ainda não decidido). */
    public Dupla participante(int rodada, int i) {
        return rodadas[rodada][i];
    }

    /** Rodadas já disputadas: 0 antes da primeira, {@link #rodadas()} com o campeão decidido. */
    public int disputadas() {
        return disputadas;
    }

    /** Ordem das vagas sorteadas (as fixas ficam fora). */
    public int[] ordemSorteio() {
        return ordemSorteio.clone();
    }

    public boolean acabou() {
        return disputadas >= rodadas();
    }

    public boolean jogadorEliminado() {
        return eliminado;
    }

    public Dupla campeao() {
        return rodadas[rodadas()][0];
    }

    /** Nome da próxima rodada do jogador, ou null se acabou para ele. */
    public String rodadaAtual() {
        return adversario() != null ? nomeRodada(disputadas) : null;
    }

    /** Posição do jogador na rodada em disputa, ou -1 se ele saiu (ou o torneio acabou). */
    private int posicaoJogador() {
        if (eliminado || acabou()) return -1;
        Dupla[] atual = rodadas[disputadas];
        for (int i = 0; i < atual.length; i++) {
            if (atual[i] == jogador) return i;
        }
        return -1;
    }

    /**
     * A emboscada fora da arena: depois que o jogador passou das quartas, a dupla que perdeu a
     * outra quarta do lado dele. Null antes das quartas, se o jogador perdeu, depois, ou no
     * Arcade (só a história tem emboscada).
     */
    public Dupla emboscada() {
        if (disputadas != 1 || eliminado || rival == null) return null;
        int lado = posicaoInicial(jogador) < vagas / 2 ? 0 : 1;
        int minha = posicaoInicial(jogador) / 2;
        int outra = lado * 2 + (minha % 2 == 0 ? 1 : 0);
        Dupla vencedor = rodadas[1][outra];
        Dupla a = rodadas[0][2 * outra], b = rodadas[0][2 * outra + 1];
        return vencedor == a ? b : a;
    }

    private int posicaoInicial(Dupla d) {
        for (int i = 0; i < vagas; i++) {
            if (rodadas[0][i] == d) return i;
        }
        return -1;
    }

    /**
     * As duplas com golpes do lado do rival na chave (a metade dele, sem ele), na ordem das
     * vagas: o jogador só as encontraria na final, onde está sempre o rival.
     */
    public List<Dupla> prontasDoLadoDoRival() {
        List<Dupla> out = new ArrayList<>();
        if (rival == null) return out;
        int metade = vagas / 2;
        int inicio = posicaoInicial(rival) < metade ? 0 : metade;
        for (int i = inicio; i < inicio + metade; i++) {
            Dupla d = rodadas[0][i];
            if (d != rival && d.pronta) out.add(d);
        }
        return out;
    }

    /** O próximo adversário do jogador, ou null se não há mais luta para ele. */
    public Dupla adversario() {
        int i = posicaoJogador();
        return i < 0 ? null : rodadas[disputadas][i ^ 1];
    }

    /**
     * Fecha a rodada atual com o resultado da luta do jogador. As outras lutas da rodada se
     * resolvem: o rival sempre avança; entre duas duplas do CPU, vencedor sorteado.
     */
    public void registrar(boolean jogadorVenceu) {
        if (acabou()) return;
        Dupla[] atual = rodadas[disputadas];
        Dupla[] proxima = rodadas[disputadas + 1];
        for (int m = 0; m < proxima.length; m++) {
            Dupla a = atual[2 * m], b = atual[2 * m + 1];
            Dupla vencedor;
            if (a == jogador || b == jogador) {
                Dupla outro = a == jogador ? b : a;
                vencedor = jogadorVenceu ? jogador : outro;
                if (!jogadorVenceu) eliminado = true;
            } else if (rival != null && (a == rival || b == rival)) {
                vencedor = rival;
            } else {
                vencedor = random.nextBoolean() ? a : b;
            }
            proxima[m] = vencedor;
        }
        disputadas++;
    }
}
