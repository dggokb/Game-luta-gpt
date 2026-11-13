package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * O corpo de cada lutador (11/11): pulo, peso e pulos no ar próprios, em vez de um só para o jogo
 * todo. A Yin-lu (p02) é leve e aérea; o Ryūga é a referência.
 */
public class CorpoDoLutadorTest {
    private static final CharacterDefinition RYUGA = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition YINLU = GeneratedCharacters.get("p02");

    /** Pulo para a frente parado: {altura máxima, quadros no ar, distância}. */
    private static float[] pulo(CharacterDefinition quem) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(quem, RYUGA, 400f, 1600f);
        CombatFighter f = s.f(0);
        float x0 = f.x, alto = 0f;
        int noAr = 0;
        s.in[0].direction = 8;   // ↗
        for (int i = 0; i < 200; i++) {
            s.step();
            if (i == 6) s.in[0].direction = 0;
            if (!f.grounded) {
                noAr++;
                alto = Math.max(alto, Arena.GROUND_Y - f.y);
            } else if (noAr > 0) {
                break;
            }
        }
        return new float[] {alto, noAr, f.x - x0};
    }

    @Test public void aYinLuPulaMaisAltoMaisDemoradoEMaisLonge() {
        float[] ryuga = pulo(RYUGA), yinlu = pulo(YINLU);
        assertEquals("altura 1,15x: " + yinlu[0] + " / " + ryuga[0], 1.15f, yinlu[0] / ryuga[0], 0.04f);
        assertEquals("tempo no ar 1,2x: " + yinlu[1] + " / " + ryuga[1], 1.2f, yinlu[1] / ryuga[1], 0.06f);
        assertEquals("distância 1,1x: " + yinlu[2] + " / " + ryuga[2], 1.1f, yinlu[2] / ryuga[2], 0.06f);
    }

    /** Pula reto, solta, e aperta ↑ de novo no quadro {@code segundo}; devolve as subidas. */
    private static int subidas(CharacterDefinition quem, int segundo, int terceiro) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(quem, RYUGA, 400f, 1600f);
        CombatFighter f = s.f(0);
        int subidas = 0;
        boolean subindo = false;
        for (int i = 0; i < 160; i++) {
            s.in[0].direction = i < 4 || (i >= segundo && i < segundo + 3) || (i >= terceiro && i < terceiro + 3) ? 7 : 0;
            s.step();
            boolean sobe = !f.grounded && f.vy < 0f;
            if (sobe && !subindo) subidas++;
            subindo = sobe;
            if (i > 20 && f.grounded) break;
        }
        return subidas;
    }

    @Test public void soAYinLuTemPuloDuplo() {
        // ↑ de novo já descendo (a subida do pulo dela dura ~30 quadros).
        assertEquals("a Yin-lu pula de novo no ar", 2, subidas(YINLU, 40, 999));
        assertEquals("uma vez por voo", 2, subidas(YINLU, 40, 60));
        assertEquals("o Ryūga não", 1, subidas(RYUGA, 34, 999));
    }

    /** Quadros até tocar o chão de quem apanha o 2H (lançador) do Ryūga de perto. */
    private static int noArApanhando(CharacterDefinition alvo) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(RYUGA, alvo, 600f, 700f);
        CombatFighter d = s.f(1);
        s.in[0].direction = 3;
        s.in[0].heavy = true;
        int noAr = 0;
        for (int i = 0; i < 240; i++) {
            s.step();
            s.in[0].heavy = false;
            if (!d.grounded) noAr++;
            else if (noAr > 0) break;
        }
        return noAr;
    }

    @Test public void aYinLuELeveDemoraMaisACairQuandoApanha() {
        int ryuga = noArApanhando(RYUGA), yinlu = noArApanhando(YINLU);
        assertTrue("o 2H lança os dois", ryuga > 10 && yinlu > 10);
        assertTrue("leve fica mais no ar: " + yinlu + " contra " + ryuga, yinlu > ryuga + 2);
    }
}
