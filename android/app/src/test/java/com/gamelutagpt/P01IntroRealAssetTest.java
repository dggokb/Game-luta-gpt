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
 * Test the actual APK asset and P01 INTRO animation mapping, never a synthetic
 * correction table. Pixel comparison is independently run in Python.
 */
@RunWith(RobolectricTestRunner.class)
public class P01IntroRealAssetTest {
    @Test public void redrawnIntroNeedsNoLegacyCorrectionsAndPreservesApprovedVictory()
            throws Exception {
        Context ctx=RuntimeEnvironment.getApplication();
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(InputStream stream=ctx.getAssets().open("sprite-autofix.json")) {
            byte[] buf=new byte[8192];
            for(int n;(n=stream.read(buf))!=-1;) bytes.write(buf,0,n);
        }
        try {
            SpriteAutoFix.replaceFromJson(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
            assertEquals("Only the approved Victory corrections remain",33,SpriteAutoFix.count());
            int active=0;
            for(int frame=0;frame<27;frame++) {
                SpriteAutoFix.Adjustment a=SpriteAutoFix.get(
                    "player_base","player_base_intro",frame);
                assertEquals("Never resize artwork",1f,a.size,.00001f);
                assertEquals("Never vertically shift artwork",0f,a.dy,.00001f);
                assertTrue("Safe atlas pixel correction",Math.abs(a.dx)<=45f);
                if(Math.abs(a.dx)>.00001f)active++;
                assertEquals("Idle MUST stay untouched",0f,
                    SpriteAutoFix.get("player_base","player_base_idle",frame).dx,.00001f);
                assertEquals("Other characters untouched",0f,
                    SpriteAutoFix.get("p02","player_base_intro",frame).dx,.00001f);
            }
            assertEquals("The replacement intro uses its authored registration",0,active);
            assertEquals(0f,SpriteAutoFix.get("player_base","player_base_intro",0).dx,.00001f);
            assertEquals(0f,SpriteAutoFix.get("player_base","player_base_intro",16).dx,.0002f);
            assertEquals(0f,SpriteAutoFix.get("player_base","player_base_intro",22).dx,.0002f);
            assertEquals(0f,SpriteAutoFix.get("player_base","player_base_intro",26).dx,.0002f);
            CharacterDefinition.Animation intro=GeneratedCharacters.get("player_base")
                .animation(SpriteStates.INTRO);
            assertEquals("same original frame count",27,intro.atlas.count);
            assertEquals(0,intro.frame(0f,0f));
            assertEquals(26,intro.frame(intro.timeOfFrame(26),0f));
            assertEquals(.09f,intro.timeOfFrame(1)-intro.timeOfFrame(0),.0001f);
            assertEquals(.06f,intro.timeOfFrame(17)-intro.timeOfFrame(16),.0001f);
            assertEquals(.135f,intro.timeOfFrame(24)-intro.timeOfFrame(23),.0001f);
            assertEquals(2.4f,intro.duration,.0001f);
            int changed=0;
            for(int frame=0;frame<33;frame++) {
                SpriteAutoFix.Adjustment v=SpriteAutoFix.get(
                    "player_base","player_base_victory",frame);
                assertEquals(1f,v.size,.00001f);
                assertEquals(0f,v.dy,.00001f);
                if(frame<7) assertEquals(0f,v.dx,.00001f);
                if(Math.abs(v.dx)>.0001f)changed++;
            }
            assertEquals("VICTORY approved offsets must not change",22,changed);
            assertEquals(-6.0577f,
                SpriteAutoFix.get("player_base","player_base_victory",32).dx,.0001f);
        } finally {
            SpriteAutoFix.replaceFromJson("{\"schema\":1,\"corrections\":[]}");
        }
    }
}
