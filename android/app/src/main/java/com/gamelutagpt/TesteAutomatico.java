package com.gamelutagpt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Teste de desempenho automático: 30 s de luta com a IA dos dois lados e o roteiro de sempre
 * (SHINKAI do jogador, Super, Ultra, SHINKAI do rival), medindo cada quadro por parte. No fim,
 * um relatório numa tela só: a média e o p95 de cada parte, o quadro por momento da luta
 * (normal, combo, SHINKAI, Super, Ultra) e os piores quadros com o que estava acontecendo.
 * O mesmo roteiro roda no celular (botão TESTE) e no PC sem tela ({@code gradle :pc:medirLuta}).
 */
final class TesteAutomatico {
    /** Quadros de simulação do teste (30 s a 60 Hz). */
    static final int DURACAO = 1800;
    static final int SHINKAI_JOGADOR = 180, SUPER = 600, ULTRA = 1000, SHINKAI_RIVAL = 1400;

    /** O que estava na tela num quadro (bits). */
    static final int COMBO = 1, SHINKAI = 2, SUPER_NA_TELA = 4, ULTRA_NA_TELA = 8;
    private static final String[] MOMENTOS = {"normal", "combo", "SHINKAI", "Super", "Ultra"};

    private boolean ativo;
    private int passos;
    private final List<long[]> quadros = new ArrayList<>();
    private final List<Integer> contextos = new ArrayList<>();
    private final List<int[]> gl = new ArrayList<>();
    private String[] relatorio;
    private String descricao = "";

    boolean ativo() { return ativo; }

    /** Relatório do último teste (linhas), ou null. */
    String[] relatorio() { return relatorio; }

    void fecharRelatorio() { relatorio = null; }

    /** {@code descricao}: a qualidade, a resolução e o desenho (vai no título do relatório). */
    void comecar(String descricao) {
        this.descricao = descricao;
        ativo = true;
        passos = 0;
        quadros.clear();
        contextos.clear();
        gl.clear();
        relatorio = null;
    }

    /** Segundos que faltam (para o botão). */
    int segundosRestantes() {
        return Math.max(0, (DURACAO - passos + 59) / 60);
    }

    /**
     * Um passo da luta: barras zeradas no começo (a IA não gasta antes da hora), vida de volta
     * abaixo de 1/3 (ninguém cai) e os pedidos do roteiro por cima do que a IA quis fazer. Cada
     * pedido se repete por até 1 s, até pegar (o lutador pode estar apanhando ou no ar).
     */
    void roteiro(CombatEngine engine, FighterInput jogador, FighterInput rival) {
        if (!ativo) return;
        FighterState p = engine.fighter(GameView.PLAYER).state, o = engine.fighter(GameView.OPPONENT).state;
        if (passos == 0) {
            for (FighterState f : new FighterState[] {p, o}) {
                f.superMeter = 0;
                f.shinkai = 0;
                f.shinkaiUsed = false;
            }
        }
        for (FighterState f : new FighterState[] {p, o}) {
            if (f.life < f.profile.maxLife / 3) f.life = f.profile.maxLife;
        }
        // O SHINKAI só na hora do roteiro: um da IA antes virava um Clash por cima do Ultra.
        if (passos < SHINKAI_JOGADOR) p.shinkai = 0;
        if (passos < SHINKAI_RIVAL) o.shinkai = 0;
        if (passos == SHINKAI_JOGADOR) p.shinkai = CombatConfig.SHINKAI_MAX;
        if (janela(SHINKAI_JOGADOR) && !p.shinkaiUsed) jogador.shinkai = true;
        if (passos == SUPER || passos == ULTRA) p.superMeter = CombatConfig.MAX_METER;
        if (janela(SUPER) && p.superMeter == CombatConfig.MAX_METER) jogador.superAttack = true;
        if (janela(ULTRA) && p.superMeter == CombatConfig.MAX_METER) {
            jogador.ultra = true;
            jogador.superAttack = true;
        }
        if (passos == SHINKAI_RIVAL) {
            o.shinkai = CombatConfig.SHINKAI_MAX;
            o.shinkaiUsed = false;
        }
        if (janela(SHINKAI_RIVAL) && !o.shinkaiUsed) rival.shinkai = true;
        passos++;
    }

    private boolean janela(int inicio) {
        return passos >= inicio && passos < inicio + 60;
    }

