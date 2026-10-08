package com.gamelutagpt;

/** Mutable per-match state of one fighter; its rules come from the character pack. */
final class FighterState {
    final CharacterDefinition character;
    final CharacterDefinition.Fighter profile;
    /** HUD title: team slot plus the character's single display name. */
    final String hudTitle;
    final String reserveHudLabel;
    int life;
    /**
     * Recoverable life (drawn red past the life bar): part of the damage taken, which comes
     * back slowly while this fighter waits off point. life + recoverableLife ≤ maxLife.
     */
    int recoverableLife;
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

    /** Back to full life (training refills, demo reset, tests). */
    void restoreLife() {
        life = profile.maxLife;
        recoverableLife = 0;
        refreshHudLabels();
    }

    /**
     * Takes damage: life goes down and {@code recoverablePermille} of it (at most what
     * fits under the max) becomes recoverable life. A KO clears it.
     */
    void takeDamage(int damage, int recoverablePermille) {
        if (damage <= 0) return;
        life = Math.max(0, life - damage);
        recoverableLife += damage * recoverablePermille / 1000;
        if (life == 0) recoverableLife = 0;
        recoverableLife = Math.min(recoverableLife, profile.maxLife - life);
        refreshLifeLabel();
    }

    /** Off point: up to {@code amount} of the recoverable life comes back. */
    void regenerate(int amount) {
        if (life <= 0 || recoverableLife <= 0 || amount <= 0) return;
        int gain = Math.min(amount, recoverableLife);
        life += gain;
        recoverableLife -= gain;
        refreshLifeLabel();
    }

    float superBars() {
        return superMeter / (float)CombatConfig.METER_PER_BAR;
    }

    void addSuperMeter(int amount) {
        if (amount <= 0) return;
        superMeter = Math.min(CombatConfig.MAX_METER, superMeter + amount);
        refreshSuperLabels();
    }

    /** Pays a cost already checked by the caller (Super, Ultra, pushblock, guard cancel). */
    void spendSuperMeter(int amount) {
        superMeter -= amount;
        refreshSuperLabels();
    }

    void refreshHudLabels() {
        refreshLifeLabel();
        refreshSuperLabels();
    }

    // Each label is rebuilt only when its own value changes (regeneration runs every frame).
    private void refreshLifeLabel() {
        lifeHudLabel = "HP " + life + " / " + profile.maxLife + (recoverableLife > 0 ? "  (+" + recoverableLife + ")" : "");
    }

    private void refreshSuperLabels() {
        int level = superMeter / CombatConfig.METER_PER_BAR;
        superHudLabel = String.format(
            java.util.Locale.US,
            "SUPER %.2f / 5  •  LV %d",
            superBars(),
            level
        );
        superLevelHudLabel = "LV " + level;
    }
}
