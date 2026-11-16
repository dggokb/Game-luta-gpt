package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.Arrays;
import org.junit.Test;

/**
 * A voadora da Natisuko em duas partes (16/11): ↓↙← é só a voadora (anda mais); apertando um golpe
 * logo depois sai o chute girando (a continuação, CancelSystem.FOLLOW_UP), cada parte no tempo do
 * vídeo. O chute joga em arco e, perto da borda, quica na parede para o combo continuar.
 */
public class ComboNatisukoTest {
    private static final String NATISUKO = "p15", BONNIE = "p16";

    private static BateriaCombos.Exec combo(String rota, float gap, boolean canto) {
        BateriaCombos.Cenario cn = new BateriaCombos.Cenario();
        cn.barras = 3;
        cn.gap = gap;
        cn.canto = canto;
        return BateriaCombos.executar(NATISUKO, BONNIE, Arrays.asList(rota.split(" ")), cn);
    }

    @Test public void voadoraEChuteGirandoComBotao() {
        CharacterDefinition n = GeneratedCharacters.get(NATISUKO);
        AttackDefinition voadora = n.attack("S4"), chute = n.attack(CancelSystem.FOLLOW_UP);
        assertEquals("a voadora é um golpe só", 1, voadora.maxHits);
        assertTrue("o botão solta o chute girando", voadora.cancelsInto(CancelSystem.FOLLOW_UP));
        assertEquals(AttackDefinition.Launch.ARC, chute.launch);
        assertTrue("quica na parede", chute.mechanics.arcWall);
        // De longe (~300 px): a voadora cruza e o chute emenda.
        BateriaCombos.Exec x = combo("S4 SG", 300f, false);
        assertTrue(x.valida);
        assertEquals(2, x.acertos);
        // Sem o botão, só a voadora.
        assertEquals(3, combo("L M S4", 100f, false).acertos);
        assertEquals(4, combo("L M S4 SG", 100f, false).acertos);
    }

    @Test public void noCantoOChuteQuicaNaParedeEOComboContinua() {
        BateriaCombos.Exec x = combo("L M S4 SG W20 2H SJ jL jM jH S4", 100f, true);
        assertTrue(x.valida);
        assertEquals(9, x.acertos);
        assertEquals(1, x.parede);
    }

    @Test public void oBotaoApertadoCedoFicaGuardado() {
        // Um toque só, logo no começo da voadora: o chute sai quando a janela abre.
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get(NATISUKO), GeneratedCharacters.get("player_base"), 500f, 700f);
        CombatFighter me = s.f(0);
        me.commandButton = "M";
        me.buffer.push(InputBuffer.Button.SPECIAL, "S4", false);
        s.steps(5);
        assertEquals("S4", s.attackId(0));
        s.in[0].light = true;
        s.step();
        s.until(() -> CancelSystem.FOLLOW_UP.equals(s.attackId(0)), 60);
    }

    @Test public void doNeutroOBotaoNaoSoltaOChute() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get(NATISUKO), GeneratedCharacters.get("player_base"), 500f, 1000f);
        s.in[0].light = true;
        s.step();
        assertEquals("L", s.attackId(0));
    }
}
