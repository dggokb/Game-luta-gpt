package com.gamelutagpt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Complete combat definition of one move, in simulation frames ({@link CombatConfig#FPS}).
 * Frame indices count from the first frame of the move (0). Immutable; {@link Builder#build}
 * rejects data that the engine could not honour.
 */
final class AttackDefinition {
    /** What the move does to a defender it hits. */
    enum Launch { NONE, KNOCKDOWN, LAUNCH, SLAM, WALL_BOUNCE, GROUND_BOUNCE }

    /** Button class; also the default buffer priority (SUPER first). */
    enum Strength { LIGHT, MEDIUM, HEAVY, SPECIAL, SUPER }

    /** NORMAL hits with hitboxes; PROJECTILE/SUPER spawn a projectile on the first active frame. */
    enum Kind { NORMAL, PROJECTILE, SUPER }

    /** Pseudo cancel target: jump (super jump while a launcher chase is open). */
    static final String JUMP = "JUMP";

    /** Inclusive frame range [start, end]. */
    static final class Window {
        final int start, end;
        Window(int start, int end) {
            this.start = start;
            this.end = end;
        }
        boolean contains(int frame) { return frame >= start && frame <= end; }
        @Override public String toString() { return "[" + start + "," + end + "]"; }
    }

    /**
     * Box relative to the fighter root, active on frames [firstFrame, lastFrame]. x grows
     * forward from the root (mirrored by facing), y grows up from the ground.
     */
    static final class Box {
        final int firstFrame, lastFrame;
        final float x0, x1, y0, y1;
        Box(int firstFrame, int lastFrame, float x0, float x1, float y0, float y1) {
            this.firstFrame = firstFrame;
            this.lastFrame = lastFrame;
            this.x0 = x0;
            this.x1 = x1;
            this.y0 = y0;
            this.y1 = y1;
        }
        boolean activeAt(int frame) { return frame >= firstFrame && frame <= lastFrame; }
    }

    /** What a counter stance catches, and what an evasion slips under. */
    enum Reads { NONE, HIGH, LOW, ANY }

    /**
     * Character mechanics of a move (docs/personagens/mecanicas.md), all optional. Frame
     * ranges count from the first frame of the move, like the hitboxes.
     */
    static final class Mechanics {
        /** Super armor: absorbs this many hits in [armorStart, armorEnd] (damage, no stun). */
        int armorHits, armorStart, armorEnd;
        /** Counter stance: a melee strike it reads in the window is answered by this move. */
        Reads counter = Reads.NONE;
        int counterStart, counterEnd;
        /**
         * Repel (o S2 da p15): a postura só bate quando repele (sem nada chegando, a janela passa
         * e é só a recuperação, aberta), e o projétil que chega na janela se desfaz.
         */
        boolean repel;
        /**
         * Agarrão de especial (o S3 do p13): o primeiro quadro ativo pega quem está até
         * {@link #grabRange} à frente e agarrável (as regras do agarrão); pegou, as pancadas
         * seguintes acertam sem caixa nem defesa; errou, o golpe sai no vazio.
         */
        boolean grab;
        float grabRange;
        /**
         * Salto do golpe (o S4 do p13, que cai com o soco): o desenho sobe em arco até
         * {@link #hopHeight} entre os quadros {@link #hopStart} e {@link #hopEnd} (o vídeo é de
         * câmera que acompanha); o corpo segue no chão e o avanço leva ele para a frente.
         */
        float hopHeight;
        int hopStart, hopEnd;
        /** Evasion: HIGH slips standing/air strikes and projectiles, ANY everything. */
        Reads evade = Reads.NONE;
        int evadeStart, evadeEnd;
        /** Travel (world units per second) in [advanceStart, advanceEnd]; negative goes back. */
        float advanceSpeed;
        int advanceStart, advanceEnd;
        /** The body passes through the opponent while advancing (cross-up). */
        boolean passThrough;
        /** Frame on which the fighter reappears behind the opponent, or -1. */
        int teleportFrame = -1;
        /** On hit the defender is pulled this far toward the attacker instead of pushed. */
        float pull;
        /** On hit: this much damage over poisonFrames (never the last point of life). */
        int poisonDamage, poisonFrames;
        /** Life the attacker pays when the move starts (never the last point). */
        int lifeCost;
        /** Charge: holding the button stops the move just before it fires, up to this many frames. */
        int chargeFrames;
        /** Extra damage of a full charge, in permille. */
        int chargeBonusPermille;
        /** Echo: the move turns on a shadow that repeats the next moves, for this many frames. */
        int echoFrames;
        /** Delay of the shadow's copy and its share of the damage (permille). */
        int echoDelay, echoDamagePermille;
        /**
         * Mergulho (golpe no ar): parado no ar durante a preparação, depois desce na diagonal,
         * nestas velocidades (frente e para baixo), até o chão.
         */
        float diveForward, diveDescent;

        Mechanics armor(int hits, int start, int end) { armorHits = hits; armorStart = start; armorEnd = end; return this; }
        Mechanics counter(Reads reads, int start, int end) { counter = reads; counterStart = start; counterEnd = end; return this; }
        Mechanics repel() { repel = true; return this; }
        Mechanics grab(float range) { grab = true; grabRange = range; return this; }
        Mechanics hop(float height, int start, int end) { hopHeight = height; hopStart = start; hopEnd = end; return this; }
        /** Altura do desenho no quadro {@code frame} do golpe (arco: sobe e desce no trecho). */
        float hopLift(int frame) {
            if (hopHeight <= 0f || frame <= hopStart || frame >= hopEnd) return 0f;
            float u = (frame - hopStart) / (float)(hopEnd - hopStart);
            return 4f * hopHeight * u * (1f - u);
        }
        Mechanics evade(Reads reads, int start, int end) { evade = reads; evadeStart = start; evadeEnd = end; return this; }
        Mechanics advance(float speed, int start, int end, boolean through) {
            advanceSpeed = speed; advanceStart = start; advanceEnd = end; passThrough = through; return this;
        }
        Mechanics teleport(int frame) { teleportFrame = frame; return this; }
        Mechanics pull(float distance) { pull = distance; return this; }
        Mechanics poison(int damage, int frames) { poisonDamage = damage; poisonFrames = frames; return this; }
        Mechanics lifeCost(int value) { lifeCost = value; return this; }
        Mechanics charge(int frames, int bonusPermille) { chargeFrames = frames; chargeBonusPermille = bonusPermille; return this; }
        Mechanics dive(float forward, float descent) { diveForward = forward; diveDescent = descent; return this; }
        boolean dives() { return diveDescent > 0f; }
        Mechanics echo(int frames, int delay, int damagePermille) {
            echoFrames = frames; echoDelay = delay; echoDamagePermille = damagePermille; return this;
        }
        /** Frame on which a charge holds the move (two frames before it fires). */
        int chargeHoldFrame(int startupFrames) { return Math.max(0, startupFrames - 2); }

        boolean armorAt(int frame) { return armorHits > 0 && frame >= armorStart && frame <= armorEnd; }
        boolean counterAt(int frame) { return counter != Reads.NONE && frame >= counterStart && frame <= counterEnd; }
        boolean evadeAt(int frame) { return evade != Reads.NONE && frame >= evadeStart && frame <= evadeEnd; }
        boolean advanceAt(int frame) { return advanceSpeed != 0f && frame >= advanceStart && frame <= advanceEnd; }
    }

    /** Mechanics with nothing in them (moves without a character mechanic). */
    static final Mechanics NO_MECHANICS = new Mechanics();

    final String id;
    final Kind kind;
    final Strength strength;
    final int damage;
    final int startupFrames, activeFrames, recoveryFrames, totalFrames;
    /** Frames before and after the active window in which a defender already guards. */
    private final int threatLead;
    final int hitstunFrames, blockstunFrames, hitstopFrames;
    /** Cancel windows per outcome of the move; null when that outcome cannot cancel. */
    final Window hitWindow, blockWindow, whiffWindow;
    private final List<String> cancelInto;
    /** Scale applied to the rest of a combo this move starts (1000 = none). */
    final int prorationPermille;
    final Launch launch;
    /** Horizontal travel given to a defender left airborne or knocked down. */
    final float knockback;
    final float pushbackOnHit, pushbackOnBlock;
    final int juggleCost, maxHits, maxUsesPerCombo;
    /**
     * Pausa (hitstop) de cada pancada de um golpe de várias, em quadros; 0 = a de sempre (as do
     * meio mais curtas, para o golpe manter o ritmo). O Super da p16 segura a torta e a marreta.
     */
    final int strikeStop;
    /**
     * Pausa da última pancada de um golpe com {@link #strikeStop}, em quadros; 0 = a mesma das
     * outras. O chute que fecha o Super da p15 segura mais e ganha o impact frame forte.
     */
    final int finalStop;
    /**
     * Onde cada pancada deixa o rival (px à frente de quem bate), ou null: a torta põe o rosto
     * no creme e a marreta deixa o rival deitado debaixo dela até o golpe acabar (o Super da p16).
     */
    final float[] strikeHold;
    /**
     * Usos do golpe numa mesma corrente de cancelamentos (desde o último golpe que saiu do
     * neutro), acertando ou na defesa; 0 = livre. Diferente de maxUsesPerCombo, zera depois de
     * uma ligação: L > M > H, parede, dash, L > M vale.
     */
    final int maxUsesPerChain;
    /** Must be guarded crouching. Airborne attackers are always overheads. */
    final boolean low;
    /** Golpe alto de chão: só se defende em pé (pega quem defende agachado). */
    final boolean overhead;
    /** No acerto, o rival sai girando no ar e cai deitado (o giro do punish counter). */
    final boolean spin;
    final float reach, hitHeight;
    /** Base of the default strike box: hitHeight minus the slack, or lower for a sweeping hit (uppercut). */
    final float hitFloor;
    private final Box[] hitboxes;
    private final Box[] hurtboxes;
    /** Character mechanics of the move; never null. */
    final Mechanics mechanics;

    private AttackDefinition(Builder b) {
        mechanics = b.mechanics;
        id = b.id;
        kind = b.kind;
        strength = b.strength;
        damage = b.damage;
        startupFrames = b.startup;
        activeFrames = b.active;
        recoveryFrames = b.recovery;
        totalFrames = b.startup + b.active + b.recovery;
        threatLead = Math.round(0.16f * totalFrames);
        hitstunFrames = b.hitstun;
        blockstunFrames = b.blockstun;
        hitstopFrames = b.hitstop;
        hitWindow = b.hitWindow;
        blockWindow = b.blockWindow;
        whiffWindow = b.whiffWindow;
        cancelInto = Collections.unmodifiableList(new ArrayList<>(b.cancelInto));
        prorationPermille = b.proration;
        launch = b.launch;
        knockback = b.knockback < 0f ? b.pushbackOnHit : b.knockback;
        pushbackOnHit = b.pushbackOnHit;
        pushbackOnBlock = b.pushbackOnBlock;
        juggleCost = b.juggleCost;
        maxHits = b.strikes.length > 0 ? b.strikes.length : b.maxHits;
        strikeStop = b.strikeStop;
        finalStop = b.finalStop;
        strikeHold = b.strikeHold;
        maxUsesPerCombo = b.maxUses;
        maxUsesPerChain = b.maxChainUses;
        low = b.low;
        overhead = b.overhead;
        spin = b.spin;
        reach = b.reach;
        hitHeight = b.hitHeight;
        hitFloor = Math.min(b.hitFloor > 0f ? b.hitFloor : Float.MAX_VALUE, hitHeight - CombatRules.VERTICAL_SLACK);
        if (b.hitboxes.isEmpty() && b.strikes.length > 0) {
            // Several strikes (the animation hits more than once): one box per strike, open
            // from its frame for STRIKE_WINDOW frames or until the next one, default geometry.
            hitboxes = new Box[b.strikes.length];
            for (int i = 0; i < b.strikes.length; i++) {
                int last = i + 1 < b.strikes.length ? b.strikes[i + 1] - 1 : startupFrames + activeFrames - 1;
                hitboxes[i] = new Box(b.strikes[i], Math.min(last, b.strikes[i] + STRIKE_WINDOW - 1),
                    0f, reach, hitFloor, hitHeight + CombatRules.VERTICAL_SLACK);
            }
        } else if (b.hitboxes.isEmpty() && (kind == Kind.NORMAL || kind == Kind.PROJECTILE && b.reach > 0f)) {
            // Default strike: from the root to the reach, from hitFloor up to hitHeight + slack, on every active frame.
            hitboxes = new Box[] { new Box(
                startupFrames, startupFrames + activeFrames - 1,
                0f, reach,
                hitFloor, hitHeight + CombatRules.VERTICAL_SLACK
            ) };
        } else {
            hitboxes = b.hitboxes.toArray(new Box[0]);
        }
        hurtboxes = b.hurtboxes.toArray(new Box[0]);
    }

    /** Frames each strike of a several-strike move can connect. */
    static final int STRIKE_WINDOW = 3;

    /** Hits with boxes (normal moves, and a Super that strikes instead of throwing a wave). */
    boolean melee() { return hitboxes.length > 0; }

    int lastActiveFrame() { return startupFrames + activeFrames - 1; }
    boolean isActive(int frame) { return frame >= startupFrames && frame <= lastActiveFrame(); }

    /** Phase name used by the state machine and the debug overlay. */
    String phase(int frame) {
        if (frame < startupFrames) return "STARTUP";
        if (frame <= lastActiveFrame()) return "ACTIVE";
        return "RECOVERY";
    }

    List<String> cancelInto() { return cancelInto; }
    boolean cancelsInto(String target) { return cancelInto.contains(target); }

    int hitboxCount() { return hitboxes.length; }
    Box hitbox(int i) { return hitboxes[i]; }
    int hurtboxCount() { return hurtboxes.length; }
    Box hurtbox(int i) { return hurtboxes[i]; }

    /** Window where a defender sees the strike coming (anticipated guard), legacy 16% lead. */
    boolean threatening(int frame) {
        return frame >= startupFrames - threatLead && frame <= lastActiveFrame() + 1 + threatLead;
    }

    /** Frames the attacker is still busy after a contact on the first active frame. */
    int busyAfterFirstContact() { return activeFrames - 1 + recoveryFrames; }

    /**
     * Frame advantage on hit for a contact on the first active frame:
     * hitstun - (remaining active frames + recovery).
     */
    int advantageOnHit() { return hitstunFrames - busyAfterFirstContact(); }
    int advantageOnBlock() { return blockstunFrames - busyAfterFirstContact(); }

    /**
     * Whether {@code next}, started on the first free frame, connects before the defender
     * recovers. {@code startupFrames} counts frames before the hitbox; the hit lands on
     * frame startup + 1, so the advantage must exceed the startup.
     */
    static boolean links(int advantage, AttackDefinition next) {
        return advantage > next.startupFrames;
    }

    static Strength strengthOf(String binding) {
        if ("SUPER".equals(binding) || "ULTRA".equals(binding)) return Strength.SUPER;
        if ("S".equals(binding) || binding.matches("S[2-9]")) return Strength.SPECIAL;
        if ("THROW".equals(binding) || VoadoraTroca.ID.equals(binding)) return Strength.HEAVY;
        char button = binding.charAt(binding.length() - 1);
        if (button == 'L') return Strength.LIGHT;
        if (button == 'M') return Strength.MEDIUM;
        if (button == 'H') return Strength.HEAVY;
        throw new IllegalArgumentException("Unknown binding " + binding);
    }

    static Window window(int start, int end) { return new Window(start, end); }

    static final class Builder {
        private final String id;
        private final Kind kind;
        private final Strength strength;
        private int damage = -1;
        private int startup = -1, active = -1, recovery = -1;
        private int hitstun = -1, blockstun = -1, hitstop = -1;
        private Window hitWindow, blockWindow, whiffWindow;
        private final List<String> cancelInto = new ArrayList<>();
        private int proration = 1000;
        private Launch launch = Launch.NONE;
        private float knockback = -1f;
        private float pushbackOnHit, pushbackOnBlock;
        private int juggleCost = 1, maxHits = 1, maxUses = 0, maxChainUses = 0;
        private boolean low, overhead, spin;
        private float reach, hitHeight, hitFloor;
        private final List<Box> hitboxes = new ArrayList<>();
        private int[] strikes = new int[0];
        private int strikeStop, finalStop;
        private float[] strikeHold;
        private final List<Box> hurtboxes = new ArrayList<>();
        private Mechanics mechanics = NO_MECHANICS;

        Builder(String id, Kind kind) {
            this.id = id;
            this.kind = kind;
            this.strength = strengthOf(id);
        }
        Builder damage(int value) { damage = value; return this; }
        Builder frames(int startupFrames, int activeFrames, int recoveryFrames) {
            startup = startupFrames; active = activeFrames; recovery = recoveryFrames; return this;
        }
        Builder stun(int hitstunFrames, int blockstunFrames, int hitstopFrames) {
            hitstun = hitstunFrames; blockstun = blockstunFrames; hitstop = hitstopFrames; return this;
        }
        Builder windows(Window hit, Window block, Window whiff) {
            hitWindow = hit; blockWindow = block; whiffWindow = whiff; return this;
        }
        Builder cancelInto(String... targets) { cancelInto.addAll(Arrays.asList(targets)); return this; }
        Builder proration(int permille) { proration = permille; return this; }
        Builder launch(Launch value) { launch = value; return this; }
        Builder knockback(float value) { knockback = value; return this; }
        Builder pushback(float onHit, float onBlock) { pushbackOnHit = onHit; pushbackOnBlock = onBlock; return this; }
        Builder juggleCost(int value) { juggleCost = value; return this; }
        Builder maxHits(int value) { maxHits = value; return this; }
        /** Frames where each strike of the animation lands; the first is the end of the startup. */
        Builder strikes(int... frames) { strikes = frames.clone(); return this; }
        Builder strikeStop(int frames) { strikeStop = frames; return this; }
        Builder strikeStop(int frames, int last) { strikeStop = frames; finalStop = last; return this; }
        Builder strikeHold(float... gaps) { strikeHold = gaps.clone(); return this; }
        Builder maxUsesPerCombo(int value) { maxUses = value; return this; }
        Builder maxUsesPerChain(int value) { maxChainUses = value; return this; }
        Builder low(boolean value) { low = value; return this; }
        Builder overhead(boolean value) { overhead = value; return this; }
        Builder spin(boolean value) { spin = value; return this; }
        Builder reach(float reachValue, float hitHeightValue) { reach = reachValue; hitHeight = hitHeightValue; return this; }
        /** Reach with the box stretched down to {@code floor} (fist path of an uppercut). */
        Builder reach(float reachValue, float hitHeightValue, float floor) { reach(reachValue, hitHeightValue); hitFloor = floor; return this; }
        Builder hitbox(int first, int last, float x0, float x1, float y0, float y1) {
            hitboxes.add(new Box(first, last, x0, x1, y0, y1)); return this;
        }
        Builder hurtbox(int first, int last, float x0, float x1, float y0, float y1) {
            hurtboxes.add(new Box(first, last, x0, x1, y0, y1)); return this;
        }
        Builder mechanics(Mechanics value) { mechanics = value; return this; }

        AttackDefinition build() {
            String name = "attack " + id + ": ";
            if (damage < 0) throw new IllegalArgumentException(name + "damage is required");
            if (startup < 0 || active < 1 || recovery < 0) {
                throw new IllegalArgumentException(name + "startup >= 0, active >= 1 and recovery >= 0 are required");
            }
            if (hitstun < 1 || blockstun < 1 || hitstop < 0) {
                throw new IllegalArgumentException(name + "hitstun and blockstun >= 1, hitstop >= 0 are required");
            }
            int total = startup + active + recovery;
            checkWindow(name + "cancelWindows.hit", hitWindow, total, startup);
            checkWindow(name + "cancelWindows.block", blockWindow, total, startup);
            checkWindow(name + "cancelWindows.whiff", whiffWindow, total, 0);
            if (proration < 1 || proration > 1000) {
                throw new IllegalArgumentException(name + "damageProration must be in (0, 1]");
            }
            if (pushbackOnHit < 0f || pushbackOnBlock < 0f) {
                throw new IllegalArgumentException(name + "pushback cannot be negative");
            }
            if (juggleCost < 0 || maxUses < 0 || maxChainUses < 0) {
                throw new IllegalArgumentException(name + "juggleCost, maxUsesPerCombo and maxUsesPerChain cannot be negative");
            }
            int lastActive = startup + active - 1;
            if (strikes.length > 0) {
                if (!hitboxes.isEmpty()) throw new IllegalArgumentException(name + "strikes build their own hitboxes");
                if (strikes[0] != startup || strikes[strikes.length - 1] > lastActive) {
                    throw new IllegalArgumentException(name + "strikes start at the first active frame and stay inside the active frames");
                }
                for (int i = 1; i < strikes.length; i++) {
                    if (strikes[i] <= strikes[i - 1]) throw new IllegalArgumentException(name + "strikes must grow");
                }
                if (kind == Kind.PROJECTILE) throw new IllegalArgumentException(name + "a projectile move cannot strike");
            }
            if (strikeHold != null && strikeHold.length != strikes.length) {
                throw new IllegalArgumentException(name + "strikeHold needs one place per strike");
            }
            if (strikeStop < 0 || strikeStop > 0 && strikes.length == 0) {
                throw new IllegalArgumentException(name + "strikeStop is the pause of each strike of a several-strike move");
            }
            if (finalStop < 0 || finalStop > 0 && strikeStop == 0) {
                throw new IllegalArgumentException(name + "finalStop is the pause of the last strike of a strikeStop move");
            }
            for (Box box : hitboxes) {
                if (box.firstFrame < startup || box.lastFrame > lastActive || box.firstFrame > box.lastFrame) {
                    throw new IllegalArgumentException(name + "hitbox frames must stay inside the active frames");
                }
                if (box.x0 > box.x1 || box.y0 > box.y1) {
                    throw new IllegalArgumentException(name + "hitbox ranges must be ordered");
                }
            }
            for (Box box : hurtboxes) {
                if (box.firstFrame < 0 || box.lastFrame >= total || box.firstFrame > box.lastFrame) {
                    throw new IllegalArgumentException(name + "hurtbox frames must stay inside the move");
                }
            }
            if (kind == Kind.NORMAL) {
                if (reach <= 0f || hitHeight <= 0f) {
                    throw new IllegalArgumentException(name + "reach and hitHeight must be positive");
                }
                int boxes = hitboxes.isEmpty() ? 1 : hitboxes.size();
                if (maxHits < 1 || maxHits > boxes) {
                    throw new IllegalArgumentException(name + "maxHits must be between 1 and the number of hitboxes");
                }
            } else if (kind == Kind.SUPER && strikes.length > 0) {
                if (reach <= 0f || hitHeight <= 0f) {
                    throw new IllegalArgumentException(name + "a striking Super needs reach and hitHeight");
                }
            } else if (kind == Kind.PROJECTILE && reach > 0f) {
                // S corpo a corpo (tapa, investida): bate com a caixa padrão em vez de lançar.
                if (hitHeight <= 0f || !hitboxes.isEmpty()) {
                    throw new IllegalArgumentException(name + "a melee S needs hitHeight and uses the default strike box");
                }
            } else if (active != 1 || !hitboxes.isEmpty()) {
                throw new IllegalArgumentException(name + "projectile moves spawn on one active frame and have no hitboxes");
            }
            Mechanics m = mechanics;
            if (m.armorHits < 0 || m.poisonDamage < 0 || m.lifeCost < 0 || m.pull < 0f ||
                m.chargeFrames < 0 || m.chargeBonusPermille < 0 || m.echoFrames < 0 || m.echoDelay < 0 ||
                m.echoDamagePermille < 0) {
                throw new IllegalArgumentException(name + "mechanics cannot be negative");
            }
            if (m.armorHits > 0) checkRange(name + "armor", m.armorStart, m.armorEnd, total);
            if (m.counter != Reads.NONE) checkRange(name + "counter", m.counterStart, m.counterEnd, total);
            if (m.evade != Reads.NONE) checkRange(name + "evade", m.evadeStart, m.evadeEnd, total);
            if (m.advanceSpeed != 0f) checkRange(name + "advance", m.advanceStart, m.advanceEnd, total);
            if (m.teleportFrame >= total) throw new IllegalArgumentException(name + "teleport frame is outside the move");
            if (m.poisonDamage > 0 && m.poisonFrames < 1) throw new IllegalArgumentException(name + "poison needs frames");
            if (m.repel && m.counter == Reads.NONE) throw new IllegalArgumentException(name + "a repel is a counter stance");
            if (m.counter != Reads.NONE && (kind != Kind.NORMAL || m.counterEnd >= startup)) {
                throw new IllegalArgumentException(name + "a counter stance is a melee move that reads before its strike");
            }
            return new AttackDefinition(this);
        }

        private static void checkRange(String name, int start, int end, int total) {
            if (start < 0 || start > end || end >= total) {
                throw new IllegalArgumentException(name + " [" + start + "," + end + "] is outside the move (0.." + (total - 1) + ")");
            }
        }

        private static void checkWindow(String name, Window w, int total, int earliest) {
            if (w == null) return;
            if (w.start > w.end || w.start < earliest || w.end >= total) {
                throw new IllegalArgumentException(name + " " + w + " is outside the move (" + earliest + ".." + (total - 1) + ")");
            }
        }
    }
}
