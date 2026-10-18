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

    @Test public void excessiveJumpAndCrouchSizesAreReduced() {
        assertTrue("Jump start must shrink",base("player_base_jump",0)[1]<.90f);
        assertTrue("2H raised pose must stay visibly tall",
                   base("player_base_crouch_heavy",4)[1]>.73f);
        assertTrue("2H torso must keep idle-like width",
                   base("player_base_crouch_heavy",4)[0]>.88f);
        assertTrue("Medium crouch must be scaled",
                   base("player_base_crouch_medium",6)[1]<.96f);
    }

    /**
     * Folhas refeitas do vídeo (tools/sprites/refazer.py): tamanho e posição já vêm na folha, então
     * são desenhadas como o IDLE (0,98), sem fator à mão (dash era 1,11; vitória 0,88) e sem
     * recentrar quadro a quadro (o corpo deslizava com os braços).
     */
    @Test public void videoRedoneSheetsAreDrawnLikeTheIdle() {
        for(String atlas:new String[]{"player_base_dash","player_base_victory"}) {
            assertEquals(1f,P01SpriteCalibration.tuned(atlas),0f);
            for(int i=0;i<P01SpriteCalibration.frames(atlas);i++) {
                float[] k=P01SpriteCalibration.get(atlas,i);
                assertEquals(atlas+" "+i,.98f,k[0],1e-4f);
                assertEquals(atlas+" "+i,.98f,k[1],1e-4f);
                assertEquals(atlas+" "+i,0f,k[2],0f);
                assertEquals(atlas+" "+i,0f,k[3],0f);
            }
        }
    }

    @Test public void appSizeAdjustmentIsRecordedOnTheCalibration() {
        assertEquals(.99f,P01SpriteCalibration.tuned("player_base_intro"),1e-4f);
    }
}
