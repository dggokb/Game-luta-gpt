package android.graphics;

/** Só as operações de recorte ({@link Canvas#clipRect(RectF, Op)}). */
public class Region {
    public enum Op { DIFFERENCE, INTERSECT, UNION, XOR, REVERSE_DIFFERENCE, REPLACE }
}
