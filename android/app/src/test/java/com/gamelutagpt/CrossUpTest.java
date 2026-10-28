package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Crossup: o jM pega também atrás de quem pula, logo depois de passar por cima do rival. Quem
 * defende segura para o lado contrário ao de onde o golpe veio: segurar o lado antigo não segura.
 */
public class CrossUpTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");

    /** Resultado de um pulo para frente por cima do rival com o jM apertado no quadro {@code press}. */
    private static final class Pulo {
        boolean acertou, defendeu, cruzado;
        float empurraoX;
    }

    /** P1 pula para frente por cima do P2 parado e aperta M no quadro {@code press}; P2 segura {@code hold} (absoluto, 0 = nada). */
    private static Pulo pulo(String golpe, int press, int hold) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(BASE, BASE, 500f, 670f);
        Pulo r = new Pulo();
        s.in[0].jump = true;
        s.in[0].direction = 1;
        for (int t = 0; t < 70; t++) {
            s.in[0].direction = s.f(0).grounded && t > 3 ? 0 : 1;
            if (t == press) {
                if ("M".equals(golpe)) s.in[0].medium = true; else s.in[0].heavy = true;
            }
            // A defesa só quando o pulo já está em cima (andar antes mudaria o encontro).
            // O lado antigo (1) segurado com o pulo chegando; o novo (5), só depois de ele passar.
            boolean passou = s.f(0).x > s.f(1).x;
            s.in[1].direction = hold == 5 ? (passou ? 5 : 0) : Math.abs(s.f(0).x - s.f(1).x) < 60f ? hold : 0;
            boolean atras = (s.f(1).x - s.f(0).x) * s.f(0).facing < 0f;
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) {
                if (e.attacker != 0) continue;
                r.cruzado = atras;
                if (e.blocked) r.defendeu = true; else r.acertou = true;
            }
            if (r.acertou || r.defendeu) {
                // O empurrão sai depois da pausa do impacto: para que lado o rival foi.
                float naHora = s.f(1).x, lado = Math.signum(s.f(1).x - s.f(0).x);
                s.in[1].direction = 0;
                s.steps(20);
                r.empurraoX = (s.f(1).x - naHora) * lado;
                break;
            }
        }
        return r;
    }

    /** O quadro em que o jM pega o P2 por trás, ou -1. */
    private static int quadroDoCrossup() {
        for (int press = 6; press < 45; press++) {
            Pulo r = pulo("M", press, 0);
            if (r.acertou && r.cruzado) return press;
        }
        return -1;
    }

    @Test public void oJmPegaPorTrasDepoisDePassar() {
        int press = quadroDoCrossup();
        assertTrue("algum quadro do jM pega por trás", press >= 0);
        Pulo r = pulo("M", press, 0);
        assertTrue(r.acertou);
        assertTrue("empurra o rival para longe de quem bateu (para trás dele): " + r.empurraoX, r.empurraoX > 0f);
    }

    @Test public void segurarOLadoAntigoNaoDefende() {
        int press = quadroDoCrossup();
        // P2 começa olhando para a esquerda (P1 à esquerda): o "trás" antigo é a direita (1).
        Pulo antigo = pulo("M", press, 1);
        assertTrue("segurando o lado antigo, apanha", antigo.acertou && !antigo.defendeu);
        // Segurando para o lado novo: defende, ou anda para longe antes de o golpe encostar.
        Pulo novo = pulo("M", press, 5);
        assertFalse("segurando para o outro lado, não apanha", novo.acertou);
    }

    @Test public void soOJmCruza() {
        for (int press = 6; press < 45; press++) {
            Pulo r = pulo("H", press, 0);
            assertFalse("o jH não pega por trás (quadro " + press + ")", r.acertou && r.cruzado);
        }
    }
}
