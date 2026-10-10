package com.gamelutagpt;

import org.junit.Test;
import static org.junit.Assert.*;

/** Prevent visual retiming from breaking the P01 L -> M -> H launcher. */
public class P01HeavyLaunchRegressionTest {
    private static final CharacterDefinition P01=GeneratedCharacters.get("player_base");

    @Test public void impactFrameStaysInSyncWithGameplayAndMoveRemainsSlower() {
        AttackDefinition h=P01.attack("H");
        CharacterDefinition.Animation art=P01.animation("HEAVY_STRAIGHT");
        assertEquals(10,h.startupFrames);
        assertEquals(5,h.activeFrames);
        assertEquals(52,h.totalFrames);
        assertEquals(AttackDefinition.Launch.WALL_BOUNCE,h.launch);
        assertEquals(h.startupFrames/(float)h.totalFrames,
            art.timeOfFrame(10)/art.duration,0.03f);
        assertEquals("Contact must actually show artwork frame 10",10,
            art.frame(art.timeFor(h.startupFrames/(float)CombatConfig.FPS,
                h.totalFrames/(float)CombatConfig.FPS),0f));
    }

    @Test public void lightMediumHeavyRemainsSameComboAndThrowsOpponentToWall() {
        CombatEngineTest.Sim s=new CombatEngineTest.Sim(
            P01,GeneratedCharacters.opponentCharacter(),500f,600f);
        s.in[0].light=true;
        boolean l=false,m=false,h=false,bounce=false;
        ComboSession original=null;
        int count=0;
        for(int i=0;i<130;i++){
            if("L".equals(s.attackId(0)))s.in[0].medium=true;
            if("M".equals(s.attackId(0)))s.in[0].heavy=true;
            s.step();
            for(CombatEngine.HitEvent event:s.engine.events()){
                if(event.attacker!=0||event.blocked)continue;
                if("L".equals(event.moveId)){
                    l=true;original=s.engine.session(1);
                } else if("M".equals(event.moveId)){
                    m=true;
                } else if("H".equals(event.moveId)){
                    h=true;
                    assertSame("H must remain inside the L-M combo",original,s.engine.session(1));
                    count=s.engine.session(1).hitCount;
                    bounce=s.f(1).status==CombatFighter.Status.AIR_HITSTUN &&
                        s.f(1).bounce==CombatFighter.BOUNCE_WALL;
                }
            }
            if(h)break;
        }
        assertTrue("L must hit",l);
        assertTrue("M must hit",m);
        assertTrue("H must hit",h);
        assertTrue("H must wall-launch the opponent",bounce);
        assertEquals("One continuous three-hit combo",3,count);
    }
}
