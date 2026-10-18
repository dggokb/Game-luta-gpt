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
    /**
     * O intro do p01 tem um tamanho só e um deslocamento só (desenhoP01 na receita): as
     * correções quadro a quadro do sprite-autofix e a normalização pela altura (0,81 a 1,05,
     * o zoom no meio da animação) saíram. Tempo e quadros continuam os mesmos.
     */
    @Test public void introHasOneSizeAndNoPerFrameCorrection() throws Exception {
        Context ctx=RuntimeEnvironment.getApplication();
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(InputStream stream=ctx.getAssets().open("sprite-autofix.json")) {
            byte[] buf=new byte[8192];
            for(int n;(n=stream.read(buf))!=-1;) bytes.write(buf,0,n);
        }
        try {
            SpriteAutoFix.replaceFromJson(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
            CharacterDefinition.Animation intro=GeneratedCharacters.get("player_base")
                .animation(SpriteStates.INTRO);
            assertEquals("same original frame count",27,intro.atlas.count);
            float[] first=P01SpriteCalibration.get("player_base_intro",0);
            for(int frame=0;frame<27;frame++) {
                SpriteAutoFix.Adjustment a=SpriteAutoFix.get("player_base","player_base_intro",frame);
                assertEquals(1f,a.size,.00001f);
                assertEquals(0f,a.dx,.00001f);
                assertEquals(0f,a.dy,.00001f);
                assertArrayEquals("um tamanho só, quadro "+frame,first,
                    P01SpriteCalibration.get("player_base_intro",frame),0f);
            }
            assertEquals(.9307f,first[0],.0001f);
            assertEquals(0,intro.frame(0f,0f));
            assertEquals(26,intro.frame(intro.timeOfFrame(26),0f));
            assertEquals(.09f,intro.timeOfFrame(1)-intro.timeOfFrame(0),.0001f);
            assertEquals(.06f,intro.timeOfFrame(17)-intro.timeOfFrame(16),.0001f);
            assertEquals(.135f,intro.timeOfFrame(24)-intro.timeOfFrame(23),.0001f);
            assertEquals(2.4f,intro.duration,.0001f);
        } finally {
            SpriteAutoFix.replaceFromJson("{\"schema\":1,\"corrections\":[]}");
        }
    }
}
