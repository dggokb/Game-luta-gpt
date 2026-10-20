package com.gamelutagpt;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpriteMotionTest {
    private void tick(SpriteMotion m,float dt,boolean ground,boolean crouch,float vy,float dx,boolean front,boolean dash,boolean back,boolean combat,boolean lock) {
        m.update(dt,ground,crouch,vy,dx,front,dash,back,null,0,combat,lock);
    }
    private static CharacterDefinition.Animation clip(String id) {
        return GeneratedCharacters.defaultCharacter().animation(id);
    }
    @Test public void walkStepsAdvanceWithDistanceRatherThanDrawCalls() {
        CharacterDefinition.Animation walk=clip(SpriteMotion.Clip.WALK_FORWARD);
        // passo fixo por desenho, ou um quarto do ciclo quando cada desenho tem o passo dele (vídeo)
        assertTrue(walk.distancePerFrame>0 || walk.length>0);
        float stride=walk.distancePerFrame>0 ? walk.distancePerFrame : walk.length/4f;
        SpriteMotion m=new SpriteMotion();
        float travelled=0;
        for(int step=0;step<4;step++) {
            float dx=step==0?1:stride;travelled+=dx;
            tick(m,.016f,true,false,0,dx,true,false,false,false,false);
            assertEquals(SpriteMotion.Clip.WALK_FORWARD,m.clip);
            int expected=walk.frame(0,travelled);
            assertEquals(expected,m.frame());
            for(int i=0;i<100;i++)assertEquals(expected,m.frame());
        }
        assertNotEquals("Moving advances the step",walk.frame(0,1),m.frame());
    }
    @Test public void sameTravelHasSameFrameAcrossUpdateRates() {
        for(int rate:new int[]{20,60,120}) {
            SpriteMotion m=new SpriteMotion();for(int i=0;i<rate;i++)tick(m,1f/rate,true,false,0,300f/rate,true,false,false,false,false);
            assertEquals(clip(SpriteMotion.Clip.WALK_FORWARD).frame(0,300),m.frame());assertEquals(300,m.distance,.01);
        }
    }
    @Test public void backwardAndDashesHaveTheirOwnClips() {
        SpriteMotion m=new SpriteMotion();
        tick(m,.016f,true,false,0,-3,false,false,false,false,false);
        assertEquals(SpriteMotion.Clip.WALK_BACK,m.clip);
        assertEquals(clip(SpriteMotion.Clip.WALK_BACK).frame(m.time,m.distance),m.frame());
        tick(m,.016f,true,false,0,8,true,true,false,false,false);
        String run=GeneratedCharacters.defaultCharacter().animations.containsKey(SpriteMotion.Clip.DASH_START)
            ? SpriteMotion.Clip.DASH_START : SpriteMotion.Clip.DASH;
        assertEquals(run,m.clip);
        assertEquals(clip(run).frame(m.time,m.distance),m.frame());
        tick(m,.016f,true,false,0,-8,false,false,true,false,false);
        assertEquals(SpriteMotion.Clip.BACKDASH,m.clip);
        assertEquals(clip(SpriteMotion.Clip.BACKDASH).frame(m.time,m.distance),m.frame());
    }
    @Test public void dashStartsOnceLoopsAndBrakesBeforeIdle() {
        CharacterDefinition base=GeneratedCharacters.defaultCharacter();
        java.util.Map<String,CharacterDefinition.Animation> a=new java.util.LinkedHashMap<>(base.animations);
        CharacterDefinition.Animation run=base.animation(SpriteMotion.Clip.DASH);
        a.put(SpriteMotion.Clip.DASH_START,new CharacterDefinition.Animation(SpriteMotion.Clip.DASH_START,run.atlas,
            new int[]{0,1},new float[]{.05f,.05f},false,0));
        a.put(SpriteMotion.Clip.DASH_STOP,new CharacterDefinition.Animation(SpriteMotion.Clip.DASH_STOP,run.atlas,
            new int[]{0,1},new float[]{.05f,.05f},false,0));
        SpriteMotion m=new SpriteMotion(base.withAnimations("test",a));
        tick(m,.016f,true,false,0,8,true,true,false,false,false);
        assertEquals(SpriteMotion.Clip.DASH_START,m.clip);
        for(int i=0;i<8;i++)tick(m,.016f,true,false,0,8,true,true,false,false,false);
        assertEquals("After the start the run loops",SpriteMotion.Clip.DASH,m.clip);
        tick(m,.016f,true,false,0,0,true,false,false,false,false);
        assertEquals(SpriteMotion.Clip.DASH_STOP,m.clip);
        for(int i=0;i<8;i++)tick(m,.016f,true,false,0,0,true,false,false,false,false);
        assertEquals(SpriteMotion.Clip.IDLE,m.clip);
        // Stopping during the start brakes too; walking cuts the brake.
        tick(m,.016f,true,false,0,8,true,true,false,false,false);
        tick(m,.016f,true,false,0,0,true,false,false,false,false);
        assertEquals(SpriteMotion.Clip.DASH_STOP,m.clip);
        tick(m,.016f,true,false,0,3,true,false,false,false,false);
        assertEquals(SpriteMotion.Clip.WALK_FORWARD,m.clip);
        // Without start/stop art the run is the DASH loop alone.
        a.remove(SpriteMotion.Clip.DASH_START);a.remove(SpriteMotion.Clip.DASH_STOP);
        m=new SpriteMotion(base.withAnimations("plain",a));
        tick(m,.016f,true,false,0,8,true,true,false,false,false);
        assertEquals(SpriteMotion.Clip.DASH,m.clip);
        tick(m,.016f,true,false,0,0,true,false,false,false,false);
        assertEquals(SpriteMotion.Clip.IDLE,m.clip);
    }
    @Test public void distanceStartAndBrakingFollowTheBody() {
        CharacterDefinition base=GeneratedCharacters.defaultCharacter();
        java.util.Map<String,CharacterDefinition.Animation> a=new java.util.LinkedHashMap<>(base.animations);
        CharacterDefinition.Animation run=base.animation(SpriteMotion.Clip.DASH);
        // The start shows each drawing while the body covers its stretch: 10, then 30 units.
        a.put(SpriteMotion.Clip.DASH_START,new CharacterDefinition.Animation(SpriteMotion.Clip.DASH_START,run.atlas,
            new int[]{0,1},new float[0],false,0,new float[]{10f,30f}));
        a.put(SpriteMotion.Clip.DASH_STOP,new CharacterDefinition.Animation(SpriteMotion.Clip.DASH_STOP,run.atlas,
            new int[]{0,1},new float[]{.05f,.05f},false,0));
        SpriteMotion m=new SpriteMotion(base.withAnimations("test",a));
        m.update(.016f,true,false,0,4,true,true,false,null,0,false,false);
        assertEquals(SpriteMotion.Clip.DASH_START,m.clip);assertEquals(0,m.frame());
        m.update(.5f,true,false,0,8,true,true,false,null,0,false,false);
        assertEquals("time alone does not advance it",1,m.frame());
        m.update(.016f,true,false,0,30,true,true,false,null,0,false,false);
        m.update(.016f,true,false,0,6,true,true,false,null,0,false,false);
        assertEquals("40 units covered: the loop",SpriteMotion.Clip.DASH,m.clip);
        // Released: the body still slides, and the slide plays the brake.
        m.update(.016f,true,false,0,6,true,false,false,null,0,false,false,true);
        assertEquals(SpriteMotion.Clip.DASH_STOP,m.clip);
        m.update(.016f,true,false,0,3,true,false,false,null,0,false,false,true);
        assertEquals(SpriteMotion.Clip.DASH_STOP,m.clip);
        for(int i=0;i<8;i++)m.update(.016f,true,false,0,0,true,false,false,null,0,false,false,false);
        assertEquals(SpriteMotion.Clip.IDLE,m.clip);
    }
    @Test public void crouchEntryHoldAndExitDoNotChangeScaleOrLoopStandup() {
        SpriteMotion m=new SpriteMotion();
        CharacterDefinition.Animation crouch=clip(SpriteMotion.Clip.CROUCH);
        tick(m,.016f,true,true,0,0,true,false,false,false,false);
        assertEquals(SpriteMotion.Clip.CROUCH,m.clip);assertEquals(crouch.frame(.016f,0),m.frame());
        tick(m,.1f,true,true,0,0,true,false,false,false,false);assertEquals(crouch.frame(.116f,0),m.frame());
        // Holding down keeps the last crouch pose instead of looping the descent.
        for(float t=0;t<crouch.duration;t+=.1f)tick(m,.1f,true,true,0,0,true,false,false,false,false);
        assertEquals(crouch.frame(crouch.duration+1,0),m.frame());
        tick(m,.016f,true,false,0,0,true,false,false,false,false);assertEquals(SpriteMotion.Clip.RISE,m.clip);
        float rise=clip(SpriteMotion.Clip.RISE).duration;
        for(float t=0;t<=rise+.02f;t+=.016f)tick(m,.016f,true,false,0,0,true,false,false,false,false);
        assertEquals(SpriteMotion.Clip.IDLE,m.clip);
    }
    @Test public void holdingDownAfterACrouchingAttackStaysCrouched() {
        SpriteMotion m=new SpriteMotion();
        CharacterDefinition.Animation crouch=GeneratedCharacters.defaultCharacter().animation(SpriteMotion.Clip.CROUCH);
        tick(m,.5f,true,true,0,0,true,false,false,false,false);
        String attack=GeneratedCharacters.defaultCharacter().moves.get("2L").animation.id;
        m.update(.016f,true,true,0,0,true,false,false,attack,.05f,true,false);
        assertEquals(attack,m.clip);
        tick(m,.016f,true,true,0,0,true,false,false,false,false);
        assertEquals(SpriteMotion.Clip.CROUCH,m.clip);
        assertEquals("No stand-up between the attack and the held crouch",
            crouch.frame(crouch.duration+1,0),m.frame());
    }
    @Test public void jumpFallAndLandingAreOneShotAndFollowPhysics() {
        SpriteMotion m=new SpriteMotion();
        tick(m,.016f,false,false,-900,0,true,false,false,false,false);
        assertEquals(SpriteMotion.Clip.JUMP,m.clip);assertEquals(clip(SpriteMotion.Clip.JUMP).frame(m.time,0),m.frame());
        tick(m,.016f,false,false,200,0,true,false,false,false,false);
        assertEquals(SpriteMotion.Clip.FALL,m.clip);assertEquals(clip(SpriteMotion.Clip.FALL).frame(m.time,0),m.frame());
        tick(m,.016f,true,false,0,0,true,false,false,false,false);
        assertEquals(SpriteMotion.Clip.LAND,m.clip);assertEquals(clip(SpriteMotion.Clip.LAND).frame(m.time,0),m.frame());
        for(int i=0;i<10;i++)tick(m,.016f,true,false,0,0,true,false,false,false,false);
        assertEquals(SpriteMotion.Clip.IDLE,m.clip);
    }
    @Test public void attacksUseTheCombatClockAndSameAttackCanRestart() {
        SpriteMotion m=new SpriteMotion();
        for(CharacterDefinition.Move move:GeneratedCharacters.defaultCharacter().moves.values()) {
            if(move.animation==null)continue;
            String animation=move.animation.id;
            m.update(.016f,true,false,0,0,true,false,false,animation,0f,true,false);
            assertEquals(animation,m.clip);assertEquals(0,m.frame());
            m.update(.016f,true,false,0,0,true,false,false,animation,move.animation.duration-.001f,true,false);
            assertEquals(move.animation.frame(move.animation.duration-.001f,0),m.frame());
            // A repeated L/M/H must not inherit the preceding attack's recovery clock.
            m.update(.016f,true,false,0,0,true,false,false,animation,0f,true,false);
            assertEquals(0,m.frame());
        }
        tick(m,.016f,true,false,0,0,true,false,false,false,false);
        assertEquals(SpriteMotion.Clip.IDLE,m.clip);
    }
    @Test public void customAnimationIdUsesGenericMotionWithoutNewBranches() {
        CharacterDefinition base=GeneratedCharacters.defaultCharacter();
        java.util.Map<String,CharacterDefinition.Animation> a=new java.util.LinkedHashMap<>(base.animations);
        a.put("CUSTOM_PUNCH",new CharacterDefinition.Animation("CUSTOM_PUNCH",base.animation("LIGHT_JAB").atlas,
            new int[]{2,0,1,2},new float[]{.03f,.04f,.05f,.06f},false,0));
        SpriteMotion m=new SpriteMotion(base.withAnimations("test",a));
        m.update(.01f,true,false,0,0,true,false,false,"CUSTOM_PUNCH",.08f,true,false);
        assertEquals("CUSTOM_PUNCH",m.clip);assertEquals(1,m.frame());
    }
    @Test public void collisionGuardAndAttackCannotPlayWalking() {
        SpriteMotion m=new SpriteMotion();
        tick(m,.016f,true,false,0,0,true,false,false,false,false);assertEquals(SpriteMotion.Clip.IDLE,m.clip);
        tick(m,.016f,true,false,0,3,true,false,false,true,false);assertEquals(SpriteMotion.Clip.COMBAT,m.clip);
        tick(m,.016f,true,false,0,-3,false,false,false,false,true);assertEquals(SpriteMotion.Clip.COMBAT,m.clip);
    }
}
