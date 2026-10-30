package com.gamelutagpt;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tamanho de cada folha de sprite ajustado no olho (DEBUG > AJUSTAR TAMANHOS). É só uma
 * prévia: o renderer multiplica a folha por este fator, em volta da raiz. SALVAR exporta os
 * fatores (Drive) e {@code tools/sprites/regua.py --aplicar} grava o tamanho na folha; o APK
 * seguinte já nasce do tamanho certo.
 *
 * A chave é a folha com as dimensões do atlas (e, no P01, o fator gravado na calibração):
 * depois que o ajuste é gravado, a chave muda e o ajuste antigo deixa de valer sozinho.
 * Várias animações podem usar a mesma folha (IDLE e COMBAT, agarrão...): o ajuste é da folha.
 */
final class AjusteTamanho {
    static final float MIN = .70f, MAX = 1.40f, STEP = .01f;
    private static final String PREF = "ajuste_tamanho_v1";
    /** Painel antigo só do P01 (DASH, 2H, VITÓRIA): os valores são trazidos uma vez. */
    private static final String LEGACY = "p01_visual_scale_v1";
    private static final Map<String,Float> values = new LinkedHashMap<>();
    private static SharedPreferences prefs;

    /** Uma folha de um personagem e as animações que a usam. */
    static final class Folha {
        final CharacterDefinition.Atlas atlas;
        final List<String> animacoes = new ArrayList<>();
        Folha(CharacterDefinition.Atlas atlas) { this.atlas = atlas; }
        String nome() { return String.join(" / ", animacoes); }
    }

    static synchronized void load(Context context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        values.clear();
        for (Map.Entry<String,?> e : prefs.getAll().entrySet())
            if (e.getValue() instanceof Float) values.put(e.getKey(), (Float)e.getValue());
        SharedPreferences legacy = context.getSharedPreferences(LEGACY, Context.MODE_PRIVATE);
        if (!legacy.getAll().isEmpty()) {
            for (Folha f : folhas(GeneratedCharacters.defaultCharacter())) {
                float old = legacy.getFloat(f.atlas.resource, 1f);
                if (old != 1f && !values.containsKey(key(f.atlas))) set(f.atlas, old);
            }
            legacy.edit().clear().apply();
        }
    }

    static String key(CharacterDefinition.Atlas a) {
        // O P01 grava o ajuste na calibração (a folha não muda de tamanho): o fator gravado
        // entra na chave para o ajuste antigo do aparelho não valer duas vezes.
        float baked = P01SpriteCalibration.tuned(a.resource);
        return a.resource + "@" + a.width + "x" + a.height + "#" + a.count
            + (baked == 1f ? "" : String.format(Locale.US, "~%.4f", baked));
    }

    /** A chave de cada folha, montada uma vez (era montada a cada desenho, com String.format no P01). */
    private static final java.util.Map<CharacterDefinition.Atlas, String> chaves = new java.util.IdentityHashMap<>();

    static synchronized float scale(CharacterDefinition.Atlas a) {
        if (values.isEmpty()) return 1f;
        String k = chaves.get(a);
        if (k == null) {
            k = key(a);
            chaves.put(a, k);
        }
        Float v = values.get(k);
        return v == null ? 1f : v;
    }

    static int percent(CharacterDefinition.Atlas a) { return Math.round(scale(a) * 100f); }

    static synchronized void set(CharacterDefinition.Atlas a, float requested) {
        float v = Math.round(Math.max(MIN, Math.min(MAX, requested)) / STEP) * STEP;
        String k = key(a);
        if (Math.abs(v - 1f) < STEP / 2f) values.remove(k); else values.put(k, v);
        if (prefs != null) {
            SharedPreferences.Editor edit = prefs.edit();
            if (values.containsKey(k)) edit.putFloat(k, v); else edit.remove(k);
            edit.apply();
        }
    }

    static void adjust(CharacterDefinition.Atlas a, int steps) { set(a, scale(a) + steps * STEP); }

    static synchronized void reset(CharacterDefinition c) {
        for (Folha f : folhas(c)) set(f.atlas, 1f);
    }

    static synchronized int changed(CharacterDefinition c) {
        int n = 0;
        for (Folha f : folhas(c)) if (values.containsKey(key(f.atlas))) n++;
        return n;
    }

    /** Folhas do personagem na ordem das animações do pacote. */
    static List<Folha> folhas(CharacterDefinition c) {
        Map<String,Folha> byAtlas = new LinkedHashMap<>();
        for (Map.Entry<String,CharacterDefinition.Animation> e : c.animations.entrySet()) {
            CharacterDefinition.Atlas a = e.getValue().atlas;
            Folha f = byAtlas.get(a.resource);
            if (f == null) byAtlas.put(a.resource, f = new Folha(a));
            f.animacoes.add(e.getKey());
        }
        return new ArrayList<>(byAtlas.values());
    }

    /**
     * Todos os ajustes diferentes de 100%, de todos os personagens, no formato que
     * {@code tools/sprites/regua.py --aplicar} lê. fator 1.05 = deixar a folha 5% maior.
     */
    static synchronized String exportJson(String appVersion, String date) {
        StringBuilder out = new StringBuilder();
        out.append("{\n  \"tipo\": \"ajuste-tamanho\",\n");
        out.append("  \"app\": \"").append(escape(appVersion)).append("\",\n");
        out.append("  \"data\": \"").append(escape(date)).append("\",\n");
        out.append("  \"como_aplicar\": \"python3 tools/sprites/regua.py --aplicar este_arquivo.json\",\n");
        out.append("  \"ajustes\": [");
        boolean first = true;
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String id : GeneratedCharacters.SELECTABLE) {
            CharacterDefinition c = GeneratedCharacters.get(id);
            for (Folha f : folhas(c)) {
                Float v = values.get(key(f.atlas));
                if (v == null || !seen.add(f.atlas.resource)) continue;
                out.append(first ? "\n" : ",\n");
                first = false;
                out.append("    {\"personagem\": \"").append(c.id).append("\", \"folha\": \"")
                    .append(f.atlas.resource).append("\", \"animacoes\": [");
                for (int i = 0; i < f.animacoes.size(); i++)
                    out.append(i == 0 ? "" : ", ").append('"').append(f.animacoes.get(i)).append('"');
                out.append("], \"fator\": ").append(String.format(Locale.US, "%.2f", v)).append('}');
            }
        }
        out.append(first ? "]\n}\n" : "\n  ]\n}\n");
        return out.toString();
    }

    private static String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }

    /** Testes: começa sem nenhum ajuste e sem gravar no aparelho. */
    static synchronized void clearForTest() { values.clear(); prefs = null; }

    private AjusteTamanho() {}
}
