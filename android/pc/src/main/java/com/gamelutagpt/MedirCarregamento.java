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
}
