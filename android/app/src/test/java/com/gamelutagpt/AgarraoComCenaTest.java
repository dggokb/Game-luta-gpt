package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Agarrões com personalidade (o dono, 18/11): com {@link CenaDoAgarrao} o rival segue o desenho de
 * quem agarra (preso, no ar, cravado), o dano entra no impacto da cena (não na hora em que pega) e ele
 * fica deitado onde caiu; quem agarrou volta à guarda no fim da cena, com o rival ainda no chão.
 */
public class AgarraoComCenaTest {
    private static final String[] COM_CENA = {"player_base", "p02", "p03", "p15", "p16"};

    @Test public void cadaUmJogaDoSeuJeito() {
        for (String id : COM_CENA) {
            CharacterDefinition c = GeneratedCharacters.get(id);
            CenaDoAgarrao cena = CenaDoAgarrao.de(c);
            assertNotNull(id, cena);
            CharacterDefinition rival = GeneratedCharacters.get("p03");
            float xa = 600f;
            CombatEngine e = new CombatEngine(new FighterState(c, "P1"), new FighterState(rival, "CPU"),
                xa, xa + c.fighter.body.halfWidth + rival.fighter.body.halfWidth + 10f, new CombatConfig());
            FighterInput[] in = {new FighterInput(), new FighterInput()};
            int vida = e.fighter(1).state.life;
            in[0].grab = true;
            int quadroDoDano = -1, inicio = -1, alturaMax = 0;
            boolean deitado = false, presoAteSoltar = true;
            for (int t = 0; t < 400; t++) {
                e.step(in[0], in[1]);
                in[0].clearPresses();
                CombatFighter a = e.fighter(0), d = e.fighter(1);
                if (inicio < 0 && a.status == CombatFighter.Status.THROW && a.throwPhase == CombatFighter.THROW_EXECUTE) inicio = t;
                if (inicio >= 0 && quadroDoDano < 0 && d.state.life < vida) quadroDoDano = a.throwFrame;
                if (inicio >= 0) alturaMax = Math.max(alturaMax, Math.round(Arena.GROUND_Y - d.y));
                if (inicio >= 0 && a.status == CombatFighter.Status.THROW && a.throwFrame < cena.solta &&
                    d.status != CombatFighter.Status.THROWN) presoAteSoltar = false;
                if (d.status == CombatFighter.Status.KNOCKDOWN && d.knockdownFrame >= e.config.knockdownFallFrames) deitado = true;
                if (inicio >= 0 && a.status == CombatFighter.Status.NEUTRAL) {
                    assertEquals(id + ": o rival ainda no chão quando ela volta", CombatFighter.Status.KNOCKDOWN, d.status);
                    break;
                }
            }
            assertTrue(id + ": agarrou e jogou", inicio >= 0);
            assertEquals(id + ": o dano entra no impacto da cena", cena.acerto, quadroDoDano);
            assertTrue(id + ": o rival fica preso na cena até soltar", presoAteSoltar);
            assertTrue(id + ": ele sai do chão (" + alturaMax + " px)", alturaMax >= 40);
            assertTrue(id + ": deitado onde caiu", deitado);
        }
    }

    @Test public void quemNaoTemCenaJogaComoSempre() {
        assertNull(CenaDoAgarrao.de(GeneratedCharacters.get("p04")));
    }

    /** O Kōga joga longe (19/11): o rival cai a mais de 250 px de quem agarrou. */
    @Test public void oKogaJogaLonge() {
        CharacterDefinition c = GeneratedCharacters.get("p03"), rival = GeneratedCharacters.get("p02");
        CombatEngine e = new CombatEngine(new FighterState(c, "P1"), new FighterState(rival, "CPU"),
            600f, 600f + c.fighter.body.halfWidth + rival.fighter.body.halfWidth + 10f, new CombatConfig());
        FighterInput[] in = {new FighterInput(), new FighterInput()};
        in[0].grab = true;
        float longe = 0f;
        for (int t = 0; t < 300; t++) {
            e.step(in[0], in[1]);
            in[0].clearPresses();
            if (e.fighter(1).status == CombatFighter.Status.KNOCKDOWN) longe = Math.max(longe, Math.abs(e.fighter(1).x - e.fighter(0).x));
        }
        assertTrue("caiu a " + longe + " px", longe >= 250f);
    }
}
