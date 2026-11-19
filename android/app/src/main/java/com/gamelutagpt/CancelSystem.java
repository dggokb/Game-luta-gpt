package com.gamelutagpt;

/**
 * The single place that decides whether a fighter starts a new attack. A buffered press
 * starts something only from NEUTRAL, or from an attack whose cancel window for the
 * current outcome (hit, block or whiff) is open and whose cancelInto lists the target.
 * Button handlers never start attacks.
 */
final class CancelSystem {
    private CancelSystem() {}

    /** Golpe de comando de chão (→ + H), quando o pacote tem um (o Soco no plexo do Ryūga). */
    static final String COMMAND_HEAVY = "6H";
    /** Golpe de corrida (13/11): o H apertado correndo (a Ombrada do Ryūga, o Carrinho da Yin-lu). */
    static final String RUN_HEAVY = "DH";
    /**
     * Continuação (16/11): o golpe que sai apertando L, M ou H durante um especial que emenda
     * nele (o chute girando depois da voadora da p15). Do neutro não sai.
     */
    static final String FOLLOW_UP = "SG";
    /**
     * Abertura do S1 em três (19/11, a Garra do tigre do Kōga): com ela no pacote, ↓↘→ + botão é este
     * golpe (o 1º corte); um botão emenda a continuação ({@link #FOLLOW_UP}, o 2º corte, o X) e outro
     * botão, dentro dela, emenda o S1 de projétil (o X lançado).
     */
    static final String ABERTURA = "S1A";

    /**
     * A continuação do golpe atual (o id do golpe), ou null: a continuação de sempre ({@link #FOLLOW_UP})
     * de quem emenda nela, e, na sequência do S1 em três, o S1 (o poder) depois da continuação.
     */
    static String continuacao(CombatFighter f) {
        CharacterDefinition c = f.character();
        if (!f.attacking() || f.attack == null) return null;
        if (c.moves.containsKey(FOLLOW_UP) && f.attack.cancelsInto(FOLLOW_UP)) return FOLLOW_UP;
        if (FOLLOW_UP.equals(f.attack.id) && c.moves.containsKey(ABERTURA) && f.attack.cancelsInto("S") &&
            c.attack("S") != null) return "S";
        return null;
    }

    /** A continuação do golpe atual, se ele emenda nela e o botão foi apertado agora. */
    private static AttackDefinition followUp(CombatFighter f) {
        String id = continuacao(f);
        return id == null ? null : f.character().attack(id);
    }

    /** Cancel window of the attack for what it has done so far; null when closed for that outcome. */
    static AttackDefinition.Window windowFor(CombatFighter f) {
        if (f.attack == null) return null;
        switch (f.outcome) {
            case HIT: return f.attack.hitWindow;
            case BLOCK: return f.attack.blockWindow;
            default: return f.attack.whiffWindow;
        }
    }

    static boolean windowOpen(CombatFighter f) {
        AttackDefinition.Window window = windowFor(f);
        return window != null && window.contains(f.attackFrame);
    }

    /** Whether the current attack may become {@code target} on this frame. */
    static boolean canCancel(CombatFighter f, String target, ComboSession session) {
        // A abertura do S1 em três sai de onde o S1 sairia (quem emenda no S1 emenda nela).
        boolean emenda = f.attacking() && (f.attack.cancelsInto(target) || ABERTURA.equals(target) && f.attack.cancelsInto("S"));
        if (!f.attacking() || !windowOpen(f) || !emenda) return false;
        // No chão é o pulo do cancelamento; no ar, o cancelamento de pulo (uma vez por voo). Golpe de
        // várias pancadas só pula depois da última (12/11: o S4 da Yin-lu lança no segundo chute).
        if (AttackDefinition.JUMP.equals(target)) {
            if (f.attack.maxHits > 1 && f.hitsOnTarget < f.attack.maxHits) return false;
            return f.grounded || !f.airJumpUsed;
        }
        AttackDefinition next = f.character().attack(target);
        return next != null && chainAllows(f, next) && (session == null || session.allows(next));
    }

    /** Limite de usos do golpe dentro da corrente atual, acertando ou na defesa. */
    static boolean chainAllows(CombatFighter f, AttackDefinition next) {
        if (next.maxUsesPerChain <= 0) return true;
        int uses = 0;
        for (String used : f.chain) if (used.equals(next.id)) uses++;
        return uses < next.maxUsesPerChain;
    }

