package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.Random;
import org.junit.Test;

/**
 * A CPU com o Super de contra-golpe da Yin-lu (Espelho do Lago, 18/11: "a CPU sempre tem que ser
 * atualizada com tudo que fazemos"): ele fecha o combo do caderno (no combo ele já sai no desvio e
 * nas palmas), e no neutro ela o usa para ler o golpe que vem e o poder que vem.
 */
public class CpuSuperDeContraTest {
    private static final CharacterDefinition YINLU = GeneratedCharacters.get("p02");
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");

    /** Contra um jogador que não defende, com barra: o Super dela entra no meio do combo (as palmas acertam). */
    @Test public void noComboOSuperDelaEmenda() {
        int emComboSuper = 0;
        for (long seed = 1; seed <= 8; seed++) {
            CombatEngine e = new CombatEngine(new FighterState(BASE, "P1"), new FighterState(YINLU, "CPU"),
                500f, 640f, new CombatConfig());
            AiController cpu = new AiController(new OpponentAi(new Random(seed)), new Random(seed * 31 + 7));
            cpu.setEnabled(true);
            cpu.setLevel(8);
            cpu.setFullKit(true);
            FighterInput p = new FighterInput(), out = new FighterInput();
            for (int fr = 0; fr < 60 * 30; fr++) {
                e.fighter(1).state.superMeter = 2 * CombatConfig.METER_PER_BAR;
                e.fighter(0).state.restoreLife();
                cpu.fill(e, 1, out);
                e.step(p, out);
                for (CombatEngine.HitEvent h : e.events()) {
                    ComboSession sess = e.session(0);
                    if (h.attacker == 1 && !h.blocked && "SUPER".equals(h.moveId) && sess != null && sess.hitCount >= 3) emComboSuper++;
                }
            }
        }
        assertTrue("as palmas do Super acertaram no combo: " + emComboSuper, emComboSuper >= 1);
    }

    /** Uma luta curta: o jogador faz {@code acao} de tempos em tempos; conta os contra-golpes do Super dela. */
    private static int contras(float gap, boolean bola) {
        int contras = 0;
        for (long seed = 1; seed <= 8; seed++) {
            CombatConfig cfg = new CombatConfig();
            CombatEngine e = new CombatEngine(new FighterState(BASE, "P1"), new FighterState(YINLU, "CPU"),
                500f, 500f + gap, cfg);
            AiController cpu = new AiController(new OpponentAi(new Random(seed)), new Random(seed * 31 + 7));
            cpu.setEnabled(true);
            cpu.setLevel(8);
            cpu.setFullKit(true);
            FighterInput p = new FighterInput(), out = new FighterInput();
            for (int fr = 0; fr < 60 * 20; fr++) {
                e.fighter(1).state.superMeter = 2 * CombatConfig.METER_PER_BAR;
                e.fighter(0).state.restoreLife();
                e.fighter(1).state.restoreLife();
                if (fr % 70 == 0 && e.fighter(0).canAct()) {
                    if (bola) p.special = "M"; else p.heavy = true;
                }
                cpu.fill(e, 1, out);
                e.step(p, out);
                p.clearPresses();
                if (e.cues().contains("COUNTER") && e.fighter(1).attacking() &&
                    e.fighter(1).attack.kind == AttackDefinition.Kind.SUPER) contras++;
            }
        }
        return contras;
    }

    @Test public void noNeutroElaLeOGolpeQueVemComOSuper() {
        int n = contras(150f, false);
        assertTrue("o Super leu golpes do rival: " + n, n >= 1);
    }

    @Test public void noNeutroElaLeOPoderQueVemComOSuper() {
        int n = contras(520f, true);
        assertTrue("o Super leu poderes do rival: " + n, n >= 1);
    }
}
