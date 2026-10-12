package com.gamelutagpt;

/** Presentation only. Absolute monotonic seconds, no combat writes or frame-count timers. */
final class HudEffects {
    static final float HIT_FLASH = .16f, HIT_HOLD = .18f, HIT_FOLLOW = .55f;
    static final float HEAL_FLASH = .38f, READY_FLASH = .48f, SPEND_FLASH = .30f, LEVEL_FLASH = .42f;
    static final class Meter {
        FighterState fighter;
        float life, trail, healFrom, healTo;
        double hitAt = -100, healAt = -100, levelAt = -100, spendAt = -100;
        float trailStart;
        int superMeter, gainFrom, gainTo, spentFrom, spentTo;
        void seed(FighterState f) {
            fighter = f;
            life = trail = f.life / (float) f.profile.maxLife;
            superMeter = f.superMeter;
            hitAt = healAt = levelAt = spendAt = -100;
        }
        void update(FighterState f, double now) {
            if (fighter != f) { seed(f); return; }
            // Advance the old tail before retargeting; consecutive hits keep continuity.
            double age = now - hitAt;
            if (age > HIT_HOLD) trail = life + (trailStart - life) *
                (float)Math.max(0, 1 - (age - HIT_HOLD) / HIT_FOLLOW);
            float next = f.life / (float) f.profile.maxLife;
            if (next < life) {
                trailStart = Math.max(trail, life); trail = trailStart; hitAt = now;
            } else if (next > life) {
                healFrom = life; healTo = next; healAt = now;
                trail = next; trailStart = next; hitAt = -100;
            }
            life = next;
            if (f.superMeter < superMeter) {
                spentFrom = f.superMeter; spentTo = superMeter; spendAt = now;
            } else if (f.superMeter / CombatConfig.METER_PER_BAR > superMeter / CombatConfig.METER_PER_BAR) {
                gainFrom = superMeter / CombatConfig.METER_PER_BAR;
                gainTo = f.superMeter / CombatConfig.METER_PER_BAR; levelAt = now;
            }
            superMeter = f.superMeter;
        }
    }
    final Meter point = new Meter(), reserve = new Meter(), cpu = new Meter();
    double now, readyAt = -100, useAt = -100;
    float assistRatio;
    boolean initialized, assistUsing, assistEnabled, recharging;
    void reset() { initialized = false; }
    void update(HudRenderer.State s, double seconds) {
        now = seconds;
        boolean enabled = s.reserve() != null;
        float ratio = enabled ? Arena.clamp(s.tagReadyRatio(), 0, 1) : 0;
        boolean using = enabled && s.assistInUse();
        // A roster change establishes a new baseline, including meters, without events.
        if (!initialized || point.fighter != s.active() || reserve.fighter != s.reserve() || cpu.fighter != s.opponent()) {
            point.seed(s.active()); cpu.seed(s.opponent());
            if (s.reserve() != null) reserve.seed(s.reserve()); else reserve.fighter = null;
            readyAt = useAt = -100; initialized = true;
        } else {
            point.update(s.active(), now); cpu.update(s.opponent(), now);
            if (s.reserve() != null) reserve.update(s.reserve(), now);
            if (enabled && assistEnabled && ratio >= 1 && assistRatio < 1 && !using) readyAt = now;
            if (using && !assistUsing) useAt = now;
        }
        recharging = enabled && !using && ratio > 0 && ratio < 1;
        assistEnabled = enabled; assistUsing = using; assistRatio = ratio;
    }
    float fade(double at, float duration) { return (float)Math.max(0, Math.min(1, 1 - (now - at) / duration)); }
}
