package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Test;

/**
 * A Bolsa de Apostas da Semifinal: o dinheiro entra em quem dá show, a zebra ganha mais, o
 * patamar paga barra de Super, quem esfria perde aposta, e a CPU lê a pressa e o show.
 */
public class BolsaApostasTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition TWO = GeneratedCharacters.get("player_two");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();

    /** Uma batalha 2x2 com a Bolsa (e o Apostador falando numa lista). */
    private static final class Luta {
        final FighterState[] p1 = {new FighterState(BASE, "P1"), new FighterState(TWO, "P2")};
        final FighterState[] cpu = {new FighterState(NPC, "C1"), new FighterState(BASE, "C2")};
        final CombatEngine engine = new CombatEngine(p1[0], cpu[0], 500f, 600f, new CombatConfig());
        final FighterInput[] in = {new FighterInput(), new FighterInput()};
        final BattleFlow flow = new BattleFlow();
        final BolsaApostas bolsa = new BolsaApostas();
        final List<String> eventos = new ArrayList<>();
        final List<Map<String, String>> vars = new ArrayList<>();
        final NarradorLuta.Voz voz = (e, v) -> {
            eventos.add(e);
            vars.add(new HashMap<>(v));
        };

        Luta() {
            engine.setTeam(0, p1);
            engine.setTeam(1, cpu);
            flow.setBattle(true);
            flow.reset();
            flow.startCountdown();
            bolsa.reset();
            bolsa.setNomes(c -> c == BASE ? "Ryūga" : "Kōga");
        }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
            flow.update(engine, true);
            bolsa.update(engine, flow, voz);
        }

        void steps(int n) { for (int i = 0; i < n; i++) step(); }

        void go() { steps(BattleFlow.READY_FRAMES + 2); }

        Map<String, String> de(String evento) {
            int i = eventos.lastIndexOf(evento);
            return i < 0 ? null : vars.get(i);
        }
    }

    @Test public void theBetsOpenEvenAndTheGamblerCallsThem() {
        Luta l = new Luta();
        assertEquals(BolsaApostas.INICIAL, l.bolsa.dinheiro(0));
        assertEquals(BolsaApostas.INICIAL, l.bolsa.dinheiro(1));
        l.go();
        assertEquals(1, java.util.Collections.frequency(l.eventos, "aposta_inicio"));
        assertEquals("Ryūga", l.de("aposta_inicio").get("p1"));
        assertEquals("odds iguais", l.bolsa.odd(0), l.bolsa.odd(1), 1e-9);
        assertEquals(0.5f, l.bolsa.parcela(0), 1e-6f);
    }

    @Test public void aHitThatLandsPaysWhoHit() {
        Luta l = new Luta();
        l.go();
        long antes = l.bolsa.dinheiro(1);
        boolean acertou = false;
        l.in[0].light = true;
        for (int i = 0; i < 40 && !acertou; i++) {
            l.step();
            for (CombatEngine.HitEvent e : l.engine.events()) acertou |= e.attacker == 0 && !e.blocked;
        }
        assertTrue("o L colado entra", acertou);
        assertTrue("quem bateu ganhou (com o primeiro golpe)",
            l.bolsa.dinheiro(0) >= BolsaApostas.INICIAL + BolsaApostas.PRIMEIRO_GOLPE / 2);
        assertEquals("quem apanhou não ganhou", antes, l.bolsa.dinheiro(1));
        assertTrue("a barra puxa para quem bateu", l.bolsa.parcela(0) > 0.5f);
    }

    @Test public void theSuperPaysBig() {
        Luta l = new Luta();
        l.go();
        l.p1[0].superMeter = CombatConfig.MAX_METER;
        l.in[0].superAttack = true;
        l.steps(3);
        assertTrue("o Super entrou na bolsa", l.bolsa.dinheiro(0) - BolsaApostas.INICIAL >= BolsaApostas.SUPER / 2);
    }

    @Test public void theUnderdogEarnsMoreForTheSameShow() {
        Luta l = new Luta();
        l.go();
        // A dupla da CPU apanhou muito: é a zebra.
        l.cpu[0].takeDamage(l.cpu[0].life * 7 / 10, 0);
        l.cpu[1].takeDamage(l.cpu[1].life / 2, 0);
        l.step();
        assertTrue("a CPU paga mais", l.bolsa.odd(1) > l.bolsa.odd(0));
        assertTrue(l.bolsa.multiplicador(1) > 1.0);
        assertTrue(l.bolsa.multiplicador(0) < 1.0);
        long a0 = l.bolsa.dinheiro(0), a1 = l.bolsa.dinheiro(1);
        l.bolsa.ganhar(l.engine, 0, 100_000L, l.voz);
        l.bolsa.ganhar(l.engine, 1, 100_000L, l.voz);
        assertTrue("o mesmo golpe vale mais na zebra", l.bolsa.dinheiro(1) - a1 > l.bolsa.dinheiro(0) - a0);
        assertTrue("o show vale mais para a CPU zebra", l.bolsa.show(1) > l.bolsa.show(0));
    }

    @Test public void everyLevelPaysAQuarterOfASuperBarToThePoint() {
        Luta l = new Luta();
        l.go();
        int barra = l.p1[0].superMeter;
        // Com as odds iguais o ganho é o valor cheio: um patamar exato.
        l.bolsa.ganhar(l.engine, 0, BolsaApostas.PATAMAR, l.voz);
        assertEquals(1, l.bolsa.patamares(0));
        assertEquals(barra + BolsaApostas.SUPER_DO_PATAMAR, l.p1[0].superMeter);
        assertTrue(l.eventos.contains("aposta_patamar"));
        assertEquals("Ryūga", l.de("aposta_patamar").get("nome"));
        assertTrue(l.de("aposta_patamar").get("valor").startsWith("¥ "));
        // Abaixo do próximo patamar: nada a mais.
        l.bolsa.ganhar(l.engine, 0, BolsaApostas.PATAMAR / 10, l.voz);
        assertEquals(1, l.bolsa.patamares(0));
        assertEquals("o outro lado não ganha barra", 0, l.cpu[0].superMeter);
    }

    @Test public void standingStillCoolsAndThenTheBetsDrop() {
        Luta l = new Luta();
        l.go();
        // Ninguém ataca: os dois esfriam.
        l.steps(BolsaApostas.ESFRIA + 2);
        assertTrue(l.eventos.contains("aposta_esfriando"));
        assertFalse("ainda não cai", l.bolsa.caindo(0));
        assertEquals(BolsaApostas.INICIAL, l.bolsa.dinheiro(0));
        assertTrue("a CPU já tem pressa", l.bolsa.pressa(1) > 0f);
        l.steps(BolsaApostas.CAI - BolsaApostas.ESFRIA + 60);
        assertTrue(l.bolsa.caindo(0));
        assertTrue(l.eventos.contains("aposta_caindo"));
        assertTrue("o dinheiro de quem ficou parado caiu", l.bolsa.dinheiro(0) < BolsaApostas.INICIAL * 97 / 100);
        assertEquals(1f, l.bolsa.pressa(1), 1e-6f);
        // Atacou: para de cair na hora.
        l.in[0].light = true;
        l.steps(3);
        assertFalse(l.bolsa.caindo(0));
        assertEquals(0f, l.bolsa.frio(0), 1e-6f);
        assertTrue("o outro, parado, continua caindo", l.bolsa.caindo(1));
    }

    @Test public void lostMoneyMustBeEarnedBackBeforeTheNextLevel() {
        Luta l = new Luta();
        l.go();
        l.steps(BolsaApostas.CAI + 600);
        long caiu = BolsaApostas.INICIAL - l.bolsa.dinheiro(0);
        assertTrue(caiu > 0);
        // Ganhar exatamente um patamar não paga: falta recuperar o que caiu.
        l.bolsa.ganhar(l.engine, 0, BolsaApostas.PATAMAR, l.voz);
        assertEquals(0, l.bolsa.patamares(0));
    }

    @Test public void moneyAndOddsAreWrittenTheWayTheHudShowsThem() {
        assertEquals("¥ 1.250.000", BolsaApostas.formatar(1_250_000L));
        assertEquals("¥ 500", BolsaApostas.formatar(500L));
        assertEquals("¥ 0", BolsaApostas.formatar(-3L));
        assertEquals("x2.4", BolsaApostas.formatarOdd(2.43));
        assertEquals("x1.0", BolsaApostas.formatarOdd(1.0));
    }

    // ------------------------------------------------------------ a CPU

    @Test public void theCpuGetsInAHurryWhenItsBetsCoolDown() {
        AiController ai = new AiController(new OpponentAi(new Random(1)), new Random(2));
        ai.setSecondsLeft(99);
        assertEquals(0f, ai.urgency(), 1e-6f);
        ai.setAposta(1f, 1.0);
        assertEquals("a pressa da aposta vira a pressa do relógio", 1f, ai.urgency(), 1e-6f);
        ai.setSecondsLeft(99);
        assertEquals("o relógio não apaga a pressa da aposta", 1f, ai.urgency(), 1e-6f);
        ai.setAposta(0f, 1.0);
        assertEquals(0f, ai.urgency(), 1e-6f);
    }

    @Test public void theUnderdogCpuGoesForTheUltraSooner() {
        long comum = 0, zebra = 0;
        for (long seed = 1; seed <= 30; seed++) {
            comum += quadrosAteOUltra(seed, 1.0);
            zebra += quadrosAteOUltra(seed, BolsaApostas.MULT_MAX);
        }
        assertTrue("zebra " + zebra + " x comum " + comum, zebra < comum);
    }

    private static int quadrosAteOUltra(long seed, double show) {
        FighterState p1 = new FighterState(BASE, "P1");
        FighterState[] cpu = {new FighterState(BASE, "CPU"), new FighterState(TWO, "CPU")};
        CombatEngine engine = new CombatEngine(p1, cpu[0], 500f, 750f, new CombatConfig());
        engine.setTeam(0, p1, new FighterState(TWO, "P2"));
        engine.setTeam(1, cpu);
        AiController ai = new AiController(new OpponentAi(new Random(seed)), new Random(seed * 0x9E3779B97F4A7C15L));
        ai.setEnabled(true);
        ai.setFullKit(true);
        ai.setAposta(0f, show);
        cpu[0].superMeter = CombatConfig.ULTRA_COST;
        FighterInput out = new FighterInput();
        for (int i = 0; i < 3000; i++) {
            ai.fill(engine, 1, out);
            if (out.ultra) return i;
        }
        return 3000;
    }
}