    /** Um quadro desenhado da luta: o tempo por parte, o que estava na tela e os números do OpenGL. */
    void quadro(PerfilQuadros perfil, int contexto, int textosNovos, int formas, int desenhos) {
        // Só a luta: a intro e o READY ainda não contam.
        if (!ativo || passos == 0) return;
        long[] q = Arrays.copyOf(perfil.ultimoQuadro, perfil.ultimoQuadro.length + 1);
        q[q.length - 1] = perfil.ultimoTotal;
        quadros.add(q);
        contextos.add(contexto);
        gl.add(new int[] {textosNovos, formas, desenhos});
        if (passos >= DURACAO) terminar();
    }

    private void terminar() {
        ativo = false;
        relatorio = montarRelatorio().toArray(new String[0]);
        for (String linha : relatorio) android.util.Log.i(PerfilQuadros.TAG, linha);
    }

    private List<String> montarRelatorio() {
        List<String> r = new ArrayList<>();
        int n = quadros.size();
        if (n == 0) {
            r.add("Teste sem quadros medidos.");
            return r;
        }
        int total = PerfilQuadros.NOMES.length;
        double[] tempos = new double[n];
        for (int i = 0; i < n; i++) tempos[i] = quadros.get(i)[total] / 1e6;
        double[] ord = tempos.clone();
        Arrays.sort(ord);
        double media = 0;
        for (double t : tempos) media += t;
        media /= n;
        int lentos = 0;
        for (double t : tempos) if (t > 20.0) lentos++;
        r.add(String.format(Locale.ROOT, "TESTE 30 s (%s): %d quadros · quadro médio %.1f ms · p95 %.1f · pior %.0f · acima de 20 ms: %d (%.0f%%)",
            descricao, n, media, ord[(int)(n * 0.95)], ord[n - 1], lentos, lentos * 100.0 / n));
        StringBuilder partes = new StringBuilder("Partes, média/p95 ms:");
        for (int k = 0; k < total; k++) {
            double[] v = new double[n];
            double m = 0;
            for (int i = 0; i < n; i++) {
                v[i] = quadros.get(i)[k] / 1e6;
                m += v[i];
            }
            m /= n;
            if (m < 0.05) continue;
            Arrays.sort(v);
            partes.append(String.format(Locale.ROOT, "  %s %.1f/%.1f", PerfilQuadros.NOMES[k], m, v[(int)(n * 0.95)]));
        }
        r.add(partes.toString());
        StringBuilder momentos = new StringBuilder("Quadro médio por momento:");
        for (int m = 0; m < MOMENTOS.length; m++) {
            double soma = 0;
            int c = 0;
            for (int i = 0; i < n; i++) {
                if (momento(contextos.get(i)) != m) continue;
                soma += tempos[i];
                c++;
            }
            if (c > 0) momentos.append(String.format(Locale.ROOT, "  %s %.1f ms (%d)", MOMENTOS[m], soma / c, c));
        }
        r.add(momentos.toString());
        double tn = 0, fo = 0, de = 0;
        int maxTextos = 0, maxFormas = 0;
        for (int[] g : gl) {
            tn += g[0];
            fo += g[1];
            de += g[2];
            maxTextos = Math.max(maxTextos, g[0]);
            maxFormas = Math.max(maxFormas, g[1]);
        }
        if (de > 0) {
            r.add(String.format(Locale.ROOT, "OpenGL por quadro: textos novos %.1f (máx %d) · formas %.1f (máx %d) · desenhos %.0f",
                tn / n, maxTextos, fo / n, maxFormas, de / n));
        }
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(tempos[b], tempos[a]));
        StringBuilder piores = new StringBuilder("Piores:");
        for (int j = 0; j < Math.min(5, n); j++) {
            int i = idx[j];
            long[] q = quadros.get(i);
            int maior = 0;
            for (int k = 1; k < total; k++) if (q[k] > q[maior]) maior = k;
            piores.append(String.format(Locale.ROOT, "  %.0f ms (%s %.0f, %s, textos %d, formas %d)", tempos[i],
                PerfilQuadros.NOMES[maior], q[maior] / 1e6, MOMENTOS[momento(contextos.get(i))], gl.get(i)[0], gl.get(i)[1]));
        }
        r.add(piores.toString());
        return r;
    }

    /** O momento que pesa mais num quadro: Ultra, Super, SHINKAI, combo ou normal. */
    private static int momento(int contexto) {
        if ((contexto & ULTRA_NA_TELA) != 0) return 4;
        if ((contexto & SUPER_NA_TELA) != 0) return 3;
        if ((contexto & SHINKAI) != 0) return 2;
        if ((contexto & COMBO) != 0) return 1;
        return 0;
    }
}
