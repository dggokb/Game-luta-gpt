package com.gamelutagpt;

/**
 * A entrada da troca (↓ + TAG) como no Marvel vs. Capcom: quem entra vem do alto, atrás de quem
 * saiu, numa voadora em diagonal até o rival. Acertou: o rival sai girando (o giro do punish
 * counter) e cai deitado; defendida ou no vento: cai no chão com uma recuperação curta e já
 * está na luta. Igual para todos os lutadores, montada do chute no ar (jH, ou jM) de cada um
 * até cada um ganhar a animação própria.
 *
 * <p>É um mergulho ({@link AttackDefinition.Mechanics#dive}): parado na preparação, depois desce
 * na diagonal batendo a descida inteira até o chão. A velocidade para frente sai na hora, para
 * chegar no rival quando toca o chão. O desenho segura a pose do chute durante a descida.
 */
final class VoadoraTroca {
    private VoadoraTroca() {}

    static final String ID = "TAG_KICK";

    // Tempos em quadros de 60 Hz. A descida (ativo) acaba no chão antes do fim.
    static final int STARTUP = 4, ACTIVE = 60, RECOVERY = 14;
    static final int HITSTUN = 30, BLOCKSTUN = 16;
    /** Dano em relação ao jH (por mil). */
    static final int DAMAGE_PERMILLE = 1100;
    /** De onde ela sai: altura acima do chão e distância atrás de quem estava na luta. */
    static final float HEIGHT = 330f, BEHIND = 200f;
    /** Velocidade de descida e os limites da velocidade para frente (unidades por segundo). */
    static final float DESCENT = 900f, MIN_FORWARD = 250f, MAX_FORWARD = 1500f;
    /**
     * Caixa do chute: da raiz até o alcance, e de abaixo dos pés (desce em cima do rival) até a
     * altura do quadril.
     */
    static final float HIT_BELOW = -90f, HIT_TOP = 110f, EXTRA_REACH = 30f;

    /** Quadros que a descida de {@link #HEIGHT} leva. */
    static int descentFrames() {
        return Math.round(HEIGHT / DESCENT * CombatConfig.FPS);
    }

    /**
     * Velocidade para frente que leva quem sai de {@code fromX} até perto do rival em
     * {@code rivalX} quando toca o chão.
     */
    static float forwardSpeed(float fromX, float rivalX, float closeGap) {
        float distance = Math.max(0f, Math.abs(rivalX - fromX) - closeGap);
        float seconds = HEIGHT / DESCENT;
        return Math.max(MIN_FORWARD, Math.min(MAX_FORWARD, distance / seconds));
    }

    /** A voadora montada do chute no ar do lutador, ou null se ele não tem nenhum. */
    static CharacterDefinition.Move from(CharacterDefinition c, float forward) {
        CharacterDefinition.Move kick = c.moves.get("jH");
        if (kick == null) kick = c.moves.get("jM");
        if (kick == null) return null;
        AttackDefinition k = kick.attack;
        AttackDefinition attack = new AttackDefinition.Builder(ID, AttackDefinition.Kind.NORMAL)
            .damage(Math.max(1, k.damage * DAMAGE_PERMILLE / 1000))
            .frames(STARTUP, ACTIVE, RECOVERY)
            .stun(HITSTUN, BLOCKSTUN, k.hitstopFrames)
            .windows(null, null, null)
            .pushback(k.pushbackOnHit, k.pushbackOnBlock)
            .reach(k.reach + EXTRA_REACH, HIT_TOP)
            .hitbox(STARTUP, STARTUP + ACTIVE - 1, -20f, k.reach + EXTRA_REACH, HIT_BELOW, HIT_TOP)
            .mechanics(new AttackDefinition.Mechanics().dive(forward, DESCENT))
            .spin(true)
            .build();
        float[][] knots = holdKnots(kick, attack);
        return new CharacterDefinition.Move(ID, kick.animation, kick.pose, attack,
            knots == null ? null : knots[0], knots == null ? null : knots[1]);
    }

    /**
     * Trechos do clipe do chute: a preparação no ritmo dele até o impacto, a pose do chute
     * segurada a descida inteira e a volta no fim. Null se o chute não tem desenho.
     */
    static float[][] holdKnots(CharacterDefinition.Move kick, AttackDefinition voadora) {
        if (kick.animation == null) return null;
        AttackDefinition k = kick.attack;
        float[] frames = {
            0f,
            voadora.startupFrames,
            voadora.startupFrames + voadora.activeFrames,
            voadora.totalFrames
        };
        float[] times = {
            0f,
            kick.animationTime(k.startupFrames / (float) CombatConfig.FPS),
            kick.animationTime((k.startupFrames + Math.max(1, k.activeFrames - 1)) / (float) CombatConfig.FPS),
            kick.animation.duration - 0.0001f
        };
        for (int i = 1; i < frames.length; i++) {
            if (frames[i] < frames[i - 1]) frames[i] = frames[i - 1];
            if (times[i] < times[i - 1]) times[i] = times[i - 1];
        }
        return new float[][] {frames, times};
    }
}
