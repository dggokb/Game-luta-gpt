package com.gamelutagpt;

import static org.junit.Assert.*;
import org.junit.Test;

public class CharacterSelectTest {
    private CharacterSelect make() {
        return new CharacterSelect(new String[]{"p01","p02","p03","p04","p05","p06","p07","p08"});
    }
    @Test public void templateHasOriginalFourByFourGridAndTeamAreas() {
        CharacterSelect s=make();
        assertEquals(4,CharacterSelect.COLUMNS);
        assertEquals(4,CharacterSelect.ROWS);
        for(int i=0;i<s.ids.length;i++)
            assertEquals(i,s.cardAt(s.cardLeft(i)+10f,s.cardTop(i)+10f));
        assertEquals(-1,s.cardAt(s.cardLeft(8)+10f,s.cardTop(8)+10f));
        assertEquals(1,s.stageCount());
    }
    @Test public void twoTeamsAreIndependentAndValid() {
        CharacterSelect s=make();s.open("p01","p03","p02","p04");
        assertEquals("p01",s.point());assertEquals("p03",s.partner());
        assertEquals("p02",s.opponentPoint());assertEquals("p04",s.opponentPartner());
        assertTrue(s.ready());
        s.tap(s.cardLeft(2)+10,s.cardTop(2)+10);
        assertEquals("p03",s.point());assertEquals("p01",s.partner());
    }
    @Test public void selectingBothSlotsChangesP1ThenP2() {
        CharacterSelect s=make();s.open("p01","p03","p02","p04");
        s.tap(s.cardLeft(1)+10,s.cardTop(1)+10);
        assertEquals("p02",s.point());assertEquals(1,s.focusSlot());
        s.tap(s.cardLeft(4)+10,s.cardTop(4)+10);
        assertEquals("p05",s.partner());assertEquals(1,s.focusSide());
        s.tap(s.cardLeft(3)+10,s.cardTop(3)+10);
        assertEquals("p04",s.opponentPoint());assertEquals("p02",s.opponentPartner());
    }
    @Test public void footerActionsAreClickableAndReflectActualImage() {
        CharacterSelect s=make();s.open("p01","p03","p02","p04");
        assertEquals(CharacterSelect.Result.CHANGED,s.tap(660,699));
        assertTrue(s.settingsOpen());
        s.tap(600,400);assertFalse(s.settingsOpen());
        assertEquals(CharacterSelect.Result.CHANGED,s.tap(914,699));
        assertEquals("p03",s.point());assertEquals("p01",s.partner());
        assertEquals(CharacterSelect.Result.CONFIRM,s.tap(370,700));
        assertEquals(CharacterSelect.Result.CANCEL,s.tap(503,700));
    }
    @Test public void dpadsAndRandomCannotDuplicateTheTwoSlots() {
        CharacterSelect s=make();s.open("p01","p03","p02","p04");
        s.moveCursor(1,0);assertEquals(1,s.cursorIndex());s.selectCursor();
        assertEquals("p02",s.point());
        for(int i=0;i<100;i++) {s.tap(1131,698);assertTrue(s.ready());}
    }
    @Test public void lockedGridSlotsNeverAcceptInput() {
        CharacterSelect s=make();s.open("p01","p03","p02","p04");
        assertEquals(CharacterSelect.Result.NONE,s.tap(s.cardLeft(9)+12,s.cardTop(9)+12));
    }
    @Test public void perspectiveHitTestsIncludeTheWiderBottomRowsAndExcludeGaps() {
        CharacterSelect s=new CharacterSelect(new String[]{"p01","p02","p03","p04","p05","p06","p07","p08",
            "p09","p10","p11","p12","p13","p14","p15","p16"});
        float[] q=new float[8];
        for(int i=0;i<16;i++) {
            s.cardCorners(i,q);
            float x=(q[0]+q[2]+q[4]+q[6])/4f,y=(q[1]+q[5])/2f;
            assertEquals("visible cell "+i,i,s.cardAt(x,y));
        }
        s.cardCorners(12,q);
        assertTrue("bottom edge is wider than top",q[0]<CharacterSelect.GRID_X);
        assertEquals(12,s.cardAt(q[6]+4,q[5]-4));
        s.cardCorners(0,q);
        assertEquals(-1,s.cardAt(q[2]+4,q[3]+1));
    }

    @Test public void illustratedFutureFacesRemainLockedAndDoNotChangeTeams() {
        CharacterSelect s=make();s.open("p01","p08","p03","p06");
        float[] q=new float[8];
        for(int i=8;i<16;i++) {
            s.cardCorners(i,q);
            assertEquals(CharacterSelect.Result.NONE,
                s.tap((q[0]+q[2]+q[4]+q[6])/4f,(q[1]+q[5])/2f));
            assertEquals("p01",s.point());assertEquals("p08",s.partner());
            assertEquals("p03",s.opponentPoint());assertEquals("p06",s.opponentPartner());
        }
    }

}
