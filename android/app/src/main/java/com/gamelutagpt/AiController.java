package com.gamelutagpt;

import java.util.List;
import java.util.Random;

/**
 * Turns {@link OpponentAi} decisions into engine input for one fighter, frame by frame.
 * The CPU presses buttons like a player: its attacks go through the same buffer and
 * state machine, so it cannot attack while busy or stunned. Pure Java.
 *
 * Tudo o que a CPU faz em resposta ao rival (defender, punir, tirar do ar, pular a onda,
 * confirmar um combo) passa pela {@link CpuPerception}: ela só vê a luta de
 * {@link CpuLevel#reactionFrames} quadros atrás, nunca antes da reação humana. O resto é hábito
 * (segurar a defesa colada) e plano (o caderno de combos), como um jogador.
 */
final class AiController {
    /** Pause after the CPU's own attack ends and after it is hit, in frames. */
    static final int ATTACK_COOLDOWN = 13;
    static final int HIT_COOLDOWN = 21;
    static final int ENERGY_COOLDOWN = 24;
    static final int SUPER_COOLDOWN = 27;

    // Batalha (IA ON): o kit inteiro, como um jogador usa. Chances por oportunidade (no nível 6;
    // os outros níveis escalam por CpuLevel.kitScale).
    /** Frames entre as decisões do kit no neutro (assist, troca, Ultra, Overdrive). */
    static final int KIT_DECISION_FRAMES = 15;
    static final double ASSIST_NEUTRAL_CHANCE = 0.18;
    /** Golpe que acertou: chama o assist para estender o combo. */
    static final double ASSIST_COMBO_CHANCE = 0.45;
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

    private static final int GUARD_NONE = 0, GUARD_HIGH = 1, GUARD_LOW = 2, GUARD_ANY = 3;

    private final OpponentAi ai;
    private final OpponentAi.Situation situation = new OpponentAi.Situation();
    /** Sorteios do kit: separados, para não mexer na sequência das decisões do OpponentAi. */
    private final Random kitRandom;
    /** Sorteios das reações e do combo (defender, punir, confirmar). */
    private final Random reactRandom;
    private final CpuPerception perception = new CpuPerception();
    private CpuLevel level = CpuLevel.of(CpuLevel.DEFAULT);
    private boolean fullKit;
    private int kitTimer;
    /** Golpe para o qual os planos de combo já foram sorteados, e em que frame dele. */
    private AttackDefinition plannedAttack;
    private int plannedAttackFrame;
    private boolean assistPlan, dhcPlan, overdrivePlan;
    private boolean wasBlocking, guardCancelPlan, pushblockPlan;
    private boolean wasAssistOut, convertPlan;
    private boolean enabled;
    private boolean movingForward;
    private boolean movingBack;
    private boolean dashing;
    private int cooldown;
    private AttackDefinition lastAttack;

    // Quadro (do motor) em que o golpe atual da CPU acertou pela primeira vez, ou -1.
    private int ownHitFrame = -1;

    // Caderno de combos: a rota em andamento e o passo dela.
    private List<String> route;
    private int step;
    private boolean awaitingStart;
    private int routeWait;
    private AttackDefinition lastSeenAttack;
    private int lastSeenAttackFrame;
    private boolean stepDecided, stepGo, commitDecided, commitGo;

    // Defesa.
    private int guardFrames;
    private boolean guardLow;
    /** A defesa atual é hábito (não viu ameaça): punir e tirar do ar passam na frente dela. */
    private boolean guardHabit;
    private int anticipationTimer;
    private int lastGuardSerial = -1, lastJumpSerial = -1, lastProjectileSerial = -1;
    private int lastOpponentAttackSerial = -1, lastOpponentAttackFrame = Integer.MIN_VALUE / 2;
    private CharacterDefinition threatFor;
    private float threatRange;

    // Cartas: punir e anti-aéreo.
    private int lastPunishSerial = -1, lastAntiAirSerial = -1;
    private boolean antiAirPlan;
    private boolean jumpOverProjectile;

    /** Por que a CPU apertou o último golpe ("neutro", "punir", "antiaereo"): para os testes. */
    String lastReason = "";

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
            movingBack = false;
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
        @Override public void moveBack() { movingBack = true; }
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
        // A semente das reações sai da IA: a mesma semente dá a mesma luta.
        this.reactRandom = new Random(ai.seed());
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

