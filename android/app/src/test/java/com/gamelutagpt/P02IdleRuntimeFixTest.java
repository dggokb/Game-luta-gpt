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

/** Verifies the same P02 IDLE runtime asset/atlas that the APK loads. */
@RunWith(RobolectricTestRunner.class)
public class P02IdleRuntimeFixTest {
    @Test public void packagedIdlesHaveOnlyTheMeasuredFiveFrameFootCorrection() throws Exception {
        Context app=RuntimeEnvironment.getApplication();
        String json;
        try(InputStream input=app.getAssets().open("sprite-autofix.json")) {
            ByteArrayOutputStream buffer=new ByteArrayOutputStream();
            byte[] bytes=new byte[4096];
            int n;
            while((n=input.read(bytes))!=-1) buffer.write(bytes,0,n);
            json=new String(buffer.toByteArray(),StandardCharsets.UTF_8);
        }
        JSONObject config=new JSONObject(json);
        JSONArray corrections=config.getJSONArray("corrections");
        int p02Entries=0;
        for(int i=0;i<corrections.length();i++) {
            JSONObject entry=corrections.getJSONObject(i);
            if(!"p02".equals(entry.getString("character")))continue;
            assertEquals("p02_idle",entry.getString("atlas"));
            assertEquals("IDLE",entry.getString("state"));
            JSONArray rows=entry.getJSONArray("frames");
            assertEquals(5,rows.length());
            for(int j=0;j<5;j++) {
                JSONArray row=rows.getJSONArray(j);
                assertEquals(51+j,row.getInt(0));
                assertEquals(1f,(float)row.getDouble(1),0f);
                assertEquals(2f,(float)row.getDouble(2),0f);
                assertEquals(0f,(float)row.getDouble(3),0f);
            }
            p02Entries++;
        }
        assertEquals(1,p02Entries);
        CharacterDefinition.Atlas atlas=GeneratedCharacters.get("p02").animation(SpriteStates.IDLE).atlas;
        assertEquals("p02_idle",atlas.resource);
        assertEquals(56,atlas.count);
        assertEquals(332,atlas.width);
        assertEquals(445,atlas.height);
        try {
            SpriteAutoFix.replaceFromJson(json);
            for(int frame=0;frame<56;frame++) {
                SpriteAutoFix.Adjustment fix=SpriteAutoFix.get("p02","p02_idle",frame);
                assertEquals(1f,fix.size,0f);
                assertEquals(frame>=51?2f:0f,fix.dx,0f);
                assertEquals(0f,fix.dy,0f);
                assertEquals(0f,SpriteAutoFix.get("player_base","p02_idle",frame).dx,0f);
                assertEquals(0f,SpriteAutoFix.get("p02","p02_intro",frame).dx,0f);
            }
        } finally {
            SpriteAutoFix.replaceFromJson("{\"schema\":1,\"corrections\":[]}");
        }
    }
}
