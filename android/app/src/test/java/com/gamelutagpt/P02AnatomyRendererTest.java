package com.gamelutagpt;

import static org.junit.Assert.*;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import java.lang.reflect.Method;

/** Real Android Canvas/Bitmap proof that SpriteAutoFix scales only the desired atlas. */
@RunWith(RobolectricTestRunner.class)
public class P02AnatomyRendererTest {
    private Bitmap render(SpriteFighterRenderer renderer, CharacterDefinition.Atlas atlas,
                          int physicalFrame) throws Exception {
        Bitmap image=Bitmap.createBitmap(950,700,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(image);
        Method draw=SpriteFighterRenderer.class.getDeclaredMethod(
            "drawAtlas",Canvas.class,CharacterDefinition.Atlas.class,int.class,
            float.class,float.class,float.class,float.class,int.class);
        draw.setAccessible(true);
        draw.invoke(renderer,canvas,atlas,physicalFrame,470f,595f,1f,1f,255);
        return image;
    }
    private int[] bounds(Bitmap image) {
        int x0=image.getWidth(),y0=image.getHeight(),x1=-1,y1=-1;
        for(int y=0;y<image.getHeight();y++)
            for(int x=0;x<image.getWidth();x++)
                if((image.getPixel(x,y)>>>24)>20) {
                    x0=Math.min(x0,x);y0=Math.min(y0,y);
                    x1=Math.max(x1,x);y1=Math.max(y1,y);
                }
        assertTrue("Sprite must draw visible pixels",x1>=x0);
        return new int[]{x0,y0,x1,y1};
    }
    @Test public void uniformDashPatchActuallyChangesAndroidPixelsAndNotIdle() throws Exception {
        Context ctx=RuntimeEnvironment.getApplication();
        SpriteFighterRenderer renderer=new SpriteFighterRenderer(ctx,GeneratedCharacters.get("p02"));
        CharacterDefinition hero=GeneratedCharacters.get("p02");
        CharacterDefinition.Atlas idle=hero.animation(SpriteStates.IDLE).atlas;
        CharacterDefinition.Atlas dash=hero.animation(SpriteStates.DASH).atlas;
        try {
            SpriteAutoFix.replaceFromJson("{\"schema\":1,\"corrections\":[]}");
            int[] original=bounds(render(renderer,dash,0));
            int[] idleBefore=bounds(render(renderer,idle,15));
            SpriteAutoFix.replaceFromJson("{\"schema\":1,\"corrections\":["
                +"{\"character\":\"p02\",\"atlas\":\"p02_dash\","
                +"\"state\":\"DASH\",\"frames\":[[0,1.10,0,0]]}]}");
            int[] corrected=bounds(render(renderer,dash,0));
            int[] idleAfter=bounds(render(renderer,idle,15));
            assertArrayEquals("Approved P02 IDLE must never be altered",idleBefore,idleAfter);
            float sx=(corrected[2]-corrected[0]+1f)/(original[2]-original[0]+1f);
            float sy=(corrected[3]-corrected[1]+1f)/(original[3]-original[1]+1f);
            assertEquals("Actual Android rendered width",1.10f,sx,.035f);
            assertEquals("Actual Android rendered height",1.10f,sy,.035f);
            assertEquals("Feet remain anchored at same floor",original[3],corrected[3],3);
            assertEquals("Renderer uses a uniform factor",sx,sy,.045f);
        } finally {
            SpriteAutoFix.replaceFromJson("{\"schema\":1,\"corrections\":[]}");
        }
    }
}
