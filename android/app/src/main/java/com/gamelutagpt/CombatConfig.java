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
    /** O especial (comando já lido) espera mais que o botão comum: apertado na recuperação ou na defesa. */
    int specialBufferFrames = 10;
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

    /**
     * Maximum juggle points a defender in AIR_HITSTUN can take in one combo. Sem limite: o
     * combo no ar acaba pelo hitstun que diminui, pelo empurrão, pelos quiques (um de cada por
     * combo) e pelo combo breaker do defensor. Os pontos continuam contados na sessão.
     */
    int juggleLimit = Integer.MAX_VALUE;

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
    /**
     * Bola de energia que acerta no chão empurra pelo menos isto: de perto, a vantagem do
     * acerto (atordoamento + congelamento só de quem apanha) passava do tempo de soltar outra,
     * e bola atrás de bola virava combo infinito. Longe, a próxima precisa viajar e não entra.
     */
    float projectileHitPushback = 120f;
    /** After a LAUNCH hit, a jump becomes a super jump for this many frames. */
    int launcherChaseFrames = 54;
    /** Atordoamento mínimo de quem foi lançado (o do 2H, 47 no Ryūga): dá tempo do super pulo. */
    int launchHitstunFrames = 44;
    /**
     * After an H that wall-bounces in a combo, holding → starts the run (dash) for this many
     * frames, without the → → (like the launcher turns the jump into a super jump).
     */
    int wallChaseFrames = 54;
    int knockdownFallFrames = 13;
    /**
     * Lying on the ground before the normal wake-up (was 72: too long to watch; then 50, room to
     * run in and set up a cross-up as in City of the Wolves, which lies ~58 and rises in ~14).
     * Agora 22 com o quique, como o KOF '97 (cai, quica, levanta rápido: ~0,9 s da queda até em pé;
     * docs/balanco/comparacao-kof97.md).
     */
    int knockdownDownFrames = 22;
    int wakeupFrames = 16;
    /**
     * Quique ao cair (como no KOF '97, docs/balanco/comparacao-kof97.md): o corpo bate no chão e sobe
     * um pouco deitado antes de assentar. Só o desenho sobe; conta dentro do tempo deitado.
     */
    int knockdownBounceFrames = 14;
    float knockdownBounceHeight = 30f;
    /** Quanto o corpo caído sobe no desenho no quadro {@code knockdownFrame} da queda. */
    float knockdownBounceLift(int knockdownFrame) {
        int t = knockdownFrame - knockdownFallFrames;
        if (t < 0 || t >= knockdownBounceFrames) return 0f;
        return knockdownBounceHeight * (float)Math.sin(Math.PI * t / knockdownBounceFrames);
    }
    /** Longest wake-up: a longer GETUP clip plays faster to fit it. */
    int maxWakeupFrames = 24;

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
    /**
     * Atrito do pé deslizando no chão (coeficiente, vezes a gravidade = quanto freia, px/s²): o
     * corpo que vinha embalado e não dá mais passo (o golpe dado na corrida) desliza e para por
     * ele. Um número só, para todo deslize de pé no chão. 0,8 com a gravidade de 1650: a corrida
     * (620 px/s) para em 0,47 s, depois de 146 px.
     */
    float atritoDoChao = 0.8f;
    /**
     * Pulo normal: velocidade de lado no ar, para frente e para trás. Mais que a de andar: com o
     * mesmo tempo no ar (~42 quadros) vai ~280 para frente (passa do alcance dos golpes de chão
     * e cruza por cima de perto) e ~205 para trás. (O pulo mais rápido do KOF '97, 03/11, foi
     * testado e o jogador pediu de volta o original em 04/11: docs/balanco/comparacao-kof97.md.)
     */
    float jumpForwardSpeed = 400f;
    float jumpBackSpeed = 290f;
    /**
     * Pulo saído da corrida (dash jump): velocidade para frente no ar. Leva o embalo do dash e
     * vai ~400, bem mais que o pulo normal.
     */
    float dashJumpSpeed = 540f;
    /**
     * Pulo normal comprometido: nos primeiros quadros no ar a direção ainda pode ser escolhida
     * (o "pré-pulo" dos jogos de luta), depois fica fixa.
     */
    int jumpDirectionFrames = 3;
    /**
     * Super pulo: aceleração do drift (u/s²) até a velocidade de andar do lado segurado. Dá
     * para frear e virar no meio do super pulo, mas não de uma vez.
     */
    float superJumpDriftAccel = 900f;
    float backdashSpeed = 640f;
    int backdashFrames = 15;
    int backdashRecoveryFrames = 12; // extra grounded neutral frames before another hop
    /** Visual retreat hop is shared by all fighters; ground collision remains unchanged. */
    float backdashHopHeight = 40f;
    float backdashVisualLift(int framesLeft) {
        return backdashVisualLift(framesLeft, backdashFrames);
    }
    /** O pulinho de um backdash de {@code total} quadros (o do lutador). */
    float backdashVisualLift(int framesLeft, int total) {
        if (framesLeft <= 0 || framesLeft > total) return 0f;
        float fraction = (total - framesLeft) / (float)total;
        return backdashHopHeight * (float)Math.sin(Math.PI * fraction);
    }
    // 60 FPS normal jump: ~42 airborne frames, apex ~1.16x P01's standing hurtbox.
    // Sized for the 226-unit P01 (the 156-unit version used 1030 / 1.70 / 2.00, apex ~182):
    // speed and gravity scale by 226/156, so the cadence stays and only the height grows.
    // O original (03/11 testamos 20% e 15% mais rápido, 1876 / 3,848 / 4,527 e 1762 / 3,409 /
    // 4,010; o jogador pediu de volta este em 04/11).
    float jumpSpeed = 1492f;
    /**
     * Preparação do pulo do chão (jump squat): quadros agachando antes de sair do chão, sem
     * golpe, dash nem andar (a corrida segue no embalo). O pulo que cancela um golpe sai na hora
     * (as rotas de combo não mudam). 0 = pula no quadro do comando.
     */
    int jumpSquatFrames = 3;
    /**
     * Aterrissagem do pulo vazio (sem golpe no ar): quadros sem golpe, pulo, dash nem andar ao
     * tocar o chão (defender pode). O golpe aéreo que aterrissa continua livre na hora.
     */
    int jumpLandingFrames = 3;
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
    /**
     * Crossup: o golpe aéreo que pega também atrás do corpo, logo depois de passar por cima do
     * rival (o rival tem que defender para o outro lado). {@code crossUpBehind} px atrás da raiz.
     */
    String crossUpMove = "jM";
    float crossUpBehind = 48f;
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
    /**
     * Barra ganha batendo dentro de um combo (ComboSession.meterGain): cheia até o 3º acerto,
     * depois 10% a menos a cada acerto até 30%; com um Super já no combo, metade disso. O combo
     * longo começado sem barra pagava um ou dois Supers no meio dele mesmo (o especial dá 450).
     */
    int comboMeterFullHits = 3;
    int comboMeterStepPermille = 100;
    int comboMeterFloorPermille = 300;
    int comboMeterAfterSuperPermille = 500;
    static final int MAX_METER = 5 * METER_PER_BAR;
    static final int SUPER_COST = METER_PER_BAR;
    /** Ultra (↓ + SUPER): three bars. */
    static final int ULTRA_COST = 3 * METER_PER_BAR;
    /** Ultra SHINKAI: o mesmo botão do Ultra, só no SHINKAI e com as cinco barras (a barra cheia). */
    static final int ULTRA_SHINKAI_COST = 5 * METER_PER_BAR;

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
    /** Pancadas no chão (o Super do p13) cuja primeira errou: quadros até ele levantar e o golpe acabar. */
    int poundMissFrames = 36;
    /** O rebote (S1 do p14) que errou o primeiro chute: quadros até cair em guarda. */
    int reboundMissFrames = 20;
    /**
     * Repel (S2 da p15) que desfez um projétil: quadros até a aura apagar e ela ficar livre. Sem
     * isso, desfazer a bola custava o golpe inteiro (64 quadros), mais do que defender.
     */
    int repelShotFreeFrames = 6;
    /** O rebote (S1 do p14) que acertou: o pulo para trás (para cima, para trás e o peso). */
    float reboundHopSpeed = 950f;
    float reboundBackSpeed = 300f;
    float reboundGravityScale = 2.4f;
    /**
     * O rebote de vários chutes (S1 do p14) que acertou: quadros depois do último chute em que ele
     * ainda fica parado no ar (a estrela do vídeo) antes de pular para trás.
     */
    int reboundFadeFrames = 8;
    /** O empurrão dos corpos encostados anda no máximo isto por quadro (cada um). */
    float pushStepMax = 12f;
    /**
     * Empurrão no ar contra quem está no chão (como no mercado): o corpo no pulo encolhe as pernas
     * e fica mais estreito, então o pulo de perto passa por cima. Antes (corpo inteiro e largo) o
     * Ryūga não passava nem do Kōga.
     */
    int airPushHalfWidthPermille = 600;
    float airPushLegsTucked = 60f;
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
    float throwRange = 40f;
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
    /** Throw invulnerability right after hitstun or wake-up (a throw never continues a combo). */
    int throwProtectFrames = 6;
    /**
     * Depois do blockstun a proteção é menor (antes 6, igual à do hitstun): o "soco defendido,
     * agarra" (tick throw) entra logo depois do bloqueio, como nos jogos de luta.
     */
    int throwProtectAfterBlockFrames = 3;
    /**
     * Quanto o pedido de agarrão (L + M) espera para sair. Maior que o buffer dos golpes: L + M
     * apertado no fim do próprio golpe sai assim que ele acaba (antes sumia sem fazer nada).
     * O tech de quem é agarrado continua com {@code bufferFrames}.
     */
    int throwBufferFrames = 10;

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
     * Contra-golpe: o primeiro acerto de um combo em quem está no meio do próprio golpe (início,
     * ativo ou recuperação) tira mais e prende mais, como o Counter do SF6 e do COTW
     * (docs/balanco/chao.md). Super e Ultra não contam.
     */
    int counterHitDamagePermille = 1200;
    int counterHitLightFrames = 2;
    int counterHitFrames = 4;
    /**
     * Pulo para dentro (docs/balanco/tres-frentes.md): o golpe no ar (jL, jM, jH) contra quem está
     * no chão alcança isto (px) abaixo do membro desenhado. O desenho do chute do pulo fica na
     * altura do corpo, e o pulo sobe mais que o rival: o jH do Ryūga só encostava apertado entre
     * os quadros 26 e 30 do pulo. Com o alcance para baixo ele pega do alto do pulo até cair.
     */
    float jumpInReachDown = 70f;
    /** O golpe no ar defendido por quem está no chão: quadros a mais de defesa (quem pulou cai na frente). */
    int jumpInBlockstunBonus = 6;
    /**
     * O contra-golpe que leu o golpe (o S3 da Yin-lu, o do p11): o rival fica
     * atordoado pelo menos isto (quadros). Com o atordoamento comum (22–25) quem leu ficava só +5
     * a +8, a 180 px: não dava para chegar e emendar, e o rival voltava a atacar.
     */
    int counterAnswerStunFrames = 50;
    /**
     * Pausa no acerto por força, igual à do Dragon Ball FighterZ: 8/11/16 quadros (fraco/médio/
     * forte; Dustloop, tabela por nível de ataque). O golpe já traz 8/10/12; estes somam o resto
     * no acerto. Os dois lutadores congelam juntos (no projétil, só quem apanha).
     * docs/revisao-base/direcao.md, regra I1.
     */
    /** Golpe normal que acertou: quadros para voltar à guarda antes de a recuperação acabar. */
    int hitCutGuardFrames = 3;
    int lightHitHoldFrames = 0;
    int mediumHitHoldFrames = 1;
    int heavyHitHoldFrames = 4;
    /**
     * A defesa congela o mesmo que o golpe traz (sem o extra do acerto): no Guilty Gear a pausa da
     * defesa é a do acerto (Dustloop); a do SF6 não foi encontrada. Regra I3.
     */
    float normalBlockHitstopScale = 1f;
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
    /** Quem acerta o quique no chão no ar desce a esta velocidade (px/s), para emendar outro combo. */
    float groundBounceAttackerDrop = 1200f;
    /**
     * OTG (12/11, como o Marvel vs. Capcom): o golpe {@code otgMove} pega o rival que caiu dentro de
     * um combo (2 acertos ou mais) nos primeiros {@code otgWindowFrames} quadros deitado, uma vez
     * por combo, e o levanta {@code otgLift} (px/s) com {@code otgHitstunFrames} para continuar.
     * Enquanto ele ainda pode vir, o combo não acaba e o caído não rola nem levanta rápido.
     */
    String otgMove = "2M";
    int otgWindowFrames = 34;
    float otgLift = 620f;
    int otgHitstunFrames = 34;
    float otgPush = 30f;
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
    /**
     * Corrida de energia (correndo, M + H): por 1/4 de barra a corrida atravessa projéteis e fica
     * mais rápida por meio segundo. É a ferramenta de todos contra quem só solta bola, como o
     * Drive Rush do SF6 e o REV do Fatal Fury (docs/balanco/cpu-contra-bola.md).
     */
    int energyRunCost = METER_PER_BAR / 4;
    int energyRunFrames = 30;
    int energyRunSpeedPermille = 1250;
    float pushblockDistance = 190f;
    /** Blockstun left after a pushblock (at most). */
    int pushblockStunFrames = 8;
    // Guard break: cada defesa gasta a guarda (FighterState.guard, 0..GUARD_MAX) pelo blockstun
    // do golpe; zerou, a defesa quebra e quem defendia fica aberto. Ela volta sozinha um pouco
    // depois de parar de defender (e cheia depois de quebrar).
    static final int GUARD_MAX = 1000;
    /**
     * Vida de todos os lutadores, por mil da vida do pacote. 1400: a luta acabava rápido demais
     * (um combo com a parede e o ar tirava quase 30% da vida).
     */
    static final int LIFE_SCALE_PERMILLE = 1400;
    /**
     * Guarda gasta por quadro de blockstun do golpe defendido (um leve ~50, um forte ~85):
     * umas 20 defesas seguidas quebram, 7–14 s só defendendo (com 10 eram 3,5–7 s, bem mais
     * rápido que o mercado: docs/balanco/mercado.md).
     */
    int guardDrainPerBlockstunFrame = 5;
    /** Batida de um golpe de várias batidas antes da última: gasta só esta fração (por mil). */
    int guardHoldingDrainPermille = 500;
    /** Quadros sem defender antes de a guarda começar a voltar. */
    int guardRegenDelayFrames = 120;
    /** Guarda que volta por quadro (cheia em ~8 s, depois de 2 s sem defender). */
    int guardRegenPerFrame = 2;
    /** Atordoamento do guard break: aberto, sem defesa, tempo de entrar um combo. */
    int guardBreakStunFrames = 50;
    /** Congelamento do estalo da quebra (os dois, como um golpe forte). */
    int guardBreakHitstopFrames = 18;
    /** Abaixo disto (por mil da guarda) o aviso da guarda fica vermelho e pisca. */
    int guardWarningPermille = 300;

    // Chip: especial e Super defendidos ainda arranham. O arranhão vira vida vermelha
    // (recuperável fora de campo) e nunca derruba. A defesa perfeita não leva.
    /** Do dano da batida de um especial defendido (por mil). */
    int chipSpecialPermille = 125;
    /** Do dano da batida de um Super/Ultra defendido (por mil). */
    int chipSuperPermille = 250;

    // Defesa perfeita: apertar trás (a defesa) em cima da hora, até estes quadros antes do
    // contato. Não gasta guarda nem leva chip, cura um pouco, o blockstun encurta e a barra
    // sobe mais. Bater trás sem parar não vale: um aperto até este tanto de quadros depois do
    // anterior não abre a janela.
    int justDefenseFrames = 5;
    int justDefenseLockoutFrames = 20;
    /** Blockstun que sobra (por mil do do golpe): sai antes e pode punir. */
    int justDefenseBlockstunPermille = 400;
    /** Vida que volta (por mil da vida máxima): primeiro a vermelha. */
    int justDefenseHealPermille = 12;

    // Combo breaker: apanhando num combo que já entrou, L, M ou H quebra na hora (o "!" sobre a
    // cabeça avisa enquanto dá): o golpe não entra, quem batia é jogado para longe e fica atordoado
    // um instante, e quem quebrou corre sozinho até ele (o dash automático). O atordoamento curto
    // deixa a corrida chegar sem troco garantido (docs/balanco/ajustes.md). Custa barra (sem barra,
    // o "!" não aparece) e não quebra Super, Ultra nem agarrão.
    /** Acertos que o combo precisa ter para dar para quebrar (o 1º golpe não quebra). */
    int comboBreakerMinHits = 2;
    int comboBreakerCost = METER_PER_BAR;
    /**
     * Atordoamento de quem teve o combo quebrado, depois do estalo. Era 44 com a corrida sozinha:
     * dava um troco garantido de ~1300 (mais que o dano que a quebra evitava). 20 ainda deixava um L
     * de graça em quase toda quebra (o empurrão de 170 px cabe no alcance do L correndo). 4 (H1): ele
     * se recompõe antes de quem quebrou alcançar; o troco que sobra é oportunista (docs/balanco/ajustes.md).
     * 32 (pedido do jogador, 02/11: "o adversário não é atordoado e não dá para emendar"): a corrida
     * chega e o combo entra (troco garantido de 45–100% conforme o personagem; 40 e 48 dão o mesmo).
     */
    int comboBreakerStunFrames = 32;
    int comboBreakerHitstopFrames = 16;
    /** O estalo joga quem batia até esta folga do corpo de quem quebrou: longe, só correndo (era 170). */
    float comboBreakerPushGap = 230f;
    /** Quadros que ele desliza para trás, arrastando o pé (12: a mesma velocidade de antes, mais longe). */
    int comboBreakerPushFrames = 12;
    /** No ar, a velocidade com que ele é jogado para trás (era 260). */
    float comboBreakerAirPushSpeed = 320f;
    /**
     * Quem quebrou corre sozinho (sem → →) por até tantos quadros, até ficar a esta folga (ao
     * alcance do L). Segurar trás desiste. Estava desligado (0) desde a etapa 1; de volta, com
     * folga para o empurrão maior (era 40).
     */
    int comboBreakerChaseFrames = 48;
    float comboBreakerCloseGap = 30f;

    /**
     * DHC: o parceiro de Super corpo a corpo entra até onde o Super dele alcança o rival (sob o
     * clarão do Super), no máximo isto à frente de onde o titular estava; 0 = entra onde o titular
     * está (antes; o Super do parceiro errava o rival que o Super do titular tinha jogado longe).
     */
    float dhcMaxAdvance = 420f;
    /**
     * A entrada do DHC desliza (como a do assist, assistEnterFrames), sem pular de lugar: px por
     * quadro, no máximo dhcEnterMaxFrames quadros. Enquanto desliza, o Super dele e o rival esperam.
     */
    float dhcEnterSpeed = 36f;
    int dhcEnterMaxFrames = 12;

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

    // SHINKAI (botão SHINKAI ou SUPER + TAG; docs/shinkai.md): a barra de cada lutador enche
    // até SHINKAI_MAX; cheia, ele ativa uma vez por round e fica nele por estes quadros.
    static final int SHINKAI_MAX = 1000;
    int shinkaiFrames = 480;
    /** Both fighters freeze on activation; the opponent's stun waits too (combo extension). */
    int shinkaiFlashFrames = 48;
    /** Barra por dano: apanhando enche rápido (cheia com uns 3.300 de dano), batendo devagar. */
    int shinkaiPerDamageTakenPermille = 300;
    int shinkaiPerDamageDealtPermille = 100;
    /** Por golpe defendido e pelo combo breaker. */
    int shinkaiOnBlock = 12;
    int shinkaiOnBreaker = 150;
    /** Vida vermelha que volta por quadro para quem está no SHINKAI (todos os tipos). */
    int shinkaiRegenPerFrame = 12;
    /** Todos: os golpes batem mais forte, e defendido todo golpe arranha (vira vida vermelha). */
    int shinkaiDamagePermille = 1250;
    int shinkaiChipPermille = 150;
    /** Todos: o primeiro Super no SHINKAI sai sem barra e ainda mais forte. */
    int shinkaiSuperDamagePermille = 1200;
    /** POWER: o golpe que a couraça segura tira esta parte do dano. O H joga na parede e ele quica. */
    int shinkaiPowerArmorPermille = 700;
    /**
     * POWER na regra da reserva (vida nova): o golpe que a couraça segura tira esta parte (por
     * mil). Com a vida nova a couraça já sai de graça; 1000 = segura o golpe sem tirar menos dano.
     */
    int shinkaiPowerArmorReservaPermille = 1000;
    /** TECHNIQUE na regra da reserva: cada aparada devolve esta parte (por mil) da vida nova cheia. */
    int shinkaiParryReservaPermille = 150;
    /** SPEED: andar, dash e air dash mais rápidos, um air dash a mais. */
    float shinkaiSpeed = 1.3f;
    /**
     * Dragon Rush (todo SHINKAI): → → não corre, voa no rival (no chão e no ar), na direção dele
     * (sobe se ele está no ar). Encostou: pancada pesada que deixa emendar normais e especiais
     * (cancela na janela de acerto). Um por combo. No chão ou no ar, de um normal que encostou
     * também sai (cancela o normal). No SPEED, com o rival até {@link #shinkaiVanishRange}, → → no
     * chão continua sendo o sumiço.
     */
    int dragonRushStartup = 6;
    /** Quadros de voo (o golpe ativo): passou disso sem encostar, recupera no ar. */
    int dragonRushFlight = 26;
    int dragonRushRecovery = 16;
    /** Velocidade do voo (unidades por segundo): ~1800 = a tela em menos de meio segundo. */
    float dragonRushSpeed = 1800f;
    /** O impacto; o golpe final da sequência dá {@link #dragonRushFimDano} (juntos, os 320 de antes). */
    int dragonRushDamage = 200;
    /** Acerto: o rival fica preso 34 quadros (a recuperação de 16 sobra +18 sem cancelar). */
    int dragonRushHitstun = 34, dragonRushBlockstun = 14, dragonRushHitstop = 13;
    float dragonRushReach = 118f, dragonRushHeight = 170f;
    /**
     * Depois do impacto (acertou): a sequência. O rival fica preso na frente (no chão ou no ar)
     * por {@link #dragonRushSequencia} quadros; cada botão de ataque apertado nela é mais uma
     * pancada de {@link #dragonRushSocoDano} (até {@link #dragonRushSocoMax}, uma a cada
     * {@link #dragonRushSocoIntervalo} quadros). No fim, o golpe final pela direção segurada:
     * ↑ lança para o combo no ar, ← troca de lado (joga para o outro lado), nada (ou →) deixa
     * o rival preso na frente, como antes.
     */
    int dragonRushSequencia = 30;
    int dragonRushSocoDano = 36, dragonRushSocoMax = 5, dragonRushSocoIntervalo = 4, dragonRushSocoHitstop = 3;
    int dragonRushFimDano = 120, dragonRushFimHitstop = 9;
    /** O voo atravessa as bolas de energia (desfaz as comuns; o Super acerta). */
    boolean dragonRushAtravessaEnergia = true;
    /** O 2H no início ou ativo vence o voo (a cabeça dele está fora do alcance do Dragon Rush). */
    boolean dragonRushPerdeParaAntiAereo = true;
    /** SPEED, o sumiço (→ → com o rival até esta distância): some, aparece atrás dele. */
    float shinkaiVanishRange = 560f;
    int shinkaiVanishFrames = 10;
    float shinkaiVanishBehind = 120f;
    /** Cada sumiço gasta este tanto do tempo do SHINKAI. */
    int shinkaiVanishCost = 45;
    /** SPEED: cada golpe é repetido pela imagem residual, estes quadros depois, com este dano. */
    int shinkaiSpeedEchoDelay = 7;
    int shinkaiSpeedEchoDamagePermille = 500;
    /** ENERGY: barra de Super por quadro (uma barra em 4 s), o projétil maior e mais rápido. */
    int shinkaiEnergyMeterPerFrame = 4;
    float shinkaiEnergyProjectileSpeed = 1.3f;
    /** ENERGY: projéteis de energia ao mesmo tempo (o S sai sem esperar o anterior). */
    int shinkaiEnergyProjectiles = 5;
    /**
     * ENERGY: a recuperação do S (o projétil) no SHINKAI, em quadros. Fora dele o S tem a duração
     * de mercado (~46 quadros); no ENERGY ele volta a sair seguido, que é o tipo.
     */
    int shinkaiEnergyRecoveryFrames = 11;
    /** ENERGY: o H solta junto um mini projétil com esta parte do dano da energia. */
    int shinkaiEnergyShotDamagePermille = 450;
    /**
     * TECHNIQUE: o primeiro golpe que chega com ele livre é aparado sozinho; depois, o parry vale
     * se o → foi apertado até estes quadros antes do golpe chegar.
     */
    int shinkaiParryWindow = 10;
    /** Os dois congelam no parry; quem aparou sai livre e o golpe do outro continua. */
    int shinkaiParryFreeze = 16;
    /**
     * SHINKAI Clash (ShinkaiClash): ligar o SHINKAI com o rival no dele vira um cabo de guerra.
     * Quem vence acerta este dano e fica com o SHINKAI cheio; quem perde voa e perde o dele.
     */
    int shinkaiClashDamage = 2000;
    /**
     * O perdedor do Clash fica com esta parte (por mil) do SHINKAI que tinha (0 = perde tudo, como
     * era até 01/11; 1000 = o SHINKAI segue, pedido do jogador: "perder o Clash não deveria desativar
     * o SHINKAI"). No protótipo do Ryūga, o desgaste fica como estava. Perdendo tudo, quem vencia o
     * Clash vencia 70–78% das lutas (docs/balanco/shinkai-ryuga.md).
     */
    int shinkaiClashPerdedorPermille = 1000;
    /** A distância entre os dois no confronto. */
    float shinkaiClashGap = 300f;

    // PROTÓTIPO do SHINKAI individual (docs/balanco/shinkai-ryuga.md): só o Ryūga, tipo PERCEPCAO.
    // Números de teste, isolados aqui. Duração pelo desgaste do corpo (sobrecarga até SHINKAI_MAX),
    // não por um relógio fixo.
    /** O corpo aguenta este tanto de desgaste (sobrecarga); cheio, chega ao limite. */
    static final int RYUGA_SOBRECARGA_MAX = 100000;
    /** Desgaste por quadro só por estar no SHINKAI (parado, chega ao limite em ~12 s). */
    int ryugaDesgasteQuadro = 139;
    /** Desgaste por golpe que ele começa, por leitura, por esquiva e por ponto de dano levado. */
    int ryugaDesgasteGolpe = 1500;
    int ryugaDesgasteLeitura = 6000;
    int ryugaDesgasteEsquiva = 4000;
    int ryugaDesgastePorDano = 6;
    /** Percepção: a janela da defesa perfeita (5 fora) e o bloqueio contra bater sem parar (20 fora). */
    int ryugaPerfeitaJanela = 12;
    int ryugaPerfeitaBloqueio = 8;
    /** Leitura: quem atacou (no chão) fica desequilibrado este tanto; o Ryūga sai livre. */
    int ryugaLeituraAbre = 24;
    /**
     * (a) Leitura na defesa comum: toda defesa de golpe de perto no SHINKAI dele desequilibra quem
     * atacou por este tanto (0 = só a defesa perfeita lê). Ele sai livre na hora.
     */
    int ryugaLeituraLeveAbre = 14;
    /**
     * (c) Esquiva automática: no SHINKAI dele, o golpe de perto que ia acertar ele livre (parado ou
     * andando) passa direto; ele recua no backdash com o rastro dourado e o golpe preciso fica
     * pronto. Depois espera este tanto para esquivar sozinho de novo (0 = desligada).
     */
    int ryugaEsquivaAutoRecarga = 180;
    /** Quadros do rastro dourado depois da esquiva. */
    int ryugaRastroQuadros = 30;
    /** (b) No SHINKAI dele, todo contra-golpe (acertar quem estava apertando botão) é golpe preciso. */
    boolean ryugaContraPreciso = true;
    /** Golpe preciso: o primeiro golpe dele nestes quadros depois da leitura bate mais e atordoa mais. */
    int ryugaPrecisoJanela = 40;
    int ryugaPrecisoDanoPermille = 1300;
    int ryugaPrecisoHitstun = 12;
    /** No limite do corpo: estes quadros para escolher (SHINKAI de novo = Ruptura; nada = acaba). */
    int ryugaLimiteJanela = 60;
    /** Exaustão depois: acabou no limite / depois da Ruptura (andar e dash mais lentos, guarda gasta mais). */
    int ryugaExaustao = 90;
    int ryugaExaustaoRuptura = 240;
    float ryugaExaustoVelocidade = 0.7f;
    int ryugaExaustoGuardaPermille = 1500;
    /** Ruptura: dura isto, dói isto por quadro (vida de verdade, nunca a última), bate mais. */
    int ryugaRupturaQuadros = 300;
    int ryugaRupturaDor = 4;
    int ryugaRupturaDanoPermille = 1400;
    /**
     * O poder (S) do Ryūga no SHINKAI: dois disparos, a palma esquerda no tempo de sempre e a
     * direita {@link #ryugaDisparoDireita} quadros depois, cada um com {@link #ryugaDisparoPermille}
     * do dano (juntos, 120%: um a mais, mas cada um mais fraco). Desligado (0), um disparo só.
     */
    int ryugaDisparoDireita = 9;
    int ryugaDisparoPermille = 600;
    /**
     * Venceu o SHINKAI Clash: o corpo recupera esta parte (por mil) do fôlego total. 1000 zera o
     * desgaste (como o relógio dos outros, que volta cheio).
     */
    int ryugaClashFolegoPermille = 1000;
    /** Fim voluntário antes do limite: esta parte do fôlego que sobrou volta para a barra. */
    int ryugaDevolvePermille = 500;

    // SHINKAI POR RESERVA (pedido do jogador, 02/11; docs/balanco/shinkai-ryuga.md): no lugar do
    // desgaste, ao ligar ele ganha uma reserva de vida à parte. Os golpes que leva gastam a reserva
    // primeiro; as técnicas potencializadas também gastam. Sem cronômetro: parado, sem gastar e sem
    // apanhar, o SHINKAI fica. A reserva acabando abre a Ruptura (gasta a vida dele). false = o
    // desgaste do protótipo anterior (para comparar).
    boolean ryugaReserva = true;
    /**
     * A regra da reserva (vida nova, sem cronômetro, limite, Ruptura, transformação quebrada) vale
     * para todos os SHINKAIs, cada tipo com a sua característica (POWER, SPEED, ENERGY,
     * TECHNIQUE, PERCEPÇÃO). false = só o do Ryūga; os outros no relógio de sempre.
     */
    boolean shinkaiReservaTodos = true;
    /** SPEED na regra da reserva: o sumiço gasta isto da reserva (no relógio gastava tempo). */
    int shinkaiReservaSumico = 150;
    /** Depois da cena de ativação, este tanto de quadros em que apertar SHINKAI de novo não desliga. */
    int shinkaiGraca = 30;
    /**
     * Responder a transformação do outro com a sua: false (o padrão desde 03/11, pedido do
     * jogador: "o Clash está atrapalhando a fluidez") = os dois ficam transformados, com um choque
     * de auras que afasta os dois e a luta segue; true = o SHINKAI Clash de antes (o minigame).
     */
    boolean shinkaiClashMinigame = false;
    /**
     * Dois Dragon Rush que se encontram no voo (os dois no SHINKAI) abrem o SHINKAI Clash (o
     * minigame), mesmo com {@link #shinkaiClashMinigame} desligado na ativação.
     */
    boolean dragonRushClash = true;
    /** O choque de auras: quanto cada um é empurrado (vezes o empurrão do combo breaker). */
    float shinkaiChoqueEmpurrao = 0.7f;
    /**
     * Na regra da reserva, quem sai de campo (troca) guarda a transformação: a vida nova fica como
     * estava e ele volta transformado (pedido do jogador, 03/11). false = a troca apaga o SHINKAI.
     */
    boolean shinkaiGuardaNaTroca = true;
    /**
     * Ultra SHINKAI (pedido do jogador, 03/11): no SHINKAI e com as cinco barras o botão do Ultra solta
     * o Ultra SHINKAI. É o golpe final da transformação: bate {@link #ultraShinkaiDanoPermille} do
     * Ultra e, quando acaba (acertou, defendido ou errou), o SHINKAI termina.
     */
    boolean ultraShinkai = true;
    int ultraShinkaiDanoPermille = 2000;
    /**
     * Ultra SHINKAI do dragão (o Ryūga): depois da ativação ele fica no soco parado e um dragão azul do
     * tamanho da tela sai do punho e atravessa a arena; quem ele pega leva o combo enquanto o corpo
     * passa e voa no último golpe. Os lutadores com esse Ultra SHINKAI (ids, separados por vírgula).
     */
    String ultraDragao = "player_base";
    /** De onde o dragão parte: à frente do lutador (px), menos {@link #dragaoAtras}. */
    float dragaoOrigem = 60f;
    /** O dragão entra de fora da tela: a cabeça começa tanto atrás do punho (px). */
    float dragaoAtras = 950f;
    /** A cabeça anda isto por quadro (px). */
    float dragaoVelocidade = 24f;
    /** Do focinho à ponta do rabo (px): enquanto passa por quem pegou, os golpes caem. */
    float dragaoComprimento = 1360f;
    /**
     * A cena antes do dragão, como a transformação do SHINKAI: a luta para, a câmera fecha nele,
     * o corte com o retrato entra e ele arma o soco (quadros).
     */
    int dragaoPreparo = 80;
    /** Pega quem estiver até esta altura do chão (o super pulo passa por cima). */
    float dragaoAltura = 450f;
    /** Golpes pequenos, um a cada {@link #dragaoIntervalo} quadros, e o último que joga longe. */
    int dragaoAcertos = 7;
    int dragaoIntervalo = 8;
    /** Dano de base do combo (antes do {@link #ultraShinkaiDanoPermille}); o último leva 30%. */
    int dragaoDano = 4200;
    int dragaoFinalPermille = 300;
    /** O soco fica parado até o rabo sair da tela do outro lado; depois volta à guarda. */
    int dragaoQuadros = 170;
    /**
     * A reserva ao ligar, em milésimos da vida inteira DELE (cada um tem a sua: o Ryūga 14000).
     * 1000 = uma vida nova inteira (pedido do jogador: "o SHINKAI é um novo life", como a
     * transformação dos animes). Agora 500, meia vida (escolha do jogador, 03/11): no 2x2 o TIME OVER
     * do nível 8 caiu de 35% para 4% e o equilíbrio do elenco ficou igual (docs/balanco/ajustes.md).
     */
    int ryugaReservaPermille = 500;
    /** Reserva neste tanto (milésimos da reserva cheia) ou menos: no limite (dá para escolher a Ruptura). */
    int ryugaReservaLimitePermille = 150;
    /**
     * Um golpe (ou combo) que pegaria a reserva de cima do limite e a zeraria para nela: o limite
     * sempre aparece e dá a chance da Ruptura (0 = pode zerar de uma vez).
     */
    int ryugaReservaTravaPermille = 75;
    /**
     * A reserva zerou (a transformação quebrou): ele fica atordoado este tanto (no chão) e cansado
     * este tanto. Quebrar a transformação do outro é a virada da luta.
     */
    int ryugaQuebraAtordoa = 45;
    int ryugaQuebraExaustao = 300;
    /** O que cada técnica potencializada gasta: especial (S, S2–S4), Super, esquiva; o golpe comum, nada. */
    int ryugaReservaEspecial = 150;
    int ryugaReservaSuper = 400;
    int ryugaReservaEsquiva = 100;
    int ryugaReservaNormal = 0;
    /** Ruptura: a vida que ela não passa, em milésimos da vida inteira (a Ruptura acaba sozinha ao chegar aqui). */
    int ryugaRupturaPisoPermille = 100;

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
    /**
     * Assist de corpo a corpo: em vez de bater 70 px atrás do titular (onde só pega o rival
     * colado), corre até onde o golpe alcança o rival, no máximo isto à frente do titular, no mesmo
     * tempo de entrada dos outros (assistEnterFrames). O golpe que avança (investida) segue
     * avançando; o mergulho cai do alto.
     */
    float assistMeleeAhead = 220f;
    /** Altura de onde o assist de mergulho (o S1 do p14, só no ar) começa a descer. */
    float assistDiveHeight = 240f;
    /** After Assist → Tag: neither another assist nor a raw tag for this long. */
    int assistTagCooldownFrames = 300;
    /**
     * Assist → Tag pedido com o titular preso (apanhando, defendendo, caído) quando o golpe do
     * assist acaba: o parceiro espera até 0,6 s e a troca sai assim que o titular se soltar; senão
     * cai, avisada (docs/balanco/ajustes.md, etapa 3).
     */
    int assistTagBufferFrames = 36;

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
