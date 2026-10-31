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
    /** Guarda (0..CombatConfig.GUARD_MAX): cada defesa gasta; zerou, guard break. */
    int guard = CombatConfig.GUARD_MAX;
    /**
     * Vida que nenhum golpe tira (0 = pode cair). A cinemática da luta segura 1 enquanto espera
     * o gatilho de vida baixa e enquanto roda, para nenhum golpe pular a cena indo direto ao KO.
     */
    int lifeFloor;
    /**
     * Dano exato do próximo golpe que entrar (o golpe do QTE da cinemática), no lugar do dano
     * do golpe; -1 = o dano normal. Depois dele, as outras batidas do mesmo golpe não tiram
     * mais nada ({@link #cenaSemDano}) até o roteiro soltar.
     */
    int cenaDano = -1;
    boolean cenaSemDano;
    /** Barra do SHINKAI (0..CombatConfig.SHINKAI_MAX); esvazia ao ativar. */
    int shinkai;
    /** Quadros de SHINKAI que faltam (0 fora dele). */
    int shinkaiFrames;
    /** Já usou o SHINKAI neste round (uma vez por personagem). */
    boolean shinkaiUsed;
    /** O tipo do SHINKAI dele (POWER, SPEED, ENERGY ou TECHNIQUE). */
    final Shinkai.Type shinkaiType;
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
        this.shinkaiType = Shinkai.typeOf(character);
        refreshHudLabels();
    }

    /** Back to full life (training refills, demo reset, tests). */
    void restoreLife() {
        life = profile.maxLife;
        recoverableLife = 0;
        guard = CombatConfig.GUARD_MAX;
        poisonLeft = poisonFramesLeft = 0;
        refreshHudLabels();
    }

    /**
     * Takes damage: life goes down and {@code recoverablePermille} of it (at most what
     * fits under the max) becomes recoverable life. A KO clears it.
     */
    void takeDamage(int damage, int recoverablePermille) {
        if (damage <= 0) return;
        if (cenaDano >= 0) {
            damage = cenaDano;
            cenaDano = -1;
            cenaSemDano = true;
            if (damage <= 0) return;
        } else if (cenaSemDano) {
            return;
        }
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

    /**
     * Chip (o arranhão do especial/Super defendido): sai da vida e vira toda vida vermelha
     * (volta fora de campo). Nunca tira o último ponto.
     */
    void chip(int amount) {
        int taken = Math.min(amount, Math.max(0, life - Math.max(1, lifeFloor)));
        if (taken <= 0) return;
        life -= taken;
        recoverableLife = Math.min(recoverableLife + taken, profile.maxLife - life);
        refreshLifeLabel();
    }

    /** Cura: primeiro a vida vermelha volta, depois o resto, até a máxima. */
    void heal(int amount) {
        if (life <= 0 || amount <= 0) return;
        int fromRed = Math.min(amount, recoverableLife);
        recoverableLife -= fromRed;
        life = Math.min(profile.maxLife, life + amount);
        recoverableLife = Math.min(recoverableLife, profile.maxLife - life);
        refreshLifeLabel();
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

    /** A barra do SHINKAI cheia e ainda não usada neste round. */
    boolean shinkaiReady() {
        return !shinkaiUsed && shinkaiFrames == 0 && shinkai >= CombatConfig.SHINKAI_MAX;
    }

    boolean inShinkai() { return shinkaiFrames > 0; }

    boolean inShinkai(Shinkai.Type type) { return shinkaiFrames > 0 && shinkaiType == type; }

    /** O primeiro Super do SHINKAI (sai sem barra) ainda não saiu. */
    boolean shinkaiFreeSuper;
    /** TECHNIQUE: a aparada automática do SHINKAI ainda não foi usada. */
    boolean shinkaiAutoParry;

    /** Dá para pagar o Super agora: uma barra, ou o Super grátis do SHINKAI. */
    boolean canPaySuper() {
        return superMeter >= CombatConfig.SUPER_COST || (inShinkai() && shinkaiFreeSuper);
    }

    /** Enche a barra do SHINKAI (nada depois de usado no round, nem durante ele). */
    void chargeShinkai(int amount) {
        if (amount <= 0 || shinkaiUsed || shinkaiFrames > 0 || life <= 0) return;
        shinkai = Math.min(CombatConfig.SHINKAI_MAX, shinkai + amount);
    }

    /** Round novo: o SHINKAI volta a poder ser usado (a barra fica como estava). */
    void refreshShinkai() {
        shinkaiUsed = false;
        shinkaiFrames = 0;
        shinkaiFreeSuper = false;
        shinkaiAutoParry = false;
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

    private int superLevelShown = -1;
    private int superMeterLabeled = -1;

    private void refreshSuperLabels() {
        // O "LV n" do botão a cada mudança de nível; o rótulo longo (só no DEBUG) fica para quem
        // pedir: o SHINKAI ENERGY enche a barra a cada passo e era um String.format por passo.
        int level = superMeter / CombatConfig.METER_PER_BAR;
        if (level != superLevelShown) {
            superLevelShown = level;
            superLevelHudLabel = "LV " + level;
        }
    }

    /** "SUPER 2.40 / 5  •  LV 2", montado só quando a barra mudou desde a última vez. */
    String superHudLabel() {
        if (superMeterLabeled != superMeter) {
            superMeterLabeled = superMeter;
            superHudLabel = String.format(java.util.Locale.US, "SUPER %.2f / 5  •  LV %d",
                superBars(), superMeter / CombatConfig.METER_PER_BAR);
        }
        return superHudLabel;
    }
}
