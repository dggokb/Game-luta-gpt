package com.gamelutagpt;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

/** JVM tests for the systems split out of GameView: AI, controls, commands and camera. */
public class ExtractedSystemsTest {
    private static final CharacterDefinition NPC=GeneratedCharacters.opponentCharacter();
    private static final CharacterDefinition.Body PLAYER=GeneratedCharacters.defaultCharacter().fighter.body;

    private static final class Recorder implements OpponentAi.Actions {
        final List<String> log=new ArrayList<>();
        @Override public void stop(){}
        @Override public void attack(String type){log.add("attack:"+type);}
        @Override public void superAttack(){log.add("super");}
        @Override public void energy(String s){log.add("energy:"+s);}
        @Override public void backdash(){log.add("backdash");}
        @Override public void moveForward(boolean dash){log.add(dash?"dash":"walk");}
        @Override public void jump(boolean s){log.add(s?"superjump":"jump");}
        @Override public void grab(){log.add("grab");}
    }
    private static OpponentAi.Situation situation(float distance) {
        OpponentAi.Situation s=new OpponentAi.Situation();
        s.self=NPC;s.target=PLAYER;s.distance=distance;s.targetGrounded=true;s.targetAlive=true;s.attackReady=true;
        return s;
    }

    @Test public void aiNeverAttacksOutOfReachAndApproachesInstead() {
        for(int seed=0;seed<200;seed++) {
            OpponentAi ai=new OpponentAi(new Random(seed));Recorder r=new Recorder();
            float distance=170f;
            ai.think(1f,situation(distance),r);
            for(String action:r.log) if(action.startsWith("attack:")) {
                String type=action.substring(7);
                assertTrue(type+" out of reach",distance<=CombatRules.maxCenterDistance(NPC.move(type,false),PLAYER));
            }
        }
        // Far beyond every move and outside projectile range: it walks or dashes.
        OpponentAi ai=new OpponentAi(new Random(1));Recorder r=new Recorder();
        ai.think(1f,situation(700f),r);
        assertTrue(r.log.toString(),r.log.get(0).equals("walk")||r.log.get(0).equals("dash")||r.log.get(0).equals("super"));
    }
    @Test public void aiWaitsForItsDecisionTimer() {
        OpponentAi ai=new OpponentAi(new Random(3));Recorder r=new Recorder();
        assertTrue(ai.think(.01f,situation(120f),r));
        assertFalse("Decides at most every "+OpponentAi.DECISION_MIN+"s",ai.think(.01f,situation(120f),r));
        ai.reset();
        assertTrue(ai.think(.01f,situation(120f),r));
    }
    @Test public void aiDoesNothingAgainstDefeatedTarget() {
        OpponentAi.Situation s=situation(120f);s.targetAlive=false;
        Recorder r=new Recorder();new OpponentAi(new Random(5)).think(1f,s,r);
        assertTrue(r.log.isEmpty());
    }
    @Test public void controlsHitTestFollowsInputPriority() {
        assertEquals(ControlsLayout.Control.AI_TOGGLE,ControlsLayout.controlAt(1100,150));
        assertEquals(ControlsLayout.Control.DEBUG_TOGGLE,ControlsLayout.controlAt(1100,200));
        assertEquals(ControlsLayout.Control.HEAL_PLAYER,ControlsLayout.controlAt(1120,250));
        assertEquals(ControlsLayout.Control.HEAL_OPPONENT,ControlsLayout.controlAt(1210,250));
        assertEquals(ControlsLayout.Control.DPAD,ControlsLayout.controlAt(ControlsLayout.DPAD_X,ControlsLayout.DPAD_Y));
        assertEquals(ControlsLayout.Control.LIGHT,ControlsLayout.controlAt(ControlsLayout.LIGHT_X,ControlsLayout.LIGHT_Y));
        assertEquals(ControlsLayout.Control.TAG,ControlsLayout.controlAt(ControlsLayout.TAG_X,ControlsLayout.TAG_Y));
        assertEquals(ControlsLayout.Control.NONE,ControlsLayout.controlAt(640,300));
        assertEquals(0,ControlsLayout.dpadDirectionAt(ControlsLayout.DPAD_X+5,ControlsLayout.DPAD_Y));
        assertEquals(1,ControlsLayout.dpadDirectionAt(ControlsLayout.DPAD_X+90,ControlsLayout.DPAD_Y));
        assertEquals(3,ControlsLayout.dpadDirectionAt(ControlsLayout.DPAD_X,ControlsLayout.DPAD_Y+90));
        assertEquals(5,ControlsLayout.dpadDirectionAt(ControlsLayout.DPAD_X-90,ControlsLayout.DPAD_Y));
        assertEquals(7,ControlsLayout.dpadDirectionAt(ControlsLayout.DPAD_X,ControlsLayout.DPAD_Y-90));
    }
    @Test public void motionParserAcceptsDiagonalsAndRejectsSlowInput() {
        int[] quarterCircle={3,1};
        MotionParser m=new MotionParser();
        m.record(3,100);m.record(2,102);m.record(1,104);
        assertTrue(m.matchCommand(quarterCircle,106,15,15));
        assertFalse("A match consumes the history",m.matchCommand(quarterCircle,106,15,15));
        m.record(3,200);m.record(1,200+16);
        assertFalse("Steps too far apart",m.matchCommand(quarterCircle,200+17,15,15));
        m.clear();m.record(3,300);m.record(1,302);
        assertFalse("Too late to press",m.matchCommand(quarterCircle,302+16,15,15));
        m.clear();m.record(1,400);m.record(0,402);m.record(1,410);
        assertTrue("Double tap",m.doubleTap(1,410,18));
        m.clear();m.record(2,500);m.record(7,510);
        assertTrue("Down then up",m.downThenUp(510,22));
        assertEquals("Facing left mirrors the pad",1,MotionParser.relative(5,-1));
        for(int i=0;i<40;i++)m.record(i%2==0?5:1,600+i);
        assertFalse(m.matchCommand(quarterCircle,640,15,15));
    }
    @Test public void cameraZoomsOutForDistantFightersAndKeepsTallOnesInFrame() {
        CameraRig near=new CameraRig(),far=new CameraRig();
        for(int i=0;i<600;i++){near.update(1/120f,500,700,Arena.GROUND_Y-230,false);far.update(1/120f,200,1500,Arena.GROUND_Y-230,false);}
        assertTrue(far.zoom<near.zoom);
        assertTrue(far.zoom>=CameraRig.CAMERA_MIN_ZOOM-1e-4f);
        CameraRig tall=new CameraRig();
        float top=Arena.GROUND_Y-600;
        for(int i=0;i<1200;i++)tall.update(1/120f,500,700,top,false);
        assertTrue("Top of the tallest fighter stays visible",tall.top<=top);
    }
}
