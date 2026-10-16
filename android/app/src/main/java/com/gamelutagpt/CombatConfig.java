package com.gamelutagpt;

/**
 * Tunables of the combat engine shared by every character. Per-move numbers live in the
 * character packs; these are the system-wide rules (buffer, scaling, decay, physics).
 * A new instance holds the defaults; tests may change fields before creating an engine.
 */
final class CombatConfig {
    /** Fixed simulation rate. Every frame count in packs and here is at this rate. */
    static final int FPS = 60;
    static final float DT = 1f / FPS;

    /** Frames a press stays usable after the frame it was pressed (hitstop does not count). */
    int bufferFrames = 4;
    /** A special command must be entered within this many frames... */
    int motionWindowFrames = 15;
    /** ...and its button pressed at most this many frames after the last direction. */
    int motionPressFrames = 15;
    /** Frames a mais na janela para cada passo além de dois (meia-lua: ← ↓ → tem 3 passos). */
    int motionWindowPerStep = 6;
    /** Comando de segurar ([←] →, [↓] ↑): tempo segurando para carregar... */
    int chargeFrames = 30;
    /** ...e quanto a carga dura depois de soltar. */
    int chargeKeepFrames = 10;
    /**
     * Com a carga para baixo pronta, o pulo espera estes frames: o botão do [↓] ↑ que chega
     * junto vira o especial em vez do pulo.
     */
    int chargeJumpGraceFrames = 3;
    /** Golpe em mergulho: frames de recuperação que sobram ao tocar o chão. */
    int diveLandingFrames = 8;
    int doubleTapFrames = 18;
    /** Down entered at most this many frames before up makes a super jump. */
    int superJumpFrames = 22;

    /** Maximum juggle points a defender in AIR_HITSTUN can take in one combo. */
    int juggleLimit = 6;

    /** Damage scaling, in permille of the base damage. */
    int damageStepPermille = 50;
    int damageFloorPermille = 200;
    int specialFloorPermille = 300;
    int superFloorPermille = 500;
    /** Extra reduction for every previous use of the same move in the combo. */
    int repeatPenaltyPermille = 200;

    /** Hitstun removed, indexed by hits already in the combo (the last value repeats). */
    int[] hitstunDecayByHits = {0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5};
    /** Hitstun removed, indexed by whole seconds the combo has lasted (the last value repeats). */
    int[] hitstunDecayBySeconds = {0, 0, 1, 2, 4, 6};
    int minHitstunFrames = 1;

    /** Pushback and knockback are spread over this many frames. */
    int pushbackFrames = 8;
    /** After a LAUNCH hit, a jump becomes a super jump for this many frames. */
    int launcherChaseFrames = 54;
    int knockdownFallFrames = 13;
    /** Lying on the ground before the normal wake-up (was 72: too long to watch). */
    int knockdownDownFrames = 30;
    int wakeupFrames = 21;
    /** Longest wake-up: a longer GETUP clip plays faster to fit it. */
    int maxWakeupFrames = 30;

    /** Default input priority when a character does not declare its own. */
    static final AttackDefinition.Strength[] DEFAULT_PRIORITY = {
        AttackDefinition.Strength.SUPER,
        AttackDefinition.Strength.SPECIAL,
        AttackDefinition.Strength.HEAVY,
        AttackDefinition.Strength.MEDIUM,
        AttackDefinition.Strength.LIGHT
    };

