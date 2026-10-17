package com.gamelutagpt;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Caixas de combate ajustadas no olho (DEBUG > AJUSTAR CAIXAS): hurtbox e empurrão do corpo,
 * alcance e altura de cada golpe, saída da magia e do super. É uma prévia na tela de ajuste;
 * SALVAR NO DRIVE exporta e {@code tools/sprites/caixas.py --aplicar} grava no character.json.
 *
 * Cada ajuste guarda o valor de base em que foi feito: quando o pacote muda (o ajuste foi
 * gravado, ou outra mudança veio depois), a base não confere e o ajuste deixa de valer.
 */
final class AjusteCaixas {
    private static final String PREF = "ajuste_caixas_v1";
    static final float MIN = 1f, MAX = 600f;
    /** personagem|campo -> {base, valor} */
    private static final Map<String,float[]> values = new LinkedHashMap<>();
    private static SharedPreferences prefs;

    /** Um número do character.json: caminho no JSON, nome na tela e valor do pacote. */
    static final class Campo {
        final String path, label;
        final float base, step;
        Campo(String path, String label, float base, float step) {
            this.path = path; this.label = label; this.base = base; this.step = step;
        }
    }

    /** O que a tela mostra junto: um golpe, o corpo ou a saída de uma magia. */
    static final class Item {
        enum Tipo { CORPO, AGACHADO, GOLPE, MAGIA, SUPER }
        final String id, label;
        final Tipo tipo;
        /** Animação de prévia e o golpe que dá o tempo (null no corpo). */
        final CharacterDefinition.Animation animation;
        final AttackDefinition attack;
        final List<Campo> campos = new ArrayList<>();
        Item(String id, String label, Tipo tipo, CharacterDefinition.Animation animation, AttackDefinition attack) {
            this.id = id; this.label = label; this.tipo = tipo; this.animation = animation; this.attack = attack;
        }
        Item add(String path, String label, float base, float step) {
            campos.add(new Campo(path, label, base, step)); return this;
        }
    }

    static synchronized void load(Context context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        values.clear();
        for (Map.Entry<String,?> e : prefs.getAll().entrySet()) {
            if (!(e.getValue() instanceof String)) continue;
            String[] parts = ((String)e.getValue()).split(";");
            try {
                values.put(e.getKey(), new float[] {Float.parseFloat(parts[0]), Float.parseFloat(parts[1])});
            } catch (RuntimeException ignored) {
                // valor antigo ou corrompido: fica de fora
            }
        }
    }

    /** Itens do personagem: corpo, golpes na ordem do pacote, especiais, magia e super. */
    static List<Item> itens(CharacterDefinition c) {
        List<Item> out = new ArrayList<>();
        CharacterDefinition.Body b = c.fighter.body;
        out.add(new Item("CORPO", "CORPO EM PÉ", Item.Tipo.CORPO, c.animations.get(SpriteStates.IDLE), null)
            .add("fighter.body.halfWidth", "largura (metade)", b.halfWidth, 2f)
            .add("fighter.body.standHeight", "altura em pé", b.standHeight, 2f)
            .add("fighter.body.pushHalfWidth", "empurrão largura", b.pushHalfWidth, 2f)
            .add("fighter.body.pushHeight", "empurrão altura", b.pushHeight, 2f));
        out.add(new Item("AGACHADO", "CORPO AGACHADO", Item.Tipo.AGACHADO, c.animations.get(SpriteStates.CROUCH), null)
            .add("fighter.body.halfWidth", "largura (metade)", b.halfWidth, 2f)
            .add("fighter.body.crouchHeight", "altura agachado", b.crouchHeight, 2f));
        for (Map.Entry<String,CharacterDefinition.Move> e : c.moves.entrySet()) {
            // O golpe alto montado do M (sem entrada no pacote) não tem caixa própria para ajustar.
            if (GolpeAlto.derived(c, e.getKey())) continue;
            golpe(out, "moves." + e.getKey(), e.getKey(), e.getValue());
        }
        for (Map.Entry<String,CharacterDefinition.Special> e : c.specials.entrySet())
            golpe(out, "specialMoves." + e.getKey(), e.getKey(), e.getValue().move);
        CharacterDefinition.Projectile energy = c.fighter.energy;
        if (energy != null) out.add(new Item("S", "MAGIA (S)", Item.Tipo.MAGIA, c.specialAnimations.get("S"), energy.attack)
            .add("fighter.energy.spawnX", "saída à frente", energy.spawnX, 2f)
            .add("fighter.energy.spawnY", "saída altura", energy.spawnY, 2f)
            .add("fighter.energy.crouchSpawnY", "altura agachado", energy.crouchSpawnY, 2f));
        CharacterDefinition.Projectile sup = c.fighter.superAttack;
        if (sup != null) out.add(new Item("SUPER", "SUPER", Item.Tipo.SUPER, c.specialAnimations.get("SUPER"), sup.attack)
            .add("fighter.super.spawnX", "saída à frente", sup.spawnX, 2f)
            .add("fighter.super.spawnY", "saída altura", sup.spawnY, 2f)
            .add("fighter.super.airSpawnY", "altura no ar", sup.airSpawnY, 2f));
        return out;
    }

