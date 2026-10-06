package com.gamelutagpt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One real combo: it starts on the first confirmed hit and lasts while the defender stays
 * unable to act. Damage scaling, hitstun decay and juggle points are computed from it.
 */
final class ComboSession {
    final int attacker;
    final int defender;
    int hitCount;
    int comboDamage;
    /** Frames since the first hit (hitstop frames included). */
    int comboDuration;
    /** Scale used by the last hit, permille. */
    int damageScale = 1000;
    /** Hitstun removed from the last hit, frames. */
    int hitstunDecay;
    String lastMove = "";
    private final List<String> usedMoves = new ArrayList<>();
    boolean airCombo;
    boolean active = true;
    int juggleCount;
    /** Proration declared by the move that started the combo. */
    int prorationPermille = 1000;

    ComboSession(int attacker, int defender) {
        this.attacker = attacker;
        this.defender = defender;
    }

    List<String> usedMoves() {
        return Collections.unmodifiableList(usedMoves);
    }

    int usesOf(String moveId) {
        int uses = 0;
        for (String used : usedMoves) if (used.equals(moveId)) uses++;
        return uses;
    }

    /** Whether the move may still be cancelled into (maxUsesPerCombo, 0 = unlimited). */
    boolean allows(AttackDefinition attack) {
        return attack.maxUsesPerCombo <= 0 || usesOf(attack.id) < attack.maxUsesPerCombo;
    }

    /**
     * Records a hit and returns the damage it deals: progressive step down to a floor,
     * starter proration, repeated-move penalty, integer math rounded down, at least 1.
     */
    int registerHit(AttackDefinition attack, int baseDamage, CombatConfig config) {
        if (hitCount == 0) prorationPermille = attack.prorationPermille;
        damageScale = DamageScaling.scale(attack, hitCount, hitCount == 0 ? 1000 : prorationPermille,
            usesOf(attack.id), config);
        int damage = DamageScaling.apply(baseDamage, damageScale);
        hitCount++;
        comboDamage += damage;
        lastMove = attack.id;
        usedMoves.add(attack.id);
        return damage;
    }

    /** Hitstun for the next hit of this combo after decay. */
    int decayedHitstun(int baseHitstun, CombatConfig config) {
        hitstunDecay = HitstunDecay.reduction(hitCount, comboDuration, config);
        return Math.max(config.minHitstunFrames, baseHitstun - hitstunDecay);
    }

    /** Pure damage scaling rules, integer permille. */
    static final class DamageScaling {
        private DamageScaling() {}

        static int scale(AttackDefinition attack, int hitIndex, int prorationPermille, int previousUses,
                         CombatConfig config) {
            long scale = Math.max(0, 1000 - (long)config.damageStepPermille * hitIndex);
            scale = scale * prorationPermille / 1000;
            for (int i = 0; i < previousUses; i++) scale = scale * (1000 - config.repeatPenaltyPermille) / 1000;
            int floor = attack.kind == AttackDefinition.Kind.SUPER
                ? config.superFloorPermille
                : attack.kind == AttackDefinition.Kind.PROJECTILE
                    ? config.specialFloorPermille
                    : config.damageFloorPermille;
            return (int)Math.min(1000, Math.max(floor, scale));
        }

        static int apply(int baseDamage, int scalePermille) {
            return Math.max(1, (int)((long)baseDamage * scalePermille / 1000));
        }
    }

    /** Hitstun reduction bands by hits already landed and by combo length in seconds. */
    static final class HitstunDecay {
        private HitstunDecay() {}

        static int reduction(int hitsSoFar, int durationFrames, CombatConfig config) {
            int byHits = CombatConfig.hitstunDecay(config.hitstunDecayByHits, hitsSoFar);
            int bySeconds = CombatConfig.hitstunDecay(config.hitstunDecayBySeconds, durationFrames / CombatConfig.FPS);
            return Math.max(byHits, bySeconds);
        }
    }
}
