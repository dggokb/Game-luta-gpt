package android.graphics;

public final class Color {
    public static final int WHITE = 0xffffffff;
    private Color() {}
    public static int rgb(int r,int g,int b){return argb(255,r,g,b);}
    public static int argb(int a,int r,int g,int b){
        return ((clamp(a)&255)<<24)|((clamp(r)&255)<<16)|((clamp(g)&255)<<8)|(clamp(b)&255);
    }
    public static int red(int c){return (c>>16)&255;}
    public static int green(int c){return (c>>8)&255;}
    public static int blue(int c){return c&255;}
    public static int alpha(int c){return (c>>>24)&255;}
    private static int clamp(int v){return Math.max(0,Math.min(255,v));}
}
