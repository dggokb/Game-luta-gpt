package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * O S3 do Kōga (20/11, a ideia do dono: "ele dá o dash curto e voadora, se acertar o adversário emenda
 * os golpes, se errar... errou"): a voadora é o primeiro acerto; acertando, saem os cinco socos e o soco
 * grande (7 acertos, o último derruba); errando ou defendida, ele cai da voadora (o SPECIAL_S3_ERRO) e o
 * golpe acaba logo.
 */
public class InvestidaDaVoadoraTest {
    private static final CharacterDefinition KOGA = GeneratedCharacters.get("p03");
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");

    private static int[] rodar(float gap, boolean defende) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(KOGA, BASE, 600f, 600f + gap);
        s.in[0].special = "S3";
        s.step();
        s.in[0].special = null;
        int acertos = 0, defesas = 0, quadros = 0;
        for (int t = 0; t < 300 && (t < 2 || s.f(0).attacking()); t++) {
            if (defende) s.in[1].direction = 1;   // o rival olha para a esquerda: para trás é →
            s.step();
            if (s.f(0).attacking()) quadros = Math.max(quadros, s.f(0).attackFrame + 1);
            for (CombatEngine.HitEvent e : s.engine.events()) {
                if (e.attacker != 0) continue;
                if (e.blocked) defesas++; else acertos++;
            }
        }
        return new int[] {acertos, defesas, quadros};
    }

    @Test public void acertandoAVoadoraEmendaARajada() {
        int[] r = rodar(250f, false);
        assertEquals("a voadora e os seis socos", 7, r[0]);
        assertEquals(KOGA.attack("S3").totalFrames, r[2], 3);
    }

    @Test public void errandoAcabaNaVoadora() {
        int[] r = rodar(900f, false);
        assertEquals(0, r[0]);
        assertTrue("o golpe acaba logo (" + r[2] + ")", r[2] < KOGA.attack("S3").totalFrames - 30);
    }

    @Test public void defendidaTambemAcaba() {
        int[] r = rodar(250f, true);
        assertEquals("nenhum soco depois da voadora defendida", 1, r[1]);
        assertEquals(0, r[0]);
    }
}
