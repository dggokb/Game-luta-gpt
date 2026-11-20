package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * O S2 do Kōga (20/11, vídeo s2 do dono): o Uppercut da casa sai do chão de verdade com o gancho
 * (o voo, como o S2 do Ryūga), acerta subindo e joga o rival em arco; a CPU usa como anti-aéreo
 * ({@link CpuSpecialsTest}).
 */
public class UppercutDaCasaTest {
    private static final CharacterDefinition KOGA = GeneratedCharacters.get("p03");
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");

    @Test public void saiDoChaoAcertaEJogaEmArco() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(KOGA, BASE, 600f, 700f);
        s.in[0].special = "S2";
        s.step();
        s.in[0].special = null;
        assertTrue("saiu o S2", s.f(0).attacking() && "S2".equals(s.f(0).attack.id));
        boolean voou = false, acertou = false;
        for (int t = 0; t < 90; t++) {
            s.step();
            voou |= !s.f(0).grounded;
            for (CombatEngine.HitEvent e : s.engine.events()) acertou |= e.attacker == 0 && !e.blocked;
        }
        assertTrue("o corpo sai do chão", voou);
        assertTrue("o gancho acerta quem está na frente", acertou);
        assertTrue("e volta ao chão", s.f(0).grounded);
    }

    @Test public void eUmGanchoSoComoOVideo() {
        AttackDefinition up = KOGA.attack("S2");
        assertEquals("um acerto (o gancho que sobe)", 1, up.maxHits);
        assertTrue("voa", up.mechanics.flies());
        assertEquals(AttackDefinition.Launch.ARC, up.launch);
    }
}
