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
    /** Quem entra vem numa voadora (VoadoraTroca), lutando de verdade até ela acabar. */
    static final int TAG_KICK = 4;
    static final int ASSIST_NONE = 0, ASSIST_ENTER = 1, ASSIST_ATTACK = 2, ASSIST_LEAVE = 3;
    /**
     * O golpe do assist acabou com a troca pedida, mas o titular ainda não pode sair (apanhando,
     * defendendo, caído): o parceiro fica parado esperando até {@link CombatConfig#assistTagBufferFrames}.
     */
    static final int ASSIST_WAIT = 4;

    /** One side's team. */
    static final class Side {
        final int index;
        FighterState[] members;
        /** Member on point (playing in the fight slot). */
        int point;

        // Raw tag: the point runs off screen, the partner runs in and poses.
        int tagPhase = TAG_NONE;
        /** A troca em andamento entra com a voadora (↓ + TAG); a da cinemática entra correndo. */
        boolean kickEntry;
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
        /** O assist entra pulando e bate de mergulho (o golpe dele só sai no ar). */
        boolean assistDive;
        /**
         * TAG no Super já solto: o DHC fica pedido e sai quando as pancadas do Super acabam (a
         * troca não corta o Super no meio).
         */
        boolean dhcPending;
        /** O DHC pedido é de um Super de capangas: vale até eles baterem, mesmo com o Super acabado. */
        boolean dhcHenchmen;
        /** TAG pressed again during the assist: it stays as the new point. */
        boolean convert;
        /**
         * O último Assist → Tag que não saiu e por quê ("APANHANDO", "DEFENDENDO", "CAÍDO"...), e o
         * quadro (do time) em que a troca pedida saiu ou caiu: o HUD avisa.
         */
        String convertFailReason;
        int convertResult;

        // Former point running off after an Assist → Tag (visual only, no hurtbox).
        FighterState leaving;
        float leavingX, leavingY;
        int leavingFacing;
        int leavingFrame = -1;

        int assistRequestAge = -1;
        int tagRequestAge = -1;

        Side(int index, FighterState state) {
            this.index = index;
            members = new FighterState[]{state};
        }

        FighterState pointState() { return members[point]; }

        /** The member waiting off screen, or null without a partner. */
        FighterState partner() { return members.length > 1 ? members[(point + 1) % members.length] : null; }

        /** The partner can come in (assist, tag): there is one and it still has life. */
        boolean partnerStanding() {
            FighterState p = partner();
            return p != null && p.life > 0;
        }

        boolean tagging() { return tagPhase != TAG_NONE; }
        /** A troca ainda no desenho (sai, entra correndo, posa); na voadora já é luta de verdade. */
        boolean tagAnimating() { return tagPhase != TAG_NONE && tagPhase != TAG_KICK; }

        boolean assistOut() { return assistPhase != ASSIST_NONE; }

        /**
         * Assist → Tag can still be asked for: while the assist comes in, attacks and runs off
         * (the run off counts: the press near the end of the assist is not lost).
         */
        boolean conversionOpen() {
            return (assistPhase == ASSIST_ENTER || assistPhase == ASSIST_ATTACK || assistPhase == ASSIST_LEAVE) && !convert;
        }

        /** TAG pressed during the assist and the swap has not happened yet. */
        boolean conversionPending() {
            return convert && assistPhase != ASSIST_NONE;
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
        calm(0);
        calm(1);
    }

    /** Um lado só, sem tag, assist ou espera em andamento (a dupla nova das ondas). */
    void calm(int side) {
        Side s = sides[side];
        s.tagPhase = TAG_NONE;
        s.tagFrame = s.tagCooldown = 0;
        s.assistPhase = ASSIST_NONE;
        s.assistFrame = s.assistCooldown = 0;
        s.assist = null;
        s.convert = false;
        s.leaving = null;
        s.leavingFrame = -1;
        s.assistRequestAge = s.tagRequestAge = -1;
        for (FighterState m : s.members) m.shinkaiFrames = 0;
    }

    /** O titular do lado está no SHINKAI. */
    boolean shinkaiActive(int side) { return sides[side].pointState().inShinkai(); }

    /**
     * O lutador pode ativar o SHINKAI agora: barra cheia e ainda não usado neste round, livre
     * ou cancelando o próprio golpe (não o Super), fora da troca.
     */
    boolean canShinkai(CombatFighter f) {
        Side s = sides[f.index];
        if (!f.state.shinkaiReady() || s.tagging() || f.locked || f.ko() || f.frozen()) return false;
        return f.status == CombatFighter.Status.NEUTRAL ||
            (f.attacking() && f.attack.kind != AttackDefinition.Kind.SUPER);
    }

    /** Round novo para o lado: cada um da dupla pode usar o SHINKAI de novo. */
    void refreshShinkai(int side) {
        for (FighterState m : sides[side].members) m.refreshShinkai();
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
        // Com o DHC já pedido, outro TAG não chama assist por cima dele.
        if (s.dhcPending) s.assistRequestAge = -1;
        if (s.assistRequestAge >= 0 && point.status == CombatFighter.Status.BLOCKSTUN) {
            // TAG while blocking: Guard Cancel Tag, when the partner is ready and there is a bar.
            if (!point.frozen() && canGuardCancel(s) && engine.guardCancel(s, point)) {
                s.assistRequestAge = -1;
                lockTeamActions(s);
            }
        } else if (s.assistRequestAge >= 0 && inReleasedSuper(point)) {
            // TAG during your own Super, once it fired: DHC, the partner comes in with its Super
            // as soon as the Super's strikes are over (below).
            if (dhcReady(s, point)) {
                s.dhcPending = true;
                s.dhcHenchmen = point.state.profile.henchmen > 0;
                s.assistRequestAge = -1;
            }
        } else if (s.assistRequestAge >= 0) {
            if (s.conversionOpen()) {
                s.convert = true;
                s.convertFailReason = null;
                s.assistRequestAge = -1;
                if (s.assistPhase == ASSIST_LEAVE) {
                    // Já correndo para fora: para onde está e espera a troca.
                    s.assistPhase = ASSIST_WAIT;
                    s.assistFrame = 0;
                    s.assistToX = s.assist.x;
                }
            } else if (canAssist(s, point)) {
                startAssist(s, point, opponent);
                s.assistRequestAge = -1;
            }
        }
        if (s.dhcPending) {
            // Os capangas do p07 batem depois que o Super dele acaba: o DHC espera por eles com o
            // titular livre; qualquer outro DHC cai quando o Super acaba.
            boolean inSuper = inReleasedSuper(point);
            if (!dhcReady(s, point) || !inSuper && (!s.dhcHenchmen || !point.canAct())) {
                s.dhcPending = false;
            } else if (!point.frozen() && (!inSuper || superStrikesDone(point)) &&
                !engine.henchmenStillToStrike(point.index) && engine.dhc(s, point)) {
                s.dhcPending = false;
                lockTeamActions(s);
            }
        }
        if (s.tagRequestAge >= 0 && canRawTag(s, point)) {
            s.tagPhase = TAG_EXIT;
            s.kickEntry = true;
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
        if (!s.partnerStanding() || s.assistOut() || s.assistCooldown > 0 || s.tagging() || s.leavingFrame >= 0) {
            return false;
        }
        if (point.locked || point.ko()) return false;
        // Called from neutral or during the point's own normals/specials (to extend a combo).
        return point.status == CombatFighter.Status.NEUTRAL ||
            (point.attacking() && point.attack.kind != AttackDefinition.Kind.SUPER);
    }

    /** The partner is free to come in (assist ready, no tag or assist going on). */
    private static boolean canGuardCancel(Side s) {
        return s.partnerStanding() && !s.tagging() && !s.assistOut() && s.assistCooldown == 0 && s.leavingFrame < 0;
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

    /**
     * As pancadas do Super já saíram todas: a janela de acerto passou ou ele já bateu tudo (o
     * Super de projétil, assim que solta; o dos capangas espera os capangas, no motor).
     */
    private static boolean superStrikesDone(CombatFighter point) {
        AttackDefinition attack = point.attack;
        return !attack.melee() || point.attackFrame > attack.lastActiveFrame() || point.hitsOnTarget >= attack.maxHits;
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

    /**
     * Cinemática da história: o titular (o primeiro da dupla) volta para a vez com a troca de
     * verdade (o da vez sai correndo, o titular entra e posa), sem esperar a recarga da troca.
     * Falso se já está na vez ou se agora não dá (titular caído, troca ou assist no meio, ou o
     * da vez ainda não pode agir).
     */
    boolean tagToStarter(int side, CombatFighter point) {
        Side s = sides[side];
        if (s.point == 0 || !s.partnerStanding() || s.tagging() || s.assistOut() || s.leavingFrame >= 0 ||
            !point.canAct()) {
            return false;
        }
        s.tagPhase = TAG_EXIT;
        s.kickEntry = false;
        s.tagFrame = 0;
        s.tagDirection = -point.facing;
        s.tagRequestAge = -1;
        point.locked = true;
        return true;
    }

    /** O titular da dupla está na vez, sem troca acontecendo nem pedida. */
    boolean starterOnPoint(int side) {
        Side s = sides[side];
        return s.point == 0 && !s.tagging() && s.tagRequestAge < 0;
    }

    /** Esquece os TAG apertados que ainda não saíram, dos dois lados (a cinemática tomou o controle). */
    void dropRequests() {
        for (Side s : sides) s.assistRequestAge = s.tagRequestAge = -1;
    }

    private boolean canRawTag(Side s, CombatFighter point) {
        return s.partnerStanding() && !s.tagging() && s.tagCooldown == 0 && !s.assistOut() &&
            s.leavingFrame < 0 && point.canAct();
    }

    private void startAssist(Side s, CombatFighter point, CombatFighter opponent) {
        FighterState partner = s.partner();
        float to = Arena.clamp(point.x - point.facing * config.assistBehind, Arena.LEFT_BOUND, Arena.RIGHT_BOUND);
        int facing = opponent.x >= to ? 1 : -1;
        CombatFighter a = new CombatFighter(point.index, partner, to - facing * config.assistEnterDistance, facing);
        AttackDefinition move = CombatEngine.assistAttack(a);
        s.assistDive = move.mechanics.dives();
        if (move.melee()) {
            // Corpo a corpo: corre até onde o golpe pega o rival (o mergulho, até onde a descida
            // chega nele), entre o lugar de sempre atrás do titular e assistMeleeAhead à frente.
            int dir = opponent.x >= point.x ? 1 : -1;
            float stop = move.reach * 0.8f;
            if (s.assistDive) stop += config.assistDiveHeight * move.mechanics.diveForward / move.mechanics.diveDescent;
            float ahead = ((opponent.x - dir * stop) - point.x) * dir;
            ahead = Math.max(-config.assistBehind, Math.min(config.assistMeleeAhead, ahead));
            to = Arena.clamp(point.x + dir * ahead, Arena.LEFT_BOUND, Arena.RIGHT_BOUND);
            facing = opponent.x >= to ? 1 : -1;
            // Entra como os outros (assistEnterDistance em assistEnterFrames), só que até o lugar novo:
            // o mesmo tempo até o golpe, que é o que encaixa o assist no combo.
            a.x = to - facing * config.assistEnterDistance;
            a.facing = facing;
            if (s.assistDive) a.grounded = false;
        }
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
        // O SHINKAI é de quem está em campo: saiu (troca), acabou. O tempo espera os
        // congelamentos (o próprio brilho, os Supers).
        for (FighterState m : s.members) {
            if (m.shinkaiFrames <= 0) continue;
            if (m != s.pointState()) m.shinkaiFrames = 0;
            else if (!engine.superFreezeActive()) {
                m.shinkaiFrames--;
                // ENERGY: a barra de Super enche sozinha.
                if (m.shinkaiType == Shinkai.Type.ENERGY) m.addSuperMeter(config.shinkaiEnergyMeterPerFrame);
            }
        }
        regenerate(s);
    }

    /**
     * Members waiting off screen get their recoverable life back, little by little. In
     * SHINKAI the point gets it back too, and faster.
     */
    private void regenerate(Side s) {
        if (s.pointState().inShinkai()) s.pointState().regenerate(config.shinkaiRegenPerFrame);
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
        if (s.tagPhase == TAG_KICK) {
            // Lutando de verdade: acaba quando a voadora acaba (no chão, defendida ou levando um
            // golpe no caminho).
            if (!point.attacking() || !VoadoraTroca.ID.equals(point.attack.id)) {
                s.tagPhase = TAG_NONE;
                s.tagFrame = 0;
                s.tagCooldown = config.tagCooldownFrames;
            }
            return;
        }
        point.locked = true;
        if (s.tagPhase == TAG_EXIT && s.tagFrame >= config.tagExitFrames) {
            swapPoint(engine, s, point);
            s.tagFrame = 0;
            if (s.kickEntry && engine.startTagKick(point)) {
                s.tagPhase = TAG_KICK;
            } else {
                s.tagPhase = TAG_ENTER;
            }
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
            // O de mergulho entra pulando: sobe até a altura de onde desce batendo.
            if (s.assistDive) a.y = Arena.GROUND_Y - config.assistDiveHeight * (1f - (1f - t) * (1f - t));
            if (t >= 1f) {
                engine.startAssistMove(a);
                s.assistPhase = ASSIST_ATTACK;
                s.assistFrame = 0;
            }
        } else if (s.assistPhase == ASSIST_ATTACK) {
            if (!engine.assistAwaitsGround(a)) a.attackFrame++;
            engine.moveAssist(a);
            if (!a.attack.melee() && a.attackFrame == a.attack.startupFrames) {
                engine.spawnAssistProjectile(a);
            }
            if (a.attackFrame >= a.attack.totalFrames - 1) {
                if (s.convert && engine.convertAssist(s, point)) {
                    s.convertResult = 1;
                    return;
                }
                a.status = CombatFighter.Status.NEUTRAL;
                a.clearAttack();
                // Troca pedida com o titular ainda preso: o parceiro espera um pouco antes de ir.
                s.assistPhase = s.convert ? ASSIST_WAIT : ASSIST_LEAVE;
                s.assistFrame = 0;
                s.assistToX = a.x;
            }
        } else if (s.assistPhase == ASSIST_WAIT) {
            if (engine.convertAssist(s, point)) {
                s.convertResult = 1;
                return;
            }
            if (s.assistFrame >= config.assistTagBufferFrames) {
                // Não deu: a troca cai, avisada, e o parceiro sai correndo.
                s.convert = false;
                s.convertFailReason = whyNoSwap(point);
                s.convertResult = -1;
                s.assistPhase = ASSIST_LEAVE;
                s.assistFrame = 0;
                s.assistToX = a.x;
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

    /** Por que o titular não pôde sair para a troca (o aviso do HUD). */
    static String whyNoSwap(CombatFighter point) {
        if (point.ko()) return "NOCAUTE";
        switch (point.status) {
            case HITSTUN: case AIR_HITSTUN: return "APANHANDO";
            case BLOCKSTUN: return "DEFENDENDO";
            case KNOCKDOWN: case WAKEUP: return "CAÍDO";
            case ULTRA: return "NO ULTRA";
            default: return "OCUPADO";
        }
    }

    /** Assist → Tag, called by the engine once the assist's move ended. */
    void becomePoint(Side s, CombatFighter point) {
        CombatFighter a = s.assist;
        leavePoint(s, point);
        s.assist = null;
        s.assistPhase = ASSIST_NONE;
        s.assistFrame = 0;
        s.convert = false;
        lockTeamActions(s);
        if (a != null) a.travel = 0f;
    }

    // ---------------------------------------------------------------- batalha 2x2

    /** Batalha 2x2: o lado perdeu quando ninguém do time tem vida. */
    boolean defeated(int side) {
        for (FighterState m : sides[side].members) if (m.life > 0) return false;
        return true;
    }

    /**
     * Fim do tempo: o lado com mais vida, comparando a proporção da vida total de cada dupla
     * (soma da vida sobre a soma da vida máxima; o vermelho recuperável não conta). -1 quando
     * as duas proporções são iguais (empate). Conta em inteiros para ser determinístico.
     */
    int moreLife() {
        long[] life = new long[2], max = new long[2];
        for (int side = 0; side < 2; side++) {
            for (FighterState m : sides[side].members) {
                life[side] += Math.max(0, m.life);
                max[side] += m.profile.maxLife;
            }
        }
        long first = life[0] * max[1], second = life[1] * max[0];
        return first > second ? 0 : second > first ? 1 : -1;
    }

    /** O próximo membro com vida depois do titular, ou -1. */
    private static int nextStanding(Side s) {
        for (int i = 1; i < s.members.length; i++) {
            int index = (s.point + i) % s.members.length;
            if (s.members[index].life > 0) return index;
        }
        return -1;
    }

    /** O titular caiu e há alguém com vida para entrar no lugar dele. */
    boolean canReplaceFallen(int side) {
        Side s = sides[side];
        return s.pointState().life <= 0 && nextStanding(s) >= 0;
    }

    /**
     * Batalha 2x2: o titular caiu; o próximo com vida vira titular e entra correndo por trás
     * (a mesma entrada da troca), olhando para {@code facing}. O assist em campo some com ele
     * e a barra do time passa para quem entra. Falso quando não há ninguém para entrar.
     */
    boolean enterAfterKo(int side, int facing) {
        Side s = sides[side];
        int next = nextStanding(s);
        if (s.pointState().life > 0 || next < 0) return false;
        s.assist = null;
        s.assistPhase = ASSIST_NONE;
        s.assistFrame = 0;
        s.convert = false;
        s.leaving = null;
        s.leavingFrame = -1;
        s.assistRequestAge = s.tagRequestAge = -1;
        FighterState fallen = s.pointState();
        s.point = next;
        handMeter(fallen, s.pointState());
        s.tagPhase = TAG_ENTER;
        s.tagFrame = 0;
        s.tagDirection = -facing;
        return true;
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

    /** After a tag, guard cancel or DHC: neither assist nor tag for a moment. */
    private void lockTeamActions(Side s) {
        s.assistCooldown = Math.max(s.assistCooldown, config.assistTagCooldownFrames);
        s.tagCooldown = Math.max(s.tagCooldown, config.assistTagCooldownFrames);
    }

    private static void handMeter(FighterState from, FighterState to) {
        if (from == to || from.superMeter == 0) return;
        to.addSuperMeter(from.superMeter);
        from.superMeter = 0;
        from.refreshHudLabels();
    }
}
