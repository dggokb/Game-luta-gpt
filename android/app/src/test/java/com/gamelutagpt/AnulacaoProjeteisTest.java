package com.gamelutagpt;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Anulação de projéteis: projéteis de lados opostos que se tocam somem antes de chegar em
 * alguém; o Super atravessa o especial e segue; o chicote da p08 não entra no choque.
 */
public class AnulacaoProjeteisTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");

    /** Os dois a uma distância em que as ondas se encontram no meio. */
    private static CombatEngineTest.Sim apart() {
        return new CombatEngineTest.Sim(BASE, BASE, 400f, 1300f);
    }

    /** Passos até o primeiro choque; falha se não houver em {@code limit} quadros. */
    private static CombatEngine.ProjectileClash untilClash(CombatEngineTest.Sim s, int limit) {
        for (int i = 0; i < limit; i++) {
            s.step();
            assertTrue("nobody is hit before the clash", s.engine.events().isEmpty());
            if (!s.engine.clashes().isEmpty()) {
                assertTrue(s.engine.cues().contains("PROJECTILE_CLASH"));
                return s.engine.clashes().get(0);
            }
        }
        fail("the projectiles never clashed");
        return null;
    }

    @Test public void twoWavesCancelEachOtherInTheMiddle() {
        CombatEngineTest.Sim s = apart();
        s.in[0].special = "M";
        s.in[1].special = "M";
        s.until(() -> s.engine.energyProjectiles.size() == 2, 40);
        CombatEngine.ProjectileClash clash = untilClash(s, 120);
        assertTrue("between the fighters: " + clash.x, clash.x > s.f(0).x && clash.x < s.f(1).x);
        assertFalse("not a Super clash", clash.big);
        assertEquals("side 0 throws to the right", 1, clash.firstDirection);
        assertTrue("both are gone", s.engine.energyProjectiles.isEmpty());
        s.steps(90);
        assertEquals(BASE.fighter.maxLife, s.f(0).state.life);
        assertEquals(BASE.fighter.maxLife, s.f(1).state.life);
    }

    @Test public void aCancelledWaveFreesTheOneAtATimeRule() {
        CombatEngineTest.Sim s = apart();
        s.in[0].special = "M";
        s.in[1].special = "M";
        s.until(() -> s.engine.energyProjectiles.size() == 2, 40);
        untilClash(s, 120);
        assertFalse(s.engine.energyProjectileAlive(0));
        assertFalse(s.engine.energyProjectileAlive(1));
    }

    @Test public void theSuperGoesThroughAWaveAndStillHits() {
        CombatEngineTest.Sim s = apart();
        s.in[1].special = "M";
        s.until(() -> !s.engine.energyProjectiles.isEmpty(), 40);
        s.f(0).state.superMeter = CombatConfig.SUPER_COST;
        s.in[0].superAttack = true;
        s.until(() -> !s.engine.superProjectiles.isEmpty(), 120);
        Projectile wave = s.engine.superProjectiles.get(0);
        CombatEngine.ProjectileClash clash = untilClash(s, 120);
        assertTrue("a Super clash", clash.big);
        assertTrue("the rival's wave is gone", s.engine.energyProjectiles.isEmpty());
        assertTrue("the Super flies on", s.engine.superProjectiles.contains(wave));
        assertEquals("with all its hits", 0, wave.hitsDone);
        boolean hit = false;
        for (int i = 0; i < 120 && !hit; i++) {
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) hit |= e.attacker == 0 && e.projectile;
        }
        assertTrue("the Super reaches the rival", hit);
    }

    @Test public void twoSupersOfEqualStrengthCancelEachOther() {
        CombatEngineTest.Sim s = apart();
        s.f(0).state.superMeter = CombatConfig.SUPER_COST;
        s.f(1).state.superMeter = CombatConfig.SUPER_COST;
        s.in[0].superAttack = true;
        s.in[1].superAttack = true;
        s.until(() -> s.engine.superProjectiles.size() == 2, 200);
        CombatEngine.ProjectileClash clash = untilClash(s, 120);
        assertTrue(clash.big);
        assertTrue("both are gone", s.engine.superProjectiles.isEmpty());
    }

    @Test public void theWhipIsAnArmNotEnergy() {
        // A onda passa pelo chicote: nenhum choque, e ela chega na p08.
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get("p08"), BASE, 400f, 1100f);
        s.in[1].special = "M";
        s.until(() -> !s.engine.energyProjectiles.isEmpty(), 40);
        s.in[0].special = "M";
        boolean waveHit = false;
        for (int i = 0; i < 150 && !waveHit; i++) {
            s.step();
            assertTrue("the whip never clashes", s.engine.clashes().isEmpty());
            for (CombatEngine.HitEvent e : s.engine.events()) waveHit |= e.attacker == 1 && e.projectile;
        }
        assertTrue("the wave reaches p08", waveHit);
    }

    @Test public void sweptTestCatchesAFastPairThatCrossedInOneStep() {
        // Antes: A 40 à esquerda de B; depois: A 40 à direita. Raios de 10 nunca se tocariam
        // olhando só o começo ou só o fim do passo.
        assertTrue(CombatRules.projectilesMeet(100f, 180f, 300f, 10f, 140f, 140f, 300f, 10f));
        assertTrue("touching at the end", CombatRules.projectilesMeet(100f, 125f, 300f, 10f, 160f, 140f, 300f, 10f));
        assertFalse("still apart", CombatRules.projectilesMeet(100f, 110f, 300f, 10f, 160f, 150f, 300f, 10f));
        assertFalse("one passes over the other", CombatRules.projectilesMeet(100f, 180f, 200f, 10f, 140f, 140f, 300f, 10f));
    }

    @Test public void theLowShadowPassesUnderAChestHighWave() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get("p06"), BASE, 400f, 1300f);
        s.in[0].special = "M";
        s.in[1].special = "M";
        s.until(() -> s.engine.energyProjectiles.size() == 2, 40);
        for (int i = 0; i < 60; i++) {
            s.step();
            assertTrue("no clash", s.engine.clashes().isEmpty());
        }
    }
}
