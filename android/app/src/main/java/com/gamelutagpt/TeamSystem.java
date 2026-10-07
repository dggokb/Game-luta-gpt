package com.gamelutagpt;

/**
 * Team rules of both sides, owned and stepped by {@link CombatEngine}: who is on point, the
 * raw tag (↓ + TAG), the assist (TAG), Assist → Tag (TAG again while the assist is out) and
 * the team meter. Everything counts simulation frames, so the team is as deterministic as
 * the rest of the fight. The shell only renders what is here.
 *
 * <p>A side with a single member (the training dummy) never tags or calls assists.
 */
final class TeamSystem {
    static final int TAG_NONE = 0, TAG_EXIT = 1, TAG_ENTER = 2, TAG_POSE = 3;
    static final int ASSIST_NONE = 0, ASSIST_ENTER = 1, ASSIST_ATTACK = 2, ASSIST_LEAVE = 3;

    /** One side's team. */
    static final class Side {
        final int index;
        FighterState[] members;
        /** Member on point (playing in the fight slot). */
        int point;

        // Raw tag: the point runs off screen, the partner runs in and poses.
        int tagPhase = TAG_NONE;
        int tagFrame;
        /** Direction the point runs off to (behind it). */
        int tagDirection = -1;
        int tagCooldown;

        // Assist: the partner runs in behind the point, performs its move and leaves.
        int assistPhase = ASSIST_NONE;
        int assistFrame;
        int assistCooldown;
        /** The partner while it is on screen (its own body, attack and hitstop). */
        CombatFighter assist;
        float assistFromX, assistToX;
        /** TAG pressed again during the assist: it stays as the new point. */
        boolean convert;

        // Former point running off after an Assist → Tag (visual only, no hurtbox).
        FighterState leaving;
        float leavingX, leavingY;
        int leavingFacing;
        int leavingFrame = -1;

        int assistRequestAge = -1;
        int tagRequestAge = -1;

        // Overdrive: frames left (0 when off) and whether this round's one use is gone.
        int overdriveFrames;
        boolean overdriveUsed;

        Side(int index, FighterState state) {
            this.index = index;
            members = new FighterState[]{state};
        }

        FighterState pointState() { return members[point]; }

        /** The member waiting off screen, or null without a partner. */
        FighterState partner() { return members.length > 1 ? members[(point + 1) % members.length] : null; }

        boolean tagging() { return tagPhase != TAG_NONE; }

        boolean assistOut() { return assistPhase != ASSIST_NONE; }

        /** Assist → Tag can still be asked for (the assist has not finished its move). */
        boolean conversionOpen() {
            return (assistPhase == ASSIST_ENTER || assistPhase == ASSIST_ATTACK) && !convert;
        }
    }

    private final CombatConfig config;
    final Side[] sides;

    TeamSystem(CombatConfig config, FighterState first, FighterState second) {
        this.config = config;
        sides = new Side[]{new Side(0, first), new Side(1, second)};
    }

    /** The members of a side; the first one is on point. */
    void setMembers(int side, FighterState[] members) {
        if (members.length == 0) throw new IllegalArgumentException("a team needs at least one member");
        Side s = sides[side];
        s.members = members.clone();
        s.point = 0;
    }

    /** Demos: no tag, assist or cooldown in progress on either side (members stay). */
    void calm() {
        for (Side s : sides) {
            s.tagPhase = TAG_NONE;
            s.tagFrame = s.tagCooldown = 0;
            s.assistPhase = ASSIST_NONE;
            s.assistFrame = s.assistCooldown = 0;
            s.assist = null;
            s.convert = false;
            s.leaving = null;
            s.leavingFrame = -1;
            s.assistRequestAge = s.tagRequestAge = -1;
            s.overdriveFrames = 0;
        }
    }

    boolean overdriveActive(int side) { return sides[side].overdriveFrames > 0; }

    /** The side can turn its Overdrive on now: unused this round, free or cancelling its own attack. */
    boolean canOverdrive(CombatFighter f) {
        Side s = sides[f.index];
        if (s.overdriveUsed || s.overdriveFrames > 0 || s.tagging() || f.locked || f.ko() || f.frozen()) return false;
        return f.status == CombatFighter.Status.NEUTRAL ||
            (f.attacking() && f.attack.kind != AttackDefinition.Kind.SUPER);
    }

