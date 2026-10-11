package com.gamelutagpt;

/**
 * Common non-looping INTRO -> approved IDLE transition policy.
 *
 * Frame-rate independent: use the authored clip clock, never physical atlas
 * indices or a specific character. At intro.duration the entire sprite is
 * exactly IDLE frame 0, including when the match advances to IDLE next tick.
 * P01's currently approved bespoke interpolation remains unchanged until
 * migrated and visually approved; this policy is used by every other fighter.
 */
final class SpriteIntroTransition {
    static final float MAX_BLEND_SECONDS = 0.42f;
    private SpriteIntroTransition() {}

    static float blend(CharacterDefinition.Animation intro,float time) {
        if (intro==null || intro.loop || !(intro.duration>0f) || !Float.isFinite(time)) {
            return 0f;
        }
        float window=Math.min(MAX_BLEND_SECONDS,intro.duration*0.16f);
        if (!(window>0f)) return 0f;
        float raw=(time-(intro.duration-window))/window;
        float p=Math.max(0f,Math.min(1f,raw));
        // Smoothstep gives zero velocity on either side of the boundary,
        // mitigating sudden jumps from nonidentical source silhouettes.
        return p*p*(3f-2f*p);
    }

    /** P01's legacy renderer remains explicitly preserved for regression. */
    static boolean usesLegacy(String character) {
        return "player_base".equals(character) || "p01_training".equals(character);
    }
}
