package com.gamelutagpt;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import static org.junit.Assert.*;

/**
 * Uses the REAL asset included in the P01 APK, not synthetic correction JSON.
 * Complements real-pixel frame validation; not a substitute for a device recording.
 */
@RunWith(RobolectricTestRunner.class)
public class P01VictoryRealAssetTest {
    @Test public void realPublishedOffsetsReachAndroidAutofixAndSpareEntryAndOtherStates()
            throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(InputStream in=context.getAssets().open("sprite-autofix.json")) {
            byte[] buf=new byte[4096];
            for(int n;(n=in.read(buf))!=-1;)bytes.write(buf,0,n);
        }
        try {
            SpriteAutoFix.replaceFromJson(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
            int changed=0;
            for(int frame=0;frame<33;frame++) {
                SpriteAutoFix.Adjustment fix=SpriteAutoFix.get(
                    "player_base","player_base_victory",frame);
                assertEquals("VICTORY must preserve anatomical scale",1f,fix.size,0.00001f);
                assertEquals("VICTORY must not shift foot Y",0f,fix.dy,0.00001f);
                if(frame<7) assertEquals("Intentional entry untouched",0f,fix.dx,0.00001f);
                if(Math.abs(fix.dx)>.0001f)changed++;
                assertEquals(0f,SpriteAutoFix.get("player_base","player_base_idle",frame).dx,.00001f);
                assertEquals(0f,SpriteAutoFix.get("p02","player_base_victory",frame).dx,.00001f);
            }
            assertEquals("Expected only source-grounded frame adjustments",22,changed);
            assertEquals("Frame 32 must counter post-calibration drift",
                         -6.0577f,SpriteAutoFix.get("player_base","player_base_victory",32).dx,.0001f);
            CharacterDefinition.Animation victory=GeneratedCharacters.get("player_base")
                .animation(SpriteStates.VICTORY);
            assertEquals(33,victory.atlas.count);
            assertEquals(7,victory.frame(victory.timeOfFrame(7),0f));
            assertEquals(32,victory.frame(victory.timeOfFrame(32),0f));
            assertEquals(.125f,victory.timeOfFrame(8)-victory.timeOfFrame(7),.0001f);
            assertEquals(.88f,P01SpriteCalibration.get("player_base_victory",7)[0],.0001f);
        } finally {
            SpriteAutoFix.replaceFromJson("{\"schema\":1,\"corrections\":[]}");
        }
    }
}
