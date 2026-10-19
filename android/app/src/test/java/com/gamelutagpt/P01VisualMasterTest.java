package com.gamelutagpt;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.HashSet;
import java.util.Set;

/** Every P01 atlas frame is calibrated against one immutable IDLE silhouette, except the sheets
 * converted at the official size (desenhoNativo), which are drawn 1:1. */
public class P01VisualMasterTest {
    private final CharacterDefinition p01=GeneratedCharacters.get("player_base");

    @Test public void allStatesAndAttacksHavePerFrameCalibration() {
        Set<String> checked=new HashSet<>();
        for(CharacterDefinition.Animation art:p01.animations.values())check(checked,art);
        for(CharacterDefinition.Animation art:p01.specialAnimations.values())check(checked,art);
        for(CharacterDefinition.Move move:p01.moves.values()) {
            if(move.animation!=null)check(checked,move.animation);
        }
        assertTrue("Expected at least 37 frame-calibrated or native P01 atlases, got "+checked.size(),
                   checked.size()>=37);
    }

    private void check(Set<String> checked,CharacterDefinition.Animation art) {
        String atlas=art.atlas.resource;
        if("player_base_crouch".equals(atlas)||"player_base_rise".equals(atlas))return;
        if(P01SpriteCalibration.nativo(atlas)) {
            assertEquals("Native sheets have no legacy calibration: "+atlas,0,P01SpriteCalibration.frames(atlas));
            assertArrayEquals(atlas,new float[]{1f,1f,0f,0f},P01SpriteCalibration.get(atlas,0),0f);
            checked.add(atlas);
            return;
        }
        assertEquals("Uncalibrated frames: "+atlas,art.atlas.count,
                     P01SpriteCalibration.frames(atlas));
        checked.add(atlas);
    }

    /**
     * Calibração antes do ajuste de tamanho feito no app (8565e3e9): o gerador multiplica cada
     * folha pelo fator {@code tuned}, e estas regras valem para o valor de antes do fator.
     */
    private static float[] base(String atlas,int frame) {
        float[] v=P01SpriteCalibration.get(atlas,frame);
        float f=P01SpriteCalibration.tuned(atlas);
        return new float[]{v[0]/f,v[1]/f};
    }

    @Test public void excessiveDashJumpAndVictorySizesAreReduced() {
        // The run comes from the video at the official size: drawn 1:1, like the idle.
        assertTrue("Dash is a native sheet",P01SpriteCalibration.nativo("player_base_dash"));
        assertTrue("Idle is a native sheet",P01SpriteCalibration.nativo("player_base_idle"));
        // Old art follows the idle reference: the 1:1 idle is 446 px tall (the .98 one was 441).
        assertTrue("Jump start must shrink",base("player_base_jump",0)[1]<.91f);
        assertEquals("Victory compromise between previous oversized and undersized",
                     .880f,base("player_base_victory",15)[1],.001f);
        assertTrue("2H raised pose must stay visibly tall",
                   base("player_base_crouch_heavy",4)[1]>.73f);
        assertTrue("2H torso must keep idle-like width",
                   base("player_base_crouch_heavy",4)[0]>.88f);
        assertTrue("Medium crouch must be scaled",
                   base("player_base_crouch_medium",6)[1]<.96f);
    }

    @Test public void appSizeAdjustmentIsAppliedOnTopOfTheCalibration() {
        // fall_air keeps a fixed 1.02 calibration; the app size adjustment multiplies it.
        float f=P01SpriteCalibration.tuned("player_base_fall_air");
        assertNotEquals(1f,f,0f);
        assertEquals(1.02f*f,P01SpriteCalibration.get("player_base_fall_air",0)[0],.001f);
    }
}
