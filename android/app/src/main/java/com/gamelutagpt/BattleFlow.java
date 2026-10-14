package com.gamelutagpt;

/**
 * Fluxo da luta depois da intro, em frames (Java puro, sem Android):
 *
 * <ol>
 *   <li><b>READY → GO</b> (nomes provisórios): entre a intro e o idle os dois ficam parados no
 *   READY; no GO o controle volta.</li>
 *   <li><b>Modo batalha</b> (IA ON): a luta de verdade, 2x2. O titular que cai fica no chão
 *   um pouco e o parceiro que ainda tem vida entra no lugar dele; o time só perde quando os
 *   dois da dupla caem. Depois vem o K.O., o resultado e (fora da história) a luta recomeça.</li>
 * </ol>
 *
 * Fora do modo batalha (o treino, IA OFF) só o READY → GO vale: o KO não encerra nada.
 */
final class BattleFlow {
    static final int READY_FRAMES = 60;
    static final int GO_FRAMES = 40;
    /** K.O. na tela antes do resultado. */
    static final int KO_FRAMES = 90;
    /** Resultado na tela antes de a luta recomeçar (fora da história). */
    static final int RESULT_FRAMES = 150;

    /** Ninguém venceu ainda; ou os dois times caíram juntos. */
    static final int NONE = -1, DRAW = 2;

    enum Phase {
        /** Esperando a intro acabar. */
        INTRO,
        READY,
        GO,
        FIGHT,
        /** Um time caiu inteiro: K.O. e resultado. */
        OVER
    }

    private boolean battle;
    private Phase phase = Phase.FIGHT;
    private int phaseFrame;
    /** Frames que o titular caído de cada lado já está no chão. */
    private final int[] downFrames = new int[2];
    private int winner = NONE;

    /** IA ON: as regras de verdade (2x2, derrota quando a dupla cai). */
    void setBattle(boolean value) { battle = value; }

    boolean battle() { return battle; }

    Phase phase() { return phase; }

    /** Uma intro nova começou: o READY espera ela acabar. */
    void reset() {
        phase = Phase.INTRO;
        phaseFrame = 0;
        downFrames[0] = downFrames[1] = 0;
        winner = NONE;
    }

    /** A intro acabou: READY. */
    void startCountdown() {
        phase = Phase.READY;
        phaseFrame = 0;
    }

    /** No READY ninguém se mexe (como na intro). */
    boolean holdsFighters() { return phase == Phase.READY; }

    boolean over() { return phase == Phase.OVER; }

    /** Lado que venceu (0 ou 1), {@link #DRAW} ou {@link #NONE} enquanto a luta segue. */
    int winner() { return winner; }

    /** O resultado já ficou o bastante na tela: hora de começar a luta de novo. */
    boolean restartDue() { return phase == Phase.OVER && phaseFrame >= KO_FRAMES + RESULT_FRAMES; }

    /**
     * Um frame depois do passo do motor. {@code canReplace}: nada segura a luta agora (o raio
     * do ultra ainda no ar, por exemplo); o parceiro só entra quando ela está livre.
     */
    void update(CombatEngine engine, boolean canReplace) {
        phaseFrame++;
        if (phase == Phase.READY && phaseFrame >= READY_FRAMES) {
            phase = Phase.GO;
            phaseFrame = 0;
        } else if (phase == Phase.GO && phaseFrame >= GO_FRAMES) {
            phase = Phase.FIGHT;
            phaseFrame = 0;
        }
        if (!battle || (phase != Phase.GO && phase != Phase.FIGHT)) return;
        boolean firstDown = engine.teams.defeated(0), secondDown = engine.teams.defeated(1);
        if (firstDown || secondDown) {
            winner = firstDown && secondDown ? DRAW : firstDown ? 1 : 0;
            phase = Phase.OVER;
            phaseFrame = 0;
            return;
        }
        for (int side = 0; side < 2; side++) {
            CombatFighter f = engine.fighter(side);
            if (!f.ko()) {
                downFrames[side] = 0;
                continue;
            }
            // Conta só depois que ele bateu no chão.
            if (f.grounded) downFrames[side]++;
            if (canReplace && downFrames[side] >= engine.config.koReplaceFrames && engine.replaceFallenPoint(side)) {
                downFrames[side] = 0;
            }
        }
    }

    /** Chamada grande no centro da tela agora, ou null. */
    String banner() {
        switch (phase) {
            case READY: return "READY";
            case GO: return "GO!";
            case OVER:
                if (phaseFrame < KO_FRAMES) return "K.O.";
                return winner == 0 ? "VITÓRIA" : winner == 1 ? "DERROTA" : "EMPATE";
            default: return null;
        }
    }

    /** 0 quando a chamada atual aparece, 1 quando ela some. */
    float bannerProgress() {
        int length;
        int frame = phaseFrame;
        switch (phase) {
            case READY: length = READY_FRAMES; break;
            case GO: length = GO_FRAMES; break;
            case OVER:
                if (frame < KO_FRAMES) {
                    length = KO_FRAMES;
                } else {
                    frame -= KO_FRAMES;
                    length = RESULT_FRAMES;
                }
                break;
            default: return 1f;
        }
        return Math.min(1f, frame / (float)length);
    }
}
