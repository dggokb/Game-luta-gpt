package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.Random;
import org.junit.Test;

/**
 * O S1 em três do Kōga (19/11, a Garra do tigre, ideia do dono): ↓↘→ + botão é o 1º corte (S1A);
 * um botão dentro dele emenda o 2º corte, o X (a continuação SG); outro botão dentro do X emenda o
 * S1 de projétil, o X lançado. Sem apertar, cada etapa volta à guarda. A CPU usa a sequência.
 */
public class GarraDoTigreTest {
    private static final CharacterDefinition KOGA = GeneratedCharacters.get("p03");
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");

    private static CombatEngineTest.Sim luta(float gap) {
        return new CombatEngineTest.Sim(KOGA, BASE, 600f, 600f + gap);
    }

    /** Aperta um botão e espera o golpe {@code id} começar (até {@code limite} quadros). */
    private static boolean apertaAte(CombatEngineTest.Sim s, String id, int limite) {
        for (int t = 0; t < limite; t++) {
            s.in[0].light = true;
            s.step();
            s.in[0].light = false;
            if (s.f(0).attacking() && id.equals(s.f(0).attack.id)) return true;
        }
        return false;
    }

    @Test public void tresApertosAteOXLancado() {
        CombatEngineTest.Sim s = luta(600f);
        s.in[0].special = "M";
        s.step();
        s.in[0].special = null;
        assertTrue("↓↘→ é o 1º corte", s.f(0).attacking() && CancelSystem.ABERTURA.equals(s.f(0).attack.id));
        assertTrue("o 2º corte (o X) emenda", apertaAte(s, CancelSystem.FOLLOW_UP, 60));
        assertTrue("o X lançado emenda", apertaAte(s, "S", 60));
        boolean saiu = false;
        for (int t = 0; t < 30 && !saiu; t++) {
            s.step();
            for (Projectile p : s.engine.energyProjectiles) saiu |= p.ownerIndex == 0;
        }
        assertTrue("o X voa (o projétil do S1)", saiu);
    }

    @Test public void semApertarOPrimeiroCorteVoltaAGuarda() {
        CombatEngineTest.Sim s = luta(600f);
        s.in[0].special = "M";
        s.step();
        s.in[0].special = null;
        for (int t = 0; t < 90; t++) s.step();
        assertEquals(CombatFighter.Status.NEUTRAL, s.f(0).status);
        assertTrue("sem projétil", s.engine.energyProjectiles.isEmpty());
    }

    @Test public void oMEmendaNoPrimeiroCorte() {
        CombatEngineTest.Sim s = luta(110f);
        s.in[0].medium = true;
        s.step();
        s.in[0].medium = false;
        boolean abriu = false;
        for (int t = 0; t < 40 && !abriu; t++) {
            if (s.f(0).outcome != CombatFighter.Outcome.NONE) s.in[0].special = "M";
            s.step();
            s.in[0].special = null;
            abriu = s.f(0).attacking() && CancelSystem.ABERTURA.equals(s.f(0).attack.id);
        }
        assertTrue("do M acertado sai o 1º corte", abriu);
    }

    /** A CPU (nível 8) contra um jogador parado: ela solta o X lançado pela sequência. */
    @Test public void aCpuVaiAteOX() {
        int lancados = 0;
        for (long seed = 1; seed <= 6 && lancados == 0; seed++) {
            CombatEngine e = new CombatEngine(new FighterState(BASE, "P1"), new FighterState(KOGA, "CPU"),
                400f, 900f, new CombatConfig());
            AiController cpu = new AiController(new OpponentAi(new Random(seed)), new Random(seed * 31 + 7));
            cpu.setEnabled(true);
            cpu.setLevel(8);
            FighterInput p = new FighterInput(), out = new FighterInput();
            AttackDefinition antes = null;
            for (int fr = 0; fr < 60 * 25; fr++) {
                e.fighter(0).state.restoreLife();
                e.fighter(1).state.restoreLife();
                cpu.fill(e, 1, out);
                e.step(p, out);
                CombatFighter c = e.fighter(1);
                AttackDefinition agora = c.attacking() ? c.attack : null;
                if (agora != null && agora != antes && "S".equals(agora.id) && antes != null &&
                    CancelSystem.FOLLOW_UP.equals(antes.id)) lancados++;
                antes = agora;
            }
        }
        assertTrue("a CPU lançou o X pela sequência", lancados >= 1);
    }
}
