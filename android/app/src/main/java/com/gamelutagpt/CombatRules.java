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

    /** Super meter gained by the attacker on a confirmed hit, thousandths of a bar. */
    static int superGainOnHit(AttackDefinition.Strength strength) {
        switch (strength) {
            case LIGHT: return 100;
            case MEDIUM: return 150;
            case HEAVY: return 200;
            case SPECIAL: return 450;
            default: return 0;
        }
    }

    /** Super meter gained by a defender that guards the attack. */
    static int superGainOnGuard(AttackDefinition.Strength strength) {
        switch (strength) {
            case LIGHT: return 50;
            case MEDIUM: return 75;
            case HEAVY: return 100;
            case SPECIAL: return 200;
            default: return 250;
        }
    }

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
        AttackDefinition attack = move.attack;
        for (int i = 0; i < attack.hitboxCount(); i++) {
            if (hitboxTouches(attackerX, attackerBaseY, facing, attack.hitbox(i),
                targetX, targetBaseY, target, targetCrouching)) return true;
        }
        return false;
    }

    /**
     * Hitbox against the target's body hurtbox. Bodies that overlap more than
     * {@link #MIN_MELEE_DISTANCE} never connect (legacy rule). With the default hitbox
     * (root to reach, hitHeight ± slack) this is the reach rule of earlier versions.
     */
    static boolean hitboxTouches(
        float attackerX, float attackerBaseY, int facing, AttackDefinition.Box box,
        float targetX, float targetBaseY, CharacterDefinition.Body target, boolean targetCrouching
    ) {
        float horizontal = (targetX - attackerX) * facing;
        if (horizontal < MIN_MELEE_DISTANCE) return false;
        return overlaps(attackerX, attackerBaseY, facing, box,
            targetX - target.halfWidth, targetBaseY - target.height(targetCrouching),
            targetX + target.halfWidth, targetBaseY);
    }

    /** Box of a fighter (root at x, baseY, looking towards facing) against a world rectangle. */
    static boolean overlaps(
        float x, float baseY, int facing, AttackDefinition.Box box,
        float left, float top, float right, float bottom
    ) {
        float boxLeft = facing > 0 ? x + box.x0 : x - box.x1;
        float boxRight = facing > 0 ? x + box.x1 : x - box.x0;
        float boxTop = baseY - box.y1;
        float boxBottom = baseY - box.y0;
        return boxRight >= left && boxLeft <= right && boxBottom >= top && boxTop <= bottom;
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

    /** Largest center-to-center distance at which {@code move} can touch {@code target}. */
    static float maxCenterDistance(CharacterDefinition.Move move, CharacterDefinition.Body target) {
        return move.reach + target.halfWidth;
    }

    /**
     * Weighted choice among the inputs whose move reaches the target from {@code distance}.
     * {@code roll} is a uniform value in [0, 1). Returns null when nothing reaches.
     */
    static String pickInRange(
        String[] types,
        double[] weights,
        CharacterDefinition attacker,
        boolean airborne,
        float distance,
        CharacterDefinition.Body target,
        double roll
    ) {
        double total = 0;
        for (int i = 0; i < types.length; i++) {
            if (reaches(attacker, types[i], airborne, distance, target)) total += weights[i];
        }
        if (total <= 0) return null;
        double point = roll * total;
        String last = null;
        for (int i = 0; i < types.length; i++) {
            if (!reaches(attacker, types[i], airborne, distance, target)) continue;
            last = types[i];
            point -= weights[i];
            if (point < 0) return types[i];
        }
        return last;
    }

    static boolean reaches(
        CharacterDefinition attacker,
        String type,
        boolean airborne,
        float distance,
        CharacterDefinition.Body target
    ) {
        return distance >= MIN_MELEE_DISTANCE &&
            distance <= maxCenterDistance(attacker.move(type, airborne), target);
    }

    /**
     * Separates two overlapping pushboxes. Each body yields half of the overlap; when one
     * is pinned against an arena bound the other takes the rest. Bodies whose pushboxes do
     * not overlap vertically (one jumped over the other) are left alone. Writes the new
     * positions into {@code out} and returns whether anything moved.
     *
     * @param tieDirection side where {@code b} goes when both share the same x
     */
    static boolean resolvePush(
        float ax, float aBaseY, CharacterDefinition.Body a,
        float bx, float bBaseY, CharacterDefinition.Body b,
        float minX, float maxX, int tieDirection, float[] out
    ) {
        out[0] = ax;
        out[1] = bx;
        boolean vertical = aBaseY - a.pushHeight < bBaseY && bBaseY - b.pushHeight < aBaseY;
        if (!vertical) return false;
        float minDistance = a.pushHalfWidth + b.pushHalfWidth;
        float dx = bx - ax;
        float overlap = minDistance - Math.abs(dx);
        if (overlap <= 0f) return false;
        int side = dx > 0f ? 1 : dx < 0f ? -1 : (tieDirection >= 0 ? 1 : -1);
        float nb = Arena.clamp(bx + side * overlap * 0.5f, minX, maxX);
        float na = Arena.clamp(nb - side * minDistance, minX, maxX);
        nb = Arena.clamp(na + side * minDistance, minX, maxX);
        out[0] = na;
        out[1] = nb;
        return true;
    }

}