    /** New round for the side: its Overdrive can be used again. */
    void refreshOverdrive(int side) {
        sides[side].overdriveUsed = false;
        sides[side].overdriveFrames = 0;
    }

    /** Horizontal offset of the point's sprite during a raw tag (it runs off and back in). */
    float tagOffset(int side) {
        Side s = sides[side];
        if (s.tagPhase == TAG_EXIT) {
            float t = Math.min(1f, s.tagFrame / (float)config.tagExitFrames);
            return s.tagDirection * config.tagTravel * t * t;
        }
        if (s.tagPhase == TAG_ENTER) {
            float t = Math.min(1f, s.tagFrame / (float)config.tagEnterFrames);
            float eased = 1f - (1f - t) * (1f - t);
            return s.tagDirection * config.tagTravel * (1f - eased);
        }
        return 0f;
    }

    /** x of the former point running off after an Assist → Tag. */
    float leavingX(int side) {
        Side s = sides[side];
        float t = Math.min(1f, s.leavingFrame / (float)config.assistLeaveFrames);
        return s.leavingX - s.leavingFacing * config.assistEnterDistance * t * t;
    }

    // ---------------------------------------------------------------- input

    void readInput(CombatFighter f, FighterInput in) {
        Side s = sides[f.index];
        if (f.locked || f.ko() || s.partner() == null) {
            s.assistRequestAge = s.tagRequestAge = -1;
            return;
        }
        if (in.tag) s.tagRequestAge = 0;
        if (in.assist) s.assistRequestAge = 0;
    }

    private void ageRequests(Side s) {
        if (s.assistRequestAge >= 0 && ++s.assistRequestAge > config.bufferFrames) s.assistRequestAge = -1;
        if (s.tagRequestAge >= 0 && ++s.tagRequestAge > config.bufferFrames) s.tagRequestAge = -1;
    }

    // ---------------------------------------------------------------- start

    /** Starts what the requests ask for, when the rules allow it. */
    void resolve(CombatEngine engine, CombatFighter point, CombatFighter opponent) {
        Side s = sides[point.index];
        if (s.partner() == null) return;
        if (s.assistRequestAge >= 0 && point.status == CombatFighter.Status.BLOCKSTUN) {
            // TAG while blocking: Guard Cancel Tag, when the partner is ready and there is a bar.
            if (!point.frozen() && canGuardCancel(s) && engine.guardCancel(s, point)) {
                s.assistRequestAge = -1;
                s.assistCooldown = Math.max(s.assistCooldown, config.assistTagCooldownFrames);
                s.tagCooldown = Math.max(s.tagCooldown, config.assistTagCooldownFrames);
            }
        } else if (s.assistRequestAge >= 0 && inReleasedSuper(point)) {
            // TAG during your own Super, once it fired: DHC, the partner comes in with its Super.
            if (!point.frozen() && dhcReady(s, point) && engine.dhc(s, point)) {
                s.assistRequestAge = -1;
                s.assistCooldown = Math.max(s.assistCooldown, config.assistTagCooldownFrames);
                s.tagCooldown = Math.max(s.tagCooldown, config.assistTagCooldownFrames);
            }
        } else if (s.assistRequestAge >= 0) {
            if (s.conversionOpen()) {
                s.convert = true;
                s.assistRequestAge = -1;
            } else if (canAssist(s, point)) {
                startAssist(engine, s, point, opponent);
                s.assistRequestAge = -1;
            }
        }
        if (s.tagRequestAge >= 0 && canRawTag(s, point)) {
            s.tagPhase = TAG_EXIT;
            s.tagFrame = 0;
            s.tagDirection = -point.facing;
            s.tagRequestAge = -1;
            point.locked = true;
        }
    }

    boolean canAssist(int side, CombatFighter point) {
        return canAssist(sides[side], point);
    }

    private boolean canAssist(Side s, CombatFighter point) {
        if (s.partner() == null || s.assistOut() || s.assistCooldown > 0 || s.tagging() || s.leavingFrame >= 0) {
            return false;
        }
        if (point.locked || point.ko()) return false;
        // Called from neutral or during the point's own normals/specials (to extend a combo).
        return point.status == CombatFighter.Status.NEUTRAL ||
            (point.attacking() && point.attack.kind != AttackDefinition.Kind.SUPER);
    }

    /** The partner is free to come in (assist ready, no tag or assist going on). */
    private static boolean canGuardCancel(Side s) {
        return s.partner() != null && !s.tagging() && !s.assistOut() && s.assistCooldown == 0 && s.leavingFrame < 0;
    }

