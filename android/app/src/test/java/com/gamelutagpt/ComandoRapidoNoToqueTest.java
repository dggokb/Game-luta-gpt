package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * ↓ ↘ → ligeiro no toque: o deslize inteiro cabe entre dois quadros (o toque chega a 120–240 por
 * segundo e era juntado num só por quadro). O direcional guarda por onde passou e o motor põe no
 * histórico, então o especial sai; antes o motor via só o → e saía o L.
 */
public class ComandoRapidoNoToqueTest {
    private static final CharacterDefinition RYUGA = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();

    /** O deslize inteiro (↓ ↘ →) e o L entre dois quadros: o caso do dedo ligeiro a 60 quadros. */
    private static AttackDefinition swipe(boolean guardaPassagem) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(RYUGA, NPC, 400f, 800f);
        PadInput pad = new PadInput();
        s.steps(10);
        pad.setDirection(3);
        pad.setDirection(2);
        pad.setDirection(1);
        pad.press(PadInput.Button.LIGHT);
        pad.drainInto(s.in[0]);
        if (!guardaPassagem) s.in[0].passouCount = 0;
        s.step();
        return s.f(0).attack;
    }

    @Test public void aFastQuarterCircleStillThrowsTheSpecial() {
        AttackDefinition a = swipe(true);
        assertNotNull(a);
        assertEquals(AttackDefinition.Kind.PROJECTILE, a.kind);
    }

    @Test public void withoutTheCrossedDiagonalItWasJustAJab() {
        AttackDefinition a = swipe(false);
        assertNotNull(a);
        assertEquals("L", a.id);
    }

    @Test public void thePadOnlyReportsWhatItCrossedSinceTheLastFrame() {
        PadInput pad = new PadInput();
        FighterInput in = new FighterInput();
        pad.setDirection(3);
        pad.drainInto(in);
        assertEquals("o ↓ partiu do neutro, que o quadro anterior já entregou", 0, in.passouCount);
        pad.setDirection(2);
        pad.setDirection(1);
        pad.drainInto(in);
        assertEquals(1, in.passouCount);
        assertEquals(2, in.passou[0]);
        assertEquals(1, in.direction);
        pad.drainInto(in);
        assertEquals(0, in.passouCount);
    }
}
