package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.Random;
import org.junit.Test;

/**
 * O S1 em três do Kōga (19/11, a Garra do tigre, ideia do dono): ↓↘→ + botão é o 1º corte (S1A);
 * um botão dentro dele emenda o 2º corte, o X (a continuação SG); outro botão dentro do X emenda o
 * S1 de projétil, o X lançado. Sem apertar, cada etapa volta à guarda. A CPU usa a sequência.
 * 20/11 (o dono: "tem q respeitar o tempo da animação"): cada etapa toca no tempo do vídeo e a
 * seguinte só sai no fim da pose segurada, por mais rápido que se aperte; acertada, a sequência
 * inteira é combo.
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

    /** Apertando o botão em todo quadro, o X só sai no fim da pose segurada do 1º corte. */
    @Test public void apertarRapidoNaoAcelera() {
        CombatEngineTest.Sim s = luta(600f);
        s.in[0].special = "M";
        s.step();
        s.in[0].special = null;
        int ultimo = -1;
        for (int t = 0; t < 80; t++) {
            CombatFighter f = s.f(0);
            if (f.attacking() && CancelSystem.ABERTURA.equals(f.attack.id)) ultimo = f.attackFrame;
            if (f.attacking() && CancelSystem.FOLLOW_UP.equals(f.attack.id)) break;
            s.in[0].light = true;
            s.step();
            s.in[0].light = false;
        }
        AttackDefinition abertura = KOGA.attack(CancelSystem.ABERTURA);
        assertTrue("o X saiu", s.f(0).attacking() && CancelSystem.FOLLOW_UP.equals(s.f(0).attack.id));
        assertTrue("o 1º corte tocou até a pose segurada (" + ultimo + ")", ultimo >= abertura.whiffWindow.start - 1);
        assertTrue("a janela abre com as marcas formadas",
            abertura.whiffWindow.start >= abertura.startupFrames + abertura.activeFrames);
    }

    /**
     * O dono, 20/11: a marca fica na tela enquanto dá para apertar de novo; sem apertar, some. A
     * janela da continuação cobre a pose segurada inteira (as marcas paradas na frente dele) e
     * fecha quando ele volta à guarda.
     */
    @Test public void aMarcaFicaEnquantoDaParaApertar() {
        segura(CancelSystem.ABERTURA, 7, 15);
        segura(CancelSystem.FOLLOW_UP, 5, 15);
    }

    /** Do começo ao fim da janela, o desenho é a pose segurada (quadros {@code de} a {@code ate} do clipe). */
    private static void segura(String id, int de, int ate) {
        CharacterDefinition.Move m = KOGA.moves.get(id);
        AttackDefinition a = m.attack;
        for (int f = a.whiffWindow.start; f <= a.whiffWindow.end; f++) {
            int q = m.animation.frame(m.animationTime((f + 0.5f) * CombatConfig.DT), 0f);
            assertTrue(id + ": no quadro " + f + " da janela o desenho é o " + q + " (marcas de " + de + " a " + ate + ")",
                q >= de && q <= ate);
        }
        int depois = m.animation.frame(m.animationTime((a.whiffWindow.end + 4.5f) * CombatConfig.DT), 0f);
        assertTrue(id + ": fechada a janela, ele volta à guarda (" + depois + ")", depois > ate);
    }

    /** De perto, os três acertam sem o rival sair do atordoamento. */
    @Test public void aSequenciaAcertadaEhCombo() {
        CombatEngineTest.Sim s = luta(150f);
        s.in[0].special = "M";
        s.step();
        s.in[0].special = null;
        boolean acertou = false, soltou = false, xAcertou = false;
        for (int t = 0; t < 200 && !xAcertou; t++) {
            s.in[0].light = true;
            s.step();
            s.in[0].light = false;
            for (CombatEngine.HitEvent e : s.engine.events()) {
                if (e.attacker == 0 && !e.blocked) {
                    acertou = true;
                    if (e.projectile) xAcertou = true;
                }
            }
            if (acertou && !xAcertou) {
                CombatFighter d = s.f(1);
                assertTrue("o rival ficou preso no quadro " + t + " (" + d.status + ")",
                    d.status != CombatFighter.Status.NEUTRAL);
            }
        }
        assertTrue("o 1º corte acertou", acertou);
        assertTrue("o X lançado acertou", xAcertou);
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