    /** TAG would Guard Cancel now (blocking on the ground with the partner ready and a bar). */
    boolean guardCancelReady(int side, CombatFighter point) {
        return canGuardCancel(sides[side]) && point.status == CombatFighter.Status.BLOCKSTUN && point.grounded &&
            point.state.superMeter >= config.guardCancelCost;
    }

    /** The point's own Super already released its attack (the DHC window). */
    private static boolean inReleasedSuper(CombatFighter point) {
        return point.attacking() && point.attack.kind == AttackDefinition.Kind.SUPER &&
            point.attackFrame >= point.attack.startupFrames;
    }

    private static boolean dhcReady(Side s, CombatFighter point) {
        FighterState partner = s.partner();
        return canGuardCancel(s) && partner.profile.hasSuperAttack() && partner.life > 0 &&
            point.state.superMeter >= CombatConfig.SUPER_COST;
    }

    /** TAG would DHC now (own Super released, partner ready with a Super, a bar left). */
    boolean dhcReady(int side, CombatFighter point) {
        Side s = sides[side];
        return s.partner() != null && inReleasedSuper(point) && dhcReady(s, point);
    }

    boolean canRawTag(int side, CombatFighter point) {
        return canRawTag(sides[side], point);
    }

    private boolean canRawTag(Side s, CombatFighter point) {
        return s.partner() != null && !s.tagging() && s.tagCooldown == 0 && !s.assistOut() &&
            s.leavingFrame < 0 && point.canAct();
    }

    private void startAssist(CombatEngine engine, Side s, CombatFighter point, CombatFighter opponent) {
        FighterState partner = s.partner();
        float to = Arena.clamp(point.x - point.facing * config.assistBehind, Arena.LEFT_BOUND, Arena.RIGHT_BOUND);
        int facing = opponent.x >= to ? 1 : -1;
        CombatFighter a = new CombatFighter(point.index, partner, to - facing * config.assistEnterDistance, facing);
        s.assist = a;
        s.assistFromX = a.x;
        s.assistToX = to;
        s.assistPhase = ASSIST_ENTER;
        s.assistFrame = 0;
        s.convert = false;
    }

    // ---------------------------------------------------------------- advance

    /** One frame of the side's team: tag phases, the assist's body and move, cooldowns. */
    void advance(CombatEngine engine, CombatFighter point) {
        Side s = sides[point.index];
        // A press during hitstop waits for it to end (the point cannot act yet).
        if (!point.frozen()) ageRequests(s);
        if (s.tagCooldown > 0) s.tagCooldown--;
        if (s.assistCooldown > 0 && !s.assistOut()) s.assistCooldown--;
        if (s.leavingFrame >= 0 && ++s.leavingFrame > config.assistLeaveFrames) {
            s.leavingFrame = -1;
            s.leaving = null;
        }
        advanceTag(engine, s, point);
        advanceAssist(engine, s, point);
        // The clock waits through freezes (its own flash, Supers).
        if (s.overdriveFrames > 0 && !engine.superFreezeActive()) s.overdriveFrames--;
        regenerate(s);
    }

    /**
     * Members waiting off screen get their recoverable life back, little by little. In
     * Overdrive every member does, the point too, and faster.
     */
    private void regenerate(Side s) {
        if (s.overdriveFrames > 0) {
            for (FighterState m : s.members) m.regenerate(config.overdriveRegenPerFrame);
            return;
        }
        if (s.members.length < 2) return;
        for (FighterState m : s.members) {
            boolean onScreen = m == s.pointState() || (s.assist != null && s.assist.state == m) ||
                (s.leavingFrame >= 0 && s.leaving == m);
            if (!onScreen) m.regenerate(config.recoverableLifePerFrame);
        }
    }

    private void advanceTag(CombatEngine engine, Side s, CombatFighter point) {
        if (s.tagPhase == TAG_NONE) return;
        s.tagFrame++;
        point.locked = true;
        if (s.tagPhase == TAG_EXIT && s.tagFrame >= config.tagExitFrames) {
            swapPoint(engine, s, point);
            s.tagPhase = TAG_ENTER;
            s.tagFrame = 0;
        } else if (s.tagPhase == TAG_ENTER && s.tagFrame >= config.tagEnterFrames) {
            s.tagPhase = TAG_POSE;
            s.tagFrame = 0;
        } else if (s.tagPhase == TAG_POSE && s.tagFrame >= config.tagPoseFrames) {
            s.tagPhase = TAG_NONE;
            s.tagFrame = 0;
            s.tagCooldown = config.tagCooldownFrames;
            point.locked = false;
        }
    }

