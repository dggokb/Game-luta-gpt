package android.content;
import android.content.res.AssetManager;
import android.content.res.Resources;
public class Context {
  private final Resources resources=new Resources();
  private final AssetManager assets=new AssetManager();
  public Resources getResources(){return resources;}
  public AssetManager getAssets(){return assets;}
  public String getPackageName(){return "com.gamelutagpt";}
}