package com.gamelutagpt;

import static org.junit.Assert.*;
import org.junit.Test;

/** Prevent false victory movement from the fight camera after KO. */
public class CameraRigRoundEndTest {
    private static float winnerScreenX(CameraRig c,float winnerX) {
        float half=Arena.VW/(2f*c.zoom);
        float left=Arena.clamp(c.x-half,0f,Arena.WORLD_WIDTH-2f*half);
        return (winnerX-left)*c.zoom;
    }

    @Test public void koLocksCameraFocusZoomAndRenderedWinnerPosition() {
        CameraRig c=new CameraRig();
        final float winnerX=760f;
        for(int i=0;i<45;i++)
            c.updateForRound(CombatConfig.DT,winnerX,1200f,
                Arena.GROUND_Y-160f,false,false);
        float x=c.x,top=c.top,zoom=c.zoom,screenX=winnerScreenX(c,winnerX);
        // Real KO body continues falling and being knocked back.
        // The winner has stopped; visual victory must not follow the loser.
        for(int frame=0;frame<160;frame++) {
            float loserX=1200f+frame*3.5f;
            float loserHeight=Arena.GROUND_Y-(160f+(frame%33)*6f);
            c.updateForRound(CombatConfig.DT,winnerX,loserX,loserHeight,
                             frame%20<10,true);
            assertEquals("Camera must not pan during victory",x,c.x,.00001f);
            assertEquals("Camera must not pan vertically during victory",top,c.top,.00001f);
            assertEquals("Camera must not zoom during victory",zoom,c.zoom,.00001f);
            assertEquals("Winner must stay at exactly the same screen X",
                         screenX,winnerScreenX(c,winnerX),.00001f);
        }
        // Another round can still reactivate normal framing.
        for(int i=0;i<80;i++)
            c.updateForRound(CombatConfig.DT,winnerX,2000f,
                             Arena.GROUND_Y-160f,false,false);
        assertTrue("Camera follows combat again after round reset",c.x>x+25f);
    }
}
