package com.gamelutagpt;

import java.util.ArrayList;
import java.util.List;

/**
 * Golpe alto de chão (→ + M), igual para todos os lutadores: pega quem defende agachado, só se
 * defende em pé. Lento de propósito (dá para reagir vendo o brilho do aviso) e, no acerto,
 * cancela nos especiais e no Super como o H. Sai só do neutro: dentro de um combo o → + M
 * continua sendo o M.
 *
 * O golpe é montado a partir do M de cada personagem (a mesma animação, esticada para a
 * duração nova) até cada um ganhar a animação própria; o pack pode declarar o seu "6M" e
 * então esse vale.
 */
final class GolpeAlto {
    private GolpeAlto() {}

    static final String BINDING = "6M";

    // Tempos em quadros de 60 Hz.
    static final int STARTUP = 20, ACTIVE = 3, RECOVERY = 14;
    /** No acerto: +3 (dá para ligar um L); na defesa: −6 (punível pelos golpes rápidos). */
    static final int HITSTUN = 20, BLOCKSTUN = 11;
    /** Dano em relação ao M (por mil). */
    static final int DAMAGE_PERMILLE = 1150;
    /**
     * Caixa do acerto: da base até a altura (mais a folga), baixa o bastante para pegar o
     * agachado mais baixo do elenco e alta o bastante para pegar quem está em pé.
     */
    static final float HIT_FLOOR = 40f, HIT_HEIGHT = 110f;

    /** {@code binding} é o 6M montado aqui (não veio do pacote do personagem). */
    static boolean derived(CharacterDefinition c, String binding) {
        if (!BINDING.equals(binding)) return false;
        CharacterDefinition.Move overhead = c.moves.get(BINDING), medium = c.moves.get("M");
        return overhead != null && medium != null && overhead.animation == medium.animation &&
            overhead.attack.startupFrames == STARTUP && overhead.attack.overhead;
    }

    /** O 6M montado do M (e do H, para os cancelamentos), ou null se faltar o M. */
    static CharacterDefinition.Move from(CharacterDefinition.Move medium, CharacterDefinition.Move heavy) {
        if (medium == null) return null;
        AttackDefinition m = medium.attack;
        List<String> targets = new ArrayList<>();
        if (heavy != null) {
            for (String t : heavy.attack.cancelInto()) {
                if (t.equals("SUPER") || t.equals("S") || t.matches("S[2-9]")) targets.add(t);
            }
        }
        AttackDefinition.Builder b = new AttackDefinition.Builder(BINDING, AttackDefinition.Kind.NORMAL)
            .damage(Math.max(1, m.damage * DAMAGE_PERMILLE / 1000))
            .frames(STARTUP, ACTIVE, RECOVERY)
            .stun(HITSTUN, BLOCKSTUN, m.hitstopFrames)
            .windows(targets.isEmpty() ? null : AttackDefinition.window(STARTUP, STARTUP + ACTIVE + 8), null, null)
            .cancelInto(targets.toArray(new String[0]))
            .pushback(m.pushbackOnHit, m.pushbackOnBlock)
            .maxUsesPerCombo(1)
            .overhead(true)
            .reach(m.reach, HIT_HEIGHT, HIT_FLOOR);
        return new CharacterDefinition.Move(BINDING, medium.animation, medium.pose, b.build());
    }
}
