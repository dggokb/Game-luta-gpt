package android.graphics;

/** Cores ARGB empacotadas em int, como no Android. */
public class Color {
    public static final int BLACK = 0xFF000000;
    public static final int DKGRAY = 0xFF444444;
    public static final int GRAY = 0xFF888888;
    public static final int LTGRAY = 0xFFCCCCCC;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int RED = 0xFFFF0000;
    public static final int GREEN = 0xFF00FF00;
    public static final int BLUE = 0xFF0000FF;
    public static final int YELLOW = 0xFFFFFF00;
    public static final int CYAN = 0xFF00FFFF;
    public static final int MAGENTA = 0xFFFF00FF;
    public static final int TRANSPARENT = 0;

    public static int alpha(int color) { return color >>> 24; }
    public static int red(int color) { return (color >> 16) & 0xFF; }
    public static int green(int color) { return (color >> 8) & 0xFF; }
    public static int blue(int color) { return color & 0xFF; }

    public static int rgb(int red, int green, int blue) {
        return 0xFF000000 | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    public static int argb(int alpha, int red, int green, int blue) {
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    public static int rgb(float red, float green, float blue) {
        return argb(1f, red, green, blue);
    }

    public static int argb(float alpha, float red, float green, float blue) {
        return argb(Math.round(alpha * 255f), Math.round(red * 255f), Math.round(green * 255f), Math.round(blue * 255f));
    }

    public static int parseColor(String colorString) {
        if (colorString.startsWith("#")) {
            long color = Long.parseLong(colorString.substring(1), 16);
            if (colorString.length() == 7) color |= 0xFF000000L;
            else if (colorString.length() != 9) throw new IllegalArgumentException("Unknown color");
            return (int) color;
        }
        throw new IllegalArgumentException("Unknown color");
    }

    public static void colorToHSV(int color, float[] hsv) {
        float[] v = java.awt.Color.RGBtoHSB(red(color), green(color), blue(color), null);
        hsv[0] = v[0] * 360f;
        hsv[1] = v[1];
        hsv[2] = v[2];
    }

    public static int HSVToColor(float[] hsv) {
        return HSVToColor(255, hsv);
    }

    public static int HSVToColor(int alpha, float[] hsv) {
        float h = ((hsv[0] % 360f) + 360f) % 360f / 360f;
        int rgb = java.awt.Color.HSBtoRGB(h, clamp(hsv[1]), clamp(hsv[2]));
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}
