package com.gamelutagpt;

import android.content.Context;
import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import static org.junit.Assert.*;

/** Regression for temporary P02-only animation workbench. */
@RunWith(RobolectricTestRunner.class)
public class P02TrainingMatchTest {
    @Test public void trainingConfigurationIsP02SoloVersusP01Npc() {
        assertTrue(P02TrainingMatch.ENABLED);
        FighterState[] team = P02TrainingMatch.newPlayerTeam();
        FighterState opponent = P02TrainingMatch.newOpponent();
        assertEquals(1, team.length);
        assertEquals("p02", team[0].character.id);
        assertEquals("player_base", opponent.character.id);
        assertNull(new TeamSystem.Side(0,team[0]).partner());
        assertEquals("p02_idle",team[0].character.animation(SpriteStates.IDLE).atlas.resource);
        assertEquals("player_base_intro",
            opponent.character.animation(SpriteStates.INTRO).atlas.resource);
    }

    @Test public void launchedGameViewUsesP02AndKeepsP01OpponentAfterReset()
            throws Exception {
        Context ctx = RuntimeEnvironment.getApplication();
        GameView game = new GameView(ctx);
        try {
            verify(game);
            java.lang.reflect.Method open = GameView.class.getDeclaredMethod("openCharacterSelect");
            open.setAccessible(true);
            open.invoke(game);
            verify(game);
        } finally {
            game.release();
        }
    }

    private static void verify(GameView game) throws Exception {
        Field teamField = GameView.class.getDeclaredField("team");
        teamField.setAccessible(true);
        FighterState[] team = (FighterState[])teamField.get(game);
        assertEquals("P02 solo: no P01 or other tag partner",1,team.length);
        assertEquals("p02",team[0].character.id);
        Field npcField = GameView.class.getDeclaredField("opponentFighter");
        npcField.setAccessible(true);
        FighterState npc = (FighterState)npcField.get(game);
        assertEquals("player_base",npc.character.id);
        assertEquals("p02",game.engine.fighter(0).character().id);
        assertEquals("player_base",game.engine.fighter(1).character().id);
        assertNull(game.engine.team(0).partner());
        Field select = GameView.class.getDeclaredField("characterSelect");
        select.setAccessible(true);
        assertFalse(((CharacterSelect)select.get(game)).isOpen());
    }
}