    /** Nível da CPU, de 1 a 8 (fora disso vira o mais próximo). */
    void setLevel(int number) {
        level = CpuLevel.of(number);
    }

    int level() { return level.number; }

    private void resetKit() {
        kitTimer = 0;
        plannedAttack = null;
        assistPlan = dhcPlan = overdrivePlan = false;
        wasBlocking = guardCancelPlan = pushblockPlan = false;
        wasAssistOut = convertPlan = false;
    }

    boolean enabled() {
        return enabled;
    }

    void setEnabled(boolean value) {
        enabled = value;
        ai.reset();
        perception.reset();
        movingForward = movingBack = dashing = false;
        cooldown = 0;
        lastAttack = null;
        clearRequests();
        resetKit();
        endRoute();
        guardFrames = 0;
        anticipationTimer = 0;
        lastGuardSerial = lastJumpSerial = lastProjectileSerial = -1;
        lastOpponentAttackSerial = -1;
        lastOpponentAttackFrame = Integer.MIN_VALUE / 2;
        lastPunishSerial = lastAntiAirSerial = -1;
        antiAirPlan = false;
        ownHitFrame = -1;
    }

    /** Fills the CPU input for the next engine step. */
    void fill(CombatEngine engine, int self, FighterInput out) {
        out.clear();
        if (!enabled) return;
        perception.record(engine, self);
        CombatFighter me = engine.fighter(self);
        CombatFighter target = engine.fighter(1 - self);
        CpuPerception.Snapshot seen = perception.seen(level.reactionFrames);
        if (me.status == CombatFighter.Status.THROWN) {
            if (techDelay == -2) techDelay = ai.techDelay(expectingThrow(engine, me, target, seen) ? level.techGuess : 0);
            if (techDelay >= 0 && techDelay-- == 0) out.grab = true;
            return;
        }
        techDelay = -2;
        if (me.frozen()) return;
        if (me.status == CombatFighter.Status.AIR_HITSTUN) {
            // Air tech once the hitstun is over, sometimes, toward a random side.
            if (airTechPlan == -2) airTechPlan = ai.airTechDirection(level.airTech);
            if (airTechPlan >= 0 && me.stunLeft == 0) {
                out.direction = MotionParser.relative(airTechPlan, me.facing);
                out.light = true;
            }
            endRoute();
            return;
        }
        airTechPlan = -2;
        if (me.status == CombatFighter.Status.KNOCKDOWN) {
            if (wakeupPlan == -2) wakeupPlan = ai.wakeupDirection();
            if (wakeupPlan > 0) out.direction = MotionParser.relative(wakeupPlan, me.facing);
            endRoute();
            return;
        }
        wakeupPlan = -2;

        if (me.attacking()) {
            if (me.outcome == CombatFighter.Outcome.HIT && ownHitFrame < 0) ownHitFrame = engine.frame();
        } else if (route == null) {
            ownHitFrame = -1;
        }

        if (fullKit && !target.ko() && fillKit(engine, me, target, seen, out)) return;

        if (cooldown > 0) cooldown--;
        if (me.framesSinceHit == 0) {
            cooldown = Math.max(cooldown, HIT_COOLDOWN);
            endRoute();
        }
        if (me.attacking()) {
            lastAttack = me.attack;
            runCombo(engine, me, out);
            return;
        }
        if (route != null && runComboFromNeutral(engine, me, target, out)) return;
        if (lastAttack != null) {
            cooldown = Math.max(cooldown,
                lastAttack.kind == AttackDefinition.Kind.SUPER ? SUPER_COOLDOWN
                    : lastAttack.kind == AttackDefinition.Kind.PROJECTILE ? ENERGY_COOLDOWN
                    : ATTACK_COOLDOWN);
            lastAttack = null;
        }
        if (target.ko()) {
            movingForward = movingBack = dashing = false;
            return;
        }
        if (defend(engine, me, target, seen, out)) return;
        if (!me.canAct()) {
            movingForward = movingBack = dashing = false;
            return;
        }
        if (seen == null || seen.ko) return;   // ainda não viu nada: espera
        if (punish(engine, me, target, seen, out)) return;
        if (antiAir(engine, me, target, seen, out)) return;
        if (habitGuard(me, target, seen, out)) return;

        situation.self = me.character();
        situation.target = target.body();
        situation.distance = Math.abs(seen.x - me.x);
        situation.verticalDistance = Math.abs(seen.y - me.y);
        situation.selfAirborne = !me.grounded;
        situation.targetGrounded = seen.grounded;
        situation.targetAlive = !seen.ko;
        situation.superReady = me.state.superMeter >= CombatConfig.SUPER_COST;
        situation.projectileActive = engine.energyProjectileAlive(self);
        situation.attackReady = cooldown <= 0;
        situation.throwReach = throwReach(engine, me, target);
        situation.level = level;
        situation.style = CpuStyle.of(me.character());
        clearRequests();
        ai.think(CombatConfig.DT, situation, actions);

        int forward = me.facing > 0 ? 1 : 5;
        if (movingForward) out.direction = forward;
        else if (movingBack) out.direction = me.facing > 0 ? 5 : 1;
        out.dash = dashing && movingForward;
        out.backdash = backdash;
        out.jump = jump && !superJump;
        out.superJump = jump && superJump;
        out.superAttack = superAttack;
        out.grab = grab;
        if (special != null) out.special = special;
        if (attack != null) {
            startRoute(me.character(), attack);
            press(attack, me, out);
            lastReason = "neutro";
        }
    }

