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

    @Test public void aKnockoutHighUpInASuperJumpBringsTheCameraBackToTheGround() {
        CameraRig c=new CameraRig();
        final float winnerX=760f;
        // Both high up in a Super Jump: the camera climbs with them.
        for(int i=0;i<90;i++)
            c.updateForRound(CombatConfig.DT,winnerX,900f,Arena.GROUND_Y-900f,true,false);
        float raised=c.top,x=c.x,zoom=c.zoom;
        float groundTop=Arena.clamp(Arena.GROUND_Y-CameraRig.CAMERA_GROUND_SCREEN_Y/zoom,
            Arena.WORLD_TOP,CameraRig.GROUND_CAMERA_TOP);
        assertTrue("the camera went up with the jump",raised<groundTop-200f);
        // KO up there: the bodies fall back to the floor.
        float height=900f;
        for(int frame=0;frame<240;frame++) {
            height=Math.max(160f,height-12f);
            c.updateForRound(CombatConfig.DT,winnerX,900f,Arena.GROUND_Y-height,false,true);
            assertEquals("no pan after the KO",x,c.x,.00001f);
            assertEquals("no zoom after the KO",zoom,c.zoom,.00001f);
        }
        assertEquals("back to the ground framing",groundTop,c.top,2f);
    }
}
