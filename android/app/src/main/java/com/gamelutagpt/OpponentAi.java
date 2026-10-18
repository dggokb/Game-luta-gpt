package com.gamelutagpt;

import java.util.Random;

/**
 * Decision-making of the CPU opponent. It only chooses; AiController turns the choice
 * into pad input ({@link Actions}) and CombatEngine plays it like a player's. Pure Java, so decisions
 * can be tested with a seeded {@link Random}.
 */
final class OpponentAi {
    interface Actions {
        /** Clears the previous movement intent before a new decision. */
        void stop();
        void attack(String type);
        void superAttack();
        void energy(String strength);
        void backdash();
        void moveForward(boolean dash);
        void jump(boolean superJump);
        /** L + M: throw. */
        void grab();
        /** Anda para trás (o estilo de longe mantém a distância dele). */
        default void moveBack() {}
    }

    /** Snapshot of what the AI may look at; filled by the caller every step. */
    static final class Situation {
        CharacterDefinition self;
        CharacterDefinition.Body target;
        float distance;
        float verticalDistance;
        boolean selfAirborne;
        boolean targetGrounded;
        /** O rival (como ela viu) está agachado: o golpe alto (6M) entra na escolha. */
        boolean targetCrouching;
        boolean targetAlive;
        boolean superReady;
        boolean projectileActive;
        boolean attackReady;
        /** Center distance within which a throw reaches the target. */
        float throwReach;
        /** O rival defendeu há pouco (ficou na defesa): o agarrão abre quem só defende. */
        boolean rivalBlocked;
        /** Nível e personalidade; null = nível 5, equilibrado. */
        CpuLevel level;
        CpuStyle style;
        /**
         * Pressa do fim do tempo (0 = calma, 1 = últimos segundos): ela quer terminar a luta,
         * não deixar o tempo acabar. Ver {@link #urgency(int)}.
         */
        float urgency;
    }

    /** A pressa começa com este tanto de segundos no relógio e chega ao máximo neste outro. */
    static final int URGENCY_START_SECONDS = 45, URGENCY_FULL_SECONDS = 10;

    /** Pressa pelos segundos que faltam (negativo = sem relógio: nenhuma). */
    static float urgency(int secondsLeft) {
        if (secondsLeft < 0 || secondsLeft >= URGENCY_START_SECONDS) return 0f;
        float u = (URGENCY_START_SECONDS - secondsLeft) / (float)(URGENCY_START_SECONDS - URGENCY_FULL_SECONDS);
        return Math.min(1f, Math.max(0f, u));
    }

    static final float DECISION_MIN = 0.12f;
    static final float DECISION_MAX = 0.28f;
    // Attack preferences; the character pack decides which of them can reach.
    static final String[] GROUND_ATTACKS = {"2L", "2M", "2H", "L", "M", "H"};
    // Os agachados (2L/2M/2H) pesam menos que os em pé: a CPU não fica o tempo todo abaixada.
    static final double[] GROUND_WEIGHTS = {0.13, 0.13, 0.12, 0.22, 0.21, 0.19};
    /** Contra quem está agachado: o golpe alto (→ + M) para abrir a defesa baixa. */
    static final String[] GROUND_ATTACKS_VS_CROUCH = {"2L", "2M", "2H", "L", "M", "H", GolpeAlto.BINDING};
    static final double[] GROUND_WEIGHTS_VS_CROUCH = {0.12, 0.12, 0.11, 0.15, 0.15, 0.13, 0.22};
    static final String[] AIR_ATTACKS = {"L", "M", "H"};
    /** Chance of throwing when the target is right next to it, and of teching a throw. */
    static final double THROW_CHANCE = 0.16;
    static final double TECH_CHANCE = 0.40;
    /** Chance of an air tech when the air hitstun ends. */
    static final double AIR_TECH_CHANCE = 0.70;
    static final double[] AIR_WEIGHTS = {0.30, 0.34, 0.36};
    /**
     * Folga de alcance dos golpes no chão: a distância é a vista (com o atraso da reação), e o
     * golpe na beira do alcance saía no vento por poucas unidades.
     */
    static final float GROUND_REACH_MARGIN = 8f;
    /** Folga de alcance dos aéreos da CPU (ver {@link #think}). */
    static final float AIR_REACH_MARGIN = 24f;

    private final Random random;
    private float decisionTimer;

    OpponentAi(Random random) {
        this.random = random;
    }

    void reset() {
        decisionTimer = 0f;
    }

