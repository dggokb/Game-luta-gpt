package com.gamelutagpt;

/**
 * Simulation state of one fighter: body, current action, reaction, freeze and input.
 * {@link CombatEngine} is the only writer; the shell (GameView) reads it to render.
 */
final class CombatFighter {
    /** Character state machine. Attack phases and hitstop are derived, see {@link #stateName}. */
    enum Status { NEUTRAL, ATTACK, HITSTUN, BLOCKSTUN, AIR_HITSTUN, KNOCKDOWN, WAKEUP }

    /** What the current attack has done so far; picks the hit/block/whiff cancel window. */
    enum Outcome { NONE, HIT, BLOCK }

    static final int GUARD_NONE = 0;
    static final int GUARD_HIGH = 1;
    static final int GUARD_LOW = 2;
    /** Holding back while airborne: blocks everything that can reach a jumping body. */
    static final int GUARD_AIR = 3;

    final int index;
    FighterState state;

    // Body.
    float x;
    float y = Arena.GROUND_Y;
    float vy;
    boolean grounded = true;
    int facing = 1;
    boolean superJumping;
    boolean crouching;
    boolean forwardDashing;
    int backdashFrames;
    /** Horizontal distance moved during the last frame (animation). */
    float travel;

    // Action.
    Status status = Status.NEUTRAL;
    AttackDefinition attack;
    /** Visual binding of a normal; null for S/SUPER. */
    CharacterDefinition.Move move;
    /** Index of the last frame of the attack that was played; -1 right after it starts. */
    int attackFrame = -1;
    Outcome outcome = Outcome.NONE;
    String specialStrength;
    /** Step of the autoCombo route this attack came from, or -1. */
    int autoStep = -1;
    int hitsOnTarget;
    long hitboxMask;

    // Reaction.
    int stunLeft;
    int stunTotal;
    int stunElapsed;
    boolean hitCrouching;
    boolean launched;
    boolean slammed;
    int knockdownFrame;
    int guard;
    int lastGuard;
    int anticipatedGuard;
    /** Frames during which nothing of this fighter advances (hitstop, super freeze). */
    int hitstop;
    float pushRemaining;
    int pushFramesLeft;
    CombatFighter pushSource;
    int launcherChase;
    int framesSinceHit = 999;
    int framesSinceBlock = 999;
    /** Set by the shell while a tag animation owns the fighter. */
    boolean locked;

    // Input.
    final InputBuffer buffer = new InputBuffer();
    final MotionParser motion = new MotionParser();
    final FighterInput input = new FighterInput();
    /** Local frame clock: it does not advance while frozen. */
    int clock;
    int pendingJumpAge = -1;
    boolean pendingSuperJump;
    boolean dashRequest;
    boolean backdashRequest;

    CombatFighter(int index, FighterState state, float x, int facing) {
        this.index = index;
        this.state = state;
        this.x = x;
        this.facing = facing;
    }

    CharacterDefinition character() {
        return state.character;
    }

    CharacterDefinition.Body body() {
        return state.profile.body;
    }

    boolean ko() {
        return state.life <= 0;
    }

    boolean frozen() {
        return hitstop > 0;
    }

    boolean attacking() {
        return status == Status.ATTACK;
    }

    boolean inHitstun() {
        return status == Status.HITSTUN || status == Status.AIR_HITSTUN;
    }

    /** Could start an action this frame (before cancels). */
    boolean canAct() {
        return status == Status.NEUTRAL && !locked && !ko() && !frozen();
    }

    /** Knocked down, getting up or KO: no hit connects. */
    boolean hittable() {
        return !ko() && status != Status.KNOCKDOWN && status != Status.WAKEUP;
    }

    /** Crouching hurtbox: crouch stance, crouching attack or crouching hit reaction. */
    boolean crouchingBody() {
        if (status == Status.ATTACK && move != null) return move.binding.startsWith("2");
        if (status == Status.HITSTUN) return hitCrouching;
        if (status == Status.BLOCKSTUN) return lastGuard == GUARD_LOW;
        return crouching || anticipatedGuard == GUARD_LOW;
    }

    float hurtTop() {
        return y - body().height(crouchingBody());
    }

    /** Name used by the debug overlay and the HUD: the states of the V2 specification. */
    String stateName() {
        if (hitstop > 0) return "HITSTOP";
        if (status == Status.ATTACK) return "ATTACK_" + attack.phase(Math.max(0, attackFrame));
        return status.name();
    }

    void clearAttack() {
        attack = null;
        move = null;
        attackFrame = -1;
        outcome = Outcome.NONE;
        specialStrength = null;
        autoStep = -1;
        hitsOnTarget = 0;
        hitboxMask = 0L;
    }
}
