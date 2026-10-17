package com.gamelutagpt;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** The generated atlas must contain the run start, a complete chronological dash cycle and the brake. */
@RunWith(RobolectricTestRunner.class)
public class P02DashRuntimeSizeTest {
    @Test public void realDashUsesAllVideoPhasesAndRetainsOriginalDuration() {
        CharacterDefinition fighter=GeneratedCharacters.get("p02");
        CharacterDefinition.Animation idle=fighter.animation(SpriteStates.IDLE);
        CharacterDefinition.Animation dash=fighter.animation(SpriteStates.DASH);
        assertEquals("p02_idle",idle.atlas.resource);
        assertEquals(56,idle.atlas.count);
        assertEquals(2f,idle.atlas.pixelScale,0);
        assertEquals(30*.041f,idle.duration,.0001f);
        assertEquals("p02_dash",dash.atlas.resource);
        assertEquals(2f,dash.atlas.pixelScale,0);
        assertTrue(dash.loop);
        // Arrancada (frames before loopFrom) plays once; the run cycle repeats from loopFrom.
        int start=dash.loopFrom;
        assertTrue("The run leaves the guard through the video's own start",start>0);
        assertEquals(0,dash.frame(0f,0f));
        CharacterDefinition.Animation stop=fighter.animation(SpriteStates.DASH_STOP);
        assertEquals("The brake shares the dash atlas",dash.atlas.resource,stop.atlas.resource);
        assertFalse(stop.loop);
        int n=countLoopFrames(dash);
        assertTrue("Real frames must fill a natural source cycle",n>=9);
        assertTrue("No fixed frame count assumptions",n<=16);
        assertEquals("Movement timing must be untouched",.260f,dash.duration-dash.entry,.001f);
        for(int i=0;i<n;i++) {
            float t=dash.entry+(i+.5f)*(dash.duration-dash.entry)/n;
            assertEquals("Native chronological phase "+i,start+i,dash.frame(t,0f));
        }
        assertEquals("The loop returns to the cycle, not to the start",start,dash.frame(dash.duration+.001f,0f));
        assertEquals("The brake follows the cycle in the atlas",start+n,stop.frame(0f,0f));
    }
    /** Distinct atlas frames shown during one pass of the loop part. */
    private static int countLoopFrames(CharacterDefinition.Animation a) {
        java.util.Set<Integer> seen=new java.util.HashSet<>();
        float loop=a.duration-a.entry;
        for(int k=0;k<2000;k++)seen.add(a.frame(a.entry+(k+.5f)*loop/2000,0f));
        return seen.size();
    }
}
