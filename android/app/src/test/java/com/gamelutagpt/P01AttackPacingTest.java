package com.gamelutagpt;

import org.junit.Test;
import static org.junit.Assert.*;

/** P01 artwork pacing: the video art fits each move without startup/active-frame regressions. */
public class P01AttackPacingTest {
    private static final CharacterDefinition P01=GeneratedCharacters.get("player_base");
    private static final String[] MOVES={"L","M","jL","jM","jH"};
    private static final String[] CLIPS={"LIGHT_JAB","MEDIUM_KICK","JUMP_LIGHT","JUMP_MEDIUM","JUMP_HEAVY"};
    private static final int[][] TIMINGS={
        // Tempos revistos em 11/11 (o jogador achou o L lento): jab de 4, soco de 6, jL ativo 3.
        {4,2,6}, {6,4,11}, {4,3,5}, {6,4,8}, {9,6,12}
    };
    /** O quadro do impacto de cada clipe (os vídeos novos de 11/2026, montagens/player_base.json). */
    private static final int[] IMPACT_FRAMES={6,5,5,7,7};

    @Test public void slowerArtLeavesHitFramesAndComboWindowsUnchanged() {
        for(int i=0;i<MOVES.length;i++) {
            AttackDefinition a=P01.attack(MOVES[i]);
            int[] expected=TIMINGS[i];
            assertEquals(MOVES[i]+" startup changed",expected[0],a.startupFrames);
            assertEquals(MOVES[i]+" active duration changed",expected[1],a.activeFrames);
            assertEquals(MOVES[i]+" recovery",expected[2],a.recoveryFrames);
            CharacterDefinition.Animation art=P01.animation(CLIPS[i]);
            // A arte (a cadência do vídeo) cabe inteira no tempo do golpe.
            assertEquals(MOVES[i]+" art fits the move",a.totalFrames/(float)CombatConfig.FPS,art.duration,.02f);
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
