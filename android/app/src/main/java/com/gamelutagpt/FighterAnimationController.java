package com.gamelutagpt;

/**
 * Pequena maquina de estados de animacao.
 *
 * stateTime zera sempre que o estado muda; isso permite criar antecipacao,
 * impacto, recovery, loops e transicoes independentes do timer de gameplay.
 */
final class FighterAnimationController {
    private FighterAnimationState state = FighterAnimationState.IDLE;
    private FighterAnimationState previousState = FighterAnimationState.IDLE;
    private float stateTime = 0f;
    private float actionPhase = 0f;

    void update(
        FighterAnimationState nextState,
        float dt,
        float nextActionPhase
    ) {
        if (nextState == null) nextState = FighterAnimationState.IDLE;

        if (nextState != state) {
            previousState = state;
            state = nextState;
            stateTime = 0f;
        } else {
            stateTime += Math.max(0f, dt);
        }

        actionPhase = clamp01(nextActionPhase);
    }

    FighterAnimationState state() {
        return state;
    }

    FighterAnimationState previousState() {
        return previousState;
    }

    float stateTime() {
        return stateTime;
    }

    float actionPhase() {
        return actionPhase;
    }

    String debugLabel() {
        return state.name();
    }

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
