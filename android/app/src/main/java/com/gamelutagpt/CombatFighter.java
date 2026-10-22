package com.gamelutagpt;

/**
 * Simulation state of one fighter: body, current action, reaction, freeze and input.
 * {@link CombatEngine} is the only writer; the shell (GameView) reads it to render.
 */
final class CombatFighter {
    /** Character state machine. Attack phases and hitstop are derived, see {@link #stateName}. */
    enum Status { NEUTRAL, ATTACK, HITSTUN, BLOCKSTUN, AIR_HITSTUN, KNOCKDOWN, WAKEUP, ULTRA, THROW, THROWN }

    /** Phases of {@link Status#ULTRA}. */
    static final int ULTRA_STARTUP = 0;
    static final int ULTRA_RUSH = 1;
    static final int ULTRA_RECOVERY = 2;
    /** Rush connected: the engine waits while the shell plays the cinematic. */
    static final int ULTRA_CINEMATIC = 3;
    /** After the cinematic: the final beam, many small hits and a last blast. */
    static final int ULTRA_BEAM = 4;
    /** Ultra que segue o vídeo: a sequência de golpes nos quadros da animação. */
    static final int ULTRA_SEQUENCE = 5;

    /** Phases of {@link Status#THROW} (L + M). */
    static final int THROW_STARTUP = 0;
    /** Holding the defender: its tech window is open. */
    static final int THROW_HOLD = 1;
    /** The throw landed: the attacker recovers while the defender falls. */
    static final int THROW_EXECUTE = 2;
    /** Nobody in reach: a punishable recovery. */
    static final int THROW_WHIFF = 3;
    /** Teched (both fighters): pushed apart, neither acts for a moment. */
    static final int THROW_TECH = 4;

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
    /** Como o lutador se move de lado no ar (veja {@link #jumpMode}). */
    static final int PULO_LIVRE = 0, PULO_COMPROMETIDO = 1, PULO_SUPER = 2, PULO_CORRIDA = 3;
    /**
     * Controle de lado no ar, decidido na saída do chão (como no FighterZ e no MvC):
     * <ul>
     *   <li>{@link #PULO_COMPROMETIDO}: pulo normal; a direção (↖ ↑ ↗) fica fixa.</li>
     *   <li>{@link #PULO_SUPER}: super pulo; o stick puxa aos poucos (drift) e o embalo fica.</li>
     *   <li>{@link #PULO_CORRIDA}: pulo saído da corrida (dash jump, do KOF); vai mais longe
     *       para frente, fixo.</li>
     *   <li>{@link #PULO_LIVRE}: no ar sem ter pulado (recuperação, depois de apanhar): o
     *       stick dirige como antes.</li>
     * </ul>
     * Volta a livre ao tocar o chão ou ao apanhar no ar.
     */
    int jumpMode;
    /** Direção na tela (-1, 0, 1) do pulo comprometido ou da corrida. */
    int jumpDirection;
    /** Velocidade de lado do super pulo (o drift muda aos poucos). */
    float jumpDriftVx;
    /** Quadros no ar desde a saída: nos primeiros ainda dá para escolher a direção. */
    int jumpFrames;
    int backdashFrames;
    /** Prevent repeated retreats while the previous hop/recovery has not reset. */
    int backdashCooldown;
    boolean backdashArmed = true;
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
    /** Último quadro do golpe normal que acertou: a recuperação acaba aí (-1: vai inteira). */
    int hitCutFrame = -1;
    /** O rival atordoado em pé quando o golpe acertou: perde o mesmo tanto no corte. */
    boolean hitCutStun;
    String specialStrength;
    /** Step of the autoCombo route this attack came from, or -1. */
    int autoStep = -1;
    int hitsOnTarget;
    long hitboxMask;
    /** Several-strike move: a strike already hit / was guarded / paid the juggle (once per move). */
    boolean strikeLanded, strikeBlocked, strikeJuggled;
    /** Hits the current move's armor already absorbed. */
    int armorUsed;
    /** Frames the current move was charged, and whether it is charging right now. */
    int charge;
    boolean charging;
    /** Frames of shadow echo left (the shadow repeats every move started meanwhile). */
    int echoLeft;
    /** Echo settings taken from the move that turned it on. */
    int echoDelay, echoDamagePermille;
    /** Jumped off the wall in this jump (once per jump), and frames of the push away. */
    boolean wallJumpUsed;
    int wallJumpFrames;
    int wallJumpDirection;

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
    /** Frames left in which holding → after a wall bounce starts the run (see wallChaseFrames). */
    int wallChase;
    int framesSinceHit = 999;
    int framesSinceBlock = 999;
    /**
     * Guard break: a defesa quebrou e ele está aberto (o atordoamento é um hitstun longo, sem
     * defesa). Some quando o hitstun acaba.
     */
    boolean guardBroken;
    /**
     * Defesa perfeita: quadros desde que trás (a defesa) foi apertado de novo, se o aperto vale
     * (não veio logo depois do anterior: quem fica batendo trás não ganha a janela) e se trás
     * estava seguro no quadro anterior.
     */
    int guardPressAge = 999;
    boolean guardPressCounts;
    boolean guardDirectionHeld;
    /** A última defesa foi perfeita (para a tela; some na próxima defesa). */
    boolean justDefended;
    /** Quem quebrou o último combo quebrado (para a tela; o outro lado zera quando quebra). */
    boolean comboBroke;
    /**
     * Combo breaker (M + H apanhando): quadros desde o último aperto, se ele vale (não veio logo
     * depois do anterior: quem fica batendo M + H não ganha a janela) e há quantos quadros saiu
     * cada metade sozinha (os dois dedos não precisam cair no mesmo quadro), ou -1.
     */
    int breakerPressAge = 999;
    boolean breakerPressCounts;
    int breakerMediumAge = -1, breakerHeavyAge = -1;
    /** Quadros em que quem quebrou o combo ainda corre sozinho até quem foi jogado para longe. */
    int breakerChase;
    /** A bola de energia (não Super) cujo acerto o segura no atordoamento agora, ou null. */
    Projectile stunProjectile;
    /** Set by the shell while a tag animation owns the fighter. */
    boolean locked;

