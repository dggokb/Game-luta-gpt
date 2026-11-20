package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.Random;
import org.junit.Test;

/**
 * A CPU usa o → + H (o golpe de comando) na punição (19/11, "a CPU acompanha tudo"): a ombrada do
 * Kōga avança e pega o erro de mais longe que o H parado. E o → + M (golpe alto) e o → + H do Kōga
 * existem no pacote, com o golpe alto marcado como alto. Correndo para dentro, ela termina a corrida
 * no golpe de corrida (o H correndo: a voadora do Kōga, o carrinho da Yin-lu...).
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

    /**
     * O jogador erra um golpe lento de longe (fora do alcance do H dela, dentro do da ombrada), com a
     * CPU livre e parada; conta as vezes em que ela pune com o → + H.
     */
    @Test public void aCpuPuneDeLongeComAOmbrada() {
        int comOmbrada = 0, tentativas = 0;
        for (long seed = 1; seed <= 200 && comOmbrada == 0; seed++) {
            CombatEngine e = new CombatEngine(new FighterState(WHIFFER, "P1"), new FighterState(KOGA, "CPU"),
                500f, 740f, new CombatConfig());
            AiController cpu = new AiController(new OpponentAi(new Random(seed)), new Random(seed * 31 + 7));
            cpu.setEnabled(true);
            cpu.setLevel(8);
            FighterInput p = new FighterInput(), out = new FighterInput();
            boolean livre = false;
            for (int i = 0; i < 600 && !livre; i++) {
                cpu.fill(e, 1, out);
                e.step(p, out);
                p.clearPresses();
                CombatFighter a = e.fighter(0), c = e.fighter(1);
                float gap = Math.abs(a.x - c.x);
                livre = a.canAct() && c.canAct() && a.grounded && c.grounded && !c.attacking() && gap >= 200f && gap <= 270f;
            }
            if (!livre) continue;
            tentativas++;
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
        assertTrue("tentativas válidas: " + tentativas, tentativas >= 3);
        assertTrue("a CPU puniu com a ombrada", comOmbrada >= 1);
    }

    /** Quantos golpes de corrida a CPU {@code id} solta em 25 s contra um jogador parado, de longe. */
    static int golpesDeCorrida(String id) {
        int n = 0;
        for (long seed = 1; seed <= 8; seed++) {
            CombatEngine e = new CombatEngine(new FighterState(BASE, "P1"), new FighterState(GeneratedCharacters.get(id), "CPU"),
                300f, 1000f, new CombatConfig());
            AiController cpu = new AiController(new OpponentAi(new Random(seed)), new Random(seed * 31 + 7));
            cpu.setEnabled(true);
            cpu.setLevel(8);
            FighterInput p = new FighterInput(), out = new FighterInput();
            Object ultimo = null;
            for (int fr = 0; fr < 60 * 25; fr++) {
                e.fighter(0).state.restoreLife();
                e.fighter(1).state.restoreLife();
                cpu.fill(e, 1, out);
                e.step(p, out);
                CombatFighter c = e.fighter(1);
                if (c.attacking() && CancelSystem.RUN_HEAVY.equals(c.attack.id) && c.attack != ultimo) n++;
                ultimo = c.attacking() ? c.attack : null;
            }
        }
        return n;
    }

    /**
     * Os que correm longe (o Ryūga dá corridas curtas, que no neutro acabam fora do alcance da
     * Ombrada; perto, ela quase sempre está correndo para ficar em cima de quem caiu).
     */
    @Test public void aCpuTerminaACorridaNoGolpeDeCorrida() {
        for (String id : new String[] {"p03", "p02", "p15", "p16"}) {
            assertNotNull(id + " tem golpe de corrida", GeneratedCharacters.get(id).attack(CancelSystem.RUN_HEAVY));
            int n = golpesDeCorrida(id);
            assertTrue(id + ": golpes de corrida da CPU " + n, n >= 1);
        }
    }
}
