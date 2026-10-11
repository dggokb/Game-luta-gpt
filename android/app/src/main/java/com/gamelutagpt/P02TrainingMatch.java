package com.gamelutagpt;

/**
 * Temporary P02-only animation workbench.
 * Keeps the production roster and character sprites untouched.
 * The combat engine already supports teams with a single member.
 */
final class P02TrainingMatch {
    static final boolean ENABLED = true;
    static final String PLAYER_ID = "p02";
    static final String NPC_ID = "player_base";

    private P02TrainingMatch() {}

    static FighterState[] newPlayerTeam() {
        return new FighterState[]{
            new FighterState(GeneratedCharacters.get(PLAYER_ID), "PLAYER 1")
        };
    }

    static FighterState newOpponent() {
        return new FighterState(GeneratedCharacters.get(NPC_ID), "CPU");
    }
}
