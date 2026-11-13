package com.gamelutagpt.historia;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Animação feita à mão numa cena da luta (a ação {@code animar} da {@link CinematicaLuta}): o
 * motor de luta para e cada lutador segue os quadros-chave do roteiro, como num programa de
 * animação. Cada chave diz, num quadro da cena (60 por segundo), a pose (qualquer quadro de
 * qualquer animação do lutador), a posição, o esticamento, a rotação e o rastro; entre duas
 * chaves os números são interpolados com a curva da chave seguinte. Assim o tempo é do roteiro
 * (antecipação segurando, o golpe saindo de uma vez, o contato parado), e não o do golpe do jogo.
 *
 * <pre>
 * {"acao": "animar", "quadros": 120,
 *  "rival":   [{"q": 0, "x": 160, "pose": "IDLE:0", "ciclo": 12},
 *              {"q": 30, "x": 40, "pose": "DASH:5", "sx": 1.4, "sy": 0.8, "rastro": 4, "curva": "sai"}],
 *  "jogador": [{"q": 0, "x": -160, "pose": "IDLE:0"}],
 *  "eventos": [{"q": 30, "acao": {"acao": "impacto", "forca": 3, "quem": "rival"}}]}
 * </pre>
 *
 * x: distância do meio entre os dois no começo, ao longo do eixo do jogador para o rival
 * (negativo, para o lado do jogador); y: altura acima do chão. Cada um olha para o outro (ou
 * para o lado contrário com {@code "vira": true}).
 */
public final class AnimacaoManual {
    /** A pose de um lutador num quadro da cena, já interpolada (x relativo ao meio, no eixo). */
    public static final class Pose {
        public String clip = "IDLE";
        public int quadro;
        public float x, altura, sx = 1f, sy = 1f, graus, alfa = 1f;
        public int rastro;
        public boolean vira;
    }

    /** Uma chave da pose. */
    static final class Chave {
        int q;
        float x, y, sx = 1f, sy = 1f, graus, alfa = 1f;
        String clip = "IDLE";
        int quadro;
        /** O quadro final do clipe até a próxima chave (anima de {@link #quadro} até ele), ou -1. */
        int ate = -1;
        /** Toca o clipe em laço, nesse ritmo (quadros do clipe por segundo), ou 0. */
        float ciclo;
        int rastro;
        boolean vira;
        /** Como chega nesta chave: linear, sai (rápido no começo), entra (rápido no fim), corte (pula). */
        String curva = "linear";
    }

    /** Um evento num quadro da animação: uma ação qualquer do roteiro (impacto, câmera, letreiro...). */
    static final class Evento {
        int q;
        CinematicaLuta.Acao acao;
    }

    final int quadros;
    final List<Chave> jogador, rival;
    final List<Evento> eventos = new ArrayList<>();

    AnimacaoManual(int quadros, List<Chave> jogador, List<Chave> rival) {
        this.quadros = quadros;
        this.jogador = jogador;
        this.rival = rival;
    }

    static List<Chave> chaves(List<Object> lista, String quem) {
        List<Chave> out = new ArrayList<>();
        int antes = -1;
        for (Object o : lista) {
            Map<String, Object> m = Episodio.asMap(o);
            Chave c = new Chave();
            c.q = (int)num(m, "q", 0f);
            if (c.q < antes) throw new IllegalArgumentException("animar: as chaves de " + quem + " fora de ordem (q " + c.q + ")");
            antes = c.q;
            c.x = num(m, "x", 0f);
            c.y = num(m, "y", 0f);
            c.sx = num(m, "sx", 1f);
            c.sy = num(m, "sy", 1f);
            c.graus = num(m, "rot", 0f);
            c.alfa = num(m, "alfa", 1f);
            c.rastro = (int)num(m, "rastro", 0f);
            c.vira = Boolean.TRUE.equals(m.get("vira"));
            c.ciclo = num(m, "ciclo", 0f);
            c.ate = (int)num(m, "ate", -1f);
            c.curva = Episodio.string(m, "curva", "linear");
            if (!c.curva.matches("linear|sai|entra|corte|suave")) {
                throw new IllegalArgumentException("animar: curva " + c.curva + " (use linear, sai, entra, suave ou corte)");
            }
            String pose = Episodio.string(m, "pose", null);
            if (pose == null) throw new IllegalArgumentException("animar: chave de " + quem + " sem \"pose\" (ex.: \"DASH:5\")");
            int dois = pose.indexOf(':');
            c.clip = dois < 0 ? pose : pose.substring(0, dois);
            c.quadro = dois < 0 ? 0 : Integer.parseInt(pose.substring(dois + 1));
            out.add(c);
        }
        if (out.isEmpty()) throw new IllegalArgumentException("animar: " + quem + " sem chaves");
        return out;
    }

    private static float num(Map<String, Object> m, String chave, float padrao) {
        Object v = m.get(chave);
        return v instanceof Number ? ((Number)v).floatValue() : padrao;
    }

    /** A pose de {@code chaves} no quadro {@code q} (fracionário) da animação. */
    static void amostra(List<Chave> chaves, float q, Pose out) {
        Chave a = chaves.get(0), b = null;
        for (int i = 0; i < chaves.size(); i++) {
            Chave c = chaves.get(i);
            if (c.q <= q) {
                a = c;
                b = i + 1 < chaves.size() ? chaves.get(i + 1) : null;
            }
        }
        float t = 0f;
        if (b != null && b.q > a.q) t = curva(b.curva, Math.max(0f, Math.min(1f, (q - a.q) / (b.q - a.q))));
        out.x = lerp(a.x, b != null ? b.x : a.x, t);
        out.altura = lerp(a.y, b != null ? b.y : a.y, t);
        out.sx = lerp(a.sx, b != null ? b.sx : a.sx, t);
        out.sy = lerp(a.sy, b != null ? b.sy : a.sy, t);
        out.graus = lerp(a.graus, b != null ? b.graus : a.graus, t);
        out.alfa = lerp(a.alfa, b != null ? b.alfa : a.alfa, t);
        out.rastro = a.rastro;
        out.vira = a.vira;
        out.clip = a.clip;
        float desde = Math.max(0f, q - a.q);
        if (a.ciclo > 0f) {
            out.quadro = a.quadro + (int)(desde * a.ciclo / 60f);
        } else if (a.ate >= 0 && b != null && b.q > a.q) {
            // Anima do quadro da chave até "ate" no tempo até a próxima (linear, quadro a quadro).
            float k = Math.min(1f, desde / (b.q - a.q));
            out.quadro = Math.round(a.quadro + (a.ate - a.quadro) * k);
        } else {
            out.quadro = a.quadro;
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** A curva de chegada: sai (rápido no começo e freando), entra (acelerando), suave, corte. */
    static float curva(String nome, float t) {
        switch (nome) {
            case "sai": return 1f - (1f - t) * (1f - t) * (1f - t);
            case "entra": return t * t * t;
            case "suave": return t * t * (3f - 2f * t);
            case "corte": return t >= 1f ? 1f : 0f;
            default: return t;
        }
    }
}
