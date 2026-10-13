package com.gamelutagpt.stage;

import com.gamelutagpt.render.MiniJson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Um personagem de fundo do cenário (quem assiste à luta: o mestre, o pai...), lido do
 * {@code ator.json} da pasta dele. Cada animação é uma folha de quadros do mesmo tamanho; o
 * ator fica na animação parada e toca as outras uma vez, quando o jogo pede uma reação.
 *
 * <pre>
 * {
 *   "nome": "Dafu",
 *   "parado": "parado",
 *   "animacoes": {
 *     "parado":   { "folha": "parado.png", "quadros": 24, "colunas": 6, "fps": 12, "loop": true },
 *     "aplaudir": { "folha": "aplaudir.png", "quadros": 30, "colunas": 6, "fps": 15 }
 *   },
 *   "reacoes": { "licao": ["aplaudir"], "impacto": ["surpreso"], "levou": ["rir"], "fim": ["aplaudir"] }
 * }
 * </pre>
 *
 * Animação sem folha (o vídeo ainda não chegou) usa os quadros da parada com um movimento
 * provisório pelo nome ({@link #provisorio}), para dar para testar as reações antes da arte.
 */
public final class StageActor {
    public static final String DEFINITION_FILE = "ator.json";

    public static final class Animation {
        public final String name;
        /** Folha relativa à pasta do ator, ou null (sem arte ainda). */
        public final String sheet;
        public final int frames;
        public final int columns;
        public final float fps;
        public final boolean loop;
        /** Duração quando não tem folha (movimento provisório). */
        public final float fallbackSeconds;

        Animation(String name, String sheet, int frames, int columns, float fps, boolean loop, float fallbackSeconds) {
            this.name = name;
            this.sheet = sheet;
            this.frames = frames;
            this.columns = columns;
            this.fps = fps;
            this.loop = loop;
            this.fallbackSeconds = fallbackSeconds;
        }

        public float duration() {
            return sheet == null ? fallbackSeconds : frames / fps;
        }
    }

    public final String name;
    public final String idle;
    public final Map<String, Animation> animations;
    /** Evento do jogo ("licao", "impacto", "levou", "fim"...) → animações possíveis. */
    public final Map<String, List<String>> reactions;

    private StageActor(String name, String idle, Map<String, Animation> animations, Map<String, List<String>> reactions) {
        this.name = name;
        this.idle = idle;
        this.animations = Collections.unmodifiableMap(animations);
        this.reactions = Collections.unmodifiableMap(reactions);
    }

    @SuppressWarnings("unchecked")
    public static StageActor fromJson(String json) {
        Object root = MiniJson.parse(json);
        if (!(root instanceof Map)) throw new IllegalArgumentException("ator.json deve ser um objeto");
        Map<String, Object> map = (Map<String, Object>)root;
        String name = map.get("nome") instanceof String ? (String)map.get("nome") : "ator";
        String idle = map.get("parado") instanceof String ? (String)map.get("parado") : "parado";

        Map<String, Animation> animations = new LinkedHashMap<>();
        Object animsField = map.get("animacoes");
        if (animsField instanceof Map) {
            for (Map.Entry<String, Object> e : ((Map<String, Object>)animsField).entrySet()) {
                if (!(e.getValue() instanceof Map)) throw new IllegalArgumentException("animação " + e.getKey() + " deve ser um objeto");
                Map<String, Object> a = (Map<String, Object>)e.getValue();
                String sheet = a.get("folha") instanceof String ? (String)a.get("folha") : null;
                int frames = Math.max(1, Math.round(number(a, "quadros", 1f)));
                int columns = Math.max(1, Math.round(number(a, "colunas", frames)));
                float fps = number(a, "fps", 12f);
                if (fps <= 0f) throw new IllegalArgumentException("animação " + e.getKey() + ": fps deve ser positivo");
                animations.put(e.getKey(), new Animation(e.getKey(), sheet, frames, Math.min(columns, frames), fps,
                    Boolean.TRUE.equals(a.get("loop")), number(a, "segundos", 1.4f)));
            }
        }
        if (!animations.containsKey(idle)) {
            throw new IllegalArgumentException("ator " + name + " sem a animação parada \"" + idle + "\"");
        }
        if (animations.get(idle).sheet == null) {
            throw new IllegalArgumentException("ator " + name + ": a animação parada precisa de \"folha\"");
        }

        Map<String, List<String>> reactions = new LinkedHashMap<>();
        Object reactField = map.get("reacoes");
        if (reactField instanceof Map) {
            for (Map.Entry<String, Object> e : ((Map<String, Object>)reactField).entrySet()) {
                List<String> list = new ArrayList<>();
                if (e.getValue() instanceof List) {
                    for (Object item : (List<Object>)e.getValue()) list.add(String.valueOf(item));
                } else if (e.getValue() instanceof String) {
                    list.add((String)e.getValue());
                }
                for (String anim : list) {
                    if (!animations.containsKey(anim)) {
                        throw new IllegalArgumentException("ator " + name + ": reação " + e.getKey() + " usa a animação \"" +
                            anim + "\", que não existe");
                    }
                }
                reactions.put(e.getKey(), Collections.unmodifiableList(list));
            }
        }
        return new StageActor(name, idle, animations, reactions);
    }

    /**
     * Movimento provisório de uma animação sem folha, em fração da altura do ator:
     * {deslocamento x, deslocamento y (negativo = sobe), escala}. {@code k} vai de 0 a 1.
     */
    static float[] provisorio(String name, float k) {
        float wave = (float)Math.sin(k * Math.PI);
        switch (name) {
            case "aplaudir":
                return new float[] {0f, -0.012f * Math.abs((float)Math.sin(k * Math.PI * 8f)), 1f};
            case "rir":
                return new float[] {0.006f * (float)Math.sin(k * Math.PI * 14f), -0.01f * wave, 1f + 0.02f * wave};
            case "surpreso":
                return new float[] {0f, -0.06f * (float)Math.sin(Math.min(1f, k * 2.5f) * Math.PI), 1f + 0.04f * wave};
            case "aprovar":
                return new float[] {0f, 0.01f * Math.abs((float)Math.sin(k * Math.PI * 4f)), 1f};
            default:
                return new float[] {0f, -0.02f * wave, 1f};
        }
    }

    private static float number(Map<String, Object> map, String key, float fallback) {
        Object v = map.get(key);
        if (v == null) return fallback;
        if (!(v instanceof Number)) throw new IllegalArgumentException("\"" + key + "\" deve ser um número");
        return ((Number)v).floatValue();
    }
}