    // ---------------------------------------------------------------- defesa

    /**
     * Defesa: reage ao que viu (golpe, pulo, projétil) com a chance e a altura do nível, segura
     * a defesa enquanto apanha nela e, colada no rival, às vezes espera defendendo agachada
     * (hábito, sem reagir a nada). Verdadeiro quando o quadro é da defesa.
     */
    private boolean defend(CombatEngine engine, CombatFighter me, CombatFighter target,
                           CpuPerception.Snapshot seen, FighterInput out) {
        boolean blockstun = me.status == CombatFighter.Status.BLOCKSTUN;
        if (!blockstun && !me.canAct()) {
            guardFrames = 0;
            return false;
        }
        CpuStyle style = CpuStyle.of(me.character());
        if (seen != null && !seen.ko) {
            float gap = Math.abs(seen.x - me.x);
            // (a defesa por hábito fica em habitGuard, depois de punir e tirar do ar)
            if (seen.threatening() && seen.attack.melee() && seen.attackSerial != lastGuardSerial &&
                gap <= seen.attack.reach + me.body().halfWidth + 60f) {
                lastGuardSerial = seen.attackSerial;
                decideGuard(!seen.grounded ? GUARD_HIGH : seen.attack.low ? GUARD_LOW : GUARD_ANY, 24);
            } else if (!seen.grounded && seen.jumpSerial != lastJumpSerial && gap < 340f && !antiAirPlan &&
                seen.status == CombatFighter.Status.NEUTRAL) {
                // Pulo vindo e ela não vai tirar do ar: defende em pé (pulo só se defende em pé).
                lastJumpSerial = seen.jumpSerial;
                decideGuard(GUARD_HIGH, 40);
            }
            if (seen.projectileGap != Float.MAX_VALUE && seen.projectileSerial != lastProjectileSerial) {
                float speed = Math.max(1f, seen.projectileSpeed * CombatConfig.DT);
                float now = seen.projectileGap - speed * level.reactionFrames;
                if (now < 420f) {
                    lastProjectileSerial = seen.projectileSerial;
                    if (!blockstun && me.grounded && !seen.projectileSuper && now > 120f &&
                        roll(Math.min(0.9, level.jumpProjectile * style.jumping))) {
                        // Pula por cima, para frente.
                        jumpOverProjectile = true;
                    } else {
                        decideGuard(seen.projectileLow ? GUARD_LOW : GUARD_ANY, Math.round(Math.max(0f, now) / speed) + 20);
                    }
                }
            }
        }
        if (jumpOverProjectile && !blockstun && me.grounded) {
            jumpOverProjectile = false;
            out.jump = true;
            out.direction = me.facing > 0 ? 8 : 6;
            return true;
        }
        jumpOverProjectile = false;
        if (blockstun) {
            guardFrames = Math.max(guardFrames, 8);
            guardHabit = false;
        }
        if (seen != null && seen.attacking() && seen.attackSerial != lastOpponentAttackSerial) {
            lastOpponentAttackSerial = seen.attackSerial;
            lastOpponentAttackFrame = seen.frame;
        }
        return !guardHabit && holdGuard(me, out);
    }

