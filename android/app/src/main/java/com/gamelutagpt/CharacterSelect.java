package com.gamelutagpt;

import java.util.ArrayList;
import java.util.List;

/**
 * Character select: the player picks the pair that fights. The first pick starts on point,
 * the second is the TAG partner; tapping a picked card drops it. Pure state and layout (in
 * the 1280×720 virtual screen); {@link CharacterSelectRenderer} draws it.
 */
final class CharacterSelect {
    enum Result { NONE, CHANGED, CONFIRM, CANCEL }

    static final float CARD_W = 190f, CARD_H = 300f, CARD_GAP = 26f, CARD_MIN_GAP = 12f, CARD_TOP = 150f;
    static final float FIGHT_LEFT = 530f, FIGHT_TOP = 560f, FIGHT_RIGHT = 750f, FIGHT_BOTTOM = 630f;
    static final float BACK_LEFT = 40f, BACK_TOP = 30f, BACK_RIGHT = 200f, BACK_BOTTOM = 80f;

    final String[] ids;
    private final List<String> picks = new ArrayList<>(2);
    private boolean open;

    CharacterSelect(String[] ids) {
        if (ids.length < 2) throw new IllegalArgumentException("pick needs at least two characters");
        this.ids = ids.clone();
    }

    /** Opens with the current pair already picked, so LUTAR alone keeps it. */
    void open(String point, String partner) {
        picks.clear();
        if (indexOf(point) >= 0) picks.add(point);
        if (indexOf(partner) >= 0 && !partner.equals(point)) picks.add(partner);
        open = true;
    }

    void close() { open = false; }
    boolean isOpen() { return open; }
    boolean ready() { return picks.size() == 2; }

    /** 1 = point, 2 = partner, 0 = not picked. */
    int order(String id) { return picks.indexOf(id) + 1; }
    String point() { return picks.get(0); }
    String partner() { return picks.get(1); }

    Result tap(float x, float y) {
        if (!open) return Result.NONE;
        if (inside(x, y, BACK_LEFT, BACK_TOP, BACK_RIGHT, BACK_BOTTOM)) return Result.CANCEL;
        if (inside(x, y, FIGHT_LEFT, FIGHT_TOP, FIGHT_RIGHT, FIGHT_BOTTOM)) return ready() ? Result.CONFIRM : Result.NONE;
        int card = cardAt(x, y);
        if (card < 0) return Result.NONE;
        String id = ids[card];
        if (picks.remove(id)) return Result.CHANGED;
        if (picks.size() == 2) return Result.NONE;  // drop one first
        picks.add(id);
        return Result.CHANGED;
    }

    int cardAt(float x, float y) {
        for (int i = 0; i < ids.length; i++) {
            float left = cardLeft(i);
            if (inside(x, y, left, CARD_TOP, left + cardWidth(), CARD_TOP + CARD_H)) return i;
        }
        return -1;
    }

    /** Cards in one centered row: the gap shrinks first, then the cards get narrower. */
    float cardLeft(int i) {
        float w = cardWidth(), gaps = Math.max(1, ids.length - 1);
        float gap = Math.max(CARD_MIN_GAP, Math.min(CARD_GAP, (Arena.VW - 40f - ids.length * w) / gaps));
        float row = ids.length * w + (ids.length - 1) * gap;
        return (Arena.VW - row) / 2f + i * (w + gap);
    }

    float cardWidth() {
        return Math.min(CARD_W, (Arena.VW - 40f - (ids.length - 1) * CARD_MIN_GAP) / ids.length);
    }

    private int indexOf(String id) {
        for (int i = 0; i < ids.length; i++) if (ids[i].equals(id)) return i;
        return -1;
    }

    private static boolean inside(float x, float y, float l, float t, float r, float b) {
        return x >= l && x <= r && y >= t && y <= b;
    }
}
