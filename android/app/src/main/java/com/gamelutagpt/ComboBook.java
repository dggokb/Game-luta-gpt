package com.gamelutagpt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Caderno de combos da CPU: a rota principal de cada lutador, montada das rotas do próprio
 * pacote (cancelInto e maxUsesPerChain) e do estilo de combo. Ninguém escreve combo à mão:
 * se o pacote muda, a rota muda junto.
 *
 * Passos: um golpe ("L", "2H", "jM", "j2H", "S", "SUPER"), {@link #JUMP} (super pulo atrás do
 * lançador) ou {@link #AIR_JUMP} (cancelamento de pulo no ar).
 */
final class ComboBook {
    static final String JUMP = "JUMP", AIR_JUMP = "AIR_JUMP";
    /** Passos que comprometem (inseguros na defesa): só depois de ver que acertou. */
    static boolean commits(String step) {
        return "2H".equals(step) || "S".equals(step) || "SUPER".equals(step) || JUMP.equals(step);
    }

    private static final Map<String, List<String>> CACHE = new HashMap<>();

    static synchronized List<String> route(CharacterDefinition c) {
        List<String> route = CACHE.get(c.id);
        if (route == null) {
            route = Collections.unmodifiableList(build(c, CpuStyle.of(c)));
            CACHE.put(c.id, route);
        }
        return route;
    }

    static List<String> build(CharacterDefinition c, CpuStyle style) {
        List<String> route = new ArrayList<>();
        Map<String, Integer> uses = new HashMap<>();
        String current = "L";
        add(route, uses, current);
        // Os estilos que vivem do H (tanque, alcance, imprevisível) vão pela parede; os outros
        // pelo lançador.
        boolean wall = "tanque".equals(style.name) || "agarrador".equals(style.name) || "alcance".equals(style.name) ||
            style.erratic();
        boolean reversed = false;
        for (int guard = 0; guard < 8; guard++) {
            String next = null;
            AttackDefinition a = c.attack(current);
            if (a == null) break;
            if ("L".equals(current) || "2L".equals(current)) {
                next = pick(c, a, uses, "M", wall ? "H" : "2H", wall ? "2H" : "H");
            } else if ("M".equals(current)) {
                next = pick(c, a, uses, "M", wall ? "H" : "2H", wall ? "2H" : "H");
            } else if ("H".equals(current)) {
                // O imprevisível volta uma vez (H > L) antes de fechar; o tanque repete o H.
                if (style.erratic() && !reversed) {
                    AttackDefinition light = c.attack("L");
                    if (light != null && a.cancelsInto("L") && uses.getOrDefault("L", 0) < light.maxUsesPerChain) {
                        next = "L";
                        reversed = true;
                        wall = false;
                        uses.remove("M");   // a volta abre o M de novo (M tem 2 usos no imprevisível)
                    }
                }
                if (next == null) next = pick(c, a, uses, "H");
            }
            if (next == null) break;
            add(route, uses, next);
            current = next;
            if ("2H".equals(next)) break;
        }
        if ("2H".equals(current)) {
            AttackDefinition launcher = c.attack("2H");
            if (launcher.launch == AttackDefinition.Launch.LAUNCH && launcher.cancelsInto(AttackDefinition.JUMP)) {
                route.add(JUMP);
                air(c, route);
            }
        } else if ("H".equals(current)) {
            AttackDefinition h = c.attack("H");
            if (h.cancelsInto("SUPER") && c.fighter.hasSuperAttack()) route.add("SUPER");
            else if (h.cancelsInto("S") && c.fighter.energy != null) route.add("S");
        }
        return route;
    }

    /** Corrente aérea depois do super pulo: jL > jM (> pulo no ar > jL > jM) > j2H ou jH. */
    private static void air(CharacterDefinition c, List<String> route) {
        if (c.attack("jL") == null || c.attack("jM") == null) return;
        Map<String, Integer> uses = new HashMap<>();
        String current = "jL";
        add(route, uses, current);
        boolean jumped = false;
        for (int guard = 0; guard < 10; guard++) {
            AttackDefinition a = c.attack(current);
            String next = null;
            if ("jL".equals(current)) next = pick(c, a, uses, "jL", "jM");
            else if ("jM".equals(current)) {
                next = pick(c, a, uses, "jM");
                if (next == null && !jumped && a.cancelsInto(AttackDefinition.JUMP)) {
                    route.add(AIR_JUMP);
                    jumped = true;
                    uses.clear();          // a corrente aérea recomeça
                    current = "jL";
                    add(route, uses, current);
                    continue;
                }
                if (next == null) next = pick(c, a, uses, "j2H", "jH");
            }
            if (next == null) break;
            add(route, uses, next);
            current = next;
            if ("j2H".equals(next) || "jH".equals(next)) break;
        }
    }

    /** Primeiro alvo da lista que o golpe cancela e que ainda tem uso na corrente. */
    private static String pick(CharacterDefinition c, AttackDefinition from, Map<String, Integer> uses,
                               String... preferences) {
        for (String target : preferences) {
            AttackDefinition t = c.attack(target);
            if (t == null || !from.cancelsInto(target)) continue;
            int used = uses.getOrDefault(target, 0);
            if (t.maxUsesPerChain > 0 && used >= t.maxUsesPerChain) continue;
            // Repetir o mesmo golpe só vale quando o estilo pede (M > M, H > H, jL > jL, jM > jM).
            if (used > 0 && t.maxUsesPerChain < 2) continue;
            if (used > 0 && "L".equals(target)) continue;
            return target;
        }
        return null;
    }

    private static void add(List<String> route, Map<String, Integer> uses, String step) {
        route.add(step);
        uses.merge(step, 1, Integer::sum);
    }

    /** O botão de um passo de golpe: "2H" → H agachado; "jM" → M no ar; "j2H" → ↓ + H no ar. */
    static boolean crouching(String step) {
        return step.startsWith("2") || step.startsWith("j2");
    }

    static char button(String step) {
        return step.charAt(step.length() - 1);
    }
}
