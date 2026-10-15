package android.graphics;

public final class Rect {
    public int left, top, right, bottom;

    public Rect() {}

    public Rect(int left, int top, int right, int bottom) { set(left, top, right, bottom); }

    public Rect(Rect r) { if (r != null) set(r.left, r.top, r.right, r.bottom); }

    public void set(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public void set(Rect r) { set(r.left, r.top, r.right, r.bottom); }

    public void setEmpty() { set(0, 0, 0, 0); }

    public boolean isEmpty() { return left >= right || top >= bottom; }

    public int width() { return right - left; }

    public int height() { return bottom - top; }

    public int centerX() { return (left + right) >> 1; }

    public int centerY() { return (top + bottom) >> 1; }

    public boolean contains(int x, int y) { return left < right && top < bottom && x >= left && x < right && y >= top && y < bottom; }

    public void offset(int dx, int dy) { left += dx; right += dx; top += dy; bottom += dy; }

    public void inset(int dx, int dy) { left += dx; right -= dx; top += dy; bottom -= dy; }

    @Override public String toString() { return "Rect(" + left + ", " + top + " - " + right + ", " + bottom + ")"; }
}
