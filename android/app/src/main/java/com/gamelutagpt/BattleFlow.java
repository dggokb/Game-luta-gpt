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
 *   <li><b>Tempo</b>: a batalha tem {@value #TIME_LIMIT_SECONDS} segundos, contados a partir do
 *   GO. O relógio para no congelamento do Super e do Ultra (quem chama diz se ele corre). Se
 *   chegar a zero sem K.O.: TIME OVER e vence a dupla com mais vida, em proporção da vida total
 *   da dupla (o vermelho recuperável não conta); vida igual é empate. K.O. no mesmo quadro do
 *   zero vale como K.O.</li>
 * </ol>
 *
 * Fora do modo batalha (o treino, IA OFF) só o READY → GO vale: o KO não encerra nada e o
 * tempo é infinito.
 */
final class BattleFlow {
    static final int READY_FRAMES = 60;
    static final int GO_FRAMES = 40;
    /** K.O. (ou TIME OVER) na tela antes do resultado. */
    static final int KO_FRAMES = 90;
    /** Resultado na tela antes de a luta recomeçar (fora da história). */
    static final int RESULT_FRAMES = 150;
    /** Duração da batalha, em segundos do relógio. */
    static final int TIME_LIMIT_SECONDS = 120;
    /** Quadros por segundo do relógio (o motor roda a 60). */
    static final int FRAMES_PER_SECOND = 60;
    static final int TIME_LIMIT_FRAMES = TIME_LIMIT_SECONDS * FRAMES_PER_SECOND;
    /** Nos últimos segundos o relógio fica vermelho e pisca. */
    static final int URGENT_SECONDS = 10;
    private static final String INFINITE = "\u221E";
    private static final String[] DIGITS = new String[TIME_LIMIT_SECONDS + 1];
    static {
        for (int i = 0; i < DIGITS.length; i++) DIGITS[i] = Integer.toString(i);
    }

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
    /** Quadros que faltam no relógio da batalha. */
    private int clockFrames = TIME_LIMIT_FRAMES;
    /** A luta acabou porque o tempo zerou (não por K.O.). */
    private boolean timeOver;
    /** Uma cena da história segura a luta: o relógio não anda nem acaba (HUD mostra ∞). */
    private boolean clockHeld;

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
        clockFrames = TIME_LIMIT_FRAMES;
        timeOver = false;
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

    /** A luta acabou pelo tempo (TIME OVER), não por K.O. */
    boolean timeOver() { return timeOver; }

    /**
     * Uma cena da história segura a luta (espera a vida baixar ou o K.O. para começar, ou está
     * rodando): o relógio fica parado e não encerra a luta, e a HUD mostra ∞.
     */
    void setClockHeld(boolean value) { clockHeld = value; }

    /** A luta tem relógio agora (batalha e nenhuma cena segurando). */
    boolean timed() { return battle && !clockHeld; }

    /** Segundos no relógio (arredonda para cima: 120 no começo, 0 só quando acabou). */
    int clockSeconds() { return (clockFrames + FRAMES_PER_SECOND - 1) / FRAMES_PER_SECOND; }

    /** Texto do relógio da HUD: os segundos na batalha, ∞ no treino ou com a cena segurando. */
    String clockLabel() { return timed() ? DIGITS[clockSeconds()] : INFINITE; }

    /** Últimos segundos da batalha, com a luta ainda rolando. */
    boolean clockUrgent() {
        return timed() && clockSeconds() <= URGENT_SECONDS && (phase == Phase.GO || phase == Phase.FIGHT);
    }

    /** O resultado já ficou o bastante na tela: hora de começar a luta de novo. */
    boolean restartDue() { return phase == Phase.OVER && phaseFrame >= KO_FRAMES + RESULT_FRAMES; }

    /** {@link #update(CombatEngine, boolean, boolean)} com o relógio correndo. */
    void update(CombatEngine engine, boolean canReplace) { update(engine, canReplace, true); }

    /**
     * Um frame depois do passo do motor. {@code canReplace}: nada segura a luta agora (o raio
     * do ultra ainda no ar, por exemplo); o parceiro só entra quando ela está livre.
     * {@code clockRuns}: o relógio anda neste quadro (falso no congelamento do Super/Ultra).
     */
    void update(CombatEngine engine, boolean canReplace, boolean clockRuns) {
        // O relógio só anda nos quadros que já começaram com o controle solto (do GO em diante).
        boolean live = phase == Phase.GO || phase == Phase.FIGHT;
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
        if (!clockHeld) {
            if (live && clockRuns && clockFrames > 0) clockFrames--;
            if (clockFrames == 0) {
                // TIME OVER: vence quem tem mais vida; vida igual, empate.
                int more = engine.teams.moreLife();
                winner = more < 0 ? DRAW : more;
                timeOver = true;
                phase = Phase.OVER;
                phaseFrame = 0;
                return;
            }
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
                if (phaseFrame < KO_FRAMES) return timeOver ? "TIME OVER" : "K.O.";
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
