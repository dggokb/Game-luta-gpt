package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.Arrays;
import org.junit.Test;

/**
 * → + H e H correndo da Natisuko e da Bonnie (16/11, vídeos do dono). A Natisuko: o Oi-zuki
 * (passo longo e soco reto, pune de meia distância) e o Gyaku-zuki firme (para a corrida
 * plantada, aguenta um golpe e lança baixo para o 2H). A Bonnie: o Salto mortal (passa por cima
 * e pega por trás: defende para o outro lado) e a Bundada do trampolim (por cima: agachado não
 * defende; o rival quica no chão).
 */
public class ComandoECorridaDaDuplaTest {
    private static final String NATISUKO = "p15", BONNIE = "p16", RYUGA = "player_base";

    private static BateriaCombos.Exec rota(String quem, String parceiro, float gap, String rota) {
        BateriaCombos.Cenario cn = new BateriaCombos.Cenario();
        cn.barras = 3;
        cn.gap = gap;
        cn.primeiroAcertoAte = rota.startsWith("C") ? 2 : 1;
        return BateriaCombos.executar(quem, parceiro, Arrays.asList(rota.split(" ")), cn);
    }

    private static void combo(String quem, String parceiro, float gap, String r, int acertos) {
        BateriaCombos.Exec x = rota(quem, parceiro, gap, r);
        assertTrue(quem + " " + r + " (a " + gap + "): o combo emenda", x.valida);
        assertEquals(quem + " " + r + " (a " + gap + "): acertos", acertos, x.acertos);
    }

    @Test public void oiZukiPuneDeMeiaDistancia() {
        AttackDefinition oi = GeneratedCharacters.get(NATISUKO).attack(CancelSystem.COMMAND_HEAVY);
        assertTrue("anda no passo longo", oi.mechanics.advanceSpeed > 0f);
        for (float gap : new float[]{140f, 260f, 320f}) {
            assertEquals("o Oi-zuki alcança a " + gap, 1, rota(NATISUKO, BONNIE, gap, "6H").acertos);
        }
        combo(NATISUKO, BONNIE, 100f, "2L M 6H S4", 4);
        combo(NATISUKO, BONNIE, 100f, "2L M 6H SUPER", 8);
    }

    @Test public void gyakuZukiAguentaUmGolpeELancaBaixo() {
        AttackDefinition gyaku = GeneratedCharacters.get(NATISUKO).attack(CancelSystem.RUN_HEAVY);
        assertEquals("aguenta um golpe", 1, gyaku.mechanics.armorHits);
        assertEquals("lança em arco baixo", AttackDefinition.Launch.ARC, gyaku.launch);
        assertTrue("baixo: " + gyaku.mechanics.arcHeight, gyaku.mechanics.arcHeight <= 160f);
        combo(NATISUKO, BONNIE, 500f, "C DH 2H", 2);
        combo(NATISUKO, BONNIE, 500f, "C DH 2H SJ jL jM jH", 5);
        combo(NATISUKO, BONNIE, 500f, "C DH SUPER", 6);   // 18/11: o Super segura o rival no ar e pega as 5
    }

    /**
     * A Bonnie dá o Salto mortal a {@code gap} do Ryūga, que segura {@code antes} com ela já em cima e,
     * depois de ela passar, {@code depois}: {acertou (1) ou defendeu (0), por trás (1)}.
     */
    private static int[] salto(float gap, int antes, int depois) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get(BONNIE), GeneratedCharacters.get(RYUGA), 900f, 900f + gap);
        CombatFighter me = s.f(0), d = s.f(1);
        s.in[0].direction = 1;
        s.in[0].heavy = true;
        s.step();
        s.in[0].direction = 0;
        for (int t = 0; t < 60; t++) {
            // Segura só com ela já em cima (andar para trás antes mudaria o encontro).
            boolean passou = me.x > d.x;
            s.in[1].direction = passou ? depois : Math.abs(d.x - me.x) < 40f ? antes : 0;
            boolean atras = (d.x - me.x) * me.facing < 0f;
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) {
                if (e.attacker == 0) return new int[]{e.blocked ? 0 : 1, atras ? 1 : 0};
            }
        }
        return new int[]{-1, 0};
    }

    @Test public void saltoMortalPassaPorCimaEPegaPorTras() {
        AttackDefinition salto = GeneratedCharacters.get(BONNIE).attack(CancelSystem.COMMAND_HEAVY);
        assertTrue(salto.mechanics.crossUp);
        assertTrue(salto.mechanics.passThrough);
        // O Ryūga olha para a esquerda: o "trás" antigo é a direita (1), o novo a esquerda (5).
        int[] parado = salto(140f, 0, 0);
        assertEquals("acerta", 1, parado[0]);
        assertEquals("por trás", 1, parado[1]);
        assertEquals("segurando o lado antigo, apanha", 1, salto(140f, 1, 1)[0]);
        assertEquals("segurando o lado novo depois de ela passar, defende", 0, salto(140f, 1, 5)[0]);
    }

    @Test public void depoisDoSaltoElaViraEContinua() {
        // Cruzando (100 a 170) ou não (200, 240): o L seguinte sai virado para o rival.
        for (float gap : new float[]{100f, 140f, 170f, 200f, 240f}) {
            combo(BONNIE, NATISUKO, gap, "6H L M S4", 7);
        }
        combo(BONNIE, NATISUKO, 140f, "6H 2L M 2H SJ jL jM jH", 7);
    }

    /** A Bonnie corre e dá a Bundada no Ryūga, que segura {@code guarda}: 1 acertou, 0 defendeu. */
    private static int bundada(int guarda) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get(BONNIE), GeneratedCharacters.get(RYUGA), 500f, 1100f);
        CombatFighter me = s.f(0), d = s.f(1);
        for (int t = 0; t < 120 && Math.abs(d.x - me.x) > 260f; t++) {
            s.in[0].dash = !me.forwardDashing;
            s.in[0].direction = 1;
            s.step();
        }
        s.in[0].direction = 1;
        s.in[0].heavy = true;
        s.step();
        assertEquals("saiu a Bundada", CancelSystem.RUN_HEAVY, s.attackId(0));
        s.in[0].direction = 0;
        for (int t = 0; t < 60; t++) {
            s.in[1].direction = guarda;
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) {
                if (e.attacker == 0) return e.blocked ? 0 : 1;
            }
        }
        return -1;
    }

    @Test public void bundadaPorCimaQuicaORival() {
        AttackDefinition bundada = GeneratedCharacters.get(BONNIE).attack(CancelSystem.RUN_HEAVY);
        assertTrue("por cima", bundada.overhead);
        assertEquals(AttackDefinition.Launch.GROUND_BOUNCE, bundada.launch);
        assertTrue("quem está em pé também quica", bundada.mechanics.standBounce);
        // O Ryūga olha para a esquerda: ↘ (2) é a defesa agachada, → (1) a em pé.
        assertEquals("agachado não defende", 1, bundada(2));
        assertEquals("em pé defende", 0, bundada(1));
        combo(BONNIE, NATISUKO, 500f, "C DH 2H SJ jL jM J jL jM jH", 6);
    }
}
