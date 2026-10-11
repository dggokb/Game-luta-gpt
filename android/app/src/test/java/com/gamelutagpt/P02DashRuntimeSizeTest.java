package com.gamelutagpt;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** The generated atlas must contain a complete chronological dash cycle. */
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
        assertTrue("Real frames must fill a natural source cycle",dash.atlas.count>=9);
        assertTrue("No fixed frame count assumptions",dash.atlas.count<=16);
        assertEquals("Movement timing must be untouched",.260f,dash.duration,.001f);
        int n=dash.atlas.count;
        for(int i=0;i<n;i++) {
            float t=(i+.5f)*dash.duration/n;
            assertEquals("Native chronological phase "+i,i,dash.frame(t,0f));
        }
        assertEquals(0,dash.frame(dash.duration+.001f,0f));
    }
}
