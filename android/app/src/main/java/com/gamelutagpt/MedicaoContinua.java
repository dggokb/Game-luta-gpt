package com.gamelutagpt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Botão MEDIR do Modo Teste (20/11, o dono: "faz um botão que fala o que você precisa"): ligado, mede
 * cada luta enquanto ele joga de verdade, em qualquer modo (arcade, história, teste), e mostra na tela
 * o resumo da luta de agora e das anteriores. Serve para achar o que pesa quando o FPS cai numa luta
 * que vem depois de outra (o Félix e o Krüger na história), coisa que o TESTE (uma luta roteirizada)
 * não pega. Vale nesta sessão do app. Só a thread do jogo mexe aqui.
 *
 * <p>Por luta: quadros por segundo de verdade (o relógio entre um quadro e o outro, com a espera da
 * tela), o tempo do quadro (médio e o pior), a fração acima de 20 ms, as partes do quadro (as do
 * {@link PerfilQuadros}: fundo, luta, HUD, a espera da tela em "trava" e "envio"), e a memória: o
 * heap do Java e as folhas decodificadas no cache.
 */
final class MedicaoContinua {
    /** No PC, {@code -Dgameluta.medir=true} começa ligado. */
    static volatile boolean ligada = Boolean.getBoolean("gameluta.medir");
    static final int GUARDADAS = 5;

    private static final class Luta {
        final String quem;
        long quadros, ocupadoNs, piorNs, acima20, relogioNs;
        final long[] partes = new long[PerfilQuadros.NOMES.length];
        long heapMax, folhasMax;
        Luta(String quem) { this.quem = quem; }
    }

    private final List<Luta> antigas = new ArrayList<>();
    private Luta atual;
    private long ultimoFim;
    /** Lutas que já fecharam e contaram (mais de 30 quadros), para numerar. */
    private int fechadas;

    /** Um quadro de luta terminou ({@code quem}: as duplas, para separar uma luta da outra). */
    void quadro(PerfilQuadros p, String quem, long folhasBytes) {
        long agora = System.nanoTime();
        long intervalo = ultimoFim == 0 ? 0 : agora - ultimoFim;
        ultimoFim = agora;
        if (atual == null || !atual.quem.equals(quem)) {
            if (atual != null && atual.quadros > 30) {
                antigas.add(atual);
                fechadas++;
                while (antigas.size() > GUARDADAS) antigas.remove(0);
            }
            atual = new Luta(quem);
            return;
        }
        // Um engasgo de carregamento (mais de meio segundo parado) não é quadro de luta.
        if (intervalo <= 0 || intervalo > 500_000_000L) return;
        atual.quadros++;
        atual.relogioNs += intervalo;
        atual.ocupadoNs += p.ultimoTotal;
        atual.piorNs = Math.max(atual.piorNs, intervalo);
        if (intervalo > 20_000_000L) atual.acima20++;
        for (int i = 0; i < atual.partes.length; i++) atual.partes[i] += p.ultimoQuadro[i];
        Runtime r = Runtime.getRuntime();
        atual.heapMax = Math.max(atual.heapMax, r.totalMemory() - r.freeMemory());
        atual.folhasMax = Math.max(atual.folhasMax, folhasBytes);
    }

    /** Saiu da luta (menu, página da história): o próximo quadro de luta não conta o tempo parado. */
    void pausa() { ultimoFim = 0; }

    /** As linhas da caixa: as lutas antigas e a de agora (a última embaixo). */
    List<String> linhas() {
        List<String> out = new ArrayList<>();
        out.add("MEDIR (toque no MEDIR do Modo Teste para desligar)  " + Runtime.getRuntime().availableProcessors() + " núcleos");
        int n = fechadas - antigas.size();
        for (Luta l : antigas) out.add(linha(++n, l, false));
        if (atual != null) out.add(linha(++n, atual, true));
        return out;
    }

    private static String linha(int n, Luta l, boolean agora) {
        if (l.quadros == 0) return String.format(Locale.ROOT, "%sluta %d %s: começando", agora ? "> " : "  ", n, l.quem);
        double fps = l.quadros * 1e9 / Math.max(1, l.relogioNs);
        StringBuilder s = new StringBuilder(String.format(Locale.ROOT,
            "%sluta %d %s: %.0f fps, quadro %.1f ms (pior %.0f), >20 ms %d%% |",
            agora ? "> " : "  ", n, l.quem, fps, l.ocupadoNs / 1e6 / l.quadros, l.piorNs / 1e6, 100 * l.acima20 / l.quadros));
        for (int i = 0; i < l.partes.length; i++) {
            double ms = l.partes[i] / 1e6 / l.quadros;
            if (ms >= 0.05) s.append(String.format(Locale.ROOT, " %s %.1f", PerfilQuadros.NOMES[i], ms));
        }
        s.append(String.format(Locale.ROOT, " | heap %d MB folhas %d MB", l.heapMax >> 20, l.folhasMax >> 20));
        return s.toString();
    }
}
