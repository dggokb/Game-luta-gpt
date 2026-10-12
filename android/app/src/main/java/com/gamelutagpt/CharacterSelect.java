package com.gamelutagpt;

import java.util.Random;

/**
 * Character selection for two independent TAG teams. Screen space is 1280x720
 * and deliberately matches the approved Moon Temple layout.
 */
final class CharacterSelect {
    enum Result { NONE, CHANGED, CONFIRM, CANCEL }
    static final int COLUMNS = 4, ROWS = 4;
    static final float GRID_X = 387f, GRID_Y = 137f, GRID_W = 506f, GRID_H = 400f, GRID_GAP = 5f;
    static final float TEAM_LEFT = 22f, TEAM_RIGHT = 1058f, TEAM_TOP = 382f;
    static final float TEAM_W = 201f, SLOT_H = 100f, SLOT_GAP = 32f;
    static final float FIGHT_LEFT = 301f, FIGHT_TOP = 685f, FIGHT_RIGHT = 439f, FIGHT_BOTTOM = 720f;
    static final float BACK_LEFT = 442f, BACK_TOP = 685f, BACK_RIGHT = 565f, BACK_BOTTOM = 720f;
    static final float SETTINGS_LEFT = 565f, SETTINGS_RIGHT = 802f;
    static final float SWAP_LEFT = 804f, SWAP_RIGHT = 1037f;
    static final float RANDOM_LEFT = 1037f, RANDOM_TOP = 685f, RANDOM_RIGHT = 1279f, RANDOM_BOTTOM = 720f;
    static final String STAGE = "TEMPLO DA LUA";
    final String[] ids;
    private final String[][] picks = new String[2][2];
    private final Random random = new Random();
    private boolean open, showHelp;
    private int focusSide, focusSlot, cursorIndex;

