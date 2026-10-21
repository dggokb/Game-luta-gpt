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

    @Test public void renderFightImpactSequence() throws Exception {
        game=new GameView(RuntimeEnvironment.getApplication());game.layout(0,0,1280,720);
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

    /** Stills of overlays that a short scripted fight does not always reach. */
    @Test public void renderEffectShowcase() throws Exception {
        game=new GameView(RuntimeEnvironment.getApplication());game.layout(0,0,1280,720);
        set("introFrame",-1);
        CombatFighter p=game.engine.fighter(0),npc=game.engine.fighter(1);
        npc.x=p.x+170f;
        frames(2);
        File dir=new File("build/visual-review/showcase");
        Bitmap b=Bitmap.createBitmap(1280,720,Bitmap.Config.ARGB_8888);
        ImpactEffects impacts=(ImpactEffects)get("impacts");
        float y=npc.y-150f;
        // Hit sparks of each size and a guard spark, a few frames after spawning.
        impacts.spawn(ImpactEffects.HIT,npc.x-30f,y,1,ImpactEffects.HEAVY,0xFF22BFFF);
        for(int i=0;i<3;i++) {impacts.update(1f/60);render(b);save(b,new File(dir,String.format("hit_heavy_%d.png",i)));}
        impacts.update(.2f);render(b);save(b,new File(dir,"hit_heavy_late.png"));
        impacts.clear();
        impacts.spawn(ImpactEffects.HIT,npc.x-30f,y,1,ImpactEffects.LIGHT,0xFF22BFFF);
        impacts.update(1f/30);render(b);save(b,new File(dir,"hit_light.png"));
        impacts.clear();
        impacts.spawn(ImpactEffects.GUARD,npc.x-30f,y,1,ImpactEffects.MEDIUM,0xFF22BFFF);
        impacts.update(1f/30);render(b);save(b,new File(dir,"guard.png"));
        impacts.clear();
        // Projectiles drawn by the production effects renderer over the fight frame.
        render(b);
        Canvas c=new Canvas(b);
        EffectsRenderer effects=new EffectsRenderer();
        java.util.List<Projectile> orbs=new java.util.ArrayList<>();
        Projectile small=new Projectile(p.x+40f,npc.y-140f,900f,600f,24f,10,0xFF22BFFF,0,1,null);
        small.x+=180f;orbs.add(small);
        AndroidRenderCanvas rc=new AndroidRenderCanvas();rc.begin(c);
        effects.drawEnergyProjectiles(c,rc,orbs);
        orbs.clear();
        Projectile big=new Projectile(p.x-200f,npc.y-300f,900f,600f,58f,10,0xFFFF4FA0,0,1,null);
        big.x+=260f;orbs.add(big);
        effects.drawSuperProjectiles(c,rc,orbs);
        c.save();c.translate(-200f,0f);
        effects.drawSuperCharge(c,new Paint(),900f,npc.y,.37f,0xFF22BFFF);
        c.restore();
        save(b,new File(dir,"projectiles_charge.png"));
        // Combo counter and banner.
        render(b);
        c=new Canvas(b);
        HudRenderer hud=(HudRenderer)get("hud");
        ComboSession combo=new ComboSession(0,1);
        combo.hitCount=7;combo.comboDamage=486;combo.damageScale=700;combo.airCombo=true;combo.juggleCount=3;
        hud.drawCombo(c,combo,null,0,true);
        ComboSession cpu=new ComboSession(1,0);cpu.hitCount=3;cpu.comboDamage=120;
        hud.drawCombo(c,cpu,null,0,false);
        hud.drawBanner(c,"AGARRÃO!",255,1.1f);
        save(b,new File(dir,"combo_banner.png"));
        // Low life and lost-life trail on the HUD.
        FighterState active=((FighterState[])get("team"))[0];
        hud.resetEffects();
        render(b);
        active.life=active.profile.maxLife/5;
        FighterState opp=(FighterState)get("opponentFighter");
        opp.life=opp.profile.maxLife/2;
        for(int i=0;i<4;i++) {frames(1);render(b);}
        save(b,new File(dir,"hud_low_life_hit.png"));
        b.recycle();
    }
}
