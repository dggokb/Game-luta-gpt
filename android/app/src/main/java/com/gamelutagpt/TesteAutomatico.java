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
    /** Tempo criando textos em cada quadro gravado (ns). */
    private final List<Long> temposDeTexto = new ArrayList<>();

    // O que o desenho em OpenGL conta durante o teste (tudo na thread do jogo).
    /** O teste está gravando (o OpenGL só conta a área pintada enquanto isso). */
    static boolean gravando;
    /**
     * A parte "hud" do quadro em pedaços, medida no aparelho (o perfil do PC não bate com o
     * celular: lá as formas são nativas, e o processador e a placa são outros).
     */
    static final int HUD_TELA = 0, HUD_CORTE = 1, HUD_TIMES = 2, HUD_MOLDURA = 3, HUD_SHINKAI = 4, HUD_BARRAS = 5,
        HUD_TEXTOS = 6, HUD_EFEITOS = 7, HUD_RELOGIO = 8, HUD_TESTE = 9, HUD_SELECAO = 10, HUD_MENSAGENS = 11,
        HUD_TUTORIAL = 12, HUD_TOQUE = 13, HUD_CINEMA = 14, HUD_RESTO = 15;
    private static final String[] PARTES_HUD = {"efeitos de tela", "corte/clash", "retratos e vida", "moldura",
        "barra SHINKAI", "barras de super", "PLAYER/TAG", "efeitos do HUD", "relógio", "botões de teste", "SELEÇÃO",
        "mensagens/combo", "tutorial", "controle de toque", "cinema/conversa", "vitória/Ultra/pausa"};
    private static final long[] tempoHud = new long[PARTES_HUD.length];
    private static long marcaHud;

    static void hudInicio() {
        if (gravando) marcaHud = System.nanoTime();
    }

    /** Fecha o pedaço {@code parte} do HUD: o tempo desde a marca anterior vai para ele. */
    static void hud(int parte) {
        if (!gravando) return;
        long agora = System.nanoTime();
        tempoHud[parte] += agora - marcaHud;
        marcaHud = agora;
    }

    /** PC, perfil de CPU ({@code -Pjfr}): sem a conta da área, para medir só o jogo. */
    static final boolean SEM_AREA = Boolean.getBoolean("gameluta.semArea");
    private static final java.util.HashMap<String, Integer> textosFeitos = new java.util.HashMap<>();
    private static long bytesNoQuadro;
    /** Tempo criando textos (desenhar o bitmap e mandar para a placa) no quadro de agora. */
    private static long nsTextosNoQuadro;

    static void tempoDeTexto(long ns) {
        if (!gravando) return;
        nsTextosNoQuadro += ns;
        if (areaPorOrigem && ns > 2_000_000L) System.out.println(String.format(Locale.ROOT, "TEXTO LENTO %.1f ms  %s", ns / 1e6, textoPedido));
    }
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
    /** O mesmo, nos quadros do Ultra. */
    private static final java.util.HashMap<String, Double> areaNoUltra = new java.util.HashMap<>();
    private static int quadrosDoUltra;

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
        if (!gravando) return;
        bytesNoQuadro += bytes;
        if (areaPorOrigem) System.out.println(String.format(Locale.ROOT, "SUBIU %.1f MB  %s", bytes / 1048576.0, origem(3)));
    }

    /** A textura dos textos encheu e recomeçou (todos os textos são refeitos). */
    static void atlasDeTextoCheio() {
        if (gravando) atlasCheio++;
    }
    private final List<Integer> contextos = new ArrayList<>();
    private final List<int[]> gl = new ArrayList<>();
    /** Tempo da placa de vídeo de cada quadro (ms; -1: sem medição). */
    private final List<Float> placa = new ArrayList<>();
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
        placa.clear();
        relatorio = null;
        subidas.clear();
        temposDeTexto.clear();
        nsTextosNoQuadro = 0;
        textosFeitos.clear();
        areaDosTextos.clear();
        textosFora = 0;
        java.util.Arrays.fill(tempoHud, 0);
        areaPorQuem.clear();
        areaNoUltra.clear();
        quadrosDoUltra = 0;
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
    void quadro(PerfilQuadros perfil, int contexto, int textosNovos, int formas, int desenhos, int telasX10,
                int placaX10, int placaAtras) {
        // Só a luta: a intro e o READY ainda não contam.
        if (!ativo || passos == 0) return;
        long[] q = Arrays.copyOf(perfil.ultimoQuadro, perfil.ultimoQuadro.length + 1);
        q[q.length - 1] = perfil.ultimoTotal;
        quadros.add(q);
        subidas.add(bytesNoQuadro);
        bytesNoQuadro = 0;
        temposDeTexto.add(nsTextosNoQuadro);
        nsTextosNoQuadro = 0;
        contextos.add(contexto);
        gl.add(new int[] {textosNovos, formas, desenhos, telasX10});
        // O tempo da placa chega 2 ou 3 quadros depois: vai para o quadro que ele mediu.
        placa.add(-1f);
        int medido = placa.size() - 1 - placaAtras;
        if (placaX10 >= 0 && placaAtras >= 1 && medido >= 0) placa.set(medido, placaX10 / 10f);
        if (areaPorOrigem) {
            if (telasX10 >= 200) {
                quadrosPesados++;
                for (java.util.Map.Entry<String, Double> e : areaNoQuadro.entrySet()) areaPorQuem.merge(e.getKey(), e.getValue(), Double::sum);
            }
            if (momento(contexto) == 4) {
                quadrosDoUltra++;
                for (java.util.Map.Entry<String, Double> e : areaNoQuadro.entrySet()) areaNoUltra.merge(e.getKey(), e.getValue(), Double::sum);
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
        if (!areaNoUltra.isEmpty()) {
            int n = Math.max(1, quadrosDoUltra);
            System.out.println("AREA nos " + quadrosDoUltra + " quadros do Ultra:");
            areaNoUltra.entrySet().stream().sorted((x, y) -> Double.compare(y.getValue(), x.getValue())).limit(25)
                .forEach(e -> System.out.println(String.format(Locale.ROOT, "ULTRA %6.2f telas/quadro  %s", e.getValue() / n, e.getKey())));
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
        // Em faixas: quadros presos à tela de 120 Hz caem em degraus de 8,3 ms (16,7 / 25 / 33).
        double[] limites = {14.5, 18.5, 22.5, 29.0, 37.0};
        String[] nomes = {"até 14", "15–18", "19–22", "23–29", "30–37", "38+"};
        int[] faixas = new int[nomes.length];
        for (double t : tempos) {
            int f = 0;
            while (f < limites.length && t > limites[f]) f++;
            faixas[f]++;
        }
        StringBuilder porFaixa = new StringBuilder("Quadros por faixa (ms):");
        for (int f = 0; f < nomes.length; f++) {
            if (faixas[f] > 0) porFaixa.append(String.format(Locale.ROOT, "  %s: %.0f%%", nomes[f], faixas[f] * 100.0 / n));
        }
        r.add(porFaixa.toString());
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
        // O que a placa de vídeo levou (a trava é a espera por ela; isto é o trabalho dela).
        StringBuilder naPlaca = new StringBuilder("Placa de vídeo, ms por quadro:");
        int medidos = 0;
        float piorPlaca = 0f;
        java.util.List<Float> todos = new ArrayList<>();
        for (int m = 0; m < MOMENTOS.length; m++) {
            double soma = 0;
            int c = 0;
            for (int i = 0; i < n && i < placa.size(); i++) {
                if (momento(contextos.get(i)) != m || placa.get(i) < 0f) continue;
                soma += placa.get(i);
                c++;
                todos.add(placa.get(i));
                piorPlaca = Math.max(piorPlaca, placa.get(i));
            }
            medidos += c;
            if (c > 0) naPlaca.append(String.format(Locale.ROOT, "  %s %.1f", MOMENTOS[m], soma / c));
        }
        if (medidos > 0) {
            java.util.Collections.sort(todos);
            naPlaca.append(String.format(Locale.ROOT, "  · p95 %.1f · pior %.1f (%d quadros medidos)",
                todos.get(Math.min(todos.size() - 1, (int)(todos.size() * 0.95))), piorPlaca, medidos));
            r.add(naPlaca.toString());
        } else {
            r.add("Placa de vídeo: sem medição (o aparelho não tem GL_EXT_disjoint_timer_query ou está no Canvas)");
        }
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
        Integer[] ordem = new Integer[PARTES_HUD.length];
        for (int i = 0; i < ordem.length; i++) ordem[i] = i;
        java.util.Arrays.sort(ordem, (x, y) -> Long.compare(tempoHud[y], tempoHud[x]));
        StringBuilder hud = new StringBuilder("HUD por pedaço (ms/quadro):");
        for (int j = 0; j < Math.min(9, ordem.length); j++) {
            double ms = tempoHud[ordem[j]] / 1e6 / n;
            if (ms < 0.05) break;
            hud.append(String.format(Locale.ROOT, "  %s %.2f", PARTES_HUD[ordem[j]], ms));
        }
        r.add(hud.toString());
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
            long somaTexto = 0, piorTexto = 0;
            for (long ns : temposDeTexto) {
                somaTexto += ns;
                piorTexto = Math.max(piorTexto, ns);
            }
            r.add(String.format(Locale.ROOT, "Criando textos: %.2f ms por quadro, pior quadro %.1f ms, %.0f ms no total",
                somaTexto / 1e6 / n, piorTexto / 1e6, somaTexto / 1e6));
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
            float placaAqui = i < placa.size() ? placa.get(i) : -1f, placaAntes = i > 0 && i - 1 < placa.size() ? placa.get(i - 1) : -1f;
            piores.append(String.format(Locale.ROOT, "  %.0f ms (%s %.0f, %s, %.1f telas, textos %d em %.0f ms, placa %s/%s antes%s)", tempos[i],
                PerfilQuadros.NOMES[maior], q[maior] / 1e6, MOMENTOS[momento(contextos.get(i))], gl.get(i)[3] / 10.0, gl.get(i)[0],
                temposDeTexto.get(i) / 1e6,
                placaAqui < 0f ? "?" : String.format(Locale.ROOT, "%.1f", placaAqui),
                placaAntes < 0f ? "?" : String.format(Locale.ROOT, "%.1f", placaAntes),
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
