package com.gamelutagpt;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

/** Animation regression: P01 2H must display every pose in sequence. */
public class P01CrouchMovementTest {
    private static final CharacterDefinition P01=GeneratedCharacters.get("player_base");

    @Test public void lowAttacksKeepTheirContactAndAllSourceFrames() {
        String[] moves={"2L","2M","2H"};
        String[] clips={"CROUCH_LIGHT","CROUCH_MEDIUM","CROUCH_HEAVY"};
        int[][] expected={{2,4,6},{12,4,24},{9,6,14}};
        int[] impact={2,6,4};
        for(int i=0;i<moves.length;i++){
            AttackDefinition a=P01.attack(moves[i]);
            CharacterDefinition.Animation art=P01.animation(clips[i]);
            assertEquals(expected[i][0],a.startupFrames);
            assertEquals(expected[i][1],a.activeFrames);
            assertEquals(expected[i][2],a.recoveryFrames);
            float length=a.totalFrames/(float)CombatConfig.FPS;
            assertEquals("Impact sprite "+moves[i],impact[i],
                art.frame(art.timeFor(a.startupFrames/(float)CombatConfig.FPS,length),0f));
            List<Integer> displayed=new ArrayList<>();
            for(int t=0;t<a.totalFrames;t++){
                int frame=art.frame(art.timeFor((t+.5f)/(float)CombatConfig.FPS,length),0f);
                if(!displayed.isEmpty())assertTrue("A pose went backwards in "+moves[i],
                    frame>=displayed.get(displayed.size()-1));
                displayed.add(frame);
            }
            for(int p=0;p<art.atlas.count;p++) {
                assertTrue("Dropped "+moves[i]+" sprite pose "+p+"; played "+displayed,displayed.contains(p));
            }
        }
        assertEquals(AttackDefinition.Launch.LAUNCH,P01.attack("2H").launch);
        assertTrue(P01.attack("2H").cancelsInto(AttackDefinition.JUMP));
    }

    @Test public void crouchMediumShowsAllSixteenPosesAtThirtyFpsWithEitherRenderPhase() {
        CharacterDefinition.Move move=P01.moves.get("2M");
        CharacterDefinition.Animation art=move.animation;
        assertEquals(40,move.attack.totalFrames);
        assertEquals(16,art.atlas.count);
        for(int phase=0;phase<2;phase++){
            List<Integer> shown=new ArrayList<>();
            for(int frame=phase;frame<move.attack.totalFrames;frame+=2){
                float clock=(frame+.5f)/CombatConfig.FPS;
                shown.add(art.frame(move.animationTime(clock),0f));
            }
            for(int pose=0;pose<16;pose++){
                assertTrue("At 30fps phase="+phase+" pose="+pose+" skipped: "+shown,
                    shown.contains(pose));
            }
            for(int i=1;i<shown.size();i++)
                assertTrue("30fps frame order reversed",shown.get(i)>=shown.get(i-1));
        }
        assertEquals("Pose 6 at impact",6,art.frame(
            move.animationTime(move.attack.startupFrames/(float)CombatConfig.FPS),0f));
    }

    @Test public void twoLightCancelsIntoTwoMediumAndCombos() {
        CombatEngine e=new CombatEngine(
            new FighterState(P01,"P01"),
            new FighterState(GeneratedCharacters.opponentCharacter(),"CPU"),
            500f,600f,new CombatConfig());
        FighterInput in=new FighterInput(),enemy=new FighterInput();
        in.direction=3;
        in.light=true;
        e.step(in,enemy);
        in.clearPresses();
        for(int t=0;t<30 && e.fighter(0).outcome!=CombatFighter.Outcome.HIT;t++)
            e.step(in,enemy);
        assertEquals(CombatFighter.Outcome.HIT,e.fighter(0).outcome);
        in.medium=true;
        e.step(in,enemy);
        in.clearPresses();
        int maxHits=0;
        boolean mediumStarted=false;
        for(int t=0;t<70;t++){
            e.step(in,enemy);
            if(e.fighter(0).attacking() && "2M".equals(e.fighter(0).attack.id))
                mediumStarted=true;
            // A 2M knockdown ends the active combo in this same engine step.
            // Inspect both the live and the finalized session, or we would
            // report a false gap even for a confirmed 2-hit knockdown combo.
            ComboSession live=e.session(1);
            ComboSession finalized=e.lastSession(1);
            if(live!=null)maxHits=Math.max(maxHits,live.hitCount);
            if(finalized!=null)maxHits=Math.max(maxHits,finalized.hitCount);
        }
        assertTrue("2M should remain cancellable from 2L",mediumStarted);
        assertTrue("2L > 2M should remain one combo, observed "+maxHits,maxHits>=2);
    }

    @Test public void dashLoopIsSlowerWithoutAlteringSpeed() {
        CharacterDefinition.Animation dash=P01.animation(SpriteStates.DASH);
        assertEquals(.288f,dash.duration,.0001f);
        assertEquals(6,dash.atlas.count);
        assertEquals(620f,new CombatConfig().dashSpeed,.001f);
    }
}