    // Ultra.
    int ultraPhase;
    /** Frames spent in the current ultra phase. */
    int ultraFrame;
    /** Damage scale of the ultra, fixed when the rush connects (permille). */
    int ultraScale = 1000;
    /** Thrown by an ultra: lands knocked down instead of recovering in the air. */
    boolean ultraFall;

    // Recovery options.
    static final int WAKE_NORMAL = 0, WAKE_QUICK = 1, WAKE_BACK_ROLL = 2, WAKE_FORWARD_ROLL = 3, WAKE_DELAY = 4;
    /** Frames since an attack button asked for an air tech, or -1. */
    int techRequestAge = -1;
    /** Frames spent free-falling after the air hitstun ended without a tech. */
    int freeFallFrames;
    /** How this knockdown ends (chosen while lying down). */
    int wakeup = WAKE_NORMAL;
    /** Frames of roll left and its world direction. */
    int rollFrames;
    int rollDirection;
    /** Knocked down by an ultra: no wake-up options. */
    boolean hardKnockdown;

    // Bounces.
    static final int BOUNCE_NONE = 0, BOUNCE_WALL = 1, BOUNCE_GROUND = 2;
    /** Bounce waiting to happen (flying to the wall or driven into the floor). */
    int bounce;
    /** Horizontal velocity of a wall-bounce flight (0 otherwise). */
    float vx;
    /** World x of the wall the defender is flying to. */
    float bounceWallX;
    /** Bounced: lands knocked down instead of recovering in the air. */
    boolean hardFall;
    /** Punish counter: flying away spinning in the air (lands knocked down). */
    boolean spinning;
    /** Beam: hits it deals, hits dealt so far, damage left for the hits and for the blast. */
    int beamHits;
    int beamHitsDone;
    int beamDamageLeft;
    int beamBlastDamage;
    /** Ultra em sequência: golpes já dados, distância e altura do rival preso no começo. */
    int ultraHitsDone;
    /**
     * Ultra cinematográfico: a sequência parou antes do último golpe para a página de mangá
     * (já pediu uma vez nesta sequência), e o bônus da nota do toque para o último golpe.
     */
    boolean ultraPaused, ultraPageDone;
    float ultraFinalBonus;
    /** Who the sequence caught (a partner coming in after a KO is not held or hit). */
    CombatFighter ultraVictim;
    FighterState ultraVictimState;
    float ultraHoldGap;
    float ultraHoldY;

    // Air dash.
    /** Frames of air dash left (0 when not dashing). */
    int airDashFrames;
    /** World direction of the current air dash (+1 right, -1 left). */
    int airDashDirection;
    boolean airDashBack;
    /** The air dash of this jump was spent; landing gives it back. */
    boolean airDashUsed;
    /** Já usou o cancelamento de pulo no ar (jM → pulo) neste voo; volta ao tocar o chão. */
    boolean airJumpUsed;
    /**
     * Golpes da corrente atual (desde o último golpe que saiu do neutro). O limite de usos do
     * golpe vale aqui também, acertando ou na defesa: assim uma corrente que volta para trás
     * (H → L do p12) ou repete um golpe (M → M) sempre acaba.
     */
    final java.util.ArrayList<String> chain = new java.util.ArrayList<>();
    /** Air dashes done in this jump (a fighter may have more than one). */
    int airDashCount;

    // Throw.
    int throwPhase;
    int throwFrame;
    /** Frames during which this fighter cannot be thrown (right after stun or wake-up). */
    int throwProtect;
    /** Frames since L + M was pressed, or -1: starts a throw or techs one. */
    int throwRequestAge = -1;
    /** Frames since M + H was pressed while blocking, or -1: pushblock. */
    int pushblockRequestAge = -1;
    /** Frames since the Overdrive was asked for, or -1. */
    int overdriveRequestAge = -1;
    /** Frames during which nothing hits this fighter (Guard Cancel entry). */
    int invulnFrames;

