package com.gamelutagpt;

/**
 * Câmera do Ultra em sequência (o "diretor"): a cada golpe que acerta a câmera corta seco para
 * outro plano, como nos Ultras do FighterZ. Os planos saem dos quadros em que cada golpe da
 * animação acerta ({@link CharacterDefinition.UltraSequence#hits}), então todo Ultra em
 * sequência ganha a montagem sem configurar nada:
 *
 * <ul>
 *   <li>antes do 1º golpe: plano médio nos dois;</li>
 *   <li>do 1º ao penúltimo golpe: close de lado, câmera baixa e plano aberto, em rodízio;</li>
 *   <li>o último plano antes do golpe final é o close de preparação (a página de mangá entra
 *       logo depois);</li>
 *   <li>depois do golpe final: a câmera da luta volta (com a câmera lenta do fim).</li>
 * </ul>
 *
 * O rodízio do meio vem do roteiro de câmera do Ultra ({@code "estilo": {"camera": ...}} no
 * ultra.json): um dos roteiros prontos ({@link #PRESETS}) ou a lista de planos escrita à mão.
 *
 * Só apresentação; Java puro.
 */
final class UltraDirector {
    static final int OPEN = 0, SIDE = 1, LOW = 2, WIDE = 3, CLOSE = 4, FINAL = 5;
    /** Nomes dos planos na lista do ultra.json, na ordem das constantes. */
    static final String[] SHOT_NAMES = {"medio", "lado", "baixo", "aberto", "close"};
    static final String[] PRESETS = {"padrao", "agil", "pesada", "sombria"};

    /**
     * Roteiro de câmera: o rodízio de planos entre o 1º golpe e o close final, quanto cada plano
     * inclina e aproxima, se a inclinação troca de lado a cada corte e o escuro do fundo.
     */
    static final class Script {
        final int[] cycle;
        final float tiltScale, zoomScale;
        final boolean alternate;
        final int darkAlpha;

        Script(int[] cycle, float tiltScale, float zoomScale, boolean alternate, int darkAlpha) {
            this.cycle = cycle;
            this.tiltScale = tiltScale;
            this.zoomScale = zoomScale;
            this.alternate = alternate;
            this.darkAlpha = darkAlpha;
        }
    }

    /** Close de lado, câmera baixa, plano aberto. */
    static final Script PADRAO = new Script(new int[] {SIDE, LOW, WIDE}, 1f, 1f, false, 120);
    /** Ágil: planos abertos e a câmera girando de um lado para o outro a cada golpe. */
    static final Script AGIL = new Script(new int[] {WIDE, SIDE, WIDE, LOW}, 1.9f, 0.92f, true, 110);
    /** Pesada: câmera baixa e closes, quase reta. */
    static final Script PESADA = new Script(new int[] {LOW, SIDE, LOW, CLOSE}, 0.6f, 1f, false, 135);
    /** Sombria: planos abertos tortos e fundo bem escuro. */
    static final Script SOMBRIA = new Script(new int[] {WIDE, LOW, WIDE, SIDE}, 1.4f, 0.96f, true, 175);

    /** O roteiro do Ultra: a lista de planos, senão o roteiro pronto com esse nome, senão o padrão. */
    static Script script(String preset, String[] shots) {
        if (shots != null && shots.length > 0) {
            int[] cycle = new int[shots.length];
            int n = 0;
            for (String name : shots) {
                int kind = shotKind(name);
                if (kind >= 0) cycle[n++] = kind;
            }
            if (n > 0) return new Script(java.util.Arrays.copyOf(cycle, n), 1f, 1f, false, 120);
        }
        String key = preset == null ? "" : preset.trim().toLowerCase(java.util.Locale.ROOT);
        if ("agil".equals(key)) return AGIL;
        if ("pesada".equals(key)) return PESADA;
        if ("sombria".equals(key)) return SOMBRIA;
        return PADRAO;
    }

