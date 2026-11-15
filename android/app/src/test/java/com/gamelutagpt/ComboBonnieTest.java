package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.Arrays;
import org.junit.Test;

/**
 * Combos de assinatura da Bonnie (16/11, docs/personagens/combos-assinatura.md): a palhaça do
 * truque. O esquete (L, M, L, M, H: o M volta para o L), a Estrelinha (S4) que lança e pula atrás,
 * a Caixa surpresa (S2) cuja luva devolve o rival em arco para ela continuar (no meio do combo ou
 * armada de longe), o pulo de circo e o assist dela, a caixa armada para o parceiro. Cada combo é
 * apertado no motor de verdade contra o Ryūga parado, e todas as pancadas têm de emendar.
 */
public class ComboBonnieTest {
    private static final String BONNIE = "p16", NATISUKO = "p15";

    private static BateriaCombos.Exec combo(String quem, String parceiro, String rota, float gap, boolean canto, int primeiro) {
        BateriaCombos.Cenario cn = new BateriaCombos.Cenario();
        cn.barras = 3;
        cn.gap = gap;
        cn.canto = canto;
        cn.primeiroAcertoAte = primeiro;
        return BateriaCombos.executar(quem, parceiro, Arrays.asList(rota.split(" ")), cn);
    }

    private static BateriaCombos.Exec emenda(BateriaCombos.Exec x, String rota, int acertos) {
        assertTrue(rota + ": o combo emenda", x.valida);
        assertEquals(rota + ": acertos", acertos, x.acertos);
        return x;
    }

    private static BateriaCombos.Exec bonnie(String rota, int acertos) {
        return emenda(combo(BONNIE, NATISUKO, rota, 100f, false, 1), rota, acertos);
    }

    @Test public void esqueteDoPalhaco() {
        // O M volta para o L: L, M, L, M e o H que joga na parede.
        assertTrue(GeneratedCharacters.get(BONNIE).attack("M").cancelsInto("L"));
        assertEquals("circo", GeneratedCharacters.get(BONNIE).fighter.comboStyle);
        assertEquals("quica na parede", 1, bonnie("L M L M H", 5).parede);
    }

    @Test public void esqueteECorreAtrasDoQuique() {
        // Do quique da parede ela corre, lança no 2H e fecha no ar com o Balão.
        bonnie("L M L M H W5 C120 2H SJ jL jM J jL jM jH S", 12);
        BateriaCombos.Exec canto = emenda(combo(BONNIE, NATISUKO, "L M L M H W20 2H SJ jL jM jH S", 100f, true, 1),
            "L M L M H W20 2H SJ jL jM jH S (canto)", 10);
        assertEquals(1, canto.parede);
    }

    @Test public void estrelinhaLancaEPulaAtras() {
        AttackDefinition s4 = GeneratedCharacters.get(BONNIE).attack("S4");
        assertEquals("a última passada lança", AttackDefinition.Launch.LAUNCH, s4.launch);
        assertTrue("cancela em pulo", s4.cancelsInto(AttackDefinition.JUMP));
        assertEquals("quatro passadas", 4, s4.maxHits);
        bonnie("L M S4 W40 SJ jL jM jH S", 10);
        bonnie("L M S4 W40 SJ jL jM J jL jM jH S", 12);
    }

    @Test public void caixaNoMeioDoCombo() {
        // Arma a caixa (pavio médio) e começa o combo: a luva pega no meio e devolve o rival em
        // arco; ela lança no 2H e fecha no ar.
        AttackDefinition s2 = GeneratedCharacters.get(BONNIE).attack("S2");
        assertEquals(AttackDefinition.Launch.ARC, s2.launch);
        assertTrue("o arco volta para ela", s2.mechanics.arcDistance < 0f);
        // Caixa perto (L): cai logo à frente; no L, M, L, M o rival é empurrado até a luva.
        emenda(combo(BONNIE, NATISUKO, "S2L L M L M W5 2H SJ jL jM J jL jM jH S", 100f, false, 2), "caixa no combo", 11);
    }

    @Test public void armadilhaDeLonge() {
        // O botão escolhe onde ela cai: M no meio, H longe. A mina abre com o rival em cima.
        emenda(combo(BONNIE, NATISUKO, "S2M W30 W10 L M S4 W40 SJ jL jM jH S", 330f, false, 3), "armadilha (M)", 8);
        emenda(combo(BONNIE, NATISUKO, "S2H W30 W10 C 2H SJ jL jM jH S", 450f, false, 3), "armadilha (H)", 6);
    }

