package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Assist de corpo a corpo (auditoria T17, etapa G2a): corre até onde o golpe pega o rival, no
 * máximo {@link CombatConfig#assistMeleeAhead} à frente do titular; a investida avança, o mergulho
 * cai do alto. Antes, p12, p13 e p15 só acertavam o rival colado e o p14 não acertava nada.
 */
public class AssistCorpoACorpoTest {
    private static final CharacterDefinition NPC = GeneratedCharacters.opponentCharacter();

    /** Titular player_base com {@code partner} de parceiro, contra o boneco parado a {@code gap} px. */
    private static final class Match {
        final CombatEngine engine;
        final FighterInput[] in = {new FighterInput(), new FighterInput()};

        Match(String partner, float gap) {
            FighterState point = new FighterState(GeneratedCharacters.get("player_base"), "P1");
            engine = new CombatEngine(point, new FighterState(NPC, "CPU"), 600f, 600f + gap, new CombatConfig());
            engine.setTeam(0, point, new FighterState(GeneratedCharacters.get(partner), "P2"));
        }

        CombatFighter f(int i) { return engine.fighter(i); }
        TeamSystem.Side team() { return engine.team(0); }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
        }

        /** Chama o assist; devolve o acerto do assist no boneco (ou null em 150 quadros). */
        CombatEngine.HitEvent assistHit() {
            in[0].assist = true;
            step();
            assertNotNull("o assist entrou", team().assist);
            for (int i = 0; i < 150 && team().assist != null; i++) {
                step();
                for (CombatEngine.HitEvent h : engine.events()) if (h.attacker == 0) return h;
            }
            return null;
        }
    }

    @Test public void meleeAssistsReachARivalAtMidRange() {
        for (String partner : new String[] {"p12", "p13", "p14", "p15"}) {
            Match m = new Match(partner, 300f);
            int life = m.f(1).state.life;
            assertNotNull(partner + " acerta o rival a 300 px", m.assistHit());
            assertTrue(partner, m.f(1).state.life < life);
        }
    }

    @Test public void theMeleeAssistStopsAtMostAssistMeleeAheadInFrontOfThePoint() {
        Match m = new Match("p15", 900f);
        float pointX = m.f(0).x;
        m.in[0].assist = true;
        m.step();
        float maxAhead = 0f;
        while (m.team().assist != null && m.team().assistPhase != TeamSystem.ASSIST_LEAVE) {
            maxAhead = Math.max(maxAhead, m.team().assist.x - pointX);
            m.step();
        }
        assertEquals(m.engine.config.assistMeleeAhead, maxAhead, 1f);
    }

    @Test public void theDiveAssistFallsFromAboveAndLands() {
        Match m = new Match("p14", 250f);
        m.in[0].assist = true;
        m.step();
        boolean wasHigh = false;
        CombatEngine.HitEvent hit = null;
        for (int i = 0; i < 150 && m.team().assist != null && hit == null; i++) {
            if (m.team().assist.y < Arena.GROUND_Y - 100f) wasHigh = true;
            m.step();
            for (CombatEngine.HitEvent h : m.engine.events()) if (h.attacker == 0) hit = h;
        }
        assertTrue("entra pulando", wasHigh);
        assertNotNull("o mergulho pega", hit);
        for (int i = 0; i < 150 && m.team().assist != null; i++) m.step();
        assertNull("acaba e sai", m.team().assist);
    }

    @Test public void theP15AssistLaunches() {
        Match m = new Match("p15", 250f);
        assertNotNull(m.assistHit());
        m.step();
        assertFalse("o 2H joga o rival para o alto", m.f(1).grounded);
    }

    @Test public void projectileAssistsStillStandBehindThePoint() {
        Match m = new Match("p02", 300f);
        float pointX = m.f(0).x;
        m.in[0].assist = true;
        m.step();
        while (m.team().assistPhase == TeamSystem.ASSIST_ENTER) m.step();
        assertEquals(pointX - m.engine.config.assistBehind, m.team().assist.x, 0.5f);
    }
}
