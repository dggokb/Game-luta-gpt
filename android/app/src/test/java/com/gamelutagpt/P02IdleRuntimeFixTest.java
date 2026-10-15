package com.gamelutagpt;

import static org.junit.Assert.*;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Android P02 IDLE uses only untouched atlas pixels and the approved scale. */
@RunWith(RobolectricTestRunner.class)
public class P02IdleRuntimeFixTest {
    @Test public void continuousBreathingPlaybackKeepsApprovedAtlasSize() throws Exception {
        Context app=RuntimeEnvironment.getApplication();
        ByteArrayOutputStream buffer=new ByteArrayOutputStream();
        try(InputStream input=app.getAssets().open("sprite-autofix.json")) {
            byte[] bytes=new byte[4096]; int n;
            while((n=input.read(bytes))!=-1) buffer.write(bytes,0,n);
        }
        String json=new String(buffer.toByteArray(),StandardCharsets.UTF_8);
        JSONObject config=new JSONObject(json);
        JSONArray patches=config.getJSONArray("corrections");
        for(int i=0;i<patches.length();i++) {
            JSONObject patch=patches.getJSONObject(i);
            assertFalse("Obsolete P02 IDLE shifts must not affect playback",
                "p02".equals(patch.getString("character")) && "p02_idle".equals(patch.getString("atlas")));
        }
        CharacterDefinition.Animation idle=GeneratedCharacters.get("p02").animation(SpriteStates.IDLE);
        CharacterDefinition.Atlas atlas=idle.atlas;
        assertEquals("p02_idle",atlas.resource);
        assertEquals(56,atlas.count); // Original 56 physical HD cells are untouched.
        // Célula reescalada a 97% pelo ajuste de tamanho feito no app (8565e3e9); era 332x445.
        assertEquals(322,atlas.width);
        assertEquals(432,atlas.height);
        assertEquals(2.0f,atlas.pixelScale,0f);
        assertTrue(idle.loop);
        assertEquals(30*0.041f,idle.duration,0.0001f);
        int[] timeline=new int[30];
        for(int i=0;i<timeline.length;i++) timeline[i]=15+i;
        for(int i=0;i<timeline.length;i++) {
            assertEquals("Unexpected original atlas cell at playback step "+i,
                timeline[i],idle.frame((i+0.5f)*0.041f,0f));
        }
        assertEquals(15,idle.frame(idle.duration+0.0205f,0f));
        try {
            SpriteAutoFix.replaceFromJson(json);
            for(int i=0;i<56;i++) {
                SpriteAutoFix.Adjustment fix=SpriteAutoFix.get("p02","p02_idle",i);
                assertEquals(1.0f,fix.size,0f);
                assertEquals(0f,fix.dx,0f);
                assertEquals(0f,fix.dy,0f);
            }
        } finally {
            SpriteAutoFix.replaceFromJson("{\"schema\":1,\"corrections\":[]}");
        }
    }
}