    static int shotKind(String name) {
        String key = name == null ? "" : name.trim().toLowerCase(java.util.Locale.ROOT);
        for (int i = 0; i < SHOT_NAMES.length; i++) if (SHOT_NAMES[i].equals(key)) return i;
        return -1;
    }

    static boolean isKnownPreset(String name) {
        if (name == null) return true;
        String key = name.trim().toLowerCase(java.util.Locale.ROOT);
        for (String p : PRESETS) if (p.equals(key)) return true;
        return false;
    }

    /**
     * Zoom (sobre o da luta), inclinação (graus, a favor de quem bate) e ponto na tela. Mais
     * aberto que antes (era 1,25 / 1,65 / 1,9 / 1,3 / 2,15): o close do último golpe ficava
     * colado demais e os outros planos cortavam os corpos.
     */
    static final float[] ZOOM = {1.15f, 1.4f, 1.55f, 1.15f, 1.45f, 1f};
    static final float[] TILT = {0f, -6f, 9f, -3f, 4f, 0f};
    /** Aproximação lenta dentro de cada plano (até +4%). */
    static final float PUSH = 0.04f, PUSH_FRAMES = 40f;

    /** Um plano: zoom, inclinação, ponto do mundo que vai para (anchorX, anchorY) da tela. */
    static final class Shot {
        int kind;
        float zoom = 1f, tilt, focusX, focusY, anchorX = 0.5f, anchorY = 0.5f;
    }

    /** Qual plano vale no quadro {@code frame} da sequência (roteiro padrão). */
    static int kind(int[] hits, int frame) {
        return kind(hits, frame, PADRAO);
    }

    static int kind(int[] hits, int frame, Script script) {
        int done = cuts(hits, frame);
        if (done == 0) return OPEN;
        if (done >= hits.length) return FINAL;
        if (done == hits.length - 1) return CLOSE;
        return script.cycle[(done - 1) % script.cycle.length];
    }

    private static int cuts(int[] hits, int frame) {
        int done = 0;
        while (done < hits.length && hits[done] <= frame) done++;
        return done;
    }

    /**
     * Preenche {@code out} para o quadro dado. (ax, ay): peito do atacante; (vx, vy): peito de
     * quem apanha; {@code facing}: para onde o atacante olha.
     */
    static void shot(Shot out, int[] hits, int frame, float ax, float ay, float vx, float vy, int facing) {
        shot(out, hits, frame, ax, ay, vx, vy, facing, PADRAO);
    }

    static void shot(Shot out, int[] hits, int frame, float ax, float ay, float vx, float vy, int facing,
                     Script script) {
        int kind = kind(hits, frame, script);
        int cutAt = 0;
        for (int hit : hits) if (hit <= frame) cutAt = hit;
        float push = 1f + PUSH * Math.min(1f, (frame - cutAt) / PUSH_FRAMES);
        out.kind = kind;
        out.zoom = kind == FINAL ? 1f : Math.max(1f, ZOOM[kind] * script.zoomScale) * push;
        float sway = script.alternate && cuts(hits, frame) % 2 == 0 ? -1f : 1f;
        out.tilt = TILT[kind] * (facing >= 0 ? 1f : -1f) * (kind == CLOSE ? 1f : script.tiltScale * sway);
        float midX = (ax + vx) * 0.5f, midY = (ay + vy) * 0.5f;
        out.anchorX = 0.5f;
        out.anchorY = 0.52f;
        switch (kind) {
            case LOW:
                // Câmera no chão olhando para cima: o golpe fica alto na tela.
                out.focusX = midX;
                out.focusY = vy + 40f;
                out.anchorY = 0.44f;
                break;
            case CLOSE:
                // Preparação: o rosto e o tronco de quem bate, um pouco para trás do centro.
                out.focusX = ax;
                out.focusY = ay - 30f;
                out.anchorX = 0.5f - 0.1f * (facing >= 0 ? 1f : -1f);
                out.anchorY = 0.5f;
                break;
            default:
                out.focusX = midX;
                out.focusY = midY;
                break;
        }
    }
}