    /**
     * Hábito: colada no rival, espera defendendo agachada (segura tudo no chão), sem reagir a
     * nada. Forte quando ela acabou de ver o rival atacar (respeita a pressão); fraco contra
     * quem está parado.
     */
    private boolean habitGuard(CombatFighter me, CombatFighter target, CpuPerception.Snapshot seen, FighterInput out) {
        if (!me.grounded) return false;
        if (guardFrames > 0) return holdGuard(me, out);
        if (anticipationTimer > 0) {
            anticipationTimer--;
            return false;
        }
        if (Math.abs(seen.x - me.x) >= threatRange(me, target)) return false;
        boolean pressured = seen.frame - lastOpponentAttackFrame < 60;
        double p = level.guardAnticipation * CpuStyle.of(me.character()).patience;
        p = pressured ? p + 0.25 * level.reactGuard : p * 0.35;
        if (roll(Math.min(0.95, p))) {
            guardFrames = 18 + reactRandom.nextInt(18);
            guardLow = true;
            guardHabit = true;
            return holdGuard(me, out);
        }
        anticipationTimer = 12;   // decidiu agir: espera um pouco antes de pensar de novo
        return false;
    }

    private boolean holdGuard(CombatFighter me, FighterInput out) {
        if (guardFrames <= 0) return false;
        guardFrames--;
        movingForward = movingBack = dashing = false;
        int back = me.facing > 0 ? 5 : 1;
        int downBack = me.facing > 0 ? 4 : 2;
        out.direction = guardLow ? downBack : back;
        return true;
    }

    /** Decide se defende o que viu e em que altura (errar a altura também é humano). */
    private void decideGuard(int required, int frames) {
        if (!roll(level.reactGuard)) return;
        guardHabit = false;
        boolean right = roll(level.guardHeight);
        if (required == GUARD_HIGH) guardLow = !right;
        else if (required == GUARD_LOW) guardLow = right;
        else guardLow = true;   // agachada segura qualquer golpe no chão
        guardFrames = Math.max(guardFrames, frames);
    }

    /** Até onde o rival alcança com um golpe no chão (mais uma folga): o "colado" da defesa. */
    private float threatRange(CombatFighter me, CombatFighter target) {
        CharacterDefinition c = target.character();
        if (c != threatFor) {
            threatFor = c;
            threatRange = 0f;
            for (String b : OpponentAi.GROUND_ATTACKS) {
                CharacterDefinition.Move move = c.move(b, false);
                if (move != null) threatRange = Math.max(threatRange, CombatRules.maxCenterDistance(move, me.body()));
            }
            threatRange += 40f;
        }
        return threatRange;
    }

    /**
     * O tech é palpite (a janela é menor que a reação humana): a CPU só aperta se já esperava o
     * agarrão, isto é, se o rival estava colado no que ela viu antes de ele agarrar.
     */
    private boolean expectingThrow(CombatEngine engine, CombatFighter me, CombatFighter target,
                                   CpuPerception.Snapshot seen) {
        // No começo da luta ela ainda não tem a reação inteira de história: vale o quadro mais
        // antigo que viu (é de antes do agarrão, e só a distância conta aqui).
        CpuPerception.Snapshot s = seen != null ? seen : perception.oldest();
        return s != null && s.grounded && Math.abs(s.x - me.x) <= throwReach(engine, me, target) + 40f;
    }

    private static float throwReach(CombatEngine engine, CombatFighter me, CombatFighter target) {
        return me.body().halfWidth + target.body().halfWidth + engine.config.throwRange - 4f;
    }

    // ---------------------------------------------------------------- cartas

    /**
     * Viu o rival errar um golpe ao alcance: se a recuperação que ela calcula que sobra dá para o
     * golpe de abertura do caderno, pune com o combo.
     */
    private boolean punish(CombatEngine engine, CombatFighter me, CombatFighter target,
                           CpuPerception.Snapshot seen, FighterInput out) {
        if (!me.grounded || !seen.grounded || !seen.attacking() || seen.threatening() ||
            seen.outcome == CombatFighter.Outcome.HIT || seen.attackSerial == lastPunishSerial) return false;
        lastPunishSerial = seen.attackSerial;
        if (!roll(level.punish)) return false;
        int left = seen.recoveryLeft() - level.reactionFrames;
        float gap = Math.abs(seen.x - me.x);
        CharacterDefinition c = me.character();
        for (String starter : new String[] {"M", "L", "2L"}) {
            AttackDefinition a = c.attack(starter);
            if (a == null || a.startupFrames > left || !CombatRules.reaches(c, starter, false, gap, target.body())) continue;
            startRoute(c, starter);
            press(starter, me, out);
            lastReason = "punir";
            return true;
        }
        return false;
    }

