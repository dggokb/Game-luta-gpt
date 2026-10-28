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

    private static AttackDefinition repel() {
        return move("H", 12, 3, 20).mechanics(new AttackDefinition.Mechanics()
            .counter(AttackDefinition.Reads.ANY, 1, 11).repel()).build();
    }

    @Test public void repelAnswersAStrikeWithoutTakingDamage() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(BASE, with(repel()), 500f, 600f);
        int max1 = s.f(1).state.life;
        assertTrue("REPEL cue", jabInto(s, 1, "REPEL"));
        assertEquals("the repel takes nothing", max1, s.f(1).state.life);
        assertTrue("the jab was repelled", s.f(0).state.life < s.f(0).state.profile.maxLife);
    }

    @Test public void repelReadsLowStrikesToo() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(BASE, with(repel()), 500f, 600f);
        s.in[1].heavy = true;
        s.step();
        s.in[0].direction = 1;
        s.in[0].light = true;
        boolean seen = false;
        for (int i = 0; i < 20; i++) { s.step(); seen |= cue(s, "REPEL"); }
        assertTrue("the crouching jab is repelled", seen);
        assertEquals(s.f(1).state.profile.maxLife, s.f(1).state.life);
    }

    @Test public void repelWithNothingToRepelDoesNotStrikeAndLeavesHerOpen() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(BASE, with(repel()), 500f, 600f);
        s.in[1].heavy = true;
        for (int i = 0; i < 20; i++) s.step();
        assertEquals("no burst without a repel", s.f(0).state.profile.maxLife, s.f(0).state.life);
        assertTrue("still recovering", s.f(1).attacking());
        s.in[0].light = true;
        s.until(() -> s.f(1).state.life < s.f(1).state.profile.maxLife, 20);
    }

    @Test public void repelDissipatesAProjectile() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(BASE, with(repel()), 300f, 600f);
        s.in[1].heavy = true;
        s.step();
        AttackDefinition energy = BASE.fighter.energy.attack;
        s.engine.energyProjectiles.add(new Projectile(560f, Arena.GROUND_Y - 120f, 600f, 900f, 30f, 500, 0, 0, 1, energy));
        boolean seen = false;
        for (int i = 0; i < 6; i++) { s.step(); seen |= cue(s, "REPEL"); }
        assertTrue("REPEL cue", seen);
        assertTrue("the projectile is gone", s.engine.energyProjectiles.isEmpty());
        assertEquals("no damage", s.f(1).state.profile.maxLife, s.f(1).state.life);
    }

    /** O jab do jogador 0 no p13 (jogador 1); devolve se ele ficou atordoado e quanto tirou. */
    private static int[] jabTheColossus(CombatEngineTest.Sim s, boolean heavy) {
        int life = s.f(1).state.life;
        if (heavy) s.in[0].heavy = true; else s.in[0].light = true;
        boolean stunned = false, cue = false;
        for (int i = 0; i < 30; i++) {
            s.step();
            stunned |= s.f(1).status == CombatFighter.Status.HITSTUN;
            cue |= cue(s, "COURACA");
            if (s.f(1).state.life < life && i > 0) break;
        }
        for (int i = 0; i < 4; i++) { s.step(); stunned |= s.f(1).status == CombatFighter.Status.HITSTUN; }
        return new int[]{stunned ? 1 : 0, life - s.f(1).state.life, cue ? 1 : 0};
    }

    @Test public void theColossusShrugsOffALightHitButNotTheSecondOne() {
        CharacterDefinition p13 = GeneratedCharacters.get("p13");
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(BASE, p13, 500f, 600f);
        int[] first = jabTheColossus(s, false);
        assertEquals("the first jab does not stun him", 0, first[0]);
        assertEquals("COURACA cue", 1, first[2]);
        assertTrue("it still hurts a little: " + first[1], first[1] > 0);
        for (int i = 0; i < 8; i++) s.step();
        int[] second = jabTheColossus(s, false);
        assertEquals("the second jab right after lands", 1, second[0]);
        assertTrue("full damage on the second: " + first[1] + " vs " + second[1], second[1] > first[1]);
    }

    @Test public void theColossusTakesAHeavyHit() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(BASE, GeneratedCharacters.get("p13"), 500f, 600f);
        int[] heavy = jabTheColossus(s, true);
        assertEquals("the heavy lands at once", 1, heavy[0]);
        assertEquals("no armor on it", 0, heavy[2]);
    }

    @Test public void theColossusKeepsAttackingThroughALightHit() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(BASE, GeneratedCharacters.get("p13"), 500f, 600f);
        s.in[1].heavy = true;
        s.step();
        s.in[0].light = true;
        boolean cue = false;
        for (int i = 0; i < 6; i++) { s.step(); cue |= cue(s, "COURACA"); }
        assertTrue("the jab hit the armor", cue);
        assertEquals("his H carries on", "H", s.attackId(1));
    }

    @Test public void theColossusRunsSlowerAndBackdashesLonger() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(BASE, GeneratedCharacters.get("p13"), 500f, 600f);
        CombatEngine e = s.engine;
        assertTrue("slower run", e.dashSpeed(s.f(1)) < e.dashSpeed(s.f(0)));
        assertTrue("longer backdash", e.backdashFrames(s.f(1)) > e.backdashFrames(s.f(0)));
        assertEquals("the others keep the engine's", e.config.backdashSpeed, e.backdashSpeed(s.f(0)), .001f);
        float d13 = e.backdashSpeed(s.f(1)) * e.backdashFrames(s.f(1));
        float d01 = e.backdashSpeed(s.f(0)) * e.backdashFrames(s.f(0));
        assertTrue("about the same distance: " + d13 + " vs " + d01, d13 < d01 && d13 > d01 * 0.7f);
    }

    /** O S3 do p13: agarra e dá três cabeçadas, a última com a pausa forte. */
    private static AttackDefinition headbutts() {
        return move("H", 10, 38, 24).damage(900).stun(28, 10, 8).strikes(10, 24, 45).strikeStop(7, 28)
            .strikeHold(88f, 88f, 88f).launch(AttackDefinition.Launch.KNOCKDOWN).knockback(90f)
            .mechanics(new AttackDefinition.Mechanics().grab(40f)).build();
    }

    /** {acertos, acertos com a pausa final, derrubou (0/1), agarrou (0/1)} */
    private static int[] grabRival(float rivalX) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(with(headbutts()), BASE, 500f, rivalX);
        s.in[0].heavy = true;
        int[] out = new int[4];
        for (int i = 0; i < 180; i++) {
            s.step();
            if (cue(s, "AGARRA")) out[3] = 1;
            if (s.f(1).status == CombatFighter.Status.KNOCKDOWN) out[2] = 1;
            for (CombatEngine.HitEvent e : s.engine.events()) {
                if (e.attacker != 0 || e.blocked) continue;
                out[0]++;
                if (e.finalStrike) out[1]++;
            }
        }
        return out;
    }

    @Test public void commandGrabHoldsTheRivalForThreeHeadbutts() {
        int[] near = grabRival(570f);
        assertEquals("grabbed", 1, near[3]);
        assertEquals("three headbutts", 3, near[0]);
        assertEquals("the last one is the heavy", 1, near[1]);
        assertEquals("and it knocks down", 1, near[2]);
    }

    @Test public void commandGrabWhiffsOutOfReach() {
        int[] far = grabRival(760f);
        assertEquals("nobody grabbed", 0, far[3]);
        assertEquals("no headbutt lands", 0, far[0]);
        CharacterDefinition p13 = GeneratedCharacters.get("p13");
        assertTrue("the colossus grabs the air instead of headbutting it",
            p13.animations.containsKey(p13.specials.get("S3").move.animation.id + "_ERRO"));
    }

    /** O S1 do p14: o chute que mergulha; acertou, quica e chuta de novo. */
    private static AttackDefinition reboundKicks() {
        return move("H", 12, 40, 20).damage(1200).stun(20, 10, 4).strikes(12, 22, 32, 42).strikeStop(4, 14)
            .strikeHold(95f, 95f, 95f, 95f).launch(AttackDefinition.Launch.KNOCKDOWN).knockback(110f)
            .mechanics(new AttackDefinition.Mechanics().rebound()).build();
    }

    /** {acertos, defendidos, quadros até ele voltar ao neutro} */
    private static int[] kickRival(float rivalX, boolean guard) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(with(reboundKicks()), BASE, 500f, rivalX);
        s.in[0].heavy = true;
        int[] out = new int[3];
        for (int i = 0; i < 160; i++) {
            if (guard) s.in[1].direction = 1;   // trás (o rival olha para a esquerda)
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) {
                if (e.attacker != 0) continue;
                if (e.blocked) out[1]++; else out[0]++;
            }
            if (out[2] == 0 && i > 2 && !s.f(0).attacking()) out[2] = i;
        }
        return out;
    }

    @Test public void theReboundKickBouncesAndKicksAgainOnHit() {
        int[] hit = kickRival(600f, false);
        assertEquals("four kicks", 4, hit[0]);
    }

    @Test public void theReboundKickStopsWhenItWhiffs() {
        int[] miss = kickRival(900f, false);
        int[] hit = kickRival(600f, false);
        assertEquals("nothing lands", 0, miss[0]);
        assertTrue("he lands and the move ends early: " + miss[2] + " vs " + hit[2], miss[2] < hit[2]);
    }

    @Test public void theReboundKickStopsWhenBlocked() {
        int[] blocked = kickRival(600f, true);
        assertEquals("no kick lands", 0, blocked[0]);
        assertEquals("only the first is blocked", 1, blocked[1]);
    }

    /** O S1 do p14 de verdade: do pulo, mergulha no chute; acertou, fica no ar e chuta mais. */
    private static AttackDefinition airReboundKicks() {
        return move("jH", 6, 4, 50).damage(1200).stun(20, 10, 4).strikes(6).launch(AttackDefinition.Launch.KNOCKDOWN).knockback(110f)
            .reach(150f, 120f)
            .mechanics(new AttackDefinition.Mechanics().rebound().dive(420f, 900f)).build();
    }

    /** {acertos, subiu depois do acerto (1/0), último quadro no ar, x do chute, x onde caiu}. */
    private static float[] diveOnto(float rivalX) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(with(airReboundKicks()), BASE, 500f, rivalX);
        CombatFighter f = s.f(0);
        f.grounded = false;
        f.y = Arena.GROUND_Y - 160f;
        f.vy = 0f;
        s.in[0].heavy = true;
        float[] out = {0, 0, 0, 0, 0};
        float hitY = -1f;
        for (int i = 0; i < 200; i++) {
            s.step();
            s.in[0].heavy = false;
            for (CombatEngine.HitEvent e : s.engine.events()) {
                if (e.attacker == 0 && !e.blocked) out[0]++;
            }
            if (out[0] == 1 && hitY < 0f) {
                hitY = f.y;
                out[3] = f.x;
            }
            if (hitY >= 0f && f.y < hitY - 20f) out[1] = 1;
            if (!f.grounded) {
                out[2] = i;
                out[4] = f.x;
            }
        }
        return out;
    }

    @Test public void theAirReboundKickHitsOnceAndHopsBack() {
        float[] hit = diveOnto(650f);
        assertEquals("one kick", 1f, hit[0], 0f);
        assertEquals("he hops up after it", 1f, hit[1], 0f);
        assertTrue("and lands back, away from the rival: " + hit[3] + " -> " + hit[4], hit[4] < hit[3] - 60f);
        float[] miss = diveOnto(1100f);
        assertEquals("nothing lands", 0f, miss[0], 0f);
        assertTrue("the miss lands sooner: " + miss[2] + " vs " + hit[2], miss[2] < hit[2]);
    }

    @Test public void theTricksterKicksAndCorkscrews() {
        CharacterDefinition p14 = GeneratedCharacters.get("p14");
        assertTrue("S1 rebounds", p14.fighter.energy.attack.mechanics.rebound);
        assertTrue("S1 dives from the jump", p14.fighter.energy.attack.mechanics.dives() && p14.fighter.energyAir);
        assertTrue("the Ultra ends past the rival", p14.fighter.ultraSequence.crossAhead > 0f);
        AttackDefinition sup = p14.fighter.superAttack.attack;
        assertTrue("the Super corkscrews through", sup.mechanics.passThrough && sup.mechanics.advanceSpeed > 0f);
        assertTrue("several strikes", sup.maxHits > 3);
    }

    @Test public void theColossusRockFliesInAnArcAndTheButtonSetsTheDistance() {
        CharacterDefinition.Projectile rock = GeneratedCharacters.get("p13").specials.get("S2").projectile;
        assertTrue("arc", rock.gravity > 0f && rock.vertical < 0f);
        assertTrue("L near, H far", rock.strengthSpeed[0] < rock.strengthSpeed[1] && rock.strengthSpeed[1] < rock.strengthSpeed[2]);
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(BASE, BASE, 300f, 1200f);
        Projectile p = new Projectile(400f, Arena.GROUND_Y - rock.spawnY, rock.range, rock.speed, 24f, 100, 0, 0, 1, null, "PEDRA");
        p.vy = rock.vertical;
        p.gravity = rock.gravity;
        s.engine.energyProjectiles.add(p);
        float peak = p.y;
        int frames = 0;
        while (s.engine.energyProjectiles.contains(p) && frames++ < 200) {
            s.step();
            peak = Math.min(peak, p.y);
        }
        assertTrue("it rises first", peak < Arena.GROUND_Y - rock.spawnY - 40f);
        assertTrue("and comes down on the ground: " + p.y, p.y >= Arena.GROUND_Y - 1f);
        assertTrue("lands a medium distance away: " + (p.x - 400f), p.x - 400f > 300f && p.x - 400f < 700f);
    }

    @Test public void theColossusJumpLiftsTheDrawingInAnArc() {
        AttackDefinition.Mechanics m = GeneratedCharacters.get("p13").specials.get("S4").move.attack.mechanics;
        int mid = (m.hopStart + m.hopEnd) / 2;
        assertEquals(0f, m.hopLift(m.hopStart), 0.01f);
        assertTrue("up in the middle: " + m.hopLift(mid), m.hopLift(mid) > 100f);
        assertEquals("down on the punch", 0f, m.hopLift(m.hopEnd), 0.01f);
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

    @Test public void negativeAdvanceBacksAway() {
        // A estrela da p16: o golpe recua em vez de avançar.
        AttackDefinition cartwheel = move("H", 4, 3, 20)
            .mechanics(new AttackDefinition.Mechanics().advance(-300f, 0, 22, false)).build();
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(with(cartwheel), BASE, 600f, 1100f);
        float start = s.f(0).x;
        s.in[0].heavy = true;
        s.steps(20);
        assertTrue("backed away from the opponent", start - s.f(0).x > 60f);
    }

    /** Caixa surpresa da p16 (→↓↘): cai e vira armadilha; o botão escolhe o pavio da luva. */
    private static int boxGloveHitFrame(boolean light, boolean heavy) {
        CharacterDefinition c = GeneratedCharacters.get("p16");
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(c, BASE, 500f, 750f);
        for (int d : new int[]{1, 1, 3, 3, 2}) { s.in[0].direction = d; s.step(); }
        s.in[0].direction = 2;
        if (light) s.in[0].light = true; else if (heavy) s.in[0].heavy = true; else s.in[0].medium = true;
        s.step();
        s.in[0].direction = 0;
        assertEquals("S2", s.attackId(0));
        int life = s.f(1).state.life;
        for (int i = 0; i < 200; i++) {
            s.step();
            if (s.f(1).state.life < life) return i;
        }
        return -1;
    }

    @Test public void boxTrapOpensByButtonAndTheGloveHits() {
        int l = boxGloveHitFrame(true, false), m = boxGloveHitFrame(false, false), h = boxGloveHitFrame(false, true);
        assertTrue("the glove hits whoever stands on the box: " + l + " " + m + " " + h, l > 0 && m > 0 && h > 0);
        assertTrue("L opens first, H last", l < m && m < h);
    }

    /** Hitstop of each strike of a several-strike H, by contact; {@code stop}: strikeStop (0 = default). */
    private static int[] strikeHitstops(int stop) {
        return strikeHitstops(stop, 0);
    }

    private static int[] strikeHitstops(int stop, int last) {
        AttackDefinition.Builder b = move("H", 6, 16, 12).stun(20, 10, 12).strikes(6, 16);
        if (stop > 0) b.strikeStop(stop, last);
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(with(b.build()), BASE, 500f, 600f);
        s.in[0].heavy = true;
        int[] stops = new int[2];
        int life = s.f(1).state.life, n = 0;
        for (int i = 0; i < 80 && n < 2; i++) {
            s.step();
            if (s.f(1).state.life < life) stops[n++] = s.f(0).hitstop;
            life = s.f(1).state.life;
        }
        return stops;
    }

    @Test public void strikeStopHoldsEveryStrike() {
        int[] plain = strikeHitstops(0), held = strikeHitstops(24);
        assertTrue("by default the first strike pauses less: " + plain[0] + " " + plain[1], plain[0] < plain[1]);
        assertEquals("pie", 24, held[0]);
        assertTrue("mallet, plus the hold of the last strike: " + held[1], held[1] >= 24);
    }

    @Test public void finalStopHoldsTheLastStrikeLonger() {
        int[] held = strikeHitstops(16, 34);
        assertEquals("punch", 16, held[0]);
        assertTrue("the closing kick: " + held[1], held[1] >= 34);
    }

    @Test public void strikeHoldPlacesTheDefenderAndPinsItDownUntilTheMoveEnds() {
        AttackDefinition pieAndMallet = move("H", 6, 16, 60).stun(20, 10, 12).strikes(6, 16)
            .strikeHold(285f, 230f).launch(AttackDefinition.Launch.KNOCKDOWN).knockback(0f).reach(300f, 78f).build();
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(with(pieAndMallet), BASE, 500f, 640f);
        s.in[0].heavy = true;
        s.until(() -> s.f(0).strikeLanded, 20);
        assertEquals("the pie puts the face on the cream", 285f, s.f(1).x - s.f(0).x, 0.5f);
        s.until(() -> s.f(1).status == CombatFighter.Status.KNOCKDOWN, 40);
        assertEquals("down under the mallet", 230f, s.f(1).x - s.f(0).x, 0.5f);
        while (s.f(0).attacking()) {
            assertEquals("stays down until the move ends", CombatFighter.Status.KNOCKDOWN, s.f(1).status);
            assertEquals(230f, s.f(1).x - s.f(0).x, 0.5f);
            s.step();
        }
        s.until(() -> s.f(1).status != CombatFighter.Status.KNOCKDOWN, 120);
    }

    /** Pancadas no chão (o Super do p13): a queda e três socos no rival deitado. */
    private static AttackDefinition groundPound() {
        return move("H", 6, 40, 30).stun(20, 10, 12).strikes(6, 16, 26, 36).strikeHold(150f, 150f, 150f, 150f)
            .launch(AttackDefinition.Launch.KNOCKDOWN).knockback(0f).reach(300f, 78f)
            .mechanics(new AttackDefinition.Mechanics().pound()).build();
    }

    @Test public void groundPoundKnocksDownWithTheFirstStrikeAndKeepsHittingTheLyingRival() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(with(groundPound()), BASE, 500f, 640f);
        s.in[0].heavy = true;
        s.until(() -> s.f(0).strikeLanded, 20);
        s.until(() -> s.f(1).status == CombatFighter.Status.KNOCKDOWN, 6);
        int hits = 1, life = s.f(1).state.life;
        while (s.f(0).attacking()) {
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) if (e.attacker == 0 && !e.blocked) hits++;
            if (s.f(0).attacking()) assertEquals("stays down", CombatFighter.Status.KNOCKDOWN, s.f(1).status);
        }
        assertEquals("the fall and the three punches on the ground", 4, hits);
        assertTrue("the punches took life", s.f(1).state.life < life);
        s.until(() -> s.f(1).status != CombatFighter.Status.KNOCKDOWN, 150);
    }

    @Test public void groundPoundThatMissesTheFallSkipsThePunchesAndEndsEarly() {
        AttackDefinition pound = groundPound();
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(with(pound), BASE, 500f, 1300f);
        s.in[0].heavy = true;
        s.until(() -> s.f(0).attacking(), 4);
        s.in[0].heavy = false;
        int frames = 0, hits = 0;
        while (s.f(0).attacking() && frames < 200) {
            s.step();
            frames++;
            if (frames == 12) s.f(1).x = s.f(0).x + 150f;   // o rival chega depois da queda: os socos não saem
            for (CombatEngine.HitEvent e : s.engine.events()) if (e.attacker == 0) hits++;
        }
        assertEquals("no punches without the fall", 0, hits);
        assertTrue("gets up early: " + frames, frames < pound.totalFrames - 10);
    }

    @Test public void ultraCameraPansSmoothlyAndTheFlightFollowsItsKeys() {
        CharacterDefinition.UltraSequence seq = new CharacterDefinition.UltraSequence(10, new int[]{20, 40}, 60, 1000, null)
            .withScene(new int[0][], null, null, new float[][]{{0f, 0f, 100f, 1.5f}, {20f, 0f, 500f, 1.2f, 10f}})
            .withFlight(new float[][]{{0f, 0f, 0f}, {10f, -40f, 200f}}, new int[]{5, 15}, new int[]{8, 12}, new float[]{300f, 50f});
        assertEquals("before the pan", 100f, seq.shotAt(19)[2], 0.01f);
        assertEquals("halfway up", 300f, seq.shotAt(25)[2], 0.01f);
        assertEquals("arrived", 500f, seq.shotAt(35)[2], 0.01f);
        assertEquals(100f, seq.flightAt(5)[1], 0.01f);
        assertEquals(-20f, seq.flightAt(5)[0], 0.01f);
        assertEquals(200f, seq.flightAt(50)[1], 0.01f);
        assertTrue(CharacterDefinition.UltraSequence.inside(seq.upsideDown, 5));
        assertFalse(CharacterDefinition.UltraSequence.inside(seq.upsideDown, 15));
    }

    @Test public void p13UltraLeavesTheRivalOnTheGroundNearby() {
        CombatEngineTest.Sim s = ultraSequence("p13", 120f);
        CombatConfig config = s.engine.config;
        s.until(() -> s.f(0).inUltraSequence(), config.ultraStartupFrames + config.ultraRushFrames + 4);
        float start = s.f(0).x;
        CharacterDefinition.UltraSequence seq = s.f(0).state.profile.ultraSequence;
        assertTrue("held upside down in the hug", seq.upsideDown != null && seq.armsInFront != null);
        s.until(() -> s.f(1).status == CombatFighter.Status.KNOCKDOWN, 2000);
        assertTrue("lands close, not across the stage: " + (s.f(1).x - s.f(0).x), Math.abs(s.f(1).x - s.f(0).x) < 400f);
        s.until(() -> !s.f(0).inUltraSequence(), 600);
        // O pulo para trás (240) e, no fim, o empurrão dos corpos encostados (o rival cai perto).
        float back = start - s.f(0).x;
        assertTrue("the jump back moved him: " + back, back >= 239f && back < 300f);
    }

    @Test public void thrownBoxSpinsTwiceUntilItLands() {
        Projectile box = new Projectile(300f, Arena.GROUND_Y - 215f, 600f, 115f, 40f, 100, 0, 0, -1, null, "CAIXA");
        assertEquals(0f, EffectsRenderer.boxSpin(box), 0.01f);
        box.y = Arena.GROUND_Y - 107.5f;
        assertEquals(-360f, EffectsRenderer.boxSpin(box), 0.01f);
        box.y = Arena.GROUND_Y;
        assertEquals("two full turns, upright on the ground", -720f, EffectsRenderer.boxSpin(box), 0.01f);
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
        assertEquals("rekka: three thrusts, like its animation", 3, GeneratedCharacters.get("p03").attack("S3").maxHits);
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
        assertTrue("p01 charges the Onda", mech("player_base", "S").chargeFrames > 0);
        assertTrue(GeneratedCharacters.get("player_base").animations.containsKey(SpriteStates.SPECIAL_CHARGE));
        assertTrue(GeneratedCharacters.get("player_base").animations.containsKey(SpriteStates.SPECIAL_ENERGY_CHARGED));
        assertTrue("p06 wakes the shadow", mech("p06", "S2").echoFrames > 0);
        assertTrue(GeneratedCharacters.get("p05").fighter.wallJump);
        assertTrue(GeneratedCharacters.get("p05").animations.containsKey(SpriteStates.WALL_JUMP));
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

    /** Copy of the base character with another fighter block (energy, wall jump...). */
    private static CharacterDefinition withFighterBlock(CharacterDefinition.Fighter fighter, AttackDefinition... moves) {
        CharacterDefinition c = CombatEngineTest.withMoves(BASE, moves);
        return new CharacterDefinition("mech_block", BASE.displayName, BASE.profile, BASE.artFacing,
            BASE.visualStandHeight, BASE.visualCrouchHeight, fighter, BASE.animations, c.moves, BASE.specialAnimations,
            BASE.specials);
    }

    private static CharacterDefinition chargeShooter() {
        CharacterDefinition.Fighter f = BASE.fighter;
        AttackDefinition shot = new AttackDefinition.Builder("S", AttackDefinition.Kind.PROJECTILE).damage(850)
            .frames(6, 1, 11).stun(20, 14, 6).windows(null, null, null).pushback(40f, 50f)
            .mechanics(new AttackDefinition.Mechanics().charge(60, 800)).build();
        CharacterDefinition.Projectile energy = new CharacterDefinition.Projectile(shot, f.energy.range, f.energy.speed,
            f.energy.spawnX, f.energy.spawnY, f.energy.crouchSpawnY, f.energy.airSpawnY);
        return withFighterBlock(new CharacterDefinition.Fighter(f.color, f.maxLife, f.autoCombo, energy,
            f.energyCommand, f.superAttack, f.body, f.inputPriority));
    }

    /** Fires S, holding the button for {@code hold} frames; returns the projectile that came out. */
    private static Projectile fire(int hold) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(chargeShooter(), BASE, 300f, 1200f);
        s.in[0].special = "M";
        s.in[0].holding = hold > 0;
        boolean charged = false;
        for (int i = 0; i < hold + 40 && s.engine.energyProjectiles.isEmpty(); i++) {
            if (i >= hold) s.in[0].holding = false;
            s.step();
            charged |= s.f(0).charging;
        }
        assertEquals("charging only while held", hold > 0, charged);
        assertFalse("a projectile came out", s.engine.energyProjectiles.isEmpty());
        return s.engine.energyProjectiles.get(0);
    }

    @Test public void holdingTheButtonChargesTheShot() {
        Projectile tap = fire(0), full = fire(80);
        assertTrue("bigger", full.radius > tap.radius * 1.5f);
        assertTrue("harder", full.damage > tap.damage * 3 / 2);
        assertTrue("faster", full.speed > tap.speed);
    }

    @Test public void theShadowRepeatsTheNextMoveLater() {
        AttackDefinition call = move("H", 6, 3, 10)
            .mechanics(new AttackDefinition.Mechanics().echo(300, 20, 500)).build();
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(with(call), BASE, 500f, 600f);
        s.in[0].heavy = true;
        s.until(() -> s.f(0).echoLeft > 0, 10);
        s.until(() -> !s.f(0).attacking(), 40);
        s.steps(30);
        s.in[0].light = true;
        boolean ownHit = false, shadowHit = false;
        for (int i = 0; i < 60; i++) {
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) {
                if (e.attacker != 0 || !"L".equals(e.moveId)) continue;
                if (e.projectile) shadowHit = true; else ownHit = true;
            }
        }
        assertTrue("the jab hit", ownHit);
        assertTrue("the shadow's copy hit later", shadowHit);
        assertTrue(s.engine.echoes.isEmpty());

        CombatEngineTest.Sim plain = new CombatEngineTest.Sim(BASE, BASE, 500f, 600f);
        plain.in[0].light = true;
        plain.steps(40);
        assertTrue("control: no shadow without the echo", plain.engine.echoes.isEmpty());
    }

    @Test public void aNinjaJumpsOffTheWall() {
        CharacterDefinition.Fighter f = BASE.fighter;
        CharacterDefinition ninja = withFighterBlock(new CharacterDefinition.Fighter(f.color, f.maxLife, f.autoCombo,
            f.energy, f.energyCommand, f.superAttack, f.body, f.inputPriority).withWallJump(true));
        float x = Arena.LEFT_BOUND + 20f;
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(ninja, BASE, x, x + 600f);
        s.in[0].jump = true;
        s.steps(8);
        s.in[0].jump = true;
        s.steps(2);
        assertTrue("off the wall", s.f(0).wallJumpUsed);
        s.steps(12);
        assertTrue("pushed away from the wall", s.f(0).x > x + 120f);

        CombatEngineTest.Sim plain = new CombatEngineTest.Sim(BASE, BASE, x, x + 600f);
        plain.in[0].jump = true;
        plain.steps(8);
        plain.in[0].jump = true;
        plain.steps(14);
        assertFalse("control: no wall jump without the mechanic", plain.f(0).wallJumpUsed);
        assertTrue(plain.f(0).x < x + 60f);
    }

    /** Super of fighter 0 with full meter; returns the hits it dealt (moveId SUPER) and whether a wave flew. */
    private static int[] superHits(CharacterDefinition caller) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(caller, BASE, 500f, 760f);
        s.f(0).state.superMeter = CombatConfig.MAX_METER;
        s.in[0].superAttack = true;
        int hits = 0, wave = 0, crew = 0;
        for (int i = 0; i < 240; i++) {
            s.step();
            if (!s.engine.superProjectiles.isEmpty()) wave = 1;
            crew = Math.max(crew, s.engine.henchmen.size());
            for (CombatEngine.HitEvent e : s.engine.events()) if (e.attacker == 0 && "SUPER".equals(e.moveId) && !e.blocked) hits++;
        }
        assertTrue("the henchmen leave the screen", s.engine.henchmen.isEmpty());
        return new int[]{hits, wave, crew};
    }

    @Test public void theSuperCallsHenchmenThatStrikeAndLeave() {
        CharacterDefinition.Fighter f = BASE.fighter;
        CharacterDefinition boss = withFighterBlock(new CharacterDefinition.Fighter(f.color, f.maxLife, f.autoCombo,
            f.energy, f.energyCommand, f.superAttack, f.body, f.inputPriority).withHenchmen(2));
        int[] crew = superHits(boss);
        assertEquals("two henchmen came", 2, crew[2]);
        assertEquals("no wave", 0, crew[1]);
        assertEquals("both struck", 2, crew[0]);
        int[] wave = superHits(BASE);
        assertEquals("control: the plain Super throws its wave", 1, wave[1]);
        assertEquals(0, wave[2]);
        assertTrue(GeneratedCharacters.get("p07").fighter.henchmen > 0);
    }

    // ------------------------------------------------------------ Ultra em sequência

    /** p02 encosta no boneco base e solta o Ultra (↓ + SUPER com 3 barras). */
    private static CombatEngineTest.Sim ultraSequence(String who, float gap) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get(who), BASE, 500f, 500f + gap);
        s.f(0).state.superMeter = CombatConfig.ULTRA_COST;
        s.in[0].direction = 3;
        s.in[0].superAttack = true;
        s.in[0].ultra = true;
        return s;
    }

    /** A tela liga a página de mangá: a sequência para antes do último golpe e retoma com a nota. */
    private static int ultraDamage(boolean page, float bonus) {
        CombatConfig config = new CombatConfig();
        config.ultraSequencePage = page;
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get("p02"), BASE, 500f, 610f, config);
        s.f(0).state.superMeter = CombatConfig.ULTRA_COST;
        s.in[0].direction = 3;
        s.in[0].superAttack = true;
        s.in[0].ultra = true;
        s.until(() -> s.f(0).inUltraSequence(), config.ultraStartupFrames + config.ultraRushFrames + 4);
        int life = s.f(1).state.life;
        CharacterDefinition.UltraSequence seq = s.f(0).state.profile.ultraSequence;
        boolean asked = false;
        for (int i = 0; i < 2000 && s.f(0).inUltraSequence(); i++) {
            s.step();
            if (s.engine.ultraConnected() == 0) {
                asked = true;
                assertTrue(s.engine.ultraWaitingPage(0));
                assertEquals("stops right before the final blow", seq.hits.length - 1, s.f(0).ultraHitsDone);
                int frame = s.f(0).ultraFrame, before = s.f(1).state.life;
                for (int k = 0; k < 90; k++) s.step();
                assertEquals("the sequence waits for the page", frame, s.f(0).ultraFrame);
                assertEquals(before, s.f(1).state.life);
                assertFalse("the victim stays held", s.f(1).canAct());
                s.engine.resumeUltraSequence(0, bonus);
                assertFalse(s.engine.ultraWaitingPage(0));
            }
        }
        assertEquals("only with the page on", page, asked);
        return life - s.f(1).state.life;
    }

    @Test public void ultraSceneHidesTheVictimAndBlacksOutTheScreen() {
        CharacterDefinition.UltraSequence seq = new CharacterDefinition.UltraSequence(10, new int[]{50, 120}, 200, 4000, null)
            .withScene(new int[][]{{40, 120}}, new int[]{60, 80}, new float[]{120f, 350f, 220f, 30f},
                new float[][]{{30f, 215f, 126f, 1.7f}, {70f, 320f, 178f, 1.35f}});
        assertFalse("seen before the hat", seq.hidesVictim(39));
        assertTrue("under the hat and in the cannon", seq.hidesVictim(40) && seq.hidesVictim(119));
        assertFalse("out of the cannon on the blast", seq.hidesVictim(120));
        assertEquals(0, seq.blackoutAlpha(59));
        assertTrue("fades in fast", seq.blackoutAlpha(60) > 0 && seq.blackoutAlpha(60) < 255);
        assertEquals(255, seq.blackoutAlpha(70));
        assertEquals("cuts back on the cannon", 0, seq.blackoutAlpha(80));
        assertEquals("the first shot holds from the start", 1.7f, seq.shotAt(0)[3], 0f);
        assertEquals("a hard cut to the cannon shot", 1.35f, seq.shotAt(70)[3], 0f);
        CharacterDefinition.UltraSequence p16 = GeneratedCharacters.get("p16").fighter.ultraSequence;
        assertNotNull("the Grande Final has its scene", p16.camera);
        assertTrue("the blast is the last blow", p16.blast[0] == p16.hits[p16.hits.length - 1]);
        assertFalse("the victim shows up for the blast", p16.hidesVictim(p16.blast[0]));
    }

    @Test public void mugenRendanVanishesInTheDarkAndReappearsFarForTheBelt() {
        CombatEngineTest.Sim s = ultraSequence("p15", 110f);
        CharacterDefinition.UltraSequence seq = s.f(0).state.profile.ultraSequence;
        assertTrue("two zanzoken", seq.vanish.length == 2 && seq.retreat != null);
        assertTrue("she vanishes into the blackout", seq.hidesAttacker(seq.blackout[0]) && seq.blackoutAlpha(seq.blackout[0] + 4) == 255);
        int inTheDark = 0;
        for (int h : seq.hits) if (h >= seq.blackout[0] && h < seq.blackout[1]) inTheDark++;
        assertTrue("hits in the dark: " + inTheDark, inTheDark >= 4);
        assertTrue("reappears back on the second zanzoken", seq.retreat[0] == seq.vanish[1][1]);
        int last = seq.hits[seq.hits.length - 1];
        assertTrue("the final blow lands while she is gone", seq.hidesAttacker(last));
        assertTrue("the manga page comes in as she vanishes", last - seq.vanish[1][0] <= new CombatConfig().ultraPageLeadFrames);
        assertTrue("she stays gone a while after it", seq.retreat[0] - last >= 50);
        assertTrue("the fight camera, no close-ups", seq.fightCamera);
        CombatConfig config = s.engine.config;
        s.until(() -> s.f(0).inUltraSequence(), config.ultraStartupFrames + config.ultraRushFrames + 4);
        s.until(() -> s.f(0).ultraHitsDone == seq.hits.length, 900);
        assertTrue("the final blow throws the rival", s.f(1).ultraFall);
        s.until(() -> s.f(0).ultraFrame >= seq.retreat[0] - 1, 200);
        float x = s.f(0).x;
        s.until(() -> s.f(0).ultraRetreated, 30);
        assertEquals("she reappears back", seq.retreat[1], Math.abs(s.f(0).x - x), 0.5f);
        assertFalse("while the rival is still flying", s.f(1).grounded);
        s.until(() -> !s.f(0).inUltraSequence(), 600);
    }

    @Test public void theCinematicUltraWaitsForTheMangaPageAndTheGradeBoostsTheLastBlow() {
        int plain = ultraDamage(false, 0f);
        assertEquals("a page without a grade changes nothing", plain, ultraDamage(true, 0f));
        int perfect = ultraDamage(true, 0.25f);
        assertTrue("PERFEITO hits harder: " + perfect + " vs " + plain, perfect > plain);
        assertTrue("only the last blow gets the bonus", perfect - plain < plain / 4);
    }

    @Test public void theUltraFollowsTheVideoHitByHit() {
        CombatEngineTest.Sim s = ultraSequence("p02", 110f);
        CharacterDefinition.UltraSequence seq = s.f(0).state.profile.ultraSequence;
        assertNotNull("p02 has its own Ultra", seq);
        CombatConfig config = s.engine.config;
        s.until(() -> s.f(0).inUltraSequence(), config.ultraStartupFrames + config.ultraRushFrames + 4);
        assertTrue("no cinematic and no beam", s.engine.ultraConnected() < 0 && !s.engine.inUltraCinematic(0));
        int life = s.f(1).state.life, hits = 0, hitstopFrames = 0;
        java.util.List<Integer> hitFrames = new java.util.ArrayList<>();
        float gap = (s.f(1).x - s.f(0).x) * s.f(0).facing;
        while (s.f(0).inUltraSequence() && hitstopFrames < 2000) {
            int frame = s.f(0).ultraFrame;
            s.step();
            hitstopFrames++;
            for (CombatEngine.HitEvent e : s.engine.events()) {
                if (e.attacker == 0 && "ULTRA".equals(e.moveId)) { hits++; hitFrames.add(s.f(0).ultraFrame); }
            }
            if (s.engine.cues().contains("ULTRA_FINISH")) {
                // The screen puts the impact frame on whoever stands across from the Ultra.
                assertEquals("still in the Ultra on its last hit", CombatFighter.Status.ULTRA, s.f(0).status);
            }
            if (s.f(0).ultraHitsDone < seq.hits.length && s.f(0).ultraFrame >= seq.hits[0]) {
                // Da primeira pancada em diante, preso onde a ponta das pancadas desenhadas encosta.
                float preso = seq.holdReach > 0f
                    ? Math.max(seq.holdReach, s.f(0).body().halfWidth + s.f(1).body().halfWidth) : gap;
                assertEquals("the defender stays held in front", preso, (s.f(1).x - s.f(0).x) * s.f(0).facing, 0.5f);
                assertFalse("nothing interrupts the sequence", s.f(0).hittable());
            }
        }
        assertEquals("one hit per strike of the animation", seq.hits.length, hits);
        for (int i = 0; i < seq.hits.length; i++) assertEquals("hit " + i + " lands on its frame", seq.hits[i], (int)hitFrames.get(i));
        assertEquals("the hits add up to the Ultra damage", seq.damage, life - s.f(1).state.life);
        assertEquals(CombatFighter.Status.NEUTRAL, s.f(0).status);
        assertTrue("the last hit throws the rival", s.f(1).ultraFall || s.f(1).status == CombatFighter.Status.KNOCKDOWN);
    }

    /**
     * O Ultra tira mais que a vida que sobra: o rival zera no primeiro golpe, mas continua preso
     * levando os golpes (sem dano) e só voa no golpe final. Antes ele saía voando 6 quadros
     * depois de zerar, e o golpe final e a finalização de mangá aconteciam no vento.
     */
    @Test public void aKnockedOutVictimStaysHeldUntilTheUltrasFinalBlow() {
        for (String id : GeneratedCharacters.SELECTABLE) {
            CharacterDefinition c = GeneratedCharacters.get(id);
            if (c.fighter.ultraSequence == null) continue;
            CombatEngineTest.Sim s = ultraSequence(id, 110f);
            CharacterDefinition.UltraSequence seq = s.f(0).state.profile.ultraSequence;
            CombatConfig config = s.engine.config;
            s.until(() -> s.f(0).inUltraSequence(), config.ultraStartupFrames + config.ultraRushFrames + 4);
            s.f(1).state.life = 1;
            int hits = 0, guard = 0;
            boolean finish = false, knockedOutEarly = false;
            while (s.f(0).inUltraSequence() && guard++ < 2000) {
                s.step();
                for (CombatEngine.HitEvent e : s.engine.events()) if (e.attacker == 0 && "ULTRA".equals(e.moveId)) hits++;
                if (s.engine.cues().contains("ULTRA_FINISH")) finish = true;
                if (s.f(1).ko() && s.f(0).ultraHitsDone < seq.hits.length) {
                    knockedOutEarly = true;
                    assertFalse(id + ": not thrown before the final blow", s.f(1).ultraFall);
                    assertTrue(id + ": the Ultra still holds the KO", s.engine.ultraHoldsKo(1));
                }
            }
            assertTrue(id + ": the first hit already knocked it out", knockedOutEarly);
            assertEquals(id + ": every strike still lands", seq.hits.length, hits);
            assertTrue(id + ": the final blow happens", finish);
            assertTrue(id + ": the final blow throws it",
                s.f(1).ultraFall || s.f(1).status == CombatFighter.Status.KNOCKDOWN);
            assertFalse(s.engine.ultraHoldsKo(1));
        }
    }

    /** p09 e p11: os Ultras dos vídeos novos batem nos quadros e somam o dano. */
    @Test public void theP09AndP11UltrasHitOnTheirFrames() {
        for (String id : new String[]{"p09", "p11"}) {
            CombatEngineTest.Sim s = ultraSequence(id, 110f);
            CharacterDefinition.UltraSequence seq = s.f(0).state.profile.ultraSequence;
            CombatConfig config = s.engine.config;
            s.until(() -> s.f(0).inUltraSequence(), config.ultraStartupFrames + config.ultraRushFrames + 4);
            int life = s.f(1).state.life, hits = 0, guard = 0;
            float highest = 0f;
            while (s.f(0).inUltraSequence() && guard++ < 2000) {
                s.step();
                for (CombatEngine.HitEvent e : s.engine.events()) if (e.attacker == 0 && "ULTRA".equals(e.moveId)) hits++;
                if (s.f(0).ultraHitsDone < seq.hits.length) highest = Math.max(highest, Arena.GROUND_Y - s.f(1).y);
            }
            assertEquals(id + ": one hit per strike", seq.hits.length, hits);
            assertEquals(id + ": the hits add up to the Ultra damage", seq.damage, life - s.f(1).state.life);
            if (id.equals("p09")) {
                // O rival fica onde o saco estava no vídeo: erguido acima da cabeça no pulo.
                assertTrue("p09 lifts the rival over his head: " + highest, highest > 200f);
                float[] slam = seq.holdAt(seq.hits[seq.hits.length - 1]);
                assertEquals("the slam puts it back on the ground", 0f, slam[1], 0.5f);
            } else {
                assertEquals("p11 strikes in place, without lifting", 0f, highest, 0.5f);
            }
        }
    }

    @Test public void theUltraWhiffsFromFarWithoutStartingTheSequence() {
        CombatEngineTest.Sim s = ultraSequence("p03", 900f);
        CombatConfig config = s.engine.config;
        boolean sequence = false;
        for (int i = 0; i < config.ultraStartupFrames + config.ultraRushFrames + config.ultraRecoveryFrames + 10; i++) {
            s.step();
            sequence |= s.f(0).inUltraSequence();
        }
        assertFalse(sequence);
        assertEquals(CombatFighter.Status.NEUTRAL, s.f(0).status);
        assertEquals(s.f(1).state.profile.maxLife, s.f(1).state.life);
    }

    @Test public void onlyTheBeamFightersKeepTheBeam() {
        for (String id : new String[]{"p02", "p03", "p04", "p05", "p07", "p08", "p09", "p10", "p11", "p12"}) {
            CharacterDefinition c = GeneratedCharacters.get(id);
            assertNotNull(id + " Ultra follows its video", c.fighter.ultraSequence);
            assertNotNull(id + " has the Ultra sheet", c.animations.get(SpriteStates.ULTRA_SEQ));
        }
        for (String id : new String[]{"player_base", "p06"}) {
            assertNull(id + " keeps the beam", GeneratedCharacters.get(id).fighter.ultraSequence);
        }
    }

    // ------------------------------------------------------------ golpes de várias pancadas

    /** Runs {@code move} of {@code who} against the base dummy, returning the frames each hit landed. */
    private static java.util.List<Integer> strikesOf(String who, String move, boolean superMove, float gap, int[] damage) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get(who), BASE, 500f, 500f + gap);
        if (superMove) {
            s.f(0).state.superMeter = CombatConfig.MAX_METER;
            s.in[0].superAttack = true;
        } else {
            s.in[0].special = move;
        }
        java.util.List<Integer> frames = new java.util.ArrayList<>();
        int life = s.f(1).state.life;
        for (int i = 0; i < 200; i++) {
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) {
                if (e.attacker == 0 && !e.blocked && move.equals(e.moveId)) frames.add(s.f(0).attackFrame);
            }
            if (i > 10 && !s.f(0).attacking()) break;
        }
        damage[0] = life - s.f(1).state.life;
        return frames;
    }

    @Test public void aSeveralStrikeSpecialHitsOnEachStrikeOfItsAnimation() {
        AttackDefinition up = GeneratedCharacters.get("p03").attack("S2");
        assertEquals("the uppercut hits three times like its animation", 3, up.maxHits);
        int[] damage = new int[1];
        java.util.List<Integer> frames = strikesOf("p03", "S2", false, 100f, damage);
        assertEquals(3, frames.size());
        for (int i = 0; i < 3; i++) {
            assertTrue("strike " + i + " lands in its window",
                up.hitbox(i).activeAt(frames.get(i)) || up.hitbox(i).activeAt(frames.get(i) - 1));
        }
        assertEquals("the strikes add up to the move's damage", up.damage, damage[0]);
    }

    @Test public void onlyTheLastStrikeKnocksDown() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get("p03"), BASE, 500f, 600f);
        s.in[0].special = "S2";
        int hits = 0;
        boolean downEarly = false;
        for (int i = 0; i < 120 && hits < 3; i++) {
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) if (e.attacker == 0 && !e.blocked) hits++;
            if (hits > 0 && hits < 3 && s.f(1).status == CombatFighter.Status.KNOCKDOWN) downEarly = true;
            if (hits > 0 && hits < 3) {
                assertTrue("held between strikes",
                    s.f(1).status == CombatFighter.Status.HITSTUN || s.f(1).status == CombatFighter.Status.AIR_HITSTUN);
            }
        }
        assertEquals(3, hits);
        assertFalse(downEarly);
    }

    @Test public void aStrikingSuperHitsWithoutAWave() {
        AttackDefinition sup = GeneratedCharacters.get("p04").attack("SUPER");
        assertTrue("p04's Super strikes", sup.melee());
        int[] damage = new int[1];
        java.util.List<Integer> frames = strikesOf("p04", "SUPER", true, 110f, damage);
        assertEquals("three punches like the animation", sup.maxHits, frames.size());
        assertEquals(sup.damage, damage[0]);
        // Control: the Super of p09 (fire on the ground) still throws its wave.
        assertFalse(GeneratedCharacters.get("p09").attack("SUPER").melee());
        assertFalse(GeneratedCharacters.get("player_base").attack("SUPER").melee());
    }

    @Test public void theStrikingSuperDoesNotSpawnAProjectile() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get("p02"), BASE, 500f, 610f);
        s.f(0).state.superMeter = CombatConfig.MAX_METER;
        s.in[0].superAttack = true;
        for (int i = 0; i < 150; i++) {
            s.step();
            assertTrue("no wave", s.engine.superProjectiles.isEmpty());
        }
    }

    @Test public void aGuardHoldsEveryStrikeAndStaysInReach() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get("p03"), BASE, 500f, 600f);
        s.in[1].direction = 1;            // back for the dummy on the right
        s.in[0].special = "S2";
        int blocked = 0;
        for (int i = 0; i < 120; i++) {
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) if (e.attacker == 0 && e.blocked) blocked++;
        }
        assertEquals("each strike meets the guard", 3, blocked);
        assertEquals("guard meter once per move",
            CombatRules.superGainOnGuard(GeneratedCharacters.get("p03").attack("S2").strength), s.f(1).state.superMeter);
    }

    @Test public void aSeveralStrikeMoveGivesTheMeterOfOneMove() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get("p03"), BASE, 500f, 600f);
        s.in[0].special = "S2";
        int hits = 0;
        for (int i = 0; i < 120; i++) {
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) if (e.attacker == 0 && !e.blocked) hits++;
        }
        assertEquals(3, hits);
        assertEquals(CombatRules.superGainOnHit(GeneratedCharacters.get("p03").attack("S2").strength),
            s.f(0).state.superMeter);
    }

    // ------------------------------------------------------------ caixas que seguem o desenho

    @Test public void duringAMoveTheBodyAndTheStrikeFollowTheDrawing() throws Exception {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get("p14"), BASE, 400f, 900f);
        CombatFighter f = s.f(0);
        assertNull("standing still: the stance box", CombatEngine.corpoNoDesenho(f));
        startMove(s.engine, 0, "H");
        java.util.Set<String> golpes = new java.util.HashSet<>();
        float maisLonge = 0f;
        for (int i = 0; i < f.attack.totalFrames - 1; i++) {
            float[] corpo = CombatEngine.corpoNoDesenho(f);
            assertNotNull("the body of the drawing at frame " + f.attackFrame, corpo);
            assertEquals("head, trunk and legs", 0, corpo.length % 4);
            float[] g = CombatEngine.golpeNoDesenho(f, f.attackFrame);
            if (g != null && f.attack.isActive(f.attackFrame)) {
                golpes.add(java.util.Arrays.toString(g));
                maisLonge = Math.max(maisLonge, g[1]);
            }
            s.step();
        }
        assertFalse("the kick has a box from the drawing while active", golpes.isEmpty());
        assertTrue("and it reaches the foot, past the body: " + maisLonge, maisLonge > f.body().halfWidth + 100f);
    }

    /** Até onde vai à frente o desenho do corpo parado (a guarda, a base aberta). */
    private static float frenteParado(CharacterDefinition c) {
        float[] corpo = c.animations.get(SpriteStates.IDLE).postura;
        float frente = c.fighter.body.halfWidth;
        for (int k = 1; corpo != null && k < corpo.length; k += 4) frente = Math.max(frente, corpo[k]);
        return frente;
    }

    @Test public void standingTheBodyIsTheDrawingNotANarrowBox() {
        CharacterDefinition p15 = GeneratedCharacters.get("p15");
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(p15, BASE, 400f, 900f);
        float[] corpo = CombatEngine.corpoAtual(s.f(0));
        assertNotNull("standing: the stance of the drawing", corpo);
        float largura = 0f;
        for (int k = 0; k + 3 < corpo.length; k += 4) largura = Math.max(largura, corpo[k + 1] - corpo[k]);
        assertTrue("the open stance is wider than the old box: " + largura, largura > p15.fighter.body.halfWidth * 2 + 20f);
    }

    @Test public void theStretchedLegCanBeHit() throws Exception {
        CharacterDefinition p14 = GeneratedCharacters.get("p14");
        float reach = BASE.attack("L").reach;
        // Longe demais para o soco encostar no corpo dele, perto o bastante para a perna esticada.
        float gap = frenteParado(p14) + reach + 30f;
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(p14, BASE, 400f, 400f + gap);
        CombatFighter kicker = s.f(0);
        startMove(s.engine, 0, "H");
        // Até a perna estar esticada além do soco.
        for (int i = 0; i < 60; i++) {
            float[] g = CombatEngine.golpeNoDesenho(kicker, kicker.attackFrame);
            float[] corpo = CombatEngine.corpoNoDesenho(kicker);
            float frente = 0f;
            for (int k = 1; corpo != null && k < corpo.length; k += 4) frente = Math.max(frente, corpo[k]);
            if (frente > gap - reach + 20f) break;
            s.step();
        }
        int hits = 0;
        startMove(s.engine, 1, "L");
        for (int i = 0; i < 30; i++) {
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) if (e.attacker == 1) hits++;
        }
        assertTrue("the jab caught the stretched leg", hits > 0);
    }

    @Test public void theJabMissesTheBodyFromThere() throws Exception {
        CharacterDefinition p14 = GeneratedCharacters.get("p14");
        float gap = frenteParado(p14) + BASE.attack("L").reach + 30f;
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(p14, BASE, 400f, 400f + gap);
        int hits = 0;
        startMove(s.engine, 1, "L");
        for (int i = 0; i < 30; i++) {
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) if (e.attacker == 1) hits++;
        }
        assertEquals("standing, nothing to hit at that distance", 0, hits);
    }

    @Test public void anAdvancingSuperStopsAtTheOpponentInsteadOfShovingIt() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get("p10"), BASE, 500f, 640f);
        s.f(0).state.superMeter = CombatConfig.MAX_METER;
        s.in[0].superAttack = true;
        float start = s.f(1).x;
        int hits = 0, strikes = GeneratedCharacters.get("p10").attack("SUPER").maxHits;
        for (int i = 0; i < 200 && hits < strikes - 1; i++) {
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) if (e.attacker == 0 && !e.blocked) hits++;
        }
        assertEquals("every strike but the last landed", strikes - 1, hits);
        assertTrue("held in front, not shoved across the stage: moved " + (s.f(1).x - start), s.f(1).x - start < 50f);
    }

    // ------------------------------------------------------------ revisão: casos de borda

    private static void startMove(CombatEngine e, int i, String id) throws Exception {
        java.lang.reflect.Method start = CombatEngine.class.getDeclaredMethod("startAttack",
            CombatFighter.class, AttackDefinition.class, String.class, int.class);
        start.setAccessible(true);
        CombatFighter f = e.fighter(i);
        start.invoke(e, f, f.character().attack(id), null, -1);
    }

    @Test public void theUltraSequenceSparesThePartnerWhoComesInAfterTheKo() {
        FighterState[] p1 = {new FighterState(GeneratedCharacters.get("p02"), "P1"),
            new FighterState(GeneratedCharacters.get("player_two"), "P2")};
        FighterState[] cpu = {new FighterState(BASE, "CPU"), new FighterState(GeneratedCharacters.get("player_two"), "CPU2")};
        CombatEngine engine = new CombatEngine(p1[0], cpu[0], 500f, 610f, new CombatConfig());
        engine.setTeam(0, p1);
        engine.setTeam(1, cpu);
        FighterInput[] in = {new FighterInput(), new FighterInput()};
        cpu[0].life = 50;   // the first Ultra hit knocks the point out
        engine.fighter(0).state.superMeter = CombatConfig.ULTRA_COST;
        in[0].direction = 3; in[0].superAttack = true; in[0].ultra = true;
        CombatFighter caught = engine.fighter(1);
        int partnerLife = cpu[1].life;
        boolean swapped = false;
        for (int i = 0; i < 400; i++) {
            engine.step(in[0], in[1]);
            in[0].clearPresses(); in[1].clearPresses();
            if (i > 2) in[0].direction = 0;
            // The partner comes in while the sequence still plays (the engine allows it on its own).
            if (!swapped && caught.ko() && engine.fighter(0).inUltraSequence()) swapped = engine.replaceFallenPoint(1);
        }
        assertTrue("the partner came in during the sequence", swapped);
        assertEquals("the rest of the sequence does not hit the partner", partnerLife, cpu[1].life);
    }

    @Test public void aStrikingSuperIsAGroundMove() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get("p02"), BASE, 500f, 700f);
        s.f(0).state.superMeter = CombatConfig.MAX_METER;
        s.in[0].jump = true;
        s.steps(10);
        assertFalse(s.f(0).grounded);
        s.in[0].superAttack = true;
        s.step();
        assertNotEquals("no striking Super in the air", "SUPER", s.attackId(0));
        assertEquals(CombatConfig.MAX_METER, s.f(0).state.superMeter);
    }

    @Test public void aTradeOfSeveralStrikeMovesTreatsBothSidesAlike() throws Exception {
        CharacterDefinition p03 = GeneratedCharacters.get("p03");
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(p03, p03, 500f, 650f);
        s.step();
        startMove(s.engine, 0, "S2");
        startMove(s.engine, 1, "S2");
        s.until(() -> !s.engine.events().isEmpty(), 60);
        assertEquals("both strikes landed together", 2, s.engine.events().size());
        assertEquals(s.f(0).status, s.f(1).status);
        assertEquals(s.f(0).stunLeft, s.f(1).stunLeft);
        assertEquals(s.f(0).state.life, s.f(1).state.life);
    }

    @Test public void theJuggleLimitStopsTheRestOfTheStrikes() throws Exception {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get("p03"), BASE, 500f, 620f);
        s.step();
        CombatFighter d = s.f(1);
        d.status = CombatFighter.Status.AIR_HITSTUN; d.grounded = false; d.y = Arena.GROUND_Y - 60f; d.vy = -300f;
        d.stunLeft = d.stunTotal = 60; d.launched = true;
        java.lang.reflect.Field field = CombatEngine.class.getDeclaredField("sessions");
        field.setAccessible(true);
        ComboSession full = new ComboSession(0, 1);
        full.hitCount = 5;
        full.juggleCount = s.engine.config.juggleLimit;
        ((ComboSession[]) field.get(s.engine))[1] = full;
        startMove(s.engine, 0, "S2");
        int hits = 0;
        for (int i = 0; i < 60; i++) {
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) if (e.attacker == 0 && !e.blocked) hits++;
        }
        assertEquals("a full juggle frees the defender from every strike of the move", 0, hits);
    }
}
