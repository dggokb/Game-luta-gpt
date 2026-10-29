package com.gamelutagpt;

import java.util.HashMap;
import java.util.Map;

/**
 * A régua da CPU: até onde cada golpe de chão acerta de verdade. O golpe acerta pelo desenho
 * (CombatEngine.golpeNoDesenho), e o alcance do pacote ({@code reach}) ficava 34–48 px aquém
 * dele na maioria dos golpes (docs/balanco/auditoria.md, T02): a CPU andava para dentro do
 * golpe do rival e não cutucava da ponta. E alguns golpes não pegam quem está agachado (o H do
 * Kōga). A medida vem da {@link CpuReachTable} (gerada no motor pelo GeraAlcanceCpu); golpe ou
 * lutador fora dela (no ar, Super, personagens da história) usa o alcance do pacote, como antes.
 * Pure Java.
 */
final class CpuReach {
    private CpuReach() {}

    /** "lutador golpe" -> {ponta contra quem está em pé, contra agachado} (-1 = não acerta). */
    private static final Map<String, float[]> TABLE = parse(CpuReachTable.DATA);

    static Map<String, float[]> parse(String data) {
        Map<String, float[]> out = new HashMap<>();
        for (String entry : data.split(";")) {
            String[] p = entry.trim().split(" ");
            if (p.length != 4) continue;
            out.put(p[0] + " " + p[1], new float[] {Float.parseFloat(p[2]), Float.parseFloat(p[3])});
        }
        return out;
    }

    /** A medida do golpe, ou null (fora da tabela). */
    static float[] measured(CharacterDefinition c, String type) {
        return TABLE.get(c.id + " " + type);
    }

    /**
     * Distância máxima entre os eixos em que o golpe (de chão) pega o rival de corpo {@code target},
     * em pé ou agachado; negativa quando não pega (agachado). No ar ou fora da tabela, a do pacote.
     */
    static float maxCenterDistance(CharacterDefinition c, String type, boolean airborne,
                                   CharacterDefinition.Body target, boolean crouching) {
        float[] m = airborne ? null : measured(c, type);
        if (m == null) {
            CharacterDefinition.Move move = c.move(type, airborne);
            return move == null ? -1f : CombatRules.maxCenterDistance(move, target);
        }
        float tip = crouching ? m[1] : m[0];
        return tip < 0f ? -1f : tip + target.halfWidth;
    }

    /** O golpe pega daqui (CombatRules.reaches com a medida real e a postura do rival). */
    static boolean reaches(CharacterDefinition c, String type, boolean airborne, float distance,
                           CharacterDefinition.Body target, boolean crouching) {
        if (c.move(type, airborne) == null) return false;
        float max = maxCenterDistance(c, type, airborne, target, crouching);
        return distance >= CombatRules.MIN_MELEE_DISTANCE && distance <= max;
    }

    /** CombatRules.pickInRange com a régua real. */
    static String pickInRange(String[] types, double[] weights, CharacterDefinition attacker, boolean airborne,
                              float distance, CharacterDefinition.Body target, boolean crouching, double roll) {
        double total = 0;
        for (int i = 0; i < types.length; i++) {
            if (reaches(attacker, types[i], airborne, distance, target, crouching)) total += weights[i];
        }
        if (total <= 0) return null;
        double point = roll * total;
        String last = null;
        for (int i = 0; i < types.length; i++) {
            if (!reaches(attacker, types[i], airborne, distance, target, crouching)) continue;
            last = types[i];
            point -= weights[i];
            if (point < 0) return types[i];
        }
        return last;
    }
}