    /** Semente para os outros sorteios da CPU (reações), tirada deste gerador. */
    long seed() {
        return random.nextLong();
    }

    /**
     * Advances the decision clock; returns true when a new decision was taken. A posição do rival
     * na {@link Situation} é a que a CPU viu (com o atraso da reação), nunca a do quadro atual.
     */
    boolean think(float dt, Situation s, Actions actions) {
        decisionTimer -= dt;
        if (decisionTimer > 0f) return false;
        CpuLevel level = s.level != null ? s.level : CpuLevel.of(CpuLevel.DEFAULT);
        CpuStyle style = s.style != null ? s.style : CpuStyle.EQUILIBRADO;
        float u = s.urgency;
        float min = Math.max(DECISION_MIN, level.decisionMin);
        float max = Math.max(min, level.decisionMax);
        // Com pressa ela decide mais rápido (até 35% menos tempo entre as decisões).
        decisionTimer = (min + random.nextFloat() * (max - min)) * (1f - 0.35f * u);
        // E luta como o estilo agressivo: no fim do tempo todo mundo vai para cima.
        double aggression = style.aggression + (1.0 - style.aggression) * u;
        actions.stop();
        // O imprevisível às vezes larga o plano e faz qualquer coisa.
        if (style.erratic() && !s.selfAirborne && s.targetAlive && random.nextDouble() < 0.12) {
            double r = random.nextDouble();
            if (r < 0.4) actions.moveForward(r < 0.2);
            else if (r < 0.7) actions.jump(r < 0.5);
            else actions.moveBack();
            return true;
        }

        // No ar a distância vista erra mais (os dois se mexem rápido, e o empurrão de quem passa
        // por cima muda o lado): ela só solta o aéreo com uma folga de alcance.
        String airAttack = s.verticalDistance <= 115f
            ? CombatRules.pickInRange(AIR_ATTACKS, AIR_WEIGHTS,
                s.self, true, s.distance + AIR_REACH_MARGIN, s.target, random.nextDouble())
            : null;
        boolean vsCrouch = s.targetCrouching && s.targetGrounded;
        String groundAttack = CombatRules.pickInRange(vsCrouch ? GROUND_ATTACKS_VS_CROUCH : GROUND_ATTACKS,
            vsCrouch ? GROUND_WEIGHTS_VS_CROUCH : GROUND_WEIGHTS,
            s.self, false, s.distance + GROUND_REACH_MARGIN, s.target, random.nextDouble());

        // Perto o bastante para o L (o começo do combo do caderno): prefere abrir com ele a
        // cutucar com um golpe solto, mais quanto maior o nível. A cutucada na ponta não emenda.
        // Com folga de alcance: a distância é a vista (com o atraso da reação), e o L é curto.
        if (groundAttack != null && !vsCrouch && !"L".equals(groundAttack) &&
            CombatRules.reaches(s.self, "L", false, s.distance + STARTER_REACH_MARGIN, s.target) &&
            random.nextDouble() < starterChance(level)) {
            groundAttack = "L";
        }
        // Quem luta de longe recua para a distância dele em vez de bater colado.
        boolean keepAway = !s.selfAirborne && s.distance < style.preferredDistance - 60f &&
            aggression < 0.5 && random.nextDouble() < 0.5 * (1.0 - u);

        if (s.selfAirborne) {
            if (airAttack != null) actions.attack(airAttack);
            else if (s.distance > 95f) actions.moveForward(false);
        } else if (!s.targetAlive) {
            // nothing to do
        } else if (
            s.superReady &&
            superInRange(s.self, s.distance) &&
            // O Super cru (sem ter acertado nada) é dos níveis baixos: os altos guardam a barra
            // para fechar combo, punir e para o Ultra.
            random.nextDouble() < 0.18 * Math.min(1.0, level.kitScale + 0.2) * (1.0 + u) * (1.0 - 0.85 * smart(level))
        ) {
            actions.superAttack();
        } else if (
            energyInRange(s.self, s.distance) &&
            !s.projectileActive &&
            random.nextDouble() < Math.min(0.6, 0.24 * style.zoning) * (1.0 - 0.5 * u)
        ) {
            double r = random.nextDouble();
            actions.energy(r < 0.33 ? "L" : (r < 0.72 ? "M" : "H"));
        } else if (s.targetGrounded && s.attackReady && s.distance <= s.throwReach &&
            random.nextDouble() < THROW_CHANCE * style.throwing * (1.0 + 0.5 * u) *
                (s.rivalBlocked ? 1.0 + 2.5 * smart(level) : 1.0)) {
            actions.grab();
        } else if (groundAttack != null && s.attackReady && !keepAway) {
            actions.attack(groundAttack);
        } else if (keepAway) {
            actions.moveBack();
        } else if (s.distance < 82f && random.nextDouble() < 0.38 * (1.5 - aggression) * (1.0 - u)) {
            actions.backdash();
        } else if (s.distance > 360f * (1f - 0.4f * u) && random.nextDouble() < 0.38 * (0.5 + aggression)) {
            // Longe: corre. Com pressa corre já de mais perto.
            actions.moveForward(true);
        } else if (groundAttack == null && s.distance > CombatRules.MIN_MELEE_DISTANCE &&
            (s.distance > style.preferredDistance - 60f || random.nextDouble() < aggression)) {
            // Approach until at least one ground move can reach the target (inside the preferred
            // distance only as often as the style is aggressive).
            actions.moveForward(false);
        } else if (random.nextDouble() < 0.12 * style.jumping) {
            actions.jump(random.nextDouble() < 0.32);
        }
        return true;
    }

