package com.gamelutagpt;
import org.junit.Test;
import static org.junit.Assert.*;

/** Pure JVM checks of hit geometry, frame data and pack-driven rules. */
public class CombatRulesTest {
    private static final CharacterDefinition BASE=GeneratedCharacters.get("player_base");
    private static final CharacterDefinition NPC=GeneratedCharacters.opponentCharacter();
    private static final CharacterDefinition.Body LEGACY=new CharacterDefinition.Body(34,145,90);

    @Test public void reachIsMeasuredToTheTargetHurtboxEdge() {
        CharacterDefinition.Move jab=BASE.moves.get("L");
        // Legacy center-to-center reach 118 is preserved for a 34-wide target...
        assertTrue(CombatRules.meleeConnects(0,565,1,jab,118,565,LEGACY,false));
        assertFalse(CombatRules.meleeConnects(0,565,1,jab,119,565,LEGACY,false));
        // ...and a wider body is touched sooner, in both directions.
        assertTrue(CombatRules.meleeConnects(0,565,1,jab,84+NPC.fighter.body.halfWidth,565,NPC.fighter.body,false));
        assertTrue(CombatRules.meleeConnects(500,565,-1,jab,500-84-NPC.fighter.body.halfWidth,565,NPC.fighter.body,false));
        assertFalse(CombatRules.meleeConnects(500,565,1,jab,400,565,NPC.fighter.body,false));
    }
    @Test public void overlappingBodiesAndFarVerticalTargetsDoNotConnect() {
        CharacterDefinition.Move jab=BASE.moves.get("L");
        assertFalse(CombatRules.meleeConnects(0,565,1,jab,10,565,LEGACY,false));
        assertTrue(CombatRules.meleeConnects(0,565,1,jab,80,565-90,LEGACY,false));
        assertFalse(CombatRules.meleeConnects(0,565,1,jab,80,565-110,LEGACY,false));
    }
    @Test public void projectileUsesCrouchHeightOfTheTarget() {
        float y=565-120;
        assertTrue(CombatRules.projectileHits(0,40,y,24,60,565,LEGACY,false));
        assertFalse(CombatRules.projectileHits(0,40,y,24,60,565,LEGACY,true));
        assertFalse(CombatRules.projectileHits(0,10,y,24,200,565,LEGACY,false));
    }
    @Test public void everyPackDeclaresEveryInputWithFrameDataInsideItsDuration() {
        String[] ids={"player_base","player_two","monster_npc"};
        String[] bindings={"L","M","H","2L","2M","2H","jL","jM","jH"};
        for(String id:ids)for(String b:bindings) {
            CharacterDefinition.Move m=GeneratedCharacters.get(id).moves.get(b);
            assertNotNull(id+"/"+b,m);
            assertTrue(m.activeStart<m.activeEnd && m.activeEnd<=m.totalTime+1e-6f);
            assertTrue(id+"/"+b+" needs art or pose",(m.animation==null)!=(m.pose==null));
        }
    }
    @Test public void airborneAndCrouchingInputsResolveToTheirOwnMoves() {
        assertEquals("jH",BASE.move("H",true).binding);
        assertEquals("jM",BASE.move("2M",true).binding);
        assertEquals("2M",BASE.move("2M",false).binding);
        assertEquals("RISING_SMASH",NPC.move("2H",false).animation.id);
        assertEquals(SpriteStates.POSE_AIR,NPC.move("L",true).pose);
    }
    @Test public void animationIsStretchedToGameplayDurationNotTheOtherWayAround() {
        CharacterDefinition.Animation jab=BASE.animation("LIGHT_JAB");
        CharacterDefinition.Move slow=new CharacterDefinition.Move("L",jab,null,300,jab.duration*2,.1f,.2f,84,78);
        assertEquals(jab.duration,slow.animationTime(slow.totalTime),1e-5f);
        assertEquals(jab.duration/2,slow.animationTime(slow.totalTime/2),1e-5f);
    }
    @Test public void threatWindowMatchesLegacyPhaseForPoseMoves() {
        CharacterDefinition.Move m=BASE.moves.get("2H");
        // Legacy anticipated guard: 20%..80% of the attack.
        assertFalse(m.threatening(.19f*m.totalTime));
        assertTrue(m.threatening(.21f*m.totalTime));
        assertTrue(m.threatening(.79f*m.totalTime));
        assertFalse(m.threatening(.81f*m.totalTime));
    }
    @Test public void opponentUsesItsOwnFighterRules() {
        assertEquals("Brutamonte",NPC.displayName);
        assertTrue(NPC.fighter.body.standHeight>BASE.fighter.body.standHeight);
        assertTrue(NPC.visualStandHeight>NPC.fighter.body.standHeight*0.9f);
        assertNotNull(NPC.specialAnimations.get("SUPER"));
        assertEquals(1,BASE.artFacing);
        assertTrue(BASE.visualStandHeight>BASE.visualCrouchHeight);
    }
    @Test public void aiPicksOnlyMovesThatReachKeepingWeights() {
        String[] types={"L","M","H"};double[] weights={1,1,1};
        float lReach=CombatRules.maxCenterDistance(NPC.moves.get("L"),LEGACY);
        float hReach=CombatRules.maxCenterDistance(NPC.moves.get("H"),LEGACY);
        // Beyond L/M but inside H: only H can be chosen, whatever the roll.
        float far=hReach-1;
        assertTrue(far>CombatRules.maxCenterDistance(NPC.moves.get("M"),LEGACY));
        for(double roll:new double[]{0,.3,.6,.99})
            assertEquals("H",CombatRules.pickInRange(types,weights,NPC,false,far,LEGACY,roll));
        assertNull(CombatRules.pickInRange(types,weights,NPC,false,hReach+1,LEGACY,.5));
        assertNull("Overlapping bodies cannot attack",CombatRules.pickInRange(types,weights,NPC,false,5,LEGACY,.5));
        assertEquals("L",CombatRules.pickInRange(types,weights,NPC,false,lReach-1,LEGACY,0));
        assertEquals("H",CombatRules.pickInRange(types,weights,NPC,false,lReach-1,LEGACY,.99));
    }
    @Test public void projectilesSpawnFromThePackLaunchPoint() {
        CharacterDefinition.Projectile energy=BASE.fighter.energy;
        assertEquals(82f,energy.spawnHeight(false,false),0);
        assertEquals(65f,energy.spawnHeight(true,false),0);
        assertEquals(86f,BASE.fighter.superAttack.spawnHeight(false,false),0);
        assertEquals(82f,BASE.fighter.superAttack.spawnHeight(false,true),0);
        assertTrue("A big fighter launches from higher up",
            NPC.fighter.energy.spawnHeight(false,false)>energy.spawnHeight(false,false));
        assertTrue(NPC.fighter.energy.spawnHeight(true,false)<=NPC.fighter.body.crouchHeight);
    }
}
