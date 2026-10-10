package com.gamelutagpt;

import static org.junit.Assert.*;
import org.junit.Test;

/** Universal movement tuning must behave equally for different character profiles. */
public class GlobalMovementTuningTest {
    private CombatEngine engine() {
        return new CombatEngine(
            new FighterState(GeneratedCharacters.get("player_base"),"P01"),
            new FighterState(GeneratedCharacters.opponentCharacter(),"CPU"),
            500f,1700f,new CombatConfig());
    }

    @Test public void higherNormalJumpAppliesToBothAndKeepsSuperJump() {
        CombatEngine e=engine();
        assertEquals(1030f,e.config.jumpSpeed,.001f);
        assertEquals(1450f,e.config.superJumpSpeed,.001f);
        assertEquals(1.70f,e.config.normalJumpRisingGravityScale,.001f);
        assertEquals(2.00f,e.config.normalJumpFallingGravityScale,.001f);
        FighterInput[] pad={new FighterInput(),new FighterInput()};
        pad[0].jump=true;pad[1].jump=true;
        e.step(pad[0],pad[1]);
        pad[0].clearPresses();pad[1].clearPresses();
        float[] apex={0f,0f};
        for(int t=0;t<75;t++) {
            for(int i=0;i<2;i++)apex[i]=Math.max(apex[i],Arena.GROUND_Y-e.fighter(i).y);
            e.step(pad[0],pad[1]);
        }
        for(int i=0;i<2;i++) {
            assertTrue("Low jump for fighter "+i+": "+apex[i],apex[i]>175f);
            assertTrue("Too high for fighter "+i+": "+apex[i],apex[i]<189f);
            assertTrue("Fighter did not land "+i,e.fighter(i).grounded);
        }
    }

    @Test public void neutralJumpMatchesDbfzCadenceWithApexJustOverRivalHeight() {
        CombatEngine e=engine();
        FighterInput jump=new FighterInput(),neutral=new FighterInput();
        jump.jump=true;
        e.step(jump,neutral);
        jump.clearPresses();
        int airborne=1;
        float apex=0;
        while(!e.fighter(0).grounded && airborne<100){
            apex=Math.max(apex,Arena.GROUND_Y-e.fighter(0).y);
            e.step(jump,neutral);
            airborne++;
        }
        assertTrue("Normal jump expected ~41-43 frames, got "+airborne,
            airborne>=40 && airborne<=44);
        float rivalHeight=e.fighter(1).body().standHeight;
        assertTrue("Normal jump must clear standing rival",apex>rivalHeight*1.10f);
        assertTrue("Normal jump should not reach superjump heights",apex<rivalHeight*1.23f);
    }

    @Test public void longerBackwardHopAndArcApplyToBoth() {
        CombatEngine e=engine();
        assertEquals(15,e.config.backdashFrames);
        assertEquals(978f,e.config.backdashSpeed,.001f);
        assertEquals(0f,e.config.backdashVisualLift(15),.001f);
        assertTrue(e.config.backdashVisualLift(7)>37f);
        assertEquals(0f,e.config.backdashVisualLift(0),.001f);
        FighterInput[] pad={new FighterInput(),new FighterInput()};
        float x0=e.fighter(0).x,x1=e.fighter(1).x;
        pad[0].backdash=true;pad[1].backdash=true;
        e.step(pad[0],pad[1]);
        pad[0].clearPresses();pad[1].clearPresses();
        for(int i=1;i<e.config.backdashFrames;i++)e.step(pad[0],pad[1]);
        float expected=e.config.backdashSpeed*e.config.backdashFrames*CombatConfig.DT;
        assertEquals(x0-expected,e.fighter(0).x,3f);
        assertEquals(x1+expected,e.fighter(1).x,3f);
        for(int i=0;i<2;i++) {
            assertTrue(e.fighter(i).grounded);
            assertEquals(0,e.fighter(i).backdashFrames);
        }
    }
    @Test public void winnerCannotWalkOrDashAfterKnockout() {
        CombatEngine e=engine();
        CombatFighter winner=e.fighter(0);
        e.fighter(1).state.life=0;
        float x=winner.x;
        FighterInput pressed=new FighterInput();
        pressed.direction=1;
        pressed.dash=true;
        pressed.backdash=true;
        FighterInput neutral=new FighterInput();
        for(int i=0;i<26;i++)e.step(pressed,neutral);
        assertEquals("Winner moved under its own victory pose",x,winner.x,.001f);
        assertEquals(0,winner.backdashFrames);
        assertFalse(winner.forwardDashing);
    }

    @Test public void normalJumpCanCrossOverAStandingOpponent() {
        // Test an actual crossover, not just an increased impulse value.
        CombatEngine e=new CombatEngine(
            new FighterState(GeneratedCharacters.get("player_base"),"P01"),
            new FighterState(GeneratedCharacters.opponentCharacter(),"CPU"),
            500f,600f,new CombatConfig());
        FighterInput moving=new FighterInput(),idle=new FighterInput();
        moving.jump=true;
        moving.direction=1;
        e.step(moving,idle);
        moving.clearPresses();
        boolean crossedAirborne=false;
        float maxHeight=0f,opponentX=e.fighter(1).x;
        for(int t=0;t<90;t++) {
            CombatFighter fighter=e.fighter(0);
            maxHeight=Math.max(maxHeight,Arena.GROUND_Y-fighter.y);
            if(!fighter.grounded && fighter.x>opponentX+5f) crossedAirborne=true;
            e.step(moving,idle);
        }
        assertTrue("Jump did not clear opponent standing height: "+maxHeight,
            maxHeight>e.fighter(1).body().standHeight+8f);
        assertTrue("Could not cross the opponent airborne",crossedAirborne);
        assertTrue("Must land on the other side",e.fighter(0).x>opponentX);
        assertTrue("Must land",e.fighter(0).grounded);
        assertEquals("Must face the crossed opponent",-1,e.fighter(0).facing);
    }

}