    private static void golpe(List<Item> out, String path, String binding, CharacterDefinition.Move move) {
        if (move == null || move.attack == null || move.attack.kind != AttackDefinition.Kind.NORMAL) return;
        String anim = move.animation == null ? "" : "  " + move.animation.id;
        out.add(new Item(binding, binding + anim, Item.Tipo.GOLPE, move.animation, move.attack)
            .add(path + ".reach", "alcance", move.attack.reach, 2f)
            .add(path + ".hitHeight", "altura do acerto", move.attack.hitHeight, 2f)
            .add(path + ".hitFloor", "base do acerto", Math.round(move.attack.hitFloor), 2f));
    }

    private static String key(CharacterDefinition c, Campo f) { return c.id + "|" + f.path; }

    /** Valor em vigor: o ajustado quando foi feito sobre a base atual, senão o do pacote. */
    static synchronized float value(CharacterDefinition c, Campo f) {
        float[] v = values.get(key(c, f));
        return v != null && v[0] == f.base ? v[1] : f.base;
    }

    /** Valor de um caminho em qualquer item do personagem (corpo aparece em dois itens). */
    static float value(CharacterDefinition c, List<Item> itens, String path) {
        for (Item item : itens) for (Campo f : item.campos) if (f.path.equals(path)) return value(c, f);
        return 0f;
    }

    static synchronized void adjust(CharacterDefinition c, Campo f, int steps) {
        set(c, f, value(c, f) + steps * f.step);
    }

    static synchronized void set(CharacterDefinition c, Campo f, float requested) {
        float v = Math.round(Math.max(MIN, Math.min(MAX, requested)));
        String k = key(c, f);
        if (v == f.base) values.remove(k); else values.put(k, new float[] {f.base, v});
        if (prefs != null) {
            SharedPreferences.Editor edit = prefs.edit();
            if (values.containsKey(k)) edit.putString(k, f.base + ";" + v); else edit.remove(k);
            edit.apply();
        }
    }

    static synchronized void reset(CharacterDefinition c) {
        for (Item item : itens(c)) for (Campo f : item.campos) set(c, f, f.base);
    }

    static synchronized int changed(CharacterDefinition c) {
        int n = 0;
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Item item : itens(c)) for (Campo f : item.campos)
            if (seen.add(f.path) && value(c, f) != f.base) n++;
        return n;
    }

    /** Todos os ajustes em vigor, de todos os personagens, para {@code tools/sprites/caixas.py --aplicar}. */
    static synchronized String exportJson(String appVersion, String date) {
        StringBuilder out = new StringBuilder();
        out.append("{\n  \"tipo\": \"ajuste-caixas\",\n");
        out.append("  \"app\": \"").append(appVersion.replace("\"", "'")).append("\",\n");
        out.append("  \"data\": \"").append(date.replace("\"", "'")).append("\",\n");
        out.append("  \"como_aplicar\": \"python3 tools/sprites/caixas.py --aplicar este_arquivo.json\",\n");
        out.append("  \"ajustes\": [");
        boolean first = true;
        for (String id : GeneratedCharacters.SELECTABLE) {
            CharacterDefinition c = GeneratedCharacters.get(id);
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (Item item : itens(c)) for (Campo f : item.campos) {
                float v = value(c, f);
                if (v == f.base || !seen.add(f.path)) continue;
                out.append(first ? "\n" : ",\n");
                first = false;
                out.append(String.format(Locale.US,
                    "    {\"personagem\": \"%s\", \"campo\": \"%s\", \"antes\": %d, \"valor\": %d}",
                    c.id, f.path, Math.round(f.base), Math.round(v)));
            }
        }
        out.append(first ? "]\n}\n" : "\n  ]\n}\n");
        return out.toString();
    }

    /** Testes: começa sem nenhum ajuste e sem gravar no aparelho. */
    static synchronized void clearForTest() { values.clear(); prefs = null; }

    private AjusteCaixas() {}
}
