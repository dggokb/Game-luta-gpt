package android.util;

public final class Log {
    private Log(){}
    public static int w(String tag,String message){
        System.err.println("[WARN] " + tag + ": " + message);
        return 0;
    }
    public static int w(String tag,String message,Throwable error){
        System.err.println("[WARN] " + tag + ": " + message + (error==null?"":" - "+error));
        return 0;
    }
    public static int d(String tag,String message){
        System.out.println("[DEBUG] " + tag + ": " + message);
        return 0;
    }
}
