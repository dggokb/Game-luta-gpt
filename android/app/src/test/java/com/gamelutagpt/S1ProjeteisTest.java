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

}
