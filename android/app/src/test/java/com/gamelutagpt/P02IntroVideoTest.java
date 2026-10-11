package com.gamelutagpt;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Actual generated runtime contract for original-video P02 INTRO. */
@RunWith(RobolectricTestRunner.class)
public class P02IntroVideoTest {
    @Test public void introPlaysAllRealVideoPhasesInOrderWithoutLooping() {
        CharacterDefinition hero=GeneratedCharacters.get("p02");
        CharacterDefinition.Animation intro=hero.animation(SpriteStates.INTRO);
        CharacterDefinition.Animation idle=hero.animation(SpriteStates.IDLE);
        CharacterDefinition.Animation dash=hero.animation(SpriteStates.DASH);
        assertNotNull(intro);
        assertEquals("p02_intro",intro.atlas.resource);
        assertEquals(2f,intro.atlas.pixelScale,0f);
        assertFalse("INTRO must stop on its last video pose",intro.loop);
        assertTrue("Intro must use natural in-between frames from source video",intro.atlas.count>=45);
        assertEquals("Intro must retain cinematic total duration",4.160f,intro.duration,.001f);
        for(int i=0;i<intro.atlas.count;i++) {
            assertEquals("Every video phase must be reachable in engine at frame "+i,
                         i,intro.frame(intro.timeOfFrame(i),0f));
        }
        assertEquals(intro.atlas.count-1,intro.frame(intro.duration+.5f,0f));
        assertEquals("Approved IDLE must remain intact",56,idle.atlas.count);
        assertEquals("Approved DASH must remain intact",12,dash.atlas.count);
        assertEquals(.260f,dash.duration,.001f);
    }
}
