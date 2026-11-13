package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.Arrays;
import org.junit.Test;

/**
 * Combos de assinatura da Yin-lu (12/11, docs/personagens/combos-assinatura.md): longos, leves e no
 * ar. O fluxo da garça (L, L, M, M, H), o Chute rodopiante (S4) que lança, o Joelho da garça (→ + H,
 * dois golpes, o segundo lança), o pulo duplo no meio do combo no ar, o "sobe duas vezes" (o 2H de
 * novo depois do ↓jH) e o assist dela, o chute que lança para o parceiro continuar no ar. Cada combo
 * é apertado no motor de verdade contra o Ryūga parado, e todas as pancadas têm de emendar.
 */
public class ComboYinluTest {
    private static final String YINLU = "p02", RYUGA = "player_base";

    private static BateriaCombos.Exec combo(String quem, String parceiro, String rota) {
        BateriaCombos.Cenario cn = new BateriaCombos.Cenario();
        cn.barras = 3;
        cn.gap = 100f;
        return BateriaCombos.executar(quem, parceiro, Arrays.asList(rota.split(" ")), cn);
    }

    private static BateriaCombos.Exec emenda(String quem, String parceiro, String rota, int acertos) {
        BateriaCombos.Exec x = combo(quem, parceiro, rota);
        assertTrue(rota + ": o combo emenda", x.valida);
        assertEquals(rota + ": acertos", acertos, x.acertos);
        return x;
    }

    private static BateriaCombos.Exec yinlu(String rota, int acertos) { return emenda(YINLU, RYUGA, rota, acertos); }

    @Test public void fluxoDaGarca() {
        BateriaCombos.Exec x = yinlu("L L M M H", 5);
        assertEquals("o crescente joga na parede", 1, x.parede);
    }

    @Test public void chuteRodopianteLancaEElaSobeAtras() {
        assertEquals(AttackDefinition.Launch.LAUNCH, GeneratedCharacters.get(YINLU).attack("S4").launch);
        // O S4 lança com o corpo no ar: o pulo depois dele é o super pulo que persegue o rival. Com
        // a arte nova (12/11) o ↓ + jH bate embaixo, colado: no fim do combo longo entra o jH.
        yinlu("L L M M S4 SJ jL jM jM J jL jM jH", 12);
    }

    @Test public void joelhoDaGarcaAbreOArSemO2H() {
        AttackDefinition joelho = GeneratedCharacters.get(YINLU).attack("6H");
        assertEquals("dois golpes", 2, joelho.maxHits);
        assertEquals("o segundo lança", AttackDefinition.Launch.LAUNCH, joelho.launch);
        assertTrue(joelho.cancelsInto(AttackDefinition.JUMP));
        yinlu("L L M M 6H SJ jL jM jM J jL jM jM j2H", 13);
    }

    @Test public void sobeDuasVezes() {
        // Termina no ar quicando o rival no chão (↓jH), pousa e lança de novo.
        BateriaCombos.Exec x = yinlu("L L M M S4 SJ jL j2H W15 2H SJ jL jM jM J jL jM jH", 15);
        assertEquals("quicou no chão", 1, x.chao);
        yinlu("L L M M 6H SJ jL jM jM j2H W15 2H SJ jL jM jM J jL jM jH", 17);
    }

    @Test public void oAssistDelaLancaParaOParceiroContinuarNoAr() {
        assertEquals("2H", GeneratedCharacters.get(YINLU).fighter.assistMove);
        // O Ryūga em campo chama a Yin-lu: o chute dela joga o rival para o alto e ele sobe atrás.
        emenda(RYUGA, YINLU, "L M M A W25 SJ jL jM J jL jM j2H", 9);
    }

    @Test public void pularSoDepoisDaUltimaPancada() {
        // Golpe de várias pancadas só cancela em pulo depois da última (senão o S4 pulava no
        // primeiro chute, antes do que lança).
        CombatFighter f = new CombatFighter(0, new FighterState(GeneratedCharacters.get(YINLU), "P1"), 500f, 1);
        AttackDefinition s4 = GeneratedCharacters.get(YINLU).attack("S4");
        f.status = CombatFighter.Status.ATTACK;
        f.attack = s4;
        f.outcome = CombatFighter.Outcome.HIT;
        f.attackFrame = s4.hitWindow.start;
        f.hitsOnTarget = 1;
        assertFalse("um chute só: ainda não pula", CancelSystem.canCancel(f, AttackDefinition.JUMP, null));
        f.hitsOnTarget = s4.maxHits;
        assertTrue("os dois chutes: pula", CancelSystem.canCancel(f, AttackDefinition.JUMP, null));
    }
}
