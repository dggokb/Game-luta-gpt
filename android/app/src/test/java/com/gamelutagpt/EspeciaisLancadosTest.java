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

}