    CharacterSelect(String[] ids) {
        if (ids == null || ids.length < 2) throw new IllegalArgumentException("need at least two fighters");
        this.ids = ids.clone();
    }
    void open(String p1, String tag1, String p2, String tag2) {
        setTeam(0, p1, tag1);
        setTeam(1, p2, tag2);
        focusSide = focusSlot = 0;
        cursorIndex = indexOf(picks[0][0]);
        showHelp = false;
        open = true;
    }
    void open(String point, String tag) {
        open(point, tag, ids[1], ids.length > 2 ? ids[2] : ids[0]);
    }
    private void setTeam(int side, String point, String tag) {
        picks[side][0] = contains(point) ? point : ids[0];
        picks[side][1] = contains(tag) && !tag.equals(picks[side][0]) ? tag : firstDifferent(picks[side][0]);
    }
    private String firstDifferent(String id) {
        for (String other : ids) if (!other.equals(id)) return other;
        throw new IllegalStateException("no partner");
    }
    private int indexOf(String id) {
        for (int i = 0; i < ids.length; i++) if (ids[i].equals(id)) return i;
        return 0;
    }
    boolean contains(String id) { return id != null && indexOf(id) < ids.length && ids[indexOf(id)].equals(id); }
    void close() { open = false; }
    boolean isOpen() { return open; }
    boolean settingsOpen() { return showHelp; }
    boolean ready() { return !picks[0][0].equals(picks[0][1]) && !picks[1][0].equals(picks[1][1]); }
    String pick(int side, int slot) { return picks[side][slot]; }
    String point() { return pick(0,0); }
    String partner() { return pick(0,1); }
    String opponentPoint() { return pick(1,0); }
    String opponentPartner() { return pick(1,1); }
    int focusSide() { return focusSide; }
    int focusSlot() { return focusSlot; }
    int cursorIndex() { return cursorIndex; }
    int stageCount() { return 1; }  // no imaginary, unimplemented stages
    int order(int side, String id) {
        if (id.equals(picks[side][0])) return 1;
        if (id.equals(picks[side][1])) return 2;
        return 0;
    }
    float cardWidth() { return (GRID_W - GRID_GAP * (COLUMNS-1)) / COLUMNS; }
    float cardHeight() { return (GRID_H - GRID_GAP * (ROWS-1)) / ROWS; }
    float cardLeft(int i) { return GRID_X + (i%COLUMNS) * (cardWidth()+GRID_GAP); }
    float cardTop(int i) { return GRID_Y + (i/COLUMNS) * (cardHeight()+GRID_GAP); }
    float slotLeft(int side) { return side == 0 ? TEAM_LEFT : TEAM_RIGHT; }
    float slotTop(int slot) { return TEAM_TOP + slot * (SLOT_H+SLOT_GAP); }
    int cardAt(float x, float y) {
        for (int i = 0; i < ids.length; i++)
            if (inside(x,y,cardLeft(i),cardTop(i),cardLeft(i)+cardWidth(),cardTop(i)+cardHeight())) return i;
        return -1;
    }
    void chooseFocus(int side, int slot) {
        focusSide = side; focusSlot = slot;
        cursorIndex = indexOf(picks[side][slot]);
    }
    void moveCursor(int dx, int dy) {
        int row = cursorIndex / COLUMNS, col = cursorIndex % COLUMNS;
        row = Math.max(0, Math.min(ROWS-1, row+dy));
        col = Math.max(0, Math.min(COLUMNS-1, col+dx));
        int next = row * COLUMNS + col;
        if (next < ids.length) cursorIndex = next;
    }
    void selectCursor() { assign(focusSide,focusSlot,ids[cursorIndex]);advanceFocus(); }
    void swapFocusedTeam() {
        String tmp = picks[focusSide][0];
        picks[focusSide][0] = picks[focusSide][1];
        picks[focusSide][1] = tmp;
        focusSlot = 0;
        cursorIndex = indexOf(picks[focusSide][0]);
    }
    void randomFocused() {
        int next = random.nextInt(ids.length);
        if (ids[next].equals(picks[focusSide][1-focusSlot])) next = (next+1)%ids.length;
        assign(focusSide, focusSlot, ids[next]);
        advanceFocus();
    }
    void toggleSettings() { showHelp = !showHelp; }
    private void assign(int side,int slot,String id) {
        int other = 1-slot;
        if (id.equals(picks[side][other])) picks[side][other] = picks[side][slot];
        picks[side][slot] = id;
        cursorIndex = indexOf(id);
    }
    private void advanceFocus() {
        if (focusSlot==0) focusSlot=1;
        else if (focusSide==0) {focusSide=1;focusSlot=0;}
        cursorIndex=indexOf(picks[focusSide][focusSlot]);
    }
    Result tap(float x,float y) {
        if (!open) return Result.NONE;
        // The bottom command bar uses exactly the same visible hit zones as the artwork.
        if (y>=FIGHT_TOP && y<=FIGHT_BOTTOM) {
            if (x>=FIGHT_LEFT && x<=FIGHT_RIGHT) return ready()?Result.CONFIRM:Result.NONE;
            if (x>=BACK_LEFT && x<=BACK_RIGHT) return Result.CANCEL;
            if (x>=SETTINGS_LEFT && x<=SETTINGS_RIGHT) {toggleSettings();return Result.CHANGED;}
            if (x>=SWAP_LEFT && x<=SWAP_RIGHT) {swapFocusedTeam();return Result.CHANGED;}
            if (x>=RANDOM_LEFT && x<=RANDOM_RIGHT) {randomFocused();return Result.CHANGED;}
        }
        if (showHelp) {showHelp=false;return Result.CHANGED;}
        for (int side=0;side<2;side++) for(int slot=0;slot<2;slot++) {
            float l=slotLeft(side),t=slotTop(slot);
            if (inside(x,y,l,t,l+TEAM_W,t+SLOT_H)) {chooseFocus(side,slot);return Result.CHANGED;}
        }
        int card=cardAt(x,y);
        if(card>=0) {assign(focusSide,focusSlot,ids[card]);advanceFocus();return Result.CHANGED;}
        if (y<TEAM_TOP && x<300f) {chooseFocus(0,0);return Result.CHANGED;}
        if (y<TEAM_TOP && x>980f) {chooseFocus(1,0);return Result.CHANGED;}
        return Result.NONE;
    }
    private static boolean inside(float x,float y,float l,float t,float r,float b) {
        return x>=l&&x<=r&&y>=t&&y<=b;
    }
}