    // Physics (world units per second, world units per second squared).
    float walkSpeed = 300f;
    /** Walking (or drifting in the air) away is slower, so retreating cannot outrun a chase. */
    float walkBackSpeed = 220f;
    float dashSpeed = 620f;
    float backdashSpeed = 978f;
    int backdashFrames = 15;
    int backdashRecoveryFrames = 12; // extra grounded neutral frames before another hop
    /** Visual retreat hop is shared by all fighters; ground collision remains unchanged. */
    float backdashHopHeight = 40f;
    float backdashVisualLift(int framesLeft) {
        if (framesLeft <= 0 || framesLeft > backdashFrames) return 0f;
        float fraction = (backdashFrames - framesLeft) / (float)backdashFrames;
        return backdashHopHeight * (float)Math.sin(Math.PI * fraction);
    }
    // 60 FPS normal jump: ~42 airborne frames, apex ~1.16x P01's standing hurtbox.
    // Sized for the 226-unit P01 (the 156-unit version used 1030 / 1.70 / 2.00, apex ~182):
    // speed and gravity scale by 226/156, so the cadence stays and only the height grows.
    float jumpSpeed = 1492f;
    float normalJumpRisingGravityScale = 2.463f;
    float normalJumpFallingGravityScale = 2.897f;
    float superJumpSpeed = 1450f;
    /** Cancelamento de pulo no ar (jM → pulo, uma vez por voo): impulso para cima. */
    float airJumpSpeed = 900f;
    float gravity = 1650f;
    float launchSpeed = 1450f;
    float slamSpeed = 1850f;
    /** Airborne energy special: vertical speed kept at start and gravity while it runs. */
    float projectileAirVelocityScale = 0.32f;
    float projectileAirGravityScale = 0.12f;
    float facingEpsilon = 6f;
    /** Space left between two bodies by a teleport or a pull (world units). */
    float teleportGap = 24f;
    /** Wall jump: how close to the wall, the push away from it and the lift (share of a jump). */
    float wallJumpReach = 40f;
    int wallJumpFrames = 22;
    float wallJumpSpeed = 760f;
    float wallJumpLift = 0.95f;
    /** Henchmen of a Super: run speed, start behind the caller, stagger, reach and strike timing. */
    float henchmanSpeed = 950f;
    float henchmanEntry = 260f;
    int henchmanStagger = 22;
    float henchmanReach = 120f;
    int henchmanStrikeFrames = 28;
    int henchmanImpactFrame = 10;

    // Super meter, in thousandths of a bar.
    static final int METER_PER_BAR = 1000;
    static final int MAX_METER = 5 * METER_PER_BAR;
    static final int SUPER_COST = METER_PER_BAR;
    /** Ultra (↓ + SUPER): three bars. */
    static final int ULTRA_COST = 3 * METER_PER_BAR;

    // Ultra: activation (the opponent freezes), rush, then either the cinematic (hit),
    // blockstun for the defender (guarded) or a punishable recovery (whiff or block).
    int ultraStartupFrames = 25;
    int ultraRushFrames = 18;
    int ultraRecoveryFrames = 30;
    float ultraRushSpeed = 2000f;
    /** Rush connects when the defender's body is this close in front of the attacker's root. */
    float ultraReach = 96f;
    int ultraBlockstunFrames = 24;
    int ultraHitstopFrames = 8;
    float ultraPushbackOnBlock = 120f;
    /** End of the cinematic: the defender flies away and lands knocked down. */
    float ultraLaunchSpeed = 1150f;
    float ultraKnockback = 420f;
    /** Ultra em sequência: pausa de impacto de cada golpe e do último (o que lança). */
    int ultraSequenceHitstop = 4;
    /** Super that strikes (no wave): frames left to react between the freeze and its first strike. */
    int strikingSuperReactFrames = 6;
    /** Several-strike move: frames of hitstun past the next strike, and the push of the strikes before the last. */
    int strikeHoldFrames = 8;
    float strikeHoldPushback = 0.2f;
    /** Upward speed given to an airborne defender by a strike before the last (stays in reach). */
    float strikeHoldLift = 260f;
    /** An advancing move stops this close to the opponent's body (px past the pushboxes). */
    float advanceContactSlack = 6f;
    int ultraSequenceFinalHitstop = 12;
    /**
     * Ultra cinematográfico (a tela liga): a sequência do vídeo para este tanto de quadros
     * antes do último golpe e espera a página de mangá com o toque no tempo; o último golpe
     * sai depois, com o bônus da nota. Desligado, a sequência corre direto (motor puro, IA).
     */
    boolean ultraSequencePage = false;
    int ultraPageLeadFrames = 4;
    // Final beam (after the cinematic): a charge ("ka-me-ha-me"), then the beam reaches the
    // defender, hits every few frames (the combo counter climbs) and the blast throws the
    // defender like the cinematic end.
    int ultraBeamChargeFrames = 36;
    int ultraBeamExtendFrames = 8;
    int ultraBeamHitInterval = 4;
    /** Frames between the last small hit and the blast. */
    int ultraBeamBlastGap = 6;
    /** Frames the beam fades after the blast before the attacker is free. */
    int ultraBeamFadeFrames = 18;
    int ultraBeamBlastHitstopFrames = 12;
    /** Share of the beam's damage kept for the blast (permille); the rest is split by the hits. */
    int ultraBeamBlastPermille = 300;
    /** Each hit pushes the defender back a little (a cornered defender pushes the attacker). */
    float ultraBeamPushPerHit = 7f;
    /** Recoil: each hit slides the attacker back (feet dragging on the floor). */
    float ultraBeamRecoilPerHit = 1.5f;
    /**
     * The cinematic ends at contact range; the fight comes back with the fighters this far
     * apart (root to root), so the beam is seen crossing the screen.
     */
    float ultraBeamDistance = 520f;

