package com.gamelutagpt.historia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Regras do modo Arcade: chave de 16 duplas (oitavas, quartas, semifinal e final 2x2), depois a
 * regra escondida (a dupla campeã luta entre si, 1x1) e o Campeão, o Takatoshi (ainda por fazer).
 *
 * O jogador escolhe uma dupla oficial (as de {@code torneio.json} que já lutam) ou monta uma
 * dupla livre com quem quiser. As outras vagas ficam com as duplas oficiais que sobraram e,
 * completando as 15, duplas livres do CPU sorteadas entre os lutadores com golpes (um lutador
 * aparece em mais de uma dupla: são 12 lutadores para 32 lugares, espalhados por igual).
 *
 * A CPU fica mais forte a cada luta, do nível escolhido na pausa até o 8 no duelo ({@link NivelCpu}).
 * As lutas são as do modo história: o episódio {@link #EPISODIO} toca o sorteio, a chave e as
 * lutas; perder repete a luta (o "continuar" do Arcade).
 */
public final class Arcade {
    /** O episódio do Arcade: sorteio, as 4 rodadas, o duelo e o Takatoshi. */
    public static final String EPISODIO = Temporada.PASTA + "/arcade/arcade.json";

    private Arcade() {}

    /** As duplas oficiais que já lutam (sem as {@code "pronta": false}). */
    public static List<Torneio.Dupla> oficiais(List<Torneio.Dupla> duplas) {
        List<Torneio.Dupla> out = new ArrayList<>();
        for (Torneio.Dupla d : duplas) if (d.pronta) out.add(d);
        return out;
    }

    /** Uma dupla livre: titular e parceiro escolhidos à vontade. */
    public static Torneio.Dupla livre(String titular, String parceiro) {
        return new Torneio.Dupla("livre:" + titular + "+" + parceiro, new String[] {titular, parceiro}, null, true);
    }

    /**
     * A chave: a dupla do jogador na vaga 0; as oficiais (menos a do jogador) e duplas livres do
     * CPU até completar 16. Cada dupla livre do CPU junta os lutadores que menos apareceram até
     * ali (empate sorteado), para ninguém ficar em quatro duplas enquanto outro fica em uma; nunca
     * repete a dupla de outra vaga (nem invertida) nem tem o mesmo lutador duas vezes.
     *
     * @param lutadores os lutadores que têm golpes (para as duplas livres do CPU)
     */
    public static Torneio chave(Torneio.Dupla jogador, List<Torneio.Dupla> oficiais, List<String> lutadores, Random random) {
        if (lutadores.size() < 3) throw new IllegalArgumentException("o Arcade precisa de pelo menos 3 lutadores");
        List<Torneio.Dupla> outras = new ArrayList<>();
        Set<String> pares = new HashSet<>();
        Map<String, Integer> vezes = new HashMap<>();
        for (String l : lutadores) vezes.put(l, 0);
        pares.add(par(jogador));
        contar(vezes, jogador);
        for (Torneio.Dupla d : oficiais) {
            if (outras.size() >= Torneio.VAGAS_ARCADE - 1 || !pares.add(par(d))) continue;
            outras.add(d);
            contar(vezes, d);
        }
        int livres = 0;
        while (outras.size() < Torneio.VAGAS_ARCADE - 1) {
            // Embaralha e ordena pelo número de vezes: os menos usados primeiro, empate sorteado.
            List<String> ordem = new ArrayList<>(lutadores);
            Collections.shuffle(ordem, random);
            Collections.sort(ordem, (a, b) -> Integer.compare(vezes.get(a), vezes.get(b)));
            Torneio.Dupla escolhida = null;
            for (int i = 0; i < ordem.size() && escolhida == null; i++) {
                for (int j = i + 1; j < ordem.size() && escolhida == null; j++) {
                    Torneio.Dupla d = livre(ordem.get(i), ordem.get(j));
                    if (!pares.contains(par(d))) escolhida = d;
                }
            }
            // Todas as duplas possíveis já estão na chave: repete a dos menos usados.
            if (escolhida == null) escolhida = livre(ordem.get(0), ordem.get(1));
            pares.add(par(escolhida));
            contar(vezes, escolhida);
            outras.add(new Torneio.Dupla("cpu" + (++livres) + ":" + escolhida.lutadores[0] + "+" + escolhida.lutadores[1],
                escolhida.lutadores, null, true));
        }
        return Torneio.arcade(jogador, outras, random);
    }

    private static void contar(Map<String, Integer> vezes, Torneio.Dupla d) {
        for (String l : d.lutadores) if (vezes.containsKey(l)) vezes.put(l, vezes.get(l) + 1);
    }

    /** A mesma dupla em qualquer ordem. */
    private static String par(Torneio.Dupla d) {
        String a = d.lutadores[0], b = d.lutadores[1];
        return a.compareTo(b) <= 0 ? a + "+" + b : b + "+" + a;
    }
}
