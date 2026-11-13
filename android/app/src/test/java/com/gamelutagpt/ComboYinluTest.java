package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.Arrays;
import org.junit.Test;

/**
 * Combos de assinatura da Yin-lu (12/11, docs/personagens/combos-assinatura.md): longos, leves e no
 * ar. O fluxo da garça (L, L, M, M, H), o Chute rodopiante (S4) que joga em arco para a frente (e
 * quica na parede perto da borda; 13/11), o Joelho da garça (→ + H,
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

    private static BateriaCombos.Exec noCanto(String rota, int acertos) {
        BateriaCombos.Cenario cn = new BateriaCombos.Cenario();
        cn.barras = 3;
        cn.gap = 100f;
        cn.canto = true;
        BateriaCombos.Exec x = BateriaCombos.executar(YINLU, RYUGA, Arrays.asList(rota.split(" ")), cn);
        assertTrue(rota + " (canto): o combo emenda", x.valida);
        assertEquals(rota + " (canto): acertos", acertos, x.acertos);
        return x;
    }

    @Test public void chuteRodopianteJogaEmArcoParaAFrente() {
        // 13/11: o giro de ponta-cabeça não lança mais para cima; joga o rival em arco para a
        // frente e, perto da borda, ele quica na parede.
        AttackDefinition s4 = GeneratedCharacters.get(YINLU).attack("S4");
        assertEquals(AttackDefinition.Launch.ARC, s4.launch);
        assertTrue("quica na parede perto da borda", s4.mechanics.arcWall);
        assertFalse("sem pulo depois: o rival vai para longe", s4.cancelsInto(AttackDefinition.JUMP));
        // No meio da arena cai derrubado longe; ela corre, pega no chão (↓ + M) e recomeça.
        assertEquals("cinco pancadas, uma a cada passada da perna no giro", 5, s4.maxHits);
        BateriaCombos.Exec meio = yinlu("L L M M S4", 9);
        assertEquals("no meio não tem parede", 0, meio.parede);
        assertTrue("caiu longe: " + meio.gapFinal, meio.gapFinal > 250f);
        yinlu("L L M M S4 W80 C120 2M L M M", 13);
        yinlu("L L M M S4 W80 C120 2M 2H SJ jL jM jM J jL jM jH", 17);
        // No canto quica na parede e volta: o Joelho lança para o combo no ar, ou o S2.
        assertEquals("quicou na parede", 1, noCanto("L L M M S4 W25 6H SJ jL jM jM J jL jM jH", 16).parede);
        noCanto("L L M M S4 W30 S2", 12);
    }

    /** O S4 da Yin-lu com o rival a {@code daParede} px da borda da direita: quicou e a maior altura depois. */
    private static float[] arcoAteAParede(float daParede) {
        float rx = Arena.RIGHT_BOUND - daParede;
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get(YINLU), GeneratedCharacters.get(RYUGA), rx - 100f, rx);
        CombatFighter me = s.f(0), r = s.f(1);
        me.commandButton = "M";
        me.buffer.push(InputBuffer.Button.SPECIAL, "S4", false);
        boolean quicou = false;
        float depois = 0f;
        for (int i = 0; i < 150; i++) {
            s.step();
            if (s.engine.cues().contains("WALL_BOUNCE")) quicou = true;
            if (quicou) depois = Math.max(depois, Arena.GROUND_Y - r.y);
        }
        return new float[] {quicou ? 1f : 0f, depois};
    }

    @Test public void oArcoQuicaNaParedeSoSeChegarNela() {
        CombatConfig c = new CombatConfig();
        float teto = c.wallBounceUp * c.wallBounceUp / (2f * c.gravity) + 15f;   // + o corpo nos quadros
        for (float perto : new float[] {0f, 150f, 250f, 330f}) {
            float[] x = arcoAteAParede(perto);
            assertEquals("a " + perto + " px da parede quica", 1f, x[0], 0f);
            // Bate já no alto do arco e sobe só até a altura de um quique de perto do chão.
            assertTrue("a " + perto + " px: sobe " + x[1], x[1] <= teto);
        }
        assertEquals("longe da parede, só cai", 0f, arcoAteAParede(450f)[0], 0f);
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
        BateriaCombos.Exec x = yinlu("L L M M 6H SJ jL jM jM j2H W15 2H SJ jL jM jM J jL jM jH", 17);
        assertEquals("quicou no chão", 1, x.chao);
    }

    @Test public void oAssistDelaLancaParaOParceiroContinuarNoAr() {
        assertEquals("2H", GeneratedCharacters.get(YINLU).fighter.assistMove);
        // O Ryūga em campo chama a Yin-lu: o chute dela joga o rival para o alto e ele sobe atrás.
        emenda(RYUGA, YINLU, "L M M A W25 SJ jL jM J jL jM j2H", 9);
    }

    @Test public void pularSoDepoisDaUltimaPancada() {
        // Golpe de várias pancadas só cancela em pulo depois da última (senão o Joelho pulava na
        // primeira, antes da que lança).
        CombatFighter f = new CombatFighter(0, new FighterState(GeneratedCharacters.get(YINLU), "P1"), 500f, 1);
        AttackDefinition joelho = GeneratedCharacters.get(YINLU).attack("6H");
        f.status = CombatFighter.Status.ATTACK;
        f.attack = joelho;
        f.outcome = CombatFighter.Outcome.HIT;
        f.attackFrame = joelho.hitWindow.start;
        f.hitsOnTarget = 1;
        assertFalse("um golpe só: ainda não pula", CancelSystem.canCancel(f, AttackDefinition.JUMP, null));
        f.hitsOnTarget = joelho.maxHits;
        assertTrue("os dois golpes: pula", CancelSystem.canCancel(f, AttackDefinition.JUMP, null));
    }
}
