package com.gamelutagpt;

import java.util.Random;

/**
 * Character selection for two independent TAG teams. Screen space is 1280x720
 * and deliberately matches the approved Moon Temple layout. {@link #openSolo} picks only the
 * player's team (the Arcade's free team: the opponents come from the bracket draw).
 */
final class CharacterSelect {
    enum Result { NONE, CHANGED, CONFIRM, CANCEL }
    static final int COLUMNS = 4, ROWS = 4;
    static final float GRID_X = 420f, GRID_Y = 137f, GRID_W = 440f, GRID_H = 400f, GRID_GAP = 6f;
    static final float TEAM_LEFT = 33f, TEAM_RIGHT = 1087f, TEAM_TOP = 383f;
    static final float TEAM_W = 160f, SLOT_H = 96f, SLOT_GAP = 34f;
    static final float FIGHT_LEFT = 301f, FIGHT_TOP = 685f, FIGHT_RIGHT = 439f, FIGHT_BOTTOM = 720f;
    static final float BACK_LEFT = 442f, BACK_TOP = 685f, BACK_RIGHT = 565f, BACK_BOTTOM = 720f;
    static final float SETTINGS_LEFT = 565f, SETTINGS_RIGHT = 802f;
    static final float SWAP_LEFT = 804f, SWAP_RIGHT = 1037f;
    static final float RANDOM_LEFT = 1037f, RANDOM_TOP = 685f, RANDOM_RIGHT = 1279f, RANDOM_BOTTOM = 720f;
    static final String STAGE = "TEMPLO DA LUA";
    /** Painel do cenário embaixo da grade: as setas trocam o cenário da luta de teste. */
    static final float STAGE_LEFT = 294f, STAGE_TOP = 547f, STAGE_RIGHT = 986f, STAGE_BOTTOM = 682f;
    static final float STAGE_PREV_RIGHT = 540f, STAGE_NEXT_LEFT = 740f;
    /**
     * Grid order: the story pairs side by side (p01+p02, p03+p04 … p15+p16, p13+p14), so each
     * pair sits on the same row. Playable ones come first; the rest are locked concept slots.
     */
    static final String[] PAIR_ORDER = {
        "player_base","p02","p03","p04","p05","p06","p07","p08",
        "p09","p10","p11","p12","p15","p16","p13","p14"};
    final String[] ids;
    private final String[][] picks = new String[2][2];
    private final Random random = new Random();
    private boolean open, showHelp;
    private int focusSide, focusSlot, cursorIndex, confirmedSteps;
    /** Picks to confirm: 4 (both teams) or 2 (only the player's, {@link #openSolo}). */
    private int steps = 4;
    /** Cenários (pastas em assets/stages) e os nomes deles; o escolhido vale para a luta de teste. */
    private String[] stageIds = {"templo_lua"}, stageNames = {STAGE};
    private int stageIndex;