    /**
     * Viu o rival pular para cima dela: com a chance do nível, espera ele chegar (pela trajetória
     * que viu) e sobe com o 2H, que lança e começa o combo aéreo.
     */
    private boolean antiAir(CombatEngine engine, CombatFighter me, CombatFighter target,
                            CpuPerception.Snapshot seen, FighterInput out) {
        if (!me.grounded) return false;
        if (seen.grounded || seen.status != CombatFighter.Status.NEUTRAL && !seen.attacking()) {
            antiAirPlan = false;
            return false;
        }
        CharacterDefinition c = me.character();
        if (c.attack("2H") == null) return false;
        if (seen.jumpSerial != lastAntiAirSerial) {
            lastAntiAirSerial = seen.jumpSerial;
            boolean coming = (me.x - seen.x) * seen.vx > 0f || Math.abs(seen.x - me.x) < 200f;
            antiAirPlan = coming && roll(level.antiAir);
        }
        if (!antiAirPlan) return false;
        // Onde ele está agora pelo que ela viu: a trajetória do pulo é previsível.
        float t = level.reactionFrames * CombatConfig.DT;
        float x = seen.x + seen.vx * t;
        float y = seen.y + seen.vy * t + 0.5f * engine.config.gravity * t * t;
        float gap = Math.abs(x - me.x);
        float height = Arena.GROUND_Y - y;
        if (height > 240f || height < 0f || !CombatRules.reaches(c, "2H", false, gap, target.body())) return false;
        antiAirPlan = false;
        startRoute(c, "2H");
        press("2H", me, out);
        lastReason = "antiaereo";
        return true;
    }

    // ---------------------------------------------------------------- caderno de combos

    /** Começa a rota do caderno no passo {@code first} (ou só ele, se não estiver na rota). */
    private void startRoute(CharacterDefinition c, String first) {
        List<String> book = ComboBook.route(c);
        int index = book.indexOf(first);
        if (index >= 0) {
            route = book;
            step = index;
        } else {
            // Iniciador fora da rota (2L, 2M...): entra nela no primeiro passo que ele cancela.
            route = java.util.Collections.singletonList(first);
            AttackDefinition a = c.attack(first);
            for (int k = 1; a != null && k < book.size(); k++) {
                String b = book.get(k);
                if (b.startsWith("j") || ComboBook.JUMP.equals(b) || ComboBook.AIR_JUMP.equals(b)) break;
                if (a.cancelsInto(b)) {
                    java.util.List<String> joined = new java.util.ArrayList<>();
                    joined.add(first);
                    joined.addAll(book.subList(k, book.size()));
                    route = joined;
                    break;
                }
            }
            step = 0;
        }
        awaitingStart = true;
        routeWait = 0;
        lastSeenAttack = null;
        stepDecided = commitDecided = false;
        ownHitFrame = -1;
    }

    private void endRoute() {
        route = null;
        step = 0;
        awaitingStart = false;
        lastSeenAttack = null;
    }

    /** O golpe na tela é o passo {@code name} do caderno. */
    private static boolean matches(AttackDefinition attack, String name) {
        return attack != null && attack.id.equals(name);
    }

    /**
     * Combo em andamento: no golpe atual, aperta o próximo passo. Golpes comuns seguem às cegas
     * (no acerto ou na defesa, como um jogador encadeia); lançador, especial, Super e o super pulo
     * só depois de ver que acertou, com a reação do nível.
     */
    private void runCombo(CombatEngine engine, CombatFighter me, FighterInput out) {
        runCombo(engine, me, engine.fighter(1 - me.index), out);
    }

