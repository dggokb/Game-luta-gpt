package com.gamelutagpt;

/**
 * Direction history of one fighter, in its own frame clock (frozen during hitstop) and
 * relative to its facing (1 = forward, 5 = back). It recognises special commands, double
 * taps (dash/backdash) and down→up (super jump).
 */
final class MotionParser {
    static final int SIZE = 16;

    private final int[] directions = new int[SIZE];
    private final int[] frames = new int[SIZE];
    private int count;

    // Carga ([←] → e [↓] ↑): desde quando o grupo está segurado (-1 solto), quanto durou a
    // última vez e quando foi solto.
    private int backSince = -1, downSince = -1;
    private int backHeld, downHeld;
    private int backReleased = Integer.MIN_VALUE / 2, downReleased = Integer.MIN_VALUE / 2;

    void clear() {
        count = 0;
        backSince = downSince = -1;
        backHeld = downHeld = 0;
    }

    /** Records the held direction; only changes enter the history. */
    void record(int relativeDirection, int clock) {
        trackCharge(relativeDirection, clock);
        if (count > 0 && directions[count - 1] == relativeDirection) return;
        if (count >= SIZE) {
            System.arraycopy(directions, 1, directions, 0, SIZE - 1);
            System.arraycopy(frames, 1, frames, 0, SIZE - 1);
            count = SIZE - 1;
        }
        directions[count] = relativeDirection;
        frames[count] = clock;
        count++;
    }

    private void trackCharge(int d, int clock) {
        if (isBack(d)) {
            if (backSince < 0) backSince = clock;
        } else if (backSince >= 0) {
            backHeld = clock - backSince;
            backReleased = clock;
            backSince = -1;
        }
        if (isDown(d)) {
            if (downSince < 0) downSince = clock;
        } else if (downSince >= 0) {
            downHeld = clock - downSince;
            downReleased = clock;
            downSince = -1;
        }
    }

    static boolean isBack(int d) { return d == 4 || d == 5 || d == 6; }
    static boolean isDown(int d) { return d == 2 || d == 3 || d == 4; }
    static boolean isForward(int d) { return d == 8 || d == 1 || d == 2; }
    static boolean isUp(int d) { return d == 6 || d == 7 || d == 8; }

    /** Direção segurada agora (0 sem história). */
    int current() {
        return count > 0 ? directions[count - 1] : 0;
    }

    /**
     * Carga pronta: trás (ou baixo) segurado por {@code chargeFrames}, ainda segurado ou solto há
     * no máximo {@code keepFrames}.
     */
    boolean charged(boolean down, int clock, int chargeFrames, int keepFrames) {
        int since = down ? downSince : backSince;
        if (since >= 0) return clock - since >= chargeFrames;
        int held = down ? downHeld : backHeld, released = down ? downReleased : backReleased;
        return held >= chargeFrames && clock - released <= keepFrames;
    }

    /**
     * Passos que o jogador precisa fazer: a diagonal entre duas direções vizinhas é opcional
     * (↓ ↘ → vale como ↓ →, a meia-lua ← ↙ ↓ ↘ → como ← ↓ →). A diagonal no fim conta (→ ↓ ↘).
     */
    static int[] requiredSteps(int[] command) {
        int[] out = new int[command.length];
        int n = 0;
        for (int i = 0; i < command.length; i++) {
            int d = command[i];
            boolean diagonal = d % 2 == 0;
            if (diagonal && i > 0 && i < command.length - 1 && neighbours(d, command[i - 1]) &&
                neighbours(d, command[i + 1]) && command[i - 1] != command[i + 1]) continue;
            out[n++] = d;
        }
        return java.util.Arrays.copyOf(out, n);
    }

    private static boolean neighbours(int a, int b) {
        int diff = Math.abs(a - b);
        return diff == 1 || diff == 7;
    }