    /**
     * The attack a buffered press means right now, given stance and resources, or null when
     * it cannot start at all (no energy, projectile still alive, not enough meter...).
     * {@code autoStepOut[0]} receives the auto-combo step for AUTO presses.
     */
    static AttackDefinition resolve(CombatFighter f, InputBuffer.Entry entry, boolean projectileAlive,
                                    int[] autoStepOut) {
        CharacterDefinition c = f.character();
        if (autoStepOut != null) autoStepOut[0] = -1;
        switch (entry.button) {
            case LIGHT:
                if (followUp(f) != null) return followUp(f);
                return c.attack(binding(f, "L", entry.crouch));
            case MEDIUM:
                if (followUp(f) != null) return followUp(f);
                // → + M do neutro, no chão: o golpe alto. Dentro de um combo continua sendo o M
                // (quem segura → durante L > M não perde a ligação).
                if (entry.forward && !entry.crouch && f.grounded && !f.attacking() &&
                    c.moves.containsKey(GolpeAlto.BINDING)) {
                    return c.attack(GolpeAlto.BINDING);
                }
                return c.attack(binding(f, "M", entry.crouch));
            case HEAVY:
                if (followUp(f) != null) return followUp(f);
                // Correndo no chão: o golpe de corrida do personagem (sai da corrida, sem parar). Na
                // perseguição da parede (segurar → depois do quique já corre) o → + H continua o de
                // sempre: ali a corrida é o combo, não a abertura.
                if (f.forwardDashing && !f.chaseRunning && f.wallChase == 0 && !entry.crouch && f.grounded && !f.attacking() &&
                    c.moves.containsKey(RUN_HEAVY)) {
                    return c.attack(RUN_HEAVY);
                }
                // → + H no chão: o golpe de comando (12/11), do neutro ou emendado no combo. Dentro
                // de um golpe que não emenda nele (a rota não tem o 6H), continua sendo o H.
                if (entry.forward && !entry.crouch && f.grounded && c.moves.containsKey(COMMAND_HEAVY) &&
                    (!f.attacking() || f.attack.cancelsInto(COMMAND_HEAVY))) {
                    return c.attack(COMMAND_HEAVY);
                }
                return c.attack(binding(f, "H", entry.crouch));
            case SPECIAL:
                // Command specials (S2, S3...): ground moves, or air-only ones; the strength names them.
                CharacterDefinition.Special special = c.specials.get(entry.strength);
                if (special != null) {
                    // Especial que lança: um projétil por vez, como a energia.
                    if (special.projectile != null && projectileAlive) return null;
                    if (!special.saiEm(f.grounded) || !especialNoArLiberado(f, special.air)) return null;
                    return c.attack(entry.strength);
                }
                // S1 em três: ↓↘→ + botão é o 1º corte (o poder sai no fim da sequência).
                if (c.moves.containsKey(ABERTURA) && f.grounded) return c.attack(ABERTURA);
                if (projectileAlive || f.state.profile.energy == null) return null;
                if (c.fighter.energyAir && f.grounded) return null;   // S1 só no ar (o p14)
                if (!especialNoArLiberado(f, c.fighter.energyAir)) return null;
                return c.attack("S");
            case SUPER:
                if (f.state.profile.superAttack == null || !f.state.canPaySuper()) return null;
                // No ar o Super também sai (parado no ar enquanto dura), dentro do limite por pulo.
                if (!especialNoArLiberado(f, false)) return null;
                return c.attack("SUPER");
            case AUTO: {
                String[] route = f.state.profile.autoCombo;
                if (route.length == 0) return null;
                int step = f.attacking() && f.autoStep >= 0 && f.autoStep + 1 < route.length ? f.autoStep + 1 : 0;
                if (autoStepOut != null) autoStepOut[0] = step;
                return c.attack(binding(f, route[step], false));
            }
            default:
                return null;
        }
    }

    /**
     * No ar, cada pulo solta até {@link CombatConfig#ESPECIAIS_NO_AR_POR_PULO} especiais de chão (S1,
     * S2..S9, Super), para ninguém ficar parado no ar para sempre. O especial só de ar não conta.
     */
    static boolean especialNoArLiberado(CombatFighter f, boolean soDeAr) {
        return f.grounded || soDeAr || f.especiaisNoAr < CombatConfig.ESPECIAIS_NO_AR_POR_PULO;
    }

    /**
     * Ground L/M/H, crouching 2X or airborne jX for the current stance. In the air, down
     * held picks j2X when the character has it (a second air version), else jX.
     */
    static String binding(CombatFighter f, String button, boolean crouch) {
        if (!f.grounded) {
            return crouch && f.character().moves.containsKey("j2" + button) ? "j2" + button : "j" + button;
        }
        return crouch ? "2" + button : button;
    }
}
