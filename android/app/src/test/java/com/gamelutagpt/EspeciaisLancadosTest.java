package com.gamelutagpt;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Especiais que lançam ou mudam de altura: a shuriken do p05 (sobe em diagonal), a seringa da
 * p11 (cai em diagonal e deixa poça de veneno), o S4 baixo do p07 e o vapor da p10.
 */
public class EspeciaisLancadosTest {
    private static final CharacterDefinition RIVAL = GeneratedCharacters.get("player_base");

    private static Projectile throwSpecial(CombatEngineTest.Sim s, String id) {
        s.in[0].special = id;
        s.until(() -> !s.engine.energyProjectiles.isEmpty(), 40);
        return s.engine.energyProjectiles.get(0);
    }

    @Test public void theShurikenFliesUpDiagonally() {
        CharacterDefinition p05 = GeneratedCharacters.get("p05");
        assertNotNull(p05.specials.get("S2").projectile);
        assertFalse("throws instead of striking", p05.attack("S2").melee());
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(p05, RIVAL, 300f, 1500f);
        Projectile p = throwSpecial(s, "S2");
        assertEquals("SHURIKEN", p.visual);
        float y0 = p.y, x0 = p.x;
        s.steps(10);
        assertTrue("rises", p.y < y0 - 40f);
        assertTrue("goes forward", p.x > x0 + 80f);
    }

    @Test public void theShurikenHitsAJumpingRival() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get("p05"), RIVAL, 300f, 620f);
        s.in[1].direction = 7;
        s.steps(8);
        s.in[1].direction = 0;
        s.in[0].special = "S2";
        boolean hit = false;
        for (int i = 0; i < 60 && !hit; i++) {
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) hit |= e.attacker == 0 && e.projectile && "S2".equals(e.moveId);
        }
        assertTrue("the shuriken catches the jump", hit);
    }

    @Test public void oneProjectileAtATime() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get("p05"), RIVAL, 300f, 1500f);
        throwSpecial(s, "M");
        s.until(() -> !s.f(0).attacking(), 60);
        s.in[0].special = "S2";
        s.steps(3);
        assertFalse("no shuriken while the kunai flies", s.f(0).attacking());
    }

    @Test public void theSyringeFallsAndLeavesAPoisonPuddle() {
        CharacterDefinition p11 = GeneratedCharacters.get("p11");
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(p11, RIVAL, 300f, 1500f);
        Projectile p = throwSpecial(s, "S2");
        assertEquals("SERINGA", p.visual);
        s.until(() -> !s.engine.puddles.isEmpty(), 60);
        assertTrue("gone into the puddle", s.engine.energyProjectiles.isEmpty());
        Puddle pd = s.engine.puddles.get(0);
        assertTrue("lands ahead of him: " + pd.x, pd.x > 450f && pd.x < 800f);
        assertFalse(s.f(1).state.poisoned());
        // O rival pisa na poça: fica envenenado; ela some depois de alguns segundos.
        s.f(1).x = pd.x;
        s.step();
        assertTrue("stepping in poisons", s.f(1).state.poisoned());
        s.steps(200);
        assertTrue("the puddle dries", s.engine.puddles.isEmpty());
    }

    @Test public void aDirectSyringeHitPoisons() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get("p11"), RIVAL, 300f, 560f);
        s.in[0].special = "S2";
        boolean hit = false;
        for (int i = 0; i < 60 && !hit; i++) {
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) hit |= e.attacker == 0 && e.projectile;
        }
        assertTrue("the syringe hits on the way down", hit);
        assertTrue(s.f(1).state.poisoned());
        assertTrue("no puddle after a hit", s.engine.puddles.isEmpty());
    }

    @Test public void theP07SpinKickIsLow() {
        assertTrue(GeneratedCharacters.get("p07").attack("S4").low);
        assertFalse("standing guard lets it through", s4Blocked(1));
        assertTrue("crouching guard stops it", s4Blocked(2));
    }

    /** O p07 dá o S4 no rival que segura {@code direction}; diz se a defesa parou. */
    private static boolean s4Blocked(int direction) {
        // O rival encostado na parede: segurar para trás não o leva para longe.
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get("p07"), RIVAL,
            Arena.RIGHT_BOUND - 200f, Arena.RIGHT_BOUND);
        s.in[1].direction = direction;
        s.in[0].special = "S4";
        for (int i = 0; i < 60; i++) {
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) if (e.attacker == 0) return e.blocked;
        }
        fail("the S4 never reached the rival");
        return false;
    }

    @Test public void theP10PalmShowsSteam() {
        assertEquals("VAPOR", GeneratedCharacters.get("p10").specials.get("S2").effect);
        assertTrue("still a strike", GeneratedCharacters.get("p10").attack("S2").melee());
    }
}
