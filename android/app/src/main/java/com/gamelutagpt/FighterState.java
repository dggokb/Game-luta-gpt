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
    /**
     * Vida que nenhum golpe tira (0 = pode cair). A cinemática da luta segura 1 enquanto espera
     * o gatilho de vida baixa e enquanto roda, para nenhum golpe pular a cena indo direto ao KO.
     */
    int lifeFloor;
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
        poisonLeft = poisonFramesLeft = 0;
        refreshHudLabels();
    }

    /**
     * Takes damage: life goes down and {@code recoverablePermille} of it (at most what
     * fits under the max) becomes recoverable life. A KO clears it.
     */
    void takeDamage(int damage, int recoverablePermille) {
        if (damage <= 0) return;
        life = Math.max(Math.min(lifeFloor, life), life - damage);
        recoverableLife += damage * recoverablePermille / 1000;
        if (life == 0) recoverableLife = 0;
        recoverableLife = Math.min(recoverableLife, profile.maxLife - life);
        refreshLifeLabel();
    }

    /** Poison still to come and the frames it is spread over (0 when clean). */
    int poisonLeft;
    int poisonFramesLeft;

    /** Poisoned: {@code damage} over {@code frames}; a new dose replaces a weaker one. */
    void poison(int damage, int frames) {
        if (damage <= 0 || frames <= 0 || life <= 0) return;
        if (damage >= poisonLeft) {
            poisonLeft = damage;
            poisonFramesLeft = frames;
        }
    }

    boolean poisoned() { return poisonLeft > 0; }

    /** One frame of poison: its share of the damage, never the last point of life. Returns it. */
    int tickPoison() {
        if (poisonLeft <= 0 || poisonFramesLeft <= 0) return 0;
        int dose = poisonLeft / poisonFramesLeft;
        poisonFramesLeft--;
        poisonLeft -= dose;
        if (poisonFramesLeft == 0) poisonLeft = 0;
        int dealt = Math.min(dose, Math.max(0, life - 1));
        if (dealt > 0) {
            life -= dealt;
            recoverableLife = Math.min(recoverableLife, profile.maxLife - life);
            refreshLifeLabel();
        }
        return dealt;
    }

    /** Pays life for a move (no recoverable part); never the last point. */
    void payLife(int amount) {
        int paid = Math.min(amount, Math.max(0, life - 1));
        if (paid <= 0) return;
        life -= paid;
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
