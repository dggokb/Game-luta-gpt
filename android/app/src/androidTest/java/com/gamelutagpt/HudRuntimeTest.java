package com.gamelutagpt;

import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.MotionEvent;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.FileOutputStream;
import java.io.File;
import java.lang.reflect.Field;
import static org.junit.Assert.*;

/** Runs the installed app and its normal loop. Only touch input; no combat-state writes. */
@RunWith(AndroidJUnit4.class)
public class HudRuntimeTest {
    private Instrumentation ins;
    private GameView game;
    private long down;
    private interface Condition { boolean met() throws Exception; }
    private void await(String message, Condition condition, long limit) throws Exception {
        long until=SystemClock.uptimeMillis()+limit;
        while(SystemClock.uptimeMillis()<until) {
            if(condition.met()) return;
            SystemClock.sleep(50);
        }
        fail(message);
    }
    private Object field(Object obj,String name) throws Exception {
        Field f=obj.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(obj);
    }
    private void event(int action,float... xy) {
        int n=xy.length/2;
        MotionEvent.PointerProperties[] pp=new MotionEvent.PointerProperties[n];
        MotionEvent.PointerCoords[] pc=new MotionEvent.PointerCoords[n];
        for(int i=0;i<n;i++) {
            pp[i]=new MotionEvent.PointerProperties();pp[i].id=i;pp[i].toolType=MotionEvent.TOOL_TYPE_FINGER;
            pc[i]=new MotionEvent.PointerCoords();pc[i].x=xy[i*2]*game.getWidth()/1280f;
            pc[i].y=xy[i*2+1]*game.getHeight()/720f;pc[i].pressure=1;pc[i].size=1;
        }
        if(action==MotionEvent.ACTION_DOWN)down=SystemClock.uptimeMillis();
        MotionEvent ev=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,n,pp,pc,0,0,1,1,0,0,0,0);
        ins.runOnMainSync(()->game.onTouchEvent(ev));ev.recycle();
    }
    private void tap(float x,float y) { event(MotionEvent.ACTION_DOWN,x,y);SystemClock.sleep(100);event(MotionEvent.ACTION_UP,x,y); }
    private void screenshot(String name) throws Exception {
        Bitmap image=ins.getUiAutomation().takeScreenshot();assertNotNull(image);
        File file=new File(ins.getTargetContext().getExternalFilesDir(null),name);
        try(FileOutputStream out=new FileOutputStream(file)){image.compress(Bitmap.CompressFormat.PNG,100,out);}
        image.recycle();
    }
    @Test public void recordAssistCooldownReadyAndRealTag() throws Exception {
        ins=InstrumentationRegistry.getInstrumentation();
        MainActivity activity=(MainActivity)ins.startActivitySync(new Intent(ins.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        game=(GameView)field(activity,"gameView");
        await("Surface ready",()->game.getWidth()>0,10000);
        await("Selection opened",()->((CharacterSelect)field(game,"characterSelect")).isOpen(),15000);
        tap((CharacterSelect.FIGHT_LEFT+CharacterSelect.FIGHT_RIGHT)/2,
            (CharacterSelect.FIGHT_TOP+CharacterSelect.FIGHT_BOTTOM)/2);
        await("Match selected",()->!((CharacterSelect)field(game,"characterSelect")).isOpen(),4000);
        await("Intro finished",()->(int)field(game,"introFrame")<0,15000);
        SystemClock.sleep(1000);
        TeamSystem.Side team=game.engine.team(0);
        FighterState original=team.pointState();
        tap(ControlsLayout.TAG_X,ControlsLayout.TAG_Y);
        await("Assist actually enters",team::assistOut,4000);
        await("Assist actually leaves",()->!team.assistOut(),7000);
        assertTrue("Real assist cooldown exists",team.assistCooldown>0);
        SystemClock.sleep(500);screenshot("hud-assist-cooldown.png");
        HudRenderer.State state=(HudRenderer.State)field(game,"hudState");
        assertTrue(state.assistStatusLabel().contains("s"));
        await("Real assist becomes ready",()->team.assistCooldown==0,9000);
        SystemClock.sleep(500);screenshot("hud-assist-ready.png");
        assertTrue(state.assistStatusLabel().contains("PRONTO"));
        // Raw TAG is the exact same down + TAG multi-touch combination as a player.
        event(MotionEvent.ACTION_DOWN,ControlsLayout.DPAD_X,ControlsLayout.DPAD_Y+80);
        SystemClock.sleep(100);
        event(MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            ControlsLayout.DPAD_X,ControlsLayout.DPAD_Y+80,ControlsLayout.TAG_X,ControlsLayout.TAG_Y);
        SystemClock.sleep(150);
        event(MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            ControlsLayout.DPAD_X,ControlsLayout.DPAD_Y+80,ControlsLayout.TAG_X,ControlsLayout.TAG_Y);
        event(MotionEvent.ACTION_UP,ControlsLayout.DPAD_X,ControlsLayout.DPAD_Y+80);
        await("Real engine swaps the point",()->team.pointState()!=original,5000);
        HudRenderer hud=(HudRenderer)field(game,"hud");
        await("Renderer observed real swap",()->hud.effects.tagAt>0,2000);
        screenshot("hud-tag-wave.png");
        await("Raw tag finishes",()->!team.tagging(),5000);
        assertTrue("Raw tag cooldown is independent",team.tagCooldown>0);
        SystemClock.sleep(1000);screenshot("hud-tag-countdown.png");
        assertTrue(state.swapStatusLabel().contains("s"));
        assertEquals("Raw tag cooldown must not empty the ready ASSIST meter",1,state.tagReadyRatio(),.001);
        SystemClock.sleep(2000);
        // Leave the activity visible until the recorder has closed; instrumentation ends.
    }
}
