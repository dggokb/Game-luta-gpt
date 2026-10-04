package com.gamelutagpt;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpriteMotionTest {
    private void tick(SpriteMotion m,float dt,boolean ground,boolean crouch,float vy,float dx,boolean front,boolean dash,boolean back,boolean combat,boolean lock) {
        m.update(dt,ground,crouch,vy,dx,front,dash,back,false,combat,lock);
    }
    @Test public void allFourStepsAdvanceWithDistanceRatherThanDrawCalls() {
        SpriteMotion m=new SpriteMotion();
        for(int frame=0;frame<4;frame++) {
            tick(m,.016f,true,false,0,frame==0?1:36,true,false,false,false,false);
            assertEquals(frame,m.frame());assertEquals(SpriteMotion.Clip.WALK_FORWARD,m.clip);
            for(int i=0;i<100;i++)assertEquals(frame,m.frame());
        }
    }
    @Test public void sameTravelHasSameFrameAcrossUpdateRates() {
        for(int rate:new int[]{20,60,120}) {
            SpriteMotion m=new SpriteMotion();for(int i=0;i<rate;i++)tick(m,1f/rate,true,false,0,300f/rate,true,false,false,false,false);
            assertEquals(0,m.frame());assertEquals(300,m.distance,.01);
        }
    }
    @Test public void backwardAndDashesHaveTheirOwnClips() {
        SpriteMotion m=new SpriteMotion();
        tick(m,.016f,true,false,0,-3,false,false,false,false,false);assertEquals(4,m.frame());
        tick(m,.016f,true,false,0,8,true,true,false,false,false);assertEquals(12,m.frame());
        tick(m,.016f,true,false,0,-8,false,false,true,false,false);assertEquals(14,m.frame());
    }
    @Test public void crouchEntryHoldAndExitDoNotChangeScaleOrLoopStandup() {
        SpriteMotion m=new SpriteMotion();
        tick(m,.016f,true,true,0,0,true,false,false,false,false);assertEquals(8,m.frame());
        tick(m,.1f,true,true,0,0,true,false,false,false,false);assertEquals(9,m.frame());
        tick(m,.016f,true,false,0,0,true,false,false,false,false);assertEquals(SpriteMotion.Clip.RISE,m.clip);
        for(int i=0;i<10;i++)tick(m,.016f,true,false,0,0,true,false,false,false,false);
        assertEquals(SpriteMotion.Clip.IDLE,m.clip);
    }
    @Test public void jumpFallAndLandingAreOneShotAndFollowPhysics() {
        SpriteMotion m=new SpriteMotion();
        tick(m,.016f,false,false,-900,0,true,false,false,false,false);assertEquals(10,m.frame());
        tick(m,.016f,false,false,200,0,true,false,false,false,false);assertEquals(11,m.frame());
        tick(m,.016f,true,false,0,0,true,false,false,false,false);assertEquals(15,m.frame());
        for(int i=0;i<10;i++)tick(m,.016f,true,false,0,0,true,false,false,false,false);
        assertEquals(SpriteMotion.Clip.IDLE,m.clip);
    }
    @Test public void lightJabUsesThreeOrderedFramesAndRecoversToIdle() {
        SpriteMotion m=new SpriteMotion();
        m.update(.016f,true,false,0,0,true,false,false,true,true,false);
        assertEquals(SpriteMotion.Clip.LIGHT_JAB,m.clip);assertEquals(0,m.frame());
        m.update(.040f,true,false,0,0,true,false,false,true,true,false);
        assertEquals(1,m.frame());
        m.update(.060f,true,false,0,0,true,false,false,true,true,false);
        assertEquals(2,m.frame());
        m.update(.016f,true,false,0,0,true,false,false,false,false,false);
        assertEquals(SpriteMotion.Clip.IDLE,m.clip);
    }
    @Test public void collisionGuardAndAttackCannotPlayWalking() {
        SpriteMotion m=new SpriteMotion();
        tick(m,.016f,true,false,0,0,true,false,false,false,false);assertEquals(SpriteMotion.Clip.IDLE,m.clip);
        tick(m,.016f,true,false,0,3,true,false,false,true,false);assertEquals(SpriteMotion.Clip.COMBAT,m.clip);
        tick(m,.016f,true,false,0,-3,false,false,false,false,true);assertEquals(SpriteMotion.Clip.COMBAT,m.clip);
    }
}