    // Input.
    final InputBuffer buffer = new InputBuffer();
    final MotionParser motion = new MotionParser();
    final FighterInput input = new FighterInput();
    /** Local frame clock: it does not advance while frozen. */
    int clock;
    int pendingJumpAge = -1;
    /** Frames que o pulo pedido ainda espera (o botão do [↓] ↑ pode chegar). */
    int jumpGrace;
    boolean pendingSuperJump;
    /** Quadros que faltam da preparação do pulo (jump squat), o pulo pedido e a direção dele. */
    int jumpSquat;
    boolean squatSuper;
    int squatDirection;
    /** Quadros que faltam da aterrissagem do pulo vazio. */
    int landingLag;
    /** Frames since ↓ + SUPER asked for an ultra, or -1 (kept for the input buffer). */
    int ultraRequestAge = -1;
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

    /** Rolling on wake-up: invulnerable and passes through the other body. */
    boolean rolling() {
        return status == Status.WAKEUP && rollFrames > 0;
    }

    boolean hittable() {
        return !ko() && status != Status.KNOCKDOWN && status != Status.WAKEUP && !firingBeam() && !inUltraSequence() && !inThrowExchange() &&
            invulnFrames == 0;
    }

    /** Holding a throw, being thrown or teching: the exchange plays out untouched. */
    boolean inThrowExchange() {
        return status == Status.THROWN ||
            (status == Status.THROW && (throwPhase == THROW_HOLD || throwPhase == THROW_TECH));
    }

    /** Firing the ultra beam: nothing interrupts it. */
    boolean firingBeam() {
        return status == Status.ULTRA && ultraPhase == ULTRA_BEAM;
    }

    /** Dando a sequência do Ultra: nada interrompe. */
    boolean inUltraSequence() {
        return status == Status.ULTRA && ultraPhase == ULTRA_SEQUENCE;
    }

    /** Crouching hurtbox: crouch stance, crouching attack or crouching hit reaction. */
    /** No chão preparando o pulo ou aterrissando de um pulo vazio: sem golpe, pulo, dash nem andar. */
    boolean busyOnGround() {
        return jumpSquat > 0 || landingLag > 0;
    }

    boolean crouchingBody() {
        if (status == Status.ATTACK && move != null) return move.binding.startsWith("2");
        if (status == Status.HITSTUN) return hitCrouching;
        if (status == Status.BLOCKSTUN) return lastGuard == GUARD_LOW;
        return crouching || anticipatedGuard == GUARD_LOW;
    }

    float hurtTop() {
        return y - body().height(crouchingBody());
    }

    /** Name shown by the debug overlay (and checked by tests): the states of the V2 specification. */
    String stateName() {
        if (hitstop > 0) return "HITSTOP";
        if (status == Status.ATTACK) return "ATTACK_" + attack.phase(Math.max(0, attackFrame));
        if (status == Status.THROW) {
            return throwPhase == THROW_STARTUP ? "THROW_STARTUP"
                : throwPhase == THROW_HOLD ? "THROW_HOLD"
                : throwPhase == THROW_EXECUTE ? "THROW_EXECUTE"
                : throwPhase == THROW_WHIFF ? "THROW_WHIFF" : "THROW_TECH";
        }
        if (status == Status.ULTRA) {
            return ultraPhase == ULTRA_STARTUP ? "ULTRA_STARTUP"
                : ultraPhase == ULTRA_RUSH ? "ULTRA_RUSH"
                : ultraPhase == ULTRA_RECOVERY ? "ULTRA_RECOVERY"
                : ultraPhase == ULTRA_BEAM ? "ULTRA_BEAM"
                : ultraPhase == ULTRA_SEQUENCE ? "ULTRA_SEQUENCE" : "ULTRA_CINEMATIC";
        }
        return status.name();
    }

    void clearAttack() {
        attack = null;
        move = null;
        attackFrame = -1;
        outcome = Outcome.NONE;
        hitCutFrame = -1;
        hitCutStun = false;
        specialStrength = null;
        autoStep = -1;
        hitsOnTarget = 0;
        hitboxMask = 0L;
        strikeLanded = strikeBlocked = strikeJuggled = false;
        armorUsed = 0;
        charge = 0;
        charging = false;
    }

    /** The charge reached the point where it counts as a charged shot. */
    boolean charged() {
        return attack != null && attack.mechanics.chargeFrames > 0 && charge >= attack.mechanics.chargeFrames / 3;
    }

    /** Mechanics of the move in progress at its current frame (none outside an attack). */
    AttackDefinition.Mechanics mechanics() {
        return attacking() ? attack.mechanics : AttackDefinition.NO_MECHANICS;
    }

    int moveFrame() {
        return Math.max(0, attackFrame);
    }
}
