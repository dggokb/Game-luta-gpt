package com.gamelutagpt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import com.gamelutagpt.pc.DadosDoJogo;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * T26 da auditoria (docs/balanco/auditoria.md), no PC: o tempo de desenho de cada quadro da luta
 * (GameView.renderFrame, o mesmo desenho do celular sobre o Java2D) e o passo da simulação, nos
 * cenários do documento: luta comum, Super, vários projéteis, SHINKAI Clash, duplas com assist em
 * campo e Ultra. Uma segunda rodada de cada cenário amostra a pilha da thread a cada ~1 ms para
 * dizer quais partes do desenho pesam. Não é o celular: serve para comparar cenários entre si.
 *
 * <pre>java ... com.gamelutagpt.BateriaQuadros saida/ [largura altura]</pre>
 */
public final class BateriaQuadros {
    private BateriaQuadros() {}

    static GameView jogo;
    static Method av, dr, sel, aiOn;
    static Field intro;
    static Bitmap tela;
    static int largura = 1280, altura = 720;

    /** O que o "jogador" (o lado de baixo, pelo pad) faz em cada quadro do cenário. */
    interface Roteiro {
        void quadro(int q, Random r);
    }

    static void novaLuta(String p1, String p2, String c1, String c2, boolean batalha) throws Exception {
        aiOn.invoke(jogo, batalha);
        sel.invoke(jogo, p1, p2, c1, c2, !batalha);
        jogo.startIntro();
    }

    static long[] medir(String nome, int aquecer, int quadros, Roteiro roteiro, boolean perfil, Map<String, Integer> amostras,
                        File png) throws Exception {
        Canvas c = new Canvas(tela);
        Random r = new Random(7);
        long[] desenho = new long[quadros], passo = new long[quadros];
        Thread principal = Thread.currentThread();
        final boolean[] ligado = {false};
        final boolean[] fim = {false};
        Thread amostrador = null;
        if (perfil) {
            amostrador = new Thread(() -> {
                while (!fim[0]) {
                    if (ligado[0]) {
                        StackTraceElement[] st = principal.getStackTrace();
                        String chave = parteDoDesenho(st);
                        if (chave != null) synchronized (amostras) { amostras.merge(chave, 1, Integer::sum); }
                    }
                    try { Thread.sleep(1); } catch (InterruptedException e) { return; }
                }
            });
            amostrador.setDaemon(true);
            amostrador.start();
        }
        for (int q = 0; q < aquecer + quadros; q++) {
            roteiro.quadro(q - aquecer, r);
            long t0 = System.nanoTime();
            av.invoke(jogo, CombatConfig.DT);
            long t1 = System.nanoTime();
            c.drawColor(0xFF000000);
            ligado[0] = q >= aquecer;
            dr.invoke(jogo, c);
            ligado[0] = false;
            long t2 = System.nanoTime();
            if (q >= aquecer) {
                passo[q - aquecer] = t1 - t0;
                desenho[q - aquecer] = t2 - t1;
                if (png != null && q - aquecer == quadros / 2) ImageIO.write(tela.image(), "png", png);
            }
        }
        fim[0] = true;
        if (amostrador != null) amostrador.join();
        long[] r2 = new long[quadros * 2];
        System.arraycopy(desenho, 0, r2, 0, quadros);
        System.arraycopy(passo, 0, r2, quadros, quadros);
        return r2;
    }

    /** A parte do desenho na pilha: o método chamado direto pelo renderFrame (e o de baixo dele). */
    static String parteDoDesenho(StackTraceElement[] st) {
        for (int i = st.length - 1; i > 0; i--) {
            if (st[i].getMethodName().equals("renderFrame") && st[i].getClassName().endsWith("GameView")) {
                // Desce até o primeiro método do jogo abaixo do renderFrame.
                StackTraceElement filho = st[i - 1];
                String nome = simples(filho);
                // Dentro de drawFight (o mundo), um nível a mais diz o que é.
                if (i >= 2 && (filho.getMethodName().startsWith("drawFight") || filho.getMethodName().equals("drawWorld"))) {
                    for (int k = i - 2; k >= 0; k--) {
                        if (st[k].getClassName().startsWith("com.gamelutagpt")) return nome + " > " + simples(st[k]);
                    }
                }
                return nome;
            }
        }
        return null;
    }

