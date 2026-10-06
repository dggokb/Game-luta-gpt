package com.gamelutagpt;

/** Mutable per-match state of one fighter; its rules come from the character pack. */
final class FighterState {
    final CharacterDefinition character;
    final CharacterDefinition.Fighter profile;
    /** HUD title: team slot plus the character's single display name. */
    final String hudTitle;
    final String reserveHudLabel;
    int life;
    float superMeter;
    String lifeHudLabel;
    String superHudLabel;
    String superLevelHudLabel;

    FighterState(CharacterDefinition character, String slotLabel) {
        this.character = character;
        this.profile = character.fighter;
        this.hudTitle = slotLabel + " · " + character.displayName;
        this.reserveHudLabel = "RESERVA: " + character.displayName;
        this.life = profile.maxLife;
        this.superMeter = 0f;
        refreshHudLabels();
    }

    void refreshHudLabels() {
        int level = (int)Math.floor(superMeter);
        lifeHudLabel = "HP " + life + " / " + profile.maxLife;
        superHudLabel = String.format(
            java.util.Locale.US,
            "SUPER %.2f / 5  •  LV %d",
            superMeter,
            level
        );
        superLevelHudLabel = "LV " + level;
    }
}
