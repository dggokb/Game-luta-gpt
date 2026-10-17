package com.gamelutagpt;

import java.util.ArrayList;
import java.util.List;

/**
 * Golpe alto de chão (→ + M), igual para todos os lutadores: pega quem defende agachado, só se
 * defende em pé. Lento de propósito (dá para reagir vendo o brilho do aviso) e, no acerto,
 * cancela nos especiais e no Super como o H. Sai só do neutro: dentro de um combo o → + M
 * continua sendo o M.
 *
 * O golpe é montado a partir do M de cada personagem até cada um ganhar a animação própria (o
 * pack pode declarar o seu "6M" e então esse vale). O desenho do M não é esticado (ficaria em
 * câmera lenta): a preparação roda no ritmo do M até a pose de antes do impacto, segura essa
 * pose (o "carregar" do golpe alto, que dá para ler) e a batida e a volta seguem no ritmo do M.
 */
final class GolpeAlto {
    private GolpeAlto() {}

    static final String BINDING = "6M";

    // Tempos em quadros de 60 Hz.
    static final int STARTUP = 20, ACTIVE = 3, RECOVERY = 14;
    /** No acerto: +3 (dá para ligar um L); na defesa: −6 (punível pelos golpes rápidos). */
    static final int HITSTUN = 20, BLOCKSTUN = 11;
    /** Dano em relação ao M (por mil). */
    static final int DAMAGE_PERMILLE = 1150;
    /**
     * Caixa do acerto: da base até a altura (mais a folga), baixa o bastante para pegar o
     * agachado mais baixo do elenco e alta o bastante para pegar quem está em pé.
     */
    static final float HIT_FLOOR = 40f, HIT_HEIGHT = 110f;

    /** {@code binding} é o 6M montado aqui (não veio do pacote do personagem). */
    static boolean derived(CharacterDefinition c, String binding) {
        if (!BINDING.equals(binding)) return false;
        CharacterDefinition.Move overhead = c.moves.get(BINDING), medium = c.moves.get("M");
        return overhead != null && medium != null && overhead.animation == medium.animation &&
            overhead.attack.startupFrames == STARTUP && overhead.attack.overhead;
    }

    /** O 6M montado do M (e do H, para os cancelamentos), ou null se faltar o M. */
    static CharacterDefinition.Move from(CharacterDefinition.Move medium, CharacterDefinition.Move heavy) {
        if (medium == null) return null;
        AttackDefinition m = medium.attack;
        List<String> targets = new ArrayList<>();
        if (heavy != null) {
            for (String t : heavy.attack.cancelInto()) {
                if (t.equals("SUPER") || t.equals("S") || t.matches("S[2-9]")) targets.add(t);
            }
        }
        AttackDefinition.Builder b = new AttackDefinition.Builder(BINDING, AttackDefinition.Kind.NORMAL)
            .damage(Math.max(1, m.damage * DAMAGE_PERMILLE / 1000))
            .frames(STARTUP, ACTIVE, RECOVERY)
            .stun(HITSTUN, BLOCKSTUN, m.hitstopFrames)
            .windows(targets.isEmpty() ? null : AttackDefinition.window(STARTUP, STARTUP + ACTIVE + 8), null, null)
            .cancelInto(targets.toArray(new String[0]))
            .pushback(m.pushbackOnHit, m.pushbackOnBlock)
            .maxUsesPerCombo(1)
            .overhead(true)
            .reach(m.reach, HIT_HEIGHT, HIT_FLOOR);
        AttackDefinition overhead = b.build();
        float[][] knots = holdKnots(medium, overhead);
        return new CharacterDefinition.Move(BINDING, medium.animation, medium.pose, overhead,
            knots == null ? null : knots[0], knots == null ? null : knots[1]);
    }

    /**
     * Trechos do clipe do M no 6M: a preparação do M no ritmo dele até 2 quadros antes do
     * impacto, a pose segurada até o impacto do 6M e daí em diante o resto do M no ritmo dele
     * (a volta, se o 6M for mais longo, um pouco mais lenta). Null se o M não tem desenho.
     */
    static float[][] holdKnots(CharacterDefinition.Move medium, AttackDefinition overhead) {
        if (medium.animation == null) return null;
        AttackDefinition m = medium.attack;
        int windUp = Math.max(0, Math.min(m.startupFrames - 2, overhead.startupFrames - 2));
        float[] frames = {
            0f,
            windUp,
            overhead.startupFrames,
            overhead.startupFrames + overhead.activeFrames,
            overhead.totalFrames
        };
        float[] times = {
            0f,
            medium.animationTime(windUp / (float) CombatConfig.FPS),
            medium.animationTime(m.startupFrames / (float) CombatConfig.FPS),
            medium.animationTime((m.startupFrames + m.activeFrames) / (float) CombatConfig.FPS),
            medium.animation.duration - 0.0001f
        };
        for (int i = 1; i < frames.length; i++) {
            if (frames[i] < frames[i - 1]) frames[i] = frames[i - 1];
            if (times[i] < times[i - 1]) times[i] = times[i - 1];
        }
        return new float[][] {frames, times};
    }
}
