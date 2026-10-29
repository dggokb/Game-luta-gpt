package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Entrada do DHC (frente H5): o parceiro de Super corpo a corpo vai até onde o Super dele pega o
 * rival deslizando, como o assist entra (sem pular de lugar num quadro), nunca dentro do corpo do
 * rival nem fora da arena, e o Super dele e o rival esperam a entrada.
 */
public class DhcEntradaTest {
    private static final String[] ELENCO = {"player_base", "p02", "p03", "p04", "p05", "p06", "p07", "p08",
        "p09", "p10", "p11", "p12", "p13", "p15", "p16"};

    /** Passo por quadro do titular depois do DHC (maior) e a folga de corpo no fim da entrada. */
    private static float[] run(String point, String partner, float x0, float x1) {
        FighterState first = new FighterState(GeneratedCharacters.get(point), "P1");
        FighterState second = new FighterState(GeneratedCharacters.get(partner), "P1");
        FighterState dummy = new FighterState(GeneratedCharacters.get("player_base"), "P2");
        CombatEngine e = new CombatEngine(first, dummy, x0, x1, new CombatConfig());
        e.setTeam(0, first, second);
        dummy.life = 10_000_000;
        first.superMeter = 3 * CombatConfig.METER_PER_BAR;
        FighterInput[] in = {new FighterInput(), new FighterInput()};
        String[] route = {"M", "H", "SUPER"};
        int k = 0;
        float maxStep = 0f, gapAfterEntry = Float.NaN;
        boolean dhc = false;
        for (int t = 0; t < 480; t++) {
            CombatFighter me = e.fighter(0);
            if (k < route.length) {
                if (route[k].equals("M")) in[0].medium = true;
                else if (route[k].equals("H")) in[0].heavy = true;
                else in[0].superAttack = true;
            } else if (me.state == first) in[0].assist = true;
            float before = me.x;
            e.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
            me = e.fighter(0);
            CombatFighter o = e.fighter(1);
            if (k < route.length && me.attacking() &&
                (route[k].equals("SUPER") ? me.attack.kind == AttackDefinition.Kind.SUPER : me.attack.id.equals(route[k]))) k++;
            if (e.cues().contains("DHC")) dhc = true;
            if (dhc && me.state == second && me.attacking() && me.attack.kind == AttackDefinition.Kind.SUPER) {
                if (me.dhcEnterFrames > 0 || Float.isNaN(gapAfterEntry)) maxStep = Math.max(maxStep, Math.abs(me.x - before));
                if (me.dhcEnterFrames == 0 && Float.isNaN(gapAfterEntry) && o.grounded) {
                    gapAfterEntry = Math.abs(o.x - me.x) - (me.body().pushHalfWidth + o.body().pushHalfWidth);
                }
                assertTrue("dentro da arena", me.x >= Arena.LEFT_BOUND - 0.5f && me.x <= Arena.RIGHT_BOUND + 0.5f);
            }
        }
        assertTrue(point + "+" + partner + ": o DHC saiu", dhc);
        return new float[] {maxStep, gapAfterEntry};
    }

    @Test public void theP08EntersSlidingWhereTheHenchmenThrewTheRival() {
        float[] r = run("p07", "p08", 900f, 1040f);
        // Antes: pulava 275 px num quadro. Agora no máximo o primeiro passo da entrada do assist.
        assertTrue("passo de " + r[0] + " px", r[0] <= 70f);
    }

    @Test public void noPartnerJumpsNorEntersTheRivalsBody() {
        for (String point : ELENCO) {
            for (String partner : new String[] {"p03", "p08", "p12", "p15"}) {
                if (point.equals(partner)) continue;
                float[] r = run(point, partner, 900f, 1040f);
                assertTrue(point + "+" + partner + ": passo de " + r[0] + " px", r[0] <= 70f);
                if (!Float.isNaN(r[1])) assertTrue(point + "+" + partner + ": folga " + r[1], r[1] >= -1f);
            }
        }
    }

    @Test public void inTheCornerThePartnerStaysInsideTheArena() {
        float[] r = run("p07", "p03", 2300f, 2440f);
        assertTrue(r[0] <= 70f);
    }
}
