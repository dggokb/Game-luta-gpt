package com.gamelutagpt;

import android.content.Context;
import com.gamelutagpt.pc.DadosDoJogo;
import java.util.Locale;

/**
 * Mede os carregamentos pesados no PC: o carregarArte inteiro (por parte, como o log
 * "Carregamento" do celular) e as folhas de cada lutador sozinhas (tempo e memória).
 *
 * <pre>gradle :pc:medirCarregamento [-Pquem=p02,p03]</pre>
 *
 * Roda sem a pré-carga paralela do PC (-Dgameluta.semPrecarga=true), como no celular.
 */
public final class MedirCarregamento {
    private MedirCarregamento() {}

    public static void main(String[] args) throws Exception {
        Context contexto = DadosDoJogo.localizar().contexto(null);
        if (args.length > 0 && args[0].equals("celular")) {
            celular(contexto, Double.parseDouble(args[1]), Double.parseDouble(args[2]), args[3].split(","));
            System.exit(0);
        }
        String quem = args.length > 0 ? args[0] : "player_base,p02,p03,p04,p05,p08";

        // 1) A arte da luta, como na primeira saída do menu (as mesmas partes do log do celular).
        long antes = System.nanoTime();
        GameView jogo = new GameView(contexto);
        System.out.printf(Locale.ROOT, "construcao+arte %.1fs%n", (System.nanoTime() - antes) / 1e9);
        System.out.println(MedicaoCarregamento.ultima());
        jogo.release();
        jogo = null;
        System.gc();

        // 2) Cada lutador sozinho: tempo das folhas e quanto ocupam decodificadas.
        SpriteAtlasCache cache = new SpriteAtlasCache(contexto);
        System.out.println("lutador  folhas  tempo   decodificado");
        for (String id : quem.split(",")) {
            CharacterDefinition c = GeneratedCharacters.get(id.trim());
            cache.retainOnly();
            System.gc();
            long t = System.nanoTime();
            cache.preload(c);
            double s = (System.nanoTime() - t) / 1e9;
            System.out.printf(Locale.ROOT, "%-8s %6d  %5.1fs  %6dMB%n", c.id, cache.size(), s, cache.decodedBytes() >> 20);
        }
        System.exit(0);
    }

    /**
     * O caminho do celular: a GameView nasce leve (menu na hora), o jogador fica {@code menu}
     * segundos no menu e sai (carregarArte, com a tela de carregamento); depois lê {@code paginas}
     * segundos de páginas da história antes de uma luta 2x2 com {@code luta} (selectTeamsNow,
     * com a tela de carregamento). Mede quanto cada tela de carregamento esperou.
     */
    private static void celular(Context contexto, double menu, double paginas, String[] luta) throws Exception {
        long t = System.nanoTime();
        GameView jogo = new GameView(contexto, true);
        System.out.printf(Locale.ROOT, "menu pronto em %.2fs%n", (System.nanoTime() - t) / 1e9);
        Thread.sleep(Math.round(menu * 1000));
        java.lang.reflect.Method arte = GameView.class.getDeclaredMethod("carregarArte", Context.class);
        arte.setAccessible(true);
        t = System.nanoTime();
        arte.invoke(jogo, contexto);
        System.out.printf(Locale.ROOT, "saida do menu (depois de %.0fs nele): carregamento %.2fs  [%s]%n",
            menu, (System.nanoTime() - t) / 1e9, MedicaoCarregamento.ultima());
        java.lang.reflect.Method ia = GameView.class.getDeclaredMethod("setOpponentAiEnabled", boolean.class);
        ia.setAccessible(true);
        ia.invoke(jogo, true);
        java.lang.reflect.Method antecipar = GameView.class.getDeclaredMethod("anteciparLutadores", String[].class);
        antecipar.setAccessible(true);
        if (paginas > 0) antecipar.invoke(jogo, (Object) luta);
        Thread.sleep(Math.round(paginas * 1000));
        java.lang.reflect.Method dupla = GameView.class.getDeclaredMethod("selectTeamsNow",
            String.class, String.class, String.class, String.class, boolean.class);
        dupla.setAccessible(true);
        t = System.nanoTime();
        dupla.invoke(jogo, luta[0], luta[1], luta[2], luta[3], false);
        System.out.printf(Locale.ROOT, "luta depois de %.0fs de paginas: carregamento %.2fs  [%s]%n",
            paginas, (System.nanoTime() - t) / 1e9, MedicaoCarregamento.ultima());
        jogo.release();
    }
}
