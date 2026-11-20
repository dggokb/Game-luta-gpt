package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Correndo para cima do rival, a corrida acaba quando encosta (20/11, o dono: "caindo do super ainda
 * desliza no dash"): o lutador não fica com o desenho de correr patinando no lugar, segurado pelo
 * empurrão das caixas. Vale para todos.
 */
public class CorridaEncostaTest {
    @Test public void aCorridaAcabaAoEncostar() {
        for (String id : new String[] {"p03", "p02", "player_base"}) {
            CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get(id), GeneratedCharacters.get("player_base"), 600f, 900f);
            boolean correu = false, encostou = false;
            for (int t = 0; t < 120; t++) {
                s.in[0].direction = 1;
                s.in[0].dash = t == 0;
                s.step();
                CombatFighter f = s.f(0), o = s.f(1);
                correu |= f.forwardDashing;
                float perto = f.body().pushHalfWidth + o.body().pushHalfWidth + 2f;
                if (Math.abs(o.x - f.x) <= perto) encostou = true;
                if (encostou && t > 5) {
                    s.step();
                    assertFalse(id + ": encostado, já não corre", s.f(0).forwardDashing);
                    break;
                }
            }
            assertTrue(id + ": correu", correu);
            assertTrue(id + ": encostou", encostou);
        }
    }
}
