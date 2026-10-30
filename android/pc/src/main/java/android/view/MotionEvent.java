package android.view;

/** Toque. No PC o mouse é o dedo 0 (botão esquerdo). */
public final class MotionEvent {
    public static final int ACTION_DOWN = 0, ACTION_UP = 1, ACTION_MOVE = 2, ACTION_CANCEL = 3;
    public static final int ACTION_POINTER_DOWN = 5, ACTION_POINTER_UP = 6;
    public static final int ACTION_MASK = 0xFF;
    public static final int ACTION_POINTER_INDEX_MASK = 0xFF00;
    public static final int ACTION_POINTER_INDEX_SHIFT = 8;

    private int action;
    private int[] ids;
    private float[] xs, ys;
    private long downTime, eventTime;

    private MotionEvent() {}

    public static MotionEvent obtain(long downTime, long eventTime, int action, float x, float y, int metaState) {
        return obtain(downTime, eventTime, action, new int[] {0}, new float[] {x}, new float[] {y});
    }

    /** Extensão do PC: vários dedos. */
    public static MotionEvent obtain(long downTime, long eventTime, int action, int[] pointerIds, float[] xs, float[] ys) {
        MotionEvent e = new MotionEvent();
        e.downTime = downTime;
        e.eventTime = eventTime;
        e.action = action;
        e.ids = pointerIds.clone();
        e.xs = xs.clone();
        e.ys = ys.clone();
        return e;
    }

    public static MotionEvent obtain(MotionEvent other) {
        return obtain(other.downTime, other.eventTime, other.action, other.ids, other.xs, other.ys);
    }

    public void recycle() {}

    public int getAction() { return action; }

    public int getActionMasked() { return action & ACTION_MASK; }

    public int getActionIndex() { return (action & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT; }

    public int getPointerCount() { return ids.length; }

    public int getPointerId(int pointerIndex) { return ids[pointerIndex]; }

    public int findPointerIndex(int pointerId) {
        for (int i = 0; i < ids.length; i++) if (ids[i] == pointerId) return i;
        return -1;
    }

    public float getX() { return xs[0]; }

    public float getY() { return ys[0]; }

    public float getX(int pointerIndex) { return xs[pointerIndex]; }

    public float getY(int pointerIndex) { return ys[pointerIndex]; }

    /** No PC cada evento traz um ponto só (sem os do meio que o Android junta). */
    public int getHistorySize() { return 0; }

    public float getHistoricalX(int pointerIndex, int pos) { return xs[pointerIndex]; }

    public float getHistoricalY(int pointerIndex, int pos) { return ys[pointerIndex]; }

    public long getDownTime() { return downTime; }

    public long getEventTime() { return eventTime; }

    @Override public String toString() { return "MotionEvent{action=" + action + ", x=" + xs[0] + ", y=" + ys[0] + "}"; }
}
