package com.gamelutagpt;

import static org.junit.Assert.*;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/** End-to-end actual atlas/Canvas regression for INTRO -> IDLE. */
@RunWith(RobolectricTestRunner.class)
public class SpriteIntroTransitionTest {
    private static final int W=900,H=680;
    private static Bitmap render(SpriteFighterRenderer renderer,String state,float seconds) {
        Bitmap shot=Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888);
        renderer.motion.clip=state;
        renderer.motion.time=seconds;
        renderer.motion.distance=0f;
        renderer.draw(new Canvas(shot),450f,612f,1,false,false);
        return shot;
    }

    private static double averagePixelDistance(Bitmap a,Bitmap b) {
        long change=0;
        long pixels=0;
        for(int y=70;y<H;y+=3) for(int x=75;x<W-75;x+=3) {
            int p=a.getPixel(x,y),q=b.getPixel(x,y);
            change+=Math.abs((p>>>24)-(q>>>24));
            change+=Math.abs(((p>>>16)&255)-((q>>>16)&255));
            change+=Math.abs(((p>>>8)&255)-((q>>>8)&255));
            change+=Math.abs((p&255)-(q&255));
            pixels++;
        }
        return (double)change/(pixels*4*255);
    }

    @Test public void realP02IntroConvergesToFirstApprovedIdleFrame() {
        CharacterDefinition actor=GeneratedCharacters.get("p02");
        SpriteFighterRenderer renderer=new SpriteFighterRenderer(RuntimeEnvironment.getApplication(),actor);
        CharacterDefinition.Animation intro=actor.animation(SpriteStates.INTRO);
        CharacterDefinition.Animation idle=actor.animation(SpriteStates.IDLE);
        assertFalse(intro.loop);
        assertEquals(49,intro.atlas.count);
        assertEquals("Idle approved indices remain unmodified",15,idle.frame(0,0));
        assertEquals("Old policy would never fade on P02",0f,
            SpriteIntroTransition.blend(intro,0f),0f);
        float window=Math.min(SpriteIntroTransition.MAX_BLEND_SECONDS,intro.duration*.16f);
        assertEquals(0f,SpriteIntroTransition.blend(intro,intro.duration-window-.001f),0f);
        assertEquals(1f,SpriteIntroTransition.blend(intro,intro.duration),0f);
        assertTrue(SpriteIntroTransition.blend(intro,intro.duration-.012f)>.99f);

        Bitmap start=render(renderer,SpriteStates.INTRO,.025f);
        Bitmap finish=render(renderer,SpriteStates.INTRO,intro.duration-.012f);
        Bitmap next=render(renderer,SpriteStates.IDLE,0f);
        double originallyDifferent=averagePixelDistance(start,next);
        double boundaryError=averagePixelDistance(finish,next);
        assertTrue("Intro must start as genuinely different art",originallyDifferent>.004);
        assertTrue("The actual INTRO -> IDLE transition must be nearly pixel-identical",
            boundaryError<.0025);
        assertTrue("The boundary must improve sharply over a naive hard cut",
            boundaryError<originallyDifferent*.15);
        start.recycle();finish.recycle();next.recycle();
    }

    @Test public void genericPolicyUsesAnimationClockInsteadOfP02FrameIndices() {
        for(String id:new String[]{"p02","p03","p04"}) {
            CharacterDefinition actor=GeneratedCharacters.get(id);
            CharacterDefinition.Animation intro=actor.animations.get(SpriteStates.INTRO);
            CharacterDefinition.Animation idle=actor.animations.get(SpriteStates.IDLE);
            assertNotNull(id+" INTRO required",intro);
            assertNotNull(id+" IDLE required",idle);
            assertEquals(0f,SpriteIntroTransition.blend(intro,0),0f);
            assertEquals(1f,SpriteIntroTransition.blend(intro,intro.duration),0f);
            assertTrue(SpriteIntroTransition.blend(intro,intro.duration-.01f)>.90f);
        }
    }
}