    /** Onde a caixa cai com L, M e H, quando cai e quando a Bonnie fica livre. */
    private static float[] caixa(String botao) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get(BONNIE), GeneratedCharacters.get("player_base"), 500f, 1500f);
        CombatFighter me = s.f(0);
        me.commandButton = botao;
        me.buffer.push(InputBuffer.Button.SPECIAL, "S2", false);
        for (int i = 0; i < 120; i++) {
            s.step();
            if (!s.engine.traps.isEmpty()) return new float[] {s.engine.traps.get(0).x - me.x, i};
        }
        return null;
    }

    @Test public void oBotaoEscolheOndeACaixaCai() {
        float[] l = caixa("L"), m = caixa("M"), h = caixa("H");
        assertTrue("L perto: " + l[0], l[0] < 220f);
        assertTrue("M no meio: " + m[0], m[0] > 250f && m[0] < 340f);
        assertTrue("H longe: " + h[0], h[0] > 380f);
        for (float[] c : new float[][] {l, m, h}) assertTrue("cai rápido: quadro " + c[1], c[1] <= 40f);
    }

    @Test public void aCaixaEMinaEAbreComORivalPerto() {
        // Armada no meio (M), espera fechada; o rival anda até a luva e ela abre e o pega.
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get(BONNIE), GeneratedCharacters.get("player_base"), 500f, 1100f);
        CombatFighter me = s.f(0), r = s.f(1);
        me.commandButton = "M";
        me.buffer.push(InputBuffer.Button.SPECIAL, "S2", false);
        s.steps(60);
        assertEquals(1, s.engine.traps.size());
        assertTrue("armada e fechada, esperando", s.engine.traps.get(0).armed());
        int abriu = -1;
        for (int i = 0; i < 200 && r.status == CombatFighter.Status.NEUTRAL; i++) {
            s.in[1].direction = 5;   // o rival anda para a Bonnie
            s.step();
            if (abriu < 0 && s.engine.cues().contains("TRAP_OPEN")) abriu = i;
        }
        assertTrue("abriu pelo rival, antes do fim da mina", abriu >= 0 && abriu < 120);
        assertEquals("a luva o devolve em arco", CombatFighter.Status.AIR_HITSTUN, r.status);
        assertTrue("voltando para a Bonnie", r.vx * (me.x - r.x) > 0f);
        // Ninguém chega: abre sozinha no fim (4 s) e some.
        CombatEngineTest.Sim so = new CombatEngineTest.Sim(GeneratedCharacters.get(BONNIE), GeneratedCharacters.get("player_base"), 500f, 1500f);
        so.f(0).commandButton = "M";
        so.f(0).buffer.push(InputBuffer.Button.SPECIAL, "S2", false);
        so.steps(37 + 8 + 240 + 50);
        assertTrue("sumiu", so.engine.traps.isEmpty());
    }

    @Test public void oAssistDelaEAArmadilha() {
        assertEquals("S2", GeneratedCharacters.get(BONNIE).fighter.assistMove);
        // A Natisuko em campo chama a Bonnie: a caixa cai e a luva pega no meio do combo dela.
        emenda(combo(NATISUKO, BONNIE, "L A L M H S4", 100f, false, 1), "assist", 7);
    }

    /** Combos da dupla (16/11): a Bonnie e a Natisuko juntas, uma chamando a outra no meio. */
    @Test public void duplaDoCirco() {
        // Bonnie em campo: o esquete joga na parede, ela corre e chama a Natisuko, cujo 2H lança; a
        // Bonnie sobe atrás e fecha no ar com o Balão.
        assertEquals("2H", GeneratedCharacters.get(NATISUKO).fighter.assistMove);
        emenda(combo(BONNIE, NATISUKO, "L M L M H W5 C120 A W25 SJ jL jM J jL jM jH S", 100f, false, 1), "dupla do circo", 12);
        // A caixa armada, a luva devolve o rival e a Natisuko o lança para o combo no ar.
        emenda(combo(BONNIE, NATISUKO, "S2L L M L M A W30 SJ jL jM J jL jM jH S", 100f, false, 2), "caixa e Natisuko", 11);
        // Natisuko em campo: chama a caixa da Bonnie no meio; a luva pega e ela continua.
        emenda(combo(NATISUKO, BONNIE, "L M A L M L M H", 100f, false, 1), "Natisuko e a caixa", 8);
    }

    @Test public void puloDeCirco() {
        CharacterDefinition b = GeneratedCharacters.get(BONNIE), base = GeneratedCharacters.get("player_base");
        // 10% mais alto e 10% mais tempo no ar: a gravidade do pulo dela é menor.
        assertTrue("fica mais tempo no ar", b.fighter.jumpGravityScale() < base.fighter.jumpGravityScale());
        assertEquals(1100, b.fighter.jumpHeightPermille);
    }
}