    CharacterSelect(String[] ids) {
        if (ids == null || ids.length < 2) throw new IllegalArgumentException("need at least two fighters");
        this.ids = ids.clone();
    }
    void open(String p1, String tag1, String p2, String tag2) {
        setTeam(0, p1, tag1);
        setTeam(1, p2, tag2);
        focusSide = focusSlot = confirmedSteps = 0;
        steps = 4;
        cursorIndex = indexOf(picks[0][0]);
        showHelp = false;
        open = true;
    }
    void open(String point, String tag) {
        open(point, tag, ids[1], ids.length > 2 ? ids[2] : ids[0]);
    }
    /** Only the player's team: point and tag (the other side stays hidden). */
    void openSolo(String point, String tag) {
        open(point, tag);
        steps = 2;
    }
    boolean solo() { return steps == 2; }
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
    boolean ready() {
        return confirmedSteps==steps && !picks[0][0].equals(picks[0][1]) && (solo() || !picks[1][0].equals(picks[1][1]));
    }
    int step() { return confirmedSteps; }
    boolean chosen(int side,int slot) { return side*2+slot<confirmedSteps; }
    String stepLabel() {
        if (solo()) {
            switch(confirmedSteps) {
                case 0:return "1/2  ESCOLHA SEU LUTADOR";
                case 1:return "2/2  ESCOLHA SEU PARCEIRO";
                default:return "DUPLA PRONTA — CONFIRMAR";
            }
        }
        switch(confirmedSteps) {
            case 0:return "1/4  ESCOLHA PLAYER 1";
            case 1:return "2/4  ESCOLHA TAG DO PLAYER 1";
            case 2:return "3/4  ESCOLHA PLAYER 2";
            case 3:return "4/4  ESCOLHA TAG DO PLAYER 2";
            default:return "EQUIPES PRONTAS — CONFIRMAR";
        }
    }
    String preview(int side) {return confirmedSteps<steps&&focusSide==side?ids[cursorIndex]:pick(side,0);}
    Result back() {
        if(showHelp) {showHelp=false;return Result.CHANGED;}
        if(confirmedSteps==0)return Result.CANCEL;
        confirmedSteps--;focusSide=confirmedSteps/2;focusSlot=confirmedSteps%2;
        cursorIndex=indexOf(picks[focusSide][focusSlot]);return Result.CHANGED;
    }
    String pick(int side, int slot) { return picks[side][slot]; }
    String point() { return pick(0,0); }
    String partner() { return pick(0,1); }
    String opponentPoint() { return pick(1,0); }
    String opponentPartner() { return pick(1,1); }
    int focusSide() { return focusSide; }
    int focusSlot() { return focusSlot; }
    int cursorIndex() { return cursorIndex; }
    int stageCount() { return stageIds.length; }
    void setStages(String[] ids, String[] names) {
        if (ids == null || names == null || ids.length == 0 || ids.length != names.length) return;
        String current = stage();
        stageIds = ids.clone();
        stageNames = names.clone();
        selectStage(current);
    }
    /** Põe o cursor do cenário no {@code id} (o da luta de agora); desconhecido: fica onde está. */
    void selectStage(String id) {
        for (int i = 0; i < stageIds.length; i++) if (stageIds[i].equals(id)) stageIndex = i;
    }
    String stage() { return stageIds[stageIndex]; }
    int stageIndex() { return stageIndex; }
    String stageName() { return stageNames[stageIndex]; }
    /** O Arcade sorteia o cenário de cada luta: lá o painel só mostra, não troca. */
    boolean stagePickable() { return !solo() && stageIds.length > 1; }
    boolean moveStage(int direction) {
        if (!stagePickable()) return false;
        stageIndex = Math.floorMod(stageIndex + direction, stageIds.length);
        return true;
    }
    int order(int side, String id) {
        if (chosen(side,0)&&id.equals(picks[side][0])) return 1;
        if (chosen(side,1)&&id.equals(picks[side][1])) return 2;
        return 0;
    }
    float cardWidth() { return (GRID_W - GRID_GAP * (COLUMNS-1)) / COLUMNS; }
    float cardHeight() { return (GRID_H - GRID_GAP * (ROWS-1)) / ROWS; }
    /** Both rendering and hit testing use the same symmetric perspective projection. */
    static float gridWidthAt(float y) {
        float t = Math.max(0f, Math.min(1f, (y-GRID_Y)/GRID_H));
        return GRID_W + 148f*t;
    }
    private static float projectX(float x, float y) {
        return 640f+(x-640f)*gridWidthAt(y)/GRID_W;
    }
    float cardLeft(int i) {
        return projectX(GRID_X+(i%COLUMNS)*(cardWidth()+GRID_GAP),cardTop(i));
    }
    float cardTop(int i) { return GRID_Y+(i/COLUMNS)*(cardHeight()+GRID_GAP); }
    void cardCorners(int i, float[] out) {
        float x=GRID_X+(i%COLUMNS)*(cardWidth()+GRID_GAP), y=cardTop(i);
        out[0]=projectX(x,y);out[1]=y;
        out[2]=projectX(x+cardWidth(),y);out[3]=y;
        out[4]=projectX(x+cardWidth(),y+cardHeight());out[5]=y+cardHeight();
        out[6]=projectX(x,y+cardHeight());out[7]=y+cardHeight();
    }
    float slotLeft(int side) { return side==0?TEAM_LEFT:TEAM_RIGHT; }
    float slotTop(int slot) { return TEAM_TOP+slot*(SLOT_H+SLOT_GAP); }
    int cardAt(float x,float y) {
        if(y<GRID_Y || y>GRID_Y+GRID_H) return -1;
        float designX=640f+(x-640f)*GRID_W/gridWidthAt(y);
        for(int i=0;i<ids.length;i++) {
            float l=GRID_X+(i%COLUMNS)*(cardWidth()+GRID_GAP),t=cardTop(i);
            if(inside(designX,y,l,t,l+cardWidth(),t+cardHeight())) return i;
        }
        return -1;
    }
    void moveCursor(int dx, int dy) {
        int row = cursorIndex / COLUMNS, col = cursorIndex % COLUMNS;
        row = Math.max(0, Math.min(ROWS-1, row+dy));
        col = Math.max(0, Math.min(COLUMNS-1, col+dx));
        int next = row * COLUMNS + col;
        if (next < ids.length) cursorIndex = next;
    }
    boolean selectCursor() {return choose(ids[cursorIndex]);}
    private boolean choose(String id) {
        if(confirmedSteps>=steps || !contains(id))return false;
        if(focusSlot==1&&id.equals(picks[focusSide][0]))return false;
        assign(focusSide,focusSlot,id);advanceFocus();return true;
    }
    boolean swapFocusedTeam() {
        if(!chosen(focusSide,1))return false;
        String tmp = picks[focusSide][0];
        picks[focusSide][0] = picks[focusSide][1];
        picks[focusSide][1] = tmp;
        cursorIndex = indexOf(picks[focusSide][focusSlot]);
        return true;
    }
    boolean randomFocused() {
        if(confirmedSteps>=steps)return false;
        int next = random.nextInt(ids.length);
        if (ids[next].equals(picks[focusSide][1-focusSlot])) next = (next+1)%ids.length;
        return choose(ids[next]);
    }
    void toggleSettings() { showHelp = !showHelp; }
    private void assign(int side,int slot,String id) {
        int other = 1-slot;
        if (id.equals(picks[side][other])) picks[side][other] = picks[side][slot];
        picks[side][slot] = id;
        cursorIndex = indexOf(id);
    }
    private void advanceFocus() {
        confirmedSteps++;
        if(confirmedSteps<steps) {focusSide=confirmedSteps/2;focusSlot=confirmedSteps%2;}
        cursorIndex=indexOf(picks[focusSide][focusSlot]);
    }
    Result tap(float x,float y) {
        if (!open) return Result.NONE;
        // The bottom command bar uses exactly the same visible hit zones as the artwork.
        if (y>=FIGHT_TOP && y<=FIGHT_BOTTOM) {
            if (x>=FIGHT_LEFT && x<=FIGHT_RIGHT) return ready()?Result.CONFIRM:(selectCursor()?Result.CHANGED:Result.NONE);
            if (x>=BACK_LEFT && x<=BACK_RIGHT) return back();
            if (x>=SETTINGS_LEFT && x<=SETTINGS_RIGHT) {toggleSettings();return Result.CHANGED;}
            if (x>=SWAP_LEFT && x<=SWAP_RIGHT) {return swapFocusedTeam()?Result.CHANGED:Result.NONE;}
            if (x>=RANDOM_LEFT && x<=RANDOM_RIGHT) {return randomFocused()?Result.CHANGED:Result.NONE;}
        }
        if (showHelp) {showHelp=false;return Result.CHANGED;}
        if (inside(x,y,STAGE_LEFT,STAGE_TOP,STAGE_RIGHT,STAGE_BOTTOM)) {
            return moveStage(x<=STAGE_PREV_RIGHT?-1:1)?Result.CHANGED:Result.NONE;
        }
        int card=cardAt(x,y);
        if(card>=0)return choose(ids[card])?Result.CHANGED:Result.NONE;
        return Result.NONE;
    }
    private static boolean inside(float x,float y,float l,float t,float r,float b) {
        return x>=l&&x<=r&&y>=t&&y<=b;
    }
}
