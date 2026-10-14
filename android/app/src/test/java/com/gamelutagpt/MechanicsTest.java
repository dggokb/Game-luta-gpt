package com.gamelutagpt;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/** Mecânicas próprias dos personagens (docs/personagens/mecanicas.md), no motor de verdade. */
public class MechanicsTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");

    private static AttackDefinition.Builder move(String id, int startup, int active, int recovery) {
        return new AttackDefinition.Builder(id, AttackDefinition.Kind.NORMAL).damage(300)
            .frames(startup, active, recovery).stun(20, 10, 0).windows(null, null, null)
            .pushback(30f, 30f).reach(140f, 78f);
    }

    private static CharacterDefinition with(AttackDefinition... attacks) {
        return CombatEngineTest.withMoves(BASE, attacks);
    }

    /** Copy of the base fighter with fighter-wide mechanics (fury, air dashes, own throw). */
    private static CharacterDefinition withFighter(int fury, int airDashes, int throwDamage, float throwRange) {
        CharacterDefinition.Fighter f = BASE.fighter;
        CharacterDefinition.Fighter fighter = new CharacterDefinition.Fighter(f.color, f.maxLife, f.autoCombo,
            f.energy, f.energyCommand, f.superAttack, f.body, f.inputPriority)
            .withMechanics(fury, airDashes, throwDamage, throwRange);
        Map<String, CharacterDefinition.Move> moves = new LinkedHashMap<>(BASE.moves);
        return new CharacterDefinition("mech_test", BASE.displayName, BASE.profile, BASE.artFacing,
            BASE.visualStandHeight, BASE.visualCrouchHeight, fighter, BASE.animations, moves, BASE.specialAnimations,
            BASE.specials);
    }

    private static boolean cue(CombatEngineTest.Sim s, String id) { return s.engine.cues().contains(id); }

    /** Fighter 1 starts H, fighter 0 jabs into it {@code delay} frames later; returns whether a cue showed up. */
    private static boolean jabInto(CombatEngineTest.Sim s, int delay, String cueId) {
        s.in[1].heavy = true;
        s.step();
        s.steps(delay);
        s.in[0].light = true;
        boolean seen = false;
        for (int i = 0; i < 20; i++) {
            s.step();
            seen |= cue(s, cueId);
        }
        return seen;
    }

    @Test public void counterStanceAnswersAStandingStrike() {
        AttackDefinition stance = move("H", 12, 3, 12)
            .mechanics(new AttackDefinition.Mechanics().counter(AttackDefinition.Reads.HIGH, 1, 11)).build();
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(BASE, with(stance), 500f, 600f);
        int max1 = s.f(1).state.life;
        assertTrue("COUNTER cue", jabInto(s, 1, "COUNTER"));
        assertEquals("the countering fighter takes nothing", max1, s.f(1).state.life);
        assertTrue("the jab was answered", s.f(0).state.life < s.f(0).state.profile.maxLife);
    }

    @Test public void lowCounterDoesNotReadAStandingStrike() {
        AttackDefinition stance = move("H", 12, 3, 12)
            .mechanics(new AttackDefinition.Mechanics().counter(AttackDefinition.Reads.LOW, 1, 11)).build();
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(BASE, with(stance), 500f, 600f);
        assertFalse(jabInto(s, 1, "COUNTER"));
        assertTrue("the jab landed", s.f(1).state.life < s.f(1).state.profile.maxLife);
    }

    @Test public void armorAbsorbsTheHitAndTheMoveGoesOn() {
        AttackDefinition charge = move("H", 20, 3, 10)
            .mechanics(new AttackDefinition.Mechanics().armor(1, 0, 19)).build();
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(BASE, with(charge), 500f, 600f);
        s.in[1].heavy = true;
        s.step();
        s.in[0].light = true;
        s.until(() -> cue(s, "ARMOR"), 12);
        assertTrue("damage still goes through", s.f(1).state.life < s.f(1).state.profile.maxLife);
        assertEquals("still attacking", "H", s.attackId(1));
        assertEquals(1, s.f(1).armorUsed);
    }

    @Test public void highEvasionSlipsAStandingStrikeButNotALowOne() {
        AttackDefinition dodge = move("H", 20, 3, 10)
            .mechanics(new AttackDefinition.Mechanics().evade(AttackDefinition.Reads.HIGH, 0, 22)).build();
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(BASE, with(dodge), 500f, 600f);
        s.in[1].heavy = true;
        s.step();
        s.in[0].light = true;
        s.steps(10);
        assertEquals("the standing jab passes over", s.f(1).state.profile.maxLife, s.f(1).state.life);
        CombatEngineTest.Sim control = new CombatEngineTest.Sim(BASE, with(move("H", 20, 3, 10).build()), 500f, 600f);
        control.in[1].heavy = true;
        control.step();
        control.in[0].light = true;
        control.steps(10);
        assertTrue("control: the jab hits a plain heavy", control.f(1).state.life < control.f(1).state.profile.maxLife);

        CombatEngineTest.Sim low = new CombatEngineTest.Sim(BASE, with(dodge), 500f, 600f);
        low.in[1].heavy = true;
        low.step();
        low.in[0].direction = 3;
        low.in[0].light = true;
        low.steps(10);
        assertTrue("a low strike still hits", low.f(1).state.life < low.f(1).state.profile.maxLife);
    }

    @Test public void evasionLetsAProjectileFlyThrough() {
        AttackDefinition dodge = move("H", 40, 3, 30)
            .mechanics(new AttackDefinition.Mechanics().evade(AttackDefinition.Reads.HIGH, 0, 72)).build();
        assertEquals("dodging: untouched", 0, projectileDamage(dodge));
        assertTrue("control: the same projectile hits a plain heavy", projectileDamage(move("H", 40, 3, 30).build()) > 0);
    }

    /** Damage fighter 1 takes from fighter 0's projectile while doing {@code heavy}. */
    private static int projectileDamage(AttackDefinition heavy) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(BASE, with(heavy), 400f, 700f);
        s.in[1].heavy = true;
        s.step();
        s.in[0].special = "M";
        boolean fired = false;
        for (int i = 0; i < 60; i++) {
            s.step();
            fired |= !s.engine.energyProjectiles.isEmpty();
        }
        assertTrue("the projectile was thrown", fired);
        return s.f(1).state.profile.maxLife - s.f(1).state.life;
    }

    @Test public void advanceCarriesTheBodyAndPassThroughCrossesUp() {
        AttackDefinition rush = move("H", 4, 3, 20)
            .mechanics(new AttackDefinition.Mechanics().advance(900f, 0, 22, false)).build();
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(with(rush), BASE, 300f, 1100f);
        float start = s.f(0).x;
        s.in[0].heavy = true;
        s.steps(20);
        assertTrue("moved forward", s.f(0).x - start > 200f);

        CombatEngineTest.Sim still = new CombatEngineTest.Sim(with(move("H", 4, 3, 20).build()), BASE, 300f, 1100f);
        still.in[0].heavy = true;
        still.steps(20);
        assertEquals("control: a plain heavy stays put", 300f, still.f(0).x, 1f);

        AttackDefinition cross = move("H", 30, 3, 10)
            .mechanics(new AttackDefinition.Mechanics().advance(900f, 0, 25, true)).build();
        CombatEngineTest.Sim c = new CombatEngineTest.Sim(with(cross), BASE, 500f, 640f);
        c.in[0].heavy = true;
        c.steps(26);
        assertTrue("ended on the other side", c.f(0).x > c.f(1).x);
    }

    @Test public void teleportReappearsBehindFacingTheOpponent() {
        AttackDefinition vanish = move("H", 12, 3, 12)
            .mechanics(new AttackDefinition.Mechanics().teleport(6).evade(AttackDefinition.Reads.ANY, 0, 8)).build();
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(with(vanish), BASE, 400f, 800f);
        s.in[0].heavy = true;
        s.until(() -> cue(s, "TELEPORT"), 12);
        assertTrue("behind", s.f(0).x > s.f(1).x);
        assertEquals("facing back toward the opponent", -1, s.f(0).facing);
    }

    @Test public void pullDragsTheDefenderIn() {
        AttackDefinition grabHand = move("H", 4, 3, 12).reach(420f, 78f)
            .mechanics(new AttackDefinition.Mechanics().pull(250f)).build();
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(with(grabHand), BASE, 300f, 650f);
        float before = s.f(1).x - s.f(0).x;
        s.in[0].heavy = true;
        s.until(() -> cue(s, "PULL"), 12);
        s.steps(12);
        assertTrue("closer than before", s.f(1).x - s.f(0).x < before - 150f);
        CombatEngineTest.Sim push = new CombatEngineTest.Sim(with(move("H", 4, 3, 12).reach(420f, 78f).build()), BASE, 300f, 650f);
        push.in[0].heavy = true;
        push.steps(24);
        assertTrue("control: a plain hit pushes away", push.f(1).x - push.f(0).x >= before);
    }

    @Test public void poisonDrainsOverTimeButNeverKills() {
        AttackDefinition needle = move("L", 3, 3, 7)
            .mechanics(new AttackDefinition.Mechanics().poison(900, 60)).build();
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(with(needle), BASE, 500f, 600f);
        s.f(1).state.life = 1200;
        s.in[0].light = true;
        s.until(() -> cue(s, "POISON"), 10);
        int afterHit = s.f(1).state.life;
        s.steps(90);
        assertTrue("it drained", s.f(1).state.life < afterHit);
        assertEquals("never the last point", 1, s.f(1).state.life);
        assertFalse(s.f(1).state.poisoned());
    }

    @Test public void lifeCostIsPaidWhenTheMoveStarts() {
        AttackDefinition frenzy = move("H", 8, 3, 12)
            .mechanics(new AttackDefinition.Mechanics().lifeCost(500)).build();
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(with(frenzy), BASE, 300f, 1100f);
        s.in[0].heavy = true;
        s.steps(2);
        assertEquals(s.f(0).state.profile.maxLife - 500, s.f(0).state.life);
    }

    @Test public void furyHitsHarderWithLessLife() {
        CharacterDefinition feral = withFighter(400, 1, 0, 0f);
        assertEquals(1000, feral.fighter.furyScale(feral.fighter.maxLife));
        assertEquals(1200, feral.fighter.furyScale(feral.fighter.maxLife / 2));
        CombatEngineTest.Sim calm = new CombatEngineTest.Sim(feral, BASE, 500f, 600f);
        CombatEngineTest.Sim angry = new CombatEngineTest.Sim(feral, BASE, 500f, 600f);
        angry.f(0).state.life = feral.fighter.maxLife / 2;
        for (CombatEngineTest.Sim s : new CombatEngineTest.Sim[]{calm, angry}) {
            s.in[0].light = true;
            s.steps(10);
        }
        int calmDamage = calm.f(1).state.profile.maxLife - calm.f(1).state.life;
        int angryDamage = angry.f(1).state.profile.maxLife - angry.f(1).state.life;
        assertEquals(calmDamage * 12 / 10, angryDamage);
    }

    @Test public void anAcrobatAirDashesTwicePerJump() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(withFighter(0, 2, 0, 0f), BASE, 300f, 1200f);
        s.in[0].jump = true;
        s.steps(8);
        s.in[0].dash = true;
        s.steps(2);
        assertEquals(1, s.f(0).airDashCount);
        s.until(() -> s.f(0).airDashFrames == 0, 30);
        s.in[0].dash = true;
        s.steps(2);
        assertEquals("second dash", 2, s.f(0).airDashCount);
        assertTrue(s.f(0).airDashFrames > 0);
    }

    @Test public void aGrapplerThrowsHarderAndFromFurther() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(withFighter(0, 1, 2200, 40f), BASE, 500f, 500f +
            BASE.fighter.body.halfWidth * 2 + 60f);
        s.in[0].grab = true;
        s.steps(60);
        assertEquals("own throw damage", s.f(1).state.profile.maxLife - 2200, s.f(1).state.life);
    }

    @Test public void everyCharacterCarriesItsOwnMechanic() {
        AttackDefinition.Reads high = AttackDefinition.Reads.HIGH, any = AttackDefinition.Reads.ANY;
        assertEquals(high, mech("p02", "S3").counter);
        assertTrue(GeneratedCharacters.get("p03").attack("S3").cancelsInto("S3"));
        assertTrue(mech("p03", "S3").advanceSpeed > 0f);
        assertEquals(high, mech("p04", "S3").evade);
        assertTrue(GeneratedCharacters.get("p04").attack("S3").cancelsInto("L"));
        assertTrue(mech("p05", "S3").teleportFrame > 0);
        assertTrue(mech("p06", "S3").pull > 0f);
        assertEquals(any, mech("p07", "S3").counter);
        assertEquals(AttackDefinition.Reads.LOW, mech("p08", "S3").counter);
        assertEquals(1, mech("p09", "S3").armorHits);
        assertEquals(2200, GeneratedCharacters.get("p09").fighter.throwDamage);
        assertEquals(2, GeneratedCharacters.get("p10").fighter.airDashes);
        assertTrue(mech("p10", "S4").passThrough);
        assertTrue(mech("p11", "S2").poisonDamage > 0);
        assertTrue(mech("p11", "S3").poisonDamage > 0);
        assertEquals(400, GeneratedCharacters.get("p12").fighter.furyPermille);
        assertTrue(mech("p12", "S2").lifeCost > 0);
    }

    private static AttackDefinition.Mechanics mech(String character, String move) {
        return GeneratedCharacters.get(character).attack(move).mechanics;
    }

    @Test public void p09ShieldAbsorbsARealJab() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(BASE, GeneratedCharacters.get("p09"), 500f, 620f);
        s.in[1].special = "S3";
        s.step();
        s.steps(2);
        s.in[0].light = true;
        s.until(() -> cue(s, "ARMOR"), 15);
        assertEquals("S3", s.attackId(1));
    }
}
