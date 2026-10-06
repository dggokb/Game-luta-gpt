package com.gamelutagpt;

/**
 * Recent D-pad directions (relative to facing) used to recognise special commands.
 * Pure Java: timing comes from the event clock passed in.
 */
final class CommandBuffer {
    static final int SIZE = 8;
    /** Max gap between two steps of a command, and per-step budget for the whole command. */
    static final long STEP_MS = 420L;

    private final int[] directions = new int[SIZE];
    private final long[] times = new long[SIZE];
    private int count;

    void reset() {
        count = 0;
    }

    int size() {
        return count;
    }

    void record(int direction, long nowMs) {
        if (direction == 0) return;

        if (count > 0 && directions[count - 1] == direction) {
            times[count - 1] = nowMs;
            return;
        }

        if (count >= SIZE) {
            for (int i = 1; i < SIZE; i++) {
                directions[i - 1] = directions[i];
                times[i - 1] = times[i];
            }
            count = SIZE - 1;
        }

        directions[count] = direction;
        times[count] = nowMs;
        count++;
    }

    /**
     * Whether the buffer ends with {@code command} performed in time. Intermediate
     * directions are allowed (↓ ↘ → still matches ↓ →). A match consumes the buffer.
     */
    boolean consume(int[] command, long nowMs) {
        if (command.length == 0 || count < command.length) return false;

        int commandIndex = command.length - 1;
        int bufferIndex = count - 1;
        long lastMatchedTime = -1L;
        long firstMatchedTime = -1L;

        while (commandIndex >= 0 && bufferIndex >= 0) {
            if (directions[bufferIndex] == command[commandIndex]) {
                long matchedTime = times[bufferIndex];
                if (lastMatchedTime > 0L && lastMatchedTime - matchedTime > STEP_MS) {
                    return false;
                }
                lastMatchedTime = matchedTime;
                firstMatchedTime = matchedTime;
                commandIndex--;
            }
            bufferIndex--;
        }

        if (commandIndex >= 0) return false;
        if (nowMs - times[count - 1] > STEP_MS) return false;
        if (nowMs - firstMatchedTime > STEP_MS * command.length) return false;
        count = 0;
        return true;
    }
}
