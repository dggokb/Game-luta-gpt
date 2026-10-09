package android.graphics;

import java.awt.geom.Path2D;

public final class Path {
    final Path2D.Float value=new Path2D.Float();
    public void moveTo(float x,float y){value.moveTo(x,y);}
    public void lineTo(float x,float y){value.lineTo(x,y);}
    public void close(){value.closePath();}
}