    static String simples(StackTraceElement e) {
        String c = e.getClassName();
        return c.substring(c.lastIndexOf('.') + 1) + "." + e.getMethodName();
    }

    static String resumo(long[] ns, int de, int n) {
        List<Long> v = new ArrayList<>();
        double soma = 0;
        for (int i = de; i < de + n; i++) { v.add(ns[i]); soma += ns[i]; }
        Collections.sort(v);
        double media = soma / n / 1e6;
        double p95 = v.get((int)Math.round(0.95 * (n - 1))) / 1e6, p99 = v.get((int)Math.round(0.99 * (n - 1))) / 1e6;
        double max = v.get(n - 1) / 1e6;
        int acima = 0;
        for (long x : v) if (x > 16_666_667L) acima++;
        return String.format(Locale.ROOT, "%.2f | %.2f | %.2f | %.2f | %d", media, p95, p99, max, acima);
    }

    public static void main(String[] args) throws Exception {
        File saida = new File(args[0]);
        saida.mkdirs();
        if (args.length > 2) { largura = Integer.parseInt(args[1]); altura = Integer.parseInt(args[2]); }
        Context ctx = DadosDoJogo.localizar().contexto(null);
        jogo = new GameView(ctx);
        jogo.setTouchControlsVisible(true);
        av = GameView.class.getDeclaredMethod("advanceSimulation", float.class);
        av.setAccessible(true);
        dr = GameView.class.getDeclaredMethod("renderFrame", Canvas.class);
        dr.setAccessible(true);
        sel = GameView.class.getDeclaredMethod("selectTeamsNow", String.class, String.class, String.class, String.class, boolean.class);
        sel.setAccessible(true);
        aiOn = GameView.class.getDeclaredMethod("setOpponentAiEnabled", boolean.class);
        aiOn.setAccessible(true);
        intro = GameView.class.getDeclaredField("introFrame");
        intro.setAccessible(true);
        // Mesmo tamanho de tela do jogo no PC (a GameView desenha na resolução da superfície).
        Method sz = GameView.class.getDeclaredMethod("onSizeChanged", int.class, int.class, int.class, int.class);
        sz.setAccessible(true);
        sz.invoke(jogo, largura, altura, 0, 0);
        tela = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888);
        PadInput pad = jogo.pad;
        final int N = 900, AQ = 420;

