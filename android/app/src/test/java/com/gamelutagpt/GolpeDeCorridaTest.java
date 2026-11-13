package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.Arrays;
import org.junit.Test;

/**
 * Golpe de corrida (13/11): o H apertado correndo sai da corrida, sem parar, com a função de
 * cada um. O Ryūga dá a Ombrada, que joga na parede mesmo sozinha (correu, acertou, combo); a
 * Yin-lu dá o Carrinho da garça, baixo, dois acertos, derruba e o ↓ + M no caído continua.
 * Quem não tem o golpe (o Kōga, por enquanto) dá o H de sempre.
 */
public class GolpeDeCorridaTest {
    private static final String RYUGA = "player_base", YINLU = "p02";

    /** Começando a 500 px, a rota abre com a corrida (o primeiro acerto é o do segundo passo). */
    private static BateriaCombos.Exec correndo(String quem, String parceiro, String rota, int acertos) {
        BateriaCombos.Cenario cn = new BateriaCombos.Cenario();
        cn.barras = 3;
        cn.gap = 500f;
        cn.primeiroAcertoAte = 2;
        BateriaCombos.Exec x = BateriaCombos.executar(quem, parceiro, Arrays.asList(rota.split(" ")), cn);
        assertTrue(quem + " " + rota + ": o combo emenda", x.valida);
        assertEquals(quem + " " + rota + ": acertos", acertos, x.acertos);
        return x;
    }

    /** O golpe que sai apertando H (com ↓ ou não) depois de correr meio segundo. */
    private static String hCorrendo(String quem, boolean baixo) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get(quem), GeneratedCharacters.get(RYUGA), 600f, 1800f);
        CombatFighter me = s.f(0);
        s.in[0].direction = 1;
        s.in[0].dash = true;
        s.step();
        s.in[0].dash = false;
        for (int i = 0; i < 30; i++) s.step();
        assertTrue("está correndo", me.forwardDashing);
        s.in[0].direction = baixo ? 3 : 1;
        s.in[0].heavy = true;
        s.step();
        s.in[0].heavy = false;
        s.until(me::attacking, 10);
        return me.attack.id;
    }

    @Test public void hCorrendoEOGolpeDeCorrida() {
        assertEquals(CancelSystem.RUN_HEAVY, hCorrendo(RYUGA, false));
        assertEquals(CancelSystem.RUN_HEAVY, hCorrendo(YINLU, false));
        assertEquals("↓ + H correndo continua sendo o 2H", "2H", hCorrendo(RYUGA, true));
        assertFalse("o Kōga ainda não tem", GeneratedCharacters.get("p03").moves.containsKey(CancelSystem.RUN_HEAVY));
        assertNotEquals(CancelSystem.RUN_HEAVY, hCorrendo("p03", false));
    }

    @Test public void ombradaJogaNaParedeMesmoSozinha() {
        AttackDefinition ombrada = GeneratedCharacters.get(RYUGA).attack(CancelSystem.RUN_HEAVY);
        assertEquals(AttackDefinition.Launch.WALL_BOUNCE, ombrada.launch);
        BateriaCombos.Exec x = correndo(RYUGA, YINLU, "C DH W45 C140 2H SJ jL jM J jL jM j2H", 7);
        assertEquals("quicou na parede", 1, x.parede);
        correndo(RYUGA, YINLU, "C DH W15 C SUPER", 6);
    }

    @Test public void carrinhoDerrubaEOChuteNoCaidoContinua() {
        AttackDefinition carrinho = GeneratedCharacters.get(YINLU).attack(CancelSystem.RUN_HEAVY);
        assertTrue("baixo: defende agachado", carrinho.low);
        assertEquals("dois acertos", 2, carrinho.maxHits);
        assertEquals(AttackDefinition.Launch.KNOCKDOWN, carrinho.launch);
        correndo(YINLU, RYUGA, "C DH", 2);
        correndo(YINLU, RYUGA, "C DH C120 2M L M M S4", 11);
    }
}
