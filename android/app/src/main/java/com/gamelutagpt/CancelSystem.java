package com.gamelutagpt;

/**
 * The single place that decides whether a fighter starts a new attack. A buffered press
 * starts something only from NEUTRAL, or from an attack whose cancel window for the
 * current outcome (hit, block or whiff) is open and whose cancelInto lists the target.
 * Button handlers never start attacks.
 */
final class CancelSystem {
    private CancelSystem() {}

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
        if (!f.attacking() || !windowOpen(f) || !f.attack.cancelsInto(target)) return false;
        if (AttackDefinition.JUMP.equals(target)) return f.grounded;
        AttackDefinition next = f.character().attack(target);
        return next != null && (session == null || session.allows(next));
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
            case LIGHT: return c.attack(binding(f, "L", entry.crouch));
            case MEDIUM: return c.attack(binding(f, "M", entry.crouch));
            case HEAVY: return c.attack(binding(f, "H", entry.crouch));
            case SPECIAL:
                // Command specials (S2, S3...) are ground moves; the strength names them.
                if (c.specials.containsKey(entry.strength)) return f.grounded ? c.attack(entry.strength) : null;
                if (projectileAlive || f.state.profile.energy == null) return null;
                return c.attack("S");
            case SUPER:
                if (f.state.profile.superAttack == null || f.state.superMeter < CombatConfig.SUPER_COST) return null;
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
