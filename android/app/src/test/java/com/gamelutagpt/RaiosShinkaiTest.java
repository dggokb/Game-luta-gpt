package com.gamelutagpt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Os raios dos dois SHINKAI: só com os dois ativos, poucos ao mesmo tempo, menos no gráfico LEVE. */
@RunWith(RobolectricTestRunner.class)
public class RaiosShinkaiTest {
    private static int nascidos(RaiosShinkai r, boolean ativos, boolean leve, int quadros) {
        int nascidos = 0, antes = 0;
        for (int i = 0; i < quadros; i++) {
            r.passo(ativos, 0xFF1E9CF0, 0xFFE0303A, 400f, 400f, 800f, 400f, 0f, 1280f, 60f, 565f, leve);
            int agora = r.vivos();
            assertTrue("no máximo " + RaiosShinkai.VAGAS + " de uma vez", agora <= RaiosShinkai.VAGAS);
            if (agora > antes) nascidos += agora - antes;
            antes = agora;
        }
        return nascidos;
    }

    @Test public void soComOsDoisNoShinkai() {
        assertEquals(0, nascidos(new RaiosShinkai(1), false, false, 600));
        int dez = nascidos(new RaiosShinkai(1), true, false, 600);
        assertTrue("uns 15–30 raios em 10 s: " + dez, dez >= 12 && dez <= 35);
    }

    @Test public void noLeveCaemMetade() {
        int normal = nascidos(new RaiosShinkai(2), true, false, 1200);
        int leve = nascidos(new RaiosShinkai(2), true, true, 1200);
        assertTrue(normal + " / " + leve, leve * 2 <= normal + 3);
    }

    @Test public void cadaUmSomeLogo() {
        RaiosShinkai r = new RaiosShinkai(3);
        nascidos(r, true, false, 300);
        nascidos(r, false, false, RaiosShinkai.VIDA);
        assertEquals("desligou: os que estavam somem em " + RaiosShinkai.VIDA + " quadros", 0, r.vivos());
    }
}
