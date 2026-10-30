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
    /** Bytes de textura subidos em cada quadro gravado. */
    private final List<Long> subidas = new ArrayList<>();

    // O que o desenho em OpenGL conta durante o teste (tudo na thread do jogo).
    /** O teste está gravando (o OpenGL só conta a área pintada enquanto isso). */
    static boolean gravando;
    /** PC, perfil de CPU ({@code -Pjfr}): sem a conta da área, para medir só o jogo. */
    static final boolean SEM_AREA = Boolean.getBoolean("gameluta.semArea");
    private static final java.util.HashMap<String, Integer> textosFeitos = new java.util.HashMap<>();
    private static long bytesNoQuadro;
    private static int atlasCheio;

    private static String textoPedido = "";
    /** Pixels que cada texto ocupou na textura dos textos (somando as vezes que foi feito). */
    private static final java.util.HashMap<String, Integer> areaDosTextos = new java.util.HashMap<>();

    /**
     * Diagnóstico (PC, -Dgameluta.areaPorOrigem=true): de onde vem cada triângulo grande, pela
     * pilha de chamadas. Caro; só para achar quem pinta a tela muitas vezes.
     */
    static final boolean areaPorOrigem = Boolean.getBoolean("gameluta.areaPorOrigem");
    private static final java.util.HashMap<String, Double> areaPorQuem = new java.util.HashMap<>();

    static void areaGrande(double telas) {
        if (!gravando) return;
        String quem = origem(2);
        areaNoQuadro.merge(quem, telas, Double::sum);
    }

    /** Quem chamou o desenho: os primeiros {@code n} lugares fora do OpenGL e do Canvas. */
    private static String origem(int n) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement f : new Throwable().getStackTrace()) {
            String c = f.getClassName();
            if (c.endsWith("GlRenderCanvas") || c.endsWith("CanvasGl") || c.endsWith("TesteAutomatico")
                || c.startsWith("android.") || c.contains("AndroidRenderCanvas") || c.contains("CanvasGl$")) continue;
            if (sb.length() > 0) sb.append(" < ");
            sb.append(c.substring(c.lastIndexOf('.') + 1)).append('.').append(f.getMethodName()).append(':').append(f.getLineNumber());
            if (--n == 0) break;
        }
        return sb.length() > 0 ? sb.toString() : "?";
    }

    /** Desenhos do quadro de agora; vão para {@link #areaPorQuem} só nos quadros pesados. */
    private static final java.util.HashMap<String, Double> areaNoQuadro = new java.util.HashMap<>();
    private static int quadrosPesados;

    /** Um texto vai virar bitmap agora (não estava guardado). */
    static void textoPedido(String texto) {
        if (!gravando) return;
        textoPedido = texto.length() > 18 ? texto.substring(0, 18) + "…" : texto;
        if (areaPorOrigem) textoPedido += " @" + origem(1);
    }

    /** O bitmap do texto pedido foi para uma textura só dele (não ocupa a dos textos). */
    static void textoGuardadoFora(int pixels) {
        if (!gravando) return;
        textosFeitos.merge(textoPedido, 1, Integer::sum);
        textosFora++;
    }

    private static int textosFora;

    /** O bitmap do texto pedido entrou na textura dos textos com {@code pixels} de lugar. */
    static void textoGuardado(int pixels) {
        if (!gravando) return;
        textosFeitos.merge(textoPedido, 1, Integer::sum);
        areaDosTextos.merge(textoPedido, pixels, Integer::sum);
    }

    /** Uma textura subiu para a placa de vídeo. */
    static void subiu(long bytes) {
        if (gravando) bytesNoQuadro += bytes;
    }

    /** A textura dos textos encheu e recomeçou (todos os textos são refeitos). */
    static void atlasDeTextoCheio() {
        if (gravando) atlasCheio++;
    }
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
        subidas.clear();
        textosFeitos.clear();
        areaDosTextos.clear();
        textosFora = 0;
        areaPorQuem.clear();
        areaNoQuadro.clear();
        quadrosPesados = 0;
        bytesNoQuadro = 0;
        atlasCheio = 0;
        gravando = true;
    }

    /** Segundos que faltam (para o botão). */
    int segundosRestantes() {
        return Math.max(0, (DURACAO - passos + 59) / 60);
    }

    /**
     * Um passo da luta: barras zeradas no começo (a IA não gasta antes da hora), vida de volta
     * abaixo de 1/3 (ninguém cai) e os pedidos do roteiro por cima do que a IA quis fazer. Cada
     * pedido se repete por até 3 s, até pegar (o lutador pode estar apanhando ou no ar).
     */
    void roteiro(CombatEngine engine, FighterInput jogador, FighterInput rival) {
        if (!ativo) return;
        FighterState p = engine.fighter(GameView.PLAYER).state, o = engine.fighter(GameView.OPPONENT).state;
        if (passos == 0) {
            superSaiu = ultraSaiu = false;
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
        // A barra fica cheia até o pedido pegar: a barra que desceu é o Super (ou o Ultra) saindo.
        if (janela(SUPER) && !superSaiu) {
            if (passos > SUPER && p.superMeter < CombatConfig.MAX_METER) {
                superSaiu = true;
            } else {
                p.superMeter = CombatConfig.MAX_METER;
                soPedido(jogador);
                jogador.superAttack = true;
            }
        }
        if (janela(ULTRA) && !ultraSaiu) {
            if (passos > ULTRA && p.superMeter < CombatConfig.MAX_METER) {
                ultraSaiu = true;
            } else {
                p.superMeter = CombatConfig.MAX_METER;
                soPedido(jogador);
                jogador.ultra = true;
                jogador.superAttack = true;
            }
        }
        if (passos == SHINKAI_RIVAL) {
            o.shinkai = CombatConfig.SHINKAI_MAX;
            o.shinkaiUsed = false;
        }
        if (janela(SHINKAI_RIVAL) && !o.shinkaiUsed) rival.shinkai = true;
        passos++;
    }

    private boolean superSaiu, ultraSaiu;

    /**
     * Sem os botões de golpe da IA: apanhando, um deles é o combo breaker, que tem a frente e
     * gasta a barra (e o roteiro achava que o Super ou o Ultra tinha saído).
     */
    private static void soPedido(FighterInput in) {
        in.light = in.medium = in.heavy = in.pushblock = in.grab = false;
        in.special = null;
        in.auto = false;
    }

    private boolean janela(int inicio) {
        return passos >= inicio && passos < inicio + 180;
    }

    /** Um quadro desenhado da luta: o tempo por parte, o que estava na tela e os números do OpenGL. */
    void quadro(PerfilQuadros perfil, int contexto, int textosNovos, int formas, int desenhos, int telasX10) {
        // Só a luta: a intro e o READY ainda não contam.
        if (!ativo || passos == 0) return;
        long[] q = Arrays.copyOf(perfil.ultimoQuadro, perfil.ultimoQuadro.length + 1);
        q[q.length - 1] = perfil.ultimoTotal;
        quadros.add(q);
        subidas.add(bytesNoQuadro);
        bytesNoQuadro = 0;
        contextos.add(contexto);
        gl.add(new int[] {textosNovos, formas, desenhos, telasX10});
        if (areaPorOrigem) {
            if (telasX10 >= 200) {
                quadrosPesados++;
                for (java.util.Map.Entry<String, Double> e : areaNoQuadro.entrySet()) areaPorQuem.merge(e.getKey(), e.getValue(), Double::sum);
            }
            areaNoQuadro.clear();
        }
        if (passos >= DURACAO) terminar();
    }

    private void terminar() {
        ativo = false;
        gravando = false;
        relatorio = montarRelatorio().toArray(new String[0]);
        if (!areaPorQuem.isEmpty()) {
            int n = Math.max(1, quadrosPesados);
            System.out.println("AREA nos " + quadrosPesados + " quadros com 20+ telas:");
            areaPorQuem.entrySet().stream().sorted((x, y) -> Double.compare(y.getValue(), x.getValue())).limit(25)
                .forEach(e -> System.out.println(String.format(Locale.ROOT, "AREA %6.2f telas/quadro  %s", e.getValue() / n, e.getKey())));
        }
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
        // As duas partes que mais pesam em cada momento.
        StringBuilder porMomento = new StringBuilder("Onde pesa:");
        for (int m = 0; m < MOMENTOS.length; m++) {
            double[] soma = new double[total];
            int c = 0;
            for (int i = 0; i < n; i++) {
                if (momento(contextos.get(i)) != m) continue;
                for (int k = 0; k < total; k++) soma[k] += quadros.get(i)[k] / 1e6;
                c++;
            }
            if (c == 0) continue;
            int a = 0, b = -1;
            for (int k = 1; k < total; k++) if (soma[k] > soma[a]) a = k;
            for (int k = 0; k < total; k++) if (k != a && (b < 0 || soma[k] > soma[b])) b = k;
            porMomento.append(String.format(Locale.ROOT, "  %s: %s %.1f, %s %.1f", MOMENTOS[m],
                PerfilQuadros.NOMES[a], soma[a] / c, PerfilQuadros.NOMES[b], soma[b] / c));
        }
        r.add(porMomento.toString());
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
            long totalSubido = 0;
            int comSubida = 0;
            for (long s : subidas) {
                totalSubido += s;
                if (s > 0) comSubida++;
            }
            r.add(String.format(Locale.ROOT, "OpenGL por quadro: textos novos %.1f (máx %d) · formas %.1f (máx %d) · desenhos %.0f"
                    + " · texturas subidas %d MB em %d quadros · textura dos textos encheu %d vezes · %d textos grandes à parte",
                tn / n, maxTextos, fo / n, maxFormas, de / n, totalSubido >> 20, comSubida, atlasCheio, textosFora));
            List<java.util.Map.Entry<String, Integer>> feitos = new ArrayList<>(textosFeitos.entrySet());
            feitos.sort((x, y) -> y.getValue() - x.getValue());
            StringBuilder t = new StringBuilder("Textos refeitos:");
            for (int j = 0; j < Math.min(6, feitos.size()); j++) {
                t.append(String.format(Locale.ROOT, "  \"%s\" ×%d", feitos.get(j).getKey(), feitos.get(j).getValue()));
            }
            if (!feitos.isEmpty()) r.add(t.toString());
            // Quem enche a textura dos textos (2048×2048 = 4,2 milhões de pixels).
            List<java.util.Map.Entry<String, Integer>> areas = new ArrayList<>(areaDosTextos.entrySet());
            areas.sort((x, y) -> Integer.compare(y.getValue(), x.getValue()));
            StringBuilder a = new StringBuilder("Textos que mais ocupam (mil pixels):");
            for (int j = 0; j < Math.min(6, areas.size()); j++) {
                a.append(String.format(Locale.ROOT, "  \"%s\" %d", areas.get(j).getKey(), areas.get(j).getValue() / 1000));
            }
            if (!areas.isEmpty()) r.add(a.toString());
            if (areaPorOrigem) {
                java.util.HashMap<String, Integer> porLugar = new java.util.HashMap<>();
                long soma = 0;
                for (java.util.Map.Entry<String, Integer> e : areaDosTextos.entrySet()) {
                    int at = e.getKey().indexOf(" @");
                    porLugar.merge(at >= 0 ? e.getKey().substring(at + 2) : "?", e.getValue(), Integer::sum);
                    soma += e.getValue();
                }
                System.out.println("TEXTOS: " + (soma / 1000) + " mil pixels no total");
                porLugar.entrySet().stream().sorted((x, y) -> Integer.compare(y.getValue(), x.getValue())).limit(15)
                    .forEach(e -> System.out.println("TEXTOS " + (e.getValue() / 1000) + " mil  " + e.getKey()));
            }
            double telas = 0;
            int maxTelas = 0;
            for (int[] g : gl) {
                telas += g[3];
                maxTelas = Math.max(maxTelas, g[3]);
            }
            r.add(String.format(Locale.ROOT, "Área pintada: %.1f telas por quadro (máx %.1f)", telas / 10.0 / n, maxTelas / 10.0));
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
            long mb = subidas.get(i) >> 20;
            piores.append(String.format(Locale.ROOT, "  %.0f ms (%s %.0f, %s, %.1f telas, textos %d%s)", tempos[i],
                PerfilQuadros.NOMES[maior], q[maior] / 1e6, MOMENTOS[momento(contextos.get(i))], gl.get(i)[3] / 10.0, gl.get(i)[0],
                subidas.get(i) > 0 ? ", subiu " + (mb > 0 ? mb + " MB" : (subidas.get(i) >> 10) + " KB") : ""));
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
