package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.Random;
import org.junit.Test;

/**
 * A CPU usa o → + H (o golpe de comando) na punição (19/11, "a CPU acompanha tudo"): a ombrada do
 * Kōga avança e pega o erro de mais longe que o H parado. E o → + M (golpe alto) e o → + H do Kōga
 * existem no pacote, com o golpe alto marcado como alto.
 */
public class CpuGolpeDeComandoTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition KOGA = GeneratedCharacters.get("p03");

    /** Golpe do jogador que nunca alcança e demora para recuperar (o mesmo do CpuTest). */
    private static final CharacterDefinition WHIFFER = semDesenho(CombatEngineTest.withMoves(BASE,
        new AttackDefinition.Builder("H", AttackDefinition.Kind.NORMAL).damage(100)
            .frames(10, 3, 40).stun(12, 8, 0).windows(null, null, null)
            .pushback(0f, 0f).reach(10f, 78f).build()), "H");

    private static CharacterDefinition semDesenho(CharacterDefinition c, String id) {
        java.util.Map<String, CharacterDefinition.Move> moves = new java.util.LinkedHashMap<>(c.moves);
        CharacterDefinition.Move m = moves.get(id);
        moves.put(id, new CharacterDefinition.Move(id, c.animations.get("IDLE"), m.pose, m.attack));
        return new CharacterDefinition(c.id, c.displayName, c.profile, c.artFacing, c.visualStandHeight,
            c.visualCrouchHeight, c.fighter, c.animations, moves, c.specialAnimations, c.specials);
    }

    @Test public void oKogaTemGolpeAltoEOmbrada() {
        assertNotNull("→ + M", KOGA.attack(GolpeAlto.BINDING));
        assertTrue("o → + M é golpe alto", KOGA.attack(GolpeAlto.BINDING).overhead);
        AttackDefinition ombrada = KOGA.attack(CancelSystem.COMMAND_HEAVY);
        assertNotNull("→ + H", ombrada);
        assertNotNull("a ombrada avança", ombrada.mechanics);
        assertTrue("o L emenda na ombrada", KOGA.attack("L").cancelsInto(CancelSystem.COMMAND_HEAVY));
    }

    /** O jogador erra um golpe lento de longe; conta as vezes em que a CPU pune com o → + H. */
    @Test public void aCpuPuneDeLongeComAOmbrada() {
        int comOmbrada = 0;
        for (long seed = 1; seed <= 60 && comOmbrada == 0; seed++) {
            for (float gap : new float[] {170f, 200f, 230f}) {
                CombatEngine e = new CombatEngine(new FighterState(WHIFFER, "P1"), new FighterState(KOGA, "CPU"),
                    500f, 500f + gap, new CombatConfig());
                AiController cpu = new AiController(new OpponentAi(new Random(seed)), new Random(seed * 31 + 7));
                cpu.setEnabled(true);
                cpu.setLevel(8);
                FighterInput p = new FighterInput(), out = new FighterInput();
                for (int i = 0; i < 20; i++) { cpu.fill(e, 1, out); e.step(p, out); p.clearPresses(); }
                if (e.fighter(1).attacking() || !e.fighter(0).canAct()) continue;
                p.heavy = true;
                for (int i = 0; i < 70; i++) {
                    cpu.fill(e, 1, out);
                    e.step(p, out);
                    p.clearPresses();
                    if (e.fighter(1).attacking() && CancelSystem.COMMAND_HEAVY.equals(e.fighter(1).attack.id) &&
                        cpu.lastReason != null && cpu.lastReason.startsWith("punir")) {
                        comOmbrada++;
                        break;
                    }
                }
            }
        }
        assertTrue("a CPU puniu com a ombrada", comOmbrada >= 1);
    }
}
