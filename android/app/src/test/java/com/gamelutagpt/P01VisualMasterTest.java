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

    @Test public void videoSheetsAreDrawnUncalibratedAndTheOldOnesKeepTheirCalibration() {
        // Folhas dos vídeos novos (11/2026): mesma escala em todos os quadros, sem calibração.
        for(String atlas:new String[]{"player_base_idle","player_base_dash","player_base_jump",
                "player_base_crouch","player_base_rise","player_base_crouch_heavy","player_base_jab"}) {
            assertTrue(atlas,P01SpriteCalibration.video(atlas));
            float[] v=base(atlas,0);
            assertEquals(atlas,1f,v[0],.0001f);
            assertEquals(atlas,1f,v[1],.0001f);
        }
        // As que ainda são da arte antiga continuam normalizadas pelo idle.
        assertFalse(P01SpriteCalibration.video("player_base_victory"));
        assertEquals("Victory compromise between previous oversized and undersized",
                     .880f,base("player_base_victory",15)[1],.001f);
    }

    @Test public void appSizeAdjustmentIsAppliedOnTopOfTheCalibration() {
        float f=P01SpriteCalibration.tuned("player_base_victory");
        assertEquals(.880f*f,P01SpriteCalibration.get("player_base_victory",15)[1],.001f);
    }
}