    /** Folga de alcance do L de abertura (a distância vista pode ter mudado desde então). */
    static final float STARTER_REACH_MARGIN = 30f;

    /**
     * Quanto a CPU usa os recursos com cabeça (0 no nível 1, 1 no 8): o Super e o Ultra crus
     * somem, a barra fica para fechar combo, punir e para o Ultra no momento certo.
     */
    static double smart(CpuLevel level) {
        return (level.number - CpuLevel.MIN) / (double)(CpuLevel.MAX - CpuLevel.MIN);
    }

    /** Chance de trocar a cutucada pelo L que abre o combo (nível 1: 15%; nível 8: 50%). */
    static double starterChance(CpuLevel level) {
        return 0.10 + 0.05 * level.number;
    }

    /**
     * Grabbed: frames to wait before teching, or -1 to let the throw land. A human reacts
     * to the grab, so the CPU techs only part of the time and not on the first frame.
     */
    int techDelay() {
        return techDelay(TECH_CHANCE);
    }

    /**
     * O tech é um palpite, não uma reação: a janela do tech é menor que a reação humana. A CPU só
     * aperta quando já esperava o agarrão (quem chama decide), com a chance do nível.
     */
    int techDelay(double chance) {
        return random.nextDouble() < chance ? 2 + random.nextInt(8) : -1;
    }

    /** Air tech when the hitstun ends: relative direction (0 neutral, 5 back, 1 forward), or -1 none. */
    int airTechDirection() {
        return airTechDirection(AIR_TECH_CHANCE);
    }

    int airTechDirection(double chance) {
        if (random.nextDouble() >= chance) return -1;
        double r = random.nextDouble();
        return r < 0.40 ? 0 : r < 0.75 ? 5 : 1;
    }

    /** Wake-up: relative direction held while down (7 quick rise, 5/1 rolls, 3 late, 0 normal). */
    int wakeupDirection() {
        double r = random.nextDouble();
        return r < 0.30 ? 7 : r < 0.50 ? 5 : r < 0.62 ? 1 : r < 0.72 ? 3 : 0;
    }

    /**
     * O S1 que voa sai de meia distância até onde ele chega; o S1 corpo a corpo (tapa, investida)
     * só de perto: o alcance dele mais o que o avanço anda.
     */
    static boolean energyInRange(CharacterDefinition self, float distance) {
        AttackDefinition s = self.attack("S");
        if (s == null) return false;
        if (s.melee()) {
            AttackDefinition.Mechanics m = s.mechanics;
            float advance = m.advanceSpeed > 0f ? m.advanceSpeed * (m.advanceEnd - m.advanceStart + 1) * CombatConfig.DT : 0f;
            return distance <= s.reach + advance;
        }
        CharacterDefinition.Projectile energy = self.fighter.energy;
        float far = Math.min(620f, energy.spawnX + energy.range);
        return distance >= Math.min(230f, far * 0.5f) && distance <= far;
    }

    /** A wave Super goes from afar; a Super that strikes only up close (its reach plus the advance). */
    static boolean superInRange(CharacterDefinition self, float distance) {
        AttackDefinition sup = self.attack("SUPER");
        if (sup != null && sup.melee()) return distance <= sup.reach + 90f;
        return distance >= 250f && distance <= 720f;
    }
}
