package com.gamelutagpt;

import static org.junit.Assert.*;
import org.junit.Test;

public class CharacterSelectTest {
    private CharacterSelect make() {
        return new CharacterSelect(new String[]{"p01","p02","p03","p04","p05","p06","p07","p08"});
    }

    @Test public void opensWithSeparateTeamsAndPointTag() {
        CharacterSelect s = make();
        s.open("p01","p03","p02","p04");
        assertTrue(s.isOpen());
        assertTrue(s.ready());
        assertEquals("p01", s.point());
        assertEquals("p03", s.partner());
        assertEquals("p02", s.opponentPoint());
        assertEquals("p04", s.opponentPartner());
    }

    @Test public void canAssignPointThenTagForBothTeams() {
        CharacterSelect s = make();
        s.open("p01","p03","p02","p04");
        // P1 point from grid row 1, col 2: p02
        assertEquals(CharacterSelect.Result.CHANGED,
            s.tap(s.cardLeft(1) + 15, s.cardTop(1) + 15));
        assertEquals("p02", s.point());
        assertEquals(1, s.focusSlot());
        // P1 tag from grid row 2, col 1: p05
        s.tap(s.cardLeft(4) + 15, s.cardTop(4) + 15);
        assertEquals("p05", s.partner());
        assertEquals(1, s.focusSide());
        assertEquals(0, s.focusSlot());
        // P2 point from grid row 1, col 4: p04, swaps previous point and TAG
        s.tap(s.cardLeft(3) + 15, s.cardTop(3) + 15);
        assertEquals("p04", s.opponentPoint());
        assertEquals("p02", s.opponentPartner());
        assertTrue(s.ready());
    }

    @Test public void selectedSlotsAndLockedCardsAreDistinctAndTappable() {
        CharacterSelect s = make();
        s.open("p01","p03","p02","p04");
        s.tap(CharacterSelect.TEAM_RIGHT + 35, s.slotTop(1) + 30);
        assertEquals(1, s.focusSide());
        assertEquals(1, s.focusSlot());
        float lockedX = s.cardLeft(8) + 10, lockedY = s.cardTop(8) + 10;
        assertEquals(CharacterSelect.Result.NONE, s.tap(lockedX, lockedY));
        assertEquals("p04", s.opponentPartner());
        assertEquals(CharacterSelect.Result.CONFIRM,
            s.tap(CharacterSelect.FIGHT_LEFT + 5, CharacterSelect.FIGHT_TOP + 5));
        assertEquals(CharacterSelect.Result.CANCEL,
            s.tap(CharacterSelect.BACK_LEFT + 5, CharacterSelect.BACK_TOP + 5));
    }

    @Test public void duplicateSelectionSwapsSlots() {
        CharacterSelect s = make();
        s.open("p01","p03","p02","p04");
        s.tap(s.cardLeft(2) + 15, s.cardTop(2) + 15);
        assertEquals("p03",s.point());
        assertEquals("p01",s.partner());
    }

    @Test public void invalidIdsAreReplacedAndRandomNeverDuplicatesWithinTeam() {
        CharacterSelect s=make();
        s.open("missing", "p01", "bad", "bad");
        assertTrue(s.ready());
        for(int i=0;i<100;i++){
            s.tap(CharacterSelect.RANDOM_LEFT+5,CharacterSelect.RANDOM_TOP+5);
            assertTrue(s.ready());
        }
    }
}