    /** Frame of the beam phase when the beam leaves the hands. */
    int ultraBeamFireFrame() {
        return ultraBeamChargeFrames;
    }

    /** Frame of the beam phase when the blast lands. */
    int ultraBeamBlastFrame(int hits) {
        return ultraBeamChargeFrames + ultraBeamExtendFrames + ultraBeamHitInterval * Math.max(0, hits - 1) +
            ultraBeamBlastGap;
    }

    // Throw (L + M): universal grab. It cannot be blocked; the defender techs with L + M.
    int throwStartupFrames = 5;
    int throwActiveFrames = 3;
    /** Gap between the two bodies within which the grab connects. */
    float throwRange = 28f;
    int throwWhiffFrames = 24;
    /** Frames after the grab during which the defender can tech. */
    int throwTechWindow = 12;
    int throwDamage = 1200;
    float throwKnockback = 170f;
    int throwHitstopFrames = 10;
    /** Attacker recovery after a landed throw (the defender is falling: oki). */
    int throwExecuteFrames = 16;
    /** Tech: both fighters pushed this far apart and frozen out of action for this long. */
    float throwTechPush = 110f;
    int throwTechFrames = 16;
    /** Throw invulnerability right after hitstun, blockstun or wake-up. */
    int throwProtectFrames = 6;

    // Air tech (attack button when the air hitstun ends; the direction picks where to go).
    /** Without a tech the fighter free-falls this long, then recovers on its own (no invulnerability). */
    int airTechWindow = 12;
    int airTechInvulnFrames = 10;
    float airTechBackDistance = 160f;
    float airTechForwardDistance = 120f;
    float airTechNeutralPop = 380f;
    // Wake-up options, chosen with the direction while lying down (relative to facing).
    /** ↑: quick rise, after at least this long on the ground. */
    int quickRiseDownFrames = 6;
    /** ← / →: roll (invulnerable, passes through the other body), then up. */
    int rollFrames = 24;
    float rollSpeed = 560f;
    /** ↓: delayed wake-up, this much longer on the ground. */
    int delayWakeupFrames = 30;

