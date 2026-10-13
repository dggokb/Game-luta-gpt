package com.gamelutagpt;

import android.graphics.*;
import android.os.SystemClock;
import java.io.*;
import java.lang.reflect.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

/**
 * Renders real production frames (selection, fight HUD and impact effects) through the
 * native Canvas so visual changes can be reviewed as images. Output: build/visual-review.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class VisualReviewTest {
    private GameView game;
    private Object get(String key) throws Exception {
        Field f=GameView.class.getDeclaredField(key);f.setAccessible(true);return f.get(game);
    }
    private void set(String key,Object v) throws Exception {
        Field f=GameView.class.getDeclaredField(key);f.setAccessible(true);f.set(game,v);
    }
    private void frames(int n) throws Exception {
        Method m=GameView.class.getDeclaredMethod("advanceSimulation",float.class);m.setAccessible(true);
        for(int i=0;i<n;i++) {m.invoke(game,1f/60);SystemClock.sleep(16);}
    }
    private void render(Bitmap bitmap) throws Exception {
        Method m=GameView.class.getDeclaredMethod("renderFrame",Canvas.class);m.setAccessible(true);
        Canvas c=new Canvas(bitmap);c.drawColor(Color.BLACK);m.invoke(game,c);
    }
    private static void save(Bitmap b,File file) throws IOException {
        file.getParentFile().mkdirs();
        try(FileOutputStream out=new FileOutputStream(file)) {assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,out));}
    }
    private CharacterSelect select() throws Exception {return (CharacterSelect)get("characterSelect");}
    private void tapCard(int i) throws Exception {
        float[] q=new float[8];select().cardCorners(i,q);
        select().tap((q[0]+q[2]+q[4]+q[6])/4,(q[1]+q[5])/2);
    }

    @Test public void renderSelectionStates() throws Exception {
        game=new GameView(RuntimeEnvironment.getApplication());game.layout(0,0,1280,720);
        select().open("player_base","p08","p03","p06");
        Bitmap b=Bitmap.createBitmap(1280,720,Bitmap.Config.ARGB_8888);
        File dir=new File("build/visual-review/selection");
        SystemClock.sleep(400);render(b);save(b,new File(dir,"step0.png"));
        tapCard(4);SystemClock.sleep(120);render(b);save(b,new File(dir,"step1_flash.png"));
        SystemClock.sleep(700);render(b);save(b,new File(dir,"step1.png"));
        tapCard(1);tapCard(2);SystemClock.sleep(900);render(b);save(b,new File(dir,"step3.png"));
        tapCard(6);SystemClock.sleep(900);render(b);save(b,new File(dir,"ready.png"));
        // Short animation of the cursor idling, for motion review.
        select().open("player_base","p08","p03","p06");tapCard(1);
        for(int i=0;i<45;i++) {SystemClock.sleep(33);render(b);save(b,new File(dir,String.format("anim/%04d.png",i)));}
        b.recycle();
    }

    @Test public void renderFightImpactSequence() throws Exception {
        game=new GameView(RuntimeEnvironment.getApplication());game.layout(0,0,1280,720);
        select().close();
        set("introFrame",-1);
        CombatFighter p=game.engine.fighter(0),npc=game.engine.fighter(1);
        npc.x=p.x+150f;
        frames(2);
        Bitmap b=Bitmap.createBitmap(1280,720,Bitmap.Config.ARGB_8888);
        File dir=new File("build/visual-review/fight");
        render(b);save(b,new File(dir,"idle.png"));
        FighterState active=((FighterState[])get("team"))[0];
        active.superMeter=CombatConfig.MAX_METER;
        int index=0;
        for(int f=0;f<230;f++) {
            npc.x=Math.max(npc.x,p.x+110f);
            if(f==0) {game.pad.setDirection(0);game.pad.press(PadInput.Button.LIGHT);}
            if(f==16) game.pad.press(PadInput.Button.MEDIUM);
            if(f==34) game.pad.press(PadInput.Button.HEAVY);
            if(f==120) {npc.x=p.x+150f;game.pad.press(PadInput.Button.SUPER);}
            frames(1);
            if(f%2==0||(f>=34&&f<60)) {render(b);save(b,new File(dir,String.format("seq/%04d.png",index++)));}
        }
        b.recycle();
    }
}
