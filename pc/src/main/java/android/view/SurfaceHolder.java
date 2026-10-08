package android.view;

import android.graphics.Canvas;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;

public final class SurfaceHolder {
    public interface Callback {
        void surfaceCreated(SurfaceHolder holder);
        void surfaceChanged(SurfaceHolder holder,int format,int width,int height);
        void surfaceDestroyed(SurfaceHolder holder);
    }

    private final SurfaceView view;
    private final Surface surface;
    private final List<Callback> callbacks=new ArrayList<>();
    private BufferStrategy strategy;

    SurfaceHolder(SurfaceView view){
        this.view=view;
        this.surface=new Surface(view);
    }

    public void addCallback(Callback callback){
        if(callback!=null&&!callbacks.contains(callback))callbacks.add(callback);
    }

    public Surface getSurface(){return surface;}

    void created(){
        ensureStrategy();
        for(Callback callback:List.copyOf(callbacks)){
            callback.surfaceCreated(this);
            callback.surfaceChanged(this,0,view.getWidth(),view.getHeight());
        }
    }

    void destroyed(){
        for(Callback callback:List.copyOf(callbacks))callback.surfaceDestroyed(this);
        strategy=null;
    }

    public Canvas lockCanvas(){
        ensureStrategy();
        if(strategy==null)return null;
        try{
            Graphics graphics=strategy.getDrawGraphics();
            if(!(graphics instanceof Graphics2D g2)){
                graphics.dispose();
                return null;
            }
            return new Canvas(g2,view.getWidth(),view.getHeight());
        }catch(IllegalStateException exception){
            return null;
        }
    }

    public Canvas lockHardwareCanvas(){ return lockCanvas(); }

    public void unlockCanvasAndPost(Canvas canvas){
        if(canvas!=null)canvas.dispose();
        if(strategy==null)return;
        try{
            strategy.show();
            Toolkit.getDefaultToolkit().sync();
        }catch(IllegalStateException ignored){}
    }

    private void ensureStrategy(){
        if(strategy!=null||!view.isDisplayable())return;
        try{
            view.createBufferStrategy(2);
            strategy=view.getBufferStrategy();
        }catch(IllegalStateException ignored){
            strategy=null;
        }
    }
}
