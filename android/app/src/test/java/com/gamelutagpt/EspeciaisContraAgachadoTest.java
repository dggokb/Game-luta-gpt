package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Especial e Super de chão pegam quem está agachado: o que acerta o rival em pé acerta também
 * o rival abaixado (antes muitos passavam por cima, e a energia por cima dos mais baixos).
 */
public class EspeciaisContraAgachadoTest {
    private static final float[] GAPS = {110f, 150f, 200f, 260f, 340f, 450f};

    /** {@code special}: o comando ("M" = a energia S1, "S2".."S4") ou null para o Super. */
    private static boolean hits(CharacterDefinition attacker, CharacterDefinition target, String special,
                                float gap, boolean crouch) {
        FighterState a = new FighterState(attacker, "A"), d = new FighterState(target, "D");
        if (special == null) a.superMeter = CombatConfig.MAX_METER;
        CombatEngine e = new CombatEngine(a, d, 600f, 600f + gap, new CombatConfig());
        FighterInput in = new FighterInput(), held = new FighterInput();
        held.direction = crouch ? 3 : 0;
        for (int i = 0; i < 12; i++) e.step(in, held);
        e.fighter(0).x = 600f;
        e.fighter(1).x = 600f + gap;
        if (special == null) in.superAttack = true; else in.special = special;
        for (int i = 0; i < 150; i++) {
            e.step(in, held);
            in.clearPresses();
            for (CombatEngine.HitEvent h : e.events()) if (h.defender == 1) return true;
        }
        return false;
    }

    private static void assertHitsCrouching(String attackerId, String special, String targetId) {
        CharacterDefinition a = GeneratedCharacters.get(attackerId), t = GeneratedCharacters.get(targetId);
        boolean standing = false, crouching = false;
        for (float gap : GAPS) {
            standing |= hits(a, t, special, gap, false);
            crouching |= hits(a, t, special, gap, true);
        }
        String name = attackerId + " " + (special == null ? "SUPER" : special) + " em " + targetId;
        assertTrue(name + ": acerta em pé", standing);
        assertTrue(name + ": acerta agachado", crouching);
    }

}
