package com.gamelutagpt;

import android.graphics.*;
import java.io.*;
import java.lang.reflect.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class HudArtTest {
    private Object field(GameView game, String name) throws Exception {
        Field f=GameView.class.getDeclaredField(name);f.setAccessible(true);return f.get(game);
    }
    private void draw(GameView game, String name, Canvas canvas) throws Exception {
        Method m=GameView.class.getDeclaredMethod(name,Canvas.class);m.setAccessible(true);m.invoke(game,canvas);
    }
    @Test public void ornamentalArtKeepsLifeLiveAndCenterTransparent() throws Exception {
        // BitmapFactory can accept a truncated PNG; validate bytes before native decode.
        java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");
        try(InputStream input=RuntimeEnvironment.getApplication().getAssets().open("ui/hud/energy-frame.png")) {
            byte[] buffer=new byte[8192];int n;
            while((n=input.read(buffer))!=-1)digest.update(buffer,0,n);
        }
        StringBuilder sha=new StringBuilder();
        for(byte value:digest.digest())sha.append(String.format(java.util.Locale.ROOT,"%02x",value&255));
        assertEquals("Complete production atlas", "80980cb5091fdcb0c2117e3e2dd529bcc8de71f06e7f50b04af2c81dbab65ea9",sha.toString());
        GameView game=new GameView(RuntimeEnvironment.getApplication());
        game.layout(0,0,1280,720);
        HudRenderer hud=(HudRenderer)field(game,"hud");
        Field art=HudRenderer.class.getDeclaredField("hudArt");art.setAccessible(true);
        Bitmap atlas=(Bitmap)art.get(hud);
        assertNotNull("The production art must be decoded",atlas);
        assertEquals(0,Color.alpha(atlas.getPixel(800,400)));
        FighterState active=((FighterState[])field(game,"team"))[0];
        CharacterSelectRenderer portraits=(CharacterSelectRenderer)field(game,"characterSelectRenderer");
        assertNotNull("Point portrait must contain real character pixels",portraits.portrait(active.character));
        assertNotNull("Reserve portrait must contain real character pixels",portraits.portrait(((FighterState[])field(game,"team"))[1].character));
        active.restoreLife();
        Bitmap full=Bitmap.createBitmap(1280,720,Bitmap.Config.ARGB_8888);
        draw(game,"drawHud",new Canvas(full));
        int healthy=full.getPixel(350,82);
        assertTrue("Live life must be green",Color.green(healthy)>Color.red(healthy));
        active.life=0;active.recoverableLife=0;
        Bitmap empty=Bitmap.createBitmap(1280,720,Bitmap.Config.ARGB_8888);
        draw(game,"drawHud",new Canvas(empty));
        assertNotEquals("Artwork must not bake in full health",healthy,empty.getPixel(350,82));
        active.restoreLife();
        Bitmap review=Bitmap.createBitmap(1280,720,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(review);
        draw(game,"drawScenario",c);draw(game,"drawPlayer",c);
        draw(game,"drawHud",c);draw(game,"drawControls",c);
        File out=new File("build/hud-review.png");out.getParentFile().mkdirs();
        try(FileOutputStream stream=new FileOutputStream(out)){review.compress(Bitmap.CompressFormat.PNG,100,stream);}
        full.recycle();empty.recycle();review.recycle();
    }
}
