package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Agarrão pelo toque (L + M, os dois dedos até {@link PadInput#THROW_LENIENCY_FRAMES} quadros
 * de diferença) com qualquer direção segurada: parado, andando (→: o M sozinho seria o golpe
 * alto) e agachado (↓: 2L/2M). O botão que chegou primeiro não pode virar golpe no lugar do
 * agarrão. E o rival agachado é agarrado como o em pé.
 */
public class AgarraoToqueTest {

    private static boolean grabs(int heldRelative, boolean mediumFirst, int delay, boolean targetCrouching) {
        FighterState a = new FighterState(GeneratedCharacters.get("player_base"), "P1");
        FighterState d = new FighterState(GeneratedCharacters.opponentCharacter(), "CPU");
        CombatEngine e = new CombatEngine(a, d, 500f, 900f, new CombatConfig());
        FighterInput in = new FighterInput(), cpu = new FighterInput();
        cpu.direction = targetCrouching ? 3 : 0;
        // Anda até encostar.
        in.direction = 1;
        float last = -1f;
        for (int i = 0; i < 200; i++) {
            e.step(in, cpu);
            if (Math.abs(e.fighter(0).x - last) < 0.01f) break;
            last = e.fighter(0).x;
        }
        PadInput pad = new PadInput();
        pad.setDirection(MotionParser.relative(heldRelative, e.fighter(0).facing));
        for (int i = 0; i < 3; i++) {
            pad.drainInto(in);
            e.step(in, cpu);
        }
        pad.press(mediumFirst ? PadInput.Button.MEDIUM : PadInput.Button.LIGHT);
        for (int i = 0; i < 40; i++) {
            if (i == delay) pad.press(mediumFirst ? PadInput.Button.LIGHT : PadInput.Button.MEDIUM);
            pad.drainInto(in);
            e.step(in, cpu);
            if (e.fighter(1).status == CombatFighter.Status.THROWN) return true;
        }
        return false;
    }

    @Test public void lightPlusMediumGrabsWhateverDirectionIsHeld() {
        int[] held = {0, 1, 3, 2};
        String[] names = {"parado", "andando (→)", "agachado (↓)", "↘"};
        for (int k = 0; k < held.length; k++) {
            for (boolean mediumFirst : new boolean[] {false, true}) {
                for (int delay = 0; delay <= PadInput.THROW_LENIENCY_FRAMES; delay++) {
                    assertTrue(names[k] + ", " + (mediumFirst ? "M" : "L") + " primeiro, o outro " + delay +
                        " quadro(s) depois", grabs(held[k], mediumFirst, delay, true));
                }
            }
        }
    }

    @Test public void theCrouchingTargetIsGrabbedLikeTheStandingOne() {
        assertTrue(grabs(0, false, 0, false));
        assertTrue(grabs(0, false, 0, true));
    }

    /**
     * "Soco defendido, agarra" (tick throw): o rival defende agachado e o L + M apertado logo
     * depois do contato (ainda no fim do próprio soco) sai assim que o soco acaba e pega.
     */
    @Test public void aBlockedJabThenThrowGrabsTheCrouchingBlocker() {
        for (int delay = 2; delay <= 12; delay += 2) {
            FighterState a = new FighterState(GeneratedCharacters.get("player_base"), "P1");
            FighterState d = new FighterState(GeneratedCharacters.opponentCharacter(), "CPU");
            CombatEngine e = new CombatEngine(a, d, 500f, 900f, new CombatConfig());
            FighterInput in = new FighterInput(), cpu = new FighterInput();
            cpu.direction = MotionParser.relative(4, -1);   // agachado defendendo
            in.direction = 1;
            float last = -1f;
            for (int i = 0; i < 200; i++) {
                e.step(in, cpu);
                if (Math.abs(e.fighter(0).x - last) < 0.01f) break;
                last = e.fighter(0).x;
            }
            in.direction = 0;
            in.light = true;
            int contact = -1;
            boolean thrown = false, blocked = false;
            for (int f = 0; f < 80 && !thrown; f++) {
                if (contact >= 0 && f == contact + delay) in.grab = true;
                e.step(in, cpu);
                in.clearPresses();
                for (CombatEngine.HitEvent h : e.events()) {
                    if (h.defender == 1 && contact < 0) {
                        contact = f;
                        blocked = h.blocked;
                    }
                }
                thrown = e.fighter(1).status == CombatFighter.Status.THROWN;
            }
            assertTrue("o soco foi defendido", blocked);
            assertTrue("L + M " + delay + " quadros depois do soco defendido agarra", thrown);
        }
    }
}
