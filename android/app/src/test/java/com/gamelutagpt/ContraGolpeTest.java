package com.gamelutagpt;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * O contra-golpe que leu o golpe (o S3 da Yin-lu, o do p11) deixa o rival
 * atordoado: quem leu fica bem na frente e dá tempo de correr e emendar (pedido do jogador: antes o
 * rival era empurrado e voltava a atacar, +5 a +8 a 180 px).
 */
public class ContraGolpeTest {
    /** {vantagem em quadros, 1 se o L de quem leu emendou no atordoamento}. */
    private static int[] contraGolpe(String id) {
        CharacterDefinition me = GeneratedCharacters.get(id), rival = GeneratedCharacters.get("player_base");
        CombatEngine e = new CombatEngine(new FighterState(me, "P1"), new FighterState(rival, "P2"), 500f, 640f, new CombatConfig());
        FighterInput a = new FighterInput(), b = new FighterInput();
        a.special = "S3";
        boolean countered = false, rivalPressed = false, emendou = false;
        int at = -1, meFree = -1, rivalFree = -1;
        for (int i = 0; i < 200; i++) {
            CombatFighter p = e.fighter(0), d = e.fighter(1);
            if (!rivalPressed && p.attacking() && p.attackFrame >= 2) {
                b.heavy = true;   // o H do rival cai dentro da janela de leitura
                rivalPressed = true;
            }
            if (meFree >= 0 && !emendou) {
                float gap = Math.abs(d.x - p.x);
                a.direction = p.facing > 0 ? 1 : 5;
                a.dash = gap > 150f;
                a.light = gap <= 150f;
            }
            boolean preso = d.status == CombatFighter.Status.HITSTUN;
            e.step(a, b);
            a.clearPresses();
            a.special = null;
            b.clearPresses();
            b.heavy = false;
            if (!countered && e.cues().contains("COUNTER")) {
                countered = true;
                at = i;
                continue;
            }
            if (countered && meFree < 0 && p.canAct()) meFree = i;
            if (countered && rivalFree < 0 && d.canAct()) rivalFree = i;
            for (CombatEngine.HitEvent ev : e.events()) if (meFree >= 0 && preso && ev.attacker == 0 && !ev.blocked) emendou = true;
            if (rivalFree >= 0 && (emendou || i > at + 90)) break;
        }
        assertTrue(id + " leu o golpe", countered);
        return new int[] {rivalFree - meFree, emendou ? 1 : 0};
    }

    @Test public void theCounterLeavesTheRivalStunnedAndTheFollowUpConnects() {
        for (String id : new String[] {"p02", "p11"}) {
            int[] r = contraGolpe(id);
            assertTrue(id + ": vantagem " + r[0], r[0] >= 25);
            assertTrue(id + ": correu e emendou o L no atordoamento", r[1] == 1);
        }
    }
}
