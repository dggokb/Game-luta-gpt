package com.gamelutagpt;

/** Mutable per-match state of one fighter; its rules come from the character pack. */
final class FighterState {
    final CharacterDefinition character;
    final CharacterDefinition.Fighter profile;
    /** HUD title: team slot plus the character's single display name. */
    final String hudTitle;
    final String reserveHudLabel;
    int life;
    /** Super meter in thousandths of a bar (0..CombatConfig.MAX_METER); integer for determinism. */
    int superMeter;
    String lifeHudLabel;
    String superHudLabel;
    String superLevelHudLabel;

    FighterState(CharacterDefinition character, String slotLabel) {
        this.character = character;
        this.profile = character.fighter;
        this.hudTitle = slotLabel + " · " + character.displayName;
        this.reserveHudLabel = "RESERVA: " + character.displayName;
        this.life = profile.maxLife;
        this.superMeter = 0;
        refreshHudLabels();
    }

    /** Test helper: back to full life. */
    void restoreLife() {
        life = profile.maxLife;
        refreshHudLabels();
    }

    float superBars() {
        return superMeter / (float)CombatConfig.METER_PER_BAR;
    }

    void addSuperMeter(int amount) {
        if (amount <= 0) return;
        superMeter = Math.min(CombatConfig.MAX_METER, superMeter + amount);
        refreshHudLabels();
    }

    void refreshHudLabels() {
        int level = superMeter / CombatConfig.METER_PER_BAR;
        lifeHudLabel = "HP " + life + " / " + profile.maxLife;
        superHudLabel = String.format(
            java.util.Locale.US,
            "SUPER %.2f / 5  •  LV %d",
            superBars(),
            level
        );
        superLevelHudLabel = "LV " + level;
    }
}
