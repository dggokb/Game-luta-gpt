package com.gamelutagpt;
import android.graphics.*;
import android.view.MotionEvent;
import java.io.*;
import java.lang.reflect.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SpriteIntegrationTest {
    private GameView game;
    @Before public void setup(){game=new GameView(RuntimeEnvironment.getApplication());game.layout(0,0,1280,720);}
    private Object get(String key)throws Exception {Field f=GameView.class.getDeclaredField(key);f.setAccessible(true);return f.get(game);}
    private void set(String key,Object v)throws Exception {Field f=GameView.class.getDeclaredField(key);f.setAccessible(true);f.set(game,v);}
    private void frames(int n)throws Exception {Method m=GameView.class.getDeclaredMethod("advanceSimulation",float.class);m.setAccessible(true);for(int i=0;i<n;i++)m.invoke(game,1f/60);}
    private SpriteMotion motion()throws Exception {return ((SpriteFighterRenderer)get("spriteFighterRenderer")).motion;}
    private void touch(int action,float x,float y){MotionEvent e=MotionEvent.obtain(0,10000,action,x,y,0);game.onTouchEvent(e);e.recycle();}
    @Test public void actualPadDrivesForwardBackAndStopsAtRelease()throws Exception {
        touch(MotionEvent.ACTION_DOWN,265,555);frames(10);assertEquals(SpriteMotion.Clip.WALK_FORWARD,motion().clip);
        touch(MotionEvent.ACTION_MOVE,85,555);frames(10);assertEquals(SpriteMotion.Clip.WALK_BACK,motion().clip);
        touch(MotionEvent.ACTION_UP,85,555);frames(10);assertEquals(SpriteMotion.Clip.IDLE,motion().clip);
    }
    @Test public void actualJumpUsesAirFramesThenHeldCrouchAtLanding()throws Exception {
        touch(MotionEvent.ACTION_DOWN,175,465);frames(8);assertEquals(SpriteMotion.Clip.JUMP,motion().clip);
        touch(MotionEvent.ACTION_MOVE,175,645);frames(60);assertEquals(SpriteMotion.Clip.CROUCH,motion().clip);assertEquals(9,motion().frame());
    }
    @Test public void facingChangesSelectForwardInBothDirections()throws Exception {
        set("playerX",1400f);set("dummyX",1000f);set("facingDirection",-1);
        touch(MotionEvent.ACTION_DOWN,85,555);frames(6);assertEquals(SpriteMotion.Clip.WALK_FORWARD,motion().clip);
    }
    @Test public void pauseClearsHeldInputWithoutStartingAnotherLoop()throws Exception {
        touch(MotionEvent.ACTION_DOWN,265,555);frames(5);game.pauseGame();assertFalse((Boolean)get("movingRight"));
        game.resumeGame();assertFalse((Boolean)get("running"));frames(1);assertEquals(SpriteMotion.Clip.IDLE,motion().clip);
    }
    @Test public void packagedAtlasIsVisibleAndEveryCropContainsOneWholePose()throws Exception {
        Bitmap atlas=BitmapFactory.decodeResource(RuntimeEnvironment.getApplication().getResources(),R.drawable.movement_astra);
        assertNotNull(atlas);assertTrue(atlas.hasAlpha());
        for(int[] r:SpriteFighterRenderer.REGIONS) {
            int count=0;for(int y=r[1];y<r[3];y++)for(int x=r[0];x<r[2];x++)if(Color.alpha(atlas.getPixel(x,y))>128)count++;
            assertTrue("Empty sprite",count>15000);assertTrue("Opaque background",count<(r[2]-r[0])*(r[3]-r[1])*.85f);
        }
    }
    @Test public void scenePaintCannotMakeTheFighterTransparent() {
        SpriteFighterRenderer renderer=new SpriteFighterRenderer(RuntimeEnvironment.getApplication());
        Bitmap a=Bitmap.createBitmap(300,260,Bitmap.Config.ARGB_8888),b=Bitmap.createBitmap(300,260,Bitmap.Config.ARGB_8888);
        Paint clean=new Paint(),dirty=new Paint();dirty.setAlpha(30);
        dirty.setShader(new LinearGradient(0,0,100,100,Color.RED,Color.BLUE,Shader.TileMode.CLAMP));
        renderer.draw(new Canvas(a),clean,150,235,false,false);
        renderer.draw(new Canvas(b),dirty,150,235,false,false);
        assertTrue(a.sameAs(b));assertEquals(30,dirty.getAlpha());assertNotNull(dirty.getShader());
    }
    @Test public void renderAllFramesUsingRealCanvasAndPackagedAssets()throws Exception {
        SpriteFighterRenderer renderer=new SpriteFighterRenderer(RuntimeEnvironment.getApplication());
        Bitmap sheet=Bitmap.createBitmap(1200,1040,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(sheet);c.drawColor(Color.rgb(43,52,65));
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setTextSize(15);p.setColor(Color.WHITE);
        SpriteMotion.Clip[] clips={SpriteMotion.Clip.WALK_FORWARD,SpriteMotion.Clip.WALK_BACK,SpriteMotion.Clip.CROUCH,SpriteMotion.Clip.CROUCH,SpriteMotion.Clip.JUMP,SpriteMotion.Clip.FALL,SpriteMotion.Clip.DASH,SpriteMotion.Clip.DASH,SpriteMotion.Clip.BACKDASH,SpriteMotion.Clip.LAND};
        for(int i=0;i<16;i++) {
            SpriteMotion m=renderer.motion;m.time=0;m.distance=0;
            if(i<8){m.clip=i<4?clips[0]:clips[1];m.distance=(i%4)*(i<4?36:32)+1;}
            else {m.clip=clips[i-6];m.time=(i==9?.12f:i==13?.12f:0);}
            assertEquals(i,m.frame());
            c.save();c.translate((i%4)*300,(i/4)*260);
            p.setColor(Color.rgb(90,105,115));c.drawLine(0,235,300,235,p);
            renderer.draw(c,p,150,235,false,false);p.setColor(Color.WHITE);c.drawText(i+" "+m.clip,10,20,p);c.restore();
        }
        File dir=new File("build/sprite-review");dir.mkdirs();
        try(FileOutputStream out=new FileOutputStream(new File(dir,"movement-frames.png"))){assertTrue(sheet.compress(Bitmap.CompressFormat.PNG,100,out));}
        // Whole gameplay screenshot using production stage, HUD, camera and sprite renderer.
        Bitmap scene=Bitmap.createBitmap(1280,720,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(scene);
        Method draw=GameView.class.getDeclaredMethod("drawScenario",Canvas.class);draw.setAccessible(true);draw.invoke(game,canvas);
        Method fighter=GameView.class.getDeclaredMethod("drawPlayer",Canvas.class);fighter.setAccessible(true);fighter.invoke(game,canvas);
        Method hud=GameView.class.getDeclaredMethod("drawHud",Canvas.class);hud.setAccessible(true);hud.invoke(game,canvas);
        Method controls=GameView.class.getDeclaredMethod("drawControls",Canvas.class);controls.setAccessible(true);controls.invoke(game,canvas);
        try(FileOutputStream out=new FileOutputStream(new File(dir,"gameplay.png"))){scene.compress(Bitmap.CompressFormat.PNG,100,out);}
    }
}
