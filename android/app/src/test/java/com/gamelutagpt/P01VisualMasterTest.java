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

    /**
     * Calibração antes do ajuste de tamanho feito no app (8565e3e9): o gerador multiplica cada
     * folha pelo fator {@code tuned}, e estas regras valem para o valor de antes do fator.
     */
    private static float[] base(String atlas,int frame) {
        float[] v=P01SpriteCalibration.get(atlas,frame);
        float f=P01SpriteCalibration.tuned(atlas);
        return new float[]{v[0]/f,v[1]/f};
    }

    @Test public void redrawnDashMatchesIdleAndLegacyMovesKeepTheirSizes() {
        assertEquals("The redrawn dash shares the idle's uniform scale",
                     base("player_base_idle",0)[0],base("player_base_dash",0)[0],.001f);
        assertEquals("The redrawn dash must not stretch on the Y axis",
                     base("player_base_idle",0)[1],base("player_base_dash",0)[1],.001f);
        assertTrue("Jump start must shrink",base("player_base_jump",0)[1]<.90f);
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
        float f=P01SpriteCalibration.tuned("player_base_dash");
        assertEquals(.980f*f,P01SpriteCalibration.get("player_base_dash",0)[0],.001f);
    }

    @Test public void redrawnIntroEndsAtTheSameScaleAndOffsetAsIdle() {
        float[] idle=P01SpriteCalibration.get("player_base_idle",0);
        CharacterDefinition.Animation intro=p01.animation(SpriteStates.INTRO);
        assertEquals(27,intro.atlas.count);
        assertEquals(2.4f,intro.duration,.0001f);
        assertFalse(intro.loop);
        for(int frame=22;frame<=26;frame++) {
            assertArrayEquals("Intro/idle handoff frame "+frame,idle,
                P01SpriteCalibration.get("player_base_intro",frame),.0001f);
        }
    }
}
