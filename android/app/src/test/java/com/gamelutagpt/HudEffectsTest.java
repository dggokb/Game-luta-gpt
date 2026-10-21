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
public class HudEffectsTest {
    private Object field(Object game, String name) throws Exception {
        Field f=game.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(game);
    }
    @Test public void damageHealAndMeterEventsAreRealAndContinuous() {
        FighterState f=new FighterState(GeneratedCharacters.get("player_base"),"P1");
        HudEffects.Meter m=new HudEffects.Meter(); m.seed(f);
        m.update(f,0); assertEquals(-100,m.hitAt,0);
        f.life-=100; m.update(f,1); assertEquals(1,m.hitAt,0);
        float old=m.trail; f.life-=50; m.update(f,1.1);
        assertEquals("Consecutive impacts retain the displayed tail",old,m.trail,.0001);
        m.update(f,2); assertEquals(m.life,m.trail,.0001);
        double hit=m.hitAt; m.update(f,3); assertEquals("Zero damage/guard cannot flash",hit,m.hitAt,0);
        f.life+=30; m.update(f,4); assertTrue(m.healTo>m.healFrom);
        f.superMeter=2200; m.update(f,5); assertEquals(2,m.gainTo);
        f.superMeter=500; m.update(f,6); assertEquals(2200,m.spentTo); assertEquals(500,m.spentFrom);
        FighterState other=new FighterState(GeneratedCharacters.get("player_base"),"P2");
        other.life=10; m.update(other,7); assertEquals(-100,m.hitAt,0);
        assertEquals(-100,m.spendAt,0);
    }
    @Test public void assistReadyIsOneShotAndResetSeedsWithoutEvents() throws Exception {
        GameView game=new GameView(RuntimeEnvironment.getApplication());
        HudRenderer.State state=(HudRenderer.State)field(game,"hudState");
        HudEffects e=new HudEffects(); e.update(state,0);
        assertEquals(-100,e.readyAt,0); assertEquals(-100,e.useAt,0);
        game.engine.team(0).assistPhase=TeamSystem.ASSIST_ENTER;
        e.update(state,.2); assertEquals(.2,e.useAt,0); assertEquals(0,e.assistRatio,0);
        e.update(state,.3); assertEquals(.2,e.useAt,0);
        game.engine.team(0).assistPhase=TeamSystem.ASSIST_NONE;
        game.engine.team(0).assistCooldown=game.engine.config.assistCooldownFrames;
        e.update(state,1); assertEquals(0,e.assistRatio,0);
        game.engine.team(0).assistCooldown=100; e.update(state,2); assertTrue(e.recharging);
        game.engine.team(0).assistCooldown=0; e.update(state,3); assertEquals(3,e.readyAt,0);
        e.update(state,4); assertEquals(3,e.readyAt,0);
        state.active().superMeter=CombatConfig.MAX_METER; e.update(state,5);
        assertEquals(1,e.assistRatio,0);
        state.active().restoreLife(); e.reset(); e.update(state,6);
        assertEquals(-100,e.point.healAt,0); assertEquals(-100,e.readyAt,0);
        assertEquals(CombatConfig.MAX_METER,e.point.superMeter);
        // Actual point swap establishes a fresh baseline even at different life/meter.
        game.engine.team(0).point=1; state.active().life=10; e.update(state,7);
        assertEquals(-100,e.point.hitAt,0); assertEquals(-100,e.point.spendAt,0);
        assertEquals("Real swap emits the tag wave once",7,e.tagAt,0);
        e.update(state,7.1);assertEquals(7,e.tagAt,0);
        e.reset();e.update(state,7.2);assertEquals("Restoring must not emit tag wave",-100,e.tagAt,0);
        HudRenderer.State disabled=(HudRenderer.State)Proxy.newProxyInstance(
            getClass().getClassLoader(),new Class[]{HudRenderer.State.class},
            (proxy,method,args)->method.getName().equals("reserve")?null:method.invoke(state,args));
        e.update(disabled,8); e.update(disabled,9);
        assertFalse(e.assistEnabled);assertFalse(e.recharging);assertEquals(-100,e.readyAt,0);
        assertFalse("Training CPU has no assist",game.engine.team(1).assistOut());
    }
    @Test public void elapsedTimeMatchesAcrossRenderRates() {
        FighterState f=new FighterState(GeneratedCharacters.get("player_base"),"P1");
        HudEffects.Meter a=new HudEffects.Meter(),b=new HudEffects.Meter();a.seed(f);b.seed(f);
        f.life-=300;a.update(f,0);b.update(f,0);
        for(int i=1;i<=15;i++)a.update(f,i/30.0);
        for(int i=1;i<=60;i++)b.update(f,i/120.0);
        assertEquals(a.trail,b.trail,.00001);
    }
    @Test public void generateNativeCanvasAnimation() throws Exception {
        GameView game=new GameView(RuntimeEnvironment.getApplication());game.layout(0,0,1280,720);
        HudRenderer hud=(HudRenderer)field(game,"hud");
        HudRenderer.State state=(HudRenderer.State)field(game,"hudState");
        HudConceptPortraits portraits=(HudConceptPortraits)field(game,"rostos");
        FighterState p=state.active(),cpu=state.opponent();
        Method scenario=GameView.class.getDeclaredMethod("drawScenario",Canvas.class);scenario.setAccessible(true);
        Method player=GameView.class.getDeclaredMethod("drawPlayer",Canvas.class);player.setAccessible(true);
        Bitmap bitmap=Bitmap.createBitmap(1280,720,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(bitmap);
        File dir=new File("build/hud-effects-frames");dir.mkdirs();
        for(int i=0;i<300;i++) {
            double t=i/30.0;
            if(i==20||i==26||i==34) {p.life-=p.profile.maxLife/8;p.recoverableLife+=p.profile.maxLife/16;}
            if(i==55) p.life+=p.profile.maxLife/6;
            if(i==70) cpu.life=cpu.profile.maxLife/5;
            if(i==72) game.engine.team(0).assistPhase=TeamSystem.ASSIST_ENTER;
            if(i==75) game.engine.team(0).assistPhase=TeamSystem.ASSIST_NONE;
            if(i>=75&&i<=120) game.engine.team(0).assistCooldown=(int)(game.engine.config.assistCooldownFrames*(120-i)/45f);
            if(i==80) p.superMeter=CombatConfig.METER_PER_BAR;
            if(i==95) p.superMeter=3*CombatConfig.METER_PER_BAR;
            if(i==125) p.superMeter=CombatConfig.MAX_METER;
            if(i==155) p.superMeter=CombatConfig.METER_PER_BAR;
            if(i==180) {hud.resetEffects();p.restoreLife();cpu.restoreLife();}
            if(i==205) game.engine.team(0).point=1;
            if(i>=205&&i<255)game.engine.team(0).tagCooldown=600-(i-205)*2;
            if(i==255) {hud.resetEffects();game.engine.team(0).point=0;game.engine.team(0).tagCooldown=0;}
            state.active().refreshHudLabels();cpu.refreshHudLabels();
            Method labels=GameView.class.getDeclaredMethod("updateTagCooldownLabels",TeamSystem.Side.class);
            labels.setAccessible(true);labels.invoke(game,game.engine.team(0));
            c.drawColor(Color.BLACK);scenario.invoke(game,c);player.invoke(game,c);
            hud.drawHudAt(c,state,portraits,t);hud.drawControls(c,state);
            Paint label=new Paint(Paint.ANTI_ALIAS_FLAG);label.setColor(Color.WHITE);label.setTextSize(18);
            c.drawText("PREVIA TECNICA • Canvas real da HUD / estados controlados",320,235,label);
            String event=i<20?"Repouso":i<55?"Impactos consecutivos":i<70?"Recuperacao":i<120?"Critico / ASSIST recarregando / niveis SUPER":i<155?"ASSIST pronto / SUPER maximo":i<180?"Descarga de SUPER":i<205?"Restauracao sem falso impacto":i<255?"TROCA: onda azul e brilho no novo titular":"Retorno da selecao sem falso impacto";
            c.drawText(event,380,261,label);
            try(FileOutputStream out=new FileOutputStream(new File(dir,String.format("%04d.png",i)))) {bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}
        }
        bitmap.recycle();
    }
}
