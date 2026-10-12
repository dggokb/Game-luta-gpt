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
    private Bitmap hud(GameView game) throws Exception {
        Bitmap bitmap=Bitmap.createBitmap(1280,720,Bitmap.Config.ARGB_8888);
        ((HudRenderer)field(game,"hud")).resetEffects();
        // Static meter assertions compare the same presentation time, so fading overlays
        // do not turn a model independence check into a wall-clock race.
        ((HudRenderer)field(game,"hud")).drawHudAt(new Canvas(bitmap),
            (HudRenderer.State)field(game,"hudState"),
            (CharacterSelectRenderer)field(game,"characterSelectRenderer"),10.0);
        return bitmap;
    }
    @Test public void upperMeterTracksAssistCooldownIndependentlyFromSuper() throws Exception {
        GameView game=new GameView(RuntimeEnvironment.getApplication());game.layout(0,0,1280,720);
        FighterState active=((FighterState[])field(game,"team"))[0];
        active.superMeter=0;
        Bitmap ready=hud(game);int charged=ready.getPixel(350,103);
        active.superMeter=5*CombatConfig.METER_PER_BAR;
        Bitmap superCharged=hud(game);
        assertEquals("Super must not change the assist meter",charged,superCharged.getPixel(350,103));
        game.engine.team(0).assistCooldown=game.engine.config.assistCooldownFrames;
        Bitmap cooling=hud(game);
        assertNotEquals("Assist cooldown must empty the upper meter",charged,cooling.getPixel(350,103));
        assertEquals("Assist must not spend the lower Super meter",superCharged.getPixel(290,687),cooling.getPixel(290,687));
        game.engine.team(0).assistCooldown=game.engine.config.assistCooldownFrames/2;
        HudRenderer.State state=(HudRenderer.State)field(game,"hudState");
        assertEquals(.5f,state.tagReadyRatio(),.001f);
        game.engine.team(0).assistCooldown=0;
        Bitmap restored=hud(game);
        assertEquals("Upper meter refills when assist becomes ready",charged,restored.getPixel(350,103));
        ready.recycle();superCharged.recycle();cooling.recycle();restored.recycle();
    }
    @Test public void superHasFiveRealLevelCellsOnBothSides() throws Exception {
        GameView game=new GameView(RuntimeEnvironment.getApplication());game.layout(0,0,1280,720);
        FighterState active=((FighterState[])field(game,"team"))[0];
        FighterState cpu=(FighterState)field(game,"opponentFighter");
        active.superMeter=CombatConfig.METER_PER_BAR;
        cpu.superMeter=CombatConfig.MAX_METER;
        Bitmap one=hud(game);
        for(int i=0;i<5;i++) {
            int x=114+i*45;
            int left=one.getPixel(x,687);
            assertEquals("One real bar fills only the first cell",i==0,Color.blue(left)>Color.red(left)+40);
            int right=one.getPixel(1280-x,687);
            assertTrue("CPU max fills all five mirrored cells",Color.red(right)>Color.blue(right)+40);
        }
        active.superMeter=CombatConfig.MAX_METER;
        Bitmap max=hud(game);
        for(int i=0;i<5;i++)assertTrue(Color.blue(max.getPixel(114+i*45,687))>180);
        one.recycle();max.recycle();
    }

    @Test public void everySelectableHudFaceComesFromDriveConcepts() throws Exception {
        HudConceptPortraits concepts=new HudConceptPortraits(RuntimeEnvironment.getApplication());
        for(String id:GeneratedCharacters.SELECTABLE) {
            String expected="ui/hud/concepts/"+("player_base".equals(id)?"p01":id)+".webp";
            assertEquals(expected,HudConceptPortraits.source(id));
            Bitmap face=concepts.get(id);
            assertNotNull("Concept must exist for "+id,face);
            assertFalse(face.isRecycled());
            assertTrue("Use authored close-up, not combat frame",face.getWidth()>250 && face.getHeight()>250);
        }
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
        assertNotNull("Point portrait must contain real character pixels",portraits.hudPortrait(active.character));
        assertNotNull("Reserve portrait must contain real character pixels",portraits.hudPortrait(((FighterState[])field(game,"team"))[1].character));
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
