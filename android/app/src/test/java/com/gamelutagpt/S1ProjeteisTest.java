package com.gamelutagpt;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * S1 de cada personagem no jogo: o desenho do projétil, a sombra baixa do p06, o chicote do
 * p08 que volta para a mão e o S1 corpo a corpo do p07 (tapa) e do p12 (investida).
 */
public class S1ProjeteisTest {
    private static final CharacterDefinition RIVAL = GeneratedCharacters.get("player_base");

    @Test public void everyS1HasItsDrawing() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("player_base", "ONDA");
        expected.put("p02", "PALMA");
        expected.put("p03", "ONDA_CURTA");
        expected.put("p04", "ESTOURO");
        expected.put("p05", "KUNAI");
        expected.put("p06", "SOMBRA");
        expected.put("p08", "CHICOTE");
        expected.put("p09", "MACHADO");
        expected.put("p10", "MACHADINHO");
        expected.put("p11", "BISTURI");
        for (Map.Entry<String, String> e : expected.entrySet()) {
            CharacterDefinition c = GeneratedCharacters.get(e.getKey());
            assertEquals(e.getKey(), e.getValue(), c.fighter.energyVisual);
            assertFalse(e.getKey() + " throws its S1", c.attack("S").melee());
        }
        for (String id : new String[]{"p07", "p12"}) {
            CharacterDefinition c = GeneratedCharacters.get(id);
            assertNull(id + ": melee S1 has no drawing", c.fighter.energyVisual);
            assertTrue(id + ": S1 strikes up close", c.attack("S").melee());
        }
        assertTrue("only the shadow is low", GeneratedCharacters.get("p06").attack("S").low);
    }

    @Test public void theShadowRunsOnTheGroundAndIsBlockedOnlyCrouching() {
        // ← em pé não segura; ↙ (agachado, de costas) segura. O rival olha para a esquerda: ↘ = 2.
        assertFalse("standing guard lets it through", shadowBlocked(1));
        assertTrue("crouching guard stops it", shadowBlocked(2));
    }

    /** O p06 lança a sombra no rival que segura {@code direction}; diz se a defesa parou. */
    private static boolean shadowBlocked(int direction) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get("p06"), RIVAL, 400f, 750f);
        s.in[0].special = "M";
        s.until(() -> !s.engine.energyProjectiles.isEmpty(), 40);
        Projectile p = s.engine.energyProjectiles.get(0);
        assertEquals("SOMBRA", p.visual);
        assertTrue("hugs the ground: " + p.y, Arena.GROUND_Y - p.y < 20f);
        s.in[1].direction = direction;
        for (int i = 0; i < 90; i++) {
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) {
                if (e.attacker == 0 && e.projectile) return e.blocked;
            }
        }
        fail("the shadow never reached the rival");
        return false;
    }

    @Test public void theWhipStretchesAndComesBackToTheHand() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get("p08"), RIVAL, 300f, 1500f);
        s.in[0].special = "M";
        s.until(() -> !s.engine.energyProjectiles.isEmpty(), 40);
        Projectile p = s.engine.energyProjectiles.get(0);
        assertEquals("CHICOTE", p.visual);
        s.until(() -> p.returning, 120);
        float tip = Math.abs(p.x - p.startX);
        assertEquals("stretches to its reach", p.range, tip, 1f);
        s.until(() -> s.engine.energyProjectiles.isEmpty(), 120);
        assertEquals(RIVAL.fighter.maxLife, s.f(1).state.life);
    }

    @Test public void theWhipHitsOnceAndRetracts() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get("p08"), RIVAL, 300f, 700f);
        s.in[0].special = "M";
        int hits = 0;
        boolean returned = false;
        for (int i = 0; i < 160; i++) {
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) if (e.attacker == 0 && e.projectile) hits++;
            if (!s.engine.energyProjectiles.isEmpty()) returned |= s.engine.energyProjectiles.get(0).returning;
        }
        assertEquals("one hit", 1, hits);
        assertTrue("goes back after the hit", returned);
        assertTrue("gone at the end", s.engine.energyProjectiles.isEmpty());
    }

    @Test public void theSlapAndTheLungeHitUpCloseWithoutAProjectile() {
        for (String id : new String[]{"p07", "p12"}) {
            CharacterDefinition c = GeneratedCharacters.get(id);
            CombatEngineTest.Sim s = new CombatEngineTest.Sim(c, RIVAL, 500f, 640f);
            s.in[0].special = "M";
            boolean knockdown = false, hit = false;
            for (int i = 0; i < 90; i++) {
                s.step();
                assertTrue(id + ": no projectile", s.engine.energyProjectiles.isEmpty());
                for (CombatEngine.HitEvent e : s.engine.events()) hit |= e.attacker == 0 && "S".equals(e.moveId);
                knockdown |= s.f(1).status == CombatFighter.Status.KNOCKDOWN;
            }
            assertTrue(id + ": the S1 hit", hit);
            assertEquals(id + ": the slap knocks down, the lunge does not", id.equals("p07"), knockdown);
        }
    }

    @Test public void theLungeStillCostsLife() {
        CharacterDefinition c = GeneratedCharacters.get("p12");
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(c, RIVAL, 300f, 1500f);
        s.in[0].special = "M";
        s.steps(40);
        assertTrue("pays with life", s.f(0).state.life < c.fighter.maxLife);
        assertTrue("advances", s.f(0).x > 360f);
    }

    @Test public void theCpuUsesAMeleeS1OnlyUpClose() {
        CharacterDefinition slap = GeneratedCharacters.get("p07");
        assertTrue(OpponentAi.energyInRange(slap, 150f));
        assertFalse(OpponentAi.energyInRange(slap, 400f));
        CharacterDefinition wave = GeneratedCharacters.get("player_base");
        assertTrue(OpponentAi.energyInRange(wave, 400f));
        assertFalse(OpponentAi.energyInRange(wave, 100f));
        // O estouro do p04 é curtinho: a CPU não solta de longe.
        assertFalse(OpponentAi.energyInRange(GeneratedCharacters.get("p04"), 600f));
    }
}
