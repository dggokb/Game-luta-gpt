package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;

/**
 * Super da p15 defendido (frente H4): as cinco pancadas pegam na defesa e ela fica perto do
 * neutro. Antes, o empurrão de 60 px por pancada defendida tirava o rival do alcance do chute
 * final, que saía no vazio, e ela ficava 83 quadros aberta.
 */
public class SuperP15DefendidoTest {
    @Test public void allFiveStrikesReachTheGuardAndSheIsNotLeftWideOpen() {
        BateriaIdentidade.Cena c = new BateriaIdentidade.Cena("p15", "player_base", 1000f, 1200f);
        List<BateriaIdentidade.Toque> r = BateriaIdentidade.roteiro(GeneratedCharacters.get("p15"), "SUPER", c.e.config);
        CombatFighter me = c.f(0), o = c.f(1);
        me.state.addSuperMeter(100000);
        int blocked = 0, hits = 0, freeMe = -1, freeRival = -1;
        boolean started = false;
        for (int fr = 0; fr < 400; fr++) {
            if (fr < r.size()) BateriaIdentidade.toca(r.get(fr), me.facing, c.in[0]);
            BateriaIdentidade.guarda(c, 1, c.in[1]);
            c.passo();
            started |= me.attacking();
            for (CombatEngine.HitEvent h : c.e.events()) if (h.attacker == 0) { if (h.blocked) blocked++; else hits++; }
            if (started && blocked > 0) {
                if (freeMe < 0 && me.canAct()) freeMe = fr;
                if (freeRival < 0 && o.canAct()) freeRival = fr;
                if (freeMe >= 0 && freeRival >= 0) break;
            }
        }
        assertEquals("nada passa da defesa", 0, hits);
        assertEquals("as cinco pancadas pegam na defesa", 5, blocked);
        assertTrue("vantagem na defesa " + (freeRival - freeMe), freeRival - freeMe >= -12);
    }
}
