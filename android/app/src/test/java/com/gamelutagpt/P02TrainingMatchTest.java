package com.gamelutagpt;

import org.junit.Test;
import static org.junit.Assert.*;

/** P02-only player against P01 CPU, tested without decoding massive Android atlases. */
public class P02TrainingMatchTest {
    @Test public void trainingConfigurationIsP02SoloVersusP01Npc() {
        assertTrue(P02TrainingMatch.ENABLED);
        FighterState[] team = P02TrainingMatch.newPlayerTeam();
        FighterState opponent = P02TrainingMatch.newOpponent();
        assertEquals(1, team.length);
        assertEquals("p02", team[0].character.id);
        assertEquals("player_base", opponent.character.id);
        assertNull(new TeamSystem.Side(0, team[0]).partner());
        assertEquals("p02_idle",team[0].character.animation(SpriteStates.IDLE).atlas.resource);
        assertEquals("player_base_intro",
            opponent.character.animation(SpriteStates.INTRO).atlas.resource);
    }

    @Test public void combatEngineUsesP02AndKeepsP01NpcAcrossResets() {
        FighterState[] team = P02TrainingMatch.newPlayerTeam();
        FighterState npc = P02TrainingMatch.newOpponent();
        CombatEngine engine = new CombatEngine(team[0], npc, 420f, 980f, new CombatConfig());
        engine.setTeam(0, team);
        assertEquals("p02", engine.fighter(0).character().id);
        assertEquals("player_base", engine.fighter(1).character().id);
        assertNull("P02 remains solo; tag disabled", engine.team(0).partner());

        // Same sequence the GameView restage performs on REINICIAR P02.
        team = P02TrainingMatch.newPlayerTeam();
        npc = P02TrainingMatch.newOpponent();
        engine.setTeam(0, team);
        engine.setTeam(1, npc);
        engine.restage(420f, 980f);
        assertEquals("p02", engine.fighter(0).character().id);
        assertEquals("player_base", engine.fighter(1).character().id);
        assertNull(engine.team(0).partner());
    }
}
