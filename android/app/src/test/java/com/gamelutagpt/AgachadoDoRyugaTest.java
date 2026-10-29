package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * O Ryūga agachado apanha pelo corpo desenhado (etapa H8a): o agachar dele não tinha caixas
 * medidas e virava a caixa genérica (±36 px), e o 2M dado na ponta encostava o pé no pé dele sem
 * acertar (T25: 33 casos). As caixas vêm do último quadro do CROUCH, reduzido como o jogo mostra.
 */
public class AgachadoDoRyugaTest {
    private static final CharacterDefinition RYUGA = GeneratedCharacters.get("player_base");

    @Test public void theCrouchHasMeasuredBodyBands() {
        float[] corpo = RYUGA.animations.get(SpriteStates.CROUCH).postura;
        assertNotNull("o agachar tem caixas medidas", corpo);
        assertTrue("os pés vão além da caixa genérica", corpo.length >= 12 && corpo[1] > RYUGA.fighter.body.halfWidth + 20f);
    }

    /** {@code atacante} dá o 2M a {@code distancia} px do Ryūga agachado parado: acertou? */
    private static boolean lowMediumHits(String atacante, float distancia) {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get(atacante), RYUGA, 600f, 600f + distancia);
        for (int i = 0; i < 12; i++) {
            s.in[1].direction = 3;
            s.step();
        }
        assertTrue("o Ryūga está agachado", s.f(1).crouchingBody());
        for (int i = 0; i < 60; i++) {
            s.in[0].direction = 2;
            if (i == 0) s.in[0].medium = true;
            s.in[1].direction = 3;
            s.step();
            for (CombatEngine.HitEvent h : s.engine.events()) if (h.attacker == 0) return true;
        }
        return false;
    }

    @Test public void aLowMediumAtTheTipHitsTheCrouchingRyuga() {
        assertTrue("2M da p02 a 190 px", lowMediumHits("p02", 190f));
        assertTrue("2M do p13 a 310 px", lowMediumHits("p13", 310f));
        // H2 (experimento B): o 2M do p09 tem alcanceMaximo 190; a ponta pega a 250 px, não mais a 280.
        assertTrue("2M do p09 a 250 px", lowMediumHits("p09", 250f));
    }

    @Test public void theP09LowMediumStopsAtItsMaximumReach() {
        // H2: alcanceMaximo 220 → 190. Com 220, o 2M ainda pegava o Ryūga agachado a 280 px.
        assertFalse("2M do p09 a 280 px", lowMediumHits("p09", 280f));
    }
}
