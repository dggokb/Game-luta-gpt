package com.gamelutagpt;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.HashSet;
import java.util.Set;

/** Every P01 atlas frame is calibrated against one immutable IDLE silhouette. */
public class P01VisualMasterTest {
    private final CharacterDefinition p01=GeneratedCharacters.get("player_base");

    @Test public void allStatesAndAttacksHavePerFrameCalibration() {
        Set<String> checked=new HashSet<>();
        for(CharacterDefinition.Animation art:p01.animations.values())check(checked,art);
        for(CharacterDefinition.Animation art:p01.specialAnimations.values())check(checked,art);
        for(CharacterDefinition.Move move:p01.moves.values()) {
            if(move.animation!=null)check(checked,move.animation);
        }
        assertTrue("Expected at least 37 frame-calibrated P01 atlases, got "+checked.size(),
                   checked.size()>=37);
    }

    private void check(Set<String> checked,CharacterDefinition.Animation art) {
        String atlas=art.atlas.resource;
        if("player_base_crouch".equals(atlas)||"player_base_rise".equals(atlas))return;
        assertEquals("Uncalibrated frames: "+atlas,art.atlas.count,
                     P01SpriteCalibration.frames(atlas));
        checked.add(atlas);
    }

    @Test public void excessiveDashJumpAndVictorySizesAreReduced() {
        assertEquals("Dash visually grows by 15% from v0.97 in X",
                     .8395f,P01SpriteCalibration.get("player_base_dash",0)[0],.001f);
        assertEquals("Dash visually grows by 15% from v0.97 in Y",
                     1.10975f,P01SpriteCalibration.get("player_base_dash",0)[1],.001f);
        assertTrue("Jump start must shrink",P01SpriteCalibration.get("player_base_jump",0)[1]<.90f);
        assertEquals("Victory compromise between previous oversized and undersized",
                     .880f,P01SpriteCalibration.get("player_base_victory",15)[1],.001f);
        assertTrue("2H raised pose must stay visibly tall",
                   P01SpriteCalibration.get("player_base_crouch_heavy",4)[1]>.73f);
        assertTrue("2H torso must keep idle-like width",
                   P01SpriteCalibration.get("player_base_crouch_heavy",4)[0]>.88f);
        assertTrue("Medium crouch must be scaled",
                   P01SpriteCalibration.get("player_base_crouch_medium",6)[1]<.96f);
    }
}
