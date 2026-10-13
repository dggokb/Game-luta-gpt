package com.gamelutagpt;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SpriteAutoFixTest {
    @Test public void correctionsAreAppliedOnlyToTheExplicitFighterAtlasAndFrame() throws Exception {
        SpriteAutoFix.replaceFromJson("{\"schema\":1,\"corrections\":["
            +"{\"character\":\"player_base\",\"atlas\":\"player_base_victory\","
            +"\"frames\":[[7,1.10,-3.25,0],[8,1.10,2.50,0]]}]}");
        assertEquals(1.10f,SpriteAutoFix.get("player_base","player_base_victory",7).size,.001f);
        assertEquals(-3.25f,SpriteAutoFix.get("player_base","player_base_victory",7).dx,.001f);
        assertEquals(2.5f,SpriteAutoFix.get("player_base","player_base_victory",8).dx,.001f);
        assertEquals(1f,SpriteAutoFix.get("player_base","player_base_victory",6).size,.001f);
        assertEquals(0f,SpriteAutoFix.get("player_base","player_base_idle",7).dx,.001f);
        assertEquals(0f,SpriteAutoFix.get("p02","player_base_victory",7).dx,.001f);
        assertEquals(2,SpriteAutoFix.count());
        SpriteAutoFix.replaceFromJson("{\"schema\":1,\"corrections\":[]}");
        assertEquals(0,SpriteAutoFix.count());
    }

    @Test public void malformedPatchesCannotPartiallyChangePreviouslyAcceptedCorrections() throws Exception {
        SpriteAutoFix.replaceFromJson("{\"schema\":1,\"corrections\":["
            +"{\"character\":\"p02\",\"atlas\":\"p02_idle\","
            +"\"frames\":[[0,1.05,2,0]]}]}");
        String[] unsafe={
            "{\"schema\":1,\"corrections\":[{\"character\":\"p02\","
                 +"\"atlas\":\"p02_idle\",\"frames\":[[0,2.0,2,0]]}]}",
            "{\"schema\":1,\"corrections\":[{\"character\":\"p02\","
                 +"\"atlas\":\"p02_idle\",\"frames\":[[0,1,2,0],[0,1,3,0]]}]}",
            "{\"schema\":3,\"corrections\":[]}"
        };
        for(String broken:unsafe) {
            try {
                SpriteAutoFix.replaceFromJson(broken);
                fail("Unsafe correction accepted");
            } catch(Exception expected) {
                assertEquals(1.05f,SpriteAutoFix.get("p02","p02_idle",0).size,.001f);
            }
        }
        SpriteAutoFix.replaceFromJson("{\"schema\":1,\"corrections\":[]}");
    }
}
