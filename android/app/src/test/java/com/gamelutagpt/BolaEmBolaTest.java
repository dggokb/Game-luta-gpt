package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Bola de energia de perto: o acerto empurra longe e bola atrás de bola não emenda (antes o
 * acerto deixava tempo de soltar outra e virava combo sem fim). Golpe que termina em bola
 * continua combo.
 */
public class BolaEmBolaTest {
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();
    private static final String[] COM_BOLA = {"player_base", "p02", "p04", "p05", "p06", "p08", "p10"};

    /** Solta bola assim que pode, colado no boneco; devolve o maior combo e quantas bolas ele defendeu. */
    private static int[] spam(String id, String forca, float gap) {
        FighterState p = new FighterState(GeneratedCharacters.get(id), "P1");
        FighterState d = new FighterState(NPC, "D");
        CombatEngine e = new CombatEngine(p, d, 500f, 500f + gap, new CombatConfig());
        FighterInput[] in = {new FighterInput(), new FighterInput()};
        int maior = 0, acertos = 0;
        for (int i = 0; i < 400; i++) {
            if (e.fighter(0).canAct()) in[0].special = forca;
            p.superMeter = 0;
            e.step(in[0], in[1]);
            in[0].clearPresses();
            for (CombatEngine.HitEvent h : e.events()) if (h.attacker == 0 && h.projectile && !h.blocked) acertos++;
            ComboSession s = e.session(1);
            if (s != null && s.active) maior = Math.max(maior, s.hitCount);
        }
        return new int[] {maior, acertos};
    }

    @Test public void fireballAfterFireballNeverCombos() {
        for (String id : COM_BOLA) {
            for (String forca : new String[] {"L", "M", "H"}) {
                for (float gap = 60f; gap <= 300f; gap += 40f) {
                    int[] r = spam(id, forca, gap);
                    assertTrue(id + " " + forca + " acertou", r[1] > 0);
                    assertEquals(id + " " + forca + " de " + gap + ": bola emendou em bola", 1, r[0]);
                }
            }
        }
    }

    @Test public void aFireballHitPushesFar() {
        FighterState p = new FighterState(GeneratedCharacters.get("player_base"), "P1");
        FighterState d = new FighterState(NPC, "D");
        CombatEngine e = new CombatEngine(p, d, 500f, 560f, new CombatConfig());
        FighterInput[] in = {new FighterInput(), new FighterInput()};
        in[0].special = "L";
        float before = Float.NaN;
        for (int i = 0; i < 60; i++) {
            e.step(in[0], in[1]);
            in[0].clearPresses();
            for (CombatEngine.HitEvent h : e.events()) if (h.projectile && Float.isNaN(before)) before = e.fighter(1).x;
        }
        assertFalse(Float.isNaN(before));
        assertTrue("empurrou " + (e.fighter(1).x - before),
            e.fighter(1).x - before >= e.config.projectileHitPushback - 1f);
    }

    /** O golpe que cancela na bola ainda emenda: a regra é só bola em cima de bola. */
    @Test public void aNormalCancelledIntoTheFireballStillCombos() {
        FighterState p = new FighterState(GeneratedCharacters.get("player_base"), "P1");
        FighterState d = new FighterState(NPC, "D");
        CombatEngine e = new CombatEngine(p, d, 500f, 590f, new CombatConfig());
        FighterInput[] in = {new FighterInput(), new FighterInput()};
        in[0].medium = true;
        boolean pressed = false;
        int maior = 0;
        for (int i = 0; i < 90; i++) {
            e.step(in[0], in[1]);
            in[0].clearPresses();
            for (CombatEngine.HitEvent h : e.events()) {
                if (h.attacker == 0 && !h.projectile && !pressed) {
                    pressed = true;
                    in[0].special = "L";
                }
            }
            ComboSession s = e.session(1);
            if (s != null && s.active) maior = Math.max(maior, s.hitCount);
        }
        assertEquals("M > bola", 2, maior);
    }
}
