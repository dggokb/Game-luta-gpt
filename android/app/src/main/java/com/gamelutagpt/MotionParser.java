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

    void clear() {
        count = 0;
    }

    /** Records the held direction; only changes enter the history. */
    void record(int relativeDirection, int clock) {
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
