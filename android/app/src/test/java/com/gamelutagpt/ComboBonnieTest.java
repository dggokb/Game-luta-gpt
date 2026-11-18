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
        // 16/11: rápida (1.º acerto no quadro 10, 45 no total) e anda ~245 px: pega depois do esquete.
        assertEquals(10, s4.startupFrames);
        assertTrue("dura no máximo 45 quadros: " + s4.totalFrames, s4.totalFrames <= 45);
        bonnie("L M S4 W20 SJ jL jM jH S", 10);
        bonnie("L M S4 W20 SJ jL jM J jL jM jH S", 12);
        bonnie("L M L M S4 W20 SJ jL jM J jL jM jH S", 14);
    }

    @Test public void caixaNoMeioDoCombo() {
        // 16/11: a luva crava o rival no chão e ele quica no lugar, aberto para o combo.
        AttackDefinition s2 = GeneratedCharacters.get(BONNIE).attack("S2");
        assertEquals(AttackDefinition.Launch.GROUND_BOUNCE, s2.launch);
        // Caixa perto (L), L, M, L: o rival é empurrado até a luva, quica, e ela lança no 2H (16/11,
        // com os normais mais rápidos o esquete cabe antes do 2H).
        BateriaCombos.Exec x = emenda(combo(BONNIE, NATISUKO, "S2L L M L 2H SJ jL jM J jL jM jH S", 100f, false, 2), "caixa e 2H", 11);
        assertEquals("quicou no chão", 1, x.chao);
        // Ou pega o quique com a Estrelinha.
        emenda(combo(BONNIE, NATISUKO, "S2L L M S4 W20 SJ jL jM J jL jM jH S", 100f, false, 2), "caixa e Estrelinha", 13);
        // Caixa no meio (M): o esquete, e o H chuta a caixa de volta no rival.
        emenda(combo(BONNIE, NATISUKO, "S2M L M L M H W10 C120 2H SJ jL jM J jL jM jH S", 100f, false, 2), "esquete chuta a caixa", 13);
    }

    @Test public void armadilhaDeLonge() {
        // A mina abre com o rival em cima, ele quica e ela corre para lançar.
        emenda(combo(BONNIE, NATISUKO, "S2L W20 C140 2H SJ jL jM jH S", 265f, false, 2), "armadilha", 6);
    }

    @Test public void chutaACaixa() {
        // De longe: a caixa cai perto dela (L), o ↓ + M chuta e ela voa rente ao chão até o rival.
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get(BONNIE), GeneratedCharacters.get("player_base"), 500f, 1100f);
        CombatFighter me = s.f(0), r = s.f(1);
        me.commandButton = "L";
        me.buffer.push(InputBuffer.Button.SPECIAL, "S2", false);
        s.steps(50);
        assertEquals(1, s.engine.traps.size());
        boolean chutou = false;
        for (int i = 0; i < 20 && !chutou; i++) {
            s.in[0].direction = 2;
            s.in[0].medium = i == 0;
            s.step();
            chutou = s.engine.cues().contains("CAIXA_CHUTADA");
        }
        s.in[0].direction = 0;
        assertTrue("chutou a caixa", chutou);
        assertTrue("a caixa saiu do chão", s.engine.traps.isEmpty());
        // Em voo não acerta: para na frente dele, abre e a luva soca.
        s.until(() -> !s.engine.traps.isEmpty(), 60);
        Armadilha aberta = s.engine.traps.get(0);
        assertTrue("abriu", aberta.open());
        assertEquals("parou com a luva no rival", r.x, aberta.x + aberta.direction * aberta.data.trapReach, 30f);
        s.until(() -> r.status != CombatFighter.Status.NEUTRAL, 30);
        assertEquals("a luva pegou e ele quica", CombatFighter.Status.AIR_HITSTUN, r.status);
        // No combo: a caixa chutada abre o caminho para correr e lançar.
        emenda(combo(BONNIE, NATISUKO, "S2L 2M W10 C120 2H SJ jL jM jH S", 600f, false, 3), "chute de longe", 5);
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
        assertEquals("a luva o pega", CombatFighter.Status.AIR_HITSTUN, r.status);
        assertEquals("cravado no chão: vai quicar", CombatFighter.BOUNCE_GROUND, r.bounce);
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
        emenda(combo(NATISUKO, BONNIE, "L A L M H S4", 100f, false, 1), "assist", 6);
    }

    /** Combos da dupla (16/11): a Bonnie e a Natisuko juntas, uma chamando a outra no meio. */
    @Test public void duplaDoCirco() {
        // Bonnie em campo: o esquete joga na parede, ela corre e chama a Natisuko, cujo 2H lança; a
        // Bonnie sobe atrás e fecha no ar com o Balão.
        assertEquals("2H", GeneratedCharacters.get(NATISUKO).fighter.assistMove);
        emenda(combo(BONNIE, NATISUKO, "L M L M H W5 C120 A W25 SJ jL jM J jL jM jH S", 100f, false, 1), "dupla do circo", 12);
        // A caixa armada, a luva devolve o rival e a Natisuko o lança para o combo no ar.
        emenda(combo(BONNIE, NATISUKO, "S2M L M L M H W10 C120 A W25 SJ jL jM J jL jM jH S", 100f, false, 2), "caixa e Natisuko", 13);
        // Natisuko em campo: chama a caixa da Bonnie no meio; a luva pega e ela continua.
        emenda(combo(NATISUKO, BONNIE, "L M A L M L M H", 100f, false, 1), "Natisuko e a caixa", 8);
    }

    @Test public void cascaFicaNoChaoEElaEscorregaNaRasteira() {
        // ↓↓ só joga a casca (não bate); ela fica no chão ~100 px à frente.
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get(BONNIE), GeneratedCharacters.get("player_base"), 500f, 850f);
        CombatFighter me = s.f(0), r = s.f(1);
        me.commandButton = "M";
        me.buffer.push(InputBuffer.Button.SPECIAL, "S3", false);
        s.steps(30);
        assertEquals(1, s.engine.cascas.size());
        assertEquals(100f, s.engine.cascas.get(0).x - me.x, 2f);
        assertEquals("jogar não bate", CombatFighter.Status.NEUTRAL, r.status);
        // Andando para a frente, pisa, escorrega e desliza; quem leva quica no chão.
        float x0 = me.x;
        for (int i = 0; i < 120 && r.status == CombatFighter.Status.NEUTRAL; i++) {
            if (!me.attacking()) s.in[0].direction = 1;
            s.step();
        }
        assertEquals(Casca.SLIDE, me.attack.id);
        assertTrue("desliza para perto dele", me.x - x0 > 120f);
        assertEquals(CombatFighter.Status.AIR_HITSTUN, r.status);
        assertEquals("vai quicar", CombatFighter.BOUNCE_GROUND, r.bounce);
        assertTrue(s.engine.cascas.isEmpty());
        // A rasteira e o Super (de ~350 px: joga, corre em cima e desliza).
        emenda(combo(BONNIE, NATISUKO, "S3 W5 C W20 SUPER", 350f, false, 3), "casca e Super", 3);
    }

    @Test public void oRivalQuePisaNaCascaCai() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get(BONNIE), GeneratedCharacters.get("player_base"), 500f, 900f);
        s.f(0).commandButton = "M";
        s.f(0).buffer.push(InputBuffer.Button.SPECIAL, "S3", false);
        boolean caiu = false;
        for (int i = 0; i < 200 && !caiu; i++) {
            if (i > 30) s.in[1].direction = 5;   // o rival anda para a Bonnie
            s.step();
            caiu = s.engine.cues().contains("ESCORREGOU");
        }
        assertTrue(caiu);
        assertEquals(CombatFighter.Status.KNOCKDOWN, s.f(1).status);
        // A casca que cai em cima dele não derruba: só depois de sair e pisar de novo.
        CombatEngineTest.Sim t = new CombatEngineTest.Sim(GeneratedCharacters.get(BONNIE), GeneratedCharacters.get("player_base"), 500f, 600f);
        t.f(0).commandButton = "M";
        t.f(0).buffer.push(InputBuffer.Button.SPECIAL, "S3", false);
        t.steps(60);
        assertEquals(CombatFighter.Status.NEUTRAL, t.f(1).status);
        assertTrue(t.engine.cascas.get(0).rivalEmCima);
    }

    @Test public void puloDeCirco() {
        CharacterDefinition b = GeneratedCharacters.get(BONNIE), base = GeneratedCharacters.get("player_base");
        // 10% mais alto e 10% mais tempo no ar: a gravidade do pulo dela é menor.
        assertTrue("fica mais tempo no ar", b.fighter.jumpGravityScale() < base.fighter.jumpGravityScale());
        assertEquals(1100, b.fighter.jumpHeightPermille);
    }
}
