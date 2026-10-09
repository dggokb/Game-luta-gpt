package com.gamelutagpt;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpriteTransitionPolicyTest {
    private final CharacterDefinition c=GeneratedCharacters.defaultCharacter();
    private static void tick(SpriteMotion m,float dt,String anim,float animTime) {
        m.update(dt,true,false,0f,0f,true,false,false,anim,animTime,false,false);
    }

    @Test public void everyManifestPairIsSafeAndBounded() {
        for(String from:c.animations.keySet()) for(String to:c.animations.keySet()) {
            float seconds=SpriteTransitionPolicy.duration(c,from,to);
            assertTrue(from+" -> "+to,Float.isFinite(seconds) && seconds>=0 && seconds<=.14f);
            assertEquals(0f,SpriteTransitionPolicy.duration(c,from,from),0f);
        }
    }
    @Test public void introToIdleBlendsAndBecomesFullyIdle() {
        SpriteMotion m=new SpriteMotion(c);
        tick(m,.016f,SpriteStates.INTRO,.5f);
        tick(m,.016f,null,0f);
        assertEquals(SpriteStates.IDLE,m.clip);
        assertEquals(SpriteStates.INTRO,m.previousClip);
        assertTrue(m.outgoingAlpha()>0);
        for(int i=0;i<11;i++)tick(m,.016f,null,0f);
        assertEquals(0f,m.outgoingAlpha(),0.0001f);
        assertNull(m.previousClip);
    }
    @Test public void attackStartsWithImmediateCombatAccuratePose() {
        SpriteMotion m=new SpriteMotion(c);
        tick(m,.016f,null,0f);
        tick(m,.016f,c.moves.get("L").animation.id,.035f);
        assertNull(m.previousClip);
        assertEquals(0f,m.outgoingAlpha(),0f);
        assertEquals(c.moves.get("L").animation.frame(.035f,0),m.frame());
    }
    @Test public void hitAndDefenseTakeVisualPriorityOverBlend() {
        SpriteMotion m=new SpriteMotion(c);
        tick(m,.016f,null,0f);
        tick(m,.016f,SpriteStates.HIT_STAND,.05f);
        assertEquals(0f,m.outgoingAlpha(),0f);
        tick(m,.016f,SpriteStates.DEFENSE_STAND,.1f);
        assertEquals(0f,m.outgoingAlpha(),0f);
    }
    @Test public void jumpFallLandCrossfadeWithoutAffectingPhysics() {
        SpriteMotion m=new SpriteMotion(c);
        m.update(.016f,false,false,-600,0,true,false,false,null,0,false,false);
        m.update(.016f,false,false,100,0,true,false,false,null,0,false,false);
        assertEquals(SpriteStates.FALL,m.clip);
        assertTrue(m.outgoingAlpha()>0);
        m.update(.016f,true,false,0,0,true,false,false,null,0,false,false);
        assertEquals(SpriteStates.LAND,m.clip);
        assertTrue(m.outgoingAlpha()>0);
    }
    @Test public void repeatMoveNeverReusesStaleBlend() {
        SpriteMotion m=new SpriteMotion(c);
        String attack=c.moves.get("M").animation.id;
        tick(m,.016f,attack,.1f);
        tick(m,.016f,attack,.02f);
        assertEquals(.02f,m.time,.001f);
        assertNull(m.previousClip);
    }
    @Test public void resettingCharacterDiscardsOutgoingPose() {
        SpriteMotion m=new SpriteMotion(c);
        tick(m,.016f,SpriteStates.INTRO,.8f);
        tick(m,.016f,null,0f);
        assertNotNull(m.previousClip);
        m.setCharacter(c);
        assertNull(m.previousClip);
        assertEquals(0f,m.outgoingAlpha(),0f);
    }
}
