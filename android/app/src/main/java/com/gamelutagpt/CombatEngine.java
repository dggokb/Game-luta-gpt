package com.gamelutagpt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Deterministic fixed-step combat simulation of two fighters. Pure Java: no Android, no
 * clocks, no randomness. The same inputs from the same state give the same frames.
 *
 * <p>Order of every {@link #step}, identical for both fighters:
 * <ol>
 *   <li>read inputs, feed the InputBuffer and the MotionParser;</li>
 *   <li>resolve ends of states and cancels (CancelSystem);</li>
 *   <li>advance state machines, movement and projectiles;</li>
 *   <li>detect collisions;</li>
 *   <li>apply reactions, hitstop and pushback;</li>
 *   <li>update the ComboSessions.</li>
 * </ol>
 * A frozen fighter (hitstop or super freeze) skips 2 and 3: none of its counters advance,
 * buffered presses do not expire and its open cancel window stays open.
 */
final class CombatEngine {
    /** One impact of the last step, for effects and HUD. */
    static final class HitEvent {
        final int attacker, defender, damage;
        final boolean blocked, projectile;
        final String moveId;
        HitEvent(int attacker, int defender, int damage, boolean blocked, boolean projectile, String moveId) {
            this.attacker = attacker;
            this.defender = defender;
            this.damage = damage;
            this.blocked = blocked;
            this.projectile = projectile;
            this.moveId = moveId;
        }
    }

    private static final class Contact {
        CombatFighter attacker, defender;
        AttackDefinition attack;
        int hitbox;
        Projectile projectile;
    }

    final CombatConfig config;
    private final CombatFighter[] fighters = new CombatFighter[2];
    /** Point/partner of each side, raw tag, assists and the team meter. */
    final TeamSystem teams;
    final List<Projectile> energyProjectiles = new ArrayList<>();
    final List<Projectile> superProjectiles = new ArrayList<>();
    private final ComboSession[] sessions = new ComboSession[2];
    private final ComboSession[] lastSessions = new ComboSession[2];
    private final int[] sessionEndFrame = {-1, -1};
    private final List<HitEvent> events = new ArrayList<>();
    /**
     * System notices of the last step, for the shell's call-outs and screen shake: "TECH",
     * "PUSHBLOCK", "GUARD_CANCEL", "WALL_BOUNCE", "GROUND_BOUNCE", "OVERDRIVE", "OD_CANCEL".
     * Not hits (no damage).
     */
    private final List<String> cues = new ArrayList<>();
    private final List<Contact> contacts = new ArrayList<>();
    private final float[] pushResult = new float[2];
    private final float[] startX = new float[2];
    private final AttackDefinition[] resolved = new AttackDefinition[1];
    private final int[] autoStep = new int[1];
    private int frame;
    private int superFreeze;
    /** Attacker whose ultra rush connected during the last step, or -1. */
    private int ultraConnected = -1;

    /**
     * Combat data of the ultra. Its damage comes from the cinematic in parts
     * ({@link #applyUltraHit}); this definition drives guard, scaling and blockstun.
     */
    private final AttackDefinition ultraAttack;
    /** Combat data of the throw (damage, scaling, meter); its timing lives in CombatConfig.throw*. */
    private final AttackDefinition throwAttack;

    CombatEngine(FighterState first, FighterState second, float firstX, float secondX, CombatConfig config) {
        this.config = config;
        fighters[0] = new CombatFighter(0, first, firstX, firstX <= secondX ? 1 : -1);
        fighters[1] = new CombatFighter(1, second, secondX, firstX <= secondX ? -1 : 1);
        teams = new TeamSystem(config, first, second);
        ultraAttack = new AttackDefinition.Builder("ULTRA", AttackDefinition.Kind.SUPER)
            .damage(0)
            .frames(config.ultraStartupFrames + config.ultraRushFrames, 1, config.ultraRecoveryFrames)
            .stun(60, config.ultraBlockstunFrames, config.ultraHitstopFrames)
            .windows(null, null, null)
            .pushback(0f, config.ultraPushbackOnBlock)
            .build();
        throwAttack = new AttackDefinition.Builder("THROW", AttackDefinition.Kind.NORMAL)
            .damage(config.throwDamage)
            .frames(config.throwStartupFrames, config.throwActiveFrames, config.throwWhiffFrames)
            .stun(1, 1, config.throwHitstopFrames)
            .windows(null, null, null)
            .launch(AttackDefinition.Launch.KNOCKDOWN)
            .knockback(config.throwKnockback)
            .reach(config.throwRange, 78f)
            .build();
    }

    CombatFighter fighter(int index) { return fighters[index]; }
    int frame() { return frame; }
    List<HitEvent> events() { return events; }
    List<String> cues() { return cues; }
    /** Active combo against {@code defender}, or null. */
    ComboSession session(int defender) { return sessions[defender]; }
    /** Last finished combo against {@code defender}, or null. */
    ComboSession lastSession(int defender) { return lastSessions[defender]; }
    int framesSinceSessionEnd(int defender) {
        return sessionEndFrame[defender] < 0 ? Integer.MAX_VALUE : frame - sessionEndFrame[defender];
    }
    boolean superFreezeActive() { return superFreeze > 0; }
    /** Attacker whose ultra connected in the last step (start the cinematic), or -1. */
    int ultraConnected() { return ultraConnected; }
    /** The fighter's ultra connected and waits for the cinematic to finish. */
    boolean inUltraCinematic(int index) {
        CombatFighter f = fighters[index];
        return f.status == CombatFighter.Status.ULTRA && f.ultraPhase == CombatFighter.ULTRA_CINEMATIC;
    }

    /** The members of a side's team; the first one starts on point. */
    void setTeam(int side, FighterState... members) {
        teams.setMembers(side, members);
        setFighterState(side, members[0]);
    }

    TeamSystem.Side team(int side) { return teams.sides[side]; }

    /** New round for a side (training refills): its Overdrive can be used again. */
    void refreshOverdrive(int side) {
        teams.refreshOverdrive(side);
    }

    /**
     * Demos and training: both fighters stand on the ground at these spots with nothing in
     * progress (no attack, stun, projectile, combo or team action). Life and meter are kept.
     */
    void restage(float firstX, float secondX) {
        float[] xs = {firstX, secondX};
        for (int i = 0; i < 2; i++) {
            int facing = xs[i] <= xs[1 - i] ? 1 : -1;
            fighters[i] = new CombatFighter(i, fighters[i].state, xs[i], facing);
            sessions[i] = null;
            lastSessions[i] = null;
            sessionEndFrame[i] = -1;
        }
        energyProjectiles.clear();
        superProjectiles.clear();
        events.clear();
        cues.clear();
        superFreeze = 0;
        ultraConnected = -1;
        teams.calm();
    }

    /** Tag: the slot now plays another team member. Any action in progress is dropped. */
    void setFighterState(int index, FighterState state) {
        CombatFighter f = fighters[index];
        f.state = state;
        f.status = CombatFighter.Status.NEUTRAL;
        f.clearAttack();
        f.buffer.clear();
        f.motion.clear();
    }

    void step(FighterInput first, FighterInput second) {
        frame++;
        events.clear();
        cues.clear();
        ultraConnected = -1;
        for (int i = 0; i < 2; i++) startX[i] = fighters[i].x;

        // 1. Inputs, buffer and motion parser.
        readInput(fighters[0], first);
        readInput(fighters[1], second);

        // 2. Ends of states, then starts and cancels (the team's tag and assist too).
        for (CombatFighter f : fighters) resolve(f);
        teams.resolve(this, fighters[0], fighters[1]);
        teams.resolve(this, fighters[1], fighters[0]);

        // 3. State machines, movement, projectiles.
        for (CombatFighter f : fighters) f.anticipatedGuard = anticipatedGuard(f);
        for (CombatFighter f : fighters) advance(f);
        teams.advance(this, fighters[0]);
        teams.advance(this, fighters[1]);
        resolveBodies();
        for (CombatFighter f : fighters) updateFacing(f);
        advanceProjectiles();

        // 4. Collisions (simultaneous: trades are possible).
        contacts.clear();
        detectMelee(fighters[0], fighters[1]);
        detectMelee(fighters[1], fighters[0]);
        for (TeamSystem.Side side : teams.sides) {
            if (side.assistPhase == TeamSystem.ASSIST_ATTACK) detectMelee(side.assist, fighters[1 - side.index]);
        }
        detectProjectiles(energyProjectiles);
        detectProjectiles(superProjectiles);

        // 5. Reactions and pushback.
        for (Contact contact : contacts) applyContact(contact);
        detectUltra(fighters[0], fighters[1]);
        detectUltra(fighters[1], fighters[0]);
        updateThrow(fighters[0], fighters[1]);
        updateThrow(fighters[1], fighters[0]);
        updateUltraBeam(fighters[0], fighters[1]);
        updateUltraBeam(fighters[1], fighters[0]);

        // 6. Combo sessions and the team meter.
        updateSessions();
        teams.poolMeter();

        for (int i = 0; i < 2; i++) fighters[i].travel = fighters[i].x - startX[i];
    }

    // ---------------------------------------------------------------- input

    private void readInput(CombatFighter f, FighterInput in) {
        f.input.copyFrom(in);
        teams.readInput(f, in);
        if (f.locked || f.ko()) {
            f.input.clear();
            f.buffer.clear();
            f.pendingJumpAge = -1;
            f.ultraRequestAge = -1;
            f.throwRequestAge = -1;
            f.pushblockRequestAge = -1;
            f.overdriveRequestAge = -1;
            return;
        }
        int relative = MotionParser.relative(in.direction, f.facing);
        f.motion.record(relative, f.clock);
        int clock = f.clock;

        if (f.motion.enteredNow(clock) && ControlsLayout.isUpDirection(relative)) {
            requestJump(f, f.motion.downThenUp(clock, config.superJumpFrames));
        }
        if (in.jump || in.superJump) requestJump(f, in.superJump);
        if (f.motion.doubleTap(1, clock, config.doubleTapFrames) || in.dash) f.dashRequest = true;
        if (f.motion.doubleTap(5, clock, config.doubleTapFrames) || in.backdash) f.backdashRequest = true;

        boolean crouch = ControlsLayout.isDownDirection(in.direction);
        if (in.light || in.medium || in.heavy) {
            String button = in.heavy ? "H" : in.medium ? "M" : "L";
            // Command specials first (longest motion wins), then the energy command.
            String commandSpecial = f.character().matchSpecial(f.motion, button, clock,
                config.motionWindowFrames, config.motionPressFrames);
            int[] command = f.state.profile.energyCommand;
            boolean special = commandSpecial == null && f.state.profile.energy != null &&
                f.motion.matchCommand(command, clock, config.motionWindowFrames, config.motionPressFrames);
            if (commandSpecial != null) {
                f.buffer.push(InputBuffer.Button.SPECIAL, commandSpecial, false);
            } else if (special) {
                // The command only arms the special; the strongest button pressed picks its strength.
                f.buffer.push(InputBuffer.Button.SPECIAL, button, false);
            } else {
                if (in.light) f.buffer.push(InputBuffer.Button.LIGHT, null, crouch);
                if (in.medium) f.buffer.push(InputBuffer.Button.MEDIUM, null, crouch);
                if (in.heavy) f.buffer.push(InputBuffer.Button.HEAVY, null, crouch);
            }
        }
        if (in.special != null) f.buffer.push(InputBuffer.Button.SPECIAL, in.special, false);
        if (in.auto) f.buffer.push(InputBuffer.Button.AUTO, null, crouch);
        if (in.grab) f.throwRequestAge = 0;
        if (in.overdrive) f.overdriveRequestAge = 0;
        if ((in.light || in.medium || in.heavy) && f.status == CombatFighter.Status.AIR_HITSTUN) f.techRequestAge = 0;
        if (in.pushblock) {
            // Only blocking turns M + H into a pushblock; otherwise it is the heavy.
            if (f.status == CombatFighter.Status.BLOCKSTUN) f.pushblockRequestAge = 0;
            else f.buffer.push(InputBuffer.Button.HEAVY, null, crouch);
        }
        if (in.ultra && f.state.superMeter >= CombatConfig.ULTRA_COST) {
            f.ultraRequestAge = 0;
        } else if (in.superAttack) {
            f.buffer.push(InputBuffer.Button.SUPER, null, false);
        }
    }

    private void requestJump(CombatFighter f, boolean superJump) {
        f.pendingJumpAge = 0;
        f.pendingSuperJump = superJump;
    }

    // ---------------------------------------------------------------- resolve

    private void resolve(CombatFighter f) {
        if (f.frozen()) return;
        endFinishedStates(f);
        if (f.locked || f.ko()) {
            f.dashRequest = f.backdashRequest = false;
            return;
        }

        if (f.overdriveRequestAge >= 0 && teams.canOverdrive(f)) {
            overdrive(f);
            f.dashRequest = f.backdashRequest = false;
            return;
        }

        if (f.ultraRequestAge >= 0 && f.status == CombatFighter.Status.NEUTRAL && f.grounded &&
            f.state.superMeter >= CombatConfig.ULTRA_COST) {
            startUltra(f);
            f.dashRequest = f.backdashRequest = false;
            return;
        }

        if (f.pushblockRequestAge >= 0 && f.status == CombatFighter.Status.BLOCKSTUN &&
            f.state.superMeter >= config.pushblockCost) {
            pushblock(f);
            return;
        }

        if (f.throwRequestAge >= 0 && f.grounded &&
            (f.status == CombatFighter.Status.NEUTRAL || throwOverridesJab(f))) {
            startThrow(f);
            f.dashRequest = f.backdashRequest = false;
            return;
        }

        if (f.pendingJumpAge >= 0 && f.grounded) {
            boolean superJump = f.pendingSuperJump || f.launcherChase > 0;
            if (f.status == CombatFighter.Status.NEUTRAL) {
                jump(f, superJump);
            } else if (CancelSystem.canCancel(f, AttackDefinition.JUMP, null)) {
                f.status = CombatFighter.Status.NEUTRAL;
                f.clearAttack();
                jump(f, superJump);
            }
        }

        if (f.status == CombatFighter.Status.NEUTRAL && f.grounded) {
            if (f.backdashRequest) {
                f.backdashFrames = config.backdashFrames;
                f.forwardDashing = false;
            } else if (f.dashRequest) {
                f.forwardDashing = true;
                f.backdashFrames = 0;
            }
        } else if (f.status == CombatFighter.Status.NEUTRAL && !f.grounded && !f.airDashUsed &&
            Arena.GROUND_Y - f.y >= config.airDashMinHeight && (f.dashRequest || f.backdashRequest)) {
            startAirDash(f, f.backdashRequest && !f.dashRequest);
        }
        f.dashRequest = f.backdashRequest = false;

        if (f.status == CombatFighter.Status.NEUTRAL) {
            startFromBuffer(f, false);
        } else if (f.attacking() && (CancelSystem.windowOpen(f) || overdriveCancelOpen(f))) {
            startFromBuffer(f, true);
        }
    }

    /**
     * Overdrive: the team's clock starts and both fighters freeze for the flash (the
     * opponent's stun waits, so a combo can go on). It cancels the fighter's own attack.
     */
    private void overdrive(CombatFighter f) {
        TeamSystem.Side side = teams.sides[f.index];
        side.overdriveUsed = true;
        side.overdriveFrames = config.overdriveFrames;
        f.overdriveRequestAge = -1;
        if (f.attacking()) {
            f.status = CombatFighter.Status.NEUTRAL;
            f.clearAttack();
        }
        f.buffer.clear();
        CombatFighter other = fighters[1 - f.index];
        f.hitstop = Math.max(f.hitstop, config.overdriveFlashFrames);
        other.hitstop = Math.max(other.hitstop, config.overdriveFlashFrames);
        superFreeze = Math.max(superFreeze, config.overdriveFlashFrames);
        cues.add("OVERDRIVE");
    }

    /** In Overdrive an attack that already hit or was blocked cancels into any other move. */
    private boolean overdriveCancelOpen(CombatFighter f) {
        return f.attacking() && teams.overdriveActive(f.index) && f.outcome != CombatFighter.Outcome.NONE &&
            f.attack.kind != AttackDefinition.Kind.SUPER;
    }

    private boolean overdriveCancel(CombatFighter f, AttackDefinition next, ComboSession session) {
        return overdriveCancelOpen(f) && !next.id.equals(f.attack.id) && (session == null || session.allows(next));
    }

    /** Meter for a hit or a block; more of it in Overdrive. */
    private void gainMeter(CombatFighter f, int amount) {
        if (teams.overdriveActive(f.index)) amount = amount * config.overdriveMeterPermille / 1000;
        f.state.addSuperMeter(amount);
    }

    private void endFinishedStates(CombatFighter f) {
        switch (f.status) {
            case ATTACK:
                if (f.attackFrame >= f.attack.totalFrames - 1) {
                    f.status = CombatFighter.Status.NEUTRAL;
                    f.clearAttack();
                }
                break;
            case HITSTUN:
            case BLOCKSTUN:
                if (f.stunLeft == 0) {
                    f.status = CombatFighter.Status.NEUTRAL;
                    f.throwProtect = config.throwProtectFrames;
                }
                break;
            case THROW:
                if ((f.throwPhase == CombatFighter.THROW_WHIFF && f.throwFrame >= config.throwWhiffFrames) ||
                    (f.throwPhase == CombatFighter.THROW_EXECUTE && f.throwFrame >= config.throwExecuteFrames) ||
                    (f.throwPhase == CombatFighter.THROW_TECH && f.throwFrame >= config.throwTechFrames)) {
                    f.status = CombatFighter.Status.NEUTRAL;
                }
                break;
            case THROWN: {
                // Only an attacker holding it keeps a fighter thrown.
                CombatFighter other = fighters[1 - f.index];
                if (other.status != CombatFighter.Status.THROW || other.throwPhase != CombatFighter.THROW_HOLD) {
                    f.status = CombatFighter.Status.NEUTRAL;
                }
                break;
            }
            case AIR_HITSTUN:
                if (f.stunLeft == 0 && !f.slammed && !f.ultraFall && !f.hardFall && f.bounce == CombatFighter.BOUNCE_NONE) {
                    if (f.techRequestAge >= 0) {
                        airTech(f);
                    } else if (++f.freeFallFrames >= config.airTechWindow) {
                        // No tech: it recovers on its own a little later, without the escape.
                        f.status = CombatFighter.Status.NEUTRAL;
                        f.launched = false;
                        f.freeFallFrames = 0;
                    }
                }
                break;
            case KNOCKDOWN: {
                if (f.knockdownFrame >= config.knockdownFallFrames && f.wakeup == CombatFighter.WAKE_NORMAL &&
                    !f.hardKnockdown && !f.ko()) {
                    chooseWakeup(f);
                }
                int down = config.knockdownDownFrames;
                if (f.wakeup == CombatFighter.WAKE_QUICK) down = Math.min(down, config.quickRiseDownFrames);
                if (f.wakeup == CombatFighter.WAKE_DELAY) down += config.delayWakeupFrames;
                boolean roll = f.wakeup == CombatFighter.WAKE_BACK_ROLL || f.wakeup == CombatFighter.WAKE_FORWARD_ROLL;
                if (roll || f.knockdownFrame >= config.knockdownFallFrames + down) {
                    f.status = CombatFighter.Status.WAKEUP;
                    f.knockdownFrame = 0;
                    if (roll) {
                        f.rollFrames = config.rollFrames;
                        int away = f.x <= fighters[1 - f.index].x ? -1 : 1;
                        f.rollDirection = f.wakeup == CombatFighter.WAKE_BACK_ROLL ? away : -away;
                    }
                }
                break;
            }
            case WAKEUP:
                if (f.knockdownFrame >= Math.max(config.wakeupFrames, f.rollFrames > 0 ? config.rollFrames : 0) &&
                    f.rollFrames == 0) {
                    f.status = CombatFighter.Status.NEUTRAL;
                    f.throwProtect = config.throwProtectFrames;
                }
                break;
            case ULTRA:
                if (f.ultraPhase == CombatFighter.ULTRA_RECOVERY && f.ultraFrame >= config.ultraRecoveryFrames) {
                    f.status = CombatFighter.Status.NEUTRAL;
                }
                if (f.ultraPhase == CombatFighter.ULTRA_BEAM &&
                    f.ultraFrame >= config.ultraBeamBlastFrame(f.beamHits) + config.ultraBeamFadeFrames) {
                    f.status = CombatFighter.Status.NEUTRAL;
                }
                break;
            default:
                break;
        }
        if (f.status == CombatFighter.Status.NEUTRAL && f.grounded) {
            f.crouching = ControlsLayout.isDownDirection(f.input.direction);
        }
    }

    private void startFromBuffer(final CombatFighter f, final boolean cancel) {
        final boolean projectileAlive = hasProjectile(energyProjectiles, f.index);
        final ComboSession session = sessions[1 - f.index];
        InputBuffer.Entry entry = f.buffer.select(e -> {
            AttackDefinition next = CancelSystem.resolve(f, e, projectileAlive, null);
            if (next == null) return null;
            if (cancel && !CancelSystem.canCancel(f, next.id, session) && !overdriveCancel(f, next, session)) {
                return null;
            }
            return next;
        }, f.state.profile.inputPriority, resolved);
        if (entry == null) return;
        AttackDefinition next = resolved[0];
        if (cancel && !CancelSystem.canCancel(f, next.id, session)) cues.add("OD_CANCEL");
        CancelSystem.resolve(f, entry, projectileAlive, autoStep);
        f.buffer.consume(entry);
        startAttack(f, next, entry.strength, autoStep[0]);
    }

    private void startAttack(CombatFighter f, AttackDefinition attack, String strength, int step) {
        f.status = CombatFighter.Status.ATTACK;
        f.clearAttack();
        f.attack = attack;
        f.move = attack.kind == AttackDefinition.Kind.NORMAL ? f.character().moveFor(attack.id) : null;
        f.specialStrength = strength;
        f.autoStep = step;
        f.forwardDashing = false;
        f.backdashFrames = 0;
        if (f.move != null) f.crouching = attack.id.startsWith("2");
        if (attack.kind == AttackDefinition.Kind.PROJECTILE && !f.grounded) {
            f.vy *= config.projectileAirVelocityScale;
        }
        if (attack.kind == AttackDefinition.Kind.SUPER) {
            f.state.superMeter -= CombatConfig.SUPER_COST;
            f.state.refreshHudLabels();
            f.vy = 0f;
            // Super freeze: the opponent and the projectiles wait for the release.
            CombatFighter other = fighters[1 - f.index];
            other.hitstop = Math.max(other.hitstop, attack.startupFrames);
            superFreeze = Math.max(superFreeze, attack.startupFrames);
        }
    }

    private void startUltra(CombatFighter f) {
        f.status = CombatFighter.Status.ULTRA;
        f.clearAttack();
        f.ultraPhase = CombatFighter.ULTRA_STARTUP;
        f.ultraFrame = 0;
        f.ultraRequestAge = -1;
        f.pendingJumpAge = -1;
        f.buffer.clear();
        f.forwardDashing = false;
        f.backdashFrames = 0;
        f.crouching = false;
        f.state.superMeter -= CombatConfig.ULTRA_COST;
        f.state.refreshHudLabels();
        // Like the Super: the opponent and the projectiles wait for the rush.
        CombatFighter other = fighters[1 - f.index];
        other.hitstop = Math.max(other.hitstop, config.ultraStartupFrames);
        superFreeze = Math.max(superFreeze, config.ultraStartupFrames);
    }

    /**
     * L + M pressed a couple of frames apart: the jab the first button started becomes the
     * throw while it is still in its first frames (before it could hit).
     */
    private boolean throwOverridesJab(CombatFighter f) {
        if (!f.attacking() || f.move == null || f.attackFrame > PadInput.THROW_LENIENCY_FRAMES) return false;
        String binding = f.move.binding;
        return ("L".equals(binding) || "M".equals(binding)) && f.attackFrame < f.attack.startupFrames;
    }

    /** Air dash: a fixed burst forward (or back) that holds the height; one per jump. */
    private void startAirDash(CombatFighter f, boolean back) {
        f.airDashUsed = true;
        f.airDashBack = back;
        f.airDashDirection = back ? -f.facing : f.facing;
        f.airDashFrames = back ? config.backAirDashFrames : config.airDashFrames;
        f.vy = 0f;
    }

    /** Air tech: control back at once, a short invulnerability and a hop where the stick points. */
    private void airTech(CombatFighter f) {
        CombatFighter other = fighters[1 - f.index];
        int relative = MotionParser.relative(f.input.direction, f.facing);
        f.status = CombatFighter.Status.NEUTRAL;
        f.launched = false;
        f.techRequestAge = -1;
        f.freeFallFrames = 0;
        f.invulnFrames = config.airTechInvulnFrames;
        // The button that asked for the tech must not come out as an air normal.
        f.buffer.clear();
        int away = f.x <= other.x ? -1 : 1;
        if (relative == 4 || relative == 5 || relative == 6) {
            push(f, other, away * config.airTechBackDistance);
        } else if (relative == 1 || relative == 2 || relative == 8) {
            push(f, other, -away * config.airTechForwardDistance);
        } else {
            f.vy = -config.airTechNeutralPop;
        }
        cues.add("AIR_TECH");
    }

    /** Lying down: the direction held picks quick rise (↑), roll (← / →) or a late rise (↓). */
    private void chooseWakeup(CombatFighter f) {
        switch (MotionParser.relative(f.input.direction, f.facing)) {
            case 6: case 7: case 8: f.wakeup = CombatFighter.WAKE_QUICK; break;
            case 4: case 5: f.wakeup = CombatFighter.WAKE_BACK_ROLL; break;
            case 1: case 2: f.wakeup = CombatFighter.WAKE_FORWARD_ROLL; break;
            case 3: f.wakeup = CombatFighter.WAKE_DELAY; break;
            default: break;
        }
    }

    private void startThrow(CombatFighter f) {
        f.status = CombatFighter.Status.THROW;
        f.clearAttack();
        f.throwPhase = CombatFighter.THROW_STARTUP;
        f.throwFrame = 0;
        f.throwRequestAge = -1;
        f.pendingJumpAge = -1;
        f.forwardDashing = false;
        f.backdashFrames = 0;
        f.crouching = false;
    }

    private void jump(CombatFighter f, boolean superJump) {
        f.pendingJumpAge = -1;
        f.grounded = false;
        f.crouching = false;
        f.superJumping = superJump;
        f.vy = -(superJump ? config.superJumpSpeed : config.jumpSpeed);
        f.y -= 2f;
        f.backdashFrames = 0;
        if (superJump) f.launcherChase = 0;
    }

    // ---------------------------------------------------------------- advance

    private void advance(CombatFighter f) {
        if (f.frozen()) {
            f.hitstop--;
            return;
        }
        f.clock++;
        f.buffer.age(config.bufferFrames);
        if (f.pendingJumpAge >= 0 && ++f.pendingJumpAge > config.bufferFrames) f.pendingJumpAge = -1;
        if (f.ultraRequestAge >= 0 && ++f.ultraRequestAge > config.bufferFrames) f.ultraRequestAge = -1;
        if (f.throwRequestAge >= 0 && ++f.throwRequestAge > config.bufferFrames) f.throwRequestAge = -1;
        if (f.pushblockRequestAge >= 0 && ++f.pushblockRequestAge > config.bufferFrames) f.pushblockRequestAge = -1;
        if (f.techRequestAge >= 0 && ++f.techRequestAge > config.bufferFrames) f.techRequestAge = -1;
        if (f.overdriveRequestAge >= 0 && ++f.overdriveRequestAge > config.bufferFrames) f.overdriveRequestAge = -1;
        if (f.invulnFrames > 0) f.invulnFrames--;
        if (f.throwProtect > 0) f.throwProtect--;
        if (f.framesSinceHit < 999) f.framesSinceHit++;
        if (f.framesSinceBlock < 999) f.framesSinceBlock++;
        if (f.launcherChase > 0) f.launcherChase--;

        switch (f.status) {
            case ATTACK:
                f.attackFrame++;
                if (f.attack.kind != AttackDefinition.Kind.NORMAL && f.attackFrame == f.attack.startupFrames) {
                    spawnProjectile(f);
                }
                break;
            case HITSTUN:
            case BLOCKSTUN:
            case AIR_HITSTUN:
                if (f.stunLeft > 0) f.stunLeft--;
                f.stunElapsed++;
                break;
            case KNOCKDOWN:
            case WAKEUP:
                f.knockdownFrame++;
                break;
            case THROW:
                f.throwFrame++;
                break;
            case ULTRA:
                if (f.ultraPhase != CombatFighter.ULTRA_CINEMATIC) f.ultraFrame++;
                if (f.ultraPhase == CombatFighter.ULTRA_STARTUP && f.ultraFrame >= config.ultraStartupFrames) {
                    f.ultraPhase = CombatFighter.ULTRA_RUSH;
                    f.ultraFrame = 0;
                }
                break;
            default:
                break;
        }

        applyPush(f);
        moveHorizontally(f);
        moveVertically(f);
        f.x = Arena.clamp(f.x, Arena.LEFT_BOUND, Arena.RIGHT_BOUND);
    }

    private void applyPush(CombatFighter f) {
        if (f.pushFramesLeft <= 0) return;
        float step = f.pushRemaining / f.pushFramesLeft;
        f.pushRemaining -= step;
        f.pushFramesLeft--;
        float target = f.x + step;
        float clamped = Arena.clamp(target, Arena.LEFT_BOUND, Arena.RIGHT_BOUND);
        f.x = clamped;
        float leftover = target - clamped;
        // Cornered: the rest of the push moves the attacker away instead.
        if (leftover != 0f && f.pushSource != null) {
            f.pushSource.x = Arena.clamp(f.pushSource.x - leftover, Arena.LEFT_BOUND, Arena.RIGHT_BOUND);
        }
    }

    private void moveHorizontally(CombatFighter f) {
        if (f.rolling()) {
            f.x += f.rollDirection * config.rollSpeed * CombatConfig.DT;
            f.rollFrames--;
            return;
        }
        // Overdrive: the team moves faster.
        float boost = teams.overdriveActive(f.index) ? config.overdriveSpeed : 1f;
        if (f.vx != 0f && f.status == CombatFighter.Status.AIR_HITSTUN) {
            f.x += f.vx * CombatConfig.DT;
            boolean reached = f.bounce == CombatFighter.BOUNCE_WALL &&
                ((f.vx > 0f && f.x >= f.bounceWallX) || (f.vx < 0f && f.x <= f.bounceWallX));
            if (reached) {
                // Wall bounce: it hits the wall, comes back a little and pops up.
                f.x = f.bounceWallX;
                f.vx = -Math.signum(f.vx) * config.wallBounceReturnSpeed;
                f.vy = -config.wallBounceUp;
                bounced(f);
                cues.add("WALL_BOUNCE");
            }
            return;
        }
        if (f.airDashFrames > 0) {
            // The burst keeps going under the air normals started from it; a hit stops it.
            if (f.grounded || !(f.status == CombatFighter.Status.NEUTRAL || f.attacking())) {
                f.airDashFrames = 0;
            } else {
                f.x += f.airDashDirection * (f.airDashBack ? config.backAirDashSpeed : config.airDashSpeed) *
                    boost * CombatConfig.DT;
                f.airDashFrames--;
                return;
            }
        }
        int direction = f.input.direction;
        int horizontal = direction == 4 || direction == 5 || direction == 6 ? -1
            : direction == 1 || direction == 2 || direction == 8 ? 1 : 0;
        boolean holdingForward = horizontal != 0 && horizontal == f.facing;
        if (!holdingForward) f.forwardDashing = false;
        float walk = (holdingForward ? config.walkSpeed : config.walkBackSpeed) * boost;

        if (f.status == CombatFighter.Status.NEUTRAL && !f.locked && !f.ko()) {
            if (!f.grounded) {
                f.x += horizontal * walk * CombatConfig.DT;
            } else if (f.backdashFrames > 0) {
                f.x -= f.facing * config.backdashSpeed * boost * CombatConfig.DT;
                f.backdashFrames--;
            } else if (!f.crouching && f.anticipatedGuard == CombatFighter.GUARD_NONE) {
                float speed = f.forwardDashing && holdingForward ? config.dashSpeed * boost : walk;
                f.x += horizontal * speed * CombatConfig.DT;
            }
        } else if (f.status == CombatFighter.Status.ULTRA && f.ultraPhase == CombatFighter.ULTRA_RUSH) {
            f.x += f.facing * config.ultraRushSpeed * CombatConfig.DT;
        } else if (f.attacking() && !f.grounded && f.attack.kind == AttackDefinition.Kind.NORMAL) {
            // Air normals keep the jump's steering; ground attacks and specials lock it.
            f.x += horizontal * walk * CombatConfig.DT;
        }
    }

    private void moveVertically(CombatFighter f) {
        if (f.grounded) return;
        if (f.airDashFrames > 0 && f.status == CombatFighter.Status.NEUTRAL) {
            // Air dash: the height holds until the burst ends (an attack lets gravity back in).
            f.vy = 0f;
            return;
        }
        if (heldByBeam(f)) {
            // Caught in the beam: it stays at the height it was hit.
            f.vy = 0f;
            return;
        }
        float scale = 1f;
        if (f.attacking() && f.attack.kind == AttackDefinition.Kind.PROJECTILE) scale = config.projectileAirGravityScale;
        if (f.attacking() && f.attack.kind == AttackDefinition.Kind.SUPER) {
            scale = 0f;
            f.vy = 0f;
        }
        f.vy += config.gravity * scale * CombatConfig.DT;
        f.y += f.vy * CombatConfig.DT;
        if (f.y >= Arena.GROUND_Y) land(f);
    }

    private void land(CombatFighter f) {
        if (f.status == CombatFighter.Status.AIR_HITSTUN && f.bounce == CombatFighter.BOUNCE_GROUND) {
            // Ground bounce: it hits the floor and pops back up.
            f.y = Arena.GROUND_Y - 1f;
            f.vy = -config.groundBounceUp;
            bounced(f);
            cues.add("GROUND_BOUNCE");
            return;
        }
        f.y = Arena.GROUND_Y;
        f.vy = 0f;
        f.grounded = true;
        f.airDashFrames = 0;
        f.airDashUsed = false;
        f.superJumping = false;
        if (f.attacking() && f.move != null && f.attack.id.startsWith("j")) {
            // Landing ends an air normal.
            f.status = CombatFighter.Status.NEUTRAL;
            f.clearAttack();
        }
        f.vx = 0f;
        f.freeFallFrames = 0;
        if (f.status == CombatFighter.Status.AIR_HITSTUN) {
            if (f.slammed || f.ultraFall || f.hardFall) {
                knockDown(f);
            } else {
                // Remaining hitstun is spent on the ground.
                f.status = CombatFighter.Status.HITSTUN;
                f.hitCrouching = false;
                f.launched = false;
            }
        }
        f.ultraFall = false;
        f.hardFall = false;
        f.bounce = CombatFighter.BOUNCE_NONE;
        if (f.status == CombatFighter.Status.NEUTRAL) f.crouching = ControlsLayout.isDownDirection(f.input.direction);
    }

    private void knockDown(CombatFighter f) {
        f.hardKnockdown = f.ultraFall;
        f.wakeup = CombatFighter.WAKE_NORMAL;
        f.rollFrames = 0;
        f.techRequestAge = -1;
        f.freeFallFrames = 0;
        f.status = CombatFighter.Status.KNOCKDOWN;
        f.clearAttack();
        f.knockdownFrame = 0;
        f.stunLeft = 0;
        f.grounded = true;
        f.airDashFrames = 0;
        f.airDashUsed = false;
        f.y = Arena.GROUND_Y;
        f.vy = 0f;
        f.launched = false;
        f.slammed = false;
        f.ultraFall = false;
        f.hardFall = false;
        f.bounce = CombatFighter.BOUNCE_NONE;
        f.vx = 0f;
        f.superJumping = false;
    }

    /** The bounce happened: fresh hitstun, juggle-able, and a knockdown when it lands. */
    private void bounced(CombatFighter f) {
        f.bounce = CombatFighter.BOUNCE_NONE;
        f.launched = true;
        f.slammed = false;
        f.hardFall = true;
        f.stunLeft = f.stunTotal = config.bounceHitstunFrames;
        f.stunElapsed = 0;
    }

    private void resolveBodies() {
        CombatFighter a = fighters[0], b = fighters[1];
        // A wake-up roll passes through the other body.
        if (a.rolling() || b.rolling()) return;
        if (CombatRules.resolvePush(a.x, a.y, a.body(), b.x, b.y, b.body(),
            Arena.LEFT_BOUND, Arena.RIGHT_BOUND, a.facing, pushResult)) {
            a.x = pushResult[0];
            b.x = pushResult[1];
        }
    }

    private void updateFacing(CombatFighter f) {
        if (f.status != CombatFighter.Status.NEUTRAL || f.frozen()) return;
        CombatFighter other = fighters[1 - f.index];
        int next = f.facing;
        if (f.x < other.x - config.facingEpsilon) next = 1;
        else if (f.x > other.x + config.facingEpsilon) next = -1;
        if (next == f.facing) return;
        f.facing = next;
        // A command started on one side must not finish on the other.
        f.motion.clear();
        f.forwardDashing = false;
        f.backdashFrames = 0;
    }

    // ---------------------------------------------------------------- guard

    /** Guard the held direction asks for, when the fighter is allowed to guard. */
    int guardFromInput(CombatFighter f) {
        boolean free = f.status == CombatFighter.Status.NEUTRAL || f.status == CombatFighter.Status.BLOCKSTUN;
        if (!free || f.locked || f.ko()) return CombatFighter.GUARD_NONE;
        int relative = MotionParser.relative(f.input.direction, f.facing);
        if (!f.grounded) {
            return relative == 4 || relative == 5 || relative == 6 ? CombatFighter.GUARD_AIR : CombatFighter.GUARD_NONE;
        }
        if (relative == 5) return CombatFighter.GUARD_HIGH;
        if (relative == 4) return CombatFighter.GUARD_LOW;
        return CombatFighter.GUARD_NONE;
    }

    /** Guard actually used against an attack: in blockstun the last guard is kept. */
    private int effectiveGuard(CombatFighter f) {
        int guard = guardFromInput(f);
        if (guard == CombatFighter.GUARD_NONE && f.status == CombatFighter.Status.BLOCKSTUN) return f.lastGuard;
        return guard;
    }

    private static boolean guardStops(int guard, AttackDefinition attack, boolean attackerAirborne, boolean projectile) {
        if (guard == CombatFighter.GUARD_NONE) return false;
        if (projectile) return true;
        if (guard == CombatFighter.GUARD_HIGH) return !attack.low;
        if (guard == CombatFighter.GUARD_LOW) return !attackerAirborne;
        return true;
    }

    /** Holding guard while a threat approaches: the fighter stops and shows the guard. */
    private int anticipatedGuard(CombatFighter f) {
        int guard = guardFromInput(f);
        if (guard == CombatFighter.GUARD_NONE) return guard;
        CombatFighter other = fighters[1 - f.index];
        if (other.attacking() && other.attack.kind == AttackDefinition.Kind.NORMAL &&
            other.attack.threatening(Math.max(0, other.attackFrame)) &&
            guardStops(guard, other.attack, !other.grounded, false)) {
            float horizontal = (f.x - other.x) * other.facing;
            if (horizontal >= 0f && horizontal - f.body().halfWidth <= other.attack.reach + 34f) return guard;
        }
        if (projectileThreat(energyProjectiles, f, 24f) || projectileThreat(superProjectiles, f, 58f)) return guard;
        return CombatFighter.GUARD_NONE;
    }

    private static boolean projectileThreat(List<Projectile> projectiles, CombatFighter f, float margin) {
        for (Projectile p : projectiles) {
            if (p.ownerIndex != f.index && (f.x - p.x) * p.direction >= -margin) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- team

    /** The assist reached its spot: it starts the move its pack declares. */
    void startAssistMove(CombatFighter a) {
        CharacterDefinition.Fighter profile = a.state.profile;
        String move = profile.assistMove;
        a.clearAttack();
        a.status = CombatFighter.Status.ATTACK;
        if ("S".equals(move) && profile.energy != null) {
            a.attack = profile.energy.attack;
            a.specialStrength = "M";
            return;
        }
        CharacterDefinition.Move m = a.character().moves.get(move);
        if (m == null) m = a.character().moves.get("H");
        a.move = m;
        a.attack = m.attack;
        a.crouching = move.startsWith("2");
    }

    void spawnAssistProjectile(CombatFighter a) {
        spawnProjectile(a);
    }

    /**
     * Assist → Tag: the assist stays as the new point where it stands and the former point
     * runs off. Refused (the assist just leaves) when the point is being hit or is down.
     */
    boolean convertAssist(TeamSystem.Side side, CombatFighter point) {
        CombatFighter.Status st = point.status;
        if (point.ko() || point.inHitstun() || st == CombatFighter.Status.BLOCKSTUN ||
            st == CombatFighter.Status.KNOCKDOWN || st == CombatFighter.Status.WAKEUP ||
            st == CombatFighter.Status.ULTRA) {
            return false;
        }
        CombatFighter a = side.assist;
        teams.becomePoint(side, point);
        setFighterState(point.index, side.pointState());
        point.x = a.x;
        point.y = Arena.GROUND_Y;
        point.vy = 0f;
        point.grounded = true;
        point.superJumping = false;
        point.crouching = false;
        point.facing = a.facing;
        point.forwardDashing = false;
        point.backdashFrames = 0;
        point.pushRemaining = 0f;
        point.pushFramesLeft = 0;
        point.hitstop = 0;
        point.launcherChase = 0;
        point.locked = false;
        // The new point did not walk there: no walk animation from the jump in position.
        startX[point.index] = point.x;
        return true;
    }

    // ---------------------------------------------------------------- projectiles

    private void spawnProjectile(CombatFighter f) {
        CharacterDefinition.Fighter profile = f.state.profile;
        boolean superKind = f.attack.kind == AttackDefinition.Kind.SUPER;
        CharacterDefinition.Projectile data = superKind ? profile.superAttack : profile.energy;
        float speed = data.speed;
        int damage = data.damage;
        if (!superKind) {
            if ("L".equals(f.specialStrength)) {
                speed *= 0.65f;
                damage = Math.round(damage * 0.60f);
            } else if ("H".equals(f.specialStrength)) {
                speed *= 1.35f;
                damage = Math.round(damage * 1.45f);
            }
        }
        float height = superKind ? data.spawnHeight(false, !f.grounded) : data.spawnHeight(f.crouching, !f.grounded);
        Projectile p = new Projectile(
            f.x + f.facing * data.spawnX, f.y - height, data.range, speed,
            superKind ? 58f : 24f, damage, profile.color, f.index, f.facing, f.attack);
        (superKind ? superProjectiles : energyProjectiles).add(p);
    }

    private void advanceProjectiles() {
        if (superFreeze > 0) {
            superFreeze--;
            return;
        }
        moveProjectiles(energyProjectiles, 120f);
        moveProjectiles(superProjectiles, 180f);
    }

    private static void moveProjectiles(List<Projectile> projectiles, float margin) {
        Iterator<Projectile> it = projectiles.iterator();
        while (it.hasNext()) {
            Projectile p = it.next();
            p.previousX = p.x;
            p.x += p.speed * p.direction * CombatConfig.DT;
            if (Math.abs(p.x - p.startX) >= p.range ||
                p.x > Arena.RIGHT_BOUND + margin || p.x < Arena.LEFT_BOUND - margin) {
                it.remove();
            }
        }
    }

    private static boolean hasProjectile(List<Projectile> projectiles, int owner) {
        for (Projectile p : projectiles) if (p.ownerIndex == owner) return true;
        return false;
    }

    // ---------------------------------------------------------------- collisions

    private void detectMelee(CombatFighter a, CombatFighter d) {
        if (a.frozen() || !a.attacking() || a.attack.kind != AttackDefinition.Kind.NORMAL) return;
        if (!a.attack.isActive(a.attackFrame) || a.hitsOnTarget >= a.attack.maxHits || !d.hittable()) return;
        CharacterDefinition.Body body = d.body();
        boolean crouch = d.crouchingBody();
        for (int i = 0; i < a.attack.hitboxCount(); i++) {
            AttackDefinition.Box box = a.attack.hitbox(i);
            if (!box.activeAt(a.attackFrame) || (a.hitboxMask & (1L << i)) != 0) continue;
            if (CombatRules.hitboxTouches(a.x, a.y, a.facing, box, d.x, d.y, body, crouch) ||
                touchesMoveHurtbox(a, box, d)) {
                addContact(a, d, a.attack, i, null);
                return;
            }
        }
    }

    /** Extra hurtboxes the defender's own move declares (an extended limb). */
    private static boolean touchesMoveHurtbox(CombatFighter a, AttackDefinition.Box hitbox, CombatFighter d) {
        if (!d.attacking() || d.attack.hurtboxCount() == 0) return false;
        if ((d.x - a.x) * a.facing < CombatRules.MIN_MELEE_DISTANCE) return false;
        for (int i = 0; i < d.attack.hurtboxCount(); i++) {
            AttackDefinition.Box hurt = d.attack.hurtbox(i);
            if (!hurt.activeAt(Math.max(0, d.attackFrame))) continue;
            float left = d.facing > 0 ? d.x + hurt.x0 : d.x - hurt.x1;
            float right = d.facing > 0 ? d.x + hurt.x1 : d.x - hurt.x0;
            if (CombatRules.overlaps(a.x, a.y, a.facing, hitbox, left, d.y - hurt.y1, right, d.y - hurt.y0)) return true;
        }
        return false;
    }

    private void detectProjectiles(List<Projectile> projectiles) {
        if (superFreeze > 0) return;
        Iterator<Projectile> it = projectiles.iterator();
        while (it.hasNext()) {
            Projectile p = it.next();
            CombatFighter d = fighters[1 - p.ownerIndex];
            if (!d.hittable()) continue;
            if (CombatRules.projectileHits(p.previousX, p.x, p.y, p.radius, d.x, d.y, d.body(), d.crouchingBody())) {
                addContact(fighters[p.ownerIndex], d, p.attack, -1, p);
                it.remove();
            }
        }
    }

    /** Ultra rush: connects once the defender's body is within reach, else ends in recovery. */
    private void detectUltra(CombatFighter a, CombatFighter d) {
        if (a.status != CombatFighter.Status.ULTRA || a.ultraPhase != CombatFighter.ULTRA_RUSH || a.frozen()) return;
        if (d.hittable() && ultraReaches(a, d)) {
            connectUltra(a, d);
        } else if (a.ultraFrame >= config.ultraRushFrames) {
            a.ultraPhase = CombatFighter.ULTRA_RECOVERY;
            a.ultraFrame = 0;
        }
    }

    private boolean ultraReaches(CombatFighter a, CombatFighter d) {
        float ahead = (d.x - a.x) * a.facing;
        float halfWidth = d.body().halfWidth;
        if (ahead < -halfWidth || ahead - halfWidth > config.ultraReach) return false;
        float chest = a.y - a.body().height(false) * 0.55f;
        return chest >= d.hurtTop() - 40f && chest <= d.y + 10f;
    }

    // ---------------------------------------------------------------- active defense

    /** Pushblock: the attacker is shoved away, the blockstun ends sooner; costs meter. */
    private void pushblock(CombatFighter f) {
        CombatFighter a = fighters[1 - f.index];
        f.pushblockRequestAge = -1;
        f.state.superMeter -= config.pushblockCost;
        f.state.refreshHudLabels();
        f.stunLeft = Math.min(f.stunLeft, config.pushblockStunFrames);
        // Push the attacker away from the defender, whatever side it is on.
        int away = a.x >= f.x ? 1 : -1;
        push(a, f, away * config.pushblockDistance);
        cues.add("PUSHBLOCK");
    }

    /**
     * DHC (Team Super): during the point's released Super, the partner takes the point where
     * it stands and starts its own Super (one more bar, its own freeze); the point runs off.
     * The combo and its scaling go on.
     */
    boolean dhc(TeamSystem.Side side, CombatFighter point) {
        FighterState partner = side.partner();
        if (partner == null || !partner.profile.hasSuperAttack() || point.state.superMeter < CombatConfig.SUPER_COST) {
            return false;
        }
        teams.leavePoint(side, point);
        setFighterState(point.index, side.pointState());
        point.locked = false;
        point.pushRemaining = 0f;
        point.pushFramesLeft = 0;
        startAttack(point, partner.profile.superAttack.attack, null, -1);
        startX[point.index] = point.x;
        cues.add("DHC");
        return true;
    }

    /**
     * Guard Cancel Tag: blocking on the ground, the partner comes in where the point stood,
     * invulnerable, doing its assist move; the point runs off and the opponent freezes for
     * a flash. Costs a bar; refused without the bar, a partner or the team busy.
     */
    boolean guardCancel(TeamSystem.Side side, CombatFighter point) {
        if (point.status != CombatFighter.Status.BLOCKSTUN || !point.grounded ||
            point.state.superMeter < config.guardCancelCost) {
            return false;
        }
        point.state.superMeter -= config.guardCancelCost;
        point.state.refreshHudLabels();
        teams.leavePoint(side, point);
        setFighterState(point.index, side.pointState());
        point.stunLeft = 0;
        point.pushRemaining = 0f;
        point.pushFramesLeft = 0;
        point.locked = false;
        startAssistMove(point);
        point.invulnFrames = point.attack.startupFrames + point.attack.activeFrames + config.guardCancelInvulnPadding;
        CombatFighter other = fighters[1 - point.index];
        other.hitstop = Math.max(other.hitstop, config.guardCancelFlashFrames);
        startX[point.index] = point.x;
        cues.add("GUARD_CANCEL");
        return true;
    }

    // ---------------------------------------------------------------- throw

    /** Throw (L + M): grab in reach, the defender's tech window, then the throw itself. */
    private void updateThrow(CombatFighter a, CombatFighter d) {
        if (a.status != CombatFighter.Status.THROW || a.frozen()) return;
        if (a.throwPhase == CombatFighter.THROW_STARTUP) {
            if (a.throwFrame < config.throwStartupFrames) return;
            if (throwReaches(a, d)) {
                boolean clash = d.status == CombatFighter.Status.THROW &&
                    d.throwPhase == CombatFighter.THROW_STARTUP &&
                    d.throwFrame >= config.throwStartupFrames && throwReaches(d, a);
                if (clash) {
                    // Both grabbed on the same frame: an automatic tech.
                    techThrow(a, d);
                    return;
                }
                if (throwable(d)) {
                    grab(a, d);
                    return;
                }
            }
            if (a.throwFrame >= config.throwStartupFrames + config.throwActiveFrames) {
                a.throwPhase = CombatFighter.THROW_WHIFF;
                a.throwFrame = 0;
            }
        } else if (a.throwPhase == CombatFighter.THROW_HOLD) {
            if (d.throwRequestAge >= 0) {
                techThrow(a, d);
            } else if (a.throwFrame >= config.throwTechWindow) {
                landThrow(a, d);
            }
        }
    }

    /** The defender's body is just in front, both on the ground. */
    private boolean throwReaches(CombatFighter a, CombatFighter d) {
        if (!a.grounded || !d.grounded) return false;
        float ahead = (d.x - a.x) * a.facing;
        float gap = Math.abs(d.x - a.x) - a.body().halfWidth - d.body().halfWidth;
        return ahead >= 0f && gap <= config.throwRange;
    }

    /**
     * Who can be grabbed: standing or crouching on the ground, free or in its own move
     * (not a Super or the ultra). Hitstun, blockstun and the frames right after them or
     * after waking up are throw-invulnerable, so a throw never continues a combo.
     */
    private static boolean throwable(CombatFighter d) {
        if (!d.hittable() || !d.grounded || d.locked || d.throwProtect > 0) return false;
        switch (d.status) {
            case NEUTRAL:
                return true;
            case ATTACK:
                return d.attack.kind != AttackDefinition.Kind.SUPER;
            case THROW:
                return d.throwPhase == CombatFighter.THROW_WHIFF || d.throwPhase == CombatFighter.THROW_EXECUTE;
            default:
                return false;
        }
    }

    private void grab(CombatFighter a, CombatFighter d) {
        d.clearAttack();
        d.status = CombatFighter.Status.THROWN;
        d.forwardDashing = false;
        d.backdashFrames = 0;
        d.crouching = false;
        d.pushRemaining = 0f;
        d.pushFramesLeft = 0;
        // Pulled in: the bodies touch while the tech window is open.
        float wanted = a.x + a.facing * (a.body().halfWidth + d.body().halfWidth + 4f);
        d.x = Arena.clamp(wanted, Arena.LEFT_BOUND, Arena.RIGHT_BOUND);
        if (d.x != wanted) a.x = Arena.clamp(d.x - (wanted - a.x), Arena.LEFT_BOUND, Arena.RIGHT_BOUND);
        a.throwPhase = CombatFighter.THROW_HOLD;
        a.throwFrame = 0;
    }

    /** Tech: no damage, both pushed apart and briefly out of action. */
    private void techThrow(CombatFighter a, CombatFighter d) {
        for (CombatFighter f : new CombatFighter[]{a, d}) {
            f.clearAttack();
            f.status = CombatFighter.Status.THROW;
            f.throwPhase = CombatFighter.THROW_TECH;
            f.throwFrame = 0;
            f.throwRequestAge = -1;
        }
        push(d, a, a.facing * config.throwTechPush);
        push(a, d, -a.facing * config.throwTechPush);
        cues.add("TECH");
    }

    /** No tech in time: damage, knockdown and the attacker's recovery (oki). */
    private void landThrow(CombatFighter a, CombatFighter d) {
        endSession(d.index);
        ComboSession session = new ComboSession(a.index, d.index);
        sessions[d.index] = session;
        int damage = session.registerHit(throwAttack, throwAttack.damage, config);
        d.state.takeDamage(damage, config.recoverableLifePermille);
        gainMeter(a, CombatRules.superGainOnHit(throwAttack.strength));
        d.framesSinceHit = 0;
        knockDown(d);
        push(d, a, a.facing * throwAttack.knockback);
        freeze(a, d, throwAttack.hitstopFrames, false);
        a.throwPhase = CombatFighter.THROW_EXECUTE;
        a.throwFrame = 0;
        events.add(new HitEvent(a.index, d.index, damage, false, false, throwAttack.id));
    }

    /** Guarded: blockstun and recovery. Hit: the attacker waits for the cinematic. */
    private void connectUltra(CombatFighter a, CombatFighter d) {
        int guard = effectiveGuard(d);
        if (guardStops(guard, ultraAttack, !a.grounded, false)) {
            d.status = CombatFighter.Status.BLOCKSTUN;
            d.clearAttack();
            d.stunLeft = d.stunTotal = ultraAttack.blockstunFrames;
            d.stunElapsed = 0;
            d.lastGuard = guard;
            d.framesSinceBlock = 0;
            d.forwardDashing = false;
            d.backdashFrames = 0;
            push(d, a, a.facing * ultraAttack.pushbackOnBlock);
            gainMeter(d, CombatRules.superGainOnGuard(ultraAttack.strength));
            freeze(a, d, ultraAttack.hitstopFrames, false);
            a.ultraPhase = CombatFighter.ULTRA_RECOVERY;
            a.ultraFrame = 0;
            events.add(new HitEvent(a.index, d.index, 0, true, false, ultraAttack.id));
            return;
        }

        ComboSession session = sessions[d.index];
        if (session == null || !d.inHitstun()) {
            endSession(d.index);
            session = new ComboSession(a.index, d.index);
            sessions[d.index] = session;
        }
        a.ultraScale = session.scaleFor(ultraAttack, config);
        a.ultraPhase = CombatFighter.ULTRA_CINEMATIC;
        a.ultraFrame = 0;

        boolean wasCrouching = d.crouchingBody();
        d.clearAttack();
        d.forwardDashing = false;
        d.backdashFrames = 0;
        d.framesSinceHit = 0;
        d.pushRemaining = 0f;
        d.pushFramesLeft = 0;
        d.vx = 0f;
        d.bounce = CombatFighter.BOUNCE_NONE;
        d.status = d.grounded ? CombatFighter.Status.HITSTUN : CombatFighter.Status.AIR_HITSTUN;
        d.hitCrouching = wasCrouching;
        d.stunLeft = d.stunTotal = ultraAttack.hitstunFrames;
        d.stunElapsed = 0;
        ultraConnected = a.index;
    }

    /**
     * One hit of the ultra cinematic, called by the shell while the engine waits: part of
     * the ultra's base damage, scaled by the combo it landed in. Returns the damage dealt.
     */
    int applyUltraHit(int attacker, int baseDamage) {
        if (!inUltraCinematic(attacker)) return 0;
        CombatFighter a = fighters[attacker];
        return dealUltraDamage(a, fighters[1 - attacker], ComboSession.DamageScaling.apply(baseDamage, a.ultraScale));
    }

    /** Damage already scaled; one more hit on the combo counter. */
    private int dealUltraDamage(CombatFighter a, CombatFighter d, int damage) {
        if (d.ko()) return 0;
        d.state.takeDamage(damage, config.recoverableLifePermille);
        d.framesSinceHit = 0;
        ComboSession session = sessions[d.index];
        if (session != null) session.addHit(damage);
        events.add(new HitEvent(a.index, d.index, damage, false, false, ultraAttack.id));
        return damage;
    }

    /** End of the cinematic without a beam: the attacker is free and the defender flies away. */
    void finishUltra(int attacker) {
        if (!inUltraCinematic(attacker)) return;
        CombatFighter a = fighters[attacker];
        a.status = CombatFighter.Status.NEUTRAL;
        a.ultraFrame = 0;
        launchUltraVictim(a, fighters[1 - attacker]);
    }

    /**
     * End of the cinematic with the final beam: the fight resumes with the attacker firing.
     * The beam deals {@code baseDamage} (scaled by the combo) in {@code hits} small hits and
     * a last blast that throws the defender like {@link #finishUltra}.
     */
    void startUltraBeam(int attacker, int baseDamage, int hits) {
        if (!inUltraCinematic(attacker)) return;
        CombatFighter a = fighters[attacker];
        CombatFighter d = fighters[1 - attacker];
        int total = ComboSession.DamageScaling.apply(Math.max(0, baseDamage), a.ultraScale);
        a.ultraPhase = CombatFighter.ULTRA_BEAM;
        a.ultraFrame = 0;
        a.beamHits = Math.max(1, hits);
        a.beamHitsDone = 0;
        a.beamBlastDamage = total * config.ultraBeamBlastPermille / 1000;
        a.beamDamageLeft = total - a.beamBlastDamage;
        placeForUltraBeam(attacker);
        keepInBeam(a, d);
    }

    /**
     * Moves the fighters apart for the beam (the page breaking hides the cut). The defender
     * goes back; against a wall, the attacker steps back instead.
     */
    void placeForUltraBeam(int attacker) {
        CombatFighter a = fighters[attacker];
        CombatFighter d = fighters[1 - attacker];
        float wanted = a.x + a.facing * config.ultraBeamDistance;
        d.x = Arena.clamp(wanted, Arena.LEFT_BOUND, Arena.RIGHT_BOUND);
        if (d.x != wanted) a.x = Arena.clamp(d.x - a.facing * config.ultraBeamDistance, Arena.LEFT_BOUND, Arena.RIGHT_BOUND);
        d.pushRemaining = 0f;
        d.pushFramesLeft = 0;
    }

    /** Frames the beam phase lasts for an ultra beam of this many hits. */
    int ultraBeamFrames(int hits) {
        return config.ultraBeamBlastFrame(Math.max(1, hits)) + config.ultraBeamFadeFrames;
    }

    /** The fighter is caught in the other's beam (before the blast throws it). */
    boolean heldByBeam(CombatFighter f) {
        CombatFighter a = fighters[1 - f.index];
        return a.firingBeam() && a.beamHitsDone <= a.beamHits;
    }

    private void updateUltraBeam(CombatFighter a, CombatFighter d) {
        if (!a.firingBeam() || a.frozen() || a.beamHitsDone > a.beamHits) return;
        if (a.beamHitsDone < a.beamHits) {
            int at = config.ultraBeamFireFrame() + config.ultraBeamExtendFrames +
                config.ultraBeamHitInterval * a.beamHitsDone;
            if (a.ultraFrame < at) return;
            int damage = a.beamDamageLeft / (a.beamHits - a.beamHitsDone);
            a.beamDamageLeft -= damage;
            a.beamHitsDone++;
            dealUltraDamage(a, d, damage);
            keepInBeam(a, d);
            push(d, a, a.facing * config.ultraBeamPushPerHit);
            a.x = Arena.clamp(a.x - a.facing * config.ultraBeamRecoilPerHit, Arena.LEFT_BOUND, Arena.RIGHT_BOUND);
        } else if (a.ultraFrame >= config.ultraBeamBlastFrame(a.beamHits)) {
            a.beamHitsDone++;
            dealUltraDamage(a, d, a.beamBlastDamage + a.beamDamageLeft);
            a.beamDamageLeft = 0;
            launchUltraVictim(a, d);
            freeze(a, d, config.ultraBeamBlastHitstopFrames, false);
        }
    }

    /** The defender stays in hitstun, at its height, while the beam lasts. */
    private void keepInBeam(CombatFighter a, CombatFighter d) {
        d.clearAttack();
        d.vx = 0f;
        d.bounce = CombatFighter.BOUNCE_NONE;
        d.status = d.grounded ? CombatFighter.Status.HITSTUN : CombatFighter.Status.AIR_HITSTUN;
        d.vy = 0f;
        int untilBlast = config.ultraBeamBlastFrame(a.beamHits) - a.ultraFrame;
        d.stunLeft = d.stunTotal = Math.max(d.stunLeft, untilBlast + config.ultraBeamHitInterval * 2);
        d.stunElapsed = 0;
    }

    /** The defender flies away and lands knocked down. */
    private void launchUltraVictim(CombatFighter a, CombatFighter d) {
        d.status = CombatFighter.Status.AIR_HITSTUN;
        d.grounded = false;
        d.y = Math.min(d.y, Arena.GROUND_Y - 2f);
        d.vy = -config.ultraLaunchSpeed;
        d.launched = true;
        d.slammed = false;
        d.superJumping = false;
        d.ultraFall = true;
        d.stunLeft = d.stunTotal = ultraAttack.hitstunFrames;
        d.stunElapsed = 0;
        push(d, a, a.facing * config.ultraKnockback);
        ComboSession session = sessions[d.index];
        if (session != null) session.airCombo = true;
    }

    private void addContact(CombatFighter a, CombatFighter d, AttackDefinition attack, int hitbox, Projectile p) {
        Contact c = new Contact();
        c.attacker = a;
        c.defender = d;
        c.attack = attack;
        c.hitbox = hitbox;
        c.projectile = p;
        contacts.add(c);
    }

    // ---------------------------------------------------------------- reactions

    private void applyContact(Contact c) {
        CombatFighter a = c.attacker, d = c.defender;
        AttackDefinition attack = c.attack;
        boolean projectile = c.projectile != null;
        if (!projectile) {
            a.hitboxMask |= 1L << c.hitbox;
            a.hitsOnTarget++;
        }
        if (!d.hittable()) return;
        int direction = projectile ? c.projectile.direction : a.facing;
        // The owner's S/SUPER learns about its projectile while it still runs.
        boolean ownerMove = !projectile || (a.attacking() && a.attack == attack);
        ComboSession session = sessions[d.index];

        if (d.status == CombatFighter.Status.AIR_HITSTUN && session != null &&
            session.juggleCount + attack.juggleCost > config.juggleLimit) {
            // Juggle limit: the defender recovers in the air instead of taking the hit.
            d.status = CombatFighter.Status.NEUTRAL;
            d.stunLeft = 0;
            d.launched = false;
            d.slammed = false;
            d.ultraFall = false;
            d.hardFall = false;
            d.bounce = CombatFighter.BOUNCE_NONE;
            d.vx = 0f;
            return;
        }

        int guard = effectiveGuard(d);
        if (guardStops(guard, attack, !a.grounded, projectile)) {
            d.status = CombatFighter.Status.BLOCKSTUN;
            d.clearAttack();
            d.stunLeft = d.stunTotal = attack.blockstunFrames;
            d.stunElapsed = 0;
            d.lastGuard = guard;
            d.framesSinceBlock = 0;
            d.forwardDashing = false;
            d.backdashFrames = 0;
            push(d, a, direction * attack.pushbackOnBlock);
            gainMeter(d, CombatRules.superGainOnGuard(attack.strength));
            freeze(a, d, attack.hitstopFrames, projectile);
            if (ownerMove && a.outcome == CombatFighter.Outcome.NONE) a.outcome = CombatFighter.Outcome.BLOCK;
            events.add(new HitEvent(a.index, d.index, 0, true, projectile, attack.id));
            return;
        }

        if (session == null || !d.inHitstun()) {
            endSession(d.index);
            session = new ComboSession(a.index, d.index);
            sessions[d.index] = session;
        }
        int hitstun = session.decayedHitstun(attack.hitstunFrames, config);
        int damage = session.registerHit(attack, projectile ? c.projectile.damage : attack.damage, config);
        d.state.takeDamage(damage, config.recoverableLifePermille);
        gainMeter(a, CombatRules.superGainOnHit(attack.strength));

        boolean wasCrouching = d.crouchingBody();
        d.clearAttack();
        d.forwardDashing = false;
        d.backdashFrames = 0;
        d.framesSinceHit = 0;
        d.stunLeft = d.stunTotal = hitstun;
        d.stunElapsed = 0;
        // A new hit takes over any flight in progress (and any free fall after the hitstun).
        d.freeFallFrames = 0;
        d.vx = 0f;
        d.bounce = CombatFighter.BOUNCE_NONE;
        // The wall bounce is a combo tool: a lone hit in neutral is a plain hit.
        boolean wallBounce = attack.launch == AttackDefinition.Launch.WALL_BOUNCE && session.hitCount >= 2 &&
            session.wallBounces < config.maxWallBounces;
        boolean groundBounce = attack.launch == AttackDefinition.Launch.GROUND_BOUNCE && !d.grounded &&
            session.groundBounces < config.maxGroundBounces;

        if (wallBounce) {
            // Sent flying to the wall (arena edge or screen edge); it bounces in moveHorizontally.
            session.wallBounces++;
            d.status = CombatFighter.Status.AIR_HITSTUN;
            d.grounded = false;
            d.y = Math.min(d.y, Arena.GROUND_Y - 2f);
            d.vx = direction * config.wallBounceSpeed;
            d.bounce = CombatFighter.BOUNCE_WALL;
            d.bounceWallX = Arena.clamp(a.x + direction * config.wallBounceDistance, Arena.LEFT_BOUND, Arena.RIGHT_BOUND);
            // High enough to reach the wall before landing, however close the defender was.
            float flight = Math.abs(d.bounceWallX - d.x) / config.wallBounceSpeed;
            d.vy = -Math.max(config.wallBounceLift, config.gravity * flight * 0.5f * 1.15f);
            d.launched = true;
            d.slammed = false;
            d.superJumping = false;
            d.hardFall = true;
            d.stunLeft = d.stunTotal = Math.max(hitstun, config.bounceHitstunFrames);
            d.pushRemaining = 0f;
            d.pushFramesLeft = 0;
            session.juggleCount += attack.juggleCost;
        } else if (groundBounce) {
            // Driven into the floor; it pops back up in land().
            session.groundBounces++;
            d.status = CombatFighter.Status.AIR_HITSTUN;
            d.vy = config.groundBounceDown;
            d.bounce = CombatFighter.BOUNCE_GROUND;
            d.launched = false;
            d.slammed = false;
            d.hardFall = true;
            d.stunLeft = d.stunTotal = Math.max(hitstun, config.bounceHitstunFrames);
            session.juggleCount += attack.juggleCost;
        } else if (attack.launch == AttackDefinition.Launch.GROUND_BOUNCE && !d.grounded && a.superJumping) {
            // Bounce already used: from a super jump it ends like the slam.
            d.status = CombatFighter.Status.AIR_HITSTUN;
            d.slammed = true;
            d.launched = false;
            d.vy = config.slamSpeed;
            session.juggleCount += attack.juggleCost;
            push(d, a, direction * attack.knockback * 0.35f);
        } else if (attack.launch == AttackDefinition.Launch.KNOCKDOWN && d.grounded) {
            knockDown(d);
            push(d, a, direction * attack.knockback);
        } else if (attack.launch == AttackDefinition.Launch.LAUNCH) {
            d.status = CombatFighter.Status.AIR_HITSTUN;
            d.grounded = false;
            d.y -= 2f;
            d.vy = -config.launchSpeed;
            d.launched = true;
            d.slammed = false;
            d.superJumping = false;
            a.launcherChase = config.launcherChaseFrames;
            session.juggleCount += attack.juggleCost;
            push(d, a, direction * attack.knockback);
        } else if (attack.launch == AttackDefinition.Launch.SLAM && !d.grounded && a.superJumping) {
            d.status = CombatFighter.Status.AIR_HITSTUN;
            d.slammed = true;
            d.launched = false;
            d.vy = config.slamSpeed;
            session.juggleCount += attack.juggleCost;
            push(d, a, direction * attack.knockback * 0.35f);
        } else if (!d.grounded) {
            d.status = CombatFighter.Status.AIR_HITSTUN;
            session.juggleCount += attack.juggleCost;
            push(d, a, direction * attack.knockback);
        } else {
            d.status = CombatFighter.Status.HITSTUN;
            d.hitCrouching = wasCrouching;
            push(d, a, direction * attack.pushbackOnHit);
        }
        if (!d.grounded) session.airCombo = true;
        freeze(a, d, attack.hitstopFrames, projectile);
        if (ownerMove) a.outcome = CombatFighter.Outcome.HIT;
        events.add(new HitEvent(a.index, d.index, damage, false, projectile, attack.id));
    }

    private void push(CombatFighter d, CombatFighter source, float distance) {
        d.pushRemaining = distance;
        d.pushFramesLeft = distance == 0f ? 0 : config.pushbackFrames;
        d.pushSource = source;
    }

    /** Hitstop: both bodies on a melee contact, only the defender for a projectile. */
    private static void freeze(CombatFighter a, CombatFighter d, int frames, boolean projectile) {
        d.hitstop = Math.max(d.hitstop, frames);
        if (!projectile) a.hitstop = Math.max(a.hitstop, frames);
    }

    // ---------------------------------------------------------------- combos

    private void updateSessions() {
        for (int i = 0; i < 2; i++) {
            ComboSession session = sessions[i];
            if (session == null) continue;
            session.comboDuration++;
            CombatFighter d = fighters[i];
            // The combo lasts while the defender cannot act; a knockdown ends it (no hit lands on the ground).
            boolean over = d.ko() ||
                d.status == CombatFighter.Status.NEUTRAL ||
                d.status == CombatFighter.Status.BLOCKSTUN ||
                d.status == CombatFighter.Status.KNOCKDOWN ||
                d.status == CombatFighter.Status.WAKEUP;
            if (over) endSession(i);
        }
    }

    private void endSession(int defender) {
        ComboSession session = sessions[defender];
        if (session == null) return;
        session.active = false;
        lastSessions[defender] = session;
        sessions[defender] = null;
        sessionEndFrame[defender] = frame;
    }
}
