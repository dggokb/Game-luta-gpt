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
    /** A vitória refeita do vídeo (refazer.py) não leva correção quadro a quadro: posição do vídeo. */
    @Test public void redoneVictoryHasNoPublishedOffsets() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(InputStream in=context.getAssets().open("sprite-autofix.json")) {
            byte[] buf=new byte[4096];
            for(int n;(n=in.read(buf))!=-1;)bytes.write(buf,0,n);
        }
        try {
            SpriteAutoFix.replaceFromJson(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
            CharacterDefinition.Animation victory=GeneratedCharacters.get("player_base")
                .animation(SpriteStates.VICTORY);
            assertEquals(97,victory.atlas.count);   // todos os quadros de vitoria.mp4 (0-96, menos 1 com efeito)
            assertEquals(4.125f,victory.duration,.0001f);   // o tempo de antes
            for(int frame=0;frame<victory.atlas.count;frame++) {
                SpriteAutoFix.Adjustment fix=SpriteAutoFix.get(
                    "player_base","player_base_victory",frame);
                assertEquals(1f,fix.size,0.00001f);
                assertEquals(0f,fix.dx,0.00001f);
                assertEquals(0f,fix.dy,0.00001f);
                assertEquals(.98f,P01SpriteCalibration.get("player_base_victory",frame)[0],.0001f);
            }
        } finally {
            SpriteAutoFix.replaceFromJson("{\"schema\":1,\"corrections\":[]}");
        }
    }
}
