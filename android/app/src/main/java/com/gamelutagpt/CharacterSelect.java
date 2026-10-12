package com.gamelutagpt;

import java.util.Random;

/**
 * Seletor de duas duplas (P1 e P2), com foco independente no titular ou TAG.
 * Coordenadas em 1280x720, as mesmas usadas pelo GameView para toque e desenho.
 * Somente os IDs em GeneratedCharacters.SELECTABLE podem ser escolhidos.
 */
final class CharacterSelect {
    enum Result { NONE, CHANGED, CONFIRM, CANCEL }

    static final int COLUMNS = 4;
    static final int ROWS = 3;
    static final float GRID_X = 385f, GRID_Y = 157f, GRID_W = 510f, GRID_H = 348f, GRID_GAP = 6f;
    static final float TEAM_LEFT = 20f, TEAM_RIGHT = 1022f, TEAM_TOP = 415f;
    static final float TEAM_W = 238f, SLOT_H = 78f, SLOT_GAP = 11f;
    static final float FIGHT_LEFT = 533f, FIGHT_TOP = 664f, FIGHT_RIGHT = 747f, FIGHT_BOTTOM = 711f;
    static final float BACK_LEFT = 340f, BACK_TOP = 664f, BACK_RIGHT = 510f, BACK_BOTTOM = 711f;
    static final float RANDOM_LEFT = 770f, RANDOM_TOP = 664f, RANDOM_RIGHT = 940f, RANDOM_BOTTOM = 711f;
    static final String STAGE = "TEMPLO DA LUA";

    final String[] ids;
    private final String[][] picks = new String[2][2];
    private final Random random = new Random();
    private boolean open;
    private int focusSide;
    private int focusSlot;

    CharacterSelect(String[] ids) {
        if (ids == null || ids.length < 2) throw new IllegalArgumentException("minimo de dois personagens");
        this.ids = ids.clone();
    }

    void open(String p1, String tag1, String p2, String tag2) {
        setTeam(0, p1, tag1);
        setTeam(1, p2, tag2);
        focusSide = 0;
        focusSlot = 0;
        open = true;
    }

    /** Compatibilidade com chamadas antigas: P2 recebe outra dupla do elenco. */
    void open(String point, String partner) {
        open(point, partner, ids[1], ids.length > 2 ? ids[2] : ids[0]);
    }

    private void setTeam(int side, String point, String tag) {
        picks[side][0] = contains(point) ? point : ids[0];
        picks[side][1] = contains(tag) && !tag.equals(picks[side][0]) ? tag : firstDifferent(picks[side][0]);
    }

    private String firstDifferent(String from) {
        for (String id : ids) if (!id.equals(from)) return id;
        throw new IllegalStateException("sem parceiro");
    }

    void close() { open = false; }
    boolean isOpen() { return open; }
    boolean ready() {
        return !picks[0][0].equals(picks[0][1]) && !picks[1][0].equals(picks[1][1]);
    }

    String pick(int side, int slot) { return picks[side][slot]; }
    String point() { return pick(0, 0); }
    String partner() { return pick(0, 1); }
    String opponentPoint() { return pick(1, 0); }
    String opponentPartner() { return pick(1, 1); }
    int focusSide() { return focusSide; }
    int focusSlot() { return focusSlot; }

    /** Uma carta pode estar selecionada pelas duas equipes. 0=ausente, 1=titular, 2=TAG. */
    int order(int side, String id) {
        if (id.equals(picks[side][0])) return 1;
        if (id.equals(picks[side][1])) return 2;
        return 0;
    }

    float cardWidth() { return (GRID_W - (COLUMNS - 1) * GRID_GAP) / COLUMNS; }
    float cardHeight() { return (GRID_H - (ROWS - 1) * GRID_GAP) / ROWS; }
    float cardLeft(int index) { return GRID_X + index % COLUMNS * (cardWidth() + GRID_GAP); }
    float cardTop(int index) { return GRID_Y + index / COLUMNS * (cardHeight() + GRID_GAP); }
    float slotLeft(int side) { return side == 0 ? TEAM_LEFT : TEAM_RIGHT; }
    float slotTop(int slot) { return TEAM_TOP + slot * (SLOT_H + SLOT_GAP); }

    int cardAt(float x, float y) {
        for (int i = 0; i < ids.length; i++) {
            if (inside(x, y, cardLeft(i), cardTop(i),
                    cardLeft(i) + cardWidth(), cardTop(i) + cardHeight())) return i;
        }
        return -1; // quadrados trancados nunca respondem ao toque
    }

    Result tap(float x, float y) {
        if (!open) return Result.NONE;
        if (inside(x, y, BACK_LEFT, BACK_TOP, BACK_RIGHT, BACK_BOTTOM)) return Result.CANCEL;
        if (inside(x, y, FIGHT_LEFT, FIGHT_TOP, FIGHT_RIGHT, FIGHT_BOTTOM))
            return ready() ? Result.CONFIRM : Result.NONE;
        if (inside(x, y, RANDOM_LEFT, RANDOM_TOP, RANDOM_RIGHT, RANDOM_BOTTOM)) {
            shuffle();
            return Result.CHANGED;
        }

        for (int side = 0; side < 2; side++) {
            float left = slotLeft(side);
            for (int slot = 0; slot < 2; slot++) {
                if (inside(x, y, left, slotTop(slot), left + TEAM_W, slotTop(slot) + SLOT_H)) {
                    focusSide = side;
                    focusSlot = slot;
                    return Result.CHANGED;
                }
            }
        }
        int card = cardAt(x, y);
        if (card >= 0) {
            assign(focusSide, focusSlot, ids[card]);
            advanceFocus();
            return Result.CHANGED;
        }
        if (y >= 35f && y < TEAM_TOP) {
            if (x < 320f) { focusSide = 0; focusSlot = 0; return Result.CHANGED; }
            if (x > 960f) { focusSide = 1; focusSlot = 0; return Result.CHANGED; }
        }
        return Result.NONE;
    }

    private void assign(int side, int slot, String id) {
        int other = 1 - slot;
        // Troca os dois slots se o novo ID já for o outro membro.
        if (id.equals(picks[side][other])) {
            picks[side][other] = picks[side][slot];
        }
        picks[side][slot] = id;
    }

    private void advanceFocus() {
        if (focusSlot == 0) focusSlot = 1;
        else if (focusSide == 0) { focusSide = 1; focusSlot = 0; }
    }

    private void shuffle() {
        for (int side = 0; side < 2; side++) {
            int a = random.nextInt(ids.length);
            int b = random.nextInt(ids.length - 1);
            if (b >= a) b++;
            picks[side][0] = ids[a];
            picks[side][1] = ids[b];
        }
        focusSide = 0;
        focusSlot = 0;
    }

    private boolean contains(String id) {
        if (id == null) return false;
        for (String value : ids) if (value.equals(id)) return true;
        return false;
    }

    private static boolean inside(float x, float y, float l, float t, float r, float b) {
        return x >= l && x <= r && y >= t && y <= b;
    }
}
