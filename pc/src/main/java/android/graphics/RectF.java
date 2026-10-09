package android.graphics;
public final class RectF {
    public float left,top,right,bottom;
    public RectF(){}
    public RectF(float left,float top,float right,float bottom){set(left,top,right,bottom);}
    public float centerX(){return (left+right)*0.5f;}
    public float centerY(){return (top+bottom)*0.5f;}
    public void set(float left,float top,float right,float bottom){
        this.left=left;this.top=top;this.right=right;this.bottom=bottom;
    }
}