    private void runCombo(CombatEngine engine, CombatFighter me, CombatFighter target, FighterInput out) {
        if (route == null) {
            // Golpe fora do caderno (S, Super, kit): só persegue o lançador que viu acertar.
            if (me.attack.launch == AttackDefinition.Launch.LAUNCH && hitSeen(engine) && roll(level.confirm)) {
                startRoute(me.character(), me.attack.id);
                awaitingStart = false;
                lastSeenAttack = me.attack;
                lastSeenAttackFrame = me.attackFrame;
            }
            return;
        }
        boolean fresh = me.attack != lastSeenAttack || me.attackFrame < lastSeenAttackFrame;
        lastSeenAttack = me.attack;
        lastSeenAttackFrame = me.attackFrame;
        if (fresh) {
            if (awaitingStart && matches(me.attack, route.get(step))) {
                awaitingStart = false;
            } else if (!awaitingStart && step + 1 < route.size() && matches(me.attack, route.get(step + 1))) {
                step++;
                stepDecided = commitDecided = false;
            } else if (!(awaitingStart || matches(me.attack, route.get(step)) && step == 0)) {
                endRoute();
                return;
            }
        }
        if (awaitingStart || step + 1 >= route.size()) return;
        String next = route.get(step + 1);
        if (me.outcome == CombatFighter.Outcome.NONE) return;   // ainda não encostou
        if (!stepDecided) {
            stepDecided = true;
            stepGo = step + 1 < level.comboDepth && !roll(level.drop);
        }
        if (!stepGo) return;
        if (ComboBook.commits(next) || ComboBook.AIR_JUMP.equals(next)) {
            if (!hitSeen(engine)) return;
            if (!commitDecided) {
                commitDecided = true;
                commitGo = roll(level.confirm);
            }
            if (!commitGo) return;
        }
        if ("SUPER".equals(next) && me.state.superMeter < CombatConfig.SUPER_COST) {
            // Sem barra: fecha com o especial, se o golpe deixar.
            if (me.attack.cancelsInto("S") && me.state.profile.energy != null && !engine.energyProjectileAlive(me.index)) {
                next = "S";
            } else {
                return;
            }
        }
        if (ComboBook.JUMP.equals(next)) return;   // o super pulo sai do neutro, depois do lançador
        if (me.grounded && !next.startsWith("j") && !"S".equals(next) && !"SUPER".equals(next) &&
            !CombatRules.reaches(me.character(), next, false, Math.abs(target.x - me.x), target.body())) {
            // O empurrão do golpe tirou o próximo do alcance: fecha com o que ainda alcança.
            next = closer(engine, me, target);
            if (next == null) return;
        }
        if (ComboBook.AIR_JUMP.equals(next)) {
            // ↑ no quadro em que a janela está aberta (a entrada do ↑ é o que conta).
            if (CancelSystem.windowOpen(me)) {
                out.direction = 7;
                step++;
                stepDecided = commitDecided = false;
            }
            return;
        }
        press(next, me, out);
    }

    /**
     * Passos do combo fora de um golpe: o super pulo atrás do lançador e o primeiro golpe aéreo
     * depois dele (ou depois do pulo no ar). Verdadeiro enquanto o combo segue.
     */
    private boolean runComboFromNeutral(CombatEngine engine, CombatFighter me, CombatFighter target, FighterInput out) {
        if (++routeWait > 45) {
            endRoute();
            return false;
        }
        if (awaitingStart) return routeWait <= 8;   // o golpe apertado ainda vai sair
        if (step + 1 >= route.size()) {
            endRoute();
            return false;
        }
        String next = route.get(step + 1);
        String current = route.get(step);
        if (ComboBook.JUMP.equals(next)) {
            // O lançador acabou: super pulo atrás dele, se viu que acertou.
            if (!me.grounded) {
                step++;
                routeWait = 0;
                return true;
            }
            if (me.launcherChase <= 0 || !hitSeen(engine)) {
                endRoute();
                return false;
            }
            out.superJump = true;
            return true;
        }
        if (ComboBook.JUMP.equals(current) || ComboBook.AIR_JUMP.equals(current)) {
            if (me.grounded) {
                endRoute();
                return false;
            }
            // No ar: o primeiro golpe aéreo quando o rival estiver ao alcance.
            float gap = Math.abs(target.x - me.x);
            if (Math.abs(target.y - me.y) <= 115f &&
                CombatRules.reaches(me.character(), next.startsWith("j") ? next.substring(1) : next, true, gap, target.body())) {
                awaitingStart = true;
                step++;
                routeWait = 0;
                press(next, me, out);
            } else if (gap > 80f) {
                out.direction = target.x > me.x ? 1 : 5;   // vai atrás dele no ar
            }
            return true;
        }
        endRoute();
        return false;
    }

