package com.gamelutagpt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Todos os especiais saem também no ar (15/11): S1, S2..S9 e o Super, parados no ar enquanto o
 * golpe dura, e caem quando ele acaba. O agarrão e o que for só de chão ficam no chão; o que é
 * só de ar (o mergulho) segue a física dele. Até dois especiais de chão por pulo.
 */
public class EspeciaisNoArTest {
    private static final float ALTURA = 260f;

    private static final class Luta {
        final CombatEngine engine;
        final FighterInput in = new FighterInput(), npc = new FighterInput();

        Luta(String id) {
            engine = new CombatEngine(new FighterState(GeneratedCharacters.get(id), "P1"),
                new FighterState(GeneratedCharacters.opponentCharacter(), "P2"), 500f, 1500f, new CombatConfig());
            engine.fighter(0).state.superMeter = CombatConfig.MAX_METER;
            step();
        }

        CombatFighter p() { return engine.fighter(0); }

        void step() {
            engine.step(in, npc);
            in.clearPresses();
        }

        /** Põe o lutador parado no alto, como no topo de um pulo. */
        void noAlto() {
            CombatFighter p = p();
            p.y = Arena.GROUND_Y - ALTURA;
            p.grounded = false;
            p.vy = 0f;
            p.vx = 0f;
        }

        /** Aperta o especial (o botão de especial do toque, ou o Super) e devolve o que saiu. */
        String solta(String id) {
            if ("SUPER".equals(id)) in.superAttack = true;
            else in.special = id;
            step();
            for (int i = 0; i < 4 && !p().attacking(); i++) step();
            return p().attacking() ? p().attack.id : "";
        }
    }

    private static List<String> especiais(CharacterDefinition c) {
        List<String> ids = new ArrayList<>(c.specials.keySet());
        if (c.fighter.energy != null) ids.add("S");
        if (c.fighter.superAttack != null) ids.add("SUPER");
        return ids;
    }

    @Test public void todoEspecialDeChaoSaiNoArParadoEnquantoDura() {
        for (String id : GeneratedCharacters.SELECTABLE) {
            CharacterDefinition c = GeneratedCharacters.get(id);
            for (String golpe : especiais(c)) {
                CharacterDefinition.Special s = c.specials.get(golpe);
                boolean soDeAr = s != null ? s.air : "S".equals(golpe) && c.fighter.energyAir;
                if (soDeAr) continue;   // o mergulho: conferido no teste dele
                Luta l = new Luta(id);
                l.noAlto();
                String saiu = l.solta(golpe);
                String nome = id + "/" + golpe;
                if (s != null && !s.saiEm(false)) {
                    assertNotEquals(nome + ": só de chão não sai no ar", golpe, saiu);
                    continue;
                }
                assertEquals(nome + " sai no ar", golpe, saiu);
                CombatFighter p = l.p();
                assertTrue(nome + " fica parado no ar", p.pairando);
                AttackDefinition a = p.attack;
                float y = p.y;
                int fimParado = a.mechanics.flies() ? a.mechanics.flightEnd - 1 : Integer.MAX_VALUE;
                int quadros = 0;
                while (p.attacking() && p.attack == a && p.attackFrame < fimParado - 1 && quadros++ < 600) {
                    l.step();
                    if (p.attacking() && p.attack == a && p.attackFrame < fimParado) {
                        assertEquals(nome + ": parado no ar no quadro " + p.attackFrame, y, p.y, 0.5f);
                    }
                }
                for (int i = 0; i < 240 && !p.grounded; i++) l.step();
                assertTrue(nome + ": cai quando o golpe acaba", p.grounded);
            }
        }
    }

    @Test public void oAgarraoFicaNoChao() {
        int agarroes = 0;
        for (String id : GeneratedCharacters.SELECTABLE) {
            CharacterDefinition c = GeneratedCharacters.get(id);
            for (java.util.Map.Entry<String, CharacterDefinition.Special> e : c.specials.entrySet()) {
                if (!e.getValue().move.attack.mechanics.grab) continue;
                agarroes++;
                Luta l = new Luta(id);
                l.noAlto();
                assertNotEquals(id + "/" + e.getKey() + ": agarrão não sai no ar", e.getKey(), l.solta(e.getKey()));
            }
        }
        assertTrue("há agarrão de comando no elenco", agarroes > 0);
    }

    @Test public void doisEspeciaisDeChaoPorPulo() {
        // O S3 da Yin-lu (não voa: acaba no ar sem pousar).
        Luta l = new Luta("p02");
        CombatFighter p = l.p();
        assertFalse(GeneratedCharacters.get("p02").specials.get("S3").move.attack.mechanics.flies());
        for (int i = 0; i < CombatConfig.ESPECIAIS_NO_AR_POR_PULO; i++) {
            l.noAlto();   // de volta ao alto, sem pousar
            assertEquals("especial " + (i + 1) + " no mesmo pulo", "S3", l.solta("S3"));
            for (int k = 0; k < 300 && p.attacking(); k++) l.step();
            assertFalse("acabou no ar", p.grounded);
        }
        l.noAlto();
        assertEquals("o limite chegou", "", l.solta("S3"));
        for (int i = 0; i < 300 && !p.grounded; i++) l.step();
        assertTrue(p.grounded);
        l.step();
        l.noAlto();
        assertEquals("o pouso zera o limite", "S3", l.solta("S3"));
    }

    @Test public void oS1QueEhSoDeArContinuaSendoOMergulho() {
        // O p14 (S1 só no ar, o chute que mergulha) e o p03 (S4 Mergulho): no ar eles ganham do
        // especial de chão de mesmo comando e descem até o chão.
        for (String[] caso : new String[][]{{"p14", "S"}, {"p03", "S4"}}) {
            Luta l = new Luta(caso[0]);
            l.noAlto();
            assertEquals(caso[0], caso[1], l.solta(caso[1]));
            assertFalse(caso[0] + ": o mergulho não paira", l.p().pairando);
        }
    }
}
