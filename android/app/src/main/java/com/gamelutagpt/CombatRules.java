package com.gamelutagpt;

/**
 * Pure combat geometry and meter rules shared by the player and the AI. No Android
 * types, so every rule is unit-testable on the JVM.
 */
final class CombatRules {
    private CombatRules() {}

    /** Attacks cannot connect when the bodies overlap this much (legacy rule). */
    static final float MIN_MELEE_DISTANCE = 18f;
    /** Vertical tolerance beyond the target's half height. */
    static final float VERTICAL_SLACK = 19.5f;

    static final float SUPER_GAIN_LIGHT = 0.10f;
    static final float SUPER_GAIN_MEDIUM = 0.15f;
    static final float SUPER_GAIN_HEAVY = 0.20f;
    static final float SUPER_GAIN_ENERGY = 0.45f;
    static final float SUPER_GUARD_GAIN_LIGHT = 0.05f;
    static final float SUPER_GUARD_GAIN_MEDIUM = 0.075f;
    static final float SUPER_GUARD_GAIN_HEAVY = 0.10f;
    static final float SUPER_GUARD_GAIN_ENERGY = 0.20f;
    static final float SUPER_GUARD_GAIN_SUPER = 0.25f;

    /**
     * {@code move.reach} is the distance from the attacker's root to the tip of the strike;
     * the target's hurtbox width is added here, so a wider fighter is easier to touch.
     */
    static boolean meleeConnects(
        float attackerX,
        float attackerBaseY,
        int facing,
        CharacterDefinition.Move move,
        float targetX,
        float targetBaseY,
        CharacterDefinition.Body target,
        boolean targetCrouching
    ) {
        float horizontal = (targetX - attackerX) * facing;
        if (horizontal < MIN_MELEE_DISTANCE) return false;
        if (horizontal - target.halfWidth > move.reach) return false;
        float halfHeight = target.height(targetCrouching) * 0.5f;
        float targetCenterY = targetBaseY - halfHeight;
        float strikeY = attackerBaseY - move.hitHeight;
        return Math.abs(strikeY - targetCenterY) <= halfHeight + VERTICAL_SLACK;
    }

    /** Whether a defender standing in range should already raise the guard. */
    static boolean meleeThreatens(
        float attackerX,
        int facing,
        CharacterDefinition.Move move,
        float elapsed,
        float targetX,
        CharacterDefinition.Body target
    ) {
        if (!move.threatening(elapsed)) return false;
        float horizontal = (targetX - attackerX) * facing;
        return horizontal >= 0f && horizontal - target.halfWidth <= move.reach + 34f;
    }

    /** Swept test of a projectile segment against a hurtbox. */
    static boolean projectileHits(
        float previousX,
        float nextX,
        float y,
        float radius,
        float targetX,
        float targetBaseY,
        CharacterDefinition.Body target,
        boolean targetCrouching
    ) {
        float left = targetX - target.halfWidth;
        float right = targetX + target.halfWidth;
        float projectileLeft = Math.min(previousX, nextX) - radius;
        float projectileRight = Math.max(previousX, nextX) + radius;
        boolean horizontal = projectileRight >= left && projectileLeft <= right;
        float top = targetBaseY - target.height(targetCrouching);
        boolean vertical = y + radius >= top && y - radius <= targetBaseY;
        return horizontal && vertical;
    }

    static float superGainForAttack(String type) {
        if ("L".equals(type) || "2L".equals(type)) return SUPER_GAIN_LIGHT;
        if ("M".equals(type) || "2M".equals(type)) return SUPER_GAIN_MEDIUM;
        if ("H".equals(type) || "2H".equals(type)) return SUPER_GAIN_HEAVY;
        if ("S".equals(type)) return SUPER_GAIN_ENERGY;
        return 0f;
    }

    static float superGainForGuard(String type) {
        if ("L".equals(type) || "2L".equals(type)) return SUPER_GUARD_GAIN_LIGHT;
        if ("M".equals(type) || "2M".equals(type)) return SUPER_GUARD_GAIN_MEDIUM;
        if ("H".equals(type) || "2H".equals(type)) return SUPER_GUARD_GAIN_HEAVY;
        if ("S".equals(type)) return SUPER_GUARD_GAIN_ENERGY;
        if ("SUPER".equals(type)) return SUPER_GUARD_GAIN_SUPER;
        return 0f;
    }

    static boolean isLowAttack(String type) {
        return "2L".equals(type) || "2M".equals(type);
    }
}
