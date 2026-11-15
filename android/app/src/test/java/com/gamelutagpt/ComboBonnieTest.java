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
        emenda(combo(BONNIE, NATISUKO, "S2M L M L M W10 2H SJ jL jM jH S", 100f, false, 2), "caixa no combo", 9);
    }

    @Test public void armadilhaDeLonge() {
        // De longe: a caixa cai na frente do rival, a luva o devolve em arco e ela pega no ar.
        emenda(combo(BONNIE, NATISUKO, "S2L W30 W30 W10 L M S4 W40 SJ jL jM jH S", 330f, false, 3), "armadilha", 9);
    }

    @Test public void oAssistDelaEAArmadilha() {
        assertEquals("S2", GeneratedCharacters.get(BONNIE).fighter.assistMove);
        // A Natisuko em campo chama a Bonnie: a caixa cai e a luva pega no meio do combo dela.
        emenda(combo(NATISUKO, BONNIE, "L M A L M H", 100f, false, 1), "assist", 6);
    }

    @Test public void puloDeCirco() {
        CharacterDefinition b = GeneratedCharacters.get(BONNIE), base = GeneratedCharacters.get("player_base");
        // 10% mais alto e 10% mais tempo no ar: a gravidade do pulo dela é menor.
        assertTrue("fica mais tempo no ar", b.fighter.jumpGravityScale() < base.fighter.jumpGravityScale());
        assertEquals(1100, b.fighter.jumpHeightPermille);
    }
}
