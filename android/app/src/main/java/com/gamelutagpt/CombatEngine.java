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
        /** Contact point in world space for presentation (NaN when the move has none, e.g. throws). */
        float impactX = Float.NaN, impactY = Float.NaN;
        /** Button class of the contact, or null when unknown. Presentation only. */
        AttackDefinition.Strength strength;
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
        /** The owner's shadow struck (the owner's own move is not touched). */
        boolean echo;
        /** Qual acerto do projétil de vários acertos é este (0 = o primeiro). */
        int strike;
    }

    /**
     * Shadow echo: a copy of one of the owner's moves, played {@code delay} frames later from
     * the spot where the owner started it. It hits like a projectile (only the defender stops)
     * with a share of the damage. The shell draws it as a tinted copy of the owner.
     */
    static final class Echo {
        final int owner;
        final AttackDefinition attack;
        final CharacterDefinition.Move move;
        final float x, y;
        final int facing;
        final int damagePermille;
        /** Só desenho (os clones do Super do p06): não acerta ninguém. */
        boolean ghost;
        /** Frame of the copied move; negative while it waits for its delay. */
        int frame;
        int hitsOnTarget;
        long hitboxMask;
        Echo(int owner, AttackDefinition attack, CharacterDefinition.Move move, float x, float y, int facing,
             int delay, int damagePermille) {
            this.owner = owner; this.attack = attack; this.move = move; this.x = x; this.y = y;
            this.facing = facing; this.frame = -delay - 1; this.damagePermille = damagePermille;
        }
        boolean visible() { return frame >= 0; }
    }

    final CombatConfig config;
    private final CombatFighter[] fighters = new CombatFighter[2];
    /** Point/partner of each side, raw tag, assists and the team meter. */
    final TeamSystem teams;
    final List<Projectile> energyProjectiles = new ArrayList<>();
    final List<Projectile> superProjectiles = new ArrayList<>();
    /** Poças de veneno no chão (seringa da p11). */
    final List<Puddle> puddles = new ArrayList<>();
    /** Shadow copies of moves (p06's echo) waiting or playing. */
    final List<Echo> echoes = new ArrayList<>();
    /** Henchmen called by a Super (p07): they run in, strike once and run off. */
    final List<Henchman> henchmen = new ArrayList<>();

    /** Dois projéteis de lados opostos que se encontraram neste passo (o shell desenha a faísca). */
    static final class ProjectileClash {
        final float x, y;
        /** Um Super entrou no choque (faísca maior). */
        final boolean big;
        /** Cor de cada lado (lado 0 e lado 1), para a faísca misturar as duas energias. */
        final int firstColor, secondColor;
        /** Para onde ia o projétil do lado 0 (+1 direita); o do lado 1 vinha do outro lado. */
        final int firstDirection;
        ProjectileClash(float x, float y, boolean big, int firstColor, int secondColor, int firstDirection) {
            this.x = x; this.y = y; this.big = big; this.firstColor = firstColor; this.secondColor = secondColor;
            this.firstDirection = firstDirection;
        }
    }
    private final List<ProjectileClash> clashes = new ArrayList<>();

    static final class Henchman {
        static final int RUN_IN = 0, STRIKE = 1, RUN_OUT = 2;
        final int owner;
        /** The second one charges with the shoulder; the first one punches. */
        final boolean shoulder;
        final float y;
        final int damage;
        final AttackDefinition attack;
        float x;
        int direction;
        int phase = RUN_IN;
        int frame;
        /** Frames before it starts running (the second one comes a little later). */
        int delay;
        boolean struck;
        Henchman(int owner, boolean shoulder, float x, float y, int direction, int delay, int damage,
                 AttackDefinition attack) {
            this.owner = owner; this.shoulder = shoulder; this.x = x; this.y = y; this.direction = direction;
            this.delay = delay; this.damage = damage; this.attack = attack;
        }
        boolean visible() { return delay <= 0; }
    }
    private final ComboSession[] sessions = new ComboSession[2];
    private final ComboSession[] lastSessions = new ComboSession[2];
    private final int[] sessionEndFrame = {-1, -1};
    private final List<HitEvent> events = new ArrayList<>();
    /**
     * System notices of the last step, for the shell's call-outs and screen shake: "TECH",
     * "PUSHBLOCK", "GUARD_CANCEL", "WALL_BOUNCE", "GROUND_BOUNCE", "OVERDRIVE", "OD_CANCEL",
     * "PROJECTILE_CLASH", "GUARD_BREAK".
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
    /** Choques de projéteis do último passo (onde, e se um Super entrou). */
    List<ProjectileClash> clashes() { return clashes; }
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
    /**
     * O lado {@code side} zerou no meio de um Ultra que ainda o segura (na página, nos golpes que
     * faltam da sequência ou no raio antes da explosão): o K.O. só vale no golpe final.
     */
    boolean ultraHoldsKo(int side) {
        CombatFighter d = fighters[side], a = fighters[1 - side];
        if (!d.ko() || a.status != CombatFighter.Status.ULTRA) return false;
        if (a.ultraPhase == CombatFighter.ULTRA_CINEMATIC) return true;
        if (a.inUltraSequence()) {
            CharacterDefinition.UltraSequence seq = a.state.profile.ultraSequence;
            return seq != null && d == a.ultraVictim && d.state == a.ultraVictimState &&
                a.ultraHitsDone < seq.hits.length;
        }
        if (a.firingBeam()) return a.beamHitsDone <= a.beamHits;
        return false;
    }

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
        puddles.clear();
        echoes.clear();
        henchmen.clear();
        events.clear();
        cues.clear();
        clashes.clear();
        superFreeze = 0;
        ultraConnected = -1;
        teams.calm();
    }

    /**
     * Batalha 2x2: o titular caído sai e o parceiro que ainda tem vida entra correndo por trás
     * no lugar dele (a entrada da troca), invulnerável até terminar a entrada. Falso quando não
     * há ninguém para entrar (o time perdeu).
     */
    boolean replaceFallenPoint(int side) {
        CombatFighter fallen = fighters[side];
        if (!fallen.ko() || !teams.canReplaceFallen(side)) return false;
        float x = Arena.clamp(fallen.x, Arena.LEFT_BOUND, Arena.RIGHT_BOUND);
        int facing = fighters[1 - side].x >= x ? 1 : -1;
        teams.enterAfterKo(side, facing);
        CombatFighter in = new CombatFighter(side, teams.sides[side].pointState(), x, facing);
        in.locked = true;
        in.invulnFrames = config.tagEnterFrames + config.tagPoseFrames + config.koEntryInvulnPadding;
        fighters[side] = in;
        sessions[side] = null;
        return true;
    }

    /**
     * Troca com voadora ({@link VoadoraTroca}): quem acabou de entrar sai do alto, atrás de onde
     * estava quem saiu, e desce em diagonal até o rival. Falso se o lutador não tem chute no ar
     * (a troca então entra correndo, como antes).
     */
    boolean startTagKick(CombatFighter f) {
        CombatFighter rival = fighters[1 - f.index];
        int toward = rival.x >= f.x ? 1 : -1;
        float startX = Arena.clamp(f.x - toward * VoadoraTroca.BEHIND, Arena.LEFT_BOUND, Arena.RIGHT_BOUND);
        int facing = rival.x >= startX ? 1 : -1;
        float close = (f.body().halfWidth + rival.body().halfWidth) * 0.5f;
        CharacterDefinition.Move kick = VoadoraTroca.from(f.character(),
            VoadoraTroca.forwardSpeed(startX, rival.x, close));
        if (kick == null) return false;
        f.x = startX;
        f.y = Arena.GROUND_Y - VoadoraTroca.HEIGHT;
        f.grounded = false;
        f.vx = f.vy = 0f;
        f.facing = facing;
        f.superJumping = false;
        f.jumpMode = CombatFighter.PULO_LIVRE;
        f.locked = false;
        startAttack(f, kick.attack, "H", 0);
        f.move = kick;
        cues.add("TAG_KICK");
        return true;
    }

    /** Tag: the slot now plays another team member. Any action in progress is dropped. */
    void setFighterState(int index, FighterState state) {
        CombatFighter f = fighters[index];
        f.state = state;
        f.status = CombatFighter.Status.NEUTRAL;
        f.clearAttack();
        f.echoLeft = 0;
        f.buffer.clear();
        f.motion.clear();
    }

    void step(FighterInput first, FighterInput second) {
        frame++;
        events.clear();
        cues.clear();
        clashes.clear();
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
        regenerateGuard();
        resolveBodies();
        for (CombatFighter f : fighters) updateFacing(f);
        advanceProjectiles();
        advanceEchoes();
        advanceHenchmen();

        // 4. Collisions (simultaneous: trades are possible).
        contacts.clear();
        detectMelee(fighters[0], fighters[1]);
        detectMelee(fighters[1], fighters[0]);
        for (TeamSystem.Side side : teams.sides) {
            if (side.assistPhase == TeamSystem.ASSIST_ATTACK) detectMelee(side.assist, fighters[1 - side.index]);
        }
        detectProjectiles(energyProjectiles);
        detectProjectiles(superProjectiles);
        detectEchoes();
        detectHenchmen();

        // 5. Reactions and pushback.
        for (Contact contact : contacts) {
            // The point is measured before pushback moves the defender.
            float ix = impactX(contact), iy = impactY(contact);
            int before = events.size();
            applyContact(contact);
            for (int e = before; e < events.size(); e++) {
                HitEvent event = events.get(e);
                event.impactX = ix;
                event.impactY = iy;
                event.strength = contact.attack.strength;
            }
        }
        detectUltra(fighters[0], fighters[1]);
        detectUltra(fighters[1], fighters[0]);
        updateThrow(fighters[0], fighters[1]);
        updateThrow(fighters[1], fighters[0]);
        updateUltraBeam(fighters[0], fighters[1]);
        updateUltraBeam(fighters[1], fighters[0]);
        updateUltraSequence(fighters[0], fighters[1]);
        updateUltraSequence(fighters[1], fighters[0]);

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
        // Require a fully completed backdash AND a release to neutral before
        // another can be recognized. This avoids backdash chaining by holding
        // backward or repeated double taps while the hop is active.
        if (f.backdashFrames == 0 && f.backdashCooldown == 0 &&
            in.direction == 0) f.backdashArmed = true;
        int relative = MotionParser.relative(in.direction, f.facing);
        f.motion.record(relative, f.clock);
        trackGuardPress(f, relative);
        int clock = f.clock;

        if (f.motion.enteredNow(clock) && ControlsLayout.isUpDirection(relative)) {
            requestJump(f, f.motion.downThenUp(clock, config.superJumpFrames));
            // [↓] ↑: com a carga para baixo pronta o pulo espera um instante pelo botão.
            if (f.grounded && hasDownCharge(f.character()) &&
                f.motion.charged(true, clock, config.chargeFrames, config.chargeKeepFrames)) {
                f.jumpGrace = config.chargeJumpGraceFrames;
            }
        } else if (ControlsLayout.isUpDirection(relative) && f.grounded && f.pendingJumpAge < 0 &&
            f.jumpGrace == 0 && f.status == CombatFighter.Status.NEUTRAL) {
            // ↑ (ou diagonal para cima) seguro desde antes de poder agir — no fim de um golpe, da
            // defesa, da queda: pula assim que fica livre, como nos jogos de luta. Antes só o
            // instante de entrar no ↑ valia, e quem já estava segurando não pulava.
            requestJump(f, false);
        }
        if (in.jump || in.superJump) requestJump(f, in.superJump);
        if (f.motion.doubleTap(1, clock, config.doubleTapFrames) || in.dash) f.dashRequest = true;
        if (f.motion.doubleTap(5, clock, config.doubleTapFrames) || in.backdash) f.backdashRequest = true;

        boolean crouch = ControlsLayout.isDownDirection(in.direction);
        if (in.light || in.medium || in.heavy) {
            String button = in.heavy ? "H" : in.medium ? "M" : "L";
            // O comando de cada personagem: S2..S4 ou a energia (S1); o mais longo ganha.
            String command = f.character().matchCommand(f.motion, button, f.grounded, clock, config);
            if (command != null) {
                // O especial de [↓] ↑ cancela o pulo que o ↑ pediu.
                f.pendingJumpAge = -1;
                f.jumpGrace = 0;
            }
            if (command != null && !"S".equals(command)) {
                f.buffer.push(InputBuffer.Button.SPECIAL, command, false);
            } else if (command != null) {
                // The command only arms the special; the strongest button pressed picks its strength.
                f.buffer.push(InputBuffer.Button.SPECIAL, button, false);
            } else {
                if (in.light) f.buffer.push(InputBuffer.Button.LIGHT, null, crouch);
                if (in.medium) f.buffer.push(InputBuffer.Button.MEDIUM, null, crouch, relative == 1);
                if (in.heavy) f.buffer.push(InputBuffer.Button.HEAVY, null, crouch);
            }
        }
        if (in.special != null) f.buffer.push(InputBuffer.Button.SPECIAL, in.special, false);
        if (in.auto) f.buffer.push(InputBuffer.Button.AUTO, null, crouch);
        if (in.grab) f.throwRequestAge = 0;
        if (in.overdrive) f.overdriveRequestAge = 0;
        if ((in.light || in.medium || in.heavy) && f.status == CombatFighter.Status.AIR_HITSTUN) f.techRequestAge = 0;
        boolean breakerPress = trackBreakerPress(f, in);
        if (in.pushblock) {
            // Blocking, M + H is the pushblock; being hit, the combo breaker; otherwise the heavy.
            if (f.status == CombatFighter.Status.BLOCKSTUN) f.pushblockRequestAge = 0;
            else if (!breakerPress) f.buffer.push(InputBuffer.Button.HEAVY, null, crouch);
        }
        if (in.ultra && f.state.superMeter >= CombatConfig.ULTRA_COST) {
            f.ultraRequestAge = 0;
        } else if (in.superAttack) {
            f.buffer.push(InputBuffer.Button.SUPER, null, false);
        }
    }

    /**
     * Combo breaker: conta os quadros desde o último M + H apertado apanhando (os dois juntos, o
     * ponto entre eles, ou um dedo até {@code comboBreakerPairFrames} depois do outro). O aperto
     * só abre a janela se veio depois do bloqueio contra bater sem parar.
     */
    private boolean trackBreakerPress(CombatFighter f, FighterInput in) {
        boolean press = false;
        if (f.inHitstun()) {
            press = in.pushblock || (in.medium && in.heavy) ||
                (in.medium && f.breakerHeavyAge >= 0) || (in.heavy && f.breakerMediumAge >= 0);
        }
        if (press) {
            f.breakerPressCounts = f.breakerPressAge >= config.comboBreakerLockoutFrames;
            f.breakerPressAge = 0;
            f.breakerMediumAge = f.breakerHeavyAge = -1;
            return true;
        }
        if (f.breakerPressAge < 999) f.breakerPressAge++;
        boolean stunned = f.inHitstun();
        f.breakerMediumAge = stunned && in.medium ? 0
            : f.breakerMediumAge >= 0 && f.breakerMediumAge < config.comboBreakerPairFrames ? f.breakerMediumAge + 1 : -1;
        f.breakerHeavyAge = stunned && in.heavy ? 0
            : f.breakerHeavyAge >= 0 && f.breakerHeavyAge < config.comboBreakerPairFrames ? f.breakerHeavyAge + 1 : -1;
        return false;
    }

    /** O personagem tem um especial de segurar baixo ([↓] ↑)? */
    private static boolean hasDownCharge(CharacterDefinition c) {
        for (CharacterDefinition.Special s : c.specials.values()) {
            if (s.hold && s.command()[0] == 3) return true;
        }
        return c.fighter.energyHold && c.fighter.energyCommand.length > 0 && c.fighter.energyCommand[0] == 3;
    }

    /**
     * Depois do H que bate na parede no combo, segurar → já sai correndo (sem o → →), como o
     * 2H que acerta já transforma o pulo em super pulo.
     */
    private static boolean wallChaseRun(CombatFighter f) {
        return f.wallChase > 0 && !f.forwardDashing && MotionParser.relative(f.input.direction, f.facing) == 1;
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

        if (f.ultraRequestAge >= 0 && f.status == CombatFighter.Status.NEUTRAL && f.grounded && !f.busyOnGround() &&
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

        if (f.throwRequestAge >= 0 && f.grounded && !f.busyOnGround() &&
            (f.status == CombatFighter.Status.NEUTRAL || throwOverridesJab(f))) {
            startThrow(f);
            f.dashRequest = f.backdashRequest = false;
            return;
        }

        if (f.pendingJumpAge >= 0 && !f.grounded && canWallJump(f)) wallJump(f);
        else if (f.pendingJumpAge >= 0 && !f.grounded && f.attacking() &&
            CancelSystem.canCancel(f, AttackDefinition.JUMP, null)) airJump(f);
        if (f.jumpGrace > 0) f.jumpGrace--;
        else if (f.pendingJumpAge >= 0 && f.grounded) {
            boolean superJump = f.pendingSuperJump || f.launcherChase > 0;
            if (f.status == CombatFighter.Status.NEUTRAL) {
                if (!f.busyOnGround()) startJump(f, superJump);
            } else if (CancelSystem.canCancel(f, AttackDefinition.JUMP, null)) {
                f.status = CombatFighter.Status.NEUTRAL;
                f.clearAttack();
                jump(f, superJump);
            }
        }

        if (f.breakerChase > 0) {
            // A corrida do combo breaker: trás ou baixo desiste; golpe, pulo ou chegar perto acaba.
            boolean giveUp = horizontal(f.input.direction) == -f.facing || ControlsLayout.isDownDirection(f.input.direction);
            if (giveUp || f.status != CombatFighter.Status.NEUTRAL) f.breakerChase = 0;
            else if (breakerChasing(f)) f.forwardDashing = true;
            else if (f.grounded) f.breakerChase = 0;
        }

        if (f.status == CombatFighter.Status.NEUTRAL && f.grounded && !f.busyOnGround()) {
            if (f.backdashRequest && f.backdashFrames == 0 &&
                f.backdashCooldown == 0 && f.backdashArmed) {
                f.backdashFrames = config.backdashFrames;
                f.backdashCooldown = config.backdashFrames + config.backdashRecoveryFrames;
                f.backdashArmed = false;
                f.forwardDashing = false;
            } else if ((f.dashRequest || wallChaseRun(f)) && f.backdashFrames == 0) {
                f.forwardDashing = true;
                f.backdashFrames = 0;
                f.wallChase = 0;
            }
        } else if (f.status == CombatFighter.Status.NEUTRAL && !f.grounded && !f.airDashUsed &&
            Arena.GROUND_Y - f.y >= config.airDashMinHeight && (f.dashRequest || f.backdashRequest)) {
            startAirDash(f, f.backdashRequest && !f.dashRequest);
        }
        f.dashRequest = f.backdashRequest = false;

        if (f.status == CombatFighter.Status.NEUTRAL) {
            // Correndo até o outro, o golpe apertado espera chegar perto (não sai de longe). Na
            // preparação do pulo e na aterrissagem do pulo vazio ele espera no buffer.
            if (!breakerChasing(f) && !f.busyOnGround()) startFromBuffer(f, false);
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
                    f.throwProtect = f.status == CombatFighter.Status.BLOCKSTUN
                        ? config.throwProtectAfterBlockFrames : config.throwProtectFrames;
                    f.status = CombatFighter.Status.NEUTRAL;
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
                // Defeated: stays down (the KO is the defeat pose).
                if (f.ko()) break;
                if (f.knockdownFrame >= config.knockdownFallFrames && f.wakeup == CombatFighter.WAKE_NORMAL &&
                    !f.hardKnockdown) {
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
                // Gets up facing the opponent: a forward roll turns around as it passes under it.
                faceOpponent(f);
                if (f.rollFrames == 0 && f.knockdownFrame >= wakeupFrames(f)) {
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
        if (f.buffer.size() == 0) return;
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
        // Golpe que sai de outro golpe continua a corrente; saindo do neutro, começa outra.
        if (!f.attacking()) f.chain.clear();
        f.chain.add(attack.id);
        f.status = CombatFighter.Status.ATTACK;
        f.clearAttack();
        f.attack = attack;
        // O golpe alto avisa na preparação (o brilho na tela), para dar para ler e defender em pé.
        if (attack.overhead) cues.add("OVERHEAD");
        // Normais e especiais (também os que lançam, como a shuriken) tocam a própria animação.
        f.move = attack.kind == AttackDefinition.Kind.SUPER ? null : f.character().moveFor(attack.id);
        f.specialStrength = strength;
        f.autoStep = step;
        f.forwardDashing = false;
        f.backdashFrames = 0;
        if (f.move != null) f.crouching = attack.id.startsWith("2");
        if (attack.mechanics.echoFrames > 0) {
            // The shadow wakes up: the moves started from now on are repeated by it.
            f.echoLeft = attack.mechanics.echoFrames;
            f.echoDelay = attack.mechanics.echoDelay;
            f.echoDamagePermille = attack.mechanics.echoDamagePermille;
            cues.add("ECHO");
        } else if (f.echoLeft > 0 && attack.kind == AttackDefinition.Kind.NORMAL && f.move != null) {
            echoes.add(new Echo(f.index, attack, f.move, f.x, f.y, f.facing, f.echoDelay, f.echoDamagePermille));
        }
        if (attack.mechanics.lifeCost > 0) {
            // Fury: the special is paid with the fighter's own life.
            f.state.payLife(attack.mechanics.lifeCost);
            cues.add("LIFE_COST");
        }
        if (attack.kind == AttackDefinition.Kind.PROJECTILE && !f.grounded) {
            f.vy *= config.projectileAirVelocityScale;
        }
        if (attack.mechanics.dives()) f.vy = 0f;
        if (attack.kind == AttackDefinition.Kind.SUPER) {
            f.state.spendSuperMeter(CombatConfig.SUPER_COST);
            callClones(f, attack);
            f.vy = 0f;
            // Super freeze: the opponent and the projectiles wait for the release. A Super that
            // strikes leaves a few frames to react after the flash (holding guard still blocks).
            int freeze = attack.melee() ? Math.max(0, attack.startupFrames - config.strikingSuperReactFrames)
                                        : attack.startupFrames;
            CombatFighter other = fighters[1 - f.index];
            other.hitstop = Math.max(other.hitstop, freeze);
            superFreeze = Math.max(superFreeze, freeze);
        }
    }

    /** Quadros de atraso entre um clone de sombra e o próximo. */
    private static final int CLONE_DELAY = 10;

    /**
     * Legião de Sombras (p06): clones roxos dela surgem atrás do rival e repetem o Super, um
     * depois do outro. Só desenho: o dano continua o dos golpes dela.
     */
    private void callClones(CombatFighter f, AttackDefinition attack) {
        CharacterDefinition.Projectile sup = f.state.profile.superAttack;
        CharacterDefinition.Animation clip = f.character().specialAnimations.get("SUPER");
        if (sup == null || sup.clones <= 0 || clip == null) return;
        CombatFighter d = fighters[1 - f.index];
        CharacterDefinition.Move move = new CharacterDefinition.Move("SUPER", clip, null, attack);
        float gap = f.body().halfWidth + d.body().halfWidth + 40f;
        for (int i = 0; i < sup.clones; i++) {
            // Atrás do rival, virados para ele; encostado na parede, o clone fica do lado dela.
            float behind = d.x + f.facing * (gap + i * 110f);
            int facing = -f.facing;
            if (behind < Arena.LEFT_BOUND || behind > Arena.RIGHT_BOUND) {
                behind = f.x - f.facing * (60f + i * 110f);
                facing = f.facing;
            }
            Echo clone = new Echo(f.index, attack, move, Arena.clamp(behind, Arena.LEFT_BOUND, Arena.RIGHT_BOUND),
                Arena.GROUND_Y, facing, CLONE_DELAY * (i + 1), 0);
            clone.ghost = true;
            echoes.add(clone);
        }
        cues.add("CLONES");
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
        f.state.spendSuperMeter(CombatConfig.ULTRA_COST);
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
        // L ou M de qualquer jeito que saiu: em pé, agachado (↓) ou o golpe alto (→ + M, andando
        // para frente). Antes só o L e o M em pé: andando ou agachado o L + M quase junto virava golpe.
        boolean lightOrMedium = "L".equals(binding) || "M".equals(binding) || "2L".equals(binding) ||
            "2M".equals(binding) || GolpeAlto.BINDING.equals(binding);
        return lightOrMedium && f.attackFrame < f.attack.startupFrames;
    }

    /** Air dash: a fixed burst forward (or back) that holds the height; one per jump (an acrobat has more). */
    private void startAirDash(CombatFighter f, boolean back) {
        f.airDashUsed = ++f.airDashCount >= Math.max(1, f.state.profile.airDashes);
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
        f.jumpMode = CombatFighter.PULO_LIVRE;
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

    /**
     * Wake-up length of a fighter: its own (timed to its GETUP art) or the engine default,
     * never longer than {@link CombatConfig#maxWakeupFrames}.
     */
    int wakeupFrames(CombatFighter f) {
        CharacterDefinition c = f.character();
        int own = c == null ? 0 : c.fighter.wakeupFrames;
        return Math.min(own > 0 ? own : config.wakeupFrames, config.maxWakeupFrames);
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

    /** Pulo do chão em neutro: agacha {@link CombatConfig#jumpSquatFrames} quadros e sai. */
    private void startJump(CombatFighter f, boolean superJump) {
        if (config.jumpSquatFrames <= 0) {
            jump(f, superJump);
            return;
        }
        f.pendingJumpAge = -1;
        f.jumpSquat = config.jumpSquatFrames;
        f.squatSuper = superJump;
        f.squatDirection = f.input.direction;
        f.crouching = false;
    }

    /** Fim da preparação: sai para a direção que ainda segura para cima, ou a do comando. */
    private void jumpFromSquat(CombatFighter f) {
        int direction = ControlsLayout.isUpDirection(f.input.direction) ? f.input.direction : f.squatDirection;
        jump(f, f.squatSuper, direction);
    }

    private void jump(CombatFighter f, boolean superJump) {
        jump(f, superJump, f.input.direction);
    }

    private void jump(CombatFighter f, boolean superJump, int direction) {
        f.pendingJumpAge = -1;
        f.jumpSquat = 0;
        f.breakerChase = 0;
        int horizontal = horizontal(direction);
        if (f.forwardDashing && f.grounded) {
            // Saindo da corrida, o pulo leva o embalo (dash jump).
            f.jumpMode = CombatFighter.PULO_CORRIDA;
            f.jumpDirection = f.facing;
        } else if (superJump) {
            f.jumpMode = CombatFighter.PULO_SUPER;
            f.jumpDriftVx = horizontal * airSpeed(f, horizontal);
        } else {
            f.jumpMode = CombatFighter.PULO_COMPROMETIDO;
            f.jumpDirection = horizontal;
        }
        f.jumpFrames = 0;
        f.forwardDashing = false;
        f.grounded = false;
        f.crouching = false;
        f.superJumping = superJump;
        f.vy = -(superJump ? config.superJumpSpeed : config.jumpSpeed);
        f.y -= 2f;
        f.backdashFrames = 0;
        if (superJump) f.launcherChase = 0;
    }

    /**
     * Cancelamento de pulo no ar: o golpe aéreo que acertou (jM) vira um novo impulso para cima,
     * uma vez por voo, e a corrente aérea recomeça (jL → jM → pulo → jL → jM → j2H).
     */
    private void airJump(CombatFighter f) {
        f.pendingJumpAge = -1;
        f.status = CombatFighter.Status.NEUTRAL;
        f.clearAttack();
        f.airJumpUsed = true;
        f.vy = -config.airJumpSpeed;
        if (f.jumpMode != CombatFighter.PULO_SUPER) {
            // O pulo no ar escolhe a direção de novo (comprometido).
            f.jumpMode = CombatFighter.PULO_COMPROMETIDO;
            f.jumpDirection = horizontal(f.input.direction);
            f.jumpFrames = 0;
        }
        cues.add("AIR_JUMP");
    }

    // ---------------------------------------------------------------- advance

    private void advance(CombatFighter f) {
        if (f.frozen()) {
            f.hitstop--;
            return;
        }
        f.clock++;
        if (!breakerChasing(f)) f.buffer.age(config.bufferFrames);
        if (f.breakerChase > 0) f.breakerChase--;
        if (f.pendingJumpAge >= 0 && ++f.pendingJumpAge > config.bufferFrames) f.pendingJumpAge = -1;
        if (f.ultraRequestAge >= 0 && ++f.ultraRequestAge > config.bufferFrames) f.ultraRequestAge = -1;
        if (f.throwRequestAge >= 0 && ++f.throwRequestAge > config.throwBufferFrames) f.throwRequestAge = -1;
        if (f.pushblockRequestAge >= 0 && ++f.pushblockRequestAge > config.bufferFrames) f.pushblockRequestAge = -1;
        if (f.techRequestAge >= 0 && ++f.techRequestAge > config.bufferFrames) f.techRequestAge = -1;
        if (f.overdriveRequestAge >= 0 && ++f.overdriveRequestAge > config.bufferFrames) f.overdriveRequestAge = -1;
        if (f.invulnFrames > 0) f.invulnFrames--;
        if (f.throwProtect > 0) f.throwProtect--;
        if (f.backdashCooldown > 0) f.backdashCooldown--;
        if (f.framesSinceHit < 999) f.framesSinceHit++;
        if (f.framesSinceBlock < 999) f.framesSinceBlock++;
        if (f.guardBroken && !f.inHitstun()) f.guardBroken = false;
        if (f.launcherChase > 0) f.launcherChase--;
        if (f.wallChase > 0) f.wallChase--;
        if (f.state.poisoned() && f.state.tickPoison() > 0 && f.state.poisonLeft == 0) cues.add("POISON_END");

        if (f.echoLeft > 0) f.echoLeft--;
        if (f.jumpSquat > 0) {
            // Apanhou, agarrou ou saiu do chão na preparação: o pulo não sai.
            if (f.status != CombatFighter.Status.NEUTRAL || !f.grounded) f.jumpSquat = 0;
            else if (--f.jumpSquat == 0) jumpFromSquat(f);
        }
        if (f.landingLag > 0) {
            if (f.status != CombatFighter.Status.NEUTRAL || !f.grounded) f.landingLag = 0;
            else f.landingLag--;
        }

        switch (f.status) {
            case ATTACK:
                AttackDefinition.Mechanics moveMechanics = f.attack.mechanics;
                if (moveMechanics.chargeFrames > 0 && f.input.holding && f.charge < moveMechanics.chargeFrames &&
                    f.attackFrame == moveMechanics.chargeHoldFrame(f.attack.startupFrames)) {
                    // Charging: the move waits just before it fires while the button is held.
                    f.charge++;
                    f.charging = true;
                    break;
                }
                f.charging = false;
                f.attackFrame++;
                if (!f.attack.melee() && f.attackFrame == f.attack.startupFrames) {
                    spawnProjectile(f);
                }
                if (f.attackFrame == f.attack.mechanics.teleportFrame) teleportBehind(f);
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
                if (f.ultraPhase != CombatFighter.ULTRA_CINEMATIC && !f.ultraPaused) f.ultraFrame++;
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

    /** Some fighter is in the middle of an Ultra sequence (the fallen partner waits for it to end). */
    boolean ultraSequenceRunning() {
        return fighters[0].inUltraSequence() || fighters[1].inUltraSequence();
    }

    /** The opponent's body is right in front (bodies touching): an advance stops there. */
    private boolean touchingAhead(CombatFighter f) {
        CombatFighter o = fighters[1 - f.index];
        float ahead = (o.x - f.x) * f.facing;
        float contact = f.body().pushHalfWidth + o.body().pushHalfWidth + config.advanceContactSlack;
        return ahead > 0f && ahead <= contact && Math.abs(o.y - f.y) < o.body().height(false);
    }

    /** Wall jump: airborne, touching a wall of the arena, and pressing up (once per jump). */
    private boolean canWallJump(CombatFighter f) {
        if (!f.state.profile.wallJump || f.wallJumpUsed || f.status != CombatFighter.Status.NEUTRAL) return false;
        return f.x - Arena.LEFT_BOUND <= config.wallJumpReach || Arena.RIGHT_BOUND - f.x <= config.wallJumpReach;
    }

    private void wallJump(CombatFighter f) {
        f.pendingJumpAge = -1;
        f.wallJumpUsed = true;
        f.wallJumpDirection = f.x - Arena.LEFT_BOUND <= config.wallJumpReach ? 1 : -1;
        f.wallJumpFrames = config.wallJumpFrames;
        f.airDashFrames = 0;
        f.superJumping = false;
        f.vy = -config.jumpSpeed * config.wallJumpLift;
        // Depois do empurrão, o pulo segue para longe da parede (comprometido).
        f.jumpMode = CombatFighter.PULO_COMPROMETIDO;
        f.jumpDirection = f.wallJumpDirection;
        f.jumpFrames = config.jumpDirectionFrames;
        cues.add("WALL_JUMP");
    }

    /** The Super's henchmen come running from behind the caller and share its damage. */
    private void callHenchmen(CombatFighter f) {
        CharacterDefinition.Fighter profile = f.state.profile;
        int count = profile.henchmen;
        int each = profile.superAttack.damage * profile.furyScale(f.state.life) / 1000 / count;
        for (int i = 0; i < count; i++) {
            float x = f.x - f.facing * (config.henchmanEntry + i * 90f);
            henchmen.add(new Henchman(f.index, i % 2 == 1, x, Arena.GROUND_Y, f.facing, i * config.henchmanStagger,
                each, f.attack));
        }
        cues.add("HENCHMEN");
    }

    private void advanceHenchmen() {
        if (superFreeze > 0) return;
        Iterator<Henchman> it = henchmen.iterator();
        while (it.hasNext()) {
            Henchman h = it.next();
            if (h.delay > 0) {
                h.delay--;
                continue;
            }
            h.frame++;
            CombatFighter d = fighters[1 - h.owner];
            if (h.phase == Henchman.RUN_IN) {
                h.x += h.direction * config.henchmanSpeed * CombatConfig.DT;
                if ((d.x - h.x) * h.direction <= config.henchmanReach || h.frame > 120) {
                    h.phase = Henchman.STRIKE;
                    h.frame = 0;
                }
            } else if (h.phase == Henchman.STRIKE) {
                if (h.frame >= config.henchmanStrikeFrames) {
                    // Job done: it turns and runs off the screen.
                    h.phase = Henchman.RUN_OUT;
                    h.direction = -h.direction;
                    h.frame = 0;
                }
            } else {
                h.x += h.direction * config.henchmanSpeed * CombatConfig.DT;
                if (h.frame > 90 || h.x < Arena.LEFT_BOUND - 400f || h.x > Arena.RIGHT_BOUND + 400f) it.remove();
            }
        }
    }

    /** Collision phase: the henchmen whose punch or shoulder reaches its impact frame now. */
    private void detectHenchmen() {
        for (Henchman h : henchmen) {
            if (h.phase != Henchman.STRIKE || h.struck || h.frame != config.henchmanImpactFrame) continue;
            h.struck = true;
            henchmanStrike(h, fighters[1 - h.owner]);
        }
    }

    /** The henchman's punch or shoulder: hits like a projectile (the caller is not frozen). */
    private void henchmanStrike(Henchman h, CombatFighter d) {
        if (!d.hittable() || Math.abs(d.x - h.x) > config.henchmanReach + d.body().halfWidth + 30f) return;
        Contact c = new Contact();
        c.attacker = fighters[h.owner];
        c.defender = d;
        c.attack = h.attack;
        c.hitbox = -1;
        c.echo = true;
        c.projectile = new Projectile(d.x, d.y - d.body().height(false) * 0.6f, 0f, 0f, 1f, h.damage,
            fighters[h.owner].state.profile.color, h.owner, h.direction, h.attack);
        contacts.add(c);
    }

    private void advanceEchoes() {
        if (superFreeze > 0) return;
        Iterator<Echo> it = echoes.iterator();
        while (it.hasNext()) {
            Echo e = it.next();
            if (fighters[e.owner].frozen() && e.frame >= 0) continue;
            e.frame++;
            if (e.frame >= e.attack.totalFrames) it.remove();
        }
    }

    /** The shadow's strike, with the attack's own hitboxes from where the shadow stands. */
    private void detectEchoes() {
        for (Echo e : echoes) {
            if (e.ghost || !e.visible() || !e.attack.isActive(e.frame) || e.hitsOnTarget >= e.attack.maxHits) continue;
            CombatFighter a = fighters[e.owner], d = fighters[1 - e.owner];
            if (!d.hittable() || evades(d, e.attack, false, true)) continue;
            for (int i = 0; i < e.attack.hitboxCount(); i++) {
                AttackDefinition.Box box = e.attack.hitbox(i);
                if (!box.activeAt(e.frame) || (e.hitboxMask & (1L << i)) != 0) continue;
                if (CombatRules.hitboxTouches(e.x, e.y, e.facing, box, d.x, d.y, d.body(),
                    d.crouchingBody() && !hitsCrouching(e.attack, a))) {
                    e.hitboxMask |= 1L << i;
                    e.hitsOnTarget++;
                    Contact c = new Contact();
                    c.attacker = a;
                    c.defender = d;
                    c.attack = e.attack;
                    c.hitbox = i;
                    c.echo = true;
                    // Hits like a projectile: a share of the damage, the owner is not frozen.
                    c.projectile = new Projectile(d.x, d.y - d.body().height(false) * 0.6f, 0f, 0f, 1f,
                        e.attack.damage / e.attack.maxHits * e.damagePermille / 1000, a.state.profile.color, e.owner,
                        e.facing, e.attack);
                    contacts.add(c);
                    break;
                }
            }
        }
    }

    /** Teleport: the fighter reappears just behind the opponent, already facing it. */
    private void teleportBehind(CombatFighter f) {
        CombatFighter other = fighters[1 - f.index];
        int side = f.x <= other.x ? 1 : -1;
        float gap = f.body().halfWidth + other.body().halfWidth + config.teleportGap;
        float target = other.x + side * gap;
        if (target < Arena.LEFT_BOUND || target > Arena.RIGHT_BOUND) {
            // Cornered opponent: there is no room behind it, so the fighter lands in front.
            target = other.x - side * gap;
        }
        f.x = Arena.clamp(target, Arena.LEFT_BOUND, Arena.RIGHT_BOUND);
        f.facing = other.x >= f.x ? 1 : -1;
        f.motion.clear();
        cues.add("TELEPORT");
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
        // The winner has already won: no stale held direction, dash or air
        // momentum may translate the victory animation around the arena.
        // Keep the defeated fighter's knockdown and physics untouched.
        if (!f.ko() && fighters[1 - f.index].ko()) {
            f.forwardDashing = false;
            f.backdashFrames = 0;
            f.airDashFrames = 0;
            return;
        }
        if (f.rolling()) {
            f.x += f.rollDirection * config.rollSpeed * CombatConfig.DT;
            f.rollFrames--;
            return;
        }
        // Apanhar (ou ser agarrado) no ar acaba com o pulo: dali em diante o ar é livre.
        if (!f.grounded && f.status != CombatFighter.Status.NEUTRAL && !f.attacking()) {
            f.jumpMode = CombatFighter.PULO_LIVRE;
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
        if (f.wallJumpFrames > 0) {
            // Off the wall: a push away from it that air control cannot cancel.
            if (f.grounded || f.status != CombatFighter.Status.NEUTRAL && !f.attacking()) {
                f.wallJumpFrames = 0;
            } else {
                f.x += f.wallJumpDirection * config.wallJumpSpeed * boost * CombatConfig.DT;
                f.wallJumpFrames--;
                return;
            }
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
        int horizontal = horizontal(f.input.direction);
        // A corrida do combo breaker anda sozinha, como se → estivesse seguro.
        if (breakerChasing(f)) horizontal = breakerFar(f) ? f.facing : 0;
        boolean holdingForward = horizontal != 0 && horizontal == f.facing;
        // Na preparação do pulo a corrida segue mesmo soltando a frente (o embalo vai para o pulo).
        if (!holdingForward && f.jumpSquat == 0) f.forwardDashing = false;
        float walk = (holdingForward ? config.walkSpeed : config.walkBackSpeed) * boost;

        if (f.status == CombatFighter.Status.NEUTRAL && !f.locked && !f.ko()) {
            if (!f.grounded) {
                airDrift(f, horizontal, walk, boost);
            } else if (f.backdashFrames > 0) {
                f.x -= f.facing * config.backdashSpeed * boost * CombatConfig.DT;
                f.backdashFrames--;
            } else if (!f.crouching && f.anticipatedGuard == CombatFighter.GUARD_NONE && f.landingLag == 0 &&
                (f.jumpSquat == 0 || f.forwardDashing)) {
                if (f.jumpSquat > 0) {
                    // Na preparação do pulo só a corrida segue (o embalo do dash jump).
                    f.x += f.facing * config.dashSpeed * boost * CombatConfig.DT;
                } else {
                    float speed = f.forwardDashing && holdingForward ? config.dashSpeed * boost : walk;
                    f.x += horizontal * speed * CombatConfig.DT;
                }
            }
        } else if (f.status == CombatFighter.Status.ULTRA && f.ultraPhase == CombatFighter.ULTRA_RUSH) {
            f.x += f.facing * config.ultraRushSpeed * CombatConfig.DT;
        } else if (f.attacking() && !f.grounded && f.attack.mechanics.dives()) {
            // Mergulho: parado durante a preparação, depois desce na diagonal (moveVertically).
            if (f.attackFrame >= f.attack.startupFrames) {
                f.x += f.facing * f.attack.mechanics.diveForward * CombatConfig.DT;
            }
        } else if (f.attacking() && f.attack.mechanics.advanceAt(f.attackFrame)) {
            // Rushing special (shoulder charge, flying kick, slide): it carries the body forward
            // until it reaches the opponent (it does not shove it across the stage), or through it.
            if (f.attack.mechanics.passThrough || !touchingAhead(f)) {
                f.x += f.facing * f.attack.mechanics.advanceSpeed * boost * CombatConfig.DT;
            }
        } else if (f.attacking() && !f.grounded && f.attack.kind == AttackDefinition.Kind.NORMAL) {
            // Air normals keep the jump's steering; ground attacks and specials lock it.
            airDrift(f, horizontal, walk, boost);
        }
    }

    /** Lado na tela (-1, 0, 1) de uma direção do stick (0 neutro, depois → ↘ ↓ ↙ ← ↖ ↑ ↗). */
    private static int horizontal(int direction) {
        return direction == 4 || direction == 5 || direction == 6 ? -1
            : direction == 1 || direction == 2 || direction == 8 ? 1 : 0;
    }

    /** Velocidade de lado no ar: a de andar para frente, ou a de andar para trás. */
    private float airSpeed(CombatFighter f, int horizontal) {
        if (horizontal == 0) return 0f;
        return horizontal == f.facing ? config.walkSpeed : config.walkBackSpeed;
    }

    /** No ar: o lado do pulo conforme o modo da saída ({@link CombatFighter#jumpMode}). */
    private void airDrift(CombatFighter f, int horizontal, float walk, float boost) {
        switch (f.jumpMode) {
            case CombatFighter.PULO_CORRIDA:
                f.x += f.jumpDirection * config.dashJumpSpeed * boost * CombatConfig.DT;
                break;
            case CombatFighter.PULO_COMPROMETIDO:
                // Logo depois de sair do chão ainda dá para escolher a direção; depois, fixa.
                if (f.jumpFrames < config.jumpDirectionFrames && horizontal != 0) f.jumpDirection = horizontal;
                f.x += f.jumpDirection * airSpeed(f, f.jumpDirection) * boost * CombatConfig.DT;
                break;
            case CombatFighter.PULO_SUPER: {
                // Drift: o stick puxa aos poucos; solto, o embalo continua.
                if (horizontal != 0) {
                    float alvo = horizontal * airSpeed(f, horizontal);
                    float passo = config.superJumpDriftAccel * CombatConfig.DT;
                    f.jumpDriftVx += Math.max(-passo, Math.min(passo, alvo - f.jumpDriftVx));
                }
                f.x += f.jumpDriftVx * boost * CombatConfig.DT;
                break;
            }
            default:
                f.x += horizontal * walk * CombatConfig.DT;
                break;
        }
        f.jumpFrames++;
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
        // Ordinary neutral jumps now rise/fall briskly; preserve the superjump
        // arc, juggle/launch physics and specialized projectile trajectories.
        if (!f.superJumping && (f.status == CombatFighter.Status.NEUTRAL ||
            (f.attacking() && f.attack.kind == AttackDefinition.Kind.NORMAL))) {
            scale = f.vy < 0f ? config.normalJumpRisingGravityScale
                              : config.normalJumpFallingGravityScale;
        }
        if (f.attacking() && f.attack.kind == AttackDefinition.Kind.PROJECTILE) scale = config.projectileAirGravityScale;
        if (f.attacking() && f.attack.kind == AttackDefinition.Kind.SUPER) {
            scale = 0f;
            f.vy = 0f;
        }
        if (f.attacking() && f.attack.mechanics.dives()) {
            scale = 0f;
            f.vy = f.attackFrame >= f.attack.startupFrames ? f.attack.mechanics.diveDescent : 0f;
        }
        f.vy += config.gravity * scale * CombatConfig.DT;
        f.y += f.vy * CombatConfig.DT;
        if (f.y >= Arena.GROUND_Y) land(f);
    }

    private void land(CombatFighter f) {
        // Pulo vazio: aterrissou em neutro, sem golpe no ar.
        boolean emptyJump = f.status == CombatFighter.Status.NEUTRAL && !f.grounded;
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
        f.jumpMode = CombatFighter.PULO_LIVRE;
        f.airDashFrames = 0;
        f.airDashUsed = false;
        f.airJumpUsed = false;
        f.airDashCount = 0;
        f.wallJumpUsed = false;
        f.wallJumpFrames = 0;
        f.superJumping = false;
        if (f.attacking() && f.move != null && f.attack.id.startsWith("j")) {
            // Landing ends an air normal.
            f.status = CombatFighter.Status.NEUTRAL;
            f.clearAttack();
        } else if (f.attacking() && f.attack.mechanics.dives()) {
            // O mergulho para no chão: sobra só um pouco da recuperação.
            f.attackFrame = Math.max(f.attackFrame, f.attack.totalFrames - config.diveLandingFrames);
        }
        f.vx = 0f;
        f.freeFallFrames = 0;
        if (f.status == CombatFighter.Status.AIR_HITSTUN) {
            // A fighter KO'd in the air lands lying down, not on its feet.
            if (f.slammed || f.ultraFall || f.hardFall || f.ko()) {
                knockDown(f);
            } else {
                // Remaining hitstun is spent on the ground.
                f.status = CombatFighter.Status.HITSTUN;
                f.stunProjectile = null;
                f.hitCrouching = false;
                f.launched = false;
            }
        }
        f.ultraFall = false;
        f.hardFall = false;
        f.spinning = false;
        f.bounce = CombatFighter.BOUNCE_NONE;
        if (f.status == CombatFighter.Status.NEUTRAL) f.crouching = ControlsLayout.isDownDirection(f.input.direction);
        if (emptyJump && f.status == CombatFighter.Status.NEUTRAL) f.landingLag = config.jumpLandingFrames;
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
        f.jumpMode = CombatFighter.PULO_LIVRE;
        f.airDashFrames = 0;
        f.airDashUsed = false;
        f.airJumpUsed = false;
        f.airDashCount = 0;
        f.wallJumpUsed = false;
        f.wallJumpFrames = 0;
        f.y = Arena.GROUND_Y;
        f.vy = 0f;
        f.launched = false;
        f.slammed = false;
        f.ultraFall = false;
        f.hardFall = false;
        f.spinning = false;
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
        // A wake-up roll passes through the other body, and so does a cross-up rush.
        if (a.rolling() || b.rolling() || passingThrough(a) || passingThrough(b)) return;
        if (CombatRules.resolvePush(a.x, a.y, a.body(), b.x, b.y, b.body(),
            Arena.LEFT_BOUND, Arena.RIGHT_BOUND, a.facing, pushResult)) {
            a.x = pushResult[0];
            b.x = pushResult[1];
        }
    }

    private static boolean passingThrough(CombatFighter f) {
        AttackDefinition.Mechanics m = f.mechanics();
        return m.passThrough && m.advanceAt(f.attackFrame);
    }

    /** Turns toward the opponent, without the command cleanup of {@link #updateFacing}. */
    private void faceOpponent(CombatFighter f) {
        CombatFighter other = fighters[1 - f.index];
        if (f.x < other.x - config.facingEpsilon) f.facing = 1;
        else if (f.x > other.x + config.facingEpsilon) f.facing = -1;
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

    /**
     * Defesa perfeita: conta os quadros desde o último aperto novo de trás (no ar, qualquer
     * defesa do ar). O aperto só abre a janela se veio depois do bloqueio contra bater sem parar.
     */
    private void trackGuardPress(CombatFighter f, int relative) {
        boolean back = relative == 4 || relative == 5 || (!f.grounded && relative == 6);
        if (back && !f.guardDirectionHeld) {
            f.guardPressCounts = f.guardPressAge >= config.justDefenseLockoutFrames;
            f.guardPressAge = 0;
        } else if (f.guardPressAge < 999) {
            f.guardPressAge++;
        }
        f.guardDirectionHeld = back;
    }

    /** O aperto de trás está na janela da defesa perfeita. */
    private boolean justDefense(CombatFighter d) {
        return d.guardPressCounts && d.guardDirectionHeld && d.guardPressAge <= config.justDefenseFrames;
    }

    /**
     * Combo breaker: o golpe que fecha a sequência (forte ou especial, corpo a corpo, a última
     * batida dele) num combo que já tem {@code comboBreakerMinHits} acertos, com M + H apertado
     * em cima da hora e barra para pagar.
     */
    private boolean comboBreaks(CombatFighter a, CombatFighter d, AttackDefinition attack, boolean projectile,
                                boolean echo, boolean lastStrike) {
        if (projectile || echo || !lastStrike || !d.inHitstun()) return false;
        if (attack.strength != AttackDefinition.Strength.HEAVY && attack.strength != AttackDefinition.Strength.SPECIAL) {
            return false;
        }
        ComboSession session = sessions[d.index];
        if (session == null || !session.active || session.hitCount < config.comboBreakerMinHits) return false;
        if (d.state.superMeter < config.comboBreakerCost) return false;
        return d.breakerPressCounts && d.breakerPressAge <= config.comboBreakerFrames;
    }

    /**
     * Quebra o combo: o golpe não entra, quem apanhava fica livre na hora (no ar, cai livre) e
     * quem batia é jogado para longe, arrastando o pé, e fica atordoado, sem o golpe. Quem quebrou
     * sai correndo sozinho até ele (sem o → →) para dar o troco.
     */
    private void comboBreaker(CombatFighter a, CombatFighter d) {
        d.state.spendSuperMeter(config.comboBreakerCost);
        d.breakerPressCounts = false;
        d.guardPressCounts = false;
        endSession(d.index);
        d.status = CombatFighter.Status.NEUTRAL;
        d.stunLeft = d.stunTotal = 0;
        d.stunElapsed = 0;
        d.launched = false;
        d.slammed = false;
        d.ultraFall = false;
        d.hardFall = false;
        d.spinning = false;
        d.bounce = CombatFighter.BOUNCE_NONE;
        d.vx = 0f;
        d.pushRemaining = 0f;
        d.pushFramesLeft = 0;
        d.comboBroke = true;
        // O que sobrou do M e do H no buffer não sai de longe: o troco é apertado na corrida.
        d.buffer.clear();
        d.breakerChase = config.comboBreakerChaseFrames;

        a.comboBroke = false;
        a.clearAttack();
        a.status = a.grounded ? CombatFighter.Status.HITSTUN : CombatFighter.Status.AIR_HITSTUN;
        a.stunProjectile = null;
        a.hitCrouching = false;
        a.stunLeft = a.stunTotal = config.comboBreakerStunFrames;
        a.stunElapsed = 0;
        a.pushRemaining = 0f;
        a.pushFramesLeft = 0;
        a.forwardDashing = false;
        a.breakerChase = 0;
        // O estalo joga quem batia para longe de quem quebrou: só correndo para alcançar.
        float away = a.x == d.x ? -d.facing : Math.signum(a.x - d.x);
        if (a.grounded) {
            a.vx = 0f;
            float wanted = a.body().halfWidth + d.body().halfWidth + config.comboBreakerPushGap;
            float gap = Math.abs(a.x - d.x);
            if (gap < wanted) {
                // Sem quem empurra: encurralado, ele só para na parede (quem quebrou não recua).
                push(a, null, away * (wanted - gap));
                a.pushFramesLeft = config.comboBreakerPushFrames;
            }
        } else {
            a.vx = away * config.comboBreakerAirPushSpeed;
        }
        freeze(a, d, config.comboBreakerHitstopFrames, false);
        cues.add("COMBO_BREAKER");
    }

    /**
     * Depois do combo breaker, quem quebrou corre sozinho até o outro: livre e no chão, sem
     * segurar trás nem baixo, enquanto ele ainda desliza ou está longe.
     */
    private boolean breakerChasing(CombatFighter f) {
        if (f.breakerChase <= 0 || f.status != CombatFighter.Status.NEUTRAL || !f.grounded || f.locked || f.ko()) {
            return false;
        }
        return fighters[1 - f.index].pushFramesLeft > 0 || breakerFar(f);
    }

    /** Ainda fora do alcance do L: a corrida anda. */
    private boolean breakerFar(CombatFighter f) {
        CombatFighter other = fighters[1 - f.index];
        float close = f.body().halfWidth + other.body().halfWidth + config.comboBreakerCloseGap;
        return Math.abs(other.x - f.x) > close;
    }

    /** Arranhão de uma batida defendida: só especial e Super/Ultra. */
    private int chipFor(AttackDefinition attack, int strikeDamage) {
        int permille = attack.strength == AttackDefinition.Strength.SUPER ? config.chipSuperPermille
            : attack.strength == AttackDefinition.Strength.SPECIAL ? config.chipSpecialPermille : 0;
        return strikeDamage * permille / 1000;
    }

    /** Guard actually used against an attack: in blockstun the last guard is kept. */
    private int effectiveGuard(CombatFighter f) {
        int guard = guardFromInput(f);
        if (guard == CombatFighter.GUARD_NONE && f.status == CombatFighter.Status.BLOCKSTUN) return f.lastGuard;
        return guard;
    }

    private static boolean guardStops(int guard, AttackDefinition attack, boolean attackerAirborne, boolean projectile) {
        if (guard == CombatFighter.GUARD_NONE) return false;
        // Projétil baixo (a sombra do p06, rente ao chão): só a defesa agachada para.
        if (projectile) return guard != CombatFighter.GUARD_HIGH || !attack.low;
        if (guard == CombatFighter.GUARD_HIGH) return !attack.low;
        // Agachado não segura o golpe no ar nem o golpe alto de chão.
        if (guard == CombatFighter.GUARD_LOW) return !attackerAirborne && !attack.overhead;
        return true;
    }

    /** Holding guard while a threat approaches: the fighter stops and shows the guard. */
    private int anticipatedGuard(CombatFighter f) {
        int guard = guardFromInput(f);
        if (guard == CombatFighter.GUARD_NONE) return guard;
        CombatFighter other = fighters[1 - f.index];
        if (other.attacking() && other.attack.melee() &&
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
            if (p.ownerIndex != f.index && !p.returning && (f.x - p.x) * p.direction >= -margin) return true;
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
        point.wallChase = 0;
        point.breakerChase = 0;
        point.locked = false;
        // The new point did not walk there: no walk animation from the jump in position.
        startX[point.index] = point.x;
        return true;
    }

    // ---------------------------------------------------------------- projectiles

    private void spawnProjectile(CombatFighter f) {
        CharacterDefinition.Fighter profile = f.state.profile;
        boolean superKind = f.attack.kind == AttackDefinition.Kind.SUPER;
        if (superKind && profile.henchmen > 0) {
            callHenchmen(f);
            return;
        }
        // Especial que lança (shuriken do p05, seringa da p11): o projétil vem do especial.
        CharacterDefinition.Special special = superKind ? null : f.character().specials.get(f.attack.id);
        CharacterDefinition.Projectile data = superKind ? profile.superAttack
            : special != null && special.projectile != null ? special.projectile : profile.energy;
        boolean energy = !superKind && data == profile.energy;
        float speed = data.speed;
        int damage = data.damage;
        if (energy) {
            if ("L".equals(f.specialStrength)) {
                speed *= 0.65f;
                damage = Math.round(damage * 0.60f);
            } else if ("H".equals(f.specialStrength)) {
                speed *= 1.35f;
                damage = Math.round(damage * 1.45f);
            }
        }
        float radius = superKind ? 58f : 24f;
        float range = data.range;
        if (energy && f.charge > 0) {
            // Charged shot: bigger, faster, further and harder, up to the full charge.
            AttackDefinition.Mechanics m = f.attack.mechanics;
            float full = Math.min(1f, f.charge / (float)Math.max(1, m.chargeFrames));
            damage = damage * (1000 + Math.round(m.chargeBonusPermille * full)) / 1000;
            speed *= 1f + 0.3f * full;
            radius *= 1f + 0.8f * full;
            range *= 1f + 0.5f * full;
            if (f.charged()) cues.add("CHARGED");
        }
        float height = superKind ? data.spawnHeight(false, !f.grounded) : data.spawnHeight(f.crouching, !f.grounded);
        // Projétil baixo (sombra): corre colado no chão, mesmo lançado do alto.
        float base = !superKind && f.attack.low ? Arena.GROUND_Y : f.y;
        Projectile p = new Projectile(
            f.x + f.facing * data.spawnX, base - height, range, speed,
            radius, damage, profile.color, f.index, f.facing, f.attack,
            energy ? profile.energyVisual : data.visual);
        p.vy = data.vertical;
        p.totalHits = data.hits;
        p.hitInterval = data.hitInterval;
        if (data.puddleFrames > 0) p.puddle = data;
        (superKind ? superProjectiles : energyProjectiles).add(p);
    }

    private void advanceProjectiles() {
        if (superFreeze > 0) {
            superFreeze--;
            return;
        }
        moveProjectiles(energyProjectiles, 120f);
        moveProjectiles(superProjectiles, 180f);
        clashProjectiles();
        advancePuddles();
    }

    /**
     * Projéteis de lados opostos que se tocam se anulam antes de chegar em alguém. A força de
     * cada um são os acertos que ainda faltam: no choque cada um perde a força do outro e some
     * quando ela acaba (iguais somem os dois; um de 3 acertos contra um de 1 segue com 2). O Super
     * não perde força contra especial: atravessa e segue inteiro. O chicote da p08 não entra (é o
     * braço, não energia).
     */
    private void clashProjectiles() {
        boolean any = false;
        int energyCount = energyProjectiles.size();
        int total = energyCount + superProjectiles.size();
        for (int i = 0; i < total; i++) {
            Projectile a = i < energyCount ? energyProjectiles.get(i) : superProjectiles.get(i - energyCount);
            for (int j = i + 1; j < total && a.clashes(); j++) {
                Projectile b = j < energyCount ? energyProjectiles.get(j) : superProjectiles.get(j - energyCount);
                if (b.ownerIndex == a.ownerIndex || !b.clashes() || !CombatRules.projectilesMeet(
                    a.previousX, a.x, a.y, a.radius, b.previousX, b.x, b.y, b.radius)) continue;
                int strengthA = a.durability(), strengthB = b.durability();
                boolean superA = a.superKind(), superB = b.superKind();
                if (!(superA && !superB)) a.hitsDone += strengthB;
                if (!(superB && !superA)) b.hitsDone += strengthA;
                a.cancelled = a.durability() == 0;
                b.cancelled = b.durability() == 0;
                // O choque fica onde os dois se tocaram, na borda de cada um.
                float x = (a.x * b.radius + b.x * a.radius) / (a.radius + b.radius);
                float y = (a.y + b.y) * .5f;
                Projectile first = a.ownerIndex == 0 ? a : b, second = first == a ? b : a;
                clashes.add(new ProjectileClash(x, y, superA || superB, first.color, second.color, first.direction));
                any = true;
            }
        }
        if (!any) return;
        cues.add("PROJECTILE_CLASH");
        removeCancelled(energyProjectiles);
        removeCancelled(superProjectiles);
    }

    private static void removeCancelled(List<Projectile> projectiles) {
        Iterator<Projectile> it = projectiles.iterator();
        while (it.hasNext()) if (it.next().cancelled) it.remove();
    }

    /** Quem pisa numa poça sem estar envenenado fica envenenado; a poça some com o tempo. */
    private void advancePuddles() {
        Iterator<Puddle> it = puddles.iterator();
        while (it.hasNext()) {
            Puddle pd = it.next();
            if (--pd.framesLeft <= 0) {
                it.remove();
                continue;
            }
            CombatFighter d = fighters[1 - pd.ownerIndex];
            if (d.grounded && d.hittable() && !d.state.poisoned() &&
                Math.abs(d.x - pd.x) <= pd.halfWidth + d.body().halfWidth) {
                d.state.poison(pd.poisonDamage, pd.poisonFrames);
                cues.add("POISON");
            }
        }
    }

    /** O projétil que cai (seringa) chega ao chão: vira poça (uma por dono) ou some. */
    private void land(Projectile p) {
        if (p.puddle == null) return;
        for (Iterator<Puddle> it = puddles.iterator(); it.hasNext();) {
            if (it.next().ownerIndex == p.ownerIndex) it.remove();
        }
        CharacterDefinition.Projectile d = p.puddle;
        puddles.add(new Puddle(p.x, d.puddleHalfWidth, p.ownerIndex, p.color, d.puddleFrames,
            d.puddleDamage, d.puddlePoisonFrames));
    }

    /** Velocidade da volta do chicote, em vezes a da ida. */
    private static final float RETRACT_SPEED = 1.6f;

    private void moveProjectiles(List<Projectile> projectiles, float margin) {
        Iterator<Projectile> it = projectiles.iterator();
        while (it.hasNext()) {
            Projectile p = it.next();
            p.previousX = p.x;
            if (p.holding) continue;   // parado no rival entre um acerto e o próximo
            if (p.returning) {
                // O chicote recolhe mais rápido do que estica e some ao chegar na mão.
                p.x -= RETRACT_SPEED * p.speed * p.direction * CombatConfig.DT;
                if ((p.x - p.startX) * p.direction <= 0f) it.remove();
                continue;
            }
            p.x += p.speed * p.direction * CombatConfig.DT;
            if (p.vy != 0f) {
                p.y += p.vy * CombatConfig.DT;
                if (p.y >= Arena.GROUND_Y) {
                    p.y = Arena.GROUND_Y;
                    land(p);
                    it.remove();
                    continue;
                }
                if (p.y < Arena.GROUND_Y - 1200f) {
                    it.remove();
                    continue;
                }
            }
            if (Math.abs(p.x - p.startX) >= p.range && p.retracts()) {
                p.x = p.startX + p.direction * p.range;
                p.returning = true;
            } else if (Math.abs(p.x - p.startX) >= p.range ||
                p.x > Arena.RIGHT_BOUND + margin || p.x < Arena.LEFT_BOUND - margin) {
                it.remove();
            }
        }
    }

    /** Whether {@code owner} still has an energy projectile on screen (one at a time). */
    boolean energyProjectileAlive(int owner) {
        return hasProjectile(energyProjectiles, owner);
    }

    private static boolean hasProjectile(List<Projectile> projectiles, int owner) {
        for (Projectile p : projectiles) if (p.ownerIndex == owner) return true;
        return false;
    }

    // ---------------------------------------------------------------- collisions

    private void detectMelee(CombatFighter a, CombatFighter d) {
        if (a.frozen() || !a.attacking() || !a.attack.melee()) return;
        int frame = hitFrame(a);
        if (!a.attack.isActive(frame) || a.hitsOnTarget >= a.attack.maxHits || !d.hittable()) return;
        if (evades(d, a.attack, !a.grounded, false)) return;
        CharacterDefinition.Body body = d.body();
        boolean crouch = d.crouchingBody() && !hitsCrouching(a.attack, a);
        for (int i = 0; i < a.attack.hitboxCount(); i++) {
            AttackDefinition.Box box = a.attack.hitbox(i);
            if (!box.activeAt(frame) || (a.hitboxMask & (1L << i)) != 0) continue;
            if (CombatRules.hitboxTouches(a.x, a.y, a.facing, box, d.x, d.y, body, crouch) ||
                touchesMoveHurtbox(a, box, d)) {
                addContact(a, d, a.attack, i, null);
                return;
            }
        }
    }

    /**
     * Especial e Super de chão (golpe ou projétil) pegam quem está agachado: contra eles o corpo
     * agachado conta com a altura de em pé (o desenho de muitos especiais e a energia passam
     * na altura do peito e iam por cima de quem abaixava). Os especiais que só saem no ar
     * seguem a altura de verdade.
     */
    private static boolean hitsCrouching(AttackDefinition attack, CombatFighter owner) {
        if (attack == null) return false;
        if (attack.strength != AttackDefinition.Strength.SPECIAL && attack.strength != AttackDefinition.Strength.SUPER) {
            return false;
        }
        CharacterDefinition.Special special = owner.character().specials.get(attack.id);
        return special == null || !special.air;
    }

    /** Quadro que vale para o acerto: no mergulho, o golpe bate a descida inteira até o chão. */
    private static int hitFrame(CombatFighter a) {
        if (a.attack.mechanics.dives() && !a.grounded && a.attackFrame >= a.attack.startupFrames) {
            return Math.min(a.attackFrame, a.attack.lastActiveFrame());
        }
        return a.attackFrame;
    }

    /**
     * The defender's move slips this strike: a dodge under standing/air strikes and
     * projectiles (HIGH), or a vanish that nothing touches (ANY). Low strikes and low projectiles still hit a HIGH dodge.
     */
    private static boolean evades(CombatFighter d, AttackDefinition attack, boolean airborneAttacker, boolean projectile) {
        AttackDefinition.Mechanics m = d.mechanics();
        if (!m.evadeAt(d.attackFrame)) return false;
        if (m.evade == AttackDefinition.Reads.ANY) return true;
        if (projectile) return !attack.low;
        return airborneAttacker || !attack.low;
    }

    /** The counter stance reads this melee strike (high = standing and air strikes, low = low strikes). */
    private static boolean reads(AttackDefinition.Reads reads, AttackDefinition attack, boolean airborneAttacker) {
        switch (reads) {
            case ANY: return true;
            case HIGH: return airborneAttacker || !attack.low;
            case LOW: return attack.low && !airborneAttacker;
            default: return false;
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
            if (p.returning) continue;
            if (p.hitCooldown > 0) {
                p.hitCooldown--;
                continue;
            }
            CombatFighter d = fighters[1 - p.ownerIndex];
            // An evading body lets the projectile fly on.
            boolean hits = d.hittable() && !evades(d, p.attack, false, true) &&
                CombatRules.projectileHits(p.previousX, p.x, p.y, p.radius, d.x, d.y, d.body(),
                    d.crouchingBody() && !hitsCrouching(p.attack, fighters[p.ownerIndex]));
            if (!hits) {
                p.holding = false;   // o rival saiu de baixo: a onda segue voando com os acertos que sobraram
                continue;
            }
            addContact(fighters[p.ownerIndex], d, p.attack, -1, p);
            contacts.get(contacts.size() - 1).strike = p.hitsDone;
            if (++p.hitsDone < p.totalHits) {
                p.holding = true;
                p.hitCooldown = p.hitInterval;
            } else if (p.retracts()) {
                p.returning = true;
            } else {
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
        f.state.spendSuperMeter(config.pushblockCost);
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
        point.state.spendSuperMeter(config.guardCancelCost);
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
            // Tech: só o L + M apertado há pouco (o buffer normal), não o pedido longo de agarrão.
            if (d.throwRequestAge >= 0 && d.throwRequestAge <= config.bufferFrames) {
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
        return ahead >= 0f && gap <= config.throwRange + a.state.profile.throwRangeBonus;
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
        // A grappler throws harder than the engine's standard throw.
        int own = a.state.profile.throwDamage;
        int base = (own > 0 ? own : throwAttack.damage) * a.state.profile.furyScale(a.state.life) / 1000;
        int damage = session.registerHit(throwAttack, base, config);
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
            block(a, d, ultraAttack, guard, a.facing, false);
            a.ultraPhase = CombatFighter.ULTRA_RECOVERY;
            a.ultraFrame = 0;
            return;
        }

        ComboSession session = sessionFor(a, d);
        a.ultraScale = session.scaleFor(ultraAttack, config);
        CharacterDefinition.UltraSequence sequence = a.state.profile.ultraSequence;
        a.ultraPhase = sequence != null ? CombatFighter.ULTRA_SEQUENCE : CombatFighter.ULTRA_CINEMATIC;
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
        d.stunProjectile = null;
        d.hitCrouching = wasCrouching;
        d.stunLeft = d.stunTotal = ultraAttack.hitstunFrames;
        d.stunElapsed = 0;
        if (sequence != null) {
            // Sem cinemática: a sequência do vídeo prende o rival onde encostou.
            a.ultraHitsDone = 0;
            a.ultraPaused = false;
            a.ultraPageDone = false;
            a.ultraFinalBonus = 0f;
            a.ultraVictim = d;
            a.ultraVictimState = d.state;
            float bodies = a.body().halfWidth + d.body().halfWidth;
            a.ultraHoldGap = Math.max(bodies, Math.min((d.x - a.x) * a.facing, bodies + config.ultraReach));
            a.ultraHoldY = d.y;
            d.stunLeft = d.stunTotal = sequence.length + ultraAttack.hitstunFrames;
            return;
        }
        ultraConnected = a.index;
    }

    /**
     * Ultra em sequência: o rival fica preso na frente (ou onde a pose o segura, no agarrão),
     * cada golpe acerta no quadro da animação com uma parte do dano, o último lança, e o
     * atacante termina a animação antes de voltar.
     */
    private void updateUltraSequence(CombatFighter a, CombatFighter d) {
        if (!a.inUltraSequence()) return;
        CharacterDefinition.UltraSequence seq = a.state.profile.ultraSequence;
        boolean sameVictim = d == a.ultraVictim && d.state == a.ultraVictimState;
        if (a.ultraHitsDone < seq.hits.length && !sameVictim) {
            // Outro entrou no lugar: o resto da sequência é só a animação.
            a.ultraHitsDone = seq.hits.length;
        }
        // Zerado no meio do Ultra, ele continua preso e levando os golpes (sem dano) até o golpe
        // final, que é o que joga longe: voando antes, o golpe final e a finalização de mangá
        // aconteciam no vento.
        if (a.ultraHitsDone < seq.hits.length) holdUltraVictim(a, d, seq);
        int finalHit = seq.hits.length - 1;
        if (config.ultraSequencePage && !a.ultraPageDone && a.ultraHitsDone == finalHit &&
            a.ultraFrame >= seq.hits[finalHit] - config.ultraPageLeadFrames) {
            // Antes do golpe final: a página de mangá entra (a tela segura a luta enquanto ela roda).
            a.ultraPageDone = true;
            a.ultraPaused = true;
            ultraConnected = a.index;
        }
        if (a.ultraPaused) {
            d.stunLeft = Math.max(d.stunLeft, seq.length - a.ultraFrame + ultraAttack.hitstunFrames);
            return;
        }
        while (a.ultraHitsDone < seq.hits.length && a.ultraFrame >= seq.hits[a.ultraHitsDone]) {
            int i = a.ultraHitsDone++;
            int damage = ComboSession.DamageScaling.apply(seq.hitDamage(i), a.ultraScale);
            if (i == finalHit) damage = Math.round(damage * (1f + a.ultraFinalBonus));
            int before = events.size();
            dealUltraDamage(a, d, damage);
            for (int e = before; e < events.size(); e++) {
                HitEvent event = events.get(e);
                event.impactX = d.x - a.facing * d.body().halfWidth * 0.5f;
                event.impactY = d.y - d.body().height(false) * 0.6f;
                event.strength = i == seq.hits.length - 1 ? AttackDefinition.Strength.HEAVY : AttackDefinition.Strength.MEDIUM;
            }
            boolean last = i == seq.hits.length - 1;
            int stop = last ? config.ultraSequenceFinalHitstop : config.ultraSequenceHitstop;
            a.hitstop = Math.max(a.hitstop, stop);
            d.hitstop = Math.max(d.hitstop, stop);
            if (last) {
                launchUltraVictim(a, d);
                cues.add("ULTRA_FINISH");
            }
        }
        if (a.ultraFrame >= seq.length) {
            a.status = CombatFighter.Status.NEUTRAL;
            a.ultraFrame = 0;
        }
    }

    /** Segura o rival na pose do Ultra; contra a parede quem recua é o atacante. */
    private void holdUltraVictim(CombatFighter a, CombatFighter d, CharacterDefinition.UltraSequence seq) {
        float[] pose = seq.holdAt(a.ultraFrame);
        float gap = pose != null ? pose[0] : a.ultraHoldGap;
        float wanted = a.x + a.facing * gap;
        d.x = Arena.clamp(wanted, Arena.LEFT_BOUND, Arena.RIGHT_BOUND);
        if (d.x != wanted) a.x = Arena.clamp(d.x - a.facing * gap, Arena.LEFT_BOUND, Arena.RIGHT_BOUND);
        d.y = pose != null ? Arena.GROUND_Y - pose[1] : a.ultraHoldY;
        d.grounded = d.y >= Arena.GROUND_Y;
        d.vx = 0f;
        d.vy = 0f;
        d.pushRemaining = 0f;
        d.pushFramesLeft = 0;
        d.stunLeft = Math.max(d.stunLeft, 2);
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

    /**
     * Fim da página de mangá de um Ultra em sequência: a animação volta a andar e o último
     * golpe sai com o bônus da nota do toque ({@code bonus}: 0, 0,10 ou 0,25).
     */
    void resumeUltraSequence(int attacker, float bonus) {
        CombatFighter a = fighters[attacker];
        if (!a.inUltraSequence() || !a.ultraPaused) return;
        a.ultraPaused = false;
        a.ultraFinalBonus = Math.max(0f, bonus);
    }

    /** Ultra em sequência parado esperando a página de mangá. */
    boolean ultraWaitingPage(int attacker) {
        CombatFighter a = fighters[attacker];
        return a.inUltraSequence() && a.ultraPaused;
    }

    /** Damage already scaled; one more hit on the combo counter. */
    private int dealUltraDamage(CombatFighter a, CombatFighter d, int damage) {
        if (d.ko()) {
            // Já zerado e ainda preso no Ultra: o golpe acerta (faísca, som), mas não tira nada.
            events.add(new HitEvent(a.index, d.index, 0, false, false, ultraAttack.id));
            return 0;
        }
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
        d.stunProjectile = null;
        d.vy = 0f;
        int untilBlast = config.ultraBeamBlastFrame(a.beamHits) - a.ultraFrame;
        d.stunLeft = d.stunTotal = Math.max(d.stunLeft, untilBlast + config.ultraBeamHitInterval * 2);
        d.stunElapsed = 0;
    }

    /** The defender flies away and lands knocked down. */
    private void launchUltraVictim(CombatFighter a, CombatFighter d) {
        d.status = CombatFighter.Status.AIR_HITSTUN;
        d.stunProjectile = null;
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

    /** Presentation contact point: the hitbox front clamped into the defender's body. */
    private static float impactX(Contact c) {
        CombatFighter a = c.attacker, d = c.defender;
        float half = d.body().halfWidth;
        if (c.projectile != null) return d.x - c.projectile.direction * half * .6f;
        AttackDefinition.Box box = c.attack.hitbox(c.hitbox);
        float front = a.facing > 0 ? a.x + box.x1 : a.x - box.x1;
        return Math.max(d.x - half, Math.min(d.x + half, front));
    }

    private static float impactY(Contact c) {
        CombatFighter a = c.attacker, d = c.defender;
        float top = d.y - d.body().height(d.crouchingBody()), bottom = d.y;
        float y = c.projectile != null ? c.projectile.y
            : a.y - (c.attack.hitbox(c.hitbox).y0 * .3f + c.attack.hitbox(c.hitbox).y1 * .7f);
        return Math.max(top + 12f, Math.min(bottom - 12f, y));
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
        boolean ownerMove = !c.echo && (!projectile || (a.attacking() && a.attack == attack));
        ComboSession session = sessions[d.index];

        // Several strikes (the animation hits more than once): the damage is split between them,
        // the ones before the last only hold the defender until the next, the last one lands
        // the move's effect (knockdown, launch, knockback).
        // O projétil de vários acertos faz o mesmo, contando os acertos dele.
        int projectileHits = projectile ? c.projectile.totalHits : 1;
        boolean multiProjectile = projectileHits > 1;
        boolean multi = !projectile && attack.maxHits > 1;
        // A trade ends both moves: each strike then lands as the last one, on both sides alike
        // (whichever contact is applied first).
        boolean stillStriking = a.attacking() && a.attack == attack && !trades(c);
        boolean lastStrike = multiProjectile ? c.strike >= projectileHits - 1
            : !multi || !stillStriking || c.hitbox >= attack.hitboxCount() - 1;
        int holdUntil = lastStrike ? 0 : multiProjectile
            ? c.projectile.hitInterval + attack.hitstopFrames + config.strikeHoldFrames
            : attack.hitbox(c.hitbox + 1).firstFrame - a.attackFrame + attack.hitstopFrames + config.strikeHoldFrames;

        if (d.status == CombatFighter.Status.AIR_HITSTUN && session != null && (!multi || !a.strikeLanded) &&
            (long)session.juggleCount + attack.juggleCost > config.juggleLimit) {
            // The rest of a several-strike move cannot catch the freed defender either.
            if (multi) a.hitsOnTarget = attack.maxHits;
            // Juggle limit: the defender recovers in the air instead of taking the hit.
            d.status = CombatFighter.Status.NEUTRAL;
            d.stunLeft = 0;
            d.launched = false;
            d.slammed = false;
            d.ultraFall = false;
            d.hardFall = false;
            d.spinning = false;
            d.bounce = CombatFighter.BOUNCE_NONE;
            d.vx = 0f;
            return;
        }

        if (comboBreaks(a, d, attack, projectile, c.echo, lastStrike)) {
            comboBreaker(a, d);
            return;
        }

        AttackDefinition.Mechanics stance = d.mechanics();
        if (!projectile && stance.counterAt(d.attackFrame) && reads(stance.counter, attack, !a.grounded)) {
            counter(d, a);
            return;
        }

        // Dano desta batida (o golpe de várias batidas divide o dano entre elas).
        int base = projectile ? c.projectile.damage : attack.damage;
        if (multi || multiProjectile) {
            int count = multi ? attack.maxHits : projectileHits;
            int each = base / count;
            base = lastStrike ? base - each * (count - 1) : each;
        }

        int guard = effectiveGuard(d);
        if (energyChain(c, d)) {
            // Bola em cima de bola não emenda: atordoado pela bola de energia de quem atirou,
            // a próxima bola dele chega como se ele já tivesse se recuperado e defendido (sem
            // arranhão nem gasto de guarda: ele não escolheu defender).
            d.status = CombatFighter.Status.BLOCKSTUN;
            d.clearAttack();
            d.stunLeft = d.stunTotal = attack.blockstunFrames;
            d.stunElapsed = 0;
            d.lastGuard = d.crouchingBody() ? CombatFighter.GUARD_LOW : CombatFighter.GUARD_HIGH;
            d.framesSinceBlock = 0;
            d.stunProjectile = null;
            push(d, a, direction * attack.pushbackOnBlock);
            freeze(a, d, attack.hitstopFrames, projectile);
            events.add(new HitEvent(a.index, d.index, 0, true, projectile, attack.id));
            if (ownerMove && a.outcome == CombatFighter.Outcome.NONE) a.outcome = CombatFighter.Outcome.BLOCK;
            return;
        }
        if (guardStops(guard, attack, !a.grounded, projectile)) {
            block(a, d, attack, guard, direction, projectile, !lastStrike,
                multiProjectile ? c.strike == 0 : !multi || !a.strikeBlocked, chipFor(attack, base));
            if (multi) a.strikeBlocked = true;
            if (!lastStrike) d.stunLeft = d.stunTotal = Math.max(d.stunLeft, holdUntil);
            if (ownerMove && a.outcome == CombatFighter.Outcome.NONE) a.outcome = CombatFighter.Outcome.BLOCK;
            return;
        }

        // Fury: the attacker hits harder the less life it has left.
        int raw = base * a.state.profile.furyScale(a.state.life) / 1000;
        if (stance.armorAt(d.attackFrame) && d.armorUsed < stance.armorHits) {
            // Super armor: the damage goes through, the stun does not; the move carries on.
            d.armorUsed++;
            d.state.takeDamage(raw, config.recoverableLifePermille);
            gainMeter(d, CombatRules.superGainOnGuard(attack.strength));
            freeze(a, d, attack.hitstopFrames, projectile);
            if (ownerMove && a.outcome == CombatFighter.Outcome.NONE) a.outcome = CombatFighter.Outcome.BLOCK;
            cues.add("ARMOR");
            events.add(new HitEvent(a.index, d.index, raw, false, projectile, attack.id));
            return;
        }

        boolean newSession = sessions[d.index] == null;
        session = sessionFor(a, d);
        // The move's first strike that hits pays meter and sets the combo scale; the later
        // strikes keep that scale (a new combo starts over as a first strike).
        boolean firstStrike = multiProjectile ? c.strike == 0 || newSession : !multi || !a.strikeLanded || newSession;
        if (multi) a.strikeLanded = true;
        int hitstun = session.decayedHitstun(attack.hitstunFrames, config);
        if (!lastStrike) hitstun = Math.max(hitstun, holdUntil);
        AttackDefinition.Launch launch = lastStrike ? attack.launch : AttackDefinition.Launch.NONE;
        float hitPush = lastStrike ? attack.pushbackOnHit : attack.pushbackOnHit * config.strikeHoldPushback;
        if (projectile && lastStrike && attack.kind != AttackDefinition.Kind.SUPER) {
            hitPush = Math.max(hitPush, config.projectileHitPushback);
        }
        float airPush = lastStrike ? attack.knockback : attack.knockback * config.strikeHoldPushback;
        // Later strikes of the same move keep the scale of its first strike.
        int damage = firstStrike ? session.registerHit(attack, raw, config) : session.registerFollowUp(raw);
        d.state.takeDamage(damage, config.recoverableLifePermille);
        if (firstStrike) gainMeter(a, CombatRules.superGainOnHit(attack.strength));
        if (attack.mechanics.poisonDamage > 0) {
            d.state.poison(attack.mechanics.poisonDamage, attack.mechanics.poisonFrames);
            cues.add("POISON");
        }

        boolean wasCrouching = d.crouchingBody();
        boolean wasGrounded = d.grounded;
        // Punish counter: a standing heavy normal caught the defender in its own move.
        boolean punish = lastStrike && !projectile && a.grounded && d.grounded && d.attacking() &&
            attack.kind == AttackDefinition.Kind.NORMAL && attack.strength == AttackDefinition.Strength.HEAVY &&
            attack.launch != AttackDefinition.Launch.LAUNCH;
        d.clearAttack();
        d.forwardDashing = false;
        d.backdashFrames = 0;
        d.framesSinceHit = 0;
        d.stunLeft = d.stunTotal = hitstun;
        d.stunElapsed = 0;
        d.stunProjectile = projectile && attack.kind != AttackDefinition.Kind.SUPER && !c.echo ? c.projectile : null;
        // A new hit takes over any flight in progress (and any free fall after the hitstun).
        d.freeFallFrames = 0;
        d.vx = 0f;
        d.bounce = CombatFighter.BOUNCE_NONE;
        d.spinning = false;
        // The wall bounce is a combo tool: a lone hit in neutral is a plain hit.
        boolean wallBounce = launch == AttackDefinition.Launch.WALL_BOUNCE && session.hitCount >= 2 &&
            session.wallBounces < config.maxWallBounces;
        boolean groundBounce = launch == AttackDefinition.Launch.GROUND_BOUNCE && !d.grounded &&
            session.groundBounces < config.maxGroundBounces;

        boolean spinHit = attack.spin && lastStrike && !projectile;
        if (punish || spinHit) {
            // Spinning knockdown: flies away in a medium arc, still open to a juggle, and
            // lands lying down (no air tech). Also the tag's flying kick (VoadoraTroca).
            d.status = CombatFighter.Status.AIR_HITSTUN;
            d.grounded = false;
            d.y -= 2f;
            d.vx = direction * config.spinSpeed;
            d.vy = -config.spinLift;
            d.spinning = true;
            d.launched = true;
            d.slammed = false;
            d.superJumping = false;
            d.hardFall = true;
            d.stunLeft = d.stunTotal = Math.max(hitstun, config.spinHitstunFrames);
            d.pushRemaining = 0f;
            d.pushFramesLeft = 0;
            cues.add(punish ? "PUNISH" : "SPIN");
        } else if (wallBounce) {
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
            a.wallChase = config.wallChaseFrames;
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
        } else if ((launch == AttackDefinition.Launch.SLAM ||
                    launch == AttackDefinition.Launch.GROUND_BOUNCE) && !d.grounded && a.superJumping) {
            // Slam (also a ground bounce already used): from a super jump, a forced dive.
            d.status = CombatFighter.Status.AIR_HITSTUN;
            d.slammed = true;
            d.launched = false;
            d.vy = config.slamSpeed;
            push(d, a, direction * attack.knockback * 0.35f);
        } else if (launch == AttackDefinition.Launch.KNOCKDOWN && d.grounded) {
            knockDown(d);
            push(d, a, direction * attack.knockback);
        } else if (launch == AttackDefinition.Launch.LAUNCH) {
            d.status = CombatFighter.Status.AIR_HITSTUN;
            // Pego no ar (ex.: voltando da parede), sobe só até a altura de um lançamento do
            // chão: senão sai na frente e o super pulo de quem lançou não alcança.
            float altura = Math.max(0f, Arena.GROUND_Y - d.y);
            d.grounded = false;
            d.y -= 2f;
            d.vy = -(float) Math.sqrt(Math.max(0f,
                config.launchSpeed * config.launchSpeed - 2f * config.gravity * altura));
            d.launched = true;
            d.slammed = false;
            d.superJumping = false;
            a.launcherChase = config.launcherChaseFrames;
            push(d, a, direction * attack.knockback);
        } else if (!d.grounded) {
            d.status = CombatFighter.Status.AIR_HITSTUN;
            push(d, a, direction * airPush);
            // Held in the air between strikes: a small pop keeps it in reach of the next one.
            if (!lastStrike) d.vy = Math.min(d.vy, -config.strikeHoldLift);
        } else {
            d.status = CombatFighter.Status.HITSTUN;
            d.hitCrouching = wasCrouching;
            push(d, a, direction * hitPush);
        }
        if (attack.mechanics.pull > 0f && d.grounded && !projectile) {
            // Pull: the defender is dragged to the attacker instead of pushed away.
            float gap = Math.abs(d.x - a.x) - a.body().halfWidth - d.body().halfWidth - config.teleportGap;
            push(d, a, -direction * Math.min(attack.mechanics.pull, Math.max(0f, gap)));
            cues.add("PULL");
        }
        // Every hit that leaves the defender juggled costs juggle points (a several-strike move once).
        // The H that sends a grounded defender to the wall is free: it hit on the ground, and the
        // launcher after the bounce plus a full air string (jL > jM > jH) still fit the limit.
        boolean groundWallBounce = wallBounce && wasGrounded;
        if (d.status == CombatFighter.Status.AIR_HITSTUN && !groundWallBounce && (!multi || !a.strikeJuggled)) {
            session.juggleCount += attack.juggleCost;
            a.strikeJuggled = true;
        }
        if (!d.grounded) session.airCombo = true;
        int stop = lastStrike ? attack.hitstopFrames : holdingHitstop(attack);
        if (lastStrike && !projectile) stop += hitHold(attack);
        if (punish) stop += config.punishHitstopBonus;
        if (d.ko()) stop += config.koHitstopBonus;
        freeze(a, d, stop, projectile);
        if (ownerMove) a.outcome = CombatFighter.Outcome.HIT;
        events.add(new HitEvent(a.index, d.index, damage, false, projectile, attack.id));
    }

    /**
     * Counter: the stance read the strike. Nothing reaches the defender; its move jumps to
     * the strike, which lands at once on the attacker (poison included when the move has it).
     */
    private void counter(CombatFighter d, CombatFighter a) {
        cues.add("COUNTER");
        d.attackFrame = d.attack.startupFrames;
        Contact answer = new Contact();
        answer.attacker = d;
        answer.defender = a;
        answer.attack = d.attack;
        answer.hitbox = 0;
        applyContact(answer);
    }

    /** The defender's own melee strike reaches the attacker in this same step (a trade). */
    private boolean trades(Contact c) {
        for (Contact other : contacts) {
            if (other != c && other.projectile == null && other.attacker == c.defender && other.defender == c.attacker) {
                return true;
            }
        }
        return false;
    }

    /**
     * Bola de energia de quem atirou chegando em quem ainda está atordoado por outra bola dele
     * (não a mesma, que pode ter vários acertos): não emenda. De perto o acerto da bola deixava
     * tempo de soltar outra, e bola atrás de bola era um combo sem fim.
     */
    private static boolean energyChain(Contact c, CombatFighter d) {
        if (c.projectile == null || c.echo || c.attack.kind == AttackDefinition.Kind.SUPER ||
            d.status != CombatFighter.Status.HITSTUN || !d.grounded) {
            return false;
        }
        Projectile before = d.stunProjectile;
        return before != null && before != c.projectile && before.ownerIndex == c.projectile.ownerIndex;
    }

    /** Hitstop of a strike before the last: shorter, the move keeps its rhythm. */
    private static int holdingHitstop(AttackDefinition attack) {
        return Math.max(3, attack.hitstopFrames * 2 / 3);
    }

    /** Guarded contact: blockstun, pushback, guard meter and hitstop. */
    private void block(CombatFighter a, CombatFighter d, AttackDefinition attack, int guard, int direction,
                       boolean projectile) {
        block(a, d, attack, guard, direction, projectile, false, true, chipFor(attack, attack.damage));
    }

    /**
     * Guarded contact. {@code holding}: a strike before the last of a several-strike move (a
     * small push and a short hitstop, so the next strike still reaches); {@code meter}: the
     * guard meter of the move, given once per move; {@code chip}: o arranhão (vira vida vermelha).
     *
     * Defesa perfeita (trás apertado em cima da hora): sem gastar guarda nem chip, um pouco de
     * vida de volta, mais barra e o blockstun curto.
     */
    private void block(CombatFighter a, CombatFighter d, AttackDefinition attack, int guard, int direction,
                       boolean projectile, boolean holding, boolean meter, int chip) {
        boolean just = justDefense(d);
        d.status = CombatFighter.Status.BLOCKSTUN;
        d.clearAttack();
        d.stunLeft = d.stunTotal = just
            ? Math.max(1, attack.blockstunFrames * config.justDefenseBlockstunPermille / 1000)
            : attack.blockstunFrames;
        d.justDefended = just;
        d.stunElapsed = 0;
        d.lastGuard = guard;
        d.framesSinceBlock = 0;
        d.forwardDashing = false;
        d.backdashFrames = 0;
        push(d, a, direction * attack.pushbackOnBlock * (holding ? config.strikeHoldPushback : 1f));
        if (meter) gainMeter(d, CombatRules.superGainOnGuard(attack.strength));
        freeze(a, d, holding ? holdingHitstop(attack) : attack.hitstopFrames, projectile);
        events.add(new HitEvent(a.index, d.index, 0, true, projectile, attack.id));
        if (just) {
            // A janela vale uma vez: a próxima batida pede outro aperto.
            d.guardPressCounts = false;
            d.state.heal(d.state.profile.maxLife * config.justDefenseHealPermille / 1000);
            gainMeter(d, CombatRules.superGainOnGuard(attack.strength));
            cues.add("JUST_DEFENSE");
            return;
        }
        d.state.chip(chip);
        int drain = attack.blockstunFrames * config.guardDrainPerBlockstunFrame;
        if (holding) drain = drain * config.guardHoldingDrainPermille / 1000;
        d.state.guard = Math.max(0, d.state.guard - drain);
        if (d.state.guard == 0) guardBreak(a, d, projectile);
    }

    /**
     * Guard break: a defesa que zerou a guarda quebra. Quem defendia fica aberto (um hitstun
     * longo, sem poder defender nem agir; no ar, cai e termina no chão), sem o empurrão da
     * defesa (fica ao alcance), e a guarda volta cheia.
     * O golpe que quebrou não tira vida: o castigo é o combo que vem depois.
     */
    private void guardBreak(CombatFighter a, CombatFighter d, boolean projectile) {
        d.state.guard = CombatConfig.GUARD_MAX;
        d.status = d.grounded ? CombatFighter.Status.HITSTUN : CombatFighter.Status.AIR_HITSTUN;
        d.stunProjectile = null;
        d.hitCrouching = false;
        d.stunLeft = d.stunTotal = config.guardBreakStunFrames;
        d.stunElapsed = 0;
        d.guardBroken = true;
        d.pushblockRequestAge = -1;
        // Sem o empurrão da defesa: fica colado em quem quebrou, ao alcance do castigo.
        d.pushRemaining = 0f;
        d.pushFramesLeft = 0;
        freeze(a, d, config.guardBreakHitstopFrames, projectile);
        cues.add("GUARD_BREAK");
    }

    /**
     * A guarda volta sozinha: de quem não está defendendo há {@code guardRegenDelayFrames}
     * (e de quem está fora de campo). Parada no congelamento do Super/Ultra.
     */
    private void regenerateGuard() {
        if (superFreeze > 0) return;
        for (TeamSystem.Side side : teams.sides) {
            CombatFighter point = fighters[side.index];
            for (FighterState m : side.members) {
                if (m.guard >= CombatConfig.GUARD_MAX || m.life <= 0) continue;
                if (m == point.state && (point.status == CombatFighter.Status.BLOCKSTUN ||
                    point.framesSinceBlock < config.guardRegenDelayFrames)) continue;
                m.guard = Math.min(CombatConfig.GUARD_MAX, m.guard + config.guardRegenPerFrame);
            }
        }
    }

    /** The defender's running combo, or a new one when it was able to act. */
    private ComboSession sessionFor(CombatFighter a, CombatFighter d) {
        ComboSession session = sessions[d.index];
        if (session == null || !d.inHitstun()) {
            endSession(d.index);
            session = new ComboSession(a.index, d.index);
            sessions[d.index] = session;
        }
        return session;
    }

    private void push(CombatFighter d, CombatFighter source, float distance) {
        d.pushRemaining = distance;
        d.pushFramesLeft = distance == 0f ? 0 : config.pushbackFrames;
        d.pushSource = source;
    }

    /** Hitstop: both bodies on a melee contact, only the defender for a projectile. */
    /** Congelamento extra que segura a pose do impacto nos acertos médios e fortes. */
    int hitHold(AttackDefinition attack) {
        if (attack.strength == AttackDefinition.Strength.MEDIUM) return config.mediumHitHoldFrames;
        if (attack.strength == AttackDefinition.Strength.HEAVY) return config.heavyHitHoldFrames;
        return 0;
    }

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
