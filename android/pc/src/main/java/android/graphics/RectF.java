package android.graphics;

public class RectF {
    public float left, top, right, bottom;

    public RectF() {}

    public RectF(float left, float top, float right, float bottom) { set(left, top, right, bottom); }

    public RectF(RectF r) { if (r != null) set(r.left, r.top, r.right, r.bottom); }

    public RectF(Rect r) { if (r != null) set(r.left, r.top, r.right, r.bottom); }

    public final void set(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public final void set(RectF r) { set(r.left, r.top, r.right, r.bottom); }

    public final void set(Rect r) { set(r.left, r.top, r.right, r.bottom); }

    public final void setEmpty() { set(0, 0, 0, 0); }

    public final boolean isEmpty() { return left >= right || top >= bottom; }

    public final float width() { return right - left; }

    public final float height() { return bottom - top; }

    public final float centerX() { return (left + right) * 0.5f; }

    public final float centerY() { return (top + bottom) * 0.5f; }

    public boolean contains(float x, float y) { return left < right && top < bottom && x >= left && x < right && y >= top && y < bottom; }

    public void offset(float dx, float dy) { left += dx; right += dx; top += dy; bottom += dy; }

    public void offsetTo(float newLeft, float newTop) { right += newLeft - left; bottom += newTop - top; left = newLeft; top = newTop; }

    public void inset(float dx, float dy) { left += dx; right -= dx; top += dy; bottom -= dy; }

    public boolean intersect(float l, float t, float r, float b) {
        if (left < r && l < right && top < b && t < bottom) {
            if (left < l) left = l;
            if (top < t) top = t;
            if (right > r) right = r;
            if (bottom > b) bottom = b;
            return true;
        }
        return false;
    }

    public boolean intersect(RectF r) { return intersect(r.left, r.top, r.right, r.bottom); }

    public boolean intersects(float l, float t, float r, float b) { return left < r && l < right && top < b && t < bottom; }

    public static boolean intersects(RectF a, RectF b) { return a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom; }

    public void union(float l, float t, float r, float b) {
        if (l >= r || t >= b) return;
        if (left < right && top < bottom) {
            left = Math.min(left, l);
            top = Math.min(top, t);
            right = Math.max(right, r);
            bottom = Math.max(bottom, b);
        } else {
            set(l, t, r, b);
        }
    }

    public void union(RectF r) { union(r.left, r.top, r.right, r.bottom); }

    public void union(float x, float y) {
        if (x < left) left = x; else if (x > right) right = x;
        if (y < top) top = y; else if (y > bottom) bottom = y;
    }

    public void sort() {
        if (left > right) { float t = left; left = right; right = t; }
        if (top > bottom) { float t = top; top = bottom; bottom = t; }
    }

    public void round(Rect dst) { dst.set(Math.round(left), Math.round(top), Math.round(right), Math.round(bottom)); }

    @Override public String toString() { return "RectF(" + left + ", " + top + ", " + right + ", " + bottom + ")"; }
}
