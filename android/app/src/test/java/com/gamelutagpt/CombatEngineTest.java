package com.gamelutagpt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Headless tests of the V2 combat engine. Every acceptance criterion of
 * "Motor de Dano e Combos V2" (section 17) has at least one test here; none renders.
 */
public class CombatEngineTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();

    /** Two fighters, scripted inputs, one engine step per call. */
    static final class Sim {
        final CombatEngine engine;
        final FighterInput[] in = {new FighterInput(), new FighterInput()};

        Sim(CharacterDefinition a, CharacterDefinition b, float xa, float xb) {
            this(a, b, xa, xb, new CombatConfig());
        }
        Sim(CharacterDefinition a, CharacterDefinition b, float xa, float xb, CombatConfig config) {
            engine = new CombatEngine(new FighterState(a, "P1"), new FighterState(b, "P2"), xa, xb, config);
        }
        CombatFighter f(int i) { return engine.fighter(i); }
        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
        }
        void steps(int n) { for (int i = 0; i < n; i++) step(); }
        /** Steps until the condition holds; fails after {@code limit} frames. */
        void until(java.util.function.BooleanSupplier condition, int limit) {
            for (int i = 0; i < limit; i++) {
                if (condition.getAsBoolean()) return;
                step();
            }
            assertTrue("condition not reached in " + limit + " frames", condition.getAsBoolean());
        }
        String attackId(int i) { return f(i).attacking() ? f(i).attack.id : ""; }
    }

    private static Sim close() { return new Sim(BASE, NPC, 500f, 600f); }
    private static Sim far() { return new Sim(BASE, NPC, 500f, 1500f); }

    /** Copy of a character with some moves replaced (data-driven test moves). */
    static CharacterDefinition withMoves(CharacterDefinition base, AttackDefinition... attacks) {
        return withMoves(base, new String[0], attacks);
    }

    static CharacterDefinition withMoves(CharacterDefinition base, String[] autoCombo, AttackDefinition... attacks) {
        CharacterDefinition.Fighter f = base.fighter;
        CharacterDefinition.Fighter fighter = new CharacterDefinition.Fighter(f.color, f.maxLife, autoCombo,
            f.energy, f.energyCommand, f.superAttack, f.body, f.inputPriority);
        Map<String, CharacterDefinition.Move> moves = new LinkedHashMap<>(base.moves);
        for (AttackDefinition a : attacks) {
            CharacterDefinition.Move old = base.moves.get(a.id);
            moves.put(a.id, new CharacterDefinition.Move(a.id, old.animation, old.pose, a));
        }
        return new CharacterDefinition(base.id + "_test", base.displayName, base.profile, base.artFacing,
            base.visualStandHeight, base.visualCrouchHeight, fighter, base.animations, moves, base.specialAnimations);
    }

    private static AttackDefinition.Builder normal(String id, int startup, int active, int recovery) {
        return new AttackDefinition.Builder(id, AttackDefinition.Kind.NORMAL).damage(100)
            .frames(startup, active, recovery).stun(12, 8, 0).windows(null, null, null)
            .pushback(0f, 0f).reach(120f, 78f);
    }

    // ------------------------------------------------------------ 1. mashing

    @Test public void mashingDoesNotInterruptTheCurrentAttack() {
        Sim s = far();
        AttackDefinition light = BASE.attack("L");
        s.in[0].light = true;
        s.step();
        assertEquals("L", s.attackId(0));
        for (int i = 1; i < light.totalFrames; i++) {
            s.in[0].medium = true;
            s.in[0].heavy = true;
            s.step();
            assertEquals("Whiffed L is not cancelled on frame " + i, "L", s.attackId(0));
            assertEquals(i, s.f(0).attackFrame);
        }
        s.in[0].medium = true;
        s.step();
        assertTrue("A buffered press starts only when L is over", s.f(0).attacking());
        assertNotEquals("L", s.attackId(0));
    }

    // ------------------------------------------------------------ 2. only neutral or a valid cancel

    @Test public void attackStartsOnlyFromNeutralOrAValidCancel() {
        // H on hit does not list L: L waits for H to end.
        Sim s = close();
        s.in[0].heavy = true;
        s.step();
        s.until(() -> s.f(0).outcome == CombatFighter.Outcome.HIT, 30);
        AttackDefinition heavy = BASE.attack("H");
        while (s.f(0).attacking() && s.f(0).attackFrame < heavy.totalFrames - 1) {
            s.in[0].light = true;
            s.step();
            if (s.f(0).attacking()) assertEquals("H", s.attackId(0));
        }
        // L (on hit) lists M: the cancel happens inside the window, before L ends.
        Sim t = close();
        t.in[0].light = true;
        t.step();
        t.until(() -> t.f(0).outcome == CombatFighter.Outcome.HIT, 30);
        t.in[0].medium = true;
        t.until(() -> t.f(0).hitstop == 0, 30);
        t.step();
        assertEquals("L cancels into M on hit", "M", t.attackId(0));
        assertEquals(0, t.f(0).attackFrame);
        assertEquals(2, t.engine.session(1) == null ? 0 : t.engine.session(1).hitCount + 1);
    }

    // ------------------------------------------------------------ 3. input buffer

    @Test public void earlyPressIsBufferedUntilTheFighterIsFree() {
        Sim s = far();
        s.in[0].light = true;
        s.step();
        AttackDefinition light = BASE.attack("L");
        s.steps(light.totalFrames - 1 - 3);
        s.in[0].medium = true;          // 3 frames before L ends (buffer is 4)
        s.steps(3);
        assertEquals("L", s.attackId(0));
        s.step();
        assertEquals("Buffered M starts on the first free frame", "M", s.attackId(0));
        assertEquals(0, s.f(0).attackFrame);

        Sim t = far();
        t.in[0].light = true;
        t.step();
        t.steps(light.totalFrames - 1 - 6);
        t.in[0].medium = true;          // too early: it expires
        t.steps(6);
        t.step();
        assertFalse("Expired press does nothing", t.f(0).attacking());
    }

    @Test public void pressDuringHitstopDoesNotExpireAndCancelsRightAfter() {
        Sim s = close();
        s.in[0].light = true;
        s.step();
        s.until(() -> s.f(0).outcome == CombatFighter.Outcome.HIT, 30);
        int contactFrame = s.f(0).attackFrame;
        assertTrue("Hitstop starts on contact", s.f(0).hitstop > 0 && s.f(1).hitstop > 0);
        s.in[0].medium = true;
        int frozen = s.f(0).hitstop;
        for (int i = 0; i < frozen; i++) {
            assertEquals("Attack frame frozen during hitstop", contactFrame, s.f(0).attackFrame);
            s.step();
        }
        assertEquals(1, s.f(0).buffer.size());
        s.step();
        assertEquals("M", s.attackId(0));
    }

    // ------------------------------------------------------------ 4. invalid routes

    @Test public void invalidRouteIsIgnoredAndExpiresWithoutBreakingTheAttack() {
        Sim s = close();
        s.in[0].light = true;
        s.step();
        s.until(() -> s.f(0).outcome == CombatFighter.Outcome.HIT, 30);
        s.until(() -> s.f(0).hitstop == 0, 30);
        s.in[0].heavy = true;           // L does not cancel into H
        int before = s.f(0).attackFrame;
        s.step();
        assertEquals("L", s.attackId(0));
        assertEquals(before + 1, s.f(0).attackFrame);
        s.steps(CombatConfig.FPS / 10);
        assertEquals("Invalid press expired in the buffer", 0, s.f(0).buffer.size());
    }

    // ------------------------------------------------------------ 5. defender cannot act in stun

    @Test public void defenderCannotActDuringHitstunOrBlockstun() {
        for (boolean guard : new boolean[] {false, true}) {
            Sim s = close();
            if (guard) s.in[1].direction = 1;   // monster faces left: right is back
            s.in[0].medium = true;
            s.step();
            s.until(() -> s.f(1).status != CombatFighter.Status.NEUTRAL, 30);
            CombatFighter.Status stun = guard ? CombatFighter.Status.BLOCKSTUN : CombatFighter.Status.HITSTUN;
            assertEquals(stun, s.f(1).status);
            s.in[1].direction = 0;
            int stunFrames = 0;
            while (s.f(1).status == stun) {
                stunFrames = s.f(1).stunElapsed;
                s.in[1].light = true;
                s.step();
                if (s.f(1).status == stun) assertFalse("No attack during " + stun, s.f(1).attacking());
            }
            assertEquals(guard ? BASE.attack("M").blockstunFrames : BASE.attack("M").hitstunFrames, stunFrames);
            assertTrue("Acts on the first free frame", s.f(1).attacking());
        }
    }

    // ------------------------------------------------------------ 6/7. real combo sessions

    @Test public void comboEndsWhenTheDefenderRecoversAndCountsRealSessions() {
        Sim s = close();
        s.in[0].light = true;
        s.step();
        s.until(() -> s.engine.session(1) != null, 30);
        ComboSession first = s.engine.session(1);
        assertEquals(1, first.hitCount);
        s.until(() -> s.engine.session(1) == null, 60);
        assertEquals(CombatFighter.Status.NEUTRAL, s.f(1).status);
        assertSame(first, s.engine.lastSession(1));
        assertFalse(first.active);
        // A hit after the defender recovered starts a new combo, even right away.
        s.until(() -> s.f(0).canAct(), 30);
        s.in[0].light = true;
        s.step();
        s.until(() -> s.engine.session(1) != null, 30);
        assertNotSame(first, s.engine.session(1));
        assertEquals(1, s.engine.session(1).hitCount);
    }

    @Test public void linkFormulaMatchesTheSimulation() {
        // Calibrated by data only: adv = hitstun - (active - 1 + recovery); links when adv > startup.
        for (int hitstun = 8; hitstun <= 14; hitstun++) {
            AttackDefinition starter = normal("L", 2, 2, 4).stun(hitstun, 8, 0).build();
            AttackDefinition follow = normal("M", 4, 2, 6).build();
            CharacterDefinition c = withMoves(BASE, starter, follow);
            Sim s = new Sim(c, NPC, 500f, 600f);
            s.in[0].light = true;
            s.step();
            // Mash M: L has no cancel route, so M starts on the first free frame (tightest link).
            for (int i = 0; i < 40 && !s.attackId(0).equals("M"); i++) {
                s.in[0].medium = true;
                s.step();
            }
            assertEquals("M", s.attackId(0));
            s.until(() -> s.f(0).outcome == CombatFighter.Outcome.HIT || !s.f(0).attacking(), 20);
            int advantage = starter.advantageOnHit();
            ComboSession combo = s.engine.session(1) != null ? s.engine.session(1) : s.engine.lastSession(1);
            boolean comboed = combo.hitCount == 2;
            assertEquals("hitstun " + hitstun + " adv " + advantage, AttackDefinition.links(advantage, follow), comboed);
        }
    }

    // ------------------------------------------------------------ 8. scaling and decay

    @Test public void damageScalingStepsDownToFloorsWithIntegerRounding() {
        CombatConfig config = new CombatConfig();
        AttackDefinition light = BASE.attack("L");
        assertEquals(1000, ComboSession.DamageScaling.scale(light, 0, 1000, 0, config));
        assertEquals(950, ComboSession.DamageScaling.scale(light, 1, 1000, 0, config));
        assertEquals(900, ComboSession.DamageScaling.scale(light, 2, 1000, 0, config));
        assertEquals("Floor", config.damageFloorPermille, ComboSession.DamageScaling.scale(light, 40, 1000, 0, config));
        assertEquals("Repeat penalty 20%", 760, ComboSession.DamageScaling.scale(light, 1, 1000, 1, config));
        assertEquals("Starter proration", 855, ComboSession.DamageScaling.scale(light, 1, 900, 0, config));
        assertEquals(config.specialFloorPermille, ComboSession.DamageScaling.scale(BASE.attack("S"), 40, 1000, 0, config));
        assertEquals(config.superFloorPermille, ComboSession.DamageScaling.scale(BASE.attack("SUPER"), 40, 1000, 0, config));
        assertEquals("Rounded down", 284, ComboSession.DamageScaling.apply(300, 949));
        assertEquals("At least 1", 1, ComboSession.DamageScaling.apply(1, 200));

        ComboSession combo = new ComboSession(0, 1);
        int total = 0;
        for (int i = 0; i < 6; i++) total += combo.registerHit(BASE.attack("M"), 500, config);
        assertEquals(total, combo.comboDamage);
        assertTrue("Scaled combo deals less than raw damage", total < 6 * 500);
    }

    @Test public void hitstunDecaysAsTheComboGrows() {
        CombatConfig config = new CombatConfig();
        ComboSession combo = new ComboSession(0, 1);
        int base = 15;
        assertEquals(base, combo.decayedHitstun(base, config));
        for (int i = 0; i < 9; i++) combo.registerHit(BASE.attack("M"), 500, config);
        assertTrue(combo.decayedHitstun(base, config) < base);
        combo.comboDuration = 4 * CombatConfig.FPS;
        assertTrue("Long combos lose more hitstun", combo.decayedHitstun(base, config) <= base - 4);
        assertTrue(combo.decayedHitstun(1, config) >= config.minHitstunFrames);
    }

    @Test public void longComboInTheEngineIsScaled() {
        Sim s = close();
        // L > M > H > S chain: every hit after the first is scaled.
        List<CombatEngine.HitEvent> hits = new ArrayList<>();
        s.in[0].light = true;
        for (int i = 0; i < 120; i++) {
            s.in[0].direction = 0;
            if (s.attackId(0).equals("L")) s.in[0].medium = true;
            if (s.attackId(0).equals("M")) s.in[0].heavy = true;
            s.step();
            hits.addAll(s.engine.events());
        }
        assertTrue("Chain lands at least 3 hits: " + hits.size(), hits.size() >= 3);
        assertEquals(300, hits.get(0).damage);
        assertTrue("Second hit scaled", hits.get(1).damage < 500);
        ComboSession combo = s.engine.lastSession(1) != null ? s.engine.lastSession(1) : s.engine.session(1);
        assertEquals(hits.size(), combo.hitCount);
    }

    // ------------------------------------------------------------ 9. maxHits

    @Test public void overlappingHitboxHitsOnceAndMultiHitRespectsMaxHits() {
        Sim s = close();
        s.in[0].medium = true;           // M has 6 active frames over the target
        s.step();
        int hits = 0;
        for (int i = 0; i < 40; i++) {
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) if (e.attacker == 0) hits++;
        }
        assertEquals(1, hits);

        AttackDefinition twoHits = normal("H", 3, 6, 6).stun(30, 10, 0).maxHits(2)
            .hitbox(3, 4, 0f, 140f, 40f, 120f).hitbox(6, 8, 0f, 140f, 40f, 120f).build();
        AttackDefinition capped = normal("M", 3, 6, 6).stun(30, 10, 0).maxHits(1)
            .hitbox(3, 4, 0f, 140f, 40f, 120f).hitbox(6, 8, 0f, 140f, 40f, 120f).build();
        CharacterDefinition c = withMoves(BASE, twoHits, capped);
        for (String button : new String[] {"H", "M"}) {
            Sim t = new Sim(c, NPC, 500f, 600f);
            if (button.equals("H")) t.in[0].heavy = true; else t.in[0].medium = true;
            int count = 0;
            for (int i = 0; i < 40; i++) {
                t.step();
                for (CombatEngine.HitEvent e : t.engine.events()) if (e.attacker == 0) count++;
            }
            assertEquals(button.equals("H") ? 2 : 1, count);
        }
    }

    // ------------------------------------------------------------ 10. no infinite route

    @Test public void everyRouteEndsByDecayJugglePointsOrPushback() {
        for (float startX : new float[] {700f, Arena.RIGHT_BOUND - 110f}) {
            for (int seed = 0; seed < 6; seed++) {
                Random random = new Random(seed);
                Sim s = new Sim(BASE, BASE, startX, startX + 100f);
                int longest = 0;
                for (int frame = 0; frame < 30 * CombatConfig.FPS; frame++) {
                    // Greedy "mash every route" bot: presses something every frame.
                    int roll = random.nextInt(8);
                    s.in[0].direction = roll == 0 ? 3 : roll == 1 ? 7 : 0;
                    s.in[0].light = roll <= 3;
                    s.in[0].medium = roll == 4 || roll == 5;
                    s.in[0].heavy = roll >= 6;
                    s.in[0].auto = random.nextBoolean();
                    s.step();
                    ComboSession combo = s.engine.session(1);
                    if (combo != null) longest = Math.max(longest, combo.hitCount);
                    if (s.f(1).ko()) break;
                }
                assertTrue("Combo too long at x=" + startX + " seed " + seed + ": " + longest, longest <= 12);
            }
        }
    }

    @Test public void lightLinkLoopEndsMidscreenAndInTheCorner() {
        for (float x : new float[] {700f, Arena.RIGHT_BOUND - 100f}) {
            Sim s = new Sim(BASE, BASE, x, x + 70f);
            int longest = 0;
            for (int frame = 0; frame < 600; frame++) {
                s.in[0].light = true;      // the fastest, most advantageous move every frame
                s.step();
                if (s.engine.session(1) != null) longest = Math.max(longest, s.engine.session(1).hitCount);
            }
            assertTrue("L loop must end: " + longest, longest > 1 && longest <= 8);
        }
    }

    @Test public void cornerPushbackMovesTheAttackerInstead() {
        float wall = Arena.RIGHT_BOUND;
        Sim s = new Sim(BASE, BASE, wall - 70f, wall);
        float attackerStart = s.f(0).x;
        s.in[0].heavy = true;
        s.steps(60);
        assertEquals(wall, s.f(1).x, 0.01f);
        assertTrue("Attacker pushed back from the cornered defender", s.f(0).x < attackerStart - 20f);
    }

    @Test public void juggleLimitMakesTheDefenderRecoverInTheAir() {
        CombatConfig config = new CombatConfig();
        config.juggleLimit = 2;            // the launcher already spends 2
        Sim s = new Sim(BASE, NPC, 500f, 600f, config);
        s.in[0].direction = 3;
        s.in[0].heavy = true;              // 2H launcher
        s.step();
        s.until(() -> s.f(1).status == CombatFighter.Status.AIR_HITSTUN, 40);
        assertEquals(2, s.engine.session(1).juggleCount);
        s.in[0].direction = 0;
        s.until(() -> s.f(0).canAct() || s.f(0).attacking() && s.f(0).hitstop == 0, 20);
        // Chase with a super jump and an air hit: it would exceed the limit.
        s.in[0].direction = 7;
        s.step();
        s.in[0].direction = 0;
        boolean reset = false;
        for (int i = 0; i < 80 && !reset; i++) {
            s.in[0].light = true;
            s.step();
            reset = s.f(1).status == CombatFighter.Status.NEUTRAL && !s.f(1).grounded;
        }
        assertTrue("Defender recovers in the air instead of taking the juggle", reset);
    }

    // ------------------------------------------------------------ 11. priority

    @Test public void simultaneousPressesFollowTheSamePriority() {
        for (int i = 0; i < 3; i++) {
            Sim s = far();
            s.in[0].light = s.in[0].medium = s.in[0].heavy = true;
            s.step();
            assertEquals("HEAVY wins over MEDIUM and LIGHT", "H", s.attackId(0));
        }
        // Equal priority: the most recent press wins (two S presses of different strength).
        CharacterDefinition.Fighter f = BASE.fighter;
        CharacterDefinition.Fighter lightFirst = new CharacterDefinition.Fighter(f.color, f.maxLife, f.autoCombo,
            f.energy, f.energyCommand, f.superAttack, f.body, new AttackDefinition.Strength[] {
                AttackDefinition.Strength.LIGHT, AttackDefinition.Strength.MEDIUM, AttackDefinition.Strength.HEAVY,
                AttackDefinition.Strength.SPECIAL, AttackDefinition.Strength.SUPER});
        CharacterDefinition custom = new CharacterDefinition("light_first", "Light first", BASE.profile, BASE.artFacing,
            BASE.visualStandHeight, BASE.visualCrouchHeight, lightFirst, BASE.animations, BASE.moves, BASE.specialAnimations);
        Sim s = new Sim(custom, NPC, 500f, 1500f);
        s.in[0].light = s.in[0].medium = s.in[0].heavy = true;
        s.step();
        assertEquals("Per-character priority", "L", s.attackId(0));
    }

    @Test public void equalPriorityPicksTheMostRecentPress() {
        InputBuffer buffer = new InputBuffer();
        buffer.push(InputBuffer.Button.LIGHT, null, false);
        buffer.push(InputBuffer.Button.LIGHT, null, true);
        AttackDefinition[] out = new AttackDefinition[1];
        InputBuffer.Entry entry = buffer.select(e -> BASE.attack(e.crouch ? "2L" : "L"), CombatConfig.DEFAULT_PRIORITY, out);
        assertTrue(entry.crouch);
        assertEquals("2L", out[0].id);
        buffer.consume(entry);
        assertEquals("A consumed press leaves the buffer", 1, buffer.size());
    }

    // ------------------------------------------------------------ 12. determinism

    @Test public void sameInputsGiveTheSameFrames() {
        List<String> first = scriptedRun(42);
        List<String> second = scriptedRun(42);
        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) assertEquals("frame " + i, first.get(i), second.get(i));
        assertNotEquals("Different inputs diverge", first, scriptedRun(7));
    }

    private static List<String> scriptedRun(long seed) {
        Random random = new Random(seed);
        Sim s = new Sim(BASE, NPC, 600f, 820f);
        AiController cpu = new AiController(new OpponentAi(new Random(seed)));
        cpu.setEnabled(true);
        List<String> frames = new ArrayList<>();
        for (int frame = 0; frame < 20 * CombatConfig.FPS; frame++) {
            s.in[0].direction = random.nextInt(9);
            s.in[0].light = random.nextInt(6) == 0;
            s.in[0].medium = random.nextInt(9) == 0;
            s.in[0].heavy = random.nextInt(12) == 0;
            s.in[0].superAttack = random.nextInt(60) == 0;
            cpu.fill(s.engine, 1, s.in[1]);
            s.step();
            frames.add(snapshot(s.engine));
        }
        return frames;
    }

    private static String snapshot(CombatEngine e) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 2; i++) {
            CombatFighter f = e.fighter(i);
            b.append(f.x).append(',').append(f.y).append(',').append(f.vy).append(',').append(f.stateName())
                .append(',').append(f.attackFrame).append(',').append(f.stunLeft).append(',').append(f.hitstop)
                .append(',').append(f.state.life).append(',').append(f.state.superMeter).append(';');
        }
        b.append(e.energyProjectiles.size()).append(e.superProjectiles.size());
        return b.toString();
    }

    // ------------------------------------------------------------ 13. hitstop

    @Test public void hitstopFreezesBothFightersOnMeleeImpact() {
        Sim s = close();
        s.in[0].heavy = true;
        s.step();
        s.until(() -> s.f(0).outcome == CombatFighter.Outcome.HIT, 30);
        AttackDefinition heavy = BASE.attack("H");
        assertEquals(heavy.hitstopFrames, s.f(0).hitstop);
        assertEquals(heavy.hitstopFrames, s.f(1).hitstop);
        assertEquals("HITSTOP", s.f(1).stateName());
        int attackFrame = s.f(0).attackFrame, stun = s.f(1).stunLeft;
        float x = s.f(1).x;
        s.steps(heavy.hitstopFrames);
        assertEquals(attackFrame, s.f(0).attackFrame);
        assertEquals(stun, s.f(1).stunLeft);
        assertEquals("No pushback during hitstop", x, s.f(1).x, 0.001f);
        s.step();
        assertEquals(attackFrame + 1, s.f(0).attackFrame);
        assertEquals(stun - 1, s.f(1).stunLeft);
    }

    // ------------------------------------------------------------ 14. data driven

    @Test public void loadRejectsBrokenRoutesAndWindows() {
        try {
            normal("L", 2, 2, 4).windows(AttackDefinition.window(3, 20), null, null).build();
            fail("Window outside the move");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("outside"));
        }
        try {
            normal("L", 4, 2, 4).windows(AttackDefinition.window(1, 6), null, null).build();
            fail("Hit window before the first active frame");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("cancelWindows.hit"));
        }
        try {
            withMoves(BASE, normal("L", 2, 2, 4).cancelInto("NOPE").build());
            fail("Unknown cancel target");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("does not exist"));
        }
        try {
            withMoves(BASE, normal("L", 2, 2, 4).cancelInto("jM").build());
            fail("Ground to air route");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("ground/air"));
        }
        try {
            withMoves(BASE, BASE.fighter.autoCombo, normal("M", 2, 2, 4).build());   // L > M > H needs M > H
            fail("Auto-combo must be a declared route");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("autoCombo"));
        }
    }

    @Test public void newMoveWorksFromDataAlone() {
        AttackDefinition poke = normal("L", 1, 3, 2).damage(777).stun(20, 10, 2)
            .windows(AttackDefinition.window(1, 5), null, null).cancelInto("H").build();
        AttackDefinition heavy = normal("H", 3, 3, 10).damage(55).stun(20, 10, 2).build();
        CharacterDefinition c = withMoves(BASE, poke, heavy);
        Sim s = new Sim(c, NPC, 500f, 600f);
        s.in[0].light = true;
        s.step();
        s.until(() -> !s.engine.events().isEmpty(), 10);
        assertEquals(777, s.engine.events().get(0).damage);
        s.in[0].heavy = true;
        s.until(() -> s.attackId(0).equals("H"), 10);
    }

    @Test public void everyShippedPackPassesValidationAndShowsItsFrameAdvantage() {
        for (String id : new String[] {"player_base", "player_two", "monster_npc"}) {
            CharacterDefinition c = GeneratedCharacters.get(id);
            for (String binding : new String[] {"L", "M", "H", "2L", "2M", "2H", "jL", "jM", "jH"}) {
                AttackDefinition a = c.attack(binding);
                assertEquals(binding, a.id);
                assertEquals(a.hitstunFrames - (a.activeFrames - 1 + a.recoveryFrames), a.advantageOnHit());
            }
            assertTrue(id + " L links into L", AttackDefinition.links(c.attack("L").advantageOnHit(), c.attack("L")));
            assertFalse(id + " L does not link into H", AttackDefinition.links(c.attack("L").advantageOnHit(), c.attack("H")));
        }
    }

    // ------------------------------------------------------------ hit timing and guard

    @Test public void standingMovesHitOnTheirFirstActiveFrameOnceInBothDirections() {
        for (String binding : new String[] {"L", "M", "H"}) {
            for (int side : new int[] {1, -1}) {
                Sim s = new Sim(BASE, NPC, 800f, 800f + 110f * side);
                AttackDefinition attack = BASE.attack(binding);
                int life = s.f(1).state.life;
                if (binding.equals("L")) s.in[0].light = true;
                if (binding.equals("M")) s.in[0].medium = true;
                if (binding.equals("H")) s.in[0].heavy = true;
                s.step();
                while (s.f(0).attackFrame < attack.startupFrames - 1) {
                    s.step();
                    assertEquals(binding + " startup must not hit", life, s.f(1).state.life);
                }
                s.step();
                assertEquals(binding + " first active frame", attack.startupFrames, s.f(0).attackFrame);
                assertEquals(life - attack.damage, s.f(1).state.life);
                s.steps(40);
                assertEquals("Hits once", life - attack.damage, s.f(1).state.life);
            }
        }
    }

    @Test public void whiffedMoveCannotHitAnOpponentArrivingDuringRecovery() {
        for (String binding : new String[] {"L", "M", "H"}) {
            Sim s = far();
            AttackDefinition attack = BASE.attack(binding);
            if (binding.equals("L")) s.in[0].light = true;
            if (binding.equals("M")) s.in[0].medium = true;
            if (binding.equals("H")) s.in[0].heavy = true;
            s.step();
            s.until(() -> s.f(0).attackFrame > attack.lastActiveFrame(), 40);
            s.f(1).x = 600f;
            int life = s.f(1).state.life;
            s.steps(attack.recoveryFrames);
            assertEquals(life, s.f(1).state.life);
        }
    }

    @Test public void guardingBuildsMeterAndAirGuardBlocksGroundAttacks() {
        Sim s = close();
        s.in[1].direction = 1;            // back for the monster
        s.in[0].light = true;
        s.step();
        s.until(() -> !s.engine.events().isEmpty(), 20);
        assertTrue(s.engine.events().get(0).blocked);
        assertEquals("Guard meter", CombatRules.superGainOnGuard(AttackDefinition.Strength.LIGHT), s.f(1).state.superMeter);
        assertEquals("No meter for a blocked attack", 0, s.f(0).state.superMeter);

        Sim air = close();
        air.f(1).grounded = false;
        air.f(1).y = Arena.GROUND_Y - 40f;
        air.in[1].direction = 1;
        air.in[0].heavy = true;
        air.step();
        air.until(() -> !air.engine.events().isEmpty(), 30);
        assertTrue("Air guard blocks", air.engine.events().get(0).blocked);
        assertEquals(CombatFighter.GUARD_AIR, air.f(1).lastGuard);
    }

    @Test public void walkingBackIsSlowerThanWalkingForward() {
        CombatConfig config = new CombatConfig();
        Sim forward = new Sim(BASE, NPC, 500f, 1500f);
        forward.in[0].direction = 1;
        forward.steps(30);
        Sim back = new Sim(BASE, NPC, 1000f, 1500f);
        back.in[0].direction = 5;
        back.steps(30);
        float advanced = forward.f(0).x - 500f;
        float retreated = 1000f - back.f(0).x;
        assertEquals(config.walkSpeed * 30 * CombatConfig.DT, advanced, 0.5f);
        assertEquals(config.walkBackSpeed * 30 * CombatConfig.DT, retreated, 0.5f);
        assertTrue("A retreating fighter is caught by a walking one", retreated < advanced);
    }

    // ------------------------------------------------------------ specials, super, tag

    @Test public void motionCommandTurnsTheButtonIntoTheSpecial() {
        Sim s = far();
        s.in[0].direction = 3;
        s.step();
        s.in[0].direction = 2;
        s.step();
        s.in[0].direction = 1;
        s.step();
        s.in[0].medium = true;
        s.step();
        assertEquals("S", s.attackId(0));
        s.steps(BASE.attack("S").startupFrames + 1);
        assertEquals(1, s.engine.energyProjectiles.size());

        Sim slow = far();
        slow.in[0].direction = 3;
        slow.step();
        slow.in[0].direction = 1;
        slow.steps(new CombatConfig().motionPressFrames + 2);
        slow.in[0].medium = true;
        slow.step();
        assertEquals("Late press is a plain M", "M", slow.attackId(0));
    }

    @Test public void heavyCancelsIntoSuperOnHitAndTheSuperFreezesTheOpponent() {
        Sim s = close();
        s.f(0).state.superMeter = CombatConfig.SUPER_COST;
        s.in[0].heavy = true;
        s.step();
        s.until(() -> s.f(0).outcome == CombatFighter.Outcome.HIT, 30);
        s.in[0].superAttack = true;
        s.until(() -> s.attackId(0).equals("SUPER"), 30);
        assertTrue("Super spent one bar", s.f(0).state.superMeter < CombatConfig.SUPER_COST);
        assertTrue("Super freeze", s.f(1).frozen());
        s.until(() -> !s.engine.superProjectiles.isEmpty() || s.engine.lastSession(1) != null
            && s.engine.lastSession(1).hitCount >= 2, 80);
        s.until(() -> s.engine.session(1) == null, 200);
        assertEquals("H > SUPER is one combo", 2, s.engine.lastSession(1).hitCount);
    }

    @Test public void launcherConfirmCancelsIntoSuperJumpChase() {
        Sim s = close();
        s.in[0].direction = 3;
        s.in[0].heavy = true;
        s.step();
        s.until(() -> s.f(0).outcome == CombatFighter.Outcome.HIT, 40);
        assertTrue(s.f(1).launched);
        s.in[0].direction = 7;
        s.until(() -> !s.f(0).grounded, 30);
        assertTrue("Jump after a launcher is a super jump", s.f(0).superJumping);
    }

    @Test public void lowsMustBeBlockedCrouchingAndAirAttacksStanding() {
        Sim s = close();
        s.in[1].direction = 1;            // stand guard (monster looks left)
        s.in[0].direction = 3;
        s.in[0].light = true;             // 2L is low
        s.step();
        s.until(() -> !s.engine.events().isEmpty(), 20);
        assertFalse("Standing guard loses to a low", s.engine.events().get(0).blocked);

        Sim t = close();
        t.in[1].direction = 2;            // crouch guard
        t.in[0].direction = 3;
        t.in[0].light = true;
        t.step();
        t.until(() -> !t.engine.events().isEmpty(), 20);
        assertTrue(t.engine.events().get(0).blocked);
        assertEquals(CombatFighter.Status.BLOCKSTUN, t.f(1).status);
    }

    @Test public void knockdownIsInvulnerableAndEndsTheCombo() {
        Sim s = close();
        s.in[0].direction = 3;
        s.in[0].medium = true;            // 2M sweep
        s.step();
        s.until(() -> s.f(1).status == CombatFighter.Status.KNOCKDOWN, 40);
        s.step();
        assertNull("Knockdown ends the combo", s.engine.session(1));
        s.in[0].direction = 0;
        int life = s.f(1).state.life;
        for (int i = 0; i < 60; i++) {
            s.in[0].light = true;
            s.step();
        }
        assertEquals("Nothing hits a knocked-down fighter", life, s.f(1).state.life);
        s.until(() -> s.f(1).status == CombatFighter.Status.WAKEUP, 200);
        s.until(() -> s.f(1).status == CombatFighter.Status.NEUTRAL, 60);
    }

    @Test public void cpuOnlyAttacksThroughTheEngineAndWithinReach() {
        Sim s = new Sim(BASE, NPC, 500f, 640f);
        AiController cpu = new AiController(new OpponentAi(new Random(11)));
        cpu.setEnabled(true);
        int started = 0;
        for (int i = 0; i < 900; i++) {
            cpu.fill(s.engine, 1, s.in[1]);
            s.step();
            CombatFighter npc = s.f(1);
            String id = s.attackId(1);
            if (!id.isEmpty() && npc.attackFrame == 0 && npc.move != null) {
                started++;
                float distance = Math.abs(s.f(0).x - npc.x);
                assertTrue(id + " out of reach at " + distance,
                    distance <= CombatRules.maxCenterDistance(npc.move, BASE.fighter.body) + 0.5f);
            }
        }
        assertTrue("CPU attacks once in range", started > 0);
    }

    // ------------------------------------------------------------ ultra (↓ + SUPER)

    /** ↓ + SUPER as the touch pad delivers it. */
    private static void pressUltra(Sim s) {
        s.in[0].direction = 3;
        s.in[0].superAttack = true;
        s.in[0].ultra = true;
    }

    @Test public void ultraNeedsThreeBarsOtherwiseThePressIsASuper() {
        Sim weak = close();
        weak.f(0).state.superMeter = CombatConfig.SUPER_COST;
        pressUltra(weak);
        weak.step();
        weak.until(() -> weak.f(0).attacking(), 6);
        assertEquals("SUPER", weak.attackId(0));

        Sim s = close();
        s.f(0).state.superMeter = CombatConfig.ULTRA_COST;
        pressUltra(s);
        s.step();
        assertEquals(CombatFighter.Status.ULTRA, s.f(0).status);
        assertEquals(CombatFighter.ULTRA_STARTUP, s.f(0).ultraPhase);
        assertEquals(0, s.f(0).state.superMeter);
        assertTrue("The opponent freezes during the activation", s.f(1).frozen());
    }

    @Test public void ultraConnectsUpCloseAndTheCinematicDealsScaledDamage() {
        Sim s = close();
        s.f(0).state.superMeter = CombatConfig.ULTRA_COST;
        pressUltra(s);
        CombatConfig config = s.engine.config;
        s.until(() -> s.engine.inUltraCinematic(0), config.ultraStartupFrames + config.ultraRushFrames + 4);
        assertEquals(CombatFighter.Status.HITSTUN, s.f(1).status);
        assertNotNull(s.engine.session(1));

        int life = s.f(1).state.life;
        int first = s.engine.applyUltraHit(0, 1000);
        assertEquals("First hit of a combo is not scaled", 1000, first);
        int second = s.engine.applyUltraHit(0, 1000);
        assertEquals(first, second);
        assertEquals(life - first - second, s.f(1).state.life);
        assertEquals(2, s.engine.session(1).hitCount);

        s.engine.finishUltra(0);
        assertEquals(CombatFighter.Status.NEUTRAL, s.f(0).status);
        assertEquals(CombatFighter.Status.AIR_HITSTUN, s.f(1).status);
        s.until(() -> s.f(1).status == CombatFighter.Status.KNOCKDOWN, 200);
        assertNull("Landing knocked down ends the combo", s.engine.session(1));
        assertEquals(2000, s.engine.lastSession(1).comboDamage);
    }

    @Test public void ultraAfterAComboIsScaled() {
        Sim s = close();
        s.f(0).state.superMeter = CombatConfig.ULTRA_COST;
        ComboSession session = new ComboSession(0, 1);
        session.hitCount = 4;
        int scale = session.scaleFor(new AttackDefinition.Builder("ULTRA", AttackDefinition.Kind.SUPER)
            .damage(0).frames(1, 1, 1).stun(1, 1, 0).windows(null, null, null).build(), s.engine.config);
        assertEquals("4 hits in: 1000 - 4 x 50", 800, scale);
    }

    @Test public void guardedUltraIsBlockedWithoutCinematic() {
        Sim s = close();
        s.f(0).state.superMeter = CombatConfig.ULTRA_COST;
        s.in[1].direction = 1; // the defender faces left: → is back, high guard
        pressUltra(s);
        CombatConfig config = s.engine.config;
        s.until(() -> s.f(1).status == CombatFighter.Status.BLOCKSTUN,
            config.ultraStartupFrames + config.ultraRushFrames + 4);
        assertFalse(s.engine.inUltraCinematic(0));
        assertEquals(CombatFighter.ULTRA_RECOVERY, s.f(0).ultraPhase);
        s.in[1].direction = 0;
        s.until(() -> s.f(0).status == CombatFighter.Status.NEUTRAL, config.ultraRecoveryFrames + 20);
    }

    @Test public void ultraWhiffsFromFarAndRecovers() {
        Sim s = far();
        s.f(0).state.superMeter = CombatConfig.ULTRA_COST;
        float start = s.f(0).x;
        pressUltra(s);
        s.step();
        s.in[0].direction = 0;
        boolean connected = false;
        for (int i = 0; i < 120 && s.f(0).status == CombatFighter.Status.ULTRA; i++) {
            s.step();
            connected |= s.engine.ultraConnected() >= 0;
        }
        assertFalse(connected);
        assertEquals(CombatFighter.Status.NEUTRAL, s.f(0).status);
        assertTrue("The rush travels forward", s.f(0).x > start + 400f);
        assertEquals("The bars stay spent", 0, s.f(0).state.superMeter);
    }
}
