package com.gamelutagpt;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

/** Animation regression: P01 2H must display every pose in sequence. */
public class P01CrouchMovementTest {
    private static final CharacterDefinition P01=GeneratedCharacters.get("player_base");

    @Test public void lowAttacksKeepTheirContactAndAllSourceFrames() {
        String[] moves={"2L","2M","2H"};
        String[] clips={"CROUCH_LIGHT","CROUCH_MEDIUM","CROUCH_HEAVY"};
        int[][] expected={{2,4,6},{6,4,16},{9,6,14}};
        int[] impact={2,6,4};
        for(int i=0;i<moves.length;i++){
            AttackDefinition a=P01.attack(moves[i]);
            CharacterDefinition.Animation art=P01.animation(clips[i]);
            assertEquals(expected[i][0],a.startupFrames);
            assertEquals(expected[i][1],a.activeFrames);
            assertEquals(expected[i][2],a.recoveryFrames);
            float length=a.totalFrames/(float)CombatConfig.FPS;
            assertEquals("Impact sprite "+moves[i],impact[i],
                art.frame(art.timeFor(a.startupFrames/(float)CombatConfig.FPS,length),0f));
            List<Integer> displayed=new ArrayList<>();
            for(int t=0;t<a.totalFrames;t++){
                int frame=art.frame(art.timeFor((t+.5f)/(float)CombatConfig.FPS,length),0f);
                if(!displayed.isEmpty())assertTrue("A pose went backwards in "+moves[i],
                    frame>=displayed.get(displayed.size()-1));
                displayed.add(frame);
            }
            for(int p=0;p<art.atlas.count;p++) {
                assertTrue("Dropped "+moves[i]+" sprite pose "+p+"; played "+displayed,displayed.contains(p));
            }
        }
        assertEquals(AttackDefinition.Launch.LAUNCH,P01.attack("2H").launch);
        assertTrue(P01.attack("2H").cancelsInto(AttackDefinition.JUMP));
    }

    @Test public void dashLoopIsSlowerWithoutAlteringSpeed() {
        CharacterDefinition.Animation dash=P01.animation(SpriteStates.DASH);
        assertEquals(.288f,dash.duration,.0001f);
        assertEquals(6,dash.atlas.count);
        assertEquals(620f,new CombatConfig().dashSpeed,.001f);
    }
}
