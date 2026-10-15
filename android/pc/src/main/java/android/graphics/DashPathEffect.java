package android.graphics;

/** Tracejado: traços e espaços alternados, em pixels do desenho. */
public class DashPathEffect extends PathEffect {
    final float[] intervals;
    final float phase;

    public DashPathEffect(float[] intervals, float phase) {
        if (intervals.length < 2 || intervals.length % 2 != 0) throw new ArrayIndexOutOfBoundsException();
        this.intervals = intervals.clone();
        this.phase = phase;
    }
}
