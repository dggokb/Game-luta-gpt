package com.gamelutagpt;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Four native P02 dash poses at the approved size, with original atlas intact. */
@RunWith(RobolectricTestRunner.class)
public class P02DashRuntimeSizeTest {
    @Test public void dashUsesNativeCycleWithOriginalSizeAndAtlas() {
        CharacterDefinition character=GeneratedCharacters.get("p02");
        CharacterDefinition.Animation idle=character.animation(SpriteStates.IDLE);
        CharacterDefinition.Animation dash=character.animation(SpriteStates.DASH);
        assertEquals("p02_idle",idle.atlas.resource);
        assertEquals(56,idle.atlas.count);
        assertEquals(2f,idle.atlas.pixelScale,0f);
        assertEquals(30*0.041f,idle.duration,0.0001f);
        assertEquals("p02_dash",dash.atlas.resource);
        assertEquals(531,dash.atlas.width);
        assertEquals(448,dash.atlas.height);
        assertEquals(282,dash.atlas.rootX);
        assertEquals(432,dash.atlas.rootY);
        assertEquals(6,dash.atlas.count);
        assertEquals(2f,dash.atlas.pixelScale,0f);
        assertTrue(dash.loop);
        assertEquals(4*.065f,dash.duration,0.0001f);
        for(int i=0;i<4;i++){
            assertEquals("DASH native frame "+i,i,dash.frame((i+0.5f)*.065f,0f));
        }
        assertEquals(0,dash.frame(dash.duration+.0325f,0f));
    }
}
