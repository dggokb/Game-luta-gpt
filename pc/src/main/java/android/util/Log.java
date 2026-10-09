package android.util;
public final class Log {
  private Log(){}
  public static int d(String tag,String msg){System.out.println("[DEBUG] "+tag+": "+msg);return 0;}
  public static int i(String tag,String msg){System.out.println("[INFO] "+tag+": "+msg);return 0;}
  public static int w(String tag,String msg){System.err.println("[WARN] "+tag+": "+msg);return 0;}
  public static int w(String tag,String msg,Throwable t){System.err.println("[WARN] "+tag+": "+msg);t.printStackTrace();return 0;}
  public static int e(String tag,String msg){System.err.println("[ERROR] "+tag+": "+msg);return 0;}
  public static int e(String tag,String msg,Throwable t){System.err.println("[ERROR] "+tag+": "+msg);t.printStackTrace();return 0;}
}