package com.gamelutagpt;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Test;

/**
 * A CPU no combo aéreo (15/11, o dono: "a CPU já tentou fazer combo aéreo mas sempre se perde").
 * Medido: depois do lançador ela esperava o 2H acabar para o super pulo (o rival já no alto), dava
 * o pulo no ar depois do jM (ela subia, o rival caía) e o jL seguinte passava no vazio; e o acerto
 * contava da pausa do impacto em diante, então a janela do cancelamento fechava antes da reação.
 * Agora: o super pulo cancela o lançador, a reação conta do acerto e a rota fecha com o especial
 * no ar de cada um ({@link ComboBook#FECHO_NO_AR}).
 */
public class CpuComboNoArTest {
    @After public void religa() {
        AiController.superPuloNoCancelamento = true;
        AiController.acertoNaPausa = true;
    }

    /** {lançamentos, os que fecharam acertando o especial no ar}. */
    private static int[] medir(String id, long seeds) {
        String fecho = ComboBook.FECHO_NO_AR.get(id)[1];
        int lancou = 0, fechou = 0;
        for (long seed = 1; seed <= seeds; seed++) {
            CombatConfig cfg = new CombatConfig();
            cfg.justDefenseFrames = -1;
            CombatEngine e = new CombatEngine(new FighterState(GeneratedCharacters.defaultCharacter(), "P1"),
                new FighterState(GeneratedCharacters.get(id), "CPU"), 600f, 760f, cfg);
            AiController cpu = new AiController(new OpponentAi(new Random(seed)), new Random(seed * 31 + 7));
            cpu.setEnabled(true);
            cpu.setLevel(8);
            FighterInput p = new FighterInput(), out = new FighterInput();
            int desde = -1;
            List<String> seq = new ArrayList<>();
            for (int f = 0; f < 60 * 60; f++) {
                e.fighter(0).state.life = e.fighter(0).state.profile.maxLife;
                e.fighter(1).state.life = e.fighter(1).state.profile.maxLife;
                cpu.fill(e, 1, out);
                e.step(p, out);
                for (CombatEngine.HitEvent h : e.events()) {
                    if (h.attacker != 1 || h.blocked) continue;
                    if (desde < 0 && "2H".equals(h.moveId)) {
                        desde = f;
                        lancou++;
                        seq.clear();
                    } else if (desde >= 0) {
                        seq.add(h.moveId);
                    }
                }
                if (desde >= 0 && f - desde >= 150) {
                    if (seq.contains("jM") && seq.contains(fecho)) fechou++;
                    desde = -1;
                }
            }
        }
        return new int[] {lancou, fechou};
    }

    @Test public void depoisDoLancadorElaFechaNoArComOEspecial() {
        for (String id : new String[] {"player_base", "p03", "p05", "p10"}) {
            int[] agora = medir(id, 2);
            assertTrue(id + " lançou", agora[0] >= 2);
            assertTrue(id + ": fechou no ar " + agora[1] + " de " + agora[0], agora[1] * 10 >= agora[0] * 6);
        }
    }

    @Test public void antesElaSePerdia() {
        AiController.superPuloNoCancelamento = false;
        AiController.acertoNaPausa = false;
        int[] antes = medir("p03", 2);
        religa();
        int[] agora = medir("p03", 2);
        assertTrue("antes " + antes[1] + "/" + antes[0] + ", agora " + agora[1] + "/" + agora[0],
            agora[1] * antes[0] > antes[1] * agora[0] + agora[0] * antes[0] / 3);
    }
}
