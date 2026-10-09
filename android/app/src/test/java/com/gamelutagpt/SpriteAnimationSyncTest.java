package com.gamelutagpt;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpriteAnimationSyncTest {
    @Test public void jumpReachesEndOfAscentAtPhysicalApex() {
        CharacterDefinition.Animation a=GeneratedCharacters.defaultCharacter().animation(SpriteStates.JUMP);
        SpriteMotion m=new SpriteMotion();
        m.update(.016f,false,false,-1450f,0,true,false,false,null,0,false,false);
        float initial=m.time;
        m.update(.016f,false,false,-150f,0,true,false,false,null,0,false,false);
        assertTrue(m.time>initial);
        assertTrue(m.time>a.duration*.85f);
        m.update(.016f,false,false,200f,0,true,false,false,null,0,false,false);
        assertEquals(SpriteStates.FALL,m.clip);
    }
    @Test public void noActualTravelMeansNoExtraStep() {
        SpriteMotion m=new SpriteMotion();
        m.update(.016f,true,false,0,14,true,false,false,null,0,false,false);
        float prior=m.distance;
        m.update(.016f,true,false,0,0,true,false,false,null,0,false,false);
        assertEquals(prior,m.distance,.00001f);
    }
    @Test public void attackCannotBeRetimedByJumpPhysics() {
        SpriteMotion m=new SpriteMotion();
        m.update(.016f,false,false,-900,0,true,false,false,null,0,false,false);
        m.update(.016f,false,false,-200,0,true,false,false,"JUMP_LIGHT",.08f,true,false);
        assertEquals(.08f,m.time,.00001f);
    }
    @Test public void secondJumpDoesNotInheritOldPhase() {
        SpriteMotion m=new SpriteMotion();
        m.update(.016f,false,false,-660,0,true,false,false,null,0,false,false);
        m.update(.016f,false,false,-50,0,true,false,false,null,0,false,false);
        float old=m.time;
        m.update(.016f,true,false,0,0,true,false,false,null,0,false,false);
        m.update(.016f,false,false,-1450,0,true,false,false,null,0,false,false);
        assertTrue(m.time<old);
    }
}
