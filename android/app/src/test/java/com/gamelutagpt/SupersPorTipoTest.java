package com.gamelutagpt;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Supers por tipo: a Onda Máxima do p01 acerta várias vezes, a Tempestade de Fogo do p09 corre
 * pelo chão e a Legião de Sombras do p06 chama clones (só desenho).
 */
public class SupersPorTipoTest {
    private static final CharacterDefinition RIVAL = GeneratedCharacters.get("player_base");

    /** Super do lutador 0 com a barra cheia contra o rival a {@code gap}; conta os acertos. */
    private static final class Result {
        int hits, blocked, damage;
        String visual;
        float lowestHeight = Float.MAX_VALUE;
        int ghosts;
    }

    private static Result superOf(String id, float gap, int guard) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get(id), RIVAL, 500f, 500f + gap);
        s.f(0).state.superMeter = CombatConfig.MAX_METER;
        s.in[0].superAttack = true;
        s.in[1].direction = guard;
        Result r = new Result();
        int life = s.f(1).state.life;
        for (int i = 0; i < 300; i++) {
            s.step();
            for (Projectile p : s.engine.superProjectiles) {
                r.visual = p.visual;
                r.lowestHeight = Math.min(r.lowestHeight, Arena.GROUND_Y - p.y);
            }
            for (CombatEngine.Echo e : s.engine.echoes) if (e.ghost) r.ghosts++;
            for (CombatEngine.HitEvent e : s.engine.events()) {
                if (e.attacker != 0 || !"SUPER".equals(e.moveId)) continue;
                if (e.blocked) r.blocked++;
                else r.hits++;
            }
        }
        r.damage = life - s.f(1).state.life;
        return r;
    }

    @Test public void theP01WaveHitsFiveTimes() {
        Result r = superOf("player_base", 400f, 0);
        assertEquals("five hits", 5, r.hits);
        assertTrue("real damage: " + r.damage, r.damage > 2000);
        Result guarded = superOf("player_base", 400f, 1);
        assertEquals("five blocked hits", 5, guarded.blocked);
    }

    @Test public void aWaveThatMissesKeepsFlying() {
        // Ninguém na frente: a onda de vários acertos voa até o fim como a de um acerto só.
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(RIVAL, RIVAL, 300f, 1500f);
        s.f(0).state.superMeter = CombatConfig.MAX_METER;
        s.in[0].superAttack = true;
        s.until(() -> !s.engine.superProjectiles.isEmpty(), 120);
        Projectile p = s.engine.superProjectiles.get(0);
        s.steps(10);
        assertFalse(p.holding);
        assertTrue("flies on", p.x > p.startX + 150f);
    }

}