    // Wall bounce and ground bounce: once each per combo (the next one is a plain hit).
    int maxWallBounces = 1;
    /**
     * Punish counter (como no SF6): um H em pé que acerta o rival no meio do golpe dele
     * (início ou recuperação) o joga girando no ar a média distância (~360 px), e ele cai deitado.
     */
    float spinSpeed = 360f;
    /** Congelamento extra do acerto no punish counter e no golpe que nocauteia (peso do impacto). */
    int punishHitstopBonus = 8;
    /**
     * Pose do impacto (como no FighterZ): golpes médios e fortes que acertam congelam um pouco
     * mais, para a pose esticada "grudar" na tela. Os dois lutadores congelam juntos, então a
     * vantagem e os combos não mudam. Defesa não ganha.
     */
    int mediumHitHoldFrames = 3;
    int heavyHitHoldFrames = 4;
    int koHitstopBonus = 16;
    float spinLift = 820f;
    int spinHitstunFrames = 30;
    int maxGroundBounces = 1;
    /** The defender flies toward the wall: the arena edge or this far from the attacker (screen edge). */
    float wallBounceSpeed = 1500f;
    float wallBounceDistance = 620f;
    float wallBounceLift = 260f;
    /** After hitting the wall: it comes back a little and pops up, open to a follow-up. */
    float wallBounceReturnSpeed = 300f;
    float wallBounceUp = 820f;
    /** Ground bounce: driven into the floor, then it pops back up. */
    float groundBounceDown = 1500f;
    float groundBounceUp = 920f;
    /** Hitstun after either bounce; landing afterwards is a knockdown (no air recovery). */
    int bounceHitstunFrames = 48;

    // Air dash (→ → / ← ← in the air): once per jump, the body holds its height while it lasts.
    int airDashFrames = 14;
    float airDashSpeed = 900f;
    int backAirDashFrames = 12;
    float backAirDashSpeed = 640f;
    /** No air dash this close to the ground (it would be a ground dash). */
    float airDashMinHeight = 36f;

    // Pushblock (M + H while blocking): pushes the attacker away and shortens the blockstun.
    int pushblockCost = METER_PER_BAR / 4;
    float pushblockDistance = 190f;
    /** Blockstun left after a pushblock (at most). */
    int pushblockStunFrames = 8;
    // Guard Cancel Tag (TAG while blocking on the ground): the partner comes in attacking.
    int guardCancelCost = METER_PER_BAR;
    /** The opponent freezes for the cross-counter flash. */
    int guardCancelFlashFrames = 12;
    /** Extra invulnerable frames of the incoming partner past its move's active frames. */
    int guardCancelInvulnPadding = 4;

    // Recoverable life: part of every hit's damage (permille) can come back while the
    // fighter waits off point, at this much life per frame.
    int recoverableLifePermille = 270;
    int recoverableLifePerFrame = 6;

    // Overdrive (botão OD ou SUPER + TAG): once per round, about 8 seconds for the team.
    int overdriveFrames = 480;
    /** Both fighters freeze on activation; the opponent's stun waits too (combo extension). */
    int overdriveFlashFrames = 20;
    /** Walk, dash and air dash speed multiplier. */
    float overdriveSpeed = 1.25f;
    /** Meter gained in Overdrive, permille of the normal gain. */
    int overdriveMeterPermille = 1500;
    /** Recoverable life per frame in Overdrive, for every member (the point too). */
    int overdriveRegenPerFrame = 12;

    // Team (TeamSystem). Raw tag (↓ + TAG): the point runs off, the partner runs in and poses.
    int tagExitFrames = 20;
    int tagEnterFrames = 23;
    int tagPoseFrames = 27;
    float tagTravel = 760f;
    int tagCooldownFrames = 600;
    // Assist (TAG): the partner runs in behind the point, does its move and runs off.
    int assistEnterFrames = 8;
    int assistLeaveFrames = 10;
    float assistBehind = 70f;
    float assistEnterDistance = 320f;
    int assistCooldownFrames = 240;
    /** After Assist → Tag: neither another assist nor a raw tag for this long. */
    int assistTagCooldownFrames = 300;

    // Batalha 2x2 (BattleFlow): o titular caído fica no chão um pouco e o parceiro entra.
    /** Frames do titular caído no chão antes do parceiro entrar. */
    int koReplaceFrames = 75;
    /** Invulnerável além da entrada (corrida + pose) do parceiro que entra depois do KO. */
    int koEntryInvulnPadding = 20;

    static int hitstunDecay(int[] table, int index) {
        if (table.length == 0) return 0;
        return table[Math.min(Math.max(0, index), table.length - 1)];
    }
}
