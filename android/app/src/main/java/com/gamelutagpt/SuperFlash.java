package com.gamelutagpt;

/**
 * Apresentação do Super de quem o ativou: o jogador, a CPU ou o parceiro que entra num DHC.
 * O cenário escurece, a câmera aproxima, a aura de carga cresce e a liberação pisca branco.
 * Nos primeiros quadros vem o aviso de ativação (o corpo de quem ativou brilha na cor dele; quem
 * desenha põe o brilho no peito e toca o som em {@link #started}). Quem ficou parado no
 * congelamento escurece, para não parecer jogo travado.
 *
 * Quem tem entrada do Super ({@link CombatEngine#superIntro}) primeiro mostra o retrato ao fundo,
 * grudado na borda do lado em que está, com tudo parado; o escuro e o aviso correm nesse tempo e,
 * quando ele acaba, o resto da cinemática segue de onde o escuro parou.
 *
 * Só lê o motor: nada aqui muda a luta. Um {@link #update} por passo de 60 Hz.
 */
final class SuperFlash {
    // Fases da cinemática, em segundos do Super (o tempo vem dos quadros do golpe).
    static final float DARKEN = 0.16f, POSE = 0.30f, FLASH = 0.12f, RELEASE = 0.20f, RECOVER = 0.22f;
    /** A cinemática do Super corpo a corpo toca mais rápido (as pancadas vêm logo depois do flash). */
    static final float STRIKING_SPEED = 2.4f;
    /** Zoom a mais na entrada do Super (o mesmo do fim da pose: o foco inteiro em quem ativou). */
    static final float INTRO_ZOOM = 0.36f;
    /** Escuro máximo do cenário (0–255). */
    static final int DARK = 190;
    /** Quadros do aviso de ativação: o corpo de quem ativou brilha e apaga. */
    static final int SIGNAL_FRAMES = 8;
    /** Quanto o congelado escurece por passo, entrando e saindo (sem pular de uma vez). */
    static final float DIM_IN = 0.2f, DIM_OUT = 0.12f;
    private static final float STEP = 1f / 60f;

    /** Quem faz o Super agora (0 ou 1), ou -1. */
    int user = -1;
    /** Um Super começou neste passo (o lutador está em {@link #user}). */
    boolean started;
    float zoom = 1f, chargeTime;
    int darkAlpha, flashAlpha;
    /** Brilho do corpo de quem ativou, de 0 a 1 (só no aviso). */
    float signal;
    /** Escurecido de quem está congelado pelo Super, de 0 a 1. */
    float frozenDim;
    /** O lutador escurecido (o que não fez o Super), ou -1. */
    int frozen = -1;
    /** Passos da entrada do Super em curso (0 = sem entrada) e quantos já passaram. */
    int intro, introAge;
    /** A entrada fica na borda esquerda da tela (quem ativou está à esquerda do rival). */
    boolean introLeft;
    /** Carga que a aura já tinha quando a entrada acabou (a aura segue sem pular). */
    private float introCharge;

    private final AttackDefinition[] seenAttack = new AttackDefinition[2];
    private final int[] seenFrame = new int[2];

    void reset() {
        user = -1;
        started = false;
        zoom = 1f;
        chargeTime = 0f;
        darkAlpha = flashAlpha = 0;
        signal = frozenDim = 0f;
        frozen = -1;
        intro = introAge = 0;
        introCharge = 0f;
        seenAttack[0] = seenAttack[1] = null;
    }

    boolean active() {
        return user >= 0;
    }

    /** O retrato da entrada está na tela. */
    boolean introShowing() {
        return user >= 0 && intro > 0 && introAge <= intro;
    }

    private static boolean doingSuper(CombatFighter f) {
        return f.attacking() && f.attack.kind == AttackDefinition.Kind.SUPER;
    }

