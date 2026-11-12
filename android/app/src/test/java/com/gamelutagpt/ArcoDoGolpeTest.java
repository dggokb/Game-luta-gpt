package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Arco (12/11): o shoryuken do Ryūga e o chute da garça da Yin-lu jogam o rival em arco para longe,
 * e ele cai derrubado. O chute da garça bate três vezes; o Chute rodopiante dela anda mais.
 */
public class ArcoDoGolpeTest {
    private static final CharacterDefinition RYUGA = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition YINLU = GeneratedCharacters.get("p02");

    /** {@code quem} solta o especial colado no Ryūga; devolve {acertos, altura máxima do rival, quanto ele andou, caiu derrubado}. */
    private static float[] arco(CharacterDefinition quem, String especial) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(quem, RYUGA, 600f, 700f);
        CombatFighter a = s.f(0), d = s.f(1);
        a.commandButton = "M";
        a.buffer.push(InputBuffer.Button.SPECIAL, especial, false);
        float x0 = d.x, alto = 0f;
        int acertos = 0;
        boolean derrubado = false;
        for (int i = 0; i < 180; i++) {
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) if (e.attacker == 0 && !e.blocked) acertos++;
            alto = Math.max(alto, Arena.GROUND_Y - d.y);
            derrubado |= d.status == CombatFighter.Status.KNOCKDOWN;
            if (derrubado && d.grounded && i > 60) break;
        }
        return new float[] {acertos, alto, Math.abs(d.x - x0), derrubado ? 1f : 0f};
    }

    @Test public void oShoryukenJogaOAdversarioEmArcoLonge() {
        AttackDefinition s2 = RYUGA.attack("S2");
        assertEquals(AttackDefinition.Launch.ARC, s2.launch);
        float[] r = arco(RYUGA, "S2");
        assertTrue("acertou", r[0] >= 1f);
        assertTrue("subiu alto: " + r[1], r[1] > 120f);
        assertTrue("foi longe: " + r[2], r[2] > 250f);
        assertEquals("caiu derrubado", 1f, r[3], 0f);
    }

    @Test public void oChuteDaGarcaBateTresVezesEJogaEmArco() {
        AttackDefinition s2 = YINLU.attack("S2");
        assertEquals(AttackDefinition.Launch.ARC, s2.launch);
        assertTrue("sobe mais (voo de 110 px)", s2.mechanics.flightHeight >= 100f);
        float[] r = arco(YINLU, "S2");
        assertEquals("três pancadas", 3f, r[0], 0f);
        assertTrue("foi longe: " + r[2], r[2] > 220f);
        assertEquals("caiu derrubado", 1f, r[3], 0f);
    }

    @Test public void asTresPancadasPegamDeMeiaDistanciaTambem() {
        // A pancada do meio leva o rival junto para cima: da distância do 2L, as três entram.
        for (float gap : new float[] {100f, 140f, 170f}) {
            CombatEngineTest.Sim s = new CombatEngineTest.Sim(YINLU, RYUGA, 600f, 600f + gap);
            CombatFighter a = s.f(0);
            a.commandButton = "M";
            a.buffer.push(InputBuffer.Button.SPECIAL, "S2", false);
            int acertos = 0;
            for (int i = 0; i < 90; i++) {
                s.step();
                for (CombatEngine.HitEvent e : s.engine.events()) if (e.attacker == 0 && !e.blocked) acertos++;
            }
            if (acertos > 0) assertEquals("a " + gap + " px: se a primeira pega, as três pegam", 3, acertos);
        }
    }

    @Test public void oChuteRodopianteAndaMais() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(YINLU, RYUGA, 300f, 1700f);
        CombatFighter a = s.f(0);
        a.commandButton = "M";
        a.buffer.push(InputBuffer.Button.SPECIAL, "S4", false);
        float x0 = a.x;
        for (int i = 0; i < 90; i++) s.step();
        assertTrue("anda uns 320 px: " + (a.x - x0), a.x - x0 > 280f);
    }
}