    private void advanceAssist(CombatEngine engine, Side s, CombatFighter point) {
        CombatFighter a = s.assist;
        if (a == null) return;
        if (a.hitstop > 0) {
            a.hitstop--;
            return;
        }
        // The opponent's Super freeze stops the assist like the point.
        if (engine.superFreezeActive() && point.frozen()) return;
        s.assistFrame++;
        a.clock++;
        a.travel = 0f;
        if (s.assistPhase == ASSIST_ENTER) {
            float t = Math.min(1f, s.assistFrame / (float)config.assistEnterFrames);
            float x = s.assistFromX + (s.assistToX - s.assistFromX) * (1f - (1f - t) * (1f - t));
            a.travel = x - a.x;
            a.x = x;
            if (t >= 1f) {
                engine.startAssistMove(a);
                s.assistPhase = ASSIST_ATTACK;
                s.assistFrame = 0;
            }
        } else if (s.assistPhase == ASSIST_ATTACK) {
            a.attackFrame++;
            if (a.attack.kind != AttackDefinition.Kind.NORMAL && a.attackFrame == a.attack.startupFrames) {
                engine.spawnAssistProjectile(a);
            }
            if (a.attackFrame >= a.attack.totalFrames - 1) {
                if (s.convert && engine.convertAssist(s, point)) return;
                a.status = CombatFighter.Status.NEUTRAL;
                a.clearAttack();
                s.assistPhase = ASSIST_LEAVE;
                s.assistFrame = 0;
            }
        } else if (s.assistPhase == ASSIST_LEAVE) {
            float t = Math.min(1f, s.assistFrame / (float)config.assistLeaveFrames);
            float x = s.assistToX - a.facing * config.assistEnterDistance * t * t;
            a.travel = x - a.x;
            a.x = x;
            if (t >= 1f) {
                s.assist = null;
                s.assistPhase = ASSIST_NONE;
                s.assistFrame = 0;
                s.convert = false;
                s.assistCooldown = config.assistCooldownFrames;
            }
        }
    }

    /** Raw tag: the partner takes the slot (same place); the team meter goes with it. */
    private void swapPoint(CombatEngine engine, Side s, CombatFighter point) {
        FighterState outgoing = s.pointState();
        s.point = (s.point + 1) % s.members.length;
        engine.setFighterState(point.index, s.pointState());
        handMeter(outgoing, s.pointState());
    }

    /** The point runs off (visual only) and the partner takes the point; the bars go along. */
    void leavePoint(Side s, CombatFighter point) {
        s.leaving = s.pointState();
        s.leavingX = point.x;
        s.leavingY = point.y;
        s.leavingFacing = point.facing;
        s.leavingFrame = 0;
        s.point = (s.point + 1) % s.members.length;
        handMeter(s.leaving, s.pointState());
    }

    /** Assist → Tag, called by the engine once the assist's move ended. */
    void becomePoint(Side s, CombatFighter point) {
        CombatFighter a = s.assist;
        leavePoint(s, point);
        s.assist = null;
        s.assistPhase = ASSIST_NONE;
        s.assistFrame = 0;
        s.convert = false;
        s.assistCooldown = Math.max(s.assistCooldown, config.assistTagCooldownFrames);
        s.tagCooldown = Math.max(s.tagCooldown, config.assistTagCooldownFrames);
        if (a != null) a.travel = 0f;
    }

    // ---------------------------------------------------------------- meter

    /**
     * One meter per team: whatever a member off point earned (an assist's hits) joins the
     * point's bars, so the HUD and every cost read a single pool.
     */
    void poolMeter() {
        for (Side s : sides) {
            if (s.members.length < 2) continue;
            FighterState point = s.pointState();
            for (FighterState m : s.members) {
                if (m == point || m.superMeter == 0) continue;
                handMeter(m, point);
            }
        }
    }

    private static void handMeter(FighterState from, FighterState to) {
        if (from == to || from.superMeter == 0) return;
        to.addSuperMeter(from.superMeter);
        from.superMeter = 0;
        from.refreshHudLabels();
        to.refreshHudLabels();
    }
}
