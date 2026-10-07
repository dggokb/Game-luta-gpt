package com.gamelutagpt;

import java.util.Random;

/**
 * Decision-making of the CPU opponent. It only chooses; GameView owns physics, timers
 * and damage and executes the choice through {@link Actions}. Pure Java, so decisions
 * can be tested with a seeded {@link Random}.
 */
final class OpponentAi {
    interface Actions {
        /** Clears the previous movement intent before a new decision. */
        void stop();
        void attack(String type);
        void superAttack();
        void energy(String strength);
        void backdash();
        void moveForward(boolean dash);
        void jump(boolean superJump);
        /** L + M: throw. */
        void grab();
    }

    /** Snapshot of what the AI may look at; filled by the caller every step. */
    static final class Situation {
        CharacterDefinition self;
        CharacterDefinition.Body target;
        float distance;
        float verticalDistance;
        boolean selfAirborne;
        boolean targetGrounded;
        boolean targetAlive;
        boolean superReady;
        boolean projectileActive;
        boolean attackReady;
        /** Center distance within which a throw reaches the target. */
        float throwReach;
    }

    static final float DECISION_MIN = 0.12f;
    static final float DECISION_MAX = 0.28f;
    // Attack preferences; the character pack decides which of them can reach.
    static final String[] GROUND_ATTACKS = {"2L", "2M", "2H", "L", "M", "H"};
    static final double[] GROUND_WEIGHTS = {0.18, 0.18, 0.17, 0.17, 0.16, 0.14};
    static final String[] AIR_ATTACKS = {"L", "M", "H"};
    /** Chance of throwing when the target is right next to it, and of teching a throw. */
    static final double THROW_CHANCE = 0.16;
    static final double TECH_CHANCE = 0.40;
    /** Chance of an air tech when the air hitstun ends. */
    static final double AIR_TECH_CHANCE = 0.70;
    static final double[] AIR_WEIGHTS = {0.30, 0.34, 0.36};

    private final Random random;
    private float decisionTimer;

    OpponentAi(Random random) {
        this.random = random;
    }

    void reset() {
        decisionTimer = 0f;
    }

    /** Advances the decision clock; returns true when a new decision was taken. */
    boolean think(float dt, Situation s, Actions actions) {
        decisionTimer -= dt;
        if (decisionTimer > 0f) return false;
        decisionTimer = DECISION_MIN + random.nextFloat() * (DECISION_MAX - DECISION_MIN);
        actions.stop();

        String airAttack = s.verticalDistance <= 115f
            ? CombatRules.pickInRange(AIR_ATTACKS, AIR_WEIGHTS,
                s.self, true, s.distance, s.target, random.nextDouble())
            : null;
        String groundAttack = CombatRules.pickInRange(GROUND_ATTACKS, GROUND_WEIGHTS,
            s.self, false, s.distance, s.target, random.nextDouble());

        if (s.selfAirborne) {
            if (airAttack != null) actions.attack(airAttack);
            else if (s.distance > 95f) actions.moveForward(false);
        } else if (!s.targetAlive) {
            // nothing to do
        } else if (
            s.superReady &&
            s.distance >= 250f &&
            s.distance <= 720f &&
            random.nextDouble() < 0.18
        ) {
            actions.superAttack();
        } else if (
            s.distance >= 230f &&
            s.distance <= 620f &&
            !s.projectileActive &&
            random.nextDouble() < 0.24
        ) {
            double r = random.nextDouble();
            actions.energy(r < 0.33 ? "L" : (r < 0.72 ? "M" : "H"));
        } else if (
            !s.targetGrounded &&
            CombatRules.reaches(s.self, "2H", false, s.distance, s.target) &&
            random.nextDouble() < 0.38
        ) {
            actions.attack("2H");
        } else if (s.targetGrounded && s.attackReady && s.distance <= s.throwReach &&
            random.nextDouble() < THROW_CHANCE) {
            actions.grab();
        } else if (groundAttack != null && s.attackReady) {
            actions.attack(groundAttack);
        } else if (s.distance < 82f && random.nextDouble() < 0.38) {
            actions.backdash();
        } else if (s.distance > 360f && random.nextDouble() < 0.38) {
            actions.moveForward(true);
        } else if (groundAttack == null && s.distance > CombatRules.MIN_MELEE_DISTANCE) {
            // Approach until at least one ground move can reach the target.
            actions.moveForward(false);
        } else if (random.nextDouble() < 0.12) {
            actions.jump(random.nextDouble() < 0.32);
        }
        return true;
    }

    /**
     * Grabbed: frames to wait before teching, or -1 to let the throw land. A human reacts
     * to the grab, so the CPU techs only part of the time and not on the first frame.
     */
    int techDelay() {
        return random.nextDouble() < TECH_CHANCE ? 2 + random.nextInt(8) : -1;
    }

    /** Air tech when the hitstun ends: relative direction (0 neutral, 5 back, 1 forward), or -1 none. */
    int airTechDirection() {
        if (random.nextDouble() >= AIR_TECH_CHANCE) return -1;
        double r = random.nextDouble();
        return r < 0.40 ? 0 : r < 0.75 ? 5 : 1;
    }

    /** Wake-up: relative direction held while down (7 quick rise, 5/1 rolls, 3 late, 0 normal). */
    int wakeupDirection() {
        double r = random.nextDouble();
        return r < 0.30 ? 7 : r < 0.50 ? 5 : r < 0.62 ? 1 : r < 0.72 ? 3 : 0;
    }
}
