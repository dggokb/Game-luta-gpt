package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.Arrays;
import org.junit.Test;

/**
 * p16 (frente H4): o M e o H cancelam em especial e Super, como no resto do elenco. O pacote dela
 * nasceu sem esses cancelamentos e nenhum combo dela fechava em especial nem em Super.
 */
public class CancelamentosP16Test {
    private static BateriaCombos.Exec rota(String... passos) {
        BateriaCombos.Cenario cn = new BateriaCombos.Cenario();
        cn.barras = 1;
        return BateriaCombos.executar("p16", "p15", Arrays.asList(passos), cn);
    }

    @Test public void mediumAndHeavyCancelLikeTheRestOfTheRoster() {
        CharacterDefinition c = GeneratedCharacters.get("p16");
        for (String s : new String[] {"S", "S2", "S3", "S4"}) assertTrue("M → " + s, c.attack("M").cancelsInto(s));
        for (String s : new String[] {"S", "SUPER", "S2", "S3", "S4"}) assertTrue("H → " + s, c.attack("H").cancelsInto(s));
    }

    @Test public void theComboClosesWithTheSuper() {
        BateriaCombos.Exec semSuper = rota("M", "H");
        BateriaCombos.Exec comSuper = rota("M", "H", "SUPER");
        assertTrue(comSuper.valida);
        assertTrue("M H SUPER rende mais que M H: " + comSuper.dano + " × " + semSuper.dano, comSuper.dano > semSuper.dano + 1000);
    }
}