        // Jogador que anda para a frente e solta L/M/H de vez em quando (a luta comum).
        Roteiro comum = (q, r) -> {
            if (q % 40 == 0) pad.setDirection(r.nextInt(3) == 0 ? 0 : 1);
            if (q % 23 == 0) pad.press(r.nextInt(3) == 0 ? PadInput.Button.LIGHT : r.nextBoolean() ? PadInput.Button.MEDIUM : PadInput.Button.HEAVY);
        };
        Object[][] cenarios = {
            {"luta comum (2x2, CPU no outro lado)", "player_base", "p02", "p03", "p04", true, comum},
            {"Super (a cada 3 s)", "player_base", "p02", "p03", "p04", true, (Roteiro)(q, r) -> {
                jogo.engine.fighter(0).state.superMeter = CombatConfig.MAX_METER;
                if (q % 180 == 0) { pad.setDirection(1); }
                if (q % 180 == 30) { pad.setDirection(0); pad.press(PadInput.Button.SUPER); }
            }},
            {"vários projéteis (SHINKAI ENERGY, onda a cada 14 quadros)", "player_base", "p02", "p06", "p05", true, (Roteiro)(q, r) -> {
                FighterState st = jogo.engine.fighter(0).state;
                st.shinkaiFrames = 400;
                int f = ((q % 14) + 14) % 14;
                pad.setDirection(f == 0 ? 3 : f == 1 ? 2 : f == 2 ? 1 : 0);
                if (f == 2) pad.press(PadInput.Button.LIGHT);
            }},
            {"SHINKAI Clash", "p03", "p04", "player_base", "p02", true, (Roteiro)(q, r) -> {
                CombatEngine e = jogo.engine;
                if (e.shinkaiClash() == null && q % 30 == 0) {
                    FighterState o = e.fighter(1).state;
                    o.shinkaiFrames = Math.max(o.shinkaiFrames, 300);
                    FighterState me = e.fighter(0).state;
                    me.shinkai = CombatConfig.SHINKAI_MAX;
                    me.shinkaiUsed = false;
                    me.shinkaiFrames = 0;
                    pad.press(PadInput.Button.SHINKAI);
                }
                if (e.shinkaiClash() != null && r.nextInt(4) == 0) pad.press(PadInput.Button.LIGHT);
            }},
            {"duplas com assist em campo (TAG a cada 4,2 s)", "player_base", "p02", "p03", "p04", true, (Roteiro)(q, r) -> {
                comum.quadro(q, r);
                if (q % 250 == 5) pad.press(PadInput.Button.TAG);
            }},
            {"Ultra (página do mangá)", "p02", "player_base", "p03", "p04", true, (Roteiro)(q, r) -> {
                jogo.engine.fighter(0).state.superMeter = CombatConfig.MAX_METER;
                if (q % 450 == 0) { jogo.engine.fighter(0).x = jogo.engine.fighter(1).x - 160f * jogo.engine.fighter(0).facing; }
                if (q % 450 == 2) { pad.setDirection(3); pad.press(PadInput.Button.SUPER); }
                if (q % 450 == 4) pad.setDirection(0);
            }},
        };
        List<String> linhas = new ArrayList<>();
        linhas.add(String.format(Locale.ROOT, "PC (Java2D, sem GPU), tela %dx%d, %d quadros medidos por cenário depois de %d de aquecimento.",
            largura, altura, N, AQ));
        linhas.add("");
        linhas.add("| cenário | desenho: média ms | p95 | p99 | pior | quadros > 16,7 ms | passo da simulação: média ms | p95 | p99 | pior | > 16,7 ms |");
        linhas.add("|---|---|---|---|---|---|---|---|---|---|---|");
        List<String> perfis = new ArrayList<>();
        int k = 0;
        for (Object[] c : cenarios) {
            k++;
            String nome = (String)c[0];
            novaLuta((String)c[1], (String)c[2], (String)c[3], (String)c[4], (Boolean)c[5]);
            long[] t = medir(nome, AQ, N, (Roteiro)c[6], false, null, new File(saida, "t26_" + k + ".png"));
            linhas.add("| " + nome + " | " + resumo(t, 0, N) + " | " + resumo(t, N, N) + " |");
            System.out.println(linhas.get(linhas.size() - 1));
            // Segunda rodada, com a amostragem da pilha.
            Map<String, Integer> amostras = new HashMap<>();
            novaLuta((String)c[1], (String)c[2], (String)c[3], (String)c[4], (Boolean)c[5]);
            medir(nome, AQ, N, (Roteiro)c[6], true, amostras, null);
            int total = 0;
            for (int v : amostras.values()) total += v;
            final int tot = Math.max(1, total);
            StringBuilder sb = new StringBuilder("**" + nome + "** (" + total + " amostras): ");
            amostras.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue()).limit(8)
                .forEach(en -> sb.append(String.format(Locale.ROOT, "%s %.0f%%; ", en.getKey(), 100.0 * en.getValue() / tot)));
            perfis.add(sb.toString());
            System.out.println(sb);
        }
        linhas.add("");
        linhas.add("Onde o tempo de desenho vai (amostra da pilha a cada ~1 ms, o método chamado pelo renderFrame):");
        linhas.add("");
        for (String p : perfis) { linhas.add("- " + p); }
        Files.write(new File(saida, "t26.md").toPath(), linhas, StandardCharsets.UTF_8);
        System.exit(0);
    }
}