    /**
     * O próximo passo não alcança mais: troca o resto da rota por um fechamento que o golpe atual
     * cancela e que alcança daqui (H, ou o S1, que voa). Null se não houver.
     */
    private String closer(CombatEngine engine, CombatFighter me, CombatFighter target) {
        CharacterDefinition c = me.character();
        float gap = Math.abs(target.x - me.x);
        for (String option : new String[] {"2H", "H", "S"}) {
            if (!me.attack.cancelsInto(option) || option.equals(route.get(step + 1))) continue;
            AttackDefinition a = c.attack(option);
            if (a == null || a.maxUsesPerChain > 0 && me.chain.contains(option)) continue;
            boolean reaches = "S".equals(option)
                ? me.state.profile.energy != null && !engine.energyProjectileAlive(me.index)
                : CombatRules.reaches(c, option, false, gap, target.body());
            if (!reaches) continue;
            java.util.List<String> rest = new java.util.ArrayList<>(route.subList(0, step + 1));
            rest.add(option);
            List<String> book = ComboBook.route(c);
            int at = book.indexOf(option);
            if (at >= 0 && !"S".equals(option)) rest.addAll(book.subList(at + 1, book.size()));
            route = rest;
            return option;
        }
        return null;
    }

    /** Viu (com a reação do nível) que o golpe atual acertou. */
    private boolean hitSeen(CombatEngine engine) {
        return ownHitFrame >= 0 && engine.frame() - ownHitFrame >= level.reactionFrames;
    }

    /** Aperta o passo {@code step}: golpe ("2H" agachado, "j2H" ↓ no ar), S1 ou Super. */
    private static void press(String step, CombatFighter me, FighterInput out) {
        if ("SUPER".equals(step)) {
            out.superAttack = true;
            return;
        }
        if ("S".equals(step)) {
            out.special = "M";
            return;
        }
        // Agachado com ↘ (para baixo e para frente): conta como agachado e não fecha comando de
        // especial com a defesa que veio antes (← ↙ ↓ + H vira o S3).
        if (ComboBook.crouching(step)) out.direction = me.facing > 0 ? 2 : 4;
        char button = ComboBook.button(step);
        out.light = button == 'L';
        out.medium = button == 'M';
        out.heavy = button == 'H';
    }

    // ---------------------------------------------------------------- kit da batalha

    /**
     * O kit da batalha neste frame. Verdadeiro quando apertou algo que pede o frame só para
     * si (a troca, o Ultra e o Overdrive no neutro): a decisão normal fica para depois.
     */
    private boolean fillKit(CombatEngine engine, CombatFighter me, CombatFighter target,
                            CpuPerception.Snapshot seen, FighterInput out) {
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

        // O próprio golpe: um sorteio por golpe novo, usado quando ela vê que ele acertou.
        if (me.attacking()) {
            if (me.attack != plannedAttack || me.attackFrame < plannedAttackFrame) {
                plannedAttack = me.attack;
                assistPlan = chance(ASSIST_COMBO_CHANCE);
                dhcPlan = chance(DHC_CHANCE);
                overdrivePlan = chance(OVERDRIVE_COMBO_CHANCE);
            }
            plannedAttackFrame = me.attackFrame;
            if (teams.dhcReady(self, me)) {
                if (dhcPlan) out.assist = true;
            } else if (hitSeen(engine) && me.attack.kind != AttackDefinition.Kind.SUPER) {
                if (overdrivePlan && teams.canOverdrive(me)) {
                    out.overdrive = true;
                    overdrivePlan = false;
                } else if (assistPlan && teams.canAssist(self, me)) {
                    out.assist = true;
                    assistPlan = false;
                }
            }
            return false;
        }
        plannedAttack = null;

        // Neutro: de tempos em tempos olha o que o time e a barra permitem.
        if (!me.canAct() || ++kitTimer < KIT_DECISION_FRAMES) return false;
        kitTimer = 0;
        float distance = Math.abs((seen != null ? seen.x : target.x) - me.x);
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

    /** Sorteio do kit, na escala do nível (o nível 1 não usa o kit). */
    private boolean chance(double p) {
        return kitRandom.nextDouble() < p * level.kitScale;
    }

    private boolean roll(double p) {
        return reactRandom.nextDouble() < p;
    }

    private void clearRequests() {
        attack = null;
        special = null;
        superAttack = backdash = jump = superJump = grab = false;
    }
}
