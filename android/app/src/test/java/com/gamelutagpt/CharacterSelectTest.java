package com.gamelutagpt;

import static org.junit.Assert.*;
import org.junit.Test;

public class CharacterSelectTest {
    private CharacterSelect make() {
        CharacterSelect s=new CharacterSelect(new String[]{"p01","p02","p03","p04","p05","p06","p07","p08"});
        s.open("p01","p08","p03","p06");return s;
    }
    private CharacterSelect.Result pick(CharacterSelect s,int i) {
        float[] q=new float[8];s.cardCorners(i,q);
        return s.tap((q[0]+q[2]+q[4]+q[6])/4,(q[1]+q[5])/2);
    }
    private void finish(CharacterSelect s) {pick(s,0);pick(s,1);pick(s,2);pick(s,3);}

    @Test public void startsWithOnlyPlayerOneActiveAndNoConfirmedSlots() {
        CharacterSelect s=make();assertFalse(s.ready());assertEquals(0,s.step());
        assertEquals(0,s.focusSide());assertEquals(0,s.focusSlot());
        for(int side=0;side<2;side++)for(int slot=0;slot<2;slot++)assertFalse(s.chosen(side,slot));
    }
    @Test public void fourPortraitTapsChooseBothTeamsInOrderWithoutSlotTaps() {
        CharacterSelect s=make();
        assertEquals(CharacterSelect.Result.CHANGED,pick(s,4));assertEquals("p05",s.point());
        assertEquals(0,s.focusSide());assertEquals(1,s.focusSlot());assertFalse(s.ready());
        pick(s,1);assertEquals("p02",s.partner());assertEquals(1,s.focusSide());assertEquals(0,s.focusSlot());
        pick(s,2);assertEquals("p03",s.opponentPoint());assertEquals(1,s.focusSlot());
        pick(s,6);assertEquals("p07",s.opponentPartner());assertEquals(4,s.step());assertTrue(s.ready());
        assertEquals(CharacterSelect.Result.CONFIRM,s.tap(370,700));
    }
    @Test public void teamPanelsAndHeroesCannotJumpAheadInTheSequence() {
        CharacterSelect s=make();
        for(float[] p:new float[][]{{110,420},{110,560},{1150,420},{1150,560},{60,100},{1210,100}})
            assertEquals(CharacterSelect.Result.NONE,s.tap(p[0],p[1]));
        assertEquals(0,s.step());pick(s,0);s.tap(1150,420);assertEquals(1,s.step());assertEquals(0,s.focusSide());
    }
    @Test public void duplicateTagIsRejectedWithoutReplacingConfirmedPoint() {
        CharacterSelect s=make();pick(s,4);assertEquals(CharacterSelect.Result.NONE,pick(s,4));
        assertEquals(1,s.step());assertEquals("p05",s.point());assertFalse(s.chosen(0,1));
        pick(s,1);pick(s,2);assertEquals(CharacterSelect.Result.NONE,pick(s,2));
        assertEquals(3,s.step());assertEquals("p03",s.opponentPoint());
    }
    @Test public void conceptsNineThroughSixteenStayLockedAtEveryStep() {
        CharacterSelect s=make();
        for(int stage=0;stage<=4;stage++) {
            for(int i=8;i<16;i++)assertEquals(CharacterSelect.Result.NONE,pick(s,i));
            assertEquals(stage,s.step());assertFalse(s.contains("p09"));assertFalse(s.contains("p16"));
            if(stage<4)pick(s,stage);
        }
    }
    @Test public void backReopensThePreviousSlotAndPreservesEarlierChoices() {
        CharacterSelect s=make();finish(s);
        assertEquals(CharacterSelect.Result.CHANGED,s.back());assertEquals(3,s.step());assertFalse(s.ready());
        assertEquals("p03",s.opponentPoint());assertFalse(s.chosen(1,1));
        pick(s,6);assertTrue(s.ready());assertEquals("p07",s.opponentPartner());
        for(int i=0;i<4;i++)assertEquals(CharacterSelect.Result.CHANGED,s.back());
        assertEquals(CharacterSelect.Result.CANCEL,s.back());
    }
    @Test public void confirmButtonChoosesCurrentCursorUntilAllFourAreConfirmed() {
        CharacterSelect s=make();
        for(int i=0;i<4;i++) {assertEquals(CharacterSelect.Result.CHANGED,s.tap(370,700));assertEquals(i+1,s.step());}
        assertEquals(CharacterSelect.Result.CONFIRM,s.tap(370,700));
        assertEquals(CharacterSelect.Result.NONE,pick(s,7));assertEquals(4,s.step());
    }
    @Test public void randomFollowsTheSameFourStepsAndNeverDuplicatesPartner() {
        for(int attempt=0;attempt<100;attempt++) {
            CharacterSelect s=make();for(int i=0;i<4;i++)assertTrue(s.randomFocused());
            assertTrue(s.ready());assertNotEquals(s.point(),s.partner());assertNotEquals(s.opponentPoint(),s.opponentPartner());
            assertFalse(s.randomFocused());
        }
    }
    @Test public void swapRequiresTwoConfirmedMembersAndKeepsSequenceComplete() {
        CharacterSelect s=make();assertFalse(s.swapFocusedTeam());finish(s);
        assertTrue(s.swapFocusedTeam());assertEquals("p04",s.opponentPoint());assertEquals("p03",s.opponentPartner());assertTrue(s.ready());
    }
    @Test public void helpDoesNotAdvanceOrUndoTheSequence() {
        CharacterSelect s=make();pick(s,0);s.toggleSettings();assertTrue(s.settingsOpen());
        assertEquals(CharacterSelect.Result.CHANGED,s.back());assertFalse(s.settingsOpen());assertEquals(1,s.step());
    }
    @Test public void perspectiveProjectionAndGapsMatchTheVisibleFourByFourGrid() {
        CharacterSelect s=new CharacterSelect(new String[]{"p01","p02","p03","p04","p05","p06","p07","p08",
            "p09","p10","p11","p12","p13","p14","p15","p16"});
        float[] q=new float[8];
        for(int i=0;i<16;i++){s.cardCorners(i,q);assertEquals(i,s.cardAt((q[0]+q[2]+q[4]+q[6])/4,(q[1]+q[5])/2));}
        s.cardCorners(12,q);assertTrue(q[0]<CharacterSelect.GRID_X);assertEquals(12,s.cardAt(q[6]+4,q[5]-4));
        s.cardCorners(0,q);assertEquals(-1,s.cardAt(q[2]+4,q[3]+1));assertEquals(1,s.stageCount());
    }
}
