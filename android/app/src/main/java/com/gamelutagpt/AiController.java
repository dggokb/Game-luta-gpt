package com.gamelutagpt;

/**
 * Turns {@link OpponentAi} decisions into engine input for one fighter, frame by frame.
 * The CPU presses buttons like a player: its attacks go through the same buffer and
 * state machine, so it cannot attack while busy or stunned. Pure Java.
 */
final class AiController {
    /** Pause after the CPU's own attack ends and after it is hit, in frames. */
    static final int ATTACK_COOLDOWN = 13;
    static final int HIT_COOLDOWN = 21;
    static final int ENERGY_COOLDOWN = 24;
    static final int SUPER_COOLDOWN = 27;

    private final OpponentAi ai;
    private final OpponentAi.Situation situation = new OpponentAi.Situation();
    private boolean enabled;
    private boolean movingForward;
    private boolean dashing;
    private int cooldown;
    private AttackDefinition lastAttack;

    // Requests of the current decision, delivered once.
    private String attack;
    private String special;
    private boolean superAttack;
    private boolean backdash;
    private boolean jump;
    private boolean superJump;
    private boolean grab;
    /** Frames left before teching the throw it is caught in; -1 no tech, -2 not decided. */
    private int techDelay = -2;
    /** Recovery planned for the current air hitstun / knockdown (relative direction), or -2 undecided. */
    private int airTechPlan = -2;
    private int wakeupPlan = -2;

    private final OpponentAi.Actions actions = new OpponentAi.Actions() {
        @Override public void stop() {
            movingForward = false;
            dashing = false;
        }
        @Override public void attack(String type) { attack = type; }
        @Override public void superAttack() { superAttack = true; }
        @Override public void energy(String strength) { special = strength; }
        @Override public void backdash() { backdash = true; }
        @Override public void moveForward(boolean dash) {
            movingForward = true;
            dashing = dash;
        }
        @Override public void grab() { grab = true; }
        @Override public void jump(boolean superJumpRequest) {
            jump = true;
            superJump = superJumpRequest;
        }
    };

    AiController(OpponentAi ai) {
        this.ai = ai;
    }

    boolean enabled() {
        return enabled;
    }

    void setEnabled(boolean value) {
        enabled = value;
        ai.reset();
        movingForward = dashing = false;
        cooldown = 0;
        lastAttack = null;
        clearRequests();
    }

    /** Fills the CPU input for the next engine step. */
    void fill(CombatEngine engine, int self, FighterInput out) {
        out.clear();
        if (!enabled) return;
        CombatFighter me = engine.fighter(self);
        CombatFighter target = engine.fighter(1 - self);
        if (me.status == CombatFighter.Status.THROWN) {
            if (techDelay == -2) techDelay = ai.techDelay();
            if (techDelay >= 0 && techDelay-- == 0) out.grab = true;
            return;
        }
        techDelay = -2;
        if (me.frozen()) return;
        if (me.status == CombatFighter.Status.AIR_HITSTUN) {
            // Air tech once the hitstun is over, sometimes, toward a random side.
            if (airTechPlan == -2) airTechPlan = ai.airTechDirection();
            if (airTechPlan >= 0 && me.stunLeft == 0) {
                out.direction = MotionParser.relative(airTechPlan, me.facing);
                out.light = true;
            }
            return;
        }
        airTechPlan = -2;
        if (me.status == CombatFighter.Status.KNOCKDOWN) {
            if (wakeupPlan == -2) wakeupPlan = ai.wakeupDirection();
            if (wakeupPlan > 0) out.direction = MotionParser.relative(wakeupPlan, me.facing);
            return;
        }
        wakeupPlan = -2;

        if (cooldown > 0) cooldown--;
        if (me.framesSinceHit == 0) cooldown = Math.max(cooldown, HIT_COOLDOWN);
        if (me.attacking()) {
            lastAttack = me.attack;
            // Confirmed launcher: chase it with a super jump (jump cancel).
            if (me.attack.launch == AttackDefinition.Launch.LAUNCH && me.outcome == CombatFighter.Outcome.HIT) {
                out.superJump = true;
            }
            return;
        }
        if (lastAttack != null) {
            cooldown = Math.max(cooldown,
                lastAttack.kind == AttackDefinition.Kind.SUPER ? SUPER_COOLDOWN
                    : lastAttack.kind == AttackDefinition.Kind.PROJECTILE ? ENERGY_COOLDOWN
                    : ATTACK_COOLDOWN);
            lastAttack = null;
        }
        if (!me.canAct() || target.ko()) {
            movingForward = dashing = false;
            return;
        }

        situation.self = me.character();
        situation.target = target.body();
        situation.distance = Math.abs(target.x - me.x);
        situation.verticalDistance = Math.abs(target.y - me.y);
        situation.selfAirborne = !me.grounded;
        situation.targetGrounded = target.grounded;
        situation.targetAlive = !target.ko();
        situation.superReady = me.state.superMeter >= CombatConfig.SUPER_COST;
        situation.projectileActive = hasProjectile(engine, self);
        situation.attackReady = cooldown <= 0;
        situation.throwReach = me.body().halfWidth + target.body().halfWidth + engine.config.throwRange - 4f;
        clearRequests();
        ai.think(CombatConfig.DT, situation, actions);

        int forward = me.facing > 0 ? 1 : 5;
        if (movingForward) out.direction = forward;
        out.dash = dashing && movingForward;
        out.backdash = backdash;
        out.jump = jump && !superJump;
        out.superJump = jump && superJump;
        out.superAttack = superAttack;
        out.grab = grab;
        if (special != null) out.special = special;
        if (attack != null) {
            if (attack.startsWith("2")) out.direction = 3;
            char button = attack.charAt(attack.length() - 1);
            out.light = button == 'L';
            out.medium = button == 'M';
            out.heavy = button == 'H';
        }
    }

    private void clearRequests() {
        attack = null;
        special = null;
        superAttack = backdash = jump = superJump = grab = false;
    }

    private static boolean hasProjectile(CombatEngine engine, int owner) {
        for (Projectile p : engine.energyProjectiles) if (p.ownerIndex == owner) return true;
        return false;
    }
}
