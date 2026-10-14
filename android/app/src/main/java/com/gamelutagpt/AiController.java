package com.gamelutagpt;

import java.util.Random;

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

    // Batalha (IA ON): o kit inteiro, como um jogador usa. Chances por oportunidade.
    /** Frames entre as decisões do kit no neutro (assist, troca, Ultra, Overdrive). */
    static final int KIT_DECISION_FRAMES = 15;
    static final double ASSIST_NEUTRAL_CHANCE = 0.18;
    /** Golpe que acertou: chama o assist para estender o combo. */
    static final double ASSIST_COMBO_CHANCE = 0.45;
    /** Golpe que acertou: cancela no Super. */
    static final double SUPER_COMBO_CHANCE = 0.35;
    /** O próprio Super saiu: DHC com o Super do parceiro. */
    static final double DHC_CHANCE = 0.55;
    /** Assist em campo: Assist → Tag. */
    static final double CONVERT_CHANCE = 0.30;
    /** Vida baixa e parceiro melhor: troca (↓ + TAG). */
    static final double TAG_CHANCE = 0.40;
    static final float TAG_LIFE_RATIO = 0.40f;
    static final double ULTRA_CHANCE = 0.35;
    /** Distância até onde a investida do Ultra alcança (com folga). */
    static final float ULTRA_REACH = 380f;
    /** Overdrive no meio do combo, ou no neutro com a vida baixa. */
    static final double OVERDRIVE_COMBO_CHANCE = 0.15;
    static final double OVERDRIVE_LOW_LIFE_CHANCE = 0.08;
    /** Defendendo: Guard Cancel Tag ou pushblock na sequência de bloqueio. */
    static final double GUARD_CANCEL_CHANCE = 0.25;
    static final double PUSHBLOCK_CHANCE = 0.35;

    private final OpponentAi ai;
    private final OpponentAi.Situation situation = new OpponentAi.Situation();
    /** Sorteios do kit: separados, para não mexer na sequência das decisões do OpponentAi. */
    private final Random kitRandom;
    private boolean fullKit;
    private int kitTimer;
    /** Golpe para o qual os planos de combo já foram sorteados, e em que frame dele. */
    private AttackDefinition plannedAttack;
    private int plannedAttackFrame;
    private boolean assistPlan, superPlan, dhcPlan, overdrivePlan;
    private boolean wasBlocking, guardCancelPlan, pushblockPlan;
    private boolean wasAssistOut, convertPlan;
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
        this(ai, new Random());
    }

    AiController(OpponentAi ai, Random kitRandom) {
        this.ai = ai;
        this.kitRandom = kitRandom;
    }

    /**
     * Batalha: além de andar, bater, soltar energia e Super, o CPU usa o kit inteiro — assist,
     * troca, Assist → Tag, DHC, Guard Cancel, pushblock, Overdrive e Ultra.
     */
    void setFullKit(boolean value) {
        fullKit = value;
        resetKit();
    }

    boolean fullKit() { return fullKit; }

    private void resetKit() {
        kitTimer = 0;
        plannedAttack = null;
        assistPlan = superPlan = dhcPlan = overdrivePlan = false;
        wasBlocking = guardCancelPlan = pushblockPlan = false;
        wasAssistOut = convertPlan = false;
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
        resetKit();
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

        if (fullKit && !target.ko() && fillKit(engine, me, target, out)) return;

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
        situation.projectileActive = engine.energyProjectileAlive(self);
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

    /**
     * O kit da batalha neste frame. Verdadeiro quando apertou algo que pede o frame só para
     * si (a troca, o Ultra e o Overdrive no neutro): a decisão normal fica para depois.
     */
    private boolean fillKit(CombatEngine engine, CombatFighter me, CombatFighter target, FighterInput out) {
        int self = me.index;
        TeamSystem teams = engine.teams;
        TeamSystem.Side team = engine.team(self);

        // Assist em campo: às vezes vira o novo titular (Assist → Tag).
        boolean assistOut = team.assistOut();
        if (assistOut && !wasAssistOut) convertPlan = chance(CONVERT_CHANCE);
        wasAssistOut = assistOut;
        if (convertPlan && team.conversionOpen()) {
            out.assist = true;
            convertPlan = false;
        }

        // Defendendo: Guard Cancel Tag (o parceiro entra batendo) ou pushblock.
        boolean blocking = me.status == CombatFighter.Status.BLOCKSTUN;
        if (blocking && !wasBlocking) {
            guardCancelPlan = chance(GUARD_CANCEL_CHANCE);
            pushblockPlan = chance(PUSHBLOCK_CHANCE);
        }
        wasBlocking = blocking;
        if (blocking) {
            if (guardCancelPlan && teams.guardCancelReady(self, me)) {
                out.assist = true;
                guardCancelPlan = false;
            } else if (pushblockPlan && me.state.superMeter >= engine.config.pushblockCost) {
                out.pushblock = true;
                pushblockPlan = false;
            }
            return false;
        }

        // O próprio golpe: um sorteio por golpe novo, usado quando ele acerta.
        if (me.attacking()) {
            if (me.attack != plannedAttack || me.attackFrame < plannedAttackFrame) {
                plannedAttack = me.attack;
                assistPlan = chance(ASSIST_COMBO_CHANCE);
                superPlan = chance(SUPER_COMBO_CHANCE);
                dhcPlan = chance(DHC_CHANCE);
                overdrivePlan = chance(OVERDRIVE_COMBO_CHANCE);
            }
            plannedAttackFrame = me.attackFrame;
            boolean hit = me.outcome == CombatFighter.Outcome.HIT;
            if (teams.dhcReady(self, me)) {
                if (dhcPlan) out.assist = true;
            } else if (hit && me.attack.kind != AttackDefinition.Kind.SUPER) {
                if (overdrivePlan && teams.canOverdrive(me)) {
                    out.overdrive = true;
                    overdrivePlan = false;
                } else if (assistPlan && teams.canAssist(self, me)) {
                    out.assist = true;
                    assistPlan = false;
                } else if (superPlan && me.state.profile.hasSuperAttack() &&
                    me.state.superMeter >= CombatConfig.SUPER_COST) {
                    // Os cancels do golpe decidem se o Super entra agora.
                    out.superAttack = true;
                }
            }
            return false;
        }
        plannedAttack = null;

        // Neutro: de tempos em tempos olha o que o time e a barra permitem.
        if (!me.canAct() || ++kitTimer < KIT_DECISION_FRAMES) return false;
        kitTimer = 0;
        float distance = Math.abs(target.x - me.x);
        float life = me.state.life / (float)me.state.profile.maxLife;
        FighterState partner = team.partner();
        if (me.grounded && target.grounded && distance <= ULTRA_REACH &&
            me.state.superMeter >= CombatConfig.ULTRA_COST && chance(ULTRA_CHANCE)) {
            out.ultra = true;
            return true;
        }
        if (team.partnerStanding() && !team.tagging() && team.tagCooldown == 0 && !assistOut &&
            life < TAG_LIFE_RATIO && partner.life / (float)partner.profile.maxLife > life + 0.15f &&
            chance(TAG_CHANCE)) {
            out.tag = true;
            return true;
        }
        if (!team.overdriveUsed && life < 0.5f && teams.canOverdrive(me) && chance(OVERDRIVE_LOW_LIFE_CHANCE)) {
            out.overdrive = true;
            return true;
        }
        if (distance >= 150f && distance <= 700f && teams.canAssist(self, me) && chance(ASSIST_NEUTRAL_CHANCE)) {
            // O assist vem junto com o que a decisão normal fizer (andar, energia).
            out.assist = true;
        }
        return false;
    }

    private boolean chance(double p) {
        return kitRandom.nextDouble() < p;
    }

    private void clearRequests() {
        attack = null;
        special = null;
        superAttack = backdash = jump = superJump = grab = false;
    }

}
