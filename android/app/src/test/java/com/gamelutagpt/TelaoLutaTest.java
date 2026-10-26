package com.gamelutagpt;

import static org.junit.Assert.*;
import org.junit.Test;

public class TelaoLutaTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");

    private static void passo(CombatEngineTest.Sim s, TelaoLuta telao) {
        s.step();
        for (int i = 0; i < 2; i++) {
            CombatFighter f = s.f(i);
            telao.gravar(i, f.character(), SpriteStates.IDLE, 0, f.x, f.y, f.facing);
        }
        telao.gravar(2, null, null, 0, 0f, 0f, 1);
        telao.gravar(3, null, null, 0, 0f, 0f, 1);
        telao.passo(s.engine);
    }

    @Test
    public void oKoPassaUmReplayEmCameraLentaDepoisDoGolpe() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(BASE, BASE, 500f, 700f);
        TelaoLuta telao = new TelaoLuta();
        for (int i = 0; i < 120; i++) passo(s, telao);
        assertFalse("sem golpe forte, só ao vivo", telao.replay());
        s.f(1).state.life = 0;
        passo(s, telao);
        for (int i = 1; i < TelaoLuta.DEPOIS; i++) {
            passo(s, telao);
            assertFalse("espera o golpe terminar", telao.replay());
        }
        passo(s, telao);
        assertTrue("começa o replay", telao.replay());
        // Antes e depois do golpe, na metade da velocidade.
        int duracao = 0;
        while (telao.replay() && duracao < 1000) {
            passo(s, telao);
            duracao++;
        }
        int esperado = Math.round((TelaoLuta.ANTES + TelaoLuta.DEPOIS) / TelaoLuta.LENTO);
        assertTrue("dura " + duracao + ", esperado ~" + esperado, Math.abs(duracao - esperado) <= 3);
        for (int i = 0; i < 200; i++) passo(s, telao);
        assertFalse("o mesmo K.O. não repete", telao.replay());
    }

    @Test
    public void lutaNovaEsqueceOReplay() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(BASE, BASE, 500f, 700f);
        TelaoLuta telao = new TelaoLuta();
        for (int i = 0; i < 10; i++) passo(s, telao);
        s.f(1).state.life = 0;
        for (int i = 0; i < TelaoLuta.DEPOIS + 2; i++) passo(s, telao);
        assertTrue(telao.replay());
        telao.reset();
        assertFalse(telao.replay());
    }
}
