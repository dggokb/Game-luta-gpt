package com.gamelutagpt;

/**
 * Tunables of the combat engine shared by every character. Per-move numbers live in the
 * character packs; these are the system-wide rules (buffer, scaling, decay, physics).
 * A new instance holds the defaults; tests may change fields before creating an engine.
 */
final class CombatConfig {
    /** Fixed simulation rate. Every frame count in packs and here is at this rate. */
    static final int FPS = 60;
    static final float DT = 1f / FPS;

    /** Frames a press stays usable after the frame it was pressed (hitstop does not count). */
    int bufferFrames = 4;
    /** A special command must be entered within this many frames... */
    int motionWindowFrames = 15;
    /** ...and its button pressed at most this many frames after the last direction. */
    int motionPressFrames = 15;
    int doubleTapFrames = 18;
    /** Down entered at most this many frames before up makes a super jump. */
    int superJumpFrames = 22;

    /** Maximum juggle points a defender in AIR_HITSTUN can take in one combo. */
    int juggleLimit = 6;

    /** Damage scaling, in permille of the base damage. */
    int damageStepPermille = 50;
    int damageFloorPermille = 200;
    int specialFloorPermille = 300;
    int superFloorPermille = 500;
    /** Extra reduction for every previous use of the same move in the combo. */
    int repeatPenaltyPermille = 200;

    /** Hitstun removed, indexed by hits already in the combo (the last value repeats). */
    int[] hitstunDecayByHits = {0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5};
    /** Hitstun removed, indexed by whole seconds the combo has lasted (the last value repeats). */
    int[] hitstunDecayBySeconds = {0, 0, 1, 2, 4, 6};
    int minHitstunFrames = 1;

    /** Pushback and knockback are spread over this many frames. */
    int pushbackFrames = 8;
    /** After a LAUNCH hit, a jump becomes a super jump for this many frames. */
    int launcherChaseFrames = 54;
    int knockdownFallFrames = 13;
    int knockdownDownFrames = 72;
    int wakeupFrames = 21;

    /** Default input priority when a character does not declare its own. */
    static final AttackDefinition.Strength[] DEFAULT_PRIORITY = {
        AttackDefinition.Strength.SUPER,
        AttackDefinition.Strength.SPECIAL,
        AttackDefinition.Strength.HEAVY,
        AttackDefinition.Strength.MEDIUM,
        AttackDefinition.Strength.LIGHT
    };

    // Physics (world units per second, world units per second squared).
    float walkSpeed = 300f;
    /** Walking (or drifting in the air) away is slower, so retreating cannot outrun a chase. */
    float walkBackSpeed = 220f;
    float dashSpeed = 620f;
    float backdashSpeed = 760f;
    int backdashFrames = 12;
    float jumpSpeed = 660f;
    float superJumpSpeed = 1450f;
    float gravity = 1650f;
    float launchSpeed = 1450f;
    float slamSpeed = 1850f;
    /** Airborne energy special: vertical speed kept at start and gravity while it runs. */
    float projectileAirVelocityScale = 0.32f;
    float projectileAirGravityScale = 0.12f;
    float facingEpsilon = 6f;

    // Super meter, in thousandths of a bar.
    static final int METER_PER_BAR = 1000;
    static final int MAX_METER = 5 * METER_PER_BAR;
    static final int SUPER_COST = METER_PER_BAR;
    /** Ultra (↓ + SUPER): three bars. */
    static final int ULTRA_COST = 3 * METER_PER_BAR;

    // Ultra: activation (the opponent freezes), rush, then either the cinematic (hit),
    // blockstun for the defender (guarded) or a punishable recovery (whiff or block).
    int ultraStartupFrames = 25;
    int ultraRushFrames = 18;
    int ultraRecoveryFrames = 30;
    float ultraRushSpeed = 2000f;
    /** Rush connects when the defender's body is this close in front of the attacker's root. */
    float ultraReach = 96f;
    int ultraBlockstunFrames = 24;
    int ultraHitstopFrames = 8;
    float ultraPushbackOnBlock = 120f;
    /** End of the cinematic: the defender flies away and lands knocked down. */
    float ultraLaunchSpeed = 1150f;
    float ultraKnockback = 420f;
    // Final beam (after the cinematic): a charge ("ka-me-ha-me"), then the beam reaches the
    // defender, hits every few frames (the combo counter climbs) and the blast throws the
    // defender like the cinematic end.
    int ultraBeamChargeFrames = 36;
    int ultraBeamExtendFrames = 8;
    int ultraBeamHitInterval = 4;
    int ultraBeamDefaultHits = 20;
    /** Frames between the last small hit and the blast. */
    int ultraBeamBlastGap = 6;
    /** Frames the beam fades after the blast before the attacker is free. */
    int ultraBeamFadeFrames = 18;
    int ultraBeamBlastHitstopFrames = 12;
    /** Share of the beam's damage kept for the blast (permille); the rest is split by the hits. */
    int ultraBeamBlastPermille = 300;
    /** Each hit pushes the defender back a little (a cornered defender pushes the attacker). */
    float ultraBeamPushPerHit = 7f;
    /** Recoil: each hit slides the attacker back (feet dragging on the floor). */
    float ultraBeamRecoilPerHit = 1.5f;
    /**
     * The cinematic ends at contact range; the fight comes back with the fighters this far
     * apart (root to root), so the beam is seen crossing the screen.
     */
    float ultraBeamDistance = 520f;

    /** Frame of the beam phase when the beam leaves the hands. */
    int ultraBeamFireFrame() {
        return ultraBeamChargeFrames;
    }

    /** Frame of the beam phase when the blast lands. */
    int ultraBeamBlastFrame(int hits) {
        return ultraBeamChargeFrames + ultraBeamExtendFrames + ultraBeamHitInterval * Math.max(0, hits - 1) +
            ultraBeamBlastGap;
    }

    // Team (TeamSystem). Raw tag (↓ + TAG): the point runs off, the partner runs in and poses.
    int tagExitFrames = 20;
    int tagEnterFrames = 23;
    int tagPoseFrames = 27;
    float tagTravel = 760f;
    int tagCooldownFrames = 600;
    // Assist (TAG): the partner runs in behind the point, does its move and runs off.
    int assistEnterFrames = 8;
    int assistLeaveFrames = 10;
    float assistBehind = 70f;
    float assistEnterDistance = 320f;
    int assistCooldownFrames = 240;
    /** After Assist → Tag: neither another assist nor a raw tag for this long. */
    int assistTagCooldownFrames = 300;

    static int hitstunDecay(int[] table, int index) {
        if (table.length == 0) return 0;
        return table[Math.min(Math.max(0, index), table.length - 1)];
    }
}
