package com.gamelutagpt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

/**
 * Combos no ar com especial (15/11, docs/personagens/combos-assinatura.md, "No ar"): depois do
 * lançador e do super pulo, jM ou jH cancelam num especial solto no ar (parado no ar enquanto
 * ele dura). Para cada um dos 16, o combo da tabela tem de emendar e o especial tem de acertar:
 * mais pancadas do que a mesma rota sem ele.
 */
public class ComboNoArTest {
    /** Personagem, rota até o golpe de ar que cancela, e o especial que fecha. */
    static final String[][] COMBOS = {
        {"player_base", "L M 2H SJ jL jM", "S3"},
        {"p02", "L M 2H SJ jL jM jH", "S4"},
        {"p03", "L M 2H SJ jL jM", "S2"},
        {"p04", "L M 2H SJ jL jM", "S2"},
        {"p05", "L M 2H SJ jL jM jH", "S2"},
        {"p06", "L M 2H SJ jL jM", "S2"},
        {"p07", "L M 2H SJ jL jM jH", "S4"},
        {"p08", "L M 2H SJ jL jM jH", "S2"},
        {"p09", "L M 2H SJ jL jM jH", "S"},
        {"p10", "L M 2H SJ jL jM jH", "S"},
        {"p11", "L M 2H SJ jL jM jH", "S"},
        {"p12", "L M 2H SJ jL jM jH", "S2"},
        {"p13", "L M 2H SJ jL jM", "S"},
        {"p14", "L M 2H SJ jL jM jH", "S2"},
        {"p15", "L M 2H SJ jL jM jH", "S3"},
        {"p16", "L M 2H SJ jL jM jH", "S"},
    };

    private static BateriaCombos.Exec executar(String quem, String rota) {
        BateriaCombos.Cenario cn = new BateriaCombos.Cenario();
        cn.barras = 3;
        return BateriaCombos.executar(quem, "p02", Arrays.asList(rota.split(" ")), cn);
    }

    @Test public void cadaUmFechaOCombarNoArComUmEspecial() {
        assertEquals("os 16", GeneratedCharacters.SELECTABLE.length, COMBOS.length);
        for (String[] c : COMBOS) {
            BateriaCombos.Exec sem = executar(c[0], c[1]);
            String rota = c[1] + " " + c[2];
            BateriaCombos.Exec com = executar(c[0], rota);
            assertTrue(c[0] + ": " + c[1] + " emenda", sem.valida);
            assertTrue(c[0] + ": " + rota + " emenda", com.valida);
            assertTrue(c[0] + ": o " + c[2] + " no ar acerta (" + sem.acertos + " → " + com.acertos + ")",
                com.acertos > sem.acertos);
            assertTrue(c[0] + ": mais dano", com.dano > sem.dano);
        }
    }

    @Test public void aCpuFechaComOMesmoEspecial() {
        // O caderno da CPU (ComboBook.FECHO_NO_AR) usa a mesma tabela.
        for (String[] c : COMBOS) {
            String[] fecho = ComboBook.FECHO_NO_AR.get(c[0]);
            String[] passos = c[1].split(" ");
            assertEquals(c[0] + ": golpe de ar que cancela", passos[passos.length - 1], fecho[0]);
            assertEquals(c[0] + ": especial", c[2], fecho[1]);
            java.util.List<String> rota = ComboBook.route(GeneratedCharacters.get(c[0]));
            if (rota.contains(ComboBook.JUMP)) {
                assertEquals(c[0] + ": a rota da CPU fecha no especial no ar: " + rota, c[2], rota.get(rota.size() - 1));
                assertTrue(c[0] + ": sem pulo no ar: " + rota, !rota.contains(ComboBook.AIR_JUMP));
            }
        }
    }

    @Test public void oGolpeDeArCancelaEmEspecial() {
        for (String id : GeneratedCharacters.SELECTABLE) {
            CharacterDefinition c = GeneratedCharacters.get(id);
            for (String ar : new String[] {"jM", "jH"}) {
                AttackDefinition a = c.attack(ar);
                if (c.fighter.energy != null) assertTrue(id + "/" + ar + " → S1", a.cancelsInto("S"));
                for (String s : c.specials.keySet()) assertTrue(id + "/" + ar + " → " + s, a.cancelsInto(s));
                if (c.fighter.superAttack != null) assertTrue(id + "/" + ar + " → Super", a.cancelsInto("SUPER"));
                assertTrue(id + "/" + ar + ": janela de cancelamento no acerto", a.hitWindow != null);
            }
        }
    }
}