    void update(CombatEngine engine) {
        started = false;
        int now = -1;
        for (int i = 0; i < 2; i++) {
            CombatFighter f = engine.fighter(i);
            if (!doingSuper(f)) {
                seenAttack[i] = null;
                continue;
            }
            // Começou agora: outro golpe antes, ou o mesmo Super de novo do quadro zero.
            if (seenAttack[i] != f.attack || f.attackFrame < seenFrame[i]) {
                started = true;
                now = i;
            }
            seenAttack[i] = f.attack;
            seenFrame[i] = f.attackFrame;
        }
        // Os dois em Super: vale o mais recente.
        if (now >= 0) {
            user = now;
            CombatFighter f = engine.fighter(now);
            intro = engine.superIntro(f);
            introAge = 0;
            introCharge = 0f;
            introLeft = f.x <= engine.fighter(1 - now).x;
        } else if (user < 0 || !doingSuper(engine.fighter(user))) {
            user = -1;
            for (int i = 0; i < 2; i++) if (doingSuper(engine.fighter(i))) user = i;
        }
        updateFrozen(engine);
        if (user < 0) {
            zoom = 1f;
            darkAlpha = flashAlpha = 0;
            chargeTime = 0f;
            signal = 0f;
            intro = introAge = 0;
            return;
        }
        CombatFighter f = engine.fighter(user);
        if (introAge < intro) {
            // Entrada: tudo parado, o escuro chega e a aura carrega enquanto o retrato passa. A
            // câmera já vai até o foco inteiro (quem ativou no meio da tela, o quadro ao lado).
            introAge++;
            float t = introAge * STEP;
            signal = introAge <= SIGNAL_FRAMES ? 1f - (introAge - 1) / (float)SIGNAL_FRAMES : 0f;
            float k = clamp(t / DARKEN);
            darkAlpha = Math.round(DARK * k);
            float z = clamp(t / (DARKEN + POSE));
            zoom = 1f + INTRO_ZOOM * z * (2f - z);
            flashAlpha = 0;
            chargeTime = introCharge = Math.max(0f, t - DARKEN);
            return;
        }
        if (intro > 0) introAge = intro + 1;
        int frame = f.attackFrame;
        signal = intro == 0 && frame < SIGNAL_FRAMES ? 1f - frame / (float)SIGNAL_FRAMES : 0f;
        float t = (frame + 1) * STEP;
        if (f.attack.melee()) t *= STRIKING_SPEED;
        // Depois da entrada o escuro já chegou: a cinemática segue da pose.
        if (intro > 0) t += DARKEN;
        float poseStart = DARKEN;
        float flashStart = poseStart + POSE;
        float releaseStart = flashStart + FLASH;
        float recoverStart = releaseStart + RELEASE;
        chargeTime = introCharge + Math.max(0f, t - poseStart);
        if (t < poseStart) {
            float k = clamp(t / DARKEN);
            darkAlpha = Math.round(DARK * k);
            zoom = 1f + 0.18f * k;
            flashAlpha = 0;
        } else if (t < flashStart) {
            float k = clamp((t - poseStart) / POSE);
            darkAlpha = DARK;
            zoom = intro > 0 ? 1f + INTRO_ZOOM : 1.18f + 0.18f * k;
            flashAlpha = 0;
        } else if (t < releaseStart) {
            float k = clamp((t - flashStart) / FLASH);
            darkAlpha = DARK;
            zoom = 1.36f;
            flashAlpha = Math.round(235f * (1f - Math.abs(0.5f - k) * 2f));
        } else if (t < recoverStart) {
            float k = clamp((t - releaseStart) / RELEASE);
            darkAlpha = Math.round(DARK * (1f - 0.40f * k));
            flashAlpha = Math.round(255f * (1f - k));
            zoom = 1.36f - 0.16f * k;
        } else {
            float k = clamp((t - recoverStart) / RECOVER);
            darkAlpha = Math.round(DARK * 0.6f * (1f - k));
            flashAlpha = 0;
            zoom = 1.20f - 0.20f * k;
        }
    }

    /** Quem o Super segurou escurece enquanto está parado e volta aos poucos depois. */
    private void updateFrozen(CombatEngine engine) {
        boolean held = false;
        if (user >= 0 && engine.superFreezeActive()) {
            CombatFighter other = engine.fighter(1 - user);
            if (other.hitstop > 0) {
                held = true;
                frozen = other.index;
            }
        }
        frozenDim = held ? Math.min(1f, frozenDim + DIM_IN) : Math.max(0f, frozenDim - DIM_OUT);
        if (frozenDim == 0f) frozen = -1;
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
