package com.gamelutagpt;

/**
 * Gameplay-neutral continuity rules shared by every character and every pair of
 * animation states. Uses character manifests to identify arbitrary attack IDs.
 *
 * No rule can delay an attack, damage reaction, guard, cancel, projectile or hitbox:
 * the simulation changes state immediately; ONLY its visual pose may blend.
 */
final class SpriteTransitionPolicy {
    private SpriteTransitionPolicy() {}

    private static boolean isAttack(CharacterDefinition c,String id) {
        if (id==null) return false;
        for(CharacterDefinition.Move m:c.moves.values())
            if(m.animation!=null && id.equals(m.animation.id)) return true;
        for(CharacterDefinition.Special s:c.specials.values())
            if(s.move.animation!=null && id.equals(s.move.animation.id)) return true;
        for(CharacterDefinition.Animation s:c.specialAnimations.values())
            if(id.equals(s.id)) return true;
        // Generic fallback for character packs exposing new actions without code changes.
        return id.startsWith("SPECIAL_") || id.startsWith("ULTRA_")
            || id.startsWith("THROW_") || id.startsWith("JUMP_LIGHT")
            || id.startsWith("JUMP_MEDIUM") || id.startsWith("JUMP_HEAVY");
    }

    private static boolean isReaction(String id) {
        return id!=null&&(id.startsWith("HIT_") || id.equals(SpriteStates.KNOCKDOWN)
                || id.equals(SpriteStates.GROUNDED)
                || id.equals(SpriteStates.GETUP));
    }

    private static boolean isGuard(String id) { return id!=null&&id.startsWith("DEFENSE_"); }

    private static boolean isLocomotion(String id) {
        return SpriteStates.WALK_FORWARD.equals(id) || SpriteStates.WALK_BACK.equals(id)
            || SpriteStates.DASH.equals(id) || SpriteStates.BACKDASH.equals(id);
    }

    private static boolean isCinematic(String id) {
        return SpriteStates.VICTORY.equals(id) || SpriteStates.DEFEAT.equals(id)
            || SpriteStates.INTRO.equals(id) || SpriteStates.TAUNT.equals(id);
    }

    /** Seconds of visual interpolation. Zero means an immediate visual transition. */
    static float duration(CharacterDefinition character,String from,String to) {
        if(from==null || to==null || from.equals(to)
                || !character.animations.containsKey(from)
                || !character.animations.containsKey(to)) return 0f;
        // Keep input and hit responses crisp and deterministic.
        if(isReaction(to) || isGuard(to) || isAttack(character,to)) return 0f;
        // Hard animation boundaries: all cinematics except the intro exit must be immediate.
        if(isCinematic(to)) return 0f;
        if(SpriteStates.INTRO.equals(from) && SpriteStates.IDLE.equals(to)) return .14f;
        if(SpriteStates.JUMP.equals(from) && SpriteStates.FALL.equals(to)) return .065f;
        if(SpriteStates.FALL.equals(from) && SpriteStates.LAND.equals(to)) return .085f;
        if(SpriteStates.LAND.equals(from) && SpriteStates.IDLE.equals(to)) return .07f;
        if(SpriteStates.CROUCH.equals(from) && SpriteStates.RISE.equals(to)) return .075f;
        if(SpriteStates.RISE.equals(from) && SpriteStates.IDLE.equals(to)) return .065f;
        if(isAttack(character,from)) return .055f;
        if(isReaction(from) || isGuard(from)) return .06f;
        if(isLocomotion(from) || isLocomotion(to)) return .075f;
        if(isCinematic(from)) return .10f;
        return .055f;
    }
}
