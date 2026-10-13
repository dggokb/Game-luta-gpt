package com.gamelutagpt.historia;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Diagramações de página (coordenadas de tela 1280x720). Cada painel é um quadrilátero
 * {x0, y0, x1, y1, x2, y2, x3, y3} no sentido horário a partir do canto de cima à esquerda;
 * as divisas inclinadas dão o ritmo de mangá de luta, como na Página Final do ultra.
 */
public final class Layouts {
    private static final Map<String, float[][]> LAYOUTS = new LinkedHashMap<>();

    static {
        LAYOUTS.put("splash", new float[][] {
            {28f, 28f, 1252f, 28f, 1252f, 692f, 28f, 692f}
        });
        LAYOUTS.put("duas-colunas", new float[][] {
            {28f, 28f, 660f, 28f, 612f, 692f, 28f, 692f},
            {680f, 28f, 1252f, 28f, 1252f, 692f, 632f, 692f}
        });
        LAYOUTS.put("faixa-e-dois", new float[][] {
            {28f, 28f, 1252f, 28f, 1252f, 252f, 28f, 282f},
            {28f, 302f, 700f, 274f, 660f, 692f, 28f, 692f},
            {720f, 272f, 1252f, 264f, 1252f, 692f, 680f, 692f}
        });
        LAYOUTS.put("dois-e-faixa", new float[][] {
            {28f, 28f, 640f, 28f, 610f, 418f, 28f, 440f},
            {660f, 28f, 1252f, 28f, 1252f, 398f, 630f, 417f},
            {28f, 460f, 630f, 437f, 1252f, 418f, 1252f, 692f, 28f, 692f}
        });
        LAYOUTS.put("tres-faixas", new float[][] {
            {28f, 28f, 1252f, 28f, 1252f, 220f, 28f, 240f},
            {28f, 258f, 1252f, 238f, 1252f, 452f, 28f, 470f},
            {28f, 488f, 1252f, 470f, 1252f, 692f, 28f, 692f}
        });
        LAYOUTS.put("tres-colunas", new float[][] {
            {28f, 28f, 440f, 28f, 400f, 692f, 28f, 692f},
            {460f, 28f, 852f, 28f, 830f, 692f, 420f, 692f},
            {872f, 28f, 1252f, 28f, 1252f, 692f, 850f, 692f}
        });
        LAYOUTS.put("quatro", new float[][] {
            {28f, 28f, 1252f, 28f, 1252f, 206f, 28f, 246f},
            {28f, 266f, 470f, 256f, 420f, 692f, 28f, 692f},
            {490f, 255f, 1252f, 226f, 1252f, 452f, 462f, 470f},
            {460f, 490f, 1252f, 472f, 1252f, 692f, 440f, 692f}
        });
        LAYOUTS.put("grade", new float[][] {
            {28f, 28f, 650f, 28f, 630f, 350f, 28f, 360f},
            {670f, 28f, 1252f, 28f, 1252f, 340f, 650f, 350f},
            {28f, 380f, 630f, 370f, 610f, 692f, 28f, 692f},
            {650f, 368f, 1252f, 360f, 1252f, 692f, 630f, 692f}
        });
    }

    private Layouts() {}

    /** Polígonos do layout, ou erro com a lista de nomes válidos. */
    public static float[][] get(String name) {
        float[][] layout = LAYOUTS.get(name);
        if (layout == null) {
            throw new IllegalArgumentException("layout desconhecido: " + name + " (use " + LAYOUTS.keySet() + ")");
        }
        return layout;
    }

    /** Layout usado quando a página não diz qual: depende só da quantidade de painéis. */
    public static String padrao(int paineis) {
        switch (paineis) {
            case 1: return "splash";
            case 2: return "duas-colunas";
            case 3: return "faixa-e-dois";
            case 4: return "quatro";
            default: throw new IllegalArgumentException("página com " + paineis + " painéis: use de 1 a 4");
        }
    }

    public static Iterable<String> nomes() {
        return LAYOUTS.keySet();
    }

    /** Caixa {esquerda, topo, direita, base} de um polígono qualquer. */
    static float[] caixa(float[] poly) {
        float l = Float.MAX_VALUE, t = Float.MAX_VALUE, r = -Float.MAX_VALUE, b = -Float.MAX_VALUE;
        for (int i = 0; i + 1 < poly.length; i += 2) {
            l = Math.min(l, poly[i]);
            r = Math.max(r, poly[i]);
            t = Math.min(t, poly[i + 1]);
            b = Math.max(b, poly[i + 1]);
        }
        return new float[] {l, t, r, b};
    }
}
