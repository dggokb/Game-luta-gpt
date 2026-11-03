package com.gamelutagpt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Ultra SHINKAI (pedido do jogador, 03/11): o mesmo botão do Ultra; no SHINKAI e com as cinco barras
 * sai o Ultra SHINKAI, que custa as cinco, bate o dobro do Ultra e encerra a transformação no fim.
 */
public class UltraShinkaiTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");

    /** p02 (Ultra em sequência, sem a página) colado no boneco base. */
    private static CombatEngineTest.Sim luta() {
        CombatConfig config = new CombatConfig();
        config.ultraSequencePage = false;
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get("p02"), BASE, 500f, 610f, config);
        s.f(1).state.life = 1_000_000;   // o Ultra inteiro entra (ninguém cai no meio)
        return s;
    }

    private static void transformar(CombatEngineTest.Sim s) {
        s.f(0).state.shinkai = CombatConfig.SHINKAI_MAX;
        s.in[0].shinkai = true;
        s.step();
        assertTrue(s.engine.cues().contains("SHINKAI"));
        for (int i = 0, n = s.engine.shinkaiAtivacaoQuadros(s.f(0)) + s.engine.config.shinkaiGraca; i < n; i++) s.step();
        assertTrue(s.f(0).state.inShinkai());
    }

    /** Solta o Ultra e soma o dano dele até acabar; devolve o dano, e em [1] se viu o aviso do Ultra SHINKAI. */
    private static int[] ultra(CombatEngineTest.Sim s, int barras) {
        s.f(0).state.superMeter = barras * CombatConfig.METER_PER_BAR;
        s.in[0].direction = 3;
        s.in[0].superAttack = true;
        s.in[0].ultra = true;
        int dano = 0, aviso = 0;
        boolean comecou = false;
        for (int i = 0; i < 3000; i++) {
            s.step();
            s.in[0].direction = 0;
            if (s.engine.cues().contains("ULTRA_SHINKAI")) aviso = 1;
            for (CombatEngine.HitEvent h : s.engine.events()) if (h.attacker == 0 && "ULTRA".equals(h.moveId)) dano += h.damage;
            if (s.f(0).status == CombatFighter.Status.ULTRA) comecou = true;
            else if (comecou) break;
        }
        assertTrue("o Ultra saiu", comecou);
        return new int[] {dano, aviso};
    }

    @Test public void noShinkaiComCincoBarrasSaiOUltraShinkaiQueBateODobroEEncerraATransformacao() {
        CombatEngineTest.Sim normal = luta();
        transformar(normal);
        int[] comum = ultra(normal, 3);
        assertEquals("três barras: o Ultra de sempre", 0, comum[1]);
        assertTrue("o Ultra de sempre não encerra o SHINKAI", normal.f(0).state.inShinkai());

        CombatEngineTest.Sim s = luta();
        transformar(s);
        int[] shinkai = ultra(s, 5);
        assertEquals("aviso do Ultra SHINKAI", 1, shinkai[1]);
        assertEquals("gasta as cinco barras", 0, s.f(0).state.superMeter);
        assertFalse("o SHINKAI acabou com o Ultra SHINKAI", s.f(0).state.inShinkai());
        assertTrue(comum[0] > 0);
        assertEquals("o dobro do Ultra", comum[0] * 2, shinkai[0], comum[0] * 0.02);
    }

    @Test public void foraDoShinkaiCincoBarrasSoltamOUltraDeSempre() {
        CombatEngineTest.Sim s = luta();
        int[] r = ultra(s, 5);
        assertEquals("sem o aviso do Ultra SHINKAI", 0, r[1]);
        assertEquals("gasta três das cinco barras", 2 * CombatConfig.METER_PER_BAR, s.f(0).state.superMeter);
    }

    @Test public void desligadoNaConfigOBotaoSoltaOUltraDeSempre() {
        CombatEngineTest.Sim s = luta();
        s.engine.config.ultraShinkai = false;
        transformar(s);
        int[] r = ultra(s, 5);
        assertEquals(0, r[1]);
        assertTrue(s.f(0).state.inShinkai());
    }
}
