package com.gamelutagpt;

import static org.junit.Assert.*;
import org.junit.Test;

/** As ondas do "Sob a Mesma Lua": duplas da CPU uma depois da outra, na mesma luta. */
public class OndasTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");

    @Test public void aDuplaCaidaNaoAcabaALutaEnquantoVemOutra() {
        CombatEngine e = new CombatEngine(new FighterState(BASE, "P1"), new FighterState(BASE, "CPU"), 500f, 700f,
            new CombatConfig());
        e.setTeam(0, new FighterState(BASE, "P1"), new FighterState(GeneratedCharacters.get("p02"), "P2"));
        e.setTeam(1, new FighterState(GeneratedCharacters.get("p05"), "C1"), new FighterState(GeneratedCharacters.get("p06"), "C2"));
        BattleFlow flow = new BattleFlow();
        flow.setBattle(true);
        flow.startCountdown();
        FighterInput a = new FighterInput(), b = new FighterInput();
        for (int i = 0; i < BattleFlow.READY_FRAMES + BattleFlow.GO_FRAMES + 5; i++) { e.step(a, b); flow.update(e, true); }
        assertEquals(BattleFlow.Phase.FIGHT, flow.phase());
        int vida = e.fighter(0).state.life = 9000;

        flow.setProximaDupla(true);
        for (FighterState m : e.team(1).members) m.life = 0;
        for (int i = 0; i < 30; i++) { e.step(a, b); flow.update(e, true); }
        assertFalse("vem outra dupla: a luta segue", flow.over());

        FighterState n1 = new FighterState(GeneratedCharacters.get("p07"), "C1");
        FighterState n2 = new FighterState(GeneratedCharacters.get("p08"), "C2");
        e.novaDupla(1, 1300f, n1, n2);
        assertSame(n1, e.fighter(1).state);
        assertEquals(1300f, e.fighter(1).x, 0f);
        assertEquals("olha para o jogador", -1, e.fighter(1).facing);
        assertFalse(e.fighter(1).ko());
        assertEquals("o jogador segue como estava", vida, e.fighter(0).state.life);

        // A última dupla: agora o K.O. dela acaba a luta.
        flow.setProximaDupla(false);
        n1.life = 0;
        n2.life = 0;
        for (int i = 0; i < 5; i++) { e.step(a, b); flow.update(e, true); }
        assertTrue(flow.over());
        assertEquals(0, flow.winner());
    }
}
