package com.gamelutagpt;

import java.util.ArrayList;
import java.util.List;

/**
 * Attack presses waiting for a moment when they are allowed. A press never interrupts
 * anything by itself: the engine asks the buffer for the best entry it may start and
 * consumes it. Entries age only on frames the owner is not in hitstop, and expire after
 * {@link CombatConfig#bufferFrames}.
 */
final class InputBuffer {
    enum Button { LIGHT, MEDIUM, HEAVY, SPECIAL, SUPER, AUTO }

    static final class Entry {
        final Button button;
        /** SPECIAL: strength "L", "M" or "H" of the energy, or the command special ("S2"...). */
        final String strength;
        /** Down was held when it was pressed (grounded presses become 2L/2M/2H). */
        final boolean crouch;
        /** Global press order; a higher value is more recent. */
        final long order;
        int age;

        Entry(Button button, String strength, boolean crouch, long order) {
            this.button = button;
            this.strength = strength;
            this.crouch = crouch;
            this.order = order;
        }

        @Override public String toString() {
            String name = button == Button.SPECIAL
                ? (strength != null && strength.startsWith("S") ? strength : "S" + strength) : button.name();
            return (crouch ? "2" : "") + name + "(" + age + ")";
        }
    }

    /** Resolves an entry to the move it would start now, or null when it cannot start. */
    interface Resolver {
        AttackDefinition resolve(Entry entry);
    }

    static final int CAPACITY = 8;
    private final List<Entry> entries = new ArrayList<>(CAPACITY);
    private long nextOrder;

    void push(Button button, String strength, boolean crouch) {
        if (entries.size() >= CAPACITY) entries.remove(0);
        entries.add(new Entry(button, strength, crouch, nextOrder++));
    }

    /** One unfrozen frame passed: everything ages and old presses expire. */
    void age(int bufferFrames) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry entry = entries.get(i);
            entry.age++;
            if (entry.age > bufferFrames) entries.remove(i);
        }
    }

    /**
     * Highest-priority entry the resolver accepts; among equal priorities the most recent
     * press wins. {@code priority} lists strengths from most to least important.
     */
    Entry select(Resolver resolver, AttackDefinition.Strength[] priority, AttackDefinition[] resolvedOut) {
        Entry best = null;
        AttackDefinition bestAttack = null;
        int bestRank = Integer.MAX_VALUE;
        for (Entry entry : entries) {
            AttackDefinition attack = resolver.resolve(entry);
            if (attack == null) continue;
            int rank = rank(priority, attack.strength);
            if (rank < bestRank || (rank == bestRank && entry.order > best.order)) {
                best = entry;
                bestAttack = attack;
                bestRank = rank;
            }
        }
        if (resolvedOut != null) resolvedOut[0] = bestAttack;
        return best;
    }

    void consume(Entry entry) {
        entries.remove(entry);
    }

    void clear() {
        entries.clear();
    }

    int size() {
        return entries.size();
    }

    List<Entry> entries() {
        return java.util.Collections.unmodifiableList(entries);
    }

    private static int rank(AttackDefinition.Strength[] priority, AttackDefinition.Strength strength) {
        for (int i = 0; i < priority.length; i++) if (priority[i] == strength) return i;
        return priority.length;
    }
}
