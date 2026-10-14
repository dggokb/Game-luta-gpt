package com.gamelutagpt.historia;

import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Retratos pequenos dos lutadores ({@code historia/torneio/rostos/pNN.jpg}, gerados por
 * {@code tools/historia/rostos_torneio.py}) para a chave do torneio e a escolha do irmão.
 * Carrega na primeira vez; sem o arquivo, desenha um quadro com o código.
 */
final class Rostos {
    static final String PASTA = Temporada.PASTA + "/torneio/rostos/";

    private final RenderAssets assets;
    private final Map<String, RenderImage> cache = new HashMap<>();

    Rostos(RenderAssets assets) {
        this.assets = assets;
    }

    /** Código do retrato de um lutador (o pack player_base é o p01). */
    static String codigo(String lutador) {
        return "player_base".equals(lutador) ? "p01" : lutador;
    }

    RenderImage get(String lutador) {
        String codigo = codigo(lutador);
        if (cache.containsKey(codigo)) return cache.get(codigo);
        RenderImage image = null;
        try {
            image = assets.loadImage(PASTA + codigo + ".jpg");
        } catch (IOException | RuntimeException ex) {
            // Sem retrato: o quadro com o código.
        }
        cache.put(codigo, image);
        return image;
    }

    void liberar() {
        cache.clear();
    }

    /** Retrato no quadrado (l, t, lado), com borda. */
    void draw(RenderCanvas c, String lutador, float l, float t, float lado, int borda, float espessura, int alpha) {
        RenderImage image = get(lutador);
        float[] quadro = {l, t, l + lado, t, l + lado, t + lado, l, t + lado};
        if (image != null) {
            c.drawImage(image, l, t, l + lado, t + lado, alpha);
        } else {
            c.fillPolygon(quadro, Tinta.withAlpha(0xFF2A2F3D, alpha));
            String texto = codigo(lutador).toUpperCase(java.util.Locale.ROOT);
            c.drawText(texto, l + lado * 0.5f, t + lado * 0.6f, lado * 0.3f, Tinta.withAlpha(0xFFE9E1CF, alpha), 0, 0f, true);
        }
        c.strokePolygon(quadro, espessura, Tinta.withAlpha(borda, alpha));
    }

    /** Nome curto do lutador: o do elenco da história (Ryūga, Kōga) ou o código (P02). */
    static String nome(Temporada temporada, String lutador) {
        if (temporada != null) {
            for (Temporada.Personagem p : temporada.elenco.values()) {
                if (lutador.equals(p.lutador)) return p.nome.toUpperCase(java.util.Locale.ROOT);
            }
        }
        return codigo(lutador).toUpperCase(java.util.Locale.ROOT);
    }

    /** "RYŪGA · P02". */
    static String nomeDupla(Temporada temporada, Torneio.Dupla dupla) {
        return nome(temporada, dupla.lutadores[0]) + " · " + nome(temporada, dupla.lutadores[1]);
    }
}
