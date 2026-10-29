package com.gamelutagpt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Para que serve cada especial (S2–S4, e o S1 de mergulho) na mão da CPU, lido dos dados do
 * golpe (AttackDefinition.Mechanics, projétil, lançamento, altura), sem nome de personagem: o
 * contra-golpe espera o golpe do rival, a esquiva e o repel esperam a bola, o agarrão de
 * especial pega colado, o projétil e a armadilha vão de longe, o avanço entra, o que sobe tira
 * do ar. A CPU (AiController) escolhe entre eles pela situação. Pure Java.
 */
final class CpuSpecials {
    private CpuSpecials() {}

    enum Role {
        /** Projétil de especial (shuriken, pedra em arco, seringa que vira poça). */
        PROJECTILE,
        /** Projétil que vira armadilha no chão (a caixa da p16). */
        TRAP,
        /** Agarrão de especial (não se defende): colado. */
        GRAB,
        /** Repel: desfaz a bola que chega na janela. */
        REPEL,
        /** Postura de contra-golpe: espera o golpe do rival. */
        COUNTER,
        /** Esquiva (ou sumiço): passa pelo golpe ou pela bola. */
        EVADE,
        /** Puxa o rival de longe. */
        PULL,
        /** Liga a sombra que repete os golpes seguintes. */
        ECHO,
        /** Só no ar: desce em mergulho. */
        DIVE,
        /** Armadura: atravessa o golpe do rival. */
        ARMOR,
        /** Sobe rápido: tira do ar. */
        ANTI_AIR,
        /** Baixo: pega quem está em pé. */
        LOW,
        /** Avança muito: entra de longe, pune de longe. */
        RUSH,
        /** Golpe de especial comum. */
        POKE
    }

    /** Um especial com a função e o quanto ele alcança (centro a centro, sem o corpo do rival). */
    static final class Option {
        /** "S2".."S4", ou "S" (o S1 de mergulho). */
        final String id;
        final AttackDefinition attack;
        final Role role;
        /** Alcance do golpe somado ao avanço até o fim do ativo (golpe corpo a corpo). */
        final float reach;
        /** Projétil: até onde voa. */
        final float range;
        /** Comando de carga ([←] → ou [↓] ↑): só sai com a carga feita, como para o jogador. */
        boolean hold, holdDown;

        Option(String id, AttackDefinition attack, Role role, float reach, float range) {
            this.id = id;
            this.attack = attack;
            this.role = role;
            this.reach = reach;
            this.range = range;
        }

        Option charge(CharacterDefinition.Special s) {
            hold = s.hold;
            holdDown = s.hold && s.command()[0] == 3;
            return this;
        }

        AttackDefinition.Mechanics m() { return attack.mechanics; }

        /** Paga com a própria vida (o p12). */
        boolean costsLife() { return attack.mechanics.lifeCost > 0; }
    }

    /** Sem estado: a mesma ficha para todos (as baterias rodam lutas em paralelo). */
    private static final Map<CharacterDefinition, List<Option>> CACHE = new ConcurrentHashMap<>();

    static List<Option> of(CharacterDefinition c) {
        return CACHE.computeIfAbsent(c, CpuSpecials::read);
    }

    private static List<Option> read(CharacterDefinition c) {
        List<Option> out = new ArrayList<>();
        for (Map.Entry<String, CharacterDefinition.Special> e : c.specials.entrySet()) {
            String id = e.getKey();
            AttackDefinition a = c.attack(id);
            if (a == null) continue;
            out.add(option(id, a, e.getValue()).charge(e.getValue()));
        }
        // O S1 de mergulho (só no ar) a OpponentAi não solta: entra aqui.
        AttackDefinition s1 = c.attack("S");
        if (s1 != null && c.fighter.energyAir && s1.mechanics.dives()) out.add(new Option("S", s1, Role.DIVE, s1.reach, 0f));
        return Collections.unmodifiableList(out);
    }

    static Option option(String id, AttackDefinition a, CharacterDefinition.Special s) {
        AttackDefinition.Mechanics m = a.mechanics;
        float reach = a.reach + travel(a);
        if (s.projectile != null) {
            float range = s.projectile.spawnX + s.projectile.range;
            return new Option(id, a, s.projectile.trapFuse != null ? Role.TRAP : Role.PROJECTILE, 0f, range);
        }
        Role role;
        if (s.air || m.dives()) role = Role.DIVE;
        else if (m.grab) role = Role.GRAB;
        else if (m.repel) role = Role.REPEL;
        else if (m.counter != AttackDefinition.Reads.NONE) role = Role.COUNTER;
        else if (m.evade != AttackDefinition.Reads.NONE) role = Role.EVADE;
        else if (m.pull > 0f) role = Role.PULL;
        else if (m.echoFrames > 0) role = Role.ECHO;
        else if (m.armorHits > 0) role = Role.ARMOR;
        else if (a.launch == AttackDefinition.Launch.LAUNCH ||
            a.startupFrames <= 8 && a.activeFrames >= 7 && travel(a) < 120f && a.hitHeight >= 120f) role = Role.ANTI_AIR;
        else if (a.low) role = Role.LOW;
        else if (travel(a) >= 120f || m.teleportFrame >= 0) role = Role.RUSH;
        else role = Role.POKE;
        return new Option(id, a, role, reach, 0f);
    }

    /** Quanto o avanço do golpe leva o corpo para a frente até o fim do ativo. */
    static float travel(AttackDefinition a) {
        AttackDefinition.Mechanics m = a.mechanics;
        if (m.advanceSpeed <= 0f) return 0f;
        int end = Math.min(m.advanceEnd, a.startupFrames + a.activeFrames - 1);
        int frames = end - m.advanceStart + 1;
        return frames <= 0 ? 0f : m.advanceSpeed * frames * CombatConfig.DT;
    }

    /** O golpe corpo a corpo pega o rival de corpo {@code target} a {@code gap} (centro a centro)? */
    static boolean reaches(Option o, float gap, CharacterDefinition.Body target) {
        return gap >= CombatRules.MIN_MELEE_DISTANCE && gap <= o.reach + target.halfWidth;
    }

    /** A postura lê esse golpe (as regras do motor: alto = em pé e no ar; baixo = baixo no chão). */
    static boolean reads(AttackDefinition.Reads reads, AttackDefinition attack, boolean airborneAttacker) {
        switch (reads) {
            case ANY: return true;
            case HIGH: return airborneAttacker || !attack.low;
            case LOW: return attack.low && !airborneAttacker;
            default: return false;
        }
    }

    /**
     * Apertando agora, a janela [start, end] do golpe (em quadros dele) cobre o contato que vem
     * daqui a {@code arrival} quadros, durante {@code length} quadros?
     */
    static boolean covers(int start, int end, float arrival, int length) {
        return arrival >= start - 0.5f && arrival + Math.min(length, 3) - 1 <= end + 0.5f;
    }
}
