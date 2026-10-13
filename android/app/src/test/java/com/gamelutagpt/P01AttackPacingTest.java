package com.gamelutagpt;

import org.junit.Test;
import static org.junit.Assert.*;

/** P01 artwork pacing: 10-13% slower without introducing startup/active-frame regressions. */
public class P01AttackPacingTest {
    private static final CharacterDefinition P01=GeneratedCharacters.get("player_base");
    private static final String[] MOVES={"L","M","jL","jM","jH"};
    private static final String[] CLIPS={"LIGHT_JAB","MEDIUM_KICK","JUMP_LIGHT","JUMP_MEDIUM","JUMP_HEAVY"};
    private static final int[][] TIMINGS={
        {2,4,5}, {4,6,8}, {4,2,5}, {6,4,8}, {9,6,12}
    };
    private static final int[] PREVIOUS_TOTALS={10,16,10,16,24};
    private static final int[] OLD_ART_DURATION_MS={169,268,167,267,400};
    private static final int[] IMPACT_FRAMES={2,3,2,2,2};

    @Test public void slowerArtLeavesHitFramesAndComboWindowsUnchanged() {
        for(int i=0;i<MOVES.length;i++) {
            AttackDefinition a=P01.attack(MOVES[i]);
            int[] expected=TIMINGS[i];
            assertEquals(MOVES[i]+" startup changed",expected[0],a.startupFrames);
            assertEquals(MOVES[i]+" active duration changed",expected[1],a.activeFrames);
            assertEquals(MOVES[i]+" recovery",expected[2],a.recoveryFrames);
            CharacterDefinition.Animation art=P01.animation(CLIPS[i]);
            float oldLength=OLD_ART_DURATION_MS[i]/1000f;
            float factor=art.duration/oldLength;
            float gameplayFactor=a.totalFrames/(float)PREVIOUS_TOTALS[i];
            assertEquals(MOVES[i]+" art must remain proportional to gameplay",gameplayFactor,factor,.02f);
            assertTrue(MOVES[i]+" must be moderately slower",factor>=1.095f && factor<=1.13f);
            float contactSeconds=a.startupFrames/(float)CombatConfig.FPS;
            int contactFrame=art.frame(art.timeFor(contactSeconds,a.totalFrames/(float)CombatConfig.FPS),0);
            assertEquals(MOVES[i]+" impact art must still connect in its real hit frame",
                IMPACT_FRAMES[i],contactFrame);
        }
    }

    @Test public void normalAndAirComboCancelRoutesAreUnchanged() {
        assertTrue(P01.attack("L").cancelsInto("M"));
        assertTrue(P01.attack("M").cancelsInto("H"));
        assertTrue(P01.attack("jL").cancelsInto("jM"));
        assertTrue(P01.attack("jM").cancelsInto("jH"));
        assertEquals(10,P01.attack("H").startupFrames);
        assertEquals(AttackDefinition.Launch.WALL_BOUNCE,P01.attack("H").launch);
        assertEquals(AttackDefinition.Launch.WALL_BOUNCE,P01.attack("jH").launch);
    }
}
