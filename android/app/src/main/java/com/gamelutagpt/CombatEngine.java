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
    final List<Projectile> energyProjectiles = new ArrayList<>();
    final List<Projectile> superProjectiles = new ArrayList<>();
    private final ComboSession[] sessions = new ComboSession[2];
    private final ComboSession[] lastSessions = new ComboSession[2];
    private final int[] sessionEndFrame = {-1, -1};
    private final List<HitEvent> events = new ArrayList<>();
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

    CombatEngine(FighterState first, FighterState second, float firstX, float secondX, CombatConfig config) {
        this.config = config;
        fighters[0] = new CombatFighter(0, first, firstX, firstX <= secondX ? 1 : -1);
        fighters[1] = new CombatFighter(1, second, secondX, firstX <= secondX ? -1 : 1);
        ultraAttack = new AttackDefinition.Builder("ULTRA", AttackDefinition.Kind.SUPER)
            .damage(0)
            .frames(config.ultraStartupFrames + config.ultraRushFrames, 1, config.ultraRecoveryFrames)
            .stun(60, config.ultraBlockstunFrames, config.ultraHitstopFrames)
            .windows(null, null, null)
            .pushback(0f, config.ultraPushbackOnBlock)
            .build();
    }

    CombatFighter fighter(int index) { return fighters[index]; }
    int frame() { return frame; }
    List<HitEvent> events() { return events; }
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
        ultraConnected = -1;
        for (int i = 0; i < 2; i++) startX[i] = fighters[i].x;

        // 1. Inputs, buffer and motion parser.
        readInput(fighters[0], first);
        readInput(fighters[1], second);

        // 2. Ends of states, then starts and cancels.
        for (CombatFighter f : fighters) resolve(f);

        // 3. State machines, movement, projectiles.
        for (CombatFighter f : fighters) f.anticipatedGuard = anticipatedGuard(f);
        for (CombatFighter f : fighters) advance(f);
        resolveBodies();
        for (CombatFighter f : fighters) updateFacing(f);
        advanceProjectiles();

        // 4. Collisions (simultaneous: trades are possible).
        contacts.clear();
        detectMelee(fighters[0], fighters[1]);
        detectMelee(fighters[1], fighters[0]);
        detectProjectiles(energyProjectiles);
        detectProjectiles(superProjectiles);

        // 5. Reactions and pushback.
        for (Contact contact : contacts) applyContact(contact);
        detectUltra(fighters[0], fighters[1]);
        detectUltra(fighters[1], fighters[0]);
        updateUltraBeam(fighters[0], fighters[1]);
        updateUltraBeam(fighters[1], fighters[0]);

        // 6. Combo sessions.
        updateSessions();

        for (int i = 0; i < 2; i++) fighters[i].travel = fighters[i].x - startX[i];
    }

    // ---------------------------------------------------------------- input

    private void readInput(CombatFighter f, FighterInput in) {
        f.input.copyFrom(in);
        if (f.locked || f.ko()) {
            f.input.clear();
            f.buffer.clear();
            f.pendingJumpAge = -1;
            f.ultraRequestAge = -1;
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
            int[] command = f.state.profile.energyCommand;
            boolean special = f.state.profile.energy != null &&
                f.motion.matchCommand(command, clock, config.motionWindowFrames, config.motionPressFrames);
            if (special) {
                // The command only arms the special; the strongest button pressed picks its strength.
                f.buffer.push(InputBuffer.Button.SPECIAL, in.heavy ? "H" : in.medium ? "M" : "L", false);
            } else {
                if (in.light) f.buffer.push(InputBuffer.Button.LIGHT, null, crouch);
                if (in.medium) f.buffer.push(InputBuffer.Button.MEDIUM, null, crouch);
                if (in.heavy) f.buffer.push(InputBuffer.Button.HEAVY, null, crouch);
            }
        }
        if (in.special != null) f.buffer.push(InputBuffer.Button.SPECIAL, in.special, false);
        if (in.auto) f.buffer.push(InputBuffer.Button.AUTO, null, crouch);
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

        if (f.ultraRequestAge >= 0 && f.status == CombatFighter.Status.NEUTRAL && f.grounded &&
            f.state.superMeter >= CombatConfig.ULTRA_COST) {
            startUltra(f);
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
        }
        f.dashRequest = f.backdashRequest = false;

        if (f.status == CombatFighter.Status.NEUTRAL) {
            startFromBuffer(f, false);
        } else if (f.attacking() && CancelSystem.windowOpen(f)) {
            startFromBuffer(f, true);
        }
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
                if (f.stunLeft == 0) f.status = CombatFighter.Status.NEUTRAL;
                break;
            case AIR_HITSTUN:
                if (f.stunLeft == 0 && !f.slammed && !f.ultraFall) {
                    // Air recovery: control returns before landing.
                    f.status = CombatFighter.Status.NEUTRAL;
                    f.launched = false;
                }
                break;
            case KNOCKDOWN:
                if (f.knockdownFrame >= config.knockdownFallFrames + config.knockdownDownFrames) {
                    f.status = CombatFighter.Status.WAKEUP;
                    f.knockdownFrame = 0;
                }
                break;
            case WAKEUP:
                if (f.knockdownFrame >= config.wakeupFrames) f.status = CombatFighter.Status.NEUTRAL;
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
            if (cancel && !CancelSystem.canCancel(f, next.id, session)) return null;
            return next;
        }, f.state.profile.inputPriority, resolved);
        if (entry == null) return;
        AttackDefinition next = resolved[0];
        CancelSystem.resolve(f, entry, projectileAlive, autoStep);
        f.buffer.consume(entry);
        startAttack(f, next, entry.strength, autoStep[0]);
    }

    private void startAttack(CombatFighter f, AttackDefinition attack, String strength, int step) {
        f.status = CombatFighter.Status.ATTACK;
        f.clearAttack();
        f.attack = attack;
        f.move = attack.kind == AttackDefinition.Kind.NORMAL ? f.character().moves.get(attack.id) : null;
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
        int direction = f.input.direction;
        int horizontal = direction == 4 || direction == 5 || direction == 6 ? -1
            : direction == 1 || direction == 2 || direction == 8 ? 1 : 0;
        boolean holdingForward = horizontal != 0 && horizontal == f.facing;
        if (!holdingForward) f.forwardDashing = false;
        float walk = holdingForward ? config.walkSpeed : config.walkBackSpeed;

        if (f.status == CombatFighter.Status.NEUTRAL && !f.locked && !f.ko()) {
            if (!f.grounded) {
                f.x += horizontal * walk * CombatConfig.DT;
            } else if (f.backdashFrames > 0) {
                f.x -= f.facing * config.backdashSpeed * CombatConfig.DT;
                f.backdashFrames--;
            } else if (!f.crouching && f.anticipatedGuard == CombatFighter.GUARD_NONE) {
                float speed = f.forwardDashing && holdingForward ? config.dashSpeed : walk;
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
        f.y = Arena.GROUND_Y;
        f.vy = 0f;
        f.grounded = true;
        f.superJumping = false;
        if (f.attacking() && f.move != null && f.attack.id.startsWith("j")) {
            // Landing ends an air normal.
            f.status = CombatFighter.Status.NEUTRAL;
            f.clearAttack();
        }
        if (f.status == CombatFighter.Status.AIR_HITSTUN) {
            if (f.slammed || f.ultraFall) {
                knockDown(f);
            } else {
                // Remaining hitstun is spent on the ground.
                f.status = CombatFighter.Status.HITSTUN;
                f.hitCrouching = false;
                f.launched = false;
            }
        }
        f.ultraFall = false;
        if (f.status == CombatFighter.Status.NEUTRAL) f.crouching = ControlsLayout.isDownDirection(f.input.direction);
    }

    private void knockDown(CombatFighter f) {
        f.status = CombatFighter.Status.KNOCKDOWN;
        f.clearAttack();
        f.knockdownFrame = 0;
        f.stunLeft = 0;
        f.grounded = true;
        f.y = Arena.GROUND_Y;
        f.vy = 0f;
        f.launched = false;
        f.slammed = false;
        f.ultraFall = false;
        f.superJumping = false;
    }

    private void resolveBodies() {
        CombatFighter a = fighters[0], b = fighters[1];
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
            d.state.addSuperMeter(CombatRules.superGainOnGuard(ultraAttack.strength));
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
        d.state.life = Math.max(0, d.state.life - damage);
        d.state.refreshHudLabels();
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
            d.state.addSuperMeter(CombatRules.superGainOnGuard(attack.strength));
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
        d.state.life = Math.max(0, d.state.life - damage);
        d.state.refreshHudLabels();
        a.state.addSuperMeter(CombatRules.superGainOnHit(attack.strength));

        boolean wasCrouching = d.crouchingBody();
        d.clearAttack();
        d.forwardDashing = false;
        d.backdashFrames = 0;
        d.framesSinceHit = 0;
        d.stunLeft = d.stunTotal = hitstun;
        d.stunElapsed = 0;

        if (attack.launch == AttackDefinition.Launch.KNOCKDOWN && d.grounded) {
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
