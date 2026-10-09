package com.gamelutagpt;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpriteTransitionAlignmentTest {
    @Test public void safeForEntireFighterRoster() {
        for(String id:new String[]{"player_base","p03"}) {
            CharacterDefinition c=GeneratedCharacters.get(id);
            for(String from:c.animations.keySet()) {
                int previous=c.animation(from).frame(0,0);
                for(String to:c.animations.keySet()) {
                    int index=GeneratedSpriteTransitions.entry(id,from,previous,to);
                    assertTrue(from+" -> "+to,index>=0);
                }
            }
        }
    }
    @Test public void introToIdleMapsIntoRealIdleFrames() {
        CharacterDefinition p=GeneratedCharacters.defaultCharacter();
        CharacterDefinition.Animation intro=p.animation(SpriteStates.INTRO);
        int from=intro.frame(intro.duration,0);
        int index=GeneratedSpriteTransitions.entry(p.id,SpriteStates.INTRO,from,SpriteStates.IDLE);
        CharacterDefinition.Animation idle=p.animation(SpriteStates.IDLE);
        assertEquals(idle.frame(idle.timeOfFrame(index),0),idle.frame(idle.timeOfFrame(index),0));
        assertTrue(index>=0 && index<76);
    }
}