    /**
     * Onde termina o comando na história, sem consumir: 0 quando o último passo é a direção mais
     * recente, 1 a anterior..., ou -1 se não bate. {@code steps} vem de {@link #requiredSteps};
     * os passos ficam a até {@code motionWindow} quadros entre o primeiro e o último, e o último
     * a até {@code pressWindow} quadros do botão.
     */
    int commandEnd(int[] steps, int clock, int motionWindow, int pressWindow) {
        if (steps.length == 0) return -1;
        int step = steps.length - 1;
        int lastIndex = -1, lastFrame = -1, firstFrame = -1;
        for (int i = count - 1; i >= 0 && step >= 0; i--) {
            if (directions[i] != steps[step]) continue;
            if (lastIndex < 0) {
                lastIndex = i;
                lastFrame = frames[i];
            }
            firstFrame = frames[i];
            step--;
        }
        if (step >= 0) return -1;
        if (clock - lastFrame > pressWindow) return -1;
        if (lastFrame - firstFrame > motionWindow) return -1;
        return count - 1 - lastIndex;
    }

    /**
     * Comando de segurar: {@code [5, 1]} = segura trás e aperta frente, {@code [3, 7]} = segura
     * baixo e aperta cima (diagonais valem). Verdadeiro com a carga pronta e a direção de soltar
     * segurada agora.
     */
    boolean holdCommand(int[] command, int clock, int chargeFrames, int keepFrames) {
        boolean down = command[0] == 3;
        int now = current();
        boolean release = down ? isUp(now) : isForward(now);
        return release && charged(down, clock, chargeFrames, keepFrames);
    }

    /** Um comando saiu: a história e as cargas recomeçam (a mesma entrada não solta outro). */
    void consume(int clock) {
        count = 0;
        backHeld = downHeld = 0;
        if (backSince >= 0) backSince = clock;
        if (downSince >= 0) downSince = clock;
    }

    /** Whether the direction was entered on this very frame. */
    boolean enteredNow(int clock) {
        return count > 0 && frames[count - 1] == clock;
    }

    /**
     * Whether the history ends with {@code command} (other directions may sit in between,
     * so ↓ ↘ → matches ↓ →): steps within {@code motionWindow} frames of each other and
     * the last one at most {@code pressWindow} frames ago. A match consumes the history.
     */
    boolean matchCommand(int[] command, int clock, int motionWindow, int pressWindow) {
        if (command.length == 0) return false;
        int step = command.length - 1;
        int lastFrame = -1;
        int firstFrame = -1;
        for (int i = count - 1; i >= 0 && step >= 0; i--) {
            if (directions[i] != command[step]) continue;
            if (lastFrame < 0) lastFrame = frames[i];
            firstFrame = frames[i];
            step--;
        }
        if (step >= 0) return false;
        if (clock - lastFrame > pressWindow) return false;
        if (lastFrame - firstFrame > motionWindow) return false;
        count = 0;
        return true;
    }

    /** {@code direction} entered now, and also entered before within {@code window} frames. */
    boolean doubleTap(int direction, int clock, int window) {
        if (!enteredNow(clock) || directions[count - 1] != direction) return false;
        for (int i = count - 2; i >= 0; i--) {
            if (clock - frames[i] > window) return false;
            if (directions[i] == direction) return true;
        }
        return false;
    }

    /** An upward direction entered now after a downward one entered within {@code window}. */
    boolean downThenUp(int clock, int window) {
        if (!enteredNow(clock) || !ControlsLayout.isUpDirection(directions[count - 1])) return false;
        for (int i = count - 2; i >= 0; i--) {
            if (clock - frames[i] > window) return false;
            if (ControlsLayout.isDownDirection(directions[i])) return true;
        }
        return false;
    }

    /** Screen direction (0..8) seen from a fighter facing {@code facing}: 1 forward, 5 back. */
    static int relative(int screenDirection, int facing) {
        if (facing > 0) return screenDirection;
        switch (screenDirection) {
            case 1: return 5;
            case 2: return 4;
            case 4: return 2;
            case 5: return 1;
            case 6: return 8;
            case 8: return 6;
            default: return screenDirection;
        }
    }
}
