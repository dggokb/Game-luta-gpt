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
    enum Launch { NONE, KNOCKDOWN, LAUNCH, SLAM }

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

    final String id;
    final Kind kind;
    final Strength strength;
    final int damage;
    final int startupFrames, activeFrames, recoveryFrames, totalFrames;
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
    /** Must be guarded crouching. Airborne attackers are always overheads. */
    final boolean low;
    final float reach, hitHeight;
    private final Box[] hitboxes;
    private final Box[] hurtboxes;

    private AttackDefinition(Builder b) {
        id = b.id;
        kind = b.kind;
        strength = b.strength;
        damage = b.damage;
        startupFrames = b.startup;
        activeFrames = b.active;
        recoveryFrames = b.recovery;
        totalFrames = b.startup + b.active + b.recovery;
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
        maxHits = b.maxHits;
        maxUsesPerCombo = b.maxUses;
        low = b.low;
        reach = b.reach;
        hitHeight = b.hitHeight;
        if (b.hitboxes.isEmpty() && kind == Kind.NORMAL) {
            // Default strike: from the root to the reach, around hitHeight, on every active frame.
            hitboxes = new Box[] { new Box(
                startupFrames, startupFrames + activeFrames - 1,
                0f, reach,
                hitHeight - CombatRules.VERTICAL_SLACK, hitHeight + CombatRules.VERTICAL_SLACK
            ) };
        } else {
            hitboxes = b.hitboxes.toArray(new Box[0]);
        }
        hurtboxes = b.hurtboxes.toArray(new Box[0]);
    }

    int firstActiveFrame() { return startupFrames; }
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
        int lead = Math.round(0.16f * totalFrames);
        return frame >= startupFrames - lead && frame <= lastActiveFrame() + 1 + lead;
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
        if ("SUPER".equals(binding)) return Strength.SUPER;
        if ("S".equals(binding)) return Strength.SPECIAL;
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
        private int juggleCost = 1, maxHits = 1, maxUses = 0;
        private boolean low;
        private float reach, hitHeight;
        private final List<Box> hitboxes = new ArrayList<>();
        private final List<Box> hurtboxes = new ArrayList<>();

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
        Builder maxUsesPerCombo(int value) { maxUses = value; return this; }
        Builder low(boolean value) { low = value; return this; }
        Builder reach(float reachValue, float hitHeightValue) { reach = reachValue; hitHeight = hitHeightValue; return this; }
        Builder hitbox(int first, int last, float x0, float x1, float y0, float y1) {
            hitboxes.add(new Box(first, last, x0, x1, y0, y1)); return this;
        }
        Builder hurtbox(int first, int last, float x0, float x1, float y0, float y1) {
            hurtboxes.add(new Box(first, last, x0, x1, y0, y1)); return this;
        }

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
            if (juggleCost < 0 || maxUses < 0) {
                throw new IllegalArgumentException(name + "juggleCost and maxUsesPerCombo cannot be negative");
            }
            int lastActive = startup + active - 1;
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
            } else if (active != 1 || !hitboxes.isEmpty()) {
                throw new IllegalArgumentException(name + "projectile moves spawn on one active frame and have no hitboxes");
            }
            return new AttackDefinition(this);
        }

        private static void checkWindow(String name, Window w, int total, int earliest) {
            if (w == null) return;
            if (w.start > w.end || w.start < earliest || w.end >= total) {
                throw new IllegalArgumentException(name + " " + w + " is outside the move (" + earliest + ".." + (total - 1) + ")");
            }
        }
    }
}
